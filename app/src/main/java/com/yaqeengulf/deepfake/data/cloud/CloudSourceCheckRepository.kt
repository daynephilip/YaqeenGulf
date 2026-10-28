package com.yaqeengulf.deepfake.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class CloudSourceCheckRepository(
    private var baseUrl: String = "https://cloudproxy-gold.vercel.app/api/check",
    private var appSecret: String = "yaqeen-default-app-secret-2026"
) {

    fun setBaseUrl(url: String) {
        this.baseUrl = url
    }

    fun setAppSecret(secret: String) {
        this.appSecret = secret
    }

    suspend fun checkUrl(
        targetUrl: String,
        provider: String,
        headlineHint: String? = null
    ): Result<CloudCheckResult> = withContext(Dispatchers.IO) {
        try {
            val url = URL(baseUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30000 // 30 seconds connect timeout
                readTimeout = 35000    // 35 seconds read timeout for slow news sites
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Yaqeen-App-Key", appSecret)
            }

            val normalizedProvider = provider.lowercase()
            val (resolvedProvider, resolvedModel) = when {
                normalizedProvider.contains("gemini") -> Pair("gemini", "gemini-2.5-flash")
                normalizedProvider.contains("qwen") -> Pair("groq", "qwen/qwen3.8-27b")
                else -> Pair("groq", "openai/gpt-oss-120b")
            }

            val payload = JSONObject().apply {
                put("url", targetUrl)
                put("provider", resolvedProvider)
                put("model", resolvedModel)
                if (!headlineHint.isNullOrBlank()) {
                    put("headline_hint", headlineHint.take(300))
                }
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val responseStr = reader.use { it.readText() }
                val json = JSONObject(responseStr)

                val result = CloudCheckResult(
                    verdict = json.optString("verdict", "uncertain"),
                    confidence = json.optDouble("confidence", 0.5).toFloat(),
                    reasoning = json.optString("reasoning", "Source check completed."),
                    providerUsed = json.optString("provider_used", provider),
                    url = targetUrl,
                    headline = json.optString("headline", headlineHint ?: ""),
                    author = json.optString("author", ""),
                    publisher = json.optString("publisher", ""),
                    trustVerdict = json.optString("trust_verdict", "")
                )
                Result.success(result)
            } else {
                val errorStream = connection.errorStream
                val errorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else ""

                val errorMsg = try {
                    JSONObject(errorBody).optString("error", "HTTP $responseCode")
                } catch (e: Exception) {
                    "HTTP $responseCode: $errorBody"
                }

                // If error contains truncated raw JSON from LLM (e.g. Arabic token limit reached on older backend)
                if (errorMsg.contains("Failed to parse JSON response") && errorMsg.contains("verdict")) {
                    val recovered = tryRecoverFromRawText(errorMsg, targetUrl, provider, headlineHint)
                    if (recovered != null) {
                        return@withContext Result.success(recovered)
                    }
                }

                val mappedMessage = when {
                    responseCode == 401 -> "Cloud service authentication failed (invalid app secret)."
                    errorMsg.isNotBlank() && !errorMsg.startsWith("HTTP ") -> errorMsg
                    responseCode == 429 -> "Rate limit reached for $provider. Please wait or switch model in Settings."
                    responseCode == 503 -> "Service capacity reached or provider temporarily unavailable."
                    responseCode == 400 -> "Invalid URL: $errorMsg"
                    else -> "Cloud Source-Check error ($responseCode): $errorMsg"
                }

                Result.failure(Exception(mappedMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Request timed out. The server took too long to respond."))
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Cannot reach Cloud Source-Check server. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception("Connection error: ${e.message}"))
        }
    }

    private fun tryRecoverFromRawText(
        errorText: String,
        targetUrl: String,
        provider: String,
        headlineHint: String?
    ): CloudCheckResult? {
        return try {
            val verdictRegex = Regex("\"verdict\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
            val confRegex = Regex("\"confidence\"\\s*:\\s*([0-9.]+)")
            val reasoningRegex = Regex("\"reasoning\"\\s*:\\s*\"([^\"]*)", RegexOption.IGNORE_CASE)
            val headlineRegex = Regex("\"headline\"\\s*:\\s*\"([^\"]*)", RegexOption.IGNORE_CASE)
            val authorRegex = Regex("\"author\"\\s*:\\s*\"([^\"]*)", RegexOption.IGNORE_CASE)
            val publisherRegex = Regex("\"publisher\"\\s*:\\s*\"([^\"]*)", RegexOption.IGNORE_CASE)
            val trustRegex = Regex("\"trust_verdict\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)

            val verdict = verdictRegex.find(errorText)?.groupValues?.get(1)?.lowercase() ?: return null
            val conf = confRegex.find(errorText)?.groupValues?.get(1)?.toFloatOrNull() ?: 0.8f
            val rawReasoning = reasoningRegex.find(errorText)?.groupValues?.get(1)?.trim()
            val reasoning = if (!rawReasoning.isNullOrBlank()) {
                rawReasoning.replace("\\n", " ").replace("\\\"", "\"").trim()
            } else {
                "Source and headline verification completed."
            }
            val headline = headlineRegex.find(errorText)?.groupValues?.get(1)?.trim()
                ?.ifBlank { headlineHint ?: "" } ?: (headlineHint ?: "")
            val author = authorRegex.find(errorText)?.groupValues?.get(1)?.trim() ?: ""
            val publisher = publisherRegex.find(errorText)?.groupValues?.get(1)?.trim() ?: ""
            val trustVerdict = trustRegex.find(errorText)?.groupValues?.get(1) ?: when (verdict) {
                "reliable" -> "TRUSTED"
                "unreliable" -> "SUSPICIOUS"
                else -> "UNVERIFIED"
            }

            CloudCheckResult(
                verdict = verdict,
                confidence = conf,
                reasoning = reasoning,
                providerUsed = provider,
                url = targetUrl,
                headline = headline,
                author = author,
                publisher = publisher,
                trustVerdict = trustVerdict
            )
        } catch (e: Exception) {
            null
        }
    }
}
