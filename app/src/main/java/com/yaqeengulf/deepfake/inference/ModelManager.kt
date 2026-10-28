package com.yaqeengulf.deepfake.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import com.yaqeengulf.deepfake.domain.model.ImageModelType
import com.yaqeengulf.deepfake.inference.preprocessor.AudioMelPreprocessor
import com.yaqeengulf.deepfake.inference.preprocessor.ImagePreprocessor
import com.yaqeengulf.deepfake.inference.preprocessor.WordPieceTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * ModelManager enforces strict single-session memory residency per Build Plan §7, §10, §12.
 * Only ONE ONNX Runtime model is resident in memory at any given time.
 * When switching models, previous OrtSession is safely and completely closed.
 */
class ModelManager(private val context: Context) {

    private val mutex = Mutex()
    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()

    private var currentModelType: ModelType? = null
    private var currentSession: OrtSession? = null

    // Lazy tokenizers and preprocessors
    private var enTokenizer: WordPieceTokenizer? = null
    private var arTokenizer: WordPieceTokenizer? = null
    private val audioPreprocessor = AudioMelPreprocessor()

    suspend fun getActiveModelType(): ModelType? = mutex.withLock {
        currentModelType
    }

    /**
     * Unloads any currently resident model session and frees native ONNX Runtime resources.
     */
    suspend fun unloadCurrentModel() = mutex.withLock {
        currentSession?.close()
        currentSession = null
        currentModelType = null
    }

