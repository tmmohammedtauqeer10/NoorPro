package com.noorpro.app.data

/**
 * 1.2.0 (114) Noor AI voice assistant logic. Pure Kotlin so it is unit tested on the JVM.
 *
 * Flow: tap the mic -> record (auto-stops after you finish speaking) -> Whisper on the Worker transcribes and
 * detects the language -> Noor AI answers in that language -> the answer is spoken with a natural cloud voice
 * (Gemini TTS; the phone's own voice offline) -> in hands-free mode it listens again.
 */
enum class VadDecision { CONTINUE, END_OF_SPEECH, NO_SPEECH, MAX_LENGTH }

/**
 * End-of-speech detection from MediaRecorder.getMaxAmplitude() samples (0..32767, polled ~every 100 ms).
 * The first samples set a noise floor; speech must rise clearly above it; after speech, [silenceMs] of quiet ends it.
 */
class VoiceActivityDetector(
    private val silenceMs: Long = 1_400,
    private val noSpeechMs: Long = 8_000,
    private val maxMs: Long = 45_000,
    private val minSpeechLevel: Int = 1_800,
) {
    private var start = -1L
    private var lastLoud = -1L
    private var noise = 0.0
    private var samples = 0
    var heardSpeech = false
        private set

    /** Normalised 0..1 level for the UI. */
    var level = 0f
        private set

    fun feed(amplitude: Int, nowMs: Long): VadDecision {
        if (start < 0) start = nowMs
        val a = amplitude.coerceIn(0, 32_767)
        level = (a / 12_000f).coerceIn(0f, 1f)
        if (samples < 5) { noise = (noise * samples + a) / (samples + 1); samples++ }
        val threshold = maxOf(minSpeechLevel.toDouble(), noise * 2.5)
        if (a >= threshold) { heardSpeech = true; lastLoud = nowMs }
        val elapsed = nowMs - start
        return when {
            elapsed >= maxMs -> VadDecision.MAX_LENGTH
            heardSpeech && nowMs - lastLoud >= silenceMs -> VadDecision.END_OF_SPEECH
            !heardSpeech && elapsed >= noSpeechMs -> VadDecision.NO_SPEECH
            else -> VadDecision.CONTINUE
        }
    }
}

enum class AssistantPhase { IDLE, LISTENING, TRANSCRIBING, THINKING, SPEAKING, ERROR }

data class AssistantState(
    val phase: AssistantPhase = AssistantPhase.IDLE,
    val heard: String = "",
    val answer: String = "",
    val language: String = "",
    val level: Float = 0f,
    val error: String? = null,
    val handsFree: Boolean = false,
    val turns: Int = 0,
) {
    val busy get() = phase == AssistantPhase.TRANSCRIBING || phase == AssistantPhase.THINKING
}

sealed interface AssistantEvent {
    object Listen : AssistantEvent
    data class Level(val level: Float) : AssistantEvent
    object Recorded : AssistantEvent
    object NoSpeech : AssistantEvent
    data class Heard(val text: String, val language: String) : AssistantEvent
    data class Answer(val text: String) : AssistantEvent
    object SpeakDone : AssistantEvent
    data class Fail(val message: String) : AssistantEvent
    object Stop : AssistantEvent
    data class HandsFree(val on: Boolean) : AssistantEvent
}

/** Pure state machine for the voice assistant overlay. */
fun reduceAssistant(s: AssistantState, e: AssistantEvent): AssistantState = when (e) {
    AssistantEvent.Listen -> s.copy(phase = AssistantPhase.LISTENING, level = 0f, error = null, heard = "")
    is AssistantEvent.Level -> if (s.phase == AssistantPhase.LISTENING) s.copy(level = e.level.coerceIn(0f, 1f)) else s
    AssistantEvent.Recorded -> if (s.phase == AssistantPhase.LISTENING) s.copy(phase = AssistantPhase.TRANSCRIBING, level = 0f) else s
    AssistantEvent.NoSpeech -> if (s.phase != AssistantPhase.LISTENING) s
        else s.copy(phase = AssistantPhase.IDLE, level = 0f, error = if (s.handsFree) "Hands-free paused. Tap the mic to talk again." else "I didn't hear anything. Tap the mic and speak.")
    is AssistantEvent.Heard -> if (s.phase != AssistantPhase.TRANSCRIBING && s.phase != AssistantPhase.LISTENING) s
        else if (e.text.isBlank()) s.copy(phase = AssistantPhase.ERROR, error = "Didn't catch that. Tap the mic and try again.")
        else s.copy(phase = AssistantPhase.THINKING, heard = e.text.trim(), language = e.language, answer = "", error = null)
    is AssistantEvent.Answer -> if (s.phase != AssistantPhase.THINKING) s
        else s.copy(phase = AssistantPhase.SPEAKING, answer = e.text, turns = s.turns + 1)
    AssistantEvent.SpeakDone -> if (s.phase != AssistantPhase.SPEAKING) s
        else if (s.handsFree) s.copy(phase = AssistantPhase.LISTENING, level = 0f, heard = "") else s.copy(phase = AssistantPhase.IDLE)
    is AssistantEvent.Fail -> s.copy(phase = AssistantPhase.ERROR, error = e.message, level = 0f)
    AssistantEvent.Stop -> s.copy(phase = AssistantPhase.IDLE, level = 0f)
    is AssistantEvent.HandsFree -> s.copy(handsFree = e.on)
}

/** Status line under the orb, by phase. */
fun assistantStatus(s: AssistantState): String = when (s.phase) {
    AssistantPhase.IDLE -> s.error ?: "Tap the mic and ask in any language"
    AssistantPhase.LISTENING -> "Listening…"
    AssistantPhase.TRANSCRIBING -> "Understanding…"
    AssistantPhase.THINKING -> "Thinking…"
    AssistantPhase.SPEAKING -> "Speaking…"
    AssistantPhase.ERROR -> s.error ?: "Something went wrong. Tap the mic to try again."
}
