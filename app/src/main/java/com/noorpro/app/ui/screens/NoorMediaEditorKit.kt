package com.noorpro.app.ui.screens

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.R
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.theme.PlusJakarta

/*
 * 1.1.3 — shared pieces for the photo and reel editors (NoorPro look: cream / deep green, gold hairlines,
 * Caslon titles). Everything here is stateless; the editors own the state.
 */

internal val NaskhFamily = FontFamily(Font(R.font.noto_naskh_arabic))

internal fun EditorFont.family(): FontFamily = when (this) {
    EditorFont.CLASSIC -> LibreCaslon; EditorFont.MODERN, EditorFont.STRONG -> PlusJakarta; EditorFont.NASKH -> NaskhFamily
}
internal fun EditorFont.weight(): FontWeight = when (this) { EditorFont.STRONG -> FontWeight.ExtraBold; EditorFont.CLASSIC -> FontWeight.Bold; else -> FontWeight.SemiBold }

/** Stage behind the media: warm parchment in light, near-black green in dark. */
@Composable internal fun editorStage(): Color = if (isStitchLight()) Color(0xFFEDE4CF) else Color(0xFF07110D)
@Composable internal fun editorAccent(): Color = if (isStitchLight()) StitchEmerald else Color(0xFF7FD3AE)

@Composable
internal fun EditorTopBar(
    title: String, subtitle: String?, canUndo: Boolean, canRedo: Boolean, dirty: Boolean,
    onClose: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, onReset: () -> Unit, onDone: () -> Unit,
    doneLabel: String = "Done", doneEnabled: Boolean = true
) {
    Row(
        Modifier.fillMaxWidth().background(stitchBackground()).padding(horizontal = 8.dp, vertical = 8.dp).testTag("editor_topbar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorIcon(Icons.Default.Close, "Close editor", true, onClose)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
            if (subtitle != null) Text(subtitle, color = stitchMutedText(), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        EditorIcon(Icons.AutoMirrored.Filled.Undo, "Undo", canUndo, onUndo, Modifier.testTag("editor_undo"))
        EditorIcon(Icons.AutoMirrored.Filled.Redo, "Redo", canRedo, onRedo, Modifier.testTag("editor_redo"))
        EditorIcon(Icons.Default.RestartAlt, "Reset all edits", dirty, onReset, Modifier.testTag("editor_reset"))
        Spacer(Modifier.width(6.dp))
        Text(
            doneLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(999.dp))
                .background(if (doneEnabled) (if (isStitchLight()) StitchEmerald else Color(0xFF1F7A5A)) else stitchLine())
                .clickable(enabled = doneEnabled, onClick = onDone).padding(horizontal = 16.dp, vertical = 9.dp).testTag("editor_done")
        )
    }
}

@Composable
internal fun EditorIcon(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(40.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = stitchText().copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(22.dp))
    }
}

data class EditorTabSpec(val key: String, val label: String, val icon: ImageVector)

@Composable
internal fun EditorTabBar(tabs: List<EditorTabSpec>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(stitchSurface())
            .padding(horizontal = 6.dp, vertical = 6.dp).testTag("editor_tabs"),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEach { t ->
            val on = t.key == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) feedMint() else Color.Transparent)
                    .clickable { onSelect(t.key) }.padding(horizontal = 2.dp, vertical = 7.dp).testTag("editor_tab_${t.key}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(t.icon, null, tint = if (on) editorAccent() else stitchMutedText(), modifier = Modifier.size(21.dp))
                Spacer(Modifier.height(3.dp))
                Text(t.label, color = if (on) editorAccent() else stitchMutedText(), fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
                Box(Modifier.padding(top = 4.dp).size(width = 16.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(if (on) StitchGold else Color.Transparent))
            }
        }
    }
}

@Composable
internal fun EditorPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(stitchBackground()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content
    )
}

@Composable
internal fun EditorLabel(text: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 14.dp, height = 2.dp).background(StitchGold))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), color = stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = editorAccent(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun EditorChip(label: String, selected: Boolean, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(999.dp)).background(if (selected) editorAccent() else stitchSurface())
            .border(1.dp, if (selected) Color.Transparent else StitchGold.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, tint = if (selected) stitchBackground() else editorAccent(), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)) }
        Text(label, color = if (selected) stitchBackground() else stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun EditorSlider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Slider(
        value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, onValueChangeFinished = onDone, valueRange = range, modifier = modifier,
        colors = SliderDefaults.colors(thumbColor = StitchGold, activeTrackColor = editorAccent(), inactiveTrackColor = stitchLine())
    )
}

