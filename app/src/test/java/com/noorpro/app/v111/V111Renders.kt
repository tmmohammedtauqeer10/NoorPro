package com.noorpro.app.v111

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.noorpro.app.islamic.calendar.HijriConverter
import com.noorpro.app.islamic.calendar.HijriDate
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import java.io.File

/** Saves the activity window as build/v111/<name>.png. */
internal fun AndroidComposeTestRule<*, out androidx.activity.ComponentActivity>.shot(name: String) {
    waitForIdle()
    mainClock.advanceTimeBy(1200)
    waitForIdle()
    val v = activity.window.decorView
    val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
    v.draw(Canvas(bmp))
    val out = File(File("build/v111").apply { mkdirs() }, "$name.png")
    out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue("$name not rendered", out.length() > 5000)
}

internal fun AndroidComposeTestRule<*, out androidx.activity.ComponentActivity>.show(dark: Boolean, content: @Composable () -> Unit) =
    setContent { MyApplicationTheme(darkTheme = dark) { content() } }

/**
 * Arithmetic (tabular, 30-year cycle) Hijri calendar, so the pure-logic tests don't depend on ICU data.
 * Epoch: 1 Muharram 1 AH = 16 July 622 (Julian) = epoch day -492148.
 */
internal val TabularHijri = HijriConverter { ed ->
    val days = ed + 492148L // days since 1 Muharram 1 AH
    val cycle = Math.floorDiv(days, 10631L)
    var rem = days - cycle * 10631L
    var year = (cycle * 30).toInt() + 1
    val leap = setOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
    while (true) {
        val len = if (((year - 1) % 30 + 1) in leap) 355 else 354
        if (rem < len) break
        rem -= len; year++
    }
    var month = 1
    while (true) {
        val len = if (month % 2 == 1) 30 else if (month == 12 && ((year - 1) % 30 + 1) in leap) 30 else 29
        if (rem < len) break
        rem -= len; month++
    }
    HijriDate(year, month, rem.toInt() + 1)
}
