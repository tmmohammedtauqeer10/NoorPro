package com.noorpro.app.data.quiz

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random

/** 1.2.0 (115) Islamic quiz: categories, offline question bank, and pure session logic. */
enum class QuizCat(val key: String) {
    QURAN("quran"), SEERAH("seerah"), HADITH("hadith"), FIQH("fiqh"), AQEEDAH("aqeedah"),
    HISTORY("history"), AWLIYA("awliya"), DUAS("duas"), KIDS("kids");

    companion object { fun of(key: String?): QuizCat? = entries.firstOrNull { it.key == key } }
}

object QuizMadhhab {
    const val HANAFI = "hanafi"; const val SHAFII = "shafii"; const val MALIKI = "maliki"; const val HANBALI = "hanbali"
    val all = listOf(HANAFI, SHAFII, MALIKI, HANBALI)
    fun norm(m: String?): String = m?.lowercase()?.replace("'", "")?.replace("’", "")?.let { v -> all.firstOrNull { it == v } } ?: HANAFI
    fun label(m: String): String = when (norm(m)) { SHAFII -> "Shafi'i"; MALIKI -> "Maliki"; HANBALI -> "Hanbali"; else -> "Hanafi" }
}

data class QuizVariant(val answer: Int, val explanation: Map<String, String>)

data class QuizQuestion2(
    val id: String,
    val cat: QuizCat,
    val level: Int,
    val text: Map<String, String>,
    val options: Map<String, List<String>>,
    val answer: Int,
    val explanation: Map<String, String>,
    val source: String,
    /** Madhhab-specific answers; the base [answer]/[explanation] are the Hanafi position. */
    val variants: Map<String, QuizVariant> = emptyMap(),
    val generated: Boolean = false,
) {
    val hasMadhhabVariants: Boolean get() = variants.isNotEmpty()
    fun answerFor(madhhab: String): Int = variants[QuizMadhhab.norm(madhhab)]?.answer ?: answer
    fun textFor(lang: String): String = text[lang]?.takeIf { it.isNotBlank() } ?: text["en"].orEmpty()
    fun optionsFor(lang: String): List<String> {
        val en = options["en"].orEmpty()
        return options[lang]?.takeIf { it.size == en.size && it.all(String::isNotBlank) } ?: en
    }
    fun explanationFor(madhhab: String, lang: String): String {
        val map = variants[QuizMadhhab.norm(madhhab)]?.explanation ?: explanation
        return map[lang]?.takeIf { it.isNotBlank() } ?: map["en"].orEmpty()
    }
    fun isTranslated(lang: String): Boolean = lang == "en" || (text[lang] != null && options[lang]?.size == options["en"]?.size)

    fun toJson(): JSONObject = JSONObject().put("id", id).put("c", cat.key).put("l", level)
        .put("q", JSONObject(text)).put("o", JSONObject(options.mapValues { JSONArray(it.value) }))
        .put("a", answer).put("e", JSONObject(explanation)).put("s", source).put("g", generated)
        .also { o -> if (variants.isNotEmpty()) o.put("m", JSONObject(variants.mapValues { (_, v) -> JSONObject().put("a", v.answer).put("e", JSONObject(v.explanation)) })) }
}

object QuizBank {
    private fun strMap(o: JSONObject?): Map<String, String> = buildMap { o?.keys()?.forEach { k -> put(k, o.optString(k)) } }
    private fun listMap(o: JSONObject?): Map<String, List<String>> = buildMap {
        o?.keys()?.forEach { k -> val a = o.optJSONArray(k) ?: return@forEach; put(k, (0 until a.length()).map { a.optString(it) }) }
    }

