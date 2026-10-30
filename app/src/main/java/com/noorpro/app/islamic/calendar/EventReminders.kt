package com.noorpro.app.islamic.calendar

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.noorpro.app.notify.NoorNotify
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Which events the user wants a reminder for (local only). */
class EventReminderStore(context: Context) {
    private val p = context.getSharedPreferences("islamic_event_reminders", Context.MODE_PRIVATE)
    fun enabled(): Set<String> = p.getStringSet("ids", emptySet()).orEmpty()
    fun isOn(id: String) = id in enabled()
    fun set(id: String, on: Boolean): Set<String> {
        val next = enabled().toMutableSet().apply { if (on) add(id) else remove(id) }
        p.edit().putStringSet("ids", next).apply()
        return next
    }
}

object EventReminderTiming {
    /** Night events remind at 17:00 on the evening they begin; day events at 20:00 the evening before. */
    fun fireAtMs(o: EventOccurrence, tz: TimeZone = TimeZone.getDefault()): Long {
        val (day, hour) = if (o.event.nightBefore) o.observeEpochDay to 17 else (o.epochDay - 1) to 20
        val utc = day * 86_400_000L + hour * 3_600_000L
        return utc - tz.getOffset(utc)
    }

    fun title(o: EventOccurrence): String = if (o.event.nightBefore) "${o.event.name} begins tonight" else "${o.event.name} is tomorrow"
    fun body(o: EventOccurrence): String = o.event.acts.first()
}

/**
 * Local reminders through WorkManager (the same scheduler the prayer refresh uses) on the existing
 * "daily reminders" notification channel. One unique job per event; after it fires it re-arms the
 * next occurrence, so no network or backend is needed.
 */
object EventReminderScheduler {
    private fun unique(id: String) = "islamic_event_$id"

    fun sync(context: Context, adjustment: Int, nowMs: Long = System.currentTimeMillis()) {
        val store = EventReminderStore(context)
        val cal = AdjustedHijri(HijriConverter.UmmAlQura, adjustment)
        val today = IslamicCalendarLogic.todayEpochDay(nowMs)
        val upcoming = IslamicCalendarLogic.upcoming(cal, today)
        val wm = WorkManager.getInstance(context)
        IslamicEventCatalog.events.forEach { e ->
            if (!store.isOn(e.id)) { wm.cancelUniqueWork(unique(e.id)); return@forEach }
            val next = upcoming.firstOrNull { it.event.id == e.id && EventReminderTiming.fireAtMs(it) > nowMs } ?: return@forEach
            val delay = EventReminderTiming.fireAtMs(next) - nowMs
            val req = OneTimeWorkRequestBuilder<EventReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("id" to e.id, "title" to EventReminderTiming.title(next), "body" to EventReminderTiming.body(next), "adj" to adjustment))
                .build()
            wm.enqueueUniqueWork(unique(e.id), ExistingWorkPolicy.REPLACE, req)
        }
    }
}

@android.annotation.SuppressLint("MissingPermission")
class EventReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.success()
        val ctx = applicationContext
        if (EventReminderStore(ctx).isOn(id)) runCatching {
            PrayerNotificationChannels.ensureAll(ctx)
            val n = NotificationCompat.Builder(ctx, PrayerNotificationChannels.DAILY)
                .setSmallIcon(NoorNotify.SMALL_ICON)
                .setColor(NoorNotify.accent(ctx))
                .setContentTitle(inputData.getString("title"))
                .setContentText(inputData.getString("body"))
                .setStyle(NotificationCompat.BigTextStyle().bigText(inputData.getString("body")))
                .setContentIntent(NoorNotify.openApp(ctx, 7300 + id.hashCode() % 100, "OPEN_ISLAMIC_CALENDAR"))
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(ctx).notify(73_000 + (id.hashCode() and 0x3FF), n)
        }
        // Re-arm for next year's occurrence (a day later so this one is skipped).
        runCatching { EventReminderScheduler.sync(ctx, inputData.getInt("adj", 0), System.currentTimeMillis() + 60_000) }
        return Result.success()
    }
}
