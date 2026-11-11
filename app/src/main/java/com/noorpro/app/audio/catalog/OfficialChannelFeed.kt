package com.noorpro.app.audio.catalog

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 1.1.6: in-app playback of an official channel's latest videos (Official channels card).
 *
 * COMPLIANCE (YouTube API Services / embedded player terms):
 *  - The list comes from the channel's PUBLIC RSS feed (no API key, no secret in the app).
 *  - Videos play ONLY in YouTube's official IFrame player (youtube.com/embed via the IFrame API), with
 *    YouTube's own controls and branding. No overlays on the player, no ad blocking, no audio-only mode,
 *    no background play (paused on ON_STOP), no downloading/extraction.
 *  - Any player error (e.g. 101/150 = owner disabled embedding) falls back to opening the video in YouTube.
 *  - Gated by Remote Config [OfficialChannelFlags.EMBED] (default OFF): with it off, the card behaves
 *    exactly like 1.0.93 (link-out only).
 */

object OfficialChannelFlags {
    const val EMBED = "official_channel_embed_enabled"
    private val DEFAULTS = mapOf(EMBED to false)
    fun default(key: String) = DEFAULTS[key] ?: false
    /** Tests / previews can force values. */
    @Volatile var override: Map<String, Boolean>? = null
    private var loaded by mutableStateOf<Map<String, Boolean>>(emptyMap())
    @Volatile private var started = false
    fun isOn(key: String) = override?.get(key) ?: loaded[key] ?: DEFAULTS[key] ?: false
    val embedEnabled: Boolean get() = isOn(EMBED)

    fun load() {
        if (started) return
        started = true
        runCatching {
            if (!com.noorpro.app.data.FirebaseGate.ready) return
            val rc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            fun apply() {
                loaded = DEFAULTS.keys.mapNotNull { k ->
                    val v = rc.getValue(k)
                    if (v.source == com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_STATIC) null else k to v.asBoolean()
                }.toMap()
            }
            apply()
            rc.fetchAndActivate().addOnCompleteListener { runCatching { apply() } }
        }
    }
}

data class OfficialVideo(val id: String, val title: String, val published: String = "", val views: Long = 0) {
    /** YouTube's own thumbnail (i.ytimg.com). */
    val thumb: String get() = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$id"
    val dateLabel: String get() = runCatching {
        OffsetDateTime.parse(published).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }.getOrDefault("")
    val viewsLabel: String get() = when {
        views <= 0 -> ""
        views >= 1_000_000 -> String.format(Locale.ENGLISH, "%.1fM views", views / 1_000_000.0).replace(".0M", "M")
        views >= 1_000 -> String.format(Locale.ENGLISH, "%.1fK views", views / 1_000.0).replace(".0K", "K")
        else -> "$views views"
    }
    val meta: String get() = listOf(dateLabel, viewsLabel).filter { it.isNotBlank() }.joinToString(" · ")
}

object OfficialChannelEmbed {
    private val ID = Regex("^[\\w-]{11}$")
    private val CHANNEL = Regex("^UC[\\w-]{22}$")
    fun isVideoId(id: String) = ID.matches(id)
    fun isChannelId(id: String) = CHANNEL.matches(id)

    fun rssUrl(channelId: String): String? = if (isChannelId(channelId)) "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId" else null
    fun channelUrl(ch: OfficialChannel): String = if (isChannelId(ch.channelId)) "https://www.youtube.com/channel/${ch.channelId}" else ch.url
    /** YouTube's own subscribe-confirmation link for the channel (opens in YouTube). */
    fun subscribeUrl(ch: OfficialChannel): String = if (isChannelId(ch.channelId)) "https://www.youtube.com/channel/${ch.channelId}?sub_confirmation=1" else ch.url
    /** Plain official embed URL (documented player parameters only). */
    fun embedUrl(id: String): String? = if (isVideoId(id)) "https://www.youtube.com/embed/$id?playsinline=1&rel=0" else null

