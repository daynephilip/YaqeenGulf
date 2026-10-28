package com.yaqeengulf.deepfake.xai

import android.graphics.Bitmap

/**
 * XAI results for each modality.
 */
data class ImageXaiResult(
    val heatmapBitmap: Bitmap?,
    val normalizedHeatmapGrid: Array<FloatArray>, // [H, W] normalized in [0.0, 1.0]
    val width: Int,
    val height: Int,
    val predictedClass: Int // 0: real, 1: fake
)

data class AudioXaiResult(
    val spectrogramHeatmapBitmap: Bitmap?,
    val normalizedHeatmapGrid: Array<FloatArray>, // [F, T] normalized in [0.0, 1.0]
    val timeCurve: FloatArray,                   // [T] temporal attribution for waveform overlay
    val freqBins: Int,
    val timeSteps: Int
)

data class WordHighlight(
    val word: String,
    val score: Float, // [0.0, 1.0]
    val startIndex: Int,
    val endIndex: Int
)

data class TextXaiResult(
    val text: String,
    val wordHighlights: List<WordHighlight>,
    val rawTokenHighlights: List<Pair<String, Float>>
)
