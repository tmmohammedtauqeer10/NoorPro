package com.noorpro.app.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.AssistantPhase
import com.noorpro.app.data.AssistantState
import com.noorpro.app.data.NoorAiOptions
import com.noorpro.app.data.VadDecision
import com.noorpro.app.data.VoiceActivityDetector
import com.noorpro.app.data.assistantStatus
import com.noorpro.app.ui.theme.MatteGold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * 1.2.0 (114) voice recorder for the Noor AI voice assistant: AAC/M4A, mono 16 kHz, stops by itself shortly after
 * you finish speaking ([VoiceActivityDetector]). Audio is only sent to Noor AI's own server for transcription.
 */
internal class NoorVoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val file get() = File(context.cacheDir, "noor_voice.m4a")

    /** [onDone] gets the recorded bytes (null when nothing was said or recording failed) and the reason it stopped. */
    private var doneCb: ((ByteArray?, VadDecision) -> Unit)? = null

    fun start(onLevel: (Float) -> Unit, onDone: (ByteArray?, VadDecision) -> Unit) {
        cancel()
        doneCb = onDone
        val r = runCatching {
            newRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(32_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        }.getOrNull()
        if (r == null) { onDone(null, VadDecision.NO_SPEECH); return }
        recorder = r
        val vad = VoiceActivityDetector()
        job = scope.launch {
            delay(150)
            while (isActive) {
                val d = vad.feed(runCatching { r.maxAmplitude }.getOrDefault(0), System.currentTimeMillis())
                onLevel(vad.level)
                if (d != VadDecision.CONTINUE) {
                    val bytes = finishRecording()
                    doneCb = null
                    onDone(if (vad.heardSpeech) bytes else null, d)
                    break
                }
                delay(100)
            }
        }
    }

    /** Stop now and deliver what was recorded to the [start] callback (user tapped "done"). */
    fun finishNow() {
        val cb = doneCb ?: return
        doneCb = null
        job?.cancel(); job = null
        val bytes = finishRecording()
        cb(bytes, if (bytes == null) VadDecision.NO_SPEECH else VadDecision.END_OF_SPEECH)
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()

    private fun finishRecording(): ByteArray? {
        val r = recorder ?: return null
        recorder = null
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        return if (ok) runCatching { file.readBytes() }.getOrNull()?.takeIf { it.size > 1_500 } else null
    }

    fun cancel() {
        doneCb = null
        job?.cancel(); job = null
        recorder?.let { runCatching { it.stop() }; runCatching { it.release() } }
        recorder = null
    }

    fun release() { cancel(); scope.cancel() }
}

/**
 * 1.2.0 (114) natural spoken answers: Gemini TTS through the Worker (Urdu, Hindi, Arabic, English and more), played
 * sentence by sentence while the next one is prepared. If the cloud voice isn't reachable it continues with the
 * phone's own voice ([NoorAiSpeaker]).
 */
internal class NoorCloudSpeaker(private val context: Context, private val device: NoorAiSpeaker, private val onState: (Long?) -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var player: MediaPlayer? = null
    var current: Long? = null
        private set

    fun speak(id: Long, plain: String, language: String, onDone: (() -> Unit)? = null) {
        stop()
        current = id; onState(id)
        val lang = language.substringBefore('-').ifBlank { "en" }
        val chunks = NoorAiOptions.speechChunks(plain)
        if (chunks.isEmpty()) { finish(onDone); return }
        job = scope.launch {
            var next = async(Dispatchers.IO) { AiFeatureService.speech(chunks[0], lang) }
            for (i in chunks.indices) {
                val audio = next.await()
                if (i + 1 < chunks.size) next = async(Dispatchers.IO) { AiFeatureService.speech(chunks[i + 1], lang) }
                if (audio == null) {
                    // Cloud voice unavailable: read the rest with the phone's voice.
                    next.cancel()
                    device.speak(id, chunks.drop(i).joinToString(" "), lang) { finish(onDone) }
                    return@launch
                }
                play(audio.first, audio.second, i)
            }
            finish(onDone)
        }
    }

    private suspend fun play(bytes: ByteArray, mime: String, i: Int) {
        val f = withContext(Dispatchers.IO) {
            File(context.cacheDir, "noor_tts_${i % 3}." + (if (mime.contains("mpeg")) "mp3" else "wav")).apply { writeBytes(bytes) }
        }
        suspendCancellableCoroutine { cont ->
            val mp = MediaPlayer()
            player = mp
            runCatching {
                mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                mp.setDataSource(f.absolutePath)
                mp.setOnCompletionListener { runCatching { it.release() }; if (player === it) player = null; if (cont.isActive) cont.resume(Unit) }
                mp.setOnErrorListener { p, _, _ -> runCatching { p.release() }; if (player === p) player = null; if (cont.isActive) cont.resume(Unit); true }
                mp.prepare()
                mp.start()
            }.onFailure { runCatching { mp.release() }; player = null; if (cont.isActive) cont.resume(Unit) }
            cont.invokeOnCancellation { runCatching { mp.stop() }; runCatching { mp.release() }; if (player === mp) player = null }
        }
    }

    private fun finish(onDone: (() -> Unit)?) {
        current = null; onState(null)
        onDone?.invoke()
    }

    /** Stops speaking (no completion callback). */
    fun stop() {
        job?.cancel(); job = null
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }; player = null
        device.stop()
        if (current != null) { current = null; onState(null) }
    }

    fun release() { stop(); scope.cancel() }
}

