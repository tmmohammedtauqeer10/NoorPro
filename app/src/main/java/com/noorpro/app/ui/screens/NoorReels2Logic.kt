package com.noorpro.app.ui.screens

import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahPost

/*
 * 1.1.5 Reels v2: pure logic (unit tested in NoorReels2LogicTest). No Android / Firebase here.
 */

// ---------------------------------------------------------------- flags

/**
 * Reels v2 / Profile v2 Remote Config flags. The two screen switches default ON (kill-switch back
 * to the old screens). Anything that needs new backend data, rules or AI modes defaults OFF and
 * the UI falls back cleanly when it is off.
 */
object Reels2Flags {
    const val REELS_V2 = "reels_v2_enabled"
    const val PROFILE_V2 = "profile_v2_enabled"
    const val SERIES = "reels_series_enabled"              // needs series/part fields on posts
    const val SOUND_PAGES = "reels_sound_pages_enabled"    // needs soundId / sounds collection
    const val VOICE_COMMENTS = "reels_voice_comments_enabled" // needs audio upload + rules
    const val REACTIONS = "reels_reactions_enabled"        // needs reactions storage (likes keep working)
    const val CREATOR_LIKED = "reels_creator_liked_enabled" // needs creatorLiked flag on comments
    const val LIVE = "reels_live_enabled"                  // live bayans (placeholder only)
    const val MINI_PLAYER = "reels_mini_player_enabled"    // floating player across screens
    const val AI_TRANSCRIPTS = "reels_ai_transcripts_enabled" // AI captions/translation + key points (needs transcripts)
    const val AI_ASK = "reels_ai_ask_enabled"              // Ask Noor AI (caption-framed askNoorAi; existing backend)
    const val DEEN_CARD = "profile_deen_card_enabled"      // public Deen card fields on ummah_profiles
    const val PROFILE_FIELDS = "profile_extended_fields_enabled" // city, learning line, cover, accent, creator type
    const val HIGHLIGHTS = "profile_highlights_enabled"    // story highlights archive
    const val CLOSE_FRIENDS = "profile_close_friends_enabled"
    const val AI_RECAP = "profile_ai_recap_enabled"        // weekly deen recap (needs activity summary mode)

    private val DEFAULTS = mapOf(
        REELS_V2 to true, PROFILE_V2 to true, AI_ASK to true,
        SERIES to false, SOUND_PAGES to false, VOICE_COMMENTS to false, REACTIONS to false, CREATOR_LIKED to false,
        LIVE to false, MINI_PLAYER to false, AI_TRANSCRIPTS to false, DEEN_CARD to false, PROFILE_FIELDS to false,
        HIGHLIGHTS to false, CLOSE_FRIENDS to false, AI_RECAP to false,
        // 1.1.6 Islamic features (see com.noorpro.app.islamic.IslamicFlags), all default OFF.
        "islamic_calendar_enabled" to false, "group_khatm_enabled" to false, "hifz_coach_enabled" to false,
        "masjid_pages_enabled" to false, "family_kids_enabled" to false, "daily_deen_goals_enabled" to false,
        // 1.1.6 social & audio features, all default OFF.
        "dua_requests_enabled" to false, "halaqa_rooms_enabled" to false, "ask_scholar_enabled" to false,
        "challenges_enabled" to false, "share_cards_enabled" to false, "quran_offline_enabled" to false,
        "audio_routines_enabled" to false, "adhan_voices_enabled" to false, "car_mode_enabled" to false,
        "bayan_podcasts_enabled" to false,
    )
    val ALL_KEYS get() = DEFAULTS.keys
    fun default(key: String) = DEFAULTS[key] ?: false
    /** Tests / previews can force values. */
    @Volatile var override: Map<String, Boolean>? = null
    @Volatile private var loaded: Map<String, Boolean> = emptyMap()
    @Volatile private var started = false
    fun isOn(key: String) = override?.get(key) ?: loaded[key] ?: DEFAULTS[key] ?: false
    /** 1.1.6: called after Remote Config values are (re)applied, e.g. to enable/disable flag-gated components. */
    private val appliedListeners = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()
    fun onApplied(listener: () -> Unit) { appliedListeners += listener }

