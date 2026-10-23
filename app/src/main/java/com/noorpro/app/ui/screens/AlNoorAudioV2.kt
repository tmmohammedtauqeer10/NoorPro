package com.noorpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.noorpro.app.R
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.podcast.FeedResult
import com.noorpro.app.audio.podcast.Podcast
import com.noorpro.app.audio.podcast.PodcastRepository
import com.noorpro.app.data.IslamicData
import com.noorpro.app.data.QuranMetaData
import com.noorpro.app.data.Reciter
import com.noorpro.app.data.Surah
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.viewmodel.AudioQueueItem
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.delay

/* Al-Noor Audio 1.0.81: total redesign modelled on Spotify + Amazon Music (Android).
 * Dark-first (#121212) with a clean light variant. */

internal class SpTheme(val light: Boolean) {
    val bg = if (light) Color(0xFFFFFFFF) else Color(0xFF121212)
    val elevated = if (light) Color(0xFFF2F2F2) else Color(0xFF1E1E1E)
    val high = if (light) Color(0xFFE6E6E6) else Color(0xFF2A2A2A)
    val text = if (light) Color(0xFF0E0E0E) else Color(0xFFFFFFFF)
    val muted = if (light) Color(0xFF6B6B6B) else Color(0xFFB3B3B3)
    val accent = Color(0xFF1ED760)
    val onAccent = Color(0xFF000000)
    val gold = StitchGold
}

@Composable internal fun rememberSpTheme(): SpTheme { val l = isStitchLight(); return remember(l) { SpTheme(l) } }

internal fun surahFor(id: Int): Surah = IslamicData.surahs.firstOrNull { it.id == id } ?: Surah(
    id, QuranMetaData.surahNamesEn.getOrElse(id - 1) { "Surah $id" }, QuranMetaData.surahNamesAr.getOrElse(id - 1) { "" },
    QuranMetaData.surahTranslations.getOrElse(id - 1) { "" }, IslamicData.surahAyahsCount[id] ?: 0,
    QuranMetaData.surahTypes.getOrElse(id - 1) { "" }, emptyList()
)

internal fun q(id: Int) = AudioQueueItem(id, null, "Surah " + QuranMetaData.surahNamesEn.getOrElse(id - 1) { "$id" }, QuranMetaData.surahNamesAr.getOrElse(id - 1) { "" })
internal val KURSI = AudioQueueItem(2, 255, "Ayatul Kursi", "آيَةُ الْكُرْسِيّ")

internal data class SpMix(val key: String, val title: String, val subtitle: String, val cover: Int?, val tint: Color, val tracks: List<AudioQueueItem>)

internal val SP_MIXES = listOf(
    SpMix("morning", "Morning Adhkar", "Al-Fatihah, Ayatul Kursi and the three Quls", R.drawable.audio_lib_playlist_adhkar, Color(0xFFB27A2E), listOf(q(1), KURSI, q(112), q(113), q(114))),
    SpMix("soothing", "Soothing Recitations", "Ar-Rahman, Al-Waqi'ah, Al-Mulk, Ya-Sin", null, Color(0xFF6B3FA0), listOf(55, 56, 67, 36, 73, 76).map(::q)),
    SpMix("jumuah", "Jumu'ah Favorites", "Al-Kahf, Ya-Sin and Ar-Rahman for Friday", R.drawable.audio_lib_playlist_jumuah, Color(0xFF0F5D46), listOf(18, 1, 36, 55).map(::q)),
    SpMix("prophets", "Stories of the Prophets", "Yusuf, Maryam, Al-Anbiya, Al-Qasas, Nuh", null, Color(0xFF1E5AA8), listOf(12, 19, 21, 28, 71).map(::q)),
    SpMix("sleep", "Sleep Supplications", "Ayatul Kursi, Al-Mulk, As-Sajdah, the Quls", R.drawable.audio_lib_playlist_sleep, Color(0xFF3B4C9B), listOf(KURSI) + listOf(67, 32, 112, 113, 114).map(::q)),
    SpMix("juzamma", "Juz 'Amma", "The 37 short surahs, An-Naba to An-Nas", null, Color(0xFFB0532A), (78..114).map(::q)),
)

internal val SP_TOP_SURAHS = listOf(36, 55, 18, 67, 1, 56, 2, 112, 19, 12, 73, 114, 113, 32, 78, 3, 25, 48, 50, 76)
internal val SP_REC_PORTRAITS = listOf(R.drawable.audio_lib_reciter_1, R.drawable.audio_lib_reciter_2, R.drawable.audio_lib_reciter_3, R.drawable.audio_lib_reciter_4)

internal object SpStore {
    private fun p(c: android.content.Context) = c.getSharedPreferences("noor_audio_lib", android.content.Context.MODE_PRIVATE)
    fun set(c: android.content.Context, k: String): Set<String> = p(c).getString(k, "").orEmpty().split(',').filter { it.isNotBlank() }.toSet()
    fun toggle(c: android.content.Context, k: String, v: String): Set<String> {
        val s = set(c, k).toMutableSet(); if (!s.add(v)) s.remove(v)
        p(c).edit().putString(k, s.joinToString(",")).apply(); return s
    }
}

internal data class SpHero(val kicker: String, val title: String, val subtitle: String, val tint: Color, val res: Int?, val url: String?, val cta: String, val onClick: () -> Unit)

/* ---------------- primitives ---------------- */

