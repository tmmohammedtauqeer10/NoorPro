package com.noorpro.app.ui.screens

import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahProfile

/*
 * 1.1.5 Profile v2: pure logic (unit tested in NoorProfile2LogicTest). No Android / Firebase here.
 */

// ---------------------------------------------------------------- username availability

enum class UsernameStatus(val message: String, val ok: Boolean) {
    UNCHANGED("This is your username", true),
    AVAILABLE("Available", true),
    TOO_SHORT("At least 3 characters", false),
    TOO_LONG("At most 24 characters", false),
    INVALID("Use letters, numbers, . and _ only", false),
    RESERVED("This name is reserved", false),
    TAKEN("Already taken", false),
}

private val reservedHandles = setOf("admin", "noorpro", "noor", "support", "moderator", "official", "help", "settings", "halalguard")

fun normalizeUsername(input: String): String = input.trim().removePrefix("@").lowercase()

/**
 * Checks a username against the profiles we already know about (live ummah_profiles snapshot +
 * searchProfiles results). Server-side uniqueness still wins; this is the instant hint.
 */
fun usernameStatus(input: String, myUid: String, myCurrent: String, known: Collection<UmmahProfile>): UsernameStatus {
    val h = normalizeUsername(input)
    if (h == normalizeUsername(myCurrent) && h.isNotBlank()) return UsernameStatus.UNCHANGED
    if (h.length < 3) return UsernameStatus.TOO_SHORT
    if (h.length > 24) return UsernameStatus.TOO_LONG
    if (!Regex("^[a-z0-9._]+$").matches(h) || h.startsWith(".") || h.endsWith(".") || h.contains("..")) return UsernameStatus.INVALID
    if (h in reservedHandles) return UsernameStatus.RESERVED
    if (known.any { it.uid != myUid && normalizeUsername(it.handle) == h }) return UsernameStatus.TAKEN
    return UsernameStatus.AVAILABLE
}

// ---------------------------------------------------------------- tabs / visibility

enum class Profile2Tab(val label: String) { POSTS("Posts"), REELS("Reels"), SERIES("Series"), SAVED("Saved"), TAGGED("Tagged") }

/** Saved is only on your own profile; Series only when the series flag is on. */
fun profile2Tabs(own: Boolean, seriesEnabled: Boolean): List<Profile2Tab> = Profile2Tab.entries.filter {
    when (it) {
        Profile2Tab.SAVED -> own
        Profile2Tab.SERIES -> seriesEnabled
        else -> true
    }
}

/** Private accounts show their grid only to the owner and to followers. */
fun canSeeProfileContent(profile: UmmahProfile?, own: Boolean, iFollow: Boolean): Boolean =
    own || profile == null || !profile.isPrivate || iFollow

fun profilePosts(all: List<UmmahPost>, uid: String, viewerIsOwner: Boolean): List<UmmahPost> =
    all.filter { it.creatorUid == uid && it.type != "reel" && (viewerIsOwner || !it.archived) }
        .sortedWith(compareByDescending<UmmahPost> { it.pinnedToProfile }.thenByDescending { it.publishedAt })

fun profileReels(all: List<UmmahPost>, uid: String, viewerIsOwner: Boolean): List<UmmahPost> =
    all.filter { it.creatorUid == uid && it.type == "reel" && (viewerIsOwner || !it.archived) }
        .sortedWith(compareByDescending<UmmahPost> { it.pinnedToProfile }.thenByDescending { it.publishedAt })

/** Posts by others that mention @handle in the caption. */
fun taggedPosts(all: List<UmmahPost>, uid: String, handle: String): List<UmmahPost> {
    val h = normalizeUsername(handle)
    if (h.isBlank()) return emptyList()
    val rx = Regex("(^|[^a-z0-9._])@" + Regex.escape(h) + "($|[^a-z0-9_])")
    return all.filter { it.creatorUid != uid && rx.containsMatchIn(it.caption.lowercase()) }.sortedByDescending { it.publishedAt }
}

