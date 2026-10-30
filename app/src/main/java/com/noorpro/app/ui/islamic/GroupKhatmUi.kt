package com.noorpro.app.ui.islamic

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.islamic.khatm.*
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

/* 1.1.6 Group Khatm UI (group_khatm_enabled). Stateless contents + one wired screen. */

data class KhatmHomeUi(
    val groups: List<KhatmGroup> = emptyList(),
    val loading: Boolean = false,
    val error: KhatmError? = null,
    val busy: Boolean = false,
)

data class KhatmGroupUi(
    val group: KhatmGroup,
    val claims: Map<Int, ParaClaim> = emptyMap(),
    val myUid: String? = null,
    val selected: Int? = null,
    val error: KhatmError? = null,
    val busy: Boolean = false,
) {
    val progress get() = KhatmLogic.progress(claims.values)
}

@Composable
fun GroupKhatmScreen(viewModel: DeenViewModel, backend: KhatmBackend = remember { FirestoreKhatmBackend() }) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { KhatmLocalStore(ctx) }
    val pending by KhatmNav.pendingCode.collectAsState()
    var page by remember { mutableStateOf("home") }
    var openCode by remember { mutableStateOf<String?>(null) }
    var home by remember { mutableStateOf(KhatmHomeUi(groups = store.codes().map { KhatmGroup(it, store.title(it).ifBlank { "Group Khatm" }, "", "", "", emptyList()) })) }
    var groupUi by remember { mutableStateOf<KhatmGroupUi?>(null) }
    val name = viewModel.userDisplayName.ifBlank { "A reader" }

    fun gate(): KhatmError? = when {
        !backend.available -> KhatmError.OFFLINE_BACKEND
        backend.uid() == null -> KhatmError.SIGN_IN
        else -> null
    }
    fun refresh() {
        gate()?.let { home = home.copy(error = it); return }
        home = home.copy(loading = true)
        scope.launch {
            runCatching { backend.mine() }
                .onSuccess { l -> l.forEach(store::remember); home = home.copy(groups = l, loading = false, error = null) }
                .onFailure { home = home.copy(loading = false, error = KhatmError.from(it)) }
        }
    }
    fun open(code: String) { openCode = code; page = "group" }
    fun join(code: String) {
        gate()?.let { home = home.copy(error = it); return }
        home = home.copy(busy = true)
        scope.launch {
            runCatching { backend.join(code) }
                .onSuccess { g -> store.remember(g); home = home.copy(busy = false, error = null); open(g.code) }
                .onFailure { home = home.copy(busy = false, error = KhatmError.from(it)) }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(pending) { pending?.let { KhatmNav.pendingCode.value = null; join(it) } }

    // Live listener while a group is open.
    DisposableEffect(openCode, page) {
        val code = openCode
        if (code == null || page == "home" || page == "create" || gate() != null) return@DisposableEffect onDispose {}
        groupUi = groupUi?.takeIf { it.group.code == code } ?: KhatmGroupUi(KhatmGroup(code, store.title(code).ifBlank { "Group Khatm" }, "", "", "", emptyList()), myUid = backend.uid())
        val stop = backend.observe(code,
            onGroup = { g -> if (g != null) groupUi = groupUi?.copy(group = g) },
            onParas = { l -> groupUi = groupUi?.copy(claims = l.associateBy { it.para }, error = null) },
            onError = { e -> groupUi = groupUi?.copy(error = KhatmError.from(e)) })
        onDispose { stop() }
    }

    fun act(block: suspend () -> Unit) {
        groupUi = groupUi?.copy(busy = true)
        scope.launch {
            runCatching { block() }
                .onSuccess { groupUi = groupUi?.copy(busy = false, error = null) }
                .onFailure { groupUi = groupUi?.copy(busy = false, error = KhatmError.from(it)) }
        }
    }
    fun share(g: KhatmGroup) = runCatching {
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, KhatmLogic.shareText(g)), "Invite to Group Khatm"))
    }

    when (page) {
        "create" -> {
            BackHandler { page = "home" }
            KhatmCreateContent(home.busy, home.error, onBack = { page = "home" }, onCreate = { t, d ->
                gate()?.let { home = home.copy(error = it); return@KhatmCreateContent }
                home = home.copy(busy = true)
                scope.launch {
                    runCatching { backend.create(t, d, name) }
                        .onSuccess { g -> store.remember(g); home = home.copy(busy = false, error = null, groups = listOf(g) + home.groups.filter { it.code != g.code }); open(g.code) }
                        .onFailure { home = home.copy(busy = false, error = KhatmError.from(it)) }
                }
            })
        }
        "group", "dua" -> {
            val ui = groupUi
            BackHandler { if (page == "dua") page = "group" else { page = "home"; openCode = null } }
            if (ui == null) KhatmHomeContent(home, {}, {}, {}, {})
            else if (page == "dua") KhatmDuaContent(ui.group, onBack = { page = "group" }, onShare = { share(ui.group) })
            else KhatmGroupContent(
                ui,
                onBack = { page = "home"; openCode = null },
                onShare = { share(ui.group) },
                onSelect = { groupUi = ui.copy(selected = it) },
                onClaim = { n -> act { backend.claim(ui.group.code, n, name) } },
                onDone = { n, d -> act { backend.setDone(ui.group.code, n, d) } },
                onRelease = { n -> act { backend.release(ui.group.code, n) } },
                onRead = { n -> viewModel.selectJuz(n) },
                onDua = { page = "dua" },
            )
        }
        else -> KhatmHomeContent(home, onBack = { viewModel.goBack() }, onCreate = { home = home.copy(error = null); page = "create" }, onJoin = ::join, onOpen = ::open)
    }
}

