package com.yaqeengulf.deepfake.c2pa

import com.yaqeengulf.deepfake.data.c2pa.C2paProvenanceManager
import com.yaqeengulf.deepfake.domain.model.ContentCredentialsResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class C2paProvenanceUnitTest {

    @Test
    fun testAiGeneratedC2paManifestDetected() {
        val payload = ("\u0000\u0000\u0000\u0018jumbc2pa\u0000\u0000" +
                "claim_generator:Adobe Firefly v2\u0000" +
                "digitalSourceType:trainedAlgorithmicMedia\u0000" +
                "c2pa.actions:[c2pa.created,c2pa.color_adjustments]\u0000" +
                "c2pa.signature:valid_cose_key").toByteArray(StandardCharsets.ISO_8859_1)

        val result = C2paProvenanceManager.parseC2paFromBytes(payload)
        assertTrue(result is ContentCredentialsResult.Verified)
        val verified = result as ContentCredentialsResult.Verified
        assertTrue(verified.isAiGenerated)
        assertEquals("Adobe Firefly", verified.producer)
        assertEquals("trainedAlgorithmicMedia", verified.digitalSourceType)
        assertTrue(verified.actions.contains("c2pa.created"))
    }

    @Test
    fun testAuthenticCameraC2paManifestDetected() {
        val payload = ("\u0000\u0000\u0000\u0018jumbc2pa\u0000\u0000" +
                "claim_generator:Leica Camera M11-P\u0000" +
                "c2pa.actions:[c2pa.created]\u0000" +
                "c2pa.signature:cose_hardware_cert").toByteArray(StandardCharsets.ISO_8859_1)

        val result = C2paProvenanceManager.parseC2paFromBytes(payload)
        assertTrue(result is ContentCredentialsResult.Verified)
        val verified = result as ContentCredentialsResult.Verified
        assertEquals(false, verified.isAiGenerated)
        assertEquals("Leica Camera", verified.producer)
    }

    @Test
    fun testIncompleteOrTamperedC2paDetected() {
        val payload = "\u0000\u0000\u0000\u0018jumbc2pa\u0000\u0000corrupted_payload_without_any_claims".toByteArray(StandardCharsets.ISO_8859_1)

        val result = C2paProvenanceManager.parseC2paFromBytes(payload)
        assertTrue(result is ContentCredentialsResult.TamperedOrUntrusted)
    }

    @Test
    fun testUnassertedImageReturnsNotPresent() {
        val payload = "JFIF\u0000\u0001\u0001\u0001\u0000H\u0000H\u0000\u0000standard_jpeg_raw_bytes".toByteArray(StandardCharsets.ISO_8859_1)

        val result = C2paProvenanceManager.parseC2paFromBytes(payload)
        assertEquals(ContentCredentialsResult.NotPresent, result)
    }
}
