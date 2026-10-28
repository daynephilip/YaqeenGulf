package com.yaqeengulf.deepfake.overlay

import android.graphics.Bitmap
import android.graphics.Rect
import com.yaqeengulf.deepfake.domain.model.CropResolutionTier
import com.yaqeengulf.deepfake.domain.model.DualImageScanResult
import com.yaqeengulf.deepfake.xai.ImageXaiResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DualImageScanUnitTest {

    @Test
    fun testCropResolutionTiers_displayNames() {
        assertEquals("Tier 1: Accessibility Node Tree", CropResolutionTier.TIER_1_ACCESSIBILITY_NODE.displayName)
        assertEquals("Tier 2: Saliency Contour Fallback", CropResolutionTier.TIER_2_CV_CONTOUR.displayName)
        assertEquals("Tier 3: Center Square Fallback", CropResolutionTier.TIER_3_CENTER_CROP.displayName)
    }

    @Test
    fun testDualImageScanResult_agreementSynthetic() {
        val dummyXai = ImageXaiResult(
            heatmapBitmap = null,
            normalizedHeatmapGrid = Array(10) { FloatArray(10) },
            width = 10,
            height = 10,
            predictedClass = 0
        )

        val result = DualImageScanResult(
            croppedBitmap = null,
            cropBounds = null,
            resolutionTier = CropResolutionTier.TIER_1_ACCESSIBILITY_NODE,
            effnetVerdict = "SYNTHETIC",
            effnetFakeProbability = 0.945f,
            effnetConfidence = 0.945f,
            effnetCam = dummyXai,
            effnetComposite = null,
            swinVerdict = "SYNTHETIC",
            swinFakeProbability = 0.912f,
            swinConfidence = 0.912f,
            swinCam = dummyXai,
            swinComposite = null,
            modelsAgree = true,
            agreementSummary = "Both models agree: Synthetic / AI-Generated"
        )

        assertTrue(result.modelsAgree)
        assertEquals(CropResolutionTier.TIER_1_ACCESSIBILITY_NODE, result.resolutionTier)
        assertEquals("SYNTHETIC", result.effnetVerdict)
        assertEquals("SYNTHETIC", result.swinVerdict)
        assertEquals(0.945f, result.effnetFakeProbability, 0.001f)
        assertEquals(0.912f, result.swinFakeProbability, 0.001f)
        assertEquals("Both models agree: Synthetic / AI-Generated", result.agreementSummary)
    }

    @Test
    fun testDualImageScanResult_disagreementDetected() {
        val dummyXai = ImageXaiResult(
            heatmapBitmap = null,
            normalizedHeatmapGrid = Array(10) { FloatArray(10) },
            width = 10,
            height = 10,
            predictedClass = 1
        )

        val result = DualImageScanResult(
            croppedBitmap = null,
            cropBounds = null,
            resolutionTier = CropResolutionTier.TIER_2_CV_CONTOUR,
            effnetVerdict = "SYNTHETIC",
            effnetFakeProbability = 0.88f,
            effnetConfidence = 0.88f,
            effnetCam = dummyXai,
            effnetComposite = null,
            swinVerdict = "REAL",
            swinFakeProbability = 0.0005f,
            swinConfidence = 0.9995f,
            swinCam = dummyXai,
            swinComposite = null,
            modelsAgree = false,
            agreementSummary = "Models Disagree — EffNet-B3: SYNTHETIC vs Swin-T: REAL"
        )

        assertFalse(result.modelsAgree)
        assertEquals(CropResolutionTier.TIER_2_CV_CONTOUR, result.resolutionTier)
        assertEquals("SYNTHETIC", result.effnetVerdict)
        assertEquals("REAL", result.swinVerdict)
    }
}
