package com.noorpro.app.ui.screens

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.formatClock
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon

/*
 * 1.1.3 — reel editor: timeline trim + split, speed, filters, timed text + stickers, original volume / mute,
 * licensed Al-Noor sound, cover. Preview = the frame under the playhead with the edit applied (the host
 * fetches frames); export runs through media3 Transformer (ReelExporter) with a progress bar.
 */

enum class ReelTab(val label: String) {
    TRIM("Clips"), SPEED("Speed"), FILTERS("Filters"), TEXT("Text"), CAPTIONS("Captions"), STICKERS("Stickers"), AUDIO("Sound"),
    VOICE("Voice"), LAYERS("Layers"), TEMPLATES("Templates"), COVER("Cover")
}

/** Voiceover recording, owned by the host (it holds the mic + RECORD_AUDIO request). */
@Stable
class VoiceControl(val onStart: () -> Unit, val onStop: () -> Unit) {
    var recording by mutableStateOf(false)
    var level by mutableFloatStateOf(0f)
    var elapsedMs by mutableLongStateOf(0L)
}

@Stable
class ReelEditorState(initial: ReelEdit, tab: ReelTab = ReelTab.TRIM) {
    private val history = EditHistory(initial)
    var edit by mutableStateOf(initial); private set
    var tab by mutableStateOf(tab)
    var playheadMs by mutableLongStateOf(0L)
    var selected by mutableStateOf<String?>(null)
    var selectedClip by mutableIntStateOf(-1)
    /** null = not exporting; 0..1 = progress. */
    var exportProgress by mutableStateOf<Float?>(null)
    var exportError by mutableStateOf<String?>(null)
    /** Live preview playing (the host drives ExoPlayer from this + [playheadMs]). */
    var playing by mutableStateOf(false)
    /** Bumped whenever the user scrubs, so the host seeks its player. */
    var seekToken by mutableIntStateOf(0)
    var picker by mutableStateOf<StickerKind?>(null)
    var selectedCaption by mutableStateOf<String?>(null)
    var filterMode by mutableIntStateOf(0)
    internal val snap = SnapState()
    fun seek(ms: Long) { playheadMs = ms.coerceIn(0, edit.outputMs); seekToken++ }
    private var rev by mutableIntStateOf(0)
    private var seq = 0

    val canUndo get() = rev.let { history.canUndo }
    val canRedo get() = rev.let { history.canRedo }
    val dirty get() = rev.let { history.isDirty } || edit != history.current
    val selectedLayer get() = edit.layers.firstOrNull { it.id == selected }

    fun live(e: ReelEdit) { edit = e }
    fun commit(e: ReelEdit = edit) { edit = e; history.commit(e); rev++; playheadMs = playheadMs.coerceIn(0, edit.outputMs) }
    fun undo() { edit = history.undo(); rev++ }
    fun redo() { edit = history.redo(); rev++ }
    fun reset() { edit = history.reset(); rev++; selected = null; selectedClip = -1 }
    fun newId(prefix: String) = "${prefix}_${System.nanoTime()}_${seq++}"
}

