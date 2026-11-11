package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.UmmahMessage
import com.noorpro.app.data.messaging.ShareTarget
import com.noorpro.app.data.messaging.ShareTargets
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon

// ============================================ shared reel / post card =======================

/** View model of a "post_share" message — same fields as the live 1.0.82 UmmahMessage.shared*. */
data class MsgShared(
    val postId: String, val kind: String = "reel", val thumb: String = "", val creatorUid: String = "",
    val creatorName: String = "", val creatorPhoto: String = "", val caption: String = "",
    val unavailable: Boolean = false, val durationLabel: String = "",
) {
    val isReel get() = kind != "post"
    companion object {
        fun from(m: UmmahMessage, unavailable: Boolean = false) = MsgShared(
            m.sharedPostId, m.sharedPostKind.ifBlank { "reel" }, m.sharedThumb, m.sharedCreatorUid,
            m.sharedCreatorName, m.sharedCreatorPhoto, m.sharedCaption, unavailable)
    }
}

/** NoorPro reel/post card inside a chat bubble: tall cover + play, creator, caption; tap opens. */
@Composable
internal fun MsgSharedCard(s: MsgShared, mine: Boolean, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val ink = if (mine) MsgCream else stitchText()
    Column(
        modifier.width(208.dp).clip(RoundedCornerShape(16.dp))
            .background(if (mine) Color.White.copy(alpha = 0.10f) else msgMint().copy(alpha = 0.55f))
            .border(1.dp, StitchGold.copy(alpha = if (mine) 0.45f else 0.35f), RoundedCornerShape(16.dp))
            .clickable(enabled = !s.unavailable, onClick = onOpen).testTag("msg_shared_${s.postId}")
    ) {
        if (s.unavailable) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HideImage, null, tint = msgGold())
                Text(if (s.isReel) "Reel unavailable" else "Post unavailable", color = ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Removed, or the creator is blocked.", color = ink.copy(alpha = 0.65f), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
            return
        }
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            UmmahAvatar(s.creatorName, s.creatorPhoto, 26.dp, ring = StitchGold)
            Spacer(Modifier.width(8.dp))
            Text(s.creatorName.ifBlank { "NoorPro creator" }, color = ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(if (s.isReel) Icons.Default.Movie else Icons.Default.Article, null, tint = if (mine) StitchGold else msgGold(), modifier = Modifier.size(16.dp))
        }
        Box(Modifier.fillMaxWidth().aspectRatio(if (s.isReel) 3f / 4f else 4f / 3f).background(MsgHeroBrush)) {
            MsgImage(s.thumb, Modifier.fillMaxSize()) { Icon(Icons.Default.Movie, null, tint = StitchGold, modifier = Modifier.size(40.dp)) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xAA033D2E)))))
            if (s.isReel) {
                Box(Modifier.align(Alignment.Center).size(58.dp).clip(CircleShape).background(Color(0x66033D2E)).border(1.5.dp, StitchGold, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, "Play reel", tint = MsgCream, modifier = Modifier.size(34.dp))
                }
                Row(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(999.dp)).background(Color(0x99033D2E)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, null, tint = StitchGold, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp))
                    Text(if (s.durationLabel.isNotBlank()) "Reel · ${s.durationLabel}" else "Reel", color = MsgCream, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (s.caption.isNotBlank()) Text(
            s.caption, color = ink, fontSize = 12.5.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
        if (!s.isReel) Row(Modifier.fillMaxWidth().background(if (mine) Color.White.copy(alpha = 0.08f) else msgCard()).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (s.isReel) "Watch reel" else "View post", color = if (mine) StitchGold else msgGreen(), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = if (mine) StitchGold else msgGreen(), modifier = Modifier.size(18.dp))
        }
    }
}

// ============================================ share sheet ===================================

data class ShareSheetState(
    val shared: MsgShared,
    val targets: List<ShareTarget>,
    val query: String = "",
    val selected: Set<String> = emptySet(),       // ShareTarget.id
    val note: String = "",
    val groupsEnabled: Boolean = true,            // MessagingFlags.GROUP_SHARE (needs rules deploy)
    val sent: Boolean = false,
)

/**
 * NoorPro share sheet for reels/posts: search, recent & frequent row (people + circles),
 * multi-select, optional message, "Send to N", and Story / Copy link / Share / Save actions.
 */
