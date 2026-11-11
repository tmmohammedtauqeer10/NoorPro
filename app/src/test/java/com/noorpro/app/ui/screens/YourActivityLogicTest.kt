package com.noorpro.app.ui.screens

import org.junit.Assert.*
import org.junit.Test

class YourActivityLogicTest {
    @Test
    fun chunkIdsRespectsFirestoreWhereInLimitAndDedupe() {
        val ids = (1..65).map { "p$it" } + listOf("p1", "", "  ")
        val chunks = YourActivityLogic.chunkIds(ids, 30)
        assertEquals(3, chunks.size)
        assertEquals(30, chunks[0].size)
        assertEquals(30, chunks[1].size)
        assertEquals(5, chunks[2].size)
        assertEquals(1, chunks.flatten().count { it == "p1" })
        assertTrue(chunks.flatten().none { it.isBlank() })
    }

    @Test
    fun pageIdsLazyLoadsWithoutOverflow() {
        val ids = (1..50).map { "id$it" }
        assertEquals((1..24).map { "id$it" }, YourActivityLogic.pageIds(ids, 0, 24))
        assertEquals((25..48).map { "id$it" }, YourActivityLogic.pageIds(ids, 24, 24))
        assertEquals(listOf("id49", "id50"), YourActivityLogic.pageIds(ids, 48, 24))
        assertTrue(YourActivityLogic.pageIds(ids, 50, 24).isEmpty())
    }

    @Test
    fun markAvailabilityKeepsOrderAndFlagsMissing() {
        val refs = YourActivityLogic.markAvailability(
            listOf("a", "b", "c", "a", ""),
            setOf("a", "c")
        )
        assertEquals(
            listOf(
                ActivityResolveRef("a", true),
                ActivityResolveRef("b", false),
                ActivityResolveRef("c", true)
            ),
            refs
        )
    }

    @Test
    fun orderWatchHistoryNewestFirst() {
        val rows = listOf(
            WatchProgressEntry("old", 1, 10_000, 100),
            WatchProgressEntry("new", 2, 10_000, 300),
            WatchProgressEntry("mid", 3, 10_000, 200)
        )
        assertEquals(listOf("new", "mid", "old"), YourActivityLogic.orderWatchHistory(rows).map { it.postId })
        assertEquals(listOf("old", "mid"), YourActivityLogic.removeWatchEntry(rows, "new").map { it.postId })
    }

    @Test
    fun filterArchivedKeepsOnlyArchivedNewestFirst() {
        val posts = listOf(
            ArchivedPostRef("live", false, 50),
            ArchivedPostRef("arch-new", true, 90),
            ArchivedPostRef("arch-old", true, 10)
        )
        assertEquals(listOf("arch-new", "arch-old"), YourActivityLogic.filterArchived(posts).map { it.id })
        assertEquals(listOf("live"), YourActivityLogic.filterArchived(posts, archivedOnly = false).map { it.id })
    }
}
