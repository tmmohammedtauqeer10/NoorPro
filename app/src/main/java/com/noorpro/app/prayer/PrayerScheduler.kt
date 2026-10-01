package com.noorpro.app.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.noorpro.app.data.PrayerTime
import com.noorpro.app.data.UserPreferencesRepository
import com.noorpro.app.receiver.PrayerAlarmReceiver
import com.noorpro.app.ui.viewmodel.PrayerSettingsController
import com.noorpro.app.utils.CrashReporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Date

/**
 * Plans every prayer-related alarm in one place:
 *  - adhan at the EXACT prayer time (five prayers, per-prayer toggle),
 *  - optional pre-prayer reminder (X minutes before, separate alarm),
 *  - silent "refresh" alarms at every prayer change + just after midnight so widgets / the ongoing
 *    notification never show a stale prayer,
 *  - daily ayah, Jumu'ah (Friday, before Dhuhr) and Ramadan (Suhoor / Iftar) reminders.
 * Only the next occurrence of each alarm is armed; the receiver re-arms after every fire, and
 * boot / app start / the 15-minute worker re-arm everything, so a missed alarm self-heals.
 */
object PrayerScheduler {
    const val KIND_ADHAN = "adhan"
    const val KIND_PRE = "pre"
    const val KIND_REFRESH = "refresh"
    const val KIND_DAILY = "daily"
    const val KIND_JUMMAH = "jummah"
    const val KIND_SUHOOR = "suhoor"
    const val KIND_IFTAR = "iftar"

    const val EXTRA_KIND = "KIND"
    const val EXTRA_PRAYER = "PRAYER_NAME"
    const val EXTRA_TRIGGER_AT = "TRIGGER_AT"
    const val EXTRA_MINUTES = "MINUTES"

    val FIVE = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    private val ALL = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    private const val RC_ADHAN = 5100
    private const val RC_PRE = 5200
    private const val RC_DAILY = 5300
    private const val RC_JUMMAH = 5301
    private const val RC_SUHOOR = 5302
    private const val RC_IFTAR = 5303
    private const val RC_REFRESH = 5400
    private const val RC_MIDNIGHT = 5410

    private const val JUMMAH_LEAD_MIN = 30
    private const val SUHOOR_LEAD_MIN = 30

    /** One planned alarm (pure data, so it can be unit tested). */
    data class Planned(val requestCode: Int, val kind: String, val prayer: String, val triggerAt: Long, val exact: Boolean)

