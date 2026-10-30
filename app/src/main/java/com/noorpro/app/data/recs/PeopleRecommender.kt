package com.noorpro.app.data.recs

import android.content.Context
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahProfile
import kotlin.math.ln

/**
 * 1.1.4: "Recommended for you" in the Ummah feed. Pure, client-side ranking over data the app
 * can already read: public ummah_profiles (incl. the 1.0.92 people search index), public
 * following lists (ummah_users/{uid}/following, readable by any signed-in member), the feed
 * posts and the member's own likes/saves. No new backend, no new rules.
 */
enum class SuggestionKind { MUTUAL, INTEREST, ACTIVE, NEARBY, POPULAR }

data class PersonSuggestion(
    val uid: String,
    val name: String,
    val handle: String,
    val photoUrl: String,
    val verified: Boolean,
    val followerCount: Int?,
    val reason: String,
    val kind: SuggestionKind,
    val score: Double,
    val mutualNames: List<String> = emptyList(),
)

/** Everything the ranker looks at. All fields optional so missing data simply scores 0. */
data class RecommendationInput(
    val me: String,
    val following: Set<String>,
    val blocked: Set<String>,
    val dismissed: Set<String> = emptySet(),
    val profiles: Map<String, UmmahProfile> = emptyMap(),
    /** followed uid -> the uids they follow (friends of friends). */
    val followingOfFollowed: Map<String, Set<String>> = emptyMap(),
    val posts: List<UmmahPost> = emptyList(),
    val liked: Set<String> = emptySet(),
    val saved: Set<String> = emptySet(),
    /** The member's own place (from their own post locations), if any. */
    val myLocation: String? = null,
    val followerCounts: Map<String, Int> = emptyMap(),
    val verified: Set<String> = emptySet(),
    val now: Long = System.currentTimeMillis(),
)

object PeopleRecommender {
    private const val DAY = 86_400_000L

    /** Topics a member cares about: categories of posts they liked/saved or that people they follow post. */
    fun myTopics(input: RecommendationInput): Map<String, Int> {
        val counts = HashMap<String, Int>()
        input.posts.forEach { p ->
            val w = (if (p.id in input.liked) 2 else 0) + (if (p.id in input.saved) 3 else 0) + (if (p.creatorUid in input.following) 1 else 0)
            if (w > 0) topicOf(p)?.let { counts[it] = (counts[it] ?: 0) + w }
        }
        return counts
    }

    internal fun topicOf(p: UmmahPost): String? {
        val c = p.category.trim()
        return when {
            c.isBlank() -> null
            c.equals("Reflection", true) || c.equals("Reflections", true) -> "Reflections"
            c.equals("Dua", true) || c.equals("Duas", true) -> "Duas"
            c.equals("Reminder", true) || c.equals("Reminders", true) -> "Reminders"
            else -> c.replaceFirstChar { it.uppercase() }
        }
    }

