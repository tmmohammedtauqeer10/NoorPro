package com.noorpro.app.prayer.channels

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.noorpro.app.prayer.AdhanSound

/**
 * Prayer notification channels. Android freezes a channel's sound/importance after it is first
 * created, so every channel here is created ONCE with its final settings (the previous *_v2
 * channels were created without a sound and then could never be fixed in code; they are deleted
 * and replaced by *_v3 ids).
 */
object PrayerNotificationChannels {
    /** Adhan at prayer time using the system alarm tone (heads-up, plays like an alarm). */
    const val ADHAN_ALARM = "prayer_adhan_alarm_v3"
    /** Adhan at prayer time using the default notification tone. */
    const val ADHAN_SOFT = "prayer_adhan_soft_v3"
    /** Adhan heads-up with vibration only. */
    const val ADHAN_SILENT = "prayer_adhan_silent_v3"
    /** Pre-prayer, Jummah, Ramadan reminders. */
    const val REMINDER = "prayer_reminder_v3"
    /** Daily ayah / dua. */
    const val DAILY = "prayer_daily_v1"
    /** Quiet ongoing next-prayer row. */
    const val ONGOING = "prayer_ongoing_v1"

    @Deprecated("old channel ids, deleted at startup")
    private val LEGACY = listOf("prayer_adhan_channel_v2", "prayer_notification_channel_v2")

    const val ONGOING_NOTIFICATION_ID = 71001

    fun adhanChannelFor(sound: AdhanSound): String = when (sound) {
        AdhanSound.ALARM -> ADHAN_ALARM
        AdhanSound.NOTIFICATION -> ADHAN_SOFT
        AdhanSound.SILENT -> ADHAN_SILENT
    }

    fun ensureAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        @Suppress("DEPRECATION")
        LEGACY.forEach { runCatching { nm.deleteNotificationChannel(it) } }

        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val notifAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val pattern = longArrayOf(0, 350, 250, 350)

        nm.createNotificationChannel(
            NotificationChannel(ADHAN_ALARM, "Prayer time (alarm tone)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pop-up and alarm tone at each prayer time"
                setSound(alarmUri, alarmAttrs)
                enableVibration(true)
                vibrationPattern = pattern
                enableLights(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(ADHAN_SOFT, "Prayer time (notification tone)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pop-up and notification tone at each prayer time"
                setSound(notifUri, notifAttrs)
                enableVibration(true)
                vibrationPattern = pattern
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(ADHAN_SILENT, "Prayer time (vibrate only)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pop-up with vibration only at each prayer time"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = pattern
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(REMINDER, "Prayer & Ramadan reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pre-prayer reminder, Jumu'ah and Ramadan (Suhoor / Iftar) reminders"
                setSound(notifUri, notifAttrs)
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(DAILY, "Daily ayah & dua", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "One short ayah or dua each day"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(ONGOING, "Prayer status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ongoing row in the notification shade: next prayer + countdown"
                setShowBadge(false)
            }
        )
    }
}
