package com.noorpro.app.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon

/*
 * 1.1.3 editor pro — shared panels for the photo + reel editors: text looks / alignment / animation,
 * layers (reorder, lock, delete), templates, Islamic content pickers (ayah, du'a, hadith from the app's own
 * offline data), card editing (incl. poll / quiz when enabled), HSL, curves, blur, snapping + haptics.
 */

/** One quote for a card sticker (ayah / du'a / hadith). */
data class QuoteItem(val text: String, val ref: String, val arabic: String = "")

/**
 * Where the Islamic stickers get their content. The composer host wires the app's offline Quran, du'a and
 * hadith data; tests pass fixed lists. All lookups are offline.
 */
class StickerSources(
    val ayahs: (String) -> List<QuoteItem> = { emptyList() },
    val duas: (String) -> List<QuoteItem> = { emptyList() },
    val hadith: (String) -> List<QuoteItem> = { emptyList() },
    val hijri: () -> String = { "" },
    val prayer: () -> String = { "" },
    val countdown: (CountdownKind) -> String = { it.label },
    val handle: String = ""
)

// ------------------------------------------------------------------ text looks

@Composable
internal fun TextLookRow(layer: EditorLayer, onChange: (EditorLayer) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).testTag("editor_text_looks"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextLook.entries.forEach { lk -> EditorChip(lk.label, lk == layer.look, Modifier.testTag("editor_look_${lk.name}")) { onChange(layer.copy(look = lk)) } }
        Spacer(Modifier.width(4.dp))
        TextAlignment.entries.forEach { a ->
            val icon = when (a) { TextAlignment.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft; TextAlignment.CENTER -> Icons.Default.FormatAlignCenter; TextAlignment.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight }
            Box(Modifier.size(36.dp).clip(CircleShape).background(if (a == layer.align) feedMint() else Color.Transparent).clickable { onChange(layer.copy(align = a)) }.testTag("editor_align_${a.name}"), contentAlignment = Alignment.Center) {
                Icon(icon, a.label, tint = if (a == layer.align) editorAccent() else stitchMutedText(), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun TextAnimRow(layer: EditorLayer, onChange: (EditorLayer) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).testTag("editor_text_anims"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Animate", color = stitchMutedText(), fontSize = 12.sp)
        TextAnim.entries.forEach { an -> EditorChip(an.label, an == layer.anim, Modifier.testTag("editor_anim_${an.name}")) { onChange(layer.copy(anim = an)) } }
    }
}

// ------------------------------------------------------------------ layers

@Composable
internal fun LayersPanel(
    layers: List<EditorLayer>, selected: String?, onSelect: (String) -> Unit,
    onMove: (String, Int) -> Unit, onLock: (String) -> Unit, onDelete: (String) -> Unit
) {
    EditorPanel(Modifier.testTag("editor_layers_panel")) {
        EditorLabel("Layers", "${layers.size} · top first")
        if (layers.isEmpty()) { EditorHint("Text, stickers and cards you add show here. Reorder, lock or delete them."); return@EditorPanel }
        Column(Modifier.heightIn(max = 210.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            layers.asReversed().forEachIndexed { i, l ->
                val on = l.id == selected
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) feedMint() else stitchSurface())
                        .border(1.dp, if (on) StitchGold else stitchLine(), RoundedCornerShape(14.dp)).clickable { onSelect(l.id) }
                        .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp).testTag("editor_layer_row_${l.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(StitchEmeraldDeep), contentAlignment = Alignment.Center) {
                        Text(if (l.isText) "T" else l.sticker!!.label.take(1), color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l.label.replace('\n', ' ').ifBlank { "Text" }, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(if (l.isText) "Text" else l.sticker!!.label, l.look.takeIf { it != TextLook.PLAIN }?.label, l.anim.takeIf { it != TextAnim.NONE }?.label, if (l.locked) "Locked" else null).joinToString(" · "),
                            color = stitchMutedText(), fontSize = 11.sp, maxLines = 1)
                    }
                    EditorIcon(Icons.Default.ArrowUpward, "Bring forward", i > 0, { onMove(l.id, 1) }, Modifier.testTag("editor_layer_up_${l.id}"))
                    EditorIcon(Icons.Default.ArrowDownward, "Send backward", i < layers.lastIndex, { onMove(l.id, -1) }, Modifier.testTag("editor_layer_down_${l.id}"))
                    EditorIcon(if (l.locked) Icons.Default.Lock else Icons.Default.LockOpen, if (l.locked) "Unlock" else "Lock", true, { onLock(l.id) }, Modifier.testTag("editor_layer_lock_${l.id}"))
                    EditorIcon(Icons.Default.Delete, "Delete layer", !l.locked, { onDelete(l.id) }, Modifier.testTag("editor_layer_delete_${l.id}"))
                }
            }
        }
    }
}

