package com.noorpro.app.ui.islamic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.podcast.Episode
import com.noorpro.app.audio.podcast.EpisodeChapter
import com.noorpro.app.audio.podcast.Podcast
import com.noorpro.app.audio.podcast.PodcastRepository
import com.noorpro.app.islamic.audio.BayanChapters
import com.noorpro.app.islamic.audio.BayanLogic
import com.noorpro.app.islamic.audio.BayanResumeStore
import com.noorpro.app.islamic.audio.BayanResumeTracker
import com.noorpro.app.islamic.audio.ResumePoint
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/* 1.1.6 Bayan podcasts (bayan_podcasts_enabled): bayan/lecture shelf on top of the Al-Noor podcast reader,
 * with resume, chapters and a clear empty state. Streams only from publishers' own feeds. */

data class BayanShowFeed(val show: Podcast, val artwork: String?, val episodes: List<Episode>, val fromCache: Boolean = false, val error: String? = null)

data class BayanUi(
    val shows: List<Podcast>,
    val language: String? = null,
    val query: String = "",
    val resume: List<ResumePoint> = emptyList(),
    val open: BayanShowFeed? = null,
    val loading: Boolean = false,
    val episode: Episode? = null,
    val chapters: List<EpisodeChapter> = emptyList(),
    val nowTrackId: String? = null,
    val nowPositionMs: Long = 0L,
    val nowPlaying: Boolean = false,
)

class BayanActions(
    val onBack: () -> Unit = {},
    val onLanguage: (String?) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onOpenShow: (Podcast) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenEpisode: (Episode) -> Unit = {},
    val onPlay: (Podcast, Episode, Long) -> Unit = { _, _, _ -> },
    val onResume: (ResumePoint) -> Unit = {},
    val onForget: (ResumePoint) -> Unit = {},
    val onChapter: (EpisodeChapter) -> Unit = {},
    val onPause: () -> Unit = {},
)

@Composable
fun BayanPodcastsScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { BayanResumeStore(ctx) }
    var ui by remember { mutableStateOf(BayanUi(BayanLogic.shows(), resume = store.all())) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { BayanResumeTracker.start(ctx) }
    // Live position of the Al-Noor player (podcast episodes play there, with the shared notification).
    LaunchedEffect(Unit) {
        runCatching { AlNoorAudioSession.init(ctx) }.onFailure { return@LaunchedEffect }
        AlNoorAudioSession.player.playback.collect { pb ->
            val id = AlNoorAudioSession.player.queue.value.currentTrack?.id
            ui = ui.copy(nowTrackId = id, nowPositionMs = pb.positionMs, nowPlaying = pb.isPlaying)
        }
    }
    val openId = ui.open?.show?.id
    LaunchedEffect(openId, reload) {
        val p = ui.open?.show ?: return@LaunchedEffect
        ui = ui.copy(loading = true)
        val r = PodcastRepository.load(ctx, p, force = reload > 0)
        ui = ui.copy(loading = false, open = BayanShowFeed(p, r.artwork, r.episodes, r.fromCache, r.error))
    }
    LaunchedEffect(ui.episode?.guid) {
        val e = ui.episode ?: return@LaunchedEffect
        ui = ui.copy(chapters = BayanLogic.offlineChapters(e))
        val more = BayanChapters.load(e)
        if (ui.episode?.guid == e.guid && more.isNotEmpty()) ui = ui.copy(chapters = more)
    }
    fun play(p: Podcast, e: Episode, startMs: Long) {
        val feed = ui.open
        val tracks = (feed?.takeIf { it.show.id == p.id }?.episodes ?: listOf(e)).map { PodcastRepository.toTrack(p, it, feed?.artwork) }
        val track = PodcastRepository.toTrack(p, e, feed?.artwork)
        com.noorpro.app.ui.screens.playFree(ctx, track, tracks)
        if (startMs > 0) runCatching { AlNoorAudioSession.player.seekTo(startMs) }
        BayanResumeTracker.start(ctx)
    }
    BayanPodcastsContent(ui, BayanActions(
        onBack = { ui = when { ui.episode != null -> ui.copy(episode = null, chapters = emptyList()); ui.open != null -> ui.copy(open = null, resume = store.all()); else -> { viewModel.goBack(); ui } } },
        onLanguage = { ui = ui.copy(language = it) },
        onQuery = { ui = ui.copy(query = it) },
        onOpenShow = { ui = ui.copy(open = BayanShowFeed(it, PodcastRepository.cachedArtwork(ctx, it), emptyList()), loading = true) },
        onRetry = { reload++ },
        onOpenEpisode = { ui = ui.copy(episode = it) },
        onPlay = { p, e, start -> play(p, e, start) },
        onResume = { r ->
            val p = ui.shows.firstOrNull { it.id == r.podcastId } ?: return@BayanActions
            scope.launch {
                val eps = PodcastRepository.cachedEpisodes(ctx, p)
                val e = eps.firstOrNull { BayanLogic.trackId(p, it) == r.trackId } ?: Episode(r.trackId, r.title, r.audioUrl, 0L, r.durationMs, r.artwork)
                play(p, e, BayanLogic.resumeFrom(r.positionMs, r.durationMs))
            }
        },
        onForget = { store.remove(it.trackId); ui = ui.copy(resume = store.all()) },
        onChapter = { c ->
            val p = ui.open?.show; val e = ui.episode
            if (p != null && e != null) {
                if (ui.nowTrackId == BayanLogic.trackId(p, e)) runCatching { AlNoorAudioSession.player.seekTo(c.startMs); AlNoorAudioSession.player.play() }
                else play(p, e, c.startMs)
            }
        },
        onPause = { runCatching { AlNoorAudioSession.player.pause() } },
    ))
}

