package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadStateInfo
import com.example.data.model.PlaylistSummary
import com.example.data.model.Track
import com.example.service.PlaybackUiState
import com.example.ui.components.ArtistShelfCard
import com.example.ui.components.PlaylistShelfCard
import com.example.ui.components.SectionRetryBanner
import com.example.ui.components.SkeletonTrackRows
import com.example.ui.components.TrackRow
import com.example.ui.theme.PulseBackground
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.viewmodel.SearchTab
import com.example.ui.viewmodel.SearchUiState

@Composable
fun SearchScreen(
    state: SearchUiState,
    recentSearches: List<String>,
    playbackState: PlaybackUiState,
    likedIds: Set<String>,
    downloadStates: Map<String, DownloadStateInfo>,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String?) -> Unit,
    onSelectTab: (SearchTab) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
    onRetry: () -> Unit,
    onPlayTracks: (List<Track>, Int, Boolean, String) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onCancelDownload: (String) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenRemotePlaylist: (PlaylistSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Songs, artists, albums, playlists") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PulseTextMuted) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = PulseTextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                onSubmitSearch(null)
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PulsePrimary,
                unfocusedBorderColor = PulseGlassBorder,
                focusedContainerColor = PulseSurface2,
                unfocusedContainerColor = PulseSurface2
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_input")
        )

        Spacer(Modifier.height(10.dp))

        // Autocomplete Suggestions
        if (state.suggestions.isNotEmpty() && state.query.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PulseSurface2)
            ) {
                state.suggestions.forEach { sug ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                keyboard?.hide()
                                onSubmitSearch(sug)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = PulseTextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(sug, style = MaterialTheme.typography.bodyLarge, color = PulseTextPrimary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Category Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchTab.entries) { tab ->
                val selected = tab == state.selectedTab
                Surface(
                    onClick = { onSelectTab(tab) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) Color.White else PulseSurface2,
                    border = BorderStroke(1.dp, if (selected) Color.White else PulseGlassBorder)
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) PulseBackground else PulseTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.query.isBlank() && state.committedQuery.isBlank() -> {
                if (recentSearches.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = 180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Search for any song, artist, album, or paste a YouTube link.", color = PulseTextMuted)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp)) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = onClearRecents) {
                                    Text("Clear all", color = PulseTextMuted)
                                }
                            }
                        }
                        items(recentSearches) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        keyboard?.hide()
                                        onSubmitSearch(item)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, tint = PulseTextMuted, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(item, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onRemoveRecent(item) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = PulseTextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            state.isLoading -> {
                Box(Modifier.padding(horizontal = 10.dp)) {
                    SkeletonTrackRows(7)
                }
            }

            state.errorMessage != null -> {
                Box(Modifier.padding(horizontal = 18.dp)) {
                    SectionRetryBanner(state.errorMessage, onRetry)
                }
            }

            state.selectedTab == SearchTab.ARTISTS -> {
                if (state.artists.isEmpty()) {
                    EmptySearchMessage("No artists found.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(132.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 180.dp)
                    ) {
                        items(state.artists) { artist ->
                            ArtistShelfCard(
                                name = artist.name,
                                avatarUrl = state.artistAvatars[artist.name] ?: artist.avatarUrl,
                                onClick = { onOpenArtist(artist.name) }
                            )
                        }
                    }
                }
            }

            state.selectedTab == SearchTab.ALBUMS -> {
                if (state.albums.isEmpty()) {
                    EmptySearchMessage("No albums found.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(146.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 180.dp)
                    ) {
                        items(state.albums) { alb ->
                            PlaylistShelfCard(alb, onClick = { onOpenRemotePlaylist(alb) })
                        }
                    }
                }
            }

            state.selectedTab == SearchTab.PLAYLISTS -> {
                if (state.playlists.isEmpty()) {
                    EmptySearchMessage("No playlists found.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(146.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 180.dp)
                    ) {
                        items(state.playlists) { pl ->
                            PlaylistShelfCard(pl, onClick = { onOpenRemotePlaylist(pl) })
                        }
                    }
                }
            }

            else -> {
                val showSongs = if (state.selectedTab == SearchTab.ALL) state.songs.take(6) else state.songs
                LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
                    if (showSongs.isNotEmpty()) {
                        if (state.selectedTab == SearchTab.ALL) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Songs", style = MaterialTheme.typography.titleLarge)
                                    if (state.songs.size > 6) {
                                        Surface(
                                            onClick = { onSelectTab(SearchTab.SONGS) },
                                            shape = RoundedCornerShape(999.dp),
                                            color = PulseSurface2,
                                            border = BorderStroke(1.dp, PulseGlassBorder)
                                        ) {
                                            Text("Show all", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                                        }
                                    }
                                }
                            }
                        }
                        itemsIndexed(showSongs) { idx, track ->
                            TrackRow(
                                track = track,
                                isNowPlaying = playbackState.currentTrack?.id == track.id,
                                isActuallyPlaying = playbackState.isPlaying,
                                isLiked = track.id in likedIds,
                                downloadState = downloadStates[track.id],
                                onClick = { onPlayTracks(showSongs, idx, true, "Search · ${state.committedQuery}") },
                                onLikeClick = { onToggleLike(track) },
                                onDownloadClick = { onDownloadTrack(track) },
                                onCancelDownloadClick = { onCancelDownload(track.id) },
                                onMoreClick = { onOpenTrackMenu(track) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    if (state.selectedTab == SearchTab.ALL && state.artists.isNotEmpty()) {
                        item {
                            Text("Artists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 8.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                                items(state.artists.take(10)) { artist ->
                                    ArtistShelfCard(
                                        name = artist.name,
                                        avatarUrl = state.artistAvatars[artist.name] ?: artist.avatarUrl,
                                        onClick = { onOpenArtist(artist.name) }
                                    )
                                }
                            }
                        }
                    }

                    if (state.selectedTab == SearchTab.ALL && state.albums.isNotEmpty()) {
                        item {
                            Text("Albums", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 8.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                                items(state.albums.take(10)) { alb ->
                                    PlaylistShelfCard(alb, onClick = { onOpenRemotePlaylist(alb) })
                                }
                            }
                        }
                    }

                    if (state.selectedTab == SearchTab.ALL && state.playlists.isNotEmpty()) {
                        item {
                            Text("Playlists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 8.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                                items(state.playlists.take(10)) { pl ->
                                    PlaylistShelfCard(pl, onClick = { onOpenRemotePlaylist(pl) })
                                }
                            }
                        }
                    }

                    if (showSongs.isEmpty() && state.artists.isEmpty() && state.albums.isEmpty() && state.playlists.isEmpty()) {
                        item { EmptySearchMessage("Nothing found. Try different words.") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchMessage(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = PulseTextMuted)
    }
}
