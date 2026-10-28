package com.noorpro.app.ui.islamic

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.halal.HalalTextFilter
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.community.CommunityException
import com.noorpro.app.islamic.social.*
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

/* 1.1.6 Good-deed challenges UI (challenges_enabled): my challenges, create, join by code, detail with gentle streaks. */

data class ChallengesUi(
    val list: List<Challenge> = emptyList(),
    val myDone: Map<String, Set<String>> = emptyMap(),
    val today: String = ChallengeLogic.dayKey(System.currentTimeMillis()),
    val loading: Boolean = false,
    val error: CommunityError? = null,
    val myUid: String? = null,
)

data class ChallengeDetailUi(
    val c: Challenge,
    val progress: List<ChallengeProgress> = emptyList(),
    val myUid: String = "",
    val today: String = ChallengeLogic.dayKey(System.currentTimeMillis()),
    val reminders: Boolean = true,
) {
    val mine get() = progress.firstOrNull { it.uid == myUid }?.done.orEmpty()
}

class ChallengesActions(
    val onBack: () -> Unit = {},
    val onCreate: () -> Unit = {},
    val onJoin: () -> Unit = {},
    val onOpen: (Challenge) -> Unit = {},
    val onToggleToday: (Challenge) -> Unit = {},
    val onTemplate: (ChallengeKind) -> Unit = {},
)

class ChallengeDetailActions(
    val onBack: () -> Unit = {},
    val onToggle: (String) -> Unit = {},
    val onInvite: () -> Unit = {},
    val onCopy: () -> Unit = {},
    val onReminders: (Boolean) -> Unit = {},
    val onLeave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onShareCard: (() -> Unit)? = null,
)

internal fun kindIcon(k: ChallengeKind): ImageVector = when (k) {
    ChallengeKind.QURAN -> Icons.Outlined.MenuBook
    ChallengeKind.ADHKAR -> Icons.Outlined.WbTwilight
    ChallengeKind.SALAWAT -> Icons.Outlined.FavoriteBorder
    ChallengeKind.SADAQAH -> Icons.Outlined.VolunteerActivism
    ChallengeKind.KINDNESS -> Icons.Outlined.EmojiPeople
    ChallengeKind.NIGHT -> Icons.Outlined.NightsStay
    ChallengeKind.CUSTOM -> Icons.Outlined.EmojiEvents
}

