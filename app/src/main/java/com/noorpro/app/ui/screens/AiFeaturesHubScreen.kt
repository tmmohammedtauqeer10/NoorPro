package com.noorpro.app.ui.screens

import android.app.Activity
import android.content.Context
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

/** A saved Noor AI conversation (local only). */
data class NoorAiConversation(val id: Long, val mode: NoorAiMode, val title: String, val updated: Long)

/** Local-only chat history (SharedPreferences): many conversations, each with its own messages. */
internal object NoorAiChatStore {
    private const val PREFS = "noor_ai_chat"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun parseItems(json: String?): List<NoorAiChatItem> = runCatching {
        val arr = JSONArray(json ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            NoorAiChatItem(o.getString("t"), o.getBoolean("u"), o.optLong("ts"), o.optInt("f"), o.optLong("id", i.toLong()))
        }
    }.getOrDefault(emptyList())

    private fun itemsJson(items: List<NoorAiChatItem>): String {
        val arr = JSONArray()
        items.takeLast(80).forEach { arr.put(JSONObject().put("t", it.text).put("u", it.isUser).put("ts", it.time).put("f", it.feedback).put("id", it.id)) }
        return arr.toString()
    }

    /** One-time move of the old per-mode history into conversations. */
    private fun migrate(ctx: Context) {
        val p = prefs(ctx)
        if (p.getBoolean("migrated_v79", false)) return
        val convs = JSONArray()
        NoorAiMode.values().forEach { m ->
            val items = parseItems(p.getString("h_${m.name}", null))
            if (items.isNotEmpty()) {
                val id = items.first().time.takeIf { it > 0 } ?: System.currentTimeMillis()
                p.edit().putString("c_$id", itemsJson(items)).putLong("cur_${m.name}", id).apply()
                convs.put(JSONObject().put("id", id).put("m", m.name).put("t", titleOf(items)).put("u", items.last().time))
            }
        }
        p.edit().putString("convs", convs.toString()).putBoolean("migrated_v79", true).apply()
    }

    fun titleOf(items: List<NoorAiChatItem>): String =
        items.firstOrNull { it.isUser }?.text?.replace("\n", " ")?.trim()?.let { if (it.length > 48) it.take(47) + "…" else it } ?: "New chat"

    fun conversations(ctx: Context): List<NoorAiConversation> {
        migrate(ctx)
        return runCatching {
            val arr = JSONArray(prefs(ctx).getString("convs", "[]"))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val mode = runCatching { NoorAiMode.valueOf(o.getString("m")) }.getOrNull() ?: return@mapNotNull null
                NoorAiConversation(o.getLong("id"), mode, o.optString("t", "New chat"), o.optLong("u"))
            }.sortedByDescending { it.updated }
        }.getOrDefault(emptyList())
    }

    private fun writeConvs(ctx: Context, list: List<NoorAiConversation>) {
        val arr = JSONArray()
        list.sortedByDescending { it.updated }.take(100).forEach { arr.put(JSONObject().put("id", it.id).put("m", it.mode.name).put("t", it.title).put("u", it.updated)) }
        prefs(ctx).edit().putString("convs", arr.toString()).apply()
    }

    fun currentId(ctx: Context, mode: NoorAiMode): Long? { migrate(ctx); return prefs(ctx).getLong("cur_${mode.name}", 0L).takeIf { it != 0L } }
    fun setCurrent(ctx: Context, mode: NoorAiMode, id: Long?) = prefs(ctx).edit().putLong("cur_${mode.name}", id ?: 0L).apply()

    fun load(ctx: Context, id: Long?): List<NoorAiChatItem> = if (id == null) emptyList() else parseItems(prefs(ctx).getString("c_$id", null))

    /** Saves messages; creates the conversation entry the first time it has content. */
    fun save(ctx: Context, mode: NoorAiMode, id: Long, items: List<NoorAiChatItem>) {
        prefs(ctx).edit().putString("c_$id", itemsJson(items)).apply()
        val rest = conversations(ctx).filterNot { it.id == id }
        if (items.isEmpty()) { writeConvs(ctx, rest); return }
        writeConvs(ctx, rest + NoorAiConversation(id, mode, titleOf(items), System.currentTimeMillis()))
        setCurrent(ctx, mode, id)
    }

    fun delete(ctx: Context, id: Long) {
        prefs(ctx).edit().remove("c_$id").apply()
        writeConvs(ctx, conversations(ctx).filterNot { it.id == id })
        NoorAiMode.values().forEach { if (currentId(ctx, it) == id) setCurrent(ctx, it, null) }
    }

    fun language(ctx: Context): String = prefs(ctx).getString("lang", "en") ?: "en"
    fun setLanguage(ctx: Context, code: String) = prefs(ctx).edit().putString("lang", code).apply()

    fun tipDismissed(ctx: Context) = prefs(ctx).getBoolean("tip_dismissed", false)
    fun dismissTip(ctx: Context) = prefs(ctx).edit().putBoolean("tip_dismissed", true).apply()
}