// ------------------------------------------------------------------ templates

@Composable
internal fun TemplatesPanel(onPick: (EditorTemplate) -> Unit) {
    EditorPanel(Modifier.testTag("editor_templates")) {
        EditorLabel("Templates", "Tap, then edit the words")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EditorTemplate.entries.forEach { t ->
                Column(
                    Modifier.width(132.dp).clip(RoundedCornerShape(16.dp)).background(stitchSurface()).border(1.dp, StitchGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onPick(t) }.padding(10.dp).testTag("editor_template_${t.name}")
                ) {
                    Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(10.dp)).background(StitchEmeraldDeep), contentAlignment = Alignment.Center) {
                        Text(t.label, color = StitchGold, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(t.note, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Islamic pickers + card editing

/** Search + list for ayah / du'a / hadith cards. */
@Composable
internal fun QuotePicker(kind: StickerKind, sources: StickerSources, onPick: (QuoteItem) -> Unit, onClose: () -> Unit) {
    var q by remember(kind) { mutableStateOf("") }
    val items = remember(kind, q) {
        runCatching { when (kind) { StickerKind.AYAH -> sources.ayahs(q); StickerKind.DUA -> sources.duas(q); else -> sources.hadith(q) } }.getOrDefault(emptyList()).take(30)
    }
    EditorPanel(Modifier.testTag("editor_quote_picker")) {
        EditorLabel(when (kind) { StickerKind.AYAH -> "Ayah"; StickerKind.DUA -> "Du'a"; else -> "Hadith" }, "Offline · from NoorPro")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(stitchSurface()).border(1.dp, StitchGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = stitchMutedText(), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) Text(if (kind == StickerKind.AYAH) "Search words or 2:255" else "Search words", color = stitchMutedText(), fontSize = 14.sp)
                    BasicTextField(q, { q = it.take(60) }, singleLine = true, textStyle = TextStyle(color = stitchText(), fontSize = 14.sp), cursorBrush = SolidColor(StitchGold), modifier = Modifier.fillMaxWidth().testTag("editor_quote_search"))
                }
            }
            Spacer(Modifier.width(6.dp))
            EditorChip("Back", false, onClick = onClose)
        }
        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (items.isEmpty()) EditorHint("Nothing found. Try another word.")
            items.forEachIndexed { i, it ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(14.dp))
                    .clickable { onPick(it) }.padding(horizontal = 12.dp, vertical = 9.dp).testTag("editor_quote_$i")) {
                    if (it.arabic.isNotBlank()) Text(it.arabic, color = stitchText(), fontFamily = NaskhFamily, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(it.text, color = stitchText(), fontSize = 12.5.sp, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(it.ref, color = editorAccent(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun CountdownPicker(sources: StickerSources, onPick: (CountdownKind, String) -> Unit, onClose: () -> Unit) {
    EditorPanel(Modifier.testTag("editor_countdown_picker")) {
        EditorLabel("Countdown", "Hijri calendar · offline")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CountdownKind.entries.forEach { k ->
                val label = remember(k) { runCatching { sources.countdown(k) }.getOrDefault(k.label) }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(14.dp))
                    .clickable { onPick(k, label) }.padding(horizontal = 12.dp, vertical = 10.dp).testTag("editor_countdown_${k.name}"), verticalAlignment = Alignment.CenterVertically) {
                    Text(k.label, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(label, color = editorAccent(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        EditorChip("Back", false, onClick = onClose)
    }
}

/** Edit a selected card / info sticker: main words, reference, poll / quiz options. */
@Composable
internal fun CardEditPanel(layer: EditorLayer, onChange: (EditorLayer, Boolean) -> Unit, onDelete: () -> Unit) {
    val k = layer.sticker ?: return
    EditorPanel(Modifier.testTag("editor_card_panel")) {
        EditorLabel(k.label, if (k.interactive) "Story only" else null)
        @Composable fun field(value: String, hint: String, tag: String, max: Int, onValue: (String) -> Unit) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(stitchSurface()).border(1.dp, StitchGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 9.dp)) {
                if (value.isEmpty()) Text(hint, color = stitchMutedText(), fontSize = 13.sp)
                BasicTextField(value, { onValue(it.take(max)) }, textStyle = TextStyle(color = stitchText(), fontSize = 13.sp), cursorBrush = SolidColor(StitchGold), modifier = Modifier.fillMaxWidth().testTag(tag))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { field(layer.text, if (k.interactive) "Ask something" else "Words", "editor_card_text", 320) { onChange(layer.copy(text = it), false) } }
            EditorIcon(Icons.Default.Delete, "Delete sticker", !layer.locked, onDelete)
        }
        if (k.card && !k.interactive) field(layer.sub, "Reference (e.g. Al-Baqarah 2:286)", "editor_card_sub", 48) { onChange(layer.copy(sub = it), false) }
        if (k == StickerKind.POLL || k == StickerKind.QUIZ) {
            val opts = layer.options.ifEmpty { listOf("", "") }
            opts.forEachIndexed { i, o ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (k == StickerKind.QUIZ) Box(Modifier.size(28.dp).clip(CircleShape).background(if (layer.answer == i) editorAccent() else stitchSurface()).border(1.dp, StitchGold, CircleShape)
                        .clickable { onChange(layer.copy(answer = i), true) }.testTag("editor_quiz_answer_$i"), contentAlignment = Alignment.Center) {
                        Text("ABCD"[i].toString(), color = if (layer.answer == i) stitchBackground() else stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.weight(1f)) { field(o, "Option ${i + 1}", "editor_option_$i", 28) { v -> onChange(layer.copy(options = opts.mapIndexed { j, x -> if (j == i) v else x }), false) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (opts.size < 4) EditorChip("Add option", false, Modifier.testTag("editor_option_add")) { onChange(layer.copy(options = opts + ""), true) }
                if (opts.size > 2) EditorChip("Remove last", false) { onChange(layer.copy(options = opts.dropLast(1), answer = layer.answer.takeIf { it < opts.size - 1 } ?: -1), true) }
            }
        }
    }
}

// ------------------------------------------------------------------ HSL + curves + blur

@Composable
internal fun HslPanel(hsl: HslAdjust, band: HueBand, onBand: (HueBand) -> Unit, onLive: (HslAdjust) -> Unit, onCommit: () -> Unit) {
    Column(Modifier.testTag("editor_hsl_panel"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HueBand.entries.forEach { b ->
                val on = b == band
                Box(Modifier.size(30.dp).clip(CircleShape).border(if (on) 2.5.dp else 1.dp, if (on) StitchGold else stitchLine(), CircleShape).padding(if (on) 4.dp else 2.dp)
                    .clip(CircleShape).background(Color(b.swatch)).clickable { onBand(b) }.testTag("editor_hsl_${b.name}")) {
                    if (!hsl.of(b).isZero) Box(Modifier.align(Alignment.Center).size(6.dp).clip(CircleShape).background(Color.White))
                }
            }
        }
        val sh = hsl.of(band)
        listOf("Hue" to sh.hue, "Saturation" to sh.sat, "Luminance" to sh.lum).forEachIndexed { i, (name, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(76.dp))
                EditorSlider(v, -1f..1f, { x -> onLive(hsl.with(band, when (i) { 0 -> sh.copy(hue = x); 1 -> sh.copy(sat = x); else -> sh.copy(lum = x) })) }, onCommit, Modifier.weight(1f).testTag("editor_hsl_slider_$i"))
                Text("${(v * 100).toInt()}", color = editorAccent(), fontSize = 12.sp, modifier = Modifier.width(32.dp))
            }
        }
    }
}

/** Tone curve with five draggable points (shadows → highlights) + presets. */
@Composable
internal fun CurvesPanel(curve: ToneCurve, onLive: (ToneCurve) -> Unit, onCommit: () -> Unit) {
    val accent = editorAccent(); val line = stitchLine()
    Column(Modifier.testTag("editor_curves_panel"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToneCurve.presets.forEach { (name, c) -> EditorChip(name, c == curve, Modifier.testTag("editor_curve_$name")) { onLive(c); onCommit() } }
        }
        val latest by rememberUpdatedState(curve)
        Canvas(
            Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(12.dp)).background(stitchSurface()).border(1.dp, line, RoundedCornerShape(12.dp)).testTag("editor_curve_canvas")
                .pointerInput(Unit) {
                    var idx = -1
                    detectDragGestures(
                        onDragStart = { p -> idx = ((p.x / size.width) * 4f).let { kotlin.math.round(it).toInt() }.coerceIn(0, 4) },
                        onDragEnd = { onCommit() },
                        onDrag = { ch, _ -> if (idx >= 0) onLive(latest.with(idx, 1f - (ch.position.y / size.height))) }
                    )
                }
        ) {
            drawIntoCanvas { cv ->
                val c = cv.nativeCanvas; val w = size.width; val h = size.height
                val grid = Paint().apply { color = android.graphics.Color.argb(50, 128, 128, 128); strokeWidth = 1.5f }
                for (i in 1..3) { c.drawLine(w * i / 4, 0f, w * i / 4, h, grid); c.drawLine(0f, h * i / 4, w, h * i / 4, grid) }
                val lut = curve.lut()
                val path = android.graphics.Path()
                for (x in 0..255) { val px = x / 255f * w; val py = h - lut[x] / 255f * h; if (x == 0) path.moveTo(px, py) else path.lineTo(px, py) }
                c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = android.graphics.Color.argb(255, (accent.red * 255).toInt(), (accent.green * 255).toInt(), (accent.blue * 255).toInt()) })
                curve.points.forEachIndexed { i, y -> c.drawCircle(w * i / 4f, h - y * h, 11f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD4AF37.toInt() }) }
            }
        }
    }
}

enum class BlurMode(val label: String) { OFF("Off"), TILT("Tilt-shift"), BRUSH("Blur brush") }

@Composable
internal fun BlurPanel(
    e: PhotoEdit, mode: BlurMode, brushWidth: Float,
    onMode: (BlurMode) -> Unit, onBrushWidth: (Float) -> Unit, onLive: (PhotoEdit) -> Unit, onCommit: (PhotoEdit) -> Unit
) {
    EditorPanel(Modifier.testTag("editor_blur_panel")) {
        EditorLabel("Blur", when (mode) { BlurMode.OFF -> "Off"; BlurMode.TILT -> "Drag the sharp band"; BlurMode.BRUSH -> "Paint where to blur" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BlurMode.entries.forEach { m ->
                EditorChip(m.label, m == mode, Modifier.testTag("editor_blur_${m.name}")) {
                    onMode(m)
                    when (m) {
                        BlurMode.OFF -> onCommit(e.copy(tiltShift = null, blurStrokes = emptyList()))
                        BlurMode.TILT -> if (e.tiltShift == null) onCommit(e.copy(tiltShift = TiltShift()))
                        BlurMode.BRUSH -> Unit
                    }
                }
            }
        }
        if (mode == BlurMode.TILT && e.tiltShift != null) {
            val t = e.tiltShift
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Position", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
                EditorSlider(t.centerY, 0.1f..0.9f, { onLive(e.copy(tiltShift = t.copy(centerY = it))) }, { onCommit(e) }, Modifier.weight(1f).testTag("editor_tilt_pos"))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sharp", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
                EditorSlider(t.band, 0.04f..0.4f, { onLive(e.copy(tiltShift = t.copy(band = it))) }, { onCommit(e) }, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Angle", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
                EditorSlider(t.angleDeg, -45f..45f, { onLive(e.copy(tiltShift = t.copy(angleDeg = it))) }, { onCommit(e) }, Modifier.weight(1f))
            }
        }
        if (mode == BlurMode.BRUSH) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Brush", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
            EditorSlider(brushWidth, 0.03f..0.16f, onBrushWidth, {}, Modifier.weight(1f))
            if (e.blurStrokes.isNotEmpty()) EditorChip("Clear", false) { onCommit(e.copy(blurStrokes = emptyList())) }
        }
        if (mode != BlurMode.OFF) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Amount", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.width(66.dp))
            EditorSlider(e.blurAmount, 0f..1f, { onLive(e.copy(blurAmount = it)) }, { onCommit(e) }, Modifier.weight(1f))
        }
    }
}

// ------------------------------------------------------------------ snapping + haptics

/** Applies snapping to a dragged layer and buzzes once when a guide engages. Returns the guides to draw. */
internal class SnapState {
    var vertical by mutableStateOf(false); var horizontal by mutableStateOf(false)
    private var engaged = false
    fun apply(l: EditorLayer, buzz: () -> Unit): EditorLayer {
        val r = Snap.apply(l)
        vertical = r.vertical; horizontal = r.horizontal
        if (r.any && !engaged) buzz()
        engaged = r.any
        return r.layer
    }
    fun clear() { vertical = false; horizontal = false; engaged = false }
}

@Composable
internal fun rememberSnapBuzz(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
}

// ------------------------------------------------------------------ adding stickers / templates (photo + reel)

/** Kinds that open a content picker first. */
internal val StickerKind.needsPicker get() = this == StickerKind.AYAH || this == StickerKind.DUA || this == StickerKind.HADITH || this == StickerKind.COUNTDOWN

/** A new sticker layer with sensible content and placement (pickers handle AYAH / DUA / HADITH / COUNTDOWN). */
internal fun stickerLayer(k: StickerKind, id: String, labels: Map<StickerKind, String>, sources: StickerSources): EditorLayer {
    val wordy = k.text != null || k.dynamic
    return when {
        k == StickerKind.HIJRI -> EditorLayer(id, sticker = k, text = labels[k] ?: runCatching { sources.hijri() }.getOrDefault("").ifBlank { "Hijri date" }, y = 0.18f)
        k == StickerKind.PRAYER -> EditorLayer(id, sticker = k, text = labels[k] ?: runCatching { sources.prayer() }.getOrDefault("").ifBlank { "Prayer time" }, y = 0.8f)
        k == StickerKind.MENTION -> EditorLayer(id, sticker = k, text = labels[k] ?: sources.handle.takeIf { it.isNotBlank() }?.let { "@" + it.removePrefix("@") } ?: "@someone", y = 0.78f)
        k == StickerKind.POLL -> EditorLayer(id, sticker = k, text = "Which dhikr after Fajr?", options = listOf("SubhanAllah", "Alhamdulillah"), y = 0.6f)
        k == StickerKind.QUIZ -> EditorLayer(id, sticker = k, text = "How many surahs are in the Qur'an?", options = listOf("110", "114", "120"), answer = 1, y = 0.6f)
        k == StickerKind.QUESTION -> EditorLayer(id, sticker = k, text = "Ask me about my Ramadan routine", y = 0.62f)
        k.card -> EditorLayer(id, sticker = k, text = labels[k] ?: k.label, y = 0.5f)
        else -> EditorLayer(id, sticker = k, text = labels[k] ?: k.label, x = if (wordy) 0.5f else 0.76f, y = if (wordy) 0.78f else 0.3f, scale = if (wordy) 1f else 0.8f)
    }
}

internal fun quoteLayer(k: StickerKind, id: String, q: QuoteItem) =
    EditorLayer(id, sticker = k, text = q.text.take(320), sub = q.ref.take(48), y = 0.5f)

internal fun templateData(sources: StickerSources): TemplateData {
    val ayah = runCatching { sources.ayahs("") }.getOrDefault(emptyList()).firstOrNull()
    val dua = runCatching { sources.duas("") }.getOrDefault(emptyList()).firstOrNull()
    val d = TemplateData()
    return d.copy(
        ayah = ayah?.text?.take(220) ?: d.ayah, ayahRef = ayah?.ref ?: d.ayahRef,
        dua = dua?.text?.take(220) ?: d.dua, duaRef = dua?.ref ?: d.duaRef,
        hijri = runCatching { sources.hijri() }.getOrDefault(""),
        countdown = runCatching { sources.countdown(CountdownKind.RAMADAN) }.getOrDefault("")
    )
}
