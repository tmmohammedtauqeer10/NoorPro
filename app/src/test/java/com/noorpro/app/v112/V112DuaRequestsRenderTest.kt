package com.noorpro.app.v112

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.social.*
import com.noorpro.app.ui.islamic.*
import com.noorpro.app.ui.screens.Reels2Flags
import com.noorpro.app.ui.viewmodel.DeenScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.runtime.*

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V112DuaRequestsRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun setUp() = loadHalalWords()
    @After fun tearDown() { Reels2Flags.override = null }

    private val requests = listOf(
        DuaRequest("d1", "My mother has heart surgery on Thursday morning. Please make dua for shifa and for the doctors' hands to be guided.", DuaCategory.HEALTH,
            name = "Aisha Rahman", authorUid = "aisha", ameenCount = 128, prayedCount = 41, createdAt = ago(95)),
        DuaRequest("d2", "Please pray that I find halal work soon. It has been seven months and my family is depending on me.", DuaCategory.RIZQ,
            anonymous = true, ameenCount = 56, createdAt = ago(5 * 60)),
        DuaRequest("d3", "My father passed away last night. Please remember him in your duas, that Allah forgives him and makes his grave spacious.", DuaCategory.DEPARTED,
            name = "Bilal Ahmed", authorUid = "bilal", ameenCount = 302, prayedCount = 87, createdAt = ago(14 * 60)),
        DuaRequest("d4", "Final exams start Monday. Make dua for barakah in my time and a calm heart.", DuaCategory.STUDIES,
            name = "Zainab", authorUid = "zainab", ameenCount = 12, prayedCount = 3, answered = true, createdAt = ago(3 * 24 * 60)),
    )
    private val mine = listOf(
        DuaRequest("m1", "Make dua that Allah grants my sister an easy delivery and a healthy baby.", DuaCategory.FAMILY, anonymous = true, ameenCount = 23, prayedCount = 9, createdAt = ago(6 * 60)),
        DuaRequest("m2", "Please pray for my acceptance into the Hifz programme this year.", DuaCategory.STUDIES, name = "Yusuf", authorUid = "me", ameenCount = 7, answered = true, createdAt = ago(9 * 24 * 60)),
    )

    @Test fun feedLight() {
        var responded: Pair<String, DuaResponse>? = null
        rule.show(false) {
            var ui by remember { mutableStateOf(DuaFeedUi(requests, mine, responded = setOf("d3/ameen"), myUid = "me")) }
            DuaFeedContent(ui, DuaActions(onRespond = { d, r, on -> responded = d.id to r
                ui = ui.copy(requests = ui.requests.map { if (it.id == d.id) DuaLogic.toggle(it, r, on) else it }, responded = if (on) ui.responded + "${d.id}/${r.key}" else ui.responded - "${d.id}/${r.key}") }))
        }
        rule.onNodeWithTag("dua_ameen_d1").performScrollTo().performClick()
        assertEquals("d1" to DuaResponse.AMEEN, responded)
        rule.onNodeWithTag("dua_count_d1").assertTextEquals("129 Ameen · 41 prayed")
        rule.onNodeWithTag("dua_feed").performTouchInput { swipeDown() }
        rule.onNodeWithTag("dua_compose_open").performScrollTo()
        rule.shot("v112_dua_feed_light")
    }

    @Test fun feedDark() {
        rule.show(true) { DuaFeedContent(DuaFeedUi(requests, mine, responded = setOf("d1/prayed", "d3/ameen"), myUid = "me"), DuaActions()) }
        rule.onNodeWithTag("dua_card_d2").assertExists()
        rule.shot("v112_dua_feed_dark")
    }

    @Test fun categoryFilterLight() {
        rule.show(false) {
            var ui by remember { mutableStateOf(DuaFeedUi(requests, myUid = "me")) }
            DuaFeedContent(ui, DuaActions(onCategory = { ui = ui.copy(category = it) }))
        }
        rule.onNodeWithTag("dua_cat_DEPARTED").performScrollTo().performClick()
        rule.onNodeWithTag("dua_card_d3").assertExists()
        rule.onNodeWithTag("dua_card_d1").assertDoesNotExist()
        rule.shot("v112_dua_category_light")
    }

    @Test fun myRequestsDark() {
        var answered = ""
        rule.show(true) { DuaFeedContent(DuaFeedUi(requests, mine, tab = 1, myUid = "me"), DuaActions(onAnswered = { answered = it.id })) }
        rule.onNodeWithTag("dua_answered_m1").performScrollTo().performClick()
        assertEquals("m1", answered)
        rule.onNodeWithTag("dua_answered_m2").assertDoesNotExist()
        rule.onNodeWithTag("dua_feed").performTouchInput { swipeDown(); swipeDown() }
        rule.shot("v112_dua_mine_dark")
    }

    @Test fun composeLight() {
        var posted: Triple<String, DuaCategory, Boolean>? = null
        rule.show(false) { DuaComposeContent("Yusuf Khan", onBack = {}) { t, c, a, done -> posted = Triple(t, c, a); done(null) } }
        rule.onNodeWithTag("dua_post").assertIsNotEnabled()
        rule.onNodeWithTag("dua_text").performTextInput("Please make dua for my grandmother's recovery after her fall.")
        rule.onNodeWithTag("dua_pick_FAMILY").performScrollTo().performClick()
        rule.onNodeWithTag("dua_anon").performClick()
        rule.onNodeWithTag("dua_post").performScrollTo().performClick()
        assertEquals(Triple("Please make dua for my grandmother's recovery after her fall.", DuaCategory.FAMILY, true), posted)
        rule.onNodeWithTag("dua_compose").performTouchInput { swipeDown() }
        rule.shot("v112_dua_compose_light")
    }

    @Test fun composeWarningDark() {
        rule.show(true) { DuaComposeContent("Yusuf Khan", onBack = {}, initialText = "Make dua for me, and earn 5000 daily from home with my link") { _, _, _, _ -> } }
        rule.onNodeWithTag("dua_hint").assert(hasText("possible scam", substring = true))
        rule.onNodeWithTag("dua_post").assertIsNotEnabled()
        rule.shot("v112_dua_compose_dark")
    }

    @Test fun unavailableDark() {
        rule.show(true) { DuaFeedContent(DuaFeedUi(error = CommunityError.OFFLINE_BACKEND), DuaActions()) }
        rule.onNodeWithTag("dua_notice").assertExists()
        rule.shot("v112_dua_unavailable_dark")
    }

    @Test fun ummahShortcutRespectsFlag() {
        Reels2Flags.override = mapOf(IslamicFlags.DUA_REQUESTS to true)
        var opened: DeenScreen? = null
        rule.show(false) { Column { UmmahCommunityShortcuts { opened = it } } }
        rule.onNodeWithTag("ummah_link_UMMAH_DUA_REQUESTS").performClick()
        assertEquals(DeenScreen.UMMAH_DUA_REQUESTS, opened)
        Reels2Flags.override = mapOf(IslamicFlags.DUA_REQUESTS to false)
        assertEquals(false, UmmahCommunityLinks.any())
    }

    @Test fun reportDialogLight() {
        var reason = ""
        rule.show(false) { IkReportDialog("dua request", onDismiss = {}) { reason = it } }
        rule.onNodeWithTag("report_reason_Spam or scam").performClick()
        rule.onNodeWithTag("report_send").performClick()
        assertEquals("Spam or scam", reason)
    }
}
