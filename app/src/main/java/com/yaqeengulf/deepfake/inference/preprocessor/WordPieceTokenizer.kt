package com.yaqeengulf.deepfake.inference.preprocessor

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Pure Kotlin WordPiece Tokenizer for BERT (uncased) and AraBERT.
 * Operates on vocab_en.txt and vocab_ar.txt loaded from app assets.
 */
class WordPieceTokenizer(
    private val vocabMap: Map<String, Int>,
    val unkToken: String = "[UNK]",
    val unkTokenId: Int = vocabMap["[UNK]"] ?: 100,
    val padTokenId: Int = vocabMap["[PAD]"] ?: 0,
    val clsTokenId: Int = vocabMap["[CLS]"] ?: 101,
    val sepTokenId: Int = vocabMap["[SEP]"] ?: 102,
    private val isLowerCase: Boolean = false
) {
    data class TokenizedInput(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokens: List<String>
    )

    fun tokenize(text: String, maxSeqLen: Int = 128): TokenizedInput {
        val processed = if (isLowerCase) text.lowercase() else text
        val basicTokens = whitespaceAndPunctuationTokenize(processed)
        val subTokens = mutableListOf<String>()

        for (token in basicTokens) {
            val pieces = wordPieceTokenize(token)
            subTokens.addAll(pieces)
        }

        // Truncate to leave room for [CLS] and [SEP]
        val maxTokens = maxSeqLen - 2
        val truncated = if (subTokens.size > maxTokens) {
            subTokens.subList(0, maxTokens)
        } else {
            subTokens
        }

        val allTokens = mutableListOf<String>()
        allTokens.add("[CLS]")
        allTokens.addAll(truncated)
        allTokens.add("[SEP]")

        val ids = LongArray(allTokens.size)
        val mask = LongArray(allTokens.size)

        for (i in allTokens.indices) {
            val tok = allTokens[i]
            val id = vocabMap[tok] ?: unkTokenId
            ids[i] = id.toLong()
            mask[i] = 1L
        }

        return TokenizedInput(ids, mask, allTokens)
    }

    private fun wordPieceTokenize(token: String): List<String> {
        if (token.isEmpty()) return emptyList()
        val subTokens = mutableListOf<String>()
        var start = 0
        val n = token.length

        while (start < n) {
            var end = n
            var curSubStr: String? = null
            while (start < end) {
                val subStr = if (start == 0) {
                    token.substring(start, end)
                } else {
                    "##" + token.substring(start, end)
                }
                if (vocabMap.containsKey(subStr)) {
                    curSubStr = subStr
                    break
                }
                end--
            }

            if (curSubStr == null) {
                return listOf(unkToken)
            }
            subTokens.add(curSubStr)
            start = end
        }

        return subTokens
    }

    private fun whitespaceAndPunctuationTokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()

        for (ch in text) {
            if (Character.isWhitespace(ch)) {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.clear()
                }
            } else if (isPunctuation(ch)) {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.clear()
                }
                tokens.add(ch.toString())
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        return tokens
    }

    private fun isPunctuation(ch: Char): Boolean {
        val type = Character.getType(ch)
        return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
                type == Character.DASH_PUNCTUATION.toInt() ||
                type == Character.START_PUNCTUATION.toInt() ||
                type == Character.END_PUNCTUATION.toInt() ||
                type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
                type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
                type == Character.OTHER_PUNCTUATION.toInt() ||
                (ch in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~")
    }

    companion object {
        fun fromAsset(context: Context, assetPath: String, isLowerCase: Boolean): WordPieceTokenizer {
            val vocab = HashMap<String, Int>(65000)
            context.assets.open(assetPath).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                    var idx = 0
                    for (line in lines) {
                        val token = line.trim()
                        if (token.isNotEmpty()) {
                            vocab[token] = idx
                        }
                        idx++
                    }
                }
            }
            return WordPieceTokenizer(vocab, isLowerCase = isLowerCase)
        }
    }
}
