package com.noorpro.app.islamic.deen

import android.content.Context
import com.noorpro.app.data.PrayerLogEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 1.1.6 Daily Deen goals (flag `daily_deen_goals_enabled`). Everything is local: salah comes from the
 * existing prayer tracker (Room `prayer_logs`), the other goals live in SharedPreferences.
 * Streaks are gentle — a day counts when *any* goal was done, today never breaks a streak, and one quiet
 * day in a row is forgiven ("grace") so a single missed day never resets the count.
 */
enum class DeenGoal(val id: String, val title: String, val sub: String) {
    SALAH("salah", "Five daily prayers", "Fajr to Isha"),
    QURAN("quran", "Qur'an pages", "Read a little every day"),
    MORNING("morning", "Morning adhkar", "After Fajr"),
    EVENING("evening", "Evening adhkar", "After Asr"),
    SADAQAH("sadaqah", "Sadaqah", "Any good, big or small"),
    DUA("dua", "Personal du'a", "A moment with Allah");

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
}

val SALAH_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

data class DeenGoalsConfig(val enabled: Set<DeenGoal> = DeenGoal.entries.toSet(), val quranPages: Int = 2) {
    val goals: List<DeenGoal> get() = DeenGoal.entries.filter { it in enabled }
}

/** One day. [salah] is a 5-bit mask (Fajr = bit 0). */
data class DeenDay(val day: Long, val salah: Int = 0, val pages: Int = 0, val done: Set<DeenGoal> = emptySet())

data class GoalWeek(val goal: DeenGoal, val days: List<Boolean>) { val count get() = days.count { it } }
data class DeenWeekSummary(
    val days: List<Long>, val perGoal: List<GoalWeek>, val dayScores: List<Float>, val activeDays: Int,
    val salahPrayed: Int, val pagesRead: Int, val bestDay: Long?, val headline: String,
)

data class DeenWidgetSnapshot(val day: Long, val done: Int, val total: Int, val streak: Int, val next: String?, val salah: Int, val pages: Int, val pagesTarget: Int)

object DeenGoalsLogic {
    const val MAX_PAGES_TARGET = 30
    const val MIN_PAGES_TARGET = 1

    fun salahMask(log: PrayerLogEntity?): Int {
        if (log == null) return 0
        return listOf(log.fajr, log.dhuhr, log.asr, log.maghrib, log.isha).foldIndexed(0) { i, m, b -> if (b) m or (1 shl i) else m }
    }
    fun salahCount(mask: Int) = Integer.bitCount(mask and 0b11111)
    fun dayOf(date: String): Long? = runCatching { LocalDate.parse(date).toEpochDay() }.getOrNull()

    fun progress(goal: DeenGoal, d: DeenDay, cfg: DeenGoalsConfig): Float = when (goal) {
        DeenGoal.SALAH -> salahCount(d.salah) / 5f
        DeenGoal.QURAN -> (d.pages.toFloat() / cfg.quranPages.coerceAtLeast(1)).coerceAtMost(1f)
        else -> if (goal in d.done) 1f else 0f
    }
    fun complete(goal: DeenGoal, d: DeenDay, cfg: DeenGoalsConfig) = progress(goal, d, cfg) >= 1f
    fun completed(d: DeenDay, cfg: DeenGoalsConfig) = cfg.goals.count { complete(it, d, cfg) }
    fun score(d: DeenDay, cfg: DeenGoalsConfig): Float = if (cfg.goals.isEmpty()) 0f else cfg.goals.sumOf { progress(it, d, cfg).toDouble() }.toFloat() / cfg.goals.size
    /** "Showed up": anything at all counts — one prayer, one page, one du'a. */
    fun showedUp(d: DeenDay?, cfg: DeenGoalsConfig) = d != null && cfg.goals.any { progress(it, d, cfg) > 0f }

