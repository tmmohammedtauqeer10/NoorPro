package com.noorpro.app.data.media.edit

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import com.noorpro.app.R
import kotlin.math.min

/**
 * Fonts for text layers: bundled res/font families (Libre Caslon, Plus Jakarta Sans, Noto Naskh) plus
 * Android system families for the extra looks — no new font files, no APK growth. Naskh covers Arabic.
 */
class EditorTypefaces(val classic: Typeface, val modern: Typeface, val strong: Typeface, val naskh: Typeface, private val extra: Map<EditorFont, Typeface> = emptyMap()) {
    fun of(f: EditorFont): Typeface = when (f) {
        EditorFont.CLASSIC -> classic; EditorFont.MODERN -> modern; EditorFont.STRONG -> strong; EditorFont.NASKH -> naskh
        else -> extra[f] ?: system(f)
    }
    companion object {
        @Volatile private var cached: EditorTypefaces? = null
        fun system(f: EditorFont): Typeface = when (f) {
            EditorFont.SERIF -> Typeface.create("serif", Typeface.NORMAL)
            EditorFont.MONO -> Typeface.create("monospace", Typeface.NORMAL)
            EditorFont.SCRIPT -> Typeface.create("cursive", Typeface.NORMAL)
            EditorFont.CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
            EditorFont.LIGHT -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
        fun load(ctx: Context): EditorTypefaces = cached ?: run {
            fun f(id: Int, fb: Typeface) = runCatching { ResourcesCompat.getFont(ctx, id) }.getOrNull() ?: fb
            EditorTypefaces(
                f(R.font.librecaslontext_bold, Typeface.SERIF), f(R.font.plusjakartasans_semibold, Typeface.SANS_SERIF),
                f(R.font.plusjakartasans_extrabold, Typeface.DEFAULT_BOLD), f(R.font.noto_naskh_arabic, Typeface.SERIF),
                mapOf(
                    EditorFont.ITALIC to f(R.font.librecaslontext_italic, Typeface.create(Typeface.SERIF, Typeface.ITALIC)),
                    EditorFont.LIGHT to f(R.font.plusjakartasans_regular, system(EditorFont.LIGHT))
                )
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
    private const val INK = 0xFF1B2A24.toInt()

    /** Base unit: text size and sticker size scale with the shorter side of the frame. */
    fun unit(w: Int, h: Int) = min(w, h).toFloat()

    /** [atMs] = output time on reels (visibility + entrance animation); null = still photo. */
    fun drawAll(c: Canvas, layers: List<EditorLayer>, w: Int, h: Int, fonts: EditorTypefaces, atMs: Long? = null, selected: String? = null) {
        layers.forEach { l -> if (atMs == null || l.visibleAt(atMs)) draw(c, l, w, h, fonts, l.id == selected, l.animAt(atMs)) }
    }

    fun draw(c: Canvas, l: EditorLayer, w: Int, h: Int, fonts: EditorTypefaces, selected: Boolean = false, anim: AnimFrame = AnimFrame.DONE) {
        val u = unit(w, h)
        if (anim.alpha <= 0.003f) return
        val layered = anim.alpha < 0.999f
        if (layered) c.saveLayerAlpha(null, (anim.alpha * 255).toInt()) else c.save()
        c.translate(l.x * w, l.y * h + anim.dy * u); c.rotate(l.rotation); c.scale(l.scale * anim.scale, l.scale * anim.scale)
        val b = localBounds(l, u, fonts)
        when {
            l.isText -> drawText(c, l, u, fonts, b, anim.chars)
            l.sticker!!.interactive -> drawInteractive(c, l, u, fonts, b)
            l.sticker.card -> drawCard(c, l, u, fonts, b, anim.chars)
            l.sticker.dynamic -> drawInfoSticker(c, l, u, fonts, b)
            l.sticker.text != null -> drawWordSticker(c, l.sticker.text, u, fonts, b)
            else -> drawArt(c, l.sticker, u * 0.26f)
        }
        if (selected) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = u * 0.006f / l.scale; color = if (l.locked) 0xFFB0B7B3.toInt() else GOLD_LIGHT
                pathEffect = DashPathEffect(floatArrayOf(u * 0.02f, u * 0.012f), 0f)
            }
            val pad = u * 0.02f
            c.drawRoundRect(RectF(b.left - pad, b.top - pad, b.right + pad, b.bottom + pad), u * 0.03f, u * 0.03f, p)
        }
        c.restore()
    }

    // ------------------------------------------------------------ geometry

    private const val CARD_W = 0.8f

    /** Bounds around the layer centre before scale/rotation. */
    fun localBounds(l: EditorLayer, u: Float, fonts: EditorTypefaces): RectF {
        val k = l.sticker
        if (k != null && !k.dynamic && !k.card && k.text == null) { val r = u * 0.2f; return RectF(-r, -r * 0.95f, r, r * 0.95f) }
        if (k != null && (k.card || k.interactive)) { val hw = u * CARD_W / 2; val hh = cardHeight(l, u, fonts) / 2; return RectF(-hw, -hh, hw, hh) }
        val p = textPaint(l, u, fonts)
        val lines = displayLines(l, p, u)
        val tw = lines.maxOf { p.measureText(it) }
        val lh = p.fontSpacing
        val padX = when { !l.isText -> u * 0.05f; l.pill == TextPill.NONE -> u * 0.015f; else -> u * 0.04f }
        val padY = if (!l.isText || l.pill != TextPill.NONE) u * 0.022f else u * 0.008f
        val extra = if (!l.isText && l.sticker!!.dynamic) u * 0.06f else 0f
        val hw = (tw + extra) / 2 + padX; val hh = lh * lines.size / 2 + padY
        return RectF(-hw, -hh, hw, hh)
    }

    /** Word-wraps [text] to [maxW]; keeps at most [maxLines]. */
    internal fun wrap(text: String, p: Paint, maxW: Float, maxLines: Int): List<String> {
        val out = mutableListOf<String>()
        text.ifBlank { " " }.split('\n').forEach { para ->
            var line = ""
            para.split(' ').forEach { word ->
                val next = if (line.isEmpty()) word else "$line $word"
                if (line.isNotEmpty() && p.measureText(next) > maxW) { out += line; line = word } else line = next
            }
            out += line
        }
        if (out.size > maxLines) { val kept = out.take(maxLines).toMutableList(); kept[maxLines - 1] = kept[maxLines - 1].trimEnd() + "…"; return kept }
        return out
    }

    /** Text layers word-wrap to ~86% of the short side so long captions never run off the frame. */
    internal fun displayLines(l: EditorLayer, p: Paint, u: Float): List<String> =
        if (!l.isText) l.label.ifBlank { " " }.split('\n').take(1) else wrap(l.label, p, u * 0.86f, 6)

    private fun textPaint(l: EditorLayer, u: Float, fonts: EditorTypefaces) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = when {
            l.isText -> fonts.of(l.font)
            l.sticker!!.dynamic -> if (ArabicText.has(l.label)) fonts.naskh else fonts.strong
            else -> fonts.classic
        }
        textSize = when { l.isText -> u * 0.075f; l.sticker!!.dynamic -> u * 0.05f; else -> u * 0.062f }
        textAlign = Paint.Align.CENTER
        color = l.color.toInt()
    }

    // ------------------------------------------------------------ text layers

    private fun drawText(c: Canvas, l: EditorLayer, u: Float, fonts: EditorTypefaces, b: RectF, chars: Int?) {
        val p = textPaint(l, u, fonts)
        when (l.pill) {
            TextPill.SOLID -> c.drawRoundRect(b, u * 0.035f, u * 0.035f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = l.pillColor.toInt() })
            TextPill.SOFT -> c.drawRoundRect(b, u * 0.035f, u * 0.035f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (l.pillColor.toInt() and 0x00FFFFFF) or (0x99 shl 24) })
            TextPill.NONE -> if (l.look == TextLook.PLAIN) p.setShadowLayer(u * 0.012f, 0f, u * 0.004f, 0x88000000.toInt())
        }
        val lines = displayLines(l, p, u); val lh = p.fontSpacing
        val top = -lh * lines.size / 2
        val padX = if (l.pill == TextPill.NONE) u * 0.015f else u * 0.04f
        val (x, align) = when (l.align) {
            TextAlignment.LEFT -> b.left + padX to Paint.Align.LEFT
            TextAlignment.RIGHT -> b.right - padX to Paint.Align.RIGHT
            TextAlignment.CENTER -> 0f to Paint.Align.CENTER
        }
        p.textAlign = align
        var left = chars ?: Int.MAX_VALUE
        lines.forEachIndexed { i, full ->
            if (left <= 0) return
            val s = if (full.length <= left) full else full.take(left)
            left -= full.length + 1
            val base = top + lh * i + (lh - (p.descent() - p.ascent())) / 2 - p.ascent()
            styledText(c, s, x, base, p, l, u, b)
        }
    }

    /** One line with the layer's look (outline / shadow / neon / gradient). */
    private fun styledText(c: Canvas, s: String, x: Float, y: Float, p: Paint, l: EditorLayer, u: Float, b: RectF) {
        when (l.look) {
            TextLook.PLAIN -> c.drawText(s, x, y, p)
            TextLook.OUTLINE -> {
                val stroke = Paint(p).apply { clearShadowLayer(); style = Paint.Style.STROKE; strokeWidth = u * 0.012f; strokeJoin = Paint.Join.ROUND }
                // outline in the contrasting colour of the fill
                stroke.color = if (EditorPalette.onColor(l.color) == 0xFF111111L) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
                c.drawText(s, x, y, stroke); c.drawText(s, x, y, Paint(p).apply { clearShadowLayer() })
            }
            TextLook.SHADOW -> {
                c.drawText(s, x + u * 0.008f, y + u * 0.008f, Paint(p).apply { clearShadowLayer(); color = 0xB3000000.toInt() })
                c.drawText(s, x, y, Paint(p).apply { setShadowLayer(u * 0.02f, 0f, u * 0.006f, 0x99000000.toInt()) })
            }
            TextLook.NEON -> {
                val glow = Paint(p).apply { clearShadowLayer(); maskFilter = BlurMaskFilter(u * 0.025f, BlurMaskFilter.Blur.NORMAL) }
                c.drawText(s, x, y, glow); c.drawText(s, x, y, Paint(glow).apply { maskFilter = BlurMaskFilter(u * 0.01f, BlurMaskFilter.Blur.NORMAL) })
                c.drawText(s, x, y, Paint(p).apply { clearShadowLayer(); color = blend(l.color.toInt(), 0xFFFFFFFF.toInt(), 0.7f) })
            }
            TextLook.GRADIENT -> {
                val g = Paint(p).apply { clearShadowLayer(); shader = LinearGradient(b.left, b.top, b.right, b.bottom, intArrayOf(GOLD_LIGHT, GOLD, l.color.toInt()), null, Shader.TileMode.CLAMP) }
                c.drawText(s, x + u * 0.004f, y + u * 0.005f, Paint(p).apply { clearShadowLayer(); color = 0x66000000 })
                c.drawText(s, x, y, g)
            }
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(sh: Int) = (((a shr sh) and 0xFF) * (1 - t) + ((b shr sh) and 0xFF) * t).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    // ------------------------------------------------------------ stickers

    private fun drawWordSticker(c: Canvas, text: String, u: Float, fonts: EditorTypefaces, b: RectF) {
        val r = u * 0.05f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(b.left, b.top, b.right, b.bottom, EMERALD, EMERALD_DEEP, Shader.TileMode.CLAMP) }
        c.drawRoundRect(b, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 })
        c.drawRoundRect(b, r, r, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = u * 0.007f; color = GOLD }
        val inset = u * 0.012f
        c.drawRoundRect(RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset), r * 0.75f, r * 0.75f, border)
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
            StickerKind.TIME, StickerKind.PRAYER -> {
                ip.style = Paint.Style.STROKE; c.drawCircle(cx, cy, icon * 1.1f, ip)
                c.drawLine(cx, cy, cx, cy - icon * 0.7f, ip); c.drawLine(cx, cy, cx + icon * 0.55f, cy + icon * 0.2f, ip)
                if (l.sticker == StickerKind.PRAYER) c.drawCircle(cx, cy - icon * 1.55f, icon * 0.28f, Paint(ip).apply { style = Paint.Style.FILL })
            }
            StickerKind.HIJRI -> {
                val moon = Path().apply { addCircle(cx, cy, icon * 1.1f, Path.Direction.CW) }
                moon.op(Path().apply { addCircle(cx + icon * 0.5f, cy - icon * 0.3f, icon * 0.95f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
                c.drawPath(moon, Paint(ip).apply { style = Paint.Style.FILL })
            }
            else -> {
                val hp = Paint(p).apply { color = GOLD; textAlign = Paint.Align.CENTER; typeface = fonts.strong }
                c.drawText(if (l.sticker == StickerKind.MENTION) "@" else "#", cx, -(hp.descent() + hp.ascent()) / 2, hp)
            }
        }
        c.drawText(l.label, startX + u * 0.06f, -(p.descent() + p.ascent()) / 2, p)
    }

    // ------------------------------------------------------------ cards (ayah / du'a / hadith / countdown)

    private fun cardBodyPaint(l: EditorLayer, u: Float, fonts: EditorTypefaces) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val arabic = ArabicText.has(l.label)
        typeface = if (arabic) fonts.naskh else if (l.sticker == StickerKind.COUNTDOWN) fonts.strong else fonts.classic
        textSize = when { l.sticker == StickerKind.COUNTDOWN -> u * 0.064f; arabic -> u * 0.054f; l.label.length > 160 -> u * 0.038f; else -> u * 0.044f }
        textAlign = Paint.Align.CENTER
    }

    private fun cardHeader(k: StickerKind) = when (k) {
        StickerKind.AYAH -> "QUR'AN"; StickerKind.DUA -> "DU'A"; StickerKind.HADITH -> "HADITH"; StickerKind.COUNTDOWN -> "COUNTDOWN"
        StickerKind.POLL -> "POLL"; StickerKind.QUIZ -> "QUIZ"; StickerKind.QUESTION -> "ASK ME"; else -> k.label.uppercase()
    }

    private fun cardHeight(l: EditorLayer, u: Float, fonts: EditorTypefaces): Float {
        val k = l.sticker!!
        val head = u * 0.075f; val pad = u * 0.045f
        if (k.interactive) {
            val tp = cardBodyPaint(l, u, fonts).apply { textSize = u * 0.048f; typeface = fonts.strong }
            val title = wrap(l.label, tp, u * (CARD_W - 0.1f), 3).size * tp.fontSpacing
            val rows = when (k) { StickerKind.QUESTION -> 1; else -> l.options.size.coerceIn(2, 4) }
            return head + title + rows * u * 0.088f + pad * 1.6f
        }
        val p = cardBodyPaint(l, u, fonts)
        val lines = wrap(l.label, p, u * (CARD_W - 0.12f), if (k == StickerKind.COUNTDOWN) 2 else 8)
        val sub = if (l.sub.isNotBlank()) u * 0.06f else 0f
        return head + lines.size * p.fontSpacing + sub + pad * 1.5f
    }

    private fun drawCard(c: Canvas, l: EditorLayer, u: Float, fonts: EditorTypefaces, b: RectF, chars: Int?) {
        val k = l.sticker!!
        val r = u * 0.045f
        val dark = k == StickerKind.COUNTDOWN || k == StickerKind.AYAH
        c.drawRoundRect(RectF(b.left, b.top + u * 0.008f, b.right, b.bottom + u * 0.012f), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40000000 })
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = if (dark) LinearGradient(b.left, b.top, b.right, b.bottom, EMERALD, EMERALD_DEEP, Shader.TileMode.CLAMP)
            else LinearGradient(b.left, b.top, b.right, b.bottom, CREAM, 0xFFF4E7C6.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(b, r, r, fill)
        val inset = u * 0.014f
        c.drawRoundRect(RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset), r * 0.7f, r * 0.7f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = u * 0.005f; color = GOLD })
        // header: eight-point star + label
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = fonts.strong; textSize = u * 0.03f; letterSpacing = 0.18f; color = if (dark) GOLD_LIGHT else GOLD; textAlign = Paint.Align.CENTER }
        val hy = b.top + u * 0.06f
        c.drawText(cardHeader(k), 0f, hy, hp)
        val hw = hp.measureText(cardHeader(k)) / 2 + u * 0.03f
        star8(c, -hw, hy - u * 0.01f, u * 0.011f, GOLD); star8(c, hw, hy - u * 0.01f, u * 0.011f, GOLD)
        val p = cardBodyPaint(l, u, fonts).apply { color = if (dark) CREAM else INK }
        val lines = wrap(l.label, p, u * (CARD_W - 0.12f), if (k == StickerKind.COUNTDOWN) 2 else 8)
        var y = b.top + u * 0.075f + u * 0.02f - p.ascent()
        var left = chars ?: Int.MAX_VALUE
        for (s in lines) {
            if (left <= 0) break
            c.drawText(if (s.length <= left) s else s.take(left), 0f, y, p); left -= s.length + 1; y += p.fontSpacing
        }
        if (l.sub.isNotBlank()) {
            val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = fonts.modern; textSize = u * 0.032f; color = if (dark) GOLD_LIGHT else EMERALD; textAlign = Paint.Align.CENTER }
            c.drawText("— ${l.sub.take(48)}", 0f, b.bottom - u * 0.04f, sp)
        }
    }

    /** Story poll / quiz / question cards (only when the interactive-stories flag is on). */
    private fun drawInteractive(c: Canvas, l: EditorLayer, u: Float, fonts: EditorTypefaces, b: RectF) {
        val k = l.sticker!!
        val r = u * 0.045f
        c.drawRoundRect(RectF(b.left, b.top + u * 0.008f, b.right, b.bottom + u * 0.012f), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40000000 })
        c.drawRoundRect(b, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CREAM })
        c.drawRoundRect(RectF(b.left, b.top, b.right, b.top + u * 0.075f), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(b.left, 0f, b.right, 0f, EMERALD, EMERALD_DEEP, Shader.TileMode.CLAMP) })
        c.drawRect(b.left, b.top + u * 0.04f, b.right, b.top + u * 0.075f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(b.left, 0f, b.right, 0f, EMERALD, EMERALD_DEEP, Shader.TileMode.CLAMP) })
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = fonts.strong; textSize = u * 0.03f; letterSpacing = 0.18f; color = GOLD_LIGHT; textAlign = Paint.Align.CENTER }
        c.drawText(cardHeader(k), 0f, b.top + u * 0.05f, hp)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = if (ArabicText.has(l.label)) fonts.naskh else fonts.strong; textSize = u * 0.048f; color = INK; textAlign = Paint.Align.CENTER }
        var y = b.top + u * 0.075f + u * 0.03f - tp.ascent()
        wrap(l.label, tp, u * (CARD_W - 0.1f), 3).forEach { c.drawText(it, 0f, y, tp); y += tp.fontSpacing }
        y += u * 0.005f
        val rowH = u * 0.072f; val gap = u * 0.016f
        val op = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = fonts.modern; textSize = u * 0.038f; color = EMERALD_DEEP; textAlign = Paint.Align.LEFT }
        if (k == StickerKind.QUESTION) {
            val row = RectF(b.left + u * 0.04f, y - u * 0.02f, b.right - u * 0.04f, y - u * 0.02f + rowH)
            c.drawRoundRect(row, rowH / 2, rowH / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEDE3C8.toInt() })
            op.color = 0xFF7C8A83.toInt(); c.drawText("Type something…", row.left + u * 0.04f, row.centerY() - (op.descent() + op.ascent()) / 2, op)
            return
        }
        val opts = l.options.ifEmpty { listOf("Yes", "No") }.take(4)
        opts.forEachIndexed { i, o ->
            val row = RectF(b.left + u * 0.04f, y - u * 0.02f, b.right - u * 0.04f, y - u * 0.02f + rowH)
            c.drawRoundRect(row, rowH / 2, rowH / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEDE3C8.toInt() })
            c.drawRoundRect(row, rowH / 2, rowH / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = u * 0.004f; color = GOLD })
            val badge = if (k == StickerKind.QUIZ) "ABCD"[i].toString() else ""
            var tx = row.left + u * 0.04f
            if (badge.isNotEmpty()) {
                c.drawCircle(row.left + rowH / 2, row.centerY(), rowH * 0.34f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EMERALD })
                c.drawText(badge, row.left + rowH / 2, row.centerY() - (op.descent() + op.ascent()) / 2, Paint(op).apply { color = CREAM; textAlign = Paint.Align.CENTER; textSize = u * 0.03f })
                tx = row.left + rowH + u * 0.01f
            }
            c.drawText(o.take(28), tx, row.centerY() - (op.descent() + op.ascent()) / 2, op)
            y += rowH + gap
        }
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
                    addRect(-r * 0.62f, r * 0.05f, r * 0.62f, r * 0.62f, Path.Direction.CW)
                    moveTo(-r * 0.42f, r * 0.08f)
                    cubicTo(-r * 0.46f, -r * 0.42f, -r * 0.1f, -r * 0.52f, 0f, -r * 0.64f)
                    cubicTo(r * 0.1f, -r * 0.52f, r * 0.46f, -r * 0.42f, r * 0.42f, r * 0.08f); close()
                    addRect(-r * 0.86f, -r * 0.5f, -r * 0.72f, r * 0.62f, Path.Direction.CW)
                    addRect(r * 0.72f, -r * 0.5f, r * 0.86f, r * 0.62f, Path.Direction.CW)
                    moveTo(-r * 0.9f, -r * 0.5f); lineTo(-r * 0.79f, -r * 0.78f); lineTo(-r * 0.68f, -r * 0.5f); close()
                    moveTo(r * 0.68f, -r * 0.5f); lineTo(r * 0.79f, -r * 0.78f); lineTo(r * 0.9f, -r * 0.5f); close()
                }
                c.drawPath(s, emerald)
                c.drawPath(s, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = r * 0.035f; color = GOLD })
                val door = Path().apply { moveTo(-r * 0.12f, r * 0.62f); lineTo(-r * 0.12f, r * 0.36f); quadTo(0f, r * 0.2f, r * 0.12f, r * 0.36f); lineTo(r * 0.12f, r * 0.62f); close() }
                c.drawPath(door, gold)
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

    /** Snapping guides (centre lines) while dragging. */
    fun drawGuides(c: Canvas, w: Int, h: Int, vertical: Boolean, horizontal: Boolean) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD_LIGHT; strokeWidth = unit(w, h) * 0.004f; pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f) }
        if (vertical) c.drawLine(w / 2f, 0f, w / 2f, h.toFloat(), p)
        if (horizontal) c.drawLine(0f, h / 2f, w.toFloat(), h / 2f, p)
    }
}
