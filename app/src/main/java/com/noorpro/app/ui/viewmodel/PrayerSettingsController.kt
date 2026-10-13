package com.noorpro.app.ui.viewmodel

import com.noorpro.app.utils.CrashReporter

import android.content.Context
import com.noorpro.app.data.PrayerTime
import com.noorpro.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.*
import kotlin.math.*

enum class Madhab {
    STANDARD,
    HANAFI
}

/** Calculation conventions. [aladhanId] is the AlAdhan API `method` id; the offline calculator maps the same names. */
enum class CalculationMethod(val aladhanId: Int, val label: String, val region: String) {
    KARACHI(1, "University of Islamic Sciences, Karachi", "Best precision for South Asia"),
    ISNA(2, "Islamic Society of North America (ISNA)", "North America"),
    MWL(3, "Muslim World League (MWL)", "Europe, Far East & global default"),
    MAKKAH(4, "Umm al-Qura University, Makkah", "Arabian Peninsula"),
    EGYPT(5, "Egyptian General Authority of Survey", "Africa, Syria, Lebanon")
}

class PrayerSettingsController(private val context: Context) {
    private val prefs = context.getSharedPreferences("prayer_settings_prefs", Context.MODE_PRIVATE)

    private val _selectedMadhab = MutableStateFlow(
        try {
            Madhab.valueOf(prefs.getString("madhab", Madhab.STANDARD.name) ?: Madhab.STANDARD.name)
        } catch (e: Exception) {
            Madhab.STANDARD
        }
    )
    val selectedMadhab: StateFlow<Madhab> = _selectedMadhab.asStateFlow()

    private val _selectedMethod = MutableStateFlow(
        try {
            CalculationMethod.valueOf(prefs.getString("method", CalculationMethod.KARACHI.name) ?: CalculationMethod.KARACHI.name)
        } catch (e: Exception) {
            CalculationMethod.KARACHI
        }
    )
    val selectedMethod: StateFlow<CalculationMethod> = _selectedMethod.asStateFlow()

    // --- CUSTOMIZABLE PRAYER ALARMS ---
    private val userPrefsRepo = com.noorpro.app.data.UserPreferencesRepository(context)

    // Older versions stored names such as "Mecca Adhan" (no custom audio ever shipped); map them
    // onto the three real choices so the settings screen shows what actually plays.
    private val _alarmSound = MutableStateFlow(
        (prefs.getString("alarm_sound", com.noorpro.app.prayer.PrayerPrefs.DEFAULT_SOUND)
            ?: com.noorpro.app.prayer.PrayerPrefs.DEFAULT_SOUND).let { saved ->
            if (saved in com.noorpro.app.prayer.PrayerPrefs.SOUND_OPTIONS) saved
            else when (com.noorpro.app.prayer.AdhanSound.fromPref(saved)) {
                com.noorpro.app.prayer.AdhanSound.NOTIFICATION -> "Notification tone"
                com.noorpro.app.prayer.AdhanSound.SILENT -> "Silent"
                else -> com.noorpro.app.prayer.PrayerPrefs.DEFAULT_SOUND
            }.also { prefs.edit().putString("alarm_sound", it).apply() }
        }
    )
    val alarmSound: StateFlow<String> = _alarmSound.asStateFlow()

    fun updateAlarmSound(sound: String) {
        prefs.edit().putString("alarm_sound", sound).apply()
        _alarmSound.value = sound
        CoroutineScope(Dispatchers.IO).launch { userPrefsRepo.updateAlarmSound(sound) }
    }

    /** The five daily prayers default to ON so a fresh install actually notifies; Sunrise never has an adhan. */
    fun isAlarmEnabled(prayerName: String): Boolean {
        if (prayerName.equals("Sunrise", ignoreCase = true)) return false
        return prefs.getBoolean("alarm_enabled_$prayerName", true)
    }

    fun setAlarmEnabled(prayerName: String, enabled: Boolean) {
        prefs.edit().putBoolean("alarm_enabled_$prayerName", enabled).apply()
        CoroutineScope(Dispatchers.IO).launch {
            userPrefsRepo.setAlarmEnabled(prayerName, enabled)
        }
    }

    fun getReminderOffset(prayerName: String): Int {
        return prefs.getInt("alarm_offset_$prayerName", 10)
    }

    fun setReminderOffset(prayerName: String, offsetMinutes: Int) {
        prefs.edit().putInt("alarm_offset_$prayerName", offsetMinutes).apply()
        CoroutineScope(Dispatchers.IO).launch {
            userPrefsRepo.setAlarmOffset(prayerName, offsetMinutes)
        }
    }

    fun updateMadhab(madhab: Madhab) {
        prefs.edit().putString("madhab", madhab.name).apply()
        _selectedMadhab.value = madhab
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        prefs.edit().putString("method", method.name).apply()
        _selectedMethod.value = method
    }

    fun calculatePrayers(
        date: Date = Date(),
        latitude: Double,
        longitude: Double,
        timeZone: Double = TimeZone.getDefault().getOffset(date.time) / 3_600_000.0,
        schedule: Boolean = true,
        allowNetwork: Boolean = false
    ): List<PrayerTime> {
        // Same source as the Prayer screen (cached AlAdhan timings), offline calculator as fallback.
        val times = com.noorpro.app.prayer.PrayerTimesProvider.timesFor(
            context, date, latitude, longitude, _selectedMethod.value, _selectedMadhab.value, timeZone,
            allowNetwork = allowNetwork
        )
        val prayersList = listOf(
            PrayerTime("Fajr", times.getValue("Fajr"), FajrColor, isAlarmEnabled("Fajr")),
            PrayerTime("Sunrise", times.getValue("Sunrise"), SunriseColor, false),
            PrayerTime("Dhuhr", times.getValue("Dhuhr"), DhuhrColor, isAlarmEnabled("Dhuhr")),
            PrayerTime("Asr", times.getValue("Asr"), AsrColor, isAlarmEnabled("Asr")),
            PrayerTime("Maghrib", times.getValue("Maghrib"), MaghribColor, isAlarmEnabled("Maghrib")),
            PrayerTime("Isha", times.getValue("Isha"), IshaColor, isAlarmEnabled("Isha"))
        )
        if (schedule) scheduleAlarms(prayersList, date)
        return prayersList
    }

    /**
     * (Re)plans every prayer alarm from the shared time source. The arguments are kept for source
     * compatibility; scheduling always uses the current preferences. Call from a background thread.
     */
    @Suppress("UNUSED_PARAMETER")
    fun scheduleAlarms(prayers: List<PrayerTime>, date: Date = Date()) {
        try {
            com.noorpro.app.prayer.PrayerScheduler.rescheduleAll(context, allowNetwork = false)
        } catch (e: Exception) {
            CrashReporter.report(e)
        }
    }
}
