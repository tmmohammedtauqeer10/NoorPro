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
import androidx.compose.ui.graphics.asComposePath
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

import androidx.compose.material.icons.filled.History
import com.noorpro.app.data.*

import com.noorpro.app.audio.catalog.FreeAudioCatalog
import com.noorpro.app.audio.models.Track
import com.noorpro.app.audio.search.AudioSearchEngine
import androidx.compose.animation.animateContentSize

/** 1.0.90 unified search document. [section] decides the grouped section it is listed under. */
internal class SearchDoc(
    val section: String, val title: String, val subtitle: String, val fields: List<String>, val pop: Int = 0,
    val art: @Composable (Modifier) -> Unit, val circle: Boolean = false, val action: () -> Unit,
)

internal val SEARCH_SECTIONS = listOf("Official channels", "Bayans", "Surahs", "Reciters", "Nasheeds", "Podcasts", "Episodes", "Duas", "Tafsir", "Urdu", "Bengali", "Translations", "Playlists")
internal val SEARCH_TRENDING = listOf("Ya-Sin", "Ar-Rahman", "Al-Kahf", "Mishary", "MAF Nasheed", "Bayans", "Morning duas", "Tafsir Urdu", "Bengali", "Yaqeen")

private object RecentSearches {
    private fun p(c: android.content.Context) = c.getSharedPreferences("noor_audio_lib", android.content.Context.MODE_PRIVATE)
    fun get(c: android.content.Context): List<String> = p(c).getString("search_recent", "").orEmpty().split('\n').filter { it.isNotBlank() }
    fun add(c: android.content.Context, q: String) { val l = (listOf(q.trim()) + get(c).filterNot { it.equals(q.trim(), true) }).take(8); p(c).edit().putString("search_recent", l.joinToString("\n")).apply() }
    fun clear(c: android.content.Context) { p(c).edit().remove("search_recent").apply() }
}

