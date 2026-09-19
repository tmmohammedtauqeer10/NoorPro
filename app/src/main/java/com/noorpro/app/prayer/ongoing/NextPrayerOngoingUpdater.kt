package com.noorpro.app.prayer.ongoing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.noorpro.app.MainActivity
import com.noorpro.app.R
import com.noorpro.app.data.UserPreferencesRepository
import com.noorpro.app.ui.viewmodel.PrayerSettingsController
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Posts / updates the quiet ongoing "Next: Maghrib · 01:24" notification.
 * Never uses the adhan HIGH channel.
 *
 * Refresh improvements:
 * - Chronometer-style countdown when possible ([NotificationCompat.setWhen] +
 *   [NotificationCompat.setUsesChronometer] counting down).
 * - [setOnlyAlertOnce] + silent + LOW channel to avoid noise on each tick.
 * - Scheduler enqueues a near-prayer one-shot so the shade advances promptly.
 */
object NextPrayerOngoingUpdater {
    const val PREFS = "prayer_settings_prefs"
    const val KEY_ONGOING_ENABLED = "ongoing_next_prayer_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONGOING_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONGOING_ENABLED, enabled)
            .apply()
        if (!enabled) {
            NotificationManagerCompat.from(context)
                .cancel(PrayerNotificationChannels.ONGOING_NOTIFICATION_ID)
            NextPrayerOngoingScheduler.cancel(context)
        } else {
            NextPrayerOngoingScheduler.schedule(context)
        }
    }

    /**
     * @return milliseconds until the next prayer (0 if unknown / location missing).
     * Used by the scheduler to enqueue a near-prayer refresh.
     */
    suspend fun update(context: Context): Long {
        PrayerNotificationChannels.ensureAll(context)
        if (!isEnabled(context)) {
            NotificationManagerCompat.from(context)
                .cancel(PrayerNotificationChannels.ONGOING_NOTIFICATION_ID)
            return 0L
        }

        val location = UserPreferencesRepository(context).locationFlow.first()
        if (!location.isAvailable) {
            post(context, "Next prayer", "Waiting for location…", null, 0L)
            return 0L
        }

        val controller = PrayerSettingsController(context)
        val now = Date()
        val prayers = controller.calculatePrayers(
            date = now,
            latitude = location.latitude,
            longitude = location.longitude,
            schedule = false,
        ).filter { it.name != "Sunrise" }

        val next = prayers.firstOrNull { prayer ->
            parseTodayTime(prayer.time)?.after(now) == true
        } ?: run {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
            controller.calculatePrayers(
                date = tomorrow,
                latitude = location.latitude,
                longitude = location.longitude,
                schedule = false,
            ).firstOrNull { it.name == "Fajr" }
        }

        if (next == null) {
            post(context, "Next prayer", "Unable to calculate", null, 0L)
            return 0L
        }

        val prayerDate = parseTodayTime(next.time) ?: now
        val target = if (!prayerDate.after(now) && next.name == "Fajr") {
            Calendar.getInstance().apply {
                time = prayerDate
                add(Calendar.DAY_OF_YEAR, 1)
            }.time
        } else prayerDate

        val remainingMs = (target.time - System.currentTimeMillis()).coerceAtLeast(0L)
        val countdown = formatCountdown(remainingMs)
        val title = "Next: ${next.name} · $countdown"
        val body = "${next.time} · Local"
        post(context, title, body, next.name, remainingMs)
        return remainingMs
    }

    private fun post(
        context: Context,
        title: String,
        body: String,
        prayerName: String?,
        remainingMs: Long,
    ) {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PRAYER", true)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getActivity(context, 71001, open, flags)

        val builder = NotificationCompat.Builder(context, PrayerNotificationChannels.ONGOING)
            .setSmallIcon(R.drawable.ic_adhan_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pi)
            .setColor(0xFF0E8C73.toInt())
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Live countdown in the shade when we know the target time.
        if (remainingMs > 0L) {
            builder
                .setWhen(System.currentTimeMillis() + remainingMs)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }

        try {
            NotificationManagerCompat.from(context)
                .notify(PrayerNotificationChannels.ONGOING_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS may be denied — ignore
        }
    }

    private fun parseTodayTime(hhmm: String): Date? {
        return try {
            val parts = hhmm.split(":")
            if (parts.size < 2) return null
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        } catch (_: Exception) {
            null
        }
    }

    private fun formatCountdown(ms: Long): String {
        val totalMin = TimeUnit.MILLISECONDS.toMinutes(ms)
        val hours = totalMin / 60
        val mins = totalMin % 60
        return String.format(Locale.US, "%02d:%02d", hours, mins)
    }
}
