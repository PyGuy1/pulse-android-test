package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadStateInfo
import com.example.data.model.DownloadStatus
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.data.model.formatBytes
import com.example.data.remote.MusicConstants
import com.example.service.PlaybackUiState
import com.example.ui.theme.PulseBackground
import com.example.ui.theme.PulseDanger
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulsePrimaryContainer
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSurface1
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseSurface3
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    playbackState: PlaybackUiState,
    downloadStates: Map<String, DownloadStateInfo>,
    onDismiss: () -> Unit,
    onJumpToIndex: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClearUpcoming: () -> Unit,
    onSaveAsPlaylist: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSaveDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(playbackState.currentIndex) {
        if (playbackState.currentIndex >= 0 && playbackState.currentIndex < playbackState.queue.size) {
            listState.scrollToItem(maxOf(0, playbackState.currentIndex - 1))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface1,
        contentColor = PulseTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Queue", style = MaterialTheme.typography.headlineMedium)
            val count = playbackState.queue.size
            Text(
                text = if (count > 0) "$count song${if (count == 1) "" else "s"} · now playing ${playbackState.currentIndex + 1} of $count"
                else "Nothing in the queue yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseTextMuted
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onClearUpcoming,
                    shape = RoundedCornerShape(999.dp),
                    color = PulseSurface2,
                    border = BorderStroke(1.dp, PulseGlassBorder)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear upcoming", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    onClick = { if (playbackState.queue.isNotEmpty()) showSaveDialog = true },
                    shape = RoundedCornerShape(999.dp),
                    color = PulseSurface2,
                    border = BorderStroke(1.dp, PulseGlassBorder)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save as playlist", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (playbackState.queue.isEmpty()) {
                Text("Play a song to start a queue.", color = PulseTextMuted, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f, fill = false)) {
                    itemsIndexed(playbackState.queue) { index, track ->
                        val isNow = index == playbackState.currentIndex
                        val isPast = index < playbackState.currentIndex
                        val tag = when {
                            track.isUserQueued -> " · Queued by you"
                            track.isAutoplay -> " · Autoplay"
                            else -> ""
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isNow) PulsePrimaryContainer else Color.Transparent)
                                .clickable { onJumpToIndex(index) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                                if (isNow) {
                                    EqualizerBars(isPlaying = playbackState.isPlaying)
                                } else {
                                    Text(
                                        "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isPast) PulseTextMuted.copy(alpha = 0.5f) else PulseTextMuted
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            PulseArtwork(
                                track = track,
                                localArtPath = downloadStates[track.id]?.localArtworkPath,
                                sizePx = 120,
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isNow) PulsePrimary else if (isPast) PulseTextPrimary.copy(alpha = 0.6f) else PulseTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist}$tag",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PulseTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!isNow) {
                                IconButton(onClick = { onRemoveAt(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove from queue", tint = PulseTextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        TextPromptDialog(
            title = "Save queue as playlist",
            initialValue = "Queue mix",
            placeholder = "Playlist name",
            confirmLabel = "Save",
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                showSaveDialog = false
                onSaveAsPlaylist(name)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuBottomSheet(
    track: Track,
    isLiked: Boolean,
    downloadState: DownloadStateInfo?,
    canRemoveFromPlaylist: Boolean,
    canRemoveFromHistory: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onToggleLike: () -> Unit,
    onStartRadio: () -> Unit,
    onGoToArtist: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onRemoveFromHistory: () -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ytUrl = "https://music.youtube.com/watch?v=${track.id}"
    val dlStatus = downloadState?.status ?: DownloadStatus.NONE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface1,
        contentColor = PulseTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulseArtwork(
                    track = track,
                    localArtPath = downloadState?.localArtworkPath,
                    sizePx = 160,
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = buildString {
                            append(track.artist.ifBlank { "Unknown artist" })
                            if (dlStatus == DownloadStatus.DOWNLOADED && downloadState != null) {
                                append(" · Offline (${formatBytes(downloadState.fileSizeBytes)})")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dlStatus == DownloadStatus.DOWNLOADED) PulseEmerald else PulseTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = PulseGlassBorder)
            Spacer(Modifier.height(6.dp))

            SheetActionItem(Icons.Filled.PlaylistPlay, "Play next") { onDismiss(); onPlayNext() }
            SheetActionItem(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { onDismiss(); onAddToQueue() }
            SheetActionItem(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") { onAddToPlaylist() }

            when (dlStatus) {
                DownloadStatus.DOWNLOADED -> {
                    val audioUriStr = downloadState?.publicUri
                    if (!audioUriStr.isNullOrBlank()) {
                        SheetActionItem(
                            icon = Icons.Filled.OpenInNew,
                            label = "Play in external music app",
                            tint = PulseEmerald
                        ) {
                            onDismiss()
                            runCatching {
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(audioUriStr), "audio/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(viewIntent, "Play with"))
                            }.onFailure {
                                onToast("No external audio player found")
                            }
                        }
                    }
                    SheetActionItem(
                        icon = Icons.Filled.DeleteOutline,
                        label = "Remove from Downloads",
                        tint = PulseDanger
                    ) { onDismiss(); onRemoveDownload() }
                }
                DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> SheetActionItem(
                    icon = Icons.Filled.Close,
                    label = "Cancel download (${((downloadState?.progress ?: 0f) * 100).toInt()}%)"
                ) { onDismiss(); onCancelDownload() }
                else -> SheetActionItem(
                    icon = Icons.Outlined.Download,
                    label = "Download to Library & Device Downloads",
                    tint = PulsePrimary
                ) { onDismiss(); onDownload() }
            }

            SheetActionItem(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = if (isLiked) "Remove from Liked Songs" else "Add to Liked Songs",
                tint = if (isLiked) PulsePrimary else PulseTextPrimary
            ) { onDismiss(); onToggleLike() }
            SheetActionItem(Icons.Filled.Radio, "Start radio") { onDismiss(); onStartRadio() }
            if (track.artist.isNotBlank()) {
                SheetActionItem(Icons.Filled.Person, "Go to artist") { onDismiss(); onGoToArtist() }
            }
            if (canRemoveFromPlaylist) {
                SheetActionItem(Icons.Filled.Close, "Remove from this playlist", tint = PulseDanger) {
                    onDismiss(); onRemoveFromPlaylist()
                }
            }
            if (canRemoveFromHistory) {
                SheetActionItem(Icons.Filled.Close, "Remove from recently played") {
                    onDismiss(); onRemoveFromHistory()
                }
            }
            SheetActionItem(Icons.Filled.OpenInNew, "Open in YouTube Music") {
                onDismiss()
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl))) }
            }
            SheetActionItem(Icons.Filled.ContentCopy, "Copy link") {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Pulse link", ytUrl))
                onDismiss()
                onToast("Link copied")
            }
            SheetActionItem(Icons.Filled.Share, "Share…") {
                onDismiss()
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, track.title)
                    putExtra(Intent.EXTRA_TEXT, "${track.title} – ${track.artist}\n$ytUrl")
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share song"))
            }
        }
    }
}

@Composable
fun SheetActionItem(
    icon: ImageVector,
    label: String,
    tint: Color = PulseTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PulseSurface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track,
    playlists: List<UserPlaylist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface1,
        contentColor = PulseTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Add to playlist", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onSelectPlaylist("Liked Songs")
                        onDismiss()
                    }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistCollageCover(isLikedFolder = true, modifier = Modifier.size(44.dp), cornerRadius = 8.dp)
                Spacer(Modifier.width(12.dp))
                Text("Liked Songs", style = MaterialTheme.typography.titleMedium)
            }
            playlists.forEach { pl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPlaylist(pl.name)
                            onDismiss()
                        }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaylistCollageCover(playlist = pl, modifier = Modifier.size(44.dp), cornerRadius = 8.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(pl.name, style = MaterialTheme.typography.titleMedium)
                        Text("${pl.tracks.size} songs", style = MaterialTheme.typography.bodySmall, color = PulseTextMuted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 60) newName = it },
                    placeholder = { Text("New playlist name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulsePrimary,
                        unfocusedBorderColor = PulseGlassBorder,
                        focusedContainerColor = PulseSurface2,
                        unfocusedContainerColor = PulseSurface2
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotEmpty()) {
                            onCreateAndAdd(trimmed)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White)
                ) {
                    Text("Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    playbackState: PlaybackUiState,
    onDismiss: () -> Unit,
    onStartTimer: (Int, Boolean) -> Unit,
    onClearTimer: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sliderVal by remember { mutableFloatStateOf(30f) }
    val active = playbackState.sleepAfterSong || playbackState.sleepTimerEndMs > 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface1,
        contentColor = PulseTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
        ) {
            Text("Sleep timer", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    playbackState.sleepAfterSong -> "Stops at the end of this song."
                    playbackState.sleepTimerEndMs > 0L -> "Stops in about ${playbackState.sleepRemainingMinutes} min."
                    else -> "Pause playback automatically. Up to 2 hours."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = PulseTextMuted
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = ((it / 5f).roundToInt() * 5).coerceIn(5, 120).toFloat() },
                    valueRange = 5f..120f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = PulsePrimary,
                        inactiveTrackColor = PulseSurface3
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                val mins = sliderVal.toInt()
                Text(
                    text = if (mins % 60 == 0) "${mins / 60} hr" else "$mins min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60, 90, 120).forEach { m ->
                    Surface(
                        onClick = { onStartTimer(m, false); onDismiss() },
                        shape = RoundedCornerShape(999.dp),
                        color = PulseSurface2,
                        border = BorderStroke(1.dp, PulseGlassBorder)
                    ) {
                        Text(
                            text = if (m % 60 == 0) "${m / 60} hr${if (m > 60) "s" else ""}" else "$m min",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
                Surface(
                    onClick = { onStartTimer(0, true); onDismiss() },
                    shape = RoundedCornerShape(999.dp),
                    color = PulseSurface2,
                    border = BorderStroke(1.dp, PulseGlassBorder)
                ) {
                    Text(
                        text = "End of song",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onStartTimer(sliderVal.toInt(), false); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White)
                ) {
                    Text("Start timer")
                }
                if (active) {
                    Button(
                        onClick = { onClearTimer(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder)
                    ) {
                        Text("Turn off")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerSheet(
    currentRegion: String,
    isOverride: Boolean,
    onDismiss: () -> Unit,
    onSelectRegion: (String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        val q = query.trim().lowercase()
        MusicConstants.ALL_COUNTRIES
            .map { it to MusicConstants.countryName(it) }
            .filter { (code, name) -> q.isEmpty() || name.lowercase().contains(q) || code.lowercase() == q }
            .sortedWith(compareBy<Pair<String, String>> { it.first != "WW" }.thenBy { it.second })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface1,
        contentColor = PulseTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Country for charts", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search countries") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulsePrimary,
                    unfocusedBorderColor = PulseGlassBorder,
                    focusedContainerColor = PulseSurface2,
                    unfocusedContainerColor = PulseSurface2
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn {
                if (isOverride && query.isBlank()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectRegion(null); onDismiss() }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PulsePrimary)
                            Spacer(Modifier.width(12.dp))
                            Text("Use my device’s region", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                itemsIndexed(list) { _, (code, name) ->
                    val selected = code == currentRegion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) PulsePrimaryContainer else Color.Transparent)
                            .clickable { onSelectRegion(code); onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PulseSurface2),
                            contentAlignment = Alignment.Center
                        ) {
                            if (code == "WW") {
                                Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                            } else {
                                Text(code, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) PulsePrimary else PulseTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = PulsePrimary)
                        }
                    }
                }
            }
        }
    }
}