private val VoiceGreen = Color(0xFF1F7A5A)

/** Full-screen ChatGPT-style voice mode: animated orb, live status, what you said and the answer captions. */
@Composable
internal fun NoorVoiceAssistantOverlay(
    state: AssistantState,
    onMic: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onHandsFree: (Boolean) -> Unit,
    onKeyboard: () -> Unit,
) {
    val t = rememberInfiniteTransition(label = "orb")
    val breathe by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(if (state.phase == AssistantPhase.SPEAKING) 420 else 1100), RepeatMode.Reverse), label = "breathe")
    val spin by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "spin")
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF07140F), Color(0xFF0E2A20), Color(0xFF07140F))))
            .statusBarsPadding().navigationBarsPadding().testTag("noorai_voice_assistant"),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Noor AI · Voice", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (state.language.isBlank()) "Speak in Urdu, Hindi, Arabic, English or your language" else "Language: " + com.noorpro.app.data.NoorAiLanguage.shortName(state.language),
                        color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close voice mode", tint = Color.White) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Hands-free conversation", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = state.handsFree, onCheckedChange = onHandsFree,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VoiceGreen), modifier = Modifier.testTag("noorai_handsfree"))
            }
            Spacer(Modifier.weight(0.6f))
            val active = state.phase == AssistantPhase.LISTENING || state.phase == AssistantPhase.SPEAKING
            val orbScale = when (state.phase) {
                AssistantPhase.LISTENING -> 1f + 0.25f * state.level
                AssistantPhase.SPEAKING -> breathe
                AssistantPhase.TRANSCRIBING, AssistantPhase.THINKING -> 0.94f + 0.06f * kotlin.math.sin(spin * 6.283f)
                else -> 1f
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
                if (active) Box(Modifier.size(220.dp).scale(breathe).clip(CircleShape).background(VoiceGreen.copy(alpha = 0.16f)))
                Box(Modifier.size(170.dp).scale(orbScale).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(MatteGold, VoiceGreen, Color(0xFF0B3B2B))))
                    .border(2.dp, Color.White.copy(alpha = 0.18f), CircleShape))
            }
            Spacer(Modifier.height(18.dp))
            Text(assistantStatus(state), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.testTag("noorai_voice_status"))
            Spacer(Modifier.height(14.dp))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.heard.isNotBlank()) Text("“${state.heard}”", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                if (state.answer.isNotBlank() && (state.phase == AssistantPhase.SPEAKING || state.phase == AssistantPhase.IDLE)) {
                    Spacer(Modifier.height(10.dp))
                    Text(NoorAiMarkdown.parse(com.noorpro.app.data.AiFeatureService.parseMeta(state.answer).body).joinToString("\n") { plainOf(it) }.take(1200),
                        color = Color.White, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                RoundAction(Icons.Default.Keyboard, "Type instead", Color.White.copy(alpha = 0.12f), onKeyboard)
                val stopMode = state.phase == AssistantPhase.LISTENING || state.phase == AssistantPhase.SPEAKING || state.busy
                Box(Modifier.size(78.dp).clip(CircleShape).background(if (stopMode) Color(0xFFC0563F) else VoiceGreen)
                    .clickable { if (stopMode) onStop() else onMic() }.testTag("noorai_voice_mic"), contentAlignment = Alignment.Center) {
                    Icon(if (stopMode) Icons.Default.Stop else Icons.Default.Mic, if (stopMode) "Stop" else "Talk", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                RoundAction(Icons.Default.Close, "End voice mode", Color.White.copy(alpha = 0.12f), onClose)
            }
            Text("Voice is sent to Noor AI's server only to understand and answer you.", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}
