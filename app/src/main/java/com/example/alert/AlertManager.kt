package com.example.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppPreferences
import com.example.data.model.YapePayment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AlertManager(
    private val context: Context,
    private val preferences: AppPreferences
) : TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_PAYMENTS = "yape_payment_alerts"
        const val CHANNEL_SERVICE = "yape_service_status"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Real-time bus for in-app UI celebration and banners
    private val _livePaymentEvents = MutableSharedFlow<YapePayment>(extraBufferCapacity = 10)
    val livePaymentEvents: SharedFlow<YapePayment> = _livePaymentEvents.asSharedFlow()

    init {
        createNotificationChannels()
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "PE"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to standard Spanish
                tts?.setLanguage(Locale("es", "ES"))
            }
            isTtsReady = true
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val paymentChannel = NotificationChannel(
                CHANNEL_PAYMENTS,
                "Alertas de Pagos Yape",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones inmediatas cuando entra un nuevo pago por Yape"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Estado del Monitor Yape",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el servicio activo en segundo plano"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(paymentChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Triggers the full alert sequence: Sound, Vibration, Voice, Heads-up Notification, and UI event.
     */
    fun triggerImmediateAlert(payment: YapePayment) {
        // 1. Emit to in-app observers
        _livePaymentEvents.tryEmit(payment)

        // 2. Play Sound
        if (preferences.isSoundEnabled()) {
            playAlertSound()
        }

        // 3. Vibrate device
        if (preferences.isVibrationEnabled()) {
            triggerVibration()
        }

        // 4. Voice announcement (TTS)
        if (preferences.isTtsEnabled()) {
            speakPaymentAlert(payment)
        }

        // 5. Heads-up Notification
        if (preferences.isHeadsUpEnabled()) {
            showHeadsUpNotification(payment)
        }
    }

    private fun playAlertSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 350)
        } catch (_: Exception) {
            try {
                val ringtone = RingtoneManager.getRingtone(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                )
                ringtone?.play()
            } catch (_: Exception) {}
        }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1)
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 150, 100, 250), -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun speakPaymentAlert(payment: YapePayment) {
        if (!isTtsReady || tts == null) return
        val amountInt = payment.amount.toInt()
        val cents = ((payment.amount - amountInt) * 100).toInt()
        val amountSpeech = if (cents > 0) {
            "$amountInt soles con $cents céntimos"
        } else {
            "$amountInt soles"
        }

        val speechText = "¡Nuevo Yape! ${payment.sender} te pagó $amountSpeech"
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "yape_${payment.id}")
    }

    private fun showHeadsUpNotification(payment: YapePayment) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            payment.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = String.format(Locale.US, "S/ %.2f", payment.amount)
        val contentTitle = "¡Pago Yape Recibido! $formattedAmount"
        val contentText = "${payment.sender} te envió un pago."

        val builder = NotificationCompat.Builder(context, CHANNEL_PAYMENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Monto: $formattedAmount\nRemitente: ${payment.sender}\n${payment.rawBody}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 300))

        notificationManager.notify(payment.id.toInt().takeIf { it != 0 } ?: 1001, builder.build())
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
