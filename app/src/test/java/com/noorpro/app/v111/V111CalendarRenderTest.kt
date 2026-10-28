package com.noorpro.app.v111

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.noorpro.app.islamic.calendar.*
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
class V111CalendarRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val today = epochDayOf(2026, 10, 28)
    private fun ui(adj: Int = 0, offset: Int = 0) = buildCalendarUi(HijriConverter.UmmAlQura, adj, today, offset, setOf("barat", "qadr27"))

    @Test fun monthLight() {
        var month = 0
        rule.show(false) { IslamicCalendarContent(ui(), CalendarActions(onMonth = { month += it })) }
        rule.onNodeWithTag("cal_today").assertExists()
        rule.onNodeWithTag("cal_cell_today").assertExists()
        rule.onNodeWithTag("cal_next").performClick()
        assertEquals(1, month)
        rule.shot("v111_cal_month_light")
    }
    @Test fun monthDark() { rule.show(true) { IslamicCalendarContent(ui(), CalendarActions()) }; rule.shot("v111_cal_month_dark") }
    @Test fun ramadanMonthLight() {
        // Ramadan 1448 (about 4 months ahead) with the Laylatul Qadr nights marked.
        rule.show(false) { IslamicCalendarContent(ui(offset = 4), CalendarActions()) }
        rule.onNodeWithText("Ramadan 1448").assertExists()
        rule.onNodeWithTag("cal_cell_27").assertExists()
        rule.shot("v111_cal_ramadan_light")
    }
    @Test fun eventsListDark() {
        rule.show(true) { IslamicCalendarContent(ui(adj = 1), CalendarActions()) }
        rule.onNodeWithTag("islamic_calendar").performScrollToNode(hasTestTag("cal_event_barat"))
        rule.onNodeWithTag("cal_bell_barat").assertExists()
        rule.shot("v111_cal_events_dark")
    }
    @Test fun eventsListLight() {
        rule.show(false) { IslamicCalendarContent(ui(), CalendarActions()) }
        rule.onNodeWithTag("islamic_calendar").performScrollToNode(hasTestTag("cal_event_barat"))
        rule.shot("v111_cal_events_light")
    }
    @Test fun detailLight() {
        val o = IslamicCalendarLogic.nextOf(AdjustedHijri(HijriConverter.UmmAlQura, 0), "qadr27", today)!!
        rule.show(false) { IslamicEventDetail(o, today, true, {}, {}) }
        rule.onNodeWithText("Recommended acts").assertExists()
        rule.onNodeWithTag("cal_detail_reminder").assertExists()
        rule.shot("v111_cal_detail_light")
    }
    @Test fun detailDark() {
        val o = IslamicCalendarLogic.nextOf(AdjustedHijri(HijriConverter.UmmAlQura, 0), "ashura", today)!!
        rule.show(true) { IslamicEventDetail(o, today, false, {}, {}) }
        rule.shot("v111_cal_detail_dark")
    }
}
