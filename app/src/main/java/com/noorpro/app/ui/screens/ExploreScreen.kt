package com.noorpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.R
import com.noorpro.app.ui.theme.GlassBorder
import com.noorpro.app.ui.theme.GlassOverlay
import com.noorpro.app.ui.theme.TextPrimary
import com.noorpro.app.ui.theme.TextSecondary
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

private data class ExploreTool(
    val title: String,
    val description: String,
    val category: String,
    val icon: ImageVector,
    val screen: DeenScreen,
    val imageRes: Int? = null
)

private val exploreTools = listOf(
    ExploreTool("Prayer Times", "Daily salah times and notification controls", "Worship", Icons.Default.AccessTime, DeenScreen.PRAYER_TIMES),
    ExploreTool("Qibla", "Find the direction of the Kaaba", "Worship", Icons.Default.Explore, DeenScreen.QIBLA_MORE),
    ExploreTool("Qaza Tracker", "Record and manage missed prayers", "Worship", Icons.Default.History, DeenScreen.QAZA_TRACKER),
    ExploreTool("Azkar", "Morning, evening, and daily remembrance", "Worship", Icons.Default.Favorite, DeenScreen.AZKAR),
    ExploreTool("Dua Collection", "Supplications organized by occasion", "Worship", Icons.Default.Language, DeenScreen.DUA_HUB),
    ExploreTool("Tasbih", "Simple digital dhikr counter", "Worship", Icons.Default.TouchApp, DeenScreen.TASBIH),
    ExploreTool("Hajj & Umrah", "Step-by-step rituals, flights, hotels, and package planning", "Worship", Icons.Default.TravelExplore, DeenScreen.HAJJ_UMRAH),
    ExploreTool("Noor AI", "Ask Islamic questions, generate duas, and explain Quran or Hadith safely", "AI", Icons.Default.AutoAwesome, DeenScreen.AI_HUB),
    ExploreTool("Quran", "Read chapters, translations, and recitations", "Quran", Icons.AutoMirrored.Filled.MenuBook, DeenScreen.QURAN),
    ExploreTool("Audio", "Quran audio playlists and recitations", "Quran", Icons.Default.Audiotrack, DeenScreen.AUDIO_LIBRARY),
    ExploreTool("Al Noor Audio", "Copyright-free nasheed & naat (Islamic Spotify)", "Worship", Icons.Default.Audiotrack, DeenScreen.AL_NOOR_AUDIO),
    ExploreTool("Indo-Pak Quran", "Read with Indo-Pak script styling", "Quran", Icons.AutoMirrored.Filled.MenuBook, DeenScreen.INDOPAK_QURAN),
    ExploreTool("Quran Bookmarks", "Return to saved verses and reading points", "Quran", Icons.Default.Bookmark, DeenScreen.BOOKMARKS),
    ExploreTool("Quran Learning", "Learn Quran with guided progress", "Learning", Icons.Default.School, DeenScreen.QURAN_LEARNING_DASHBOARD),
    ExploreTool("Qaida Tutor", "Practice Arabic letters and foundational reading", "Learning", Icons.Default.School, DeenScreen.QAIDA_TUTOR),
    ExploreTool("Hadith Library", "Browse supported Hadith collections", "Learning", Icons.Default.LibraryBooks, DeenScreen.HADITH_LIBRARY),
    ExploreTool("Islamic Library", "Read books from the verified catalog", "Learning", Icons.Default.LibraryBooks, DeenScreen.LIBRARY_DASHBOARD),
    ExploreTool("Ummah", "Approved Islamic reels, images, and reminders", "Daily Life", Icons.Default.Public, DeenScreen.UMMAH),
    ExploreTool("Knowledge Quiz", "Test and strengthen Islamic knowledge", "Learning", Icons.Default.Star, DeenScreen.QUIZ_DASHBOARD),
    ExploreTool("99 Names of Allah", "Read and reflect on Asma ul Husna", "Daily Life", Icons.Default.Favorite, DeenScreen.ASMA_UL_HUSNA),
    ExploreTool("Hijri Calendar", "View Islamic dates and important events", "Daily Life", Icons.Default.CalendarMonth, DeenScreen.CALENDAR),
    ExploreTool("Zakat Calculator", "Calculate using current prices you provide", "Daily Life", Icons.Default.Calculate, DeenScreen.ZAKAT),
    ExploreTool("Health & Wellness", "Fasting, hydration, sleep, and Sunnah foods", "Daily Life", Icons.Default.HealthAndSafety, DeenScreen.HEALTH_WELLNESS),
    ExploreTool("Settings", "Prayer methods, appearance, and app preferences", "Personal", Icons.Default.Settings, DeenScreen.SETTINGS)
)

// Recolored gold → green to match the app's accent color.
private val MatteGold = Color(0xFF0E8C73)

@Composable
fun ExploreScreen(viewModel: DeenViewModel) {
    var query by remember { mutableStateOf("") }
    val filteredTools = remember(query) {
        if (query.isBlank()) {
            exploreTools
        } else {
            exploreTools.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 48.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Explore",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "All your reliable Islamic tools in one place",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search Quran, prayer, dua, learning...") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredTools.isEmpty()) {
            item {
                Text(
                    text = "No tools match \"$query\".",
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            filteredTools.groupBy { it.category }.forEach { (category, tools) ->
                item {
                    Text(
                        text = category,
                        color = MatteGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }
                items(tools, key = { it.title }) { tool ->
                    ExploreToolCard(tool = tool, onClick = { viewModel.navigateTo(tool.screen) })
                }
            }
        }
    }
}

@Composable
private fun ExploreToolCard(tool: ExploreTool, onClick: () -> Unit) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val iconTileGradient = if (isLightTheme) {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFEAF3)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF101D32), Color(0xFF07101F)))
    }
    val iconBorder = if (isLightTheme) Color(0xFFCFE0D8) else MatteGold.copy(alpha = .18f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isLightTheme) Color.White.copy(alpha = 0.92f) else GlassOverlay),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (tool.imageRes != null) iconBorder else GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconTileGradient)
                    .border(1.dp, iconBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Simple Material icon, tinted for visibility in both light and dark themes.
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = if (isLightTheme) Color(0xFF0E8C73) else MatteGold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(tool.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    tool.description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
