package com.yaqeengulf.deepfake.inference

enum class ModelType(
    val modelFileName: String,
    val isImage: Boolean = false,
    val isAudio: Boolean = false,
    val isText: Boolean = false
) {
    IMAGE_EFFNET("img_det_effnet.onnx", isImage = true),
    IMAGE_SWIN("img_det_swin.onnx", isImage = true),
    VOICE_EN("voice_en.onnx", isAudio = true),
    VOICE_AR("voice_ar.onnx", isAudio = true),
    TEXT_EN("text_en.onnx", isText = true),
    TEXT_AR("text_ar.onnx", isText = true)
}

data class ModelInferenceResult(
    val modelType: ModelType,
    val logits: FloatArray,
    val xaiTensorName: String,
    val xaiShape: LongArray,
    val xaiData: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelInferenceResult

        if (modelType != other.modelType) return false
        if (!logits.contentEquals(other.logits)) return false
        if (xaiTensorName != other.xaiTensorName) return false
        if (!xaiShape.contentEquals(other.xaiShape)) return false
        if (!xaiData.contentEquals(other.xaiData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = modelType.hashCode()
        result = 31 * result + logits.contentHashCode()
        result = 31 * result + xaiTensorName.hashCode()
        result = 31 * result + xaiShape.contentHashCode()
        result = 31 * result + xaiData.contentHashCode()
        return result
    }
}
