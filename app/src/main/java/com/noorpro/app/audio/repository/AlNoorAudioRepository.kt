package com.noorpro.app.audio.repository

import com.noorpro.app.audio.models.Artist
import com.noorpro.app.audio.models.Playlist
import com.noorpro.app.audio.models.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Catalog + playlist access for Al Noor Audio.
 * Stub: empty flows until a JSON/CDN catalog is wired.
 */
interface AlNoorAudioRepository {
    fun browseShelves(): Flow<Map<String, List<Track>>>
    fun search(query: String): Flow<List<Track>>
    fun getTrack(id: String): Flow<Track?>
    fun getArtist(id: String): Flow<Artist?>
    fun getPlaylist(id: String): Flow<Playlist?>
    fun systemPlaylists(): Flow<List<Playlist>>
    fun userPlaylists(): Flow<List<Playlist>>
}

/** In-memory stub used until assets/remote catalog exists. */
class StubAlNoorAudioRepository : AlNoorAudioRepository {
    override fun browseShelves(): Flow<Map<String, List<Track>>> = flowOf(emptyMap())
    override fun search(query: String): Flow<List<Track>> = flowOf(emptyList())
    override fun getTrack(id: String): Flow<Track?> = flowOf(null)
    override fun getArtist(id: String): Flow<Artist?> = flowOf(null)
    override fun getPlaylist(id: String): Flow<Playlist?> = flowOf(null)
    override fun systemPlaylists(): Flow<List<Playlist>> = flowOf(emptyList())
    override fun userPlaylists(): Flow<List<Playlist>> = flowOf(emptyList())
}
