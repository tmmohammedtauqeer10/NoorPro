package com.noorpro.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.noorpro.app.audio.catalog.BayanChannel
import com.noorpro.app.audio.catalog.CcBayan
import com.noorpro.app.audio.catalog.CcBayans
import com.noorpro.app.audio.catalog.OfficialChannels

/*
 * 1.0.95 "Bayans": CC BY bayans from the scholars' official YouTube channels.
 * Compliance: videos are played ONLY by YouTube's official embedded IFrame player, which is always visible while it
 * plays (paused when the app goes to the background). Nothing is downloaded or extracted. Every item carries the
 * channel credit + CC BY, a link to the original video and a "Subscribe on YouTube" button.
 */
private val YtRedB = Color(0xFFFF0033)
private val BayanTint = Color(0xFF0F5D46)

/** Set by search to open a bayan in the player. */
internal val bayanOpenRequest = mutableStateOf<CcBayan?>(null)

/** YouTube's official thumbnail (local file in screenshot tests only). */
@Composable
internal fun BayanThumb(b: CcBayan, modifier: Modifier) {
    val pa = remember(b.videoId) { mafPreviewArt?.invoke(b.videoId)?.removePrefix("file://")?.let { android.graphics.BitmapFactory.decodeFile(it) } }
    Box(modifier.background(Color(0xFF1A1A1A))) {
        if (pa != null) Image(pa.asImageBitmap(), b.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else AsyncImage(b.thumbUrl, b.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

/** Neutral initials avatar (no scholar photos). */
@Composable
internal fun BayanChannelAvatar(ch: BayanChannel, size: Dp) {
    val ini = if (ch.key == "jalali") "AJ" else "MC"
    Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF0F5D46), Color(0xFF2E7D6B)))), contentAlignment = Alignment.Center) {
        Text(ini, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
    }
}

@Composable
private fun CcChip(dark: Boolean = true, label: String = "CC BY") {
    Text(label, color = if (dark) Color.White else Color(0xFF0F5D46), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (dark) Color.Black.copy(alpha = 0.65f) else Color(0x220F5D46)).padding(horizontal = 5.dp, vertical = 2.dp))
}

