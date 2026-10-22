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

    private val AYAH_COUNTS = intArrayOf(7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6)
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

    internal fun keywords(prompt: String): List<String> =
        prompt.lowercase().split(Regex("[^\\p{L}\\p{N}']+"))
            .map { it.trim('\'') }.filter { it.length >= 3 && it !in STOP }.distinct().take(8)

    private fun score(text: String, kws: List<String>): Int {
        val t = text.lowercase()
        return kws.sumOf { k -> if (t.contains(k)) 1 else 0 }
    }

    /** Builds the offline answer, or null when nothing relevant is found / assets are unavailable. */
    fun answer(prompt: String): String? {
        val ctx = appContext ?: return null
        val kws = keywords(prompt)
        if (kws.isEmpty()) return null
        return runCatching {
            val need = if (kws.size >= 3) 2 else 1
            val ayat = loadQuran(ctx).map { it to score(it.substringBefore('\t'), kws) }
                .filter { it.second >= need }.sortedByDescending { it.second }.take(3)
            val ahadith = loadHadith(ctx).map { it to score(it.first, kws) }
                .filter { it.second >= need }.sortedByDescending { it.second }.take(2)
            if (ayat.isEmpty() && ahadith.isEmpty()) return@runCatching null
            buildString {
                append("Noor AI's online assistant is unavailable right now, so here are related passages from the Qur'an and Hadith saved on your phone. These are quoted texts, not a full answer; please ask a qualified scholar for a ruling.\n")
                if (ayat.isNotEmpty()) {
                    append("\nQur'an (Sahih International):\n")
                    ayat.forEach { (l, _) -> append("- \"${l.substringBefore('\t')}\" (Qur'an ${l.substringAfter('\t')})\n") }
                }
                if (ahadith.isNotEmpty()) {
                    append("\nHadith:\n")
                    ahadith.forEach { (h, _) ->
                        val txt = h.first.let { if (it.length > 600) it.take(600).trimEnd() + "…" else it }
                        append("- \"$txt\" (${h.second})\n")
                    }
                }
                append("\nTap send again later for a full answer.")
            }
        }.getOrNull()
    }
}
