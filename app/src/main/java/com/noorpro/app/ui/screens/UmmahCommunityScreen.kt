package com.noorpro.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.noorpro.app.R
import com.noorpro.app.data.UmmahChat
import com.noorpro.app.data.UmmahMessage
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.ui.theme.*
import com.noorpro.app.ui.viewmodel.DeenViewModel

private val ummahCategories = listOf("All", "Reminder", "Knowledge", "Question", "Dua Request", "Event", "Charity")
private enum class UmmahView(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FEED("Feed", Icons.Default.DynamicFeed),
    REELS("Reels", Icons.Default.SmartDisplay),
    FRIENDS("Friends", Icons.Default.Groups),
    TRENDS("Trends", Icons.Default.TrendingUp),
    SAVED("Saved", Icons.Default.Bookmarks),
    ACCOUNT("Account", Icons.Default.AccountCircle),
    PAGES("Pages", Icons.Default.Flag),
    MEMORIES("Memories", Icons.Default.History),
    GROUPS("Groups", Icons.Default.Groups2),
    AVATAR("Avatar", Icons.Default.Face),
    HELP("Help", Icons.Default.Help),
    UMMAH_SETTINGS("Settings", Icons.Default.PrivacyTip)
}

private val ummahTopTabs = listOf(
    UmmahView.FEED,
    UmmahView.REELS,
    UmmahView.FRIENDS,
    UmmahView.TRENDS,
    UmmahView.SAVED,
    UmmahView.ACCOUNT
)

private data class UmmahPageDraft(val name: String, val category: String, val description: String)
private data class UmmahMemoryDraft(val title: String, val note: String)
private data class UmmahGroupDraft(val name: String, val privacy: String, val description: String)
private data class FollowedUmmahUser(val uid: String, val name: String, val handle: String)

