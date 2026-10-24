package com.noorpro.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.noorpro.app.R
import com.noorpro.app.data.PrayerLogEntity
import com.noorpro.app.data.PrayerTime
import com.noorpro.app.data.QuranLastRead
import com.noorpro.app.data.Surah
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
 * 1.0.99 Home: header + next-prayer hero, Daily essentials, Continue reading, Explore your day,
 * Ask Noor AI, Al-Noor Audio, Today's devotion and Daily reflection — all from real app data.
 */

private val HomeMint = Color(0xFFE6F2EC)
private val HomeMintDark = Color(0xFF173A2F)
private val HomeReflection = Color(0xFF0B4A36)

@Composable private fun homeMint(): Color = if (isStitchLight()) HomeMint else HomeMintDark
@Composable private fun homeAccent(): Color = if (isStitchLight()) StitchEmerald else Color(0xFF7FD3AE)

/** Local, per-device day streak: consecutive days the app's Home was opened. */
internal object HomeDayStreak {
    private const val PREFS = "home_day_streak"
    private fun day(cal: Calendar) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    /** Records today's visit and returns the current streak (1 on first use or after a gap). */
    fun recordVisit(ctx: Context, now: Calendar = Calendar.getInstance()): Int {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = day(now)
        val last = p.getString("last_day", null)
        val prev = p.getInt("streak", 0)
        if (last == today && prev > 0) return prev
        val y = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val next = if (last == day(y)) prev + 1 else 1
        p.edit().putString("last_day", today).putInt("streak", next).apply()
        return next
    }
}

/** Unread dot on the bell: shown until the reminders/notifications page is opened today,
 *  or while prayer notifications still need permission. */
internal object HomeBell {
    private const val PREFS = "home_bell"
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    fun hasUnread(ctx: Context): Boolean {
        val seen = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("seen_day", null) == today()
        val notifOk = android.os.Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        return !seen || !notifOk
    }
    fun markSeen(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("seen_day", today()).apply()
    }
}

/** Time source for Home (JVM previews pin it; the app always uses the real clock). */
internal object HomeClock {
    @Volatile var nowOverride: Long? = null
    fun now(): Long = nowOverride ?: System.currentTimeMillis()
}

/** Hero pill text, e.g. "12 Jumada al-Awwal · 1448 AH", honouring the user's Hijri adjustment. */
internal fun homeHijriLabel(adjustment: Int, nowMillis: Long = HomeClock.now()): String = runCatching {
    val cal = android.icu.util.IslamicCalendar()
    cal.timeInMillis = nowMillis
    cal.add(android.icu.util.Calendar.DAY_OF_MONTH, adjustment)
    val months = listOf("Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhul-Qadah", "Dhul-Hijjah")
    "${cal.get(android.icu.util.Calendar.DAY_OF_MONTH)} ${months.getOrElse(cal.get(android.icu.util.Calendar.MONTH)) { "" }} · ${cal.get(android.icu.util.Calendar.YEAR)} AH"
}.getOrDefault("")

internal fun prayersCompleted(log: PrayerLogEntity?): Int =
    if (log == null) 0 else listOf(log.fajr, log.dhuhr, log.asr, log.maghrib, log.isha).count { it }

/** "So remember Me; I will remember you. (2:152)" -> ("So remember Me; I will remember you.", "2:152"). */
internal fun splitAyahReference(quote: String): Pair<String, String> {
    val m = Regex("""^(.*?)\s*\((\d{1,3}:\d{1,3}(?:-\d{1,3})?)\)\s*$""").find(quote.trim())
    return if (m != null) m.groupValues[1].trim() to m.groupValues[2] else quote.trim() to ""
}

internal fun capitalizedCountdown(countdown: String): String =
    friendlyCountdown(countdown).replaceFirstChar { it.uppercase() }

