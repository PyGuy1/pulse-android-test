package com.example.data.remote

import com.example.data.model.ArtistSummary
import com.example.data.model.AudioQuality
import com.example.data.model.ImportCandidate
import com.example.data.model.ImportMatchStatus
import com.example.data.model.PlaylistSummary
import com.example.data.model.RemotePlaylistResult
import com.example.data.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MusicApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(9, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    private val defaultPiped = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.private.coffee"
    )
    private val defaultInvidious = listOf(
        "https://yewtu.be",
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de"
    )

    @Volatile
    private var pipedInstances: List<String> = defaultPiped

    @Volatile
    private var invidiousInstances: List<String> = defaultInvidious

    @Volatile
    private var lastDiscoverMs: Long = 0L

    @Volatile
    var customPipedUrl: String = ""

    private data class CacheEntry(val timestampMs: Long, val value: Any)
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    val artistThumbCache = ConcurrentHashMap<String, String>()
    val artistAvatarCache = ConcurrentHashMap<String, String>()

    fun clearCache() {
        memoryCache.clear()
    }

    fun clearStreamCache() {
        memoryCache.keys.filter { it.startsWith("st:") }.forEach { memoryCache.remove(it) }
    }

    fun clearForYouCache() {
        memoryCache.remove("fy")
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(key: String, ttlMs: Long = 900_000L, loader: suspend () -> T): T {
        val existing = memoryCache[key]
        if (existing != null && System.currentTimeMillis() - existing.timestampMs < ttlMs) {
            return existing.value as T
        }
        val result = loader()
        memoryCache[key] = CacheEntry(System.currentTimeMillis(), result)
        return result
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private suspend fun fetchString(url: String, userAgent: String = "PulseAndroid/1.0"): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
                resp.body?.string() ?: error("Empty body")
            }
        }

    private suspend fun getJsonAny(url: String): Any {
        val text = fetchString(url).trim()
        return if (text.startsWith("[")) JSONArray(text) else JSONObject(text)
    }

    /**
     * Races a list of URLs concurrently and returns the first non-empty parsed result.
     */
    private suspend fun <T> raceAny(urls: List<String>, parse: (Any) -> T?): T = coroutineScope {
        if (urls.isEmpty()) error("No URLs provided")
        val jobs = urls.map { url ->
            async(Dispatchers.IO) {
                val parsed = parse(getJsonAny(url)) ?: error("Empty result")
                parsed
            }
        }
        val remaining = jobs.toMutableList()
        var lastError: Throwable? = null
        while (remaining.isNotEmpty()) {
            try {
                val result = select<T> {
                    remaining.forEach { deferred ->
                        deferred.onAwait { value -> value }
                    }
                }
                remaining.forEach { it.cancel() }
                return@coroutineScope result
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                lastError = t
                remaining.removeAll { it.isCompleted }
            }
        }
        throw lastError ?: IllegalStateException("All instances failed")
    }

    suspend fun discoverInstances() {
        val now = System.currentTimeMillis()
        if (now - lastDiscoverMs < 6 * 3_600_000L) return
        lastDiscoverMs = now
        withContext(Dispatchers.IO) {
            try {
                val arr = JSONArray(fetchString("https://piped-instances.kavin.rocks/"))
                val list = ArrayList<Pair<Double, String>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val apiUrl = obj.optString("api_url").trimEnd('/')
                    val uptime = obj.optDouble("uptime_30d", 100.0)
                    if (apiUrl.startsWith("https://") && uptime > 50.0) {
                        list.add(uptime to apiUrl)
                    }
                }
                val sorted = list.sortedByDescending { it.first }.map { it.second }
                if (sorted.isNotEmpty()) {
                    pipedInstances = (sorted.take(3) + defaultPiped + sorted.drop(3).take(6)).distinct()
                }
            } catch (_: Exception) {}

            try {
                val arr = JSONArray(fetchString("https://api.invidious.io/instances.json?sort_by=type,health"))
                val list = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val entry = arr.optJSONArray(i) ?: continue
                    val meta = entry.optJSONObject(1) ?: continue
                    if (meta.optString("type") == "https" && meta.optBoolean("api") && meta.optBoolean("cors")) {
                        val uri = meta.optString("uri").trimEnd('/')
                        if (uri.startsWith("https://")) list.add(uri)
                    }
                }
                if (list.isNotEmpty()) {
                    invidiousInstances = (list.take(3) + defaultInvidious + list.drop(3).take(6)).distinct()
                }
            } catch (_: Exception) {}
        }
    }

    private fun pipedUrls(path: String): List<String> {
        val custom = customPipedUrl.trim().trimEnd('/')
        val base = if (custom.isNotEmpty()) listOf(custom + path) else emptyList()
        return base + pipedInstances.take(7).map { it + path }
    }

    private fun invidiousUrls(path: String): List<String> {
        val custom = customPipedUrl.trim().trimEnd('/')
        val base = if (custom.isNotEmpty()) listOf(custom + path) else emptyList()
        return base + invidiousInstances.take(5).map { it + path }
    }

    private fun parseTracks(raw: Any, isMusicCatalog: Boolean): List<Track> {
        val arr: JSONArray = when (raw) {
            is JSONArray -> raw
            is JSONObject -> raw.optJSONArray("items") ?: return emptyList()
            else -> return emptyList()
        }
        val noiseRegex = Regex(" - Topic$")
        val list = ArrayList<Track>(arr.length())
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val type = e.optString("type")
            if (type.isNotEmpty() && type != "stream") continue

            val rawUrl = e.optString("url")
            val id = e.optString("videoId").ifBlank {
                Regex("[?&]v=([\\w-]{11})").find(rawUrl)?.groupValues?.getOrNull(1) ?: ""
            }
            if (id.length != 11) continue

            val uploader = e.optString("author").ifBlank { e.optString("uploaderName") }
            val isTopic = noiseRegex.containsMatchIn(uploader)
            val artist = uploader.replace(noiseRegex, "").trim()
            val rawThumb = e.optString("thumbnail")
            val thumb = if (isMusicCatalog && (rawThumb.contains("googleusercontent") || rawThumb.contains("ggpht"))) {
                rawThumb
            } else {
                "https://i.ytimg.com/vi/$id/mqdefault.jpg"
            }

            val durRaw = e.optInt("lengthSeconds", e.optInt("duration", -1))
            val dur = if (durRaw > 0) durRaw else null
            val verified = e.optBoolean("uploaderVerified") || e.optBoolean("authorVerified")
            val title = (e.optString("title").ifBlank { e.optString("name") }).trim()
            if (title.isEmpty()) continue

            if (artist.isNotEmpty() && !artistThumbCache.containsKey(artist)) {
                artistThumbCache[artist] = thumb
            }

            list.add(
                Track(
                    id = id,
                    title = title,
                    artist = artist,
                    durationSeconds = dur,
                    thumbUrl = thumb,
                    isTopic = isTopic,
                    isVerified = verified,
                    isMusicCatalog = isMusicCatalog
                )
            )
        }
        return list
    }

    private fun parseArtists(raw: Any): List<ArtistSummary> {
        val obj = raw as? JSONObject ?: return emptyList()
        val items = obj.optJSONArray("items") ?: return emptyList()
        val noiseRegex = Regex(" - Topic$")
        val out = ArrayList<ArtistSummary>()
        for (i in 0 until items.length()) {
            val e = items.optJSONObject(i) ?: continue
            val type = e.optString("type")
            val url = e.optString("url")
            if (type == "channel" || url.contains("/channel/")) {
                val name = e.optString("name").ifBlank { e.optString("title") }.replace(noiseRegex, "").trim()
                if (name.isNotEmpty()) {
                    val avatar = e.optString("thumbnail")
                    if (avatar.isNotEmpty()) artistAvatarCache.putIfAbsent(name, avatar)
                    out.add(ArtistSummary(name = name, avatarUrl = avatar, channelId = url.substringAfterLast('/')))
                }
            }
        }
        return out
    }

    private fun parsePlaylists(raw: Any, isAlbum: Boolean): List<PlaylistSummary> {
        val obj = raw as? JSONObject ?: return emptyList()
        val items = obj.optJSONArray("items") ?: return emptyList()
        val noiseRegex = Regex(" - Topic$")
        val out = ArrayList<PlaylistSummary>()
        for (i in 0 until items.length()) {
            val e = items.optJSONObject(i) ?: continue
            val type = e.optString("type")
            val url = e.optString("url")
            if (type == "playlist" || url.contains("list=") || url.contains("/playlist")) {
                val id = Regex("list=([\\w-]+)").find(url)?.groupValues?.getOrNull(1)
                    ?: Regex("playlists?/([\\w-]+)").find(url)?.groupValues?.getOrNull(1)
                    ?: continue
                val name = e.optString("name").ifBlank { e.optString("title") }.trim()
                if (name.isEmpty()) continue
                out.add(
                    PlaylistSummary(
                        id = id,
                        name = name,
                        thumbUrl = e.optString("thumbnail"),
                        artist = e.optString("uploaderName").replace(noiseRegex, "").trim(),
                        trackCount = e.optInt("videos", 0),
                        isAlbum = isAlbum
                    )
                )
            }
        }
        return out
    }

    suspend fun searchSongs(query: String): List<Track> = cached("ss:$query", 600_000L) {
        try {
            return@cached raceAny(pipedUrls("/search?q=${enc(query)}&filter=music_songs")) {
                parseTracks(it, true).ifEmpty { null }
            }
        } catch (_: Exception) {}

        val fallbackUrls = pipedInstances.take(4).map { "$it/search?q=${enc(query)}&filter=videos" } +
            invidiousInstances.take(4).map { "$it/api/v1/search?q=${enc(query)}&type=video" }
        val r = raceAny(fallbackUrls) { parseTracks(it, false).ifEmpty { null } }
        val pure = r.filter { MusicFilter.isPure(it) && !MusicFilter.isFiller(it) }
        pure.ifEmpty { r }
    }

    suspend fun searchSongsLite(query: String): List<Track> = cached("ssl:$query", 600_000L) {
        val custom = customPipedUrl.trim().trimEnd('/')
        val urls = (if (custom.isNotEmpty()) listOf(custom) else emptyList()) + pipedInstances.take(3)
        try {
            raceAny(urls.map { "$it/search?q=${enc(query)}&filter=music_songs" }) {
                parseTracks(it, true).ifEmpty { null }
            }
        } catch (_: Exception) {
            searchSongs(query)
        }
    }

    suspend fun searchArtists(query: String): List<ArtistSummary> = cached("as:$query", 600_000L) {
        try {
            return@cached raceAny(pipedUrls("/search?q=${enc(query)}&filter=music_artists")) {
                parseArtists(it).ifEmpty { null }
            }
        } catch (_: Exception) {
            val songs = searchSongs(query)
            songs.map { it.artist }.filter { it.isNotBlank() }.distinct().take(8)
                .map { ArtistSummary(name = it, avatarUrl = artistAvatarCache[it] ?: artistThumbCache[it] ?: "") }
        }
    }

    suspend fun searchAlbums(query: String): List<PlaylistSummary> = cached("al:$query", 600_000L) {
        raceAny(pipedUrls("/search?q=${enc(query)}&filter=music_albums")) {
            parsePlaylists(it, isAlbum = true).ifEmpty { null }
        }
    }

    suspend fun searchPlaylists(query: String): List<PlaylistSummary> = cached("pls:$query", 600_000L) {
        raceAny(pipedUrls("/search?q=${enc(query)}&filter=music_playlists")) {
            parsePlaylists(it, isAlbum = false).ifEmpty { null }
        }
    }

    suspend fun getSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return raceAny(pipedUrls("/suggestions?query=${enc(query)}")) { raw ->
            val arr = raw as? JSONArray ?: return@raceAny null
            val out = (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
            out.ifEmpty { null }
        }
    }

    suspend fun getPlaylist(id: String, fallbackName: String = "", fallbackArtist: String = ""): RemotePlaylistResult =
        cached("pl:$id", 30 * 60_000L) {
            val urls = pipedUrls("/playlists/$id") + invidiousUrls("/api/v1/playlists/$id")
            try {
                return@cached raceAny(urls) { raw ->
                    val obj = raw as? JSONObject ?: return@raceAny null
                    val arr = obj.optJSONArray("relatedStreams") ?: obj.optJSONArray("videos") ?: return@raceAny null
                    val tracks = parseTracks(arr, true)
                    if (tracks.isEmpty()) return@raceAny null
                    RemotePlaylistResult(
                        name = obj.optString("name").ifBlank { obj.optString("title").ifBlank { fallbackName } },
                        thumbUrl = obj.optString("thumbnailUrl"),
                        artist = obj.optString("uploader").ifBlank { obj.optString("author") }
                            .replace(Regex(" - Topic$"), "").trim(),
                        tracks = tracks
                    )
                }
            } catch (e: Exception) {
                if (fallbackName.isNotBlank()) {
                    val q = "$fallbackName $fallbackArtist".trim()
                    val approx = MusicFilter.clean(searchSongs(q), 40)
                    if (approx.isNotEmpty()) {
                        return@cached RemotePlaylistResult(
                            name = fallbackName,
                            thumbUrl = "",
                            artist = fallbackArtist,
                            tracks = approx,
                            isApproximate = true
                        )
                    }
                }
                throw e
            }
        }

    data class StreamBundle(val audioUrls: List<String>, val related: List<Track>)

    suspend fun getStreamAndRelated(videoId: String, quality: AudioQuality): StreamBundle {
        val cacheKey = "st:$videoId:${quality.key}"
        return cached(cacheKey, 25 * 60_000L) {
            val urls = pipedUrls("/streams/$videoId") + invidiousUrls("/api/v1/videos/$videoId?local=true")
            try {
                val bundle = raceAny(urls) { raw ->
                    val obj = raw as? JSONObject ?: return@raceAny null
                    val candidates = ArrayList<Pair<Int, String>>()
                    var related: List<Track> = emptyList()

                    val pipedStreams = obj.optJSONArray("audioStreams")
                    if (pipedStreams != null) {
                        for (i in 0 until pipedStreams.length()) {
                            val s = pipedStreams.optJSONObject(i) ?: continue
                            val u = s.optString("url")
                            val mime = s.optString("mimeType")
                            val br = s.optInt("bitrate", 0)
                            if (u.isNotBlank() && Regex("audio/mp4|m4a|audio/webm", RegexOption.IGNORE_CASE).containsMatchIn(mime)) {
                                val bonus = if (Regex("mp4|m4a", RegexOption.IGNORE_CASE).containsMatchIn(mime)) 0 else 500_000
                                candidates.add((kotlin.math.abs(br - quality.targetBitrate) + bonus) to u)
                            }
                        }
                        obj.optJSONArray("relatedStreams")?.let { related = parseTracks(it, false) }
                    } else {
                        val adaptive = obj.optJSONArray("adaptiveFormats")
                        if (adaptive != null) {
                            for (i in 0 until adaptive.length()) {
                                val s = adaptive.optJSONObject(i) ?: continue
                                val type = s.optString("type")
                                val u = s.optString("url")
                                val br = s.optString("bitrate").toIntOrNull() ?: 0
                                if (u.isNotBlank() && type.startsWith("audio")) {
                                    val bonus = if (Regex("mp4|m4a", RegexOption.IGNORE_CASE).containsMatchIn(type)) 0 else 500_000
                                    candidates.add((kotlin.math.abs(br - quality.targetBitrate) + bonus) to u)
                                }
                            }
                        }
                        obj.optJSONArray("recommendedVideos")?.let { related = parseTracks(it, false) }
                    }

                    val sortedUrls = candidates.sortedBy { it.first }.map { it.second }.distinct()
                    if (sortedUrls.isEmpty()) return@raceAny null
                    StreamBundle(sortedUrls, related)
                }
                val invDirect = invidiousInstances.take(4).map { "$it/latest_version?id=$videoId&itag=${quality.itag}&local=true" }
                bundle.copy(audioUrls = (bundle.audioUrls + invDirect).distinct())
            } catch (e: Exception) {
                val invDirect = invidiousInstances.take(4).map { "$it/latest_version?id=$videoId&itag=${quality.itag}&local=true" }
                if (invDirect.isNotEmpty()) {
                    StreamBundle(invDirect, emptyList())
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun getRelated(track: Track): List<Track> {
        val existing = memoryCache.entries.firstOrNull { it.key.startsWith("st:${track.id}:") }
        if (existing != null) {
            (existing.value.value as? StreamBundle)?.related?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return try {
            cached("rel:${track.id}", 3_600_000L) {
                val urls = pipedUrls("/streams/${track.id}") + invidiousUrls("/api/v1/videos/${track.id}?local=true")
                raceAny(urls) { raw ->
                    val obj = raw as? JSONObject ?: return@raceAny null
                    val arr = obj.optJSONArray("relatedStreams") ?: obj.optJSONArray("recommendedVideos") ?: return@raceAny null
                    parseTracks(arr, false).ifEmpty { null }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getVideoEmbedMetadata(videoId: String): Pair<String, String>? {
        return try {
            val json = JSONObject(fetchString("https://noembed.com/embed?url=${enc("https://www.youtube.com/watch?v=$videoId")}"))
            val title = json.optString("title").ifBlank { return null }
            val author = json.optString("author_name").replace(Regex(" - Topic$"), "").trim()
            title to author
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchWikipediaArtistImages(names: List<String>) = withContext(Dispatchers.IO) {
        val need = names.filter { !artistAvatarCache.containsKey(it) }.take(24)
        if (need.isEmpty()) return@withContext
        try {
            val titles = need.map { if (it == "Drake") "Drake (musician)" else it }
            val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&origin=*&redirects=1&prop=pageimages&pithumbsize=240&titles=${enc(titles.joinToString("|"))}"
            val obj = JSONObject(fetchString(url))
            val query = obj.optJSONObject("query") ?: return@withContext
            val normMap = HashMap<String, String>()
            query.optJSONArray("normalized")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    normMap[o.getString("from")] = o.getString("to")
                }
            }
            val redirMap = HashMap<String, String>()
            query.optJSONArray("redirects")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    redirMap[o.getString("from")] = o.getString("to")
                }
            }
            val pageImages = HashMap<String, String>()
            val pages = query.optJSONObject("pages") ?: return@withContext
            pages.keys().forEach { key ->
                val page = pages.optJSONObject(key) ?: return@forEach
                val title = page.optString("title")
                val src = page.optJSONObject("thumbnail")?.optString("source") ?: ""
                if (title.isNotEmpty() && src.isNotEmpty()) pageImages[title] = src
            }
            need.forEachIndexed { i, originalName ->
                var t = titles[i]
                t = normMap[t] ?: t
                t = redirMap[t] ?: t
                pageImages[t]?.let { artistAvatarCache[originalName] = it }
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchSpotifyPlaylist(type: String, id: String): Triple<String, String, List<ImportCandidate>> {
        val html = fetchString(
            "https://open.spotify.com/embed/$type/$id",
            userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36"
        )
        val match = Regex("<script id=\"__NEXT_DATA__\"[^>]*>([\\s\\S]*?)</script>").find(html)
            ?: error("Spotify embed metadata unavailable")
        val json = JSONObject(match.groupValues[1])
        val entity = json.optJSONObject("props")
            ?.optJSONObject("pageProps")
            ?.optJSONObject("state")
            ?.optJSONObject("data")
            ?.optJSONObject("entity") ?: error("Empty Spotify entity")
        val trackList = entity.optJSONArray("trackList") ?: error("No trackList")
        if (trackList.length() == 0) error("Empty Spotify playlist")

        val cleanStr: (String) -> String = { it.replace('\u00a0', ' ').trim() }
        val sources = entity.optJSONObject("coverArt")?.optJSONArray("sources")
        val coverUrl = if (sources != null && sources.length() > 0) {
            sources.getJSONObject(sources.length() - 1).optString("url")
        } else ""
        val name = cleanStr(entity.optString("name").ifBlank { entity.optString("title") }).ifBlank { "Spotify playlist" }
        val items = ArrayList<ImportCandidate>(trackList.length())
        for (i in 0 until trackList.length()) {
            val tr = trackList.getJSONObject(i)
            val title = cleanStr(tr.optString("title"))
            if (title.isEmpty()) continue
            val artist = cleanStr(tr.optString("subtitle"))
            val album = cleanStr(tr.optString("albumName").ifBlank { tr.optJSONObject("album")?.optString("name") ?: "" })
            val durMs = tr.optInt("duration", 0)
            val art = tr.optJSONObject("coverArt")?.optJSONArray("sources")?.optJSONObject(0)?.optString("url") ?: ""
            items.add(
                ImportCandidate(
                    sourceTitle = title,
                    sourceArtist = artist,
                    sourceAlbum = album,
                    sourceDurationSeconds = if (durMs > 0) durMs / 1000 else null,
                    sourceArtUrl = art,
                    matchedTrack = null,
                    status = ImportMatchStatus.PENDING,
                    isSelected = true
                )
            )
        }
        return Triple(name, coverUrl, items)
    }

    suspend fun fetchYtmPlaylistForImport(id: String): Triple<String, String, List<ImportCandidate>> {
        val pl = getPlaylist(id)
        val items = pl.tracks.map { t ->
            ImportCandidate(
                sourceTitle = t.title,
                sourceArtist = t.artist,
                sourceAlbum = t.album ?: "",
                sourceDurationSeconds = t.durationSeconds,
                sourceArtUrl = t.artworkUrl(160),
                matchedTrack = t,
                status = ImportMatchStatus.MATCHED,
                isSelected = true
            )
        }
        return Triple(pl.name, pl.thumbUrl, items)
    }

    suspend fun matchImportTrack(candidate: ImportCandidate): Pair<Track?, ImportMatchStatus> {
        var bestTrack: Track? = null
        var bestScore = -1.0
        val queries = listOf("${candidate.sourceArtist} ${candidate.sourceTitle}".trim(), candidate.sourceTitle).distinct()
        for (q in queries) {
            if (q.isBlank()) continue
            val results = try { searchSongsLite(q) } catch (_: Exception) { emptyList() }
            for (c in results.take(10)) {
                val sc = MusicFilter.scoreCandidate(
                    candidate.sourceTitle,
                    candidate.sourceArtist,
                    candidate.sourceDurationSeconds,
                    c
                )
                if (sc > bestScore) {
                    bestScore = sc
                    bestTrack = c
                }
            }
            if (bestScore >= 0.78) break
        }
        if (bestTrack == null || bestScore < 0.45) {
            return null to ImportMatchStatus.NOT_FOUND
        }
        val status = if (bestScore >= 0.7) ImportMatchStatus.MATCHED else ImportMatchStatus.CHECK
        return bestTrack to status
    }

    suspend fun detectCountryViaIp(): String? = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://api.country.is/" to "country",
            "https://ipwho.is/" to "country_code",
            "https://get.geojs.io/v1/ip/country.json" to "country"
        )
        val counts = HashMap<String, Int>()
        coroutineScope {
            endpoints.map { (url, field) ->
                async {
                    try {
                        val obj = JSONObject(fetchString(url))
                        val cc = obj.optString(field).uppercase()
                        if (Regex("^[A-Z]{2}$").matches(cc)) cc else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().forEach { cc ->
                counts[cc] = (counts[cc] ?: 0) + 1
            }
        }
        counts.maxByOrNull { it.value }?.key
    }
}
