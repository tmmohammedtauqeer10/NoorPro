package com.noorpro.app.ui.islamic

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.noorpro.app.data.halal.HalalTextFilter
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.community.CommunityException
import com.noorpro.app.islamic.community.CommunityReports
import com.noorpro.app.islamic.social.*
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

/* 1.1.6 Halaqa rooms UI (halaqa_rooms_enabled): home, create/schedule, live room. */

data class HalaqaHomeUi(
    val rooms: List<HalaqaRoom> = emptyList(),
    val reminders: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: CommunityError? = null,
    val now: Long = System.currentTimeMillis(),
)

class HalaqaHomeActions(
    val onBack: () -> Unit = {},
    val onCreate: () -> Unit = {},
    val onOpen: (HalaqaRoom) -> Unit = {},
    val onRemind: (HalaqaRoom, Boolean) -> Unit = { _, _ -> },
)

data class HalaqaRoomUi(
    val room: HalaqaRoom,
    val members: List<HalaqaMember> = emptyList(),
    val myUid: String? = null,
    val link: HalaqaLink = HalaqaLink.IDLE,
    val reminded: Boolean = false,
    val speakerOn: Boolean = true,
    val joining: Boolean = false,
    val error: CommunityError? = null,
    val now: Long = System.currentTimeMillis(),
) {
    val me get() = members.firstOrNull { it.uid == myUid }
    val isHost get() = myUid != null && myUid == room.hostUid
}

class HalaqaRoomActions(
    val onBack: () -> Unit = {},
    val onMic: (Boolean) -> Unit = {},
    val onHand: (Boolean) -> Unit = {},
    val onSpeaker: (Boolean) -> Unit = {},
    val onInvite: (HalaqaMember) -> Unit = {},
    val onMoveDown: (HalaqaMember) -> Unit = {},
    val onMute: (HalaqaMember) -> Unit = {},
    val onRemove: (HalaqaMember) -> Unit = {},
    val onGoLive: () -> Unit = {},
    val onEnd: () -> Unit = {},
    val onLeave: () -> Unit = {},
    val onRemind: (Boolean) -> Unit = {},
    val onReport: () -> Unit = {},
)

@Composable
fun HalaqaScreen(viewModel: DeenViewModel, backend: HalaqaBackend = remember { FirestoreHalaqaBackend() }) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val reminders = remember { HalaqaReminders(ctx) }
    var home by remember { mutableStateOf(HalaqaHomeUi(reminders = reminders.ids())) }
    var creating by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<HalaqaRoom?>(null) }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun refresh() {
        if (!backend.available) { home = home.copy(error = CommunityError.OFFLINE_BACKEND); return }
        home = home.copy(loading = true)
        scope.launch {
            runCatching { backend.rooms() }
                .onSuccess { home = home.copy(rooms = it, loading = false, error = null, now = System.currentTimeMillis()) }
                .onFailure { home = home.copy(loading = false, error = CommunityError.from(it)) }
        }
    }
    LaunchedEffect(Unit) { HalalTextFilter.ensureLoaded(ctx); refresh() }

    open?.let { r ->
        HalaqaRoomScreen(r, backend, viewModel.userDisplayName, reminders, onClose = { open = null; refresh() })
        return
    }
    if (creating) {
        BackHandler { creating = false }
        HalaqaCreateContent(onBack = { creating = false }) { title, topic, desc, startAt, done ->
            if (backend.uid() == null) { done(CommunityError.SIGN_IN.body); return@HalaqaCreateContent }
            scope.launch {
                runCatching { backend.create(title, topic, desc, viewModel.userDisplayName, startAt) }
                    .onSuccess { r ->
                        done(null); creating = false
                        if (r.status == HalaqaStatus.SCHEDULED) {
                            home = home.copy(rooms = listOf(r) + home.rooms, reminders = reminders.set(r, true))
                            toast("Scheduled. We'll remind you ${HalaqaLogic.REMIND_BEFORE_MIN} minutes before.")
                        } else open = r
                    }
                    .onFailure { e -> done((e as? CommunityException)?.detail?.ifBlank { null } ?: CommunityError.from(e).body) }
            }
        }
        return
    }
    HalaqaHomeContent(home, HalaqaHomeActions(
        onBack = { viewModel.goBack() },
        onCreate = { if (!backend.available) toast(CommunityError.OFFLINE_BACKEND.body) else if (backend.uid() == null) toast(CommunityError.SIGN_IN.body) else creating = true },
        onOpen = { if (backend.uid() == null) toast(CommunityError.SIGN_IN.body) else open = it },
        onRemind = { r, on -> home = home.copy(reminders = reminders.set(r, on)); if (on) toast("We'll remind you ${HalaqaLogic.REMIND_BEFORE_MIN} minutes before it starts.") },
    ))
}

