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
import com.noorpro.app.data.AssistantEvent
import com.noorpro.app.data.AssistantState
import com.noorpro.app.data.reduceAssistant
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
data class NoorAiConversation(
    val id: Long, val mode: NoorAiMode, val title: String, val updated: Long,
    /** 1.1.4: pinned chats stay on top; customTitle = renamed by the user (not overwritten by auto-title). */
    val pinned: Boolean = false, val customTitle: Boolean = false,
)

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
                NoorAiConversation(o.getLong("id"), mode, o.optString("t", "New chat"), o.optLong("u"), o.optBoolean("p"), o.optBoolean("ct"))
            }.sortedWith(compareByDescending<NoorAiConversation> { it.pinned }.thenByDescending { it.updated })
        }.getOrDefault(emptyList())
    }

    private fun writeConvs(ctx: Context, list: List<NoorAiConversation>) {
        val arr = JSONArray()
        list.sortedWith(compareByDescending<NoorAiConversation> { it.pinned }.thenByDescending { it.updated }).take(100)
            .forEach { arr.put(JSONObject().put("id", it.id).put("m", it.mode.name).put("t", it.title).put("u", it.updated).put("p", it.pinned).put("ct", it.customTitle)) }
        prefs(ctx).edit().putString("convs", arr.toString()).apply()
    }

    fun currentId(ctx: Context, mode: NoorAiMode): Long? { migrate(ctx); return prefs(ctx).getLong("cur_${mode.name}", 0L).takeIf { it != 0L } }
    fun setCurrent(ctx: Context, mode: NoorAiMode, id: Long?) = prefs(ctx).edit().putLong("cur_${mode.name}", id ?: 0L).apply()

    fun load(ctx: Context, id: Long?): List<NoorAiChatItem> = if (id == null) emptyList() else parseItems(prefs(ctx).getString("c_$id", null))

    /** Saves messages; creates the conversation entry the first time it has content. */
    fun save(ctx: Context, mode: NoorAiMode, id: Long, items: List<NoorAiChatItem>) {
        prefs(ctx).edit().putString("c_$id", itemsJson(items)).apply()
        val all = conversations(ctx)
        val old = all.firstOrNull { it.id == id }
        val rest = all.filterNot { it.id == id }
        if (items.isEmpty()) { writeConvs(ctx, rest); return }
        val title = if (old?.customTitle == true) old.title else titleOf(items)
        writeConvs(ctx, rest + NoorAiConversation(id, mode, title, System.currentTimeMillis(), old?.pinned ?: false, old?.customTitle ?: false))
        setCurrent(ctx, mode, id)
    }

    fun delete(ctx: Context, id: Long) {
        prefs(ctx).edit().remove("c_$id").apply()
        writeConvs(ctx, conversations(ctx).filterNot { it.id == id })
        NoorAiMode.values().forEach { if (currentId(ctx, it) == id) setCurrent(ctx, it, null) }
    }

    fun rename(ctx: Context, id: Long, title: String) {
        val clean = title.replace("\n", " ").trim().take(60)
        writeConvs(ctx, conversations(ctx).map { if (it.id == id) (if (clean.isBlank()) it.copy(title = titleOf(load(ctx, id)), customTitle = false) else it.copy(title = clean, customTitle = true)) else it })
    }

    fun setPinned(ctx: Context, id: Long, pinned: Boolean) =
        writeConvs(ctx, conversations(ctx).map { if (it.id == id) it.copy(pinned = pinned) else it })

    /** Title or message text match (case-insensitive); pinned first, then newest. */
    fun search(ctx: Context, query: String): List<NoorAiConversation> {
        val q = query.trim()
        val all = conversations(ctx)
        if (q.isEmpty()) return all
        return all.filter { c -> c.title.contains(q, true) || load(ctx, c.id).any { it.text.contains(q, true) } }
    }

    fun autoRead(ctx: Context) = prefs(ctx).getBoolean("auto_read", false)
    fun setAutoRead(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("auto_read", on).apply()
    fun voiceLanguage(ctx: Context): String = prefs(ctx).getString("voice_lang", null) ?: language(ctx).takeIf { l -> NOOR_VOICE_LANGUAGES.any { it.first == l } } ?: "en"
    fun setVoiceLanguage(ctx: Context, code: String) = prefs(ctx).edit().putString("voice_lang", code).apply()

    /**
     * 1.2.0 (113): answers default to "auto" (reply in the language of the question). A fixed language
     * chosen before 113 is kept, except the old implicit "en" default, which becomes auto.
     */
    fun language(ctx: Context): String = prefs(ctx).getString("lang2", null)
        ?: prefs(ctx).getString("lang", null)?.takeIf { it != "en" && NOOR_AI_LANGUAGES.any { l -> l.first == it } }
        ?: com.noorpro.app.data.NoorAiLanguage.AUTO
    fun setLanguage(ctx: Context, code: String) = prefs(ctx).edit().putString("lang2", code).apply()

    /** 1.2.0 (114): fiqh madhhab (default Hanafi), answer depth (default Balanced) and voice hands-free mode. */
    fun madhhab(ctx: Context): String = com.noorpro.app.data.NoorAiOptions.normMadhhab(prefs(ctx).getString("madhhab", null))
    fun setMadhhab(ctx: Context, code: String) = prefs(ctx).edit().putString("madhhab", com.noorpro.app.data.NoorAiOptions.normMadhhab(code)).apply()
    fun tier(ctx: Context): String = com.noorpro.app.data.NoorAiOptions.normTier(prefs(ctx).getString("tier", null))
    fun setTier(ctx: Context, code: String) = prefs(ctx).edit().putString("tier", com.noorpro.app.data.NoorAiOptions.normTier(code)).apply()
    fun handsFree(ctx: Context) = prefs(ctx).getBoolean("hands_free", false)
    fun setHandsFree(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("hands_free", on).apply()

    fun tipDismissed(ctx: Context) = prefs(ctx).getBoolean("tip_dismissed", false)
    fun dismissTip(ctx: Context) = prefs(ctx).edit().putBoolean("tip_dismissed", true).apply()
}

/** Answer languages offered in Noor AI (roadmap: Urdu, Hindi, Bengali answers). */
internal val NOOR_AI_LANGUAGES = listOf(
    com.noorpro.app.data.NoorAiLanguage.AUTO to "Auto · same as my question",
    "en" to "English", "ur" to "اردو Urdu", "ur-Latn" to "Roman Urdu", "hi" to "हिन्दी Hindi", "bn" to "বাংলা Bengali", "ar" to "العربية Arabic",
)
internal fun speechTag(code: String) = when (code) { "ur" -> "ur-PK"; "hi" -> "hi-IN"; "bn" -> "bn-BD"; "ar" -> "ar-SA"; else -> "en-IN" }

/** Language an answer is in: its stored metadata, else the chosen language, else detected from the text. */
internal fun answerLanguageOf(text: String, setting: String): String =
    com.noorpro.app.data.AiFeatureService.parseMeta(text).language
        ?: setting.takeIf { it != com.noorpro.app.data.NoorAiLanguage.AUTO }
        ?: com.noorpro.app.data.NoorAiLanguage.detect(text)

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
    var messages by remember { mutableStateOf(emptyList<NoorAiChatItem>()) }
    var conversations by remember { mutableStateOf(emptyList<NoorAiConversation>()) }
    var language by remember { mutableStateOf(NoorAiChatStore.language(context)) }
    var input by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var revealId by remember { mutableStateOf<Long?>(null) }
    var showTip by remember { mutableStateOf(!NoorAiChatStore.tipDismissed(context)) }
    // 1.2.0 (114): madhhab, answer depth (Fast / Balanced / Research) and research progress
    var madhhab by remember { mutableStateOf(NoorAiChatStore.madhhab(context)) }
    var tier by remember { mutableStateOf(NoorAiChatStore.tier(context)) }
    var researchStages by remember { mutableStateOf(emptyList<com.noorpro.app.data.NoorResearchStage>()) }
    var showImageStudio by remember { mutableStateOf(false) }
    AiFeatureService.answerLanguage = language
    AiFeatureService.madhhab = madhhab
    AiFeatureService.tier = tier
    // 1.1.4 voice + spoken answers; 114 natural cloud voice and voice assistant
    var voice by remember { mutableStateOf(NoorVoiceState(language = NoorAiChatStore.voiceLanguage(context))) }
    var speakingId by remember { mutableStateOf<Long?>(null) }
    var autoRead by remember { mutableStateOf(NoorAiChatStore.autoRead(context)) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val voiceInput = remember { NoorVoiceInput(context) { e -> mainHandler.post { voice = reduceVoice(voice, e) } } }
    val deviceSpeaker = remember { NoorAiSpeaker(context) { id -> mainHandler.post { speakingId = id } } }
    val speaker = remember { NoorCloudSpeaker(context, deviceSpeaker) { id -> mainHandler.post { speakingId = id } } }
    val recorder = remember { NoorVoiceRecorder(context) }
    var assistantOpen by remember { mutableStateOf(false) }
    var assistant by remember { mutableStateOf(AssistantState(handsFree = NoorAiChatStore.handsFree(context))) }
    var assistantJob by remember { mutableStateOf<Job?>(null) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { voiceInput.stop(); recorder.release(); speaker.release(); deviceSpeaker.shutdown() }
    }

    suspend fun refreshConversations() { conversations = NoorAiHistory.conversations(context) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        NoorAiHistory.migrate(context)
        val loaded = NoorAiHistory.load(context, convId)
        if (messages.isEmpty()) messages = loaded
        refreshConversations()
    }

    fun speakItem(item: NoorAiChatItem, onDone: (() -> Unit)? = null) {
        val plain = NoorAiMarkdown.parse(AiFeatureService.splitFollowups(item.text).first).joinToString("\n") { plainOf(it) }
        speaker.speak(item.id, plain, answerLanguageOf(item.text, language).substringBefore('-')) { mainHandler.post { onDone?.invoke() } }
    }

    fun update(list: List<NoorAiChatItem>) {
        messages = list
        val m = mode; val id = convId
        scope.launch { NoorAiHistory.save(context, m, id, list); refreshConversations() }
    }

    /** Asks Noor AI. [voiceLang] / [voiceTurn] are set by the voice assistant (answer in the spoken language, short and speakable). */
    fun ask(prompt: String, base: List<NoorAiChatItem>, voiceLang: String? = null, onAnswer: ((NoorAiChatItem?) -> Unit)? = null) {
        val history = base.dropLast(1).map { NoorAiMessage(AiFeatureService.splitFollowups(it.text).first, it.isUser) }
        val askedMode = mode
        val askedConv = convId
        val askedTier = if (voiceLang != null && tier == com.noorpro.app.data.NoorAiOptions.RESEARCH) com.noorpro.app.data.NoorAiOptions.BALANCED
            else if (voiceLang != null) com.noorpro.app.data.NoorAiOptions.FAST else tier
        isTyping = true
        researchStages = emptyList()
        job = scope.launch {
            val answer = try {
                AiFeatureService.generateResponse(
                    prompt, askedMode, history, tier = askedTier,
                    language = voiceLang ?: language,
                    context = if (voiceLang != null) com.noorpro.app.data.NoorAiContext(note = "The user asked by voice and will hear the answer read aloud. Reply in short, natural spoken sentences in the same language, without tables, headings or markdown.") else null,
                    onStage = { st -> mainHandler.post { if (askedConv == convId) researchStages = researchStages + st } },
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                isTyping = false; researchStages = emptyList(); onAnswer?.invoke(null); throw e
            } catch (_: Exception) {
                com.noorpro.app.data.NoorAiLanguage.unavailable(com.noorpro.app.data.NoorAiLanguage.resolve(voiceLang ?: language, prompt))
            }
            var reply: NoorAiChatItem? = null
            if (askedMode == mode && askedConv == convId) {
                reply = NoorAiChatItem(answer, false)
                revealId = reply.id
                update(base + reply)
                if (voiceLang == null && autoRead && !AiFeatureService.isOffline(answer)) speakItem(reply)
            }
            isTyping = false
            researchStages = emptyList()
            onAnswer?.invoke(reply)
        }
    }

    fun send(text: String, voiceLang: String? = null, onAnswer: ((NoorAiChatItem?) -> Unit)? = null) {
        val clean = text.trim()
        if (clean.isBlank() || isTyping) { onAnswer?.invoke(null); return }
        val next = messages + NoorAiChatItem(clean, true)
        update(next)
        input = ""
        focusManager.clearFocus()
        ask(clean, next, voiceLang, onAnswer)
    }

    fun newChat() {
        job?.cancel(); isTyping = false; revealId = null; speaker.stop(); researchStages = emptyList()
        convId = System.currentTimeMillis()
        messages = emptyList()
        NoorAiChatStore.setCurrent(context, mode, null)
    }

    val voiceFallback = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val heard = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (r.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) send(heard)
    }

    fun launchFallback() {
        runCatching { voiceFallback.launch(NoorVoiceInput.recognizerIntent(voice.language)) }.onFailure {
            android.widget.Toast.makeText(context, "Voice input isn't available on this phone.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    fun startListening() {
        speaker.stop()
        if (voiceInput.isAvailable()) voiceInput.start(voice.language) else launchFallback()
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else voice = reduceVoice(voice, VoiceEvent.PermissionDenied)
    }
    androidx.compose.runtime.LaunchedEffect(voice.finalText) {
        voice.finalText?.let { heard -> voice = reduceVoice(voice, VoiceEvent.Consumed); send(heard) }
    }

    // ---- 1.2.0 (114) voice assistant: record -> Whisper (auto language) -> answer -> natural voice -> (hands-free) listen again
    fun assistantEvent(e: AssistantEvent) { assistant = reduceAssistant(assistant, e) }
    fun assistantListen() {
        speaker.stop()
        assistantJob?.cancel()
        assistantEvent(AssistantEvent.Listen)
        recorder.start(onLevel = { l -> assistantEvent(AssistantEvent.Level(l)) }) { bytes, decision ->
            if (bytes == null) { assistantEvent(if (decision == com.noorpro.app.data.VadDecision.NO_SPEECH) AssistantEvent.NoSpeech else AssistantEvent.Fail("Microphone isn't available right now.")); return@start }
            assistantEvent(AssistantEvent.Recorded)
            assistantJob = scope.launch {
                val hint = language.takeIf { it != com.noorpro.app.data.NoorAiLanguage.AUTO }?.substringBefore('-') ?: voice.language
                val heard = AiFeatureService.transcribeVoice(bytes, hint)
                if (heard == null) { assistantEvent(AssistantEvent.Fail("Couldn't reach Noor AI's voice service. Check your connection, or tap ⌨ to type.")); return@launch }
                // Reply language: the language spoken (a fixed answer language in settings still wins).
                val replyLang = language.takeIf { it != com.noorpro.app.data.NoorAiLanguage.AUTO } ?: heard.language
                assistantEvent(AssistantEvent.Heard(heard.text, replyLang))
                send(heard.text, voiceLang = replyLang) { reply ->
                    if (reply == null || AiFeatureService.isOffline(reply.text)) {
                        assistantEvent(AssistantEvent.Fail(reply?.text?.let { AiFeatureService.parseMeta(it).body } ?: "Noor AI couldn't answer right now."))
                    } else {
                        assistantEvent(AssistantEvent.Answer(reply.text))
                        val plain = NoorAiMarkdown.parse(AiFeatureService.splitFollowups(reply.text).first).joinToString("\n") { plainOf(it) }
                        speaker.speak(reply.id, plain, replyLang) {
                            mainHandler.post {
                                val wasSpeaking = assistant.phase == com.noorpro.app.data.AssistantPhase.SPEAKING
                                assistantEvent(AssistantEvent.SpeakDone)
                                if (wasSpeaking && assistantOpen && assistant.phase == com.noorpro.app.data.AssistantPhase.LISTENING) assistantListen()
                            }
                        }
                    }
                }
            }
        }
    }
    fun assistantStop() {
        recorder.cancel(); speaker.stop(); assistantJob?.cancel()
        if (isTyping) { job?.cancel(); isTyping = false }
        assistantEvent(AssistantEvent.Stop)
    }
    val assistantMicPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) assistantListen() else assistantEvent(AssistantEvent.Fail("Microphone permission is needed to talk to Noor AI."))
    }
    fun openAssistant() {
        assistantOpen = true
        focusManager.clearFocus()
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) assistantListen() else assistantMicPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }
    fun closeAssistant() { assistantStop(); assistantOpen = false; assistant = AssistantState(handsFree = assistant.handsFree) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        NoorAiLaunch.pending?.let { q ->
            NoorAiLaunch.pending = null
            if (mode == NoorAiMode.AskNoor) { if (messages.isNotEmpty()) newChat(); send(q) }
        }
    }

    androidx.activity.compose.BackHandler(enabled = assistantOpen) { closeAssistant() }

    NoorAiChatContent(
        mode = mode,
        messages = messages,
        input = input,
        isTyping = isTyping,
        showTip = showTip,
        revealId = revealId,
        onBack = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.DASHBOARD) },
        onInput = { input = it },
        onSend = { send(input) },
        onSuggestion = { send(it) },
        onStop = { job?.cancel(); isTyping = false; researchStages = emptyList() },
        onModeChange = {
            if (it != mode) {
                job?.cancel(); isTyping = false; researchStages = emptyList()
                mode = it
                val id = NoorAiChatStore.currentId(context, it) ?: System.currentTimeMillis()
                convId = id
                messages = emptyList()
                scope.launch { val loaded = NoorAiHistory.load(context, id); if (convId == id) messages = loaded }
            }
        },
        onClearChat = { job?.cancel(); isTyping = false; update(emptyList()) },
        onDismissTip = { showTip = false; NoorAiChatStore.dismissTip(context) },
        onCopy = { clipboard.setText(AnnotatedString(NoorAiCopy.plain(it.text))); android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show() },
        onShare = { item ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, NoorAiCopy.shareText(item.text))
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
        detectedLanguage = messages.lastOrNull { !it.isUser }?.let { answerLanguageOf(it.text, language) }
            ?: messages.lastOrNull { it.isUser }?.let { com.noorpro.app.data.NoorAiLanguage.detect(it.text) },
        onLanguage = { language = it; NoorAiChatStore.setLanguage(context, it); AiFeatureService.answerLanguage = it },
        conversations = conversations,
        currentConversation = convId,
        onNewChat = { newChat() },
        onOpenConversation = { c ->
            job?.cancel(); isTyping = false; revealId = null; researchStages = emptyList()
            mode = c.mode; convId = c.id
            NoorAiChatStore.setCurrent(context, c.mode, c.id)
            scope.launch { val loaded = NoorAiHistory.load(context, c.id); if (convId == c.id) messages = loaded }
        },
        onDeleteConversation = { c ->
            scope.launch { NoorAiHistory.delete(context, c.id); refreshConversations() }
            if (c.id == convId) newChat()
        },
        onVoice = {
            // RECORD_AUDIO is requested only now, when the mic is tapped.
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            when {
                granted -> startListening()
                !voiceInput.isAvailable() -> launchFallback() // the system dialog handles its own mic permission
                else -> micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        },
        voice = voice,
        onVoiceDone = { voiceInput.finish() },
        onVoiceCancel = { voiceInput.stop(); voice = reduceVoice(voice, VoiceEvent.Cancel) },
        onVoiceLanguage = { code ->
            NoorAiChatStore.setVoiceLanguage(context, code)
            val wasListening = voice.active
            voice = voice.copy(language = code)
            if (wasListening) voiceInput.start(code)
        },
        speakingId = speakingId,
        onSpeak = { item -> if (speakingId == item.id) speaker.stop() else speakItem(item) },
        autoRead = autoRead,
        onAutoRead = { autoRead = it; NoorAiChatStore.setAutoRead(context, it); if (!it) speaker.stop() },
        onRenameConversation = { c, t -> scope.launch { NoorAiHistory.rename(context, c.id, t); refreshConversations() } },
        onPinConversation = { c -> scope.launch { NoorAiHistory.setPinned(context, c.id, !c.pinned); refreshConversations() } },
        searchConversations = { q -> NoorAiHistory.search(context, q) },
        dailyTip = noorDailyTip(),
        tier = tier,
        onTier = { tier = com.noorpro.app.data.NoorAiOptions.normTier(it); NoorAiChatStore.setTier(context, tier); AiFeatureService.tier = tier },
        madhhab = madhhab,
        onMadhhab = { madhhab = com.noorpro.app.data.NoorAiOptions.normMadhhab(it); NoorAiChatStore.setMadhhab(context, madhhab); AiFeatureService.madhhab = madhhab },
        researchStages = researchStages,
        onVoiceMode = { openAssistant() },
        onImageStudio = { showImageStudio = true },
    )

    if (showImageStudio) NoorImageStudioSheet(onDismiss = { showImageStudio = false }, language = language)

    if (assistantOpen) {
        NoorVoiceAssistantOverlay(
            state = assistant,
            onMic = { assistantListen() },
            onStop = {
                // While listening, "stop" means "I'm done talking": send what was recorded.
                if (assistant.phase == com.noorpro.app.data.AssistantPhase.LISTENING) recorder.finishNow() else assistantStop()
            },
            onClose = { closeAssistant() },
            onHandsFree = { on -> assistantEvent(AssistantEvent.HandsFree(on)); NoorAiChatStore.setHandsFree(context, on) },
            onKeyboard = { closeAssistant() },
            onCopyAnswer = { text -> clipboard.setText(AnnotatedString(text)); android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show() },
        )
    }
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
