package com.example

import com.example.data.OfflinePrayerCalculator
import com.example.ui.viewmodel.CalculationMethod
import com.example.ui.viewmodel.Madhab
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class OfflinePrayerCalculatorTest {
    private val date = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(2026, 8, 7, 6, 0)
    }.time
    private fun times(offset: Double = 5.5, madhab: Madhab = Madhab.STANDARD) =
        OfflinePrayerCalculator.calculate(date, 12.9716, 77.5946, offset, CalculationMethod.KARACHI, madhab)
    private fun minutes(value: String): Int = value.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    @Test fun sunriseAndSunsetAreNotCollapsedAroundNoon() {
        val values = times().values.map(::minutes)
        assertTrue(values.zipWithNext().all { (earlier, later) -> earlier < later })
        assertTrue(values[1] in 5 * 60..7 * 60) // Bengaluru in September
        assertTrue(values[4] in 17 * 60..19 * 60)
        assertTrue(values[4] - values[1] > 10 * 60)
    }
    @Test fun hanafiChangesAsrWithoutChangingTheOtherPrayers() {
        val standard = times()
        val hanafi = times(madhab = Madhab.HANAFI)
        assertTrue(minutes(hanafi.getValue("Asr")) > minutes(standard.getValue("Asr")))
        assertEquals(standard - "Asr", hanafi - "Asr")
    }
    @Test fun requestedUtcOffsetIsApplied() {
        val original = times()
        val shifted = times(offset = 6.5)
        original.keys.forEach { assertEquals(60, minutes(shifted.getValue(it)) - minutes(original.getValue(it))) }
    }
    @Test fun arabicLocaleKeepsAlarmTimesMachineReadable() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"))
            assertTrue(times().values.all { it.matches(Regex("[0-9]{2}:[0-9]{2}")) })
        } finally { Locale.setDefault(original) }
    }
}
