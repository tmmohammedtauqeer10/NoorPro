package com.noorpro.app.audio.podcast

import android.content.Context
import android.util.Xml
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Al-Noor Audio "Podcasts" (1.0.69): curated public podcast RSS feeds, read live at runtime exactly
 * like any podcast app. Episodes stream from the publisher's own enclosure URL through the Al-Noor
 * player; nothing is downloaded for offline use or rehosted. Parsed feeds are cached (6h TTL) so the
 * list still shows offline.
 */
data class Podcast(
    val id: String,
    val name: String,
    val publisher: String,
    val language: String,
    val category: String,
    val feedUrl: String,
    val website: String,
    /** "podcast" (Podcasts tab) or "nasheed" (Nasheeds tab). */
    val kind: String = "podcast",
    val note: String = "",
)

data class Episode(
    val guid: String,
    val title: String,
    val audioUrl: String,
    val pubDateMs: Long,
    val durationMs: Long,
    val imageUrl: String?,
    /** 1.1.6: plain-text show notes (HTML stripped, capped) — timestamps here become chapters. */
    val notes: String = "",
    /** 1.1.6: Podcasting 2.0 `<podcast:chapters url=…>` JSON, fetched only when the episode is opened. */
    val chaptersUrl: String? = null,
    /** 1.1.6: inline Podlove Simple Chapters (`<psc:chapter start title>`). */
    val chapters: List<EpisodeChapter> = emptyList(),
)

/** One chapter marker inside an episode. */
data class EpisodeChapter(val startMs: Long, val title: String)

data class FeedResult(val artwork: String?, val episodes: List<Episode>, val fromCache: Boolean, val error: String? = null)

object PodcastRepository {
    val languages = listOf("English", "Urdu", "Hindi", "Arabic", "Bengali")
    val nasheedLanguages = listOf("Arabic", "Urdu", "English", "Bengali", "Turkish", "Malay")

