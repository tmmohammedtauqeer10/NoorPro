package com.noorpro.app.v109

import android.graphics.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.junit.Assert.assertTrue
import java.io.File

/** Saves the activity window as build/v109/<name>.png. */
internal fun AndroidComposeTestRule<*, out androidx.activity.ComponentActivity>.shot(name: String) {
    waitForIdle()
    mainClock.advanceTimeBy(1200)
    waitForIdle()
    val v = activity.window.decorView
    val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
    v.draw(Canvas(bmp))
    val out = File(File("build/v109").apply { mkdirs() }, "$name.png")
    out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue("$name not rendered", out.length() > 5000)
}

/** A stand-in video frame of the given size: gradient, dome, label, and a gold edge so cropping is visible. */
internal fun frame(w: Int, h: Int, a: Int, b: Int, label: String): Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bm ->
    val c = Canvas(bm)
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), a, b, Shader.TileMode.CLAMP) })
    val r = minOf(w, h) * 0.22f
    c.drawCircle(w / 2f, h * 0.55f, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD4AF37.toInt() })
    c.drawRect(w / 2f - r, h * 0.55f, w / 2f + r, h * 0.55f + r * 1.1f, Paint().apply { color = 0xFFD4AF37.toInt() })
    c.drawRect(w * 0.08f, h * 0.2f, w * 0.08f + r * 0.25f, h * 0.55f + r * 1.1f, Paint().apply { color = 0xFFF3E7C3.toInt() })
    c.drawRect(w * 0.92f - r * 0.25f, h * 0.2f, w * 0.92f, h * 0.55f + r * 1.1f, Paint().apply { color = 0xFFF3E7C3.toInt() })
    c.drawText(label, w / 2f, h * 0.18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = minOf(w, h) * 0.12f; textAlign = Paint.Align.CENTER; isFakeBoldText = true })
    c.drawRect(3f, 3f, w - 3f, h - 3f, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 8f; color = 0xFFFFD45A.toInt() })
}
