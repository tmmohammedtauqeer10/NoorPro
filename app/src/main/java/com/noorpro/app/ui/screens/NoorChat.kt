package com.noorpro.app.ui.screens

import android.content.Context
import android.media.MediaRecorder
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.UmmahMessage
import com.noorpro.app.ui.components.*
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// 1.0.92 chat redesign: WhatsApp/Telegram-style bubbles, wallpaper, composer, voice notes.
// ---------------------------------------------------------------------------------------------

internal val CHAT_TAIL = 7.dp

internal data class ChatColors(val mine: Color, val other: Color, val onMine: Color, val onOther: Color, val meta: Color, val header: Color)

@Composable
internal fun chatColors(): ChatColors = if (isStitchLight()) ChatColors(
    mine = Color(0xFFD6F5E3), other = Color.White, onMine = Color(0xFF0F1F1A), onOther = Color(0xFF111B21),
    meta = Color(0xFF667781), header = Color.White
) else ChatColors(
    mine = Color(0xFF0D5C48), other = Color(0xFF1F2C33), onMine = Color(0xFFE9EDEF), onOther = Color(0xFFE9EDEF),
    meta = Color(0xFF8696A0), header = Color(0xFF1A242A)
)

/** Rounded bubble with an optional WhatsApp tail on the top outer corner. Always reserves [CHAT_TAIL]. */
internal class ChatBubbleShape(private val mine: Boolean, private val tail: Boolean, private val radius: androidx.compose.ui.unit.Dp = 16.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = with(density) { CHAT_TAIL.toPx() }
        val r = with(density) { radius.toPx() }
        val small = with(density) { 4.dp.toPx() }
        val left = if (mine) 0f else t
        val right = if (mine) size.width - t else size.width
        val p = Path()
        p.addRoundRect(
            RoundRect(
                left, 0f, right, size.height,
                topLeftCornerRadius = CornerRadius(if (!mine && tail) 0f else if (!mine) small else r),
                topRightCornerRadius = CornerRadius(if (mine && tail) 0f else if (mine) small else r),
                bottomRightCornerRadius = CornerRadius(r), bottomLeftCornerRadius = CornerRadius(r)
            )
        )
        if (tail) {
            val tp = Path()
            if (mine) { tp.moveTo(right - 1f, 0f); tp.lineTo(size.width, 0f); tp.lineTo(right - 1f, t * 1.6f); tp.close() }
            else { tp.moveTo(left + 1f, 0f); tp.lineTo(0f, 0f); tp.lineTo(left + 1f, t * 1.6f); tp.close() }
            p.op(p, tp, PathOperation.Union)
        }
        return Outline.Generic(p)
    }
}

// ----------------------------------------- wallpaper -----------------------------------------

internal object ChatWallpaper {
    val names = listOf("Classic", "Plain", "Emerald mist", "Sand", "Midnight")
    fun load(c: Context) = c.getSharedPreferences("noor_chat_ui", Context.MODE_PRIVATE).getInt("wallpaper", 0)
    fun save(c: Context, i: Int) = c.getSharedPreferences("noor_chat_ui", Context.MODE_PRIVATE).edit().putInt("wallpaper", i).apply()
}

/** Subtle generated wallpaper (soft gradient + faint dot grid). Never the Recitation pattern image. */
@Composable
internal fun Modifier.chatWallpaper(index: Int): Modifier {
    val light = isStitchLight()
    val (a, b) = when (index) {
        1 -> if (light) Color(0xFFF4F5F4) to Color(0xFFF4F5F4) else Color(0xFF0E1417) to Color(0xFF0E1417)
        2 -> if (light) Color(0xFFE3F3EA) to Color(0xFFF3F8F2) else Color(0xFF071C17) to Color(0xFF0C1714)
        3 -> if (light) Color(0xFFF5EEDC) to Color(0xFFFBF7EE) else Color(0xFF1B170F) to Color(0xFF12100B)
        4 -> Color(0xFF0B141A) to Color(0xFF101D24)
        else -> if (light) Color(0xFFEFEAE2) to Color(0xFFF3EFE8) else Color(0xFF0B141A) to Color(0xFF0D171D)
    }
    val dot = if (light) Color.Black.copy(alpha = 0.035f) else Color.White.copy(alpha = 0.03f)
    return this.background(Brush.verticalGradient(listOf(a, b))).then(
        if (index == 1) Modifier else Modifier.drawBehind {
            val step = 22.dp.toPx(); var y = step / 2; var row = 0
            while (y < size.height) {
                var x = if (row % 2 == 0) step / 2 else step
                while (x < size.width) { drawCircle(dot, 1.4.dp.toPx(), Offset(x, y)); x += step }
                y += step; row++
            }
        }
    )
}

