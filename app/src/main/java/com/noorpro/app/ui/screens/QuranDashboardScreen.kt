package com.noorpro.app.ui.screens

import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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

val LearningDeepSpaceBlue: Color
    @Composable get() = MaterialTheme.colorScheme.background
val LearningDarkSlateBlue: Color
    @Composable get() = MaterialTheme.colorScheme.surface
val LearningGoldAccent: Color
    @Composable get() = com.noorpro.app.ui.theme.AppGold

@Composable
fun QuranDashboardScreen(viewModel: DeenViewModel) {
    val surahs by viewModel.learningSurahs.collectAsState()
    val isLoading by viewModel.isLearningSurahsLoading.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { viewModel.loadLearningSurahs() }
    val course = remember { runCatching { com.noorpro.app.data.QuranCourseRepository.load(ctx) }.getOrNull() }
    val done = remember { com.noorpro.app.data.QuranCourseProgress(ctx).completed() }
    fun openCourse(target: String?) { QuranCourseLaunch.pending = target; viewModel.navigateTo(DeenScreen.QAIDA_TUTOR) }
    QuranLearningMenu(
        totalLessons = course?.allLessons?.size ?: 0, doneLessons = done.size,
        surahs = surahs, isLoading = isLoading,
        onBack = { viewModel.navigateTo(DeenScreen.EDUCATION) },
        onCourse = { openCourse(null) },
        onHifz = { openCourse("lesson:l4_hifz") },
        onSelfTest = { openCourse("lesson:l4_test") },
        onChart = { openCourse("chart") },
        onJuzAmma = { openCourse("lesson:l4_read") },
        onSurah = { viewModel.openLearningSurah(it) },
    )
}

@Composable
fun QuranLearningMenu(
    totalLessons: Int, doneLessons: Int, surahs: List<QuranLearningSurah>, isLoading: Boolean,
    onBack: () -> Unit, onCourse: () -> Unit, onHifz: () -> Unit, onSelfTest: () -> Unit, onChart: () -> Unit,
    onJuzAmma: () -> Unit, onSurah: (QuranLearningSurah) -> Unit,
) {
    val p = coursePalette()
    Column(Modifier.fillMaxSize().background(p.bg)) {
        CourseTopBar("Quran Learning", "Read, recite and memorise step by step", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.navigationBarsPadding()) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CourseGreen).clickable(onClick = onCourse).padding(18.dp)) {
                    Text("Quran Reading Course", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Qaida → Tajweed → Madd & Waqf → Practice → Advanced", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { if (totalLessons == 0) 0f else doneLessons / totalLessons.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Color(0xFFE9C95B), trackColor = Color.White.copy(alpha = 0.25f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("$doneLessons of $totalLessons lessons complete", color = Color.White, fontSize = 12.sp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MenuTile(p, "Hifz mode", "Repeat each ayah", Icons.Default.Repeat, Modifier.weight(1f), onHifz)
                    MenuTile(p, "Self-test", "Recite, then reveal", Icons.Default.CheckCircle, Modifier.weight(1f), onSelfTest)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MenuTile(p, "Tajweed chart", "Rule colours", Icons.Default.Palette, Modifier.weight(1f), onChart)
                    MenuTile(p, "Juz Amma", "Read with Husary", Icons.AutoMirrored.Filled.MenuBook, Modifier.weight(1f), onJuzAmma)
                }
            }
            item {
                Text("Surah Learning · word by word", color = p.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                Text("All 114 surahs with transliteration, translation and Husary audio (needs internet).", color = p.muted, fontSize = 12.sp)
            }
            if (isLoading) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CourseGreen) } }
            else if (surahs.isEmpty()) item { Text("Surah list needs an internet connection. The course above works offline.", color = p.muted, fontSize = 13.sp) }
            items(surahs, key = { it.number }) { s -> SurahRow(p, s) { onSurah(s) } }
        }
    }
}

@Composable
private fun MenuTile(p: CoursePalette, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(CourseGreen.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CourseGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(subtitle, color = p.muted, fontSize = 12.sp)
    }
}

@Composable
private fun SurahRow(p: CoursePalette, surah: QuranLearningSurah, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(p.chip), contentAlignment = Alignment.Center) {
            Text(surah.number.toString(), color = CourseGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(surah.nameEnglish, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(listOfNotNull("${surah.ayahsCount} ayahs", surah.revelationType).joinToString(" · "), color = p.muted, fontSize = 12.sp)
        }
        if (surah.nameArabic.isNotBlank()) Text(surah.nameArabic, color = com.noorpro.app.ui.theme.QuranInk, fontFamily = com.noorpro.app.ui.components.QuranArabicFont, fontSize = 20.sp)
    }
}
