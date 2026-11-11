package com.noorpro.app.deen

import com.noorpro.app.data.PrayerLogEntity
import com.noorpro.app.islamic.deen.DeenDay
import com.noorpro.app.islamic.deen.DeenGoal
import com.noorpro.app.islamic.deen.DeenGoalsConfig
import com.noorpro.app.islamic.deen.DeenGoalsLogic
import com.noorpro.app.islamic.deen.SALAH_NAMES
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.canonical
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Merge of Daily Deen + Spiritual Progress: shared salah source, sunnah migration, streak, history. */
class DailyTrackerMergeTest {
    private val cfg = DeenGoalsConfig()
    private val today = 20_000L

    private fun log(day: Long, fajr: Boolean = false, dhuhr: Boolean = false, asr: Boolean = false, maghrib: Boolean = false, isha: Boolean = false) =
        PrayerLogEntity(java.time.LocalDate.ofEpochDay(day).toString(), fajr = fajr, dhuhr = dhuhr, asr = asr, maghrib = maghrib, isha = isha)

    @Test
    fun spiritualProgressRedirectsToDailyDeen() {
        assertEquals(DeenScreen.DEEN_GOALS, DeenScreen.SPIRITUAL_PROGRESS.canonical())
        assertEquals(DeenScreen.PRAYER_TIMES, DeenScreen.PRAYER_TIMES.canonical())
        assertEquals(DeenScreen.DEEN_GOALS, DeenScreen.DEEN_GOALS.canonical())
    }

    @Test
    fun prayerTicksFromTrackerAppearInMergedDay() {
        val stored = mapOf(today to DeenDay(today, pages = 2, done = setOf(DeenGoal.DUA)))
        val merged = DeenGoalsLogic.mergeStored(stored, listOf(log(today, fajr = true, isha = true)))
        val d = DeenGoalsLogic.history(merged, today)
        assertEquals(0b10001, d.salah)
        assertEquals(2, DeenGoalsLogic.salahCount(d.salah))
        assertEquals(2, d.pages)
        assertTrue(DeenGoal.DUA in d.done)
        assertEquals(0, DeenGoalsLogic.history(merged, today - 1).salah)
    }

    @Test
    fun prayerOnlyLogsStillBuildAGentleStreak() {
        val logs = listOf(
            log(today, fajr = true),
            log(today - 1, dhuhr = true),
            log(today - 2, asr = true),
        )
        val days = DeenGoalsLogic.mergeStored(emptyMap(), logs)
        assertEquals(3, DeenGoalsLogic.streak(days, cfg, today))
        val untilYesterday = DeenGoalsLogic.mergeStored(emptyMap(), logs.filter { it.date != java.time.LocalDate.ofEpochDay(today).toString() })
        assertEquals(2, DeenGoalsLogic.streak(untilYesterday, cfg, today))
    }

    @Test
    fun legacySunnahPrefsMergeWithoutClobberingStoredGoals() {
        val parsed = DeenGoalsLogic.parseLegacySunnah(
            true,
            mapOf(
                "sunnah_mode" to emptySet(),
                "sunnah_" + java.time.LocalDate.ofEpochDay(today) to setOf("Fajr", "Isha"),
                "sunnah_bad" to setOf("Fajr"),
            ),
        )
        assertTrue(parsed.mode)
        assertEquals(0b10001, parsed.byDay[today])
        assertFalse(parsed.byDay.containsKey(today - 1))
        val stored = mapOf(today to DeenDay(today, pages = 3, done = setOf(DeenGoal.SADAQAH), sunnah = 0b00010))
        val merged = DeenGoalsLogic.mergeStored(stored, listOf(log(today, dhuhr = true)), parsed.byDay)
        val d = merged[today]!!
        assertEquals(0b00010, d.salah)
        assertEquals(0b10011, d.sunnah)
        assertEquals(3, d.pages)
        assertEquals(setOf(DeenGoal.SADAQAH), d.done)
        assertEquals(3, DeenGoalsLogic.sunnahCount(d.sunnah))
        assertTrue(DeenGoalsLogic.sunnahOn(d.sunnah, 0))
        assertEquals(0b00101, DeenGoalsLogic.sunnahMask(listOf("Fajr", "asr")))
        assertEquals("Isha", SALAH_NAMES[4])
    }

    @Test
    fun completionPercentAndTappableWeekHistory() {
        val d = DeenDay(today, salah = 0b11111, pages = 2, done = setOf(DeenGoal.MORNING, DeenGoal.EVENING, DeenGoal.SADAQAH, DeenGoal.DUA))
        assertEquals(100, DeenGoalsLogic.completionPercent(d, cfg))
        assertEquals(0, DeenGoalsLogic.completionPercent(DeenDay(today), cfg))
        assertEquals(0, DeenGoalsLogic.completionPercent(d, DeenGoalsConfig(enabled = emptySet())))
        val days = mapOf(today to d, today - 2 to DeenDay(today - 2, done = setOf(DeenGoal.DUA)))
        val viewed = DeenGoalsLogic.history(days, today - 2)
        assertEquals(setOf(DeenGoal.DUA), viewed.done)
        assertEquals(16, DeenGoalsLogic.completionPercent(viewed, cfg))
        val week = DeenGoalsLogic.calendarWeek(today)
        assertEquals(7, week.size)
        val start = java.time.LocalDate.ofEpochDay(week.first())
        assertEquals(java.time.DayOfWeek.SUNDAY, start.dayOfWeek)
        assertTrue(today in week)
        val prev = DeenGoalsLogic.calendarWeek(today, -1)
        assertEquals(week.first() - 7, prev.first())
        assertEquals(week, DeenGoalsLogic.calendarWeek(week.last()))
    }
}
