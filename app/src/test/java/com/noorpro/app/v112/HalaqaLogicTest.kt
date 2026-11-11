package com.noorpro.app.v112

import com.noorpro.app.islamic.social.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class HalaqaLogicTest {
    private val tz = TimeZone.getTimeZone("Asia/Kolkata")
    private fun m(uid: String, role: HalaqaRole = HalaqaRole.LISTENER, hand: Boolean = false, muted: Boolean = true, at: Long = 0) = HalaqaMember(uid, uid, role, hand, muted, at)

    @Test fun meshLinksOnlyTouchTheStage() {
        val all = listOf(m("host", HalaqaRole.HOST), m("sp", HalaqaRole.SPEAKER), m("l1"), m("l2"))
        assertEquals(listOf("host", "sp"), HalaqaLogic.neededPeers(all[2], all))       // listener ↔ stage only
        assertEquals(listOf("l1", "l2", "sp"), HalaqaLogic.neededPeers(all[0], all))   // host ↔ everyone
        // Total links = stage pairs + stage×audience, never listener↔listener.
        val links = all.flatMap { me -> HalaqaLogic.neededPeers(me, all).map { HalaqaLogic.linkId(me.uid, it) } }.toSet()
        assertEquals(5, links.size)
        assertFalse("l1_l2" in links)
    }

    @Test fun offererIsAgreedWithoutSignalling() {
        assertTrue(HalaqaLogic.isOfferer("a1", "b2")); assertFalse(HalaqaLogic.isOfferer("b2", "a1"))
        assertEquals("a1_b2", HalaqaLogic.linkId("b2", "a1")); assertEquals(HalaqaLogic.linkId("a1", "b2"), HalaqaLogic.linkId("b2", "a1"))
    }

    @Test fun micOnlyOnStageAndUnmuted() {
        assertTrue(HalaqaLogic.micLive(m("s", HalaqaRole.SPEAKER, muted = false)))
        assertFalse(HalaqaLogic.micLive(m("s", HalaqaRole.SPEAKER, muted = true)))
        assertFalse(HalaqaLogic.micLive(m("l", HalaqaRole.LISTENER, muted = false)))
        assertFalse(HalaqaLogic.micLive(null))
    }

    @Test fun capsAndOrdering() {
        val full = (1..HalaqaLogic.MAX_MEMBERS).map { m("u$it") }
        assertFalse(HalaqaLogic.canJoin(full, "new")); assertTrue(HalaqaLogic.canJoin(full, "u3"))
        val stage = (1..HalaqaLogic.MAX_SPEAKERS).map { m("s$it", HalaqaRole.SPEAKER) }
        assertFalse(HalaqaLogic.canInvite(stage)); assertTrue(HalaqaLogic.canInvite(stage.drop(1)))
        val mixed = listOf(m("s", HalaqaRole.SPEAKER, at = 1), m("h", HalaqaRole.HOST, at = 5), m("a", at = 2), m("b", hand = true, at = 9))
        assertEquals(listOf("h", "s"), HalaqaLogic.stage(mixed).map { it.uid })
        assertEquals(listOf("b", "a"), HalaqaLogic.audience(mixed).map { it.uid })
        assertEquals(listOf("b"), HalaqaLogic.hands(mixed).map { it.uid })
    }

    @Test fun noTurnMeansNeedsRelayNotFailure() {
        assertEquals(HalaqaLink.IDLE, HalaqaLogic.health(emptyList(), false))
        assertEquals(HalaqaLink.NEEDS_RELAY, HalaqaLogic.health(listOf(HalaqaLink.CONNECTED, HalaqaLink.FAILED), hasTurn = false))
        assertEquals(HalaqaLink.FAILED, HalaqaLogic.health(listOf(HalaqaLink.CONNECTED, HalaqaLink.FAILED), hasTurn = true))
        assertEquals(HalaqaLink.CONNECTING, HalaqaLogic.health(listOf(HalaqaLink.CONNECTED, HalaqaLink.CONNECTING), false))
        assertEquals(HalaqaLink.CONNECTED, HalaqaLogic.health(listOf(HalaqaLink.CONNECTED), false))
    }

    @Test fun schedulingAndReminders() {
        val now = 1_793_000_000_000L // a fixed instant
        val tonight = HalaqaLogic.slot(now, 0, 20, 0, tz)
        val tomorrow = HalaqaLogic.slot(now, 1, 6, 0, tz)
        val r = HalaqaRoom("r", "Tafseer", scheduledAt = tonight)
        assertEquals(tonight - 10 * 60_000L, HalaqaLogic.remindAt(r))
        assertEquals("Tomorrow, 6:00 AM", HalaqaLogic.whenLine(r.copy(scheduledAt = tomorrow), now, tz))
        assertTrue(HalaqaLogic.whenLine(r, now, tz).endsWith("8:00 PM"))
        assertEquals("Live now", HalaqaLogic.whenLine(r.copy(status = HalaqaStatus.LIVE), now, tz))
        assertNotNull(HalaqaLogic.validateTitle("ab")); assertNull(HalaqaLogic.validateTitle("Surah Al-Mulk circle"))
    }

    @Test fun listingHidesStaleRooms() {
        val now = 100L * 3_600_000L
        val rooms = listOf(
            HalaqaRoom("live", "Live", status = HalaqaStatus.LIVE, scheduledAt = now - 3_600_000L),
            HalaqaRoom("stale", "Forgotten", status = HalaqaStatus.LIVE, scheduledAt = now - 20 * 3_600_000L),
            HalaqaRoom("later", "Later", scheduledAt = now + 7_200_000L),
            HalaqaRoom("soon", "Soon", scheduledAt = now + 600_000L),
            HalaqaRoom("missed", "Missed", scheduledAt = now - 3 * 3_600_000L),
        )
        val (live, soon) = HalaqaLogic.listed(rooms, now)
        assertEquals(listOf("live"), live.map { it.id }); assertEquals(listOf("soon", "later"), soon.map { it.id })
    }

    @Test fun parsesFirestoreShapes() {
        val ts = com.google.firebase.Timestamp(java.util.Date(1_000_000L))
        val r = HalaqaLogic.roomFrom("x", mapOf("title" to "Naat night", "topic" to "NAAT", "status" to "live", "hostUid" to "h", "scheduledAt" to ts))
        assertEquals(HalaqaTopic.NAAT, r.topic); assertEquals(HalaqaStatus.LIVE, r.status); assertEquals(1_000_000L, r.scheduledAt)
        val m = HalaqaLogic.memberFrom("u", mapOf("name" to "Ali", "role" to "speaker", "hand" to true, "muted" to false))
        assertEquals(HalaqaRole.SPEAKER, m.role); assertTrue(m.hand); assertFalse(m.muted)
        assertEquals(HalaqaRole.LISTENER, HalaqaLogic.memberFrom("u", emptyMap()).role)
    }
}
