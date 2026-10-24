package com.noorpro.app.prayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
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

    /** Hide secondary lines when the user shrinks a widget below these heights (dp). */
    private const val MIN_HEIGHT_FULL_NEXT_DP = 140
    private const val MIN_HEIGHT_SUB_DAY_DP = 150
    private const val MIN_HEIGHT_HEADER_DAY_DP = 120

    suspend fun refreshAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val nextIds = mgr.getAppWidgetIds(ComponentName(context, NextPrayerWidgetReceiver::class.java))
        val dayIds = mgr.getAppWidgetIds(ComponentName(context, DayPrayerWidgetReceiver::class.java))
        if (nextIds.isEmpty() && dayIds.isEmpty()) {
            WidgetTicker.cancel(context)
            return
        }

        val snapshot = loadSnapshot(context)
        nextIds.forEach { updateNext(context, mgr, it, snapshot) }
        dayIds.forEach { updateDay(context, mgr, it, snapshot) }
        // 1.0.98: minute countdown ("In 42 min") - re-armed after every refresh.
        WidgetTicker.scheduleNext(context)
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

    /** "4:35 PM" or "16:35", following the app's 12/24-hour setting. */
    fun displayTime(context: Context, hhmm: String, withSuffix: Boolean = true): String {
        if (!use12Hour(context)) return hhmm
        val label = PrayerDisplay.clock12(hhmm)
        return if (withSuffix) label else label.substringBefore(' ')
    }

    private fun use12Hour(context: Context): Boolean = runCatching {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("use_12_hour_time", true)
    }.getOrDefault(true)

    /** "In 42 min" / "In 2 h 5 min"; minutes are rounded up so it never reads "In 0 min" early. */
    fun countdownWords(ms: Long): String {
        val totalMin = (ms.coerceAtLeast(0) + 59_999L) / 60_000L
        return when {
            totalMin <= 1L -> "In 1 min"
            totalMin < 60L -> "In $totalMin min"
            totalMin % 60L == 0L -> "In ${totalMin / 60} h"
            else -> "In ${totalMin / 60} h ${totalMin % 60} min"
        }
    }

    private fun countdownText(snap: Snapshot): String {
        val remaining = snap.targetMs - System.currentTimeMillis()
        return if (snap.targetMs > 0L && remaining > 0L) countdownWords(remaining) else "In ${snap.countdown}"
    }

    /** Sun / sunset / moon for the upcoming prayer. */
    fun timeOfDayIcon(nextName: String): Int = when (nextName.lowercase(Locale.ROOT)) {
        "dhuhr", "asr", "sunrise" -> R.drawable.ic_widget_sun
        "maghrib" -> R.drawable.ic_widget_sunset
        else -> R.drawable.ic_widget_moon
    }

    /** RemoteViews for the 2x2 Next prayer widget. [minHeightDp] <= 0 means unknown (show everything). */
    fun buildNextViews(context: Context, id: Int, snap: Snapshot, minHeightDp: Int = 0): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_next_prayer_2x2)
        if (snap.available) {
            views.setTextViewText(R.id.widget_next_name, snap.nextName)
            views.setTextViewText(R.id.widget_next_time, displayTime(context, snap.nextTime))
            views.setTextViewText(R.id.widget_next_countdown, countdownText(snap))
            views.setImageViewResource(R.id.widget_next_icon, timeOfDayIcon(snap.nextName))
        } else {
            views.setTextViewText(R.id.widget_next_name, "Prayer")
            views.setTextViewText(R.id.widget_next_time, "--:--")
            views.setTextViewText(R.id.widget_next_countdown, "Set location")
        }
        // Very short widgets: drop the caption so name + time + countdown still fit.
        val compact = minHeightDp in 1 until MIN_HEIGHT_FULL_NEXT_DP
        views.setViewVisibility(R.id.widget_next_label, if (compact) View.GONE else View.VISIBLE)
        views.setOnClickPendingIntent(android.R.id.background, openPrayerPending(context, id))
        return views
    }

    /** RemoteViews for the 4x2 Prayer times widget. [minHeightDp] <= 0 means unknown (show everything). */
    fun buildDayViews(context: Context, id: Int, snap: Snapshot, minHeightDp: Int = 0): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_day_prayers_4x2)
        val accent = context.getColorCompat(R.color.widget_accent_text)
        val ink = context.getColorCompat(R.color.widget_ink)

        fun set(slotId: Int, nameId: Int, timeId: Int, name: String) {
            val isNext = snap.available && snap.nextName.equals(name, ignoreCase = true)
            views.setTextViewText(nameId, name)
            views.setTextViewText(timeId, snap.times[name]?.let { displayTime(context, it, withSuffix = false) } ?: "--:--")
            views.setTextColor(nameId, if (isNext) accent else ink)
            views.setTextColor(timeId, if (isNext) accent else ink)
            // Mint pill behind the next prayer column (0 clears it).
            views.setInt(slotId, "setBackgroundResource", if (isNext) R.drawable.widget_slot_mint else 0)
        }
        set(R.id.widget_day_fajr_slot, R.id.widget_day_fajr_label, R.id.widget_day_fajr_time, "Fajr")
        set(R.id.widget_day_dhuhr_slot, R.id.widget_day_dhuhr_label, R.id.widget_day_dhuhr_time, "Dhuhr")
        set(R.id.widget_day_asr_slot, R.id.widget_day_asr_label, R.id.widget_day_asr_time, "Asr")
        set(R.id.widget_day_maghrib_slot, R.id.widget_day_maghrib_label, R.id.widget_day_maghrib_time, "Maghrib")
        set(R.id.widget_day_isha_slot, R.id.widget_day_isha_label, R.id.widget_day_isha_time, "Isha")
        if (snap.available) {
            views.setTextViewText(R.id.widget_day_next_name, snap.nextName)
            views.setTextViewText(R.id.widget_day_next_time, displayTime(context, snap.nextTime))
            views.setTextViewText(R.id.widget_day_sub, "Next prayer \u00B7 " + countdownText(snap).replaceFirstChar { it.lowercase() })
        } else {
            views.setTextViewText(R.id.widget_day_next_name, "Prayer times")
            views.setTextViewText(R.id.widget_day_next_time, "")
            views.setTextViewText(R.id.widget_day_sub, "Open NoorPro to set your location")
        }
        views.setTextViewText(R.id.widget_day_today, context.getString(R.string.widget_day_header))
        val h = minHeightDp
        views.setViewVisibility(R.id.widget_day_sub, if (h in 1 until MIN_HEIGHT_SUB_DAY_DP) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_day_header_row, if (h in 1 until MIN_HEIGHT_HEADER_DAY_DP) View.GONE else View.VISIBLE)
        views.setOnClickPendingIntent(android.R.id.background, openPrayerPending(context, id + 1000))
        return views
    }

    private fun minHeightDp(mgr: AppWidgetManager, id: Int): Int = try {
        mgr.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)  // portrait height
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
