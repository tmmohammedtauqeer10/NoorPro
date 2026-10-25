package com.noorpro.app.data.creator

import android.content.Context
import com.noorpro.app.data.ComposerDraft
import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahStory
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * 1.1.4 Creator Studio — pure logic (insights, content filters, comment inbox). Everything here works
 * from data the app already reads: lifetime counters on the creator's own posts, the public
 * followers subcollection (createdAt) and comments on their posts. No new backend needed.
 * Optional per-day history ([dailyDeltas]) needs the flagged `creatorDailySnapshot` function.
 */
object CreatorFlags {
    /** Caption edit / pin to profile / archive / hide or delete others' comments on your posts / pin comment. Needs the additive 1.1.4 rules. */
    const val KEY_EDIT = "creator_studio_edit_enabled"
    /** Per-day views/likes history from the scheduled `creatorDailySnapshot` function. */
    const val KEY_DAILY = "creator_daily_stats_enabled"
    @Volatile var override: Map<String, Boolean>? = null
    @Volatile private var loaded: Map<String, Boolean> = emptyMap()
    @Volatile private var started = false
    fun isOn(key: String) = override?.get(key) ?: loaded[key] ?: false
    val edit get() = isOn(KEY_EDIT)
    val daily get() = isOn(KEY_DAILY)
    fun load() {
        if (started) return
        started = true
        runCatching {
            val rc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val keys = listOf(KEY_EDIT, KEY_DAILY)
            fun apply() { loaded = keys.associateWith { rc.getBoolean(it) } }
            apply()
            rc.fetchAndActivate().addOnCompleteListener { runCatching { apply() } }
        }
    }
}

enum class InsightRange(val days: Int, val label: String) { D7(7, "7 days"), D28(28, "28 days"), D90(90, "90 days") }

data class ChartPoint(val label: String, val value: Long)
data class BestSlot(val hour: Int, val score: Double, val posts: Int, val prayerNote: String)

data class CreatorInsights(
    val range: InsightRange,
    val posts: Int,
    val reels: Int,
    val views: Long,
    val uniqueReelViewers: Long,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val saves: Long,
    /** (likes+comments+shares+saves) / views, 0..1. */
    val engagementRate: Double,
    /** Same window just before this one (content published then), for the ▲/▼ change. */
    val previousViews: Long,
    val followers: Int,
    val followerGain: Int,
    val followerSeries: List<ChartPoint>,
    val viewsSeries: List<ChartPoint>,
    val topPosts: List<UmmahPost>,
    val topReels: List<UmmahPost>,
    val bestSlots: List<BestSlot>,
    /** True when bestSlots came from the creator's own history (else general guidance). */
    val bestFromHistory: Boolean,
) {
    val interactions get() = likes + comments + shares + saves
    val viewsChangePct: Int? get() = if (previousViews <= 0) null else (((views - previousViews) * 100.0) / previousViews).roundToInt()
}

/** Prayer times for "best time to post" labels: name -> "HH:mm" (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha). */
typealias PrayerClock = Map<String, String>

object CreatorInsightsCalc {
    private const val DAY = 86_400_000L
    fun isReel(p: UmmahPost) = p.type == "reel" || p.type == "video"
    fun engagement(p: UmmahPost) = p.likeCount + p.commentCount + p.shareCount + p.saveCount