    /**
     * Pure planning step. [dayTimes] maps day offset (0 = today) to the six HH:mm timings;
     * [fridayDhuhr] maps day offset (0..14) -> Dhuhr HH:mm for Fridays only.
     */
    fun plan(
        nowMs: Long,
        notBeforeMs: Long,
        base: Calendar,
        dayTimes: Map<Int, Map<String, String>>,
        enabled: (String) -> Boolean,
        preMinutes: Int?,
        dailyMinuteOfDay: Int?,
        jummah: Boolean,
        fridayDhuhr: Map<Int, String>,
        ramadanDays: Set<Int>,
    ): List<Planned> {
        val floor = maxOf(nowMs, notBeforeMs)
        val out = ArrayList<Planned>()
        fun at(offset: Int, hhmm: String): Long? {
            val parts = hhmm.split(":")
            val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
            val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return null
            return (base.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        fun firstFuture(name: String, leadMin: Int = 0): Long? =
            dayTimes.keys.sorted().mapNotNull { off -> dayTimes[off]?.get(name)?.let { at(off, it) } }
                .map { it - leadMin * 60_000L }
                .firstOrNull { it > floor }

        ALL.forEachIndexed { idx, name ->
            if (name in FIVE && enabled(name)) {
                firstFuture(name)?.let { out += Planned(RC_ADHAN + idx, KIND_ADHAN, name, it, true) }
                if (preMinutes != null) {
                    firstFuture(name, preMinutes)?.let { out += Planned(RC_PRE + idx, KIND_PRE, name, it, true) }
                }
            }
            firstFuture(name)?.let { out += Planned(RC_REFRESH + idx, KIND_REFRESH, name, it + 3_000L, false) }
        }
        // Just after local midnight: new date, new Hijri day, new timings.
        (base.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 1); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis.let { out += Planned(RC_MIDNIGHT, KIND_REFRESH, "", it, false) }

        if (dailyMinuteOfDay != null) {
            val c = (base.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, dailyMinuteOfDay / 60); set(Calendar.MINUTE, dailyMinuteOfDay % 60)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (c.timeInMillis <= floor) c.add(Calendar.DAY_OF_YEAR, 1)
            out += Planned(RC_DAILY, KIND_DAILY, "", c.timeInMillis, false)
        }
        if (jummah) {
            fridayDhuhr.keys.sorted().firstNotNullOfOrNull { off ->
                at(off, fridayDhuhr.getValue(off))?.minus(JUMMAH_LEAD_MIN * 60_000L)?.takeIf { it > floor }
            }?.let { out += Planned(RC_JUMMAH, KIND_JUMMAH, "Dhuhr", it, true) }
        }
        if (ramadanDays.isNotEmpty()) {
            ramadanDays.sorted().firstNotNullOfOrNull { off ->
                dayTimes[off]?.get("Fajr")?.let { at(off, it) }?.minus(SUHOOR_LEAD_MIN * 60_000L)?.takeIf { it > floor }
            }?.let { out += Planned(RC_SUHOOR, KIND_SUHOOR, "Fajr", it, true) }
            ramadanDays.sorted().firstNotNullOfOrNull { off ->
                dayTimes[off]?.get("Maghrib")?.let { at(off, it) }?.takeIf { it > floor }
            }?.let { out += Planned(RC_IFTAR, KIND_IFTAR, "Maghrib", it, true) }
        }
        return out
    }

    /** All request codes this scheduler can ever arm, used to cancel alarms that are no longer wanted. */
    private val ALL_CODES: List<Int> =
        ALL.indices.flatMap { listOf(RC_ADHAN + it, RC_PRE + it, RC_REFRESH + it) } +
            listOf(RC_DAILY, RC_JUMMAH, RC_SUHOOR, RC_IFTAR, RC_MIDNIGHT)

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    /** Fire-and-forget re-plan from UI code. */
    fun rescheduleAsync(context: Context, allowNetwork: Boolean = false) {
        val app = context.applicationContext
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching {
                rescheduleAll(app, allowNetwork)
                com.noorpro.app.prayer.widget.PrayerWidgetUpdater.refreshAll(app)
                com.noorpro.app.prayer.ongoing.NextPrayerOngoingUpdater.update(app)
            }.onFailure { CrashReporter.report(it as? Exception ?: Exception(it)) }
        }
    }

    /** Blocking; call from a background thread. */
    fun rescheduleAll(context: Context, allowNetwork: Boolean, notBeforeMs: Long = 0L) {
        val app = context.applicationContext
        val location = runBlocking { UserPreferencesRepository(app).locationFlow.first() }
        val prefs = PrayerPrefs(app)
        val controller = PrayerSettingsController(app)
        val now = System.currentTimeMillis()
        val base = Calendar.getInstance().apply { timeInMillis = now }

        val dayTimes = HashMap<Int, Map<String, String>>()
        var hasLocation = location.isAvailable
        if (hasLocation) {
            for (off in 0..2) {
                val date = Date(now + off * 86_400_000L)
                val list: List<PrayerTime> = runCatching {
                    controller.calculatePrayers(
                        date = date, latitude = location.latitude, longitude = location.longitude,
                        schedule = false, allowNetwork = allowNetwork && off <= 1
                    )
                }.getOrNull() ?: emptyList()
                if (list.size >= 6) dayTimes[off] = list.associate { it.name to it.time }
            }
            if (dayTimes.isEmpty()) hasLocation = false
        }

        val fridays = HashMap<Int, String>()
        val ramadan = HashSet<Int>()
        if (hasLocation) {
            val hijriAdj = app.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getInt("hijri_adjustment", 0)
            for (off in 0..14) {
                val day = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, off) }
                if (prefs.jummahEnabled && day.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                    val times = dayTimes[off] ?: runCatching {
                        controller.calculatePrayers(
                            date = day.time, latitude = location.latitude, longitude = location.longitude,
                            schedule = false
                        ).associate { it.name to it.time }
                    }.getOrNull()
                    times?.get("Dhuhr")?.let { fridays[off] = it }
                }
                if (off <= 2 && prefs.ramadanEnabled && isRamadan(day.time, hijriAdj)) ramadan += off
            }
        }

