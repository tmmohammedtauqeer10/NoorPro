package com.noorpro.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.ui.viewmodel.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Store screenshot captures (real screens, sample Bengaluru location). Output: build/store/<name>.png */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class StoreShotsTest {
    @get:Rule val rule = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun prep() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, Configuration.Builder().build())
        ctx.getSharedPreferences("onboarding", android.content.Context.MODE_PRIVATE).edit()
            .putBoolean("permissions_intro_complete", true).putBoolean("media_notif_asked", true).putBoolean("exact_alarm_asked", true).commit()
        com.noorpro.app.prayer.PrayerPrefs(ctx).permissionPromptSnoozeUntil = System.currentTimeMillis() + 86_400_000L
        runBlocking { com.noorpro.app.data.UserPreferencesRepository(ctx).updateLocation(12.9716, 77.5946) }
    }

    private fun setLocName(vm: DeenViewModel) {
        runCatching {
            val f = DeenViewModel::class.java.getDeclaredField("_currentLocationName"); f.isAccessible = true
            @Suppress("UNCHECKED_CAST") (f.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<String>).value = "Bengaluru, IN"
        }
    }

    @Test fun shots() {
        val names = (System.getenv("STORE_SCREENS") ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val dark = mutableStateOf(false)
        val target = mutableStateOf(DeenScreen.DASHBOARD)
        var vmRef: DeenViewModel? = null
        rule.setContent {
            val vm: DeenViewModel = viewModel(); vmRef = vm
            LaunchedEffect(dark.value) { vm.updateThemeMode(if (dark.value) ThemeMode.DARK else ThemeMode.LIGHT) }
            LaunchedEffect(target.value) { vm.navigateTo(target.value) }
            MyApplicationTheme(darkTheme = dark.value) { MainLayout() }
        }
        rule.waitForIdle(); vmRef!!.recalculatePrayers()
        Thread.sleep(2500); rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        for (spec in names) {
            val (scr, mode) = spec.split(':').let { it[0] to (it.getOrNull(1) ?: "light") }
            runCatching {
                dark.value = mode == "dark"
                if (scr.startsWith("SURAH")) {
                    val id = scr.removePrefix("SURAH").toInt()
                    val s = vmRef!!.surahs.value.firstOrNull { it.id == id } ?: com.noorpro.app.data.IslamicData.surahs.first { it.id == id }
                    vmRef!!.selectSurah(s)
                } else target.value = DeenScreen.valueOf(scr)
                rule.waitForIdle(); Thread.sleep(1500); rule.mainClock.advanceTimeBy(2500); rule.waitForIdle()
                setLocName(vmRef!!); rule.mainClock.advanceTimeBy(500); rule.waitForIdle()
                rule.onRoot().captureRoboImage(filePath = "build/store/${scr}_$mode.png")
            }.onFailure { println("STORE_FAIL $spec ${it.message?.take(300)}") }
        }
    }
}
