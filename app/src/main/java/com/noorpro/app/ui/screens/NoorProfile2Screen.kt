package com.noorpro.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Link
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.data.StoryRepository
import com.noorpro.app.data.UmmahProfile
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * 1.1.5 Profile v2 wired to real data: live ummah_profiles, approved posts, followers/following,
 * saved interactions, stories (ring), the Quran streak, privacy (ummah_profiles isPrivate/hideCounts),
 * and Noor AI (askNoorAi, AskNoor mode) for bio ideas. Behind Remote Config `profile_v2_enabled`.
 */

private const val P2_PREFS = "noor_profile2"

private enum class P2Sheet { QR, PRIVACY, MENU }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NoorProfile2Screen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val features = remember { Profile2Features() }
    val prefs = remember { context.getSharedPreferences(P2_PREFS, Context.MODE_PRIVATE) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val feedState = rememberStitchUmmahFeed(refreshKey)
    val repository = remember { UmmahRepository() }
    DisposableEffect(repository) { onDispose { repository.close() } }
    val myUid = remember(viewModel.isLoggedIn) { repository.currentUserUid().orEmpty() }
    val viewedUid = viewModel.viewedCreatorUid
    val isOther = viewedUid.isNotBlank() && viewedUid != myUid
    val own = !isOther
    val profileUid = if (isOther) viewedUid else myUid

    if (own && !viewModel.isLoggedIn) {
        Box(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().padding(22.dp)) {
            StitchGuestSignInProfileCard(onSignIn = { viewModel.navigateTo(DeenScreen.LOGIN) }, modifier = Modifier.padding(top = 60.dp))
        }
        return
    }

    val liveProfiles = rememberStitchProfiles()
    val followingSet = rememberStitchFollowing(viewModel.isLoggedIn)
    val followers = rememberStitchFollowers(viewModel.isLoggedIn, if (isOther) profileUid else null)
    val followingUsers = rememberStitchFollowingUsers(viewModel.isLoggedIn, if (isOther) profileUid else null)
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    val streak by viewModel.currentStreak.collectAsState()
    val locationName by viewModel.currentLocationName.collectAsState()

    // Story ring: any live story by this member.
    var hasStory by remember(profileUid) { mutableStateOf(false) }
    DisposableEffect(profileUid, viewModel.isLoggedIn) {
        val reg = runCatching { StoryRepository().observeActive { s -> hasStory = s.any { it.authorUid == profileUid } } }.getOrNull()
        onDispose { reg?.remove() }
    }

    // Optimistic follow so the button flips instantly.
    var followOverride by remember(profileUid) { mutableStateOf<Boolean?>(null) }
    val iFollow = followOverride ?: (profileUid in followingSet)
    var privateOn by remember { mutableStateOf(viewModel.ummahPrivateAccount()) }
    var hideCountsOn by remember { mutableStateOf(viewModel.ummahHideCounts()) }
    var hideDeen by remember { mutableStateOf(prefs.getBoolean("hide_deen", false)) }

    val live = liveProfiles[profileUid]
    val profile = if (own) UmmahProfile(
        uid = myUid,
        name = viewModel.userDisplayName.ifBlank { live?.name.orEmpty() }.ifBlank { "NoorPro member" },
        handle = viewModel.ummahUsername.ifBlank { live?.handle.orEmpty() }.ifBlank { viewModel.userEmail.substringBefore("@") },
        photoUrl = viewModel.userPhotoUrl.ifBlank { live?.photoUrl.orEmpty() },
        bio = live?.bio.orEmpty().ifBlank { viewModel.userBio },
        isPrivate = privateOn, hideCounts = hideCountsOn, verified = live?.verified ?: false,
    ) else live ?: UmmahProfile(profileUid, viewModel.viewedCreatorName.ifBlank { "Community member" }, viewModel.viewedCreatorHandle.removePrefix("@"), "")

    val all = feedState.posts
    val posts = remember(all, profileUid, own) { profilePosts(all, profileUid, own) }
    val reels = remember(all, profileUid, own) { profileReels(all, profileUid, own) }
    val tabs = remember(own) { profile2Tabs(own, features.series) }
    var tabName by rememberSaveable(profileUid) { mutableStateOf(Profile2Tab.POSTS.name) }
    val tab = tabs.firstOrNull { it.name == tabName } ?: Profile2Tab.POSTS
    val locked = !canSeeProfileContent(profile, own, iFollow)
    val grid = when (tab) {
        Profile2Tab.POSTS -> posts
        Profile2Tab.REELS -> reels
        Profile2Tab.SAVED -> all.filter { it.id in interactions.saved }
        Profile2Tab.TAGGED -> taggedPosts(all, profileUid, profile.handle)
        Profile2Tab.SERIES -> emptyList()
    }
    val followingCount = if (isOther) followingUsers.size else followingSet.size
    val followerCount = followers.size + when { followOverride == true && profileUid !in followingSet -> 1; followOverride == false && profileUid in followingSet -> -1; else -> 0 }
    val stats = profile2Stats(posts.size + reels.size, followerCount.coerceAtLeast(0), followingCount, if (own) streak else null, own, profile.hideCounts)
    val ui = Profile2Ui(
        profile = profile, own = own, following = iFollow, stats = stats, hasStory = hasStory,
        city = if (own) myCityFrom(locationName) else "", learning = "",
        tabs = tabs, tab = tab, grid = grid,
        series = if (tab == Profile2Tab.SERIES) profileSeries(all, profileUid) else emptyList(),
        locked = locked, highlights = if (features.highlights) listOf("Quran", "Ramadan", "Umrah") else emptyList(),
        deenCard = if (own) DeenCardUi(streak, "", 0, hideDeen) else null,
        recap = if (own && features.aiRecap) weeklyDeenRecap(streak, posts + reels, remember { ReelWatchStore(context).all().count { System.currentTimeMillis() - it.updatedAt < 7L * 86_400_000 } }, System.currentTimeMillis()) else null,
        totalViews = (posts + reels).sumOf { it.viewCount },
        loading = feedState.loading && all.isEmpty(),
        accent = Color(prefs.getInt("accent", R2Green.toArgb())),
    )
    val shareUrl = profileShareUrl(profileUid)
    fun share() {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${profile.name} (${ui.handle}) on NoorPro\n$shareUrl")
        runCatching { context.startActivity(Intent.createChooser(i, "Share profile")) }
    }
    var sheet by remember { mutableStateOf<P2Sheet?>(null) }
    var reportOpen by remember { mutableStateOf(false) }

    val actions = Profile2Actions(
        onBack = { viewModel.goBack() },
        onMenu = { sheet = if (own) P2Sheet.PRIVACY else P2Sheet.MENU },
        onQr = { sheet = P2Sheet.QR },
        onStat = { key ->
            when (key) {
                "posts" -> tabName = Profile2Tab.POSTS.name
                "followers" -> if (!locked) viewModel.navigateTo(DeenScreen.UMMAH_FOLLOWERS)
                "following" -> if (!locked) viewModel.navigateTo(DeenScreen.UMMAH_FOLLOWING)
                "streak" -> viewModel.navigateTo(DeenScreen.QURAN)
            }
        },
        onFollow = {
            if (!viewModel.isLoggedIn) { viewModel.navigateTo(DeenScreen.LOGIN); return@Profile2Actions }
            val next = !iFollow
            followOverride = next
            repository.setFollowing(profileUid, profile.name, ui.handle, next) { ok ->
                if (!ok) { followOverride = null; Toast.makeText(context, "Couldn't update follow", Toast.LENGTH_SHORT).show() }
            }
        },
        onMessage = { if (!viewModel.isLoggedIn) viewModel.navigateTo(DeenScreen.LOGIN) else viewModel.openCreatorChat(profileUid, profile.name) },
        onShare = { share() },
        onEdit = { viewModel.navigateTo(DeenScreen.UMMAH_PROFILE_EDIT) },
        onStudio = { viewModel.navigateTo(DeenScreen.CREATOR_STUDIO) },
        onTab = { tabName = it.name },
        onOpen = { p -> if (p.opensInVideoViewer()) viewModel.openReel(p.id) else viewModel.navigateTo(DeenScreen.UMMAH) },
        onLink = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
        onToggleDeenCard = { hideDeen = !hideDeen; prefs.edit().putBoolean("hide_deen", hideDeen).apply() },
        onAvatar = { if (own) viewModel.navigateTo(DeenScreen.UMMAH_PROFILE_EDIT) },
        onCreate = { viewModel.navigateTo(if (tab == Profile2Tab.REELS) DeenScreen.UMMAH_CREATE_REEL else DeenScreen.UMMAH_CREATE_POST) },
    )
    CompositionLocalProvider(LocalReels2RemoteImages provides true) {
        NoorProfile2(ui, actions, features)
    }

    if (sheet != null) ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = stitchBackground()) {
        when (sheet) {
            P2Sheet.QR -> {
                val qr = remember(shareUrl) { runCatching { msgQrBitmap(shareUrl, 480)?.asImageBitmap() }.getOrNull() }
                Profile2QrCard(profile.name, ui.handle, shareUrl, qr, onShare = { share() }, onCopy = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("NoorPro profile", shareUrl))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                })
            }
            P2Sheet.PRIVACY -> Profile2PrivacySheet(
                privateOn, hideCountsOn, hideDeen, 0, features,
                onPrivate = { privateOn = it; viewModel.setUmmahPrivacy(it, hideCountsOn) },
                onHideCounts = { hideCountsOn = it; viewModel.setUmmahPrivacy(privateOn, it) },
                onHideDeen = { hideDeen = it; prefs.edit().putBoolean("hide_deen", it).apply() },
                onCloseFriends = { Toast.makeText(context, "Close friends is coming soon", Toast.LENGTH_SHORT).show() },
            )
            P2Sheet.MENU -> Reels2SheetFrame(profile.name, ui.handle) {
                P2MenuRow(Icons.Outlined.Link, "Copy profile link") {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("NoorPro profile", shareUrl)); sheet = null
                }
                P2MenuRow(Icons.Outlined.Flag, "Report account") {
                    sheet = null
                    if (!viewModel.isLoggedIn) Toast.makeText(context, "Sign in to report.", Toast.LENGTH_SHORT).show() else reportOpen = true
                }
                P2MenuRow(Icons.Outlined.Block, "Block ${profile.name}") {
                    sheet = null
                    if (!viewModel.isLoggedIn) Toast.makeText(context, "Sign in to block.", Toast.LENGTH_SHORT).show()
                    else repository.setBlocked(profileUid, true) { ok -> Toast.makeText(context, if (ok) "Blocked." else "Unable to block.", Toast.LENGTH_SHORT).show(); if (ok) viewModel.goBack() }
                }
            }
            null -> {}
        }
    }
    if (reportOpen) UgcReportDialog(ReportTarget("user", profileUid, profileUid, profile.name)) { reportOpen = false }
}

