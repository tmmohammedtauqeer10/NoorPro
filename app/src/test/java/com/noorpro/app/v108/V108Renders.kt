package com.noorpro.app.v108

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.junit.Assert.assertTrue
import java.io.File

/** Saves the activity window as build/v108/<name>.png (same capture as the 1.1.0 feed renders). */
internal fun AndroidComposeTestRule<*, out androidx.activity.ComponentActivity>.saveShot(name: String) {
    waitForIdle()
    mainClock.advanceTimeBy(1200)
    waitForIdle()
    val v = activity.window.decorView
    val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
    v.draw(android.graphics.Canvas(bmp))
    val out = File(File("build/v108").apply { mkdirs() }, "$name.png")
    out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue("$name not rendered", out.length() > 5000)
}