/** Answer languages offered in Noor AI (roadmap: Urdu, Hindi, Bengali answers). */
internal val NOOR_AI_LANGUAGES = listOf("en" to "English", "ur" to "اردو Urdu", "hi" to "हिन्दी Hindi", "bn" to "বাংলা Bengali", "ar" to "العربية Arabic")
internal fun speechTag(code: String) = when (code) { "ur" -> "ur-PK"; "hi" -> "hi-IN"; "bn" -> "bn-BD"; "ar" -> "ar-SA"; else -> "en-IN" }

/** Daily Islamic tip, rotated by day of year (bundled, offline). */
data class NoorDailyTip(val text: String, val ref: String)
internal val NOOR_DAILY_TIPS = listOf(
    NoorDailyTip("Actions are judged by intentions — renew your niyyah before work, study and sleep.", "Sahih al-Bukhari 1"),
    NoorDailyTip("Smiling at your brother is charity.", "Jami at-Tirmidhi 1956"),
    NoorDailyTip("The most beloved deeds to Allah are those done consistently, even if small.", "Sahih al-Bukhari 6464"),
    NoorDailyTip("Say SubhanAllahi wa bihamdihi 100 times; your sins are forgiven even if like the foam of the sea.", "Sahih al-Bukhari 6405"),
    NoorDailyTip("Verily, with hardship comes ease.", "Qur'an 94:6"),
    NoorDailyTip("Whoever believes in Allah and the Last Day, let him speak good or remain silent.", "Sahih al-Bukhari 6018"),
    NoorDailyTip("Recite Ayat al-Kursi after every fard salah.", "Sunan an-Nasa'i al-Kubra 9928"),
    NoorDailyTip("Remember Me; I will remember you.", "Qur'an 2:152"),
    NoorDailyTip("None of you truly believes until he loves for his brother what he loves for himself.", "Sahih al-Bukhari 13"),
    NoorDailyTip("Read Surah al-Kahf on Friday; it gives light between the two Fridays.", "Al-Mustadrak 3392"),
    NoorDailyTip("The strong one is not the wrestler, but the one who controls himself when angry.", "Sahih al-Bukhari 6114"),
    NoorDailyTip("Allah does not burden a soul beyond what it can bear.", "Qur'an 2:286"),
    NoorDailyTip("Two blessings many people lose: health and free time.", "Sahih al-Bukhari 6412"),
    NoorDailyTip("Make your tongue moist with the remembrance of Allah.", "Jami at-Tirmidhi 3375"),
)
internal fun noorDailyTip(dayOfYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)) =
    NOOR_DAILY_TIPS[Math.floorMod(dayOfYear, NOOR_DAILY_TIPS.size)]

