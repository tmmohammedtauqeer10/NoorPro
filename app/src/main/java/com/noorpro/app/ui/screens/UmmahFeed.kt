package com.noorpro.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

/*
 * 1.1.0 — the redesigned Ummah community feed.
 *
 * NoorPro's own take on a modern social feed: serif "Ummah" masthead, a mint segmented
 * For You / Following / Latest switcher that stays pinned while scrolling, topic filters,
 * a "Share a reflection" composer, airy post cards (author + Follow, coloured #hashtags that
 * open search, Arabic text panel, rounded media with double-tap like) and a calm action row
 * (like · comment · repost/quote · share · save). Moderation (report/block overflow), stories,
 * the reels strip, inline ads, comments and infinite scroll are all carried over.
 */

// ---------------------------------------------------------------- pure logic (unit tested)

internal enum class UmmahFeedTab(val label: String) {
    FOR_YOU("For You"), FOLLOWING("Following"), LATEST("Latest")
}

internal val ummahFeedTopics = listOf("All", "Quran", "Hadith", "Reflections", "Duas", "Reminders", "Community")

/** Topic chip match: category first, then caption / Arabic text ("Duas" also matches "dua"). */
internal fun matchesUmmahTopic(post: UmmahPost, topic: String): Boolean {
    if (topic.isBlank() || topic == "All") return true
    val needle = topic.lowercase().removeSuffix("s")
    return listOf(post.category, post.caption, post.arabicText).any { it.lowercase().contains(needle) }
}

/**
 * The posts for one feed tab. Reels live in their own strip, so they are excluded here.
 *  - For You: the existing ranking (freshness + engagement + relationship + interest).
 *  - Following: only creators you follow, newest first.
 *  - Latest: everyone, newest first.
 */
internal fun ummahTabPosts(
    tab: UmmahFeedTab,
    posts: List<UmmahPost>,
    following: Set<String>,
    liked: Set<String>,
    saved: Set<String>,
    topic: String = "All"
): List<UmmahPost> {
    val base = posts.filterNot { it.isReelLike() }
    val ordered = when (tab) {
        UmmahFeedTab.FOR_YOU -> rankUmmahPosts(base, following, liked, saved)
        UmmahFeedTab.FOLLOWING -> base.filter { it.creatorUid.isNotBlank() && it.creatorUid in following }
            .sortedByDescending { it.publishedAt }
        UmmahFeedTab.LATEST -> base.sortedByDescending { it.publishedAt }
    }
    return ordered.filter { matchesUmmahTopic(it, topic) }
}

/** A run of caption text; [tag] is set for a #hashtag or @mention. */
internal data class CaptionSegment(val text: String, val tag: String? = null)

private val captionTagRegex = Regex("""(?<![\w#@])([#@][\p{L}\p{N}_]{2,40})""")

/** Splits a caption into plain text and tappable #hashtag / @mention segments. */
internal fun captionSegments(text: String): List<CaptionSegment> {
    if (text.isEmpty()) return emptyList()
    val out = mutableListOf<CaptionSegment>()
    var last = 0
    for (m in captionTagRegex.findAll(text)) {
        val r = m.groups[1]!!.range
        if (r.first > last) out += CaptionSegment(text.substring(last, r.first))
        out += CaptionSegment(m.groupValues[1], m.groupValues[1])
        last = r.last + 1
    }
    if (last < text.length) out += CaptionSegment(text.substring(last))
    return out
}

/** Server count adjusted for an optimistic toggle that the server has not reflected yet. */
internal fun optimisticCount(serverCount: Long, serverActive: Boolean, shownActive: Boolean): Long =
    (serverCount + when {
        shownActive && !serverActive -> 1L
        !shownActive && serverActive -> -1L
        else -> 0L
    }).coerceAtLeast(0L)

/** Media frame aspect (width / height): stored dimensions, clamped to a tidy 4:5 … 16:9 range. */
internal fun ummahMediaAspect(post: UmmahPost): Float {
    if (post.isReelLike()) return 4f / 5f
    if (post.isVideoMedia()) return 16f / 9f
    val rotated = post.mediaRotationDegrees % 180 != 0
    val w = (if (rotated) post.mediaHeight else post.mediaWidth).toFloat()
    val h = (if (rotated) post.mediaWidth else post.mediaHeight).toFloat()
    if (w <= 0f || h <= 0f) return if (post.mediaUrls.size > 1) 1f else 4f / 3f
    return (w / h).coerceIn(0.8f, 16f / 9f)
}

/** A tapped #hashtag opens Ummah search pre-filled with it. */
internal object UmmahSearchLaunch {
    @Volatile private var pending: String = ""
    fun open(query: String) { pending = query.trim() }
    fun consume(): String = pending.also { pending = "" }
}

