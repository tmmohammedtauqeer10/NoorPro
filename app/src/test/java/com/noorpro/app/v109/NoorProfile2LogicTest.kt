package com.noorpro.app.v109

import com.noorpro.app.data.UmmahProfile
import com.noorpro.app.ui.screens.*
import org.junit.Assert.*
import org.junit.Test

class NoorProfile2LogicTest {
    private val known = listOf(UmmahProfile("u1", "Aisha", "@aisha.k", ""), UmmahProfile("me", "Me", "tauqeer", ""))

    @Test fun usernameAvailability() {
        assertEquals(UsernameStatus.UNCHANGED, usernameStatus("@Tauqeer", "me", "tauqeer", known))
        assertEquals(UsernameStatus.TAKEN, usernameStatus("Aisha.K", "me", "tauqeer", known))
        assertEquals(UsernameStatus.AVAILABLE, usernameStatus("noor_learner", "me", "tauqeer", known))
        assertEquals(UsernameStatus.TOO_SHORT, usernameStatus("ab", "me", "tauqeer", known))
        assertEquals(UsernameStatus.TOO_LONG, usernameStatus("a".repeat(25), "me", "tauqeer", known))
        assertEquals(UsernameStatus.INVALID, usernameStatus("bad name", "me", "tauqeer", known))
        assertEquals(UsernameStatus.INVALID, usernameStatus(".dot", "me", "tauqeer", known))
        assertEquals(UsernameStatus.INVALID, usernameStatus("a..b", "me", "tauqeer", known))
        assertEquals(UsernameStatus.RESERVED, usernameStatus("Admin", "me", "tauqeer", known))
        assertTrue(UsernameStatus.AVAILABLE.ok); assertFalse(UsernameStatus.TAKEN.ok)
        assertEquals("aisha", normalizeUsername(" @Aisha "))
    }

    @Test fun tabsSavedOnlyOwnSeriesOnlyWhenFlagged() {
        assertEquals(listOf(Profile2Tab.POSTS, Profile2Tab.REELS, Profile2Tab.SAVED, Profile2Tab.TAGGED), profile2Tabs(own = true, seriesEnabled = false))
        assertEquals(listOf(Profile2Tab.POSTS, Profile2Tab.REELS, Profile2Tab.SERIES, Profile2Tab.TAGGED), profile2Tabs(own = false, seriesEnabled = true))
    }

    @Test fun privateAccountsLockForNonFollowers() {
        val priv = UmmahProfile("x", "X", "x", "", isPrivate = true)
        assertFalse(canSeeProfileContent(priv, own = false, iFollow = false))
        assertTrue(canSeeProfileContent(priv, own = false, iFollow = true))
        assertTrue(canSeeProfileContent(priv, own = true, iFollow = false))
        assertTrue(canSeeProfileContent(priv.copy(isPrivate = false), own = false, iFollow = false))
    }

    @Test fun postsReelsTaggedSeries() {
        val all = listOf(
            post("p1", "hello", type = "image", uid = "me", at = 1), post("p2", "pinned", type = "image", uid = "me", at = 0, pinned = true),
            post("p3", "old", type = "image", uid = "me", at = 5, archived = true), post("r1", "Seerah — Part 2/3", uid = "me", at = 3),
            post("r2", "Seerah — Part 1/3", uid = "me", at = 2), post("s1", "short", type = "short", uid = "me", at = 9),
            post("t1", "Great talk by @tauqeer!", uid = "o", at = 4), post("t2", "mail tauqeer@x.com", uid = "o"), post("t3", "@tauqeer_2 hi", uid = "o"),
        )
        assertEquals(listOf("p2", "p1"), profilePosts(all, "me", viewerIsOwner = false).map { it.id })
        assertEquals(listOf("p2", "p3", "p1"), profilePosts(all, "me", viewerIsOwner = true).map { it.id })
        assertEquals(listOf("s1", "r1", "r2"), profileReels(all, "me", false).map { it.id })
        assertEquals(listOf("t1"), taggedPosts(all, "me", "@Tauqeer").map { it.id })
        assertTrue(taggedPosts(all, "me", "").isEmpty())
        val series = profileSeries(all, "me")
        assertEquals(1, series.size); assertEquals("Seerah", series[0].first); assertEquals(listOf("r2", "r1"), series[0].second.map { it.id })
    }

    @Test fun statsRespectHideCountsForOthersOnly() {
        val other = profile2Stats(12, 1500, 30, null, own = false, hideCounts = true)
        assertEquals(listOf("posts", "followers", "following"), other.map { it.key })
        assertEquals("—", other[1].value); assertTrue(other[1].hidden)
        val mine = profile2Stats(12, 1500, 30, 7, own = true, hideCounts = true)
        assertEquals("1.5K", mine[1].value); assertEquals("7d", mine[3].value)
    }

    @Test fun bioLinkSplitAndShareUrl() {
        val b = splitBioLink("Student of knowledge. noorpro.app/learn, Hyderabad")
        assertEquals("noorpro.app/learn", b.link); assertEquals("Student of knowledge. Hyderabad", b.text)
        assertEquals("https://x.org/a", splitBioLink("see https://x.org/a.").link)
        assertNull(splitBioLink("email me @name.com? no — just text").link?.takeIf { it.startsWith("@") })
        assertNull(splitBioLink("Just a learner").link)
        assertEquals("https://noorpro.app/learn", linkHref("noorpro.app/learn")); assertEquals("http://a.com", linkHref("http://a.com"))
        assertEquals("https://noor-pro-d87e3.web.app/u?uid=abc123", profileShareUrl("abc123"))
    }

    @Test fun aiBioPromptAndParsing() {
        val p = Profile2AiPrompts.bio("Tauqeer", "Learning Quran", "")
        assertTrue(p.contains("3") && p.contains("no emoji") && p.contains("Quran, learning"))
        val parsed = Profile2AiPrompts.parseBios("Here you go:\n1. \"Seeking knowledge, one ayah at a time\"\n2) Student of Quran\n3. Grateful learner\n4. extra\nFOLLOWUPS: more")
        assertEquals(listOf("Seeking knowledge, one ayah at a time", "Student of Quran", "Grateful learner"), parsed)
        assertEquals(listOf("A plain line"), Profile2AiPrompts.parseBios("A plain line"))
    }

    @Test fun weeklyRecapHeadlines() {
        val now = 10L * 86_400_000
        val posts = listOf(post("a", at = now - 1000), post("b", at = now - 9L * 86_400_000))
        assertTrue(weeklyDeenRecap(7, posts, 3, now).headline.startsWith("A full week"))
        assertTrue(weeklyDeenRecap(4, posts, 3, now).headline.startsWith("4 days"))
        val r = weeklyDeenRecap(0, posts, 3, now)
        assertEquals(1, r.postsThisWeek); assertEquals("You shared 1 reminder with the Ummah.", r.headline)
        assertTrue(weeklyDeenRecap(0, emptyList(), 0, now).headline.startsWith("A new week"))
    }
}
