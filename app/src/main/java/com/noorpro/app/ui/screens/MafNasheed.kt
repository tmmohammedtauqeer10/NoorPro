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
data class YtChannelFeed(val title: String, val avatar: String, val videos: List<YtVideo>)

object YtChannelRepository {
    private fun cacheFile(c: Context) = File(c.cacheDir, "yt_$MAF_CHANNEL_ID.json")

    internal fun parseJson(s: String): YtChannelFeed {
        val o = JSONObject(s); val a = o.optJSONArray("videos") ?: JSONArray()
        val v = (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { j ->
            val id = j.optString("id"); if (!Regex("^[\\w-]{11}$").matches(id)) null
            else YtVideo(id, j.optString("title"), j.optString("published"), j.optString("ago"), j.optString("audioUrl")) } }
        return YtChannelFeed(o.optString("title", MAF_CHANNEL_NAME).ifBlank { MAF_CHANNEL_NAME }, o.optString("avatar"), v)
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

/** Owner-uploaded audio (never YouTube audio) as an Al-Noor ExoPlayer track: background play,
 *  notification + lock-screen controls via the shared MediaSession, YouTube thumbnail as artwork. */
internal fun YtVideo.toTrack() = com.noorpro.app.audio.models.Track(
    id = "maf-$id", title = title, artistId = "maf-nasheed", artistName = "MAF Nasheed", audioUrl = audioUrl, durationMs = 0L,
    coverUrl = "https://i.ytimg.com/vi/$id/maxresdefault.jpg", license = com.noorpro.app.audio.models.LicenseType.PERMISSION_LETTER,
    attributionText = "$MAF_CHANNEL_NAME · owner-provided audio", sourceUrl = "https://www.youtube.com/watch?v=$id", tags = listOf("nasheed", "maf"),
)

/** YouTube Music-style Audio | Video pill. Audio is disabled ("Video only") when no owner audio exists. */
@Composable
internal fun AudioVideoPill(t: SpTheme, audio: Boolean, audioEnabled: Boolean, onChange: (Boolean) -> Unit) {
    val w = 92.dp
    val x by animateDpAsState(if (audio) 0.dp else w, spring(dampingRatio = 0.78f, stiffness = 520f), label = "pill")
    Box(Modifier.width(w * 2 + 8.dp).height(38.dp).clip(CircleShape).background(t.high).padding(4.dp)) {
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(CircleShape).background(t.text))
        Row(Modifier.fillMaxSize()) {
            listOf(true, false).forEach { a ->
                val sel = a == audio; val enabled = !a || audioEnabled
                val col by animateColorAsState(when { sel -> t.bg; enabled -> t.text; else -> t.muted.copy(alpha = 0.5f) }, tween(220), label = "pc")
                Box(Modifier.width(w).fillMaxHeight().clip(CircleShape).clickable(enabled = enabled && !sel) { onChange(a) }, contentAlignment = Alignment.Center) {
                    Text(if (a) (if (audioEnabled) "Audio" else "Video only") else "Video", color = col, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/** Player sheet: Video = official YouTube IFrame embed (no extraction); Audio = our ExoPlayer/MediaSession. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YtPlayerDialog(t: SpTheme, v: YtVideo, all: List<YtVideo>, previewAudio: Boolean? = null, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var audio by remember(v.id) { mutableStateOf(previewAudio ?: v.hasAudio) }
    val queue = remember(all) { all.filter { it.hasAudio }.map { it.toTrack() } }
    val track = remember(v.id) { if (v.hasAudio) v.toTrack() else null }
    val live = previewAudio == null && com.noorpro.app.audio.AlNoorAudioSession.isInitialized()
    val pb by (if (live) com.noorpro.app.audio.AlNoorAudioSession.player.playback else remember { kotlinx.coroutines.flow.MutableStateFlow(com.noorpro.app.audio.models.PlaybackState()) }).collectAsState()
    val q by (if (live) com.noorpro.app.audio.AlNoorAudioSession.player.queue else remember { kotlinx.coroutines.flow.MutableStateFlow(com.noorpro.app.audio.models.QueueState()) }).collectAsState()
    val isCur = track != null && q.currentTrack?.id == track.id
    LaunchedEffect(audio) {
        if (previewAudio != null) return@LaunchedEffect
        if (audio && track != null) { if (!isCur) playFree(ctx, track, queue.ifEmpty { listOf(track) }) }
        else if (isCur && com.noorpro.app.audio.AlNoorAudioSession.isInitialized()) runCatching { com.noorpro.app.audio.AlNoorAudioSession.player.pause() }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(14.dp)).background(t.elevated)) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("MAF Nasheed", color = t.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Close, "Close", tint = t.text, modifier = Modifier.clip(CircleShape).clickable(onClick = onClose).padding(10.dp))
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AudioVideoPill(t, audio, v.hasAudio) { audio = it } }
            Spacer(Modifier.height(10.dp))
            androidx.compose.animation.AnimatedContent(audio, transitionSpec = {
                (androidx.compose.animation.fadeIn(tween(260)) + androidx.compose.animation.scaleIn(tween(260), initialScale = 0.96f)) togetherWith androidx.compose.animation.fadeOut(tween(160))
            }, label = "av") { a ->
                if (a) Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(t.high), contentAlignment = Alignment.Center) {
                        AsyncImage(v.thumb, v.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
                        SpPlayButton(t, isCur && pb.isPlaying, 64.dp) {
                            if (isCur) com.noorpro.app.audio.AlNoorAudioSession.player.togglePlayPause() else if (track != null) playFree(ctx, track, queue.ifEmpty { listOf(track) })
                        }
                        Text("Audio · keeps playing in the background", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
                    }
                    if (live && track != null) {
                        Slider(
                            value = if (isCur && pb.durationMs > 0) (pb.positionMs.toFloat() / pb.durationMs).coerceIn(0f, 1f) else 0f,
                            onValueChange = { f -> if (isCur && pb.durationMs > 0) com.noorpro.app.audio.AlNoorAudioSession.player.seekTo((pb.durationMs * f).toLong()) },
                            colors = SliderDefaults.colors(thumbColor = t.accent, activeTrackColor = t.accent),
                        )
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isCur) fmt(pb.positionMs) else "0:00", color = t.muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            Text(if (isCur) fmt(pb.durationMs) else "0:00", color = t.muted, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { com.noorpro.app.audio.AlNoorAudioSession.player.skipPrevious() }) { Icon(Icons.Default.SkipPrevious, "Previous", tint = t.text) }
                            IconButton(onClick = { com.noorpro.app.audio.AlNoorAudioSession.player.seekTo(pb.positionMs - 10_000L) }) { Icon(Icons.Default.Replay10, "Back 10 seconds", tint = t.text) }
                            IconButton(onClick = {
                                if (isCur) com.noorpro.app.audio.AlNoorAudioSession.player.togglePlayPause() else playFree(ctx, track, queue.ifEmpty { listOf(track) })
                            }) { Icon(if (isCur && pb.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", tint = t.text, modifier = Modifier.size(34.dp)) }
                            IconButton(onClick = { com.noorpro.app.audio.AlNoorAudioSession.player.seekTo(pb.positionMs + 10_000L) }) { Icon(Icons.Default.Forward10, "Forward 10 seconds", tint = t.text) }
                            IconButton(onClick = { com.noorpro.app.audio.AlNoorAudioSession.player.skipNext() }) { Icon(Icons.Default.SkipNext, "Next", tint = t.text) }
                        }
                    }
                } else {
                    val html = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                        <style>html,body{margin:0;background:#000;height:100%}iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style></head>
                        <body><iframe src="https://www.youtube.com/embed/${v.id}?autoplay=1&playsinline=1&rel=0&modestbranding=1"
                        allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe></body></html>"""
                    if (previewAudio != null) Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black))
                    else AndroidView(factory = { c ->
                        WebView(c).apply {
                            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            webChromeClient = WebChromeClient()
                            // A real https origin is required by the embed player (otherwise error 152/153).
                            loadDataWithBaseURL("https://noor-pro-d87e3.web.app/", html, "text/html", "utf-8", null)
                        }
                    }, onRelease = { it.loadUrl("about:blank"); it.destroy() }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(v.title, color = t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(listOf(MAF_CHANNEL_NAME, v.dateLabel).filter { it.isNotBlank() }.joinToString(" · "), color = t.muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text("Watch on YouTube", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clip(CircleShape).clickable { openYouTube(ctx, "https://www.youtube.com/watch?v=${v.id}") }.padding(vertical = 6.dp))
            }
        }
    }
}
