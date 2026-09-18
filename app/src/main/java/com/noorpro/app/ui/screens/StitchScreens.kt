package com.noorpro.app.ui.screens

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.noorpro.app.data.BookItem
import com.noorpro.app.data.PrayerTime
import com.noorpro.app.data.Surah
import com.noorpro.app.data.UmmahChat
import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahFollowUser
import com.noorpro.app.data.UmmahGroup
import com.noorpro.app.data.UmmahMessage
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.data.UmmahSavedCollection
import com.noorpro.app.ui.components.StitchCard
import com.noorpro.app.ui.components.StitchCream
import com.noorpro.app.ui.components.StitchEmerald
import com.noorpro.app.ui.components.StitchEmeraldDeep
import com.noorpro.app.ui.components.StitchEyebrow
import com.noorpro.app.ui.components.StitchFeatureTile
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.StitchHeadline
import com.noorpro.app.ui.components.StitchIconBubble
import com.noorpro.app.ui.components.StitchInk
import com.noorpro.app.ui.components.StitchLine
import com.noorpro.app.ui.components.StitchListRow
import com.noorpro.app.ui.components.StitchMuted
import com.noorpro.app.ui.components.StitchPill
import com.noorpro.app.ui.components.StitchScreen
import com.noorpro.app.ui.components.StitchSectionLabel
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchSoftSurface
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.ui.viewmodel.ThemeMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.io.File
import java.net.URL
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun StitchHomeScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val prayers by viewModel.prayers.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val nextPrayerCountdown by viewModel.nextPrayerCountdown.collectAsState()
    val location by viewModel.currentLocationName.collectAsState()
    val hasLocation by viewModel.hasCurrentLocation.collectAsState()
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val lastReadSurah by viewModel.lastReadSurah.collectAsState()
    val lastReadAyah by viewModel.lastReadAyah.collectAsState()
    val playingSurah by viewModel.playingSurah.collectAsState()
    val currentAudioTrack by viewModel.currentAudioTrack.collectAsState()
    val displayName = viewModel.userDisplayName.ifBlank { if (viewModel.isLoggedIn) "Friend" else "Guest" }
    val firstName = displayName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Guest" }
    val bottomInset = if (playingSurah != null || currentAudioTrack != null) 268.dp else 168.dp

    val streak by viewModel.currentStreak.collectAsState()
    val completedSurahs by viewModel.completedSurahs.collectAsState()
    val hijriAdjustment by viewModel.hijriAdjustment.collectAsState()

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkLocationAndRefresh { }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            if (com.noorpro.app.data.DeviceLocationProvider.isLocationEnabled(context)) {
                viewModel.checkLocationAndRefresh { }
            } else {
                locationSettingsLauncher.launch(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }
    val requestLocation: () -> Unit = {
        if (com.noorpro.app.data.DeviceLocationProvider.hasPermission(context)) {
            if (com.noorpro.app.data.DeviceLocationProvider.isLocationEnabled(context)) {
                viewModel.checkLocationAndRefresh { }
            } else {
                locationSettingsLauncher.launch(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 30.dp, end = 20.dp, bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            item {
                StitchHomeHeader(
                    name = firstName,
                    photoUrl = viewModel.userPhotoUrl,
                    onProfile = { viewModel.navigateTo(DeenScreen.PROFILE_DASHBOARD) },
                    onNotifications = { viewModel.navigateTo(DeenScreen.SETTINGS) }
                )
            }
            item {
                StitchImmersivePrayerHero(
                    prayers = prayers,
                    nextPrayerName = nextPrayerName,
                    countdown = nextPrayerCountdown,
                    location = location,
                    hasLocation = hasLocation,
                    hijriAdjustment = hijriAdjustment,
                    displayTime = viewModel::displayPrayerTime,
                    onLocationClick = requestLocation
                )
            }
            item {
                StitchSacredToolsCard(
                    onTool = { viewModel.navigateTo(it) },
                    onSeeAll = { viewModel.navigateTo(DeenScreen.EXPLORE) }
                )
            }
            item {
                StitchAudioHubEntryCard(onOpen = { viewModel.navigateTo(DeenScreen.AUDIO_LIBRARY) })
            }
            item {
                StitchHomeSponsoredAdCard()
            }
            item {
                StitchDevotionCard(
                    prayers = prayers,
                    streak = streak,
                    lastReadSurah = lastReadSurah,
                    lastReadAyah = lastReadAyah,
                    completedSurahs = completedSurahs.size,
                    onOpen = { viewModel.navigateTo(DeenScreen.SPIRITUAL_PROGRESS) }
                )
            }
            item {
                StitchUmmahSpotlightCard(
                    quote = dailyQuote,
                    onJoin = { viewModel.navigateTo(DeenScreen.UMMAH) }
                )
            }
            item {
                StitchContinueReadingCard(
                    surah = lastReadSurah,
                    ayah = lastReadAyah,
                    onContinue = { viewModel.selectSurah(lastReadSurah) }
                )
            }
            item {
                StitchDailyAyahCard(quote = dailyQuote)
            }
        }
    }
}

/** Today's Hijri date, e.g. "Dhul-Hijjah 14, 1446 AH", honouring the user's adjustment. */
private fun hijriDateLabel(adjustment: Int): String {
    return runCatching {
        val cal = android.icu.util.IslamicCalendar()
        cal.add(android.icu.util.Calendar.DAY_OF_MONTH, adjustment)
        val months = listOf(
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal",
            "Jumada al-Thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal",
            "Dhul-Qadah", "Dhul-Hijjah"
        )
        val m = months.getOrElse(cal.get(android.icu.util.Calendar.MONTH)) { "" }
        val d = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val y = cal.get(android.icu.util.Calendar.YEAR)
        "$m $d, $y AH"
    }.getOrDefault("")
}

/** Immersive prayer hero: real mosque photo backdrop with a dark gradient, Hijri date pill,
 *  gold NEXT PRAYER label, location, gold countdown, and the big prayer time     matches the
 *  premium "Islamic Faith Hub" design. */
@Composable
private fun StitchImmersivePrayerHero(
    prayers: List<PrayerTime>,
    nextPrayerName: String,
    countdown: String,
    location: String,
    hasLocation: Boolean,
    hijriAdjustment: Int,
    displayTime: (String) -> String,
    onLocationClick: () -> Unit
) {
    val currentPrayer = prayers.firstOrNull { it.name == nextPrayerName } ?: prayers.firstOrNull()
    val time = currentPrayer?.let { displayTime(it.time) } ?: "--:--"
    val (clock, meridiem) = remember(time) {
        val parts = time.trim().split(" ")
        parts.getOrElse(0) { time } to parts.getOrElse(1) { "" }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(228.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(StitchEmeraldDeep)
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = com.noorpro.app.R.drawable.noor_hero_mosque),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(StitchEmeraldDeep.copy(alpha = 0.35f), StitchEmeraldDeep.copy(alpha = 0.55f), StitchEmeraldDeep.copy(alpha = 0.95f))
                )
            )
        )
        Column(modifier = Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(hijriDateLabel(hijriAdjustment), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (hasLocation) "NEXT PRAYER" else "PRAYER TIMES", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(if (hasLocation) nextPrayerName else "Location", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (hasLocation) Color.Transparent else StitchGold.copy(alpha = 0.18f))
                            .clickable(onClick = onLocationClick)
                            .padding(horizontal = if (hasLocation) 0.dp else 10.dp, vertical = if (hasLocation) 0.dp else 6.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (hasLocation) location.ifBlank { "Location updating" } else "Enable location",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            fontWeight = if (hasLocation) FontWeight.Normal else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(if (hasLocation) friendlyCountdown(countdown) else "Tap to set your location", color = StitchGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(clock, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
                    if (meridiem.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Text(meridiem.uppercase(), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
        }
    }
}

private data class StitchTool(val label: String, val icon: ImageVector, val screen: DeenScreen)

/** White "Sacred Tools" card with four soft-green rounded-square tool shortcuts. */
@Composable
private fun StitchSacredToolsCard(onTool: (DeenScreen) -> Unit, onSeeAll: () -> Unit) {
    val tools = listOf(
        StitchTool("Quran", Icons.AutoMirrored.Filled.MenuBook, DeenScreen.QURAN),
        StitchTool("Tasbih", Icons.Default.Fingerprint, DeenScreen.TASBIH),
        StitchTool("Qibla", Icons.Default.Explore, DeenScreen.QIBLA_MORE),
        StitchTool("Dua", Icons.Default.VolunteerActivism, DeenScreen.DUA_HUB)
    )
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Sacred Tools", color = stitchPrimary(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onSeeAll)
                ) {
                    Text("See All", color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                tools.forEach { tool ->
                    Column(
                        modifier = Modifier.weight(1f).clickable { onTool(tool.screen) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(20.dp)).background(StitchEmerald.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tool.icon, contentDescription = tool.label, tint = stitchPrimary(), modifier = Modifier.size(27.dp))
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(tool.label, color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StitchEmerald.copy(alpha = 0.10f))
                    .clickable { onTool(DeenScreen.QURAN) }
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(stitchPrimary()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = StitchGold, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Read Quran", color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Arabic reading, optional translations and offline access", color = stitchMutedText(), fontSize = 11.sp)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open Quran reader", tint = stitchPrimary(), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** "Today's Devotion" card with three progress rows from real data: prayers elapsed today,
 *  Quran reading position, and the day streak. */
@Composable
private fun StitchDevotionCard(
    prayers: List<PrayerTime>,
    streak: Int,
    lastReadSurah: Surah,
    lastReadAyah: Int,
    completedSurahs: Int,
    onOpen: () -> Unit
) {
    // Prayers whose time has passed today (a real, honest "on-track" signal).
    val fardh = remember(prayers) { prayers.filter { it.name != "Sunrise" } }
    val nowMinutes = remember {
        val c = java.util.Calendar.getInstance()
        c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
    }
    fun toMinutes(t: String): Int {
        val m = Regex("(\\d{1,2}):(\\d{2})").find(t) ?: return 0
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        if (t.contains("PM", true) && h != 12) h += 12
        if (t.contains("AM", true) && h == 12) h = 0
        return h * 60 + min
    }
    val elapsed = fardh.count { toMinutes(it.time) <= nowMinutes }.coerceAtMost(5)
    val quranFraction = (lastReadAyah.toFloat() / lastReadSurah.versesCount.coerceAtLeast(1)).coerceIn(0f, 1f)

    StitchCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(28.dp)) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("Today's Devotion", color = stitchPrimary(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
            Spacer(Modifier.height(20.dp))
            StitchDevotionRow("Salat (Prayer)", "$elapsed/5", elapsed / 5f, stitchPrimary())
            Spacer(Modifier.height(18.dp))
            StitchDevotionRow("Quran Reading", "${lastReadSurah.nameEnglish}", quranFraction, StitchGold)
            Spacer(Modifier.height(18.dp))
            StitchDevotionRow("Day Streak", "$streak days", (streak / 30f).coerceIn(0.04f, 1f), StitchEmerald)
        }
    }
}

@Composable
private fun StitchDevotionRow(label: String, value: String, fraction: Float, barColor: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = barColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Spacer(Modifier.height(9.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(stitchSoftSurface())) {
            Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(barColor))
        }
    }
}

/** Dark-green "Ummah Spotlight" card: a reflection quote with a Join Discussion CTA. */
@Composable
private fun StitchUmmahSpotlightCard(quote: Pair<String, String>, onJoin: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(StitchEmeraldDeep, StitchEmerald)))
    ) {
        Icon(
            Icons.Default.Groups,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.06f),
            modifier = Modifier.size(160.dp).align(Alignment.BottomEnd).padding(4.dp)
        )
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(StitchGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = StitchEmeraldDeep, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Ummah Reflection", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "   ${quote.first}   ",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontFamily = com.noorpro.app.ui.theme.LibreCaslon
            )
            if (quote.second.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("    ${quote.second}", color = StitchGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable(onClick = onJoin)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Join Discussion", color = StitchEmeraldDeep, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = StitchEmeraldDeep, modifier = Modifier.size(15.dp))
            }
        }
    }
}

/** Centered "Daily Ayah" card with thin gold dividers, matching the premium design. */
@Composable
private fun StitchDailyAyahCard(quote: Pair<String, String>) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.width(70.dp).height(1.dp).background(StitchGold.copy(alpha = 0.5f)))
            Spacer(Modifier.height(22.dp))
            Text(
                "   ${quote.first}   ",
                color = stitchPrimary(),
                fontSize = 24.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = com.noorpro.app.ui.theme.LibreCaslon,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "    ${quote.second.uppercase()}",
                color = StitchGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Box(Modifier.width(70.dp).height(1.dp).background(StitchGold.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun StitchHomeHeader(
    name: String,
    photoUrl: String = "",
    onProfile: () -> Unit,
    onNotifications: () -> Unit
) {
    // Premium-mock header: avatar + "Al-Noor" title + salam subtitle on the left, bell on the right.
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding().fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(StitchEmerald)
                .border(1.dp, StitchLine, RoundedCornerShape(15.dp))
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "My profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp))
                )
            } else {
                Text(name.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Al-Noor", color = stitchPrimary(), fontSize = 23.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
            Text("As-Salamu alaykum, $name", color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(stitchSurface())
                .border(1.dp, StitchLine, CircleShape)
                .clickable(onClick = onNotifications),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = stitchPrimary())
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(StitchGold)
            )
        }
    }
}

/** Mock-matched decorative hero: soft gold mosque skyline with a crescent + open Quran,
 *  sitting between the greeting and the Next Prayer card. Pure vector icons, no assets. */
@Composable
private fun StitchHomeHeroArt() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Faint mosque skyline behind.
        Icon(
            imageVector = Icons.Default.Mosque,
            contentDescription = null,
            tint = StitchGold.copy(alpha = 0.16f),
            modifier = Modifier.size(140.dp).align(Alignment.CenterStart).padding(start = 6.dp)
        )
        Icon(
            imageVector = Icons.Default.Mosque,
            contentDescription = null,
            tint = StitchGold.copy(alpha = 0.12f),
            modifier = Modifier.size(110.dp).align(Alignment.CenterEnd).padding(end = 10.dp)
        )
        // Centre piece: open Quran with a gold crescent + star above it.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = StitchGold,
                    modifier = Modifier.size(34.dp).rotate(-25f)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = StitchGold.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = StitchGold.copy(alpha = 0.9f),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

/** Format an "HH:MM:SS" / "MM:SS" countdown string into a friendly "in 2h 5m" / "in 45 mins". */
internal fun friendlyCountdown(countdown: String): String {
    val parts = countdown.split(":").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
    val (h, m) = when (parts.size) {
        3 -> parts[0] to parts[1]
        2 -> 0 to parts[0]
        else -> return "soon"
    }
    return when {
        h > 0 -> "in ${h}h ${m}m"
        m > 0 -> "in $m mins"
        else -> "now"
    }
}

/** A time-of-day glyph for a prayer name. */
internal fun prayerGlyph(name: String): ImageVector = when (name.lowercase()) {
    "fajr" -> Icons.Default.WbTwilight
    "sunrise" -> Icons.Default.LightMode
    "maghrib" -> Icons.Default.WbTwilight
    "isha" -> Icons.Default.DarkMode
    else -> Icons.Default.LightMode
}

@Composable
private fun StitchNextPrayerHero(
    prayers: List<PrayerTime>,
    nextPrayerName: String,
    countdown: String,
    location: String,
    displayTime: (String) -> String
) {
    val currentPrayer = prayers.firstOrNull { it.name == nextPrayerName } ?: prayers.firstOrNull()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(StitchEmerald)
    ) {
        // Decorative mosque silhouette, low opacity in the bottom-right corner.
        Icon(
            imageVector = Icons.Default.Mosque,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp)
                .size(150.dp)
        )
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "NEXT PRAYER",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nextPrayerName,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.noorpro.app.ui.theme.LibreCaslon
                    )
                    Text(
                        text = friendlyCountdown(countdown),
                        color = StitchGold,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        imageVector = prayerGlyph(nextPrayerName),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentPrayer?.let { displayTime(it.time) } ?: "--:--",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    location.ifBlank { "Location updating" },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StitchTodaysSchedule(
    prayers: List<PrayerTime>,
    nextPrayerName: String,
    displayTime: (String) -> String,
    onViewAll: () -> Unit
) {
    val schedule = prayers.filter { it.name != "Sunrise" }
    StitchCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Today's Schedule", color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "View All",
                    color = stitchPrimary(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onViewAll)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                schedule.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { prayer ->
                            StitchScheduleItem(
                                prayer = prayer,
                                active = prayer.name == nextPrayerName,
                                time = displayTime(prayer.time),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onViewAll,
                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Full Schedule", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StitchScheduleItem(
    prayer: PrayerTime,
    active: Boolean,
    time: String,
    modifier: Modifier = Modifier
) {
    // Mock style: the active prayer is a solid green tile with a gold border and white text;
    // the others are soft white tiles.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) StitchEmerald else stitchSoftSurface())
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) StitchGold else StitchLine.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (active) Color.White.copy(alpha = 0.16f) else stitchSurface()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (active) Icons.Default.Mosque else prayerGlyph(prayer.name),
                    contentDescription = null,
                    tint = if (active) Color.White else StitchGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    prayer.name,
                    color = if (active) Color.White.copy(alpha = 0.85f) else stitchMutedText(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    time,
                    color = if (active) Color.White else stitchText(),
                    fontSize = if (active) 17.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

private data class StitchGoal(val label: String, val icon: ImageVector, val screen: DeenScreen)

@Composable
private fun StitchDailyGoals(viewModel: DeenViewModel) {
    // Mock-matched: one clean row of five circular shortcuts. Everything else lives in
    // Explore via "View All".
    val goals = listOf(
        StitchGoal("Quran", Icons.AutoMirrored.Filled.MenuBook, DeenScreen.QURAN),
        StitchGoal("Duas", Icons.Default.VolunteerActivism, DeenScreen.DUA_HUB),
        StitchGoal("Tasbih", Icons.Default.Fingerprint, DeenScreen.TASBIH),
        StitchGoal("Qibla", Icons.Default.Explore, DeenScreen.QIBLA_MORE),
        StitchGoal("Calendar", Icons.Default.CalendarMonth, DeenScreen.CALENDAR)
    )
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Daily Goals", color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "View All",
                color = stitchPrimary(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { viewModel.navigateTo(DeenScreen.EXPLORE) }
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            goals.chunked(5).forEach { rowGoals ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowGoals.forEach { goal ->
                        StitchGoalTile(
                            goal = goal,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(goal.screen) }
                        )
                    }
                    repeat(5 - rowGoals.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StitchGoalTile(
    goal: StitchGoal,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(stitchSurface())
                .border(1.dp, StitchLine, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(goal.icon, contentDescription = goal.label, tint = stitchPrimary(), modifier = Modifier.size(25.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            goal.label,
            color = stitchText().copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StitchContinueReadingCard(
    surah: Surah,
    ayah: Int,
    onContinue: () -> Unit
) {
    val totalAyahs = surah.versesCount.coerceAtLeast(1)
    val progress = (ayah.toFloat() / totalAyahs).coerceIn(0f, 1f)

    // Premium mock: white card with the text on the left and the Quran-on-rehal illustration
    // on the right.
    StitchCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(206.dp)
            .clickable(onClick = onContinue),
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 18.dp, end = 12.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                Text("CONTINUE READING", color = stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = surah.nameEnglish,
                    color = stitchPrimary(),
                    fontSize = 29.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = com.noorpro.app.ui.theme.LibreCaslon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${surah.englishTranslation}     Ayah $ayah", color = stitchMutedText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(13.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(stitchSoftSurface())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(StitchGold)
                    )
                }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(StitchEmerald)
                        .clickable(onClick = onContinue)
                        .padding(horizontal = 20.dp, vertical = 11.dp)
                ) {
                    Text("Continue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .background(StitchEmerald.copy(alpha = 0.10f))
                    .border(1.dp, stitchPrimary().copy(alpha = 0.10f), RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.noorpro.app.R.drawable.noor_quran_rehal),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun StitchReadingProgressCard(
    surah: Surah,
    ayah: Int
) {
    val total = surah.versesCount.coerceAtLeast(1)
    val fraction = (ayah.toFloat() / total).coerceIn(0f, 1f)
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(surah.nameEnglish, color = stitchPrimary(), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text("${(fraction * 100).toInt()}%", color = Color(0xFF7A6500), fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(stitchSoftSurface())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFFFD96A))
                )
            }
        }
    }
}

@Composable
private fun StitchReflectionCard(quote: Pair<String, String>) {
    // quote.first carries the English text + a trailing reference like "(94:6)".
    val raw = quote.first.trim()
    val refMatch = Regex("\\(([0-9]+:[0-9]+)\\)\\s*$").find(raw)
    val reference = refMatch?.groupValues?.getOrNull(1)
    val body = refMatch?.let { raw.removeRange(it.range).trim() } ?: raw
    StitchCard(modifier = Modifier.fillMaxWidth()) {
        Box {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = stitchPrimary().copy(alpha = 0.1f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp)
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Today's Reflection", color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Row {
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(StitchGold)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "   $body   ",
                        color = stitchText(),
                        fontSize = 20.sp,
                        lineHeight = 30.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = com.noorpro.app.ui.theme.LibreCaslon
                    )
                }
                if (reference != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "    Quran $reference",
                        color = stitchMutedText(),
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchReflectionCardClean(quote: Pair<String, String>) {
    val body = quote.second.ifBlank { quote.first }.trim()
    StitchCard(modifier = Modifier.fillMaxWidth()) {
        Box {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = stitchPrimary().copy(alpha = 0.1f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp)
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Today's Reflection", color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Row {
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(StitchGold)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "\"$body\"",
                        color = stitchText(),
                        fontSize = 20.sp,
                        lineHeight = 30.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = com.noorpro.app.ui.theme.LibreCaslon
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Daily reminder",
                    color = stitchMutedText(),
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun StitchQuranScreen(viewModel: DeenViewModel) {
    val selectedSurah by viewModel.selectedSurah.collectAsState()
    val lastReadSurah by viewModel.lastReadSurah.collectAsState()
    val lastReadAyah by viewModel.lastReadAyah.collectAsState()
    val recentQuranReads by viewModel.recentQuranReads.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val translationManager = viewModel.translationManager
    val selectedTranslation by translationManager.selectedTranslation.collectAsState()
    val selectedTranslationName = translationManager.availableTranslations
        .firstOrNull { it.key == selectedTranslation }?.name ?: "Default English"
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Surahs") }
    var searchOpen by remember { mutableStateOf(false) }
    var showTranslationSheet by remember { mutableStateOf(false) }
    var isDownloadingOfflineQuran by remember { mutableStateOf(false) }
    var offlineDownloadComplete by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedSurah != null) {
        viewModel.closeSelectedSurah()
    }

    if (selectedSurah != null) {
        SurahReaderView(
            viewModel = viewModel,
            surah = selectedSurah!!,
            lastReadAyah = if (lastReadSurah.id == selectedSurah!!.id) lastReadAyah else 1,
            onBack = { viewModel.closeSelectedSurah() },
            onAyahLastRead = { ayah -> viewModel.updateLastRead(selectedSurah!!, ayah) },
            onAyahBookmarkToggle = { ayah, text -> viewModel.toggleBookmark(selectedSurah!!, ayah, text) }
        )
        return
    }

    val filtered = remember(surahs, searchQuery) {
        if (searchQuery.isBlank()) surahs else surahs.filter { surah ->
            surah.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                surah.englishTranslation.contains(searchQuery, ignoreCase = true) ||
                surah.id.toString() == searchQuery.trim()
        }
    }
    val continueReads = remember(recentQuranReads, lastReadSurah, lastReadAyah, surahs) {
        val juzThirty = Surah(
            id = -30,
            nameEnglish = "Juz 30",
            nameArabic = "Juz 30",
            englishTranslation = "Amma",
            versesCount = com.noorpro.app.data.IslamicData.juzAyahsCount[30] ?: 564,
            type = "Juz",
            verses = emptyList()
        )
        val fallbackReads = listOf(
            lastReadSurah to lastReadAyah,
            (surahs.firstOrNull { it.id == 18 } ?: lastReadSurah) to 10,
            (surahs.firstOrNull() ?: lastReadSurah) to 1,
            juzThirty to 1
        )
        (listOf(lastReadSurah to lastReadAyah) + recentQuranReads + fallbackReads)
            .distinctBy { it.first.id }
            .take(3)
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 34.dp, end = 20.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    Text("Quran", color = stitchPrimary(), fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { searchOpen = !searchOpen }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = stitchText(), modifier = Modifier.size(26.dp))
                    }
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.NOOR_PRO_PLUS) }) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = "Noor Pro Plus", tint = StitchGold, modifier = Modifier.size(26.dp))
                    }
                }
            }
            if (searchOpen || searchQuery.isNotBlank()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = stitchMutedText()) },
                        placeholder = { Text("Search Surah, Juz, or Ayah...", color = stitchMutedText()) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = stitchSoftSurface(),
                            unfocusedContainerColor = stitchSoftSurface(),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = stitchText(),
                            unfocusedTextColor = stitchText()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                StitchQuranReadingOptionsCard(
                    translationName = selectedTranslationName,
                    translationDownloaded = translationManager.isDownloaded(selectedTranslation),
                    isDownloading = isDownloadingOfflineQuran,
                    offlineReady = offlineDownloadComplete,
                    onTranslations = { showTranslationSheet = true },
                    onOfflineDownload = {
                        if (!isDownloadingOfflineQuran) {
                            isDownloadingOfflineQuran = true
                            viewModel.downloadAllQuranData { success ->
                                isDownloadingOfflineQuran = false
                                offlineDownloadComplete = success
                                Toast.makeText(
                                    context,
                                    if (success) "Full Quran is ready offline" else "Download incomplete. Check your internet and try again.",
                                    if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onIndoPak = { viewModel.navigateTo(DeenScreen.INDOPAK_QURAN) },
                    onBookmarks = { viewModel.navigateTo(DeenScreen.BOOKMARKS) }
                )
            }
            item {
                StitchQuranContinuePager(
                    reads = continueReads,
                    onContinue = { surah, ayah -> viewModel.resumeReading(surah, ayah) },
                    onPlay = { surah ->
                        if (surah.id > 0) {
                            viewModel.selectSurahForPlayback(surah, triggerPlayback = true)
                            viewModel.navigateTo(DeenScreen.NOW_PLAYING)
                        } else {
                            viewModel.selectJuz(-surah.id)
                        }
                    }
                )
            }
            item {
                StitchSurahJuzTabs(selected = tab, onSelect = { tab = it })
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Reading Progress", color = stitchPrimary(), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = stitchPrimary().copy(alpha = 0.58f), modifier = Modifier.size(28.dp))
                }
            }
            item {
                StitchReadingProgressCard(surah = lastReadSurah, ayah = lastReadAyah)
            }
            if (tab == "Surahs") {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Surahs", color = stitchPrimary(), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (searchQuery.isBlank()) "View All" else "${filtered.size} found",
                            color = stitchPrimary().copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                items(filtered, key = { it.id }) { surah ->
                    StitchSurahRow(
                        surah = surah,
                        onOpen = { viewModel.selectSurah(surah) },
                        onPlay = {
                            viewModel.selectSurahForPlayback(surah, triggerPlayback = true)
                            viewModel.navigateTo(DeenScreen.NOW_PLAYING)
                        }
                    )
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Juz'", color = stitchPrimary(), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("30 Parts", color = stitchPrimary().copy(alpha = 0.65f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                items((1..30).toList(), key = { "juz$it" }) { juz ->
                    StitchJuzRow(juz = juz) { viewModel.selectJuz(juz) }
                }
            }
        }
    }
    if (showTranslationSheet) {
        TranslationSelectionBottomSheet(
            viewModel = viewModel,
            translationManager = translationManager,
            onDismiss = { showTranslationSheet = false }
        )
    }
}

@Composable
private fun StitchQuranReadingOptionsCard(
    translationName: String,
    translationDownloaded: Boolean,
    isDownloading: Boolean,
    offlineReady: Boolean,
    onTranslations: () -> Unit,
    onOfflineDownload: () -> Unit,
    onIndoPak: () -> Unit,
    onBookmarks: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(StitchEmerald.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("Quran reading options", color = stitchText(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Choose how you want to read", color = stitchMutedText(), fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StitchQuranOption(
                    icon = Icons.Default.Translate,
                    title = "Translations",
                    subtitle = "$translationName${if (translationDownloaded) " · Offline" else ""}",
                    onClick = onTranslations,
                    modifier = Modifier.weight(1f)
                )
                StitchQuranOption(
                    icon = Icons.Default.CloudDownload,
                    title = when {
                        isDownloading -> "Downloading…"
                        offlineReady -> "Ready offline"
                        else -> "Offline Quran"
                    },
                    subtitle = "Arabic, English & Urdu",
                    onClick = onOfflineDownload,
                    modifier = Modifier.weight(1f),
                    loading = isDownloading
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StitchQuranOption(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Indo-Pak script",
                    subtitle = "Traditional reading layout",
                    onClick = onIndoPak,
                    modifier = Modifier.weight(1f)
                )
                StitchQuranOption(
                    icon = Icons.Default.BookmarkBorder,
                    title = "Bookmarks",
                    subtitle = "Continue saved ayahs",
                    onClick = onBookmarks,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StitchQuranOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Row(
        modifier = modifier
            .heightIn(min = 78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(color = stitchPrimary(), strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Icon(icon, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = stitchMutedText(), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StitchSurahJuzTabs(selected: String, onSelect: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .padding(5.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Surahs", "Juz'").forEach { label ->
                val active = selected == label
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (active) stitchSurface() else Color.Transparent)
                        .clickable { onSelect(label) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (active) stitchPrimary() else stitchMutedText(),
                        fontSize = 20.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchJuzRow(juz: Int, onOpen: () -> Unit) {
    StitchCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(StitchEmeraldDeep),
                contentAlignment = Alignment.Center
            ) {
                Text(juz.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Juz' $juz", color = stitchPrimary(), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Part $juz of 30", color = stitchMutedText(), fontSize = 16.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText().copy(alpha = 0.45f), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun StitchQuranContinuePager(
    reads: List<Pair<Surah, Int>>,
    onContinue: (Surah, Int) -> Unit,
    onPlay: (Surah) -> Unit
) {
    val pages = reads.take(3).ifEmpty {
        listOf(com.noorpro.app.data.IslamicData.surahs.first() to 1)
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(356.dp)
    ) { page ->
        val (surah, ayah) = pages[page]
        StitchQuranLastReadCard(
            surah = surah,
            ayah = ayah,
            pageCount = pages.size,
            activePage = pagerState.currentPage,
            onContinue = { onContinue(surah, ayah) },
            onPlay = { onPlay(surah) }
        )
    }
}

@Composable
private fun StitchQuranLastReadCard(
    surah: Surah,
    ayah: Int,
    pageCount: Int,
    activePage: Int,
    onContinue: () -> Unit,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(356.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF105437), Color(0xFF063B25), Color(0xFF002F1E))
                )
            )
            .clickable(onClick = onContinue)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.44f,
                center = Offset(size.width * 0.96f, size.height * 0.20f)
            )
            drawCircle(
                color = StitchEmerald.copy(alpha = 0.18f),
                radius = size.width * 0.40f,
                center = Offset(size.width * 0.46f, size.height * 0.80f)
            )
            drawCircle(
                color = StitchGold.copy(alpha = 0.10f),
                radius = size.width * 0.18f,
                center = Offset(size.width * 0.55f, size.height * 0.70f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(190.dp)
                .height(176.dp)
                .clip(RoundedCornerShape(bottomStart = 80.dp))
                .background(Color.White.copy(alpha = 0.10f))
        )

        Column(modifier = Modifier.padding(start = 34.dp, top = 34.dp, end = 28.dp)) {
            Text(
                "CONTINUE READING",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                surah.nameEnglish,
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                surah.englishTranslation.ifBlank { "Ayah $ayah" },
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (surah.id < 0) {
                    "Part ${-surah.id} - Quran Juz"
                } else {
                    "Ayah ${ayah.coerceAtLeast(1)} of ${surah.versesCount.coerceAtLeast(1)}"
                },
                color = Color.White.copy(alpha = 0.66f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 62.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFFFDE78))
                    .clickable(onClick = onContinue)
                    .padding(horizontal = 20.dp, vertical = 13.dp)
            ) {
                Text("Continue", color = Color(0xFF241A00), fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF241A00), modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .height(86.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0A4A33).copy(alpha = 0.66f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.noorpro.app.R.drawable.stitch_quran_hero),
                    contentDescription = "Open Quran on rehal",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount.coerceAtLeast(1)) { index ->
                val active = index == activePage.coerceIn(0, pageCount.coerceAtLeast(1) - 1)
                Box(
                    Modifier
                        .width(if (active) 28.dp else 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) Color(0xFFFFDE78) else Color.White.copy(alpha = 0.20f))
                )
            }
        }
    }
}

@Composable
private fun StitchSurahRow(
    surah: Surah,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    StitchCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(StitchEmeraldDeep),
                contentAlignment = Alignment.Center
            ) {
                Text(surah.id.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${surah.id}. ${surah.nameEnglish}", color = stitchPrimary(), fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    surah.englishTranslation,
                    color = stitchMutedText(),
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = repairMojibake(surah.nameArabic),
                    color = stitchPrimary(),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = com.noorpro.app.ui.theme.LibreCaslon,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${surah.versesCount} Verses", color = stitchMutedText().copy(alpha = 0.48f), fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText().copy(alpha = 0.38f), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun StitchQiblaScreen(viewModel: DeenViewModel) {
    val bearing by viewModel.qiblaBearing.collectAsState()
    val azimuth by viewModel.sensorAzimuth.collectAsState()
    val distanceKm by viewModel.qiblaDistanceKm.collectAsState()
    val rotation = (bearing - azimuth + 360f) % 360f

    DisposableEffect(Unit) {
        viewModel.startCompassListening()
        onDispose { viewModel.stopCompassListening() }
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 52.dp, end = 24.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                Column {
                    StitchEyebrow("Qibla")
                    StitchHeadline("Direction to Makkah", fontSize = 36)
                }
            }
            item {
                StitchCompass(rotation = rotation)
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${bearing.toInt()}   from North", color = stitchText(), fontSize = 35.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Heading ${azimuth.toInt()}   - rotate yourself until the gold marker is at the top",
                        color = stitchMutedText(),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                StitchCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = stitchPrimary())
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Your location", color = stitchText(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StitchInfoBox("Distance", if (distanceKm > 0f) "${distanceKm.toInt()} km" else "Updating", Modifier.weight(1f))
                            StitchInfoBox("Bearing", "${bearing.toInt()}  ", Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(listOf("Makkah", "Madinah", "Riyadh", "Istanbul", "Jakarta")) { city ->
                                StitchPill(city, selected = false) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchCompass(rotation: Float) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = min(size.width, size.height) / 2 - 10.dp.toPx()
                drawCircle(Color.White, radius = radius, center = center)
                drawCircle(StitchLine, radius = radius, center = center, style = Stroke(2.dp.toPx()))
                for (i in 0 until 60) {
                    val angle = Math.toRadians((i * 6 - 90).toDouble())
                    val outer = Offset(
                        center.x + cos(angle).toFloat() * radius,
                        center.y + sin(angle).toFloat() * radius
                    )
                    val innerRadius = radius - if (i % 15 == 0) 18.dp.toPx() else 9.dp.toPx()
                    val inner = Offset(
                        center.x + cos(angle).toFloat() * innerRadius,
                        center.y + sin(angle).toFloat() * innerRadius
                    )
                    drawLine(if (i % 15 == 0) StitchEmerald else StitchLine, inner, outer, if (i % 15 == 0) 3.dp.toPx() else 1.dp.toPx())
                }
            }
            Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp), color = stitchMutedText(), fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), color = stitchMutedText(), fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 28.dp), color = stitchMutedText(), fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(start = 28.dp), color = stitchMutedText(), fontWeight = FontWeight.Black, fontSize = 20.sp)
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .rotate(rotation),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(7.dp)
                        .height(118.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(StitchEmerald)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StitchGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mosque, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(StitchEmerald))
        }
    }
}

@Composable
private fun StitchInfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(stitchSoftSurface())
            .padding(16.dp)
    ) {
        Text(label, color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StitchProfileSettingsScreen(viewModel: DeenViewModel) {
    val points by viewModel.totalPoints.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val completedSurahs by viewModel.completedSurahs.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isLoggedIn = viewModel.isLoggedIn
    val displayName = viewModel.userDisplayName.ifBlank { if (isLoggedIn) "User" else "Guest" }
    val email = viewModel.userEmail.ifBlank { "Sign in to post in the ummah" }

    if (!isLoggedIn) {
        StitchGuestProfileScreen(
            onBack = { viewModel.goBack() },
            onEdit = { viewModel.navigateTo(DeenScreen.LOGIN) },
            onSignIn = { viewModel.navigateTo(DeenScreen.LOGIN) }
        )
        return
    }

    val repository = remember { UmmahRepository() }
    val currentUid = remember(isLoggedIn) { repository.currentUserUid().orEmpty() }
    val feedState = rememberStitchUmmahFeed()
    val followers = rememberStitchFollowers(isLoggedIn)
    val followingUsers = rememberStitchFollowingUsers(isLoggedIn)
    val cleanHandle = viewModel.ummahUsername.ifBlank { email.substringBefore("@") }
        .ifBlank { displayName.replace(" ", "").lowercase() }
        .removePrefix("@")
    val profileHandle = "@$cleanHandle"
    val profilePosts = remember(feedState.posts, currentUid, cleanHandle, displayName, email) {
        val handleKey = cleanHandle.trim().lowercase()
        val emailHandleKey = email.substringBefore("@").trim().lowercase()
        val nameKey = displayName.trim().lowercase()
        feedState.posts.filter { post ->
            val postHandleKey = post.creatorHandle.removePrefix("@").trim().lowercase()
            val postNameKey = post.creatorName.trim().lowercase()
            post.creatorUid == currentUid ||
                (post.creatorUid.isBlank() && (
                    (handleKey.isNotBlank() && postHandleKey == handleKey) ||
                        (emailHandleKey.isNotBlank() && postHandleKey == emailHandleKey) ||
                        (nameKey.isNotBlank() && postNameKey == nameKey)
                    ))
        }
    }
    val postItems = remember(profilePosts) { profilePosts.filter { !it.isReelLike() } }
    val reelItems = remember(profilePosts) { profilePosts.filter { it.isReelLike() } }
    val profileBg = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color(0xFFF4FAFD)
    } else {
        MaterialTheme.colorScheme.background
    }

    StitchScreen {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(profileBg)
        ) {
            StitchProfilePlusPattern(Modifier.matchParentSize())
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 286.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            item {
                StitchMoreProfileTopBar(
                    initial = displayName.take(1).uppercase(),
                    onSearch = { viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) }
                )
            }
            item {
                StitchMoreSettingsProfileCard(
                    name = displayName,
                    handle = profileHandle,
                    email = viewModel.userEmail,
                    photoUrl = viewModel.userPhotoUrl,
                    posts = postItems.size,
                    reels = reelItems.size,
                    followers = followers.size,
                    following = followingUsers.size,
                    onOpen = { viewModel.openMyProfile() },
                    onEdit = { viewModel.navigateTo(DeenScreen.UMMAH_PROFILE_EDIT) }
                )
            }
            item {
                StitchStreakPointsCard(
                    streak = streak,
                    points = points,
                    onClick = { viewModel.navigateTo(DeenScreen.DEEN_POINTS) }
                )
            }
            item {
                StitchPremiumBanner(onUpgrade = { viewModel.navigateTo(DeenScreen.NOOR_PRO_PLUS) })
            }
            item {
                StitchProfileSection("Quick Access") {
                    StitchListRow("Bookmarks", null, Icons.Default.Bookmark) { viewModel.navigateTo(DeenScreen.BOOKMARKS) }
                    StitchDivider()
                    StitchListRow("99 Names of Allah", null, Icons.Default.Star) { viewModel.navigateTo(DeenScreen.ASMA_UL_HUSNA) }
                    StitchDivider()
                    StitchListRow("Zakat Calculator", null, Icons.Default.Calculate) { viewModel.navigateTo(DeenScreen.ZAKAT) }
                    StitchDivider()
                    StitchListRow("Daily Reminder", null, Icons.Default.Notifications) { viewModel.navigateTo(DeenScreen.SETTINGS) }
                }
            }
            item {
                StitchProfileSection("Your Activity") {
                    StitchListRow("Saved", "Saved reels and posts", Icons.Default.Bookmark) { viewModel.navigateTo(DeenScreen.UMMAH_SAVED) }
                    StitchDivider()
                    StitchListRow("Archive", "Private archived Ummah content", Icons.Default.CloudQueue) { viewModel.navigateTo(DeenScreen.UMMAH_ARCHIVE) }
                    StitchDivider()
                    StitchListRow("Likes & comments", "See posts you liked and comments you made", Icons.Default.ChatBubble) { viewModel.navigateTo(DeenScreen.UMMAH_LIKES_COMMENTS) }
                    StitchDivider()
                    StitchListRow("Time management", "Daily usage reminders and focus controls", Icons.Default.CalendarMonth) { viewModel.navigateTo(DeenScreen.UMMAH_TIME_MANAGEMENT) }
                }
            }
            item {
                StitchProfileSection("Daily Dashboard") {
                    StitchListRow("Spiritual Progress", "Streak, daily checklist & weekly yield", Icons.Default.AutoGraph) { viewModel.navigateTo(DeenScreen.SPIRITUAL_PROGRESS) }
                    StitchDivider()
                    StitchListRow("Prayer Tracker", null, Icons.Default.CheckCircle) { viewModel.navigateTo(DeenScreen.QAZA_TRACKER) }
                    StitchDivider()
                    StitchListRow("Tasbih", null, Icons.Default.Fingerprint) { viewModel.navigateTo(DeenScreen.TASBIH) }
                    StitchDivider()
                    StitchListRow("Dua & Adhkar", null, Icons.Default.VolunteerActivism) { viewModel.navigateTo(DeenScreen.DUA_HUB) }
                }
            }
            item {
                StitchHubSectionLabel("Knowledge & Growth")
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        StitchHubTile("Discover Library", Icons.Default.LocalLibrary, Modifier.weight(1f)) { viewModel.navigateTo(DeenScreen.LIBRARY_DASHBOARD) }
                        StitchHubTile("Quiz", Icons.Default.Quiz, Modifier.weight(1f)) { viewModel.navigateTo(DeenScreen.QUIZ_DASHBOARD) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        StitchHubTile("Learning", Icons.Default.AutoStories, Modifier.weight(1f)) { viewModel.navigateTo(DeenScreen.QURAN_LEARNING_DASHBOARD) }
                        StitchHubTile("Hajj & Umrah", Icons.Default.Mosque, Modifier.weight(1f)) { viewModel.navigateTo(DeenScreen.HAJJ_UMRAH) }
                    }
                }
            }
            item {
                StitchProfileSection("Lifestyle") {
                    StitchListRow("Health", null, Icons.Default.Favorite) { viewModel.navigateTo(DeenScreen.HEALTH_WELLNESS) }
                    StitchDivider()
                    StitchListRow("DeenPoints", null, Icons.Default.Star) { viewModel.navigateTo(DeenScreen.DEEN_POINTS) }
                    StitchDivider()
                    StitchListRow("Hijri Calendar", null, Icons.Default.CalendarMonth) { viewModel.navigateTo(DeenScreen.CALENDAR) }
                }
            }
            item {
                StitchProfileSection("Settings & Privacy") {
                    StitchListRow("Notifications", "Reels, comments, follows and reminders", Icons.Default.Notifications) { viewModel.navigateTo(DeenScreen.UMMAH_NOTIFICATIONS) }
                    StitchDivider()
                    StitchListRow("Profile visibility", "Profile previews, counts, and blocked accounts", Icons.Default.Fingerprint) { viewModel.navigateTo(DeenScreen.UMMAH_PRIVACY) }
                    StitchDivider()
                    StitchListRow("App Constitution", "Islamic content rules, moderation and account safety", Icons.Default.Gavel) { viewModel.navigateTo(DeenScreen.UMMAH_CONSTITUTION) }
                    StitchDivider()
                    StitchListRow("Close friends", "Create your private Ummah audience", Icons.Default.Groups) { viewModel.navigateTo(DeenScreen.UMMAH_CLOSE_FRIENDS) }
                    StitchDivider()
                    StitchListRow("Blocked", "Manage blocked accounts", Icons.Default.Logout) { viewModel.navigateTo(DeenScreen.UMMAH_BLOCKED) }
                }
            }
            item {
                StitchProfileSection("Subscription") {
                    StitchListRow("Noor Pro Plus", "Premium AI, learning tools and creator features", Icons.Default.Star) { viewModel.navigateTo(DeenScreen.NOOR_PRO_PLUS) }
                }
            }
            item {
                StitchProfileSection("Preferences") {
                    StitchListRow(
                        title = "Light Theme",
                        subtitle = null,
                        icon = Icons.Default.LightMode,
                        trailing = {
                            Switch(
                                checked = themeMode != ThemeMode.DARK,
                                onCheckedChange = { checked ->
                                    viewModel.updateThemeMode(if (checked) ThemeMode.LIGHT else ThemeMode.DARK)
                                }
                            )
                        }
                    ) {
                        viewModel.updateThemeMode(if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
                    }
                    StitchDivider()
                    StitchListRow("Adhan Notification", null, Icons.Default.Notifications) { viewModel.navigateTo(DeenScreen.SETTINGS) }
                    StitchDivider()
                    StitchListRow(
                        title = "Language",
                        subtitle = null,
                        icon = Icons.Default.Language,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("English", color = stitchMutedText(), fontSize = 14.sp)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
                            }
                        }
                    ) { viewModel.navigateTo(DeenScreen.SETTINGS) }
                }
            }
            item {
                StitchProfileSection("Account") {
                    StitchListRow("Account Settings", null, Icons.Default.Settings) { viewModel.navigateTo(DeenScreen.SETTINGS) }
                    StitchDivider()
                    StitchListRow(
                        "Switch Account",
                        "Add accounts and switch securely by Firebase uid",
                        Icons.Default.Person
                    ) { viewModel.navigateTo(DeenScreen.ACCOUNT_SWITCHER) }
                    StitchDivider()
                    if (isLoggedIn) {
                        StitchListRow(
                            title = "Sign Out",
                            subtitle = null,
                            icon = Icons.Default.Logout,
                            iconTint = Color(0xFFBA1A1A),
                            titleColor = Color(0xFFBA1A1A),
                            trailing = {}
                        ) {
                            viewModel.handleLogout()
                            viewModel.navigateTo(DeenScreen.LOGIN)
                        }
                    } else {
                        StitchListRow("Sign In", "Post in the Ummah and sync progress", Icons.Default.Person) {
                            viewModel.navigateTo(DeenScreen.LOGIN)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun StitchGuestProfileScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSignIn: () -> Unit
) {
    StitchScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = stitchText(),
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "Profile",
                    color = stitchText(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Sign in",
                        tint = stitchPrimary(),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(88.dp))

            StitchGuestSignInProfileCard(onSignIn = onSignIn)
        }
    }
}

@Composable
private fun StitchGuestSignInProfileCard(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = stitchPrimary().copy(alpha = 0.08f),
                spotColor = stitchPrimary().copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .padding(horizontal = 34.dp, vertical = 58.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 54.dp, y = (-58).dp)
                .size(174.dp)
                .clip(CircleShape)
                .background(stitchSoftSurface())
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                stitchPrimary().copy(alpha = 0.10f),
                                stitchPrimary().copy(alpha = 0.02f)
                            )
                        )
                    )
                    .border(1.dp, stitchPrimary().copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = stitchPrimary().copy(alpha = 0.78f),
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(38.dp))
            Text(
                "Guest Profile",
                color = stitchText(),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                "Sign in to access your personal sanctuary, view posts, and save reels.",
                color = stitchMutedText(),
                fontSize = 20.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )
            Spacer(modifier = Modifier.height(54.dp))
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = stitchPrimary(),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(vertical = 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
            ) {
                Text("Sign In", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StitchProfilePlusPattern(modifier: Modifier = Modifier) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val tint = if (isLight) stitchPrimary().copy(alpha = 0.035f) else Color.White.copy(alpha = 0.025f)
    Canvas(modifier = modifier) {
        val step = 38.dp.toPx()
        val arm = 4.5.dp.toPx()
        val stroke = 1.dp.toPx()
        var x = 0f
        while (x <= size.width + step) {
            var y = 0f
            while (y <= size.height + step) {
                drawLine(tint, Offset(x - arm, y), Offset(x + arm, y), stroke, StrokeCap.Round)
                drawLine(tint, Offset(x, y - arm), Offset(x, y + arm), stroke, StrokeCap.Round)
                y += step
            }
            x += step
        }
    }
}

@Composable
private fun StitchMoreProfileTopBar(
    initial: String,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(stitchPrimary()),
            contentAlignment = Alignment.Center
        ) {
            Text(initial.ifBlank { "U" }, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("More", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onSearch) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = stitchText(), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun StitchMoreSettingsProfileCard(
    name: String,
    handle: String,
    email: String,
    photoUrl: String,
    posts: Int,
    reels: Int,
    followers: Int,
    following: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = stitchPrimary().copy(alpha = 0.07f),
                spotColor = stitchPrimary().copy(alpha = 0.09f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(stitchSurface())
            .clickable { onOpen() }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(stitchPrimary()),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(name.take(1).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    color = stitchText(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(handle, color = stitchPrimary(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (email.isNotBlank()) {
                    Text(email, color = stitchMutedText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = stitchPrimary())
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(stitchSoftSurface())
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StitchMoreCompactStat(posts.toString(), "Posts", Modifier.weight(1f))
            StitchStatDivider()
            StitchMoreCompactStat(reels.toString(), "Reels", Modifier.weight(1f))
            StitchStatDivider()
            StitchMoreCompactStat(followers.toString(), "Followers", Modifier.weight(1f))
            StitchStatDivider()
            StitchMoreCompactStat(following.toString(), "Following", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StitchMoreCompactStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = StitchGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun StitchMoreProfileHero(
    name: String,
    handle: String,
    email: String,
    photoUrl: String,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(118.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(5.dp, Color.White, CircleShape)
                .clickable { onEdit() },
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(stitchPrimary()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(1).uppercase(), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-14).dp, y = 8.dp)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD73535))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            name,
            color = stitchText(),
            fontSize = 36.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 340.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(handle, color = stitchPrimary(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(email, color = stitchMutedText(), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StitchMoreProfileStats(
    posts: Int,
    reels: Int,
    followers: Int,
    following: Int,
    onPosts: () -> Unit,
    onReels: () -> Unit,
    onFollowers: () -> Unit,
    onFollowing: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = stitchPrimary().copy(alpha = 0.06f), spotColor = stitchPrimary().copy(alpha = 0.08f))
            .clip(RoundedCornerShape(14.dp))
            .background(stitchSurface())
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StitchMoreProfileStat(posts.toString(), "Posts", Modifier.weight(1f), onPosts)
        StitchStatDivider()
        StitchMoreProfileStat(reels.toString(), "Reels", Modifier.weight(1f), onReels)
        StitchStatDivider()
        StitchMoreProfileStat(followers.toString(), "Followers", Modifier.weight(1f), onFollowers)
        StitchStatDivider()
        StitchMoreProfileStat(following.toString(), "Following", Modifier.weight(1f), onFollowing)
    }
}

@Composable
private fun StitchMoreProfileStat(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = StitchGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = stitchText(), fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun StitchStatDivider() {
    Box(
        modifier = Modifier
            .height(42.dp)
            .width(1.dp)
            .background(StitchLine.copy(alpha = 0.75f))
    )
}

@Composable
private fun StitchMoreProfileActions(
    onNewPost: () -> Unit,
    onNewReel: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onNewPost,
            colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(56.dp)
        ) {
            Text("New Post", fontSize = 16.sp)
        }
        Button(
            onClick = onNewReel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD75E), contentColor = stitchPrimary()),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(56.dp)
        ) {
            Text("New Reel", fontSize = 16.sp)
        }
    }
}

@Composable
private fun StitchMoreProfileUtilityCards(
    onCreatorStudio: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(stitchPrimary())
                .clickable { onCreatorStudio() }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFD75E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(27.dp))
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Creator Studio", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Private analytics dashboard", color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun StitchMoreProfileContentTabs(
    posts: Int,
    reels: Int,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StitchMoreProfileTab("Posts $posts", selected == "posts") { onSelect("posts") }
        StitchMoreProfileTab("Reels $reels", selected == "reels") { onSelect("reels") }
    }
}

@Composable
private fun StitchMoreProfileTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) stitchPrimary() else stitchSurface())
            .border(1.dp, if (selected) stitchPrimary() else StitchLine, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 28.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else stitchText(), fontSize = 15.sp)
    }
}

@Composable
private fun StitchMoreProfileContentPanel(
    selected: String,
    posts: List<UmmahPost>,
    onCreate: () -> Unit,
    onOpen: (UmmahPost) -> Unit
) {
    if (posts.isEmpty()) {
        StitchMoreProfileEmptyState(
            title = if (selected == "reels") "No reels yet" else "No posts yet",
            message = if (selected == "reels") "Your uploaded reels will appear here." else "Your uploaded posts will appear here.",
            action = if (selected == "reels") "Create Reel" else "Create Post",
            onAction = onCreate
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(stitchSurface())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            posts.take(4).forEach { post ->
                StitchMoreProfilePostPreview(post = post, onOpen = { onOpen(post) })
            }
        }
    }
}

@Composable
private fun StitchMoreProfileEmptyState(
    title: String,
    message: String,
    action: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(stitchSurface())
            .padding(horizontal = 28.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(stitchSoftSurface()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(title, color = stitchText(), fontSize = 27.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))
        Text(message, color = stitchMutedText(), fontSize = 18.sp, lineHeight = 28.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(34.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(56.dp).widthIn(min = 188.dp)
        ) {
            Text(action, fontSize = 17.sp)
        }
    }
}

@Composable
private fun StitchMoreProfilePostPreview(post: UmmahPost, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(stitchSoftSurface()),
            contentAlignment = Alignment.Center
        ) {
            if (post.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = post.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    if (post.isReelLike()) Icons.Default.PlayArrow else Icons.Default.Article,
                    contentDescription = null,
                    tint = stitchPrimary()
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                post.caption.ifBlank { post.arabicText.ifBlank { "Shared with the Ummah" } },
                color = stitchText(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${compactCount(post.viewCount)} views",
                color = stitchMutedText(),
                fontSize = 12.sp
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
    }
}

@Composable
private fun StitchProfileSummary(
    name: String,
    photoUrl: String,
    isLoggedIn: Boolean,
    onEdit: () -> Unit
) {
    // Mock-matched identity card: gold-ring avatar, name, "Keep your faith strong", gold pencil.
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(if (isLoggedIn) StitchEmeraldDeep else Color.Transparent)
                    .border(2.5.dp, StitchGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoggedIn && photoUrl.isNotBlank() -> AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                    isLoggedIn -> Text(name.take(1).uppercase(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    else -> Icon(Icons.Default.Person, contentDescription = null, tint = StitchEmerald, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    if (isLoggedIn) "Keep your faith strong" else "Sign in to begin your journey",
                    color = stitchMutedText(),
                    fontSize = 14.sp
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = StitchGold)
            }
        }
    }
}

/** Mock-matched green combo card: Streak      N days | Points     N points, with a gold badge. */
@Composable
private fun StitchStreakPointsCard(streak: Int, points: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(StitchEmerald, StitchEmeraldDeep)))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Streak", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = StitchGold, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("$streak", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("days", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            Box(Modifier.width(1.dp).height(64.dp).background(Color.White.copy(alpha = 0.25f)))
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1.2f)) {
                Text("Points", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = StitchGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("$points", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Text("points", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(StitchGold.copy(alpha = 0.22f))
                    .border(1.5.dp, StitchGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = StitchGold, modifier = Modifier.size(30.dp))
            }
        }
    }
}

/** Mock-matched gold "Upgrade to Premium" banner. */
@Composable
private fun StitchPremiumBanner(onUpgrade: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StitchGold.copy(alpha = 0.12f))
            .border(1.dp, StitchGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .clickable(onClick = onUpgrade)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = StitchGold, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Upgrade to Premium", color = StitchGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Unlock all features & remove ads", color = stitchMutedText(), fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(StitchGold)
                .clickable(onClick = onUpgrade)
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text("Upgrade", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StitchHubTile(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    StitchCard(
        modifier = modifier
            .height(116.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(stitchSoftSurface()),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(title, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StitchHubSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        color = StitchGold,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun StitchProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StitchHubSectionLabel(title)
        StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(content = content)
        }
    }
}

@Composable
private fun StitchDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 82.dp)
            .background(StitchLine.copy(alpha = 0.65f))
    )
}

private data class StitchUmmahFeedState(
    val posts: List<UmmahPost>,
    val error: String?,
    val loading: Boolean
)

private data class StitchInteractionState(
    val liked: Set<String>,
    val saved: Set<String>
)

private data class StitchSavedLibraryState(
    val collections: List<UmmahSavedCollection>,
    val assignments: Map<String, String>
)

private data class StitchUserCommentState(
    val comments: List<UmmahComment>,
    val loading: Boolean
)

@Composable
private fun rememberStitchUmmahFeed(refreshKey: Int = 0): StitchUmmahFeedState {
    val repository = remember(refreshKey) { UmmahRepository() }
    var posts by remember { mutableStateOf<List<UmmahPost>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, com.noorpro.app.data.UmmahProfile>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember(refreshKey) { mutableStateOf(true) }

    DisposableEffect(repository) {
        repository.observeApprovedPosts { livePosts, liveError ->
            posts = livePosts
            error = liveError
            loading = false
        }
        // Live public profiles: overlay current name/@username/photo onto every post so a
        // profile edit updates reels + Ummah instantly (Instagram-style), even on old posts.
        repository.observeProfiles { profiles = it }
        onDispose { repository.close() }
    }

    val mergedPosts = remember(posts, profiles) {
        if (profiles.isEmpty()) posts else posts.map { post ->
            // Only overlay a live profile when the post has a real creator uid. Legacy reels/posts
            // stored a blank creatorUid; looking those up (profiles[""]) could match a stray
            // blank-keyed profile and stamp ONE person's name/@handle/photo onto many different
            // reels     the "usernames mixed across reels" bug. A blank uid keeps its own stored name.
            val profile = post.creatorUid.takeIf { it.isNotBlank() }?.let { profiles[it] } ?: return@map post
            post.copy(
                creatorName = profile.name.ifBlank { post.creatorName },
                creatorHandle = profile.handle.ifBlank { post.creatorHandle.removePrefix("@") }
                    .let { if (it.startsWith("@")) it else "@$it" },
                creatorPhotoUrl = profile.photoUrl.ifBlank { post.creatorPhotoUrl }
            )
        }
    }

    return StitchUmmahFeedState(posts = mergedPosts, error = error, loading = loading)
}

/**
 * Facebook/Instagram-style "For You" ranking. Each post is scored by:
 *  - freshness (strong at first, decaying over ~2 days)
 *  - engagement (comments and shares weigh more than likes, log-dampened so one viral
 *    post doesn't pin the feed forever)
 *  - relationship (creators you follow, creators whose posts you've liked/saved)
 *  - interest (categories you've liked/saved before)
 * A small deterministic jitter keeps the order feeling alive without reshuffling on
 * every recomposition.
 */
private fun rankUmmahPosts(
    posts: List<UmmahPost>,
    following: Set<String>,
    liked: Set<String>,
    saved: Set<String>,
    reelFeedback: ReelFeedback = ReelFeedback()
): List<UmmahPost> {
    if (posts.size < 2) return posts
    val likedCreators = posts.filter { it.id in liked }.map { it.creatorUid }.toSet()
    val savedCreators = posts.filter { it.id in saved }.map { it.creatorUid }.toSet()
    val interestCategories = posts.filter { it.id in liked || it.id in saved }.map { it.category }.toSet()
    val now = System.currentTimeMillis()

    fun score(post: UmmahPost): Double {
        var score = 0.0
        // Freshness: ~100 for a brand-new post, halving roughly every 12 hours.
        val hours = ((now - post.publishedAt).coerceAtLeast(0L)) / 3_600_000.0
        score += 100.0 / (1.0 + hours / 12.0)
        // Engagement: comments/shares signal more than likes; ln-dampened.
        score += 14.0 * kotlin.math.ln(1.0 + post.likeCount) +
            22.0 * kotlin.math.ln(1.0 + post.commentCount) +
            26.0 * kotlin.math.ln(1.0 + post.shareCount)
        // Relationship boosts.
        if (post.creatorUid in following) score += 45.0
        if (post.creatorUid in likedCreators) score += 18.0
        if (post.creatorUid in savedCreators) score += 22.0
        // Interest match on category.
        if (post.category in interestCategories) score += 15.0
        score += reelFeedbackScore(post, reelFeedback)
        // Deterministic jitter (stable per post) so equal scores don't always tie the same way.
        score += (post.id.hashCode() % 7)
        return score
    }
    return posts.sortedByDescending { score(it) }
}

/** Live uid     public profile map, so avatars/names update everywhere the moment a profile
 *  is edited (photo, name, @username). */
@Composable
private fun rememberStitchProfiles(): Map<String, com.noorpro.app.data.UmmahProfile> {
    val repository = remember { UmmahRepository() }
    var profiles by remember { mutableStateOf<Map<String, com.noorpro.app.data.UmmahProfile>>(emptyMap()) }
    DisposableEffect(repository) {
        repository.observeProfiles { profiles = it }
        onDispose { repository.close() }
    }
    return profiles
}

@Composable
private fun rememberStitchFollowing(isLoggedIn: Boolean): Set<String> {
    val repository = remember(isLoggedIn) { UmmahRepository() }
    var following by remember(isLoggedIn) { mutableStateOf<Set<String>>(emptySet()) }

    DisposableEffect(repository, isLoggedIn) {
        repository.observeFollowing { following = it }
        onDispose { repository.close() }
    }

    return following
}

@Composable
private fun rememberStitchFollowingUsers(isLoggedIn: Boolean, targetUid: String? = null): List<UmmahFollowUser> {
    val repository = remember(isLoggedIn, targetUid) { UmmahRepository() }
    var users by remember(isLoggedIn, targetUid) { mutableStateOf<List<UmmahFollowUser>>(emptyList()) }
    DisposableEffect(repository, isLoggedIn, targetUid) {
        repository.observeFollowingUsers(targetUid) { users = it }
        onDispose { repository.close() }
    }
    return users
}

@Composable
private fun rememberStitchFollowers(isLoggedIn: Boolean, targetUid: String? = null): List<UmmahFollowUser> {
    val repository = remember(isLoggedIn, targetUid) { UmmahRepository() }
    var users by remember(isLoggedIn, targetUid) { mutableStateOf<List<UmmahFollowUser>>(emptyList()) }
    DisposableEffect(repository, isLoggedIn, targetUid) {
        repository.observeFollowers(targetUid) { users = it }
        onDispose { repository.close() }
    }
    return users
}

@Composable
private fun rememberStitchInteractions(isLoggedIn: Boolean): StitchInteractionState {
    val repository = remember(isLoggedIn) { UmmahRepository() }
    var liked by remember(isLoggedIn) { mutableStateOf<Set<String>>(emptySet()) }
    var saved by remember(isLoggedIn) { mutableStateOf<Set<String>>(emptySet()) }

    DisposableEffect(repository, isLoggedIn) {
        repository.observeUserInteractions { liveLiked, liveSaved ->
            liked = liveLiked
            saved = liveSaved
        }
        onDispose { repository.close() }
    }

    return StitchInteractionState(liked = liked, saved = saved)
}

@Composable
private fun rememberStitchSavedLibrary(isLoggedIn: Boolean): StitchSavedLibraryState {
    val repository = remember(isLoggedIn) { UmmahRepository() }
    var collections by remember(isLoggedIn) { mutableStateOf<List<UmmahSavedCollection>>(emptyList()) }
    var assignments by remember(isLoggedIn) { mutableStateOf<Map<String, String>>(emptyMap()) }

    DisposableEffect(repository, isLoggedIn) {
        repository.observeSavedCollections { collections = it }
        repository.observeSavedAssignments { assignments = it }
        onDispose { repository.close() }
    }

    return StitchSavedLibraryState(collections = collections, assignments = assignments)
}

@Composable
private fun rememberStitchUserComments(isLoggedIn: Boolean): StitchUserCommentState {
    val repository = remember(isLoggedIn) { UmmahRepository() }
    var comments by remember(isLoggedIn) { mutableStateOf<List<UmmahComment>>(emptyList()) }
    var loading by remember(isLoggedIn) { mutableStateOf(isLoggedIn) }

    DisposableEffect(repository, isLoggedIn) {
        repository.observeUserComments { liveComments ->
            comments = liveComments
            loading = false
        }
        onDispose { repository.close() }
    }

    return StitchUserCommentState(comments = comments, loading = loading)
}

private fun shareStitchUmmahPost(context: android.content.Context, post: UmmahPost, repository: UmmahRepository): Boolean {
    val body = buildString {
        val text = post.caption.ifBlank { post.arabicText }.trim()
        if (text.isNotBlank()) {
            append(text)
            append("\n\n")
        }
        // Share a clean Noor Pro web link (never a raw cloud/storage URL). The page plays the
        // reel/post in the browser and offers the app download.
        append("    Watch on Noor Pro: https://noor-pro-d87e3.web.app/reel?id=")
        append(post.id)
        append("\n\n")
        append("Get the Noor Pro app: https://noor-pro-d87e3.web.app")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Noor Pro reel")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(intent, "Share from Noor Pro"))
        repository.incrementShare(post.id)
        true
    }.getOrElse {
        Toast.makeText(context, "No sharing app is available", Toast.LENGTH_SHORT).show()
        false
    }
}

private fun copyStitchReelLink(context: android.content.Context, postId: String) {
    val link = "https://noor-pro-d87e3.web.app/reel?id=$postId"
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Noor Pro reel", link))
    Toast.makeText(context, "Reel link copied", Toast.LENGTH_SHORT).show()
}

private fun UmmahPost.isReelLike(): Boolean {
    val cleanType = type.lowercase()
    return cleanType == "reel" || cleanType == "short"
}

private fun UmmahPost.isVideoMedia(): Boolean {
    val cleanType = type.lowercase()
    val cleanUrl = mediaUrl.lowercase()
    return cleanType == "reel" ||
        cleanType == "video" ||
        cleanUrl.endsWith(".mp4") ||
        cleanUrl.endsWith(".webm") ||
        cleanUrl.endsWith(".mov") ||
        cleanUrl.contains("video")
}

private fun UmmahPost.opensInVideoViewer(): Boolean = isReelLike() || isVideoMedia()

private data class StitchReelQuality(
    val label: String,
    val maxWidth: Int,
    val maxHeight: Int
)

private val stitchReelQualityOptions = listOf(
    StitchReelQuality("Auto", Int.MAX_VALUE, Int.MAX_VALUE),
    StitchReelQuality("480p", 854, 480),
    StitchReelQuality("720p", 1280, 720),
    StitchReelQuality("1080p", 1920, 1080)
)

private val stitchReelSpeedOptions = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

private fun ExoPlayer.applyStitchReelQuality(quality: StitchReelQuality) {
    setTrackSelectionParameters(
        trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(quality.maxWidth, quality.maxHeight)
            .build()
    )
}

/** Toggle subtitles/captions if the video carries a text track (no-op otherwise). */
private fun ExoPlayer.applyStitchCaptions(enabled: Boolean) {
    setTrackSelectionParameters(
        trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !enabled)
            .setSelectUndeterminedTextLanguage(enabled)
            .build()
    )
}

private fun compactCount(value: Long): String {
    return when {
        value >= 1_000_000 -> "${value / 1_000_000}M"
        value >= 1_000 -> "${value / 1_000}k"
        else -> value.toString()
    }
}

private fun timeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return "Just now"
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun StitchReelsScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    var feedRefreshKey by remember { mutableStateOf(0) }
    val feedState = rememberStitchUmmahFeed(feedRefreshKey)
    // Instagram-style pull-to-refresh: swipe down on the first reel to reload the feed.
    var reelsRefreshing by remember { mutableStateOf(false) }
    val following = rememberStitchFollowing(viewModel.isLoggedIn)
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    val followRepository = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { followRepository.currentUserUid().orEmpty() }
    val feedbackStore = remember(context, currentUid) { ReelFeedbackStore(context.applicationContext, currentUid) }
    var reelFeedback by remember(feedbackStore) { mutableStateOf(feedbackStore.load()) }
    var topTab by remember { mutableStateOf("For You") }
    var userPaused by remember { mutableStateOf(false) }
    var landscapeViewer by remember { mutableStateOf(false) }
    var commentPost by remember { mutableStateOf<UmmahPost?>(null) }
    var optimisticFollowing by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    // Optimistic like/save state so tapping updates the heart/bookmark INSTANTLY (Instagram-smooth)
    // instead of waiting for the Firestore round-trip; reverted only if the write fails.
    var optimisticLikes by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var optimisticSaves by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var optimisticCommentCounts by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var optimisticShareCounts by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var playbackErrorReelId by remember { mutableStateOf<String?>(null) }
    var playbackRetryToken by remember { mutableStateOf(0) }
    // Reels already counted as a view this session (dedupe so one watch = one view).
    var viewedReelIds by remember { mutableStateOf(emptySet<String>()) }
    val requestedReelId = viewModel.reelFocusId.trim()
    val rankedReels = remember(
        feedState.posts, topTab, following, interactions.liked, interactions.saved, reelFeedback
    ) {
        feedState.posts
            .filter { it.opensInVideoViewer() }
            .filter { topTab != "Following" || it.creatorUid in following }
            .filterNot { it.id in reelFeedback.hiddenReelIds }
            .let { list ->
                if (topTab == "Following") list else {
                    rankUmmahPosts(list, following, interactions.liked, interactions.saved, reelFeedback)
                }
            }
    }
    var stableReelIds by remember(feedRefreshKey, topTab, currentUid) { mutableStateOf(emptyList<String>()) }
    val orderedReelIds = stableReelOrder(stableReelIds, rankedReels.map { it.id })
    SideEffect {
        if (stableReelIds != orderedReelIds) stableReelIds = orderedReelIds
    }
    val reelById = remember(rankedReels) { rankedReels.associateBy { it.id } }
    val reels = remember(orderedReelIds, reelById) { orderedReelIds.mapNotNull(reelById::get) }
    val displayName = viewModel.userDisplayName.ifBlank { if (viewModel.isLoggedIn) "Noor" else "Guest" }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    // A reel opened from Profile/Saved/Liked always belongs to the full feed.
    LaunchedEffect(requestedReelId) {
        if (requestedReelId.isNotBlank()) {
            topTab = "For You"
            reelFeedback = feedbackStore.unhide(requestedReelId)
        }
    }

    val activity = context as? Activity
    fun updateReelLandscape(enabled: Boolean) {
        landscapeViewer = enabled
        // Facebook-style wide view: actually rotate the Activity to landscape so the 16:9 video
        // fills the screen naturally. The manifest's android:configChanges keeps the Activity (and
        // the ExoPlayer) alive across the rotation instead of recreating it.
        activity?.requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        viewModel.setUmmahReelsImmersive(enabled)
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            viewModel.setUmmahReelsImmersive(false)
        }
    }
    // Back press exits the wide/landscape view first instead of leaving the reels feed.
    BackHandler(enabled = landscapeViewer) { updateReelLandscape(false) }

    // One ExoPlayer for the whole reels feed; it follows the settled page. A snapping
    // VerticalPager locks scroll to exactly one reel, and a single decoder (instead of one
    // per visible full-height item) is what removes the stutter on fast scrolls.
    val pagerState = rememberPagerState(pageCount = { reels.size })
    LaunchedEffect(reels.size) {
        if (reels.isNotEmpty() && pagerState.currentPage >= reels.size) {
            pagerState.scrollToPage(reels.lastIndex)
        }
    }
    val reelPlayer = remember {
        // Tuned load control: start playback after ~0.6s of buffer instead of the default 2.5s,
        // so swiping to the next reel feels instant (Instagram/Shorts-style), while still
        // buffering ahead generously once playing.
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 600,
                /* bufferForPlaybackAfterRebufferMs = */ 1_200
            )
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
            }
    }
    DisposableEffect(Unit) { onDispose { reelPlayer.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { reelPlayer.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (!userPaused) reelPlayer.play() }

    // When a pull-to-refresh finishes, jump back to the top so the newest reel plays    
    // the same behaviour as Instagram.
    LaunchedEffect(feedState.loading) {
        if (!feedState.loading && reelsRefreshing) {
            reelsRefreshing = false
            if (reels.isNotEmpty()) pagerState.scrollToPage(0)
        }
    }
    val activePage = pagerState.settledPage
    val activePost = reels.getOrNull(activePage)
    // Reels whose adaptive HLS stream failed (e.g. still transcoding)     play their MP4 instead.
    var hlsFailedIds by remember { mutableStateOf(emptySet<String>()) }
    // Keep measured dimensions per reel so a newly selected portrait reel never briefly inherits
    // the previous wide reel's Rotate button or letterboxed layout.
    var measuredReelAspects by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }

    // Watch-counting + ad cadence + landscape reset on each newly settled reel.
    LaunchedEffect(activePost?.id) {
        if (activePost == null) return@LaunchedEffect
        playbackErrorReelId = null
        if (landscapeViewer) updateReelLandscape(false)
        // Count one real view per reel per session (deduped). The Firestore increment flows back
        // through the live feed listener, so the count updates in search, profile & Creator Studio.
        val watchedId = activePost.id
        if (watchedId.isNotBlank() && watchedId !in viewedReelIds) {
            viewedReelIds = viewedReelIds + watchedId
            followRepository.incrementViews(watchedId)
        }
        userPaused = false
    }

    // Load the active reel's stream. Prefer adaptive HLS (smooth on any network); if it errors or
    // is still transcoding, hlsFailedIds flips and this re-runs to play the original MP4.
    val useMp4Fallback = activePost != null && activePost.id in hlsFailedIds
    LaunchedEffect(activePost?.id, useMp4Fallback, playbackRetryToken) {
        val post = activePost ?: run { reelPlayer.clearMediaItems(); return@LaunchedEffect }
        // HLS files created before source geometry was stored were forced to 16:9 by the old AWS
        // job. Play their original MP4 so existing portrait reels are repaired too.
        val hasTrustedSourceGeometry = uploadedReelAspectRatio(
            post.mediaWidth,
            post.mediaHeight,
            post.mediaRotationDegrees
        ) != null
        val playUrl = if (post.hlsUrl.isNotBlank() && hasTrustedSourceGeometry && post.id !in hlsFailedIds) {
            post.hlsUrl
        } else {
            post.mediaUrl
        }
        if (playUrl.isNotBlank()) {
            reelPlayer.setMediaItem(MediaItem.Builder().setUri(Uri.parse(playUrl)).setMediaId(post.id).build())
            reelPlayer.prepare()
            reelPlayer.seekTo(0)
            if (!userPaused) reelPlayer.play()
        } else {
            reelPlayer.clearMediaItems()
        }
    }

    // HLS     MP4 fallback: if the active reel's HLS stream errors, mark it so the loader retries
    // with the original MP4 (which is always present).
    val activePostState = rememberUpdatedState(activePost)
    val hlsFailedState = rememberUpdatedState(hlsFailedIds)
    DisposableEffect(reelPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY &&
                    reelPlayer.currentMediaItem?.mediaId == activePostState.value?.id
                ) playbackErrorReelId = null
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                val post = activePostState.value ?: return
                if (reelPlayer.currentMediaItem?.mediaId != post.id) return
                val sourceAspect = uploadedReelAspectRatio(
                    post.mediaWidth, post.mediaHeight, post.mediaRotationDegrees
                )
                // Media3 applies container rotation to VideoSize on supported Android devices, so
                // the displayed dimensions are safe as the fallback for pre-metadata MP4 reels.
                val streamAspect = displayedReelAspectRatio(
                    width = videoSize.width,
                    height = videoSize.height,
                    pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio
                )
                if (post.id !in hlsFailedState.value &&
                    reelPlayer.currentMediaItem?.localConfiguration?.uri?.toString() == post.hlsUrl &&
                    shouldFallbackFromPaddedHls(sourceAspect, streamAspect)
                ) {
                    hlsFailedIds = hlsFailedIds + post.id
                    return
                }
                val aspect = sourceAspect ?: streamAspect
                if (aspect.isFinite() && aspect > 0f) {
                    measuredReelAspects = measuredReelAspects + (post.id to aspect)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val post = activePostState.value ?: return
                if (reelPlayer.currentMediaItem?.mediaId != post.id) return
                if (post.id !in hlsFailedState.value &&
                    reelPlayer.currentMediaItem?.localConfiguration?.uri?.toString() == post.hlsUrl
                ) {
                    hlsFailedIds = hlsFailedIds + post.id
                } else {
                    playbackErrorReelId = post.id
                }
            }
        }
        reelPlayer.addListener(listener)
        onDispose { reelPlayer.removeListener(listener) }
    }

    // Resolve the exact reel ID. A separate Profile listener can receive a new upload slightly
    // before this feed listener; retain the request during that gap instead of clearing it and
    // leaving the pager on an unrelated old reel.
    val reelOrderKey = remember(reels) { reels.joinToString(separator = "|") { it.id } }
    val focusedReelIndex = requestedReelIndex(reels.map { it.id }, requestedReelId)
    val waitingForFocusedReel = requestedReelId.isNotBlank() && focusedReelIndex == null
    LaunchedEffect(reelOrderKey, requestedReelId, topTab, feedState.loading) {
        val focusId = requestedReelId
        if (focusId.isBlank()) return@LaunchedEffect
        val index = requestedReelIndex(reels.map { it.id }, focusId)
        if (index != null) {
            pagerState.scrollToPage(index)
            userPaused = false
            reelPlayer.play()
            if (viewModel.reelFocusId == focusId) viewModel.reelFocusId = ""
        } else if (!feedState.loading && topTab == "For You") {
            // Give Firestore's second listener time to deliver the newly uploaded submission.
            // If it never arrives, report that exact reel as unavailable; never open an old one.
            delay(2_500)
            if (viewModel.reelFocusId == focusId && requestedReelIndex(reels.map { it.id }, focusId) == null) {
                Toast.makeText(context, "This reel is not available yet. Pull down to refresh.", Toast.LENGTH_LONG).show()
                viewModel.reelFocusId = ""
            }
        }
    }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = reelsRefreshing,
        onRefresh = {
            reelsRefreshing = true
            feedRefreshKey++
        },
        modifier = Modifier.fillMaxSize()
    ) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            feedState.loading -> {
                CircularProgressIndicator(
                    color = StitchGold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            feedState.error != null -> {
                StitchReelsEmptyState(
                    title = "Unable to load reels",
                    message = feedState.error,
                    onCreate = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            waitingForFocusedReel -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = StitchGold)
                    Spacer(Modifier.height(12.dp))
                    Text("Opening reel...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            reels.isEmpty() -> {
                StitchReelsEmptyState(
                    title = "No reels yet",
                    message = "Upload the first Islamic reminder reel for the Ummah.",
                    onCreate = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    // The live feed can shrink while Pager still asks for an old page key.
                    key = { page -> reels.getOrNull(page)?.id ?: "stale-reel-$page" },
                    // Preload the neighbouring reel so the next swipe shows instantly (Instagram-smooth).
                    beyondViewportPageCount = 1,
                    // Lower snap threshold = a short, light flick already advances to the next reel,
                    // so scrolling feels fast and effortless like Instagram (default needs a ~50% drag).
                    flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapPositionalThreshold = 0.25f
                    )
                ) { page ->
                    // A follow/filter/feed update may remove pages before Pager settles.
                    // Never index the changing list with a page from the previous frame.
                    val post = reels.getOrNull(page) ?: return@VerticalPager
                    val effectiveFollowing = optimisticFollowing[post.creatorUid] ?: (post.creatorUid in following)
                    val persistedLiked = post.id in interactions.liked
                    val effectiveLiked = optimisticLikes[post.id] ?: persistedLiked
                    val persistedSaved = post.id in interactions.saved
                    val effectiveSaved = optimisticSaves[post.id] ?: persistedSaved
                    val displayedLikeCount = (post.likeCount + when {
                        effectiveLiked && !persistedLiked -> 1L
                        !effectiveLiked && persistedLiked -> -1L
                        else -> 0L
                    }).coerceAtLeast(0L)
                    val displayedSaveCount = (post.saveCount + when {
                        effectiveSaved && !persistedSaved -> 1L
                        !effectiveSaved && persistedSaved -> -1L
                        else -> 0L
                    }).coerceAtLeast(0L)
                        StitchLiveReelPage(
                            post = post,
                            displayName = displayName,
                            ownUsername = viewModel.ummahUsername,
                            modifier = Modifier.fillMaxSize(),
                            isFollowing = effectiveFollowing,
                            isOwnPost = currentUid.isNotBlank() && post.creatorUid == currentUid,
                            isLiked = effectiveLiked,
                            isSaved = effectiveSaved,
                            displayLikeCount = displayedLikeCount,
                            displayCommentCount = maxOf(post.commentCount, optimisticCommentCounts[post.id] ?: 0L),
                            displaySaveCount = displayedSaveCount,
                            displayShareCount = maxOf(post.shareCount, optimisticShareCounts[post.id] ?: 0L),
                            isActive = page == activePage,
                            isPlaying = page == activePage && !userPaused,
                            playbackFailed = page == activePage && playbackErrorReelId == post.id,
                            videoAspectRatio = uploadedReelAspectRatio(
                                post.mediaWidth, post.mediaHeight, post.mediaRotationDegrees
                            ) ?: measuredReelAspects[post.id] ?: 0f,
                            landscapeMode = landscapeViewer,
                            player = reelPlayer,
                            onFollow = {
                                if (post.creatorUid == currentUid) {
                                    Toast.makeText(context, "This is your reel", Toast.LENGTH_SHORT).show()
                                } else if (post.creatorUid.isBlank()) {
                                    Toast.makeText(context, "This creator cannot be followed yet", Toast.LENGTH_SHORT).show()
                                } else if (!viewModel.isLoggedIn) {
                                    Toast.makeText(context, "Sign in to follow creators", Toast.LENGTH_SHORT).show()
                                } else {
                                    val nextFollowing = !effectiveFollowing
                                    optimisticFollowing = optimisticFollowing + (post.creatorUid to nextFollowing)
                                    followRepository.setFollowing(
                                        creatorUid = post.creatorUid,
                                        creatorName = post.creatorName,
                                        creatorHandle = post.creatorHandle,
                                        following = nextFollowing
                                    ) { ok ->
                                        if (!ok) {
                                            optimisticFollowing = optimisticFollowing + (post.creatorUid to effectiveFollowing)
                                            Toast.makeText(context, "Unable to update follow", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onLike = {
                                if (!viewModel.isLoggedIn) {
                                    Toast.makeText(context, "Sign in to like reels", Toast.LENGTH_SHORT).show()
                                } else {
                                    val current = optimisticLikes[post.id] ?: (post.id in interactions.liked)
                                    val next = !current
                                    optimisticLikes = optimisticLikes + (post.id to next)
                                    followRepository.toggleInteraction(post.id, "likes", next) { ok ->
                                        if (!ok) {
                                            optimisticLikes = optimisticLikes + (post.id to current)
                                            Toast.makeText(context, "Unable to update like", Toast.LENGTH_SHORT).show()
                                        } else if (next) {
                                            reelFeedback = feedbackStore.recordPositive(post, 0.25f)
                                        }
                                    }
                                }
                            },
                            onComment = {
                                if (!viewModel.isLoggedIn) {
                                    Toast.makeText(context, "Sign in to comment", Toast.LENGTH_SHORT).show()
                                } else {
                                    commentPost = post
                                }
                            },
                            onSave = {
                                if (!viewModel.isLoggedIn) {
                                    Toast.makeText(context, "Sign in to save reels", Toast.LENGTH_SHORT).show()
                                } else {
                                    val current = optimisticSaves[post.id] ?: (post.id in interactions.saved)
                                    val next = !current
                                    optimisticSaves = optimisticSaves + (post.id to next)
                                    followRepository.toggleInteraction(post.id, "saved", next) { ok ->
                                        if (!ok) {
                                            optimisticSaves = optimisticSaves + (post.id to current)
                                            Toast.makeText(context, "Unable to update saved", Toast.LENGTH_SHORT).show()
                                        } else if (next) {
                                            reelFeedback = feedbackStore.recordPositive(post, 0.45f)
                                        }
                                    }
                                }
                            },
                            onShare = {
                                if (shareStitchUmmahPost(context, post, followRepository)) {
                                    val next = maxOf(post.shareCount, optimisticShareCounts[post.id] ?: 0L) + 1L
                                    optimisticShareCounts = optimisticShareCounts + (post.id to next)
                                }
                            },
                            onOpenProfile = {
                                viewModel.openCreatorProfile(post.creatorUid, post.creatorName, post.creatorHandle)
                            },
                            onDoubleTapLike = {
                                if (!viewModel.isLoggedIn) {
                                    Toast.makeText(context, "Sign in to like reels", Toast.LENGTH_SHORT).show()
                                } else if (!(optimisticLikes[post.id] ?: (post.id in interactions.liked))) {
                                    optimisticLikes = optimisticLikes + (post.id to true)
                                    followRepository.toggleInteraction(post.id, "likes", true) { ok ->
                                        if (!ok) {
                                            optimisticLikes = optimisticLikes + (post.id to false)
                                            Toast.makeText(context, "Unable to update like", Toast.LENGTH_SHORT).show()
                                        } else {
                                            reelFeedback = feedbackStore.recordPositive(post, 0.25f)
                                        }
                                    }
                                }
                            },
                            onLandscapeModeChange = { updateReelLandscape(it) },
                            onReport = {
                                if (!viewModel.isLoggedIn) {
                                    Toast.makeText(context, "Sign in to report", Toast.LENGTH_SHORT).show()
                                } else {
                                    followRepository.report(post.id, "Reported from reels") { ok ->
                                        Toast.makeText(context, if (ok) "Reported. Thank you." else "Unable to report", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onInterested = { reelFeedback = feedbackStore.markInterested(post) },
                            onNotInterested = { reelFeedback = feedbackStore.markNotInterested(post) },
                            onResetRecommendations = { reelFeedback = feedbackStore.reset() },
                            onRetry = {
                                playbackErrorReelId = null
                                playbackRetryToken++
                            },
                            onTogglePlay = {
                                val nextPaused = !userPaused
                                userPaused = nextPaused
                                if (nextPaused) reelPlayer.pause() else reelPlayer.play()
                            }
                        )
                }
            }
        }

        if (!landscapeViewer) {
            StitchReelsTopChrome(
                displayName = displayName,
                photoUrl = viewModel.userPhotoUrl,
                selected = topTab,
                onSelected = { topTab = it },
                onProfile = { viewModel.openMyProfile() },
                onCreate = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL) },
                onSearch = { viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 18.dp, start = 22.dp, end = 22.dp)
            )
        }
        commentPost?.let { target ->
            StitchCommentSheet(
                post = target,
                repository = followRepository,
                isLoggedIn = viewModel.isLoggedIn,
                viewModel = viewModel,
                onOpenProfile = { uid, name, handle ->
                    commentPost = null
                    viewModel.openCreatorProfile(uid, name, handle)
                },
                onCommentAdded = {
                    val next = maxOf(target.commentCount, optimisticCommentCounts[target.id] ?: 0L) + 1L
                    optimisticCommentCounts = optimisticCommentCounts + (target.id to next)
                },
                onDismiss = { commentPost = null }
            )
        }
    }
    }
}

@Composable
private fun StitchReelsTopChrome(
    displayName: String,
    photoUrl: String = "",
    selected: String,
    onSelected: (String) -> Unit,
    onProfile: () -> Unit,
    onCreate: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.42f), CircleShape)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "My profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StitchReelsTopTab("For You", selected == "For You") { onSelected("For You") }
                Spacer(modifier = Modifier.width(16.dp))
                StitchReelsTopTab("Following", selected == "Following") { onSelected("Following") }
            }
            IconButton(onClick = onCreate) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.22f))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create reel", tint = Color.White, modifier = Modifier.size(23.dp))
                }
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search Ummah", tint = Color.White, modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun StitchReelsTopTab(label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Text(label, color = Color.White.copy(alpha = if (active) 1f else 0.72f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(if (active) 58.dp else 0.dp)
                .height(3.dp)
                .background(Color(0xFF89E6C8))
        )
    }
}

