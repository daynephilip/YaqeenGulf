package com.yaqeengulf.deepfake.data.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudCheckResult(
    val verdict: String,
    val confidence: Float,
    val reasoning: String,
    @SerialName("provider_used")
    val providerUsed: String = "gemini",
    val url: String = "",
    val headline: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val trustVerdict: String? = null
)
