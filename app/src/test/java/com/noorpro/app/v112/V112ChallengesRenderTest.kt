package com.noorpro.app.v112

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.social.*
import com.noorpro.app.ui.islamic.*
import com.noorpro.app.ui.screens.Reels2Flags
import com.noorpro.app.ui.viewmodel.DeenScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V112ChallengesRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun setUp() = loadHalalWords()
    @After fun tearDown() { Reels2Flags.override = null }

    private val today = "2026-10-28"
    private fun d(vararg n: Int) = n.map { if (it > 31) "2026-11-%02d".format(it - 31) else "2026-10-%02d".format(it) }.toSet()
    private val quran = Challenge("ABCD2345", "A page a day", ChallengeKind.QURAN, "Read at least one page of Quran", 21, "2026-10-20", "aisha", "Aisha",
        listOf("me", "aisha", "bilal", "yusuf", "maryam"))
    private val salawat = Challenge("SLWT7788", "100 salawat daily", ChallengeKind.SALAWAT, "Send 100 salawat on the Prophet ﷺ", 7, "2026-10-29", "me", "Me", listOf("me", "zara"))
    private val kind = Challenge("KIND4455", "One kind deed", ChallengeKind.KINDNESS, "One kind word or deed for someone", 14, "2026-10-01", "me", "Me", listOf("me"))
    private val home = ChallengesUi(listOf(quran, salawat, kind), mapOf("ABCD2345" to d(20, 21, 22, 23, 25, 26, 27), "KIND4455" to d(1, 2, 3, 5, 6, 7, 8, 9, 10, 12, 13, 14)), today, myUid = "me")
    private val detail = ChallengeDetailUi(quran, listOf(
        ChallengeProgress("me", "You", d(20, 21, 22, 23, 25, 26, 27)),
        ChallengeProgress("aisha", "Aisha Rahman", d(20, 21, 22, 23, 24, 25, 26, 27, 28)),
        ChallengeProgress("bilal", "Bilal Khan", d(20, 22, 24, 27)),
        ChallengeProgress("yusuf", "Yusuf Ali", d(21, 22, 23, 24, 25, 26)),
    ), myUid = "me", today = today)

    @Test fun homeLight() {
        var toggled: String? = null; var opened: String? = null
        rule.show(false) { ChallengesHomeContent(home, ChallengesActions(onToggleToday = { toggled = it.code }, onOpen = { opened = it.code })) }
        rule.onNodeWithTag("challenge_today_ABCD2345").performClick()
        assertEquals("ABCD2345", toggled)
        rule.onNodeWithTag("challenge_today_SLWT7788").assertDoesNotExist() // starts tomorrow
        rule.onNodeWithText("Starts tomorrow · 7 days").assertExists()
        rule.onNodeWithText("Completed · 12 of 14 days").assertExists()
        rule.onNodeWithText("7-day streak · a grace day kept it going").assertExists()
        rule.onNodeWithTag("challenge_card_KIND4455").performClick()
        assertEquals("KIND4455", opened)
        rule.onNodeWithTag("challenges_screen").performTouchInput { swipeDown() }
        rule.shot("v112_challenges_home_light")
    }

    @Test fun emptyDark() {
        var idea: ChallengeKind? = null
        rule.show(true) { ChallengesHomeContent(ChallengesUi(today = today, myUid = "me"), ChallengesActions(onTemplate = { idea = it })) }
        rule.onNodeWithTag("challenges_empty").assertExists()
        rule.onNodeWithTag("challenges_idea_SADAQAH").performScrollTo().performClick()
        assertEquals(ChallengeKind.SADAQAH, idea)
        rule.onNodeWithTag("challenges_screen").performTouchInput { swipeDown() }
        rule.shot("v112_challenges_empty_dark")
    }

    @Test fun unavailable() {
        rule.show(false) { ChallengesHomeContent(ChallengesUi(error = CommunityError.OFFLINE_BACKEND, today = today), ChallengesActions()) }
        rule.onNodeWithTag("challenges_notice").assertExists()
    }

    @Test fun detailLight() {
        var toggled: String? = null
        rule.show(false) { ChallengeDetailContent(detail, ChallengeDetailActions(onToggle = { toggled = it }, onShareCard = {})) }
        rule.onNodeWithTag("challenge_mark_today").assertTextContains("I did today's deed", substring = true)
        rule.onNodeWithTag("challenge_streak").assertTextEquals("7-day streak · a grace day kept it going")
        rule.onNodeWithTag("challenge_day_4").performClick()
        assertEquals("2026-10-23", toggled)
        rule.onNodeWithTag("challenge_day_12").assertIsNotEnabled()
        rule.onNodeWithTag("challenge_together").assertTextEquals("26 deeds done together by 5 people, alhamdulillah")
        rule.onNodeWithTag("challenge_code_shown").performScrollTo().assertTextEquals("ABCD 2345")
        rule.onNodeWithTag("challenge_detail").performTouchInput { swipeDown() }
        rule.shot("v112_challenges_detail_light")
    }

    @Test fun detailDarkMembers() {
        rule.show(true) { ChallengeDetailContent(detail.copy(progress = detail.progress.map { if (it.uid == "me") it.copy(done = it.done + today) else it }), ChallengeDetailActions()) }
        rule.onNodeWithTag("challenge_mark_today").assertTextContains("Done today", substring = true)
        rule.onNodeWithTag("challenge_member_aisha").performScrollTo()
        rule.onNodeWithText("+ 1 more who haven't checked in yet").assertExists()
        rule.shot("v112_challenges_members_dark")
    }

    @Test fun leaveConfirm() {
        var left = false
        rule.show(false) { ChallengeDetailContent(detail, ChallengeDetailActions(onLeave = { left = true })) }
        rule.onNodeWithTag("challenge_menu").performClick()
        rule.onNodeWithTag("challenge_leave").performClick()
        rule.onNodeWithText("Leave this challenge?").assertExists()
        rule.onNodeWithTag("challenge_leave_confirm").performClick()
        assertTrue(left)
    }

    @Test fun createLight() {
        var made: List<Any>? = null
        rule.show(false) { ChallengeCreateContent(ChallengeKind.QURAN, today, onBack = {}) { t, k, g, n, s, _ -> made = listOf(t, k, g, n, s) } }
        rule.onNodeWithTag("challenge_kind_ADHKAR").performScrollTo().performClick()
        rule.onNodeWithTag("challenge_title").assertTextContains("Morning & evening adhkar", substring = true)
        rule.onNodeWithTag("challenge_days_40").performScrollTo().performClick()
        rule.onNodeWithTag("challenge_start_tomorrow").performClick()
        rule.onNodeWithTag("challenge_create_go").performClick()
        assertEquals(listOf("Morning & evening adhkar", ChallengeKind.ADHKAR, "Morning and evening adhkar", 40, "2026-10-29"), made)
        rule.onNodeWithTag("challenge_create").performTouchInput { swipeDown() }
        rule.onNodeWithTag("challenge_days_21").performScrollTo().performClick()
        rule.shot("v112_challenges_create_light")
    }

    @Test fun createHoldsScamDark() {
        rule.show(true) { ChallengeCreateContent(ChallengeKind.CUSTOM, today, onBack = {}) { _, _, _, _, _, _ -> fail("should be held") } }
        rule.onNodeWithTag("challenge_title").performTextInput("Earn 5000 daily from home")
        rule.onNodeWithTag("challenge_hint").assertTextContains("community guidelines", substring = true)
        rule.onNodeWithTag("challenge_create_go").assertHasClickAction().performClick()
        rule.shot("v112_challenges_create_held_dark")
    }

    @Test fun joinDark() {
        var code: String? = null
        rule.show(true) { ChallengeJoinContent(onBack = {}) { c, _ -> code = c } }
        rule.onNodeWithTag("challenge_code").performTextInput("abcd-23o45")
        rule.onNodeWithTag("challenge_join_go").performClick()
        assertEquals("ABCD2345", code)
        rule.shot("v112_challenges_join_dark")
    }

    @Test fun ummahShortcutAndFlagDefault() {
        assertFalse(IslamicFlags.isOn(IslamicFlags.CHALLENGES))
        assertFalse(IslamicFlags.isOn(IslamicFlags.SHARE_CARDS))
        Reels2Flags.override = mapOf(IslamicFlags.CHALLENGES to true, IslamicFlags.SHARE_CARDS to true)
        var opened: DeenScreen? = null
        rule.show(false) { Column { UmmahCommunityShortcuts { opened = it } } }
        rule.onNodeWithTag("ummah_link_UMMAH_CHALLENGES").performClick()
        assertEquals(DeenScreen.UMMAH_CHALLENGES, opened)
        rule.onNodeWithTag("ummah_link_UMMAH_SHARE_CARDS").performScrollTo().performClick()
        assertEquals(DeenScreen.UMMAH_SHARE_CARDS, opened)
    }
}
