package com.noorpro.app.widget

import android.app.AlarmManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.QuranLastRead
import com.noorpro.app.prayer.widget.PrayerWidgetUpdater
import com.noorpro.app.prayer.widget.WidgetTicker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 1.0.98 home-screen widgets: renders the REAL RemoteViews of all four widgets (light + dark) to
 * build/widget-previews/v98 (used for the review sheet and the pre-Android-12 picker images) and
 * checks the data paths (countdown, last-read fallback, now-playing idle, minute ticker).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V98WidgetsTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun snap() = PrayerWidgetUpdater.Snapshot(
        times = mapOf("Fajr" to "05:12", "Sunrise" to "06:28", "Dhuhr" to "12:30", "Asr" to "16:35", "Maghrib" to "18:52", "Isha" to "20:15"),
        nextName = "Asr",
        nextTime = "16:35",
        countdown = "42m",
        available = true,
        targetMs = System.currentTimeMillis() + 41 * 60_000L + 30_000L,
    )

    private val playing = NowPlayingWidget.State(
        title = "Surah Al-Kahf", subtitle = "Mishary Rashid Alafasy", artUri = null,
        isPlaying = true, positionMs = 134_000L, durationMs = 765_000L, isQuran = true,
    )

    private fun inflate(views: RemoteViews, wDp: Int, hDp: Int, scale: Float = 1f): Pair<View, Bitmap> {
        val d = ctx.resources.displayMetrics.density
        val w = (wDp * d).toInt()
        val h = (hDp * d).toInt()
        val v = views.apply(ctx, FrameLayout(ctx))
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply { scale(scale, scale); v.draw(this) }
        return v to bmp
    }

    private fun save(bmp: Bitmap, name: String): File {
        val out = File(File("build/widget-previews/v98").apply { mkdirs() }, "$name.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name not rendered", out.length() > 1500)
        return out
    }

    private fun text(v: View, id: Int) = v.findViewById<TextView>(id).text.toString()

    private fun renderAll(suffix: String) {
        val next = inflate(PrayerWidgetUpdater.buildNextViews(ctx, 1, snap(), 0), 170, 170)
        assertEquals("Asr", text(next.first, R.id.widget_next_name))
        assertTrue(text(next.first, R.id.widget_next_time).startsWith("4:35"))
        assertEquals("In 42 min", text(next.first, R.id.widget_next_countdown))
        save(next.second, "next_prayer_$suffix")

        val target = ContinueQuranWidget.Target(18, 10, "Al-Kahf", "Continue Quran", "Continue \u2192")
        val cq = inflate(ContinueQuranWidget.buildViews(ctx, target), 170, 170)
        assertEquals("Ayah 10", text(cq.first, R.id.widget_cq_ayah))
        save(cq.second, "continue_quran_$suffix")

        val day = inflate(PrayerWidgetUpdater.buildDayViews(ctx, 2, snap(), 0), 360, 170)
        assertEquals("Asr", text(day.first, R.id.widget_day_next_name))
        save(day.second, "prayer_times_$suffix")

        val np = inflate(NowPlayingWidget.buildViews(ctx, playing, null, null), 360, 170)
        assertEquals("Surah Al-Kahf", text(np.first, R.id.widget_np_title))
        assertEquals("12:45", text(np.first, R.id.widget_np_duration))
        save(np.second, "now_playing_$suffix")

        val idle = inflate(NowPlayingWidget.buildViews(ctx, null, null, null), 360, 170)
        assertEquals("Play Quran", text(idle.first, R.id.widget_np_title))
        save(idle.second, "now_playing_idle_$suffix")

        save(inflate(PrayerWidgetUpdater.buildNextViews(ctx, 1, snap(), 110), 120, 120).second, "next_prayer_compact_$suffix")
        save(inflate(PrayerWidgetUpdater.buildDayViews(ctx, 2, snap(), 110), 300, 110).second, "prayer_times_compact_$suffix")
    }

    @Test fun rendersLight() {
        renderAll("light")
        // Picker images for Android 11 and older (previewLayout covers 12+).
        save(inflate(PrayerWidgetUpdater.buildNextViews(ctx, 1, snap(), 0), 170, 170, 0.5f).second, "picker_next_prayer")
        save(inflate(ContinueQuranWidget.buildViews(ctx, ContinueQuranWidget.Target(18, 10, "Al-Kahf", "Continue Quran", "Continue \u2192")), 170, 170, 0.5f).second, "picker_continue_quran")
        save(inflate(PrayerWidgetUpdater.buildDayViews(ctx, 2, snap(), 0), 360, 170, 0.5f).second, "picker_prayer_times")
        save(inflate(NowPlayingWidget.buildViews(ctx, playing, null, null), 360, 170, 0.5f).second, "picker_now_playing")
    }

    @Test fun rendersDark() {
        RuntimeEnvironment.setQualifiers("+night")
        renderAll("dark")
    }

    @Test fun previewLayoutsInflate() {
        listOf(R.layout.widget_preview_next, R.layout.widget_preview_day, R.layout.widget_preview_quran, R.layout.widget_preview_now_playing)
            .forEach { assertNotNull(inflate(RemoteViews(ctx.packageName, it), 360, 170).first) }
    }

    @Test fun continueQuranFallsBackToFatihaThenUsesSavedPosition() = runBlocking {
        val first = ContinueQuranWidget.resolveTarget(ctx)
        assertEquals(1, first.surahId)
        assertEquals("Start reading \u2192", first.button)
        QuranLastRead.save(ctx, 18, 10, "Al-Kahf", "الكهف")
        val saved = ContinueQuranWidget.resolveTarget(ctx)
        assertEquals(18, saved.surahId)
        assertEquals(10, saved.ayah)
        assertEquals("Al-Kahf", saved.name)
        assertEquals("Continue \u2192", saved.button)
    }

    @Test fun nowPlayingClockAndIdleLastPlayed() {
        assertEquals("02:14", NowPlayingWidget.clock(134_000))
        assertEquals("1:02:03", NowPlayingWidget.clock(3_723_000))
        val v = inflate(NowPlayingWidget.buildViews(ctx, null, NowPlayingWidget.LastPlayed("Surah Yaseen", "Abdul Basit", null, true), null), 360, 170).first
        assertEquals("Surah Yaseen", text(v, R.id.widget_np_title))
        assertEquals(View.GONE, v.findViewById<View>(R.id.widget_np_prev).visibility)
    }

    @Test fun countdownWords() {
        assertEquals("In 42 min", PrayerWidgetUpdater.countdownWords(42 * 60_000L - 10_000L))
        assertTrue(PrayerWidgetUpdater.countdownWords(2 * 3_600_000L + 5 * 60_000L).startsWith("In 2 h"))
    }

    @Test fun minuteTickerIsNonWakeupAndCancellable() {
        val am = ctx.getSystemService(AlarmManager::class.java)
        WidgetTicker.scheduleNext(ctx)
        val alarm = shadowOf(am).peekNextScheduledAlarm()
        assertNotNull(alarm)
        assertEquals(AlarmManager.RTC, alarm!!.type)
        assertTrue(alarm.triggerAtTime - System.currentTimeMillis() <= 61_000L)
        WidgetTicker.cancel(ctx)
        assertEquals(null, shadowOf(am).peekNextScheduledAlarm())
    }
}
