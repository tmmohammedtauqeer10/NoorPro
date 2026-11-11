package com.noorpro.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.os.Build
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class NoorAiMode(
    val title: String,
    val shortTitle: String,
    val helperText: String,
    val promptHint: String
) {
    AskNoor(
        title = "Ask Noor AI",
        shortTitle = "Ask",
        helperText = "General Islamic guidance with safe boundaries.",
        promptHint = "Ask about salah, manners, family, learning, or daily faith..."
    ),
    DuaGenerator(
        title = "Dua Generator",
        shortTitle = "Dua",
        helperText = "Turn your situation into a respectful dua draft.",
        promptHint = "Example: dua for exam stress, parents, forgiveness..."
    ),
    QuranHelper(
        title = "Quran Helper",
        shortTitle = "Quran",
        helperText = "Simple reflections and lessons from ayat.",
        promptHint = "Ask: explain this ayah simply, what lesson should I take?"
    ),
    HadithExplainer(
        title = "Hadith Explainer",
        shortTitle = "Hadith",
        helperText = "Understand hadith meaning and daily action points.",
        promptHint = "Paste a hadith or ask for a simple explanation..."
    ),
    HajjUmrahGuide(
        title = "Hajj & Umrah Guide",
        shortTitle = "Hajj",
        helperText = "Step-by-step ritual guidance and travel reminders.",
        promptHint = "Ask: what do I do after tawaf? how to prepare for umrah?"
    ),
    AppGuide(
        title = "App Help",
        shortTitle = "App Help",
        helperText = "Ask how to use any Noor Pro feature.",
        promptHint = "Ask: how do I upload a reel? change my username? find Qibla?"
    )
}

data class NoorAiMessage(
    val text: String,
    val isUser: Boolean
)

/** A parsed Noor AI answer: visible body plus the hidden trailing metadata lines. */
data class NoorAiAnswerMeta(val body: String, val followups: List<String>, val fiqh: Boolean?, val language: String?)

/** Result of an online-only Noor AI call (Worker first, then the askNoorAi Cloud Function). */
data class NoorAiReply(val text: String, val language: String, val fiqh: Boolean, val followups: List<String>, val source: String)

/** Extra context sent with a question (e.g. the ayah being memorised in Hifz Coach). */
data class NoorAiContext(
    val surah: String = "", val ayahRef: String = "", val arabic: String = "", val translation: String = "", val note: String = "",
)

object AiFeatureService {
    // Self-host is only reachable from the Android emulator (10.0.2.2 -> host loopback). On real phones it
    // would just burn the connect timeout, so it is skipped unless an explicit non-default override is set.
    internal const val NOOR_AI_SELFHOST_URL = "http://10.0.2.2:8080/v1/chat"

    /**
     * 1.2.0 (113): free Noor AI backend, the Cloudflare Worker `noorpro-ai` (Workers AI; Groq when a key is set
     * as a Worker secret). No key or secret is in the app. Remote Config `noor_ai_url` can move it.
     */
    const val DEFAULT_WORKER_URL = "https://noorpro-ai.noorpro.workers.dev"
    internal const val REMOTE_CONFIG_URL_KEY = "noor_ai_url"

    /** Optional explicit self-host URL (set at app start, or NOOR_AI_SELFHOST_URL env). Non-default only. */
    @Volatile
    var selfHostUrlOverride: String? = null

    /** Answer language: [NoorAiLanguage.AUTO] (match the question) or a fixed code. Set from the chat screen. */
    @Volatile
    var answerLanguage: String = NoorAiLanguage.AUTO

    /** Language of the most recent answer (for the chat header). */
    @Volatile
    var lastLanguage: String = "en"

    private const val NOOR_AI_FUNCTION_URL =
        "https://us-central1-noor-pro-d87e3.cloudfunctions.net/askNoorAi"
    private const val SAFE_DISCLAIMER =
        "Note: Noor AI can make mistakes. For fatwa, divorce, inheritance, medical, legal, or serious personal issues, consult a qualified scholar."
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        // Mobile networks (and a cold-started Cloud Function) routinely need more than 2s to connect.
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        // Whole-call cap (blocking execute is not cancelled by coroutine timeouts).
        .callTimeout(50, TimeUnit.SECONDS)
        .build()
    private val workerClient = client.newBuilder().callTimeout(55, TimeUnit.SECONDS).build()
    private val uploadClient = client.newBuilder().writeTimeout(60, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS).build()

