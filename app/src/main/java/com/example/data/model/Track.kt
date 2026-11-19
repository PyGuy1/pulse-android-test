package com.example.data.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int? = null,
    val thumbUrl: String = "",
    val album: String? = null,
    val isTopic: Boolean = false,
    val isVerified: Boolean = false,
    val isMusicCatalog: Boolean = false,
    val isUserQueued: Boolean = false,
    val isPlayNext: Boolean = false,
    val isAutoplay: Boolean = false
) {
    fun artworkUrl(px: Int = 260): String {
        if (thumbUrl.contains("googleusercontent") || thumbUrl.contains("ggpht")) {
            return thumbUrl.replace(
                Regex("=(?:w\\d+-h\\d+|s\\d+)[^=?#]*$"),
                "=w$px-h$px-l90-rj"
            )
        }
        val quality = when {
            px >= 500 -> "maxresdefault"
            px >= 240 -> "hqdefault"
            else -> "mqdefault"
        }
        return "https://i.ytimg.com/vi/$id/$quality.jpg"
    }

    fun fallbackArtworkUrl(): String = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}

data class ArtistSummary(
    val name: String,
    val avatarUrl: String = "",
    val channelId: String = ""
)

data class PlaylistSummary(
    val id: String,
    val name: String,
    val thumbUrl: String = "",
    val artist: String = "",
    val trackCount: Int = 0,
    val isAlbum: Boolean = false
)

data class RemotePlaylistResult(
    val name: String,
    val thumbUrl: String,
    val artist: String,
    val tracks: List<Track>,
    val isApproximate: Boolean = false
)

data class MoodGenre(
    val label: String,
    val query: String,
    val colorHex: Long
)

data class UserPlaylist(
    val name: String,
    val customCoverUri: String? = null,
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class AudioQuality(val key: String, val label: String, val description: String, val targetBitrate: Int, val itag: Int) {
    LOW("low", "Low", "Saves data (48–64 kbps)", 50_000, 139),
    NORMAL("normal", "Normal", "Balanced quality (128 kbps)", 128_000, 140),
    HIGH("high", "High", "Best available audio", 1_000_000_000, 140);

    companion object {
        fun fromKey(key: String?): AudioQuality = entries.find { it.key == key } ?: NORMAL
    }
}

enum class DownloadStatus {
    NONE,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

data class DownloadStateInfo(
    val trackId: String,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val fileSizeBytes: Long = 0L,
    val localAudioPath: String? = null,
    val localArtworkPath: String? = null,
    val errorMessage: String? = null,
    val publicUri: String? = null
)

data class DownloadedTrack(
    val track: Track,
    val localAudioPath: String,
    val localArtworkPath: String?,
    val fileSizeBytes: Long,
    val quality: String,
    val downloadedAt: Long,
    val publicUri: String? = null,
    val publicFileName: String? = null
)

data class ImportCandidate(
    val sourceTitle: String,
    val sourceArtist: String,
    val sourceAlbum: String,
    val sourceDurationSeconds: Int?,
    val sourceArtUrl: String,
    val matchedTrack: Track?,
    val status: ImportMatchStatus,
    val isDuplicate: Boolean = false,
    val isSelected: Boolean = true
)

enum class ImportMatchStatus {
    PENDING,
    MATCHED,
    CHECK,
    NOT_FOUND
}

data class ImportPreview(
    val playlistName: String,
    val coverUrl: String,
    val sourceLabel: String,
    val items: List<ImportCandidate>,
    val isMatching: Boolean = false,
    val matchedCount: Int = 0,
    val totalToMatch: Int = 0
)

fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return ""
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = (ms / 1000L).toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb)
    else String.format(java.util.Locale.US, "%.0f KB", kb)
}
