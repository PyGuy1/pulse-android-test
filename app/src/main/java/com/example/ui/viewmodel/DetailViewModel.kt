package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.PlaylistSummary
import com.example.data.model.Track
import com.example.data.remote.MusicConstants
import com.example.data.remote.MusicFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistDetailUiState(
    val artistName: String = "",
    val avatarUrl: String = "",
    val allSongs: List<Track> = emptyList(),
    val shownCount: Int = 10,
    val fullCatalogLoaded: Boolean = false,
    val isExpandingCatalog: Boolean = false,
    val albums: List<PlaylistSummary> = emptyList(),
    val similarArtists: List<String> = emptyList(),
    val artistAvatars: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class RemoteCollectionUiState(
    val title: String = "",
    val subtitle: String = "",
    val isApproximate: Boolean = false,
    val playlists: List<PlaylistSummary> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class DetailViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val repo = container.musicRepository

    private val _artistState = MutableStateFlow(ArtistDetailUiState())
    val artistState: StateFlow<ArtistDetailUiState> = _artistState.asStateFlow()

    private val _collectionState = MutableStateFlow(RemoteCollectionUiState())
    val collectionState: StateFlow<RemoteCollectionUiState> = _collectionState.asStateFlow()

    fun loadArtist(artistName: String) {
        _artistState.value = ArtistDetailUiState(
            artistName = artistName,
            avatarUrl = repo.api.artistAvatarCache[artistName] ?: repo.api.artistThumbCache[artistName] ?: "",
            isLoading = true
        )
        viewModelScope.launch {
            launch {
                val map = repo.ensureArtistImages(listOf(artistName))
                _artistState.update {
                    it.copy(
                        avatarUrl = map[artistName] ?: it.avatarUrl,
                        artistAvatars = it.artistAvatars + map
                    )
                }
            }

            val songs = try {
                MusicFilter.dedupe(MusicFilter.clean(repo.songsByArtist(artistName), 40))
            } catch (_: Exception) {
                emptyList()
            }

            _artistState.update {
                it.copy(
                    allSongs = songs,
                    shownCount = minOf(10, songs.size),
                    isLoading = false,
                    error = if (songs.isEmpty()) "No songs found right now. Servers may be busy." else null
                )
            }

            launch {
                val k = artistName.lowercase()
                val albums = try {
                    val r = repo.api.searchAlbums(artistName)
                    val m = r.filter { it.artist.lowercase().contains(k) }
                    (if (m.isNotEmpty()) m else r).take(12)
                } catch (_: Exception) {
                    emptyList()
                }
                _artistState.update { it.copy(albums = albums) }
            }

            songs.firstOrNull()?.let { seed ->
                launch {
                    val k = artistName.lowercase()
                    val sim = try {
                        repo.getSimilarTracks(seed)
                            .map { it.artist }
                            .filter { it.isNotBlank() && it.lowercase() != k }
                            .distinct()
                            .take(12)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    _artistState.update { it.copy(similarArtists = sim) }
                    if (sim.isNotEmpty()) {
                        val map = repo.ensureArtistImages(sim)
                        _artistState.update { it.copy(artistAvatars = it.artistAvatars + map) }
                    }
                }
            }
        }
    }

    fun showMoreArtistSongs() {
        val cur = _artistState.value
        if (cur.isExpandingCatalog) return
        if (!cur.fullCatalogLoaded) {
            _artistState.update { it.copy(isExpandingCatalog = true) }
            viewModelScope.launch {
                val expanded = try {
                    repo.getArtistFullCatalog(cur.artistName, cur.allSongs)
                } catch (_: Exception) {
                    cur.allSongs
                }
                val finalSongs = if (expanded.size > cur.allSongs.size) expanded else cur.allSongs
                _artistState.update {
                    it.copy(
                        allSongs = finalSongs,
                        fullCatalogLoaded = true,
                        isExpandingCatalog = false,
                        shownCount = minOf(finalSongs.size, cur.shownCount + 25)
                    )
                }
            }
        } else {
            _artistState.update {
                it.copy(shownCount = minOf(it.allSongs.size, it.shownCount + 25))
            }
        }
    }

    fun showLessArtistSongs() {
        _artistState.update { it.copy(shownCount = minOf(10, it.allSongs.size)) }
    }

    fun loadRemotePlaylist(id: String, title: String, by: String) {
        _collectionState.value = RemoteCollectionUiState(
            title = title.ifBlank { "Playlist" },
            subtitle = by,
            isLoading = true
        )
        viewModelScope.launch {
            try {
                val res = repo.api.getPlaylist(id, title, by)
                val tr = MusicFilter.dedupe(res.tracks.filter { it.durationSeconds == null || it.durationSeconds >= 30 })
                _collectionState.update {
                    it.copy(
                        title = res.name.ifBlank { title.ifBlank { "Playlist" } },
                        subtitle = res.artist.ifBlank { by },
                        isApproximate = res.isApproximate,
                        tracks = tr,
                        isLoading = false,
                        error = if (tr.isEmpty()) "Empty playlist." else null
                    )
                }
            } catch (_: Exception) {
                _collectionState.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn’t load this collection (servers busy)."
                    )
                }
            }
        }
    }

    fun loadMood(moodIndex: Int) {
        val mood = MusicConstants.MOODS.getOrNull(moodIndex) ?: MusicConstants.MOODS[0]
        _collectionState.value = RemoteCollectionUiState(
            title = mood.label,
            subtitle = "MOOD & GENRE",
            isLoading = true
        )
        viewModelScope.launch {
            try {
                val (pls, tr) = repo.getMoodData(mood)
                _collectionState.update {
                    it.copy(
                        playlists = pls.take(12),
                        tracks = tr,
                        isLoading = false,
                        error = if (tr.isEmpty() && pls.isEmpty()) "Couldn’t load this collection." else null
                    )
                }
            } catch (_: Exception) {
                _collectionState.update {
                    it.copy(isLoading = false, error = "Couldn’t load this collection (servers busy).")
                }
            }
        }
    }

    fun loadLanguage(language: String) {
        _collectionState.value = RemoteCollectionUiState(
            title = language,
            subtitle = "MUSIC BY LANGUAGE",
            isLoading = true
        )
        viewModelScope.launch {
            try {
                val (pls, tr) = repo.getLanguageData(language)
                _collectionState.update {
                    it.copy(
                        playlists = pls.take(12),
                        tracks = tr,
                        isLoading = false,
                        error = if (tr.isEmpty() && pls.isEmpty()) "Couldn’t load this language." else null
                    )
                }
            } catch (_: Exception) {
                _collectionState.update {
                    it.copy(isLoading = false, error = "Couldn’t load this collection (servers busy).")
                }
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailViewModel(container) as T
        }
    }
}
