package com.noorpro.app.audio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noorpro.app.audio.models.Track

/**
 * Compose stubs for Al Noor Audio. Wire into app navigation when ready.
 * Full UX: docs/AL_NOOR_AUDIO.md
 */

@Composable
fun AlNoorAudioHomeScreen(
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onPlayTrack: (Track) -> Unit = {},
) {
    PlaceholderScreen(
        title = "Al Noor Audio",
        subtitle = "Browse copyright-free nasheed & naat",
        modifier = modifier,
    )
    // onOpenSearch / onOpenPlaylist / onPlayTrack reserved for navigation wiring
}

@Composable
fun AlNoorSearchScreen(
    modifier: Modifier = Modifier,
    onPlayTrack: (Track) -> Unit = {},
) {
    PlaceholderScreen(
        title = "Search",
        subtitle = "Find nasheed & naat",
        modifier = modifier,
    )
}

@Composable
fun AlNoorPlaylistScreen(
    playlistId: String,
    modifier: Modifier = Modifier,
    onPlayTrack: (Track) -> Unit = {},
) {
    PlaceholderScreen(
        title = "Playlist",
        subtitle = "id=$playlistId",
        modifier = modifier,
    )
}

@Composable
fun AlNoorNowPlayingScreen(
    modifier: Modifier = Modifier,
    track: Track? = null,
) {
    PlaceholderScreen(
        title = track?.title ?: "Now Playing",
        subtitle = track?.attributionText ?: "Attribution appears here",
        modifier = modifier,
    )
}

@Composable
fun AlNoorMiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onPlayPause: () -> Unit = {},
) {
    if (track == null) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = buildString {
                append(track.title)
                append(if (isPlaying) " · playing" else " · paused")
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        // onExpand / onPlayPause reserved for scaffold wiring
    }
}

@Composable
fun AlNoorQueueSheet(
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = "Queue",
        subtitle = "Up next",
        modifier = modifier,
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
