package com.noorpro.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.data.UmmahSavedCollection
import com.noorpro.app.data.creator.RefCheck
import com.noorpro.app.data.creator.ReferenceChecker
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/*
 * 1.1.5 Reels v2 — the live screen. Real feed / follows / likes / saves / comments / reports from
 * UmmahRepository, one ExoPlayer for the pager (same tuned buffers + HLS↔MP4 fallback as the old
 * screen). Behind Reels2Flags.REELS_V2 (MainActivity falls back to StitchReelsScreen when off).
 */

private enum class R2Sheet { COMMENTS, MENU, REPORT, WHY, AI, VERIFY, COLLECTION, SOUND, SEARCH }

/** Local watch progress for "Continue watching" (device only; no backend). */
internal class ReelWatchStore(context: Context) {
    private val prefs = context.getSharedPreferences("noor_reels2_progress", Context.MODE_PRIVATE)
    fun all(): List<WatchProgress> = prefs.all.mapNotNull { (k, v) ->
        runCatching { val o = JSONObject(v as String); WatchProgress(k, o.getLong("p"), o.getLong("d"), o.getLong("t")) }.getOrNull()
    }
    fun get(id: String): WatchProgress? = all().firstOrNull { it.postId == id }
    fun save(p: WatchProgress) {
        if (p.postId.isBlank() || p.durationMs <= 0) return
        prefs.edit().putString(p.postId, JSONObject().put("p", p.positionMs).put("d", p.durationMs).put("t", p.updatedAt).toString()).apply()
        // keep the 40 most recent
        val extra = all().sortedByDescending { it.updatedAt }.drop(40)
        if (extra.isNotEmpty()) prefs.edit().apply { extra.forEach { remove(it.postId) } }.apply()
    }
}

/** Halal reaction chosen per reel, kept on device until reactions storage exists (flag). */
internal class ReelReactionStore(context: Context) {
    private val prefs = context.getSharedPreferences("noor_reels2_reactions", Context.MODE_PRIVATE)
    fun get(id: String): HalalReaction? = prefs.getString(id, null)?.let { n -> HalalReaction.entries.firstOrNull { it.name == n } }
    fun set(id: String, r: HalalReaction?) { prefs.edit().apply { if (r == null) remove(id) else putString(id, r.name) }.apply() }
}

