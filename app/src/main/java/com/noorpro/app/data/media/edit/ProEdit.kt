package com.noorpro.app.data.media.edit

import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * 1.1.3 "editor pro" — pure maths for the extra photo tools (tone curve, HSL per colour, highlights /
 * shadows, sharpen, grain, auto-enhance), collage layouts, templates and the Islamic date stickers.
 * No Android types so it is unit-tested on the JVM; PhotoRenderer applies it to real bitmaps.
 */

// ---------------------------------------------------------------- tone curve

/** RGB tone curve: output levels at inputs 0, ¼, ½, ¾, 1 (monotone cubic between them). */
data class ToneCurve(val points: List<Float> = LINEAR) {
    init { require(points.size == 5) }
    val isIdentity get() = points.indices.all { abs(points[it] - LINEAR[it]) < 1e-4f }
    fun with(i: Int, y: Float) = copy(points = points.mapIndexed { k, v -> if (k == i) y.coerceIn(0f, 1f) else v })

    /** 256-entry lookup (0..255 → 0..255), Fritsch–Carlson so the curve never overshoots. */
    fun lut(): IntArray {
        val xs = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f); val ys = points.toFloatArray()
        val n = 5; val d = FloatArray(n - 1) { (ys[it + 1] - ys[it]) / (xs[it + 1] - xs[it]) }
        val m = FloatArray(n)
        m[0] = d[0]; m[n - 1] = d[n - 2]
        for (i in 1 until n - 1) m[i] = if (d[i - 1] * d[i] <= 0f) 0f else (d[i - 1] + d[i]) / 2f
        for (i in 0 until n - 1) {
            if (d[i] == 0f) { m[i] = 0f; m[i + 1] = 0f; continue }
            val a = m[i] / d[i]; val b = m[i + 1] / d[i]; val s = a * a + b * b
            if (s > 9f) { val t = 3f / kotlin.math.sqrt(s); m[i] = t * a * d[i]; m[i + 1] = t * b * d[i] }
        }
        return IntArray(256) { v ->
            val x = v / 255f
            val k = min(3, (x / 0.25f).toInt())
            val h = xs[k + 1] - xs[k]; val t = (x - xs[k]) / h
            val t2 = t * t; val t3 = t2 * t
            val y = (2 * t3 - 3 * t2 + 1) * ys[k] + (t3 - 2 * t2 + t) * h * m[k] + (-2 * t3 + 3 * t2) * ys[k + 1] + (t3 - t2) * h * m[k + 1]
            (y.coerceIn(0f, 1f) * 255f).roundToInt()
        }
    }

    companion object {
        val LINEAR = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        val presets = linkedMapOf(
            "Linear" to ToneCurve(),
            "Soft S" to ToneCurve(listOf(0f, 0.2f, 0.5f, 0.8f, 1f)),
            "Matte" to ToneCurve(listOf(0.1f, 0.3f, 0.52f, 0.74f, 0.94f)),
            "Lift" to ToneCurve(listOf(0.04f, 0.32f, 0.58f, 0.8f, 1f)),
            "Deep" to ToneCurve(listOf(0f, 0.17f, 0.44f, 0.72f, 0.98f))
        )
    }
}

// ---------------------------------------------------------------- HSL per colour

enum class HueBand(val label: String, val centerDeg: Float, val swatch: Long) {
    RED("Red", 0f, 0xFFD9453B), ORANGE("Orange", 30f, 0xFFE8892E), YELLOW("Yellow", 58f, 0xFFE9C83A), GREEN("Green", 120f, 0xFF3FA055),
    AQUA("Aqua", 180f, 0xFF35B3B0), BLUE("Blue", 220f, 0xFF2F6BD0), PURPLE("Purple", 275f, 0xFF7E4FC4), MAGENTA("Magenta", 320f, 0xFFC94FA4)
}

/** Each in -1..1: hue ±30°, saturation ×(1±1), lightness ±0.25. */
data class HslShift(val hue: Float = 0f, val sat: Float = 0f, val lum: Float = 0f) { val isZero get() = hue == 0f && sat == 0f && lum == 0f }