@Composable
fun NoorReelEditor(
    state: ReelEditorState,
    frame: Bitmap?,
    filmstrip: List<Bitmap>,
    sounds: List<ReelSound>,
    onCancel: () -> Unit,
    onDone: (ReelEdit) -> Unit,
    onCancelExport: () -> Unit = {},
    modifier: Modifier = Modifier,
    stickerLabels: Map<StickerKind, String> = emptyMap(),
    coverFrame: Bitmap? = null,
    livePreview: (@Composable (Modifier) -> Unit)? = null,
    stickerSources: StickerSources = StickerSources(),
    interactive: Boolean = false,
    onAddClip: (() -> Unit)? = null,
    onRecordClip: (() -> Unit)? = null,
    voice: VoiceControl? = null
) {
    val ctx = LocalContext.current
    val fonts = remember { EditorTypefaces.load(ctx) }
    val e = state.edit
    val thumb = remember(frame) { frame?.let { Bitmap.createScaledBitmap(it, 140, (140f * it.height / it.width).toInt().coerceAtLeast(1), true) } }
    Box(modifier.fillMaxSize().background(stitchBackground()).testTag("reel_editor")) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            EditorTopBar(
                title = "Edit reel", subtitle = "${formatClock(e.outputMs)} · ${e.clips.size} clip${if (e.clips.size == 1) "" else "s"} · ${if (e.ramp != SpeedRamp.NONE) e.ramp.label else e.speed.label}" +
                    (e.sound?.let { " · ♪ ${it.title}" } ?: ""),
                canUndo = state.canUndo, canRedo = state.canRedo, dirty = state.dirty,
                onClose = onCancel, onUndo = state::undo, onRedo = state::redo, onReset = state::reset,
                onDone = { onDone(state.edit) }, doneLabel = if (ReelExportParams.needsFullExport(e)) "Export" else "Done",
                doneEnabled = state.exportProgress == null
            )
            Box(Modifier.weight(1f).fillMaxWidth().background(editorStage()).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                ReelPreview(state, if (state.tab == ReelTab.COVER) coverFrame ?: frame else frame, fonts, if (state.tab == ReelTab.COVER) null else livePreview)
            }
            when (state.tab) {
                ReelTab.TRIM -> TrimPanel(state, filmstrip, onAddClip, onRecordClip)
                ReelTab.SPEED -> EditorPanel(Modifier.testTag("reel_speed_panel")) {
                    EditorLabel("Speed", "${formatClock(e.keptMs)} → ${formatClock(e.outputMs)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReelSpeed.entries.forEach { sp -> EditorChip(sp.label, sp == e.speed, Modifier.weight(1f).testTag("reel_speed_${sp.name}")) { state.commit(state.edit.withSpeed(sp)) } }
                    }
                    EditorLabel("Speed ramp", e.ramp.label)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpeedRamp.entries.forEach { r -> EditorChip(r.label, r == e.ramp, Modifier.testTag("reel_ramp_${r.name}")) { state.commit(state.edit.withRamp(r)) } }
                    }
                    RampGraph(e.ramp)
                    EditorHint(when {
                        e.ramp != SpeedRamp.NONE -> "Ramps speed up and slow down across the whole reel. Text and stickers stay on their moments."
                        e.speed == ReelSpeed.HALF -> "Slow motion — great for calm recitation moments. Sound is slowed too; pitch is kept."
                        else -> "Text and stickers stay on the same moments when you change speed."
                    })
                }
                ReelTab.FILTERS -> if (state.filterMode == 0) EditorPanel(Modifier.testTag("reel_filter_panel")) {
                    FilterModeRow(state)
                    EditorLabel("Filters", e.filter.note)
                    FilterRow(thumb, e.filter) { f -> state.commit(state.edit.copy(filter = f, strength = if (f == state.edit.filter) state.edit.strength else 1f)) }
                    if (e.filter != NoorFilter.ORIGINAL) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Strength", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
                        EditorSlider(e.strength, 0f..1f, { state.live(state.edit.copy(strength = it)) }, { state.commit() }, Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vignette", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
                        EditorSlider(e.adjust.vignette, 0f..1f, { state.live(state.edit.copy(adjust = state.edit.adjust.copy(vignette = it))) }, { state.commit() }, Modifier.weight(1f))
                    }
                } else {
                    var kind by remember { mutableStateOf(AdjustKind.BRIGHTNESS) }
                    AdjustPanel(e.adjust, kind, { kind = it }, { a -> state.live(state.edit.copy(adjust = a)) }, { state.commit() }, videoOnly = true) { FilterModeRow(state) }
                }
                ReelTab.TEXT -> TextToolPanel(
                    layer = state.selectedLayer?.takeIf { it.isText },
                    onAdd = {
                        val start = state.playheadMs.coerceAtMost((e.outputMs - 3000).coerceAtLeast(0))
                        val l = EditorLayer.text(state.newId("t"), "Alhamdulillah", EditorFont.CLASSIC).copy(y = 0.36f, pill = TextPill.SOFT, pillColor = 0xFF0B6243, anim = TextAnim.POP)
                            .timed(start, start + 3000, e.outputMs)
                        state.commit(state.edit.upsert(l)); state.selected = l.id
                    },
                    onChange = { l, commit -> val n = state.edit.copy(layers = state.edit.layers.map { if (it.id == l.id) l else it }); if (commit) state.commit(n) else state.live(n) },
                    onDelete = { l -> state.commit(state.edit.remove(l.id)); state.selected = null },
                    showAnim = true
                ) {
                    state.selectedLayer?.let { l -> LayerTimingRow(l, e.outputMs) { nl, commit -> val n = state.edit.copy(layers = state.edit.layers.map { if (it.id == nl.id) nl else it }); if (commit) state.commit(n) else state.live(n) } }
                    LayerLanes(e, state)
                }
                ReelTab.CAPTIONS -> CaptionsPanel(state)
                ReelTab.STICKERS -> Column {
                    val sel = state.selectedLayer?.takeIf { !it.isText }
                    val pk = state.picker
                    fun add(l: EditorLayer) {
                        val start = state.playheadMs.coerceAtMost((state.edit.outputMs - 3000).coerceAtLeast(0))
                        val t = l.timed(start, start + 4000, state.edit.outputMs)
                        state.commit(state.edit.upsert(t)); state.selected = t.id; state.picker = null
                    }
                    when {
                        pk == StickerKind.COUNTDOWN -> CountdownPicker(stickerSources, { _, label -> add(EditorLayer(state.newId("s"), sticker = StickerKind.COUNTDOWN, text = label, sub = "Prepare your heart", y = 0.5f)) }) { state.picker = null }
                        pk != null -> QuotePicker(pk, stickerSources, { q -> add(quoteLayer(pk, state.newId("s"), q)) }) { state.picker = null }
                        else -> {
                            sel?.let { l ->
                                EditorPanel { LayerTimingRow(l, e.outputMs) { nl, commit -> val n = state.edit.copy(layers = state.edit.layers.map { if (it.id == nl.id) nl else it }); if (commit) state.commit(n) else state.live(n) } }
                                if (l.sticker?.let { it.card || it.interactive } == true) CardEditPanel(l, { nl, commit -> val n = state.edit.copy(layers = state.edit.layers.map { if (it.id == nl.id) nl else it }); if (commit) state.commit(n) else state.live(n) }) {
                                    state.commit(state.edit.remove(l.id)); state.selected = null
                                }
                            }
                            if (sel?.sticker?.let { it.card || it.interactive } != true) StickerTray(fonts, stickerLabels, interactive) { k ->
                                if (k.needsPicker) state.picker = k else add(stickerLayer(k, state.newId("s"), stickerLabels, stickerSources))
                            }
                        }
                    }
                }
                ReelTab.AUDIO -> AudioPanel(state, sounds)
                ReelTab.VOICE -> VoicePanel(state, voice)
                ReelTab.LAYERS -> LayersPanel(
                    e.layers, state.selected, { state.selected = it },
                    { id, d -> state.commit(state.edit.copy(layers = Layers.move(state.edit.layers, id, d))) },
                    { id -> state.commit(state.edit.copy(layers = Layers.toggleLock(state.edit.layers, id))) },
                    { id -> state.commit(state.edit.remove(id)); if (state.selected == id) state.selected = null }
                )
                ReelTab.TEMPLATES -> TemplatesPanel { t ->
                    val (filter, layers) = t.build(templateData(stickerSources), state::newId, state.edit.outputMs)
                    var n = state.edit; if (filter != null && n.filter == NoorFilter.ORIGINAL) n = n.copy(filter = filter, strength = 0.85f)
                    layers.forEach { n = n.upsert(it) }
                    state.commit(n); state.selected = layers.firstOrNull()?.id; state.seek(0)
                }
                ReelTab.COVER -> EditorPanel(Modifier.testTag("reel_cover_panel")) {
                    EditorLabel("Cover", formatClock(e.coverMs))
                    EditorSlider(e.coverMs.toFloat(), 0f..e.outputMs.coerceAtLeast(1).toFloat(), { state.live(state.edit.copy(coverMs = it.toLong())); state.playheadMs = it.toLong() }, { state.commit() })
                    EditorHint("The frame people see first in the feed and on your profile. Filters, text and stickers show on the cover too.")
                    EditorLabel("Export quality", e.quality.note)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportQuality.entries.forEach { q -> EditorChip(q.label, q == e.quality, Modifier.weight(1f).testTag("reel_quality_${q.name}")) { state.commit(state.edit.copy(quality = q)) } }
                    }
                }
            }
            EditorTabBar(
                listOf(
                    EditorTabSpec(ReelTab.TRIM.name, "Clips", Icons.Default.ContentCut), EditorTabSpec(ReelTab.SPEED.name, "Speed", Icons.Default.Speed),
                    EditorTabSpec(ReelTab.FILTERS.name, "Filters", Icons.Default.AutoAwesome), EditorTabSpec(ReelTab.TEXT.name, "Text", Icons.Default.TextFields),
                    EditorTabSpec(ReelTab.CAPTIONS.name, "Captions", Icons.Default.ClosedCaption),
                    EditorTabSpec(ReelTab.STICKERS.name, "Stickers", Icons.Default.EmojiEmotions), EditorTabSpec(ReelTab.AUDIO.name, "Sound", Icons.Default.MusicNote),
                    EditorTabSpec(ReelTab.VOICE.name, "Voice", Icons.Default.Mic), EditorTabSpec(ReelTab.LAYERS.name, "Layers", Icons.Default.Layers),
                    EditorTabSpec(ReelTab.TEMPLATES.name, "Templates", Icons.Default.Dashboard), EditorTabSpec(ReelTab.COVER.name, "Cover", Icons.Default.Image)
                ), state.tab.name
            ) { state.tab = ReelTab.valueOf(it); state.picker = null }
        }
        state.exportProgress?.let { p -> ExportOverlay(p, frame, e, onCancelExport) }
    }
}

