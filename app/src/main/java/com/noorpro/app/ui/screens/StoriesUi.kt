package com.noorpro.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.noorpro.app.data.StoryRepository
import com.noorpro.app.data.StorySeenStore
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.data.UmmahStory
import com.noorpro.app.data.UmmahStoryGroup
import com.noorpro.app.data.groupStories
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon
import kotlin.math.abs
import kotlinx.coroutines.launch

private val SeenRing = Brush.linearGradient(listOf(Color(0xFF9AA5A1), Color(0xFF9AA5A1)))

/**
 * 1.1.2: mine first, then unseen stories (newest first), then seen ones. The viewer uses the same
 * order so swiping continues in the order the row shows.
 */
internal fun orderStoryGroups(groups: List<UmmahStoryGroup>, myUid: String?, isSeen: (UmmahStoryGroup) -> Boolean): List<UmmahStoryGroup> {
    val mine = groups.filter { myUid != null && it.authorUid == myUid }
    val others = groups.filter { myUid == null || it.authorUid != myUid }
        .sortedWith(compareBy<UmmahStoryGroup> { isSeen(it) }.thenByDescending { it.stories.lastOrNull()?.createdAt ?: 0L })
    return mine + others
}

/**
 * 1.1.2: NoorPro stories ("status") row shown at the top of the Ummah feed in every tab, under the
 * masthead and above the tabs/composer. "Your story" is always first (even when nobody else has a
 * story); unseen stories carry the emerald/gold ring, seen ones a quiet grey ring. Pure UI.
 */
@Composable
internal fun NoorStoriesRow(
    groups: List<UmmahStoryGroup>,
    myUid: String?,
    myName: String,
    myAvatarUrl: String,
    isSeen: (UmmahStoryGroup) -> Boolean,
    uploading: Float? = null,
    onAddStory: () -> Unit,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val mine = groups.indexOfFirst { myUid != null && it.authorUid == myUid }
    val othersCount = groups.size - if (mine >= 0) 1 else 0
    val unseen = groups.count { it.authorUid != myUid && !isSeen(it) }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(stitchSurface())
            .border(1.dp, feedGold().copy(alpha = 0.28f), RoundedCornerShape(22.dp))
            .padding(top = 12.dp, bottom = 10.dp).testTag("ummah_stories_row")
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Stories", color = feedAccent(), fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("24h", color = feedAccent(), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    unseen > 0 -> "$unseen new"
                    othersCount > 0 -> "All caught up"
                    else -> "Share a moment"
                },
                color = if (unseen > 0) feedGold() else stitchMutedText(), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(Modifier.testTag("ummah_stories_list"), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "you") {
                val myGroup = groups.getOrNull(mine)
                StoryBubble(
                    label = "Your story",
                    avatarUrl = myAvatarUrl,
                    initial = myName.trim().take(1).uppercase().ifBlank { "Y" },
                    ring = when { myGroup == null -> null; isSeen(myGroup) -> SeenRing; else -> NoorRingBrush },
                    showAdd = true,
                    progress = uploading,
                    onClick = { if (mine >= 0) onOpen(mine) else onAddStory() },
                    onAdd = onAddStory,
                    modifier = Modifier.testTag("story_you")
                )
            }
            itemsIndexed(groups, key = { _, g -> g.authorUid }) { i, g ->
                if (i == mine) return@itemsIndexed
                StoryBubble(
                    label = g.authorName.ifBlank { g.authorHandle.removePrefix("@") },
                    avatarUrl = g.avatarUrl,
                    initial = g.authorName.take(1).uppercase(),
                    ring = if (isSeen(g)) SeenRing else NoorRingBrush,
                    video = g.stories.lastOrNull()?.type == "video",
                    dim = isSeen(g),
                    onClick = { onOpen(i) },
                    modifier = Modifier.testTag("story_${g.authorUid}").staggeredEntrance(i)
                )
            }
        }
    }
}