@Composable
fun ChallengesScreen(viewModel: DeenViewModel, backend: ChallengeBackend = remember { FirestoreChallengeBackend() }) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val local = remember { ChallengeLocal(ctx) }
    val clipboard = LocalClipboardManager.current
    var ui by remember { mutableStateOf(ChallengesUi(myUid = backend.uid())) }
    var creating by remember { mutableStateOf<ChallengeKind?>(null) }
    var joining by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<ChallengeDetailUi?>(null) }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()
    fun err(e: Throwable) = (e as? CommunityException)?.detail?.ifBlank { null } ?: CommunityError.from(e).body

    fun refresh() {
        if (!backend.available) { ui = ui.copy(error = CommunityError.OFFLINE_BACKEND); return }
        if (backend.uid() == null) { ui = ui.copy(error = CommunityError.SIGN_IN); return }
        ui = ui.copy(loading = true)
        scope.launch {
            runCatching { backend.mine() }.onSuccess { list ->
                local.remember(list)
                ui = ui.copy(list = list, myDone = list.associate { it.code to local.done(it.code) }, error = null, myUid = backend.uid())
                if (local.reminders && list.isNotEmpty()) ChallengeReminder.ensureScheduled(ctx)
            }.onFailure { ui = ui.copy(error = CommunityError.from(it)) }
            ui = ui.copy(loading = false)
        }
    }
    LaunchedEffect(Unit) { HalalTextFilter.ensureLoaded(ctx); refresh() }

    fun setMine(c: Challenge, done: Set<String>) {
        local.setDone(c.code, done)
        ui = ui.copy(myDone = ui.myDone + (c.code to done))
        open?.let { d -> if (d.c.code == c.code) open = d.copy(progress = d.progress.filter { it.uid != d.myUid } + ChallengeProgress(d.myUid, viewModel.userDisplayName, done)) }
        scope.launch { runCatching { backend.setDone(c, viewModel.userDisplayName, done) }.onFailure { toast(err(it)) } }
    }
    fun toggle(c: Challenge, day: String) {
        val cur = ui.myDone[c.code].orEmpty()
        val on = day !in cur
        setMine(c, ChallengeLogic.toggle(c, cur, day, on))
        if (on && day == ui.today) toast("MashaAllah. May Allah accept it.")
    }

    if (creating != null) {
        BackHandler { creating = null }
        ChallengeCreateContent(creating!!, ui.today, onBack = { creating = null }) { title, kind, goal, days, start, done ->
            scope.launch {
                runCatching { backend.create(title, kind, goal, days, start, viewModel.userDisplayName) }
                    .onSuccess { c -> done(null); creating = null; ui = ui.copy(list = listOf(c) + ui.list); local.remember(ui.list)
                        if (local.reminders) ChallengeReminder.ensureScheduled(ctx)
                        open = ChallengeDetailUi(c, myUid = backend.uid().orEmpty(), today = ui.today, reminders = local.reminders) }
                    .onFailure { done(err(it)) }
            }
        }
        return
    }
    if (joining) {
        BackHandler { joining = false }
        ChallengeJoinContent(onBack = { joining = false }) { code, done ->
            scope.launch {
                runCatching { backend.join(code, viewModel.userDisplayName) }
                    .onSuccess { c -> done(null); joining = false; ui = ui.copy(list = (listOf(c) + ui.list).distinctBy { it.code }); local.remember(ui.list)
                        if (local.reminders) ChallengeReminder.ensureScheduled(ctx)
                        open = ChallengeDetailUi(c, myUid = backend.uid().orEmpty(), today = ui.today, reminders = local.reminders); toast("You've joined. Bismillah!") }
                    .onFailure { done(err(it)) }
            }
        }
        return
    }
    open?.let { d ->
        BackHandler { open = null }
        LaunchedEffect(d.c.code) {
            runCatching { backend.progress(d.c) }.onSuccess { p ->
                open = open?.copy(progress = p)
                p.firstOrNull { it.uid == d.myUid }?.let { local.setDone(d.c.code, it.done); ui = ui.copy(myDone = ui.myDone + (d.c.code to it.done)) }
            }
        }
        val mine = ui.myDone[d.c.code].orEmpty()
        val shown = d.copy(progress = d.progress.filter { it.uid != d.myUid } + ChallengeProgress(d.myUid, viewModel.userDisplayName, mine))
        ChallengeDetailContent(shown, ChallengeDetailActions(
            onBack = { open = null },
            onToggle = { day -> toggle(d.c, day) },
            onInvite = { runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ChallengeLogic.inviteText(d.c)), "Invite friends")) } },
            onCopy = { clipboard.setText(AnnotatedString(d.c.code)); toast("Code copied") },
            onReminders = { on -> local.reminders = on; open = d.copy(reminders = on); if (on) ChallengeReminder.ensureScheduled(ctx) else ChallengeReminder.cancel(ctx) },
            onLeave = { scope.launch { runCatching { backend.leave(d.c) }.onSuccess { open = null; ui = ui.copy(list = ui.list.filter { it.code != d.c.code }); local.remember(ui.list) }.onFailure { toast(err(it)) } } },
            onDelete = { scope.launch { runCatching { backend.delete(d.c) }.onSuccess { open = null; ui = ui.copy(list = ui.list.filter { it.code != d.c.code }); local.remember(ui.list) }.onFailure { toast(err(it)) } } },
            onShareCard = if (com.noorpro.app.islamic.IslamicFlags.isOn(com.noorpro.app.islamic.IslamicFlags.SHARE_CARDS)) {
                { ShareCardLaunch.open("Bukhari 6464"); viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.UMMAH_SHARE_CARDS) }
            } else null,
        ))
        return
    }
    ChallengesHomeContent(ui, ChallengesActions(
        onBack = { viewModel.goBack() },
        onCreate = { if (ui.error == null) creating = ChallengeKind.QURAN else toast(ui.error!!.body) },
        onJoin = { if (ui.error == null) joining = true else toast(ui.error!!.body) },
        onOpen = { c -> open = ChallengeDetailUi(c, myUid = backend.uid().orEmpty(), today = ui.today, reminders = local.reminders) },
        onToggleToday = { c -> toggle(c, ui.today) },
        onTemplate = { k -> if (ui.error == null) creating = k else toast(ui.error!!.body) },
    ))
}

