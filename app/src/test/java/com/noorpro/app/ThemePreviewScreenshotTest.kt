package com.noorpro.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.data.QuranCourseRepository
import com.noorpro.app.data.QuranLearningAyah
import com.noorpro.app.data.QuranLearningSurah
import com.noorpro.app.ui.screens.*
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

/** Light/dark previews -> build/theme/theme_<screen>_<mode>.png */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class ThemePreviewScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun prep() {
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, Configuration.Builder().build())
        ctx.getSharedPreferences("onboarding", android.content.Context.MODE_PRIVATE).edit().putBoolean("permissions_intro_complete", true).putBoolean("media_notif_asked", true).commit()
        com.noorpro.app.prayer.PrayerPrefs(ctx).permissionPromptSnoozeUntil = System.currentTimeMillis() + 86_400_000L
    }

    private val dark = mutableStateOf(false)
    private fun both(name: String, content: @Composable () -> Unit) {
        rule.setContent { MyApplicationTheme(darkTheme = dark.value) { content() } }
        for (d in listOf(false, true)) {
            dark.value = d; rule.waitForIdle(); rule.mainClock.advanceTimeBy(1200); rule.waitForIdle()
            rule.onRoot().captureRoboImage(filePath = "build/theme/theme_${name}_${if (d) "dark" else "light"}.png")
        }
    }

    private val surahs = listOf(
        QuranLearningSurah(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", 7, "Meccan"),
        QuranLearningSurah(36, "سُورَةُ يسٓ", "Yaseen", 83, "Meccan"),
        QuranLearningSurah(67, "سُورَةُ ٱلْمُلْكِ", "Al-Mulk", 30, "Meccan"),
        QuranLearningSurah(112, "سُورَةُ ٱلْإِخْلَاصِ", "Al-Ikhlaas", 4, "Meccan"),
    )

    @Test fun all() {
        val c = QuranCourseRepository.load(ctx)
        val all = c.allLessons
        val stage = mutableStateOf(0)
        rule.setContent {
            MyApplicationTheme(darkTheme = dark.value) {
                when (stage.value) {
                    0 -> QuranLearningMenu(all.size, 3, surahs, false, {}, {}, {}, {}, {}, {}, {})
                    1 -> CourseHome(c, setOf("l1_letters", "l1_pairs", "l1_joined"), "l1_muqattaat", {}, {}, {}, {})
                    2 -> { val i = all.indexOfFirst { it.id == "l1_pairs" }
                        CourseLessonView(all[i], c.levels.first().title, i, all.size, false, -1, {}, {}, {}, {}, {}, {}) }
                    3 -> Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        QuranAyahCard(QuranLearningAyah(1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "Bismillaahir Rahmaanir Raheem", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", null), 1, false, {}, {})
                        QuranAyahCard(QuranLearningAyah(2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", "Alhamdu lillaahi Rabbil 'aalameen", "All praise is due to Allah, Lord of the worlds.", null), 1, true, {}, {})
                    }
                    else -> NoorAiChatContent(
                        mode = NoorAiMode.AskNoor, messages = listOf(
                            NoorAiChatItem("How do I renew my intention?", true, 1_760_000_000_000L, id = 1),
                            NoorAiChatItem("Renewing your intention keeps every deed for Allah alone.\n\n### Dua\n- **Arabic:** اللَّهُمَّ إِنِّي أَسْأَلُكَ الْإِخْلَاصَ\n- **Meaning:** O Allah, I ask You for sincerity. The letter ع is pronounced from the throat.", false, 1_760_000_060_000L, id = 2),
                        ), input = "", isTyping = false, showTip = false, revealId = null,
                        onBack = {}, onInput = {}, onSend = {}, onSuggestion = {}, onStop = {}, onModeChange = {}, onClearChat = {},
                        onDismissTip = {}, onCopy = {}, onShare = {}, onRegenerate = {}, onFeedback = { _, _ -> }, onRevealDone = {},
                    )
                }
            }
        }
        listOf("quran_learning_menu", "course_home", "lesson", "surah_learning", "noorai_chat").forEachIndexed { s, n ->
            for (d in listOf(false, true)) {
                stage.value = s; dark.value = d; rule.waitForIdle(); rule.mainClock.advanceTimeBy(1200); rule.waitForIdle()
                rule.onRoot().captureRoboImage(filePath = "build/theme/theme_${n}_${if (d) "dark" else "light"}.png")
            }
        }
    }

    @Test fun appScreens() {
        val target = mutableStateOf(DeenScreen.DASHBOARD)
        rule.setContent {
            val vm: DeenViewModel = viewModel()
            LaunchedEffect(dark.value) { vm.updateThemeMode(if (dark.value) ThemeMode.DARK else ThemeMode.LIGHT) }
            LaunchedEffect(target.value) { vm.navigateTo(target.value) }
            MyApplicationTheme(darkTheme = dark.value) { MainLayout() }
        }
        for ((s, n) in listOf(DeenScreen.TASAWWUF to "tasawwuf_reader", DeenScreen.INDOPAK_QURAN to "quran_reader")) {
            target.value = s
            for (d in listOf(true, false, true)) { dark.value = d; rule.waitForIdle(); rule.mainClock.advanceTimeBy(2500); rule.waitForIdle()
                rule.onRoot().captureRoboImage(filePath = "build/theme/theme_${n}_${if (d) "dark" else "light"}.png") }
        }
    }
}
