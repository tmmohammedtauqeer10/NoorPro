package com.noorpro.app

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.noorpro.app.ui.screens.pressScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.togetherWith
import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

    override fun onStart() {
        super.onStart()
        runCatching { com.noorpro.app.calls.IncomingCallWatcher.start(this) }
    }

    override fun onStop() {
        runCatching { com.noorpro.app.calls.IncomingCallWatcher.stop() }
        super.onStop()
    }

    /** 1.0.96: screen requested by a notification action (cold start or onNewIntent). */
    private val notificationNav = androidx.compose.runtime.mutableStateOf<com.noorpro.app.ui.viewmodel.DeenScreen?>(null)

    private fun notificationTarget(i: Intent?, includePrayer: Boolean): com.noorpro.app.ui.viewmodel.DeenScreen? = when {
        i == null -> null
        i.getBooleanExtra("OPEN_QIBLA", false) -> com.noorpro.app.ui.viewmodel.DeenScreen.QIBLA_MORE
        i.getBooleanExtra("OPEN_QURAN", false) -> com.noorpro.app.ui.viewmodel.DeenScreen.QURAN
        i.getBooleanExtra("OPEN_ISLAMIC_CALENDAR", false) -> com.noorpro.app.ui.viewmodel.DeenScreen.ISLAMIC_CALENDAR
        includePrayer && i.getBooleanExtra("OPEN_PRAYER", false) -> com.noorpro.app.ui.viewmodel.DeenScreen.PRAYER_TIMES
        else -> null
    }

    /** 1.0.98: request from a home-screen widget (Continue Quran / Now playing). */
    private data class WidgetNav(val surahId: Int = 0, val ayah: Int = 1, val nowPlaying: Boolean = false, val audio: Boolean = false)
    private val widgetNav = androidx.compose.runtime.mutableStateOf<WidgetNav?>(null)

    private fun widgetTarget(i: Intent?): WidgetNav? = when {
        i == null -> null
        i.getBooleanExtra(EXTRA_QURAN_RESUME, false) ->
            WidgetNav(surahId = i.getIntExtra(EXTRA_QURAN_SURAH_ID, 1), ayah = i.getIntExtra(EXTRA_QURAN_AYAH, 1).coerceAtLeast(1))
        i.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) || i.getBooleanExtra("OPEN_AL_NOOR_AUDIO", false) -> WidgetNav(nowPlaying = true)
        i.getBooleanExtra(EXTRA_OPEN_AUDIO, false) -> WidgetNav(audio = true)
        else -> null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTarget(intent, includePrayer = true)?.let { notificationNav.value = it }
        widgetTarget(intent)?.let { widgetNav.value = it }
    }

    companion object {
        const val EXTRA_QURAN_RESUME = "OPEN_QURAN_RESUME"
        const val EXTRA_QURAN_SURAH_ID = "QURAN_SURAH_ID"
        const val EXTRA_QURAN_AYAH = "QURAN_AYAH"
        const val EXTRA_OPEN_NOW_PLAYING = "OPEN_NOW_PLAYING"
        const val EXTRA_OPEN_AUDIO = "OPEN_AUDIO_LIBRARY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) notificationNav.value = notificationTarget(intent, includePrayer = false)
        if (savedInstanceState == null) widgetNav.value = widgetTarget(intent)
        // Android 12+ SplashScreen API (back-ported): brand green + app mark, then hands over to Compose.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val coldStart = savedInstanceState == null

        enableEdgeToEdge()
        // Google UMP consent -> then MobileAds.initialize (never throws; no-op under tests).
        runCatching { com.noorpro.app.ads.ConsentManager.gatherConsent(this) }
        // App Link (https://noor-pro-d87e3.web.app/reel?id=… or /u?uid=…) that launched us.
        val appLink = intent?.data
        // 1.1.5 Halal Guard: Remote Config flags + on-device word list (off the main thread).
        runCatching { com.noorpro.app.data.halal.HalalFlags.load() }
        // 1.1.5 Reels v2 / Profile v2 kill-switches (default ON) + backend feature flags (default OFF).
        runCatching { com.noorpro.app.ui.screens.Reels2Flags.load() }
        runCatching { com.noorpro.app.audio.catalog.OfficialChannelFlags.load() }
        Thread { runCatching { com.noorpro.app.data.halal.HalalTextFilter.ensureLoaded(applicationContext) } }.start()
        setContent {
            val viewModel: DeenViewModel = viewModel()
            // 1.0.78: start presence + incoming-call ringing as soon as the member signs in.
            androidx.compose.runtime.LaunchedEffect(viewModel.isLoggedIn) {
                if (viewModel.isLoggedIn) runCatching { com.noorpro.app.calls.IncomingCallWatcher.start(this@MainActivity) }
            }

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
                    // 1.1.6 Group Khatm invite (https://noor-pro-d87e3.web.app/k?c=CODE), only when the flag is ON.
                    "/k" -> com.noorpro.app.islamic.khatm.KhatmLogic.parseInvite(link.toString())
                        ?.takeIf { com.noorpro.app.islamic.IslamicFlags.isOn(com.noorpro.app.islamic.IslamicFlags.GROUP_KHATM) }?.let { code ->
                            com.noorpro.app.islamic.khatm.KhatmNav.pendingCode.value = code
                            viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.GROUP_KHATM)
                        }
                }
            }
            if (intent?.getBooleanExtra("OPEN_PRAYER", false) == true) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.PRAYER_TIMES)
                }
            }
            // 1.0.96: notification buttons (Qibla / Read Quran / Open) also work when the app is already open.
            val notifTarget = notificationNav.value
            androidx.compose.runtime.LaunchedEffect(notifTarget) {
                notifTarget?.let { viewModel.navigateTo(it); notificationNav.value = null }
            }
            val widgetReq = widgetNav.value
            androidx.compose.runtime.LaunchedEffect(widgetReq) {
                val req = widgetReq ?: return@LaunchedEffect
                widgetNav.value = null
                when {
                    req.surahId != 0 -> {
                        val surah = if (req.surahId > 0) {
                            com.noorpro.app.data.IslamicData.surahs.firstOrNull { it.id == req.surahId }
                                ?: com.noorpro.app.data.IslamicData.surahs.first()
                        } else {
                            com.noorpro.app.data.Surah(
                                id = req.surahId, nameEnglish = "Juz ${-req.surahId}", nameArabic = "Juz ${-req.surahId}",
                                englishTranslation = "", versesCount = com.noorpro.app.data.IslamicData.juzAyahsCount[-req.surahId] ?: 0,
                                type = "Juz", verses = emptyList(),
                            )
                        }
                        viewModel.resumeReading(surah, req.ayah)
                    }
                    req.nowPlaying -> {
                        val hasMedia = com.noorpro.app.audio.session.AlNoorSessionRouter.currentPlayer()?.mediaItemCount?.let { it > 0 } == true
                        viewModel.navigateTo(
                            if (hasMedia) com.noorpro.app.ui.viewmodel.DeenScreen.NOW_PLAYING
                            else com.noorpro.app.ui.viewmodel.DeenScreen.AUDIO_LIBRARY,
                        )
                    }
                    req.audio -> viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.AUDIO_LIBRARY)
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
                var showSplash by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(coldStart) }
                androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.delay(1100); showSplash = false }
                androidx.compose.foundation.layout.Box {
                    MainLayout()
                    // 1.1.5: "your post wasn't published" / warning notices from Halal Guard.
                    if (viewModel.isLoggedIn) com.noorpro.app.ui.screens.HalalNoticeHost()
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSplash,
                        enter = androidx.compose.animation.EnterTransition.None,
                        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(450)),
                    ) { com.noorpro.app.ui.screens.BrandSplash() }
                }
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

    if (!onboardingComplete) {
        com.noorpro.app.ui.screens.EntranceOnboarding(viewModel) {
            onboardingPrefs.edit().putBoolean("permissions_intro_complete", true).apply()
            onboardingComplete = true
        }
        return@CompositionLocalProvider
    }

    com.noorpro.app.ui.screens.PrayerPermissionPrompt(suppress = currentScreen == DeenScreen.LOGIN)

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
        // 1.0.73: Material fade-through between screens (outgoing fades 90ms, incoming fades in
        // + scales 0.96->1 over 210ms). Bottom-nav tabs use a plain crossfade. Instant for the
        // full player, and when the system "Remove animations" setting is on.
        val motionOn = com.noorpro.app.ui.components.animationsEnabled()
        androidx.compose.animation.AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val tabs = setOf(DeenScreen.DASHBOARD, DeenScreen.QURAN, DeenScreen.REELS, DeenScreen.UMMAH, DeenScreen.PROFILE_DASHBOARD)
                when {
                    !motionOn || targetState == DeenScreen.NOW_PLAYING ->
                        androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                    initialState in tabs && targetState in tabs ->
                        androidx.compose.animation.fadeIn(tween(220, delayMillis = 60)) togetherWith androidx.compose.animation.fadeOut(tween(120))
                    else ->
                        (androidx.compose.animation.fadeIn(tween(210, delayMillis = 90)) +
                            androidx.compose.animation.scaleIn(tween(210, delayMillis = 90), initialScale = 0.96f)) togetherWith
                            androidx.compose.animation.fadeOut(tween(90))
                }
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                DeenScreen.LOGIN -> com.noorpro.app.ui.screens.LoginScreen(viewModel = viewModel)
                DeenScreen.DASHBOARD -> StitchHomeScreen(viewModel = viewModel)
                DeenScreen.QURAN -> StitchQuranScreen(viewModel = viewModel)
                DeenScreen.REELS -> if (com.noorpro.app.ui.screens.Reels2Flags.isOn(com.noorpro.app.ui.screens.Reels2Flags.REELS_V2)) com.noorpro.app.ui.screens.NoorReels2Screen(viewModel) else StitchReelsScreen(viewModel = viewModel)
                DeenScreen.REELS_EXPLORE -> com.noorpro.app.ui.screens.ReelsExploreScreen(viewModel = viewModel)
                DeenScreen.NOW_PLAYING -> com.noorpro.app.ui.screens.AlNoorNowPlayingScreen(viewModel)
                DeenScreen.PRAYER_TIMES -> PrayerTimesScreen(viewModel = viewModel)
                DeenScreen.QIBLA_MORE -> StitchQiblaScreen(viewModel = viewModel)
                DeenScreen.SETTINGS -> com.noorpro.app.ui.screens.SettingsScreen(viewModel = viewModel)
                DeenScreen.ADVANCED_SETTINGS -> com.noorpro.app.ui.screens.AdvancedSettingsScreen(viewModel = viewModel)
                DeenScreen.ACCOUNT_SWITCHER -> com.noorpro.app.ui.screens.AccountSwitcherScreen(viewModel = viewModel)
                DeenScreen.AZKAR -> com.noorpro.app.ui.screens.AzkarScreen(viewModel = viewModel)
                DeenScreen.TASBIH -> com.noorpro.app.ui.screens.TasbihScreen(viewModel = viewModel)
                DeenScreen.DAROOD_SHARIF -> com.noorpro.app.ui.screens.SpiritualContentScreen(viewModel = viewModel, sectionId = "darood")
                DeenScreen.RUHANI -> com.noorpro.app.ui.screens.SpiritualContentScreen(viewModel = viewModel, sectionId = "ruhani")
                DeenScreen.TASAWWUF -> com.noorpro.app.ui.screens.SpiritualContentScreen(viewModel = viewModel, sectionId = "tasawwuf")
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
                DeenScreen.QAIDA_TUTOR -> com.noorpro.app.ui.screens.QuranCourseScreen(viewModel = viewModel)
                DeenScreen.UMMAH -> StitchUmmahHubScreen(viewModel = viewModel)
                DeenScreen.UMMAH_FULL -> StitchUmmahHubScreen(viewModel = viewModel)
                DeenScreen.UMMAH_CREATE_POST -> StitchUmmahCreateScreen(viewModel = viewModel, reelMode = false)
                DeenScreen.UMMAH_CREATE_REEL -> StitchUmmahCreateScreen(viewModel = viewModel, reelMode = true)
                DeenScreen.UMMAH_PROFILE -> if (com.noorpro.app.ui.screens.Reels2Flags.isOn(com.noorpro.app.ui.screens.Reels2Flags.PROFILE_V2)) com.noorpro.app.ui.screens.NoorProfile2Screen(viewModel) else StitchUmmahProfileScreen(viewModel = viewModel)
                DeenScreen.UMMAH_PROFILE_EDIT -> if (com.noorpro.app.ui.screens.Reels2Flags.isOn(com.noorpro.app.ui.screens.Reels2Flags.PROFILE_V2)) com.noorpro.app.ui.screens.NoorProfile2EditScreen(viewModel) else StitchUmmahProfileEditScreen(viewModel = viewModel)
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
                DeenScreen.HALAL_GUIDELINES -> com.noorpro.app.ui.screens.HalalGuidelinesScreen(onBack = { viewModel.goBack() })
                DeenScreen.SAFETY_SETTINGS -> com.noorpro.app.ui.screens.SafetySettingsScreen(viewModel = viewModel)
                DeenScreen.MODERATOR_PANEL -> com.noorpro.app.ui.screens.ModeratorPanelScreen(viewModel = viewModel)
                DeenScreen.UMMAH_FOLLOWERS -> StitchFollowersScreen(viewModel = viewModel)
                DeenScreen.UMMAH_FOLLOWING -> StitchFollowingScreen(viewModel = viewModel)
                DeenScreen.DEEN_POINTS -> StitchDeenPointsScreen(viewModel = viewModel)
                DeenScreen.AUDIO_LIBRARY -> com.noorpro.app.ui.screens.StitchAudioLibraryScreen(viewModel = viewModel)
                DeenScreen.AUDIO_PLAYLIST -> com.noorpro.app.ui.screens.AlNoorAudioScreen(viewModel = viewModel, startRoute = "mix:" + viewModel.audioPlaylistKey.ifBlank { "morning" })
                DeenScreen.DISCOVER_GROUPS -> com.noorpro.app.ui.screens.StitchDiscoverGroupsScreen(viewModel = viewModel)
                // 1.1.6 Islamic features: flag OFF falls back to the closest existing screen.
                DeenScreen.ISLAMIC_CALENDAR -> if (com.noorpro.app.islamic.IslamicFlags.isOn(com.noorpro.app.islamic.IslamicFlags.CALENDAR)) com.noorpro.app.ui.islamic.IslamicCalendarScreen(viewModel) else com.noorpro.app.ui.screens.HijriCalendarScreen(viewModel = viewModel)
                DeenScreen.GROUP_KHATM -> if (com.noorpro.app.islamic.IslamicFlags.isOn(com.noorpro.app.islamic.IslamicFlags.GROUP_KHATM)) com.noorpro.app.ui.islamic.GroupKhatmScreen(viewModel) else com.noorpro.app.ui.islamic.IslamicFeatureOff { viewModel.goBack() }
            }
        }

        // Keep controls available for every audio source after leaving its page. Quran audio lives
        // in the shared ViewModel while podcasts, nasheeds, naats, duas and tafsir use Al-Noor's
        // ExoPlayer queue, so both sources must participate in mini-player visibility.
        com.noorpro.app.audio.AlNoorAudioSession.init(context)
        val alNoorQueue by com.noorpro.app.audio.AlNoorAudioSession.player.queue.collectAsState()
        val alNoorAudible by com.noorpro.app.audio.AlNoorAudioSession.anyAudible.collectAsState()
        val showMiniPlayer = (playingSurah != null || currentAudioTrack != null || alNoorQueue.currentTrack != null) &&
            currentScreen != DeenScreen.NOW_PLAYING &&
            currentScreen != DeenScreen.LOGIN &&
            !((currentScreen == DeenScreen.UMMAH_FULL || currentScreen == DeenScreen.REELS) && isUmmahReelsImmersive)

        val showBottomNavigation = (currentScreen == DeenScreen.DASHBOARD ||
            currentScreen == DeenScreen.PROFILE_DASHBOARD ||
            currentScreen == DeenScreen.REELS ||
            currentScreen == DeenScreen.UMMAH ||
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
        
        // Beautiful glassmorphic floating bottom navigation bar - hidden when actively reading a Surah page or Tafsir screen
        // Android 13+: the media notification needs POST_NOTIFICATIONS; ask the first time audio plays.
        val mediaNotifLauncher = rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(alNoorAudible) {
            if (alNoorAudible &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                !onboardingPrefs.getBoolean("media_notif_asked", false)
            ) {
                onboardingPrefs.edit().putBoolean("media_notif_asked", true).apply()
                runCatching { mediaNotifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
            }
        }
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
        // 1.0.82: Instagram-style in-app share sheet for reels / Ummah posts.
        com.noorpro.app.ui.screens.ShareToChatHost(viewModel)
        // 1.0.81: Spotify-style mini player that expands (shared element) into full Now Playing.
        com.noorpro.app.ui.screens.AlNoorPlayerHost(
            vm = viewModel,
            visible = showMiniPlayer,
            bottomPadding = when {
                showBottomNavigation -> 88.dp
                currentScreen == DeenScreen.AUDIO_LIBRARY || currentScreen == DeenScreen.AUDIO_PLAYLIST -> 64.dp
                else -> 12.dp
            }
        )
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
            .height(62.dp)
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
    // 1.0.99: selected tab = soft mint pill behind a green icon (dark: deep-green pill, gold icon).
    val selectedContainer = if (isLightTheme) Color(0xFFE2F0E9) else Color(0xFF1B3A30)
    val selectedLabelColor = if (isLightTheme) StitchEmerald else MatteGold
    val labelColor by androidx.compose.animation.animateColorAsState(if (isSelected) selectedLabelColor else unselectedColor, tween(200), label = "navLabel")
    val pillColor by androidx.compose.animation.animateColorAsState(if (isSelected) selectedContainer else Color.Transparent, tween(200), label = "navPill")
    val iconScale by androidx.compose.animation.core.animateFloatAsState(if (isSelected) 1.08f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 500f), label = "navIcon")
    val haptic = com.noorpro.app.ui.components.rememberHaptic()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = { if (!isSelected) haptic(); onClick() })
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
                    .background(pillColor)
                    .padding(horizontal = if (isSelected) 18.dp else 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) selectedLabelColor else labelColor,
                    modifier = Modifier.size(21.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                color = labelColor,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
