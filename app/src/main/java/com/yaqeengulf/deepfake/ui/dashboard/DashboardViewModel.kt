package com.yaqeengulf.deepfake.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yaqeengulf.deepfake.YaqeenApp
import com.yaqeengulf.deepfake.data.local.ScanHistoryEntity
import com.yaqeengulf.deepfake.domain.model.ImageModelType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(private val app: YaqeenApp) : ViewModel() {

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
        }
    }

    val isOverlayEnabled: StateFlow<Boolean> = app.preferencesRepository.isOverlayEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleOverlay(context: android.content.Context) {
        viewModelScope.launch {
            val current = isOverlayEnabled.value
            val target = !current
            app.preferencesRepository.setOverlayEnabled(target)
            if (target) {
                if (android.provider.Settings.canDrawOverlays(context)) {
                    com.yaqeengulf.deepfake.service.FloatingOverlayService.start(context)
                } else {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
            } else {
                com.yaqeengulf.deepfake.service.FloatingOverlayService.stop(context)
            }
        }
    }

    val recentScans: StateFlow<List<ScanHistoryEntity>> = app.database.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    private val _isScanning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _currentScanResult = kotlinx.coroutines.flow.MutableStateFlow<com.yaqeengulf.deepfake.domain.orchestrator.UnifiedScanResult?>(null)
    val currentScanResult: StateFlow<com.yaqeengulf.deepfake.domain.orchestrator.UnifiedScanResult?> = _currentScanResult

    private val _cloudScanResult = kotlinx.coroutines.flow.MutableStateFlow<com.yaqeengulf.deepfake.data.cloud.CloudCheckResult?>(null)
    val cloudScanResult: StateFlow<com.yaqeengulf.deepfake.data.cloud.CloudCheckResult?> = _cloudScanResult

    private val _errorMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _navigationTarget = kotlinx.coroutines.flow.MutableStateFlow<com.yaqeengulf.deepfake.ui.navigation.Screen?>(null)
    val navigationTarget: StateFlow<com.yaqeengulf.deepfake.ui.navigation.Screen?> = _navigationTarget

    fun navigateTo(screen: com.yaqeengulf.deepfake.ui.navigation.Screen) {
        _navigationTarget.value = screen
    }

    fun clearNavigationTarget() {
        _navigationTarget.value = null
    }

    val showAudioDialog = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showTextDialog = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun clearScanResult() {
        _currentScanResult.value = null
        _cloudScanResult.value = null
        _errorMessage.value = null
    }

    fun clearCloudResult() {
        _cloudScanResult.value = null
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            app.database.deleteScan(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            app.database.clearAll()
        }
    }

    fun scanImage(
        bitmap: android.graphics.Bitmap,
        sourceTag: String = "Manual Image Pick",
        imageUri: android.net.Uri? = null
    ) {
        viewModelScope.launch {
            _isScanning.value = true
            _errorMessage.value = null
            try {
                val c2paResult = if (imageUri != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.yaqeengulf.deepfake.data.c2pa.C2paProvenanceManager.inspect(app, imageUri)
                    }
                } else {
                    com.yaqeengulf.deepfake.domain.model.ContentCredentialsResult.NotPresent
                }
                val model = selectedImageModel.value
                val result = app.verificationOrchestrator.verifyImage(bitmap, model, sourceTag, c2paResult)
                _currentScanResult.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Image verification error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun scanText(text: String, isArabic: Boolean? = null, sourceTag: String = "Manual Text Input") {
        viewModelScope.launch {
            if (text.isBlank()) return@launch
            val trimmed = text.trim()

            // Check if input contains a URL
            val extractedUrl = extractUrl(trimmed)
            if (extractedUrl != null) {
                _navigationTarget.value = com.yaqeengulf.deepfake.ui.navigation.Screen.CloudVerification
                val isCloudEnabled = app.preferencesRepository.isCloudCheckEnabled.first()
                if (!isCloudEnabled) {
                    _errorMessage.value = "Shared link detected. Enable Cloud Source-Check in Settings to verify web links with Gemini/Groq, or copy the article text directly for on-device verification."
                    return@launch
                }

                // Extract any accompanying headline/caption from the shared payload
                val headlineSnippet = trimmed.replace(extractedUrl, "").trim()
                    .takeIf { it.isNotBlank() }

                // Cloud Source-Check flow
                _isScanning.value = true
                _errorMessage.value = null
                try {
                    val provider = app.preferencesRepository.cloudProvider.first()
                    val result = app.cloudRepository.checkUrl(
                        targetUrl = extractedUrl,
                        provider = provider,
                        headlineHint = headlineSnippet
                    )
                    result.onSuccess { cloudRes ->
                        _cloudScanResult.value = cloudRes
                        // Record in Room/SQLite with explicit sourceType = 'cloud'
                        val entity = ScanHistoryEntity(
                            modality = "URL_CHECK",
                            verdict = cloudRes.verdict.uppercase(),
                            confidenceScore = cloudRes.confidence,
                            modelUsed = when {
                                cloudRes.providerUsed.contains("qwen", ignoreCase = true) -> "Groq (Qwen 3.8)"
                                cloudRes.providerUsed.contains("gemini", ignoreCase = true) -> "Google Gemini"
                                cloudRes.providerUsed.contains("gpt-oss", ignoreCase = true) || cloudRes.providerUsed.contains("gptoss", ignoreCase = true) -> "GPT-OSS (120B)"
                                else -> "GPT-OSS (120B)"
                            },
                            timestamp = System.currentTimeMillis(),
                            summary = cloudRes.reasoning,
                            sourceIdentifier = cloudRes.url,
                            sourceType = "cloud"
                        )
                        app.database.insertScan(entity)
                    }.onFailure { err ->
                        _errorMessage.value = err.message ?: "Cloud verification failed"
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Cloud verification error: ${e.message}"
                } finally {
                    _isScanning.value = false
                }
                return@launch
            }

            // Normal text: on-device verification
            _isScanning.value = true
            _errorMessage.value = null
            try {
                val result = app.verificationOrchestrator.verifyText(text, isArabic, sourceTag)
                _currentScanResult.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Text verification error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun scanAudio(samples: FloatArray, isArabic: Boolean = true, sourceTag: String = "Microphone / Audio Pick") {
        viewModelScope.launch {
            _isScanning.value = true
            _errorMessage.value = null
            try {
                val result = app.verificationOrchestrator.verifyAudio(samples, isArabic, sourceTag)
                _currentScanResult.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Audio verification error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun checkCloudUrl(url: String, headlineHint: String? = null) {
        viewModelScope.launch {
            var trimmed = url.trim()
            if (trimmed.isBlank()) return@launch
            if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed = "https://$trimmed"
            }

            val isCloudEnabled = app.preferencesRepository.isCloudCheckEnabled.first()
            if (!isCloudEnabled) {
                _errorMessage.value = "Please enable Cloud Source-Check in Settings to verify web links with Gemini/Groq."
                return@launch
            }

            _isScanning.value = true
            _errorMessage.value = null
            try {
                val provider = app.preferencesRepository.cloudProvider.first()
                val result = app.cloudRepository.checkUrl(
                    targetUrl = trimmed,
                    provider = provider,
                    headlineHint = headlineHint?.takeIf { it.isNotBlank() }
                )
                result.onSuccess { cloudRes ->
                    _cloudScanResult.value = cloudRes
                    val entity = ScanHistoryEntity(
                        modality = "URL_CHECK",
                        verdict = cloudRes.verdict.uppercase(),
                        confidenceScore = cloudRes.confidence,
                        modelUsed = when {
                            cloudRes.providerUsed.contains("qwen", ignoreCase = true) -> "Groq (Qwen 3.8)"
                            cloudRes.providerUsed.contains("gemini", ignoreCase = true) -> "Google Gemini"
                            cloudRes.providerUsed.contains("gpt-oss", ignoreCase = true) || cloudRes.providerUsed.contains("gptoss", ignoreCase = true) -> "GPT-OSS (120B)"
                            else -> "GPT-OSS (120B)"
                        },
                        timestamp = System.currentTimeMillis(),
                        summary = cloudRes.reasoning,
                        sourceIdentifier = cloudRes.url,
                        sourceType = "cloud"
                    )
                    app.database.insertScan(entity)
                }.onFailure { err ->
                    _errorMessage.value = err.message ?: "Cloud verification failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Cloud verification error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    companion object {
        private val URL_REGEX = Regex("(https?://\\S+|www\\.[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\S*)", RegexOption.IGNORE_CASE)

        fun extractUrl(text: String): String? {
            val match = URL_REGEX.find(text) ?: return null
            var url = match.value
            // Strip trailing punctuation often attached in shared text
            url = url.trimEnd('.', ',', ';', '!', '?', ')', ']', '}')
            if (url.startsWith("www.", ignoreCase = true)) {
                url = "https://$url"
            }
            return url
        }

        fun isBareUrl(text: String): Boolean {
            val trimmed = text.trim()
            val extracted = extractUrl(trimmed) ?: return false
            return trimmed.equals(extracted, ignoreCase = true) ||
                    trimmed.equals("http://" + extracted.removePrefix("https://"), ignoreCase = true) ||
                    trimmed.equals(extracted.removePrefix("https://"), ignoreCase = true)
        }
    }

    class Factory(private val app: YaqeenApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(app) as T
        }
    }
}
