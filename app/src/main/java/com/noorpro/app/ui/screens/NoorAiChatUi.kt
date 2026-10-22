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
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    language: String = "en",
    onLanguage: (String) -> Unit = {},
    conversations: List<NoorAiConversation> = emptyList(),
    currentConversation: Long? = null,
    onNewChat: () -> Unit = {},
    onOpenConversation: (NoorAiConversation) -> Unit = {},
    onDeleteConversation: (NoorAiConversation) -> Unit = {},
    onVoice: () -> Unit = {},
    dailyTip: NoorDailyTip? = null,
    initialHistoryOpen: Boolean = false,
) {
    var showHistory by remember { mutableStateOf(initialHistoryOpen) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val p = remember(dark) { ChatPalette(dark) }
    val listState = rememberLazyListState()
    var showInfo by remember { mutableStateOf(false) }
    val itemCount = messages.size + (if (isTyping) 1 else 0)
    LaunchedEffect(itemCount, mode) { if (itemCount > 0) listState.animateScrollToItem(itemCount - 1) }

    Column(
        Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        ChatHeader(p, onBack, { showInfo = true }, onClearChat, messages.isNotEmpty(), language, onLanguage, { showHistory = true }, onNewChat)
        ModeRow(p, mode, onModeChange)
        if (showTip) TipRow(p, onDismissTip)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && !isTyping) {
                EmptyState(p, mode, onSuggestion, dailyTip)
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
                            onFollowUp = onSuggestion,
                        )
                    }
                    if (isTyping) item(key = "typing") { TypingRow(p) }
                }
            }
        }
        InputBar(p, input, mode.promptHint, isTyping, onInput, onSend, onStop, onVoice)
    }

    if (showHistory) {
        HistorySheet(p, conversations, currentConversation, onDismiss = { showHistory = false },
            onOpen = { showHistory = false; onOpenConversation(it) }, onDelete = onDeleteConversation,
            onNew = { showHistory = false; onNewChat() })
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
private fun ChatHeader(
    p: ChatPalette, onBack: () -> Unit, onInfo: () -> Unit, onClear: () -> Unit, canClear: Boolean,
    language: String, onLanguage: (String) -> Unit, onHistory: () -> Unit, onNewChat: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = p.text) }
        IconButton(onClick = onHistory) { Icon(Icons.Default.History, "Chat history", tint = p.text) }
        Avatar(26.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Noor AI", color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = NoorGreen, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text("Sunni-grounded · " + (NOOR_AI_LANGUAGES.firstOrNull { it.first == language }?.second?.substringAfterLast(' ') ?: "English"),
                    color = p.muted, fontSize = 11.sp, maxLines = 1)
            }
        }
        IconButton(onClick = onNewChat) { Icon(Icons.Default.AddComment, "New chat", tint = p.text) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More", tint = p.muted) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                Text("Answer language", color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                NOOR_AI_LANGUAGES.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        leadingIcon = { if (code == language) Icon(Icons.Default.Check, null, tint = NoorGreen) else Spacer(Modifier.size(24.dp)) },
                        onClick = { menu = false; onLanguage(code) })
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text("About Noor AI") }, onClick = { menu = false; onInfo() })
                DropdownMenuItem(text = { Text("Clear this chat") }, enabled = canClear, onClick = { menu = false; onClear() })
            }
        }
    }
    HorizontalDivider(color = p.line, thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    p: ChatPalette, conversations: List<NoorAiConversation>, current: Long?,
    onDismiss: () -> Unit, onOpen: (NoorAiConversation) -> Unit, onDelete: (NoorAiConversation) -> Unit, onNew: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = p.surface) {
        HistoryList(p, conversations, current, onOpen, onDelete, onNew)
    }
}