@Composable
private fun StitchReelAction(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (selected) StitchEmerald.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.28f))
                .border(1.dp, if (selected) StitchGold.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (selected) StitchGold else Color.White, modifier = Modifier.size(23.dp))
        }
        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StitchLandscapePlayerButton(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) StitchEmerald.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.44f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StitchLandscapePlaybackSettings(
    selectedQuality: StitchReelQuality,
    selectedSpeed: Float,
    onQualitySelected: (StitchReelQuality) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(min = 260.dp, max = 320.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Video settings", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            StitchLandscapeSettingsGroup(
                title = "Quality",
                options = stitchReelQualityOptions,
                selected = selectedQuality,
                label = { it.label },
                onSelected = onQualitySelected
            )
            StitchLandscapeSettingsGroup(
                title = "Speed",
                options = stitchReelSpeedOptions,
                selected = selectedSpeed,
                label = { if (it == 1f) "Normal" else "${it}x" },
                onSelected = onSpeedSelected
            )
        }
    }
}

@Composable
private fun <T> StitchLandscapeSettingsGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        options.chunked(3).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                rowOptions.forEach { option ->
                    val active = option == selected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) StitchGold else Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = if (active) 0f else 0.18f), RoundedCornerShape(999.dp))
                            .clickable { onSelected(option) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label(option),
                            color = if (active) Color(0xFF08111F) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchLiveReelPage(
    post: UmmahPost,
    displayName: String,
    ownUsername: String = "",
    modifier: Modifier = Modifier,
    isFollowing: Boolean,
    isOwnPost: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    displayLikeCount: Long,
    displayCommentCount: Long,
    displaySaveCount: Long,
    displayShareCount: Long,
    isActive: Boolean,
    isPlaying: Boolean,
    playbackFailed: Boolean,
    videoAspectRatio: Float,
    landscapeMode: Boolean,
    player: ExoPlayer?,
    onFollow: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onOpenProfile: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onLandscapeModeChange: (Boolean) -> Unit,
    onReport: () -> Unit,
    onInterested: () -> Unit = {},
    onNotInterested: () -> Unit = {},
    onResetRecommendations: () -> Unit = {},
    onRetry: () -> Unit,
    onTogglePlay: () -> Unit
) {
    var captionExpanded by remember(post.id) { mutableStateOf(false) }
    var showPlaybackSettings by remember(post.id) { mutableStateOf(false) }
    var showMoreSheet by remember(post.id) { mutableStateOf(false) }
    // Auto-captions default ON (Instagram-style): embedded video text tracks are enabled and
    // any Arabic/dua text on the post is shown as a subtitle overlay.
    var captionsOn by remember(post.id) { mutableStateOf(true) }
    var selectedQuality by remember(post.id) { mutableStateOf(stitchReelQualityOptions.first()) }
    var selectedSpeed by remember(post.id) { mutableStateOf(1f) }
    var speedBoosting by remember(post.id) { mutableStateOf(false) }
    var seekFeedback by remember(post.id) { mutableStateOf<String?>(null) }
    var showTransportControls by remember(post.id) { mutableStateOf(false) }
    val context = LocalContext.current
    // Username shown on the reel: for your own reels always use your chosen @username (so it
    // matches your profile), never the email-derived handle stored on older posts.
    val shownUsername = if (isOwnPost && ownUsername.isNotBlank()) "@${ownUsername.removePrefix("@")}" else reelUsername(post)
    fun seekBy(deltaMs: Long) {
        val p = player ?: return
        val target = (p.currentPosition + deltaMs).coerceIn(0L, if (p.duration > 0) p.duration else Long.MAX_VALUE)
        p.seekTo(target)
        seekFeedback = if (deltaMs >= 0) "+10s" else "   10s"
    }
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) { delay(650); seekFeedback = null }
    }
    LaunchedEffect(landscapeMode, isPlaying, showTransportControls) {
        if (landscapeMode && isPlaying && showTransportControls) {
            delay(1600)
            showTransportControls = false
        }
    }
    LaunchedEffect(landscapeMode, isPlaying) {
        if (landscapeMode && !isPlaying) showTransportControls = true
    }
    val captionText = post.caption.ifBlank { post.arabicText.ifBlank { "Shared with the Ummah" } }
    val hasMoreCaption = captionText.length > 92
    val showReelChrome = !landscapeMode
    // A Rotate action belongs only to a genuinely wide source. Portrait phone clips stay vertical;
    // square clips fit the screen width without being cropped into a tall viewport.
    val safeAspectRatio = videoAspectRatio.takeIf { it.isFinite() && it > 0f } ?: (9f / 16f)
    val showRotateAction = post.isVideoMedia() && (safeAspectRatio > 1.15f || landscapeMode)
    val fitUprightFrame = !landscapeMode && shouldFitReelFrame(safeAspectRatio)
    val wideVideoChrome = showReelChrome && showRotateAction
    val visibleCaption = if (!captionExpanded && hasMoreCaption) {
        captionText.take(92).trimEnd() + "... more"
    } else {
        captionText
    }
    // No poster thumbnail anymore (user asked to remove it). A bare black page looks broken
    // while a reel buffers or on the non-settled pages (which have no decoder surface), so we
    // back every page with a soft emerald   black wash. The live (texture) surface, when present,
    // covers it edge-to-edge     but the wash means the frame never flashes pure black.
    val reelBackdrop = Brush.verticalGradient(
        if (wideVideoChrome || landscapeMode) {
            listOf(Color.Black, Color.Black)
        } else {
            listOf(StitchEmerald.copy(alpha = 0.55f), Color.Black, Color.Black)
        }
    )
    LaunchedEffect(isActive, player, selectedQuality, selectedSpeed, captionsOn) {
        if (isActive && player != null) {
            player.applyStitchReelQuality(selectedQuality)
            player.setPlaybackSpeed(selectedSpeed)
            player.applyStitchCaptions(captionsOn)
        }
    }
    BoxWithConstraints(modifier = modifier.background(reelBackdrop)) {
        val wideFrame = if (wideVideoChrome) {
            wideReelFrame(maxWidth.value, maxHeight.value, safeAspectRatio)
        } else null
        if (post.mediaUrl.isNotBlank()) {
            // The live (texture) surface is attached only to the page the pager has settled
            // on, so one decoder serves the whole feed.
            if (isActive && player != null) {
                // In landscape mode the whole Activity is really rotated (Facebook-style), so the
                // surface just fills the now-wide screen     RESIZE_MODE_FIT shows the full 16:9 frame.
                val surfaceModifier = if (wideFrame != null) {
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = wideFrame.topDp.dp)
                        .width(wideFrame.widthDp.dp)
                        .height(wideFrame.heightDp.dp)
                        .background(Color.Black)
                } else if (fitUprightFrame) {
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .aspectRatio(safeAspectRatio)
                        .background(Color.Black)
                } else {
                    Modifier.fillMaxSize()
                }
                StitchReelSurface(
                    player = player,
                    landscapeMode = landscapeMode,
                    fitFrame = landscapeMode || fitUprightFrame,
                    modifier = surfaceModifier
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(StitchEmerald, Color.Black))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mosque, contentDescription = null, tint = StitchGold.copy(alpha = 0.2f), modifier = Modifier.size(220.dp))
            }
        }

        // Tap anywhere on the reel to play/pause.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(post.id, selectedSpeed) {
                    detectTapGestures(
                        onTap = {
                            if (landscapeMode) {
                                showTransportControls = if (isPlaying) !showTransportControls else true
                            } else {
                                onTogglePlay()
                            }
                        },
                        onDoubleTap = { offset ->
                            // YouTube-style zones: double-tap left = back 10s, right = forward 10s,
                            // middle = like.
                            val third = size.width / 3f
                            when {
                                offset.x < third -> seekBy(-10_000L)
                                offset.x > third * 2 -> seekBy(10_000L)
                                else -> onDoubleTapLike()
                            }
                        },
                        onPress = {
                            // Instagram-style: press and hold to fast-forward at 2x; release to restore.
                            val pressScope = this
                            val releasedQuickly = withTimeoutOrNull(180L) { pressScope.tryAwaitRelease() }
                            if (releasedQuickly == null) {
                                speedBoosting = true
                                player?.setPlaybackSpeed(2f)
                                pressScope.tryAwaitRelease()
                                player?.setPlaybackSpeed(selectedSpeed)
                                speedBoosting = false
                            }
                        }
                    )
                }
        )

        // "2x" indicator while holding to fast-forward.
        if (speedBoosting) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("2x", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (showReelChrome) {
                            listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        } else {
                            listOf(Color.Black.copy(alpha = 0.04f), Color.Transparent, Color.Black.copy(alpha = 0.1f))
                        }
                    )
                )
        )

        if (showReelChrome && !wideVideoChrome) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 118.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Every counter is visible from zero so a newly published reel has the same action
                // layout as an established reel.
                StitchReelAction(Icons.Default.Favorite, compactCount(displayLikeCount), selected = isLiked, onClick = onLike)
                StitchReelAction(Icons.Default.ChatBubble, compactCount(displayCommentCount), onClick = onComment)
                StitchReelAction(Icons.Default.Bookmark, compactCount(displaySaveCount), selected = isSaved, onClick = onSave)
                StitchReelAction(Icons.Default.Share, compactCount(displayShareCount), onClick = onShare)
                if (showRotateAction) {
                    StitchReelAction(
                        Icons.Default.ScreenRotation,
                        "Rotate",
                        selected = false,
                        onClick = { onLandscapeModeChange(true) }
                    )
                }
                StitchReelAction(Icons.Default.MoreVert, "More", onClick = { showMoreSheet = true })
            }
        } else if (wideVideoChrome) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (wideFrame?.actionsTopDp ?: 0f).dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StitchLandscapeMiniAction(Icons.Default.Favorite, compactCount(displayLikeCount), isLiked, onLike)
                StitchLandscapeMiniAction(Icons.Default.ChatBubble, compactCount(displayCommentCount), false, onComment)
                StitchLandscapeMiniAction(Icons.Default.Bookmark, compactCount(displaySaveCount), isSaved, onSave)
                StitchLandscapeMiniAction(Icons.Default.Share, compactCount(displayShareCount), false, onShare)
                StitchWideIconAction(Icons.Default.ScreenRotation, "Rotate") { onLandscapeModeChange(true) }
                StitchWideIconAction(Icons.Default.MoreVert, "More") { showMoreSheet = true }
            }
        } else if (showRotateAction && (!isPlaying || showTransportControls || showPlaybackSettings)) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 14.dp, end = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StitchLandscapePlayerButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    selected = showPlaybackSettings,
                    onClick = { showPlaybackSettings = !showPlaybackSettings }
                )
                StitchLandscapePlayerButton(
                    icon = Icons.Default.ScreenRotation,
                    label = "Close",
                    selected = true,
                    onClick = {
                        showPlaybackSettings = false
                        onLandscapeModeChange(false)
                    }
                )
            }
        }

        // Landscape (16:9 wide): Facebook-style CENTRE transport controls     back 10s, play/pause,
        // forward 10s     placed in the middle of the player so they're easy to reach.
        if (landscapeMode && (!isPlaying || showTransportControls)) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StitchLandscapeTransportButton(Icons.Default.Replay10, "Back 10 seconds", 56.dp) { seekBy(-10_000L) }
                StitchLandscapeTransportButton(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Play or pause",
                    78.dp,
                    onClick = {
                        showTransportControls = true
                        onTogglePlay()
                    }
                )
                StitchLandscapeTransportButton(Icons.Default.Forward10, "Forward 10 seconds", 56.dp) { seekBy(10_000L) }
            }
        }

        // Landscape: like / comment / save / share sit DOWN on the player line (a full-width row
        // just above the scrubber), Facebook-style, instead of a floating corner pill.
        if (landscapeMode && (!isPlaying || showTransportControls)) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StitchLandscapeMiniAction(Icons.Default.Favorite, "", isLiked, onLike)
                StitchLandscapeMiniAction(Icons.Default.ChatBubble, "", false, onComment)
                StitchLandscapeMiniAction(Icons.Default.Bookmark, "Save", isSaved, onSave)
                StitchLandscapeMiniAction(Icons.Default.Share, "", false, onShare)
            }
        }

        if (landscapeMode && showPlaybackSettings && player != null) {
            StitchLandscapePlaybackSettings(
                selectedQuality = selectedQuality,
                selectedSpeed = selectedSpeed,
                onQualitySelected = { quality ->
                    selectedQuality = quality
                    player.applyStitchReelQuality(quality)
                },
                onSpeedSelected = { speed ->
                    selectedSpeed = speed
                    player.setPlaybackSpeed(speed)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 76.dp, end = 18.dp)
            )
        }

        // Brief    10s / +10s feedback after a side double-tap.
        seekFeedback?.let { label ->
            val onRight = label.startsWith("+")
            Row(
                modifier = Modifier
                    .align(if (onRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (onRight) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Portrait feed only: a single centre play button when paused. In the wide (landscape)
        // view the centre transport row below owns play/pause, so this is hidden to avoid a
        // duplicate button.
        if (!isPlaying && !landscapeMode && !playbackFailed) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play reel",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        if (isActive && playbackFailed) {
            Column(
                modifier = Modifier.align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Couldn't play this reel", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = StitchEmerald)) {
                    Text("Retry")
                }
            }
        }

        if (showReelChrome) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 16.dp,
                        end = if (wideVideoChrome) 16.dp else 84.dp,
                        bottom = 126.dp
                    )
                    .fillMaxWidth()
            ) {
            // Username only (Instagram-style)     tap avatar or @handle to open the profile,
            // with a compact inline Follow chip beside it.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // fill = false so this row only takes the width it needs (avatar + name); the
                    // Follow pill then sits right beside the @username, Instagram/Facebook-style,
                    // instead of being pushed to the far edge of the screen.
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenProfile
                    ).weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(StitchGold)
                            .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (post.creatorPhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = post.creatorPhotoUrl,
                                contentDescription = "${post.creatorName} photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(post.creatorName.take(1).uppercase().ifBlank { "N" }, color = StitchEmerald, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        shownUsername,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 170.dp)
                    )
                }
                if (!isOwnPost) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Small inline Follow pill right beside the username, like Instagram/Facebook.
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(min = 76.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isFollowing) Color.Transparent else StitchEmerald.copy(alpha = 0.96f))
                            .border(1.dp, Color.White.copy(alpha = if (isFollowing) 0.72f else 0.45f), RoundedCornerShape(999.dp))
                            .clickable(onClick = onFollow)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isFollowing) "Following" else "Follow",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                visibleCaption,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = if (captionExpanded) 5 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = hasMoreCaption) {
                    captionExpanded = !captionExpanded
                }
            )
            // Keep the views row present from the first play instead of making it appear later.
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                "▶  ${compactCount(post.viewCount)} views",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            }
        }

        // Auto-caption overlay: show the reel's Arabic/dua text as subtitles while playing.
        if (captionsOn && showReelChrome && post.arabicText.isNotBlank()) {
            Box(
                modifier = (if (wideFrame != null) {
                    Modifier.align(Alignment.TopCenter)
                        .offset(y = (wideFrame.topDp + wideFrame.heightDp - 72f).dp)
                } else {
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 218.dp)
                })
                    .padding(start = 30.dp, end = 30.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    post.arabicText,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Instagram-style scrubber: drag (or tap) to seek/forward through the reel. Only the
        // settled page owns the shared player, so the bar is drawn for the active reel only.
        if (isActive && player != null && (!landscapeMode || !isPlaying || showTransportControls)) {
            ReelSeekBar(
                player = player,
                isActive = isActive,
                modifier = if (wideFrame != null) {
                    Modifier.align(Alignment.TopCenter)
                        .offset(y = (wideFrame.topDp + wideFrame.heightDp - 4f).dp)
                        .padding(horizontal = 12.dp)
                } else {
                    Modifier.align(Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 12.dp, bottom = if (landscapeMode) 14.dp else 96.dp)
                }
            )
        }

        // Three-dot menu: quality, playback speed, captions, share and report     like Instagram.
        if (showMoreSheet) {
            StitchReelMoreSheet(
                selectedQuality = selectedQuality,
                selectedSpeed = selectedSpeed,
                captionsOn = captionsOn,
                isSaved = isSaved,
                onQualitySelected = { selectedQuality = it; player?.applyStitchReelQuality(it) },
                onSpeedSelected = { selectedSpeed = it; player?.setPlaybackSpeed(it) },
                onCaptionsToggle = { captionsOn = it; player?.applyStitchCaptions(it) },
                onInterested = {
                    onInterested()
                    Toast.makeText(context, "We'll show more reminders like this", Toast.LENGTH_SHORT).show()
                },
                onNotInterested = {
                    onNotInterested()
                    Toast.makeText(context, "We'll show fewer like this", Toast.LENGTH_SHORT).show()
                },
                onResetRecommendations = {
                    onResetRecommendations()
                    Toast.makeText(context, "Recommendations reset. Pull down to refresh.", Toast.LENGTH_SHORT).show()
                },
                onFindSupport = {
                    Toast.makeText(context, "Need help? Open Health & Wellness in Noor Pro for support resources.", Toast.LENGTH_LONG).show()
                },
                onSave = onSave,
                onCopyLink = { copyStitchReelLink(context, post.id) },
                onReport = onReport,
                onShare = onShare,
                onDismiss = { showMoreSheet = false }
            )
        }
    }
}

