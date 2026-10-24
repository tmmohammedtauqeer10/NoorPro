package com.noorpro.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 1.0.86: featured "MAF Nasheed" YouTube channel shelf on the Nasheeds tab.
 * Video list comes from our youtubeChannelFeed Cloud Function (public channel RSS + channel page,
 * cached server-side so the list updates without an app release); falls back to the public RSS feed
 * directly, then to the last cached copy. Playback is ONLY via YouTube's official embedded player.
 */
const val MAF_CHANNEL_ID = "UCDPznGiG1OrrNsRgmSCcAgg"
const val MAF_CHANNEL_NAME = "MAF Nasheed Channel"
const val MAF_CHANNEL_URL = "https://www.youtube.com/channel/$MAF_CHANNEL_ID"
private const val YT_FEED_FN = "https://us-central1-noor-pro-d87e3.cloudfunctions.net/youtubeChannelFeed?channel=$MAF_CHANNEL_ID"
private const val YT_RSS = "https://www.youtube.com/feeds/videos.xml?channel_id=$MAF_CHANNEL_ID"
private const val CACHE_MS = 30 * 60 * 1000L

data class YtVideo(val id: String, val title: String, val published: String = "", val ago: String = "", val audioUrl: String = "") {
    val hasAudio get() = audioUrl.startsWith("https://")
    val thumb get() = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
    val dateLabel: String get() = runCatching {
        OffsetDateTime.parse(published).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }.getOrNull() ?: ago
}
/** 1.0.90: owner audio not (yet) on YouTube; the feed function auto-links it to a future matching video. */
data class YtAudioOnly(val key: String, val title: String, val titleEn: String, val audioUrl: String, val durationSec: Double = 0.0) {
    val label get() = titleEn.ifBlank { title }
    fun toTrack(cover: String? = null) = com.noorpro.app.audio.models.Track(
        id = "maf-new-$key", title = if (titleEn.isNotBlank() && title.isNotBlank()) "$titleEn — $title" else label, artistId = "maf-nasheed",
        artistName = "MAF Nasheed", audioUrl = audioUrl, durationMs = (durationSec * 1000).toLong(), coverUrl = cover,
        license = com.noorpro.app.audio.models.LicenseType.PERMISSION_LETTER, attributionText = "$MAF_CHANNEL_NAME · owner-provided audio (new)",
        sourceUrl = MAF_CHANNEL_URL, tags = listOf("nasheed", "maf", "new"))
}
data class YtChannelFeed(val title: String, val avatar: String, val videos: List<YtVideo>, val audioOnly: List<YtAudioOnly> = emptyList())

object YtChannelRepository {
    private fun cacheFile(c: Context) = File(c.cacheDir, "yt_$MAF_CHANNEL_ID.json")

    internal fun parseJson(s: String): YtChannelFeed {
        val o = JSONObject(s); val a = o.optJSONArray("videos") ?: JSONArray()
        val v = (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { j ->
            val id = j.optString("id"); if (!Regex("^[\\w-]{11}$").matches(id)) null
            else YtVideo(id, j.optString("title"), j.optString("published"), j.optString("ago"), j.optString("audioUrl")) } }
        val ao = o.optJSONArray("audioOnly") ?: JSONArray()
        val n = (0 until ao.length()).mapNotNull { i -> ao.optJSONObject(i)?.let { j ->
            val u = j.optString("audioUrl"); if (!u.startsWith("https://") || Regex("youtube\\.com|youtu\\.be|googlevideo").containsMatchIn(u)) null
            else YtAudioOnly(j.optString("key"), j.optString("title"), j.optString("titleEn"), u, j.optDouble("durationSec", 0.0)) } }
        return YtChannelFeed(o.optString("title", MAF_CHANNEL_NAME).ifBlank { MAF_CHANNEL_NAME }, o.optString("avatar"), v, n)
    }

