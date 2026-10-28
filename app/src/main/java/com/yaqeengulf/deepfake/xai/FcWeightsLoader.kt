package com.yaqeengulf.deepfake.xai

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class EffnetFcWeights(
    val weight: Array<FloatArray>, // [2, 1536]
    val bias: FloatArray           // [2]
)

data class SwinFcWeights(
    val weight: Array<FloatArray>, // [2, 768]
    val bias: FloatArray           // [2]
)

data class AudioFcWeights(
    val layer1Weight: Array<FloatArray>, // [128, 256]
    val layer1Bias: FloatArray,          // [128]
    val layer2Weight: Array<FloatArray>, // [2, 128]
    val layer2Bias: FloatArray           // [2]
)

object FcWeightsLoader {

    fun loadEffnetWeights(context: Context): EffnetFcWeights {
        val jsonStr = readAssetString(context, "models/img_det_effnet_fc_weights.json")
        val obj = JSONObject(jsonStr)
        val wJson = obj.getJSONArray("weight")
        val bJson = obj.getJSONArray("bias")

        val numClasses = wJson.length()
        val channels = wJson.getJSONArray(0).length()
        val weight = Array(numClasses) { c ->
            val row = wJson.getJSONArray(c)
            FloatArray(channels) { i -> row.getDouble(i).toFloat() }
        }
        val bias = FloatArray(bJson.length()) { i -> bJson.getDouble(i).toFloat() }
        return EffnetFcWeights(weight, bias)
    }

    fun loadSwinWeights(context: Context): SwinFcWeights {
        val jsonStr = readAssetString(context, "models/img_det_swin_fc_weights.json")
        val obj = JSONObject(jsonStr)
        val wJson = obj.getJSONArray("weight")
        val bJson = obj.getJSONArray("bias")

        val numClasses = wJson.length()
        val channels = wJson.getJSONArray(0).length()
        val weight = Array(numClasses) { c ->
            val row = wJson.getJSONArray(c)
            FloatArray(channels) { i -> row.getDouble(i).toFloat() }
        }
        val bias = FloatArray(bJson.length()) { i -> bJson.getDouble(i).toFloat() }
        return SwinFcWeights(weight, bias)
    }

    fun loadAudioWeights(context: Context, isArabic: Boolean = true): AudioFcWeights {
        val fileName = "models/voice_ar_fc_weights.json"
        val jsonStr = readAssetString(context, fileName)
        val obj = JSONObject(jsonStr)

        val w1Json = obj.getJSONArray("layer1_weight")
        val b1Json = obj.getJSONArray("layer1_bias")
        val w2Json = obj.getJSONArray("layer2_weight")
        val b2Json = obj.getJSONArray("layer2_bias")

        val l1W = Array(w1Json.length()) { r ->
            val row = w1Json.getJSONArray(r)
            FloatArray(row.length()) { c -> row.getDouble(c).toFloat() }
        }
        val l1B = FloatArray(b1Json.length()) { i -> b1Json.getDouble(i).toFloat() }

        val l2W = Array(w2Json.length()) { r ->
            val row = w2Json.getJSONArray(r)
            FloatArray(row.length()) { c -> row.getDouble(c).toFloat() }
        }
        val l2B = FloatArray(b2Json.length()) { i -> b2Json.getDouble(i).toFloat() }

        return AudioFcWeights(l1W, l1B, l2W, l2B)
    }

    private fun readAssetString(context: Context, path: String): String {
        return context.assets.open(path).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        }
    }
}
