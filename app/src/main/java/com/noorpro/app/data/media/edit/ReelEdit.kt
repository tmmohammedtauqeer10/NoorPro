package com.noorpro.app.data.media.edit

import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Track
import kotlin.math.roundToLong

/** One kept piece of the source clip, in source ms. */
data class ReelClip(val startMs: Long, val endMs: Long) {
    val lengthMs get() = (endMs - startMs).coerceAtLeast(0)
}

enum class ReelSpeed(val factor: Float, val label: String) {
    HALF(0.5f, "0.5x"), NORMAL(1f, "1x"), FAST(1.5f, "1.5x"), DOUBLE(2f, "2x")
}

/** Background sound for a reel — only from Al-Noor Audio tracks whose licence allows reuse in a new video, with credit. */
data class ReelSound(
    val trackId: String,
    val title: String,
    val artist: String,
    val url: String,
    val licenseLabel: String,
    val sourceUrl: String
) {
    /** Short licence name for the credit line. */
    val licenseShort get() = when {
        licenseLabel.contains("Public Domain", true) -> "Public domain"
        licenseLabel.contains("CC0", true) -> "CC0"
        licenseLabel.contains("CC BY", true) -> "CC BY"
        else -> licenseLabel.take(20)
    }
    /** Shown on the reel ("Sound: …") through the existing credit / sourceReference field (≤ 80 chars like the field). */
    val credit get() = "$title — $artist ($licenseShort)".let { if (it.length <= 80) it else "${title.take(40)}… — ${artist.take(20)} ($licenseShort)".take(80) }
}

object ReelSounds {
    /** Licences that allow putting the audio into a new video: public domain / CC0 / CC BY (with credit). */
    private val reusable = setOf(LicenseType.PUBLIC_DOMAIN, LicenseType.CC0, LicenseType.CC_BY)

    /**
     * Only own/licensed Al-Noor Audio tracks. Excludes stream-only sources (SOURCE_TERMS, e.g. Quran CDN
     * recitations), podcast RSS, share-alike, permission letters, and anything that is not a direct audio file.
     * Never YouTube, never CC-bayan streams, never commercial music.
     */
    fun eligible(t: Track): Boolean {
        val url = t.audioUrl.lowercase()
        return t.license in reusable && url.startsWith("https://") && !url.contains("youtube") && !url.contains("youtu.be") &&
            !url.contains("googlevideo") && t.attributionText.isNotBlank()
    }

    fun from(t: Track): ReelSound? = if (!eligible(t)) null else ReelSound(
        t.id, t.title.removeSuffix(" …").trim(), t.artistName,
        t.audioUrl, t.attributionText.substringAfter(" · ", t.attributionText), t.sourceUrl
    )
}

/**
 * Everything the reel editor changes. Layer times are on the OUTPUT timeline (after trim/split + speed),
 * which is what the editor shows and what the exporter's overlay sees.
 */