@Composable
private fun ReelPreview(s: ReelEditorState, frame: Bitmap?, fonts: EditorTypefaces, live: (@Composable (Modifier) -> Unit)?) {
    val e = s.edit
    val paint = remember(e.filter, e.strength, e.adjust) { e.matrix.paint() }
    val at = if (s.tab == ReelTab.COVER) e.coverMs else s.playheadMs
    val look = if (s.tab == ReelTab.COVER) null else e.transitionAt(at)
    val drawn = remember(e.layers, e.captions, e.captionStyle) { e.drawLayers() }
    val buzz = rememberSnapBuzz()
    Box(
        Modifier.fillMaxHeight().aspectRatio(9f / 16f).clip(RoundedCornerShape(18.dp)).background(StitchEmeraldDeep)
            .border(1.dp, StitchGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).testTag("reel_preview")
    ) {
        if (live != null) live(Modifier.fillMaxSize().graphicsLayer {
            scaleX = look?.scale ?: 1f; scaleY = look?.scale ?: 1f; translationX = (look?.dx ?: 0f) * size.width
        })
        Canvas(
            Modifier.fillMaxSize().pointerInput(s.tab) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val hit = LayerPainter.hit(s.edit.layers, down.position.x, down.position.y, size.width, size.height, fonts, at)
                    s.selected = hit?.id
                    if (hit == null) return@awaitEachGesture
                    if (s.tab != ReelTab.LAYERS) s.tab = if (hit.isText) ReelTab.TEXT else ReelTab.STICKERS
                    if (hit.locked) return@awaitEachGesture
                    s.playing = false
                    var moved = false
                    var raw: EditorLayer = hit
                    do {
                        val ev = awaitPointerEvent()
                        val pan = ev.calculatePan(); val zoom = ev.calculateZoom(); val rot = ev.calculateRotation()
                        if (pan != Offset.Zero || zoom != 1f || rot != 0f) {
                            moved = true
                            raw = raw.dragBy(pan.x / size.width, pan.y / size.height).transform(zoom, rot)
                            val shown = s.snap.apply(raw, buzz)
                            s.live(s.edit.copy(layers = s.edit.layers.map { if (it.id == hit.id) shown else it }))
                            ev.changes.forEach { it.consume() }
                        }
                    } while (ev.changes.any { it.pressed })
                    s.snap.clear()
                    if (moved) s.commit()
                }
            }
        ) {
            drawIntoCanvas { cv ->
                val c = cv.nativeCanvas; val w = size.width; val h = size.height
                if (live == null && frame != null) {
                    c.save()
                    look?.let { lk -> c.translate(lk.dx * w, 0f); c.scale(lk.scale, lk.scale, w / 2, h / 2) }
                    c.drawBitmap(frame, centerCrop(frame.width, frame.height, w / h), RectF(0f, 0f, w, h), paint)
                    c.restore()
                }
                look?.takeIf { it.dim > 0f }?.let { c.drawColor(android.graphics.Color.argb((it.dim * 255).toInt().coerceIn(0, 255), 0, 0, 0)) }
                LayerPainter.drawVignette(c, w.toInt(), h.toInt(), e.adjust.vignette)
                LayerPainter.drawAll(c, drawn, w.toInt(), h.toInt(), fonts, atMs = at, selected = s.selected)
                LayerPainter.drawGuides(c, w.toInt(), h.toInt(), s.snap.vertical, s.snap.horizontal)
            }
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.55f))
                .clickable(enabled = s.tab != ReelTab.COVER) { if (!s.playing && s.playheadMs >= e.outputMs - 50) s.seek(0); s.playing = !s.playing }
                .padding(start = 6.dp, end = 9.dp, top = 3.dp, bottom = 3.dp).testTag("reel_play"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (s.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (s.playing) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text("${formatClock(at)} / ${formatClock(e.outputMs)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (e.speed != ReelSpeed.NORMAL || e.ramp != SpeedRamp.NONE || e.muted || e.sound != null || e.voices.isNotEmpty() || e.segmentAt(at)?.freeze == true) Row(
            Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (e.segmentAt(at)?.freeze == true) PreviewPill("Freeze")
            if (e.ramp != SpeedRamp.NONE) PreviewPill(e.ramp.label) else if (e.speed != ReelSpeed.NORMAL) PreviewPill(e.speed.label)
            if (e.muted) PreviewPill("Muted")
            if (e.voices.isNotEmpty()) PreviewPill("🎙 ${e.voices.size}")
            e.sound?.let { PreviewPill("♪ ${it.title}") }
        }
    }
}

