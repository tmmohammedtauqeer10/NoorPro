package com.noorpro.app.v111

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.islamic.hifz.HifzQuran
import com.noorpro.app.islamic.hifz.HifzRange
import com.noorpro.app.islamic.kids.*
import com.noorpro.app.ui.islamic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V111KidsRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val members = listOf(FamilyMember("a", "Abbu", 0), FamilyMember("b", "Ammi", 3), FamilyMember("c", "Ayesha", 1), FamilyMember("d", "Yusuf", 2))
    private val today = 20_000L
    private val log: Map<Long, Map<String, Int>> = (0..6).associate { i ->
        (today - i) to mapOf("a" to 0b11111, "b" to if (i % 3 == 0) 0b01111 else 0b11111, "c" to (0b11111 shr (i % 4)), "d" to (0b10101 shr (i % 2)))
    } + (today to mapOf("a" to 0b00111, "b" to 0b00111, "c" to 0b00011, "d" to 0b00001))

    @Test fun hubLight() {
        var started = false
        rule.show(false) { FamilyHubContent(KidsHomeUi("Ayesha", 23, 2), 4, 9, {}, { started = true }, {}, {}, {}) }
        rule.onNodeWithTag("kids_start").performClick(); assertTrue(started)
        rule.shot("v111_kids_hub_light")
    }
    @Test fun kidsHomeActiveDark() {
        var parent = false
        rule.show(true) { KidsHomeContent(KidsHomeUi("Ayesha", 23, 3, active = true), null, {}, { parent = true }) }
        rule.onNodeWithTag("ik_back").assertDoesNotExist()
        rule.onNodeWithTag("kids_parent").performClick(); assertTrue(parent)
        rule.shot("v111_kids_home_dark")
    }
    @Test fun kidsHomeLight() {
        var opened = ""
        rule.show(false) { KidsHomeContent(KidsHomeUi("Yusuf", 8, 1, active = true), null, { opened = it }, {}) }
        rule.onNodeWithTag("kids_tile_stories").performClick(); assertEquals("stories", opened)
        rule.shot("v111_kids_home_light")
    }
    @Test fun surahListLight() {
        rule.show(false) { KidsSurahListContent(KIDS_SURAHS.map { it to (it.id in setOf(1, 112, 108)) }, {}) {} }
        rule.shot("v111_kids_surahs_light")
    }
    @Test fun surahDetailLight() {
        val ayahs = HifzQuran.ayahs(ApplicationProvider.getApplicationContext(), HifzRange(112, 1, 300))
        assertEquals(4, ayahs.size)
        var listened = false; var starred = false
        rule.show(false) { KidsSurahContent(KIDS_SURAHS[1], ayahs, false, {}, { listened = true }, { starred = true }) }
        rule.onNodeWithTag("kids_listen").performClick(); rule.onNodeWithTag("kids_star").performScrollTo().performClick()
        assertTrue(listened && starred)
        rule.shot("v111_kids_surah_light")
    }
    @Test fun duasDark() {
        var star = ""
        rule.show(true) { KidsDuasContent(KIDS_DUAS.mapIndexed { i, d -> d to (i == 0) }, {}) { star = it.id } }
        rule.onNodeWithTag("kids_dua_star_ate").performClick(); assertEquals("ate", star)
        rule.shot("v111_kids_duas_dark")
    }
    @Test fun storiesLight() {
        rule.show(false) { KidsStoriesContent(PROPHET_STORIES.mapIndexed { i, p -> p to (i < 2) }, {}) {} }
        rule.onNodeWithTag("kids_story_yunus").assertExists()
        rule.shot("v111_kids_stories_light")
    }
    @Test fun storyDark() {
        rule.show(true) { KidsStoryContent(PROPHET_STORIES[4], false, {}) {} }
        rule.onNodeWithTag("kids_lesson").assertExists()
        rule.shot("v111_kids_story_dark")
    }
    @Test fun mannersLight() {
        var star = ""
        rule.show(false) { KidsMannersContent(MANNERS_CARDS.mapIndexed { i, m -> m to (i == 1) }, {}) { star = it.id } }
        rule.onNodeWithTag("kids_manner_star_salam").performClick(); assertEquals("salam", star)
        rule.shot("v111_kids_manners_light")
    }
    @Test fun pinEnterDark() {
        val entered = mutableListOf<String>()
        rule.show(true) { KidsPinContent(PinMode.ENTER, "That PIN isn't right.", "7 × 8 + 13", 0, {}, {}) { entered += it } }
        listOf("pin_2", "pin_5", "pin_8", "pin_0").forEach { rule.onNodeWithTag(it).performClick() }
        assertEquals(listOf("2580"), entered)
        rule.onNodeWithTag("pin_1").performClick(); rule.onNodeWithTag("pin_4").performClick()
        rule.shot("v111_kids_pin_dark")
    }
    @Test fun familyLight() {
        var toggled: Pair<String, Int>? = null
        rule.show(false) { FamilyPrayerContent(members, log, today, today, {}, {}, { id, p -> toggled = id to p }, {}, {}) }
        rule.onNodeWithTag("family_c_2").performClick(); assertEquals("c" to 2, toggled)
        rule.shot("v111_kids_family_light")
    }
    @Test fun familyDark() {
        rule.show(true) { FamilyPrayerContent(members, log, today - 1, today, {}, {}, { _, _ -> }, {}, {}) }
        rule.onNodeWithText("Yesterday").assertExists()
        rule.shot("v111_kids_family_dark")
    }
}
