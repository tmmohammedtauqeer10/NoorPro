package com.noorpro.app.messaging

import com.noorpro.app.data.messaging.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class MessagingLogicTest {
    private val people = MessagingSamples.people
    private val chats = MessagingSamples.chats(1_000_000_000L)
    private val groups = MessagingSamples.discover()

    @Test fun exactNameFirstThenPrefixRankedByRelationship() {
        val r = UnifiedSearch.search("Amina", chats, people, groups, MessagingSamples.ME)
        assertEquals(listOf("u9"), r.best.map { it.id })
        assertEquals(listOf("u1", "u6"), r.people.map { it.id })   // mutual follow outranks stranger
    }

    @Test fun exactHandleWithAtSignIsBestMatch() {
        val r = UnifiedSearch.search("@bilal.h", chats, people, groups, MessagingSamples.ME)
        assertEquals("u2", r.best.single().id)
        assertTrue(r.chats.none { it.id == "c2" } || r.best.single().kind == UnifiedSearch.Kind.PERSON)
    }

    @Test fun privateGroupsOnlyForMembers() {
        val priv = groups.first { it.visibility == GroupVisibility.PRIVATE }
        assertTrue(UnifiedSearch.search("Seerah", chats, people, groups, MessagingSamples.ME).groups.any { it.id == priv.id })
        assertTrue(UnifiedSearch.search("Seerah", chats, people, groups, "stranger").groups.none { it.id == priv.id })
        assertTrue(GroupDiscovery.browse(groups, null).none { it.visibility == GroupVisibility.PRIVATE })
    }

    @Test fun discoveryByTopicBiggestFirst() {
        val q = GroupDiscovery.browse(groups, "quran")
        assertTrue(q.all { it.topicId == "quran" })
        assertEquals(q.sortedByDescending { it.memberCount }, q)
        assertEquals("d1", GroupDiscovery.browse(groups, null, "#riyadussalihin").first().id)
    }

    @Test fun permissions() {
        val g = MessagingSamples.groups()[0]   // owner me, admin u1
        assertTrue(GroupPermissions.canRemove(g, "me", "u1"))
        assertFalse(GroupPermissions.canRemove(g, "u1", "me"))
        assertTrue(GroupPermissions.canRemove(g, "u1", "u2"))
        assertFalse(GroupPermissions.canRemove(g, "u2", "u3"))
        assertFalse(GroupPermissions.canLeave(g, "me"))
        assertTrue(GroupPermissions.canLeave(g, "u2"))
        assertTrue(GroupPermissions.canDeleteMessage(g, "u1", "u3"))
        assertFalse(GroupPermissions.canDeleteMessage(g, "u2", "u3"))
        assertEquals(GroupPermissions.JoinAction.OPEN, GroupPermissions.joinAction(g, "me"))
        assertEquals(GroupPermissions.JoinAction.REQUEST, GroupPermissions.joinAction(g, "x"))
        assertEquals(GroupPermissions.JoinAction.REQUESTED, GroupPermissions.joinAction(g, "x", requested = true))
    }

    @Test fun islamicTouches() {
        assertEquals("Wa alaikum assalam", IslamicTouches.quickReplies("Assalamu alaikum brother").first())
        assertEquals("Assalamu alaikum", IslamicTouches.quickReplies("hey").first())
        val fri = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 23) }
        assertEquals(IslamicTouches.Moment.JUMUAH, IslamicTouches.moment(fri, 5))
        assertEquals(IslamicTouches.Moment.RAMADAN, IslamicTouches.moment(fri, 9))
    }

    @Test fun counts() {
        assertEquals("842", formatCount(842)); assertEquals("12.4K", formatCount(12_400)); assertEquals("2K", formatCount(2_000))
    }
}
