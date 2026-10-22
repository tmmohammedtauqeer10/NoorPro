package com.noorpro.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.ui.theme.MatteGold
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NoorGreen = Color(0xFF1F7A5A)

private class ChatPalette(dark: Boolean) {
    val bg = if (dark) Color(0xFF0B1512) else Color(0xFFFBF8F1)
    val surface = if (dark) Color(0xFF13211C) else Color.White
    val text = if (dark) Color(0xFFE9EFEA) else Color(0xFF1C2622)
    val muted = if (dark) Color(0xFF93A39B) else Color(0xFF6B7770)
    val userBubble = if (dark) Color(0xFF1E3A2F) else Color(0xFFF3E7C4)
    val userText = if (dark) Color(0xFFF1F5F2) else Color(0xFF2A2410)
    val chip = if (dark) Color(0xFF1A2A24) else Color(0xFFF1EEE4)
    val line = if (dark) Color(0xFF223630) else Color(0xFFE7E1D3)
    val md = NoorAiMdColors(
        text = text, muted = muted, accent = if (dark) MatteGold else Color(0xFF9A7A12),
        quoteBar = MatteGold, quoteBg = if (dark) Color(0xFF17261F) else Color(0xFFF7F1E0),
        chipBg = chip, codeBg = chip,
    )
}

private const val DISCLAIMER = "Noor AI can make mistakes. For fatwa, divorce, inheritance, medical, legal or serious personal matters, please consult a qualified scholar."

@Composable
fun NoorAiChatContent(
    mode: NoorAiMode,
    messages: List<NoorAiChatItem>,
    input: String,
    isTyping: Boolean,
    showTip: Boolean,
    revealId: Long?,
    onBack: () -> Unit,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
    onStop: () -> Unit,
    onModeChange: (NoorAiMode) -> Unit,
    onClearChat: () -> Unit,
    onDismissTip: () -> Unit,
    onCopy: (NoorAiChatItem) -> Unit,
    onShare: (NoorAiChatItem) -> Unit,
    onRegenerate: (NoorAiChatItem) -> Unit,
    onFeedback: (NoorAiChatItem, Int) -> Unit,
    onRevealDone: () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val p = remember(dark) { ChatPalette(dark) }
    val listState = rememberLazyListState()
    var showInfo by remember { mutableStateOf(false) }
    val itemCount = messages.size + (if (isTyping) 1 else 0)
    LaunchedEffect(itemCount, mode) { if (itemCount > 0) listState.animateScrollToItem(itemCount - 1) }

    Column(
        Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        ChatHeader(p, onBack, { showInfo = true }, onClearChat, messages.isNotEmpty())
        ModeRow(p, mode, onModeChange)
        if (showTip) TipRow(p, onDismissTip)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && !isTyping) {
                EmptyState(p, mode, onSuggestion)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(messages, key = { it.id }) { m ->
                        if (m.isUser) UserBubble(p, m)
                        else AssistantMessage(
                            p, m, reveal = m.id == revealId, isLast = m == messages.last(), busy = isTyping,
                            onCopy = { onCopy(m) }, onShare = { onShare(m) }, onRegenerate = { onRegenerate(m) },
                            onFeedback = { onFeedback(m, it) }, onRevealDone = onRevealDone,
                        )
                    }
                    if (isTyping) item(key = "typing") { TypingRow(p) }
                }
            }
        }
        InputBar(p, input, mode.promptHint, isTyping, onInput, onSend, onStop)
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
            title = { Text("About Noor AI") },
            text = { Text("Noor AI gives Sunni-grounded study help with Qur'an and Hadith references. If the online assistant is unavailable, it shows related passages saved on your phone.\n\n$DISCLAIMER") },
        )
    }
}

@Composable
private fun ChatHeader(p: ChatPalette, onBack: () -> Unit, onInfo: () -> Unit, onClear: () -> Unit, canClear: Boolean) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = p.text) }
        Avatar(28.dp)
        Spacer(Modifier.width(10.dp))
        Text("Noor AI", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(NoorGreen.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Shield, null, tint = NoorGreen, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text("Safe", color = NoorGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onInfo) { Icon(Icons.Default.Info, "About Noor AI", tint = p.muted) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More", tint = p.muted) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Clear chat") }, enabled = canClear, onClick = { menu = false; onClear() })
            }
        }
    }
    HorizontalDivider(color = p.line, thickness = 0.5.dp)
}

