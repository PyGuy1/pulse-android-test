package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioQuality
import com.example.data.model.DownloadedTrack
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.data.model.formatBytes
import com.example.data.remote.MusicConstants
import com.example.ui.components.AudioQualitySheet
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PlaylistCollageCover
import com.example.ui.components.TextPromptDialog
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulsePrimaryStrong
import com.example.ui.theme.PulseSurface1
import com.example.ui.theme.PulseSurface2
import com.example.ui.theme.PulseSurface3
import com.example.ui.theme.PulseTextMuted
import com.example.ui.theme.PulseTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    autoplay: Boolean,
    audioQuality: AudioQuality,
    downloadQuality: AudioQuality,
    regionCode: String,
    reduceMotion: Boolean,
    customPipedUrl: String,
    downloadedTracks: List<DownloadedTrack>,
    likedTracks: List<Track>,
    userPlaylists: List<UserPlaylist>,
    onToggleAutoplay: (Boolean) -> Unit,
    onSelectAudioQuality: (AudioQuality) -> Unit,
    onSelectDownloadQuality: (AudioQuality) -> Unit,
    onOpenCountryPicker: () -> Unit,
    onToggleReduceMotion: (Boolean) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenImport: () -> Unit,
    onExportPlaylist: (String, List<Track>) -> Unit,
    onBackupExport: () -> Unit,
    onRestoreBackupJson: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClearRecentSearches: () -> Unit,
    onClearDownloads: () -> Unit,
    onSetCustomServer: (String) -> Unit,
    onResetPulse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showStreamQualitySheet by remember { mutableStateOf(false) }
    var showDownloadQualitySheet by remember { mutableStateOf(false) }
    var showPickPlaylistForExport by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                if (text.isNotBlank()) onRestoreBackupJson(text)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 180.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.displaySmall, color = PulseTextPrimary)
            Spacer(Modifier.height(10.dp))
        }

        item { SettingsSectionHeader("Library") }
        item { SettingsActionRow(Icons.Filled.UploadFile, "Import playlist", "From Spotify, YouTube Music or a file", onClick = onOpenImport) }
        item { SettingsActionRow(Icons.Filled.IosShare, "Export playlist", "Save as CSV, JSON, TXT or M3U", onClick = { showPickPlaylistForExport = true }) }
        item { SettingsActionRow(Icons.Filled.Backup, "Back up everything", "All playlists and liked songs in one file", onClick = onBackupExport) }
        item { SettingsActionRow(Icons.Filled.Restore, "Restore backup", "Add playlists from a Pulse backup file", onClick = { backupLauncher.launch(arrayOf("application/json", "*/*")) }) }

        item { SettingsSectionHeader("Playback & Downloads") }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.Radio,
                title = "Autoplay similar songs",
                desc = "Keep playing songs like the last one when the queue ends",
                checked = autoplay,
                onCheckedChange = onToggleAutoplay
            )
        }
        item {
            SettingsActionRow(
                icon = Icons.Filled.GraphicEq,
                title = "Streaming audio quality",
                desc = "Lower quality uses less mobile data",
                valueText = audioQuality.label,
                onClick = { showStreamQualitySheet = true }
            )
        }
        item {
            SettingsActionRow(
                icon = Icons.Filled.CloudDownload,
                title = "Download audio quality",
                desc = "Quality saved in Library → Downloads",
                valueText = downloadQuality.label,
                onClick = { showDownloadQualitySheet = true }
            )
        }
        item {
            SettingsActionRow(
                icon = Icons.Filled.Timer,
                title = "Sleep timer",
                desc = "Stop playback after a set time",
                onClick = onOpenSleepTimer
            )
        }

        item { SettingsSectionHeader("Charts") }
        item {
            SettingsActionRow(
                icon = Icons.Filled.Public,
                title = "Country for charts",
                desc = "Used for “Top in …” on Home",
                valueText = MusicConstants.countryName(regionCode),
                onClick = onOpenCountryPicker
            )
        }

        item { SettingsSectionHeader("Appearance") }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.Speed,
                title = "Reduce motion",
                desc = "Minimize UI animations",
                checked = reduceMotion,
                onCheckedChange = onToggleReduceMotion
            )
        }

        item { SettingsSectionHeader("Data & Storage") }
        item {
            SettingsActionRow(
                icon = Icons.Filled.Download,
                title = "Clear downloads",
                desc = "${downloadedTracks.size} offline songs (${formatBytes(downloadedTracks.sumOf { it.fileSizeBytes })})",
                onClick = { if (downloadedTracks.isNotEmpty()) confirmAction = "downloads" }
            )
        }
        item { SettingsActionRow(Icons.Filled.DeleteOutline, "Clear recently played", "Remove your listening history", onClick = { confirmAction = "history" }) }
        item { SettingsActionRow(Icons.Filled.History, "Clear search history", "Remove recent searches", onClick = { confirmAction = "searches" }) }
        item {
            SettingsActionRow(
                icon = Icons.Filled.Refresh,
                title = "Search server",
                desc = "Advanced: use your own Piped server",
                valueText = if (customPipedUrl.isBlank()) "Automatic" else "Custom",
                onClick = { showServerDialog = true }
            )
        }
        item {
            SettingsActionRow(
                icon = Icons.Filled.DeleteForever,
                title = "Reset Pulse",
                desc = "Delete playlists, downloads, liked songs and history on this device",
                onClick = { confirmAction = "reset" }
            )
        }

        item { SettingsSectionHeader("About") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(PulseSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = PulsePrimary)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Pulse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Streams audio natively in the background via Media3 ExoPlayer and saves offline songs inside Library → Downloads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextMuted
                    )
                }
            }
        }
    }

    if (showStreamQualitySheet) {
        AudioQualitySheet(
            title = "Streaming audio quality",
            current = audioQuality,
            onDismiss = { showStreamQualitySheet = false },
            onSelect = onSelectAudioQuality
        )
    }

    if (showDownloadQualitySheet) {
        AudioQualitySheet(
            title = "Download audio quality",
            current = downloadQuality,
            onDismiss = { showDownloadQualitySheet = false },
            onSelect = onSelectDownloadQuality
        )
    }

    if (showPickPlaylistForExport) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPickPlaylistForExport = false },
            sheetState = sheetState,
            containerColor = PulseSurface1,
            contentColor = PulseTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 26.dp)
            ) {
                Text("Export which playlist?", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showPickPlaylistForExport = false
                            onExportPlaylist("Liked Songs", likedTracks)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaylistCollageCover(isLikedFolder = true, modifier = Modifier.size(44.dp), cornerRadius = 8.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Liked Songs", style = MaterialTheme.typography.titleSmall)
                        Text("${likedTracks.size} songs", style = MaterialTheme.typography.bodySmall, color = PulseTextMuted)
                    }
                }
                userPlaylists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showPickPlaylistForExport = false
                                onExportPlaylist(pl.name, pl.tracks)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistCollageCover(playlist = pl, modifier = Modifier.size(44.dp), cornerRadius = 8.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(pl.name, style = MaterialTheme.typography.titleSmall)
                            Text("${pl.tracks.size} songs", style = MaterialTheme.typography.bodySmall, color = PulseTextMuted)
                        }
                    }
                }
            }
        }
    }

    if (showServerDialog) {
        TextPromptDialog(
            title = "Search server",
            initialValue = customPipedUrl,
            placeholder = "https://your-piped-server (empty = automatic)",
            confirmLabel = "Save",
            onDismiss = { showServerDialog = false },
            onConfirm = { url ->
                showServerDialog = false
                onSetCustomServer(url)
            }
        )
    }

    when (confirmAction) {
        "downloads" -> ConfirmActionDialog(
            title = "Clear all downloads?",
            message = "All downloaded audio files in Library → Downloads will be deleted.",
            confirmLabel = "Clear",
            isDanger = true,
            onDismiss = { confirmAction = null },
            onConfirm = { confirmAction = null; onClearDownloads() }
        )
        "history" -> ConfirmActionDialog(
            title = "Clear recently played?",
            message = "This removes all songs from your listening history.",
            confirmLabel = "Clear",
            isDanger = true,
            onDismiss = { confirmAction = null },
            onConfirm = { confirmAction = null; onClearHistory() }
        )
        "searches" -> ConfirmActionDialog(
            title = "Clear search history?",
            message = "This removes all recent search queries.",
            confirmLabel = "Clear",
            isDanger = true,
            onDismiss = { confirmAction = null },
            onConfirm = { confirmAction = null; onClearRecentSearches() }
        )
        "reset" -> ConfirmActionDialog(
            title = "Reset Pulse?",
            message = "Playlists, downloads, liked songs, history and settings on this device will be deleted. This can’t be undone.",
            confirmLabel = "Reset",
            isDanger = true,
            onDismiss = { confirmAction = null },
            onConfirm = { confirmAction = null; onResetPulse() }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = PulseTextMuted,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    desc: String,
    valueText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(PulseSurface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PulseTextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = PulseTextPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = PulseTextMuted)
        }
        if (!valueText.isNullOrBlank()) {
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = PulseTextMuted)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PulseTextMuted)
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(PulseSurface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PulseTextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = PulseTextPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = PulseTextMuted)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PulsePrimaryStrong,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = PulseSurface3
            )
        )
    }
}
