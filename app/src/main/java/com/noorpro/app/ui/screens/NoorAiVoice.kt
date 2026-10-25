package com.noorpro.app.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * 1.1.4 Noor AI voice: live speech-to-text (Android SpeechRecognizer, partial results) and spoken
 * answers (TextToSpeech). Everything runs on the phone; nothing new is sent to the backend.
 */

/** Voice languages offered for asking by voice. */
internal val NOOR_VOICE_LANGUAGES = listOf("en" to "English", "ur" to "اردو", "hi" to "हिन्दी", "ar" to "العربية")

enum class VoicePhase { IDLE, NEEDS_PERMISSION, LISTENING, PROCESSING, ERROR }

data class NoorVoiceState(
    val phase: VoicePhase = VoicePhase.IDLE,
    val partial: String = "",
    val level: Float = 0f,
    val language: String = "en",
    val error: String? = null,
    /** Final text, set once when recognition finishes; the screen sends it and resets. */
    val finalText: String? = null,
) {
    val active get() = phase == VoicePhase.LISTENING || phase == VoicePhase.PROCESSING
}

sealed interface VoiceEvent {
    data class Start(val language: String) : VoiceEvent
    object PermissionDenied : VoiceEvent
    object Ready : VoiceEvent
    data class Level(val rmsDb: Float) : VoiceEvent
    data class Partial(val text: String) : VoiceEvent
    object EndOfSpeech : VoiceEvent
    data class Final(val text: String) : VoiceEvent
    data class Error(val code: Int) : VoiceEvent
    object Cancel : VoiceEvent
    object Consumed : VoiceEvent
}

/** Pure state machine for the mic (unit tested). */
internal fun reduceVoice(s: NoorVoiceState, e: VoiceEvent): NoorVoiceState = when (e) {
    is VoiceEvent.Start -> NoorVoiceState(VoicePhase.LISTENING, language = e.language)
    VoiceEvent.PermissionDenied -> s.copy(phase = VoicePhase.ERROR, error = "Microphone permission is needed to ask by voice.")
    VoiceEvent.Ready -> if (s.phase == VoicePhase.LISTENING) s else s
    is VoiceEvent.Level -> if (s.phase == VoicePhase.LISTENING) s.copy(level = ((e.rmsDb + 2f) / 12f).coerceIn(0f, 1f)) else s
    is VoiceEvent.Partial -> if (s.active && e.text.isNotBlank()) s.copy(partial = e.text) else s
    VoiceEvent.EndOfSpeech -> if (s.phase == VoicePhase.LISTENING) s.copy(phase = VoicePhase.PROCESSING, level = 0f) else s
    is VoiceEvent.Final -> {
        val text = e.text.trim().ifBlank { s.partial.trim() }
        if (!s.active) s
        else if (text.isBlank()) s.copy(phase = VoicePhase.ERROR, error = "Didn't catch that. Tap the mic and try again.", level = 0f)
        else s.copy(phase = VoicePhase.IDLE, partial = text, finalText = text, level = 0f)
    }
    is VoiceEvent.Error -> if (!s.active) s else if (s.partial.isNotBlank() && e.code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        s.copy(phase = VoicePhase.IDLE, finalText = s.partial.trim(), level = 0f)
    else s.copy(phase = VoicePhase.ERROR, error = voiceErrorText(e.code), level = 0f)
    VoiceEvent.Cancel -> s.copy(phase = VoicePhase.IDLE, partial = "", level = 0f, finalText = null, error = null)
    VoiceEvent.Consumed -> s.copy(finalText = null, partial = "")
}

internal fun voiceErrorText(code: Int): String = when (code) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Tap the mic and try again."
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice needs a connection for this language."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed to ask by voice."
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "This language isn't available for voice on your phone."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice is busy. Try again in a moment."
    else -> "Voice input stopped. Tap the mic to try again."
}

/** Wraps SpeechRecognizer; emits [VoiceEvent]s. Use [isAvailable] first and fall back to RecognizerIntent. */
internal class NoorVoiceInput(private val context: Context, private val emit: (VoiceEvent) -> Unit) {
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    fun start(language: String) {
        stop()
        emit(VoiceEvent.Start(language))
        val r = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull() ?: run { emit(VoiceEvent.Error(-1)); return }
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = emit(VoiceEvent.Ready)
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) = emit(VoiceEvent.Level(rmsdB))
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = emit(VoiceEvent.EndOfSpeech)
            override fun onError(error: Int) = emit(VoiceEvent.Error(error))
            override fun onResults(results: Bundle?) =
                emit(VoiceEvent.Final(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()))
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { emit(VoiceEvent.Partial(it)) }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        runCatching { r.startListening(recognizerIntent(language)) }.onFailure { emit(VoiceEvent.Error(-1)) }
    }

    /** Stop listening and deliver what was heard. */
    fun finish() { runCatching { recognizer?.stopListening() } }

    fun stop() { runCatching { recognizer?.cancel(); recognizer?.destroy() }; recognizer = null }

    companion object {
        fun recognizerIntent(language: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechTag(language))
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechTag(language))
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Noor AI")
    }
}