/** Build the search index from every catalog we have locally (no network). */
internal fun buildSearchIndex(ctx: android.content.Context, surahs: List<Surah>, reciters: List<Reciter>, pods: List<Podcast>, artFor: (Podcast) -> String?,
                              open: (String) -> Unit, playSurah: (Surah) -> Unit): List<SearchDoc> {
    val out = ArrayList<SearchDoc>()
    fun play(tr: Track, q: List<Track>) = playFree(ctx, tr, q)
    // 1.0.93: official-channel link-outs (content not hosted; opens YouTube externally).
    com.noorpro.app.audio.catalog.OfficialChannels.all.forEach { ch ->
        out += SearchDoc("Official channels", ch.name, ch.subtitle, ch.searchFields, pop = 60, circle = true,
            art = { m -> ChannelInitials(ch, 48.dp) }) { com.noorpro.app.audio.catalog.OfficialChannels.open(ctx, ch.url) }
    }
    // 1.0.95: CC BY bayans (YouTube's embedded player).
    com.noorpro.app.audio.catalog.CcBayans.all.forEach { b ->
        out += SearchDoc("Bayans", b.title, "Bayan · ${b.channel.speaker} · ${b.topic}", b.searchFields, pop = 40,
            art = { m -> BayanThumb(b, m.clip(RoundedCornerShape(4.dp))) }) { if (!openBayanAudio(ctx, b)) bayanOpenRequest.value = b }
    }
    surahs.forEach { s -> out += SearchDoc("Surahs", s.nameEnglish, "Surah ${s.id} · ${s.englishTranslation}",
        listOf(s.nameEnglish, s.nameArabic, s.englishTranslation, s.id.toString(), "surah ${s.nameEnglish}"), pop = 120 - s.id,
        art = { m -> SpArt(m, surah = s, corner = 4.dp) }) { playSurah(s) } }
    reciters.forEachIndexed { i, r -> out += SearchDoc("Reciters", r.name, "Reciter", listOf(r.name, r.description), pop = 50 - i, circle = true,
        art = { _ -> ReciterAvatarArt(r, SP_REC_PORTRAITS.getOrNull(IslamicData.reciters.indexOf(r)), 48.dp) }) { open("reciter:${r.id}") } }
    SP_MIXES.forEach { m -> out += SearchDoc("Playlists", m.title, "Playlist · Noor Pro", listOf(m.title, m.subtitle), art = { mod -> SpArt(mod, res = m.cover, tint = m.tint) }) { open("mix:${m.key}") } }
    pods.forEach { p ->
        val sec = when { p.kind == "nasheed" -> "Nasheeds"; p.language == "Urdu" -> "Urdu"; p.language == "Bengali" -> "Bengali"; else -> "Podcasts" }
        val art = artFor(p)
        out += SearchDoc(sec, p.name, (if (p.kind == "nasheed") "Nasheeds · " else "Podcast · ") + p.publisher, listOf(p.name, p.publisher, p.category, p.language),
            pop = 10, art = { m -> SpArt(m, url = art, tint = coverColor(p.id.hashCode()), label = p.name, corner = 4.dp) }) { open("podcast:${p.id}") }
        val eps = runCatching { PodcastRepository.cachedEpisodes(ctx, p) }.getOrDefault(emptyList())
        val tracks by lazy { eps.map { PodcastRepository.toTrack(p, it, art) } }
        eps.forEachIndexed { i, e -> out += SearchDoc(if (sec == "Podcasts") "Episodes" else sec, e.title, "Episode · ${p.name}", listOf(e.title, p.name),
            art = { m -> SpArt(m, url = e.imageUrl?.takeIf { it.isNotBlank() } ?: art, tint = coverColor(p.id.hashCode()), label = p.name, corner = 4.dp) }) { play(tracks[i], tracks) } }
    }
    YtChannelRepository.cached(ctx)?.let { f ->
        f.audioOnly.forEach { a -> out += SearchDoc("Nasheeds", a.label, "New · MAF Nasheed · audio", listOf(a.title, a.titleEn, "maf nasheed", "new"), pop = 30,
            art = { m -> SpArt(m, tint = Color(0xFF5B2A86), label = a.label, corner = 4.dp) }) { val tr = a.toTrack(); play(tr, listOf(tr)) } }
        f.videos.forEach { v -> out += SearchDoc("Nasheeds", v.title, "MAF Nasheed · " + if (v.hasAudio) "audio + video" else "YouTube video", listOf(v.title, "maf nasheed"), pop = 20,
            art = { m -> SpArt(m, url = v.thumb, tint = Color(0xFF7A1F3D), corner = 4.dp) }) { mafOpenRequest.value = v } }
    }
    runCatching { FreeAudioCatalog.duaGroups(ctx) }.getOrDefault(emptyList()).forEach { g ->
        g.tracks.forEach { tr -> out += SearchDoc("Duas", tr.title, "Dua · ${g.title}", listOf(tr.title, g.title, "dua"), art = { m -> SpArt(m, tint = Color(0xFF1F5F4A), label = g.title, corner = 4.dp) }) { play(tr, g.tracks) } }
    }
    runCatching { FreeAudioCatalog.scholars(ctx) }.getOrDefault(emptyList()).forEach { sc ->
        sc.surahs.forEach { g -> val first = g.tracks.firstOrNull() ?: return@forEach
            out += SearchDoc(if (sc.language == "Urdu") "Urdu" else "Tafsir", "Tafsir · ${g.title}", "${sc.name} · ${sc.language}", listOf(g.title, sc.name, "tafsir", sc.language),
                art = { m -> SpArt(m, tint = Color(0xFF3B4A7A), label = "Tafsir", corner = 4.dp) }) { play(first, g.tracks) }
            if (sc.language == "Urdu") out += SearchDoc("Tafsir", "Tafsir · ${g.title}", "${sc.name} · Urdu", listOf(g.title, sc.name, "tafsir"),
                art = { m -> SpArt(m, tint = Color(0xFF3B4A7A), label = "Tafsir", corner = 4.dp) }) { play(first, g.tracks) }
        }
    }
    FreeAudioCatalog.editions.forEach { ed ->
        out += SearchDoc(when (ed.language) { "Urdu" -> "Urdu"; "Bengali" -> "Bengali"; else -> "Translations" }, ed.title, "${ed.language} · ${ed.subtitle}",
            listOf(ed.title, ed.subtitle, ed.language, "translation", "tarjuma"), art = { m -> SpArt(m, tint = Color(0xFF6A4A1F), label = ed.language, corner = 4.dp) }) {
            val q = ed.surahTracks(1); q.firstOrNull()?.let { play(it, q) } }
    }
    return out
}

/** Set by search to open a MAF video in the player sheet. */
internal val mafOpenRequest = mutableStateOf<YtVideo?>(null)

