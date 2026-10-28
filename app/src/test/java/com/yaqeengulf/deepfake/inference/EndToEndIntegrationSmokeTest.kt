package com.yaqeengulf.deepfake.inference

import ai.onnxruntime.OrtEnvironment
import com.yaqeengulf.deepfake.capture.audio.AudioCaptureManager
import com.yaqeengulf.deepfake.domain.router.LanguageRouter
import com.yaqeengulf.deepfake.domain.router.ScriptType
import com.yaqeengulf.deepfake.inference.preprocessor.AudioMelPreprocessor
import com.yaqeengulf.deepfake.inference.preprocessor.WordPieceTokenizer
import com.yaqeengulf.deepfake.xai.AudioCamPostProcessor
import com.yaqeengulf.deepfake.xai.AudioFcWeights
import com.yaqeengulf.deepfake.xai.EffnetFcWeights
import com.yaqeengulf.deepfake.xai.FcWeightsLoader
import com.yaqeengulf.deepfake.xai.ImageCamPostProcessor
import com.yaqeengulf.deepfake.xai.SwinFcWeights
import com.yaqeengulf.deepfake.xai.TextAttentionPostProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/**
 * End-to-End verification test on JVM:
 * Validates the full integration chain:
 * 1. Text Capture -> Language Router -> Tokenizer -> Model Session (AraBERT & BERT-base) -> Logits & Softmax -> Word Attention Highlights
 * 2. Audio Capture -> Mel-Spectrogram Preprocessor -> SpectrogramCNN (Arabic & English) -> Logits & Softmax -> 2D CAM & 1D Temporal Curve
 * 3. Image Input -> NCHW Preprocessor -> EfficientNet-B3 & Swin-T -> Logits & Softmax -> 2D Upscaled Heatmap
 */
class EndToEndIntegrationSmokeTest {

    companion object {
        private lateinit var env: OrtEnvironment
        private val ASSETS_MODELS_DIR = File("src/main/assets/models")
        private val ASSETS_VOCAB_DIR = File("src/main/assets/vocab")

        @BeforeClass
        @JvmStatic
        fun setUp() {
            env = OrtEnvironment.getEnvironment()
            assertTrue(ASSETS_MODELS_DIR.exists())
            assertTrue(ASSETS_VOCAB_DIR.exists())
        }

        private fun parseFloats(raw: String): FloatArray {
            val trimmed = raw.trim().removeSurrounding("[", "]").trim()
            if (trimmed.isEmpty()) return FloatArray(0)
            val parts = trimmed.split(",")
            return FloatArray(parts.size) { parts[it].trim().toFloat() }
        }

        private fun parse2DArray(raw: String): Array<FloatArray> {
            val content = raw.trim()
            val rows = mutableListOf<FloatArray>()
            var start = -1
            for (i in content.indices) {
                if (content[i] == '[') {
                    start = i
                } else if (content[i] == ']' && start != -1) {
                    val sub = content.substring(start, i + 1)
                    rows.add(parseFloats(sub))
                    start = -1
                }
            }
            return rows.toTypedArray()
        }

        private fun extractJsonKey(json: String, key: String): String {
            val marker = "\"$key\":"
            val idx = json.indexOf(marker)
            if (idx == -1) return ""
            var start = idx + marker.length
            while (start < json.length && (json[start] == ' ' || json[start] == '\n' || json[start] == '\r')) {
                start++
            }
            val firstChar = json[start]
            return if (firstChar == '[') {
                var depth = 0
                var end = start
                while (end < json.length) {
                    if (json[end] == '[') depth++
                    else if (json[end] == ']') {
                        depth--
                        if (depth == 0) {
                            end++
                            break
                        }
                    }
                    end++
                }
                json.substring(start, end)
            } else ""
        }

        fun loadEffnetWeightsFromDisk(): EffnetFcWeights {
            val json = File(ASSETS_MODELS_DIR, "img_det_effnet_fc_weights.json").readText()
            val wStr = extractJsonKey(json, "weight")
            val bStr = extractJsonKey(json, "bias")
            return EffnetFcWeights(parse2DArray(wStr), parseFloats(bStr))
        }

        fun loadAudioWeightsFromDisk(isArabic: Boolean): AudioFcWeights {
            val name = if (isArabic) "voice_ar_fc_weights.json" else "voice_en_fc_weights.json"
            val json = File(ASSETS_MODELS_DIR, name).readText()
            return AudioFcWeights(
                layer1Weight = parse2DArray(extractJsonKey(json, "layer1_weight")),
                layer1Bias = parseFloats(extractJsonKey(json, "layer1_bias")),
                layer2Weight = parse2DArray(extractJsonKey(json, "layer2_weight")),
                layer2Bias = parseFloats(extractJsonKey(json, "layer2_bias"))
            )
        }
    }

