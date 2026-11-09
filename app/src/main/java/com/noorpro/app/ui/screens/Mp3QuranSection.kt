package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.catalog.Mp3QuranCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val MP3QURAN_TINT = Color(0xFF0B5E4A)

@Composable
internal fun rememberMp3QuranResolved(): List<Mp3QuranCatalog.Resolved> {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf(Mp3QuranCatalog.fallbackResolved()) }
    LaunchedEffect(Unit) {
        list = withContext(Dispatchers.IO) { Mp3QuranCatalog.loadResolved(ctx) }
    }
    return list
}

@Composable
internal fun Mp3QuranReciterArt(r: Mp3QuranCatalog.Resolved, modifier: Modifier, circle: Boolean = false, showStyle: Boolean = false) {
    val tint = coverColor(r.curated.moshafId + 17)
    val shape = if (circle) CircleShape else RoundedCornerShape(10.dp)
    val initials = r.curated.displayName.split(' ')
        .filter { it.isNotBlank() && it.first().isUpperCase() }
        .take(2).joinToString("") { it.take(1) }
        .ifBlank { r.curated.displayName.take(1) }
    Box(
        modifier.clip(shape)
            .background(Brush.linearGradient(listOf(lerp(tint, Color.White, 0.18f), tint, lerp(tint, Color.Black, 0.45f)))),
        contentAlignment = Alignment.Center,
    ) {
        SpPattern(Modifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Text(initials, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
            if (showStyle) {
                Text(r.curated.style, color = Color(0xFFE9C46A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun Mp3QuranReciterArtFixed(r: Mp3QuranCatalog.Resolved, size: Dp, circle: Boolean = false) {
    Mp3QuranReciterArt(r, Modifier.size(size), circle = circle, showStyle = !circle && size >= 96.dp)
}

internal fun LazyListScope.mp3QuranShelfItems(
    t: SpTheme,
    list: List<Mp3QuranCatalog.Resolved>,
    onOpen: (String) -> Unit,
) {
    itemsIndexed(list, key = { _, r -> "mq${r.curated.key}" }) { _, r ->
        SpCard(
            t,
            r.curated.displayName,
            r.subtitle,
            size = 130.dp,
            onClick = { onOpen("mp3quran:${r.curated.key}") },
        ) { mod -> Mp3QuranReciterArt(r, mod, showStyle = true) }
    }
}

@Composable
internal fun Mp3QuranCreditLine(color: Color, modifier: Modifier = Modifier) {
    Text(
        Mp3QuranCatalog.CREDIT + " · streamed, not hosted by Noor Pro",
        color = color, fontSize = 11.sp, lineHeight = 15.sp, modifier = modifier.padding(horizontal = 16.dp),
    )
}

@Composable
internal fun Mp3QuranListPage(
    t: SpTheme,
    list: List<Mp3QuranCatalog.Resolved>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    SpDetailPage(
        t,
        SpDetailHeader(
            "Quran Recitations",
            "Curated voices · ${Mp3QuranCatalog.CREDIT}",
            "${list.size} recitations · Hafs",
            MP3QURAN_TINT,
        ) { m -> SpArt(m, tint = MP3QURAN_TINT, label = "Qur'an\nMP3") },
        false, false, null, null, onBack, null, {}, {}, {},
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)).background(t.elevated).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.MenuBook, null, tint = MP3QURAN_TINT, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Stream from MP3Quran.net", color = t.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Tap a reciter, then any surah. Plays in the shared Al Noor player (background & car mode).",
                        color = t.muted, fontSize = 11.sp, lineHeight = 14.sp,
                    )
                }
            }
        }
        itemsIndexed(list, key = { _, r -> "mql${r.curated.key}" }) { _, r ->
            SpTrackRow(
                t, null, r.curated.displayName, r.subtitle, false,
                onClick = { onOpen("mp3quran:${r.curated.key}") },
                art = { m -> Mp3QuranReciterArt(r, m, circle = true) },
            )
        }
        item { Spacer(Modifier.height(8.dp)); Mp3QuranCreditLine(t.muted); Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
internal fun Mp3QuranReciterPage(
    t: SpTheme,
    resolved: Mp3QuranCatalog.Resolved,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val tracks = remember(resolved) { Mp3QuranCatalog.tracksFor(resolved) }
    val playingId = rememberMp3QuranPlayingId()
    val isPlayingHere = playingId != null && tracks.any { it.id == playingId }
    SpDetailPage(
        t,
        SpDetailHeader(
            resolved.curated.displayName,
            "${resolved.curated.style} · ${Mp3QuranCatalog.CREDIT}",
            resolved.subtitle,
            coverColor(resolved.curated.moshafId + 17),
        ) { m -> Mp3QuranReciterArt(resolved, m, showStyle = true) },
        playing = isPlayingHere,
        shuffle = false,
        liked = null,
        download = null,
        onBack = onBack,
        onPlay = {
            val start = tracks.indexOfFirst { it.id == playingId }.takeIf { it >= 0 } ?: 0
            playFree(ctx, tracks[start], tracks)
        },
        onShuffle = {},
        onLike = {},
        onDownload = {},
    ) {
        itemsIndexed(tracks, key = { _, tr -> tr.id }) { i, tr ->
            SpTrackRow(
                t, i + 1, tr.title, tr.artistName, playing = tr.id == playingId,
                onClick = { playFree(ctx, tr, tracks) },
            )
        }
        item { Spacer(Modifier.height(8.dp)); Mp3QuranCreditLine(t.muted); Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
internal fun rememberMp3QuranPlayingId(): String? {
    if (FreeAudioPreview.disablePlayer || !AlNoorAudioSession.isInitialized()) return null
    val q by AlNoorAudioSession.player.queue.collectAsState()
    val id = q.currentTrack?.id
    return id?.takeIf { it.startsWith("mp3q-") }
}