/** Joins, keeps the WebRTC mesh in step with the members list, and leaves on exit. */
@Composable
private fun HalaqaRoomScreen(start: HalaqaRoom, backend: HalaqaBackend, name: String, reminders: HalaqaReminders, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = remember { backend.uid() }
    var ui by remember { mutableStateOf(HalaqaRoomUi(start, myUid = uid, reminded = start.id in reminders.ids(), joining = true)) }
    var rtc by remember { mutableStateOf<HalaqaRtc?>(null) }
    var reporting by remember { mutableStateOf(false) }
    var pendingUnmute by remember { mutableStateOf(false) }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()
    fun act(block: suspend () -> Unit) = scope.launch { runCatching { block() }.onFailure { toast(CommunityError.from(it).body) } }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok && pendingUnmute) act { backend.setMuted(ui.room, uid ?: "", false) }
        else if (!ok) toast("Microphone permission is needed to speak. You can still listen.")
        pendingUnmute = false
    }

    DisposableEffect(start.id) {
        val stopRoom = backend.watchRoom(start) { r -> if (r == null) ui = ui.copy(error = CommunityError.NOT_FOUND) else ui = ui.copy(room = r) }
        val stopMembers = backend.watchMembers(start) { m -> ui = ui.copy(members = m) }
        onDispose {
            stopRoom(); stopMembers()
            rtc?.release(); rtc = null
            if (uid != null) scope.launch { runCatching { backend.leave(start) } }
        }
    }
    LaunchedEffect(start.id) {
        // Scheduled rooms: only the host waits inside; others see the countdown card.
        if (start.status == HalaqaStatus.SCHEDULED && uid != start.hostUid) { ui = ui.copy(joining = false); return@LaunchedEffect }
        runCatching { backend.join(start, name) }.onFailure { ui = ui.copy(error = CommunityError.from(it)) }
        ui = ui.copy(joining = false)
    }
    // Keep the audio mesh in step with roles/mute; tear it down when the room ends.
    var wasIn by remember { mutableStateOf(false) }
    LaunchedEffect(ui.members, ui.room.status) {
        val me = ui.me
        if (me != null) wasIn = true
        when {
            ui.room.status == HalaqaStatus.ENDED -> { rtc?.release(); rtc = null }
            me != null && ui.room.status == HalaqaStatus.LIVE && uid != null ->
                (rtc ?: HalaqaRtc(ctx, start.id, uid) { h -> ui = ui.copy(link = h) }.also { rtc = it }).sync(me, ui.members)
            me == null && wasIn && !ui.joining -> { toast("You've left the halaqa."); onClose() }
        }
    }

    BackHandler { onClose() }
    HalaqaRoomContent(ui, HalaqaRoomActions(
        onBack = onClose,
        onMic = { on ->
            if (on && ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                pendingUnmute = true; micPerm.launch(Manifest.permission.RECORD_AUDIO)
            } else act { backend.setMuted(ui.room, uid ?: "", !on) }
        },
        onHand = { up -> act { backend.setHand(ui.room, up) } },
        onSpeaker = { on -> rtc?.setSpeaker(on); ui = ui.copy(speakerOn = on) },
        onInvite = { m -> if (!HalaqaLogic.canInvite(ui.members)) toast("The stage is full (${HalaqaLogic.MAX_SPEAKERS} voices).") else act { backend.setRole(ui.room, m.uid, HalaqaRole.SPEAKER) } },
        onMoveDown = { m -> act { backend.setRole(ui.room, m.uid, HalaqaRole.LISTENER) } },
        onMute = { m -> act { backend.setMuted(ui.room, m.uid, true) } },
        onRemove = { m -> act { backend.kick(ui.room, m.uid) } },
        onGoLive = { act { backend.goLive(ui.room) } },
        onEnd = { act { backend.end(ui.room) } },
        onLeave = onClose,
        onRemind = { on -> reminders.set(ui.room, on); ui = ui.copy(reminded = on) },
        onReport = { reporting = true },
    ))
    if (reporting) IkReportDialog("halaqa", onDismiss = { reporting = false }) { reason ->
        reporting = false
        CommunityReports.report("halaqa_room", ui.room.id, ui.room.hostUid, reason, ui.room.title) { ok -> toast(if (ok) "Thank you. Our moderators will review it." else "Couldn't send the report. Please try again.") }
    }
}

