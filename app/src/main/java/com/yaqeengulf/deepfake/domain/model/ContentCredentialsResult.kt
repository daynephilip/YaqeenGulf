package com.yaqeengulf.deepfake.domain.model

/**
 * C2PA Content Credentials provenance result sitting alongside on-device neural network verdicts.
 * Provides cryptographic origin verification (camera metadata, software tools, AI generators).
 */
sealed class ContentCredentialsResult {

    data class Verified(
        val producer: String?,
        val actions: List<String>,
        val digitalSourceType: String?,
        val ingredientCount: Int,
        val isAiGenerated: Boolean,
        val title: String? = null,
        val format: String = "C2PA / JUMBF (ISO 19566-5)"
    ) : ContentCredentialsResult()

    data class TamperedOrUntrusted(
        val reason: String
    ) : ContentCredentialsResult()

    object NotPresent : ContentCredentialsResult()
}
