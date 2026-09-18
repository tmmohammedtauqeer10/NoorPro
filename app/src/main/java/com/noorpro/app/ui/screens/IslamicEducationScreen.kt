package com.noorpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.theme.*
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

@Composable
fun IslamicEducationScreen(viewModel: DeenViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 90.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.padding(top = 64.dp))
        
        Text(
            text = "Islamic Learning",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        )
        Text(
            text = "Enhance your knowledge and test your understanding",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary
            )
        )
        Spacer(modifier = Modifier.height(32.dp))

        EducationCard(
            title = "Islamic Library",
            description = "Explore a vast collection of books, articles, and historical accounts.",
            icon = Icons.AutoMirrored.Filled.LibraryBooks,
            onClick = { viewModel.navigateTo(DeenScreen.LIBRARY_DASHBOARD) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Quran Learning Module",
            description = "Learn Quran interactively with gamification and full recitations.",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = { viewModel.navigateTo(DeenScreen.QURAN_LEARNING_DASHBOARD) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Indo-Pak Quran (15 Line)",
            description = "Read the full Quran in Indo-Pak script — pure Arabic, no transliteration.",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = { viewModel.navigateTo(DeenScreen.INDOPAK_QURAN) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Qaida Digital Tutor",
            description = "Master Arabic letters, pronunciation, and basic reading rules.",
            icon = Icons.AutoMirrored.Filled.MenuBook, // Reusing icon for now
            onClick = { viewModel.navigateTo(DeenScreen.QAIDA_TUTOR) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Knowledge Quiz",
            description = "Test your Islamic knowledge with interactive quizzes.",
            icon = Icons.Default.Star,
            onClick = { viewModel.navigateTo(DeenScreen.QUIZ_DASHBOARD) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Hadith Collection",
            description = "Read authentic hadith from the major books with full chapters.",
            icon = Icons.Default.AutoStories,
            onClick = { viewModel.navigateTo(DeenScreen.HADITH_LIBRARY) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Tafsir & Reflection",
            description = "Understand the meaning of the Quran verse by verse.",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = { viewModel.navigateTo(DeenScreen.TAFSIR) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "99 Names of Allah",
            description = "Learn Asma-ul-Husna with meanings and benefits.",
            icon = Icons.Default.Star,
            onClick = { viewModel.navigateTo(DeenScreen.ASMA_UL_HUSNA) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Dua & Adhkar",
            description = "Daily supplications and remembrance for every occasion.",
            icon = Icons.Default.VolunteerActivism,
            onClick = { viewModel.navigateTo(DeenScreen.DUA_HUB) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Morning & Evening Azkar",
            description = "Fortify your day with the authentic morning and evening azkar.",
            icon = Icons.Default.WbSunny,
            onClick = { viewModel.navigateTo(DeenScreen.AZKAR) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Hajj & Umrah Guide",
            description = "Step-by-step rituals and preparation for the sacred journey.",
            icon = Icons.Default.Mosque,
            onClick = { viewModel.navigateTo(DeenScreen.HAJJ_UMRAH) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EducationCard(
            title = "Ask Noor AI",
            description = "Get instant Islamic guidance, dua drafts and lesson explanations.",
            icon = Icons.Default.AutoAwesome,
            onClick = { viewModel.navigateTo(DeenScreen.AI_HUB) }
        )
    }
}

@Composable
fun EducationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassOverlay),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MatteGold.copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MatteGold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp)
                )
            }
        }
    }
}
