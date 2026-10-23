package com.noorpro.app.data.search

import java.text.Normalizer

/**
 * 1.0.91: search keys shared with the Cloud Function (functions/index.js reelSearchKey) — keep in sync.
 * Lowercase, accents and Arabic tashkeel stripped, alef/yeh/heh forms unified, Latin transliteration
 * variants folded (ee/ea/ie->i, oo/ou->u, doubled letters collapsed).
 */
object ReelSearchKeys {
    const val MAX_PREFIX = 15
    private val tashkeel = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")

    fun key(token: String): String {
        var x = Normalizer.normalize(token.lowercase(), Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036f]"), "").replace(tashkeel, "")
            .replace(Regex("[\\u0622\\u0623\\u0625\\u0671]"), "\u0627").replace('\u0649', '\u064A').replace('\u06CC', '\u064A')
            .replace('\u0629', '\u0647').replace('\u06C1', '\u0647').replace('\u06BE', '\u0647')
        x = x.replace(Regex("[^\\p{L}\\p{N}_]"), "")
        return x.replace(Regex("(ee|ea|ie)"), "i").replace(Regex("(oo|ou)"), "u").replace(Regex("([a-z])\\1+"), "$1")
    }

    fun tokens(text: String): List<String> = text.split(Regex("[^\\p{L}\\p{N}_\\u064B-\\u065F\\u0670]+")).map { key(it) }.filter { it.isNotEmpty() }

    /** Lowercase #hashtags (letters/digits/_ incl. Urdu/Arabic), without '#', de-duplicated. */
    fun hashtags(text: String): List<String> = Regex("""(?<![\p{L}\p{N}_])#([\p{L}\p{N}_]{1,40})""").findAll(text)
        .map { it.groupValues[1].lowercase() }.distinct().take(30).toList()

    /** The array-contains key the server stored for a query: first token's prefix (2..15 chars). */
    fun queryKey(q: String): String? = tokens(q.removePrefix("#").removePrefix("@")).maxByOrNull { it.length }?.take(MAX_PREFIX)?.takeIf { it.length >= 2 }
}
