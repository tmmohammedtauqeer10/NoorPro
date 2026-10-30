package com.noorpro.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.prayer.DailyReminderContent
import com.noorpro.app.prayer.PrayerDisplay
import com.noorpro.app.prayer.PrayerNotifications
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
        Log.i(PrayerNotifications.TAG, "alarm fired kind=$kind prayer=$prayer triggerAt=$triggerAt late=${System.currentTimeMillis() - triggerAt}ms")
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
                            PrayerNotifications.postAdhan(app, prayer, triggerAt)
                            withContext(Dispatchers.Main) {
                                runCatching {
                                    if (AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.pauseForAdhan()
                                }
                            }
                        }
                        PrayerScheduler.KIND_PRE -> PrayerNotifications.postReminder(
                            app, 7100 + prayer.hashCode().mod(50), prayer,
                            "$prayer in $minutes minutes",
                            if (triggerAt > 0L) "$prayer at ${PrayerDisplay.clock12(triggerAt + minutes * 60_000L)} \u00B7 get ready for salah" else "Get ready for $prayer prayer.",
                            "Make wudu and prepare for salah."
                        )
                        PrayerScheduler.KIND_JUMMAH -> PrayerNotifications.postReminder(
                            app, 7160, "Jumuah", "Jumu'ah Mubarak",
                            "Jumu'ah prayer is soon. Take ghusl, wear your best, and head to the masjid early.",
                            "Recite Surah Al-Kahf and send salawat upon the Prophet \uFDFA."
                        )
                        PrayerScheduler.KIND_SUHOOR -> PrayerNotifications.postReminder(
                            app, 7161, "Suhoor", "Suhoor time", "Fajr is in about 30 minutes - time for suhoor.", "Ramadan Mubarak."
                        )
                        PrayerScheduler.KIND_IFTAR -> PrayerNotifications.postReminder(
                            app, 7162, "Iftar", "Iftar time", "Maghrib has begun - you may break your fast.",
                            "Dua: Dhahaba al-zama' wabtallatil-'uruq, wa thabata al-ajr in sha Allah."
                        )
                        PrayerScheduler.KIND_DAILY -> showDaily(app)
                    }
                } else {
                    Log.w(PrayerNotifications.TAG, "dropped stale alarm kind=$kind prayer=$prayer (late ${lateMs / 1000}s)")
                }
            } catch (e: Exception) {
                Log.e(PrayerNotifications.TAG, "notification failed kind=$kind", e)
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

    private fun showDaily(context: Context) {
        val item = DailyReminderContent.forDay(Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
        PrayerNotifications.postDaily(context, item.text, item.reference)
    }

    companion object {
        /** Adhan alarms older than this are dropped (e.g. the phone was off at prayer time). */
        const val MAX_LATE_MS = 20 * 60_000L
    }
}
