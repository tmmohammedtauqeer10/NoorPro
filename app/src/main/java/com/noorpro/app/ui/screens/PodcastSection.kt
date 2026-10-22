package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.noorpro.app.audio.podcast.FeedResult
import com.noorpro.app.audio.podcast.Podcast
import com.noorpro.app.audio.podcast.PodcastRepository
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchSoftSurface
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import java.text.DateFormat
import java.util.Date

/** Al-Noor Audio "Podcasts" tab: curated public RSS feeds by language + category. */
@Composable
internal fun PodcastSection(initialPodcastId: String? = null) {
    var lang by rememberSaveable { mutableStateOf(PodcastRepository.languages.first()) }
    var cat by rememberSaveable(lang) { mutableStateOf("All") }
    var openId by rememberSaveable { mutableStateOf(initialPodcastId) }
    val open = PodcastRepository.podcasts.firstOrNull { it.id == openId }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Islamic Podcasts", color = stitchPrimary(), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
        FreeNowPlayingBar()
        if (open != null) {
            PodcastEpisodes(open) { openId = null }
        } else {
            FreeChips(PodcastRepository.languages, lang) { lang = it }
            FreeChips(PodcastRepository.categories(lang), cat) { cat = it }
            val list = PodcastRepository.podcasts.filter { it.language == lang && (cat == "All" || it.category == cat) }
            list.forEach { p -> PodcastRow(p) { openId = p.id } }
            Text("Episodes load live from each publisher's public RSS feed and stream from their servers.",
                color = stitchMutedText(), fontSize = 11.sp)
        }
    }
}

@Composable
private fun Artwork(url: String?, size: Int) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(stitchSoftSurface()).border(1.dp, stitchLine(), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Podcasts, null, tint = stitchPrimary().copy(alpha = 0.6f), modifier = Modifier.size((size / 2).dp))
        if (!url.isNullOrBlank()) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp))
    }
}

@Composable
private fun PodcastRow(p: Podcast, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val art = remember(p.id) { PodcastRepository.cachedArtwork(ctx, p) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface())
            .border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(art, 56)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${p.publisher} · ${p.category}", color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("via RSS", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Default.PlayArrow, "Open", tint = stitchPrimary())
    }
}

@Composable
private fun PodcastEpisodes(p: Podcast, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var reload by remember { mutableIntStateOf(0) }
    var result by remember(p.id) { mutableStateOf<FeedResult?>(null) }
    var loading by remember(p.id) { mutableStateOf(true) }
    LaunchedEffect(p.id, reload) {
        loading = true
        result = PodcastRepository.load(ctx, p, force = reload > 0)
        loading = false
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ArrowBack, "Back", tint = stitchText(), modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(6.dp))
        Spacer(Modifier.width(6.dp))
        Text("All podcasts", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.weight(1f).clickable(onClick = onBack))
        Icon(Icons.Default.Refresh, "Refresh", tint = stitchMutedText(), modifier = Modifier.clip(CircleShape).clickable { reload++ }.padding(6.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Artwork(result?.artwork, 88)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, color = stitchText(), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
            Text(p.publisher, color = stitchMutedText(), fontSize = 13.sp)
            Text("${p.language} · ${p.category} · via RSS", color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (p.website.isNotBlank()) Text(p.website.removePrefix("https://"), color = stitchMutedText(), fontSize = 12.sp)
        }
    }
    val r = result
    when {
        loading && r == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = stitchPrimary(), strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
        r == null || r.episodes.isEmpty() -> StateCard(
            "Couldn't load episodes",
            "Check your internet connection and try again. Episodes stream from the publisher's feed.",
        ) { reload++ }
        else -> {
            if (r.fromCache && r.error != null) Text("Offline — showing saved episode list", color = StitchGold, fontSize = 12.sp)
            val tracks = remember(r) { r.episodes.map { PodcastRepository.toTrack(p, it, r.artwork) } }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface())
                    .border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                r.episodes.forEachIndexed { i, e ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { playFree(ctx, tracks[i], tracks) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Artwork(e.imageUrl ?: r.artwork, 44)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val meta = listOfNotNull(
                                e.pubDateMs.takeIf { it > 0 }?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) },
                                e.durationMs.takeIf { it > 0 }?.let { fmt(it) },
                            ).joinToString(" · ")
                            if (meta.isNotEmpty()) Text(meta, color = stitchMutedText(), fontSize = 11.sp)
                        }
                        PlayCircle { playFree(ctx, tracks[i], tracks) }
                    }
                }
            }
            Text("Source: ${p.name} (${p.publisher}) via public RSS — ${p.website.ifBlank { p.feedUrl }}", color = stitchMutedText(), fontSize = 11.sp)
        }
    }
}

@Composable
private fun StateCard(title: String, body: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface())
            .border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(body, color = stitchMutedText(), fontSize = 12.sp)
        Text("Retry", color = stitchPrimary(), fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onRetry).padding(vertical = 6.dp))
    }
}
