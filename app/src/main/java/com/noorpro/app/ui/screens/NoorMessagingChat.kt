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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.messaging.*
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon

// ============================================ shared chat bits ==============================

enum class MsgLineType { TEXT, VOICE, IMAGE, DAY, SYSTEM, DUA, SHARED }

data class MsgLine(
    val id: String,
    val type: MsgLineType = MsgLineType.TEXT,
    val mine: Boolean = false,
    val text: String = "",
    val time: String = "",
    val tick: Int = 3,
    val senderName: String = "",
    val senderPhoto: String = "",
    val senderRole: GroupRole = GroupRole.MEMBER,
    val firstInRun: Boolean = true,
    val replyTo: Pair<String, String>? = null,   // sender, snippet
    val reactions: String = "",
    val durationMs: Long = 0,
    val imageUrl: String = "",
    /** 1.1.3: shared reel/post card (maps 1:1 to the live "post_share" message fields). */
    val shared: MsgShared? = null,
)

private val nameColors = listOf(Color(0xFF0B6243), Color(0xFFA9842A), Color(0xFF7A4E9E), Color(0xFF1F6FA3), Color(0xFFB0503A))
@Composable private fun senderColor(name: String): Color {
    val c = nameColors[(name.hashCode() and 0x7fffffff) % nameColors.size]
    return if (isStitchLight()) c else c.copy(red = (c.red + 0.45f).coerceAtMost(1f), green = (c.green + 0.45f).coerceAtMost(1f), blue = (c.blue + 0.45f).coerceAtMost(1f))
}

@Composable
private fun MsgLineView(m: MsgLine, group: Boolean, onOpenShared: (MsgShared) -> Unit = {}) {
    when (m.type) {
        MsgLineType.DAY -> Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(m.text, color = msgGold(), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(msgCard()).border(1.dp, msgHair(), RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 4.dp))
        }
        MsgLineType.SYSTEM -> Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("msg_system"), contentAlignment = Alignment.Center) {
            Row(Modifier.clip(RoundedCornerShape(999.dp)).background(msgMint()).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = msgGreen(), modifier = Modifier.size(13.dp)); Spacer(Modifier.width(5.dp))
                Text(m.text, color = msgGreen(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        else -> MsgBubbleRow(m, group, onOpenShared)
    }
}

@Composable
private fun MsgBubbleRow(m: MsgLine, group: Boolean, onOpenShared: (MsgShared) -> Unit = {}) {
    val shape = if (m.mine) RoundedCornerShape(20.dp, 20.dp, if (m.firstInRun) 6.dp else 20.dp, 20.dp)
    else RoundedCornerShape(20.dp, 20.dp, 20.dp, if (m.firstInRun) 6.dp else 20.dp)
    val content = if (m.mine) MsgCream else stitchText()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (m.firstInRun) 4.dp else 1.5.dp),
        horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom
    ) {
        if (group && !m.mine) {
            if (m.firstInRun) UmmahAvatar(m.senderName, m.senderPhoto, 30.dp) else Spacer(Modifier.width(30.dp))
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 300.dp)) {
            Column(
                Modifier.clip(shape)
                    .then(if (m.mine) Modifier.background(Brush.linearGradient(listOf(Color(0xFF0E7A55), MsgGreen))) else Modifier.background(msgCard()).border(1.dp, msgHair(), shape))
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)
            ) {
                if (group && !m.mine && m.firstInRun) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.senderName, color = senderColor(m.senderName), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    if (m.senderRole != GroupRole.MEMBER) { Spacer(Modifier.width(6.dp)); MsgRoleChip(m.senderRole, small = true) }
                }
                m.replyTo?.let { (who, snip) ->
                    Row(Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(if (m.mine) Color.White.copy(alpha = 0.12f) else msgMint()).height(IntrinsicSize.Min)) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(StitchGold))
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(who, color = if (m.mine) StitchGold else msgGreen(), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text(snip, color = content.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                when (m.type) {
                    MsgLineType.VOICE -> MsgVoiceBody(m, content)
                    MsgLineType.IMAGE -> {
                        MsgImage(m.imageUrl, Modifier.padding(top = 2.dp).width(240.dp).height(150.dp).clip(RoundedCornerShape(14.dp)).background(msgMint())) {
                            Icon(Icons.Default.Image, null, tint = msgGreen())
                        }
                        if (m.text.isNotBlank()) Text(m.text, color = content, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    MsgLineType.SHARED -> {
                        m.shared?.let { MsgSharedCard(it, m.mine, Modifier.padding(top = 2.dp)) { onOpenShared(it) } }
                        if (m.text.isNotBlank()) Text(m.text, color = content, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp).widthIn(max = 208.dp))
                    }
                    MsgLineType.DUA -> Column(Modifier.padding(vertical = 2.dp)) {
                        Text("DUA", color = StitchGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        Text(m.text, color = content, fontFamily = LibreCaslon, fontSize = 16.sp, fontStyle = FontStyle.Italic, lineHeight = 22.sp)
                    }
                    else -> Text(m.text, color = content, fontSize = 15.sp, lineHeight = 20.sp)
                }
                Row(Modifier.align(Alignment.End).padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.time, color = content.copy(alpha = 0.6f), fontSize = 10.5.sp)
                    if (m.mine) { Spacer(Modifier.width(3.dp)); MsgTicks(m.tick, onGreen = true) }
                }
            }
            if (m.reactions.isNotBlank()) Text(
                m.reactions, fontSize = 12.sp, color = stitchText(),
                modifier = Modifier.offset(y = (-4).dp).padding(horizontal = 8.dp).clip(RoundedCornerShape(999.dp)).background(msgCard())
                    .border(1.dp, msgHair(), RoundedCornerShape(999.dp)).padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MsgVoiceBody(m: MsgLine, content: Color) {
    val bars = remember(m.id) { val r = java.util.Random(m.id.hashCode().toLong()); List(34) { 15 + r.nextInt(75) } }
    Row(Modifier.width(232.dp).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (m.mine) StitchGold else MsgGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, "Play voice message", tint = if (m.mine) StitchEmeraldDeep else MsgCream)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Waveform(bars, 0.35f, played = if (m.mine) StitchGold else msgGold(), idle = content.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth().height(28.dp))
            Text(formatVoice(m.durationMs), color = content.copy(alpha = 0.65f), fontSize = 11.sp)
        }
    }
}

@Composable
internal fun MsgRoleChip(role: GroupRole, small: Boolean = false) {
    val owner = role == GroupRole.OWNER
    Text(
        role.label, color = if (owner) StitchEmeraldDeep else msgGreen(), fontSize = if (small) 9.5.sp else 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (owner) StitchGold else msgMint()).padding(horizontal = if (small) 5.dp else 8.dp, vertical = if (small) 1.dp else 3.dp)
    )
}

