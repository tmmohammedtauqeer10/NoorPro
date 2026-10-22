package com.noorpro.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.NoorAiMessage
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** One chat bubble. feedback: 1 = helpful, -1 = not helpful, 0 = none (stored locally only). */
data class NoorAiChatItem(
    val text: String,
    val isUser: Boolean,
    val time: Long = System.currentTimeMillis(),
    val feedback: Int = 0,
    val id: Long = System.nanoTime(),
)

/** Local-only chat history (SharedPreferences), kept per mode. */
internal object NoorAiChatStore {
    private const val PREFS = "noor_ai_chat"
    fun load(ctx: Context, mode: NoorAiMode): List<NoorAiChatItem> = runCatching {
        val arr = JSONArray(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("h_${mode.name}", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            NoorAiChatItem(o.getString("t"), o.getBoolean("u"), o.optLong("ts"), o.optInt("f"), o.optLong("id", i.toLong()))
        }
    }.getOrDefault(emptyList())

    fun save(ctx: Context, mode: NoorAiMode, items: List<NoorAiChatItem>) {
        val arr = JSONArray()
        items.takeLast(60).forEach { arr.put(JSONObject().put("t", it.text).put("u", it.isUser).put("ts", it.time).put("f", it.feedback).put("id", it.id)) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("h_${mode.name}", arr.toString()).apply()
    }

    fun tipDismissed(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("tip_dismissed", false)
    fun dismissTip(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("tip_dismissed", true).apply()
}

@Composable
fun AiFeaturesHubScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    var mode by remember { mutableStateOf(NoorAiMode.AskNoor) }
    var messages by remember { mutableStateOf(NoorAiChatStore.load(context, NoorAiMode.AskNoor)) }
    var input by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var revealId by remember { mutableStateOf<Long?>(null) }
    var showTip by remember { mutableStateOf(!NoorAiChatStore.tipDismissed(context)) }

    fun update(list: List<NoorAiChatItem>) { messages = list; NoorAiChatStore.save(context, mode, list) }

    fun ask(prompt: String, base: List<NoorAiChatItem>) {
        val history = base.dropLast(1).map { NoorAiMessage(it.text, it.isUser) }
        val askedMode = mode
        isTyping = true
        job = scope.launch {
            val answer = try {
                AiFeatureService.generateResponse(prompt, askedMode, history)
            } catch (e: kotlinx.coroutines.CancellationException) {
                isTyping = false; throw e
            } catch (_: Exception) {
                AiFeatureService.UNAVAILABLE_MESSAGE
            }
            if (askedMode == mode) {
                val reply = NoorAiChatItem(answer, false)
                revealId = reply.id
                update(base + reply)
            }
            isTyping = false
        }
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || isTyping) return
        val next = messages + NoorAiChatItem(clean, true)
        update(next)
        input = ""
        focusManager.clearFocus()
        ask(clean, next)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        NoorAiLaunch.pending?.let { q -> NoorAiLaunch.pending = null; if (mode == NoorAiMode.AskNoor) send(q) }
    }

    NoorAiChatContent(
        mode = mode,
        messages = messages,
        input = input,
        isTyping = isTyping,
        showTip = showTip,
        revealId = revealId,
        onBack = { viewModel.navigateTo(DeenScreen.DASHBOARD) },
        onInput = { input = it },
        onSend = { send(input) },
        onSuggestion = { send(it) },
        onStop = { job?.cancel(); isTyping = false },
        onModeChange = {
            if (it != mode) {
                job?.cancel(); isTyping = false
                mode = it
                messages = NoorAiChatStore.load(context, it)
            }
        },
        onClearChat = { job?.cancel(); isTyping = false; update(emptyList()) },
        onDismissTip = { showTip = false; NoorAiChatStore.dismissTip(context) },
        onCopy = { clipboard.setText(AnnotatedString(NoorAiMarkdown.parse(it.text).joinToString("\n") { b -> plainOf(b) })) },
        onShare = { item ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, NoorAiMarkdown.parse(item.text).joinToString("\n") { plainOf(it) })
            runCatching { context.startActivity(Intent.createChooser(send, "Share answer")) }
        },
        onRegenerate = { item ->
            if (!isTyping) {
                val idx = messages.indexOfFirst { it.id == item.id }
                val base = if (idx > 0) messages.subList(0, idx) else emptyList()
                val prompt = base.lastOrNull { it.isUser }?.text
                if (prompt != null) { update(base); ask(prompt, base) }
            }
        },
        onFeedback = { item, value ->
            update(messages.map { if (it.id == item.id) it.copy(feedback = if (it.feedback == value) 0 else value) else it })
        },
        onRevealDone = { revealId = null },
    )
}

internal fun plainOf(b: MdBlock): String = when (b) {
    is MdBlock.Heading -> NoorAiMarkdown.stripInline(b.text)
    is MdBlock.Paragraph -> NoorAiMarkdown.stripInline(b.text)
    is MdBlock.Bullet -> "• " + NoorAiMarkdown.stripInline(b.text)
    is MdBlock.Numbered -> "${b.number}. " + NoorAiMarkdown.stripInline(b.text)
    is MdBlock.Quote -> "“" + NoorAiMarkdown.stripInline(b.text) + "”"
    is MdBlock.Sources -> "Sources: " + b.refs.joinToString("; ")
    MdBlock.Rule -> ""
}

internal fun noorAiSuggestions(mode: NoorAiMode): List<String> = when (mode) {
    NoorAiMode.AskNoor -> listOf("How can I improve focus in salah?", "How do I build a daily Quran habit?", "What breaks wudu?", "How to make sincere tawbah?", "Rights of parents in Islam")
    NoorAiMode.DuaGenerator -> listOf("Dua for exam stress", "Dua for my parents' health", "Dua for forgiveness", "Dua before travelling", "Dua for anxiety")
    NoorAiMode.QuranHelper -> listOf("Lessons from Surah Al-Fatiha", "Explain Ayat al-Kursi simply", "What does the Quran say about patience?", "How to reflect on an ayah?")
    NoorAiMode.HadithExplainer -> listOf("Explain the hadith on intentions", "Hadith about smiling as charity", "How is a hadith graded?", "Hadith on kindness to neighbours")
    NoorAiMode.HajjUmrahGuide -> listOf("Umrah steps in order", "What do I do after tawaf?", "Packing list for Umrah", "Rules of ihram")
    NoorAiMode.AppGuide -> listOf("How do I upload a reel?", "How to change my username?", "How do group chats work?", "Where is the Qibla finder?")
}


/** A question handed over from the Home "Ask Noor AI" card; sent once when the chat opens. */
object NoorAiLaunch { var pending: String? = null }
