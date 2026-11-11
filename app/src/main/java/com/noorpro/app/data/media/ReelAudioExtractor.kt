package com.noorpro.app.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

/**
 * 1.2.0 (113): copies a reel's audio track (no re-encode) into a small .m4a for Noor AI subtitles.
 * AAC is remuxed as-is, so this is fast and needs no codec work. Returns null for videos without
 * AAC audio or when the result would be too large to upload.
 */
object ReelAudioExtractor {
    const val MAX_MS = 180_000L
    const val MAX_BYTES = 4_400_000

    fun extractM4a(context: Context, uri: Uri, maxMs: Long = MAX_MS): ByteArray? {
        val extractor = MediaExtractor()
        val out = File(context.cacheDir, "noorai_subs_${System.nanoTime()}.m4a")
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_AAC
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val m = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }
            val dst = m.addTrack(format)
            m.start()
            val size = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024) else 256 * 1024
            val buf = ByteBuffer.allocate(size)
            val info = MediaCodec.BufferInfo()
            var written = 0L
            while (true) {
                info.size = extractor.readSampleData(buf, 0)
                if (info.size < 0) break
                info.offset = 0
                info.presentationTimeUs = extractor.sampleTime
                if (info.presentationTimeUs > maxMs * 1000) break
                @Suppress("WrongConstant")
                info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                m.writeSampleData(dst, buf, info)
                written += info.size
                if (written > MAX_BYTES) break
                extractor.advance()
            }
            m.stop()
            out.readBytes().takeIf { it.isNotEmpty() && it.size <= MAX_BYTES + 200_000 }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
            out.delete()
        }
    }
}