data class ReelEdit(
    val sourceDurationMs: Long,
    val clips: List<ReelClip> = listOf(ReelClip(0, sourceDurationMs)),
    val speed: ReelSpeed = ReelSpeed.NORMAL,
    val filter: NoorFilter = NoorFilter.ORIGINAL,
    val strength: Float = 1f,
    val adjust: Adjustments = Adjustments(),
    val layers: List<EditorLayer> = emptyList(),
    val originalVolume: Float = 1f,
    val sound: ReelSound? = null,
    val soundVolume: Float = 0.6f,
    val coverMs: Long = 0L
) {
    val keptMs get() = clips.sumOf { it.lengthMs }
    val outputMs get() = (keptMs / speed.factor).roundToLong()
    val muted get() = originalVolume <= 0.001f
    val matrix get() = colorMatrixFor(filter, strength, adjust)
    val trimStartMs get() = clips.firstOrNull()?.startMs ?: 0
    val trimEndMs get() = clips.lastOrNull()?.endMs ?: sourceDurationMs

    /** Single-range trim (keeps the existing slider working). */
    fun trim(startMs: Long, endMs: Long): ReelEdit {
        val s = startMs.coerceIn(0, sourceDurationMs); val e = endMs.coerceIn(s + MIN_CLIP_MS, sourceDurationMs.coerceAtLeast(s + MIN_CLIP_MS))
        return copy(clips = listOf(ReelClip(s, e))).clampLayers()
    }

    /** Split at an output-timeline position into two clips. No-op if either side would be < MIN_CLIP_MS. */
    fun splitAt(atMs: Long): ReelEdit {
        val src = keptOffset((atMs * speed.factor).roundToLong()) ?: return this
        val (i, at) = src
        val c = clips[i]
        if (at - c.startMs < MIN_CLIP_MS || c.endMs - at < MIN_CLIP_MS) return this
        val next = clips.toMutableList().apply { removeAt(i); add(i, ReelClip(at, c.endMs)); add(i, ReelClip(c.startMs, at)) }
        return copy(clips = next)
    }

    /** Remove a clip (never the last one). */
    fun deleteClip(i: Int): ReelEdit = if (clips.size <= 1 || i !in clips.indices) this else copy(clips = clips.filterIndexed { k, _ -> k != i }).clampLayers()

    fun withSpeed(s: ReelSpeed): ReelEdit {
        // Keep layers at the same moment of the source when speed changes.
        val k = speed.factor / s.factor
        return copy(speed = s, layers = layers.map { l ->
            l.copy(startMs = (l.startMs * k).roundToLong(), endMs = if (l.endMs == Long.MAX_VALUE) l.endMs else (l.endMs * k).roundToLong())
        }).clampLayers()
    }

    /** Output ms -> (clip index, source ms). */
    fun sourceAt(atMs: Long): Pair<Int, Long>? = keptOffset((atMs.coerceIn(0, outputMs) * speed.factor).roundToLong())

    private fun keptOffset(keptMs: Long): Pair<Int, Long>? {
        var left = keptMs
        clips.forEachIndexed { i, c -> if (left <= c.lengthMs) return i to (c.startMs + left); left -= c.lengthMs }
        return null
    }

    /** Clip boundaries on the output timeline (for the split markers). */
    fun cutsOnOutput(): List<Long> {
        var acc = 0L
        return clips.dropLast(1).map { acc += it.lengthMs; (acc / speed.factor).roundToLong() }
    }

    fun upsert(l: EditorLayer) = copy(layers = layers.filterNot { it.id == l.id } + l)
    fun remove(id: String) = copy(layers = layers.filterNot { it.id == id })

    private fun clampLayers(): ReelEdit {
        val total = outputMs
        return copy(layers = layers.map { l -> if (l.startMs == 0L && l.endMs == Long.MAX_VALUE) l else l.timed(l.startMs, minOf(l.endMs, total), total) })
    }

    companion object { const val MIN_CLIP_MS = 1000L }
}

/** Exactly what the exporter will do — kept separate (and testable) from the media3 calls. */
data class ReelExportParams(
    val clips: List<ReelClip>,
    val speed: Float,
    val removeAudio: Boolean,
    val originalVolume: Float,
    val soundUrl: String?,
    val soundVolume: Float,
    val colorMatrixGl: FloatArray?,
    val vignette: Float,
    val layers: List<EditorLayer>,
    val outputWidth: Int,
    val outputHeight: Int,
    val expectedDurationMs: Long
) {
    val hasOverlay get() = layers.isNotEmpty() || vignette > 0f
    val needsVideoEffects get() = colorMatrixGl != null || hasOverlay || speed != 1f

    companion object {
        const val MAX_LONG_SIDE = 1280

        /** Output size: keep aspect, longest side ≤ 1280, even dimensions (H.264). */
        fun outputSize(srcW: Int, srcH: Int): Pair<Int, Int> {
            if (srcW <= 0 || srcH <= 0) return 720 to 1280
            val k = minOf(1f, MAX_LONG_SIDE.toFloat() / maxOf(srcW, srcH))
            fun even(v: Float) = (v.toInt() / 2 * 2).coerceAtLeast(2)
            return even(srcW * k) to even(srcH * k)
        }

        fun from(e: ReelEdit, srcW: Int, srcH: Int): ReelExportParams {
            val (w, h) = outputSize(srcW, srcH)
            val m = e.matrix
            return ReelExportParams(
                clips = e.clips, speed = e.speed.factor,
                removeAudio = e.muted, originalVolume = e.originalVolume.coerceIn(0f, 1f),
                soundUrl = e.sound?.url, soundVolume = e.soundVolume.coerceIn(0f, 1f),
                colorMatrixGl = if (m.isIdentity()) null else m.toGl(), vignette = e.adjust.vignette,
                layers = e.layers, outputWidth = w, outputHeight = h, expectedDurationMs = e.outputMs
            )
        }

        /** Only a plain single trim (or nothing) → the existing 1.1.1 trim path is enough. */
        fun needsFullExport(e: ReelEdit): Boolean =
            e.clips.size > 1 || e.speed != ReelSpeed.NORMAL || !e.matrix.isIdentity() || e.adjust.vignette > 0f ||
                e.layers.isNotEmpty() || e.originalVolume < 0.999f || e.sound != null
    }
}