    /**
     * Gentle streak ending today. Today being empty never breaks it (the day isn't over); a single quiet
     * day between active days is forgiven. Two quiet days in a row end the run.
     */
    fun streak(days: Map<Long, DeenDay>, cfg: DeenGoalsConfig, today: Long): Int {
        var n = 0; var d = today; var gap = 0
        if (!showedUp(days[today], cfg)) d-- // today still open
        while (d > today - 400) {
            if (showedUp(days[d], cfg)) { n++; gap = 0 } else { gap++; if (gap >= 2) break }
            d--
        }
        return n
    }

    fun streakLine(streak: Int, todayDone: Int): String = when {
        streak == 0 && todayDone == 0 -> "Every day is a fresh start. Begin with one small deed."
        streak == 0 -> "A good start today — Alhamdulillah."
        streak == 1 -> "1 day of showing up. Small and steady is beloved to Allah."
        streak < 7 -> "$streak days of showing up. Keep it gentle and steady."
        else -> "$streak days of showing up — MashaAllah, may Allah accept it."
    }

    fun nextGoal(d: DeenDay, cfg: DeenGoalsConfig): DeenGoal? = cfg.goals.firstOrNull { !complete(it, d, cfg) }

    fun week(days: Map<Long, DeenDay>, cfg: DeenGoalsConfig, today: Long): DeenWeekSummary {
        val range = (today - 6..today).toList()
        val list = range.map { days[it] ?: DeenDay(it) }
        val perGoal = cfg.goals.map { g -> GoalWeek(g, list.map { complete(g, it, cfg) }) }
        val scores = list.map { score(it, cfg) }
        val active = list.count { showedUp(it, cfg) }
        val best = list.maxByOrNull { score(it, cfg) }?.takeIf { score(it, cfg) > 0f }?.day
        val salah = list.sumOf { salahCount(it.salah) }
        val pages = list.sumOf { it.pages }
        val headline = when {
            active == 0 -> "A quiet week. Allah's door is always open — one small deed today is a beautiful start."
            active >= 7 -> "You showed up every day this week. MashaAllah."
            active >= 4 -> "You showed up on $active days this week. Steady steps, Alhamdulillah."
            else -> "You showed up on $active ${if (active == 1) "day" else "days"} this week. Every deed counts."
        }
        return DeenWeekSummary(range, perGoal, scores, active, salah, pages, best, headline)
    }

    /** Prompt for the optional Noor AI encouragement. Asks explicitly for warmth and no guilt. */
    fun aiPrompt(s: DeenWeekSummary): String {
        val goals = s.perGoal.joinToString("; ") { "${it.goal.title}: ${it.count}/7 days" }
        return "Write 2-3 short, warm sentences of encouragement for a Muslim reviewing their week of small daily goals. " +
            "They showed up on ${s.activeDays} of 7 days, prayed ${s.salahPrayed} of 35 salah and read ${s.pagesRead} Qur'an pages. $goals. " +
            "Be gentle and hopeful, never shaming or guilt-inducing, praise consistency over quantity, and you may mention one authentic " +
            "hadith such as 'the most beloved deeds to Allah are the most consistent, even if small' (Bukhari, Muslim). No lists, no headings."
    }

    fun snapshot(d: DeenDay, cfg: DeenGoalsConfig, streak: Int) =
        DeenWidgetSnapshot(d.day, completed(d, cfg), cfg.goals.size, streak, nextGoal(d, cfg)?.title, salahCount(d.salah), d.pages, cfg.quranPages)

    fun snapshotJson(s: DeenWidgetSnapshot): String = JSONObject().put("day", s.day).put("done", s.done).put("total", s.total).put("streak", s.streak)
        .put("next", s.next ?: JSONObject.NULL).put("salah", s.salah).put("pages", s.pages).put("pagesTarget", s.pagesTarget).toString()
    fun snapshotFrom(json: String?): DeenWidgetSnapshot? = runCatching {
        val o = JSONObject(json!!)
        DeenWidgetSnapshot(o.getLong("day"), o.getInt("done"), o.getInt("total"), o.getInt("streak"),
            if (o.isNull("next")) null else o.getString("next"), o.getInt("salah"), o.getInt("pages"), o.getInt("pagesTarget"))
    }.getOrNull()
}