@Composable
private fun StoryBubble(
    label: String, avatarUrl: String, initial: String, ring: Brush?, showAdd: Boolean = false,
    progress: Float? = null, video: Boolean = false, dim: Boolean = false,
    onClick: () -> Unit, onAdd: () -> Unit = {}, modifier: Modifier = Modifier
) {
    val preview = UmmahFeedPreview.images[avatarUrl]
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.width(74.dp).pressScale().clickable(onClick = onClick)) {
        Box(Modifier.size(70.dp)) {
            Box(
                Modifier.fillMaxSize()
                    .then(
                        if (ring != null) Modifier.border(if (dim) 2.dp else 3.dp, ring, CircleShape)
                        else Modifier.border(1.5.dp, feedGold().copy(alpha = 0.45f), CircleShape)
                    )
                    .padding(4.dp).clip(CircleShape)
                    .background(if (isStitchLight()) Color(0xFFF3E6C4) else Color(0xFF3A3220)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    preview != null -> androidx.compose.foundation.Image(preview.asImageBitmap(), label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    avatarUrl.isNotBlank() -> AsyncImage(avatarUrl, label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else -> Text(initial.ifBlank { "U" }, color = if (isStitchLight()) Color(0xFF7A5A12) else StitchGold,
                        fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, fontSize = 24.sp)
                }
            }
            if (progress != null) CircularProgressIndicator(progress = { progress }, color = StitchGold, strokeWidth = 3.dp, modifier = Modifier.fillMaxSize())
            if (video) Box(
                Modifier.align(Alignment.BottomStart).size(20.dp).clip(CircleShape).background(Color(0xCC0B2B22))
                    .border(1.5.dp, stitchSurface(), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Videocam, "Video story", tint = Color.White, modifier = Modifier.size(11.dp)) }
            if (showAdd) Box(
                Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF0E8C73), Color(0xFF0B5D4B))))
                    .border(2.dp, stitchSurface(), CircleShape).clickable(onClick = onAdd).testTag("story_add"),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, "Add story", tint = StitchGold, modifier = Modifier.size(15.dp)) }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (dim) stitchMutedText() else stitchText(), fontSize = 11.5.sp,
            fontWeight = if (dim) FontWeight.Normal else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Owns the live stories listener, upload, seen-state and viewer; renders the row. */
@Composable
internal fun StoriesSection(
    myName: String, myHandle: String, myAvatarUrl: String, isLoggedIn: Boolean,
    blocked: Set<String>, onOpenChat: (uid: String, name: String) -> Unit,
    onCreateStory: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val repo = remember { runCatching { StoryRepository() }.getOrNull() }
    val seenStore = remember { StorySeenStore(context) }
    var stories by remember { mutableStateOf<List<UmmahStory>>(emptyList()) }
    var seenTick by remember { mutableStateOf(0) }
    var uploading by remember { mutableStateOf<Float?>(null) }
    var openIndex by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(isLoggedIn) {
        val reg = repo?.observeActive { stories = it }
        onDispose { reg?.remove() }
    }
    val myUid = repo?.currentUid()
    val groups = remember(stories, blocked, myUid) { groupStories(stories.filter { it.authorUid !in blocked }, myUid) }
    fun upload(uri: Uri) {
        val r = repo ?: return
        val done: (Boolean, String?) -> Unit = { ok, msg ->
            uploading = null
            Toast.makeText(context, msg ?: if (ok) "Story shared" else "Couldn't share story", Toast.LENGTH_SHORT).show()
        }
        uploading = 0f
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime.startsWith("video/") || uri.toString().endsWith(".mp4")) {
            val len = runCatching {
                val m = android.media.MediaMetadataRetriever(); m.setDataSource(context, uri)
                (m.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L).also { m.release() }
            }.getOrDefault(0L)
            if (len > 61_000) Toast.makeText(context, "Stories are up to 60 s — using the first minute.", Toast.LENGTH_SHORT).show()
            r.createVideoStory(context, uri, "", myName, myHandle, myAvatarUrl, onProgress = { uploading = it }, onResult = done)
        } else r.createImageStory(context, uri, "", myName, myHandle, myAvatarUrl, onProgress = { uploading = it }, onResult = done)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> if (uri != null) upload(uri) }
    // 1.0.92: record a <=60 s video with the camera app.
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val recordVideo = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val u = captureUri
        if (res.resultCode == android.app.Activity.RESULT_OK) (res.data?.data ?: u)?.let { upload(it) }
    }
    fun launchRecord() {
        val f = java.io.File(context.cacheDir, "story_rec_${System.currentTimeMillis()}.mp4")
        val u = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        captureUri = u
        val intent = android.content.Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 60)
            .putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1)
            .putExtra(android.provider.MediaStore.EXTRA_OUTPUT, u)
            .addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { recordVideo.launch(intent) }.onFailure { Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show() }
    }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) launchRecord() else Toast.makeText(context, "Camera permission is needed to record", Toast.LENGTH_SHORT).show()
    }
    var chooser by remember { mutableStateOf(false) }
    // 1.1.2: the + opens the full composer with Story preselected (it handles sign-in for guests);
    // the old quick chooser is only a fallback when no composer route is wired.
    val add = {
        when {
            onCreateStory != null -> onCreateStory()
            !isLoggedIn -> Toast.makeText(context, "Sign in to add a story", Toast.LENGTH_SHORT).show()
            else -> chooser = true
        }
    }
    if (chooser) StoryAddSheet(
        onDismiss = { chooser = false },
        onGallery = { chooser = false; picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
        onRecord = {
            chooser = false
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) launchRecord()
            else camPerm.launch(android.Manifest.permission.CAMERA)
        }
    )
    // 1.1.2: never hide the row — "Your story" shows even for guests and when nobody has a story.
    val seen: (UmmahStoryGroup) -> Boolean = { g -> g.stories.all { seenStore.isSeen(it.id) } }
    val ordered = remember(groups, seenTick, myUid) { orderStoryGroups(groups, myUid, seen) }
    NoorStoriesRow(ordered, myUid, myName, myAvatarUrl, isSeen = seen, uploading = uploading, onAddStory = add, onOpen = { openIndex = it })
    openIndex?.let { start ->
        StoryViewer(
            groups = ordered, startGroup = start, myUid = myUid,
            onSeen = { s -> if (!seenStore.isSeen(s.id)) { seenStore.markSeen(s.id); repo?.markViewed(s) } },
            onDelete = { s -> repo?.delete(s) { ok -> Toast.makeText(context, if (ok) "Story deleted" else "Couldn't delete", Toast.LENGTH_SHORT).show() } },
            viewerCount = { s, cb -> repo?.viewerCount(s, cb) },
            onReply = { s, text -> onOpenChat(s.authorUid, s.authorName); replyToStory(context, s, text) },
            onDismiss = { openIndex = null; seenTick++ }
        )
    }
}

