package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@JsonClass(generateAdapter = true)
data class WebhookPayload(
    @Json(name = "event")
    val event: String = "yape_payment_received",
    @Json(name = "payment_id")
    val paymentId: Long,
    @Json(name = "sender")
    val sender: String,
    @Json(name = "amount")
    val amount: Double,
    @Json(name = "currency")
    val currency: String = "PEN",
    @Json(name = "timestamp")
    val timestamp: Long,
    @Json(name = "datetime_iso")
    val datetimeIso: String,
    @Json(name = "raw_title")
    val rawTitle: String,
    @Json(name = "raw_message")
    val rawMessage: String,
    @Json(name = "app_source")
    val appSource: String,
    @Json(name = "device_name")
    val deviceName: String
) {
    companion object {
        fun fromPayment(payment: YapePayment, deviceName: String): WebhookPayload {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val isoDate = sdf.format(Date(payment.timestamp))

            return WebhookPayload(
                event = "yape_payment_received",
                paymentId = payment.id,
                sender = payment.sender,
                amount = payment.amount,
                currency = payment.currency,
                timestamp = payment.timestamp,
                datetimeIso = isoDate,
                rawTitle = payment.rawTitle,
                rawMessage = payment.rawBody,
                appSource = payment.packageName.ifEmpty { "com.bcp.innovacxion.yapeapp" },
                deviceName = deviceName
            )
        }
    }
}
