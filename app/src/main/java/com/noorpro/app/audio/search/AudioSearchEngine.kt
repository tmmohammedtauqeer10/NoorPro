package com.noorpro.app.audio.search

import java.text.Normalizer

/**
 * 1.0.90 unified audio search ranking. Pure Kotlin (unit-tested).
 * Tiers: EXACT > PREFIX > CONTAINS > FUZZY (typo-tolerant, Arabic diacritics-insensitive,
 * transliteration variants such as Rahman/Rehman, Yasin/Yaseen, Al-Fatiha/Fatihah).
 */
object AudioSearchEngine {
    const val EXACT = 4; const val PREFIX = 3; const val CONTAINS = 2; const val FUZZY = 1; const val NONE = 0

    private val tashkeel = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640\\u200C\\u200D]")

    /** Lowercase, strip Latin accents + Arabic tashkeel/tatweel, unify alef/yeh/heh forms, drop "al-", collapse variants. */
    fun normalize(s: String): String {
        var x = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036f]"), "").replace(tashkeel, "")
            .replace(Regex("[\\u0622\\u0623\\u0625\\u0671]"), "\u0627").replace('\u0649', '\u064A').replace('\u06CC', '\u064A')
            .replace('\u0629', '\u0647').replace('\u06C1', '\u0647').replace('\u06BE', '\u0647')
            .replace(Regex("[’'`ʿʾ‘]"), "")
        x = x.replace(Regex("(^|[^\\p{L}])((al|el|ul)[- ]|(as|an|ar|ad|ash|at)-)(?=\\p{L})"), "$1")
        x = x.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }.joinToString(" ")
        return x.replace(Regex("(ee|ea|ie|ey)"), "i").replace(Regex("(oo|ou)"), "u")
            .replace(Regex("([a-z])\\1+"), "$1").replace(Regex("(?<=[a-z]{3})h\\b"), "").trim()
    }

    /** Vowel-less consonant skeleton: rahman/rehman -> rhmn, yasin/yaseen -> ysn. */
    fun skeleton(s: String): String {
        val n = normalize(s).replace(" ", ""); if (n.isEmpty()) return n
        return n.first() + n.drop(1).replace(Regex("[aeiou]"), "")
    }

    fun lev(a: String, b: String): Int {
        if (a == b) return 0; if (a.isEmpty()) return b.length; if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1); cur[0] = i
            for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = cur
        }
        return prev[b.length]
    }

    /** Best tier of [query] against any of [fields]. */
    fun tier(query: String, fields: List<String>): Int {
        val q = normalize(query); if (q.isEmpty()) return NONE
        val qs = q.replace(" ", ""); val qk = skeleton(query)
        var best = NONE
        for (raw in fields) {
            if (raw.isBlank()) continue
            val f = normalize(raw); if (f.isEmpty()) continue
            val fs = f.replace(" ", "")
            val t = when {
                f == q || fs == qs -> EXACT
                f.startsWith(q) || fs.startsWith(qs) || f.split(" ").any { it.startsWith(q) } -> PREFIX
                f.contains(q) || fs.contains(qs) -> CONTAINS
                fuzzy(q, f, qk, raw) -> FUZZY
                else -> NONE
            }
            if (t > best) best = t; if (best == EXACT) return best
        }
        return best
    }

    private fun fuzzy(q: String, f: String, qk: String, raw: String): Boolean {
        if (q.length < 3) return false
        val words = f.split(" ")
        val tol = if (q.length <= 5) 1 else 2
        if (words.any { w -> lev(q, w) <= tol || (w.length > q.length && lev(q, w.take(q.length)) <= tol - (if (q.length <= 4) 1 else 0)) }) return true
        if (lev(q.replace(" ", ""), f.replace(" ", "")) <= tol) return true
        if (qk.length >= 3) { val fk = skeleton(raw); if (fk.contains(qk) || words.any { skeleton(it) == qk }) return true }
        return false
    }

    data class Hit<T>(val item: T, val tier: Int, val pop: Int)

    /** Rank [items] by tier, then popularity, then shorter title. */
    fun <T> rank(query: String, items: List<T>, fields: (T) -> List<String>, pop: (T) -> Int = { 0 }): List<T> =
        items.asSequence().map { Hit(it, tier(query, fields(it)), pop(it)) }.filter { it.tier > NONE }
            .sortedWith(compareByDescending<Hit<T>> { it.tier }.thenByDescending { it.pop }.thenBy { fields(it.item).firstOrNull()?.length ?: 0 })
            .map { it.item }.toList()
}
