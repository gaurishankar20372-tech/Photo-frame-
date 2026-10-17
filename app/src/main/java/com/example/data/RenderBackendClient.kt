package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.util.DslrProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

sealed class BackendStatus {
    object Disconnected : BackendStatus()
    object Connecting : BackendStatus()
    data class Connected(val latencyMs: Long, val serviceName: String) : BackendStatus()
    data class Error(val message: String) : BackendStatus()
}

data class RemoteEnhanceResult(
    val bitmap: Bitmap,
    val sharpnessBefore: Float,
    val sharpnessAfter: Float,
    val sharpnessImprovement: String,
    val profile: String
)

object RenderBackendClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Checks if the Render backend is alive, returns latency in milliseconds.
     */
    suspend fun checkHealth(baseUrl: String): Result<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val sanitizedUrl = baseUrl.trim().trimEnd('/')
        if (sanitizedUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Backend URL cannot be empty"))
        }

        val healthUrl = "$sanitizedUrl/api/health"
        val request = Request.Builder()
            .url(healthUrl)
            .get()
            .build()

        val startTime = System.currentTimeMillis()
        try {
            httpClient.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = try { JSONObject(bodyString) } catch (_: Exception) { JSONObject() }
                    val service = json.optString("service", "PHOTO FRAME Backend")
                    Result.success(Pair(elapsed, service))
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calls the Render backend to enhance the given bitmap using server-side OpenCV + CLAHE + DSLR grading.
     */
    suspend fun enhanceDslrRemote(
        baseUrl: String,
        bitmap: Bitmap,
        profile: DslrProfile,
        sharpness: Float,
        microContrast: Float,
        shadowRecovery: Float,
        vignette: Float,
        scaleFactor: Int = 1
    ): Result<RemoteEnhanceResult> = withContext(Dispatchers.IO) {
        val sanitizedUrl = baseUrl.trim().trimEnd('/')
        val endpoint = "$sanitizedUrl/api/enhance/dslr/json"

        // Encode source bitmap to Base64
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
        val imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val jsonPayload = JSONObject().apply {
            put("image_base64", imageBase64)
            put("profile", profile.name)
            put("sharpness", sharpness)
            put("micro_contrast", microContrast)
            put("shadow_recovery", shadowRecovery)
            put("vignette", vignette)
            put("scale_factor", scaleFactor)
            put("output_format", "JPEG")
        }

        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }

                val bodyStr = response.body?.string() ?: ""
                val resJson = JSONObject(bodyStr)

                val resultBase64 = resJson.getString("enhanced_image_base64")
                val decodedBytes = Base64.decode(resultBase64, Base64.DEFAULT)
                val outBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    ?: return@withContext Result.failure(Exception("Failed to decode returned image bitmap"))

                val metricsBefore = resJson.optJSONObject("metrics_before")
                val metricsAfter = resJson.optJSONObject("metrics_after")

                val sharpBefore = metricsBefore?.optDouble("sharpness_score", 0.0)?.toFloat() ?: 0f
                val sharpAfter = metricsAfter?.optDouble("sharpness_score", 0.0)?.toFloat() ?: 0f
                val improvement = resJson.optString("sharpness_improvement", "+0 pts")

                Result.success(
                    RemoteEnhanceResult(
                        bitmap = outBitmap,
                        sharpnessBefore = sharpBefore,
                        sharpnessAfter = sharpAfter,
                        sharpnessImprovement = improvement,
                        profile = resJson.optString("profile", profile.name)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
