package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DownloadStateInfo
import com.example.data.model.DownloadStatus
import com.example.data.model.PlaylistSummary
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.data.model.formatDuration
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulseGlassHighlight
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulsePrimaryContainer
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSecondary
import com.example.ui.theme.PulseSurface1
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseSurface3
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary
import java.io.File
import kotlin.math.abs

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tintColor: Color = PulsePrimary,
    tintAlpha: Float = 0.08f,
    content: @Composable () -> Unit
) {
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xB81E2749),
            Color(0xCC111730),
            Color(0xC2151C3A)
        )
    )
    val diagonalSheen = Brush.linearGradient(
        colors = listOf(
            Color(0x30FFFFFF),
            tintColor.copy(alpha = tintAlpha),
            Color.Transparent,
            PulseSecondary.copy(alpha = 0.06f)
        )
    )
    val topSpecular = Brush.verticalGradient(
        colors = listOf(
            Color(0x40FFFFFF),
            Color(0x10FFFFFF),
            Color.Transparent
        )
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x75FFFFFF),
            Color(0x24FFFFFF),
            Color(0x52C3ADFF),
            Color(0x408EE6F8)
        )
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .background(diagonalSheen)
            .border(1.15.dp, borderBrush, shape)
    ) {
        // Inner curved glass top light-catch sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(topSpecular)
        )
        content()
    }
}

@Composable
fun PulseArtwork(
    track: Track,
    modifier: Modifier = Modifier,
    sizePx: Int = 240,
    localArtPath: String? = null,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val context = LocalContext.current
    var fallbackStep by remember(track.id, localArtPath) { mutableIntStateOf(0) }
    val model: Any? = remember(track.id, localArtPath, fallbackStep) {
        if (localArtPath != null && File(localArtPath).exists() && fallbackStep == 0) {
            File(localArtPath)
        } else {
            when (fallbackStep) {
                0 -> track.artworkUrl(sizePx)
                1 -> track.fallbackArtworkUrl()
                else -> null
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(PulseSurface3),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = PulseTextMuted,
            modifier = Modifier.size(24.dp)
        )
        if (model != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = "${track.title} artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    if (fallbackStep < 2) fallbackStep++
                }
            )
        }
    }
}

@Composable
fun PlaylistCollageCover(
    playlist: UserPlaylist? = null,
    isLikedFolder: Boolean = false,
    isDownloadsFolder: Boolean = false,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    when {
        isLikedFolder -> {
            Box(
                modifier = modifier
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PulsePrimaryStrong, PulseSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Liked Songs",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(0.42f)
                )
            }
        }

        isDownloadsFolder -> {
            Box(
                modifier = modifier
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E7E68), Color(0xFF3A7BD5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DownloadDone,
                    contentDescription = "Downloads Folder",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(0.44f)
                )
            }
        }

        playlist != null && !playlist.customCoverUri.isNullOrBlank() -> {
            AsyncImage(
                model = playlist.customCoverUri,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(PulseSurface3)
            )
        }

        playlist != null && playlist.tracks.size >= 4 -> {
            val picked = remember(playlist.tracks) {
                val distinct = playlist.tracks.distinctBy { it.thumbUrl.ifBlank { it.id } }.take(4)
                if (distinct.size == 4) distinct else playlist.tracks.take(4)
            }
            Column(
                modifier = modifier
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(PulseSurface3)
            ) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    PulseArtwork(picked[0], Modifier.weight(1f).fillMaxSize(), sizePx = 160, shape = RoundedCornerShape(0.dp))
                    PulseArtwork(picked[1], Modifier.weight(1f).fillMaxSize(), sizePx = 160, shape = RoundedCornerShape(0.dp))
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    PulseArtwork(picked[2], Modifier.weight(1f).fillMaxSize(), sizePx = 160, shape = RoundedCornerShape(0.dp))
                    PulseArtwork(picked[3], Modifier.weight(1f).fillMaxSize(), sizePx = 160, shape = RoundedCornerShape(0.dp))
                }
            }
        }

        playlist != null && playlist.tracks.isNotEmpty() -> {
            PulseArtwork(
                track = playlist.tracks.first(),
                modifier = modifier.aspectRatio(1f),
                sizePx = 240,
                shape = shape
            )
        }

        else -> {
            Box(
                modifier = modifier
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(PulseSurface3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = PulseTextMuted,
                    modifier = Modifier.fillMaxSize(0.4f)
                )
            }
        }
    }
}

fun artistHueColor(name: String): Color {
    val hash = name.fold(7) { acc, c -> acc * 31 + c.code }
    val hue = (abs(hash) % 360).toFloat()
    return Color.hsl(hue, 0.38f, 0.34f)
}

@Composable
fun ArtistAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 24
) {
    val initials = remember(name) {
        name.split(Regex("\\s+"))
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "P" }
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(artistHueColor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = fontSizeSp.sp
        )
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = PulsePrimary
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val h1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "b1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
        label = "b2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(490), RepeatMode.Reverse),
        label = "b3"
    )
    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val f1 = if (isPlaying) h1 else 0.4f
        val f2 = if (isPlaying) h2 else 0.7f
        val f3 = if (isPlaying) h3 else 0.5f
        Box(Modifier.width(3.dp).height((14 * f1).dp).clip(RoundedCornerShape(2.dp)).background(barColor))
        Box(Modifier.width(3.dp).height((14 * f2).dp).clip(RoundedCornerShape(2.dp)).background(barColor))
        Box(Modifier.width(3.dp).height((14 * f3).dp).clip(RoundedCornerShape(2.dp)).background(barColor))
    }
}

