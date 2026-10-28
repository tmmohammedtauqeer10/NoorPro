package com.noorpro.app.v112

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import java.io.File

/** Saves the activity window as build/v112/<name>.png. */
internal fun AndroidComposeTestRule<*, out androidx.activity.ComponentActivity>.shot(name: String) {
    waitForIdle()
    mainClock.advanceTimeBy(1200)
    waitForIdle()
    val v = activity.window.decorView
    val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
    v.draw(Canvas(bmp))
    val out = File(File("build/v112").apply { mkdirs() }, "$name.png")
    out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue("$name not rendered", out.length() > 5000)
}

internal fun AndroidComposeTestRule<*, out androidx.activity.ComponentActivity>.show(dark: Boolean, content: @Composable () -> Unit) =
    setContent { MyApplicationTheme(darkTheme = dark) { content() } }

/** Loads the Halal Guard word list from the app assets (same file the app ships). */
internal fun loadHalalWords() {
    if (com.noorpro.app.data.halal.HalalTextFilter.isLoaded) return
    com.noorpro.app.data.halal.HalalTextFilter.load(listOf("src/main/assets/halal/words.json", "app/src/main/assets/halal/words.json").map(::File).first { it.exists() }.readText())
}

internal val NOW = System.currentTimeMillis()
internal fun ago(minutes: Long) = NOW - minutes * 60_000L
