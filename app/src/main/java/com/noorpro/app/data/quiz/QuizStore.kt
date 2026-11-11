package com.noorpro.app.data.quiz

import android.content.Context
import org.json.JSONObject

/** Local quiz progress: settings, daily streak, XP, per-category best, mistakes to review, AI questions and translations. */
class QuizStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("quiz_v115", Context.MODE_PRIVATE)

    var madhhab: String
        get() = QuizMadhhab.norm(p.getString("madhhab", null))
        set(v) = p.edit().putString("madhhab", QuizMadhhab.norm(v)).apply()
    val hasMadhhab: Boolean get() = p.contains("madhhab")
    var lang: String?
        get() = p.getString("lang", null)
        set(v) = p.edit().putString("lang", v).apply()
    var timer: Boolean
        get() = p.getBoolean("timer", false)
        set(v) = p.edit().putBoolean("timer", v).apply()
    var level: Int?
        get() = p.getInt("level", 0).takeIf { it in 1..3 }
        set(v) = p.edit().putInt("level", v ?: 0).apply()

    val xp: Int get() = p.getInt("xp", 0)
    val answered: Int get() = p.getInt("answered", 0)
    val correct: Int get() = p.getInt("correct", 0)
    val streak: Int get() = p.getInt("streak", 0).let { s -> if (lastDaily == QuizEngine.dayKey() || lastDaily == QuizEngine.yesterdayKey()) s else 0 }
    val lastDaily: String? get() = p.getString("last_daily", null)
    val lastDailyScore: Int? get() = p.getInt("last_daily_score", -1).takeIf { it >= 0 && lastDaily == QuizEngine.dayKey() }
    fun best(cat: QuizCat): Int? = p.getInt("best_${cat.key}", -1).takeIf { it >= 0 }
    val mistakes: Set<String> get() = p.getStringSet("mistakes", emptySet()).orEmpty()
    val seen: Set<String> get() = p.getStringSet("seen", emptySet()).orEmpty()

    fun recordAnswer(q: QuizQuestion2, right: Boolean, xpGain: Int) {
        val m = mistakes.toMutableSet().apply { if (right) remove(q.id) else add(q.id) }
        val s = seen.toMutableSet().apply { add(q.id) }
        p.edit().putInt("answered", answered + 1).putInt("correct", correct + if (right) 1 else 0).putInt("xp", xp + xpGain)
            .putStringSet("mistakes", m).putStringSet("seen", if (s.size > 600) setOf(q.id) else s).apply()
    }

    fun finishCategory(cat: QuizCat?, score: Int) {
        if (cat != null && score > (best(cat) ?: -1)) p.edit().putInt("best_${cat.key}", score).apply()
    }

    /** Returns the new streak. */
    fun finishDaily(score: Int, bonusXp: Int): Int {
        val today = QuizEngine.dayKey()
        val already = p.getString("last_daily", null) == today
        val next = QuizEngine.nextStreak(p.getString("last_daily", null), p.getInt("streak", 0), today, QuizEngine.yesterdayKey())
        p.edit().putString("last_daily", today).putInt("streak", next).putInt("last_daily_score", score)
            .putInt("xp", xp + if (already) 0 else bonusXp).apply()
        return next
    }

    var generated: List<QuizQuestion2>
        get() = QuizBank.parse(p.getString("generated", "[]") ?: "[]")
        set(v) = p.edit().putString("generated", QuizBank.toJson(v.takeLast(200))).apply()

    fun translation(lang: String, id: String): JSONObject? = p.getString("tr_${lang}_$id", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
    fun saveTranslation(lang: String, id: String, o: JSONObject) = p.edit().putString("tr_${lang}_$id", o.toString()).apply()
}
