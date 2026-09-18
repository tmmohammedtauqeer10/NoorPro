package com.noorpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.IslamicData
import com.noorpro.app.ui.theme.*
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(viewModel: DeenViewModel) {
    var selectedCategory by remember { mutableStateOf("Morning") }
    val azkarCounts by viewModel.azkarCounts.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Morning & Evening Azkar", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.DUA_HUB) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MatteGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NightBackground)
            )
        },
        containerColor = NightBackground
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // Sub selector pill (Morning vs Evening)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(GlassOverlay)
                    .border(1.dp, GlassBorder, RoundedCornerShape(30.dp))
                    .padding(4.dp)
            ) {
                val categories = listOf("Morning", "Evening")
                categories.forEach { category ->
                    val isSel = selectedCategory == category
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(26.dp))
                            .background(if (isSel) MatteGold.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSel) MatteGold.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(26.dp))
                            .clickable { selectedCategory = category }
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (category == "Morning") Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = category,
                                tint = if (isSel) MatteGold else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$category Azkar",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MatteGold else TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // List of filtered Azkar
            val filteredList = IslamicData.azkarList.filter { it.category == selectedCategory }

            filteredList.forEach { azkar ->
                val count = azkarCounts[azkar.id] ?: 0
                val isCompleted = count >= azkar.targetCount

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) MatteGold.copy(alpha = 0.05f) else GlassOverlay
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) MatteGold.copy(alpha = 0.4f) else GlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        // Header title & reset button
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = azkar.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCompleted) LightGold else TextPrimary
                                )
                            )

                            if (count > 0) {
                                Text(
                                    text = "RESET",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MatteGold
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.resetAzkarCount(azkar.id) }
                                        .padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Elegant Arabic Script panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NightBackground.copy(alpha = 0.4f))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = azkar.arabic,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LightGold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = azkar.translation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary.copy(alpha = 0.9f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = azkar.reference,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Tap-Counter trigger button
                        Button(
                            onClick = { viewModel.incrementAzkarCount(azkar.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) MatteGold else GlassOverlay,
                                contentColor = if (isCompleted) NightBackground else MatteGold
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isCompleted) null else BorderStroke(1.dp, MatteGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                                    contentDescription = "Tap counter",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCompleted) "COMPLETED (${azkar.targetCount}/${azkar.targetCount})"
                                           else "TAP TO REMEMBER (${count}/${azkar.targetCount})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
