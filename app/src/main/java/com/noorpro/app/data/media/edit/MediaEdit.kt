package com.noorpro.app.data.media.edit

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * 1.1.3 — in-app media editor models (photos + reels). Pure Kotlin so the maths is unit-tested;
 * Android rendering lives in PhotoRenderer / ReelExporter and the UI in NoorMediaEditor.kt.
 * Feature references: Instagram / Wahda. Look, names and art are NoorPro's own.
 */

/** A 4×5 colour matrix (rows R,G,B,A; columns r,g,b,a,offset). Offsets are in 0..1 (not 0..255). */
class ColorMatrix5(val m: FloatArray = IDENTITY.copyOf()) {
    init { require(m.size == 20) }

    /** this ∘ other: apply [other] first, then this. */
    operator fun times(other: ColorMatrix5): ColorMatrix5 {
        val a = m; val b = other.m; val out = FloatArray(20)
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                var s = 0f
                for (k in 0 until 4) s += a[r * 5 + k] * b[k * 5 + c]
                out[r * 5 + c] = s
            }
            var o = a[r * 5 + 4]
            for (k in 0 until 4) o += a[r * 5 + k] * b[k * 5 + 4]
            out[r * 5 + 4] = o
        }
        return ColorMatrix5(out)
    }

    /** Apply to an RGB colour in 0..1, clamped. */
    fun apply(r: Float, g: Float, b: Float): FloatArray = FloatArray(3) { row ->
        (m[row * 5] * r + m[row * 5 + 1] * g + m[row * 5 + 2] * b + m[row * 5 + 4]).coerceIn(0f, 1f)
    }

    /** Linear blend between identity (t=0) and this (t=1) — the filter "strength" slider. */
    fun strength(t: Float): ColorMatrix5 {
        val k = t.coerceIn(0f, 1f)
        return ColorMatrix5(FloatArray(20) { i -> IDENTITY[i] + (m[i] - IDENTITY[i]) * k })
    }

    /** android.graphics.ColorMatrix layout (offsets in 0..255). */
    fun toAndroid(): FloatArray = FloatArray(20) { i -> if (i % 5 == 4) m[i] * 255f else m[i] }

    /** Column-major 4×4 for media3 RgbMatrix (shader does M · vec4(rgb, 1); offsets go in column 3). */
    fun toGl(): FloatArray {
        val o = FloatArray(16)
        for (r in 0 until 3) {
            for (c in 0 until 3) o[c * 4 + r] = m[r * 5 + c]
            o[3 * 4 + r] = m[r * 5 + 4]
        }
        o[15] = 1f
        return o
    }

    fun isIdentity(eps: Float = 1e-4f) = m.indices.all { abs(m[it] - IDENTITY[it]) < eps }

    companion object {
        val IDENTITY = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        private const val LR = 0.2126f; private const val LG = 0.7152f; private const val LB = 0.0722f

        fun scale(r: Float, g: Float, b: Float) = ColorMatrix5(floatArrayOf(r, 0f, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
        fun offset(r: Float, g: Float, b: Float) = ColorMatrix5(floatArrayOf(1f, 0f, 0f, 0f, r, 0f, 1f, 0f, 0f, g, 0f, 0f, 1f, 0f, b, 0f, 0f, 0f, 1f, 0f))
        /** s=1 keeps colour, 0 = greyscale, >1 = more vivid. */
        fun saturation(s: Float): ColorMatrix5 {
            val i = 1 - s
            return ColorMatrix5(floatArrayOf(
                i * LR + s, i * LG, i * LB, 0f, 0f,
                i * LR, i * LG + s, i * LB, 0f, 0f,
                i * LR, i * LG, i * LB + s, 0f, 0f,
                0f, 0f, 0f, 1f, 0f))
        }
        /** c=1 unchanged; pivots on mid-grey. */
        fun contrast(c: Float): ColorMatrix5 { val t = (1 - c) / 2f; return scale(c, c, c).let { offset(t, t, t) * it } }
        fun brightness(b: Float) = offset(b, b, b)
        /** w>0 warmer (amber), w<0 cooler (blue). */
        fun warmth(w: Float) = scale(1 + 0.12f * w, 1 + 0.02f * w, 1 - 0.14f * w)
        fun tint(r: Float, g: Float, b: Float, amount: Float) = ColorMatrix5(floatArrayOf(
            1 - amount, 0f, 0f, 0f, r * amount,
            0f, 1 - amount, 0f, 0f, g * amount,
            0f, 0f, 1 - amount, 0f, b * amount,
            0f, 0f, 0f, 1f, 0f))
        fun chain(vararg ms: ColorMatrix5): ColorMatrix5 = ms.fold(ColorMatrix5()) { acc, x -> x * acc }
    }
}

/** NoorPro filters: names are places and times of day; tuned to stay gentle on skin and skies. */
enum class NoorFilter(val label: String, val note: String) {
    ORIGINAL("Original", "As shot"),
    FAJR("Fajr", "Cool dawn blue, lifted shadows"),
    NOOR("Noor", "Bright, soft and clean"),
    MADINAH("Madinah", "Warm green and gold"),
    SAHARA("Sahara", "Sunlit amber"),
    LAYL("Layl", "Deep night, cool and moody"),
    ZAMZAM("Zamzam", "Fresh and clear"),
    ANDALUS("Andalus", "Faded terracotta film"),
    QAMAR("Qamar", "Silver moonlight (mono)"),
    RAWDAH("Rawdah", "Lush garden greens");

    fun matrix(): ColorMatrix5 = when (this) {
        ORIGINAL -> ColorMatrix5()
        FAJR -> ColorMatrix5.chain(ColorMatrix5.warmth(-0.7f), ColorMatrix5.contrast(0.92f), ColorMatrix5.brightness(0.04f), ColorMatrix5.saturation(0.9f))
        NOOR -> ColorMatrix5.chain(ColorMatrix5.brightness(0.07f), ColorMatrix5.contrast(0.94f), ColorMatrix5.saturation(1.05f), ColorMatrix5.warmth(0.15f))
        MADINAH -> ColorMatrix5.chain(ColorMatrix5.warmth(0.45f), ColorMatrix5.scale(1f, 1.04f, 0.96f), ColorMatrix5.saturation(1.12f), ColorMatrix5.contrast(1.05f))
        SAHARA -> ColorMatrix5.chain(ColorMatrix5.warmth(1f), ColorMatrix5.saturation(1.08f), ColorMatrix5.brightness(0.02f))
        LAYL -> ColorMatrix5.chain(ColorMatrix5.warmth(-0.5f), ColorMatrix5.brightness(-0.06f), ColorMatrix5.contrast(1.15f), ColorMatrix5.saturation(0.8f))
        ZAMZAM -> ColorMatrix5.chain(ColorMatrix5.scale(0.97f, 1.02f, 1.05f), ColorMatrix5.contrast(1.08f), ColorMatrix5.saturation(1.1f))
        ANDALUS -> ColorMatrix5.chain(ColorMatrix5.tint(0.86f, 0.55f, 0.40f, 0.12f), ColorMatrix5.contrast(0.88f), ColorMatrix5.saturation(0.85f), ColorMatrix5.brightness(0.03f))
        QAMAR -> ColorMatrix5.chain(ColorMatrix5.saturation(0f), ColorMatrix5.contrast(1.1f), ColorMatrix5.scale(0.98f, 1f, 1.04f))
        RAWDAH -> ColorMatrix5.chain(ColorMatrix5.scale(0.97f, 1.06f, 0.98f), ColorMatrix5.saturation(1.18f), ColorMatrix5.contrast(1.04f))
    }
}

/** Adjust sliders, each in -1..1 (vignette 0..1). 0 = untouched. */
data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val warmth: Float = 0f,
    val saturation: Float = 0f,
    val vignette: Float = 0f
) {
    val isNeutral get() = brightness == 0f && contrast == 0f && warmth == 0f && saturation == 0f && vignette == 0f
    fun matrix(): ColorMatrix5 = ColorMatrix5.chain(
        ColorMatrix5.brightness(brightness * 0.25f),
        ColorMatrix5.contrast(1f + contrast * 0.5f),
        ColorMatrix5.warmth(warmth),
        ColorMatrix5.saturation(1f + saturation)
    )
    fun with(kind: AdjustKind, v: Float): Adjustments = when (kind) {
        AdjustKind.BRIGHTNESS -> copy(brightness = v.coerceIn(-1f, 1f))
        AdjustKind.CONTRAST -> copy(contrast = v.coerceIn(-1f, 1f))
        AdjustKind.WARMTH -> copy(warmth = v.coerceIn(-1f, 1f))
        AdjustKind.SATURATION -> copy(saturation = v.coerceIn(-1f, 1f))
        AdjustKind.VIGNETTE -> copy(vignette = v.coerceIn(0f, 1f))
    }
    fun value(kind: AdjustKind): Float = when (kind) {
        AdjustKind.BRIGHTNESS -> brightness; AdjustKind.CONTRAST -> contrast; AdjustKind.WARMTH -> warmth
        AdjustKind.SATURATION -> saturation; AdjustKind.VIGNETTE -> vignette
    }
}

