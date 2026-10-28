package com.yaqeengulf.deepfake.ui.settings

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yaqeengulf.deepfake.YaqeenApp
import com.yaqeengulf.deepfake.domain.model.ImageModelType
import com.yaqeengulf.deepfake.service.FloatingOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val app: YaqeenApp) : ViewModel() {

    val selectedImageModel: StateFlow<ImageModelType> = app.preferencesRepository.selectedImageModel
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ImageModelType.EFFICIENTNET_B3
        )

    val selectedAudioModel: StateFlow<String> = app.preferencesRepository.selectedAudioModel
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "arabic"
        )

    val isOverlayEnabled: StateFlow<Boolean> = app.preferencesRepository.isOverlayEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isCloudCheckEnabled: StateFlow<Boolean> = app.preferencesRepository.isCloudCheckEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val cloudProvider: StateFlow<String> = app.preferencesRepository.cloudProvider
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "gemini"
        )

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    fun updatePermissionStatus(context: Context) {
        _hasOverlayPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun selectImageModel(model: ImageModelType) {
        viewModelScope.launch {
            app.preferencesRepository.setImageModel(model)
            val modelType = when (model) {
                ImageModelType.EFFICIENTNET_B3 -> com.yaqeengulf.deepfake.inference.ModelType.IMAGE_EFFNET
                ImageModelType.SWIN_T -> com.yaqeengulf.deepfake.inference.ModelType.IMAGE_SWIN
            }
            app.modelManager.loadModel(modelType)
        }
    }

    fun selectAudioModel(model: String) {
        viewModelScope.launch {
            app.preferencesRepository.setAudioModel(model)
            val modelType = if (model.equals("arabic", ignoreCase = true)) {
                com.yaqeengulf.deepfake.inference.ModelType.VOICE_AR
            } else {
                com.yaqeengulf.deepfake.inference.ModelType.VOICE_EN
            }
            app.modelManager.loadModel(modelType)
        }
    }

    fun toggleOverlay(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setOverlayEnabled(enabled)
            if (enabled) {
                if (hasOverlayPermission.value) {
                    FloatingOverlayService.start(context)
                }
            } else {
                FloatingOverlayService.stop(context)
            }
        }
    }

    fun toggleCloudCheck(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setCloudCheckEnabled(enabled)
        }
    }

    fun selectCloudProvider(provider: String) {
        viewModelScope.launch {
            app.preferencesRepository.setCloudProvider(provider)
        }
    }

    class Factory(private val app: YaqeenApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(app) as T
        }
    }
}