@Composable
internal fun BayanCard(t: SpTheme, b: CcBayan, onClick: () -> Unit) {
    Column(Modifier.width(232.dp).pressScale(onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            BayanThumb(b, Modifier.fillMaxSize())
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
            }
            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { CcChip(label = b.licence) }
            Text(b.durationText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 5.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(b.title, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
        Text("${b.topic} · ${b.channel.speaker}", color = t.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SubscribePill(ch: BayanChannel, compact: Boolean = false) {
    val ctx = LocalContext.current
    Row(Modifier.clip(CircleShape).background(YtRedB).clickable { OfficialChannels.open(ctx, ch.subscribeUrl) }
        .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 7.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
        YouTubeGlyph(if (compact) 11.dp else 13.dp, tint = Color.White, play = YtRedB); Spacer(Modifier.width(6.dp))
        Text(if (compact) "Subscribe" else "Subscribe on YouTube", color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (compact) 12.sp else 13.sp)
    }
}

/** Channel header used on the shelf and browse page: avatar, name, credit, Subscribe. */
@Composable
internal fun BayanChannelHeader(t: SpTheme, ch: BayanChannel, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BayanChannelAvatar(ch, 40.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(ch.name, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            Text(ch.credit, color = t.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        SubscribePill(ch, compact = true)
    }
}

/**
 * Audio-first: if the item has owner-permitted official audio, play it in NoorPro's audio player
 * (queue = the channel's other audio-first items) and return true. Otherwise return false so the
 * caller opens YouTube's official embedded player.
 */
internal fun openBayanAudio(ctx: android.content.Context, b: CcBayan): Boolean {
    val tr = b.toTrack() ?: return false
    playFree(ctx, tr, CcBayans.byChannel(b.channelKey).mapNotNull { it.toTrack() })
    return true
}

/** Home shelf: one row of bayan cards per channel. */
@Composable
internal fun BayansShelf(t: SpTheme, onShowAll: (() -> Unit)? = null) {
    var playing by remember { mutableStateOf<CcBayan?>(null) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Official channels · Creative Commons", color = t.muted, fontSize = 12.sp)
                Text("Bayans", color = t.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            if (onShowAll != null) Text("Show all", color = t.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onShowAll).padding(6.dp))
        }
        CcBayans.channels.forEach { ch ->
            Spacer(Modifier.height(12.dp))
            BayanChannelHeader(t, ch, Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(10.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(CcBayans.byChannel(ch.key), key = { "bay-" + it.videoId }) { b -> BayanCard(t, b) { if (!openBayanAudio(ctx, b)) playing = b } }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Shared by the channels under CC BY · played in YouTube's official player", color = t.muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp))
    }
    playing?.let { b -> BayanPlayerDialog(t, b) { playing = null } }
}

/** Browse page rows ("browse:Bayans"). */
internal fun LazyListScope.bayanBrowseItems(t: SpTheme, onPlay: (CcBayan) -> Unit) {
    CcBayans.channels.forEach { ch ->
        item(key = "bh-${ch.key}") { BayanChannelHeader(t, ch, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)) }
        items(CcBayans.byChannel(ch.key), key = { "bb-" + it.videoId }) { b ->
            SpTrackRow(t, null, b.title, "${b.topic} · ${b.durationText} · ${b.channel.speaker} · ${b.licence}", false, { onPlay(b) },
                art = { m -> BayanThumb(b, m.clip(RoundedCornerShape(4.dp))) })
        }
    }
}

/**
 * Bayan player: YouTube's official embedded player, always visible at the top while it plays.
 * [preview] = screenshot tests (static thumbnail in place of the WebView).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun BayanPlayerDialog(t: SpTheme, start: CcBayan, preview: Boolean = false, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var cur by remember(start.videoId) { mutableStateOf(start) }
    val list = remember(cur.channelKey) { CcBayans.byChannel(cur.channelKey).filterNot { it.isAudioFirst } }
    var web by remember { mutableStateOf<WebView?>(null) }
    var playing by remember { mutableStateOf(preview) }
    var pos by remember { mutableStateOf(if (preview) 312_000L else 0L) }
    var dur by remember { mutableStateOf(if (preview) cur.durationSec * 1000L else 0L) }
    var info by remember { mutableStateOf(false) }
    fun js(c: String) { web?.evaluateJavascript("try{$c}catch(e){}", null) }
    fun go(d: Int) { val i = list.indexOfFirst { it.videoId == cur.videoId }; if (list.size > 1) cur = list[Math.floorMod((if (i < 0) 0 else i) + d, list.size)] }
    val onState: (Int) -> Unit = { st -> playing = st == 1 || st == 3; if (st == 0) go(1) }
    val stateNow by rememberUpdatedState(onState)
    LaunchedEffect(cur.videoId) { js("player.loadVideoById('${cur.videoId}');") }
    // ToS: never play while not visible — pause when the app goes to the background.
    val lo = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lo) {
        val o = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) js("player.pauseVideo();") }
        lo.lifecycle.addObserver(o); onDispose { lo.lifecycle.removeObserver(o) }
    }
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.KeyboardArrowDown, "Close", tint = Color.White, modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose).padding(6.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BAYANS", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text(cur.channel.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.Info, "About", tint = Color.White, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { info = true }.padding(9.dp))
            }
            // Official YouTube player — always visible.
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF111111))) {
                if (preview) { BayanThumb(cur, Modifier.fillMaxSize()) }
                else AndroidView(factory = { c ->
                    WebView(c).apply {
                        settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.mediaPlaybackRequiresUserGesture = false
                        webChromeClient = WebChromeClient()
                        addJavascriptInterface(YtBridge({ stateNow(it) }, { p, d -> pos = p; dur = d }), "NP")
                        loadDataWithBaseURL("https://noor-pro-d87e3.web.app/", ytHtml(cur.videoId, 0), "text/html", "utf-8", null)
                        web = this
                    }
                }, onRelease = { web = null; playing = false; it.loadUrl("about:blank"); it.destroy() }, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(14.dp))
                Text(cur.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 25.sp)
                Spacer(Modifier.height(6.dp))
                // Credit
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BayanChannelAvatar(cur.channel, 22.dp); Spacer(Modifier.width(8.dp))
                    Text(cur.channel.credit, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text("${cur.topic} · ${cur.language} · ${cur.durationText}", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                Slider(value = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f, onValueChange = { f -> if (dur > 0) js("player.seekTo(${dur * f / 1000.0},true);") },
                    colors = SliderDefaults.colors(thumbColor = MafGold, activeTrackColor = MafGold, inactiveTrackColor = Color.White.copy(alpha = 0.2f)))
                Row(Modifier.fillMaxWidth()) {
                    Text(fmt(pos), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp); Spacer(Modifier.weight(1f))
                    Text(if (dur > 0) fmt(dur) else cur.durationText, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(48.dp).clip(CircleShape).clickable { go(-1) }.padding(6.dp))
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White).clickable { js(if (playing) "player.pauseVideo();" else "player.playVideo();") }, contentAlignment = Alignment.Center) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play", tint = Color.Black, modifier = Modifier.size(36.dp))
                    }
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(48.dp).clip(CircleShape).clickable { go(1) }.padding(6.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SubscribePill(cur.channel)
                    Row(Modifier.clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape).clickable { OfficialChannels.open(ctx, cur.watchUrl) }
                        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp))
                        Text("Original video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Streaming from YouTube · licensed by the channel under CC BY", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Text("More from ${cur.channel.name}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) {
                items(list.filter { it.videoId != cur.videoId }, key = { "up-" + it.videoId }) { b ->
                    Row(Modifier.fillMaxWidth().clickable { cur = b }.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        BayanThumb(b, Modifier.width(96.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${b.topic} · ${b.durationText} · CC BY", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        if (info) androidx.compose.material3.AlertDialog(onDismissRequest = { info = false }, confirmButton = { androidx.compose.material3.TextButton({ info = false }) { Text("OK") } },
            title = { Text("About this video") },
            text = { Text("This bayan is shared by ${cur.channel.name} on YouTube under the Creative Commons Attribution licence (CC BY). " +
                "It plays in YouTube's official player, which stays visible while it plays. NoorPro doesn't download, host or extract the audio. " +
                "Please support the channel by subscribing on YouTube.") })
    }
    if (preview) body()
    else Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) { body() }
}
