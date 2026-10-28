package com.yaqeengulf.deepfake.xai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pure Kotlin parser for companion JSON weights files for JVM testing
 * to avoid android.util/org.json stubbing issues on host JVM.
 */
class XaiPostProcessorUnitTest {

    private val ASSETS_MODELS_DIR = File("src/main/assets/models")

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
        } else {
            val end = json.indexOfAny(charArrayOf(',', '}'), start)
            json.substring(start, if (end != -1) end else json.length).trim()
        }
    }

    private fun loadEffnetWeightsDirect(): EffnetFcWeights {
        val json = File(ASSETS_MODELS_DIR, "img_det_effnet_fc_weights.json").readText(Charsets.UTF_8)
        val wStr = extractJsonKey(json, "weight")
        val bStr = extractJsonKey(json, "bias")
        val weight = parse2DArray(wStr)
        val bias = parseFloats(bStr)
        return EffnetFcWeights(weight, bias)
    }

    private fun loadSwinWeightsDirect(): SwinFcWeights {
        val json = File(ASSETS_MODELS_DIR, "img_det_swin_fc_weights.json").readText(Charsets.UTF_8)
        val wStr = extractJsonKey(json, "weight")
        val bStr = extractJsonKey(json, "bias")
        val weight = parse2DArray(wStr)
        val bias = parseFloats(bStr)
        return SwinFcWeights(weight, bias)
    }

    private fun loadAudioWeightsDirect(isArabic: Boolean = true): AudioFcWeights {
        val fileName = "voice_ar_fc_weights.json"
        val json = File(ASSETS_MODELS_DIR, fileName).readText(Charsets.UTF_8)
        val w1Str = extractJsonKey(json, "layer1_weight")
        val b1Str = extractJsonKey(json, "layer1_bias")
        val w2Str = extractJsonKey(json, "layer2_weight")
        val b2Str = extractJsonKey(json, "layer2_bias")
        return AudioFcWeights(
            parse2DArray(w1Str),
            parseFloats(b1Str),
            parse2DArray(w2Str),
            parseFloats(b2Str)
        )
    }

    @Test
    fun testEffnetCamComputationAndUpscale() = runBlocking {
        val weights = loadEffnetWeightsDirect()
        assertEquals(2, weights.weight.size)
        assertEquals(1536, weights.weight[0].size)

        val featureMap = FloatArray(1536 * 7 * 7) { 0.5f }
        for (c in 0 until 1536) {
            featureMap[c * 49 + 3 * 7 + 3] = 2.0f
        }

        val result = ImageCamPostProcessor.processEffnetCam(
            featureMap = featureMap,
            weights = weights,
            targetClass = 1,
            upscaleWidth = 224,
            upscaleHeight = 224,
            createBitmap = false
        )

        assertEquals(224, result.width)
        assertEquals(224, result.height)
        assertEquals(1, result.predictedClass)
        assertNotNull(result.normalizedHeatmapGrid)

        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val v = result.normalizedHeatmapGrid[i][j]
                assertTrue("Heatmap intensity must be in [0, 1] (was $v)", v in 0f..1f)
            }
        }
        println("EffNet CAM: Center=${result.normalizedHeatmapGrid[112][112]}, Corner=${result.normalizedHeatmapGrid[0][0]}")
    }

    @Test
    fun testSwinCamComputationAndUpscale() = runBlocking {
        val weights = loadSwinWeightsDirect()
        assertEquals(2, weights.weight.size)
        assertEquals(768, weights.weight[0].size)

        val featureMap = FloatArray(768 * 7 * 7) { 0.0f }
        val classWeights = weights.weight[1]
        for (c in 0 until 768) {
            if (classWeights[c] > 0) {
                featureMap[c * 49 + 3 * 7 + 3] = 3.0f
            }
        }

        val result = ImageCamPostProcessor.processSwinCam(
            featureMap = featureMap,
            weights = weights,
            targetClass = 1,
            upscaleWidth = 224,
            upscaleHeight = 224,
            createBitmap = false
        )

        assertEquals(224, result.width)
        assertEquals(224, result.height)
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val v = result.normalizedHeatmapGrid[i][j]
                assertTrue(v in 0f..1f)
            }
        }
        assertTrue("Center should have high intensity", result.normalizedHeatmapGrid[112][112] > 0.8f)
        assertTrue("Corner should have low intensity", result.normalizedHeatmapGrid[0][0] < 0.2f)
        println("Swin-T CAM: Center=${result.normalizedHeatmapGrid[112][112]}, Corner=${result.normalizedHeatmapGrid[0][0]}")
    }

    @Test
    fun testAudioAnalyticalGradientCamAndTemporalCurve() = runBlocking {
        val weightsEn = loadAudioWeightsDirect(isArabic = false)
        assertEquals(128, weightsEn.layer1Weight.size)
        assertEquals(256, weightsEn.layer1Weight[0].size)
        assertEquals(2, weightsEn.layer2Weight.size)
        assertEquals(128, weightsEn.layer2Weight[0].size)

        val featureMap = FloatArray(256 * 10 * 50) { 0.2f }
        for (c in 0 until 256) {
            for (f in 0 until 10) {
                for (t in 20..30) {
                    featureMap[c * 500 + f * 50 + t] = 1.5f
                }
            }
        }

        val result = AudioCamPostProcessor.processAudioCam(
            featureMap = featureMap,
            weights = weightsEn,
            targetClass = 1,
            upscaleWidth = 401,
            upscaleHeight = 80,
            createBitmap = false
        )

        assertEquals(80, result.freqBins)
        assertEquals(401, result.timeSteps)
        assertEquals(50, result.timeCurve.size)

        for (t in 0 until 50) {
            val v = result.timeCurve[t]
            assertTrue("Time curve at $t was $v", v in 0f..1f)
        }
        println("Audio CAM (Analytical): Peak at t=25: ${result.timeCurve[25]}, Quiet at t=5: ${result.timeCurve[5]}")
    }

    @Test
    fun testTextAttentionSubwordAggregation() = runBlocking {
        val seqLen = 6
        val numHeads = 12
        val totalElements = numHeads * seqLen * seqLen

        val attnData = FloatArray(totalElements)
        val tokens = listOf("[CLS]", "deep", "##fa", "##ke", "news", "[SEP]")
        val text = "deepfake news"

        for (h in 0 until numHeads) {
            val headOffset = h * seqLen * seqLen
            attnData[headOffset + 0 * seqLen + 0] = 0.05f
            attnData[headOffset + 0 * seqLen + 1] = 0.20f
            attnData[headOffset + 0 * seqLen + 2] = 0.50f
            attnData[headOffset + 0 * seqLen + 3] = 0.15f
            attnData[headOffset + 0 * seqLen + 4] = 0.10f
            attnData[headOffset + 0 * seqLen + 5] = 0.00f
        }

        val result = TextAttentionPostProcessor.processAttention(
            attentionData = attnData,
            numHeads = numHeads,
            seqLen = seqLen,
            tokens = tokens,
            originalText = text
        )

        assertEquals(text, result.text)
        assertEquals(2, result.wordHighlights.size)

        val deepfakeWord = result.wordHighlights[0]
        assertEquals("deepfake", deepfakeWord.word)
        assertEquals(1.0f, deepfakeWord.score, 1e-4f)

        val newsWord = result.wordHighlights[1]
        assertEquals("news", newsWord.word)
        assertTrue(newsWord.score < deepfakeWord.score)

        println("Text XAI: Words: ${result.wordHighlights.map { "${it.word} (${it.score})" }}")
    }
}
