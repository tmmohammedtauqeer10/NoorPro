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
import androidx.compose.material.icons.filled.AutoAwesome
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

enum class ReelTab(val label: String) { TRIM("Trim"), SPEED("Speed"), FILTERS("Filters"), TEXT("Text"), STICKERS("Stickers"), AUDIO("Sound"), COVER("Cover") }

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
    coverFrame: Bitmap? = null
) {
    val ctx = LocalContext.current
    val fonts = remember { EditorTypefaces.load(ctx) }
    val e = state.edit
    val thumb = remember(frame) { frame?.let { Bitmap.createScaledBitmap(it, 140, (140f * it.height / it.width).toInt().coerceAtLeast(1), true) } }
    Box(modifier.fillMaxSize().background(stitchBackground()).testTag("reel_editor")) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            EditorTopBar(
                title = "Edit reel", subtitle = "${formatClock(e.outputMs)} · ${e.clips.size} clip${if (e.clips.size == 1) "" else "s"} · ${e.speed.label}" +
                    (e.sound?.let { " · ♪ ${it.title}" } ?: ""),
                canUndo = state.canUndo, canRedo = state.canRedo, dirty = state.dirty,
                onClose = onCancel, onUndo = state::undo, onRedo = state::redo, onReset = state::reset,
                onDone = { onDone(state.edit) }, doneLabel = if (ReelExportParams.needsFullExport(e)) "Export" else "Done",
                doneEnabled = state.exportProgress == null
            )
            Box(Modifier.weight(1f).fillMaxWidth().background(editorStage()).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                ReelPreview(state, if (state.tab == ReelTab.COVER) coverFrame ?: frame else frame, fonts)
            }
            when (state.tab) {
                ReelTab.TRIM -> TrimPanel(state, filmstrip)
                ReelTab.SPEED -> EditorPanel(Modifier.testTag("reel_speed_panel")) {
                    EditorLabel("Speed", "${formatClock(e.keptMs)} → ${formatClock(e.outputMs)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReelSpeed.entries.forEach { sp -> EditorChip(sp.label, sp == e.speed, Modifier.weight(1f).testTag("reel_speed_${sp.name}")) { state.commit(e.withSpeed(sp)) } }
                    }
                    EditorHint(if (e.speed == ReelSpeed.HALF) "Slow motion — great for calm recitation moments. Sound is slowed too; pitch is kept." else "Text and stickers stay on the same moments when you change speed.")
                }
                ReelTab.FILTERS -> EditorPanel(Modifier.testTag("reel_filter_panel")) {
                    EditorLabel("Filters", e.filter.note)
                    FilterRow(thumb, e.filter) { f -> state.commit(e.copy(filter = f, strength = if (f == e.filter) e.strength else 1f)) }
                    if (e.filter != NoorFilter.ORIGINAL) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Strength", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
                        EditorSlider(e.strength, 0f..1f, { state.live(state.edit.copy(strength = it)) }, { state.commit() }, Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vignette", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
                        EditorSlider(e.adjust.vignette, 0f..1f, { state.live(state.edit.copy(adjust = state.edit.adjust.copy(vignette = it))) }, { state.commit() }, Modifier.weight(1f))
                    }
                }
                ReelTab.TEXT -> TextToolPanel(
                    layer = state.selectedLayer?.takeIf { it.isText },
                    onAdd = {
                        val start = state.playheadMs.coerceAtMost((e.outputMs - 3000).coerceAtLeast(0))
                        val l = EditorLayer.text(state.newId("t"), "Alhamdulillah", EditorFont.CLASSIC).copy(y = 0.36f, pill = TextPill.SOFT, pillColor = 0xFF0B6243)
                            .timed(start, start + 3000, e.outputMs)
                        state.commit(e.upsert(l)); state.selected = l.id
                    },
                    onChange = { l, commit -> val n = state.edit.copy(layers = state.edit.layers.map { if (it.id == l.id) l else it }); if (commit) state.commit(n) else state.live(n) },
                    onDelete = { l -> state.commit(e.remove(l.id)); state.selected = null }
                ) {
                    state.selectedLayer?.let { l -> LayerTimingRow(l, e.outputMs) { nl, commit -> val n = state.edit.copy(layers = state.edit.layers.map { if (it.id == nl.id) nl else it }); if (commit) state.commit(n) else state.live(n) } }
                    LayerLanes(e, state)
                }
                ReelTab.STICKERS -> Column {
                    state.selectedLayer?.takeIf { !it.isText }?.let { l ->
                        EditorPanel { LayerTimingRow(l, e.outputMs) { nl, commit -> val n = state.edit.copy(layers = state.edit.layers.map { if (it.id == nl.id) nl else it }); if (commit) state.commit(n) else state.live(n) } }
                    }
                    StickerTray(fonts, stickerLabels) { k ->
                        val l = EditorLayer(state.newId("s"), sticker = k, text = stickerLabels[k] ?: k.label, y = if (k.text != null || k.dynamic) 0.72f else 0.25f, x = if (k.text != null || k.dynamic) 0.5f else 0.78f)
                        state.commit(e.upsert(l)); state.selected = l.id
                    }
                }
                ReelTab.AUDIO -> AudioPanel(state, sounds)
                ReelTab.COVER -> EditorPanel(Modifier.testTag("reel_cover_panel")) {
                    EditorLabel("Cover", formatClock(e.coverMs))
                    EditorSlider(e.coverMs.toFloat(), 0f..e.outputMs.coerceAtLeast(1).toFloat(), { state.live(state.edit.copy(coverMs = it.toLong())); state.playheadMs = it.toLong() }, { state.commit() })
                    EditorHint("The frame people see first in the feed and on your profile. Filters, text and stickers show on the cover too.")
                }
            }
            EditorTabBar(
                listOf(
                    EditorTabSpec(ReelTab.TRIM.name, "Trim", Icons.Default.ContentCut), EditorTabSpec(ReelTab.SPEED.name, "Speed", Icons.Default.Speed),
                    EditorTabSpec(ReelTab.FILTERS.name, "Filters", Icons.Default.AutoAwesome), EditorTabSpec(ReelTab.TEXT.name, "Text", Icons.Default.TextFields),
                    EditorTabSpec(ReelTab.STICKERS.name, "Stickers", Icons.Default.EmojiEmotions), EditorTabSpec(ReelTab.AUDIO.name, "Sound", Icons.Default.MusicNote),
                    EditorTabSpec(ReelTab.COVER.name, "Cover", Icons.Default.Image)
                ), state.tab.name
            ) { state.tab = ReelTab.valueOf(it) }
        }
        state.exportProgress?.let { p -> ExportOverlay(p, frame, e, onCancelExport) }
    }
}