@Composable
private fun FilterModeRow(s: ReelEditorState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorChip("Filters", s.filterMode == 0, Modifier.testTag("reel_filters_mode")) { s.filterMode = 0 }
        EditorChip("Adjust", s.filterMode == 1, Modifier.testTag("reel_adjust_mode")) { s.filterMode = 1 }
    }
}

@Composable
private fun RampGraph(r: SpeedRamp) {
    val accent = editorAccent()
    Canvas(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(stitchSurface()).testTag("reel_ramp_graph")) {
        val maxF = 2.6f; var x0 = 0f
        r.steps.forEach { (end, f) ->
            val x1 = end * size.width; val y = size.height * (1f - (f / maxF).coerceIn(0.05f, 1f))
            drawRect(accent.copy(alpha = 0.35f), topLeft = Offset(x0, y), size = androidx.compose.ui.geometry.Size(x1 - x0, size.height - y))
            drawLine(accent, Offset(x0, y), Offset(x1, y), strokeWidth = 5f)
            x0 = x1
        }
        val one = size.height * (1f - 1f / maxF)
        drawLine(StitchGold.copy(alpha = 0.7f), Offset(0f, one), Offset(size.width, one), strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    }
}

@Composable
private fun PreviewPill(text: String) {
    Text(text, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 150.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, StitchGold.copy(alpha = 0.6f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** Clips: timeline (source view for one simple video, output view otherwise), trim, split, freeze, reorder, transitions, add clips. */
@Composable
private fun TrimPanel(s: ReelEditorState, filmstrip: List<Bitmap>, onAddClip: (() -> Unit)?, onRecordClip: (() -> Unit)?) {
    val e = s.edit
    val simple = e.sources.size <= 1 && !e.hasFreeze && e.clips.zipWithNext().all { (a, b) -> a.endMs <= b.startMs }
    EditorPanel(Modifier.testTag("reel_trim_panel")) {
        EditorLabel("Timeline", if (simple) "${formatClock(e.trimStartMs)} – ${formatClock(e.trimEndMs)} · ${formatClock(e.outputMs)}" else "${e.clips.size} clips · ${formatClock(e.outputMs)}")
        Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(StitchEmeraldDeep).testTag("reel_filmstrip")) {
            if (simple) SourceTimeline(s, filmstrip) else OutputTimeline(s, filmstrip)
        }
        if (e.clips.size == 1) RangeSlider(
            value = e.trimStartMs.toFloat()..e.trimEndMs.toFloat(),
            onValueChange = { r -> s.live(s.edit.trim(r.start.toLong(), r.endInclusive.toLong())) },
            onValueChangeFinished = { s.commit() },
            valueRange = 0f..e.durationOf(e.clips[0].source).coerceAtLeast(1).toFloat(), modifier = Modifier.testTag("reel_trim_range"),
            colors = SliderDefaults.colors(thumbColor = StitchGold, activeTrackColor = editorAccent(), inactiveTrackColor = stitchLine())
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            EditorChip("Split at ${formatClock(s.playheadMs)}", false, Modifier.testTag("reel_split"), icon = Icons.Default.ContentCut) {
                val n = s.edit.splitAt(s.playheadMs); if (n != s.edit) s.commit(n)
            }
            EditorChip("Freeze", false, Modifier.testTag("reel_freeze"), icon = Icons.Default.AcUnit) {
                val n = s.edit.freezeAt(s.playheadMs); if (n != s.edit) s.commit(n)
            }
            if (e.clips.size > 1 && s.selectedClip in e.clips.indices)
                EditorChip("Delete clip ${s.selectedClip + 1}", false, Modifier.testTag("reel_delete_clip"), icon = Icons.Default.Delete) { s.commit(s.edit.deleteClip(s.selectedClip)); s.selectedClip = -1 }
            if (onAddClip != null) EditorChip("Add clip", false, Modifier.testTag("reel_add_clip"), icon = Icons.Default.AddPhotoAlternate) { s.playing = false; onAddClip() }
            if (onRecordClip != null) EditorChip("Record", false, Modifier.testTag("reel_record_clip"), icon = Icons.Default.Videocam) { s.playing = false; onRecordClip() }
        }
        if (e.clips.size > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()).testTag("reel_clip_strip"), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                e.clips.forEachIndexed { i, c ->
                    if (i > 0) {
                        val tr = c.transition
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(if (tr == ReelTransition.NONE) stitchSurface() else editorAccent())
                            .border(1.dp, StitchGold.copy(alpha = 0.6f), RoundedCornerShape(9.dp))
                            .clickable { s.commit(s.edit.withTransition(i, ReelTransition.entries[(tr.ordinal + 1) % ReelTransition.entries.size])) }
                            .testTag("reel_transition_$i"), contentAlignment = Alignment.Center) {
                            Text(when (tr) { ReelTransition.NONE -> "|"; ReelTransition.FADE -> "F"; ReelTransition.SLIDE -> "S"; ReelTransition.ZOOM -> "Z" },
                                color = if (tr == ReelTransition.NONE) stitchMutedText() else stitchBackground(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    val on = i == s.selectedClip
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) feedMint() else stitchSurface())
                            .border(if (on) 2.dp else 1.dp, if (on) StitchGold else stitchLine(), RoundedCornerShape(12.dp))
                            .clickable { s.selectedClip = if (on) -1 else i }.padding(horizontal = 10.dp, vertical = 6.dp).testTag("reel_clip_$i")
                    ) {
                        Text("${i + 1}${if (c.isFreeze) " · Freeze" else if (e.sources.size > 1) " · " + (e.sources.getOrNull(c.source)?.label?.ifBlank { null } ?: "Video ${c.source + 1}") else ""}",
                            color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(if (c.isFreeze) formatClock(c.freezeMs) else "${formatClock(c.startMs)}–${formatClock(c.endMs)}", color = stitchMutedText(), fontSize = 11.sp)
                    }
                }
            }
            if (s.selectedClip in e.clips.indices) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val i = s.selectedClip
                EditorChip("Move earlier", false, Modifier.testTag("reel_clip_left"), icon = Icons.AutoMirrored.Filled.ArrowBack) { if (i > 0) { s.commit(s.edit.moveClip(i, i - 1)); s.selectedClip = i - 1 } }
                EditorChip("Move later", false, Modifier.testTag("reel_clip_right"), icon = Icons.AutoMirrored.Filled.ArrowForward) { if (i < e.clips.lastIndex) { s.commit(s.edit.moveClip(i, i + 1)); s.selectedClip = i + 1 } }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("All cuts", color = stitchMutedText(), fontSize = 12.sp)
                ReelTransition.entries.forEach { t ->
                    EditorChip(t.label, e.clips.drop(1).all { it.transition == t }, Modifier.testTag("reel_transition_all_${t.name}")) { s.commit(s.edit.allTransitions(t)) }
                }
            }
        }
    }
}

@Composable
private fun SourceTimeline(s: ReelEditorState, filmstrip: List<Bitmap>) {
    val e = s.edit
    Canvas(Modifier.fillMaxSize().pointerInput(e) {
        awaitEachGesture {
            val d = awaitFirstDown(requireUnconsumed = false)
            fun seek(x: Float) {
                val srcMs = (x / size.width).coerceIn(0f, 1f) * e.sourceDurationMs
                var acc = 0L
                for ((i, c) in e.clips.withIndex()) {
                    if (srcMs >= c.startMs && srcMs <= c.endMs) { s.seek(e.outputAtKept(acc + (srcMs - c.startMs).toDouble())); s.selectedClip = i; s.playing = false; return }
                    acc += c.lengthMs
                }
            }
            seek(d.position.x)
            do { val ev = awaitPointerEvent(); ev.changes.firstOrNull()?.let { seek(it.position.x) } } while (ev.changes.any { it.pressed })
        }
    }) {
        drawIntoCanvas { cv ->
            val c = cv.nativeCanvas; val w = size.width; val h = size.height
            if (filmstrip.isNotEmpty()) {
                val cell = w / filmstrip.size
                filmstrip.forEachIndexed { i, b -> c.drawBitmap(b, centerCrop(b.width, b.height, cell / h), RectF(i * cell, 0f, (i + 1) * cell, h), null) }
            }
            val dur = e.sourceDurationMs.coerceAtLeast(1).toFloat()
            val dim = android.graphics.Paint().apply { color = 0xB3000000.toInt() }
            var last = 0f
            e.clips.forEach { cl -> val l = cl.startMs / dur * w; if (l > last) c.drawRect(last, 0f, l, h, dim); last = cl.endMs / dur * w }
            if (last < w) c.drawRect(last, 0f, w, h, dim)
            val gold = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = 6f; color = 0xFFD4AF37.toInt() }
            e.clips.forEachIndexed { i, cl ->
                val r = RectF(cl.startMs / dur * w + 3, 3f, cl.endMs / dur * w - 3, h - 3)
                c.drawRoundRect(r, 14f, 14f, gold.apply { color = if (i == s.selectedClip) 0xFFF3D98B.toInt() else 0xFFD4AF37.toInt(); strokeWidth = if (i == s.selectedClip) 9f else 6f })
            }
            s.edit.sourceAt(s.playheadMs)?.let { (_, src) ->
                val x = src / dur * w
                c.drawRect(x - 3f, 0f, x + 3f, h, android.graphics.Paint().apply { color = 0xFFFFFFFF.toInt() })
            }
        }
    }
}

