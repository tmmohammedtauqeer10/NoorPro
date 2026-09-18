package com.noorpro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.data.QuranLearningAyah
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahLearningScreen(viewModel: DeenViewModel) {
    val surah by viewModel.learningSelectedSurah.collectAsState()
    val ayahs by viewModel.learningAyahs.collectAsState()
    val isLoading by viewModel.isLearningAyahsLoading.collectAsState()
    var bookmarkedAyah by remember { mutableStateOf<Int?>(null) } // In-memory

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surah?.nameEnglish ?: "Surah", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.QURAN_LEARNING_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LearningGoldAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LearningDeepSpaceBlue)
            )
        },
        containerColor = LearningDeepSpaceBlue
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LearningGoldAccent)
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "بِسْمِ ٱللَّٰهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                            fontSize = 32.sp,
                            color = LearningGoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(100.dp).height(2.dp).background(LearningGoldAccent.copy(alpha = 0.5f)))
                    }
                }
                
                items(ayahs) { ayah ->
                    QuranAyahCard(
                        ayah = ayah,
                        surahNumber = surah?.number ?: 0,
                        isBookmarked = bookmarkedAyah == ayah.number,
                        onBookmark = { bookmarkedAyah = ayah.number },
                        onPlay = { 
                            viewModel.playAyahAudio(ayah.audioUrl)
                            viewModel.addGamificationPoints(2)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuranAyahCard(
    ayah: QuranLearningAyah,
    surahNumber: Int,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onPlay: () -> Unit
) {
    var isLearnMode by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onBookmark() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = LearningDarkSlateBlue),
        shape = RoundedCornerShape(12.dp),
        border = if (isBookmarked) androidx.compose.foundation.BorderStroke(2.dp, LearningGoldAccent) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            var displayArabicText = ayah.arabicText
            if (surahNumber != 1 && ayah.number == 1 && ayah.arabicText.contains("بِسْمِ")) {
                displayArabicText = ayah.arabicText
                    .replace("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ", "")
                    .replace("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ", "")
                    .replace("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ\n", "")
                    .replace("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "")
                    .trim()
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = displayArabicText,
                    fontSize = 26.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    softWrap = true,
                    overflow = TextOverflow.Visible,
                    lineHeight = 44.sp,
                    style = LocalTextStyle.current.copy(
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = ayah.transliteration,
                fontSize = 16.sp,
                color = LearningGoldAccent,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = ayah.englishTranslation,
                fontSize = 14.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedVisibility(visible = isLearnMode) {
                QuranAyahLearnMode(ayah = ayah)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(LearningGoldAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ayah.number.toString(),
                        color = LearningGoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                
                Row {
                    IconButton(
                        onClick = { isLearnMode = !isLearnMode },
                        modifier = Modifier
                            .background(LearningGoldAccent.copy(alpha = if (isLearnMode) 0.4f else 0.1f), RoundedCornerShape(50))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Learn Mode",
                            tint = LearningGoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .background(LearningGoldAccent.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Audio",
                            tint = LearningGoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranAyahLearnMode(ayah: QuranLearningAyah) {
    val arabicWords = ayah.arabicText.split(" ")
    val transliterationWords = ayah.transliteration.split(" ")
    
    var activeWordIndex by remember { mutableIntStateOf(-1) }
    var isPlayingSequence by remember { mutableStateOf(false) }

    LaunchedEffect(isPlayingSequence) {
        if (isPlayingSequence) {
            for (i in arabicWords.indices) {
                activeWordIndex = i
                delay(800) // fake duration
            }
            activeWordIndex = -1
            isPlayingSequence = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Word-by-Word Learning", color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = {
                    activeWordIndex = -1
                    isPlayingSequence = true
                }
            ) {
                Text(if (isPlayingSequence) "Playing..." else "Start", color = LearningGoldAccent)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Keep rows explicit: the FlowRow ABI used by older tester builds can
        // fail at runtime when a transitive dependency selects another Compose version.
        Column(modifier = Modifier.fillMaxWidth()) {
            arabicWords.withIndex().chunked(4).forEach { rowWords ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    rowWords.forEach { (index, word) ->
                        val isActive = index == activeWordIndex
                        val bgColor by animateColorAsState(targetValue = if (isActive) LearningGoldAccent.copy(alpha = 0.3f) else Color.Transparent)
                        val translitWord = transliterationWords.getOrNull(index) ?: ""

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { activeWordIndex = index }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = word,
                                fontSize = 24.sp,
                                color = Color.White,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = translitWord,
                                fontSize = 12.sp,
                                color = LearningGoldAccent
                            )
                        }
                    }
                }
            }
        }
    }
}
