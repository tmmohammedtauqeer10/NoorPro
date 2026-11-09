package com.noorpro.app.audio.catalog

import android.content.Context
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Track
import com.noorpro.app.data.QuranMetaData
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 1.1.9: curated Qur'an recitations streamed from MP3Quran.net's public API / CDN.
 * Streaming only — audio files are never bundled in the AAB. Terms (eng/privacy):
 * "we allow any visitor or developer to copy any material or use any link".
 *
 * Fetch at runtime → cache on disk → offline-safe curated fallback with known CDN servers.
 */
object Mp3QuranCatalog {

    const val CREDIT = "Audio: MP3Quran.net"
    const val SOURCE_URL = "https://www.mp3quran.net"
    const val TERMS_URL = "https://www.mp3quran.net/eng/privacy"
    const val API_URL = "https://www.mp3quran.net/api/v3/reciters?language=eng"
    const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    private const val CACHE_NAME = "mp3quran_reciters_eng.json"

    val source = FreeAudioCatalog.AudioSource(
        key = "mp3quran",
        name = "MP3Quran.net",
        by = "MP3Quran.net · curated reciters",
        license = LicenseType.SOURCE_TERMS,
        licenseLabel = "Free streaming permitted by MP3Quran.net privacy/terms",
        licenseUrl = TERMS_URL,
        sourceUrl = SOURCE_URL,
    )

    /**
     * Curated, well-known set. [moshafId] is the preferred Hafs mushaf (114 surahs).
     * [fallbackServer] is the known CDN base verified at ship time (offline-safe).
     */
    data class Curated(
        val key: String,
        val displayName: String,
        val style: String,
        val reciterId: Int,
        val moshafId: Int,
        val fallbackServer: String,
    )

    data class ApiMoshaf(
        val id: Int,
        val name: String,
        val server: String,
        val surahTotal: Int,
        val surahList: List<Int>,
    )

    data class ApiReciter(
        val id: Int,
        val name: String,
        val moshafs: List<ApiMoshaf>,
    )

    data class Resolved(
        val curated: Curated,
        val server: String,
        val surahs: List<Int>,
        val fromApi: Boolean,
    ) {
        val subtitle: String get() = "${curated.style} · ${surahs.size} surahs"
    }

    val CURATED: List<Curated> = listOf(
        Curated("mishary-murattal", "Mishary Alafasy", "Murattal", 123, 123, "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/"),
        Curated("basit-murattal", "Abdul Basit Abdus Samad", "Murattal", 51, 53, "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r3/"),
        Curated("basit-mujawwad", "Abdul Basit Abdus Samad", "Mujawwad", 51, 51, "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r1/"),
        Curated("sudais-murattal", "Abdulrahman Al-Sudais", "Murattal", 54, 54, "https://cdn.mp3quran.net/audio/abdulrahman-sudais/r1/"),
        Curated("shuraim-murattal", "Saud Al-Shuraim", "Murattal", 31, 31, "https://cdn.mp3quran.net/audio/saud-shuraim/r1/"),
        Curated("ghamdi-murattal", "Saad Al-Ghamdi", "Murattal", 30, 30, "https://cdn.mp3quran.net/audio/saad-ghamdi/r1/"),
        Curated("maher-murattal", "Maher Al-Muaiqly", "Murattal", 102, 102, "https://cdn.mp3quran.net/audio/maher-muaiqly/r1/"),
        Curated("maher-mujawwad", "Maher Al-Muaiqly", "Mujawwad", 102, 133, "https://cdn.mp3quran.net/audio/maher-muaiqly/r3/"),
        Curated("minshawi-murattal", "Muhammad Siddiq Al-Minshawi", "Murattal", 112, 112, "https://cdn.mp3quran.net/audio/muhammad-minshawi/r1/"),
        Curated("minshawi-mujawwad", "Muhammad Siddiq Al-Minshawi", "Mujawwad", 112, 113, "https://cdn.mp3quran.net/audio/muhammad-minshawi/r2/"),
        Curated("husary-murattal", "Mahmoud Khalil Al-Husary", "Murattal", 118, 118, "https://cdn.mp3quran.net/audio/mahmoud-husary/r1/"),
        Curated("husary-mujawwad", "Mahmoud Khalil Al-Husary", "Mujawwad", 118, 119, "https://cdn.mp3quran.net/audio/mahmoud-husary/r2/"),
        Curated("dosari-murattal", "Yasser Al-Dosari", "Murattal", 92, 92, "https://cdn.mp3quran.net/audio/yasser-dosari/r1/"),
    )

