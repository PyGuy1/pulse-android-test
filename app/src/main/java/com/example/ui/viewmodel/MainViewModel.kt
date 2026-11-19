package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.AudioQuality
import com.example.data.model.DownloadStateInfo
import com.example.data.model.DownloadedTrack
import com.example.data.model.ImportMatchStatus
import com.example.data.model.ImportPreview
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.data.remote.MusicConstants
import com.example.data.remote.MusicFilter
import com.example.service.PlaybackUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class MainViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val repo = container.musicRepository
    private val downloads = container.downloadRepository
    private val prefs = container.prefs
    val playback = container.playbackController

    val playbackState: StateFlow<PlaybackUiState> = playback.state
    val messages: SharedFlow<String> = playback.messages

    val likedTracks: StateFlow<List<Track>> = repo.likedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedIds: StateFlow<Set<String>> = repo.likedIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val userPlaylists: StateFlow<List<UserPlaylist>> = repo.userPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyTracks: StateFlow<List<Track>> = repo.historyTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTracks: StateFlow<List<DownloadedTrack>> = downloads.downloadedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadStates: StateFlow<Map<String, DownloadStateInfo>> = downloads.downloadStates

    val autoplay: StateFlow<Boolean> = prefs.autoplay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val audioQuality: StateFlow<AudioQuality> = prefs.audioQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioQuality.NORMAL)

    val downloadQuality: StateFlow<AudioQuality> = prefs.downloadQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioQuality.HIGH)

    val reduceMotion: StateFlow<Boolean> = prefs.reduceMotion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val customPipedUrl: StateFlow<String> = prefs.customPipedUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val regionCode: StateFlow<String> = combine(
        prefs.regionOverride,
        prefs.detectedRegion
    ) { override, detected ->
        override ?: detected ?: MusicConstants.detectDeviceCountryCode()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MusicConstants.detectDeviceCountryCode())

    val isRegionOverride: StateFlow<Boolean> = prefs.regionOverride
        .combine(prefs.detectedRegion) { ov, _ -> ov != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _importBusy = MutableStateFlow(false)
    val importBusy: StateFlow<Boolean> = _importBusy.asStateFlow()

    fun toast(msg: String) = playback.emitMessage(msg)

    fun playTracks(tracks: List<Track>, index: Int, radioMode: Boolean, sourceLabel: String = "Your queue") {
        playback.playTracks(tracks, index, radioMode, sourceLabel)
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun next() = playback.next()
    fun prev() = playback.prev()
    fun seekTo(ms: Long) = playback.seekTo(ms)
    fun toggleShuffle() = playback.toggleShuffle()
    fun cycleRepeatMode() = playback.cycleRepeatMode()
    fun jumpToQueueIndex(index: Int) = playback.jumpToQueueIndex(index)
    fun enqueue(track: Track, playNext: Boolean) = playback.enqueue(track, playNext)
    fun removeFromQueue(index: Int) = playback.removeFromQueue(index)
    fun clearUpcoming() = playback.clearUpcoming()

    fun saveQueueAsPlaylist(name: String) {
        val tracks = playbackState.value.queue.distinctBy { it.id }
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            if (repo.createPlaylist(name, tracks)) {
                toast("Saved ${tracks.size} songs to “$name”")
            } else {
                toast("Choose a different playlist name")
            }
        }
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            val nowLiked = repo.toggleLike(track)
            toast(if (nowLiked) "Added to Liked Songs" else "Removed from Liked Songs")
        }
    }

    fun downloadTrack(track: Track) {
        downloads.downloadTrack(track)
        toast("Saving “${track.title}” to Downloads & Device Download/Pulse")
    }

    fun downloadAll(tracks: List<Track>) {
        val count = downloads.downloadAll(tracks)
        if (count > 0) {
            toast("Saving $count song${if (count == 1) "" else "s"} to Downloads & Device Download/Pulse")
        } else {
            toast("All songs are already downloaded")
        }
    }

    fun cancelDownload(trackId: String) = downloads.cancelDownload(trackId)

    fun removeDownload(trackId: String) {
        viewModelScope.launch {
            downloads.removeDownload(trackId)
            toast("Removed from Downloads")
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloads.clearAllDownloads()
            toast("All downloads cleared")
        }
    }

    fun createPlaylist(name: String, onCreated: (String) -> Unit = {}) {
        val clean = name.trim()
        if (clean.isBlank()) return
        if (clean.equals("Liked Songs", ignoreCase = true) || userPlaylists.value.any { it.name == clean }) {
            toast("You already have a playlist with that name")
            return
        }
        viewModelScope.launch {
            if (repo.createPlaylist(clean)) {
                onCreated(clean)
            }
        }
    }

    fun addTrackToPlaylist(playlistName: String, track: Track) {
        viewModelScope.launch {
            if (playlistName == "Liked Songs") {
                repo.setLiked(track, true)
            } else {
                repo.addTrackToPlaylist(playlistName, track)
            }
            toast("Added to $playlistName")
        }
    }

    fun removeTrackFromPlaylist(playlistName: String, trackId: String) {
        viewModelScope.launch {
            repo.removeTrackFromPlaylist(playlistName, trackId)
            toast("Removed from $playlistName")
        }
    }

    fun updatePlaylist(oldName: String, newName: String, coverUri: String?, clearCover: Boolean) {
        val cleanNew = newName.trim()
        if (cleanNew.isBlank()) {
            toast("Give it a name")
            return
        }
        if (oldName != cleanNew && (cleanNew.equals("Liked Songs", ignoreCase = true) || userPlaylists.value.any { it.name == cleanNew })) {
            toast("You already have a playlist with that name")
            return
        }
        viewModelScope.launch {
            repo.updatePlaylist(oldName, cleanNew, coverUri, clearCover)
            toast("Saved")
        }
    }

    fun deletePlaylist(name: String) {
        viewModelScope.launch {
            repo.deletePlaylist(name)
            toast("Playlist deleted")
        }
    }

    fun startSleepTimer(minutes: Int, endOfSong: Boolean = false) = playback.startSleepTimer(minutes, endOfSong)
    fun clearSleepTimer() = playback.clearSleepTimer()

    fun setAutoplay(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoplay(enabled)
            toast("Autoplay ${if (enabled) "on" else "off"}")
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            prefs.setAudioQuality(quality)
            repo.api.clearStreamCache()
            toast("Applies to the next song")
        }
    }

    fun setDownloadQuality(quality: AudioQuality) {
        viewModelScope.launch {
            prefs.setDownloadQuality(quality)
            toast("Download quality: ${quality.label}")
        }
    }

    fun setRegion(regionCode: String?) {
        viewModelScope.launch {
            prefs.setRegionOverride(regionCode)
            repo.api.clearCache()
        }
    }

    fun setReduceMotion(reduce: Boolean) {
        viewModelScope.launch { prefs.setReduceMotion(reduce) }
    }

    fun setCustomPipedUrl(url: String) {
        viewModelScope.launch {
            prefs.setCustomPipedUrl(url)
            repo.api.clearCache()
            toast(if (url.isBlank()) "Using automatic servers" else "Custom server saved")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearHistory()
            toast("History cleared")
        }
    }

    fun removeHistoryTrack(trackId: String) {
        viewModelScope.launch { repo.removeHistoryTrack(trackId) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repo.clearRecentSearches()
            toast("Search history cleared")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            downloads.clearAllDownloads()
            repo.resetAllUserData()
            toast("Pulse reset complete")
        }
    }

    // Import & Export
    fun clearImportState() {
        _importPreview.value = null
        _importError.value = null
        _importBusy.value = false
    }

    fun startUrlImport(rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            _importError.value = "Paste a playlist link first."
            return
        }
        _importError.value = null
        _importBusy.value = true

        viewModelScope.launch {
            try {
                val spotifyMatch = Regex("(?:open\\.spotify\\.com/(?:intl-[a-z]+/)?|spotify:)(playlist|album)[/:]([A-Za-z0-9]{10,})").find(trimmed)
                val ytmMatch = Regex("[?&]list=([\\w-]{10,})").find(trimmed)
                    ?: Regex("^(PL[\\w-]{10,}|OLAK5uy_[\\w-]+|RDCLAK[\\w-]+|VL[\\w-]+)$").find(trimmed)

                val (name, cover, rawItems, sourceLabel) = when {
                    spotifyMatch != null -> {
                        val (n, c, items) = repo.api.fetchSpotifyPlaylist(spotifyMatch.groupValues[1], spotifyMatch.groupValues[2])
                        Quad(n, c, items, "Spotify")
                    }
                    ytmMatch != null -> {
                        val listId = ytmMatch.groupValues[1].removePrefix("VL")
                        val (n, c, items) = repo.api.fetchYtmPlaylistForImport(listId)
                        Quad(n, c, items, "YouTube Music")
                    }
                    else -> {
                        _importError.value = "That doesn’t look like a Spotify or YouTube Music playlist link."
                        _importBusy.value = false
                        return@launch
                    }
                }
                beginPreviewMatching(name, cover, sourceLabel, rawItems)
            } catch (e: Exception) {
                _importError.value = "Couldn’t fetch that playlist. Make sure it is public or try importing as CSV/JSON."
            } finally {
                _importBusy.value = false
            }
        }
    }

    fun startFileImport(fileName: String, textContent: String) {
        _importError.value = null
        viewModelScope.launch {
            try {
                if (fileName.endsWith(".json", ignoreCase = true)) {
                    val obj = try { JSONObject(textContent) } catch (_: Exception) { null }
                    if (obj != null && obj.optString("app") == "Pulse" && obj.has("playlists") && obj.optJSONObject("playlists") != null) {
                        val count = repo.restoreBackupJson(textContent)
                        toast("Restored $count songs from backup")
                        clearImportState()
                        return@launch
                    }
                }
                val (name, sourceLabel, rawItems) = MusicFilter.parseImportFile(fileName, textContent)
                if (rawItems.isEmpty()) {
                    _importError.value = "No songs found in that file."
                    return@launch
                }
                beginPreviewMatching(name, "", sourceLabel, rawItems)
            } catch (_: Exception) {
                _importError.value = "Couldn’t read that file. Use CSV, JSON, TXT, or M3U."
            }
        }
    }

    private fun beginPreviewMatching(
        name: String,
        cover: String,
        sourceLabel: String,
        rawItems: List<com.example.data.model.ImportCandidate>
    ) {
        val pendingCount = rawItems.count { it.status == ImportMatchStatus.PENDING }
        val initial = ImportPreview(
            playlistName = name.take(60),
            coverUrl = cover,
            sourceLabel = sourceLabel,
            items = recomputeDuplicates(name, rawItems),
            isMatching = pendingCount > 0,
            matchedCount = 0,
            totalToMatch = pendingCount
        )
        _importPreview.value = initial
        if (pendingCount == 0) return

        viewModelScope.launch {
            val items = initial.items.toMutableList()
            val pendingIndices = items.indices.filter { items[it].status == ImportMatchStatus.PENDING }
            var done = 0
            pendingIndices.chunked(3).forEach { batch ->
                if (_importPreview.value == null) return@launch
                batch.map { idx ->
                    async {
                        val (matched, status) = repo.api.matchImportTrack(items[idx])
                        items[idx] = items[idx].copy(
                            matchedTrack = matched,
                            status = status,
                            isSelected = matched != null && status != ImportMatchStatus.NOT_FOUND
                        )
                        done++
                    }
                }.awaitAll()
                val cur = _importPreview.value ?: return@launch
                _importPreview.value = cur.copy(
                    items = recomputeDuplicates(cur.playlistName, items),
                    matchedCount = done
                )
            }
            _importPreview.update { cur ->
                cur?.copy(
                    items = recomputeDuplicates(cur.playlistName, cur.items),
                    isMatching = false
                )
            }
        }
    }

    private fun recomputeDuplicates(
        playlistName: String,
        items: List<com.example.data.model.ImportCandidate>
    ): List<com.example.data.model.ImportCandidate> {
        val existingTracks = userPlaylists.value.find { it.name == playlistName.trim() }?.tracks.orEmpty()
        val ids = existingTracks.map { it.id }.toMutableSet()
        val keys = existingTracks.mapNotNull { MusicFilter.trackKey(it).takeIf { k -> k.isNotEmpty() } }.toMutableSet()
        return items.map { c ->
            val t = c.matchedTrack
            if (t == null) {
                c.copy(isDuplicate = false)
            } else {
                val tk = MusicFilter.trackKey(t)
                val dup = t.id in ids || (tk.isNotEmpty() && tk in keys)
                ids.add(t.id)
                if (tk.isNotEmpty()) keys.add(tk)
                c.copy(isDuplicate = dup, isSelected = if (dup) false else c.isSelected)
            }
        }
    }

    fun updateImportName(newName: String) {
        _importPreview.update { cur ->
            cur?.copy(
                playlistName = newName.take(60),
                items = recomputeDuplicates(newName, cur.items)
            )
        }
    }

    fun toggleImportCandidate(index: Int) {
        _importPreview.update { cur ->
            if (cur == null || index !in cur.items.indices) return@update cur
            val list = cur.items.toMutableList()
            val item = list[index]
            if (item.matchedTrack != null) {
                list[index] = item.copy(isSelected = !item.isSelected)
            }
            cur.copy(items = list)
        }
    }

    fun commitImport(onSuccess: (String) -> Unit) {
        val cur = _importPreview.value ?: return
        val name = cur.playlistName.trim().ifBlank { "Imported playlist" }.take(60)
        val selected = cur.items.filter { it.isSelected && it.matchedTrack != null }
            .map { it.matchedTrack!!.copy(album = it.sourceAlbum.ifBlank { it.matchedTrack.album }) }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repo.addTracksToPlaylist(name, selected)
            _importPreview.value = null
            toast("Imported ${selected.size} song${if (selected.size == 1) "" else "s"} into “$name”")
            onSuccess(name)
        }
    }

    fun sharePlaylistExport(context: Context, name: String, tracks: List<Track>, format: String) {
        if (tracks.isEmpty()) {
            toast("Nothing to export yet")
            return
        }
        viewModelScope.launch {
            val (fileName, mimeType, body) = MusicFilter.exportPlaylist(name, tracks, format)
            shareTextFile(context, fileName, mimeType, body)
        }
    }

    fun shareBackupExport(context: Context) {
        viewModelScope.launch {
            val json = repo.createBackupJson()
            shareTextFile(context, "Pulse backup.json", "application/json", json)
        }
    }

    fun restoreBackupJson(jsonText: String) {
        viewModelScope.launch {
            try {
                val count = repo.restoreBackupJson(jsonText)
                toast("Restored $count songs")
            } catch (_: Exception) {
                toast("That isn’t a valid Pulse backup")
            }
        }
    }

    private suspend fun shareTextFile(context: Context, fileName: String, mimeType: String, content: String) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val out = File(dir, fileName)
            out.writeText(content)
            out
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export $fileName"))
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