@Composable
private fun StitchReelMoreSheet(
    selectedQuality: StitchReelQuality,
    selectedSpeed: Float,
    captionsOn: Boolean,
    isSaved: Boolean,
    onQualitySelected: (StitchReelQuality) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onCaptionsToggle: (Boolean) -> Unit,
    onInterested: () -> Unit,
    onNotInterested: () -> Unit,
    onResetRecommendations: () -> Unit,
    onFindSupport: () -> Unit,
    onSave: () -> Unit,
    onCopyLink: () -> Unit,
    onReport: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.82f
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11161D)),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = sheetMaxHeight)
                        .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(44.dp).height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    StitchLandscapeSettingsGroup(
                        title = "Quality",
                        options = stitchReelQualityOptions,
                        selected = selectedQuality,
                        label = { it.label },
                        onSelected = onQualitySelected
                    )
                    StitchLandscapeSettingsGroup(
                        title = "Playback speed",
                        options = stitchReelSpeedOptions,
                        selected = selectedSpeed,
                        label = { if (it == 1f) "Normal" else "${it}x" },
                        onSelected = onSpeedSelected
                    )
                    StitchReelSheetToggleRow(Icons.Default.ClosedCaption, "Auto captions", captionsOn) { onCaptionsToggle(!captionsOn) }
                    StitchReelSheetActionRow(Icons.Default.Recommend, "Interested") { onInterested(); onDismiss() }
                    StitchReelSheetActionRow(Icons.Default.NotInterested, "Not interested") { onNotInterested(); onDismiss() }
                    StitchReelSheetActionRow(Icons.Default.Refresh, "Reset recommendations") { onResetRecommendations(); onDismiss() }
                    StitchReelSheetActionRow(Icons.Default.SupportAgent, "Find support") { onFindSupport(); onDismiss() }
                    StitchReelSheetActionRow(Icons.Default.Bookmark, if (isSaved) "Remove from saved" else "Save reel") { onSave(); onDismiss() }
                    StitchReelSheetActionRow(Icons.Default.ContentCopy, "Copy link") { onCopyLink(); onDismiss() }
                    StitchReelSheetActionRow(Icons.Default.Share, "Share") { onShare(); onDismiss() }
                    StitchReelSheetActionRow(Icons.Default.Flag, "Report") { onReport(); onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun StitchReelSheetToggleRow(icon: ImageVector, label: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun StitchReelSheetActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Circular glassy transport button used for the Facebook-style landscape play/seek controls. */
@Composable
private fun StitchLandscapeTransportButton(
    icon: ImageVector,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun StitchLandscapeMiniAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = if (active) Color(0xFFE8505B) else Color.White, modifier = Modifier.size(20.dp))
        if (label.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StitchWideIconAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.34f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/**
 * Thin draggable progress bar pinned to the bottom of a reel. Polls the shared player's
 * position while the reel is active; dragging or tapping seeks the player. The track thickens
 * slightly while the user is scrubbing, like Instagram/TikTok.
 */
@Composable
private fun ReelSeekBar(
    player: ExoPlayer,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(isActive) {
        while (isActive) {
            durationMs = player.duration.coerceAtLeast(0L)
            positionMs = player.currentPosition.coerceAtLeast(0L)
            kotlinx.coroutines.delay(200)
        }
    }

    val fraction = dragFraction
        ?: if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val scrubbing = dragFraction != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0L && size.width > 0) {
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        player.seekTo((f * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (size.width > 0) dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        if (size.width > 0) dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        val f = dragFraction
                        if (f != null && durationMs > 0L) player.seekTo((f * durationMs).toLong())
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (scrubbing) 5.dp else 3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.28f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
            )
        }
    }
}

/** Display the creator's @username only (Instagram-style), normalizing the handle. */
private fun reelUsername(post: UmmahPost): String {
    val raw = post.creatorHandle.ifBlank { post.creatorName }.ifBlank { "ummah" }
    return if (raw.startsWith("@")) raw else "@$raw"
}

// A single shared PlayerView, inflated as a TextureView, attached to the reels-feed player.
// TextureView avoids the SurfaceView z-order black flashes a pager produces while scrolling.
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun StitchReelSurface(
    player: ExoPlayer,
    landscapeMode: Boolean,
    fitFrame: Boolean,
    modifier: Modifier = Modifier
) {
    // Vertical feed: every reel FILLS the screen edge-to-edge (ZOOM), like Instagram / TikTok     so
    // no reel ever shows as a small letterboxed box (portrait clips with rotation metadata used to
    // be mis-detected as landscape and shrunk). Rotated wide view: FIT, so a genuine 16:9 video
    // shows its full frame (Facebook-style) without cropping.
    val resize = if (landscapeMode || fitFrame)
        AspectRatioFrameLayout.RESIZE_MODE_FIT
    else
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            (android.view.LayoutInflater.from(viewContext)
                .inflate(com.noorpro.app.R.layout.ummah_reel_player, null) as PlayerView).apply {
                useController = false
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                resizeMode = resize
                setKeepContentOnPlayerReset(false)
            }
        },
        update = {
            it.player = player
            it.resizeMode = resize
        },
        onRelease = { it.player = null }
    )
}