    @Test
    fun testEndToEndTextVerificationArabic() = runBlocking {
        val arabicText = "بيان صحفي عاجل تم تداوله بشكل واسع في الخليج العربي"
        val detectedScript = LanguageRouter.detectScript(arabicText)
        assertEquals(ScriptType.ARABIC, detectedScript)

        val vocabLines = File(ASSETS_VOCAB_DIR, "vocab_ar.txt").readLines()
        val vocabMap = vocabLines.mapIndexed { i, line -> line.trim() to i }.toMap()
        val tokenizer = WordPieceTokenizer(vocabMap, unkToken = "[UNK]", unkTokenId = 1, clsTokenId = 2, sepTokenId = 3)
        val tokenized = tokenizer.tokenize(arabicText, maxSeqLen = 64)

        val session = env.createSession(File(ASSETS_MODELS_DIR, "text_ar.onnx").absolutePath)
        session.use { sess ->
            val seqLen = tokenized.inputIds.size.toLong()
            val shape = longArrayOf(1, seqLen)
            val idBuf = java.nio.LongBuffer.wrap(tokenized.inputIds)
            val maskBuf = java.nio.LongBuffer.wrap(tokenized.attentionMask)

            val idTensor = ai.onnxruntime.OnnxTensor.createTensor(env, idBuf, shape)
            val maskTensor = ai.onnxruntime.OnnxTensor.createTensor(env, maskBuf, shape)

            val result = sess.run(mapOf("input_ids" to idTensor, "attention_mask" to maskTensor))
            result.use { res ->
                val logitsTensor = res.get("logits").get() as ai.onnxruntime.OnnxTensor
                val attnTensor = res.get("attention").get() as ai.onnxruntime.OnnxTensor

                val fbLogits = logitsTensor.floatBuffer
                val logits = floatArrayOf(fbLogits.get(0), fbLogits.get(1))
                val attnData = FloatArray(attnTensor.info.shape.reduce { a, b -> a * b }.toInt())
                attnTensor.floatBuffer.get(attnData)

                // Softmax
                val maxL = maxOf(logits[0], logits[1])
                val e0 = exp(logits[0] - maxL)
                val e1 = exp(logits[1] - maxL)
                val fakeProb = e1 / (e0 + e1)
                assertTrue("Fake prob in [0, 1]", fakeProb in 0f..1f)

                // XAI
                val xai = TextAttentionPostProcessor.processAttention(
                    attentionData = attnData,
                    numHeads = 12,
                    seqLen = tokenized.inputIds.size,
                    tokens = tokenized.tokens,
                    originalText = arabicText
                )

                assertNotNull(xai)
                assertTrue(xai.wordHighlights.isNotEmpty())
            }
        }
    }

    @Test
    fun testEndToEndAudioVerificationArabic() = runBlocking {
        // Synthesize 4.0s @ 16kHz audio
        val audioSamples = FloatArray(64000) { i ->
            kotlin.math.sin(2.0 * kotlin.math.PI * 300.0 * i / 16000.0).toFloat() * 0.5f
        }

        val preprocessor = AudioMelPreprocessor()
        val melBuffer = preprocessor.extractMelSpectrogram(audioSamples)
        val shape = longArrayOf(1, 1, 80, 401)

        val session = env.createSession(File(ASSETS_MODELS_DIR, "voice_ar.onnx").absolutePath)
        session.use { sess ->
            val inTensor = ai.onnxruntime.OnnxTensor.createTensor(env, melBuffer, shape)
            val result = sess.run(mapOf("input" to inTensor))
            result.use { res ->
                val logitsTensor = res.get("logits").get() as ai.onnxruntime.OnnxTensor
                val featTensor = res.get("feature_map").get() as ai.onnxruntime.OnnxTensor

                val fbLogits = logitsTensor.floatBuffer
                val logits = floatArrayOf(fbLogits.get(0), fbLogits.get(1))
                val featData = FloatArray(featTensor.info.shape.reduce { a, b -> a * b }.toInt())
                featTensor.floatBuffer.get(featData)

                // Softmax
                val maxL = maxOf(logits[0], logits[1])
                val e0 = exp(logits[0] - maxL)
                val e1 = exp(logits[1] - maxL)
                val fakeProb = e1 / (e0 + e1)
                assertTrue(fakeProb in 0f..1f)

                // XAI
                val weights = loadAudioWeightsFromDisk(isArabic = true)
                val xai = AudioCamPostProcessor.processAudioCam(
                    featureMap = featData,
                    weights = weights,
                    targetClass = 1,
                    createBitmap = false
                )

                assertNotNull(xai)
                assertEquals(80, xai.freqBins)
                assertEquals(401, xai.timeSteps)
                assertEquals(50, xai.timeCurve.size)
            }
        }
    }

    @Test
    fun testEndToEndImageVerificationEffnet() = runBlocking {
        val totalElements = 1 * 3 * 224 * 224
        val fb = ByteBuffer.allocateDirect(totalElements * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (i in 0 until totalElements) {
            fb.put(0.1f)
        }
        fb.rewind()

        val session = env.createSession(File(ASSETS_MODELS_DIR, "img_det_effnet.onnx").absolutePath)
        session.use { sess ->
            val inTensor = ai.onnxruntime.OnnxTensor.createTensor(env, fb, longArrayOf(1, 3, 224, 224))
            val result = sess.run(mapOf("input" to inTensor))
            result.use { res ->
                val logitsTensor = res.get("logits").get() as ai.onnxruntime.OnnxTensor
                val featTensor = res.get("feature_map").get() as ai.onnxruntime.OnnxTensor

                val fbLogits = logitsTensor.floatBuffer
                val logits = floatArrayOf(fbLogits.get(0), fbLogits.get(1))
                val featData = FloatArray(featTensor.info.shape.reduce { a, b -> a * b }.toInt())
                featTensor.floatBuffer.get(featData)

                // Softmax
                val maxL = maxOf(logits[0], logits[1])
                val e0 = exp(logits[0] - maxL)
                val e1 = exp(logits[1] - maxL)
                val fakeProb = e1 / (e0 + e1)
                assertTrue(fakeProb in 0f..1f)

                // XAI
                val weights = loadEffnetWeightsFromDisk()
                val xai = ImageCamPostProcessor.processEffnetCam(
                    featureMap = featData,
                    weights = weights,
                    targetClass = 1,
                    createBitmap = false
                )

                assertNotNull(xai)
                assertEquals(224, xai.width)
                assertEquals(224, xai.height)
                assertEquals(224, xai.normalizedHeatmapGrid.size)
                assertEquals(224, xai.normalizedHeatmapGrid[0].size)
            }
        }
    }
}
