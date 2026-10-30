package com.noorpro.app.ui.islamic

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.halal.HalalTextFilter
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.community.CommunityException
import com.noorpro.app.islamic.community.CommunityReports
import com.noorpro.app.islamic.community.CommunityText
import com.noorpro.app.islamic.social.*
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

/* 1.1.6 Ask a Scholar UI (ask_scholar_enabled): library + search, my questions, saved, scholar inbox. */

data class ScholarUi(
    val library: List<ScholarQuestion> = emptyList(),
    val mine: List<ScholarQuestion> = emptyList(),
    val inbox: List<ScholarQuestion> = emptyList(),
    val saved: Set<String> = emptySet(),
    val scholar: Scholar? = null,
    val tab: Int = 0,
    val query: String = "",
    val category: ScholarCategory? = null,
    val loading: Boolean = false,
    val error: CommunityError? = null,
    val myUid: String? = null,
    val now: Long = System.currentTimeMillis(),
)

class ScholarActions(
    val onBack: () -> Unit = {},
    val onTab: (Int) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onCategory: (ScholarCategory?) -> Unit = {},
    val onAsk: () -> Unit = {},
    val onSave: (ScholarQuestion) -> Unit = {},
    val onReport: (ScholarQuestion) -> Unit = {},
    val onWithdraw: (ScholarQuestion) -> Unit = {},
    val onAnswer: (ScholarQuestion) -> Unit = {},
)

@Composable
fun AskScholarScreen(viewModel: DeenViewModel, backend: ScholarBackend = remember { FirestoreScholarBackend() }) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saves = remember { ScholarSaves(ctx) }
    var ui by remember { mutableStateOf(ScholarUi(saved = saves.ids(), myUid = backend.uid())) }
    var asking by remember { mutableStateOf(false) }
    var answering by remember { mutableStateOf<ScholarQuestion?>(null) }
    var reporting by remember { mutableStateOf<ScholarQuestion?>(null) }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun refresh() {
        if (!backend.available) { ui = ui.copy(error = CommunityError.OFFLINE_BACKEND); return }
        ui = ui.copy(loading = true)
        scope.launch {
            runCatching { backend.library() }.onSuccess { ui = ui.copy(library = it, error = null) }.onFailure { ui = ui.copy(error = CommunityError.from(it)) }
            if (backend.uid() != null) {
                val mine = runCatching { backend.mine() }.getOrDefault(emptyList())
                val scholar = runCatching { backend.me() }.getOrNull()
                val inbox = if (scholar != null) runCatching { backend.inbox() }.getOrDefault(emptyList()) else emptyList()
                ui = ui.copy(mine = mine, scholar = scholar, inbox = inbox, myUid = backend.uid())
                ScholarNotifier.markSeen(ctx, mine)
            }
            ui = ui.copy(loading = false)
        }
    }
    LaunchedEffect(Unit) { HalalTextFilter.ensureLoaded(ctx); refresh() }

    if (asking) {
        BackHandler { asking = false }
        AskQuestionContent(viewModel.userDisplayName, onBack = { asking = false }) { text, cat, anon, done ->
            scope.launch {
                runCatching { backend.ask(text, cat, anon, viewModel.userDisplayName) }
                    .onSuccess { q -> done(null); asking = false; ui = ui.copy(mine = listOf(q) + ui.mine, tab = 1); ScholarNotifier.ensureScheduled(ctx)
                        toast("Sent. You'll be notified when a scholar answers, in sha Allah.") }
                    .onFailure { e -> done((e as? CommunityException)?.detail?.ifBlank { null } ?: CommunityError.from(e).body) }
            }
        }
        return
    }
    answering?.let { q ->
        val s = ui.scholar ?: return@let
        BackHandler { answering = null }
        AnswerContent(q, s, onBack = { answering = null }) { text, done ->
            scope.launch {
                runCatching { backend.answer(q.id, text, s) }
                    .onSuccess { done(null); answering = null; ui = ui.copy(inbox = ui.inbox.filter { it.id != q.id },
                        library = listOf(q.copy(answered = true, answer = text.trim(), scholarUid = s.uid, scholarName = s.name, scholarTitle = s.title, answeredAt = System.currentTimeMillis())) + ui.library)
                        toast("JazakAllahu khairan. Your answer is published.") }
                    .onFailure { e -> done((e as? CommunityException)?.detail?.ifBlank { null } ?: CommunityError.from(e).body) }
            }
        }
        return
    }
    AskScholarContent(ui, ScholarActions(
        onBack = { viewModel.goBack() },
        onTab = { ui = ui.copy(tab = it) },
        onQuery = { ui = ui.copy(query = it) },
        onCategory = { ui = ui.copy(category = it) },
        onAsk = { if (!backend.available) toast(CommunityError.OFFLINE_BACKEND.body) else if (backend.uid() == null) toast(CommunityError.SIGN_IN.body) else asking = true },
        onSave = { q -> ui = ui.copy(saved = saves.toggle(q.id)) },
        onReport = { reporting = it },
        onWithdraw = { q -> scope.launch { runCatching { backend.withdraw(q.id) }.onSuccess { ui = ui.copy(mine = ui.mine.filter { it.id != q.id }) }.onFailure { toast(CommunityError.from(it).body) } } },
        onAnswer = { answering = it },
    ))
    reporting?.let { q ->
        IkReportDialog("answer", onDismiss = { reporting = null }) { reason ->
            reporting = null
            CommunityReports.report("scholar_answer", q.id, q.scholarUid, reason, q.text.take(120)) { ok -> toast(if (ok) "Thank you. Our moderators will review it." else "Couldn't send the report. Please try again.") }
        }
    }
}