@Composable
private fun StitchReelsEmptyState(
    title: String,
    message: String?,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(86.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = StitchGold, modifier = Modifier.size(42.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message.orEmpty(), color = Color.White.copy(alpha = 0.78f), fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onCreate,
            colors = ButtonDefaults.buttonColors(containerColor = StitchGold, contentColor = StitchEmerald),
            shape = RoundedCornerShape(999.dp)
        ) {
            Icon(Icons.Default.Videocam, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Reel", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StitchUmmahHubScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    var feedRefreshKey by remember { mutableStateOf(0) }
    val feedState = rememberStitchUmmahFeed(feedRefreshKey)
    val following = rememberStitchFollowing(viewModel.isLoggedIn)
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    val followRepository = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { followRepository.currentUserUid().orEmpty() }
    val displayName = viewModel.userDisplayName.ifBlank { if (viewModel.isLoggedIn) "Noor" else "Guest" }
    // Content category chips (the Ummah "social hybrid" design): All / Quran / Hadith / Community /
    // Reflections. "All" shows the ranked feed; the others filter by category/caption text.
    val feedCategories = listOf("All", "Quran", "Hadith", "Community", "Reflections")
    var selectedTab by remember { mutableStateOf("All") }
    var commentPost by remember { mutableStateOf<UmmahPost?>(null) }
    // Instant (optimistic) like/save so the feed feels smooth and lag-free.
    var optimisticLikes by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var optimisticSaves by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val feedPosts = feedState.posts.filterNot { it.isReelLike() }
    val feedReels = feedState.posts.filter { it.isReelLike() }
    // Main feed is algorithm-ranked like Facebook/Instagram (freshness + engagement + creators you
    // follow/like + your interest categories), then narrowed to the chosen category chip.
    val rankedPosts = rankUmmahPosts(feedPosts, following, interactions.liked, interactions.saved)
    val posts = if (selectedTab == "All") rankedPosts else {
        val needle = selectedTab.lowercase().removeSuffix("s")
        rankedPosts.filter { p ->
            listOf(p.category, p.caption, p.arabicText).any { it.lowercase().contains(needle) }
        }
    }
    val reelStripPosts = rankUmmahPosts(feedReels, following, interactions.liked, interactions.saved).take(10)
    // Instagram-style pull-to-refresh: swipe down at the top of the feed to reload, then
    // jump back to the top so the freshest posts show.
    var refreshing by remember { mutableStateOf(false) }
    val hubListState = rememberLazyListState()
    LaunchedEffect(feedState.loading) {
        if (!feedState.loading && refreshing) {
            refreshing = false
            hubListState.scrollToItem(0)
        }
    }

    StitchScreen {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            feedRefreshKey++
        },
        modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            state = hubListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 30.dp, end = 12.dp, bottom = 184.dp),
            // Tight, connected stacking like the Facebook/YouTube feed in the design (cards nearly
            // touch, separated only by a thin gap) instead of large spaces between cards.
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StitchUmmahTopBar(
                    displayName = displayName,
                    photoUrl = viewModel.userPhotoUrl,
                    onProfile = { viewModel.openMyProfile() },
                    onSearch = { viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) },
                    onCreate = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST) }
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(feedCategories) { cat ->
                        StitchSavedFilterChip(cat, cat == selectedTab) { selectedTab = cat }
                    }
                }
            }
            item {
                StitchComposerCard(
                    avatar = displayName.take(1).uppercase(),
                    onOpen = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST) }
                )
            }
            item {
                StitchYourPostStrip(
                    reels = reelStripPosts,
                    onCreate = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL) },
                    onWatchAll = { viewModel.navigateTo(DeenScreen.REELS) },
                    onOpenReel = { post -> viewModel.openReel(post.id) }
                )
            }
            when {
                feedState.loading -> item {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = stitchPrimary())
                    }
                }
                feedState.error != null -> item {
                    StitchUmmahEmptyCard(
                        title = "Unable to load Ummah",
                        message = feedState.error.orEmpty(),
                        action = "Try Upload",
                        onAction = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST) }
                    )
                }
                posts.isEmpty() -> item {
                    StitchUmmahEmptyCard(
                        title = if (selectedTab == "All") "The posts are quiet" else "Nothing in $selectedTab yet",
                        message = if (selectedTab == "All") "Be the first to share a reflection, image, or reel." else "No posts match \"$selectedTab\" right now. Try another category.",
                        action = if (selectedTab == "All") "Create a post" else "Show all",
                        onAction = { if (selectedTab == "All") viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST) else selectedTab = "All" }
                    )
                }
                else -> itemsIndexed(posts, key = { _, it -> it.id }) { index, post ->
                    // Sponsored cards sit between posts, never over media or interaction buttons.
                    if (shouldShowUmmahFeedAd(index)) {
                        StitchSponsoredPostCard(modifier = Modifier.padding(bottom = 4.dp))
                    }
                    val isLiked = optimisticLikes[post.id] ?: (post.id in interactions.liked)
                    val isSaved = optimisticSaves[post.id] ?: (post.id in interactions.saved)
                    if (post.isVideoMedia() && !post.isReelLike()) {
                        // Long-form video posts render as a YouTube-style card (big thumbnail,
                        // channel row, like/dislike + Share) to match the Ummah design.
                        StitchYouTubePostCard(
                            post = post,
                            isLiked = isLiked,
                            onLike = {
                                if (!viewModel.isLoggedIn) Toast.makeText(context, "Sign in to like posts", Toast.LENGTH_SHORT).show()
                                else {
                                    val next = !isLiked
                                    optimisticLikes = optimisticLikes + (post.id to next)
                                    followRepository.toggleInteraction(post.id, "likes", next) { ok ->
                                        if (!ok) { optimisticLikes = optimisticLikes + (post.id to isLiked); Toast.makeText(context, "Unable to update like", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            },
                            onShare = { shareStitchUmmahPost(context, post, followRepository) },
                            onOpen = { viewModel.openReel(post.id) },
                            onOpenProfile = { viewModel.openCreatorProfile(post.creatorUid, post.creatorName, post.creatorHandle) }
                        )
                    } else {
                    StitchRealPostCard(
                        post = post,
                        isFollowing = post.creatorUid in following,
                        isOwnPost = post.creatorUid.isBlank() || post.creatorUid == currentUid,
                        isLiked = isLiked,
                        isSaved = isSaved,
                        onFollow = {
                            if (post.creatorUid.isBlank() || post.creatorUid == currentUid) {
                                Toast.makeText(context, "This is your post", Toast.LENGTH_SHORT).show()
                            } else if (!viewModel.isLoggedIn) {
                                Toast.makeText(context, "Sign in to follow creators", Toast.LENGTH_SHORT).show()
                            } else {
                                followRepository.setFollowing(
                                    creatorUid = post.creatorUid,
                                    creatorName = post.creatorName,
                                    creatorHandle = post.creatorHandle,
                                    following = post.creatorUid !in following
                                ) { ok ->
                                    if (!ok) Toast.makeText(context, "Unable to update follow", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onLike = {
                            if (!viewModel.isLoggedIn) {
                                Toast.makeText(context, "Sign in to like posts", Toast.LENGTH_SHORT).show()
                            } else {
                                val next = !isLiked
                                optimisticLikes = optimisticLikes + (post.id to next)
                                followRepository.toggleInteraction(post.id, "likes", next) { ok ->
                                    if (!ok) { optimisticLikes = optimisticLikes + (post.id to isLiked); Toast.makeText(context, "Unable to update like", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        },
                        onComment = {
                            if (!viewModel.isLoggedIn) Toast.makeText(context, "Sign in to comment", Toast.LENGTH_SHORT).show() else commentPost = post
                        },
                        onSave = {
                            if (!viewModel.isLoggedIn) {
                                Toast.makeText(context, "Sign in to save posts", Toast.LENGTH_SHORT).show()
                            } else {
                                val next = !isSaved
                                optimisticSaves = optimisticSaves + (post.id to next)
                                followRepository.toggleInteraction(post.id, "saved", next) { ok ->
                                    if (!ok) { optimisticSaves = optimisticSaves + (post.id to isSaved); Toast.makeText(context, "Unable to update saved", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        },
                        onShare = { shareStitchUmmahPost(context, post, followRepository) },
                        onOpenProfile = { viewModel.openCreatorProfile(post.creatorUid, post.creatorName, post.creatorHandle) },
                        onOpenReel = { viewModel.openReel(post.id) }
                    )
                    }
                }
            }
        }
        }
    }
    commentPost?.let { target ->
        StitchCommentSheet(
            post = target,
            repository = followRepository,
            isLoggedIn = viewModel.isLoggedIn,
            viewModel = viewModel,
            onOpenProfile = { uid, name, _ ->
                commentPost = null
                viewModel.openCreatorProfile(uid, name, "")
            },
            onDismiss = { commentPost = null }
        )
    }
}

@Composable
private fun StitchUmmahTopBar(
    displayName: String,
    photoUrl: String = "",
    onProfile: () -> Unit,
    onSearch: () -> Unit,
    onCreate: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(StitchGold)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "My profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(displayName.take(1).uppercase(), color = StitchEmerald, fontWeight = FontWeight.Black)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("UMMAH", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("Community", color = stitchText(), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
        }
        IconButton(
            onClick = onSearch,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(stitchSurface())
                .border(1.dp, StitchLine, CircleShape)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search Ummah", tint = stitchPrimary())
        }
        Spacer(modifier = Modifier.width(10.dp))
        IconButton(
            onClick = onCreate,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(stitchSurface())
                .border(1.dp, StitchLine, CircleShape)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Create post", tint = stitchPrimary())
        }
    }
}

@Composable
fun StitchUmmahSearchScreen(viewModel: DeenViewModel) {
    val feedState = rememberStitchUmmahFeed()
    var query by remember { mutableStateOf("") }
    val cleanQuery = query.trim()
    val profiles = rememberStitchProfiles()
    val q = cleanQuery.lowercase()
    // Fast in-memory ranked results: split query into words, require all to match, rank by where.
    val results = remember(feedState.posts, cleanQuery) {
        if (cleanQuery.isBlank()) emptyList() else {
            val terms = q.split(Regex("\\s+")).filter { it.isNotBlank() }
            val weights = listOf(6, 5, 4, 3, 2)
            feedState.posts.mapNotNull { post ->
                val fields = listOf(post.creatorHandle, post.creatorName, post.category, post.caption, post.arabicText).map { it.lowercase() }
                var score = 0
                var allMatched = true
                for (term in terms) {
                    var ts = 0
                    fields.forEachIndexed { i, f -> if (f.contains(term)) ts += weights[i] }
                    if (ts == 0) { allMatched = false; break }
                    score += ts
                }
                if (allMatched && score > 0) post to score else null
            }.sortedByDescending { it.second }.map { it.first }
        }
    }
    val videoResults = remember(results) { results.filter { it.isReelLike() || it.isVideoMedia() } }
    val postResults = remember(results) { results.filter { !it.isReelLike() && !it.isVideoMedia() } }
    // People search: live profiles + post creators, matched by name / @handle. Uses live profile
    // photos, so results auto-update the moment someone changes their picture.
    val users = remember(feedState.posts, profiles, cleanQuery) {
        if (cleanQuery.isBlank()) emptyList() else {
            val needle = q.removePrefix("@")
            val map = LinkedHashMap<String, StitchSearchUser>()
            profiles.values.forEach { p -> if (p.uid.isNotBlank()) map[p.uid] = StitchSearchUser(p.uid, p.name, p.handle, p.photoUrl) }
            feedState.posts.forEach { post ->
                if (post.creatorUid.isNotBlank() && !map.containsKey(post.creatorUid)) {
                    map[post.creatorUid] = StitchSearchUser(post.creatorUid, post.creatorName, post.creatorHandle, post.creatorPhotoUrl)
                }
            }
            map.values.filter { u -> u.name.lowercase().contains(needle) || u.handle.lowercase().removePrefix("@").contains(needle) }.take(12)
        }
    }
    val hasResults = users.isNotEmpty() || videoResults.isNotEmpty() || postResults.isNotEmpty()

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Search Ummah", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = stitchPrimary()) },
                    placeholder = { Text("Search posts, reels, creators...") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            when {
                cleanQuery.isBlank() -> item {
                    StitchUmmahEmptyCard(
                        title = "Find beneficial content",
                        message = "Search people, reels, videos, captions, and topics.",
                        action = "Back",
                        onAction = { viewModel.goBack() }
                    )
                }
                feedState.loading && !hasResults -> item {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = stitchPrimary())
                    }
                }
                !hasResults -> item {
                    StitchUmmahEmptyCard(
                        title = "No results",
                        message = "Try another word, creator name, or topic.",
                        action = "Clear",
                        onAction = { query = "" }
                    )
                }
                else -> {
                    if (users.isNotEmpty()) {
                        item { StitchSearchSectionLabel("People") }
                        items(users, key = { "u_" + it.uid }) { u ->
                            StitchSearchUserRow(u) { viewModel.openCreatorProfile(u.uid, u.name, u.handle) }
                        }
                    }
                    if (videoResults.isNotEmpty()) {
                        item { StitchSearchSectionLabel("Videos & Reels") }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(videoResults, key = { it.id }) { post ->
                                    StitchMiniLiveReelCard(post = post, onClick = {
                                        if (post.opensInVideoViewer()) viewModel.openReel(post.id) else viewModel.navigateTo(DeenScreen.UMMAH)
                                    })
                                }
                            }
                        }
                    }
                    if (postResults.isNotEmpty()) {
                        item { StitchSearchSectionLabel("Posts") }
                        items(postResults, key = { it.id }) { post ->
                            StitchSearchResultCard(
                                post = post,
                                onOpen = {
                                    if (post.opensInVideoViewer()) viewModel.openReel(post.id) else viewModel.navigateTo(DeenScreen.UMMAH)
                                },
                                onOpenProfile = { viewModel.openCreatorProfile(post.creatorUid, post.creatorName, post.creatorHandle) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StitchSearchUser(val uid: String, val name: String, val handle: String, val photoUrl: String)

@Composable
private fun StitchSearchSectionLabel(text: String) {
    Text(text, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
}

/** A "People" search result     live avatar + name + @handle, tap to open the creator's profile. */
@Composable
private fun StitchSearchUserRow(user: StitchSearchUser, onClick: () -> Unit) {
    StitchCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(StitchEmerald),
                contentAlignment = Alignment.Center
            ) {
                if (user.photoUrl.isNotBlank()) {
                    AsyncImage(model = user.photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                    Text(user.name.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name.ifBlank { "Community member" }, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val handle = user.handle.ifBlank { "ummah" }
                Text(if (handle.startsWith("@")) handle else "@$handle", color = stitchMutedText(), fontSize = 13.sp, maxLines = 1)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
        }
    }
}

@Composable
private fun StitchSearchResultCard(
    post: UmmahPost,
    onOpen: () -> Unit,
    onOpenProfile: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(18.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(stitchSoftSurface()),
                contentAlignment = Alignment.Center
            ) {
                if (post.thumbnailUrl.isNotBlank()) {
                    AsyncImage(model = post.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (post.mediaUrl.isNotBlank() && !post.isReelLike()) {
                    AsyncImage(model = post.mediaUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(if (post.isReelLike()) Icons.Default.SmartDisplay else Icons.Default.Article, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.caption.ifBlank { post.arabicText.ifBlank { post.category } },
                    color = stitchText(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${if (post.isReelLike()) "Reel" else "Post"} - ${post.creatorName.ifBlank { "Community member" }}",
                    color = stitchMutedText(),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.clickable(onClick = onOpenProfile)
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
        }
    }
}

@Composable
fun StitchUmmahSavedScreen(viewModel: DeenViewModel) {
    val feedState = rememberStitchUmmahFeed()
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    val savedLibrary = rememberStitchSavedLibrary(viewModel.isLoggedIn)
    val repository = remember { UmmahRepository() }
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("all") }
    var showCreateCollection by remember { mutableStateOf(false) }
    var collectionTitle by remember { mutableStateOf("") }
    val savedPosts = remember(feedState.posts, interactions.saved) {
        feedState.posts.filter { it.id in interactions.saved }
    }
    val visiblePosts = remember(savedPosts, selectedFilter, savedLibrary.assignments) {
        when (selectedFilter) {
            "reels" -> savedPosts.filter { it.isReelLike() }
            "posts" -> savedPosts.filter { !it.isReelLike() }
            "all" -> savedPosts
            else -> savedPosts.filter { savedLibrary.assignments[it.id] == selectedFilter }
        }
    }

    if (showCreateCollection) {
        AlertDialog(
            onDismissRequest = { showCreateCollection = false },
            title = { Text("New collection", color = stitchText(), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = collectionTitle,
                    onValueChange = { collectionTitle = it.take(36) },
                    placeholder = { Text("Example: Quran reminders") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.createSavedCollection(collectionTitle) { ok ->
                            if (ok) {
                                collectionTitle = ""
                                showCreateCollection = false
                            } else {
                                Toast.makeText(context, "Unable to create collection", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollection = false }) {
                    Text("Cancel", color = stitchMutedText())
                }
            },
            containerColor = stitchSurface()
        )
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Saved", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.navigateTo(DeenScreen.UMMAH_ACTIVITY) }) {
                        Text("Activity", color = stitchPrimary(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(54.dp).clip(CircleShape).background(stitchSoftSurface()),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Saved reels and posts", color = stitchText(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("${savedPosts.size} saved items. Create private collections.", color = stitchMutedText(), fontSize = 13.sp)
                            }
                            IconButton(onClick = { showCreateCollection = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Create collection", tint = stitchPrimary())
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StitchSavedStatCard("All", savedPosts.size.toString(), Modifier.weight(1f))
                            StitchSavedStatCard("Reels", savedPosts.count { it.isReelLike() }.toString(), Modifier.weight(1f))
                            StitchSavedStatCard("Posts", savedPosts.count { !it.isReelLike() }.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        StitchSavedFilterChip("All", selectedFilter == "all") { selectedFilter = "all" }
                    }
                    item {
                        StitchSavedFilterChip("Reels", selectedFilter == "reels") { selectedFilter = "reels" }
                    }
                    item {
                        StitchSavedFilterChip("Posts", selectedFilter == "posts") { selectedFilter = "posts" }
                    }
                    items(savedLibrary.collections, key = { it.id }) { collection ->
                        StitchSavedFilterChip(collection.title, selectedFilter == collection.id) { selectedFilter = collection.id }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, stitchPrimary(), RoundedCornerShape(999.dp))
                                .clickable { showCreateCollection = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("New", color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            when {
                !viewModel.isLoggedIn -> item {
                    StitchUmmahEmptyCard(
                        title = "Sign in to save",
                        message = "Saved reels and posts are stored on your account.",
                        action = "Sign in",
                        onAction = { viewModel.navigateTo(DeenScreen.LOGIN) }
                    )
                }
                feedState.loading -> item {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = stitchPrimary())
                    }
                }
                savedPosts.isEmpty() -> item {
                    StitchUmmahEmptyCard(
                        title = "No saved content yet",
                        message = "Tap the bookmark on a reel or post to save it here.",
                        action = "Watch Reels",
                        onAction = { viewModel.navigateTo(DeenScreen.REELS) }
                    )
                }
                visiblePosts.isEmpty() -> item {
                    StitchUmmahEmptyCard(
                        title = "This collection is empty",
                        message = "Open All, then add saved reels or posts into this collection.",
                        action = "Show all",
                        onAction = { selectedFilter = "all" }
                    )
                }
                else -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        visiblePosts.forEach { post ->
                            StitchSavedContentCard(
                                post = post,
                                collections = savedLibrary.collections,
                                assignedCollectionId = savedLibrary.assignments[post.id],
                                onOpen = { if (post.opensInVideoViewer()) viewModel.openReel(post.id) else viewModel.navigateTo(DeenScreen.UMMAH) },
                                onOpenProfile = { viewModel.openCreatorProfile(post.creatorUid, post.creatorName, post.creatorHandle) },
                                onAssign = { collection ->
                                    repository.assignSavedToCollection(post.id, collection.id) { ok ->
                                        Toast.makeText(
                                            context,
                                            if (ok) "Saved to ${collection.title}" else "Unable to save to collection",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StitchUmmahActivityScreen(viewModel: DeenViewModel) {
    val feedState = rememberStitchUmmahFeed()
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    var selectedTab by remember { mutableStateOf("likes") }
    val likedPosts = remember(feedState.posts, interactions.liked) {
        feedState.posts.filter { it.id in interactions.liked }
    }
    val savedPosts = remember(feedState.posts, interactions.saved) {
        feedState.posts.filter { it.id in interactions.saved }
    }
    val activePosts = if (selectedTab == "likes") likedPosts else savedPosts

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Your Activity", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Your activity", color = stitchText(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Review what you liked and saved in Ummah.", color = stitchMutedText(), fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StitchSavedStatCard("Liked", likedPosts.size.toString(), Modifier.weight(1f))
                            StitchSavedStatCard("Saved", savedPosts.size.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StitchSavedFilterChip("Liked", selectedTab == "likes") { selectedTab = "likes" }
                    StitchSavedFilterChip("Saved", selectedTab == "saved") { selectedTab = "saved" }
                }
            }
            when {
                selectedTab == "Constitution" -> item {
                    StitchConstitutionContent()
                }
                !viewModel.isLoggedIn -> item {
                    StitchUmmahEmptyCard(
                        title = "Sign in to see activity",
                        message = "Your likes, saves, and collections are private to your account.",
                        action = "Sign in",
                        onAction = { viewModel.navigateTo(DeenScreen.LOGIN) }
                    )
                }
                feedState.loading -> item {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = stitchPrimary())
                    }
                }
                activePosts.isEmpty() -> item {
                    StitchUmmahEmptyCard(
                        title = if (selectedTab == "likes") "No liked posts yet" else "No saved posts yet",
                        message = if (selectedTab == "likes") "Tap the heart on reels and posts to build your activity." else "Tap the bookmark to save reels and posts.",
                        action = if (selectedTab == "likes") "Watch Reels" else "Open Saved",
                        onAction = { viewModel.navigateTo(if (selectedTab == "likes") DeenScreen.REELS else DeenScreen.UMMAH_SAVED) }
                    )
                }
                else -> item {
                    StitchProfileMediaGrid(
                        posts = activePosts,
                        canDelete = false,
                        onOpenPost = { post -> if (post.opensInVideoViewer()) viewModel.openReel(post.id) else viewModel.navigateTo(DeenScreen.UMMAH) },
                        onDelete = {}
                    )
                }
            }
        }
    }
}

@Composable
fun StitchUmmahArchiveScreen(viewModel: DeenViewModel) {
    StitchUmmahSettingsScaffold(
        viewModel = viewModel,
        title = "Archive",
        eyebrow = "Your activity",
        subtitle = "Private posts and reels you archive will appear here.",
        icon = Icons.Default.CloudQueue
    ) {
        item {
            StitchUmmahEmptyCard(
                title = "No archived content",
                message = "When you archive your posts or reels, they will move here privately.",
                action = "Open Ummah",
                onAction = { viewModel.navigateTo(DeenScreen.UMMAH) }
            )
        }
    }
}

@Composable
fun StitchUmmahLikesCommentsScreen(viewModel: DeenViewModel) {
    val feedState = rememberStitchUmmahFeed()
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    val commentState = rememberStitchUserComments(viewModel.isLoggedIn)
    var selectedTab by remember { mutableStateOf("likes") }
    val likedPosts = remember(feedState.posts, interactions.liked) {
        feedState.posts.filter { it.id in interactions.liked }
    }
    val postsById = remember(feedState.posts) { feedState.posts.associateBy { it.id } }

    StitchUmmahSettingsScaffold(
        viewModel = viewModel,
        title = "Likes & Comments",
        eyebrow = "Your activity",
        subtitle = "Review posts you liked and comments you wrote.",
        icon = Icons.Default.ChatBubble
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StitchSavedFilterChip("Liked", selectedTab == "likes") { selectedTab = "likes" }
                StitchSavedFilterChip("Comments", selectedTab == "comments") { selectedTab = "comments" }
            }
        }
        when {
            !viewModel.isLoggedIn -> item {
                StitchUmmahEmptyCard(
                    title = "Sign in to see activity",
                    message = "Likes and comments are connected to your account.",
                    action = "Sign in",
                    onAction = { viewModel.navigateTo(DeenScreen.LOGIN) }
                )
            }
            selectedTab == "likes" && feedState.loading -> item {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = stitchPrimary())
                }
            }
            selectedTab == "comments" && commentState.loading -> item {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = stitchPrimary())
                }
            }
            selectedTab == "likes" && likedPosts.isEmpty() -> item {
                StitchUmmahEmptyCard("No liked posts yet", "Tap the heart on any reel or post to see it here.", "Watch Reels") {
                    viewModel.navigateTo(DeenScreen.REELS)
                }
            }
            selectedTab == "comments" && commentState.comments.isEmpty() -> item {
                StitchUmmahEmptyCard("No comments yet", "Your comments will appear here after you post them.", "Open Ummah") {
                    viewModel.navigateTo(DeenScreen.UMMAH)
                }
            }
            selectedTab == "likes" -> item {
                StitchProfileMediaGrid(
                    posts = likedPosts,
                    canDelete = false,
                    onOpenPost = { post -> if (post.opensInVideoViewer()) viewModel.openReel(post.id) else viewModel.navigateTo(DeenScreen.UMMAH) },
                    onDelete = {}
                )
            }
            else -> items(commentState.comments, key = { it.id }) { comment ->
                StitchCommentActivityCard(
                    comment = comment,
                    post = postsById[comment.postId],
                    onOpenPost = {
                        val post = postsById[comment.postId]
                        if (post?.opensInVideoViewer() == true) viewModel.openReel(post.id) else viewModel.navigateTo(DeenScreen.UMMAH)
                    }
                )
            }
        }
    }
}

@Composable
fun StitchUmmahNotificationsScreen(viewModel: DeenViewModel) {
    var comments by remember { mutableStateOf(true) }
    var follows by remember { mutableStateOf(true) }
    var reminders by remember { mutableStateOf(true) }
    StitchUmmahSettingsScaffold(
        viewModel = viewModel,
        title = "Notifications",
        eyebrow = "Settings",
        subtitle = "Choose what Noor Pro should notify you about.",
        icon = Icons.Default.Notifications
    ) {
        item {
            StitchSettingsToggleCard("Comments", "Replies and comments on your posts", Icons.Default.ChatBubble, comments) { comments = it }
        }
        item {
            StitchSettingsToggleCard("Follows", "New followers and profile activity", Icons.Default.Groups, follows) { follows = it }
        }
        item {
            StitchSettingsToggleCard("Daily reminders", "Prayer, adhkar, and learning nudges", Icons.Default.Notifications, reminders) { reminders = it }
        }
    }
}

@Composable
fun StitchUmmahTimeManagementScreen(viewModel: DeenViewModel) {
    var dailyLimit by remember { mutableStateOf(false) }
    var quietMode by remember { mutableStateOf(true) }
    StitchUmmahSettingsScaffold(
        viewModel = viewModel,
        title = "Time Management",
        eyebrow = "Your activity",
        subtitle = "Keep Ummah beneficial without letting it take over your day.",
        icon = Icons.Default.CalendarMonth
    ) {
        item {
            StitchSettingsToggleCard("Daily usage reminder", "Show a gentle reminder after 20 minutes", Icons.Default.CalendarMonth, dailyLimit) { dailyLimit = it }
        }
        item {
            StitchSettingsToggleCard("Quiet mode", "Reduce social prompts during prayer and night hours", Icons.Default.DarkMode, quietMode) { quietMode = it }
        }
        item {
            StitchUmmahEmptyCard("Balanced use", "Use Noor Pro for benefit, learning, and connection. Your time controls will expand here.", "Back") {
                viewModel.goBack()
            }
        }
    }
}

@Composable
fun StitchUmmahPrivacyScreen(viewModel: DeenViewModel) {
    var privateAccount by remember { mutableStateOf(viewModel.ummahPrivateAccount()) }
    var hideCounts by remember { mutableStateOf(viewModel.ummahHideCounts()) }
    StitchUmmahSettingsScaffold(
        viewModel = viewModel,
        title = "Profile Visibility",
        eyebrow = "Settings",
        subtitle = "Published community posts and reels are public. These controls affect your profile preview.",
        icon = Icons.Default.Fingerprint
    ) {
        item {
            StitchSettingsToggleCard("Limit profile preview", "Show your profile grid to followers. Published posts remain visible in the public feed.", Icons.Default.Fingerprint, privateAccount) {
                privateAccount = it
                viewModel.setUmmahPrivacy(it, hideCounts)
            }
        }
        item {
            StitchSettingsToggleCard("Hide follower & following counts", "Others won't see your follower or following numbers", Icons.Default.Groups, hideCounts) {
                hideCounts = it
                viewModel.setUmmahPrivacy(privateAccount, it)
            }
        }
        item {
            StitchListRow("Blocked accounts", "Manage accounts you blocked", Icons.Default.Logout) {
                viewModel.navigateTo(DeenScreen.UMMAH_BLOCKED)
            }
        }
    }
}

@Composable
fun StitchUmmahCloseFriendsScreen(viewModel: DeenViewModel) {
    StitchUmmahSettingsScaffold(
        viewModel = viewModel,
        title = "Close Friends",
        eyebrow = "Settings",
        subtitle = "Close-friends sharing is not available yet. Published posts are public.",
        icon = Icons.Default.Groups
    ) {
        item {
            StitchUmmahEmptyCard(
                title = "No close friends yet",
                message = "You can discover and follow creators. Private sharing is not available yet.",
                action = "Find creators",
                onAction = { viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) }
            )
        }
    }
}

@Composable
fun StitchUmmahBlockedScreen(viewModel: DeenViewModel) {
    StitchUmmahSettingsScaffold(
        viewModel = viewModel,
        title = "Blocked",
        eyebrow = "Settings",
        subtitle = "People you block will not appear in your Ummah feed.",
        icon = Icons.Default.Logout
    ) {
        item {
            StitchUmmahEmptyCard(
                title = "No blocked accounts",
                message = "If you block someone from Ummah, you can manage them here.",
                action = "Back",
                onAction = { viewModel.goBack() }
            )
        }
    }
}

@Composable
fun StitchNoorProPlusScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    StitchUmmahSettingsScaffold(
        viewModel = viewModel,
        title = "Noor Pro Plus",
        eyebrow = "Subscription",
        subtitle = "Premium features for deeper learning and a cleaner Ummah creator experience.",
        icon = Icons.Default.Star
    ) {
        item {
            StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Coming soon", color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Noor Pro Plus", color = stitchText(), fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    listOf(
                        "Advanced Ask Imam Noor AI history",
                        "Premium learning paths and progress insights",
                        "Creator tools for Ummah reels and posts",
                        "More storage for saved collections"
                    ).forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(benefit, color = stitchText(), fontSize = 14.sp)
                        }
                    }
                    Button(
                        onClick = { Toast.makeText(context, "Noor Pro Plus coming soon", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Notify me", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchUmmahSettingsScaffold(
    viewModel: DeenViewModel,
    title: String,
    eyebrow: String,
    subtitle: String,
    icon: ImageVector,
    content: LazyListScope.() -> Unit
) {
    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(title, color = stitchText(), fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(stitchSoftSurface()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(eyebrow.uppercase(), color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(subtitle, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun StitchSettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(stitchSoftSurface()),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = stitchText(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = stitchMutedText(), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun StitchCommentActivityCard(
    comment: UmmahComment,
    post: UmmahPost?,
    onOpenPost: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPost), shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(stitchSoftSurface()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChatBubble, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("You commented", color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(commentAgo(comment.createdAt), color = stitchMutedText(), fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(comment.text, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    post?.caption?.ifBlank { post.arabicText }?.ifBlank { "Open related post" } ?: "Open related post",
                    color = stitchPrimary(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
        }
    }
}

@Composable
private fun StitchSavedStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = stitchText(), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(title, color = stitchMutedText(), fontSize = 11.sp)
        }
    }
}

@Composable
private fun StitchSavedFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) stitchPrimary() else stitchSurface())
            .border(1.dp, if (selected) stitchPrimary() else StitchLine, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StitchSavedContentCard(
    post: UmmahPost,
    collections: List<UmmahSavedCollection>,
    assignedCollectionId: String?,
    onOpen: () -> Unit,
    onOpenProfile: () -> Unit,
    onAssign: (UmmahSavedCollection) -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpen)) {
                Box(
                    modifier = Modifier
                        .size(width = 74.dp, height = 92.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(stitchSoftSurface()),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.thumbnailUrl.isNotBlank()) {
                        AsyncImage(model = post.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else if (post.mediaUrl.isNotBlank() && !post.isReelLike()) {
                        AsyncImage(model = post.mediaUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(if (post.isReelLike()) Icons.Default.SmartDisplay else Icons.Default.Article, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.caption.ifBlank { post.arabicText.ifBlank { post.category } },
                        color = stitchText(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "${if (post.isReelLike()) "Reel" else "Post"} by ${post.creatorName.ifBlank { "Community member" }}",
                        color = stitchMutedText(),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable(onClick = onOpenProfile)
                    )
                    val collectionTitle = collections.firstOrNull { it.id == assignedCollectionId }?.title
                    if (collectionTitle != null) {
                        Spacer(Modifier.height(5.dp))
                        Text("In $collectionTitle", color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
            }
            if (collections.isNotEmpty()) {
                Text("Add to collection", color = stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(collections, key = { it.id }) { collection ->
                        val selected = assignedCollectionId == collection.id
                        StitchSavedFilterChip(collection.title, selected) { onAssign(collection) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchUmmahTabs(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Posts", "Following").forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) stitchSurface() else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tab, color = if (active) stitchPrimary() else stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StitchYourPostStrip(
    reels: List<UmmahPost>,
    onCreate: () -> Unit,
    onWatchAll: () -> Unit,
    onOpenReel: (UmmahPost) -> Unit
) {
    Column {
        // "     Reels     See all" header, matching the Ummah social-hybrid design.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reels", color = stitchText(), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
            Spacer(Modifier.weight(1f))
            Text(
                if (reels.isEmpty()) "Upload" else "See all",
                color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { if (reels.isEmpty()) onCreate() else onWatchAll() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(248.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, stitchPrimary().copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                        .background(stitchSurface())
                        .clickable(onClick = onCreate),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(stitchPrimary()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Create", color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(reels.take(10), key = { it.id }) { post ->
                StitchMiniLiveReelCard(post = post, onClick = { onOpenReel(post) })
            }
        }
    }
}

@Composable
private fun StitchMiniLiveReelCard(post: UmmahPost, onClick: () -> Unit) {
    // Real view count (auto-updates from the live feed).
    val views = post.viewCount
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(248.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(StitchEmerald, Color.Black)))
            .clickable(onClick = onClick)
    ) {
        when {
            post.thumbnailUrl.isNotBlank() -> {
                AsyncImage(
                    model = post.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            post.mediaUrl.isNotBlank() -> {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Icon(Icons.Default.Mosque, contentDescription = null, tint = StitchGold.copy(alpha = 0.26f), modifier = Modifier.align(Alignment.Center).size(52.dp))
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
        Icon(
            Icons.Default.MoreVert,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(18.dp)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(
                post.caption.ifBlank { post.creatorName.ifBlank { "Reel" } },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
            if (views > 0) {
                Spacer(modifier = Modifier.height(3.dp))
                Text("${compactCount(views)} views", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StitchUmmahChatPreview(
    followedCreators: List<UmmahPost>,
    onCreatePost: () -> Unit,
    onCreateReel: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StitchHubSectionLabel("Chat")
        StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Message creators you follow", color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Follow individual users from Posts or Reels. They will appear here for private Ummah chat.",
                    color = stitchMutedText(),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                if (followedCreators.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(stitchSoftSurface())
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No followed users yet", color = stitchMutedText(), fontSize = 13.sp)
                    }
                } else {
                    followedCreators.take(8).forEach { creator ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(StitchEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(creator.creatorName.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(creator.creatorName.ifBlank { "Community member" }, color = stitchText(), fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(creator.creatorHandle.ifBlank { "@ummah" }, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1)
                            }
                            Text("Following", color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StitchHubTile("Create Post", Icons.Default.Edit, Modifier.weight(1f), onClick = onCreatePost)
            StitchHubTile("Upload Reel", Icons.Default.Videocam, Modifier.weight(1f), onClick = onCreateReel)
        }
        StitchUmmahEmptyCard("Your profile", "See your own posts, reels, and followed creators.", "Open profile", onOpenProfile)
    }
}

@Composable
internal fun StitchUmmahEmptyCard(
    title: String,
    message: String,
    action: String,
    onAction: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StitchIconBubble(icon = Icons.Default.Groups, modifier = Modifier.size(62.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(title, color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = stitchMutedText(), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(action, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StitchRealPostCard(
    post: UmmahPost,
    isFollowing: Boolean = false,
    isOwnPost: Boolean = false,
    isLiked: Boolean = false,
    isSaved: Boolean = false,
    onFollow: () -> Unit = {},
    onLike: () -> Unit = {},
    onComment: () -> Unit = {},
    onSave: () -> Unit = {},
    onShare: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenReel: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            StitchPostHeader(
                // Show the @username as the primary identity, matching the reels feed.
                name = reelUsername(post),
                subtitle = timeAgo(post.publishedAt),
                photoUrl = post.creatorPhotoUrl,
                icon = if (post.isReelLike()) Icons.Default.SmartDisplay else Icons.Default.Person,
                onClick = onOpenProfile,
                trailing = {
                    Button(
                        onClick = onFollow,
                        enabled = !isOwnPost,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) stitchSoftSurface() else stitchPrimary(),
                            contentColor = if (isFollowing) stitchPrimary() else Color.White
                        ),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(if (isOwnPost) "You" else if (isFollowing) "Following" else "Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (post.category.isNotBlank()) {
                Text(
                    post.category,
                    color = stitchPrimary(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(StitchEmerald.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                post.caption.ifBlank { post.arabicText },
                color = stitchText(),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            if (post.mediaUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                StitchFeedMediaPreview(post = post, onOpenReel = onOpenReel)
            }
            StitchPostActions(
                isLiked = isLiked,
                isSaved = isSaved,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                shareCount = post.shareCount,
                viewCount = post.viewCount,
                isVideo = post.isVideoMedia() || post.isReelLike(),
                onLike = onLike,
                onComment = onComment,
                onSave = onSave,
                onShare = onShare
            )
        }
    }
}

/** YouTube-style video post: full-width thumbnail with a play button, a channel row (avatar +
 *  title + creator + views  time), and a like/dislike + Share action row     matching the design. */
@Composable
private fun StitchYouTubePostCard(
    post: UmmahPost,
    isLiked: Boolean,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val views = post.viewCount
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(210.dp).background(Color.Black).clickable(onClick = onOpen),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = post.thumbnailUrl.ifBlank { post.mediaUrl },
                    contentDescription = post.caption.ifBlank { "Video" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)))))
                Box(
                    modifier = Modifier
                        .size(58.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play video", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(StitchGold).clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.creatorPhotoUrl.isNotBlank()) {
                        AsyncImage(model = post.creatorPhotoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Text(reelUsername(post).removePrefix("@").take(1).uppercase().ifBlank { "U" }, color = StitchEmerald, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.caption.ifBlank { "Video" },
                        color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(reelUsername(post), color = stitchMutedText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val metaLine = if (views > 0) "${compactCount(views)} views · ${timeAgo(post.publishedAt)}" else timeAgo(post.publishedAt)
                    Text(metaLine, color = stitchMutedText(), fontSize = 12.sp)
                }
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = stitchMutedText(), modifier = Modifier.padding(start = 4.dp))
            }
            Row(
                modifier = Modifier.padding(start = 64.dp, end = 12.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(stitchSoftSurface())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onLike).padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = if (isLiked) stitchPrimary() else stitchText(), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(compactCount(post.likeCount + if (isLiked) 1L else 0L), color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.width(1.dp).height(20.dp).background(StitchLine))
                    Box(
                        modifier = Modifier.clickable { }.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", tint = stitchText(), modifier = Modifier.size(18.dp))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(stitchSoftSurface()).clickable(onClick = onShare).padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = stitchText(), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StitchFeedMediaPreview(post: UmmahPost, onOpenReel: () -> Unit) {
    val isPostVideo = post.isVideoMedia() && !post.isReelLike()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                when {
                    post.isReelLike() -> 240.dp
                    isPostVideo -> 214.dp
                    else -> 190.dp
                }
            )
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPostVideo) StitchEmeraldDeep else StitchEmerald)
            .clickable(enabled = post.opensInVideoViewer(), onClick = onOpenReel),
        contentAlignment = Alignment.Center
    ) {
        if (isPostVideo) {
            // Show the real video frame (Coil decodes the first frame from the mediaUrl) instead of
            // a black box     like Instagram. A soft scrim + small play button keep it readable.
            AsyncImage(
                model = post.thumbnailUrl.ifBlank { post.mediaUrl },
                contentDescription = post.caption.ifBlank { "Post video" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)))))
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Open video", tint = Color.White, modifier = Modifier.size(34.dp))
            }
        } else if (!post.isReelLike()) {
            AsyncImage(
                model = post.thumbnailUrl.ifBlank { post.mediaUrl },
                contentDescription = post.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Reel preview: show the real first frame (Coil video decoder) instead of a gradient.
            AsyncImage(
                model = post.thumbnailUrl.ifBlank { post.mediaUrl },
                contentDescription = post.caption.ifBlank { "Reel" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play reel", tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Text(
                post.caption.ifBlank { "Tap to watch reel" },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun StitchInlinePostVideoPlayer(
    mediaUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(mediaUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)))
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        player.pause()
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setKeepContentOnPlayerReset(true)
                this.contentDescription = contentDescription
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null }
    )
}

@Composable
private fun StitchUmmahSectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = stitchText(), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
        Spacer(modifier = Modifier.weight(1f))
        Text(action, color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAction))
    }
}

@Composable
private fun StitchComposerCard(avatar: String, onOpen: () -> Unit) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(StitchGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatar, color = StitchEmerald, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(stitchSoftSurface())
                        .border(1.dp, StitchLine.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 18.dp, vertical = 13.dp)
                ) {
                    Text("Share a reflection or ask a question...", color = stitchMutedText(), fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                StitchPostTool(Icons.Default.Image, "Photo")
                StitchPostTool(Icons.Default.Videocam, "Video")
                StitchPostTool(Icons.Default.Article, "Article")
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 7.dp)
                ) {
                    Text("Post", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StitchPostTool(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 14.dp)) {
        Icon(icon, contentDescription = null, tint = stitchMutedText(), modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StitchReflectionPostCard() {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            StitchPostHeader("Amina Y.", "2 hours ago    London, UK")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Reflection",
                color = stitchPrimary(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(StitchEmerald.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 5.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Reflecting on Surah Al-Duha today. A beautiful reminder that even in moments of silence, Allah is present.",
                color = stitchText(),
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            StitchPostActions()
        }
    }
}

@Composable
private fun StitchEventPostCard() {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), borderColor = StitchGold.copy(alpha = 0.7f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            StitchPostHeader("East London Mosque", "Sponsored Event", icon = Icons.Default.Mosque)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(StitchEmerald, Color(0xFF08130F))))
            ) {
                Icon(Icons.Default.Mosque, contentDescription = null, tint = StitchGold.copy(alpha = 0.25f), modifier = Modifier.align(Alignment.Center).size(112.dp))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text("Weekly Tafsir Circle", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Text("Every Friday after Maghrib", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("+42 attending", color = stitchMutedText(), fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text("Participate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StitchQuestionPostCard() {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            StitchPostHeader("Ibrahim H.", "5 hours ago    Q&A")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Best resources for learning Tajweed?", color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
            Spacer(modifier = Modifier.height(8.dp))
            Text("As-salamu alaykum. I'm looking to improve my Quran recitation and start with tajweed basics.", color = stitchText(), fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(stitchSoftSurface())
                    .padding(14.dp)
            ) {
                Text("Top Answer: Start with Noorani Qaida and a local teacher if possible.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 18.sp)
            }
            StitchPostActions()
        }
    }
}

/** A compact advertisement that scrolls with community content. */
@Composable
private fun StitchSponsoredPostCard(modifier: Modifier = Modifier) {
    StitchCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        com.noorpro.app.ads.SponsoredFeedCard(Modifier.fillMaxWidth().padding(10.dp))
    }
}

/** A quiet, scroll-away Home ad directly below Noor Audio, as requested. */
@Composable
private fun StitchHomeSponsoredAdCard(modifier: Modifier = Modifier) {
    StitchCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        com.noorpro.app.ads.SponsoredHomeCard(Modifier.fillMaxWidth().padding(10.dp))
    }
}

@Composable
private fun StitchPostHeader(
    name: String,
    subtitle: String,
    photoUrl: String = "",
    icon: ImageVector = Icons.Default.Person,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {
        Icon(Icons.Default.MoreVert, contentDescription = null, tint = stitchMutedText())
    }
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(stitchSoftSurface()),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(icon, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(23.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = stitchMutedText(), fontSize = 11.sp)
            }
        }
        trailing()
    }
}

@Composable
private fun StitchPostActions(
    isLiked: Boolean = false,
    isSaved: Boolean = false,
    likeCount: Long = 0,
    commentCount: Long = 0,
    shareCount: Long = 0,
    viewCount: Long = 0,
    isVideo: Boolean = false,
    onLike: () -> Unit = {},
    onComment: () -> Unit = {},
    onSave: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    Spacer(modifier = Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(StitchLine.copy(alpha = 0.7f)))
    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val likeTint = if (isLiked) Color(0xFFE8505B) else stitchMutedText()
        val saveTint = if (isSaved) stitchPrimary() else stitchMutedText()
        val countStyleColor = stitchMutedText()
        Icon(Icons.Default.Favorite, contentDescription = "Like", tint = likeTint, modifier = Modifier.size(19.dp).clickable(onClick = onLike))
        if (likeCount > 0) { Spacer(Modifier.width(6.dp)); Text(compactCount(likeCount), color = countStyleColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.width(18.dp))
        Icon(Icons.Default.ChatBubble, contentDescription = "Comment", tint = stitchMutedText(), modifier = Modifier.size(18.dp).clickable(onClick = onComment))
        if (commentCount > 0) { Spacer(Modifier.width(6.dp)); Text(compactCount(commentCount), color = countStyleColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.width(18.dp))
        Icon(Icons.Default.Share, contentDescription = "Share", tint = stitchMutedText(), modifier = Modifier.size(18.dp).clickable(onClick = onShare))
        if (shareCount > 0) { Spacer(Modifier.width(6.dp)); Text(compactCount(shareCount), color = countStyleColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.Bookmark, contentDescription = "Save", tint = saveTint, modifier = Modifier.size(18.dp).clickable(onClick = onSave))
    }
    if (isVideo && viewCount > 0) {
        Spacer(modifier = Modifier.height(6.dp))
        Text("${compactCount(viewCount)} views", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StitchCommentSheet(
    post: UmmahPost,
    repository: UmmahRepository,
    isLoggedIn: Boolean,
    viewModel: DeenViewModel,
    onOpenProfile: (uid: String, name: String, handle: String) -> Unit,
    onCommentAdded: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var comments by remember(post.id) { mutableStateOf<List<UmmahComment>>(emptyList()) }
    var loading by remember(post.id) { mutableStateOf(true) }
    var draft by remember(post.id) { mutableStateOf("") }
    var sending by remember(post.id) { mutableStateOf(false) }
    var replyTarget by remember(post.id) { mutableStateOf<UmmahComment?>(null) }
    val liveProfiles = rememberStitchProfiles()

    // Hide the app's floating bottom navigation while the sheet is open so its input row
    // never collides with the navbar.
    DisposableEffect(Unit) {
        viewModel.setModalOverlayActive(true)
        onDispose { viewModel.setModalOverlayActive(false) }
    }

    DisposableEffect(post.id) {
        val registration = repository.observeComments(post.id) { list ->
            comments = list
            loading = false
        }
        onDispose { registration.remove() }
    }

    BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = stitchSurface()),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(44.dp).height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(stitchMutedText().copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (comments.isEmpty()) "Comments" else "${comments.size} comments",
                        color = stitchText(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            loading -> CircularProgressIndicator(color = stitchPrimary(), modifier = Modifier.align(Alignment.Center))
                            comments.isEmpty() -> Text(
                                "No comments yet. Be the first to share a kind word.",
                                color = stitchMutedText(), fontSize = 14.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center).padding(24.dp)
                            )
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(comments, key = { it.id }) { c ->
                                    StitchCommentRow(
                                        comment = c,
                                        photoUrl = liveProfiles[c.creatorUid]?.photoUrl.orEmpty(),
                                        onOpenProfile = { onOpenProfile(c.creatorUid, c.creatorName, "") },
                                        onReply = { target ->
                                            replyTarget = target
                                            draft = draft.ifBlank { "@${target.creatorName.ifBlank { "member" }} " }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        replyTarget?.let { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(stitchSoftSurface())
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Replying to ${target.creatorName.ifBlank { "member" }}",
                                    color = stitchPrimary(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Cancel",
                                    color = stitchMutedText(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        replyTarget = null
                                        draft = ""
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it.take(500) },
                                placeholder = { Text("Add a comment   ") },
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                shape = RoundedCornerShape(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = !sending && draft.trim().isNotBlank(),
                                onClick = {
                                    if (!isLoggedIn) {
                                        Toast.makeText(context, "Sign in to comment", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    sending = true
                                    val target = replyTarget
                                    repository.submitComment(
                                        postId = post.id,
                                        text = draft.trim(),
                                        parentCommentId = target?.id.orEmpty(),
                                        replyToName = target?.creatorName.orEmpty()
                                    ) { ok ->
                                        sending = false
                                        if (ok) {
                                            draft = ""
                                            replyTarget = null
                                            onCommentAdded()
                                        } else {
                                            Toast.makeText(context, "Unable to comment", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                if (sending) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(if (replyTarget == null) "Comment" else "Reply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun StitchCommentRow(
    comment: UmmahComment,
    photoUrl: String = "",
    onOpenProfile: () -> Unit,
    onReply: (UmmahComment) -> Unit
) {
    var liked by remember(comment.id) { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = if (comment.parentCommentId.isBlank()) 0.dp else 28.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(StitchEmerald).clickable(onClick = onOpenProfile),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "${comment.creatorName} photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(comment.creatorName.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.creatorName.ifBlank { "Member" },
                    color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onOpenProfile)
                )
                Spacer(Modifier.width(8.dp))
                Text(commentAgo(comment.createdAt), color = stitchMutedText(), fontSize = 12.sp)
            }
            Spacer(Modifier.height(3.dp))
            if (comment.replyToName.isNotBlank()) {
                Text(
                    "Replying to ${comment.replyToName}",
                    color = stitchPrimary(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
            }
            Text(comment.text, color = stitchText().copy(alpha = 0.92f), fontSize = 14.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Like",
                    color = if (liked) Color(0xFFE8505B) else stitchMutedText(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { liked = !liked }
                )
                Spacer(Modifier.width(18.dp))
                Text(
                    "Reply",
                    color = stitchMutedText(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onReply(comment) }
                )
            }
        }
        Icon(
            Icons.Default.Favorite,
            contentDescription = "Like comment",
            tint = if (liked) Color(0xFFE8505B) else stitchMutedText().copy(alpha = 0.55f),
            modifier = Modifier.size(17.dp).clickable { liked = !liked }
        )
    }
}

private fun commentAgo(createdAt: Long): String {
    if (createdAt <= 0L) return "now"
    val mins = (System.currentTimeMillis() - createdAt) / 60000
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        mins < 1440 -> "${mins / 60}h"
        else -> "${mins / 1440}d"
    }
}

@Composable
private fun StitchSignInRequiredScreen(
    title: String,
    message: String,
    onBack: () -> Unit,
    onSignIn: () -> Unit
) {
    StitchScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = stitchText(),
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    title,
                    color = stitchText(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                StitchSignInRequiredCard(
                    message = message,
                    onSignIn = onSignIn
                )
            }
        }
    }
}

@Composable
private fun StitchSignInRequiredCard(
    message: String,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 390.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = stitchPrimary().copy(alpha = 0.06f),
                spotColor = stitchPrimary().copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(stitchSurface())
            .padding(horizontal = 30.dp, vertical = 40.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 54.dp, y = (-46).dp)
                .size(166.dp)
                .clip(CircleShape)
                .background(stitchSoftSurface().copy(alpha = 0.70f))
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(stitchSoftSurface())
                    .border(1.dp, StitchLine.copy(alpha = 0.75f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = stitchPrimary(),
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Sign in required",
                color = stitchText(),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                color = stitchMutedText(),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 292.dp)
            )
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = stitchPrimary(),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Sign In", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StitchUmmahCreateScreen(
    viewModel: DeenViewModel,
    reelMode: Boolean
) {
    val context = LocalContext.current
    val repository = remember { UmmahRepository() }
    var caption by remember { mutableStateOf("") }
    var arabicText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (reelMode) "Reel" else "Reflection") }
    var postType by remember { mutableStateOf(if (reelMode) "reel" else "text") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        mediaUri = uri
        postType = "image"
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        mediaUri = uri
        postType = if (reelMode) "reel" else "video"
    }

    if (!viewModel.isLoggedIn) {
        StitchSignInRequiredScreen(
            title = if (reelMode) "Upload Reel" else "Create Post",
            message = "Please sign in before uploading posts or reels.",
            onBack = { viewModel.goBack() },
            onSignIn = { viewModel.navigateTo(DeenScreen.LOGIN) }
        )
        return
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, top = 42.dp, end = 22.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        if (reelMode) "Upload Reel" else "Create Post",
                        color = stitchText(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.noorpro.app.ui.theme.LibreCaslon
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StitchSectionLabel(if (reelMode) "Reel details" else "Post details")
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            label = { Text("Caption") },
                            placeholder = { Text("Share something beneficial...") },
                            minLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = arabicText,
                            onValueChange = { arabicText = it },
                            label = { Text("Arabic / dua text optional") },
                            minLines = 2,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(listOf("Reflection", "Reminder", "Knowledge", "Question", "Dua", "Reel")) { chip ->
                                StitchPill(chip, selected = category == chip) { category = chip }
                            }
                        }
                    }
                }
            }

            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StitchSectionLabel("Media")
                        if (mediaUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (reelMode) 320.dp else 210.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(stitchSoftSurface()),
                                contentAlignment = Alignment.Center
                            ) {
                                if (postType == "image") {
                                    AsyncImage(
                                        model = mediaUri,
                                        contentDescription = "Selected image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    StitchInlinePostVideoPlayer(
                                        mediaUrl = mediaUri.toString(),
                                        contentDescription = "Selected video",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        if (reelMode) {
                            Button(
                                onClick = { videoPicker.launch("video/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (mediaUri == null) "Choose Reel Video" else "Change Reel Video", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { imagePicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = stitchSoftSurface(), contentColor = stitchPrimary()),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Photo")
                                }
                                Button(
                                    onClick = { videoPicker.launch("video/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = stitchSoftSurface(), contentColor = stitchPrimary()),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Video")
                                }
                            }
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Text(error.orEmpty(), color = Color(0xFFBA1A1A), fontSize = 13.sp)
                }
            }

            item {
                Button(
                    enabled = !submitting,
                    onClick = {
                        submitting = true
                        error = null
                        val finalType = if (reelMode) "reel" else postType
                        repository.submitPost(
                            context = context,
                            caption = caption,
                            arabicText = arabicText,
                            category = category,
                            type = finalType,
                            mediaUri = mediaUri,
                            sourceReference = "",
                            creatorHandle = "@${viewModel.ummahUsername.ifBlank { viewModel.userEmail.substringBefore("@") }}",
                            creatorDisplayName = viewModel.userDisplayName,
                            creatorPhotoUrlOverride = viewModel.userPhotoUrl
                        ) { success, message ->
                            submitting = false
                            if (success) {
                                Toast.makeText(
                                    context,
                                    message ?: "Published to Ummah",
                                    Toast.LENGTH_LONG
                                ).show()
                                viewModel.navigateTo(if (reelMode) DeenScreen.REELS else DeenScreen.UMMAH)
                            } else {
                                error = message ?: "Unable to submit."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (submitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (reelMode) "Publish Reel" else "Publish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StitchUmmahProfileScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    val feedState = rememberStitchUmmahFeed(refreshKey)
    val repository = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { repository.currentUserUid().orEmpty() }
    val following = rememberStitchFollowing(viewModel.isLoggedIn)
    var deleteTarget by remember { mutableStateOf<UmmahPost?>(null) }
    var statDialog by remember { mutableStateOf<String?>(null) }

    val viewedUid = viewModel.viewedCreatorUid
    val isOther = viewedUid.isNotBlank() && viewedUid != currentUid
    val profileUid = if (isOther) viewedUid else currentUid
    var gridTab by remember { mutableStateOf("posts") }
    val liveProfiles = rememberStitchProfiles()
    val ownName = viewModel.userDisplayName.ifBlank { if (viewModel.isLoggedIn) "User" else "Guest" }
    val displayName = if (isOther) {
        liveProfiles[profileUid]?.name?.ifBlank { null }
            ?: viewModel.viewedCreatorName.ifBlank { "Community member" }
    } else ownName
    val profileHandle = if (isOther) {
        (liveProfiles[profileUid]?.handle?.ifBlank { null } ?: viewModel.viewedCreatorHandle.ifBlank { "member" })
            .let { if (it.startsWith("@")) it else "@$it" }
    } else {
        "@${viewModel.ummahUsername.ifBlank { viewModel.userEmail.substringBefore("@") }}"
    }
    val userPosts = feedState.posts.filter { post ->
        // Names and handles are editable and are not proof of post ownership.
        profileUid.isNotBlank() && post.creatorUid == profileUid
    }
    val visiblePosts = userPosts.filter { !it.isReelLike() }
    val userReels = userPosts.filter { it.isReelLike() }
    val profileTotalViews = userPosts.sumOf { it.viewCount }
    val isFollowingCreator = profileUid in following
    // Live followers/following for whichever profile is shown (own or another member's).
    val profileFollowers = rememberStitchFollowers(viewModel.isLoggedIn, if (isOther) profileUid else null)
    val profileFollowingUsers = rememberStitchFollowingUsers(viewModel.isLoggedIn, if (isOther) profileUid else null)
    val followersCount = profileFollowers.size
    val followingCount = if (isOther) profileFollowingUsers.size else following.size
    // Respect the viewed member's privacy flags (only applies when viewing someone else).
    val viewedProfile = if (isOther) liveProfiles[profileUid] else null
    val hideOtherCounts = viewedProfile?.hideCounts == true
    val privateLocked = viewedProfile?.isPrivate == true && !isFollowingCreator
    val profileBio = if (isOther) {
        liveProfiles[profileUid]?.bio.orEmpty()
    } else {
        liveProfiles[profileUid]?.bio.orEmpty().ifBlank { viewModel.userBio }
    }

    // Instagram-style "Saved" tab (own profile only) — reuse the live saved-interaction set.
    val interactions = rememberStitchInteractions(viewModel.isLoggedIn)
    val savedPosts = remember(feedState.posts, interactions.saved) {
        feedState.posts.filter { it.id in interactions.saved }
    }
    // Real device location used as the profile's location line (own profile).
    val locationName by viewModel.currentLocationName.collectAsState()
    val hasCurrentLocation by viewModel.hasCurrentLocation.collectAsState()

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, top = 42.dp, end = 22.dp, bottom = 62.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        if (isOther) displayName else "Profile",
                        color = stitchText(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.noorpro.app.ui.theme.LibreCaslon,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isOther) {
                        Spacer(modifier = Modifier.size(48.dp))
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.navigateTo(
                                    if (viewModel.isLoggedIn) DeenScreen.UMMAH_PROFILE_EDIT else DeenScreen.LOGIN
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = if (viewModel.isLoggedIn) "Edit" else "Sign in",
                                tint = stitchPrimary()
                            )
                        }
                    }
                }
            }

            if (!viewModel.isLoggedIn && !isOther) {
                item {
                    StitchGuestSignInProfileCard(
                        onSignIn = { viewModel.navigateTo(DeenScreen.LOGIN) },
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
                return@LazyColumn
            }

            // Instagram-style hero: avatar on the left, live content stats on the right.
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(listOf(StitchGold, StitchEmerald, StitchGold))
                            )
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(StitchEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        // Live avatar: own photo from the account, other members' from their
                        // public profile card so photo changes show here instantly.
                        val profilePhoto = if (!isOther) viewModel.userPhotoUrl
                        else liveProfiles[profileUid]?.photoUrl.orEmpty()
                        if (profilePhoto.isNotBlank()) {
                            AsyncImage(
                                model = profilePhoto,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.weight(1f)
                    ) {
                        StitchProfileStatButton(compactCount(visiblePosts.size.toLong()), "Posts", Modifier.weight(1f)) {
                            gridTab = "posts"
                        }
                        StitchProfileStatButton(compactCount(userReels.size.toLong()), "Reels", Modifier.weight(1f)) {
                            gridTab = "reels"
                        }
                        StitchProfileStatButton(if (hideOtherCounts) "—" else compactCount(followersCount.toLong()), "Followers", Modifier.weight(1f)) {
                            if (!hideOtherCounts) viewModel.navigateTo(DeenScreen.UMMAH_FOLLOWERS)
                        }
                        StitchProfileStatButton(if (hideOtherCounts) "—" else compactCount(followingCount.toLong()), "Following", Modifier.weight(1f)) {
                            if (!hideOtherCounts) viewModel.navigateTo(DeenScreen.UMMAH_FOLLOWING)
                        }
                    }
                }
            }

            // Public identity. Verification must come from a trusted verification process.
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayName,
                            color = stitchText(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = com.noorpro.app.ui.theme.LibreCaslon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(profileHandle, color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (profileBio.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            profileBio,
                            color = stitchText(),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    } else if (!isOther) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add a short bio from Edit Profile.",
                            color = stitchMutedText(),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.clickable { viewModel.navigateTo(DeenScreen.UMMAH_PROFILE_EDIT) }
                        )
                    }
                    if (!isOther && hasCurrentLocation && locationName.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = stitchMutedText(), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(locationName, color = stitchMutedText(), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("${compactCount(profileTotalViews)} total views", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                if (isOther) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (!viewModel.isLoggedIn) {
                                    Toast.makeText(context, "Sign in to follow creators", Toast.LENGTH_SHORT).show()
                                } else {
                                    repository.setFollowing(profileUid, displayName, profileHandle, !isFollowingCreator) { ok ->
                                        if (!ok) Toast.makeText(context, "Unable to update follow", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowingCreator) stitchSurface() else stitchPrimary(),
                                contentColor = if (isFollowingCreator) stitchText() else Color.White
                            ),
                            border = if (isFollowingCreator) BorderStroke(1.dp, StitchLine) else null,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isFollowingCreator) "Following" else "Follow")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.navigateTo(if (viewModel.isLoggedIn) DeenScreen.UMMAH_PROFILE_EDIT else DeenScreen.LOGIN) },
                            colors = ButtonDefaults.buttonColors(containerColor = stitchSoftSurface(), contentColor = stitchText()),
                            border = BorderStroke(1.dp, StitchLine),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        StitchProfileIconAction(Icons.Default.Share, "Share profile") {
                            val shareLink = noorProfileLink(profileUid)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "$displayName on NoorPro\n$shareLink")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share profile"))
                        }
                        StitchProfileIconAction(Icons.Default.PersonAddAlt, "Discover people") {
                            viewModel.navigateTo(DeenScreen.UMMAH_SEARCH)
                        }
                    }
                }
            }

            if (!isOther) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(listOf(StitchEmerald, StitchEmeraldDeep))
                            )
                            .clickable { viewModel.navigateTo(DeenScreen.CREATOR_STUDIO) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(StitchGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = StitchEmerald, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Creator Studio", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Private analytics dashboard", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            if (privateLocked) {
                // Private account: non-followers can't see the posts/reels grid.
                item {
                    StitchUmmahEmptyCard(
                        title = "Profile preview limited",
                        message = "Follow $profileHandle to see this profile grid. Published posts remain public in Ummah.",
                        action = "Go Back",
                        onAction = { viewModel.goBack() }
                    )
                }
            } else {
                item {
                    val tabs = buildList {
                        add(Triple("posts", Icons.Default.GridOn, "Posts"))
                        add(Triple("reels", Icons.Default.SmartDisplay, "Reels"))
                        if (!isOther) add(Triple("saved", Icons.Default.BookmarkBorder, "Saved"))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.dp, Color.Transparent))
                    ) {
                        tabs.forEach { (key, icon, label) ->
                            val active = gridTab == key
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clickable { gridTab = key }
                                    .padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (active) stitchPrimary() else stitchMutedText(),
                                    modifier = Modifier.size(22.dp).padding(bottom = 6.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(if (active) stitchPrimary() else Color.Transparent)
                                )
                            }
                        }
                    }
                }

                val gridPosts = when (gridTab) {
                    "reels" -> userReels
                    "saved" -> savedPosts
                    else -> visiblePosts
                }
                when {
                    feedState.loading -> item {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = stitchPrimary())
                        }
                    }
                    feedState.error != null -> item {
                        StitchUmmahEmptyCard(
                            title = "Couldn't load posts",
                            message = "Check your connection and try again.",
                            action = "Try Again",
                            onAction = { refreshKey++ }
                        )
                    }
                    gridPosts.isEmpty() -> item {
                        StitchUmmahEmptyCard(
                            title = when (gridTab) {
                                "reels" -> "No reels yet"
                                "saved" -> "Nothing saved yet"
                                else -> "No posts yet"
                            },
                            message = when {
                                gridTab == "saved" -> "Posts and reels you save will appear here."
                                isOther && gridTab == "reels" -> "This member hasn't shared any reels yet."
                                isOther -> "This member hasn't shared any posts yet."
                                gridTab == "reels" -> "Your reels will appear here after review."
                                else -> "Your posts will appear here after review."
                            },
                            action = when {
                                gridTab == "saved" -> "Explore Ummah"
                                isOther -> "Go Back"
                                gridTab == "reels" -> "Create Reel"
                                else -> "Create Post"
                            },
                            onAction = {
                                when {
                                    gridTab == "saved" -> viewModel.navigateTo(DeenScreen.UMMAH)
                                    isOther -> viewModel.goBack()
                                    gridTab == "reels" -> viewModel.navigateTo(DeenScreen.UMMAH_CREATE_REEL)
                                    else -> viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST)
                                }
                            }
                        )
                    }
                    else -> item {
                        StitchProfileMediaGrid(
                            posts = gridPosts,
                            // Saved items may belong to other creators — never show delete there.
                            canDelete = !isOther && gridTab != "saved",
                            onOpenPost = { post ->
                                if (post.opensInVideoViewer()) viewModel.openReel(post.id) else viewModel.navigateTo(DeenScreen.UMMAH)
                            },
                            onDelete = { post -> deleteTarget = post }
                        )
                    }
                }
            }
        }
    }
    statDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { statDialog = null },
            containerColor = stitchSurface(),
            title = { Text(title, color = stitchText(), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when (title) {
                        "Posts" -> "${visiblePosts.size} posts on this profile."
                        "Reels" -> "${userReels.size} reels on this profile."
                        "Followers" -> if (followersCount > 0) "You follow this creator." else "No followers list is available yet."
                        "Following" -> if (!isOther && following.isNotEmpty()) "You are following ${following.size} creator(s)." else "No following list is available yet."
                        else -> "Live profile statistic."
                    },
                    color = stitchMutedText()
                )
            },
            confirmButton = {
                TextButton(onClick = { statDialog = null }) {
                    Text("Close", color = stitchPrimary(), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = stitchSurface(),
            title = { Text("Delete ${if (target.isReelLike()) "reel" else "post"}?", color = stitchText(), fontWeight = FontWeight.Bold) },
            text = { Text("This permanently removes it from your profile and the Ummah feed.", color = stitchMutedText()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deletePost(target) { ok ->
                            Toast.makeText(
                                context,
                                if (ok) "${if (target.isReelLike()) "Reel" else "Post"} deleted" else "Unable to delete",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        deleteTarget = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFE5484D), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = stitchMutedText())
                }
            }
        )
    }
}

@Composable
fun StitchFollowingScreen(viewModel: DeenViewModel) {
    val currentUid = remember(viewModel.isLoggedIn) { UmmahRepository().currentUserUid().orEmpty() }
    val targetUid = viewModel.viewedCreatorUid.trim().ifBlank { null }?.takeIf { it != currentUid }
    val isOther = targetUid != null
    val users = rememberStitchFollowingUsers(viewModel.isLoggedIn, targetUid)
    StitchFollowListScreen(
        viewModel = viewModel,
        title = if (isOther) "${viewModel.viewedCreatorName.ifBlank { "Member" }}     Following" else "Following",
        users = users,
        emptyTitle = "Not following anyone yet",
        emptyMessage = if (isOther) "This member isn't following anyone yet." else "When you follow creators, they appear here."
    )
}

@Composable
fun StitchFollowersScreen(viewModel: DeenViewModel) {
    val currentUid = remember(viewModel.isLoggedIn) { UmmahRepository().currentUserUid().orEmpty() }
    val targetUid = viewModel.viewedCreatorUid.trim().ifBlank { null }?.takeIf { it != currentUid }
    val isOther = targetUid != null
    val users = rememberStitchFollowers(viewModel.isLoggedIn, targetUid)
    StitchFollowListScreen(
        viewModel = viewModel,
        title = if (isOther) "${viewModel.viewedCreatorName.ifBlank { "Member" }}     Followers" else "Followers",
        users = users,
        emptyTitle = "No followers yet",
        emptyMessage = if (isOther) "This member has no followers yet." else "Share great reels and your followers will appear here."
    )
}

@Composable
private fun StitchFollowListScreen(
    viewModel: DeenViewModel,
    title: String,
    users: List<UmmahFollowUser>,
    emptyTitle: String,
    emptyMessage: String
) {
    val context = LocalContext.current
    val repository = remember { UmmahRepository() }
    val following = rememberStitchFollowing(viewModel.isLoggedIn)
    val liveProfiles = rememberStitchProfiles()
    val currentUid = remember(viewModel.isLoggedIn) { repository.currentUserUid().orEmpty() }
    var optimistic by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(title, color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
            if (!viewModel.isLoggedIn) {
                item {
                    StitchUmmahEmptyCard("Sign in", "Sign in to see your $title.", "Sign in") { viewModel.navigateTo(DeenScreen.LOGIN) }
                }
                return@LazyColumn
            }
            item {
                Text("${users.size} ${title.lowercase()}", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (users.isEmpty()) {
                item {
                    StitchUmmahEmptyCard(emptyTitle, emptyMessage, "Back") { viewModel.goBack() }
                }
            } else {
                items(users, key = { it.uid }) { user ->
                    val isMe = user.uid == currentUid
                    val isFollowing = optimistic[user.uid] ?: (user.uid in following)
                    val liveProfile = liveProfiles[user.uid]
                    StitchFollowUserRow(
                        user = if (liveProfile == null) user else user.copy(
                            name = liveProfile.name.ifBlank { user.name },
                            handle = liveProfile.handle.ifBlank { user.handle.removePrefix("@") }
                                .let { if (it.startsWith("@")) it else "@$it" }
                        ),
                        photoUrl = liveProfile?.photoUrl.orEmpty(),
                        isMe = isMe,
                        isFollowing = isFollowing,
                        onOpen = { viewModel.openCreatorProfile(user.uid, user.name, user.handle) },
                        onMessage = { viewModel.openCreatorChat(user.uid, user.name) },
                        onToggleFollow = {
                            val next = !isFollowing
                            optimistic = optimistic + (user.uid to next)
                            repository.setFollowing(user.uid, user.name, user.handle, next) { ok ->
                                if (!ok) {
                                    optimistic = optimistic + (user.uid to isFollowing)
                                    Toast.makeText(context, "Unable to update follow", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchFollowUserRow(
    user: UmmahFollowUser,
    photoUrl: String = "",
    isMe: Boolean,
    isFollowing: Boolean,
    onOpen: () -> Unit,
    onMessage: () -> Unit,
    onToggleFollow: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(StitchEmerald),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "${user.name} photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(user.name.take(1).uppercase().ifBlank { "N" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name.ifBlank { "Community member" }, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user.handle.ifBlank { "@ummah" }, color = stitchMutedText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!isMe) {
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isFollowing) stitchSoftSurface() else stitchPrimary())
                        .clickable(onClick = onToggleFollow)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (isFollowing) "Following" else "Follow",
                        color = if (isFollowing) stitchText() else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class StitchLibraryCat(val title: String, val desc: String, val icon: ImageVector, val keywords: List<String>)

private val stitchLibraryCats = listOf(
    StitchLibraryCat("Fiqh", "Jurisprudence and Islamic rulings for daily life.", Icons.Default.Gavel, listOf("fiqh", "jurisprud", "ruling", "hanafi", "shafi", "halal", "haram")),
    StitchLibraryCat("Hadith", "Sayings and teachings of the Prophet Muhammad (PBUH).", Icons.Default.ChatBubble, listOf("hadith", "sunnah", "bukhari", "muslim", "nawawi", "riyad", "tirmidhi")),
    StitchLibraryCat("History", "The life of the Prophet and Islamic history.", Icons.Default.HistoryEdu, listOf("history", "seerah", "biograph", "prophet", "sahaba", "khilafa", "caliph")),
    StitchLibraryCat("Spirituality", "Tazkiyah, purification of the soul, and inner peace.", Icons.Default.Spa, listOf("spiritual", "tazkiyah", "tasawwuf", "soul", "heart", "ihya", "ghazali", "purif"))
)

/**
 * Discover / Islamic Library     curated digital books browser (matches the "Al-Noor / Islamic Library"
 * design). Header, search, category cards, a Featured Reading hero, and the real book catalog
 * (viewModel.libraryCategories from the GitHub catalog). Tapping a book opens it in the PDF reader.
 */
@Composable
fun StitchDiscoverLibraryScreen(viewModel: DeenViewModel) {
    val categories by viewModel.libraryCategories.collectAsState()
    val isLoading by viewModel.isLibraryLoading.collectAsState()
    val error by viewModel.libraryError.collectAsState()
    val displayName = viewModel.userDisplayName.ifBlank { if (viewModel.isLoggedIn) "Al-Noor" else "Guest" }
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { if (categories.isEmpty()) viewModel.loadLibraryCatalog() }

    val allBooks = remember(categories) { categories.flatMap { it.books } }
    val featured = remember(allBooks) {
        allBooks.firstOrNull { b -> listOf("garden", "righteous", "riyad", "nawawi").any { b.title.contains(it, true) } } ?: allBooks.firstOrNull()
    }
    val catKeywords = stitchLibraryCats.firstOrNull { it.title == selectedCat }?.keywords
    val visibleBooks = remember(allBooks, query, selectedCat) {
        allBooks.filter { b ->
            (query.isBlank() || b.title.contains(query, true) || b.author.contains(query, true)) &&
                (catKeywords == null || catKeywords.any { k -> b.title.contains(k, true) || b.author.contains(k, true) })
        }
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(StitchEmerald).clickable { viewModel.openMyProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(displayName, color = stitchText(), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = stitchText(), modifier = Modifier.size(26.dp))
                }
            }

            item {
                Column {
                    Text("Islamic Library", color = stitchText(), fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(Modifier.height(6.dp))
                    Text("Explore a curated collection of digital books, articles, and timeless wisdom.", color = stitchMutedText(), fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search books, authors, or topics   ", color = stitchMutedText()) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = stitchMutedText()) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = stitchSoftSurface(),
                        unfocusedContainerColor = stitchSoftSurface(),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = stitchText(),
                        unfocusedTextColor = stitchText()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Text("Categories", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon) }
            items(stitchLibraryCats, key = { it.title }) { cat ->
                StitchLibraryCategoryCard(cat = cat, selected = selectedCat == cat.title) {
                    selectedCat = if (selectedCat == cat.title) null else cat.title
                }
            }

            if (featured != null && query.isBlank() && selectedCat == null) {
                item { Text("Featured Reading", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon) }
                item { StitchFeaturedBookCard(featured) { viewModel.openBook(featured) } }
            }

            item {
                Text(
                    if (selectedCat != null) "$selectedCat Books" else "All Books",
                    color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon
                )
            }
            when {
                isLoading && allBooks.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = stitchPrimary()) }
                }
                error != null && allBooks.isEmpty() -> item {
                    StitchUmmahEmptyCard("Couldn't load the library", error ?: "Check your connection.", "Retry") { viewModel.loadLibraryCatalog() }
                }
                visibleBooks.isEmpty() -> item {
                    StitchUmmahEmptyCard("No books found", "Try a different search or category.", "Clear") { query = ""; selectedCat = null }
                }
                else -> items(visibleBooks.chunked(2)) { rowBooks ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        rowBooks.forEach { book ->
                            StitchLibraryBookCard(book, Modifier.weight(1f)) { viewModel.openBook(book) }
                        }
                        if (rowBooks.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchLibraryCategoryCard(cat: StitchLibraryCat, selected: Boolean, onClick: () -> Unit) {
    StitchCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        borderColor = if (selected) stitchPrimary() else null
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) stitchPrimary() else stitchSoftSurface()), contentAlignment = Alignment.Center) {
                Icon(cat.icon, contentDescription = null, tint = if (selected) Color.White else stitchText(), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(cat.title, color = stitchText(), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
            Spacer(Modifier.height(6.dp))
            Text(cat.desc, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun StitchFeaturedBookCard(book: BookItem, onRead: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(StitchEmeraldDeep, StitchEmerald)))
    ) {
        Icon(Icons.Default.Mosque, contentDescription = null, tint = StitchGold.copy(alpha = 0.16f), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(170.dp))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
        Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text("Editors' Pick", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(book.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(book.author.ifBlank { "Timeless Islamic wisdom" }, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onRead,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = StitchEmerald),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Reading", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StitchLibraryBookCard(book: BookItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = remember(book.id) { stitchBookCoverPalette(book.id.ifBlank { book.title }) }
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(palette))
        ) {
            if (book.coverUrl.isNotBlank()) {
                AsyncImage(model = book.coverUrl, contentDescription = book.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                // Generated book cover when the catalog has no image: a per-book colored spine with
                // the title set in the serif heading face, so every book still shows a real cover.
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White.copy(alpha = 0.12f), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(56.dp))
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Box(modifier = Modifier.width(32.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(StitchGold))
                    Spacer(Modifier.weight(1f))
                    Text(book.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon, lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(book.author.ifBlank { "Islamic Library" }, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(book.title, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.author.ifBlank { "Unknown author" }, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Deterministic cover gradient per book, so generated covers are varied but stable across reloads. */
private fun stitchBookCoverPalette(seed: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF0E5A4A), Color(0xFF05322A)),
        listOf(Color(0xFF1E3A5F), Color(0xFF0C1B30)),
        listOf(Color(0xFF5C3D2E), Color(0xFF2E1C14)),
        listOf(Color(0xFF3E2C5A), Color(0xFF1C1230)),
        listOf(Color(0xFF1A4D4A), Color(0xFF0A2624)),
        listOf(Color(0xFF6B4226), Color(0xFF33200F))
    )
    return palettes[kotlin.math.abs(seed.hashCode()) % palettes.size]
}

// DeenPoints level ladder (name to threshold). Used to compute the current rank, the next rank,
// and the "points to go" progress shown on the DeenPoints dashboard.
private val deenPointLevels = listOf(
    "Beginner" to 0,
    "Seeker" to 2_500,
    "Student of Knowledge" to 10_000,
    "Mu'allim" to 15_000,
    "Scholar" to 25_000,
    "Hafiz" to 50_000
)

private fun deenPointsFormat(value: Int): String =
    "%,d".format(value)

/**
 * DeenPoints dashboard     gamified points hub (matches the "Al-Noor / DEENPOINTS" design). Shows the
 * real total points, current rank + progress to the next rank, an activity breakdown, and rewards/
 * milestones. The per-activity split is estimated from the total until per-category tracking exists.
 */
@Composable
fun StitchDeenPointsScreen(viewModel: DeenViewModel) {
    val points by viewModel.totalPoints.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val completedSurahs by viewModel.completedSurahs.collectAsState()
    val displayName = viewModel.userDisplayName.ifBlank { if (viewModel.isLoggedIn) "Al-Noor" else "Guest" }

    val currentIndex = deenPointLevels.indexOfLast { points >= it.second }.coerceAtLeast(0)
    val current = deenPointLevels[currentIndex]
    val next = deenPointLevels.getOrNull(currentIndex + 1)
    val toGo = next?.let { (it.second - points).coerceAtLeast(0) } ?: 0
    val levelProgress = if (next != null && next.second > current.second) {
        ((points - current.second).toFloat() / (next.second - current.second)).coerceIn(0f, 1f)
    } else 1f

    // Estimated split of the total across activities (sums to the real total).
    val prayerPts = (points * 0.34f).toInt()
    val quranPts = (points * 0.41f).toInt()
    val communityPts = (points * 0.17f).toInt()
    val charityPts = (points - prayerPts - quranPts - communityPts).coerceAtLeast(0)

    val surahCount = completedSurahs.size
    val streakEarned = streak >= 30
    val umrahUnlocked = points >= 25_000

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(StitchEmerald)
                            .clickable { viewModel.openMyProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(displayName, color = stitchText(), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = stitchText(), modifier = Modifier.size(26.dp))
                }
            }

            // Rank badge + big total + progress
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(StitchGold.copy(alpha = 0.9f)).padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = StitchEmerald, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(current.first, color = StitchEmerald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(deenPointsFormat(points), color = stitchPrimary(), fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Text("DEENPOINTS", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (next != null) "Next: ${next.first}" else "Max rank", color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(if (next != null) "${deenPointsFormat(toGo)} pts to go" else "Complete", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        color = stitchPrimary(),
                        trackColor = StitchLine,
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp))
                    )
                }
            }

            item {
                Text("Activity Breakdown", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
            }

            // Prayer + Quran row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    StitchDeenActivityCard("PRAYER", deenPointsFormat(prayerPts), Icons.Default.Schedule, Modifier.weight(1f))
                    StitchDeenActivityCard("QURAN", deenPointsFormat(quranPts), Icons.AutoMirrored.Filled.MenuBook, Modifier.weight(1f))
                }
            }

            // Community emerald card
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(StitchEmerald).padding(20.dp)
                ) {
                    Icon(Icons.Default.Mosque, contentDescription = null, tint = Color.White.copy(alpha = 0.08f), modifier = Modifier.align(Alignment.CenterEnd).size(120.dp))
                    Column {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("COMMUNITY", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(deenPointsFormat(communityPts), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null, tint = StitchGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+15% this week", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Charity card with Give More
            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(StitchGold.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = StitchGold, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CHARITY", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(deenPointsFormat(charityPts), color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.navigateTo(DeenScreen.ZAKAT) },
                            colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text("Give More", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Rewards & Milestones header
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Rewards & Milestones", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon, modifier = Modifier.weight(1f))
                    Text("View All", color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.navigateTo(DeenScreen.QUIZ_DASHBOARD) })
                }
            }

            item {
                StitchMilestoneCard(
                    icon = Icons.Default.Star,
                    title = "30-Day Prayer Streak",
                    subtitle = if (streakEarned) "Completed all five prayers on time" else "Pray all five on time, ${streak}/30 days",
                    trailing = if (streakEarned) "Earned" else "${streak}/30",
                    progress = if (streakEarned) null else (streak / 30f).coerceIn(0f, 1f)
                )
            }
            item {
                StitchMilestoneCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Quran Explorer",
                    subtitle = "Surahs completed",
                    trailing = "$surahCount/114",
                    progress = (surahCount / 114f).coerceIn(0f, 1f)
                )
            }

            // Locked reward (Umrah Essentials Kit)
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(StitchEmerald, StitchEmeraldDeep, Color.Black)))
                ) {
                    Icon(Icons.Default.Mosque, contentDescription = null, tint = StitchGold.copy(alpha = 0.18f), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(150.dp))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                        Text(if (umrahUnlocked) "REWARD UNLOCKED" else "LOCKED REWARD", color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Umrah Essentials Kit", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (umrahUnlocked) Icons.Default.CheckCircle else Icons.Default.Fingerprint, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(if (umrahUnlocked) "Unlocked     claim in profile" else "Requires 25,000 Points", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchDeenActivityCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    StitchCard(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(stitchSoftSurface()), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = stitchText(), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(label, color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = stitchText(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StitchMilestoneCard(icon: ImageVector, title: String, subtitle: String, trailing: String, progress: Float?) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(stitchSoftSurface()), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(trailing, color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (progress != null) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = StitchGold,
                        trackColor = StitchLine,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun StitchCreatorStudioScreen(viewModel: DeenViewModel) {
    val feedState = rememberStitchUmmahFeed()
    val repository = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { repository.currentUserUid().orEmpty() }

    val profileHandleKey = viewModel.ummahUsername.ifBlank { viewModel.userEmail.substringBefore("@") }
        .removePrefix("@")
        .trim()
        .lowercase()
    val profileNameKey = viewModel.userDisplayName.trim().lowercase()
    val emailHandleKey = viewModel.userEmail.substringBefore("@").trim().lowercase()
    val userPosts = feedState.posts.filter { post ->
        val postHandleKey = post.creatorHandle.removePrefix("@").trim().lowercase()
        val postNameKey = post.creatorName.trim().lowercase()
        post.creatorUid == currentUid ||
            (post.creatorUid.isBlank() && (
                (profileHandleKey.isNotBlank() && postHandleKey == profileHandleKey) ||
                    (emailHandleKey.isNotBlank() && postHandleKey == emailHandleKey) ||
                    (profileNameKey.isNotBlank() && postNameKey == profileNameKey)
                ))
    }
    val reels = userPosts.filter { it.isReelLike() }
    val posts = userPosts.filter { !it.isReelLike() }
    val totalLikes = userPosts.sumOf { it.likeCount }
    val totalComments = userPosts.sumOf { it.commentCount }
    val totalShares = userPosts.sumOf { it.shareCount }
    val totalEngagement = totalLikes + totalComments + totalShares
    // Real, auto-updating view total (summed from live post viewCounts).
    val totalViews = userPosts.sumOf { it.viewCount }
    val recentContent = userPosts.sortedByDescending { it.publishedAt }.take(7).reversed()
    val chartBars = recentContent.mapIndexed { i, p -> "${i + 1}" to p.viewCount }
    val topContent = userPosts.sortedByDescending { it.viewCount }.take(3)

    val reqReels = reels.size >= 5
    val reqEngagement = totalEngagement >= 100
    val reqConsistency = userPosts.size >= 10

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Creator Studio", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            if (!viewModel.isLoggedIn) {
                item {
                    StitchUmmahEmptyCard(
                        title = "Sign in to view Creator Studio",
                        message = "Creator Studio tracks live reach, likes, comments, shares, posts, and reels.",
                        action = "Sign in",
                        onAction = { viewModel.navigateTo(DeenScreen.LOGIN) }
                    )
                }
                return@LazyColumn
            }

            // Live views hero
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.verticalGradient(listOf(StitchEmerald, StitchEmeraldDeep)))
                        .padding(20.dp)
                ) {
                    Column {
                        Text("LIVE VIEWS", color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(compactCount(totalViews), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Auto-updates from real Firestore view counts on your posts and reels.",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StitchDarkStatPill("Likes", compactCount(totalLikes), Modifier.weight(1f))
                            StitchDarkStatPill("Comments", compactCount(totalComments), Modifier.weight(1f))
                            StitchDarkStatPill("Shares", compactCount(totalShares), Modifier.weight(1f))
                        }
                    }
                }
            }

            // Reach + engagement metrics
            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StitchSectionLabel("Your reach")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StitchSavedStatCard("Views", compactCount(totalViews), Modifier.weight(1f))
                            StitchSavedStatCard("Reels", reels.size.toString(), Modifier.weight(1f))
                            StitchSavedStatCard("Posts", posts.size.toString(), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StitchSavedStatCard("Likes", compactCount(totalLikes), Modifier.weight(1f))
                            StitchSavedStatCard("Comments", compactCount(totalComments), Modifier.weight(1f))
                            StitchSavedStatCard("Shares", compactCount(totalShares), Modifier.weight(1f))
                        }
                    }
                }
            }

            // Performance graph (YouTube Studio style)
            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StitchSectionLabel("Performance")
                        Text("Real views across your ${recentContent.size} most recent uploads.", color = stitchMutedText(), fontSize = 12.sp)
                        if (chartBars.isEmpty()) {
                            Text("Share posts or reels to see your performance graph.", color = stitchMutedText(), fontSize = 13.sp)
                        } else {
                            StitchCreatorBarChart(bars = chartBars)
                        }
                    }
                }
            }

            // Top content ranking
            if (topContent.isNotEmpty()) {
                item {
                    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StitchSectionLabel("Top content")
                            topContent.forEachIndexed { i, p ->
                                StitchTopContentRow(rank = i + 1, post = p)
                            }
                        }
                    }
                }
            }

            // Creator health checklist
            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StitchSectionLabel("Creator health")
                        StitchEligibilityRow("Publish at least 5 reels", "${reels.size} / 5", reqReels)
                        StitchEligibilityRow("Reach 100 total engagements", "${compactCount(totalEngagement)} / 100", reqEngagement)
                        StitchEligibilityRow("Share 10 posts or reels", "${userPosts.size} / 10", reqConsistency)
                    }
                }
            }

            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Live data", color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "These private analytics update from the Ummah feed in real time and are shown here in Creator Studio.",
                            color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }

}

/** Simple, dependency-free bar chart (gold→emerald bars) for the Creator Studio performance graph. */
@Composable
private fun StitchCreatorBarChart(bars: List<Pair<String, Long>>) {
    val maxV = (bars.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { (label, value) ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(compactCount(value), color = stitchMutedText(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        val frac = (value.toFloat() / maxV.toFloat()).coerceIn(0.04f, 1f)
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((frac * 118f).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(Brush.verticalGradient(listOf(StitchGold, StitchEmerald)))
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(label, color = stitchMutedText(), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StitchTopContentRow(rank: Int, post: UmmahPost) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(StitchEmerald.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                post.caption.ifBlank { if (post.isReelLike()) "Reel" else "Post" },
                color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${compactCount(post.viewCount)} views · ${compactCount(post.likeCount)} likes · ${compactCount(post.commentCount)} comments",
                color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(compactCount(post.viewCount), color = stitchPrimary(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StitchDarkStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun StitchEligibilityRow(label: String, value: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.Lightbulb,
            contentDescription = null,
            tint = if (done) stitchPrimary() else stitchMutedText(),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = stitchText(), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (done) stitchPrimary() else stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StitchUmmahProfileEditScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf(viewModel.userDisplayName.ifBlank { "User" }) }
    var username by remember { mutableStateOf(viewModel.ummahUsername.ifBlank { viewModel.userEmail.substringBefore("@") }) }
    var bio by remember { mutableStateOf(viewModel.userBio) }
    var saving by remember { mutableStateOf(false) }
    var sendingReset by remember { mutableStateOf(false) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val photoUrl = viewModel.userPhotoUrl
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadingPhoto = true
            error = null
            message = null
            viewModel.updateProfilePhoto(context, uri) { ok, result ->
                uploadingPhoto = false
                if (ok) {
                    Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                } else {
                    error = result ?: "Unable to update photo."
                }
            }
        }
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, top = 42.dp, end = 22.dp, bottom = 78.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Edit Profile", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            if (!viewModel.isLoggedIn) {
                item {
                    StitchUmmahEmptyCard(
                        title = "Sign in required",
                        message = "Sign in to edit your Ummah account.",
                        action = "Sign in",
                        onAction = { viewModel.navigateTo(DeenScreen.LOGIN) }
                    )
                }
                return@LazyColumn
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier.size(104.dp).clip(CircleShape).background(StitchEmerald)
                                .clickable(enabled = !uploadingPhoto) { photoPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                            }
                            if (uploadingPhoto) {
                                Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(stitchPrimary())
                                .border(2.dp, stitchSurface(), CircleShape)
                                .clickable(enabled = !uploadingPhoto) { photoPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (uploadingPhoto) "Uploading photo   " else "Tap to change profile photo",
                        color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("@${username.removePrefix("@")}", color = stitchPrimary(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StitchSectionLabel("Profile")
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display name") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.removePrefix("@").replace(" ", "_") },
                            label = { Text("Username") },
                            prefix = { Text("@") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it.take(160) },
                            label = { Text("Bio") },
                            placeholder = { Text("Tell people about your Islamic, education, or motivation journey") },
                            supportingText = { Text("${bio.length}/160") },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StitchSectionLabel("Account")
                        Text("Email", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(viewModel.userEmail, color = stitchText(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Button(
                            enabled = !sendingReset,
                            onClick = {
                                sendingReset = true
                                error = null
                                message = null
                                viewModel.sendPasswordReset { success, result ->
                                    sendingReset = false
                                    if (success) {
                                        message = "Password reset email sent."
                                        Toast.makeText(context, "Password reset email sent", Toast.LENGTH_SHORT).show()
                                    } else {
                                        error = result ?: "Unable to send reset email."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = stitchSoftSurface(), contentColor = stitchPrimary()),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (sendingReset) "Sending..." else "Send password reset email", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (message != null || error != null) {
                item {
                    Text(
                        message ?: error.orEmpty(),
                        color = if (message != null) stitchPrimary() else Color(0xFFBA1A1A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        error = null
                        message = null
                        viewModel.updateUmmahProfile(displayName, username, bio) { success, result ->
                            saving = false
                            if (success) {
                                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                viewModel.goBack()
                            } else {
                                error = result ?: "Unable to save profile."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (saving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchTinyProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = stitchMutedText(), fontSize = 12.sp)
    }
}

@Composable
private fun StitchProfileIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = stitchText(), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StitchProfileStatButton(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(value, color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StitchProfileMediaGrid(
    posts: List<UmmahPost>,
    canDelete: Boolean,
    onOpenPost: (UmmahPost) -> Unit,
    onDelete: (UmmahPost) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        posts.chunked(3).forEach { rowPosts ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowPosts.forEach { post ->
                    StitchProfileMediaTile(
                        post = post,
                        modifier = Modifier.weight(1f),
                        canDelete = canDelete,
                        onClick = { onOpenPost(post) },
                        onDelete = { onDelete(post) }
                    )
                }
                repeat(3 - rowPosts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StitchProfileMediaTile(
    post: UmmahPost,
    modifier: Modifier = Modifier,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = modifier
            .height(if (post.isReelLike()) 178.dp else 126.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        when {
            // Show the real visual (Instagram-style): a thumbnail if present, otherwise the media
            // itself     for videos/reels the app-wide Coil VideoFrameDecoder renders the first frame
            // instead of a black/gradient placeholder.
            post.thumbnailUrl.isNotBlank() -> AsyncImage(
                model = post.thumbnailUrl,
                contentDescription = post.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            post.mediaUrl.isNotBlank() -> AsyncImage(
                model = post.mediaUrl,
                contentDescription = post.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                Box(Modifier.fillMaxSize().background(stitchSurface()))
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = stitchPrimary().copy(alpha = 0.4f), modifier = Modifier.align(Alignment.Center).size(42.dp))
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f)))))
        if (post.isReelLike()) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Reel", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp))
        }
        if (canDelete) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(17.dp))
            }
        }
        Text(
            post.caption.ifBlank { post.category },
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(9.dp)
        )
    }
}

@Composable
private fun StitchProfileContentTabs(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Posts", "Reels").forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) stitchSurface() else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tab, color = if (active) stitchPrimary() else stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun repairMojibake(value: String): String {
    return try {
        if (value.any { it.code in 0x0080..0x00FF }) {
            String(value.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        } else {
            value
        }
    } catch (_: Exception) {
        value
    }
}

private fun createNoorChatCacheFile(context: Context, extension: String): File {
    val dir = File(context.cacheDir, "noor_chat_media").apply { mkdirs() }
    return File(dir, "noor_chat_${System.currentTimeMillis()}.$extension")
}

private fun createNoorChatFileUri(context: Context, extension: String): Uri {
    val file = createNoorChatCacheFile(context, extension)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// ---- Instagram-style direct messages -----------------------------------------------------

/** Renders [content] as a QR bitmap (black on white) sized [size]px. */
private fun generateQrBitmap(content: String, size: Int = 640): android.graphics.Bitmap? {
    return runCatching {
        val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
        val matrix = com.google.zxing.qrcode.QRCodeWriter()
            .encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        android.graphics.Bitmap.createBitmap(pixels, size, size, android.graphics.Bitmap.Config.RGB_565)
    }.getOrNull()
}

private const val NoorWebBaseUrl = "https://noor-pro-d87e3.web.app"
private const val NoorAndroidPackageName = "com.noorpro.app"

private fun noorProfileLink(uid: String): String =
    "$NoorWebBaseUrl/u?uid=${Uri.encode(uid)}"

private fun noorGroupLink(groupId: String, groupName: String): String =
    "$NoorWebBaseUrl/g?id=${Uri.encode(groupId)}&name=${Uri.encode(groupName)}"

private fun noorAndroidIntentLink(webLink: String): String {
    val linkWithoutScheme = webLink.removePrefix("https://")
    return "intent://$linkWithoutScheme#Intent;scheme=https;package=$NoorAndroidPackageName;S.browser_fallback_url=${Uri.encode(webLink)};end"
}

/** WhatsApp-style QR sheet for sharing a profile or group. Scanning the code opens [content]. */
@Composable
private fun StitchQrDialog(
    title: String,
    subtitle: String,
    content: String,
    shareText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qr = remember(content) { generateQrBitmap(content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = stitchSurface(),
        title = { Text(title, color = stitchText(), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(subtitle, color = stitchMutedText(), fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "QR code",
                        modifier = Modifier
                            .size(230.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(2.dp, StitchGold, RoundedCornerShape(18.dp))
                            .padding(10.dp)
                    )
                } else {
                    Text("Unable to generate QR", color = stitchMutedText())
                }
                Spacer(Modifier.height(10.dp))
                Text("Scan opens Noor Pro if installed, otherwise the web page.", color = stitchMutedText(), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share link", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = stitchMutedText()) }
        }
    )
}

@Composable
fun StitchMessagesComingSoonScreen(viewModel: DeenViewModel) {
    StitchScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.UMMAH) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                }
                Text(
                    "Messages",
                    color = stitchText(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.weight(1f))
            StitchCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(stitchSoftSurface()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(42.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Messages coming soon",
                        color = stitchText(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Private chats and community groups are paused for this version. We will add the full messaging section in the next release.",
                        color = stitchMutedText(),
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                    Button(
                        onClick = { viewModel.navigateTo(DeenScreen.UMMAH) },
                        colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Back to Ummah", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StitchMessagesScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val repository = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { repository.currentUserUid().orEmpty() }
    val userInitials = remember(viewModel.userDisplayName, viewModel.userEmail) {
        val source = viewModel.userDisplayName.ifBlank { viewModel.userEmail.substringBefore("@") }.ifBlank { "TM" }
        source.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.take(1).uppercase() }
            .ifBlank { "TM" }
    }
    var chats by remember { mutableStateOf<List<UmmahChat>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var groups by remember { mutableStateOf<List<UmmahGroup>>(emptyList()) }
    var groupError by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf("Chats") }
    var peopleQuery by remember { mutableStateOf("") }
    var showMyQr by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var creatingGroup by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<UmmahGroup?>(null) }
    var profiles by remember { mutableStateOf<Map<String, com.noorpro.app.data.UmmahProfile>>(emptyMap()) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val followingUsers = rememberStitchFollowingUsers(viewModel.isLoggedIn)
    val followerUsers = rememberStitchFollowers(viewModel.isLoggedIn)
    // Merge following + followers and overlay each person's live profile (name/@username/photo)
    // so the list always shows current identity     never internal account IDs.
    val people = remember(followingUsers, followerUsers, currentUid, profiles) {
        (followingUsers + followerUsers)
            .distinctBy { it.uid }
            .filter { it.uid.isNotBlank() && it.uid != currentUid }
            .map { user ->
                val profile = profiles[user.uid] ?: return@map user
                user.copy(
                    name = profile.name.ifBlank { user.name },
                    handle = profile.handle.ifBlank { user.handle.removePrefix("@") }
                        .let { if (it.startsWith("@")) it else "@$it" }
                )
            }
    }
    val filteredPeople = remember(people, peopleQuery) {
        val q = peopleQuery.trim().lowercase()
        if (q.isBlank()) people else people.filter {
            it.name.lowercase().contains(q) || it.handle.lowercase().contains(q)
        }
    }

    DisposableEffect(viewModel.isLoggedIn) {
        if (viewModel.isLoggedIn) {
            repository.observeChats { list, err -> chats = list; error = err }
            repository.observeGroups { list, err -> groups = list; groupError = err }
            repository.observeProfiles { profiles = it }
        }
        onDispose { repository.close() }
    }

    LaunchedEffect(groups, viewModel.groupFocusId) {
        val targetGroupId = viewModel.groupFocusId
        if (targetGroupId.isNotBlank()) {
            selectedTab = "Groups"
            groups.firstOrNull { it.id == targetGroupId }?.let { group ->
                selectedGroup = group
                viewModel.consumeGroupInviteFocus()
            }
        }
    }

    selectedGroup?.let { group ->
        // Keep the open panel in sync with live group data (e.g. after adding members).
        val liveGroup = groups.firstOrNull { it.id == group.id } ?: group
        StitchGroupChatPanel(
            group = liveGroup,
            repository = repository,
            currentUid = currentUid,
            profiles = profiles,
            people = people,
            onOpenProfile = { uid, name -> viewModel.openCreatorProfile(uid, name, "") },
            onBack = { selectedGroup = null }
        )
        return
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(stitchSurface()),
            contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                StitchMessagesHubTopBar(
                    initials = userInitials,
                    onSearch = { selectedTab = "People" },
                    onQr = { showMyQr = true },
                    onAdd = { showCreateGroup = true }
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 42.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Private Ummah messaging",
                        color = stitchPrimary(),
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Engage in private, respectful, and beneficial conversations within the community.",
                        color = stitchMutedText(),
                        fontSize = 17.sp,
                        lineHeight = 26.sp
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E9EC))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Chats", "People", "Groups", "Constitution").forEach { tab ->
                        StitchMessageTab(
                            label = tab,
                            selected = selectedTab == tab,
                            modifier = Modifier.weight(1f)
                        ) { selectedTab = tab }
                    }
                }
            }
            when {
                !viewModel.isLoggedIn -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((screenHeight - 300.dp).coerceAtLeast(420.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        StitchMessagesSignInCard(
                            onSignIn = { viewModel.navigateTo(DeenScreen.LOGIN) }
                        )
                    }
                }
                selectedTab == "Chats" && error != null -> item {
                    StitchUmmahEmptyCard(title = "Messages unavailable", message = error.orEmpty(), action = "Find people", onAction = { selectedTab = "People" })
                }
                selectedTab == "Chats" && chats.isEmpty() -> item {
                    StitchUmmahEmptyCard(
                        title = "No conversations yet",
                        message = "Find someone in People     everyone you follow and your followers are there.",
                        action = "Find people",
                        onAction = { selectedTab = "People" }
                    )
                }
                selectedTab == "Chats" -> items(chats, key = { it.id }) { chat ->
                    val otherUid = chat.participantUids.firstOrNull { it != currentUid }.orEmpty()
                    val profile = profiles[otherUid]
                    val otherName = profile?.name?.ifBlank { null }
                        ?: chat.participantNames[otherUid]?.ifBlank { null } ?: "Member"
                    val otherPhoto = profile?.photoUrl.orEmpty()
                    StitchCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            repository.markChatRead(chat.id)
                            viewModel.openCreatorChat(otherUid, otherName)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape).background(StitchEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                if (otherPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = otherPhoto,
                                        contentDescription = "$otherName photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Text(otherName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(otherName, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                                    Spacer(Modifier.width(8.dp))
                                    if (chat.updatedAt > 0) {
                                        Text(commentAgo(chat.updatedAt), color = stitchMutedText(), fontSize = 11.sp)
                                    }
                                }
                                val preview = chat.lastMessage.ifBlank { "Tap to chat" }
                                Text(
                                    if (chat.lastSenderUid == currentUid && chat.lastMessage.isNotBlank()) "You: $preview" else preview,
                                    color = if (chat.unreadCount > 0) stitchText() else stitchMutedText(),
                                    fontSize = 13.sp,
                                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (chat.unreadCount > 0) {
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .height(22.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(stitchPrimary())
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (chat.unreadCount > 9) "9+" else chat.unreadCount.toString(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("New", color = stitchPrimary(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                selectedTab == "People" -> {
                    item {
                        OutlinedTextField(
                            value = peopleQuery,
                            onValueChange = { peopleQuery = it.take(80) },
                            placeholder = { Text("Search by name or @username") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = stitchPrimary()) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                    if (filteredPeople.isEmpty()) {
                        item {
                            StitchUmmahEmptyCard(
                                title = "No people yet",
                                message = "Follow creators from posts and reels     everyone you follow (and your followers) appears here, ready to message.",
                                action = "Search Ummah",
                                onAction = { viewModel.navigateTo(DeenScreen.UMMAH_SEARCH) }
                            )
                        }
                    } else {
                        items(filteredPeople, key = { it.uid }) { user ->
                            StitchMessagePersonRow(
                                user = user,
                                photoUrl = profiles[user.uid]?.photoUrl.orEmpty(),
                                onOpen = { viewModel.openCreatorProfile(user.uid, user.name, user.handle) },
                                onMessage = { viewModel.openCreatorChat(user.uid, user.name) }
                            )
                        }
                    }
                }
                selectedTab == "Groups" -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showCreateGroup = true },
                                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.navigateTo(DeenScreen.DISCOVER_GROUPS) },
                                colors = ButtonDefaults.buttonColors(containerColor = StitchGold, contentColor = StitchEmeraldDeep),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Discover & join", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    groupError?.let { message ->
                        item { Text(message, color = Color(0xFFE5484D), fontSize = 12.sp) }
                    }
                    if (groups.isEmpty()) {
                        item {
                            StitchUmmahEmptyCard(
                                title = "No groups yet",
                                message = "Create a clean Islamic, education, or motivation group for your community.",
                                action = "Create group",
                                onAction = { showCreateGroup = true }
                            )
                        }
                    } else {
                        items(groups, key = { it.id }) { group ->
                            StitchCommunityGroupRow(group = group, onOpen = { selectedGroup = group })
                        }
                    }
                }
                selectedTab == "Constitution" -> item {
                    StitchConstitutionContent()
                }
            }
        }
    }

    if (showMyQr) {
        val myProfile = profiles[currentUid]
        val myLink = noorProfileLink(currentUid)
        val qrLink = noorAndroidIntentLink(myLink)
        StitchQrDialog(
            title = "My Noor QR",
            subtitle = "Friends scan this to find ${myProfile?.name?.ifBlank { null } ?: "you"} on Noor Pro and start messaging.",
            content = qrLink,
            shareText = "Find me on Noor Pro: $myLink\n\nIf Noor Pro is installed, this opens my profile in the app.\nGet the app: $NoorWebBaseUrl\nHelp & contact: noorpro.official@gmail.com",
            onDismiss = { showMyQr = false }
        )
    }

    if (showCreateGroup) {
        AlertDialog(
            onDismissRequest = { if (!creatingGroup) showCreateGroup = false },
            containerColor = stitchSurface(),
            title = { Text("Create community group", color = stitchText(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it.take(60) },
                        placeholder = { Text("Group name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it.take(180) },
                        placeholder = { Text("Purpose: Islamic learning, motivation, clean education") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Groups must follow the Noor Pro Constitution: beneficial, clean, Islamic adab, no harmful or haram content.",
                        color = stitchMutedText(),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !creatingGroup,
                    onClick = {
                        creatingGroup = true
                        repository.createGroup(groupName, groupDescription) { ok, message ->
                            creatingGroup = false
                            if (ok) {
                                groupName = ""
                                groupDescription = ""
                                showCreateGroup = false
                            } else {
                                Toast.makeText(
                                    context,
                                    message ?: "Unable to create group",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) { Text(if (creatingGroup) "Creating..." else "Create", color = stitchPrimary()) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroup = false }) { Text("Cancel", color = stitchMutedText()) }
            }
        )
    }
}

@Composable
private fun StitchMessagesHubTopBar(
    initials: String,
    onSearch: () -> Unit,
    onQr: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E9EC)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials.take(2),
                color = stitchPrimary(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            "Ummah",
            color = stitchPrimary(),
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearch) {
            Icon(Icons.Default.Search, contentDescription = "Search messages", tint = stitchPrimary(), modifier = Modifier.size(29.dp))
        }
        IconButton(onClick = onQr) {
            Icon(Icons.Default.QrCode2, contentDescription = "QR", tint = stitchPrimary(), modifier = Modifier.size(27.dp))
        }
        IconButton(onClick = onAdd) {
            Box(
                modifier = Modifier
                    .size(31.dp)
                    .clip(CircleShape)
                    .border(2.dp, stitchPrimary(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create group", tint = stitchPrimary(), modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun StitchMessagesSignInCard(onSignIn: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .shadow(22.dp, RoundedCornerShape(22.dp), clip = false)
            .padding(horizontal = 28.dp, vertical = 46.dp),
        contentAlignment = Alignment.Center
    ) {
        StitchSubtleGeometry(modifier = Modifier.matchParentSize(), tint = stitchPrimary().copy(alpha = 0.018f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEF5F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "Sign in to message",
                color = stitchPrimary(),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Connect with brothers and sisters globally. Your conversations are secure and private.",
                color = stitchMutedText(),
                fontSize = 16.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF745C00)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text("Sign In / Register", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StitchSubtleGeometry(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val gap = 52.dp.toPx()
        val radius = 18.dp.toPx()
        var x = -gap
        while (x < size.width + gap) {
            var y = -gap
            while (y < size.height + gap) {
                drawCircle(color = tint, radius = radius, center = Offset(x, y), style = Stroke(width = 1.dp.toPx()))
                drawLine(color = tint, start = Offset(x - radius, y), end = Offset(x + radius, y), strokeWidth = 1.dp.toPx())
                drawLine(color = tint, start = Offset(x, y - radius), end = Offset(x, y + radius), strokeWidth = 1.dp.toPx())
                y += gap
            }
            x += gap
        }
    }
}

@Composable
private fun StitchMessageTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) stitchPrimary() else Color(0xFF404943),
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StitchMessagePersonRow(
    user: UmmahFollowUser,
    photoUrl: String = "",
    onOpen: () -> Unit,
    onMessage: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(StitchEmerald),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "${user.name} photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(user.name.take(1).uppercase().ifBlank { "N" }, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(user.name.ifBlank { "Community member" }, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(user.handle.ifBlank { "@ummah" }, color = stitchMutedText(), fontSize = 13.sp, maxLines = 1)
            }
            Button(
                onClick = onMessage,
                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Message", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StitchCommunityGroupRow(group: UmmahGroup, onOpen: () -> Unit) {
    StitchCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(stitchPrimary()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    group.lastMessage.ifBlank { group.description.ifBlank { "${group.memberUids.size} member(s)" } },
                    color = stitchMutedText(),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
        }
    }
}

@Composable
private fun StitchGroupChatPanel(
    group: UmmahGroup,
    repository: UmmahRepository,
    currentUid: String,
    profiles: Map<String, com.noorpro.app.data.UmmahProfile> = emptyMap(),
    people: List<UmmahFollowUser> = emptyList(),
    onOpenProfile: (uid: String, name: String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    var messages by remember(group.id) { mutableStateOf<List<UmmahMessage>>(emptyList()) }
    var error by remember(group.id) { mutableStateOf<String?>(null) }
    var draft by remember(group.id) { mutableStateOf("") }
    var sending by remember(group.id) { mutableStateOf(false) }
    var showGroupInfo by remember(group.id) { mutableStateOf(false) }
    var openGroupInfoInAddMode by remember(group.id) { mutableStateOf(false) }
    var pendingMedia by remember(group.id) { mutableStateOf<Uri?>(null) }
    var pendingType by remember(group.id) { mutableStateOf("text") }
    var cameraCaptureUri by remember(group.id) { mutableStateOf<Uri?>(null) }
    var recorder by remember(group.id) { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember(group.id) { mutableStateOf<File?>(null) }
    var isRecording by remember(group.id) { mutableStateOf(false) }
    var showAttachments by remember(group.id) { mutableStateOf(false) }
    var replyTarget by remember(group.id) { mutableStateOf<UmmahMessage?>(null) }
    val context = LocalContext.current

    fun sendGroupPayload(
        text: String = draft.trim(),
        type: String = pendingType,
        mediaUri: Uri? = pendingMedia,
        sharedMediaUrl: String = ""
    ) {
        if (sending) return
        if (text.isBlank() && mediaUri == null && sharedMediaUrl.isBlank()) {
            Toast.makeText(context, "Write a message or choose media.", Toast.LENGTH_SHORT).show()
            return
        }
        sending = true
        val reply = replyTarget
        repository.sendGroupMessage(
            context = context,
            groupId = group.id,
            text = text,
            type = type,
            mediaUri = mediaUri,
            sharedMediaUrl = sharedMediaUrl,
            replyToMessageId = reply?.id.orEmpty(),
            replyToName = reply?.senderName.orEmpty(),
            replyToText = reply?.let { noorMessageReplyText(it) }.orEmpty(),
            replyToType = reply?.type.orEmpty()
        ) { ok, message ->
            sending = false
            if (ok) {
                draft = ""
                pendingMedia = null
                pendingType = "text"
                replyTarget = null
                showAttachments = false
            } else {
                Toast.makeText(context, message ?: "Unable to send", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingType = "image"
            pendingMedia = it
            showAttachments = true
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingType = "video"
            pendingMedia = it
            showAttachments = true
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingType = "file"
            pendingMedia = it
            showAttachments = true
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingType = "pdf"
            pendingMedia = it
            showAttachments = true
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            cameraCaptureUri?.let {
                pendingType = "image"
                pendingMedia = it
                showAttachments = true
            }
        } else {
            cameraCaptureUri = null
        }
    }

    fun captureCameraPhoto() {
        val uri = createNoorChatFileUri(context, "jpg")
        cameraCaptureUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) captureCameraPhoto() else Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_SHORT).show()
    }

    fun startVoiceRecording() {
        try {
            val file = createNoorChatCacheFile(context, "m4a")
            @Suppress("DEPRECATION")
            val newRecorder = MediaRecorder()
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
            recorder = newRecorder
            recordingFile = file
            isRecording = true
            Toast.makeText(context, "Recording voice message...", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            recorder?.release()
            recorder = null
            recordingFile = null
            isRecording = false
            Toast.makeText(context, error.localizedMessage ?: "Unable to record audio.", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVoiceRecordingAndSend() {
        val activeRecorder = recorder ?: return
        val file = recordingFile
        val stopped = runCatching { activeRecorder.stop() }.isSuccess
        runCatching { activeRecorder.release() }
        recorder = null
        recordingFile = null
        isRecording = false
        if (!stopped || file == null || !file.exists()) {
            Toast.makeText(context, "Recording was too short.", Toast.LENGTH_SHORT).show()
            return
        }
        sendGroupPayload(
            text = draft.trim().ifBlank { "Voice message" },
            type = "audio",
            mediaUri = Uri.fromFile(file)
        )
    }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecording() else Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
    }

    fun sendCurrentLocation() {
        if (sending) return
        sending = true
        try {
            val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            client.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                sending = false
                if (location == null) {
                    Toast.makeText(context, "Turn on GPS and try again.", Toast.LENGTH_SHORT).show()
                } else {
                    val lat = location.latitude
                    val lng = location.longitude
                    val geoUri = "geo:$lat,$lng?q=$lat,$lng(Noor group location)"
                    sendGroupPayload(text = "Shared location", type = "location", mediaUri = null, sharedMediaUrl = geoUri)
                }
            }.addOnFailureListener { err ->
                sending = false
                Toast.makeText(context, err.localizedMessage ?: "Unable to read location.", Toast.LENGTH_SHORT).show()
            }
        } catch (securityError: SecurityException) {
            sending = false
            Toast.makeText(context, "Location permission needed.", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendCurrentLocation() else Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(group.id) {
        repository.observeGroupMessages(group.id) { list, err ->
            messages = list
            error = err
        }
        onDispose { }
    }

    DisposableEffect(group.id) {
        onDispose {
            runCatching {
                if (isRecording) recorder?.stop()
                recorder?.release()
            }
        }
    }

    val groupListState = rememberLazyListState()
    val groupChatScope = rememberCoroutineScope()
    var highlightedGroupMessageId by remember(group.id) { mutableStateOf("") }
    val displayedGroupMessages = messages.asReversed()

    fun openGroupReplyTarget(messageId: String) {
        if (messageId.isBlank()) return
        val index = displayedGroupMessages.indexOfFirst { it.id == messageId }
        if (index < 0) {
            Toast.makeText(context, "Original message is not loaded yet.", Toast.LENGTH_SHORT).show()
            return
        }
        groupChatScope.launch {
            groupListState.animateScrollToItem(index)
            highlightedGroupMessageId = messageId
            delay(1400)
            if (highlightedGroupMessageId == messageId) highlightedGroupMessageId = ""
        }
    }

    StitchScreen {
        Column(modifier = Modifier.fillMaxSize().background(stitchSurface())) {
            StitchGroupChatHeader(
                group = group,
                currentUid = currentUid,
                onBack = onBack,
                onInfo = { showGroupInfo = true },
                onAdd = {
                    openGroupInfoInAddMode = true
                    showGroupInfo = true
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(stitchSoftSurface())
            ) {
                StitchChatDottedBackground(modifier = Modifier.fillMaxSize())
                LazyColumn(
                    state = groupListState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(displayedGroupMessages, key = { it.id }) { message ->
                        val mineMsg = message.senderUid == currentUid
                        StitchChatMessageBubble(
                            message = message,
                            mine = mineMsg,
                            canDelete = mineMsg,
                            currentUid = currentUid,
                            highlighted = highlightedGroupMessageId == message.id,
                            onReply = { replyTarget = it },
                            onOpenReplyTarget = ::openGroupReplyTarget,
                            onReact = { emoji ->
                                val current = message.reactions[currentUid]
                                repository.reactToGroupMessage(group.id, message.id, if (emoji == current) "" else emoji)
                            },
                            onDelete = { repository.deleteGroupMessage(group.id, message.id) }
                        )
                    }
                    item {
                        StitchGroupConstitutionNotice()
                    }
                    item {
                        StitchChatDateChip()
                    }
                }
            }

            error?.let {
                Text(
                    it,
                    color = Color(0xFFE5484D),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (showAttachments || pendingMedia != null || isRecording) {
                StitchGroupAttachmentPanel(
                    isRecording = isRecording,
                    pendingType = pendingType,
                    pendingMedia = pendingMedia,
                    onClearPending = {
                        pendingMedia = null
                        pendingType = "text"
                    },
                    onGallery = {
                        showAttachments = true
                        imagePicker.launch("image/*")
                    },
                    onCamera = {
                        showAttachments = true
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            captureCameraPhoto()
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onVideo = {
                        showAttachments = true
                        videoPicker.launch("video/*")
                    },
                    onRecord = {
                        showAttachments = true
                        if (isRecording) {
                            stopVoiceRecordingAndSend()
                        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startVoiceRecording()
                        } else {
                            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onLocation = {
                        showAttachments = true
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            sendCurrentLocation()
                        } else {
                            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    onFile = {
                        showAttachments = true
                        filePicker.launch("*/*")
                    },
                    onPdf = {
                        showAttachments = true
                        pdfPicker.launch("application/pdf")
                    }
                )
            }

            replyTarget?.let { target ->
                StitchReplyComposerPreview(
                    target = target,
                    onCancel = { replyTarget = null },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            StitchGroupMessageComposer(
                draft = draft,
                sending = sending,
                hasPendingMedia = pendingMedia != null,
                attachmentsOpen = showAttachments,
                onDraftChange = { draft = it.take(2000) },
                onAddClick = { showAttachments = !showAttachments },
                onSend = { sendGroupPayload() }
            )
        }
    }

    if (showGroupInfo) {
        StitchGroupInfoSheet(
            group = group,
            repository = repository,
            currentUid = currentUid,
            profiles = profiles,
            people = people,
            startAddMode = openGroupInfoInAddMode,
            onOpenProfile = onOpenProfile,
            onDismiss = {
                showGroupInfo = false
                openGroupInfoInAddMode = false
            }
        )
    }
}

@Composable
private fun StitchGroupChatHeader(
    group: UmmahGroup,
    currentUid: String,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .background(stitchSurface())
            .border(BorderStroke(1.dp, StitchLine.copy(alpha = 0.75f)))
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = stitchText(),
                modifier = Modifier.size(28.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(stitchPrimary())
                .clickable(onClick = onInfo),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onInfo)
                .padding(vertical = 2.dp)
        ) {
            Text(group.name, color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${group.memberUids.size} members • tap for\ngroup info",
                color = stitchMutedText(),
                fontSize = 16.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (group.ownerUid == currentUid) {
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(5.dp))
                Text("Add", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onInfo) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Group info",
                tint = stitchPrimary(),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun StitchChatDottedBackground(modifier: Modifier = Modifier) {
    val dotColor = stitchMutedText().copy(alpha = 0.12f)
    Canvas(modifier = modifier) {
        val step = 30.dp.toPx()
        val radius = 1.1.dp.toPx()
        var y = 14.dp.toPx()
        while (y < size.height) {
            var x = 14.dp.toPx()
            while (x < size.width) {
                drawCircle(dotColor, radius = radius, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

@Composable
private fun StitchGroupConstitutionNotice() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = stitchSurface()),
            border = BorderStroke(1.dp, StitchLine.copy(alpha = 0.80f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Keep this group Islamic, educational, motivational and clean. Harmful or non-Islamic content can be removed under the App Constitution.",
                color = stitchMutedText(),
                fontSize = 16.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
private fun StitchGroupAttachmentPanel(
    isRecording: Boolean,
    pendingType: String,
    pendingMedia: Uri?,
    onClearPending: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onVideo: () -> Unit,
    onRecord: () -> Unit,
    onLocation: () -> Unit,
    onFile: () -> Unit,
    onPdf: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(stitchSurface())
            .border(BorderStroke(1.dp, StitchLine.copy(alpha = 0.75f)))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (pendingMedia != null || isRecording) {
            StitchPendingMediaPreview(
                pendingType = pendingType,
                pendingMedia = pendingMedia,
                isRecording = isRecording,
                onRemove = onClearPending
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            StitchGroupAttachmentAction(Icons.Default.Image, "Gallery", onGallery)
            StitchGroupAttachmentAction(Icons.Default.PhotoCamera, "Camera", onCamera)
            StitchGroupAttachmentAction(Icons.Default.Videocam, "Video", onVideo)
            StitchGroupAttachmentAction(
                icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                label = if (isRecording) "Send" else "Record",
                onClick = onRecord,
                active = isRecording
            )
        }
        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
            StitchGroupAttachmentAction(Icons.Default.LocationOn, "Location", onLocation)
            Spacer(Modifier.width(8.dp))
            StitchGroupAttachmentAction(Icons.Default.Article, "Files", onFile)
            Spacer(Modifier.width(8.dp))
            StitchGroupAttachmentAction(Icons.Default.LocalLibrary, "PDF", onPdf)
        }
    }
}

@Composable
private fun StitchGroupAttachmentAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) stitchPrimary() else stitchSoftSurface()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) Color.White else stitchPrimary(),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = stitchMutedText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StitchGroupMessageComposer(
    draft: String,
    sending: Boolean,
    hasPendingMedia: Boolean,
    attachmentsOpen: Boolean,
    onDraftChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onSend: () -> Unit
) {
    val canSend = draft.trim().isNotBlank() || hasPendingMedia
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(stitchSurface())
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAddClick) {
            Icon(
                if (attachmentsOpen) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (attachmentsOpen) "Close attachments" else "Add attachment",
                tint = if (attachmentsOpen) stitchPrimary() else stitchMutedText(),
                modifier = Modifier.size(32.dp)
            )
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text("Message group") },
            trailingIcon = {
                Text("☺", color = stitchMutedText(), fontSize = 21.sp)
            },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            shape = RoundedCornerShape(999.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = stitchSoftSurface(),
                unfocusedContainerColor = stitchSoftSurface(),
                disabledContainerColor = stitchSoftSurface(),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(Modifier.width(10.dp))
        Button(
            enabled = !sending && canSend,
            onClick = onSend,
            colors = ButtonDefaults.buttonColors(
                containerColor = stitchPrimary(),
                contentColor = Color.White,
                disabledContainerColor = stitchSoftSurface(),
                disabledContentColor = stitchMutedText()
            ),
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 22.dp),
            modifier = Modifier.height(58.dp)
        ) {
            if (sending) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Send", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/** WhatsApp-style group info: description, member list with live profiles, owner badge,
 *  and (for the owner) an add-members picker from following/followers. */
@Composable
private fun StitchGroupInfoSheet(
    group: UmmahGroup,
    repository: UmmahRepository,
    currentUid: String,
    profiles: Map<String, com.noorpro.app.data.UmmahProfile>,
    people: List<UmmahFollowUser>,
    startAddMode: Boolean = false,
    onOpenProfile: (uid: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isOwner = group.ownerUid == currentUid
    var addMode by remember(group.id, startAddMode) { mutableStateOf(startAddMode) }
    var selectedNew by remember { mutableStateOf<Set<String>>(emptySet()) }
    var saving by remember { mutableStateOf(false) }
    var showGroupQr by remember { mutableStateOf(false) }
    val candidates = people.filter { it.uid !in group.memberUids }

    fun memberDisplay(uid: String): Pair<String, String> {
        val profile = profiles[uid]
        val name = profile?.name?.ifBlank { null }
            ?: group.memberNames[uid]?.ifBlank { null }
            ?: if (uid == currentUid) "You" else "Member"
        return name to profile?.photoUrl.orEmpty()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = stitchSurface()),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f).navigationBarsPadding().imePadding()
            ) {
                // Explicit bottom padding: inside a Dialog window the system-bar insets can
                // report zero, which used to hide the bottom "Add member(s)" button behind
                // the gesture bar.
                Column(modifier = Modifier.fillMaxSize().padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 34.dp)) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                            .width(44.dp).height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(stitchMutedText().copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(stitchPrimary()), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.name, color = stitchText(), fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${group.memberUids.size} members", color = stitchMutedText(), fontSize = 13.sp)
                        }
                    }
                    if (group.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(group.description, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(14.dp))
                    // Actions row: Add members (owner) + Share group QR     sized side by side so
                    // nothing overlaps the member list below.
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (isOwner) {
                            Button(
                                onClick = { addMode = !addMode; selectedNew = emptySet() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (addMode) stitchSoftSurface() else stitchPrimary(),
                                    contentColor = if (addMode) stitchText() else Color.White
                                ),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(if (addMode) Icons.Default.Close else Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (addMode) "Cancel" else "Add members", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        Button(
                            onClick = { showGroupQr = true },
                            colors = ButtonDefaults.buttonColors(containerColor = stitchSoftSurface(), contentColor = stitchPrimary()),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = if (isOwner) Modifier else Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = "Group QR", modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (addMode) "Choose people to add" else "Members",
                        color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        // Big bottom clearance: dialog windows don't always report the system
                        // nav-bar inset, so the last rows/button must scroll clear of it.
                        contentPadding = PaddingValues(bottom = 84.dp)
                    ) {
                        if (addMode) {
                            if (candidates.isEmpty()) {
                                item {
                                    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(30.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("No people to add yet", color = stitchText(), fontWeight = FontWeight.Bold)
                                            Text(
                                                "Follow a user, or have them follow you, then they will appear here as a group member candidate.",
                                                color = stitchMutedText(),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                            items(candidates, key = { it.uid }) { person ->
                                val checked = person.uid in selectedNew
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (checked) stitchPrimary().copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            selectedNew = if (checked) selectedNew - person.uid else selectedNew + person.uid
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    val photo = profiles[person.uid]?.photoUrl.orEmpty()
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(StitchEmerald), contentAlignment = Alignment.Center) {
                                        if (photo.isNotBlank()) {
                                            AsyncImage(model = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                        } else {
                                            Text(person.name.take(1).uppercase().ifBlank { "N" }, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(person.name.ifBlank { "Community member" }, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(person.handle.ifBlank { "@ummah" }, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1)
                                    }
                                    Icon(
                                        if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (checked) stitchPrimary() else stitchMutedText(),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            // Confirm button scrolls WITH the list (dialog windows can't be
                            // trusted to report the nav-bar inset, so a pinned bottom button
                            // could hide behind it     this is always reachable).
                            if (candidates.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(6.dp))
                                    Button(
                                        enabled = selectedNew.isNotEmpty() && !saving,
                                        onClick = {
                                            saving = true
                                            val members = candidates.filter { it.uid in selectedNew }
                                                .associate { it.uid to it.name }
                                            repository.addGroupMembers(group.id, members) { ok, message ->
                                                saving = false
                                                if (ok) {
                                                    Toast.makeText(context, "Members added", Toast.LENGTH_SHORT).show()
                                                    addMode = false
                                                    selectedNew = emptySet()
                                                } else {
                                                    Toast.makeText(context, message ?: "Unable to add members", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                                        shape = RoundedCornerShape(999.dp),
                                        contentPadding = PaddingValues(vertical = 14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (saving) "Adding   " else "Add ${selectedNew.size} member(s)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            items(group.memberUids, key = { it }) { uid ->
                                val (name, photo) = memberDisplay(uid)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable(enabled = uid != currentUid) { onOpenProfile(uid, name) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(StitchEmerald), contentAlignment = Alignment.Center) {
                                        if (photo.isNotBlank()) {
                                            AsyncImage(model = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                        } else {
                                            Text(name.take(1).uppercase().ifBlank { "M" }, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(name, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                                    if (uid == group.ownerUid) {
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(StitchGold.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("Admin", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGroupQr) {
        val groupLink = noorGroupLink(group.id, group.name)
        val qrLink = noorAndroidIntentLink(groupLink)
        StitchQrDialog(
            title = "Group QR - ${group.name}",
            subtitle = "Share this so friends can open Noor Pro Messages and ask to join.",
            content = qrLink,
            shareText = "Join my group \"${group.name}\" on Noor Pro: $groupLink\n\nIf Noor Pro is installed, this opens Messages in the app.\nGet the app: $NoorWebBaseUrl\nHelp & contact: noorpro.official@gmail.com",
            onDismiss = { showGroupQr = false }
        )
    }

    if (false && showGroupQr) {
        StitchQrDialog(
            title = "Group QR     ${group.name}",
            subtitle = "Share this so friends can ask to join. The group admin adds members from People.",
            content = "Join my Noor Pro group \"${group.name}\"     get the app: https://noor-pro-d87e3.web.app (Group: ${group.name})",
            shareText = "Join my group \"${group.name}\" on Noor Pro!\n\nGet the app: https://noor-pro-d87e3.web.app\nThen follow me and I'll add you from Messages     People.\nHelp & contact: noorpro.official@gmail.com",
            onDismiss = { showGroupQr = false }
        )
    }
}

@Composable
fun StitchAppConstitutionScreen(viewModel: DeenViewModel) {
    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, top = 40.dp, end = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding().fillMaxWidth()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("App Constitution", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
            item { StitchConstitutionContent() }
        }
    }
}

@Composable
private fun StitchConstitutionContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Noor Pro Constitution", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "A beneficial Ummah space for Islamic learning, education, motivation, family-safe creativity and sincere reminders.",
                    color = stitchMutedText(),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }
        StitchConstitutionRule(
            "Allowed content",
            "Quran and Hadith reminders with respect, Islamic education, beneficial motivation, clean community posts, charity awareness, study groups, and family-safe creativity."
        )
        StitchConstitutionRule(
            "Not allowed",
            "Obscene material, harassment, fraud, hate, sectarian abuse, extremist propaganda, harmful misinformation, mockery of Islam, or content against Islamic adab and clean community standards."
        )
        StitchConstitutionRule(
            "Uploading content",
            "Only upload what you own or have clear permission to share. Give credit to scholars, reciters, and original creators. No copyrighted films, music, or books without rights. Keep media family-safe (no nudity, violence, or background music that contradicts Islamic adab). Arabic Quran/Hadith must be quoted accurately with correct references."
        )
        StitchConstitutionRule(
            "Sharing & reposting",
            "Share to benefit others, not for fame or fitnah. Do not spread unverified news, rumours, or chain messages. Do not screenshot or forward private chats without consent. When you share a reminder, keep the source and meaning intact     do not edit verses or narrations out of context."
        )
        StitchConstitutionRule(
            "Moderation",
            "Content against the Constitution may be removed. Repeated violations can lead to restricted posting, wiped harmful content, suspension, or account deletion."
        )
        StitchConstitutionRule(
            "Groups and popularity",
            "Groups, badges and popular placement should reward benefit, knowledge, good manners and community safety, not shock content or empty fame."
        )
        StitchConstitutionRule(
            "Privacy and security",
            "Messages and groups are private and member-only, protected by server-side security rules. Keep private conversations respectful     never share someone's private messages without their consent."
        )
        StitchConstitutionRule(
            "Religious care",
            "The app can share general reminders, but binding fatwa/rulings should be checked with qualified scholars."
        )
    }
}

@Composable
private fun StitchConstitutionRule(title: String, body: String) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(stitchSoftSurface()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(body, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
fun StitchChatScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val repository = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { repository.currentUserUid().orEmpty() }
    val otherUid = viewModel.chatTargetUid
    val otherName = viewModel.chatTargetName.ifBlank { "Member" }
    var messages by remember(otherUid) { mutableStateOf<List<UmmahMessage>>(emptyList()) }
    var error by remember(otherUid) { mutableStateOf<String?>(null) }
    var draft by remember(otherUid) { mutableStateOf("") }
    var sending by remember(otherUid) { mutableStateOf(false) }
    var pendingMedia by remember(otherUid) { mutableStateOf<Uri?>(null) }
    var pendingType by remember(otherUid) { mutableStateOf("text") }
    var cameraCaptureUri by remember(otherUid) { mutableStateOf<Uri?>(null) }
    var recorder by remember(otherUid) { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember(otherUid) { mutableStateOf<File?>(null) }
    var isRecording by remember(otherUid) { mutableStateOf(false) }
    var showAttachments by remember(otherUid) { mutableStateOf(false) }
    var replyTarget by remember(otherUid) { mutableStateOf<UmmahMessage?>(null) }
    val chatId = remember(otherUid, currentUid) {
        if (otherUid.isBlank() || currentUid.isBlank()) null else listOf(currentUid, otherUid).sorted().joinToString("_")
    }

    fun sendChatPayload(
        text: String = draft.trim(),
        type: String = pendingType,
        mediaUri: Uri? = pendingMedia,
        sharedMediaUrl: String = ""
    ) {
        if (sending) return
        if (text.isBlank() && mediaUri == null && sharedMediaUrl.isBlank()) {
            Toast.makeText(context, "Write a message or choose media.", Toast.LENGTH_SHORT).show()
            return
        }
        sending = true
        val reply = replyTarget
        repository.sendChatMessage(
            context = context,
            otherUid = otherUid,
            otherName = otherName,
            text = text,
            type = type,
            mediaUri = mediaUri,
            sharedMediaUrl = sharedMediaUrl,
            replyToMessageId = reply?.id.orEmpty(),
            replyToName = reply?.senderName.orEmpty(),
            replyToText = reply?.let { noorMessageReplyText(it) }.orEmpty(),
            replyToType = reply?.type.orEmpty()
        ) { ok, err ->
            sending = false
            if (ok) {
                draft = ""
                pendingMedia = null
                pendingType = "text"
                replyTarget = null
                showAttachments = false
            } else {
                Toast.makeText(context, err ?: "Unable to send", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingType = "image"
            pendingMedia = it
            showAttachments = true
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingType = "video"
            pendingMedia = it
            showAttachments = true
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingType = "file"
            pendingMedia = it
            showAttachments = true
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingType = "pdf"
            pendingMedia = it
            showAttachments = true
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            cameraCaptureUri?.let {
                pendingType = "image"
                pendingMedia = it
                showAttachments = true
            }
        } else {
            cameraCaptureUri = null
        }
    }

    fun captureCameraPhoto() {
        val uri = createNoorChatFileUri(context, "jpg")
        cameraCaptureUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) captureCameraPhoto() else Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_SHORT).show()
    }

    fun startVoiceRecording() {
        try {
            val file = createNoorChatCacheFile(context, "m4a")
            @Suppress("DEPRECATION")
            val newRecorder = MediaRecorder()
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
            recorder = newRecorder
            recordingFile = file
            isRecording = true
            Toast.makeText(context, "Recording voice message...", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            recorder?.release()
            recorder = null
            recordingFile = null
            isRecording = false
            Toast.makeText(context, error.localizedMessage ?: "Unable to record audio.", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVoiceRecordingAndSend() {
        val activeRecorder = recorder ?: return
        val file = recordingFile
        val stopped = runCatching { activeRecorder.stop() }.isSuccess
        runCatching { activeRecorder.release() }
        recorder = null
        recordingFile = null
        isRecording = false
        if (!stopped || file == null || !file.exists()) {
            Toast.makeText(context, "Recording was too short.", Toast.LENGTH_SHORT).show()
            return
        }
        sendChatPayload(
            text = draft.trim().ifBlank { "Voice message" },
            type = "audio",
            mediaUri = Uri.fromFile(file)
        )
    }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecording() else Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
    }

    fun sendCurrentLocation() {
        if (sending) return
        sending = true
        try {
            val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            client.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                sending = false
                if (location == null) {
                    Toast.makeText(context, "Turn on GPS and try again.", Toast.LENGTH_SHORT).show()
                } else {
                    val lat = location.latitude
                    val lng = location.longitude
                    val geoUri = "geo:$lat,$lng?q=$lat,$lng(Noor shared location)"
                    sendChatPayload(text = "Shared location", type = "location", mediaUri = null, sharedMediaUrl = geoUri)
                }
            }.addOnFailureListener { err ->
                sending = false
                Toast.makeText(context, err.localizedMessage ?: "Unable to read location.", Toast.LENGTH_SHORT).show()
            }
        } catch (securityError: SecurityException) {
            sending = false
            Toast.makeText(context, "Location permission needed.", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendCurrentLocation() else Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(chatId) {
        if (chatId != null) {
            repository.markChatRead(chatId)
            repository.observeMessages(chatId) { list, err -> messages = list; error = err }
        }
        onDispose { repository.close() }
    }

    DisposableEffect(otherUid) {
        onDispose {
            runCatching {
                if (isRecording) recorder?.stop()
                recorder?.release()
            }
        }
    }

    val chatListState = rememberLazyListState()
    val chatScope = rememberCoroutineScope()
    var highlightedChatMessageId by remember(otherUid) { mutableStateOf("") }
    val displayedChatMessages = messages.asReversed()

    fun openChatReplyTarget(messageId: String) {
        if (messageId.isBlank()) return
        val index = displayedChatMessages.indexOfFirst { it.id == messageId }
        if (index < 0) {
            Toast.makeText(context, "Original message is not loaded yet.", Toast.LENGTH_SHORT).show()
            return
        }
        chatScope.launch {
            chatListState.animateScrollToItem(index)
            highlightedChatMessageId = messageId
            delay(1400)
            if (highlightedChatMessageId == messageId) highlightedChatMessageId = ""
        }
    }

    StitchScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = { viewModel.goBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                }
                // WhatsApp-style: tap the person's name/photo to open their profile.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = otherUid.isNotBlank()) {
                            viewModel.openCreatorProfile(otherUid, otherName, "")
                        }
                        .padding(vertical = 4.dp)
                ) {
                    val otherPhoto = rememberStitchProfiles()[otherUid]?.photoUrl.orEmpty()
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(StitchEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        if (otherPhoto.isNotBlank()) {
                            AsyncImage(
                                model = otherPhoto,
                                contentDescription = "$otherName photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(otherName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(otherName, color = stitchText(), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon, maxLines = 1)
                        Text("View profile", color = stitchMutedText(), fontSize = 11.sp)
                    }
                }
            }

            if (!viewModel.isLoggedIn) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Sign in to message.", color = stitchMutedText())
                }
                return@Column
            }

            LazyColumn(
                state = chatListState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedChatMessages, key = { it.id }) { m ->
                    val mine = m.senderUid == currentUid
                    StitchChatMessageBubble(
                        message = m,
                        mine = mine,
                        canDelete = mine,
                        currentUid = currentUid,
                        highlighted = highlightedChatMessageId == m.id,
                        onReply = { replyTarget = it },
                        onOpenReplyTarget = ::openChatReplyTarget,
                        onReact = { emoji ->
                            val current = m.reactions[currentUid]
                            repository.reactToChatMessage(chatId.orEmpty(), m.id, if (emoji == current) "" else emoji)
                        },
                        onDelete = { repository.deleteChatMessage(chatId.orEmpty(), m.id) }
                    )
                }
                item {
                    StitchChatDateChip()
                }
            }
            error?.let {
                Text(it, color = Color(0xFFE5484D), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                replyTarget?.let { target ->
                    StitchReplyComposerPreview(
                        target = target,
                        onCancel = { replyTarget = null }
                    )
                }
                if (pendingMedia != null || isRecording) {
                    StitchPendingMediaPreview(
                        pendingType = pendingType,
                        pendingMedia = pendingMedia,
                        isRecording = isRecording,
                        onRemove = {
                            pendingMedia = null
                            pendingType = "text"
                        }
                    )
                }
                if (showAttachments || pendingMedia != null || isRecording) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            StitchMessageTool(Icons.Default.PhotoCamera, "Camera") {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    captureCameraPhoto()
                                } else {
                                    cameraPermission.launch(Manifest.permission.CAMERA)
                                }
                            }
                        }
                        item {
                            StitchMessageTool(Icons.Default.Image, "Image") { imagePicker.launch("image/*") }
                        }
                        item {
                            StitchMessageTool(Icons.Default.Videocam, "Video") { videoPicker.launch("video/*") }
                        }
                        item {
                            StitchMessageTool(
                                icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                label = if (isRecording) "Send voice" else "Record",
                                active = isRecording
                            ) {
                                if (isRecording) {
                                    stopVoiceRecordingAndSend()
                                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    startVoiceRecording()
                                } else {
                                    audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        item {
                            StitchMessageTool(Icons.Default.LocationOn, "Location") {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    sendCurrentLocation()
                                } else {
                                    locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            }
                        }
                        item {
                            StitchMessageTool(Icons.Default.Article, "Files") { filePicker.launch("*/*") }
                        }
                        item {
                            StitchMessageTool(Icons.Default.LocalLibrary, "PDF") { pdfPicker.launch("application/pdf") }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showAttachments = !showAttachments }) {
                        Icon(
                            if (showAttachments) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (showAttachments) "Close attachments" else "Add attachment",
                            tint = if (showAttachments) stitchPrimary() else stitchMutedText()
                        )
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(2000) },
                        placeholder = { Text("Message   ") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !sending && (draft.trim().isNotBlank() || pendingMedia != null),
                        onClick = { sendChatPayload() },
                        colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        if (sending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun noorMessageTypeLabel(type: String): String = when (type) {
    "image" -> "Photo"
    "video" -> "Video"
    "reel" -> "Reel"
    "audio" -> "Voice message"
    "location" -> "Location"
    "pdf" -> "PDF"
    "file" -> "File"
    else -> "Message"
}

private fun noorMessageReplyText(message: UmmahMessage): String =
    message.text.ifBlank { noorMessageTypeLabel(message.type) }.take(220)

@Composable
private fun StitchReplyComposerPreview(
    target: UmmahMessage,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(999.dp)).background(stitchPrimary()))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Replying to ${target.senderName.ifBlank { "message" }}",
                color = stitchPrimary(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                noorMessageReplyText(target),
                color = stitchMutedText(),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = stitchMutedText(), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StitchReplyQuote(
    replyName: String,
    replyText: String,
    mine: Boolean,
    replyMessageId: String = "",
    onClick: () -> Unit = {}
) {
    val quoteBg = if (mine) Color.White.copy(alpha = 0.16f) else stitchPrimary().copy(alpha = 0.08f)
    val quoteText = if (mine) Color.White else stitchText()
    val quoteAccent = if (mine) StitchGold else stitchPrimary()
    val clickModifier = if (replyMessageId.isNotBlank()) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(quoteBg)
            .then(clickModifier)
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(999.dp)).background(quoteAccent))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                replyName.ifBlank { "Replied message" },
                color = quoteAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                replyText,
                color = quoteText.copy(alpha = 0.86f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StitchPendingMediaPreview(
    pendingType: String,
    pendingMedia: Uri?,
    isRecording: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(stitchSoftSurface())
            .border(1.dp, StitchLine.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(68.dp).clip(RoundedCornerShape(14.dp)).background(stitchSurface()),
            contentAlignment = Alignment.Center
        ) {
            when {
                pendingMedia != null && (pendingType == "image" || pendingType == "video") -> {
                    AsyncImage(
                        model = pendingMedia,
                        contentDescription = "${pendingType.replaceFirstChar { it.uppercase() }} preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (pendingType == "video") {
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.42f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                isRecording -> Icon(Icons.Default.Mic, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(32.dp))
                pendingType == "pdf" -> Icon(Icons.Default.Article, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(34.dp))
                else -> Icon(Icons.Default.CloudQueue, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isRecording) "Recording voice message..." else "${pendingType.replaceFirstChar { it.uppercase() }} ready to send",
                color = stitchText(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (pendingType == "image" || pendingType == "video") "Preview it here. Add or edit caption below before sending." else "Add a message below before sending.",
                color = stitchMutedText(),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!isRecording) {
            Text(
                "Remove",
                color = stitchPrimary(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRemove).padding(6.dp)
            )
        }
    }
}

@Composable
private fun StitchChatDateChip() {
    val label = remember { "TODAY ${android.text.format.DateFormat.format("h:mm a", System.currentTimeMillis())}" }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            label,
            color = stitchMutedText(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFDDE3E6).copy(alpha = 0.72f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StitchChatAvatar(name: String, mine: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (mine) stitchPrimary() else Color(0xFFE3E9EB)),
        contentAlignment = Alignment.Center
    ) {
        if (name.isNotBlank()) {
            Text(
                name.take(1).uppercase(),
                color = if (mine) Color.White else stitchMutedText(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = if (mine) Color.White else stitchMutedText(),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun StitchSharedMediaCard(
    message: UmmahMessage,
    mine: Boolean,
    onOpen: () -> Unit,
    onMenu: () -> Unit
) {
    val isVideo = message.type == "video" || message.type == "reel"
    val cardBg = if (mine) stitchPrimary().copy(alpha = 0.92f) else Color(0xFFE3E9EB)
    Box(
        modifier = Modifier
            .width(232.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = message.mediaUrl,
            contentDescription = message.text.ifBlank { noorMessageTypeLabel(message.type) },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isVideo) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play video", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        if (isVideo) {
            Text(
                "Video",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.48f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun StitchMessageTool(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) stitchPrimary() else stitchSoftSurface())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) Color.White else stitchPrimary(),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (active) Color.White else stitchText(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StitchChatMessageBubble(
    message: UmmahMessage,
    mine: Boolean,
    canDelete: Boolean = false,
    currentUid: String = "",
    highlighted: Boolean = false,
    onReply: (UmmahMessage) -> Unit = {},
    onOpenReplyTarget: (String) -> Unit = {},
    onReact: (String) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val bubbleColor = if (mine) stitchPrimary() else stitchSurface()
    val textColor = if (mine) Color.White else stitchText()
    val isArabic = message.text.any { it in '\u0600'..'\u06FF' }
    var menuOpen by remember(message.id) { mutableStateOf(false) }
    var previewOpen by remember(message.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isHttpMedia = message.mediaUrl.startsWith("http")
    val hasMedia = message.mediaUrl.isNotBlank()
    val bubbleShape = RoundedCornerShape(
        topStart = if (!mine) 2.dp else 12.dp,
        topEnd = if (mine) 2.dp else 12.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )
    val highlightBorder = if (highlighted) BorderStroke(2.dp, StitchGold) else BorderStroke(0.dp, Color.Transparent)

    fun openMedia() {
        if (message.mediaUrl.isBlank()) return
        if (message.type == "location") {
            openNoorChatMediaExternal(context, message.mediaUrl)
        } else {
            previewOpen = true
        }
    }
    fun downloadMedia() {
        if (message.mediaUrl.isBlank()) return
        val extension = when (message.type) {
            "image" -> "jpg"
            "video", "reel" -> "mp4"
            "audio" -> "m4a"
            "pdf" -> "pdf"
            else -> "bin"
        }
        downloadNoorChatMedia(context, message.mediaUrl, "noor-${message.type}-${message.id.ifBlank { System.currentTimeMillis().toString() }}.$extension")
    }
    fun copyText() {
        if (message.text.isBlank()) return
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Noor message", message.text))
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }
    fun openWithChooser() {
        if (!hasMedia) return
        if (message.type == "location") {
            openNoorChatMediaExternal(context, message.mediaUrl)
        } else {
            scope.launch { openNoorChatMediaWithChooser(context, message) }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        if (!mine) {
            StitchChatAvatar(name = message.senderName, mine = false)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 292.dp)
        ) {
            if (!mine && message.senderName.isNotBlank()) {
                Text(
                    message.senderName,
                    color = stitchMutedText(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                )
            }
            Box(
                modifier = Modifier
                    .shadow(if (highlighted) 10.dp else 0.dp, bubbleShape, clip = false)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .border(highlightBorder, bubbleShape)
                    .pointerInput(message.id) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0) dragTotal += dragAmount
                            },
                            onDragEnd = {
                                if (dragTotal > 90f) onReply(message)
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f }
                        )
                    }
                    .combinedClickable(
                        onClick = { if (hasMedia) openMedia() },
                        onLongClick = { menuOpen = true }
                    )
                    .padding(if (hasMedia && message.type in listOf("image", "video", "reel")) 5.dp else 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (message.replyToText.isNotBlank() || message.replyToType.isNotBlank()) {
                        StitchReplyQuote(
                            replyName = message.replyToName,
                            replyText = message.replyToText.ifBlank { noorMessageTypeLabel(message.replyToType) },
                            mine = mine,
                            replyMessageId = message.replyToMessageId,
                            onClick = { onOpenReplyTarget(message.replyToMessageId) }
                        )
                    }
                    when {
                        message.type == "image" && hasMedia -> StitchSharedMediaCard(message = message, mine = mine, onOpen = ::openMedia, onMenu = { menuOpen = true })
                        message.type == "video" && hasMedia -> StitchSharedMediaCard(message = message, mine = mine, onOpen = ::openMedia, onMenu = { menuOpen = true })
                        message.type == "reel" && hasMedia -> StitchSharedMediaCard(message = message, mine = mine, onOpen = ::openMedia, onMenu = { menuOpen = true })
                        message.type == "audio" && hasMedia -> StitchVoiceMessagePlayer(url = message.mediaUrl, accent = textColor, mine = mine)
                        message.type == "location" && hasMedia -> StitchMessageMediaRow(Icons.Default.LocationOn, "Open shared location", textColor, hasMenu = false, onClick = ::openMedia)
                        message.type == "pdf" && hasMedia -> StitchMessageMediaRow(Icons.Default.Article, "Open PDF", textColor, hasMenu = true, onClick = ::openMedia, onMenu = { menuOpen = true })
                        message.type == "file" && hasMedia -> StitchMessageMediaRow(Icons.Default.CloudQueue, "Open file", textColor, hasMenu = true, onClick = ::openMedia, onMenu = { menuOpen = true })
                    }
                    if (message.text.isNotBlank() && !(message.type == "audio" && message.text == "Voice message")) {
                        Text(
                            message.text,
                            color = textColor,
                            fontSize = 14.sp,
                            textAlign = if (isArabic) TextAlign.End else TextAlign.Start
                        )
                    }
                    if (message.text.isBlank() && message.mediaUrl.isBlank()) {
                        Text(message.type.replaceFirstChar { it.uppercase() }, color = textColor, fontSize = 14.sp)
                    }
                    // Timestamp, right-aligned like WhatsApp.
                    val stamp = if (message.createdAt > 0) android.text.format.DateFormat.format("h:mm a", message.createdAt).toString() else ""
                    if (stamp.isNotBlank()) {
                        Text(stamp, color = textColor.copy(alpha = 0.7f), fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
                    }
                }
            }
            // Reaction chips below the bubble (emoji + count), tap to toggle your own reaction.
            if (message.reactions.isNotEmpty()) {
                val grouped = message.reactions.values.groupingBy { it }.eachCount()
                val myReaction = message.reactions[currentUid]
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    grouped.forEach { (emoji, count) ->
                        val isMine = emoji == myReaction
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isMine) stitchPrimary().copy(alpha = 0.18f) else stitchSoftSurface())
                                .clickable { onReact(emoji) }
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 12.sp)
                            if (count > 1) {
                                Spacer(Modifier.width(3.dp))
                                Text("$count", fontSize = 11.sp, color = stitchMutedText(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                // Quick emoji reaction row (WhatsApp/WeChat-style).
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("❤️", "👍", "🤲", "😊", "😢", "😮").forEach { e ->
                                Text(e, fontSize = 22.sp, modifier = Modifier.clickable { menuOpen = false; onReact(e) })
                            }
                        }
                    },
                    onClick = {}
                )
                androidx.compose.material3.DropdownMenuItem(text = { Text("Reply") }, onClick = { menuOpen = false; onReply(message) })
                if (hasMedia) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Open with...") }, onClick = { menuOpen = false; openWithChooser() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Preview in Noor") }, onClick = { menuOpen = false; openMedia() })
                }
                if (isHttpMedia) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Download") }, onClick = { menuOpen = false; downloadMedia() })
                }
                if (message.text.isNotBlank()) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Copy") }, onClick = { menuOpen = false; copyText() })
                }
                if (canDelete) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFBA1A1A), fontWeight = FontWeight.SemiBold) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
        if (mine) {
            Spacer(Modifier.width(8.dp))
            StitchChatAvatar(name = message.senderName, mine = true)
        }
    }

    if (previewOpen) {
        StitchMessageMediaPreviewDialog(
            message = message,
            onDismiss = { previewOpen = false },
            onDownload = { downloadMedia() },
            onOpenExternal = { openNoorChatMediaExternal(context, message.mediaUrl) }
        )
    }
}

private fun openNoorChatMediaExternal(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "No app found to open this message.", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun openNoorChatMediaWithChooser(context: Context, message: UmmahMessage) {
    if (message.mediaUrl.isBlank()) return
    runCatching {
        val (uri, mimeType) = cacheNoorChatMediaForOpen(context, message)
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            clipData = ClipData.newUri(context.contentResolver, "Noor message media", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(openIntent, "Open with"))
    }.onFailure {
        Toast.makeText(context, it.localizedMessage ?: "No app found to open this file.", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun cacheNoorChatMediaForOpen(context: Context, message: UmmahMessage): Pair<Uri, String> {
    val mimeType = noorChatMimeFor(message.type)
    if (!message.mediaUrl.startsWith("http")) return Uri.parse(message.mediaUrl) to mimeType
    return withContext(Dispatchers.IO) {
        val safeId = message.id.ifBlank { message.mediaUrl.hashCode().toString() }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(context.cacheDir, "noor-open-$safeId.${noorChatExtensionFor(message.type)}")
        if (!file.exists() || file.length() == 0L) {
            URL(message.mediaUrl).openStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        uri to mimeType
    }
}

private fun noorChatExtensionFor(type: String): String = when (type) {
    "image" -> "jpg"
    "video", "reel" -> "mp4"
    "audio" -> "m4a"
    "pdf" -> "pdf"
    else -> "bin"
}

private fun noorChatMimeFor(type: String): String = when (type) {
    "image" -> "image/*"
    "video", "reel" -> "video/*"
    "audio" -> "audio/*"
    "pdf" -> "application/pdf"
    else -> "*/*"
}

@Composable
private fun StitchMessageMediaPreviewDialog(
    message: UmmahMessage,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var openingExternal by remember(message.id) { mutableStateOf(false) }
    fun openWithChooser() {
        if (openingExternal) return
        scope.launch {
            openingExternal = true
            openNoorChatMediaWithChooser(context, message)
            openingExternal = false
        }
    }
    val title = when (message.type) {
        "image" -> "Photo"
        "video" -> "Video"
        "reel" -> "Reel"
        "pdf" -> "PDF"
        "file" -> "File"
        else -> "Attachment"
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDownload) {
                    Text("Download", color = StitchGold, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { openWithChooser() }) {
                    Text(if (openingExternal) "Preparing..." else "Open", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (message.type) {
                    "image" -> {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = message.text.ifBlank { "Photo message" },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "video", "reel" -> {
                        StitchInlinePostVideoPlayer(
                            mediaUrl = message.mediaUrl,
                            contentDescription = message.text.ifBlank { "Video message" },
                            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                        )
                    }
                    "pdf" -> {
                        StitchPdfMessagePreview(url = message.mediaUrl, modifier = Modifier.fillMaxSize())
                    }
                    else -> {
                        StitchGenericFilePreview(
                            title = message.text.ifBlank { "File attachment" },
                            onDownload = onDownload,
                            onOpenExternal = { openWithChooser() }
                        )
                    }
                }
            }

            if (message.text.isNotBlank() && message.type != "file" && message.type != "pdf") {
                Text(
                    message.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.92f)).padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun StitchPdfMessagePreview(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var file by remember(url) { mutableStateOf<File?>(null) }
    var error by remember(url) { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        error = null
        file = null
        runCatching {
            withContext(Dispatchers.IO) {
                val target = File(context.cacheDir, "noor-message-${url.hashCode().toString().replace("-", "m")}.pdf")
                if (!target.exists() || target.length() == 0L) {
                    URL(url).openStream().use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                target
            }
        }.onSuccess {
            file = it
        }.onFailure {
            error = it.localizedMessage ?: "Unable to load PDF."
        }
    }

    Box(modifier = modifier.background(Color.White), contentAlignment = Alignment.Center) {
        when {
            file != null -> PdfViewer(file = file!!, initialPage = 0, onPageChanged = { _, _ -> }, modifier = Modifier.fillMaxSize())
            error != null -> Text(error.orEmpty(), color = Color(0xFFBA1A1A), modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center)
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = stitchPrimary())
                Spacer(Modifier.height(14.dp))
                Text("Loading PDF...", color = stitchText(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StitchGenericFilePreview(
    title: String,
    onDownload: () -> Unit,
    onOpenExternal: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(82.dp).clip(RoundedCornerShape(24.dp)).background(stitchSoftSurface()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Article, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Preview is not available for this file type. You can download it or open it with another app.",
            color = stitchMutedText(),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onDownload,
            colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onOpenExternal, modifier = Modifier.fillMaxWidth()) {
            Text("Open in another app", color = stitchPrimary(), fontWeight = FontWeight.Bold)
        }
    }
}

private fun downloadNoorChatMedia(context: Context, url: String, fileName: String) {
    if (!url.startsWith("http")) {
        Toast.makeText(context, "This item opens from its shared location.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading Noor Pro message media")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, it.localizedMessage ?: "Unable to download this file.", Toast.LENGTH_SHORT).show()
    }
}

/** Inline voice-message player (WhatsApp/Instagram-style): plays in the bubble, never leaves the chat. */
@Composable
private fun StitchVoiceMessagePlayer(url: String, accent: Color, mine: Boolean) {
    val context = LocalContext.current
    var playing by remember(url) { mutableStateOf(false) }
    var durationMs by remember(url) { mutableStateOf(0) }
    var positionMs by remember(url) { mutableStateOf(0) }
    val player = remember(url) { MediaPlayer() }

    DisposableEffect(url) {
        runCatching {
            player.setDataSource(url)
            player.setOnPreparedListener { durationMs = it.duration }
            player.setOnCompletionListener {
                playing = false
                positionMs = 0
                runCatching { it.seekTo(0) }
            }
            player.prepareAsync()
        }
        onDispose { runCatching { player.release() } }
    }

    LaunchedEffect(playing) {
        while (playing) {
            positionMs = runCatching { player.currentPosition }.getOrDefault(positionMs)
            if (durationMs == 0) durationMs = runCatching { player.duration }.getOrDefault(0)
            delay(150)
        }
    }

    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val track = if (mine) Color.White.copy(alpha = 0.35f) else stitchMutedText().copy(alpha = 0.35f)
    fun fmt(ms: Int): String {
        val total = ms / 1000
        return "%d:%02d".format(total / 60, total % 60)
    }

    Row(
        modifier = Modifier.widthIn(min = 180.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (mine) Color.White.copy(alpha = 0.2f) else stitchPrimary().copy(alpha = 0.14f))
                .clickable {
                    if (playing) {
                        runCatching { player.pause() }
                        playing = false
                    } else {
                        runCatching { player.start() }
                        playing = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause voice message" else "Play voice message",
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = accent.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Voice message", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
                color = accent,
                trackColor = track
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (positionMs > 0) "${fmt(positionMs)} / ${fmt(durationMs)}" else fmt(durationMs),
                color = accent.copy(alpha = 0.75f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun StitchMessageMediaRow(
    icon: ImageVector,
    label: String,
    textColor: Color,
    hasMenu: Boolean = false,
    onClick: () -> Unit,
    onMenu: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = textColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (hasMenu) {
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = textColor,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable(onClick = onMenu)
            )
        }
    }
}

// ---- Spiritual Progress (prayer tracker) -------------------------------------------------

@Composable
fun StitchSpiritualProgressScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val prayers by viewModel.prayers.collectAsState()
    val todayLog by viewModel.todayPrayerLog.collectAsState()
    val weeklyLogs by viewModel.weeklyPrayerLogs.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()

    val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    fun fardDone(name: String) = when (name) {
        "Fajr" -> todayLog?.fajr == true
        "Dhuhr" -> todayLog?.dhuhr == true
        "Asr" -> todayLog?.asr == true
        "Maghrib" -> todayLog?.maghrib == true
        "Isha" -> todayLog?.isha == true
        else -> false
    }
    val timeFor = remember(prayers) { prayers.associate { it.name to it.time } }

    // Sunnah completion is stored locally (SharedPreferences)     Fard uses the real Room tracker.
    val prefs = remember { context.getSharedPreferences("noor_sunnah", android.content.Context.MODE_PRIVATE) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var sunnahMode by remember { mutableStateOf(prefs.getBoolean("sunnah_mode", false)) }
    var sunnahToday by remember { mutableStateOf(prefs.getStringSet("sunnah_$todayStr", emptySet()).orEmpty().toSet()) }
    fun toggleSunnah(name: String) {
        val next = sunnahToday.toMutableSet().apply { if (contains(name)) remove(name) else add(name) }
        sunnahToday = next
        prefs.edit().putStringSet("sunnah_$todayStr", next).apply()
    }

    val weekDates = remember { currentWeekDates() }
    // "Completion" reflects today's five fard prayers.
    val todayDone = listOf(
        todayLog?.fajr == true, todayLog?.dhuhr == true, todayLog?.asr == true,
        todayLog?.maghrib == true, todayLog?.isha == true
    ).count { it }
    val completionPct = todayDone * 100 / 5
    // Weekly yield is out of the full week (5 fard prayers x 7 days).
    val weeklyPossible = 35
    val fardWeek = weeklyLogs.sumOf { listOf(it.fajr, it.dhuhr, it.asr, it.maghrib, it.isha).count { d -> d } }
        .coerceAtMost(weeklyPossible)
    val sunnahWeek = weekDates.sumOf { d -> prefs.getStringSet("sunnah_$d", emptySet()).orEmpty().size }
        .coerceAtMost(weeklyPossible)
    val fardFrac = (fardWeek.toFloat() / weeklyPossible).coerceIn(0f, 1f)
    val sunnahFrac = (sunnahWeek.toFloat() / weeklyPossible).coerceIn(0f, 1f)
    val avgScore = if (sunnahMode) (((fardFrac + sunnahFrac) / 2f) * 100).toInt() else (fardFrac * 100).toInt()

    val bestPrayer = remember(weeklyLogs) {
        mapOf(
            "Fajr" to weeklyLogs.count { it.fajr },
            "Dhuhr" to weeklyLogs.count { it.dhuhr },
            "Asr" to weeklyLogs.count { it.asr },
            "Maghrib" to weeklyLogs.count { it.maghrib },
            "Isha" to weeklyLogs.count { it.isha }
        ).maxByOrNull { it.value }?.key ?: "Fajr"
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding().fillMaxWidth()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Text("Al-Noor", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = stitchText(), modifier = Modifier.size(24.dp))
                }
            }
            item {
                Column {
                    Text("Spiritual Progress", color = stitchText(), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "May your devotion bring you peace. Here is your journey through the sacred times of prayer.",
                        color = stitchMutedText(), fontSize = 14.sp, lineHeight = 20.sp
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    SpiritualStatCard(Modifier.weight(1f), Icons.Default.LocalFireDepartment, StitchGold, "$streak Days", "Current Streak")
                    SpiritualStatCard(Modifier.weight(1f), Icons.Default.CheckCircle, StitchEmerald, "$completionPct%", "Completion")
                }
            }
            item { SpiritualCalendar() }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Checklist", color = stitchText(), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Sunnah Mode", color = stitchMutedText(), fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = sunnahMode,
                        onCheckedChange = { sunnahMode = it; prefs.edit().putBoolean("sunnah_mode", it).apply() }
                    )
                }
            }
            items(prayerNames) { name ->
                SpiritualPrayerRow(
                    name = name,
                    time = formatPrayerClock(timeFor[name].orEmpty()),
                    fardDone = fardDone(name),
                    sunnahMode = sunnahMode,
                    sunnahDone = name in sunnahToday,
                    onFard = { viewModel.toggleDailyPrayer(name) },
                    onSunnah = { toggleSunnah(name) }
                )
            }
            item {
                SpiritualWeeklyYield(
                    fardFrac = fardFrac,
                    sunnahFrac = sunnahFrac,
                    avgScore = avgScore,
                    fardLabel = "$fardWeek/$weeklyPossible",
                    sunnahLabel = "$sunnahWeek/$weeklyPossible",
                    sunnahMode = sunnahMode,
                    bestPrayer = bestPrayer
                )
            }
        }
    }
}

@Composable
private fun SpiritualStatCard(modifier: Modifier, icon: ImageVector, iconColor: Color, value: String, label: String) {
    StitchCard(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = stitchMutedText(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SpiritualCalendar() {
    var weekOffset by remember { mutableStateOf(0) }
    val monthLabel = remember(weekOffset) {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, weekOffset * 7) }
        SimpleDateFormat("MMMM yyyy", Locale.US).format(c.time)
    }
    val days = remember(weekOffset) {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, weekOffset * 7) }
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val now = Calendar.getInstance()
        (0..6).map {
            val day = c.get(Calendar.DAY_OF_MONTH)
            val isToday = isSameDay(c, now)
            c.add(Calendar.DAY_OF_YEAR, 1)
            day to isToday
        }
    }
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(monthLabel, color = stitchText(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { weekOffset-- }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week", tint = stitchMutedText())
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { weekOffset++ }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next week", tint = stitchMutedText())
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(it, color = stitchMutedText(), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEach { (day, isToday) ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                .background(if (isToday) StitchEmerald else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$day",
                                color = if (isToday) Color.White else stitchText(),
                                fontSize = 14.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpiritualPrayerRow(
    name: String,
    time: String,
    fardDone: Boolean,
    sunnahMode: Boolean,
    sunnahDone: Boolean,
    onFard: () -> Unit,
    onSunnah: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape)
                    .background(if (fardDone) StitchEmerald else Color.Transparent)
                    .border(if (fardDone) 0.dp else 2.dp, if (fardDone) StitchEmerald else StitchLine, CircleShape)
                    .clickable(onClick = onFard),
                contentAlignment = Alignment.Center
            ) {
                if (fardDone) Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(time, color = stitchMutedText(), fontSize = 12.sp)
            }
            if (sunnahMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (sunnahDone) StitchGold.copy(alpha = 0.18f) else stitchSoftSurface())
                        .clickable(onClick = onSunnah)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        if (sunnahDone) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Sunnah",
                        tint = if (sunnahDone) StitchGold else stitchMutedText(),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sunnah", color = if (sunnahDone) StitchGold else stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SpiritualWeeklyYield(
    fardFrac: Float,
    sunnahFrac: Float,
    avgScore: Int,
    fardLabel: String,
    sunnahLabel: String,
    sunnahMode: Boolean,
    bestPrayer: String
) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Weekly Yield", color = stitchText(), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                SpiritualDonut(fardFrac, sunnahFrac, sunnahMode)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$avgScore%", color = stitchText(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Avg. Score", color = stitchMutedText(), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            SpiritualYieldLegend(StitchEmerald, "Fard Prayers", fardLabel, stitchText())
            Spacer(modifier = Modifier.height(10.dp))
            if (sunnahMode) {
                SpiritualYieldLegend(StitchGold, "Sunnah Acts", sunnahLabel, StitchGold)
            } else {
                SpiritualYieldLegend(StitchGold, "Sunnah Acts", "Turn on Sunnah Mode", StitchGold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(StitchEmerald.copy(alpha = 0.07f)).padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StitchEmerald, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "You're most consistent during $bestPrayer this week. Keep up the steady devotion!",
                    color = stitchText(), fontSize = 13.sp, lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun SpiritualYieldLegend(dot: Color, label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = stitchText(), fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpiritualDonut(fardFrac: Float, sunnahFrac: Float, sunnahMode: Boolean) {
    val track = stitchSoftSurface()
    Canvas(modifier = Modifier.size(170.dp)) {
        val stroke = 16.dp.toPx()
        val inset = stroke / 2f
        val outerSize = Size(size.width - stroke, size.height - stroke)
        val outerTopLeft = Offset(inset, inset)
        drawArc(track, -90f, 360f, false, topLeft = outerTopLeft, size = outerSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(StitchEmerald, -90f, 360f * fardFrac, false, topLeft = outerTopLeft, size = outerSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (sunnahMode) {
            val gap = stroke * 1.4f
            val innerStroke = 13.dp.toPx()
            val innerInset = inset + gap
            val innerSize = Size(size.width - stroke - 2f * gap, size.height - stroke - 2f * gap)
            val innerTopLeft = Offset(innerInset, innerInset)
            drawArc(track, -90f, 360f, false, topLeft = innerTopLeft, size = innerSize, style = Stroke(innerStroke, cap = StrokeCap.Round))
            drawArc(StitchGold, -90f, 360f * sunnahFrac, false, topLeft = innerTopLeft, size = innerSize, style = Stroke(innerStroke, cap = StrokeCap.Round))
        }
    }
}

private fun formatPrayerClock(hhmm: String): String {
    if (hhmm.isBlank() || !hhmm.contains(":")) return hhmm
    return try {
        val parser = SimpleDateFormat("HH:mm", Locale.US)
        val out = SimpleDateFormat("h:mm a", Locale.US)
        out.format(parser.parse(hhmm)!!)
    } catch (_: Exception) {
        hhmm
    }
}

private fun currentWeekDates(): List<String> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val c = Calendar.getInstance()
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    return (0..6).map { val s = fmt.format(c.time); c.add(Calendar.DAY_OF_YEAR, 1); s }
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

