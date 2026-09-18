package com.example // Force rebuild to break cache corruption

import android.os.Bundle
import android.util.Log // Added
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noorpro.app.ui.screens.HomeScreen
import com.noorpro.app.ui.screens.PrayerTimesScreen
import com.noorpro.app.ui.screens.QiblaSettingsScreen
import com.noorpro.app.ui.screens.StitchCreatorStudioScreen
import com.noorpro.app.ui.screens.StitchDeenPointsScreen
import com.noorpro.app.ui.screens.StitchDiscoverLibraryScreen
import com.noorpro.app.ui.screens.StitchFollowersScreen
import com.noorpro.app.ui.screens.StitchFollowingScreen
import com.noorpro.app.ui.screens.StitchHomeScreen
import com.noorpro.app.ui.screens.StitchProfileSettingsScreen
import com.noorpro.app.ui.screens.StitchQiblaScreen
import com.noorpro.app.ui.screens.StitchQuranScreen
import com.noorpro.app.ui.screens.StitchReelsScreen
import com.noorpro.app.ui.screens.StitchUmmahCreateScreen
import com.noorpro.app.ui.screens.StitchUmmahHubScreen
import com.noorpro.app.ui.screens.StitchUmmahActivityScreen
import com.noorpro.app.ui.screens.StitchUmmahArchiveScreen
import com.noorpro.app.ui.screens.StitchUmmahBlockedScreen
import com.noorpro.app.ui.screens.StitchUmmahCloseFriendsScreen
import com.noorpro.app.ui.screens.StitchUmmahLikesCommentsScreen
import com.noorpro.app.ui.screens.StitchUmmahNotificationsScreen
import com.noorpro.app.ui.screens.StitchUmmahPrivacyScreen
import com.noorpro.app.ui.screens.StitchUmmahProfileEditScreen
import com.noorpro.app.ui.screens.StitchUmmahProfileScreen
import com.noorpro.app.ui.screens.StitchUmmahSavedScreen
import com.noorpro.app.ui.screens.StitchUmmahSearchScreen
import com.noorpro.app.ui.screens.StitchUmmahTimeManagementScreen
import com.noorpro.app.ui.screens.StitchNoorProPlusScreen
import com.noorpro.app.ui.screens.SurahListScreen
import com.noorpro.app.ui.screens.TafsirScreen
import com.noorpro.app.ui.components.StitchCream
import com.noorpro.app.ui.components.StitchDarkBackground
import com.noorpro.app.ui.components.StitchDarkLine
import com.noorpro.app.ui.components.StitchDarkMuted
import com.noorpro.app.ui.components.StitchDarkSoft
import com.noorpro.app.ui.components.StitchDarkSurface
import com.noorpro.app.ui.components.StitchEmerald
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.StitchInk
import com.noorpro.app.ui.components.StitchLine
import com.noorpro.app.ui.components.StitchMuted
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.noorpro.app.ui.theme.NightBackground
import com.noorpro.app.ui.theme.DeepNightBlue
import com.noorpro.app.ui.theme.MatteGold
import com.noorpro.app.ui.theme.GlassOverlay
import com.noorpro.app.ui.theme.GlassBorder
import com.noorpro.app.ui.theme.LightGold
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

import com.noorpro.app.ui.theme.LocalAppStrings
import com.noorpro.app.ui.theme.LocalAppLanguage
import com.noorpro.app.ui.theme.EnStrings
import com.noorpro.app.ui.theme.ArStrings
import com.noorpro.app.ui.theme.UrStrings
import com.noorpro.app.ui.theme.AppLanguage

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        // App Link (https://noor-pro-d87e3.web.app/reel?id=… or /u?uid=…) that launched us.
        val appLink = intent?.data
        setContent {
            val viewModel: DeenViewModel = viewModel()

            // Deep-link into the shared content once, after the UI is up.
            androidx.compose.runtime.LaunchedEffect(appLink) {
                val link = appLink ?: return@LaunchedEffect
                when (link.path) {
                    "/reel" -> link.getQueryParameter("id")?.takeIf { it.isNotBlank() }?.let { id ->
                        viewModel.openReel(id)
                    }
                    "/u" -> link.getQueryParameter("uid")?.takeIf { it.isNotBlank() }?.let { uid ->
                        viewModel.openCreatorProfile(uid, "", "")
                    }
                    "/g" -> link.getQueryParameter("id")?.takeIf { it.isNotBlank() }?.let { id ->
                        viewModel.openGroupInvite(id)
                    }
                }
            }
            val themeMode by viewModel.themeMode.collectAsState()
            val systemTheme = androidx.compose.foundation.isSystemInDarkTheme()
            val useDarkTheme = when(themeMode) {
                com.noorpro.app.ui.viewmodel.ThemeMode.SYSTEM -> systemTheme
                com.noorpro.app.ui.viewmodel.ThemeMode.DARK -> true
                com.noorpro.app.ui.viewmodel.ThemeMode.LIGHT -> false
            }
            MyApplicationTheme(darkTheme = useDarkTheme) {
                MainLayout()
            }
        }
    }
}

