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
class V112AskScholarRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun setUp() = loadHalalWords()
    @After fun tearDown() { Reels2Flags.override = null }

    private val travel = ScholarQuestion("q1", "I travel for work two days every week. May I shorten and combine Dhuhr and Asr on those days?", ScholarCategory.SALAH,
        anonymous = true, answered = true,
        answer = "If your trip reaches the distance considered travel and you are not staying long at the destination, you may shorten the four-unit prayers to two and combine Dhuhr with Asr. " +
            "This is a concession from Allah, and the Prophet ﷺ used it on his journeys. A regular weekly trip is still a journey. Once you reach home, pray in full again. " +
            "If you are unsure about the distance in your case, ask your local imam, as schools of fiqh measure it slightly differently.",
        scholarUid = "s1", scholarName = "Shaykh Abdullah Hasan", scholarTitle = "Imam · Leicester Central Mosque", createdAt = ago(4 * 24 * 60), answeredAt = ago(2 * 24 * 60))
    private val gold = ScholarQuestion("q2", "Is zakat due on gold jewellery that I wear regularly?", ScholarCategory.ZAKAT, anonymous = false, name = "Aisha", answered = true,
        answer = "Scholars have two well-known positions. Many hold that zakat is due on gold jewellery once it reaches the nisab and a lunar year passes; others exempt jewellery worn in normal use. " +
            "Paying it is the more cautious choice, and it purifies your wealth.",
        scholarUid = "s2", scholarName = "Ustadha Maryam Ali", scholarTitle = "Alimah · Fiqh teacher", createdAt = ago(9 * 24 * 60), answeredAt = ago(6 * 24 * 60))
    private val fasts = ScholarQuestion("q3", "I missed six fasts last Ramadan because of illness. How should I make them up?", ScholarCategory.FASTING, answered = true,
        answer = "Make up each missed day with one day of fasting before next Ramadan if you are able. They do not need to be consecutive. If the illness is lasting and you cannot fast, feed one poor person for each day instead.",
        scholarUid = "s1", scholarName = "Shaykh Abdullah Hasan", scholarTitle = "Imam · Leicester Central Mosque", answeredAt = ago(11 * 24 * 60))
    private val pending = ScholarQuestion("p1", "My parents want me to marry a cousin I don't feel ready for. How do I speak to them with respect?", ScholarCategory.FAMILY, createdAt = ago(180))
    private val ui = ScholarUi(library = listOf(travel, gold, fasts), mine = listOf(pending, gold), saved = setOf("q2"), myUid = "me", now = NOW)

    @Test fun libraryLight() {
        var saved: String? = null
        rule.show(false) { AskScholarContent(ui, ScholarActions(onSave = { saved = it.id })) }
        rule.onNodeWithTag("scholar_save_q1").performClick()
        assertEquals("q1", saved)
        rule.onNodeWithTag("scholar_disclaimer").assertExists()
        rule.shot("v112_scholar_library_light")
    }

    @Test fun searchDark() {
        rule.show(true) {
            var s by remember { mutableStateOf(ui) }
            AskScholarContent(s, ScholarActions(onQuery = { s = s.copy(query = it) }))
        }
        rule.onNodeWithTag("scholar_search").performTextInput("travel")
        rule.onNodeWithTag("scholar_qa_q1").assertExists()
        rule.onNodeWithTag("scholar_qa_q2").assertDoesNotExist()
        rule.onNodeWithTag("scholar_more_q1").performScrollTo().performClick()
        rule.onNodeWithText("Show less").assertExists()
        rule.onNodeWithTag("scholar_screen").performTouchInput { swipeDown() }
        rule.onNodeWithTag("scholar_search").performScrollTo()
        rule.shot("v112_scholar_search_dark")
    }

    @Test fun noResults() {
        rule.show(false) { AskScholarContent(ui.copy(query = "inheritance"), ScholarActions()) }
        rule.onNodeWithTag("scholar_empty").assertTextContains("inheritance", substring = true)
    }

    @Test fun mineLight() {
        var withdrew: String? = null
        rule.show(false) { AskScholarContent(ui.copy(tab = 1), ScholarActions(onWithdraw = { withdrew = it.id })) }
        rule.onNodeWithTag("scholar_pending_p1").assertExists()
        rule.onNodeWithTag("scholar_report_q2").assertDoesNotExist()
        rule.onNodeWithTag("scholar_withdraw_p1").performClick()
        assertEquals("p1", withdrew)
        rule.shot("v112_scholar_mine_light")
    }

    @Test fun savedDark() {
        rule.show(true) { AskScholarContent(ui.copy(tab = 2), ScholarActions()) }
        rule.onNodeWithTag("scholar_qa_q2").assertExists()
        rule.onNodeWithTag("scholar_qa_q1").assertDoesNotExist()
        rule.shot("v112_scholar_saved_dark")
    }

    @Test fun scholarInboxLight() {
        var answering: String? = null
        val s = Scholar("s1", "Shaykh Abdullah Hasan", "Imam · Leicester Central Mosque")
        val q = ScholarQuestion("o1", "Is it allowed to pray Fajr after sunrise if I overslept without meaning to?", ScholarCategory.SALAH, createdAt = ago(40))
        rule.show(false) { AskScholarContent(ui.copy(scholar = s, inbox = listOf(q, pending.copy(id = "o2")), tab = 3), ScholarActions(onAnswer = { answering = it.id })) }
        rule.onNodeWithText("Inbox · 2").assertExists()
        rule.onNodeWithTag("scholar_answer_open_o1").performClick()
        assertEquals("o1", answering)
        rule.shot("v112_scholar_inbox_light")
    }

    @Test fun inboxHiddenForNonScholars() {
        rule.show(false) { AskScholarContent(ui, ScholarActions()) }
        rule.onNodeWithTag("scholar_tab_3").assertDoesNotExist()
    }

    @Test fun askLight() {
        var sent: Triple<String, ScholarCategory, Boolean>? = null
        rule.show(false) { AskQuestionContent("Yusuf", onBack = {}) { t, c, a, done -> sent = Triple(t, c, a); done(null) } }
        rule.onNodeWithTag("scholar_send").assertIsNotEnabled()
        rule.onNodeWithTag("scholar_pick_FAMILY").performScrollTo().performClick()
        rule.onNodeWithTag("scholar_text").performTextInput("My parents want me to marry a cousin I don't feel ready for. How do I speak to them with respect?")
        rule.onNodeWithTag("scholar_ask").performTouchInput { swipeDown() }
        rule.shot("v112_scholar_ask_light")
        rule.onNodeWithTag("scholar_send").performScrollTo().performClick()
        assertEquals(ScholarCategory.FAMILY, sent!!.second); assertTrue(sent!!.third)   // anonymous by default
    }

    @Test fun askHoldsScamDark() {
        rule.show(true) { AskQuestionContent("Yusuf", onBack = {}, initialText = "Is it halal to double your money in 7 days with this scheme?") { _, _, _, _ -> } }
        rule.onNodeWithTag("scholar_hint").assertTextContains("guidelines", substring = true)
        rule.onNodeWithTag("scholar_send").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("scholar_ask").performTouchInput { swipeDown() }
        rule.shot("v112_scholar_ask_dark")
    }

    @Test fun answerDark() {
        var published: String? = null
        val s = Scholar("s1", "Shaykh Abdullah Hasan", "Imam · Leicester Central Mosque")
        val q = ScholarQuestion("o1", "Is it allowed to pray Fajr after sunrise if I overslept without meaning to?", ScholarCategory.SALAH, createdAt = ago(40))
        rule.show(true) { AnswerContent(q, s, onBack = {}) { t, done -> published = t; done(null) } }
        rule.onNodeWithTag("scholar_publish").assertIsNotEnabled()
        rule.onNodeWithTag("scholar_answer_text").performTextInput("Yes. Whoever sleeps through a prayer should pray it as soon as they wake. Do not feel guilty for sleep you didn't choose, and set an alarm for tomorrow.")
        rule.onNodeWithTag("scholar_publish").assertIsEnabled()
        rule.shot("v112_scholar_answer_dark")
        rule.onNodeWithTag("scholar_publish").performClick()
        assertNotNull(published)
    }

    @Test fun unavailable() {
        rule.show(false) { AskScholarContent(ScholarUi(error = CommunityError.OFFLINE_BACKEND), ScholarActions()) }
        rule.onNodeWithTag("scholar_notice").assertExists()
        rule.onNodeWithTag("scholar_search").assertDoesNotExist()
        rule.shot("v112_scholar_unavailable_light")
    }

    @Test fun ummahShortcutAndFlagDefault() {
        assertFalse(IslamicFlags.isOn(IslamicFlags.ASK_SCHOLAR))
        Reels2Flags.override = mapOf(IslamicFlags.ASK_SCHOLAR to true)
        var opened: DeenScreen? = null
        rule.show(false) { Column { UmmahCommunityShortcuts { opened = it } } }
        rule.onNodeWithTag("ummah_link_UMMAH_ASK_SCHOLAR").performClick()
        assertEquals(DeenScreen.UMMAH_ASK_SCHOLAR, opened)
    }
}
