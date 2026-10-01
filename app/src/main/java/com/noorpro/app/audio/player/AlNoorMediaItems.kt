package com.noorpro.app.audio.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.noorpro.app.audio.models.Track

/** Maps catalog [Track]s to Media3 [MediaItem]s (metadata feeds notification / lock screen / QS player). */
object AlNoorMediaItems {
    const val ALBUM_TITLE = "Al Noor Audio"

    fun fromTrack(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistName)
            .setAlbumTitle(ALBUM_TITLE)
            .setDisplayTitle(track.title)
            .setSubtitle(track.artistName)
            .setDescription(track.attributionText)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .apply { track.coverUrl?.takeIf { it.isNotBlank() }?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.audioUrl)
            .setMediaMetadata(metadata)
            .build()
    }
}
