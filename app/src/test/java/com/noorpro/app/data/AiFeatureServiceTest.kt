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
}
