package com.noorpro.app.v111

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.noorpro.app.islamic.deen.*
import com.noorpro.app.ui.islamic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V111DeenGoalsRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val t = LocalDate.of(2026, 10, 28).toEpochDay()
    private val cfg = DeenGoalsConfig()
    private val days = mapOf(
        t to DeenDay(t, 0b00111, 1, setOf(DeenGoal.MORNING, DeenGoal.SADAQAH)),
        t - 1 to DeenDay(t - 1, 0b11111, 2, setOf(DeenGoal.MORNING, DeenGoal.EVENING, DeenGoal.DUA, DeenGoal.SADAQAH)),
        t - 2 to DeenDay(t - 2, 0b11011, 3, setOf(DeenGoal.MORNING, DeenGoal.DUA)),
        t - 3 to DeenDay(t - 3, 0b11111, 2, setOf(DeenGoal.EVENING)),
        t - 5 to DeenDay(t - 5, 0b01111, 1, setOf(DeenGoal.MORNING, DeenGoal.EVENING)),
        t - 6 to DeenDay(t - 6, 0b11111, 4, setOf(DeenGoal.MORNING, DeenGoal.EVENING, DeenGoal.SADAQAH, DeenGoal.DUA)),
    )
    private fun ui(d: Map<Long, DeenDay> = days, c: DeenGoalsConfig = cfg) =
        DeenTodayUi(d[t] ?: DeenDay(t), c, DeenGoalsLogic.streak(d, c, t), DeenGoalsLogic.week(d, c, t))

    @Test fun todayLight() {
        var salah = -1; var pages = 0; var toggled: DeenGoal? = null
        rule.show(false) { DeenTodayContent(ui(), {}, {}, {}, { salah = it }, { pages += it }, { toggled = it }) }
        rule.onNodeWithTag("deen_salah_3").performClick(); assertEquals(3, salah)
        rule.onNodeWithTag("deen_pages_plus").performClick(); assertEquals(1, pages)
        assertEquals(null, toggled)
        rule.shot("v111_deen_today_light")
    }
    @Test fun todayDark() {
        rule.show(true) { DeenTodayContent(ui(), {}, {}, {}, {}, {}, {}) }
        rule.onNodeWithTag("deen_streak").assertTextContains("days of showing up", substring = true)
        rule.shot("v111_deen_today_dark")
    }
    @Test fun todayBottomLight() {
        var week = false; var toggled: DeenGoal? = null
        rule.show(false) { DeenTodayContent(ui(), {}, { week = true }, {}, {}, {}, { toggled = it }) }
        rule.onNodeWithTag("deen_goal_dua").performScrollTo().performClick(); assertEquals(DeenGoal.DUA, toggled)
        rule.onNodeWithTag("deen_week_open").performScrollTo().performClick(); assertTrue(week)
        rule.shot("v111_deen_today_bottom_light")
    }
    @Test fun freshStartDark() {
        rule.show(true) { DeenTodayContent(ui(emptyMap()), {}, {}, {}, {}, {}, {}) }
        rule.onNodeWithText("Every day is a fresh start", substring = true).assertExists()
        rule.shot("v111_deen_fresh_dark")
    }
    @Test fun weekLight() {
        var asked = false
        rule.show(false) { DeenWeekContent(ui().week, DeenAiState.Idle, {}) { asked = true } }
        rule.onNodeWithTag("deen_ai_ask").performScrollTo().performClick(); assertTrue(asked)
        rule.shot("v111_deen_week_light")
    }
    @Test fun weekAiDark() {
        val text = "You kept coming back to Allah on six days this week — that steadiness is beautiful. The Prophet ﷺ said the most beloved deeds to Allah are the most consistent, even if small. Keep your steps gentle and sincere."
        rule.show(true) { DeenWeekContent(ui().week, DeenAiState.Text(text), {}) {} }
        rule.onNodeWithTag("deen_ai_text").performScrollTo().assertExists()
        rule.shot("v111_deen_week_ai_dark")
    }
    @Test fun weekOfflineLight() {
        rule.show(false) { DeenWeekContent(ui().week, DeenAiState.Unavailable, {}) {} }
        rule.onNodeWithTag("deen_ai_off").performScrollTo().assertExists()
        rule.shot("v111_deen_week_offline_light")
    }
    @Test fun settingsDark() {
        var changed: DeenGoalsConfig? = null
        rule.show(true) { DeenGoalsSettingsContent(cfg.copy(enabled = cfg.enabled - DeenGoal.SADAQAH), {}) { changed = it } }
        rule.onNodeWithTag("deen_target_plus").performClick(); assertEquals(3, changed?.quranPages)
        rule.onNodeWithTag("deen_switch_sadaqah").performScrollTo().performClick(); assertTrue(DeenGoal.SADAQAH in changed!!.enabled)
        rule.shot("v111_deen_settings_dark")
    }
}