@Composable
fun NoorHomeScreen(viewModel: DeenViewModel) {
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
    val todayLog by viewModel.todayPrayerLog.collectAsState()
    val hijriAdjustment by viewModel.hijriAdjustment.collectAsState()
    val firstName = viewModel.userDisplayName.trim().split(" ").firstOrNull().orEmpty()
        .takeIf { viewModel.isLoggedIn }.orEmpty()
    val bottomInset = if (playingSurah != null || currentAudioTrack != null) 260.dp else 150.dp
    val streak = remember { HomeDayStreak.recordVisit(context, Calendar.getInstance().apply { timeInMillis = HomeClock.now() }) }
    var bellUnread by remember { mutableStateOf(HomeBell.hasUnread(context)) }
    val hasSavedRead = remember(lastReadSurah, lastReadAyah) { QuranLastRead.load(context) != null }

    val locationSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.checkLocationAndRefresh { }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            if (com.noorpro.app.data.DeviceLocationProvider.isLocationEnabled(context)) viewModel.checkLocationAndRefresh { }
            else locationSettingsLauncher.launch(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
    val requestLocation: () -> Unit = {
        if (com.noorpro.app.data.DeviceLocationProvider.hasPermission(context)) {
            if (com.noorpro.app.data.DeviceLocationProvider.isLocationEnabled(context)) viewModel.checkLocationAndRefresh { }
            else locationSettingsLauncher.launch(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    val openBell = {
        HomeBell.markSeen(context); bellUnread = HomeBell.hasUnread(context)
        viewModel.navigateTo(DeenScreen.SETTINGS)
    }
    val askNoor: (String) -> Unit = { q ->
        NoorAiLaunch.pending = q.trim().ifBlank { null }
        viewModel.navigateTo(DeenScreen.AI_HUB)
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val systemNavInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val compactBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    StitchScreen {
        Box(Modifier.fillMaxSize().testTag("noor_home")) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("home_list"),
                contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = bottomInset + systemNavInset),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "header") {
                    HomeHeader(firstName, viewModel.userPhotoUrl, bellUnread,
                        onProfile = { viewModel.navigateTo(DeenScreen.PROFILE_DASHBOARD) }, onBell = openBell)
                }
                item(key = "hero") {
                    HomePrayerHero(prayers, nextPrayerName, nextPrayerCountdown, location, hasLocation, hijriAdjustment,
                        viewModel::displayPrayerTime, requestLocation)
                }
                item(key = "essentials") {
                    Column {
                        PermissionNudgeBanner(viewModel, Modifier.padding(bottom = 14.dp))
                        HomeDailyEssentials(onTool = { viewModel.navigateTo(it) }, onViewAll = { viewModel.navigateTo(DeenScreen.EXPLORE) })
                    }
                }
                item(key = "continue") {
                    HomeContinueReading(lastReadSurah, lastReadAyah, hasSavedRead) { viewModel.resumeReading(lastReadSurah, lastReadAyah) }
                }
                item(key = "explore") {
                    HomeExploreRow { scope.launch { listState.animateScrollToItem(5) } }
                }
                item(key = "ask") { HomeAskNoorCard(onAsk = askNoor) }
                item(key = "audio") {
                    // Sponsor + inline ad live with the audio banner so a hidden sponsor / unfilled ad adds no gap.
                    Column {
                        com.noorpro.app.sponsor.SponsorCardSlot("home", Modifier.padding(bottom = 14.dp))
                        HomeAudioBanner { viewModel.navigateTo(DeenScreen.AUDIO_LIBRARY) }
                        StitchHomeSponsoredAdCard(Modifier.padding(top = 14.dp))
                    }
                }
                item(key = "devotion") {
                    HomeDevotionCard(prayersCompleted(todayLog), lastReadSurah.nameEnglish, streak) {
                        viewModel.navigateTo(DeenScreen.SPIRITUAL_PROGRESS)
                    }
                }
                item(key = "reflection") {
                    HomeReflectionCard(dailyQuote.first) {
                        viewModel.navigateTo(if (viewModel.isLoggedIn) DeenScreen.UMMAH_CREATE_POST else DeenScreen.UMMAH)
                    }
                }
            }
            // Compact top bar once the header scrolls away ("Home · continued").
            AnimatedVisibility(visible = compactBar, enter = fadeIn(tween(180)), exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.TopCenter)) {
                Row(
                    Modifier.fillMaxWidth().background(stitchBackground().copy(alpha = 0.97f)).statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 8.dp).testTag("home_compact_bar"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NoorPro", color = homeAccent(), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon,
                        modifier = Modifier.weight(1f))
                    HomeBellButton(bellUnread, openBell)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(firstName: String, photoUrl: String, unread: Boolean, onProfile: () -> Unit, onBell: () -> Unit) {
    Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF8A6A5A)).clickable(onClick = onProfile)
                .testTag("home_avatar"),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(model = photoUrl, contentDescription = "My profile", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            } else if (firstName.isNotBlank()) {
                Text(firstName.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Default.Person, "Profile", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("NoorPro", color = homeAccent(), fontSize = 27.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, lineHeight = 30.sp)
            Text(if (firstName.isBlank()) "As-salamu alaykum" else "As-salamu alaykum, $firstName",
                color = stitchMutedText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HomeBellButton(unread, onBell)
    }
}

@Composable
private fun HomeBellButton(unread: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(stitchSurface()).border(1.dp, stitchLine(), CircleShape)
            .clickable(onClick = onClick).testTag("home_bell"),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Notifications, "Notifications and reminders", tint = homeAccent(), modifier = Modifier.size(21.dp))
        if (unread) Box(Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 10.dp).size(8.dp).clip(CircleShape)
            .background(StitchGold).border(1.5.dp, stitchSurface(), CircleShape).testTag("home_bell_dot"))
    }
}

@Composable
private fun HomePrayerHero(
    prayers: List<PrayerTime>, nextPrayerName: String, countdown: String, location: String, hasLocation: Boolean,
    hijriAdjustment: Int, displayTime: (String) -> String, onLocationClick: () -> Unit
) {
    val current = prayers.firstOrNull { it.name == nextPrayerName } ?: prayers.firstOrNull()
    val time = current?.let { displayTime(it.time) } ?: "--:--"
    val parts = time.trim().split(" ")
    val clock = parts.getOrElse(0) { time }
    val meridiem = parts.getOrElse(1) { "" }
    Box(
        Modifier.fillMaxWidth().height(206.dp).clip(RoundedCornerShape(26.dp)).background(StitchEmeraldDeep).testTag("home_hero")
    ) {
        Image(painterResource(R.drawable.noor_home_hero_mosque), null, contentScale = ContentScale.Crop,
            alignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color(0xFF062F24).copy(alpha = 0.30f), Color(0xFF062F24).copy(alpha = 0.25f), Color(0xFF041F18).copy(alpha = 0.88f)))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF041F18).copy(alpha = 0.35f)))))
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(homeHijriLabel(hijriAdjustment).ifBlank { "Hijri date" }, color = Color.White, fontSize = 12.sp, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (hasLocation) "NEXT PRAYER" else "PRAYER TIMES", color = StitchGold, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                    Text(if (hasLocation) nextPrayerName.ifBlank { "—" } else "Location", color = Color.White, fontSize = 34.sp,
                        fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, maxLines = 1, lineHeight = 38.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onLocationClick).padding(vertical = 2.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (hasLocation) location.ifBlank { "Location updating" } else "Enable location", color = Color.White,
                            fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (hasLocation) FontWeight.Normal else FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    val motion = animationsEnabled()
                    AnimatedContent(
                        targetState = if (hasLocation) capitalizedCountdown(countdown) else "Tap to set your location",
                        transitionSpec = {
                            if (motion) (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(160)))
                            else EnterTransition.None togetherWith ExitTransition.None
                        }, label = "homeCountdown"
                    ) { txt -> Text(txt, color = StitchGold, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.testTag("home_countdown")) }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    val size = if (clock.length > 4) 42.sp else 46.sp
                    Text(clock, color = Color.White, fontSize = size, fontWeight = FontWeight.SemiBold, lineHeight = size, maxLines = 1, softWrap = false)
                    if (meridiem.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Text(meridiem.uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
        }
    }
}

