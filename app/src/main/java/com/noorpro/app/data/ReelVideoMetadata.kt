package com.noorpro.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

/** Original video geometry captured before AWS transcoding changes the container. */
data class ReelVideoMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val rotationDegrees: Int = 0
) {
    val hasDimensions: Boolean get() = width > 0 && height > 0

    val displayAspectRatio: Float
        get() {
            if (!hasDimensions) return 0f
            return if (rotationDegrees == 90 || rotationDegrees == 270) {
                height.toFloat() / width.toFloat()
            } else {
                width.toFloat() / height.toFloat()
            }
        }
}

/** Reads both pixel dimensions and phone rotation metadata from a selected local video. */
fun readReelVideoMetadata(context: Context, uri: Uri): ReelVideoMetadata {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull()
            ?.let { ((it % 360) + 360) % 360 }
            ?.takeIf { it == 0 || it == 90 || it == 180 || it == 270 }
            ?: 0
        ReelVideoMetadata(width = width, height = height, rotationDegrees = rotation)
    } catch (_: Exception) {
        ReelVideoMetadata()
    } finally {
        runCatching { retriever.release() }
    }
}
