package com.example.data.repository

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.local.DownloadedTrackEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.PulseDao
import com.example.data.model.DownloadStateInfo
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadedTrack
import com.example.data.model.Track
import com.example.data.remote.MusicApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadRepository(
    context: Context,
    private val dao: PulseDao,
    private val api: MusicApiClient,
    private val prefs: PreferencesManager
) {
    private val appContext = context.applicationContext
    private val downloadsDir = File(appContext.filesDir, "downloads").apply { mkdirs() }
    private val artDir = File(downloadsDir, "art").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val inFlightStates = MutableStateFlow<Map<String, DownloadStateInfo>>(emptyMap())

    val downloadedTracks: Flow<List<DownloadedTrack>> = dao.observeDownloadedTracks().map { entities ->
        entities.mapNotNull { e ->
            val file = File(e.localAudioPath)
            if (file.exists() && file.length() > 0L) e.toDownloadedTrack() else null
        }
    }

    val downloadStates: StateFlow<Map<String, DownloadStateInfo>> = combine(
        dao.observeDownloadedTracks(),
        inFlightStates
    ) { dbList, active ->
        val map = HashMap<String, DownloadStateInfo>(dbList.size + active.size)
        dbList.forEach { entity ->
            val f = File(entity.localAudioPath)
            if (f.exists() && f.length() > 0L) {
                map[entity.id] = DownloadStateInfo(
                    trackId = entity.id,
                    status = DownloadStatus.DOWNLOADED,
                    progress = 1f,
                    fileSizeBytes = entity.fileSizeBytes,
                    localAudioPath = entity.localAudioPath,
                    localArtworkPath = entity.localArtworkPath,
                    publicUri = entity.publicUri
                )
            }
        }
        active.forEach { (id, info) ->
            map[id] = info
        }
        map
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    suspend fun getLocalAudioPathIfExists(trackId: String): String? {
        val entity = dao.getDownloadedTrackById(trackId) ?: return null
        val file = File(entity.localAudioPath)
        return if (file.exists() && file.length() > 1024L) file.absolutePath else null
    }

    /**
     * Ensures any songs previously cached inside Pulse's private Downloads folder
     * are also mirrored into the device's public Download/Pulse folder.
     */
    suspend fun syncExistingDownloadsToPublicFolder() = withContext(Dispatchers.IO) {
        val all = dao.getDownloadedTracksOnce()
        all.forEach { entity ->
            val localFile = File(entity.localAudioPath)
            if (!localFile.exists() || localFile.length() <= 1024L) return@forEach

            val hasValidPublic = entity.publicUri?.let { uriStr ->
                runCatching {
                    val uri = Uri.parse(uriStr)
                    if (uri.scheme == "file") {
                        File(uri.path.orEmpty()).exists()
                    } else {
                        appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                    }
                }.getOrDefault(false)
            } ?: false

            if (!hasValidPublic) {
                val track = entity.toDownloadedTrack().track
                val (pubUri, pubName) = exportToDeviceDownloadsFolder(track, localFile)
                if (pubUri != null) {
                    dao.upsertDownloadedTrack(
                        entity.copy(publicUri = pubUri, publicFileName = pubName)
                    )
                }
            }
        }
    }

    fun downloadTrack(track: Track) {
        val existingJob = activeJobs[track.id]
        if (existingJob != null && existingJob.isActive) return

        val targetFile = File(downloadsDir, "${track.id}.m4a")
        if (targetFile.exists() && targetFile.length() > 4096L && downloadStates.value[track.id]?.status == DownloadStatus.DOWNLOADED) {
            return
        }

        inFlightStates.update {
            it + (track.id to DownloadStateInfo(track.id, DownloadStatus.QUEUED, progress = 0f))
        }

        val job = scope.launch {
            try {
                val quality = prefs.getDownloadQualityOnce()
                inFlightStates.update {
                    it + (track.id to DownloadStateInfo(track.id, DownloadStatus.DOWNLOADING, progress = 0.02f))
                }

                val bundle = api.getStreamAndRelated(track.id, quality)
                val tmpFile = File(downloadsDir, "${track.id}.tmp")
                var downloadedBytes = 0L
                var success = false

                for (streamUrl in bundle.audioUrls) {
                    if (tmpFile.exists()) tmpFile.delete()
                    try {
                        downloadedBytes = streamToFile(streamUrl, tmpFile) { fraction ->
                            inFlightStates.update {
                                it + (track.id to DownloadStateInfo(
                                    trackId = track.id,
                                    status = DownloadStatus.DOWNLOADING,
                                    progress = fraction.coerceIn(0.03f, 0.96f)
                                ))
                            }
                        }
                        if (downloadedBytes > 8192L) {
                            success = true
                            break
                        }
                    } catch (ce: CancellationException) {
                        tmpFile.delete()
                        throw ce
                    } catch (_: Exception) {
                        tmpFile.delete()
                    }
                }

                if (!success) {
                    error("Unable to download audio stream")
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tmpFile.renameTo(targetFile)) {
                    tmpFile.copyTo(targetFile, overwrite = true)
                    tmpFile.delete()
                }

                val artPath = downloadArtwork(track)
                val (publicUri, publicFileName) = exportToDeviceDownloadsFolder(track, targetFile)

                dao.upsertDownloadedTrack(
                    DownloadedTrackEntity(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        durationSeconds = track.durationSeconds,
                        thumbUrl = track.thumbUrl,
                        album = track.album,
                        isTopic = track.isTopic,
                        isVerified = track.isVerified,
                        isMusicCatalog = track.isMusicCatalog,
                        localAudioPath = targetFile.absolutePath,
                        localArtworkPath = artPath,
                        fileSizeBytes = targetFile.length(),
                        quality = quality.label,
                        publicUri = publicUri,
                        publicFileName = publicFileName
                    )
                )

                inFlightStates.update { it - track.id }
            } catch (ce: CancellationException) {
                inFlightStates.update { it - track.id }
                throw ce
            } catch (e: Exception) {
                inFlightStates.update {
                    it + (track.id to DownloadStateInfo(
                        trackId = track.id,
                        status = DownloadStatus.FAILED,
                        progress = 0f,
                        errorMessage = e.message ?: "Download failed"
                    ))
                }
            } finally {
                activeJobs.remove(track.id)
            }
        }
        activeJobs[track.id] = job
    }

    fun downloadAll(tracks: List<Track>): Int {
        var started = 0
        val current = downloadStates.value
        tracks.forEach { t ->
            val st = current[t.id]?.status
            if (st != DownloadStatus.DOWNLOADED && st != DownloadStatus.DOWNLOADING && st != DownloadStatus.QUEUED) {
                downloadTrack(t)
                started++
            }
        }
        return started
    }

    fun cancelDownload(trackId: String) {
        activeJobs.remove(trackId)?.cancel()
        File(downloadsDir, "$trackId.tmp").delete()
        inFlightStates.update { it - trackId }
    }

    suspend fun removeDownload(trackId: String) = withContext(Dispatchers.IO) {
        activeJobs.remove(trackId)?.cancel()
        inFlightStates.update { it - trackId }
        val existing = dao.getDownloadedTrackById(trackId)
        deletePublicFile(existing?.publicUri)
        File(downloadsDir, "$trackId.m4a").delete()
        File(downloadsDir, "$trackId.tmp").delete()
        File(artDir, "$trackId.jpg").delete()
        dao.deleteDownloadedTrack(trackId)
    }

    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        inFlightStates.value = emptyMap()
        dao.getDownloadedTracksOnce().forEach { deletePublicFile(it.publicUri) }
        downloadsDir.listFiles()?.forEach { f ->
            if (f.isDirectory) f.deleteRecursively() else f.delete()
        }
        downloadsDir.mkdirs()
        artDir.mkdirs()
        dao.clearDownloadedTracks()
    }

    /**
     * Saves the downloaded audio file into the device's public Download/Pulse/ folder
     * so users can browse it in the Files app and play it in any external music player.
     */
    private fun exportToDeviceDownloadsFolder(track: Track, sourceFile: File): Pair<String?, String> {
        val fileName = buildPublicAudioFileName(track)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) return null to fileName

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = appContext.contentResolver
                val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/Pulse"

                // Remove any older duplicate with same display name in Download/Pulse
                runCatching {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                        arrayOf(fileName, "%Download/Pulse%")
                    )
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching null to fileName

                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                    out.flush()
                }

                val publishValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, publishValues, null, null)

                // Notify media scanner when physical path is available so external audio players index right away
                runCatching {
                    resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (idx >= 0) {
                                val path = cursor.getString(idx)
                                if (!path.isNullOrBlank()) {
                                    MediaScannerConnection.scanFile(
                                        appContext,
                                        arrayOf(path),
                                        arrayOf("audio/mp4"),
                                        null
                                    )
                                }
                            }
                        }
                    }
                }

                uri.toString() to fileName
            }.getOrElse { null to fileName }
        } else {
            runCatching {
                val hasPermission = ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) return@runCatching null to fileName

                @Suppress("DEPRECATION")
                val publicDownloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Pulse"
                ).apply { mkdirs() }
                val dest = File(publicDownloadDir, fileName)
                sourceFile.copyTo(dest, overwrite = true)
                MediaScannerConnection.scanFile(
                    appContext,
                    arrayOf(dest.absolutePath),
                    arrayOf("audio/mp4"),
                    null
                )
                Uri.fromFile(dest).toString() to fileName
            }.getOrElse { null to fileName }
        }
    }

    private fun deletePublicFile(publicUriStr: String?) {
        if (publicUriStr.isNullOrBlank()) return
        runCatching {
            val uri = Uri.parse(publicUriStr)
            if (uri.scheme == "file") {
                File(uri.path.orEmpty()).delete()
            } else {
                appContext.contentResolver.delete(uri, null, null)
            }
        }
    }

    private fun buildPublicAudioFileName(track: Track): String {
        val artist = track.artist.trim()
        val title = track.title.trim().ifBlank { track.id }
        val rawBase = if (artist.isNotBlank() && !title.contains(artist, ignoreCase = true)) {
            "$artist - $title"
        } else {
            title
        }
        val clean = rawBase
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(96)
            .ifBlank { track.id }
        return "$clean.m4a"
    }

    private fun streamToFile(url: String, target: File, onProgress: (Float) -> Unit): Long {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "PulseAndroid/1.0")
            .build()
        downloadClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body ?: error("Empty body")
            val total = body.contentLength()
            var readTotal = 0L
            var lastEmit = 0L
            body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        output.write(buf, 0, read)
                        readTotal += read
                        if (total > 0L) {
                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 150L) {
                                lastEmit = now
                                onProgress(readTotal.toFloat() / total.toFloat())
                            }
                        }
                    }
                    output.flush()
                }
            }
            return readTotal
        }
    }

    private fun downloadArtwork(track: Track): String? {
        val artFile = File(artDir, "${track.id}.jpg")
        if (artFile.exists() && artFile.length() > 512L) return artFile.absolutePath
        val urls = listOf(track.artworkUrl(512), track.fallbackArtworkUrl()).distinct()
        for (u in urls) {
            try {
                val req = Request.Builder().url(u).build()
                downloadClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bytes = resp.body?.bytes()
                        if (bytes != null && bytes.size > 512) {
                            artFile.writeBytes(bytes)
                            return artFile.absolutePath
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }
}
