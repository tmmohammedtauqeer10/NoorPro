package com.noorpro.app.data.media.edit

import android.content.Context
import android.graphics.Matrix
import android.net.Uri
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * 1.1.3 editor pro — live reel preview on ExoPlayer (already in the app). Plays the edit's segments as a clipped
 * playlist with per-segment speed, the filter as a GL colour matrix, freeze frames as holds, the licensed sound
 * (volume + fades) and the voiceover mix in sync. Text / stickers / captions are drawn by Compose on top, so they
 * match the export exactly. Main thread only.
 */
@OptIn(UnstableApi::class)
class ReelLivePlayer(private val ctx: Context, private val primary: Uri) {
    private val video = ExoPlayer.Builder(ctx).build()
    private val music = ExoPlayer.Builder(ctx).build().apply { repeatMode = Player.REPEAT_MODE_ALL }
    private val voice = ExoPlayer.Builder(ctx).build()
    @Volatile private var gl: FloatArray = IDENTITY
    private var effectsOk = true
    private var edit: ReelEdit? = null
    private var playlistKey: Any? = null
    private var soundUrl: String? = null
    private var voiceKey: Any? = null
    private var voiceFile: File? = null
    /** playlist item → index in edit.segments */
    private var itemSeg: IntArray = IntArray(0)
    private var lastItem = -1
    private var holdEndOut = -1.0
    private var holdOut = 0.0
    private var holdWall = 0L
    private var tv: TextureView? = null
    private var vSize: VideoSize = VideoSize.UNKNOWN
    var positionMs = 0L; private set
    var ended = false; private set

