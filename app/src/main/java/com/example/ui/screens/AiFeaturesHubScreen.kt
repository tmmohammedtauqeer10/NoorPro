package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiFeatureService
import com.example.data.NoorAiMessage
import com.example.data.NoorAiMode
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassOverlay
import com.example.ui.theme.MatteGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

@Composable
fun AiFeaturesHubScreen(viewModel: DeenViewModel) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val screenBrush = if (isLightTheme) {
        Brush.verticalGradient(listOf(Color(0xFFFFF7FB), Color(0xFFFFFCFA), Color(0xFFFFF2EC)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF07110F), Color(0xFF0B1713), Color(0xFF101B18)))
    }

    var selectedMode by remember { mutableStateOf(NoorAiMode.AskNoor) }
    var input by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var messages by remember {
        mutableStateOf(
            listOf(
                NoorAiMessage(
                    text = "Assalamu alaikum. I am Noor AI. Ask me for Islamic guidance, dua help, Quran reflection, Hadith explanation, Hajj & Umrah steps, or help using the Noor Pro app. I answer in a detailed step-by-step style.",
                    isUser = false
                )
            )
        )
    }

    fun sendMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || isTyping) return
        val previousMessages = messages
        messages = previousMessages + NoorAiMessage(clean, true)
        input = ""
        isTyping = true
        focusManager.clearFocus()
        scope.launch {
            // Pass the running conversation before the new user turn so Noor AI has stable memory.
            val answer = AiFeatureService.generateResponse(clean, selectedMode, previousMessages)
            messages = messages + NoorAiMessage(answer, false)
            isTyping = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBrush)
                .padding(padding)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            NoorAiHeader(
                isLightTheme = isLightTheme,
                onBack = { viewModel.navigateTo(DeenScreen.DASHBOARD) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            NoorAiSafetyCard(isLightTheme = isLightTheme)
            Spacer(modifier = Modifier.height(14.dp))
            NoorAiModeSelector(
                selectedMode = selectedMode,
                onModeSelected = {
                    selectedMode = it
                    messages = listOf(
                        NoorAiMessage(
                            text = "${it.title} is ready. ${it.helperText}",
                            isUser = false
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    NoorAiPromptSuggestions(
                        mode = selectedMode,
                        onSuggestion = { input = it }
                    )
                }
                items(messages) { message ->
                    NoorAiMessageBubble(message = message, isLightTheme = isLightTheme)
                }
                if (isTyping) {
                    item { NoorAiTypingBubble(isLightTheme = isLightTheme) }
                }
            }

            NoorAiInputBar(
                value = input,
                onValueChange = { input = it },
                placeholder = selectedMode.promptHint,
                isTyping = isTyping,
                onSend = { sendMessage(input) }
            )
        }
    }
}

@Composable
private fun NoorAiHeader(
    isLightTheme: Boolean,
    onBack: () -> Unit
) {
    val card = if (isLightTheme) Color.White.copy(alpha = 0.94f) else Color(0xFF101B18).copy(alpha = 0.96f)
    val border = if (isLightTheme) Color(0xFFF1C8D8) else Color(0xFF2B3C35)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(MatteGold, Color(0xFFE58E10))))
                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF050C18), modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Noor AI", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Fast help, longer answers, app guidance", color = TextSecondary, fontSize = 13.sp)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = card),
            border = BorderStroke(1.dp, border),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MatteGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Safe", color = MatteGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NoorAiSafetyCard(isLightTheme: Boolean) {
    val container = if (isLightTheme) Color.White.copy(alpha = 0.96f) else Color(0xFF101B18).copy(alpha = 0.96f)
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, if (isLightTheme) Color(0xFFF1C8D8) else Color(0xFF2B3C35)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MatteGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "AI can make mistakes. For fatwa, divorce, inheritance, medical, legal, or serious personal issues, ask a qualified scholar.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun NoorAiModeSelector(
    selectedMode: NoorAiMode,
    onModeSelected: (NoorAiMode) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(NoorAiMode.entries) { mode ->
            val selected = selectedMode == mode
            Surface(
                modifier = Modifier.clickable { onModeSelected(mode) },
                color = if (selected) MatteGold else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                contentColor = if (selected) Color(0xFF050C18) else TextPrimary,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (selected) MatteGold else GlassBorder)
            ) {
                Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(noorAiModeIcon(mode), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(mode.shortTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun NoorAiPromptSuggestions(
    mode: NoorAiMode,
    onSuggestion: (String) -> Unit
) {
    val suggestions = remember(mode) {
        when (mode) {
            NoorAiMode.AskNoor -> listOf("How can I improve my salah?", "How to build daily Quran habit?", "What should I ask a scholar?")
            NoorAiMode.DuaGenerator -> listOf("Dua for exam stress", "Dua for parents health", "Dua for forgiveness")
            NoorAiMode.QuranHelper -> listOf("Explain mercy in Quran simply", "What lesson from Surah Fatiha?", "How to reflect on an ayah?")
            NoorAiMode.HadithExplainer -> listOf("Explain hadith about intentions", "Daily action from this hadith", "How to check hadith authenticity?")
            NoorAiMode.HajjUmrahGuide -> listOf("Umrah steps in order", "What after Tawaf?", "Packing list for Umrah")
            NoorAiMode.AppGuide -> listOf("How do I upload a reel?", "How to change my username?", "How do group chats work?")
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(suggestions) { suggestion ->
            Surface(
                modifier = Modifier.clickable { onSuggestion(suggestion) },
                color = MatteGold.copy(alpha = 0.12f),
                contentColor = MatteGold,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.25f))
            ) {
                Text(suggestion, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NoorAiMessageBubble(
    message: NoorAiMessage,
    isLightTheme: Boolean
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when {
        message.isUser -> MatteGold
        isLightTheme -> Color.White.copy(alpha = 0.96f)
        else -> Color(0xFF101B18).copy(alpha = 0.98f)
    }
    val textColor = if (message.isUser) Color(0xFF050C18) else TextPrimary

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (message.isUser) 22.dp else 6.dp,
                        bottomEnd = if (message.isUser) 6.dp else 22.dp
                    )
                )
                .background(bubbleColor)
                .border(1.dp, if (message.isUser) MatteGold else GlassBorder, RoundedCornerShape(22.dp))
                .padding(14.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun NoorAiTypingBubble(isLightTheme: Boolean) {
    val container = if (isLightTheme) Color.White.copy(alpha = 0.96f) else Color(0xFF101B18).copy(alpha = 0.98f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = MatteGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text("Noor AI is preparing a detailed answer...", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("If the cloud is slow, a safe offline answer appears quickly.", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NoorAiInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isTyping: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            shape = RoundedCornerShape(24.dp),
            minLines = 1,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isTyping) MatteGold.copy(alpha = 0.45f) else MatteGold)
                .clickable(enabled = !isTyping) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF050C18))
        }
    }
}

private fun noorAiModeIcon(mode: NoorAiMode): ImageVector = when (mode) {
    NoorAiMode.AskNoor -> Icons.Default.AutoAwesome
    NoorAiMode.DuaGenerator -> Icons.Default.Favorite
    NoorAiMode.QuranHelper -> Icons.Default.MenuBook
    NoorAiMode.HadithExplainer -> Icons.Default.Star
    NoorAiMode.HajjUmrahGuide -> Icons.Default.Mosque
    NoorAiMode.AppGuide -> Icons.Default.Info
}
