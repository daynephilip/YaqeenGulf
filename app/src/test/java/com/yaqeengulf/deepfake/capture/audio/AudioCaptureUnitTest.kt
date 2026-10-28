package com.yaqeengulf.deepfake.capture.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioCaptureUnitTest {

    @Test
    fun testPcm16ToNormalizedFloatConversion() {
        // Test short values: 0, 32767 (max pos), -32768 (min neg), 16384 (~0.5)
        val shorts = ShortArray(4)
        shorts[0] = 0
        shorts[1] = 32767
        shorts[2] = -32768
        shorts[3] = 16384

        val floats = AudioCaptureManager.convertPcm16ToNormalizedFloat(shorts, targetLength = 64000)

        assertEquals(64000, floats.size)
        assertEquals(0.0f, floats[0], 1e-5f)
        assertEquals(32767f / 32768f, floats[1], 1e-4f)
        assertEquals(-1.0f, floats[2], 1e-5f)
        assertEquals(0.5f, floats[3], 1e-4f)

        // Trailing padded elements should be 0.0f
        assertEquals(0.0f, floats[4], 1e-5f)
        assertEquals(0.0f, floats[63999], 1e-5f)
    }

    @Test
    fun testSyntheticWavParsing() {
        val sampleRate = 16000
        val numSamples = 1600 // 0.1s
        val header = createWavHeader(numSamples, sampleRate = sampleRate, channels = 1, bitsPerSample = 16)

        val pcmData = ByteArray(numSamples * 2)
        val sBuf = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        for (i in 0 until numSamples) {
            sBuf.put((i % 1000).toShort())
        }

        val wavBytes = header + pcmData
        val inputStream = wavBytes.inputStream()

        val parsedFloats = AudioCaptureManager.decodeWavStream(inputStream)
        assertEquals(64000, parsedFloats.size)
        assertEquals(0f, parsedFloats[0], 1e-5f)
        assertEquals(100f / 32768f, parsedFloats[100], 1e-4f)
    }

    private fun createWavHeader(numSamples: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalDataLen = numSamples * channels * (bitsPerSample / 8)
        val totalAudioLen = totalDataLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalAudioLen and 0xff).toByte()
        header[5] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[6] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[7] = ((totalAudioLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalDataLen and 0xff).toByte()
        header[41] = ((totalDataLen shr 8) and 0xff).toByte()
        header[42] = ((totalDataLen shr 16) and 0xff).toByte()
        header[43] = ((totalDataLen shr 24) and 0xff).toByte()
        return header
    }
}
