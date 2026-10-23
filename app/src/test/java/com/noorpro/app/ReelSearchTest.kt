package com.noorpro.app

import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.search.ReelSearchKeys
import com.noorpro.app.data.search.ReelSearchRanker
import org.junit.Assert.assertEquals
import org.junit.Test

class ReelSearchTest {
    private fun post(id: String, caption: String, handle: String = "u", views: Long = 0) =
        UmmahPost(id = id, type = "reel", mediaUrl = "https://x/$id.mp4", creatorUid = "c$id", creatorName = "Name $id", creatorHandle = handle, caption = caption, sourceReference = "", thumbnailUrl = "", publishedAt = 1L, viewCount = views)

    @Test fun keysMatchServer() {
        // Same outputs as functions/index.js reelSearchKey (checked with node).
        assertEquals("rehman", ReelSearchKeys.key("Rehmaan"))
        assertEquals("yasin", ReelSearchKeys.key("Yaseen"))
        assertEquals("الرحمن", ReelSearchKeys.key("الرَّحْمَٰن"))
        assertEquals("ابهي", ReelSearchKeys.key("ابھی"))
        assertEquals("cafe", ReelSearchKeys.key("Café"))
        assertEquals("ramadan", ReelSearchKeys.queryKey("#Ramadan"))
    }

    @Test fun hashtagsLowercaseAndUrdu() {
        assertEquals(listOf("ramadan", "سکون"), ReelSearchKeys.hashtags("Beautiful #Ramadan reminder #سکون #ramadan"))
    }

    @Test fun rankingExactPrefixContainsFuzzy() {
        val posts = listOf(post("fz", "rahmn mercy"), post("ct", "the arrahmanic"), post("px", "rahmani nights"), post("ex", "rahman"))
        assertEquals(listOf("ex", "px", "ct", "fz"), ReelSearchRanker.reels("rahman", posts).map { it.id })
    }

    @Test fun hashtagAndUserQueries() {
        val posts = listOf(post("a", "#dua daily", handle = "@noor"), post("b", "dua", handle = "@other"))
        assertEquals(listOf("a"), ReelSearchRanker.reels("#dua", posts).map { it.id })
        assertEquals(listOf("a"), ReelSearchRanker.reels("@noo", posts).map { it.id })
    }
}
