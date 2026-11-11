package com.noorpro.app.data.media.edit

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.VideoEncoderSettings
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import java.io.File
import kotlin.math.max

/** Photo side: decode (EXIF-aware), rotate, crop, colour, vignette, brush, layers → JPEG in cache. */
object PhotoRenderer {
    const val MAX_SIDE = 2048

    fun decode(ctx: Context, uri: Uri, maxSide: Int = MAX_SIDE): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= 28) {
            val src = android.graphics.ImageDecoder.createSource(ctx.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(src) { d, info, _ ->
                val s = max(info.size.width, info.size.height)
                if (s > maxSide) { val k = maxSide.toFloat() / s; d.setTargetSize((info.size.width * k).toInt().coerceAtLeast(1), (info.size.height * k).toInt().coerceAtLeast(1)) }
                d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                d.isMutableRequired = false
            }
        } else {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            var sample = 1; while (max(o.outWidth, o.outHeight) / sample > maxSide) sample *= 2
            val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
            val deg = ctx.contentResolver.openInputStream(uri)?.use { exifDegrees(it) } ?: 0
            if (deg == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(deg.toFloat()) }, true)
        }
    }.getOrNull()

    private fun exifDegrees(input: java.io.InputStream): Int = runCatching {
        when (android.media.ExifInterface(input).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
            6 -> 90; 3 -> 180; 8 -> 270; else -> 0
        }
    }.getOrDefault(0)

    /** Apply [e] to [src]; returns a new bitmap sized to the crop. */
    fun render(src: Bitmap, e: PhotoEdit, fonts: EditorTypefaces): Bitmap {
        val rotated = if (e.quarterTurns == 0) src else Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(90f * e.quarterTurns) }, true)
        val cl = (e.crop.left * rotated.width).toInt().coerceIn(0, rotated.width - 1)
        val ct = (e.crop.top * rotated.height).toInt().coerceIn(0, rotated.height - 1)
        val cw = (e.crop.width * rotated.width).toInt().coerceIn(1, rotated.width - cl)
        val ch = (e.crop.height * rotated.height).toInt().coerceIn(1, rotated.height - ct)
        var base = Bitmap.createBitmap(rotated, cl, ct, cw, ch)
        if (e.needsPixels) base = pixels(base, e)
        val out = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            val m = e.matrix; if (!m.isIdentity()) colorFilter = ColorMatrixColorFilter(m.toAndroid())
        }
        c.drawBitmap(base, null, Rect(0, 0, cw, ch), paint)
        if (e.hasBlur) BlurTools.drawBlurred(c, BlurTools.blur(out, e.blurAmount), null, RectF(0f, 0f, cw.toFloat(), ch.toFloat()), BlurTools.mask(cw, ch, e.tiltShift, e.blurStrokes))
        LayerPainter.drawVignette(c, cw, ch, e.adjust.vignette)
        LayerPainter.drawStrokes(c, e.strokes, cw, ch)
        LayerPainter.drawAll(c, e.layers, cw, ch, fonts)
        return out
    }

    /** Tone curve / HSL / highlights / shadows / sharpen / grain on a copy of [b]. */
    fun pixels(b: Bitmap, e: PhotoEdit): Bitmap {
        val w = b.width; val h = b.height
        val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h)
        ProPixels.process(px, w, h, e.adjust, e.curve, e.hsl)
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, w, 0, 0, w, h) }
    }

    /** Auto-enhance suggestion for a bitmap (samples ~40k pixels). */
    fun autoEnhance(b: Bitmap, base: Adjustments): Adjustments {
        val w = b.width; val h = b.height; val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h)
        return AutoEnhance.suggest(AutoEnhance.stats(px, maxOf(1, px.size / 40_000)), base)
    }

    /** Collage of [photos] → one bitmap (the edited photos are already rendered). */
    fun collage(photos: List<Bitmap>, e: CollageEdit, longSide: Int = MAX_SIDE): Bitmap {
        val (w, h) = e.outputSize(longSide)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(out)
        c.drawColor(e.background.toInt())
        val r = e.radius * minOf(w, h)
        e.rects(w, h).forEachIndexed { i, cell ->
            if (photos.isEmpty()) return@forEachIndexed
            val b = photos[e.photoFor(i, photos.size)]
            val dst = RectF(cell[0], cell[1], cell[2], cell[3])
            val s = maxOf(dst.width() / b.width, dst.height() / b.height)
            val sw = dst.width() / s; val sh = dst.height() / s
            val src = Rect(((b.width - sw) / 2).toInt(), ((b.height - sh) / 2).toInt(), ((b.width + sw) / 2).toInt(), ((b.height + sh) / 2).toInt())
            c.save(); c.clipPath(Path().apply { addRoundRect(dst, r, r, Path.Direction.CW) })
            c.drawBitmap(b, src, dst, Paint(Paint.FILTER_BITMAP_FLAG)); c.restore()
        }
        return out
    }

    fun renderToFile(ctx: Context, uri: Uri, e: PhotoEdit): File? = runCatching {
        val src = decode(ctx, uri) ?: return null
        val bmp = render(src, e, EditorTypefaces.load(ctx))
        val f = File(ctx.cacheDir, "noor_edit_${System.currentTimeMillis()}.jpg")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        f
    }.getOrNull()
}

