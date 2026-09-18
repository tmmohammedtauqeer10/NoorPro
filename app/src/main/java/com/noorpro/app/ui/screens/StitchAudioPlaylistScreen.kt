package com.noorpro.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.noorpro.app.ui.components.StitchEmerald
import com.noorpro.app.ui.components.StitchEmeraldDeep
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.StitchLine
import com.noorpro.app.ui.components.StitchScreen
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchSoftSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.AudioQueueItem
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

/** One entry in a curated audio playlist — a full surah, or a single ayah (e.g. Ayatul Kursi). */
private data class PlaylistTrack(
    val title: String,
    val subtitle: String,
    val surahId: Int,
    val ayah: Int? = null,
    val arabic: String = ""
)

private data class AudioPlaylistDef(
    val key: String,
    val title: String,
    val description: String,
    val cover: Int,
    val tracks: List<PlaylistTrack>
)

/** Curated, authentic playlists. Morning Adhkar includes Ayatul Kursi and the three Quls. */
private fun audioPlaylistFor(key: String): AudioPlaylistDef = when (key) {
    "sleep" -> AudioPlaylistDef(
        "sleep", "Sleep Supplications",
        "Recite before sleep for a peaceful, protected night.",
        R.drawable.audio_lib_playlist_sleep,
        listOf(
            PlaylistTrack("Ayatul Kursi", "Al-Baqarah · 2:255", 2, 255, "آيَةُ الْكُرْسِيّ"),
            PlaylistTrack("Surah Al-Mulk", "The Sovereignty · 67", 67),
            PlaylistTrack("Surah As-Sajdah", "The Prostration · 32", 32),
            PlaylistTrack("Surah Al-Ikhlas", "Sincerity · 112", 112),
            PlaylistTrack("Surah Al-Falaq", "The Daybreak · 113", 113),
            PlaylistTrack("Surah An-Nas", "Mankind · 114", 114)
        )
    )
    "jumuah" -> AudioPlaylistDef(
        "jumuah", "Jumu'ah Favorites",
        "Beloved recitations for the blessed day of Friday.",
        R.drawable.audio_lib_playlist_jumuah,
        listOf(
            PlaylistTrack("Surah Al-Kahf", "The Cave · 18", 18),
            PlaylistTrack("Surah Al-Fatihah", "The Opening · 1", 1),
            PlaylistTrack("Surah Ya-Sin", "Ya Sin · 36", 36),
            PlaylistTrack("Surah Ar-Rahman", "The Most Merciful · 55", 55)
        )
    )
    else -> AudioPlaylistDef(
        "morning", "Morning Adhkar",
        "Begin your day in the remembrance of Allah.",
        R.drawable.audio_lib_playlist_adhkar,
        listOf(
            PlaylistTrack("Surah Al-Fatihah", "The Opening · 1", 1),
            PlaylistTrack("Ayatul Kursi", "Al-Baqarah · 2:255", 2, 255, "آيَةُ الْكُرْسِيّ"),
            PlaylistTrack("Surah Al-Ikhlas", "Sincerity · 112 · ×3", 112),
            PlaylistTrack("Surah Al-Falaq", "The Daybreak · 113 · ×3", 113),
            PlaylistTrack("Surah An-Nas", "Mankind · 114 · ×3", 114)
        )
    )
}

/**
 * Spotify-style playlist detail screen: a cover header with a "Play all" button and a numbered
 * track list. Tapping a track (or Play all) starts real recitation audio and opens the player.
 */
@Composable
fun StitchAudioPlaylistScreen(viewModel: DeenViewModel) {
    val playlist = remember(viewModel.audioPlaylistKey) { audioPlaylistFor(viewModel.audioPlaylistKey) }

    // Start the whole playlist as a queue from [index] — it then autoplays track-by-track. Tapping
    // a track starts from there; "Play all" starts from the top.
    fun playFrom(index: Int) {
        val queue = playlist.tracks.map { AudioQueueItem(it.surahId, it.ayah, it.title, it.arabic) }
        viewModel.playQueue(queue, index)
        viewModel.navigateTo(DeenScreen.NOW_PLAYING)
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 210.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                PlaylistHeader(
                    playlist = playlist,
                    onBack = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.AUDIO_LIBRARY) },
                    onPlayAll = { playFrom(0) }
                )
            }
            itemsIndexed(playlist.tracks) { index, track ->
                PlaylistTrackRow(number = index + 1, track = track, onClick = { playFrom(index) })
            }
        }
    }
}

@Composable
private fun PlaylistHeader(playlist: AudioPlaylistDef, onBack: () -> Unit, onPlayAll: () -> Unit) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Image(
                painter = painterResource(playlist.cover),
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.25f),
                            0.6f to StitchEmeraldDeep.copy(alpha = 0.45f),
                            1f to StitchEmeraldDeep.copy(alpha = 0.96f)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 14.dp, top = 8.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
            ) {
                Text("PLAYLIST", color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(playlist.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
            }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(playlist.description, color = stitchMutedText(), fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${playlist.tracks.size} tracks", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(stitchPrimary())
                        .clickable(onClick = onPlayAll)
                        .padding(horizontal = 22.dp, vertical = 11.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play all", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(number: Int, track: PlaylistTrack, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            number.toString(),
            color = stitchMutedText(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(26.dp)
        )
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(stitchSoftSurface()),
            contentAlignment = Alignment.Center
        ) {
            if (track.arabic.isNotBlank()) {
                Text(track.arabic.take(3), color = stitchPrimary(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            } else {
                Text(track.surahId.toString(), color = stitchPrimary(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(track.subtitle, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Play ${track.title}",
            tint = stitchPrimary(),
            modifier = Modifier.size(24.dp)
        )
    }
}
