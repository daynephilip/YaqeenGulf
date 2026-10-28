package com.yaqeengulf.deepfake.domain.model

/**
 * Model architectures available for Image Deepfake Detection.
 * Per master build plan §3, EfficientNet-B3 is the default, and Swin-T is a user-selectable alternative.
 */
enum class ImageModelType(
    val id: String,
    val displayName: String,
    val fileName: String,
    val description: String,
    val parameterCount: String
) {
    EFFICIENTNET_B3(
        id = "effnet",
        displayName = "EfficientNet-B3 (Default)",
        fileName = "img_det_effnet.onnx",
        description = "Lightweight CNN with global-average-pool. Produces plain CAM heatmaps.",
        parameterCount = "~12M params"
    ),
    SWIN_T(
        id = "swin_t",
        displayName = "Swin-T (Alternative)",
        fileName = "img_det_swin.onnx",
        description = "Hierarchical Vision Transformer. Uses patch-grid feature activation mapping.",
        parameterCount = "~28M params"
    );

    companion object {
        fun fromId(id: String?): ImageModelType {
            return values().firstOrNull { it.id == id } ?: EFFICIENTNET_B3
        }
    }
}

enum class Modality(val label: String) {
    IMAGE("Image"),
    TEXT("Text"),
    AUDIO("Audio")
}