        val planned = plan(
            nowMs = now,
            notBeforeMs = notBeforeMs,
            base = base,
            dayTimes = dayTimes,
            enabled = { controller.isAlarmEnabled(it) },
            preMinutes = if (prefs.preReminderEnabled) prefs.preReminderMinutes else null,
            dailyMinuteOfDay = if (prefs.dailyAyahEnabled) prefs.dailyAyahMinuteOfDay else null,
            jummah = prefs.jummahEnabled,
            fridayDhuhr = fridays,
            ramadanDays = ramadan,
        ).let { all ->
            // Without a location only the daily ayah alarm can be planned.
            if (hasLocation) all else all.filter { it.kind == KIND_DAILY }
        }

        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelLegacy(app, am)
        val wanted = planned.associateBy { it.requestCode }
        val exactOk = canScheduleExact(app)
        for (code in ALL_CODES) {
            val p = wanted[code]
            if (p == null) cancel(app, am, code) else arm(app, am, p, exactOk)
        }
    }

    private fun isRamadan(date: Date, adjustmentDays: Int): Boolean = runCatching {
        val cal = android.icu.util.IslamicCalendar()
        cal.time = date
        cal.add(android.icu.util.Calendar.DAY_OF_MONTH, adjustmentDays)
        cal.get(android.icu.util.Calendar.MONTH) == 8 // Ramadan (0-based)
    }.getOrDefault(false)

    private fun intentFor(context: Context, code: Int) =
        Intent(context, PrayerAlarmReceiver::class.java).setAction("com.noorpro.app.PRAYER_ALARM_$code")

    private fun pending(context: Context, code: Int, intent: Intent, create: Boolean): PendingIntent? {
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, code, intent, flags)
    }

    private fun cancel(context: Context, am: AlarmManager, code: Int) {
        pending(context, code, intentFor(context, code), create = false)?.let {
            am.cancel(it); it.cancel()
        }
    }

    /** Alarms armed by builds <= 1.0.34 used name.hashCode() request codes and no action. */
    private fun cancelLegacy(context: Context, am: AlarmManager) {
        for (name in FIVE) {
            val legacy = Intent(context, PrayerAlarmReceiver::class.java)
            pending(context, name.hashCode(), legacy, create = false)?.let { am.cancel(it); it.cancel() }
        }
    }

    private fun arm(context: Context, am: AlarmManager, p: Planned, exactOk: Boolean) {
        val intent = intentFor(context, p.requestCode)
            .putExtra(EXTRA_KIND, p.kind)
            .putExtra(EXTRA_PRAYER, p.prayer)
            .putExtra(EXTRA_TRIGGER_AT, p.triggerAt)
            .putExtra(EXTRA_MINUTES, PrayerPrefs(context).preReminderMinutes)
        val pi = pending(context, p.requestCode, intent, create = true) ?: return
        try {
            if (p.exact && exactOk) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.triggerAt, pi)
            }
        } catch (e: SecurityException) {
            // Exact-alarm permission revoked between the check and the call: degrade to inexact.
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.triggerAt, pi) }
            CrashReporter.report(e, "exact alarm denied; fell back to inexact")
        }
    }
}
