package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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

object AiFeatureService {
    private const val NOOR_AI_FUNCTION_URL =
        "https://us-central1-noor-pro-d87e3.cloudfunctions.net/askNoorAi"
    private const val SAFE_DISCLAIMER =
        "Note: Noor AI can make mistakes. For fatwa, divorce, inheritance, medical, legal, or serious personal issues, consult a qualified scholar."
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(28, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        prompt: String,
        mode: NoorAiMode,
        history: List<NoorAiMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        callNoorAiFunction(prompt.trim(), mode, history) ?: run {
            delay(120)
            buildLocalGuidedResponse(prompt.trim(), mode)
        }
    }

    private suspend fun callNoorAiFunction(prompt: String, mode: NoorAiMode, history: List<NoorAiMessage>): String? {
        if (prompt.isBlank()) return null
        return try {
            // Send the recent conversation so Noor AI remembers context. The backend may cap this,
            // but giving it a little more recent context improves follow-up answers.
            val historyJson = org.json.JSONArray()
            history.takeLast(10).forEach { message ->
                historyJson.put(
                    JSONObject()
                        .put("role", if (message.isUser) "user" else "assistant")
                        .put("content", message.text.take(3500))
                )
            }
            val payload = JSONObject()
                .put("prompt", buildDetailedPrompt(prompt, mode).take(4500))
                .put("mode", mode.name)
                .put("answerStyle", "detailed_structured_fast")
                .put("maxWords", 950)
                .put(
                    "instructions",
                    "Give a detailed, practical, Sunni-friendly answer in clear sections. Use simple language, include action steps, and mention when a qualified scholar is needed."
                )
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
                JSONObject(body).optString("answer").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildDetailedPrompt(prompt: String, mode: NoorAiMode): String {
        return """
            Mode: ${mode.title}

            User question:
            $prompt

            Noor Pro answer requirements:
            - Give a detailed but clear answer, around 500-900 words when the question needs depth.
            - Use headings, short paragraphs, and practical action steps.
            - Keep the tone warm, Sunni-friendly, and respectful.
            - For Quran or hadith, explain safely and avoid inventing references.
            - For fatwa, divorce, inheritance, medical, legal, or serious personal issues, advise the user to ask a qualified scholar.
            - If the user asks how to use the Noor Pro app, answer with exact app steps.
        """.trimIndent()
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
