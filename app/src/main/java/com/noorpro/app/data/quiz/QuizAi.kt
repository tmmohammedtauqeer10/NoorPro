package com.noorpro.app.data.quiz

import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.data.NoorAiOptions
import org.json.JSONArray
import org.json.JSONObject

/** Online extras through the Noor AI Worker: translating bundled questions and generating new checked ones. */
object QuizAi {
    // ---- Translation (Urdu / Hindi / Arabic). Sources stay as written (they are references).
    fun translationBlock(q: QuizQuestion2, madhhab: String): String = buildString {
        append("[Q] ").append(q.textFor("en").replace('\n', ' ')).append('\n')
        q.optionsFor("en").forEachIndexed { i, o -> append('[').append(i + 1).append("] ").append(o.replace('\n', ' ')).append('\n') }
        append("[E] ").append(q.explanationFor(madhhab, "en").replace('\n', ' '))
    }

    /** Parses the translated block back; null unless every line came back. */
    fun parseTranslation(text: String, optionCount: Int): JSONObject? {
        val lines = text.lines().map { it.trim().trimStart('-', '*', ' ') }.filter { it.isNotBlank() }
        fun find(tag: String) = lines.firstOrNull { it.startsWith("[$tag]") }?.removePrefix("[$tag]")?.trim()?.takeIf { it.isNotBlank() }
        val q = find("Q") ?: return null
        val opts = (1..optionCount).map { find(it.toString()) ?: return null }
        val e = find("E") ?: return null
        return JSONObject().put("q", q).put("o", JSONArray(opts)).put("e", e)
    }

    fun applyTranslation(q: QuizQuestion2, lang: String, madhhab: String, t: JSONObject): QuizQuestion2 {
        val o = t.optJSONArray("o") ?: return q
        val opts = (0 until o.length()).map { o.optString(it) }
        if (opts.size != q.optionsFor("en").size) return q
        val m = QuizMadhhab.norm(madhhab)
        val e = t.optString("e")
        val variants = if (q.variants.containsKey(m)) q.variants.mapValues { (k, v) -> if (k == m) v.copy(explanation = v.explanation + (lang to e)) else v } else q.variants
        val expl = if (q.variants.containsKey(m)) q.explanation else q.explanation + (lang to e)
        return q.copy(text = q.text + (lang to t.optString("q")), options = q.options + (lang to opts), explanation = expl, variants = variants)
    }

    suspend fun translate(q: QuizQuestion2, lang: String, madhhab: String): JSONObject? {
        if (lang == "en") return null
        val r = AiFeatureService.creator("translate", translationBlock(q, madhhab), lang = lang, kind = "quiz") ?: return null
        return parseTranslation(r.optString("text"), q.optionsFor("en").size)
    }

    // ---- Generation
    private val topics = mapOf(
        QuizCat.QURAN to "the Quran (surahs, verses, revelation, stories of the prophets in the Quran)",
        QuizCat.SEERAH to "the Seerah (life of the Prophet Muhammad ﷺ)",
        QuizCat.HADITH to "well-known authentic hadith and hadith sciences",
        QuizCat.FIQH to "everyday fiqh of purification, salah, fasting and zakat",
        QuizCat.AQEEDAH to "Sunni aqidah as taught in the Ash'ari and Maturidi schools",
        QuizCat.HISTORY to "Islamic history after the Prophet ﷺ",
        QuizCat.AWLIYA to "the awliya and tasawwuf (the great Sufi masters, their books and teachings)",
        QuizCat.DUAS to "authentic duas and adhkar from the Quran and Sunnah",
        QuizCat.KIDS to "very simple Islamic basics for children aged 6–10",
    )

    fun generationPrompt(cat: QuizCat, madhhab: String, count: Int, level: Int?): String = buildString {
        append("Write ").append(count).append(" multiple-choice quiz questions about ").append(topics[cat]).append(". ")
        append("Follow Ahl as-Sunnah wal-Jama'ah (Ash'ari/Maturidi creed), respectful of tasawwuf and the awliya; nothing against Sunni scholarship or Sufism. ")
        append("For any fiqh ruling, give the answer of the ").append(QuizMadhhab.label(madhhab)).append(" madhhab. ")
        append("Only facts that are accurate and well established, each with a verifiable source (Quran surah:ayah, a hadith collection with number, or a classical book). ")
        append("Difficulty: ").append(when (level) { 1 -> "easy"; 3 -> "hard"; 2 -> "medium"; else -> "mixed" }).append(". ")
        append("Exactly 4 options, one correct. Reply with ONLY a JSON array, no other text: ")
        append("[{\"q\":\"question\",\"options\":[\"a\",\"b\",\"c\",\"d\"],\"answer\":0,\"explanation\":\"one or two sentences\",\"source\":\"reference\"}]")
    }

    private val unsafe = Regex("(?i)\\b(bid'?ah of|grave worship|is shirk to|quburi|wahhabi|deviant sufi|sufis are)\\b")

    /** Structural checks on the model's JSON; returns candidate questions (not yet answer-checked). */
    fun parseGenerated(text: String, cat: QuizCat, level: Int?): List<QuizQuestion2> {
        val start = text.indexOf('['); val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        val arr = runCatching { JSONArray(text.substring(start, end + 1)) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val q = o.optString("q").trim()
            val opts = o.optJSONArray("options")?.let { a -> (0 until a.length()).map { a.optString(it).trim() } }.orEmpty()
            val a = o.optInt("answer", -1)
            val e = o.optString("explanation").trim()
            val s = o.optString("source").trim()
            val ok = q.length in 10..300 && opts.size == 4 && opts.all { it.isNotBlank() && it.length <= 140 } &&
                opts.map { it.lowercase() }.toSet().size == 4 && a in 0..3 && e.isNotBlank() && s.isNotBlank() &&
                !unsafe.containsMatchIn("$q ${opts.joinToString(" ")} $e")
            if (!ok) null else QuizQuestion2("ai_" + Integer.toHexString((q + opts.joinToString()).hashCode()), cat, level ?: 2,
                mapOf("en" to q), mapOf("en" to opts), a, mapOf("en" to e), "$s · checked by Noor AI", generated = true)
        }
    }

    fun checkPrompt(q: QuizQuestion2): String = buildString {
        append("Answer this quiz question. Reply with only the number of the correct option (1-4).\n")
        append(q.textFor("en")).append('\n')
        q.optionsFor("en").forEachIndexed { i, o -> append(i + 1).append(") ").append(o).append('\n') }
    }

    fun parseCheck(text: String?): Int? = text?.let { Regex("[1-4]").find(it)?.value?.toInt()?.minus(1) }

    /** Generates and independently re-answers each question; keeps only the ones whose answers agree. */
    suspend fun generate(cat: QuizCat, madhhab: String, level: Int?, count: Int = 5): List<QuizQuestion2> {
        val m = NoorAiOptions.normMadhhab(madhhab)
        val raw = AiFeatureService.ask(generationPrompt(cat, madhhab, count, level), NoorAiMode.AskNoor, language = "en", usePassages = false, madhhab = m)?.text
            ?: return emptyList()
        return parseGenerated(raw, cat, level).filter { q ->
            val check = AiFeatureService.ask(checkPrompt(q), NoorAiMode.AskNoor, language = "en", usePassages = false, madhhab = m)?.text
            parseCheck(check) == q.answer
        }
    }
}
