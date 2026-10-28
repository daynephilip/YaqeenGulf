package com.yaqeengulf.deepfake.capture.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audio Capture Manager for Yaqeen Gulf.
 * Supports:
 * 1. Live microphone recording via AudioRecord (16000 Hz, mono 16-bit PCM).
 * 2. Audio file decoding (.wav or raw PCM) from content URI into 4.0s (64,000 float) chunk.
 */
object AudioCaptureManager {

    const val SAMPLE_RATE = 16000
    const val TARGET_SAMPLES = 64000 // 4.0 seconds @ 16kHz
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    /**
     * Records audio from the microphone for [durationSeconds] seconds.
     * Requires RECORD_AUDIO permission.
     * Returns a FloatArray normalized to [-1.0, 1.0], padded or truncated to 64,000 samples.
     */
    suspend fun recordAudio(
        durationSeconds: Int = 4,
        onProgress: ((Float) -> Unit)? = null
    ): Result<FloatArray> = withContext(Dispatchers.IO) {
        runCatching {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = maxOf(minBufferSize, 4096)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                throw IllegalStateException("AudioRecord failed to initialize. Ensure RECORD_AUDIO permission is granted.")
            }

            val totalExpectedSamples = durationSeconds * SAMPLE_RATE
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
                        onProgress?.invoke(samplesReadTotal.toFloat() / totalExpectedSamples.toFloat())
                    } else if (readCount < 0) {
                        throw IllegalStateException("Error reading audio data: $readCount")
                    }
                }
            } finally {
                try {
                    audioRecord.stop()
                } catch (_: Exception) {}
                audioRecord.release()
            }

            val shortArray = ShortArray(recordedShorts.size) { recordedShorts[it] }
            convertPcm16ToNormalizedFloat(shortArray, TARGET_SAMPLES)
        }
    }

    /**
     * Reads audio from a .wav or raw audio URI, extracting up to 4.0 seconds @ 16kHz.
     */
    suspend fun loadAudioFromUri(context: Context, uri: Uri): Result<FloatArray> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                decodeWavStream(inputStream)
            } ?: throw IllegalStateException("Cannot open input stream for audio Uri: $uri")
        }
    }

    /**
     * Parses a WAV stream and extracts normalized float PCM samples.
     * If the WAV header is absent or custom, falls back to parsing as raw 16-bit PCM.
     */
    fun decodeWavStream(inputStream: InputStream): FloatArray {
        val bytes = inputStream.readBytes()
        if (bytes.size < 44) {
            throw IllegalArgumentException("Audio file is too short to be valid audio data.")
        }

        // Check for 'RIFF' header and 'WAVE' format
        val isRiff = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
        val isWave = bytes[8] == 'W'.code.toByte() && bytes[9] == 'A'.code.toByte() &&
                bytes[10] == 'V'.code.toByte() && bytes[11] == 'E'.code.toByte()

        var pcmDataOffset = 44
        var numChannels = 1
        var sampleRate = 16000
        var bitsPerSample = 16

        if (isRiff && isWave) {
            // Parse RIFF chunks to find 'fmt ' and 'data'
            var offset = 12
            while (offset + 8 <= bytes.size) {
                val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
                val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                if (chunkId == "fmt ") {
                    numChannels = ByteBuffer.wrap(bytes, offset + 10, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                    sampleRate = ByteBuffer.wrap(bytes, offset + 12, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    bitsPerSample = ByteBuffer.wrap(bytes, offset + 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                } else if (chunkId == "data") {
                    pcmDataOffset = offset + 8
                    break
                }
                offset += 8 + chunkSize
            }
        } else {
            // Assume raw PCM 16-bit mono 16kHz
            pcmDataOffset = 0
        }

        val pcmBytes = bytes.copyOfRange(pcmDataOffset, bytes.size)
        return convertBytesToFloatSamples(pcmBytes, numChannels, bitsPerSample, sampleRate)
    }

    /**
     * Converts raw PCM bytes into 16kHz mono float array padded/truncated to 64,000 samples.
     */
    fun convertBytesToFloatSamples(
        pcmBytes: ByteArray,
        numChannels: Int,
        bitsPerSample: Int,
        sourceSampleRate: Int
    ): FloatArray {
        val shorts = when (bitsPerSample) {
            16 -> {
                val shortCount = pcmBytes.size / 2
                val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                val rawShorts = ShortArray(shortCount)
                buffer.get(rawShorts)

                if (numChannels > 1) {
                    // Downmix to mono by averaging interleaved channels
                    val monoCount = shortCount / numChannels
                    ShortArray(monoCount) { i ->
                        var sum = 0
                        for (c in 0 until numChannels) {
                            sum += rawShorts[i * numChannels + c]
                        }
                        (sum / numChannels).toShort()
                    }
                } else {
                    rawShorts
                }
            }
            8 -> {
                // 8-bit unsigned PCM to 16-bit signed
                ShortArray(pcmBytes.size) { i ->
                    ((pcmBytes[i].toInt() and 0xFF) - 128 shl 8).toShort()
                }
            }
            else -> {
                // Fallback assume 16-bit
                val shortCount = pcmBytes.size / 2
                val rawShorts = ShortArray(shortCount)
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rawShorts)
                rawShorts
            }
        }

        // Resample if source sample rate differs from 16kHz
        val resampledShorts = if (sourceSampleRate != SAMPLE_RATE && sourceSampleRate > 0) {
            simpleLinearResample(shorts, sourceSampleRate, SAMPLE_RATE)
        } else {
            shorts
        }

        return convertPcm16ToNormalizedFloat(resampledShorts, TARGET_SAMPLES)
    }

    /**
     * Converts a 16-bit PCM ShortArray to a normalized FloatArray [-1.0, 1.0], padded/trimmed to targetLength.
     */
    fun convertPcm16ToNormalizedFloat(shorts: ShortArray, targetLength: Int = TARGET_SAMPLES): FloatArray {
        val result = FloatArray(targetLength)
        val copyLen = minOf(shorts.size, targetLength)
        for (i in 0 until copyLen) {
            result[i] = shorts[i] / 32768.0f
        }
        return result
    }

    private fun simpleLinearResample(input: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        if (fromRate == toRate || input.isEmpty()) return input
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outputLength = (input.size / ratio).toInt()
        val output = ShortArray(outputLength)

        for (i in 0 until outputLength) {
            val srcIndex = i * ratio
            val index0 = srcIndex.toInt()
            val index1 = minOf(index0 + 1, input.size - 1)
            val frac = (srcIndex - index0).toFloat()
            val sample = (1f - frac) * input[index0] + frac * input[index1]
            output[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
        }
        return output
    }
}
