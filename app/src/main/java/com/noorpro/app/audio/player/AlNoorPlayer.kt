package com.noorpro.app.audio.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.noorpro.app.audio.models.PlaybackState
import com.noorpro.app.audio.models.QueueState
import com.noorpro.app.audio.models.RepeatMode
import com.noorpro.app.audio.models.Track
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

    private val exo: ExoPlayer = ExoPlayer.Builder(appContext).build().also { player ->
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playback.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionUpdates() else stopPositionUpdates()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onTrackEnded()
                }
                val duration = player.duration.coerceAtLeast(0L)
                if (duration > 0) {
                    _playback.update { it.copy(durationMs = duration) }
                }
            }
        })
    }

    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackState())
    val playback: StateFlow<PlaybackState> = _playback.asStateFlow()

    fun setQueue(tracks: List<Track>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) {
            clearQueue()
            return
        }
        val index = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.value = QueueState(tracks = tracks, currentIndex = index)
        prepareCurrent(autoPlay)
    }

    fun playTrack(track: Track, queueTracks: List<Track> = listOf(track)) {
        val list = if (queueTracks.any { it.id == track.id }) queueTracks else listOf(track) + queueTracks
        val index = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        setQueue(list, index, autoPlay = true)
    }

    fun play() {
        if (_queue.value.isEmpty) return
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
        val q = _queue.value
        if (q.isEmpty) return
        val next = when {
            q.currentIndex < q.tracks.lastIndex -> q.currentIndex + 1
            q.repeatMode == RepeatMode.ALL -> 0
            else -> return
        }
        _queue.update { it.copy(currentIndex = next) }
        prepareCurrent(autoPlay = true)
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty) return
        if (_playback.value.positionMs > 3_000L) {
            seekTo(0L)
            return
        }
        val prev = if (q.currentIndex > 0) q.currentIndex - 1 else return
        _queue.update { it.copy(currentIndex = prev) }
        prepareCurrent(autoPlay = true)
    }

    fun toggleShuffle() {
        _queue.update { it.copy(shuffle = !it.shuffle) }
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
        exo.repeatMode = when (_queue.value.repeatMode) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
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
        if (index == q.currentIndex) prepareCurrent(autoPlay = _playback.value.isPlaying)
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
        stopPositionUpdates()
        clearQueue()
        exo.release()
    }

    private fun prepareCurrent(autoPlay: Boolean) {
        val track = _queue.value.currentTrack ?: return
        val item = MediaItem.fromUri(track.audioUrl)
        exo.setMediaItem(item)
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

    private fun onTrackEnded() {
        val q = _queue.value
        when {
            q.repeatMode == RepeatMode.ONE -> {
                seekTo(0L)
                play()
            }
            q.currentIndex < q.tracks.lastIndex -> skipNext()
            q.repeatMode == RepeatMode.ALL -> {
                _queue.update { it.copy(currentIndex = 0) }
                prepareCurrent(autoPlay = true)
            }
            else -> {
                _playback.update { it.copy(isPlaying = false, positionMs = 0L) }
            }
        }
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
}