@Composable
fun AiFeaturesHubScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    var mode by remember { mutableStateOf(NoorAiMode.AskNoor) }
    var convId by remember { mutableStateOf(NoorAiChatStore.currentId(context, NoorAiMode.AskNoor) ?: System.currentTimeMillis()) }
    var messages by remember { mutableStateOf(NoorAiChatStore.load(context, convId)) }
    var conversations by remember { mutableStateOf(NoorAiChatStore.conversations(context)) }
    var language by remember { mutableStateOf(NoorAiChatStore.language(context)) }
    var input by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var revealId by remember { mutableStateOf<Long?>(null) }
    var showTip by remember { mutableStateOf(!NoorAiChatStore.tipDismissed(context)) }
    AiFeatureService.answerLanguage = language

    fun update(list: List<NoorAiChatItem>) {
        messages = list
        NoorAiChatStore.save(context, mode, convId, list)
        conversations = NoorAiChatStore.conversations(context)
    }

    fun ask(prompt: String, base: List<NoorAiChatItem>) {
        val history = base.dropLast(1).map { NoorAiMessage(AiFeatureService.splitFollowups(it.text).first, it.isUser) }
        val askedMode = mode
        val askedConv = convId
        isTyping = true
        job = scope.launch {
            val answer = try {
                AiFeatureService.generateResponse(prompt, askedMode, history)
            } catch (e: kotlinx.coroutines.CancellationException) {
                isTyping = false; throw e
            } catch (_: Exception) {
                AiFeatureService.UNAVAILABLE_MESSAGE
            }
            if (askedMode == mode && askedConv == convId) {
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

    fun newChat() {
        job?.cancel(); isTyping = false; revealId = null
        convId = System.currentTimeMillis()
        messages = emptyList()
        NoorAiChatStore.setCurrent(context, mode, null)
    }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val heard = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (r.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) send(heard)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        NoorAiLaunch.pending?.let { q ->
            NoorAiLaunch.pending = null
            if (mode == NoorAiMode.AskNoor) { if (messages.isNotEmpty()) newChat(); send(q) }
        }
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
                convId = NoorAiChatStore.currentId(context, it) ?: System.currentTimeMillis()
                messages = NoorAiChatStore.load(context, convId)
            }
        },
        onClearChat = { job?.cancel(); isTyping = false; update(emptyList()) },
        onDismissTip = { showTip = false; NoorAiChatStore.dismissTip(context) },
        onCopy = { clipboard.setText(AnnotatedString(NoorAiMarkdown.parse(it.text).joinToString("\n") { b -> plainOf(b) })) },
        onShare = { item ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, NoorAiMarkdown.parse(item.text).joinToString("\n") { plainOf(it) } + "\n\n— via Noor AI (Noor Pro)")
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
        language = language,
        onLanguage = { language = it; NoorAiChatStore.setLanguage(context, it); AiFeatureService.answerLanguage = it },
        conversations = conversations,
        currentConversation = convId,
        onNewChat = { newChat() },
        onOpenConversation = { c ->
            job?.cancel(); isTyping = false; revealId = null
            mode = c.mode; convId = c.id
            messages = NoorAiChatStore.load(context, c.id)
            NoorAiChatStore.setCurrent(context, c.mode, c.id)
        },
        onDeleteConversation = { c ->
            NoorAiChatStore.delete(context, c.id)
            conversations = NoorAiChatStore.conversations(context)
            if (c.id == convId) newChat()
        },
        onVoice = {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechTag(language))
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Noor AI")
            runCatching { voice.launch(i) }.onFailure {
                android.widget.Toast.makeText(context, "Voice input isn't available on this phone.", android.widget.Toast.LENGTH_SHORT).show()
            }
        },
        dailyTip = noorDailyTip(),
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
    is MdBlock.Table -> (listOf(b.header) + b.rows).joinToString("\n") { it.joinToString(" | ") { c -> NoorAiMarkdown.stripInline(c) } }
    is MdBlock.Code -> b.text
    is MdBlock.Evidence -> b.title + "\n" + b.blocks.joinToString("\n") { plainOf(it) }
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
