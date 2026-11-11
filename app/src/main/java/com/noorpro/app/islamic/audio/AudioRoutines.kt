package com.noorpro.app.islamic.audio

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.community.LocalNotice
import com.noorpro.app.ui.viewmodel.AudioQueueItem
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

/* 1.1.6 Audio routines (audio_routines_enabled): recurring listening habits such as Al-Kahf on Friday
 * or Al-Mulk before sleep. Scheduled entirely on the device (WorkManager); nothing is sent anywhere.
 * At the chosen time the user gets a gentle notification and starts playback with one tap: Android
 * does not let apps start audio from the background on their own, and a surprise recitation would
 * not be respectful anyway. */

data class AudioRoutine(
    val id: String,
    val title: String,
    val surahs: List<Int>,
    /** ISO day numbers 1 (Mon) … 7 (Sun); all seven = every day. */
    val days: Set<Int>,
    val minuteOfDay: Int,
    val enabled: Boolean = true,
    val why: String = "",
) {
    val daily get() = days.size == 7
}

object RoutineLogic {
    val EVERY_DAY = (1..7).toSet()
    const val MAX_ROUTINES = 12
    const val MAX_SURAHS = 10

    val PRESETS = listOf(
        AudioRoutine("kahf_friday", "Al-Kahf on Friday", listOf(18), setOf(5), 9 * 60, why = "Reciting Al-Kahf on Friday brings light between the two Fridays (al-Hakim)."),
        AudioRoutine("mulk_night", "Al-Mulk before sleep", listOf(67), EVERY_DAY, 22 * 60, why = "The Prophet ﷺ would not sleep until he recited Al-Mulk (Tirmidhi)."),
        AudioRoutine("quls_night", "The three Quls at night", listOf(112, 113, 114), EVERY_DAY, 22 * 60 + 15, why = "Recited with the hands cupped before sleeping (Bukhari 5017)."),
        AudioRoutine("yasin_morning", "Yasin in the morning", listOf(36), EVERY_DAY, 6 * 60 + 30, why = "A calm start to the day."),
        AudioRoutine("waqiah_evening", "Al-Waqi'ah in the evening", listOf(56), EVERY_DAY, 19 * 60 + 30, why = "A favourite evening surah for many."),
    )

    private val dayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    fun dayLabel(d: Int) = dayShort[(d - 1).coerceIn(0, 6)]

    fun daysLabel(days: Set<Int>) = when {
        days.size == 7 -> "Every day"
        days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
        days == setOf(6, 7) -> "Weekends"
        days.isEmpty() -> "No days"
        else -> days.sorted().joinToString(", ") { dayLabel(it) }
    }

