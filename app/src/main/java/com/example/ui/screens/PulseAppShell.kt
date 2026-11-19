package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.data.model.PlaylistSummary
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.CountryPickerSheet
import com.example.ui.components.EditPlaylistSheet
import com.example.ui.components.ExportPlaylistSheet
import com.example.ui.components.FullScreenPlayerOverlay
import com.example.ui.components.GlassContainer
import com.example.ui.components.ImportPlaylistSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PlaylistCollageCover
import com.example.ui.components.QueueBottomSheet
import com.example.ui.components.SleepTimerSheet
import com.example.ui.components.TextPromptDialog
import com.example.ui.components.TrackMenuBottomSheet
import com.example.ui.theme.PulseBackground
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulsePrimaryContainer
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSecondary
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.viewmodel.DetailViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SearchViewModel

private object PulseRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val DOWNLOADS = "downloads"
    const val LIKED = "liked"
    const val RECENT = "recent"
    const val PLAYLIST = "playlist/{name}"
    const val ARTIST = "artist/{name}"
    const val MOOD = "mood/{index}"
    const val LANGUAGE = "language/{name}"
    const val REMOTE_PLAYLIST = "remote_playlist?id={id}&title={title}&by={by}"

    fun playlist(name: String) = "playlist/${Uri.encode(name)}"
    fun artist(name: String) = "artist/${Uri.encode(name)}"
    fun mood(index: Int) = "mood/$index"
    fun language(name: String) = "language/${Uri.encode(name)}"
    fun remotePlaylist(pl: PlaylistSummary) =
        "remote_playlist?id=${Uri.encode(pl.id)}&title=${Uri.encode(pl.name)}&by=${Uri.encode(pl.artist)}"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopNavTabs = listOf(
    NavTab(PulseRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavTab(PulseRoutes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    NavTab(PulseRoutes.LIBRARY, "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
    NavTab(PulseRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun PulseAppShell(container: AppContainer) {
    val mainVm: MainViewModel = viewModel(factory = MainViewModel.Factory(container))
    val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container))
    val searchVm: SearchViewModel = viewModel(factory = SearchViewModel.Factory(container))
    val detailVm: DetailViewModel = viewModel(factory = DetailViewModel.Factory(container))

    val playbackState by mainVm.playbackState.collectAsStateWithLifecycle()
    val likedTracks by mainVm.likedTracks.collectAsStateWithLifecycle()
    val likedIds by mainVm.likedIds.collectAsStateWithLifecycle()
    val userPlaylists by mainVm.userPlaylists.collectAsStateWithLifecycle()
    val historyTracks by mainVm.historyTracks.collectAsStateWithLifecycle()
    val downloadedTracks by mainVm.downloadedTracks.collectAsStateWithLifecycle()
    val downloadStates by mainVm.downloadStates.collectAsStateWithLifecycle()
    val autoplay by mainVm.autoplay.collectAsStateWithLifecycle()
    val audioQuality by mainVm.audioQuality.collectAsStateWithLifecycle()
    val downloadQuality by mainVm.downloadQuality.collectAsStateWithLifecycle()
    val reduceMotion by mainVm.reduceMotion.collectAsStateWithLifecycle()
    val customPipedUrl by mainVm.customPipedUrl.collectAsStateWithLifecycle()
    val regionCode by mainVm.regionCode.collectAsStateWithLifecycle()
    val isRegionOverride by mainVm.isRegionOverride.collectAsStateWithLifecycle()
    val importPreview by mainVm.importPreview.collectAsStateWithLifecycle()
    val importBusy by mainVm.importBusy.collectAsStateWithLifecycle()
    val importError by mainVm.importError.collectAsStateWithLifecycle()

    val homeState by homeVm.state.collectAsStateWithLifecycle()
    val searchState by searchVm.state.collectAsStateWithLifecycle()
    val recentSearches by searchVm.recentSearches.collectAsStateWithLifecycle()
    val artistState by detailVm.artistState.collectAsStateWithLifecycle()
    val collectionState by detailVm.collectionState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: PulseRoutes.HOME
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Modal / Overlay UI State
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showCountrySheet by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var confirmClearHistoryDialog by remember { mutableStateOf(false) }

    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var menuContextPlaylistName by remember { mutableStateOf<String?>(null) }
    var menuContextIsHistory by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var editingPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var deletePlaylistTarget by remember { mutableStateOf<UserPlaylist?>(null) }
    var exportTarget by remember { mutableStateOf<Pair<String, List<Track>>?>(null) }

    // Gently ask for notification permission on Android 13+ when playback begins
    var askedNotifPermission by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    fun ensurePlaybackNotificationPermission() {
        if (!askedNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askedNotifPermission = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun playTracksGuarded(tracks: List<Track>, index: Int, radioMode: Boolean, sourceLabel: String) {
        ensurePlaybackNotificationPermission()
        mainVm.playTracks(tracks, index, radioMode, sourceLabel)
    }

    fun shufflePlayGuarded(tracks: List<Track>, sourceLabel: String) {
        if (tracks.isEmpty()) return
        val shuffled = tracks.shuffled()
        playTracksGuarded(shuffled, 0, false, sourceLabel)
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(regionCode) {
        homeVm.refreshRegionIfNeeded(regionCode)
    }

    LaunchedEffect(mainVm) {
        mainVm.messages.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Back button closes full-screen player first
    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }

    val openTrackMenu: (Track, String?, Boolean) -> Unit = { track, plName, isHist ->
        menuTrack = track
        menuContextPlaylistName = plName
        menuContextIsHistory = isHist
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseBackground)
    ) {
        // Signature Pulse ambient radial glow backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PulsePrimary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PulseSecondary.copy(alpha = 0.06f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 760.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                if (isWide) {
                    PulseWideSidebar(
                        currentRoute = currentRoute,
                        downloadedCount = downloadedTracks.size,
                        likedCount = likedTracks.size,
                        playlists = userPlaylists,
                        onNavigateTab = { navigateTopTab(it) },
                        onOpenDownloads = { navController.navigate(PulseRoutes.DOWNLOADS) },
                        onOpenLiked = { navController.navigate(PulseRoutes.LIKED) },
                        onOpenPlaylist = { name -> navController.navigate(PulseRoutes.playlist(name)) },
                        onNewPlaylist = { showNewPlaylistDialog = true }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = PulseRoutes.HOME,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(PulseRoutes.HOME) {
                            HomeScreen(
                                state = homeState,
                                downloadStates = downloadStates,
                                onPlayTrack = { tracks, idx, radio, src ->
                                    playTracksGuarded(tracks, idx, radio, src)
                                },
                                onOpenTrackMenu = { openTrackMenu(it, null, false) },
                                onOpenArtist = { name -> navController.navigate(PulseRoutes.artist(name)) },
                                onOpenRemotePlaylist = { pl -> navController.navigate(PulseRoutes.remotePlaylist(pl)) },
                                onOpenRecent = { navController.navigate(PulseRoutes.RECENT) },
                                onOpenMood = { idx -> navController.navigate(PulseRoutes.mood(idx)) },
                                onOpenLanguage = { lang -> navController.navigate(PulseRoutes.language(lang)) },
                                onOpenCountryPicker = { showCountrySheet = true },
                                onSurpriseMe = {
                                    homeVm.surpriseMe { tracks, idx, label ->
                                        playTracksGuarded(tracks, idx, true, label)
                                    }
                                },
                                onRefresh = { homeVm.loadHome(forceRefresh = true) }
                            )
                        }

                        composable(PulseRoutes.SEARCH) {
                            SearchScreen(
                                state = searchState,
                                recentSearches = recentSearches,
                                playbackState = playbackState,
                                likedIds = likedIds,
                                downloadStates = downloadStates,
                                onQueryChange = searchVm::onQueryChanged,
                                onSubmitSearch = { q ->
                                    searchVm.submitSearch(q) { directTrack ->
                                        playTracksGuarded(listOf(directTrack), 0, true, "YouTube link")
                                    }
                                },
                                onSelectTab = searchVm::selectTab,
                                onRemoveRecent = searchVm::removeRecentSearch,
                                onClearRecents = searchVm::clearRecentSearches,
                                onRetry = searchVm::retry,
                                onPlayTracks = ::playTracksGuarded,
                                onToggleLike = mainVm::toggleLike,
                                onDownloadTrack = mainVm::downloadTrack,
                                onCancelDownload = mainVm::cancelDownload,
                                onOpenTrackMenu = { openTrackMenu(it, null, false) },
                                onOpenArtist = { name -> navController.navigate(PulseRoutes.artist(name)) },
                                onOpenRemotePlaylist = { pl -> navController.navigate(PulseRoutes.remotePlaylist(pl)) }
                            )
                        }

                        composable(PulseRoutes.LIBRARY) {
                            LibraryScreen(
                                likedCount = likedTracks.size,
                                downloadedTracks = downloadedTracks,
                                downloadStates = downloadStates,
                                userPlaylists = userPlaylists,
                                onOpenDownloads = { navController.navigate(PulseRoutes.DOWNLOADS) },
                                onOpenLiked = { navController.navigate(PulseRoutes.LIKED) },
                                onOpenPlaylist = { name -> navController.navigate(PulseRoutes.playlist(name)) },
                                onNewPlaylist = { showNewPlaylistDialog = true },
                                onImportPlaylist = {
                                    mainVm.clearImportState()
                                    showImportSheet = true
                                }
                            )
                        }

                        composable(PulseRoutes.SETTINGS) {
                            SettingsScreen(
                                autoplay = autoplay,
                                audioQuality = audioQuality,
                                downloadQuality = downloadQuality,
                                regionCode = regionCode,
                                reduceMotion = reduceMotion,
                                customPipedUrl = customPipedUrl,
                                downloadedTracks = downloadedTracks,
                                likedTracks = likedTracks,
                                userPlaylists = userPlaylists,
                                onToggleAutoplay = mainVm::setAutoplay,
                                onSelectAudioQuality = mainVm::setAudioQuality,
                                onSelectDownloadQuality = mainVm::setDownloadQuality,
                                onOpenCountryPicker = { showCountrySheet = true },
                                onToggleReduceMotion = mainVm::setReduceMotion,
                                onOpenSleepTimer = { showSleepTimerSheet = true },
                                onOpenImport = {
                                    mainVm.clearImportState()
                                    showImportSheet = true
                                },
                                onExportPlaylist = { name, tracks -> exportTarget = name to tracks },
                                onBackupExport = { mainVm.shareBackupExport(context) },
                                onRestoreBackupJson = mainVm::restoreBackupJson,
                                onClearHistory = mainVm::clearHistory,
                                onClearRecentSearches = mainVm::clearRecentSearches,
                                onClearDownloads = mainVm::clearAllDownloads,
                                onSetCustomServer = mainVm::setCustomPipedUrl,
                                onResetPulse = mainVm::resetAllData
                            )
                        }

                        composable(PulseRoutes.DOWNLOADS) {
                            DownloadsScreen(
                                downloadedTracks = downloadedTracks,
                                downloadStates = downloadStates,
                                playbackState = playbackState,
                                likedIds = likedIds,
                                onBack = { navController.popBackStack() },
                                onPlayTracks = ::playTracksGuarded,
                                onShufflePlay = { tracks -> shufflePlayGuarded(tracks, "Downloads") },
                                onToggleLike = mainVm::toggleLike,
                                onCancelDownload = mainVm::cancelDownload,
                                onRemoveDownload = mainVm::removeDownload,
                                onClearAllDownloads = mainVm::clearAllDownloads,
                                onOpenTrackMenu = { openTrackMenu(it, null, false) }
                            )
                        }

                        composable(PulseRoutes.LIKED) {
                            LocalPlaylistScreen(
                                title = "Liked Songs",
                                subtitleTag = "COLLECTION",
                                tracks = likedTracks,
                                playlistObj = null,
                                isLikedCollection = true,
                                isRecentCollection = false,
                                playbackState = playbackState,
                                likedIds = likedIds,
                                downloadStates = downloadStates,
                                onBack = { navController.popBackStack() },
                                onPlayTracks = ::playTracksGuarded,
                                onShufflePlay = { shufflePlayGuarded(it, "Liked Songs") },
                                onDownloadAll = mainVm::downloadAll,
                                onEditPlaylist = null,
                                onExportPlaylist = { exportTarget = "Liked Songs" to likedTracks },
                                onClearRecent = null,
                                onRemoveRecentTrack = null,
                                onToggleLike = mainVm::toggleLike,
                                onDownloadTrack = mainVm::downloadTrack,
                                onCancelDownload = mainVm::cancelDownload,
                                onOpenTrackMenu = { openTrackMenu(it, null, false) }
                            )
                        }

                        composable(PulseRoutes.RECENT) {
                            LocalPlaylistScreen(
                                title = "Recently played",
                                subtitleTag = "HISTORY",
                                tracks = historyTracks,
                                playlistObj = null,
                                isLikedCollection = false,
                                isRecentCollection = true,
                                playbackState = playbackState,
                                likedIds = likedIds,
                                downloadStates = downloadStates,
                                onBack = { navController.popBackStack() },
                                onPlayTracks = ::playTracksGuarded,
                                onShufflePlay = { shufflePlayGuarded(it, "Recently played") },
                                onDownloadAll = mainVm::downloadAll,
                                onEditPlaylist = null,
                                onExportPlaylist = null,
                                onClearRecent = { confirmClearHistoryDialog = true },
                                onRemoveRecentTrack = mainVm::removeHistoryTrack,
                                onToggleLike = mainVm::toggleLike,
                                onDownloadTrack = mainVm::downloadTrack,
                                onCancelDownload = mainVm::cancelDownload,
                                onOpenTrackMenu = { openTrackMenu(it, null, true) }
                            )
                        }

                        composable(
                            route = PulseRoutes.PLAYLIST,
                            arguments = listOf(navArgument("name") { type = NavType.StringType })
                        ) { entry ->
                            val rawName = Uri.decode(entry.arguments?.getString("name").orEmpty())
                            val pl = userPlaylists.find { it.name == rawName }
                            if (pl == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Playlist not found", color = PulseTextMuted)
                                }
                            } else {
                                LocalPlaylistScreen(
                                    title = pl.name,
                                    subtitleTag = "PLAYLIST",
                                    tracks = pl.tracks,
                                    playlistObj = pl,
                                    isLikedCollection = false,
                                    isRecentCollection = false,
                                    playbackState = playbackState,
                                    likedIds = likedIds,
                                    downloadStates = downloadStates,
                                    onBack = { navController.popBackStack() },
                                    onPlayTracks = ::playTracksGuarded,
                                    onShufflePlay = { shufflePlayGuarded(it, pl.name) },
                                    onDownloadAll = mainVm::downloadAll,
                                    onEditPlaylist = { editingPlaylist = pl },
                                    onExportPlaylist = { exportTarget = pl.name to pl.tracks },
                                    onClearRecent = null,
                                    onRemoveRecentTrack = null,
                                    onToggleLike = mainVm::toggleLike,
                                    onDownloadTrack = mainVm::downloadTrack,
                                    onCancelDownload = mainVm::cancelDownload,
                                    onOpenTrackMenu = { openTrackMenu(it, pl.name, false) }
                                )
                            }
                        }

                        composable(
                            route = PulseRoutes.ARTIST,
                            arguments = listOf(navArgument("name") { type = NavType.StringType })
                        ) { entry ->
                            val name = Uri.decode(entry.arguments?.getString("name").orEmpty())
                            LaunchedEffect(name) { detailVm.loadArtist(name) }
                            ArtistDetailScreen(
                                state = artistState,
                                playbackState = playbackState,
                                likedIds = likedIds,
                                downloadStates = downloadStates,
                                onBack = { navController.popBackStack() },
                                onRetry = { detailVm.loadArtist(name) },
                                onPlayTracks = ::playTracksGuarded,
                                onShufflePlay = { shufflePlayGuarded(it, artistState.artistName) },
                                onDownloadAll = mainVm::downloadAll,
                                onShowMoreSongs = detailVm::showMoreArtistSongs,
                                onShowLessSongs = detailVm::showLessArtistSongs,
                                onOpenRemotePlaylist = { pl -> navController.navigate(PulseRoutes.remotePlaylist(pl)) },
                                onOpenArtist = { other -> navController.navigate(PulseRoutes.artist(other)) },
                                onToggleLike = mainVm::toggleLike,
                                onDownloadTrack = mainVm::downloadTrack,
                                onCancelDownload = mainVm::cancelDownload,
                                onOpenTrackMenu = { openTrackMenu(it, null, false) }
                            )
                        }

                        composable(
                            route = PulseRoutes.MOOD,
                            arguments = listOf(navArgument("index") { type = NavType.IntType })
                        ) { entry ->
                            val idx = entry.arguments?.getInt("index") ?: 0
                            LaunchedEffect(idx) { detailVm.loadMood(idx) }
                            RemoteCollectionScreen(
                                state = collectionState,
                                playbackState = playbackState,
                                likedIds = likedIds,
                                downloadStates = downloadStates,
                                onBack = { navController.popBackStack() },
                                onRetry = { detailVm.loadMood(idx) },
                                onPlayTracks = ::playTracksGuarded,
                                onShufflePlay = { shufflePlayGuarded(it, collectionState.title) },
                                onDownloadAll = mainVm::downloadAll,
                                onOpenRemotePlaylist = { pl -> navController.navigate(PulseRoutes.remotePlaylist(pl)) },
                                onToggleLike = mainVm::toggleLike,
                                onDownloadTrack = mainVm::downloadTrack,
                                onCancelDownload = mainVm::cancelDownload,
                                onOpenTrackMenu = { openTrackMenu(it, null, false) }
                            )
                        }

                        composable(
                            route = PulseRoutes.LANGUAGE,
                            arguments = listOf(navArgument("name") { type = NavType.StringType })
                        ) { entry ->
                            val lang = Uri.decode(entry.arguments?.getString("name").orEmpty())
                            LaunchedEffect(lang) { detailVm.loadLanguage(lang) }
                            RemoteCollectionScreen(
                                state = collectionState,
                                playbackState = playbackState,
                                likedIds = likedIds,
                                downloadStates = downloadStates,
                                onBack = { navController.popBackStack() },
                                onRetry = { detailVm.loadLanguage(lang) },
                                onPlayTracks = ::playTracksGuarded,
                                onShufflePlay = { shufflePlayGuarded(it, collectionState.title) },
                                onDownloadAll = mainVm::downloadAll,
                                onOpenRemotePlaylist = { pl -> navController.navigate(PulseRoutes.remotePlaylist(pl)) },
                                onToggleLike = mainVm::toggleLike,
                                onDownloadTrack = mainVm::downloadTrack,
                                onCancelDownload = mainVm::cancelDownload,
                                onOpenTrackMenu = { openTrackMenu(it, null, false) }
                            )
                        }

                        composable(
                            route = PulseRoutes.REMOTE_PLAYLIST,
                            arguments = listOf(
                                navArgument("id") { type = NavType.StringType; defaultValue = "" },
                                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                                navArgument("by") { type = NavType.StringType; defaultValue = "" }
                            )
                        ) { entry ->
                            val id = Uri.decode(entry.arguments?.getString("id").orEmpty())
                            val title = Uri.decode(entry.arguments?.getString("title").orEmpty())
                            val by = Uri.decode(entry.arguments?.getString("by").orEmpty())
                            LaunchedEffect(id, title, by) { detailVm.loadRemotePlaylist(id, title, by) }
                            RemoteCollectionScreen(
                                state = collectionState,
                                playbackState = playbackState,
                                likedIds = likedIds,
                                downloadStates = downloadStates,
                                onBack = { navController.popBackStack() },
                                onRetry = { detailVm.loadRemotePlaylist(id, title, by) },
                                onPlayTracks = ::playTracksGuarded,
                                onShufflePlay = { shufflePlayGuarded(it, collectionState.title) },
                                onDownloadAll = mainVm::downloadAll,
                                onOpenRemotePlaylist = { pl -> navController.navigate(PulseRoutes.remotePlaylist(pl)) },
                                onToggleLike = mainVm::toggleLike,
                                onDownloadTrack = mainVm::downloadTrack,
                                onCancelDownload = mainVm::cancelDownload,
                                onOpenTrackMenu = { openTrackMenu(it, null, false) }
                            )
                        }
                    }

                    // Bottom Dock: Snackbar + MiniPlayer + Compact Bottom Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = PulseSurface2,
                                contentColor = PulseTextPrimary,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        MiniPlayerBar(
                            playbackState = playbackState,
                            isLiked = playbackState.currentTrack?.id in likedIds,
                            downloadState = playbackState.currentTrack?.let { downloadStates[it.id] },
                            onExpand = { if (playbackState.currentTrack != null) isPlayerExpanded = true },
                            onTogglePlayPause = mainVm::togglePlayPause,
                            onNext = mainVm::next,
                            onPrev = mainVm::prev,
                            onToggleLike = { playbackState.currentTrack?.let(mainVm::toggleLike) },
                            onOpenQueue = { showQueueSheet = true }
                        )

                        if (!isWide) {
                            PulseBottomNavBar(
                                currentRoute = currentRoute,
                                onSelectTab = ::navigateTopTab
                            )
                        }
                    }
                }
            }
        }

        // Full-screen Now Playing overlay
        AnimatedVisibility(
            visible = isPlayerExpanded && playbackState.currentTrack != null,
            enter = if (reduceMotion) fadeIn() else slideInVertically { it / 2 } + fadeIn(),
            exit = if (reduceMotion) fadeOut() else slideOutVertically { it / 2 } + fadeOut()
        ) {
            val current = playbackState.currentTrack
            FullScreenPlayerOverlay(
                playbackState = playbackState,
                isLiked = current?.id in likedIds,
                autoplayEnabled = autoplay,
                downloadState = current?.let { downloadStates[it.id] },
                onClose = { isPlayerExpanded = false },
                onTogglePlayPause = mainVm::togglePlayPause,
                onNext = mainVm::next,
                onPrev = mainVm::prev,
                onSeekTo = mainVm::seekTo,
                onToggleShuffle = mainVm::toggleShuffle,
                onCycleRepeat = mainVm::cycleRepeatMode,
                onToggleLike = { current?.let(mainVm::toggleLike) },
                onDownloadClick = mainVm::downloadTrack,
                onCancelDownload = mainVm::cancelDownload,
                onToggleAutoplay = { mainVm.setAutoplay(!autoplay) },
                onOpenSleepTimer = { showSleepTimerSheet = true },
                onOpenQueue = { showQueueSheet = true },
                onOpenMore = { tr -> openTrackMenu(tr, null, false) }
            )
        }
    }

    // Modal Bottom Sheets & Dialogs
    if (showQueueSheet) {
        QueueBottomSheet(
            playbackState = playbackState,
            downloadStates = downloadStates,
            onDismiss = { showQueueSheet = false },
            onJumpToIndex = mainVm::jumpToQueueIndex,
            onRemoveAt = mainVm::removeFromQueue,
            onClearUpcoming = mainVm::clearUpcoming,
            onSaveAsPlaylist = mainVm::saveQueueAsPlaylist
        )
    }

    menuTrack?.let { tr ->
        val plName = menuContextPlaylistName
        val isHist = menuContextIsHistory
        TrackMenuBottomSheet(
            track = tr,
            isLiked = tr.id in likedIds,
            downloadState = downloadStates[tr.id],
            canRemoveFromPlaylist = plName != null,
            canRemoveFromHistory = isHist,
            onDismiss = { menuTrack = null },
            onPlayNext = { mainVm.enqueue(tr, playNext = true) },
            onAddToQueue = { mainVm.enqueue(tr, playNext = false) },
            onAddToPlaylist = { addToPlaylistTrack = tr },
            onDownload = { mainVm.downloadTrack(tr) },
            onCancelDownload = { mainVm.cancelDownload(tr.id) },
            onRemoveDownload = { mainVm.removeDownload(tr.id) },
            onToggleLike = { mainVm.toggleLike(tr) },
            onStartRadio = { playTracksGuarded(listOf(tr), 0, true, "Radio · ${tr.title}") },
            onGoToArtist = {
                isPlayerExpanded = false
                if (tr.artist.isNotBlank()) navController.navigate(PulseRoutes.artist(tr.artist))
            },
            onRemoveFromPlaylist = {
                if (plName != null) mainVm.removeTrackFromPlaylist(plName, tr.id)
            },
            onRemoveFromHistory = { mainVm.removeHistoryTrack(tr.id) },
            onToast = mainVm::toast
        )
    }

    addToPlaylistTrack?.let { tr ->
        AddToPlaylistSheet(
            track = tr,
            playlists = userPlaylists,
            onDismiss = { addToPlaylistTrack = null },
            onSelectPlaylist = { plName -> mainVm.addTrackToPlaylist(plName, tr) },
            onCreateAndAdd = { newName ->
                mainVm.createPlaylist(newName) { created ->
                    mainVm.addTrackToPlaylist(created, tr)
                }
            }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(
            playbackState = playbackState,
            onDismiss = { showSleepTimerSheet = false },
            onStartTimer = mainVm::startSleepTimer,
            onClearTimer = mainVm::clearSleepTimer
        )
    }

    if (showCountrySheet) {
        CountryPickerSheet(
            currentRegion = regionCode,
            isOverride = isRegionOverride,
            onDismiss = { showCountrySheet = false },
            onSelectRegion = mainVm::setRegion
        )
    }

    if (showImportSheet) {
        ImportPlaylistSheet(
            preview = importPreview,
            isBusy = importBusy,
            errorMessage = importError,
            onDismiss = { showImportSheet = false },
            onFetchUrl = mainVm::startUrlImport,
            onFilePicked = mainVm::startFileImport,
            onNameChange = mainVm::updateImportName,
            onToggleCandidate = mainVm::toggleImportCandidate,
            onBackToStart = mainVm::clearImportState,
            onCommitImport = {
                mainVm.commitImport { importedName ->
                    showImportSheet = false
                    navController.navigate(PulseRoutes.playlist(importedName))
                }
            }
        )
    }

    exportTarget?.let { (name, tracks) ->
        ExportPlaylistSheet(
            playlistName = name,
            trackCount = tracks.size,
            onDismiss = { exportTarget = null },
            onSelectFormat = { fmt -> mainVm.sharePlaylistExport(context, name, tracks, fmt) }
        )
    }

    editingPlaylist?.let { pl ->
        EditPlaylistSheet(
            playlist = pl,
            onDismiss = { editingPlaylist = null },
            onSave = { newName, customCover, clearCover ->
                mainVm.updatePlaylist(pl.name, newName, customCover, clearCover)
                if (newName.trim().isNotBlank() && newName.trim() != pl.name) {
                    navController.popBackStack()
                    navController.navigate(PulseRoutes.playlist(newName.trim()))
                }
            },
            onDelete = { deletePlaylistTarget = pl }
        )
    }

    deletePlaylistTarget?.let { pl ->
        ConfirmActionDialog(
            title = "Delete “${pl.name}”?",
            message = "This removes the playlist from your Library. Downloaded songs inside Downloads stay safe.",
            confirmLabel = "Delete",
            isDanger = true,
            onDismiss = { deletePlaylistTarget = null },
            onConfirm = {
                deletePlaylistTarget = null
                mainVm.deletePlaylist(pl.name)
                navController.popBackStack()
            }
        )
    }

    if (showNewPlaylistDialog) {
        TextPromptDialog(
            title = "New playlist",
            initialValue = "",
            placeholder = "Playlist name",
            confirmLabel = "Create",
            onDismiss = { showNewPlaylistDialog = false },
            onConfirm = { name ->
                showNewPlaylistDialog = false
                mainVm.createPlaylist(name) { created ->
                    navController.navigate(PulseRoutes.playlist(created))
                }
            }
        )
    }

    if (confirmClearHistoryDialog) {
        ConfirmActionDialog(
            title = "Clear recently played?",
            message = "This clears your listening history on this device.",
            confirmLabel = "Clear",
            isDanger = true,
            onDismiss = { confirmClearHistoryDialog = false },
            onConfirm = {
                confirmClearHistoryDialog = false
                mainVm.clearHistory()
            }
        )
    }
}

@Composable
private fun PulseBottomNavBar(
    currentRoute: String,
    onSelectTab: (String) -> Unit
) {
    val barShape = RoundedCornerShape(999.dp)
    Surface(
        shape = barShape,
        color = Color.Transparent,
        shadowElevation = 20.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        GlassContainer(
            shape = barShape,
            tintColor = PulseSecondary,
            tintAlpha = 0.08f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopNavTabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val pillModifier = if (selected) {
                        Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x47FFFFFF),
                                        PulsePrimary.copy(alpha = 0.24f),
                                        Color(0x18FFFFFF)
                                    )
                                ),
                                shape = RoundedCornerShape(999.dp)
                            )
                    } else {
                        Modifier
                    }
                    Surface(
                        onClick = { onSelectTab(tab.route) },
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Transparent,
                        border = if (selected) BorderStroke(0.9.dp, Color(0x66FFFFFF)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tab.testTag)
                    ) {
                        Column(
                            modifier = pillModifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = if (selected) Color.White else PulseTextMuted,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else PulseTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseWideSidebar(
    currentRoute: String,
    downloadedCount: Int,
    likedCount: Int,
    playlists: List<UserPlaylist>,
    onNavigateTab: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onNewPlaylist: () -> Unit
) {
    GlassContainer(
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(248.dp)
            .fillMaxHeight()
            .padding(top = 8.dp, bottom = 8.dp, start = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PulsePrimaryStrong,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("Pulse", style = MaterialTheme.typography.headlineMedium, color = PulseTextPrimary)
            }

            Spacer(Modifier.height(10.dp))

            TopNavTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) PulsePrimaryContainer else Color.Transparent)
                        .clickable { onNavigateTab(tab.route) }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("sidebar_${tab.testTag}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = if (selected) PulsePrimary else PulseTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) PulsePrimary else PulseTextPrimary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = PulseGlassBorder)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("LIBRARY", style = MaterialTheme.typography.labelSmall, color = PulseTextMuted)
                IconButton(onClick = onNewPlaylist, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "New playlist", tint = PulseTextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (currentRoute == PulseRoutes.DOWNLOADS) PulsePrimaryContainer else Color.Transparent)
                            .clickable(onClick = onOpenDownloads)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PulseEmerald.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = PulseEmerald, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Downloads", style = MaterialTheme.typography.titleSmall, color = PulseTextPrimary)
                            Text("$downloadedCount offline", style = MaterialTheme.typography.labelSmall, color = PulseEmerald)
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (currentRoute == PulseRoutes.LIKED) PulsePrimaryContainer else Color.Transparent)
                            .clickable(onClick = onOpenLiked)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PulsePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = PulsePrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Liked Songs", style = MaterialTheme.typography.titleSmall, color = PulseTextPrimary)
                            Text("$likedCount songs", style = MaterialTheme.typography.labelSmall, color = PulseTextMuted)
                        }
                    }
                }
                items(playlists, key = { it.name }) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenPlaylist(pl.name) }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistCollageCover(playlist = pl, modifier = Modifier.size(34.dp), cornerRadius = 8.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pl.name, style = MaterialTheme.typography.titleSmall, color = PulseTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${pl.tracks.size} songs", style = MaterialTheme.typography.labelSmall, color = PulseTextMuted)
                        }
                    }
                }
            }
        }
    }
}
