package com.example.data.repository

import com.example.data.local.ArtistPlayCountEntity
import com.example.data.local.HistoryTrackEntity
import com.example.data.local.LikedTrackEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.PulseDao
import com.example.data.local.RecentSearchEntity
import com.example.data.model.ArtistSummary
import com.example.data.model.MoodGenre
import com.example.data.model.PlaylistSummary
import com.example.data.model.RemotePlaylistResult
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.data.remote.MusicApiClient
import com.example.data.remote.MusicConstants
import com.example.data.remote.MusicFilter
import com.example.data.remote.MusicFilter.WeightedTrack
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class MusicRepository(
    private val dao: PulseDao,
    val prefs: PreferencesManager,
    val api: MusicApiClient
) {
    val likedTracks: Flow<List<Track>> = dao.observeLikedTracks().map { list -> list.map { it.toTrack() } }

    val likedIds: Flow<Set<String>> = likedTracks.map { list -> list.map { it.id }.toSet() }

    val userPlaylists: Flow<List<UserPlaylist>> = combine(
        dao.observePlaylists(),
        dao.observeAllPlaylistTracks()
    ) { pls, tracks ->
        val grouped = tracks.groupBy { it.playlistName }
        pls.map { pl ->
            UserPlaylist(
                name = pl.name,
                customCoverUri = pl.customCoverUri,
                tracks = (grouped[pl.name] ?: emptyList()).map { it.toTrack() },
                createdAt = pl.createdAt
            )
        }
    }

    val historyTracks: Flow<List<Track>> = dao.observeHistory().map { list -> list.map { it.toTrack() } }

    val artistPlayCounts: Flow<List<ArtistPlayCountEntity>> = dao.observeArtistPlayCounts()

    val recentSearches: Flow<List<String>> = dao.observeRecentSearches().map { list -> list.map { it.query } }

    suspend fun toggleLike(track: Track): Boolean {
        val current = dao.getLikedTracksOnce().any { it.id == track.id }
        if (current) {
            dao.deleteLikedTrack(track.id)
        } else {
            dao.insertLikedTrack(LikedTrackEntity.fromTrack(track))
        }
        api.clearForYouCache()
        return !current
    }

    suspend fun setLiked(track: Track, liked: Boolean) {
        if (liked) dao.insertLikedTrack(LikedTrackEntity.fromTrack(track))
        else dao.deleteLikedTrack(track.id)
        api.clearForYouCache()
    }

    suspend fun createPlaylist(name: String, initialTracks: List<Track> = emptyList()): Boolean {
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank() || cleanName.equals("Liked Songs", ignoreCase = true)) return false
        dao.insertPlaylist(PlaylistEntity(name = cleanName))
        if (initialTracks.isNotEmpty()) {
            addTracksToPlaylist(cleanName, initialTracks)
        }
        return true
    }

    suspend fun addTrackToPlaylist(playlistName: String, track: Track) {
        dao.insertPlaylist(PlaylistEntity(name = playlistName))
        val existing = dao.getPlaylistTracksOnce(playlistName)
        if (existing.none { it.trackId == track.id }) {
            val nextPos = (existing.maxOfOrNull { it.position } ?: -1) + 1
            dao.insertPlaylistTrack(PlaylistTrackEntity.fromTrack(playlistName, track, nextPos))
        }
    }

    suspend fun addTracksToPlaylist(playlistName: String, tracks: List<Track>) {
        dao.insertPlaylist(PlaylistEntity(name = playlistName))
        val existing = dao.getPlaylistTracksOnce(playlistName)
        val existingIds = existing.map { it.trackId }.toSet()
        var nextPos = (existing.maxOfOrNull { it.position } ?: -1) + 1
        val toInsert = tracks
            .distinctBy { it.id }
            .filter { it.id !in existingIds }
            .map { PlaylistTrackEntity.fromTrack(playlistName, it, nextPos++) }
        if (toInsert.isNotEmpty()) dao.insertPlaylistTracks(toInsert)
    }

    suspend fun removeTrackFromPlaylist(playlistName: String, trackId: String) {
        dao.removeTrackFromPlaylist(playlistName, trackId)
    }

    suspend fun updatePlaylist(oldName: String, newName: String, coverUri: String?, clearCover: Boolean = false) {
        val trimmed = newName.trim().take(60)
        if (trimmed.isBlank()) return
        if (oldName != trimmed) {
            dao.renamePlaylist(oldName, trimmed, if (clearCover) null else coverUri)
        } else if (clearCover || coverUri != null) {
            dao.updatePlaylistCover(oldName, if (clearCover) null else coverUri)
        }
    }

    suspend fun deletePlaylist(name: String) {
        dao.deleteAllTracksFromPlaylist(name)
        dao.deletePlaylist(name)
    }

    suspend fun recordPlay(track: Track) {
        dao.upsertHistoryTrack(HistoryTrackEntity.fromTrack(track))
        if (track.artist.isNotBlank()) {
            val prev = dao.getArtistPlayCount(track.artist) ?: 0
            dao.upsertArtistPlayCount(ArtistPlayCountEntity(track.artist, prev + 1))
        }
        api.clearForYouCache()
    }

    suspend fun removeHistoryTrack(trackId: String) {
        dao.deleteHistoryTrack(trackId)
        api.clearForYouCache()
    }

    suspend fun clearHistory() {
        dao.clearHistory()
        api.clearForYouCache()
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isNotEmpty()) dao.upsertRecentSearch(RecentSearchEntity(clean))
    }

    suspend fun removeRecentSearch(query: String) {
        dao.deleteRecentSearch(query)
    }

    suspend fun clearRecentSearches() {
        dao.clearRecentSearches()
    }

    suspend fun topArtists(limit: Int): List<String> {
        val scores = HashMap<String, Int>()
        dao.getArtistPlayCountsOnce().forEach { scores[it.artistName] = it.playCount }
        dao.getLikedTracksOnce().forEach {
            if (it.artist.isNotBlank()) {
                scores[it.artist] = (scores[it.artist] ?: 0) + 2
            }
        }
        return scores.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    suspend fun songsByArtist(artist: String): List<Track> {
        val r = api.searchSongs(artist)
        val k = artist.lowercase()
        val matched = r.filter {
            val a = it.artist.lowercase()
            a.isNotEmpty() && (a.contains(k) || k.contains(a))
        }
        return if (matched.size >= 4) matched else r
    }

    suspend fun getSimilarTracks(seed: Track): List<Track> {
        val related = api.getRelated(seed).filter { MusicFilter.isPure(it) }
        val relArtists = related.map { it.artist }.filter { it.isNotBlank() && it != seed.artist }.distinct().take(4)
        val searchArtists = (listOfNotNull(seed.artist.takeIf { it.isNotBlank() }) + relArtists)
        val sets = coroutineScope {
            searchArtists.map { a ->
                async {
                    try { songsByArtist(a) } catch (_: Exception) { emptyList() }
                }
            }.awaitAll()
        }
        val pool = ArrayList<WeightedTrack>()
        related.forEachIndexed { i, t -> pool.add(WeightedTrack(t, 3.5 - i * 0.08)) }
        sets.forEachIndexed { j, tracks ->
            tracks.forEachIndexed { i, t ->
                pool.add(WeightedTrack(t, (if (j == 0) 2.2 else 1.6) - i * 0.05))
            }
        }

        val recentIds = dao.getHistoryOnce().take(25).map { it.id }.toSet()
        val fav = topArtists(6).toSet()
        var out = MusicFilter.rank(pool, seed = seed, recentHistoryIds = recentIds, favArtists = fav)
        if (out.size < 8) {
            try {
                val extra = api.searchSongs("${seed.artist} ${MusicFilter.baseTitle(seed.title)}".trim())
                val skipIds = out.map { it.id }.toSet()
                out = out + MusicFilter.rank(
                    extra.map { WeightedTrack(it) },
                    seed = seed,
                    recentHistoryIds = recentIds,
                    skipIds = skipIds,
                    favArtists = fav
                )
            } catch (_: Exception) {}
        }
        return out
    }

    suspend fun getChart(regionCode: String): List<Track> {
        val cn = MusicConstants.countryName(regionCode)
        var results = emptyList<Track>()
        try {
            val query = if (regionCode == "WW") "top 100 songs global" else "top 100 songs $cn"
            for (pl in api.searchPlaylists(query).take(3)) {
                val tracks = MusicFilter.clean(api.getPlaylist(pl.id).tracks, 25)
                if (tracks.size >= 8) {
                    results = tracks
                    break
                }
            }
        } catch (_: Exception) {}

        if (results.size < 8) {
            try {
                val q = if (regionCode == "WW") "top hits" else "top songs $cn"
                results = MusicFilter.clean(api.searchSongs(q), 20)
            } catch (_: Exception) {}
        }
        if (results.isEmpty()) error("Chart unavailable")
        return results
    }

    suspend fun getForYou(): List<Track> {
        val arts = topArtists(3)
        val seeds = dao.getHistoryOnce().take(2).map { it.toTrack() }
        if (arts.isEmpty()) {
            return MusicFilter.clean(api.searchSongs("top hits"), 15)
        }
        val sets = coroutineScope {
            val aJobs = arts.map { a -> async { try { songsByArtist(a) } catch (_: Exception) { emptyList() } } }
            val sJobs = seeds.map { s -> async { try { getSimilarTracks(s) } catch (_: Exception) { emptyList() } } }
            (aJobs + sJobs).awaitAll()
        }
        val pool = ArrayList<WeightedTrack>()
        for (i in 0 until 12) {
            sets.forEach { list ->
                if (i < list.size) pool.add(WeightedTrack(list[i]))
            }
        }
        val recentIds = dao.getHistoryOnce().take(25).map { it.id }.toSet()
        val fav = topArtists(6).toSet()
        val ranked = MusicFilter.rank(pool, recentHistoryIds = recentIds, favArtists = fav, boostArtists = arts.toSet()).take(20)
        return ranked.ifEmpty { MusicFilter.clean(api.searchSongs("top hits"), 15) }
    }

    suspend fun pickPlaylists(queries: List<String>, perQuery: Int = 2, maxTotal: Int = 14): List<PlaylistSummary> {
        val results = coroutineScope {
            queries.map { q -> async { try { api.searchPlaylists(q) } catch (_: Exception) { emptyList() } } }.awaitAll()
        }
        val out = ArrayList<PlaylistSummary>()
        val seen = HashSet<String>()
        results.forEach { list ->
            val sorted = list.filter { it.artist.contains("youtube", ignoreCase = true) } +
                list.filterNot { it.artist.contains("youtube", ignoreCase = true) }
            sorted.take(perQuery).forEach { pl ->
                if (seen.add(pl.id)) out.add(pl)
            }
        }
        return out.take(maxTotal)
    }

    suspend fun getMoodData(mood: MoodGenre): Pair<List<PlaylistSummary>, List<Track>> {
        val pls = try { api.searchPlaylists(mood.query) } catch (_: Exception) { emptyList() }
        val tr = ArrayList<Track>()
        for (pl in pls.take(3)) {
            try {
                tr.addAll(api.getPlaylist(pl.id, pl.name, pl.artist).tracks)
            } catch (_: Exception) {}
            if (tr.size >= 30) break
        }
        var cleaned = MusicFilter.dedupe(MusicFilter.clean(tr, 60))
        if (cleaned.isEmpty()) {
            cleaned = try { MusicFilter.clean(api.searchSongs(mood.query), 40) } catch (_: Exception) { emptyList() }
        }
        return pls to cleaned
    }

    suspend fun getLanguageData(language: String): Pair<List<PlaylistSummary>, List<Track>> {
        val pls = pickPlaylists(
            listOf("$language top songs", "$language hits", "best of $language", "new $language songs", "$language romantic songs"),
            perQuery = 2,
            maxTotal = 16
        )
        val tr = ArrayList<Track>()
        for (pl in pls.take(4)) {
            try {
                tr.addAll(api.getPlaylist(pl.id, pl.name, pl.artist).tracks)
            } catch (_: Exception) {}
            if (tr.size >= 40) break
        }
        var cleaned = MusicFilter.dedupe(MusicFilter.clean(tr, 80))
        if (cleaned.isEmpty()) {
            cleaned = try { MusicFilter.clean(api.searchSongs("$language songs"), 40) } catch (_: Exception) { emptyList() }
        }
        return pls to cleaned
    }

    suspend fun getArtistFullCatalog(artist: String, seed: List<Track>): List<Track> {
        val k = artist.lowercase()
        val matchesArtist: (Track) -> Boolean = { t ->
            t.artist.isEmpty() || t.artist.lowercase().contains(k) || k.contains(t.artist.lowercase())
        }
        val albums = try { api.searchAlbums(artist) } catch (_: Exception) { emptyList() }
        val ownAlbums = albums.filter {
            val a = it.artist.lowercase()
            a.isNotEmpty() && (a.contains(k) || k.contains(a))
        }
        val useAlbums = (if (ownAlbums.isNotEmpty()) ownAlbums else albums).take(8)
        val lists = coroutineScope {
            useAlbums.map { alb ->
                async {
                    try {
                        val pl = api.getPlaylist(alb.id)
                        if (ownAlbums.isNotEmpty()) pl.tracks else pl.tracks.filter(matchesArtist)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll()
        }
        val extra = coroutineScope {
            listOf("$artist songs", "$artist hits", "$artist best songs").map { q ->
                async { try { api.searchSongs(q) } catch (_: Exception) { emptyList() } }
            }.awaitAll()
        }
        val out = ArrayList<Track>()
        val keys = HashSet<String>()
        (seed + extra.flatten().filter(matchesArtist) + lists.flatten()).forEach { t ->
            val d = t.durationSeconds
            if ((d != null && (d < 60 || d > 600)) || MusicFilter.isFiller(t)) return@forEach
            val tk = MusicFilter.trackKey(t)
            if (tk.isNotEmpty() && keys.add(tk)) out.add(t)
        }
        return out
    }

    suspend fun getFamousArtists(): List<String> {
        val counts = HashMap<String, Double>()
        fun addTracks(tracks: List<Track>, weight: Double) {
            tracks.forEachIndexed { i, t ->
                val a = t.artist.split(Regex(",|&| feat\\.? | x ", RegexOption.IGNORE_CASE)).firstOrNull()?.trim() ?: ""
                if (a.isNotEmpty() && a.length < 40) {
                    counts[a] = (counts[a] ?: 0.0) + weight * (1.0 + (30 - minOf(i, 30)) / 60.0)
                }
            }
        }
        try { addTracks(getChart("WW"), 1.0) } catch (_: Exception) {}
        try {
            val pls = pickPlaylists(listOf("top 100 songs global", "global top 50", "biggest hits right now"), 2, 5)
            pls.take(4).forEach { pl ->
                try { addTracks(api.getPlaylist(pl.id).tracks, 0.8) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        val names = counts.entries.sortedByDescending { it.value }.map { it.key }
        return if (names.size >= 8) names.take(24) else MusicConstants.FAMOUS_ARTISTS
    }

    suspend fun ensureArtistImages(names: List<String>): Map<String, String> {
        val distinct = names.filter { it.isNotBlank() }.distinct()
        val famous = distinct.filter { it in MusicConstants.FAMOUS_ARTISTS }
        if (famous.isNotEmpty()) api.fetchWikipediaArtistImages(famous)
        val need = distinct.filter { !api.artistAvatarCache.containsKey(it) }.take(12)
        if (need.isNotEmpty()) {
            coroutineScope {
                need.map { n ->
                    async {
                        try {
                            val r = api.searchArtists(n)
                            val k = n.lowercase()
                            val best = r.find { it.name.lowercase() == k }
                                ?: r.find { it.avatarUrl.isNotEmpty() && it.name.lowercase().contains(k) }
                                ?: r.firstOrNull()
                            if (best != null && best.avatarUrl.isNotEmpty()) {
                                api.artistAvatarCache[n] = best.avatarUrl
                            }
                        } catch (_: Exception) {}
                    }
                }.awaitAll()
            }
        }
        return api.artistAvatarCache.toMap()
    }

    suspend fun createBackupJson(): String {
        val liked = dao.getLikedTracksOnce().map { it.toTrack() }
        val pls = dao.getPlaylistsOnce()
        val allTracks = dao.getAllPlaylistTracksOnce().groupBy { it.playlistName }

        fun trackJson(t: Track) = JSONObject().apply {
            put("id", t.id)
            put("title", t.title)
            put("artist", t.artist)
            put("duration", t.durationSeconds ?: JSONObject.NULL)
            put("thumb", t.thumbUrl)
            put("album", t.album ?: JSONObject.NULL)
            put("ms", t.isMusicCatalog)
        }

        val root = JSONObject().apply {
            put("app", "Pulse")
            put("version", 2)
            put("liked", JSONArray().apply { liked.forEach { put(trackJson(it)) } })
            put("playlists", JSONObject().apply {
                pls.forEach { pl ->
                    val arr = JSONArray()
                    (allTracks[pl.name] ?: emptyList()).forEach { arr.put(trackJson(it.toTrack())) }
                    put(pl.name, arr)
                }
            })
            put("covers", JSONObject().apply {
                pls.forEach { pl ->
                    if (!pl.customCoverUri.isNullOrBlank()) {
                        put(pl.name, JSONObject().put("cover", pl.customCoverUri))
                    }
                }
            })
        }
        return root.toString(2)
    }

    suspend fun restoreBackupJson(jsonText: String): Int {
        val root = JSONObject(jsonText)
        val plsObj = root.optJSONObject("playlists") ?: JSONObject()
        val coversObj = root.optJSONObject("covers") ?: JSONObject()
        val likedArr = root.optJSONArray("liked") ?: JSONArray()

        fun parseTrackObj(o: JSONObject): Track? {
            val id = o.optString("id")
            if (id.isBlank()) return null
            return Track(
                id = id,
                title = o.optString("title", "Unknown"),
                artist = o.optString("artist", ""),
                durationSeconds = o.optInt("duration", -1).takeIf { it > 0 },
                thumbUrl = o.optString("thumb"),
                album = o.optString("album").takeIf { it.isNotBlank() },
                isMusicCatalog = o.optBoolean("ms", false)
            )
        }

        var count = 0
        val existingLiked = dao.getLikedTracksOnce().map { it.id }.toSet()
        for (i in 0 until likedArr.length()) {
            val t = likedArr.optJSONObject(i)?.let(::parseTrackObj) ?: continue
            if (t.id !in existingLiked) {
                dao.insertLikedTrack(LikedTrackEntity.fromTrack(t))
                count++
            }
        }

        plsObj.keys().forEach { name ->
            if (name == "Liked Songs") return@forEach
            val cover = coversObj.optJSONObject(name)?.optString("cover")?.takeIf { it.isNotBlank() }
            dao.insertPlaylist(PlaylistEntity(name = name, customCoverUri = cover))
            val arr = plsObj.optJSONArray(name) ?: return@forEach
            val tracks = ArrayList<Track>()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let(::parseTrackObj)?.let { tracks.add(it) }
            }
            addTracksToPlaylist(name, tracks)
            count += tracks.size
        }
        return count
    }

    suspend fun resetAllUserData() {
        dao.clearLikedTracks()
        dao.clearPlaylists()
        dao.clearPlaylistTracks()
        dao.clearHistory()
        dao.clearRecentSearches()
        dao.clearArtistPlayCounts()
        prefs.clearAll()
        api.clearCache()
    }
}
