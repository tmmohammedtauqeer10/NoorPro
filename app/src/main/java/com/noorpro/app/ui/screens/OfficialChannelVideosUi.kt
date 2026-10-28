package com.noorpro.app.ui.screens

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.noorpro.app.audio.catalog.OfficialChannel
import com.noorpro.app.audio.catalog.OfficialChannelEmbed
import com.noorpro.app.audio.catalog.OfficialChannelRepository
import com.noorpro.app.audio.catalog.OfficialChannels
import com.noorpro.app.audio.catalog.OfficialVideo
import kotlinx.coroutines.launch

/*
 * 1.1.6: "Official channel" videos screen (behind official_channel_embed_enabled, default OFF).
 * Latest uploads from the channel's public RSS feed; playback ONLY in YouTube's official embedded player
 * (YouTube controls + branding, nothing drawn over it, paused when the app leaves the foreground).
 * Any player error -> opens the video in YouTube. Subscribe / Open on YouTube always visible.
 */
private val OcYtRed = Color(0xFFFF0033)

/** Screenshot tests: local thumbnails instead of network images. */
internal var officialPreviewArt: ((String) -> ImageBitmap?)? = null

internal sealed interface OfficialFeedState {
    data object Loading : OfficialFeedState
    data class Ready(val videos: List<OfficialVideo>) : OfficialFeedState
    data object Failed : OfficialFeedState
}

