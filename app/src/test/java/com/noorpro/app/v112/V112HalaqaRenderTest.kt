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
class V112HalaqaRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun setUp() = loadHalalWords()
    @After fun tearDown() { Reels2Flags.override = null }

    private val rooms = listOf(
        HalaqaRoom("r1", "Tafseer of Surah Al-Kahf — verses 1 to 10", HalaqaTopic.DARS, "A Friday reflection on the opening verses and the young men of the cave.",
            "hana", "Ustadha Hana Siddiqui", HalaqaStatus.LIVE, ago(25)),
        HalaqaRoom("r2", "Tajweed circle: Surah Al-Mulk", HalaqaTopic.QURAN, "", "omar", "Qari Omar Farooq", HalaqaStatus.LIVE, ago(50)),
        HalaqaRoom("r3", "Evening adhkar together", HalaqaTopic.DHIKR, "", "maryam", "Maryam Khan", HalaqaStatus.SCHEDULED, NOW + 3 * 3_600_000L),
        HalaqaRoom("r4", "Seerah Q&A for new Muslims", HalaqaTopic.QA, "Bring your questions about the life of the Prophet ﷺ.", "yusuf", "Yusuf Ali", HalaqaStatus.SCHEDULED, NOW + 26 * 3_600_000L),
    )
    private fun m(uid: String, name: String, role: HalaqaRole = HalaqaRole.LISTENER, hand: Boolean = false, muted: Boolean = true, at: Long = 0) = HalaqaMember(uid, name, role, hand, muted, at)
    private val members = listOf(
        m("hana", "Hana Siddiqui", HalaqaRole.HOST, muted = false, at = 1), m("ibrahim", "Ibrahim Patel", HalaqaRole.SPEAKER, muted = false, at = 2),
        m("sara", "Sara Malik", HalaqaRole.SPEAKER, at = 3), m("me", "Yusuf", at = 4), m("ali", "Ali Hassan", hand = true, at = 5),
        m("fatima", "Fatima Noor", at = 6), m("khalid", "Khalid Omar", at = 7), m("amina", "Amina Yusuf", hand = true, at = 8), m("zaid", "Zaid Ahmed", at = 9),
    )

    @Test fun homeLight() {
        var reminded: Pair<String, Boolean>? = null
        rule.show(false) {
            var ui by remember { mutableStateOf(HalaqaHomeUi(rooms, reminders = setOf("r4"), now = NOW)) }
            HalaqaHomeContent(ui, HalaqaHomeActions(onRemind = { r, on -> reminded = r.id to on; ui = ui.copy(reminders = if (on) ui.reminders + r.id else ui.reminders - r.id) }))
        }
        rule.onNodeWithTag("halaqa_remind_r3").performScrollTo().performClick()
        assertEquals("r3" to true, reminded)
        rule.onNodeWithTag("halaqa_remind_r3").assertTextContains("Reminder on", substring = true)
        rule.onNodeWithTag("halaqa_home").performTouchInput { swipeDown() }
        rule.onNodeWithTag("halaqa_create_open").performScrollTo()
        rule.shot("v112_halaqa_home_light")
    }

    @Test fun homeDark() {
        rule.show(true) { HalaqaHomeContent(HalaqaHomeUi(rooms, reminders = setOf("r4"), now = NOW), HalaqaHomeActions()) }
        rule.shot("v112_halaqa_home_dark")
    }

    @Test fun homeUnavailable() {
        rule.show(false) { HalaqaHomeContent(HalaqaHomeUi(error = CommunityError.OFFLINE_BACKEND), HalaqaHomeActions()) }
        rule.onNodeWithTag("halaqa_notice").assertExists()
        rule.onNodeWithTag("halaqa_live_empty").assertDoesNotExist()
        rule.shot("v112_halaqa_unavailable")
    }

    @Test fun listenerRoomLight() {
        var hand: Boolean? = null
        rule.show(false) {
            var ui by remember { mutableStateOf(HalaqaRoomUi(rooms[0], members, myUid = "me", link = HalaqaLink.CONNECTED, now = NOW)) }
            HalaqaRoomContent(ui, HalaqaRoomActions(onHand = { up -> hand = up; ui = ui.copy(members = ui.members.map { if (it.uid == "me") it.copy(hand = up) else it }) }))
        }
        rule.onNodeWithTag("halaqa_mic").assertDoesNotExist()   // listeners can't unmute
        rule.onNodeWithTag("halaqa_hand").performClick()
        assertEquals(true, hand)
        rule.onNodeWithText("Lower hand").assertExists()
        rule.onNodeWithTag("halaqa_invite_ali").assertDoesNotExist() // only the host sees hands
        rule.shot("v112_halaqa_room_listener_light")
    }

    @Test fun hostRoomDark() {
        val invited = mutableListOf<String>()
        rule.show(true) {
            HalaqaRoomContent(HalaqaRoomUi(rooms[0], members, myUid = "hana", link = HalaqaLink.CONNECTED, now = NOW), HalaqaRoomActions(onInvite = { invited += it.uid }))
        }
        rule.onNodeWithTag("halaqa_mic").assertExists()
        rule.onNodeWithTag("halaqa_end").assertExists()
        rule.onNodeWithTag("halaqa_invite_ali").performScrollTo().performClick()
        rule.onNodeWithTag("halaqa_member_fatima").performScrollTo().performClick()
        rule.onNodeWithTag("halaqa_menu_invite").performClick()
        assertEquals(listOf("ali", "fatima"), invited)
        rule.onNodeWithTag("halaqa_room").performTouchInput { swipeDown() }
        rule.shot("v112_halaqa_room_host_dark")
    }

    @Test fun hostMenuForSpeakerAndEndConfirm() {
        val did = mutableListOf<String>()
        rule.show(false) {
            HalaqaRoomContent(HalaqaRoomUi(rooms[0], members, myUid = "hana", now = NOW),
                HalaqaRoomActions(onMute = { did += "mute:${it.uid}" }, onMoveDown = { did += "down:${it.uid}" }, onEnd = { did += "end" }))
        }
        rule.onNodeWithTag("halaqa_member_ibrahim").performClick()
        rule.onNodeWithTag("halaqa_menu_mute").performClick()
        rule.onNodeWithTag("halaqa_member_sara").performClick()
        rule.onNodeWithTag("halaqa_menu_mute").assertDoesNotExist()   // already muted
        rule.onNodeWithTag("halaqa_menu_down").performClick()
        rule.onNodeWithTag("halaqa_end").performClick()
        rule.onNodeWithTag("halaqa_end_confirm").performClick()
        assertEquals(listOf("mute:ibrahim", "down:sara", "end"), did)
    }

    @Test fun needsRelayNoticeLight() {
        rule.show(false) { HalaqaRoomContent(HalaqaRoomUi(rooms[1], members.take(5), myUid = "ibrahim", link = HalaqaLink.NEEDS_RELAY, now = NOW), HalaqaRoomActions()) }
        rule.onNodeWithTag("halaqa_relay").assertExists()
        rule.onNodeWithTag("halaqa_stepdown").assertExists()
        rule.shot("v112_halaqa_needs_relay_light")
    }

    @Test fun scheduledAndEnded() {
        var remind: Boolean? = null
        rule.show(false) {
            var ui by remember { mutableStateOf(HalaqaRoomUi(rooms[2], myUid = "me", now = NOW)) }
            HalaqaRoomContent(ui, HalaqaRoomActions(onRemind = { remind = it; ui = ui.copy(reminded = it) }))
        }
        rule.onNodeWithTag("halaqa_golive").assertDoesNotExist()
        rule.onNodeWithTag("halaqa_room_remind").performClick()
        assertEquals(true, remind)
        rule.onNodeWithText("Reminder on").assertExists()
        rule.shot("v112_halaqa_scheduled_light")
    }

    @Test fun endedDark() {
        rule.show(true) { HalaqaRoomContent(HalaqaRoomUi(rooms[0].copy(status = HalaqaStatus.ENDED), myUid = "me", now = NOW), HalaqaRoomActions()) }
        rule.onNodeWithTag("halaqa_ended").assertExists()
        rule.onNodeWithTag("halaqa_leave").assertDoesNotExist()
        rule.shot("v112_halaqa_ended_dark")
    }

    @Test fun createScheduleLight() {
        var made: Triple<String, HalaqaTopic, Long?>? = null
        rule.show(false) {
            HalaqaCreateContent(onBack = {}, now = NOW, initialTitle = "Seerah night: the Hijrah") { t, topic, _, at, done -> made = Triple(t, topic, at); done(null) }
        }
        rule.onNodeWithTag("halaqa_topic_DARS").performClick()
        rule.onNodeWithTag("halaqa_when_1").performClick()
        rule.onNodeWithTag("halaqa_day_1").performClick()
        rule.onNodeWithTag("halaqa_time_20_0").performClick()
        rule.onNodeWithTag("halaqa_submit").performScrollTo().assertIsEnabled()
        rule.onNodeWithTag("halaqa_create").performTouchInput { swipeDown() }
        rule.shot("v112_halaqa_create_light")
        rule.onNodeWithTag("halaqa_submit").performScrollTo().performClick()
        assertEquals(HalaqaTopic.DARS, made!!.second)
        assertEquals(HalaqaLogic.slot(NOW, 1, 20, 0), made!!.third)
    }

    @Test fun createBlocksScamAndNow() {
        rule.show(true) { HalaqaCreateContent(onBack = {}, now = NOW, initialTitle = "Earn 5000 daily from home") { _, _, _, _, _ -> } }
        rule.onNodeWithTag("halaqa_hint").assertExists()
        rule.onNodeWithTag("halaqa_submit").assertIsNotEnabled()
        rule.onNodeWithTag("halaqa_title").performTextReplacement("Quran circle after Isha")
        rule.onNodeWithTag("halaqa_submit").assertIsEnabled().assertTextContains("Go live now")
        rule.shot("v112_halaqa_create_dark")
    }

    @Test fun ummahShortcutAndFlagDefault() {
        assertFalse(IslamicFlags.isOn(IslamicFlags.HALAQA_ROOMS))
        Reels2Flags.override = mapOf(IslamicFlags.HALAQA_ROOMS to true)
        var opened: DeenScreen? = null
        rule.show(false) { Column { UmmahCommunityShortcuts { opened = it } } }
        rule.onNodeWithTag("ummah_link_UMMAH_HALAQA").performClick()
        assertEquals(DeenScreen.UMMAH_HALAQA, opened)
        rule.onNodeWithTag("ummah_link_UMMAH_DUA_REQUESTS").assertDoesNotExist()
    }
}