@Composable
fun AskScholarContent(ui: ScholarUi, actions: ScholarActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("scholar_screen")) {
        IkHeader("Ask a Scholar", "Verified answers, gently given", actions.onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("ASK THE PEOPLE OF KNOWLEDGE", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Text("“So ask the people of knowledge, if you do not know.”", color = IkCream, fontFamily = LibreCaslon, fontSize = 17.sp, lineHeight = 23.sp, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(6.dp))
            Text("Surah An-Nahl 16:43", color = IkCream.copy(alpha = 0.7f), fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(onClick = actions.onAsk).padding(horizontal = 16.dp, vertical = 11.dp).testTag("scholar_ask_open"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ContactSupport, null, tint = IkGreenDeep, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Ask a question", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        val tabs = buildList {
            add("Library"); add("Mine${if (ui.mine.isNotEmpty()) " · ${ui.mine.size}" else ""}"); add("Saved${if (ui.saved.isNotEmpty()) " · ${ui.saved.size}" else ""}")
            if (ui.scholar != null) add("Inbox${if (ui.inbox.isNotEmpty()) " · ${ui.inbox.size}" else ""}")
        }
        IkSegments(tabs, ui.tab.coerceAtMost(tabs.lastIndex), Modifier.padding(horizontal = 16.dp), "scholar_tab", actions.onTab)
        ui.error?.let { IkNotice(it.title, if (it == CommunityError.OFFLINE_BACKEND) "Ask a Scholar isn't switched on yet. Please check back soon." else it.body,
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = it == CommunityError.OFFLINE_BACKEND || it == CommunityError.NETWORK, tag = "scholar_notice") }
        when (ui.tab) {
            0 -> {
                if (ui.error == null || ui.library.isNotEmpty()) {
                    SearchBox(ui.query, actions.onQuery)
                    IkPillRow(listOf<ScholarCategory?>(null) + ScholarCategory.entries, ui.category, { it?.label ?: "All" }, tag = { "scholar_cat_${it?.name ?: "ALL"}" }, onPick = actions.onCategory)
                    Spacer(Modifier.height(6.dp))
                    val list = ScholarLogic.search(ui.library, ui.query, ui.category)
                    if (list.isEmpty()) Empty(if (ui.loading) "Loading answers…" else if (ui.query.isNotBlank()) "No answers match “${ui.query.trim()}” yet. You can ask it — someone else may be wondering too."
                        else "No answers here yet. Ask the first question.", "scholar_empty")
                    list.forEach { QaCard(it, ui, actions) }
                }
            }
            1 -> {
                Spacer(Modifier.height(8.dp))
                if (ui.myUid == null) IkNotice(CommunityError.SIGN_IN.title, "Sign in to ask and to see your questions.", Modifier.padding(horizontal = 16.dp), tag = "scholar_mine_signin")
                else if (ui.mine.isEmpty()) Empty("Your questions appear here. Only you and verified scholars can see them until they're answered.", "scholar_mine_empty")
                ui.mine.forEach { q -> if (q.answered) QaCard(q, ui, actions, mine = true) else PendingCard(q, ui, actions) }
            }
            2 -> {
                Spacer(Modifier.height(8.dp))
                val saved = (ui.library + ui.mine).distinctBy { it.id }.filter { it.id in ui.saved && it.answered }
                if (saved.isEmpty()) Empty("Tap the bookmark on any answer to keep it here, on this phone.", "scholar_saved_empty")
                saved.forEach { QaCard(it, ui, actions) }
            }
            else -> {
                Spacer(Modifier.height(8.dp))
                ui.scholar?.let { s -> ScholarBanner(s) }
                if (ui.inbox.isEmpty()) Empty("No open questions. JazakAllahu khairan for your time.", "scholar_inbox_empty")
                ui.inbox.forEach { q -> InboxCard(q, ui, actions) }
            }
        }
        Disclaimer(Modifier.padding(16.dp))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Empty(text: String, tag: String) = Text(text, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp,
    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).testTag(tag))

@Composable
private fun SearchBox(q: String, onQuery: (String) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ikCard())
        .border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, null, tint = stitchMutedText(), modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (q.isEmpty()) Text("Search answers — e.g. travel prayer, zakat on gold", color = stitchMutedText(), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(q, { onQuery(it.take(80)) }, singleLine = true, textStyle = TextStyle(color = stitchText(), fontSize = 14.sp),
                cursorBrush = SolidColor(IkGreen), modifier = Modifier.fillMaxWidth().testTag("scholar_search"))
        }
        if (q.isNotEmpty()) Icon(Icons.Outlined.Close, "Clear", tint = stitchMutedText(), modifier = Modifier.size(18.dp).clickable { onQuery("") }.testTag("scholar_search_clear"))
    }
}

@Composable
internal fun ScholarBadge(name: String, title: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IkAvatar(name, size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.Verified, "Verified scholar", tint = IkGold, modifier = Modifier.size(15.dp))
            }
            Text(title.ifBlank { "Verified scholar" }, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QaCard(q: ScholarQuestion, ui: ScholarUi, actions: ScholarActions, mine: Boolean = false) {
    var open by remember(q.id) { mutableStateOf(false) }
    IkCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).testTag("scholar_qa_${q.id}"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkTag(q.category.label.uppercase())
            Spacer(Modifier.width(8.dp))
            Text("${if (mine) "Your question" else q.askedBy} · ${CommunityText.ago(q.answeredAt.takeIf { it > 0 } ?: q.createdAt, ui.now)}", color = stitchMutedText(), fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            val saved = q.id in ui.saved
            Box(Modifier.size(32.dp).clip(CircleShape).clickable { actions.onSave(q) }.testTag("scholar_save_${q.id}"), contentAlignment = Alignment.Center) {
                Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, if (saved) "Saved" else "Save", tint = if (saved) IkGoldDeep else stitchMutedText(), modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(q.text, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ikSoft()).padding(12.dp)) {
            ScholarBadge(q.scholarName, q.scholarTitle)
            Spacer(Modifier.height(8.dp))
            Text(q.answer, color = stitchText(), fontSize = 14.sp, lineHeight = 21.sp, maxLines = if (open) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("scholar_answer_${q.id}"))
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (q.answer.length > 260) Text(if (open) "Show less" else "Read full answer", color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.clickable { open = !open }.padding(vertical = 4.dp).testTag("scholar_more_${q.id}"))
                Spacer(Modifier.weight(1f))
                if (!mine) Text("Report", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.clickable { actions.onReport(q) }.padding(4.dp).testTag("scholar_report_${q.id}"))
            }
        }
    }
}

@Composable
private fun PendingCard(q: ScholarQuestion, ui: ScholarUi, actions: ScholarActions) {
    IkCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).testTag("scholar_pending_${q.id}"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkTag(q.category.label.uppercase())
            Spacer(Modifier.width(8.dp))
            Text("${if (q.anonymous) "Anonymous" else "With your name"} · ${CommunityText.ago(q.createdAt, ui.now)}", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(q.text, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.HourglassTop, null, tint = IkAmber, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp))
            Text("Waiting for a scholar", color = IkAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("Withdraw", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.clickable { actions.onWithdraw(q) }.padding(4.dp).testTag("scholar_withdraw_${q.id}"))
        }
    }
}

