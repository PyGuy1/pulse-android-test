package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.ArtistSummary
import com.example.data.model.PlaylistSummary
import com.example.data.model.Track
import com.example.data.remote.MusicFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchTab(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    PLAYLISTS("Playlists")
}

data class SearchUiState(
    val query: String = "",
    val committedQuery: String = "",
    val selectedTab: SearchTab = SearchTab.ALL,
    val suggestions: List<String> = emptyList(),
    val songs: List<Track> = emptyList(),
    val artists: List<ArtistSummary> = emptyList(),
    val albums: List<PlaylistSummary> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val artistAvatars: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SearchViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val repo = container.musicRepository
    private val api = repo.api

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recentSearches: StateFlow<List<String>> = repo.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var suggestJob: Job? = null
    private var debounceSearchJob: Job? = null
    private var searchJob: Job? = null

    fun onQueryChanged(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
        val trimmed = newQuery.trim()
        suggestJob?.cancel()
        debounceSearchJob?.cancel()

        if (trimmed.isEmpty()) {
            _state.update {
                it.copy(
                    committedQuery = "",
                    suggestions = emptyList(),
                    songs = emptyList(),
                    artists = emptyList(),
                    albums = emptyList(),
                    playlists = emptyList(),
                    isLoading = false,
                    errorMessage = null
                )
            }
            return
        }

        suggestJob = viewModelScope.launch {
            delay(220)
            try {
                val sug = api.getSuggestions(trimmed).take(6)
                if (_state.value.query.trim() == trimmed) {
                    _state.update { it.copy(suggestions = sug) }
                }
            } catch (_: Exception) {}
        }

        if (trimmed.length >= 3) {
            debounceSearchJob = viewModelScope.launch {
                delay(650)
                executeSearch(trimmed, commit = false)
            }
        }
    }

    fun submitSearch(queryOverride: String? = null, onDirectPlay: (Track) -> Unit = {}) {
        val q = (queryOverride ?: _state.value.query).trim()
        if (q.isEmpty()) return
        suggestJob?.cancel()
        debounceSearchJob?.cancel()
        _state.update { it.copy(query = q, suggestions = emptyList()) }
        executeSearch(q, commit = true, onDirectPlay = onDirectPlay)
    }

    fun selectTab(tab: SearchTab) {
        _state.update { it.copy(selectedTab = tab) }
        val q = _state.value.committedQuery.ifBlank { _state.value.query.trim() }
        if (q.isNotBlank()) {
            executeSearch(q, commit = false)
        }
    }

    fun removeRecentSearch(q: String) {
        viewModelScope.launch { repo.removeRecentSearch(q) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { repo.clearRecentSearches() }
    }

    fun retry() {
        val q = _state.value.committedQuery.ifBlank { _state.value.query.trim() }
        if (q.isNotBlank()) executeSearch(q, commit = false)
    }

    private fun executeSearch(q: String, commit: Boolean, onDirectPlay: (Track) -> Unit = {}) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (commit) {
                repo.addRecentSearch(q)
                _state.update { it.copy(suggestions = emptyList()) }
            }

            // Direct YouTube URL / Video ID match
            val vidMatch = Regex("(?:v=|youtu\\.be/|shorts/|embed/)([\\w-]{11})|^([\\w-]{11})$").find(q)
            if (vidMatch != null && (q.contains("/") || q.contains("=") || (Regex("^[\\w-]{11}$").matches(q) && Regex("[\\d_-]").containsMatchIn(q)))) {
                val id = vidMatch.groupValues[1].ifBlank { vidMatch.groupValues[2] }
                _state.update { it.copy(isLoading = true, errorMessage = null, committedQuery = q) }
                val meta = api.getVideoEmbedMetadata(id)
                val track = Track(
                    id = id,
                    title = meta?.first ?: "YouTube video $id",
                    artist = meta?.second ?: "",
                    thumbUrl = "https://i.ytimg.com/vi/$id/mqdefault.jpg"
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        selectedTab = SearchTab.SONGS,
                        songs = listOf(track)
                    )
                }
                onDirectPlay(track)
                return@launch
            }

            _state.update { it.copy(isLoading = true, errorMessage = null, committedQuery = q) }
            val tab = _state.value.selectedTab

            val songsDef = if (tab == SearchTab.ALL || tab == SearchTab.SONGS) async {
                runCatching { api.searchSongs(q).filter { MusicFilter.okSongForSearch(it, q) } }
            } else null

            val artistsDef = if (tab == SearchTab.ALL || tab == SearchTab.ARTISTS) async {
                runCatching { api.searchArtists(q) }
            } else null

            val albumsDef = if (tab == SearchTab.ALL || tab == SearchTab.ALBUMS) async {
                runCatching { api.searchAlbums(q) }
            } else null

            val playlistsDef = if (tab == SearchTab.ALL || tab == SearchTab.PLAYLISTS) async {
                runCatching { api.searchPlaylists(q) }
            } else null

            val songsRes = songsDef?.await()
            val artistsRes = artistsDef?.await()
            val albumsRes = albumsDef?.await()
            val playlistsRes = playlistsDef?.await()

            val allFailed = listOfNotNull(songsRes, artistsRes, albumsRes, playlistsRes).all { it.isFailure }
            if (allFailed) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Search is unavailable right now (servers are busy)."
                    )
                }
                return@launch
            }

            val artistsList = artistsRes?.getOrNull() ?: _state.value.artists
            _state.update { cur ->
                cur.copy(
                    isLoading = false,
                    errorMessage = null,
                    songs = songsRes?.getOrNull() ?: cur.songs,
                    artists = artistsList,
                    albums = albumsRes?.getOrNull() ?: cur.albums,
                    playlists = playlistsRes?.getOrNull() ?: cur.playlists
                )
            }

            if (artistsList.isNotEmpty()) {
                val map = repo.ensureArtistImages(artistsList.take(10).map { it.name })
                _state.update { it.copy(artistAvatars = it.artistAvatars + map) }
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(container) as T
        }
    }
}
