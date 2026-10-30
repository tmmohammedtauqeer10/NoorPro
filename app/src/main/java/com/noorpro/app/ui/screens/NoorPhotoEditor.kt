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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
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

enum class PhotoTab(val label: String) {
    CROP("Crop"), FILTERS("Filters"), ADJUST("Adjust"), TEXT("Text"), STICKERS("Stickers"), DRAW("Draw"),
    BLUR("Blur"), LAYERS("Layers"), TEMPLATES("Templates")
}

enum class AdjustMode(val label: String) { LIGHT("Light & colour"), HSL("HSL"), CURVES("Curves") }

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
    var adjustMode by mutableStateOf(AdjustMode.LIGHT)
    var hslBand by mutableStateOf(HueBand.ORANGE)
    var blurMode by mutableStateOf(if (initial.blurStrokes.isNotEmpty()) BlurMode.BRUSH else if (initial.tiltShift != null) BlurMode.TILT else BlurMode.OFF)
    var blurWidth by mutableStateOf(0.07f)
    var liveBlur by mutableStateOf<BrushStroke?>(null)
    /** Open content picker in the sticker tab (ayah / du'a / hadith / countdown). */
    var picker by mutableStateOf<StickerKind?>(null)
    internal val snap = SnapState()
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
    saving: Boolean = false,
    stickerSources: StickerSources = StickerSources(),
    interactive: Boolean = false
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
            PhotoTab.ADJUST -> PhotoAdjustPanel(state, bitmap)
            PhotoTab.TEXT -> TextToolPanel(
                layer = state.selectedLayer?.takeIf { it.isText },
                onAdd = { val l = EditorLayer.text(state.newId("t"), "SubhanAllah", EditorFont.CLASSIC).copy(y = 0.42f); state.commit(state.edit.upsert(l)); state.selected = l.id },
                onChange = { l, commit -> val e = state.edit.copy(layers = state.edit.layers.map { if (it.id == l.id) l else it }); if (commit) state.commit(e) else state.live(e) },
                onDelete = { l -> state.commit(state.edit.remove(l.id)); state.selected = null }
            )
            PhotoTab.STICKERS -> {
                val sel = state.selectedLayer?.takeIf { it.sticker?.let { k -> k.card || k.interactive || k.dynamic } == true }
                val pk = state.picker
                when {
                    pk == StickerKind.COUNTDOWN -> CountdownPicker(stickerSources, { _, label ->
                        val l = EditorLayer(state.newId("s"), sticker = StickerKind.COUNTDOWN, text = label, sub = "Prepare your heart", y = 0.5f)
                        state.commit(state.edit.upsert(l)); state.selected = l.id; state.picker = null
                    }) { state.picker = null }
                    pk != null -> QuotePicker(pk, stickerSources, { q ->
                        val l = quoteLayer(pk, state.newId("s"), q); state.commit(state.edit.upsert(l)); state.selected = l.id; state.picker = null
                    }) { state.picker = null }
                    sel != null -> Column {
                        CardEditPanel(sel, { l, commit -> val e = state.edit.copy(layers = state.edit.layers.map { if (it.id == l.id) l else it }); if (commit) state.commit(e) else state.live(e) }) {
                            state.commit(state.edit.remove(sel.id)); state.selected = null
                        }
                        EditorPanel { EditorChip("More stickers", false) { state.commit(); state.selected = null } }
                    }
                    else -> StickerTray(fonts, stickerLabels, interactive) { k ->
                        if (k.needsPicker) state.picker = k
                        else { val l = stickerLayer(k, state.newId("s"), stickerLabels, stickerSources); state.commit(state.edit.upsert(l)); state.selected = l.id }
                    }
                }
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
            PhotoTab.BLUR -> BlurPanel(state.edit, state.blurMode, state.blurWidth, { state.blurMode = it }, { state.blurWidth = it }, { state.live(it) }, { state.commit(it) })
            PhotoTab.LAYERS -> LayersPanel(
                state.edit.layers, state.selected, { state.selected = it },
                { id, d -> state.commit(state.edit.copy(layers = Layers.move(state.edit.layers, id, d))) },
                { id -> state.commit(state.edit.copy(layers = Layers.toggleLock(state.edit.layers, id))) },
                { id -> state.commit(state.edit.remove(id)); if (state.selected == id) state.selected = null }
            )
            PhotoTab.TEMPLATES -> TemplatesPanel { t ->
                val (filter, layers) = t.build(templateData(stickerSources), state::newId)
                var e = state.edit; if (filter != null && e.filter == NoorFilter.ORIGINAL) e = e.copy(filter = filter, strength = 0.85f)
                layers.forEach { e = e.upsert(it) }
                state.commit(e); state.selected = layers.firstOrNull()?.id
            }
        }
        EditorTabBar(
            listOf(
                EditorTabSpec(PhotoTab.CROP.name, "Crop", Icons.Default.Crop), EditorTabSpec(PhotoTab.FILTERS.name, "Filters", Icons.Default.AutoAwesome),
                EditorTabSpec(PhotoTab.ADJUST.name, "Adjust", Icons.Default.Tune), EditorTabSpec(PhotoTab.TEXT.name, "Text", Icons.Default.TextFields),
                EditorTabSpec(PhotoTab.STICKERS.name, "Stickers", Icons.Default.EmojiEmotions), EditorTabSpec(PhotoTab.DRAW.name, "Draw", Icons.Default.Brush),
                EditorTabSpec(PhotoTab.BLUR.name, "Blur", Icons.Default.BlurOn), EditorTabSpec(PhotoTab.LAYERS.name, "Layers", Icons.Default.Layers),
                EditorTabSpec(PhotoTab.TEMPLATES.name, "Templates", Icons.Default.Dashboard)
            ), state.tab.name
        ) { state.tab = PhotoTab.valueOf(it); state.picker = null }
    }
}