@Composable
private fun ReelPreview(s: ReelEditorState, frame: Bitmap?, fonts: EditorTypefaces) {
    val e = s.edit
    val paint = remember(e.filter, e.strength, e.adjust) { e.matrix.paint() }
    val at = if (s.tab == ReelTab.COVER) e.coverMs else s.playheadMs
    Box(
        Modifier.fillMaxHeight().aspectRatio(9f / 16f).clip(RoundedCornerShape(18.dp)).background(StitchEmeraldDeep)
            .border(1.dp, StitchGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).testTag("reel_preview")
    ) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(s.tab) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val hit = LayerPainter.hit(s.edit.layers, down.position.x, down.position.y, size.width, size.height, fonts, at)
                    s.selected = hit?.id
                    if (hit == null) return@awaitEachGesture
                    s.tab = if (hit.isText) ReelTab.TEXT else ReelTab.STICKERS
                    var moved = false
                    do {
                        val ev = awaitPointerEvent()
                        val pan = ev.calculatePan(); val zoom = ev.calculateZoom(); val rot = ev.calculateRotation()
                        if (pan != Offset.Zero || zoom != 1f || rot != 0f) {
                            moved = true
                            s.live(s.edit.copy(layers = s.edit.layers.map { if (it.id == hit.id) it.dragBy(pan.x / size.width, pan.y / size.height).transform(zoom, rot) else it }))
                            ev.changes.forEach { it.consume() }
                        }
                    } while (ev.changes.any { it.pressed })
                    if (moved) s.commit()
                }
            }
        ) {
            drawIntoCanvas { cv ->
                val c = cv.nativeCanvas; val w = size.width; val h = size.height
                if (frame != null) c.drawBitmap(frame, centerCrop(frame.width, frame.height, w / h), RectF(0f, 0f, w, h), paint)
                LayerPainter.drawVignette(c, w.toInt(), h.toInt(), e.adjust.vignette)
                LayerPainter.drawAll(c, e.layers, w.toInt(), h.toInt(), fonts, atMs = at, selected = s.selected)
            }
        }
        Text(
            "${formatClock(at)} / ${formatClock(e.outputMs)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 8.dp, vertical = 3.dp)
        )
        if (e.speed != ReelSpeed.NORMAL || e.muted || e.sound != null) Row(
            Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (e.speed != ReelSpeed.NORMAL) PreviewPill(e.speed.label)
            if (e.muted) PreviewPill("Muted")
            e.sound?.let { PreviewPill("♪ ${it.title}") }
        }
    }
}

