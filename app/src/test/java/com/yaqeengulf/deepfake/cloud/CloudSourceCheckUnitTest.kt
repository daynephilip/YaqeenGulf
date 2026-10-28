package com.yaqeengulf.deepfake.cloud

import com.yaqeengulf.deepfake.data.cloud.CloudCheckResult
import com.yaqeengulf.deepfake.ui.dashboard.DashboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSourceCheckUnitTest {

    @Test
    fun testIsBareUrl_recognizesStandardWebUrls() {
        assertTrue(DashboardViewModel.isBareUrl("https://www.reuters.com/world/middle-east/gulf-economy-2026"))
        assertTrue(DashboardViewModel.isBareUrl("http://aljazeera.net/news/politics/12345"))
        assertTrue(DashboardViewModel.isBareUrl("https://bbc.com/news"))
        assertTrue(DashboardViewModel.isBareUrl("https://subdomain.domain.org/path?param=value&id=1#anchor"))
    }

    @Test
    fun testIsBareUrl_rejectsRegularArticleText() {
        assertFalse(DashboardViewModel.isBareUrl("Breaking news: Riyadh and Abu Dhabi announce a new bilateral agreement."))
        assertFalse(DashboardViewModel.isBareUrl("أعلنت وزارة الخارجية اليوم عن مبادرة استثمارية جديدة في قطاع الطاقة النظيفة."))
        assertFalse(DashboardViewModel.isBareUrl("Check out this story at https://example.com/article inside a sentence."))
        assertFalse(DashboardViewModel.isBareUrl(""))
        assertFalse(DashboardViewModel.isBareUrl("   "))
    }

    @Test
    fun testCloudCheckResult_propertiesAndDefaultValues() {
        val result = CloudCheckResult(
            verdict = "reliable",
            confidence = 0.95f,
            reasoning = "Established regional news agency with verified editorial standards.",
            providerUsed = "gemini",
            url = "https://example.com/news"
        )

        assertEquals("reliable", result.verdict)
        assertEquals(0.95f, result.confidence, 0.001f)
        assertEquals("Established regional news agency with verified editorial standards.", result.reasoning)
        assertEquals("gemini", result.providerUsed)
        assertEquals("https://example.com/news", result.url)
    }

    @Test
    fun testCloudCheckResult_groqProviderAssignment() {
        val result = CloudCheckResult(
            verdict = "unreliable",
            confidence = 0.88f,
            reasoning = "Source has frequent retractions and lack of editorial transparency.",
            providerUsed = "groq",
            url = "https://clickbait-tabloid.xyz/story"
        )

        assertEquals("unreliable", result.verdict)
        assertEquals("groq", result.providerUsed)
    }
}
