package com.noorpro.app.ui.screens

import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.*
import com.noorpro.app.ui.theme.*
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

@Composable
fun TafsirScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSurah by viewModel.selectedSurah.collectAsState()
    val activeDbVerses by viewModel.activeDbVerses.collectAsState()
    val currentAyahNumber by viewModel.selectedTafsirAyah.collectAsState()

    // Handle physical back click
    BackHandler {
        viewModel.navigateTo(DeenScreen.QURAN)
    }

    val currentSurah = selectedSurah
    if (currentSurah == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No Surah Selected", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.navigateTo(DeenScreen.QURAN) },
                colors = ButtonDefaults.buttonColors(containerColor = MatteGold)
            ) {
                Text("Go to Quran")
            }
        }
        return
    }

    val totalVerses = currentSurah.verses.size
    val dbVerse = activeDbVerses.getOrNull(currentAyahNumber - 1)
    val arabicText = currentSurah.verses.getOrNull(currentAyahNumber - 1)?.first ?: dbVerse?.ayaTextTashkil ?: ""
    val englishText = currentSurah.verses.getOrNull(currentAyahNumber - 1)?.second ?: dbVerse?.tafseerMoysar ?: ""

    val ibnKathirText by viewModel.ibnKathirText.collectAsState()
    val ayahWords by viewModel.ayahWords.collectAsState()
    val isLoadingDynamicTafsir by viewModel.isLoadingDynamicTafsir.collectAsState()

    var selectedTafsirTab by remember { mutableStateOf(0) } // 0: Ibn Kathir, 1: Vocab, 2: Grammar

    val activeTafsirText = ibnKathirText ?: "Loading detailed explanations..."
    val isArabicText = activeTafsirText.any { it.code in 0x0600..0x06FF }
    val paragraphs = remember(activeTafsirText) {
        activeTafsirText.split("\n\n").filter { it.isNotBlank() }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.padding(top = 54.dp))

        // 1. Navigation Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(DeenScreen.QURAN) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassOverlay)
                    .testTag("tafsir_back_btn")
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
                    text = "Tafseer Study Companion",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${currentSurah.nameEnglish} • Verse $currentAyahNumber of $totalVerses",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Verse Selector Bar (Previous, Dropdown context, Next)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (currentAyahNumber > 1) {
                            viewModel.navigateToTafsir(currentSurah, currentAyahNumber - 1)
                        }
                    },
                    enabled = currentAyahNumber > 1,
                    modifier = Modifier.testTag("tafsir_prev_ayah_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Verse",
                        tint = if (currentAyahNumber > 1) MatteGold else TextSecondary.copy(alpha = 0.3f)
                    )
                }

                Text(
                    text = "Verse $currentAyahNumber",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MatteGold
                    ),
                    modifier = Modifier.testTag("tafsir_current_ayah_text")
                )

                IconButton(
                    onClick = {
                        if (currentAyahNumber < totalVerses) {
                            viewModel.navigateToTafsir(currentSurah, currentAyahNumber + 1)
                        }
                    },
                    enabled = currentAyahNumber < totalVerses,
                    modifier = Modifier.testTag("tafsir_next_ayah_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Verse",
                        tint = if (currentAyahNumber < totalVerses) MatteGold else TextSecondary.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // 3. Scrollable Detailed Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp)
        ) {
            item {
                // Verse Presentation Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MatteGold.copy(alpha = 0.03f)),
                    border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = arabicText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightGold,
                                fontFamily = FontFamily.Serif,
                                lineHeight = 40.sp,
                                textAlign = TextAlign.Right
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        HorizontalDivider(
                            color = GlassBorder,
                            thickness = 1.dp,
                            modifier = Modifier
                                .width(80.dp)
                                .padding(vertical = 8.dp)
                        )

                        Text(
                            text = englishText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Tafsir Selection Tabs (Minimalist M3 design, each item fully clickable >= 48dp)
                ScrollableTabRow(
                    selectedTabIndex = selectedTafsirTab,
                    containerColor = Color.Transparent,
                    contentColor = MatteGold,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        if (selectedTafsirTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTafsirTab]),
                                color = MatteGold
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassOverlay, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    val tabs = listOf("Ibn Kathir", "Vocabulary", "Grammar & Syntax")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTafsirTab == index,
                            onClick = { selectedTafsirTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTafsirTab == index) MatteGold else TextSecondary
                                    )
                                )
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("tafsir_tab_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 5. Active Tafsir Context Renderer
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassOverlay),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        val currentTafsirTitle = when (selectedTafsirTab) {
                            0 -> "Tafsir Ibn Kathir (Detailed Explanation)"
                            1 -> "Vocabulary & Word-by-Word Translation"
                            2 -> "Grammar & Syntax Analysis"
                            else -> "Information"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentTafsirTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MatteGold
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            if (isLoadingDynamicTafsir) {
                                Spacer(modifier = Modifier.width(12.dp))
                                CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(bottom = 12.dp), color = MatteGold, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedTafsirTab == 1) {
                if (ayahWords.isEmpty() && !isLoadingDynamicTafsir) {
                    item {
                        Text(
                            text = "Vocabulary data is currently unavailable.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "WORD-BY-WORD TRANSLATION",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(ayahWords.size) { i ->
                        val word = ayahWords[i]
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().background(GlassOverlay, RoundedCornerShape(8.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = word.translation,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = word.arabic,
                                style = MaterialTheme.typography.titleLarge.copy(color = GlowGold, fontFamily = FontFamily.Serif),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            } else if (selectedTafsirTab == 2) {
                if (ayahWords.isEmpty() && !isLoadingDynamicTafsir) {
                    item {
                        Text(
                            text = "Grammar data is currently unavailable.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "SYNTAX OVERVIEW & MORPHOLOGY",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(ayahWords.size) { i ->
                        val word = ayahWords[i]
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().background(GlassOverlay, RoundedCornerShape(8.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Syntax analysis for this word context.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Morphology & syntactic role available via Corpus data projection.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = word.arabic,
                                style = MaterialTheme.typography.titleLarge.copy(color = GlowGold, fontFamily = FontFamily.Serif),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            } else {
                items(paragraphs.size) { index ->
                    Text(
                        text = paragraphs[index].trim(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            lineHeight = 26.sp,
                            textAlign = if (isArabicText) TextAlign.Right else TextAlign.Left,
                            fontFamily = if (isArabicText) FontFamily.Serif else FontFamily.Default
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
