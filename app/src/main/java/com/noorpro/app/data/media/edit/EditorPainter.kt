package com.noorpro.app.data.media.edit

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import com.noorpro.app.R
import kotlin.math.min

/** Bundled fonts for text layers (already in res/font — no new assets). Naskh covers Arabic. */
class EditorTypefaces(val classic: Typeface, val modern: Typeface, val strong: Typeface, val naskh: Typeface) {
    fun of(f: EditorFont): Typeface = when (f) {
        EditorFont.CLASSIC -> classic; EditorFont.MODERN -> modern; EditorFont.STRONG -> strong; EditorFont.NASKH -> naskh
    }
    companion object {
        @Volatile private var cached: EditorTypefaces? = null
        fun load(ctx: Context): EditorTypefaces = cached ?: run {
            fun f(id: Int, fb: Typeface) = runCatching { ResourcesCompat.getFont(ctx, id) }.getOrNull() ?: fb
            EditorTypefaces(
                f(R.font.librecaslontext_bold, Typeface.SERIF), f(R.font.plusjakartasans_semibold, Typeface.SANS_SERIF),
                f(R.font.plusjakartasans_extrabold, Typeface.DEFAULT_BOLD), f(R.font.noto_naskh_arabic, Typeface.SERIF)
            ).also { cached = it }
        }
    }
}

/**
 * Draws text + sticker layers with android.graphics so the editor preview, the saved photo and the
 * video overlay bitmaps are pixel-identical. Stickers are original vector art drawn in code.
 */
object LayerPainter {
    private const val GOLD = 0xFFD4AF37.toInt()
    private const val GOLD_LIGHT = 0xFFF3D98B.toInt()
    private const val EMERALD = 0xFF0B6243.toInt()
    private const val EMERALD_DEEP = 0xFF003527.toInt()
    private const val CREAM = 0xFFFDF6E3.toInt()

    /** Base unit: text size and sticker size scale with the shorter side of the frame. */
    fun unit(w: Int, h: Int) = min(w, h).toFloat()

    fun drawAll(c: Canvas, layers: List<EditorLayer>, w: Int, h: Int, fonts: EditorTypefaces, atMs: Long? = null, selected: String? = null) {
        layers.forEach { l -> if (atMs == null || l.visibleAt(atMs)) draw(c, l, w, h, fonts, l.id == selected) }
    }

