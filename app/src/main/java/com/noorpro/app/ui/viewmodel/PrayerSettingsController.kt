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

enum class CalculationMethod {
    KARACHI,
    MWL
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

    private val _alarmSound = MutableStateFlow(prefs.getString("alarm_sound", "Mecca Adhan") ?: "Mecca Adhan")
    val alarmSound: StateFlow<String> = _alarmSound.asStateFlow()

    fun updateAlarmSound(sound: String) {
        prefs.edit().putString("alarm_sound", sound).apply()
        _alarmSound.value = sound
        CoroutineScope(Dispatchers.IO).launch { userPrefsRepo.updateAlarmSound(sound) }
    }

    fun isAlarmEnabled(prayerName: String): Boolean {
        return prefs.getBoolean("alarm_enabled_$prayerName", false)
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
        schedule: Boolean = true
    ): List<PrayerTime> {
        val times = com.noorpro.app.data.OfflinePrayerCalculator.calculate(
            date, latitude, longitude, timeZone, _selectedMethod.value, _selectedMadhab.value
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

    fun scheduleAlarms(prayers: List<PrayerTime>, date: Date = Date()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val now = Calendar.getInstance()

        for (prayer in prayers) {
            // Sunrise is informational only — never schedule an Adhan for it.
            if (prayer.name.equals("Sunrise", ignoreCase = true)) continue

            val intent = android.content.Intent(context, com.noorpro.app.receiver.PrayerAlarmReceiver::class.java).apply {
                putExtra("PRAYER_NAME", prayer.name)
                putExtra("ALARM_SOUND", _alarmSound.value)
                putExtra("REMINDER_OFFSET", getReminderOffset(prayer.name))
            }
            // Use prayer name hash code as a simple unique request code
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                prayer.name.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel existing alarm
            alarmManager.cancel(pendingIntent)

            if (prayer.isNotificationEnabled) {
                val timeParts = prayer.time.split(":")
                if (timeParts.size == 2) {
                    val hour = timeParts[0].toIntOrNull() ?: continue
                    val min = timeParts[1].toIntOrNull() ?: continue

                    val offset = getReminderOffset(prayer.name)

                    val alarmTime = Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, min)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.MINUTE, -offset) // subtract offset (e.g. 10 mins early)
                    }

                    // If time already passed today, schedule for tomorrow
                    if (alarmTime.before(now)) {
                        alarmTime.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    // Preserve the prayer's calendar day even when an early reminder
                    // crosses midnight; the receiver uses it to schedule the next day.
                    val prayerDay = (alarmTime.clone() as Calendar).apply { add(Calendar.MINUTE, offset) }
                    intent.putExtra("PRAYER_DATE", prayerDay.timeInMillis)
                    android.app.PendingIntent.getBroadcast(
                        context, prayer.name.hashCode(), intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    android.app.AlarmManager.RTC_WAKEUP,
                                    alarmTime.timeInMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setAndAllowWhileIdle(
                                    android.app.AlarmManager.RTC_WAKEUP,
                                    alarmTime.timeInMillis,
                                    pendingIntent
                                )
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                android.app.AlarmManager.RTC_WAKEUP,
                                alarmTime.timeInMillis,
                                pendingIntent
                            )
                        }
                    } catch (e: Exception) {
                        CrashReporter.report(e)
                    }
                }
            }
        }
    }
}
