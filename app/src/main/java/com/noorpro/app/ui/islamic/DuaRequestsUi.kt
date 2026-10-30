package com.noorpro.app.ui.islamic

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OutlinedFlag
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.halal.HalalTextFilter
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.community.CommunityReports
import com.noorpro.app.islamic.community.CommunityText
import com.noorpro.app.islamic.social.*
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

/* 1.1.6 Dua requests UI (dua_requests_enabled). Stateless contents + one wired screen. */

data class DuaFeedUi(
    val requests: List<DuaRequest> = emptyList(),
    val mine: List<DuaRequest> = emptyList(),
    val responded: Set<String> = emptySet(),   // "<id>/<ameen|prayed>"
    val category: DuaCategory? = null,
    val tab: Int = 0,
    val loading: Boolean = false,
    val error: CommunityError? = null,
    val myUid: String? = null,
)

class DuaActions(
    val onBack: () -> Unit = {},
    val onTab: (Int) -> Unit = {},
    val onCategory: (DuaCategory?) -> Unit = {},
    val onCompose: () -> Unit = {},
    val onRespond: (DuaRequest, DuaResponse, Boolean) -> Unit = { _, _, _ -> },
    val onReport: (DuaRequest) -> Unit = {},
    val onAnswered: (DuaRequest) -> Unit = {},
    val onDelete: (DuaRequest) -> Unit = {},
)

@Composable
fun DuaRequestsScreen(viewModel: DeenViewModel, backend: DuaBackend = remember { FirestoreDuaBackend() }) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf(DuaFeedUi(myUid = backend.uid())) }
    var composing by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf<DuaRequest?>(null) }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun refresh() {
        if (!backend.available) { ui = ui.copy(error = CommunityError.OFFLINE_BACKEND); return }
        ui = ui.copy(loading = true)
        scope.launch {
            runCatching { backend.recent() }
                .onSuccess { l -> ui = ui.copy(requests = l, loading = false, error = null) }
                .onFailure { ui = ui.copy(loading = false, error = CommunityError.from(it)) }
            if (backend.uid() != null) {
                val mine = runCatching { backend.get(backend.myRequestIds()) }.getOrDefault(emptyList())
                val resp = runCatching { backend.myResponses(ui.requests.take(30).map { it.id }) }.getOrDefault(emptySet())
                ui = ui.copy(mine = mine.sortedByDescending { it.createdAt }, responded = resp, myUid = backend.uid())
                DuaSeenStore(ctx).markSeen(mine)
            }
        }
    }
    LaunchedEffect(Unit) { HalalTextFilter.ensureLoaded(ctx); refresh() }

    if (composing) {
        BackHandler { composing = false }
        DuaComposeContent(viewModel.userDisplayName, onBack = { composing = false }) { text, cat, anon, done ->
            if (backend.uid() == null) { done(CommunityError.SIGN_IN.body); return@DuaComposeContent }
            scope.launch {
                runCatching { backend.post(text, cat, anon, viewModel.userDisplayName) }
                    .onSuccess { d ->
                        ui = ui.copy(requests = listOf(d) + ui.requests, mine = listOf(d) + ui.mine, tab = 0)
                        DuaNotifier.ensureScheduled(ctx); composing = false; toast("Your dua request is shared. May Allah grant it.")
                        done(null)
                    }
                    .onFailure { e -> done((e as? com.noorpro.app.islamic.community.CommunityException)?.detail ?: CommunityError.from(e).body) }
            }
        }
        return
    }
    DuaFeedContent(ui, DuaActions(
        onBack = { viewModel.goBack() },
        onTab = { ui = ui.copy(tab = it) },
        onCategory = { ui = ui.copy(category = it) },
        onCompose = { if (!backend.available) toast(CommunityError.OFFLINE_BACKEND.body) else if (backend.uid() == null) toast(CommunityError.SIGN_IN.body) else composing = true },
        onRespond = { d, r, on ->
            val key = "${d.id}/${r.key}"
            fun apply(state: Boolean) {
                ui = ui.copy(requests = ui.requests.map { if (it.id == d.id) DuaLogic.toggle(it, r, state) else it },
                    responded = if (state) ui.responded + key else ui.responded - key)
            }
            if (backend.uid() == null) toast(CommunityError.SIGN_IN.body) else {
                apply(on)
                scope.launch { runCatching { backend.respond(d.id, r, on) }.onFailure { apply(!on); toast(CommunityError.from(it).body) } }
            }
        },
        onReport = { reporting = it },
        onAnswered = { d -> scope.launch { runCatching { backend.markAnswered(d.id) }.onSuccess {
            ui = ui.copy(mine = ui.mine.map { if (it.id == d.id) it.copy(answered = true) else it }, requests = ui.requests.map { if (it.id == d.id) it.copy(answered = true) else it }); toast("Alhamdulillah") } } },
        onDelete = { d -> scope.launch { runCatching { backend.delete(d.id) }.onSuccess {
            ui = ui.copy(mine = ui.mine.filter { it.id != d.id }, requests = ui.requests.filter { it.id != d.id }) } } },
    ))
    reporting?.let { d ->
        IkReportDialog("dua request", onDismiss = { reporting = null }) { reason ->
            reporting = null
            CommunityReports.report("dua_request", d.id, d.authorUid, reason, d.text.take(120)) { ok -> toast(if (ok) "Thank you. Our moderators will review it." else "Couldn't send the report. Please try again.") }
        }
    }
}