/** Output-time timeline for reordered / multi-source / freeze edits. */
@Composable
private fun OutputTimeline(s: ReelEditorState, filmstrip: List<Bitmap>) {
    val e = s.edit
    Canvas(Modifier.fillMaxSize().pointerInput(e) {
        awaitEachGesture {
            val d = awaitFirstDown(requireUnconsumed = false)
            fun seek(x: Float) { val at = ((x / size.width).coerceIn(0f, 1f) * e.outputMs).toLong(); s.seek(at); s.playing = false; e.segmentAt(at)?.let { s.selectedClip = it.clip } }
            seek(d.position.x)
            do { val ev = awaitPointerEvent(); ev.changes.firstOrNull()?.let { seek(it.position.x) } } while (ev.changes.any { it.pressed })
        }
    }) {
        drawIntoCanvas { cv ->
            val c = cv.nativeCanvas; val w = size.width; val h = size.height
            val total = e.outputMs.coerceAtLeast(1).toFloat()
            val tints = intArrayOf(0xFF0B6243.toInt(), 0xFF5B3D8F.toInt(), 0xFF8A5A1F.toInt(), 0xFF1F5A8A.toInt(), 0xFF7A2E3B.toInt())
            val dur0 = e.durationOf(0).coerceAtLeast(1).toFloat()
            e.segments.forEach { sg ->
                val x0 = (sg.outStart / total * w).toFloat(); val x1 = (sg.outEnd / total * w).toFloat()
                if (sg.source == 0 && filmstrip.isNotEmpty()) {
                    val cell = h * 0.6f; var x = x0
                    c.save(); c.clipRect(x0, 0f, x1, h)
                    while (x < x1) {
                        val f = if (sg.freeze) sg.srcStartMs.toFloat() else sg.srcStartMs + (sg.srcEndMs - sg.srcStartMs) * ((x - x0) / (x1 - x0).coerceAtLeast(1f))
                        val b = filmstrip[(f / dur0 * filmstrip.size).toInt().coerceIn(0, filmstrip.lastIndex)]
                        c.drawBitmap(b, centerCrop(b.width, b.height, cell / h), RectF(x, 0f, x + cell, h), null); x += cell
                    }
                    c.restore()
                } else c.drawRect(x0, 0f, x1, h, android.graphics.Paint().apply { color = tints[sg.source % tints.size] })
                if (sg.freeze) c.drawRect(x0, 0f, x1, h, android.graphics.Paint().apply { color = 0x6680C8FF })
            }
            val gold = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE }
            e.clips.indices.forEach { i ->
                val segs = e.segments.filter { it.clip == i }; if (segs.isEmpty()) return@forEach
                val r = RectF((segs.first().outStart / total * w).toFloat() + 3, 3f, (segs.last().outEnd / total * w).toFloat() - 3, h - 3)
                gold.color = if (i == s.selectedClip) 0xFFF3D98B.toInt() else 0xFFD4AF37.toInt(); gold.strokeWidth = if (i == s.selectedClip) 9f else 5f
                c.drawRoundRect(r, 14f, 14f, gold)
            }
            val dia = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF3D98B.toInt() }
            e.cutsOnOutput().forEachIndexed { k, cut ->
                if (e.clips[k + 1].transition == ReelTransition.NONE) return@forEachIndexed
                val x = cut / total * w; val p = android.graphics.Path().apply { moveTo(x, h / 2 - 12); lineTo(x + 12, h / 2); lineTo(x, h / 2 + 12); lineTo(x - 12, h / 2); close() }
                c.drawPath(p, dia)
            }
            val x = s.playheadMs / total * w
            c.drawRect(x - 3f, 0f, x + 3f, h, android.graphics.Paint().apply { color = 0xFFFFFFFF.toInt() })
        }
    }
}