enum class AdjustKind(val label: String, val min: Float) {
    BRIGHTNESS("Brightness", -1f), CONTRAST("Contrast", -1f), WARMTH("Warmth", -1f), SATURATION("Saturation", -1f), VIGNETTE("Vignette", 0f)
}

/** Filter (at strength) followed by the adjust sliders — one matrix for preview, photo render and video export. */
fun colorMatrixFor(filter: NoorFilter, strength: Float, adjust: Adjustments): ColorMatrix5 =
    adjust.matrix() * filter.matrix().strength(strength)

// ---------------------------------------------------------------- crop + rotate

enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("Free", null), SQUARE("1:1", 1f), PORTRAIT("4:5", 4f / 5f), WIDE("16:9", 16f / 9f)
}

/** Normalised crop rectangle (0..1 of the rotated image). */
data class CropRect(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    val width get() = right - left
    val height get() = bottom - top
    val isFull get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f
    fun pixelAspect(imgW: Int, imgH: Int) = (width * imgW) / (height * imgH)
}

object Crop {
    /** Largest centred rect of [aspect] (w/h in pixels) inside an imgW×imgH image. */
    fun fit(imgW: Int, imgH: Int, aspect: CropAspect): CropRect {
        val ratio = aspect.ratio ?: return CropRect()
        val img = imgW.toFloat() / imgH
        return if (img > ratio) { val w = ratio / img; CropRect((1 - w) / 2, 0f, (1 + w) / 2, 1f) }
        else { val h = img / ratio; CropRect(0f, (1 - h) / 2, 1f, (1 + h) / 2) }
    }
    /** Move the rect by (dx, dy) normalised, staying inside the image. */
    fun pan(r: CropRect, dx: Float, dy: Float): CropRect {
        val x = (r.left + dx).coerceIn(0f, 1f - r.width); val y = (r.top + dy).coerceIn(0f, 1f - r.height)
        return CropRect(x, y, x + r.width, y + r.height)
    }
    /** Zoom the crop around its centre (z>1 tighter), keeping the aspect, min 15% of the image. */
    fun zoom(r: CropRect, z: Float): CropRect {
        val w = (r.width / z).coerceIn(0.15f, 1f); val h = (r.height / z).coerceIn(0.15f, 1f)
        val k = min(w / r.width, h / r.height).let { if (r.width * it > 1f || r.height * it > 1f) min(1f / r.width, 1f / r.height) else it }
        val nw = r.width * k; val nh = r.height * k
        val cx = (r.left + r.right) / 2; val cy = (r.top + r.bottom) / 2
        val x = (cx - nw / 2).coerceIn(0f, 1f - nw); val y = (cy - nh / 2).coerceIn(0f, 1f - nh)
        return CropRect(x, y, x + nw, y + nh)
    }
    /** Image size after quarter turns. */
    fun rotatedSize(w: Int, h: Int, quarterTurns: Int) = if (quarterTurns % 2 == 0) w to h else h to w
}

