package com.noorpro.app.v114

import com.noorpro.app.data.AssistantEvent
import com.noorpro.app.data.AssistantPhase
import com.noorpro.app.data.AssistantState
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.data.NoorAiOptions
import com.noorpro.app.data.VadDecision
import com.noorpro.app.data.VoiceActivityDetector
import com.noorpro.app.data.assistantStatus
import com.noorpro.app.data.reduceAssistant
import com.noorpro.app.ui.screens.NoorAiConversation
import com.noorpro.app.ui.screens.groupConversations
import com.noorpro.app.ui.screens.recentConversations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/** 1.2.0 (114): madhhab / tier settings, research SSE parsing, speech chunks, voice assistant, history grouping. */
class NoorAiV114LogicTest {

    @Test fun madhhabNormalisation() {
        assertEquals("hanafi", NoorAiOptions.normMadhhab(null))
        assertEquals("hanafi", NoorAiOptions.normMadhhab("Hanfi"))
        assertEquals("shafii", NoorAiOptions.normMadhhab("Shafi"))
        assertEquals("shafii", NoorAiOptions.normMadhhab("shafi'i"))
        assertEquals("maliki", NoorAiOptions.normMadhhab("Maliki"))
        assertEquals("hanbali", NoorAiOptions.normMadhhab("HANBALI"))
        assertEquals("compare", NoorAiOptions.normMadhhab("all four"))
        assertEquals("compare", NoorAiOptions.normMadhhab("compare"))
        assertEquals("Shafi'i", NoorAiOptions.madhhabLabel("shafi"))
        assertEquals(5, NoorAiOptions.MADHHABS.size)
    }

    @Test fun tierNormalisationAndEffectiveTier() {
        assertEquals("balanced", NoorAiOptions.normTier(null))
        assertEquals("balanced", NoorAiOptions.normTier("medium"))
        assertEquals("fast", NoorAiOptions.normTier("Fast"))
        assertEquals("research", NoorAiOptions.normTier("research"))
        assertEquals("research", NoorAiOptions.effectiveTier("research", NoorAiMode.AskNoor))
        assertEquals("balanced", NoorAiOptions.effectiveTier("research", NoorAiMode.AppGuide))
        assertEquals("balanced", NoorAiOptions.effectiveTier("research", NoorAiMode.DuaGenerator))
        assertEquals("fast", NoorAiOptions.effectiveTier("fast", NoorAiMode.QuranHelper))
        assertEquals(listOf("Fast", "Balanced", "Research"), NoorAiOptions.TIERS.map { it.label })
    }

    @Test fun sseLines() {
        assertEquals("{\"type\":\"stage\"}", NoorAiOptions.sseData("data: {\"type\":\"stage\"}"))
        assertEquals("{}", NoorAiOptions.sseData("data:{}\r"))
        assertNull(NoorAiOptions.sseData(""))
        assertNull(NoorAiOptions.sseData(": keep-alive"))
        assertNull(NoorAiOptions.sseData("data:   "))
    }