/** Manual timed captions (auto speech-to-text isn't available offline for files on all supported phones). */
@Composable
private fun CaptionsPanel(s: ReelEditorState) {
    val e = s.edit
    var script by remember { mutableStateOf("") }
    val sel = e.captions.firstOrNull { it.id == s.selectedCaption }
    EditorPanel(Modifier.testTag("reel_captions_panel")) {
        EditorLabel("Captions", if (e.captions.isEmpty()) "Typed by you · timed to the reel" else "${e.captions.size} · ${e.captionStyle.label}")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CaptionStyle.entries.forEach { st -> EditorChip(st.label, st == e.captionStyle, Modifier.testTag("reel_caption_style_${st.name}")) { s.commit(s.edit.copy(captionStyle = st)) } }
        }
        if (sel != null) {
            CaptionField(sel.text, "Caption words", "reel_caption_text") { t -> s.live(s.edit.upsertCaption(sel.copy(text = t))) }
            androidx.compose.material3.RangeSlider(
                value = sel.startMs.toFloat()..sel.endMs.toFloat(),
                onValueChange = { r -> s.live(s.edit.upsertCaption(sel.copy(startMs = r.start.toLong(), endMs = maxOf(r.endInclusive.toLong(), r.start.toLong() + 300)))); s.playheadMs = r.start.toLong() },
                onValueChangeFinished = { s.commit() }, valueRange = 0f..e.outputMs.coerceAtLeast(1).toFloat(), modifier = Modifier.testTag("reel_caption_timing"),
                colors = SliderDefaults.colors(thumbColor = StitchGold, activeTrackColor = editorAccent(), inactiveTrackColor = stitchLine())
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorChip("Done", false) { s.commit(); s.selectedCaption = null }
                EditorChip("Delete", false, Modifier.testTag("reel_caption_delete"), icon = Icons.Default.Delete) { s.commit(s.edit.removeCaption(sel.id)); s.selectedCaption = null }
            }
        } else {
            CaptionField(script, "Type or paste what is said — we split it into timed lines", "reel_caption_script") { script = it }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorChip("Add at ${formatClock(s.playheadMs)}", false, Modifier.testTag("reel_caption_add"), icon = Icons.Default.Add) {
                    val st = s.playheadMs.coerceAtMost((e.outputMs - 1000).coerceAtLeast(0)); val id = s.newId("c")
                    s.commit(s.edit.upsertCaption(Caption(id, script.trim().ifBlank { "Bismillah" }.take(90), st, (st + 2500).coerceAtMost(e.outputMs)))); s.selectedCaption = id; script = ""
                }
                if (script.isNotBlank()) EditorChip("Spread over reel", false, Modifier.testTag("reel_caption_split"), icon = Icons.Default.AutoAwesome) {
                    val base = s.newId("c")
                    var n = s.edit.copy(captions = emptyList())
                    Captions.fromTranscript(script, 0, e.outputMs, { "${base}_$it" }).forEach { n = n.upsertCaption(it) }
                    s.commit(n); script = ""
                }
            }
            if (e.captions.isNotEmpty()) Column(Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                e.captions.forEach { c ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(12.dp))
                        .clickable { s.selectedCaption = c.id; s.seek(c.startMs) }.padding(horizontal = 10.dp, vertical = 7.dp).testTag("reel_caption_${c.id}"), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatClock(c.startMs), color = editorAccent(), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                        Text(c.text, color = stitchText(), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionField(value: String, hint: String, tag: String, onValue: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(stitchSurface()).border(1.dp, StitchGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 9.dp)) {
        if (value.isEmpty()) Text(hint, color = stitchMutedText(), fontSize = 13.sp)
        androidx.compose.foundation.text.BasicTextField(value, { onValue(it.take(600)) }, textStyle = androidx.compose.ui.text.TextStyle(color = stitchText(), fontSize = 13.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(StitchGold), modifier = Modifier.fillMaxWidth().heightIn(max = 80.dp).testTag(tag))
    }
}

/** Voiceover: record over the playhead, per-take volume, delete. */
@Composable
private fun VoicePanel(s: ReelEditorState, voice: VoiceControl?) {
    val e = s.edit
    EditorPanel(Modifier.testTag("reel_voice_panel")) {
        EditorLabel("Voiceover", if (voice?.recording == true) "Recording · ${formatClock(voice.elapsedMs)}" else "Starts at ${formatClock(s.playheadMs)}")
        Row(verticalAlignment = Alignment.CenterVertically) {
            val rec = voice?.recording == true
            Box(Modifier.size(56.dp).clip(CircleShape).background(if (rec) Color(0xFFC0392B) else editorAccent()).border(3.dp, StitchGold, CircleShape)
                .clickable(enabled = voice != null) { if (rec) voice!!.onStop() else { s.playing = true; voice!!.onStart() } }.testTag("reel_voice_record"), contentAlignment = Alignment.Center) {
                Icon(if (rec) Icons.Default.Stop else Icons.Default.Mic, if (rec) "Stop" else "Record voiceover", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (rec) LinearProgressIndicator(progress = { voice!!.level.coerceIn(0f, 1f) }, color = StitchGold, trackColor = stitchLine(), modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                else EditorHint(if (voice == null) "Voiceover isn't available here." else "Tap to record while the reel plays. Your voice stays on your phone until you post.")
            }
        }
        if (e.voices.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            e.voices.forEachIndexed { i, v ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(12.dp)).padding(start = 10.dp, end = 2.dp)
                    .testTag("reel_voice_$i"), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, null, tint = StitchGold, modifier = Modifier.size(16.dp))
                    Text(" ${formatClock(v.startMs)} · ${formatClock(v.durationMs)}", color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(98.dp))
                    EditorSlider(v.volume, 0f..1.5f, { s.live(s.edit.upsertVoice(v.copy(volume = it))) }, { s.commit() }, Modifier.weight(1f))
                    EditorIcon(Icons.Default.Delete, "Delete take", true, { s.commit(s.edit.removeVoice(v.id)) })
                }
            }
        }
    }
}

/** Shows which text / sticker layers are on screen when, on the output timeline. */
@Composable
private fun LayerLanes(e: ReelEdit, s: ReelEditorState) {
    if (e.layers.isEmpty()) return
    Column(Modifier.testTag("reel_layer_lanes"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val total = e.outputMs.coerceAtLeast(1).toFloat()
        e.layers.forEach { l ->
            val start = l.startMs / total; val end = (if (l.endMs == Long.MAX_VALUE) e.outputMs else l.endMs).coerceAtMost(e.outputMs) / total
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l.label.ifBlank { l.sticker?.label ?: "Text" }, color = stitchText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(92.dp))
                BoxWithConstraints(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(stitchSoftSurface()).clickable { s.selected = l.id }) {
                    Box(Modifier.offset(x = maxWidth * start).width(maxWidth * (end - start).coerceAtLeast(0.02f)).fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp)).background(if (l.id == s.selected) StitchGold else editorAccent().copy(alpha = 0.7f)))
                }
            }
        }
    }
}

