package com.noorpro.app.ui.screens

/**
 * Pure helpers for Instagram-style Your Activity (1.1.9).
 * No Android / Firebase — unit tested in YourActivityLogicTest.
 */
object YourActivityLogic {
    const val ID_CHUNK = 30
    const val PAGE_SIZE = 24

    /** Split ids into Firestore whereIn-safe chunks (max 30). Blanks dropped, order kept, de-duped. */
    fun chunkIds(ids: Collection<String>, size: Int = ID_CHUNK): List<List<String>> {
        val clean = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (clean.isEmpty() || size < 1) return emptyList()
        return clean.chunked(size)
    }

    /** Next page of ids for lazy loading. */
    fun pageIds(ids: List<String>, offset: Int, pageSize: Int = PAGE_SIZE): List<String> {
        if (ids.isEmpty() || pageSize < 1 || offset >= ids.size) return emptyList()
        val start = offset.coerceAtLeast(0)
        return ids.subList(start, minOf(start + pageSize, ids.size))
    }

    /**
     * Build an ordered resolve list: keep [requested] order; mark missing ids unavailable.
     * [foundIds] = document ids that came back from ummah_posts.
     */
    fun markAvailability(requested: List<String>, foundIds: Set<String>): List<ActivityResolveRef> {
        val seen = HashSet<String>()
        return requested.mapNotNull { raw ->
            val id = raw.trim()
            if (id.isEmpty() || !seen.add(id)) null
            else ActivityResolveRef(id = id, available = id in foundIds)
        }
    }

    /** Newest watch progress first (by updatedAt). */
    fun orderWatchHistory(progress: List<WatchProgressEntry>): List<WatchProgressEntry> =
        progress.sortedWith(compareByDescending<WatchProgressEntry> { it.updatedAt }.thenBy { it.postId })

    fun removeWatchEntry(progress: List<WatchProgressEntry>, postId: String): List<WatchProgressEntry> =
        progress.filterNot { it.postId == postId }

    /** Own posts that are archived (or not), newest publishedAt first when timestamps provided. */
    fun filterArchived(
        posts: List<ArchivedPostRef>,
        archivedOnly: Boolean = true
    ): List<ArchivedPostRef> =
        posts.filter { it.archived == archivedOnly }
            .sortedWith(compareByDescending<ArchivedPostRef> { it.publishedAt }.thenBy { it.id })
}

data class ActivityResolveRef(val id: String, val available: Boolean)

/** Lightweight watch row for tests (mirrors WatchProgress fields used by history). */
data class WatchProgressEntry(
    val postId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

data class ArchivedPostRef(
    val id: String,
    val archived: Boolean,
    val publishedAt: Long = 0L
)