@Composable
private fun MsgChatHeader(title: String, subtitle: String, subtitleGreen: Boolean, avatar: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(msgBg()).statusBarsPadding().padding(start = 4.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).clickable {}, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = msgGreen())
        }
        avatar()
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = if (subtitleGreen) Color(0xFF1FA66A) else stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(msgHair()))
}

@Composable
private fun MsgQuickReplies(replies: List<String>) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp).testTag("msg_quick_replies"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        replies.forEach { r ->
            Row(Modifier.clip(RoundedCornerShape(999.dp)).background(msgCard()).border(1.dp, StitchGold.copy(alpha = 0.5f), RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = msgGold(), modifier = Modifier.size(13.dp)); Spacer(Modifier.width(5.dp))
                Text(r, color = msgGreen(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MsgComposer(draft: String, hint: String = "Write a kind message…") {
    Row(Modifier.fillMaxWidth().background(msgBg()).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp).testTag("msg_composer"), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = 50.dp).clip(RoundedCornerShape(26.dp)).background(msgCard()).border(1.dp, msgHair(), RoundedCornerShape(26.dp)).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MsgComposerIcon(Icons.Default.EmojiEmotions, "Emoji")
            Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                if (draft.isEmpty()) Text(hint, color = stitchMutedText(), fontSize = 15.sp)
                BasicTextField(draft, {}, textStyle = TextStyle(color = stitchText(), fontSize = 15.sp), cursorBrush = SolidColor(msgGreen()), modifier = Modifier.fillMaxWidth())
            }
            MsgComposerIcon(Icons.Default.MenuBook, "Share an ayah or dua")
            MsgComposerIcon(Icons.Default.AttachFile, "Attach")
            MsgComposerIcon(Icons.Default.PhotoCamera, "Camera")
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(50.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF0E7A55), MsgGreen)))
                .border(1.dp, StitchGold.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(if (draft.isBlank()) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send, if (draft.isBlank()) "Record voice" else "Send", tint = StitchGold) }
    }
}

