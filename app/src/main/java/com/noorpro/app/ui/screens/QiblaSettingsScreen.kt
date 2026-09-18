package com.noorpro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.*
import com.noorpro.app.ui.theme.*
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlin.math.roundToInt

private fun bearingToCardinal(bearing: Float): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return directions[((bearing + 22.5f) / 45f).toInt() % directions.size]
}

@Composable
fun QiblaSettingsScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 90.dp) // Space for floating bottom navbar
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.padding(top = 64.dp))
        QiblaCompassTab(viewModel = viewModel)
    }
}

@Composable
fun QiblaCompassTab(viewModel: DeenViewModel) {
    val sensorAzimuth by viewModel.sensorAzimuth.collectAsState()
    val qiblaBearing by viewModel.qiblaBearing.collectAsState()
    val qiblaDistanceKm by viewModel.qiblaDistanceKm.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startCompassListening()
        onDispose { viewModel.stopCompassListening() }
    }
    
    // Visual calculations: The compass dial rotates relative to phone orientation (-azimuth)
    // The Kaaba pointer is relative to screen.
    val relativePointerAzimuth = (qiblaBearing - sensorAzimuth + 360f) % 360f
    val alignmentError = minOf(relativePointerAzimuth, 360f - relativePointerAzimuth)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Qibla Finder",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        )
        Text(
            text = "Align your device carefully to orient with the Kaaba",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Elegant physical compass housing
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(GlassOverlay, NightBackground.copy(alpha = 0.5f))
                    )
                )
                .border(2.dp, MatteGold, CircleShape)
                .border(8.dp, GlassOverlay, CircleShape)
                .testTag("qibla_compass_disc")
        ) {
            // Compass markers markings canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2
                drawCircle(color = GlassBorder.copy(alpha = 0.3f), radius = radius - 20.dp.toPx())

                // Draw N, S, E, W cardinal directions
                // N, S, E, W directions representation
            }

            // Outer dial rotating layer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = -sensorAzimuth
                    }
            ) {
                // Outer dial markings if needed
            }
            
            // Pointer layer that should always rotate to point to Kaaba
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = relativePointerAzimuth
                    }
            ) {
                // Kaaba Icon Pointer
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(26.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MatteGold)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Kaaba symbol",
                            tint = NightBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        "KABAH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MatteGold,
                            fontSize = 9.sp
                        )
                    )
                }

                // Inner Elegant compass needle ring representation
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Compass Needle",
                    tint = LightGold,
                    modifier = Modifier.size(120.dp)
                )
            }

            // Kaaba central coordinate text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val accuracyColor = if (alignmentError < 5f) GlowGold else TextPrimary
                Text(
                    text = "${qiblaBearing.roundToInt()}° ${bearingToCardinal(qiblaBearing)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = accuracyColor
                    )
                )
                Text(
                    text = "Makkah",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accuracyColor.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Precision metrics grid card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = if (qiblaDistanceKm > 0f) "${qiblaDistanceKm.roundToInt()} km" else "Calculating...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                VerticalDivider(
                    color = GlassBorder,
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Heading",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = "${qiblaBearing.roundToInt()}° ${bearingToCardinal(qiblaBearing)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                VerticalDivider(
                    color = GlassBorder,
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Accuracy",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = if (alignmentError < 5f) "Aligned" else "${alignmentError.roundToInt()}° to align",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlowGold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PreferencesTab() {
    var asrMadhab by remember { mutableStateOf(0) } // 0: Standard, 1: Hanafi
    var calculationMethod by remember { mutableStateOf(0) } // 0: Muslim World League, 1: Umm al-Qura
    var locationServices by remember { mutableStateOf(true) }
    var autoDnd by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Calculation Settings",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold
            )
        )

        // Preference item 1: Asr calculation school
        PreferenceItemRow(
            title = "Asr Juristic Method",
            subtitle = if (asrMadhab == 0) "Standard (Shafi'i, Maliki, Hanbali)" else "Hanafi School",
            icon = Icons.Default.Gavel,
            onClick = { asrMadhab = if (asrMadhab == 0) 1 else 0 }
        )

        // Preference item 2: Islamic Convention standard
        PreferenceItemRow(
            title = "Calculation Convention",
            subtitle = if (calculationMethod == 0) "Muslim World League (MWL)" else "Umm al-Qura (Makkah)",
            icon = Icons.Default.Public,
            onClick = { calculationMethod = if (calculationMethod == 0) 1 else 0 }
        )

        Text(
            text = "App Features",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold
            ),
            modifier = Modifier.padding(top = 8.dp)
        )

        // Toggle Row 1: Location Services
        ToggleOptionRow(
            title = "Automatic Location Detection",
            subtitle = "Enable GPS to calculate precise local timings",
            icon = Icons.Default.MyLocation,
            checked = locationServices,
            onCheckedChange = { locationServices = it }
        )

        // Toggle Row 2: Auto Do-Not-Disturb during prayer
        ToggleOptionRow(
            title = "Silent Mode During Prayers",
            subtitle = "Automatically quiet phone notifications during salah",
            icon = Icons.AutoMirrored.Filled.VolumeMute,
            checked = autoDnd,
            onCheckedChange = { autoDnd = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Version branding badge
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Deen — Premium Islamic Companion",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MatteGold.copy(alpha = 0.8f)
                )
            )
            Text(
                text = "Version 1.1.0 • Made with absolute devotion",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun PreferenceItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassOverlay),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassOverlay)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MatteGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Edit selection",
                tint = MatteGold,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ToggleOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassOverlay),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassOverlay)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MatteGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NightBackground,
                    checkedTrackColor = MatteGold,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = GlassOverlay
                )
            )
        }
    }
}

@Composable
fun AzkarTab(
    viewModel: DeenViewModel
) {
    var selectedCategory by remember { mutableStateOf("Morning") }
    val azkarCounts by viewModel.azkarCounts.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        // Tab Header and Description
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Daily Remembrance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MatteGold
                    )
                )
                Text(
                    text = "Purify your heart through morning & evening remembrance",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }
        }

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
                        .testTag("azkar_category_$category")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("azkar_card_${azkar.id}")
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
                                    .testTag("azkar_reset_${azkar.id}")
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

                    // Translation text
                    Text(
                        text = azkar.translation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Reference and Benefit tagline
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
                            .testTag("azkar_counter_btn_${azkar.id}")
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
    }
}
