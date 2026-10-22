package com.noorpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.models.QueueState
import com.noorpro.app.audio.models.PlaybackState
import com.noorpro.app.audio.models.RepeatMode
import com.noorpro.app.data.Surah
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.viewmodel.AudioQueueItem
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

/** Snapshot of whichever player (Quran VM or free Al-Noor player) is active. Stateless for previews. */
data class NpState(
    val title: String, val subtitle: String, val surah: Surah?, val coverUrl: String?, val tint: Color,
    val playing: Boolean, val progress: Float, val pos: String, val dur: String,
    val shuffle: Boolean, val repeat: Boolean, val lyrics: List<Pair<String, String>>, val activeLine: Int,
    val playingFrom: String, val isFree: Boolean, val attribution: String? = null, val speed: Float = 1f,
    val sleepOn: Boolean = false, val liked: Boolean = false,
)

class NpActions(
    val toggle: () -> Unit = {}, val next: () -> Unit = {}, val prev: () -> Unit = {}, val seek: (Float) -> Unit = {},
    val shuffle: () -> Unit = {}, val repeat: () -> Unit = {}, val like: () -> Unit = {}, val share: () -> Unit = {},
    val queue: () -> Unit = {}, val sleep: (Int?) -> Unit = {}, val speed: (Float) -> Unit = {}, val close: () -> Unit = {},
)

private fun msFmt(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }

@Composable
internal fun rememberNpState(vm: DeenViewModel): Pair<NpState?, NpActions> {
    val ctx = LocalContext.current
    val surah by vm.playingSurah.collectAsState(); val track by vm.currentAudioTrack.collectAsState()
    val playing by vm.isAudioPlaying.collectAsState(); val prog by vm.audioProgress.collectAsState()
    val pos by vm.audioPosition.collectAsState(); val dur by vm.audioDuration.collectAsState()
    val sh by vm.isShuffleEnabled.collectAsState(); val rp by vm.isRepeatEnabled.collectAsState()
    val sel by vm.selectedSurah.collectAsState(); val ayah by vm.playingAyahIndex.collectAsState()
    val reciter by vm.selectedReciter.collectAsState(); val speed by vm.audioPlaybackSpeed.collectAsState()
    val sleep by vm.sleepTimerEndsAt.collectAsState()
    val init = AlNoorAudioSession.isInitialized()
    val fq by (if (init) AlNoorAudioSession.player.queue else remember { kotlinx.coroutines.flow.MutableStateFlow(QueueState()) }).collectAsState()
    val fp by (if (init) AlNoorAudioSession.player.playback else remember { kotlinx.coroutines.flow.MutableStateFlow(PlaybackState()) }).collectAsState()
    var likedSet by remember { mutableStateOf(AudioLibraryStore.liked(ctx)) }
    val ft = fq.currentTrack
    val useFree = ft != null && (fp.isPlaying || (surah == null && track == null))
    val share: (String) -> Unit = { text ->
        runCatching {
            ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text) }, "Share").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    if (useFree && ft != null) {
        val p = AlNoorAudioSession.player
        val st = NpState(ft.title, ft.artistName, null, ft.coverUrl, coverColor(ft.title.hashCode()), fp.isPlaying,
            if (fp.durationMs > 0) fp.positionMs.toFloat() / fp.durationMs else 0f, msFmt(fp.positionMs), msFmt(fp.durationMs),
            fq.shuffle, fq.repeatMode != RepeatMode.OFF, emptyList(), -1,
            if (ft.id.startsWith("pod-")) "PODCAST" else "AL-NOOR AUDIO", true, ft.attributionText.ifBlank { null })
        return st to NpActions(toggle = { p.togglePlayPause() }, next = { p.skipNext() }, prev = { p.skipPrevious() },
            seek = { f -> p.seekTo((f * fp.durationMs).toLong()) }, shuffle = { p.toggleShuffle() }, repeat = { p.cycleRepeatMode() },
            share = { share(ft.title + " — " + ft.artistName + "\n" + ft.sourceUrl) }, close = { if (fp.isPlaying) p.togglePlayPause() })
    }
    if (surah == null && track == null) return null to NpActions()
    val s = surah
    val verses = if (s != null && sel?.id == s.id) sel?.verses.orEmpty() else emptyList()
    val st = NpState(track?.title ?: s?.nameEnglish.orEmpty(), track?.artist ?: reciter.name, if (track == null) s else null, track?.coverUrl,
        coverColor(s?.id ?: 7), playing, prog, pos, dur, sh, rp, verses, ayah, if (track == null) "RECITATION" else "AL-NOOR AUDIO", false,
        speed = speed, sleepOn = sleep != null, liked = s != null && s.id in likedSet)
    return st to NpActions(toggle = { vm.toggleAudioPlayback() }, next = { vm.playNextSurah() }, prev = { vm.playPreviousSurah() },
        seek = { vm.seekAudio(it) }, shuffle = { vm.toggleShuffleMode() }, repeat = { vm.toggleRepeatMode() },
        like = { if (s != null) likedSet = AudioLibraryStore.toggleLike(ctx, s.id) },
        share = { share("Listening to " + st.title + " by " + st.subtitle + " on Noor Pro") },
        sleep = { vm.setSleepTimer(it) }, speed = { vm.setAudioPlaybackSpeed(it) }, close = { vm.dismissMiniAudioPlayer() })
}