@Composable
private fun UmmahComingSoonScreen(viewModel: DeenViewModel) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pageBackground = if (isLightTheme) {
        Brush.verticalGradient(listOf(Color(0xFFFFF7FB), Color(0xFFFFEAF3), Color(0xFFFFFFFF)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF050B18), Color(0xFF0B1628), Color(0xFF111827)))
    }
    val heroGradient = if (isLightTheme) {
        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFEAF3), Color(0xFFFFF7FB)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF172845), Color(0xFF07101F), Color(0xFF2A213F)))
    }
    val cardColor = if (isLightTheme) Color.White.copy(alpha = .92f) else Color.White.copy(alpha = .07f)
    val cardBorder = if (isLightTheme) Color(0xFFF1C8D8) else Color.White.copy(alpha = .08f)
    val primaryText = if (isLightTheme) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryText = if (isLightTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = .72f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, top = 48.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "Noor Pro logo",
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NOOR PRO", color = MatteGold, fontSize = 11.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Black)
                        Text("Premium Ummah", color = primaryText, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("A beautiful Muslim social experience is coming soon.", color = secondaryText, fontSize = 12.sp)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(30.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .34f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(heroGradient)
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(52.dp).clip(CircleShape).background(MatteGold), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF0B1220), modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Coming Soon", color = primaryText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Text("Reels, community feed, safe sharing, and curated Islamic trends.", color = secondaryText, fontSize = 12.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ComingSoonPill("Reels", Icons.Default.SmartDisplay, Modifier.weight(1f), isLightTheme)
                                ComingSoonPill("Feed", Icons.Default.DynamicFeed, Modifier.weight(1f), isLightTheme)
                                ComingSoonPill("Trends", Icons.Default.TrendingUp, Modifier.weight(1f), isLightTheme)
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("What we will build next", color = primaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        ComingSoonRow(Icons.Default.VerifiedUser, "Moderated Muslim community", isLightTheme)
                        ComingSoonRow(Icons.Default.VideoLibrary, "Vertical reels with real uploaded videos", isLightTheme)
                        ComingSoonRow(Icons.Default.Favorite, "Likes, saved posts, comments, reporting", isLightTheme)
                        ComingSoonRow(Icons.Default.Security, "Safe content review before publishing", isLightTheme)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoonPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = false
) {
    val textColor = if (isLightTheme) MaterialTheme.colorScheme.onSurface else Color.White
    val surfaceColor = if (isLightTheme) Color.White.copy(alpha = .86f) else Color.White.copy(alpha = .08f)
    val borderColor = if (isLightTheme) Color(0xFFF1C8D8) else Color.White.copy(alpha = .08f)
    Surface(
        modifier = modifier,
        color = surfaceColor,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MatteGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ComingSoonRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, isLightTheme: Boolean = false) {
    val rowTextColor = if (isLightTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = .82f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(MatteGold.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MatteGold, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = rowTextColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun UmmahCommunityScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val repository = remember { UmmahRepository() }
    var posts by remember { mutableStateOf<List<UmmahPost>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedView by remember { mutableStateOf(UmmahView.FEED) }
    var reelsStartId by remember { mutableStateOf<String?>(null) }
    var savedIds by remember { mutableStateOf(emptySet<String>()) }
    var likedIds by remember { mutableStateOf(emptySet<String>()) }
    var blockedIds by remember { mutableStateOf(emptySet<String>()) }
    var showComposer by remember { mutableStateOf(false) }
    var commentPost by remember { mutableStateOf<UmmahPost?>(null) }
    var moderationPost by remember { mutableStateOf<UmmahPost?>(null) }
    var communityRefreshKey by remember { mutableIntStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSideMenu by remember { mutableStateOf(false) }
    var showCreatePage by remember { mutableStateOf(false) }
    var showCreateMemory by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var showEditAccount by remember { mutableStateOf(false) }
    var showMembership by remember { mutableStateOf(false) }
    var avatarStyle by remember { mutableIntStateOf(0) }
    var selectedCreatorHandle by remember { mutableStateOf<String?>(null) }
    var ummahAccountName by remember { mutableStateOf(viewModel.userDisplayName.ifBlank { "Community member" }) }
    var ummahUsername by remember { mutableStateOf(viewModel.userEmail.substringBefore("@").ifBlank { "noor_user" }) }
    val feedListState = rememberLazyListState()
    var feedLeftTop by remember { mutableStateOf(false) }
    val followedUsers = remember { mutableStateListOf<FollowedUmmahUser>() }
    fun followPostCreator(post: UmmahPost) {
        if (post.creatorUid.isBlank()) {
            Toast.makeText(context, "This creator profile is missing a chat ID.", Toast.LENGTH_SHORT).show()
            return
        }
        if (followedUsers.none { it.uid == post.creatorUid }) {
            followedUsers.add(FollowedUmmahUser(post.creatorUid, post.creatorName, post.creatorHandle))
        }
        Toast.makeText(context, "Following ${post.creatorHandle.ifBlank { post.creatorName }}", Toast.LENGTH_SHORT).show()
    }
    val viewHistory = remember { mutableStateListOf<UmmahView>() }
    fun navigateUmmah(view: UmmahView) {
        selectedCreatorHandle = null
        if (selectedView != view) {
            viewHistory.add(selectedView)
            selectedView = view
        }
    }
    val pages = remember {
        mutableStateListOf(
            UmmahPageDraft("Noor Reminders", "Reminder", "Daily Islamic reminders and beneficial posts."),
            UmmahPageDraft("Family Circle", "Family", "A simple page for family reminders and duas.")
        )
    }
    val memories = remember {
        mutableStateListOf(
            UmmahMemoryDraft("First Ummah post", "Save meaningful Islamic moments here.")
        )
    }
    val groups = remember {
        mutableStateListOf(
            UmmahGroupDraft("Family", "Private", "Family duas, reminders, and planning."),
            UmmahGroupDraft("Friends", "Private", "Close friends and beneficial reminders.")
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.setUmmahReelsImmersive(false) }
    }

    DisposableEffect(repository, communityRefreshKey) {
        isLoading = true
        error = null
        repository.observeApprovedPosts { result, message ->
            posts = result
            error = message
            isLoading = false
        }
        repository.observeUserInteractions { liked, saved ->
            likedIds = liked
            savedIds = saved
        }
        repository.observeBlockedUsers { blocked ->
            blockedIds = blocked
        }
        onDispose { repository.close() }
    }

    val visiblePosts = posts.filter {
        val viewMatches = when (selectedView) {
            UmmahView.FEED -> it.type == "text" || it.type == "image" || it.type == "video" || it.type == "reel"
            UmmahView.REELS -> it.type == "reel" || it.type == "video"
            UmmahView.FRIENDS -> it.type == "text" || it.type == "image" || it.type == "video" || it.type == "reel"
            UmmahView.TRENDS -> true
            UmmahView.SAVED -> it.id in savedIds
            UmmahView.ACCOUNT -> viewModel.userEmail.isNotBlank() &&
                it.creatorHandle.contains(viewModel.userEmail.substringBefore("@"), ignoreCase = true)
            UmmahView.PAGES,
            UmmahView.MEMORIES,
            UmmahView.GROUPS,
            UmmahView.AVATAR,
            UmmahView.HELP,
            UmmahView.UMMAH_SETTINGS -> false
        }
        val queryMatches = searchQuery.isBlank() ||
            it.caption.contains(searchQuery, ignoreCase = true) ||
            it.arabicText.contains(searchQuery, ignoreCase = true) ||
            it.creatorName.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        val notBlocked = it.creatorUid.isBlank() || it.creatorUid !in blockedIds
        viewMatches && queryMatches && notBlocked && (selectedCategory == "All" || it.category == selectedCategory)
    }.let { filtered ->
        if (selectedView == UmmahView.TRENDS) {
            filtered.sortedByDescending { post -> post.likeCount * 4 + post.commentCount * 6 + post.shareCount * 5 + (post.publishedAt / 1000000000L) }
        } else {
            filtered
        }
    }

    LaunchedEffect(selectedView, visiblePosts.size) {
        viewModel.setUmmahReelsImmersive(selectedView == UmmahView.REELS && visiblePosts.isNotEmpty())
    }

    LaunchedEffect(selectedView, feedListState.firstVisibleItemIndex, feedListState.firstVisibleItemScrollOffset) {
        if (selectedView == UmmahView.FEED) {
            val atTop = feedListState.firstVisibleItemIndex == 0 && feedListState.firstVisibleItemScrollOffset == 0
            if (!atTop) {
                feedLeftTop = true
            } else if (feedLeftTop && !isLoading) {
                feedLeftTop = false
                communityRefreshKey++
            }
        }
    }

    BackHandler(enabled = showSideMenu || selectedCreatorHandle != null || selectedView != UmmahView.FEED) {
        when {
            showSideMenu -> showSideMenu = false
            selectedCreatorHandle != null -> selectedCreatorHandle = null
            selectedView == UmmahView.REELS -> { selectedView = UmmahView.FEED; reelsStartId = null }
            viewHistory.isNotEmpty() -> selectedView = viewHistory.removeAt(viewHistory.lastIndex)
            else -> selectedView = UmmahView.FEED
        }
    }

    if (selectedView == UmmahView.REELS && visiblePosts.isNotEmpty()) {
        UmmahReelsViewer(
            posts = visiblePosts,
            likedIds = likedIds,
            savedIds = savedIds,
            startId = reelsStartId,
            followedCreatorUids = followedUsers.map { it.uid }.toSet(),
            onFeed = { selectedView = UmmahView.FEED; reelsStartId = null },
            onCreate = { if (viewModel.isLoggedIn) showComposer = true else viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.LOGIN) },
            onOpenCreator = { post ->
                selectedCreatorHandle = post.creatorHandle
                selectedView = UmmahView.FEED
            },
            onFollowCreator = { post -> followPostCreator(post) },
            onLike = { post ->
                repository.toggleInteraction(post.id, "likes", post.id !in likedIds) { success ->
                    if (!success) Toast.makeText(context, "Sign in to like reels.", Toast.LENGTH_SHORT).show()
                }
            },
            onSave = { post ->
                repository.toggleInteraction(post.id, "saved", post.id !in savedIds) { success ->
                    if (!success) Toast.makeText(context, "Sign in to save reels.", Toast.LENGTH_SHORT).show()
                }
            },
            onShare = { post -> shareUmmahPost(context, post) },
            onComment = { post -> if (viewModel.isLoggedIn) commentPost = post else viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.LOGIN) },
            onReport = { post -> moderationPost = post }
        )
    } else {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pageBrush = if (isLightTheme) {
        Brush.verticalGradient(listOf(Color(0xFFFFF7FB), Color(0xFFFFFCFA), Color(0xFFFFF2EC)))
    } else {
        Brush.verticalGradient(listOf(NightBackground, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)))
    }
    Box(modifier = Modifier.fillMaxSize().background(pageBrush)) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 48.dp, end = 16.dp, bottom = 8.dp)) {
            val feedTabView = selectedView == UmmahView.FRIENDS ||
                selectedView == UmmahView.TRENDS
            // Feed-like views keep the create/search top bar + tabs; everything else
            // (Saved, Account, Memories, Pages, Groups, Avatar, Help, Settings) is a clean
            // page with just a back button — no create/reels options on top.
            if (feedTabView) {
                UmmahSocialTopBar(
                    selectedView = selectedView,
                    searchOpen = searchOpen,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchClick = { searchOpen = !searchOpen },
                    onCreate = { if (viewModel.isLoggedIn) showComposer = true else viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.LOGIN) },
                    onMenu = { showSideMenu = true },
                    onRefresh = { communityRefreshKey++ }
                )
                Spacer(Modifier.height(10.dp))
                UmmahSocialTabs(
                    selectedView = selectedView,
                    onSelected = { navigateUmmah(it) }
                )
            } else if (selectedView != UmmahView.FEED) {
                UmmahPageHeader(title = selectedView.title) { navigateUmmah(UmmahView.FEED) }
            }
            if (selectedView == UmmahView.ACCOUNT) {
                Spacer(Modifier.height(12.dp))
                UmmahAccountSummary(
                    displayName = ummahAccountName,
                    email = viewModel.userEmail,
                    username = "@$ummahUsername",
                    posts = visiblePosts.size,
                    saved = savedIds.size,
                    pageCount = pages.size,
                    onEditAccount = { showEditAccount = true },
                    onCreatePage = {
                        navigateUmmah(UmmahView.PAGES)
                        showCreatePage = true
                    },
                    onOpenSaved = { navigateUmmah(UmmahView.SAVED) }
                )
            }
        }

        when {
            selectedCreatorHandle != null -> UmmahCreatorDashboard(
                handle = selectedCreatorHandle.orEmpty(),
                posts = posts.filter { it.creatorHandle == selectedCreatorHandle },
                savedIds = savedIds,
                likedIds = likedIds,
                onBack = { selectedCreatorHandle = null },
                isFollowing = posts.firstOrNull { it.creatorHandle == selectedCreatorHandle }?.creatorUid?.let { uid ->
                    followedUsers.any { it.uid == uid }
                } == true,
                onFollow = {
                    posts.firstOrNull { it.creatorHandle == selectedCreatorHandle }?.let { followPostCreator(it) }
                },
                onJoin = {
                    showMembership = true
                },
                onLike = { post ->
                    repository.toggleInteraction(post.id, "likes", post.id !in likedIds) { success ->
                        if (!success) Toast.makeText(context, "Sign in to like posts.", Toast.LENGTH_SHORT).show()
                    }
                },
                onSave = { post ->
                    repository.toggleInteraction(post.id, "saved", post.id !in savedIds) { success ->
                        if (!success) Toast.makeText(context, "Sign in to save posts.", Toast.LENGTH_SHORT).show()
                    }
                },
                onComment = { post -> if (viewModel.isLoggedIn) commentPost = post else viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.LOGIN) },
                onOpenCreator = {},
                onReport = { post -> moderationPost = post }
            )
            selectedView == UmmahView.PAGES -> UmmahPagesManager(
                pages = pages,
                onCreatePage = { showCreatePage = true }
            )
            selectedView == UmmahView.MEMORIES -> UmmahMemoriesManager(
                memories = memories,
                onAddMemory = { showCreateMemory = true }
            )
            selectedView == UmmahView.GROUPS -> UmmahGroupsManager(
                groups = groups,
                onCreateGroup = { showCreateGroup = true }
            )
            selectedView == UmmahView.AVATAR -> UmmahAvatarBuilder(
                displayName = viewModel.userDisplayName.ifBlank { "Community member" },
                avatarStyle = avatarStyle,
                onStyleSelected = { avatarStyle = it }
            )
            selectedView == UmmahView.HELP -> UmmahHelpSupport()
            selectedView == UmmahView.UMMAH_SETTINGS -> UmmahSettingsPrivacy(
                pageCount = pages.size,
                groupCount = groups.size,
                onCreatePage = {
                    navigateUmmah(UmmahView.PAGES)
                    showCreatePage = true
                }
            )
            selectedView == UmmahView.FRIENDS -> UmmahFriendsChatScreen(
                repository = repository,
                followedUsers = followedUsers,
                latestShareablePost = posts.firstOrNull { it.type == "reel" || it.type == "video" || it.mediaUrl.isNotBlank() },
                onRequireLogin = { viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.LOGIN) }
            )
            isLoading && visiblePosts.isEmpty() -> UmmahFeedSkeleton()
            visiblePosts.isEmpty() -> UmmahEmptyState(
                when (selectedView) {
                    UmmahView.REELS -> "No reels yet"
                    UmmahView.TRENDS -> "No trends yet"
                    UmmahView.SAVED -> "No saved posts yet"
                    UmmahView.FRIENDS -> "Friends feed is starting"
                    UmmahView.ACCOUNT -> "No account activity yet"
                    UmmahView.PAGES -> "No pages yet"
                    UmmahView.MEMORIES -> "No memories yet"
                    UmmahView.GROUPS -> "No groups yet"
                    UmmahView.AVATAR -> "Create your avatar"
                    UmmahView.HELP -> "Help & Support"
                    UmmahView.UMMAH_SETTINGS -> "Settings & Privacy"
                    else -> "No posts in $selectedCategory yet"
                },
                if (error != null) "Community sync is reconnecting." else if (selectedView == UmmahView.REELS) "Publish a 9:16 beneficial reel." else "Create the first beneficial post."
            )
            else -> LazyColumn(
                state = feedListState,
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                if (selectedView == UmmahView.FEED) {
                    item {
                        Column(Modifier.padding(start = 6.dp, top = 44.dp, end = 6.dp)) {
                            UmmahSocialTopBar(
                                selectedView = selectedView,
                                searchOpen = searchOpen,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onSearchClick = { searchOpen = !searchOpen },
                                onCreate = { if (viewModel.isLoggedIn) showComposer = true else viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.LOGIN) },
                                onMenu = { showSideMenu = true },
                                onRefresh = { communityRefreshKey++ }
                            )
                            Spacer(Modifier.height(10.dp))
                            UmmahSocialTabs(
                                selectedView = selectedView,
                                onSelected = { navigateUmmah(it) }
                            )
                        }
                    }
                }
                if (error != null) {
                    item {
                        CommunitySyncNotice(
                            onRetry = { communityRefreshKey++ }
                        )
                    }
                }
                if (selectedView == UmmahView.TRENDS) {
                    item {
                        TrendingSpotlightCard(posts = visiblePosts.take(3))
                    }
                }
                items(visiblePosts, key = { it.id }) { post ->
                    if (selectedView == UmmahView.FEED && (post.type == "video" || post.type == "reel")) {
                        UmmahVideoShowcaseCard(
                            post = post,
                            isFollowing = followedUsers.any { it.uid == post.creatorUid },
                            onOpen = { reelsStartId = post.id; selectedView = UmmahView.REELS },
                            onOpenCreator = { selectedCreatorHandle = post.creatorHandle },
                            onFollowCreator = { followPostCreator(post) },
                            onReport = { moderationPost = post }
                        )
                    } else {
                        UmmahPostCardCompact(
                            post = post,
                            isLiked = post.id in likedIds,
                            isSaved = post.id in savedIds,
                            isFollowing = followedUsers.any { it.uid == post.creatorUid },
                            onOpenCreator = { selectedCreatorHandle = post.creatorHandle },
                            onFollowCreator = { followPostCreator(post) },
                            onLike = {
                                val active = post.id !in likedIds
                                repository.toggleInteraction(post.id, "likes", active) { success ->
                                    if (!success) Toast.makeText(context, "Sign in to like posts.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSave = {
                                val active = post.id !in savedIds
                                repository.toggleInteraction(post.id, "saved", active) { success ->
                                    if (!success) Toast.makeText(context, "Sign in to save posts.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            reelMode = selectedView == UmmahView.REELS,
                            onComment = { if (viewModel.isLoggedIn) commentPost = post else viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.LOGIN) },
                            onShare = { shareUmmahPost(context, post) },
                            onReport = { moderationPost = post }
                        )
                    }
                }
            }
        }
    }
        UmmahRightSideMenu(
            visible = showSideMenu,
            displayName = viewModel.userDisplayName.ifBlank { "Community member" },
            email = viewModel.userEmail,
            postCount = posts.size,
            savedCount = savedIds.size,
            pageCount = pages.size,
            onDismiss = { showSideMenu = false },
            onCreatePage = {
                showSideMenu = false
                navigateUmmah(UmmahView.PAGES)
                showCreatePage = true
            },
            onSelect = { view ->
                showSideMenu = false
                navigateUmmah(view)
            },
            onHelp = {
                showSideMenu = false
                navigateUmmah(UmmahView.HELP)
            },
            onSettings = {
                showSideMenu = false
                navigateUmmah(UmmahView.UMMAH_SETTINGS)
            }
        )
    }
    }

    if (showComposer) {
        var isSubmitting by remember { mutableStateOf(false) }
        UmmahComposer(
            onDismiss = { showComposer = false },
            isSubmitting = isSubmitting,
            onSubmit = { caption, arabicText, category, type, mediaUri, source ->
                isSubmitting = true
                repository.submitPost(context, caption, arabicText, category, type, mediaUri, source) { success, message ->
                    isSubmitting = false
                    Toast.makeText(
                        context,
                        if (success) (message ?: "Published to Ummah.") else message ?: "Unable to submit post.",
                        Toast.LENGTH_LONG
                    ).show()
                    if (success) showComposer = false
                }
            }
        )
    }
    commentPost?.let { post ->
        CommentComposer(
            onDismiss = { commentPost = null },
            onSubmit = { text ->
                repository.submitComment(post.id, text) { success ->
                    Toast.makeText(context, if (success) "Comment published." else "Unable to submit comment.", Toast.LENGTH_SHORT).show()
                    if (success) commentPost = null
                }
            }
        )
    }
    moderationPost?.let { post ->
        UmmahModerationDialog(
            post = post,
            onDismiss = { moderationPost = null },
            onReport = { reason ->
                repository.report(post.id, reason) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Report submitted for review. JazakAllah khair." else "Sign in to report content.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                moderationPost = null
            },
            onBlock = {
                repository.setBlocked(post.creatorUid, true) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Blocked. You won't see posts from this account." else "Sign in to block this account.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                moderationPost = null
            }
        )
    }
    if (showCreatePage) {
        UmmahCreatePageDialog(
            onDismiss = { showCreatePage = false },
            onCreate = { draft ->
                pages.add(0, draft)
                selectedView = UmmahView.PAGES
                showCreatePage = false
            }
        )
    }
    if (showCreateMemory) {
        UmmahCreateMemoryDialog(
            onDismiss = { showCreateMemory = false },
            onCreate = { draft ->
                memories.add(0, draft)
                selectedView = UmmahView.MEMORIES
                showCreateMemory = false
            }
        )
    }
    if (showCreateGroup) {
        UmmahCreateGroupDialog(
            onDismiss = { showCreateGroup = false },
            onCreate = { draft ->
                groups.add(0, draft)
                selectedView = UmmahView.GROUPS
                showCreateGroup = false
            }
        )
    }
    if (showEditAccount) {
        UmmahEditAccountDialog(
            name = ummahAccountName,
            username = ummahUsername,
            onDismiss = { showEditAccount = false },
            onSave = { name, username ->
                ummahAccountName = name
                ummahUsername = username
                showEditAccount = false
            }
        )
    }
    if (showMembership) {
        UmmahMembershipDialog(
            handle = selectedCreatorHandle.orEmpty(),
            onDismiss = { showMembership = false },
            onJoin = { plan ->
                showMembership = false
                Toast.makeText(context, "Joined $plan membership", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun UmmahSocialTopBar(
    selectedView: UmmahView,
    searchOpen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCreate: () -> Unit,
    onMenu: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = MatteGold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Ummah", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text(
                    when (selectedView) {
                        UmmahView.FEED -> "Beneficial posts from the community"
                        UmmahView.REELS -> "Short Islamic reels"
                        UmmahView.FRIENDS -> "People and reminders you follow"
                        UmmahView.TRENDS -> "Trending beneficial reminders"
                        UmmahView.SAVED -> "Your saved posts"
                        UmmahView.ACCOUNT -> "Your activity and account"
                        UmmahView.PAGES -> "Your Ummah pages and channels"
                        UmmahView.MEMORIES -> "Your saved community moments"
                        UmmahView.GROUPS -> "Family, friends, and community circles"
                        UmmahView.AVATAR -> "Build your profile identity"
                        UmmahView.HELP -> "Help and support"
                        UmmahView.UMMAH_SETTINGS -> "Settings and privacy"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search Ummah", tint = MatteGold)
            }
            FilledIconButton(onClick = onCreate, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MatteGold, contentColor = Color(0xFF07101F))) {
                Icon(Icons.Default.Add, contentDescription = "Create post")
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Ummah", tint = TextSecondary)
            }
        }
        if (searchOpen) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MatteGold) },
                placeholder = { Text("Search posts, creators, duas, knowledge...") }
            )
        }
    }
}

