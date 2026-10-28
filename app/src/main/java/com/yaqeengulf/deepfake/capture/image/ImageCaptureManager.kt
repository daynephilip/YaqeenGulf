package com.yaqeengulf.deepfake.capture.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Image Capture Manager for manual selection via photo picker / gallery Uri.
 * Handles decoding into memory-efficient RGB Bitmaps and standardizing format for the inference pipeline.
 */
object ImageCaptureManager {

    /**
     * Loads a Bitmap from a content URI.
     */
    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Result<Bitmap> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }.recoverCatching {
            // Secondary fallback via InputStream
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: throw IllegalStateException("Unable to open input stream for Uri: $uri")
        }
    }
}
