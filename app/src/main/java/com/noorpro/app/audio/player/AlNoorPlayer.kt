package com.noorpro.app.audio.player

import com.noorpro.app.audio.models.PlaybackState
import com.noorpro.app.audio.models.QueueState
import com.noorpro.app.audio.models.RepeatMode
import com.noorpro.app.audio.models.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Queue + playback façade for Al Noor Audio.
 * Stub holds state only; Media3 ExoPlayer wiring comes later
 * (see docs/AL_NOOR_AUDIO.md). Keep separate from Quran MediaPlayer.
 */
class AlNoorPlayer {
    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackState())
    val playback: StateFlow<PlaybackState> = _playback.asStateFlow()

    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) {
            _queue.value = QueueState()
            _playback.value = PlaybackState()
            return
        }
        val index = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.value = QueueState(tracks = tracks, currentIndex = index)
        _playback.update { it.copy(durationMs = tracks[index].durationMs, positionMs = 0L) }
    }

    fun play() {
        if (_queue.value.isEmpty) return
        _playback.update { it.copy(isPlaying = true) }
        // TODO: ExoPlayer.play()
    }

    fun pause() {
        _playback.update { it.copy(isPlaying = false) }
        // TODO: ExoPlayer.pause()
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
        _playback.update {
            it.copy(positionMs = 0L, durationMs = q.tracks[next].durationMs)
        }
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty) return
        val prev = if (q.currentIndex > 0) q.currentIndex - 1 else return
        _queue.update { it.copy(currentIndex = prev) }
        _playback.update {
            it.copy(positionMs = 0L, durationMs = q.tracks[prev].durationMs)
        }
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
    }

    fun clearQueue() {
        _queue.value = QueueState()
        _playback.value = PlaybackState()
    }

    fun release() {
        clearQueue()
        // TODO: release ExoPlayer / MediaSession
    }
}