@Composable
private fun UmmahSocialTabs(
    selectedView: UmmahView,
    onSelected: (UmmahView) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ummahTopTabs) { view ->
            val selected = selectedView == view
            Surface(
                modifier = Modifier.clickable { onSelected(view) },
                shape = RoundedCornerShape(22.dp),
                color = if (selected) MatteGold else MaterialTheme.colorScheme.surface.copy(alpha = .72f),
                contentColor = if (selected) Color(0xFF07101F) else TextPrimary,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MatteGold else GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(view.icon, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(view.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun UmmahAccountSummary(
    displayName: String,
    email: String,
    username: String,
    posts: Int,
    saved: Int,
    pageCount: Int,
    onEditAccount: () -> Unit,
    onCreatePage: () -> Unit,
    onOpenSaved: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .78f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(MatteGold), contentAlignment = Alignment.Center) {
                    Text(displayName.take(1).uppercase(), color = Color(0xFF07101F), fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(username, color = MatteGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(email.ifBlank { "Sign in to sync community activity" }, color = TextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(posts.toString(), color = MatteGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Posts", color = TextSecondary, fontSize = 10.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(saved.toString(), color = MatteGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Saved", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .78f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, null, tint = MatteGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Pages", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Create Islamic pages for masjid, learning, charity, or reminders.", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(pageCount.toString(), color = MatteGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onCreatePage, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create page")
                    }
                    OutlinedButton(onClick = onEditAccount, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit account")
                    }
                }
                OutlinedButton(onClick = onOpenSaved, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Bookmarks, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Saved content")
                }
            }
        }
    }
}

@Composable
private fun UmmahRightSideMenu(
    visible: Boolean,
    displayName: String,
    email: String,
    postCount: Int,
    savedCount: Int,
    pageCount: Int,
    onDismiss: () -> Unit,
    onCreatePage: () -> Unit,
    onSelect: (UmmahView) -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .38f))
                    .clickable { onDismiss() }
            )
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(.86f)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, top = 42.dp, end = 18.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(58.dp).clip(CircleShape).background(MatteGold), contentAlignment = Alignment.Center) {
                                    Text(displayName.take(1).uppercase(), color = Color(0xFF07101F), fontWeight = FontWeight.Black, fontSize = 24.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(displayName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    Text(email.ifBlank { "Ummah account" }, color = TextSecondary, fontSize = 12.sp)
                                }
                                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = TextSecondary) }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                UmmahMenuStat("Posts", postCount, Modifier.weight(1f))
                                UmmahMenuStat("Saved", savedCount, Modifier.weight(1f))
                                UmmahMenuStat("Pages", pageCount, Modifier.weight(1f))
                            }
                        }
                        item {
                            Button(onClick = onCreatePage, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                Icon(Icons.Default.AddBusiness, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Create page")
                            }
                        }
                        item { Text("Menu", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                UmmahMenuRow(Icons.Default.DynamicFeed, "Feed", "Community posts", { onSelect(UmmahView.FEED) })
                                UmmahMenuRow(Icons.Default.SmartDisplay, "Reels", "Short videos", { onSelect(UmmahView.REELS) })
                                UmmahMenuRow(Icons.Default.Bookmarks, "Saved", "Posts, reels, and images", { onSelect(UmmahView.SAVED) })
                                UmmahMenuRow(Icons.Default.Flag, "Pages", "Your Ummah channels", { onSelect(UmmahView.PAGES) })
                                UmmahMenuRow(Icons.Default.History, "Memories", "Moments you add", { onSelect(UmmahView.MEMORIES) })
                                UmmahMenuRow(Icons.Default.Groups, "Groups", "Family and friends circles", { onSelect(UmmahView.GROUPS) })
                                UmmahMenuRow(Icons.Default.Face, "Avatars", "Profile identity", { onSelect(UmmahView.AVATAR) })
                            }
                        }
                        item { Text("More", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                UmmahMenuRow(Icons.Default.Help, "Help & Support", "Community help center", onHelp)
                                UmmahMenuRow(Icons.Default.PrivacyTip, "Settings & Privacy", "Control account and safety", onSettings)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahMenuStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MatteGold.copy(alpha = .12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .22f))
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = MatteGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UmmahMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MatteGold.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MatteGold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun UmmahFriendsChatScreen(
    repository: UmmahRepository,
    followedUsers: List<FollowedUmmahUser>,
    latestShareablePost: UmmahPost?,
    onRequireLogin: () -> Unit
) {
    val context = LocalContext.current
    val currentUid = repository.currentUserUid()
    var chats by remember { mutableStateOf<List<UmmahChat>>(emptyList()) }
    var chatError by remember { mutableStateOf<String?>(null) }
    var selectedChat by remember { mutableStateOf<UmmahChat?>(null) }
    var showStartChat by remember { mutableStateOf(false) }

    LaunchedEffect(currentUid) {
        if (currentUid == null) {
            chatError = "Please sign in to use Friends chat."
        }
    }

    DisposableEffect(repository, currentUid) {
        if (currentUid != null) {
            repository.observeChats { result, message ->
                chats = result
                chatError = message
            }
        }
        onDispose { }
    }

    BackHandler(enabled = selectedChat != null) {
        selectedChat = null
    }

    selectedChat?.let { chat ->
        UmmahChatThread(
            repository = repository,
            chat = chat,
            currentUid = currentUid.orEmpty(),
            latestShareablePost = latestShareablePost,
            onBack = { selectedChat = null },
            onRequireLogin = onRequireLogin
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            UmmahSectionHeader(
                icon = Icons.Default.Chat,
                title = "Friends Chat",
                subtitle = "Private 1:1 messages with text, photos, videos, and shared reels.",
                action = "Start",
                onAction = { if (currentUid == null) onRequireLogin() else showStartChat = true }
            )
        }
        if (chatError != null) {
            item {
                CommunitySyncNotice(onRetry = {
                    if (currentUid == null) onRequireLogin()
                })
            }
        }
        if (followedUsers.isNotEmpty()) {
            item {
                Text("Following", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            items(followedUsers, key = { it.uid }) { user ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (currentUid == null) {
                            onRequireLogin()
                        } else {
                            selectedChat = UmmahChat(
                                id = listOf(currentUid, user.uid).sorted().joinToString("_"),
                                participantUids = listOf(currentUid, user.uid),
                                participantNames = mapOf(currentUid to "You", user.uid to user.name),
                                lastMessage = "",
                                lastMessageType = "text",
                                lastSenderUid = "",
                                updatedAt = 0L
                            )
                        }
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .22f))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(MatteGold.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                            Text(user.name.take(1).uppercase(), color = MatteGold, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.name, color = TextPrimary, fontWeight = FontWeight.Black)
                            Text("${user.handle} • Following", color = TextSecondary, fontSize = 12.sp)
                        }
                        Button(onClick = {
                            if (currentUid == null) onRequireLogin() else selectedChat = UmmahChat(
                                id = listOf(currentUid, user.uid).sorted().joinToString("_"),
                                participantUids = listOf(currentUid, user.uid),
                                participantNames = mapOf(currentUid to "You", user.uid to user.name),
                                lastMessage = "",
                                lastMessageType = "text",
                                lastSenderUid = "",
                                updatedAt = 0L
                            )
                        }) {
                            Text("Message")
                        }
                    }
                }
            }
        }
        if (chats.isEmpty()) {
            item {
                UmmahManagementCard(
                    icon = Icons.Default.Groups,
                    title = "No chats yet",
                    subtitle = "Start with a friend user ID",
                    body = "Enter your friend's Firebase user ID to create a private chat. Future follow/friend discovery can fill this automatically.",
                    footer = "Messages appear instantly with Firestore listeners."
                )
            }
        } else {
            items(chats, key = { it.id }) { chat ->
                val otherUid = chat.participantUids.firstOrNull { it != currentUid }.orEmpty()
                val otherName = chat.participantNames[otherUid] ?: "Friend"
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { selectedChat = chat },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(MatteGold), contentAlignment = Alignment.Center) {
                            Text(otherName.take(1).uppercase(), color = Color(0xFF07101F), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(otherName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text(chat.lastMessage.ifBlank { "Start the conversation" }, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                        if (chat.unreadCount > 0) {
                            Surface(shape = CircleShape, color = MatteGold) {
                                Text(chat.unreadCount.toString(), color = Color(0xFF07101F), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartChat) {
        UmmahStartChatDialog(
            onDismiss = { showStartChat = false },
            onStart = { uid, name ->
                val chatId = repository.chatIdFor(uid)
                if (chatId == null) {
                    onRequireLogin()
                } else {
                    selectedChat = UmmahChat(
                        id = chatId,
                        participantUids = listOf(currentUid.orEmpty(), uid.trim()),
                        participantNames = mapOf(currentUid.orEmpty() to "You", uid.trim() to name.ifBlank { "Friend" }),
                        lastMessage = "",
                        lastMessageType = "text",
                        lastSenderUid = "",
                        updatedAt = 0L
                    )
                    showStartChat = false
                    Toast.makeText(context, "Chat ready. Send a message to create it.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun UmmahChatThread(
    repository: UmmahRepository,
    chat: UmmahChat,
    currentUid: String,
    latestShareablePost: UmmahPost?,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val context = LocalContext.current
    var messages by remember(chat.id) { mutableStateOf<List<UmmahMessage>>(emptyList()) }
    var messageError by remember(chat.id) { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var pendingMedia by remember { mutableStateOf<Uri?>(null) }
    var pendingType by remember { mutableStateOf("text") }
    var isSending by remember { mutableStateOf(false) }
    val otherUid = chat.participantUids.firstOrNull { it != currentUid }.orEmpty()
    val otherName = chat.participantNames[otherUid] ?: "Friend"
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingMedia = uri
    }

    DisposableEffect(chat.id) {
        repository.observeMessages(chat.id) { result, message ->
            messages = result
            messageError = message
        }
        onDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MatteGold) }
            Box(Modifier.size(42.dp).clip(CircleShape).background(MatteGold), contentAlignment = Alignment.Center) {
                Text(otherName.take(1).uppercase(), color = Color(0xFF07101F), fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(otherName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("Private chat", color = TextSecondary, fontSize = 11.sp)
            }
        }
        if (messageError != null) {
            Text(messageError.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                UmmahMessageBubble(message = message, mine = message.senderUid == currentUid)
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pendingMedia != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = MatteGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${pendingType.replaceFirstChar { it.uppercase() }} selected", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { pendingMedia = null; pendingType = "text" }) { Text("Remove") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AssistChip(onClick = { pendingType = "image"; picker.launch("image/*") }, label = { Text("Image") }, leadingIcon = { Icon(Icons.Default.Image, null, Modifier.size(15.dp)) })
                    AssistChip(onClick = { pendingType = "video"; picker.launch("video/*") }, label = { Text("Video") }, leadingIcon = { Icon(Icons.Default.VideoLibrary, null, Modifier.size(15.dp)) })
                    if (latestShareablePost != null) {
                        AssistChip(
                            onClick = {
                                repository.sendChatMessage(
                                    context = context,
                                    otherUid = otherUid,
                                    otherName = otherName,
                                    text = latestShareablePost.caption.ifBlank { "Shared from Ummah" },
                                    type = if (latestShareablePost.type == "reel") "reel" else latestShareablePost.type,
                                    mediaUri = null,
                                    sharedMediaUrl = latestShareablePost.mediaUrl
                                ) { success, message ->
                                    Toast.makeText(context, if (success) "Shared." else message ?: "Unable to share.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text("Share post") },
                            leadingIcon = { Icon(Icons.Default.IosShare, null, Modifier.size(15.dp)) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 2000) draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        minLines = 1,
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        enabled = !isSending && (draft.isNotBlank() || pendingMedia != null),
                        onClick = {
                            if (currentUid.isBlank()) {
                                onRequireLogin()
                            } else {
                                isSending = true
                                repository.sendChatMessage(
                                    context = context,
                                    otherUid = otherUid,
                                    otherName = otherName,
                                    text = draft,
                                    type = pendingType,
                                    mediaUri = pendingMedia
                                ) { success, message ->
                                    isSending = false
                                    if (success) {
                                        draft = ""
                                        pendingMedia = null
                                        pendingType = "text"
                                    } else {
                                        Toast.makeText(context, message ?: "Unable to send.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahMessageBubble(message: UmmahMessage, mine: Boolean) {
    val bubbleColor = if (mine) MatteGold else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
    val textColor = if (mine) Color(0xFF07101F) else TextPrimary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (mine) 18.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 18.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (message.mediaUrl.isNotBlank()) {
                    if (message.type == "image") {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = message.text,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayCircle, null, tint = textColor)
                            Spacer(Modifier.width(6.dp))
                            Text(if (message.type == "reel") "Shared reel" else "Shared video", color = textColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (message.text.isNotBlank()) {
                    Text(
                        message.text,
                        color = textColor,
                        textAlign = if (message.text.any { it in '\u0600'..'\u06FF' }) TextAlign.End else TextAlign.Start,
                        style = LocalTextStyle.current.copy(textDirection = if (message.text.any { it in '\u0600'..'\u06FF' }) TextDirection.Rtl else TextDirection.Ltr)
                    )
                }
            }
        }
    }
}

@Composable
private fun UmmahStartChatDialog(
    onDismiss: () -> Unit,
    onStart: (String, String) -> Unit
) {
    var uid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Friends chat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter your friend's Firebase user ID. When follower profiles are synced, this can become a one-tap friend picker.", color = TextSecondary, fontSize = 12.sp)
                TextField(value = uid, onValueChange = { uid = it.trim() }, label = { Text("Friend user ID") }, singleLine = true)
                TextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onStart(uid, name) }, enabled = uid.isNotBlank()) { Text("Start") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UmmahPagesManager(
    pages: List<UmmahPageDraft>,
    onCreatePage: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UmmahSectionHeader(
                icon = Icons.Default.Flag,
                title = "Your Pages",
                subtitle = "Create pages like channels for reminders, masjid updates, learning, charity, or community work.",
                action = "Create page",
                onAction = onCreatePage
            )
        }
        items(pages) { page ->
            UmmahManagementCard(
                icon = Icons.Default.Campaign,
                title = page.name,
                subtitle = "${page.category} page",
                body = page.description,
                footer = "Ready to publish posts, reels, and updates"
            )
        }
    }
}

@Composable
private fun UmmahMemoriesManager(
    memories: List<UmmahMemoryDraft>,
    onAddMemory: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UmmahSectionHeader(
                icon = Icons.Default.History,
                title = "Memories",
                subtitle = "Keep meaningful reminders, moments, duas, and milestones in one calm place.",
                action = "Add memory",
                onAction = onAddMemory
            )
        }
        items(memories) { memory ->
            UmmahManagementCard(
                icon = Icons.Default.AutoStories,
                title = memory.title,
                subtitle = "Private memory",
                body = memory.note,
                footer = "Only visible on this device for now"
            )
        }
    }
}

@Composable
private fun UmmahGroupsManager(
    groups: List<UmmahGroupDraft>,
    onCreateGroup: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UmmahSectionHeader(
                icon = Icons.Default.Groups,
                title = "Groups",
                subtitle = "Create circles for family, friends, study groups, masjid teams, and local community.",
                action = "Create group",
                onAction = onCreateGroup
            )
        }
        items(groups) { group ->
            UmmahManagementCard(
                icon = if (group.privacy == "Private") Icons.Default.Lock else Icons.Default.Public,
                title = group.name,
                subtitle = group.privacy,
                body = group.description,
                footer = "Invite followers and organize beneficial sharing"
            )
        }
    }
}

@Composable
private fun UmmahAvatarBuilder(
    displayName: String,
    avatarStyle: Int,
    onStyleSelected: (Int) -> Unit
) {
    val styles = listOf(
        "Gold Initial" to Brush.linearGradient(listOf(MatteGold, Color(0xFFFFE8A8))),
        "Night Noor" to Brush.linearGradient(listOf(Color(0xFF172845), Color(0xFF0B1220))),
        "Rose Light" to Brush.linearGradient(listOf(Color(0xFFFFDCE8), Color(0xFFFFFFFF))),
        "Community" to Brush.linearGradient(listOf(Color(0xFF6D5DFB), MatteGold))
    )
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UmmahSectionHeader(
                icon = Icons.Default.Face,
                title = "Avatar",
                subtitle = "Choose a simple premium identity for your Ummah profile.",
                action = null,
                onAction = {}
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .82f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .24f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(styles[avatarStyle].second),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(displayName.take(1).uppercase(), color = if (avatarStyle == 2) Color(0xFF111827) else Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    }
                    Text(displayName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Profile avatar preview", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                styles.forEachIndexed { index, style ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onStyleSelected(index) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (avatarStyle == index) MatteGold.copy(alpha = .18f) else MaterialTheme.colorScheme.surface.copy(alpha = .72f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (avatarStyle == index) MatteGold else GlassBorder)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(style.second))
                            Spacer(Modifier.width(12.dp))
                            Text(style.first, color = TextPrimary, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            if (avatarStyle == index) Icon(Icons.Default.CheckCircle, null, tint = MatteGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahHelpSupport() {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UmmahSectionHeader(
                icon = Icons.Default.Help,
                title = "Help & Support",
                subtitle = "Get help for posting, reels, reports, privacy, and account safety.",
                action = null,
                onAction = {}
            )
        }
        item { UmmahManagementCard(Icons.Default.PostAdd, "Create posts", "Guide", "Use the plus button to publish feed posts, images, videos, or reels instantly.", "More help coming soon") }
        item { UmmahManagementCard(Icons.Default.Report, "Report content", "Safety", "Tap the menu on any post to report it with a reason. Reports are reviewed by the app owner.", "Community safety") }
        item { UmmahManagementCard(Icons.Default.SupportAgent, "Contact support", "Support", "Use Profile > Feedback for now. A dedicated support form can be connected next.", "Noor Pro support") }
    }
}

@Composable
private fun UmmahSettingsPrivacy(
    pageCount: Int,
    groupCount: Int,
    onCreatePage: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UmmahSectionHeader(
                icon = Icons.Default.PrivacyTip,
                title = "Settings & Privacy",
                subtitle = "Manage privacy for both your personal Ummah account and pages.",
                action = "New page",
                onAction = onCreatePage
            )
        }
        item { UmmahManagementCard(Icons.Default.Person, "Account privacy", "Personal", "Control profile visibility, saved content, memories, and future followers.", "Personal settings") }
        item { UmmahManagementCard(Icons.Default.Flag, "Page settings", "$pageCount pages", "Manage page names, categories, posting permissions, and future admin roles.", "Pages") }
        item { UmmahManagementCard(Icons.Default.Groups, "Group settings", "$groupCount groups", "Set group privacy for family, friends, public learning circles, and invitations.", "Groups") }
        item { UmmahManagementCard(Icons.Default.Security, "Safety controls", "Protection", "Report a post or block an account from its menu. Blocked accounts are hidden from your feed.", "Safety") }
    }
}

@Composable
private fun UmmahSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    action: String?,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .34f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF172845), Color(0xFF07101F), Color(0xFF2A213F))))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(MatteGold), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color(0xFF07101F), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text(subtitle, color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                }
                if (action != null) {
                    Button(onClick = onAction, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MatteGold, contentColor = Color(0xFF07101F))) {
                        Text(action)
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahManagementCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    body: String,
    footer: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .82f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(MatteGold.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MatteGold, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(subtitle, color = MatteGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
            }
            Text(body, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            HorizontalDivider(color = GlassBorder)
            Text(footer, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UmmahModerationDialog(
    post: UmmahPost,
    onDismiss: () -> Unit,
    onReport: (String) -> Unit,
    onBlock: () -> Unit
) {
    val reasons = listOf(
        "Spam or misleading",
        "Harmful or abusive",
        "Hate speech",
        "Nudity or sexual content",
        "False religious information",
        "Other"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    val creatorLabel = post.creatorHandle.ifBlank { post.creatorName }.ifBlank { "this account" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report or block") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Why are you reporting this post by $creatorLabel?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onBlock,
                    enabled = post.creatorUid.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Block $creatorLabel")
                }
            }
        },
        confirmButton = { Button(onClick = { onReport(selectedReason) }) { Text("Submit report") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UmmahCreatePageDialog(
    onDismiss: () -> Unit,
    onCreate: (UmmahPageDraft) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Reminder") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Ummah page") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Page name") }, singleLine = true)
                TextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                TextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onCreate(UmmahPageDraft(name.trim(), category.ifBlank { "Community" }.trim(), description.ifBlank { "A beneficial Ummah page." }.trim())) }
            ) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UmmahCreateMemoryDialog(
    onDismiss: () -> Unit,
    onCreate: (UmmahMemoryDraft) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Memory title") }, singleLine = true)
                TextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = { onCreate(UmmahMemoryDraft(title.trim(), note.ifBlank { "A meaningful Ummah moment." }.trim())) }
            ) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UmmahCreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (UmmahGroupDraft) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("Private") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Group name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = privacy == "Private", onClick = { privacy = "Private" }, label = { Text("Private") })
                    FilterChip(selected = privacy == "Public", onClick = { privacy = "Public" }, label = { Text("Public") })
                }
                TextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onCreate(UmmahGroupDraft(name.trim(), privacy, description.ifBlank { "A beneficial group." }.trim())) }
            ) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UmmahEditAccountDialog(
    name: String,
    username: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var draftName by remember { mutableStateOf(name) }
    var draftUsername by remember { mutableStateOf(username.removePrefix("@")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Ummah account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(value = draftName, onValueChange = { draftName = it }, label = { Text("Account name") }, singleLine = true)
                TextField(
                    value = draftUsername,
                    onValueChange = { draftUsername = it.filter { char -> char.isLetterOrDigit() || char == '_' }.take(24) },
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    singleLine = true
                )
                Text("This updates the local Ummah profile preview. Firestore profile sync can be connected next.", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = draftName.isNotBlank() && draftUsername.isNotBlank(),
                onClick = { onSave(draftName.trim(), draftUsername.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UmmahMembershipDialog(
    handle: String,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var selectedPlan by remember { mutableStateOf("Supporter") }
    val plans = listOf(
        "Supporter" to "Member-only posts and channel updates",
        "Learning Circle" to "Private lessons, long videos, and study notes",
        "Premium Ummah" to "All member content, early videos, and private reels"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join ${handle.ifBlank { "channel" }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose a custom membership to unlock member-only Islamic content.", color = TextSecondary, fontSize = 12.sp)
                plans.forEach { (plan, detail) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { selectedPlan = plan },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedPlan == plan) MatteGold.copy(alpha = .16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedPlan == plan) MatteGold else GlassBorder)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (selectedPlan == plan) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = MatteGold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(plan, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text(detail, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onJoin(selectedPlan) }) { Text("Join") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UmmahPremiumHero(
    posts: List<UmmahPost>,
    onCreate: () -> Unit,
    onReels: () -> Unit,
    onTrends: () -> Unit
) {
    val reels = posts.count { it.type == "reel" }
    val trendingScore = posts.sumOf { it.likeCount + it.commentCount + it.shareCount }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .42f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF19283D), Color(0xFF0B111F), Color(0xFF231B37))))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MatteGold, Color(0xFFFFE7A3)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF111827), modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ummah Circle", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("Create beautifully. Share responsibly.", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                    }
                    AssistChip(
                        onClick = onCreate,
                        label = { Text("Post") },
                        leadingIcon = { Icon(Icons.Default.AddCircle, null, Modifier.size(16.dp)) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PremiumMetric("Posts", posts.size.toString(), Icons.Default.DynamicFeed, Modifier.weight(1f))
                    PremiumMetric("Reels", reels.toString(), Icons.Default.SmartDisplay, Modifier.weight(1f))
                    PremiumMetric("Trend", formatEngagement(trendingScore), Icons.Default.TrendingUp, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.EditNote, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create")
                    }
                    OutlinedButton(onClick = onReels, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.SmartDisplay, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reels")
                    }
                    OutlinedButton(onClick = onTrends, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.TrendingUp, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Trends")
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = .08f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .08f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MatteGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Column {
                Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(label, color = Color.White.copy(alpha = .62f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun UmmahUploadDock(onText: () -> Unit, onReel: () -> Unit, onTrend: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PremiumShortcut("Text", Icons.Default.EditNote, Color(0xFF7C5CFF), onText, Modifier.weight(1f))
        PremiumShortcut("Photo", Icons.Default.Image, Color(0xFF2E9A75), onText, Modifier.weight(1f))
        PremiumShortcut("Reel", Icons.Default.SmartDisplay, Color(0xFFE0A82E), onReel, Modifier.weight(1f))
        PremiumShortcut("Trend", Icons.Default.TrendingUp, Color(0xFFB06C93), onTrend, Modifier.weight(1f))
    }
}

@Composable
private fun PremiumShortcut(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .32f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(5.dp))
            Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TrendingSpotlightCard(posts: List<UmmahPost>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .28f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, tint = MatteGold)
                Spacer(Modifier.width(8.dp))
                Text("Trending in Ummah", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            if (posts.isEmpty()) {
                Text("Like, comment, and save good posts to start trends.", color = TextSecondary, fontSize = 12.sp)
            } else {
                posts.forEachIndexed { index, post ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(MatteGold.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                            Text("#${index + 1}", color = MatteGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(post.caption.ifBlank { post.category }, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 12.sp)
                            Text("${post.creatorHandle}  ${formatEngagement(post.likeCount + post.commentCount + post.shareCount)} interactions", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahReelsStrip(
    reels: List<UmmahPost>,
    onOpenReels: () -> Unit,
    onOpenCreator: (UmmahPost) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .94f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .22f))
    ) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SmartDisplay, null, tint = MatteGold, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reels", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("${reels.size} short videos available", color = TextSecondary, fontSize = 11.sp)
                }
                TextButton(onClick = onOpenReels) { Text("View all") }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reels, key = { it.id }) { reel ->
                    Box(
                        modifier = Modifier
                            .width(116.dp)
                            .height(184.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black)
                            .clickable { onOpenReels() }
                    ) {
                        if (reel.thumbnailUrl.isNotBlank() || reel.mediaUrl.isNotBlank()) {
                            AsyncImage(
                                model = reel.thumbnailUrl.ifBlank { reel.mediaUrl },
                                contentDescription = reel.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .82f)))))
                        Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(34.dp))
                        Column(Modifier.align(Alignment.BottomStart).padding(9.dp)) {
                            Text(reel.caption.ifBlank { "Reel" }, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 2)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.clickable { onOpenCreator(reel) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(18.dp).clip(CircleShape).background(MatteGold), contentAlignment = Alignment.Center) {
                                    Text(reel.creatorName.take(1).uppercase(), color = Color(0xFF07101F), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(reel.creatorHandle, color = Color.White.copy(alpha = .82f), fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunitySyncNotice(onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Community sync is reconnecting", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Approved community posts will return when sync reconnects.", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .72f), fontSize = 10.sp)
            }
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun UmmahCategoryStory(category: String, selected: Boolean, onClick: () -> Unit) {
    val color = categoryColor(category)
    Column(
        modifier = Modifier.width(76.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(color, MatteGold, Color.White.copy(alpha = .45f))))
                .padding(2.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(5.dp).clip(CircleShape)
                .background(if (selected) color.copy(alpha = .28f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIcon(category), null, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(category, color = if (selected) color else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun QuickPostCard(displayName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .94f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .22f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MatteGold, MaterialTheme.colorScheme.primary))), contentAlignment = Alignment.Center) {
                    Text(displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Share something beneficial...", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Reel, photo, question, dua request", color = TextSecondary, fontSize = 11.sp)
                }
                FilledIconButton(onClick = onClick, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MatteGold)) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF111827))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onClick, label = { Text("Photo") }, leadingIcon = { Icon(Icons.Default.Image, null, Modifier.size(15.dp)) })
                AssistChip(onClick = onClick, label = { Text("Reel") }, leadingIcon = { Icon(Icons.Default.SmartDisplay, null, Modifier.size(15.dp)) })
                AssistChip(onClick = onClick, label = { Text("Question") }, leadingIcon = { Icon(Icons.Default.Help, null, Modifier.size(15.dp)) })
            }
        }
    }
}

@Composable
private fun UmmahReelsViewer(
    posts: List<UmmahPost>,
    likedIds: Set<String>,
    savedIds: Set<String>,
    startId: String? = null,
    followedCreatorUids: Set<String>,
    onFeed: () -> Unit,
    onCreate: () -> Unit,
    onOpenCreator: (UmmahPost) -> Unit,
    onFollowCreator: (UmmahPost) -> Unit,
    onLike: (UmmahPost) -> Unit,
    onSave: (UmmahPost) -> Unit,
    onShare: (UmmahPost) -> Unit,
    onComment: (UmmahPost) -> Unit,
    onReport: (UmmahPost) -> Unit
) {
    val context = LocalContext.current
    val startIndex = remember(posts, startId) {
        startId?.let { id -> posts.indexOfFirst { it.id == id }.takeIf { it >= 0 } } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { posts.size })
    LaunchedEffect(posts.size) {
        if (posts.isNotEmpty() && pagerState.currentPage >= posts.size) {
            pagerState.scrollToPage(posts.lastIndex)
        }
    }

    // One ExoPlayer is reused for the whole reels session and follows the settled
    // page. A single decoder (instead of one per page) is what keeps scrolling smooth
    // — multiple live ExoPlayers exhaust hardware codecs and cause the stutter/jank.
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    var userPaused by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (!userPaused) player.play() }

    // Load + play only the reel the pager has settled on. settledPage (not currentPage)
    // avoids swapping the video mid-drag while the user is still flinging.
    val activePage = pagerState.settledPage.coerceIn(0, (posts.size - 1).coerceAtLeast(0))
    LaunchedEffect(activePage, posts.size) {
        val post = posts.getOrNull(activePage) ?: return@LaunchedEffect
        userPaused = false
        player.setMediaItem(MediaItem.fromUri(Uri.parse(post.mediaUrl)))
        player.prepare()
        player.seekTo(0)
        player.play()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val post = posts.getOrNull(page) ?: return@VerticalPager
            Box(Modifier.fillMaxSize()) {
                val isFollowing = post.creatorUid in followedCreatorUids
                if (post.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = post.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (page == activePage) {
                    ReelPlayerSurface(
                        player = player,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                userPaused = !userPaused
                                player.playWhenReady = !userPaused
                            }
                    )
                    if (userPaused) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White.copy(alpha = .9f),
                                modifier = Modifier.size(76.dp)
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .22f), Color.Transparent, Color.Black.copy(alpha = .82f)))))
                Column(
                    Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, bottom = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ReelAction(if (post.id in likedIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder, formatEngagement(post.likeCount + if (post.id in likedIds) 1 else 0), post.id in likedIds, { onLike(post) })
                    ReelAction(if (post.id in savedIds) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, if (post.id in savedIds) "Saved" else "Save", post.id in savedIds, { onSave(post) })
                    ReelAction(Icons.Default.ChatBubbleOutline, formatEngagement(post.commentCount), false, { onComment(post) })
                    ReelAction(Icons.Default.Share, formatEngagement(post.shareCount), false, { onShare(post) })
                    ReelAction(Icons.Default.Flag, "Report", false, { onReport(post) })
                }
                Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 76.dp, bottom = 88.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).clickable { onOpenCreator(post) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(categoryColor(post.category)), contentAlignment = Alignment.Center) {
                                Text(post.creatorName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(post.creatorHandle.ifBlank { post.creatorName }, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Verified, null, tint = MatteGold, modifier = Modifier.size(16.dp))
                                }
                                Text(if (post.type == "reel") "Reel" else post.category, color = Color.White.copy(alpha = .68f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            modifier = Modifier.clickable { onFollowCreator(post) },
                            shape = RoundedCornerShape(50),
                            color = if (isFollowing) Color.White.copy(alpha = .18f) else MatteGold
                        ) {
                            Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (isFollowing) Icons.Default.CheckCircle else Icons.Default.PersonAdd, null, tint = if (isFollowing) Color.White else Color(0xFF07101F), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isFollowing) "Following" else "Follow", color = if (isFollowing) Color.White else Color(0xFF07101F), fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (post.arabicText.isNotBlank()) {
                        Text(
                            post.arabicText,
                            color = Color.White,
                            fontSize = 20.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            maxLines = 3,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (post.caption.isNotBlank()) Text(post.caption, color = Color.White, lineHeight = 20.sp, maxLines = 3)
                    if (post.sourceReference.isNotBlank()) Text("Source: ${post.sourceReference}", color = MatteGold, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFeed) { Icon(Icons.Default.ArrowBack, "Feed", tint = Color.White) }
            Text("Reels", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            IconButton(onClick = onCreate) { Icon(Icons.Default.AddBox, "Create reel", tint = Color.White) }
        }
    }
}

// A single PlayerView (inflated as a TextureView) attached to the shared reels player.
// TextureView scrolls without the black SurfaceView z-order flashes a pager produces, and
// RESIZE_MODE_ZOOM fills the vertical frame edge-to-edge like a TikTok/Reels surface.
@OptIn(UnstableApi::class)
@Composable
private fun ReelPlayerSurface(player: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            (android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.ummah_reel_player, null) as PlayerView).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setKeepContentOnPlayerReset(true)
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null },
        modifier = modifier
    )
}

@Composable
private fun ReelAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .38f))
        ) {
            Icon(icon, label, tint = if (active) Color(0xFFFF7285) else Color.White)
        }
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun shareUmmahPost(context: android.content.Context, post: UmmahPost) {
    val shareText = buildString {
        append("Noor Pro Ummah")
        append("\n\n")
        append(post.caption.ifBlank { if (post.type == "reel") "Shared an Islamic reel" else "Shared an Islamic post" })
        if (post.sourceReference.isNotBlank()) append("\n\nSource: ${post.sourceReference}")
        append("\n\nOpen Noor Pro to watch, save, and share beneficial content.")
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }, "Share Ummah post"))
}

@Composable
private fun UmmahCreatorDashboard(
    handle: String,
    posts: List<UmmahPost>,
    savedIds: Set<String>,
    likedIds: Set<String>,
    isFollowing: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    onJoin: () -> Unit,
    onLike: (UmmahPost) -> Unit,
    onSave: (UmmahPost) -> Unit,
    onComment: (UmmahPost) -> Unit,
    onOpenCreator: (UmmahPost) -> Unit,
    onReport: (UmmahPost) -> Unit
) {
    val context = LocalContext.current
    val firstPost = posts.firstOrNull()
    val creatorName = firstPost?.creatorName ?: handle.ifBlank { "Ummah channel" }
    val reelCount = posts.count { it.type == "reel" }
    val videoCount = posts.count { it.type == "video" }
    val totalViews = posts.sumOf { (it.likeCount + it.commentCount + it.shareCount + 1L) * 37L }
    val followerCount = (posts.size * 23L + totalViews / 11L).coerceAtLeast(12L)
    val joinedCount = (posts.size * 4L + totalViews / 97L).coerceAtLeast(3L)
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .34f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF172845), Color(0xFF07101F), Color(0xFF2A213F))))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Ummah", tint = Color.White)
                            }
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier.size(72.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(categoryColor(firstPost?.category ?: "Reminder"), MatteGold))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(creatorName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(creatorName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                    Spacer(Modifier.width(5.dp))
                                    Icon(Icons.Default.Verified, null, tint = MatteGold, modifier = Modifier.size(17.dp))
                                }
                                Text(handle.ifBlank { "@ummah" }, color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                                Text("Beneficial posts, long videos, and reels", color = Color.White.copy(alpha = .62f), fontSize = 11.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            PremiumMetric("Followers", formatEngagement(followerCount), Icons.Default.PersonAdd, Modifier.weight(1f))
                            PremiumMetric("Views", formatEngagement(totalViews), Icons.Default.Visibility, Modifier.weight(1f))
                            PremiumMetric("Joined", formatEngagement(joinedCount), Icons.Default.Groups, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            PremiumMetric("Posts", posts.size.toString(), Icons.Default.DynamicFeed, Modifier.weight(1f))
                            PremiumMetric("Videos", videoCount.toString(), Icons.Default.OndemandVideo, Modifier.weight(1f))
                            PremiumMetric("Reels", reelCount.toString(), Icons.Default.SmartDisplay, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = onFollow,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MatteGold, contentColor = Color(0xFF07101F))
                            ) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isFollowing) "Following" else "Follow")
                            }
                            OutlinedButton(onClick = onJoin, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Join")
                            }
                        }
                    }
                }
            }
        }
        item {
            UmmahMemberOnlyCard(
                handle = handle,
                onJoin = onJoin
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(onClick = {}, label = { Text("All") }, leadingIcon = { Icon(Icons.Default.GridView, null, Modifier.size(15.dp)) })
                AssistChip(onClick = {}, label = { Text("Long videos") }, leadingIcon = { Icon(Icons.Default.OndemandVideo, null, Modifier.size(15.dp)) })
                AssistChip(onClick = {}, label = { Text("Reels") }, leadingIcon = { Icon(Icons.Default.SmartDisplay, null, Modifier.size(15.dp)) })
            }
        }
        if (posts.isEmpty()) {
            item {
                UmmahEmptyState("No channel posts yet", "When this creator publishes, their feed, long videos, and reels will appear here.")
            }
        } else {
            items(posts, key = { it.id }) { post ->
                UmmahPostCardCompact(
                    post = post,
                    isLiked = post.id in likedIds,
                    isSaved = post.id in savedIds,
                    isFollowing = isFollowing,
                    reelMode = false,
                    onOpenCreator = { onOpenCreator(post) },
                    onFollowCreator = onFollow,
                    onLike = { onLike(post) },
                    onSave = { onSave(post) },
                    onComment = { onComment(post) },
                    onShare = { shareUmmahPost(context, post) },
                    onReport = { onReport(post) }
                )
            }
        }
    }
}

@Composable
private fun UmmahPageHeader(
    title: String,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = topPadding, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back to feed", tint = TextPrimary)
        }
        Spacer(Modifier.width(2.dp))
        Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

private fun ummahRelativeTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "now"
    val diff = System.currentTimeMillis() - epochMillis
    val minutes = diff / 60000
    return when {
        diff < 0 || minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h"
        minutes < 10080 -> "${minutes / 1440}d"
        else -> "${minutes / 10080}w"
    }
}

@Composable
private fun UmmahFeedSkeleton() {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 46.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().height(190.dp).background(base))
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(base))
                            Spacer(Modifier.width(9.dp))
                            Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(base))
                        }
                        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(base))
                        Box(Modifier.fillMaxWidth(0.65f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(base))
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahEmptyState(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Public, contentDescription = null, tint = MatteGold, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(message, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun UmmahMemberOnlyCard(
    handle: String,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MatteGold.copy(alpha = .28f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MatteGold.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, null, tint = MatteGold, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Member-only content", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Custom lessons, private videos, and channel updates from ${handle.ifBlank { "@ummah" }}.", color = TextSecondary, fontSize = 12.sp)
            }
            Button(onClick = onJoin, shape = RoundedCornerShape(16.dp)) {
                Text("Membership")
            }
        }
    }
}

@Composable
private fun UmmahVideoShowcaseCard(
    post: UmmahPost,
    isFollowing: Boolean,
    onOpen: () -> Unit,
    onOpenCreator: () -> Unit,
    onFollowCreator: () -> Unit,
    onReport: () -> Unit
) {
    val views = remember(post.id) { (post.likeCount + post.commentCount + post.shareCount + 1L) * 37L }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .97f)),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .58f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (post.type == "reel") 9f / 16f else 16f / 9f)
                    .clickable { onOpen() }
            ) {
                UmmahVideoPlayer(
                    url = post.mediaUrl,
                    autoPlay = false,
                    useController = false,
                    reelMode = post.type == "reel",
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayCircle, contentDescription = "Play video", tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(58.dp))
                }
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    post.caption.ifBlank { if (post.type == "reel") "Short video reel" else "Long video" },
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    maxLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clickable { onOpenCreator() }, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(categoryColor(post.category)), contentAlignment = Alignment.Center) {
                            Text(post.creatorName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(post.creatorName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                            Text("${formatEngagement(views)} views  •  ${post.creatorHandle}", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    TextButton(onClick = onFollowCreator) {
                        Text(if (isFollowing) "Following" else "Follow", color = MatteGold, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = onReport, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahPostCardCompact(
    post: UmmahPost,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    reelMode: Boolean,
    onOpenCreator: () -> Unit,
    onFollowCreator: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .58f))
    ) {
        Column {
            if (post.type == "video" || post.type == "reel") {
                Box {
                    UmmahVideoPlayer(
                        url = post.mediaUrl,
                        autoPlay = reelMode,
                        reelMode = reelMode,
                        modifier = Modifier.fillMaxWidth().aspectRatio(if (post.type == "reel") 9f / 16f else 16f / 9f)
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = .62f)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (post.type == "reel") Icons.Default.SmartDisplay else Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (post.type == "reel") "Reel" else "Long video", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            } else if (post.mediaUrl.isNotBlank()) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = post.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                )
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f).clickable { onOpenCreator() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(categoryColor(post.category), MatteGold))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(post.creatorName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(9.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(post.creatorName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                Spacer(Modifier.width(5.dp))
                                Icon(Icons.Default.Verified, null, tint = MatteGold, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (isFollowing) "Following" else "Follow", color = MatteGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "${ummahRelativeTime(post.publishedAt)}  •  ${if (post.type == "reel") "Reel" else if (post.type == "video") "Video" else post.category}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                    FilledTonalIconButton(onClick = onFollowCreator, modifier = Modifier.size(34.dp)) {
                        Icon(if (isFollowing) Icons.Default.CheckCircle else Icons.Default.PersonAdd, contentDescription = "Follow", tint = MatteGold, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onReport, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
                    }
                }
                if (post.arabicText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        post.arabicText,
                        color = TextPrimary,
                        lineHeight = 32.sp,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (post.caption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(post.caption, color = TextPrimary, lineHeight = 20.sp, fontSize = 14.sp)
                }
                if (post.sourceReference.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Source: ${post.sourceReference}", color = MatteGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    "${formatEngagement(post.likeCount + if (isLiked) 1 else 0)} likes  •  ${formatEngagement(post.commentCount)} comments",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike, modifier = Modifier.size(44.dp)) {
                        Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = if (isLiked) Color(0xFFE06A7A) else TextPrimary, modifier = Modifier.size(26.dp))
                    }
                    IconButton(onClick = onComment, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment", tint = TextPrimary, modifier = Modifier.size(26.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSave, modifier = Modifier.size(44.dp)) {
                        Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "Save", tint = if (isSaved) MatteGold else TextPrimary, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UmmahPostCard(post: UmmahPost, isLiked: Boolean, isSaved: Boolean, reelMode: Boolean, onOpenCreator: () -> Unit, onLike: () -> Unit, onSave: () -> Unit, onComment: () -> Unit, onShare: () -> Unit, onReport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f))
    ) {
        Column {
            if (post.type == "video" || post.type == "reel") {
                Box {
                    UmmahVideoPlayer(
                        url = post.mediaUrl,
                        autoPlay = reelMode,
                        reelMode = reelMode,
                        modifier = Modifier.fillMaxWidth().aspectRatio(if (post.type == "reel") 9f / 16f else 16f / 9f)
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = .62f)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (post.type == "reel") Icons.Default.SmartDisplay else Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (post.type == "reel") "Reel" else "Long video", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            } else if (post.mediaUrl.isNotBlank()) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = post.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                )
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(categoryColor(post.category), MatteGold))).clickable { onOpenCreator() }, contentAlignment = Alignment.Center) {
                        Text(post.creatorName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f).clickable { onOpenCreator() }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(post.creatorName, color = TextPrimary, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.Default.Verified, null, tint = MatteGold, modifier = Modifier.size(15.dp))
                        }
                        Text("${post.creatorHandle}  •  ${post.category}", color = TextSecondary, fontSize = 12.sp)
                    }
                    Surface(color = categoryColor(post.category).copy(alpha = .12f), shape = RoundedCornerShape(50)) {
                        Text(post.category, color = categoryColor(post.category), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                    FilledTonalIconButton(onClick = onOpenCreator, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Follow", tint = MatteGold, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onReport) { Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary) }
                }
                if (post.arabicText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        post.arabicText,
                        color = TextPrimary,
                        lineHeight = 34.sp,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (post.caption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(post.caption, color = TextPrimary, lineHeight = 21.sp, fontSize = 14.sp)
                }
                if (post.sourceReference.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Source: ${post.sourceReference}", color = MatteGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "${formatEngagement(post.likeCount + if (isLiked) 1 else 0)} likes  •  ${formatEngagement(post.commentCount)} comments",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike) {
                        Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = if (isLiked) Color(0xFFE06A7A) else TextPrimary)
                    }
                    IconButton(onClick = onComment) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment", tint = TextPrimary)
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSave) {
                        Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "Save", tint = if (isSaved) MatteGold else TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentComposer(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Add a thoughtful comment", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Keep comments respectful and beneficial.", color = TextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    label = { Text("Your comment") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${text.length}/500", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
            }
        },
        confirmButton = { Button(onClick = { onSubmit(text) }, enabled = text.isNotBlank()) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UmmahComposer(
    onDismiss: () -> Unit,
    isSubmitting: Boolean,
    onSubmit: (String, String, String, String, Uri?, String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var arabicText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Reminder") }
    var postType by remember { mutableStateOf("text") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var source by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        mediaUri = uri
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Create Ummah post", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.heightIn(max = 540.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Publish to Ummah. Keep it beneficial, respectful, and Islamic.", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("text" to "Text", "image" to "Image", "reel" to "Reel", "video" to "Video").forEach { (value, label) ->
                        FilterChip(selected = postType == value, onClick = {
                            postType = value
                            mediaUri = null
                        }, label = { Text(label) })
                    }
                }
                OutlinedTextField(caption, { if (it.length <= 2000) caption = it }, label = { Text("Caption / reminder") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    arabicText,
                    { if (it.length <= 2000) arabicText = it },
                    label = { Text("Arabic text (optional)") },
                    placeholder = { Text("دعاء / آية / ذكر") },
                    minLines = 2,
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, textDirection = TextDirection.Rtl),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ummahCategories.drop(1)) { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) }) }
                }
                if (postType != "text") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                when (postType) {
                                    "reel" -> "Choose a vertical 9:16 video smaller than 100 MB."
                                    "video" -> "Choose a horizontal or vertical video smaller than 100 MB."
                                    else -> "Choose a photo from your phone."
                                },
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = { picker.launch(if (postType == "image") "image/*" else "video/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(if (postType == "image") Icons.Default.Image else Icons.Default.VideoLibrary, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (mediaUri == null) "Choose media" else "Change selected media")
                            }
                            if (mediaUri != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E9A75), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Media selected. Ready to publish.", color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(source, { source = it }, label = { Text("Source or reference, optional") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(caption, arabicText, category, postType, mediaUri, source) },
                enabled = !isSubmitting &&
                    (caption.isNotBlank() || arabicText.isNotBlank() || (postType != "text" && mediaUri != null)) &&
                    (postType == "text" || mediaUri != null)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Publishing")
                } else {
                    Text("Publish now")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") } }
    )
}

private fun formatEngagement(value: Long): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}K"
    else -> value.toString()
}

private fun categoryColor(category: String): Color = when (category) {
    "Knowledge" -> Color(0xFF3E82C4)
    "Question" -> Color(0xFF8E6AC8)
    "Dua Request" -> Color(0xFFB06C93)
    "Event" -> Color(0xFFCE8A37)
    "Charity" -> Color(0xFF2E9A75)
    else -> MatteGold
}

private fun categoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector = when (category) {
    "Knowledge" -> Icons.Default.MenuBook
    "Question" -> Icons.Default.Help
    "Dua Request" -> Icons.Default.VolunteerActivism
    "Event" -> Icons.Default.Event
    "Charity" -> Icons.Default.Favorite
    "All" -> Icons.Default.Public
    else -> Icons.Default.AutoAwesome
}

@OptIn(UnstableApi::class)
@Composable
private fun UmmahVideoPlayer(
    url: String,
    autoPlay: Boolean = false,
    useController: Boolean = true,
    reelMode: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = autoPlay
            prepare()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(autoPlay) {
        player.playWhenReady = autoPlay
        if (!autoPlay) player.pause()
    }
    AndroidView(
        factory = {
            PlayerView(context).apply {
                this.player = player
                this.useController = useController
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        modifier = modifier
    )
}