    internal fun parseRss(xml: String): YtChannelFeed {
        fun unesc(s: String) = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        val v = Regex("<entry>([\\s\\S]*?)</entry>").findAll(xml).mapNotNull { m ->
            val e = m.groupValues[1]
            val id = Regex("<yt:videoId>([\\w-]{11})</yt:videoId>").find(e)?.groupValues?.get(1) ?: return@mapNotNull null
            YtVideo(id, unesc(Regex("<title>([\\s\\S]*?)</title>").find(e)?.groupValues?.get(1).orEmpty()),
                Regex("<published>([^<]+)</published>").find(e)?.groupValues?.get(1).orEmpty())
        }.toList()
        return YtChannelFeed(MAF_CHANNEL_NAME, "", v)
    }

    private fun get(url: String): String = (URL(url).openConnection() as HttpURLConnection).run {
        connectTimeout = 10_000; readTimeout = 15_000
        try { if (responseCode !in 200..299) error("HTTP $responseCode"); inputStream.bufferedReader().readText() } finally { disconnect() }
    }

    fun cached(c: Context): YtChannelFeed? = runCatching { parseJson(cacheFile(c).readText()) }.getOrNull()?.takeIf { it.videos.isNotEmpty() }

    suspend fun load(c: Context, force: Boolean = false): YtChannelFeed? = withContext(Dispatchers.IO) {
        val f = cacheFile(c)
        if (!force && f.exists() && System.currentTimeMillis() - f.lastModified() < CACHE_MS) cached(c)?.let { return@withContext it }
        val fresh = runCatching { get(YT_FEED_FN).let { s -> parseJson(s).also { if (it.videos.isNotEmpty()) f.writeText(s) } } }.getOrNull()
            ?.takeIf { it.videos.isNotEmpty() }
            ?: runCatching { parseRss(get(YT_RSS)) }.getOrNull()?.takeIf { it.videos.isNotEmpty() }?.also { feed ->
                f.writeText(JSONObject().put("title", feed.title).put("videos", JSONArray(feed.videos.map {
                    JSONObject().put("id", it.id).put("title", it.title).put("published", it.published) })).toString())
            }
        fresh ?: cached(c)
    }
}

