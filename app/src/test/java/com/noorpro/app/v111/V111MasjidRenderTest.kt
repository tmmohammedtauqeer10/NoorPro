package com.noorpro.app.v111

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.noorpro.app.islamic.masjid.*
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
class V111MasjidRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val noor = Masjid("noor-blr", "Masjid-e-Noor", "12 Cubbon Road, Shivajinagar", "Bengaluru", null, null,
        mapOf("Fajr" to "05:15", "Dhuhr" to "13:30", "Asr" to "16:45", "Maghrib" to "18:22", "Isha" to "20:00"), "13:15", "Rights of neighbours in Islam", listOf("imam1"))
    private val list = listOf(noor,
        Masjid("falah", "Jamia Masjid Al-Falah", "Mission Road", "Bengaluru", jamaat = mapOf("Fajr" to "05:20", "Dhuhr" to "13:45", "Asr" to "17:00", "Maghrib" to "18:22", "Isha" to "20:15"), jummahTime = "13:30"),
        Masjid("taqwa", "Masjid At-Taqwa", "4th Block, Jayanagar", "Bengaluru", jamaat = mapOf("Fajr" to "05:10", "Dhuhr" to "13:15", "Asr" to "16:30", "Maghrib" to "18:20", "Isha" to "19:45")),
        Masjid("rahma", "Masjid Ar-Rahma", "Frazer Town", "Bengaluru"),
    )
    private val ann = listOf(
        Announcement("a1", "Tafseer circle after Maghrib every Thursday — all are welcome. Sisters' section is open.", 1793030400000),
        Announcement("a2", "Isha jamaat moves to 8:00 PM from Sunday.", 1792771200000),
    )
    private val profile = MasjidProfileUi(noor, ann, following = true, nowMinutes = 15 * 60, isFriday = false)

    @Test fun homeLight() {
        var opened = ""
        rule.show(false) { MasjidHomeContent(MasjidHomeUi(list, followed = noor, nowMinutes = 15 * 60), MasjidActions(onOpen = { opened = it.id })) }
        rule.onNodeWithTag("masjid_row_taqwa").performClick()
        assertEquals("taqwa", opened)
        rule.shot("v111_masjid_home_light")
    }
    @Test fun homeEmptyBackendMissingDark() {
        rule.show(true) { MasjidHomeContent(MasjidHomeUi(error = MasjidError.OFFLINE_BACKEND, nowMinutes = 600), MasjidActions()) }
        rule.onNodeWithTag("masjid_notice").assertExists()
        rule.onNodeWithTag("masjid_empty").assertExists()
        rule.shot("v111_masjid_unavailable_dark")
    }
    @Test fun searchFiltersDark() {
        rule.show(true) { MasjidHomeContent(MasjidHomeUi(list, followed = noor, query = "taqwa", nowMinutes = 15 * 60, moderator = true, pendingCount = 2), MasjidActions()) }
        rule.onNodeWithTag("masjid_row_taqwa").assertExists()
        rule.onNodeWithTag("masjid_row_falah").assertDoesNotExist()
        rule.onNodeWithTag("masjid_review_open").assertExists()
        rule.shot("v111_masjid_search_dark")
    }
    @Test fun profileLight() {
        var follow: Boolean? = null; var dir = false
        rule.show(false) { MasjidProfileContent(profile, MasjidActions(onFollow = { follow = it }, onDirections = { dir = true })) }
        rule.onNodeWithTag("masjid_follow").performClick(); assertEquals(false, follow)
        rule.onNodeWithTag("masjid_directions").performClick(); assertTrue(dir)
        rule.onNodeWithTag("masjid_jamaat").assertExists(); rule.onNodeWithText("NEXT").assertExists()
        rule.shot("v111_masjid_profile_light")
    }
    @Test fun profileDarkManager() {
        rule.show(true) { MasjidProfileContent(profile.copy(following = false, canManage = true, isFriday = true), MasjidActions()) }
        rule.onNodeWithTag("masjid_edit").assertExists()
        rule.shot("v111_masjid_profile_dark")
    }
    @Test fun claimFlowLight() {
        var sent: List<String>? = null
        rule.show(false) { MasjidClaimContent(list[3], "Abdul Kareem", false, null, {}) { a, b, c, d -> sent = listOf(a, b, c, d) } }
        rule.onNodeWithTag("claim_submit").performClick()
        rule.onNodeWithTag("claim_problem").assertExists()
        rule.onNodeWithTag("claim_role_imam").performClick()
        rule.onNodeWithTag("claim_contact").performTextInput("+91 98450 12345")
        rule.shot("v111_masjid_claim_light")
        rule.onNodeWithTag("claim_submit").performClick()
        assertEquals(listOf("Abdul Kareem", "Imam", "+91 98450 12345", ""), sent)
    }
    @Test fun editDark() {
        var saved: Map<String, String>? = null; var jt = ""
        rule.show(true) { MasjidEditContent(noor, false, null, {}, { j, t, _ -> saved = j; jt = t }, {}) }
        rule.onNodeWithTag("masjid_t_Fajr").performTextClearance()
        rule.onNodeWithTag("masjid_t_Fajr").performTextInput("5:20")
        rule.shot("v111_masjid_edit_dark")
        rule.onNodeWithTag("masjid_save").performScrollTo().performClick()
        assertEquals("05:20", saved?.get("Fajr")); assertEquals("20:00", saved?.get("Isha")); assertEquals("13:15", jt)
    }
    @Test fun reviewLight() {
        val c = MasjidClaim("rahma_u1", "rahma", "Masjid Ar-Rahma", "u1", "Abdul Kareem", "Imam", "+91 98450 12345", "Imam here since 2019; the committee secretary can confirm.")
        var decided: Boolean? = null
        rule.show(false) { MasjidReviewContent(listOf(c), {}) { _, ok -> decided = ok } }
        rule.onNodeWithTag("claim_approve_rahma_u1").performClick(); assertEquals(true, decided)
        rule.shot("v111_masjid_review_light")
    }
    @Test fun prayerCardBothThemes() {
        rule.show(false) { Column(Modifier.padding(16.dp)) { FollowedMasjidJamaatCard(noor, {}) } }
        rule.onNodeWithTag("prayer_masjid_jamaat").assertExists()
        rule.shot("v111_masjid_prayercard_light")
    }
    @Test fun prayerCardDark() {
        rule.show(true) { Column(Modifier.padding(16.dp)) { FollowedMasjidJamaatCard(noor, {}) } }
        rule.shot("v111_masjid_prayercard_dark")
    }
}