@Composable
internal fun NpArt(s: NpState, modifier: Modifier, corner: Int = 8) {
    SpArt(modifier, surah = s.surah, url = s.coverUrl, tint = s.tint, label = if (s.surah == null && s.coverUrl == null) s.title else "", corner = corner.dp)
}

/** Spotify-style mini player: artwork tint, progress line, swipe to skip. */
@Composable
internal fun NpMini(t: SpTheme, s: NpState, a: NpActions, artModifier: Modifier, modifier: Modifier, onExpand: () -> Unit) {
    val drag = remember { androidx.compose.animation.core.Animatable(0f) }; val scope = rememberCoroutineScope()
    val bg = lerp(s.tint, if (t.light) Color.White else Color.Black, if (t.light) 0.55f else 0.45f)
    Box(modifier.fillMaxWidth().height(60.dp).graphicsLayer { translationX = drag.value }.clip(RoundedCornerShape(10.dp)).background(bg)
        .pointerInput(s.title) { detectHorizontalDragGestures(onDragEnd = {
            if (drag.value < -140f) a.next() else if (drag.value > 140f) a.prev()
            scope.launch { drag.animateTo(0f, spring(dampingRatio = 0.55f)) } }) { _, dx -> scope.launch { drag.snapTo(drag.value + dx) } } }
        .clickable(onClick = onExpand)
        .drawWithContent { drawContent(); val h = 2.dp.toPx()
            drawRect(Color.White.copy(alpha = 0.25f), Offset(8.dp.toPx(), size.height - h), Size(size.width - 16.dp.toPx(), h))
            drawRect(Color.White, Offset(8.dp.toPx(), size.height - h), Size((size.width - 16.dp.toPx()) * s.progress.coerceIn(0f, 1f), h)) }) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            NpArt(s, artModifier.size(44.dp), 4)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = a.like) { Icon(if (s.liked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Like", tint = if (s.liked) t.accent else Color.White) }
            IconButton(onClick = a.toggle) { Icon(if (s.playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(30.dp)) }
            IconButton(onClick = a.close) { Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = 0.7f)) }
        }
    }
}

