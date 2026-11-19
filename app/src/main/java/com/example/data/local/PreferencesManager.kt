package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AudioQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pulse_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val AUTOPLAY = booleanPreferencesKey("autoplay")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val REGION_OVERRIDE = stringPreferencesKey("region_override")
        val DETECTED_REGION = stringPreferencesKey("detected_region")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val CUSTOM_PIPED_URL = stringPreferencesKey("custom_piped_url")
        val LAST_QUEUE_JSON = stringPreferencesKey("last_queue_json")
        val LAST_QUEUE_INDEX = intPreferencesKey("last_queue_index")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val LAST_RADIO_MODE = booleanPreferencesKey("last_radio_mode")
    }

    val autoplay: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTOPLAY] ?: true }
    val audioQuality: Flow<AudioQuality> = context.dataStore.data.map { AudioQuality.fromKey(it[Keys.AUDIO_QUALITY]) }
    val downloadQuality: Flow<AudioQuality> = context.dataStore.data.map { AudioQuality.fromKey(it[Keys.DOWNLOAD_QUALITY] ?: AudioQuality.HIGH.key) }
    val regionOverride: Flow<String?> = context.dataStore.data.map { it[Keys.REGION_OVERRIDE]?.ifBlank { null } }
    val detectedRegion: Flow<String?> = context.dataStore.data.map { it[Keys.DETECTED_REGION]?.ifBlank { null } }
    val reduceMotion: Flow<Boolean> = context.dataStore.data.map { it[Keys.REDUCE_MOTION] ?: false }
    val customPipedUrl: Flow<String> = context.dataStore.data.map { it[Keys.CUSTOM_PIPED_URL] ?: "" }

    suspend fun getAutoplayOnce(): Boolean = autoplay.first()
    suspend fun getAudioQualityOnce(): AudioQuality = audioQuality.first()
    suspend fun getDownloadQualityOnce(): AudioQuality = downloadQuality.first()
    suspend fun getCustomPipedUrlOnce(): String = customPipedUrl.first()
    suspend fun getRegionOverrideOnce(): String? = regionOverride.first()
    suspend fun getDetectedRegionOnce(): String? = detectedRegion.first()

    suspend fun setAutoplay(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY] = enabled }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.AUDIO_QUALITY] = quality.key }
    }

    suspend fun setDownloadQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.DOWNLOAD_QUALITY] = quality.key }
    }

    suspend fun setRegionOverride(regionCode: String?) {
        context.dataStore.edit {
            if (regionCode.isNullOrBlank()) it.remove(Keys.REGION_OVERRIDE)
            else it[Keys.REGION_OVERRIDE] = regionCode
        }
    }

    suspend fun setDetectedRegion(regionCode: String) {
        context.dataStore.edit { it[Keys.DETECTED_REGION] = regionCode }
    }

    suspend fun setReduceMotion(reduce: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_MOTION] = reduce }
    }

    suspend fun setCustomPipedUrl(url: String) {
        context.dataStore.edit { it[Keys.CUSTOM_PIPED_URL] = url.trim().trimEnd('/') }
    }

    suspend fun saveSession(queueJson: String, index: Int, positionMs: Long, radioMode: Boolean) {
        context.dataStore.edit {
            it[Keys.LAST_QUEUE_JSON] = queueJson
            it[Keys.LAST_QUEUE_INDEX] = index
            it[Keys.LAST_POSITION_MS] = positionMs
            it[Keys.LAST_RADIO_MODE] = radioMode
        }
    }

    suspend fun loadSession(): SavedSession? {
        val prefs = context.dataStore.data.first()
        val json = prefs[Keys.LAST_QUEUE_JSON] ?: return null
        return SavedSession(
            queueJson = json,
            index = prefs[Keys.LAST_QUEUE_INDEX] ?: 0,
            positionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            radioMode = prefs[Keys.LAST_RADIO_MODE] ?: false
        )
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

data class SavedSession(
    val queueJson: String,
    val index: Int,
    val positionMs: Long,
    val radioMode: Boolean
)
