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
        t.id.startsWith("dua-") -> "DUA"
        t.id.startsWith("tafsir-") -> "TAFSIR"
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
        val f = File(dir, "c_${(title + kicker + seed).hashCode().toUInt()}.png")
        if (!f.exists() || f.length() == 0L) f.outputStream().use { render(title, kicker, tintFor(seed)).compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    fun render(title: String, kicker: String, tint: Int, size: Int = 512): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val dark = blend(tint, 0xFF000000.toInt(), 0.55f); val light = blend(tint, 0xFFFFFFFF.toInt(), 0.18f)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), light, dark, Shader.TileMode.CLAMP) })
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = size / 220f; color = 0x2EFFFFFF }
        val step = size / 4f
        var y = 0f
        while (y <= size + step) { var x = 0f; while (x <= size + step) { c.drawPath(star(x, y, step * 0.42f), line); x += step }; y += step }
        c.drawCircle(size * 0.82f, size * 0.18f, size * 0.32f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x14FFFFFF })
        val pad = size * 0.08f
        val kp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE9C46A.toInt(); textSize = size * 0.055f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.18f }
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = size * 0.105f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val sl = StaticLayout.Builder.obtain(title, 0, title.length, tp, (size - pad * 2).toInt()).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).setLineSpacing(0f, 1.0f).build()
        val top = size - pad - sl.height
        c.drawText(kicker, pad, top - size * 0.035f, kp)
        c.save(); c.translate(pad, top); sl.draw(c); c.restore()
        c.drawRect(pad, size - pad * 0.6f, pad + size * 0.12f, size - pad * 0.6f + size / 110f, Paint().apply { color = 0xFFE9C46A.toInt() })
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
