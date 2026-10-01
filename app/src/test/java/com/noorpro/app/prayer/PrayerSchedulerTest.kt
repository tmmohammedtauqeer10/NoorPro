package com.noorpro.app.prayer

import com.noorpro.app.data.OfflinePrayerCalculator
import com.noorpro.app.ui.viewmodel.CalculationMethod
import com.noorpro.app.ui.viewmodel.Madhab
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerSchedulerTest {
    private val tz = TimeZone.getTimeZone("Asia/Kolkata")
    private fun cal(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        GregorianCalendar(tz).apply { clear(); set(y, mo, d, h, mi, 0) }

    private val day = mapOf("Fajr" to "05:00", "Sunrise" to "06:10", "Dhuhr" to "12:20", "Asr" to "15:40", "Maghrib" to "18:30", "Isha" to "19:45")
    private val tomorrow = mapOf("Fajr" to "05:01", "Sunrise" to "06:11", "Dhuhr" to "12:20", "Asr" to "15:40", "Maghrib" to "18:29", "Isha" to "19:44")

    private fun plan(
        now: Calendar,
        pre: Int? = null,
        daily: Int? = null,
        jummah: Boolean = false,
        friday: Map<Int, String> = emptyMap(),
        ramadan: Set<Int> = emptySet(),
        enabled: (String) -> Boolean = { true }
    ) = PrayerScheduler.plan(
        nowMs = now.timeInMillis, notBeforeMs = 0, base = now,
        dayTimes = mapOf(0 to day, 1 to tomorrow),
        enabled = enabled, preMinutes = pre, dailyMinuteOfDay = daily,
        jummah = jummah, fridayDhuhr = friday, ramadanDays = ramadan
    )

    @Test fun adhanFiresAtTheExactPrayerMinuteNotEarly() {
        val now = cal(2026, Calendar.OCTOBER, 1, 12, 0)
        val adhan = plan(now).first { it.kind == PrayerScheduler.KIND_ADHAN && it.prayer == "Dhuhr" }
        assertEquals(cal(2026, Calendar.OCTOBER, 1, 12, 20).timeInMillis, adhan.triggerAt)
        assertTrue(adhan.exact)
    }

    @Test fun passedPrayersRollOverToTomorrow() {
        val now = cal(2026, Calendar.OCTOBER, 1, 20, 30)
        val fajr = plan(now).first { it.kind == PrayerScheduler.KIND_ADHAN && it.prayer == "Fajr" }
        assertEquals(cal(2026, Calendar.OCTOBER, 2, 5, 1).timeInMillis, fajr.triggerAt)
    }

    @Test fun disabledPrayerGetsNoAdhanButStillRefreshes() {
        val now = cal(2026, Calendar.OCTOBER, 1, 4, 0)
        val planned = plan(now) { it != "Asr" }
        assertTrue(planned.none { it.kind == PrayerScheduler.KIND_ADHAN && it.prayer == "Asr" })
        assertTrue(planned.any { it.kind == PrayerScheduler.KIND_REFRESH && it.prayer == "Asr" })
    }

    @Test fun sunriseNeverGetsAnAdhan() {
        val now = cal(2026, Calendar.OCTOBER, 1, 4, 0)
        assertTrue(plan(now).none { it.kind == PrayerScheduler.KIND_ADHAN && it.prayer == "Sunrise" })
    }

    @Test fun preReminderIsTenMinutesBefore() {
        val now = cal(2026, Calendar.OCTOBER, 1, 12, 0)
        val pre = plan(now, pre = 10).first { it.kind == PrayerScheduler.KIND_PRE && it.prayer == "Dhuhr" }
        assertEquals(cal(2026, Calendar.OCTOBER, 1, 12, 10).timeInMillis, pre.triggerAt)
    }

    @Test fun preReminderAlreadyPassedMovesToNextDay() {
        val now = cal(2026, Calendar.OCTOBER, 1, 12, 15) // 12:10 reminder is past, adhan 12:20 is not
        val planned = plan(now, pre = 10)
        val pre = planned.first { it.kind == PrayerScheduler.KIND_PRE && it.prayer == "Dhuhr" }
        assertEquals(cal(2026, Calendar.OCTOBER, 2, 12, 10).timeInMillis, pre.triggerAt)
        assertEquals(cal(2026, Calendar.OCTOBER, 1, 12, 20).timeInMillis,
            planned.first { it.kind == PrayerScheduler.KIND_ADHAN && it.prayer == "Dhuhr" }.triggerAt)
    }

    @Test fun midnightRefreshIsArmedAfterMidnight() {
        val now = cal(2026, Calendar.OCTOBER, 1, 12, 0)
        val midnight = plan(now).first { it.kind == PrayerScheduler.KIND_REFRESH && it.prayer.isEmpty() }
        assertEquals(cal(2026, Calendar.OCTOBER, 2, 0, 1).timeInMillis, midnight.triggerAt)
        assertEquals(false, midnight.exact)
    }

    @Test fun dailyAyahUsesNextOccurrence() {
        val now = cal(2026, Calendar.OCTOBER, 1, 9, 0)
        val before = plan(now, daily = 8 * 60).first { it.kind == PrayerScheduler.KIND_DAILY }
        assertEquals(cal(2026, Calendar.OCTOBER, 2, 8, 0).timeInMillis, before.triggerAt)
        val after = plan(now, daily = 20 * 60).first { it.kind == PrayerScheduler.KIND_DAILY }
        assertEquals(cal(2026, Calendar.OCTOBER, 1, 20, 0).timeInMillis, after.triggerAt)
    }

    @Test fun jummahReminderIsThirtyMinutesBeforeFridayDhuhr() {
        val now = cal(2026, Calendar.OCTOBER, 1, 9, 0) // Thursday
        val planned = plan(now, jummah = true, friday = mapOf(1 to "12:20"))
        val j = planned.first { it.kind == PrayerScheduler.KIND_JUMMAH }
        assertEquals(cal(2026, Calendar.OCTOBER, 2, 11, 50).timeInMillis, j.triggerAt)
        assertNull(plan(now, jummah = false, friday = mapOf(1 to "12:20")).firstOrNull { it.kind == PrayerScheduler.KIND_JUMMAH })
    }

    @Test fun ramadanAddsSuhoorAndIftar() {
        val now = cal(2026, Calendar.OCTOBER, 1, 20, 30)
        val planned = plan(now, ramadan = setOf(1))
        assertEquals(cal(2026, Calendar.OCTOBER, 2, 4, 31).timeInMillis, planned.first { it.kind == PrayerScheduler.KIND_SUHOOR }.triggerAt)
        assertEquals(cal(2026, Calendar.OCTOBER, 2, 18, 29).timeInMillis, planned.first { it.kind == PrayerScheduler.KIND_IFTAR }.triggerAt)
    }

    @Test fun requestCodesAreUnique() {
        val planned = plan(cal(2026, Calendar.OCTOBER, 1, 4, 0), pre = 10, daily = 480, jummah = true, friday = mapOf(1 to "12:20"), ramadan = setOf(0))
        assertEquals(planned.size, planned.map { it.requestCode }.toSet().size)
    }

    @Test fun everyNewCalculationMethodProducesOrderedTimes() {
        val date = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, 8, 7, 6, 0) }.time
        CalculationMethod.values().forEach { method ->
            val t = OfflinePrayerCalculator.calculate(date, 21.4225, 39.8262, 3.0, method, Madhab.STANDARD).values
                .map { it.split(":").let { p -> p[0].toInt() * 60 + p[1].toInt() } }
            assertTrue("$method not ordered: $t", t.zipWithNext().all { (a, b) -> a < b })
        }
    }

    @Test fun adhanSoundLegacyNamesMap() {
        assertEquals(AdhanSound.ALARM, AdhanSound.fromPref(null))
        assertEquals(AdhanSound.ALARM, AdhanSound.fromPref("Mecca Adhan"))
        assertEquals(AdhanSound.NOTIFICATION, AdhanSound.fromPref("System Sound"))
        assertEquals(AdhanSound.NOTIFICATION, AdhanSound.fromPref("Notification tone"))
        assertEquals(AdhanSound.SILENT, AdhanSound.fromPref("Silent"))
    }
}
