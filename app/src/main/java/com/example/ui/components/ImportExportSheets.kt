package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AudioQuality
import com.example.data.model.ImportMatchStatus
import com.example.data.model.ImportPreview
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.data.remote.MusicFilter
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseBackground
import com.example.ui.theme.PulseDanger
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseGlassBorder
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulsePrimaryContainer
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSecondary
import com.example.ui.theme.PulseSurface1
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseSurface3
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary

@Composable
fun TextPromptDialog(
    title: String,
    initialValue: String = "",
    placeholder: String = "",
    confirmLabel: String = "OK",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PulseSurface2,
        titleContentColor = PulseTextPrimary,
        textContentColor = PulseTextMuted,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 80) value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulsePrimary,
                    unfocusedBorderColor = PulseGlassBorder,
                    focusedContainerColor = PulseSurface1,
                    unfocusedContainerColor = PulseSurface1
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PulseTextMuted)
            }
        }
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    isDanger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PulseSurface2,
        titleContentColor = PulseTextPrimary,
        textContentColor = PulseTextMuted,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDanger) PulseDanger else PulsePrimaryStrong,
                    contentColor = if (isDanger) PulseBackground else Color.White
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PulseTextMuted)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlaylistSheet(
    preview: ImportPreview?,
    isBusy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onFetchUrl: (String) -> Unit,
    onFilePicked: (String, String) -> Unit,
    onNameChange: (String) -> Unit,
    onToggleCandidate: (Int) -> Unit,
    onBackToStart: () -> Unit,
    onCommitImport: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var urlInput by remember { mutableStateOf("") }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "import.txt"
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                onFilePicked(name, content)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface1,
        contentColor = PulseTextPrimary
    ) {
        if (preview == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 26.dp)
            ) {
                Text("Import playlist", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bring a playlist in from Spotify, YouTube Music or a file (CSV, JSON, TXT, M3U). Songs are matched automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseTextMuted
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("Paste a Spotify or YouTube Music playlist link") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulsePrimary,
                        unfocusedBorderColor = PulseGlassBorder,
                        focusedContainerColor = PulseSurface2,
                        unfocusedContainerColor = PulseSurface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onFetchUrl(urlInput) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isBusy) "Fetching…" else "Fetch playlist")
                    }
                    Button(
                        onClick = { fileLauncher.launch(arrayOf("*/*")) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary),
                        border = BorderStroke(1.dp, PulseGlassBorder)
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import file")
                    }
                }
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = PulseDanger)
                }
            }
        } else {
            val selectedCount = preview.items.count { it.isSelected && it.matchedTrack != null }
            val dupsCount = preview.items.count { it.isDuplicate }
            val notFoundCount = preview.items.count { it.status == ImportMatchStatus.NOT_FOUND }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text("Import preview", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (preview.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = preview.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(PulseSurface3),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = PulsePrimary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = preview.playlistName,
                            onValueChange = onNameChange,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulsePrimary,
                                unfocusedBorderColor = PulseGlassBorder,
                                focusedContainerColor = PulseSurface2,
                                unfocusedContainerColor = PulseSurface2
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${preview.sourceLabel} · ${preview.items.size} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTextMuted
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (preview.isMatching) {
                    val progress = preview.matchedCount.toFloat() / preview.totalToMatch.coerceAtLeast(1).toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        color = PulsePrimary,
                        trackColor = PulseSurface3,
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Matching songs ${preview.matchedCount}/${preview.totalToMatch}…", style = MaterialTheme.typography.bodySmall, color = PulseTextMuted)
                } else {
                    Text(
                        "$selectedCount will be imported · $dupsCount duplicates · $notFoundCount not found. Tap to toggle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextMuted
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(preview.items) { idx, item ->
                        val isOn = item.isSelected && item.matchedTrack != null
                        val badgeText = when {
                            item.status == ImportMatchStatus.PENDING -> "Matching…"
                            item.isDuplicate -> "Duplicate"
                            item.status == ImportMatchStatus.MATCHED -> "Matched"
                            item.status == ImportMatchStatus.CHECK -> "Check"
                            else -> "Not found"
                        }
                        val badgeColor = when {
                            item.status == ImportMatchStatus.PENDING -> PulseTextMuted
                            item.isDuplicate -> PulseSecondary
                            item.status == ImportMatchStatus.MATCHED -> PulseEmerald
                            item.status == ImportMatchStatus.CHECK -> PulseAmber
                            else -> PulseDanger
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = item.matchedTrack != null) { onToggleCandidate(idx) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isOn) PulsePrimaryStrong else PulseSurface3),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isOn) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.sourceTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.sourceArtist, style = MaterialTheme.typography.bodySmall, color = PulseTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val matched = item.matchedTrack
                                if (matched != null && item.status == ImportMatchStatus.CHECK) {
                                    Text("→ ${matched.title} · ${matched.artist}", style = MaterialTheme.typography.bodySmall, color = PulsePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(badgeText, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCommitImport,
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import $selectedCount song${if (selectedCount == 1) "" else "s"}")
                    }
                    Button(
                        onClick = onBackToStart,
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseTextPrimary)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPlaylistSheet(
    playlistName: String,
    trackCount: Int,
    onDismiss: () -> Unit,
    onSelectFormat: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Export “$playlistName”", style = MaterialTheme.typography.headlineMedium)
            Text("$trackCount songs, in current order.", style = MaterialTheme.typography.bodyMedium, color = PulseTextMuted)
            Spacer(Modifier.height(4.dp))
            listOf(
                Triple("csv", "CSV", "Opens in Excel / Sheets · title, artist, album, link"),
                Triple("json", "JSON", "Full data · re-import into Pulse without matching"),
                Triple("txt", "TXT", "One “Artist - Title” per line"),
                Triple("m3u", "M3U playlist", "For players like VLC · links to YouTube Music")
            ).forEach { (fmt, title, desc) ->
                Surface(
                    onClick = { onSelectFormat(fmt); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    color = PulseSurface2
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = PulseTextMuted)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistSheet(
    playlist: UserPlaylist,
    onDismiss: () -> Unit,
    onSave: (String, String?, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(playlist.name) }
    var customCover by remember { mutableStateOf(playlist.customCoverUri) }
    var clearCover by remember { mutableStateOf(false) }
    var pickFromSongs by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customCover = uri.toString()
            clearCover = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface1,
        contentColor = PulseTextPrimary
    ) {
        if (pickFromSongs) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text("Choose a cover", style = MaterialTheme.typography.headlineMedium)
                Text("Tap a song’s artwork to use it as the cover.", style = MaterialTheme.typography.bodyMedium, color = PulseTextMuted)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(88.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(playlist.tracks.take(80)) { t ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    customCover = t.artworkUrl(544)
                                    clearCover = false
                                    pickFromSongs = false
                                }
                        ) {
                            PulseArtwork(t, Modifier.fillMaxWidth().aspectRatio(1f), sizePx = 240)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { pickFromSongs = false }, colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2)) {
                    Text("Back")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Edit playlist", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(14.dp))
                val previewPl = playlist.copy(customCoverUri = if (clearCover) null else customCover)
                PlaylistCollageCover(playlist = previewPl, modifier = Modifier.size(140.dp), cornerRadius = 14.dp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = PulseSurface2,
                        border = BorderStroke(1.dp, PulseGlassBorder)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Photo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (playlist.tracks.isNotEmpty()) {
                        Surface(
                            onClick = { pickFromSongs = true },
                            shape = RoundedCornerShape(999.dp),
                            color = PulseSurface2,
                            border = BorderStroke(1.dp, PulseGlassBorder)
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("From songs", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Surface(
                        onClick = { customCover = null; clearCover = true },
                        shape = RoundedCornerShape(999.dp),
                        color = PulseSurface2,
                        border = BorderStroke(1.dp, PulseGlassBorder)
                    ) {
                        Text("Collage", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulsePrimary,
                        unfocusedBorderColor = PulseGlassBorder,
                        focusedContainerColor = PulseSurface2,
                        unfocusedContainerColor = PulseSurface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onSave(name, customCover, clearCover); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimaryStrong, contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                    Button(
                        onClick = { onDismiss(); onDelete() },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseSurface2, contentColor = PulseDanger),
                        border = BorderStroke(1.dp, PulseDanger.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioQualitySheet(
    title: String,
    current: AudioQuality,
    onDismiss: () -> Unit,
    onSelect: (AudioQuality) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            AudioQuality.entries.forEach { q ->
                val selected = q == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) PulsePrimaryContainer else Color.Transparent)
                        .clickable { onSelect(q); onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(q.label, style = MaterialTheme.typography.titleSmall, color = if (selected) PulsePrimary else PulseTextPrimary)
                        Text(q.description, style = MaterialTheme.typography.bodySmall, color = PulseTextMuted)
                    }
                    if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = PulsePrimary)
                }
            }
        }
    }
}
