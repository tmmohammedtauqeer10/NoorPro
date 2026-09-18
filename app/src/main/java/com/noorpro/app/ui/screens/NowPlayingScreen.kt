package com.noorpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.IslamicData
import com.noorpro.app.data.AudioCategory
import com.noorpro.app.data.AudioItem
import com.noorpro.app.data.Surah
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

private fun completeAudioSurahList(): List<Surah> = (1..114).map { id ->
    IslamicData.surahs.find { it.id == id } ?: Surah(
        id = id,
        nameEnglish = com.noorpro.app.data.QuranMetaData.surahNamesEn[id - 1],
        nameArabic = com.noorpro.app.data.QuranMetaData.surahNamesAr[id - 1],
        englishTranslation = com.noorpro.app.data.QuranMetaData.surahTranslations[id - 1],
        versesCount = IslamicData.surahAyahsCount[id] ?: 0,
        type = com.noorpro.app.data.QuranMetaData.surahTypes[id - 1],
        verses = emptyList()
    )
}

@Composable
fun IslamicWallpaperCover(
    surah: Surah?,
    modifier: Modifier = Modifier
) {
    val metallicGold = Color(0xFFD4AF37)
    
    // Choose majestic theme dynamically based on Makkiyah vs Madaniyah
    val bgGradientColors = when {
        surah == null -> listOf(Color(0xFF152243), Color(0xFF0B132B))
        surah.type.contains("Makk", ignoreCase = true) || surah.type.contains("Mecc", ignoreCase = true) -> {
            // Celestial velvet night royal indigo skies for Makkah
            listOf(Color(0xFF1B2349), Color(0xFF090E1F))
        }
        else -> {
            // Majestic spiritual deep emerald and forest gold green for Madinah
            listOf(Color(0xFF0C2B1D), Color(0xFF040E0A))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(colors = bgGradientColors),
                shape = RoundedCornerShape(20.dp)
            )
            .border(2.dp, metallicGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width * 0.42f
            val innerRadius = size.width * 0.33f

            // Radial Glow
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(metallicGold.copy(alpha = 0.12f), Color.Transparent),
                    center = centerOffset,
                    radius = outerRadius
                ),
                radius = outerRadius
            )

            // Pattern circles
            drawCircle(
                color = metallicGold.copy(alpha = 0.4f),
                radius = outerRadius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            drawCircle(
                color = metallicGold.copy(alpha = 0.2f),
                radius = innerRadius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            )

            // Mathematical Rub el Hizb (8-pointed star) in background
            val starSize = innerRadius * 0.75f
            rotate(0f, centerOffset) {
                drawRect(
                    color = metallicGold.copy(alpha = 0.08f),
                    topLeft = androidx.compose.ui.geometry.Offset(centerOffset.x - starSize, centerOffset.y - starSize),
                    size = androidx.compose.ui.geometry.Size(starSize * 2, starSize * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                )
            }
            rotate(45f, centerOffset) {
                drawRect(
                    color = metallicGold.copy(alpha = 0.08f),
                    topLeft = androidx.compose.ui.geometry.Offset(centerOffset.x - starSize, centerOffset.y - starSize),
                    size = androidx.compose.ui.geometry.Size(starSize * 2, starSize * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                )
            }

            // Outer Mehrab Arch Path
            val path = androidx.compose.ui.graphics.Path().apply {
                val w = size.width
                val h = size.height
                moveTo(w * 0.15f, h * 0.88f)
                lineTo(w * 0.15f, h * 0.45f)
                cubicTo(w * 0.15f, h * 0.28f, w * 0.32f, h * 0.15f, w * 0.5f, h * 0.10f)
                cubicTo(w * 0.68f, h * 0.15f, w * 0.85f, h * 0.28f, w * 0.85f, h * 0.45f)
                lineTo(w * 0.85f, h * 0.88f)
                close()
            }

            drawPath(
                path = path,
                color = metallicGold.copy(alpha = 0.08f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }

        // Beautiful layered texts
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = (surah?.type ?: "RECITATION").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = metallicGold,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = surah?.nameArabic ?: "القرآن الكريم",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = metallicGold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = surah?.let { "SURAH ${it.id}" } ?: "TAP SELECT SURAH",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = surah?.nameEnglish ?: "Choose an audio recitation",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
private fun AudioIntroCard(
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val surface = if (isLight) Color(0xFFFFFFFF) else Color(0xFF101B32).copy(alpha = 0.82f)
    val textPrimary = if (isLight) Color(0xFF241019) else Color.White
    val textSecondary = if (isLight) Color(0xFF8A7079) else Color(0xFFB0B0B0)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF7B5615)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0B132B))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Noor Audio", color = textPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    "Your Islamic audio hub for Quran recitation, nasheeds, playlists, podcasts, seerah, tafsir, naat, and hamd.",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun AudioTrackCover(
    track: AudioItem?,
    modifier: Modifier = Modifier
) {
    val metallicGold = Color(0xFFD4AF37)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF241832), Color(0xFF0B132B), Color(0xFF07101F))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(2.dp, metallicGold.copy(alpha = 0.28f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(metallicGold.copy(alpha = 0.18f), Color.Transparent),
                    center = center,
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f
            )
            repeat(5) { index ->
                drawCircle(
                    color = metallicGold.copy(alpha = 0.08f + index * 0.025f),
                    radius = size.width * (0.16f + index * 0.055f),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(metallicGold, Color(0xFF7B5615)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0B132B), modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = track?.title ?: "Noor Audio",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = track?.artist ?: "Choose Islamic audio below",
                color = metallicGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AudioDiscoveryPanel(
    categories: List<AudioCategory>,
    isLoading: Boolean,
    error: String?,
    currentTrack: AudioItem?,
    onTrackClick: (AudioItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val textPrimary = if (isLight) Color(0xFF241019) else Color.White
    val textSecondary = if (isLight) Color(0xFF8A7079) else Color(0xFFB0B0B0)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color(0xFFD4AF37),
                strokeWidth = 2.dp
            )
        }
        if (error != null) {
            Text(error, color = Color(0xFFFFC0CB), style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Nasheeds, playlists, Islamic songs, seerah, tafsir, podcasts, and reminders from your Deenflow catalog.",
            color = textSecondary,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 17.sp
        )
        categories.forEach { category ->
            AudioCategorySection(
                category = category,
                currentTrack = currentTrack,
                onTrackClick = onTrackClick
            )
        }
    }
}

@Composable
private fun AudioCategorySection(
    category: AudioCategory,
    currentTrack: AudioItem?,
    onTrackClick: (AudioItem) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val surface = if (isLight) Color(0xFFFFFFFF) else Color(0xFF101B32).copy(alpha = 0.72f)
    val textPrimary = if (isLight) Color(0xFF241019) else Color.White
    val textSecondary = if (isLight) Color(0xFF8A7079) else Color(0xFFB0B0B0)
    val goldAccent = if (isLight) Color(0xFFB07D08) else Color(0xFFD4AF37)
    val rowActive = if (isLight) Color(0xFFF6DCE7) else Color(0xFF253B6A)
    val rowInactive = if (isLight) Color(0xFFF7EEF1) else Color(0xFF0B132B).copy(alpha = 0.64f)
    val circleInactive = if (isLight) Color(0xFFE7D5DD) else Color(0xFF172542)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.16f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(category.categoryTitle, color = goldAccent, fontWeight = FontWeight.Black)
            category.description?.let {
                Text(it, color = textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            category.items.take(6).forEach { track ->
                val isActive = currentTrack?.id == track.id
                val canPlay = track.audioUrl.isNotBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) rowActive else rowInactive)
                        .clickable(enabled = canPlay) { onTrackClick(track) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isActive) goldAccent else circleInactive),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isActive) Color(0xFF0B132B) else textPrimary.copy(alpha = if (canPlay) 1f else 0.55f)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = textPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            listOfNotNull(track.artist, track.language, track.duration).joinToString(" • "),
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    if (!canPlay) {
                        Text("Soon", color = goldAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val audioDuration by viewModel.audioDuration.collectAsState()
    val audioPosition by viewModel.audioPosition.collectAsState()
    val audioProgress by viewModel.audioProgress.collectAsState()
    val audioCategories by viewModel.audioCategories.collectAsState()
    val isAudioCatalogLoading by viewModel.isAudioCatalogLoading.collectAsState()
    val audioCatalogError by viewModel.audioCatalogError.collectAsState()
    val currentAudioTrack by viewModel.currentAudioTrack.collectAsState()
    
    val selectedSurah by viewModel.playingSurah.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val audioSurahs = remember { completeAudioSurahList() }

    var selectedTab by remember { mutableStateOf(0) } // 0: Player, 1: Surahs, 2: Reciters
    var showSurahDialog by remember { mutableStateOf(false) }
    var showQariDialog by remember { mutableStateOf(false) }

    // Pre-populate with first Surah on launch if none is selected
    LaunchedEffect(Unit) {
        viewModel.loadAudioCatalog()
        if (selectedSurah == null) {
            viewModel.selectSurahForPlayback(audioSurahs[0], triggerPlayback = false)
        }
    }

    // Design Theme Colors — page chrome adapts to light/dark; the album-art covers
    // (IslamicWallpaperCover / AudioTrackCover) stay rich and dark by design.
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val metallicGold = if (isLight) Color(0xFFB07D08) else Color(0xFFD4AF37)
    val deepSpaceBlue = Color(0xFF0B132B) // dark ink used on gold buttons in both themes
    val darkSlateBlue = if (isLight) Color(0xFFFFFFFF) else Color(0xFF152243) // dropdown surface
    val mutedSilver = if (isLight) Color(0xFF8A7079) else Color(0xFFB0B0B0)
    val textPrimary = if (isLight) Color(0xFF241019) else Color.White
    val pageTop = if (isLight) Color(0xFFFFFFFF) else Color(0xFF152243)
    val pageBottom = if (isLight) Color(0xFFFCEDF2) else Color(0xFF0B132B)
    val selectorSurface = if (isLight) Color(0xFFFBF1F4) else Color(0xFF152243).copy(alpha = 0.6f)
    val chipActive = if (isLight) Color(0xFFF6DCE7) else Color(0xFF1E2F54)
    val chipInactiveSurface = if (isLight) Color(0xFFF7EEF1) else Color(0xFF152243).copy(alpha = 0.5f)
    val circleInactive = if (isLight) Color(0xFFE7D5DD) else Color(0xFF1C2D54)
    val dialogSurface = if (isLight) Color(0xFFFFFFFF) else Color(0xFF0B132B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(pageTop, pageBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 90.dp) // Leave safety padding at bottom for navigation bar
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "AUDIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = mutedSilver,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Playback Speed Selector (Always available in top corner)
                val currentSpeed by viewModel.audioPlaybackSpeed.collectAsState()
                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            text = "${currentSpeed}x",
                            color = metallicGold,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(darkSlateBlue)
                    ) {
                        speeds.forEach { speed ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "${speed}x", 
                                        color = if (speed == currentSpeed) metallicGold else textPrimary
                                    ) 
                                },
                                onClick = {
                                    viewModel.setAudioPlaybackSpeed(speed)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AudioIntroCard(
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: PLAYER INTERFACE with Unified Quick Selection Cards
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Breathtaking Islamic Wallpaper Dynamic Cover
                        if (currentAudioTrack != null) {
                            AudioTrackCover(
                                track = currentAudioTrack,
                                modifier = Modifier.fillMaxWidth(0.92f)
                            )
                        } else {
                            IslamicWallpaperCover(
                                surah = selectedSurah,
                                modifier = Modifier.fillMaxWidth(0.92f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Recitations Selector cards: Direct selection before triggering playback!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Surah Pick Custom Selector
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showSurahDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = selectorSurface
                                ),
                                border = BorderStroke(1.dp, metallicGold.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Select Surah",
                                        tint = metallicGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "SURAH",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = metallicGold,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Text(
                                            text = selectedSurah?.nameEnglish ?: "Choose...",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Qari Pick Custom Selector
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showQariDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = selectorSurface
                                ),
                                border = BorderStroke(1.dp, metallicGold.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Select Qari",
                                        tint = metallicGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "QARI",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = metallicGold,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Text(
                                            text = selectedReciter.name.split(" ").lastOrNull() ?: selectedReciter.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        AudioDiscoveryPanel(
                            categories = audioCategories,
                            isLoading = isAudioCatalogLoading,
                            error = audioCatalogError,
                            currentTrack = currentAudioTrack,
                            onTrackClick = { viewModel.playAudioTrack(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Active Selections Info Panel
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentAudioTrack?.title ?: selectedSurah?.englishTranslation ?: "No Audio Selected",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentAudioTrack?.let { "${it.artist} • ${it.language ?: "Islamic audio"}" }
                                    ?: "Quran audio by ${selectedReciter.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = mutedSilver
                                ),
                                maxLines = 1
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Progress Slider panel
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = audioProgress,
                                onValueChange = { viewModel.seekAudio(it) },
                                colors = SliderDefaults.colors(
                                    thumbColor = metallicGold,
                                    activeTrackColor = metallicGold,
                                    inactiveTrackColor = textPrimary.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = audioPosition,
                                    style = MaterialTheme.typography.bodySmall.copy(color = mutedSilver)
                                )
                                Text(
                                    text = audioDuration,
                                    style = MaterialTheme.typography.bodySmall.copy(color = mutedSilver)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Central Playback Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.playPreviousSurah() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Surah", tint = metallicGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Previous Surah", color = metallicGold)
                            }
                            TextButton(onClick = { viewModel.playNextSurah() }) {
                                Text("Next Surah", color = metallicGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.SkipNext, contentDescription = "Next Surah", tint = metallicGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 28.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleShuffleMode() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Toggle Shuffle",
                                    tint = if (isShuffleEnabled) metallicGold else textPrimary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.seekAudioByMs(-10000) },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10 Seconds",
                                    tint = textPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(metallicGold)
                                    .clickable { viewModel.toggleAudioPlayback() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = deepSpaceBlue,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.seekAudioByMs(10000) },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Fast Forward 10 Seconds",
                                    tint = textPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleRepeatMode() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Toggle Repeat",
                                    tint = if (isRepeatEnabled) metallicGold else textPrimary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
                
                1 -> {
                    // TAB 1: FULL SURAH LIST VIEW (Selects Surah without immediate autoplay)
                    var searchQuery by remember { mutableStateOf("") }
                    val filteredSurahs = remember(searchQuery, audioSurahs) {
                        audioSurahs.filter {
                            it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                            it.englishTranslation.contains(searchQuery, ignoreCase = true) ||
                            it.id.toString() == searchQuery
                        }
                    }
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Surah En/Ar...", color = mutedSilver) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = metallicGold,
                                unfocusedBorderColor = textPrimary.copy(alpha = 0.2f),
                                cursorColor = metallicGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredSurahs) { surah ->
                                val isActive = selectedSurah?.id == surah.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectSurahForPlayback(surah, triggerPlayback = false)
                                            selectedTab = 0 // Return to player after selection
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActive) chipActive else chipInactiveSurface
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isActive) metallicGold else textPrimary.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(
                                                        color = if (isActive) metallicGold else circleInactive,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = surah.id.toString(),
                                                    color = if (isActive) deepSpaceBlue else textPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = surah.nameEnglish,
                                                    color = textPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = "${surah.versesCount} Ayahs • ${surah.type}",
                                                    color = mutedSilver,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        Text(
                                            text = surah.nameArabic,
                                            color = metallicGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                2 -> {
                    // TAB 2: FULL RECITER LIST SELECTION
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(IslamicData.reciters) { reciter ->
                            val isActive = selectedReciter.id == reciter.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectReciter(reciter)
                                        selectedTab = 0 // Return to player after selection
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) chipActive else chipInactiveSurface
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isActive) metallicGold else textPrimary.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = if (isActive) metallicGold else circleInactive,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Active Indicator",
                                            tint = if (isActive) deepSpaceBlue else textPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reciter.name,
                                            color = textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = reciter.description,
                                            color = mutedSilver,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // SURAH DIRECT DIALOG SELECTOR
    if (showSurahDialog) {
        var dialogSearchQuery by remember { mutableStateOf("") }
        val dialogFilteredSurahs = remember(dialogSearchQuery, audioSurahs) {
            audioSurahs.filter {
                it.nameEnglish.contains(dialogSearchQuery, ignoreCase = true) ||
                it.englishTranslation.contains(dialogSearchQuery, ignoreCase = true) ||
                it.id.toString() == dialogSearchQuery
            }
        }

        AlertDialog(
            onDismissRequest = { showSurahDialog = false },
            title = {
                Text(
                    text = "SELECT SURAH",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = metallicGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                    OutlinedTextField(
                        value = dialogSearchQuery,
                        onValueChange = { dialogSearchQuery = it },
                        placeholder = { Text("Search Surah...", color = mutedSilver) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mutedSilver) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = metallicGold,
                            unfocusedBorderColor = textPrimary.copy(alpha = 0.2f),
                            cursorColor = metallicGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dialogFilteredSurahs) { surah ->
                            val isSelected = selectedSurah?.id == surah.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        color = if (isSelected) chipActive else chipInactiveSurface
                                    )
                                    .clickable {
                                        viewModel.selectSurahForPlayback(surah, triggerPlayback = false)
                                        showSurahDialog = false
                                    }
                                    .padding(12.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) metallicGold else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = if (isSelected) metallicGold else circleInactive,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = surah.id.toString(),
                                            color = if (isSelected) deepSpaceBlue else textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = surah.nameEnglish,
                                            color = textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${surah.versesCount} Verses • ${surah.type}",
                                            color = mutedSilver,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Text(
                                    text = surah.nameArabic,
                                    color = metallicGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSurahDialog = false }) {
                    Text("CLOSE", color = metallicGold)
                }
            },
            containerColor = dialogSurface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }

    // QARI DIRECT DIALOG SELECTOR
    if (showQariDialog) {
        AlertDialog(
            onDismissRequest = { showQariDialog = false },
            title = {
                Text(
                    text = "SELECT QARI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = metallicGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(IslamicData.reciters) { reciter ->
                            val isSelected = selectedReciter.id == reciter.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        color = if (isSelected) chipActive else chipInactiveSurface
                                    )
                                    .clickable {
                                        viewModel.selectReciter(reciter)
                                        showQariDialog = false
                                    }
                                    .padding(12.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) metallicGold else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = if (isSelected) metallicGold else circleInactive,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isSelected) deepSpaceBlue else textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = reciter.name,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = reciter.description,
                                        color = mutedSilver,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQariDialog = false }) {
                    Text("CLOSE", color = metallicGold)
                }
            },
            containerColor = dialogSurface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }
}
