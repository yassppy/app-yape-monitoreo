package com.example.network

import android.os.Build
import com.example.data.model.WebhookPayload
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class WebhookResult(
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val responseBody: String? = null,
    val errorMessage: String? = null,
    val latencyMs: Long = 0,
    val payloadJson: String = ""
)

class FastApiWebhookClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(WebhookPayload::class.java)

    /**
     * Sends the payment webhook to the specified FastAPI endpoint.
     */
    suspend fun sendPaymentWebhook(
        targetUrl: String,
        payload: WebhookPayload,
        authToken: String = ""
    ): WebhookResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val jsonPayload = try {
            payloadAdapter.toJson(payload)
        } catch (e: Exception) {
            return@withContext WebhookResult(
                isSuccess = false,
                errorMessage = "Error serializando JSON: ${e.message}"
            )
        }

        if (targetUrl.isBlank() || !targetUrl.startsWith("http")) {
            return@withContext WebhookResult(
                isSuccess = false,
                errorMessage = "URL de webhook inválida. Debe comenzar con http:// o https://",
                payloadJson = jsonPayload
            )
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .post(body)
            .header("Content-Type", "application/json")
            .header("User-Agent", "YapeMonitor-Android/${Build.MODEL}")

        if (authToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
            requestBuilder.header("X-API-Key", authToken)
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBodyString = response.body?.string()?.take(500) ?: ""

            if (response.isSuccessful) {
                WebhookResult(
                    isSuccess = true,
                    httpCode = response.code,
                    responseBody = responseBodyString,
                    latencyMs = latency,
                    payloadJson = jsonPayload
                )
            } else {
                WebhookResult(
                    isSuccess = false,
                    httpCode = response.code,
                    responseBody = responseBodyString,
                    errorMessage = "Servidor respondió con código HTTP ${response.code}",
                    latencyMs = latency,
                    payloadJson = jsonPayload
                )
            }
        } catch (e: IOException) {
            val latency = System.currentTimeMillis() - startTime
            WebhookResult(
                isSuccess = false,
                errorMessage = "Fallo de conexión de red: ${e.localizedMessage ?: e.javaClass.simpleName}",
                latencyMs = latency,
                payloadJson = jsonPayload
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            WebhookResult(
                isSuccess = false,
                errorMessage = "Error inesperado: ${e.message}",
                latencyMs = latency,
                payloadJson = jsonPayload
            )
        }
    }

    /**
     * Test connection directly to the FastAPI server with a ping payload.
     */
    suspend fun testConnection(targetUrl: String, authToken: String): WebhookResult {
        val testPayload = WebhookPayload(
            event = "test_ping",
            paymentId = 0L,
            sender = "PRUEBA DE CONEXIÓN",
            amount = 1.0,
            currency = "PEN",
            timestamp = System.currentTimeMillis(),
            datetimeIso = "2026-10-18T22:45:00Z",
            rawTitle = "Test de Conexión FastAPI",
            rawMessage = "Yape! PRUEBA DE CONEXIÓN te envió un pago por S/ 1.00",
            appSource = "com.bcp.innovacxion.yapeapp",
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
        return sendPaymentWebhook(targetUrl, testPayload, authToken)
    }
}
