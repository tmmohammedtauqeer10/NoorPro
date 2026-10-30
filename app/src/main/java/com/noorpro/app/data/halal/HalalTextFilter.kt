package com.noorpro.app.data.halal

import android.content.Context
import com.squareup.moshi.JsonReader
import okio.Buffer
import java.text.Normalizer

/**
 * 1.1.5 Halal Guard text filter: the Kotlin port of functions/halal.js scanText()/maskText().
 * Word list = assets/halal/words.json (byte copy of functions/halal-words.json); both ports run the
 * same vectors (functions/halal-vectors.json) in HalalTextFilterTest. In the app this is a friendly,
 * on-device check: severe = can't send/post until edited, mild = warning (the server masks/queues).
 */
object HalalTextFilter {
    enum class Action { ALLOW, MASK, QUEUE, BLOCK }
    data class Match(val term: String, val cat: String, val severe: Boolean, val start: Int, val end: Int, val pattern: Boolean)
    data class Result(val action: Action, val categories: List<String>, val matches: List<Match>) {
        val blocked get() = action == Action.BLOCK
        val message: String get() = userMessage(this)
    }

    private data class Term(val raw: String, val n: String, val c: String, val severe: Boolean, val cat: String, val mode: String)
    private data class Pattern(val id: String, val rx: Regex, val severe: Boolean, val cat: String)
    private class Compiled(val terms: List<Term>, val patterns: List<Pattern>)

    @Volatile private var compiled: Compiled? = null

    fun ensureLoaded(context: Context) {
        if (compiled != null) return
        runCatching {
            context.applicationContext.assets.open("halal/words.json").bufferedReader().use { load(it.readText()) }
        }
    }

    fun load(json: String) { compiled = compile(json) }
    val isLoaded get() = compiled != null

    private val ZERO_WIDTH = Regex("[\\u200B-\\u200D\\u2060\\uFEFF\\u00AD]")
    private val TOKEN = Regex("[\\p{L}\\p{M}\\p{N}@$!|+*\\u200B-\\u200D\\u2060\\uFEFF\\u00AD]+")
    private val MN = Regex("\\p{Mn}")
    private val REPEAT = Regex("(.)\\1+")
    private val LEET = mapOf('0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '8' to 'b', '9' to 'g',
        '@' to 'a', '$' to 's', '!' to 'i', '|' to 'i', '+' to 't')

    fun normToken(raw: String): String {
        var x = Normalizer.normalize(raw.replace(ZERO_WIDTH, "").lowercase(), Normalizer.Form.NFD)
            .replace(MN, "").replace("\u0640", "")
            .replace(Regex("[\\u0622\\u0623\\u0625\\u0671]"), "\u0627").replace("\u0629", "\u0647")
            .replace(Regex("[\\u0649\\u06CC]"), "\u064A").replace("\u06A9", "\u0643")
            .replace(Regex("[\\u06C1\\u06BE]"), "\u0647")
        x = x.replace(Regex("^@+"), "").replace(Regex("^[!|+*]+"), "").replace(Regex("[!|+*]+$"), "")
        if (x.any { it in 'a'..'z' }) x = x.map { c -> if (c in "0134578@$!|+9") LEET[c] ?: c else c }.joinToString("")
        return x
    }

    fun collapse(s: String) = s.replace(REPEAT, "$1")

    @Suppress("UNCHECKED_CAST")
    private fun compile(json: String): Compiled {
        val root = JsonReader.of(Buffer().writeUtf8(json)).readJsonValue() as Map<String, Any?>
        val terms = (root["terms"] as? List<Map<String, Any?>>).orEmpty().mapNotNull { t ->
            val raw = t["t"] as? String ?: return@mapNotNull null
            val n = normToken(raw)
            if (n.isEmpty()) null else Term(raw, n, collapse(n), t["sev"] == "severe", t["cat"] as? String ?: "", t["mode"] as? String ?: "word")
        }
        val patterns = (root["patterns"] as? List<Map<String, Any?>>).orEmpty().map { p ->
            Pattern(p["id"] as String, Regex(p["re"] as String), p["sev"] == "severe", p["cat"] as? String ?: "")
        }
        return Compiled(terms, patterns)
    }

