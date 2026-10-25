package com.noorpro.app.data.media.edit

import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Track
import kotlin.math.roundToLong

/**
 * One kept piece of a source clip, in source ms. [source] indexes [ReelEdit.sources] (0 = the first / only video).
 * [transition] plays INTO this clip from the previous one (ignored on the first clip).
 * [freezeMs] > 0 makes this a freeze frame: the frame at [startMs] held for [freezeMs] of output (endMs == startMs).
 */
data class ReelClip(
    val startMs: Long,
    val endMs: Long,
    val source: Int = 0,
    val transition: ReelTransition = ReelTransition.NONE,
    val freezeMs: Long = 0L
) {
    val lengthMs get() = (endMs - startMs).coerceAtLeast(0)
    val isFreeze get() = freezeMs > 0
}

/** Cut transitions — no overlap needed: the outgoing half and incoming half share the cut. */
enum class ReelTransition(val label: String, val durationMs: Long) {
    NONE("Cut", 0), FADE("Fade", 500), SLIDE("Slide", 400), ZOOM("Zoom", 450);

    /**
     * Look at [offsetMs] from the cut (negative = before). Returns null outside the transition window.
     * dim = black over the frame, dx = horizontal shift (fraction of width), scale = zoom.
     */
    fun look(offsetMs: Long): TransitionLook? {
        if (this == NONE) return null
        val half = durationMs / 2
        if (offsetMs < -half || offsetMs >= half) return null
        val p = (offsetMs + half).toFloat() / durationMs // 0..1 across the window
        val toCut = if (offsetMs < 0) p * 2f else (1f - p) * 2f // 0 at edges → 1 at the cut
        return when (this) {
            FADE -> TransitionLook(dim = toCut.coerceIn(0f, 1f))
            SLIDE -> TransitionLook(dx = if (offsetMs < 0) -toCut * 0.5f else toCut * 0.5f, dim = toCut * 0.35f)
            ZOOM -> TransitionLook(scale = 1f + 0.35f * toCut, dim = toCut * 0.5f)
            NONE -> null
        }
    }
}

data class TransitionLook(val dim: Float = 0f, val dx: Float = 0f, val scale: Float = 1f)

/** Speed ramp across the whole reel: piecewise speed multipliers over the kept (source) timeline fractions. */
enum class SpeedRamp(val label: String, val steps: List<Pair<Float, Float>>) {
    NONE("Off", listOf(1f to 1f)),
    MONTAGE("Montage", listOf(0.25f to 2f, 0.75f to 0.6f, 1f to 2f)),
    HERO("Hero", listOf(0.4f to 1f, 0.65f to 0.4f, 1f to 1f)),
    BULLET("Bullet", listOf(0.5f to 0.5f, 1f to 2f)),
    FLASH_IN("Flash in", listOf(0.3f to 2.5f, 1f to 1f)),
    FLASH_OUT("Flash out", listOf(0.7f to 1f, 1f to 2.5f));
}

/** Where a video file came from (multi-clip import). [uri] is a content:// or file:// string. */
data class ReelSource(val uri: String, val durationMs: Long, val label: String = "")

/** One constant-speed piece of the output (what the exporter / player actually plays). */
data class ReelSegment(
    val clip: Int,
    val source: Int,
    val srcStartMs: Long,
    val srcEndMs: Long,
    val speed: Float,
    val outStart: Double,
    val outEnd: Double,
    val freeze: Boolean
) {
    val outStartMs get() = outStart.roundToLong()
    val outEndMs get() = outEnd.roundToLong()
}

/** A recorded voiceover (WAV in app storage), placed on the output timeline. */
data class VoiceClip(val id: String, val path: String, val startMs: Long, val durationMs: Long, val volume: Float = 1f) {
    val endMs get() = startMs + durationMs
}

data class Caption(val id: String, val text: String, val startMs: Long, val endMs: Long)

enum class CaptionStyle(val label: String) { CLEAN("Clean"), BOXED("Boxed"), NOOR("Noor"), POP("Pop") }

