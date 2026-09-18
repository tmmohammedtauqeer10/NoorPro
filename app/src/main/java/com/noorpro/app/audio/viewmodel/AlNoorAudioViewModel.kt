package com.noorpro.app.audio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.models.PlaybackState
import com.noorpro.app.audio.models.Playlist
import com.noorpro.app.audio.models.QueueState
import com.noorpro.app.audio.models.Track
import com.noorpro.app.audio.player.AlNoorPlayer
import com.noorpro.app.audio.repository.AlNoorAudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlNoorAudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: AlNoorAudioRepository
    val player: AlNoorPlayer

    init {
        AlNoorAudioSession.init(application)
        repo = AlNoorAudioSession.repository
        player = AlNoorAudioSession.player
    }

    val queue: StateFlow<QueueState> = player.queue
    val playback: StateFlow<PlaybackState> = player.playback

    private val _shelves = MutableStateFlow<Map<String, List<Track>>>(emptyMap())
    val shelves: StateFlow<Map<String, List<Track>>> = _shelves.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _demoNotice = MutableStateFlow<String?>(null)
    val demoNotice: StateFlow<String?> = _demoNotice.asStateFlow()

    private val _playlistTracks = MutableStateFlow<List<Track>>(emptyList())
    val playlistTracks: StateFlow<List<Track>> = _playlistTracks.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        refreshHome()
    }

    fun refreshHome() {
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                repo.ensureLoaded()
                _demoNotice.value = repo.demoNotice().first()
                _shelves.value = repo.browseShelves().first()
                _playlists.value = repo.systemPlaylists().first()
            }
            _loading.value = false
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _searchResults.value = if (query.isBlank()) emptyList() else repo.search(query).first()
        }
    }

    fun loadPlaylist(playlistId: String) {
        viewModelScope.launch {
            _selectedPlaylist.value = repo.getPlaylist(playlistId).first()
            _playlistTracks.value = repo.getPlaylistTracks(playlistId).first()
        }
    }

    fun playTrack(track: Track, contextTracks: List<Track> = listOf(track)) {
        player.playTrack(track, contextTracks.ifEmpty { listOf(track) })
    }

    fun playPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        player.setQueue(tracks, startIndex, autoPlay = true)
    }
}
