package com.noorpro.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.ui.components.*

/*
 * 1.1.3 — photo editor for each photo of a post / story: crop + rotate, NoorPro filters with strength,
 * adjust, text, Islamic stickers, brush, undo / redo / reset. Stateless over [PhotoEditorState];
 * the composer host decodes the photo and renders the result with PhotoRenderer.
 */

enum class PhotoTab(val label: String) { CROP("Crop"), FILTERS("Filters"), ADJUST("Adjust"), TEXT("Text"), STICKERS("Stickers"), DRAW("Draw") }

@Stable
class PhotoEditorState(initial: PhotoEdit = PhotoEdit(), tab: PhotoTab = PhotoTab.FILTERS) {
    private val history = EditHistory(initial)
    var edit by mutableStateOf(initial); private set
    var tab by mutableStateOf(tab)
    var selected by mutableStateOf<String?>(null)
    var adjustKind by mutableStateOf(AdjustKind.BRIGHTNESS)
    var brushColor by mutableStateOf(0xFFD4AF37)
    var brushWidth by mutableStateOf(0.012f)
    var brushGlow by mutableStateOf(true)
    var liveStroke by mutableStateOf<BrushStroke?>(null)
    private var rev by mutableIntStateOf(0)
    private var seq = 0

    val canUndo get() = rev.let { history.canUndo }
    val canRedo get() = rev.let { history.canRedo }
    val dirty get() = rev.let { history.isDirty } || edit != history.current
    val selectedLayer get() = edit.layers.firstOrNull { it.id == selected }

    /** Live change during a gesture / slider drag. */
    fun live(e: PhotoEdit) { edit = e }
    /** Record the current (or given) edit as one undo step. */
    fun commit(e: PhotoEdit = edit) { edit = e; history.commit(e); rev++ }
    fun undo() { edit = history.undo(); rev++; if (selectedLayer == null) selected = null }
    fun redo() { edit = history.redo(); rev++ }
    fun reset() { edit = history.reset(); rev++; selected = null }
    fun newId(prefix: String) = "${prefix}_${System.nanoTime()}_${seq++}"
}

@Composable
fun NoorPhotoEditor(
    state: PhotoEditorState,
    bitmap: Bitmap?,
    onCancel: () -> Unit,
    onDone: (PhotoEdit) -> Unit,
    modifier: Modifier = Modifier,
    positionLabel: String? = null,
    stickerLabels: Map<StickerKind, String> = emptyMap(),
    saving: Boolean = false
) {
    val ctx = LocalContext.current
    val fonts = remember { EditorTypefaces.load(ctx) }
    val thumb = remember(bitmap) { bitmap?.let { Bitmap.createScaledBitmap(it, 160, (160f * it.height / it.width).toInt().coerceAtLeast(1), true) } }
    Column(modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().navigationBarsPadding().testTag("photo_editor")) {
        EditorTopBar(
            title = "Edit photo", subtitle = positionLabel ?: "Crop, filters, text and stickers",
            canUndo = state.canUndo, canRedo = state.canRedo, dirty = state.dirty,
            onClose = onCancel, onUndo = state::undo, onRedo = state::redo, onReset = state::reset,
            onDone = { onDone(state.edit) }, doneLabel = if (saving) "Saving…" else "Done", doneEnabled = !saving && bitmap != null
        )
        Box(Modifier.weight(1f).fillMaxWidth().background(editorStage()).padding(14.dp), contentAlignment = Alignment.Center) {
            if (bitmap != null) PhotoCanvas(bitmap, state, fonts)
            else Text("Opening photo…", color = stitchMutedText(), fontSize = 13.sp)
        }
        when (state.tab) {
            PhotoTab.CROP -> CropPanel(state, bitmap)
            PhotoTab.FILTERS -> EditorPanel(Modifier.testTag("editor_filter_panel")) {
                EditorLabel("Filters", state.edit.filter.note)
                FilterRow(thumb, state.edit.filter) { f -> state.commit(state.edit.copy(filter = f, strength = if (f == state.edit.filter) state.edit.strength else 1f)) }
                if (state.edit.filter != NoorFilter.ORIGINAL) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Strength", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
                    EditorSlider(state.edit.strength, 0f..1f, { state.live(state.edit.copy(strength = it)) }, { state.commit() }, Modifier.weight(1f).testTag("editor_strength"))
                    Text("${(state.edit.strength * 100).toInt()}", color = editorAccent(), fontSize = 12.sp, modifier = Modifier.width(32.dp))
                }
            }
            PhotoTab.ADJUST -> AdjustPanel(state.edit.adjust, state.adjustKind, { state.adjustKind = it }, { a -> state.live(state.edit.copy(adjust = a)) }, { state.commit() })
            PhotoTab.TEXT -> TextToolPanel(
                layer = state.selectedLayer?.takeIf { it.isText },
                onAdd = { val l = EditorLayer.text(state.newId("t"), "SubhanAllah", EditorFont.CLASSIC).copy(y = 0.42f); state.commit(state.edit.upsert(l)); state.selected = l.id },
                onChange = { l, commit -> val e = state.edit.copy(layers = state.edit.layers.map { if (it.id == l.id) l else it }); if (commit) state.commit(e) else state.live(e) },
                onDelete = { l -> state.commit(state.edit.remove(l.id)); state.selected = null }
            )
            PhotoTab.STICKERS -> StickerTray(fonts, stickerLabels) { k ->
                val l = EditorLayer(state.newId("s"), sticker = k, text = stickerLabels[k] ?: k.label, x = if (k.text != null || k.dynamic) 0.5f else 0.76f, y = if (k.text != null || k.dynamic) 0.78f else 0.3f, scale = if (k.text != null || k.dynamic) 1f else 0.8f)
                state.commit(state.edit.upsert(l)); state.selected = l.id
            }
            PhotoTab.DRAW -> EditorPanel(Modifier.testTag("editor_draw_panel")) {
                EditorLabel("Brush", if (state.edit.strokes.isEmpty()) "Draw on the photo" else "${state.edit.strokes.size} stroke${if (state.edit.strokes.size == 1) "" else "s"}")
                ColorDots(state.brushColor) { state.brushColor = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    EditorSlider(state.brushWidth, 0.004f..0.04f, { state.brushWidth = it }, {}, Modifier.weight(1f))
                    Text("Glow", color = stitchMutedText(), fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Switch(state.brushGlow, { state.brushGlow = it }, colors = SwitchDefaults.colors(checkedThumbColor = StitchGold, checkedTrackColor = editorAccent()))
                }
            }
        }
        EditorTabBar(
            listOf(
                EditorTabSpec(PhotoTab.CROP.name, "Crop", Icons.Default.Crop), EditorTabSpec(PhotoTab.FILTERS.name, "Filters", Icons.Default.AutoAwesome),
                EditorTabSpec(PhotoTab.ADJUST.name, "Adjust", Icons.Default.Tune), EditorTabSpec(PhotoTab.TEXT.name, "Text", Icons.Default.TextFields),
                EditorTabSpec(PhotoTab.STICKERS.name, "Stickers", Icons.Default.EmojiEmotions), EditorTabSpec(PhotoTab.DRAW.name, "Draw", Icons.Default.Brush)
            ), state.tab.name
        ) { state.tab = PhotoTab.valueOf(it) }
    }
}