    fun draw(c: Canvas, l: EditorLayer, w: Int, h: Int, fonts: EditorTypefaces, selected: Boolean = false) {
        val u = unit(w, h)
        c.save()
        c.translate(l.x * w, l.y * h); c.rotate(l.rotation); c.scale(l.scale, l.scale)
        val b = localBounds(l, u, fonts)
        when {
            l.isText -> drawText(c, l, u, fonts, b)
            l.sticker!!.dynamic -> drawInfoSticker(c, l, u, fonts, b)
            l.sticker.text != null -> drawWordSticker(c, l.sticker.text, u, fonts, b)
            else -> drawArt(c, l.sticker, u * 0.26f)
        }
        if (selected) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = u * 0.006f / l.scale; color = GOLD_LIGHT
                pathEffect = DashPathEffect(floatArrayOf(u * 0.02f, u * 0.012f), 0f)
            }
            val pad = u * 0.02f
            c.drawRoundRect(RectF(b.left - pad, b.top - pad, b.right + pad, b.bottom + pad), u * 0.03f, u * 0.03f, p)
        }
        c.restore()
    }

    /** Bounds around the layer centre before scale/rotation. */
    fun localBounds(l: EditorLayer, u: Float, fonts: EditorTypefaces): RectF {
        if (!l.isText && !l.sticker!!.dynamic && l.sticker.text == null) { val r = u * 0.2f; return RectF(-r, -r * 0.95f, r, r * 0.95f) }
        val p = textPaint(l, u, fonts)
        val lines = displayLines(l, p, u)
        val tw = lines.maxOf { p.measureText(it) }
        val lh = p.fontSpacing
        val padX = when { !l.isText -> u * 0.05f; l.pill == TextPill.NONE -> u * 0.01f; else -> u * 0.04f }
        val padY = if (!l.isText || l.pill != TextPill.NONE) u * 0.022f else u * 0.005f
        val extra = if (!l.isText && l.sticker!!.dynamic) u * 0.06f else 0f
        val hw = (tw + extra) / 2 + padX; val hh = lh * lines.size / 2 + padY
        return RectF(-hw, -hh, hw, hh)
    }

    /** Text layers word-wrap to ~86% of the short side so long captions never run off the frame. */
    internal fun displayLines(l: EditorLayer, p: Paint, u: Float): List<String> {
        val raw = l.label.ifBlank { " " }.split('\n')
        if (!l.isText) return raw.take(1)
        val maxW = u * 0.86f
        val out = mutableListOf<String>()
        raw.forEach { para ->
            var line = ""
            para.split(' ').forEach { word ->
                val next = if (line.isEmpty()) word else "$line $word"
                if (line.isNotEmpty() && p.measureText(next) > maxW) { out += line; line = word } else line = next
            }
            out += line
        }
        return out.take(6)
    }

    private fun textPaint(l: EditorLayer, u: Float, fonts: EditorTypefaces) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = when {
            l.isText -> fonts.of(l.font)
            l.sticker!!.dynamic -> fonts.strong
            else -> fonts.classic
        }
        textSize = when { l.isText -> u * 0.075f; l.sticker!!.dynamic -> u * 0.05f; else -> u * 0.062f }
        textAlign = Paint.Align.CENTER
        color = l.color.toInt()
    }

    private fun drawText(c: Canvas, l: EditorLayer, u: Float, fonts: EditorTypefaces, b: RectF) {
        val p = textPaint(l, u, fonts)
        when (l.pill) {
            TextPill.SOLID -> c.drawRoundRect(b, u * 0.035f, u * 0.035f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = l.pillColor.toInt() })
            TextPill.SOFT -> c.drawRoundRect(b, u * 0.035f, u * 0.035f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (l.pillColor.toInt() and 0x00FFFFFF) or (0x99 shl 24) })
            TextPill.NONE -> p.setShadowLayer(u * 0.012f, 0f, u * 0.004f, 0x88000000.toInt())
        }
        val lines = displayLines(l, p, u); val lh = p.fontSpacing
        var y = -lh * lines.size / 2 - p.ascent() - (p.descent() + p.ascent()) * 0f
        val top = -lh * lines.size / 2
        lines.forEachIndexed { i, s ->
            val base = top + lh * i + (lh - (p.descent() - p.ascent())) / 2 - p.ascent()
            c.drawText(s, 0f, base, p); y = base
        }
    }

    private fun drawWordSticker(c: Canvas, text: String, u: Float, fonts: EditorTypefaces, b: RectF) {
        val r = u * 0.05f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(b.left, b.top, b.right, b.bottom, EMERALD, EMERALD_DEEP, Shader.TileMode.CLAMP) }
        c.drawRoundRect(b, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000; maskFilter = null }.also { }) // soft base
        c.drawRoundRect(b, r, r, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = u * 0.007f; color = GOLD }
        val inset = u * 0.012f
        c.drawRoundRect(RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset), r * 0.75f, r * 0.75f, border)
        // tiny eight-point stars at the corners (geometric, not figurative)
        listOf(b.left + inset * 2.6f to b.top + inset * 2.6f, b.right - inset * 2.6f to b.bottom - inset * 2.6f).forEach { (x, y) -> star8(c, x, y, u * 0.014f, GOLD) }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = fonts.classic; textSize = u * 0.062f; textAlign = Paint.Align.CENTER; color = CREAM }
        c.drawText(text, 0f, -(p.descent() + p.ascent()) / 2, p)
    }

    private fun drawInfoSticker(c: Canvas, l: EditorLayer, u: Float, fonts: EditorTypefaces, b: RectF) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CREAM }
        c.drawRoundRect(b, b.height() / 2, b.height() / 2, bg)
        c.drawRoundRect(b, b.height() / 2, b.height() / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = u * 0.005f; color = GOLD })
        val p = textPaint(l, u, fonts).apply { color = EMERALD; textAlign = Paint.Align.LEFT }
        val icon = u * 0.022f
        val tw = p.measureText(l.label)
        val startX = -(tw + u * 0.06f) / 2
        val cx = startX + icon; val cy = 0f
        val ip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD; strokeWidth = u * 0.006f; strokeCap = Paint.Cap.ROUND }
        when (l.sticker) {
            StickerKind.LOCATION -> {
                val path = Path().apply {
                    moveTo(cx, cy + icon * 1.3f)
                    cubicTo(cx - icon * 1.6f, cy - icon * 0.2f, cx - icon * 0.9f, cy - icon * 1.4f, cx, cy - icon * 1.4f)
                    cubicTo(cx + icon * 0.9f, cy - icon * 1.4f, cx + icon * 1.6f, cy - icon * 0.2f, cx, cy + icon * 1.3f); close()
                }
                c.drawPath(path, ip); c.drawCircle(cx, cy - icon * 0.45f, icon * 0.4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CREAM })
            }
            StickerKind.TIME -> {
                ip.style = Paint.Style.STROKE; c.drawCircle(cx, cy, icon * 1.1f, ip)
                c.drawLine(cx, cy, cx, cy - icon * 0.7f, ip); c.drawLine(cx, cy, cx + icon * 0.55f, cy + icon * 0.2f, ip)
            }
            else -> {
                val hp = Paint(p).apply { color = GOLD; textAlign = Paint.Align.CENTER }
                c.drawText("#", cx, -(hp.descent() + hp.ascent()) / 2, hp)
            }
        }
        c.drawText(l.label, startX + u * 0.06f, -(p.descent() + p.ascent()) / 2, p)
    }

    /** Original geometric sticker art (no fonts, no imported clip-art). [r] = half size. */
    fun drawArt(c: Canvas, kind: StickerKind, r: Float) {
        val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(-r, -r, r, r, GOLD_LIGHT, GOLD, Shader.TileMode.CLAMP) }
        val emerald = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, -r, 0f, r, 0xFF1F7A5A.toInt(), EMERALD_DEEP, Shader.TileMode.CLAMP) }
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(0f, 0f, r, 0x66F3D98B, 0x00F3D98B, Shader.TileMode.CLAMP) }
        when (kind) {
            StickerKind.CRESCENT -> {
                c.drawCircle(0f, 0f, r, glow)
                val moon = Path().apply { addCircle(-r * 0.08f, 0f, r * 0.62f, Path.Direction.CW) }
                val bite = Path().apply { addCircle(r * 0.18f, -r * 0.12f, r * 0.52f, Path.Direction.CW) }
                moon.op(bite, Path.Op.DIFFERENCE); c.drawPath(moon, gold)
                star5(c, r * 0.42f, -r * 0.08f, r * 0.17f, gold)
            }
            StickerKind.LANTERN -> {
                c.drawCircle(0f, r * 0.1f, r, glow)
                val line = Paint(gold).apply { style = Paint.Style.STROKE; strokeWidth = r * 0.06f; shader = null; color = GOLD }
                c.drawCircle(0f, -r * 0.86f, r * 0.1f, line)
                val cap = Path().apply { moveTo(-r * 0.34f, -r * 0.5f); lineTo(0f, -r * 0.76f); lineTo(r * 0.34f, -r * 0.5f); close() }
                c.drawPath(cap, gold)
                val body = Path().apply {
                    moveTo(-r * 0.36f, -r * 0.46f); lineTo(r * 0.36f, -r * 0.46f); lineTo(r * 0.44f, r * 0.42f)
                    lineTo(-r * 0.44f, r * 0.42f); close()
                }
                c.drawPath(body, emerald)
                val light = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(0f, 0f, r * 0.4f, 0xFFFFF3C4.toInt(), 0x00FFF3C4, Shader.TileMode.CLAMP) }
                c.drawCircle(0f, 0f, r * 0.4f, light)
                c.drawPath(body, line)
                c.drawLine(0f, -r * 0.46f, 0f, r * 0.42f, line)
                c.drawLine(-r * 0.4f, 0f, r * 0.4f, 0f, Paint(line).apply { strokeWidth = r * 0.035f })
                val base = Path().apply { moveTo(-r * 0.5f, r * 0.42f); lineTo(r * 0.5f, r * 0.42f); lineTo(r * 0.3f, r * 0.62f); lineTo(-r * 0.3f, r * 0.62f); close() }
                c.drawPath(base, gold)
            }
            StickerKind.MOSQUE -> {
                c.drawCircle(0f, 0f, r, glow)
                val s = Path().apply {
                    // base hall
                    addRect(-r * 0.62f, r * 0.05f, r * 0.62f, r * 0.62f, Path.Direction.CW)
                    // dome
                    moveTo(-r * 0.42f, r * 0.08f)
                    cubicTo(-r * 0.46f, -r * 0.42f, -r * 0.1f, -r * 0.52f, 0f, -r * 0.64f)
                    cubicTo(r * 0.1f, -r * 0.52f, r * 0.46f, -r * 0.42f, r * 0.42f, r * 0.08f); close()
                    // minarets
                    addRect(-r * 0.86f, -r * 0.5f, -r * 0.72f, r * 0.62f, Path.Direction.CW)
                    addRect(r * 0.72f, -r * 0.5f, r * 0.86f, r * 0.62f, Path.Direction.CW)
                    moveTo(-r * 0.9f, -r * 0.5f); lineTo(-r * 0.79f, -r * 0.78f); lineTo(-r * 0.68f, -r * 0.5f); close()
                    moveTo(r * 0.68f, -r * 0.5f); lineTo(r * 0.79f, -r * 0.78f); lineTo(r * 0.9f, -r * 0.5f); close()
                }
                c.drawPath(s, emerald)
                c.drawPath(s, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = r * 0.035f; color = GOLD })
                // door arch
                val door = Path().apply { moveTo(-r * 0.12f, r * 0.62f); lineTo(-r * 0.12f, r * 0.36f); quadTo(0f, r * 0.2f, r * 0.12f, r * 0.36f); lineTo(r * 0.12f, r * 0.62f); close() }
                c.drawPath(door, gold)
                // finial crescent
                val m = Path().apply { addCircle(0f, -r * 0.76f, r * 0.09f, Path.Direction.CW) }
                m.op(Path().apply { addCircle(r * 0.04f, -r * 0.79f, r * 0.075f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
                c.drawPath(m, gold)
            }
            else -> Unit
        }
    }

    private fun star5(c: Canvas, x: Float, y: Float, r: Float, p: Paint) {
        val path = Path()
        for (i in 0 until 10) {
            val a = Math.toRadians(-90.0 + i * 36.0); val rr = if (i % 2 == 0) r else r * 0.45f
            val px = x + (rr * Math.cos(a)).toFloat(); val py = y + (rr * Math.sin(a)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close(); c.drawPath(path, p)
    }

    private fun star8(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        c.save(); c.translate(x, y)
        c.drawRect(-r * 0.7f, -r * 0.7f, r * 0.7f, r * 0.7f, p); c.rotate(45f); c.drawRect(-r * 0.7f, -r * 0.7f, r * 0.7f, r * 0.7f, p)
        c.restore()
    }

    /** Hit test in frame pixels: topmost layer under (px, py). */
    fun hit(layers: List<EditorLayer>, px: Float, py: Float, w: Int, h: Int, fonts: EditorTypefaces, atMs: Long? = null): EditorLayer? {
        val u = unit(w, h)
        return layers.asReversed().firstOrNull { l ->
            if (atMs != null && !l.visibleAt(atMs)) return@firstOrNull false
            val m = Matrix().apply { postScale(l.scale, l.scale); postRotate(l.rotation); postTranslate(l.x * w, l.y * h) }
            val inv = Matrix(); m.invert(inv)
            val pt = floatArrayOf(px, py); inv.mapPoints(pt)
            val b = localBounds(l, u, fonts); val slop = u * 0.03f / l.scale
            pt[0] in (b.left - slop)..(b.right + slop) && pt[1] in (b.top - slop)..(b.bottom + slop)
        }
    }

    fun drawStrokes(c: Canvas, strokes: List<BrushStroke>, w: Int, h: Int) {
        val u = unit(w, h)
        strokes.forEach { s ->
            if (s.points.isEmpty()) return@forEach
            val path = Path()
            s.points.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x * w, y * h) else path.lineTo(x * w, y * h) }
            if (s.points.size == 1) path.lineTo(s.points[0].first * w + 0.1f, s.points[0].second * h)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = s.width * u; color = s.color.toInt()
                strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            }
            if (s.glow) c.drawPath(path, Paint(p).apply { strokeWidth = s.width * u * 2.6f; alpha = 70 })
            c.drawPath(path, p)
        }
    }

    fun drawVignette(c: Canvas, w: Int, h: Int, amount: Float) {
        if (amount <= 0f) return
        val r = kotlin.math.hypot(w / 2f, h / 2f)
        val a = (amount.coerceIn(0f, 1f) * 0.72f * 255).toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(w / 2f, h / 2f, r, intArrayOf(0x00000000, 0x00000000, Color.argb(a, 0, 0, 0)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
    }
}
