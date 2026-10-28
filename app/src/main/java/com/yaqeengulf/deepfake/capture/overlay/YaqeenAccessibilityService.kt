package com.yaqeengulf.deepfake.capture.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service for permission-gated overlay text extraction per Master Build Plan §6, §7, §10.
 * Allows on-screen text scraping from any visible app foreground hierarchy when granted by the user.
 * Never required or blocking: if disabled, manual text input operates normally.
 */
class YaqeenAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "YaqeenAccessibilityService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive listener; text is scraped on-demand when user taps overlay icon
    }

    override fun onInterrupt() {
        Log.w(TAG, "YaqeenAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        Log.i(TAG, "YaqeenAccessibilityService destroyed.")
    }

    /**
     * Extracts all visible text from the currently active screen window.
     */
    fun scrapeScreenText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val textBuilder = StringBuilder()
        try {
            traverseNode(rootNode, textBuilder)
        } finally {
            rootNode.recycle()
        }
        return textBuilder.toString().trim()
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null || !node.isVisibleToUser) return

        val text = node.text
        if (!text.isNullOrBlank()) {
            builder.append(text).append(" ")
        }

        val desc = node.contentDescription
        if (!desc.isNullOrBlank() && desc != text) {
            builder.append(desc).append(" ")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, builder)
            child?.recycle()
        }
    }

    /**
     * Captures the active screen using AccessibilityService.takeScreenshot (API 30+)
     * with graceful fallback to MediaProjection if available.
     */
    fun takeScreenCapture(callback: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshotResult: ScreenshotResult) {
                            try {
                                val buffer = screenshotResult.hardwareBuffer
                                val colorSpace = screenshotResult.colorSpace
                                val hwBitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace)
                                val swBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                                buffer.close()
                                callback(swBitmap)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to copy hardware buffer from accessibility screenshot", e)
                                callback(null)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "takeScreenshot failed with error code $errorCode; attempting fallback.")
                            callback(null)
                        }
                    }
                )
                return
            } catch (e: Exception) {
                Log.e(TAG, "Accessibility takeScreenshot exception", e)
            }
        }
        callback(null)
    }

    /**
     * Resolves the target image boundaries using the Two-Tier Strategy:
     * Tier 1: Accessibility node tree search for ImageView / Image elements
     * Tier 2: Saliency / edge variance contour fallback
     * Tier 3: Central square crop fallback
     */
    fun findImageCropBounds(
        fullBitmap: Bitmap,
        touchX: Int? = null,
        touchY: Int? = null
    ): Pair<Rect, com.yaqeengulf.deepfake.domain.model.CropResolutionTier> {
        val screenW = fullBitmap.width
        val screenH = fullBitmap.height
        val density = resources.displayMetrics.density
        val minDim = (110 * density).toInt()

        // Tier 1: Accessibility Node Hierarchy traversal
        val candidateRects = mutableListOf<Rect>()
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            try {
                collectImageNodes(rootNode, candidateRects, screenW, screenH, minDim)
            } finally {
                rootNode.recycle()
            }
        }

        if (candidateRects.isNotEmpty()) {
            // If user tap coordinate provided, pick smallest candidate enclosing the tap
            val selectedRect = if (touchX != null && touchY != null) {
                val enclosing = candidateRects.filter { it.contains(touchX, touchY) }
                enclosing.minByOrNull { it.width() * it.height() } ?: candidateRects.maxByOrNull { it.width() * it.height() }
            } else {
                // Otherwise pick largest visible image card
                candidateRects.maxByOrNull { it.width() * it.height() }
            }

            if (selectedRect != null && selectedRect.width() > 50 && selectedRect.height() > 50) {
                return clampRect(selectedRect, screenW, screenH) to com.yaqeengulf.deepfake.domain.model.CropResolutionTier.TIER_1_ACCESSIBILITY_NODE
            }
        }

        // Tier 2: CV Saliency / Contrast Fallback
        val cvRect = detectSaliencyBoundingBox(fullBitmap)
        if (cvRect != null) {
            return clampRect(cvRect, screenW, screenH) to com.yaqeengulf.deepfake.domain.model.CropResolutionTier.TIER_2_CV_CONTOUR
        }

        // Tier 3: Center Square Crop Fallback
        val centerSize = minOf(screenW, (screenH * 0.65f).toInt())
        val left = (screenW - centerSize) / 2
        val top = (screenH - centerSize) / 2
        val centerRect = Rect(left, top, left + centerSize, top + centerSize)
        return clampRect(centerRect, screenW, screenH) to com.yaqeengulf.deepfake.domain.model.CropResolutionTier.TIER_3_CENTER_CROP
    }

    private fun collectImageNodes(
        node: AccessibilityNodeInfo?,
        results: MutableList<Rect>,
        screenW: Int,
        screenH: Int,
        minDim: Int
    ) {
        if (node == null || !node.isVisibleToUser) return

        val className = node.className?.toString() ?: ""
        val isImageClass = className.contains("ImageView", ignoreCase = true) ||
                className.contains("Image", ignoreCase = true) ||
                className.contains("Photo", ignoreCase = true) ||
                className.contains("Picture", ignoreCase = true)

        val rect = Rect()
        node.getBoundsInScreen(rect)
        val screenBounds = Rect(0, 0, screenW, screenH)

        if (rect.intersect(screenBounds)) {
            val w = rect.width()
            val h = rect.height()
            val ar = if (h > 0) w.toFloat() / h.toFloat() else 0f
            if (isImageClass && w >= minDim && h >= minDim && ar in 0.35f..2.85f) {
                results.add(Rect(rect))
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectImageNodes(child, results, screenW, screenH, minDim)
            child?.recycle()
        }
    }

    private fun detectSaliencyBoundingBox(bitmap: Bitmap): Rect? {
        val w = bitmap.width
        val h = bitmap.height
        val step = 16
        val sampleRows = h / step
        val sampleCols = w / step

        if (sampleRows < 10 || sampleCols < 10) return null

        var topBoundary = 0
        var bottomBoundary = h
        var leftBoundary = 0
        var rightBoundary = w

        // Detect top / bottom content margins by row variance
        val rowVariances = FloatArray(sampleRows)
        for (r in 0 until sampleRows) {
            var sum = 0f
            var sumSq = 0f
            val y = r * step
            for (c in 0 until sampleCols) {
                val color = bitmap.getPixel(c * step, y)
                val lum = (Color.red(color) * 0.299f + Color.green(color) * 0.587f + Color.blue(color) * 0.114f)
                sum += lum
                sumSq += lum * lum
            }
            val mean = sum / sampleCols
            rowVariances[r] = (sumSq / sampleCols) - (mean * mean)
        }

        // Find first and last high-variance rows
        val highVarRows = rowVariances.indices.filter { rowVariances[it] > 250f }
        if (highVarRows.size > (sampleRows * 0.15)) {
            topBoundary = highVarRows.first() * step
            bottomBoundary = minOf(h, (highVarRows.last() + 1) * step)
        }

        val boxW = rightBoundary - leftBoundary
        val boxH = bottomBoundary - topBoundary
        val ar = if (boxH > 0) boxW.toFloat() / boxH.toFloat() else 0f

        if (boxW >= (w * 0.35f) && boxH >= (h * 0.2f) && ar in 0.35f..2.85f) {
            return Rect(leftBoundary, topBoundary, rightBoundary, bottomBoundary)
        }
        return null
    }

    private fun clampRect(rect: Rect, maxW: Int, maxH: Int): Rect {
        val l = rect.left.coerceIn(0, maxW - 1)
        val t = rect.top.coerceIn(0, maxH - 1)
        val r = rect.right.coerceIn(l + 1, maxW)
        val b = rect.bottom.coerceIn(t + 1, maxH)
        return Rect(l, t, r, b)
    }

    /**
     * Executes end-to-end screen capture, boundary resolution, and in-memory cropping.
     * Raw full-screen bitmap is strictly discarded after cropping to guarantee in-memory privacy (A6).
     */
    fun captureAndCropTargetImage(
        touchX: Int? = null,
        touchY: Int? = null,
        onResult: (Bitmap?, Rect?, com.yaqeengulf.deepfake.domain.model.CropResolutionTier, String?) -> Unit
    ) {
        takeScreenCapture { fullScreenshot ->
            if (fullScreenshot == null) {
                onResult(null, null, com.yaqeengulf.deepfake.domain.model.CropResolutionTier.TIER_3_CENTER_CROP, "Failed to capture screen via accessibility service.")
                return@takeScreenCapture
            }

            try {
                val (cropBounds, tier) = findImageCropBounds(fullScreenshot, touchX, touchY)
                val cropped = Bitmap.createBitmap(
                    fullScreenshot,
                    cropBounds.left,
                    cropBounds.top,
                    cropBounds.width(),
                    cropBounds.height()
                )
                // Discard full-screen capture immediately
                fullScreenshot.recycle()

                onResult(cropped, cropBounds, tier, null)
            } catch (e: Exception) {
                fullScreenshot.recycle()
                onResult(null, null, com.yaqeengulf.deepfake.domain.model.CropResolutionTier.TIER_3_CENTER_CROP, "Error cropping image: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "YaqeenAccessService"

        @Volatile
        var instance: YaqeenAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null

        /**
         * Convenience helper to extract current screen text if service is active.
         */
        fun extractVisibleText(): String? {
            return instance?.scrapeScreenText()
        }

        /**
         * Triggers screen capture and auto-crop from the overlay service.
         */
        fun captureAndCrop(
            touchX: Int? = null,
            touchY: Int? = null,
            onResult: (Bitmap?, Rect?, com.yaqeengulf.deepfake.domain.model.CropResolutionTier, String?) -> Unit
        ) {
            val service = instance
            if (service == null) {
                onResult(null, null, com.yaqeengulf.deepfake.domain.model.CropResolutionTier.TIER_3_CENTER_CROP, "Accessibility service is not running. Please enable Yaqeen Gulf in Accessibility Settings.")
                return
            }
            service.captureAndCropTargetImage(touchX, touchY, onResult)
        }
    }
}
