package com.noorpro.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.DuaCategory
import com.noorpro.app.data.DuaData
import com.noorpro.app.ui.theme.MatteGold
import com.noorpro.app.ui.theme.NightBackground
import com.noorpro.app.ui.theme.TextPrimary
import com.noorpro.app.ui.theme.TextSecondary
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

// Recolored gold → green to match the app's accent color (local vals shadow the theme's gold tokens).
private val MatteGold = Color(0xFF0E8C73)
private val GlowGold = Color(0xFF17B892)
private val LightGold = Color(0xFF7FD1BD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaHubScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val categories = DuaData.categories
    var searchQuery by remember { mutableStateOf("") }
    val filteredCategories = remember(searchQuery, categories) {
        categories.filter {
            searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.duas.any { dua -> dua.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Animated breathing backdrop circle for cosmic vibe
    val infiniteTransition = rememberInfiniteTransition(label = "DuaHubPulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CosmicCircle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SUPPLICATING HEART",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MatteGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = "Dua Treasures",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Serif
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) },
                        modifier = Modifier
                            .padding(8.dp)
                            .testTag("dua_hub_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MatteGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = NightBackground,
        modifier = modifier.fillMaxSize().testTag("dua_hub_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subtle rotating backdrop lines
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.04f)) {
                val center = Offset(size.width * 0.5f, size.height * 0.3f)
                val baseRadius = size.width * 0.5f
                drawCircle(
                    color = MatteGold,
                    radius = baseRadius * pulseSize,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = MatteGold,
                    radius = baseRadius * 1.5f,
                    center = center,
                    style = Stroke(width = 0.8.dp.toPx())
                )
                
                // Draw decorative lines
                for (angle in 0 until 360 step 30) {
                    val radians = Math.toRadians(angle.toDouble())
                    val start = Offset(
                        (center.x + Math.cos(radians) * baseRadius * 0.3f).toFloat(),
                        (center.y + Math.sin(radians) * baseRadius * 0.3f).toFloat()
                    )
                    val end = Offset(
                        (center.x + Math.cos(radians) * baseRadius * 2f).toFloat(),
                        (center.y + Math.sin(radians) * baseRadius * 2f).toFloat()
                    )
                    drawLine(color = MatteGold, start = start, end = end, strokeWidth = 1.dp.toPx())
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search categories and duas") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MatteGold) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp)
                )

                // Tasbih & Azkar Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Azkar Special Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(DeenScreen.AZKAR) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MatteGold.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Azkar",
                                tint = MatteGold,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Azkar",
                                color = MatteGold,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Tasbih Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(DeenScreen.TASBIH) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MatteGold.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Tasbih",
                                tint = MatteGold,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tasbih",
                                color = MatteGold,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2-Column Grid Layout for Dua Categories
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("dua_categories_grid")
                ) {
                    items(filteredCategories, key = { it.id }) { category: DuaCategory ->
                        DuaCategoryCard(
                            category = category,
                            onClick = { viewModel.selectDuaCategory(category) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AnimatedDuaOrb(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "DuaOrb")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "DuaOrbRotation"
    )
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2200), repeatMode = RepeatMode.Reverse),
        label = "DuaOrbGlow"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(124.dp).graphicsLayer { rotationZ = rotation }) {
            val center = Offset(size.width / 2, size.height / 2)
            repeat(8) { index ->
                rotate(index * 45f, center) {
                    drawRoundRect(
                        color = MatteGold.copy(alpha = 0.12f + index * 0.015f),
                        topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.64f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                        style = Stroke(1.2.dp.toPx())
                    )
                }
            }
            drawCircle(MatteGold.copy(alpha = glow * 0.25f), radius = size.width * 0.37f)
            drawCircle(MatteGold.copy(alpha = glow), radius = size.width * 0.24f, style = Stroke(2.dp.toPx()))
        }
        Icon(
            Icons.Default.Favorite,
            contentDescription = null,
            tint = MatteGold,
            modifier = Modifier.size(38.dp).graphicsLayer {
                scaleX = 0.9f + glow * 0.12f
                scaleY = 0.9f + glow * 0.12f
            }
        )
    }
}

@Composable
fun DuaCategoryCard(
    category: DuaCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = duaCardPalette(category.id)
    val transition = rememberInfiniteTransition(label = "DuaCard_${category.id}")
    val floatY by transition.animateFloat(
        initialValue = -4f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200 + category.id.length * 110, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DuaCardFloat"
    )
    val glow by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(1800), repeatMode = RepeatMode.Reverse),
        label = "DuaCardGlow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick)
            .testTag("dua_cat_${category.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.38f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(palette.start, palette.end)))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = palette.accent.copy(alpha = glow),
                    radius = size.minDimension * 0.32f,
                    center = Offset(size.width * 0.88f, size.height * 0.12f + floatY)
                )
                drawCircle(
                    color = palette.accent.copy(alpha = 0.22f),
                    radius = size.minDimension * 0.22f,
                    center = Offset(size.width * 0.86f, size.height * 0.14f + floatY),
                    style = Stroke(1.2.dp.toPx())
                )
                repeat(3) { index ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f + index * 0.03f),
                        radius = (2 + index).dp.toPx(),
                        center = Offset(size.width * (0.68f + index * 0.09f), size.height * (0.18f + index * 0.08f) + floatY)
                    )
                }
            }

            Icon(
                imageVector = mapIconNameToVector(category.iconName),
                contentDescription = null,
                tint = palette.accent.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(58.dp)
                    .graphicsLayer {
                        translationY = floatY
                        rotationZ = floatY * 0.8f
                    }
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
            // Category Icon Badge representing the topic beautifully
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(16.dp))
                    .border(1.dp, palette.accent.copy(alpha = 0.42f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = mapIconNameToVector(category.iconName),
                    contentDescription = category.title,
                    tint = palette.accent,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = "${category.duas.size} duas",
                    color = Color(0xFFC8D0C2),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = category.title,
                    color = palette.accent,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 22.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.description,
                    color = Color(0xFFC8D0C2),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                )
            }
            }
        }
    }
}

