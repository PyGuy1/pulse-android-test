package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadStateInfo
import com.example.data.model.DownloadStatus
import com.example.data.model.Track
import com.example.data.model.formatDurationMs
import com.example.service.PlaybackUiState
import com.example.service.RepeatModeState
import com.example.ui.theme.PulseBackground
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulseSecondary
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseSurface3
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary
import java.io.File

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    isLiked: Boolean,
    downloadState: DownloadStateInfo?,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack
    val pillShape = RoundedCornerShape(999.dp)
    val activeTint = if (playbackState.isOfflinePlayback) PulseEmerald else PulsePrimary

    Surface(
        shape = pillShape,
        color = Color.Transparent,
        shadowElevation = 18.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("mini_player_bar")
    ) {
        GlassContainer(
            shape = pillShape,
            tintColor = activeTint,
            tintAlpha = if (playbackState.isPlaying) 0.14f else 0.08f,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = track != null, onClick = onExpand)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular artwork inside glass rim
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(1.5.dp)
                    ) {
                        if (track != null) {
                            PulseArtwork(
                                track = track,
                                localArtPath = downloadState?.localArtworkPath,
                                sizePx = 160,
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(PulseSurface3)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "Nothing playing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PulseTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (playbackState.isOfflinePlayback) {
                                Surface(
                                    shape = CircleShape,
                                    color = PulseEmerald.copy(alpha = 0.18f),
                                    border = BorderStroke(0.8.dp, PulseEmerald.copy(alpha = 0.45f)),
                                    modifier = Modifier.padding(end = 5.dp)
                                ) {
                                    Text(
                                        text = "OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PulseEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = track?.artist?.ifBlank { "Pulse" } ?: "Pick a song to start",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (track != null) {
                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier
                                .size(40.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("mini_like_btn")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) PulsePrimary else PulseTextPrimary.copy(alpha = 0.78f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onPrev,
                            enabled = playbackState.currentIndex > 0,
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = if (playbackState.currentIndex > 0) PulseTextPrimary else PulseTextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Frosted specular Play/Pause pill
                        Surface(
                            onClick = onTogglePlayPause,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, Color.White),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("mini_play_pause_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (playbackState.isBuffering) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.2.dp,
                                        color = PulseBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                        tint = PulseBackground,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("mini_next_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = PulseTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenQueue,
                            modifier = Modifier
                                .size(40.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = PulseTextPrimary.copy(alpha = 0.78f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Sleek liquid-glass progress hairline inside bottom curve
                if (track != null && playbackState.durationMs > 0L) {
                    val fraction = (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat())
                        .coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(PulsePrimary, PulseSecondary)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenPlayerOverlay(
    playbackState: PlaybackUiState,
    isLiked: Boolean,
    autoplayEnabled: Boolean,
    downloadState: DownloadStateInfo?,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onDownloadClick: (Track) -> Unit,
    onCancelDownload: (String) -> Unit,
    onToggleAutoplay: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenMore: (Track) -> Unit
) {
    val track = playbackState.currentTrack ?: return
    val artModel: Any = remember(track.id, downloadState?.localArtworkPath) {
        val local = downloadState?.localArtworkPath
        if (local != null && File(local).exists()) File(local) else track.artworkUrl(1000)
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    val currentFraction = if (isDragging) {
        dragFraction
    } else {
        (playbackState.currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseBackground)
    ) {
        // Blurred ambient artwork backdrop
        AsyncImage(
            model = artModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(70.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88060A18),
                            Color(0xCC060A18),
                            Color(0xF2060A18)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PulseGlassBorder),
                    modifier = Modifier.size(44.dp).testTag("fs_close_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close player", tint = Color.White)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTextMuted,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = playbackState.sourceLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = PulseTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    onClick = { onOpenMore(track) },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PulseGlassBorder),
                    modifier = Modifier.size(44.dp).testTag("fs_more_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MoreHoriz, contentDescription = "More options", tint = Color.White)
                    }
                }
            }

            // Center Artwork
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 24.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .widthIn(max = 360.dp)
                    .aspectRatio(1f)
            ) {
                PulseArtwork(
                    track = track,
                    localArtPath = downloadState?.localArtworkPath,
                    sizePx = 1000,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // Metadata + Seek + Controls
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = PulseTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = track.artist.ifBlank { "Unknown artist" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = PulseTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TrackDownloadButton(
                        downloadState = downloadState,
                        onDownloadClick = { onDownloadClick(track) },
                        onCancelClick = { onCancelDownload(track.id) },
                        onRemoveClick = { onOpenMore(track) }
                    )
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("fs_like_btn")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) PulsePrimary else PulseTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Scrubber
                Slider(
                    value = currentFraction,
                    onValueChange = {
                        isDragging = true
                        dragFraction = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onSeekTo((dragFraction * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = PulsePrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val shownPos = if (isDragging) (dragFraction * durationMs).toLong() else playbackState.currentPositionMs
                    Text(formatDurationMs(shownPos), style = MaterialTheme.typography.labelMedium, color = PulseTextMuted)
                    Text(formatDurationMs(playbackState.durationMs), style = MaterialTheme.typography.labelMedium, color = PulseTextMuted)
                }

                Spacer(Modifier.height(12.dp))

                // Main transport row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleEnabled) PulsePrimary else PulseTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Surface(
                        onClick = onPrev,
                        enabled = playbackState.currentIndex > 0,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = if (playbackState.currentIndex > 0) Color.White else PulseTextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Surface(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(70.dp).testTag("fs_play_pause_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (playbackState.isBuffering) {
                                CircularProgressIndicator(strokeWidth = 3.dp, color = PulseBackground, modifier = Modifier.size(28.dp))
                            } else {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = PulseBackground,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                    Surface(
                        onClick = onNext,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.repeatMode == RepeatModeState.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != RepeatModeState.OFF) PulsePrimary else PulseTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Status Pill (Offline vs Streaming)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GlassContainer(
                        shape = CircleShape,
                        tintColor = if (playbackState.isOfflinePlayback) PulseEmerald else PulsePrimary,
                        tintAlpha = 0.12f
                    ) {
                        Text(
                            text = if (playbackState.isOfflinePlayback) {
                                "Offline · Saved in Downloads & Device Download/Pulse"
                            } else {
                                "Pulse Audio Stream · Active"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (playbackState.isOfflinePlayback) PulseEmerald else PulseTextPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick action dock
                GlassContainer(shape = RoundedCornerShape(999.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val sleepActive = playbackState.sleepAfterSong || playbackState.sleepTimerEndMs > 0L
                        val sleepText = when {
                            playbackState.sleepAfterSong -> "End"
                            playbackState.sleepRemainingMinutes > 0 -> "${playbackState.sleepRemainingMinutes}m"
                            else -> "Sleep"
                        }
                        FsActionTile(
                            icon = Icons.Filled.Timer,
                            label = sleepText,
                            active = sleepActive,
                            onClick = onOpenSleepTimer
                        )
                        FsActionTile(
                            icon = Icons.Filled.Radio,
                            label = "Autoplay",
                            active = autoplayEnabled,
                            onClick = onToggleAutoplay
                        )
                        val dlStatus = downloadState?.status ?: DownloadStatus.NONE
                        FsActionTile(
                            icon = if (dlStatus == DownloadStatus.DOWNLOADED) Icons.Filled.DownloadDone else Icons.Outlined.Download,
                            label = when (dlStatus) {
                                DownloadStatus.DOWNLOADED -> "Saved"
                                DownloadStatus.DOWNLOADING -> "${(downloadState!!.progress * 100).toInt()}%"
                                DownloadStatus.QUEUED -> "Queued"
                                else -> "Download"
                            },
                            active = dlStatus == DownloadStatus.DOWNLOADED || dlStatus == DownloadStatus.DOWNLOADING,
                            activeColor = if (dlStatus == DownloadStatus.DOWNLOADED) PulseEmerald else PulsePrimary,
                            onClick = {
                                when (dlStatus) {
                                    DownloadStatus.DOWNLOADED -> onOpenMore(track)
                                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> onCancelDownload(track.id)
                                    else -> onDownloadClick(track)
                                }
                            }
                        )
                        FsActionTile(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            label = "Queue",
                            active = false,
                            onClick = onOpenQueue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FsActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color = PulsePrimary,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) activeColor else PulseTextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (active) activeColor else PulseTextMuted
        )
    }
}