// ---------------------------------------------------------------- overlays

enum class EditorFont(val label: String, val arabic: Boolean = false) {
    CLASSIC("Classic"), MODERN("Modern"), STRONG("Strong"), NASKH("Naskh", arabic = true)
}

enum class TextPill(val label: String) { NONE("Plain"), SOLID("Pill"), SOFT("Soft") }

/** Palette for text, pills and the brush: NoorPro cream, emerald, gold + a few neutrals. */
object EditorPalette {
    val colors = listOf(0xFFFFFFFF, 0xFF111111, 0xFFD4AF37, 0xFF0B6243, 0xFFF6E7C1, 0xFF7FD3AE, 0xFFB5523B, 0xFF1F6FA3, 0xFF7A4E9E)
    /** A readable text colour on top of [bg]. */
    fun onColor(bg: Long): Long {
        val r = ((bg shr 16) and 0xFF) / 255f; val g = ((bg shr 8) and 0xFF) / 255f; val b = (bg and 0xFF) / 255f
        return if (0.2126f * r + 0.7152f * g + 0.0722f * b > 0.6f) 0xFF111111 else 0xFFFFFFFF
    }
}

enum class StickerKind(val label: String, val text: String? = null, val dynamic: Boolean = false) {
    CRESCENT("Crescent"), LANTERN("Lantern"), MOSQUE("Mosque"),
    JUMUAH("Jumu'ah Mubarak", "Jumu'ah Mubarak"), RAMADAN("Ramadan Kareem", "Ramadan Kareem"),
    ALHAMDULILLAH("Alhamdulillah", "Alhamdulillah"), SUBHANALLAH("SubhanAllah", "SubhanAllah"),
    LOCATION("Location", dynamic = true), TIME("Time", dynamic = true), HASHTAG("Hashtag", dynamic = true)
}

