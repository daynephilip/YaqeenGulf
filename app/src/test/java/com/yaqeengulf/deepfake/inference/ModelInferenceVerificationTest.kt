package com.yaqeengulf.deepfake.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.abs

/**
 * JVM Verification Suite for Phase 3:
 * Loads each of the 6 exported ONNX models from app/src/main/assets/models/
 * runs inference on the exact standardized inputs,
 * verifies output tensor names ('logits', 'feature_map' or 'attention'),
 * and checks raw logits against Phase 2 expected values within tolerance (< 1e-3).
 */
class ModelInferenceVerificationTest {

    companion object {
        private lateinit var env: OrtEnvironment
        private const val TOLERANCE = 1e-3f
        private val ASSETS_DIR = File("src/main/assets/models")

        @BeforeClass
        @JvmStatic
        fun setUp() {
            env = OrtEnvironment.getEnvironment()
            assertTrue("Assets models directory must exist", ASSETS_DIR.exists())
        }

        private fun createSession(fileName: String): OrtSession {
            val modelFile = File(ASSETS_DIR, fileName)
            assertTrue("Model file $fileName must exist", modelFile.exists())
            return env.createSession(modelFile.absolutePath)
        }
    }

    @Test
    fun testEfficientNetB3Logits() {
        val session = createSession("img_det_effnet.onnx")
        session.use { sess ->
            val inputShape = longArrayOf(1, 3, 224, 224)
            val totalElements = (1 * 3 * 224 * 224)
            val fb = ByteBuffer.allocateDirect(totalElements * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            for (i in 0 until totalElements) {
                fb.put(1.0f)
            }
            fb.rewind()

            val tensor = OnnxTensor.createTensor(env, fb, inputShape)
            tensor.use { t ->
                val result = sess.run(mapOf("input" to t))
                result.use { res ->
                    assertTrue(res.get("logits").isPresent)
                    assertTrue(res.get("feature_map").isPresent)

                    val logitsTensor = res.get("logits").get() as OnnxTensor
                    val logits = FloatArray(2)
                    logitsTensor.floatBuffer.get(logits)

                    val expected0 = 0.277684897f
                    val expected1 = -0.297166079f

                    println("EffNet Logits: [${logits[0]}, ${logits[1]}]")
                    assertEquals("EffNet logit[0] diff", expected0, logits[0], TOLERANCE)
                    assertEquals("EffNet logit[1] diff", expected1, logits[1], TOLERANCE)
                }
            }
        }
    }

    @Test
    fun testSwinTLogits() {
        val session = createSession("img_det_swin.onnx")
        session.use { sess ->
            val inputShape = longArrayOf(1, 3, 224, 224)
            val totalElements = (1 * 3 * 224 * 224)
            val fb = ByteBuffer.allocateDirect(totalElements * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            for (i in 0 until totalElements) {
                fb.put(1.0f)
            }
            fb.rewind()

            val tensor = OnnxTensor.createTensor(env, fb, inputShape)
            tensor.use { t ->
                val result = sess.run(mapOf("input" to t))
                result.use { res ->
                    assertTrue(res.get("logits").isPresent)
                    assertTrue(res.get("feature_map").isPresent)

                    val logitsTensor = res.get("logits").get() as OnnxTensor
                    val logits = FloatArray(2)
                    logitsTensor.floatBuffer.get(logits)

                    val expected0 = 0.73698717f
                    val expected1 = -0.61284017f

                    println("Swin-T Logits: [${logits[0]}, ${logits[1]}]")
                    assertEquals("Swin-T logit[0] diff", expected0, logits[0], TOLERANCE)
                    assertEquals("Swin-T logit[1] diff", expected1, logits[1], TOLERANCE)
                }
            }
        }
    }

    @Test
    fun testAudioEnglishLogits() {
        val session = createSession("voice_en.onnx")
        session.use { sess ->
            val inputShape = longArrayOf(1, 1, 80, 401)
            val totalElements = (1 * 1 * 80 * 401)
            val fb = ByteBuffer.allocateDirect(totalElements * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            for (i in 0 until totalElements) {
                fb.put(1.0f)
            }
            fb.rewind()

            val tensor = OnnxTensor.createTensor(env, fb, inputShape)
            tensor.use { t ->
                val result = sess.run(mapOf("input" to t))
                result.use { res ->
                    assertTrue(res.get("logits").isPresent)
                    assertTrue(res.get("feature_map").isPresent)

                    val logitsTensor = res.get("logits").get() as OnnxTensor
                    val logits = FloatArray(2)
                    logitsTensor.floatBuffer.get(logits)

                    val expected0 = 0.33004185f
                    val expected1 = -0.31272754f

                    println("Voice EN Logits: [${logits[0]}, ${logits[1]}]")
                    assertEquals("Voice EN logit[0] diff", expected0, logits[0], TOLERANCE)
                    assertEquals("Voice EN logit[1] diff", expected1, logits[1], TOLERANCE)
                }
            }
        }
    }

    @Test
    fun testAudioArabicLogits() {
        val session = createSession("voice_ar.onnx")
        session.use { sess ->
            val inputShape = longArrayOf(1, 1, 80, 401)
            val totalElements = (1 * 1 * 80 * 401)
            val fb = ByteBuffer.allocateDirect(totalElements * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            for (i in 0 until totalElements) {
                fb.put(1.0f)
            }
            fb.rewind()

            val tensor = OnnxTensor.createTensor(env, fb, inputShape)
            tensor.use { t ->
                val result = sess.run(mapOf("input" to t))
                result.use { res ->
                    assertTrue(res.get("logits").isPresent)
                    assertTrue(res.get("feature_map").isPresent)

                    val logitsTensor = res.get("logits").get() as OnnxTensor
                    val logits = FloatArray(2)
                    logitsTensor.floatBuffer.get(logits)

                    val expected0 = -8.7593612f
                    val expected1 = 8.4063453f

                    println("Voice AR Logits: [${logits[0]}, ${logits[1]}]")
                    assertEquals("Voice AR logit[0] diff", expected0, logits[0], TOLERANCE)
                    assertEquals("Voice AR logit[1] diff", expected1, logits[1], TOLERANCE)
                }
            }
        }
    }

    @Test
    fun testTextEnglishLogits() {
        val session = createSession("text_en.onnx")
        session.use { sess ->
            val seqLen = 4
            val shape = longArrayOf(1, seqLen.toLong())

            val idBuf = LongBuffer.wrap(longArrayOf(101L, 7592L, 2088L, 102L))
            val maskBuf = LongBuffer.wrap(longArrayOf(1L, 1L, 1L, 1L))

            val idTensor = OnnxTensor.createTensor(env, idBuf, shape)
            val maskTensor = OnnxTensor.createTensor(env, maskBuf, shape)

            idTensor.use { idT ->
                maskTensor.use { maskT ->
                    val result = sess.run(mapOf("input_ids" to idT, "attention_mask" to maskT))
                    result.use { res ->
                        assertTrue(res.get("logits").isPresent)
                        assertTrue(res.get("attention").isPresent)

                        val logitsTensor = res.get("logits").get() as OnnxTensor
                        val logits = FloatArray(2)
                        logitsTensor.floatBuffer.get(logits)

                        val expected0 = -4.1881608f
                        val expected1 = 4.5184822f

                        println("Text EN Logits: [${logits[0]}, ${logits[1]}]")
                        assertEquals("Text EN logit[0] diff", expected0, logits[0], TOLERANCE)
                        assertEquals("Text EN logit[1] diff", expected1, logits[1], TOLERANCE)
                    }
                }
            }
        }
    }

    @Test
    fun testTextArabicLogits() {
        val session = createSession("text_ar.onnx")
        session.use { sess ->
            val seqLen = 4
            val shape = longArrayOf(1, seqLen.toLong())

            val idBuf = LongBuffer.wrap(longArrayOf(2L, 24570L, 19495L, 3L))
            val maskBuf = LongBuffer.wrap(longArrayOf(1L, 1L, 1L, 1L))

            val idTensor = OnnxTensor.createTensor(env, idBuf, shape)
            val maskTensor = OnnxTensor.createTensor(env, maskBuf, shape)

            idTensor.use { idT ->
                maskTensor.use { maskT ->
                    val result = sess.run(mapOf("input_ids" to idT, "attention_mask" to maskT))
                    result.use { res ->
                        assertTrue(res.get("logits").isPresent)
                        assertTrue(res.get("attention").isPresent)

                        val logitsTensor = res.get("logits").get() as OnnxTensor
                        val logits = FloatArray(2)
                        logitsTensor.floatBuffer.get(logits)

                        val expected0 = 2.4789144f
                        val expected1 = -1.9192094f

                        println("Text AR Logits: [${logits[0]}, ${logits[1]}]")
                        assertEquals("Text AR logit[0] diff", expected0, logits[0], TOLERANCE)
                        assertEquals("Text AR logit[1] diff", expected1, logits[1], TOLERANCE)
                    }
                }
            }
        }
    }
}
