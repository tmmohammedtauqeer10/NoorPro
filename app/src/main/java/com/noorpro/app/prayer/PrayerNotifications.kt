package com.noorpro.app.prayer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.noorpro.app.R
import com.noorpro.app.data.UserPreferencesRepository
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import com.noorpro.app.notify.NoorNotify
import com.noorpro.app.receiver.NotificationActionReceiver
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
        return "enabled=${nm.areNotificationsEnabled()} channel=$channelId importance=${(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ch?.importance else null) ?: "n/a"} exact=${PrayerScheduler.canScheduleExact(context)}"
    }

    suspend fun postAdhan(context: Context, prayer: String, triggerAt: Long, test: Boolean = false): PostResult {
        val app = context.applicationContext
        PrayerNotificationChannels.ensureAll(app)
        val sound = AdhanSound.fromPref(PrayerPrefs(app).alarmSound)
        val channelId = PrayerNotificationChannels.adhanChannelFor(sound)
        val whenMs = if (triggerAt > 0L) triggerAt else System.currentTimeMillis()
        val c = content(prayer, whenMs, PrayerDisplay.hijriLabel(app, short = false, atMillis = whenMs), resolveLocation(app))
        Log.i(TAG, "posting adhan prayer=$prayer test=$test title='${c.title}' ${healthSnapshot(app, channelId)}")

        val id = if (test) TEST_NOTIFICATION_ID else adhanId(prayer)
        val builder = adhanBuilder(app, channelId, prayer, c, whenMs, test, sound != AdhanSound.SILENT, id)
            .setDefaults(if (sound == AdhanSound.SILENT) NotificationCompat.DEFAULT_VIBRATE else NotificationCompat.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && sound != AdhanSound.SILENT) {
            builder.setSound(
                android.media.RingtoneManager.getDefaultUri(
                    if (sound == AdhanSound.ALARM) android.media.RingtoneManager.TYPE_ALARM else android.media.RingtoneManager.TYPE_NOTIFICATION
                )
            )
        }
        return notifySafely(app, id, builder.build(), channelId)
    }

    fun adhanId(prayer: String) = 7000 + prayer.hashCode().mod(50)

    /** Collapsed line under the title: "It's time for Maghrib · المغرب". */
    fun adhanLine(prayer: String): String =
        "It's time for $prayer" + (NoorNotify.arabicName(prayer)?.let { " \u00B7 $it" } ?: "")

    /**
     * 1.0.96 modern adhan notification: masjid mark small icon + green accent, time-of-day tile as
     * large icon, a text-free banner (dark / light) when expanded, and Stop adhan / Prayed / Qibla.
     */
    fun adhanBuilder(app: Context, channelId: String, prayer: String, c: Content, whenMs: Long, test: Boolean, hasSound: Boolean, id: Int): NotificationCompat.Builder {
        val title = if (test) "${c.title} (test)" else c.title
        val summary = listOf(c.dateLine, c.location).filter { it.isNotBlank() }.joinToString(" \u00B7 ")
        val pi = NoorNotify.openApp(app, prayer.hashCode(), "OPEN_PRAYER")
        val b = NotificationCompat.Builder(app, channelId)
            .setSmallIcon(NoorNotify.SMALL_ICON)
            .setColor(NoorNotify.accent(app))
            .setContentTitle(title)
            .setContentText(adhanLine(prayer))
            .setSubText(c.location.ifBlank { null })
            .setContentIntent(pi)
            .setWhen(whenMs)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
        val tile = NoorNotify.tile(app, prayer)
        tile?.let { b.setLargeIcon(it) }
        val banner = NoorNotify.banner(app, prayer)
        if (banner != null) {
            b.setStyle(NotificationCompat.BigPictureStyle().bigPicture(banner).bigLargeIcon(null as android.graphics.Bitmap?)
                .setBigContentTitle(title).setSummaryText(adhanLine(prayer) + "\n" + summary))
        } else {
            b.setStyle(NotificationCompat.BigTextStyle().bigText(c.bigText).setBigContentTitle(title))
        }
        if (hasSound) b.addAction(0, "Stop adhan", NoorNotify.action(app, id * 10 + 1, NotificationActionReceiver.ACTION_STOP) {
            putExtra(NotificationActionReceiver.EXTRA_ID, id)
        })
        if (NoorNotify.isFardPrayer(prayer)) b.addAction(0, "Prayed \u2713", NoorNotify.action(app, id * 10 + 2, NotificationActionReceiver.ACTION_PRAYED) {
            putExtra(NotificationActionReceiver.EXTRA_ID, id); putExtra(NotificationActionReceiver.EXTRA_PRAYER, prayer)
        })
        b.addAction(0, "Qibla", NoorNotify.openApp(app, id * 10 + 3, "OPEN_QIBLA"))
        return b
    }

    /** Pre-prayer / Jumu'ah / Suhoor / Iftar reminder: tile, short lines, Snooze + Qibla. */
    suspend fun postReminder(context: Context, id: Int, prayer: String?, title: String, text: String, big: String, snooze: Boolean = true): PostResult {
        val app = context.applicationContext
        PrayerNotificationChannels.ensureAll(app)
        val now = System.currentTimeMillis()
        val place = resolveLocation(app)
        val dateLine = PrayerDisplay.dateLine(PrayerDisplay.gregorianLabel(now), PrayerDisplay.hijriLabel(app, short = false))
        val b = NotificationCompat.Builder(app, PrayerNotificationChannels.REMINDER)
            .setSmallIcon(NoorNotify.SMALL_ICON)
            .setColor(NoorNotify.accent(app))
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(place.ifBlank { null })
            .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title)
                .bigText(listOf(text, big, dateLine).filter { it.isNotBlank() }.joinToString("\n")))
            .setContentIntent(NoorNotify.openApp(app, id, "OPEN_PRAYER"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
        NoorNotify.tile(app, prayer)?.let { b.setLargeIcon(it) }
        if (snooze) b.addAction(0, "Snooze 5 min", NoorNotify.action(app, id * 10 + 4, NotificationActionReceiver.ACTION_SNOOZE) {
            putExtra(NotificationActionReceiver.EXTRA_ID, id); putExtra(NotificationActionReceiver.EXTRA_PRAYER, prayer)
            putExtra(NotificationActionReceiver.EXTRA_TITLE, title); putExtra(NotificationActionReceiver.EXTRA_TEXT, text)
            putExtra(NotificationActionReceiver.EXTRA_BIG, big)
        })
        b.addAction(0, "Qibla", NoorNotify.openApp(app, id * 10 + 3, "OPEN_QIBLA"))
        return notifySafely(app, id, b.build(), PrayerNotificationChannels.REMINDER)
    }

    /** Daily ayah: Quran cover as large icon, full text when expanded, "Read Quran". */
    fun postDaily(context: Context, text: String, reference: String): PostResult {
        val app = context.applicationContext
        val pi = NoorNotify.openApp(app, 7170, "OPEN_QURAN")
        val b = NotificationCompat.Builder(app, PrayerNotificationChannels.DAILY)
            .setSmallIcon(NoorNotify.SMALL_ICON)
            .setColor(NoorNotify.accent(app))
            .setContentTitle("Ayah of the day")
            .setContentText(text)
            .setSubText(reference.ifBlank { null })
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n\n$reference").setBigContentTitle("Ayah of the day"))
            .setContentIntent(pi)
            .addAction(0, "Read Quran", pi)
            .setAutoCancel(true)
        NoorNotify.bitmap(app, R.drawable.noor_cover_quran)?.let { b.setLargeIcon(it) }
        return notifySafely(app, 7170, b.build(), PrayerNotificationChannels.DAILY)
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
