package com.example.data.remote

import com.example.data.model.ImportCandidate
import com.example.data.model.ImportMatchStatus
import com.example.data.model.Track
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import kotlin.math.abs

object MusicFilter {
    private val FILLER_WORDS = listOf(
        "remix", "remixes", "rmx", "mashup", "mashups", "mash-up", "bootleg", "slowed",
        "reverb", "sped up", "speed up", "nightcore", "8d", "bass boosted", "megamix",
        "dj mix", "nonstop", "non-stop", "jukebox", "playlist", "compilation", "reaction",
        "reacts", "tutorial", "karaoke", "instrumental", "cover", "covered by", "full album",
        "1 hour", "hours", "ringtone", "shorts", "asmr", "trailer", "teaser", "interview",
        "behind the scenes", "making of", "lesson", "review", "unboxing", "amv", "best of",
        "greatest hits", "status", "extended", "looped", "live at", "live in", "live from",
        "live on", "live session", "live performance", "live version", "(live)", "[live]",
        "tiktok", "mix", "edit", "edits", "edit audio", "fanmade", "fan made", "whatsapp",
        "reels", "viral", "trending sound", "movie scene", "sound effect"
    )

    private val FILLER_REGEX = Regex(
        "(?:^|[^a-z0-9])(?:" + FILLER_WORDS.joinToString("|") { Regex.escape(it) } + ")(?:[^a-z0-9]|$)",
        RegexOption.IGNORE_CASE
    )

    private val CHANNEL_FILLER_REGEX = Regex(
        "(edits?|remix(es)?|mashups?|nightcore|slowed|status|whatsapp|shorts|reacts?|karaoke|covers?|8d|clips?|reels|fan ?page|lyrics? ?(hub|station))\\s*$",
        RegexOption.IGNORE_CASE
    )

    private val BRACKET_REGEX = Regex("[\\(\\[].*?[\\)\\]]")
    private val NOISE_TOKENS_REGEX = Regex("official|video|audio|lyrics?|\\bhd\\b|4k|(ft|feat)\\.?.*", RegexOption.IGNORE_CASE)
    private val NON_ALNUM_REGEX = Regex("[^a-z0-9 ]")
    private val MULTI_SPACE_REGEX = Regex("\\s+")
    private val VIDEO_ID_EXTRACT = Regex("(?:v=|youtu\\.be/|shorts/|embed/)([\\w-]{11})")

    fun fillerWord(title: String?): String {
        val match = FILLER_REGEX.find((title ?: "").lowercase()) ?: return ""
        return match.value.replace(Regex("^[^a-z0-9(\\[]+|[^a-z0-9)\\]]+$"), "")
    }

    fun isFiller(track: Track, seedTitle: String? = null): Boolean {
        val w = fillerWord(track.title)
        if (w.isNotEmpty() && !(seedTitle != null && fillerWord(seedTitle) == w)) return true
        return CHANNEL_FILLER_REGEX.containsMatchIn(track.artist)
    }

    fun isPure(track: Track): Boolean {
        return track.isMusicCatalog ||
            track.isTopic ||
            track.artist.endsWith("vevo", ignoreCase = true) ||
            (track.isVerified && Regex("official|\\(audio\\)|lyric", RegexOption.IGNORE_CASE).containsMatchIn(track.title))
    }

    fun clean(tracks: List<Track>, limit: Int = 15, strict: Boolean = false): List<Track> {
        return tracks.filter { t ->
            val d = t.durationSeconds
            (d == null || d in 75..480) && !isFiller(t) && (!strict || isPure(t))
        }.take(limit)
    }

    fun dedupe(tracks: List<Track>): List<Track> {
        val seen = HashSet<String>()
        return tracks.filter { seen.add(it.id) }
    }

