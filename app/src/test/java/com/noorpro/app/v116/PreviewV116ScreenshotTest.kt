package com.noorpro.app.v116

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.data.spiritual.TasawwufPath
import com.noorpro.app.ui.islamic.PathPage
import com.noorpro.app.ui.islamic.TasawwufPathActions
import com.noorpro.app.ui.islamic.TasawwufPathContent
import com.noorpro.app.ui.screens.AzkarNav
import com.noorpro.app.ui.screens.AzkarScreen
import com.noorpro.app.ui.screens.SpiritualContentScreen
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.noorpro.app.ui.viewmodel.DeenViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 1.2.0 (116) previews -> build/v116/<name>.png */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class PreviewV116ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val content = mutableStateOf<@Composable () -> Unit>({})
    private val gen = mutableIntStateOf(0)
    private val dark = mutableStateOf(false)
    private var started = false

    private fun show(isDark: Boolean = false, c: @Composable () -> Unit) {
        content.value = c; dark.value = isDark; gen.intValue++
        if (!started) { started = true; rule.setContent { MyApplicationTheme(darkTheme = dark.value) { key(gen.intValue) { content.value() } } } }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
    }
    private fun cap(name: String) { rule.waitForIdle(); rule.onRoot().captureRoboImage(filePath = "build/v116/$name.png") }

    /** The 5 key screens only (usage-conscious). */
    @Test fun keyScreens() {
        val vm = DeenViewModel(ctx)
        AzkarNav.initialTab = "morning"; show { AzkarScreen(vm) }
        cap("1_adhkar_morning")
        AzkarNav.initialTab = "daily"; show { AzkarScreen(vm) }
        rule.onNodeWithTag("adhkar_lang_hi").performClick(); cap("2_adhkar_daily_hindi")
        val p = TasawwufPath.load(ctx); val first = p.ordered.first().id
        show { TasawwufPathContent(p, PathPage.HOME, null, setOf(first), null, TasawwufPathActions()) }; cap("3_tasawwuf_path_home")
        show { TasawwufPathContent(p, PathPage.LESSON, first, setOf(first), p.lesson(first)?.check?.answer, TasawwufPathActions()) }; cap("4_tasawwuf_lesson_check")
        show { SpiritualContentScreen(vm, "darood") }; cap("5_darood_with_friday")
    }
}
