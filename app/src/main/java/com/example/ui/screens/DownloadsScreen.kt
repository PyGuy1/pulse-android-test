package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadStateInfo
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadedTrack
import com.example.data.model.Track
import com.example.data.model.formatBytes
import com.example.service.PlaybackUiState
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PlaylistCollageCover
import com.example.ui.components.TrackRow
import com.example.ui.theme.PulseDanger
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseSurface3
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary

@Composable
fun DownloadsScreen(
    downloadedTracks: List<DownloadedTrack>,
    downloadStates: Map<String, DownloadStateInfo>,
    playbackState: PlaybackUiState,
    likedIds: Set<String>,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int, Boolean, String) -> Unit,
    onShufflePlay: (List<Track>) -> Unit,
    onToggleLike: (Track) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onClearAllDownloads: () -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val activeDownloads = remember(downloadStates) {
        downloadStates.values.filter {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
        }
    }

    val filtered = remember(downloadedTracks, filterQuery) {
        val q = filterQuery.trim().lowercase()
        if (q.isEmpty()) downloadedTracks
        else downloadedTracks.filter {
            it.track.title.lowercase().contains(q) || it.track.artist.lowercase().contains(q)
        }
    }
    val allTracks = remember(filtered) { filtered.map { it.track } }
    val totalBytes = remember(downloadedTracks) { downloadedTracks.sumOf { it.fileSizeBytes } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("downloads_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library", tint = PulseTextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PlaylistCollageCover(
                        isDownloadsFolder = true,
                        modifier = Modifier.size(120.dp),
                        cornerRadius = 18.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DownloadDone, contentDescription = null, tint = PulseEmerald, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "DOWNLOADS · DEVICE DOWNLOAD/PULSE",
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Downloads", style = MaterialTheme.typography.headlineLarge, color = PulseTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${downloadedTracks.size} song${if (downloadedTracks.size == 1) "" else "s"} · ${formatBytes(totalBytes)} · Saved to Download/Pulse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PulseTextMuted
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { if (allTracks.isNotEmpty()) onPlayTracks(allTracks, 0, false, "Downloads") },
                        enabled = allTracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("downloads_play_all_btn")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    Button(
                        onClick = { if (allTracks.isNotEmpty()) onShufflePlay(allTracks) },
                        enabled = allTracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("downloads_shuffle_btn")
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                    if (downloadedTracks.isNotEmpty()) {
                        Button(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseDanger),
                            border = BorderStroke(1.dp, PulseDanger.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear all downloads", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (downloadedTracks.size >= 4) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        placeholder = { Text("Filter downloaded songs") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PulseTextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulsePrimary,
                            unfocusedBorderColor = PulseGlassBorder,
                            focusedContainerColor = PulseSurface2,
                            unfocusedContainerColor = PulseSurface2
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        // Active Downloads Progress Section
        if (activeDownloads.isNotEmpty()) {
            item {
                Text(
                    text = "Downloading now (${activeDownloads.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = PulsePrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeDownloads.forEach { info ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PulseSurface2,
                            border = BorderStroke(1.dp, PulseGlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = if (info.status == DownloadStatus.QUEUED) "Queued…" else "Downloading (${(info.progress * 100).toInt()}%)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = PulseTextPrimary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { info.progress.coerceIn(0.05f, 1f) },
                                        color = PulsePrimary,
                                        trackColor = PulseSurface3,
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                IconButton(onClick = { onCancelDownload(info.trackId) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = PulseTextMuted)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (filtered.isEmpty() && activeDownloads.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PulseSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = PulseEmerald, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (filterQuery.isNotBlank()) "No matching downloads" else "No downloaded songs yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = PulseTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (filterQuery.isNotBlank()) "Try a different search." else "Tap the download button on any song, album, or playlist to save it for offline playback here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PulseTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            itemsIndexed(filtered, key = { _, d -> d.track.id }) { idx, dl ->
                TrackRow(
                    track = dl.track,
                    isNowPlaying = playbackState.currentTrack?.id == dl.track.id,
                    isActuallyPlaying = playbackState.isPlaying,
                    isLiked = dl.track.id in likedIds,
                    downloadState = downloadStates[dl.track.id],
                    onClick = { onPlayTracks(allTracks, idx, false, "Downloads") },
                    onLikeClick = { onToggleLike(dl.track) },
                    onDownloadClick = {},
                    onCancelDownloadClick = {},
                    onMoreClick = { onOpenTrackMenu(dl.track) },
                    extraSubtitle = "${formatBytes(dl.fileSizeBytes)} · ${dl.quality}",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }

    if (showClearDialog) {
        ConfirmActionDialog(
            title = "Remove all downloads?",
            message = "All ${downloadedTracks.size} offline songs (${formatBytes(totalBytes)}) will be deleted from your device.",
            confirmLabel = "Delete all",
            isDanger = true,
            onDismiss = { showClearDialog = false },
            onConfirm = {
                showClearDialog = false
                onClearAllDownloads()
            }
        )
    }
}
