package com.yaqeengulf.deepfake.data.local

/**
 * Entity representing a scan history record.
 * Per master build plan §10 Phase 6, results are stored locally for user review.
 */
data class ScanHistoryEntity(
    val id: Long = 0,
    val modality: String, // "IMAGE", "TEXT", "AUDIO"
    val verdict: String,  // "REAL", "SYNTHETIC", "SUSPICIOUS"
    val confidenceScore: Float, // 0.0 - 1.0
    val modelUsed: String, // e.g. "EfficientNet-B3", "Swin-T", "AraBERT"
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String,
    val sourceIdentifier: String? = null, // file name or snippet
    val sourceType: String = "on_device" // "on_device" vs "cloud"
)