    /** Worker base URL: Remote Config override if it is a valid https URL, else the built-in default. */
    internal fun resolveWorkerUrl(remote: String?): String {
        val r = remote?.trim()?.trimEnd('/').orEmpty()
        return if (r.startsWith("https://") && r.length > 12 && !r.contains(' ')) r else DEFAULT_WORKER_URL
    }

    private fun workerUrl(path: String): String = resolveWorkerUrl(
        runCatching { com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getString(REMOTE_CONFIG_URL_KEY) }.getOrNull()
    ) + path

    /**
     * Noor AI chat: Worker -> askNoorAi -> offline. The returned text carries hidden FOLLOWUPS / NOORMETA lines
     * that [parseMeta] strips for display.
     */
    suspend fun generateResponse(
        prompt: String,
        mode: NoorAiMode,
        history: List<NoorAiMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val clean = prompt.trim()
        val lang = NoorAiLanguage.resolve(answerLanguage, clean)
        val selfHostUrl = resolveSelfHostUrl(isEmulator(), selfHostUrlOverride ?: System.getenv("NOOR_AI_SELFHOST_URL"))
        // Hard cap on the online attempt so the chat never spins for minutes on a bad network.
        val reply = kotlinx.coroutines.withTimeoutOrNull(ONLINE_TIMEOUT_MS) {
            ask(clean, mode, history, language = answerLanguage)?.let { encode(it) }
                ?: (if (selfHostUrl != null) callNoorAiSelfHost(selfHostUrl, clean, mode, history) else null)
        }
        reply ?: offlineFallback(clean, mode, lang).also { lastLanguage = lang }
    }

    /**
     * Online-only call for features that need a real answer (Hifz, Reels, Creator, goals): returns null when
     * offline, so callers never show offline Qur'an/hadith quotes as if they were tips or captions.
     */
    suspend fun ask(
        prompt: String,
        mode: NoorAiMode,
        history: List<NoorAiMessage> = emptyList(),
        context: NoorAiContext? = null,
        serverMode: String = mode.name,
        language: String = answerLanguage,
        usePassages: Boolean = true,
    ): NoorAiReply? = withContext(Dispatchers.IO) {
        val clean = prompt.trim()
        if (clean.isBlank()) return@withContext null
        val lang = NoorAiLanguage.resolve(language, clean)
        kotlinx.coroutines.withTimeoutOrNull(ONLINE_TIMEOUT_MS) {
            callWorkerChat(clean, serverMode, history, context, language, usePassages && mode != NoorAiMode.DuaGenerator && mode != NoorAiMode.AppGuide)
                ?: callNoorAiFunctionWithRetry(withContextText(clean, context), mode, history, lang)?.let { raw ->
                    val meta = parseMeta(raw)
                    NoorAiReply(meta.body, lang, false, meta.followups, "askNoorAi")
                }
        }?.also { lastLanguage = it.language }
    }

    /** Same as [ask] but returns plain answer text (no hidden lines) or null. */
    suspend fun generateOnline(prompt: String, mode: NoorAiMode = NoorAiMode.AskNoor, language: String = answerLanguage): String? =
        ask(prompt, mode, language = language, usePassages = false)?.text

    private fun withContextText(prompt: String, ctx: NoorAiContext?): String {
        if (ctx == null) return prompt
        val parts = listOfNotNull(
            ctx.surah.takeIf { it.isNotBlank() }?.let { "Surah: $it" }, ctx.ayahRef.takeIf { it.isNotBlank() }?.let { "Ayah: $it" },
            ctx.arabic.takeIf { it.isNotBlank() }?.let { "Arabic: ${it.take(800)}" }, ctx.translation.takeIf { it.isNotBlank() }?.let { "Translation: ${it.take(800)}" },
        )
        return (parts + prompt).joinToString("\n").take(3800)
    }

    /** Text stored in the chat: body, then hidden NOORMETA and FOLLOWUPS lines. */
    internal fun encode(r: NoorAiReply): String = buildString {
        append(r.text.trimEnd())
        append("\nNOORMETA: lang=").append(r.language).append(";fiqh=").append(if (r.fiqh) "1" else "0")
        if (r.followups.isNotEmpty()) append("\nFOLLOWUPS: ").append(r.followups.joinToString(" | "))
    }