// ------------------------------------------ list chrome ---------------------------------------

internal fun chatDayLabel(ts: Long, now: Long = System.currentTimeMillis()): String {
    if (ts <= 0) return "Today"
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    val n = Calendar.getInstance().apply { timeInMillis = now }
    val sameYear = c.get(Calendar.YEAR) == n.get(Calendar.YEAR)
    val dayDiff = if (sameYear) n.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR) else 999
    return when {
        dayDiff == 0 -> "Today"
        dayDiff == 1 -> "Yesterday"
        dayDiff in 2..6 -> android.text.format.DateFormat.format("EEEE", ts).toString()
        sameYear -> android.text.format.DateFormat.format("d MMMM", ts).toString()
        else -> android.text.format.DateFormat.format("d MMMM yyyy", ts).toString()
    }
}

internal fun sameChatDay(a: Long, b: Long): Boolean {
    val x = Calendar.getInstance().apply { timeInMillis = a }; val y = Calendar.getInstance().apply { timeInMillis = b }
    return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
}

@Composable
internal fun NoorDateChip(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            label, color = chatColors().meta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.shadow(1.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(chatColors().header)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
internal fun NoorUnreadDivider(count: Int) {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(chatColors().header.copy(alpha = 0.7f)).padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
        Text("$count unread message${if (count == 1) "" else "s"}", color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Animated three-dot "typing" bubble on the incoming side. */
@Composable
internal fun NoorTypingBubble() {
    val col = chatColors()
    val tr = rememberInfiniteTransition(label = "typing")
    Box(Modifier.padding(top = 2.dp)) {
        Row(
            Modifier.clip(ChatBubbleShape(false, true)).background(col.other).padding(start = CHAT_TAIL + 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(3) { i ->
                val a by tr.animateFloat(0.25f, 1f, infiniteRepeatable(keyframes { durationMillis = 1100; 0.25f at (i * 180); 1f at (i * 180 + 280); 0.25f at (i * 180 + 600) }), label = "d$i")
                Box(Modifier.size(7.dp).alpha(a).clip(CircleShape).background(col.meta))
            }
        }
    }
}


@Composable
internal fun NoorScrollFab(unseen: Int, onClick: () -> Unit) {
    Box {
        Box(
            Modifier.size(42.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(chatColors().header).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.KeyboardArrowDown, "Scroll to latest", tint = chatColors().meta) }
        if (unseen > 0) Text(
            if (unseen > 99) "99+" else "$unseen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp).clip(CircleShape).background(stitchPrimary()).padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

// --------------------------------------------- voice ------------------------------------------

/** MediaRecorder (AAC/m4a) with live amplitude sampling for the recording waveform. */
internal class VoiceRecorderController(private val context: Context) {
    var recording by mutableStateOf(false); private set
    var locked by mutableStateOf(false)
    var elapsedMs by mutableLongStateOf(0L); private set
    val levels = mutableStateListOf<Float>()
    private var rec: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    data class Clip(val file: File, val durationMs: Long, val waveform: List<Int>)

    fun start(): Boolean = runCatching {
        val f = File(context.cacheDir, "noor-voice-${System.currentTimeMillis()}.m4a")
        val r = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioChannels(1); r.setAudioSamplingRate(44_100); r.setAudioEncodingBitRate(64_000)
        r.setOutputFile(f.absolutePath)
        r.prepare(); r.start()
        rec = r; file = f; startedAt = System.currentTimeMillis(); levels.clear(); elapsedMs = 0; locked = false; recording = true
        true
    }.getOrElse { runCatching { rec?.release() }; rec = null; false }

    /** Called ~every 70ms by the UI while recording. */
    fun sample() {
        val r = rec ?: return
        elapsedMs = System.currentTimeMillis() - startedAt
        val amp = runCatching { r.maxAmplitude }.getOrDefault(0)
        levels.add((kotlin.math.sqrt(amp / 32767f)).coerceIn(0.04f, 1f))
    }

    fun cancel() { stop(); file?.delete(); file = null }

    fun finish(): Clip? {
        val d = System.currentTimeMillis() - startedAt
        val ok = stop()
        val f = file ?: return null
        file = null
        if (!ok || d < 700 || !f.exists() || f.length() == 0L) { f.delete(); return null }
        return Clip(f, d, downsample(levels.toList(), 48))
    }

    private fun stop(): Boolean {
        val r = rec ?: return false
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        rec = null; recording = false; locked = false
        return ok
    }

    companion object {
        fun downsample(src: List<Float>, bars: Int): List<Int> {
            if (src.isEmpty()) return List(bars) { 8 }
            return List(bars) { i ->
                val a = i * src.size / bars; val b = maxOf(a + 1, (i + 1) * src.size / bars)
                ((src.subList(a, minOf(b, src.size)).maxOrNull() ?: 0f) * 100).roundToInt().coerceIn(4, 100)
            }
        }
    }
}

/** App-wide voice-note player: survives switching chats (Telegram-style); one note at a time. */
internal object VoicePlayer {
    var currentId by mutableStateOf(""); private set
    var playing by mutableStateOf(false); private set
    var positionMs by mutableLongStateOf(0L); private set
    var durationMs by mutableLongStateOf(0L); private set
    var speed by mutableFloatStateOf(1f); private set
    private var player: androidx.media3.exoplayer.ExoPlayer? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val p = player ?: return
            positionMs = p.currentPosition; if (p.duration > 0) durationMs = p.duration
            if (playing) handler.postDelayed(this, 50)
        }
    }

    fun toggle(context: Context, id: String, url: String, knownDuration: Long) {
        VoicePlayed.mark(context, id)
        val p = player ?: androidx.media3.exoplayer.ExoPlayer.Builder(context.applicationContext).build().also { ex ->
            ex.setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH).build(), true
            )
            ex.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying; if (isPlaying) { handler.removeCallbacks(tick); handler.post(tick) } }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == androidx.media3.common.Player.STATE_ENDED) { playing = false; positionMs = 0; currentId = ""; ex.pause(); ex.seekTo(0) }
                }
            })
            player = ex
        }
        if (currentId == id) { if (p.isPlaying) p.pause() else p.play(); return }
        currentId = id; positionMs = 0; durationMs = knownDuration
        p.setMediaItem(androidx.media3.common.MediaItem.fromUri(url)); p.prepare()
        p.setPlaybackSpeed(speed); p.play()
    }

    fun seek(id: String, fraction: Float) {
        val p = player ?: return
        if (id != currentId) return
        val d = if (p.duration > 0) p.duration else durationMs
        p.seekTo((d * fraction.coerceIn(0f, 1f)).toLong()); positionMs = p.currentPosition
    }

    fun cycleSpeed() {
        speed = when (speed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }
        player?.setPlaybackSpeed(speed)
    }

    /** Preview hook. */
    internal fun fake(id: String, pos: Long, dur: Long, isPlaying: Boolean) { currentId = id; positionMs = pos; durationMs = dur; playing = isPlaying }
}