/** Test/preview hook: decoded images by URL so renders don't need the network. */
internal object UmmahFeedPreview {
    @Volatile var images: Map<String, android.graphics.Bitmap> = emptyMap()
    /** 1.1.1: render tests set this so poll cards don't open Firestore listeners. */
    @Volatile var offline: Boolean = false
}

// ---------------------------------------------------------------- palette

private val FeedMint = Color(0xFFE6F2EC)
private val FeedMintDark = Color(0xFF173A2F)
private val FeedLike = Color(0xFFD9475A)
@Composable internal fun feedMint(): Color = if (isStitchLight()) FeedMint else FeedMintDark
@Composable internal fun feedAccent(): Color = if (isStitchLight()) StitchEmerald else Color(0xFF7FD3AE)
@Composable internal fun feedGold(): Color = if (isStitchLight()) Color(0xFFA9842A) else StitchGold

// ---------------------------------------------------------------- state holders

internal data class UmmahFeedUi(
    val posts: List<UmmahPost>,
    val reels: List<UmmahPost> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val currentUid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val following: Set<String> = emptySet(),
    val liked: Set<String> = emptySet(),
    val saved: Set<String> = emptySet(),
    val serverLiked: Set<String> = liked,
    val serverSaved: Set<String> = saved
)

internal class UmmahFeedActions(
    val onSearch: () -> Unit = {},
    val onNotifications: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onCompose: () -> Unit = {},
    val onCreateReel: () -> Unit = {},
    val onWatchReels: () -> Unit = {},
    val onOpenReel: (UmmahPost) -> Unit = {},
    val onOpenProfile: (UmmahPost) -> Unit = {},
    val onFollow: (UmmahPost) -> Unit = {},
    val onLike: (UmmahPost) -> Unit = {},
    val onComment: (UmmahPost) -> Unit = {},
    val onRepost: (UmmahPost) -> Unit = {},
    val onQuote: (UmmahPost) -> Unit = {},
    val onShare: (UmmahPost) -> Unit = {},
    val onSave: (UmmahPost) -> Unit = {},
    val onTag: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onFindPeople: () -> Unit = {}
)