@Composable
fun MainLayout() {
    val viewModel: DeenViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedSurah by viewModel.selectedSurah.collectAsState()
    val playingSurah by viewModel.playingSurah.collectAsState()
    val currentAudioTrack by viewModel.currentAudioTrack.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isUmmahReelsImmersive by viewModel.isUmmahReelsImmersive.collectAsState()
    val isModalOverlayActive by viewModel.isModalOverlayActive.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val onboardingPrefs = remember { context.getSharedPreferences("onboarding", android.content.Context.MODE_PRIVATE) }
    var onboardingComplete by remember { mutableStateOf(onboardingPrefs.getBoolean("permissions_intro_complete", false)) }
    
    val strings = when (appLang) {
        AppLanguage.AR -> ArStrings
        AppLanguage.UR -> UrStrings
        else -> EnStrings
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalAppLanguage provides appLang
    ) {
        val locationPermissionRequest = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarseGranted = permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (fineGranted || coarseGranted) {
            viewModel.checkLocationAndRefresh { }
        }
        onboardingPrefs.edit().putBoolean("permissions_intro_complete", true).apply()
        onboardingComplete = true
    }

    if (!onboardingComplete) {
        com.noorpro.app.ui.screens.PermissionOnboardingScreen(
            selectedTheme = themeMode,
            onThemeSelected = viewModel::updateThemeMode,
            onEnableAndContinue = {
                val requested = buildList {
                    add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        add(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                locationPermissionRequest.launch(requested.toTypedArray())
            },
            onSkip = {
                onboardingPrefs.edit().putBoolean("permissions_intro_complete", true).apply()
                onboardingComplete = true
            }
        )
        return@CompositionLocalProvider
    }

    LaunchedEffect(onboardingComplete) {
        val hasLocationPermission =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            viewModel.refreshLocationIfNeeded()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocationIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Global system-back handling for screens that don't define their own.
    // Disabled on root screens so back exits the app. Screen-level BackHandlers
    // are composed deeper and take priority over this one.
    BackHandler(enabled = currentScreen != DeenScreen.DASHBOARD && currentScreen != DeenScreen.LOGIN) {
        if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.DASHBOARD)
    }

    val appIsLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val appBackground = if (appIsLightTheme) {
        Brush.verticalGradient(listOf(StitchCream, Color(0xFFF7F2EC), Color.White))
    } else {
        Brush.verticalGradient(listOf(StitchDarkBackground, Color(0xFF0B1713), StitchDarkSurface))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        // Core Screens crossfade transition
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(300),
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                DeenScreen.LOGIN -> com.noorpro.app.ui.screens.LoginScreen(viewModel = viewModel)
                DeenScreen.DASHBOARD -> StitchHomeScreen(viewModel = viewModel)
                DeenScreen.QURAN -> StitchQuranScreen(viewModel = viewModel)
                DeenScreen.REELS -> StitchReelsScreen(viewModel = viewModel)
                DeenScreen.NOW_PLAYING -> com.noorpro.app.ui.screens.StitchImmersivePlayerScreen(viewModel = viewModel)
                DeenScreen.PRAYER_TIMES -> PrayerTimesScreen(viewModel = viewModel)
                DeenScreen.QIBLA_MORE -> StitchQiblaScreen(viewModel = viewModel)
                DeenScreen.SETTINGS -> com.noorpro.app.ui.screens.SettingsScreen(viewModel = viewModel)
                DeenScreen.ADVANCED_SETTINGS -> com.noorpro.app.ui.screens.AdvancedSettingsScreen(viewModel = viewModel)
                DeenScreen.ACCOUNT_SWITCHER -> com.noorpro.app.ui.screens.AccountSwitcherScreen(viewModel = viewModel)
                DeenScreen.AZKAR -> com.noorpro.app.ui.screens.AzkarScreen(viewModel = viewModel)
                DeenScreen.TASBIH -> com.noorpro.app.ui.screens.TasbihScreen(viewModel = viewModel)
                DeenScreen.EXPLORE -> com.noorpro.app.ui.screens.ExploreScreen(viewModel = viewModel)
                DeenScreen.EDUCATION -> com.noorpro.app.ui.screens.IslamicEducationScreen(viewModel = viewModel)
                DeenScreen.TAFSIR -> TafsirScreen(viewModel = viewModel)
                DeenScreen.CALENDAR -> com.noorpro.app.ui.screens.HijriCalendarScreen(viewModel = viewModel)
                DeenScreen.BOOKMARKS -> com.noorpro.app.ui.screens.BookmarksScreen(viewModel = viewModel)
                DeenScreen.HADITH_LIBRARY -> com.noorpro.app.ui.screens.HadithLibraryScreen(viewModel = viewModel)
                DeenScreen.HADITH_CHAPTERS -> com.noorpro.app.ui.screens.HadithChaptersScreen(viewModel = viewModel)
                DeenScreen.HADITH_READING -> com.noorpro.app.ui.screens.HadithReadingScreen(viewModel = viewModel)
                DeenScreen.DUA_HUB -> com.noorpro.app.ui.screens.DuaHubScreen(viewModel = viewModel)
                DeenScreen.DUA_DETAILS -> com.noorpro.app.ui.screens.DuaDetailsScreen(viewModel = viewModel)
                DeenScreen.QAZA_TRACKER -> com.noorpro.app.ui.screens.QazaTrackerScreen(viewModel = viewModel)
                DeenScreen.LIBRARY_DASHBOARD -> StitchDiscoverLibraryScreen(viewModel = viewModel)
                DeenScreen.INDOPAK_QURAN -> com.noorpro.app.ui.screens.IndoPakQuranScreen(viewModel = viewModel)
                DeenScreen.PDF_READER -> com.noorpro.app.ui.screens.SmartPdfViewerScreen(
                    book = com.noorpro.app.data.DriveBook(
                        id = viewModel.currentBookId,
                        title = viewModel.currentPdfTitle,
                        author = "",
                        pdf_url = viewModel.currentPdfUrl
                    ),
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(DeenScreen.LIBRARY_DASHBOARD) }
                )
                DeenScreen.QUIZ_DASHBOARD -> com.noorpro.app.ui.screens.QuizDashboardScreen(viewModel = viewModel)
                DeenScreen.ACTIVE_QUIZ -> com.noorpro.app.ui.screens.ActiveQuizScreen(viewModel = viewModel)
                DeenScreen.PROFILE_DASHBOARD -> StitchProfileSettingsScreen(viewModel = viewModel)
                DeenScreen.QURAN_LEARNING_DASHBOARD -> com.noorpro.app.ui.screens.QuranDashboardScreen(viewModel = viewModel)
                DeenScreen.QURAN_LEARNING_READER -> com.noorpro.app.ui.screens.SurahLearningScreen(viewModel = viewModel)
                DeenScreen.QAIDA_TUTOR -> com.noorpro.app.ui.screens.QaidaScreen(viewModel = viewModel)
                DeenScreen.UMMAH -> StitchUmmahHubScreen(viewModel = viewModel)
                DeenScreen.UMMAH_FULL -> StitchUmmahHubScreen(viewModel = viewModel)
                DeenScreen.UMMAH_CREATE_POST -> StitchUmmahCreateScreen(viewModel = viewModel, reelMode = false)
                DeenScreen.UMMAH_CREATE_REEL -> StitchUmmahCreateScreen(viewModel = viewModel, reelMode = true)
                DeenScreen.UMMAH_PROFILE -> StitchUmmahProfileScreen(viewModel = viewModel)
                DeenScreen.UMMAH_PROFILE_EDIT -> StitchUmmahProfileEditScreen(viewModel = viewModel)
                DeenScreen.UMMAH_SEARCH -> StitchUmmahSearchScreen(viewModel = viewModel)
                DeenScreen.UMMAH_SAVED -> StitchUmmahSavedScreen(viewModel = viewModel)
                DeenScreen.UMMAH_ACTIVITY -> StitchUmmahActivityScreen(viewModel = viewModel)
                DeenScreen.UMMAH_ARCHIVE -> StitchUmmahArchiveScreen(viewModel = viewModel)
                DeenScreen.UMMAH_NOTIFICATIONS -> StitchUmmahNotificationsScreen(viewModel = viewModel)
                DeenScreen.UMMAH_LIKES_COMMENTS -> StitchUmmahLikesCommentsScreen(viewModel = viewModel)
                DeenScreen.UMMAH_TIME_MANAGEMENT -> StitchUmmahTimeManagementScreen(viewModel = viewModel)
                DeenScreen.UMMAH_PRIVACY -> StitchUmmahPrivacyScreen(viewModel = viewModel)
                DeenScreen.UMMAH_CLOSE_FRIENDS -> StitchUmmahCloseFriendsScreen(viewModel = viewModel)
                DeenScreen.UMMAH_BLOCKED -> StitchUmmahBlockedScreen(viewModel = viewModel)
                DeenScreen.NOOR_PRO_PLUS -> StitchNoorProPlusScreen(viewModel = viewModel)
                DeenScreen.UMMAH_MESSAGES -> com.noorpro.app.ui.screens.StitchMessagesScreen(viewModel = viewModel)
                DeenScreen.UMMAH_CONSTITUTION -> com.noorpro.app.ui.screens.StitchAppConstitutionScreen(viewModel = viewModel)
                DeenScreen.UMMAH_CHAT -> com.noorpro.app.ui.screens.StitchChatScreen(viewModel = viewModel)
                DeenScreen.SPIRITUAL_PROGRESS -> com.noorpro.app.ui.screens.StitchSpiritualProgressScreen(viewModel = viewModel)
                DeenScreen.ASMA_UL_HUSNA -> com.noorpro.app.ui.screens.AsmaUlHusnaScreen(viewModel = viewModel)
                DeenScreen.ZAKAT -> com.noorpro.app.ui.screens.ZakatCalculatorScreen(viewModel = viewModel)
                DeenScreen.HEALTH_WELLNESS -> com.noorpro.app.ui.screens.HealthWellnessScreen(viewModel = viewModel)
                DeenScreen.AI_HUB -> com.noorpro.app.ui.screens.AiFeaturesHubScreen(viewModel = viewModel)
                DeenScreen.PDF_LIBRARY -> com.noorpro.app.ui.screens.PdfLibraryHubScreen(viewModel = viewModel)
                DeenScreen.HAJJ_UMRAH -> com.noorpro.app.ui.screens.HajjUmrahScreen(viewModel = viewModel)
                DeenScreen.CREATOR_STUDIO -> StitchCreatorStudioScreen(viewModel = viewModel)
                DeenScreen.UMMAH_FOLLOWERS -> StitchFollowersScreen(viewModel = viewModel)
                DeenScreen.UMMAH_FOLLOWING -> StitchFollowingScreen(viewModel = viewModel)
                DeenScreen.DEEN_POINTS -> StitchDeenPointsScreen(viewModel = viewModel)
                DeenScreen.AUDIO_LIBRARY -> com.noorpro.app.ui.screens.StitchAudioLibraryScreen(viewModel = viewModel)
                DeenScreen.AUDIO_PLAYLIST -> com.noorpro.app.ui.screens.StitchAudioPlaylistScreen(viewModel = viewModel)
                DeenScreen.DISCOVER_GROUPS -> com.noorpro.app.ui.screens.StitchDiscoverGroupsScreen(viewModel = viewModel)
                DeenScreen.AL_NOOR_AUDIO -> com.noorpro.app.audio.ui.AlNoorAudioHomeScreen(
                    onOpenSearch = { viewModel.openAlNoorSearch() },
                    onOpenPlaylist = { viewModel.openAlNoorPlaylist(it) },
                    onOpenNowPlaying = { viewModel.openAlNoorNowPlaying() },
                    onBack = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.EXPLORE) },
                )
                DeenScreen.AL_NOOR_SEARCH -> com.noorpro.app.audio.ui.AlNoorSearchScreen(
                    onPlayTrack = { viewModel.openAlNoorNowPlaying() },
                    onBack = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.AL_NOOR_AUDIO) },
                )
                DeenScreen.AL_NOOR_PLAYLIST -> com.noorpro.app.audio.ui.AlNoorPlaylistScreen(
                    playlistId = viewModel.alNoorPlaylistId,
                    onPlayTrack = { viewModel.openAlNoorNowPlaying() },
                    onBack = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.AL_NOOR_AUDIO) },
                )
                DeenScreen.AL_NOOR_NOW_PLAYING -> com.noorpro.app.audio.ui.AlNoorNowPlayingScreen(
                    onBack = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.AL_NOOR_AUDIO) },
                )
            }
        }

        // Keep Quran audio controls available after leaving Home/Quran. The
        // MediaPlayer is owned by the shared ViewModel, so this overlay should
        // follow active playback across the app instead of disappearing.
        val showMiniPlayer = (playingSurah != null || currentAudioTrack != null) &&
            currentScreen != DeenScreen.NOW_PLAYING &&
            currentScreen != DeenScreen.LOGIN &&
            !((currentScreen == DeenScreen.UMMAH_FULL || currentScreen == DeenScreen.REELS) && isUmmahReelsImmersive)

        val showBottomNavigation = (currentScreen == DeenScreen.DASHBOARD ||
            currentScreen == DeenScreen.PROFILE_DASHBOARD ||
            currentScreen == DeenScreen.REELS ||
            currentScreen == DeenScreen.UMMAH ||
            currentScreen == DeenScreen.AUDIO_LIBRARY ||
            currentScreen == DeenScreen.AL_NOOR_AUDIO ||
            (currentScreen == DeenScreen.QURAN && selectedSurah == null)) &&
            !((currentScreen == DeenScreen.UMMAH_FULL || currentScreen == DeenScreen.REELS) && isUmmahReelsImmersive) &&
            !isModalOverlayActive

        if (showBottomNavigation || showMiniPlayer) {
            val bottomFade = if (appIsLightTheme) {
                listOf(Color.Transparent, Color(0xFFFFF7FB).copy(alpha = 0.96f))
            } else {
                listOf(Color.Transparent, StitchDarkBackground.copy(alpha = 0.98f))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(if (showMiniPlayer) 178.dp else 88.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            bottomFade
                        )
                    )
            )
        }
        
        if (showMiniPlayer) {
            val miniContainer = if (appIsLightTheme) Color.White.copy(alpha = 0.98f) else StitchDarkSurface.copy(alpha = 0.98f)
            val miniBorder = if (appIsLightTheme) StitchLine else StitchDarkLine
            val miniTitle = currentAudioTrack?.title ?: playingSurah?.nameEnglish.orEmpty()
            val miniSubtitle = currentAudioTrack?.let { track ->
                listOfNotNull(track.artist, track.language).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Noor Audio" }
            } ?: selectedReciter.name
            val miniTitleColor = if (appIsLightTheme) StitchInk else Color(0xFFFAF4E8)
            val miniSubColor = if (appIsLightTheme) StitchMuted else StitchDarkMuted

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = miniContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, miniBorder),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 88.dp, start = 18.dp, end = 18.dp)
                    .fillMaxWidth()
                    .height(62.dp)
                    .clickable { viewModel.navigateTo(DeenScreen.NOW_PLAYING) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MatteGold)
                            .clickable { viewModel.toggleAudioPlayback() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF050C18),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Tracks Information
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = miniTitle,
                            color = miniTitleColor,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = miniSubtitle,
                            color = miniSubColor,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }

                    if (currentAudioTrack == null) {
                        IconButton(onClick = { viewModel.playNextSurah() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Surah",
                                tint = MatteGold
                            )
                        }
                    }
                    
                    // Gorgeous Arabic Surah Badge
                    if (currentAudioTrack == null && !playingSurah?.nameArabic.isNullOrBlank()) {
                        Text(
                            text = playingSurah?.nameArabic ?: "",
                            color = MatteGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.dismissMiniAudioPlayer() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close mini audio",
                            tint = miniSubColor
                        )
                    }
                }
            }
        }

        // Beautiful glassmorphic floating bottom navigation bar - hidden when actively reading a Surah page or Tafsir screen
        // Al Noor Audio mini-player (nasheed/naat) — separate from Quran mini player above.
        com.noorpro.app.audio.AlNoorAudioSession.init(context)
        val alNoorQueue by com.noorpro.app.audio.AlNoorAudioSession.player.queue.collectAsState()
        val alNoorPlayback by com.noorpro.app.audio.AlNoorAudioSession.player.playback.collectAsState()
        val showAlNoorMini = !alNoorQueue.isEmpty &&
            currentScreen != DeenScreen.AL_NOOR_NOW_PLAYING &&
            currentScreen != DeenScreen.LOGIN &&
            currentScreen != DeenScreen.NOW_PLAYING &&
            !showMiniPlayer
        com.noorpro.app.audio.ui.AlNoorMiniPlayer(
            track = alNoorQueue.currentTrack,
            isPlaying = alNoorPlayback.isPlaying,
            visible = showAlNoorMini,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    bottom = if (showBottomNavigation) 88.dp else 16.dp,
                    start = 18.dp,
                    end = 18.dp,
                ),
            onExpand = { viewModel.openAlNoorNowPlaying() },
            onPlayPause = { com.noorpro.app.audio.AlNoorAudioSession.player.togglePlayPause() },
        )

        if (showBottomNavigation) {
            FloatingBottomNavigationBar(
                currentScreen = currentScreen,
                onNavSelected = { viewModel.navigateTo(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 0.dp, start = 0.dp, end = 0.dp)
            )
        }
    }
    }
}

