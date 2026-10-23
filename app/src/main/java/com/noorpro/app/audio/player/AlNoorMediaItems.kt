package com.noorpro.app.audio.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.noorpro.app.audio.models.Track

/** Maps catalog [Track]s to Media3 [MediaItem]s (metadata feeds notification / lock screen / QS player). */
object AlNoorMediaItems {
    const val ALBUM_TITLE = "Al Noor Audio"
    val FALLBACK_ART = "android.resource://${com.noorpro.app.BuildConfig.APPLICATION_ID}/drawable/audio_player_artwork"
    val QURAN_ART = "android.resource://${com.noorpro.app.BuildConfig.APPLICATION_ID}/drawable/noor_quran_rehal"

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
            // 1.0.83: always give the notification / lock screen artwork (bundled fallback when the
            // feed has no cover) so the shade never shows a blank tile.
            .setArtworkUri(Uri.parse(track.coverUrl?.takeIf { it.isNotBlank() } ?: FALLBACK_ART))
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.audioUrl)
            .setMediaMetadata(metadata)
            .build()
    }
}
