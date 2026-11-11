package com.noorpro.app.v113

import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.NoorAiLanguage
import com.noorpro.app.data.NoorAiLocalCorpus
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.data.NoorAiReply
import com.noorpro.app.data.creator.CreatorAiTask
import com.noorpro.app.data.creator.CreatorSubtitles
import com.noorpro.app.data.creator.SubtitleLine
import com.noorpro.app.islamic.hifz.HifzAiPrompt
import com.noorpro.app.ui.screens.isFiqhQuestion
import com.noorpro.app.ui.screens.noorAiHeaderLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 1.2.0 (113): free Noor AI Worker, auto language, honest offline fallback, Hifz + creator AI. */
class NoorAiV113Test {
    @Test fun detectsLanguageOfQuestion() {
        assertEquals("ur", NoorAiLanguage.detect("مجھے حج کے بارے میں جاننا ہے"))
        assertEquals("ar", NoorAiLanguage.detect("ما هي أركان الإسلام؟"))
        assertEquals("hi", NoorAiLanguage.detect("नमाज़ में ध्यान कैसे लगाएं?"))
        assertEquals("bn", NoorAiLanguage.detect("নামাজে মনোযোগ কিভাবে বাড়াব?"))
        assertEquals("ur-Latn", NoorAiLanguage.detect("namaz mein dhyan kaise lagaye mujhe batao"))
        assertEquals("en", NoorAiLanguage.detect("How can I improve focus in salah?"))
        assertEquals("en", NoorAiLanguage.detect("12345"))
    }

    @Test fun explicitLanguageWinsOverAuto() {
        assertEquals("hi", NoorAiLanguage.resolve("hi", "What is zakat?"))
        assertEquals("ur", NoorAiLanguage.resolve(NoorAiLanguage.AUTO, "زکوٰۃ کیا ہے؟"))
        assertEquals("en", NoorAiLanguage.resolve("xx", "What is zakat?"))
    }

    @Test fun offlineMessagesAreLocalizedAndRecognised() {
        val ur = NoorAiLanguage.unavailable("ur")
        assertNotEquals(NoorAiLanguage.unavailable("en"), ur)
        assertTrue(ur.contains("نور"))
        assertTrue(AiFeatureService.isOffline(ur))
        assertTrue(AiFeatureService.isOffline(AiFeatureService.UNAVAILABLE_MESSAGE))
        assertFalse(AiFeatureService.isOffline("Here are five ways to focus in salah."))
        // Urdu question offline -> Urdu note, never English hadith quotes.
        assertEquals(ur, AiFeatureService.offlineFallback("مجھے حج کے بارے میں جاننا ہے", NoorAiMode.HajjUmrahGuide))
    }

    @Test fun answerMetadataRoundTrips() {
        val stored = AiFeatureService.encode(NoorAiReply("**Hajj** is the fifth pillar.", "ur", fiqh = false, followups = listOf("a?", "b?"), source = "worker"))
        val m = AiFeatureService.parseMeta(stored)
        assertEquals("**Hajj** is the fifth pillar.", m.body)
        assertEquals(listOf("a?", "b?"), m.followups)
        assertEquals(false, m.fiqh)
        assertEquals("ur", m.language)
        assertEquals("**Hajj** is the fifth pillar.", AiFeatureService.splitFollowups(stored).first)
        // Answers saved by older versions still parse.
        val legacy = AiFeatureService.parseMeta("Body\nFOLLOWUPS: x | y")
        assertEquals("Body", legacy.body); assertNull(legacy.fiqh); assertNull(legacy.language)
    }

    @Test fun fiqhNoteOnlyOnRulingQuestions() {
        assertFalse(isFiqhQuestion("How can I improve focus in salah?"))
        assertFalse(isFiqhQuestion("How can I build a Quran habit?"))
        assertFalse(isFiqhQuestion("مجھے حج کے بارے میں جاننا ہے"))
        assertTrue(isFiqhQuestion("Can I eat gelatin sweets?"))
        assertTrue(isFiqhQuestion("Is it halal to take a mortgage?"))
        assertTrue(isFiqhQuestion("Does vomiting break my fast?"))
        assertTrue(isFiqhQuestion("کیا سود حرام ہے؟"))
    }

