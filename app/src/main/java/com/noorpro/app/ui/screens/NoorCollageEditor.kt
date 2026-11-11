package com.noorpro.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.ui.components.*

/*
 * 1.1.3 editor pro — layout / collage: 2–6 photos in a NoorPro frame. Tap a cell to cycle which photo it shows.
 * The result is one image; edit it further (filters, text, stickers) in the photo editor.
 */
@Stable
class CollageEditorState(initial: CollageEdit) {
    private val history = EditHistory(initial)
    var edit by mutableStateOf(initial); private set
    private var rev by mutableIntStateOf(0)
    val canUndo get() = rev.let { history.canUndo }
    val canRedo get() = rev.let { history.canRedo }
    val dirty get() = rev.let { history.isDirty } || edit != history.current
    fun live(e: CollageEdit) { edit = e }
    fun commit(e: CollageEdit = edit) { edit = e; history.commit(e); rev++ }
    fun undo() { edit = history.undo(); rev++ }
    fun redo() { edit = history.redo(); rev++ }
    fun reset() { edit = history.reset(); rev++ }
}

private val collageBackgrounds = listOf(0xFFFDF6E3, 0xFFFFFFFF, 0xFF0B6243, 0xFF06231A, 0xFFD4AF37, 0xFF111111)

@Composable
fun NoorCollageEditor(state: CollageEditorState, photos: List<Bitmap>, onCancel: () -> Unit, onDone: (CollageEdit) -> Unit, saving: Boolean = false, modifier: Modifier = Modifier) {
    val e = state.edit
    Box(modifier.fillMaxSize().background(stitchBackground()).testTag("collage_editor")) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            EditorTopBar("Layout", "${photos.size} photos · ${e.layout.label}", state.canUndo, state.canRedo, state.dirty,
                onCancel, state::undo, state::redo, state::reset, { onDone(state.edit) }, doneEnabled = !saving && photos.isNotEmpty())
            Box(Modifier.weight(1f).fillMaxWidth().background(editorStage()).padding(16.dp), contentAlignment = Alignment.Center) {
                val ratio = e.aspect.ratio ?: (4f / 5f)
                Canvas(
                    Modifier.fillMaxHeight().aspectRatio(ratio, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(10.dp)).testTag("collage_canvas")
                        .pointerInput(e, photos.size) {
                            detectTapGestures { p ->
                                val rects = e.rects(size.width, size.height)
                                val i = rects.indexOfFirst { p.x in it[0]..it[2] && p.y in it[1]..it[3] }
                                if (i >= 0 && photos.size > 1) state.commit(e.swap(i, photos.size))
                            }
                        }
                ) {
                    drawIntoCanvas { cv ->
                        val c = cv.nativeCanvas; val w = size.width.toInt(); val h = size.height.toInt()
                        val bmp = PhotoRenderer.collage(photos, e, maxOf(w, h))
                        c.drawBitmap(bmp, null, android.graphics.RectF(0f, 0f, size.width, size.height), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                    }
                }
            }
            EditorPanel(Modifier.testTag("collage_panel")) {
                EditorLabel("Layout", "Tap a photo to swap it")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CollageLayout.forCount(photos.size).forEach { l ->
                        val on = l == e.layout
                        Column(Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) feedMint() else stitchSurface())
                            .border(if (on) 2.dp else 1.dp, if (on) StitchGold else stitchLine(), RoundedCornerShape(12.dp))
                            .clickable { state.commit(e.copy(layout = l, order = emptyList())) }.padding(8.dp).testTag("collage_layout_${l.name}"),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Canvas(Modifier.size(width = 36.dp, height = 45.dp)) {
                                l.cells.forEach { cell ->
                                    drawRoundRect(StitchGold.copy(alpha = 0.85f), topLeft = androidx.compose.ui.geometry.Offset(cell[0] * size.width + 2, cell[1] * size.height + 2),
                                        size = androidx.compose.ui.geometry.Size((cell[2] - cell[0]) * size.width - 4, (cell[3] - cell[1]) * size.height - 4),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
                                }
                            }
                            Text(l.label, color = stitchText(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CropAspect.PORTRAIT, CropAspect.SQUARE, CropAspect.WIDE).forEach { a -> EditorChip(a.label, a == e.aspect, Modifier.testTag("collage_aspect_${a.name}")) { state.commit(e.copy(aspect = a)) } }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Spacing", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(60.dp))
                    EditorSlider(e.gap, 0f..0.05f, { state.live(state.edit.copy(gap = it)) }, { state.commit() }, Modifier.weight(1f))
                    Text("Corners", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp).width(54.dp))
                    EditorSlider(e.radius, 0f..0.08f, { state.live(state.edit.copy(radius = it)) }, { state.commit() }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    collageBackgrounds.forEach { c ->
                        val on = c == e.background
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(999.dp)).border(if (on) 2.5.dp else 1.dp, if (on) StitchGold else stitchLine(), RoundedCornerShape(999.dp))
                            .padding(if (on) 4.dp else 2.dp).clip(RoundedCornerShape(999.dp)).background(Color(c)).clickable { state.commit(e.copy(background = c)) }.testTag("collage_bg_${java.lang.Long.toHexString(c)}"))
                    }
                }
                EditorHint("The collage replaces the photos it uses. You can still add filters, text and stickers to it after.")
            }
        }
    }
}
