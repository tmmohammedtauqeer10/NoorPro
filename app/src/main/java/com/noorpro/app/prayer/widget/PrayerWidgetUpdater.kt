package com.noorpro.app.prayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.noorpro.app.MainActivity
import com.noorpro.app.R
import com.noorpro.app.data.UserPreferencesRepository
import com.noorpro.app.prayer.PrayerDisplay
import com.noorpro.app.ui.viewmodel.PrayerSettingsController
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object PrayerWidgetUpdater {

    /** Hide secondary lines (Hijri date) when the user shrinks a widget below this height (dp). */
    private const val MIN_HEIGHT_FOR_HIJRI_NEXT_DP = 130
    private const val MIN_HEIGHT_FOR_HIJRI_DAY_DP = 100

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
        val hijri: String = "",
        /** Wall-clock time (epoch ms) of the next prayer; 0 when unknown. Drives the live countdown. */
        val targetMs: Long = 0L,
    )

    private suspend fun loadSnapshot(context: Context): Snapshot {
        val hijri = PrayerDisplay.hijriLabel(context, short = true)
        val location = UserPreferencesRepository(context).locationFlow.first()
        if (!location.isAvailable) {
            return Snapshot(emptyMap(), "-", "--:--", "--:--", false, hijri)
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
            parseTime(p.time, 0)?.after(now) == true
        }

        var nextName = upcoming?.name ?: "Fajr"
        var nextTime = upcoming?.time ?: map["Fajr"].orEmpty()
        var dayOffset = 0
        if (upcoming == null) {
            // After Isha: count down to tomorrow's Fajr instead of showing a blank countdown.
            dayOffset = 1
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
            val tomorrowFajr = runCatching {
                controller.calculatePrayers(
                    date = tomorrow,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    schedule = false,
                ).firstOrNull { it.name == "Fajr" }
            }.getOrNull()
            if (tomorrowFajr != null) {
                nextName = "Fajr"
                nextTime = tomorrowFajr.time
            }
        }
        val target = parseTime(nextTime, dayOffset)
        val countdown = if (target != null && target.after(now)) {
            formatCountdown(target.time - now.time)
        } else {
            "--"
        }
        return Snapshot(map, nextName, nextTime, countdown, true, hijri, target?.time ?: 0L)
    }

    fun updateNext(context: Context, mgr: AppWidgetManager, id: Int, snap: Snapshot) {
        mgr.updateAppWidget(id, buildNextViews(context, id, snap, minHeightDp(mgr, id)))
    }

    fun updateDay(context: Context, mgr: AppWidgetManager, id: Int, snap: Snapshot) {
        mgr.updateAppWidget(id, buildDayViews(context, id, snap, minHeightDp(mgr, id)))
    }

    /** RemoteViews for the 2x2 widget. [minHeightDp] <= 0 means unknown (show everything). */
    fun buildNextViews(context: Context, id: Int, snap: Snapshot, minHeightDp: Int = 0): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_next_prayer_2x2)
        views.setTextViewText(R.id.widget_next_name, if (snap.available) snap.nextName else "Prayer")
        views.setTextViewText(R.id.widget_next_time, if (snap.available) snap.nextTime else "--:--")
        setCountdown(views, R.id.widget_next_countdown, snap, "in %s", "Set location")
        val showHijri = snap.hijri.isNotBlank() && (minHeightDp <= 0 || minHeightDp >= MIN_HEIGHT_FOR_HIJRI_NEXT_DP)
        views.setTextViewText(R.id.widget_next_hijri, snap.hijri)
        views.setViewVisibility(R.id.widget_next_hijri, if (showHijri) View.VISIBLE else View.GONE)
        views.setOnClickPendingIntent(R.id.widget_next_root, openPrayerPending(context, id))
        return views
    }

    /** RemoteViews for the 4x2 widget. [minHeightDp] <= 0 means unknown (show everything). */
    fun buildDayViews(context: Context, id: Int, snap: Snapshot, minHeightDp: Int = 0): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_day_prayers_4x2)
        val gold = context.getColorCompat(R.color.widget_gold)
        val muted = context.getColorCompat(R.color.widget_muted)
        val text = context.getColorCompat(R.color.widget_text)

        fun set(slotId: Int, nameId: Int, timeId: Int, name: String) {
            val isNext = snap.available && snap.nextName.equals(name, ignoreCase = true)
            views.setTextViewText(nameId, name)
            views.setTextViewText(timeId, snap.times[name] ?: "--:--")
            views.setTextColor(nameId, if (isNext) gold else muted)
            views.setTextColor(timeId, if (isNext) gold else text)
            // Rounded gold-tinted chip behind the next prayer column (0 clears it).
            views.setInt(slotId, "setBackgroundResource", if (isNext) R.drawable.widget_slot_next else 0)
        }
        set(R.id.widget_day_fajr_slot, R.id.widget_day_fajr_label, R.id.widget_day_fajr_time, "Fajr")
        set(R.id.widget_day_dhuhr_slot, R.id.widget_day_dhuhr_label, R.id.widget_day_dhuhr_time, "Dhuhr")
        set(R.id.widget_day_asr_slot, R.id.widget_day_asr_label, R.id.widget_day_asr_time, "Asr")
        set(R.id.widget_day_maghrib_slot, R.id.widget_day_maghrib_label, R.id.widget_day_maghrib_time, "Maghrib")
        set(R.id.widget_day_isha_slot, R.id.widget_day_isha_label, R.id.widget_day_isha_time, "Isha")
        setCountdown(
            views, R.id.widget_day_header, snap,
            "Next: ${snap.nextName.replace("%", "")} \u00B7 in %s", "Prayer times"
        )
        val showHijri = snap.hijri.isNotBlank() && (minHeightDp <= 0 || minHeightDp >= MIN_HEIGHT_FOR_HIJRI_DAY_DP)
        views.setTextViewText(R.id.widget_day_hijri, snap.hijri)
        views.setViewVisibility(R.id.widget_day_hijri, if (showHijri) View.VISIBLE else View.GONE)
        views.setOnClickPendingIntent(R.id.widget_day_root, openPrayerPending(context, id + 1000))
        return views
    }

    /**
     * Live countdown: a Chronometer in count-down mode is ticked by the launcher itself every
     * second, so no wake-ups are needed. The refresh alarms only re-point it at the next prayer.
     */
    private fun setCountdown(views: RemoteViews, viewId: Int, snap: Snapshot, format: String, placeholder: String) {
        val remaining = snap.targetMs - System.currentTimeMillis()
        if (snap.available && snap.targetMs > 0L && remaining > 0L) {
            views.setChronometerCountDown(viewId, true)
            views.setChronometer(viewId, SystemClock.elapsedRealtime() + remaining, format, true)
        } else {
            val text = if (snap.available) format.replace("%s", snap.countdown) else placeholder
            views.setChronometer(viewId, SystemClock.elapsedRealtime(), text, false)
        }
    }

    private fun minHeightDp(mgr: AppWidgetManager, id: Int): Int = try {
        mgr.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    } catch (_: Exception) {
        0
    }

    private fun Context.getColorCompat(resId: Int): Int =
        if (Build.VERSION.SDK_INT >= 23) getColor(resId)
        else @Suppress("DEPRECATION") resources.getColor(resId)

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

    private fun parseTime(hhmm: String, dayOffset: Int): Date? = try {
        val parts = hhmm.split(":")
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
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