/** Local store for the non-salah goals and settings (prefs "deen_goals_v1"). */
class DeenGoalsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("deen_goals_v1", Context.MODE_PRIVATE)
    private val widget = context.applicationContext.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)

    fun config(): DeenGoalsConfig {
        val ids = prefs.getString("enabled", null)
        val enabled = ids?.split(',')?.mapNotNull { DeenGoal.of(it) }?.toSet() ?: DeenGoal.entries.toSet()
        return DeenGoalsConfig(enabled, prefs.getInt("pages", 2).coerceIn(DeenGoalsLogic.MIN_PAGES_TARGET, DeenGoalsLogic.MAX_PAGES_TARGET))
    }
    fun setConfig(c: DeenGoalsConfig) {
        prefs.edit().putString("enabled", c.enabled.joinToString(",") { it.id })
            .putInt("pages", c.quranPages.coerceIn(DeenGoalsLogic.MIN_PAGES_TARGET, DeenGoalsLogic.MAX_PAGES_TARGET)).apply()
    }

    /** Non-salah part of each day (salah is merged from the prayer tracker by the caller). */
    fun days(): Map<Long, DeenDay> {
        val o = runCatching { JSONObject(prefs.getString("days", "{}")!!) }.getOrDefault(JSONObject())
        return o.keys().asSequence().mapNotNull { k ->
            val day = k.toLongOrNull() ?: return@mapNotNull null
            val v = o.optJSONObject(k) ?: return@mapNotNull null
            val arr = v.optJSONArray("done") ?: JSONArray()
            day to DeenDay(day, 0, v.optInt("pages").coerceAtLeast(0), (0 until arr.length()).mapNotNull { DeenGoal.of(arr.optString(it)) }.toSet())
        }.toMap()
    }
    private fun put(d: DeenDay) {
        val all = days().toMutableMap(); all[d.day] = d
        val o = JSONObject()
        all.values.filter { it.day > d.day - KEEP_DAYS }.forEach { x ->
            o.put(x.day.toString(), JSONObject().put("pages", x.pages).put("done", JSONArray(x.done.map { it.id })))
        }
        prefs.edit().putString("days", o.toString()).apply()
    }
    fun toggle(day: Long, goal: DeenGoal) {
        if (goal == DeenGoal.SALAH || goal == DeenGoal.QURAN) return
        val d = days()[day] ?: DeenDay(day)
        put(d.copy(done = if (goal in d.done) d.done - goal else d.done + goal))
    }
    fun addPages(day: Long, delta: Int) {
        val d = days()[day] ?: DeenDay(day)
        put(d.copy(pages = (d.pages + delta).coerceIn(0, 604)))
    }

    /** Merges salah masks from the prayer tracker into the stored days. */
    fun merged(prayerLogs: List<PrayerLogEntity>): Map<Long, DeenDay> {
        val m = days().toMutableMap()
        prayerLogs.forEach { log ->
            val day = DeenGoalsLogic.dayOf(log.date) ?: return@forEach
            m[day] = (m[day] ?: DeenDay(day)).copy(salah = DeenGoalsLogic.salahMask(log))
        }
        return m
    }

    fun saveWidget(s: DeenWidgetSnapshot) { widget.edit().putString("snapshot", DeenGoalsLogic.snapshotJson(s)).apply() }
    fun widgetSnapshot(): DeenWidgetSnapshot? = DeenGoalsLogic.snapshotFrom(widget.getString("snapshot", null))

    companion object {
        const val KEEP_DAYS = 120
        /** Read by a home-screen widget: a JSON [DeenWidgetSnapshot] under key "snapshot". */
        const val WIDGET_PREFS = "deen_goals_widget"
    }
}
