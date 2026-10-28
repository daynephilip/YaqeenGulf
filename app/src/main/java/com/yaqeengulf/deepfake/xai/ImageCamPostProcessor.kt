package com.yaqeengulf.deepfake.xai

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Image CAM Post-Processor.
 * Handles both EfficientNet-B3 (CNN conv feature map [1536, 7, 7])
 * and Swin-T (Transformer patch-grid feature map [768, 7, 7]).
 * Generates an upscaled 224x224 RGBA heatmap overlay bitmap.
 */
object ImageCamPostProcessor {

    suspend fun processEffnetCam(
        featureMap: FloatArray, // [1536, 7, 7]
        weights: EffnetFcWeights,
        targetClass: Int = 1, // 1 = fake class
        upscaleWidth: Int = 224,
        upscaleHeight: Int = 224,
        createBitmap: Boolean = true
    ): ImageXaiResult = withContext(Dispatchers.Default) {
        val channels = 1536
        val h = 7
        val w = 7
        val classWeights = weights.weight[targetClass]

        val rawCam = computeWeightedFeatureGrid(featureMap, classWeights, channels, h, w)
        val normalizedGrid = normalizeMinMax(rawCam, h, w)
        val upscaledGrid = bilinearUpscale(normalizedGrid, h, w, upscaleHeight, upscaleWidth)
        val bitmap = if (createBitmap) renderHeatmapBitmap(upscaledGrid, upscaleWidth, upscaleHeight) else null

        ImageXaiResult(
            heatmapBitmap = bitmap,
            normalizedHeatmapGrid = upscaledGrid,
            width = upscaleWidth,
            height = upscaleHeight,
            predictedClass = targetClass
        )
    }

    suspend fun processSwinCam(
        featureMap: FloatArray, // [768, 7, 7] permuted from transformer patch tokens
        weights: SwinFcWeights,
        targetClass: Int = 1,
        upscaleWidth: Int = 224,
        upscaleHeight: Int = 224,
        createBitmap: Boolean = true
    ): ImageXaiResult = withContext(Dispatchers.Default) {
        val channels = 768
        val h = 7
        val w = 7
        val classWeights = weights.weight[targetClass]

        val rawCam = computeWeightedFeatureGrid(featureMap, classWeights, channels, h, w)
        val normalizedGrid = normalizeMinMax(rawCam, h, w)
        val upscaledGrid = bilinearUpscale(normalizedGrid, h, w, upscaleHeight, upscaleWidth)
        val bitmap = if (createBitmap) renderHeatmapBitmap(upscaledGrid, upscaleWidth, upscaleHeight) else null

        ImageXaiResult(
            heatmapBitmap = bitmap,
            normalizedHeatmapGrid = upscaledGrid,
            width = upscaleWidth,
            height = upscaleHeight,
            predictedClass = targetClass
        )
    }

    private fun computeWeightedFeatureGrid(
        feat: FloatArray,
        w: FloatArray,
        channels: Int,
        h: Int,
        wDim: Int
    ): Array<FloatArray> {
        val grid = Array(h) { FloatArray(wDim) }
        val hw = h * wDim

        for (c in 0 until channels) {
            val weight = w[c]
            val offset = c * hw
            for (i in 0 until h) {
                for (j in 0 until wDim) {
                    grid[i][j] += weight * feat[offset + i * wDim + j]
                }
            }
        }

        // ReLU: keep positive evidence
        for (i in 0 until h) {
            for (j in 0 until wDim) {
                if (grid[i][j] < 0f) {
                    grid[i][j] = 0f
                }
            }
        }
        return grid
    }

    fun normalizeMinMax(grid: Array<FloatArray>, h: Int, w: Int): Array<FloatArray> {
        var minVal = Float.MAX_VALUE
        var maxVal = -Float.MAX_VALUE

        for (i in 0 until h) {
            for (j in 0 until w) {
                val v = grid[i][j]
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
            }
        }

        val range = maxVal - minVal
        val norm = Array(h) { FloatArray(w) }
        for (i in 0 until h) {
            for (j in 0 until w) {
                norm[i][j] = if (range > 1e-7f) {
                    (grid[i][j] - minVal) / range
                } else {
                    0f
                }
            }
        }
        return norm
    }

    fun bilinearUpscale(
        src: Array<FloatArray>,
        srcH: Int,
        srcW: Int,
        dstH: Int,
        dstW: Int
    ): Array<FloatArray> {
        val dst = Array(dstH) { FloatArray(dstW) }
        val xRatio = if (dstW > 1) (srcW - 1).toFloat() / (dstW - 1) else 0f
        val yRatio = if (dstH > 1) (srcH - 1).toFloat() / (dstH - 1) else 0f

        for (i in 0 until dstH) {
            val y = i * yRatio
            val y1 = y.toInt()
            val y2 = min(y1 + 1, srcH - 1)
            val yDiff = y - y1

            for (j in 0 until dstW) {
                val x = j * xRatio
                val x1 = x.toInt()
                val x2 = min(x1 + 1, srcW - 1)
                val xDiff = x - x1

                val a = src[y1][x1]
                val b = src[y1][x2]
                val c = src[y2][x1]
                val d = src[y2][x2]

                val value = (a * (1 - xDiff) * (1 - yDiff) +
                        b * xDiff * (1 - yDiff) +
                        c * yDiff * (1 - xDiff) +
                        d * xDiff * yDiff)
                dst[i][j] = value
            }
        }
        return dst
    }

    fun renderHeatmapBitmap(grid: Array<FloatArray>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in 0 until height) {
            for (j in 0 until width) {
                val intensity = grid[i][j].coerceIn(0f, 1f)
                pixels[i * width + j] = intensityToHeatmapColor(intensity)
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Converts normalized intensity [0.0, 1.0] into a smooth RGBA heatmap color
     * with alpha scaling for natural overlay onto background images.
     */
    fun intensityToHeatmapColor(intensity: Float): Int {
        // Alpha increases from 0 (cool/background) to ~180 (hot region)
        val alpha = (intensity * 180f).toInt().coerceIn(0, 255)
        if (alpha == 0) return 0 // fully transparent

        // Jet-like color spectrum: Blue -> Cyan -> Green -> Yellow -> Red
        val fourX = 4f * intensity
        val r = (min(fourX - 1.5f, -fourX + 4.5f)).coerceIn(0f, 1f) * 255f
        val g = (min(fourX - 0.5f, -fourX + 3.5f)).coerceIn(0f, 1f) * 255f
        val b = (min(fourX + 0.5f, -fourX + 2.5f)).coerceIn(0f, 1f) * 255f

        return Color.argb(alpha, r.toInt(), g.toInt(), b.toInt())
    }
}