data class HslAdjust(val bands: Map<HueBand, HslShift> = emptyMap()) {
    val isNeutral get() = bands.values.all { it.isZero }
    fun of(b: HueBand) = bands[b] ?: HslShift()
    fun with(b: HueBand, s: HslShift) = copy(bands = (bands + (b to s)).filterValues { !it.isZero })

    /** rgb in 0..1 → adjusted rgb. Weights fall off linearly 45° either side of a band's centre. */
    fun apply(r: Float, g: Float, b: Float, out: FloatArray = FloatArray(3)): FloatArray {
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val l = (mx + mn) / 2f
        if (mx - mn < 1e-4f || isNeutral) { out[0] = r; out[1] = g; out[2] = b; return out }
        val dlt = mx - mn
        val s = if (l > 0.5f) dlt / (2f - mx - mn) else dlt / (mx + mn)
        var h = when (mx) { r -> ((g - b) / dlt) % 6f; g -> (b - r) / dlt + 2f; else -> (r - g) / dlt + 4f } * 60f
        if (h < 0) h += 360f
        var dh = 0f; var ds = 0f; var dl = 0f
        for ((band, sh) in bands) {
            var dist = abs(h - band.centerDeg); if (dist > 180f) dist = 360f - dist
            val w = (1f - dist / 45f).coerceAtLeast(0f)
            if (w > 0f) { dh += sh.hue * 30f * w; ds += sh.sat * w; dl += sh.lum * 0.25f * w }
        }
        val weight = min(1f, s * 2.5f) // grey-ish pixels move less
        h = (h + dh * weight + 360f) % 360f
        val ns = (s * (1f + ds * weight)).coerceIn(0f, 1f)
        val nl = (l + dl * weight * ns).coerceIn(0f, 1f)
        return hslToRgb(h, ns, nl, out)
    }

    companion object {
        fun hslToRgb(h: Float, s: Float, l: Float, out: FloatArray = FloatArray(3)): FloatArray {
            val c = (1f - abs(2f * l - 1f)) * s; val x = c * (1f - abs((h / 60f) % 2f - 1f)); val m = l - c / 2f
            val (r, g, b) = when ((h / 60f).toInt() % 6) { 0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x); 3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x) }
            out[0] = (r + m).coerceIn(0f, 1f); out[1] = (g + m).coerceIn(0f, 1f); out[2] = (b + m).coerceIn(0f, 1f); return out
        }
    }
}

// ---------------------------------------------------------------- blur tools

/** Linear tilt-shift: sharp band around [centerY] (0..1), [band] = half height of the sharp zone, then fades to blurred. */
data class TiltShift(val centerY: Float = 0.5f, val band: Float = 0.16f, val feather: Float = 0.18f, val angleDeg: Float = 0f) {
    /** 0 = sharp, 1 = fully blurred at a normalised y (angle ignored; for tests / mask building). */
    fun blurAt(y: Float): Float { val d = abs(y - centerY) - band; return (d / feather.coerceAtLeast(0.01f)).coerceIn(0f, 1f) }
}

// ---------------------------------------------------------------- per-pixel pass

object ProPixels {
    /** Highlights / shadows tone curve combined with the RGB curve into one lookup. */
    fun toneLut(a: Adjustments, curve: ToneCurve): IntArray {
        val c = if (curve.isIdentity) null else curve.lut()
        return IntArray(256) { v ->
            val x = v / 255f
            val shadowW = 1f - smooth(0f, 0.55f, x)
            val highW = smooth(0.45f, 1f, x)
            val y = (x + 0.28f * a.shadows * shadowW * (1f - x) + 0.28f * a.highlights * highW * (if (a.highlights > 0) 1f - x else x)).coerceIn(0f, 1f)
            val i = (y * 255f).roundToInt()
            c?.get(i) ?: i
        }
    }

