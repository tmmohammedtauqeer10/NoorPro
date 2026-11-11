package com.noorpro.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.messaging.*
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon

// ============================================ Messages home =================================

enum class MsgHomeTab(val label: String) { CHATS("Chats"), GROUPS("Groups"), DISCOVER("Discover") }

data class MessagesHomeState(
    val tab: MsgHomeTab = MsgHomeTab.CHATS,
    val myName: String = "Tauqeer",
    val chats: List<MsgChat> = emptyList(),
    val groups: List<NoorGroup> = emptyList(),
    val discover: List<NoorGroup> = emptyList(),
    val moment: IslamicTouches.Moment = IslamicTouches.Moment.NONE,
    val salahHint: String = "",
    val fabOpen: Boolean = false,
    val now: Long = System.currentTimeMillis(),
)

@Composable
fun NoorMessagesHome(
    state: MessagesHomeState,
    onTab: (MsgHomeTab) -> Unit = {},
    onOpenChat: (MsgChat) -> Unit = {},
    onOpenGroup: (NoorGroup) -> Unit = {},
    onSearch: () -> Unit = {},
    onFab: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onNewGroup: () -> Unit = {},
    onJoinWithCode: () -> Unit = {},
    onDiscoverTopic: (String) -> Unit = {},
) {
    Box(Modifier.fillMaxSize().background(msgBg()).testTag("msg_home")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // masthead
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                NoorBrandMark(40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    MsgTitle("Messages", 30)
                    Text("Assalamu alaikum, ${state.myName}", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                MsgRoundButton(Icons.Default.QrCode2, "My QR")
                Spacer(Modifier.width(8.dp))
                MsgRoundButton(Icons.Default.Edit, "New message", onClick = onNewChat)
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.padding(horizontal = 16.dp).clickable(onClick = onSearch).testTag("msg_search_entry")) {
                MsgSearchPill("", "Search people, circles and chats")
            }
            if (state.moment != IslamicTouches.Moment.NONE) {
                Spacer(Modifier.height(12.dp)); MsgMomentBanner(state.moment, state.salahHint)
            }
            Spacer(Modifier.height(14.dp))
            MsgPillTabs(
                MsgHomeTab.values().map { it.label }, state.tab.ordinal, Modifier.padding(horizontal = 16.dp),
                badges = mapOf(0 to state.chats.count { it.unread > 0 }, 1 to state.groups.count { it.unread > 0 && !it.muted })
            ) { onTab(MsgHomeTab.values()[it]) }
            Spacer(Modifier.height(10.dp))
            when (state.tab) {
                MsgHomeTab.CHATS -> {
                    val pinned = state.chats.filter { it.pinned }
                    if (pinned.isNotEmpty()) { MsgSectionLabel("Pinned"); pinned.forEach { MsgChatRow(it, state.now) { onOpenChat(it) } } }
                    MsgSectionLabel("All chats", trailing = "${state.chats.size}")
                    state.chats.filter { !it.pinned }.forEach { MsgChatRow(it, state.now) { onOpenChat(it) } }
                }
                MsgHomeTab.GROUPS -> {
                    MsgCreateCircleCard(onNewGroup)
                    MsgSectionLabel("Your circles", trailing = "${state.groups.size}")
                    state.groups.forEach { MsgGroupRow(it, state.now) { onOpenGroup(it) } }
                }
                MsgHomeTab.DISCOVER -> {
                    MsgSectionLabel("Browse by topic")
                    MsgTopicGrid(GroupTopics.all.take(8), null, onDiscoverTopic)
                    MsgSectionLabel("Popular circles", trailing = "See all")
                    GroupDiscovery.browse(state.discover, null).take(4).forEach { g ->
                        MsgDiscoverCard(g, GroupPermissions.joinAction(g, MessagingSamples.ME).label) { onOpenGroup(g) }
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
        }
        if (state.fabOpen) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isStitchLight()) 0.28f else 0.5f)).clickable(onClick = onFab))
        MsgFab(state.fabOpen, onFab, onNewChat, onNewGroup, onJoinWithCode, Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun MsgMomentBanner(moment: IslamicTouches.Moment, salahHint: String) {
    val (kicker, title, body) = when (moment) {
        IslamicTouches.Moment.RAMADAN -> Triple("RAMADAN KAREEM", "Share an iftar dua", "Send a Ramadan greeting to your circles")
        else -> Triple("JUMU'AH MUBARAK", "Send salawat & salam", "Remind your circles: Surah Al-Kahf today")
    }
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MsgHeroBrush)
            .border(1.dp, StitchGold.copy(alpha = 0.45f), RoundedCornerShape(24.dp)).padding(16.dp).testTag("msg_moment")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(kicker, color = StitchGold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text(title, color = MsgCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(body, color = MsgCream.copy(alpha = 0.78f), fontSize = 12.5.sp)
                if (salahHint.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = StitchGold, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(salahHint, color = MsgCream, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).border(1.dp, StitchGold.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (moment == IslamicTouches.Moment.RAMADAN) Icons.Default.Brightness3 else Icons.Default.Mosque, null, tint = StitchGold, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
internal fun MsgChatRow(c: MsgChat, now: Long, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp).testTag("msg_chat_${c.id}"), verticalAlignment = Alignment.CenterVertically) {
        MsgPersonAvatar(c.name, c.photoUrl, 54.dp, online = c.online, ring = c.unread > 0)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, color = stitchText(), fontWeight = if (c.unread > 0) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                if (c.pinned) { Spacer(Modifier.width(5.dp)); Icon(Icons.Default.PushPin, "Pinned", tint = msgGold(), modifier = Modifier.size(13.dp)) }
                Spacer(Modifier.weight(1f))
                Text(msgTime(c.updatedAt, now), color = if (c.unread > 0) msgGreen() else stitchMutedText(), fontSize = 11.5.sp, fontWeight = if (c.unread > 0) FontWeight.Bold else FontWeight.Medium)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c.typing) {
                    Text("typing…", color = msgGreen(), fontSize = 13.5.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                } else {
                    if (c.lastMine) { MsgTicks(c.tick); Spacer(Modifier.width(4.dp)) }
                    if (c.voice) { Icon(Icons.Default.Mic, null, tint = msgGreen(), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)) }
                    Text(c.lastMessage, color = if (c.unread > 0) stitchText() else stitchMutedText(), fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (c.unread > 0) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                }
                if (c.unread > 0) { Spacer(Modifier.width(8.dp)); MsgBadge(c.unread) }
            }
        }
    }
}

/** NoorPro ticks: gold when read (not blue), muted when sent/delivered. */
@Composable
internal fun MsgTicks(tick: Int, onGreen: Boolean = false) {
    val icon = when (tick) { 0 -> Icons.Default.Schedule; 1 -> Icons.Default.Done; else -> Icons.Default.DoneAll }
    val tint = when {
        tick >= 3 -> if (onGreen) StitchGold else msgGold()
        onGreen -> MsgCream.copy(alpha = 0.6f)
        else -> stitchMutedText()
    }
    Icon(icon, when (tick) { 0 -> "Sending"; 1 -> "Sent"; 2 -> "Delivered"; else -> "Read" }, tint = tint, modifier = Modifier.size(15.dp))
}

@Composable
internal fun MsgGroupRow(g: NoorGroup, now: Long, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp).testTag("msg_group_${g.id}"), verticalAlignment = Alignment.CenterVertically) {
        MsgGroupAvatar(g, 54.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.name, color = stitchText(), fontWeight = if (g.unread > 0) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                if (g.visibility == GroupVisibility.PRIVATE) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Lock, "Private", tint = stitchMutedText(), modifier = Modifier.size(13.dp)) }
                Spacer(Modifier.weight(1f))
                Text(msgTime(g.updatedAt, now), color = if (g.unread > 0 && !g.muted) msgGreen() else stitchMutedText(), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(buildString { if (g.lastSenderName.isNotBlank()) append(g.lastSenderName).append(": "); append(g.lastMessage) },
                    color = if (g.unread > 0) stitchText() else stitchMutedText(), fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (g.muted) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.NotificationsOff, "Muted", tint = stitchMutedText(), modifier = Modifier.size(15.dp)) }
                if (g.unread > 0) { Spacer(Modifier.width(6.dp)); MsgBadge(g.unread, muted = g.muted) }
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MsgTopicTag(g.topic)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Groups, null, tint = stitchMutedText(), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text("${formatCount(g.memberCount)} members", color = stitchMutedText(), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MsgCreateCircleCard(onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(msgCard())
            .border(1.dp, msgHair(), RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MsgHeroBrush), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.GroupAdd, null, tint = StitchGold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Start a circle", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Quran halaqah, family, masjid or study group", color = stitchMutedText(), fontSize = 12.5.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = msgGreen())
    }
}

