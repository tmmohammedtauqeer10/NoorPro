package com.noorpro.app.ui.screens

import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.Surah
import com.noorpro.app.ui.theme.*
import com.noorpro.app.ui.viewmodel.DeenViewModel

// Quran screen recolored from gold → the app's green/white theme. These local constants shadow the
// theme's gold tokens (MatteGold/GlowGold/LightGold), so every existing usage now renders green.
private val MatteGold = Color(0xFF0E8C73)
private val GlowGold = Color(0xFF17B892)
private val LightGold = Color(0xFF7FD1BD)

@Composable
fun SurahListScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSurah by viewModel.selectedSurah.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lastReadSurah by viewModel.lastReadSurah.collectAsState()
    val lastReadAyah by viewModel.lastReadAyah.collectAsState()

    val surahs by viewModel.surahs.collectAsState()

    val completedSurahs by viewModel.completedSurahs.collectAsState()

    // Handle physical system back clicks when SurahReaderView is open
    BackHandler(enabled = selectedSurah != null) {
        viewModel.closeSelectedSurah()
    }

    // Slide-out/in layout for the reading screen
    Crossfade(targetState = selectedSurah, label = "SurahNavigation") { surah ->
        if (surah != null) {
            SurahReaderView(
                viewModel = viewModel,
                surah = surah,
                lastReadAyah = if (lastReadSurah.id == surah.id) lastReadAyah else 1,
                onBack = { viewModel.closeSelectedSurah() },
                onAyahLastRead = { ayah -> viewModel.updateLastRead(surah, ayah) },
                onAyahBookmarkToggle = { ayah, text -> viewModel.toggleBookmark(surah, ayah, text) }
            )
        } else {
            SurahListContent(
                surahs = surahs,
                completedSurahs = completedSurahs,
                searchQuery = searchQuery,
                onSearchChanged = { viewModel.updateSearchQuery(it) },
                onSurahSelected = { viewModel.selectSurah(it) },
                onPlayClick = {
                    viewModel.selectSurahForPlayback(it, triggerPlayback = true)
                    viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.NOW_PLAYING)
                },
                onJuzSelected = { viewModel.selectJuz(it) },
                onToggleComplete = { viewModel.toggleSurahCompletion(it.id) },
                lastReadSurahId = lastReadSurah.id,
                modifier = modifier
            )
        }
    }
}

fun getJuzStartInfo(juzId: Int): String {
    return when (juzId) {
        1 -> "Starts at Al-Fatihah 1"
        2 -> "Starts at Al-Baqarah 142"
        3 -> "Starts at Al-Baqarah 253"
        4 -> "Starts at Ali 'Imran 93"
        5 -> "Starts at An-Nisa' 24"
        6 -> "Starts at An-Nisa' 148"
        7 -> "Starts at Al-Ma'idah 82"
        8 -> "Starts at Al-An'am 111"
        9 -> "Starts at Al-A'raf 88"
        10 -> "Starts at Al-Anfal 41"
        11 -> "Starts at At-Tawbah 93"
        12 -> "Starts at Hud 6"
        13 -> "Starts at Yusuf 53"
        14 -> "Starts at Al-Hijr 1"
        15 -> "Starts at Al-Isra' 1"
        16 -> "Starts at Al-Kahf 75"
        17 -> "Starts at Al-Anbiya' 1"
        18 -> "Starts at Al-Mu'minun 1"
        19 -> "Starts at Al-Furqan 21"
        20 -> "Starts at An-Naml 56"
        21 -> "Starts at Al-Ankabut 46"
        22 -> "Starts at Al-Ahzab 31"
        23 -> "Starts at Ya-Sin 28"
        24 -> "Starts at Az-Zumar 32"
        25 -> "Starts at Fussilat 47"
        26 -> "Starts at Al-Ahqaf 1"
        27 -> "Starts at Adh-Dhariyat 31"
        28 -> "Starts at Al-Mujadilah 1"
        29 -> "Starts at Al-Mulk 1"
        30 -> "Starts at An-Naba' 1"
        else -> ""
    }
}