    /**
     * Loads the target model, unloading any previously loaded session.
     * Guaranteed single session in memory.
     */
    suspend fun loadModel(modelType: ModelType): OrtSession = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadModelInternal(modelType)
        }
    }

    /**
     * Image Inference for EfficientNet-B3 or Swin-T.
     */
    suspend fun runImageInference(
        bitmap: Bitmap,
        imageModelType: ImageModelType = ImageModelType.EFFICIENTNET_B3
    ): ModelInferenceResult = withContext(Dispatchers.Default) {
        val targetModel = when (imageModelType) {
            ImageModelType.EFFICIENTNET_B3 -> ModelType.IMAGE_EFFNET
            ImageModelType.SWIN_T -> ModelType.IMAGE_SWIN
        }

        val floatBuffer = ImagePreprocessor.preprocess(bitmap)
        val shape = longArrayOf(1, 3, 224, 224)

        mutex.withLock {
            val session = loadModelInternal(targetModel)
            val inputTensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)
            inputTensor.use { tensor ->
                val inputs = mapOf("input" to tensor)
                session.run(inputs).use { result ->
                    val logitsTensor = result.get("logits").get() as OnnxTensor
                    val featTensor = result.get("feature_map").get() as OnnxTensor

                    val logits = extractLogits(logitsTensor)
                    val featData = extractFloatArray(featTensor)
                    val featShape = featTensor.info.shape

                    ModelInferenceResult(
                        modelType = targetModel,
                        logits = logits,
                        xaiTensorName = "feature_map",
                        xaiShape = featShape,
                        xaiData = featData
                    )
                }
            }
        }
    }

    /**
     * Text Inference for English BERT-base or Arabic AraBERT.
     */
    suspend fun runTextInference(
        text: String,
        isArabic: Boolean
    ): ModelInferenceResult = withContext(Dispatchers.Default) {
        val targetModel = if (isArabic) ModelType.TEXT_AR else ModelType.TEXT_EN
        val tokenizer = getTokenizer(isArabic)
        val tokenized = tokenizer.tokenize(text, maxSeqLen = 64)

        val seqLen = tokenized.inputIds.size.toLong()
        val shape = longArrayOf(1, seqLen)

        // Allocate direct NIO LongBuffers for OnnxTensor
        val idsBuffer = ByteBuffer.allocateDirect(tokenized.inputIds.size * 8)
            .order(ByteOrder.nativeOrder())
            .asLongBuffer()
            .apply {
                put(tokenized.inputIds)
                rewind()
            }

        val maskBuffer = ByteBuffer.allocateDirect(tokenized.attentionMask.size * 8)
            .order(ByteOrder.nativeOrder())
            .asLongBuffer()
            .apply {
                put(tokenized.attentionMask)
                rewind()
            }

        mutex.withLock {
            val session = loadModelInternal(targetModel)
            val idsTensor = OnnxTensor.createTensor(ortEnvironment, idsBuffer, shape)
            val maskTensor = OnnxTensor.createTensor(ortEnvironment, maskBuffer, shape)

            idsTensor.use { idT ->
                maskTensor.use { maskT ->
                    val inputs = mapOf(
                        "input_ids" to idT,
                        "attention_mask" to maskT
                    )
                    session.run(inputs).use { result ->
                        val logitsTensor = result.get("logits").get() as OnnxTensor
                        val attnTensor = result.get("attention").get() as OnnxTensor

                        val logits = extractLogits(logitsTensor)
                        val attnData = extractFloatArray(attnTensor)
                        val attnShape = attnTensor.info.shape

                        ModelInferenceResult(
                            modelType = targetModel,
                            logits = logits,
                            xaiTensorName = "attention",
                            xaiShape = attnShape,
                            xaiData = attnData
                        )
                    }
                }
            }
        }
    }

    /**
     * Audio Inference using unified bilingual ArEnAV SpectrogramCNN.
     */
    suspend fun runAudioInference(
        audioSamples: FloatArray,
        isArabic: Boolean = true
    ): ModelInferenceResult = withContext(Dispatchers.Default) {
        val targetModel = ModelType.VOICE_AR
        val melBuffer = audioPreprocessor.extractMelSpectrogram(audioSamples)
        val shape = longArrayOf(1, 1, 80, 401)

        mutex.withLock {
            val session = loadModelInternal(targetModel)
            val inputTensor = OnnxTensor.createTensor(ortEnvironment, melBuffer, shape)
            inputTensor.use { tensor ->
                val inputs = mapOf("input" to tensor)
                session.run(inputs).use { result ->
                    val logitsTensor = result.get("logits").get() as OnnxTensor
                    val featTensor = result.get("feature_map").get() as OnnxTensor

                    val logits = extractLogits(logitsTensor)
                    val featData = extractFloatArray(featTensor)
                    val featShape = featTensor.info.shape

                    ModelInferenceResult(
                        modelType = targetModel,
                        logits = logits,
                        xaiTensorName = "feature_map",
                        xaiShape = featShape,
                        xaiData = featData
                    )
                }
            }
        }
    }

    /**
     * Direct test runner on arbitrary pre-formatted FloatBuffer.
     * Useful for automated unit tests verifying Phase 2 tolerances.
     */
    suspend fun runRawInference(
        modelType: ModelType,
        inputs: Map<String, Pair<FloatBuffer, LongArray>>
    ): ModelInferenceResult = mutex.withLock {
        val session = loadModelInternal(modelType)
        val createdTensors = inputs.mapValues { (_, pair) ->
            OnnxTensor.createTensor(ortEnvironment, pair.first, pair.second)
        }

        try {
            session.run(createdTensors).use { result ->
                val logitsTensor = result.get("logits").get() as OnnxTensor
                val xaiName = if (modelType.isText) "attention" else "feature_map"
                val xaiTensor = result.get(xaiName).get() as OnnxTensor

                val logits = extractLogits(logitsTensor)
                val xaiData = extractFloatArray(xaiTensor)
                val xaiShape = xaiTensor.info.shape

                ModelInferenceResult(
                    modelType = modelType,
                    logits = logits,
                    xaiTensorName = xaiName,
                    xaiShape = xaiShape,
                    xaiData = xaiData
                )
            }
        } finally {
            createdTensors.values.forEach { it.close() }
        }
    }

    private fun getOrExtractModelFile(modelType: ModelType): java.io.File {
        val modelsDir = java.io.File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        val targetFile = java.io.File(modelsDir, modelType.modelFileName)
        val assetPath = "models/${modelType.modelFileName}"

        if (!targetFile.exists() || targetFile.length() == 0L) {
            val tempFile = java.io.File(modelsDir, "${modelType.modelFileName}.tmp")
            context.assets.open(assetPath).use { input ->
                java.io.FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            tempFile.renameTo(targetFile)
        }
        return targetFile
    }

    private fun loadModelInternal(modelType: ModelType): OrtSession {
        if (currentModelType == modelType && currentSession != null) {
            return currentSession!!
        }

        currentSession?.close()
        currentSession = null
        currentModelType = null

        val modelFile = getOrExtractModelFile(modelType)
        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
        }
        val session = ortEnvironment.createSession(modelFile.absolutePath, sessionOptions)
        currentSession = session
        currentModelType = modelType
        return session
    }

    fun getTokenizer(isArabic: Boolean): WordPieceTokenizer {
        return if (isArabic) {
            if (arTokenizer == null) {
                arTokenizer = WordPieceTokenizer.fromAsset(context, "vocab/vocab_ar.txt", isLowerCase = false)
            }
            arTokenizer!!
        } else {
            if (enTokenizer == null) {
                enTokenizer = WordPieceTokenizer.fromAsset(context, "vocab/vocab_en.txt", isLowerCase = true)
            }
            enTokenizer!!
        }
    }

    private fun extractLogits(tensor: OnnxTensor): FloatArray {
        val fb = tensor.floatBuffer
        val arr = FloatArray(fb.remaining())
        fb.get(arr)
        return arr
    }

    private fun extractFloatArray(tensor: OnnxTensor): FloatArray {
        val fb = tensor.floatBuffer
        val arr = FloatArray(fb.remaining())
        fb.get(arr)
        return arr
    }
}