@Composable
private fun ScholarBanner(s: Scholar) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(IkGold.copy(alpha = 0.14f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        ScholarBadge(s.name, s.title, Modifier.weight(1f))
        Text("Answering as you", color = IkGoldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InboxCard(q: ScholarQuestion, ui: ScholarUi, actions: ScholarActions) {
    IkCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).testTag("scholar_inbox_${q.id}"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkTag(q.category.label.uppercase()); Spacer(Modifier.width(8.dp))
            Text("${q.askedBy} · ${CommunityText.ago(q.createdAt, ui.now)}", color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(q.text, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(10.dp))
        IkButton("Write an answer", Modifier.fillMaxWidth(), Icons.Outlined.EditNote, tag = "scholar_answer_open_${q.id}") { actions.onAnswer(q) }
    }
}

@Composable
private fun Disclaimer(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).padding(12.dp).testTag("scholar_disclaimer")) {
        Icon(Icons.Outlined.Info, null, tint = stitchMutedText(), modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Spacer(Modifier.width(8.dp))
        Text(ScholarLogic.DISCLAIMER, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
fun AskQuestionContent(displayName: String, onBack: () -> Unit, modifier: Modifier = Modifier, initialText: String = "", onSend: (String, ScholarCategory, Boolean, (String?) -> Unit) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    var cat by remember { mutableStateOf(ScholarCategory.SALAH) }
    var anon by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val check = remember(text) { if (text.isBlank()) null else HalalTextFilter.check(text) }
    val held = check != null && (check.action == HalalTextFilter.Action.BLOCK || check.action == HalalTextFilter.Action.QUEUE)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("scholar_ask")) {
        IkHeader("Ask a question", "Private until a scholar answers", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            IkSection("Topic")
        }
        Spacer(Modifier.height(6.dp))
        IkPillRow(ScholarCategory.entries, cat, { it.label }, tag = { "scholar_pick_${it.name}" }) { cat = it }
        Column(Modifier.padding(horizontal = 16.dp)) {
            IkSection("Your question", Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            IkField(text, { text = it; error = null }, "Describe your situation (no names, please)", Modifier.heightIn(min = 150.dp), tag = "scholar_text",
                singleLine = false, maxLen = ScholarLogic.MAX_Q)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                val hint = when {
                    held -> "Please remove links, offers or anything against the community guidelines."
                    error != null -> error!!
                    else -> ""
                }
                Text(hint, color = IkAmber, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f).testTag("scholar_hint"))
                Text("${text.length}/${ScholarLogic.MAX_Q}", color = stitchMutedText(), fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IkAvatar(if (anon) "" else displayName, size = 38.dp, anonymous = anon)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ask anonymously", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(if (anon) "Your name is never shown, even in the library" else "Shown as ${displayName.ifBlank { "your profile name" }}", color = stitchMutedText(), fontSize = 12.sp,
                        maxLines = 2, lineHeight = 16.sp)
                }
                Switch(anon, { anon = it }, Modifier.testTag("scholar_anon"), colors = SwitchDefaults.colors(checkedTrackColor = IkGreen))
            }
            Spacer(Modifier.height(10.dp))
            Text("Once answered, your question and the answer join the public library so others benefit too.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(14.dp))
            IkButton(if (busy) "Sending…" else "Send to scholars", Modifier.fillMaxWidth(), Icons.Outlined.Send, enabled = !busy && ScholarLogic.validateQuestion(text) == null && !held, tag = "scholar_send") {
                busy = true
                onSend(text.trim(), cat, anon) { err -> busy = false; error = err }
            }
            Spacer(Modifier.height(12.dp))
            Disclaimer()
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun AnswerContent(q: ScholarQuestion, scholar: Scholar, onBack: () -> Unit, modifier: Modifier = Modifier, initialText: String = "", onPublish: (String, (String?) -> Unit) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("scholar_answer_compose")) {
        IkHeader("Answer", q.category.label, onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            IkCard(padding = 14.dp) {
                Text(q.askedBy, color = stitchMutedText(), fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text(q.text, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(12.dp))
            ScholarBanner(scholar)
            Spacer(Modifier.height(8.dp))
            IkField(text, { text = it; error = null }, "Your answer, with evidence where you can", Modifier.heightIn(min = 220.dp), tag = "scholar_answer_text", singleLine = false, maxLen = ScholarLogic.MAX_A)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(error ?: "Published with your name and verified badge.", color = if (error != null) IkAmber else stitchMutedText(), fontSize = 11.sp, modifier = Modifier.weight(1f).testTag("scholar_answer_hint"))
                Text("${text.length}/${ScholarLogic.MAX_A}", color = stitchMutedText(), fontSize = 11.sp)
            }
            Spacer(Modifier.height(14.dp))
            IkButton(if (busy) "Publishing…" else "Publish answer", Modifier.fillMaxWidth(), Icons.Outlined.Verified, enabled = !busy && ScholarLogic.validateAnswer(text) == null, tag = "scholar_publish") {
                busy = true
                onPublish(text) { err -> busy = false; error = err }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