    @Test fun speechChunksStripMarkdownAndSplitOnSentences() {
        val text = "### Summary\n**Wudu** is required for salah [K1]. It has four fard parts in the Hanafi school! Is it hard? No.\n\nSources:\n- Nur al-Idah"
        val chunks = NoorAiOptions.speechChunks(text, max = 60, first = 40)
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.none { it.contains("[K1]") || it.contains("*") || it.contains("#") || it.contains("Nur al-Idah") })
        assertTrue(chunks.first().length <= 40)
        assertTrue(chunks.all { it.length <= 60 })
        assertEquals("Summary Wudu is required for salah . It has four fard parts in the Hanafi school! Is it hard? No.".replace(" .", "."),
            chunks.joinToString(" ").replace(" .", "."))
        // Urdu full stop and question mark
        val ur = NoorAiOptions.speechChunks("وضو نماز کے لیے فرض ہے۔ کیا خون سے وضو ٹوٹتا ہے؟ جی ہاں۔", max = 30, first = 30)
        assertTrue(ur.size >= 2)
        assertTrue(NoorAiOptions.speechChunks("   ").isEmpty())
        // A very long sentence is hard-split on words
        val long = NoorAiOptions.speechChunks(List(200) { "word" }.joinToString(" "), max = 100, first = 100)
        assertTrue(long.size >= 8 && long.all { it.length <= 100 })
    }

    @Test fun voiceActivityDetector() {
        val vad = VoiceActivityDetector(silenceMs = 1000, noSpeechMs = 3000, maxMs = 20_000)
        var t = 0L
        repeat(5) { assertEquals(VadDecision.CONTINUE, vad.feed(400, t)); t += 100 }
        repeat(10) { assertEquals(VadDecision.CONTINUE, vad.feed(6000, t)); t += 100 }
        assertTrue(vad.heardSpeech)
        var d = VadDecision.CONTINUE
        while (d == VadDecision.CONTINUE && t < 5000) { d = vad.feed(300, t); t += 100 }
        assertEquals(VadDecision.END_OF_SPEECH, d)

        val quiet = VoiceActivityDetector(silenceMs = 1000, noSpeechMs = 3000)
        t = 0; d = VadDecision.CONTINUE
        while (d == VadDecision.CONTINUE && t < 10_000) { d = quiet.feed(500, t); t += 100 }
        assertEquals(VadDecision.NO_SPEECH, d)
        assertFalse(quiet.heardSpeech)

        // Loud background raises the threshold: steady noise is not speech.
        val noisy = VoiceActivityDetector(silenceMs = 1000, noSpeechMs = 2000)
        t = 0; d = VadDecision.CONTINUE
        while (d == VadDecision.CONTINUE && t < 10_000) { d = noisy.feed(2500, t); t += 100 }
        assertEquals(VadDecision.NO_SPEECH, d)

        val endless = VoiceActivityDetector(maxMs = 1000)
        t = 0; d = VadDecision.CONTINUE
        while (d == VadDecision.CONTINUE && t < 5000) { d = endless.feed(9000, t); t += 100 }
        assertEquals(VadDecision.MAX_LENGTH, d)
    }

    @Test fun assistantFlowWithHandsFree() {
        var s = AssistantState(handsFree = true)
        s = reduceAssistant(s, AssistantEvent.Listen); assertEquals(AssistantPhase.LISTENING, s.phase)
        s = reduceAssistant(s, AssistantEvent.Level(0.7f)); assertEquals(0.7f, s.level, 0.001f)
        s = reduceAssistant(s, AssistantEvent.Recorded); assertEquals(AssistantPhase.TRANSCRIBING, s.phase); assertTrue(s.busy)
        s = reduceAssistant(s, AssistantEvent.Heard("وضو کیسے کریں؟", "ur")); assertEquals(AssistantPhase.THINKING, s.phase); assertEquals("ur", s.language)
        s = reduceAssistant(s, AssistantEvent.Answer("پہلے نیت کریں…")); assertEquals(AssistantPhase.SPEAKING, s.phase); assertEquals(1, s.turns)
        s = reduceAssistant(s, AssistantEvent.SpeakDone); assertEquals(AssistantPhase.LISTENING, s.phase) // hands-free listens again
        s = reduceAssistant(s, AssistantEvent.NoSpeech); assertEquals(AssistantPhase.IDLE, s.phase)
        assertTrue(assistantStatus(s).contains("Hands-free paused"))
    }

    @Test fun assistantWithoutHandsFreeAndErrors() {
        var s = reduceAssistant(AssistantState(), AssistantEvent.Listen)
        s = reduceAssistant(s, AssistantEvent.Recorded)
        s = reduceAssistant(s, AssistantEvent.Heard("How do I pray witr?", "en"))
        s = reduceAssistant(s, AssistantEvent.Answer("Witr is …"))
        s = reduceAssistant(s, AssistantEvent.SpeakDone); assertEquals(AssistantPhase.IDLE, s.phase)
        // Stale events are ignored
        assertEquals(s, reduceAssistant(s, AssistantEvent.Answer("late")))
        assertEquals(s, reduceAssistant(s, AssistantEvent.Recorded))
        s = reduceAssistant(s, AssistantEvent.Listen)
        s = reduceAssistant(s, AssistantEvent.Recorded)
        s = reduceAssistant(s, AssistantEvent.Heard("  ", "en")); assertEquals(AssistantPhase.ERROR, s.phase)
        s = reduceAssistant(s, AssistantEvent.Fail("offline")); assertEquals("offline", assistantStatus(s))
        s = reduceAssistant(s, AssistantEvent.HandsFree(true)); assertTrue(s.handsFree)
        assertEquals(AssistantPhase.IDLE, reduceAssistant(s, AssistantEvent.Stop).phase)
    }

    @Test fun historyGroupsLikeChatGpt() {
        val utc = TimeZone.getTimeZone("UTC")
        val now = 1_794_400_000_000L // a fixed instant
        val day = 86_400_000L
        val startOfToday = now - now % day
        fun c(id: Long, at: Long, pinned: Boolean = false) = NoorAiConversation(id, NoorAiMode.AskNoor, "c$id", at, pinned)
        val list = listOf(
            c(1, now - 1000), c(2, startOfToday - 1000), c(3, now - 3 * day), c(4, now - 20 * day), c(5, now - 90 * day), c(6, now - 50 * day, pinned = true),
        )
        val g = groupConversations(list, now, utc)
        assertEquals(listOf("Pinned", "Today", "Yesterday", "Previous 7 days", "Previous 30 days", "Older"), g.map { it.title })
        assertEquals(listOf(6L), g[0].items.map { it.id })
        assertEquals(listOf(1L), g[1].items.map { it.id })
        assertEquals(listOf(2L), g[2].items.map { it.id })
        assertEquals(listOf(5L), g.last().items.map { it.id })
        assertEquals(listOf(1L, 2L, 3L), recentConversations(list, exclude = null, max = 3).map { it.id })
        assertEquals(listOf(2L, 3L), recentConversations(list, exclude = 1L, max = 2).map { it.id })
        assertTrue(groupConversations(emptyList(), now, utc).isEmpty())
    }
}