object Captions {
    /** Split a typed / pasted transcript into short timed captions spread over [fromMs]..[toMs] by length. */
    fun fromTranscript(text: String, fromMs: Long, toMs: Long, newId: (Int) -> String = { "c$it" }, maxWords: Int = 6): List<Caption> {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty() || toMs <= fromMs) return emptyList()
        val chunks = mutableListOf<String>(); val cur = mutableListOf<String>()
        for (w in words) {
            cur += w
            if (cur.size >= maxWords || w.endsWith(".") || w.endsWith("?") || w.endsWith("!") || w.endsWith("،") || w.endsWith(",") && cur.size >= 3) { chunks += cur.joinToString(" "); cur.clear() }
        }
        if (cur.isNotEmpty()) chunks += cur.joinToString(" ")
        val weights = chunks.map { it.length + 6.0 }; val total = weights.sum(); val span = (toMs - fromMs).toDouble()
        var acc = fromMs.toDouble()
        return chunks.mapIndexed { i, c ->
            val st = acc; acc += span * weights[i] / total
            Caption(newId(i), c, st.roundToLong(), if (i == chunks.lastIndex) toMs else acc.roundToLong())
        }
    }

    /** Captions become ordinary timed text layers for drawing (preview + export share LayerPainter). */
    fun layer(c: Caption, style: CaptionStyle): EditorLayer {
        val base = EditorLayer.text("cap_${c.id}", c.text, EditorFont.MODERN).copy(y = 0.8f, scale = 0.82f, startMs = c.startMs, endMs = c.endMs)
        return when (style) {
            CaptionStyle.CLEAN -> base.copy(look = TextLook.SHADOW)
            CaptionStyle.BOXED -> base.copy(pill = TextPill.SOLID, pillColor = 0xCC000000, color = 0xFFFFFFFF)
            CaptionStyle.NOOR -> base.copy(pill = TextPill.SOLID, pillColor = 0xFF0B6243, color = 0xFFF3D98B, font = EditorFont.STRONG)
            CaptionStyle.POP -> base.copy(look = TextLook.OUTLINE, font = EditorFont.STRONG, color = 0xFFFFE27A, anim = TextAnim.POP, scale = 0.95f)
        }
    }
}

