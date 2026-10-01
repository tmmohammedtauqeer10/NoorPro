package com.noorpro.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.noorpro.app.MainActivity
import com.noorpro.app.R
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.prayer.AdhanSound
import com.noorpro.app.prayer.DailyReminderContent
import com.noorpro.app.prayer.PrayerDisplay
import com.noorpro.app.prayer.PrayerPrefs
import com.noorpro.app.prayer.PrayerScheduler
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import com.noorpro.app.prayer.ongoing.NextPrayerOngoingUpdater
import com.noorpro.app.prayer.widget.PrayerWidgetUpdater
import com.noorpro.app.utils.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Receives every prayer alarm planned by [PrayerScheduler]. Whatever the alarm kind, it ALWAYS
 * re-plans the next alarms and refreshes the widgets / ongoing notification afterwards, so the
 * chain cannot silently stop.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val kind = intent.getStringExtra(PrayerScheduler.EXTRA_KIND) ?: PrayerScheduler.KIND_ADHAN
        val prayer = intent.getStringExtra(PrayerScheduler.EXTRA_PRAYER).orEmpty()
        val triggerAt = intent.getLongExtra(PrayerScheduler.EXTRA_TRIGGER_AT, 0L)
        val minutes = intent.getIntExtra(PrayerScheduler.EXTRA_MINUTES, 10)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ignore alarms delivered very late (e.g. phone was off) so we never announce a stale prayer.
                val lateMs = System.currentTimeMillis() - triggerAt
                val fresh = triggerAt == 0L || lateMs < MAX_LATE_MS
                if (fresh) {
                    PrayerNotificationChannels.ensureAll(app)
                    when (kind) {
                        PrayerScheduler.KIND_ADHAN -> {
                            showAdhan(app, prayer)
                            withContext(Dispatchers.Main) {
                                runCatching {
                                    if (AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.pauseForAdhan()
                                }
                            }
                        }
                        PrayerScheduler.KIND_PRE -> showReminder(
                            app, 7100 + prayer.hashCode().mod(50),
                            "$prayer in $minutes minutes", "Get ready for $prayer prayer.", "Prepare for salah."
                        )
                        PrayerScheduler.KIND_JUMMAH -> showReminder(
                            app, 7160, "Jumu'ah Mubarak",
                            "Jumu'ah prayer is soon. Take ghusl, wear your best, and head to the masjid early.",
                            "Recite Surah Al-Kahf and send salawat upon the Prophet \uFDFA."
                        )
                        PrayerScheduler.KIND_SUHOOR -> showReminder(
                            app, 7161, "Suhoor time", "Fajr is in about 30 minutes - time for suhoor.", "Ramadan Mubarak."
                        )
                        PrayerScheduler.KIND_IFTAR -> showReminder(
                            app, 7162, "Iftar time", "Maghrib has begun - you may break your fast.",
                            "Dua: Dhahaba al-zama' wabtallatil-'uruq, wa thabata al-ajr in sha Allah."
                        )
                        PrayerScheduler.KIND_DAILY -> showDaily(app)
                    }
                }
            } catch (e: Exception) {
                CrashReporter.report(e, "Prayer notification failed")
            } finally {
                try {
                    // Re-arm the next occurrence first (cheap, offline), then refresh the UI surfaces.
                    PrayerScheduler.rescheduleAll(app, allowNetwork = false, notBeforeMs = triggerAt + 1000L)
                    NextPrayerOngoingUpdater.update(app)
                    PrayerWidgetUpdater.refreshAll(app)
                } catch (e: Exception) {
                    CrashReporter.report(e, "Unable to schedule next prayer reminder")
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private fun openApp(context: Context, code: Int, extra: String): PendingIntent {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(extra, true)
        }
        return PendingIntent.getActivity(context, code, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun largeIcon(context: Context) = PrayerDisplay.masjidLargeIcon(context) ?: try {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    } catch (_: Exception) {
        null
    }

    private fun notifyOrIgnore(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked - nothing we can show.
        }
    }

    private fun showAdhan(context: Context, prayerName: String) {
        val sound = AdhanSound.fromPref(PrayerPrefs(context).alarmSound)
        val channelId = PrayerNotificationChannels.adhanChannelFor(sound)
        val pi = openApp(context, prayerName.hashCode(), "OPEN_PRAYER")
        val hijri = PrayerDisplay.hijriLabel(context, short = false)
        val title = "Time for $prayerName"
        val text = "It's time for $prayerName prayer."
        val big = "$text\n\u062D\u064E\u064A\u064E\u0651 \u0639\u064E\u0644\u064E\u0649 \u0627\u0644\u0635\u064E\u0651\u0644\u064E\u0627\u0629  \u2022  Hayya 'alas-salah\nHasten to the prayer. May Allah accept it from you."
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_adhan_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big).setBigContentTitle(title))
            .setColor(PrayerDisplay.DEEP_GREEN)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_adhan_notification, "Open", pi)
            // HIGH priority + channel IMPORTANCE_HIGH => heads-up pop-up. No full-screen intent is used.
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(if (sound == AdhanSound.SILENT) NotificationCompat.DEFAULT_VIBRATE else NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
        largeIcon(context)?.let { builder.setLargeIcon(it) }
        if (hijri.isNotBlank()) builder.setSubText(hijri)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && sound != AdhanSound.SILENT) {
            val uri = android.media.RingtoneManager.getDefaultUri(
                if (sound == AdhanSound.ALARM) android.media.RingtoneManager.TYPE_ALARM
                else android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            builder.setSound(uri)
        }
        notifyOrIgnore(context, 7000 + prayerName.hashCode().mod(50), builder.build())
    }

    private fun showReminder(context: Context, id: Int, title: String, text: String, big: String) {
        val pi = openApp(context, id, "OPEN_PRAYER")
        val builder = NotificationCompat.Builder(context, PrayerNotificationChannels.REMINDER)
            .setSmallIcon(R.drawable.ic_adhan_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$big").setBigContentTitle(title))
            .setColor(PrayerDisplay.EMERALD)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
        largeIcon(context)?.let { builder.setLargeIcon(it) }
        notifyOrIgnore(context, id, builder.build())
    }

    private fun showDaily(context: Context) {
        val item = DailyReminderContent.forDay(Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
        val pi = openApp(context, 7170, "OPEN_QURAN")
        val builder = NotificationCompat.Builder(context, PrayerNotificationChannels.DAILY)
            .setSmallIcon(R.drawable.ic_adhan_notification)
            .setContentTitle("Ayah of the day")
            .setContentText(item.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${item.text}\n\n${item.reference}").setBigContentTitle("Ayah of the day"))
            .setColor(PrayerDisplay.EMERALD)
            .setContentIntent(pi)
            .setAutoCancel(true)
        largeIcon(context)?.let { builder.setLargeIcon(it) }
        notifyOrIgnore(context, 7170, builder.build())
    }

    companion object {
        /** Adhan alarms older than this are dropped (e.g. the phone was off at prayer time). */
        const val MAX_LATE_MS = 10 * 60_000L
    }
}
