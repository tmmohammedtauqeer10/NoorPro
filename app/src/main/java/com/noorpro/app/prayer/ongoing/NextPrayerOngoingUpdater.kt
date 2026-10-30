package com.noorpro.app.prayer.ongoing

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.noorpro.app.notify.NoorNotify
import com.noorpro.app.data.UserPreferencesRepository
import com.noorpro.app.prayer.PrayerDisplay
import com.noorpro.app.prayer.PrayerNotifications
import com.noorpro.app.ui.viewmodel.PrayerSettingsController
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Posts / updates the quiet ongoing next-prayer notification:
 *   title  "Dhuhr · 12:16"
 *   text   "Next prayer in 2h 56m"  (+ live chronometer countdown)
 *   sub    Hijri date, masjid-tile large icon, emerald accent, expanded list of today's times.
 * Never uses the adhan HIGH channel.
 *
 * - Chronometer countdown ([NotificationCompat.setWhen] + [NotificationCompat.setUsesChronometer]).
 * - [setOnlyAlertOnce] + silent + LOW channel to avoid noise on each tick.
 * - Scheduler enqueues a near-prayer one-shot so the shade advances promptly.
 */
object NextPrayerOngoingUpdater {
    const val PREFS = "prayer_settings_prefs"
    const val KEY_ONGOING_ENABLED = "ongoing_next_prayer_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONGOING_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONGOING_ENABLED, enabled)
            .apply()
        if (!enabled) {
            NotificationManagerCompat.from(context)
                .cancel(PrayerNotificationChannels.ONGOING_NOTIFICATION_ID)
            NextPrayerOngoingScheduler.cancel(context)
        } else {
            NextPrayerOngoingScheduler.schedule(context)
        }
    }

    /**
     * @return milliseconds until the next prayer (0 if unknown / location missing).
     * Used by the scheduler to enqueue a near-prayer refresh.
     */
    suspend fun update(context: Context): Long {
        PrayerNotificationChannels.ensureAll(context)
        if (!isEnabled(context)) {
            NotificationManagerCompat.from(context)
                .cancel(PrayerNotificationChannels.ONGOING_NOTIFICATION_ID)
            return 0L
        }

        val location = UserPreferencesRepository(context).locationFlow.first()
        if (!location.isAvailable) {
            post(context, "Next prayer", "Waiting for location\u2026", null, 0L, null)
            return 0L
        }

        val controller = PrayerSettingsController(context)
        val now = Date()
        val prayers = controller.calculatePrayers(
            date = now,
            latitude = location.latitude,
            longitude = location.longitude,
            schedule = false,
        ).filter { it.name != "Sunrise" }

        val next = prayers.firstOrNull { prayer ->
            parseTodayTime(prayer.time)?.after(now) == true
        } ?: run {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
            controller.calculatePrayers(
                date = tomorrow,
                latitude = location.latitude,
                longitude = location.longitude,
                schedule = false,
            ).firstOrNull { it.name == "Fajr" }
        }

        if (next == null) {
            post(context, "Next prayer", "Unable to calculate", null, 0L, null)
            return 0L
        }

        val prayerDate = parseTodayTime(next.time) ?: now
        val target = if (!prayerDate.after(now) && next.name == "Fajr") {
            Calendar.getInstance().apply {
                time = prayerDate
                add(Calendar.DAY_OF_YEAR, 1)
            }.time
        } else prayerDate

        val remainingMs = (target.time - System.currentTimeMillis()).coerceAtLeast(0L)
        val countdown = formatCountdown(remainingMs)
        val title = PrayerDisplay.prayerTitle(next.name, PrayerDisplay.clock12(next.time))
        val body = "Next prayer in $countdown"
        val whenMs = target.time
        val dateLine = PrayerDisplay.dateLine(
            PrayerDisplay.gregorianLabel(System.currentTimeMillis()),
            PrayerDisplay.hijriLabel(context, short = false)
        )
        val place = PrayerNotifications.resolveLocation(context)
        val expanded = buildString {
            append(body)
            append('\n')
            append(dateLine)
            if (place.isNotBlank()) append("\n\uD83D\uDCCD ").append(place)
            append('\n')
            append(
                prayers.joinToString("   ") { p ->
                    if (p.name == next.name) "\u25B8 ${p.name} ${p.time}" else "${p.name} ${p.time}"
                }
            )
        }
        val rows = prayers.map { it.name to PrayerDisplay.clock12(it.time) }
        post(context, title, dateLine + " \u00B7 " + body, next.name, remainingMs, expanded, place, rows, dateLine)
        return remainingMs
    }

    private fun post(
        context: Context,
        title: String,
        body: String,
        prayerName: String?,
        remainingMs: Long,
        expanded: String?,
        place: String = "",
        rows: List<Pair<String, String>> = emptyList(),
        dateLine: String = "",
    ) {
        val pi = NoorNotify.openApp(context, 71001, "OPEN_PRAYER")

        val builder = NotificationCompat.Builder(context, PrayerNotificationChannels.ONGOING)
            .setSmallIcon(NoorNotify.SMALL_ICON)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pi)
            .setColor(NoorNotify.accent(context))
            .setShowWhen(true)
            .addAction(0, "Qibla", NoorNotify.openApp(context, 71002, "OPEN_QIBLA"))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        (prayerName?.let { NoorNotify.tile(context, it) } ?: PrayerDisplay.brandLargeIcon(context))?.let { builder.setLargeIcon(it) }
        if (place.isNotBlank()) builder.setSubText(place)
        else PrayerDisplay.hijriLabel(context, short = false).takeIf { it.isNotBlank() }?.let { builder.setSubText(it) }
        if (rows.isNotEmpty()) {
            // 1.0.96: today's timetable as a clean list, next prayer in bold with a gold-free marker.
            val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
            style.addLine(body)
            rows.forEach { (name, time) ->
                val line = "$name  \u00B7  $time"
                if (name == prayerName) {
                    val sp = android.text.SpannableString("\u25B8 $line")
                    sp.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, sp.length, 0)
                    style.addLine(sp)
                } else style.addLine("   $line")
            }
            if (dateLine.isNotBlank()) style.setSummaryText(dateLine)
            builder.setStyle(style)
        } else if (expanded != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(expanded).setBigContentTitle(title))
        }

        // Live countdown in the shade when we know the target time.
        if (remainingMs > 0L) {
            builder
                .setWhen(System.currentTimeMillis() + remainingMs)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }

        try {
            NotificationManagerCompat.from(context)
                .notify(PrayerNotificationChannels.ONGOING_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS may be denied - ignore
        }
    }

    private fun parseTodayTime(hhmm: String): Date? {
        return try {
            val parts = hhmm.split(":")
            if (parts.size < 2) return null
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        } catch (_: Exception) {
            null
        }
    }

    private fun formatCountdown(ms: Long): String {
        val totalMin = TimeUnit.MILLISECONDS.toMinutes(ms)
        val hours = totalMin / 60
        val mins = totalMin % 60
        return if (hours > 0) String.format(Locale.US, "%dh %dm", hours, mins)
        else String.format(Locale.US, "%dm", mins)
    }
}
