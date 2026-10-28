package com.yaqeengulf.deepfake.capture.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.yaqeengulf.deepfake.capture.audio.AudioCaptureManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume

/**
 * MediaProjection Capture Manager for overlay mode per Master Build Plan §6, §7, §10:
 * - Screenshot capture via VirtualDisplay and ImageReader.
 * - Internal system audio capture via AudioPlaybackCaptureConfiguration (available natively since minSdk 29).
 */
class MediaProjectionCaptureManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Captures a single screenshot using the active MediaProjection token.
     */
    suspend fun captureScreenshot(projection: MediaProjection): Result<Bitmap> = withContext(Dispatchers.IO) {
        runCatching {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            suspendCancellableCoroutine<Bitmap> { continuation ->
                var virtualDisplay: VirtualDisplay? = null

                imageReader.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        try {
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * width

                            val bitmap = Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height,
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.copyPixelsFromBuffer(buffer)

                            val croppedBitmap = if (rowPadding != 0) {
                                Bitmap.createBitmap(bitmap, 0, 0, width, height)
                            } else {
                                bitmap
                            }

                            if (continuation.isActive) {
                                continuation.resume(croppedBitmap)
                            }
                        } catch (e: Exception) {
                            if (continuation.isActive) {
                                continuation.resumeWith(Result.failure(e))
                            }
                        } finally {
                            image.close()
                            imageReader.close()
                            virtualDisplay?.release()
                        }
                    }
                }, mainHandler)

                virtualDisplay = projection.createVirtualDisplay(
                    "YaqeenScreenshot",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    null
                )

                continuation.invokeOnCancellation {
                    imageReader.close()
                    virtualDisplay?.release()
                }
            }
        }
    }

    /**
     * Captures internal device audio (media playback from other apps) for [durationSeconds]
     * using Android's native AudioPlaybackCaptureConfiguration (available on Android 10 / API 29+).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun captureInternalAudio(
        projection: MediaProjection,
        durationSeconds: Int = 4
    ): Result<FloatArray> = withContext(Dispatchers.IO) {
        runCatching {
            val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val sampleRate = AudioCaptureManager.SAMPLE_RATE
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, 4096)

            val audioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                throw IllegalStateException("Failed to initialize AudioRecord for internal playback capture.")
            }

            val totalExpectedSamples = durationSeconds * sampleRate
            val shortBuffer = ShortArray(bufferSize / 2)
            val recordedShorts = ArrayList<Short>(totalExpectedSamples)

            try {
                audioRecord.startRecording()
                var samplesReadTotal = 0

                while (samplesReadTotal < totalExpectedSamples) {
                    val readCount = audioRecord.read(shortBuffer, 0, minOf(shortBuffer.size, totalExpectedSamples - samplesReadTotal))
                    if (readCount > 0) {
                        for (i in 0 until readCount) {
                            recordedShorts.add(shortBuffer[i])
                        }
                        samplesReadTotal += readCount
                    } else if (readCount < 0) {
                        throw IllegalStateException("Error reading internal audio: $readCount")
                    }
                }
            } finally {
                try {
                    audioRecord.stop()
                } catch (_: Exception) {}
                audioRecord.release()
            }

            val shortArray = ShortArray(recordedShorts.size) { recordedShorts[it] }
            AudioCaptureManager.convertPcm16ToNormalizedFloat(shortArray, AudioCaptureManager.TARGET_SAMPLES)
        }
    }

    companion object {
        private const val TAG = "MediaProjCaptureMgr"

        /**
         * Globally shared active projection token passed from MainActivity on permission grant.
         */
        @Volatile
        var activeProjection: MediaProjection? = null
    }
}