    private fun smooth(e0: Float, e1: Float, x: Float): Float { val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f); return t * t * (3 - 2 * t) }

    /** In-place on ARGB pixels: tone LUT, HSL, sharpen, grain. Alpha is kept. */
    fun process(px: IntArray, w: Int, h: Int, a: Adjustments, curve: ToneCurve, hsl: HslAdjust, seed: Int = 7) {
        val lut = toneLut(a, curve)
        val identityLut = lut.indices.all { lut[it] == it }
        val tmp = FloatArray(3)
        if (!identityLut || !hsl.isNeutral) for (i in px.indices) {
            val p = px[i]
            var r = lut[(p shr 16) and 0xFF]; var g = lut[(p shr 8) and 0xFF]; var b = lut[p and 0xFF]
            if (!hsl.isNeutral) {
                hsl.apply(r / 255f, g / 255f, b / 255f, tmp)
                r = (tmp[0] * 255f).roundToInt(); g = (tmp[1] * 255f).roundToInt(); b = (tmp[2] * 255f).roundToInt()
            }
            px[i] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
        }
        if (a.sharpen > 0f && w >= 3 && h >= 3) sharpen(px, w, h, a.sharpen)
        if (a.grain > 0f) grain(px, a.grain, seed)
    }

    /** Unsharp mask with a 3×3 box blur. */
    fun sharpen(px: IntArray, w: Int, h: Int, amount: Float) {
        val src = px.copyOf(); val k = amount.coerceIn(0f, 1f) * 1.4f
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            var sr = 0; var sg = 0; var sb = 0
            for (dy in -1..1) for (dx in -1..1) { val q = src[i + dy * w + dx]; sr += (q shr 16) and 0xFF; sg += (q shr 8) and 0xFF; sb += q and 0xFF }
            val p = src[i]
            fun ch(v: Int, blur: Int) = (v + k * (v - blur / 9f)).roundToInt().coerceIn(0, 255)
            px[i] = (p and -0x1000000) or (ch((p shr 16) and 0xFF, sr) shl 16) or (ch((p shr 8) and 0xFF, sg) shl 8) or ch(p and 0xFF, sb)
        }
    }

    /** Deterministic monochrome film grain (same photo + seed → same grain). */
    fun grain(px: IntArray, amount: Float, seed: Int) {
        val amp = amount.coerceIn(0f, 1f) * 34f
        var s = seed * 1103515245 + 12345
        for (i in px.indices) {
            s = s * 1664525 + 1013904223
            val n = (((s ushr 8) and 0xFFFF) / 65535f - 0.5f) * 2f * amp
            val p = px[i]
            val r = (((p shr 16) and 0xFF) + n).roundToInt().coerceIn(0, 255)
            val g = (((p shr 8) and 0xFF) + n).roundToInt().coerceIn(0, 255)
            val b = ((p and 0xFF) + n).roundToInt().coerceIn(0, 255)
            px[i] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
        }
    }
}

/** One-tap auto-enhance from the photo's own histogram (no ML). Keeps the user's vignette / creative sliders. */
object AutoEnhance {
    data class Stats(val p02: Float, val p10: Float, val p90: Float, val p98: Float, val mean: Float, val meanSat: Float)

