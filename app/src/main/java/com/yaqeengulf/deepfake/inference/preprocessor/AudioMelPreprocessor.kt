package com.yaqeengulf.deepfake.inference.preprocessor

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin Mel-Spectrogram Preprocessor matching spectrogram_dataset.py exactly:
 * - sample_rate: 16000
 * - n_fft: 512
 * - win_length: 400 (Hann window)
 * - hop_length: 160
 * - n_mels: 80
 * - top_db: 80.0
 * - zero-mean, unit-std per-chunk normalization: (spec - mean) / (std + 1e-6)
 * Produces FloatBuffer [1, 1, 80, 401] for a 4.0s (64,000 samples) chunk.
 */
class AudioMelPreprocessor(
    val sampleRate: Int = 16000,
    val nFft: Int = 512,
    val winLength: Int = 400,
    val hopLength: Int = 160,
    val nMels: Int = 80,
    val fMin: Float = 0.0f,
    val fMax: Float = 8000.0f,
    val topDb: Float = 80.0f
) {
    private val numFreqBins = nFft / 2 + 1 // 257
    private val window: FloatArray = FloatArray(winLength)
    private val melFilterBank: Array<FloatArray> // [nMels][numFreqBins]

    init {
        // Hann window: 0.5 * (1 - cos(2 * pi * n / winLength))
        for (i in 0 until winLength) {
            window[i] = (0.5 * (1.0 - cos(2.0 * PI * i / winLength))).toFloat()
        }

        melFilterBank = createMelFilterBank(nMels, numFreqBins, sampleRate, fMin, fMax)
    }

    private fun hzToMel(hz: Float): Float {
        return (2595.0 * log10(1.0 + hz / 700.0)).toFloat()
    }

    private fun melToHz(mel: Float): Float {
        return (700.0 * (10.0.pow(mel / 2595.0) - 1.0)).toFloat()
    }

    private fun createMelFilterBank(
        nMels: Int,
        numBins: Int,
        sr: Int,
        fMin: Float,
        fMax: Float
    ): Array<FloatArray> {
        val fb = Array(nMels) { FloatArray(numBins) }
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)

        val melPoints = FloatArray(nMels + 2)
        val deltaMel = (melMax - melMin) / (nMels + 1)
        for (i in 0 until nMels + 2) {
            melPoints[i] = melMin + i * deltaMel
        }

        val binIndices = IntArray(nMels + 2)
        for (i in 0 until nMels + 2) {
            val hz = melToHz(melPoints[i])
            binIndices[i] = ((nFft + 1) * hz / sr).toInt()
        }

        for (m in 1..nMels) {
            val fMinus = binIndices[m - 1]
            val fZero = binIndices[m]
            val fPlus = binIndices[m + 1]

            for (k in fMinus until fZero) {
                if (fZero != fMinus && k < numBins) {
                    fb[m - 1][k] = (k - fMinus).toFloat() / (fZero - fMinus).toFloat()
                }
            }
            for (k in fZero until fPlus) {
                if (fPlus != fZero && k < numBins) {
                    fb[m - 1][k] = (fPlus - k).toFloat() / (fPlus - fZero).toFloat()
                }
            }
        }
        return fb
    }

    /**
     * Compute log-mel spectrogram tensor for audio input [4.0s @ 16kHz = 64,000 samples].
     * Target output shape: [1, 1, 80, 401]
     */
    fun extractMelSpectrogram(samples: FloatArray): FloatBuffer {
        val targetLength = 64000
        val paddedAudio = FloatArray(targetLength)
        val copyLen = minOf(samples.size, targetLength)
        System.arraycopy(samples, 0, paddedAudio, 0, copyLen)

        // torchaudio MelSpectrogram center=True padding: n_fft / 2 on both sides
        val pad = nFft / 2
        val centeredAudio = FloatArray(targetLength + 2 * pad)
        System.arraycopy(paddedAudio, 0, centeredAudio, pad, targetLength)
        // reflect pad on boundaries
        for (i in 0 until pad) {
            centeredAudio[pad - 1 - i] = paddedAudio[i + 1]
            centeredAudio[centeredAudio.size - pad + i] = paddedAudio[targetLength - 2 - i]
        }

        val numFrames = (centeredAudio.size - nFft) / hopLength + 1 // 401 frames
        val powerSpec = Array(numFrames) { FloatArray(numFreqBins) }

        val real = DoubleArray(nFft)
        val imag = DoubleArray(nFft)

        for (t in 0 until numFrames) {
            val start = t * hopLength
            for (i in 0 until nFft) {
                if (i < winLength) {
                    real[i] = (centeredAudio[start + i] * window[i]).toDouble()
                } else {
                    real[i] = 0.0
                }
                imag[i] = 0.0
            }

            fft(real, imag)

            for (k in 0 until numFreqBins) {
                val p = (real[k] * real[k] + imag[k] * imag[k]).toFloat()
                powerSpec[t][k] = p
            }
        }

        // Apply Mel Filterbank: [nMels, numFrames]
        val melSpec = Array(nMels) { FloatArray(numFrames) }
        var maxVal = 1e-10f

        for (m in 0 until nMels) {
            val filter = melFilterBank[m]
            for (t in 0 until numFrames) {
                var sum = 0.0f
                for (k in 0 until numFreqBins) {
                    sum += filter[k] * powerSpec[t][k]
                }
                val clamped = max(sum, 1e-10f)
                melSpec[m][t] = clamped
                if (clamped > maxVal) {
                    maxVal = clamped
                }
            }
        }

        // Amplitude to dB: 10 * log10(max(x, 1e-10)) - 10 * log10(maxVal), top_db=80
        val logRef = 10.0f * log10(maxVal)
        var totalSum = 0.0
        var totalSqSum = 0.0
        val totalElements = nMels * numFrames

        for (m in 0 until nMels) {
            for (t in 0 until numFrames) {
                var db = 10.0f * log10(melSpec[m][t]) - logRef
                if (db < -topDb) {
                    db = -topDb
                }
                melSpec[m][t] = db
                totalSum += db
                totalSqSum += db * db
            }
        }

        // Per-chunk normalization: (spec - mean) / (std + 1e-6)
        val mean = (totalSum / totalElements).toFloat()
        val variance = max(0.0, (totalSqSum / totalElements) - (mean * mean)).toFloat()
        val std = sqrt(variance)

        val byteBuffer = ByteBuffer.allocateDirect(totalElements * 4).order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()

        for (m in 0 until nMels) {
            for (t in 0 until numFrames) {
                val normalized = (melSpec[m][t] - mean) / (std + 1e-6f)
                floatBuffer.put(normalized)
            }
        }

        floatBuffer.rewind()
        return floatBuffer
    }

    /**
     * In-place Radix-2 Cooley-Tukey FFT.
     */
    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wStepR = cos(angle)
            val wStepI = sin(angle)

            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until halfLen) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + halfLen] * wR - imag[i + k + halfLen] * wI
                    val vI = real[i + k + halfLen] * wI + imag[i + k + halfLen] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + halfLen] = uR - vR
                    imag[i + k + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}