@Composable
private fun Avatar(size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(MatteGold, NoorGreen))),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
private fun ModeRow(p: ChatPalette, mode: NoorAiMode, onMode: (NoorAiMode) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(NoorAiMode.values().toList()) { m ->
            val sel = m == mode
            Text(
                m.shortTitle,
                color = if (sel) Color.White else p.text,
                fontSize = 13.sp,
                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (sel) NoorGreen else p.chip)
                    .clickable { onMode(m) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun TipRow(p: ChatPalette, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(10.dp)).background(p.chip)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Tip: Noor AI can make mistakes; ask a scholar for rulings.", color = p.muted, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Dismiss tip", tint = p.muted, modifier = Modifier.size(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(p: ChatPalette, mode: NoorAiMode, onSuggestion: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Avatar(56.dp)
        Spacer(Modifier.height(14.dp))
        Text("Assalamu alaikum", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(mode.helperText, color = p.muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            noorAiSuggestions(mode).forEach { s ->
                Text(
                    s, color = p.text, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, p.line, RoundedCornerShape(16.dp))
                        .background(p.surface).clickable { onSuggestion(s) }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun timeOf(ms: Long) = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

@Composable
private fun UserBubble(p: ChatPalette, m: NoorAiChatItem) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Text(
            m.text, color = p.userText, fontSize = 15.sp, lineHeight = 22.sp,
            modifier = Modifier.widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)).background(p.userBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        Text(timeOf(m.time), color = p.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp, end = 4.dp))
    }
}

@Composable
private fun AssistantMessage(
    p: ChatPalette, m: NoorAiChatItem, reveal: Boolean, isLast: Boolean, busy: Boolean,
    onCopy: () -> Unit, onShare: () -> Unit, onRegenerate: () -> Unit, onFeedback: (Int) -> Unit, onRevealDone: () -> Unit,
) {
    // Simple streaming-like reveal: grow the visible text by whole lines for the newest answer.
    val lines = remember(m.text) { m.text.lines() }
    var shown by remember(m.id) { mutableIntStateOf(if (reveal) 1 else lines.size) }
    LaunchedEffect(m.id, reveal) {
        if (reveal) {
            while (shown < lines.size) { delay(35); shown++ }
            onRevealDone()
        }
    }
    val visible = if (shown >= lines.size) m.text else lines.take(shown).joinToString("\n")
    Row(Modifier.fillMaxWidth()) {
        Avatar(26.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            NoorAiMarkdownText(visible, p.md)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp).alpha(if (shown >= lines.size) 1f else 0f)) {
                Text(timeOf(m.time), color = p.muted, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                ActionIcon(Icons.Default.ContentCopy, "Copy", p.muted, onCopy)
                ActionIcon(Icons.Default.Share, "Share", p.muted, onShare)
                if (isLast && !busy) ActionIcon(Icons.Default.Refresh, "Regenerate", p.muted, onRegenerate)
                ActionIcon(if (m.feedback == 1) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp, "Helpful", if (m.feedback == 1) NoorGreen else p.muted) { onFeedback(1) }
                ActionIcon(if (m.feedback == -1) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown, "Not helpful", if (m.feedback == -1) Color(0xFFC0563F) else p.muted) { onFeedback(-1) }
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) { Icon(icon, label, tint = tint, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun TypingRow(p: ChatPalette) {
    val t = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(26.dp)
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).background(p.chip).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { i ->
                val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse), label = "dot$i")
                Box(Modifier.size(7.dp).alpha(a).clip(CircleShape).background(p.muted))
            }
        }
    }
}

@Composable
private fun InputBar(p: ChatPalette, value: String, hint: String, busy: Boolean, onValue: (String) -> Unit, onSend: () -> Unit, onStop: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp)).background(p.surface).border(1.dp, p.line, RoundedCornerShape(26.dp))
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
            if (value.isEmpty()) Text(hint, color = p.muted, fontSize = 15.sp, maxLines = 1)
            BasicTextField(
                value = value, onValueChange = onValue, maxLines = 5,
                textStyle = TextStyle(color = p.text, fontSize = 15.sp, lineHeight = 21.sp),
                cursorBrush = SolidColor(NoorGreen), modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(6.dp))
        val canSend = value.isNotBlank()
        val bg = when { busy -> p.text; canSend -> NoorGreen; else -> p.chip }
        IconButton(
            onClick = { if (busy) onStop() else if (canSend) onSend() },
            enabled = busy || canSend,
            modifier = Modifier.size(38.dp).clip(CircleShape).background(bg),
        ) {
            if (busy) Icon(Icons.Default.Stop, "Stop", tint = p.bg, modifier = Modifier.size(18.dp))
            else Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (canSend) Color.White else p.muted, modifier = Modifier.size(18.dp))
        }
    }
}