    private fun wildEq(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        for (i in a.indices) if (a[i] != '*' && a[i] != b[i]) return false
        return true
    }

    private fun hit(n: String, t: Term): Boolean {
        if ('*' in n) {
            val stars = n.count { it == '*' }
            if (n.length < 3 || n.length - stars < 2) return false
            return when (t.mode) {
                "word" -> wildEq(n, t.n)
                "stem" -> n.length >= t.n.length && wildEq(n.substring(0, t.n.length), t.n)
                else -> false
            }
        }
        val nc = collapse(n)
        val useC = t.c.length >= 3
        return when (t.mode) {
            "word" -> n == t.n || (useC && nc == t.c)
            "stem" -> n.startsWith(t.n) || (useC && nc.startsWith(t.c))
            else -> n.contains(t.n) || (useC && nc.contains(t.c))
        }
    }

    private data class Tok(val start: Int, val end: Int, val n: String)

    private fun tokenize(text: String): List<Tok> = TOKEN.findAll(text).mapNotNull { m ->
        val n = normToken(m.value)
        if (n.isEmpty()) null else Tok(m.range.first, m.range.last + 1, n)
    }.toList()

    private fun joinedRuns(tokens: List<Tok>): List<Tok> {
        val runs = mutableListOf<Tok>()
        var cur = mutableListOf<Tok>()
        fun flush() {
            if (cur.size >= 3) runs += Tok(cur.first().start, cur.last().end, cur.joinToString("") { it.n })
            cur = mutableListOf()
        }
        for (t in tokens) {
            val single = t.n.codePointCount(0, t.n.length) == 1
            val gapOk = cur.isEmpty() || t.start - cur.last().end <= 3
            if (single && gapOk) cur += t else {
                flush()
                if (single) cur += t
            }
        }
        flush()
        return runs
    }

    fun check(text: String): Result {
        val c = compiled ?: return Result(Action.ALLOW, emptyList(), emptyList())
        if (text.isBlank()) return Result(Action.ALLOW, emptyList(), emptyList())
        val tokens = tokenize(text)
        val matches = mutableListOf<Match>()
        for (tok in tokens + joinedRuns(tokens)) {
            for (t in c.terms) {
                if (hit(tok.n, t)) { matches += Match(t.raw, t.cat, t.severe, tok.start, tok.end, false); break }
            }
        }
        val joined = tokens.joinToString(" ") { it.n }
        for (p in c.patterns) {
            if (p.rx.containsMatchIn(joined)) matches += Match(p.id, p.cat, p.severe, -1, -1, true)
        }
        val action = when {
            matches.any { it.severe } -> Action.BLOCK
            matches.any { it.cat == "hate" || it.cat == "scam" } -> Action.QUEUE
            matches.isNotEmpty() -> Action.MASK
            else -> Action.ALLOW
        }
        return Result(action, matches.map { it.cat }.distinct(), matches)
    }

    fun mask(text: String, result: Result = check(text)): String {
        val spans = result.matches.filter { !it.pattern && it.start >= 0 }.sortedBy { it.start }
        val sb = StringBuilder()
        var pos = 0
        for (m in spans) {
            if (m.start < pos) continue
            val word = text.substring(m.start, m.end)
            val first = String(Character.toChars(word.codePointAt(0)))
            val len = word.codePointCount(0, word.length)
            sb.append(text, pos, m.start).append(first).append("\u2022".repeat(maxOf(2, len - 1)))
            pos = m.end
        }
        return sb.append(text.substring(pos)).toString()
    }

    private val CAT_LABEL = mapOf("profanity" to "bad language", "sexual" to "sexual content", "hate" to "hate or sectarian insults", "scam" to "a possible scam")

    /** Friendly, non-preachy wording shown under text fields. */
    fun userMessage(r: Result): String {
        val what = r.categories.joinToString(", ") { CAT_LABEL[it] ?: it }
        return when (r.action) {
            Action.BLOCK -> "This contains $what, which breaks the Halal community guidelines. Please edit it before sending."
            Action.QUEUE -> "This may contain $what. If posted, a moderator may review it."
            Action.MASK -> "Heads up: some words here ($what) will be hidden. Kind words carry reward."
            Action.ALLOW -> ""
        }
    }
}