/** Export size / bitrate. HD is the 1.1.3 default (same 1280 cap as before). */
enum class ExportQuality(val label: String, val note: String, val longSide: Int, val bitrate: Int) {
    DATA_SAVER("Data saver", "960p · smaller upload", 960, 2_000_000),
    HD("720p", "Balanced (default)", 1280, 5_000_000),
    FHD("1080p", "Sharpest · bigger file", 1920, 10_000_000)
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
 * Everything the reel editor changes. Layer / caption / voice times are on the OUTPUT timeline (after
 * clips, speed, ramp and freezes), which is what the editor shows and what the exporter's overlay sees.
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
    val coverMs: Long = 0L,
    /** Extra sources for multi-clip reels; empty = single video of [sourceDurationMs]. */
    val sources: List<ReelSource> = emptyList(),
    val ramp: SpeedRamp = SpeedRamp.NONE,
    val soundFadeInMs: Long = 0L,
    val soundFadeOutMs: Long = 0L,
    val voices: List<VoiceClip> = emptyList(),
    val captions: List<Caption> = emptyList(),
    val captionStyle: CaptionStyle = CaptionStyle.BOXED,
    val quality: ExportQuality = ExportQuality.HD
) {
    val keptMs get() = clips.sumOf { it.lengthMs }
    val segments: List<ReelSegment> by lazy { buildSegments() }
    val outputMs: Long get() = (segments.lastOrNull()?.outEnd ?: 0.0).roundToLong()
    val muted get() = originalVolume <= 0.001f
    val matrix get() = colorMatrixFor(filter, strength, adjust)
    val trimStartMs get() = clips.firstOrNull()?.startMs ?: 0
    val trimEndMs get() = clips.lastOrNull()?.endMs ?: sourceDurationMs
    val sourceCount get() = maxOf(1, sources.size)
    val hasTransitions get() = clips.drop(1).any { it.transition != ReelTransition.NONE }
    val hasFreeze get() = clips.any { it.isFreeze }
    fun durationOf(source: Int): Long = sources.getOrNull(source)?.durationMs ?: sourceDurationMs

    /** Layers + captions (captions drawn with the chosen style), in draw order. */
    fun drawLayers(): List<EditorLayer> = layers + captions.map { Captions.layer(it, captionStyle) }

    private fun buildSegments(): List<ReelSegment> {
        val total = keptMs.toDouble().coerceAtLeast(1.0)
        val bounds = ramp.steps.map { it.first * total to it.second }
        val out = mutableListOf<ReelSegment>()
        var kept = 0.0; var t = 0.0
        clips.forEachIndexed { i, c ->
            if (c.isFreeze) {
                out += ReelSegment(i, c.source, c.startMs, c.startMs, 0f, t, t + c.freezeMs, true); t += c.freezeMs; return@forEachIndexed
            }
            var pos = c.startMs.toDouble(); val end = c.endMs.toDouble()
            while (pos < end - 1e-6) {
                val k = kept + (pos - c.startMs)
                val stepIdx = bounds.indexOfFirst { k < it.first - 1e-6 }.let { if (it < 0) bounds.lastIndex else it }
                val stepEndKept = bounds[stepIdx].first
                val pieceEnd = minOf(end, c.startMs + (stepEndKept - kept)).let { if (it <= pos + 1e-6) end else it }
                val f = speed.factor * bounds[stepIdx].second
                val dur = (pieceEnd - pos) / f
                out += ReelSegment(i, c.source, pos.roundToLong(), pieceEnd.roundToLong(), f, t, t + dur, false)
                t += dur; pos = pieceEnd
            }
            kept += c.lengthMs
        }
        return out
    }

    /** Single-range trim (keeps the existing slider working; single-source reels). */
    fun trim(startMs: Long, endMs: Long): ReelEdit {
        val s = startMs.coerceIn(0, sourceDurationMs); val e = endMs.coerceIn(s + MIN_CLIP_MS, sourceDurationMs.coerceAtLeast(s + MIN_CLIP_MS))
        return copy(clips = listOf(ReelClip(s, e))).clampLayers()
    }

    /** Trim one clip of a multi-clip reel (in that clip's source ms). */
    fun trimClip(i: Int, startMs: Long, endMs: Long): ReelEdit {
        val c = clips.getOrNull(i) ?: return this
        if (c.isFreeze) return this
        val dur = durationOf(c.source)
        val s = startMs.coerceIn(0, (dur - MIN_CLIP_MS).coerceAtLeast(0)); val e = endMs.coerceIn(s + MIN_CLIP_MS, dur.coerceAtLeast(s + MIN_CLIP_MS))
        return copy(clips = clips.mapIndexed { k, x -> if (k == i) x.copy(startMs = s, endMs = e) else x }).clampLayers()
    }

    /** Split at an output-timeline position into two clips. No-op if either side would be < MIN_CLIP_MS. */
    fun splitAt(atMs: Long): ReelEdit {
        val (i, at) = sourceAt(atMs) ?: return this
        val c = clips[i]
        if (c.isFreeze || at - c.startMs < MIN_CLIP_MS || c.endMs - at < MIN_CLIP_MS) return this
        val next = clips.toMutableList().apply { removeAt(i); add(i, c.copy(startMs = at, transition = ReelTransition.NONE)); add(i, c.copy(endMs = at)) }
        return copy(clips = next)
    }

    /** Hold the frame under the playhead for [holdMs] (splits the clip there when possible). */
    fun freezeAt(atMs: Long, holdMs: Long = 1500L): ReelEdit {
        val (i, at) = sourceAt(atMs) ?: return this
        val c = clips[i]; if (c.isFreeze) return this
        val hold = holdMs.coerceIn(500L, 5000L)
        val fr = ReelClip(at, at, c.source, ReelTransition.NONE, hold)
        val next = clips.toMutableList()
        when {
            at - c.startMs >= MIN_CLIP_MS && c.endMs - at >= MIN_CLIP_MS -> { next.removeAt(i); next.addAll(i, listOf(c.copy(endMs = at), fr, c.copy(startMs = at, transition = ReelTransition.NONE))) }
            at - c.startMs < MIN_CLIP_MS -> next.add(i, fr.copy(startMs = c.startMs, endMs = c.startMs))
            else -> next.add(i + 1, fr.copy(startMs = c.endMs - 40, endMs = c.endMs - 40))
        }
        return remapTimes(copy(clips = next))
    }

    /** Remove a clip (never the last real one). */
    fun deleteClip(i: Int): ReelEdit {
        if (i !in clips.indices) return this
        if (!clips[i].isFreeze && clips.count { !it.isFreeze } <= 1) return this
        return copy(clips = firstCut(clips.filterIndexed { k, _ -> k != i })).clampLayers()
    }

    private fun firstCut(l: List<ReelClip>) = l.mapIndexed { k, c -> if (k == 0 && c.transition != ReelTransition.NONE) c.copy(transition = ReelTransition.NONE) else c }

    /** Reorder clips (drag in the clip strip). The first clip never keeps a transition. */
    fun moveClip(from: Int, to: Int): ReelEdit {
        if (from !in clips.indices) return this
        val t = to.coerceIn(0, clips.lastIndex); if (from == t) return this
        return copy(clips = firstCut(clips.toMutableList().apply { add(t, removeAt(from)) })).clampLayers()
    }

    fun withTransition(i: Int, tr: ReelTransition) = if (i !in clips.indices || i == 0) this else copy(clips = clips.mapIndexed { k, c -> if (k == i) c.copy(transition = tr) else c })
    fun allTransitions(tr: ReelTransition) = copy(clips = clips.mapIndexed { k, c -> if (k == 0) c else c.copy(transition = tr) })

    /** Append another video (gallery pick or recording) as a new source + clip. */
    fun addSource(src: ReelSource): ReelEdit {
        val base = if (sources.isEmpty()) listOf(ReelSource("", sourceDurationMs)) else sources
        val idx = base.size
        return copy(sources = base + src, clips = clips + ReelClip(0, src.durationMs.coerceAtLeast(MIN_CLIP_MS), idx, if (clips.size > 0) ReelTransition.FADE else ReelTransition.NONE)).clampLayers()
    }

    fun withSpeed(s: ReelSpeed): ReelEdit = remapTimes(copy(speed = s))
    fun withRamp(r: SpeedRamp): ReelEdit = remapTimes(copy(ramp = r))

    /** Keep layers / captions / voices at the same source moment when the timing of [next] differs. */
    private fun remapTimes(next: ReelEdit): ReelEdit {
        fun m(t: Long): Long = if (t == Long.MAX_VALUE) t else next.outputAtKept(keptAt(t))
        return next.copy(
            layers = layers.map { l -> if (l.startMs == 0L && l.endMs == Long.MAX_VALUE) l else l.copy(startMs = m(l.startMs), endMs = m(l.endMs)) },
            captions = captions.map { it.copy(startMs = m(it.startMs), endMs = m(it.endMs)) },
            voices = voices.map { it.copy(startMs = m(it.startMs)) }
        ).clampLayers()
    }

    /** Output ms → position on the kept (source) timeline (freezes collapse to their frame). */
    fun keptAt(atMs: Long): Double {
        var kept = 0.0
        for (sg in segments) {
            if (atMs < sg.outEnd || sg === segments.last()) {
                if (sg.freeze) return kept
                val off = ((atMs - sg.outStart).coerceIn(0.0, sg.outEnd - sg.outStart)) * sg.speed
                return kept + off
            }
            kept += sg.srcEndMs - sg.srcStartMs
        }
        return kept
    }

    fun outputAtKept(k: Double): Long {
        var kept = 0.0
        for (sg in segments) {
            if (sg.freeze) continue
            val len = (sg.srcEndMs - sg.srcStartMs).toDouble()
            if (k <= kept + len) return (sg.outStart + (k - kept) / sg.speed).roundToLong()
            kept += len
        }
        return outputMs
    }

    /** Output ms → (clip index, source ms). */
    fun sourceAt(atMs: Long): Pair<Int, Long>? {
        val segs = segments; if (segs.isEmpty()) return null
        val at = atMs.coerceIn(0, outputMs).toDouble()
        val sg = segs.firstOrNull { at < it.outEnd } ?: segs.last()
        if (sg.freeze) return sg.clip to sg.srcStartMs
        return sg.clip to (sg.srcStartMs + (at - sg.outStart) * sg.speed).roundToLong().coerceAtMost(sg.srcEndMs)
    }

    /** Segment at an output time (for the live preview). */
    fun segmentAt(atMs: Long): ReelSegment? = segments.firstOrNull { atMs < it.outEnd } ?: segments.lastOrNull()

    /** Clip boundaries on the output timeline (split markers + transitions). */
    fun cutsOnOutput(): List<Long> = clips.indices.drop(1).mapNotNull { i -> segments.firstOrNull { it.clip == i }?.outStartMs }

    /** Transition look at an output time (null when not inside one). */
    fun transitionAt(atMs: Long): TransitionLook? {
        clips.indices.drop(1).forEach { i ->
            val tr = clips[i].transition; if (tr == ReelTransition.NONE) return@forEach
            val cut = segments.firstOrNull { it.clip == i }?.outStartMs ?: return@forEach
            tr.look(atMs - cut)?.let { return it }
        }
        return null
    }

    fun upsert(l: EditorLayer) = copy(layers = Layers.upsert(layers, l))
    fun remove(id: String) = copy(layers = layers.filterNot { it.id == id })
    fun upsertCaption(c: Caption) = copy(captions = (captions.filterNot { it.id == c.id } + c).sortedBy { it.startMs })
    fun removeCaption(id: String) = copy(captions = captions.filterNot { it.id == id })
    fun upsertVoice(v: VoiceClip) = copy(voices = voices.filterNot { it.id == v.id } + v)
    fun removeVoice(id: String) = copy(voices = voices.filterNot { it.id == id })

    private fun clampLayers(): ReelEdit {
        val total = outputMs
        return copy(
            layers = layers.map { l -> if (l.startMs == 0L && l.endMs == Long.MAX_VALUE) l else l.timed(l.startMs, minOf(l.endMs, total), total) },
            captions = captions.filter { it.startMs < total }.map { it.copy(endMs = minOf(it.endMs, total)) },
            voices = voices.filter { it.startMs < total }
        )
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
    val expectedDurationMs: Long,
    val segments: List<ReelSegment> = emptyList(),
    /** Source uris by index ("" = the editor's primary uri). */
    val sourceUris: List<String> = emptyList(),
    /** (cut on output ms, transition) for every non-NONE transition. */
    val transitions: List<Pair<Long, ReelTransition>> = emptyList(),
    val soundFadeInMs: Long = 0L,
    val soundFadeOutMs: Long = 0L,
    val voices: List<VoiceClip> = emptyList(),
    val bitrate: Int = ExportQuality.HD.bitrate
) {
    val hasOverlay get() = layers.isNotEmpty() || vignette > 0f
    /** Animated layers (entrance animations) need a per-frame bitmap instead of a static one. */
    val hasAnimatedLayers get() = layers.any { it.anim != TextAnim.NONE }
    val needsVideoEffects get() = colorMatrixGl != null || hasOverlay || speed != 1f || transitions.isNotEmpty()
    val multiSource get() = segments.map { it.source }.distinct().size > 1 || segments.any { it.freeze }

    companion object {
        const val MAX_LONG_SIDE = 1280

        /** Output size: keep aspect, longest side ≤ [longSide] (default 1280), even dimensions (H.264). */
        fun outputSize(srcW: Int, srcH: Int, longSide: Int = MAX_LONG_SIDE): Pair<Int, Int> {
            if (srcW <= 0 || srcH <= 0) return if (longSide >= 1280) 720 to 1280 else (longSide * 9 / 16 / 2 * 2) to longSide
            val k = minOf(1f, longSide.toFloat() / maxOf(srcW, srcH))
            fun even(v: Float) = (v.toInt() / 2 * 2).coerceAtLeast(2)
            return even(srcW * k) to even(srcH * k)
        }

        fun from(e: ReelEdit, srcW: Int, srcH: Int): ReelExportParams {
            val (w, h) = outputSize(srcW, srcH, e.quality.longSide)
            val m = e.matrix
            return ReelExportParams(
                clips = e.clips, speed = e.speed.factor,
                removeAudio = e.muted, originalVolume = e.originalVolume.coerceIn(0f, 1f),
                soundUrl = e.sound?.url, soundVolume = e.soundVolume.coerceIn(0f, 1f),
                colorMatrixGl = if (m.isIdentity()) null else m.toGl(), vignette = e.adjust.vignette,
                layers = e.drawLayers(), outputWidth = w, outputHeight = h, expectedDurationMs = e.outputMs,
                segments = e.segments,
                sourceUris = if (e.sources.isEmpty()) listOf("") else e.sources.map { it.uri },
                transitions = e.clips.indices.drop(1).filter { e.clips[it].transition != ReelTransition.NONE }
                    .mapNotNull { i -> e.segments.firstOrNull { it.clip == i }?.let { it.outStartMs to e.clips[i].transition } },
                soundFadeInMs = if (e.sound != null) e.soundFadeInMs else 0, soundFadeOutMs = if (e.sound != null) e.soundFadeOutMs else 0,
                voices = e.voices.map { it.copy(volume = it.volume.coerceIn(0f, 1.5f)) }, bitrate = e.quality.bitrate
            )
        }

        /** Only a plain single trim (or nothing) → the existing 1.1.1 trim path is enough. */
        fun needsFullExport(e: ReelEdit): Boolean =
            e.clips.size > 1 || e.speed != ReelSpeed.NORMAL || !e.matrix.isIdentity() || e.adjust.vignette > 0f ||
                e.layers.isNotEmpty() || e.originalVolume < 0.999f || e.sound != null ||
                e.ramp != SpeedRamp.NONE || e.sources.size > 1 || e.hasFreeze || e.voices.isNotEmpty() || e.captions.isNotEmpty() ||
                e.quality != ExportQuality.HD
    }
}

/** Gain for music fade in / out at an output position (pure; used by the exporter's audio processor and the preview). */
object AudioFades {
    fun gain(posMs: Long, totalMs: Long, fadeInMs: Long, fadeOutMs: Long): Float {
        var g = 1f
        if (fadeInMs > 0 && posMs < fadeInMs) g = minOf(g, posMs.coerceAtLeast(0).toFloat() / fadeInMs)
        if (fadeOutMs > 0 && totalMs > 0 && posMs > totalMs - fadeOutMs) g = minOf(g, ((totalMs - posMs).coerceAtLeast(0)).toFloat() / fadeOutMs)
        return g.coerceIn(0f, 1f)
    }
}

/** Voiceover mixing in 16-bit mono PCM (pure; WAV I/O lives in VoiceOver). */
object VoiceMix {
    const val RATE = 44_100

    /** Lays every take at its start time (ms on the output timeline), sums with clipping, trims to [totalMs]. */
    fun mix(takes: List<Pair<VoiceClip, ShortArray>>, totalMs: Long, rate: Int = RATE): ShortArray {
        if (takes.isEmpty()) return ShortArray(0)
        val end = minOf(totalMs, takes.maxOf { (v, pcm) -> v.startMs + pcm.size * 1000L / rate })
        val out = IntArray((end * rate / 1000).toInt().coerceAtLeast(0))
        for ((v, pcm) in takes) {
            val off = (v.startMs * rate / 1000).toInt()
            for (i in pcm.indices) { val j = off + i; if (j >= out.size) break; if (j >= 0) out[j] += (pcm[i] * v.volume).toInt() }
        }
        return ShortArray(out.size) { out[it].coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() }
    }

    /** Peak levels (0..1) in [bins] buckets for the voice lane waveform. */
    fun peaks(pcm: ShortArray, bins: Int): FloatArray {
        if (pcm.isEmpty() || bins <= 0) return FloatArray(bins)
        val per = maxOf(1, pcm.size / bins)
        return FloatArray(bins) { b -> var m = 0; val s = b * per; for (i in s until minOf(pcm.size, s + per)) m = maxOf(m, kotlin.math.abs(pcm[i].toInt())); m / 32768f }
    }
}