private data class HomeTool(val label: String, val icon: ImageVector, val screen: DeenScreen)

@Composable
private fun HomeDailyEssentials(onTool: (DeenScreen) -> Unit, onViewAll: () -> Unit) {
    val tools = listOf(
        HomeTool("Quran", Icons.AutoMirrored.Filled.MenuBook, DeenScreen.QURAN),
        HomeTool("Tasbih", TasbihBeadsIcon, DeenScreen.TASBIH),
        HomeTool("Qibla", Icons.Default.Explore, DeenScreen.QIBLA_MORE),
        HomeTool("Dua", DuaHandsIcon, DeenScreen.DUA_HUB),
    )
    StitchCard(Modifier.fillMaxWidth().testTag("home_essentials"), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily essentials", color = stitchText(), fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon,
                    modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onViewAll).padding(horizontal = 6.dp, vertical = 4.dp).testTag("home_view_all")) {
                    Text("View all", color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = stitchText(), modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tools.forEach { t ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onTool(t.screen) },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(homeMint()),
                            contentAlignment = Alignment.Center) {
                            Icon(t.icon, t.label, tint = homeAccent(), modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(t.label, color = stitchText(), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContinueReading(surah: Surah, ayah: Int, hasSavedRead: Boolean, onContinue: () -> Unit) {
    val total = surah.versesCount.coerceAtLeast(1)
    val progress = if (surah.id > 0) (ayah.toFloat() / total).coerceIn(0.04f, 1f) else 0.1f
    val animated by animateFloatAsState(progress, tween(if (animationsEnabled()) 700 else 0), label = "readProgress")
    StitchCard(Modifier.fillMaxWidth().testTag("home_continue"), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(if (hasSavedRead) "CONTINUE READING" else "START READING", color = stitchMutedText(), fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(6.dp))
                Text(surah.nameEnglish, color = stitchText(), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 34.sp)
                Text(listOf(surah.englishTranslation, "Ayah $ayah").filter { it.isNotBlank() }.joinToString(" · "),
                    color = stitchMutedText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)).background(stitchLine())) {
                    Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(StitchGold))
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(StitchEmerald).clickable(onClick = onContinue)
                        .padding(horizontal = 22.dp, vertical = 11.dp).testTag("home_continue_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (hasSavedRead) "Continue" else "Start", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
            Image(painterResource(R.drawable.noor_home_quran_rehal), "Quran on a rehal", contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 142.dp, height = 138.dp).clip(RoundedCornerShape(18.dp)))
        }
    }
}

@Composable
private fun HomeExploreRow(onClick: () -> Unit) {
    StitchCard(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("home_explore"), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(homeMint()), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = homeAccent(), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Explore your day", color = stitchMutedText(), fontSize = 12.sp)
                Text("Noor AI, audio & reflection", color = stitchText(), fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
            }
            Icon(Icons.Default.ChevronRight, null, tint = stitchText())
        }
    }
}