fun getJuzArabicName(juzId: Int): String {
    val names = mapOf(
        1 to "الجزء ١", 2 to "الجزء ٢", 3 to "الجزء ٣", 4 to "الجزء ٤", 5 to "الجزء ٥",
        6 to "الجزء ٦", 7 to "الجزء ٧", 8 to "الجزء ٨", 9 to "الجزء ٩", 10 to "الجزء ١٠",
        11 to "الجزء ١١", 12 to "الجزء ١٢", 13 to "الجزء ١٣", 14 to "الجزء ١٤", 15 to "الجزء ١٥",
        16 to "الجزء ١٦", 17 to "الجزء ١٧", 18 to "الجزء ١٨", 19 to "الجزء ١٩", 20 to "الجزء ٢٠",
        21 to "الجزء ٢١", 22 to "الجزء ٢٢", 23 to "الجزء ٢٣", 24 to "الجزء ٢٤", 25 to "الجزء ٢٥",
        26 to "الجزء ٢٦", 27 to "الجزء ٢٧", 28 to "الجزء ٢٨", 29 to "الجزء ٢٩", 30 to "الجزء ٣٠"
    )
    return names[juzId] ?: "الجزء"
}

@Composable
fun JuzListItem(
    juzId: Int,
    isLastRead: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLastRead) MatteGold.copy(alpha = 0.08f) else GlassOverlay
        ),
        border = BorderStroke(
            1.dp,
            if (isLastRead) MatteGold.copy(alpha = 0.6f) else GlassBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("juz_item_$juzId")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .drawBehind {
                        drawCircle(
                            color = MatteGold.copy(alpha = 0.15f),
                            radius = size.width * 0.5f
                        )
                        drawCircle(
                            color = if (isLastRead) GlowGold else MatteGold,
                            radius = size.width * 0.44f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
            ) {
                Text(
                    text = juzId.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLastRead) GlowGold else LightGold,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Juz $juzId",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    if (isLastRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MatteGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Last Read",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    color = MatteGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val ayahsCount = com.noorpro.app.data.IslamicData.juzAyahsCount[juzId] ?: 0
                Text(
                    text = "${getJuzStartInfo(juzId)} • $ayahsCount Ayahs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            Text(
                text = getJuzArabicName(juzId),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MatteGold,
                    fontFamily = LibreCaslon
                )
            )
        }
    }
}