    fun load() {
        if (started) return
        started = true
        runCatching {
            if (!com.noorpro.app.data.FirebaseGate.ready) return
            val rc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            fun apply() {
                loaded = DEFAULTS.keys.mapNotNull { k ->
                    val v = rc.getValue(k)
                    if (v.source == com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_STATIC) null else k to v.asBoolean()
                }.toMap()
                appliedListeners.forEach { l -> runCatching { l() } }
            }
            apply()
            rc.fetchAndActivate().addOnCompleteListener { runCatching { apply() } }
        }
    }
}

// ---------------------------------------------------------------- frame / aspect

enum class ReelFrameMode { FULL_BLEED, FIT_BLUR }

data class ReelFrame(val mode: ReelFrameMode, val aspect: Float) {
    /** Only real landscape clips get the 'Full screen' pill (1:1 and 4:5 are already big enough). */
    val showFullScreenPill: Boolean get() = mode == ReelFrameMode.FIT_BLUR && aspect >= 1.15f
    val label: String get() = when {
        aspect <= 0f -> "unknown"
        aspect >= 1.6f -> "16:9"
        aspect >= 1.15f -> "landscape"
        aspect >= 0.95f -> "1:1"
        aspect >= 0.75f -> "4:5"
        else -> "9:16"
    }
}

/**
 * How a reel is framed in the vertical feed. Tall clips (9:16 up to ~3:4) fill the screen.
 * Square, 4:5 and landscape clips are never cropped: fitted to width and centred over a blurred,
 * darkened copy of the same frame. Uses the rotation-aware [displayedReelAspectRatio].
 */
fun reelFrameFor(width: Int, height: Int, rotationDegrees: Int = 0, pixelRatio: Float = 1f): ReelFrame {
    val a = displayedReelAspectRatio(width, height, pixelRatio, rotationDegrees)
    return reelFrameForAspect(a)
}

fun reelFrameForAspect(aspect: Float): ReelFrame = when {
    !aspect.isFinite() || aspect <= 0f -> ReelFrame(ReelFrameMode.FULL_BLEED, 0f)
    aspect < 0.72f -> ReelFrame(ReelFrameMode.FULL_BLEED, aspect)
    else -> ReelFrame(ReelFrameMode.FIT_BLUR, aspect)
}

/** Height in dp of a fitted (FIT_BLUR) clip on a screen [screenW]x[screenH] dp, never taller than 70% of it. */
fun fittedReelHeightDp(screenW: Float, screenH: Float, aspect: Float): Float {
    if (aspect <= 0f || screenW <= 0f) return 0f
    return (screenW / aspect).coerceAtMost(screenH * 0.7f)
}

// ---------------------------------------------------------------- tabs / topics

enum class Reels2Tab(val label: String) { FOR_YOU("For You"), FOLLOWING("Following"), NEARBY("Nearby") }

val reels2Topics = listOf("All", "Quran", "Naat", "Bayan", "Kids")

private val topicWords = mapOf(
    "Quran" to listOf("quran", "qur'an", "tilawat", "tilawah", "recitation", "surah", "ayah", "qari"),
    "Naat" to listOf("naat", "nasheed", "nazm", "hamd", "manqabat"),
    "Bayan" to listOf("bayan", "lecture", "khutbah", "talk", "reminder", "dars", "tafsir"),
    "Kids" to listOf("kids", "children", "child", "bachon", "bachchon", "qaida", "noorani"),
)

fun matchesReelTopic(post: UmmahPost, topic: String): Boolean {
    if (topic.isBlank() || topic == "All") return true
    val hay = (post.category + " " + post.caption).lowercase()
    return topicWords[topic].orEmpty().any { hay.contains(it) } || hay.contains(topic.lowercase())
}

/** Nearby = same city/area text in locationName (posts already carry it; no geo queries needed). */
fun isNearby(post: UmmahPost, myCity: String): Boolean {
    val city = myCity.trim().lowercase()
    if (city.isBlank()) return false
    return post.locationName.lowercase().contains(city)
}