internal object VoicePlayed {
    private fun prefs(c: Context) = c.getSharedPreferences("noor_voice_played", Context.MODE_PRIVATE)
    fun isPlayed(c: Context, id: String) = prefs(c).getBoolean(id, false)
    fun mark(c: Context, id: String) { if (id.isNotBlank()) prefs(c).edit().putBoolean(id, true).apply() }
}

internal fun formatVoice(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

@Composable
internal fun Waveform(bars: List<Int>, progress: Float, played: Color, idle: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = bars.size.coerceAtLeast(1)
        val gap = 2.dp.toPx(); val w = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1.5f)
        bars.forEachIndexed { i, v ->
            val h = (size.height * (v / 100f)).coerceAtLeast(3.dp.toPx())
            val x = i * (w + gap)
            drawRoundRect(if ((i + 0.5f) / n <= progress) played else idle, Offset(x, (size.height - h) / 2), Size(w, h), CornerRadius(w / 2))
        }
    }
}

/** Voice-note bubble body: play/pause, waveform seek, duration, speed, played/unplayed state. */
@Composable
internal fun VoiceNoteBubble(message: UmmahMessage, mine: Boolean, contentColor: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val active = VoicePlayer.currentId == message.id && message.id.isNotBlank()
    val playing = active && VoicePlayer.playing
    val dur = if (active && VoicePlayer.durationMs > 0) VoicePlayer.durationMs else message.durationMs
    val progress = if (active && dur > 0) VoicePlayer.positionMs.toFloat() / dur else 0f
    var played by remember(message.id) { mutableStateOf(mine || VoicePlayed.isPlayed(context, message.id)) }
    LaunchedEffect(active) { if (active) played = true }
    val bars = message.waveform.ifEmpty { remember(message.id) { val r = java.util.Random(message.id.hashCode().toLong()); List(40) { 15 + r.nextInt(70) } } }
    val accent = if (played) stitchPrimary() else Color(0xFF34B7F1)
    Row(Modifier.width(250.dp).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (mine) stitchPrimary() else accent).clickable { VoicePlayer.toggle(context, message.id, message.mediaUrl, message.durationMs) },
            contentAlignment = Alignment.Center
        ) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play voice message", tint = Color.White) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Waveform(
                bars, progress, played = if (mine) stitchPrimary() else accent, idle = contentColor.copy(alpha = 0.28f),
                modifier = Modifier.fillMaxWidth().height(30.dp)
                    .pointerInput(message.id, active) {
                        detectTapGestures { o -> if (active) VoicePlayer.seek(message.id, o.x / size.width) else VoicePlayer.toggle(context, message.id, message.mediaUrl, message.durationMs) }
                    }
                    .pointerInput(message.id, active) {
                        detectHorizontalDragGestures { ch, _ -> if (active) VoicePlayer.seek(message.id, ch.position.x / size.width) }
                    }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatVoice(if (active) VoicePlayer.positionMs.takeIf { it > 0 } ?: dur else dur), color = contentColor.copy(alpha = 0.65f), fontSize = 11.sp)
                if (!played) { Spacer(Modifier.width(5.dp)); Box(Modifier.size(7.dp).clip(CircleShape).background(accent)) }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (active) Text(
            "${VoicePlayer.speed.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }}×", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(contentColor.copy(alpha = 0.12f)).clickable { VoicePlayer.cycleSpeed() }.padding(horizontal = 7.dp, vertical = 3.dp)
        ) else Box(Modifier.size(34.dp).clip(CircleShape).background(contentColor.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Mic, null, tint = if (played) contentColor.copy(alpha = 0.55f) else accent, modifier = Modifier.size(18.dp))
        }
    }
}

