package com.noorpro.app.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.noorpro.app.data.PrayerDatabase
import com.noorpro.app.data.PrayerLogEntity
import com.noorpro.app.notify.NoorNotify
import com.noorpro.app.prayer.PrayerNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1.0.96: handles the buttons on prayer notifications.
 * - Stop adhan: cancels the notification, which stops the channel adhan sound.
 * - Prayed ✓: ticks today's prayer in the tracker (same table as the Prayer tracker screen).
 * - Snooze 5 min: re-posts the reminder via an inexact, idle-allowed alarm (no exact-alarm permission).
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val id = intent.getIntExtra(EXTRA_ID, -1)
        val prayer = intent.getStringExtra(EXTRA_PRAYER)
        when (intent.action) {
            ACTION_STOP -> cancel(app, id)
            ACTION_PRAYED -> {
                cancel(app, id)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        markPrayed(app, prayer, today())
                    } catch (e: Exception) {
                        Log.w(TAG, "mark prayed failed", e)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                cancel(app, id)
                val fire = NoorNotify.action(app, id * 10 + 5, ACTION_SNOOZE_FIRE) { putExtras(intent) }
                val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                runCatching {
                    am?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + SNOOZE_MS, fire)
                }.onFailure { Log.w(TAG, "snooze failed", it) }
            }
            ACTION_SNOOZE_FIRE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        PrayerNotifications.postReminder(
                            app, id, prayer,
                            intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                            intent.getStringExtra(EXTRA_TEXT).orEmpty(),
                            intent.getStringExtra(EXTRA_BIG).orEmpty(),
                            snooze = false,
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "snoozed reminder failed", e)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun cancel(context: Context, id: Int) {
        if (id >= 0) runCatching { NotificationManagerCompat.from(context).cancel(id) }
    }

    companion object {
        private const val TAG = "NoorNotifAction"
        const val ACTION_STOP = "com.noorpro.app.notify.STOP_ADHAN"
        const val ACTION_PRAYED = "com.noorpro.app.notify.MARK_PRAYED"
        const val ACTION_SNOOZE = "com.noorpro.app.notify.SNOOZE"
        const val ACTION_SNOOZE_FIRE = "com.noorpro.app.notify.SNOOZE_FIRE"
        const val EXTRA_ID = "notif_id"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_BIG = "big"
        const val SNOOZE_MS = 5 * 60_000L

        fun today(now: Long = System.currentTimeMillis()): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

        /** Returns [log] with [prayer] ticked; unknown names leave it unchanged. */
        fun withPrayed(log: PrayerLogEntity, prayer: String?): PrayerLogEntity = when (prayer) {
            "Fajr" -> log.copy(fajr = true)
            "Dhuhr" -> log.copy(dhuhr = true)
            "Asr" -> log.copy(asr = true)
            "Maghrib" -> log.copy(maghrib = true)
            "Isha" -> log.copy(isha = true)
            else -> log
        }

        suspend fun markPrayed(context: Context, prayer: String?, date: String) {
            val dao = PrayerDatabase.getDatabase(context).prayerTrackerDao()
            val current = dao.getLogForDateSync(date) ?: PrayerLogEntity(date)
            val next = withPrayed(current, prayer)
            if (next != current || dao.getLogForDateSync(date) == null) dao.upsertPrayerLog(next)
        }
    }
}
