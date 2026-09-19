package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.data.DuaCategory
import com.example.data.DuaItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel

// Recolored gold → green to match the app's accent color (local vals shadow the theme's gold tokens).
private val MatteGold = Color(0xFF0E8C73)
private val GlowGold = Color(0xFF17B892)
private val LightGold = Color(0xFF7FD1BD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaDetailsScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val categoryState by viewModel.selectedDuaCategory.collectAsState()
    val category = categoryState ?: return
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val screenBackground = if (isLightTheme) MaterialTheme.colorScheme.background else NightBackground

    val haptic = LocalHapticFeedback.current

    // Local dictionary state to track counts separate per invocation
    val counters = remember { mutableStateMapOf<Int, Int>() }

    // Floating mandala rotation animation for when Lottie fails or is loading
    val infiniteTransition = rememberInfiniteTransition(label = "DetailCelestial")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MandalaRotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Serif
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(DeenScreen.DUA_HUB) },
                        modifier = Modifier.testTag("dua_details_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to list",
                            tint = MatteGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = screenBackground,
        modifier = modifier.fillMaxSize().testTag("dua_details_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ==================== BACKGROUND HEADER AREA (TOP 30%) ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = if (isLightTheme) {
                                listOf(Color(0xFFF8F1EA), screenBackground)
                            } else {
                                listOf(Color(0xFF0F1A35), screenBackground)
                            }
                        )
                    )
            ) {
                // Try to load online fluid Lottie animation with elegant fallback
                val compositionResult = rememberLottieComposition(
                    spec = LottieCompositionSpec.Url(category.remoteUrl)
                )
                
                if (compositionResult.value != null) {
                    LottieAnimation(
                        composition = compositionResult.value,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.7f),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    // Majestic premium procedural rotating crescent and stars drawn in real-time
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = rotationAngle },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = size.width / 2
                            
                            // Concentric sacred geometry
                            drawCircle(
                                color = MatteGold.copy(alpha = 0.15f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawCircle(
                                color = MatteGold.copy(alpha = 0.08f),
                                radius = radius * 0.7f,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            
                            // Delicate star rays
                            for (i in 0 until 12) {
                                val angleRad = Math.toRadians((i * 30).toDouble())
                                val startX = (center.x + Math.cos(angleRad) * radius * 0.6f).toFloat()
                                val startY = (center.y + Math.sin(angleRad) * radius * 0.6f).toFloat()
                                val endX = (center.x + Math.cos(angleRad) * radius * 0.95f).toFloat()
                                val endY = (center.y + Math.sin(angleRad) * radius * 0.95f).toFloat()
                                drawLine(
                                    color = MatteGold.copy(alpha = 0.2f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }
                }

                // Smooth luxury dark overlay gradient near bottom to blend the animation seamlessly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, screenBackground)
                            )
                        )
                )
            }

            // ==================== OVERLAPPING SCROLLABLE CARDS ====================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Transparent spacer so the top 30% header stays beautifully visible
                item {
                    Spacer(modifier = Modifier.height(210.dp))
                }

                // Category summary/intro banner at root of list
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                        .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLightTheme) Color.White else Color(0xFF152243)
                        ),
                        border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = category.description,
                                color = if (isLightTheme) Color(0xFF404944) else TextSecondary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "TAP TO RECORD REPETITIONS",
                                color = MatteGold,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }
                    }
                }

                // Stack of individual Duas
                items(category.duas) { dua ->
                    val currentCount = counters[dua.id] ?: 0
                    val isCompleted = currentCount >= dua.targetCount

                    DuaInvocationCard(
                        dua = dua,
                        currentCount = currentCount,
                        isCompleted = isCompleted,
                        onIncrement = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val next = currentCount + 1
                            if (next <= dua.targetCount) {
                                counters[dua.id] = next
                            } else {
                                // Cycle back to 0 on next tap after completion
                                counters[dua.id] = 0
                            }
                        },
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            counters[dua.id] = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DuaInvocationCard(
    dua: DuaItem,
    currentCount: Int,
    isCompleted: Boolean,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cardBackground = when {
        isCompleted && isLightTheme -> Color(0xFFEAF7F1)
        isCompleted -> Color(0xFF0F2B24)
        isLightTheme -> Color.White
        else -> Color(0xFF152243)
    }
    val mainTextColor = if (isLightTheme) Color(0xFF0D1C2F) else Color.White
    val secondaryTextColor = if (isLightTheme) Color(0xFF404944) else TextSecondary
    val dividerColor = if (isLightTheme) Color(0xFFE4E8E5) else Color.White.copy(alpha = 0.05f)
    val completedAccent = if (isLightTheme) Color(0xFF067A54) else Color(0xFF8CEFA4)
    val labelAccent = MatteGold.copy(alpha = if (isLightTheme) 0.95f else 0.7f)
    val urduTextColor = if (isLightTheme) Color(0xFF0E8C73) else LightGold

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("dua_card_${dua.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        border = BorderStroke(
            width = if (isCompleted) 1.5.dp else 1.dp,
            color = if (isCompleted) Color(0xFF4BB543).copy(alpha = 0.5f) else MatteGold.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 1. Invocation Header/Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dua.title,
                    color = if (isCompleted) completedAccent else MatteGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f)
                )
                
                if (currentCount > 0) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset count",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Beautiful Arabic Text in RTL layout, size 26, white, Amiri-Serif font style
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = dua.arabic,
                    color = mainTextColor,
                    fontSize = 26.sp,
                    lineHeight = 44.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("dua_arabic_${dua.id}"),
                    textAlign = TextAlign.Justify
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stacked English, Urdu (Kanz-ul-Iman style), and Roman Urdu translations divided by faint gridlines
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Separator 1
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                
                // English Translation
                Column {
                    Text(
                        text = "English",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = labelAccent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dua.english,
                        color = mainTextColor.copy(alpha = 0.92f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }

                // Separator 2
                HorizontalDivider(color = dividerColor, thickness = 1.dp)

                // Urdu Translation (Kanz-ul-Iman dynamic tone)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "اردو (کنز الایمان)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = labelAccent,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dua.urdu,
                            color = urduTextColor,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Separator 3
                HorizontalDivider(color = dividerColor, thickness = 1.dp)

                // Roman Urdu Translation
                Column {
                    Text(
                        text = "Roman Urdu",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = labelAccent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dua.romanUrdu,
                        color = secondaryTextColor,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Interactive golden/emerald tap counter trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentCount > 0) {
                    TextButton(
                        onClick = onReset,
                        colors = ButtonDefaults.textButtonColors(contentColor = secondaryTextColor)
                    ) {
                        Text("Reset", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Interactive Bubble
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) Color(0xFF4BB543).copy(alpha = 0.2f) else MatteGold.copy(alpha = 0.15f)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isCompleted) Color(0xFF4BB543) else MatteGold,
                            shape = CircleShape
                        )
                        .clickable(onClick = onIncrement)
                        .testTag("dua_increment_${dua.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF4BB543),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "$currentCount",
                                color = MatteGold,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "/ ${dua.targetCount}",
                                color = MatteGold.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