// ---------------------------------------------------------------- spoken answers

/** One chunk to speak; [arabic] chunks use an Arabic voice (or are skipped if none is installed). */
internal data class TtsSegment(val text: String, val arabic: Boolean)

private val arabicRun = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF][\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF\\s\\u064B-\\u065F\\u0670۝٬،؛؟.,]*")

/**
 * Splits an answer (markdown already stripped) into speakable segments. Arabic-script runs become
 * their own segments. When the answer language itself is Urdu/Arabic the whole text is native and
 * is not split. Follow-up suggestions and the "Sources" list are not read aloud.
 */
internal fun ttsSegments(plain: String, answerLanguage: String = "en", arabicVoice: Boolean = true): List<TtsSegment> {
    val body = plain.substringBefore("\nSources:").replace(Regex("[*_#>`]"), "").trim()
    if (body.isBlank()) return emptyList()
    if (answerLanguage == "ar" || answerLanguage == "ur") return listOf(TtsSegment(body, answerLanguage == "ar"))
    val out = mutableListOf<TtsSegment>()
    var last = 0
    arabicRun.findAll(body).forEach { m ->
        val t = m.value.trim()
        if (t.count { it.code in 0x0600..0x06FF } < 2) return@forEach
        body.substring(last, m.range.first).trim().takeIf { it.isNotBlank() }?.let { out += TtsSegment(it, false) }
        if (arabicVoice) out += TtsSegment(t, true)
        last = m.range.last + 1
    }
    body.substring(last).trim().takeIf { it.isNotBlank() }?.let { out += TtsSegment(it, false) }
    // TextToSpeech has an input cap (~4000 chars); split long English runs on sentence ends.
    return out.flatMap { seg ->
        if (seg.text.length <= 3500) listOf(seg)
        else seg.text.split(Regex("(?<=[.!?])\\s+")).fold(mutableListOf<StringBuilder>()) { acc, s ->
            if (acc.isEmpty() || acc.last().length + s.length > 3500) acc += StringBuilder(s) else acc.last().append(' ').append(s); acc
        }.map { TtsSegment(it.toString(), seg.arabic) }
    }
}

internal fun ttsLocale(code: String): Locale = when (code) {
    "ur" -> Locale("ur", "PK"); "hi" -> Locale("hi", "IN"); "bn" -> Locale("bn", "BD"); "ar" -> Locale("ar"); else -> Locale.ENGLISH
}

/** Thin TextToSpeech wrapper: speak one answer at a time; [onState] reports the speaking message id (or null). */
internal class NoorAiSpeaker(context: Context, private val onState: (Long?) -> Unit) {
    private var ready = false
    private var arabicOk = false
    private var pending: (() -> Unit)? = null
    private var current: Long? = null
    private lateinit var tts: TextToSpeech
    init { tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            arabicOk = runCatching { tts.isLanguageAvailable(Locale("ar")) >= TextToSpeech.LANG_AVAILABLE }.getOrDefault(false)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { if (utteranceId?.endsWith("#last") == true) finished() }
                @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { finished() }
            })
            pending?.invoke(); pending = null
        } else { pending = null; finished() }
    } }

    private fun finished() { current = null; onState(null) }

    fun speak(id: Long, plain: String, language: String) {
        stop()
        current = id; onState(id)
        val run = {
            val segs = ttsSegments(plain, language, arabicOk)
            if (segs.isEmpty()) finished()
            segs.forEachIndexed { i, s ->
                tts.language = if (s.arabic) Locale("ar") else ttsLocale(language).takeIf { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE } ?: Locale.ENGLISH
                tts.speak(s.text, TextToSpeech.QUEUE_ADD, null, "$id-$i" + if (i == segs.lastIndex) "#last" else "")
            }
        }
        if (ready) run() else pending = run
    }

    fun stop() { if (current != null) { runCatching { tts.stop() }; finished() } }
    fun shutdown() { runCatching { tts.stop(); tts.shutdown() } }
}

/** Questions about rulings get a clear fiqh disclaimer under the answer. */
internal fun isFiqhQuestion(q: String): Boolean {
    val t = q.lowercase()
    val words = listOf("halal", "haram", "makruh", "fard", "wajib", "sunnah or", "permissible", "allowed", "is it ok", "is it okay", "can i ", "can we ",
        "ruling", "fatwa", "fiqh", "madhhab", "divorce", "talaq", "inheritance", "zakat", "nikah", "mahr", "riba", "interest", "mortgage",
        "invalid", "break my fast", "breaks wudu", "break wudu", "kaffarah", "حلال", "حرام", "فتوى", "جائز", "طلاق", "جائز ہے", "حرام ہے", "ज़कात", "हलाल", "हराम")
    return words.any { t.contains(it) }
}
