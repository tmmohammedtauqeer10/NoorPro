package com.noorpro.app.halal

import com.noorpro.app.data.halal.*
import com.noorpro.app.ui.screens.UgcTerms
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class HalalGuardLogicTest {
    @After fun reset() { HalalFlags.override = null }

    // ---- moderator visibility (mirrors firestore.rules isModerator from f1a4ffa)
    @Test fun moderatorNeedsClaimOrAdminDoc() {
        assertTrue(ModeratorAccess.isModerator(true, false))
        assertTrue(ModeratorAccess.isModerator(null, true))
        assertFalse(ModeratorAccess.isModerator(null, false))
        assertFalse(ModeratorAccess.isModerator("true", false))
        assertFalse(ModeratorAccess.isModerator(1, false))
    }

    @Test fun panelHiddenUnlessSignedInModeratorAndFlag() {
        assertTrue(ModeratorAccess.showPanel(true, true, true))
        assertFalse(ModeratorAccess.showPanel(false, true, true))
        assertFalse(ModeratorAccess.showPanel(true, false, true))
        assertFalse(ModeratorAccess.showPanel(true, true, false))
        // default: flag off -> nobody sees it until the backend is deployed
        assertFalse(ModeratorAccess.showPanel(true, true, HalalFlags.moderatorPanel))
        HalalFlags.override = mapOf(HalalFlags.KEY_MOD_PANEL to true)
        assertTrue(ModeratorAccess.showPanel(true, true, HalalFlags.moderatorPanel))
    }

    @Test fun backendDependentFlagsDefaultOff() {
        assertFalse(HalalFlags.default(HalalFlags.KEY_MOD_PANEL))
        assertFalse(HalalFlags.default(HalalFlags.KEY_CIRCLES))
        assertFalse(HalalFlags.default(HalalFlags.KEY_NOTICES))
        assertTrue(HalalFlags.default(HalalFlags.KEY_TEXT_WARN))
        assertTrue(HalalFlags.default(HalalFlags.KEY_SAFETY))
        assertEquals(6, HalalFlags.ALL_KEYS.size)
        assertTrue(HalalFlags.ALL_KEYS.all { it.startsWith("halal_") && it.endsWith("_enabled") })
    }

    // ---- report reasons
    @Test fun islamicReportReasonsComeFirstAndExistingKept() {
        val r = UgcTerms.REPORT_REASONS
        assertEquals(listOf("Immodest / indecent", "Haram content", "Wrong Quran / hadith", "Sectarian hate / takfir", "Scam / fake charity"), r.take(5))
        assertTrue("Spam or scam" in r)
        assertTrue("Child safety concern" in r)
        assertEquals(r.size, r.toSet().size)
    }

    // ---- safety rules
    private data class C(val id: String, val other: String, val mine: Boolean = false)
    @Test fun messageRequestsFolder() {
        val chats = listOf(C("a", "friend"), C("b", "stranger"), C("c", "stranger2", mine = true), C("d", ""))
        val (inbox, req) = SafetyRules.splitRequests(chats, { it.other }, { it.mine }, setOf("friend"), MessageRequestMode.FOLLOWING)
        assertEquals(listOf("a", "c", "d"), inbox.map { it.id })
        assertEquals(listOf("b"), req.map { it.id })
        val (all, none) = SafetyRules.splitRequests(chats, { it.other }, { it.mine }, setOf("friend"), MessageRequestMode.EVERYONE)
        assertEquals(4, all.size); assertTrue(none.isEmpty())
    }

    @Test fun hideCommentsFromNonFollowers() {
        val cs = listOf("owner", "me", "follower", "stranger")
        assertEquals(cs, SafetyRules.visibleComments(cs, { it }, "owner", "me", setOf("follower"), false))
        assertEquals(listOf("owner", "me", "follower"), SafetyRules.visibleComments(cs, { it }, "owner", "me", setOf("follower"), true))
    }

    @Test fun circles() {
        assertTrue(SafetyRules.canJoin(CircleAudience.ALL, CircleGender.UNSET))
        assertTrue(SafetyRules.canJoin(CircleAudience.SISTERS, CircleGender.FEMALE))
        assertFalse(SafetyRules.canJoin(CircleAudience.SISTERS, CircleGender.MALE))
        assertFalse(SafetyRules.canJoin(CircleAudience.BROTHERS, CircleGender.UNSET))
        assertTrue(SafetyRules.joinBlockedReason(CircleAudience.BROTHERS, CircleGender.UNSET).contains("Safety settings"))
        assertEquals("", SafetyRules.joinBlockedReason(CircleAudience.ALL, CircleGender.MALE))
        assertEquals(CircleAudience.SISTERS, CircleAudience.of("sisters"))
        assertEquals(CircleAudience.ALL, CircleAudience.of(null))
        assertEquals(CircleAudience.ALL, CircleAudience.of("weird"))
    }

    // ---- blocked / queued post notice
    @Test fun noticeOnlyForGuardRejectionsAndReview() {
        assertEquals(HalalNotice("p1:rejected", false, "text_severe"), HalalNotices.noticeFor("p1", "rejected", "text_severe", true))
        assertNull(HalalNotices.noticeFor("p1", "rejected", "upload_failed", false))
        assertEquals(true, HalalNotices.noticeFor("p2", "in_review", "", false)?.queued)
        assertNull(HalalNotices.noticeFor("p3", "published", "", false))
        assertNull(HalalNotices.noticeFor("p3", "pending", "", false))
    }

    @Test fun strikesAndReasonLabels() {
        assertTrue(StrikeRecord(banned = true).restricted(0))
        assertTrue(StrikeRecord(3, suspendedUntilMs = 2000).restricted(1000))
        assertFalse(StrikeRecord(3, suspendedUntilMs = 500).restricted(1000))
        assertEquals("Possible nudity", ModerationText.reason("possible_adult"))
        assertEquals("Something new", ModerationText.reason("something_new"))
        assertEquals(listOf("approve", "remove", "warn", "suspend", "ban", "unban", "dismiss"), ModAction.entries.map { it.wire })
    }
}