@Composable
fun NoorShareSheet(
    state: ShareSheetState,
    onQuery: (String) -> Unit = {}, onToggle: (ShareTarget) -> Unit = {}, onNote: (String) -> Unit = {},
    onSend: () -> Unit = {}, onAddStory: () -> Unit = {}, onCopyLink: () -> Unit = {}, onShareOut: () -> Unit = {}, onSave: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val ranked = ShareTargets.rank(state.targets, state.query, state.groupsEnabled)
    val count = state.selected.size
    Box(Modifier.fillMaxSize().testTag("msg_share_host")) {
        // the reel behind the sheet
        MsgImage(state.shared.thumb, Modifier.fillMaxSize().background(MsgHeroBrush)) {}
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().shadow(16.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(msgBg())
                .border(1.dp, StitchGold.copy(alpha = 0.35f), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .navigationBarsPadding().testTag("msg_share_sheet")
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).size(44.dp, 4.dp).clip(CircleShape).background(StitchGold.copy(alpha = 0.7f)))
            Row(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                MsgImage(state.shared.thumb, Modifier.size(40.dp, 56.dp).clip(RoundedCornerShape(10.dp)).background(MsgHeroBrush)) {
                    Icon(Icons.Default.Movie, null, tint = StitchGold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    MsgTitle(if (state.shared.isReel) "Share reel" else "Share post", 22)
                    Text("${state.shared.creatorName} · ${state.shared.caption}", color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(msgMint()).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, "Close", tint = msgGreen(), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            MsgSearchPill(state.query, "Search people and circles", Modifier.padding(horizontal = 16.dp).testTag("msg_share_search"), onQuery)
            MsgSectionLabel(if (state.query.isBlank()) "Recent & frequent" else "Results", Modifier.padding(top = 8.dp), trailing = if (count > 0) "$count selected" else "")
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp).testTag("msg_share_recent"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ranked.take(8).forEach { t -> MsgShareBubble(t, t.id in state.selected) { onToggle(t) } }
            }
            Column(Modifier.padding(top = 4.dp)) {
                ranked.drop(8).take(2).forEach { t -> MsgShareRow(t, t.id in state.selected) { onToggle(t) } }
            }
            // optional note + send
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (count > 0) Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(999.dp)).background(msgCard()).border(1.dp, msgHair(), RoundedCornerShape(999.dp)).padding(horizontal = 16.dp).testTag("msg_share_note"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EditNote, null, tint = msgGold(), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (state.note.isEmpty()) Text("Add a message…", color = stitchMutedText(), fontSize = 14.5.sp)
                        BasicTextField(state.note, { if (it.length <= 500) onNote(it) }, singleLine = true, textStyle = TextStyle(color = stitchText(), fontSize = 14.5.sp),
                            cursorBrush = SolidColor(msgGreen()), modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(999.dp))
                        .background(if (count > 0 || state.sent) Brush.linearGradient(listOf(Color(0xFF0E7A55), MsgGreen)) else Brush.linearGradient(listOf(msgMint(), msgMint())))
                        .border(1.dp, if (count > 0) StitchGold.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(999.dp))
                        .clickable(enabled = count > 0 && !state.sent, onClick = onSend).testTag("msg_share_send"),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (state.sent) Icons.Default.Check else Icons.AutoMirrored.Filled.Send, null, tint = if (count > 0) StitchGold else stitchMutedText(), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.sent) "Sent · JazakAllahu khayran" else ShareTargets.sendLabel(count), color = if (count > 0) MsgCream else stitchMutedText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(msgHair()))
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("msg_share_actions"), horizontalArrangement = Arrangement.SpaceEvenly) {
                MsgShareAction(Icons.Default.AddCircleOutline, "Add to story", onAddStory)
                MsgShareAction(Icons.Default.Link, "Copy link", onCopyLink)
                MsgShareAction(Icons.Default.Share, "Share out", onShareOut)
                MsgShareAction(Icons.Default.BookmarkBorder, "Save", onSave)
            }
        }
    }
}

@Composable
private fun MsgShareBubble(t: ShareTarget, on: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(78.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp).testTag("msg_share_t_${t.id}"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(Modifier.size(64.dp).then(if (on) Modifier.border(2.5.dp, msgGreen(), if (t.kind == ShareTarget.Kind.GROUP) RoundedCornerShape(20.dp) else CircleShape) else Modifier).padding(3.dp)) {
                if (t.kind == ShareTarget.Kind.GROUP && t.group != null) MsgGroupAvatar(t.group, 58.dp) else MsgPersonAvatar(t.name, t.photoUrl, 58.dp, online = t.online && !on)
            }
            if (on) Box(Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(MsgGreen).border(2.dp, msgBg(), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, "Selected", tint = StitchGold, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(t.name.substringBefore(' ').take(12), color = stitchText(), fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (t.kind == ShareTarget.Kind.GROUP) "Circle" else if (t.sharesSent > 2) "Frequent" else "Recent", color = if (t.kind == ShareTarget.Kind.GROUP) msgGold() else stitchMutedText(), fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun MsgShareRow(t: ShareTarget, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp).testTag("msg_share_row_${t.id}"), verticalAlignment = Alignment.CenterVertically) {
        if (t.kind == ShareTarget.Kind.GROUP && t.group != null) MsgGroupAvatar(t.group, 42.dp) else MsgPersonAvatar(t.name, t.photoUrl, 42.dp, online = t.online)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.subtitle, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(26.dp).clip(CircleShape).background(if (on) MsgGreen else Color.Transparent).border(1.5.dp, if (on) MsgGreen else stitchMutedText(), CircleShape), contentAlignment = Alignment.Center) {
            if (on) Icon(Icons.Default.Check, "Selected", tint = StitchGold, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun MsgShareAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.width(80.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(msgMint()).border(1.dp, msgHair(), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = msgGreen(), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = stitchText(), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