@Composable
fun HalaqaHomeContent(ui: HalaqaHomeUi, actions: HalaqaHomeActions, modifier: Modifier = Modifier) {
    val (live, soon) = HalaqaLogic.listed(ui.rooms, ui.now)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("halaqa_home")) {
        IkHeader("Halaqa", "Gather to recite, learn and remember", actions.onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("TRANQUILLITY DESCENDS", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Text("“No people gather to recite the Book of Allah and study it together, except that tranquillity descends upon them and mercy covers them.”",
                color = IkCream, fontFamily = LibreCaslon, fontSize = 16.sp, lineHeight = 22.sp, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(6.dp))
            Text("Sahih Muslim 2699", color = IkCream.copy(alpha = 0.7f), fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(onClick = actions.onCreate).padding(horizontal = 16.dp, vertical = 11.dp).testTag("halaqa_create_open"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.RecordVoiceOver, null, tint = IkGreenDeep, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Start or schedule a halaqa", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        ui.error?.let { IkNotice(it.title, if (it == CommunityError.OFFLINE_BACKEND) "Halaqa rooms aren't switched on yet. Please check back soon." else it.body,
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = it == CommunityError.OFFLINE_BACKEND || it == CommunityError.NETWORK, tag = "halaqa_notice") }
        if (ui.error == null || ui.rooms.isNotEmpty()) {
            IkSection("Live now", Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp))
            if (live.isEmpty()) Text(if (ui.loading) "Looking for live halaqas…" else "No one is live right now. Start one, or set a reminder for an upcoming circle.",
                color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).testTag("halaqa_live_empty"))
            live.forEach { HalaqaRoomCard(it, ui, actions) }
            IkSection("Upcoming", Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp))
            if (soon.isEmpty() && !ui.loading) Text("Nothing scheduled yet.", color = stitchMutedText(), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).testTag("halaqa_soon_empty"))
            soon.forEach { HalaqaRoomCard(it, ui, actions) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Halaqas are voice-only and aren't recorded. Speak with adab; hosts can mute or move anyone, and reports go to NoorPro moderators.",
            color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun HalaqaRoomCard(r: HalaqaRoom, ui: HalaqaHomeUi, actions: HalaqaHomeActions) {
    val live = r.status == HalaqaStatus.LIVE
    IkCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).clickable { actions.onOpen(r) }.testTag("halaqa_room_${r.id}"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkIconTile(topicIcon(r.topic), size = 42.dp, tint = if (live) Color.White else ikAccentText(), bg = if (live) IkGreen else ikSoft())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                Text("${r.topic.label} · with ${r.hostName.ifBlank { "a host" }}", color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (r.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(r.description, color = stitchText(), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (live) LiveDot() else Icon(Icons.Outlined.Schedule, null, tint = ikAccentText(), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(HalaqaLogic.whenLine(r, ui.now), color = if (live) IkRed else ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (!live) {
                val on = r.id in ui.reminders
                IkPill(if (on) "Reminder on" else "Remind me", on, icon = if (on) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsNone,
                    tag = "halaqa_remind_${r.id}") { actions.onRemind(r, !on) }
            } else IkPill("Join", true, icon = Icons.Outlined.Headphones, tag = "halaqa_join_${r.id}") { actions.onOpen(r) }
        }
    }
}

@Composable
private fun LiveDot() = Box(Modifier.size(8.dp).clip(CircleShape).background(IkRed))

private fun topicIcon(t: HalaqaTopic): ImageVector = when (t) {
    HalaqaTopic.QURAN -> Icons.Outlined.MenuBook
    HalaqaTopic.DARS -> Icons.Outlined.School
    HalaqaTopic.NAAT -> Icons.Outlined.GraphicEq
    HalaqaTopic.DHIKR -> Icons.Outlined.Spa
    HalaqaTopic.QA -> Icons.Outlined.QuestionAnswer
}

@Composable
fun HalaqaCreateContent(onBack: () -> Unit, modifier: Modifier = Modifier, now: Long = System.currentTimeMillis(), initialTitle: String = "",
                        initialSchedule: Boolean = false, onCreate: (String, HalaqaTopic, String, Long?, (String?) -> Unit) -> Unit) {
    var title by remember { mutableStateOf(initialTitle) }
    var desc by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf(HalaqaTopic.QURAN) }
    var schedule by remember { mutableStateOf(initialSchedule) }
    var day by remember { mutableStateOf(0) }
    var time by remember { mutableStateOf(HALAQA_TIMES[3]) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val startAt = if (schedule) HalaqaLogic.slot(now, day, time.first, time.second) else null
    val past = startAt != null && startAt <= now
    val check = remember(title, desc) { "$title $desc".trim().takeIf { it.isNotEmpty() }?.let { HalalTextFilter.check(it) } }
    val held = check != null && check.action != HalalTextFilter.Action.ALLOW && check.action != HalalTextFilter.Action.MASK
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("halaqa_create")) {
        IkHeader("New halaqa", "Voice-only · up to ${HalaqaLogic.MAX_MEMBERS} people", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            IkSection("Topic")
        }
        Spacer(Modifier.height(6.dp))
        IkPillRow(HalaqaTopic.entries, topic, { it.label }, tag = { "halaqa_topic_${it.name}" }) { topic = it }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(topic.blurb + if (topic == HalaqaTopic.NAAT) " — no instruments, please." else "", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            IkSection("Title", Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            IkField(title, { title = it; error = null }, "e.g. Tajweed circle: Surah Al-Mulk", tag = "halaqa_title", maxLen = 80)
            Spacer(Modifier.height(10.dp))
            IkField(desc, { desc = it }, "What will you cover? (optional)", Modifier.heightIn(min = 90.dp), tag = "halaqa_desc", singleLine = false, maxLen = 280)
            val hint = when {
                held -> "Please remove links, offers or anything against the community guidelines."
                error != null -> error!!
                past -> "That time has already passed today — pick a later slot."
                else -> ""
            }
            if (hint.isNotEmpty()) Text(hint, color = IkAmber, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 6.dp).testTag("halaqa_hint"))
            IkSection("When", Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            IkSegments(listOf("Start now", "Schedule"), if (schedule) 1 else 0, tagPrefix = "halaqa_when") { schedule = it == 1 }
        }
        if (schedule) {
            Spacer(Modifier.height(10.dp))
            IkPillRow((0..6).toList(), day, { dayLabel(now, it) }, tag = { "halaqa_day_$it" }) { day = it }
            Spacer(Modifier.height(8.dp))
            IkPillRow(HALAQA_TIMES, time, { timeLabel(it) }, tag = { "halaqa_time_${it.first}_${it.second}" }) { time = it }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, null, tint = ikAccentText(), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("You'll get a reminder ${HalaqaLogic.REMIND_BEFORE_MIN} minutes before. Others can tap “Remind me”.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        Column(Modifier.padding(16.dp)) {
            IkButton(if (busy) "Please wait…" else if (schedule) "Schedule halaqa" else "Go live now", Modifier.fillMaxWidth(),
                if (schedule) Icons.Outlined.EventAvailable else Icons.Outlined.Sensors,
                enabled = !busy && HalaqaLogic.validateTitle(title) == null && !held && !past, tag = "halaqa_submit") {
                busy = true
                onCreate(title.trim(), topic, desc.trim(), startAt) { err -> busy = false; error = err }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

internal val HALAQA_TIMES = listOf(6 to 0, 13 to 30, 17 to 0, 20 to 0, 21 to 30, 22 to 30)
private fun timeLabel(t: Pair<Int, Int>) = "%d:%02d %s".format(if (t.first % 12 == 0) 12 else t.first % 12, t.second, if (t.first < 12) "AM" else "PM")
private fun dayLabel(now: Long, off: Int) = when (off) {
    0 -> "Today"; 1 -> "Tomorrow"
    else -> java.text.SimpleDateFormat("EEE d", java.util.Locale.ENGLISH).format(now + off * 86_400_000L)
}

@Composable
fun HalaqaRoomContent(ui: HalaqaRoomUi, actions: HalaqaRoomActions, modifier: Modifier = Modifier) {
    val r = ui.room
    val me = ui.me
    var menuFor by remember { mutableStateOf<HalaqaMember?>(null) }
    var confirmEnd by remember { mutableStateOf(false) }
    Column(modifier.ikScreen().testTag("halaqa_room")) {
        IkHeader(r.title, "${r.topic.label} · with ${r.hostName.ifBlank { "a host" }}", actions.onBack) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = actions.onReport).testTag("halaqa_report"), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.OutlinedFlag, "Report", tint = stitchMutedText(), modifier = Modifier.size(19.dp))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when {
                ui.error != null && ui.error != CommunityError.FULL && me == null && r.status != HalaqaStatus.ENDED ->
                    IkNotice(ui.error.title, if (ui.error == CommunityError.OFFLINE_BACKEND) "Halaqa rooms aren't switched on yet." else ui.error.body,
                        Modifier.padding(16.dp), offline = ui.error == CommunityError.NETWORK || ui.error == CommunityError.OFFLINE_BACKEND, tag = "halaqa_room_notice")
                r.status == HalaqaStatus.ENDED -> HalaqaEnded(actions)
                r.status == HalaqaStatus.SCHEDULED -> HalaqaWaiting(ui, actions)
                else -> {
                    ui.error?.takeIf { it == CommunityError.FULL }?.let { IkNotice(it.title, "This halaqa is full (${HalaqaLogic.MAX_MEMBERS} people). Please try again later.", Modifier.padding(16.dp), tag = "halaqa_full") }
                    LinkNotice(ui.link)
                    StatusStrip(ui)
                    IkSection("On stage", Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp)) {
                        Text("${HalaqaLogic.stage(ui.members).size}/${HalaqaLogic.MAX_SPEAKERS}", color = stitchMutedText(), fontSize = 12.sp)
                    }
                    MemberGrid(HalaqaLogic.stage(ui.members), ui, big = true) { if (ui.isHost && it.uid != ui.myUid) menuFor = it }
                    val hands = HalaqaLogic.hands(ui.members)
                    if (ui.isHost && hands.isNotEmpty()) {
                        IkSection("Raised hands · ${hands.size}", Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp))
                        hands.forEach { h -> HandRow(h, actions) }
                    }
                    val aud = HalaqaLogic.audience(ui.members)
                    IkSection("Listening · ${aud.size}", Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp))
                    if (aud.isEmpty()) Text("Share the halaqa so others can join you.", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
                    MemberGrid(aud, ui, big = false) { if (ui.isHost) menuFor = it }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        if (r.status == HalaqaStatus.LIVE && me != null) ControlBar(ui, me, actions) { confirmEnd = true }
    }
    menuFor?.let { m ->
        AlertDialog(onDismissRequest = { menuFor = null }, title = { Text(m.name.ifBlank { "Member" }, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (!m.role.onStage) MenuRow(Icons.Outlined.RecordVoiceOver, "Invite to speak", "halaqa_menu_invite") { menuFor = null; actions.onInvite(m) }
                    if (m.role == HalaqaRole.SPEAKER && !m.muted) MenuRow(Icons.Outlined.MicOff, "Mute", "halaqa_menu_mute") { menuFor = null; actions.onMute(m) }
                    if (m.role == HalaqaRole.SPEAKER) MenuRow(Icons.Outlined.ArrowDownward, "Move to listeners", "halaqa_menu_down") { menuFor = null; actions.onMoveDown(m) }
                    MenuRow(Icons.Outlined.PersonRemove, "Remove from halaqa", "halaqa_menu_remove") { menuFor = null; actions.onRemove(m) }
                }
            },
            confirmButton = { TextButton(onClick = { menuFor = null }) { Text("Close") } })
    }
    if (confirmEnd) AlertDialog(onDismissRequest = { confirmEnd = false }, title = { Text("End this halaqa?", fontWeight = FontWeight.Bold) },
        text = { Text("Everyone will leave the room. JazakAllahu khairan for hosting.") },
        confirmButton = { TextButton(onClick = { confirmEnd = false; actions.onEnd() }, modifier = Modifier.testTag("halaqa_end_confirm")) { Text("End for all") } },
        dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Cancel") } })
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ikAccentText(), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(text, fontSize = 15.sp)
    }
}

@Composable
private fun StatusStrip(ui: HalaqaRoomUi) {
    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        LiveDot(); Spacer(Modifier.width(6.dp))
        Text("LIVE", color = IkRed, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
        Spacer(Modifier.width(10.dp))
        Text("${ui.members.size} in the circle", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.weight(1f))
        IkTag(ui.room.topic.label.uppercase())
    }
}

@Composable
private fun LinkNotice(link: HalaqaLink) {
    when (link) {
        HalaqaLink.NEEDS_RELAY -> IkNotice("Some voices can't reach you",
            "Your network blocks direct audio and NoorPro doesn't have a relay server yet. Try Wi-Fi or another network — the room keeps working for others.",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = true, tag = "halaqa_relay")
        HalaqaLink.FAILED -> IkNotice("Audio couldn't connect", "Please leave and join again.", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = true, tag = "halaqa_failed")
        else -> Unit
    }
}

@Composable
private fun MemberGrid(list: List<HalaqaMember>, ui: HalaqaRoomUi, big: Boolean, onTap: (HalaqaMember) -> Unit) {
    val cols = if (big) 3 else 4
    Column(Modifier.padding(horizontal = 12.dp)) {
        list.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { m -> MemberTile(m, ui, big, Modifier.weight(1f)) { onTap(m) } }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MemberTile(m: HalaqaMember, ui: HalaqaRoomUi, big: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val size = if (big) 68.dp else 50.dp
    Column(modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onTap).padding(vertical = 8.dp).testTag("halaqa_member_${m.uid}"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            val ring = if (HalaqaLogic.micLive(m)) Modifier.border(2.5.dp, IkGreen, CircleShape).padding(4.dp) else Modifier.padding(4.dp)
            IkAvatar(m.name, ring, size = size)
            if (m.role.onStage) Box(Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape).background(ikCard()).border(1.dp, stitchLine(), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (m.muted) Icons.Outlined.MicOff else Icons.Outlined.Mic, if (m.muted) "Muted" else "Speaking", tint = if (m.muted) stitchMutedText() else IkGreen, modifier = Modifier.size(14.dp))
            } else if (m.hand) Box(Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(IkGold), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PanTool, "Hand raised", tint = IkGreenDeep, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(if (m.uid == ui.myUid) "You" else m.name.ifBlank { "Guest" }, color = stitchText(), fontSize = if (big) 13.sp else 12.sp, fontWeight = if (big) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp))
        if (big) Text(when (m.role) { HalaqaRole.HOST -> "Host"; HalaqaRole.SPEAKER -> "Speaker"; else -> "" }, color = if (m.role == HalaqaRole.HOST) IkGoldDeep else stitchMutedText(), fontSize = 11.sp)
    }
}

@Composable
private fun HandRow(m: HalaqaMember, actions: HalaqaRoomActions) {
    IkCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("halaqa_hand_${m.uid}"), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkAvatar(m.name, size = 34.dp); Spacer(Modifier.width(10.dp))
            Text(m.name.ifBlank { "Guest" }, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IkPill("Invite", true, icon = Icons.Outlined.RecordVoiceOver, tag = "halaqa_invite_${m.uid}") { actions.onInvite(m) }
        }
    }
}

@Composable
private fun ControlBar(ui: HalaqaRoomUi, me: HalaqaMember, actions: HalaqaRoomActions, onEnd: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(ikCard()).border(1.dp, stitchLine()).padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (me.role.onStage) RoundControl(if (me.muted) Icons.Outlined.MicOff else Icons.Outlined.Mic, if (me.muted) "Unmute" else "Mute", active = !me.muted, tag = "halaqa_mic") { actions.onMic(me.muted) }
        else RoundControl(Icons.Outlined.PanTool, if (me.hand) "Lower hand" else "Raise hand", active = me.hand, tag = "halaqa_hand") { actions.onHand(!me.hand) }
        RoundControl(if (ui.speakerOn) Icons.Outlined.VolumeUp else Icons.Outlined.Hearing, if (ui.speakerOn) "Speaker" else "Earpiece", active = false, tag = "halaqa_speaker") { actions.onSpeaker(!ui.speakerOn) }
        Spacer(Modifier.weight(1f))
        if (ui.isHost) IkButton("End", icon = Icons.Outlined.StopCircle, primary = false, tag = "halaqa_end") { onEnd() }
        else if (me.role == HalaqaRole.SPEAKER) IkButton("Step down", icon = Icons.Outlined.ArrowDownward, primary = false, tag = "halaqa_stepdown") { actions.onMoveDown(me) }
        IkButton("Leave", icon = Icons.Outlined.Logout, tag = "halaqa_leave") { actions.onLeave() }
    }
}

@Composable
private fun RoundControl(icon: ImageVector, label: String, active: Boolean, tag: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(if (active) IkGreen else ikSoft()).clickable(onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = if (active) Color.White else ikAccentText(), modifier = Modifier.size(22.dp))
        }
        Text(label, color = stitchMutedText(), fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun HalaqaWaiting(ui: HalaqaRoomUi, actions: HalaqaRoomActions) {
    IkCard(Modifier.padding(16.dp).testTag("halaqa_waiting"), padding = 20.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IkIconTile(topicIcon(ui.room.topic), size = 56.dp)
            Spacer(Modifier.height(12.dp))
            Text(HalaqaLogic.whenLine(ui.room, ui.now), color = stitchText(), fontFamily = LibreCaslon, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(ui.room.description.ifBlank { "A ${ui.room.topic.label.lowercase()} halaqa with ${ui.room.hostName.ifBlank { "a host" }}." }, color = stitchMutedText(), fontSize = 13.sp,
                lineHeight = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            if (ui.isHost) IkButton("Go live now", Modifier.fillMaxWidth(), Icons.Outlined.Sensors, tag = "halaqa_golive") { actions.onGoLive() }
            else IkButton(if (ui.reminded) "Reminder on" else "Remind me ${HalaqaLogic.REMIND_BEFORE_MIN} min before", Modifier.fillMaxWidth(),
                if (ui.reminded) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsNone, primary = !ui.reminded, tag = "halaqa_room_remind") { actions.onRemind(!ui.reminded) }
        }
    }
}

@Composable
private fun HalaqaEnded(actions: HalaqaRoomActions) {
    IkCard(Modifier.padding(16.dp).testTag("halaqa_ended"), padding = 20.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IkIconTile(Icons.Outlined.CheckCircle, size = 56.dp)
            Spacer(Modifier.height(12.dp))
            Text("This halaqa has ended", color = stitchText(), fontFamily = LibreCaslon, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("May Allah accept it from everyone who gathered.\nSubhanaka Allahumma wa bihamdika, ash-hadu an la ilaha illa anta, astaghfiruka wa atubu ilayk.", color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            IkButton("Back to halaqas", Modifier.fillMaxWidth(), tag = "halaqa_back") { actions.onBack() }
        }
    }
}
