package com.noorpro.app.prayer.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 1.0.98: keeps the "In 42 min" countdown on the prayer widgets fresh once a minute.
 *
 * Uses a NON-wakeup RTC alarm at the next minute boundary: it is delivered only while the device is
 * awake (screen on / already running), so it never wakes the phone and costs nothing in Doze.
 * Each tick refreshes the widgets, and [PrayerWidgetUpdater.refreshAll] re-arms the next tick
 * (or cancels it when no prayer widget is on the home screen).
 */
object WidgetTicker {
    private const val REQUEST_CODE = 72_098

    fun scheduleNext(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        val at = now - (now % 60_000L) + 60_000L + 500L
        val pi = pending(context)
        runCatching {
            val exactOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (exactOk) am.setExact(AlarmManager.RTC, at, pi) else am.setWindow(AlarmManager.RTC, at, 30_000L, pi)
        }.onFailure { runCatching { am.set(AlarmManager.RTC, at, pi) } }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching { am.cancel(pending(context)) }
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, WidgetTickReceiver::class.java).setAction(ACTION_TICK)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    const val ACTION_TICK = "com.noorpro.app.widget.TICK"
}

/** Receives [WidgetTicker] minute ticks and refreshes the prayer widgets (which re-arms the next tick). */
class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetTicker.ACTION_TICK) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerWidgetUpdater.refreshAll(app)
            } finally {
                pending.finish()
            }
        }
    }
}
