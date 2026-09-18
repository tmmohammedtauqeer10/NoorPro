package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.SectionHeader
import com.example.ui.components.StitchCard
import com.example.ui.components.StitchEmerald
import com.example.ui.components.StitchLine
import com.example.ui.components.StitchScreen
import com.example.ui.components.stitchMutedText
import com.example.ui.components.stitchPrimary
import com.example.ui.components.stitchSoftSurface
import com.example.ui.components.stitchSurface
import com.example.ui.components.stitchText
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PrayerTimesScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val prayers by viewModel.prayers.collectAsState()
    val activeIndex by viewModel.activePrayerIndex.collectAsState()
    val countdown by viewModel.nextPrayerCountdown.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    
    val todayHijri by viewModel.todayHijri.collectAsState()
    val todayGregorian by viewModel.todayGregorian.collectAsState()
    val currentLocationName by viewModel.currentLocationName.collectAsState()
    
    val weeklyLogs by viewModel.weeklyPrayerLogs.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val todayPrayerLog by viewModel.todayPrayerLog.collectAsState()
    val monthlyLogs by viewModel.currentMonthLogs.collectAsState()
    val monthlyGoal by viewModel.userPreferencesRepo.monthlyPrayerGoalFlow.collectAsState(initial = 150)
    var isEditingGoal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val nextIndex = prayers.indexOfFirst { it.name == nextPrayerName }.let { if (it < 0) activeIndex else it }

    StitchScreen {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 90.dp) // Space for bottom navbar
        ) {
            Spacer(modifier = Modifier.padding(top = 40.dp))

            // 1. Header: avatar + Al-Noor + bell
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(StitchEmerald)
                        .clickable { viewModel.navigateTo(com.example.ui.viewmodel.DeenScreen.PROFILE_DASHBOARD) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("Al-Noor", color = stitchPrimary(), fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = stitchPrimary(), modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Location pill + countdown headline + Hijri date (centered)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(stitchSoftSurface())
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = stitchMutedText(), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(currentLocationName.ifBlank { "Locating…" }, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Calm crossfade when next prayer or countdown ticks (Islamic-app subtle motion).
                AnimatedContent(
                    targetState = nextPrayerName to countdown,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(320)) togetherWith
                            fadeOut(animationSpec = tween(240))
                    },
                    label = "prayerCountdown",
                ) { (name, cd) ->
                    Text(
                        text = "$name is ${friendlyCountdown(cd)}",
                        color = stitchPrimary(),
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LibreCaslon,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(todayHijri.ifBlank { todayGregorian }, color = stitchMutedText(), fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Vertical Timeline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Draw continuous line for vertical timeline
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                        .padding(vertical = 24.dp)
                ) {
                    drawLine(
                        color = StitchLine,
                        start = Offset(22.dp.toPx(), 0.0f),
                        end = Offset(22.dp.toPx(), size.height),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(prayers) { index, prayer ->
                        PrayerTimesRowItem(
                            prayer = prayer,
                            displayTime = viewModel.displayPrayerTime(prayer.time),
                            isActive = prayer.name == nextPrayerName,
                            isPassed = index < nextIndex,
                            onNotifyToggle = { viewModel.toggleNotification(prayer.name) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(title = "Weekly Prayer Tracker")

                    WeeklyStreakCalendar(
                        weeklyLogs = weeklyLogs,
                        currentStreak = currentStreak
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
        }
    }
}

@Composable
fun PrayerTimesRowItem(
    prayer: PrayerTime,
    displayTime: String,
    isActive: Boolean,
    isPassed: Boolean,
    onNotifyToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSunrise = prayer.name.equals("Sunrise", ignoreCase = true)
    val nameColor = when {
        isActive -> Color.White
        isPassed || isSunrise -> stitchMutedText()
        else -> stitchText()
    }
    val timeColor = when {
        isActive -> Color.White
        isPassed || isSunrise -> stitchMutedText()
        else -> stitchText()
    }

    val glyphBg by animateColorAsState(
        targetValue = if (isActive) StitchEmerald else stitchSurface(),
        animationSpec = tween(380),
        label = "prayerGlyphBg",
    )
    val glyphBorder by animateColorAsState(
        targetValue = if (isActive) StitchEmerald else StitchLine,
        animationSpec = tween(380),
        label = "prayerGlyphBorder",
    )
    val cardBg by animateColorAsState(
        targetValue = if (isActive) StitchEmerald else stitchSurface(),
        animationSpec = tween(380),
        label = "prayerCardBg",
    )
    val highlightSlide by animateDpAsState(
        targetValue = if (isActive) 6.dp else 0.dp,
        animationSpec = tween(380),
        label = "prayerHighlightSlide",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = highlightSlide)
            .testTag("prayer_row_${prayer.name}")
    ) {
        // Left timeline glyph
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(glyphBg)
                    .border(1.dp, glyphBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = prayerGlyph(prayer.name),
                    contentDescription = null,
                    tint = if (isActive) Color.White else if (isPassed) stitchMutedText() else stitchPrimary(),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isSunrise) {
            // Sunrise: plain row, no card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Text(prayer.name, color = stitchMutedText(), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                Text(displayTime, color = stitchMutedText(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            return@Row
        }

        // Prayer card (active = filled emerald; color+slide animate when next prayer changes)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .then(
                    if (!isActive) Modifier.border(1.dp, StitchLine, RoundedCornerShape(18.dp))
                    else Modifier
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        text = prayer.name,
                        fontFamily = LibreCaslon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = nameColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) Color.White.copy(alpha = 0.85f) else timeColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Alarm Toggle Switch button icon
                    IconButton(
                        onClick = onNotifyToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (prayer.isNotificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Notification alert toggle",
                            tint = when {
                                isActive -> Color.White
                                prayer.isNotificationEnabled -> stitchPrimary()
                                else -> stitchMutedText()
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
