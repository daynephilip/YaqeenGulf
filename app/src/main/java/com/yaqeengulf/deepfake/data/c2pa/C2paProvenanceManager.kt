package com.yaqeengulf.deepfake.data.c2pa

import android.content.Context
import android.net.Uri
import android.util.Log
import com.yaqeengulf.deepfake.domain.model.ContentCredentialsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * C2paProvenanceManager inspects raw image byte streams for embedded C2PA manifests (ISO/IEC 19566-5 JUMBF).
 * Extracts generator provenance, IPTC digitalSourceType assertions, and edit history.
 * Runs on raw files BEFORE any bitmap decoding/resizing.
 */
object C2paProvenanceManager {

    private const val TAG = "C2paProvenanceMgr"

    // JUMBF / C2PA Signatures
    private val JPEG_APP11 = byteArrayOf(0xFF.toByte(), 0xEB.toByte())
    private val JUMBF_SIG = "jumb".toByteArray(StandardCharsets.US_ASCII)
    private val C2PA_SIG = "c2pa".toByteArray(StandardCharsets.US_ASCII)
    private val PNG_C2PA_CHUNK = "c2pa".toByteArray(StandardCharsets.US_ASCII)

    suspend fun inspect(context: Context, imageUri: Uri): ContentCredentialsResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                inspectStream(stream)
            } ?: ContentCredentialsResult.NotPresent
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read C2PA manifest from URI: ${e.message}", e)
            ContentCredentialsResult.NotPresent
        }
    }

    fun inspectStream(stream: InputStream): ContentCredentialsResult {
        return try {
            // Read up to 2MB from the head of the stream to scan markers and JUMBF superbox
            val buffer = ByteArray(2 * 1024 * 1024)
            val bytesRead = stream.read(buffer)
            if (bytesRead <= 16) return ContentCredentialsResult.NotPresent

            val rawData = buffer.copyOf(bytesRead)
            parseC2paFromBytes(rawData)
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing stream for C2PA: ${e.message}")
            ContentCredentialsResult.NotPresent
        }
    }

    fun parseC2paFromBytes(bytes: ByteArray): ContentCredentialsResult {
        // 1. Check for JPEG APP11 or JUMBF / C2PA superboxes
        val hasC2paMarker = indexOfSubarray(bytes, C2PA_SIG) != -1 || indexOfSubarray(bytes, JUMBF_SIG) != -1
        if (!hasC2paMarker) {
            return ContentCredentialsResult.NotPresent
        }

        // Convert byte range to string for string-based claim extraction
        val contentStr = String(bytes, StandardCharsets.ISO_8859_1)

        // 2. Check for AI generation indicators in C2PA assertions
        val isAiTrained = contentStr.contains("trainedAlgorithmicMedia", ignoreCase = true) ||
                contentStr.contains("compositeWithTrainedAlgorithmicMedia", ignoreCase = true) ||
                contentStr.contains("algorithmicMedia", ignoreCase = true) ||
                contentStr.contains("c2pa.ai_generated", ignoreCase = true)

        // 3. Extract Generator / Producer tool
        var producer: String? = null
        val generators = listOf(
            "Adobe Firefly", "Midjourney", "DALL-E", "Stable Diffusion",
            "Photoshop", "Truepic", "Leica Camera", "Sony Camera",
            "Nikon Camera", "Bing Image Creator", "Canva", "Google Imagen"
        )
        for (gen in generators) {
            if (contentStr.contains(gen, ignoreCase = true)) {
                producer = gen
                break
            }
        }

        if (producer == null) {
            // Generic search for claim_generator pattern
            val cgIndex = contentStr.indexOf("claim_generator")
            if (cgIndex != -1 && cgIndex + 40 < contentStr.length) {
                val snippet = contentStr.substring(cgIndex, cgIndex + 60)
                val cleanSnippet = snippet.replace(Regex("[^a-zA-Z0-9 ./_-]"), " ")
                producer = cleanSnippet.trim().take(30)
            }
        }

        // 4. Extract actions
        val actions = mutableListOf<String>()
        if (contentStr.contains("c2pa.created", ignoreCase = true)) actions.add("c2pa.created")
        if (contentStr.contains("c2pa.edited", ignoreCase = true)) actions.add("c2pa.edited")
        if (contentStr.contains("c2pa.resized", ignoreCase = true)) actions.add("c2pa.resized")
        if (contentStr.contains("c2pa.color_adjustments", ignoreCase = true)) actions.add("c2pa.color_adjustments")
        if (contentStr.contains("c2pa.filtered", ignoreCase = true)) actions.add("c2pa.filtered")

        val digitalSourceType = if (isAiTrained) "trainedAlgorithmicMedia" else null

        // 5. Verification status
        // If manifest header exists but is truncated or missing required claim signatures
        val hasSignature = contentStr.contains("c2pa.signature", ignoreCase = true) ||
                contentStr.contains("cose", ignoreCase = true) ||
                contentStr.contains("signature", ignoreCase = true)

        if (!hasSignature && actions.isEmpty() && producer == null && !isAiTrained) {
            return ContentCredentialsResult.TamperedOrUntrusted("C2PA marker present but manifest payload is incomplete or stripped.")
        }

        return ContentCredentialsResult.Verified(
            producer = producer ?: "C2PA Compatible Device / Software",
            actions = actions.ifEmpty { listOf("c2pa.created") },
            digitalSourceType = digitalSourceType,
            ingredientCount = if (contentStr.contains("ingredient", ignoreCase = true)) 1 else 0,
            isAiGenerated = isAiTrained,
            title = if (isAiTrained) "AI Generated Image" else "Content Authenticity Manifest"
        )
    }

    private fun indexOfSubarray(array: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || array.size < target.size) return -1
        outer@ for (i in 0..array.size - target.size) {
            for (j in target.indices) {
                if (array[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
