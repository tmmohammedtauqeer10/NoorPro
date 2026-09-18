package com.noorpro.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.noorpro.app.data.IslamicData
import com.noorpro.app.data.Reciter
import com.noorpro.app.data.Surah
import com.noorpro.app.ui.components.StitchCream
import com.noorpro.app.ui.components.StitchEmerald
import com.noorpro.app.ui.components.StitchEmeraldDeep
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.StitchLine
import com.noorpro.app.ui.components.StitchScreen
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchSoftSurface
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

/** A curated audio playlist tile shown in the "Recent Playlists" grid. */
private data class AudioPlaylist(
    val title: String,
    val subtitle: String,
    val count: Int,
    val cover: Int,
    val onOpen: () -> Unit
)

/**
 * "Al-Noor Audio" — premium Audio Library hub matching the Islamic Faith Hub Stitch design.
 * Wired to real app data: the reciter list (tap to set your reciter), Featured recitation and
 * playlists start playback through the shared ViewModel, so the app's global mini-player picks
 * them up automatically (no duplicate player is drawn here). The bottom navigation + mini-player
 * are provided by the app scaffold in MainActivity.
 */
@Composable
fun StitchAudioLibraryScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val light = isStitchLight()
    val surahs by viewModel.surahs.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()

    // Featured recitation: Surah Ar-Rahman if present, else the first available surah.
    val featured = remember(surahs) {
        surahs.firstOrNull { it.id == 55 } ?: surahs.firstOrNull()
    }

    var selectedCategory by remember { mutableStateOf("Recitations") }
    val categories = listOf("Recitations", "Nasheeds", "Podcasts", "Dua Audio", "Tafsir")

    val reciters = IslamicData.reciters
    // Portrait art for the first few reciters; the rest fall back to gradient initials.
    val portraits = listOf(
        R.drawable.audio_lib_reciter_1,
        R.drawable.audio_lib_reciter_2,
        R.drawable.audio_lib_reciter_3,
        R.drawable.audio_lib_reciter_4
    )

    fun openFullPlayer() {
        featured?.let { viewModel.selectSurahForPlayback(it) }
        viewModel.navigateTo(DeenScreen.NOW_PLAYING)
    }

    val playlists = listOf(
        // Each card opens a Spotify-style playlist detail screen with a full, tappable track list.
        AudioPlaylist("Morning Adhkar", "Ayatul Kursi · 3 Quls · 5 tracks", 5, R.drawable.audio_lib_playlist_adhkar) {
            viewModel.openAudioPlaylist("morning")
        },
        AudioPlaylist("Sleep Supplications", "Al-Mulk · Ayatul Kursi · 6 tracks", 6, R.drawable.audio_lib_playlist_sleep) {
            viewModel.openAudioPlaylist("sleep")
        },
        AudioPlaylist("Jumu'ah Favorites", "Al-Kahf · Ya-Sin · 4 tracks", 4, R.drawable.audio_lib_playlist_jumuah) {
            viewModel.openAudioPlaylist("jumuah")
        }
    )

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Generous bottom inset so the app's floating nav + mini-player never cover content.
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 210.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            item {
                AudioTopBar(
                    photoUrl = viewModel.userPhotoUrl,
                    onMenu = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.DASHBOARD) },
                    onProfile = { viewModel.navigateTo(DeenScreen.PROFILE_DASHBOARD) }
                )
            }

            // Featured Today
            item {
                Column {
                    AudioSectionTitle("Featured Today")
                    Spacer(Modifier.height(14.dp))
                    AudioFeaturedCard(
                        title = featured?.nameEnglish ?: "Surah Ar-Rahman",
                        reciter = selectedReciter.name,
                        onPlay = { featured?.let { viewModel.selectSurahForPlayback(it) } },
                        onBookmark = {
                            Toast.makeText(context, "Saved to your library", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Category chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categories) { cat ->
                        AudioCategoryChip(
                            label = cat,
                            selected = cat == selectedCategory,
                            onClick = {
                                selectedCategory = cat
                                if (cat != "Recitations") {
                                    Toast.makeText(context, "$cat coming soon", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // Top Reciters
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AudioSectionTitle("Top Reciters", modifier = Modifier.weight(1f))
                        Text(
                            "SEE ALL",
                            color = StitchGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.clickable { openFullPlayer() }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        items(reciters) { reciter ->
                            val idx = reciters.indexOf(reciter)
                            AudioReciterAvatar(
                                reciter = reciter,
                                portrait = portraits.getOrNull(idx),
                                selected = reciter.id == selectedReciter.id,
                                onClick = {
                                    viewModel.selectReciter(reciter)
                                    Toast.makeText(
                                        context,
                                        "Reciter set to ${reciter.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                        item {
                            AudioExploreMoreAvatar(onClick = { openFullPlayer() })
                        }
                    }
                }
            }

            // Recent Playlists
            item {
                AudioSectionTitle("Recent Playlists")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AudioPlaylistCard(playlists[0], Modifier.weight(1f))
                    AudioPlaylistCard(playlists[1], Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AudioPlaylistCard(playlists[2], Modifier.weight(1f))
                    AudioCreatePlaylistCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            Toast.makeText(context, "Create playlist coming soon", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioTopBar(photoUrl: String, onMenu: () -> Unit, onProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Menu,
            contentDescription = "Menu",
            tint = stitchText(),
            modifier = Modifier.size(26.dp).clickable(onClick = onMenu)
        )
        Spacer(Modifier.weight(1f))
        Text(
            "Al-Noor Audio",
            color = stitchPrimary(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LibreCaslon
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(stitchSoftSurface())
                .border(1.dp, StitchLine, CircleShape)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(Icons.Default.Menu, contentDescription = null, tint = stitchMutedText(), modifier = Modifier.size(0.dp))
            }
        }
    }
}

@Composable
private fun AudioSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = stitchPrimary(),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = LibreCaslon,
        modifier = modifier
    )
}

@Composable
private fun AudioFeaturedCard(
    title: String,
    reciter: String,
    onPlay: () -> Unit,
    onBookmark: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StitchEmeraldDeep)
    ) {
        Image(
            painter = painterResource(R.drawable.audio_lib_audio_hero),
            contentDescription = "Featured recitation",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to StitchEmeraldDeep.copy(alpha = 0.45f),
                        1f to StitchEmeraldDeep.copy(alpha = 0.94f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                "BEAUTIFUL RECITATION",
                color = StitchGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LibreCaslon
            )
            Spacer(Modifier.height(4.dp))
            Text(
                reciter,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 15.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StitchGold)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = StitchEmeraldDeep,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onBookmark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BookmarkAdd,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) stitchPrimary() else stitchPrimary().copy(alpha = 0.10f)
    val fg = if (selected) Color.White else stitchPrimary()
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else stitchPrimary().copy(alpha = 0.2f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AudioReciterAvatar(
    reciter: Reciter,
    portrait: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(onClick = onClick)
    ) {
        val ringBrush = if (selected) {
            Brush.linearGradient(listOf(StitchGold, StitchEmerald))
        } else {
            Brush.linearGradient(listOf(StitchLine, StitchLine))
        }
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(ringBrush)
                .padding(2.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(stitchSoftSurface()),
                contentAlignment = Alignment.Center
            ) {
                if (portrait != null) {
                    Image(
                        painter = painterResource(portrait),
                        contentDescription = reciter.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        reciter.name.take(1),
                        color = stitchPrimary(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LibreCaslon
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            reciter.name,
            color = stitchText(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AudioExploreMoreAvatar(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(stitchSoftSurface())
                .border(1.dp, StitchLine, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = "Explore more", tint = stitchMutedText())
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Explore More",
            color = stitchMutedText(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AudioPlaylistCard(playlist: AudioPlaylist, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(stitchSurface())
            .border(1.dp, StitchEmerald.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = playlist.onOpen)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Image(
                painter = painterResource(playlist.cover),
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StitchEmeraldDeep.copy(alpha = 0.78f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text("${playlist.count}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            playlist.title,
            color = stitchText(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            playlist.subtitle,
            color = stitchMutedText(),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AudioCreatePlaylistCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(stitchSurface())
            .border(1.dp, StitchEmerald.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(stitchSoftSurface()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create new", tint = stitchMutedText(), modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Create New", color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text("Your playlist", color = stitchMutedText(), fontSize = 12.sp)
    }
}

/**
 * Home-screen entry into the Audio Library — a compact premium banner card. Placed under the
 * Sacred Tools card so users can reach the audio hub without adding a sixth navigation tab.
 */
@Composable
fun StitchAudioHubEntryCard(onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(StitchEmeraldDeep, StitchEmerald)))
            .clickable(onClick = onOpen)
            .padding(22.dp)
    ) {
        Icon(
            Icons.Default.QueueMusic,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.07f),
            modifier = Modifier.size(140.dp).align(Alignment.CenterEnd)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(StitchGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = StitchEmeraldDeep, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Al-Noor Audio", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Recitations, Nasheeds & Duas",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