@Composable
fun DuaFeedContent(ui: DuaFeedUi, actions: DuaActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("dua_feed")) {
        IkHeader("Dua Requests", "Ask the Ummah to remember you", actions.onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("THE ANGELS SAY AMEEN", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Text("“A Muslim's dua for his brother in his absence is answered; an angel beside him says: Ameen, and for you the same.”",
                color = IkCream, fontFamily = LibreCaslon, fontSize = 16.sp, lineHeight = 22.sp, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(6.dp))
            Text("Sahih Muslim 2733", color = IkCream.copy(alpha = 0.7f), fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(onClick = actions.onCompose).padding(horizontal = 16.dp, vertical = 11.dp).testTag("dua_compose_open"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AddComment, null, tint = IkGreenDeep, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Ask for dua", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        IkSegments(listOf("Community", "My requests${if (ui.mine.isNotEmpty()) " · ${ui.mine.size}" else ""}"), ui.tab, Modifier.padding(horizontal = 16.dp), "dua_tab", actions.onTab)
        ui.error?.let { IkNotice(it.title, if (it == CommunityError.OFFLINE_BACKEND) "Dua requests aren't switched on yet. Please check back soon." else it.body,
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = it == CommunityError.OFFLINE_BACKEND || it == CommunityError.NETWORK, tag = "dua_notice") }
        if (ui.tab == 0) {
            if (ui.error == null || ui.requests.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            IkPillRow(listOf<DuaCategory?>(null) + DuaCategory.entries, ui.category, { it?.label ?: "All" }, tag = { "dua_cat_${it?.name ?: "ALL"}" }, onPick = actions.onCategory)
            }
            Spacer(Modifier.height(6.dp))
            val list = DuaLogic.visible(ui.requests, ui.category)
            if (list.isEmpty() && ui.error == null) Text(if (ui.loading) "Loading dua requests…" else "No requests here yet. When someone asks, you'll be able to say Ameen.",
                color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).testTag("dua_empty"))
            list.forEach { d -> DuaCard(d, ui, actions, mine = false) }
        } else {
            Spacer(Modifier.height(8.dp))
            if (ui.myUid == null) IkNotice(CommunityError.SIGN_IN.title, "Sign in to see who made dua for your requests.", Modifier.padding(horizontal = 16.dp), tag = "dua_mine_signin")
            else if (ui.mine.isEmpty()) Text("You haven't asked for dua yet. Your requests and the Ameens they gather appear here.",
                color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).testTag("dua_mine_empty"))
            ui.mine.forEach { d -> DuaCard(d, ui, actions, mine = true) }
        }
        Spacer(Modifier.height(10.dp))
        Text("Please don't share private details. Anonymous requests never show your name, and reports go to NoorPro moderators.",
            color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun DuaCard(d: DuaRequest, ui: DuaFeedUi, actions: DuaActions, mine: Boolean) {
    IkCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).testTag("dua_card_${d.id}"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkAvatar(d.shownName, size = 36.dp, anonymous = d.anonymous)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.shownName, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (d.anonymous) { Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.VisibilityOff, "Anonymous", tint = stitchMutedText(), modifier = Modifier.size(13.dp)) }
                }
                Text("${d.category.label} · ${CommunityText.ago(d.createdAt)}", color = stitchMutedText(), fontSize = 11.sp, maxLines = 1)
            }
            if (d.answered) IkTag("ANSWERED", bg = IkGold.copy(alpha = 0.18f), fg = IkGoldDeep)
            if (!mine) Box(Modifier.size(32.dp).clip(RoundedCornerShape(50)).clickable { actions.onReport(d) }.testTag("dua_report_${d.id}"), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.OutlinedFlag, "Report", tint = stitchMutedText(), modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(d.text, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(10.dp))
        Text(DuaLogic.countLine(d), color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("dua_count_${d.id}"))
        Spacer(Modifier.height(8.dp))
        if (!mine) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DuaResponse.entries.forEach { r ->
                val on = "${d.id}/${r.key}" in ui.responded
                Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) IkGreen else ikSoft())
                    .clickable { actions.onRespond(d, r, !on) }.testTag("dua_${r.key}_${d.id}"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(if (on) (if (r == DuaResponse.AMEEN) Icons.Filled.Favorite else Icons.Outlined.CheckCircle) else (if (r == DuaResponse.AMEEN) Icons.Outlined.FavoriteBorder else Icons.Outlined.CheckCircle),
                        null, tint = if (on) Color.White else ikAccentText(), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(r.label, color = if (on) Color.White else ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!d.answered) IkButton("Mark answered", Modifier.weight(1f), Icons.Outlined.CheckCircle, primary = false, tag = "dua_answered_${d.id}") { actions.onAnswered(d) }
            IkButton("Remove", Modifier.weight(1f), Icons.Outlined.Delete, primary = false, tag = "dua_delete_${d.id}") { actions.onDelete(d) }
        }
    }
}

@Composable
fun DuaComposeContent(displayName: String, onBack: () -> Unit, modifier: Modifier = Modifier, initialText: String = "", initialAnon: Boolean = false,
                      onPost: (String, DuaCategory, Boolean, (String?) -> Unit) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    var cat by remember { mutableStateOf(DuaCategory.HEALTH) }
    var anon by remember { mutableStateOf(initialAnon) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Dua requests are public straight away, so anything Halal Guard would hold for review must be edited first.
    val check = remember(text) { if (text.isBlank()) null else HalalTextFilter.check(text) }
    val held = check != null && (check.action == HalalTextFilter.Action.BLOCK || check.action == HalalTextFilter.Action.QUEUE)
    val hint = when {
        check == null || check.action == HalalTextFilter.Action.ALLOW -> ""
        check.action == HalalTextFilter.Action.QUEUE -> "This looks like it may contain a possible scam. Please remove links, offers or money requests."
        else -> check.message
    }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("dua_compose")) {
        IkHeader("Ask for dua", "Shared with the NoorPro Ummah", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            IkSection("What should we pray for?")
            Spacer(Modifier.height(6.dp))
            IkField(text, { text = it; error = null }, "Your dua request", Modifier.heightIn(min = 130.dp), tag = "dua_text", singleLine = false, maxLen = DuaLogic.MAX_TEXT)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(hint.ifBlank { error ?: "" }, color = if (hint.isNotBlank() || error != null) IkAmber else stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f).testTag("dua_hint"))
                Text("${text.length}/${DuaLogic.MAX_TEXT}", color = stitchMutedText(), fontSize = 11.sp)
            }
            IkSection("Category", Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(6.dp))
        IkPillRow(DuaCategory.entries, cat, { it.label }, tag = { "dua_pick_${it.name}" }) { cat = it }
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IkAvatar(if (anon) "" else displayName, size = 38.dp, anonymous = anon)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Post anonymously", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(if (anon) "Shown as “${CommunityText.ANON_NAME}”" else "Shown as ${displayName.ifBlank { "your profile name" }}", color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Switch(anon, { anon = it }, Modifier.testTag("dua_anon"), colors = SwitchDefaults.colors(checkedTrackColor = IkGreen))
            }
            Spacer(Modifier.height(10.dp))
            Text("You'll get one gentle notification when people make dua for you — never one per tap.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(14.dp))
            val invalid = DuaLogic.validate(text)
            IkButton(if (busy) "Sharing…" else "Share request", Modifier.fillMaxWidth(), Icons.Outlined.AddComment, enabled = !busy && invalid == null && !held, tag = "dua_post") {
                busy = true
                onPost(text, cat, anon) { err -> busy = false; error = err }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