/** Spotify-style full Now Playing. Stateless (preview friendly). */
@Composable
internal fun NpFull(t: SpTheme, s: NpState, a: NpActions, artModifier: Modifier, onCollapse: () -> Unit) {
    val top by animateColorAsState(lerp(s.tint, Color.Black, 0.25f), tween(600), label = "np")
    val base = if (t.light) Color(0xFF2A2A2A) else Color(0xFF121212)
    var menu by remember { mutableStateOf(false) }
    var dragY by remember { mutableStateOf(0f) }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, lerp(top, base, 0.6f), base)))
        .pointerInput(Unit) { detectVerticalDragGestures(onDragEnd = { if (dragY > 220f) onCollapse(); dragY = 0f }) { _, dy -> dragY += dy } }
        .statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCollapse) { Icon(Icons.Default.KeyboardArrowDown, "Collapse", tint = Color.White, modifier = Modifier.size(32.dp)) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PLAYING FROM " + s.playingFrom, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 1.sp)
                Text(s.subtitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            }
            Box { IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More", tint = Color.White) }
                DropdownMenu(menu, { menu = false }) {
                    if (!s.isFree) {
                        listOf(15, 30, 45, 60).forEach { m -> DropdownMenuItem({ Text("Sleep timer: $m min") }, { a.sleep(m); menu = false }, leadingIcon = { Icon(Icons.Default.Bedtime, null) }) }
                        if (s.sleepOn) DropdownMenuItem({ Text("Turn off sleep timer") }, { a.sleep(null); menu = false })
                        listOf(0.75f, 1f, 1.25f, 1.5f).forEach { v -> DropdownMenuItem({ Text("Speed ${v}x" + if (v == s.speed) "  ✓" else "") }, { a.speed(v); menu = false }, leadingIcon = { Icon(Icons.Default.Speed, null) }) }
                    }
                    DropdownMenuItem({ Text("Share") }, { a.share(); menu = false }, leadingIcon = { Icon(Icons.Default.Share, null) })
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        NpArt(s, artModifier.fillMaxWidth().aspectRatio(1f), 10)
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, maxLines = 1)
            }
            val sc by animateFloatAsState(if (s.liked) 1.15f else 1f, spring(dampingRatio = 0.4f), label = "lk")
            IconButton(onClick = a.like) { Icon(if (s.liked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Like",
                tint = if (s.liked) t.accent else Color.White, modifier = Modifier.size(28.dp).graphicsLayer { scaleX = sc; scaleY = sc }) }
        }
        Slider(s.progress.coerceIn(0f, 1f), a.seek, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)))
        Row { Text(s.pos, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text(s.dur, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp) }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = a.shuffle) { Icon(Icons.Default.Shuffle, "Shuffle", tint = if (s.shuffle) t.accent else Color.White) }
            IconButton(onClick = a.prev, Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(40.dp)) }
            Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable(onClick = a.toggle), contentAlignment = Alignment.Center) {
                AnimatedContent(s.playing, label = "pp") { p -> Icon(if (p) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.Black, modifier = Modifier.size(40.dp)) }
            }
            IconButton(onClick = a.next, Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(40.dp)) }
            IconButton(onClick = a.repeat) { Icon(Icons.Default.Repeat, "Repeat", tint = if (s.repeat) t.accent else Color.White) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Smartphone, null, tint = t.accent, modifier = Modifier.size(18.dp))
            Text("  This phone", color = t.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = a.share) { Icon(Icons.Default.Share, "Share", tint = Color.White) }
            IconButton(onClick = a.queue) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", tint = Color.White) }
        }
        Spacer(Modifier.height(12.dp))
        NpLyricsCard(s)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
internal fun NpLyricsCard(s: NpState) {
    val card = lerp(s.tint, Color.Black, 0.15f)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(card).padding(16.dp)) {
        Text(if (s.isFree) "About this track" else "Ayat & translation", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        if (s.lyrics.isEmpty()) {
            Text(s.attribution ?: "Open the surah in Quran to follow along ayah by ayah.", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        } else {
            val start = (s.activeLine - 1).coerceIn(0, (s.lyrics.size - 1).coerceAtLeast(0))
            s.lyrics.drop(start).take(4).forEachIndexed { i, (ar, en) ->
                val on = start + i == s.activeLine
                val c = if (on) Color.White else Color.White.copy(alpha = 0.45f)
                Text(ar, color = c, fontFamily = QuranArabicFont, fontSize = 22.sp, textAlign = TextAlign.End, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), modifier = Modifier.fillMaxWidth())
                Text(en, color = c, fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(bottom = 12.dp))
            }
        }
    }
}

