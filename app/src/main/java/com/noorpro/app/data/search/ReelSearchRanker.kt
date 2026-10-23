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

    /** Trending = tag use in the last 14 days; falls back to all-time tags, then to categories. */
    fun trending(posts: List<UmmahPost>, now: Long = System.currentTimeMillis(), limit: Int = 12): List<Tag> {
        val recent = now - 14L * 86_400_000L
        val w = HashMap<String, Int>()
        posts.forEach { p -> postHashtags(p).forEach { w[it] = (w[it] ?: 0) + if (p.publishedAt >= recent) 3 else 1 } }
        if (w.isEmpty()) {
            // No hashtags at all: surface categories / frequent caption words so Explore never looks empty.
            val words = posts.flatMap { p -> listOf(p.category) + ReelSearchKeys.tokens(p.caption).filter { it.length >= 4 } }
                .filter { it.isNotBlank() && it.lowercase() !in STOP }.groupingBy { it.lowercase() }.eachCount()
            return words.entries.sortedByDescending { it.value }.take(limit).map { Tag(it.key, it.value) }
        }
        val counts = tagCounts(posts).associate { it.tag to it.count }
        return w.entries.sortedByDescending { it.value }.take(limit).map { Tag(it.key, counts[it.key] ?: 0) }
    }

    private val STOP = setOf("this", "that", "with", "from", "have", "your", "what", "when", "they", "will", "about", "general", "reel", "video")

    /**
     * Related reels for a set of matches: same hashtags (weighted by overlap) or same creator,
     * excluding the matches themselves.
     */
    fun related(matches: List<UmmahPost>, pool: List<UmmahPost>, limit: Int = 30): List<UmmahPost> {
        if (matches.isEmpty()) return emptyList()
        val ids = matches.map { it.id }.toSet()
        val tags = matches.take(10).flatMap { postHashtags(it) }.groupingBy { it }.eachCount()
        val creators = matches.take(10).map { it.creatorUid }.filter { it.isNotBlank() }.toSet()
        return pool.asSequence().filter { it.id !in ids }.map { p ->
            val overlap = postHashtags(p).sumOf { tags[it] ?: 0 }
            p to (overlap * 2 + if (p.creatorUid in creators) 3 else 0)
        }.filter { it.second > 0 }.sortedWith(compareByDescending<Pair<UmmahPost, Int>> { it.second }.thenByDescending { pop(it.first) })
            .take(limit).map { it.first }.toList()
    }

    /** "Related searches" chips: co-occurring hashtags + matched creators' handles. */
    fun relatedSearches(query: String, matches: List<UmmahPost>, limit: Int = 8): List<String> {
        val q = query.trim().trimStart('#', '@').lowercase()
        val tags = matches.take(20).flatMap { postHashtags(it) }.filter { it != q }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { "#" + it.key }
        val handles = matches.take(10).map { it.creatorHandle.removePrefix("@") }.filter { it.isNotBlank() && it.lowercase() != q }.distinct().map { "@$it" }
        return (tags.take(limit - 2) + handles.take(2) + tags.drop(limit - 2)).distinct().take(limit)
    }

    /** Explore order: popularity with a recency boost. */
    fun explore(posts: List<UmmahPost>, now: Long = System.currentTimeMillis()): List<UmmahPost> =
        posts.sortedByDescending { p -> pop(p) + 3.0 * kotlin.math.exp(-((now - p.publishedAt).coerceAtLeast(0) / 86_400_000.0) / 7.0) }
}
