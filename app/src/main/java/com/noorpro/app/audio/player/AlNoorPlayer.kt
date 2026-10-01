package com.noorpro.app.audio.player

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.noorpro.app.audio.models.PlaybackState
import com.noorpro.app.audio.models.QueueState
import com.noorpro.app.audio.models.RepeatMode
import com.noorpro.app.audio.models.Track
import com.noorpro.app.audio.session.AlNoorMediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Media3 ExoPlayer wrapper for Al Noor Audio (nasheed/naat).
 * Kept separate from Quran [android.media.MediaPlayer] in DeenViewModel.
 */
class AlNoorPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var positionJob: Job? = null

    private val exo: ExoPlayer = ExoPlayer.Builder(appContext)
        // Real media audio attributes + automatic audio focus (pauses for calls / other media apps).
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        // Keeps CPU + Wi-Fi awake while streaming with the screen off (needs WAKE_LOCK).
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build().also { player ->
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playback.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionUpdates() else stopPositionUpdates()
                updateAudible()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateAudible()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error ${error.errorCodeName}: ${error.message}")
                _playback.update { it.copy(isPlaying = false) }
                updateAudible()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // PLAYLIST_CHANGED transitions come from our own setMediaItems/remove calls, which
                // already update the queue state. Auto-advance / seek / repeat come from ExoPlayer.
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    syncQueueIndexFromExo()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onQueueEnded()
                }
                val duration = player.duration.coerceAtLeast(0L)
                if (duration > 0) {
                    _playback.update { it.copy(durationMs = duration) }
                }
                updateAudible()
            }
        })
    }

    private val _audible = MutableStateFlow(false)

    /** True while Al Noor audio is playing or about to play (buffering). Reels observe this to pause. */
    val audible: StateFlow<Boolean> = _audible.asStateFlow()

    private fun updateAudible() {
        _audible.value = exo.playWhenReady &&
            exo.playbackState != Player.STATE_IDLE && exo.playbackState != Player.STATE_ENDED
    }

    /**
     * Player handed to the MediaSession. ExoPlayer holds the whole queue (see [loadQueueIntoExo]),
     * so previous / next / seek commands work natively from the notification, lock screen,
     * Bluetooth and Android Auto. Only play / pause are routed through this class so the service is
     * (re)started and our state flows stay in sync.
     */
    val sessionPlayer: Player by lazy {
        object : ForwardingPlayer(exo) {
            override fun play() = this@AlNoorPlayer.play()
            override fun pause() = this@AlNoorPlayer.pause()
        }
    }

    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackState())
    val playback: StateFlow<PlaybackState> = _playback.asStateFlow()

    /** Exposes the underlying ExoPlayer for MediaSession binding; null only after [release]. */
    fun exoPlayerOrNull(): ExoPlayer? = if (released) null else exo

    private var released = false

    fun setQueue(tracks: List<Track>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) {
            clearQueue()
            return
        }
        val index = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.update { it.copy(tracks = tracks, currentIndex = index) }
        loadQueueIntoExo(autoPlay)
    }

    fun playTrack(track: Track, queueTracks: List<Track> = listOf(track)) {
        val list = if (queueTracks.any { it.id == track.id }) queueTracks else listOf(track) + queueTracks
        val index = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        setQueue(list, index, autoPlay = true)
    }

    fun play() {
        if (_queue.value.isEmpty) return
        ensureService()
        if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0L)
        if (exo.playbackState == Player.STATE_IDLE) exo.prepare()
        exo.play()
        _playback.update { it.copy(isPlaying = true) }
    }

    fun pause() {
        exo.pause()
        _playback.update { it.copy(isPlaying = false) }
    }

    fun togglePlayPause() {
        if (_playback.value.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        exo.seekTo(positionMs.coerceAtLeast(0L))
        _playback.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun skipNext() {
        if (_queue.value.isEmpty || !exo.hasNextMediaItem()) return
        exo.seekToNextMediaItem()
        resumeAfterSkip()
    }

    fun skipPrevious() {
        if (_queue.value.isEmpty) return
        if (_playback.value.positionMs > 3_000L || !exo.hasPreviousMediaItem()) {
            seekTo(0L)
            return
        }
        exo.seekToPreviousMediaItem()
        resumeAfterSkip()
    }

    private fun resumeAfterSkip() {
        ensureService()
        if (exo.playbackState == Player.STATE_IDLE) exo.prepare()
        exo.play()
        syncQueueIndexFromExo()
        _playback.update { it.copy(positionMs = 0L, isPlaying = true) }
    }

    fun toggleShuffle() {
        _queue.update { it.copy(shuffle = !it.shuffle) }
        applyModesToExo()
    }

    fun cycleRepeatMode() {
        _queue.update {
            val next = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = next)
        }
        applyModesToExo()
    }

    private fun applyModesToExo() {
        val q = _queue.value
        exo.repeatMode = when (q.repeatMode) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }
        exo.shuffleModeEnabled = q.shuffle
    }

    /** ExoPlayer owns the playback order (incl. shuffle); mirror its current item into [queue]. */
    private fun syncQueueIndexFromExo() {
        val q = _queue.value
        val idx = exo.currentMediaItemIndex
        if (idx !in q.tracks.indices || idx == q.currentIndex) {
            if (idx in q.tracks.indices) resetProgressFor(q.tracks[idx])
            return
        }
        _queue.value = q.copy(currentIndex = idx)
        resetProgressFor(q.tracks[idx])
    }

    private fun resetProgressFor(track: Track) {
        _playback.update { it.copy(positionMs = 0L, bufferedMs = 0L, durationMs = track.durationMs) }
    }

    fun removeFromQueue(index: Int) {
        val q = _queue.value
        if (index !in q.tracks.indices) return
        val newTracks = q.tracks.toMutableList().also { it.removeAt(index) }
        if (newTracks.isEmpty()) {
            clearQueue()
            return
        }
        val newIndex = when {
            index < q.currentIndex -> q.currentIndex - 1
            index == q.currentIndex -> index.coerceAtMost(newTracks.lastIndex)
            else -> q.currentIndex
        }
        _queue.value = q.copy(tracks = newTracks, currentIndex = newIndex)
        if (index == q.currentIndex) {
            loadQueueIntoExo(autoPlay = _playback.value.isPlaying)
        } else {
            exo.removeMediaItem(index)
        }
    }

    fun clearQueue() {
        stopPositionUpdates()
        exo.stop()
        exo.clearMediaItems()
        _queue.value = QueueState()
        _playback.value = PlaybackState()
    }

    /** Pause when adhan fires — call from prayer alarm path. */
    fun pauseForAdhan() {
        if (_playback.value.isPlaying) pause()
    }

    fun release() {
        released = true
        stopPositionUpdates()
        clearQueue()
        exo.release()
    }

    /** (Re)starts the foreground-capable MediaSessionService so notification + lock-screen controls exist. */
    private fun ensureService() {
        runCatching { AlNoorMediaSession.obtain(appContext).startSession() }
            .onFailure { Log.w(TAG, "Could not start Al Noor media service", it) }
    }

    /**
     * Hands the FULL queue to ExoPlayer as MediaItems (with title / artist / artwork metadata) so the
     * Media3 session + notification expose real previous / next and update per track.
     */
    private fun loadQueueIntoExo(autoPlay: Boolean) {
        val q = _queue.value
        val track = q.currentTrack ?: return
        if (autoPlay) ensureService()
        exo.setMediaItems(q.tracks.map { AlNoorMediaItems.fromTrack(it) }, q.currentIndex, 0L)
        applyModesToExo()
        exo.prepare()
        _playback.update {
            it.copy(
                positionMs = 0L,
                durationMs = track.durationMs,
                bufferedMs = 0L,
                isPlaying = autoPlay,
            )
        }
        if (autoPlay) exo.play() else exo.pause()
    }

    /** Fired when the last item finished and repeat is off (repeat ONE/ALL never reach ENDED). */
    private fun onQueueEnded() {
        _playback.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    private fun startPositionUpdates() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                val pos = exo.currentPosition.coerceAtLeast(0L)
                val buf = exo.bufferedPosition.coerceAtLeast(0L)
                val dur = exo.duration.takeIf { it > 0 } ?: _playback.value.durationMs
                _playback.update {
                    it.copy(positionMs = pos, bufferedMs = buf, durationMs = dur, isPlaying = exo.isPlaying)
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    companion object {
        private const val TAG = "AlNoorPlayer"
    }
}
