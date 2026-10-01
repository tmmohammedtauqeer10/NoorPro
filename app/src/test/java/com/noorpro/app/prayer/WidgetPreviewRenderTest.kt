package com.noorpro.app.prayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.prayer.widget.PrayerWidgetUpdater
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Inflates the real RemoteViews the widgets produce (so it also proves they are RemoteViews-safe)
 * and renders them to PNGs in build/widget-previews for design review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WidgetPreviewRenderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val snap = PrayerWidgetUpdater.Snapshot(
        times = mapOf("Fajr" to "05:04", "Sunrise" to "06:16", "Dhuhr" to "12:16", "Asr" to "15:36", "Maghrib" to "18:07", "Isha" to "19:24"),
        nextName = "Dhuhr",
        nextTime = "12:16",
        countdown = "2h 56m",
        available = true,
        hijri = "19 Rabi al-Awwal",
    )

    private fun render(views: RemoteViews, wDp: Int, hDp: Int, name: String): File {
        val d = context.resources.displayMetrics.density
        val w = (wDp * d).toInt()
        val h = (hDp * d).toInt()
        val parent = FrameLayout(context)
        val v: View = views.apply(context, parent)
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        val pad = (16 * d).toInt()
        val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        // Neutral "wallpaper" behind the widget so rounded corners are visible.
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), Color.parseColor("#3B4A52"), Color.parseColor("#1E2A33"), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), paint)
        c.translate(pad.toFloat(), pad.toFloat())
        v.draw(c)
        val dir = File("build/widget-previews").apply { mkdirs() }
        val out = File(dir, "$name.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return out
    }

    @Test
    fun renderWidgets() {
        val next = render(PrayerWidgetUpdater.buildNextViews(context, 1, snap, 0), 160, 160, "widget_next_2x2")
        val day = render(PrayerWidgetUpdater.buildDayViews(context, 2, snap, 0), 320, 160, "widget_day_4x2")
        val nextSmall = render(PrayerWidgetUpdater.buildNextViews(context, 1, snap, 110), 110, 110, "widget_next_2x2_compact")
        val none = snap.copy(available = false, nextName = "-", times = emptyMap())
        val unavailable = render(PrayerWidgetUpdater.buildNextViews(context, 1, none, 0), 160, 160, "widget_next_2x2_no_location")
        listOf(next, day, nextSmall, unavailable).forEach { assertTrue("${it.name} not rendered", it.length() > 1000) }
    }

    @Test
    fun hijriLabelDoesNotThrow() {
        val s = PrayerDisplay.hijriLabel(context, short = true)
        assertTrue(s.isEmpty() || s.first().isDigit())
    }
}
