package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadedTrack
import com.example.data.model.Track

@Entity(tableName = "liked_tracks")
data class LikedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int?,
    val thumbUrl: String,
    val album: String?,
    val isTopic: Boolean,
    val isVerified: Boolean,
    val isMusicCatalog: Boolean,
    val likedAt: Long = System.currentTimeMillis()
) {
    fun toTrack() = Track(
        id = id,
        title = title,
        artist = artist,
        durationSeconds = durationSeconds,
        thumbUrl = thumbUrl,
        album = album,
        isTopic = isTopic,
        isVerified = isVerified,
        isMusicCatalog = isMusicCatalog
    )

    companion object {
        fun fromTrack(t: Track) = LikedTrackEntity(
            id = t.id,
            title = t.title,
            artist = t.artist,
            durationSeconds = t.durationSeconds,
            thumbUrl = t.thumbUrl,
            album = t.album,
            isTopic = t.isTopic,
            isVerified = t.isVerified,
            isMusicCatalog = t.isMusicCatalog
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val name: String,
    val customCoverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistName", "trackId"])
data class PlaylistTrackEntity(
    val playlistName: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int?,
    val thumbUrl: String,
    val album: String?,
    val isTopic: Boolean,
    val isVerified: Boolean,
    val isMusicCatalog: Boolean,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrack() = Track(
        id = trackId,
        title = title,
        artist = artist,
        durationSeconds = durationSeconds,
        thumbUrl = thumbUrl,
        album = album,
        isTopic = isTopic,
        isVerified = isVerified,
        isMusicCatalog = isMusicCatalog
    )

    companion object {
        fun fromTrack(playlistName: String, t: Track, position: Int) = PlaylistTrackEntity(
            playlistName = playlistName,
            trackId = t.id,
            title = t.title,
            artist = t.artist,
            durationSeconds = t.durationSeconds,
            thumbUrl = t.thumbUrl,
            album = t.album,
            isTopic = t.isTopic,
            isVerified = t.isVerified,
            isMusicCatalog = t.isMusicCatalog,
            position = position
        )
    }
}

@Entity(tableName = "history_tracks")
data class HistoryTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int?,
    val thumbUrl: String,
    val album: String?,
    val isTopic: Boolean,
    val isVerified: Boolean,
    val isMusicCatalog: Boolean,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toTrack() = Track(
        id = id,
        title = title,
        artist = artist,
        durationSeconds = durationSeconds,
        thumbUrl = thumbUrl,
        album = album,
        isTopic = isTopic,
        isVerified = isVerified,
        isMusicCatalog = isMusicCatalog
    )

    companion object {
        fun fromTrack(t: Track) = HistoryTrackEntity(
            id = t.id,
            title = t.title,
            artist = t.artist,
            durationSeconds = t.durationSeconds,
            thumbUrl = t.thumbUrl,
            album = t.album,
            isTopic = t.isTopic,
            isVerified = t.isVerified,
            isMusicCatalog = t.isMusicCatalog
        )
    }
}

@Entity(tableName = "artist_play_counts")
data class ArtistPlayCountEntity(
    @PrimaryKey val artistName: String,
    val playCount: Int = 1
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int?,
    val thumbUrl: String,
    val album: String?,
    val isTopic: Boolean,
    val isVerified: Boolean,
    val isMusicCatalog: Boolean,
    val localAudioPath: String,
    val localArtworkPath: String?,
    val fileSizeBytes: Long,
    val quality: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val publicUri: String? = null,
    val publicFileName: String? = null
) {
    fun toDownloadedTrack() = DownloadedTrack(
        track = Track(
            id = id,
            title = title,
            artist = artist,
            durationSeconds = durationSeconds,
            thumbUrl = thumbUrl,
            album = album,
            isTopic = isTopic,
            isVerified = isVerified,
            isMusicCatalog = isMusicCatalog
        ),
        localAudioPath = localAudioPath,
        localArtworkPath = localArtworkPath,
        fileSizeBytes = fileSizeBytes,
        quality = quality,
        downloadedAt = downloadedAt,
        publicUri = publicUri,
        publicFileName = publicFileName
    )
}
