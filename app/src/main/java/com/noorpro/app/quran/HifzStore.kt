package com.noorpro.app.quran

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/** Per-ayah memorisation state with a light SM-2 spaced-repetition schedule. */
data class HifzCard(val reps: Int = 0, val intervalDays: Int = 0, val ease: Double = 2.5, val due: Long = 0, val lastGood: Boolean = false)

object HifzSchedule {
    /** Next card after a self-test: correct -> grow interval (1, 3, then *ease); needs work -> reset to 1 day. */
    fun grade(c: HifzCard, correct: Boolean, today: Long): HifzCard =
        if (!correct) HifzCard(reps = 0, intervalDays = 1, ease = (c.ease - 0.2).coerceAtLeast(1.3), due = today + 1, lastGood = false)
        else {
            val reps = c.reps + 1
            val iv = when (reps) { 1 -> 1; 2 -> 3; else -> (c.intervalDays * c.ease).toInt().coerceAtLeast(c.intervalDays + 1) }
            HifzCard(reps, iv, (c.ease + 0.1).coerceAtMost(3.0), today + iv, true)
        }
    /** "Memorised" once an ayah has passed 3+ spaced tests in a row. */
    fun memorised(c: HifzCard?) = c != null && c.reps >= 3
}

class HifzStore(context: Context) {
    private val p = context.getSharedPreferences("hifz_v1", Context.MODE_PRIVATE)
    private fun today() = LocalDate.now().toEpochDay()

    fun card(surah: Int, ayah: Int): HifzCard? = p.getString("c_${surah}_$ayah", null)?.let {
        val o = JSONObject(it); HifzCard(o.optInt("r"), o.optInt("i"), o.optDouble("e", 2.5), o.optLong("d"), o.optBoolean("g"))
    }

    fun grade(surah: Int, ayah: Int, correct: Boolean, juz: Int = 0): HifzCard {
        val next = HifzSchedule.grade(card(surah, ayah) ?: HifzCard(), correct, today())
        p.edit().putString("c_${surah}_$ayah", JSONObject().put("r", next.reps).put("i", next.intervalDays).put("e", next.ease).put("d", next.due).put("g", next.lastGood).put("j", juz).toString()).apply()
        touchStreak()
        return next
    }

    /** Ayahs of a surah whose review is due today or earlier. */
    fun due(surah: Int, count: Int): List<Int> = (1..count).filter { a -> card(surah, a)?.let { it.due <= today() } == true }
    fun memorisedCount(surah: Int, count: Int) = (1..count).count { HifzSchedule.memorised(card(surah, it)) }
    fun startedCount(surah: Int, count: Int) = (1..count).count { card(surah, it) != null }

    /** Memorised ayahs in a juz (only ayahs practised in Hifz mode carry their juz). */
    fun memorisedInJuz(juz: Int): Int = p.all.count { (k, v) ->
        k.startsWith("c_") && (v as? String)?.let { val o = JSONObject(it); o.optInt("j") == juz && o.optInt("r") >= 3 } == true
    }

    fun touchStreak() {
        val t = today(); val last = p.getLong("streak_last", -10)
        val s = when (t - last) { 0L -> p.getInt("streak", 1); 1L -> p.getInt("streak", 0) + 1; else -> 1 }
        p.edit().putLong("streak_last", t).putInt("streak", s).putInt("best", maxOf(s, p.getInt("best", 0))).apply()
    }
    /** 1.1.6 Hifz coach: every practised ayah (surah, ayah) -> card, for the revision plan. */
    fun allCards(): Map<Pair<Int, Int>, HifzCard> = p.all.mapNotNull { (k, _) ->
        val parts = k.split('_')
        if (parts.size != 3 || parts[0] != "c") return@mapNotNull null
        val s = parts[1].toIntOrNull() ?: return@mapNotNull null
        val a = parts[2].toIntOrNull() ?: return@mapNotNull null
        card(s, a)?.let { (s to a) to it }
    }.toMap()
    fun todayEpochDay(): Long = today()

    fun streak(): Int = if (today() - p.getLong("streak_last", -10) <= 1) p.getInt("streak", 0) else 0
    fun bestStreak(): Int = p.getInt("best", 0)
}