    /**
     * IFrame-player error codes. 101/150 = the owner does not allow embedded playback; 100 = removed/private;
     * 2/5 = bad id / HTML5 error; 152/153 = embedder config. Every error falls back to the YouTube app.
     */
    fun errorMessage(code: Int): String = when (code) {
        101, 150 -> "The channel doesn't allow this video to play outside YouTube."
        100 -> "This video is private or no longer available."
        else -> "This video can't play here."
    }
    fun shouldFallBack(code: Int): Boolean = code != 0

    /**
     * The official IFrame API player (no custom controls, nothing drawn over it). We only listen for
     * state + errors so we can fall back to YouTube when embedding isn't allowed.
     */
    fun playerHtml(id: String): String {
        require(isVideoId(id)) { "bad video id" }
        return """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;background:#000;height:100%;overflow:hidden}#p,iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style></head>
<body><div id="p"></div><script src="https://www.youtube.com/iframe_api"></script><script>
var player;function onYouTubeIframeAPIReady(){player=new YT.Player('p',{videoId:'$id',playerVars:{autoplay:1,playsinline:1,rel:0,controls:1,fs:1},
events:{onStateChange:function(e){NPOC.state(e.data);},onError:function(e){NPOC.error(e.data);}}});}
</script></body></html>"""
    }

    private fun unesc(s: String) = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")

    /** Parses YouTube's public channel Atom feed (latest ~15 uploads), newest first, de-duplicated. */
    fun parseRss(xml: String): List<OfficialVideo> =
        Regex("<entry>([\\s\\S]*?)</entry>").findAll(xml).mapNotNull { m ->
            val e = m.groupValues[1]
            val id = Regex("<yt:videoId>([\\w-]{11})</yt:videoId>").find(e)?.groupValues?.get(1) ?: return@mapNotNull null
            OfficialVideo(
                id = id,
                title = unesc(Regex("<title>([\\s\\S]*?)</title>").find(e)?.groupValues?.get(1).orEmpty()).trim(),
                published = Regex("<published>([^<]+)</published>").find(e)?.groupValues?.get(1).orEmpty().trim(),
                views = Regex("<media:statistics[^>]*views=\"(\\d+)\"").find(e)?.groupValues?.get(1)?.toLongOrNull() ?: 0,
            )
        }.distinctBy { it.id }.sortedByDescending { runCatching { OffsetDateTime.parse(it.published).toEpochSecond() }.getOrDefault(0) }.toList()
}

/** Latest uploads from the public RSS feed, cached for 30 min (last good copy used offline). */
object OfficialChannelRepository {
    private const val CACHE_MS = 30 * 60 * 1000L
    private fun cacheFile(c: Context, channelId: String) = File(c.cacheDir, "official_rss_$channelId.xml")

    private fun get(url: String): String = (URL(url).openConnection() as HttpURLConnection).run {
        connectTimeout = 10_000; readTimeout = 15_000
        try { if (responseCode !in 200..299) error("HTTP $responseCode"); inputStream.bufferedReader().readText() } finally { disconnect() }
    }

    fun cached(c: Context, channelId: String): List<OfficialVideo> =
        runCatching { OfficialChannelEmbed.parseRss(cacheFile(c, channelId).readText()) }.getOrDefault(emptyList())

    suspend fun load(c: Context, ch: OfficialChannel, force: Boolean = false): List<OfficialVideo> = withContext(Dispatchers.IO) {
        val url = OfficialChannelEmbed.rssUrl(ch.channelId) ?: return@withContext emptyList()
        val f = cacheFile(c, ch.channelId)
        if (!force && f.exists() && System.currentTimeMillis() - f.lastModified() < CACHE_MS) cached(c, ch.channelId).takeIf { it.isNotEmpty() }?.let { return@withContext it }
        runCatching { get(url) }.getOrNull()?.let { xml ->
            OfficialChannelEmbed.parseRss(xml).takeIf { it.isNotEmpty() }?.also { runCatching { f.writeText(xml) } }
        } ?: cached(c, ch.channelId)
    }
}
