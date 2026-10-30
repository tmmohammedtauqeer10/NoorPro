package com.noorpro.app.v108

import android.content.Context
import android.speech.SpeechRecognizer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class NoorAiV108Test {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()

    // ---------------------------------------------------------------- history store CRUD

    @Test fun historyCreateRenamePinSearchDelete() {
        val a = 1_000L; val b = 2_000L
        NoorAiChatStore.save(ctx, NoorAiMode.AskNoor, a, listOf(NoorAiChatItem("What breaks wudu?", true, id = 1), NoorAiChatItem("Sleeping deeply…", false, id = 2)))
        NoorAiChatStore.save(ctx, NoorAiMode.QuranHelper, b, listOf(NoorAiChatItem("Explain Ayat al-Kursi", true, id = 3)))
        var convs = NoorAiChatStore.conversations(ctx)
        assertEquals(listOf(b, a), convs.map { it.id })
        assertEquals("What breaks wudu?", convs.last().title)

        NoorAiChatStore.rename(ctx, a, "Wudu notes")
        NoorAiChatStore.save(ctx, NoorAiMode.AskNoor, a, NoorAiChatStore.load(ctx, a) + NoorAiChatItem("And ghusl?", true, id = 4))
        assertEquals("Wudu notes", NoorAiChatStore.conversations(ctx).first { it.id == a }.title) // rename survives new messages
        assertEquals(3, NoorAiChatStore.load(ctx, a).size)

        NoorAiChatStore.setPinned(ctx, b, false); NoorAiChatStore.setPinned(ctx, a, true)
        convs = NoorAiChatStore.conversations(ctx)
        assertEquals(a, convs.first().id); assertTrue(convs.first().pinned)

        assertEquals(listOf(a), NoorAiChatStore.search(ctx, "sleeping").map { it.id })   // message text
        assertEquals(listOf(b), NoorAiChatStore.search(ctx, "kursi").map { it.id })       // title
        assertEquals(2, NoorAiChatStore.search(ctx, "").size)

        NoorAiChatStore.rename(ctx, a, "  ")
        assertEquals("What breaks wudu?", NoorAiChatStore.conversations(ctx).first { it.id == a }.title) // blank reverts to auto

        NoorAiChatStore.delete(ctx, a)
        assertEquals(listOf(b), NoorAiChatStore.conversations(ctx).map { it.id })
        assertTrue(NoorAiChatStore.load(ctx, a).isEmpty())
    }

    @Test fun autoReadAndVoiceLanguagePersist() {
        assertFalse(NoorAiChatStore.autoRead(ctx))
        NoorAiChatStore.setAutoRead(ctx, true); assertTrue(NoorAiChatStore.autoRead(ctx))
        NoorAiChatStore.setVoiceLanguage(ctx, "ur"); assertEquals("ur", NoorAiChatStore.voiceLanguage(ctx))
    }

    // ---------------------------------------------------------------- voice state machine

    @Test fun voiceFlowPartialThenFinal() {
        var s = reduceVoice(NoorVoiceState(), VoiceEvent.Start("ar"))
        assertEquals(VoicePhase.LISTENING, s.phase); assertEquals("ar", s.language)
        s = reduceVoice(s, VoiceEvent.Level(10f)); assertTrue(s.level > 0.9f)
        s = reduceVoice(s, VoiceEvent.Partial("what is"))
        s = reduceVoice(s, VoiceEvent.Partial("what is tawakkul"))
        assertEquals("what is tawakkul", s.partial)
        s = reduceVoice(s, VoiceEvent.EndOfSpeech); assertEquals(VoicePhase.PROCESSING, s.phase)
        s = reduceVoice(s, VoiceEvent.Final("What is tawakkul?"))
        assertEquals(VoicePhase.IDLE, s.phase); assertEquals("What is tawakkul?", s.finalText)
        s = reduceVoice(s, VoiceEvent.Consumed); assertNull(s.finalText)
    }

    @Test fun voiceErrorsCancelAndTimeoutKeepPartial() {
        val l = reduceVoice(NoorVoiceState(), VoiceEvent.Start("en"))
        val e = reduceVoice(l, VoiceEvent.Error(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(VoicePhase.ERROR, e.phase); assertNotNull(e.error)
        val withPartial = reduceVoice(l, VoiceEvent.Partial("dua for rain"))
        assertEquals("dua for rain", reduceVoice(withPartial, VoiceEvent.Error(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)).finalText)
        assertEquals(VoicePhase.IDLE, reduceVoice(withPartial, VoiceEvent.Cancel).phase)
        assertEquals("", reduceVoice(withPartial, VoiceEvent.Cancel).partial)
        assertEquals(VoicePhase.ERROR, reduceVoice(l, VoiceEvent.Final("  ")).phase)
        // late events after cancel are ignored
        val idle = reduceVoice(withPartial, VoiceEvent.Cancel)
        assertEquals(idle, reduceVoice(idle, VoiceEvent.Partial("late")))
        assertEquals(VoicePhase.ERROR, reduceVoice(NoorVoiceState(), VoiceEvent.PermissionDenied).phase)
    }

    @Test fun ttsSplitsArabicAndSkipsWithoutVoice() {
        val text = "Recite this dua: اللَّهُمَّ إِنِّي أَسْأَلُكَ الْإِخْلَاصَ It means O Allah, I ask You for sincerity.\nSources:\n- Bukhari 1"
        val segs = ttsSegments(text)
        assertEquals(3, segs.size); assertTrue(segs[1].arabic); assertFalse(segs[2].text.contains("Bukhari"))
        val noAr = ttsSegments(text, arabicVoice = false)
        assertEquals(2, noAr.size); assertTrue(noAr.none { it.arabic })
        assertEquals(1, ttsSegments("یہ اردو جواب ہے", "ur").size)
        assertTrue(ttsSegments("   ").isEmpty())
    }

    @Test fun fiqhDetection() {
        assertTrue(isFiqhQuestion("Is it halal to take a mortgage?"))
        assertTrue(isFiqhQuestion("Does vomiting break wudu?"))
        assertFalse(isFiqhQuestion("Tell me the story of Prophet Yusuf"))
    }

    // ---------------------------------------------------------------- renders

    private val answer = """
        Tawakkul means relying on Allah while doing what you can.

        ### In practice
        - **Tie your camel**, then trust Allah.
        - Make dua: حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ

        Sources:
        - Jami at-Tirmidhi 2517
        - Al-Imran 3:173
        FOLLOWUPS: How do I build tawakkul? | Dua for anxiety | Stories of tawakkul
    """.trimIndent()

    private fun chat(dark: Boolean, voice: NoorVoiceState, speaking: Boolean, msgs: List<NoorAiChatItem>) {
        rule.setContent {
            MyApplicationTheme(darkTheme = dark) {
                NoorAiChatContent(
                    mode = NoorAiMode.AskNoor, messages = msgs, input = "", isTyping = false, showTip = false, revealId = null,
                    onBack = {}, onInput = {}, onSend = {}, onSuggestion = {}, onStop = {}, onModeChange = {}, onClearChat = {},
                    onDismissTip = {}, onCopy = {}, onShare = {}, onRegenerate = {}, onFeedback = { _, _ -> }, onRevealDone = {},
                    voice = voice, speakingId = if (speaking) 2L else null,
                )
            }
        }
    }

    private val msgs = listOf(
        NoorAiChatItem("What is tawakkul?", true, 1_760_000_000_000L, id = 1),
        NoorAiChatItem(answer, false, 1_760_000_060_000L, feedback = 1, id = 2),
    )

    @Test fun listeningAndSpokenLight() {
        chat(false, NoorVoiceState(VoicePhase.LISTENING, partial = "Is it halal to pray in", level = 0.6f, language = "en"), true, msgs)
        rule.onNodeWithTag("noorai_voice_panel").assertIsDisplayed()
        rule.onNodeWithText("Is it halal to pray in").assertIsDisplayed()
        rule.onNodeWithTag("noorai_speak").assertExists()
        rule.saveShot("noorai_voice_light")
    }

    @Test fun listeningAndSpokenDark() {
        chat(true, NoorVoiceState(VoicePhase.LISTENING, partial = "کیا سفر میں نماز قصر", level = 0.4f, language = "ur"), true, msgs)
        rule.onNodeWithTag("noorai_voice_panel").assertIsDisplayed()
        rule.saveShot("noorai_voice_dark")
    }

    @Test fun fiqhNoteShowsForRulingQuestion() {
        chat(false, NoorVoiceState(), false, listOf(NoorAiChatItem("Is it halal to take a mortgage?", true, id = 1), NoorAiChatItem("Scholars differ…", false, id = 2)))
        rule.onNodeWithTag("noorai_fiqh_note").assertExists()
        rule.saveShot("noorai_fiqh_light")
    }

    private val convs = listOf(
        NoorAiConversation(1, NoorAiMode.AskNoor, "What is tawakkul?", System.currentTimeMillis(), pinned = true),
        NoorAiConversation(2, NoorAiMode.QuranHelper, "Explain Ayat al-Kursi simply", System.currentTimeMillis() - 86_400_000L),
        NoorAiConversation(3, NoorAiMode.DuaGenerator, "Dua for my parents' health", System.currentTimeMillis() - 3 * 86_400_000L),
        NoorAiConversation(4, NoorAiMode.HajjUmrahGuide, "Umrah steps in order", System.currentTimeMillis() - 9 * 86_400_000L),
    )

    @Test fun historyLight() {
        rule.setContent { MyApplicationTheme(darkTheme = false) { HistoryListPreview(false, convs) } }
        rule.onNodeWithText("Your chats").assertIsDisplayed()
        rule.onNodeWithText("Search chats").assertIsDisplayed()
        rule.saveShot("noorai_history_light")
    }

    @Test fun historyDark() {
        rule.setContent { MyApplicationTheme(darkTheme = true) { HistoryListPreview(true, convs) } }
        rule.saveShot("noorai_history_dark")
    }
}
