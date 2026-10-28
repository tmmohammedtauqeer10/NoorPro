package com.noorpro.app.ui.islamic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.session.AlNoorSessionRouter
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.audio.*
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import java.util.Calendar

/* 1.1.6 Car mode (car_mode_enabled): one screen, huge targets (≥ 76dp), minimal text, screen kept on. */

enum class CarSource { QURAN, BAYAN, NONE }

data class CarNowPlaying(val title: String, val subtitle: String, val source: CarSource, val playing: Boolean, val progress: Float = 0f)

data class CarModeUi(
    val hour: Int,
    val clock: String,
    val now: CarNowPlaying?,
    val saved: Set<Int> = emptySet(),
    val routines: List<AudioRoutine> = emptyList(),
    val resume: ResumePoint? = null,
    val bayansOn: Boolean = false,
    val autoOn: Boolean = true,
)

class CarModeActions(
    val onExit: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onForward: () -> Unit = {},
    val onSurah: (Int) -> Unit = {},
    val onRoutine: (AudioRoutine) -> Unit = {},
    val onResume: (ResumePoint) -> Unit = {},
    val onBayans: () -> Unit = {},
)

@Composable
fun CarModeScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    LaunchedEffect(Unit) { CarComponents.sync(ctx); BayanResumeTracker.start(ctx) }
    val qPlaying by viewModel.isAudioPlaying.collectAsState()
    val qSurah by viewModel.playingSurah.collectAsState()
    val active by AlNoorSessionRouter.active.collectAsState()
    val reciter by viewModel.selectedReciter.collectAsState()
    var pb by remember { mutableStateOf<Pair<com.noorpro.app.audio.models.Track?, com.noorpro.app.audio.models.PlaybackState>?>(null) }
    LaunchedEffect(Unit) {
        runCatching { AlNoorAudioSession.init(ctx) }.onFailure { return@LaunchedEffect }
        AlNoorAudioSession.player.playback.collect { pb = AlNoorAudioSession.player.queue.value.currentTrack to it }
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); tick++ } }
    val bayansOn = IslamicFlags.isOn(IslamicFlags.BAYAN_PODCASTS)
    val saved = remember(reciter.id) { OfflineQuranStore.storage(ctx, reciter).completeSurahs }
    val routines = remember { RoutineStore(ctx).all() }
    val resume = remember(tick, pb?.first?.id) { if (bayansOn) BayanLogic.inProgress(BayanResumeStore(ctx).all()).firstOrNull() else null }
    val cal = remember(tick) { Calendar.getInstance() }
    val alNoor = pb?.first
    val now = when {
        active == AlNoorSessionRouter.Source.AL_NOOR && alNoor != null -> CarNowPlaying(alNoor.title, alNoor.artistName,
            if (alNoor.id.startsWith("pod-")) CarSource.BAYAN else CarSource.NONE, pb?.second?.isPlaying == true,
            BayanLogic.progress(pb?.second?.positionMs ?: 0, pb?.second?.durationMs?.takeIf { it > 0 } ?: alNoor.durationMs))
        qSurah != null -> CarNowPlaying(qSurah!!.nameEnglish, reciter.name, CarSource.QURAN, qPlaying)
        else -> null
    }
    val ui = CarModeUi(cal.get(Calendar.HOUR_OF_DAY), "%d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)), now, saved, routines, resume, bayansOn)
    val alNoorActive = active == AlNoorSessionRouter.Source.AL_NOOR && alNoor != null
    CarModeContent(ui, CarModeActions(
        onExit = { viewModel.goBack() },
        onPlayPause = { if (alNoorActive) AlNoorAudioSession.player.togglePlayPause() else viewModel.toggleAudioPlayback() },
        onBack = { if (alNoorActive) AlNoorAudioSession.player.let { it.seekTo((it.playback.value.positionMs - 30_000).coerceAtLeast(0)) } else viewModel.playPreviousSurah() },
        onForward = { if (alNoorActive) AlNoorAudioSession.player.let { it.seekTo(it.playback.value.positionMs + 30_000) } else viewModel.playNextSurah() },
        onSurah = { n -> viewModel.playQueue(RoutineLogic.queue(AudioRoutine("car", "", listOf(n), emptySet(), 0, true, "")), 0) },
        onRoutine = { r -> viewModel.playQueue(RoutineLogic.queue(r), 0) },
        onResume = { r ->
            val p = BayanLogic.shows().firstOrNull { it.id == r.podcastId } ?: return@CarModeActions
            val e = com.noorpro.app.audio.podcast.PodcastRepository.cachedEpisodes(ctx, p).firstOrNull { BayanLogic.trackId(p, it) == r.trackId }
                ?: com.noorpro.app.audio.podcast.Episode(r.trackId, r.title, r.audioUrl, 0L, r.durationMs, r.artwork)
            val t = com.noorpro.app.audio.podcast.PodcastRepository.toTrack(p, e, r.artwork)
            com.noorpro.app.ui.screens.playFree(ctx, t, listOf(t))
            runCatching { AlNoorAudioSession.player.seekTo(BayanLogic.resumeFrom(r.positionMs, r.durationMs)) }
        },
        onBayans = { viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.BAYAN_PODCASTS) },
    ))
}

