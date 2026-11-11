package com.noorpro.app.home

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.FloatingBottomNavigationBar
import com.noorpro.app.data.IslamicData
import com.noorpro.app.data.PrayerLogEntity
import com.noorpro.app.data.PrayerTime
import com.noorpro.app.data.QuranLastRead
import com.noorpro.app.ui.screens.BrandSplash
import com.noorpro.app.ui.screens.HomeDayStreak
import com.noorpro.app.ui.screens.StitchHomeScreen
import com.noorpro.app.ui.screens.capitalizedCountdown
import com.noorpro.app.ui.screens.prayersCompleted
import com.noorpro.app.ui.screens.splitAyahReference
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

/** 1.0.99 Home + splash: logic checks and real renders -> build/v99/. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V99HomeTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()

    private fun save(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        val out = File(File("build/v99").apply { mkdirs() }, "$name.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name not rendered", out.length() > 5000)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> DeenViewModel.set(field: String, value: T) {
        val f = DeenViewModel::class.java.getDeclaredField(field).apply { isAccessible = true }
        (f.get(this) as MutableStateFlow<T>).value = value
    }

    private fun seed(vm: DeenViewModel) {
        vm.isLoggedIn = true
        vm.userDisplayName = "Tauqeer Mohammed"
        vm.userPhotoUrl = ""
        vm.set("_prayers", listOf(
            PrayerTime("Fajr", "05:06", Color.White), PrayerTime("Dhuhr", "12:19", Color.White),
            PrayerTime("Asr", "15:41", Color.White), PrayerTime("Maghrib", "18:07", Color.White),
            PrayerTime("Isha", "19:21", Color.White)))
        vm.set("_nextPrayerName", "Fajr")
        vm.set("_nextPrayerCountdown", "06:36:10")
        vm.set("_currentLocationName", "Hagaribommanahalli")
        vm.set("_hasCurrentLocation", true)
        vm.set("_todayPrayerLog", PrayerLogEntity("2026-10-24", fajr = true, dhuhr = true))
        val kahf = IslamicData.surahs.first { it.id == 18 }
        vm.set("_lastReadSurah", kahf)
        vm.set("_lastReadAyah", 1)
        vm.set("_dailyQuote", IslamicData.quranQuotes[1])
    }

    private fun renderHome(dark: Boolean, prefix: String) {
        // Pin the preview clock (Robolectric's wall clock is near 1970): 24 Oct 2026, 22:30 IST.
        com.noorpro.app.ui.screens.HomeClock.nowOverride = 1792861200000L
        QuranLastRead.save(ctx, 18, 1, "Al-Kahf", "الكهف")
        val vm = DeenViewModel(ctx as android.app.Application)
        val darkState = mutableStateOf(dark)
        rule.setContent {
            MyApplicationTheme(darkTheme = darkState.value) {
                Box(Modifier.fillMaxSize()) {
                    StitchHomeScreen(vm)
                    FloatingBottomNavigationBar(DeenScreen.DASHBOARD, {}, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        rule.waitForIdle(); seed(vm); rule.mainClock.advanceTimeBy(1200); rule.waitForIdle()
        seed(vm); rule.waitForIdle()
        rule.onNodeWithText("NoorPro").assertIsDisplayed()
        rule.onNodeWithText("As-salamu alaykum, Tauqeer").assertIsDisplayed()
        rule.onNodeWithText("In 6h 36m").assertIsDisplayed()
        rule.onNodeWithText("Daily essentials").assertIsDisplayed()
        save("${prefix}_top")
        rule.onNodeWithTag("home_list").performScrollToIndex(5)
        rule.mainClock.advanceTimeBy(800); seed(vm); rule.waitForIdle()
        rule.onNodeWithTag("home_compact_bar").assertIsDisplayed()
        rule.onNodeWithText("Al-Noor Audio").assertIsDisplayed()
        rule.onNodeWithText("2 / 5").assertExists()
        save("${prefix}_scrolled")
        rule.onNodeWithTag("home_list").performScrollToIndex(8)
        rule.mainClock.advanceTimeBy(800); rule.waitForIdle()
        rule.onNodeWithText("Reflect with the Ummah").assertExists()
        rule.onNodeWithText("Quran 2:152").assertExists()
        save("${prefix}_bottom")
    }

    @Test fun homeLight() = renderHome(false, "home_light")
    @Test fun homeDark() = renderHome(true, "home_dark")

    @Test fun reflectionOpensUmmahComposerWhenSignedIn() {
        val vm = DeenViewModel(ctx as android.app.Application)
        rule.setContent { MyApplicationTheme(darkTheme = false) { StitchHomeScreen(vm) } }
        rule.waitForIdle(); seed(vm); rule.waitForIdle()
        rule.onNodeWithTag("home_list").performScrollToIndex(8); rule.waitForIdle()
        rule.onNodeWithTag("home_reflect").performClick(); rule.waitForIdle()
        assertEquals(DeenScreen.UMMAH_CREATE_POST, vm.currentScreen.value)
    }

    @Test fun guestHeaderHasNoName() {
        val vm = DeenViewModel(ctx as android.app.Application)
        vm.isLoggedIn = false; vm.userDisplayName = ""
        rule.setContent { MyApplicationTheme(darkTheme = false) { StitchHomeScreen(vm) } }
        rule.waitForIdle()
        rule.onNodeWithText("As-salamu alaykum").assertIsDisplayed()
    }

    @Test fun splash() {
        val dark = mutableStateOf(false)
        rule.setContent { MyApplicationTheme(darkTheme = dark.value) { BrandSplash() } }
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onNodeWithText("Al-Hikmah Digital").assertIsDisplayed()
        rule.onNodeWithText("from").assertIsDisplayed()
        rule.onNodeWithText("بِسْمِ ٱللَّهِ").assertDoesNotExist()
        save("splash")
    }

    @org.junit.After fun clearClock() { com.noorpro.app.ui.screens.HomeClock.nowOverride = null }

    @Test fun hijriLabelFormat() {
        val label = com.noorpro.app.ui.screens.homeHijriLabel(0, 1792861200000L)
        assertTrue(label, Regex("""^\d{1,2} [A-Za-z' -]+ · 14\d\d AH$""").matches(label))
    }

    @Test fun logic() {
        assertEquals("So remember Me; I will remember you." to "2:152", splitAyahReference("So remember Me; I will remember you. (2:152)"))
        assertEquals("No ref" to "", splitAyahReference("No ref"))
        IslamicData.quranQuotes.forEach { assertTrue(it.first, splitAyahReference(it.first).second.isNotBlank()) }
        assertEquals(0, prayersCompleted(null))
        assertEquals(3, prayersCompleted(PrayerLogEntity("d", fajr = true, asr = true, isha = true)))
        assertEquals("In 6h 36m", capitalizedCountdown("06:36:10"))
        val d1 = Calendar.getInstance().apply { set(2026, 9, 20, 9, 0) }
        assertEquals(1, HomeDayStreak.recordVisit(ctx, d1))
        assertEquals(1, HomeDayStreak.recordVisit(ctx, d1))
        assertEquals(2, HomeDayStreak.recordVisit(ctx, (d1.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }))
        assertEquals(3, HomeDayStreak.recordVisit(ctx, (d1.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }))
        assertEquals(1, HomeDayStreak.recordVisit(ctx, (d1.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 5) }))
    }

    @Test fun splashBrandingDrawableRenders() {
        val d = androidx.core.content.ContextCompat.getDrawable(ctx, com.noorpro.app.R.drawable.splash_branding)!!
        val b = Bitmap.createBitmap(600, 240, Bitmap.Config.ARGB_8888)
        b.eraseColor(android.graphics.Color.parseColor("#0B6243"))
        d.setBounds(0, 0, 600, 240); d.draw(android.graphics.Canvas(b))
        File("build/v99").mkdirs(); File("build/v99/splash_branding.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
