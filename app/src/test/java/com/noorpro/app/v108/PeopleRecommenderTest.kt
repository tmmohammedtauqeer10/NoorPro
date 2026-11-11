package com.noorpro.app.v108

import com.noorpro.app.data.UmmahProfile
import com.noorpro.app.data.recs.PeopleRecommender
import com.noorpro.app.data.recs.RecommendationInput
import com.noorpro.app.data.recs.SuggestionKind
import com.noorpro.app.ummah.up
import org.junit.Assert.*
import org.junit.Test

class PeopleRecommenderTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L
    private fun prof(uid: String, name: String) = UmmahProfile(uid, name, uid, "")

    private val profiles = listOf("me", "bilal", "amina", "yusuf", "sara", "khalid", "blocked", "zaid").associateWith { prof(it, it.replaceFirstChar { c -> c.uppercase() } + " K") }

    private fun input(dismissed: Set<String> = emptySet()) = RecommendationInput(
        me = "me", following = setOf("bilal", "amina", "yusuf"), blocked = setOf("blocked"), dismissed = dismissed,
        profiles = profiles,
        followingOfFollowed = mapOf("bilal" to setOf("sara", "khalid", "me", "blocked"), "amina" to setOf("sara"), "yusuf" to setOf("sara")),
        posts = listOf(
            up(id = "q1", creatorUid = "bilal", creatorName = "Bilal", category = "Quran", publishedAt = now - day),
            up(id = "q2", creatorUid = "zaid", creatorName = "Zaid", category = "Quran", publishedAt = now - 2 * day),
            up(id = "q3", creatorUid = "zaid", creatorName = "Zaid", category = "Quran", publishedAt = now - 3 * day),
            up(id = "b1", creatorUid = "blocked", creatorName = "Bad", category = "Quran", publishedAt = now - day, likeCount = 9999),
        ),
        liked = setOf("q1"), saved = emptySet(), now = now,
    )

    @Test fun excludesSelfFollowedAndBlocked() {
        val ids = PeopleRecommender.recommend(input()).map { it.uid }
        assertFalse("me" in ids); assertFalse("blocked" in ids)
        listOf("bilal", "amina", "yusuf").forEach { assertFalse(it in ids) }
    }

    @Test fun mutualsRankFirstWithReasonLine() {
        val out = PeopleRecommender.recommend(input())
        assertEquals("sara", out.first().uid)
        assertEquals(SuggestionKind.MUTUAL, out.first().kind)
        assertEquals("Followed by Bilal + 2", out.first().reason)
        val khalid = out.first { it.uid == "khalid" }
        assertEquals("Followed by Bilal", khalid.reason)
    }

    @Test fun interestReasonForTopicCreators() {
        val zaid = PeopleRecommender.recommend(input()).first { it.uid == "zaid" }
        assertEquals(SuggestionKind.INTEREST, zaid.kind)
        assertEquals("Posts about Quran", zaid.reason)
    }

    @Test fun dismissedNeverReturns() {
        assertFalse("sara" in PeopleRecommender.recommend(input(setOf("sara"))).map { it.uid })
    }

    @Test fun nearbyWhenLocationExistsAndGracefulWithout() {
        val posts = listOf(
            up(id = "m", creatorUid = "me", publishedAt = now - day).copy(locationName = "Hyderabad"),
            up(id = "n", creatorUid = "newbie", creatorName = "Newbie", category = "", publishedAt = now - day).copy(locationName = "Hyderabad"),
        )
        val base = RecommendationInput(me = "me", following = emptySet(), blocked = emptySet(), posts = posts, now = now,
            myLocation = PeopleRecommender.myLocation("me", posts))
        val n = PeopleRecommender.recommend(base).single()
        assertEquals(SuggestionKind.NEARBY, n.kind)
        assertEquals("New near you · Hyderabad", n.reason)
        assertNotEquals(SuggestionKind.NEARBY, PeopleRecommender.recommend(base.copy(myLocation = null)).single().kind)
    }

    @Test fun insertionPointsAndCounts() {
        assertEquals(emptyList<Int>(), PeopleRecommender.insertionPoints(0))
        assertEquals(listOf(3), PeopleRecommender.insertionPoints(3))
        assertEquals(listOf(4), PeopleRecommender.insertionPoints(12))
        assertEquals(listOf(4, 22, 40), PeopleRecommender.insertionPoints(80))
        assertEquals("1.2K", PeopleRecommender.formatCount(1234))
        assertEquals("12K", PeopleRecommender.formatCount(12_345))
        assertEquals("980", PeopleRecommender.formatCount(980))
    }

    @Test fun emptyDataGivesNoSuggestions() {
        assertTrue(PeopleRecommender.recommend(RecommendationInput("me", emptySet(), emptySet(), profiles = profiles)).isEmpty())
    }
}