@Composable
internal fun AdjustPanel(a: Adjustments, kind: AdjustKind, onKind: (AdjustKind) -> Unit, onLive: (Adjustments) -> Unit, onCommit: () -> Unit, videoOnly: Boolean = false, header: @Composable ColumnScope.() -> Unit = { EditorLabel("Adjust", kind.label) }) {
    EditorPanel(Modifier.testTag("editor_adjust_panel")) {
        header()
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdjustKind.entries.filter { !videoOnly || it.video }.forEach { k ->
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

/** Photo adjust: light & colour sliders, HSL per colour, tone curve and one-tap auto-enhance. */
@Composable
private fun PhotoAdjustPanel(state: PhotoEditorState, bitmap: Bitmap?) {
    val modes = @Composable {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AdjustMode.entries.forEach { m -> EditorChip(m.label, m == state.adjustMode, Modifier.testTag("editor_adjust_mode_${m.name}")) { state.adjustMode = m } }
            EditorChip("Auto", false, Modifier.testTag("editor_auto_enhance"), icon = Icons.Default.AutoFixHigh) {
                if (bitmap != null) state.commit(state.edit.copy(adjust = PhotoRenderer.autoEnhance(bitmap, state.edit.adjust)))
            }
        }
    }
    when (state.adjustMode) {
        AdjustMode.LIGHT -> AdjustPanel(state.edit.adjust, state.adjustKind, { state.adjustKind = it }, { a -> state.live(state.edit.copy(adjust = a)) }, { state.commit() }) { modes() }
        AdjustMode.HSL -> EditorPanel(Modifier.testTag("editor_adjust_panel")) {
            modes()
            HslPanel(state.edit.hsl, state.hslBand, { state.hslBand = it }, { state.live(state.edit.copy(hsl = it)) }, { state.commit() })
        }
        AdjustMode.CURVES -> EditorPanel(Modifier.testTag("editor_adjust_panel")) {
            modes()
            CurvesPanel(state.edit.curve, { state.live(state.edit.copy(curve = it)) }, { state.commit() })
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
    // Pixel pass (curves / HSL / highlights / shadows / sharpen / grain) on a preview-sized copy.
    val preview = remember(rotated) {
        val long = maxOf(rotated.width, rotated.height)
        if (long <= 900) rotated else Bitmap.createScaledBitmap(rotated, (rotated.width * 900f / long).toInt().coerceAtLeast(1), (rotated.height * 900f / long).toInt().coerceAtLeast(1), true)
    }
    val img = remember(preview, e.adjust, e.curve, e.hsl) { if (e.needsPixels) PhotoRenderer.pixels(preview, e) else rotated }
    val blurred = remember(img, e.blurAmount, e.hasBlur || s.tab == PhotoTab.BLUR) { if (e.hasBlur || s.tab == PhotoTab.BLUR) BlurTools.blur(img, e.blurAmount) else null }
    val cropMode = s.tab == PhotoTab.CROP
    val aspect = if (cropMode) rotated.width.toFloat() / rotated.height else e.crop.pixelAspect(rotated.width, rotated.height)
    val paint = remember(e.filter, e.strength, e.adjust) { e.matrix.paint() }
    val buzz = rememberSnapBuzz()
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val maxW = constraints.maxWidth.toFloat(); val maxH = constraints.maxHeight.toFloat()
        val (wPx, hPx) = if (maxW / maxH > aspect) (maxH * aspect) to maxH else maxW to (maxW / aspect)
        val wDp = with(density) { wPx.toDp() }; val hDp = with(density) { hPx.toDp() }
        val mask = remember(wPx.toInt(), hPx.toInt(), e.tiltShift, e.blurStrokes, s.liveBlur) {
            if (e.tiltShift != null || e.blurStrokes.isNotEmpty() || s.liveBlur != null) BlurTools.mask(wPx.toInt(), hPx.toInt(), e.tiltShift, e.blurStrokes + listOfNotNull(s.liveBlur)) else null
        }
        Canvas(
            Modifier.size(wDp, hDp).clip(RoundedCornerShape(if (cropMode) 4.dp else 16.dp)).testTag("photo_canvas")
                .pointerInput(s.tab, rotated, s.blurMode) {
                    when {
                        s.tab == PhotoTab.CROP -> awaitEachGesture {
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
                        s.tab == PhotoTab.DRAW || (s.tab == PhotoTab.BLUR && s.blurMode == BlurMode.BRUSH) -> awaitEachGesture {
                            val blur = s.tab == PhotoTab.BLUR
                            fun stroke(pts: List<Pair<Float, Float>>) = if (blur) BrushStroke(pts, 0xFFFFFFFF, s.blurWidth, false) else BrushStroke(pts, s.brushColor, s.brushWidth, s.brushGlow)
                            fun show(st: BrushStroke?) { if (blur) s.liveBlur = st else s.liveStroke = st }
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pts = mutableListOf(down.position.x / size.width to down.position.y / size.height)
                            show(stroke(pts.toList()))
                            do {
                                val ev = awaitPointerEvent()
                                ev.changes.firstOrNull()?.let { ch ->
                                    pts += (ch.position.x / size.width).coerceIn(0f, 1f) to (ch.position.y / size.height).coerceIn(0f, 1f); ch.consume()
                                }
                                show(stroke(pts.toList()))
                            } while (ev.changes.any { it.pressed })
                            show(null)
                            s.commit(if (blur) s.edit.copy(blurStrokes = s.edit.blurStrokes + stroke(pts.toList())) else s.edit.copy(strokes = s.edit.strokes + stroke(pts.toList())))
                        }
                        s.tab == PhotoTab.BLUR -> awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var moved = false
                            do {
                                val ev = awaitPointerEvent()
                                val pan = ev.calculatePan()
                                val t = s.edit.tiltShift
                                if (t != null && pan != Offset.Zero) {
                                    moved = true; s.live(s.edit.copy(tiltShift = t.copy(centerY = (t.centerY + pan.y / size.height).coerceIn(0.05f, 0.95f)))); ev.changes.forEach { it.consume() }
                                }
                            } while (ev.changes.any { it.pressed })
                            if (moved) s.commit()
                        }
                        else -> awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val hit = LayerPainter.hit(s.edit.layers, down.position.x, down.position.y, size.width, size.height, fonts)
                            s.selected = hit?.id
                            if (hit != null && hit.isText && s.tab != PhotoTab.LAYERS) s.tab = PhotoTab.TEXT
                            if (hit != null && !hit.isText && (hit.sticker?.card == true || hit.sticker?.dynamic == true) && s.tab != PhotoTab.LAYERS) s.tab = PhotoTab.STICKERS
                            if (hit == null || hit.locked) return@awaitEachGesture
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
                }
        ) {
            drawIntoCanvas { cv ->
                val c = cv.nativeCanvas; val w = size.width; val h = size.height
                if (cropMode) {
                    c.drawBitmap(img, null, RectF(0f, 0f, w, h), paint)
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
                        (e.crop.left * img.width).toInt(), (e.crop.top * img.height).toInt(),
                        (e.crop.right * img.width).toInt(), (e.crop.bottom * img.height).toInt()
                    )
                    val dst = RectF(0f, 0f, w, h)
                    c.drawBitmap(img, src, dst, paint)
                    if (blurred != null && mask != null) BlurTools.drawBlurred(c, blurred, src, dst, mask, paint)
                    if (s.tab == PhotoTab.BLUR && e.tiltShift != null) {
                        val t = e.tiltShift; val cy = t.centerY * h
                        val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCF3D98B.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE; pathEffect = android.graphics.DashPathEffect(floatArrayOf(14f, 10f), 0f) }
                        c.save(); c.rotate(t.angleDeg, w / 2, cy)
                        c.drawLine(-w, cy - t.band * h, 2 * w, cy - t.band * h, guide); c.drawLine(-w, cy + t.band * h, 2 * w, cy + t.band * h, guide)
                        c.restore()
                    }
                    LayerPainter.drawVignette(c, w.toInt(), h.toInt(), e.adjust.vignette)
                    LayerPainter.drawStrokes(c, e.strokes + listOfNotNull(s.liveStroke), w.toInt(), h.toInt())
                    val sel = if (s.tab == PhotoTab.TEXT || s.tab == PhotoTab.STICKERS || s.tab == PhotoTab.LAYERS) s.selected else null
                    LayerPainter.drawAll(c, e.layers, w.toInt(), h.toInt(), fonts, selected = sel)
                    LayerPainter.drawGuides(c, w.toInt(), h.toInt(), s.snap.vertical, s.snap.horizontal)
                }
            }
        }
    }
}