internal fun myCityFrom(location: String): String =
    location.takeIf { it.isNotBlank() && !it.startsWith("Locating") }?.split(',')?.lastOrNull()?.trim().orEmpty()

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun NoorReels2Screen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val features = remember { Reels2Features() }
    var feedRefreshKey by remember { mutableIntStateOf(0) }
    val feedState = rememberStitchUmmahFeed(feedRefreshKey)
    val following = rememberStitchFollowing(viewModel.isLoggedIn)
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    val profiles = rememberStitchProfiles()
    val repo = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { repo.currentUserUid().orEmpty() }
    val feedbackStore = remember(currentUid) { ReelFeedbackStore(context.applicationContext, currentUid) }
    var reelFeedback by remember(feedbackStore) { mutableStateOf(feedbackStore.load()) }
    val watchStore = remember { ReelWatchStore(context.applicationContext) }
    val reactionStore = remember { ReelReactionStore(context.applicationContext) }
    val location by viewModel.currentLocationName.collectAsState()
    val myCity = myCityFrom(location)

    var tab by rememberSaveable { mutableStateOf(Reels2Tab.FOR_YOU) }
    var topic by rememberSaveable { mutableStateOf("All") }
    var userPaused by remember { mutableStateOf(false) }
    var holdPaused by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(Reels2Overlay.NONE) }
    var captionsOn by rememberSaveable { mutableStateOf(false) }
    var captionLang by rememberSaveable { mutableStateOf("EN") }
    var captionLine by remember { mutableStateOf("") }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var landscape by remember { mutableStateOf(false) }
    var expandedCaption by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<R2Sheet?>(null) }
    var sheetPost by remember { mutableStateOf<UmmahPost?>(null) }
    var optimisticFollowing by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var optimisticLikes by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var optimisticSaves by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var optimisticLikeCounts by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var optimisticBase by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var optimisticCommentCounts by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var reactions by remember { mutableStateOf<Map<String, HalalReaction?>>(emptyMap()) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var measuredAspects by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var hlsFailedIds by remember { mutableStateOf(emptySet<String>()) }
    var mp4FailedIds by remember { mutableStateOf(emptySet<String>()) }
    var autoRetried by remember { mutableStateOf(emptySet<String>()) }
    var playbackErrorId by remember { mutableStateOf<String?>(null) }
    var retryToken by remember { mutableIntStateOf(0) }
    var viewedIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingSeek by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val exploreQueue = viewModel.reelQueue
    val requestedId = viewModel.reelFocusId.trim()
    val allReels = remember(feedState.posts, viewModel.reelFocusPost) {
        (feedState.posts + listOfNotNull(viewModel.reelFocusPost).filter { f -> feedState.posts.none { it.id == f.id } })
            .filter { it.opensInVideoViewer() }
    }
    val ranked = remember(allReels, tab, topic, following, interactions.liked, interactions.saved, reelFeedback, myCity, exploreQueue) {
        if (exploreQueue.isNotEmpty()) return@remember exploreQueue.filter { it.opensInVideoViewer() && it.id !in reelFeedback.hiddenReelIds }
        val list = reels2Feed(tab, topic, allReels, following, myCity, reelFeedback.hiddenReelIds)
        if (tab == Reels2Tab.FOR_YOU) rankUmmahPosts(list, following, interactions.liked, interactions.saved, reelFeedback) else list
    }
    // Keep the order stable while watching (a live like/count update must not reshuffle pages).
    var stableIds by remember(feedRefreshKey, tab, topic, currentUid) { mutableStateOf(emptyList<String>()) }
    val orderedIds = reelOrderWithFocus(stableIds, ranked.map { it.id }, requestedId)
    SideEffect { if (stableIds != orderedIds) stableIds = orderedIds }
    val byId = remember(ranked) { ranked.associateBy { it.id } }
    val reels = remember(orderedIds, byId) { orderedIds.mapNotNull(byId::get) }

    val pager = rememberPagerState(pageCount = { reels.size })
    val activePage = pager.settledPage
    val activePost = reels.getOrNull(activePage)

    fun setLandscape(on: Boolean) {
        landscape = on
        // Rotate the Activity (manifest configChanges keep it + the player alive), so the same
        // ExoPlayer keeps playing from the same position in both orientations.
        activity?.requestedOrientation = if (on) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewModel.setUmmahReelsImmersive(on)
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            viewModel.setUmmahReelsImmersive(false)
            viewModel.setModalOverlayActive(false)
            viewModel.reelQueue = emptyList()
        }
    }
    BackHandler(enabled = landscape) { setLandscape(false) }
    BackHandler(enabled = !landscape && overlay != Reels2Overlay.NONE) { overlay = Reels2Overlay.NONE }

    val player = remember {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 30_000, 600, 1_200).build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(com.noorpro.app.data.media.ReelCache.dataSourceFactory(context)))
            .build().apply { repeatMode = Player.REPEAT_MODE_ONE; playWhenReady = false }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    val alNoorAudible by com.noorpro.app.audio.AlNoorAudioSession.also { it.init(context) }.anyAudible.collectAsState()
    LaunchedEffect(alNoorAudible) { if (alNoorAudible) player.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (!userPaused && !holdPaused && !alNoorAudible && sheet == null) player.play() }
    LaunchedEffect(speed) { player.setPlaybackSpeed(speed) }
    LaunchedEffect(captionsOn) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !captionsOn)
            .setSelectUndeterminedTextLanguage(captionsOn).build()
        if (!captionsOn) captionLine = ""
    }
    // A sheet over the reel pauses it (like a real player), and resumes when closed.
    LaunchedEffect(sheet) {
        viewModel.setModalOverlayActive(sheet != null)
        if (sheet != null) player.pause() else if (!userPaused && !holdPaused && !alNoorAudible && activePost != null) player.play()
    }

    // Load the active reel (HLS preferred when geometry is trusted; MP4 fallback and back).
    val useMp4 = activePost != null && activePost.id in hlsFailedIds
    val useHls = activePost != null && activePost.id in mp4FailedIds
    LaunchedEffect(activePost?.id, useMp4, useHls, retryToken) {
        val post = activePost ?: run { player.clearMediaItems(); return@LaunchedEffect }
        if (landscape && !reelFrameFor(post.mediaWidth, post.mediaHeight, post.mediaRotationDegrees).showFullScreenPill &&
            (measuredAspects[post.id] ?: 0f) < 1.15f) setLandscape(false)
        overlay = Reels2Overlay.NONE; holdPaused = false; userPaused = false; expandedCaption = null
        val trusted = uploadedReelAspectRatio(post.mediaWidth, post.mediaHeight, post.mediaRotationDegrees) != null
        val url = reelPlaybackUrl(post.mediaUrl, post.hlsUrl, trusted, post.id in hlsFailedIds, post.id in mp4FailedIds)
        if (url.isBlank()) { player.clearMediaItems(); return@LaunchedEffect }
        player.setMediaItem(MediaItem.Builder().setUri(Uri.parse(url)).setMediaId(post.id).build())
        player.prepare()
        val resume = pendingSeek?.takeIf { it.first == post.id }?.second ?: 0L
        pendingSeek = null
        player.seekTo(resume)
        if (!alNoorAudible && sheet == null) player.play()
    }
    // Prefetch the next 2 reels.
    LaunchedEffect(activePage, reels.size) {
        com.noorpro.app.data.media.ReelCache.prefetch(context, (1..2).mapNotNull { reels.getOrNull(activePage + it)?.mediaUrl })
    }
    // Position for the scrubber, chapters, recitation captions + local "Continue watching".
    LaunchedEffect(activePost?.id) {
        val id = activePost?.id ?: return@LaunchedEffect
        var tick = 0
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0)
            durationMs = player.duration.takeIf { it > 0 } ?: 0L
            if (++tick % 20 == 0 && durationMs > 0) watchStore.save(WatchProgress(id, positionMs, durationMs, System.currentTimeMillis()))
            delay(250)
        }
    }
    // A view counts after ~2.5 s on screen, once per user per reel (server receipt).
    LaunchedEffect(activePost?.id) {
        val post = activePost ?: return@LaunchedEffect
        if (post.id.isBlank() || post.id in viewedIds) return@LaunchedEffect
        if (com.noorpro.app.data.media.ReelViewLedger.hasViewed(context, currentUid, post.id)) { viewedIds = viewedIds + post.id; return@LaunchedEffect }
        delay(REEL_VIEW_THRESHOLD_MS)
        repo.recordUniqueView(post.id, REEL_VIEW_THRESHOLD_MS) { r ->
            if (r != com.noorpro.app.data.ReelViewResult.FAILED) {
                viewedIds = viewedIds + post.id
                com.noorpro.app.data.media.ReelViewLedger.markViewed(context, currentUid, post.id)
            }
        }
    }
    // Decoded frame size (the truth for 16:9 vs 9:16) and playback errors.
    val activeState = rememberUpdatedState(activePost)
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                val post = activeState.value ?: return
                if (player.currentMediaItem?.mediaId != post.id) return
                val stream = displayedReelAspectRatio(videoSize.width, videoSize.height, videoSize.pixelWidthHeightRatio)
                val source = uploadedReelAspectRatio(post.mediaWidth, post.mediaHeight, post.mediaRotationDegrees)
                val playingHls = player.currentMediaItem?.localConfiguration?.uri?.toString() == post.hlsUrl
                if (playingHls && shouldFallbackFromPaddedHls(source, stream) && post.id !in hlsFailedIds && post.id !in mp4FailedIds) {
                    hlsFailedIds = hlsFailedIds + post.id; return
                }
                val a = stream.takeIf { it.isFinite() && it > 0f } ?: source ?: 0f
                if (a > 0f) measuredAspects = measuredAspects + (post.id to a)
            }
            override fun onCues(cueGroup: CueGroup) {
                captionLine = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n")
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && player.currentMediaItem?.mediaId == activeState.value?.id) playbackErrorId = null
            }
            override fun onPlayerError(error: PlaybackException) {
                val post = activeState.value ?: return
                if (player.currentMediaItem?.mediaId != post.id) return
                val failed = player.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
                val http = (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode
                when {
                    failed == post.hlsUrl && post.id !in hlsFailedIds -> if (post.mediaUrl.isNotBlank()) hlsFailedIds = hlsFailedIds + post.id else playbackErrorId = post.id
                    failed == post.mediaUrl && post.hlsUrl.isNotBlank() && post.id !in hlsFailedIds && post.id !in mp4FailedIds -> mp4FailedIds = mp4FailedIds + post.id
                    isTransientReelError(error.errorCode, http) && post.id !in autoRetried -> { autoRetried = autoRetried + post.id; player.prepare() }
                    else -> playbackErrorId = post.id
                }
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }
    // Opened from a profile / share / search: jump to that exact reel.
    LaunchedEffect(orderedIds.joinToString("|"), requestedId) {
        if (requestedId.isBlank()) return@LaunchedEffect
        requestedReelIndex(reels.map { it.id }, requestedId)?.let { i ->
            pager.scrollToPage(i); if (viewModel.reelFocusId == requestedId) viewModel.reelFocusId = ""
        } ?: run {
            if (!feedState.loading) { delay(2_500); if (viewModel.reelFocusId == requestedId) { viewModel.reelFocusId = ""; Toast.makeText(context, "This reel is not available yet.", Toast.LENGTH_SHORT).show() } }
        }
    }

    fun requireLogin(what: String): Boolean {
        if (viewModel.isLoggedIn) return true
        Toast.makeText(context, "Sign in to $what", Toast.LENGTH_SHORT).show(); return false
    }
    fun isLiked(p: UmmahPost) = optimisticLikes[p.id] ?: (p.id in interactions.liked)
    fun isSaved(p: UmmahPost) = optimisticSaves[p.id] ?: (p.id in interactions.saved)
    fun likeCount(p: UmmahPost) = displayedEngagementCount(p.likeCount, isLiked(p), optimisticLikeCounts[p.id], optimisticBase["like:${p.id}"])
    fun setLike(p: UmmahPost, next: Boolean) {
        if (!requireLogin("like reels")) return
        val cur = isLiked(p); if (cur == next) return
        val prevCount = likeCount(p)
        optimisticLikes = optimisticLikes + (p.id to next)
        optimisticBase = optimisticBase + ("like:${p.id}" to p.likeCount)
        optimisticLikeCounts = optimisticLikeCounts + (p.id to nextEngagementCount(prevCount, next))
        repo.toggleInteraction(p.id, "likes", next) { ok ->
            if (!ok) { optimisticLikes = optimisticLikes + (p.id to cur); optimisticLikeCounts = optimisticLikeCounts + (p.id to prevCount); Toast.makeText(context, "Unable to update like", Toast.LENGTH_SHORT).show() }
            else if (next) reelFeedback = feedbackStore.recordPositive(p, 0.25f)
        }
    }
    fun setSaved(p: UmmahPost, next: Boolean, onDone: (Boolean) -> Unit = {}) {
        if (!requireLogin("save reels")) return
        val cur = isSaved(p); if (cur == next) return onDone(true)
        optimisticSaves = optimisticSaves + (p.id to next)
        repo.toggleInteraction(p.id, "saved", next) { ok ->
            if (!ok) { optimisticSaves = optimisticSaves + (p.id to cur); Toast.makeText(context, "Unable to update saved", Toast.LENGTH_SHORT).show() }
            else if (next) reelFeedback = feedbackStore.recordPositive(p, 0.45f)
            onDone(ok)
        }
    }
    fun openSheet(s: R2Sheet, p: UmmahPost) { sheetPost = p; sheet = s; overlay = Reels2Overlay.NONE }
    fun goTo(post: UmmahPost) {
        val i = reels.indexOfFirst { it.id == post.id }
        if (i >= 0) scope.launch { pager.animateScrollToPage(i) } else { viewModel.reelFocusPost = post; viewModel.reelFocusId = post.id }
    }

    val navVisible = !landscape
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CompositionLocalProvider(LocalReels2RemoteImages provides true) {
            when {
                feedState.loading -> StitchReelSkeleton()
                feedState.error != null && reels.isEmpty() -> Reels2Empty("Unable to load reels", feedState.error.orEmpty()) { feedRefreshKey++ }
                reels.isEmpty() -> Reels2Empty(
                    when (tab) { Reels2Tab.FOLLOWING -> "No reels from people you follow yet"; Reels2Tab.NEARBY -> if (myCity.isBlank()) "Turn on location for Nearby" else "No reels near $myCity yet"; else -> "No reels yet" },
                    if (topic != "All") "Try another topic, or clear “$topic”." else "Upload the first Islamic reminder reel for the Ummah.",
                    action = if (topic != "All") "Show all" else "Create a reel",
                ) { if (topic != "All") topic = "All" else viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL) }
                else -> VerticalPager(
                    state = pager, modifier = Modifier.fillMaxSize(), key = { reels.getOrNull(it)?.id ?: "stale-$it" }, beyondViewportPageCount = 1,
                    flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(state = pager, snapPositionalThreshold = 0.25f),
                ) { page ->
                    val post = reels.getOrNull(page) ?: return@VerticalPager
                    val active = page == activePage
                    val creator = profiles[post.creatorUid]
                    val refs = remember(post.id) { runCatching { ReferenceChecker.find(post.caption + "\n" + post.sourceReference) }.getOrDefault(emptyList()) }
                    val next = if (features.series) remember(post.id, allReels) { nextSeriesPart(post, allReels) } else null
                    val ui = Reels2Ui(
                        post = post, liked = isLiked(post), saved = isSaved(post),
                        following = optimisticFollowing[post.creatorUid] ?: (post.creatorUid in following),
                        own = currentUid.isNotBlank() && post.creatorUid == currentUid, verified = creator?.verified == true,
                        reaction = if (features.reactions) (reactions[post.id] ?: reactionStore.get(post.id)) else null,
                        likeCount = likeCount(post),
                        commentCount = displayedEngagementCount(post.commentCount, false, optimisticCommentCounts[post.id], optimisticBase["comment:${post.id}"]),
                        positionMs = if (active) positionMs else 0, durationMs = if (active) durationMs else 0,
                        speed = speed, captionsOn = captionsOn, captionLang = captionLang, captionLine = if (active) captionLine else "",
                        soundTitle = remember(post.id, post.sourceReference) { com.noorpro.app.data.media.edit.ReelSoundTag.parse(post.sourceReference, post.creatorName).display }, nextPart = next, refCount = refs.size,
                        measuredAspect = measuredAspects[post.id] ?: 0f,
                    )
                    val actions = Reels2Actions(
                        onLike = { setLike(post, !isLiked(post)) },
                        onReact = { r ->
                            reactions = reactions + (post.id to r); reactionStore.set(post.id, r)
                            if (reactionCountsAsLike(r)) setLike(post, true); overlay = Reels2Overlay.NONE
                        },
                        onReactions = { overlay = Reels2Overlay.REACTIONS },
                        onComment = { if (requireLogin("comment")) openSheet(R2Sheet.COMMENTS, post) },
                        onSave = { if (isSaved(post)) setSaved(post, false) else openSheet(R2Sheet.COLLECTION, post) },
                        onShare = { viewModel.sharePost = post },
                        onRemix = { if (requireLogin("remix")) { Toast.makeText(context, "Make your own reel inspired by ${post.creatorName}", Toast.LENGTH_SHORT).show(); viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL) } },
                        onSound = { if (features.soundPages) openSheet(R2Sheet.SOUND, post) else viewModel.openCreatorProfile(post.creatorUid, post.creatorName, post.creatorHandle) },
                        onFollow = {
                            when {
                                post.creatorUid.isBlank() -> Toast.makeText(context, "This creator cannot be followed yet", Toast.LENGTH_SHORT).show()
                                !requireLogin("follow creators") -> Unit
                                else -> {
                                    val cur = optimisticFollowing[post.creatorUid] ?: (post.creatorUid in following)
                                    optimisticFollowing = optimisticFollowing + (post.creatorUid to !cur)
                                    repo.setFollowing(creatorUid = post.creatorUid, creatorName = post.creatorName, creatorHandle = post.creatorHandle, following = !cur) { ok ->
                                        if (!ok) { optimisticFollowing = optimisticFollowing + (post.creatorUid to cur); Toast.makeText(context, "Unable to update follow", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            }
                        },
                        onProfile = { viewModel.openCreatorProfile(post.creatorUid, post.creatorName, post.creatorHandle) },
                        onDoubleTap = { setLike(post, true) },
                        onTogglePlay = {
                            when {
                                overlay == Reels2Overlay.HOLD -> { overlay = Reels2Overlay.NONE; holdPaused = false; if (!userPaused) player.play() }
                                overlay == Reels2Overlay.REACTIONS -> overlay = Reels2Overlay.NONE
                                playbackErrorId == post.id -> { playbackErrorId = null; hlsFailedIds = hlsFailedIds - post.id; mp4FailedIds = mp4FailedIds - post.id; autoRetried = autoRetried - post.id; retryToken++ }
                                else -> { userPaused = !userPaused; if (userPaused) player.pause() else player.play() }
                            }
                        },
                        onHold = { on -> if (on) { holdPaused = true; player.pause(); overlay = Reels2Overlay.HOLD } },
                        onSpeed = { speed = nextReelSpeed(speed) },
                        onCaptions = { captionsOn = !captionsOn },
                        onCaptionLang = { captionLang = it },
                        onFullScreen = { setLandscape(true) },
                        onSeries = { next?.let { goTo(it) } },
                        onNextPart = { next?.let { goTo(it) } },
                        onVerify = { openSheet(R2Sheet.VERIFY, post) },
                        onAskAi = { openSheet(R2Sheet.AI, post) },
                        onMenu = { openSheet(R2Sheet.MENU, post) },
                        onSeek = { player.seekTo(it); positionMs = it },
                    )
                    val frame = ui.frame
                    Box(Modifier.fillMaxSize()) {
                        Reels2Page(
                            ui = ui, actions = actions,
                            background = { m -> Reels2Thumb(post, null, m) },
                            foreground = { m ->
                                Box(m) {
                                    Reels2Thumb(post, null, Modifier.matchParentSize(), if (frame.mode == ReelFrameMode.FULL_BLEED && frame.aspect > 0f) ContentScale.Crop else ContentScale.Fit)
                                    if (active && !landscape) ReelPlayerSurface(player, fit = frame.mode == ReelFrameMode.FIT_BLUR || frame.aspect <= 0f, Modifier.matchParentSize())
                                }
                            },
                            overlay = if (active) overlay else Reels2Overlay.NONE,
                            features = features,
                            captionExpanded = expandedCaption == post.id,
                            onExpandCaption = { expandedCaption = if (expandedCaption == post.id) null else post.id },
                            recitationIndex = if (active && isRecitationReel(post)) ayahIndexAt(positionMs, durationMs, splitAyahs(post.arabicText).size) else -1,
                            bottomInset = if (navVisible) 84.dp else 0.dp,
                            aboveInfo = if (next != null && ui.series != null && active && durationMs > 0 && positionMs > durationMs * 0.6) {
                                { Reels2NextPartCard(next, ui.series!!, null, onOpen = { goTo(next) }) }
                            } else null,
                        )
                        if (active && playbackErrorId == post.id) Text("Couldn't play this reel · tap to retry", color = Color.White, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(999.dp)).background(Color(0x99000000)).padding(horizontal = 16.dp, vertical = 10.dp))
                        if (active && userPaused && overlay == Reels2Overlay.NONE) androidx.compose.material3.Icon(
                            Icons.Filled.PlayArrow, "Play", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.align(Alignment.Center).size(84.dp))
                    }
                }
            }
            if (!landscape) {
                Reels2TopBar(tab, { tab = it; viewModel.reelQueue = emptyList() }, topic, { topic = it },
                    onSearch = { activePost?.let { sheetPost = it }; sheet = R2Sheet.SEARCH },
                    onCreate = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL) }, modifier = Modifier.align(Alignment.TopCenter))
                if (exploreQueue.isNotEmpty()) Text("Search results · ${exploreQueue.size}  ✕", color = StitchGold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 118.dp).clip(RoundedCornerShape(999.dp))
                        .background(Color(0x99000000)).clickable { viewModel.reelQueue = emptyList() }.padding(horizontal = 14.dp, vertical = 6.dp))
            }

            // Landscape player: same ExoPlayer → same position; swipe up for the next video.
            if (landscape && activePost != null) {
                val nextVideo = reels.getOrNull(activePage + 1)
                val ui = Reels2Ui(activePost, positionMs = positionMs, durationMs = durationMs, speed = speed, captionsOn = captionsOn,
                    captionLine = captionLine, measuredAspect = measuredAspects[activePost.id] ?: 0f)
                var drag by remember { mutableFloatStateOf(0f) }
                Box(Modifier.fillMaxSize().pointerInput(nextVideo?.id) {
                    detectVerticalDragGestures(onDragStart = { drag = 0f }, onVerticalDrag = { _, d -> drag += d },
                        onDragEnd = { if (drag < -120f && nextVideo != null) scope.launch { pager.scrollToPage(activePage + 1) } })
                }) {
                    Reels2LandscapePlayer(
                        ui = ui, video = { m -> ReelPlayerSurface(player, fit = true, m) }, onBack = { setLandscape(false) },
                        onPlayPause = { userPaused = !userPaused; if (userPaused) player.pause() else player.play() }, playing = !userPaused,
                        onSpeed = { speed = nextReelSpeed(speed) }, onCaptions = { captionsOn = !captionsOn },
                        onSeek = { player.seekTo(it); positionMs = it }, next = nextVideo,
                        onNext = { scope.launch { pager.scrollToPage(activePage + 1) } },
                    )
                    if (captionsOn && captionLine.isNotBlank()) Text(captionLine, color = Color.White, fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xAA000000)).padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }

        val sp = sheetPost
        val open = sheet
        if (open != null && sp != null) {
            val state = rememberModalBottomSheetState(skipPartiallyExpanded = open != R2Sheet.SEARCH)
            ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = state, dragHandle = null, containerColor = stitchBackground(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                when (open) {
                    R2Sheet.COMMENTS -> CommentsHost(sp, repo, features, viewModel) {
                        optimisticBase = optimisticBase + ("comment:${sp.id}" to sp.commentCount)
                        optimisticCommentCounts = optimisticCommentCounts + (sp.id to ((optimisticCommentCounts[sp.id] ?: sp.commentCount) + 1))
                    }
                    R2Sheet.MENU, R2Sheet.REPORT -> Reels2LongPressMenu(
                        onNotInterested = { reelFeedback = feedbackStore.markNotInterested(sp); sheet = null; Toast.makeText(context, "You'll see fewer reels like this", Toast.LENGTH_SHORT).show() },
                        onWhy = { sheet = R2Sheet.WHY },
                        onWatchLater = { sheet = R2Sheet.COLLECTION },
                        onReport = { reason ->
                            if (requireLogin("report")) repo.reportContent("reel", sp.id, sp.creatorUid, reason, sp.caption.take(300)) { ok ->
                                Toast.makeText(context, if (ok) "Thanks — Halal Guard will review this reel" else "Couldn't send the report", Toast.LENGTH_SHORT).show()
                            }
                            sheet = null
                        },
                        showReasons = open == R2Sheet.REPORT, onShowReasons = { sheet = R2Sheet.REPORT },
                    )
                    R2Sheet.WHY -> Reels2WhySheet(
                        whyThisReel(sp, following, reelFeedback.interestedCategories.flatMap { c -> reels2Topics.filter { it.equals(c, true) } }.toSet() +
                            reels2Topics.drop(1).filter { t -> allReels.any { (it.id in interactions.liked || it.id in interactions.saved) && matchesReelTopic(it, t) } }, myCity),
                        onManage = { reelFeedback = feedbackStore.reset(); sheet = null; Toast.makeText(context, "For You has been reset", Toast.LENGTH_SHORT).show() })
                    R2Sheet.AI -> AskAiHost(sp)
                    R2Sheet.VERIFY -> VerifyHost(sp)
                    R2Sheet.COLLECTION -> CollectionHost(sp, isSaved(sp), { p, done -> setSaved(p, true, done) }) { sheet = null }
                    R2Sheet.SOUND -> {
                        // 1.1.9: real sound pages — reels grouped by the sound credit they were shared with.
                        val tag = remember(sp.id) { com.noorpro.app.data.media.edit.ReelSoundTag.parse(sp.sourceReference, sp.creatorName) }
                        val same = remember(sp.id, allReels) {
                            allReels.filter { r ->
                                val t = com.noorpro.app.data.media.edit.ReelSoundTag.parse(r.sourceReference, r.creatorName)
                                if (tag.credit.isBlank()) t.credit.isBlank() && r.creatorUid == sp.creatorUid else t.key == tag.key
                            }
                        }
                        val library = remember { com.noorpro.app.data.media.edit.ReelSoundLibrary.load(context) }
                        val reusable = remember(tag) { if (tag.original) null else com.noorpro.app.data.media.edit.ReelSoundLibrary.findByCredit(library, tag.credit) }
                        val (playing, toggle) = rememberSoundPreview()
                        Reels2SoundPage(
                            Reels2Sound(tag.title, tag.artist, same.size.toLong(), reusable != null),
                            same.take(12).map { it to null },
                            onUse = {
                                sheet = null; toggle(null)
                                if (reusable == null) Toast.makeText(context, "This is the creator's original audio — make your own reel with a sound from Al-Noor Audio", Toast.LENGTH_SHORT).show()
                                viewModel.pendingReelSound = reusable
                                viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL)
                            },
                            onPreview = { if (reusable != null) toggle(reusable) else Toast.makeText(context, "Play the reel to hear its original audio", Toast.LENGTH_SHORT).show() },
                            previewPlaying = reusable != null && playing == reusable.trackId,
                            onOpen = { sheet = null; toggle(null); goTo(it) }, modifier = Modifier.heightIn(max = 640.dp))
                    }
                    R2Sheet.SEARCH -> SearchHost(allReels, watchStore, features, onOpen = { list, start, resumeAt ->
                        sheet = null
                        if (resumeAt != null) pendingSeek = start.id to resumeAt
                        viewModel.reelQueue = list
                        scope.launch { delay(50); pager.scrollToPage(list.indexOfFirst { it.id == start.id }.coerceAtLeast(0)) }
                    }, onExplore = { sheet = null; viewModel.navigateTo(DeenScreen.REELS_EXPLORE) })
                }
            }
        }
    }
}