/** Paint for a colour matrix (null when it is the identity). */
internal fun ColorMatrix5.paint(): Paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
    if (!isIdentity()) colorFilter = ColorMatrixColorFilter(toAndroid())
}

/** Row of filter swatches rendered from the real image. */
@Composable
internal fun FilterRow(thumb: Bitmap?, selected: NoorFilter, onPick: (NoorFilter) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).testTag("editor_filters"), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NoorFilter.entries.forEach { f ->
            val on = f == selected
            Column(Modifier.width(64.dp).clickable { onPick(f) }.testTag("editor_filter_${f.name}"), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(width = 60.dp, height = 74.dp).clip(RoundedCornerShape(14.dp)).background(StitchEmeraldDeep)
                        .border(if (on) 2.5.dp else 1.dp, if (on) StitchGold else stitchLine(), RoundedCornerShape(14.dp))
                ) {
                    if (thumb != null) {
                        val paint = remember(f) { f.matrix().paint() }
                        Canvas(Modifier.fillMaxSize()) {
                            drawIntoCanvas { c ->
                                val src = centerCrop(thumb.width, thumb.height, size.width / size.height)
                                c.nativeCanvas.drawBitmap(thumb, src, RectF(0f, 0f, size.width, size.height), paint)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(f.label, color = if (on) editorAccent() else stitchText(), fontSize = 11.5.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = if (on) LibreCaslon else PlusJakarta, maxLines = 1)
            }
        }
    }
}

internal fun centerCrop(w: Int, h: Int, aspect: Float): android.graphics.Rect {
    val img = w.toFloat() / h
    return if (img > aspect) { val cw = (h * aspect).toInt(); val x = (w - cw) / 2; android.graphics.Rect(x, 0, x + cw, h) }
    else { val ch = (w / aspect).toInt(); val y = (h - ch) / 2; android.graphics.Rect(0, y, w, y + ch) }
}

/** Text tool: edit field, font, colour, pill. Works on the selected text layer. */
@Composable
internal fun TextToolPanel(
    layer: EditorLayer?,
    onAdd: () -> Unit,
    onChange: (EditorLayer, commit: Boolean) -> Unit,
    onDelete: (EditorLayer) -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    EditorPanel(Modifier.testTag("editor_text_panel")) {
        if (layer == null || !layer.isText) {
            EditorLabel("Text")
            Text("Add a line, a verse reference or a du'a. Drag to move, pinch to size and turn. Arabic uses the Naskh font automatically.",
                color = stitchMutedText(), fontSize = 12.5.sp, lineHeight = 18.sp)
            EditorChip("Add text", true, icon = Icons.Default.TextFields, modifier = Modifier.testTag("editor_add_text"), onClick = onAdd)
            return@EditorPanel
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = layer.text, onValueChange = { v -> onChange(layer.copy(text = v.take(EditorLayer.MAX_TEXT), font = if (ArabicText.has(v) && !ArabicText.has(layer.text)) EditorFont.NASKH else layer.font), false) },
                textStyle = TextStyle(color = stitchText(), fontSize = 15.sp, fontFamily = layer.font.family(), fontWeight = layer.font.weight()),
                cursorBrush = SolidColor(StitchGold),
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(stitchSurface())
                    .border(1.dp, StitchGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 11.dp).testTag("editor_text_field")
            )
            EditorIcon(Icons.Default.Delete, "Delete text", true, { onDelete(layer) })
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorFont.entries.forEach { f ->
                val on = f == layer.font
                Text(
                    if (f.arabic) "نور  ${f.label}" else "Aa  ${f.label}", fontFamily = f.family(), fontWeight = f.weight(), fontSize = 14.sp,
                    color = if (on) stitchBackground() else stitchText(),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) editorAccent() else stitchSurface())
                        .border(1.dp, if (on) Color.Transparent else stitchLine(), RoundedCornerShape(12.dp))
                        .clickable { onChange(layer.copy(font = f), true) }.padding(horizontal = 12.dp, vertical = 7.dp).testTag("editor_font_${f.name}")
                )
            }
        }
        ColorDots(layer.color) { onChange(layer.copy(color = it), true) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextPill.entries.forEach { p -> EditorChip(p.label, p == layer.pill) { onChange(layer.copy(pill = p, pillColor = if (p != TextPill.NONE && layer.color == layer.pillColor) 0xFF111111 else layer.pillColor), true) } }
            if (layer.pill != TextPill.NONE) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0xFF0B6243, 0xFF111111, 0xFFD4AF37, 0xFFFDF6E3).forEach { c ->
                    Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(Color(c))
                        .border(if (c == layer.pillColor) 2.dp else 1.dp, if (c == layer.pillColor) StitchGold else stitchLine(), RoundedCornerShape(7.dp))
                        .clickable { onChange(layer.copy(pillColor = c, color = if (layer.color == c) EditorPalette.onColor(c) else layer.color), true) })
                }
            }
        }
        extra()
    }
}