    fun compute(
        posts: List<UmmahPost>,
        followerTimes: List<Long>,
        range: InsightRange,
        now: Long = System.currentTimeMillis(),
        prayer: PrayerClock = emptyMap(),
        zone: TimeZone = TimeZone.getDefault(),
    ): CreatorInsights {
        val start = now - range.days * DAY
        val inRange = posts.filter { it.publishedAt in start..now }
        val prev = posts.filter { it.publishedAt in (start - range.days * DAY) until start }
        val views = inRange.sumOf { it.viewCount }
        val inter = inRange.sumOf { engagement(it) }
        val (slots, fromHistory) = bestTimes(posts.filter { it.publishedAt >= now - 90 * DAY }, prayer, zone)
        return CreatorInsights(
            range = range,
            posts = inRange.count { !isReel(it) },
            reels = inRange.count { isReel(it) },
            views = views,
            uniqueReelViewers = inRange.filter { it.type == "reel" }.sumOf { it.viewCount },
            likes = inRange.sumOf { it.likeCount },
            comments = inRange.sumOf { it.commentCount },
            shares = inRange.sumOf { it.shareCount },
            saves = inRange.sumOf { it.saveCount },
            engagementRate = if (views > 0) (inter.toDouble() / views).coerceAtMost(1.0) else 0.0,
            previousViews = prev.sumOf { it.viewCount },
            followers = followerTimes.size,
            followerGain = followerTimes.count { it in start..now },
            followerSeries = followerSeries(followerTimes, range, now, zone),
            viewsSeries = bucket(range, now, zone) { s, e -> inRange.filter { it.publishedAt in s until e }.sumOf { it.viewCount } },
            topPosts = inRange.filter { !isReel(it) }.sortedWith(compareByDescending<UmmahPost> { it.viewCount + 3 * engagement(it) }.thenByDescending { it.publishedAt }).take(3),
            topReels = inRange.filter { isReel(it) }.sortedWith(compareByDescending<UmmahPost> { it.viewCount + 3 * engagement(it) }.thenByDescending { it.publishedAt }).take(3),
            bestSlots = slots,
            bestFromHistory = fromHistory,
        )
    }

    /** 7 → 7 daily bars, 28 → 4 weekly bars, 90 → 13 weekly bars (oldest first). */
    internal fun bucket(range: InsightRange, now: Long, zone: TimeZone, value: (Long, Long) -> Long): List<ChartPoint> {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val endOfToday = cal.timeInMillis + DAY
        val size = if (range == InsightRange.D7) DAY else 7 * DAY
        val n = when (range) { InsightRange.D7 -> 7; InsightRange.D28 -> 4; InsightRange.D90 -> 13 }
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return (n - 1 downTo 0).map { i ->
            val e = endOfToday - i * size; val s = e - size
            val label = if (range == InsightRange.D7) days[Calendar.getInstance(zone).apply { timeInMillis = s }.get(Calendar.DAY_OF_WEEK) - 1]
            else "W${n - i}"
            ChartPoint(label, value(s, e))
        }
    }

    /** New followers per bucket. */
    fun followerSeries(times: List<Long>, range: InsightRange, now: Long, zone: TimeZone = TimeZone.getDefault()) =
        bucket(range, now, zone) { s, e -> times.count { it in s until e }.toLong() }

    /**
     * Best hours to post: average (views + 3·interactions) of the creator's posts by local hour,
     * needing ≥3 posts in an hour band (2-hour bands). With too little history we return general
     * guidance anchored on prayer times (after Fajr, after Maghrib/Isha — when the Ummah is on phones).
     */
    fun bestTimes(posts: List<UmmahPost>, prayer: PrayerClock = emptyMap(), zone: TimeZone = TimeZone.getDefault()): Pair<List<BestSlot>, Boolean> {
        val byBand = posts.groupBy { (Calendar.getInstance(zone).apply { timeInMillis = it.publishedAt }.get(Calendar.HOUR_OF_DAY) / 2) * 2 }
        val scored = byBand.filter { it.value.size >= 3 }
            .map { (h, ps) -> BestSlot(h, ps.map { it.viewCount + 3.0 * engagement(it) }.average(), ps.size, prayerNote(h, prayer)) }
            .sortedByDescending { it.score }.take(3)
        if (scored.isNotEmpty()) return scored to true
        val fajr = hourOf(prayer["Fajr"]) ?: 5
        val maghrib = hourOf(prayer["Maghrib"]) ?: 18
        val isha = hourOf(prayer["Isha"]) ?: 20
        val general = listOf(maghrib, isha + 1, fajr + 1).distinct().map { h -> BestSlot(h % 24, 0.0, 0, prayerNote(h % 24, prayer)) }
        return general to false
    }

    private fun hourOf(hhmm: String?): Int? = hhmm?.substringBefore(':')?.toIntOrNull()?.takeIf { it in 0..23 }
    private fun minutesOf(hhmm: String?): Int? = hhmm?.split(':')?.takeIf { it.size == 2 }?.let { (h, m) -> (h.toIntOrNull() ?: return null) * 60 + (m.toIntOrNull() ?: return null) }