    fun stats(px: IntArray, step: Int = 1): Stats {
        val hist = IntArray(256); var satSum = 0f; var n = 0
        var i = 0
        while (i < px.size) {
            val p = px[i]; val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
            hist[((r * 54 + g * 183 + b * 19) shr 8).coerceIn(0, 255)]++
            val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); if (mx > 0) satSum += (mx - mn) / mx.toFloat()
            n++; i += step
        }
        if (n == 0) return Stats(0f, 0.1f, 0.9f, 1f, 0.5f, 0.3f)
        fun pct(q: Float): Float { var acc = 0; val target = q * n; for (v in 0..255) { acc += hist[v]; if (acc >= target) return v / 255f }; return 1f }
        var mean = 0f; for (v in 0..255) mean += v * hist[v]
        return Stats(pct(0.02f), pct(0.10f), pct(0.90f), pct(0.98f), mean / n / 255f, satSum / n)
    }

    fun suggest(s: Stats, base: Adjustments = Adjustments()): Adjustments {
        val range = s.p98 - s.p02
        val contrast = ((0.82f - range) * 0.9f).coerceIn(-0.15f, 0.45f)
        val brightness = ((0.5f - s.mean) * 0.7f).coerceIn(-0.25f, 0.3f)
        val shadows = if (s.p10 < 0.14f) ((0.14f - s.p10) * 3f).coerceIn(0f, 0.45f) else 0f
        val highlights = if (s.p90 > 0.9f) (-(s.p90 - 0.9f) * 4f).coerceIn(-0.45f, 0f) else 0f
        val saturation = if (s.meanSat < 0.22f) 0.22f else if (s.meanSat < 0.35f) 0.1f else 0f
        fun r2(v: Float) = (v * 100f).roundToInt() / 100f
        return base.copy(brightness = r2(brightness), contrast = r2(contrast), shadows = r2(shadows), highlights = r2(highlights), saturation = r2(saturation), sharpen = max(base.sharpen, 0.15f))
    }
}

// ---------------------------------------------------------------- collage

enum class CollageLayout(val label: String, val count: Int, val cells: List<FloatArray>) {
    DUO_SIDE("Side by side", 2, listOf(floatArrayOf(0f, 0f, .5f, 1f), floatArrayOf(.5f, 0f, 1f, 1f))),
    DUO_STACK("Stacked", 2, listOf(floatArrayOf(0f, 0f, 1f, .5f), floatArrayOf(0f, .5f, 1f, 1f))),
    TRIO_FEATURE("Feature", 3, listOf(floatArrayOf(0f, 0f, 1f, .6f), floatArrayOf(0f, .6f, .5f, 1f), floatArrayOf(.5f, .6f, 1f, 1f))),
    TRIO_COLUMNS("Columns", 3, listOf(floatArrayOf(0f, 0f, 1f / 3, 1f), floatArrayOf(1f / 3, 0f, 2f / 3, 1f), floatArrayOf(2f / 3, 0f, 1f, 1f))),
    QUAD("Grid", 4, listOf(floatArrayOf(0f, 0f, .5f, .5f), floatArrayOf(.5f, 0f, 1f, .5f), floatArrayOf(0f, .5f, .5f, 1f), floatArrayOf(.5f, .5f, 1f, 1f))),
    MOSAIC("Mosaic", 4, listOf(floatArrayOf(0f, 0f, .62f, .62f), floatArrayOf(.62f, 0f, 1f, .31f), floatArrayOf(.62f, .31f, 1f, .62f), floatArrayOf(0f, .62f, 1f, 1f))),
    SIX("Six", 6, (0 until 6).map { i -> val c = i % 2; val r = i / 2; floatArrayOf(c * .5f, r / 3f, (c + 1) * .5f, (r + 1) / 3f) });

    companion object { fun forCount(n: Int) = entries.filter { it.count <= max(2, n) } }
}

data class CollageEdit(
    val layout: CollageLayout = CollageLayout.DUO_SIDE,
    /** Photo index shown in each cell (cycles through the picked photos). */
    val order: List<Int> = emptyList(),
    val gap: Float = 0.012f,
    val radius: Float = 0.02f,
    val background: Long = 0xFFFDF6E3,
    val aspect: CropAspect = CropAspect.PORTRAIT
) {
    fun photoFor(cell: Int, photos: Int): Int = if (photos <= 0) 0 else (order.getOrNull(cell) ?: cell) % photos
    fun swap(cell: Int, photos: Int): CollageEdit {
        val o = (0 until layout.count).map { photoFor(it, photos) }.toMutableList(); o[cell] = (o[cell] + 1) % photos
        return copy(order = o)
    }
    /** Cell rectangles in pixels (left, top, right, bottom) after the outer + inner gap. */
    fun rects(w: Int, h: Int): List<FloatArray> {
        val g = gap * min(w, h)
        return layout.cells.map { c ->
            floatArrayOf(c[0] * w + (if (c[0] == 0f) g else g / 2), c[1] * h + (if (c[1] == 0f) g else g / 2),
                c[2] * w - (if (c[2] == 1f) g else g / 2), c[3] * h - (if (c[3] == 1f) g else g / 2))
        }
    }
    fun outputSize(longSide: Int = 2048): Pair<Int, Int> {
        val r = aspect.ratio ?: (4f / 5f)
        return if (r >= 1f) longSide to (longSide / r).roundToInt() else (longSide * r).roundToInt() to longSide
    }
}