private fun replyToStory(context: android.content.Context, s: UmmahStory, text: String) {
    if (text.isBlank()) return
    runCatching { UmmahRepository() }.getOrNull()?.sendChatMessage(
        context = context, otherUid = s.authorUid, otherName = s.authorName,
        text = "Replied to your story: ${text.trim()}", type = "text", mediaUri = null,
        sharedMediaUrl = s.mediaUrl
    ) { ok, _ -> Toast.makeText(context, if (ok) "Reply sent" else "Couldn't send reply", Toast.LENGTH_SHORT).show() }
}

private const val STORY_MS = 5_000

/** Full-screen story viewer: segmented progress, tap left/right, hold to pause, swipe down to close. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StoryViewer(
    groups: List<UmmahStoryGroup>, startGroup: Int, myUid: String?,
    onSeen: (UmmahStory) -> Unit, onDelete: (UmmahStory) -> Unit,
    viewerCount: (UmmahStory, (Int) -> Unit) -> Unit,
    onReply: (UmmahStory, String) -> Unit, onDismiss: () -> Unit,
    previewFrozen: Boolean = false
) {
    if (groups.isEmpty()) { onDismiss(); return }
    val pager = rememberPagerState(initialPage = startGroup.coerceIn(0, groups.lastIndex)) { groups.size }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        var dragY by remember { mutableStateOf(0f) }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().background(Color.Black)
                .graphicsLayer { translationY = dragY; val f = 1f - (abs(dragY) / 2400f).coerceIn(0f, 0.15f); scaleX = f; scaleY = f }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { if (dragY > 260f) onDismiss() else dragY = 0f },
                        onDragCancel = { dragY = 0f }
                    ) { _, d -> dragY = (dragY + d).coerceAtLeast(0f) }
                }
        ) { page ->
            StoryGroupPage(
                group = groups[page], active = pager.currentPage == page, myUid = myUid, frozen = previewFrozen,
                onSeen = onSeen, onDelete = { onDelete(it); onDismiss() }, viewerCount = viewerCount, onReply = onReply,
                onNextGroup = { if (page < groups.lastIndex) pager.requestScrollToPage(page + 1) else onDismiss() },
                onPrevGroup = { if (page > 0) pager.requestScrollToPage(page - 1) },
                onClose = onDismiss
            )
        }
    }
}

@Composable
internal fun StoryGroupPage(
    group: UmmahStoryGroup, active: Boolean, myUid: String?, frozen: Boolean,
    onSeen: (UmmahStory) -> Unit, onDelete: (UmmahStory) -> Unit,
    viewerCount: (UmmahStory, (Int) -> Unit) -> Unit, onReply: (UmmahStory, String) -> Unit,
    onNextGroup: () -> Unit, onPrevGroup: () -> Unit, onClose: () -> Unit
) {
    var index by remember(group.authorUid) { mutableStateOf(0) }
    val story = group.stories[index.coerceIn(0, group.stories.lastIndex)]
    var paused by remember { mutableStateOf(false) }
    var replying by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf("") }
    val progress = remember(story.id) { Animatable(if (frozen) 0.45f else 0f) }
    val progressScope = rememberCoroutineScope()
    val isMine = story.authorUid == myUid
    var views by remember(story.id) { mutableStateOf(-1) }
    LaunchedEffect(story.id, active) { if (active) { onSeen(story); if (isMine) viewerCount(story) { views = it } } }
    LaunchedEffect(story.id, active, paused, replying) {
        if (!active || paused || replying || frozen || story.isVideo) return@LaunchedEffect
        progress.animateTo(1f, tween(((1f - progress.value) * STORY_MS).toInt(), easing = LinearEasing))
        if (index < group.stories.lastIndex) index++ else onNextGroup()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val tapModifier = Modifier.fillMaxSize()
            .pointerInput(story.id) {
                detectTapGestures(
                    onPress = { paused = true; tryAwaitRelease(); paused = false },
                    onTap = { o ->
                        if (o.x < size.width / 3f) { if (index > 0) index-- else onPrevGroup() }
                        else { if (index < group.stories.lastIndex) index++ else onNextGroup() }
                    }
                )
            }
        if (story.isVideo) {
            // 1.0.92: video story — progress follows the real playback position/duration.
            val art = StoryPreviewArt.frames[story.id]
            if (frozen && art != null) androidx.compose.foundation.Image(art, null, contentScale = ContentScale.Fit, modifier = tapModifier)
            else if (frozen) AsyncImage(story.thumbUrl, null, contentScale = ContentScale.Fit, modifier = tapModifier)
            else StoryVideoPlayer(
                url = story.mediaUrl, playing = active && !paused && !replying, modifier = tapModifier,
                onProgress = { f -> progressScope.launch { progress.snapTo(f.coerceIn(0f, 1f)) } },
                onEnded = { if (index < group.stories.lastIndex) index++ else onNextGroup() }
            )
            if (frozen) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (paused) Icon(Icons.Default.Pause, null, tint = Color.White, modifier = Modifier.size(64.dp))
            }
            Text(
                "▶ ${((story.durationMs + 999) / 1000).coerceAtLeast(1)}s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 64.dp, end = 12.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 3.dp)
            )
        } else AsyncImage(story.mediaUrl, story.caption.ifBlank { "Story" }, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()
            .pointerInput(story.id) {
                detectTapGestures(
                    onPress = { paused = true; tryAwaitRelease(); paused = false },
                    onTap = { o ->
                        if (o.x < size.width / 3f) { if (index > 0) index-- else onPrevGroup() }
                        else { if (index < group.stories.lastIndex) index++ else onNextGroup() }
                    }
                )
            })
        // Top scrim + segmented progress + header
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))).statusBarsPadding().padding(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                group.stories.forEachIndexed { i, _ ->
                    val f = when { i < index -> 1f; i == index -> progress.value; else -> 0f }
                    Box(Modifier.weight(1f).height(2.5.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.35f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(Color.White))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).border(2.dp, NoorRingBrush, CircleShape).padding(3.dp).clip(CircleShape).background(StitchGold), contentAlignment = Alignment.Center) {
                    if (group.avatarUrl.isNotBlank()) AsyncImage(group.avatarUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Text(group.authorName.take(1).uppercase(), color = StitchEmerald, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text(group.authorHandle.ifBlank { group.authorName }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(storyAge(story.createdAt), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                if (isMine) IconButton(onClick = { onDelete(story) }) { Icon(Icons.Default.Delete, "Delete story", tint = Color.White) }
                else UgcOverflowButton(ReportTarget("story", story.id, story.authorUid, group.authorHandle, story.caption), tint = Color.White)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
            }
        }
        // Bottom: caption + reply / views
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))).navigationBarsPadding().padding(14.dp)) {
            if (story.caption.isNotBlank()) Text(story.caption, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(bottom = 10.dp))
            if (isMine) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (views >= 0) "$views viewer" + (if (views == 1) "" else "s") else "Viewers", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = reply, onValueChange = { reply = it.take(300) },
                    placeholder = { Text("Reply to ${group.authorHandle.ifBlank { group.authorName }}…", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true, shape = RoundedCornerShape(999.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f).onFocusReply { replying = it }
                )
                IconButton(enabled = reply.isNotBlank(), onClick = { onReply(story, reply); reply = ""; replying = false }) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send reply", tint = Color.White)
                }
            }
        }
    }
}

private fun Modifier.onFocusReply(cb: (Boolean) -> Unit): Modifier =
    this.then(Modifier.onFocusChanged { cb(it.isFocused) })

private fun storyAge(ts: Long): String {
    val m = ((System.currentTimeMillis() - ts) / 60_000L).coerceAtLeast(0)
    return if (m < 60) "${m}m" else "${m / 60}h"
}

/** Instagram-style swipeable photo carousel with dots + 1/N counter; double-tap likes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhotoCarousel(urls: List<String>, contentDescription: String, onDoubleTap: () -> Unit, modifier: Modifier = Modifier, models: List<Any>? = null) {
    val pager = rememberPagerState { urls.size }
    var burst by remember { mutableStateOf(0) }
    Box(modifier) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { i ->
            AsyncImage(models?.getOrNull(i) ?: urls[i], "$contentDescription (${i + 1} of ${urls.size})", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().pointerInput(i) { detectTapGestures(onDoubleTap = { burst++; onDoubleTap() }) })
        }
        if (burst > 0) StitchHeartBurst(burst, Modifier.align(Alignment.Center))
        Text("${pager.currentPage + 1}/${urls.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 9.dp, vertical = 3.dp))
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(urls.size) { i ->
                val sel = i == pager.currentPage
                val w by androidx.compose.animation.core.animateDpAsState(if (sel) 16.dp else 6.dp, label = "dot")
                Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(Color.White.copy(alpha = if (sel) 1f else 0.55f)))
            }
        }
    }
}

/** Preview hooks (Robolectric can't decode video). */
internal object StoryPreviewArt { var frames: Map<String, androidx.compose.ui.graphics.ImageBitmap> = emptyMap() }