@Composable
private fun AudioPanel(s: ReelEditorState, sounds: List<ReelSound>) {
    val e = s.edit
    EditorPanel(Modifier.testTag("reel_audio_panel")) {
        EditorLabel("Original sound", if (e.muted) "Muted" else "${(e.originalVolume * 100).toInt()}%")
        Row(verticalAlignment = Alignment.CenterVertically) {
            EditorIcon(if (e.muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, if (e.muted) "Unmute" else "Mute", true,
                { s.commit(s.edit.copy(originalVolume = if (e.muted) 1f else 0f)) }, Modifier.testTag("reel_mute"))
            EditorSlider(e.originalVolume, 0f..1f, { s.live(s.edit.copy(originalVolume = it)) }, { s.commit() }, Modifier.weight(1f))
        }
        EditorLabel("Al-Noor sounds", "Licensed for reuse · credited")
        Column(Modifier.heightIn(max = 168.dp).verticalScrollCompat(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (sounds.isEmpty()) EditorHint("No reusable Al-Noor tracks loaded yet.")
            sounds.forEach { snd ->
                val on = e.sound?.trackId == snd.trackId
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) feedMint() else stitchSurface())
                        .border(1.dp, if (on) StitchGold else stitchLine(), RoundedCornerShape(14.dp))
                        .clickable { s.commit(s.edit.copy(sound = if (on) null else snd)) }.padding(horizontal = 12.dp, vertical = 9.dp).testTag("reel_sound_${snd.trackId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(StitchEmeraldDeep), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = StitchGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(snd.title, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = LibreCaslon)
                        Text("${snd.artist} · ${snd.licenseLabel}", color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(if (on) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (on) editorAccent() else stitchLine())
                }
            }
        }
        if (e.sound != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sound", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(50.dp))
                EditorSlider(e.soundVolume, 0f..1f, { s.live(s.edit.copy(soundVolume = it)) }, { s.commit() }, Modifier.weight(1f))
                Text("${(e.soundVolume * 100).toInt()}%", color = editorAccent(), fontSize = 12.sp, modifier = Modifier.width(40.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fade in", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(56.dp))
                EditorSlider(e.soundFadeInMs.toFloat(), 0f..3000f, { s.live(s.edit.copy(soundFadeInMs = it.toLong())) }, { s.commit() }, Modifier.weight(1f).testTag("reel_fade_in"))
                Text("Out", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp).width(28.dp))
                EditorSlider(e.soundFadeOutMs.toFloat(), 0f..3000f, { s.live(s.edit.copy(soundFadeOutMs = it.toLong())) }, { s.commit() }, Modifier.weight(1f).testTag("reel_fade_out"))
            }
            EditorHint("Credit on your reel: Sound: ${e.sound.credit}", Modifier.testTag("reel_sound_credit"))
        }
    }
}

@Composable
private fun Modifier.verticalScrollCompat(): Modifier = this.then(Modifier.verticalScroll(rememberScrollState()))

@Composable
private fun ExportOverlay(progress: Float, frame: Bitmap?, e: ReelEdit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).testTag("reel_export"), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(28.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(stitchBackground())
                .border(1.dp, StitchGold.copy(alpha = 0.6f), RoundedCornerShape(26.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(width = 84.dp, height = 140.dp).clip(RoundedCornerShape(14.dp)).background(StitchEmeraldDeep).border(1.5.dp, StitchGold, RoundedCornerShape(14.dp))) {
                if (frame != null) {
                    val paint = remember(e.filter, e.strength, e.adjust) { e.matrix.paint() }
                    Canvas(Modifier.fillMaxSize()) { drawIntoCanvas { it.nativeCanvas.drawBitmap(frame, centerCrop(frame.width, frame.height, size.width / size.height), RectF(0f, 0f, size.width, size.height), paint) } }
                }
            }
            Text("Preparing your reel", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Applying trim, speed, filter, text and sound on your phone — nothing is uploaded until you post.",
                color = stitchMutedText(), fontSize = 12.5.sp, lineHeight = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) }, color = editorAccent(), trackColor = stitchLine(),
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).testTag("reel_export_progress")
            )
            Text("${(progress * 100).toInt()}% · ${formatClock(e.outputMs)} reel", color = editorAccent(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Cancel", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onCancel).padding(horizontal = 14.dp, vertical = 6.dp))
        }
    }
}
