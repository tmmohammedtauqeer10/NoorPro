package com.noorpro.app.prayer.channels

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Prayer notification channels per docs/PRAYER_NOTIFICATIONS_WIDGETS.md.
 * Adhan HIGH and soft DEFAULT already used by PrayerAlarmReceiver; this
 * ensures the quiet ongoing LOW channel also exists at app start.
 */
object PrayerNotificationChannels {
    const val ADHAN = "prayer_adhan_channel_v2"
    const val SOFT = "prayer_notification_channel_v2"
    const val ONGOING = "prayer_ongoing_channel_v1"

    const val ONGOING_NOTIFICATION_ID = 71001

    fun ensureAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(ADHAN, "Prayer Adhan", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Exact prayer / pre-adhan alarm"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(SOFT, "Prayer Notifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Soft prayer reminders"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(ONGOING, "Prayer status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ongoing shade row: next prayer + countdown"
                setShowBadge(false)
            }
        )
    }
}
