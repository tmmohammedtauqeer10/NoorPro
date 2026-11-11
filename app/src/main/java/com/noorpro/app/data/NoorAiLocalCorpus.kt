package com.noorpro.app.data

import android.content.Context
import org.json.JSONObject
import java.util.zip.GZIPInputStream

/**
 * Offline fallback for Noor AI: keyword search over the bundled Qur'an (Sahih International) and
 * Sahih al-Bukhari / Sahih Muslim (English) assets. Returns quoted texts with references only;
 * it never generates an answer of its own.
 */
object NoorAiLocalCorpus {
    @Volatile var appContext: Context? = null

    internal val AYAH_COUNTS = intArrayOf(7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6)
    private val COLLECTIONS = listOf("eng-bukhari" to "Sahih al-Bukhari", "eng-muslim" to "Sahih Muslim")
    private val STOP = setOf("the","and","for","what","how","why","who","when","does","did","can","should","about","with","that","this","from","have","has","are","was","were","you","your","is","in","of","to","a","an","it","on","me","my","i","do","be","or","as","at","by","we","our","they","their","tell","explain","please","islam","islamic","allah","quran","hadith","there","which","will","would","into","any","some","more","than","them","his","her","its","not","but","all","one")

    @Volatile private var quran: List<String>? = null
    @Volatile private var hadith: List<Pair<String, String>>? = null

    private fun gz(ctx: Context, path: String): String =
        GZIPInputStream(ctx.assets.open(path)).bufferedReader().use { it.readText() }

    private fun loadQuran(ctx: Context): List<String> = quran ?: run {
        val lines = gz(ctx, "quran/tr/en.sahih.jgz").split('\n')
        val out = ArrayList<String>(6236)
        var i = 0
        for ((s, n) in AYAH_COUNTS.withIndex()) for (a in 1..n) {
            if (i < lines.size) out.add("${lines[i].trim()}\t${s + 1}:$a")
            i++
        }
        out.also { quran = it }
    }

    private fun loadHadith(ctx: Context): List<Pair<String, String>> = hadith ?: run {
        val out = ArrayList<Pair<String, String>>()
        for ((dir, name) in COLLECTIONS) {
            val files = ctx.assets.list("hadith/$dir").orEmpty()
            for (f in files) runCatching {
                val arr = JSONObject(gz(ctx, "hadith/$dir/$f")).optJSONArray("hadiths") ?: return@runCatching
                for (k in 0 until arr.length()) {
                    val o = arr.optJSONObject(k) ?: continue
                    val t = o.optString("text").trim()
                    if (t.length > 20) out.add(t to "$name ${o.optString("hadithnumber")}")
                }
            }
        }
        out.also { hadith = it }
    }

    private val SYNONYMS = mapOf(
        "salah" to "pray", "salat" to "pray", "salaah" to "pray", "namaz" to "pray", "namaaz" to "pray", "prayer" to "pray", "prayers" to "pray",
        "sabr" to "patien", "patience" to "patien", "patient" to "patien", "dua" to "supplicat", "duaa" to "supplicat",
        "sawm" to "fast", "roza" to "fast", "fasting" to "fast", "jannah" to "paradise", "jahannam" to "hell", "zakat" to "zakat",
        "khushu" to "humbl", "focus" to "humbl", "concentration" to "humbl",
    )

    internal fun keywords(prompt: String): List<String> =
        prompt.lowercase().split(Regex("[^\\p{L}\\p{N}']+"))
            .map { it.trim('\'') }.filter { it.length >= 3 && it !in STOP && it.all { c -> c in 'a'..'z' || c == '\'' } }
            .map { SYNONYMS[it] ?: it }.distinct().take(8)

    /** Distinct keywords found at a word start (so "pray" matches "prayer" but "cat" doesn't match "supplicate"). */
    internal fun score(text: String, kws: List<String>): Int = scoreP(text, patterns(kws))
    private fun patterns(kws: List<String>) = kws.map { k -> Regex("(^|[^\\p{L}])" + Regex.escape(k)) }
    private fun scoreP(text: String, pats: List<Regex>): Int {
        val t = text.lowercase()
        return pats.count { it.containsMatchIn(t) }
    }

    /** Minimum distinct keyword hits for a passage to count as related (strict, so no loosely related dumps). */
    internal fun needed(keywordCount: Int): Int = maxOf(2, kotlin.math.ceil(keywordCount * 0.6).toInt())

    /** Retrieval for the online Worker: up to [max] related passages as (reference, text). English questions only. */
    fun passages(prompt: String, max: Int = 5): List<Pair<String, String>> {
        val ctx = appContext ?: return emptyList()
        val kws = keywords(prompt)
        if (kws.size < 2) return emptyList()
        return runCatching {
            val need = 2.coerceAtMost(kws.size)
            val pats = patterns(kws)
            val ayat = loadQuran(ctx).asSequence().map { it to scoreP(it.substringBefore('\t'), pats) }
                .filter { it.second >= need }.sortedByDescending { it.second }.take(3)
                .map { (l, _) -> "Qur'an ${l.substringAfter('\t')}" to l.substringBefore('\t').take(500) }.toList()
            val ahadith = loadHadith(ctx).asSequence().map { it to scoreP(it.first, pats) }
                .filter { it.second >= need }.sortedByDescending { it.second }.take(2)
                .map { (h, _) -> h.second to h.first.take(500) }.toList()
            (ayat + ahadith).take(max)
        }.getOrDefault(emptyList())
    }

    /**
     * Offline answer: only strongly related passages (most of the question's keywords present), short quotes,
     * localized heading. Returns null when nothing clearly matches, so the caller shows a plain "offline" note.
     */
    fun answer(prompt: String, lang: String = "en"): String? {
        val ctx = appContext ?: return null
        val kws = keywords(prompt)
        if (kws.size < 2) return null
        return runCatching {
            val need = needed(kws.size)
            val pats = patterns(kws)
            val ayat = loadQuran(ctx).map { it to scoreP(it.substringBefore('\t'), pats) }
                .filter { it.second >= need }.sortedByDescending { it.second }.take(2)
            val ahadith = loadHadith(ctx).map { it to scoreP(it.first, pats) }
                .filter { it.second >= need && it.first.first.length <= 700 }.sortedByDescending { it.second }.take(1)
            if (ayat.isEmpty() && ahadith.isEmpty()) return@runCatching null
            buildString {
                append(NoorAiLanguage.offlineHeading(lang)).append('\n')
                ayat.forEach { (l, _) -> append("\n> \"${l.substringBefore('\t').take(320)}\" (Qur'an ${l.substringAfter('\t')})\n") }
                ahadith.forEach { (h, _) -> append("\n> \"${h.first.take(320).let { if (h.first.length > 320) it.trimEnd() + "…" else it }}\" (${h.second})\n") }
                append("\n").append(NoorAiLanguage.unavailable(lang))
            }
        }.getOrNull()
    }
}
