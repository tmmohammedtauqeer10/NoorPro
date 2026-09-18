package com.noorpro.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaScreen(viewModel: DeenViewModel) {
    var currentSection by remember { mutableStateOf("Sections") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentSection == "Sections") "Qaida Tutor" else currentSection, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentSection != "Sections") currentSection = "Sections"
                        else viewModel.navigateTo(DeenScreen.EDUCATION)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LearningGoldAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LearningDeepSpaceBlue)
            )
        },
        containerColor = LearningDeepSpaceBlue
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentSection) {
                "Sections" -> QaidaSectionsScreen { currentSection = it }
                "Arabic Alphabet" -> ArabicAlphabetSection()
                "Harakat" -> HarakatSection()
                "Joining Letters" -> JoiningLettersSection()
                "Practice Words" -> PracticeWordsSection()
            }
        }
    }
}

@Composable
fun QaidaSectionsScreen(onSectionSelected: (String) -> Unit) {
    val sections = listOf("Arabic Alphabet", "Harakat", "Joining Letters", "Practice Words")
    
    Column(modifier = Modifier.padding(16.dp)) {
        sections.forEach { section ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onSectionSelected(section) },
                colors = CardDefaults.cardColors(containerColor = LearningDarkSlateBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = section, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ArabicAlphabetSection() {
    val alphabet = listOf(
        Pair("ا", "Alif"), Pair("ب", "Ba"), Pair("ت", "Ta"), Pair("ث", "Tha"),
        Pair("ج", "Jeem"), Pair("ح", "Ha"), Pair("خ", "Kha"), Pair("د", "Dal"),
        Pair("ذ", "Dhal"), Pair("ر", "Ra"), Pair("ز", "Zay"), Pair("س", "Seen"),
        Pair("ش", "Sheen"), Pair("ص", "Sad"), Pair("ض", "Dad"), Pair("ط", "Ta"),
        Pair("ظ", "Za"), Pair("ع", "Ain"), Pair("غ", "Ghain"), Pair("ف", "Fa"),
        Pair("ق", "Qaf"), Pair("ك", "Kaf"), Pair("ل", "Lam"), Pair("م", "Meem"),
        Pair("ن", "Noon"), Pair("ه", "Ha"), Pair("و", "Waw"), Pair("ي", "Ya")
    )
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(alphabet) { letter ->
            FlipCard(
                frontContent = { 
                    Text(letter.first, fontSize = 48.sp, color = LearningGoldAccent, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif) 
                },
                backContent = { 
                    Text(letter.second, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) 
                }
            )
        }
    }
}

@Composable
fun HarakatSection() {
    val harakat = listOf(
        Pair("بَ", "Fatha (Ba)"), Pair("بِ", "Kasra (Bi)"), Pair("بُ", "Damma (Bu)")
    )
    
    Column(modifier = Modifier.padding(16.dp)) {
        harakat.forEach { mark ->
            FlipCard(
                frontContent = { 
                    Text(mark.first, fontSize = 64.sp, color = LearningGoldAccent, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif) 
                },
                backContent = { 
                    Text(mark.second, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold) 
                },
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun JoiningLettersSection() {
    val examples = listOf(
        Triple("Isolated", "ب", "Ba"),
        Triple("Initial", "بـ", "Ba"),
        Triple("Medial", "ـبـ", "Ba"),
        Triple("Final", "ـب", "Ba")
    )
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(examples) { item ->
            Card(
                modifier = Modifier.height(140.dp),
                colors = CardDefaults.cardColors(containerColor = LearningDarkSlateBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(item.first, color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.second, color = LearningGoldAccent, fontSize = 40.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
fun PracticeWordsSection() {
    val words = listOf(
        Pair("كَتَبَ", "He wrote"), Pair("قَرَأَ", "He read"), Pair("ذَهَبَ", "He went")
    )
    
    Column(modifier = Modifier.padding(16.dp)) {
        words.forEach { word ->
            FlipCard(
                frontContent = { 
                    Text(word.first, fontSize = 48.sp, color = LearningGoldAccent, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif) 
                },
                backContent = { 
                    Text(word.second, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold) 
                },
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun FlipCard(
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
    modifier: Modifier = Modifier.height(100.dp)
) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400)
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { flipped = !flipped },
        colors = CardDefaults.cardColors(containerColor = LearningDarkSlateBlue),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                frontContent()
            } else {
                Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                    backContent()
                }
            }
        }
    }
}
