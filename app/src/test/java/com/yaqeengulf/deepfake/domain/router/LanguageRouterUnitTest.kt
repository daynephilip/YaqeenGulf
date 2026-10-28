package com.yaqeengulf.deepfake.domain.router

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageRouterUnitTest {

    @Test
    fun testPureArabicText() {
        val pureArabic = "هذا مقطع صوتي تم توليده باستخدام الذكاء الاصطناعي"
        assertEquals(ScriptType.ARABIC, LanguageRouter.detectScript(pureArabic))
        assertTrue(LanguageRouter.isArabic(pureArabic))
    }

    @Test
    fun testPureEnglishText() {
        val pureEnglish = "Breaking news: this is an official statement regarding digital safety."
        assertEquals(ScriptType.ENGLISH, LanguageRouter.detectScript(pureEnglish))
        assertFalse(LanguageRouter.isArabic(pureEnglish))
    }

    @Test
    fun testGulfArabicWithBrandNamesMajorityArabic() {
        // Arabic text containing English brand names like "WhatsApp" and "AI"
        val mixedArabic = "تم تداول رسالة تحذيرية عبر WhatsApp بخصوص تقنيات Deepfake في الخليج العربي"
        assertEquals(ScriptType.ARABIC, LanguageRouter.detectScript(mixedArabic))
        assertTrue(LanguageRouter.isArabic(mixedArabic))
    }

    @Test
    fun testEnglishWithArabicLoanWordMajorityEnglish() {
        // English sentence containing a short Arabic name
        val mixedEnglish = "The conference in Dubai discussed yaqeen يقين verification standards."
        assertEquals(ScriptType.ENGLISH, LanguageRouter.detectScript(mixedEnglish))
        assertFalse(LanguageRouter.isArabic(mixedEnglish))
    }

    @Test
    fun testAmbiguousOrNoScriptDefaultsToArabic() {
        // Numbers and punctuation only -> default fallback to Arabic for MENA region
        val digitsOnly = "1234567890 !!! ??? ..."
        assertEquals(ScriptType.ARABIC, LanguageRouter.detectScript(digitsOnly))
        assertTrue(LanguageRouter.isArabic(digitsOnly))

        val emptyText = ""
        assertEquals(ScriptType.ARABIC, LanguageRouter.detectScript(emptyText))
        assertTrue(LanguageRouter.isArabic(emptyText))
    }
}
