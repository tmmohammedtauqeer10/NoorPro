package com.noorpro.app.audio.repository

import android.content.Context
import com.noorpro.app.audio.models.Artist
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Playlist
import com.noorpro.app.audio.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Catalog + playlist access for Al Noor Audio (copyright-free nasheed/naat only).
 */
interface AlNoorAudioRepository {
    fun browseShelves(): Flow<Map<String, List<Track>>>
    fun search(query: String): Flow<List<Track>>
    fun getTrack(id: String): Flow<Track?>
    fun getArtist(id: String): Flow<Artist?>
    fun getPlaylist(id: String): Flow<Playlist?>
    fun systemPlaylists(): Flow<List<Playlist>>
    fun userPlaylists(): Flow<List<Playlist>>
    fun getPlaylistTracks(playlistId: String): Flow<List<Track>>
    fun demoNotice(): Flow<String?>
    suspend fun ensureLoaded()
}

/**
 * Loads [assets/al_noor_audio/catalog.json] — clearly marked demo / public-domain
 * placeholders. No commercial / pirated tracks.
 */
class BundledAlNoorAudioRepository(
    private val context: Context,
    private val assetPath: String = "al_noor_audio/catalog.json",
) : AlNoorAudioRepository {

    private val loaded = AtomicBoolean(false)
    private val artists = MutableStateFlow<Map<String, Artist>>(emptyMap())
    private val tracks = MutableStateFlow<Map<String, Track>>(emptyMap())
    private val playlists = MutableStateFlow<Map<String, Playlist>>(emptyMap())
    private val shelfOrder = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    private val notice = MutableStateFlow<String?>(null)

    override suspend fun ensureLoaded() {
        if (loaded.get()) return
        withContext(Dispatchers.IO) {
            synchronized(this@BundledAlNoorAudioRepository) {
                if (loaded.get()) return@withContext
                val raw = context.assets.open(assetPath).bufferedReader().use { it.readText() }
                parseCatalog(raw)
                loaded.set(true)
            }
        }
    }

    private fun parseCatalog(raw: String) {
        val root = JSONObject(raw)
        notice.value = root.optString("demoNotice").ifBlank { null }

        val artistMap = linkedMapOf<String, Artist>()
        root.optJSONArray("artists")?.forEachObject { obj ->
            val id = obj.getString("id")
            artistMap[id] = Artist(
                id = id,
                name = obj.getString("name"),
                bio = obj.optStringOrNull("bio"),
                imageUrl = obj.optStringOrNull("imageUrl"),
            )
        }
        artists.value = artistMap

        val trackMap = linkedMapOf<String, Track>()
        root.optJSONArray("tracks")?.forEachObject { obj ->
            val id = obj.getString("id")
            val license = runCatching {
                LicenseType.valueOf(obj.getString("license"))
            }.getOrDefault(LicenseType.PUBLIC_DOMAIN)
            trackMap[id] = Track(
                id = id,
                title = obj.getString("title"),
                artistId = obj.getString("artistId"),
                artistName = obj.optString("artistName", artistMap[obj.optString("artistId")]?.name ?: ""),
                audioUrl = obj.getString("audioUrl"),
                durationMs = obj.optLong("durationMs", 0L),
                coverUrl = obj.optStringOrNull("coverUrl"),
                license = license,
                attributionText = obj.getString("attributionText"),
                sourceUrl = obj.getString("sourceUrl"),
                licenseUrl = obj.optStringOrNull("licenseUrl"),
                tags = obj.optJSONArray("tags")?.toStringList().orEmpty(),
                language = obj.optStringOrNull("language"),
            )
        }
        tracks.value = trackMap

        val playlistMap = linkedMapOf<String, Playlist>()
        root.optJSONArray("playlists")?.forEachObject { obj ->
            val id = obj.getString("id")
            playlistMap[id] = Playlist(
                id = id,
                title = obj.getString("title"),
                description = obj.optStringOrNull("description"),
                coverUrl = obj.optStringOrNull("coverUrl"),
                trackIds = obj.optJSONArray("trackIds")?.toStringList().orEmpty(),
                isSystem = obj.optBoolean("isSystem", true),
            )
        }
        playlists.value = playlistMap

        val shelves = mutableListOf<Pair<String, String>>()
        root.optJSONArray("shelves")?.forEachObject { obj ->
            shelves += obj.getString("title") to obj.getString("playlistId")
        }
        shelfOrder.value = shelves
    }

    override fun browseShelves(): Flow<Map<String, List<Track>>> = flow {
        ensureLoaded()
        val map = linkedMapOf<String, List<Track>>()
        for ((title, playlistId) in shelfOrder.value) {
            val pl = playlists.value[playlistId] ?: continue
            map[title] = pl.trackIds.mapNotNull { tracks.value[it] }
        }
        emit(map)
    }

    override fun search(query: String): Flow<List<Track>> = flow {
        ensureLoaded()
        val q = query.trim()
        if (q.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        emit(
            tracks.value.values.filter { track ->
                track.title.contains(q, ignoreCase = true) ||
                    track.artistName.contains(q, ignoreCase = true) ||
                    track.tags.any { it.contains(q, ignoreCase = true) } ||
                    (track.language?.contains(q, ignoreCase = true) == true)
            }
        )
    }

    override fun getTrack(id: String): Flow<Track?> = flow {
        ensureLoaded()
        emit(tracks.value[id])
    }

    override fun getArtist(id: String): Flow<Artist?> = flow {
        ensureLoaded()
        emit(artists.value[id])
    }

    override fun getPlaylist(id: String): Flow<Playlist?> = flow {
        ensureLoaded()
        emit(playlists.value[id])
    }

    override fun systemPlaylists(): Flow<List<Playlist>> = flow {
        ensureLoaded()
        emit(playlists.value.values.filter { it.isSystem })
    }

    override fun userPlaylists(): Flow<List<Playlist>> = flow {
        ensureLoaded()
        emit(playlists.value.values.filter { !it.isSystem })
    }

    override fun getPlaylistTracks(playlistId: String): Flow<List<Track>> = flow {
        ensureLoaded()
        val pl = playlists.value[playlistId]
        emit(pl?.trackIds?.mapNotNull { tracks.value[it] }.orEmpty())
    }

    override fun demoNotice(): Flow<String?> = notice

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key)
        return v.ifBlank { null }
    }

    private fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) {
            block(getJSONObject(i))
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}

/** Empty stub kept for tests / previews. */
class StubAlNoorAudioRepository : AlNoorAudioRepository {
    override fun browseShelves(): Flow<Map<String, List<Track>>> = flow { emit(emptyMap()) }
    override fun search(query: String): Flow<List<Track>> = flow { emit(emptyList()) }
    override fun getTrack(id: String): Flow<Track?> = flow { emit(null) }
    override fun getArtist(id: String): Flow<Artist?> = flow { emit(null) }
    override fun getPlaylist(id: String): Flow<Playlist?> = flow { emit(null) }
    override fun systemPlaylists(): Flow<List<Playlist>> = flow { emit(emptyList()) }
    override fun userPlaylists(): Flow<List<Playlist>> = flow { emit(emptyList()) }
    override fun getPlaylistTracks(playlistId: String): Flow<List<Track>> = flow { emit(emptyList()) }
    override fun demoNotice(): Flow<String?> = flow { emit(null) }
    override suspend fun ensureLoaded() = Unit
}