    @Test fun headerShowsDetectedLanguageInAutoMode() {
        assertEquals("Auto language", noorAiHeaderLanguage(NoorAiLanguage.AUTO, null))
        assertEquals("Urdu", noorAiHeaderLanguage(NoorAiLanguage.AUTO, "ur"))
        assertEquals("Roman Urdu", noorAiHeaderLanguage(NoorAiLanguage.AUTO, "ur-Latn"))
        assertEquals("Hindi", noorAiHeaderLanguage("hi", "ur"))
    }

    @Test fun workerUrlFromRemoteConfigOrDefault() {
        assertEquals(AiFeatureService.DEFAULT_WORKER_URL, AiFeatureService.resolveWorkerUrl(null))
        assertEquals(AiFeatureService.DEFAULT_WORKER_URL, AiFeatureService.resolveWorkerUrl(""))
        assertEquals(AiFeatureService.DEFAULT_WORKER_URL, AiFeatureService.resolveWorkerUrl("http://insecure.example"))
        assertEquals("https://ai.example.com", AiFeatureService.resolveWorkerUrl(" https://ai.example.com/ "))
        assertTrue(AiFeatureService.DEFAULT_WORKER_URL.startsWith("https://noorpro-ai."))
    }

    @Test fun offlineRetrievalIsStrict() {
        assertEquals(listOf("improve", "humbl", "pray"), NoorAiLocalCorpus.keywords("How can I improve focus in salah?"))
        assertTrue(NoorAiLocalCorpus.keywords("مجھے حج کے بارے میں جاننا ہے").isEmpty())
        assertEquals(2, NoorAiLocalCorpus.needed(3))
        assertEquals(3, NoorAiLocalCorpus.needed(5))
        // "pray" counts at a word start only.
        assertEquals(1, NoorAiLocalCorpus.score("He prayed two rak'ah", listOf("pray", "humbl")))
        assertEquals(0, NoorAiLocalCorpus.score("He spray painted", listOf("pray")))
    }

    @Test fun creatorTasksBeyondCaptions() {
        val names = CreatorAiTask.entries.map { it.name }
        assertTrue(names.containsAll(listOf("CAPTIONS", "HOOKS", "HASHTAGS", "TRANSLATE", "SUBTITLES", "MODERATE", "CHECK")))
        assertEquals("hooks", CreatorAiTask.HOOKS.workerTask)
        assertEquals("moderate", CreatorAiTask.MODERATE.workerTask)
        assertNull(CreatorAiTask.CHECK.workerTask)
    }

    @Test fun subtitlesExportAsSrt() {
        val lines = listOf(SubtitleLine(1, 0.0, 1.5, "Bismillah"), SubtitleLine(2, 61.25, 63.0, "Be patient", translated = "صبر کرو"))
        assertEquals("1|Bismillah\n2|Be patient", CreatorSubtitles.toIndexed(lines))
        val srt = CreatorSubtitles.toSrt(lines)
        assertTrue(srt.contains("00:00:00,000 --> 00:00:01,500\nBismillah"))
        assertTrue(srt.contains("00:01:01,250 --> 00:01:03,000\nصبر کرو"))
        assertEquals("Bismillah صبر کرو", CreatorSubtitles.transcript(lines))
        assertEquals("1:01", CreatorSubtitles.clock(61.25))
    }

    @Test fun hifzAiPromptsAndLanguage() {
        assertTrue(HifzAiPrompt.meaning("Al-Ikhlas", 112, 1).contains("112:1"))
        assertTrue(HifzAiPrompt.question("اس آیت کا مطلب کیا ہے؟", "Al-Ikhlas", 112, 2).contains("112:2"))
        assertEquals("ur", HifzAiPrompt.language("اس آیت کا مطلب کیا ہے؟", NoorAiLanguage.AUTO, "en"))
        assertEquals("hi", HifzAiPrompt.language("", "hi", "en"))
        assertEquals("ur", HifzAiPrompt.language("", NoorAiLanguage.AUTO, "ur"))
        assertEquals("en", HifzAiPrompt.language("", NoorAiLanguage.AUTO, "fr"))
    }
}