@Composable
private fun P2MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(icon, null, tint = R2Green)
        Spacer(Modifier.width(14.dp))
        androidx.compose.material3.Text(label, color = com.noorpro.app.ui.components.stitchText())
    }
}


@Composable
fun NoorProfile2EditScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val features = remember { Profile2Features() }
    val prefs = remember { context.getSharedPreferences(P2_PREFS, Context.MODE_PRIVATE) }
    val repository = remember { UmmahRepository() }
    DisposableEffect(repository) { onDispose { repository.close() } }
    val myUid = remember(viewModel.isLoggedIn) { repository.currentUserUid().orEmpty() }
    if (!viewModel.isLoggedIn) {
        Box(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().padding(22.dp)) {
            StitchGuestSignInProfileCard(onSignIn = { viewModel.navigateTo(DeenScreen.LOGIN) }, modifier = Modifier.padding(top = 60.dp))
        }
        return
    }
    val current = viewModel.ummahUsername.ifBlank { viewModel.userEmail.substringBefore("@") }
    var name by remember { mutableStateOf(viewModel.userDisplayName) }
    var username by remember { mutableStateOf(current) }
    var bio by remember { mutableStateOf(viewModel.userBio) }
    var saving by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var accent by remember { mutableStateOf(Color(prefs.getInt("accent", R2Green.toArgb()))) }
    val liveProfiles = rememberStitchProfiles()
    var searched by remember { mutableStateOf(emptyList<UmmahProfile>()) }
    LaunchedEffect(username) {
        val h = normalizeUsername(username)
        if (h.length < 3 || h == normalizeUsername(current)) return@LaunchedEffect
        delay(350)
        runCatching { repository.searchProfiles(h) { searched = it } }
    }
    val status = usernameStatus(username, myUid, current, liveProfiles.values + searched)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.updateProfilePhoto(context, uri) { ok, msg ->
            Toast.makeText(context, if (ok) "Profile photo updated" else msg ?: "Unable to update photo", Toast.LENGTH_SHORT).show()
        }
    }
    CompositionLocalProvider(LocalReels2RemoteImages provides true) {
        Profile2EditForm(
            name = name, onName = { name = it.take(40) },
            username = username, onUsername = { username = it.take(26) }, status = status,
            bio = bio, onBio = { bio = it.take(160) },
            suggestions = suggestions, aiLoading = aiLoading, aiError = aiError,
            onAskAi = {
                aiLoading = true; aiError = null
                scope.launch {
                    val raw = runCatching { AiFeatureService.generateResponse(Profile2AiPrompts.bio(name, bio, ""), NoorAiMode.AskNoor) }.getOrNull()
                    val parsed = if (raw.isNullOrBlank() || raw == AiFeatureService.UNAVAILABLE_MESSAGE) emptyList() else Profile2AiPrompts.parseBios(raw)
                    suggestions = parsed
                    aiError = if (parsed.isEmpty()) "Noor AI isn't reachable right now. Please try again later." else null
                    aiLoading = false
                }
            },
            onPick = { bio = it.take(160) },
            avatar = null, photoUrl = viewModel.userPhotoUrl, onPhoto = { picker.launch("image/*") },
            accent = accent, onAccent = { accent = it; prefs.edit().putInt("accent", it.toArgb()).apply() },
            onCover = { Toast.makeText(context, "Cover photos are coming soon", Toast.LENGTH_SHORT).show() },
            f = features, saving = saving,
            onSave = {
                saving = true
                viewModel.updateUmmahProfile(name.trim(), normalizeUsername(username), bio.trim()) { ok, msg ->
                    saving = false
                    Toast.makeText(context, if (ok) "Profile saved" else msg ?: "Unable to save", Toast.LENGTH_SHORT).show()
                    if (ok) viewModel.goBack()
                }
            },
            onBack = { viewModel.goBack() },
        )
    }
}