/** Text or sticker on top of the photo / video. Position = centre, normalised 0..1. Times in output ms (video only). */
data class EditorLayer(
    val id: String,
    val sticker: StickerKind? = null,
    val text: String = "",
    val font: EditorFont = EditorFont.MODERN,
    val color: Long = 0xFFFFFFFF,
    val pill: TextPill = TextPill.NONE,
    val pillColor: Long = 0xFF0B6243,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val startMs: Long = 0L,
    val endMs: Long = Long.MAX_VALUE
) {
    val isText get() = sticker == null
    /** What a sticker or text layer says (dynamic stickers carry their label in [text]). */
    val label get() = when {
        sticker == null -> text
        !sticker.dynamic -> sticker.text ?: ""
        else -> text.ifBlank { sticker.label }
    }

    fun dragBy(dx: Float, dy: Float) = copy(x = (x + dx).coerceIn(0.02f, 0.98f), y = (y + dy).coerceIn(0.02f, 0.98f))
    fun transform(zoom: Float, rotateDeg: Float) = copy(scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE), rotation = normaliseDeg(rotation + rotateDeg))
    fun visibleAt(ms: Long) = ms >= startMs && ms < endMs
    fun timed(start: Long, end: Long, total: Long): EditorLayer {
        val s = start.coerceIn(0, max(0, total - MIN_ON_SCREEN_MS)); val e = end.coerceIn(s + MIN_ON_SCREEN_MS, max(s + MIN_ON_SCREEN_MS, total))
        return copy(startMs = s, endMs = e)
    }

    companion object {
        const val MIN_SCALE = 0.35f
        const val MAX_SCALE = 4f
        const val MIN_ON_SCREEN_MS = 500L
        const val MAX_TEXT = 140
        fun normaliseDeg(d: Float): Float { var r = d % 360f; if (r > 180f) r -= 360f; if (r <= -180f) r += 360f; return r }
        fun text(id: String, text: String, font: EditorFont = EditorFont.MODERN) =
            EditorLayer(id, text = text.take(MAX_TEXT), font = if (ArabicText.has(text)) EditorFont.NASKH else font)
    }
}

object ArabicText {
    fun has(s: String) = s.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
}

/** A brush stroke. Points normalised to the (cropped) photo; width as a fraction of the shorter side. */
data class BrushStroke(val points: List<Pair<Float, Float>>, val color: Long = 0xFFD4AF37, val width: Float = 0.012f, val glow: Boolean = false)

// ---------------------------------------------------------------- photo edit + history

data class PhotoEdit(
    val quarterTurns: Int = 0,
    val aspect: CropAspect = CropAspect.FREE,
    val crop: CropRect = CropRect(),
    val filter: NoorFilter = NoorFilter.ORIGINAL,
    val strength: Float = 1f,
    val adjust: Adjustments = Adjustments(),
    val layers: List<EditorLayer> = emptyList(),
    val strokes: List<BrushStroke> = emptyList()
) {
    val isUntouched get() = this == PhotoEdit()
    val matrix get() = colorMatrixFor(filter, strength, adjust)
    fun rotate() = copy(quarterTurns = (quarterTurns + 1) % 4, crop = CropRect(), aspect = CropAspect.FREE)
    fun withAspect(a: CropAspect, imgW: Int, imgH: Int): PhotoEdit {
        val (w, h) = Crop.rotatedSize(imgW, imgH, quarterTurns)
        return copy(aspect = a, crop = Crop.fit(w, h, a))
    }
    fun upsert(l: EditorLayer) = copy(layers = layers.filterNot { it.id == l.id } + l)
    fun remove(id: String) = copy(layers = layers.filterNot { it.id == id })
}

/** Undo / redo with a cap. Commit on gesture end, not every frame. */
class EditHistory<T>(initial: T, private val cap: Int = 50) {
    private val past = ArrayDeque<T>()
    private val future = ArrayDeque<T>()
    private val first = initial
    var current: T = initial; private set
    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()
    val isDirty get() = current != first

    fun commit(next: T) {
        if (next == current) return
        past.addLast(current); if (past.size > cap) past.removeFirst()
        future.clear(); current = next
    }
    fun undo(): T { if (past.isNotEmpty()) { future.addLast(current); current = past.removeLast() }; return current }
    fun redo(): T { if (future.isNotEmpty()) { past.addLast(current); current = future.removeLast() }; return current }
    /** Back to the original; undoable. */
    fun reset(): T { commit(first); return current }
}