@Composable
internal fun SpSearchTab(t: SpTheme, query: String, onQuery: (String) -> Unit, surahs: List<Surah>, reciters: List<Reciter>, pods: List<Podcast>,
                         artFor: (Podcast) -> String?, open: (String) -> Unit, playSurah: (Surah) -> Unit, previewIndex: List<SearchDoc>? = null) {
    val ctx = LocalContext.current
    var dq by remember { mutableStateOf(query) }
    LaunchedEffect(query) { if (previewIndex == null) delay(250); dq = query }
    val index by produceState(previewIndex ?: emptyList(), pods) {
        if (previewIndex == null) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { buildSearchIndex(ctx, surahs, reciters, pods, artFor, open, playSurah) }
    }
    var recent by remember { mutableStateOf(if (previewIndex == null) RecentSearches.get(ctx) else listOf("Ya-Sin", "MAF Nasheed")) }
    val q = dq.trim()
    val grouped = remember(q, index) {
        if (q.isEmpty()) emptyMap() else AudioSearchEngine.rank(q, index, { it.fields }, { it.pop }).groupBy { it.section }
    }
    fun pick(d: SearchDoc) { if (previewIndex == null) { RecentSearches.add(ctx, q); recent = RecentSearches.get(ctx) }; d.action() }
    LazyColumn(Modifier.fillMaxSize().animateContentSize(), contentPadding = PaddingValues(bottom = 190.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(16.dp)) {
                Text("Search", color = t.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(if (t.light) Color(0xFFEDEDED) else Color.White).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Color(0xFF121212))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Surahs, reciters, nasheeds, duas…", color = Color(0xFF535353), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        BasicTextField(query, onQuery, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF121212), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            cursorBrush = SolidColor(Color(0xFF121212)), modifier = Modifier.fillMaxWidth())
                    }
                    if (query.isNotEmpty()) Icon(Icons.Default.Close, "Clear", tint = Color(0xFF121212), modifier = Modifier.clip(CircleShape).clickable { onQuery("") }.padding(4.dp))
                }
            }
        }
        if (q.isEmpty()) {
            if (recent.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent searches", color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Clear", color = t.muted, fontSize = 13.sp, modifier = Modifier.clip(CircleShape).clickable { RecentSearches.clear(ctx); recent = emptyList() }.padding(6.dp))
                    }
                }
                items(recent, key = { "rc$it" }) { r ->
                    Row(Modifier.fillMaxWidth().clickable { onQuery(r) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = t.muted, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(14.dp))
                        Text(r, color = t.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Text("Trending", color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SEARCH_TRENDING.forEach { s ->
                        Text(s, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(CircleShape).background(t.high).clickable { onQuery(s) }.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
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
        } else if (grouped.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(84.dp).clip(CircleShape)) { SpPattern(Modifier.fillMaxSize()) }
                    Spacer(Modifier.height(14.dp))
                    Text("No results for \"$q\"", color = t.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Check the spelling, or try a surah name or number, a reciter, nasheed, dua or podcast.", color = t.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SEARCH_TRENDING.take(4).forEach { s -> Text(s, color = t.text, fontSize = 13.sp, modifier = Modifier.clip(CircleShape).background(t.high).clickable { onQuery(s) }.padding(horizontal = 12.dp, vertical = 7.dp)) }
                    }
                }
            }
        } else {
            SEARCH_SECTIONS.forEach { sec ->
                val list = grouped[sec].orEmpty(); if (list.isEmpty()) return@forEach
                item(key = "h$sec") {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sec, color = t.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${list.size}", color = t.muted, fontSize = 12.sp)
                    }
                }
                if (sec == "Official channels") items(list, key = { "oc|${it.title}" }) { d ->
                    val ch = com.noorpro.app.audio.catalog.OfficialChannels.all.firstOrNull { it.name == d.title }
                    if (ch != null) OfficialChannelCard(ch, t.elevated, t.text, t.muted, Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        onOpen = { if (previewIndex == null) RecentSearches.add(ctx, q); com.noorpro.app.audio.catalog.OfficialChannels.open(ctx, it) })
                } else items(list.take(if (sec == "Surahs" || sec == "Reciters") 6 else 5), key = { "$sec|${it.title}|${it.subtitle}" }) { d ->
                    SpTrackRow(t, null, d.title, d.subtitle, false, { pick(d) }, art = d.art)
                }
            }
        }
    }
    bayanOpenRequest.value?.let { b -> BayanPlayerDialog(t, b) { bayanOpenRequest.value = null } }
    mafOpenRequest.value?.let { v -> YtPlayerDialog(t, v, YtChannelRepository.cached(ctx)?.videos.orEmpty()) { mafOpenRequest.value = null } }
}
