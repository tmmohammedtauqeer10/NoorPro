package com.example.audio.models

enum class RepeatMode {
    OFF,
    ALL,
    ONE,
}

data class QueueState(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
) {
    val currentTrack: Track?
        get() = tracks.getOrNull(currentIndex)

    val isEmpty: Boolean
        get() = tracks.isEmpty()
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val durationMs: Long = 0L,
)