@Composable
internal fun OfficialVideoThumb(v: OfficialVideo, modifier: Modifier) {
    val pa = remember(v.id) { officialPreviewArt?.invoke(v.id) }
    Box(modifier.background(Color(0xFF1A1A1A))) {
        if (pa != null) Image(pa, v.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else AsyncImage(v.thumb, v.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun OcSubscribeButton(ch: OfficialChannel, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Row(modifier.height(40.dp).clip(CircleShape).background(OcYtRed).clickable { OfficialChannels.open(ctx, OfficialChannelEmbed.subscribeUrl(ch)) }
        .padding(horizontal = 16.dp).testTag("oc_subscribe"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        YouTubeGlyph(13.dp, tint = Color.White, play = OcYtRed); Spacer(Modifier.width(7.dp))
        Text("Subscribe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun OcOutlineButton(t: SpTheme, label: String, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.height(40.dp).clip(CircleShape).border(1.dp, t.muted.copy(alpha = 0.45f), CircleShape).clickable(onClick = onClick)
        .padding(horizontal = 14.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = t.text, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp))
        Text(label, color = t.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

/**
 * The official player. [preview] = screenshot tests (static thumbnail + YouTube-style play mark instead of
 * the WebView). [onError] receives IFrame API error codes.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun OfficialEmbedPlayer(v: OfficialVideo, preview: Boolean, onError: (Int) -> Unit, onFullscreen: (View?) -> Unit, modifier: Modifier = Modifier) {
    var web by remember { mutableStateOf<WebView?>(null) }
    val err by rememberUpdatedState(onError)
    val fs by rememberUpdatedState(onFullscreen)
    // ToS: never play while not visible — pause when the app goes to the background.
    val lo = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lo) {
        val o = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) web?.evaluateJavascript("try{player.pauseVideo();}catch(e){}", null)
        }
        lo.lifecycle.addObserver(o); onDispose { lo.lifecycle.removeObserver(o) }
    }
    Box(modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black).testTag("oc_player")) {
        if (preview) {
            OfficialVideoThumb(v, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.Center)) { YouTubeGlyph(34.dp) }
        } else key(v.id) {
            AndroidView(factory = { c ->
                WebView(c).apply {
                    settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) { fs(view) }
                        override fun onHideCustomView() { fs(null) }
                    }
                    addJavascriptInterface(OcBridge { err(it) }, "NPOC")
                    // A real https origin is required by the embed player (otherwise error 152/153).
                    loadDataWithBaseURL("https://noor-pro-d87e3.web.app/", OfficialChannelEmbed.playerHtml(v.id), "text/html", "utf-8", null)
                    web = this
                }
            }, onRelease = { web = null; it.loadUrl("about:blank"); it.destroy() }, modifier = Modifier.fillMaxSize())
        }
    }
}

internal class OcBridge(val onError: (Int) -> Unit) {
    private val h = android.os.Handler(android.os.Looper.getMainLooper())
    @android.webkit.JavascriptInterface fun state(@Suppress("UNUSED_PARAMETER") s: Int) {}
    @android.webkit.JavascriptInterface fun error(code: Int) { h.post { onError(code) } }
}

@Composable
private fun OcVideoRow(t: SpTheme, v: OfficialVideo, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) noorLogoGreen.copy(alpha = if (t.light) 0.08f else 0.22f) else Color.Transparent)
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp).testTag("oc_video_${v.id}"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(132.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            OfficialVideoThumb(v, Modifier.fillMaxSize())
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(v.title, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            if (v.meta.isNotBlank()) Text(v.meta, color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            if (selected) Text("Now playing", color = if (t.light) noorLogoGreen else Color(0xFF6FD3A8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/**
 * Full screen content. [state] / [playingId] / [errorCode] are hoisted for screenshot tests; the live
 * dialog ([OfficialChannelVideosDialog]) drives them from the RSS feed and the player.
 */
@Composable
internal fun OfficialChannelVideosScreen(
    t: SpTheme, ch: OfficialChannel, state: OfficialFeedState, playingId: String?, errorCode: Int?, preview: Boolean = false,
    onBack: () -> Unit = {}, onPlay: (OfficialVideo) -> Unit = {}, onRetry: () -> Unit = {}, onError: (Int) -> Unit = {}, onFullscreen: (View?) -> Unit = {},
) {
    val ctx = LocalContext.current
    val videos = (state as? OfficialFeedState.Ready)?.videos.orEmpty()
    val playing = videos.firstOrNull { it.id == playingId }
    Column(Modifier.fillMaxSize().background(t.bg).statusBarsPadding().navigationBarsPadding().testTag("oc_screen")) {
        // Top bar: back · NoorPro mark · title
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.text, modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack).padding(10.dp))
            NoorBrandMark(26.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("OFFICIAL CHANNEL", color = t.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text(ch.name, color = t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (playing != null) {
            if (errorCode != null) {
                // Not embeddable (or any player error): explain + open in YouTube.
                Column(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF111111)).padding(20.dp).testTag("oc_fallback"),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    YouTubeGlyph(26.dp); Spacer(Modifier.height(10.dp))
                    Text(OfficialChannelEmbed.errorMessage(errorCode), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.height(40.dp).clip(CircleShape).background(OcYtRed).clickable { OfficialChannels.open(ctx, playing.watchUrl) }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Watch on YouTube", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            } else OfficialEmbedPlayer(playing, preview, onError, onFullscreen)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(playing.title, color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp)
                if (playing.meta.isNotBlank()) Text(playing.meta, color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        // Channel header: avatar, name, Subscribe + Open on YouTube
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(t.elevated).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelInitials(ch, 44.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(ch.name, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        YouTubeGlyph(11.dp); Spacer(Modifier.width(5.dp))
                        Text("${ch.subtitle} · ${ch.language}", color = t.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OcSubscribeButton(ch, Modifier.weight(1f))
                OcOutlineButton(t, "Open on YouTube", "oc_open_youtube", Modifier.weight(1.3f)) {
                    OfficialChannels.open(ctx, playing?.watchUrl ?: OfficialChannelEmbed.channelUrl(ch))
                }
            }
        }
        Text("Latest videos", color = t.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp))
        when (state) {
            OfficialFeedState.Loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = noorLogoGreen, modifier = Modifier.testTag("oc_loading"))
            }
            OfficialFeedState.Failed -> Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp).testTag("oc_failed"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Couldn't load the latest videos.", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("You can still watch everything on the official channel.", color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.height(40.dp).clip(CircleShape).background(noorLogoGreen).clickable(onClick = onRetry).padding(horizontal = 18.dp).testTag("oc_retry"), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, null, tint = noorLogoCream, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp))
                    Text("Try again", color = noorLogoCream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            is OfficialFeedState.Ready -> LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("oc_list"), contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 12.dp)) {
                items(state.videos, key = { "oc-" + it.id }) { v -> OcVideoRow(t, v, v.id == playingId) { onPlay(v) } }
                item(key = "oc-foot") {
                    Text("Videos from ${ch.name}'s official YouTube channel, played in YouTube's own player. Noor Pro does not host, download or change this content.",
                        color = t.muted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                }
            }
        }
    }
}

/** Live dialog: loads the RSS feed, plays in the official player, falls back to YouTube on any player error. */
@Composable
internal fun OfficialChannelVideosDialog(ch: OfficialChannel, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val t = rememberSpTheme()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<OfficialFeedState>(OfficialFeedState.Loading) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var errorCode by remember { mutableStateOf<Int?>(null) }
    var fullscreen by remember { mutableStateOf<View?>(null) }
    fun load(force: Boolean) {
        state = OfficialFeedState.Loading
        scope.launch {
            val v = runCatching { OfficialChannelRepository.load(ctx, ch, force) }.getOrDefault(emptyList())
            state = if (v.isEmpty()) OfficialFeedState.Failed else OfficialFeedState.Ready(v)
        }
    }
    LaunchedEffect(ch.id) { load(false) }
    Dialog(onDismissRequest = { if (fullscreen != null) fullscreen = null else onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize()) {
            OfficialChannelVideosScreen(t, ch, state, playingId, errorCode, onBack = onClose,
                onPlay = { playingId = it.id; errorCode = null }, onRetry = { load(true) },
                onError = { code ->
                    if (OfficialChannelEmbed.shouldFallBack(code)) {
                        errorCode = code
                        // Not embeddable here -> open the video in YouTube (the panel stays for when they come back).
                        (state as? OfficialFeedState.Ready)?.videos?.firstOrNull { it.id == playingId }?.let { OfficialChannels.open(ctx, it.watchUrl) }
                    }
                },
                onFullscreen = { fullscreen = it })
            // YouTube's own fullscreen button (WebChromeClient custom view), shown as-is.
            fullscreen?.let { v ->
                AndroidView(factory = { (v.parent as? ViewGroup)?.removeView(v); v }, modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }
    }
}
