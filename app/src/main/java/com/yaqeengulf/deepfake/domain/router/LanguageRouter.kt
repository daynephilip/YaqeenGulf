package com.yaqeengulf.deepfake.domain.router

/**
 * Heuristic Unicode-script detector per Master Build Plan §6, §7, §10 and Directive 5.
 * Checks Arabic Unicode blocks vs Latin/English alphabet to automatically route Text
 * and Audio inputs to the proper model (AraBERT vs BERT-base, voice_ar vs voice_en).
 *
 * Arabic script blocks in Unicode standard:
 * - Arabic: \u0600 - \u06FF
 * - Arabic Supplement: \u0750 - \u077F
 * - Arabic Extended-A: \u08A0 - \u08FF
 * - Arabic Presentation Forms-A: \uFB50 - \uFDFF
 * - Arabic Presentation Forms-B: \uFE70 - \uFEFF
 *
 * Latin/English script blocks:
 * - Basic Latin / Latin-1 Supplement: 'a'..'z', 'A'..'Z'
 */
enum class ScriptType {
    ARABIC,
    ENGLISH
}

object LanguageRouter {

    /**
     * Determines whether the given text is primarily Arabic or English.
     * Implements a majority-script rule for mixed text.
     * Defaults to ARABIC if ambiguous or if no script characters are present (MENA region baseline).
     */
    fun detectScript(text: String): ScriptType {
        var arabicCount = 0
        var latinCount = 0

        for (char in text) {
            when {
                isArabicCharacter(char) -> arabicCount++
                isLatinCharacter(char) -> latinCount++
            }
        }

        return when {
            arabicCount > latinCount -> ScriptType.ARABIC
            latinCount > arabicCount -> ScriptType.ENGLISH
            // When counts are equal or text has no script characters (e.g. digits or symbols),
            // if there is at least one Arabic character, route to Arabic; otherwise default to Arabic as primary Gulf language
            arabicCount > 0 -> ScriptType.ARABIC
            latinCount > 0 -> ScriptType.ENGLISH
            else -> ScriptType.ARABIC
        }
    }

    /**
     * Helper boolean returning true if Arabic model should be chosen.
     */
    fun isArabic(text: String): Boolean {
        return detectScript(text) == ScriptType.ARABIC
    }

    private fun isArabicCharacter(c: Char): Boolean {
        val code = c.code
        return (code in 0x0600..0x06FF) || // Arabic
                (code in 0x0750..0x077F) || // Arabic Supplement
                (code in 0x08A0..0x08FF) || // Arabic Extended-A
                (code in 0xFB50..0xFDFF) || // Arabic Presentation Forms-A
                (code in 0xFE70..0xFEFF)    // Arabic Presentation Forms-B
    }

    private fun isLatinCharacter(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z')
    }
}