fun reels2Feed(
    tab: Reels2Tab, topic: String, reels: List<UmmahPost>, following: Set<String>, myCity: String,
    hidden: Set<String> = emptySet(),
): List<UmmahPost> {
    val base = reels.filter { it.id !in hidden && it.creatorUid !in hidden }
    val tabbed = when (tab) {
        Reels2Tab.FOR_YOU -> base
        Reels2Tab.FOLLOWING -> base.filter { it.creatorUid in following }.sortedByDescending { it.publishedAt }
        Reels2Tab.NEARBY -> base.filter { isNearby(it, myCity) }.sortedByDescending { it.publishedAt }
    }
    return tabbed.filter { matchesReelTopic(it, topic) }
}

// ---------------------------------------------------------------- reactions

/** Halal reaction set (long-press like). Drawn with NoorPro vector glyphs, not emoji. */
enum class HalalReaction(val label: String, val color: Long) {
    LIKE("Like", 0xFFD9475A), MASHAALLAH("MashaAllah", 0xFF0B6243), SUBHANALLAH("SubhanAllah", 0xFF1F7A8C),
    AMEEN("Ameen", 0xFFA9842A), LOVE("Love", 0xFFC2416B), INSIGHTFUL("Insightful", 0xFF6B4FA3)
}

/** Picking any reaction also counts as a like (so the existing like counter/storage keeps working). */
fun reactionCountsAsLike(r: HalalReaction?): Boolean = r != null

/** Top reactions to show beside the count: most used first, at most 3. */
fun topReactions(counts: Map<HalalReaction, Long>): List<HalalReaction> =
    counts.filterValues { it > 0 }.entries.sortedWith(compareByDescending<Map.Entry<HalalReaction, Long>> { it.value }.thenBy { it.key.ordinal })
        .take(3).map { it.key }

// ---------------------------------------------------------------- playback controls

val reelSpeeds = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)
fun nextReelSpeed(current: Float): Float {
    val i = reelSpeeds.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
    return reelSpeeds[(if (i < 0) 1 else i + 1) % reelSpeeds.size]
}
fun speedLabel(s: Float): String = if (s % 1f == 0f) "${s.toInt()}x" else "${s}x"