@Composable
fun KhatmHomeContent(ui: KhatmHomeUi, onBack: () -> Unit, onCreate: () -> Unit, onJoin: (String) -> Unit, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    var code by remember { mutableStateOf("") }
    val parsed = KhatmLogic.parseInvite(code)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("khatm_home")) {
        IkHeader("Group Khatm", "Complete the Qur'an together", onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("خَتْمُ القُرْآن", color = IkGold, fontFamily = QuranArabicFont, fontSize = 22.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("30 paras, shared by family and friends", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Start a Khatm, share the link, and everyone picks a para. Progress updates live.", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.width(12.dp))
                MiniParaWheel()
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(enabled = !ui.busy, onClick = onCreate).testTag("khatm_create"),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = IkGreenDeep, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("Start a Group Khatm", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        ui.error?.let { IkNotice(it.title, it.body, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = it == KhatmError.OFFLINE_BACKEND || it == KhatmError.NETWORK, tag = "khatm_notice") }
        IkSection("Join with a code or link", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), padding = 12.dp) {
            IkField(code, { code = it }, "e.g. K7M2QP or the invite link", tag = "khatm_code", maxLen = 120)
            Spacer(Modifier.height(10.dp))
            IkButton(if (ui.busy) "Joining…" else "Join Khatm", Modifier.fillMaxWidth(), icon = Icons.Outlined.Link, enabled = parsed != null && !ui.busy, tag = "khatm_join") { parsed?.let(onJoin) }
        }
        IkSection("My Khatms", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        if (ui.groups.isEmpty()) {
            Text(if (ui.loading) "Loading…" else "Khatms you start or join will appear here.", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }
        ui.groups.forEach { g ->
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp))
                    .clickable { onOpen(g.code) }.padding(12.dp).testTag("khatm_group_${g.code}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IkIconTile(Icons.Outlined.Groups, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOfNotNull(g.dedication.takeIf { it.isNotBlank() }, g.memberUids.size.takeIf { it > 0 }?.let { "$it reader" + if (it == 1) "" else "s" }).joinToString(" · ")
                    Text(sub.ifBlank { "Code ${g.code}" }, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(g.code, color = ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ikSoft()).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

/** Small decorative ring of 30 segments (pure geometry — no imagery). */
@Composable
private fun MiniParaWheel() {
    androidx.compose.foundation.Canvas(Modifier.size(78.dp)) {
        val r = size.minDimension / 2f
        for (i in 0 until 30) {
            val a = Math.toRadians(i * 12.0 - 90).toFloat()
            val inner = r * 0.62f; val outer = r * 0.95f
            drawLine(if (i < 19) IkGold else Color.White.copy(alpha = 0.22f),
                androidx.compose.ui.geometry.Offset(center.x + inner * kotlin.math.cos(a), center.y + inner * kotlin.math.sin(a)),
                androidx.compose.ui.geometry.Offset(center.x + outer * kotlin.math.cos(a), center.y + outer * kotlin.math.sin(a)),
                strokeWidth = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        drawCircle(Color.White.copy(alpha = 0.10f), r * 0.42f)
    }
}

@Composable
fun KhatmCreateContent(busy: Boolean, error: KhatmError?, onBack: () -> Unit, onCreate: (String, String) -> Unit, modifier: Modifier = Modifier) {
    var title by remember { mutableStateOf("") }
    var dedication by remember { mutableStateOf("") }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("khatm_create_screen")) {
        IkHeader("New Group Khatm", "Everyone you invite can pick a para", onBack)
        IkCard(Modifier.padding(16.dp)) {
            IkField(title, { title = it }, "Name, e.g. Family Ramadan Khatm", tag = "khatm_title", maxLen = 80)
            Spacer(Modifier.height(10.dp))
            IkField(dedication, { dedication = it }, "Intention or dedication (optional)", tag = "khatm_dedication", singleLine = false, maxLen = 200)
            Spacer(Modifier.height(6.dp))
            Text("For example: for the recovery of a loved one, or as sadaqah for a parent who has passed away.", color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(14.dp))
            IkButton(if (busy) "Creating…" else "Create and get invite link", Modifier.fillMaxWidth(), icon = Icons.Default.Share, enabled = !busy, tag = "khatm_create_go") { onCreate(title, dedication) }
        }
        error?.let { IkNotice(it.title, it.body, Modifier.padding(horizontal = 16.dp), offline = it == KhatmError.OFFLINE_BACKEND, tag = "khatm_notice") }
        IkSection("How it works", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        listOf(
            "Share the link or 6-letter code on WhatsApp or anywhere.",
            "Each reader taps an open para to take it — it shows their name.",
            "Mark it complete when you've read it. The ring fills for everyone.",
            "When all 30 are done, make dua together for the intention.",
        ).forEachIndexed { i, s ->
            Row(Modifier.padding(horizontal = 20.dp, vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(ikSoft()), contentAlignment = Alignment.Center) { Text("${i + 1}", color = ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Text(s, color = stitchText(), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun KhatmGroupContent(
    ui: KhatmGroupUi, onBack: () -> Unit, onShare: () -> Unit, onSelect: (Int) -> Unit, onClaim: (Int) -> Unit,
    onDone: (Int, Boolean) -> Unit, onRelease: (Int) -> Unit, onRead: (Int) -> Unit, onDua: () -> Unit, modifier: Modifier = Modifier,
) {
    val g = ui.group
    val p = ui.progress
    val sel = ui.selected ?: ui.claims.values.firstOrNull { it.uid == ui.myUid && it.status == ParaStatus.CLAIMED }?.para ?: KhatmLogic.nextOpen(ui.claims)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("khatm_group")) {
        IkHeader(g.title, "Code ${g.code}", onBack) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(ikSoft()).clickable(onClick = onShare).testTag("khatm_share"), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Share, "Invite", tint = ikAccentText(), modifier = Modifier.size(19.dp))
            }
        }
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).testTag("khatm_progress")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IkRing(p.fraction, size = 112.dp, stroke = 10.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${p.done}", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 30.sp)
                        Text("of 30", color = IkCream.copy(alpha = 0.75f), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (p.complete) "Khatm complete" else "Reading together", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    if (g.dedication.isNotBlank()) Text(g.dedication, color = IkCream, fontFamily = LibreCaslon, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(8.dp))
                    StatLine(IkGold, "${p.done} completed")
                    StatLine(Color(0xFF9ED9BE), "${p.claimed} being read")
                    StatLine(Color.White.copy(alpha = 0.35f), "${p.open} open")
                    if (g.memberUids.isNotEmpty()) Text("${g.memberUids.size} reader" + (if (g.memberUids.size == 1) "" else "s"), color = IkCream.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (p.complete) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(onClick = onDua).testTag("khatm_dua_open"),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("Make the completion dua", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        ui.error?.let { IkNotice(it.title, it.body, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), offline = it == KhatmError.OFFLINE_BACKEND || it == KhatmError.NETWORK, tag = "khatm_notice") }
        sel?.let { ParaPanel(it, ui, onClaim, onDone, onRelease, onRead, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        IkSection("All 30 paras", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        Column(Modifier.padding(horizontal = 12.dp)) {
            (1..30).chunked(5).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { n -> ParaTile(n, KhatmLogic.view(n, ui.claims, ui.myUid), ui.claims[n], n == sel, Modifier.weight(1f)) { onSelect(n) } }
                }
            }
        }
        Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(ikSoft(), "Open"); Legend(IkGold, "Yours"); Legend(IkGreen, "Done"); Legend(if (isStitchLight()) Color(0xFFE9E4D8) else Color(0xFF2A322E), "Taken")
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable private fun StatLine(c: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(6.dp)); Text(text, color = IkCream, fontSize = 12.sp)
    }
}

@Composable private fun Legend(c: Color, t: String) {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c).border(1.dp, stitchLine(), RoundedCornerShape(3.dp)))
    Text(" $t   ", color = stitchMutedText(), fontSize = 11.sp)
}

@Composable
private fun ParaTile(n: Int, v: ParaView, claim: ParaClaim?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val light = isStitchLight()
    val (bg, fg) = when (v) {
        ParaView.OPEN -> ikSoft() to ikAccentText()
        ParaView.MINE -> IkGold to IkGreenDeep
        ParaView.MINE_DONE, ParaView.DONE -> IkGreen to Color.White
        ParaView.TAKEN -> (if (light) Color(0xFFE9E4D8) else Color(0xFF2A322E)) to stitchText()
    }
    Box(modifier.padding(4.dp).height(62.dp).clip(RoundedCornerShape(14.dp)).background(bg)
        .let { if (selected) it.border(2.dp, if (v == ParaView.MINE) IkGreenDeep else IkGold, RoundedCornerShape(14.dp)) else it }
        .clickable(onClick = onClick).testTag("khatm_para_$n"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$n", color = fg, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 20.sp)
            val label = when (v) {
                ParaView.OPEN -> "open"
                ParaView.MINE -> "yours"
                ParaView.MINE_DONE -> "done"
                else -> claim?.name?.substringBefore(' ')?.take(8).orEmpty()
            }
            Text(label, color = fg.copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 3.dp))
        }
        if (v == ParaView.MINE_DONE || v == ParaView.DONE) Icon(Icons.Default.Check, null, tint = IkGold, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(12.dp))
    }
}

@Composable
private fun ParaPanel(n: Int, ui: KhatmGroupUi, onClaim: (Int) -> Unit, onDone: (Int, Boolean) -> Unit, onRelease: (Int) -> Unit, onRead: (Int) -> Unit, modifier: Modifier) {
    val v = KhatmLogic.view(n, ui.claims, ui.myUid)
    val c = ui.claims[n]
    val (s, a) = Paras.starts[n - 1]
    IkCard(modifier.testTag("khatm_panel"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(ikSoft()), contentAlignment = Alignment.Center) {
                Text("$n", color = ikAccentText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Para $n · ${Paras.name(n)}", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Begins at surah $s, ayah $a", color = stitchMutedText(), fontSize = 12.sp)
                val who = when (v) {
                    ParaView.OPEN -> "Open — anyone can take it"
                    ParaView.MINE -> "You're reading this para"
                    ParaView.MINE_DONE -> "You completed this para"
                    ParaView.TAKEN -> "${c?.name} is reading"
                    ParaView.DONE -> "Completed by ${c?.name}"
                }
                Text(who, color = if (v == ParaView.MINE) IkGoldDeep else ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (v) {
                ParaView.OPEN -> IkButton("Take this para", Modifier.weight(1f), icon = Icons.Default.Add, enabled = !ui.busy, tag = "khatm_claim") { onClaim(n) }
                ParaView.MINE -> {
                    IkButton("Mark complete", Modifier.weight(1f), icon = Icons.Default.Check, enabled = !ui.busy, tag = "khatm_done") { onDone(n, true) }
                    IkButton("Release", primary = false, enabled = !ui.busy, tag = "khatm_release") { onRelease(n) }
                }
                ParaView.MINE_DONE -> IkButton("Undo complete", Modifier.weight(1f), primary = false, enabled = !ui.busy, tag = "khatm_undo") { onDone(n, false) }
                else -> {}
            }
            IkButton("Read", primary = v != ParaView.OPEN && v != ParaView.MINE, icon = Icons.AutoMirrored.Filled.MenuBook, tag = "khatm_read",
                modifier = if (v == ParaView.TAKEN || v == ParaView.DONE) Modifier.weight(1f) else Modifier) { onRead(n) }
        }
    }
}

@Composable
fun KhatmDuaContent(g: KhatmGroup, onBack: () -> Unit, onShare: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("khatm_dua")) {
        IkHeader("Khatm complete", g.title, onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                IkRing(1f, size = 96.dp, stroke = 8.dp) { Icon(Icons.Outlined.AutoStories, null, tint = IkGold, modifier = Modifier.size(38.dp)) }
                Spacer(Modifier.height(10.dp))
                Text("Alhamdulillah — all 30 paras read", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                if (g.dedication.isNotBlank()) Text(g.dedication, color = IkGold, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Text("May Allah accept it from everyone who took part.", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        }
        IkSection("Duas from the Qur'an", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        KHATM_DUAS.forEach { (ar, en, ref) ->
            IkCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                Text(ar, color = stitchText(), fontFamily = QuranArabicFont, fontSize = 22.sp, lineHeight = 40.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(en, color = stitchText(), fontSize = 13.sp, lineHeight = 19.sp)
                Text(ref, color = IkGoldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Text("There is no single fixed wording for a Khatm dua. Make these, and any dua from your heart, for the intention of this Khatm.",
            color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        IkButton("Share the good news", Modifier.padding(horizontal = 16.dp).fillMaxWidth(), icon = Icons.Default.Share, tag = "khatm_dua_share", onClick = onShare)
        Spacer(Modifier.height(40.dp))
    }
}

/** Qur'anic duas (Arabic from the Mushaf text; English renderings are NoorPro's own). */
internal val KHATM_DUAS = listOf(
    Triple("رَبَّنَا تَقَبَّلْ مِنَّآ ۖ إِنَّكَ أَنتَ ٱلسَّمِيعُ ٱلْعَلِيمُ", "Our Lord, accept this from us. You alone are the All-Hearing, the All-Knowing.", "Al-Baqarah 2:127"),
    Triple("رَبَّنَآ ءَاتِنَا فِى ٱلدُّنْيَا حَسَنَةً وَفِى ٱلْءَاخِرَةِ حَسَنَةً وَقِنَا عَذَابَ ٱلنَّارِ", "Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.", "Al-Baqarah 2:201"),
    Triple("رَّبِّ زِدْنِى عِلْمًا", "My Lord, increase me in knowledge.", "Ta-Ha 20:114"),
)