    fun timeLabel(minuteOfDay: Int): String {
        val h = minuteOfDay / 60; val m = minuteOfDay % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.US, "%d:%02d %s", h12, m, if (h < 12) "AM" else "PM")
    }

    fun summary(r: AudioRoutine) = "${daysLabel(r.days)} · ${timeLabel(r.minuteOfDay)}"

    fun surahsLabel(r: AudioRoutine) = r.surahs.joinToString(" · ") { OfflineQuranLogic.surahName(it) }

    /** Next time this routine should fire strictly after [now], or null when it has no days / is off. */
    fun nextFire(r: AudioRoutine, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!r.enabled || r.days.isEmpty()) return null
        val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
        val t = LocalTime.of(r.minuteOfDay / 60, r.minuteOfDay % 60)
        for (i in 0..7) {
            val d: LocalDate = start.toLocalDate().plusDays(i.toLong())
            if (d.dayOfWeek.value !in r.days) continue
            val at = d.atTime(t).atZone(zone).toInstant().toEpochMilli()
            if (at > now) return at
        }
        return null
    }

    /** "Today 10:00 PM", "Tomorrow 9:00 AM" or "Fri 9:00 AM". */
    fun whenLabel(at: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val d = LocalDateTime.ofInstant(Instant.ofEpochMilli(at), zone)
        val today = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone).toLocalDate()
        val day = when (d.toLocalDate()) { today -> "Today"; today.plusDays(1) -> "Tomorrow"; else -> dayLabel(d.dayOfWeek.value) }
        return "$day ${timeLabel(d.hour * 60 + d.minute)}"
    }

    /** The routine that fires soonest, with its time. */
    fun next(list: List<AudioRoutine>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<AudioRoutine, Long>? =
        list.mapNotNull { r -> nextFire(r, now, zone)?.let { r to it } }.minByOrNull { it.second }

    /** Loose transliteration key: "Yaseen" and "Yasin", "Al-Waaqia" and "waqiah" match. */
    internal fun norm(s: String) = s.lowercase().filter { it in 'a'..'z' }
        .replace("ee", "i").replace("oo", "u").replace("aa", "a").replace("ii", "i").replace('e', 'i').trimEnd('h')

    /** Matches surahs by number ("18") or by name ("kahf"). */
    fun searchSurah(q: String, limit: Int = 6): List<Int> {
        val raw = q.trim()
        if (raw.isEmpty()) return emptyList()
        raw.toIntOrNull()?.let { return if (it in 1..114) listOf(it) else emptyList() }
        val t = norm(raw.removePrefix("al ").removePrefix("Al "))
        if (t.isEmpty()) return emptyList()
        return (1..114).filter { norm(OfflineQuranLogic.surahName(it)).contains(t) }.take(limit)
    }

    fun newId(now: Long) = "r$now"

    fun validate(title: String, surahs: List<Int>, days: Set<Int>): String? = when {
        title.isBlank() -> "Give the routine a name."
        title.length > 40 -> "Keep the name under 40 characters."
        surahs.isEmpty() -> "Pick at least one surah."
        surahs.size > MAX_SURAHS -> "Up to $MAX_SURAHS surahs per routine."
        surahs.any { it !in 1..114 } -> "Unknown surah."
        days.isEmpty() -> "Pick at least one day."
        else -> null
    }

    fun queue(r: AudioRoutine) = r.surahs.map { AudioQueueItem(it, null, OfflineQuranLogic.surahName(it), com.noorpro.app.data.QuranMetaData.surahNamesAr.getOrElse(it - 1) { "" }) }

    fun toJson(list: List<AudioRoutine>): String = JSONArray().apply {
        list.forEach { r ->
            put(JSONObject().put("id", r.id).put("title", r.title).put("surahs", JSONArray(r.surahs)).put("days", JSONArray(r.days.sorted()))
                .put("minute", r.minuteOfDay).put("enabled", r.enabled).put("why", r.why))
        }
    }.toString()

    fun fromJson(s: String?): List<AudioRoutine> = runCatching {
        val a = JSONArray(s ?: return emptyList())
        (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val surahs = o.optJSONArray("surahs")?.let { arr -> (0 until arr.length()).map { arr.optInt(it) } }.orEmpty().filter { it in 1..114 }
            val days = o.optJSONArray("days")?.let { arr -> (0 until arr.length()).map { arr.optInt(it) } }.orEmpty().filter { it in 1..7 }.toSet()
            AudioRoutine(o.optString("id"), o.optString("title"), surahs, days, o.optInt("minute").coerceIn(0, 1439), o.optBoolean("enabled", true), o.optString("why"))
                .takeIf { it.id.isNotBlank() && it.surahs.isNotEmpty() }
        }
    }.getOrDefault(emptyList())
}

class RoutineStore(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("audio_routines", Context.MODE_PRIVATE)
    fun all(): List<AudioRoutine> = RoutineLogic.fromJson(p.getString("routines", null))
    fun save(list: List<AudioRoutine>) { p.edit().putString("routines", RoutineLogic.toJson(list.take(RoutineLogic.MAX_ROUTINES))).apply() }
    fun upsert(r: AudioRoutine) = save(all().filterNot { it.id == r.id } + r)
    fun remove(id: String) = save(all().filterNot { it.id == id })
    /** Routine whose notification was tapped most recently (shown as "Ready to play"). */
    var pending: String?
        get() = p.getString("pending", null)
        set(v) { p.edit().putString("pending", v).apply() }
}

object RoutineScheduler {
    private fun work(id: String) = "audio_routine_$id"
    fun notificationId(id: String) = 74_700 + (id.hashCode() and 0xFF)

    fun schedule(ctx: Context, r: AudioRoutine, now: Long = System.currentTimeMillis()) {
        val at = RoutineLogic.nextFire(r, now)
        if (at == null) { cancel(ctx, r.id); return }
        val req = OneTimeWorkRequestBuilder<AudioRoutineWorker>().setInitialDelay(at - now, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("id" to r.id)).addTag("audio_routine").build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(work(r.id), ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(ctx: Context, id: String) = WorkManager.getInstance(ctx).cancelUniqueWork(work(id))

    /** Re-arms every enabled routine (called when the screen opens and after each edit). */
    fun syncAll(ctx: Context) {
        val on = IslamicFlags.isOn(IslamicFlags.AUDIO_ROUTINES)
        RoutineStore(ctx).all().forEach { if (on && it.enabled) schedule(ctx, it) else cancel(ctx, it.id) }
    }

    fun fire(ctx: Context, id: String) {
        val store = RoutineStore(ctx)
        val r = store.all().firstOrNull { it.id == id } ?: return
        if (!IslamicFlags.isOn(IslamicFlags.AUDIO_ROUTINES) || !r.enabled) return
        store.pending = r.id
        LocalNotice.post(ctx, notificationId(r.id), r.title, "${RoutineLogic.surahsLabel(r)} · tap to listen", "AUDIO_ROUTINES")
        schedule(ctx, r, System.currentTimeMillis() + 60_000)
    }
}

class AudioRoutineWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        inputData.getString("id")?.let { runCatching { RoutineScheduler.fire(applicationContext, it) } }
        return Result.success()
    }
}