/** ExoPlayer story video: plays while [playing], caps at 60 s, reports progress and end. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun StoryVideoPlayer(url: String, playing: Boolean, modifier: Modifier, onProgress: (Float) -> Unit, onEnded: () -> Unit) {
    val context = LocalContext.current
    val ended by rememberUpdatedState(onEnded)
    val player = remember(url) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(url)); prepare(); repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(player) {
        val l = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == androidx.media3.common.Player.STATE_ENDED) ended() }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    LaunchedEffect(player, playing) { player.playWhenReady = playing }
    LaunchedEffect(player) {
        while (true) {
            val d = player.duration.takeIf { it > 0 }?.coerceAtMost(com.noorpro.app.data.StoryVideo.MAX_MS) ?: 0L
            if (d > 0) {
                onProgress(player.currentPosition.toFloat() / d)
                if (player.currentPosition >= d && d >= com.noorpro.app.data.StoryVideo.MAX_MS) { player.pause(); ended(); break }
            }
            kotlinx.coroutines.delay(50)
        }
    }
    Box(modifier.background(Color.Black)) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { c -> androidx.media3.ui.PlayerView(c).apply { useController = false; this.player = player; resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT } },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryAddSheet(onDismiss: () -> Unit, onGallery: () -> Unit, onRecord: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = stitchSurface()) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 6.dp)) {
            Text("Add to your story", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
            StoryAddRow(Icons.Default.PhotoLibrary, "Photo or video", "From your gallery · videos up to 60 s", onGallery)
            StoryAddRow(Icons.Default.Videocam, "Record video", "Camera · 60 s max", onRecord)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StoryAddRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(stitchSoftSurface()), contentAlignment = Alignment.Center) { Icon(icon, null, tint = stitchPrimary()) }
        Spacer(Modifier.width(14.dp))
        Column { Text(title, color = stitchText(), fontWeight = FontWeight.SemiBold); Text(sub, color = stitchMutedText(), fontSize = 12.sp) }
    }
}