@Composable
fun BayanPodcastsContent(ui: BayanUi, actions: BayanActions, modifier: Modifier = Modifier) {
    val open = ui.open
    val ep = ui.episode
    when {
        open != null && ep != null -> EpisodeDetail(ui, open, ep, actions, modifier)
        open != null -> ShowEpisodes(ui, open, actions, modifier)
        else -> BayanHome(ui, actions, modifier)
    }
}

@Composable
private fun BayanHome(ui: BayanUi, actions: BayanActions, modifier: Modifier) {
    val list = BayanLogic.filter(ui.shows, ui.language, ui.query)
    val progress = BayanLogic.inProgress(ui.resume)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("bayan_screen")) {
        IkHeader("Bayan podcasts", "Talks and lectures from publishers' own feeds", actions.onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val top = progress.firstOrNull()
            IkHero(Modifier.testTag("bayan_hero")) {
                if (top != null) {
                    val show = ui.shows.firstOrNull { it.id == top.podcastId }
                    Text("CONTINUE LISTENING", color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(top.title, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(show?.name.orEmpty(), color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    ProgressLine(BayanLogic.progress(top.positionMs, top.durationMs), IkGold, Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.height(6.dp))
                    Text("${BayanLogic.clock(top.positionMs)} · ${BayanLogic.remainingLabel(top.positionMs, top.durationMs)}", color = IkCream.copy(alpha = 0.8f), fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    HeroButton("Resume", Icons.Outlined.PlayArrow, "bayan_resume_top") { actions.onResume(top) }
                } else {
                    Text("LISTEN AND BENEFIT", color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Beneficial talks, wherever you are", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    Text(if (ui.shows.isEmpty()) "Shows appear here once a publisher's feed is approved" else "${ui.shows.size} shows · ${BayanLogic.languages(ui.shows).size} languages · picks up where you left off",
                        color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
            if (ui.shows.isEmpty()) {
                IkNotice("No licensed bayan shows yet", "Shows appear here only when the publisher offers them as a public podcast for listening apps. Nothing is copied or rehosted.", tag = "bayan_empty")
                SourcesNote(); Spacer(Modifier.height(24.dp)); return@Column
            }
            if (progress.size > 1) {
                IkSection("Continue listening")
                progress.drop(1).take(3).forEach { r -> ResumeRow(r, ui.shows.firstOrNull { it.id == r.podcastId }, actions) }
            }
            IkField(ui.query, actions.onQuery, "Search shows, scholars, topics", tag = "bayan_search")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IkPill("All", ui.language == null, tag = "bayan_lang_all") { actions.onLanguage(null) }
                BayanLogic.languages(ui.shows).forEach { l -> IkPill(l, ui.language == l, tag = "bayan_lang_$l") { actions.onLanguage(l) } }
            }
            IkSection("Shows") { Text("${list.size}", color = stitchMutedText(), fontSize = 12.sp) }
            if (list.isEmpty()) IkNotice("Nothing matches", "Try another language or a shorter search.", tag = "bayan_no_match")
            list.forEach { p -> ShowRow(p, actions) }
            SourcesNote()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SourcesNote() = IkNotice(
    "Where these come from",
    "Every show streams from its publisher's own public podcast feed; nothing is downloaded or rehosted by NoorPro. Organisations that ask us not to offer their bayans as audio (such as Idara Islah-e-Muashra) are never listed; see their official channels instead.",
    tag = "bayan_sources",
)

@Composable
private fun ShowRow(p: Podcast, actions: BayanActions) {
    IkCard(Modifier.clickable { actions.onOpenShow(p) }.testTag("bayan_show_${p.id}"), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkIconTile(Icons.Outlined.Podcasts, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${p.publisher} · ${p.category}", color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            IkTag(p.language)
        }
    }
}

@Composable
private fun ResumeRow(r: ResumePoint, show: Podcast?, actions: BayanActions) {
    IkCard(Modifier.testTag("bayan_resume_${r.trackId}"), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.title, color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(show?.name, BayanLogic.remainingLabel(r.positionMs, r.durationMs)).joinToString(" · "), color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                ProgressLine(BayanLogic.progress(r.positionMs, r.durationMs), ikAccentText(), stitchLine())
            }
            Spacer(Modifier.width(10.dp))
            RoundIcon(Icons.Outlined.PlayArrow, "Resume", "bayan_resume_play_${r.trackId}") { actions.onResume(r) }
            Icon(Icons.Outlined.Close, "Forget", tint = stitchMutedText(), modifier = Modifier.padding(start = 4.dp).clip(CircleShape).clickable { actions.onForget(r) }.padding(6.dp).size(18.dp))
        }
    }
}

@Composable
private fun ShowEpisodes(ui: BayanUi, feed: BayanShowFeed, actions: BayanActions, modifier: Modifier) {
    val p = feed.show
    val resume = ui.resume.associateBy { it.trackId }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("bayan_show_screen")) {
        IkHeader(p.name, "${p.publisher} · ${p.language}", actions.onBack) {
            Icon(Icons.Outlined.Refresh, "Refresh", tint = stitchMutedText(), modifier = Modifier.clip(CircleShape).clickable(onClick = actions.onRetry).padding(8.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IkHero {
                Text(p.category.uppercase(), color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text(p.name, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${feed.episodes.size} episodes · publisher's public feed", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            when {
                ui.loading && feed.episodes.isEmpty() -> IkCard { Text("Loading episodes…", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.testTag("bayan_loading")) }
                feed.episodes.isEmpty() -> {
                    IkNotice("Couldn't load episodes", "Check your connection and try again. Episodes stream from ${p.publisher}'s own feed.", tag = "bayan_feed_error")
                    IkButton("Try again", icon = Icons.Outlined.Refresh, primary = false, tag = "bayan_retry", onClick = actions.onRetry)
                }
                else -> {
                    if (feed.fromCache && feed.error != null) Text("Offline · showing the saved episode list", color = IkAmber, fontSize = 12.sp, modifier = Modifier.testTag("bayan_offline"))
                    feed.episodes.forEach { e ->
                        val tid = BayanLogic.trackId(p, e)
                        val r = resume[tid]
                        val playingThis = ui.nowTrackId == tid && ui.nowPlaying
                        IkCard(Modifier.clickable { actions.onOpenEpisode(e) }.testTag("bayan_ep_${e.guid}"), padding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.title, color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    val chapters = BayanLogic.offlineChapters(e).size
                                    val meta = listOfNotNull(
                                        e.pubDateMs.takeIf { it > 0 }?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) },
                                        e.durationMs.takeIf { it > 0 }?.let { BayanLogic.clock(it) },
                                        chapters.takeIf { it > 0 }?.let { "$it chapters" },
                                    ).joinToString(" · ")
                                    if (meta.isNotEmpty()) Text(meta, color = stitchMutedText(), fontSize = 11.sp)
                                    if (r != null && r.positionMs >= BayanLogic.MIN_RESUME_MS) {
                                        Spacer(Modifier.height(6.dp))
                                        ProgressLine(BayanLogic.progress(r.positionMs, r.durationMs.takeIf { it > 0 } ?: e.durationMs), ikAccentText(), stitchLine())
                                        Text(BayanLogic.remainingLabel(r.positionMs, r.durationMs.takeIf { it > 0 } ?: e.durationMs), color = ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                if (playingThis) RoundIcon(Icons.Outlined.Pause, "Pause", "bayan_ep_pause_${e.guid}", actions.onPause)
                                else RoundIcon(Icons.Outlined.PlayArrow, "Play", "bayan_ep_play_${e.guid}") {
                                    actions.onPlay(p, e, r?.let { BayanLogic.resumeFrom(it.positionMs, it.durationMs.takeIf { d -> d > 0 } ?: e.durationMs) } ?: 0L)
                                }
                            }
                        }
                    }
                    Text("Source: ${p.publisher} · ${p.website.ifBlank { p.feedUrl }.removePrefix("https://")}", color = stitchMutedText(), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EpisodeDetail(ui: BayanUi, feed: BayanShowFeed, e: Episode, actions: BayanActions, modifier: Modifier) {
    val p = feed.show
    val tid = BayanLogic.trackId(p, e)
    val r = ui.resume.firstOrNull { it.trackId == tid }
    val dur = r?.durationMs?.takeIf { it > 0 } ?: e.durationMs
    val isCurrent = ui.nowTrackId == tid
    val pos = if (isCurrent) ui.nowPositionMs else r?.positionMs ?: 0L
    val current = if (isCurrent || r != null) BayanLogic.chapterAt(ui.chapters, pos) else -1
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("bayan_episode_screen")) {
        IkHeader("Episode", p.name, actions.onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IkHero {
                Text(p.name.uppercase(), color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(e.title, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (dur > 0) {
                    Spacer(Modifier.height(10.dp))
                    ProgressLine(BayanLogic.progress(pos, dur), IkGold, Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.height(4.dp))
                    Row { Text(BayanLogic.clock(pos), color = IkCream.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.weight(1f)); Text(BayanLogic.clock(dur), color = IkCream.copy(alpha = 0.8f), fontSize = 11.sp) }
                }
                Spacer(Modifier.height(12.dp))
                val resumeAt = r?.let { BayanLogic.resumeFrom(it.positionMs, dur) } ?: 0L
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        isCurrent && ui.nowPlaying -> HeroButton("Pause", Icons.Outlined.Pause, "bayan_detail_pause", onClick = actions.onPause)
                        resumeAt > 0 -> {
                            HeroButton("Resume at ${BayanLogic.clock(resumeAt)}", Icons.Outlined.PlayArrow, "bayan_detail_resume") { actions.onPlay(p, e, resumeAt) }
                            HeroButton("Start over", Icons.Outlined.Replay, "bayan_detail_restart", primary = false) { actions.onPlay(p, e, 0L) }
                        }
                        else -> HeroButton("Play episode", Icons.Outlined.PlayArrow, "bayan_detail_play") { actions.onPlay(p, e, 0L) }
                    }
                }
            }
            IkSection("Chapters") { if (ui.chapters.isNotEmpty()) Text("${ui.chapters.size}", color = stitchMutedText(), fontSize = 12.sp) }
            if (ui.chapters.isEmpty()) Text("This episode has no chapter markers. When a publisher adds timestamps to the notes, they show up here.",
                color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.testTag("bayan_no_chapters"))
            else IkCard(padding = 6.dp) {
                ui.chapters.forEachIndexed { i, c ->
                    val on = i == current
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) ikSoft() else Color.Transparent)
                            .clickable { actions.onChapter(c) }.padding(horizontal = 10.dp, vertical = 10.dp).testTag("bayan_chapter_$i"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(BayanLogic.clock(c.startMs), color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(56.dp))
                        Text(c.title, color = stitchText(), fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (on) Icon(Icons.Outlined.GraphicEq, "Now", tint = ikAccentText(), modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (e.notes.isNotBlank()) {
                IkSection("Notes")
                IkCard { Text(e.notes.take(700), color = stitchText(), fontSize = 13.sp, lineHeight = 19.sp) }
            }
            Text("Streams from ${p.publisher}'s public feed. Your position is saved on this phone only.", color = stitchMutedText(), fontSize = 11.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProgressLine(f: Float, fg: Color, bg: Color) {
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(bg)) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(fg))
    }
}

@Composable
private fun HeroButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, primary: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (primary) IkGold else Color.White.copy(alpha = 0.14f)).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (primary) IkGreenDeep else IkCream, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (primary) IkGreenDeep else IkCream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tag: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(IkGreen).clickable(onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}