@Composable
internal fun AdjustPanel(a: Adjustments, kind: AdjustKind, onKind: (AdjustKind) -> Unit, onLive: (Adjustments) -> Unit, onCommit: () -> Unit) {
    EditorPanel(Modifier.testTag("editor_adjust_panel")) {
        EditorLabel("Adjust", kind.label)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdjustKind.entries.forEach { k ->
                val v = a.value(k)
                EditorChip(if (v == 0f) k.label else "${k.label} ${if (v > 0) "+" else ""}${(v * 100).toInt()}", k == kind, Modifier.testTag("editor_adjust_${k.name}")) { onKind(k) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            EditorSlider(a.value(kind), kind.min..1f, { onLive(a.with(kind, it)) }, onCommit, Modifier.weight(1f).testTag("editor_adjust_slider"))
            Text("${(a.value(kind) * 100).toInt()}", color = editorAccent(), fontSize = 12.sp, modifier = Modifier.width(36.dp))
        }
    }
}

@Composable
private fun CropPanel(state: PhotoEditorState, bitmap: Bitmap?) {
    EditorPanel(Modifier.testTag("editor_crop_panel")) {
        EditorLabel("Crop & rotate", "Drag to move · pinch to zoom")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CropAspect.entries.forEach { a ->
                EditorChip(a.label, state.edit.aspect == a, Modifier.testTag("editor_aspect_${a.name}")) {
                    if (bitmap != null) state.commit(state.edit.withAspect(a, bitmap.width, bitmap.height))
                }
            }
            Spacer(Modifier.weight(1f))
            EditorIcon(Icons.AutoMirrored.Filled.RotateRight, "Rotate 90°", true, { state.commit(state.edit.rotate()) }, Modifier.testTag("editor_rotate"))
        }
    }
}