// ---------------------------------------------------------------- screen (wired to Firestore)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoorUmmahFeedScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    val feedState = rememberStitchUmmahFeed(refreshKey)
    val following = rememberStitchFollowing(viewModel.isLoggedIn)
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    val repo = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { repo.currentUserUid().orEmpty() }
    val displayName = viewModel.userDisplayName.ifBlank { if (viewModel.isLoggedIn) "Noor" else "Guest" }
    var optimisticLikes by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var optimisticSaves by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var optimisticFollows by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var commentPost by remember { mutableStateOf<UmmahPost?>(null) }
    var quoteTarget by remember { mutableStateOf<UmmahPost?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(UmmahFeedTab.FOR_YOU) }
    var topic by rememberSaveable { mutableStateOf("All") }
    val listState = rememberLazyListState()
    LaunchedEffect(feedState.loading) {
        if (!feedState.loading && refreshing) { refreshing = false; listState.scrollToItem(0) }
    }
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val effectiveFollowing = remember(following, optimisticFollows) {
        following + optimisticFollows.filterValues { it }.keys - optimisticFollows.filterValues { !it }.keys
    }
    val liked = remember(interactions.liked, optimisticLikes) {
        interactions.liked + optimisticLikes.filterValues { it }.keys - optimisticLikes.filterValues { !it }.keys
    }
    val saved = remember(interactions.saved, optimisticSaves) {
        interactions.saved + optimisticSaves.filterValues { it }.keys - optimisticSaves.filterValues { !it }.keys
    }
    // 1.1.1: followers-only posts (separate collection) — only fetched when the feature flag is on.
    LaunchedEffect(Unit) { com.noorpro.app.data.ComposerFlags.load() }
    LaunchedEffect(feedState.posts) { com.noorpro.app.data.ComposerTagSource.captions = feedState.posts.take(200).map { it.caption } }
    var followerPosts by remember { mutableStateOf<List<UmmahPost>>(emptyList()) }
    LaunchedEffect(following, com.noorpro.app.data.ComposerFlags.visibility) {
        if (com.noorpro.app.data.ComposerFlags.visibility && viewModel.isLoggedIn && following.isNotEmpty())
            UmmahRepository().fetchFollowerPosts(following) { followerPosts = it }
    }
    val reels = remember(feedState.posts, following, interactions) {
        rankUmmahPosts(feedState.posts.filter { it.isReelLike() }, following, interactions.liked, interactions.saved).take(10)
    }
    val ui = UmmahFeedUi(
        posts = if (followerPosts.isEmpty()) feedState.posts
            else (feedState.posts + followerPosts.filter { it.creatorUid in effectiveFollowing }).distinctBy { it.id },
        reels = reels, loading = feedState.loading, error = feedState.error,
        isLoggedIn = viewModel.isLoggedIn, currentUid = currentUid, displayName = displayName,
        photoUrl = viewModel.userPhotoUrl, following = effectiveFollowing, liked = liked, saved = saved,
        serverLiked = interactions.liked, serverSaved = interactions.saved
    )
    val actions = UmmahFeedActions(
        onSearch = { viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) },
        onNotifications = { viewModel.navigateTo(DeenScreen.UMMAH_NOTIFICATIONS) },
        onProfile = { viewModel.openMyProfile() },
        onCompose = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST) },
        onCreateReel = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL) },
        onWatchReels = { viewModel.navigateTo(DeenScreen.REELS) },
        onOpenReel = { viewModel.openReel(it.id) },
        onOpenProfile = { viewModel.openCreatorProfile(it.creatorUid, it.creatorName, it.creatorHandle) },
        onFollow = { post ->
            when {
                post.creatorUid.isBlank() || post.creatorUid == currentUid -> toast("This is your post")
                !viewModel.isLoggedIn -> toast("Sign in to follow creators")
                else -> {
                    val next = post.creatorUid !in effectiveFollowing
                    optimisticFollows = optimisticFollows + (post.creatorUid to next)
                    repo.setFollowing(post.creatorUid, post.creatorName, post.creatorHandle, next) { ok ->
                        if (!ok) { optimisticFollows = optimisticFollows - post.creatorUid; toast("Unable to update follow") }
                    }
                }
            }
        },
        onLike = { post ->
            if (!viewModel.isLoggedIn) toast("Sign in to like posts") else {
                val was = post.id in liked
                optimisticLikes = optimisticLikes + (post.id to !was)
                repo.toggleInteraction(post.id, "likes", !was) { ok ->
                    if (!ok) { optimisticLikes = optimisticLikes + (post.id to was); toast("Unable to update like") }
                }
            }
        },
        onComment = { post -> if (!viewModel.isLoggedIn) toast("Sign in to comment") else commentPost = post },
        onRepost = { post ->
            if (!viewModel.isLoggedIn) toast("Sign in to repost")
            else repo.repost(context, post, "") { _, msg -> toast(msg ?: "Done") }
        },
        onQuote = { post -> if (!viewModel.isLoggedIn) toast("Sign in to quote") else quoteTarget = post },
        onShare = { viewModel.sharePost = it },
        onSave = { post ->
            if (!viewModel.isLoggedIn) toast("Sign in to save posts") else {
                val was = post.id in saved
                optimisticSaves = optimisticSaves + (post.id to !was)
                repo.toggleInteraction(post.id, "saved", !was) { ok ->
                    if (!ok) { optimisticSaves = optimisticSaves + (post.id to was); toast("Unable to update saved") }
                }
            }
        },
        onTag = { tag ->
            if (tag.startsWith("@")) {
                val target = feedState.posts.firstOrNull { it.creatorHandle.equals(tag, ignoreCase = true) && it.creatorUid.isNotBlank() }
                if (target != null) viewModel.openCreatorProfile(target.creatorUid, target.creatorName, target.creatorHandle)
                else { UmmahSearchLaunch.open(tag); viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) }
            } else { UmmahSearchLaunch.open(tag); viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) }
        },
        onRetry = { refreshKey++ },
        onFindPeople = { viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) }
    )

    StitchScreen {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refreshing = true; refreshKey++ },
            modifier = Modifier.fillMaxSize()
        ) {
            NoorUmmahFeed(
                ui = ui, actions = actions, tab = tab, onTab = { tab = it }, topic = topic, onTopic = { topic = it },
                listState = listState,
                stories = {
                    var blocked by remember { mutableStateOf<Set<String>>(emptySet()) }
                    LaunchedEffect(viewModel.isLoggedIn) { if (viewModel.isLoggedIn) repo.observeBlockedUsers { blocked = it } }
                    StoriesSection(
                        myName = displayName,
                        myHandle = "@${viewModel.ummahUsername.ifBlank { viewModel.userEmail.substringBefore("@") }}",
                        myAvatarUrl = viewModel.userPhotoUrl,
                        isLoggedIn = viewModel.isLoggedIn,
                        blocked = blocked,
                        onOpenChat = { _, _ -> }
                    )
                },
                ad = { StitchSponsoredPostCard() }
            )
        }
    }
    commentPost?.let { target ->
        StitchCommentSheet(
            post = target, repository = repo, isLoggedIn = viewModel.isLoggedIn, viewModel = viewModel,
            onOpenProfile = { uid, name, handle -> commentPost = null; viewModel.openCreatorProfile(uid, name, handle) },
            onDismiss = { commentPost = null }
        )
    }
    quoteTarget?.let { target ->
        var quoteText by remember(target.id) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { quoteTarget = null },
            title = { Text("Quote with a reflection") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = quoteText, onValueChange = { quoteText = it.take(500) },
                    placeholder = { Text("Add your reflection") }, minLines = 3
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(enabled = quoteText.isNotBlank(), onClick = {
                    quoteTarget = null
                    repo.repost(context, target, quoteText) { _, msg -> toast(msg ?: "Done") }
                }) { Text("Post") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { quoteTarget = null }) { Text("Cancel") } }
        )
    }
}

