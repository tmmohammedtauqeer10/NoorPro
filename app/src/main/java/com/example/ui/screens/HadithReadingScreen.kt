package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithReadingScreen(viewModel: DeenViewModel) {
    val hadiths by viewModel.hadithList.collectAsState()
    val isLoading by viewModel.isLoadingHadith.collectAsState()
    val chapterId by viewModel.selectedHadithChapterId.collectAsState()
    val currentLang by viewModel.selectedHadithLanguage.collectAsState()
    val colors = MaterialTheme.colorScheme
    
    var showLangMenu by remember { mutableStateOf(false) }
    
    val languages = listOf(
        "eng" to "English",
        "urd" to "Urdu",
        "ben" to "Bengali",
        "tur" to "Turkish"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(DeenScreen.HADITH_CHAPTERS) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Chapter $chapterId",
                color = colors.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            Box {
                IconButton(onClick = { showLangMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = colors.primary
                    )
                }
                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false }
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name, color = if (code == currentLang) colors.primary else colors.onSurface) },
                            onClick = {
                                viewModel.setHadithLanguage(code)
                                showLangMenu = false
                            }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(hadiths) { hadith ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Hadith ${hadith.hadithNo}",
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Arabic Text
                            Text(
                                text = hadith.arabicText,
                                color = colors.onSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                lineHeight = 36.sp
                            )

                            HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(bottom = 16.dp))

                            // Translation Text
                            Text(
                                text = hadith.translationText,
                                color = colors.onSurfaceVariant,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