@Composable
fun SurahListContent(
    surahs: List<Surah>,
    completedSurahs: Set<Int>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onSurahSelected: (Surah) -> Unit,
    onPlayClick: (Surah) -> Unit,
    onJuzSelected: (Int) -> Unit,
    onToggleComplete: (Surah) -> Unit,
    lastReadSurahId: Int,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Surah, 1 = Juz

    val filteredSurahs = remember(surahs, searchQuery) {
        if (searchQuery.isBlank()) {
            surahs
        } else {
            surahs.filter {
                it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                it.englishTranslation.contains(searchQuery, ignoreCase = true) ||
                it.id.toString() == searchQuery.trim()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 90.dp) // Space for bottom floating bar
    ) {
        Spacer(modifier = Modifier.padding(top = 64.dp))

        // Title Header
        Text(
            text = "AL-QUR'AN",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold,
                letterSpacing = 2.4.sp,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Holy Al-Qur'an",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = "Explore chapters and verses of absolute truth",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom M3 Tab Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(GlassOverlay, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeTab == 0) MatteGold else Color.Transparent)
                    .clickable { activeTab = 0 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_surah")
            ) {
                Text(
                    text = "SURAH (CHAPTERS)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == 0) NightBackground else TextPrimary
                    )
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeTab == 1) MatteGold else Color.Transparent)
                    .clickable { activeTab = 1 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_juz")
            ) {
                Text(
                    text = "JUZ (PARTS)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == 1) NightBackground else TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress section
        if (surahs.isNotEmpty()) {
            val progress = completedSurahs.size.toFloat() / 114f
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    text = "Reading Progress",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${completedSurahs.size} / 114 Surahs",
                    style = MaterialTheme.typography.labelSmall.copy(color = MatteGold)
                )
            }
            LinearProgressIndicator(
                progress = progress,
                color = MatteGold,
                trackColor = GlassOverlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (activeTab == 0) {
            // Search box only relevant for Surah list
            SearchBox(query = searchQuery, onQueryChange = onSearchChanged)

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredSurahs.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No results found",
                        tint = MatteGold.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No chapters found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Check spelling or look for surah number",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                    )
                }
            } else {
                // LazyColumn for Surahs
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSurahs, key = { it.id }) { surah ->
                        val isLastRead = surah.id == lastReadSurahId
                        val isCompleted = completedSurahs.contains(surah.id)
                        SurahListItem(
                            surah = surah,
                            isLastRead = isLastRead,
                            isCompleted = isCompleted,
                            onClick = { onSurahSelected(surah) },
                            onPlayClick = { onPlayClick(surah) },
                            onToggleComplete = { onToggleComplete(surah) }
                        )
                    }
                }
            }
        } else {
            // LazyColumn for 30 Juz
            val juzs = remember { (1..30).toList() }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(juzs, key = { it }) { juzId ->
                    val isLastRead = lastReadSurahId == -juzId
                    JuzListItem(
                        juzId = juzId,
                        isLastRead = isLastRead,
                        onClick = { onJuzSelected(juzId) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Search Surah by English or Index...",
                color = TextSecondary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "SearchIcon",
                tint = MatteGold
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ClearIcon",
                        tint = TextSecondary
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = GlassOverlay,
            unfocusedContainerColor = GlassOverlay,
            disabledContainerColor = GlassOverlay,
            focusedIndicatorColor = MatteGold,
            unfocusedIndicatorColor = GlassBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = MatteGold
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .testTag("surah_search_field")
    )
}

@Composable
fun SurahListItem(
    surah: Surah,
    isLastRead: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLastRead) MatteGold.copy(alpha = 0.08f) else GlassOverlay
        ),
        border = BorderStroke(
            1.dp,
            if (isLastRead) MatteGold.copy(alpha = 0.6f) else GlassBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("surah_item_${surah.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Elegant hexagonal or circular styled surah index badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .drawBehind {
                        // Drawing ornamental custom circles for classic Islamic look
                        drawCircle(
                            color = MatteGold.copy(alpha = 0.15f),
                            radius = size.width * 0.5f
                        )
                        drawCircle(
                            color = if (isLastRead) GlowGold else MatteGold,
                            radius = size.width * 0.44f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
            ) {
                Text(
                    text = surah.id.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLastRead) GlowGold else LightGold,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        // Display Arabic name prominently instead of English mapping
                        text = if (surah.id < 0) surah.nameArabic else "سُورَةُ ${surah.nameArabic}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = LibreCaslon
                        )
                    )
                    if (isLastRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MatteGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Last Read",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    color = MatteGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${surah.englishTranslation} • ${surah.versesCount} Ayahs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            // English transliterated text on the right side
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MatteGold
                    )
                )
                Text(
                    text = surah.type,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onPlayClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Audio",
                    tint = MatteGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onToggleComplete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Mark Correct",
                    tint = if (isCompleted) MatteGold else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SurahReaderView(
    viewModel: DeenViewModel,
    surah: Surah,
    lastReadAyah: Int,
    onBack: () -> Unit,
    onAyahLastRead: (Int) -> Unit,
    onAyahBookmarkToggle: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    // Collect playback states from ViewModel
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val playingIndex by viewModel.playingAyahIndex.collectAsState()
    val rawProgress by viewModel.audioProgress.collectAsState()
    val position by viewModel.audioPosition.collectAsState()
    val duration by viewModel.audioDuration.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()

    val translationManager = viewModel.translationManager
    val selectedTranslationKey by translationManager.selectedTranslation.collectAsState()
    val selectedTranslationName = translationManager.availableTranslations
        .firstOrNull { it.key == selectedTranslationKey }?.name ?: "Default English"
    var showTranslations by remember(surah.id) { mutableStateOf(false) }
    var showTranslationSheet by remember { mutableStateOf(false) }

    val activeDbVerses by viewModel.activeDbVerses.collectAsState()
    val isQuranLoading by viewModel.isQuranLoading.collectAsState()
    val quranError by viewModel.quranError.collectAsState()
    var expandedAyahIndex by remember { mutableStateOf<Int?>(null) }
    val displayVerses = remember(surah.verses, activeDbVerses) {
        if (surah.verses.isNotEmpty()) {
            surah.verses
        } else {
            activeDbVerses.map { verse ->
                verse.ayaTextTashkil to verse.tafseerMoysar
            }
        }
    }

    val bookmarks by viewModel.bookmarks.collectAsState()

    var reciterDropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    onClick = onBack,
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (surah.id < 0) surah.nameArabic else "سُورَةُ ${surah.nameArabic}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontFamily = LibreCaslon
                        )
                    )
                    Text(
                        text = if (surah.id < 0) "${surah.englishTranslation} • ${activeDbVerses.size} Ayahs" else "${surah.englishTranslation} • ${surah.type} • ${surah.versesCount} Ayahs",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }

                // Translation selector action
                IconButton(
                    onClick = { showTranslationSheet = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Select Translation",
                        tint = MatteGold,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                val context = androidx.compose.ui.platform.LocalContext.current

                // Simple share surah info
                IconButton(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val shareText = "Reading Surah ${surah.nameArabic} (${surah.englishTranslation}) with ${activeDbVerses.size} Ayahs. Read the Quran on Noor Pro."
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Surah via"))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Surah",
                        tint = MatteGold,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (showTranslationSheet) {
                    com.noorpro.app.ui.screens.TranslationSelectionBottomSheet(
                        viewModel = viewModel,
                        translationManager = translationManager,
                        onDismiss = { showTranslationSheet = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassOverlay)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to "Arabic only", true to "Translation").forEach { (translationMode, label) ->
                    val selected = showTranslations == translationMode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MatteGold else Color.Transparent)
                            .clickable { showTranslations = translationMode }
                            .padding(vertical = 11.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = if (showTranslations) {
                    "$selectedTranslationName · Tap the translate icon to change"
                } else {
                    "Simple Quran reading without translation"
                },
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 220.dp) // Generous bottom padding to avoid player overlap!
            ) {
                // Surah Greeting Backdrop Banner
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassOverlay),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = surah.nameArabic,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LightGold,
                                    fontFamily = LibreCaslon
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "— ${surah.englishTranslation} —",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = GlassBorder, thickness = 1.dp, modifier = Modifier.width(120.dp))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Bismillah
                            if (surah.id > 0 && surah.id != 9) { // At-Tawbah does not have Bismillah
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MatteGold,
                                        fontFamily = LibreCaslon,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (showTranslations) {
                                    Text(
                                        text = "In the name of Allah, the Entirely Merciful, the Especially Merciful",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Beautiful Verses rendering
                itemsIndexed(displayVerses) { index, verse ->
                    val ayahNumber = index + 1
                    val isLastRead = lastReadAyah == ayahNumber
                    val isBookmarked = bookmarks.any { it.surahId == surah.id && it.ayahNumber == ayahNumber }
                    val isPlayingNow = isPlaying && playingIndex == index

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isPlayingNow) MatteGold.copy(alpha = 0.08f)
                                else if (isLastRead) NightBackground.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isPlayingNow) MatteGold.copy(alpha = 0.5f)
                                else if (isLastRead) GlassBorder
                                else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.selectAyahToPlay(index) } // Tapping verse starts audio directly from that verse!
                            .padding(16.dp)
                            .testTag("ayah_block_$index")
                    ) {
                        // Verse Header bar: Ayah index, active badge, and action triggers
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassOverlay, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPlayingNow) GlowGold.copy(alpha = 0.3f)
                                            else MatteGold.copy(alpha = 0.2f)
                                        )
                                ) {
                                    Text(
                                        text = "$ayahNumber",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isPlayingNow) GlowGold else MatteGold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                if (isPlayingNow) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "RECITING NOW",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = GlowGold,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Tap to Play Verse specific Icon
                                IconButton(
                                    onClick = { viewModel.selectAyahToPlay(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingNow) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                        contentDescription = "Play this Verse",
                                        tint = MatteGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Bookmark Ayah action
                                IconButton(
                                    onClick = { onAyahBookmarkToggle(ayahNumber, verse.first) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark this Verse",
                                        tint = MatteGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Last Read indicator action
                                IconButton(
                                    onClick = { onAyahLastRead(ayahNumber) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLastRead) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Mark as Last Read",
                                        tint = MatteGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Tafseer Study info toggle
                                IconButton(
                                    onClick = { expandedAyahIndex = if (expandedAyahIndex == index) null else index },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (expandedAyahIndex == index) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.AutoStories,
                                        contentDescription = "Tafseer & study details",
                                        tint = MatteGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Arabic Verse Text Align Right
                        Text(
                            text = verse.first,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightGold,
                                fontFamily = LibreCaslon,
                                lineHeight = 44.sp,
                                textAlign = TextAlign.Right
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        if (showTranslations) {
                            Text(
                                text = selectedTranslationName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MatteGold,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            )
                            Text(
                                text = verse.second,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextPrimary,
                                    lineHeight = if (selectedTranslationKey.startsWith("ur.")) 32.sp else 24.sp,
                                    textAlign = if (selectedTranslationKey.startsWith("ur.")) TextAlign.Right else TextAlign.Start
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Expandable study details (Tafseer, vocabulary, syntax)
                        val dbVerse = activeDbVerses.getOrNull(index)
                        if (expandedAyahIndex == index && dbVerse != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MatteGold.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth().animateContentSize()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Vocabulary / Maany
                                    if (dbVerse.maanyAya.isNotBlank() && dbVerse.maanyAya != "null" && dbVerse.maanyAya != "لا يوجد") {
                                        Text(
                                            text = "معاني الكلمات (Vocabulary)",
                                            fontWeight = FontWeight.Bold,
                                            color = MatteGold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = dbVerse.maanyAya,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)
                                        )
                                    }

                                    // Revelation Reason
                                    if (dbVerse.reasonsOfVerses.isNotBlank() && dbVerse.reasonsOfVerses != "null" && dbVerse.reasonsOfVerses != "لا يوجد") {
                                        Text(
                                            text = "سبب النزول (Revelation Reason)",
                                            fontWeight = FontWeight.Bold,
                                            color = MatteGold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = dbVerse.reasonsOfVerses,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)
                                        )
                                    }



                                    // Grammatical Earab
                                    if (dbVerse.earabQuran.isNotBlank() && dbVerse.earabQuran != "null") {
                                        Text(
                                            text = "إعراب الآية (Syntax & Grammar)",
                                            fontWeight = FontWeight.Bold,
                                            color = MatteGold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = dbVerse.earabQuran,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.navigateToTafsir(surah, index + 1) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MatteGold),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("launch_tafsir_button_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = "Read Full Tafsir",
                                            tint = NightBackground,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Read Comparative Tafseer",
                                            color = NightBackground,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (index < displayVerses.size - 1) {
                        HorizontalDivider(color = GlassBorder.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        // Floating Glassmorphic Audio Controller Card Layer
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, Brush.verticalGradient(colors = listOf(GlassBorder, Color.Transparent))),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.NOW_PLAYING) }
                .testTag("audio_playback_controller_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header of player: Reciter selector button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Reciter Audio",
                            tint = MatteGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RECITER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MatteGold,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Floating Dropdown menu structure
                    Box {
                        TextButton(
                            onClick = { reciterDropdownExpanded = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = LightGold)
                        ) {
                            Text(
                                text = selectedReciter.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown selector",
                                tint = MatteGold
                            )
                        }

                        DropdownMenu(
                            expanded = reciterDropdownExpanded,
                            onDismissRequest = { reciterDropdownExpanded = false },
                            modifier = Modifier.background(NightBackground).border(1.dp, GlassBorder)
                        ) {
                            com.noorpro.app.data.IslamicData.reciters.forEach { reciter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = reciter.name,
                                            color = if (reciter.id == selectedReciter.id) MatteGold else TextPrimary,
                                            fontWeight = if (reciter.id == selectedReciter.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectReciter(reciter)
                                        reciterDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("reciter_option_${reciter.id}")
                                )
                            }
                        }
                    }
                }

                    var sliderPosition by remember { mutableStateOf(rawProgress) }
                    var isDragging by remember { mutableStateOf(false) }

                    // Sync external progress to slider when not dragging
                    LaunchedEffect(rawProgress) {
                        if (!isDragging) {
                            sliderPosition = rawProgress
                        }
                    }

                    // Slider Progress Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = position,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )

                        Slider(
                            value = if (sliderPosition.isNaN()) 0f else sliderPosition.coerceIn(0f, 1f),
                            onValueChange = {
                                sliderPosition = it
                                isDragging = true
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                viewModel.seekAudio(sliderPosition)
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = MatteGold,
                                inactiveTrackColor = GlassBorder,
                                thumbColor = GlowGold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("audio_progressbar_seek")
                        )

                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                Spacer(modifier = Modifier.height(10.dp))

                // Playback controls row: back/pause/skip trigger
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Previous verse skip button
                    IconButton(
                        onClick = {
                            val nextIndex = (playingIndex - 1).coerceAtLeast(0)
                            viewModel.selectAyahToPlay(nextIndex)
                        },
                        enabled = playingIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Verse",
                            tint = if (playingIndex > 0) MatteGold else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Pulse main circle play action
                    FloatingActionButton(
                        onClick = { viewModel.toggleAudioPlayback() },
                        containerColor = MatteGold,
                        contentColor = NightBackground,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("play_pause_fab")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Next verse skip button
                    IconButton(
                        onClick = {
                            val nextIndex = (playingIndex + 1).coerceAtMost(displayVerses.size - 1)
                            viewModel.selectAyahToPlay(nextIndex)
                        },
                        enabled = playingIndex < displayVerses.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Verse",
                            tint = if (playingIndex < displayVerses.size - 1) MatteGold else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        if (isQuranLoading && displayVerses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NightBackground.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MatteGold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading authentic verses...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Caching Quran text for offline reading",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        } else if (quranError != null && displayVerses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NightBackground.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = quranError ?: "Unable to load Quran content.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.retryQuranContent() },
                        colors = ButtonDefaults.buttonColors(containerColor = MatteGold)
                    ) {
                        Text("Retry", color = NightBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