// -------------------------------------------- composer -----------------------------------------

private val EMOJI = listOf(
    "😀", "😂", "🥹", "😊", "😍", "🥰", "😘", "😎", "🤔", "😴", "😢", "😭", "😡", "😮", "🙄", "😇",
    "🤲", "🙏", "👍", "👎", "👏", "🙌", "💪", "🤝", "❤️", "💚", "💛", "💙", "🔥", "✨", "⭐", "🌙",
    "🕌", "📿", "📖", "🕋", "☪️", "🌹", "🎉", "✅", "💯", "🤍", "😅", "🤗", "🥺", "😌", "🫶", "👋"
)

/**
 * Pill composer: emoji • text • attach • camera, and a mic that morphs into send. Hold mic to
 * record, slide left to cancel, slide up to lock, release to send.
 */
@Composable
internal fun NoorChatComposer(
    draft: String,
    onDraft: (String) -> Unit,
    sending: Boolean,
    canSendWithoutText: Boolean,
    recorder: VoiceRecorderController?,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onCamera: () -> Unit,
    ensureMic: () -> Boolean,
    onVoice: (VoiceRecorderController.Clip) -> Unit,
    previewRecording: Boolean = false,
    previewLevels: List<Float> = emptyList(),
    previewElapsed: Long = 0
) {
    val col = chatColors()
    var emojiOpen by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val cancelPx = with(density) { 110.dp.toPx() }; val lockPx = with(density) { 90.dp.toPx() }
    val recording = previewRecording || recorder?.recording == true
    val locked = recorder?.locked == true
    val canSend = draft.isNotBlank() || canSendWithoutText
    if (recorder != null) LaunchedEffect(recorder.recording) { while (recorder.recording) { recorder.sample(); delay(70) } }
    val levels = if (previewRecording) previewLevels else recorder?.levels.orEmpty()
    val elapsed = if (previewRecording) previewElapsed else recorder?.elapsedMs ?: 0

    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
            Row(
                Modifier.weight(1f).heightIn(min = 48.dp).shadow(1.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(col.header)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recording) {
                    val tr = rememberInfiniteTransition(label = "rec")
                    val a by tr.animateFloat(1f, 0.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "blink")
                    if (locked) IconButton(onClick = { recorder?.cancel() }) { Icon(Icons.Default.Delete, "Discard recording", tint = Color(0xFFE5484D)) }
                    else Box(Modifier.padding(start = 14.dp).size(10.dp).clip(CircleShape).background(Color(0xFFE5484D).copy(alpha = a)))
                    Text(formatVoice(elapsed), color = col.onOther, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp, end = 8.dp))
                    Waveform(
                        VoiceRecorderController.downsample(levels.takeLast(60), 30), 1f, played = stitchPrimary(), idle = col.meta,
                        modifier = Modifier.weight(1f).height(26.dp)
                    )
                    if (!locked) Text(
                        "‹ Slide to cancel", color = col.meta, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp).offset { IntOffset((drag.x / 2).roundToInt().coerceAtMost(0), 0) }
                    ) else Spacer(Modifier.width(8.dp))
                } else {
                    IconButton(onClick = { emojiOpen = !emojiOpen }) { Icon(if (emojiOpen) Icons.Default.Keyboard else Icons.Default.EmojiEmotions, "Emoji", tint = col.meta) }
                    Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        if (draft.isEmpty()) Text("Message", color = col.meta, fontSize = 16.sp)
                        BasicTextField(
                            value = draft, onValueChange = onDraft, maxLines = 5,
                            textStyle = TextStyle(color = col.onOther, fontSize = 16.sp),
                            cursorBrush = SolidColor(stitchPrimary()), modifier = Modifier.fillMaxWidth()
                        )
                    }
                    IconButton(onClick = onAttach) { Icon(Icons.Default.AttachFile, "Attach", tint = col.meta) }
                    AnimatedVisibility(draft.isEmpty(), enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                        IconButton(onClick = onCamera) { Icon(Icons.Default.PhotoCamera, "Camera", tint = col.meta) }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(contentAlignment = Alignment.BottomCenter) {
                if (recording && !locked) Column(
                    Modifier.offset(y = (-58).dp).width(40.dp).clip(RoundedCornerShape(20.dp)).background(col.header).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, "Slide up to lock", tint = col.meta, modifier = Modifier.size(18.dp))
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = col.meta, modifier = Modifier.size(18.dp))
                }
                val showSend = canSend || locked
                val scale by animateFloatAsState(if (recording && !locked) 1.35f else 1f, label = "micScale")
                Box(
                    Modifier.size(48.dp).scale(scale).offset { if (recording && !locked) IntOffset(drag.x.roundToInt().coerceAtMost(0) / 3, drag.y.roundToInt().coerceAtMost(0) / 3) else IntOffset.Zero }
                        .clip(CircleShape).background(stitchPrimary())
                        .then(
                            if (showSend) Modifier.clickable(enabled = !sending) {
                                if (locked) recorder?.finish()?.let(onVoice) else onSend()
                            } else Modifier.pointerInput(recorder) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    if (recorder == null || !ensureMic() || !recorder.start()) return@awaitEachGesture
                                    drag = Offset.Zero
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                        drag = ch.position - down.position
                                        if (!ch.pressed) { recorder.finish()?.let(onVoice); break }
                                        if (drag.x < -cancelPx) { recorder.cancel(); break }
                                        if (drag.y < -lockPx) { recorder.locked = true; break }
                                        ch.consume()
                                    }
                                    drag = Offset.Zero
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(showSend, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "micSend") { s ->
                        if (sending && s) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Icon(if (s) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic, if (s) "Send" else "Hold to record", tint = Color.White)
                    }
                }
            }
        }
        AnimatedVisibility(emojiOpen && !recording) {
            LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxWidth().height(200.dp).background(col.header), contentPadding = PaddingValues(6.dp)) {
                gridItems(EMOJI) { e -> Text(e, fontSize = 26.sp, modifier = Modifier.clickable { onDraft(draft + e) }.padding(6.dp)) }
            }
        }
    }
}