    fun parseQuestion(o: JSONObject): QuizQuestion2? {
        val cat = QuizCat.of(o.optString("c")) ?: return null
        val options = listMap(o.optJSONObject("o"))
        val en = options["en"] ?: return null
        val text = strMap(o.optJSONObject("q"))
        val a = o.optInt("a", -1)
        if (text["en"].isNullOrBlank() || en.size < 2 || a !in en.indices) return null
        val m = o.optJSONObject("m")
        val variants = buildMap {
            m?.keys()?.forEach { k -> val v = m.optJSONObject(k) ?: return@forEach; val va = v.optInt("a", -1); if (va in en.indices) put(QuizMadhhab.norm(k), QuizVariant(va, strMap(v.optJSONObject("e")))) }
        }
        return QuizQuestion2(o.optString("id").ifBlank { return null }, cat, o.optInt("l", 1).coerceIn(1, 3), text, options, a,
            strMap(o.optJSONObject("e")), o.optString("s"), variants, o.optBoolean("g", false))
    }

    fun parse(json: String): List<QuizQuestion2> = runCatching {
        val root = json.trim()
        val arr = if (root.startsWith("[")) JSONArray(root) else JSONObject(root).optJSONArray("questions") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseQuestion) }
    }.getOrDefault(emptyList())

    fun toJson(list: List<QuizQuestion2>): String = JSONArray(list.map { it.toJson() }).toString()

    @Volatile private var cached: List<QuizQuestion2>? = null
    fun load(ctx: android.content.Context): List<QuizQuestion2> = cached ?: runCatching {
        ctx.assets.open("quiz/bank_v1.json").bufferedReader(Charsets.UTF_8).use { parse(it.readText()) }
    }.getOrDefault(emptyList()).also { if (it.isNotEmpty()) cached = it }
}

object QuizEngine {
    const val SESSION_SIZE = 10
    const val TIMER_SECONDS = 30

    /** Questions for one category (or all non-kids categories when [cat] is null), filtered by level, shuffled by [seed]. */
    fun session(bank: List<QuizQuestion2>, cat: QuizCat?, level: Int?, seed: Long, size: Int = SESSION_SIZE,
                avoid: Set<String> = emptySet()): List<QuizQuestion2> {
        val pool = bank.filter { (cat == null && it.cat != QuizCat.KIDS) || it.cat == cat }
            .filter { level == null || it.level == level }
        val rnd = Random(seed)
        val fresh = pool.filter { it.id !in avoid }.shuffled(rnd)
        val seen = pool.filter { it.id in avoid }.shuffled(rnd)
        return (fresh + seen).take(size)
    }

    fun dayKey(cal: Calendar = Calendar.getInstance()): String =
        "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

    /** The same 10 questions for everyone on a given day: one per adult category plus two more, easy/medium first. */
    fun daily(bank: List<QuizQuestion2>, dayKey: String, kids: Boolean = false): List<QuizQuestion2> {
        val rnd = Random(dayKey.hashCode().toLong() * 31 + 7)
        val usable = bank.filter { !it.generated }
        if (kids) return usable.filter { it.cat == QuizCat.KIDS }.shuffled(rnd).take(SESSION_SIZE)
        val cats = QuizCat.entries.filter { it != QuizCat.KIDS }
        val picks = LinkedHashSet<QuizQuestion2>()
        cats.forEach { c -> usable.filter { it.cat == c && it.level <= 2 }.shuffled(rnd).firstOrNull()?.let(picks::add) }
        usable.filter { it.cat != QuizCat.KIDS && it !in picks }.shuffled(rnd).forEach { if (picks.size < SESSION_SIZE) picks.add(it) }
        return picks.toList().shuffled(rnd)
    }

    fun xpFor(level: Int, correct: Boolean, fast: Boolean = false): Int = if (!correct) 0 else 5 * level.coerceIn(1, 3) + 5 + if (fast) 2 else 0

    /** New daily streak after finishing today's quiz. */
    fun nextStreak(lastDay: String?, streak: Int, today: String, yesterday: String): Int = when (lastDay) {
        today -> streak.coerceAtLeast(1)
        yesterday -> streak + 1
        else -> 1
    }

    fun yesterdayKey(cal: Calendar = Calendar.getInstance()): String = dayKey((cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) })
}
