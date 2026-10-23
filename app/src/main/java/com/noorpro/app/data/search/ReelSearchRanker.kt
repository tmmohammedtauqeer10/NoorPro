package com.noorpro.app.data.search

import com.noorpro.app.audio.search.AudioSearchEngine
import com.noorpro.app.data.UmmahPost
import kotlin.math.ln

/** 1.0.91: pure ranking for Reels search — exact > prefix > contains > fuzzy, then popularity. */
object ReelSearchRanker {
    data class Account(val uid: String, val name: String, val handle: String, val photoUrl: String, val reels: Int)
    data class Tag(val tag: String, val count: Int)

    fun postHashtags(p: UmmahPost): List<String> = ReelSearchKeys.hashtags(p.caption + " " + p.arabicText)

    private fun pop(p: UmmahPost): Double = ln(1.0 + p.viewCount) + 2 * ln(1.0 + p.likeCount) + ln(1.0 + p.shareCount + p.commentCount)

    fun reels(query: String, posts: List<UmmahPost>): List<UmmahPost> {
        val q = query.trim(); if (q.isEmpty()) return emptyList()
        val tagQuery = q.startsWith("#"); val userQuery = q.startsWith("@")
        val bare = q.trimStart('#', '@')
        return posts.mapNotNull { p ->
            val tags = postHashtags(p)
            val t = when {
                tagQuery -> AudioSearchEngine.tier(bare, tags)
                userQuery -> AudioSearchEngine.tier(bare, listOf(p.creatorHandle.removePrefix("@"), p.creatorName))
                else -> maxOf(
                    AudioSearchEngine.tier(bare, tags + listOf(p.creatorHandle.removePrefix("@"), p.creatorName, p.category)),
                    AudioSearchEngine.tier(bare, listOf(p.caption, p.arabicText))
                )
            }
            if (t == AudioSearchEngine.NONE) null else Triple(p, t, pop(p))
        }.sortedWith(compareByDescending<Triple<UmmahPost, Int, Double>> { it.second }.thenByDescending { it.third }.thenByDescending { it.first.publishedAt })
            .map { it.first }
    }

    fun accounts(query: String, accounts: List<Account>): List<Account> {
        val bare = query.trim().trimStart('@', '#'); if (bare.isEmpty()) return emptyList()
        return accounts.map { it to AudioSearchEngine.tier(bare, listOf(it.handle.removePrefix("@"), it.name)) }
            .filter { it.second > AudioSearchEngine.NONE }
            .sortedWith(compareByDescending<Pair<Account, Int>> { it.second }.thenByDescending { it.first.reels })
            .map { it.first }
    }

    fun tagCounts(posts: List<UmmahPost>): List<Tag> = posts.flatMap { postHashtags(it) }.groupingBy { it }.eachCount()
        .map { Tag(it.key, it.value) }.sortedByDescending { it.count }

    fun hashtags(query: String, posts: List<UmmahPost>): List<Tag> {
        val bare = query.trim().trimStart('#', '@'); if (bare.isEmpty()) return emptyList()
        return tagCounts(posts).map { it to AudioSearchEngine.tier(bare, listOf(it.tag)) }.filter { it.second > AudioSearchEngine.NONE }
            .sortedWith(compareByDescending<Pair<Tag, Int>> { it.second }.thenByDescending { it.first.count }).map { it.first }
    }

    /** Trending = tag use in the last 14 days counts triple. */
    fun trending(posts: List<UmmahPost>, now: Long = System.currentTimeMillis(), limit: Int = 12): List<Tag> {
        val recent = now - 14L * 86_400_000L
        val w = HashMap<String, Int>()
        posts.forEach { p -> postHashtags(p).forEach { w[it] = (w[it] ?: 0) + if (p.publishedAt >= recent) 3 else 1 } }
        val counts = tagCounts(posts).associate { it.tag to it.count }
        return w.entries.sortedByDescending { it.value }.take(limit).map { Tag(it.key, counts[it.key] ?: 0) }
    }

    /** Explore order: popularity with a recency boost. */
    fun explore(posts: List<UmmahPost>, now: Long = System.currentTimeMillis()): List<UmmahPost> =
        posts.sortedByDescending { p -> pop(p) + 3.0 * kotlin.math.exp(-((now - p.publishedAt).coerceAtLeast(0) / 86_400_000.0) / 7.0) }
}
