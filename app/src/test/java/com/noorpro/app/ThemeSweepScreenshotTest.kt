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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders many screens in light and dark (set SWEEP_SCREENS=A,B to limit). Output: build/sweep/<screen>_<mode>.png */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class ThemeSweepScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Before fun wm() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, Configuration.Builder().build())
        ctx.getSharedPreferences("onboarding", android.content.Context.MODE_PRIVATE).edit().putBoolean("permissions_intro_complete", true).putBoolean("media_notif_asked", true).commit()
        com.noorpro.app.prayer.PrayerPrefs(ctx).permissionPromptSnoozeUntil = System.currentTimeMillis() + 86_400_000L
    }

    @Test fun sweep() {
        val names = (System.getenv("SWEEP_SCREENS") ?: System.getProperty("sweep.screens"))?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }
        val screens = names?.map { DeenScreen.valueOf(it) } ?: listOf(
            DeenScreen.DASHBOARD, DeenScreen.EXPLORE, DeenScreen.EDUCATION, DeenScreen.QURAN_LEARNING_DASHBOARD, DeenScreen.QURAN_LEARNING_READER,
            DeenScreen.QAIDA_TUTOR, DeenScreen.QURAN, DeenScreen.AI_HUB, DeenScreen.TASAWWUF, DeenScreen.DAROOD_SHARIF, DeenScreen.RUHANI,
            DeenScreen.PRAYER_TIMES, DeenScreen.SETTINGS, DeenScreen.ADVANCED_SETTINGS, DeenScreen.AZKAR, DeenScreen.TASBIH, DeenScreen.CALENDAR,
            DeenScreen.HADITH_LIBRARY, DeenScreen.DUA_HUB, DeenScreen.QAZA_TRACKER, DeenScreen.LIBRARY_DASHBOARD, DeenScreen.QUIZ_DASHBOARD,
            DeenScreen.ASMA_UL_HUSNA, DeenScreen.ZAKAT, DeenScreen.HEALTH_WELLNESS, DeenScreen.HAJJ_UMRAH, DeenScreen.AUDIO_LIBRARY,
            DeenScreen.SPIRITUAL_PROGRESS, DeenScreen.BOOKMARKS, DeenScreen.QIBLA_MORE, DeenScreen.PROFILE_DASHBOARD, DeenScreen.INDOPAK_QURAN,
            DeenScreen.PDF_LIBRARY, DeenScreen.DEEN_POINTS, DeenScreen.ACCOUNT_SWITCHER, DeenScreen.UMMAH_MESSAGES, DeenScreen.LOGIN, DeenScreen.TAFSIR, DeenScreen.ACTIVE_QUIZ,
        )
        val dark = mutableStateOf(false)
        val target = mutableStateOf(DeenScreen.DASHBOARD)
        rule.setContent {
            val vm: DeenViewModel = viewModel()
            LaunchedEffect(dark.value) { vm.updateThemeMode(if (dark.value) ThemeMode.DARK else ThemeMode.LIGHT) }
            LaunchedEffect(target.value, dark.value) { vm.navigateTo(target.value) }
            MyApplicationTheme(darkTheme = dark.value) { MainLayout() }
        }
        // A screen that throws during composition (e.g. eager Firebase access when it is not
        // configured) leaves Compose permanently "not idle": every later screen then burned a
        // 60 s idle timeout (~80 min total, looked like a hang). Fail fast and name the screen.
        val failures = mutableListOf<String>()
        var stuck = 0
        loop@ for (s in screens) for (d in listOf(false, true)) {
            runCatching {
                target.value = s; dark.value = d
                rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
                rule.onRoot().captureRoboImage(filePath = "build/sweep/${s.name}_${if (d) "dark" else "light"}.png")
                stuck = 0
            }.onFailure {
                println("SWEEP_FAIL ${s.name} $d ${it.message?.take(200)}")
                failures += "${s.name}/${if (d) "dark" else "light"}: ${it.javaClass.simpleName} ${it.message?.take(160)}"
                if (++stuck >= 2) break@loop
            }
        }
        org.junit.Assert.assertTrue("Theme sweep failed:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