// ---------------------------------------------------------------- Islamic date stickers

enum class CountdownKind(val label: String) { JUMUAH("Jumu'ah"), RAMADAN("Ramadan"), EID_FITR("Eid al-Fitr"), EID_ADHA("Eid al-Adha") }

object IslamicDates {
    /** Hijri (month, day) for a Gregorian day — injected so tests can use a fake calendar. */
    fun interface HijriOf { fun of(cal: Calendar): Pair<Int, Int> }

    /** Whole days until the next [kind] (0 = today), scanning up to ~13 lunar months. */
    fun daysUntil(kind: CountdownKind, now: Calendar, hijri: HijriOf): Int? {
        val c = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }
        if (kind == CountdownKind.JUMUAH) return (Calendar.FRIDAY - c.get(Calendar.DAY_OF_WEEK) + 7) % 7
        val (tm, td) = when (kind) { CountdownKind.RAMADAN -> 9 to 1; CountdownKind.EID_FITR -> 10 to 1; else -> 12 to 10 }
        for (d in 0..400) {
            val (m, day) = hijri.of(c)
            if (m == tm && day == td) return d
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }

    fun countdownLabel(kind: CountdownKind, days: Int?): String = when {
        days == null -> kind.label
        days == 0 -> if (kind == CountdownKind.JUMUAH) "Jumu'ah Mubarak — today" else "${kind.label} Mubarak — today"
        days == 1 -> "1 day to ${kind.label}"
        else -> "$days days to ${kind.label}"
    }

    fun hijriLabel(day: Int, month: Int, year: Int, monthNames: List<String>): String =
        "$day ${monthNames.getOrElse(month - 1) { "" }} $year AH".replace("  ", " ")
}

// ---------------------------------------------------------------- templates

/** What the template placeholders are filled with (all optional; the user edits them after). */
data class TemplateData(
    val ayah: String = "Indeed, with hardship comes ease.",
    val ayahRef: String = "Ash-Sharh 94:6",
    val dua: String = "Our Lord, give us good in this world and good in the Hereafter.",
    val duaRef: String = "Al-Baqarah 2:201",
    val hijri: String = "",
    val countdown: String = "",
    val reminder: String = "Tap to write your reminder"
)

enum class EditorTemplate(val label: String, val note: String) {
    REMINDER("Reminder", "Title + your words, gentle fade"),
    AYAH_OF_DAY("Ayah of the day", "Verse card with reference"),
    DUA_CARD("Du'a", "Du'a card on a calm filter"),
    JUMUAH("Jumu'ah", "Jumu'ah badge + reminder"),
    COUNTDOWN("Countdown", "Days to Ramadan / Eid"),
    HIJRI_DAY("Hijri date", "Today's Hijri date + note");