    fun recommend(input: RecommendationInput, limit: Int = 12): List<PersonSuggestion> {
        val exclude = input.following + input.blocked + input.dismissed + input.me + ""
        val byCreator = input.posts.filter { it.creatorUid.isNotBlank() }.groupBy { it.creatorUid }
        val myTopics = myTopics(input)
        val nameOf = { uid: String -> input.profiles[uid]?.name?.ifBlank { null } ?: byCreator[uid]?.firstOrNull()?.creatorName?.ifBlank { null } }

        // Mutual: who among the people I follow also follows X.
        val mutual = HashMap<String, MutableList<String>>()
        input.followingOfFollowed.forEach { (friend, theirs) ->
            if (friend !in input.following) return@forEach
            theirs.forEach { x -> mutual.getOrPut(x) { mutableListOf() } += friend }
        }

        val candidates = (input.profiles.keys + byCreator.keys + mutual.keys).filter { it !in exclude }.toSet()
        val out = candidates.mapNotNull { uid ->
            val profile = input.profiles[uid]
            val posts = byCreator[uid].orEmpty()
            val name = profile?.name?.ifBlank { null } ?: posts.firstOrNull()?.creatorName?.ifBlank { null } ?: return@mapNotNull null
            val handleRaw = profile?.handle?.ifBlank { null } ?: posts.firstOrNull()?.creatorHandle.orEmpty()
            val handle = handleRaw.removePrefix("@").let { if (it.isBlank()) "" else "@$it" }
            val photo = profile?.photoUrl?.ifBlank { null } ?: posts.firstOrNull()?.creatorPhotoUrl.orEmpty()

            val friends = mutual[uid].orEmpty()
            val mutualScore = 30.0 * friends.size

            val theirTopics = posts.mapNotNull { topicOf(it) }.groupingBy { it }.eachCount()
            val shared = theirTopics.keys.filter { it in myTopics }.sortedByDescending { (myTopics[it] ?: 0) * 10 + (theirTopics[it] ?: 0) }
            val interestScore = shared.sumOf { 6.0 * kotlin.math.min(myTopics[it] ?: 0, 5) + 2.0 * kotlin.math.min(theirTopics[it] ?: 0, 5) }

            val recent = posts.count { input.now - it.publishedAt in 0..(14 * DAY) }
            val engagement = posts.sumOf { it.likeCount + 2 * it.commentCount + 3 * it.shareCount }
            val activeScore = 4.0 * kotlin.math.min(recent, 6) + 3.0 * ln(1.0 + engagement)

            val place = input.myLocation?.trim()?.takeIf { it.isNotEmpty() }
            val isNear = place != null && posts.any { it.locationName.trim().equals(place, true) }
            val isNew = posts.isNotEmpty() && posts.minOf { it.publishedAt } > input.now - 30 * DAY
            val nearScore = if (isNear) (if (isNew) 18.0 else 10.0) else 0.0

            val followers = input.followerCounts[uid]
            val popScore = if (followers != null) 2.0 * ln(1.0 + followers) else 0.0
            val verified = uid in input.verified || profile?.verified == true

            val score = mutualScore + interestScore + activeScore + nearScore + popScore + (if (verified) 4.0 else 0.0)
            if (score <= 0.0 && posts.isEmpty() && friends.isEmpty()) return@mapNotNull null

            val names = friends.mapNotNull { nameOf(it)?.substringBefore(' ') }
            val (kind, reason) = when {
                friends.isNotEmpty() -> SuggestionKind.MUTUAL to mutualReason(names, friends.size)
                shared.isNotEmpty() -> SuggestionKind.INTEREST to "Posts about ${shared.first()}"
                isNear -> SuggestionKind.NEARBY to (if (isNew) "New near you · $place" else "Near you · $place")
                recent >= 2 -> SuggestionKind.ACTIVE to "Active creator · $recent posts lately"
                else -> SuggestionKind.POPULAR to "Popular in the Ummah"
            }
            PersonSuggestion(uid, name, handle, photo, verified, followers, reason, kind, score, names)
        }
        return out.sortedWith(compareByDescending<PersonSuggestion> { it.score }.thenBy { it.uid }).take(limit)
    }

    internal fun mutualReason(names: List<String>, total: Int): String {
        val first = names.firstOrNull() ?: return if (total == 1) "Followed by 1 person you follow" else "Followed by $total people you follow"
        return if (total <= 1) "Followed by $first" else "Followed by $first + ${total - 1}"
    }

    /** The member's own place, taken from their most recent post with a location. */
    fun myLocation(me: String, posts: List<UmmahPost>): String? =
        posts.filter { it.creatorUid == me && it.locationName.isNotBlank() }.maxByOrNull { it.publishedAt }?.locationName

    /** Feed indices (0-based post index) BEFORE which a recommendation row is inserted. */
    fun insertionPoints(postCount: Int, first: Int = 4, every: Int = 18): List<Int> {
        if (postCount <= 0) return emptyList()
        if (postCount <= first) return listOf(postCount) // after the last post
        val out = mutableListOf(first)
        var next = first + every
        while (next < postCount - 2 && out.size < 3) { out += next; next += every }
        return out
    }

    fun formatCount(n: Int): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).replace(".0M", "M")
        n >= 10_000 -> "${n / 1000}K"
        n >= 1_000 -> "%.1fK".format(n / 1000.0).replace(".0K", "K")
        else -> n.toString()
    }
}

/** Local memory of dismissed suggestions and the "hide this row" choice. */
class SuggestionPrefs(context: Context) {
    private val p = context.getSharedPreferences("ummah_suggestions", Context.MODE_PRIVATE)
    fun dismissed(): Set<String> = p.getStringSet("dismissed", emptySet()).orEmpty()
    fun dismiss(uid: String) { p.edit().putStringSet("dismissed", dismissed() + uid).apply() }
    fun rowHiddenUntil(): Long = p.getLong("row_hidden_until", 0L)
    fun isRowHidden(now: Long = System.currentTimeMillis()) = rowHiddenUntil() > now
    /** Hides the whole row for [days] (default 30). */
    fun hideRow(now: Long = System.currentTimeMillis(), days: Int = 30) { p.edit().putLong("row_hidden_until", now + days * 86_400_000L).apply() }
    fun reset() { p.edit().clear().apply() }
}
