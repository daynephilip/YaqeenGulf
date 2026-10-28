package com.yaqeengulf.deepfake.inference.preprocessor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PreprocessorUnitTest {

    @Test
    fun testWordPieceTokenizerEnglish() {
        val vocab = mapOf(
            "[PAD]" to 0,
            "[UNK]" to 100,
            "[CLS]" to 101,
            "[SEP]" to 102,
            "hello" to 7592,
            "world" to 2088,
            "," to 1010,
            "deep" to 2784,
            "##fa" to 7011,
            "##ke" to 3489,
            "test" to 3231,
            "!" to 999
        )
        val tokenizer = WordPieceTokenizer(vocab, isLowerCase = true)
        val result = tokenizer.tokenize("Hello world, deepfake test!")

        assertEquals(10, result.inputIds.size)
        assertEquals(101L, result.inputIds[0]) // [CLS]
        assertEquals(7592L, result.inputIds[1]) // hello
        assertEquals(2088L, result.inputIds[2]) // world
        assertEquals(1010L, result.inputIds[3]) // ,
        assertEquals(2784L, result.inputIds[4]) // deep
        assertEquals(7011L, result.inputIds[5]) // ##fa
        assertEquals(3489L, result.inputIds[6]) // ##ke
        assertEquals(3231L, result.inputIds[7]) // test
        assertEquals(999L, result.inputIds[8]) // !
        assertEquals(102L, result.inputIds[9]) // [SEP]
    }

    @Test
    fun testWordPieceTokenizerArabic() {
        val vocab = mapOf(
            "[PAD]" to 0,
            "[UNK]" to 1,
            "[CLS]" to 2,
            "[SEP]" to 3,
            "[MASK]" to 4,
            "مرحبا" to 24570,
            "بالعالم" to 19495,
            "اختبار" to 7639
        )
        val tokenizer = WordPieceTokenizer(
            vocab,
            unkToken = "[UNK]",
            unkTokenId = 1,
            padTokenId = 0,
            clsTokenId = 2,
            sepTokenId = 3,
            isLowerCase = false
        )
        val result = tokenizer.tokenize("مرحبا بالعالم اختبار")

        assertEquals(5, result.inputIds.size)
        assertEquals(2L, result.inputIds[0]) // [CLS]
        assertEquals(24570L, result.inputIds[1]) // مرحبا
        assertEquals(19495L, result.inputIds[2]) // بالعالم
        assertEquals(7639L, result.inputIds[3]) // اختبار
        assertEquals(3L, result.inputIds[4]) // [SEP]
    }

    @Test
    fun testAudioMelPreprocessorDimensionsAndNorm() {
        val preprocessor = AudioMelPreprocessor()
        // 4.0s @ 16kHz audio sample array (64,000 floats)
        val audioSamples = FloatArray(64000) { i ->
            kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * i / 16000.0).toFloat()
        }

        val fb = preprocessor.extractMelSpectrogram(audioSamples)
        assertEquals(80 * 401, fb.remaining())

        // Check per-chunk zero-mean and approx unit-std
        var sum = 0.0
        var sqSum = 0.0
        val count = fb.remaining()
        while (fb.hasRemaining()) {
            val v = fb.get().toDouble()
            sum += v
            sqSum += v * v
        }
        val mean = sum / count
        val std = kotlin.math.sqrt((sqSum / count) - (mean * mean))

        assertTrue("Mean should be ~ 0 (was $mean)", abs(mean) < 1e-4)
        assertTrue("Std should be ~ 1 (was $std)", abs(std - 1.0) < 0.05)
    }
}
