package com.yaqeengulf.deepfake.xai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Text Attention Post-Processor for BERT-base and AraBERT.
 *
 * Takes the attention tensor from the final transformer layer [1, num_heads, seq_len, seq_len],
 * averages attention weights across all attention heads,
 * extracts the [CLS] attention row (which measures how much each token influenced the classification token),
 * and maps subword tokens back to words to create smooth, readable word-level highlight maps.
 */
object TextAttentionPostProcessor {

    suspend fun processAttention(
        attentionData: FloatArray, // [1, numHeads, seqLen, seqLen]
        numHeads: Int = 12,
        seqLen: Int,
        tokens: List<String>,
        originalText: String
    ): TextXaiResult = withContext(Dispatchers.Default) {
        val headStride = seqLen * seqLen

        // 1. Average across attention heads for row 0 ([CLS] token attention to all tokens)
        val clsAttn = FloatArray(seqLen)
        for (h in 0 until numHeads) {
            val offset = h * headStride
            for (j in 0 until seqLen) {
                clsAttn[j] += attentionData[offset + 0 * seqLen + j]
            }
        }
        for (j in 0 until seqLen) {
            clsAttn[j] /= numHeads.toFloat()
        }

        // 2. Normalize token scores between [0.0, 1.0] (excluding special tokens [CLS] and [SEP] from range calculation)
        var minVal = Float.MAX_VALUE
        var maxVal = -Float.MAX_VALUE
        for (i in 1 until (seqLen - 1)) {
            val v = clsAttn[i]
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }

        val range = maxVal - minVal
        val normTokenScores = FloatArray(seqLen)
        for (i in 0 until seqLen) {
            normTokenScores[i] = if (range > 1e-7f) {
                ((clsAttn[i] - minVal) / range).coerceIn(0f, 1f)
            } else {
                0f
            }
        }

        val rawPairs = mutableListOf<Pair<String, Float>>()
        for (i in 0 until minOf(tokens.size, seqLen)) {
            rawPairs.add(Pair(tokens[i], normTokenScores[i]))
        }

        // 3. Aggregate subwords into whole words
        val wordHighlights = aggregateSubwords(tokens, normTokenScores, originalText)

        TextXaiResult(
            text = originalText,
            wordHighlights = wordHighlights,
            rawTokenHighlights = rawPairs
        )
    }

    private fun aggregateSubwords(
        tokens: List<String>,
        scores: FloatArray,
        originalText: String
    ): List<WordHighlight> {
        val highlights = mutableListOf<WordHighlight>()
        var i = 0
        var searchIndex = 0

        while (i < tokens.size) {
            val tok = tokens[i]
            if (tok == "[CLS]" || tok == "[SEP]" || tok == "[PAD]") {
                i++
                continue
            }

            // Group consecutive subwords
            val groupTokens = mutableListOf<String>()
            val groupScores = mutableListOf<Float>()
            groupTokens.add(tok)
            groupScores.add(scores[i])

            var next = i + 1
            while (next < tokens.size && tokens[next].startsWith("##")) {
                groupTokens.add(tokens[next])
                groupScores.add(scores[next])
                next++
            }

            // Reconstruct word string
            val reconstructed = StringBuilder()
            for (t in groupTokens) {
                if (t.startsWith("##")) {
                    reconstructed.append(t.substring(2))
                } else {
                    reconstructed.append(t)
                }
            }
            val wordStr = reconstructed.toString()

            // Max-pooling score over subwords
            val wordScore = groupScores.maxOrNull() ?: 0f

            // Locate in original text
            val start = originalText.indexOf(wordStr, searchIndex, ignoreCase = true)
            if (start != -1) {
                val end = (start + wordStr.length).coerceAtMost(originalText.length)
                highlights.add(WordHighlight(wordStr, wordScore, start, end))
                searchIndex = end
            } else {
                // Fallback if punctuation or whitespace split differently
                val clampedStart = searchIndex.coerceIn(0, originalText.length)
                val clampedEnd = (searchIndex + wordStr.length).coerceIn(clampedStart, originalText.length)
                highlights.add(WordHighlight(wordStr, wordScore, clampedStart, clampedEnd))
                searchIndex = clampedEnd
            }

            i = next
        }

        return highlights
    }
}