// ---------------------------------------------------------------- feed (stateless, rendered in tests)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NoorUmmahFeed(
    ui: UmmahFeedUi,
    actions: UmmahFeedActions,
    tab: UmmahFeedTab,
    onTab: (UmmahFeedTab) -> Unit,
    topic: String,
    onTopic: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    stories: (@Composable () -> Unit)? = null,
    ad: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val posts = remember(ui.posts, tab, topic, ui.following, ui.serverLiked, ui.serverSaved) {
        ummahTabPosts(tab, ui.posts, ui.following, ui.serverLiked, ui.serverSaved, topic)
    }
    var visible by remember(tab, topic, ui.posts.size == 0) { mutableStateOf(12) }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val atTop by remember { derivedStateOf { listState.firstVisibleItemIndex < 3 } }

    Box(modifier.fillMaxSize().testTag("ummah_feed")) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag("ummah_feed_list"),
            contentPadding = PaddingValues(bottom = 190.dp + navInset)
        ) {
            item(key = "masthead") {
                UmmahMasthead(ui.displayName, ui.photoUrl, actions)
            }
            if (stories != null) item(key = "stories") {
                Box(Modifier.padding(horizontal = 12.dp).padding(bottom = 6.dp)) { stories() }
            }
            stickyHeader(key = "tabs") {
                UmmahTabBar(tab, onTab)
            }
            item(key = "topics") { UmmahTopicRow(topic, onTopic) }
            if (tab == UmmahFeedTab.FOR_YOU) item(key = "composer") {
                UmmahComposerBar(ui.displayName, ui.photoUrl, actions)
            }
            when {
                ui.loading -> items(3, key = { "sk$it" }) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { StitchPostSkeleton() }
                }
                ui.error != null -> item(key = "error") {
                    UmmahFeedState(
                        icon = Icons.Default.WifiOff, title = "Couldn't load the Ummah",
                        message = "Check your connection and try again.", action = "Try again", onAction = actions.onRetry,
                        tag = "ummah_error"
                    )
                }
                posts.isEmpty() -> item(key = "empty") {
                    when {
                        tab == UmmahFeedTab.FOLLOWING && !ui.isLoggedIn -> UmmahFeedState(
                            Icons.Default.Person, "Sign in to see who you follow",
                            "Posts from people you follow will appear here.", "Explore For You", { onTab(UmmahFeedTab.FOR_YOU) })
                        tab == UmmahFeedTab.FOLLOWING && topic == "All" -> UmmahFeedState(
                            Icons.Default.PersonAdd, "Your circle is quiet",
                            "Follow brothers and sisters whose reflections inspire you, and their posts will gather here.",
                            "Find people", actions.onFindPeople, tag = "ummah_empty_following")
                        topic != "All" -> UmmahFeedState(
                            Icons.Default.Groups, "Nothing in $topic yet",
                            "No posts match this topic right now.", "Show all topics", { onTopic("All") })
                        else -> UmmahFeedState(
                            Icons.Default.Edit, "Begin the conversation",
                            "Be the first to share a reflection, a verse or a reminder.", "Share a reflection", actions.onCompose)
                    }
                }
                else -> {
                    itemsIndexed(posts.take(visible), key = { _, p -> p.id }) { index, post ->
                        Column {
                            if (ad != null && shouldShowUmmahFeedAd(index)) {
                                Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { ad() }
                            }
                            if (tab == UmmahFeedTab.FOR_YOU && index == 3 && ui.reels.isNotEmpty()) {
                                Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    StitchYourPostStrip(ui.reels, actions.onCreateReel, actions.onWatchReels, actions.onOpenReel)
                                }
                            }
                            UmmahPostCard(
                                post = post,
                                isOwn = post.creatorUid.isBlank() || post.creatorUid == ui.currentUid,
                                isFollowing = post.creatorUid in ui.following,
                                isLiked = post.id in ui.liked,
                                isSaved = post.id in ui.saved,
                                likeCount = optimisticCount(post.likeCount, post.id in ui.serverLiked, post.id in ui.liked),
                                saveCount = optimisticCount(post.saveCount, post.id in ui.serverSaved, post.id in ui.saved),
                                actions = actions
                            )
                        }
                    }
                    if (posts.size > visible) item(key = "more-$visible") {
                        LaunchedEffect(visible) { kotlinx.coroutines.delay(250); visible += 10 }
                        Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { StitchPostSkeleton() }
                    }
                    if (tab == UmmahFeedTab.FOR_YOU && posts.size <= 3 && ui.reels.isNotEmpty()) item(key = "reels") {
                        Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            StitchYourPostStrip(ui.reels, actions.onCreateReel, actions.onWatchReels, actions.onOpenReel)
                        }
                    }
                }
            }
        }
        UmmahComposeFab(
            expanded = atTop,
            onClick = actions.onCompose,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 104.dp + navInset)
        )
    }
}

