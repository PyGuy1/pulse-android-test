package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadStateInfo
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadedTrack
import com.example.data.model.UserPlaylist
import com.example.data.model.formatBytes
import com.example.ui.components.PlaylistCollageCover
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary

@Composable
fun LibraryScreen(
    likedCount: Int,
    downloadedTracks: List<DownloadedTrack>,
    downloadStates: Map<String, DownloadStateInfo>,
    userPlaylists: List<UserPlaylist>,
    onOpenDownloads: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onNewPlaylist: () -> Unit,
    onImportPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = downloadStates.values.count {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }
    val totalDownloadBytes = downloadedTracks.sumOf { it.fileSizeBytes }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("Your Library", style = MaterialTheme.typography.displaySmall, color = PulseTextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNewPlaylist,
                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("lib_new_playlist_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New playlist")
                    }
                    Button(
                        onClick = onImportPlaylist,
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("lib_import_playlist_btn")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import playlist")
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        // Downloads Folder Card (Prominent in Library)
        item {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenDownloads)
                    .padding(6.dp)
                    .testTag("library_downloads_folder_card")
            ) {
                Box {
                    PlaylistCollageCover(isDownloadsFolder = true, modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xCC060A18),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (activeCount > 0) Icons.Filled.Download else Icons.Filled.Folder,
                                contentDescription = null,
                                tint = if (activeCount > 0) PulsePrimary else PulseEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (activeCount > 0) "$activeCount active" else "Folder",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeCount > 0) PulsePrimary else PulseEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.titleMedium,
                    color = PulseTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val dlSub = buildString {
                    append("${downloadedTracks.size} song${if (downloadedTracks.size == 1) "" else "s"}")
                    if (totalDownloadBytes > 0L) append(" · ${formatBytes(totalDownloadBytes)}")
                }
                Text(
                    text = dlSub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Liked Songs Card
        item {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenLiked)
                    .padding(6.dp)
                    .testTag("library_liked_songs_card")
            ) {
                PlaylistCollageCover(isLikedFolder = true, modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Liked Songs",
                    style = MaterialTheme.typography.titleMedium,
                    color = PulseTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Playlist · $likedCount song${if (likedCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // User Playlists
        items(userPlaylists, key = { it.name }) { pl ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenPlaylist(pl.name) }
                    .padding(6.dp)
            ) {
                PlaylistCollageCover(playlist = pl, modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = pl.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PulseTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Playlist · ${pl.tracks.size} song${if (pl.tracks.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
