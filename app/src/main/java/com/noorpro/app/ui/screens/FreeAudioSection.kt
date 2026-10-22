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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.catalog.FreeAudioCatalog
import com.noorpro.app.audio.models.Track
import com.noorpro.app.audio.session.AlNoorSessionRouter
import com.noorpro.app.data.QuranMetaData
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchSoftSurface
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon

/** Test hook: lets Robolectric previews render without starting ExoPlayer. */
internal object FreeAudioPreview { var disablePlayer = false }

private fun playFree(ctx: android.content.Context, track: Track, queue: List<Track>) {
    if (FreeAudioPreview.disablePlayer) return
    runCatching {
        AlNoorAudioSession.init(ctx)
        AlNoorSessionRouter.select(ctx, AlNoorSessionRouter.Source.AL_NOOR)
        AlNoorAudioSession.player.playTrack(track, queue)
    }
}

@Composable
internal fun FreeAudioSection(category: String) {
    val ctx = LocalContext.current
    var showSources by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (category) { "Dua Audio" -> "Dua Audio"; "Tarjuma" -> "Quran Tarjuma"; else -> "Tafsir Audio" },
                color = stitchPrimary(), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier.clip(CircleShape).clickable { showSources = true }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Info, null, tint = StitchGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Audio sources", color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        FreeNowPlayingBar()
        when (category) {
            "Dua Audio" -> DuaList(ctx)
            "Tarjuma" -> TarjumaList(ctx)
            else -> TafsirList(ctx)
        }
        Text(
            "Streaming from free, licensed sources. Tap “Audio sources” for attribution.",
            color = stitchMutedText(), fontSize = 11.sp,
        )
    }
    if (showSources) AudioSourcesSheet { showSources = false }
}

@Composable
private fun FreeNowPlayingBar() {
    if (FreeAudioPreview.disablePlayer || !AlNoorAudioSession.isInitialized()) return
    val q by AlNoorAudioSession.player.queue.collectAsState()
    val pb by AlNoorAudioSession.player.playback.collectAsState()
    val t = q.currentTrack ?: return
    if (!t.id.startsWith("free-")) return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(stitchPrimary().copy(alpha = 0.12f))
            .border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("NOW PLAYING", color = StitchGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(t.title, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.artistName, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PlayCircle(pb.isPlaying) { AlNoorAudioSession.player.togglePlayPause() }
    }
}

@Composable
private fun PlayCircle(playing: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(stitchPrimary()).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play",
            tint = stitchSurface(), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun FreeChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val sel = o == selected
            Box(
                Modifier.clip(CircleShape)
                    .background(if (sel) StitchGold.copy(alpha = 0.22f) else stitchSoftSurface())
                    .border(1.dp, if (sel) StitchGold else stitchLine(), CircleShape)
                    .clickable { onSelect(o) }.padding(horizontal = 14.dp, vertical = 7.dp),
            ) { Text(o, color = if (sel) stitchText() else stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun GroupCard(
    title: String, subtitle: String, expanded: Boolean, onToggle: () -> Unit, onPlayAll: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface())
            .border(1.dp, stitchLine(), RoundedCornerShape(16.dp)),
    ) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = stitchMutedText())
            Spacer(Modifier.width(8.dp))
            PlayCircle(onClick = onPlayAll)
        }
        if (expanded) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) { content() }
    }
}

@Composable
private fun TrackRow(index: Int, t: Track, onPlay: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onPlay).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("$index", color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, color = stitchText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.durationMs > 0) Text(fmt(t.durationMs), color = stitchMutedText(), fontSize = 11.sp)
        }
        Icon(Icons.Default.PlayArrow, "Play", tint = stitchPrimary(), modifier = Modifier.size(20.dp))
    }
}

private fun fmt(ms: Long): String { val s = ms / 1000; return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60) }

@Composable
private fun DuaList(ctx: android.content.Context) {
    val groups = remember { FreeAudioCatalog.duaGroups(ctx) }
    var open by rememberSaveable { mutableStateOf(groups.firstOrNull()?.title ?: "") }
    groups.forEach { g ->
        GroupCard(g.title, g.subtitle, open == g.title, { open = if (open == g.title) "" else g.title },
            { g.tracks.firstOrNull()?.let { playFree(ctx, it, g.tracks) } }) {
            g.tracks.forEachIndexed { i, t -> TrackRow(i + 1, t) { playFree(ctx, t, g.tracks) } }
        }
    }
}

