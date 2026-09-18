package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveQuizScreen(viewModel: DeenViewModel) {
    val category = viewModel.currentQuizCategory

    LaunchedEffect(category) {
        if (category == null) {
            viewModel.navigateTo(DeenScreen.QUIZ_DASHBOARD)
        }
    }

    if (category == null) {
        // Show empty box while redirecting
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val questions = category.questions
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val currentQuestion = questions.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.QUIZ_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Quiz", tint = GoldAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlue
                )
            )
        },
        containerColor = DeepBlue
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isFinished) {
                // Summary Screen
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Quiz Completed!", color = GoldAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You earned ${score} points!",
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            viewModel.addQuizPoints(score)
                            viewModel.navigateTo(DeenScreen.QUIZ_DASHBOARD)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Claim Rewards & Exit", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else if (currentQuestion != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp)
                ) {
                    // Progress Indicator
                    Text(
                        "Question ${currentIndex + 1} of ${questions.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size.toFloat() },
                        color = GoldAccent,
                        trackColor = SlateCard,
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // Question Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = currentQuestion.text,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Options Grid
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        currentQuestion.options.forEachIndexed { index, option ->
                            val isSelected = selectedOptionIndex == index
                            val isCorrect = index == currentQuestion.correctIndex
                            val hasAnswered = selectedOptionIndex != null

                            val backgroundColor = when {
                                !hasAnswered -> SlateCard
                                isSelected && isCorrect -> Color(0xFF2E7D32) // Emerald Green
                                isSelected && !isCorrect -> Color(0xFFC62828) // Crimson Red
                                !isSelected && isCorrect -> Color(0xFF2E7D32) // Highlight correct privately
                                else -> SlateCard.copy(alpha = 0.5f)
                            }
                            
                            val textColor = if (hasAnswered && (isSelected || isCorrect)) Color.White else Color.White

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(backgroundColor)
                                    .clickable(enabled = !hasAnswered) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        selectedOptionIndex = index
                                        if (isCorrect) {
                                            score += category.pointsPerQuestion
                                        }
                                    }
                                    .padding(vertical = 16.dp, horizontal = 20.dp)
                            ) {
                                Text(
                                    text = option,
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Auto-advance logic
                    LaunchedEffect(selectedOptionIndex) {
                        if (selectedOptionIndex != null) {
                            delay(1500)
                            if (currentIndex < questions.size - 1) {
                                currentIndex++
                                selectedOptionIndex = null
                            } else {
                                isFinished = true
                            }
                        }
                    }
                }
            }
        }
    }
}
