package com.noorpro.app.v111

import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.calendar.*
import com.noorpro.app.ui.screens.Reels2Flags
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IslamicCalendarLogicTest {
    @After fun reset() { Reels2Flags.override = null }

    @Test fun flagsRegisteredAndDefaultOff() {
        IslamicFlags.ALL.forEach {
            assertTrue("$it registered alongside reels_v2_enabled", it in Reels2Flags.ALL_KEYS)
            assertFalse("$it defaults OFF", Reels2Flags.default(it))
            assertFalse(IslamicFlags.isOn(it))
        }
        Reels2Flags.override = mapOf(IslamicFlags.CALENDAR to true)
        assertTrue(IslamicFlags.isOn(IslamicFlags.CALENDAR))
        assertFalse(IslamicFlags.isOn(IslamicFlags.GROUP_KHATM))
    }

    @Test fun moreCatalogHidesFlaggedItemsUntilOn() {
        val item = com.noorpro.app.ui.screens.moreCatalog.flatMap { it.items }.first { it.flag == IslamicFlags.CALENDAR }
        assertFalse(item.visible)
        Reels2Flags.override = mapOf(IslamicFlags.CALENDAR to true)
        assertTrue(item.visible)
    }

    @Test fun catalogHasAllKeyEvents() {
        val ids = IslamicEventCatalog.events.map { it.id }.toSet()
        listOf("new_year", "ashura", "milad", "meraj", "barat", "ramadan", "qadr21", "qadr23", "qadr25", "qadr27", "qadr29", "eid_fitr", "arafah", "eid_adha")
            .forEach { assertTrue(it, it in ids) }
        assertEquals(10, IslamicEventCatalog.byId("ashura")!!.day)
        assertEquals(1 to 10, IslamicEventCatalog.byId("eid_fitr")!!.let { it.month to it.day }.let { it.second to it.first })
        assertTrue(IslamicEventCatalog.events.all { it.acts.isNotEmpty() && it.summary.isNotBlank() })
        assertTrue(IslamicEventCatalog.events.filter { it.id.startsWith("qadr") }.all { it.nightBefore && it.month == 9 && it.day % 2 == 1 })
    }

    @Test fun ummAlQuraKnownDates() {
        val c = AdjustedHijri(HijriConverter.UmmAlQura, 0)
        assertEquals(HijriDate(1447, 9, 1), c.hijri(epochDayOf(2026, 2, 18)))   // 1 Ramadan 1447
        assertEquals(HijriDate(1447, 10, 1), c.hijri(epochDayOf(2026, 3, 20)))  // Eid al-Fitr 1447
        assertEquals(HijriDate(1447, 12, 10), c.hijri(epochDayOf(2026, 5, 27))) // Eid al-Adha 1447
    }

    @Test fun adjustmentShiftsDates() {
        val ed = epochDayOf(2026, 3, 20)
        val base = AdjustedHijri(TabularHijri, 0).hijri(ed)
        val plus = AdjustedHijri(TabularHijri, 1).hijri(ed)
        val minus = AdjustedHijri(TabularHijri, -1).hijri(ed)
        assertEquals(AdjustedHijri(TabularHijri, 0).hijri(ed + 1), plus)
        assertEquals(AdjustedHijri(TabularHijri, 0).hijri(ed - 1), minus)
        assertNotEquals(base, plus)
        assertEquals(2, AdjustedHijri(TabularHijri, 7).adjustment)  // clamped
        assertEquals(-2, AdjustedHijri(TabularHijri, -9).adjustment)
        // Shifting +1 moves an event one Gregorian day earlier.
        val from = epochDayOf(2026, 1, 1)
        val e0 = IslamicCalendarLogic.nextOf(AdjustedHijri(TabularHijri, 0), "eid_fitr", from)!!
        val e1 = IslamicCalendarLogic.nextOf(AdjustedHijri(TabularHijri, 1), "eid_fitr", from)!!
        assertEquals(e0.epochDay - 1, e1.epochDay)
    }

    @Test fun upcomingIsOrderedAndCoversAYear() {
        val cal = AdjustedHijri(TabularHijri, 0)
        val list = IslamicCalendarLogic.upcoming(cal, epochDayOf(2026, 10, 28))
        assertEquals(list.sortedBy { it.epochDay }, list)
        assertTrue(IslamicEventCatalog.events.all { e -> list.any { it.event.id == e.id } })
        list.forEach { assertEquals(it.event.month, it.hijri.month); assertEquals(it.event.day, it.hijri.day) }
        val qadr27 = list.first { it.event.id == "qadr27" }
        assertEquals(qadr27.epochDay - 1, qadr27.observeEpochDay)
    }

    @Test fun monthGridShape() {
        val cal = AdjustedHijri(TabularHijri, 0)
        val ed = epochDayOf(2026, 10, 28)
        val m = IslamicCalendarLogic.monthGrid(cal, ed)
        val days = m.cells.filterNotNull()
        assertTrue(days.size == 29 || days.size == 30)
        assertEquals((1..days.size).toList(), days.map { it.hijriDay })
        assertEquals(utcCalendar(m.startEpochDay).get(java.util.Calendar.DAY_OF_WEEK) - 1, m.cells.takeWhile { it == null }.size)
        assertTrue(days.any { it.epochDay == ed })
        // Ramadan month grid shows the Laylatul Qadr nights.
        val ram = IslamicCalendarLogic.nextOf(cal, "ramadan", ed)!!
        val rm = IslamicCalendarLogic.monthGrid(cal, ram.epochDay)
        assertEquals(9, rm.month)
        assertEquals(listOf(1, 21, 23, 25, 27, 29), rm.cells.filterNotNull().filter { it.events.isNotEmpty() }.map { it.hijriDay })
        // Month navigation round-trips.
        val next = IslamicCalendarLogic.shiftMonth(cal, ed, 1)
        assertEquals(m.startEpochDay, IslamicCalendarLogic.shiftMonth(cal, next, -1))
        assertEquals(1, cal.hijri(next).day)
        assertEquals(m.month % 12 + 1, cal.hijri(next).month)
        assertEquals(m.startEpochDay, IslamicCalendarLogic.shiftMonth(cal, IslamicCalendarLogic.shiftMonth(cal, ed, 13), -13))
    }

    @Test fun labelsAndCountdown() {
        assertEquals("Today", IslamicCalendarLogic.countdown(0))
        assertEquals("Tomorrow", IslamicCalendarLogic.countdown(1))
        assertEquals("In 5 days", IslamicCalendarLogic.countdown(5))
        assertEquals("In 3 weeks", IslamicCalendarLogic.countdown(21))
        assertEquals("In 4 months", IslamicCalendarLogic.countdown(125))
        assertEquals("Wed, 28 Oct 2026", IslamicCalendarLogic.gregorianLabel(epochDayOf(2026, 10, 28)))
        assertEquals("+1 day", IslamicCalendarLogic.adjustmentLabel(1))
        assertEquals("-2 days", IslamicCalendarLogic.adjustmentLabel(-2))
    }

    @Test fun reminderTiming() {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = AdjustedHijri(TabularHijri, 0)
        val from = epochDayOf(2026, 10, 28)
        val night = IslamicCalendarLogic.nextOf(cal, "barat", from)!!
        val day = IslamicCalendarLogic.nextOf(cal, "eid_adha", from)!!
        val nAt = EventReminderTiming.fireAtMs(night, tz)
        val dAt = EventReminderTiming.fireAtMs(day, tz)
        // 17:00 IST on the evening before the 15th = 11:30 UTC.
        assertEquals((night.epochDay - 1) * 86_400_000L + 11 * 3_600_000L + 30 * 60_000L, nAt)
        // 20:00 IST the evening before = 14:30 UTC.
        assertEquals((day.epochDay - 1) * 86_400_000L + 14 * 3_600_000L + 30 * 60_000L, dAt)
        assertTrue(EventReminderTiming.title(night).endsWith("begins tonight"))
        assertTrue(EventReminderTiming.title(day).endsWith("is tomorrow"))
    }

    @Test fun reminderStorePersists() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val s = EventReminderStore(ctx)
        s.set("ashura", true); s.set("eid_fitr", true); s.set("ashura", false)
        assertEquals(setOf("eid_fitr"), EventReminderStore(ctx).enabled())
    }
}