/** Two-column topic tiles (explicit rows — no FlowRow, see SurahLearningScreen note). */
@Composable
internal fun MsgTopicGrid(topics: List<GroupTopic>, selected: String?, onTopic: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        topics.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    val on = t.id == selected
                    Row(
                        Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(18.dp)).background(if (on) MsgGreen else msgCard())
                            .border(1.dp, if (on) StitchGold.copy(alpha = 0.6f) else msgHair(), RoundedCornerShape(18.dp))
                            .clickable { onTopic(t.id) }.padding(horizontal = 12.dp).testTag("msg_topic_${t.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(if (on) Color.White.copy(alpha = 0.12f) else msgMint()), contentAlignment = Alignment.Center) {
                            Icon(topicIcon(t.id), null, tint = if (on) StitchGold else msgGreen(), modifier = Modifier.size(17.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(t.label, color = if (on) MsgCream else stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun MsgDiscoverCard(g: NoorGroup, joinLabel: String, onClick: () -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(msgCard())
            .border(1.dp, msgHair(), RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(14.dp).testTag("msg_discover_${g.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MsgGroupAvatar(g, 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MsgTopicTag(g.topic); Spacer(Modifier.width(6.dp))
                    Icon(visibilityIcon(g.visibility), null, tint = stitchMutedText(), modifier = Modifier.size(12.dp)); Spacer(Modifier.width(3.dp))
                    Text("${formatCount(g.memberCount)}", color = stitchMutedText(), fontSize = 11.sp)
                }
            }
            MsgJoinButton(joinLabel)
        }
        if (g.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(g.description, color = stitchMutedText(), fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun MsgFab(open: Boolean, onFab: () -> Unit, onNewChat: () -> Unit, onNewGroup: () -> Unit, onCode: () -> Unit, modifier: Modifier) {
    Column(modifier.navigationBarsPadding().padding(end = 18.dp, bottom = 22.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (open) {
            Column(
                Modifier.width(230.dp).shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(msgCard())
                    .border(1.dp, msgHair(), RoundedCornerShape(22.dp)).padding(vertical = 6.dp).testTag("msg_fab_menu")
            ) {
                MsgFabItem(Icons.Default.PersonAdd, "New chat", "Message a brother or sister", onNewChat)
                MsgFabItem(Icons.Default.GroupAdd, "New circle", "Create a group", onNewGroup)
                MsgFabItem(Icons.Default.QrCodeScanner, "Join with code", "Invite link or QR", onCode)
            }
        }
        Row(
            Modifier.height(56.dp).shadow(8.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0B6243), StitchEmerald))).border(1.dp, StitchGold.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .clickable(onClick = onFab).padding(horizontal = 18.dp).testTag("msg_fab"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (open) Icons.Default.Close else Icons.Default.Add, null, tint = StitchGold)
            Spacer(Modifier.width(8.dp))
            Text(if (open) "Close" else "New", color = MsgCream, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun MsgFabItem(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(msgMint()), contentAlignment = Alignment.Center) { Icon(icon, null, tint = msgGreen(), modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(sub, color = stitchMutedText(), fontSize = 11.5.sp)
        }
    }
}

// ============================================ Discover ======================================

data class DiscoverState(val groups: List<NoorGroup>, val topicId: String? = null, val query: String = "", val requested: Set<String> = emptySet())

@Composable
fun NoorDiscoverGroups(state: DiscoverState, onBack: () -> Unit = {}, onTopic: (String?) -> Unit = {}, onQuery: (String) -> Unit = {}, onOpen: (NoorGroup) -> Unit = {}) {
    val list = GroupDiscovery.browse(state.groups, state.topicId, state.query)
    val featured = list.firstOrNull()
    Column(Modifier.fillMaxSize().background(msgBg()).verticalScroll(rememberScrollState()).testTag("msg_discover")) {
        MsgTopBar("Discover circles", "Public Islamic groups near and far", onBack) { MsgRoundButton(Icons.Default.QrCodeScanner, "Join with code") }
        MsgSearchPill(state.query, "Search circles, topics or #tags", Modifier.padding(horizontal = 16.dp), onQuery)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MsgChip("All", state.topicId == null, Icons.Default.AutoAwesome) { onTopic(null) }
            GroupTopics.all.forEach { t -> MsgChip(t.label, t.id == state.topicId, topicIcon(t.id), Modifier.testTag("msg_chip_${t.id}")) { onTopic(t.id) } }
        }
        state.topicId?.let { GroupTopics.byId(it) }?.let { t ->
            Text(t.blurb, color = stitchMutedText(), fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        if (featured != null) {
            MsgSectionLabel("Featured circle")
            MsgFeaturedCircle(featured, GroupPermissions.joinAction(featured, MessagingSamples.ME, featured.id in state.requested).label) { onOpen(featured) }
        }
        MsgSectionLabel(state.topicId?.let { "Popular in ${GroupTopics.byId(it)?.label}" } ?: "Popular circles", trailing = "${list.size}")
        if (list.size <= 1) Text("No other public circles here yet — start one!", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.padding(20.dp))
        list.drop(1).forEach { g -> MsgDiscoverCard(g, GroupPermissions.joinAction(g, MessagingSamples.ME, g.id in state.requested).label) { onOpen(g) } }
        Text("Private circles never appear here. Report anything that breaks the Ummah guidelines.", color = stitchMutedText(), fontSize = 11.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
private fun MsgFeaturedCircle(g: NoorGroup, joinLabel: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().height(190.dp).clip(RoundedCornerShape(26.dp)).background(MsgHeroBrush)
            .border(1.dp, StitchGold.copy(alpha = 0.5f), RoundedCornerShape(26.dp)).clickable(onClick = onClick).testTag("msg_featured")
    ) {
        MsgImage(g.photoUrl, Modifier.fillMaxSize()) {}
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33033D2E), Color(0xEE033D2E)))))
        Row(Modifier.align(Alignment.TopStart).padding(14.dp).clip(RoundedCornerShape(999.dp)).background(StitchGold).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = StitchEmeraldDeep, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp))
            Text("This week", color = StitchEmeraldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(g.topic?.label?.uppercase() ?: "", color = StitchGold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Text(g.name, color = MsgCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${formatCount(g.memberCount)} members · ${g.visibility.label}", color = MsgCream.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                Box(Modifier.height(34.dp).clip(RoundedCornerShape(999.dp)).background(StitchGold).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(joinLabel, color = StitchEmeraldDeep, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ============================================ Find people / unified search ==================

enum class FindFilter(val label: String) { ALL("All"), PEOPLE("People"), GROUPS("Circles"), CHATS("Chats") }

data class FindState(
    val query: String = "",
    val filter: FindFilter = FindFilter.ALL,
    val chats: List<MsgChat> = emptyList(),
    val people: List<MsgPerson> = emptyList(),
    val groups: List<NoorGroup> = emptyList(),
    val myHandle: String = "tauqeer",
)

@Composable
fun NoorFindPeople(state: FindState, onBack: () -> Unit = {}, onQuery: (String) -> Unit = {}, onFilter: (FindFilter) -> Unit = {}, onHit: (UnifiedSearch.Hit) -> Unit = {}) {
    val results = remember(state) { UnifiedSearch.search(state.query, state.chats, state.people, state.groups, MessagingSamples.ME) }
    Column(Modifier.fillMaxSize().background(msgBg()).verticalScroll(rememberScrollState()).testTag("msg_find")) {
        MsgTopBar("Find", "People, circles and chats in one place", onBack)
        MsgSearchPill(state.query, "Name, @username or circle", Modifier.padding(horizontal = 16.dp).testTag("msg_find_query"), onQuery) {
            if (state.query.isNotEmpty()) Box(Modifier.size(34.dp).clip(CircleShape).clickable { onQuery("") }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, "Clear", tint = stitchMutedText(), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FindFilter.values().forEach { f -> MsgChip(f.label, f == state.filter) { onFilter(f) } }
        }
        Spacer(Modifier.height(6.dp))
        if (state.query.isBlank()) {
            MsgAddFriendCard(state.myHandle)
            MsgSectionLabel("Suggested for you")
            UnifiedSearch.suggested(state.people, state.chats.map { it.otherUid }.toSet(), MessagingSamples.ME, 5).forEach { p ->
                MsgPersonResult(p.name, "@${p.handle} · ${p.reason}", p.photoUrl, if (p.following) "Message" else "Follow", Modifier.testTag("msg_suggest_${p.uid}"))
            }
        } else {
            val f = state.filter
            if (results.best.isNotEmpty() && f == FindFilter.ALL) {
                MsgSectionLabel("Best match")
                results.best.forEach { MsgBestMatch(it, state) { onHit(it) } }
            }
            if (f == FindFilter.ALL || f == FindFilter.PEOPLE) results.people.takeIf { it.isNotEmpty() }?.let { hits ->
                MsgSectionLabel("People", trailing = "${hits.size}")
                hits.take(if (f == FindFilter.ALL) 4 else 50).forEach { h ->
                    val p = state.people.firstOrNull { it.uid == h.id }
                    MsgPersonResult(h.title, h.subtitle, h.photoUrl, if (p?.following == true) "Message" else "Follow", Modifier.testTag("msg_hit_person_${h.id}")) { onHit(h) }
                }
            }
            if (f == FindFilter.ALL || f == FindFilter.GROUPS) results.groups.takeIf { it.isNotEmpty() }?.let { hits ->
                MsgSectionLabel("Circles", trailing = "${hits.size}")
                hits.forEach { h -> state.groups.firstOrNull { it.id == h.id }?.let { g -> MsgGroupResult(g) { onHit(h) } } }
            }
            if (f == FindFilter.ALL || f == FindFilter.CHATS) results.chats.takeIf { it.isNotEmpty() }?.let { hits ->
                MsgSectionLabel("Recent chats", trailing = "${hits.size}")
                hits.forEach { h -> MsgPersonResult(h.title, h.subtitle, h.photoUrl, "Open", Modifier.testTag("msg_hit_chat_${h.id}")) { onHit(h) } }
            }
            if (results.isEmpty) Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PersonSearch, null, tint = msgGold(), modifier = Modifier.size(40.dp))
                Text("No one found for “${state.query}”", color = stitchText(), fontWeight = FontWeight.Bold)
                Text("Try their exact @username or scan their NoorPro QR", color = stitchMutedText(), fontSize = 12.5.sp)
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
private fun MsgBestMatch(h: UnifiedSearch.Hit, state: FindState, onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MsgHeroBrush)
            .border(1.dp, StitchGold.copy(alpha = 0.55f), RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(14.dp).testTag("msg_best_${h.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val g = state.groups.firstOrNull { it.id == h.id }
        if (h.kind == UnifiedSearch.Kind.GROUP && g != null) MsgGroupAvatar(g, 56.dp) else UmmahAvatar(h.title, h.photoUrl, 56.dp, ring = StitchGold)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(h.title, color = MsgCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                Spacer(Modifier.width(6.dp))
                Text("EXACT", color = StitchEmeraldDeep, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(StitchGold).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Text(h.subtitle, color = MsgCream.copy(alpha = 0.78f), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.height(34.dp).clip(RoundedCornerShape(999.dp)).background(StitchGold).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(if (h.kind == UnifiedSearch.Kind.GROUP) "View" else "Message", color = StitchEmeraldDeep, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MsgPersonResult(title: String, subtitle: String, photo: String, action: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        UmmahAvatar(title, photo, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = stitchMutedText(), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MsgJoinButton(action)
    }
}

@Composable
private fun MsgGroupResult(g: NoorGroup, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp).testTag("msg_hit_group_${g.id}"), verticalAlignment = Alignment.CenterVertically) {
        MsgGroupAvatar(g, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(g.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                MsgTopicTag(g.topic); Spacer(Modifier.width(6.dp))
                Text("${formatCount(g.memberCount)} members", color = stitchMutedText(), fontSize = 11.5.sp)
            }
        }
        MsgJoinButton(GroupPermissions.joinAction(g, MessagingSamples.ME).label)
    }
}

@Composable
private fun MsgAddFriendCard(handle: String) {
    val qr = remember(handle) { msgQrBitmap("https://noorpro.app/u/$handle", 360) }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(msgCard())
            .border(1.dp, msgHair(), RoundedCornerShape(24.dp)).padding(14.dp).testTag("msg_add_friend"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)).background(MsgCream).border(2.dp, StitchGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(6.dp), contentAlignment = Alignment.Center) {
            if (qr != null) Image(qr.asImageBitmap(), "My NoorPro QR", modifier = Modifier.fillMaxSize())
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(MsgCream).padding(2.dp)) { NoorBrandMark(20.dp) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Add by @username or QR", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("@$handle", color = msgGreen(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Exact usernames always come first.", color = stitchMutedText(), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MsgJoinButton("Scan")
                MsgJoinButton("Share")
            }
        }
    }
}