@Composable
internal fun HistoryListPreview(dark: Boolean, conversations: List<NoorAiConversation>) {
    val p = remember(dark) { ChatPalette(dark) }
    Box(Modifier.fillMaxSize().background(p.bg).statusBarsPadding()) {
        Column(Modifier.fillMaxWidth().padding(top = 60.dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(p.surface).padding(top = 18.dp)) {
            HistoryList(p, conversations, conversations.firstOrNull()?.id, {}, {}, {})
        }
    }
}

@Composable
private fun HistoryList(
    p: ChatPalette, conversations: List<NoorAiConversation>, current: Long?,
    onOpen: (NoorAiConversation) -> Unit, onDelete: (NoorAiConversation) -> Unit, onNew: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chats", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(NoorGreen).clickable(onClick = onNew).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.AddComment, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New chat", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (conversations.isEmpty()) {
            Text("No saved chats yet. Your conversations stay on this phone.", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 18.dp))
        }
        LazyColumn(Modifier.heightIn(max = 520.dp)) {
            items(conversations, key = { it.id }) { c ->
                val sel = c.id == current
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (sel) p.chip else Color.Transparent)
                        .clickable { onOpen(c) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = p.muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.title, color = p.text, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("${c.mode.shortTitle} · ${relativeDay(c.updated)}", color = p.muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { onDelete(c) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, "Delete chat", tint = p.muted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun relativeDay(ms: Long): String {
    val days = ((System.currentTimeMillis() - ms) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "Today"
        days == 1 -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
    }
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
private fun EmptyState(p: ChatPalette, mode: NoorAiMode, onSuggestion: (String) -> Unit, tip: NoorDailyTip?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Avatar(56.dp)
        Spacer(Modifier.height(14.dp))
        Text("Assalamu alaikum", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(mode.helperText, color = p.muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (tip != null && mode == NoorAiMode.AskNoor) {
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(NoorGreen.copy(alpha = 0.16f), MatteGold.copy(alpha = 0.14f))))
                    .border(1.dp, p.line, RoundedCornerShape(18.dp)).padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, null, tint = MatteGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Today's Islamic tip", color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                Text(tip.text, color = p.text, fontSize = 15.sp, lineHeight = 22.sp)
                Text(tip.ref, color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ask Noor AI about this", color = NoorGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onSuggestion("Explain in detail: \"${tip.text}\" (${tip.ref}) — meaning, context and how to act on it today.") }
                        .padding(vertical = 4.dp)
                )
            }
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantMessage(
    p: ChatPalette, m: NoorAiChatItem, reveal: Boolean, isLast: Boolean, busy: Boolean,
    onCopy: () -> Unit, onShare: () -> Unit, onRegenerate: () -> Unit, onFeedback: (Int) -> Unit, onRevealDone: () -> Unit,
    onFollowUp: (String) -> Unit = {},
) {
    val (body, followUps) = remember(m.text) { com.noorpro.app.data.AiFeatureService.splitFollowups(m.text) }
    // Streaming-style typing: reveal the newest answer a few words at a time with a blinking caret.
    val words = remember(body) { Regex("\\S+\\s*").findAll(body).map { it.value }.toList() }
    var shown by remember(m.id) { mutableIntStateOf(if (reveal) 0 else words.size) }
    LaunchedEffect(m.id, reveal) {
        if (reveal) {
            while (shown < words.size) { delay(16); shown = (shown + 3).coerceAtMost(words.size) }
            onRevealDone()
        }
    }
    val done = shown >= words.size
    val visible = if (done) body else words.take(shown).joinToString("") + " ▍"
    Row(Modifier.fillMaxWidth()) {
        Avatar(26.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            NoorAiMarkdownText(visible, p.md)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp).alpha(if (done) 1f else 0f)) {
                Text(timeOf(m.time), color = p.muted, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                ActionIcon(Icons.Default.ContentCopy, "Copy", p.muted, onCopy)
                ActionIcon(Icons.Default.Share, "Share", p.muted, onShare)
                if (isLast && !busy) ActionIcon(Icons.Default.Refresh, "Regenerate", p.muted, onRegenerate)
                ActionIcon(if (m.feedback == 1) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp, "Helpful", if (m.feedback == 1) NoorGreen else p.muted) { onFeedback(1) }
                ActionIcon(if (m.feedback == -1) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown, "Not helpful", if (m.feedback == -1) Color(0xFFC0563F) else p.muted) { onFeedback(-1) }
            }
            if (isLast && done && !busy && followUps.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    followUps.forEach { q ->
                        Row(
                            Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, p.line, RoundedCornerShape(14.dp)).background(p.surface)
                                .clickable { onFollowUp(q) }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.SubdirectoryArrowRight, null, tint = NoorGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(q, color = p.text, fontSize = 13.sp)
                        }
                    }
                }
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
private fun InputBar(p: ChatPalette, value: String, hint: String, busy: Boolean, onValue: (String) -> Unit, onSend: () -> Unit, onStop: () -> Unit, onVoice: () -> Unit = {}) {
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
        if (!busy && !canSend) {
            IconButton(onClick = onVoice, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Mic, "Ask by voice", tint = p.text, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(2.dp))
        }
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