@Composable
internal fun ColorDots(selected: Long, onPick: (Long) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).testTag("editor_colors"), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        EditorPalette.colors.forEach { c ->
            val on = c == selected
            Box(
                Modifier.size(30.dp).clip(CircleShape).border(if (on) 2.5.dp else 1.dp, if (on) StitchGold else stitchLine(), CircleShape)
                    .padding(if (on) 4.dp else 2.dp).clip(CircleShape).background(Color(c)).clickable { onPick(c) }
            )
        }
    }
}

/** Sticker tray: original Islamic art + word badges + location / time / hashtag. */
@Composable
internal fun StickerTray(fonts: EditorTypefaces, labels: Map<StickerKind, String>, onPick: (StickerKind) -> Unit) {
    EditorPanel(Modifier.testTag("editor_stickers")) {
        EditorLabel("Stickers", "Made for NoorPro")
        val rows = StickerKind.entries.chunked(5)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    val preview = remember(k, labels[k]) { EditorLayer("p_$k", sticker = k, text = labels[k] ?: k.label, scale = 1f) }
                    Box(
                        Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(14.dp)).background(stitchSurface())
                            .border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).clickable { onPick(k) }.testTag("editor_sticker_${k.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                            drawIntoCanvas { c ->
                                val w = size.width.toInt(); val h = size.height.toInt()
                                val u = LayerPainter.unit(w, h)
                                val b = LayerPainter.localBounds(preview, u, fonts)
                                val k2 = minOf(size.width * 0.92f / b.width(), size.height * 0.84f / b.height(), 2.2f)
                                LayerPainter.draw(c.nativeCanvas, preview.copy(scale = k2), w, h, fonts)
                            }
                        }
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Small timing bar for a layer on the reel timeline. */
@Composable
internal fun LayerTimingRow(layer: EditorLayer, totalMs: Long, onChange: (EditorLayer, Boolean) -> Unit) {
    val end = if (layer.endMs == Long.MAX_VALUE) totalMs else layer.endMs.coerceAtMost(totalMs)
    EditorLabel("On screen", "${com.noorpro.app.data.formatClock(layer.startMs)} – ${com.noorpro.app.data.formatClock(end)}")
    androidx.compose.material3.RangeSlider(
        value = layer.startMs.toFloat()..end.toFloat(),
        onValueChange = { r -> onChange(layer.timed(r.start.toLong(), r.endInclusive.toLong(), totalMs), false) },
        onValueChangeFinished = { onChange(layer, true) },
        valueRange = 0f..totalMs.coerceAtLeast(1).toFloat(), modifier = Modifier.testTag("editor_layer_timing"),
        colors = SliderDefaults.colors(thumbColor = StitchGold, activeTrackColor = editorAccent(), inactiveTrackColor = stitchLine())
    )
}

@Composable
internal fun EditorHint(text: String, modifier: Modifier = Modifier) {
    Text(text, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Start, modifier = modifier)
}

internal fun Modifier.goldFrame(radius: Int = 20) = this.border(BorderStroke(1.dp, StitchGold.copy(alpha = 0.55f)), RoundedCornerShape(radius.dp))

@Suppress("unused") internal fun Modifier.dim(on: Boolean) = if (on) alpha(0.4f) else this
