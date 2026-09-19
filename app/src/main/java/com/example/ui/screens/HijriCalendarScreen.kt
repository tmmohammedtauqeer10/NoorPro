package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel

// Recolored gold → green to match the app's accent color (local vals shadow the theme's gold tokens).
private val MatteGold = Color(0xFF0E8C73)
private val GlowGold = Color(0xFF17B892)
private val LightGold = Color(0xFF7FD1BD)

@Composable
fun HijriCalendarScreen(viewModel: DeenViewModel, modifier: Modifier = Modifier) {
    val todayHijri by viewModel.todayHijri.collectAsState()
    val todayGregorian by viewModel.todayGregorian.collectAsState()
    val calendarDays by viewModel.hijriCalendarDays.collectAsState()
    val isLoading by viewModel.isLoadingCalendar.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.padding(top = 54.dp))

        // Navigation Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassOverlay)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MatteGold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Hijri Calendar",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = todayGregorian,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading && calendarDays.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GlowGold)
            }
        } else {
            // Beautiful Events & Calendar Section
            Text(
                text = "MONTHLY HIJRI OVERVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(calendarDays.size) { index ->
                    val day = calendarDays[index]
                    val parts = todayHijri.split(" ")
                    val currentDay = parts.firstOrNull()?.toIntOrNull() ?: -1
                    val isToday = day.hijriDay.toIntOrNull() == currentDay
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isToday) MatteGold.copy(alpha=0.15f) else GlassOverlay)
                            .border(if (isToday) 1.dp else 0.dp, if (isToday) GlowGold else Color.Transparent, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${day.hijriDay} ${day.hijriMonthEn}",
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isToday) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(GlowGold).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("TODAY", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black))
                                    }
                                }
                            }
                            
                            Text(
                                text = "${day.gregorianDate} - ${day.hijriWeekdayEn}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            
                            if (day.holidays.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE57373).copy(alpha=0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(
                                        text = day.holidays.joinToString(", "),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEF9A9A), fontWeight=FontWeight.Bold)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = day.hijriWeekdayAr,
                            style = MaterialTheme.typography.titleLarge.copy(color = MatteGold, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }
    }
}