@Composable
private fun MsgComposerIcon(icon: ImageVector, desc: String) =
    Box(Modifier.size(38.dp).clip(CircleShape).clickable {}, contentAlignment = Alignment.Center) { Icon(icon, desc, tint = msgGreen().copy(alpha = 0.8f), modifier = Modifier.size(21.dp)) }

// ============================================ 1:1 chat ======================================

data class DirectChatState(
    val name: String, val handle: String = "", val photoUrl: String = "", val online: Boolean = false, val typing: Boolean = false,
    val lines: List<MsgLine> = emptyList(), val draft: String = "", val salahHint: String = "",
)

@Composable
fun NoorDirectChat(state: DirectChatState, onOpenShared: (MsgShared) -> Unit = {}) {
    val lastIncoming = state.lines.lastOrNull { !it.mine && it.type == MsgLineType.TEXT }?.text.orEmpty()
    Column(Modifier.fillMaxSize().background(msgBg()).testTag("msg_direct_chat")) {
        MsgChatHeader(
            state.name, if (state.typing) "typing…" else if (state.online) "online · @${state.handle}" else "@${state.handle}", state.online || state.typing,
            avatar = { MsgPersonAvatar(state.name, state.photoUrl, 42.dp, online = state.online) }
        ) {
            MsgRoundButton(Icons.Default.Call, "Voice call"); Spacer(Modifier.width(8.dp)); MsgRoundButton(Icons.Default.Videocam, "Video call")
        }
        Column(Modifier.weight(1f).fillMaxWidth().msgWallpaper(isStitchLight()).verticalScroll(rememberScrollState(), reverseScrolling = true).padding(vertical = 6.dp)) {
            Row(
                Modifier.padding(horizontal = 40.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(msgCard().copy(alpha = 0.9f))
                    .border(1.dp, msgHair(), RoundedCornerShape(14.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, null, tint = msgGold(), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                Text("Be kind — say salam. You can report or block from the menu at any time.", color = stitchMutedText(), fontSize = 11.5.sp, lineHeight = 15.sp)
            }
            state.lines.forEach { MsgLineView(it, group = false, onOpenShared) }
            if (state.salahHint.isNotBlank()) Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(MsgGreen).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mosque, null, tint = StitchGold, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(6.dp))
                    Text(state.salahHint, color = MsgCream, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        MsgQuickReplies(IslamicTouches.quickReplies(lastIncoming))
        MsgComposer(state.draft)
    }
}

// ============================================ group chat ====================================

data class GroupChatState(val group: NoorGroup, val lines: List<MsgLine>, val draft: String = "", val typingNames: List<String> = emptyList())

@Composable
fun NoorGroupChat(state: GroupChatState, onOpenShared: (MsgShared) -> Unit = {}) {
    val g = state.group
    Column(Modifier.fillMaxSize().background(msgBg()).testTag("msg_group_chat")) {
        MsgChatHeader(
            g.name, if (state.typingNames.isNotEmpty()) "${state.typingNames.joinToString()} typing…" else "${formatCount(g.memberCount)} members · ${g.onlineCount} online",
            state.typingNames.isNotEmpty(), avatar = { MsgGroupAvatar(g, 42.dp) }
        ) { MsgRoundButton(Icons.Default.Call, "Group call"); Spacer(Modifier.width(8.dp)); MsgRoundButton(Icons.Default.Info, "Circle info") }
        if (g.pinnedNote.isNotBlank()) Row(
            Modifier.fillMaxWidth().background(msgCard()).padding(horizontal = 14.dp, vertical = 9.dp).testTag("msg_pinned_note"), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(3.dp).height(30.dp).clip(RoundedCornerShape(2.dp)).background(StitchGold)); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Pinned by Amina", color = msgGold(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(g.pinnedNote, color = stitchText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.PushPin, null, tint = msgGold(), modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth().msgWallpaper(isStitchLight()).verticalScroll(rememberScrollState(), reverseScrolling = true).padding(vertical = 6.dp)) {
            state.lines.forEach { MsgLineView(it, group = true, onOpenShared) }
        }
        MsgQuickReplies(listOf("Ameen", "JazakAllahu khayran", "In sha Allah", "SubhanAllah"))
        MsgComposer(state.draft, "Message the circle…")
    }
}

// ============================================ group info ====================================

data class GroupInfoState(val group: NoorGroup, val members: List<MsgMember>, val me: String = MessagingSamples.ME, val joinRequests: Int = 0, val inviteCode: String = "")

@Composable
fun NoorGroupInfo(state: GroupInfoState) {
    val g = state.group
    val admin = GroupPermissions.isAdmin(g, state.me)
    Column(Modifier.fillMaxSize().background(msgBg()).verticalScroll(rememberScrollState()).testTag("msg_group_info")) {
        MsgTopBar("Circle info") { if (GroupPermissions.canEditInfo(g, state.me)) MsgRoundButton(Icons.Default.Edit, "Edit circle") }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(104.dp).clip(CircleShape).border(2.dp, StitchGold, CircleShape).padding(5.dp)) {
                MsgImage(g.photoUrl, Modifier.fillMaxSize().clip(CircleShape).background(MsgHeroBrush)) { Icon(topicIcon(g.topicId), null, tint = StitchGold, modifier = Modifier.size(42.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            MsgTitle(g.name, 26, Modifier)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MsgTopicTag(g.topic); Spacer(Modifier.width(8.dp))
                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(msgCard()).border(1.dp, msgHair(), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(visibilityIcon(g.visibility), null, tint = msgGold(), modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp))
                    Text(g.visibility.label, color = stitchText(), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                Text("${formatCount(g.memberCount)} members", color = stitchMutedText(), fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(g.description, color = stitchMutedText(), fontSize = 13.5.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MsgInfoAction(Icons.Default.NotificationsOff, "Mute"); MsgInfoAction(Icons.Default.PersonAdd, "Invite")
                MsgInfoAction(Icons.Default.Search, "Search"); MsgInfoAction(Icons.Default.PermMedia, "Media")
            }
        }
        Spacer(Modifier.height(12.dp))
        if (state.inviteCode.isNotBlank()) MsgCard(Modifier.testTag("msg_invite_card")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(msgMint()), contentAlignment = Alignment.Center) { Icon(Icons.Default.Link, null, tint = msgGreen()) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Invite link", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Text("noorpro.app/c/${state.inviteCode}", color = msgGreen(), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(state.inviteCode, color = StitchEmeraldDeep, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(StitchGold).padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MsgJoinButton("Share"); MsgJoinButton("QR code"); MsgJoinButton("Reset") }
        }
        if (admin) {
            MsgSectionLabel("Admin tools")
            MsgCard(Modifier.testTag("msg_admin_tools")) {
                MsgSettingRow(Icons.Default.HowToReg, "Join requests", "Approve brothers & sisters waiting", badge = state.joinRequests)
                MsgSettingRow(Icons.Default.Edit, "Edit name, photo & topic", "Owner and admins")
                MsgSettingRow(Icons.Default.Forum, "Who can message", "Everyone · or admins only for announcements")
                MsgSettingRow(Icons.Default.Shield, "Moderation", "Remove messages, mute or remove members")
            }
        }
        MsgSectionLabel("Members", trailing = "Add")
        MsgCard {
            state.members.forEach { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp).testTag("msg_member_${m.uid}"), verticalAlignment = Alignment.CenterVertically) {
                    MsgPersonAvatar(m.name, m.photoUrl, 42.dp, online = m.online)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (m.uid == state.me) "You" else m.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text("@${m.handle}", color = stitchMutedText(), fontSize = 12.sp)
                    }
                    if (m.role != GroupRole.MEMBER) MsgRoleChip(m.role)
                    if (GroupPermissions.canRemove(g, state.me, m.uid)) Icon(Icons.Default.MoreVert, "Manage ${m.name}", tint = stitchMutedText(), modifier = Modifier.padding(start = 6.dp).size(20.dp))
                }
            }
            Text("See all ${formatCount(g.memberCount)}", color = msgGreen(), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        MsgCard {
            MsgSettingRow(Icons.Default.Flag, "Report circle", "Sent to NoorPro moderators", danger = true)
            if (GroupPermissions.canLeave(g, state.me)) MsgSettingRow(Icons.AutoMirrored.Filled.ExitToApp, "Leave circle", "", danger = true)
            else MsgSettingRow(Icons.AutoMirrored.Filled.ExitToApp, "Transfer ownership & leave", "Pick a new owner first", danger = true)
        }
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
private fun MsgInfoAction(icon: ImageVector, label: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(msgCard()).border(1.dp, msgHair(), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = msgGreen())
    }
    Spacer(Modifier.height(5.dp))
    Text(label, color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
internal fun MsgCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(msgCard())
        .border(1.dp, msgHair(), RoundedCornerShape(22.dp)).padding(14.dp), content = content)

@Composable
private fun MsgSettingRow(icon: ImageVector, title: String, sub: String, badge: Int = 0, danger: Boolean = false) {
    val red = Color(0xFFC0392B)
    Row(Modifier.fillMaxWidth().clickable {}.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (danger) red.copy(alpha = 0.1f) else msgMint()), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (danger) red else msgGreen(), modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) red else stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            if (sub.isNotBlank()) Text(sub, color = stitchMutedText(), fontSize = 12.sp)
        }
        if (badge > 0) MsgBadge(badge)
        if (!danger) Icon(Icons.Default.ChevronRight, null, tint = stitchMutedText())
    }
}

// ============================================ create group ==================================

data class CreateGroupState(
    val name: String = "", val description: String = "", val topicId: String? = null,
    val visibility: GroupVisibility = GroupVisibility.PUBLIC_REQUEST, val tags: String = "",
    val candidates: List<MsgPerson> = emptyList(), val selected: Set<String> = emptySet(),
)

@Composable
fun NoorCreateGroup(state: CreateGroupState) {
    Box(Modifier.fillMaxSize().background(msgBg()).testTag("msg_create_group")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MsgTopBar("New circle", "Gather for Quran, ilm and good")
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(msgMint()).border(2.dp, StitchGold.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AddAPhoto, "Circle photo", tint = msgGreen(), modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    MsgField("Circle name", state.name, "e.g. Fajr Quran Circle")
                    Spacer(Modifier.height(8.dp))
                    MsgField("About", state.description, "What will you do together?")
                }
            }
            MsgSectionLabel("Topic", Modifier.padding(top = 10.dp))
            MsgTopicGrid(GroupTopics.all.take(6), state.topicId) {}
            MsgSectionLabel("Who can join", Modifier.padding(top = 10.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupVisibility.values().forEach { v ->
                    val on = v == state.visibility
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (on) msgMint() else msgCard())
                            .border(if (on) 1.5.dp else 1.dp, if (on) msgGreen() else msgHair(), RoundedCornerShape(18.dp)).padding(12.dp).testTag("msg_vis_${v.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(visibilityIcon(v), null, tint = if (on) msgGreen() else msgGold(), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.label, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text(v.blurb, color = stitchMutedText(), fontSize = 12.sp)
                        }
                        Icon(if (on) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, null, tint = if (on) msgGreen() else stitchMutedText())
                    }
                }
            }
            MsgSectionLabel("Add members", Modifier.padding(top = 10.dp), trailing = "${state.selected.size} selected")
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.candidates.filter { it.uid in state.selected }.forEach { p ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(62.dp)) {
                        Box {
                            UmmahAvatar(p.name, p.photoUrl, 52.dp, ring = StitchGold)
                            Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(stitchMutedText()), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                        Text(p.name.substringBefore(' '), color = stitchText(), fontSize = 11.5.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            state.candidates.filter { it.uid !in state.selected }.take(3).forEach { p ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    UmmahAvatar(p.name, p.photoUrl, 42.dp); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text(p.reason, color = stitchMutedText(), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.RadioButtonUnchecked, null, tint = stitchMutedText())
                }
            }
            Spacer(Modifier.height(110.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, msgBg(), msgBg()))).navigationBarsPadding().padding(16.dp)) {
            MsgPrimaryButton("Create circle · ${state.selected.size + 1} members", Modifier.fillMaxWidth().testTag("msg_create_button"), Icons.Default.Check)
        }
    }
}

@Composable
private fun MsgField(label: String, value: String, placeholder: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(msgCard()).border(1.dp, msgHair(), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(label, color = msgGold(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Box {
            if (value.isEmpty()) Text(placeholder, color = stitchMutedText(), fontSize = 14.5.sp)
            BasicTextField(value, {}, textStyle = TextStyle(color = stitchText(), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold), cursorBrush = SolidColor(msgGreen()), modifier = Modifier.fillMaxWidth())
        }
    }
}