@Composable
fun TrackDownloadButton(
    downloadState: DownloadStateInfo?,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val status = downloadState?.status ?: DownloadStatus.NONE
    when (status) {
        DownloadStatus.DOWNLOADED -> {
            IconButton(
                onClick = { onRemoveClick?.invoke() ?: onDownloadClick() },
                modifier = modifier
                    .minimumInteractiveComponentSize()
                    .testTag("track_downloaded_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.DownloadDone,
                    contentDescription = "Downloaded offline",
                    tint = PulseEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
            IconButton(
                onClick = onCancelClick,
                modifier = modifier
                    .minimumInteractiveComponentSize()
                    .testTag("track_downloading_btn")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                    val progress = downloadState?.progress ?: 0f
                    if (status == DownloadStatus.QUEUED || progress <= 0.02f) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = PulsePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { progress },
                            strokeWidth = 2.dp,
                            color = PulsePrimary,
                            trackColor = PulseSurface3,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cancel download",
                        tint = PulseTextPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        DownloadStatus.FAILED, DownloadStatus.NONE -> {
            IconButton(
                onClick = onDownloadClick,
                modifier = modifier
                    .minimumInteractiveComponentSize()
                    .testTag("track_download_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Download song",
                    tint = if (status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else PulseTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    isNowPlaying: Boolean,
    isActuallyPlaying: Boolean,
    isLiked: Boolean,
    downloadState: DownloadStateInfo?,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelDownloadClick: () -> Unit,
    onMoreClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
    extraSubtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isNowPlaying) PulsePrimaryContainer else Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseArtwork(
            track = track,
            localArtPath = downloadState?.localArtworkPath,
            sizePx = 160,
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isNowPlaying) {
                    EqualizerBars(isPlaying = isActuallyPlaying)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isNowPlaying) PulsePrimary else PulseTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (downloadState?.status == DownloadStatus.DOWNLOADED) {
                    Text(
                        text = "OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseEmerald,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x1F8FF0C8))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                val sub = buildString {
                    append(track.artist.ifBlank { "Unknown artist" })
                    val dur = formatDuration(track.durationSeconds)
                    if (dur.isNotEmpty()) append(" · $dur")
                    if (!extraSubtitle.isNullOrBlank()) append(" · $extraSubtitle")
                }
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        TrackDownloadButton(
            downloadState = downloadState,
            onDownloadClick = onDownloadClick,
            onCancelClick = onCancelDownloadClick,
            onRemoveClick = onMoreClick
        )
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("track_like_btn")
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) PulsePrimary else PulseTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        if (onRemoveClick != null) {
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove",
                    tint = PulseTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("track_more_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "More options",
                    tint = PulseTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TrackShelfCard(
    track: Track,
    downloadState: DownloadStateInfo?,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(146.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(modifier = Modifier.size(130.dp)) {
            PulseArtwork(
                track = track,
                localArtPath = downloadState?.localArtworkPath,
                sizePx = 260,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(10.dp)
            )
            Surface(
                onClick = onMoreClick,
                shape = CircleShape,
                color = Color(0xB3060A18),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "More options",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (downloadState?.status == DownloadStatus.DOWNLOADED) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC060A18)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = PulseEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PulsePrimaryStrong),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = PulseTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist.ifBlank { "Song" },
            style = MaterialTheme.typography.bodyMedium,
            color = PulseTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistShelfCard(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(146.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PulseSurface3),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = PulseTextMuted,
                modifier = Modifier.size(32.dp)
            )
            if (playlist.thumbUrl.isNotBlank()) {
                AsyncImage(
                    model = playlist.thumbUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            color = PulseTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.artist.ifBlank { if (playlist.isAlbum) "Album" else "Playlist" },
            style = MaterialTheme.typography.bodyMedium,
            color = PulseTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistShelfCard(
    name: String,
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(136.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArtistAvatar(
            name = name,
            avatarUrl = avatarUrl,
            modifier = Modifier.size(116.dp),
            fontSizeSp = 28
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = PulseTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Artist",
            style = MaterialTheme.typography.bodyMedium,
            color = PulseTextMuted,
            maxLines = 1
        )
    }
}

@Composable
fun SkeletonTrackRows(count: Int = 6) {
    val transition = rememberInfiniteTransition(label = "sk")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skAlpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PulseSurface3.copy(alpha = alpha))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.65f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PulseSurface3.copy(alpha = alpha))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.4f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PulseSurface3.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
fun SkeletonShelfCards(count: Int = 5) {
    val transition = rememberInfiniteTransition(label = "skc")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skcAlpha"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Column(Modifier.width(146.dp).padding(8.dp)) {
                Box(
                    Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PulseSurface3.copy(alpha = alpha))
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PulseSurface3.copy(alpha = alpha))
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PulseSurface3.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
fun SectionRetryBanner(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = PulseTextMuted,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
            border = BorderStroke(1.dp, PulseGlassBorder)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Retry")
        }
    }
}