@Composable
internal fun SpArt(modifier: Modifier, surah: Surah? = null, res: Int? = null, url: String? = null, tint: Color = coverColor(surah?.id ?: 7), label: String = "", corner: Dp = 6.dp, circle: Boolean = false) {
    val shape = if (circle) CircleShape else RoundedCornerShape(corner)
    BoxWithConstraints(modifier.clip(shape).background(Brush.linearGradient(listOf(lerp(tint, Color.White, 0.15f), tint, lerp(tint, Color.Black, 0.5f)))), contentAlignment = Alignment.Center) {
        val w = maxWidth
        when {
            res != null -> Image(painterResource(res), label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            surah != null -> {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent))))
                if (w >= 56.dp) Text(surah.nameArabic, color = Color.White, fontFamily = QuranArabicFont, fontSize = (w.value / 4.6f).sp, maxLines = 1, textAlign = TextAlign.Center)
                Text("${surah.id}", color = Color.White.copy(alpha = 0.9f), fontSize = (w.value / 9f).coerceAtLeast(9f).sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.BottomStart).padding(w / 14))
            }
            label.isNotBlank() -> Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = (w.value / 7f).coerceAtLeast(10f).sp, lineHeight = (w.value / 6.2f).coerceAtLeast(12f).sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(w / 12), maxLines = 3)
        }
        if (!url.isNullOrBlank()) AsyncImage(model = url, contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun SpShimmer(t: SpTheme, modifier: Modifier) = Box(modifier.background(shimmerBrush(AudioTheme(t.light))))

@Composable
internal fun SpPlayButton(t: SpTheme, playing: Boolean, size: Dp = 56.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).shadow(8.dp, CircleShape).clip(CircleShape).background(t.accent).pressScale(onClick), contentAlignment = Alignment.Center) {
        AnimatedContent(playing, transitionSpec = { (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith fadeOut() }, label = "pp") { p ->
            Icon(if (p) Icons.Default.Pause else Icons.Default.PlayArrow, if (p) "Pause" else "Play", tint = t.onAccent, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
internal fun SpChips(t: SpTheme, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val sel = o == selected
            val bg by animateColorAsState(if (sel) t.accent else t.high, tween(250), label = "chip")
            Box(Modifier.clip(CircleShape).background(bg).pressScale { onSelect(o) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(o, color = if (sel) t.onAccent else t.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun SpShelf(t: SpTheme, title: String, kicker: String? = null, action: String? = null, onAction: () -> Unit = {}, content: LazyListScope.() -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (kicker != null) Text(kicker, color = t.muted, fontSize = 12.sp)
                Text(title, color = t.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (action != null) Text(action, color = t.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(6.dp))
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
internal fun SpCard(t: SpTheme, title: String, subtitle: String, size: Dp = 150.dp, circle: Boolean = false, onClick: () -> Unit, art: @Composable (Modifier) -> Unit) {
    Column(Modifier.width(size).pressScale(onClick), horizontalAlignment = if (circle) Alignment.CenterHorizontally else Alignment.Start) {
        art(Modifier.size(size).shadow(if (t.light) 2.dp else 6.dp, if (circle) CircleShape else RoundedCornerShape(6.dp)))
        Spacer(Modifier.height(8.dp))
        Text(title, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, minLines = if (circle) 2 else 1, overflow = TextOverflow.Ellipsis,
            textAlign = if (circle) TextAlign.Center else TextAlign.Start, lineHeight = 17.sp)
        Text(subtitle, color = t.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = if (circle) TextAlign.Center else TextAlign.Start, lineHeight = 15.sp)
    }
}

@Composable
internal fun SpTrackRow(t: SpTheme, index: Int?, title: String, subtitle: String, playing: Boolean, onClick: () -> Unit,
                        art: (@Composable (Modifier) -> Unit)? = null, liked: Boolean? = null, downloaded: Boolean = false,
                        menu: List<Pair<String, () -> Unit>> = emptyList()) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (index != null) Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
            if (playing) EqualizerBars(t.accent, Modifier.size(14.dp)) else Text("$index", color = t.muted, fontSize = 15.sp)
        }
        if (art != null) { art(Modifier.size(48.dp)); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = if (playing) t.accent else t.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (downloaded) { Icon(Icons.Default.DownloadDone, "Downloaded", tint = t.accent, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                Text(subtitle, color = t.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (liked == true) Icon(Icons.Default.Favorite, "Liked", tint = t.accent, modifier = Modifier.padding(horizontal = 4.dp).size(18.dp))
        if (menu.isNotEmpty()) Box {
            var open by remember { mutableStateOf(false) }
            Icon(Icons.Default.MoreVert, "More", tint = t.muted, modifier = Modifier.clip(CircleShape).clickable { open = true }.padding(8.dp))
            DropdownMenu(open, { open = false }, modifier = Modifier.background(t.elevated)) {
                menu.forEach { (label, act) -> DropdownMenuItem(text = { Text(label, color = t.text) }, onClick = { open = false; act() }) }
            }
        }
    }
}

@Composable internal fun LikedArt(m: Modifier) = Box(m.background(Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFFC4EFD9)))), contentAlignment = Alignment.Center) {
    Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.fillMaxSize(0.42f))
}

@Composable
internal fun SpCredits(t: SpTheme) {
    Text(
        "Sources & credits: Qur'an recitations stream from EveryAyah.com. Nasheeds and podcasts load from each publisher's official public RSS feed and stream from their servers. " +
            "Dua, tarjuma and tafsir audio comes from free, licensed sources; open a category and tap “Audio sources” for full attribution.",
        color = t.muted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/* ---------------- Home ---------------- */

internal class SpHomeData(
    val greeting: String, val initial: String,
    val recent: List<Surah>, val liked: Set<Int>,
    val reciters: List<Reciter>, val selectedReciterId: Int,
    val playingSurahId: Int?, val isPlaying: Boolean,
    val nasheeds: List<Podcast>, val podcasts: List<Podcast>, val artFor: (Podcast) -> String?,
)

@Composable
internal fun SpHeroCarousel(t: SpTheme, items: List<SpHero>, autoScroll: Boolean = true) {
    val pager = rememberPagerState { items.size }
    if (autoScroll) LaunchedEffect(pager, items.size) {
        while (items.size > 1) { delay(5000); pager.animateScrollToPage((pager.currentPage + 1) % items.size, animationSpec = tween(650)) }
    }
    Column {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 10.dp) { i ->
            val h = items[i]
            Box(Modifier.fillMaxWidth().height(208.dp).clip(RoundedCornerShape(14.dp)).pressScale(h.onClick)) {
                SpArt(Modifier.fillMaxSize(), res = h.res, url = h.url, tint = h.tint, corner = 0.dp)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, h.tint.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.88f)))))
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(h.kicker.uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text(h.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 30.sp)
                    Text(h.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text(h.cta, color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(items.size) { i ->
                val w by animateFloatAsState(if (pager.currentPage == i) 18f else 6f, spring(stiffness = Spring.StiffnessMediumLow), label = "dot")
                Box(Modifier.padding(horizontal = 3.dp).height(6.dp).width(w.dp).clip(CircleShape).background(if (pager.currentPage == i) t.text else t.muted.copy(alpha = 0.4f)))
            }
        }
    }
}

internal class SpTile(val title: String, val art: @Composable (Modifier) -> Unit, val onClick: () -> Unit)

@Composable
internal fun SpQuickTiles(t: SpTheme, tiles: List<SpTile>, playingTitle: String?) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tile ->
                    Row(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(6.dp)).background(t.high).pressScale(tile.onClick), verticalAlignment = Alignment.CenterVertically) {
                        tile.art(Modifier.size(56.dp))
                        Text(tile.title, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                        if (tile.title == playingTitle) EqualizerBars(t.accent, Modifier.padding(end = 10.dp).size(12.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun SpHomeTab(t: SpTheme, d: SpHomeData, listState: LazyListState, open: (String) -> Unit, playSurah: (Surah) -> Unit, playMix: (List<AudioQueueItem>, Int) -> Unit,
                       onExit: () -> Unit, onProfile: () -> Unit, heroAuto: Boolean = true) {
    var chip by rememberSaveable { mutableStateOf("All") }
    val tint by animateColorAsState(coverColor(d.playingSurahId ?: 55), tween(600), label = "ht")
    val heroes = buildList {
        add(SpHero("Featured recitation", "Surah Ar-Rahman", "Which of the favours of your Lord will you deny?", coverColor(55), R.drawable.audio_lib_audio_hero, null, "Play now") { playSurah(surahFor(55)) })
        add(SpHero("Friday sunnah", "Surah Al-Kahf", "A light between the two Fridays", Color(0xFF0F5D46), R.drawable.audio_lib_playlist_jumuah, null, "Listen") { playSurah(surahFor(18)) })
        d.nasheeds.firstOrNull()?.let { n -> add(SpHero("Nasheeds", n.name, n.publisher, Color(0xFF8C2F5B), null, d.artFor(n), "Open") { open("podcast:${n.id}") }) }
        add(SpHero("Made for you", "Morning Adhkar", "Start your day with remembrance", Color(0xFFB27A2E), R.drawable.audio_lib_playlist_adhkar, null, "Play mix") { playMix(SP_MIXES[0].tracks, 0) })
        d.podcasts.firstOrNull()?.let { p -> add(SpHero("Podcasts", p.name, p.publisher, Color(0xFF1E5AA8), null, d.artFor(p), "Open") { open("podcast:${p.id}") }) }
    }
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
        item(key = "top") {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(tint.copy(alpha = if (t.light) 0.22f else 0.42f), t.bg)))) {
                Column(Modifier.statusBarsPadding().padding(top = 10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(t.accent).clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                            Text(d.initial, color = t.onAccent, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(d.greeting, color = t.text, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.History, "Recently played", tint = t.text, modifier = Modifier.clip(CircleShape).clickable { open("recent") }.padding(8.dp))
                        Icon(Icons.Default.Close, "Close Al-Noor Audio", tint = t.text, modifier = Modifier.clip(CircleShape).clickable(onClick = onExit).padding(8.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    SpChips(t, listOf("All", "Quran", "Nasheeds", "Podcasts", "Duas"), chip) { chip = it }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        val all = chip == "All"
        val quran = all || chip == "Quran"
        if (all) item(key = "hero") { SpHeroCarousel(t, heroes, heroAuto) }
        if (quran) item(key = "quick") {
            val ids = (d.recent.map { it.id } + listOf(36, 18, 67, 1)).distinct().take(4)
            val tiles = buildList {
                add(SpTile("Liked Surahs", { m -> LikedArt(m) }) { open("liked") })
                add(SpTile(SP_MIXES[0].title, { m -> SpArt(m, res = SP_MIXES[0].cover, corner = 0.dp) }) { open("mix:morning") })
                ids.forEach { id -> val s = surahFor(id); add(SpTile(s.nameEnglish, { m -> SpArt(m, surah = s, corner = 0.dp) }) { playSurah(s) }) }
            }
            SpQuickTiles(t, tiles, d.playingSurahId?.let { surahFor(it).nameEnglish }?.takeIf { d.isPlaying })
        }
        if (quran) item(key = "mfy") {
            SpShelf(t, "Made for you", kicker = "Curated mixes") {
                items(SP_MIXES, key = { it.key }) { m -> SpCard(t, m.title, m.subtitle, onClick = { open("mix:${m.key}") }) { mod -> SpArt(mod, res = m.cover, tint = m.tint, label = if (m.cover == null) m.title else "") } }
            }
        }
        if (quran && d.recent.isNotEmpty()) item(key = "recent") {
            SpShelf(t, "Recently played", action = "See all", onAction = { open("recent") }) {
                items(d.recent.take(10), key = { "r${it.id}" }) { s -> SpCard(t, s.nameEnglish, s.englishTranslation.ifBlank { "Surah ${s.id}" }, size = 130.dp, onClick = { playSurah(s) }) { mod -> SpArt(mod, surah = s) } }
            }
        }
        if (quran) item(key = "reciters") {
            SpShelf(t, "Top reciters", action = "Show all", onAction = { open("reciters") }) {
                itemsIndexed(d.reciters, key = { _, r -> "rc${r.id}" }) { i, r ->
                    SpCard(t, r.name, "Reciter", size = 120.dp, circle = true, onClick = { open("reciter:${r.id}") }) { _ -> ReciterAvatarArt(r, SP_REC_PORTRAITS.getOrNull(i), 120.dp) }
                }
            }
        }
        if ((all || chip == "Nasheeds") && d.nasheeds.isNotEmpty()) item(key = "nasheeds") {
            SpShelf(t, "Nasheeds & Naats", action = "Show all", onAction = { open("browse:Nasheeds") }) {
                items(d.nasheeds, key = { "n${it.id}" }) { p -> SpCard(t, p.name, "${p.publisher} · ${p.language}", onClick = { open("podcast:${p.id}") }) { mod -> SpArt(mod, url = d.artFor(p), tint = coverColor(p.id.hashCode()), label = p.name) } }
            }
        }
        if ((all || chip == "Podcasts") && d.podcasts.isNotEmpty()) item(key = "podcasts") {
            SpShelf(t, "Islamic podcasts", action = "Show all", onAction = { open("browse:Podcasts") }) {
                items(d.podcasts, key = { "p${it.id}" }) { p -> SpCard(t, p.name, "${p.publisher} · ${p.category}", onClick = { open("podcast:${p.id}") }) { mod -> SpArt(mod, url = d.artFor(p), tint = coverColor(p.id.hashCode() + 3), label = p.name, corner = 10.dp) } }
            }
        }
        if (all || chip == "Duas") item(key = "duas") {
            SpShelf(t, "Duas & Adhkar") {
                item { SpCard(t, "Dua Audio", "Daily duas, free licensed audio", onClick = { open("cat:Dua Audio") }) { mod -> SpArt(mod, tint = Color(0xFFB0532A), label = "Dua\nAudio") } }
                item { SpCard(t, "Ayatul Kursi", "Al-Baqarah 2:255", onClick = { playMix(listOf(KURSI), 0) }) { mod -> SpArt(mod, tint = Color(0xFF7A6A1E), label = "Ayatul\nKursi") } }
                item { SpCard(t, "Morning Adhkar", "Mix", onClick = { open("mix:morning") }) { mod -> SpArt(mod, res = R.drawable.audio_lib_playlist_adhkar) } }
                item { SpCard(t, "Sleep Supplications", "Mix", onClick = { open("mix:sleep") }) { mod -> SpArt(mod, res = R.drawable.audio_lib_playlist_sleep) } }
            }
        }
        if (all || chip == "Duas" || chip == "Quran") item(key = "tafsir") {
            SpShelf(t, "Tafsir & Tarjuma") {
                item { SpCard(t, "Tafsir Audio", "Explanation of the Qur'an", onClick = { open("cat:Tafsir") }) { mod -> SpArt(mod, tint = Color(0xFF8C2F5B), label = "Tafsir") } }
                item { SpCard(t, "Quran Tarjuma", "Translation audio", onClick = { open("cat:Tarjuma") }) { mod -> SpArt(mod, tint = Color(0xFF2E7D6B), label = "Tarjuma") } }
                item { SpCard(t, "All 114 surahs", "Full recitations", onClick = { open("recitations") }) { mod -> SpArt(mod, tint = Color(0xFF0F5D46), label = "Al-Qur'an") } }
            }
        }
        if (quran) item(key = "charts") {
            Column {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) { Text("Charts", color = t.muted, fontSize = 12.sp); Text("Top surahs", color = t.text, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    Text("See all", color = t.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable { open("charts") }.padding(6.dp))
                }
                Spacer(Modifier.height(6.dp))
                SP_TOP_SURAHS.take(5).forEachIndexed { i, id -> ChartRow(t, i + 1, surahFor(id), d.playingSurahId == id) { playSurah(surahFor(id)) } }
            }
        }
        item(key = "credits") { SpCredits(t) }
    }
}

@Composable
internal fun ChartRow(t: SpTheme, rank: Int, s: Surah, playing: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$rank", color = t.text, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(34.dp))
        SpArt(Modifier.size(52.dp), surah = s, corner = 4.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.nameEnglish, color = if (playing) t.accent else t.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${s.englishTranslation.ifBlank { "Surah" }} · ${s.versesCount} ayahs", color = t.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(s.nameArabic, color = t.muted, fontFamily = QuranArabicFont, fontSize = 16.sp)
    }
}

/* ---------------- Search ---------------- */

internal data class SpBrowse(val label: String, val color: Color, val icon: ImageVector, val route: String)
internal val SP_BROWSE = listOf(
    SpBrowse("Recitations", Color(0xFF0F7A4F), Icons.Default.MenuBook, "recitations"),
    SpBrowse("Nasheeds", Color(0xFF8D3FD1), Icons.Default.MusicNote, "browse:Nasheeds"),
    SpBrowse("Podcasts", Color(0xFF1E5AA8), Icons.Default.Podcasts, "browse:Podcasts"),
    SpBrowse("Duas", Color(0xFFD2691E), Icons.Default.FrontHand, "cat:Dua Audio"),
    SpBrowse("Tafsir", Color(0xFFB02E6B), Icons.Default.AutoStories, "cat:Tafsir"),
    SpBrowse("Tarjuma", Color(0xFF3A7D2C), Icons.Default.Translate, "cat:Tarjuma"),
    SpBrowse("Reciters", Color(0xFF6B4E16), Icons.Default.RecordVoiceOver, "reciters"),
    SpBrowse("Charts", Color(0xFF283593), Icons.Default.TrendingUp, "charts"),
    SpBrowse("Made for you", Color(0xFF8C2F5B), Icons.Default.AutoAwesome, "tab:library"),
    SpBrowse("Downloads", Color(0xFF00796B), Icons.Default.DownloadForOffline, "downloads"),
)

@Composable
internal fun SpSearchTab(t: SpTheme, query: String, onQuery: (String) -> Unit, surahs: List<Surah>, reciters: List<Reciter>, pods: List<Podcast>,
                         artFor: (Podcast) -> String?, open: (String) -> Unit, playSurah: (Surah) -> Unit) {
    val q = query.trim()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 190.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(16.dp)) {
                Text("Search", color = t.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(if (t.light) Color(0xFFEDEDED) else Color.White).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Color(0xFF121212))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("What do you want to listen to?", color = Color(0xFF535353), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        BasicTextField(query, onQuery, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF121212), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            cursorBrush = SolidColor(Color(0xFF121212)), modifier = Modifier.fillMaxWidth())
                    }
                    if (query.isNotEmpty()) Icon(Icons.Default.Close, "Clear", tint = Color(0xFF121212), modifier = Modifier.clip(CircleShape).clickable { onQuery("") }.padding(4.dp))
                }
            }
        }
        if (q.isEmpty()) {
            item { Text("Browse all", color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
            items(SP_BROWSE.chunked(2)) { row ->
                Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { b ->
                        Box(Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(8.dp)).background(b.color).pressScale { open(b.route) }) {
                            Text(b.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(12.dp))
                            Icon(b.icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.align(Alignment.BottomEnd).offset(x = 14.dp, y = 6.dp).size(64.dp).rotate(24f))
                        }
                    }
                }
            }
        } else {
            val sr = surahs.filter { it.nameEnglish.contains(q, true) || it.englishTranslation.contains(q, true) || it.id.toString() == q || it.nameArabic.contains(q) }.take(20)
            val rr = reciters.filter { it.name.contains(q, true) }
            val pr = pods.filter { it.name.contains(q, true) || it.publisher.contains(q, true) || it.category.contains(q, true) }
            val mr = SP_MIXES.filter { it.title.contains(q, true) || it.subtitle.contains(q, true) }
            if (sr.isEmpty() && rr.isEmpty() && pr.isEmpty() && mr.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't find \"$q\"", color = t.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Try a surah name, number, reciter or podcast.", color = t.muted, fontSize = 13.sp)
                }
            }
            items(rr, key = { "sr${it.id}" }) { r -> SpTrackRow(t, null, r.name, "Reciter", false, { open("reciter:${r.id}") }, art = { _ -> ReciterAvatarArt(r, SP_REC_PORTRAITS.getOrNull(IslamicData.reciters.indexOf(r)), 48.dp) }) }
            items(mr, key = { "sm${it.key}" }) { m -> SpTrackRow(t, null, m.title, "Playlist · Noor Pro", false, { open("mix:${m.key}") }, art = { mod -> SpArt(mod, res = m.cover, tint = m.tint) }) }
            items(pr, key = { "sp${it.id}" }) { p -> SpTrackRow(t, null, p.name, (if (p.kind == "nasheed") "Nasheeds · " else "Podcast · ") + p.publisher, false, { open("podcast:${p.id}") },
                art = { mod -> SpArt(mod, url = artFor(p), tint = coverColor(p.id.hashCode()), corner = 4.dp) }) }
            items(sr, key = { "ss${it.id}" }) { s -> SpTrackRow(t, null, s.nameEnglish, "Surah · ${s.englishTranslation}", false, { playSurah(s) }, art = { m -> SpArt(m, surah = s, corner = 4.dp) }) }
        }
    }
}

/* ---------------- Library ---------------- */

internal class SpLibItem(val key: String, val title: String, val subtitle: String, val kind: String, val circle: Boolean = false, val art: @Composable (Modifier) -> Unit)

@Composable
internal fun SpLibraryTab(t: SpTheme, initial: String, items: List<SpLibItem>, open: (String) -> Unit, onProfile: () -> Unit, startGrid: Boolean = false, startFilter: String? = null) {
    var filter by remember { mutableStateOf(startFilter) }
    var grid by remember { mutableStateOf(startGrid) }
    val shown = items.filter { filter == null || it.kind == filter }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(top = 10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(t.accent).clickable(onClick = onProfile), contentAlignment = Alignment.Center) { Text(initial, color = t.onAccent, fontWeight = FontWeight.Black) }
                Spacer(Modifier.width(12.dp))
                Text("Your Library", color = t.text, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Search, "Search", tint = t.text, modifier = Modifier.clip(CircleShape).clickable { open("tab:search") }.padding(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(filter != null) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(t.high).clickable { filter = null }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, "Clear filter", tint = t.text, modifier = Modifier.size(18.dp)) }
                }
                listOf("Playlists", "Reciters", "Podcasts", "Downloads").forEach { f ->
                    if (filter == null || filter == f) {
                        val sel = filter == f
                        Box(Modifier.clip(CircleShape).background(if (sel) t.accent else t.high).pressScale { filter = if (sel) null else f }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text(f, color = if (sel) t.onAccent else t.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapVert, null, tint = t.text, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text("Recents", color = t.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(if (grid) Icons.AutoMirrored.Filled.List else Icons.Default.GridView, if (grid) "List view" else "Grid view", tint = t.text,
                    modifier = Modifier.clip(CircleShape).clickable { grid = !grid }.padding(4.dp).size(20.dp))
            }
        }
        AnimatedContent(grid, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "libmode", modifier = Modifier.weight(1f)) { g ->
            if (g) LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 190.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(shown.size, key = { shown[it].key }) { idx ->
                    val it2 = shown[idx]
                    Column(Modifier.pressScale { open(it2.key) }, horizontalAlignment = if (it2.circle) Alignment.CenterHorizontally else Alignment.Start) {
                        it2.art(Modifier.fillMaxWidth().aspectRatio(1f).clip(if (it2.circle) CircleShape else RoundedCornerShape(4.dp)))
                        Spacer(Modifier.height(6.dp))
                        Text(it2.title, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp, textAlign = if (it2.circle) TextAlign.Center else TextAlign.Start)
                        Text(it2.subtitle, color = t.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else LazyColumn(contentPadding = PaddingValues(bottom = 190.dp)) {
                items(shown, key = { it.key }) { it2 ->
                    Row(Modifier.fillMaxWidth().clickable { open(it2.key) }.padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        it2.art(Modifier.size(64.dp).clip(if (it2.circle) CircleShape else RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(it2.title, color = t.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (it2.key == "liked") { Icon(Icons.Default.PushPin, null, tint = t.accent, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)) }
                                Text(it2.subtitle, color = t.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (shown.isEmpty()) item {
                    Text(when (filter) {
                        "Downloads" -> "No downloads yet. Tap the download icon on a Qur'an playlist, or ⋮ → Download on a surah. Nasheeds and podcasts stream only, per their publishers' licences."
                        "Podcasts" -> "Follow a podcast or nasheed channel with ♡ to see it here."
                        else -> "Nothing here yet."
                    }, color = t.muted, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

/* ---------------- Detail page ---------------- */

internal class SpDetailHeader(val title: String, val subtitle: String, val meta: String, val tint: Color, val circle: Boolean = false, val art: @Composable (Modifier) -> Unit)

@Composable
internal fun SpDetailPage(
    t: SpTheme, h: SpDetailHeader, playing: Boolean, shuffle: Boolean, liked: Boolean?, download: String?, onBack: () -> Unit,
    onPlay: (() -> Unit)?, onShuffle: () -> Unit, onLike: () -> Unit, onDownload: () -> Unit, pattern: Boolean = false,
    listState: LazyListState = rememberLazyListState(), content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val headerPx = with(density) { 300.dp.toPx() }
    val scroll by remember { derivedStateOf { if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else headerPx } }
    val frac = (scroll / headerPx).coerceIn(0f, 1f)
    val tint by animateColorAsState(h.tint, tween(500), label = "dt")
    Box(Modifier.fillMaxSize().background(t.bg)) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 190.dp)) {
            item(key = "header") {
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(lerp(tint, t.bg, if (t.light) 0.35f else 0.1f), lerp(tint, t.bg, 0.65f), t.bg)))
                    .then(if (pattern) Modifier.recitationPattern(StitchGold.copy(alpha = 0.09f)) else Modifier)) {
                    Spacer(Modifier.statusBarsPadding().height(52.dp))
                    Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                        h.art(Modifier.graphicsLayer {
                            translationY = scroll * 0.45f; val s = 1f - 0.35f * frac; scaleX = s; scaleY = s; alpha = 1f - frac * 0.9f
                        }.shadow(24.dp, if (h.circle) CircleShape else RoundedCornerShape(4.dp)).size(236.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(h.title, color = t.text, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 32.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(h.subtitle, color = t.text.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 16.dp))
                    Text(h.meta, color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (liked != null) {
                            val s by animateFloatAsState(if (liked) 1.15f else 1f, spring(dampingRatio = 0.35f), label = "lk")
                            Icon(if (liked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, if (liked) "Unlike" else "Like", tint = if (liked) t.accent else t.muted,
                                modifier = Modifier.clip(CircleShape).clickable(onClick = onLike).padding(10.dp).size(26.dp).graphicsLayer { scaleX = s; scaleY = s })
                        }
                        if (download != null) Icon(
                            when (download) { "done" -> Icons.Default.DownloadDone; "busy" -> Icons.Default.Downloading; else -> Icons.Default.Download },
                            "Download", tint = if (download == "done") t.accent else t.muted, modifier = Modifier.clip(CircleShape).clickable(onClick = onDownload).padding(10.dp).size(26.dp))
                        Spacer(Modifier.weight(1f))
                        if (onPlay != null) {
                            Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) t.accent else t.muted, modifier = Modifier.clip(CircleShape).clickable(onClick = onShuffle).padding(10.dp).size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            SpPlayButton(t, playing, 56.dp, onPlay)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            content()
        }
        Row(Modifier.fillMaxWidth().background(lerp(tint, t.bg, 0.45f).copy(alpha = ((frac - 0.55f) / 0.35f).coerceIn(0f, 1f))).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f * (1f - frac))).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = if (frac < 0.5f) Color.White else t.text)
            }
            Spacer(Modifier.width(10.dp))
            Text(h.title, color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = ((frac - 0.7f) / 0.3f).coerceIn(0f, 1f) })
            if (onPlay != null) AnimatedVisibility(frac > 0.92f, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(), exit = fadeOut()) { SpPlayButton(t, playing, 40.dp, onPlay) }
        }
    }
}

@Composable
internal fun SpTabBar(t: SpTheme, tab: Int, modifier: Modifier = Modifier, onTab: (Int) -> Unit) {
    Box(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, t.bg.copy(alpha = 0.94f), t.bg)))) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 18.dp).height(56.dp)) {
            listOf(Triple("Home", Icons.Default.Home, Icons.Outlined.Home), Triple("Search", Icons.Default.Search, Icons.Outlined.Search), Triple("Your Library", Icons.Default.LibraryMusic, Icons.Outlined.LibraryMusic))
                .forEachIndexed { i, (label, on, off) ->
                    val sel = i == tab
                    val sc by animateFloatAsState(if (sel) 1.08f else 1f, spring(dampingRatio = 0.45f), label = "tab")
                    Column(Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onTab(i) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(if (sel) on else off, label, tint = if (sel) t.text else t.muted, modifier = Modifier.size(26.dp).graphicsLayer { scaleX = sc; scaleY = sc })
                        Text(label, color = if (sel) t.text else t.muted, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
        }
    }
}

/* ---------------- Screen (state + routing) ---------------- */

@Composable
fun AlNoorAudioScreen(viewModel: DeenViewModel, startRoute: String? = null) {
    val ctx = LocalContext.current
    val t = rememberSpTheme()
    val surahsVm by viewModel.surahs.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val playingSurah by viewModel.playingSurah.collectAsState()
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val shuffle by viewModel.isShuffleEnabled.collectAsState()
    val dl by viewModel.surahAudioDownload.collectAsState()
    val allSurahs = remember(surahsVm) { if (surahsVm.size >= 114) surahsVm else (1..114).map(::surahFor) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<String>().also { if (startRoute != null) it.add(startRoute) } }
    var query by rememberSaveable { mutableStateOf("") }
    var liked by remember { mutableStateOf(AudioLibraryStore.liked(ctx)) }
    var recent by remember { mutableStateOf(AudioLibraryStore.recent(ctx)) }
    var follows by remember { mutableStateOf(SpStore.set(ctx, "follows")) }
    var cached by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var dlTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedReciter.id, dlTick) {
        cached = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { (1..114).filter { viewModel.isSurahAudioCached(it) }.toSet() }
    }
    val homeState = rememberLazyListState()
    val pods = PodcastRepository.podcasts
    val nasheeds = remember { pods.filter { it.kind == "nasheed" } }
    val podcasts = remember { pods.filter { it.kind != "nasheed" } }
    var podcastArtwork by remember {
        mutableStateOf(pods.associate { it.id to PodcastRepository.cachedArtwork(ctx, it) })
    }
    LaunchedEffect(Unit) {
        // Feed artwork is the publisher's real podcast/channel image. Load it progressively so
        // cards replace generated colour tiles with relevant art without blocking the screen.
        pods.forEach { p ->
            if (podcastArtwork[p.id].isNullOrBlank()) {
                PodcastRepository.load(ctx, p).artwork?.takeIf { it.isNotBlank() }?.let { art ->
                    podcastArtwork = podcastArtwork + (p.id to art)
                }
            }
        }
    }
    val artFor: (Podcast) -> String? = { p -> podcastArtwork[p.id] ?: PodcastRepository.cachedArtwork(ctx, p) }
    val initial = viewModel.userDisplayName.take(1).ifBlank { "N" }.uppercase()

    fun open(r: String) {
        when (r) {
            "tab:search" -> { tab = 1; stack.clear() }
            "tab:library" -> { tab = 2; stack.clear() }
            else -> stack.add(r)
        }
    }
    fun play(s: Surah) {
        if (playingSurah?.id == s.id) viewModel.toggleAudioPlayback() else viewModel.selectSurahForPlayback(s)
        recent = AudioLibraryStore.addRecent(ctx, s.id)
    }
    fun playItems(items: List<AudioQueueItem>, i: Int) {
        viewModel.playQueue(items, i)
        items.getOrNull(i)?.let { recent = AudioLibraryStore.addRecent(ctx, it.surahId) }
    }
    fun downloadAll(ids: List<Int>) {
        val todo = ids.filter { it !in cached }
        if (todo.isEmpty()) return
        if (dl != null) { android.widget.Toast.makeText(ctx, "A download is already running…", android.widget.Toast.LENGTH_SHORT).show(); return }
        android.widget.Toast.makeText(ctx, "Downloading ${todo.size} surah(s) for offline listening…", android.widget.Toast.LENGTH_SHORT).show()
        fun next(rest: List<Int>) {
            val id = rest.firstOrNull() ?: run { dlTick++; return }
            viewModel.downloadSurahAudio(id) { dlTick++; next(rest.drop(1)) }
        }
        next(todo)
    }
    fun share(text: String) {
        val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
        runCatching { ctx.startActivity(android.content.Intent.createChooser(i, "Share")) }
    }
    fun surahMenu(s: Surah, canDownload: Boolean = true): List<Pair<String, () -> Unit>> = buildList {
        add((if (s.id in liked) "Remove from Liked Surahs" else "Add to Liked Surahs") to { liked = AudioLibraryStore.toggleLike(ctx, s.id) })
        if (canDownload) add((if (s.id in cached) "Downloaded" else "Download") to { downloadAll(listOf(s.id)) })
        add("Share" to { share("Listen to Surah ${s.nameEnglish} (${s.nameArabic}) on Noor Pro") })
    }

    BackHandler(enabled = stack.isNotEmpty() || tab != 0) { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else tab = 0 }

    Box(Modifier.fillMaxSize().background(t.bg)) {
        val depth = stack.size
        AnimatedContent(targetState = Triple(tab, stack.lastOrNull(), depth), transitionSpec = {
            when {
                targetState.first != initialState.first -> fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                targetState.third > initialState.third -> (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                else -> (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            }
        }, label = "audioNav", modifier = Modifier.fillMaxSize()) { (tb, rt, _) ->
            val back: () -> Unit = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
            when {
                rt == null && tb == 0 -> SpHomeTab(t, SpHomeData(greetingFor(), initial, recent.map(::surahFor), liked, IslamicData.reciters, selectedReciter.id, playingSurah?.id, isPlaying, nasheeds, podcasts, artFor),
                    homeState, ::open, ::play, ::playItems,
                    onExit = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.DASHBOARD) }, onProfile = { viewModel.navigateTo(DeenScreen.PROFILE_DASHBOARD) })
                rt == null && tb == 1 -> SpSearchTab(t, query, { query = it }, allSurahs, IslamicData.reciters, pods, artFor, ::open, ::play)
                rt == null -> SpLibraryTab(t, initial, spLibraryItems(liked.size, recent.size, follows, selectedReciter.id, cached.size, selectedReciter.name, pods, artFor), ::open, { viewModel.navigateTo(DeenScreen.PROFILE_DASHBOARD) })
                rt.startsWith("mix:") || rt in setOf("liked", "recent", "charts", "downloads", "recitations") -> {
                    val mix = SP_MIXES.firstOrNull { "mix:${it.key}" == rt }
                    val tracks: List<AudioQueueItem> = when (rt) {
                        "liked" -> allSurahs.filter { it.id in liked }.map { q(it.id) }
                        "recent" -> recent.map(::q)
                        "charts" -> SP_TOP_SURAHS.map(::q)
                        "downloads" -> cached.sorted().map(::q)
                        "recitations" -> (1..114).map(::q)
                        else -> mix?.tracks.orEmpty()
                    }
                    val h = spListHeader(rt, mix, tracks.size, selectedReciter.name)
                    val ids = tracks.filter { it.ayah == null }.map { it.surahId }
                    val allCached = ids.isNotEmpty() && ids.all { it in cached }
                    val here = isPlaying && tracks.any { it.surahId == playingSurah?.id }
                    SpDetailPage(t, h, here, shuffle,
                        liked = if (mix != null) "m${mix.key}" in follows else null,
                        download = if (rt == "downloads" || ids.isEmpty()) null else if (allCached) "done" else if (dl != null) "busy" else "idle",
                        onBack = back, onPlay = if (tracks.isEmpty()) null else ({ if (here) viewModel.toggleAudioPlayback() else playItems(tracks, if (shuffle) tracks.indices.random() else 0) }),
                        onShuffle = { viewModel.toggleShuffleMode() }, onLike = { mix?.let { follows = SpStore.toggle(ctx, "follows", "m${it.key}") } }, onDownload = { downloadAll(ids) },
                        pattern = rt == "recitations") {
                        if (tracks.isEmpty()) item { Text(if (rt == "liked") "Tap ⋮ → Add to Liked Surahs on any track." else "Nothing here yet.", color = t.muted, modifier = Modifier.padding(24.dp)) }
                        itemsIndexed(tracks, key = { i, it -> "$i-${it.surahId}-${it.ayah}" }) { i, it ->
                            val s = surahFor(it.surahId)
                            SpTrackRow(t, i + 1, if (it.ayah != null) it.title else s.nameEnglish,
                                if (it.ayah != null) "${s.nameEnglish} ${it.surahId}:${it.ayah} · ${selectedReciter.name}" else "${s.englishTranslation} · ${selectedReciter.name}",
                                playing = playingSurah?.id == it.surahId, onClick = { playItems(tracks, i) },
                                art = if (rt == "charts" || rt == "recitations") null else ({ m -> SpArt(m, surah = s, corner = 4.dp) }), liked = it.ayah == null && s.id in liked,
                                downloaded = it.ayah == null && s.id in cached, menu = if (it.ayah == null) surahMenu(s) else emptyList())
                        }
                        item { Spacer(Modifier.height(16.dp)); SpCredits(t) }
                    }
                }
                rt.startsWith("reciter:") -> {
                    val r = IslamicData.reciters.firstOrNull { "reciter:${it.id}" == rt } ?: selectedReciter
                    val portrait = SP_REC_PORTRAITS.getOrNull(IslamicData.reciters.indexOf(r))
                    val tracks = remember { (1..114).map(::q) }
                    val active = selectedReciter.id == r.id
                    SpDetailPage(t, spReciterHeader(r, portrait, rememberCoverColor(portrait, coverColor(r.id + 40))), isPlaying && active, shuffle, liked = "r${r.id}" in follows,
                        download = if (!active) null else if (cached.size == 114) "done" else if (dl != null) "busy" else "idle",
                        onBack = back, onPlay = {
                            if (isPlaying && active) viewModel.toggleAudioPlayback()
                            else { if (!active) viewModel.selectReciter(r); playItems(tracks, if (shuffle) tracks.indices.random() else 0) }
                        }, onShuffle = { viewModel.toggleShuffleMode() }, onLike = { follows = SpStore.toggle(ctx, "follows", "r${r.id}") },
                        onDownload = { downloadAll(SP_TOP_SURAHS.take(10)) }) {
                        item { Text("Popular", color = t.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 8.dp)) }
                        itemsIndexed(tracks, key = { _, it -> "rt${it.surahId}" }) { i, it ->
                            val s = surahFor(it.surahId)
                            SpTrackRow(t, i + 1, s.nameEnglish, "${s.englishTranslation} · ${s.versesCount} ayahs", active && playingSurah?.id == s.id,
                                onClick = { if (!active) viewModel.selectReciter(r); playItems(tracks, i) }, liked = s.id in liked, downloaded = active && s.id in cached,
                                menu = surahMenu(s, canDownload = active))
                        }
                        item { Spacer(Modifier.height(16.dp)); SpCredits(t) }
                    }
                }
                rt.startsWith("podcast:") -> {
                    val p = pods.firstOrNull { "podcast:${it.id}" == rt }
                    if (p == null) LaunchedEffect(rt) { back() }
                    else SpPodcastPage(t, p, "p${p.id}" in follows, back) { follows = SpStore.toggle(ctx, "follows", "p${p.id}") }
                }
                rt == "reciters" -> SpDetailPage(t, SpDetailHeader("Top reciters", "Renowned voices of the Qur'an", "${IslamicData.reciters.size} reciters", Color(0xFF6B4E16)) { m -> SpArt(m, tint = Color(0xFF6B4E16), label = "Reciters") },
                    false, false, null, null, back, null, {}, {}, {}) {
                    itemsIndexed(IslamicData.reciters, key = { _, r -> "ar${r.id}" }) { i, r ->
                        SpTrackRow(t, null, r.name, if (r.id == selectedReciter.id) "Reciter · selected" else "Reciter", false, { open("reciter:${r.id}") }, art = { _ -> ReciterAvatarArt(r, SP_REC_PORTRAITS.getOrNull(i), 48.dp) })
                    }
                }
                rt.startsWith("browse:") -> {
                    val kind = rt.removePrefix("browse:")
                    val list = if (kind == "Nasheeds") nasheeds else podcasts
                    val col = if (kind == "Nasheeds") Color(0xFF8D3FD1) else Color(0xFF1E5AA8)
                    SpDetailPage(t, SpDetailHeader(if (kind == "Nasheeds") "Nasheeds & Naats" else "Islamic Podcasts", "From publishers' official public RSS feeds", "${list.size} channels", col) { m -> SpArt(m, tint = col, label = kind) },
                        false, false, null, null, back, null, {}, {}, {}) {
                        // 1.0.86: featured MAF Nasheed YouTube channel at the top of Nasheeds.
                        if (kind == "Nasheeds") item(key = "maf") { Spacer(Modifier.height(8.dp)); MafNasheedShelf(t); Spacer(Modifier.height(18.dp)) }
                        items(list, key = { "b${it.id}" }) { p -> SpTrackRow(t, null, p.name, "${p.publisher} · ${p.language} · ${p.category}", false, { open("podcast:${p.id}") },
                            art = { m -> SpArt(m, url = artFor(p), tint = coverColor(p.id.hashCode()), corner = 4.dp) }, liked = "p${p.id}" in follows) }
                        item { Spacer(Modifier.height(16.dp)); SpCredits(t) }
                    }
                }
                rt.startsWith("cat:") -> {
                    val c = rt.removePrefix("cat:")
                    val col = when (c) { "Dua Audio" -> Color(0xFFD2691E); "Tafsir" -> Color(0xFFB02E6B); else -> Color(0xFF3A7D2C) }
                    SpDetailPage(t, SpDetailHeader(when (c) { "Dua Audio" -> "Dua Audio"; "Tarjuma" -> "Quran Tarjuma"; else -> "Tafsir Audio" }, "Free, licensed audio · see “Audio sources” for attribution", "Al-Noor Audio", col) { m -> SpArt(m, tint = col, label = c) },
                        false, false, null, null, back, null, {}, {}, {}) {
                        item { Box(Modifier.padding(horizontal = 16.dp)) { FreeAudioSection(c) } }
                    }
                }
                else -> LaunchedEffect(rt) { back() }
            }
        }
        SpTabBar(t, tab, Modifier.align(Alignment.BottomCenter)) { i -> tab = i; stack.clear() }
    }
}

internal fun spLibraryItems(likedCount: Int, recentCount: Int, follows: Set<String>, selectedId: Int, cachedCount: Int, reciterName: String, pods: List<Podcast>, artFor: (Podcast) -> String?): List<SpLibItem> = buildList {
    add(SpLibItem("liked", "Liked Surahs", "Playlist · $likedCount surahs", "Playlists") { m -> LikedArt(m) })
    SP_MIXES.forEach { m -> add(SpLibItem("mix:${m.key}", m.title, "Playlist · Noor Pro", "Playlists") { mod -> SpArt(mod, res = m.cover, tint = m.tint, label = if (m.cover == null) m.title else "") }) }
    add(SpLibItem("recent", "Recently played", "Playlist · $recentCount surahs", "Playlists") { m -> SpArt(m, tint = Color(0xFF283593), label = "Recent") })
    IslamicData.reciters.forEachIndexed { i, r -> if ("r${r.id}" in follows || r.id == selectedId) add(SpLibItem("reciter:${r.id}", r.name, "Reciter", "Reciters", circle = true) { m -> spReciterArt(r, SP_REC_PORTRAITS.getOrNull(i), m) }) }
    pods.filter { "p${it.id}" in follows }.forEach { p -> add(SpLibItem("podcast:${p.id}", p.name, (if (p.kind == "nasheed") "Nasheeds" else "Podcast") + " · ${p.publisher}", "Podcasts") { m -> SpArt(m, url = artFor(p), tint = coverColor(p.id.hashCode()), label = p.name) }) }
    if (cachedCount > 0) add(SpLibItem("downloads", "Downloaded surahs", "$cachedCount surahs · $reciterName", "Downloads") { m -> SpArt(m, tint = Color(0xFF00796B), label = "Offline") })
}

@Composable
internal fun spReciterArt(r: Reciter, portrait: Int?, m: Modifier) {
    if (portrait != null) Image(painterResource(portrait), r.name, contentScale = ContentScale.Crop, modifier = m.clip(CircleShape))
    else SpArt(m, tint = coverColor(r.id + 40), label = r.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }, circle = true)
}

internal fun spReciterHeader(r: Reciter, portrait: Int?, tint: Color) =
    SpDetailHeader(r.name, r.description.ifBlank { "Complete Qur'an recitation" }, "114 surahs · EveryAyah.com", tint, circle = true) { m -> spReciterArt(r, portrait, m) }

@Composable
internal fun spListHeader(rt: String, mix: SpMix?, n: Int, reciter: String): SpDetailHeader = when (rt) {
    "liked" -> SpDetailHeader("Liked Surahs", "Your favourite recitations", "$n surahs", Color(0xFF5038A0)) { m -> LikedArt(m) }
    "recent" -> SpDetailHeader("Recently played", "Jump back in", "$n surahs", Color(0xFF283593)) { m -> SpArt(m, tint = Color(0xFF283593), label = "Recent") }
    "charts" -> SpDetailHeader("Top surahs", "Charts · community favourites", "$n surahs · $reciter", Color(0xFF283593)) { m -> SpArt(m, tint = Color(0xFF283593), label = "Top\nSurahs") }
    "downloads" -> SpDetailHeader("Downloaded", "Available offline", "$n surahs · $reciter", Color(0xFF00796B)) { m -> SpArt(m, tint = Color(0xFF00796B), label = "Offline") }
    "recitations" -> SpDetailHeader("The Holy Qur'an", "All 114 surahs · $reciter", "Complete recitation · EveryAyah.com", Color(0xFF0F5D46)) { m -> SpArt(m, tint = Color(0xFF0F5D46), label = "Al-Qur'an") }
    else -> {
        val mx = mix ?: SP_MIXES[0]
        SpDetailHeader(mx.title, mx.subtitle, "Noor Pro · $n tracks", if (mx.cover != null) rememberCoverColor(mx.cover, mx.tint) else mx.tint) { m -> SpArt(m, res = mx.cover, tint = mx.tint, label = if (mx.cover == null) mx.title else "") }
    }
}

/** Podcast / nasheed show page. Streams only (publisher licence). */
@Composable
internal fun SpPodcastPage(t: SpTheme, p: Podcast, followed: Boolean, onBack: () -> Unit, onFollow: () -> Unit) {
    val ctx = LocalContext.current
    var result by remember(p.id) { mutableStateOf<FeedResult?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(p.id, reload) { result = PodcastRepository.load(ctx, p, force = reload > 0) }
    val r = result
    val art = r?.artwork ?: PodcastRepository.cachedArtwork(ctx, p)
    val tracks = remember(r) { r?.episodes?.map { PodcastRepository.toTrack(p, it, r.artwork) }.orEmpty() }
    // 1.0.83: reflect the real free-player state (was hard-coded "not playing").
    val fq by (if (AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.queue else remember { kotlinx.coroutines.flow.MutableStateFlow(com.noorpro.app.audio.models.QueueState()) }).collectAsState()
    val fpb by (if (AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.playback else remember { kotlinx.coroutines.flow.MutableStateFlow(com.noorpro.app.audio.models.PlaybackState()) }).collectAsState()
    val curId = fq.currentTrack?.id
    val here = curId != null && tracks.any { it.id == curId }
    val pagePlaying = here && fpb.isPlaying
    SpDetailPage(t, SpDetailHeader(p.name, p.publisher, "${if (p.kind == "nasheed") "Nasheeds" else "Podcast"} · ${p.language} · ${p.category}", coverColor(p.id.hashCode())) { m ->
        SpArt(m, url = art, tint = coverColor(p.id.hashCode()), label = p.name)
    }, pagePlaying, fq.shuffle && here, followed, null, onBack, if (tracks.isEmpty()) null else ({ if (here) AlNoorAudioSession.player.togglePlayPause() else playFree(ctx, tracks[0], tracks) }),
        { if (tracks.isNotEmpty()) { val i = tracks.indices.random(); playFree(ctx, tracks[i], tracks) } }, onFollow, {}) {
        when {
            r == null -> items(6) { Row(Modifier.padding(16.dp, 8.dp)) { SpShimmer(t, Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))); Spacer(Modifier.width(12.dp)); SpShimmer(t, Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(4.dp))) } }
            r.episodes.isEmpty() -> item {
                Column(Modifier.padding(24.dp)) {
                    Text("Couldn't load episodes", color = t.text, fontWeight = FontWeight.Bold)
                    Text("Check your connection. Episodes stream from the publisher's feed.", color = t.muted, fontSize = 13.sp)
                    Text("Retry", color = t.accent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { reload++ }.padding(vertical = 8.dp))
                }
            }
            else -> {
                if (r.fromCache && r.error != null) item { Text("Offline — showing saved episode list", color = t.gold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
                itemsIndexed(r.episodes, key = { i, e -> "${i}_${e.guid}" }) { i, e ->
                    val meta = listOfNotNull(e.pubDateMs.takeIf { it > 0 }?.let { java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(it)) }, e.durationMs.takeIf { it > 0 }?.let { fmt(it) }).joinToString(" · ")
                    SpTrackRow(t, i + 1, e.title, meta.ifBlank { p.publisher }, tracks[i].id == curId && fpb.isPlaying, { if (tracks[i].id == curId) AlNoorAudioSession.player.togglePlayPause() else playFree(ctx, tracks[i], tracks) },
                        art = { m -> SpArt(m, url = e.imageUrl ?: art, tint = coverColor((p.id + e.guid).hashCode()), label = p.name, corner = 4.dp) }, menu = listOf(
                        "Play" to { playFree(ctx, tracks[i], tracks) },
                        "Share" to { runCatching { ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, "${e.title} — ${p.name}\n${p.website.ifBlank { p.feedUrl }}"), "Share")) } },
                    ))
                }
            }
        }
        item {
            Text("Streams only, as the publisher's licence requires (no downloads). Source: ${p.name} (${p.publisher}) via official public RSS — ${p.website.ifBlank { p.feedUrl }}",
                color = t.muted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(16.dp))
        }
    }
}