/** Blur brush + tilt-shift: a cheap down/up-scaled blur composited through a soft mask. */
object BlurTools {
    fun blur(src: Bitmap, amount: Float): Bitmap {
        val k = (8f + amount.coerceIn(0f, 1f) * 16f)
        val sw = (src.width / k).toInt().coerceAtLeast(2); val sh = (src.height / k).toInt().coerceAtLeast(2)
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val mid = Bitmap.createScaledBitmap(small, (sw * 2).coerceAtMost(src.width), (sh * 2).coerceAtMost(src.height), true)
        return Bitmap.createScaledBitmap(mid, src.width, src.height, true)
    }

    /** White = blurred. Tilt-shift band + brushed strokes (soft edges). */
    fun mask(w: Int, h: Int, tilt: TiltShift?, strokes: List<BrushStroke>): Bitmap {
        val m = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888); val c = Canvas(m)
        tilt?.let { t ->
            val cy = t.centerY * h; val band = t.band * h; val f = t.feather * h
            c.save(); c.rotate(t.angleDeg, w / 2f, cy)
            val p = Paint().apply {
                shader = LinearGradient(0f, cy - band - f - h, 0f, cy + band + f + h,
                    intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE, Color.WHITE),
                    floatArrayOf(0f, (h.toFloat()) / (2 * (band + f + h)), (h + f) / (2 * (band + f + h)), (h + f + 2 * band) / (2 * (band + f + h)), (h + 2 * f + 2 * band) / (2 * (band + f + h)), 1f),
                    Shader.TileMode.CLAMP)
            }
            c.drawRect(-w.toFloat(), cy - band - f - h, 2f * w, cy + band + f + h, p)
            c.restore()
        }
        val u = minOf(w, h).toFloat()
        strokes.forEach { s ->
            if (s.points.isEmpty()) return@forEach
            val path = Path(); s.points.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x * w, y * h) else path.lineTo(x * w, y * h) }
            if (s.points.size == 1) path.lineTo(s.points[0].first * w + 0.1f, s.points[0].second * h)
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = s.width * u; color = Color.WHITE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                maskFilter = BlurMaskFilter((s.width * u * 0.3f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            })
        }
        return m
    }

    /** Draw [blurred] into [dst] only where [mask] is set. */
    fun drawBlurred(c: Canvas, blurred: Bitmap, src: Rect?, dst: RectF, mask: Bitmap, paint: Paint? = null) {
        val layer = c.saveLayer(dst, null)
        c.drawBitmap(blurred, src, dst, Paint(paint ?: Paint()).apply { isFilterBitmap = true })
        c.drawBitmap(mask, null, dst, Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
        c.restoreToCount(layer)
    }
}

/** Reel side: media3 Transformer (already in the app) — clips, speed, colour, overlays, volume, licensed sound. */
@OptIn(UnstableApi::class)
object ReelExporter {

    /** Video size after rotation metadata, for the overlay canvas. */
    fun videoSize(ctx: Context, uri: Uri): Pair<Int, Int> = runCatching {
        val r = android.media.MediaMetadataRetriever(); r.setDataSource(ctx, uri)
        val w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rot = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        r.release(); if (rot == 90 || rot == 270) h to w else w to h
    }.getOrDefault(0 to 0)

    private class Speed(val f: Float) : SpeedProvider {
        override fun getSpeed(timeUs: Long) = f
        override fun getNextSpeedChangeTimeUs(timeUs: Long) = C.TIME_UNSET
    }

