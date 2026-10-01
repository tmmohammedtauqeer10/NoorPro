package com.noorpro.app.prayer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.noorpro.app.MainActivity
import com.noorpro.app.R
import com.noorpro.app.data.UserPreferencesRepository
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Builds and posts the adhan / prayer-time notification (masjid-themed deep green + gold) with the
 * prayer name + time, Gregorian and Hijri date and the user's location. Shared by the alarm receiver
 * and the "Send test notification" button so both look identical.
 */
object PrayerNotifications {
    const val TAG = "PrayerNotify"
    private const val TEST_NOTIFICATION_ID = 7199

    /** Result of an attempt to post, for logging and the health check. */
    enum class PostResult { POSTED, NOTIFICATIONS_DISABLED, CHANNEL_BLOCKED, FAILED }

    data class Content(val title: String, val dateLine: String, val location: String) {
        val bigText: String
            get() = buildString {
                append(dateLine)
                if (location.isNotBlank()) append("\n\uD83D\uDCCD ").append(location)
                append("\n\u062D\u064E\u064A\u064E\u0651 \u0639\u064E\u0644\u064E\u0649 \u0627\u0644\u0635\u064E\u0651\u0644\u064E\u0627\u0629  \u2022  Hayya 'alas-salah")
            }
    }

    /** Location string identical to the Home card; falls back to coordinates, never blank if known. */
    suspend fun resolveLocation(context: Context): String {
        val saved = PrayerDisplay.locationLabel(context)
        if (saved.isNotBlank() && !saved.startsWith("Locat", ignoreCase = true)) return saved
        return runCatching {
            val loc = UserPreferencesRepository(context).locationFlow.first()
            if (loc.isAvailable) String.format(Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude) else ""
        }.getOrDefault("")
    }

    /** Pure content builder (unit tested). [timeMillis] is the prayer time the title shows. */
    fun content(prayer: String, timeMillis: Long, hijri: String, location: String, tz: java.util.TimeZone = java.util.TimeZone.getDefault()): Content =
        Content(
            title = PrayerDisplay.prayerTitle(prayer, PrayerDisplay.clock12(timeMillis, tz)),
            dateLine = PrayerDisplay.dateLine(PrayerDisplay.gregorianLabel(timeMillis, tz), hijri),
            location = location,
        )

    fun healthSnapshot(context: Context, channelId: String): String {
        val nm = NotificationManagerCompat.from(context)
        val ch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).getNotificationChannel(channelId)
        } else null
        return "enabled=${nm.areNotificationsEnabled()} channel=$channelId importance=${ch?.importance ?: "n/a"} exact=${PrayerScheduler.canScheduleExact(context)}"
    }

    suspend fun postAdhan(context: Context, prayer: String, triggerAt: Long, test: Boolean = false): PostResult {
        val app = context.applicationContext
        PrayerNotificationChannels.ensureAll(app)
        val sound = AdhanSound.fromPref(PrayerPrefs(app).alarmSound)
        val channelId = PrayerNotificationChannels.adhanChannelFor(sound)
        val whenMs = if (triggerAt > 0L) triggerAt else System.currentTimeMillis()
        val c = content(prayer, whenMs, PrayerDisplay.hijriLabel(app, short = false, atMillis = whenMs), resolveLocation(app))
        Log.i(TAG, "posting adhan prayer=$prayer test=$test title='${c.title}' ${healthSnapshot(app, channelId)}")

        val open = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PRAYER", true)
        }
        val pi = PendingIntent.getActivity(app, prayer.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(app, channelId)
            .setSmallIcon(R.drawable.ic_adhan_notification)
            .setContentTitle(if (test) "${c.title} (test)" else c.title)
            .setContentText(c.dateLine)
            .setSubText(c.location.ifBlank { null })
            .setStyle(NotificationCompat.BigTextStyle().bigText(c.bigText).setBigContentTitle(if (test) "${c.title} (test)" else c.title))
            .setColor(PrayerDisplay.DEEP_GREEN)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_adhan_notification, "Open", pi)
            .setWhen(whenMs)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(if (sound == AdhanSound.SILENT) NotificationCompat.DEFAULT_VIBRATE else NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
        (PrayerDisplay.masjidLargeIcon(app) ?: runCatching { BitmapFactory.decodeResource(app.resources, R.mipmap.ic_launcher) }.getOrNull())
            ?.let { builder.setLargeIcon(it) }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && sound != AdhanSound.SILENT) {
            builder.setSound(
                android.media.RingtoneManager.getDefaultUri(
                    if (sound == AdhanSound.ALARM) android.media.RingtoneManager.TYPE_ALARM else android.media.RingtoneManager.TYPE_NOTIFICATION
                )
            )
        }
        return notifySafely(app, if (test) TEST_NOTIFICATION_ID else 7000 + prayer.hashCode().mod(50), builder.build(), channelId)
    }

    /** Posts [notification] and returns why it did / did not appear (also logged). */
    fun notifySafely(context: Context, id: Int, notification: android.app.Notification, channelId: String): PostResult {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) {
            Log.w(TAG, "NOT posted id=$id: notifications disabled for the app (permission or system setting)")
            return PostResult.NOTIFICATIONS_DISABLED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).getNotificationChannel(channelId)
            if (ch != null && ch.importance == NotificationManager.IMPORTANCE_NONE) {
                Log.w(TAG, "NOT posted id=$id: channel $channelId is blocked by the user")
                return PostResult.CHANNEL_BLOCKED
            }
        }
        return try {
            nm.notify(id, notification)
            Log.i(TAG, "posted id=$id channel=$channelId")
            PostResult.POSTED
        } catch (e: SecurityException) {
            Log.w(TAG, "NOT posted id=$id: SecurityException ${e.message}")
            PostResult.FAILED
        } catch (e: Exception) {
            Log.e(TAG, "NOT posted id=$id", e)
            PostResult.FAILED
        }
    }
}