@Composable
fun FloatingBottomNavigationBar(
    currentScreen: DeenScreen,
    onNavSelected: (DeenScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    // The Reels feed is always a dark, full-bleed surface, so the nav chrome must stay
    // dark there even when the app is in light mode — otherwise a white bar clashes with the video.
    val forceDarkChrome = currentScreen == DeenScreen.REELS
    val isLightTheme = !forceDarkChrome && MaterialTheme.colorScheme.background.luminance() > 0.5f
    val navColors = if (isLightTheme) {
        listOf(Color.White.copy(alpha = 0.98f), Color.White.copy(alpha = 0.94f))
    } else {
        listOf(StitchDarkSurface.copy(alpha = 0.99f), StitchDarkBackground.copy(alpha = 0.99f))
    }
    val borderColor = if (isLightTheme) StitchLine else StitchDarkLine
    val navShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(navShape)
            .background(
                Brush.verticalGradient(navColors)
            )
            .border(1.dp, borderColor, navShape)
            .testTag("floating_navbar")
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            NavBarItem(
                screen = DeenScreen.DASHBOARD,
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentScreen == DeenScreen.DASHBOARD,
                isLightTheme = isLightTheme,
                onClick = { onNavSelected(DeenScreen.DASHBOARD) }
            )

            NavBarItem(
                screen = DeenScreen.QURAN,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "Quran",
                isSelected = currentScreen == DeenScreen.QURAN,
                isLightTheme = isLightTheme,
                onClick = { onNavSelected(DeenScreen.QURAN) }
            )

            NavBarItem(
                screen = DeenScreen.REELS,
                icon = Icons.Default.SmartDisplay,
                label = "Reels",
                isSelected = currentScreen == DeenScreen.REELS,
                isLightTheme = isLightTheme,
                onClick = { onNavSelected(DeenScreen.REELS) }
            )

            NavBarItem(
                screen = DeenScreen.UMMAH,
                icon = Icons.Default.Groups,
                label = "Ummah",
                isSelected = currentScreen == DeenScreen.UMMAH,
                isLightTheme = isLightTheme,
                onClick = { onNavSelected(DeenScreen.UMMAH) }
            )

            NavBarItem(
                screen = DeenScreen.PROFILE_DASHBOARD,
                icon = Icons.Default.Apps,
                label = "More",
                isSelected = currentScreen == DeenScreen.PROFILE_DASHBOARD,
                isLightTheme = isLightTheme,
                onClick = { onNavSelected(DeenScreen.PROFILE_DASHBOARD) }
            )
        }
    }
}

@Composable
fun RowScope.NavBarItem(
    screen: DeenScreen,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isLightTheme: Boolean,
    onClick: () -> Unit
) {
    val unselectedColor = if (isLightTheme) Color(0xFF8DA19A) else StitchDarkMuted
    val selectedContainer = if (isLightTheme) StitchEmerald else StitchDarkSoft
    val selectedLabelColor = if (isLightTheme) StitchEmerald else MatteGold
    val labelColor = if (isSelected) selectedLabelColor else unselectedColor
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .testTag("nav_${label.lowercase()}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .animateContentSize()
                .padding(vertical = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) selectedContainer else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) selectedContainer else Color.Transparent,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = if (isSelected) 9.dp else 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) MatteGold else labelColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = label,
                color = labelColor,
                fontSize = 8.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