@Composable
fun CarModeContent(ui: CarModeUi, actions: CarModeActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).padding(16.dp).testTag("car_screen"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BigRound(Icons.Outlined.Close, "Exit car mode", 64.dp, ikSoft(), ikAccentText(), "car_exit", onClick = actions.onExit)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(CarModeLogic.greeting(ui.hour), color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Text("Car mode · eyes on the road", color = stitchMutedText(), fontSize = 14.sp)
            }
            Text(ui.clock, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 30.sp, modifier = Modifier.testTag("car_clock"))
        }

        IkHero(Modifier.testTag("car_now")) {
            val n = ui.now
            Text(when (n?.source) { CarSource.QURAN -> "QURAN"; CarSource.BAYAN -> "BAYAN"; else -> if (n == null) "READY" else "NOW PLAYING" },
                color = IkGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            Text(n?.title ?: "Pick something to listen to", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 28.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("car_now_title"))
            if (n != null) Text(n.subtitle, color = IkCream.copy(alpha = 0.8f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (n?.source == CarSource.BAYAN) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.2f))) {
                    Box(Modifier.fillMaxWidth(n.progress).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(IkGold))
                }
            }
            Spacer(Modifier.height(18.dp))
            val talk = n?.source == CarSource.BAYAN
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                BigRound(if (talk) Icons.Outlined.Replay30 else Icons.Outlined.SkipPrevious, if (talk) "Back 30 seconds" else "Previous surah",
                    CarModeLogic.MIN_TARGET_DP.dp, Color.White.copy(alpha = 0.14f), IkCream, "car_back", enabled = n != null, onClick = actions.onBack)
                BigRound(if (n?.playing == true) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (n?.playing == true) "Pause" else "Play",
                    104.dp, IkGold, IkGreenDeep, "car_play", enabled = n != null, onClick = actions.onPlayPause)
                BigRound(if (talk) Icons.Outlined.Forward30 else Icons.Outlined.SkipNext, if (talk) "Forward 30 seconds" else "Next surah",
                    CarModeLogic.MIN_TARGET_DP.dp, Color.White.copy(alpha = 0.14f), IkCream, "car_forward", enabled = n != null, onClick = actions.onForward)
            }
        }

        val r = ui.resume
        if (r != null) BigTile(Icons.Outlined.Podcasts, "Continue bayan", r.title + " · " + BayanLogic.remainingLabel(r.positionMs, r.durationMs), "car_resume", accent = true) { actions.onResume(r) }

        Text("QURAN", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        CarModeLogic.QUICK_SURAHS.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { s ->
                    BigTile(null, OfflineQuranLogic.surahName(s), if (s in ui.saved) "Saved · offline" else "Streams",
                        "car_surah_$s", Modifier.weight(1f)) { actions.onSurah(s) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (ui.routines.isNotEmpty()) {
            Text("MY ROUTINES", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            ui.routines.take(3).forEach { rt -> BigTile(Icons.Outlined.EventRepeat, rt.title, RoutineLogic.surahsLabel(rt), "car_routine_${rt.id}") { actions.onRoutine(rt) } }
        }
        if (ui.bayansOn) BigTile(Icons.Outlined.Podcasts, "Bayans & lectures", "Choose a show before you drive", "car_bayans") { actions.onBayans() }

        if (ui.autoOn) IkNotice("Android Auto",
            "With car mode on, NoorPro appears in your car's media apps: Quran, your routines and bayans, played with the car's own controls.",
            tag = "car_auto")
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BigRound(icon: ImageVector, label: String, size: Dp, bg: Color, fg: Color, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(if (enabled) bg else bg.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick).semantics { contentDescription = label }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) fg else fg.copy(alpha = 0.5f), modifier = Modifier.size(size * 0.46f)) }
}

@Composable
private fun BigTile(icon: ImageVector?, title: String, subtitle: String, tag: String, modifier: Modifier = Modifier, accent: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(22.dp)).background(if (accent) ikSoft() else ikCard())
            .border(1.dp, if (accent) ikAccentText().copy(alpha = 0.5f) else stitchLine(), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).padding(16.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { IkIconTile(icon, size = 48.dp); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = stitchMutedText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