    private val ALL_SURAHS: List<Int> = (1..114).toList()

    /** Surah audio URL: `{server}{surah:03d}.mp3` (e.g. …/001.mp3). */
    fun audioUrl(server: String, surah: Int): String {
        require(surah in 1..114) { "surah out of range: $surah" }
        val base = if (server.endsWith("/")) server else "$server/"
        return "%s%03d.mp3".format(java.util.Locale.US, base, surah)
    }

    fun parseSurahList(csv: String): List<Int> =
        csv.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..114 }.distinct()

    /**
     * Pure JVM-safe parser for the MP3Quran v3 reciters JSON.
     * Avoids org.json so unit tests run without Robolectric.
     */
    fun parseRecitersJson(json: String): List<ApiReciter> {
        val root = JsonLite.parse(json) as? Map<*, *> ?: return emptyList()
        val arr = root["reciters"] as? List<*> ?: return emptyList()
        return arr.mapNotNull { item ->
            val o = item as? Map<*, *> ?: return@mapNotNull null
            val id = (o["id"] as? Number)?.toInt() ?: return@mapNotNull null
            val name = o["name"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val moshafs = (o["moshaf"] as? List<*>).orEmpty().mapNotNull { m ->
                val mo = m as? Map<*, *> ?: return@mapNotNull null
                val mid = (mo["id"] as? Number)?.toInt() ?: return@mapNotNull null
                val server = mo["server"]?.toString()?.trim().orEmpty()
                if (!server.startsWith("https://")) return@mapNotNull null
                val total = (mo["surah_total"] as? Number)?.toInt() ?: 0
                val list = parseSurahList(mo["surah_list"]?.toString().orEmpty()).ifEmpty {
                    if (total >= 114) ALL_SURAHS else emptyList()
                }
                ApiMoshaf(mid, mo["name"]?.toString().orEmpty(), if (server.endsWith("/")) server else "$server/", total, list)
            }
            ApiReciter(id, name, moshafs)
        }
    }

    /** Merge live API data onto the curated set; fall back to hard-coded servers. */
    fun resolve(api: List<ApiReciter>?): List<Resolved> {
        val byReciter = api?.associateBy { it.id }.orEmpty()
        return CURATED.map { c ->
            val m = byReciter[c.reciterId]?.moshafs?.firstOrNull { it.id == c.moshafId }
            if (m != null && m.server.startsWith("https://") && m.surahList.isNotEmpty()) {
                Resolved(c, m.server, m.surahList, fromApi = true)
            } else {
                Resolved(c, c.fallbackServer, ALL_SURAHS, fromApi = false)
            }
        }
    }

    fun fallbackResolved(): List<Resolved> = resolve(null)

    fun tracksFor(r: Resolved): List<Track> = r.surahs.map { s ->
        val name = QuranMetaData.surahNamesEn.getOrElse(s - 1) { "Surah $s" }
        Track(
            id = "mp3q-${r.curated.key}-$s",
            title = "$s. $name",
            artistId = "mp3q-${r.curated.key}",
            artistName = "${r.curated.displayName} · ${r.curated.style}",
            audioUrl = audioUrl(r.server, s),
            durationMs = 0L,
            coverUrl = null,
            license = LicenseType.SOURCE_TERMS,
            attributionText = "$CREDIT · ${r.curated.displayName}",
            sourceUrl = SOURCE_URL,
            licenseUrl = TERMS_URL,
            tags = listOf("quran", "recitation", r.curated.style.lowercase()),
            language = "ar",
        )
    }

    fun findResolved(list: List<Resolved>, key: String): Resolved? = list.firstOrNull { it.curated.key == key }

    /* ---------------- disk cache ---------------- */

    fun cacheFile(ctx: Context): File = File(ctx.applicationContext.cacheDir, CACHE_NAME)

    /** Testable cache helpers — operate on a [File], no Android APIs beyond that. */
    fun isCacheFresh(file: File, nowMs: Long = System.currentTimeMillis(), ttlMs: Long = CACHE_TTL_MS): Boolean =
        file.isFile && file.length() > 0L && (nowMs - file.lastModified()) in 0 until ttlMs

    fun readCacheText(file: File): String? = runCatching {
        if (!file.isFile || file.length() == 0L) null else file.readText(Charsets.UTF_8)
    }.getOrNull()

    fun writeCacheText(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text, Charsets.UTF_8)
    }

    fun loadCachedReciters(file: File): List<ApiReciter>? =
        readCacheText(file)?.let { parseRecitersJson(it).takeIf { r -> r.isNotEmpty() } }

    fun resolvedFromCacheOrFallback(file: File, nowMs: Long = System.currentTimeMillis()): List<Resolved> {
        val cached = if (isCacheFresh(file, nowMs)) loadCachedReciters(file) else null
        return resolve(cached)
    }

    /** Blocking network fetch. Returns parsed reciters or null on failure. */
    fun fetchRecitersJson(): String? = runCatching {
        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                .takeIf { it.contains("\"reciters\"") }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /**
     * Prefer fresh cache; else network → write cache; else stale cache; else fallback.
     * Safe to call from a background thread.
     */
    fun loadResolved(ctx: Context): List<Resolved> {
        val file = cacheFile(ctx)
        if (isCacheFresh(file)) {
            loadCachedReciters(file)?.let { return resolve(it) }
        }
        val body = fetchRecitersJson()
        if (body != null) {
            runCatching { writeCacheText(file, body) }
            val parsed = parseRecitersJson(body)
            if (parsed.isNotEmpty()) return resolve(parsed)
        }
        loadCachedReciters(file)?.let { return resolve(it) }
        return fallbackResolved()
    }
}

