package com.yaqeengulf.deepfake.xai

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Audio CAM Post-Processor for SpectrogramCNN (English and Arabic).
 *
 * Implements the verified analytical gradient formula for the 2-layer classifier head:
 * Linear(256, 128) -> ReLU -> Linear(128, 2)
 *
 * Let g = GAP(feat) = mean_{h, w}(feat) [256]
 * z_1 = W_1 * g + b_1 [128]
 * mask_j = I(z_1[j] > 0)
 * alpha_k^(c) = sum_j W_2[c, j] * mask_j * W_1[j, k] [256]
 *
 * 2D Heatmap: M(f, t) = ReLU(sum_k alpha_k^(c) * A_k(f, t)) [10, 50]
 * 1D Temporal Waveform Curve: timeCurve(t) = mean_f M(f, t) [50]
 */
object AudioCamPostProcessor {

    suspend fun processAudioCam(
        featureMap: FloatArray, // [256, 10, 50]
        weights: AudioFcWeights,
        targetClass: Int = 1,   // 1 = fake class
        upscaleWidth: Int = 401,
        upscaleHeight: Int = 80,
        createBitmap: Boolean = true
    ): AudioXaiResult = withContext(Dispatchers.Default) {
        val channels = 256
        val fDim = 10
        val tDim = 50
        val spatialSize = fDim * tDim

        // 1. Compute Global Average Pooling g [256]
        val g = FloatArray(channels)
        for (c in 0 until channels) {
            var sum = 0f
            val offset = c * spatialSize
            for (i in 0 until spatialSize) {
                sum += featureMap[offset + i]
            }
            g[c] = sum / spatialSize
        }

        // 2. First layer linear transformation: z_1 = W_1 * g + b_1 [128]
        val hiddenDim = 128
        val z1 = FloatArray(hiddenDim)
        val reluMask = FloatArray(hiddenDim)

        for (j in 0 until hiddenDim) {
            var dot = weights.layer1Bias[j]
            val w1Row = weights.layer1Weight[j]
            for (k in 0 until channels) {
                dot += w1Row[k] * g[k]
            }
            z1[j] = dot
            reluMask[j] = if (dot > 0f) 1f else 0f
        }

        // 3. Exact analytical gradient alpha_k^(c) = sum_j W_2[c, j] * mask_j * W_1[j, k] [256]
        val alpha = FloatArray(channels)
        val w2Row = weights.layer2Weight[targetClass]

        for (k in 0 until channels) {
            var sum = 0f
            for (j in 0 until hiddenDim) {
                if (reluMask[j] > 0f) {
                    sum += w2Row[j] * weights.layer1Weight[j][k]
                }
            }
            alpha[k] = sum
        }

        // 4. Compute 2D CAM grid [10, 50]
        val rawGrid = Array(fDim) { FloatArray(tDim) }
        for (c in 0 until channels) {
            val a = alpha[c]
            val offset = c * spatialSize
            for (f in 0 until fDim) {
                for (t in 0 until tDim) {
                    rawGrid[f][t] += a * featureMap[offset + f * tDim + t]
                }
            }
        }

        // ReLU on activations
        for (f in 0 until fDim) {
            for (t in 0 until tDim) {
                if (rawGrid[f][t] < 0f) {
                    rawGrid[f][t] = 0f
                }
            }
        }

        // 5. Min-max normalization on the 2D grid
        val normalizedGrid = ImageCamPostProcessor.normalizeMinMax(rawGrid, fDim, tDim)

        // 6. Compute 1D temporal curve across time steps [50]
        val timeCurve = FloatArray(tDim)
        for (t in 0 until tDim) {
            var sum = 0f
            for (f in 0 until fDim) {
                sum += normalizedGrid[f][t]
            }
            timeCurve[t] = sum / fDim
        }

        // 7. Upscale grid to full spectrogram dimensions [80, 401]
        val upscaledGrid = ImageCamPostProcessor.bilinearUpscale(
            normalizedGrid,
            fDim,
            tDim,
            upscaleHeight,
            upscaleWidth
        )

        val bitmap = if (createBitmap) {
            ImageCamPostProcessor.renderHeatmapBitmap(upscaledGrid, upscaleWidth, upscaleHeight)
        } else {
            null
        }

        AudioXaiResult(
            spectrogramHeatmapBitmap = bitmap,
            normalizedHeatmapGrid = upscaledGrid,
            timeCurve = timeCurve,
            freqBins = upscaleHeight,
            timeSteps = upscaleWidth
        )
    }
}