    private fun volume(v: Float): AudioProcessor = ChannelMixingAudioProcessor().apply {
        putChannelMixingMatrix(ChannelMixingMatrix.create(1, 1).scaleBy(v))
        putChannelMixingMatrix(ChannelMixingMatrix.create(2, 2).scaleBy(v))
    }

    /** Full-frame bitmap overlay shown only inside a time window (output timeline). */
    private class TimedOverlay(private val bmp: Bitmap, private val startMs: Long, private val endMs: Long) : BitmapOverlay() {
        private val on = OverlaySettings.Builder().build()
        private val off = OverlaySettings.Builder().setAlphaScale(0f).build()
        override fun getBitmap(presentationTimeUs: Long) = bmp
        override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
            val ms = presentationTimeUs / 1000
            return if (ms >= startMs && ms < endMs) on else off
        }
    }

    /**
     * All text / sticker / caption layers in ONE frame-sized bitmap that is redrawn only when what is visible
     * changes (a layer appears / leaves, or an entrance animation advances). BitmapOverlay re-uploads on a new
     * generation id, so static stretches cost nothing.
     */
    class LiveLayersOverlay(private val layers: List<EditorLayer>, private val w: Int, private val h: Int, private val fonts: EditorTypefaces) : BitmapOverlay() {
        private val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        private val canvas = Canvas(bmp)
        private var lastKey: String? = null
        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            val ms = presentationTimeUs / 1000
            val key = frameKey(layers, ms)
            if (key != lastKey) {
                bmp.eraseColor(Color.TRANSPARENT)
                LayerPainter.drawAll(canvas, layers, w, h, fonts, atMs = ms)
                lastKey = key
            }
            return bmp
        }
        companion object {
            /** Identity of a frame's overlay: visible ids + quantised animation state. */
            fun frameKey(layers: List<EditorLayer>, ms: Long): String = buildString {
                layers.forEach { l ->
                    if (!l.visibleAt(ms)) return@forEach
                    append(l.id)
                    val f = l.animAt(ms)
                    if (f != AnimFrame.DONE) append('@').append((f.alpha * 40).toInt()).append(',').append((f.scale * 40).toInt()).append(',').append((f.dy * 400).toInt()).append(',').append(f.chars ?: -1)
                    append('|')
                }
            }
        }
    }

    /** Music fade in / out on 16-bit PCM, counted on the output timeline (not reset on flush). */
    private class FadeProcessor(private val totalMs: Long, private val inMs: Long, private val outMs: Long) : BaseAudioProcessor() {
        private var frames = 0L
        override fun onConfigure(f: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (f.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(f)
            return f
        }
        override fun queueInput(input: java.nio.ByteBuffer) {
            val fmt = inputAudioFormat; val ch = fmt.channelCount.coerceAtLeast(1)
            val out = replaceOutputBuffer(input.remaining()).order(java.nio.ByteOrder.nativeOrder())
            input.order(java.nio.ByteOrder.nativeOrder())
            while (input.remaining() >= 2 * ch) {
                val g = AudioFades.gain(frames * 1000 / fmt.sampleRate.coerceAtLeast(1), totalMs, inMs, outMs)
                repeat(ch) { out.putShort((input.short * g).toInt().toShort()) }
                frames++
            }
            input.position(input.limit()); out.flip()
        }
        override fun onReset() { frames = 0 }
    }

    /** Frame at a source position, letterboxed to the output size (freeze-frame overlay). */
    private fun frameBitmap(ctx: Context, uri: Uri, atMs: Long, w: Int, h: Int): Bitmap? = runCatching {
        val r = android.media.MediaMetadataRetriever(); r.setDataSource(ctx, uri)
        val f = r.getFrameAtTime(atMs * 1000, android.media.MediaMetadataRetriever.OPTION_CLOSEST); r.release()
        f ?: return null
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(out); c.drawColor(Color.BLACK)
        val s = minOf(w.toFloat() / f.width, h.toFloat() / f.height)
        val dw = f.width * s; val dh = f.height * s
        c.drawBitmap(f, null, RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG)); out
    }.getOrNull()

    /** Mixed voiceover takes → one WAV on the output timeline (null when there are none). */
    fun voiceMixFile(ctx: Context, voices: List<VoiceClip>, totalMs: Long): File? {
        val takes = voices.mapNotNull { v -> File(v.path).takeIf { it.exists() }?.let { v to Wav.read(it) } }
        if (takes.isEmpty()) return null
        val pcm = VoiceMix.mix(takes, totalMs)
        return File(ctx.cacheDir, "reel_voice_mix_${System.currentTimeMillis()}.wav").also { Wav.write(it, pcm) }
    }

    fun overlays(p: ReelExportParams, fonts: EditorTypefaces): List<TextureOverlay> {
        val w = p.outputWidth; val h = p.outputHeight
        val list = mutableListOf<TextureOverlay>()
        if (p.layers.isNotEmpty()) list += LiveLayersOverlay(p.layers, w, h, fonts)
        if (p.vignette > 0f) {
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); LayerPainter.drawVignette(Canvas(b), w, h, p.vignette)
            list += TimedOverlay(b, 0, Long.MAX_VALUE)
        }
        return list
    }

    /** Transition look on the output timeline from the export params (pure mirror of ReelEdit.transitionAt). */
    private fun lookAt(p: ReelExportParams, ms: Long): TransitionLook? {
        p.transitions.forEach { (cut, tr) -> tr.look(ms - cut)?.let { return it } }
        return null
    }

    fun composition(ctx: Context, uri: Uri, p: ReelExportParams): Composition {
        val segs = p.segments.ifEmpty {
            // 1.1.3 shape (tests / older callers): every clip at one speed from the primary source.
            var t = 0.0
            p.clips.mapIndexed { i, c -> val d = c.lengthMs / p.speed.toDouble(); ReelSegment(i, 0, c.startMs, c.endMs, p.speed, t, t + d, false).also { t += d } }
        }
        fun uriOf(src: Int): Uri = p.sourceUris.getOrNull(src)?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: uri
        val freezeOverlays = mutableListOf<TextureOverlay>()
        val items = segs.map { sg ->
            val start = sg.srcStartMs
            val end = if (sg.freeze) sg.srcStartMs + FREEZE_SOURCE_MS else sg.srcEndMs
            val speed = if (sg.freeze) (FREEZE_SOURCE_MS.toFloat() / (sg.outEnd - sg.outStart).toFloat().coerceAtLeast(1f)) else sg.speed
            val item = MediaItem.Builder().setUri(uriOf(sg.source)).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(start).setEndPositionMs(end).build()
            ).build()
            val audio = mutableListOf<AudioProcessor>(); val video = mutableListOf<androidx.media3.common.Effect>()
            if (speed != 1f) Effects.createExperimentalSpeedChangingEffect(Speed(speed)).let { audio += it.first; video += it.second }
            val vol = if (sg.freeze) 0f else p.originalVolume
            if (!p.removeAudio && vol < 0.999f) audio += volume(vol)
            video += Presentation.createForWidthAndHeight(p.outputWidth, p.outputHeight, Presentation.LAYOUT_SCALE_TO_FIT)
            if (sg.freeze) frameBitmap(ctx, uriOf(sg.source), sg.srcStartMs, p.outputWidth, p.outputHeight)?.let { freezeOverlays += TimedOverlay(it, sg.outStartMs, sg.outEndMs) }
            EditedMediaItem.Builder(item).setRemoveAudio(p.removeAudio).setEffects(Effects(audio, video)).build()
        }
        val sequences = mutableListOf(EditedMediaItemSequence(items))
        p.soundUrl?.let { url ->
            val ap = mutableListOf<AudioProcessor>(volume(p.soundVolume))
            if (p.soundFadeInMs > 0 || p.soundFadeOutMs > 0) ap += FadeProcessor(p.expectedDurationMs, p.soundFadeInMs, p.soundFadeOutMs)
            val sound = EditedMediaItem.Builder(MediaItem.fromUri(url)).setRemoveVideo(true).setEffects(Effects(ap, emptyList())).build()
            sequences += EditedMediaItemSequence(listOf(sound), /* isLooping = */ true)
        }
        voiceMixFile(ctx, p.voices, p.expectedDurationMs)?.let { f ->
            sequences += EditedMediaItemSequence(listOf(EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(f))).setRemoveVideo(true).build()))
        }
        val fx = mutableListOf<androidx.media3.common.Effect>(
            Presentation.createForWidthAndHeight(p.outputWidth, p.outputHeight, Presentation.LAYOUT_SCALE_TO_FIT)
        )
        if (freezeOverlays.isNotEmpty()) fx += OverlayEffect(ImmutableList.copyOf(freezeOverlays))
        val base = p.colorMatrixGl
        if (base != null || p.transitions.isNotEmpty()) fx += RgbMatrix { t, _ ->
            val dim = lookAt(p, t / 1000)?.dim ?: 0f
            val m = base ?: IDENTITY_GL
            if (dim <= 0f) m else FloatArray(16) { i -> if (i % 4 != 3 && i < 12 || i in 12..14) m[i] * (1f - dim) else m[i] }
        }
        if (p.transitions.any { it.second == ReelTransition.SLIDE || it.second == ReelTransition.ZOOM }) fx += MatrixTransformation { t ->
            val l = lookAt(p, t / 1000)
            Matrix().apply { if (l != null) { postScale(l.scale, l.scale); postTranslate(l.dx * 2f, 0f) } }
        }
        val ov = overlays(p, EditorTypefaces.load(ctx))
        if (ov.isNotEmpty()) fx += OverlayEffect(ImmutableList.copyOf(ov))
        val multi = segs.map { it.source }.distinct().size > 1 || segs.any { it.freeze }
        return Composition.Builder(sequences)
            .setEffects(Effects(emptyList(), fx))
            .experimentalSetForceAudioTrack((p.removeAudio && (p.soundUrl != null || p.voices.isNotEmpty())) || multi)
            .build()
    }

    private const val FREEZE_SOURCE_MS = 120L
    private val IDENTITY_GL = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)

    /** Animated story: one photo (already rendered without the animated layers) + layers with entrance animations → MP4. */
    fun storyMotion(ctx: Context, photo: File, layers: List<EditorLayer>, w: Int, h: Int, durationMs: Long = 6_000L): Composition {
        val img = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(photo)))
            .setDurationUs(durationMs * 1000).setFrameRate(30)
            .setEffects(Effects(emptyList(), listOf(Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT)))).build()
        val fx = listOf<androidx.media3.common.Effect>(OverlayEffect(ImmutableList.of<TextureOverlay>(LiveLayersOverlay(layers, w, h, EditorTypefaces.load(ctx)))))
        return Composition.Builder(listOf(EditedMediaItemSequence(listOf(img)))).setEffects(Effects(emptyList(), fx)).build()
    }

    /** Export to an H.264/AAC MP4 in cache. Progress 0..1 and result on the main thread. */
    fun export(ctx: Context, uri: Uri, p: ReelExportParams, onProgress: (Float) -> Unit, onDone: (File?) -> Unit): () -> Unit =
        run(ctx, { composition(ctx, uri, p) }, p.bitrate, "reel_edit", onProgress, onDone)

    /** Export an animated story photo to MP4 (see [storyMotion]). */
    fun exportStoryMotion(ctx: Context, photo: File, layers: List<EditorLayer>, w: Int, h: Int, onProgress: (Float) -> Unit, onDone: (File?) -> Unit): () -> Unit {
        val (ow, oh) = ReelExportParams.outputSize(w, h)
        return run(ctx, { storyMotion(ctx, photo, layers, ow, oh) }, ExportQuality.HD.bitrate, "story_motion", onProgress, onDone)
    }

    private fun run(ctx: Context, build: () -> Composition, bitrate: Int, prefix: String, onProgress: (Float) -> Unit, onDone: (File?) -> Unit): () -> Unit {
        val main = Handler(Looper.getMainLooper())
        val out = File(ctx.cacheDir, "${prefix}_${System.currentTimeMillis()}.mp4")
        var transformer: Transformer? = null
        var finished = false
        main.post {
            runCatching {
                val t = Transformer.Builder(ctx.applicationContext)
                    .setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(DefaultEncoderFactory.Builder(ctx.applicationContext)
                        .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build()).build())
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(c: Composition, r: ExportResult) { finished = true; onProgress(1f); onDone(out) }
                        override fun onError(c: Composition, r: ExportResult, e: ExportException) { finished = true; out.delete(); onDone(null) }
                    }).build()
                transformer = t
                t.start(build(), out.absolutePath)
                val holder = ProgressHolder()
                main.post(object : Runnable {
                    override fun run() {
                        if (finished) return
                        if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress / 100f)
                        main.postDelayed(this, 250)
                    }
                })
            }.onFailure { finished = true; onDone(null) }
        }
        return { main.post { if (!finished) { finished = true; runCatching { transformer?.cancel() }; out.delete(); onDone(null) } } }
    }
}