    /** Splits hidden trailing FOLLOWUPS / NOORMETA lines off an answer. */
    fun parseMeta(answer: String): NoorAiAnswerMeta {
        val lines = answer.trimEnd().lines().toMutableList()
        var followups = emptyList<String>()
        var fiqh: Boolean? = null
        var lang: String? = null
        while (lines.isNotEmpty()) {
            val last = lines.last().trim()
            when {
                last.startsWith("FOLLOWUPS:", ignoreCase = true) -> {
                    followups = last.substringAfter(':').split("|").map { it.replace("**", "").trim() }.filter { it.isNotEmpty() }.take(3)
                }
                last.startsWith("NOORMETA:") -> last.substringAfter(':').split(';').forEach { kv ->
                    val (k, v) = kv.trim().split('=', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                    if (k == "lang" && v.isNotBlank()) lang = v
                    if (k == "fiqh") fiqh = v == "1"
                }
                last.isEmpty() -> Unit
                else -> break
            }
            lines.removeAt(lines.lastIndex)
        }
        return NoorAiAnswerMeta(lines.joinToString("\n").trimEnd(), followups, fiqh, lang)
    }

    /** Splits the trailing "FOLLOWUPS: a | b | c" line (and hidden metadata) off an answer. */
    fun splitFollowups(answer: String): Pair<String, List<String>> = parseMeta(answer).let { it.body to it.followups }

    /** True for the localized "can't reach the server" notes (never for real answers). */
    fun isOffline(text: String?): Boolean {
        val t = text?.trim().orEmpty()
        return t.isEmpty() || t == UNAVAILABLE_MESSAGE || NoorAiLanguage.allUnavailable.any { t.endsWith(it) }
    }

    /**
     * Shown only when the real AI could not be reached. App Help keeps its static feature map (it is
     * not an AI answer anyway); other modes show only strongly related saved passages, or a short
     * localized "offline" note - never a dump of loosely related hadith.
     */
    internal fun offlineFallback(prompt: String, mode: NoorAiMode, lang: String = NoorAiLanguage.detect(prompt)): String =
        if (mode == NoorAiMode.AppGuide) buildLocalGuidedResponse(prompt, mode)
        else (if (mode == NoorAiMode.DuaGenerator || lang != "en") null else NoorAiLocalCorpus.answer(prompt, lang)) ?: NoorAiLanguage.unavailable(lang)

    private const val ONLINE_TIMEOUT_MS = 75_000L

    internal val UNAVAILABLE_MESSAGE: String get() = NoorAiLanguage.unavailable("en")

    private suspend fun callWorkerChat(
        prompt: String, serverMode: String, history: List<NoorAiMessage>, ctx: NoorAiContext?, language: String, usePassages: Boolean,
    ): NoorAiReply? {
        val historyJson = org.json.JSONArray()
        trimHistoryForRequest(history, prompt).forEach { m ->
            historyJson.put(JSONObject().put("role", if (m.isUser) "user" else "assistant").put("content", parseMeta(m.text).body.take(2500)))
        }
        val passages = org.json.JSONArray()
        if (usePassages) NoorAiLocalCorpus.passages(prompt).forEach { (ref, text) -> passages.put(JSONObject().put("ref", ref).put("text", text)) }
        val payload = JSONObject()
            .put("prompt", prompt.take(4000))
            .put("mode", serverMode)
            .put("language", language.ifBlank { NoorAiLanguage.AUTO })
            .put("history", historyJson)
            .put("passages", passages)
        if (ctx != null) payload.put("context", JSONObject().put("surah", ctx.surah).put("ayahRef", ctx.ayahRef)
            .put("arabic", ctx.arabic.take(1500)).put("translation", ctx.translation.take(1500)).put("note", ctx.note.take(600)))
        val root = postWorker("/v1/chat", payload, workerClient) ?: return null
        return parseWorkerChat(root)
    }

    internal fun parseWorkerChat(root: JSONObject): NoorAiReply? {
        val answer = root.optString("answer").trim().takeIf { it.isNotBlank() } ?: return null
        val fu = root.optJSONArray("followups")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).trim().takeIf { q -> q.isNotEmpty() } } }.orEmpty()
        val cited = withCitations(answer, jsonRefs(root.optJSONArray("citations")))
        return NoorAiReply(cited, root.optString("language").ifBlank { "en" }, root.optBoolean("fiqh", false), fu.take(3), "worker")
    }

    /** Creator tools on the Worker: captions, hooks, hashtags, translate, subtitles, moderate. Null when offline. */
    suspend fun creator(task: String, text: String, topic: String = "", lang: String = NoorAiLanguage.AUTO, kind: String = "reel"): JSONObject? =
        withContext(Dispatchers.IO) {
            postWorker("/v1/creator", JSONObject().put("task", task).put("text", text.take(3000)).put("topic", topic).put("lang", lang).put("kind", kind), workerClient)
        }

    /** Speech-to-text for reel subtitles (AAC/M4A bytes). Null when offline or too large. */
    suspend fun transcribe(audio: ByteArray, lang: String? = null): JSONObject? = withContext(Dispatchers.IO) {
        if (audio.isEmpty() || audio.size > 4_500_000) return@withContext null
        val payload = JSONObject().put("audio", android.util.Base64.encodeToString(audio, android.util.Base64.NO_WRAP))
        if (!lang.isNullOrBlank()) payload.put("lang", lang)
        postWorker("/v1/transcribe", payload, uploadClient)
    }

    private suspend fun postWorker(path: String, payload: JSONObject, http: OkHttpClient): JSONObject? = try {
        val rb = Request.Builder().url(workerUrl(path)).post(payload.toString().toRequestBody(jsonMediaType)).addHeader("Content-Type", "application/json")
        FirebaseCallableHeaders.idToken()?.let { rb.addHeader("Authorization", "Bearer $it") }
        FirebaseCallableHeaders.appCheckToken()?.let { rb.addHeader("X-Firebase-AppCheck", it) }
        http.newCall(rb.build()).execute().use { r ->
            if (!r.isSuccessful) null else JSONObject(r.body?.string().orEmpty())
        }
    } catch (_: Exception) { null }

    /** Builds the history array sent to the backend: recent turns only, never the question being asked. */
    internal fun trimHistoryForRequest(history: List<NoorAiMessage>, currentPrompt: String): List<NoorAiMessage> {
        val turns = history.filter { it.text.isNotBlank() && !isOffline(it.text) }.toMutableList()
        if (turns.isNotEmpty() && turns.last().isUser && turns.last().text.trim() == currentPrompt.trim()) {
            turns.removeAt(turns.lastIndex)
        }
        // Drop the greeting that the screen seeds as the first assistant message.
        while (turns.isNotEmpty() && !turns.first().isUser) turns.removeAt(0)
        return turns.takeLast(10)
    }

    /** Appends a "Sources" block only when the model did not already include one. */
    internal fun withCitations(answer: String, refs: List<String>): String {
        val clean = refs.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(6)
        if (clean.isEmpty() || Regex("(?im)^\\s*\\**sources?:?\\**\\s*$").containsMatchIn(answer)) return answer
        return answer.trimEnd() + "\n\nSources:\n" + clean.joinToString("\n") { "- $it" }
    }

    /** Returns the self-host URL to try, or null to skip straight to Firebase. */
    internal fun resolveSelfHostUrl(isEmulator: Boolean, override: String?): String? {
        val custom = override?.trim()?.takeIf { it.isNotEmpty() && it != NOOR_AI_SELFHOST_URL }
        return custom ?: if (isEmulator) NOOR_AI_SELFHOST_URL else null
    }

    internal fun isEmulator(
        fingerprint: String? = Build.FINGERPRINT,
        model: String? = Build.MODEL,
        product: String? = Build.PRODUCT,
        manufacturer: String? = Build.MANUFACTURER,
        brand: String? = Build.BRAND,
        device: String? = Build.DEVICE,
        hardware: String? = Build.HARDWARE
    ): Boolean {
        val fp = fingerprint.orEmpty()
        val md = model.orEmpty()
        val pr = product.orEmpty()
        val hw = hardware.orEmpty()
        return fp.startsWith("generic") ||
            fp.startsWith("unknown") ||
            fp.contains("emulator", true) ||
            md.contains("google_sdk", true) ||
            md.contains("Emulator", true) ||
            md.contains("Android SDK built for", true) ||
            manufacturer.orEmpty().contains("Genymotion", true) ||
            (brand.orEmpty().startsWith("generic") && device.orEmpty().startsWith("generic")) ||
            pr.contains("sdk_gphone", true) ||
            pr.contains("google_sdk", true) ||
            pr.contains("emulator", true) ||
            pr.contains("simulator", true) ||
            pr == "sdk" || pr == "sdk_x86" || pr == "vbox86p" ||
            hw.contains("goldfish", true) || hw.contains("ranchu", true)
    }

    private suspend fun callNoorAiSelfHost(
        url: String,
        prompt: String,
        mode: NoorAiMode,
        history: List<NoorAiMessage>
    ): String? {
        if (prompt.isBlank()) return null
        return try {
            val historyJson = org.json.JSONArray()
            history.takeLast(10).forEach { message ->
                historyJson.put(
                    JSONObject()
                        .put("role", if (message.isUser) "user" else "assistant")
                        .put("content", message.text.take(3500))
                )
            }
            val payload = JSONObject()
                .put("message", prompt.take(4000))
                .put("mode", mode.name)
                .put("history", historyJson)
                .toString()
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                val root = JSONObject(body)
                val answer = root.optString("answer").takeIf { it.isNotBlank() } ?: return null
                withCitations(answer, jsonRefs(root.optJSONArray("citations")))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun jsonRefs(array: org.json.JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val item = array.opt(i)
            when (item) {
                is JSONObject -> item.optString("ref").takeIf { it.isNotBlank() }
                is String -> item.takeIf { it.isNotBlank() }
                else -> null
            }
        }
    }

    private suspend fun callNoorAiFunctionWithRetry(
        prompt: String,
        mode: NoorAiMode,
        history: List<NoorAiMessage>,
        language: String,
    ): String? {
        if (prompt.isBlank()) return null
        repeat(2) { attempt ->
            callNoorAiFunction(prompt, mode, history, language)?.let { return it }
            if (attempt == 0) delay(1200)
        }
        return null
    }

    private suspend fun callNoorAiFunction(prompt: String, mode: NoorAiMode, history: List<NoorAiMessage>, language: String): String? {
        if (prompt.isBlank()) return null
        return try {
            // Send the raw question + recent turns. The backend owns the (Sunni-grounded) system prompt;
            // wrapping the question in a long template used to push it past the server's length cap.
            val historyJson = org.json.JSONArray()
            trimHistoryForRequest(history, prompt).forEach { message ->
                historyJson.put(
                    JSONObject()
                        .put("role", if (message.isUser) "user" else "assistant")
                        .put("content", message.text.take(3500))
                )
            }
            val payload = JSONObject()
                .put("prompt", prompt.take(3800))
                .put("mode", mode.name)
                .put("instructions", mode.styleHint())
                .put("language", language.substringBefore('-'))
                .put("history", historyJson)
                .toString()

            val requestBuilder = Request.Builder()
                .url(NOOR_AI_FUNCTION_URL)
                .post(payload.toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")

            FirebaseCallableHeaders.idToken()?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            FirebaseCallableHeaders.appCheckToken()?.let {
                requestBuilder.addHeader("X-Firebase-AppCheck", it)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                val root = JSONObject(body)
                val answer = root.optString("answer").trim().takeIf { it.isNotBlank() } ?: return null
                val follow = root.optJSONArray("followups")
                val fu = if (follow == null) emptyList() else (0 until follow.length()).mapNotNull { follow.optString(it).trim().takeIf { q -> q.isNotEmpty() } }
                val cited = withCitations(answer, jsonRefs(root.optJSONArray("citations")))
                if (fu.isEmpty()) cited else cited.trimEnd() + "\nFOLLOWUPS: " + fu.joinToString(" | ")
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun NoorAiMode.styleHint(): String = when (this) {
        NoorAiMode.DuaGenerator -> "Keep it concise and heartfelt."
        NoorAiMode.AppGuide -> "Answer briefly with exact tap-by-tap steps."
        else -> "Answer the exact question asked, with practical steps; be clear and not longer than needed (about 300-600 words)."
    }

    private fun buildLocalGuidedResponse(prompt: String, mode: NoorAiMode): String {
        val cleanPrompt = prompt.ifBlank { "your question" }
        return when (mode) {
            NoorAiMode.AskNoor -> """
                Bismillah. Here is a detailed, balanced way to think about it:

                For your question: "$cleanPrompt"

                1. Start from what is certain: Allah loves sincerity, patience, prayer, good character, and asking people of knowledge.
                2. If this is a worship question, avoid guessing. Check the Quran, authentic Sunnah, and your trusted local Sunni scholar.
                3. If this is a daily-life problem, choose the option that protects faith, family rights, honesty, modesty, and peace.
                4. Make the next step small: pray two rak'ah if appropriate, make dua, write down the facts, and avoid acting while angry or confused.
                5. If rights of another person are involved, be fair. Islam does not teach benefit for one person through oppression of another.

                Practical action plan:
                - Write the exact situation in 3-5 lines.
                - Note what you already tried.
                - Mention your country and the madhhab/scholar you normally follow.
                - Ask Noor AI to turn that into a clear question for a qualified scholar.

                $SAFE_DISCLAIMER
            """.trimIndent()

            NoorAiMode.DuaGenerator -> """
                May Allah make it easy for you. You can say:

                Arabic style dua:
                Allahumma inni as'aluka khayra hadhal-amr, wa barakatahu, wa an taj'alahu sabab l-qurbi minka.

                Meaning:
                O Allah, I ask You for goodness in this matter, its blessing, and that You make it a means of coming closer to You.

                Personal wording for: "$cleanPrompt"
                O Allah, give me clarity, patience, halal ease, and a heart that trusts You. Protect me from anxiety, guide my decisions, and place barakah in my time and actions.

                Longer dua:
                O Allah, open for me the doors of mercy, wisdom, and halal provision. Make me pleased with what You choose, protect me from haste, and do not leave me to myself even for the blink of an eye. Put light in my heart, truth on my tongue, and sincerity in my actions.

                Daily practice:
                Repeat it after salah, especially in sujood and before salam, and combine dua with a practical halal step.

                $SAFE_DISCLAIMER
            """.trimIndent()

            NoorAiMode.QuranHelper -> """
                Quran reflection helper:

                For: "$cleanPrompt"

                A safe way to understand Quran is:
                1. Read the ayah with the verses before and after it.
                2. Check a reliable tafsir instead of guessing.
                3. Take a practical lesson: stronger tawheed, better character, more patience, or more gratitude.

                Simple reflection:
                Ask yourself: what does this teach me about Allah, my responsibility, and my next action today?

                A deeper reflection structure:
                - Belief: What name or attribute of Allah does this remind me of?
                - Character: What behavior should I improve?
                - Worship: What act of obedience can I do today?
                - Caution: What sin, arrogance, or negligence should I avoid?

                $SAFE_DISCLAIMER
            """.trimIndent()

            NoorAiMode.HadithExplainer -> """
                Hadith explanation helper:

                For: "$cleanPrompt"

                To understand a hadith safely:
                1. Check the source and authenticity.
                2. Understand who was being addressed and why.
                3. Convert it into one daily action: better salah, cleaner speech, mercy, honesty, or avoiding harm.

                Daily action:
                Pick one small Sunnah-linked action today and do it consistently.

                Useful checklist:
                - Is the narration authentic or at least accepted by reliable scholars?
                - Is it about belief, worship, manners, family, trade, or society?
                - What is one action I can begin without becoming harsh with people?

                $SAFE_DISCLAIMER
            """.trimIndent()

            NoorAiMode.HajjUmrahGuide -> """
                Hajj & Umrah helper:

                For: "$cleanPrompt"

                Basic Umrah order:
                1. Ihram and niyyah from miqat.
                2. Tawaf around the Kaaba.
                3. Pray two rak'ah after tawaf if possible.
                4. Sa'i between Safa and Marwah.
                5. Cut/trim hair to exit ihram.

                If you are unsure during travel, ask an official guide or scholar at the Haram before acting.

                Preparation checklist:
                - Learn the dua and talbiyah before travel.
                - Keep slippers, unscented soap, documents, and medicine ready.
                - Save your hotel location and group contact.
                - Do not argue in ihram; protect your tongue and patience.

                $SAFE_DISCLAIMER
            """.trimIndent()

            NoorAiMode.AppGuide -> """
                Noor Pro quick guide:

                For: "$cleanPrompt"

                • Home — next prayer, today's schedule, daily goals.
                • Quran — read surahs, continue reading, audio, bookmarks.
                • Reels — swipe up/down; double-tap sides for ±10s; hold for 2x; pull down to refresh.
                • Ummah — community posts; + to create a post or reel; follow creators.
                • Messages — Profile → Messages: Chats, People, Groups (admin adds members via Group info), your QR.
                • Profile — pencil icon edits your name, @username, and photo.

                Ask me a specific question like "how do I upload a reel?" for step-by-step help.
            """.trimIndent()
        }
    }
}

