package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PulseDao {

    // Liked Tracks
    @Query("SELECT * FROM liked_tracks ORDER BY likedAt DESC")
    fun observeLikedTracks(): Flow<List<LikedTrackEntity>>

    @Query("SELECT * FROM liked_tracks ORDER BY likedAt DESC")
    suspend fun getLikedTracksOnce(): List<LikedTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedTrack(track: LikedTrackEntity)

    @Query("DELETE FROM liked_tracks WHERE id = :trackId")
    suspend fun deleteLikedTrack(trackId: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    suspend fun getPlaylistsOnce(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_tracks ORDER BY position ASC, addedAt ASC")
    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY position ASC, addedAt ASC")
    suspend fun getAllPlaylistTracksOnce(): List<PlaylistTrackEntity>

    @Query("SELECT * FROM playlist_tracks WHERE playlistName = :playlistName ORDER BY position ASC, addedAt ASC")
    suspend fun getPlaylistTracksOnce(playlistName: String): List<PlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET customCoverUri = :coverUri WHERE name = :name")
    suspend fun updatePlaylistCover(name: String, coverUri: String?)

    @Query("DELETE FROM playlists WHERE name = :name")
    suspend fun deletePlaylist(name: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistName = :playlistName")
    suspend fun deleteAllTracksFromPlaylist(playlistName: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistName = :playlistName AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistName: String, trackId: String)

    @Transaction
    suspend fun renamePlaylist(oldName: String, newName: String, coverUri: String?) {
        val oldTracks = getPlaylistTracksOnce(oldName)
        insertPlaylist(PlaylistEntity(name = newName, customCoverUri = coverUri))
        val renamedTracks = oldTracks.map { it.copy(playlistName = newName) }
        insertPlaylistTracks(renamedTracks)
        deleteAllTracksFromPlaylist(oldName)
        deletePlaylist(oldName)
    }

    // History
    @Query("SELECT * FROM history_tracks ORDER BY playedAt DESC LIMIT 300")
    fun observeHistory(): Flow<List<HistoryTrackEntity>>

    @Query("SELECT * FROM history_tracks ORDER BY playedAt DESC LIMIT 300")
    suspend fun getHistoryOnce(): List<HistoryTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoryTrack(track: HistoryTrackEntity)

    @Query("DELETE FROM history_tracks WHERE id = :trackId")
    suspend fun deleteHistoryTrack(trackId: String)

    @Query("DELETE FROM history_tracks")
    suspend fun clearHistory()

    // Artist Play Counts
    @Query("SELECT * FROM artist_play_counts ORDER BY playCount DESC")
    fun observeArtistPlayCounts(): Flow<List<ArtistPlayCountEntity>>

    @Query("SELECT * FROM artist_play_counts ORDER BY playCount DESC")
    suspend fun getArtistPlayCountsOnce(): List<ArtistPlayCountEntity>

    @Query("SELECT playCount FROM artist_play_counts WHERE artistName = :artistName")
    suspend fun getArtistPlayCount(artistName: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtistPlayCount(entity: ArtistPlayCountEntity)

    // Recent Searches
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 12")
    fun observeRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()

    // Downloaded Tracks (Downloads Folder in Library)
    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAt DESC")
    fun observeDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAt DESC")
    suspend fun getDownloadedTracksOnce(): List<DownloadedTrackEntity>

    @Query("SELECT * FROM downloaded_tracks WHERE id = :trackId LIMIT 1")
    suspend fun getDownloadedTrackById(trackId: String): DownloadedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadedTrack(track: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE id = :trackId")
    suspend fun deleteDownloadedTrack(trackId: String)

    @Query("DELETE FROM downloaded_tracks")
    suspend fun clearDownloadedTracks()

    @Query("DELETE FROM liked_tracks")
    suspend fun clearLikedTracks()

    @Query("DELETE FROM playlists")
    suspend fun clearPlaylists()

    @Query("DELETE FROM playlist_tracks")
    suspend fun clearPlaylistTracks()

    @Query("DELETE FROM artist_play_counts")
    suspend fun clearArtistPlayCounts()
}
