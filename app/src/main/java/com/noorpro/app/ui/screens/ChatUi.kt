package com.noorpro.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.noorpro.app.ui.components.StitchEmerald
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchText
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 1.0.78: WhatsApp-style building blocks for Messages. */

val NoorReadBlue = Color(0xFF34B7F1)
val NoorOnlineGreen = Color(0xFF25D366)

/** 0 = pending (clock), 1 = sent (one grey tick), 2 = delivered (two grey), 3 = read (two blue). */
@Composable
fun NoorReadTicks(tick: Int, tint: Color, modifier: Modifier = Modifier) {
    AnimatedContent(targetState = tick, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "ticks") { t ->
        val (icon, color) = when (t) {
            0 -> Icons.Default.Schedule to tint
            1 -> Icons.Default.Done to tint
            2 -> Icons.Default.DoneAll to tint
            else -> Icons.Default.DoneAll to NoorReadBlue
        }
        Icon(icon, contentDescription = when (t) { 0 -> "Sending"; 1 -> "Sent"; 2 -> "Delivered"; else -> "Read" }, tint = color, modifier = modifier.size(15.dp))
    }
}

@Composable
fun NoorTypingDots(color: Color = NoorOnlineGreen) {
    val tr = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("typing", color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        repeat(3) { i ->
            val a by tr.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse), label = "d$i")
            Box(Modifier.padding(start = 2.dp).size(4.dp).alpha(a).clip(CircleShape).background(color))
        }
    }
}

/** Swipe right to reply: the bubble follows the finger and springs back, with a reply icon. */
@Composable
fun NoorSwipeToReply(onReply: () -> Unit, content: @Composable () -> Unit) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = com.noorpro.app.ui.components.rememberHaptic()
    var armed by remember { mutableStateOf(false) }
    Box {
        Icon(
            Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = stitchPrimary(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp).size(22.dp)
                .graphicsLayer { alpha = (offset.value / 140f).coerceIn(0f, 1f); scaleX = 0.6f + alpha * 0.4f; scaleY = scaleX }
        )
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (armed) onReply()
                            armed = false
                            scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        },
                        onDragCancel = { armed = false; scope.launch { offset.animateTo(0f) } },
                        onHorizontalDrag = { change, drag ->
                            val next = (offset.value + drag * 0.6f).coerceIn(0f, 180f)
                            if (next > 120f && !armed) { armed = true; haptic() }
                            if (next < 120f) armed = false
                            scope.launch { offset.snapTo(next) }
                            change.consume()
                        }
                    )
                }
        ) { content() }
    }
}

/** Centered WhatsApp-style call-log chip ("Missed voice call", "Video call · 2:31"). */
@Composable
fun NoorCallLogBubble(text: String, mine: Boolean, time: String, onCallBack: () -> Unit) {
    val missed = text.startsWith("Missed") || text.startsWith("Declined")
    val video = text.contains("video", ignoreCase = true)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(stitchLine().copy(alpha = 0.35f)).clickable(onClick = onCallBack)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(if (missed) Color(0xFFE5484D).copy(alpha = 0.15f) else NoorOnlineGreen.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(
                    if (missed) Icons.AutoMirrored.Filled.CallMissed else if (video) Icons.Default.Videocam else Icons.AutoMirrored.Filled.CallMade,
                    contentDescription = null, tint = if (missed) Color(0xFFE5484D) else NoorOnlineGreen, modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (!mine && text.startsWith("Missed")) text else text, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("$time · Tap to call back", color = stitchMutedText(), fontSize = 11.sp)
            }
        }
    }
}

fun noorChatListTime(ms: Long, now: Long = System.currentTimeMillis()): String {
    if (ms <= 0) return ""
    return when {
        android.text.format.DateUtils.isToday(ms) -> android.text.format.DateFormat.format("h:mm a", ms).toString()
        android.text.format.DateUtils.isToday(ms + 86_400_000L) -> "Yesterday"
        now - ms < 6 * 86_400_000L -> android.text.format.DateFormat.format("EEE", ms).toString()
        else -> android.text.format.DateFormat.format("d/M/yy", ms).toString()
    }
}

@Composable
fun NoorAvatar(name: String, photoUrl: String, size: Int, online: Boolean = false) {
    Box(Modifier.size(size.dp)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(StitchEmerald), contentAlignment = Alignment.Center) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(model = photoUrl, contentDescription = "$name photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.6).sp)
            }
        }
        if (online) {
            Box(
                Modifier.align(Alignment.BottomEnd).size((size / 3.8).dp).clip(CircleShape)
                    .background(NoorOnlineGreen).border(2.dp, com.noorpro.app.ui.components.stitchBackground(), CircleShape)
            )
        }
    }
}

/** Flat WhatsApp/Instagram chat row: avatar (+online dot), name + time, ticks/typing/preview, unread badge. */
@Composable
fun NoorChatListRow(
    name: String,
    photoUrl: String,
    preview: String,
    previewIsMine: Boolean,
    tick: Int,
    typing: Boolean,
    online: Boolean,
    unread: Long,
    updatedAt: Long,
    isCall: Boolean,
    onClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            NoorAvatar(name, photoUrl, 52, online)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = stitchText(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        noorChatListTime(updatedAt),
                        color = if (unread > 0) NoorOnlineGreen else stitchMutedText(),
                        fontSize = 12.sp, fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (typing) NoorTypingDots() else Row(verticalAlignment = Alignment.CenterVertically) {
                            if (previewIsMine && preview.isNotBlank()) { NoorReadTicks(tick, stitchMutedText()); Spacer(Modifier.width(3.dp)) }
                            if (isCall) { Icon(Icons.Default.Call, null, tint = stitchMutedText(), modifier = Modifier.size(14.dp)); Spacer(Modifier.width(3.dp)) }
                            Text(
                                preview.ifBlank { "Tap to chat" },
                                color = if (unread > 0) stitchText() else stitchMutedText(),
                                fontSize = 14.sp, fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (unread > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.defaultMinSize(minWidth = 22.dp).height(22.dp).clip(RoundedCornerShape(999.dp)).background(NoorOnlineGreen).padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(if (unread > 99) "99+" else unread.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        HorizontalDivider(color = stitchLine().copy(alpha = 0.4f), modifier = Modifier.padding(start = 71.dp))
    }
}

/** Live presence for up to 30 members (chat list online dots). */
@Composable
fun rememberPresenceMap(uids: List<String>): Map<String, com.noorpro.app.data.Presence> {
    val key = uids.distinct().filter { it.isNotBlank() }.take(30)
    var map by remember { mutableStateOf<Map<String, com.noorpro.app.data.Presence>>(emptyMap()) }
    DisposableEffect(key.joinToString(",")) {
        val regs = key.mapNotNull { uid ->
            com.noorpro.app.data.ChatRealtime.observePresence(uid) { p -> map = if (p == null) map - uid else map + (uid to p) }
        }
        onDispose { regs.forEach { it.remove() } }
    }
    return map
}

/** Ticks every few seconds so "typing" and "online" expire on screen. */
@Composable
fun rememberNowTicker(periodMs: Long = 3_000L): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) { while (true) { kotlinx.coroutines.delay(periodMs); now = System.currentTimeMillis() } }
    return now
}