@Composable
fun ChallengesHomeContent(ui: ChallengesUi, actions: ChallengesActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("challenges_screen")) {
        IkHeader("Good-deed challenges", "Small deeds, together", actions.onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("CONSISTENCY OVER QUANTITY", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Text("“The most beloved deed to Allah is the most regular and constant, even if it were little.”", color = IkCream, fontFamily = LibreCaslon,
                fontSize = 16.sp, lineHeight = 22.sp, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(6.dp))
            Text("Sahih al-Bukhari 6464", color = IkCream.copy(alpha = 0.7f), fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(onClick = actions.onCreate).padding(horizontal = 14.dp, vertical = 11.dp).testTag("challenges_create"),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Add, null, tint = IkGreenDeep, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text("Start one", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row(Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, IkCream.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).clickable(onClick = actions.onJoin)
                    .padding(horizontal = 14.dp, vertical = 11.dp).testTag("challenges_join"), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Key, null, tint = IkCream, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text("Join with a code", color = IkCream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        ui.error?.let { e ->
            IkNotice(e.title, when (e) { CommunityError.OFFLINE_BACKEND -> "Challenges aren't switched on yet. Please check back soon."; CommunityError.SIGN_IN -> "Sign in to start or join a challenge with friends."; else -> e.body },
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = e == CommunityError.OFFLINE_BACKEND || e == CommunityError.NETWORK, tag = "challenges_notice")
        }
        if (ui.list.isNotEmpty()) {
            IkSection("Your challenges", Modifier.padding(horizontal = 16.dp).padding(top = 8.dp))
            ui.list.forEach { c -> ChallengeCard(c, ui.myDone[c.code].orEmpty(), ui.today, actions) }
        } else if (ui.error == null) {
            Text(if (ui.loading) "Loading your challenges…" else "No challenges yet. Pick a small deed below and invite a friend or two.",
                color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).testTag("challenges_empty"))
        }
        IkSection("Ideas to start with", Modifier.padding(horizontal = 16.dp).padding(top = 12.dp))
        ChallengeKind.entries.filter { it != ChallengeKind.CUSTOM }.chunked(2).forEach { row ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { k ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp))
                        .clickable { actions.onTemplate(k) }.padding(12.dp).testTag("challenges_idea_${k.name}")) {
                        IkIconTile(kindIcon(k), size = 34.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(k.defaultTitle, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(k.goal, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Text("No leaderboards here — only a shared total. Deeds are between you and Allah; the group just helps you stay consistent.",
            color = stitchMutedText(), fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ChallengeCard(c: Challenge, done: Set<String>, today: String, actions: ChallengesActions) {
    val phase = ChallengeLogic.phase(c, today)
    val n = ChallengeLogic.dayNumber(c, today)
    val doneToday = today in done
    IkCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).clickable { actions.onOpen(c) }.testTag("challenge_card_${c.code}"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkIconTile(kindIcon(c.kind), size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(when (phase) {
                    ChallengePhase.UPCOMING -> "Starts ${if (n == 0) "tomorrow" else "in ${1 - n} days"} · ${c.days} days"
                    ChallengePhase.ACTIVE -> "Day $n of ${c.days} · ${c.members.size} ${if (c.members.size == 1) "person" else "people"}"
                    ChallengePhase.FINISHED -> "Completed · ${ChallengeLogic.doneInWindow(c, done)} of ${c.days} days"
                }, color = stitchMutedText(), fontSize = 12.sp)
            }
            if (phase == ChallengePhase.ACTIVE) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(if (doneToday) IkGreen else Color.Transparent)
                    .border(2.dp, if (doneToday) IkGreen else IkGold, CircleShape).clickable { actions.onToggleToday(c) }.testTag("challenge_today_${c.code}"),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, if (doneToday) "Done today" else "Mark today done", tint = if (doneToday) Color.White else IkGoldDeep, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(ChallengeLogic.fraction(c, done))
        Spacer(Modifier.height(6.dp))
        val s = ChallengeLogic.streak(done, today)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Spa, null, tint = ikAccentText(), modifier = Modifier.size(14.dp)); Spacer(Modifier.width(5.dp))
            Text(ChallengeLogic.streakLine(s), color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProgressBar(f: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ikSoft())) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(IkGreen))
    }
}

@Composable
fun ChallengeCreateContent(initial: ChallengeKind, today: String, onBack: () -> Unit, modifier: Modifier = Modifier,
                           onCreate: (String, ChallengeKind, String, Int, String, (String?) -> Unit) -> Unit) {
    var kind by remember { mutableStateOf(initial) }
    var title by remember { mutableStateOf(initial.defaultTitle) }
    var goal by remember { mutableStateOf(initial.goal) }
    var days by remember { mutableStateOf(21) }
    var startToday by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val held = remember(title, goal) { listOf(title, goal).any { t -> t.isNotBlank() && HalalTextFilter.check(t).let { it.action == HalalTextFilter.Action.BLOCK || it.action == HalalTextFilter.Action.QUEUE } } }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("challenge_create")) {
        IkHeader("New challenge", "Pick a small deed", onBack)
        Spacer(Modifier.height(4.dp))
        IkPillRow(ChallengeKind.entries, kind, { it.label }, tag = { "challenge_kind_${it.name}" }) { k ->
            if (title.isBlank() || ChallengeKind.entries.any { it.defaultTitle == title }) title = k.defaultTitle
            if (goal.isBlank() || ChallengeKind.entries.any { it.goal == goal }) goal = k.goal
            kind = k
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(10.dp))
            IkField(title, { title = it; error = null }, "Name", tag = "challenge_title", maxLen = 60)
            Spacer(Modifier.height(8.dp))
            IkField(goal, { goal = it; error = null }, "Each day, we will…", tag = "challenge_goal", maxLen = 120)
            IkSection("How long", Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChallengeLogic.LENGTHS.forEach { n -> IkPill("$n days", days == n, tag = "challenge_days_$n") { days = n } }
            }
            IkSection("Starting", Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IkPill("Today", startToday, tag = "challenge_start_today") { startToday = true }
                IkPill("Tomorrow", !startToday, tag = "challenge_start_tomorrow") { startToday = false }
            }
            Spacer(Modifier.height(12.dp))
            Text(when {
                held -> "Please remove links, offers or anything against the community guidelines."
                error != null -> error!!
                else -> "You'll get a code to invite up to ${ChallengeLogic.MAX_MEMBERS - 1} friends or family."
            }, color = if (held || error != null) IkAmber else stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.testTag("challenge_hint"))
            Spacer(Modifier.height(14.dp))
            IkButton(if (busy) "Creating…" else "Create and get invite code", Modifier.fillMaxWidth(), Icons.Outlined.EmojiEvents,
                enabled = !busy && !held && ChallengeLogic.validateTitle(title) == null, tag = "challenge_create_go") {
                busy = true
                val start = if (startToday) today else LocalDate.parse(today).plusDays(1).toString()
                onCreate(title.trim(), kind, goal.trim(), days, start) { e -> busy = false; error = e }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun ChallengeJoinContent(onBack: () -> Unit, modifier: Modifier = Modifier, initial: String = "", onJoin: (String, (String?) -> Unit) -> Unit) {
    var code by remember { mutableStateOf(initial) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("challenge_join")) {
        IkHeader("Join a challenge", "Enter the code a friend shared", onBack)
        Column(Modifier.padding(16.dp)) {
            IkField(code, { code = ChallengeLogic.normalizeCode(it); error = null }, "8-character code", tag = "challenge_code", maxLen = 12)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                (0 until ChallengeLogic.CODE_LEN).forEach { i ->
                    Box(Modifier.size(34.dp, 42.dp).clip(RoundedCornerShape(10.dp)).background(ikCard())
                        .border(1.5.dp, if (i < code.length) IkGreen else stitchLine(), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Text(code.getOrNull(i)?.toString() ?: "", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(error ?: "Codes use letters and numbers only, without 0, O, 1 or I.", color = if (error != null) IkAmber else stitchMutedText(), fontSize = 12.sp,
                modifier = Modifier.testTag("challenge_join_hint"))
            Spacer(Modifier.height(16.dp))
            IkButton(if (busy) "Joining…" else "Join", Modifier.fillMaxWidth(), Icons.Outlined.GroupAdd, enabled = !busy && ChallengeLogic.validCode(code), tag = "challenge_join_go") {
                busy = true; onJoin(code) { e -> busy = false; error = e }
            }
        }
    }
}

@Composable
fun ChallengeDetailContent(ui: ChallengeDetailUi, actions: ChallengeDetailActions, modifier: Modifier = Modifier) {
    val c = ui.c
    val phase = ChallengeLogic.phase(c, ui.today)
    val n = ChallengeLogic.dayNumber(c, ui.today)
    val mine = ui.mine
    val isOwner = c.createdBy == ui.myUid
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("challenge_detail")) {
        IkHeader(c.title, c.kind.label, actions.onBack) {
            Box {
                Icon(Icons.Outlined.MoreVert, "More", tint = stitchText(), modifier = Modifier.size(40.dp).clip(CircleShape).clickable { menu = true }.padding(9.dp).testTag("challenge_menu"))
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text(if (isOwner) "Delete challenge" else "Leave challenge") }, { menu = false; confirm = true }, modifier = Modifier.testTag("challenge_leave"))
                }
            }
        }
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(when (phase) { ChallengePhase.UPCOMING -> "STARTS ${if (n == 0) "TOMORROW" else "SOON"}"; ChallengePhase.ACTIVE -> "DAY $n OF ${c.days}"; ChallengePhase.FINISHED -> "COMPLETED" },
                        color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(c.goal.ifBlank { c.title }, color = IkCream, fontFamily = LibreCaslon, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(ChallengeLogic.streakLine(ChallengeLogic.streak(mine, ui.today)), color = IkCream.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.testTag("challenge_streak"))
                }
                Spacer(Modifier.width(12.dp))
                IkRing(ChallengeLogic.fraction(c, mine), size = 92.dp, stroke = 8.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${ChallengeLogic.doneInWindow(c, mine)}", color = IkCream, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("of ${c.days}", color = IkCream.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
            if (phase == ChallengePhase.ACTIVE) {
                Spacer(Modifier.height(14.dp))
                val doneToday = ui.today in mine
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (doneToday) Color.White.copy(alpha = 0.12f) else IkGold)
                    .clickable { actions.onToggle(ui.today) }.padding(vertical = 12.dp).testTag("challenge_mark_today"),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (doneToday) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (doneToday) IkCream else IkGreenDeep, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (doneToday) "Done today, alhamdulillah" else "I did today's deed", color = if (doneToday) IkCream else IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        IkSection("Your days", Modifier.padding(horizontal = 16.dp).padding(top = 8.dp))
        DayGrid(c, mine, ui.today, actions.onToggle)
        Text("Missed a day? That's okay — tap a past day if you did it, and one grace day each week keeps your streak.",
            color = stitchMutedText(), fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

        IkSection("Together", Modifier.padding(horizontal = 16.dp).padding(top = 12.dp))
        val total = ChallengeLogic.together(ui.progress, c)
        Text(ChallengeLogic.togetherLine(total, ui.progress.size.coerceAtLeast(c.members.size)), color = ikAccentText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).testTag("challenge_together"))
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = 12.dp) {
            val people = ui.progress.sortedWith(compareBy<ChallengeProgress> { it.uid != ui.myUid }.thenBy { it.name.lowercase() })
            people.forEachIndexed { i, p -> MemberRow(p, c, ui.today, p.uid == ui.myUid); if (i < people.lastIndex) Spacer(Modifier.height(10.dp)) }
            val quiet = (c.members.size - people.size).coerceAtLeast(0)
            if (quiet > 0) Text("+ $quiet more who haven't checked in yet", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }

        IkSection("Invite", Modifier.padding(horizontal = 16.dp).padding(top = 12.dp))
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Invite code", color = stitchMutedText(), fontSize = 11.sp)
                    Text(c.code.chunked(4).joinToString(" "), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 2.sp, modifier = Modifier.testTag("challenge_code_shown"))
                }
                Icon(Icons.Outlined.ContentCopy, "Copy code", tint = stitchMutedText(), modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = actions.onCopy).padding(10.dp))
            }
            Spacer(Modifier.height(10.dp))
            IkButton("Invite friends", Modifier.fillMaxWidth(), Icons.Outlined.Share, tag = "challenge_invite", onClick = actions.onInvite)
            Text("${c.members.size} of ${ChallengeLogic.MAX_MEMBERS} places taken", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikCard())
            .border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IkIconTile(Icons.Outlined.NotificationsNone, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Gentle evening reminder", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Around 8 pm, only if today isn't marked yet", color = stitchMutedText(), fontSize = 12.sp)
            }
            Switch(ui.reminders, actions.onReminders, Modifier.testTag("challenge_reminders"), colors = SwitchDefaults.colors(checkedTrackColor = IkGreen))
        }
        actions.onShareCard?.let { share ->
            IkButton("Share a reminder card", Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(), Icons.Outlined.Style, primary = false, tag = "challenge_share_card", onClick = share)
        }
        Spacer(Modifier.height(28.dp))
    }
    if (confirm) {
        AlertDialog(onDismissRequest = { confirm = false },
            title = { Text(if (isOwner) "Delete this challenge?" else "Leave this challenge?") },
            text = { Text(if (isOwner) "It will be removed for everyone. Your good deeds stay with Allah." else "Your check-ins will be removed from the group.") },
            confirmButton = { TextButton({ confirm = false; if (isOwner) actions.onDelete() else actions.onLeave() }, Modifier.testTag("challenge_leave_confirm")) { Text(if (isOwner) "Delete" else "Leave", color = IkRed) } },
            dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } })
    }
}

@Composable
private fun DayGrid(c: Challenge, done: Set<String>, today: String, onToggle: (String) -> Unit) {
    val days = ChallengeLogic.window(c)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikCard())
        .border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).padding(12.dp).testTag("challenge_days")) {
        days.chunked(7).forEachIndexed { r, row ->
            if (r > 0) Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { i, key ->
                    val num = r * 7 + i + 1
                    val isDone = key in done
                    val future = key > today
                    val isToday = key == today
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                        .background(when { isDone -> IkGreen; future -> Color.Transparent; else -> ikSoft() })
                        .border(if (isToday && !isDone) 2.dp else 1.dp, when { isToday && !isDone -> IkGold; future -> stitchLine(); else -> Color.Transparent }, CircleShape)
                        .clickable(enabled = !future) { onToggle(key) }.testTag("challenge_day_$num"), contentAlignment = Alignment.Center) {
                        if (isDone) Icon(Icons.Outlined.Check, "Day $num done", tint = Color.White, modifier = Modifier.size(16.dp))
                        else Text("$num", color = if (future) stitchMutedText() else stitchText(), fontSize = 12.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MemberRow(p: ChallengeProgress, c: Challenge, today: String, me: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("challenge_member_${p.uid}")) {
        IkAvatar(p.name, size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (me) "You" else p.name.ifBlank { "Friend" }, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${ChallengeLogic.doneInWindow(c, p.done)} of ${c.days} days", color = stitchMutedText(), fontSize = 11.sp)
        }
        val last = (6 downTo 0).map { LocalDate.parse(today).minusDays(it.toLong()).toString() }.filter { it in ChallengeLogic.window(c) }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            last.forEach { k -> Box(Modifier.size(9.dp).clip(CircleShape).background(if (k in p.done) IkGreen else ikSoft())) }
        }
    }
}