// ---------------------------------------------------------------- pieces

@Composable
private fun UmmahMasthead(displayName: String, photoUrl: String, actions: UmmahFeedActions) {
    Row(
        Modifier.statusBarsPadding().fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Ummah", color = feedAccent(), fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon,
                lineHeight = 34.sp, modifier = Modifier.testTag("ummah_title"))
            Text("Reflections from the community", color = stitchMutedText(), fontSize = 12.5.sp)
        }
        UmmahRoundButton(Icons.Default.Search, "Search people, hashtags and reels", actions.onSearch, Modifier.testTag("ummah_search"))
        Spacer(Modifier.width(8.dp))
        UmmahRoundButton(Icons.Default.Notifications, "Ummah notifications", actions.onNotifications)
        Spacer(Modifier.width(8.dp))
        UmmahAvatar(displayName, photoUrl, 40.dp, onClick = actions.onProfile, contentDescription = "My profile")
    }
}

@Composable
private fun UmmahRoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(feedMint()).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = feedAccent(), modifier = Modifier.size(20.dp)) }
}

@Composable
internal fun UmmahAvatar(
    name: String, photoUrl: String, size: androidx.compose.ui.unit.Dp,
    ring: Color? = null, onClick: (() -> Unit)? = null, contentDescription: String? = null
) {
    val preview = UmmahFeedPreview.images[photoUrl]
    Box(
        Modifier.size(size)
            .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape).padding(3.dp) else Modifier)
            .clip(CircleShape)
            .background(if (isStitchLight()) Color(0xFFF3E6C4) else Color(0xFF3A3220))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            preview != null -> Image(preview.asImageBitmap(), contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            photoUrl.isNotBlank() -> AsyncImage(photoUrl, contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> Text(
                name.removePrefix("@").trim().take(1).uppercase().ifBlank { "U" },
                color = if (isStitchLight()) Color(0xFF7A5A12) else StitchGold,
                fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon
            )
        }
    }
}