@Composable
private fun HomeAskNoorCard(onAsk: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    StitchCard(Modifier.fillMaxWidth().testTag("home_ask_ai"), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(homeMint()), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = homeAccent(), modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Ask Noor AI", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
                    Text("Islamic questions, with references", color = stitchMutedText(), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(stitchSoftSurface())
                    .padding(start = 14.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = stitchMutedText(), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Ask about salah, Quran, duas…", color = stitchMutedText(), fontSize = 14.sp, maxLines = 1)
                    BasicTextField(
                        value = query, onValueChange = { query = it.take(500) }, singleLine = true,
                        textStyle = TextStyle(color = stitchText(), fontSize = 14.sp),
                        cursorBrush = SolidColor(homeAccent()),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onAsk(query); query = "" }),
                        modifier = Modifier.fillMaxWidth().testTag("home_ask_field")
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(38.dp).clip(CircleShape).background(StitchEmerald).clickable { onAsk(query); query = "" }
                    .testTag("home_ask_send"), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Ask Noor AI", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Focus in salah" to "How can I improve focus in salah?", "What breaks wudu?" to "What breaks wudu?",
                    "Daily Quran habit" to "How do I build a daily Quran habit?")) { (label, q) ->
                    Text(label, color = stitchText(), fontSize = 13.sp, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, stitchLine(), RoundedCornerShape(999.dp))
                            .clickable { onAsk(q) }.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeAudioBanner(onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF0B4A36), StitchEmerald)))
            .clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 18.dp).testTag("home_audio"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE0B54A)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = StitchEmeraldDeep, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Al-Noor Audio", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
            Text("Recitations, nasheeds & duas", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronRight, "Open Al-Noor Audio", tint = Color.White)
    }
}

@Composable
private fun HomeDevotionCard(completed: Int, surahName: String, streak: Int, onOpen: () -> Unit) {
    StitchCard(Modifier.fillMaxWidth().clickable(onClick = onOpen).testTag("home_devotion"), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Text("Today’s devotion", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DevotionIcon(Icons.Default.Mosque)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Prayers completed", color = stitchText(), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("$completed / 5", color = stitchText(), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("home_prayers_done"))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { i ->
                            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp))
                                .background(if (i < completed) homeAccent() else stitchLine()))
                        }
                    }
                }
            }
            DevotionDivider()
            DevotionRow(Icons.AutoMirrored.Filled.MenuBook, "Quran reading", surahName)
            DevotionDivider()
            DevotionRow(Icons.Default.CalendarMonth, "Day streak", if (streak == 1) "1 day" else "$streak days")
        }
    }
}

