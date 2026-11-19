package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DownloadedTrackEntity
import com.example.data.local.PulseDatabase
import com.example.data.model.Track
import com.example.data.remote.MusicFilter
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: PulseDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appNameMatchesPulse() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pulse", appName)
    }

    @Test
    fun downloadedTrackPersistsInDownloadsFolderDao() = runTest {
        val dao = db.pulseDao()
        val entity = DownloadedTrackEntity(
            id = "dQw4w9WgXcQ",
            title = "Midnight City",
            artist = "M83",
            durationSeconds = 243,
            thumbUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg",
            album = "Hurry Up, We're Dreaming",
            isTopic = true,
            isVerified = true,
            isMusicCatalog = true,
            localAudioPath = "/tmp/dQw4w9WgXcQ.m4a",
            localArtworkPath = "/tmp/dQw4w9WgXcQ.jpg",
            fileSizeBytes = 3_920_000L,
            quality = "High",
            publicUri = "content://media/external/downloads/42",
            publicFileName = "M83 - Midnight City.m4a"
        )
        dao.upsertDownloadedTrack(entity)

        val fetched = dao.getDownloadedTrackById("dQw4w9WgXcQ")
        assertNotNull(fetched)
        assertEquals("Midnight City", fetched?.title)
        assertEquals("content://media/external/downloads/42", fetched?.publicUri)
        assertEquals("M83 - Midnight City.m4a", fetched?.publicFileName)
        assertEquals(1, dao.getDownloadedTracksOnce().size)

        dao.deleteDownloadedTrack("dQw4w9WgXcQ")
        assertTrue(dao.getDownloadedTracksOnce().isEmpty())
    }

    @Test
    fun musicFilterCleansTitlesAndFiltersNonMusic() {
        assertEquals(
            "starboy",
            MusicFilter.baseTitle("Starboy (Official Music Video) [4K]")
        )
        val junkTrack = Track(
            id = "abc12345678",
            title = "10 Hours Sleep Rain Sounds (Slowed + Reverb)",
            artist = "Ambient Channel",
            durationSeconds = 36000
        )
        assertTrue(MusicFilter.isFiller(junkTrack))
        assertFalse(MusicFilter.okSongForSearch(junkTrack, "rain"))

        val validTrack = Track(
            id = "xyz98765432",
            title = "Blinding Lights",
            artist = "The Weeknd",
            durationSeconds = 200,
            isMusicCatalog = true
        )
        assertTrue(MusicFilter.isPure(validTrack))
        assertTrue(MusicFilter.okSongForSearch(validTrack, "Blinding Lights"))
    }

    @Test
    fun playlistImportAndExportRoundTrip() {
        val tracks = listOf(
            Track(id = "vid11111111", title = "Numb", artist = "Linkin Park", durationSeconds = 185),
            Track(id = "vid22222222", title = "In the End", artist = "Linkin Park", durationSeconds = 216)
        )
        val (fileName, _, m3uBody) = MusicFilter.exportPlaylist("Rock Mix", tracks, "m3u")
        assertEquals("Rock Mix.m3u", fileName)
        assertTrue(m3uBody.contains("https://music.youtube.com/watch?v=vid11111111"))

        val (parsedName, sourceLabel, candidates) = MusicFilter.parseImportFile("Rock Mix.m3u", m3uBody)
        assertEquals("Rock Mix", parsedName)
        assertEquals("File", sourceLabel)
        assertEquals(2, candidates.size)
        assertEquals("Numb", candidates[0].sourceTitle)
        assertEquals("vid11111111", candidates[0].matchedTrack?.id)
    }
}