/** Series grouped from captions ("… Part 2/5"): title → parts in order. */
fun profileSeries(all: List<UmmahPost>, uid: String): List<Pair<String, List<UmmahPost>>> =
    all.filter { it.creatorUid == uid }
        .mapNotNull { p -> parseReelSeries(p.caption)?.let { it to p } }
        .groupBy { it.first.title.lowercase() }
        .values.map { parts -> parts.first().first.title to parts.sortedBy { it.first.part }.map { it.second } }
        .sortedByDescending { (_, ps) -> ps.maxOf { it.publishedAt } }

// ---------------------------------------------------------------- stats

data class Profile2Stat(val key: String, val label: String, val value: String, val hidden: Boolean)

fun profile2Stats(posts: Int, followers: Int, following: Int, streakDays: Int?, own: Boolean, hideCounts: Boolean): List<Profile2Stat> {
    val hide = hideCounts && !own
    return buildList {
        add(Profile2Stat("posts", "Posts", reelCount(posts.toLong()), false))
        add(Profile2Stat("followers", "Followers", if (hide) "—" else reelCount(followers.toLong()), hide))
        add(Profile2Stat("following", "Following", if (hide) "—" else reelCount(following.toLong()), hide))
        if (streakDays != null) add(Profile2Stat("streak", "Quran streak", "${streakDays}d", false))
    }
}

// ---------------------------------------------------------------- bio / sharing

data class BioParts(val text: String, val link: String?)

/** Pull the first link out of the bio so it can be shown as a tappable chip. */
fun splitBioLink(bio: String): BioParts {
    val m = Regex("""(https?://\S+|(?<![@\w])(?:www\.)?[a-z0-9-]+\.(?:com|org|net|app|io|pk|in|me|co)(?:/\S*)?)""", RegexOption.IGNORE_CASE).find(bio)
        ?: return BioParts(bio.trim(), null)
    val link = m.value.trimEnd('.', ',', ')')
    val text = (bio.removeRange(m.range)).replace(Regex("[ \t]+"), " ").trim()
    return BioParts(text, link)
}

fun linkHref(link: String): String = if (link.startsWith("http", true)) link else "https://$link"

fun profileShareUrl(handle: String): String = "https://noorpro.app/u/${normalizeUsername(handle)}"

// ---------------------------------------------------------------- AI bio helper (askNoorAi, AskNoor mode)

object Profile2AiPrompts {
    fun bio(name: String, current: String, interests: String): String =
        "Write 3 short alternative Instagram-style bios (max 120 characters each) for a Muslim member of the NoorPro app. " +
            "Name: ${name.take(40)}. Current bio: \"${current.take(160)}\". Interests: ${interests.take(120).ifBlank { "Quran, learning" }}. " +
            "Humble, sincere, no emoji, no exaggerated religious titles, no hashtags. Reply as a numbered list 1. 2. 3. only."

    /** Parse "1. … 2. … 3. …" (or bullet lines) into at most 3 bios of ≤160 chars. */
    fun parseBios(answer: String): List<String> {
        val body = answer.substringBefore("FOLLOWUPS:")
        val lines = body.lines().map { it.trim() }.filter { it.isNotBlank() }
        val numbered = lines.mapNotNull { Regex("""^(?:\d+[.)]|[-•*])\s*(.+)$""").find(it)?.groupValues?.get(1) }
        return (numbered.ifEmpty { lines }).map { it.trim().trim('"', '“', '”').take(160) }
            .filter { it.length >= 4 }.distinct().take(3)
    }
}

// ---------------------------------------------------------------- weekly deen recap (flagged)

data class DeenRecap(val streakDays: Int, val postsThisWeek: Int, val reelsWatched: Int, val headline: String)

fun weeklyDeenRecap(streakDays: Int, myPosts: List<UmmahPost>, reelsWatched: Int, now: Long): DeenRecap {
    val week = 7L * 24 * 3600 * 1000
    val posts = myPosts.count { now - it.publishedAt in 0..week }
    val headline = when {
        streakDays >= 7 -> "A full week with the Quran — MashaAllah."
        streakDays >= 3 -> "$streakDays days of Quran this week. Keep the habit going."
        posts > 0 -> "You shared $posts reminder${if (posts == 1) "" else "s"} with the Ummah."
        else -> "A new week — start with one page today."
    }
    return DeenRecap(streakDays, posts, reelsWatched, headline)
}
