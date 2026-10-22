package com.noorpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

import com.noorpro.app.ui.components.stitchLine
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
import com.noorpro.app.R
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
/** Local-only liked surahs + recently played (SharedPreferences). */
internal object AudioLibraryStore {
    private fun p(ctx: android.content.Context) = ctx.getSharedPreferences("noor_audio_lib", android.content.Context.MODE_PRIVATE)
    fun liked(ctx: android.content.Context): Set<Int> = p(ctx).getString("liked", "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet()
    fun toggleLike(ctx: android.content.Context, id: Int): Set<Int> {
        val cur = liked(ctx).toMutableSet(); if (!cur.add(id)) cur.remove(id)
        p(ctx).edit().putString("liked", cur.joinToString(",")).apply(); return cur
    }
    fun recent(ctx: android.content.Context): List<Int> = p(ctx).getString("recent", "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }
    fun addRecent(ctx: android.content.Context, id: Int): List<Int> {
        val list = (listOf(id) + recent(ctx).filter { it != id }).take(12)
        p(ctx).edit().putString("recent", list.joinToString(",")).apply(); return list
    }
}

@Composable
fun StitchAudioLibraryScreen(viewModel: DeenViewModel, initialCategory: String = "All") {
    val context = LocalContext.current
    val surahs by viewModel.surahs.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val playingSurah by viewModel.playingSurah.collectAsState()
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val shuffle by viewModel.isShuffleEnabled.collectAsState()
    val t = rememberAudioTheme()

    var filter by remember { mutableStateOf(initialCategory) }
    var page by remember { mutableStateOf("home") }
    var openReciter by remember { mutableStateOf<Reciter?>(null) }
    var query by remember { mutableStateOf("") }
    var liked by remember { mutableStateOf(AudioLibraryStore.liked(context)) }
    var recent by remember { mutableStateOf(AudioLibraryStore.recent(context)) }

    val reciters = IslamicData.reciters
    val portraits = listOf(R.drawable.audio_lib_reciter_1, R.drawable.audio_lib_reciter_2, R.drawable.audio_lib_reciter_3, R.drawable.audio_lib_reciter_4)
    val byId = remember(surahs) { surahs.associateBy { it.id } }

    fun play(s: Surah) {
        if (playingSurah?.id == s.id) viewModel.toggleAudioPlayback() else viewModel.selectSurahForPlayback(s)
        recent = AudioLibraryStore.addRecent(context, s.id)
    }
    fun like(s: Surah) { liked = AudioLibraryStore.toggleLike(context, s.id) }

    val playlists = listOf(
        AudioPlaylistUi("Morning Adhkar", "Ayatul Kursi · 3 Quls", 5, R.drawable.audio_lib_playlist_adhkar) { viewModel.openAudioPlaylist("morning") },
        AudioPlaylistUi("Sleep Supplications", "Al-Mulk · Ayatul Kursi", 6, R.drawable.audio_lib_playlist_sleep) { viewModel.openAudioPlaylist("sleep") },
        AudioPlaylistUi("Jumu'ah Favorites", "Al-Kahf · Ya-Sin", 4, R.drawable.audio_lib_playlist_jumuah) { viewModel.openAudioPlaylist("jumuah") },
    )

    BackHandler(enabled = page != "home" || filter != "All") {
        when { page != "home" -> page = "home"; else -> filter = "All" }
    }

    AnimatedContent(page, transitionSpec = {
        (androidx.compose.animation.slideInHorizontally(spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { if (targetState == "home") -it / 4 else it } + androidx.compose.animation.fadeIn()) togetherWith
            (androidx.compose.animation.slideOutHorizontally { if (targetState == "home") it else -it / 4 } + androidx.compose.animation.fadeOut())
    }, label = "audioPage") { pg ->
        when (pg) {
            "search" -> {
                val results = remember(query, surahs) {
                    val q = query.trim()
                    if (q.isBlank()) emptyList() else surahs.filter { it.nameEnglish.contains(q, true) || it.englishTranslation.contains(q, true) || it.id.toString() == q }
                }
                AudioSearchPage(t, query, { query = it }, results, { page = "home" }, ::play, { filter = it; page = "home" }, liked, ::like)
            }
            "reciter" -> {
                val r = openReciter ?: selectedReciter
                val idx = reciters.indexOf(r)
                val tint = rememberCoverColor(portraits.getOrNull(idx), coverColor(r.id + 40))
                val first = surahs.firstOrNull()
                CollapsingDetailPage(
                    t, r.name, r.description.ifBlank { "Complete Qur'an recitation" } + " · ${surahs.size} surahs", tint,
                    art = { size -> ReciterAvatarArt(r, portraits.getOrNull(idx), size) },
                    playing = isPlaying && selectedReciter.id == r.id, shuffle = shuffle,
                    onBack = { page = "home" },
                    onPlay = {
                        if (selectedReciter.id != r.id) viewModel.selectReciter(r)
                        if (isPlaying && selectedReciter.id == r.id) viewModel.toggleAudioPlayback()
                        else (if (shuffle) surahs.randomOrNull() else first)?.let(::play)
                    },
                    onShuffle = { viewModel.toggleShuffleMode() },
                ) {
                    items(surahs, key = { it.id }) { s ->
                        AudioTrackRow(t, s, "${r.name} · ${s.versesCount} ayahs", playingSurah?.id == s.id && selectedReciter.id == r.id, s.id in liked,
                            { if (selectedReciter.id != r.id) viewModel.selectReciter(r); play(s) }, { like(s) })
                    }
                }
            }
            else -> Box(Modifier.fillMaxSize().background(t.bg)) {
                val tint = when (filter) {
                    "All", "Library" -> coverColor(playingSurah?.id ?: 55)
                    else -> AUDIO_BROWSE.firstOrNull { it.label == filter }?.color ?: StitchEmerald
                }
                val animTint by androidx.compose.animation.animateColorAsState(tint, tween(600), label = "tint")
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 210.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    item(key = "hdr") {
                        Box(if (filter == "Recitations") Modifier.recitationPattern(StitchGold.copy(alpha = if (t.light) 0.10f else 0.07f)) else Modifier) {
                            AudioHomeHeader(t, animTint, viewModel.userDisplayName.take(1).ifBlank { "N" }.uppercase(),
                                onBack = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.DASHBOARD) },
                                onSearch = { page = "search" }, onProfile = { viewModel.navigateTo(DeenScreen.PROFILE_DASHBOARD) },
                                filter = filter, onFilter = { filter = it })
                        }
                    }
                    when (filter) {
                        "All" -> {
                            val quickIds = (recent + listOf(55, 18, 36, 67, 1, 112)).distinct().take(6)
                            val quick = quickIds.mapNotNull { byId[it] }.map { s -> AudioQuickItem(s.nameEnglish, s) { play(s) } }
                            if (quick.isNotEmpty()) item(key = "quick") { AudioQuickGrid(t, quick, playingSurah?.nameEnglish?.takeIf { isPlaying }) }
                            val featured = byId[55] ?: surahs.firstOrNull()
                            if (featured != null) item(key = "featured") {
                                FeaturedHero(t, featured, selectedReciter.name, isPlaying && playingSurah?.id == featured.id, { play(featured) }, featured.id in liked) { like(featured) }
                            }
                            item(key = "popular") {
                                Column { AudioSectionHeader(t, "Popular surahs", "Show all") { filter = "Recitations" }; Spacer(Modifier.height(12.dp))
                                    SurahCarousel(t, listOf(1, 36, 55, 56, 67, 18, 2, 112, 114, 113).mapNotNull { byId[it] }, surahs.isEmpty(), ::play) }
                            }
                            item(key = "reciters") {
                                Column { AudioSectionHeader(t, "Top reciters"); Spacer(Modifier.height(12.dp))
                                    ReciterCarousel(t, reciters, portraits, selectedReciter.id) { openReciter = it; page = "reciter" } }
                            }
                            item(key = "playlists") {
                                Column { AudioSectionHeader(t, "Playlists for you"); Spacer(Modifier.height(12.dp)); PlaylistCarousel(t, playlists) }
                            }
                            item(key = "browse") { Column { AudioSectionHeader(t, "Browse all"); Spacer(Modifier.height(12.dp)); BrowseTiles(t) { filter = it } } }
                            item(key = "credits") { AudioCredits(t) }
                        }
                        "Library" -> {
                            val likedList = surahs.filter { it.id in liked }
                            val recentList = recent.mapNotNull { byId[it] }
                            item { AudioSectionHeader(t, "Liked surahs · ${likedList.size}") }
                            if (likedList.isEmpty()) item { Text("Tap ♡ on any surah to save it here.", color = t.muted, modifier = Modifier.padding(horizontal = 16.dp)) }
                            items(likedList, key = { "l${it.id}" }) { s -> AudioTrackRow(t, s, selectedReciter.name, playingSurah?.id == s.id, true, { play(s) }, { like(s) }) }
                            item { AudioSectionHeader(t, "Recently played") }
                            if (recentList.isEmpty()) item { Text("Nothing played yet.", color = t.muted, modifier = Modifier.padding(horizontal = 16.dp)) }
                            items(recentList, key = { "r${it.id}" }) { s -> AudioTrackRow(t, s, selectedReciter.name, playingSurah?.id == s.id, s.id in liked, { play(s) }, { like(s) }) }
                            item { Text("Downloads: Qur'an recitations can be saved offline from the Now Playing screen; nasheeds and podcasts stream only, as their publishers' licences require.",
                                color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
                        }
                        "Recitations" -> {
                            item(key = "rec-reciters") { Column { AudioSectionHeader(t, "Reciters"); Spacer(Modifier.height(12.dp))
                                ReciterCarousel(t, reciters, portraits, selectedReciter.id) { openReciter = it; page = "reciter" } } }
                            item(key = "rec-title") { AudioSectionHeader(t, "All surahs · ${selectedReciter.name}") }
                            if (surahs.isEmpty()) items(6) { Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush(t))) }
                            items(surahs, key = { "s${it.id}" }) { s ->
                                AudioTrackRow(t, s, "${s.englishTranslation} · ${s.versesCount} ayahs", playingSurah?.id == s.id, s.id in liked, { play(s) }, { like(s) })
                            }
                        }
                        "Dua Audio", "Tarjuma", "Tafsir" -> item(key = "free-$filter") { Box(Modifier.padding(horizontal = 16.dp)) { FreeAudioSection(filter) } }
                        "Podcasts" -> item(key = "podcasts") { Box(Modifier.padding(horizontal = 16.dp)) { PodcastSection() } }
                        "Nasheeds" -> item(key = "nasheeds") { Box(Modifier.padding(horizontal = 16.dp)) { PodcastSection(kind = "nasheed") } }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AudioCredits(t: AudioTheme) {
    Text(
        "Credits: Qur'an recitations stream from EveryAyah.com; nasheeds and podcasts come from their publishers' official feeds; dua, tarjuma and tafsir audio is used under its free licences. Each section lists its source.",
        color = t.muted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 16.dp)
    )
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
                .border(1.dp, stitchLine(), CircleShape)
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
            Brush.linearGradient(listOf(stitchLine(), stitchLine()))
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
                .border(1.dp, stitchLine(), CircleShape),
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
        // Decorative watermark sized to the card (no longer drives the card height / gets clipped).
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Icon(
                Icons.Default.QueueMusic,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(76.dp)
            )
        }
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
