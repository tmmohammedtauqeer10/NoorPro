package com.example.audio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.models.LicenseAttribution
import com.example.audio.models.RepeatMode
import com.example.audio.models.Track
import com.example.audio.viewmodel.AlNoorAudioViewModel

private val AlNoorAccent = Color(0xFF0E8C73)

@Composable
fun AlNoorAudioHomeScreen(
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onOpenNowPlaying: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    vm: AlNoorAudioViewModel = viewModel(),
) {
    val shelves by vm.shelves.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val notice by vm.demoNotice.collectAsState()
    val loading by vm.loading.collectAsState()
    val queue by vm.queue.collectAsState()
    val playback by vm.playback.collectAsState()

    LaunchedEffect(Unit) { vm.refreshHome() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Al Noor Audio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Copyright-free nasheed & naat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AlNoorAccent)
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            notice?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AlNoorAccent.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            if (playlists.isNotEmpty()) {
                item {
                    Text("Curated playlists", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(playlists, key = { it.id }) { pl ->
                            Card(
                                modifier = Modifier
                                    .width(140.dp)
                                    .pressScale { onOpenPlaylist(pl.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AlNoorAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Default.MusicNote, null, tint = AlNoorAccent)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(pl.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        pl.description.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            shelves.forEach { (shelfTitle, tracks) ->
                item {
                    Text(shelfTitle, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(tracks, key = { "${shelfTitle}_${it.id}" }) { track ->
                    TrackRow(
                        track = track,
                        isPlaying = queue.currentTrack?.id == track.id && playback.isPlaying,
                        onClick = {
                            vm.playTrack(track, tracks)
                            onOpenNowPlaying()
                        },
                    )
                }
            }

            if (shelves.isEmpty() && playlists.isEmpty()) {
                item {
                    Text("No catalog loaded. Check assets/al_noor_audio/catalog.json")
                }
            }
        }
    }
}

@Composable
fun AlNoorSearchScreen(
    modifier: Modifier = Modifier,
    onPlayTrack: (Track) -> Unit = {},
    onBack: (() -> Unit)? = null,
    vm: AlNoorAudioViewModel = viewModel(),
) {
    var query by remember { mutableStateOf("") }
    val results by vm.searchResults.collectAsState()
    val queue by vm.queue.collectAsState()
    val playback by vm.playback.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text("Search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                vm.search(it)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Title, artist, tagsΓÇª") },
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(results, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    isPlaying = queue.currentTrack?.id == track.id && playback.isPlaying,
                    onClick = {
                        vm.playTrack(track, results)
                        onPlayTrack(track)
                    },
                )
            }
        }
    }
}

@Composable
fun AlNoorPlaylistScreen(
    playlistId: String,
    modifier: Modifier = Modifier,
    onPlayTrack: (Track) -> Unit = {},
    onBack: (() -> Unit)? = null,
    vm: AlNoorAudioViewModel = viewModel(),
) {
    val playlist by vm.selectedPlaylist.collectAsState()
    val tracks by vm.playlistTracks.collectAsState()
    val queue by vm.queue.collectAsState()
    val playback by vm.playback.collectAsState()

    LaunchedEffect(playlistId) { vm.loadPlaylist(playlistId) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist?.title ?: "Playlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    playlist?.description.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            if (tracks.isNotEmpty()) {
                IconButton(onClick = {
                    vm.playPlaylist(tracks, 0)
                    tracks.firstOrNull()?.let(onPlayTrack)
                }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play all", tint = AlNoorAccent)
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = queue.currentTrack?.id == track.id && playback.isPlaying,
                    onClick = {
                        vm.playPlaylist(tracks, index)
                        onPlayTrack(track)
                    },
                )
            }
        }
    }
}

@Composable
fun AlNoorNowPlayingScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    vm: AlNoorAudioViewModel = viewModel(),
) {
    val queue by vm.queue.collectAsState()
    val playback by vm.playback.collectAsState()
    val track = queue.currentTrack
    val player = vm.player

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text("Now Playing", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = track,
            transitionSpec = {
                fadeIn(tween(340)) togetherWith fadeOut(tween(260))
            },
            label = "alNoorNowPlayingArt",
        ) { current ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AlNoorAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(80.dp), tint = AlNoorAccent)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    current?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    current?.artistName.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
                current?.let {
                    Text(
                        LicenseAttribution.from(it).displayLine(),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        val duration = playback.durationMs.coerceAtLeast(1L).toFloat()
        Slider(
            value = playback.positionMs.toFloat().coerceIn(0f, duration),
            onValueChange = { player.seekTo(it.toLong()) },
            valueRange = 0f..duration,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(playback.positionMs), style = MaterialTheme.typography.labelSmall)
            Text(formatMs(playback.durationMs), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { player.toggleShuffle() }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (queue.shuffle) AlNoorAccent else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = { player.skipPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AlNoorAccent)
                    .clickable { player.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = { player.skipNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { player.cycleRepeatMode() }) {
                Icon(
                    if (queue.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (queue.repeatMode != RepeatMode.OFF) AlNoorAccent else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun AlNoorMiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    visible: Boolean = track != null,
    onExpand: () -> Unit = {},
    onPlayPause: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible && track != null,
        enter = slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it / 3 },
        ) + fadeIn(tween(280)),
        exit = slideOutVertically(
            animationSpec = tween(240),
            targetOffsetY = { it / 3 },
        ) + fadeOut(tween(200)),
        modifier = modifier,
    ) {
        val current = track ?: return@AnimatedVisibility
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AlNoorAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = AlNoorAccent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                AnimatedContent(
                    targetState = Triple(current.id, current.title, current.artistName),
                    transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) },
                    label = "alNoorMiniMeta",
                    modifier = Modifier.weight(1f),
                ) { (_, title, artist) ->
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            artist,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = AlNoorAccent,
                    )
                }
            }
            if (isPlaying) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = AlNoorAccent,
                )
            }
        }
    }
}

@Composable
fun AlNoorQueueSheet(
    modifier: Modifier = Modifier,
    vm: AlNoorAudioViewModel = viewModel(),
) {
    val queue by vm.queue.collectAsState()
    LazyColumn(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        item {
            Text("Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(queue.tracks, key = { _, t -> t.id }) { index, track ->
            val current = index == queue.currentIndex
            Text(
                "${index + 1}. ${track.title}",
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                color = if (current) AlNoorAccent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.playPlaylist(queue.tracks, index) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}


@Composable
private fun Modifier.pressScale(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
}

@Composable
private fun TrackRow(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pressScale(onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isPlaying) AlNoorAccent else AlNoorAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.MusicNote,
                null,
                tint = if (isPlaying) Color.White else AlNoorAccent,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${track.artistName} ┬╖ ${track.license.name.replace('_', ' ')} ┬╖ ${track.attributionText}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
        Text(formatMs(track.durationMs), style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
