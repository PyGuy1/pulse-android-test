package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.PlaylistSummary
import com.example.data.model.Track
import com.example.data.remote.MusicConstants
import com.example.data.remote.MusicFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ShelfState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HomeUiState(
    val greeting: String = "Good evening",
    val subtitle: String = "Play a few songs and Pulse will learn your taste.",
    val regionCode: String = "US",
    val regionName: String = "United States",
    val recentlyPlayed: List<Track> = emptyList(),
    val lastPlayedTitle: String? = null,
    val userTopArtists: List<String> = emptyList(),
    val forYou: ShelfState<Track> = ShelfState(),
    val becauseYouPlayed: ShelfState<Track> = ShelfState(isLoading = false),
    val topPlaylists: ShelfState<PlaylistSummary> = ShelfState(),
    val chartTracks: ShelfState<Track> = ShelfState(),
    val hitsPlaylists: ShelfState<PlaylistSummary> = ShelfState(),
    val bestOfPlaylists: ShelfState<PlaylistSummary> = ShelfState(),
    val famousArtists: ShelfState<String> = ShelfState(),
    val popularArtists: ShelfState<String> = ShelfState(),
    val newReleases: ShelfState<PlaylistSummary> = ShelfState(),
    val artistAvatars: Map<String, String> = emptyMap(),
    val languages: List<String> = MusicConstants.languagesFor("US"),
    val isSurpriseLoading: Boolean = false
)

class HomeViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val repo = container.musicRepository
    private val prefs = container.prefs

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadedRegion: String? = null

    init {
        loadHome(forceRefresh = false)
    }

    fun loadHome(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            repo.api.clearCache()
            viewModelScope.launch { repo.api.discoverInstances() }
        }

        viewModelScope.launch {
            val code = prefs.getRegionOverrideOnce()
                ?: prefs.getDetectedRegionOnce()
                ?: MusicConstants.detectDeviceCountryCode()
            val cn = MusicConstants.countryName(code)
            val isGlobal = code == "WW"

            val history = repo.historyTracks.first()
            val topArts = repo.topArtists(4)
            val playCounts = repo.artistPlayCounts.first().sumOf { it.playCount }

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 5 -> "Good night"
                hour < 12 -> "Good morning"
                hour < 18 -> "Good afternoon"
                else -> "Good evening"
            }

            val subtitle = if (playCounts > 0) {
                val topPart = topArts.firstOrNull()?.let { " · top artist $it" } ?: ""
                "$playCounts songs played$topPart"
            } else {
                "Play a few songs and Pulse will learn your taste."
            }

            val lastTrack = history.firstOrNull()
            loadedRegion = code

            _state.update {
                it.copy(
                    greeting = greeting,
                    subtitle = subtitle,
                    regionCode = code,
                    regionName = cn,
                    recentlyPlayed = history.take(15),
                    lastPlayedTitle = lastTrack?.title?.take(36),
                    userTopArtists = topArts,
                    languages = MusicConstants.languagesFor(code),
                    forYou = ShelfState(isLoading = true),
                    becauseYouPlayed = ShelfState(isLoading = lastTrack != null),
                    topPlaylists = ShelfState(isLoading = true),
                    chartTracks = ShelfState(isLoading = true),
                    hitsPlaylists = ShelfState(isLoading = true),
                    bestOfPlaylists = ShelfState(isLoading = true),
                    famousArtists = ShelfState(isLoading = true),
                    popularArtists = ShelfState(isLoading = true),
                    newReleases = ShelfState(isLoading = true)
                )
            }

            if (topArts.isNotEmpty()) {
                launch {
                    val map = repo.ensureArtistImages(topArts)
                    _state.update { it.copy(artistAvatars = it.artistAvatars + map) }
                }
            }

            // Suggested for you
            launch {
                try {
                    val items = repo.getForYou()
                    _state.update { it.copy(forYou = ShelfState(items = items, isLoading = false)) }
                } catch (_: Exception) {
                    _state.update { it.copy(forYou = ShelfState(isLoading = false, error = "Couldn’t load suggestions right now.")) }
                }
            }

            // Because you played
            if (lastTrack != null) {
                launch {
                    try {
                        val items = repo.getSimilarTracks(lastTrack).take(15)
                        _state.update { it.copy(becauseYouPlayed = ShelfState(items = items, isLoading = false)) }
                    } catch (_: Exception) {
                        _state.update { it.copy(becauseYouPlayed = ShelfState(isLoading = false, error = "No matches found yet.")) }
                    }
                }
            }

            // Chart + Popular artists
            launch {
                try {
                    val chart = repo.getChart(code)
                    val popArtists = chart.map { it.artist }.filter { it.isNotBlank() }.distinct().take(15)
                    _state.update {
                        it.copy(
                            chartTracks = ShelfState(items = chart, isLoading = false),
                            popularArtists = ShelfState(items = popArtists, isLoading = false)
                        )
                    }
                    val map = repo.ensureArtistImages(popArtists)
                    _state.update { it.copy(artistAvatars = it.artistAvatars + map) }
                } catch (_: Exception) {
                    _state.update {
                        it.copy(
                            chartTracks = ShelfState(isLoading = false, error = "Charts unavailable right now."),
                            popularArtists = ShelfState(isLoading = false, error = "Unavailable right now.")
                        )
                    }
                }
            }

            // Top playlists
            launch {
                val queries = if (isGlobal) {
                    listOf("top 100 songs global", "global top 50", "top hits right now", "viral hits")
                } else {
                    listOf("top 100 songs $cn", "top 50 $cn", "$cn top hits", "$cn viral songs")
                }
                val pls = repo.pickPlaylists(queries, perQuery = 2, maxTotal = 14)
                _state.update {
                    it.copy(topPlaylists = ShelfState(items = pls, isLoading = false, error = if (pls.isEmpty()) "Unavailable right now." else null))
                }
            }

            // Hits
            launch {
                val pls = repo.pickPlaylists(
                    listOf("all time hits", "pop hits", "hip hop hits", "rock hits", "dance hits", "r&b hits", "latin hits"),
                    perQuery = 1,
                    maxTotal = 14
                )
                _state.update { it.copy(hitsPlaylists = ShelfState(items = pls, isLoading = false)) }
            }

            // Best of decades
            launch {
                val pls = repo.pickPlaylists(
                    listOf("best of 2020s", "best of 2010s", "best of 2000s", "best of 90s", "best of 80s", "best of 70s"),
                    perQuery = 1,
                    maxTotal = 14
                )
                _state.update { it.copy(bestOfPlaylists = ShelfState(items = pls, isLoading = false)) }
            }

            // Famous artists
            launch {
                val famous = try { repo.getFamousArtists() } catch (_: Exception) { MusicConstants.FAMOUS_ARTISTS }
                _state.update { it.copy(famousArtists = ShelfState(items = famous, isLoading = false)) }
                val map = repo.ensureArtistImages(famous)
                _state.update { it.copy(artistAvatars = it.artistAvatars + map) }
            }

            // New releases
            launch {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val releases = try { repo.api.searchAlbums("new releases $year").take(14) } catch (_: Exception) { emptyList() }
                _state.update { it.copy(newReleases = ShelfState(items = releases, isLoading = false)) }
            }
        }
    }

    fun refreshRegionIfNeeded(newRegionCode: String) {
        if (loadedRegion != null && loadedRegion != newRegionCode) {
            loadHome(forceRefresh = false)
        }
    }

    fun surpriseMe(onResult: (List<Track>, Int, String) -> Unit) {
        if (_state.value.isSurpriseLoading) return
        _state.update { it.copy(isSurpriseLoading = true) }
        viewModelScope.launch {
            try {
                val mood = MusicConstants.MOODS.random()
                val (_, tracks) = repo.getMoodData(mood)
                if (tracks.isNotEmpty()) {
                    val idx = (0 until minOf(tracks.size, 8)).random()
                    onResult(tracks, idx, "Surprise: ${mood.label}")
                } else {
                    container.playbackController.emitMessage("Nothing found, try again")
                }
            } catch (_: Exception) {
                container.playbackController.emitMessage("Search busy, try again")
            } finally {
                _state.update { it.copy(isSurpriseLoading = false) }
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(container) as T
        }
    }
}
