package com.yaqeengulf.deepfake.inference.preprocessor

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Preprocessor for Image models (EfficientNet-B3 and Swin-T).
 * Resizes bitmap to 224x224, converts RGB to [0.0, 1.0], normalizes with ImageNet mean and std:
 * mean = [0.485, 0.456, 0.406]
 * std  = [0.229, 0.224, 0.225]
 * Returns FloatBuffer in NCHW format: [1, 3, 224, 224]
 */
object ImagePreprocessor {
    const val TARGET_WIDTH = 224
    const val TARGET_HEIGHT = 224

    private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun preprocess(bitmap: Bitmap): FloatBuffer {
        // Match training eval_tf: transforms.Resize(256) -> transforms.CenterCrop(224)
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height

        // 1. Resize smaller side to 256 while preserving aspect ratio
        val resizeScale = if (srcWidth < srcHeight) {
            256f / srcWidth
        } else {
            256f / srcHeight
        }
        val targetScaleW = (srcWidth * resizeScale).toInt().coerceAtLeast(TARGET_WIDTH)
        val targetScaleH = (srcHeight * resizeScale).toInt().coerceAtLeast(TARGET_HEIGHT)

        val scaled = Bitmap.createScaledBitmap(bitmap, targetScaleW, targetScaleH, true)

        // 2. Center crop 224x224
        val cropX = ((targetScaleW - TARGET_WIDTH) / 2).coerceAtLeast(0)
        val cropY = ((targetScaleH - TARGET_HEIGHT) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(scaled, cropX, cropY, TARGET_WIDTH, TARGET_HEIGHT)
        if (scaled != bitmap && scaled != cropped) {
            scaled.recycle()
        }

        val pixels = IntArray(TARGET_WIDTH * TARGET_HEIGHT)
        cropped.getPixels(pixels, 0, TARGET_WIDTH, 0, 0, TARGET_WIDTH, TARGET_HEIGHT)
        if (cropped != bitmap) {
            cropped.recycle()
        }

        val totalElements = 1 * 3 * TARGET_WIDTH * TARGET_HEIGHT
        val byteBuffer = ByteBuffer.allocateDirect(totalElements * 4).order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()

        val channelStride = TARGET_WIDTH * TARGET_HEIGHT

        // Efficient planar NCHW layout write
        val rArray = FloatArray(channelStride)
        val gArray = FloatArray(channelStride)
        val bArray = FloatArray(channelStride)

        for (i in 0 until channelStride) {
            val color = pixels[i]
            val r = ((color shr 16) and 0xFF) / 255.0f
            val g = ((color shr 8) and 0xFF) / 255.0f
            val b = (color and 0xFF) / 255.0f

            rArray[i] = (r - MEAN[0]) / STD[0]
            gArray[i] = (g - MEAN[1]) / STD[1]
            bArray[i] = (b - MEAN[2]) / STD[2]
        }

        floatBuffer.put(rArray)
        floatBuffer.put(gArray)
        floatBuffer.put(bArray)
        floatBuffer.rewind()

        return floatBuffer
    }
}
