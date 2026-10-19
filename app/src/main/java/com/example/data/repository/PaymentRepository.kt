package com.example.data.repository

import android.os.Build
import com.example.alert.AlertManager
import com.example.data.AppPreferences
import com.example.data.db.PaymentDao
import com.example.data.model.SyncStatus
import com.example.data.model.WebhookPayload
import com.example.data.model.YapePayment
import com.example.network.FastApiWebhookClient
import com.example.network.WebhookResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val webhookClient: FastApiWebhookClient,
    private val preferences: AppPreferences,
    private val alertManager: AlertManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val allPayments: Flow<List<YapePayment>> = paymentDao.getAllPayments()

    fun getTodayPayments(): Flow<List<YapePayment>> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return paymentDao.getPaymentsSince(cal.timeInMillis)
    }

    suspend fun isPaymentAlreadySaved(payment: YapePayment): Boolean {
        if (payment.rawBody.isNotBlank()) {
            val countByBody = paymentDao.countWithRawBody(payment.rawBody)
            if (countByBody > 0) return true
        }
        val countSimilar = paymentDao.countSimilar(payment.sender, payment.amount, payment.timestamp)
        return countSimilar > 0
    }

    /**
     * Called when a new Yape notification is detected or simulated.
     * Inserts into DB, triggers immediate sound/vibration/voice alert, and optionally transmits to FastAPI if enabled.
     */
    suspend fun processIncomingPayment(payment: YapePayment, isCatchUp: Boolean = false): Long {
        // 1. Determine initial status: if FastAPI sync is not enabled yet, set PENDING without failing
        val isFastApiEnabled = preferences.isFastApiSyncEnabled()
        val initialPayment = if (!isFastApiEnabled) {
            payment.copy(
                syncStatus = SyncStatus.PENDING,
                errorMessage = "Modo solo alertas locales (FastAPI no activado aún)"
            )
        } else {
            payment
        }

        val generatedId = paymentDao.insertPayment(initialPayment)
        val paymentWithId = initialPayment.copy(id = generatedId)

        // 2. Trigger immediate alerts (Sound, Vibrate, TTS, Heads-Up Notification)
        alertManager.triggerImmediateAlert(paymentWithId)

        // 3. Dispatch to FastAPI server only if user has enabled FastAPI sync
        if (isFastApiEnabled) {
            scope.launch {
                sendWebhookForPayment(paymentWithId)
            }
        }

        return generatedId
    }

    suspend fun retrySync(paymentId: Long): WebhookResult {
        val payment = paymentDao.getPaymentById(paymentId)
            ?: return WebhookResult(isSuccess = false, errorMessage = "Pago no encontrado")
        return sendWebhookForPayment(payment)
    }

    private suspend fun sendWebhookForPayment(payment: YapePayment): WebhookResult {
        val targetUrl = preferences.getWebhookUrl()
        val authToken = preferences.getAuthToken()
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        val payload = WebhookPayload.fromPayment(payment, deviceName)

        val result = webhookClient.sendPaymentWebhook(targetUrl, payload, authToken)

        // Update database with sync result
        val updatedPayment = if (result.isSuccess) {
            payment.copy(
                syncStatus = SyncStatus.SUCCESS,
                httpStatusCode = result.httpCode,
                responseBody = result.responseBody,
                errorMessage = null,
                attempts = payment.attempts + 1,
                syncedAt = System.currentTimeMillis()
            )
        } else {
            payment.copy(
                syncStatus = SyncStatus.FAILED,
                httpStatusCode = result.httpCode,
                responseBody = result.responseBody,
                errorMessage = result.errorMessage,
                attempts = payment.attempts + 1
            )
        }

        paymentDao.updatePayment(updatedPayment)
        return result
    }

    suspend fun testFastApiConnection(url: String, token: String): WebhookResult {
        return webhookClient.testConnection(url, token)
    }

    suspend fun deletePayment(id: Long) {
        paymentDao.deletePaymentById(id)
    }

    suspend fun deleteDuplicates() {
        paymentDao.deleteDuplicates()
    }

    suspend fun deleteTestPayments() {
        paymentDao.deleteTestPayments()
    }

    suspend fun clearHistory() {
        paymentDao.clearAllPayments()
    }
}
