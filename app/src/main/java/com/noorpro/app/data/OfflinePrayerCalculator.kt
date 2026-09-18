package com.noorpro.app.data

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.noorpro.app.ui.viewmodel.CalculationMethod
import com.noorpro.app.ui.viewmodel.Madhab
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.SimpleTimeZone
import kotlin.math.roundToInt

/** Offline fallback; retains the user's selected method, madhab and UTC offset. */
internal object OfflinePrayerCalculator {
    fun calculate(
        date: Date,
        latitude: Double,
        longitude: Double,
        utcOffsetHours: Double,
        method: CalculationMethod,
        selectedMadhab: Madhab
    ): Map<String, String> {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(utcOffsetHours.isFinite() && utcOffsetHours in -14.0..14.0)
        val zone = SimpleTimeZone((utcOffsetHours * 3_600_000).roundToInt(), "Prayer timezone")
        val calendar = Calendar.getInstance(zone).apply { time = date }
        val localDate = DateComponents(
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        val parameters = when (method) {
            CalculationMethod.KARACHI -> com.batoulapps.adhan.CalculationMethod.KARACHI.parameters
            CalculationMethod.MWL -> com.batoulapps.adhan.CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        }.apply {
            this.madhab = if (selectedMadhab == Madhab.HANAFI) com.batoulapps.adhan.Madhab.HANAFI
                else com.batoulapps.adhan.Madhab.SHAFI
        }
        val times = PrayerTimes(Coordinates(latitude, longitude), localDate, parameters)
        // Keep the machine-readable HH:mm format ASCII even on Arabic-language devices.
        val formatter = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = zone }
        fun format(value: Date?): String = value?.let(formatter::format) ?: "--:--"
        return linkedMapOf(
            "Fajr" to format(times.fajr), "Sunrise" to format(times.sunrise),
            "Dhuhr" to format(times.dhuhr), "Asr" to format(times.asr),
            "Maghrib" to format(times.maghrib), "Isha" to format(times.isha)
        )
    }
}