    /** "After Maghrib" style label for an hour (uses the prayer that most recently started). */
    fun prayerNote(hour: Int, prayer: PrayerClock): String {
        val order = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        val mins = hour * 60 + 30
        val known = order.mapNotNull { n -> minutesOf(prayer[n])?.let { n to it } }
        if (known.isEmpty()) return when (hour) {
            in 4..6 -> "around Fajr"; in 12..14 -> "around Dhuhr"; in 15..17 -> "around Asr"; in 18..19 -> "after Maghrib"; in 20..23 -> "after Isha"; else -> "late night"
        }
        val before = known.lastOrNull { it.second <= mins } ?: known.last()
        return if (before.first == "Sunrise") "morning, after Fajr" else "after ${before.first}"
    }

    fun hourLabel(h: Int): String {
        val hh = if (h % 12 == 0) 12 else h % 12
        val end = (h + 2) % 24; val ee = if (end % 12 == 0) 12 else end % 12
        return "$hh–$ee ${if (end < 12 || end == 24) "AM" else "PM"}".let { if (h < 12 && end >= 12) "$hh AM–$ee PM" else it }
    }

    /** Daily snapshot totals (oldest first) → per-day change. Used only when [CreatorFlags.daily] is on. */
    fun dailyDeltas(totals: List<Pair<String, Long>>): List<ChartPoint> =
        totals.zipWithNext { a, b -> ChartPoint(b.first, (b.second - a.second).coerceAtLeast(0)) }
}

// ------------------------------------------------------------------ content manager

enum class StudioKind(val label: String) { POST("Posts"), REEL("Reels"), STORY("Stories"), DRAFT("Drafts") }
enum class StudioSort(val label: String) { NEWEST("Newest"), MOST_VIEWED("Most viewed"), MOST_ENGAGED("Most engaged") }

data class StudioItem(
    val id: String,
    val kind: StudioKind,
    val title: String,
    val thumbUrl: String,
    val time: Long,
    val views: Long = 0,
    val likes: Long = 0,
    val comments: Long = 0,
    val engagement: Long = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val post: UmmahPost? = null,
    val story: UmmahStory? = null,
    val draft: ComposerDraft? = null,
)

object StudioContent {
    fun of(p: UmmahPost) = StudioItem(
        p.id, if (CreatorInsightsCalc.isReel(p)) StudioKind.REEL else StudioKind.POST,
        p.caption.ifBlank { p.arabicText }.ifBlank { if (CreatorInsightsCalc.isReel(p)) "Reel" else "Post" },
        p.thumbnailUrl.ifBlank { p.mediaUrl.takeIf { p.type == "image" }.orEmpty() }, p.publishedAt,
        p.viewCount, p.likeCount, p.commentCount, CreatorInsightsCalc.engagement(p), p.pinnedToProfile, p.archived, post = p,
    )
    fun of(s: UmmahStory) = StudioItem(s.id, StudioKind.STORY, s.caption.ifBlank { if (s.isVideo) "Video story" else "Photo story" }, s.coverUrl, s.createdAt, story = s)
    fun of(d: ComposerDraft) = StudioItem(d.id, StudioKind.DRAFT, d.preview, d.media.firstOrNull().orEmpty().takeIf { !d.isVideo }.orEmpty(), d.updatedAt, draft = d)

    fun filter(items: List<StudioItem>, kind: StudioKind, sort: StudioSort, query: String, showArchived: Boolean): List<StudioItem> {
        val q = query.trim().lowercase()
        return items.filter { it.kind == kind && (showArchived == it.archived || kind == StudioKind.DRAFT || kind == StudioKind.STORY) && (q.isEmpty() || it.title.lowercase().contains(q)) }
            .sortedWith(
                compareByDescending<StudioItem> { it.pinned && sort == StudioSort.NEWEST }
                    .then(when (sort) {
                        StudioSort.NEWEST -> compareByDescending { it.time }
                        StudioSort.MOST_VIEWED -> compareByDescending<StudioItem> { it.views }.thenByDescending { it.time }
                        StudioSort.MOST_ENGAGED -> compareByDescending<StudioItem> { it.engagement }.thenByDescending { it.time }
                    })
            )
    }
}

