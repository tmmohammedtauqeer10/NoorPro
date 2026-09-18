package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.theme.GlassBorder
import com.noorpro.app.ui.theme.GlassOverlay
import com.noorpro.app.ui.theme.MatteGold
import com.noorpro.app.ui.theme.NightBackground
import com.noorpro.app.ui.theme.TextPrimary
import com.noorpro.app.ui.theme.TextSecondary
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QazaTrackerScreen(viewModel: DeenViewModel) {
    val qazaCounts by viewModel.qazaCounts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Qaza-e-Umri Tracker", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MatteGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = NightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Information block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MatteGold.copy(alpha = 0.08f))
                    .border(1.dp, MatteGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MatteGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Tap the large number or minus (-) button whenever you offer a Qaza prayer to decrease your total lifetime balance.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(qazaCounts) { countItem ->
                    QazaCard(
                        prayerName = countItem.prayerName,
                        remaining = countItem.totalRemaining,
                        onIncrement = { viewModel.incrementQaza(countItem.prayerName) },
                        onDecrement = { viewModel.decrementQaza(countItem.prayerName) }
                    )
                }
            }
        }
    }
}

@Composable
fun QazaCard(
    prayerName: String,
    remaining: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GlassOverlay),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = prayerName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Core Tappable Display Counter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NightBackground.copy(alpha = 0.5f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDecrement()
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = remaining.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MatteGold,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onIncrement) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = "Add",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDecrement()
                    }
                ) {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = "Subtract",
                        tint = MatteGold,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