@Composable
private fun TarjumaList(ctx: android.content.Context) {
    var lang by rememberSaveable { mutableStateOf(FreeAudioCatalog.tarjumaLanguages.first()) }
    val eds = FreeAudioCatalog.editions.filter { it.language == lang }
    var edKey by rememberSaveable(lang) { mutableStateOf(eds.first().key) }
    val ed = eds.firstOrNull { it.key == edKey } ?: eds.first()
    var showAll by rememberSaveable(edKey) { mutableStateOf(false) }
    FreeChips(FreeAudioCatalog.tarjumaLanguages, lang) { lang = it }
    if (eds.size > 1) FreeChips(eds.map { it.title }, ed.title) { t -> edKey = eds.first { it.title == t }.key }
    Text(ed.subtitle + " · 114 surahs", color = stitchMutedText(), fontSize = 12.sp)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface())
            .border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        val n = if (showAll) 114 else 10
        (1..n).forEach { s ->
            Row(Modifier.fillMaxWidth().clickable {
                val tr = ed.surahTracks(s); playFree(ctx, tr.first(), tr)
            }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$s", color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                Column(Modifier.weight(1f)) {
                    Text(QuranMetaData.surahNamesEn.getOrElse(s - 1) { "Surah $s" }, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (ed.verseByVerse) "${FreeAudioCatalog.AYAH_COUNTS[s - 1]} ayahs · verse by verse" else "Full surah",
                        color = stitchMutedText(), fontSize = 11.sp)
                }
                Text(QuranMetaData.surahNamesAr.getOrElse(s - 1) { "" }, color = stitchPrimary(), fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.PlayArrow, "Play", tint = stitchPrimary(), modifier = Modifier.size(20.dp))
            }
        }
        Text(if (showAll) "Show fewer" else "Show all 114 surahs", color = StitchGold, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().clickable { showAll = !showAll }.padding(vertical = 10.dp))
    }
    Text(FreeAudioCatalog.UNAVAILABLE_LANGUAGES_NOTE, color = stitchMutedText(), fontSize = 11.sp)
}

@Composable
private fun TafsirList(ctx: android.content.Context) {
    val scholars = remember { FreeAudioCatalog.scholars(ctx) }
    if (scholars.isEmpty()) return
    var who by rememberSaveable { mutableStateOf(scholars.first().name) }
    val sc = scholars.firstOrNull { it.name == who } ?: scholars.first()
    var open by rememberSaveable(who) { mutableStateOf(sc.surahs.firstOrNull()?.title ?: "") }
    FreeChips(scholars.map { it.name }, sc.name) { who = it }
    Text("${sc.language} · ${sc.surahs.sumOf { it.tracks.size }} parts across ${sc.surahs.size} surah(s)", color = stitchMutedText(), fontSize = 12.sp)
    sc.surahs.forEach { g ->
        GroupCard(g.title, g.subtitle, open == g.title, { open = if (open == g.title) "" else g.title },
            { g.tracks.firstOrNull()?.let { playFree(ctx, it, g.tracks) } }) {
            g.tracks.forEachIndexed { i, t -> TrackRow(i + 1, t) { playFree(ctx, t, g.tracks) } }
        }
    }
}

@Composable
private fun AudioSourcesSheet(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { AudioSourcesContent(onDismiss) }
}

@Composable
internal fun AudioSourcesContent(onDismiss: () -> Unit) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(stitchSurface())
                .border(1.dp, stitchLine(), RoundedCornerShape(20.dp)).padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Audio sources", color = stitchPrimary(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
            FreeAudioCatalog.sources.forEach { s ->
                Column {
                    Text(s.name, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(s.by, color = stitchMutedText(), fontSize = 12.sp)
                    Text("License: ${s.licenseLabel}", color = stitchText(), fontSize = 12.sp)
                    Text(s.licenseUrl, color = StitchGold, fontSize = 11.sp)
                    Text(s.sourceUrl, color = stitchMutedText(), fontSize = 11.sp)
                }
            }
            Text("All audio is streamed from the original source; Noor Pro does not host or sell it. Copyright of recitations remains with the reciters.",
                color = stitchMutedText(), fontSize = 11.sp)
            Text("Close", color = stitchPrimary(), fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End).clip(CircleShape).clickable(onClick = onDismiss).padding(8.dp))
        }
    }