// ------------------------------------------------------------------ comments manager

enum class InboxFilter(val label: String) { ALL("All"), UNREPLIED("Needs reply"), FILTERED("Filtered"), HIDDEN("Hidden") }

data class InboxComment(
    val comment: UmmahComment,
    val postTitle: String,
    val replied: Boolean,
    val keywordHit: String?,
    val hidden: Boolean,
    val restricted: Boolean,
    val pinned: Boolean,
)

object CommentInbox {
    /** Case/diacritic-light keyword match on word boundaries (also matches simple obfuscation like "s p a m"). */
    fun keywordHit(text: String, keywords: Collection<String>): String? {
        val norm = text.lowercase()
        val squashed = norm.replace(Regex("[^\\p{L}\\p{N}]"), "")
        return keywords.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.firstOrNull { k ->
            Regex("(^|[^\\p{L}\\p{N}])${Regex.escape(k)}($|[^\\p{L}\\p{N}])").containsMatchIn(norm) ||
                (k.length >= 4 && squashed.contains(k.replace(Regex("[^\\p{L}\\p{N}]"), "")))
        }
    }

    fun build(
        comments: List<UmmahComment>,
        myUid: String,
        postTitles: Map<String, String>,
        hiddenIds: Set<String>,
        keywords: Set<String>,
        restricted: Set<String>,
        blocked: Set<String>,
        pinned: Map<String, String> = emptyMap(),
    ): List<InboxComment> {
        val repliedTo = comments.filter { it.creatorUid == myUid && it.parentCommentId.isNotBlank() }.map { it.parentCommentId }.toSet()
        return comments.filter { it.creatorUid != myUid && it.creatorUid !in blocked }
            .map { c ->
                InboxComment(
                    c, postTitles[c.postId].orEmpty(), c.id in repliedTo, keywordHit(c.text, keywords),
                    hidden = c.id in hiddenIds || c.status == "hidden", restricted = c.creatorUid in restricted, pinned = pinned[c.postId] == c.id,
                )
            }.sortedByDescending { it.comment.createdAt }
    }

    fun filter(items: List<InboxComment>, f: InboxFilter, query: String = ""): List<InboxComment> {
        val q = query.trim().lowercase()
        val clean = { i: InboxComment -> !i.hidden && i.keywordHit == null && !i.restricted }
        return items.filter { i ->
            when (f) {
                InboxFilter.ALL -> clean(i)
                InboxFilter.UNREPLIED -> clean(i) && !i.replied
                InboxFilter.FILTERED -> !i.hidden && (i.keywordHit != null || i.restricted)
                InboxFilter.HIDDEN -> i.hidden
            } && (q.isEmpty() || i.comment.text.lowercase().contains(q) || i.comment.creatorName.lowercase().contains(q))
        }
    }
}

/** Local creator moderation prefs (hidden comments, keyword filter, restricted accounts, local pins). */
class CreatorModerationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("creator_studio", Context.MODE_PRIVATE)
    private fun set(key: String) = prefs.getStringSet(key, emptySet()).orEmpty().toSet()
    private fun put(key: String, v: Set<String>) = prefs.edit().putStringSet(key, v).apply()
    fun hidden() = set("hidden")
    fun setHidden(id: String, on: Boolean) = put("hidden", if (on) hidden() + id else hidden() - id)
    fun keywords() = set("keywords")
    fun addKeyword(k: String) { val t = k.trim().lowercase().take(40); if (t.isNotEmpty()) put("keywords", keywords() + t) }
    fun removeKeyword(k: String) = put("keywords", keywords() - k)
    fun restricted() = set("restricted")
    fun setRestricted(uid: String, on: Boolean) = put("restricted", if (on) restricted() + uid else restricted() - uid)
    /** postId -> commentId (local pin; mirrored to the post when the edit flag is on). */
    fun pins(): Map<String, String> = set("pins").mapNotNull { it.split('|').takeIf { p -> p.size == 2 }?.let { (a, b) -> a to b } }.toMap()
    fun setPin(postId: String, commentId: String?) = put("pins", (pins() - postId).let { if (commentId != null) it + (postId to commentId) else it }.map { "${it.key}|${it.value}" }.toSet())
}