    /** Layers for a frame; [newId] makes unique ids. Times are for reels (photos ignore them). */
    fun build(data: TemplateData, newId: (String) -> String, totalMs: Long = 0L): Pair<NoorFilter?, List<EditorLayer>> {
        val end = if (totalMs > 0) totalMs else Long.MAX_VALUE
        fun t(text: String, y: Float, font: EditorFont, anim: TextAnim, start: Long = 0, scale: Float = 1f, pill: TextPill = TextPill.NONE, look: TextLook = TextLook.SHADOW) =
            EditorLayer.text(newId("t"), text, font).copy(y = y, anim = anim, startMs = start, endMs = end, scale = scale, pill = pill, look = look,
                pillColor = 0xFF0B6243)
        return when (this) {
            REMINDER -> NoorFilter.NOOR to listOf(
                t("Reminder", 0.3f, EditorFont.CLASSIC, TextAnim.FADE, scale = 1.25f, look = TextLook.GRADIENT),
                t(data.reminder, 0.5f, EditorFont.MODERN, TextAnim.TYPEWRITER, start = if (totalMs > 0) 600 else 0, pill = TextPill.SOFT, look = TextLook.PLAIN))
            AYAH_OF_DAY -> NoorFilter.FAJR to listOf(
                t("Ayah of the day", 0.2f, EditorFont.CLASSIC, TextAnim.SLIDE, look = TextLook.GRADIENT),
                EditorLayer(newId("s"), sticker = StickerKind.AYAH, text = data.ayah, sub = data.ayahRef, y = 0.52f, anim = TextAnim.POP, startMs = if (totalMs > 0) 500 else 0, endMs = end))
            DUA_CARD -> NoorFilter.MADINAH to listOf(
                EditorLayer(newId("s"), sticker = StickerKind.DUA, text = data.dua, sub = data.duaRef, y = 0.5f, anim = TextAnim.FADE, endMs = end))
            JUMUAH -> NoorFilter.MADINAH to listOf(
                EditorLayer(newId("s"), sticker = StickerKind.JUMUAH, y = 0.28f, scale = 1.3f, anim = TextAnim.POP, endMs = end),
                t("Send salawat upon the Prophet ﷺ", 0.62f, EditorFont.MODERN, TextAnim.FADE, start = if (totalMs > 0) 700 else 0, pill = TextPill.SOFT, look = TextLook.PLAIN))
            COUNTDOWN -> NoorFilter.LAYL to listOf(
                EditorLayer(newId("s"), sticker = StickerKind.COUNTDOWN, text = data.countdown.ifBlank { "Ramadan is coming" }, sub = "Prepare your heart", y = 0.45f, anim = TextAnim.POP, endMs = end),
                EditorLayer(newId("s"), sticker = StickerKind.LANTERN, x = 0.78f, y = 0.18f, scale = 0.8f, endMs = end))
            HIJRI_DAY -> NoorFilter.NOOR to listOf(
                EditorLayer(newId("s"), sticker = StickerKind.HIJRI, text = data.hijri.ifBlank { "Hijri date" }, y = 0.2f, anim = TextAnim.SLIDE, endMs = end),
                t(data.reminder, 0.55f, EditorFont.CLASSIC, TextAnim.FADE, start = if (totalMs > 0) 500 else 0))
        }
    }
}

// ---------------------------------------------------------------- story poll / question / quiz (flagged off)

/**
 * Interactive story stickers need a backend for votes / answers. Until those rules are deployed they stay hidden:
 * [enabled] is false, so the sticker tray never offers them and nothing is written. The payload below is what a
 * story doc would carry (additive `interactive` key — see firestore.rules ummah_stories + tools/rules-tests).
 */
object StoryInteractive {
    const val enabled = false

    /** The `interactive` map for a story doc, or null when the story has no interactive sticker. */
    fun payload(layers: List<EditorLayer>): Map<String, Any>? {
        val l = layers.firstOrNull { it.sticker?.interactive == true } ?: return null
        val kind = when (l.sticker) { StickerKind.POLL -> "poll"; StickerKind.QUIZ -> "quiz"; else -> "question" }
        val opts = l.options.map { it.trim().take(28) }.filter { it.isNotEmpty() }.take(4)
        return buildMap {
            put("kind", kind); put("prompt", l.text.trim().take(120))
            if (kind != "question") put("options", opts)
            if (kind == "quiz") put("answer", l.answer.coerceIn(0, (opts.size - 1).coerceAtLeast(0)))
        }
    }
}
