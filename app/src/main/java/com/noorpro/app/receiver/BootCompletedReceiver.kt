package com.noorpro.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noorpro.app.prayer.PrayerRefreshWorker
import com.noorpro.app.prayer.PrayerScheduler
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import com.noorpro.app.utils.CrashReporter
import com.noorpro.app.prayer.ongoing.NextPrayerOngoingScheduler
import com.noorpro.app.prayer.ongoing.NextPrayerOngoingUpdater
import com.noorpro.app.prayer.widget.PrayerWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules prayer Adhan alarms after reboot or app update.
 * AlarmManager exact alarms do not survive process death across reboot.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_DATE_CHANGED &&
            action != "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                PrayerNotificationChannels.ensureAll(appContext)
                PrayerScheduler.rescheduleAll(appContext, allowNetwork = false)
                PrayerRefreshWorker.schedule(appContext)
                NextPrayerOngoingScheduler.schedule(appContext)
                if (NextPrayerOngoingUpdater.isEnabled(appContext)) {
                    NextPrayerOngoingUpdater.update(appContext)
                }
                PrayerWidgetUpdater.refreshAll(appContext)
            } catch (e: Exception) {
                CrashReporter.report(e, "BootCompletedReceiver failed to reschedule prayer alarms")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
