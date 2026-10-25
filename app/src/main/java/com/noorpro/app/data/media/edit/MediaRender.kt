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
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
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
        val out = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            val m = e.matrix; if (!m.isIdentity()) colorFilter = ColorMatrixColorFilter(m.toAndroid())
        }
        c.drawBitmap(rotated, Rect(cl, ct, cl + cw, ct + ch), Rect(0, 0, cw, ch), paint)
        LayerPainter.drawVignette(c, cw, ch, e.adjust.vignette)
        LayerPainter.drawStrokes(c, e.strokes, cw, ch)
        LayerPainter.drawAll(c, e.layers, cw, ch, fonts)
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

    /** Full-frame bitmap overlay shown only inside the layer's time window (output timeline). */
    private class TimedOverlay(private val bmp: Bitmap, private val startMs: Long, private val endMs: Long) : BitmapOverlay() {
        private val on = OverlaySettings.Builder().build()
        private val off = OverlaySettings.Builder().setAlphaScale(0f).build()
        override fun getBitmap(presentationTimeUs: Long) = bmp
        override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
            val ms = presentationTimeUs / 1000
            return if (ms >= startMs && ms < endMs) on else off
        }
    }

    fun overlays(p: ReelExportParams, fonts: EditorTypefaces): List<TextureOverlay> {
        val w = p.outputWidth; val h = p.outputHeight
        val list = mutableListOf<TextureOverlay>()
        if (p.vignette > 0f) {
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); LayerPainter.drawVignette(Canvas(b), w, h, p.vignette)
            list += TimedOverlay(b, 0, Long.MAX_VALUE)
        }
        p.layers.forEach { l ->
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); LayerPainter.draw(Canvas(b), l, w, h, fonts)
            list += TimedOverlay(b, l.startMs, l.endMs)
        }
        return list
    }

    fun composition(ctx: Context, uri: Uri, p: ReelExportParams): Composition {
        val items = p.clips.map { clip ->
            val item = MediaItem.Builder().setUri(uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs).setEndPositionMs(clip.endMs).build()
            ).build()
            val audio = mutableListOf<AudioProcessor>(); val video = mutableListOf<androidx.media3.common.Effect>()
            if (p.speed != 1f) Effects.createExperimentalSpeedChangingEffect(Speed(p.speed)).let { audio += it.first; video += it.second }
            if (!p.removeAudio && p.originalVolume < 0.999f) audio += volume(p.originalVolume)
            EditedMediaItem.Builder(item).setRemoveAudio(p.removeAudio).setEffects(Effects(audio, video)).build()
        }
        val sequences = mutableListOf(EditedMediaItemSequence(items))
        p.soundUrl?.let { url ->
            val sound = EditedMediaItem.Builder(MediaItem.fromUri(url)).setRemoveVideo(true)
                .setEffects(Effects(listOf(volume(p.soundVolume)), emptyList())).build()
            sequences += EditedMediaItemSequence(listOf(sound), /* isLooping = */ true)
        }
        val fx = mutableListOf<androidx.media3.common.Effect>(
            Presentation.createForWidthAndHeight(p.outputWidth, p.outputHeight, Presentation.LAYOUT_SCALE_TO_FIT)
        )
        p.colorMatrixGl?.let { m -> fx += RgbMatrix { _, _ -> m } }
        val ov = overlays(p, EditorTypefaces.load(ctx))
        if (ov.isNotEmpty()) fx += OverlayEffect(ImmutableList.copyOf(ov))
        return Composition.Builder(sequences)
            .setEffects(Effects(emptyList(), fx))
            .experimentalSetForceAudioTrack(p.removeAudio && p.soundUrl != null)
            .build()
    }

    /** Export to an H.264/AAC MP4 in cache. Progress 0..1 and result on the main thread. */
    fun export(ctx: Context, uri: Uri, p: ReelExportParams, onProgress: (Float) -> Unit, onDone: (File?) -> Unit): () -> Unit {
        val main = Handler(Looper.getMainLooper())
        val out = File(ctx.cacheDir, "reel_edit_${System.currentTimeMillis()}.mp4")
        var transformer: Transformer? = null
        var finished = false
        main.post {
            runCatching {
                val t = Transformer.Builder(ctx.applicationContext)
                    .setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(c: Composition, r: ExportResult) { finished = true; onProgress(1f); onDone(out) }
                        override fun onError(c: Composition, r: ExportResult, e: ExportException) { finished = true; out.delete(); onDone(null) }
                    }).build()
                transformer = t
                t.start(composition(ctx, uri, p), out.absolutePath)
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
