package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel

// Recolored from gold → green/white: local constants shadow the theme's gold tokens so the
// profile ("My") section matches the app's green + white look.
private val MatteGold = Color(0xFF0E8C73)
private val GlowGold = Color(0xFF17B892)

@Composable
fun ProfileDashboardScreen(viewModel: DeenViewModel) {
    val points by viewModel.totalPoints.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val completedSurahs by viewModel.completedSurahs.collectAsState()
    val isLoggedIn = viewModel.isLoggedIn
    val displayName = viewModel.userDisplayName.ifBlank { if (isLoggedIn) "User" else "Guest" }
    val playingSurah by viewModel.playingSurah.collectAsState()
    // The floating mini audio player (~166dp tall above the system nav) overlays this
    // screen while audio is playing; reserve extra bottom space so the last option
    // (Settings) isn't hidden behind it.
    val bottomInset = if (playingSurah != null) 210.dp else 136.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            )
        ),
        contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        item {
            Text("My Noor", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Profile & Progress", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .76f), MaterialTheme.colorScheme.surface)
                        )
                    ).padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(78.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFFB58A20))))
                                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoggedIn) Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            else Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(42.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(displayName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text(if (isLoggedIn) viewModel.userEmail else "Begin your daily journey", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                            Spacer(Modifier.height(8.dp))
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(20.dp)) {
                                Text("Level ${(points / 250) + 1} companion", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            NoorPointsCard(points = points, streak = streak, completedSurahs = completedSurahs.size)
        }
        item {
            Text("Every prayer, verse, and remembrance builds your Noor journey.", color = TextSecondary, fontSize = 12.sp)
        }

        if (!isLoggedIn) {
            item {
                Button(
                    onClick = { viewModel.navigateTo(DeenScreen.LOGIN) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MatteGold)
                ) {
                    Text("Login / Create Account", color = NightBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            ProfileSectionTitle("Your Journey")
            ProfileOption("My Bookmarks", Icons.Default.Bookmark) { viewModel.navigateTo(DeenScreen.BOOKMARKS) }
            Spacer(modifier = Modifier.height(10.dp))
            ProfileOption("Prayer Progress", Icons.Default.Insights) { viewModel.navigateTo(DeenScreen.PRAYER_TIMES) }
            Spacer(modifier = Modifier.height(10.dp))
            ProfileOption("Learning & Quiz", Icons.Default.School) { viewModel.navigateTo(DeenScreen.EDUCATION) }
            Spacer(modifier = Modifier.height(10.dp))
            ProfileOption("Explore All Tools", Icons.Default.Apps) { viewModel.navigateTo(DeenScreen.EXPLORE) }
            Spacer(modifier = Modifier.height(20.dp))
            ProfileSectionTitle("Community & Essentials")
            Spacer(modifier = Modifier.height(10.dp))
            ProfileOption("Ummah Community", Icons.Default.Public) { viewModel.navigateTo(DeenScreen.UMMAH) }
            Spacer(modifier = Modifier.height(10.dp))
            ProfileOption("Hijri Calendar", Icons.Default.CalendarMonth) { viewModel.navigateTo(DeenScreen.CALENDAR) }
            Spacer(modifier = Modifier.height(10.dp))
            ProfileOption("Zakat Calculator", Icons.Default.Calculate) { viewModel.navigateTo(DeenScreen.ZAKAT) }
            Spacer(modifier = Modifier.height(20.dp))
            ProfileSectionTitle("Account")
            if (isLoggedIn) {
                Spacer(modifier = Modifier.height(10.dp))
                ProfileOption("Logout", Icons.Default.Logout, Color(0xFFE57373)) {
                    viewModel.handleLogout()
                    viewModel.navigateTo(DeenScreen.LOGIN)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            ProfileOption("Settings", Icons.Default.Settings) { viewModel.navigateTo(DeenScreen.SETTINGS) }
        }
    }
}

@Composable
fun NoorPointsCard(points: Int, streak: Int, completedSurahs: Int) {
    val level = (points / 250) + 1
    val levelProgress = (points % 250) / 250f
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = GlowGold, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("$points Noor Points", color = MaterialTheme.colorScheme.primary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Level $level | ${250 - (points % 250)} points to next level", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { levelProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color = MatteGold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ProfileStat("$streak", "Day streak")
                ProfileStat("$completedSurahs", "Surahs")
                ProfileStat("$level", "Level")
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    com.example.ui.components.SectionHeader(title = title)
}

@Composable
private fun ProfileOption(title: String, icon: ImageVector, tint: Color = TextPrimary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (tint == TextPrimary) MaterialTheme.colorScheme.primary else tint, modifier = Modifier.size(21.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, color = tint, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
