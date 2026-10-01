package com.noorpro.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiFeatureServiceTest {
    private val default = AiFeatureService.NOOR_AI_SELFHOST_URL

    @Test
    fun realDevice_skipsSelfHost() {
        assertNull(AiFeatureService.resolveSelfHostUrl(false, null))
        assertNull(AiFeatureService.resolveSelfHostUrl(false, ""))
        assertNull(AiFeatureService.resolveSelfHostUrl(false, default))
    }

    @Test
    fun emulator_usesDefaultSelfHost() {
        assertEquals(default, AiFeatureService.resolveSelfHostUrl(true, null))
    }

    @Test
    fun explicitOverride_usedEverywhere() {
        val url = "https://ai.example.com/v1/chat"
        assertEquals(url, AiFeatureService.resolveSelfHostUrl(false, url))
        assertEquals(url, AiFeatureService.resolveSelfHostUrl(true, " $url "))
    }

    @Test
    fun emulatorDetection() {
        assertTrue(AiFeatureService.isEmulator("generic/sdk_gphone64_x86_64/emu64xa:14/UE1A/1:userdebug/dev-keys", "sdk_gphone64_x86_64", "sdk_gphone64_x86_64", "Google", "google", "emu64xa", "ranchu"))
        assertFalse(AiFeatureService.isEmulator("samsung/a54xnsxx/a54x:14/UP1A/1:user/release-keys", "SM-A546E", "a54xnsxx", "samsung", "samsung", "a54x", "s5e8835"))
        assertFalse(AiFeatureService.isEmulator("google/husky/husky:14/UD1A/1:user/release-keys", "Pixel 8 Pro", "husky", "Google", "google", "husky", "husky"))
    }

    @Test
    fun offlineFallback_isHonestAndNotGenericAdvice() {
        val a = AiFeatureService.offlineFallback("How many rakah is Fajr?", NoorAiMode.AskNoor)
        val b = AiFeatureService.offlineFallback("Is music haram?", NoorAiMode.AskNoor)
        assertEquals(AiFeatureService.UNAVAILABLE_MESSAGE, a)
        assertEquals(a, b)
        assertTrue(a.contains("could not be reached"))
    }

    @Test
    fun history_dropsGreetingAndCurrentQuestion() {
        val history = listOf(
            NoorAiMessage("Assalamu alaikum", false),
            NoorAiMessage("What is zakat?", true),
            NoorAiMessage("Zakat is...", false),
            NoorAiMessage("And on gold?", true)
        )
        val sent = AiFeatureService.trimHistoryForRequest(history, "And on gold?")
        assertEquals(listOf("What is zakat?", "Zakat is..."), sent.map { it.text })
    }

    @Test
    fun citations_appendedOnlyOnce() {
        val plain = AiFeatureService.withCitations("Answer", listOf("Quran 2:255", "Sahih al-Bukhari 1"))
        assertTrue(plain.contains("Sources:"))
        assertTrue(plain.contains("- Quran 2:255"))
        val already = "Answer\nSources:\n- Quran 2:255"
        assertEquals(already, AiFeatureService.withCitations(already, listOf("Quran 2:255")))
    }
}