fun openYouTube(c: Context, url: String) {
    runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Featured channel shelf: channel card (credit + View channel / Subscribe) and a row of video cards. */
@Composable
internal fun MafNasheedShelf(t: SpTheme, preview: YtChannelFeed? = null) {
    val ctx = LocalContext.current
    var feed by remember { mutableStateOf(preview ?: if (preview == null) YtChannelRepository.cached(ctx) else null) }
    var failed by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<YtVideo?>(null) }
    if (preview == null) LaunchedEffect(Unit) { val f = YtChannelRepository.load(ctx); if (f != null) feed = f else failed = feed == null }
    val red = Color(0xFFE53935)
    Column(Modifier.fillMaxWidth()) {
        // Channel card
        Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF7A1F3D), Color(0xFF3A1240), if (t.light) Color(0xFF221133) else Color(0xFF120A1A))))
            .padding(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFC857), Color(0xFFE0457B)))), contentAlignment = Alignment.Center) {
                        Text("MAF", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        if (!feed?.avatar.isNullOrBlank()) AsyncImage(feed!!.avatar, MAF_CHANNEL_NAME, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("FEATURED CHANNEL", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text("MAF Nasheed", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(feed?.let { "${it.videos.size} videos · on YouTube" } ?: "Nasheeds & naats · on YouTube", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.clip(CircleShape).background(red).clickable { openYouTube(ctx, "$MAF_CHANNEL_URL?sub_confirmation=1") }.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) { Text("Subscribe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    Row(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable { openYouTube(ctx, MAF_CHANNEL_URL) }.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OpenInNew, null, tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp))
                        Text("View channel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Latest from MAF Nasheed", color = t.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        val vids = feed?.videos.orEmpty()
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (vids.isEmpty() && !failed) items(4) { SpShimmer(t, Modifier.width(232.dp).height(130.dp).clip(RoundedCornerShape(8.dp))) }
            if (vids.isEmpty() && failed) item {
                Text("Couldn't load videos. Open the channel on YouTube.", color = t.muted, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { openYouTube(ctx, MAF_CHANNEL_URL) }.padding(8.dp))
            }
            items(feed?.audioOnly.orEmpty(), key = { "new-" + it.key }) { a -> YtNewAudioCard(t, a) }
            items(vids, key = { it.id }) { v -> YtVideoCard(t, v) { playing = v } }
        }
        Spacer(Modifier.height(6.dp))
        Text("Videos © MAF Nasheed Channel · played with YouTube's official player", color = t.muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp))
    }
    playing?.let { v -> YtPlayerDialog(t, v, feed?.videos.orEmpty()) { playing = null } }
}

@Composable
private fun YtVideoCard(t: SpTheme, v: YtVideo, onClick: () -> Unit) {
    Column(Modifier.width(232.dp).pressScale(onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(t.high), contentAlignment = Alignment.Center) {
            AsyncImage(v.thumb, v.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
            }
            if (v.hasAudio) Text("AUDIO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 5.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(v.title, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
        Text(listOf("MAF Nasheed", v.dateLabel).filter { it.isNotBlank() }.joinToString(" · "), color = t.muted, fontSize = 12.sp, maxLines = 1)
    }
}

/** Audio-only "New" card: our hosted file on ExoPlayer/MediaSession with a generated cover. */
@Composable
private fun YtNewAudioCard(t: SpTheme, a: YtAudioOnly) {
    val ctx = LocalContext.current
    val cover = remember(a.key) { runCatching { com.noorpro.app.audio.art.CoverArt.fileFor(ctx, a.label, "NEW NASHEED", "maf-${a.key}").toURI().toString() }.getOrNull() }
    Column(Modifier.width(232.dp).pressScale { val tr = a.toTrack(cover); playFree(ctx, tr, listOf(tr)) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            SpArt(Modifier.fillMaxSize(), tint = Color(0xFF5B2A86), label = a.label, corner = 8.dp)
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
            }
            Text("NEW", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(MafGold).padding(horizontal = 6.dp, vertical = 2.dp))
            Text("AUDIO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 5.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(a.label, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(listOf(a.title, "New · audio only").filter { it.isNotBlank() }.joinToString(" · "), color = t.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal val MafGold = Color(0xFFF5C542)
/** Screenshot tests only: local artwork instead of i.ytimg.com. */
internal var mafPreviewArt: ((String) -> String?)? = null

/** Owner-uploaded audio (never YouTube audio) as an Al-Noor ExoPlayer track: background play,
 *  notification + lock-screen controls via the shared MediaSession, YouTube thumbnail as artwork. */
internal fun YtVideo.toTrack() = com.noorpro.app.audio.models.Track(
    id = "maf-$id", title = title, artistId = "maf-nasheed", artistName = "MAF Nasheed", audioUrl = audioUrl, durationMs = 0L,
    coverUrl = "https://i.ytimg.com/vi/$id/maxresdefault.jpg", license = com.noorpro.app.audio.models.LicenseType.PERMISSION_LETTER,
    attributionText = "$MAF_CHANNEL_NAME · owner-provided audio", sourceUrl = "https://www.youtube.com/watch?v=$id", tags = listOf("nasheed", "maf"),
)

/** Audio | Video pill: the active option sits on an animated gold highlight. */
@Composable
internal fun AudioVideoPill(t: SpTheme, audio: Boolean, audioEnabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val w = 92.dp
    val x by animateDpAsState(if (audio) 0.dp else w, spring(dampingRatio = 0.78f, stiffness = 520f), label = "pill")
    Box(Modifier.width(w * 2 + 8.dp).height(38.dp).clip(CircleShape).background(Color(0xFF2A2A2A)).padding(4.dp)) {
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(CircleShape).background(MafGold))
        Row(Modifier.fillMaxSize()) {
            listOf(true, false).forEach { a ->
                val sel = a == audio; val enabled = !a || audioEnabled
                val col by animateColorAsState(when { sel -> Color.Black; enabled -> Color.White; else -> Color.White.copy(alpha = 0.4f) }, tween(220), label = "pc")
                Box(Modifier.width(w).fillMaxHeight().clip(CircleShape).clickable(enabled = enabled && !sel) { onChange(a) }, contentAlignment = Alignment.Center) {
                    Text(if (a) "Audio" else "Video", color = col, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/** Bridge from the official YouTube IFrame API (state + time) back to Compose. */
internal class YtBridge(val onState: (Int) -> Unit, val onTime: (Long, Long) -> Unit) {
    private val h = android.os.Handler(android.os.Looper.getMainLooper())
    @android.webkit.JavascriptInterface fun state(s: Int) { h.post { onState(s) } }
    @android.webkit.JavascriptInterface fun time(pos: Double, dur: Double) { h.post { onTime(pos.toLong(), dur.toLong()) } }
}

internal fun ytHtml(id: String, startSec: Long) = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;background:#000;height:100%;overflow:hidden}#p,iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style></head>
<body><div id="p"></div><script src="https://www.youtube.com/iframe_api"></script><script>
var player;function onYouTubeIframeAPIReady(){player=new YT.Player('p',{videoId:'$id',playerVars:{autoplay:1,playsinline:1,rel:0,modestbranding:1,start:$startSec},
events:{onReady:function(e){e.target.playVideo();},onStateChange:function(e){NP.state(e.data);}}});
setInterval(function(){try{if(player&&player.getCurrentTime)NP.time(player.getCurrentTime()*1000,player.getDuration()*1000);}catch(x){}},500);}
</script></body></html>"""

/**
 * 1.0.90 full-screen MAF player.
 * COMPLIANCE (YouTube ToS): YouTube content is only ever played by YouTube's official IFrame player, which stays
 * visible whenever it plays. Audio mode for a video WITH owner audio (nasheed_audio) stops/removes the YouTube player
 * and plays our hosted file on ExoPlayer. Audio mode for a video WITHOUT owner audio keeps the YouTube player
 * playing as a visible minimised player under the artwork; nothing is extracted, hidden, or backgrounded.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YtPlayerDialog(t: SpTheme, v: YtVideo, all: List<YtVideo>, previewAudio: Boolean? = null, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val list = remember(all, v) { all.ifEmpty { listOf(v) } }
    var cur by remember(v.id) { mutableStateOf(v) }
    var audio by remember(v.id) { mutableStateOf(previewAudio ?: true) }
    val owner = cur.hasAudio
    val queue = remember(list) { list.filter { it.hasAudio }.map { it.toTrack() } }
    val track = remember(cur.id) { if (cur.hasAudio) cur.toTrack() else null }
    val live = previewAudio == null && com.noorpro.app.audio.AlNoorAudioSession.isInitialized()
    val pb by (if (live) com.noorpro.app.audio.AlNoorAudioSession.player.playback else remember { kotlinx.coroutines.flow.MutableStateFlow(com.noorpro.app.audio.models.PlaybackState()) }).collectAsState()
    val q by (if (live) com.noorpro.app.audio.AlNoorAudioSession.player.queue else remember { kotlinx.coroutines.flow.MutableStateFlow(com.noorpro.app.audio.models.QueueState()) }).collectAsState()
    val useExo = audio && owner
    val isCur = track != null && q.currentTrack?.id == track.id
    // YouTube player state
    var web by remember { mutableStateOf<WebView?>(null) }
    var ytPlaying by remember { mutableStateOf(previewAudio != null) }
    var ytPos by remember { mutableStateOf(if (previewAudio != null) 72_000L else 0L) }
    var ytDur by remember { mutableStateOf(if (previewAudio != null) 245_000L else 0L) }
    var repeat by remember { mutableStateOf(false) }
    var liked by remember(cur.id) { mutableStateOf(cur.id in SpStore.set(ctx, "maf_liked")) }
    var info by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val idx = list.indexOfFirst { it.id == cur.id }
    fun go(d: Int) { if (list.size > 1) cur = list[Math.floorMod((if (idx < 0) 0 else idx) + d, list.size)] }
    fun js(c: String) { web?.evaluateJavascript("try{$c}catch(e){}", null) }
    val onYtState: (Int) -> Unit = { st -> ytPlaying = st == 1 || st == 3; if (st == 0) { if (repeat) js("player.seekTo(0,true);player.playVideo();") else go(1) } }
    val ytStateNow by rememberUpdatedState(onYtState)
    // Owner audio on ExoPlayer: start when entering audio mode; YouTube is removed (not hidden) in that case.
    LaunchedEffect(useExo, cur.id) {
        if (previewAudio != null) return@LaunchedEffect
        if (useExo && track != null) { if (!isCur) playFree(ctx, track, queue.ifEmpty { listOf(track) }) }
        else if (isCur) runCatching { com.noorpro.app.audio.AlNoorAudioSession.player.pause() }
    }
    LaunchedEffect(cur.id) { if (!useExo) js("player.loadVideoById('${cur.id}');") }
    // ToS: the YouTube player never plays while not visible — pause it when the app goes to the background.
    val owner2 = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner2) {
        val o = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) js("player.pauseVideo();") }
        owner2.lifecycle.addObserver(o); onDispose { owner2.lifecycle.removeObserver(o) }
    }
    val playing = if (useExo) isCur && pb.isPlaying else ytPlaying
    val pos = if (useExo) (if (isCur) pb.positionMs else 0L) else ytPos
    val dur = if (useExo) (if (isCur) pb.durationMs else 0L) else ytDur
    fun toggle() {
        if (useExo) { if (isCur) com.noorpro.app.audio.AlNoorAudioSession.player.togglePlayPause() else if (track != null) playFree(ctx, track, queue.ifEmpty { listOf(track) }) }
        else js(if (ytPlaying) "player.pauseVideo();" else "player.playVideo();")
    }
    fun seek(ms: Long) { if (useExo) { if (isCur) com.noorpro.app.audio.AlNoorAudioSession.player.seekTo(ms) } else js("player.seekTo(${ms / 1000.0},true);") }
    fun share() = runCatching {
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "${cur.title} — MAF Nasheed\nhttps://www.youtube.com/watch?v=${cur.id}"), "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    // One persistent official player; movableContentOf keeps the same WebView when it moves between layouts.
    val ytPlayer = remember {
        movableContentOf { m: Modifier ->
            if (previewAudio != null) Box(m.background(Color(0xFF111111)), contentAlignment = Alignment.Center) {
                AsyncImage(mafPreviewArt?.invoke(cur.id) ?: cur.thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                YouTubeMark(Modifier.align(Alignment.BottomEnd).padding(6.dp))
            } else AndroidView(factory = { c ->
                WebView(c).apply {
                    settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(YtBridge({ ytStateNow(it) }, { p, d -> ytPos = p; ytDur = d }), "NP")
                    // A real https origin is required by the embed player (otherwise error 152/153).
                    loadDataWithBaseURL("https://noor-pro-d87e3.web.app/", ytHtml(cur.id, pb.positionMs.takeIf { isCur }?.div(1000) ?: 0), "text/html", "utf-8", null)
                    web = this
                }
            }, onRelease = { web = null; ytPlaying = false; it.loadUrl("about:blank"); it.destroy() }, modifier = m)
        }
    }
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            // Top bar
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.KeyboardArrowDown, "Close", tint = Color.White, modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose).padding(6.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM ARTIST", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("MAF Nasheed", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box {
                    Icon(Icons.Default.MoreVert, "More", tint = Color.White, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { menu = true }.padding(8.dp))
                    androidx.compose.material3.DropdownMenu(menu, { menu = false }) {
                        androidx.compose.material3.DropdownMenuItem({ Text("Open in YouTube") }, { menu = false; openYouTube(ctx, "https://www.youtube.com/watch?v=${cur.id}") })
                        androidx.compose.material3.DropdownMenuItem({ Text("View channel") }, { menu = false; openYouTube(ctx, MAF_CHANNEL_URL) })
                        androidx.compose.material3.DropdownMenuItem({ Text("Share") }, { menu = false; share() })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AudioVideoPill(t, audio) { audio = it } }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedContent(audio, transitionSpec = {
                    (androidx.compose.animation.fadeIn(tween(280)) + androidx.compose.animation.scaleIn(tween(280), initialScale = 0.94f)) togetherWith androidx.compose.animation.fadeOut(tween(160))
                }, label = "av") { a ->
                    if (a) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        val big = if (owner) 1f else 0.78f
                        Box(Modifier.fillMaxWidth(big).aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1A1A1A))) {
                            val pa = mafPreviewArt?.invoke(cur.id)?.removePrefix("file://")?.let { android.graphics.BitmapFactory.decodeFile(it) }
                            if (pa != null) androidx.compose.foundation.Image(pa.asImageBitmap(), cur.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else AsyncImage("https://i.ytimg.com/vi/${cur.id}/maxresdefault.jpg", cur.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                                error = coil.compose.rememberAsyncImagePainter(cur.thumb))
                        }
                        if (!owner) {
                            Spacer(Modifier.height(12.dp))
                            // Visible, minimised official YouTube player (still the one playing).
                            ytPlayer(Modifier.width(176.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)))
                        }
                    } else Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MafGold, Color(0xFFE0457B)))), contentAlignment = Alignment.Center) {
                                Text("MAF", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cur.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(MAF_CHANNEL_NAME, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        ytPlayer(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)))
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(36.dp).clip(CircleShape).clickable { share() }.padding(8.dp))
                            Spacer(Modifier.weight(1f))
                            YouTubeMark(Modifier.clip(RoundedCornerShape(4.dp)).clickable { openYouTube(ctx, "https://www.youtube.com/watch?v=${cur.id}") })
                        }
                    }
                }
            }
            // Source label
            Row(Modifier.padding(top = 14.dp).clip(CircleShape).clickable { info = true }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (useExo) "Streaming from NoorPro · owner audio" else "Streaming from YouTube", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Info, "Info", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    androidx.compose.animation.AnimatedContent(cur.title, label = "title") { tt ->
                        Text(tt, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 26.sp)
                    }
                    Text("MAF Nasheed", color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
                }
                val hs by animateFloatAsState(if (liked) 1.15f else 1f, spring(dampingRatio = 0.4f), label = "heart")
                Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like", tint = if (liked) MafGold else Color.White,
                    modifier = Modifier.size(40.dp).graphicsLayer { scaleX = hs; scaleY = hs }.clip(CircleShape)
                        .clickable { liked = cur.id in SpStore.toggle(ctx, "maf_liked", cur.id) }.padding(8.dp))
            }
            // Gold seek bar
            Slider(value = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f, onValueChange = { f -> if (dur > 0) seek((dur * f).toLong()) },
                colors = SliderDefaults.colors(thumbColor = MafGold, activeTrackColor = MafGold, inactiveTrackColor = Color.White.copy(alpha = 0.2f)))
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(pos), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp); Spacer(Modifier.weight(1f))
                Text(if (dur > 0) fmt(dur) else "--:--", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { share() }) { Icon(Icons.Default.Share, "Share", tint = Color.White) }
                IconButton(onClick = { go(-1) }) { Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(36.dp)) }
                val ps by animateFloatAsState(if (playing) 1f else 0.94f, spring(dampingRatio = 0.5f), label = "pp")
                Box(Modifier.size(72.dp).graphicsLayer { scaleX = ps; scaleY = ps }.clip(CircleShape).background(Color.White).clickable { toggle() }, contentAlignment = Alignment.Center) {
                    androidx.compose.animation.Crossfade(playing, label = "ppi") { p ->
                        Icon(if (p) Icons.Default.Pause else Icons.Default.PlayArrow, if (p) "Pause" else "Play", tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                }
                IconButton(onClick = { go(1) }) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp)) }
                IconButton(onClick = { repeat = !repeat; if (useExo && live) com.noorpro.app.audio.AlNoorAudioSession.player.cycleRepeatMode() }) {
                    Icon(if (repeat) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = if (repeat) MafGold else Color.White)
                }
            }
        }
        if (info) androidx.compose.material3.AlertDialog(onDismissRequest = { info = false }, confirmButton = { androidx.compose.material3.TextButton({ info = false }) { Text("OK") } },
            title = { Text(if (useExo) "Owner audio" else "Streaming from YouTube") },
            text = { Text(if (useExo) "This track is the audio file provided by MAF Nasheed and hosted by NoorPro. Switch to Video to watch it on YouTube."
                else "This video plays in YouTube's official player, which stays visible while it plays. NoorPro never downloads or extracts YouTube audio.") })
    }
    if (previewAudio != null) body()
    else Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) { body() }
}

@Composable
private fun YouTubeMark(m: Modifier = Modifier) {
    Row(m.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 20.dp, height = 14.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF0000)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
        Spacer(Modifier.width(3.dp))
        Text("YouTube", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
    }
}
