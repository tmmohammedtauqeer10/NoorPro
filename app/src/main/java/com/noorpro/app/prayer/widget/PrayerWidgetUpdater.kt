package com.noorpro.app.prayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.UserPreferencesRepository
import com.example.ui.viewmodel.PrayerSettingsController
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object PrayerWidgetUpdater {

    suspend fun refreshAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val nextIds = mgr.getAppWidgetIds(ComponentName(context, NextPrayerWidgetReceiver::class.java))
        val dayIds = mgr.getAppWidgetIds(ComponentName(context, DayPrayerWidgetReceiver::class.java))
        if (nextIds.isEmpty() && dayIds.isEmpty()) return

        val snapshot = loadSnapshot(context)
        nextIds.forEach { updateNext(context, mgr, it, snapshot) }
        dayIds.forEach { updateDay(context, mgr, it, snapshot) }
    }

    data class Snapshot(
        val times: Map<String, String>,
        val nextName: String,
        val nextTime: String,
        val countdown: String,
        val available: Boolean,
    )

    private suspend fun loadSnapshot(context: Context): Snapshot {
        val location = UserPreferencesRepository(context).locationFlow.first()
        if (!location.isAvailable) {
            return Snapshot(emptyMap(), "—", "--:--", "--:--", false)
        }
        val controller = PrayerSettingsController(context)
        val now = Date()
        val prayers = controller.calculatePrayers(
            date = now,
            latitude = location.latitude,
            longitude = location.longitude,
            schedule = false,
        )
        val map = prayers.associate { it.name to it.time }
        val upcoming = prayers.filter { it.name != "Sunrise" }.firstOrNull { p ->
            parseToday(p.time)?.after(now) == true
        }
        val nextName = upcoming?.name ?: "Fajr"
        val nextTime = upcoming?.time ?: map["Fajr"].orEmpty()
        val target = parseToday(nextTime)
        val countdown = if (target != null && target.after(now)) {
            formatCountdown(target.time - now.time)
        } else {
            "—"
        }
        return Snapshot(map, nextName, nextTime, countdown, true)
    }

    fun updateNext(context: Context, mgr: AppWidgetManager, id: Int, snap: Snapshot) {
        val views = RemoteViews(context.packageName, R.layout.widget_next_prayer_2x2)
        views.setTextViewText(R.id.widget_next_name, if (snap.available) snap.nextName else "Prayer")
        views.setTextViewText(R.id.widget_next_time, if (snap.available) snap.nextTime else "--:--")
        views.setTextViewText(
            R.id.widget_next_countdown,
            if (snap.available) "in ${snap.countdown}" else "Set location",
        )
        views.setOnClickPendingIntent(R.id.widget_next_root, openPrayerPending(context, id))
        mgr.updateAppWidget(id, views)
    }

    fun updateDay(context: Context, mgr: AppWidgetManager, id: Int, snap: Snapshot) {
        val views = RemoteViews(context.packageName, R.layout.widget_day_prayers_4x2)
        fun set(nameId: Int, timeId: Int, name: String) {
            views.setTextViewText(nameId, name)
            views.setTextViewText(timeId, snap.times[name] ?: "--:--")
        }
        set(R.id.widget_day_fajr_label, R.id.widget_day_fajr_time, "Fajr")
        set(R.id.widget_day_dhuhr_label, R.id.widget_day_dhuhr_time, "Dhuhr")
        set(R.id.widget_day_asr_label, R.id.widget_day_asr_time, "Asr")
        set(R.id.widget_day_maghrib_label, R.id.widget_day_maghrib_time, "Maghrib")
        set(R.id.widget_day_isha_label, R.id.widget_day_isha_time, "Isha")
        views.setTextViewText(
            R.id.widget_day_header,
            if (snap.available) "Today · next ${snap.nextName}" else "Prayer times",
        )
        views.setOnClickPendingIntent(R.id.widget_day_root, openPrayerPending(context, id + 1000))
        mgr.updateAppWidget(id, views)
    }

    private fun openPrayerPending(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PRAYER", true)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun parseToday(hhmm: String): Date? = try {
        val parts = hhmm.split(":")
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    } catch (_: Exception) {
        null
    }

    private fun formatCountdown(ms: Long): String {
        val totalMin = TimeUnit.MILLISECONDS.toMinutes(ms.coerceAtLeast(0))
        return if (totalMin >= 60) {
            String.format(Locale.US, "%dh %dm", totalMin / 60, totalMin % 60)
        } else {
            String.format(Locale.US, "%dm", totalMin)
        }
    }
}