/** Mint segmented switcher with a sliding surface pill — pinned while the feed scrolls. */
@Composable
private fun UmmahTabBar(selected: UmmahFeedTab, onSelect: (UmmahFeedTab) -> Unit) {
    val tabs = UmmahFeedTab.entries
    Box(Modifier.fillMaxWidth().background(stitchBackground()).padding(horizontal = 16.dp, vertical = 8.dp).testTag("ummah_tabs")) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(4.dp)
        ) {
            val segment = maxWidth / tabs.size
            val offset by animateDpAsState(segment * tabs.indexOf(selected), spring(dampingRatio = 0.8f, stiffness = 500f), label = "tabPill")
            Box(
                Modifier.offset(x = offset).width(segment).fillMaxHeight().clip(RoundedCornerShape(999.dp))
                    .background(if (isStitchLight()) Color.White else Color(0xFF0E5A41))
            )
            Row(Modifier.fillMaxSize()) {
                tabs.forEach { t ->
                    val active = t == selected
                    val color by animateColorAsState(if (active) feedAccent() else stitchMutedText(), label = "tabC")
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(999.dp)).clickable { onSelect(t) }
                            .testTag("ummah_tab_${t.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (active) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(StitchGold))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(t.label, color = if (active && !isStitchLight()) Color.White else color, fontSize = 13.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahTopicRow(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp)
    ) {
        items(ummahFeedTopics) { t ->
            val active = t == selected
            Text(
                t,
                color = if (active) (if (isStitchLight()) Color.White else Color(0xFF06291D)) else stitchText(),
                fontSize = 12.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (active) feedAccent() else Color.Transparent)
                    .border(1.dp, if (active) Color.Transparent else stitchLine(), RoundedCornerShape(999.dp))
                    .clickable { onSelect(t) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun UmmahComposerBar(displayName: String, photoUrl: String, actions: UmmahFeedActions) {
    Row(
        Modifier.padding(horizontal = 12.dp).padding(bottom = 10.dp).fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)).background(stitchSurface())
            .border(1.dp, stitchLine().copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("ummah_composer"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UmmahAvatar(displayName, photoUrl, 38.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            "Share a reflection…", color = stitchMutedText(), fontSize = 14.sp,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).clickable(onClick = actions.onCompose)
                .padding(horizontal = 6.dp, vertical = 10.dp)
        )
        UmmahComposerTool(Icons.Default.Image, "Add a photo", actions.onCompose)
        Spacer(Modifier.width(6.dp))
        UmmahComposerTool(Icons.Default.SmartDisplay, "Create a reel", actions.onCreateReel)
    }
}

@Composable
private fun UmmahComposerTool(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(feedMint()).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = feedAccent(), modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun UmmahComposeFab(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = rememberHaptic()
    Row(
        modifier.height(56.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0B6243), StitchEmerald)))
            .border(1.dp, StitchGold.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .clickable { haptic(); onClick() }
            .padding(horizontal = 16.dp)
            .testTag("ummah_fab"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Edit, "New post", tint = StitchGold, modifier = Modifier.size(22.dp))
        AnimatedVisibility(expanded, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            Text("Reflect", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun UmmahFeedState(
    icon: ImageVector, title: String, message: String, action: String, onAction: () -> Unit, tag: String = "ummah_empty"
) {
    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(stitchSurface()).padding(horizontal = 26.dp, vertical = 30.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(feedMint()), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = feedAccent(), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = stitchText(), fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, color = stitchMutedText(), fontSize = 13.5.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            action, color = if (isStitchLight()) Color.White else Color(0xFF06291D), fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(feedAccent()).clickable(onClick = onAction)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

// ---------------------------------------------------------------- post card

@Composable
internal fun UmmahPostCard(
    post: UmmahPost,
    isOwn: Boolean,
    isFollowing: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    likeCount: Long,
    saveCount: Long,
    actions: UmmahFeedActions
) {
    val repostOf = post.sourceReference.startsWith("repost:")
    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 5.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)).background(stitchSurface())
            .border(1.dp, stitchLine().copy(alpha = if (isStitchLight()) 0.55f else 0.35f), RoundedCornerShape(24.dp))
            .padding(top = 14.dp, bottom = 6.dp)
            .testTag("ummah_post_${post.id}")
    ) {
        if (repostOf) {
            Row(Modifier.padding(start = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Repeat, null, tint = stitchMutedText(), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Shared to the Ummah", color = stitchMutedText(), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        // Author row
        Row(Modifier.padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { actions.onOpenProfile(post) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                UmmahAvatar(post.creatorName.ifBlank { post.creatorHandle }, post.creatorPhotoUrl, 44.dp,
                    ring = if (isFollowing) StitchGold else null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(post.creatorName.ifBlank { reelUsername(post).removePrefix("@") }, color = stitchText(), fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${reelUsername(post)} · ${timeAgo(post.publishedAt).removeSuffix(" ago")}" +
                        (if (post.locationName.isNotBlank()) " · ${post.locationName}" else "") +
                        (if (post.visibility == "followers") " · Followers" else ""), color = stitchMutedText(),
                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (!isOwn) {
                UmmahFollowPill(isFollowing) { actions.onFollow(post) }
                UgcOverflowButton(
                    ReportTarget("post", post.id, post.creatorUid, reelUsername(post), post.caption),
                    tint = stitchMutedText()
                )
            } else Spacer(Modifier.width(10.dp))
        }
        // Topic label
        val label = post.category.trim()
        if (label.isNotBlank()) {
            Row(Modifier.padding(start = 18.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 14.dp, height = 2.dp).background(feedGold()))
                Spacer(Modifier.width(6.dp))
                Text(label.uppercase(), color = feedGold(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                if (post.aiLabel) {
                    Spacer(Modifier.width(8.dp))
                    UmmahAiBadge()
                }
            }
        } else if (post.aiLabel) Row(Modifier.padding(start = 18.dp, top = 10.dp)) { UmmahAiBadge() }
        // Body
        val caption = post.caption.trim()
        if (caption.isNotBlank()) {
            UmmahCaption(caption, onTag = actions.onTag, modifier = Modifier.padding(horizontal = 18.dp).padding(top = 8.dp))
        }
        if (post.arabicText.isNotBlank()) {
            Column(
                Modifier.padding(horizontal = 14.dp).padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(feedMint()).padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    post.arabicText.trim(), color = stitchText(), fontSize = 21.sp, lineHeight = 36.sp,
                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl),
                    textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()
                )
                val ref = post.sourceReference.trim()
                if (ref.isNotBlank() && !repostOf) {
                    Text("— $ref", color = feedAccent(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        if (post.mediaUrl.isNotBlank() || post.mediaUrls.isNotEmpty()) {
            UmmahPostMedia(post, onOpen = { actions.onOpenReel(post) }, onDoubleTapLike = { if (!isLiked) actions.onLike(post) },
                modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp))
        }
        if (post.pollOptions.size >= 2) UmmahPollBlock(post, modifier = Modifier.padding(horizontal = 14.dp).padding(top = 12.dp))
        UmmahActionRow(post, isLiked, isSaved, likeCount, saveCount, actions)
    }
}

/** 1.1.1: "AI-assisted" disclosure set by the creator in the composer. */
@Composable
internal fun UmmahAiBadge() {
    Text(
        "AI-assisted", color = feedAccent(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Percent per option (0..100), rounded; zero votes -> zeros. */
internal fun pollPercents(tally: Map<Int, Int>, options: Int): List<Int> {
    val total = (0 until options).sumOf { tally[it] ?: 0 }
    return (0 until options).map { if (total == 0) 0 else Math.round((tally[it] ?: 0) * 100f / total) }
}

/** 1.1.1: poll on a published post. Votes need the 1.1.1 rules (poll_votes); fails gracefully before that. */
@Composable
internal fun UmmahPollBlock(
    post: UmmahPost,
    previewTally: Map<Int, Int>? = null,
    previewMine: Int = -1,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tally by remember(post.id) { mutableStateOf(previewTally.orEmpty()) }
    var mine by remember(post.id) { mutableStateOf(previewMine) }
    if (previewTally == null && !UmmahFeedPreview.offline) {
        DisposableEffect(post.id) {
            val reg = runCatching { UmmahRepository().observePoll(post.id) { t, m -> tally = t; mine = m } }.getOrNull()
            onDispose { reg?.remove() }
        }
    }
    val closed = post.pollEndsAt in 1 until System.currentTimeMillis()
    val showResults = mine >= 0 || closed
    val percents = pollPercents(tally, post.pollOptions.size)
    val total = tally.values.sum()
    Column(modifier.fillMaxWidth().testTag("ummah_poll_${post.id}"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        post.pollOptions.forEachIndexed { i, option ->
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(shape)
                    .border(1.dp, if (mine == i) feedAccent() else stitchLine(), shape)
                    .clickable(enabled = !showResults) {
                        mine = i
                        UmmahRepository().votePoll(post.id, i) { ok ->
                            if (!ok) { mine = -1; Toast.makeText(context, "Couldn't record your vote. Try again later.", Toast.LENGTH_SHORT).show() }
                        }
                    }
            ) {
                if (showResults) Box(Modifier.fillMaxHeight().fillMaxWidth(percents[i] / 100f).background(feedMint()))
                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(option, color = stitchText(), fontSize = 14.sp, fontWeight = if (mine == i) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (showResults) Text("${percents[i]}%", color = feedAccent(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            "$total vote${if (total == 1) "" else "s"} · " + when {
                closed -> "Final results"
                post.pollEndsAt > 0 -> "${((post.pollEndsAt - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(1)}h left"
                else -> "Open"
            },
            color = stitchMutedText(), fontSize = 12.sp
        )
    }
}

@Composable
private fun UmmahFollowPill(following: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (following) feedMint() else Color.Transparent, label = "fpBg")
    Text(
        if (following) "Following" else "Follow",
        color = feedAccent(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.animateContentSize().clip(RoundedCornerShape(999.dp)).background(bg)
            .border(1.dp, if (following) Color.Transparent else feedAccent().copy(alpha = 0.7f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

@Composable
private fun UmmahCaption(text: String, onTag: (String) -> Unit, modifier: Modifier = Modifier) {
    val accent = feedAccent()
    val annotated = remember(text, accent) {
        buildAnnotatedString {
            captionSegments(text).forEach { seg ->
                if (seg.tag == null) append(seg.text)
                else withLink(
                    LinkAnnotation.Clickable(seg.tag, TextLinkStyles(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold))) { onTag(seg.tag) }
                ) { append(seg.text) }
            }
        }
    }
    var expanded by remember(text) { mutableStateOf(false) }
    var overflowing by remember(text) { mutableStateOf(false) }
    Column(modifier) {
        Text(
            annotated, color = stitchText(), fontSize = 15.sp, lineHeight = 22.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 6, overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow },
            modifier = Modifier.animateContentSize()
        )
        if (overflowing || expanded) Text(
            if (expanded) "Show less" else "Read more", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp).clickable { expanded = !expanded }
        )
    }
}

@Composable
private fun UmmahPostMedia(post: UmmahPost, onOpen: () -> Unit, onDoubleTapLike: () -> Unit, modifier: Modifier = Modifier) {
    val isVideo = post.isVideoMedia() || post.isReelLike()
    var burst by remember(post.id) { mutableStateOf(0) }
    val shape = RoundedCornerShape(18.dp)
    val urls = post.mediaUrls.ifEmpty { listOf(post.mediaUrl) }
    Box(
        modifier.fillMaxWidth().aspectRatio(ummahMediaAspect(post)).clip(shape)
            .background(if (isStitchLight()) Color(0xFFEDE6D6) else StitchEmeraldDeep)
            .then(
                if (isVideo) Modifier.clickable(onClick = onOpen)
                else if (urls.size <= 1) Modifier.pointerInput(post.id) { detectTapGestures(onDoubleTap = { burst++; onDoubleTapLike() }) }
                else Modifier
            )
            .testTag("ummah_media_${post.id}"),
        contentAlignment = Alignment.Center
    ) {
        if (!isVideo && urls.size > 1) {
            val previews = urls.map { UmmahFeedPreview.images[it] }
            PhotoCarousel(urls, post.caption, onDoubleTap = onDoubleTapLike, modifier = Modifier.fillMaxSize(),
                models = if (previews.all { it != null }) previews.filterNotNull() else null)
        } else {
            val key = if (isVideo) post.thumbnailUrl.ifBlank { post.mediaUrl } else post.thumbnailUrl.ifBlank { post.mediaUrl }
            val preview = UmmahFeedPreview.images[key] ?: UmmahFeedPreview.images[post.mediaUrl]
            if (preview != null) Image(preview.asImageBitmap(), post.caption, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else AsyncImage(post.previewModel(), post.caption.ifBlank { if (isVideo) "Video" else "Post image" },
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (isVideo) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f))
                    .border(1.5.dp, StitchGold.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.PlayArrow, if (post.isReelLike()) "Play reel" else "Play video", tint = Color.White, modifier = Modifier.size(32.dp)) }
            Row(
                Modifier.align(Alignment.BottomStart).padding(12.dp).clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SmartDisplay, null, tint = StitchGold, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                val views = if (post.viewCount > 0) " · ${compactCount(post.viewCount)} views" else ""
                Text((if (post.isReelLike()) "Reel" else "Video") + views, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (burst > 0) StitchHeartBurst(burst, Modifier.zIndex(2f))
    }
}

@Composable
private fun UmmahActionRow(
    post: UmmahPost, isLiked: Boolean, isSaved: Boolean, likeCount: Long, saveCount: Long, actions: UmmahFeedActions
) {
    val haptic = rememberHaptic()
    val likePop = popScale(isLiked)
    val savePop = popScale(isSaved)
    val likeTint by animateColorAsState(if (isLiked) FeedLike else stitchMutedText(), label = "likeTint")
    var repostMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        UmmahAction(
            if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            if (isLiked) "Unlike" else "Like", likeCount, likeTint, likePop, "like_${post.id}"
        ) { haptic(); actions.onLike(post) }
        UmmahAction(Icons.Outlined.ChatBubbleOutline, "Comment", post.commentCount, stitchMutedText(), tag = "comment_${post.id}") { actions.onComment(post) }
        Box {
            UmmahAction(Icons.Outlined.Repeat, "Repost", 0, stitchMutedText(), tag = "repost_${post.id}") { repostMenu = true }
            DropdownMenu(expanded = repostMenu, onDismissRequest = { repostMenu = false }) {
                DropdownMenuItem(text = { Text("Repost") }, leadingIcon = { Icon(Icons.Outlined.Repeat, null) },
                    onClick = { repostMenu = false; actions.onRepost(post) })
                DropdownMenuItem(text = { Text("Quote with a reflection") }, leadingIcon = { Icon(Icons.Default.FormatQuote, null) },
                    onClick = { repostMenu = false; actions.onQuote(post) })
            }
        }
        UmmahAction(Icons.Outlined.Share, "Share", post.shareCount, stitchMutedText(), tag = "share_${post.id}") { actions.onShare(post) }
        Spacer(Modifier.weight(1f))
        UmmahAction(
            if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
            if (isSaved) "Saved" else "Save", saveCount, if (isSaved) feedGold() else stitchMutedText(), savePop, "save_${post.id}"
        ) { haptic(); actions.onSave(post) }
    }
}

@Composable
private fun UmmahAction(
    icon: ImageVector, description: String, count: Long, tint: Color, pop: Float = 1f, tag: String = "", onClick: () -> Unit
) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp)
            .then(if (tag.isNotBlank()) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(21.dp).graphicsLayer { scaleX = pop; scaleY = pop })
        if (count > 0) {
            Spacer(Modifier.width(5.dp))
            Text(compactCount(count), color = if (tint == FeedLike) FeedLike else stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