private data class DuaCardPalette(val start: Color, val end: Color, val accent: Color)

private fun duaCardPalette(id: String): DuaCardPalette = when (id) {
    "morning" -> DuaCardPalette(Color(0xFF6F4218), Color(0xFF27203F), Color(0xFFFFCF66))
    "evening" -> DuaCardPalette(Color(0xFF312B65), Color(0xFF151A3B), Color(0xFFB9B4FF))
    "mosque" -> DuaCardPalette(Color(0xFF15594F), Color(0xFF102E38), Color(0xFF75E0C4))
    "protection" -> DuaCardPalette(Color(0xFF224C75), Color(0xFF17253E), Color(0xFF8CCAFF))
    "happiness" -> DuaCardPalette(Color(0xFF70435C), Color(0xFF32213C), Color(0xFFFFA9D1))
    "travel" -> DuaCardPalette(Color(0xFF226177), Color(0xFF173344), Color(0xFF7EDCF2))
    "food" -> DuaCardPalette(Color(0xFF75512A), Color(0xFF39291E), Color(0xFFFFC47E))
    "family" -> DuaCardPalette(Color(0xFF5D3F76), Color(0xFF2B2442), Color(0xFFD9ACFF))
    "sleep" -> DuaCardPalette(Color(0xFF283A72), Color(0xFF171C38), Color(0xFF9EB3FF))
    "repentance" -> DuaCardPalette(Color(0xFF6F2C44), Color(0xFF24172E), Color(0xFFFF9FC1))
    "parents" -> DuaCardPalette(Color(0xFF4F3D76), Color(0xFF211B3B), Color(0xFFD9BEFF))
    else -> DuaCardPalette(Color(0xFF244E58), Color(0xFF17263A), MatteGold)
}

// Map database string keys to M3 Icons dynamically 
fun mapIconNameToVector(name: String): ImageVector {
    return when (name) {
        "wb_sunny" -> Icons.Default.WbSunny
        "dark_mode" -> Icons.Default.DarkMode
        "mosque" -> Icons.Default.MenuBook
        "security" -> Icons.Default.Security
        "mood" -> Icons.Default.Face
        "explore" -> Icons.Default.Explore
        "restaurant" -> Icons.Default.Restaurant
        "family" -> Icons.Default.Groups
        "bedtime" -> Icons.Default.Bedtime
        else -> Icons.Default.Star
    }
}
