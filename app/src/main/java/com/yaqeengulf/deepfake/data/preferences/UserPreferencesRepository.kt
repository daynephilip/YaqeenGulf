package com.yaqeengulf.deepfake.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yaqeengulf.deepfake.domain.model.ImageModelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yaqeen_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_IMAGE_MODEL = stringPreferencesKey("key_image_model")
        val KEY_AUDIO_MODEL = stringPreferencesKey("key_audio_model")
        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("key_overlay_enabled")
        val KEY_CLOUD_CHECK_ENABLED = booleanPreferencesKey("key_cloud_check_enabled")
        val KEY_CLOUD_PROVIDER = stringPreferencesKey("key_cloud_provider")
    }

    val selectedAudioModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_AUDIO_MODEL] ?: "arabic"
    }

    suspend fun setAudioModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_AUDIO_MODEL] = model
        }
    }

    val selectedImageModel: Flow<ImageModelType> = context.dataStore.data.map { preferences ->
        val modelId = preferences[PreferencesKeys.KEY_IMAGE_MODEL] ?: ImageModelType.EFFICIENTNET_B3.id
        ImageModelType.fromId(modelId)
    }

    suspend fun setImageModel(model: ImageModelType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_IMAGE_MODEL] = model.id
        }
    }

    val isOverlayEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_OVERLAY_ENABLED] ?: false
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_OVERLAY_ENABLED] = enabled
        }
    }

    val isCloudCheckEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_CLOUD_CHECK_ENABLED] ?: false
    }

    suspend fun setCloudCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_CLOUD_CHECK_ENABLED] = enabled
        }
    }

    val cloudProvider: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_CLOUD_PROVIDER] ?: "groq"
    }

    suspend fun setCloudProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_CLOUD_PROVIDER] = provider
        }
    }
}
