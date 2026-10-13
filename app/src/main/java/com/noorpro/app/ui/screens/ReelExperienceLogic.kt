package com.noorpro.app.ui.screens

/**
 * Returns the aspect ratio of the frame as the viewer actually sees it.
 *
 * Many Android phones record portrait video as landscape pixels plus 90/270 degree rotation
 * metadata. Looking only at the stored width and height therefore classifies a portrait reel as
 * landscape and makes PlayerView letterbox it into a small box.
 */
internal fun displayedReelAspectRatio(
    width: Int,
    height: Int,
    pixelWidthHeightRatio: Float = 1f,
    rotationDegrees: Int = 0
): Float {
    if (width <= 0 || height <= 0) return 0f
    val pixelRatio = pixelWidthHeightRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
    val storedWidth = width.toFloat() * pixelRatio
    val storedHeight = height.toFloat()
    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
    return if (normalizedRotation == 90 || normalizedRotation == 270) {
        storedHeight / storedWidth
    } else {
        storedWidth / storedHeight
    }
}

internal fun isWideReelVideo(
    width: Int,
    height: Int,
    pixelWidthHeightRatio: Float = 1f,
    rotationDegrees: Int = 0
): Boolean = displayedReelAspectRatio(width, height, pixelWidthHeightRatio, rotationDegrees) > 1.15f

/** Uploaded source metadata is the authority when HLS has changed the encoded frame. */
internal fun uploadedReelAspectRatio(
    width: Int,
    height: Int,
    rotationDegrees: Int
): Float? = displayedReelAspectRatio(width, height, rotationDegrees = rotationDegrees)
    .takeIf { it.isFinite() && it > 0f }

/** Older HLS jobs could letterbox a portrait source inside a landscape output. */
internal fun shouldFallbackFromPaddedHls(sourceAspect: Float?, streamAspect: Float): Boolean =
    sourceAspect != null && sourceAspect.isFinite() && sourceAspect > 0f &&
        streamAspect.isFinite() && streamAspect > 0f &&
        kotlin.math.abs(streamAspect / sourceAspect - 1f) > 0.18f

/** Portrait clips fill the phone. Square/wide media stays fully visible without destructive crop. */
internal fun shouldFitReelFrame(aspectRatio: Float): Boolean =
    aspectRatio.isFinite() && aspectRatio >= 0.85f

/** Reserve room for the header, controls, and creator details below a wide clip. */
internal data class WideReelFrame(
    val widthDp: Float,
    val heightDp: Float,
    val topDp: Float
) {
    val actionsTopDp: Float get() = topDp + heightDp + 12f
}

internal fun wideReelFrame(viewportWidthDp: Float, viewportHeightDp: Float, aspectRatio: Float): WideReelFrame {
    val width = viewportWidthDp.coerceAtLeast(1f)
    val height = viewportHeightDp.coerceAtLeast(1f)
    val aspect = aspectRatio.takeIf { it.isFinite() && it > 1.15f } ?: (16f / 9f)
    val frameHeight = (width / aspect).coerceAtMost((height - 360f).coerceAtLeast(120f))
    val frameWidth = frameHeight * aspect
    val top = ((height - 180f - frameHeight) / 2f).coerceAtLeast(112f)
        .coerceAtMost((height - frameHeight - 80f).coerceAtLeast(0f))
    return WideReelFrame(frameWidth, frameHeight, top)
}

/** Return an index only when the exact requested reel exists. Never fall back to an old page. */
internal fun requestedReelIndex(reelIds: List<String>, requestedId: String): Int? {
    val cleanId = requestedId.trim()
    if (cleanId.isEmpty()) return null
    return reelIds.indexOfFirst { it == cleanId }.takeIf { it >= 0 }
}

/** Keep the current reel anchored while live counts and profiles update the feed. */
internal fun stableReelOrder(previousIds: List<String>, rankedIds: List<String>): List<String> {
    val available = rankedIds.toSet()
    val retained = previousIds.filter { it in available }
    val retainedSet = retained.toSet()
    return retained + rankedIds.filterNot { it in retainedSet }
}

/**
 * A reel opened from Profile, Saved, a notification, or immediately after upload starts a fresh
 * viewing session at page zero. Keeping the rest of the stable feed after it is important: if a
 * brand-new reel were merely appended to the end, the pager would open on its final page and the
 * member could not swipe forward to another reel.
 */
internal fun reelOrderWithFocus(
    previousIds: List<String>,
    rankedIds: List<String>,
    focusedId: String
): List<String> {
    val stable = stableReelOrder(previousIds, rankedIds)
    val cleanFocus = focusedId.trim()
    if (cleanFocus.isEmpty() || cleanFocus !in stable) return stable
    return listOf(cleanFocus) + stable.filterNot { it == cleanFocus }
}

/** Keep a successful local interaction visible until its Firestore counter snapshot catches up. */
internal fun displayedEngagementCount(
    remoteCount: Long,
    activeForCurrentUser: Boolean,
    optimisticCount: Long?
): Long = optimisticCount
    ?: maxOf(remoteCount, if (activeForCurrentUser) 1L else 0L)

internal fun nextEngagementCount(currentCount: Long, activating: Boolean): Long =
    (currentCount + if (activating) 1L else -1L).coerceAtLeast(0L)

/**
 * Ads stay between ordinary Ummah cards: one after the first post and then only after eight more.
 * This gives a new/small feed one useful placement without making a busy feed feel ad-heavy.
 */
internal fun shouldShowUmmahFeedAd(nextPostPosition: Int): Boolean =
    nextPostPosition > 0 && (nextPostPosition - 1) % 8 == 0