@Composable
private fun ReelPlayerSurface(player: ExoPlayer, fit: Boolean, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { c -> PlayerView(c).apply { useController = false; setShutterBackgroundColor(android.graphics.Color.TRANSPARENT); setKeepContentOnPlayerReset(true) } },
        update = { v ->
            v.resizeMode = if (fit) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            if (v.player !== player) v.player = player
        },
        onRelease = { it.player = null },
    )
}

@Composable
private fun Reels2Empty(title: String, message: String, action: String = "Retry", onAction: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(title, color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(message, color = Color(0xCCFFFFFF), fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Text(action, color = R2Green, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(R2Mint).clickable(onClick = onAction).padding(horizontal = 18.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun CommentsHost(post: UmmahPost, repo: UmmahRepository, features: Reels2Features, viewModel: DeenViewModel, onAdded: () -> Unit) {
    val context = LocalContext.current
    var comments by remember(post.id) { mutableStateOf<List<UmmahComment>>(emptyList()) }
    var myLikes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draft by remember(post.id) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val termsGate = rememberUgcTermsGate()
    val blocked = rememberBlockedUids()
    DisposableEffect(post.id) {
        val reg = runCatching { repo.observeComments(post.id) { comments = it } }.getOrNull()
        val likes = runCatching { repo.observeMyCommentLikes { myLikes = it } }.getOrNull()
        onDispose { reg?.remove(); likes?.remove() }
    }
    val ordered = orderReelComments(comments.filter { it.creatorUid !in blocked }, post.pinnedCommentId)
    Reels2CommentsSheet(
        ordered, maxOf(post.commentCount, comments.size.toLong()), post.creatorName, features,
        draft = draft, onDraft = { draft = it.take(500) },
        onSend = {
            val text = draft.trim()
            when {
                sending || text.isBlank() -> Unit
                halalBlocks(text) -> Toast.makeText(context, "Please keep comments kind and respectful", Toast.LENGTH_SHORT).show()
                !termsGate.check() -> Unit
                else -> { sending = true; repo.submitComment(post.id, text) { ok -> sending = false; if (ok) { draft = ""; onAdded() } else Toast.makeText(context, "Unable to comment", Toast.LENGTH_SHORT).show() } }
            }
        },
        onLikeComment = { c -> val on = c.id !in myLikes; myLikes = if (on) myLikes + c.id else myLikes - c.id; repo.toggleCommentLike(c.id, on) },
        likedIds = myLikes,
        onVoice = { Toast.makeText(context, "Voice comments are coming soon", Toast.LENGTH_SHORT).show() },
        modifier = Modifier.heightIn(max = 620.dp),
    )
}

@Composable
private fun AskAiHost(post: UmmahPost) {
    var answer by remember(post.id) { mutableStateOf(Reels2AiAnswer("", "", "", "", loading = true)) }
    var simple by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(post.id) {
        val raw = runCatching { AiFeatureService.generateResponse(Reels2AiPrompts.about(post), NoorAiMode.AskNoor) }.getOrNull()
        answer = if (raw.isNullOrBlank() || raw == AiFeatureService.UNAVAILABLE_MESSAGE) {
            Reels2AiAnswer("", "", "", "", error = "Noor AI isn't reachable right now. Please try again later.")
        } else {
            val ref = Reels2AiPrompts.section(raw, "REFERENCE").takeUnless { it.contains("none stated", true) }.orEmpty()
            val refText = withContext(Dispatchers.IO) {
                runCatching { ReferenceChecker.find(ref).firstOrNull { it.ok }?.let { ReferenceChecker.withText(context, it).text.lineSequence().firstOrNull().orEmpty() } }.getOrNull().orEmpty()
            }
            Reels2AiAnswer(Reels2AiPrompts.section(raw, "SUMMARY").ifBlank { raw.substringBefore("FOLLOWUPS:").take(400) }, ref, refText, Reels2AiPrompts.section(raw, "SIMPLE"))
        }
    }
    Reels2AskAiSheet(answer, simple, { simple = !simple }, keyPoints = parseReelChapters(post.caption), modifier = Modifier.heightIn(max = 620.dp))
}

@Composable
private fun VerifyHost(post: UmmahPost) {
    val context = LocalContext.current
    var checks by remember(post.id) { mutableStateOf<List<RefCheck>>(emptyList()) }
    LaunchedEffect(post.id) {
        checks = withContext(Dispatchers.IO) {
            ReferenceChecker.find(post.caption + "\n" + post.sourceReference).map { runCatching { ReferenceChecker.withText(context, it) }.getOrDefault(it) }
        }
    }
    Reels2VerifySheet(checks, Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()))
}

@Composable
private fun CollectionHost(post: UmmahPost, saved: Boolean, save: (UmmahPost, (Boolean) -> Unit) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { UmmahRepository() }
    var collections by remember { mutableStateOf<List<UmmahSavedCollection>>(emptyList()) }
    var assignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pending by remember { mutableStateOf<String?>(null) }
    var naming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        runCatching { repo.observeSavedCollections { collections = it }; repo.observeSavedAssignments { assignments = it } }
        onDispose { repo.close() }
    }
    fun assign(c: UmmahSavedCollection) = save(post) { ok ->
        if (ok) repo.assignSavedToCollection(post.id, c.id) { done ->
            Toast.makeText(context, if (done) "Saved to ${c.title}" else "Saved", Toast.LENGTH_SHORT).show()
        }
        onClose()
    }
    LaunchedEffect(collections, pending) {
        val title = pending ?: return@LaunchedEffect
        collections.firstOrNull { it.title.equals(title, true) }?.let { pending = null; assign(it) }
    }
    fun pick(title: String) {
        val c = collections.firstOrNull { it.title.equals(title, true) }
        if (c != null) assign(c) else { pending = title; repo.createSavedCollection(title) { ok -> if (!ok) { pending = null; save(post) { onClose() } } } }
    }
    val selected = collections.firstOrNull { it.id == assignments[post.id] }?.title ?: if (saved) "Saved" else null
    Column {
        Reels2CollectionSheet(collections.map { it.title }, selected, ::pick, onNew = { naming = true })
        if (naming) Row(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(newName, { newName = it.take(40) }, singleLine = true, placeholder = { Text("Collection name") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text("Create", color = R2Green, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { if (newName.isNotBlank()) pick(newName.trim()) }.padding(8.dp))
        }
    }
}

@Composable
private fun SearchHost(all: List<UmmahPost>, watch: ReelWatchStore, features: Reels2Features, onOpen: (List<UmmahPost>, UmmahPost, Long?) -> Unit, onExplore: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val parsed = remember(text) { if (text.isBlank()) null else parseSmartReelQuery(text) }
    val results = remember(parsed, all) { parsed?.let { smartReelMatches(it, all) }.orEmpty() }
    val cont = remember(all) { continueWatching(watch.all(), all) }
    Reels2SheetFrame("Search reels", "Smart search runs on your phone — try “find reels about patience in Urdu”.", Modifier.heightIn(max = 680.dp)) {
        Reels2SmartSearch(text, { text = it }, parsed)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (parsed != null) {
                if (results.isEmpty()) Text("No reels found for “${parsed.topic}” yet.", color = stitchMutedText(), fontSize = 13.sp)
                results.take(20).forEach { p ->
                    Row(Modifier.fillMaxWidth().clickable { onOpen(results, p, null) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Reels2Thumb(p, null, Modifier.size(width = 54.dp, height = 72.dp).clip(RoundedCornerShape(10.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(captionWithoutChapters(p.caption).lineSequence().firstOrNull().orEmpty(), color = stitchText(), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.5.sp)
                            Text(p.creatorName, color = stitchMutedText(), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (cont.isNotEmpty()) {
                Reels2ContinueRow(cont.map { Triple(it.first, it.second, null) }, onOpen = { p ->
                    onOpen(cont.map { it.first }, p, cont.firstOrNull { it.first.id == p.id }?.second?.positionMs)
                })
                Spacer(Modifier.height(12.dp))
            }
            if (features.live) { Reels2LiveCard("Friday tafsir circle", "Live bayans", "Fri after Isha"); Spacer(Modifier.height(12.dp)) }
            Text("Open Explore", color = R2Green, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(R2Mint).clickable(onClick = onExplore).padding(horizontal = 16.dp, vertical = 9.dp))
            Spacer(Modifier.height(12.dp))
        }
    }
}