/** Queue sheet. Quran queue: long-press drag handle to reorder; free queue: remove only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NpQueueSheet(t: SpTheme, vm: DeenViewModel, free: Boolean, onDismiss: () -> Unit) {
    LaunchedEffect(free) { if (!free) vm.ensureEditableQueue() }
    val vq by vm.queueState.collectAsState()
    val fq by (if (free && AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.queue else remember { kotlinx.coroutines.flow.MutableStateFlow(QueueState()) }).collectAsState()
    ModalBottomSheet(onDismiss, containerColor = t.elevated) {
        Text("Queue", color = t.text, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        if (free) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                itemsIndexed(fq.tracks, key = { i, tr -> tr.id + i }) { i, tr ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr.title, color = if (i == fq.currentIndex) t.accent else t.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(tr.artistName, color = t.muted, fontSize = 12.sp, maxLines = 1)
                        }
                        if (i != fq.currentIndex) IconButton(onClick = { AlNoorAudioSession.player.removeFromQueue(i) }) { Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = t.muted) }
                    }
                }
            }
        } else NpReorderList(t, vq.first, vq.second, onMove = { f, to -> vm.moveQueueItem(f, to) }, onPlay = { vm.playQueueAt(it) }, onRemove = { vm.removeQueueItem(it) })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun NpReorderList(t: SpTheme, items: List<AudioQueueItem>, current: Int, onMove: (Int, Int) -> Unit, onPlay: (Int) -> Unit, onRemove: (Int) -> Unit) {
    val rowH = 60.dp; val rowPx = with(androidx.compose.ui.platform.LocalDensity.current) { rowH.toPx() }
    var dragIndex by remember { mutableStateOf(-1) }; var dragOff by remember { mutableStateOf(0f) }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), state = rememberLazyListState()) {
        itemsIndexed(items, key = { i, it -> "${it.surahId}-${it.ayah}-$i" }) { i, it ->
            val dragging = i == dragIndex
            Row(Modifier.fillMaxWidth().height(rowH).zIndex(if (dragging) 1f else 0f)
                .graphicsLayer { translationY = if (dragging) dragOff else 0f; scaleX = if (dragging) 1.03f else 1f; scaleY = scaleX }
                .background(if (dragging) t.high else Color.Transparent).clickable { onPlay(i) }.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                SpArt(Modifier.size(44.dp), surah = surahFor(it.surahId), corner = 4.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(it.title, color = if (i == current) t.accent else t.text, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(it.arabic, color = t.muted, fontSize = 12.sp, maxLines = 1)
                }
                if (i != current) IconButton(onClick = { onRemove(i) }) { Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = t.muted) }
                Icon(Icons.Default.DragHandle, "Drag to reorder", tint = t.muted, modifier = Modifier.size(28.dp).pointerInput(items.size, i) {
                    detectDragGesturesAfterLongPress(onDragStart = { dragIndex = i; dragOff = 0f },
                        onDragEnd = { val to = (dragIndex + Math.round(dragOff / rowPx)).coerceIn(0, items.size - 1); if (to != dragIndex) onMove(dragIndex, to); dragIndex = -1; dragOff = 0f },
                        onDragCancel = { dragIndex = -1; dragOff = 0f }) { ch, d -> ch.consume(); dragOff += d.y }
                })
            }
        }
    }
}

/** App-wide host: mini player that shared-element-expands into the full player. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlNoorPlayerHost(vm: DeenViewModel, visible: Boolean, bottomPadding: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val t = rememberSpTheme()
    val (s, a) = rememberNpState(vm)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var queue by remember { mutableStateOf(false) }
    if (s == null || (!visible && !expanded)) { return }
    BackHandler(expanded) { expanded = false }
    val acts = NpActions(a.toggle, a.next, a.prev, a.seek, a.shuffle, a.repeat, a.like, a.share, { queue = true }, a.sleep, a.speed, a.close)
    SharedTransitionLayout(modifier.fillMaxSize()) {
        AnimatedContent(expanded, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "np-host") { ex ->
            val art = Modifier.sharedElement(rememberSharedContentState("np-art"), this@AnimatedContent)
            if (ex) NpFull(t, s, acts, art) { expanded = false }
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                NpMini(t, s, acts, art, Modifier.navigationBarsPadding().padding(bottom = bottomPadding, start = 8.dp, end = 8.dp)) { expanded = true }
            }
        }
    }
    if (queue) NpQueueSheet(t, vm, s.isFree) { queue = false }
}

/** NOW_PLAYING route (deep links / notification). */
@Composable
fun AlNoorNowPlayingScreen(vm: DeenViewModel) {
    val t = rememberSpTheme(); val (s, a) = rememberNpState(vm)
    var queue by remember { mutableStateOf(false) }
    val back = { vm.goBack(); Unit }
    BackHandler { back() }
    if (s == null) { Box(Modifier.fillMaxSize().background(t.bg), contentAlignment = Alignment.Center) { Text("Nothing playing", color = t.muted) }; return }
    NpFull(t, s, NpActions(a.toggle, a.next, a.prev, a.seek, a.shuffle, a.repeat, a.like, a.share, { queue = true }, a.sleep, a.speed, a.close), Modifier, back)
    if (queue) NpQueueSheet(t, vm, s.isFree) { queue = false }
}
