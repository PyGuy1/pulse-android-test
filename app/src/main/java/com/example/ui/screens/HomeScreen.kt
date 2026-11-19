package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadStateInfo
import com.example.data.model.PlaylistSummary
import com.example.data.model.Track
import com.example.data.remote.MusicConstants
import com.example.ui.components.ArtistShelfCard
import com.example.ui.components.PlaylistShelfCard
import com.example.ui.components.SectionRetryBanner
import com.example.ui.components.SkeletonShelfCards
import com.example.ui.components.TrackShelfCard
import com.example.ui.components.artistHueColor
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.viewmodel.HomeUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    downloadStates: Map<String, DownloadStateInfo>,
    onPlayTrack: (List<Track>, Int, Boolean, String) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenRemotePlaylist: (PlaylistSummary) -> Unit,
    onOpenRecent: () -> Unit,
    onOpenMood: (Int) -> Unit,
    onOpenLanguage: (String) -> Unit,
    onOpenCountryPicker: () -> Unit,
    onSurpriseMe: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 180.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PulsePrimaryStrong,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Pulse",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PulseTextPrimary
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.displaySmall,
                    color = PulseTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PulseTextMuted
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSurpriseMe,
                        enabled = !state.isSurpriseLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulsePrimaryStrong,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("home_surprise_btn")
                    ) {
                        if (state.isSurpriseLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Surprise me")
                    }
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulseSurface2,
                            contentColor = PulseTextPrimary
                        ),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("home_refresh_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh")
                    }
                }
            }
        }

        if (state.recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently played",
                    trailing = {
                        Surface(
                            onClick = onOpenRecent,
                            shape = CircleShape,
                            color = PulseSurface2,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "See all recently played",
                                    tint = PulseTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
                TrackShelfRow(
                    tracks = state.recentlyPlayed,
                    downloadStates = downloadStates,
                    onClickTrack = { idx -> onPlayTrack(state.recentlyPlayed, idx, true, "Recently played") },
                    onMoreClick = onOpenTrackMenu
                )
            }
        }

        item {
            SectionHeader(title = if (state.userTopArtists.isNotEmpty()) "Suggested for you" else "Popular right now")
            when {
                state.forYou.isLoading -> Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
                state.forYou.error != null -> Box(Modifier.padding(horizontal = 18.dp)) {
                    SectionRetryBanner(state.forYou.error, onRefresh)
                }
                else -> TrackShelfRow(
                    tracks = state.forYou.items,
                    downloadStates = downloadStates,
                    onClickTrack = { idx -> onPlayTrack(state.forYou.items, idx, true, "Suggested for you") },
                    onMoreClick = onOpenTrackMenu
                )
            }
        }

        if (state.lastPlayedTitle != null) {
            item {
                SectionHeader(title = "Because you played ${state.lastPlayedTitle}")
                when {
                    state.becauseYouPlayed.isLoading -> Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
                    state.becauseYouPlayed.error != null -> Box(Modifier.padding(horizontal = 18.dp)) {
                        SectionRetryBanner(state.becauseYouPlayed.error, onRefresh)
                    }
                    else -> TrackShelfRow(
                        tracks = state.becauseYouPlayed.items,
                        downloadStates = downloadStates,
                        onClickTrack = { idx -> onPlayTrack(state.becauseYouPlayed.items, idx, true, "Because you played") },
                        onMoreClick = onOpenTrackMenu
                    )
                }
            }
        }

        item {
            val isGlobal = state.regionCode == "WW"
            SectionHeader(title = if (isGlobal) "Top playlists" else "Top playlists in ${state.regionName}")
            when {
                state.topPlaylists.isLoading -> Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
                state.topPlaylists.items.isNotEmpty() -> PlaylistShelfRow(
                    playlists = state.topPlaylists.items,
                    onClick = onOpenRemotePlaylist
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Top in ", style = MaterialTheme.typography.titleLarge, color = PulseTextPrimary)
                Surface(
                    onClick = onOpenCountryPicker,
                    shape = RoundedCornerShape(10.dp),
                    color = PulseSurface2,
                    border = BorderStroke(1.dp, PulseGlassBorder),
                    modifier = Modifier.testTag("home_country_picker_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.regionName, style = MaterialTheme.typography.titleMedium, color = PulseTextPrimary)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Change country", tint = PulseTextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
            when {
                state.chartTracks.isLoading -> Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
                state.chartTracks.error != null -> Box(Modifier.padding(horizontal = 18.dp)) {
                    SectionRetryBanner(state.chartTracks.error, onRefresh)
                }
                else -> TrackShelfRow(
                    tracks = state.chartTracks.items,
                    downloadStates = downloadStates,
                    onClickTrack = { idx -> onPlayTrack(state.chartTracks.items, idx, true, "Top in ${state.regionName}") },
                    onMoreClick = onOpenTrackMenu
                )
            }
        }

        if (state.hitsPlaylists.isLoading || state.hitsPlaylists.items.isNotEmpty()) {
            item {
                SectionHeader(title = "Hits")
                if (state.hitsPlaylists.isLoading) {
                    Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
                } else {
                    PlaylistShelfRow(state.hitsPlaylists.items, onOpenRemotePlaylist)
                }
            }
        }

        if (state.bestOfPlaylists.isLoading || state.bestOfPlaylists.items.isNotEmpty()) {
            item {
                SectionHeader(title = "Best of")
                if (state.bestOfPlaylists.isLoading) {
                    Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
                } else {
                    PlaylistShelfRow(state.bestOfPlaylists.items, onOpenRemotePlaylist)
                }
            }
        }

        item {
            val isGlobal = state.regionCode == "WW"
            SectionHeader(title = if (isGlobal) "Music by language" else "Languages in ${state.regionName}")
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.languages.forEach { lang ->
                    Box(
                        modifier = Modifier
                            .width(154.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(artistHueColor(lang), artistHueColor(lang + "2").copy(alpha = 0.85f))
                                )
                            )
                            .clickable { onOpenLanguage(lang) }
                            .padding(12.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(lang, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Famous artists")
            if (state.famousArtists.isLoading) {
                Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
            } else {
                ArtistShelfRow(state.famousArtists.items, state.artistAvatars, onOpenArtist)
            }
        }

        if (state.popularArtists.isLoading || state.popularArtists.items.isNotEmpty()) {
            item {
                SectionHeader(title = "Popular artists in ${state.regionName}")
                if (state.popularArtists.isLoading) {
                    Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
                } else {
                    ArtistShelfRow(state.popularArtists.items, state.artistAvatars, onOpenArtist)
                }
            }
        }

        if (state.userTopArtists.isNotEmpty()) {
            item {
                SectionHeader(title = "Your top artists")
                ArtistShelfRow(state.userTopArtists, state.artistAvatars, onOpenArtist)
            }
        }

        item {
            SectionHeader(title = "Browse by mood & genre")
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MusicConstants.MOODS.forEachIndexed { idx, mood ->
                    Box(
                        modifier = Modifier
                            .width(154.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(mood.colorHex))
                            .clickable { onOpenMood(idx) }
                            .padding(12.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(mood.label, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state.newReleases.isLoading || state.newReleases.items.isNotEmpty()) {
            item {
                SectionHeader(title = "New releases")
                if (state.newReleases.isLoading) {
                    Box(Modifier.padding(horizontal = 12.dp)) { SkeletonShelfCards() }
                } else {
                    PlaylistShelfRow(state.newReleases.items, onOpenRemotePlaylist)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = PulseTextPrimary)
        trailing?.invoke()
    }
}

@Composable
private fun TrackShelfRow(
    tracks: List<Track>,
    downloadStates: Map<String, DownloadStateInfo>,
    onClickTrack: (Int) -> Unit,
    onMoreClick: (Track) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(tracks) { idx, t ->
            TrackShelfCard(
                track = t,
                downloadState = downloadStates[t.id],
                onClick = { onClickTrack(idx) },
                onMoreClick = { onMoreClick(t) }
            )
        }
    }
}

@Composable
private fun PlaylistShelfRow(
    playlists: List<PlaylistSummary>,
    onClick: (PlaylistSummary) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(playlists) { pl ->
            PlaylistShelfCard(playlist = pl, onClick = { onClick(pl) })
        }
    }
}

@Composable
private fun ArtistShelfRow(
    names: List<String>,
    avatars: Map<String, String>,
    onClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(names) { name ->
            ArtistShelfCard(
                name = name,
                avatarUrl = avatars[name],
                onClick = { onClick(name) }
            )
        }
    }
}