fun reelClock(ms: Long): String {
    val t = (ms.coerceAtLeast(0) / 1000)
    val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// ---------------------------------------------------------------- chapters

data class ReelChapter(val startMs: Long, val title: String)

/**
 * Chapters from timestamp lines in the caption ("0:00 Intro", "12:40 - Sabr in hardship").
 * Needs at least 2 stamps, the first at 0:00, strictly increasing. Works with today's data.
 */
fun parseReelChapters(caption: String): List<ReelChapter> {
    val rx = Regex("""^\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})\s*[-–—:|]?\s*(.+)$""")
    val list = caption.lines().mapNotNull { line ->
        val m = rx.find(line) ?: return@mapNotNull null
        val h = m.groupValues[1].toLongOrNull() ?: 0L
        val mm = m.groupValues[2].toLong(); val ss = m.groupValues[3].toLong()
        if (ss >= 60) return@mapNotNull null
        ReelChapter(((h * 3600) + mm * 60 + ss) * 1000, m.groupValues[4].trim().take(60))
    }
    if (list.size < 2 || list.first().startMs != 0L) return emptyList()
    if (list.zipWithNext().any { (a, b) -> b.startMs <= a.startMs }) return emptyList()
    return list
}

fun chapterIndexAt(positionMs: Long, chapters: List<ReelChapter>): Int =
    chapters.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(if (chapters.isEmpty()) -1 else 0)

/** Caption text with the chapter lines removed (they are shown on the scrubber instead). */
fun captionWithoutChapters(caption: String): String {
    if (parseReelChapters(caption).isEmpty()) return caption
    val rx = Regex("""^\s*(?:\d{1,2}:)?\d{1,2}:\d{2}\s*[-–—:|]?\s*.+$""")
    return caption.lines().filterNot { rx.matches(it) }.joinToString("\n").trim()
}

// ---------------------------------------------------------------- series

data class ReelSeriesInfo(val title: String, val part: Int, val total: Int?)

/** "Seerah for kids — Part 2/5", "Sabr series pt 3", "Ep. 4 of 10" → series info (flagged UI). */
fun parseReelSeries(caption: String): ReelSeriesInfo? {
    val first = caption.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return null
    val m = Regex("""^(.*?)[\s\-–—|:,]*(?:part|pt\.?|ep\.?|episode)\s*(\d{1,3})(?:\s*(?:/|of)\s*(\d{1,3}))?""", RegexOption.IGNORE_CASE).find(first) ?: return null
    val title = m.groupValues[1].trim().trimEnd('-', '–', '—', '|', ':', ',').trim().ifBlank { return null }
    val part = m.groupValues[2].toInt()
    val total = m.groupValues[3].toIntOrNull()
    if (part < 1 || (total != null && part > total)) return null
    return ReelSeriesInfo(title, part, total)
}

/** The next part by the same creator with the same series title, if published. */
fun nextSeriesPart(current: UmmahPost, all: List<UmmahPost>): UmmahPost? {
    val s = parseReelSeries(current.caption) ?: return null
    return all.firstOrNull { p ->
        p.id != current.id && p.creatorUid == current.creatorUid &&
            parseReelSeries(p.caption)?.let { it.title.equals(s.title, true) && it.part == s.part + 1 } == true
    }
}

// ---------------------------------------------------------------- recitation mode

/** Quran recitation reels: category/caption says Quran and the post carries Arabic ayah text. */
fun isRecitationReel(post: UmmahPost): Boolean = post.arabicText.isNotBlank() && matchesReelTopic(post, "Quran")

/** Split the Arabic text into ayahs (۝ / ﴿n﴾ / Arabic-digit markers / newlines). */
fun splitAyahs(arabic: String): List<String> =
    arabic.split(Regex("""[۝\n]|﴿[^﴾]*﴾|[\u0660-\u0669]+"""))
        .map { it.trim() }.filter { it.length > 1 }

/** Even timing until per-ayah timestamps exist: which ayah is on screen at [positionMs]. */
fun ayahIndexAt(positionMs: Long, durationMs: Long, count: Int): Int {
    if (count <= 0) return -1
    if (durationMs <= 0) return 0
    return ((positionMs.coerceIn(0, durationMs - 1) * count) / durationMs).toInt().coerceIn(0, count - 1)
}

// ---------------------------------------------------------------- comments

data class Reels2Comment(val comment: UmmahComment, val pinned: Boolean, val creatorLiked: Boolean)

/** Pinned first, then creator-liked, then most liked, then newest. Replies are excluded. */
fun orderReelComments(comments: List<UmmahComment>, pinnedId: String, creatorLikedIds: Set<String> = emptySet()): List<Reels2Comment> =
    comments.filter { it.parentCommentId.isBlank() && it.status != "hidden" }
        .map { Reels2Comment(it, it.id == pinnedId && pinnedId.isNotBlank(), it.id in creatorLikedIds) }
        .sortedWith(compareByDescending<Reels2Comment> { it.pinned }.thenByDescending { it.creatorLiked }
            .thenByDescending { it.comment.likeCount }.thenByDescending { it.comment.createdAt })

// ---------------------------------------------------------------- why am I seeing this

fun whyThisReel(post: UmmahPost, following: Set<String>, likedTopics: Set<String>, myCity: String): List<String> {
    val out = mutableListOf<String>()
    if (post.creatorUid in following) out += "You follow ${post.creatorName.ifBlank { "this creator" }}."
    reels2Topics.drop(1).filter { matchesReelTopic(post, it) && it in likedTopics }.forEach { out += "You often watch $it reels." }
    if (isNearby(post, myCity)) out += "It was posted near ${myCity.trim()}."
    if (post.likeCount + post.saveCount >= 100) out += "It is popular with the Ummah right now."
    if (out.isEmpty()) out += "It's new and matches what people in your region watch."
    return out
}

// ---------------------------------------------------------------- continue watching

data class WatchProgress(val postId: String, val positionMs: Long, val durationMs: Long, val updatedAt: Long) {
    val fraction: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** Long videos (> 60 s) watched 5%..90%, newest first. */
fun continueWatching(progress: Collection<WatchProgress>, posts: List<UmmahPost>, limit: Int = 10): List<Pair<UmmahPost, WatchProgress>> {
    val byId = posts.associateBy { it.id }
    return progress.filter { it.durationMs > 60_000 && it.fraction in 0.05f..0.9f }
        .sortedByDescending { it.updatedAt }
        .mapNotNull { p -> byId[p.postId]?.let { it to p } }.take(limit)
}

// ---------------------------------------------------------------- smart search

data class SmartReelQuery(val topic: String, val language: String?, val raw: String)

private val langWords = mapOf("urdu" to "ur", "hindi" to "hi", "arabic" to "ar", "english" to "en", "bangla" to "bn", "bengali" to "bn")
private val synonyms = mapOf("patience" to "sabr", "gratitude" to "shukr", "prayer" to "salah", "namaz" to "salah", "trust" to "tawakkul", "repentance" to "tawbah")

/** "find reels about patience in Urdu" → topic "sabr" (+patience), language "ur". Offline, instant. */
fun parseSmartReelQuery(input: String): SmartReelQuery {
    var q = input.lowercase().trim()
    var lang: String? = null
    Regex("""\b(?:in|ka|ki|mein|me)\s+(urdu|hindi|arabic|english|bangla|bengali)\b|\b(urdu|hindi|arabic|english|bangla|bengali)\b""").find(q)?.let { m ->
        lang = langWords[(m.groupValues[1].ifBlank { m.groupValues[2] })]
        q = q.removeRange(m.range)
    }
    q = q.replace(Regex("""\b(find|show|me|search|reels?|videos?|clips?|about|on|for|please|some|the|a)\b"""), " ")
        .replace(Regex("\\s+"), " ").trim()
    val topic = synonyms[q] ?: q
    return SmartReelQuery(topic, lang, input)
}

fun smartReelMatches(query: SmartReelQuery, posts: List<UmmahPost>): List<UmmahPost> {
    if (query.topic.isBlank()) return emptyList()
    val words = setOf(query.topic) + synonyms.filterValues { it == query.topic }.keys
    return posts.filter { p -> val hay = (p.caption + " " + p.category).lowercase(); words.any { hay.contains(it) } }
}

// ---------------------------------------------------------------- AI prompt framing (askNoorAi, existing AskNoor mode)

object Reels2AiPrompts {
    fun about(post: UmmahPost): String =
        "A viewer on NoorPro asks about this short Islamic video. Caption: \"${post.caption.take(800)}\"" +
            (if (post.arabicText.isNotBlank()) " Arabic text shown: \"${post.arabicText.take(400)}\"." else ".") +
            (if (post.sourceReference.isNotBlank()) " Creator's source: ${post.sourceReference}." else "") +
            " Give: SUMMARY: 2 sentences. REFERENCE: the ayah or hadith it refers to with exact source, or 'none stated'. " +
            "SIMPLE: explain it simply for a 12-year-old in 3 sentences. Do not invent references or give fatwa."

    fun section(answer: String, key: String): String =
        Regex("""$key:\s*(.+?)(?=\n?\s*(?:SUMMARY|REFERENCE|SIMPLE):|$)""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(answer.substringBefore("FOLLOWUPS:"))?.groupValues?.get(1)?.trim().orEmpty()
}

val reels2CaptionLanguages = listOf("EN", "UR", "HI", "AR")

fun reelCount(n: Long): String = when {
    n >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", n / 1_000_000.0).replace(".0M", "M")
    n >= 10_000 -> "${n / 1000}K"
    n >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", n / 1000.0).replace(".0K", "K")
    else -> n.toString()
}
