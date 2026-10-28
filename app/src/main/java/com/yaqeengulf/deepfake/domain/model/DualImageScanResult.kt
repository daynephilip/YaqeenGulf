package com.yaqeengulf.deepfake.domain.model

import android.graphics.Bitmap
import android.graphics.Rect
import com.yaqeengulf.deepfake.xai.ImageXaiResult

/**
 * Resolution tier used to detect and crop an on-screen image.
 * Logged per scan for thesis deployment-evaluation telemetry.
 */
enum class CropResolutionTier(val displayName: String) {
    TIER_1_ACCESSIBILITY_NODE("Tier 1: Accessibility Node Tree"),
    TIER_2_CV_CONTOUR("Tier 2: Saliency Contour Fallback"),
    TIER_3_CENTER_CROP("Tier 3: Center Square Fallback")
}

/**
 * Comprehensive dual-model verification result comparing
 * EfficientNet-B3 (CNN conv Grad-CAM) and Swin-T (Transformer patch-grid CAM)
 * on the exact same cropped image.
 */
data class DualImageScanResult(
    val croppedBitmap: Bitmap? = null,
    val cropBounds: Rect? = null,
    val resolutionTier: CropResolutionTier = CropResolutionTier.TIER_1_ACCESSIBILITY_NODE,

    // Model 1: EfficientNet-B3
    val effnetVerdict: String,
    val effnetFakeProbability: Float,
    val effnetConfidence: Float,
    val effnetCam: ImageXaiResult,
    val effnetComposite: Bitmap? = null,

    // Model 2: Swin-T
    val swinVerdict: String,
    val swinFakeProbability: Float,
    val swinConfidence: Float,
    val swinCam: ImageXaiResult,
    val swinComposite: Bitmap? = null,

    // Cross-model agreement
    val modelsAgree: Boolean,
    val agreementSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)
