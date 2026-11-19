package com.example.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.local.PreferencesManager
import com.example.data.model.Track
import com.example.data.remote.MusicFilter
import com.example.data.repository.DownloadRepository
import com.example.data.repository.MusicRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

enum class RepeatModeState {
    OFF,
    ONE,
    ALL
}

data class PlaybackUiState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isOfflinePlayback: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val radioMode: Boolean = false,
    val sourceLabel: String = "Your queue",
    val sleepTimerEndMs: Long = 0L,
    val sleepAfterSong: Boolean = false,
    val sleepRemainingMinutes: Int = 0
)

class PlaybackController(
    context: Context,
    private val repository: MusicRepository,
    private val downloadRepository: DownloadRepository,
    private val prefs: PreferencesManager
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var shuffleSnapshot: List<Track>? = null
    private var queueGeneration = 0
    private var fillJob: Job? = null
    private var loadJob: Job? = null
    private var sleepJob: Job? = null
    private var sleepTickerJob: Job? = null
    private val loadToken = AtomicInteger(0)

    private var candidateUrls: List<String> = emptyList()
    private var candidateUrlIndex = 0
    private var pendingSeekMs = 0L
    private var isRestoredSession = false
    private var restoredSeekMs = 0L
    private var consecutiveErrors = 0

    init {
        connectService()
        restoreSession()
        startProgressLoop()
    }

    private fun connectService() {
        val token = SessionToken(appContext, ComponentName(appContext, PulsePlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val ctrl = future.get()
                    mediaController = ctrl
                    attachPlayerListener(ctrl)
                } catch (_: Exception) {}
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun attachPlayerListener(ctrl: MediaController) {
        ctrl.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _state.update { it.copy(isBuffering = true) }
                    Player.STATE_READY -> {
                        consecutiveErrors = 0
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                durationMs = ctrl.duration.takeIf { d -> d > 0 } ?: it.durationMs
                            )
                        }
                        if (pendingSeekMs > 0L) {
                            ctrl.seekTo(pendingSeekMs)
                            pendingSeekMs = 0L
                        }
                    }
                    Player.STATE_ENDED -> onTrackEnded()
                    Player.STATE_IDLE -> _state.update { it.copy(isBuffering = false) }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val track = _state.value.currentTrack ?: return
                if (candidateUrlIndex + 1 < candidateUrls.size) {
                    candidateUrlIndex++
                    val nextUrl = candidateUrls[candidateUrlIndex]
                    setMediaOnController(track, Uri.parse(nextUrl), isOffline = false, autoPlay = true)
                } else {
                    consecutiveErrors++
                    _state.update { it.copy(isBuffering = false, isPlaying = false) }
                    if (consecutiveErrors <= 3) {
                        _messages.tryEmit("Unable to play “${track.title}”. Skipping…")
                        scope.launch {
                            delay(800)
                            next()
                        }
                    } else {
                        _messages.tryEmit("Servers busy. Try another song or play from Downloads.")
                    }
                }
            }
        })
    }

    private fun startProgressLoop() {
        scope.launch {
            var tick = 0
            while (isActive) {
                val ctrl = mediaController
                if (ctrl != null && _state.value.currentTrack != null && !isRestoredSession) {
                    val pos = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration.takeIf { it > 0L }
                        ?: ((_state.value.currentTrack?.durationSeconds ?: 0) * 1000L)
                    _state.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            isPlaying = ctrl.isPlaying
                        )
                    }
                    if (ctrl.isPlaying && tick % 10 == 0) {
                        saveSessionAsync()
                    }
                }
                tick++
                delay(500L)
            }
        }
    }

    fun hasNextInQueue(): Boolean {
        val s = _state.value
        return s.queue.isNotEmpty() && (s.currentIndex + 1 < s.queue.size || s.repeatMode == RepeatModeState.ALL)
    }

    fun hasPrevInQueue(): Boolean = _state.value.currentIndex > 0

    fun playTracks(tracks: List<Track>, index: Int, radioMode: Boolean, sourceLabel: String = "Your queue") {
        if (tracks.isEmpty() || index !in tracks.indices) return
        val initialQueue = if (radioMode) listOf(tracks[index]) else tracks.toList()
        val startIdx = if (radioMode) 0 else index

        queueGeneration++
        fillJob?.cancel()
        fillJob = null
        shuffleSnapshot = null
        isRestoredSession = false
        restoredSeekMs = 0L
        pendingSeekMs = 0L
        consecutiveErrors = 0

        val finalQueue = if (_state.value.shuffleEnabled && !radioMode) {
            shuffleUpcomingList(initialQueue, startIdx)
        } else {
            initialQueue
        }

        _state.update {
            it.copy(
                queue = finalQueue,
                currentIndex = startIdx,
                currentTrack = finalQueue[startIdx],
                radioMode = radioMode,
                sourceLabel = if (radioMode) "Radio · ${finalQueue[startIdx].title.take(34)}" else sourceLabel,
                currentPositionMs = 0L,
                durationMs = (finalQueue[startIdx].durationSeconds ?: 0) * 1000L
            )
        }

        if (radioMode) {
            fillQueue(40, force = true)
        }
        loadCurrentTrack()
    }

    fun jumpToQueueIndex(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        if (index == s.currentIndex) {
            togglePlayPause()
            return
        }
        isRestoredSession = false
        _state.update {
            it.copy(
                currentIndex = index,
                currentTrack = s.queue[index],
                currentPositionMs = 0L
            )
        }
        loadCurrentTrack()
    }

    fun enqueue(track: Track, playNext: Boolean) {
        val s = _state.value
        if (s.queue.isEmpty() || s.currentIndex < 0) {
            playTracks(listOf(track), 0, radioMode = true)
            return
        }
        val flagged = track.copy(isUserQueued = true, isPlayNext = playNext, isAutoplay = false)
        val updated = s.queue.toMutableList()
        var insertPos = s.currentIndex + 1
        if (playNext) {
            while (insertPos < updated.size && updated[insertPos].isPlayNext) insertPos++
        } else {
            while (insertPos < updated.size && updated[insertPos].isUserQueued) insertPos++
        }
        updated.add(insertPos, flagged)
        _state.update { it.copy(queue = updated) }
        _messages.tryEmit(if (playNext) "Playing next" else "Added to queue")
        saveSessionAsync()
    }

    fun removeFromQueue(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices || index == s.currentIndex) return
        val updated = s.queue.toMutableList()
        updated.removeAt(index)
        val newIdx = if (index < s.currentIndex) s.currentIndex - 1 else s.currentIndex
        _state.update { it.copy(queue = updated, currentIndex = newIdx) }
        saveSessionAsync()
        topUpQueue()
    }

    fun clearUpcoming() {
        val s = _state.value
        if (s.currentIndex !in s.queue.indices) return
        val kept = s.queue.take(s.currentIndex + 1)
        shuffleSnapshot = null
        _state.update { it.copy(queue = kept) }
        _messages.tryEmit("Upcoming songs cleared")
        saveSessionAsync()
        scope.launch {
            if (prefs.getAutoplayOnce()) fillQueue(25, force = true)
        }
    }

    fun toggleShuffle() {
        val s = _state.value
        val newShuffle = !s.shuffleEnabled
        if (s.queue.isEmpty() || s.currentIndex < 0) {
            _state.update { it.copy(shuffleEnabled = newShuffle) }
            return
        }
        val updatedQueue = if (newShuffle) {
            shuffleUpcomingList(s.queue, s.currentIndex)
        } else {
            unshuffleUpcomingList(s.queue, s.currentIndex)
        }
        _state.update { it.copy(shuffleEnabled = newShuffle, queue = updatedQueue) }
        saveSessionAsync()
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            RepeatModeState.OFF -> RepeatModeState.ALL
            RepeatModeState.ALL -> RepeatModeState.ONE
            RepeatModeState.ONE -> RepeatModeState.OFF
        }
        _state.update { it.copy(repeatMode = next) }
        val msg = when (next) {
            RepeatModeState.OFF -> "Repeat off"
            RepeatModeState.ALL -> "Repeating queue"
            RepeatModeState.ONE -> "Repeating current song"
        }
        _messages.tryEmit(msg)
    }

    fun togglePlayPause() {
        val s = _state.value
        if (s.queue.isEmpty() || s.currentIndex < 0) return
        if (isRestoredSession) {
            isRestoredSession = false
            pendingSeekMs = restoredSeekMs
            restoredSeekMs = 0L
            loadCurrentTrack()
            return
        }
        val ctrl = mediaController ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else {
            if (ctrl.playbackState == Player.STATE_IDLE || ctrl.mediaItemCount == 0) {
                loadCurrentTrack()
            } else {
                ctrl.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val ctrl = mediaController
        if (ctrl != null && !isRestoredSession) {
            ctrl.seekTo(positionMs.coerceAtLeast(0L))
            _state.update { it.copy(currentPositionMs = positionMs) }
        }
    }

    fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        scope.launch {
            val sNow = _state.value
            if (sNow.currentIndex + 1 >= sNow.queue.size) {
                if (sNow.repeatMode == RepeatModeState.ALL) {
                    _state.update {
                        it.copy(
                            currentIndex = 0,
                            currentTrack = sNow.queue[0],
                            currentPositionMs = 0L
                        )
                    }
                    loadCurrentTrack()
                    return@launch
                }
                if (prefs.getAutoplayOnce()) {
                    _state.update { it.copy(isBuffering = true) }
                    fillQueueAndAwait(20, force = true)
                }
                val afterFill = _state.value
                if (afterFill.currentIndex + 1 >= afterFill.queue.size) {
                    _state.update { it.copy(isBuffering = false, isPlaying = false) }
                    mediaController?.pause()
                    mediaController?.seekTo(0L)
                    return@launch
                }
            }
            val latest = _state.value
            val nextIdx = latest.currentIndex + 1
            if (nextIdx in latest.queue.indices) {
                isRestoredSession = false
                _state.update {
                    it.copy(
                        currentIndex = nextIdx,
                        currentTrack = latest.queue[nextIdx],
                        currentPositionMs = 0L
                    )
                }
                loadCurrentTrack()
            }
        }
    }

    fun prev() {
        val ctrl = mediaController
        val pos = ctrl?.currentPosition ?: _state.value.currentPositionMs
        if (!hasPrevInQueue() || pos > 3000L) {
            seekTo(0L)
            return
        }
        val s = _state.value
        val prevIdx = s.currentIndex - 1
        if (prevIdx in s.queue.indices) {
            isRestoredSession = false
            _state.update {
                it.copy(
                    currentIndex = prevIdx,
                    currentTrack = s.queue[prevIdx],
                    currentPositionMs = 0L
                )
            }
            loadCurrentTrack()
        }
    }

    private fun onTrackEnded() {
        val s = _state.value
        if (s.sleepAfterSong) {
            clearSleepTimer()
            mediaController?.pause()
            _state.update { it.copy(isPlaying = false) }
            _messages.tryEmit("Sleep timer: playback stopped")
            return
        }
        if (s.repeatMode == RepeatModeState.ONE) {
            seekTo(0L)
            mediaController?.play()
            return
        }
        next()
    }

    private fun loadCurrentTrack() {
        val track = _state.value.currentTrack ?: return
        val token = loadToken.incrementAndGet()
        loadJob?.cancel()

        loadJob = scope.launch {
            _state.update {
                it.copy(
                    isBuffering = true,
                    currentTrack = track,
                    durationMs = (track.durationSeconds ?: 0) * 1000L
                )
            }

            repository.recordPlay(track)
            saveSessionAsync()
            topUpQueue()

            // Prefer local offline downloaded file if present!
            val localPath = downloadRepository.getLocalAudioPathIfExists(track.id)
            if (localPath != null) {
                if (token != loadToken.get()) return@launch
                candidateUrls = emptyList()
                candidateUrlIndex = 0
                setMediaOnController(track, Uri.fromFile(File(localPath)), isOffline = true, autoPlay = true)
                return@launch
            }

            try {
                val quality = prefs.getAudioQualityOnce()
                val bundle = repository.api.getStreamAndRelated(track.id, quality)
                if (token != loadToken.get()) return@launch
                candidateUrls = bundle.audioUrls
                candidateUrlIndex = 0
                if (candidateUrls.isEmpty()) error("No stream URLs")
                setMediaOnController(track, Uri.parse(candidateUrls[0]), isOffline = false, autoPlay = true)
                prefetchNextTrack()
            } catch (_: Exception) {
                if (token != loadToken.get()) return@launch
                _state.update { it.copy(isBuffering = false, isPlaying = false) }
                _messages.tryEmit("Stream unavailable for “${track.title}”.")
            }
        }
    }

    private fun setMediaOnController(track: Track, uri: Uri, isOffline: Boolean, autoPlay: Boolean) {
        val ctrl = mediaController
        if (ctrl == null) {
            scope.launch {
                delay(250)
                setMediaOnController(track, uri, isOffline, autoPlay)
            }
            return
        }
        val localArt = downloadRepository.downloadStates.value[track.id]?.localArtworkPath
        val artUri = if (localArt != null && File(localArt).exists()) {
            Uri.fromFile(File(localArt))
        } else {
            Uri.parse(track.artworkUrl(512))
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist.ifBlank { "Pulse" })
            .setAlbumTitle(track.album)
            .setArtworkUri(artUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()

        _state.update { it.copy(isOfflinePlayback = isOffline) }
        ctrl.setMediaItem(mediaItem)
        ctrl.prepare()
        ctrl.playWhenReady = autoPlay
    }

    private fun prefetchNextTrack() {
        val s = _state.value
        val nextTrack = s.queue.getOrNull(s.currentIndex + 1) ?: return
        scope.launch(Dispatchers.IO) {
            try {
                if (downloadRepository.getLocalAudioPathIfExists(nextTrack.id) == null) {
                    repository.api.getStreamAndRelated(nextTrack.id, prefs.getAudioQualityOnce())
                }
            } catch (_: Exception) {}
        }
    }

    private fun topUpQueue() {
        val s = _state.value
        val upcoming = s.queue.size - 1 - s.currentIndex
        val threshold = if (s.radioMode) 15 else 3
        if (upcoming < threshold) {
            scope.launch {
                if (prefs.getAutoplayOnce()) {
                    fillQueue(if (s.radioMode) 30 else 25, force = false)
                }
            }
        }
    }

    private fun fillQueue(want: Int, force: Boolean) {
        if (fillJob?.isActive == true) return
        fillJob = scope.launch {
            fillQueueAndAwait(want, force)
        }
    }

    private suspend fun fillQueueAndAwait(want: Int, force: Boolean) {
        val gen = queueGeneration
        val s = _state.value
        if (s.queue.isEmpty()) return
        if (!force && !prefs.getAutoplayOnce()) return

        val inQueue = s.queue.map { it.id }.toMutableSet()
        val addedTracks = ArrayList<Track>()
        val tried = HashSet<String>()

        for (step in 0 until 7) {
            if (addedTracks.size >= want || gen != queueGeneration) break
            val curQueue = _state.value.queue + addedTracks
            val seedCandidates = (listOfNotNull(curQueue.lastOrNull(), _state.value.currentTrack) +
                curQueue.takeLast(6).reversed()).filter { it.id !in tried }
            val cand = seedCandidates.firstOrNull() ?: break
            tried.add(cand.id)
            val sim = try { repository.getSimilarTracks(cand) } catch (_: Exception) { emptyList() }
            for (t in sim) {
                if (addedTracks.size >= want) break
                if (inQueue.add(t.id)) {
                    addedTracks.add(t.copy(isAutoplay = true))
                }
            }
        }

        if (gen == queueGeneration && addedTracks.size < minOf(want, 8)) {
            try {
                val region = prefs.getRegionOverrideOnce() ?: prefs.getDetectedRegionOnce() ?: "US"
                val chart = MusicFilter.clean(repository.getChart(region), 25)
                for (t in chart) {
                    if (addedTracks.size >= want) break
                    if (inQueue.add(t.id)) addedTracks.add(t.copy(isAutoplay = true))
                }
            } catch (_: Exception) {}
        }

        if (gen == queueGeneration && addedTracks.isNotEmpty()) {
            val batch = if (_state.value.shuffleEnabled) addedTracks.shuffled() else addedTracks
            _state.update { it.copy(queue = it.queue + batch) }
            saveSessionAsync()
        }
    }

    private fun shuffleUpcomingList(list: List<Track>, currentIdx: Int): List<Track> {
        val up = list.drop(currentIdx + 1)
        shuffleSnapshot = up
        val userQueued = up.filter { it.isUserQueued }
        val others = up.filterNot { it.isUserQueued }.shuffled()
        return list.take(currentIdx + 1) + userQueued + others
    }

    private fun unshuffleUpcomingList(list: List<Track>, currentIdx: Int): List<Track> {
        val snap = shuffleSnapshot
        shuffleSnapshot = null
        if (snap == null) return list
        val up = list.drop(currentIdx + 1)
        val keepIds = up.map { it.id }.toSet()
        val orig = snap.filter { it.id in keepIds }
        val origIds = orig.map { it.id }.toSet()
        val extra = up.filter { it.id !in origIds }
        return list.take(currentIdx + 1) + extra.filter { it.isUserQueued } + orig + extra.filterNot { it.isUserQueued }
    }

    // Sleep timer
    fun startSleepTimer(minutes: Int, endOfSong: Boolean = false) {
        clearSleepTimer()
        if (endOfSong) {
            _state.update { it.copy(sleepAfterSong = true, sleepTimerEndMs = 0L, sleepRemainingMinutes = 0) }
            _messages.tryEmit("Will stop after this song")
            return
        }
        val endMs = System.currentTimeMillis() + minutes * 60_000L
        _state.update { it.copy(sleepAfterSong = false, sleepTimerEndMs = endMs, sleepRemainingMinutes = minutes) }
        _messages.tryEmit("Sleep timer: $minutes min")

        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            val ctrl = mediaController
            if (ctrl != null) {
                val origVol = ctrl.volume
                for (step in 9 downTo 0) {
                    ctrl.volume = origVol * (step / 10f)
                    delay(250L)
                }
                ctrl.pause()
                ctrl.volume = origVol
            }
            clearSleepTimer()
            _state.update { it.copy(isPlaying = false) }
            _messages.tryEmit("Sleep timer: playback stopped")
        }

        sleepTickerJob = scope.launch {
            while (isActive) {
                val remaining = ((endMs - System.currentTimeMillis() + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                _state.update { it.copy(sleepRemainingMinutes = remaining) }
                delay(15_000L)
            }
        }
    }

    fun clearSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        sleepTickerJob?.cancel()
        sleepTickerJob = null
        _state.update { it.copy(sleepAfterSong = false, sleepTimerEndMs = 0L, sleepRemainingMinutes = 0) }
    }

    private fun saveSessionAsync() {
        val s = _state.value
        if (s.queue.isEmpty() || s.currentIndex !in s.queue.indices) return
        val start = maxOf(0, s.currentIndex - 60)
        val end = minOf(s.queue.size, s.currentIndex + 100)
        val slice = s.queue.subList(start, end)
        val relativeIdx = s.currentIndex - start
        val pos = s.currentPositionMs
        val radio = s.radioMode
        scope.launch(Dispatchers.IO) {
            val arr = JSONArray()
            slice.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("dur", t.durationSeconds ?: -1)
                    put("thumb", t.thumbUrl)
                    put("album", t.album ?: "")
                    put("tp", t.isTopic)
                    put("v", t.isVerified)
                    put("ms", t.isMusicCatalog)
                    put("uq", t.isUserQueued)
                    put("auto", t.isAutoplay)
                })
            }
            prefs.saveSession(arr.toString(), relativeIdx, pos, radio)
        }
    }

    private fun restoreSession() {
        scope.launch {
            val saved = prefs.loadSession() ?: return@launch
            if (_state.value.queue.isNotEmpty()) return@launch
            try {
                val arr = JSONArray(saved.queueJson)
                val restoredQueue = ArrayList<Track>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    restoredQueue.add(
                        Track(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            artist = o.optString("artist"),
                            durationSeconds = o.optInt("dur", -1).takeIf { it > 0 },
                            thumbUrl = o.optString("thumb"),
                            album = o.optString("album").takeIf { it.isNotBlank() },
                            isTopic = o.optBoolean("tp"),
                            isVerified = o.optBoolean("v"),
                            isMusicCatalog = o.optBoolean("ms"),
                            isUserQueued = o.optBoolean("uq"),
                            isAutoplay = o.optBoolean("auto")
                        )
                    )
                }
                if (restoredQueue.isNotEmpty()) {
                    val idx = saved.index.coerceIn(0, restoredQueue.lastIndex)
                    val track = restoredQueue[idx]
                    isRestoredSession = true
                    restoredSeekMs = saved.positionMs
                    _state.update {
                        it.copy(
                            queue = restoredQueue,
                            currentIndex = idx,
                            currentTrack = track,
                            radioMode = saved.radioMode,
                            currentPositionMs = saved.positionMs,
                            durationMs = (track.durationSeconds ?: 0) * 1000L,
                            sourceLabel = if (saved.radioMode) "Radio · ${track.title.take(34)}" else "Your queue"
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun emitMessage(msg: String) {
        _messages.tryEmit(msg)
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
