package com.noorpro.app.prayer

import android.content.Context

/** Simple typed access to the prayer/reminder preferences shared by UI, receivers and workers. */
class PrayerPrefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var preReminderEnabled: Boolean
        get() = p.getBoolean("pre_reminder_enabled", false)
        set(v) { p.edit().putBoolean("pre_reminder_enabled", v).apply() }

    var preReminderMinutes: Int
        get() = p.getInt("pre_reminder_minutes", 10).coerceIn(1, 60)
        set(v) { p.edit().putInt("pre_reminder_minutes", v.coerceIn(1, 60)).apply() }

    var jummahEnabled: Boolean
        get() = p.getBoolean("jummah_enabled", true)
        set(v) { p.edit().putBoolean("jummah_enabled", v).apply() }

    var ramadanEnabled: Boolean
        get() = p.getBoolean("ramadan_enabled", true)
        set(v) { p.edit().putBoolean("ramadan_enabled", v).apply() }

    var dailyAyahEnabled: Boolean
        get() = p.getBoolean("daily_ayah_enabled", false)
        set(v) { p.edit().putBoolean("daily_ayah_enabled", v).apply() }

    /** Minutes after local midnight for the daily ayah / dua notification (default 08:00). */
    var dailyAyahMinuteOfDay: Int
        get() = p.getInt("daily_ayah_minute", 8 * 60).coerceIn(0, 24 * 60 - 1)
        set(v) { p.edit().putInt("daily_ayah_minute", v.coerceIn(0, 24 * 60 - 1)).apply() }

    var alarmSound: String
        get() = p.getString("alarm_sound", DEFAULT_SOUND) ?: DEFAULT_SOUND
        set(v) { p.edit().putString("alarm_sound", v).apply() }

    /** Epoch millis until which the in-app permission prompt must stay quiet. */
    var permissionPromptSnoozeUntil: Long
        get() = p.getLong("perm_prompt_snooze_until", 0L)
        set(v) { p.edit().putLong("perm_prompt_snooze_until", v).apply() }

    var notificationAskCount: Int
        get() = p.getInt("notif_ask_count", 0)
        set(v) { p.edit().putInt("notif_ask_count", v).apply() }

    companion object {
        const val NAME = "prayer_settings_prefs"
        const val DEFAULT_SOUND = "Alarm tone"
        val SOUND_OPTIONS = listOf("Alarm tone", "Notification tone", "Silent")
    }
}

/** Which notification channel (and therefore sound) the adhan uses. */
enum class AdhanSound {
    ALARM, NOTIFICATION, SILENT;

    companion object {
        /** Legacy names ("Mecca Adhan", "System Sound"...) are mapped so old settings keep working. */
        fun fromPref(value: String?): AdhanSound = when (value?.trim()?.lowercase()) {
            "notification tone", "system sound" -> NOTIFICATION
            "silent" -> SILENT
            else -> ALARM
        }
    }
}
