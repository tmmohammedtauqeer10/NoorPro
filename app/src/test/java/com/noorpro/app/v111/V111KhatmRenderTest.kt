package com.noorpro.app.v111

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.noorpro.app.islamic.khatm.*
import com.noorpro.app.ui.islamic.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V111KhatmRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val group = KhatmGroup("K7M2QP", "Family Ramadan Khatm", "For Nani's recovery", "me", "Tauqeer", listOf("me", "a", "b", "c", "d", "e"))
    private val names = listOf("Ayesha", "Bilal", "Fatima", "Yusuf", "Maryam")
    private fun claims(done: Int, claimed: Int): Map<Int, ParaClaim> {
        val m = mutableMapOf<Int, ParaClaim>()
        for (n in 1..done) m[n] = ParaClaim(n, if (n == 3) "me" else "u${n % 5}", if (n == 3) "Tauqeer" else names[n % 5], ParaStatus.DONE)
        for (n in done + 1..done + claimed) m[n] = ParaClaim(n, if (n == done + 2) "me" else "u${n % 5}", if (n == done + 2) "Tauqeer" else names[n % 5], ParaStatus.CLAIMED)
        return m
    }
    private val mine = listOf(KhatmGroup("K7M2QP", "Family Ramadan Khatm", "For Nani's recovery", "me", "", listOf("me", "a", "b", "c", "d", "e")),
        KhatmGroup("Q9RT4W", "Masjid Jumu'ah circle", "", "x", "", List(14) { "u$it" }))

    @Test fun homeLight() {
        var joined = ""
        rule.show(false) { KhatmHomeContent(KhatmHomeUi(groups = mine), {}, {}, { joined = it }, {}) }
        rule.onNodeWithTag("khatm_code").performTextInput("https://noor-pro-d87e3.web.app/k?c=h4kq9z")
        rule.onNodeWithTag("khatm_join").performClick()
        assertEquals("H4KQ9Z", joined)
        rule.onNodeWithTag("khatm_code").performTextClearance()
        rule.shot("v111_khatm_home_light")
    }
    @Test fun homeBackendMissingDark() {
        rule.show(true) { KhatmHomeContent(KhatmHomeUi(error = KhatmError.OFFLINE_BACKEND), {}, {}, {}, {}) }
        rule.onNodeWithTag("khatm_notice").assertExists()
        rule.shot("v111_khatm_unavailable_dark")
    }
    @Test fun createLight() { rule.show(false) { KhatmCreateContent(false, null, {}, { _, _ -> }) }; rule.shot("v111_khatm_create_light") }
    @Test fun groupLight() {
        var claimed = 0
        rule.show(false) { KhatmGroupContent(KhatmGroupUi(group, claims(12, 7), "me", selected = 25), {}, {}, {}, { claimed = it }, { _, _ -> }, {}, {}, {}) }
        rule.onNodeWithTag("khatm_progress").assertExists()
        rule.onNodeWithTag("khatm_claim").performClick()
        assertEquals(25, claimed)
        rule.shot("v111_khatm_group_light")
    }
    @Test fun groupDark() {
        rule.show(true) { KhatmGroupContent(KhatmGroupUi(group, claims(12, 7), "me"), {}, {}, {}, {}, { _, _ -> }, {}, {}, {}) }
        rule.onNodeWithTag("khatm_done").assertExists() // my claimed para is pre-selected
        rule.shot("v111_khatm_group_dark")
    }
    @Test fun groupGridDark() {
        rule.show(true) { KhatmGroupContent(KhatmGroupUi(group, claims(12, 7), "me", selected = 6), {}, {}, {}, {}, { _, _ -> }, {}, {}, {}) }
        rule.onNodeWithTag("khatm_group").performScrollToNode(hasTestTag("khatm_para_30"))
        rule.shot("v111_khatm_grid_dark")
    }
    @Test fun completeLight() {
        rule.show(false) { KhatmGroupContent(KhatmGroupUi(group, claims(30, 0), "me"), {}, {}, {}, {}, { _, _ -> }, {}, {}, {}) }
        rule.onNodeWithTag("khatm_dua_open").assertExists()
        rule.shot("v111_khatm_complete_light")
    }
    @Test fun duaLight() { rule.show(false) { KhatmDuaContent(group, {}, {}) }; rule.shot("v111_khatm_dua_light") }
    @Test fun duaDark() { rule.show(true) { KhatmDuaContent(group, {}, {}) }; rule.shot("v111_khatm_dua_dark") }
}