    /** Every feed below was verified from the build box: HTTP 200, valid RSS, audio enclosures. */
    val podcasts = listOf(
        Podcast("yaqeen", "Yaqeen Podcast", "Yaqeen Institute", "English", "General", "https://rss.buzzsprout.com/1014445.rss", "https://yaqeeninstitute.org"),
        Podcast("firsts", "The Firsts with Dr. Omar Suleiman", "Yaqeen Institute", "English", "Seerah", "https://rss.buzzsprout.com/1194665.rss", "https://yaqeeninstitute.org"),
        Podcast("q30", "Qur'an 30 for 30", "Yaqeen Institute", "English", "Quran", "https://rss.buzzsprout.com/2449265.rss", "https://yaqeeninstitute.org"),
        Podcast("nak", "Nouman Ali Khan: Bayyinah Podcast", "Bayyinah Institute", "English", "Quran", "https://app.fusebox.fm/data/show/VNDXqyNW4X/feed", "https://bayyinah.com"),
        Podcast("hammad-qadri", "Shaykh Hammad Mustafa al-Madani al-Qadri", "Minhaj-ul-Quran (official)", "English", "Spirituality", "https://anchor.fm/s/d9c69d08/podcast/rss", "https://podcasters.spotify.com/pod/show/shaykh-hammad"),
        Podcast("mc-omar", "Omar Suleiman (Muslim Central)", "Muslim Central", "English", "Reminders", "https://rss.muslimcentral.com/omar-suleiman.rss", "https://muslimcentral.com"),
        Podcast("mc-yq", "Yasir Qadhi (Muslim Central)", "Muslim Central", "English", "Lectures", "https://rss.muslimcentral.com/yasir-qadhi.rss", "https://muslimcentral.com"),
        Podcast("mc-baajour", "Mohamad Baajour (Muslim Central)", "Muslim Central", "English", "Reminders", "https://rss.muslimcentral.com/mohamad-baajour.rss", "https://muslimcentral.com"),
        Podcast("seerah-jangda", "The Seerah Podcast — Abdul Nasir Jangda", "Qalam Institute", "English", "Seerah", "https://feeds.transistor.fm/the-sirah-podcast", "https://qalam.institute"),
        Podcast("qalam", "Qalam Institute Podcast", "Qalam Institute", "English", "General", "https://feeds.feedburner.com/qalaminstitute/podcast", "https://qalam.institute"),
        Podcast("seerah-yq", "Seerah of Prophet Muhammad ﷺ — Yasir Qadhi", "Supporting Yasir Qadhi", "English", "Seerah", "https://anchor.fm/s/1789206c/podcast/rss", "https://muslimcentral.com"),
        Podcast("tariq-jamil", "Tariq Jamil Podcast", "Maulana Tariq Jameel", "Urdu", "Bayan", "https://feeds.acast.com/public/shows/66c7556ca45b7b50379c85ef", "https://tariqjamil.org"),
        Podcast("israr", "Bayan ul Quran — Dr. Israr Ahmed", "BayanulQuran", "Urdu", "Tafsir", "https://anchor.fm/s/4cc85c20/podcast/rss", "https://tanzeem.org"),
        Podcast("minhaj-tuq", "Shaykh-ul-Islam Dr Muhammad Tahir-ul-Qadri", "Minhaj TV India (Minhaj-ul-Quran)", "Urdu", "Ahl-e-Sunnat (Barelvi) Bayan", "https://anchor.fm/s/cce4c560/podcast/rss", "https://www.minhaj.org"),
        Podcast("sdi-sadiq-razvi", "Maulana Mohammad Sadiq Razvi", "Sunni Dawat-e-Islami, Mumbai", "Urdu", "Ahl-e-Sunnat (Barelvi) Bayan", "https://anchor.fm/s/5b8bf7d0/podcast/rss", "https://sunnidawateislami.net"),
        Podcast("nak-bangla", "নোমান আলী খান বাংলা (Nouman Ali Khan in Bangla)", "NAK Bangla Podcast", "Bengali", "Quran", "https://rss.buzzsprout.com/1933402.rss", "https://www.nakbanglapodcast.com"),
        Podcast("seerah-bangla", "Seerah in Bangla — Yasir Qadhi (translated)", "Shaer Hassan & team", "Bengali", "Seerah", "https://anchor.fm/s/1154b8320/podcast/rss", ""),
        // Nasheeds tab: artists' / publishers' own official podcast feeds only.
        Podcast("nasheed-abu-hassan", "Abu Hassan Nasheeds", "Abu Hassan (self-written nasheeds)", "English", "Nasheed", "https://feeds.hubhopper.com/8b0cdd1331c87cd8e0f7810fc8cf0e42.rss", "https://www.abuhassanmedia.com", kind = "nasheed"),
        Podcast("naat-moin", "Naat by Moin", "Moin (own recordings)", "Urdu", "Naat", "https://anchor.fm/s/46e6ca58/podcast/rss", "", kind = "nasheed"),
        Podcast("nasheed-badsha-baitul", "Badsha Baitul Haque", "Badsha Baitul (official artist feed)", "Bengali", "Gojol / Nasheed", "https://feeds.hubhopper.com/15f6368ada81c93adebc162d410cd324.rss", "", kind = "nasheed"),
        Podcast("naat-geceleri", "Naat Geceleri", "Çağlayan Dergisi", "Turkish", "Naat / İlahi", "https://www.spreaker.com/show/3290986/episodes/feed", "", kind = "nasheed"),
        Podcast("qc-urdu", "Urdu Translation — Al-Huthaify (Quran Central)", "Quran Central", "Urdu", "Quran", "https://rss.qurancentral.com/urdu-translation-ali-bin-abdur-rahman-al-huthaify.rss", "https://qurancentral.com"),
        Podcast("nadwi", "Jumma Bayan — Maulana Abdul Aleem Khateeb Nadwi", "QV Audio (Quranic Voice)", "Hindi", "Bayan", "https://anchor.fm/s/2d923ab0/podcast/rss", ""),
        Podcast("hindi-minshawi", "Quran with Hindi translation (Minshawi)", "qurantranslations.net", "Hindi", "Quran", "https://www.qurantranslations.net/podcast/Hindi/Holy_Quran_in_the_Hindi_Language_minshawi.rss", "https://www.qurantranslations.net"),
        Podcast("nabulsi-baqarah", "تفسير سورة البقرة - محمد راتب النابلسي", "Tafsir Al-Quran Al-Kareem", "Arabic", "Tafsir", "https://anchor.fm/s/e949a630/podcast/rss", "https://nabulsi.com"),
        Podcast("hosny-seerah", "نور - السيرة النبوية - مصطفى حسني", "Mustafa Hosny", "Arabic", "Seerah", "https://anchor.fm/s/ef0fe82c/podcast/rss", ""),
    )

    fun categories(language: String, kind: String = "podcast") = listOf("All") + podcasts.filter { it.language == language && it.kind == kind }.map { it.category }.distinct()

    /** Preview/test hook: when set, used instead of the network. */
    @Volatile var previewFeeds: Map<String, FeedResult>? = null

    private const val UA = "NoorPro/1.0 (Android; podcast RSS reader)"
    private const val MAX_EPISODES = 60
    private const val TTL_MS = 6 * 60 * 60 * 1000L
    private val mem = HashMap<String, Pair<Long, FeedResult>>()

