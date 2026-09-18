package com.noorpro.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.data.QuranLearningSurah
import kotlinx.coroutines.launch

val LearningDeepSpaceBlue = Color(0xFF0B132B)
val LearningDarkSlateBlue = Color(0xFF152243)
val LearningGoldAccent = Color(0xFFD4AF37)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranDashboardScreen(viewModel: DeenViewModel) {
    val surahs by viewModel.learningSurahs.collectAsState()
    val isLoading by viewModel.isLearningSurahsLoading.collectAsState()
    val points by viewModel.learningGamificationPoints.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLearningSurahs()
    }

    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val screenBackground = if (isLightTheme) MaterialTheme.colorScheme.background else LearningDeepSpaceBlue
    val appBarColor = if (isLightTheme) Color(0xFFFFF7FB) else LearningDeepSpaceBlue
    val titleColor = if (isLightTheme) MaterialTheme.colorScheme.onBackground else Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quran Learning", color = titleColor, fontWeight = FontWeight.Bold)
                        Text(
                            text = "PTS: $points",
                            color = LearningGoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.EDUCATION) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LearningGoldAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
            )
        },
        containerColor = screenBackground
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
                itemsIndexed(surahs) { index, surah ->
                    QuranSurahItemAnimated(surah = surah, index = index, onClick = { viewModel.openLearningSurah(it) })
                }
            }
        }
    }
}

@Composable
fun QuranSurahItemAnimated(surah: QuranLearningSurah, index: Int, onClick: (QuranLearningSurah) -> Unit) {
    val slideAnim = remember { Animatable(300f) }
    val alphaAnim = remember { Animatable(0f) }
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cardColor = if (isLightTheme) Color.White else LearningDarkSlateBlue
    val primaryText = if (isLightTheme) MaterialTheme.colorScheme.onSurface else Color.White
    val secondaryText = if (isLightTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray

    LaunchedEffect(Unit) {
        val delay = minOf(index * 50, 1000) // Staggered delay, cap at 1s
        kotlinx.coroutines.delay(delay.toLong())
        launch {
            slideAnim.animateTo(0f, animationSpec = tween(400))
        }
        launch {
            alphaAnim.animateTo(1f, animationSpec = tween(400))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                translationX = slideAnim.value
                alpha = alphaAnim.value
            }
            .clickable { onClick(surah) },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLightTheme) Color(0xFFF1C8D8) else LearningGoldAccent.copy(alpha = 0.12f)
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
                        .size(40.dp)
                        .background(LearningGoldAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.number.toString(),
                        color = LearningGoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = surah.nameEnglish,
                        color = primaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${surah.ayahsCount} Ayahs",
                            color = secondaryText,
                            fontSize = 12.sp
                        )
                        if (surah.revelationType != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LearningGoldAccent.copy(alpha = 0.1f))
                                    .border(1.dp, LearningGoldAccent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = surah.revelationType,
                                    color = LearningGoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = surah.nameArabic,
                color = LearningGoldAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
            )
        }
    }
}
