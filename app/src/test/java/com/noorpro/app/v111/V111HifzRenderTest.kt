package com.noorpro.app.v111

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.islamic.hifz.*
import com.noorpro.app.quran.HifzCard
import com.noorpro.app.ui.islamic.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V111HifzRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val surahs by lazy { HifzQuran.surahs(ctx) }
    private val today = 20_000L
    private fun home(): HifzHomeUi {
        val cards = mutableMapOf<Pair<Int, Int>, HifzCard>()
        (1..7).forEach { cards[1 to it] = HifzCard(4, 12, 2.7, today + it) }
        (1..4).forEach { cards[112 to it] = HifzCard(1, 1, 2.5, if (it < 4) today else today + 1) }
        (1..3).forEach { cards[67 to it] = HifzCard(2, 3, 2.5, today - 1) }
        return HifzHomeUi(HifzPlanner.plan(cards, today), 6, 14, HifzRange(67, 1, 5), surahs[66], 5)
    }
    private fun practice(range: HifzRange, idx: Int = 0) = HifzPracticeUi(surahs[range.surah - 1], range, HifzQuran.ayahs(ctx, range), index = idx)

    @Test fun homeLight() {
        rule.show(false) { HifzHomeContent(home(), surahs, {}, {}, {}, {}, {}, {}) }
        rule.onNodeWithTag("hifz_due_67").assertExists()
        rule.shot("v111_hifz_home_light")
    }
    @Test fun homeDark() {
        rule.show(true) { HifzHomeContent(home(), surahs, {}, {}, {}, {}, {}, {}) }
        rule.onNodeWithTag("hifz_home").performScrollToNode(hasTestTag("hifz_start"))
        rule.shot("v111_hifz_home_dark")
    }
    @Test fun readLevelLight() {
        rule.show(false) { HifzPracticeContent(practice(HifzRange(2, 255, 255)).copy(repeats = 2), HifzActions()) }
        rule.shot("v111_hifz_read_light")
    }
    @Test fun hiddenLevelDark() {
        var revealed = -1
        rule.show(true) { HifzPracticeContent(practice(HifzRange(2, 255, 255)).copy(level = 2, repeats = 4, revealed = setOf(5)), HifzActions(onReveal = { revealed = it })) }
        rule.onNodeWithTag("hifz_hidden_1").performClick()
        assertEquals(1, revealed)
        rule.shot("v111_hifz_hidden_dark")
    }
    @Test fun memoryLevelLight() {
        rule.show(false) { HifzPracticeContent(practice(HifzRange(67, 1, 5), 1).copy(level = 4, listening = true, partial = "الذي خلق الموت والحياة"), HifzActions()) }
        rule.shot("v111_hifz_listening_light")
    }
    @Test fun resultLight() {
        val p = practice(HifzRange(67, 1, 5), 1)
        val r = RecitationChecker.check(p.ayah!!.text, "الذي خلق الموت والحياه ليبلوكم ايكم احسن عملا وهو الغفور")
        rule.show(false) { HifzPracticeContent(p.copy(level = 4, result = r, graded = r.passed, repeats = 3), HifzActions()) }
        rule.onNodeWithTag("hifz_result").assertExists()
        rule.shot("v111_hifz_result_light")
    }
    @Test fun resultDarkWithTips() {
        val p = practice(HifzRange(112, 1, 4), 0)
        val r = RecitationChecker.check(p.ayah!!.text, "قل الله احد")
        rule.show(true) { HifzPracticeContent(p.copy(level = 3, result = r, graded = false, aiTips = "1. Split the ayah into two halves and link them.\n2. Recite it in your sunnah prayers today.\n3. Revise tomorrow after Fajr before learning anything new.\n4. Ask a teacher to listen once a week."), HifzActions()) }
        rule.onNodeWithTag("hifz_practice").performScrollToNode(hasTestTag("hifz_ai"))
        rule.shot("v111_hifz_result_dark")
    }
    @Test fun micUnavailable() {
        rule.show(false) { HifzPracticeContent(practice(HifzRange(1, 1, 7), 1).copy(micAvailable = false, message = HifzRecognizer.UNAVAILABLE), HifzActions()) }
        rule.onNodeWithTag("hifz_message").assertExists()
        rule.shot("v111_hifz_nomic_light")
    }
}
