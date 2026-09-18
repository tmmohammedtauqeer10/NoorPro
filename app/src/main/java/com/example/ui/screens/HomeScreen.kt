package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SuggestionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GlassOverlay),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MatteGold.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MatteGold, modifier = Modifier.size(21.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
fun EntranceTransition(
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    var isStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.coerceAtMost(60).toLong())
        isStarted = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
        label = "alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isStarted) 0f else 24f, // 24dp subtle rise
        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
        label = "offsetY"
    )
    
    Box(
        modifier = Modifier
            .alpha(alpha)
            .graphicsLayer {
                translationY = offsetY * density
            }
    ) {
        content()
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.SectionHeader(title = title, modifier = modifier)
}

@Composable
fun HomeScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val nextPrayerCountdown by viewModel.nextPrayerCountdown.collectAsState()
    val lastReadSurah by viewModel.lastReadSurah.collectAsState()
    val lastReadAyah by viewModel.lastReadAyah.collectAsState()
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val dailyHadith by viewModel.dailyHadith.collectAsState()
    val isDailyHadithLoading by viewModel.isDailyHadithLoading.collectAsState()
    val prayers by viewModel.prayers.collectAsState()
    val activePrayerIndex by viewModel.activePrayerIndex.collectAsState()
    val todayHijri by viewModel.todayHijri.collectAsState()
    val todayGregorian by viewModel.todayGregorian.collectAsState()
    val currentLocationName by viewModel.currentLocationName.collectAsState()
    val dynamicHijriEvents by viewModel.dynamicHijriEvents.collectAsState()
    
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val totalPoints by viewModel.totalPoints.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val completedSurahs by viewModel.completedSurahs.collectAsState()

    val strings = LocalAppStrings.current
    
    val scrollState = rememberScrollState()
    val isLightHome = currentThemeMode == com.example.ui.viewmodel.ThemeMode.LIGHT ||
        (currentThemeMode == com.example.ui.viewmodel.ThemeMode.SYSTEM && !isSystemInDarkTheme())
    val homeBackground =
        if (isLightHome) {
            Brush.verticalGradient(
                listOf(Color(0xFFFFF7FB), Color(0xFFFFEAF3), Color(0xFFFFFFFF))
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFF020817), Color(0xFF050C18), Color(0xFF030A16))
            )
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(homeBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 220.dp) // Space for floating navigation and audio player
        ) {
        // 1. Premium Geometric Header & Prayer Countdown (Instant or extremely quick)
        EntranceTransition(delayMillis = 0) {
            CountdownHeader(
                prayerName = nextPrayerName,
                countdown = nextPrayerCountdown,
                prayers = prayers.map { it.copy(time = viewModel.displayPrayerTime(it.time)) },
                activeIndex = activePrayerIndex,
                currentLocationName = currentLocationName,
                themeMode = currentThemeMode,
                onMenuClick = { viewModel.navigateTo(DeenScreen.EXPLORE) },
                onNotificationClick = { viewModel.navigateTo(DeenScreen.SETTINGS) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            EntranceTransition(delayMillis = 60) {
                DailyRhythmCard(
                    sehriTime = prayers.firstOrNull { it.name == "Fajr" }?.let { viewModel.displayPrayerTime(it.time) } ?: "--:--",
                    iftarTime = prayers.firstOrNull { it.name == "Maghrib" }?.let { viewModel.displayPrayerTime(it.time) } ?: "--:--",
                    location = currentLocationName,
                    onPrayerClick = { viewModel.navigateTo(DeenScreen.PRAYER_TIMES) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Daily Ayah/Hadith Quote Slider (Inspiring element)
            EntranceTransition(delayMillis = 80) {
                DailyQuoteCard(quote = dailyQuote)
            }

            Spacer(modifier = Modifier.height(16.dp))
            EntranceTransition(delayMillis = 160) {
                DailyHadithCard(
                    hadith = dailyHadith,
                    isLoading = isDailyHadithLoading
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            EntranceTransition(delayMillis = 200) {
                NoorPointsCard(
                    points = totalPoints,
                    streak = currentStreak,
                    completedSurahs = completedSurahs.size
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Quick Action Buttons Row
            EntranceTransition(delayMillis = 240) {
                Column {
                    HomeSectionHeader(title = strings.quickActions)
                    QuickActionsRow(
                        strings = strings,
                        onQuranSelected = { viewModel.navigateTo(DeenScreen.QURAN) },
                        onPrayersSelected = { viewModel.navigateTo(DeenScreen.PRAYER_TIMES) },
                        onQiblaSelected = { 
                            viewModel.navigateTo(DeenScreen.QIBLA_MORE)
                        },
                        onQazaSelected = {
                            viewModel.navigateTo(DeenScreen.QAZA_TRACKER)
                        },
                        onBookmarksSelected = {
                            viewModel.navigateTo(DeenScreen.BOOKMARKS)
                        },
                        onHadithSelected = {
                            viewModel.navigateTo(DeenScreen.HADITH_LIBRARY)
                        },
                        onDuaSelected = {
                            viewModel.navigateTo(DeenScreen.DUA_HUB)
                        },
                        onLibrarySelected = {
                            viewModel.navigateTo(DeenScreen.LIBRARY_DASHBOARD)
                        },
                        onQuizSelected = {
                            viewModel.navigateTo(DeenScreen.QUIZ_DASHBOARD)
                        },
                        onRecitationsSelected = {
                            viewModel.navigateTo(DeenScreen.NOW_PLAYING)
                        },
                        onTasbihSelected = {
                            viewModel.navigateTo(DeenScreen.TASBIH)
                        },
                        onAzkarSelected = {
                            viewModel.navigateTo(DeenScreen.AZKAR)
                        },
                        onHajjUmrahSelected = {
                            viewModel.navigateTo(DeenScreen.HAJJ_UMRAH)
                        },
                        onAsmaSelected = {
                            viewModel.navigateTo(DeenScreen.ASMA_UL_HUSNA)
                        },
                        onCalendarSelected = {
                            viewModel.navigateTo(DeenScreen.CALENDAR)
                        },
                        onZakatSelected = {
                            viewModel.navigateTo(DeenScreen.ZAKAT)
                        },
                        onHealthSelected = {
                            viewModel.navigateTo(DeenScreen.HEALTH_WELLNESS)
                        },
                        onAiSelected = {
                            viewModel.navigateTo(DeenScreen.AI_HUB)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3b. Hijri Calendar & Holy Events
            EntranceTransition(delayMillis = 320) {
                Column {
                    HomeSectionHeader(title = strings.calendar)
                    HijriCalendarCard(
                        strings = strings,
                        todayHijri = todayHijri, 
                        todayGregorian = todayGregorian,
                        onClick = { viewModel.navigateTo(DeenScreen.CALENDAR) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            EntranceTransition(delayMillis = 400) {
                val todayPrayerLog by viewModel.todayPrayerLog.collectAsState()
                DailyPrayerTrackerCard(
                    log = todayPrayerLog,
                    onTogglePrayer = { viewModel.toggleDailyPrayer(it) },
                    onQazaShortcut = { viewModel.navigateTo(DeenScreen.QAZA_TRACKER) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            EntranceTransition(delayMillis = 440) {
                val weeklyLogs by viewModel.weeklyPrayerLogs.collectAsState()
                PrayerPunctualityChart(weeklyLogs = weeklyLogs)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            EntranceTransition(delayMillis = 460) {
                val monthlyLogs by viewModel.currentMonthLogs.collectAsState()
                val monthlyGoal by viewModel.userPreferencesRepo.monthlyPrayerGoalFlow.collectAsState(initial = 150)
                var isEditingGoal by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                MonthlyGoalProgressCard(
                    monthlyLogs = monthlyLogs,
                    currentGoal = monthlyGoal,
                    isEditingGoal = isEditingGoal,
                    onEditGoalClick = { isEditingGoal = it },
                    onGoalChange = { newGoal ->
                        coroutineScope.launch {
                            viewModel.userPreferencesRepo.updateMonthlyPrayerGoal(newGoal)
                            isEditingGoal = false
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Quick Resume Quran Card
            EntranceTransition(delayMillis = 480) {
                Column {
                    HomeSectionHeader(title = strings.lastRead)
                    QuickResumeCard(
                        strings = strings,
                        surah = lastReadSurah,
                        ayah = lastReadAyah,
                        onClick = { viewModel.resumeReading(lastReadSurah, lastReadAyah) }
                    )
                }
            }
        } // ends inner Column
        } // ends outer Column
        
    } // ends Box
} // ends HomeScreen()

@Composable
fun CountdownHeader(
    prayerName: String,
    countdown: String,
    prayers: List<PrayerTime>,
    activeIndex: Int,
    currentLocationName: String,
    themeMode: com.example.ui.viewmodel.ThemeMode = com.example.ui.viewmodel.ThemeMode.SYSTEM,
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visiblePrayers = prayers.filter { it.name != "Sunrise" }.take(5)
    val isLightTheme = themeMode == com.example.ui.viewmodel.ThemeMode.LIGHT ||
        (themeMode == com.example.ui.viewmodel.ThemeMode.SYSTEM && !isSystemInDarkTheme())
    val headerGradient = if (isLightTheme) {
        listOf(Color(0xFFFFFFFF), Color(0xFFFFF7FB), Color(0xFFFFEAF3))
    } else {
        listOf(Color(0xFF020817), Color(0xFF071326), Color(0xFF030A16))
    }
    val heroGradient = if (isLightTheme) {
        listOf(Color(0xFFFBFCF8), Color(0xFFEAF4F0), Color(0xFFDBEDE5))
    } else {
        listOf(Color(0xFF071326), Color(0xFF0B172B), Color(0xFF07101F))
    }
    val heroOverlay = if (isLightTheme) {
        listOf(Color.Transparent, Color(0x66FFFFFF), Color(0xEFF1F6F2))
    } else {
        listOf(Color.Transparent, Color(0xCC07101F), Color(0xF207101F))
    }
    val primaryTextColor = if (isLightTheme) Color(0xFF10231F) else Color.White
    val secondaryTextColor = if (isLightTheme) Color(0xFF5E7169) else Color(0xFFCBD5E1).copy(alpha = 0.88f)
    val prayerStripColor = if (isLightTheme) Color(0xEFF1F6F2) else Color(0xCC07101F)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.verticalGradient(
                    colors = headerGradient,
                    startY = 0.0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(GlassBorder, Color.Transparent)
                ),
                RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(top = 38.dp, bottom = 18.dp)
    ) {
        // Delicate geometric backdrop layer
        Canvas(modifier = Modifier.matchParentSize().alpha(0.05f)) {
            val center = Offset(size.width * 0.5f, size.height * 0.4f)
            val radius = size.width * 0.45f
            drawCircle(
                color = MatteGold,
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = MatteGold,
                radius = radius * 0.7f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = MatteGold,
                radius = radius * 0.4f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Explore",
                    tint = MatteGold,
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notification settings",
                    tint = primaryTextColor
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 48.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(274.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            heroGradient
                        )
                    )
                    .border(1.dp, MatteGold.copy(alpha = 0.42f), RoundedCornerShape(24.dp))
                    .testTag("premium_prayer_hero")
            ) {
                PrayerHeroBackdrop(isLightTheme = isLightTheme)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                heroOverlay
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(start = 22.dp, top = 28.dp, end = 22.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Spacer(modifier = Modifier.weight(0.72f))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "UP NEXT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MatteGold,
                                letterSpacing = 2.4.sp,
                                fontSize = 10.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "$prayerName Prayer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor,
                                letterSpacing = 0.5.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = countdown,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MatteGold,
                                letterSpacing = (-2).sp,
                                fontSize = 42.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("countdown_timer")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MatteGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentLocationName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = secondaryTextColor,
                                    letterSpacing = 0.2.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(prayerStripColor)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    visiblePrayers.forEach { prayer ->
                        // Highlight the prayer being counted down to (matches the hero header),
                        // like Muslim Pro — fall back to the active period if no name match.
                        val isCurrent = if (visiblePrayers.any { it.name == prayerName }) {
                            prayer.name == prayerName
                        } else {
                            prayers.indexOf(prayer) == activeIndex
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isCurrent) {
                                        if (isLightTheme) {
                                            Brush.verticalGradient(listOf(Color(0xFFCFEBE0), Color(0xFFB6E0D2)))
                                        } else {
                                            Brush.verticalGradient(listOf(Color(0xFF6F4815), Color(0xFF2A211A)))
                                        }
                                    } else {
                                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) MatteGold.copy(alpha = 0.58f) else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = prayer.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MatteGold else secondaryTextColor,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = prayer.time,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isCurrent) MatteGold else primaryTextColor,
                                    fontSize = if (isCurrent) 13.sp else 11.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerHeroBackdrop(isLightTheme: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                if (isLightTheme) {
                    listOf(Color(0xFFFBFCF8), Color(0xFFDCEDE6), Color(0xFFF1ECDC))
                } else {
                    listOf(Color(0xFF07112C), Color(0xFF123456), Color(0xFF06101F))
                },
                startY = 0f,
                endY = h
            )
        )
        val sunCenter = Offset(w * 0.22f, h * 0.32f)
        drawCircle(
            color = Color(0xFFFFC266).copy(alpha = if (isLightTheme) 0.28f else 0.22f),
            radius = w * 0.46f,
            center = sunCenter
        )
        drawCircle(
            color = Color(0xFFFFA928).copy(alpha = if (isLightTheme) 0.55f else 0.48f),
            radius = w * 0.25f,
            center = sunCenter
        )
        drawCircle(
            color = Color(0xFFFFE09A).copy(alpha = 0.70f),
            radius = w * 0.14f,
            center = sunCenter
        )
        repeat(11) { index ->
            val angle = Math.toRadians((index * 16 + 185).toDouble())
            val start = Offset(
                (sunCenter.x + Math.cos(angle) * w * 0.18f).toFloat(),
                (sunCenter.y + Math.sin(angle) * w * 0.18f).toFloat()
            )
            val end = Offset(
                (sunCenter.x + Math.cos(angle) * w * 0.32f).toFloat(),
                (sunCenter.y + Math.sin(angle) * w * 0.32f).toFloat()
            )
            drawLine(Color(0xFFFFC04D).copy(alpha = 0.20f), start, end, strokeWidth = 3.dp.toPx())
        }

        if (!isLightTheme) {
            listOf(
                Offset(w * 0.08f, h * 0.16f),
                Offset(w * 0.24f, h * 0.11f),
                Offset(w * 0.36f, h * 0.18f)
            ).forEachIndexed { index, star ->
                drawCircle(MatteGold.copy(alpha = 0.75f), radius = (2.0f + index).dp.toPx(), center = star)
            }
        }

        val ground = h * 0.76f
        val baseColor = if (isLightTheme) Color(0xFFFFFBFD).copy(alpha = 0.96f) else Color(0xFF050A14).copy(alpha = 0.96f)
        val shadeColor = if (isLightTheme) Color(0xFFCFE0D8).copy(alpha = 0.66f) else Color(0xFF0B172B).copy(alpha = 0.98f)
        val domeColor = if (isLightTheme) Color(0xFFFFE2A2).copy(alpha = 0.96f) else Color(0xFFD9A33A).copy(alpha = 0.92f)
        val domeShade = if (isLightTheme) Color(0xFFD69A31).copy(alpha = 0.35f) else Color(0xFF7A4F15).copy(alpha = 0.42f)
        val minaretGold = if (isLightTheme) Color(0xFFD8A938) else MatteGold
        val wallStroke = minaretGold.copy(alpha = if (isLightTheme) 0.42f else 0.35f)

        fun minaret(centerX: Float, towerHeight: Float, widthFactor: Float = 0.036f) {
            val tw = w * widthFactor
            val x = centerX - tw / 2f
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(x, ground - towerHeight),
                size = androidx.compose.ui.geometry.Size(tw, towerHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(tw * 0.20f, tw * 0.20f)
            )
            drawRect(shadeColor, Offset(x + tw * 0.62f, ground - towerHeight), androidx.compose.ui.geometry.Size(tw * 0.38f, towerHeight))
            drawRoundRect(
                color = minaretGold,
                topLeft = Offset(x - tw * 0.16f, ground - towerHeight - tw * 0.56f),
                size = androidx.compose.ui.geometry.Size(tw * 1.32f, tw * 0.76f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(tw * 0.22f, tw * 0.22f)
            )
            drawCircle(baseColor, radius = tw * 0.48f, center = Offset(x + tw / 2f, ground - towerHeight - tw * 0.78f))
            drawLine(minaretGold.copy(alpha = 0.70f), Offset(x + tw / 2f, ground - towerHeight - tw * 1.28f), Offset(x + tw / 2f, ground - towerHeight - tw * 0.72f), strokeWidth = 1.4.dp.toPx())
        }

        fun dome(centerX: Float, radius: Float, baseY: Float, color: Color, crescent: Boolean = false) {
            drawCircle(color, radius = radius, center = Offset(centerX, baseY))
            drawRect(
                color = baseColor,
                topLeft = Offset(centerX - radius, baseY),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius)
            )
            drawCircle(
                color = domeShade,
                radius = radius * 1.04f,
                center = Offset(centerX + radius * 0.15f, baseY + radius * 0.04f)
            )
            if (crescent) {
                drawLine(
                    minaretGold.copy(alpha = 0.74f),
                    Offset(centerX, baseY - radius * 0.95f),
                    Offset(centerX, baseY - radius * 1.45f),
                    strokeWidth = 1.3.dp.toPx()
                )
                drawArc(
                    color = minaretGold,
                    startAngle = 70f,
                    sweepAngle = 235f,
                    useCenter = false,
                    topLeft = Offset(centerX - radius * 0.17f, baseY - radius * 1.72f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.36f, radius * 0.36f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        minaret(w * 0.17f, h * 0.42f, 0.036f)
        minaret(w * 0.84f, h * 0.42f, 0.036f)

        drawRoundRect(
            color = baseColor,
            topLeft = Offset(w * 0.17f, ground - h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.67f, h * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f, w * 0.03f)
        )
        drawLine(
            wallStroke,
            Offset(w * 0.18f, ground - h * 0.20f),
            Offset(w * 0.83f, ground - h * 0.20f),
            strokeWidth = 1.dp.toPx()
        )

        dome(w * 0.50f, w * 0.15f, ground - h * 0.22f, domeColor, crescent = true)

        drawRoundRect(
            color = if (isLightTheme) Color(0xFFD7EAE2).copy(alpha = 0.54f) else Color(0xFF0B172B).copy(alpha = 0.82f),
            topLeft = Offset(w * 0.20f, ground - h * 0.095f),
            size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.095f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
        )
        drawRoundRect(
            color = Color(0xFFFFA51F).copy(alpha = 0.32f),
            topLeft = Offset(w * 0.46f, ground - h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, w * 0.04f)
        )
        drawRect(
            color = if (isLightTheme) Color(0xFFE7F0EB).copy(alpha = 0.70f) else Color(0xFF020817).copy(alpha = 0.66f),
            topLeft = Offset(0f, ground),
            size = androidx.compose.ui.geometry.Size(w, h - ground)
        )
    }
}

@Composable
fun DailyRhythmCard(
    sehriTime: String,
    iftarTime: String,
    location: String,
    onPrayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cardColor = if (isLightTheme) Color.White else Color(0xFF071326).copy(alpha = 0.98f)
    val locationColor = if (isLightTheme) Color(0xFF7A5A66) else Color(0xFFCBD5E1)
    val sehriTileColors = if (isLightTheme) listOf(Color(0xFFEAF4F0), Color(0xFFD7EAE2)) else listOf(Color(0xFF14294D), Color(0xFF1D4161))
    val iftarTileColors = if (isLightTheme) listOf(Color(0xFFFFE7C4), Color(0xFFFFB45C)) else listOf(Color(0xFF553217), Color(0xFF80521D))
    Card(
        onClick = onPrayerClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, if (isLightTheme) Color(0xFFCFE0D8) else MatteGold.copy(alpha = 0.36f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "TODAY'S RHYTHM",
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MatteGold
                    )
                    Text(
                        location,
                        style = MaterialTheme.typography.bodySmall,
                        color = locationColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.WbTwilight, contentDescription = null, tint = MatteGold)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RhythmTimeTile(
                    title = "Sehri ends",
                    time = sehriTime,
                    icon = Icons.Default.NightsStay,
                    colors = sehriTileColors,
                    modifier = Modifier.weight(1f)
                )
                RhythmTimeTile(
                    title = "Iftar",
                    time = iftarTime,
                    icon = Icons.Default.WbSunny,
                    colors = iftarTileColors,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RhythmTimeTile(
    title: String,
    time: String,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val isLightTile = colors.firstOrNull()?.luminance()?.let { it > 0.65f } == true
    val tileTextColor = if (isLightTile) Color(0xFF2B1821) else Color.White
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(colors))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tileTextColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = tileTextColor.copy(alpha = 0.72f), fontSize = 11.sp)
            Text(time, color = tileTextColor, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun DailyQuoteCard(
    quote: Pair<String, String>,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLightTheme) Color.White.copy(alpha = 0.94f) else GlassOverlay),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Book icon",
                    tint = MatteGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Qur'an Verse",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MatteGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Beautiful Arabic Text
            Text(
                text = quote.second,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = LightGold,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 30.sp
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // English translation
            Text(
                text = quote.first,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QuickResumeCard(
    strings: AppStrings,
    surah: Surah,
    ayah: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLightTheme) Color.White.copy(alpha = 0.94f) else GlassOverlay),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("quick_resume_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Elegant Quran book badge shape
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkEmerald, Color(0xFF0C1917))
                        )
                    )
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Quran icon",
                    tint = MatteGold,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Surah ${surah.nameEnglish}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${surah.englishTranslation} • Ayah $ayah",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }

            // Beautiful arrow icon
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassOverlay)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Resume reading",
                    tint = MatteGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private enum class QuickActionIconKey {
    Quran,
    Audio,
    Dua,
    PrayerTimes,
    Hadith,
    Qibla,
    Qaza,
    Bookmarks,
    Library,
    Quiz,
    Tasbih,
    Azkar,
    HajjUmrah,
    Asma,
    Calendar,
    Zakat,
    Health,
    NoorAi
}

private data class QuickActionItemSpec(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
    val iconKey: QuickActionIconKey
)

@Composable
fun DailyHadithCard(
    hadith: com.example.data.Hadith?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLightTheme) Color.White.copy(alpha = 0.94f) else GlassOverlay),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Hadith log",
                    tint = MatteGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Daily Authentic Hadith",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MatteGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoading) {
                CircularProgressIndicator(color = MatteGold, modifier = Modifier.size(24.dp))
            } else if (hadith != null) {
                Text(
                    text = hadith.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${hadith.chapterTitle} — Hadith ${hadith.id}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Awaiting Daily Hadith...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    strings: AppStrings,
    onQuranSelected: () -> Unit,
    onPrayersSelected: () -> Unit,
    onQiblaSelected: () -> Unit,
    onQazaSelected: () -> Unit,
    onBookmarksSelected: () -> Unit,
    onHadithSelected: () -> Unit,
    onDuaSelected: () -> Unit,
    onLibrarySelected: () -> Unit,
    onQuizSelected: () -> Unit,
    onRecitationsSelected: () -> Unit,
    onTasbihSelected: () -> Unit,
    onAzkarSelected: () -> Unit,
    onHajjUmrahSelected: () -> Unit,
    onAsmaSelected: () -> Unit,
    onCalendarSelected: () -> Unit,
    onZakatSelected: () -> Unit,
    onHealthSelected: () -> Unit,
    onAiSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        QuickActionItemSpec("Noor AI", Icons.Default.AutoAwesome, onAiSelected, QuickActionIconKey.NoorAi),
        QuickActionItemSpec(strings.quran, Icons.AutoMirrored.Filled.MenuBook, onQuranSelected, QuickActionIconKey.Quran),
        QuickActionItemSpec("Audio", Icons.Default.Headphones, onRecitationsSelected, QuickActionIconKey.Audio),
        QuickActionItemSpec("Dua Hub", Icons.Default.VolunteerActivism, onDuaSelected, QuickActionIconKey.Dua),
        QuickActionItemSpec(strings.prayerTimes, Icons.Default.Schedule, onPrayersSelected, QuickActionIconKey.PrayerTimes),
        QuickActionItemSpec("Hadith", Icons.Default.AutoStories, onHadithSelected, QuickActionIconKey.Hadith),
        QuickActionItemSpec("Qibla", Icons.Default.Explore, onQiblaSelected, QuickActionIconKey.Qibla),
        QuickActionItemSpec("Qaza", Icons.Default.Checklist, onQazaSelected, QuickActionIconKey.Qaza),
        QuickActionItemSpec("Bookmarks", Icons.Default.Bookmark, onBookmarksSelected, QuickActionIconKey.Bookmarks),
        QuickActionItemSpec("Library", Icons.Default.LocalLibrary, onLibrarySelected, QuickActionIconKey.Library),
        QuickActionItemSpec("Quiz", Icons.Default.Quiz, onQuizSelected, QuickActionIconKey.Quiz),
        QuickActionItemSpec("Tasbih", Icons.Default.Loop, onTasbihSelected, QuickActionIconKey.Tasbih),
        QuickActionItemSpec("Azkar", Icons.Default.WbSunny, onAzkarSelected, QuickActionIconKey.Azkar),
        QuickActionItemSpec("Hajj & Umrah", Icons.Default.Mosque, onHajjUmrahSelected, QuickActionIconKey.HajjUmrah),
        QuickActionItemSpec("99 Names", Icons.Default.Star, onAsmaSelected, QuickActionIconKey.Asma),
        QuickActionItemSpec("Calendar", Icons.Default.CalendarMonth, onCalendarSelected, QuickActionIconKey.Calendar),
        QuickActionItemSpec("Zakat", Icons.Default.Paid, onZakatSelected, QuickActionIconKey.Zakat),
        QuickActionItemSpec("Health", Icons.Default.MonitorHeart, onHealthSelected, QuickActionIconKey.Health)
    )
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cardGradient = if (isLightTheme) {
        Brush.verticalGradient(listOf(Color(0xFFFFFCFA), Color(0xFFFFF2EC)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF101D32), Color(0xFF07101F)))
    }
    val titleColor = if (isLightTheme) Color(0xFF2B1821) else Color.White

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val pageWidth = maxWidth
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items.chunked(6)) { pageItems ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.width(pageWidth)
                ) {
                    pageItems.chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { item ->
                                QuickActionTile(
                                    item = item,
                                    cardGradient = cardGradient,
                                    titleColor = titleColor,
                                    isLightTheme = isLightTheme,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    item: QuickActionItemSpec,
    cardGradient: Brush,
    titleColor: Color,
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(cardGradient)
            .border(1.dp, if (isLightTheme) Color(0xFFCFE0D8) else MatteGold.copy(alpha = .18f), RoundedCornerShape(22.dp))
            .clickable { item.onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .testTag("quick_${item.title.lowercase().replace(" ", "_")}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                PremiumQuickActionIcon(
                    iconKey = item.iconKey,
                    title = item.title,
                    modifier = Modifier.size(58.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PremiumQuickActionIcon(
    iconKey: QuickActionIconKey,
    title: String,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val gold = if (isLightTheme) Color(0xFFD58A00) else MatteGold
    val deepGold = if (isLightTheme) Color(0xFF9C6400) else Color(0xFFFFD56A)
    val softGold = if (isLightTheme) Color(0xFFFACB82) else Color(0xFFFFE8A3)
    val accent = if (isLightTheme) Color(0xFFF3A950) else Color(0xFFFFC45A)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun p(x: Float, y: Float) = Offset(w * x, h * y)
            val stroke = Stroke(width = w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val thinStroke = Stroke(width = w * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val fillAlpha = if (isLightTheme) 0.22f else 0.18f

            fun drawSimpleStar(cx: Float, cy: Float, outer: Float, inner: Float, points: Int = 5, color: Color = gold) {
                val path = Path()
                for (i in 0 until points * 2) {
                    val radius = if (i % 2 == 0) outer else inner
                    val angle = -PI / 2.0 + i * PI / points
                    val x = cx + (cos(angle) * radius).toFloat()
                    val y = cy + (sin(angle) * radius).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = color)
            }

            fun drawOpenBook() {
                drawLine(gold, p(0.50f, 0.27f), p(0.50f, 0.78f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                val left = Path().apply {
                    moveTo(w * 0.12f, h * 0.28f)
                    quadraticBezierTo(w * 0.30f, h * 0.20f, w * 0.50f, h * 0.34f)
                    lineTo(w * 0.50f, h * 0.78f)
                    quadraticBezierTo(w * 0.30f, h * 0.65f, w * 0.12f, h * 0.72f)
                    close()
                }
                val right = Path().apply {
                    moveTo(w * 0.88f, h * 0.28f)
                    quadraticBezierTo(w * 0.70f, h * 0.20f, w * 0.50f, h * 0.34f)
                    lineTo(w * 0.50f, h * 0.78f)
                    quadraticBezierTo(w * 0.70f, h * 0.65f, w * 0.88f, h * 0.72f)
                    close()
                }
                drawPath(left, color = softGold.copy(alpha = fillAlpha))
                drawPath(right, color = softGold.copy(alpha = fillAlpha))
                drawPath(left, color = gold, style = thinStroke)
                drawPath(right, color = gold, style = thinStroke)
                drawLine(deepGold, p(0.22f, 0.43f), p(0.42f, 0.47f), strokeWidth = w * 0.026f, cap = StrokeCap.Round)
                drawLine(deepGold, p(0.58f, 0.47f), p(0.78f, 0.43f), strokeWidth = w * 0.026f, cap = StrokeCap.Round)
            }

            fun drawAudio() {
                drawArc(gold, startAngle = 210f, sweepAngle = 120f, useCenter = false, topLeft = p(0.18f, 0.18f), size = Size(w * 0.64f, h * 0.64f), style = stroke)
                drawLine(gold, p(0.24f, 0.52f), p(0.24f, 0.68f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                drawLine(gold, p(0.76f, 0.52f), p(0.76f, 0.68f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                val play = Path().apply {
                    moveTo(w * 0.44f, h * 0.37f)
                    lineTo(w * 0.44f, h * 0.63f)
                    lineTo(w * 0.65f, h * 0.50f)
                    close()
                }
                drawPath(play, color = gold)
                listOf(0.37f to 0.67f, 0.45f to 0.74f, 0.53f to 0.69f, 0.61f to 0.76f).forEach { (x, y) ->
                    drawLine(accent, p(x, 0.82f), p(x, y), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                }
            }

            fun drawDua() {
                val leftPalm = Path().apply {
                    moveTo(w * 0.30f, h * 0.72f)
                    cubicTo(w * 0.18f, h * 0.58f, w * 0.17f, h * 0.38f, w * 0.28f, h * 0.30f)
                    cubicTo(w * 0.39f, h * 0.42f, w * 0.41f, h * 0.58f, w * 0.43f, h * 0.74f)
                    close()
                }
                val rightPalm = Path().apply {
                    moveTo(w * 0.70f, h * 0.72f)
                    cubicTo(w * 0.82f, h * 0.58f, w * 0.83f, h * 0.38f, w * 0.72f, h * 0.30f)
                    cubicTo(w * 0.61f, h * 0.42f, w * 0.59f, h * 0.58f, w * 0.57f, h * 0.74f)
                    close()
                }
                drawPath(leftPalm, color = gold.copy(alpha = 0.84f))
                drawPath(rightPalm, color = gold.copy(alpha = 0.84f))
                drawLine(deepGold, p(0.32f, 0.33f), p(0.32f, 0.54f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
                drawLine(deepGold, p(0.25f, 0.36f), p(0.27f, 0.55f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
                drawLine(deepGold, p(0.39f, 0.38f), p(0.39f, 0.58f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
                drawLine(deepGold, p(0.68f, 0.33f), p(0.68f, 0.54f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
                drawLine(deepGold, p(0.75f, 0.36f), p(0.73f, 0.55f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
                drawLine(deepGold, p(0.61f, 0.38f), p(0.61f, 0.58f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
                drawLine(gold, p(0.30f, 0.72f), p(0.41f, 0.86f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(gold, p(0.70f, 0.72f), p(0.59f, 0.86f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawArc(accent, startAngle = 110f, sweepAngle = 250f, useCenter = false, topLeft = p(0.42f, 0.08f), size = Size(w * 0.22f, h * 0.22f), style = thinStroke)
                drawSimpleStar(w * 0.68f, h * 0.16f, w * 0.050f, w * 0.020f, 4, softGold)
            }

            fun drawCalendar(withClock: Boolean) {
                drawRoundRect(gold.copy(alpha = fillAlpha), topLeft = p(0.16f, 0.24f), size = Size(w * 0.62f, h * 0.56f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f))
                drawRoundRect(gold, topLeft = p(0.16f, 0.24f), size = Size(w * 0.62f, h * 0.56f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f), style = thinStroke)
                drawLine(gold, p(0.26f, 0.18f), p(0.26f, 0.33f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
                drawLine(gold, p(0.62f, 0.18f), p(0.62f, 0.33f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
                drawLine(gold, p(0.16f, 0.42f), p(0.78f, 0.42f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
                if (withClock) {
                    drawCircle(color = accent.copy(alpha = 0.92f), radius = w * 0.18f, center = p(0.72f, 0.70f), style = thinStroke)
                    drawLine(accent, p(0.72f, 0.70f), p(0.72f, 0.60f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                    drawLine(accent, p(0.72f, 0.70f), p(0.81f, 0.70f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                }
            }

            fun drawCompass() {
                drawCircle(gold.copy(alpha = fillAlpha), radius = w * 0.36f, center = p(0.50f, 0.50f))
                drawCircle(gold, radius = w * 0.36f, center = p(0.50f, 0.50f), style = stroke)
                val needle = Path().apply {
                    moveTo(w * 0.63f, h * 0.25f)
                    lineTo(w * 0.53f, h * 0.58f)
                    lineTo(w * 0.25f, h * 0.75f)
                    lineTo(w * 0.44f, h * 0.44f)
                    close()
                }
                drawPath(needle, color = gold)
                drawCircle(deepGold, radius = w * 0.035f, center = p(0.50f, 0.50f))
            }

            fun drawScales() {
                drawCircle(gold.copy(alpha = fillAlpha), radius = w * 0.34f, center = p(0.50f, 0.50f))
                drawLine(gold, p(0.50f, 0.23f), p(0.50f, 0.75f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(gold, p(0.28f, 0.37f), p(0.72f, 0.37f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(gold, p(0.31f, 0.37f), p(0.22f, 0.58f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
                drawLine(gold, p(0.31f, 0.37f), p(0.40f, 0.58f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
                drawArc(accent, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = p(0.20f, 0.52f), size = Size(w * 0.22f, h * 0.16f), style = thinStroke)
                drawLine(gold, p(0.69f, 0.37f), p(0.60f, 0.58f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
                drawLine(gold, p(0.69f, 0.37f), p(0.78f, 0.58f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
                drawArc(accent, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = p(0.58f, 0.52f), size = Size(w * 0.22f, h * 0.16f), style = thinStroke)
                drawLine(gold, p(0.36f, 0.80f), p(0.64f, 0.80f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
            }

            fun drawZakatHand() {
                val hand = Path().apply {
                    moveTo(w * 0.14f, h * 0.70f)
                    cubicTo(w * 0.27f, h * 0.63f, w * 0.37f, h * 0.60f, w * 0.52f, h * 0.60f)
                    cubicTo(w * 0.63f, h * 0.60f, w * 0.70f, h * 0.55f, w * 0.78f, h * 0.48f)
                    cubicTo(w * 0.84f, h * 0.54f, w * 0.77f, h * 0.66f, w * 0.61f, h * 0.72f)
                    cubicTo(w * 0.47f, h * 0.77f, w * 0.34f, h * 0.76f, w * 0.14f, h * 0.82f)
                    close()
                }
                drawPath(hand, color = gold.copy(alpha = 0.86f))
                drawLine(deepGold, p(0.34f, 0.63f), p(0.60f, 0.63f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                drawCircle(color = accent.copy(alpha = 0.95f), radius = w * 0.19f, center = p(0.67f, 0.33f))
                drawCircle(color = deepGold.copy(alpha = 0.75f), radius = w * 0.19f, center = p(0.67f, 0.33f), style = thinStroke)
                drawCircle(color = softGold.copy(alpha = 0.38f), radius = w * 0.13f, center = p(0.67f, 0.33f))
                drawLine(deepGold, p(0.63f, 0.25f), p(0.71f, 0.25f), strokeWidth = w * 0.024f, cap = StrokeCap.Round)
                drawLine(deepGold, p(0.67f, 0.22f), p(0.67f, 0.45f), strokeWidth = w * 0.030f, cap = StrokeCap.Round)
                drawArc(deepGold, startAngle = 105f, sweepAngle = 255f, useCenter = false, topLeft = p(0.59f, 0.27f), size = Size(w * 0.16f, h * 0.13f), style = Stroke(width = w * 0.028f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            fun drawBookmark() {
                val path = Path().apply {
                    moveTo(w * 0.30f, h * 0.20f)
                    lineTo(w * 0.70f, h * 0.20f)
                    lineTo(w * 0.70f, h * 0.78f)
                    lineTo(w * 0.50f, h * 0.64f)
                    lineTo(w * 0.30f, h * 0.78f)
                    close()
                }
                drawPath(path, color = gold.copy(alpha = fillAlpha))
                drawPath(path, color = gold, style = stroke)
            }

            fun drawBooks() {
                listOf(0.20f to 0.28f, 0.42f to 0.22f, 0.64f to 0.34f).forEachIndexed { index, (x, y) ->
                    val bookColor = if (index == 1) accent else gold
                    drawRoundRect(bookColor.copy(alpha = fillAlpha), topLeft = p(x, y), size = Size(w * 0.16f, h * 0.48f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.035f, w * 0.035f))
                    drawRoundRect(bookColor, topLeft = p(x, y), size = Size(w * 0.16f, h * 0.48f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.035f, w * 0.035f), style = thinStroke)
                    drawLine(deepGold, p(x + 0.035f, y + 0.18f), p(x + 0.13f, y + 0.18f), strokeWidth = w * 0.022f, cap = StrokeCap.Round)
                }
                drawLine(gold, p(0.16f, 0.82f), p(0.84f, 0.82f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
            }

            fun drawTasbih() {
                val beads = listOf(
                    0.27f to 0.27f, 0.40f to 0.20f, 0.55f to 0.21f, 0.68f to 0.31f,
                    0.74f to 0.48f, 0.67f to 0.64f, 0.51f to 0.72f, 0.35f to 0.67f, 0.25f to 0.52f
                )
                beads.forEach { (x, y) -> drawCircle(gold, radius = w * 0.045f, center = p(x, y)) }
                drawLine(gold, p(0.50f, 0.72f), p(0.50f, 0.88f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
                drawLine(accent, p(0.43f, 0.88f), p(0.57f, 0.88f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
                drawArc(softGold, startAngle = 105f, sweepAngle = 220f, useCenter = false, topLeft = p(0.63f, 0.66f), size = Size(w * 0.18f, h * 0.18f), style = thinStroke)
            }

            fun drawHajjUmrah() {
                drawArc(gold, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = p(0.16f, 0.08f), size = Size(w * 0.68f, h * 0.70f), style = stroke)
                drawLine(gold, p(0.19f, 0.43f), p(0.19f, 0.78f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawLine(gold, p(0.81f, 0.43f), p(0.81f, 0.78f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
                drawRoundRect(gold.copy(alpha = fillAlpha), topLeft = p(0.33f, 0.50f), size = Size(w * 0.34f, h * 0.28f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.035f, w * 0.035f))
                drawRoundRect(gold, topLeft = p(0.33f, 0.50f), size = Size(w * 0.34f, h * 0.28f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.035f, w * 0.035f), style = thinStroke)
                drawLine(accent, p(0.33f, 0.60f), p(0.67f, 0.60f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
            }

            fun drawHadith() {
                val feather = Path().apply {
                    moveTo(w * 0.70f, h * 0.17f)
                    quadraticBezierTo(w * 0.24f, h * 0.28f, w * 0.36f, h * 0.72f)
                    quadraticBezierTo(w * 0.65f, h * 0.63f, w * 0.70f, h * 0.17f)
                    close()
                }
                drawPath(feather, color = gold.copy(alpha = 0.78f))
                drawLine(deepGold, p(0.33f, 0.75f), p(0.72f, 0.18f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
                drawLine(gold, p(0.30f, 0.82f), p(0.64f, 0.82f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)
            }

            fun drawHealth() {
                val heart = Path().apply {
                    moveTo(w * 0.50f, h * 0.76f)
                    cubicTo(w * 0.14f, h * 0.52f, w * 0.24f, h * 0.24f, w * 0.43f, h * 0.34f)
                    cubicTo(w * 0.50f, h * 0.18f, w * 0.79f, h * 0.25f, w * 0.74f, h * 0.50f)
                    cubicTo(w * 0.72f, h * 0.62f, w * 0.60f, h * 0.70f, w * 0.50f, h * 0.76f)
                    close()
                }
                drawPath(heart, color = gold.copy(alpha = 0.80f))
                drawLine(softGold, p(0.30f, 0.53f), p(0.42f, 0.53f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                drawLine(softGold, p(0.42f, 0.53f), p(0.49f, 0.42f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                drawLine(softGold, p(0.49f, 0.42f), p(0.57f, 0.60f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                drawLine(softGold, p(0.57f, 0.60f), p(0.70f, 0.60f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
            }

            fun drawNoorAi() {
                drawRoundRect(
                    color = gold.copy(alpha = fillAlpha),
                    topLeft = p(0.16f, 0.25f),
                    size = Size(w * 0.68f, h * 0.48f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, w * 0.12f)
                )
                drawRoundRect(
                    color = gold,
                    topLeft = p(0.16f, 0.25f),
                    size = Size(w * 0.68f, h * 0.48f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, w * 0.12f),
                    style = thinStroke
                )
                val tail = Path().apply {
                    moveTo(w * 0.34f, h * 0.72f)
                    lineTo(w * 0.28f, h * 0.86f)
                    lineTo(w * 0.48f, h * 0.73f)
                    close()
                }
                drawPath(tail, color = gold.copy(alpha = fillAlpha))
                drawPath(tail, color = gold, style = thinStroke)
                drawSimpleStar(w * 0.34f, h * 0.47f, w * 0.070f, w * 0.026f, 4, accent)
                drawSimpleStar(w * 0.58f, h * 0.44f, w * 0.095f, w * 0.036f, 4, gold)
                drawCircle(softGold, radius = w * 0.030f, center = p(0.69f, 0.55f))
            }

            when (iconKey) {
                QuickActionIconKey.Quran -> drawOpenBook()
                QuickActionIconKey.Audio -> drawAudio()
                QuickActionIconKey.Dua -> drawDua()
                QuickActionIconKey.PrayerTimes, QuickActionIconKey.Calendar -> drawCalendar(withClock = iconKey == QuickActionIconKey.PrayerTimes)
                QuickActionIconKey.Hadith -> drawHadith()
                QuickActionIconKey.Qibla -> drawCompass()
                QuickActionIconKey.Qaza -> drawScales()
                QuickActionIconKey.Zakat -> drawZakatHand()
                QuickActionIconKey.Bookmarks -> drawBookmark()
                QuickActionIconKey.Library -> drawBooks()
                QuickActionIconKey.Quiz -> drawSimpleStar(w * 0.50f, h * 0.50f, w * 0.37f, w * 0.18f, 5, gold)
                QuickActionIconKey.Tasbih -> drawTasbih()
                QuickActionIconKey.Azkar -> {
                    drawSimpleStar(w * 0.50f, h * 0.50f, w * 0.36f, w * 0.28f, 8, gold.copy(alpha = fillAlpha))
                    drawSimpleStar(w * 0.50f, h * 0.50f, w * 0.36f, w * 0.28f, 8, gold)
                    drawArc(accent, startAngle = 115f, sweepAngle = 235f, useCenter = false, topLeft = p(0.36f, 0.33f), size = Size(w * 0.26f, h * 0.26f), style = thinStroke)
                }
                QuickActionIconKey.HajjUmrah -> drawHajjUmrah()
                QuickActionIconKey.Asma -> {
                    drawCircle(gold.copy(alpha = fillAlpha), radius = w * 0.36f, center = p(0.50f, 0.50f))
                    drawCircle(gold, radius = w * 0.36f, center = p(0.50f, 0.50f), style = stroke)
                    drawSimpleStar(w * 0.70f, h * 0.30f, w * 0.055f, w * 0.022f, 4, accent)
                }
                QuickActionIconKey.Health -> drawHealth()
                QuickActionIconKey.NoorAi -> drawNoorAi()
            }
        }

        if (iconKey == QuickActionIconKey.Quiz || iconKey == QuickActionIconKey.Asma) {
            Text(
                text = if (iconKey == QuickActionIconKey.Asma) "99" else "?",
                color = if (iconKey == QuickActionIconKey.Quiz) Color.White else deepGold,
                fontWeight = FontWeight.Black,
                fontSize = if (iconKey == QuickActionIconKey.Asma) 18.sp else 24.sp
            )
        }
    }
}

@Composable
fun HijriCalendarCard(
    todayHijri: String,
    todayGregorian: String,
    strings: AppStrings,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLightTheme) Color.White.copy(alpha = 0.94f) else Color(0xFF071326).copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, if (isLightTheme) Color(0xFFCFE0D8) else MatteGold.copy(alpha = 0.38f)),
        modifier = modifier.fillMaxWidth().testTag("hijri_calendar_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TODAY'S DATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MatteGold,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = todayHijri,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontSize = 27.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todayGregorian,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isLightTheme) {
                                Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFEAF3)))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xFF1A2638), Color(0xFF342715)))
                            }
                        )
                        .border(1.dp, MatteGold.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MatteGold,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyStreakCalendar(
    weeklyLogs: List<com.example.data.PrayerLogEntity>,
    currentStreak: Int,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val dayNameFormat = java.text.SimpleDateFormat("E", java.util.Locale.US)
    val dayNumberFormat = java.text.SimpleDateFormat("d", java.util.Locale.US)
    
    val now = java.util.Date()
    val todayStr = dateFormat.format(now)
    
    val c = java.util.Calendar.getInstance()
    c.firstDayOfWeek = java.util.Calendar.MONDAY
    c.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
    val weekDays = (0..6).map { i ->
        val date = c.time
        c.add(java.util.Calendar.DAY_OF_YEAR, 1)
        date
    }

    val history = weeklyLogs.associateBy { it.date }

    androidx.compose.material3.Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = GlassOverlay),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Consistency",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .background(MatteGold.copy(alpha = 0.1f))
                        .border(1.dp, MatteGold.copy(alpha = 0.4f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = MatteGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currentStreak Day Streak",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MatteGold,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { date ->
                    val formattedDate = dateFormat.format(date)
                    val dayName = dayNameFormat.format(date).take(2)
                    val dayNumber = dayNumberFormat.format(date)
                    val isToday = formattedDate == todayStr
                    
                    val log = history[formattedDate]
                    var completedCount = 0
                    if (log != null) {
                        if (log.fajr) completedCount++
                        if (log.dhuhr) completedCount++
                        if (log.asr) completedCount++
                        if (log.maghrib) completedCount++
                        if (log.isha) completedCount++
                    }
                    val targetCompletion = completedCount / 5f
                    val completion by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = targetCompletion,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, delayMillis = 100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "calendarDayProgress"
                    )
                    val isPerfectDay = targetCompletion == 1.0f

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isToday) MatteGold else TextSecondary,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (isPerfectDay) MatteGold.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                                .border(
                                    width = if (isToday) 2.dp else 1.5.dp,
                                    color = if (isPerfectDay) MatteGold else if (isToday) MatteGold else TextSecondary.copy(alpha = 0.35f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (completion > 0f && !isPerfectDay) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    progress = completion,
                                    modifier = Modifier.fillMaxSize(),
                                    color = MatteGold.copy(alpha = 0.5f),
                                    trackColor = androidx.compose.ui.graphics.Color.Transparent,
                                    strokeWidth = 2.5.dp
                                )
                            }
                            
                            if (isPerfectDay) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Perfect Day",
                                    tint = MatteGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = dayNumber,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isToday) MatteGold else TextPrimary,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerPunctualityChart(
    weeklyLogs: List<com.example.data.PrayerLogEntity>,
    modifier: Modifier = Modifier
) {
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val dayNameFormat = java.text.SimpleDateFormat("E", java.util.Locale.US)
    
    val c = java.util.Calendar.getInstance()
    c.add(java.util.Calendar.DAY_OF_YEAR, -6)
    
    val last7Days = (0..6).map { i ->
        val date = c.time
        c.add(java.util.Calendar.DAY_OF_YEAR, 1)
        dateFormat.format(date) to dayNameFormat.format(date).take(3)
    }

    val history = weeklyLogs.associateBy { it.date }
    val dataPoints = last7Days.map { (dateStr, dayName) ->
        val log = history[dateStr]
        var count = 0
        if (log != null) {
            if (log.fajr) count++
            if (log.dhuhr) count++
            if (log.asr) count++
            if (log.maghrib) count++
            if (log.isha) count++
        }
        dayName to count
    }

    val chartColor = MatteGold
    val backgroundColor = Color(0xFFCBD5E1).copy(alpha = 0.2f)
    val textColor = TextSecondary

    androidx.compose.material3.Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = GlassOverlay),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Punctuality (Past 7 Days)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                var animationPlayed by remember { mutableStateOf(false) }
                val animationProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (animationPlayed) 1f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, delayMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    label = "barAnimation"
                )
                LaunchedEffect(Unit) {
                    animationPlayed = true
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxPrayers = 5f
                    val barSpacing = size.width / (dataPoints.size * 2f)
                    val barWidth = barSpacing * 0.8f
                    val chartHeight = size.height - 30.dp.toPx() // Leave room for labels

                    dataPoints.forEachIndexed { index, (_, count) ->
                        val x = (index * 2 * barSpacing) + barSpacing
                        
                        // Draw background bar
                        drawRoundRect(
                            color = backgroundColor,
                            topLeft = Offset(x - barWidth / 2, 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, chartHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
                        )

                        // Draw foreground bar (actual data)
                        val targetBarHeight = (count / maxPrayers) * chartHeight
                        val barHeight = targetBarHeight * animationProgress
                        drawRoundRect(
                            color = chartColor,
                            topLeft = Offset(x - barWidth / 2, chartHeight - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
                        )
                    }
                }
                
                // Draw labels using Compose Text for better typography
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(top = 130.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dataPoints.forEach { (dayName, _) ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun MonthlyGoalProgressCard(
    monthlyLogs: List<com.example.data.PrayerLogEntity>,
    currentGoal: Int,
    isEditingGoal: Boolean,
    onEditGoalClick: (Boolean) -> Unit,
    onGoalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPrayers = monthlyLogs.sumOf { log ->
        var count = 0
        if (log.fajr) count++
        if (log.dhuhr) count++
        if (log.asr) count++
        if (log.maghrib) count++
        if (log.isha) count++
        count
    }

    val progress = if (currentGoal > 0) (totalPrayers.toFloat() / currentGoal).coerceIn(0f, 1f) else 0f

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) progress else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, delayMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "monthlyProgressAnim"
    )
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    androidx.compose.material3.Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = GlassOverlay),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Goal",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                if (!isEditingGoal) {
                    androidx.compose.material3.IconButton(
                        onClick = { onEditGoalClick(true) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Goal",
                            tint = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (isEditingGoal) {
                var goalInput by remember { mutableStateOf(currentGoal.toString()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it.filter { char -> char.isDigit() } },
                        label = { Text("New Goal") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MatteGold,
                            cursorColor = MatteGold
                        )
                    )
                    androidx.compose.material3.Button(
                        onClick = { 
                            val parsed = goalInput.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                onGoalChange(parsed)
                            } else {
                                onEditGoalClick(false)
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MatteGold)
                    ) {
                        Text("Save", color = NightBackground)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalPrayers",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = MatteGold)
                    )
                    Text(
                        text = "/ $currentGoal prayers",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.material3.LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    color = MatteGold,
                    trackColor = TextSecondary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun DailyPrayerTrackerCard(
    log: com.example.data.PrayerLogEntity?,
    onTogglePrayer: (String) -> Unit,
    onQazaShortcut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var completedCount = 0
    if (log != null) {
        if (log.fajr) completedCount++
        if (log.dhuhr) completedCount++
        if (log.asr) completedCount++
        if (log.maghrib) completedCount++
        if (log.isha) completedCount++
    }
    val targetProgress = completedCount / 5f
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, delayMillis = 100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "dailyTrackerProgress"
    )

    androidx.compose.material3.Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = GlassOverlay),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row with Progress Ring
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Tracker",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MatteGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Today's Prayers",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.W700
                        )
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(50.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxSize(),
                        color = MatteGold,
                        trackColor = TextSecondary.copy(alpha = 0.2f),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "$completedCount/5",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Prayer Checkboxes Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val prayers = listOf(
                    Triple("Fajr", log?.fajr ?: false, Icons.Default.WbTwilight),
                    Triple("Dhuhr", log?.dhuhr ?: false, Icons.Default.WbSunny),
                    Triple("Asr", log?.asr ?: false, Icons.Default.LightMode),
                    Triple("Maghrib", log?.maghrib ?: false, Icons.Default.WbTwilight),
                    Triple("Isha", log?.isha ?: false, Icons.Default.NightsStay)
                )

                prayers.forEach { (name, isCompleted, icon) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onTogglePrayer(name)
                            },
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (isCompleted) MatteGold else androidx.compose.ui.graphics.Color.Transparent)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isCompleted) MatteGold else TextSecondary.copy(alpha = 0.5f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color(0xFF050C18),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                androidx.compose.material3.Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = MatteGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isCompleted) MatteGold else TextSecondary,
                                fontWeight = if (isCompleted) FontWeight.W600 else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onQazaShortcut) {
                    Text("Log as Qaza", color = MatteGold)
                }
            }
        }
    }
}