    fun baseTitle(text: String?): String {
        return (text ?: "")
            .lowercase()
            .replace(BRACKET_REGEX, "")
            .replace(NOISE_TOKENS_REGEX, "")
            .replace(NON_ALNUM_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    fun trackKey(track: Track): String {
        val titleWithoutArtist = if (track.artist.isNotBlank()) {
            track.title.lowercase().replace(track.artist.lowercase(), "")
        } else {
            track.title.lowercase()
        }
        return baseTitle(titleWithoutArtist)
    }

    fun okSongForSearch(track: Track, query: String): Boolean {
        val d = track.durationSeconds
        if (d != null && (d < 60 || d > 600)) return false
        val w = fillerWord(track.title)
        return !(w.isNotEmpty() && !query.lowercase().contains(w))
    }

    data class WeightedTrack(val track: Track, val priorWeight: Double = 0.0)

    fun rank(
        candidates: List<WeightedTrack>,
        seed: Track? = null,
        recentHistoryIds: Set<String> = emptySet(),
        skipIds: Set<String> = emptySet(),
        favArtists: Set<String> = emptySet(),
        boostArtists: Set<String> = emptySet()
    ): List<Track> {
        val seen = HashSet<String>(recentHistoryIds.size + skipIds.size).apply {
            addAll(recentHistoryIds)
            addAll(skipIds)
        }
        val keys = HashSet<String>()
        if (seed != null) {
            val k = trackKey(seed)
            if (k.isNotEmpty()) keys.add(k)
        }

        val scored = candidates.mapIndexedNotNull { i, item ->
            val t = item.track
            val d = t.durationSeconds
            if (d != null && (d < 90 || d > 480)) return@mapIndexedNotNull null
            if (!isPure(t) || isFiller(t, seed?.title) || seen.contains(t.id)) return@mapIndexedNotNull null
            val k = trackKey(t)
            if (k.isEmpty() || keys.contains(k)) return@mapIndexedNotNull null
            keys.add(k)

            var score = item.priorWeight
            if (seed != null && t.artist.isNotEmpty() && t.artist == seed.artist) score += 1.5
            if (favArtists.contains(t.artist)) score += 1.2
            if (boostArtists.contains(t.artist)) score += 1.8
            if (t.isMusicCatalog) score += 1.0
            if (t.isTopic) score += 1.0
            if (t.isVerified) score += 0.6
            score -= i * 0.004
            t to score
        }.sortedByDescending { it.second }

        val counts = HashMap<String, Int>()
        val first = ArrayList<Track>()
        val rest = ArrayList<Track>()
        for ((track, _) in scored) {
            val c = (counts[track.artist] ?: 0) + 1
            counts[track.artist] = c
            if (c <= 3) first.add(track) else rest.add(track)
        }
        return first + rest
    }

    fun words(s: String?): List<String> =
        baseTitle(s).split(' ').filter { it.length > 1 }

    fun jaccard(a: String?, b: String?): Double {
        val setA = words(a).toSet()
        val setB = words(b).toSet()
        if (setA.isEmpty() || setB.isEmpty()) return 0.0
        val intersection = setA.count { it in setB }
        return intersection.toDouble() / (setA.size + setB.size - intersection)
    }

    fun scoreCandidate(sourceTitle: String, sourceArtist: String, sourceDurationSec: Int?, candidate: Track): Double {
        val arts = sourceArtist.lowercase()
            .split(Regex(",|;|&| feat\\.?| x | and "))
            .map { it.trim() }
            .filter { it.length > 1 }
        val ts = maxOf(
            jaccard(sourceTitle, candidate.title),
            jaccard(sourceTitle, candidate.title.replace(candidate.artist, "", ignoreCase = true))
        )
        val ca = candidate.artist.lowercase()
        val artistScore = if (arts.isNotEmpty()) {
            if (arts.any { a -> ca.contains(a) || (ca.length > 2 && a.contains(ca)) }) 1.0 else 0.0
        } else 0.5
        val durScore = if (sourceDurationSec != null && candidate.durationSeconds != null) {
            val diff = abs(sourceDurationSec - candidate.durationSeconds)
            when {
                diff <= 3 -> 1.0
                diff <= 8 -> 0.7
                diff <= 15 -> 0.3
                else -> 0.0
            }
        } else 0.5
        val fillerPenalty = if (isFiller(candidate, sourceTitle)) 0.5 else 0.0
        return 0.5 * ts + 0.3 * artistScore + 0.2 * durScore - fillerPenalty
    }

    fun extractVideoId(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return VIDEO_ID_EXTRACT.find(input)?.groupValues?.getOrNull(1) ?: ""
    }

    fun parseImportFile(fileName: String, textInput: String): Triple<String, String, List<ImportCandidate>> {
        val text = textInput.removePrefix("\uFEFF")
        val ext = fileName.substringAfterLast('.', "").lowercase()
        var playlistName = fileName.substringBeforeLast('.')
        val rawItems = ArrayList<ImportCandidate>()
        val sniff = text.trimStart().firstOrNull()

        when {
            ext == "json" || sniff == '{' || sniff == '[' -> {
                val trimmed = text.trim()
                val arr: JSONArray = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val obj = JSONObject(trimmed)
                    if (obj.has("name") && obj.optString("name").isNotBlank()) {
                        playlistName = obj.optString("name")
                    }
                    when {
                        obj.has("tracks") -> obj.getJSONArray("tracks")
                        obj.has("items") -> obj.getJSONArray("items")
                        obj.has("playlists") -> {
                            val pls = obj.getJSONArray("playlists")
                            if (pls.length() > 0) {
                                val firstPl = pls.getJSONObject(0)
                                if (firstPl.optString("name").isNotBlank()) playlistName = firstPl.optString("name")
                                firstPl.optJSONArray("items") ?: JSONArray()
                            } else JSONArray()
                        }
                        else -> JSONArray()
                    }
                }

                for (i in 0 until arr.length()) {
                    val item = arr.get(i)
                    if (item is String) {
                        val parts = item.split(" - ", limit = 2)
                        val artist = if (parts.size > 1) parts[0].trim() else ""
                        val title = if (parts.size > 1) parts[1].trim() else item.trim()
                        rawItems.add(makeCandidate(title, artist, "", null, ""))
                    } else if (item is JSONObject) {
                        val tr = item.optJSONObject("track") ?: item
                        val title = tr.optString("title").ifBlank {
                            tr.optString("name").ifBlank { tr.optString("trackName").ifBlank { tr.optString("track") } }
                        }
                        val artist = extractArtistField(tr)
                        val albumObj = tr.optJSONObject("album")
                        val album = albumObj?.optString("name") ?: tr.optString("album").ifBlank { tr.optString("albumName") }
                        val dur = parseDurationField(tr)
                        val rawId = tr.optString("id")
                        val vid = if (rawId.matches(Regex("^[\\w-]{11}$"))) rawId else extractVideoId(tr.optString("url").ifBlank { tr.optString("link").ifBlank { rawId } })
                        if (title.isNotBlank() || vid.isNotBlank()) {
                            rawItems.add(makeCandidate(title, artist, album, dur, vid))
                        }
                    }
                }
            }

            ext == "m3u" || ext == "m3u8" || text.startsWith("#EXTM3U") -> {
                var pendingTitle = ""
                var pendingArtist = ""
                var pendingDur: Int? = null
                text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                    when {
                        line.startsWith("#PLAYLIST:") -> {
                            val n = line.removePrefix("#PLAYLIST:").trim()
                            if (n.isNotEmpty()) playlistName = n
                        }
                        line.startsWith("#EXTINF:") -> {
                            val m = Regex("^#EXTINF:(-?\\d+)[^,]*,(.*)$").find(line)
                            val dur = m?.groupValues?.getOrNull(1)?.toIntOrNull()
                            val label = (m?.groupValues?.getOrNull(2) ?: "").trim()
                            val splitIdx = label.indexOf(" - ")
                            pendingDur = if (dur != null && dur > 0) dur else null
                            if (splitIdx > 0) {
                                pendingArtist = label.substring(0, splitIdx).trim()
                                pendingTitle = label.substring(splitIdx + 3).trim()
                            } else {
                                pendingArtist = ""
                                pendingTitle = label
                            }
                        }
                        !line.startsWith("#") -> {
                            val vid = extractVideoId(line)
                            val fallbackTitle = try {
                                URLDecoder.decode(line.substringAfterLast('/').substringBeforeLast('.'), "UTF-8")
                            } catch (_: Exception) {
                                line.substringAfterLast('/')
                            }
                            val title = pendingTitle.ifBlank { fallbackTitle }
                            rawItems.add(makeCandidate(title, pendingArtist, "", pendingDur, vid))
                            pendingTitle = ""
                            pendingArtist = ""
                            pendingDur = null
                        }
                    }
                }
            }

            ext == "csv" || (text.lineSequence().firstOrNull()?.contains(',') == true && ext != "txt") -> {
                val rows = parseCsv(text)
                if (rows.isNotEmpty()) {
                    val header = rows[0].map { it.lowercase().trim() }
                    var ti = header.indexOfFirst { Regex("^(track( name)?|title|song( name)?|name)$").matches(it) }
                    var ai = header.indexOfFirst { it.contains("artist") }
                    var bi = header.indexOfFirst { Regex("^album( name)?$").matches(it) }
                    val di = header.indexOfFirst { it.contains("duration") }
                    val ii = header.indexOfFirst { Regex("^(id|video ?id|youtube id)$").matches(it) }
                    val ui = header.indexOfFirst { it.contains("url") || it.contains("link") }
                    val hasHeader = ti >= 0 || ai >= 0
                    if (!hasHeader) {
                        ti = 0; ai = 1; bi = 2
                    }
                    rows.drop(if (hasHeader) 1 else 0).forEach { row ->
                        fun col(idx: Int) = if (idx in row.indices) row[idx].trim() else ""
                        val title = col(ti)
                        val artist = col(ai).replace(Regex(";\\s*"), ", ")
                        val album = col(bi)
                        val dur = col(di).toDoubleOrNull()?.let { if (it > 10000) (it / 1000).toInt() else it.toInt() }
                        val rawId = col(ii)
                        val vid = if (rawId.matches(Regex("^[\\w-]{11}$"))) rawId else extractVideoId(col(ui))
                        if (title.isNotBlank() || vid.isNotBlank()) {
                            rawItems.add(makeCandidate(title, artist, album, dur, vid))
                        }
                    }
                }
            }

            else -> {
                text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
                    val vid = extractVideoId(line)
                    if (vid.isNotEmpty()) {
                        rawItems.add(makeCandidate(line, "", "", null, vid))
                    } else {
                        val splitIdx = line.indexOf(" - ")
                        if (splitIdx > 0) {
                            rawItems.add(makeCandidate(line.substring(splitIdx + 3).trim(), line.substring(0, splitIdx).trim(), "", null, ""))
                        } else {
                            rawItems.add(makeCandidate(line, "", "", null, ""))
                        }
                    }
                }
            }
        }
        return Triple(playlistName.ifBlank { "Imported playlist" }, "File", rawItems)
    }

    private fun makeCandidate(
        title: String,
        artist: String,
        album: String,
        durationSec: Int?,
        videoId: String
    ): ImportCandidate {
        return if (videoId.isNotEmpty()) {
            val cleanTitle = if (title.isNotBlank() && !title.startsWith("http")) title else "YouTube video"
            ImportCandidate(
                sourceTitle = cleanTitle,
                sourceArtist = artist,
                sourceAlbum = album,
                sourceDurationSeconds = durationSec,
                sourceArtUrl = "",
                matchedTrack = Track(
                    id = videoId,
                    title = cleanTitle,
                    artist = artist,
                    durationSeconds = durationSec,
                    thumbUrl = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg",
                    album = album.ifBlank { null }
                ),
                status = ImportMatchStatus.MATCHED,
                isSelected = true
            )
        } else {
            ImportCandidate(
                sourceTitle = title,
                sourceArtist = artist,
                sourceAlbum = album,
                sourceDurationSeconds = durationSec,
                sourceArtUrl = "",
                matchedTrack = null,
                status = ImportMatchStatus.PENDING,
                isSelected = true
            )
        }
    }

    private fun extractArtistField(tr: JSONObject): String {
        val raw = tr.opt("artists") ?: tr.opt("artist") ?: tr.opt("artistName") ?: tr.opt("artist_name") ?: return ""
        return when (raw) {
            is JSONArray -> (0 until raw.length()).mapNotNull { i ->
                when (val el = raw.get(i)) {
                    is JSONObject -> el.optString("name")
                    is String -> el
                    else -> null
                }
            }.joinToString(", ")
            is JSONObject -> raw.optString("name")
            is String -> raw
            else -> ""
        }
    }

    private fun parseDurationField(tr: JSONObject): Int? {
        val v = tr.optDouble("duration", Double.NaN).takeIf { !it.isNaN() }
            ?: tr.optDouble("durationMs", Double.NaN).takeIf { !it.isNaN() }
            ?: tr.optDouble("duration_ms", Double.NaN).takeIf { !it.isNaN() }
            ?: tr.optDouble("lengthSeconds", Double.NaN).takeIf { !it.isNaN() }
            ?: return null
        if (v <= 0) return null
        return if (v > 10000) (v / 1000.0).toInt() else v.toInt()
    }

    private fun parseCsv(t: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < t.length) {
            val ch = t[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < t.length && t[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(cell.toString())
                        cell.clear()
                    }
                    '\n', '\r' -> {
                        if (ch == '\r' && i + 1 < t.length && t[i + 1] == '\n') i++
                        row.add(cell.toString())
                        cell.clear()
                        if (row.any { it.isNotEmpty() }) rows.add(row)
                        row = ArrayList()
                    }
                    else -> cell.append(ch)
                }
            }
            i++
        }
        row.add(cell.toString())
        if (row.any { it.isNotEmpty() }) rows.add(row)
        return rows
    }

    fun exportPlaylist(name: String, tracks: List<Track>, format: String): Triple<String, String, String> {
        val safeName = name.replace(Regex("[^\\w\\- ]+"), "").trim().ifBlank { "playlist" }
        val url: (Track) -> String = { "https://music.youtube.com/watch?v=${it.id}" }
        return when (format.lowercase()) {
            "json" -> {
                val root = JSONObject().apply {
                    put("app", "Pulse")
                    put("version", 1)
                    put("name", name)
                    put("tracks", JSONArray().apply {
                        tracks.forEach { t ->
                            put(JSONObject().apply {
                                put("title", t.title)
                                put("artist", t.artist)
                                put("album", t.album ?: "")
                                put("duration", t.durationSeconds ?: JSONObject.NULL)
                                put("id", t.id)
                                put("url", url(t))
                                put("thumbnail", t.artworkUrl(544))
                            })
                        }
                    })
                }
                Triple("$safeName.json", "application/json", root.toString(2))
            }
            "csv" -> {
                fun q(s: String?) = "\"" + (s ?: "").replace("\"", "\"\"") + "\""
                val sb = StringBuilder("Title,Artist,Album,Duration (s),Video ID,URL\r\n")
                tracks.forEach { t ->
                    sb.append(listOf(q(t.title), q(t.artist), q(t.album), q(t.durationSeconds?.toString() ?: ""), q(t.id), q(url(t))).joinToString(","))
                    sb.append("\r\n")
                }
                Triple("$safeName.csv", "text/csv", "\uFEFF$sb")
            }
            "txt" -> {
                val body = tracks.joinToString("\n") { t ->
                    if (t.artist.isNotBlank()) "${t.artist} - ${t.title}" else t.title
                }
                Triple("$safeName.txt", "text/plain", body)
            }
            else -> {
                val sb = StringBuilder("#EXTM3U\n#PLAYLIST:$name\n")
                tracks.forEach { t ->
                    val label = if (t.artist.isNotBlank()) "${t.artist} - ${t.title}" else t.title
                    sb.append("#EXTINF:${t.durationSeconds ?: -1},$label\n${url(t)}\n")
                }
                Triple("$safeName.m3u", "audio/x-mpegurl", sb.toString())
            }
        }
    }
}
