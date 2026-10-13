package com.noorpro.app.data.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Path.Companion.toOkioPath

/**
 * Coil model for "show a frame of this remote video". Unlike handing the video URL to Coil's
 * VideoFrameDecoder (which downloads the entire file first, so large / 4K reels never showed a
 * thumbnail), this asks MediaMetadataRetriever to stream just what it needs via HTTP range
 * requests, then caches a small JPEG on disk.
 */
data class RemoteVideoFrame(val url: String)

/** Model for a post's preview: its thumbnail if it has one, else a frame from its MP4. */
fun videoPreviewModel(thumbnailUrl: String, mediaUrl: String): Any? = when {
    thumbnailUrl.isNotBlank() -> thumbnailUrl
    mediaUrl.isNotBlank() -> RemoteVideoFrame(mediaUrl)
    else -> null
}

class RemoteVideoFrameFetcher(
    private val data: RemoteVideoFrame,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val dir = File(options.context.cacheDir, "video_frames").apply { mkdirs() }
        val file = File(dir, sha1(data.url.substringBefore('?') + "|" + data.url.substringAfter("token=", "")) + ".jpg")
        if (!file.exists() || file.length() == 0L) {
            gate.withPermit {
                if (!file.exists() || file.length() == 0L) extract(file)
            }
        }
        return SourceResult(
            source = ImageSource(file.toOkioPath(), okio.FileSystem.SYSTEM),
            mimeType = "image/jpeg",
            dataSource = DataSource.DISK
        )
    }

    private fun extract(target: File) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(data.url, HashMap())
            val frame: Bitmap = (if (Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 540, 960)
            } else {
                retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }) ?: retriever.getFrameAtTime(0) ?: error("no frame")
            val tmp = File(target.parentFile, target.name + ".tmp")
            tmp.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            frame.recycle()
            tmp.renameTo(target)
        } catch (e: Exception) {
            Log.w("NoorReels", "thumbnail frame failed url=${MediaUrls.redact(data.url)}: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        } finally {
            runCatching { retriever.release() }
        }
    }

    class Factory : Fetcher.Factory<RemoteVideoFrame> {
        override fun create(data: RemoteVideoFrame, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (MediaUrls.isHttp(data.url)) RemoteVideoFrameFetcher(data, options) else null
    }

    private companion object {
        // Frame extraction opens a network stream + decoder; keep it to two at a time.
        val gate = Semaphore(2)
        fun sha1(s: String): String =
            MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
