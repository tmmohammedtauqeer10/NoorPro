package com.noorpro.app

import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.search.PeopleRanker
import com.noorpro.app.data.search.PeopleRanker.Person
import com.noorpro.app.data.search.ReelSearchRanker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleSearchTest {
    private fun post(id: String, caption: String, creator: String = "c$id", publishedAt: Long = 1L) =
        UmmahPost(id = id, type = "reel", mediaUrl = "https://x/$id.mp4", creatorUid = creator, creatorName = "N", creatorHandle = "@h", caption = caption, sourceReference = "", thumbnailUrl = "", publishedAt = publishedAt, viewCount = 0)

    private val people = listOf(
        Person("1", "Ahmed Khan", "ahmedk", "", reels = 9),
        Person("2", "Ahmad Raza", "araza", "", following = true, followsMe = true),
        Person("3", "Bilal", "ahmed_official", "", following = true),
        Person("me", "Ahmed Me", "ahmedme", "")
    )

    @Test fun prefixOnNameAndHandleExcludesMe() {
        val r = PeopleRanker.search("ahmed", people, me = "me").map { it.uid }
        assertTrue("1" in r && "3" in r); assertTrue("me" !in r)
    }

    @Test fun fuzzyMatchesSpellingVariant() {
        assertTrue("2" in PeopleRanker.search("ahmad", people).map { it.uid })
        assertTrue("1" in PeopleRanker.search("ahmd khan", people).map { it.uid } || "1" in PeopleRanker.search("ahmd", people).map { it.uid })
    }

    @Test fun atHandleAndRelationOrdering() {
        assertEquals("3", PeopleRanker.search("@ahmed_off", people).first().uid)
        val same = listOf(Person("a", "Zaid", "zaid1", ""), Person("b", "Zaid", "zaid2", "", following = true, followsMe = true))
        assertEquals(listOf("b", "a"), PeopleRanker.search("zaid", same).map { it.uid })
        assertEquals("Mutual", same[1].subtitle)
    }

    @Test fun suggestedSkipsFollowedAndPrefersFollowers() {
        val s = PeopleRanker.suggested(listOf(Person("x", "X", "x", "", reels = 50), Person("y", "Y", "y", "", followsMe = true), Person("z", "Z", "z", "", following = true)))
        assertEquals(listOf("y", "x"), s.map { it.uid })
    }

    @Test fun trendingNeverEmptyWithoutHashtags() {
        val old = listOf(post("a", "Beautiful dua for parents"), post("b", "dua before sleep"))
        assertTrue(ReelSearchRanker.trending(old).isNotEmpty())
    }

    @Test fun relatedByHashtagAndCreator() {
        val m = listOf(post("a", "#dua morning", creator = "u1"))
        val pool = m + listOf(post("b", "#dua night"), post("c", "other", creator = "u1"), post("d", "unrelated"))
        val r = ReelSearchRanker.related(m, pool).map { it.id }
        assertTrue("b" in r && "c" in r && "a" !in r && "d" !in r)
    }
}
