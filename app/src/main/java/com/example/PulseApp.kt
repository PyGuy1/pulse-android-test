package com.example

import android.app.Application
import com.example.data.local.PreferencesManager
import com.example.data.local.PulseDatabase
import com.example.data.remote.MusicApiClient
import com.example.data.remote.MusicConstants
import com.example.data.repository.DownloadRepository
import com.example.data.repository.MusicRepository
import com.example.service.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Application) {
    val database = PulseDatabase.getInstance(context)
    val dao = database.pulseDao()
    val prefs = PreferencesManager(context)
    val apiClient = MusicApiClient()
    val musicRepository = MusicRepository(dao, prefs, apiClient)
    val downloadRepository = DownloadRepository(context, dao, apiClient, prefs)
    val playbackController = PlaybackController(context, musicRepository, downloadRepository, prefs)
}

class PulseApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        appScope.launch {
            container.prefs.customPipedUrl.collect { url ->
                container.apiClient.customPipedUrl = url
            }
        }
        appScope.launch {
            container.downloadRepository.syncExistingDownloadsToPublicFolder()
        }
        appScope.launch {
            container.apiClient.discoverInstances()
        }
        appScope.launch {
            val existing = container.prefs.getDetectedRegionOnce()
            if (existing.isNullOrBlank()) {
                val deviceRegion = MusicConstants.detectDeviceCountryCode()
                container.prefs.setDetectedRegion(deviceRegion)
                val ipCountry = container.apiClient.detectCountryViaIp()
                if (!ipCountry.isNullOrBlank()) {
                    container.prefs.setDetectedRegion(ipCountry)
                }
            }
        }
    }
}