    init {
        runCatching { video.setVideoEffects(listOf(RgbMatrix { _, _ -> gl })) }.onFailure { effectsOk = false }
        video.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) { vSize = videoSize; fit() }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Some GPUs can't run the effect pipeline on a TextureView — fall back to plain playback.
                if (effectsOk) { effectsOk = false; runCatching { video.setVideoEffects(emptyList()) }; video.prepare() }
            }
        })
    }

    fun attach(view: TextureView) {
        tv = view; video.setVideoTextureView(view)
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fit() }
    }
    fun detach(view: TextureView) { video.clearVideoTextureView(view); if (tv === view) tv = null }

    /** Centre-crop the video into the 9:16 preview, like the export. */
    private fun fit() {
        val v = tv ?: return
        val vw = v.width.toFloat(); val vh = v.height.toFloat()
        if (vw <= 0 || vh <= 0 || vSize.width <= 0 || vSize.height <= 0) return
        val srcW = vSize.width * vSize.pixelWidthHeightRatio; val srcH = vSize.height.toFloat()
        val k = maxOf(vw / srcW, vh / srcH)
        v.setTransform(Matrix().apply { setScale(srcW * k / vw, srcH * k / vh, vw / 2, vh / 2) })
    }

    private fun uriOf(e: ReelEdit, src: Int): Uri = e.sources.getOrNull(src)?.uri?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: primary

    /** Apply an edit; rebuilds only what changed. */
    fun load(e: ReelEdit) {
        edit = e
        val m = e.matrix; gl = if (m.isIdentity()) IDENTITY else m.toGl()
        val key = e.segments.map { Triple(it.source, it.srcStartMs, it.srcEndMs) to it.freeze } to e.sources
        if (key != playlistKey) {
            playlistKey = key
            val items = ArrayList<MediaItem>(); val map = ArrayList<Int>()
            e.segments.forEachIndexed { i, sg ->
                if (sg.freeze) return@forEachIndexed
                items += MediaItem.Builder().setUri(uriOf(e, sg.source)).setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder().setStartPositionMs(sg.srcStartMs).setEndPositionMs(sg.srcEndMs).build()).build()
                map += i
            }
            itemSeg = map.toIntArray(); lastItem = -1
            video.setMediaItems(items); video.prepare()
            seek(positionMs)
        }
        video.volume = e.originalVolume.coerceIn(0f, 1f)
        val url = e.sound?.url
        if (url != soundUrl) {
            soundUrl = url
            if (url == null) music.clearMediaItems() else { music.setMediaItem(MediaItem.fromUri(url)); music.prepare() }
        }
        val vk = e.voices to e.outputMs
        if (vk != voiceKey) {
            voiceKey = vk
            voice.clearMediaItems(); voiceFile?.delete(); voiceFile = null
            if (e.voices.isNotEmpty()) {
                val f = runCatching { ReelExporter.voiceMixFile(ctx, e.voices, e.outputMs) }.getOrNull()
                if (f != null) { voiceFile = f; voice.setMediaItem(MediaItem.fromUri(Uri.fromFile(f))); voice.prepare() }
            }
        }
        updateGains()
    }

    /** Seek to an output position (also used before play). */
    fun seek(outMs: Long) {
        val e = edit ?: return
        positionMs = outMs.coerceIn(0, e.outputMs); ended = false
        val segIdx = e.segments.indexOfFirst { positionMs < it.outEnd }.let { if (it < 0) e.segments.lastIndex else it }
        val sg = e.segments.getOrNull(segIdx) ?: return
        holdEndOut = -1.0
        if (sg.freeze) {
            val next = itemSeg.indexOfFirst { it > segIdx }
            if (next >= 0) { video.seekTo(next, 0); lastItem = next }
            holdOut = positionMs.toDouble(); holdEndOut = sg.outEnd; holdWall = android.os.SystemClock.elapsedRealtime()
        } else {
            val item = itemSeg.indexOf(segIdx)
            if (item >= 0) { video.seekTo(item, ((positionMs - sg.outStart) * sg.speed).toLong().coerceAtLeast(0)); lastItem = item; setSpeed(sg.speed) }
        }
        syncSide()
    }

    fun play(fromMs: Long) {
        seek(fromMs)
        if (holdEndOut < 0) video.play()
        holdWall = android.os.SystemClock.elapsedRealtime()
        if (soundUrl != null) music.play()
        if (voiceFile != null) voice.play()
    }

    fun pause() { video.pause(); music.pause(); voice.pause() }

    /** Advance the output clock; call once per frame while playing. */
    fun tick(): Long {
        val e = edit ?: return positionMs
        val now = android.os.SystemClock.elapsedRealtime()
        if (holdEndOut >= 0) {
            val out = holdOut + (now - holdWall)
            if (out >= holdEndOut) { holdEndOut = -1.0; positionMs = out.toLong(); video.play() } else { positionMs = out.toLong(); updateGains(); return positionMs }
        } else holdWall = now
        val item = video.currentMediaItemIndex
        val segIdx = itemSeg.getOrNull(item)
        if (segIdx != null) {
            val sg = e.segments[segIdx]
            if (item != lastItem) {
                val prev = e.segments.getOrNull(segIdx - 1)
                lastItem = item; setSpeed(sg.speed)
                if (prev != null && prev.freeze) {
                    video.pause(); holdOut = prev.outStart; holdEndOut = prev.outEnd; holdWall = now; positionMs = prev.outStartMs; return positionMs
                }
            }
            positionMs = (sg.outStart + video.currentPosition / sg.speed).toLong().coerceAtMost(e.outputMs)
        }
        if (video.playbackState == Player.STATE_ENDED || positionMs >= e.outputMs - 30) { ended = true; positionMs = e.outputMs; pause() }
        updateGains()
        return positionMs
    }

    private fun setSpeed(f: Float) { if (video.playbackParameters.speed != f) video.playbackParameters = PlaybackParameters(f) }

    private fun syncSide() {
        val e = edit ?: return
        if (soundUrl != null) { val d = music.duration; music.seekTo(if (d > 0) positionMs % d else positionMs) }
        if (voiceFile != null) voice.seekTo(positionMs)
        updateGains()
    }

    private fun updateGains() {
        val e = edit ?: return
        music.volume = (e.soundVolume * AudioFades.gain(positionMs, e.outputMs, e.soundFadeInMs, e.soundFadeOutMs)).coerceIn(0f, 1f)
        voice.volume = 1f
    }

    fun release() { video.release(); music.release(); voice.release() }

    companion object {
        private val IDENTITY = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
    }
}