    /** Episodes already cached (memory/disk) — used by unified search; never hits the network. */
    fun cachedEpisodes(ctx: Context, p: Podcast): List<Episode> =
        previewFeeds?.get(p.id)?.episodes ?: mem[p.id]?.second?.episodes ?: readCache(ctx, p)?.episodes.orEmpty()

    fun cachedArtwork(ctx: Context, p: Podcast): String? =
        previewFeeds?.get(p.id)?.artwork ?: mem[p.id]?.second?.artwork ?: readCache(ctx, p)?.artwork

    suspend fun load(ctx: Context, p: Podcast, force: Boolean = false): FeedResult = withContext(Dispatchers.IO) {
        previewFeeds?.let { return@withContext it[p.id] ?: FeedResult(null, emptyList(), false, "Preview") }
        val now = System.currentTimeMillis()
        mem[p.id]?.let { (t, r) -> if (!force && now - t < TTL_MS) return@withContext r }
        val disk = readCache(ctx, p)
        if (!force && disk != null && now - cacheFile(ctx, p).lastModified() < TTL_MS) {
            mem[p.id] = cacheFile(ctx, p).lastModified() to disk; return@withContext disk
        }
        runCatching { fetch(p) }.fold(
            onSuccess = { r -> writeCache(ctx, p, r); mem[p.id] = now to r; r },
            onFailure = { e ->
                disk?.copy(fromCache = true, error = "offline")
                    ?: FeedResult(null, emptyList(), false, e.message ?: "Could not load feed")
            },
        )
    }

    fun toTrack(p: Podcast, e: Episode, artwork: String?) = Track(
        id = "pod-${p.id}-${e.guid.hashCode()}", title = e.title, artistId = "podcast-${p.id}", artistName = p.name,
        audioUrl = e.audioUrl, durationMs = e.durationMs, coverUrl = e.imageUrl?.takeIf { it.isNotBlank() } ?: artwork?.takeIf { it.isNotBlank() },
        license = LicenseType.PUBLISHER_RSS, attributionText = "${p.name} · via RSS · ${p.publisher}",
        sourceUrl = p.website.ifBlank { p.feedUrl }, licenseUrl = p.feedUrl, tags = listOf("podcast", p.category), language = p.language,
    )