/**
 * Minimal JSON → Map/List/Number/String/Boolean/null reader for JVM unit tests
 * (no org.json / Moshi codegen dependency).
 */
internal object JsonLite {
    fun parse(text: String): Any? = Parser(text).parseValue()

    private class Parser(private val s: String) {
        private var i = 0
        fun parseValue(): Any? {
            skip()
            if (i >= s.length) return null
            return when (s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> { expect("true"); true }
                'f' -> { expect("false"); false }
                'n' -> { expect("null"); null }
                '-', in '0'..'9' -> parseNumber()
                else -> null
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{'); skip()
            val out = linkedMapOf<String, Any?>()
            if (peek('}')) { i++; return out }
            while (true) {
                skip(); val key = parseString(); skip(); expect(':'); val v = parseValue(); out[key] = v
                skip()
                when {
                    peek(',') -> { i++; continue }
                    peek('}') -> { i++; break }
                    else -> break
                }
            }
            return out
        }

        private fun parseArray(): List<Any?> {
            expect('['); skip()
            val out = mutableListOf<Any?>()
            if (peek(']')) { i++; return out }
            while (true) {
                out += parseValue(); skip()
                when {
                    peek(',') -> { i++; continue }
                    peek(']') -> { i++; break }
                    else -> break
                }
            }
            return out
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                when (c) {
                    '"' -> break
                    '\\' -> {
                        if (i >= s.length) break
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\u0008')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 <= s.length) {
                                    val hex = s.substring(i, i + 4)
                                    sb.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                                    i += 4
                                }
                            }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseNumber(): Number {
            val start = i
            if (peek('-')) i++
            while (i < s.length && s[i] in '0'..'9') i++
            var isDouble = false
            if (peek('.')) {
                isDouble = true; i++
                while (i < s.length && s[i] in '0'..'9') i++
            }
            if (peek('e') || peek('E')) {
                isDouble = true; i++
                if (peek('+') || peek('-')) i++
                while (i < s.length && s[i] in '0'..'9') i++
            }
            val raw = s.substring(start, i)
            return if (isDouble) raw.toDouble() else raw.toLong().let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
        }

        private fun skip() {
            while (i < s.length) {
                when (s[i]) {
                    ' ', '\n', '\r', '\t' -> i++
                    else -> return
                }
            }
        }

        private fun peek(c: Char) = i < s.length && s[i] == c
        private fun expect(c: Char) { skip(); if (i < s.length && s[i] == c) i++ }
        private fun expect(word: String) {
            skip()
            if (s.regionMatches(i, word, 0, word.length)) i += word.length
        }
    }
}
