package com.noorpro.app.audio.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.noorpro.app.audio.models.Track
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * 1.0.90: generated cover art for tracks that have no publisher artwork (duas, tafsir, tarjuma…).
 * A title on an Islamic eight-point-star lattice over a tinted gradient, rendered once to a PNG in
 * the cache so the notification, lock screen, mini and full player all show the same image.
 */
object CoverArt {
    private val PALETTE = intArrayOf(0xFF0F5D46.toInt(), 0xFF6B3FA0.toInt(), 0xFF1E5AA8.toInt(), 0xFFB0532A.toInt(),
        0xFF8C2F5B.toInt(), 0xFF00796B.toInt(), 0xFF3B4C9B.toInt(), 0xFF6B4E16.toInt())

    fun tintFor(seed: String): Int = PALETTE[Math.floorMod(seed.hashCode(), PALETTE.size)]

    /** Category label shown above the title, from the track id/tags. */
    fun kicker(t: Track): String = when {
        t.ayahKind == "tafsir" || t.id.contains("tafsir") -> "TAFSIR"
        t.ayahKind == "tarjuma" -> "TARJUMA"
        t.ayahKind == "arabic" -> "QURAN"
        t.id.startsWith("dua-") || t.id.startsWith("free-dua-") -> "DUA"
        t.id.startsWith("maf-") -> "NASHEED"
        t.id.startsWith("pod-") -> "PODCAST"
        t.language?.startsWith("ur") == true -> "URDU"
        t.language?.startsWith("bn") == true -> "BENGALI"
        else -> "AL-NOOR AUDIO"
    }

    /** Returns [t] with a cover: its own art if present, otherwise a generated file:// cover. */
    fun withCover(ctx: Context, t: Track): Track =
        if (!t.coverUrl.isNullOrBlank()) t else runCatching { t.copy(coverUrl = fileFor(ctx, t.artistName.ifBlank { t.title }, kicker(t), t.artistId.ifBlank { t.id }).toURI().toString()) }.getOrDefault(t)

    fun fileFor(ctx: Context, title: String, kicker: String, seed: String): File {
        val dir = File(ctx.cacheDir, "covers").apply { mkdirs() }
        val f = File(dir, "c3_${(title + kicker + seed).hashCode().toUInt()}.png")
        if (!f.exists() || f.length() == 0L) f.outputStream().use { render(title, kicker, tintFor(seed)).compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    fun render(title: String, kicker: String, tint: Int, size: Int = 512): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val cream = 0xFFF6EFE3.toInt()
        val emerald = 0xFF0F5D46.toInt()
        val deep = blend(emerald, 0xFF000000.toInt(), 0.25f)
        val gold = 0xFFC9A24A.toInt()
        val ink = 0xFF14352C.toInt()
        c.drawColor(cream)
        c.drawRect(0f, 0f, size.toFloat(), size * 0.42f,
            Paint().apply { shader = LinearGradient(0f, 0f, 0f, size * 0.42f, deep, cream, Shader.TileMode.CLAMP) })
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = size / 90f; color = gold; strokeCap = Paint.Cap.ROUND
        }
        val inset = size * 0.12f
        val arch = Path().apply {
            moveTo(inset, size * 0.55f)
            lineTo(inset, size * 0.36f)
            cubicTo(inset, size * 0.16f, size - inset, size * 0.16f, size - inset, size * 0.36f)
            lineTo(size - inset, size * 0.55f)
        }
        c.drawPath(arch, stroke)
        c.drawLine(inset, size * 0.55f, size - inset, size * 0.55f, stroke)
        // modest corner ornaments — not a repeating pattern
        val orn = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = size / 180f; color = gold }
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            c.drawLine(x, y, x + dx * size * 0.08f, y, orn)
            c.drawLine(x, y, x, y + dy * size * 0.08f, orn)
            c.drawCircle(x + dx * size * 0.03f, y + dy * size * 0.03f, size * 0.012f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold })
        }
        val m = size * 0.06f
        corner(m, m, 1f, 1f); corner(size - m, m, -1f, 1f)
        corner(m, size - m, 1f, -1f); corner(size - m, size - m, -1f, -1f)
        val pad = size * 0.12f
        val kp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold; textSize = size * 0.05f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.16f; textAlign = Paint.Align.CENTER
        }
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = size * 0.085f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        c.drawText(kicker, size / 2f, size * 0.68f, kp)
        val sl = StaticLayout.Builder.obtain(title, 0, title.length, tp, (size - pad * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).setLineSpacing(0f, 1.05f).build()
        c.save(); c.translate(pad, size * 0.71f); sl.draw(c); c.restore()
        c.drawRect(size * 0.42f, size * 0.93f, size * 0.58f, size * 0.93f + size / 140f, Paint().apply { color = gold })
        return b
    }

    /** Eight-point star (two overlapping squares) centred at (cx, cy). */
    fun star(cx: Float, cy: Float, r: Float): Path = Path().apply {
        for (k in 0 until 16) {
            val a = Math.PI / 8 * k - Math.PI / 2
            val rr = if (k % 2 == 0) r else r * 0.72f
            val px = cx + (rr * cos(a)).toFloat(); val py = cy + (rr * sin(a)).toFloat()
            if (k == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    private fun blend(a: Int, b: Int, f: Float): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) * (1 - f) + ((b shr s) and 0xFF) * f).toInt() and 0xFF
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
