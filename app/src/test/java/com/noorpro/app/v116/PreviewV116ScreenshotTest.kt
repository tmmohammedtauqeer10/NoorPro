package com.noorpro.app.v116

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

    @Test fun adhkar() {
        val vm = DeenViewModel(ctx)
        AzkarNav.initialTab = "morning"; show { AzkarScreen(vm) }; cap("adhkar_1_morning")
        rule.onNodeWithTag("adhkar_tap_m_quls").performClick(); rule.onNodeWithTag("adhkar_tap_m_quls").performClick(); cap("adhkar_1b_morning_counting")
        AzkarNav.initialTab = "evening"; show(isDark = true) { AzkarScreen(vm) }
        rule.onNodeWithTag("adhkar_lang_ur").performClick(); cap("adhkar_2_evening_urdu_dark")
        AzkarNav.initialTab = "after_salah"; show { AzkarScreen(vm) }; cap("adhkar_3_after_salah")
        AzkarNav.initialTab = "daily"; show { AzkarScreen(vm) }
        rule.onNodeWithTag("adhkar_lang_hi").performClick(); cap("adhkar_4_daily_hindi")
    }

    @Test fun tasawwufPath() {
        val p = TasawwufPath.load(ctx)
        val first = p.ordered.first().id
        show { TasawwufPathContent(p, PathPage.HOME, null, setOf(first), null, TasawwufPathActions()) }; cap("tasawwuf_1_path_home")
        show { TasawwufPathContent(p, PathPage.LESSON, first, emptySet(), null, TasawwufPathActions()) }; cap("tasawwuf_2_lesson")
        show { TasawwufPathContent(p, PathPage.LESSON, first, setOf(first), p.lesson(first)?.check?.answer, TasawwufPathActions()) }; cap("tasawwuf_3_lesson_checked")
        show(isDark = true) { TasawwufPathContent(p, PathPage.GLOSSARY, null, emptySet(), null, TasawwufPathActions()) }; cap("tasawwuf_4_glossary_dark")
        show { TasawwufPathContent(p, PathPage.BOOKS, null, emptySet(), null, TasawwufPathActions()) }; cap("tasawwuf_5_books")
        show { TasawwufPathContent(p, PathPage.FAQ, null, emptySet(), null, TasawwufPathActions()) }; cap("tasawwuf_6_faq")
    }

    @Test fun sections() {
        val vm = DeenViewModel(ctx)
        show { SpiritualContentScreen(vm, "tasawwuf") }; cap("section_1_tasawwuf_entry")
        show { SpiritualContentScreen(vm, "darood") }; cap("section_2_darood")
        runCatching { rule.onNodeWithText("Friday (Jumu'ah) Darood", substring = true).performClick(); rule.mainClock.advanceTimeBy(800); cap("section_3_darood_friday") }
        runCatching { rule.onNodeWithText("Send abundant salawat on Friday", substring = true).performClick(); rule.mainClock.advanceTimeBy(800); cap("section_4_darood_friday_item") }
        show { SpiritualContentScreen(vm, "ruhani") }; cap("section_5_ruhani")
        runCatching { rule.onNodeWithText("Qasida Burda", substring = true).performClick(); rule.mainClock.advanceTimeBy(800)
            rule.onNodeWithText("Burda, Chapter 1", substring = true).performClick(); rule.mainClock.advanceTimeBy(800); cap("section_6_burda_ch1") }
    }
}