    private fun fetch(p: Podcast): FeedResult {
        var url = URL(p.feedUrl)
        var conn: HttpURLConnection
        var hops = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000; readTimeout = 25000; instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA); setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
            }
            val code = conn.responseCode
            if (code in 300..399 && hops++ < 5) { url = URL(url, conn.getHeaderField("Location")); conn.disconnect(); continue }
            if (code != 200) { conn.disconnect(); throw IllegalStateException("HTTP $code") }
            break
        }
        return conn.inputStream.use { parse(it) }.also { conn.disconnect() }
    }

    internal fun parse(input: java.io.InputStream): FeedResult {
        val xp = Xml.newPullParser()
        xp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        xp.setInput(input, null)
        var artwork: String? = null
        val eps = ArrayList<Episode>()
        var inItem = false
        var title = ""; var guid = ""; var audio = ""; var date = 0L; var dur = 0L; var img: String? = null
        var notes = ""; var chUrl: String? = null; var chapters = ArrayList<EpisodeChapter>()
        var ev = xp.eventType
        while (ev != XmlPullParser.END_DOCUMENT && eps.size < MAX_EPISODES) {
            if (ev == XmlPullParser.START_TAG) {
                when (xp.name) {
                    "item" -> { inItem = true; title = ""; guid = ""; audio = ""; date = 0L; dur = 0L; img = null; notes = ""; chUrl = null; chapters = ArrayList() }
                    "itunes:image", "googleplay:image" -> { val h = xp.getAttributeValue(null, "href")?.trim()?.ifBlank { null }
                        if (h != null) { if (inItem) { if (img == null) img = h } else if (artwork == null) artwork = h } }
                    // 1.0.90: also media:thumbnail / media:content images (channel and episode level).
                    "media:thumbnail" -> { val h = xp.getAttributeValue(null, "url")?.trim()?.ifBlank { null }
                        if (h != null) { if (inItem) { if (img == null) img = h } else if (artwork == null) artwork = h } }
                    "media:content" -> { val h = xp.getAttributeValue(null, "url")?.trim()?.ifBlank { null }
                        val isImg = xp.getAttributeValue(null, "medium") == "image" || xp.getAttributeValue(null, "type").orEmpty().startsWith("image")
                        if (h != null && isImg) { if (inItem) { if (img == null) img = h } else if (artwork == null) artwork = h } }
                    "url" -> if (!inItem && artwork == null) artwork = xp.nextText().trim().ifBlank { null }
                    "enclosure" -> if (inItem) {
                        val type = xp.getAttributeValue(null, "type").orEmpty()
                        val u = xp.getAttributeValue(null, "url").orEmpty()
                        if (u.isNotBlank() && (type.startsWith("audio") || type.isBlank())) audio = u
                    }
                    "title" -> if (inItem) title = xp.nextText().trim()
                    "guid" -> if (inItem) guid = xp.nextText().trim()
                    "pubDate" -> if (inItem) date = parseDate(xp.nextText().trim())
                    "itunes:duration" -> if (inItem) dur = parseDuration(xp.nextText().trim())
                    // 1.1.6 bayan podcasts: show notes + chapter markers.
                    "description", "itunes:summary", "content:encoded" -> if (inItem) {
                        val t = runCatching { plainNotes(xp.nextText()) }.getOrDefault("")
                        if (t.length > notes.length) notes = t
                    }
                    "podcast:chapters" -> if (inItem) chUrl = xp.getAttributeValue(null, "url")?.trim()?.ifBlank { null }
                    "psc:chapter" -> if (inItem) {
                        val st = parseDuration(xp.getAttributeValue(null, "start").orEmpty().substringBefore('.').trim())
                        val ct = xp.getAttributeValue(null, "title").orEmpty().trim()
                        if (ct.isNotBlank()) chapters += EpisodeChapter(st, ct)
                    }
                }
            } else if (ev == XmlPullParser.END_TAG && xp.name == "item") {
                inItem = false
                if (audio.isNotBlank() && title.isNotBlank()) eps += Episode(guid.ifBlank { audio }, title, audio, date, dur, img, notes, chUrl, chapters.sortedBy { it.startMs })
            }
            ev = xp.next()
        }
        return FeedResult(artwork, eps, false)
    }

    /** HTML show notes → plain text with line breaks kept (timestamps sit at line starts). */
    internal fun plainNotes(html: String): String = html
        .replace(Regex("(?i)<br\\s*/?>|</p>|</li>|</div>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&").replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">")
        .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n").take(4000)

    internal fun parseDuration(s: String): Long {
        if (s.isBlank()) return 0L
        val parts = s.split(':').map { it.trim().toDoubleOrNull() ?: return 0L }
        val sec = parts.fold(0.0) { acc, v -> acc * 60 + v }
        return (sec * 1000).toLong()
    }

    private val dateFormats = listOf("EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, d MMM yyyy HH:mm:ss Z", "dd MMM yyyy HH:mm:ss Z")
    internal fun parseDate(s: String): Long {
        for (f in dateFormats) runCatching { return SimpleDateFormat(f, Locale.US).parse(s)!!.time }
        return 0L
    }

    private fun cacheFile(ctx: Context, p: Podcast) = File(File(ctx.cacheDir, "podcast_feeds").apply { mkdirs() }, "${p.id}.json")

    private fun writeCache(ctx: Context, p: Podcast, r: FeedResult) = runCatching {
        val arr = JSONArray()
        r.episodes.forEach { e -> arr.put(JSONObject().put("g", e.guid).put("t", e.title).put("u", e.audioUrl).put("d", e.pubDateMs).put("l", e.durationMs).put("i", e.imageUrl ?: "")
            .put("n", e.notes).put("cu", e.chaptersUrl ?: "")
            .put("ch", JSONArray().apply { e.chapters.forEach { c -> put(JSONObject().put("s", c.startMs).put("t", c.title)) } })) }
        cacheFile(ctx, p).writeText(JSONObject().put("art", r.artwork ?: "").put("eps", arr).toString())
    }

    private fun readCache(ctx: Context, p: Podcast): FeedResult? = runCatching {
        val f = cacheFile(ctx, p); if (!f.exists()) return null
        val o = JSONObject(f.readText()); val a = o.getJSONArray("eps")
        FeedResult(o.optString("art").ifBlank { null }, (0 until a.length()).map { i -> a.getJSONObject(i).let {
            Episode(it.getString("g"), it.getString("t"), it.getString("u"), it.optLong("d"), it.optLong("l"), it.optString("i").ifBlank { null },
                it.optString("n"), it.optString("cu").ifBlank { null },
                it.optJSONArray("ch")?.let { ch -> (0 until ch.length()).map { j -> ch.getJSONObject(j).let { c -> EpisodeChapter(c.optLong("s"), c.optString("t")) } } }.orEmpty()) } }, true)
    }.getOrNull()
}