@Composable
private fun PreviewPill(text: String) {
    Text(text, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 150.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, StitchGold.copy(alpha = 0.6f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** Filmstrip with kept clips, split markers and the playhead; range trim for single clips; split / delete. */
@Composable
private fun TrimPanel(s: ReelEditorState, filmstrip: List<Bitmap>) {
    val e = s.edit
    EditorPanel(Modifier.testTag("reel_trim_panel")) {
        EditorLabel("Timeline", "${formatClock(e.trimStartMs)} – ${formatClock(e.trimEndMs)} · ${formatClock(e.outputMs)}")
        Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(StitchEmeraldDeep).testTag("reel_filmstrip")) {
            Canvas(Modifier.fillMaxSize().pointerInput(e) {
                awaitEachGesture {
                    val d = awaitFirstDown(requireUnconsumed = false)
                    fun seek(x: Float) {
                        val srcMs = (x / size.width).coerceIn(0f, 1f) * e.sourceDurationMs
                        // map source position to output time if inside a kept clip
                        var acc = 0L
                        for ((i, c) in e.clips.withIndex()) {
                            if (srcMs >= c.startMs && srcMs <= c.endMs) { s.playheadMs = ((acc + (srcMs - c.startMs)) / e.speed.factor).toLong(); s.selectedClip = i; return }
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
                    // dim everything not kept
                    var last = 0f
                    e.clips.forEach { cl ->
                        val l = cl.startMs / dur * w; if (l > last) c.drawRect(last, 0f, l, h, dim); last = cl.endMs / dur * w
                    }
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
        if (e.clips.size == 1) RangeSlider(
            value = e.trimStartMs.toFloat()..e.trimEndMs.toFloat(),
            onValueChange = { r -> s.live(s.edit.trim(r.start.toLong(), r.endInclusive.toLong())) },
            onValueChangeFinished = { s.commit() },
            valueRange = 0f..e.sourceDurationMs.coerceAtLeast(1).toFloat(), modifier = Modifier.testTag("reel_trim_range"),
            colors = SliderDefaults.colors(thumbColor = StitchGold, activeTrackColor = editorAccent(), inactiveTrackColor = stitchLine())
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            EditorChip("Split at ${formatClock(s.playheadMs)}", false, Modifier.testTag("reel_split"), icon = Icons.Default.ContentCut) {
                val n = s.edit.splitAt(s.playheadMs); if (n != s.edit) s.commit(n)
            }
            if (e.clips.size > 1 && s.selectedClip in e.clips.indices)
                EditorChip("Delete clip ${s.selectedClip + 1}", false, Modifier.testTag("reel_delete_clip"), icon = Icons.Default.Delete) { s.commit(s.edit.deleteClip(s.selectedClip)); s.selectedClip = -1 }
        }
        if (e.clips.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            e.clips.forEachIndexed { i, c -> EditorChip("${i + 1} · ${formatClock(c.startMs)}–${formatClock(c.endMs)}", i == s.selectedClip) { s.selectedClip = i } }
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
