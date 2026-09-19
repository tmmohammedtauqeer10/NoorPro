package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.UserPreferencesRepository
import com.example.ui.viewmodel.PrayerSettingsController
import com.example.utils.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
            action != Intent.ACTION_TIME_CHANGED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val location = UserPreferencesRepository(appContext).locationFlow.first()
                if (!location.isAvailable) return@launch
                val controller = PrayerSettingsController(appContext)
                // calculatePrayers() also calls scheduleAlarms() for enabled prayers.
                controller.calculatePrayers(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            } catch (e: Exception) {
                CrashReporter.report(e, "BootCompletedReceiver failed to reschedule prayer alarms")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