/** The image with the live edit applied; gestures depend on the tab (crop / layers / brush). */
@Composable
private fun PhotoCanvas(bitmap: Bitmap, s: PhotoEditorState, fonts: EditorTypefaces) {
    val e = s.edit
    val rotated = remember(bitmap, e.quarterTurns) {
        if (e.quarterTurns == 0) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90f * e.quarterTurns) }, true)
    }
    val cropMode = s.tab == PhotoTab.CROP
    val aspect = if (cropMode) rotated.width.toFloat() / rotated.height else e.crop.pixelAspect(rotated.width, rotated.height)
    val paint = remember(e.filter, e.strength, e.adjust) { e.matrix.paint() }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val maxW = constraints.maxWidth.toFloat(); val maxH = constraints.maxHeight.toFloat()
        val (wPx, hPx) = if (maxW / maxH > aspect) (maxH * aspect) to maxH else maxW to (maxW / aspect)
        val wDp = with(density) { wPx.toDp() }; val hDp = with(density) { hPx.toDp() }
        Canvas(
            Modifier.size(wDp, hDp).clip(RoundedCornerShape(if (cropMode) 4.dp else 16.dp)).testTag("photo_canvas")
                .pointerInput(s.tab, rotated) {
                    when (s.tab) {
                        PhotoTab.CROP -> awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var changed = false
                            do {
                                val ev = awaitPointerEvent()
                                val pan = ev.calculatePan(); val zoom = ev.calculateZoom()
                                if (pan != Offset.Zero || zoom != 1f) {
                                    changed = true
                                    var c = Crop.pan(s.edit.crop, pan.x / size.width, pan.y / size.height)
                                    if (zoom != 1f) c = Crop.zoom(c, zoom)
                                    s.live(s.edit.copy(crop = c)); ev.changes.forEach { it.consume() }
                                }
                            } while (ev.changes.any { it.pressed })
                            if (changed) s.commit()
                        }
                        PhotoTab.DRAW -> awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pts = mutableListOf(down.position.x / size.width to down.position.y / size.height)
                            s.liveStroke = BrushStroke(pts.toList(), s.brushColor, s.brushWidth, s.brushGlow)
                            do {
                                val ev = awaitPointerEvent()
                                ev.changes.firstOrNull()?.let { ch ->
                                    pts += (ch.position.x / size.width).coerceIn(0f, 1f) to (ch.position.y / size.height).coerceIn(0f, 1f); ch.consume()
                                }
                                s.liveStroke = BrushStroke(pts.toList(), s.brushColor, s.brushWidth, s.brushGlow)
                            } while (ev.changes.any { it.pressed })
                            s.liveStroke = null
                            s.commit(s.edit.copy(strokes = s.edit.strokes + BrushStroke(pts.toList(), s.brushColor, s.brushWidth, s.brushGlow)))
                        }
                        else -> awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val hit = LayerPainter.hit(s.edit.layers, down.position.x, down.position.y, size.width, size.height, fonts)
                            s.selected = hit?.id
                            if (hit != null && hit.isText) s.tab = PhotoTab.TEXT
                            if (hit == null) return@awaitEachGesture
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
                }
        ) {
            drawIntoCanvas { cv ->
                val c = cv.nativeCanvas; val w = size.width; val h = size.height
                if (cropMode) {
                    c.drawBitmap(rotated, null, RectF(0f, 0f, w, h), paint)
                    val r = RectF(e.crop.left * w, e.crop.top * h, e.crop.right * w, e.crop.bottom * h)
                    val dim = Paint().apply { color = 0x99000000.toInt() }
                    c.drawRect(0f, 0f, w, r.top, dim); c.drawRect(0f, r.bottom, w, h, dim)
                    c.drawRect(0f, r.top, r.left, r.bottom, dim); c.drawRect(r.right, r.top, w, r.bottom, dim)
                    val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88FFFFFF.toInt(); strokeWidth = 1.5f }
                    for (i in 1..2) {
                        c.drawLine(r.left + r.width() * i / 3, r.top, r.left + r.width() * i / 3, r.bottom, grid)
                        c.drawLine(r.left, r.top + r.height() * i / 3, r.right, r.top + r.height() * i / 3, grid)
                    }
                    c.drawRect(r, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = 0xFFD4AF37.toInt() })
                    val k = minOf(r.width(), r.height()) * 0.12f
                    val corner = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 12f; color = 0xFFF3D98B.toInt(); strokeCap = Paint.Cap.ROUND }
                    listOf(r.left to r.top, r.right to r.top, r.left to r.bottom, r.right to r.bottom).forEach { (x, y) ->
                        val dx = if (x == r.left) k else -k; val dy = if (y == r.top) k else -k
                        c.drawLine(x, y, x + dx, y, corner); c.drawLine(x, y, x, y + dy, corner)
                    }
                } else {
                    val src = android.graphics.Rect(
                        (e.crop.left * rotated.width).toInt(), (e.crop.top * rotated.height).toInt(),
                        (e.crop.right * rotated.width).toInt(), (e.crop.bottom * rotated.height).toInt()
                    )
                    c.drawBitmap(rotated, src, RectF(0f, 0f, w, h), paint)
                    LayerPainter.drawVignette(c, w.toInt(), h.toInt(), e.adjust.vignette)
                    LayerPainter.drawStrokes(c, e.strokes + listOfNotNull(s.liveStroke), w.toInt(), h.toInt())
                    LayerPainter.drawAll(c, e.layers, w.toInt(), h.toInt(), fonts, selected = if (s.tab == PhotoTab.TEXT || s.tab == PhotoTab.STICKERS) s.selected else null)
                }
            }
        }
    }
}