@Composable private fun DevotionDivider() =
    Box(Modifier.padding(start = 48.dp, top = 12.dp, bottom = 12.dp).fillMaxWidth().height(1.dp).background(stitchLine()))

@Composable
private fun DevotionIcon(icon: ImageVector) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(homeMint()), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = homeAccent(), modifier = Modifier.size(19.dp))
    }
    Spacer(Modifier.width(14.dp))
}

@Composable
private fun DevotionRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DevotionIcon(icon)
        Text(label, color = stitchText(), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = homeAccent(), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun HomeReflectionCard(quote: String, onReflect: () -> Unit) {
    val (text, ref) = remember(quote) { splitAyahReference(quote) }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(Brush.linearGradient(listOf(HomeReflection, Color(0xFF06372A)))).testTag("home_reflection")) {
        MasjidLanternArt(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("DAILY REFLECTION", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(76.dp).height(1.5.dp).background(StitchGold.copy(alpha = 0.8f)))
            Spacer(Modifier.height(14.dp))
            Text("“$text”", color = Color.White, fontSize = 22.sp, fontFamily = LibreCaslon, lineHeight = 29.sp,
                modifier = Modifier.fillMaxWidth(0.78f))
            if (ref.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Quran $ref", color = StitchGold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                    .clickable(onClick = onReflect).padding(horizontal = 16.dp, vertical = 9.dp).testTag("home_reflect"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reflect with the Ummah", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}

/** Subtle line art (arch frame, domes, minarets, hanging lantern) drawn on the reflection card. */
@Composable
private fun MasjidLanternArt(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val line = Color(0xFF8FD1B0).copy(alpha = 0.16f)
        val st = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val left = w * 0.56f; val right = w - 10.dp.toPx()
        val cx = (left + right) / 2f
        // Pointed arch frame.
        val arch = Path().apply {
            moveTo(left, h); lineTo(left, h * 0.38f)
            quadraticBezierTo(left, h * 0.10f, cx, h * 0.04f)
            quadraticBezierTo(right, h * 0.10f, right, h * 0.38f); lineTo(right, h)
        }
        drawPath(arch, line, style = st)
        val base = h - 14.dp.toPx()
        drawLine(line, Offset(left + 8.dp.toPx(), base), Offset(right - 8.dp.toPx(), base), st.width)
        // Central dome + side domes.
        val domeR = (right - left) * 0.16f
        val bodyTop = base - (right - left) * 0.20f
        drawRect(line, Offset(cx - domeR * 1.15f, bodyTop), Size(domeR * 2.3f, base - bodyTop), style = st)
        drawArc(line, 180f, 180f, false, Offset(cx - domeR, bodyTop - domeR), Size(domeR * 2, domeR * 2), style = st)
        drawLine(line, Offset(cx, bodyTop - domeR), Offset(cx, bodyTop - domeR - 8.dp.toPx()), st.width)
        val sr = domeR * 0.55f
        listOf(cx - domeR * 2.0f, cx + domeR * 2.0f).forEach { sx ->
            val top = base - (right - left) * 0.11f
            drawRect(line, Offset(sx - sr, top), Size(sr * 2, base - top), style = st)
            drawArc(line, 180f, 180f, false, Offset(sx - sr, top - sr), Size(sr * 2, sr * 2), style = st)
        }
        // Minarets.
        listOf(cx - domeR * 3.2f, cx + domeR * 3.2f).forEach { mx ->
            val mw = 4.dp.toPx(); val top = base - (right - left) * 0.48f
            drawRect(line, Offset(mx - mw / 2, top), Size(mw, base - top), style = st)
            val tip = Path().apply { moveTo(mx - mw / 2, top); lineTo(mx, top - 9.dp.toPx()); lineTo(mx + mw / 2, top) }
            drawPath(tip, line, style = st)
            drawLine(line, Offset(mx - mw, top + 10.dp.toPx()), Offset(mx + mw, top + 10.dp.toPx()), st.width)
        }
        // Hanging lantern with a soft glow.
        val lx = cx; val ly = h * 0.30f
        val gold = StitchGold.copy(alpha = 0.55f)
        drawLine(gold.copy(alpha = 0.35f), Offset(lx, h * 0.06f), Offset(lx, ly - 9.dp.toPx()), 1.dp.toPx())
        drawCircle(Brush.radialGradient(listOf(StitchGold.copy(alpha = 0.28f), Color.Transparent), Offset(lx, ly + 4.dp.toPx()), 20.dp.toPx()),
            20.dp.toPx(), Offset(lx, ly + 4.dp.toPx()))
        val lw = 7.dp.toPx(); val lh = 16.dp.toPx()
        val lantern = Path().apply {
            moveTo(lx, ly - 9.dp.toPx()); lineTo(lx + lw, ly - 2.dp.toPx()); lineTo(lx + lw, ly + lh - 4.dp.toPx())
            lineTo(lx, ly + lh); lineTo(lx - lw, ly + lh - 4.dp.toPx()); lineTo(lx - lw, ly - 2.dp.toPx()); close()
        }
        drawPath(lantern, gold, style = st)
        drawRoundRect(StitchGold.copy(alpha = 0.35f), Offset(lx - 2.5.dp.toPx(), ly + 2.dp.toPx()), Size(5.dp.toPx(), 8.dp.toPx()),
            CornerRadius(2.dp.toPx()))
    }
}

/* ---------------------------------------------------------------- custom outline icons */

private fun ImageVector.Builder.circle(cx: Float, cy: Float, r: Float, width: Float) = path(
    stroke = SolidColor(Color.Black), strokeLineWidth = width
) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
    arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
    close()
}

/** Prayer beads (misbaha): a loop of beads with a tassel. */
internal val TasbihBeadsIcon: ImageVector by lazy {
    ImageVector.Builder("TasbihBeads", 24.dp, 24.dp, 24f, 24f).apply {
        val cx = 12f; val cy = 9.2f; val R = 7.0f
        for (i in 0 until 10) {
            val deg = 90f + 24f + i * (312f / 9f)
            val a = Math.toRadians(deg.toDouble())
            circle(cx + R * kotlin.math.cos(a).toFloat(), cy + R * kotlin.math.sin(a).toFloat(), 1.45f, 1.5f)
        }
        circle(12f, 17.4f, 1.3f, 1.5f)
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
            moveTo(12f, 18.5f); lineTo(12f, 22f)
            moveTo(12f, 19.6f); lineTo(10.4f, 22f)
            moveTo(12f, 19.6f); lineTo(13.6f, 22f)
        }
    }.build()
}

