package com.example.ui.screens

import android.content.Context
import com.example.data.UmmahPost
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
import org.json.JSONObject

/** Small, account-scoped on-device preference signals for the Reels feed. */
internal data class ReelFeedback(
    val interestedCreators: Set<String> = emptySet(),
    val lessCreators: Set<String> = emptySet(),
    val interestedCategories: Set<String> = emptySet(),
    val lessCategories: Set<String> = emptySet(),
    val hiddenReelIds: Set<String> = emptySet(),
    val topicWeights: Map<String, Float> = emptyMap()
)

private fun feedbackCategory(category: String): String? =
    category.trim().lowercase(Locale.ROOT).takeIf { it.isNotEmpty() && it != "reminder" }

private val reelWordPattern = Regex("""[\p{L}\p{N}]{3,}""")
private val reelStopWords = setOf(
    "the", "and", "for", "you", "with", "this", "that", "from", "your", "have", "will",
    "are", "our", "was", "allah", "islam", "islamic", "noor", "الله", "هذه", "هذا"
)

/** Short, bounded content features keep training and scoring off the video playback path. */
internal fun reelTopicTerms(post: UmmahPost): Set<String> = sequenceOf(
    feedbackCategory(post.category).orEmpty(),
    post.caption.take(280),
    post.sourceReference.take(100),
    post.arabicText.take(120)
).joinToString(" ").lowercase(Locale.ROOT).let { reelWordPattern.findAll(it) }
    .map { it.value }
    .filterNot { it in reelStopWords }
    .distinct()
    .take(16)
    .toSet()

private fun updatedTopicWeights(
    existing: Map<String, Float>,
    terms: Set<String>,
    signal: Float
): Map<String, Float> {
    val updated = existing.mapValues { (_, weight) -> weight * 0.98f }
        .filterValues { abs(it) >= 0.03f }
        .toMutableMap()
    terms.forEach { term ->
        updated[term] = ((updated[term] ?: 0f) + signal).coerceIn(-3f, 3f)
    }
    return updated.entries.sortedByDescending { abs(it.value) }.take(128)
        .associate { it.key to it.value }
}

internal fun ReelFeedback.interestedIn(post: UmmahPost): ReelFeedback {
    val creator = post.creatorUid.takeIf { it.isNotBlank() }
    val category = feedbackCategory(post.category)
    return copy(
        interestedCreators = if (creator == null) interestedCreators else interestedCreators + creator,
        lessCreators = if (creator == null) lessCreators else lessCreators - creator,
        interestedCategories = if (category == null) interestedCategories else interestedCategories + category,
        lessCategories = if (category == null) lessCategories else lessCategories - category,
        hiddenReelIds = hiddenReelIds - post.id,
        topicWeights = updatedTopicWeights(topicWeights, reelTopicTerms(post), 0.8f)
    )
}

internal fun ReelFeedback.notInterestedIn(post: UmmahPost): ReelFeedback {
    val creator = post.creatorUid.takeIf { it.isNotBlank() }
    val category = feedbackCategory(post.category)
    return copy(
        interestedCreators = if (creator == null) interestedCreators else interestedCreators - creator,
        lessCreators = if (creator == null) lessCreators else lessCreators + creator,
        interestedCategories = if (category == null) interestedCategories else interestedCategories - category,
        lessCategories = if (category == null) lessCategories else lessCategories + category,
        hiddenReelIds = (hiddenReelIds + post.id).toList().takeLast(500).toSet(),
        topicWeights = updatedTopicWeights(topicWeights, reelTopicTerms(post), -1f)
    )
}

/** A successful Like or Save is a weaker positive signal than an explicit Interested choice. */
internal fun ReelFeedback.positiveSignal(post: UmmahPost, strength: Float): ReelFeedback =
    copy(topicWeights = updatedTopicWeights(topicWeights, reelTopicTerms(post), strength))

internal fun reelFeedbackScore(post: UmmahPost, feedback: ReelFeedback): Double {
    val category = feedbackCategory(post.category)
    val terms = if (feedback.topicWeights.isEmpty()) emptySet() else reelTopicTerms(post)
    val learnedScore = if (terms.isEmpty()) 0.0 else {
        (terms.sumOf { (feedback.topicWeights[it] ?: 0f).toDouble() } / sqrt(terms.size.toDouble()) * 11.0)
            .coerceIn(-30.0, 30.0)
    }
    return (if (post.creatorUid in feedback.interestedCreators) 32.0 else 0.0) -
        (if (post.creatorUid in feedback.lessCreators) 28.0 else 0.0) +
        (if (category != null && category in feedback.interestedCategories) 12.0 else 0.0) -
        (if (category != null && category in feedback.lessCategories) 12.0 else 0.0) +
        learnedScore
}

/** This small recommendation model trains locally; no captions or feedback leave the device. */
internal class ReelFeedbackStore(context: Context, userId: String) {
    private val preferences = context.getSharedPreferences("noor_reel_feedback", Context.MODE_PRIVATE)
    private val prefix = "${userId.ifBlank { "guest" }}."

    fun load(): ReelFeedback = ReelFeedback(
        interestedCreators = read("interested_creators"),
        lessCreators = read("less_creators"),
        interestedCategories = read("interested_categories"),
        lessCategories = read("less_categories"),
        hiddenReelIds = read("hidden_reels"),
        topicWeights = readTopicWeights()
    )

    fun markInterested(post: UmmahPost): ReelFeedback =
        load().interestedIn(post).also(::save)

    fun markNotInterested(post: UmmahPost): ReelFeedback =
        load().notInterestedIn(post).also(::save)

    fun recordPositive(post: UmmahPost, strength: Float): ReelFeedback =
        load().positiveSignal(post, strength).also(::save)

    fun unhide(postId: String): ReelFeedback =
        load().let { it.copy(hiddenReelIds = it.hiddenReelIds - postId) }.also(::save)

    fun reset(): ReelFeedback = ReelFeedback().also(::save)

    private fun read(name: String): Set<String> =
        preferences.getStringSet(prefix + name, emptySet())?.toSet().orEmpty()

    private fun readTopicWeights(): Map<String, Float> = runCatching {
        val json = JSONObject(preferences.getString(prefix + "topic_weights", "{}") ?: "{}")
        json.keys().asSequence().take(128).mapNotNull { term ->
            json.optDouble(term, 0.0).toFloat().takeIf { it.isFinite() && it != 0f }
                ?.let { term to it.coerceIn(-3f, 3f) }
        }.toMap()
    }.getOrDefault(emptyMap())

    private fun save(feedback: ReelFeedback) {
        val topics = JSONObject().apply {
            feedback.topicWeights.forEach { (term, weight) -> put(term, weight.toDouble()) }
        }
        preferences.edit()
            .putStringSet(prefix + "interested_creators", feedback.interestedCreators)
            .putStringSet(prefix + "less_creators", feedback.lessCreators)
            .putStringSet(prefix + "interested_categories", feedback.interestedCategories)
            .putStringSet(prefix + "less_categories", feedback.lessCategories)
            .putStringSet(prefix + "hidden_reels", feedback.hiddenReelIds)
            .putString(prefix + "topic_weights", topics.toString())
            .apply()
    }
}
