package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadStateInfo
import com.example.data.model.PlaylistSummary
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.service.PlaybackUiState
import com.example.ui.components.ArtistAvatar
import com.example.ui.components.ArtistShelfCard
import com.example.ui.components.PlaylistCollageCover
import com.example.ui.components.PlaylistShelfCard
import com.example.ui.components.SectionRetryBanner
import com.example.ui.components.SkeletonShelfCards
import com.example.ui.components.SkeletonTrackRows
import com.example.ui.components.TrackRow
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.viewmodel.ArtistDetailUiState
import com.example.ui.viewmodel.RemoteCollectionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalPlaylistScreen(
    title: String,
    subtitleTag: String,
    tracks: List<Track>,
    playlistObj: UserPlaylist?,
    isLikedCollection: Boolean,
    isRecentCollection: Boolean,
    playbackState: PlaybackUiState,
    likedIds: Set<String>,
    downloadStates: Map<String, DownloadStateInfo>,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int, Boolean, String) -> Unit,
    onShufflePlay: (List<Track>) -> Unit,
    onDownloadAll: (List<Track>) -> Unit,
    onEditPlaylist: (() -> Unit)?,
    onExportPlaylist: (() -> Unit)?,
    onClearRecent: (() -> Unit)?,
    onRemoveRecentTrack: ((String) -> Unit)?,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onCancelDownload: (String) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMinutes = (tracks.sumOf { it.durationSeconds ?: 0 } / 60.0).toInt()
    val durText = if (totalMinutes > 0) {
        if (totalMinutes >= 60) " · ${totalMinutes / 60} hr ${totalMinutes % 60} min" else " · $totalMinutes min"
    } else ""

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PulseTextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!isRecentCollection) {
                        PlaylistCollageCover(
                            playlist = playlistObj,
                            isLikedFolder = isLikedCollection,
                            modifier = Modifier.size(120.dp),
                            cornerRadius = 16.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(subtitleTag, style = MaterialTheme.typography.labelSmall, color = PulseTextMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(title, style = MaterialTheme.typography.headlineLarge, color = PulseTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${tracks.size} song${if (tracks.size == 1) "" else "s"}$durText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PulseTextMuted
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (tracks.isNotEmpty()) onPlayTracks(tracks, 0, false, title) },
                        enabled = tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    Button(
                        onClick = { if (tracks.isNotEmpty()) onShufflePlay(tracks) },
                        enabled = tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                    Button(
                        onClick = { if (tracks.isNotEmpty()) onDownloadAll(tracks) },
                        enabled = tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("playlist_download_all_btn")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download all")
                    }
                    if (onEditPlaylist != null) {
                        Button(
                            onClick = onEditPlaylist,
                            colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                            border = BorderStroke(1.dp, PulseGlassBorder),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit")
                        }
                    }
                    if (onExportPlaylist != null) {
                        Button(
                            onClick = onExportPlaylist,
                            enabled = tracks.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                            border = BorderStroke(1.dp, PulseGlassBorder),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                    }
                    if (onClearRecent != null && tracks.isNotEmpty()) {
                        Button(
                            onClick = onClearRecent,
                            colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                            border = BorderStroke(1.dp, PulseGlassBorder),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear all")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        if (tracks.isEmpty()) {
            item {
                Text(
                    text = if (isRecentCollection) "Nothing played yet." else "Nothing here yet. Like a song or add one from search.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PulseTextMuted,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
                )
            }
        } else {
            itemsIndexed(tracks) { idx, track ->
                TrackRow(
                    track = track,
                    isNowPlaying = playbackState.currentTrack?.id == track.id,
                    isActuallyPlaying = playbackState.isPlaying,
                    isLiked = track.id in likedIds,
                    downloadState = downloadStates[track.id],
                    onClick = { onPlayTracks(tracks, idx, false, title) },
                    onLikeClick = { onToggleLike(track) },
                    onDownloadClick = { onDownloadTrack(track) },
                    onCancelDownloadClick = { onCancelDownload(track.id) },
                    onMoreClick = { onOpenTrackMenu(track) },
                    onRemoveClick = if (onRemoveRecentTrack != null) { { onRemoveRecentTrack(track.id) } } else null,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemoteCollectionScreen(
    state: RemoteCollectionUiState,
    playbackState: PlaybackUiState,
    likedIds: Set<String>,
    downloadStates: Map<String, DownloadStateInfo>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayTracks: (List<Track>, Int, Boolean, String) -> Unit,
    onShufflePlay: (List<Track>) -> Unit,
    onDownloadAll: (List<Track>) -> Unit,
    onOpenRemotePlaylist: (PlaylistSummary) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onCancelDownload: (String) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PulseTextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Text(state.subtitle.ifBlank { "COLLECTION" }, style = MaterialTheme.typography.labelSmall, color = PulseTextMuted)
                Spacer(Modifier.height(4.dp))
                Text(state.title, style = MaterialTheme.typography.headlineLarge, color = PulseTextPrimary)
                if (state.isApproximate) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Showing top songs matching this collection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextMuted
                    )
                }
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (state.tracks.isNotEmpty()) onPlayTracks(state.tracks, 0, false, state.title) },
                        enabled = state.tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    Button(
                        onClick = { if (state.tracks.isNotEmpty()) onShufflePlay(state.tracks) },
                        enabled = state.tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                    Button(
                        onClick = { if (state.tracks.isNotEmpty()) onDownloadAll(state.tracks) },
                        enabled = state.tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download all")
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        if (state.playlists.isNotEmpty()) {
            item {
                Text("Playlists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                    items(state.playlists) { pl ->
                        PlaylistShelfCard(pl, onClick = { onOpenRemotePlaylist(pl) })
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        when {
            state.isLoading -> item { Box(Modifier.padding(horizontal = 10.dp)) { SkeletonTrackRows(8) } }
            state.error != null -> item { Box(Modifier.padding(horizontal = 18.dp)) { SectionRetryBanner(state.error, onRetry) } }
            else -> {
                itemsIndexed(state.tracks) { idx, track ->
                    TrackRow(
                        track = track,
                        isNowPlaying = playbackState.currentTrack?.id == track.id,
                        isActuallyPlaying = playbackState.isPlaying,
                        isLiked = track.id in likedIds,
                        downloadState = downloadStates[track.id],
                        onClick = { onPlayTracks(state.tracks, idx, false, state.title) },
                        onLikeClick = { onToggleLike(track) },
                        onDownloadClick = { onDownloadTrack(track) },
                        onCancelDownloadClick = { onCancelDownload(track.id) },
                        onMoreClick = { onOpenTrackMenu(track) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistDetailScreen(
    state: ArtistDetailUiState,
    playbackState: PlaybackUiState,
    likedIds: Set<String>,
    downloadStates: Map<String, DownloadStateInfo>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayTracks: (List<Track>, Int, Boolean, String) -> Unit,
    onShufflePlay: (List<Track>) -> Unit,
    onDownloadAll: (List<Track>) -> Unit,
    onShowMoreSongs: () -> Unit,
    onShowLessSongs: () -> Unit,
    onOpenRemotePlaylist: (PlaylistSummary) -> Unit,
    onOpenArtist: (String) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onCancelDownload: (String) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleSongs = state.allSongs.take(state.shownCount)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PulseTextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ArtistAvatar(
                        name = state.artistName,
                        avatarUrl = state.avatarUrl,
                        modifier = Modifier.size(106.dp),
                        fontSizeSp = 36
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ARTIST", style = MaterialTheme.typography.labelSmall, color = PulseTextMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(state.artistName, style = MaterialTheme.typography.headlineLarge, color = PulseTextPrimary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (state.allSongs.isNotEmpty()) onPlayTracks(state.allSongs, 0, false, state.artistName) },
                        enabled = state.allSongs.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    Button(
                        onClick = { if (state.allSongs.isNotEmpty()) onShufflePlay(state.allSongs) },
                        enabled = state.allSongs.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                    Button(
                        onClick = { if (visibleSongs.isNotEmpty()) onDownloadAll(visibleSongs) },
                        enabled = visibleSongs.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download")
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Songs", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
            }
        }

        when {
            state.isLoading -> item { Box(Modifier.padding(horizontal = 10.dp)) { SkeletonTrackRows(6) } }
            state.error != null -> item { Box(Modifier.padding(horizontal = 18.dp)) { SectionRetryBanner(state.error, onRetry) } }
            else -> {
                itemsIndexed(visibleSongs) { idx, track ->
                    TrackRow(
                        track = track,
                        isNowPlaying = playbackState.currentTrack?.id == track.id,
                        isActuallyPlaying = playbackState.isPlaying,
                        isLiked = track.id in likedIds,
                        downloadState = downloadStates[track.id],
                        onClick = { onPlayTracks(state.allSongs, idx, false, state.artistName) },
                        onLikeClick = { onToggleLike(track) },
                        onDownloadClick = { onDownloadTrack(track) },
                        onCancelDownloadClick = { onCancelDownload(track.id) },
                        onMoreClick = { onOpenTrackMenu(track) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!state.fullCatalogLoaded || state.shownCount < state.allSongs.size) {
                            Button(
                                onClick = onShowMoreSongs,
                                enabled = !state.isExpandingCatalog,
                                colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                                border = BorderStroke(1.dp, PulseGlassBorder),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                if (state.isExpandingCatalog) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Loading all songs…")
                                } else {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    val rem = if (state.fullCatalogLoaded) " (${state.allSongs.size - state.shownCount} more)" else ""
                                    Text("Show more$rem")
                                }
                            }
                        }
                        if (state.shownCount > 10) {
                            Button(
                                onClick = onShowLessSongs,
                                colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                                border = BorderStroke(1.dp, PulseGlassBorder),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text("Show less")
                            }
                        }
                    }
                }
            }
        }

        if (state.albums.isNotEmpty()) {
            item {
                Text("Albums", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                    items(state.albums) { alb ->
                        PlaylistShelfCard(alb, onClick = { onOpenRemotePlaylist(alb) })
                    }
                }
            }
        }

        if (state.similarArtists.isNotEmpty()) {
            item {
                Text("Fans also like", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                    items(state.similarArtists) { name ->
                        ArtistShelfCard(
                            name = name,
                            avatarUrl = state.artistAvatars[name],
                            onClick = { onOpenArtist(name) }
                        )
                    }
                }
            }
        }
    }
}