/** Two raised open hands (dua). */
internal val DuaHandsIcon: ImageVector by lazy {
    ImageVector.Builder("DuaHands", 24.dp, 24.dp, 24f, 24f).apply {
        fun hand(m: Float) = path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            // m = +1 left hand, -1 right hand (mirrored around x = 12).
            fun x(v: Float) = if (m > 0) v else 24f - v
            moveTo(x(10.6f), 21f)
            lineTo(x(10.6f), 18.2f)
            curveTo(x(10.6f), 17f, x(11.2f), 16.3f, x(11.2f), 15f)
            lineTo(x(11.2f), 10.6f)
            curveTo(x(11.2f), 9.7f, x(10.2f), 9.4f, x(9.7f), 10.1f)
            lineTo(x(8.8f), 12.4f)
            lineTo(x(8.8f), 4.4f)
            curveTo(x(8.8f), 3.4f, x(7.3f), 3.4f, x(7.3f), 4.4f)
            lineTo(x(7.3f), 9.8f)
            moveTo(x(7.3f), 5.6f)
            curveTo(x(7.3f), 4.6f, x(5.8f), 4.6f, x(5.8f), 5.6f)
            lineTo(x(5.8f), 10.2f)
            moveTo(x(5.8f), 7.2f)
            curveTo(x(5.8f), 6.2f, x(4.3f), 6.2f, x(4.3f), 7.2f)
            lineTo(x(4.3f), 14.6f)
            curveTo(x(4.3f), 16.4f, x(5.2f), 17.6f, x(6.4f), 18.4f)
            lineTo(x(6.4f), 21f)
        }
        hand(1f); hand(-1f)
    }.build()
}
