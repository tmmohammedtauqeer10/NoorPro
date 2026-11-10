package com.noorpro.app.v111

import com.noorpro.app.islamic.calendar.AdjustedHijri
import com.noorpro.app.islamic.calendar.EventCategory
import com.noorpro.app.islamic.calendar.IslamicCalendarLogic
import com.noorpro.app.islamic.calendar.IslamicEventCatalog
import com.noorpro.app.islamic.calendar.epochDayOf
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.canonical
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM event lookup for the merged Calendar (Islamic + Sufi). Uses TabularHijri. */
class CalendarEventLookupTest {
    private val cal = AdjustedHijri(TabularHijri, 0)
    private val from = epochDayOf(2026, 1, 1)

    @Test fun catalogIncludesVerifiedSufiDates() {
        val g = IslamicEventCatalog.byId("gyarhween")!!
        assertTrue(g.recurringMonthly)
        assertEquals(11, g.day)
        assertEquals(EventCategory.SUFI, g.category)
        (1..12).forEach { m ->
            assertTrue(g.covers(m, 11))
            assertTrue(g.listsOn(m, 11))
            assertFalse(g.covers(m, 10))
        }

        val jilani = IslamicEventCatalog.byId("urs_jilani")!!
        assertEquals(4 to 11, jilani.month to jilani.day)

        val chishti = IslamicEventCatalog.byId("urs_chishti")!!
        assertEquals(7, chishti.month)
        assertEquals(1, chishti.day)
        assertEquals(6, chishti.endDay)
        assertEquals(6, chishti.primaryDay)
        assertTrue(chishti.covers(7, 1) && chishti.covers(7, 6))
        assertFalse(chishti.covers(7, 7))
        assertTrue(chishti.listsOn(7, 1))
        assertFalse(chishti.listsOn(7, 6))

        val nizam = IslamicEventCatalog.byId("urs_nizamuddin")!!
        assertEquals("Rabi ul Akhir, not Rabi ul Awwal", 4, nizam.month)
        assertEquals(17, nizam.day)
        assertEquals(18, nizam.endDay)
        assertTrue(nizam.covers(4, 17) && nizam.covers(4, 18))
        assertFalse(nizam.covers(3, 17))
        assertFalse(nizam.covers(4, 16))

        val data = IslamicEventCatalog.byId("urs_hujwiri")!!
        assertEquals(2, data.month)
        assertEquals(18, data.day)
        assertEquals(20, data.endDay)
        (18..20).forEach { assertTrue(data.covers(2, it)) }
        assertFalse(data.covers(2, 17))

        assertEquals(3 to 3, IslamicEventCatalog.byId("urs_naqshband")!!.let { it.month to it.day })

        val raza = IslamicEventCatalog.byId("urs_raza")!!
        assertEquals(2, raza.month)
        assertEquals(23, raza.day)
        assertEquals(25, raza.endDay)
        assertEquals(25, raza.primaryDay)

        val farid = IslamicEventCatalog.byId("urs_farid")!!
        assertEquals(1, farid.month)
        assertEquals(5, farid.day)
        assertEquals(10, farid.endDay)

        val kaki = IslamicEventCatalog.byId("urs_kaki")!!
        assertEquals(3, kaki.month)
        assertEquals(12, kaki.day)
        assertEquals(14, kaki.endDay)
        assertEquals(14, kaki.primaryDay)
        assertTrue(kaki.covers(3, 13))
        assertFalse(kaki.listsOn(3, 14))
    }

    @Test fun monthlyGyarhweenAppearsOnceInUpcoming() {
        val list = IslamicCalendarLogic.upcoming(cal, from, 400)
        val monthly = list.filter { it.event.id == "gyarhween" }
        assertEquals(1, monthly.size)
        assertEquals(11, monthly.first().hijri.day)
        assertTrue(monthly.first().epochDay >= from)
    }

    @Test fun upcomingListsRangeOnStartAndOngoingMidRange() {
        val rajab3 = (0..800).map { from + it }.first { ed ->
            cal.hijri(ed).let { it.month == 7 && it.day == 3 }
        }
        val mid = IslamicCalendarLogic.upcoming(cal, rajab3, 40)
        val ongoing = mid.filter { it.event.id == "urs_chishti" }
        assertTrue(ongoing.any { it.epochDay == rajab3 && it.hijri.day == 3 })
        assertFalse(ongoing.any { it.hijri.day == 1 && it.epochDay >= rajab3 })

        val rajab1 = (0..800).map { from + it }.first { ed ->
            cal.hijri(ed).let { it.month == 7 && it.day == 1 }
        }
        val start = IslamicCalendarLogic.upcoming(cal, rajab1, 20)
        assertTrue(start.any { it.event.id == "urs_chishti" && it.hijri.day == 1 })
        val days = IslamicCalendarLogic.monthGrid(cal, rajab1).cells.filterNotNull()
            .filter { it.events.any { e -> e.id == "urs_chishti" } }
            .map { it.hijriDay }
        assertEquals((1..6).toList(), days)
    }

    @Test fun categoryFilterSeparatesIslamicAndSufi() {
        val sufi = IslamicCalendarLogic.upcoming(cal, from, 400, setOf(EventCategory.SUFI))
        assertTrue(sufi.isNotEmpty())
        assertTrue(sufi.all { it.event.category == EventCategory.SUFI })
        assertTrue(sufi.any { it.event.id == "urs_jilani" })
        assertFalse(sufi.any { it.event.id == "milad" })

        val islamic = IslamicCalendarLogic.upcoming(cal, from, 400, setOf(EventCategory.ISLAMIC))
        assertTrue(islamic.all { it.event.category == EventCategory.ISLAMIC })
        assertTrue(islamic.any { it.event.id == "milad" })
        assertFalse(islamic.any { it.event.id == "gyarhween" })
    }

    @Test fun hijriAdjustmentStillShiftsEventDay() {
        val e0 = IslamicCalendarLogic.nextOf(AdjustedHijri(TabularHijri, 0), "urs_jilani", from)!!
        val e1 = IslamicCalendarLogic.nextOf(AdjustedHijri(TabularHijri, 1), "urs_jilani", from)!!
        assertEquals(e0.epochDay - 1, e1.epochDay)
        assertEquals(4, e0.hijri.month)
        assertEquals(11, e0.hijri.day)
        assertNotEquals(e0.epochDay, e1.epochDay)
    }

    @Test fun miladStaysOnTwelfthAndKakiSharesTheDay() {
        val milad = IslamicEventCatalog.on(3, 12)
        assertTrue(milad.any { it.id == "milad" })
        assertTrue(milad.any { it.id == "urs_kaki" })
        assertEquals(1, milad.count { it.id == "milad" })
    }

    @Test fun islamicCalendarRouteCanonicalizesToCalendar() {
        assertEquals(DeenScreen.CALENDAR, DeenScreen.ISLAMIC_CALENDAR.canonical())
        assertEquals(DeenScreen.CALENDAR, DeenScreen.CALENDAR.canonical())
    }
}