// --------------------------------------------- header ------------------------------------------

@Composable
internal fun NoorChatHeader(
    name: String, photoUrl: String, status: String, canCall: Boolean, wallpaper: Int,
    onWallpaper: (Int) -> Unit, onBack: () -> Unit, onProfile: () -> Unit, onCall: (Boolean) -> Unit
) {
    val col = chatColors()
    var menu by remember { mutableStateOf(false) }
    var wpMenu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().shadow(3.dp).background(col.header).statusBarsPadding().padding(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = col.onOther) }
        Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onProfile).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                PersonAvatar(name, photoUrl, 42.dp)
                if (status == "online") Box(
                    Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(col.header).padding(2.dp).clip(CircleShape).background(NoorOnlineGreen)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(name, color = col.onOther, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                AnimatedContent(status, label = "status") { st ->
                    when {
                        st == "typing…" -> Text("typing…", color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        st == "online" -> Text("online", color = NoorOnlineGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        st.isNotBlank() -> Text(st, color = col.meta, fontSize = 12.sp, maxLines = 1)
                        else -> Text("tap for profile", color = col.meta, fontSize = 12.sp)
                    }
                }
            }
        }
        if (canCall) {
            IconButton(onClick = { onCall(true) }) { Icon(Icons.Default.Videocam, "Video call", tint = col.onOther) }
            IconButton(onClick = { onCall(false) }) { Icon(Icons.Default.Call, "Voice call", tint = col.onOther) }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More", tint = col.onOther) }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("View profile") }, onClick = { menu = false; onProfile() })
                DropdownMenuItem(text = { Text("Wallpaper") }, leadingIcon = { Icon(Icons.Default.Wallpaper, null) }, onClick = { menu = false; wpMenu = true })
            }
            DropdownMenu(wpMenu, onDismissRequest = { wpMenu = false }) {
                ChatWallpaper.names.forEachIndexed { i, n ->
                    DropdownMenuItem(
                        text = { Text(n, fontWeight = if (i == wallpaper) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = { Box(Modifier.size(22.dp).clip(CircleShape).chatWallpaper(i)) },
                        onClick = { wpMenu = false; onWallpaper(i) }
                    )
                }
            }
        }
    }
}

// --------------------------------------------- forward -----------------------------------------

/** Forward a message to another existing chat (re-uses the original media URL; no re-upload). */
@Composable
internal fun ForwardMessageDialog(message: UmmahMessage, myUid: String, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { com.noorpro.app.data.UmmahRepository() }
    var chats by remember { mutableStateOf<List<com.noorpro.app.data.UmmahChat>>(emptyList()) }
    val sent = remember { mutableStateListOf<String>() }
    DisposableEffect(Unit) { repo.observeChats { l, _ -> chats = l }; onDispose { repo.close() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Forward to…") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(chats.size) { i ->
                    val c = chats[i]
                    val other = c.participantUids.firstOrNull { it != myUid } ?: return@items
                    val nm = c.participantNames[other].orEmpty().ifBlank { "Member" }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PersonAvatar(nm, "", 38.dp); Spacer(Modifier.width(10.dp))
                        Text(nm, Modifier.weight(1f), maxLines = 1)
                        val done = other in sent
                        TextButton(enabled = !done, onClick = {
                            sent += other
                            val extra = buildMap<String, Any> {
                                put("forwarded", true)
                                if (message.type == "audio") { put("durationMs", message.durationMs); put("waveform", message.waveform) }
                            }
                            repo.sendChatMessage(context, other, nm, message.text, message.type, null, sharedMediaUrl = message.mediaUrl, extra = extra) { ok, err ->
                                if (!ok) { sent -= other; android.widget.Toast.makeText(context, err ?: "Couldn't forward", android.widget.Toast.LENGTH_SHORT).show() }
                            }
                        }) { Text(if (done) "Sent" else "Send") }
                    }
                }
            }
        }
    )
}
