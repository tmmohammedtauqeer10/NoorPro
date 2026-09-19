package com.example.ui.screens

import com.example.data.UmmahPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelExperienceLogicTest {
    @Test
    fun portraitPixelsRemainPortrait() {
        assertFalse(isWideReelVideo(width = 1080, height = 1920))
    }

    @Test
    fun landscapePixelsWithQuarterTurnMetadataBecomePortrait() {
        assertFalse(
            isWideReelVideo(
                width = 1920,
                height = 1080,
                rotationDegrees = 90
            )
        )
        assertEquals(
            1080f / 1920f,
            displayedReelAspectRatio(1920, 1080, rotationDegrees = 270),
            0.001f
        )
    }

    @Test
    fun genuineLandscapeVideoOffersWideViewer() {
        assertTrue(isWideReelVideo(width = 1920, height = 1080))
    }

    @Test
    fun uploadedPhoneRotationControlsReelLayout() {
        assertEquals(
            1080f / 1920f,
            uploadedReelAspectRatio(1920, 1080, 90)!!,
            0.001f
        )
        assertTrue(isWideReelVideo(width = 1920, height = 1080, rotationDegrees = 0))
    }

    @Test
    fun onlySquareAndWideFramesUseFitLayout() {
        assertFalse(shouldFitReelFrame(9f / 16f))
        assertTrue(shouldFitReelFrame(1f))
        assertTrue(shouldFitReelFrame(16f / 9f))
    }

    @Test
    fun profileTargetNeverFallsBackToPreviousReel() {
        assertEquals(1, requestedReelIndex(listOf("old", "new", "other"), "new"))
        assertNull(requestedReelIndex(listOf("old", "other"), "new"))
        assertNull(requestedReelIndex(listOf("old"), " "))
    }

    @Test
    fun liveFeedUpdatesKeepTheCurrentReelAtItsPage() {
        assertEquals(
            listOf("first", "watching", "new"),
            stableReelOrder(
                previousIds = listOf("first", "watching", "removed"),
                rankedIds = listOf("new", "watching", "first")
            )
        )
        assertEquals(listOf("new", "first"), stableReelOrder(emptyList(), listOf("new", "first")))
    }

    @Test
    fun ummahAdsStayBetweenPostsAtAWideCadence() {
        assertFalse(shouldShowUmmahFeedAd(0))
        assertTrue(shouldShowUmmahFeedAd(1))
        assertFalse(shouldShowUmmahFeedAd(2))
        assertFalse(shouldShowUmmahFeedAd(8))
        assertTrue(shouldShowUmmahFeedAd(9))
    }

    @Test
    fun wideFrameKeepsFullVideoAndActionsInsidePortraitViewport() {
        val frame = wideReelFrame(360f, 700f, 16f / 9f)
        assertEquals(360f, frame.widthDp, 0.01f)
        assertEquals(202.5f, frame.heightDp, 0.01f)
        assertTrue(frame.topDp >= 112f)
        assertTrue(frame.actionsTopDp + 48f < 700f - 126f)
    }

    @Test
    fun feedbackChangesCreatorRankingAndHidesOnlySelectedReel() {
        val selected = feedbackPost("selected", "creator-a", "Quran")
        val sameCreator = feedbackPost("next", "creator-a", "Quran")
        val otherCreator = feedbackPost("other", "creator-b", "Quran")

        val positive = ReelFeedback().interestedIn(selected)
        assertTrue(reelFeedbackScore(sameCreator, positive) > reelFeedbackScore(otherCreator, positive))
        assertTrue(positive.hiddenReelIds.isEmpty())

        val negative = positive.notInterestedIn(selected)
        assertTrue("selected" in negative.hiddenReelIds)
        assertFalse("next" in negative.hiddenReelIds)
        assertTrue(reelFeedbackScore(sameCreator, negative) < reelFeedbackScore(otherCreator, negative))
    }

    @Test
    fun paddedLandscapeStreamFallsBackToOriginalPortraitVideo() {
        assertTrue(shouldFallbackFromPaddedHls(9f / 16f, 16f / 9f))
        assertFalse(shouldFallbackFromPaddedHls(9f / 16f, 9f / 16f))
        assertFalse(shouldFallbackFromPaddedHls(null, 16f / 9f))
    }

    @Test
    fun explicitFeedbackLearnsSimilarReelsAcrossCreators() {
        val chosen = feedbackPost("chosen", "creator-a", "Reminder").copy(caption = "Beautiful naat from Madinah")
        val similar = feedbackPost("similar", "creator-b", "Reminder").copy(caption = "Madinah naat recitation")
        val unrelated = feedbackPost("other", "creator-c", "Reminder").copy(caption = "Cooking a family meal")

        val interested = ReelFeedback().interestedIn(chosen)
        assertTrue(reelFeedbackScore(similar, interested) > reelFeedbackScore(unrelated, interested))

        val uninterested = ReelFeedback().notInterestedIn(chosen)
        assertTrue(reelFeedbackScore(similar, uninterested) < reelFeedbackScore(unrelated, uninterested))
        assertTrue(chosen.id in uninterested.hiddenReelIds)
    }

    @Test
    fun successfulLikeIsWeakerThanExplicitInterestAndModelStaysBounded() {
        val chosen = feedbackPost("chosen", "creator-a", "Reminder").copy(caption = "Madinah naat recitation")
        val related = feedbackPost("related", "creator-b", "Reminder").copy(caption = "Madinah naat")
        val liked = ReelFeedback().positiveSignal(chosen, 0.25f)
        val explicit = ReelFeedback().interestedIn(chosen)
        assertTrue(reelFeedbackScore(related, liked) > 0.0)
        assertTrue(reelFeedbackScore(related, explicit) > reelFeedbackScore(related, liked))

        val manySignals = (1..80).fold(ReelFeedback()) { feedback, index ->
            feedback.positiveSignal(chosen.copy(caption = "unique${index} word${index} topic${index}"), 0.45f)
        }
        assertTrue(manySignals.topicWeights.size <= 128)
    }

    private fun feedbackPost(id: String, creator: String, category: String) = UmmahPost(
        id = id,
        type = "reel",
        mediaUrl = "https://example.com/$id.mp4",
        thumbnailUrl = "",
        creatorUid = creator,
        creatorName = creator,
        creatorHandle = creator,
        caption = "",
        sourceReference = "",
        publishedAt = 0L,
        category = category
    )
}
