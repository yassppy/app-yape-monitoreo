package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("yape_monitor_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WEBHOOK_URL = "webhook_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_FOREGROUND_ENABLED = "foreground_enabled"
        private const val KEY_HEADS_UP_ENABLED = "heads_up_enabled"
        private const val KEY_AUTO_RETRY_ENABLED = "auto_retry_enabled"
        private const val KEY_FASTAPI_SYNC_ENABLED = "fastapi_sync_enabled"

        // Default local IP address or emulator host
        const val DEFAULT_WEBHOOK_URL = "http://192.168.1.50:8000/api/yape-webhook"
    }

    private val _webhookUrlFlow = MutableStateFlow(getWebhookUrl())
    val webhookUrlFlow: StateFlow<String> = _webhookUrlFlow.asStateFlow()

    fun getWebhookUrl(): String {
        return prefs.getString(KEY_WEBHOOK_URL, DEFAULT_WEBHOOK_URL) ?: DEFAULT_WEBHOOK_URL
    }

    fun setWebhookUrl(url: String) {
        val trimmed = url.trim()
        val normalized = if (trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "http://$trimmed"
        } else {
            trimmed
        }
        prefs.edit().putString(KEY_WEBHOOK_URL, normalized).commit()
        _webhookUrlFlow.value = normalized
    }

    fun getAuthToken(): String {
        return prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
    }

    fun setAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token.trim()).commit()
    }

    fun isFastApiSyncEnabled(): Boolean = prefs.getBoolean(KEY_FASTAPI_SYNC_ENABLED, false)
    fun setFastApiSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FASTAPI_SYNC_ENABLED, enabled).commit()
    }

    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun isTtsEnabled(): Boolean = prefs.getBoolean(KEY_TTS_ENABLED, true)
    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }

    fun isForegroundEnabled(): Boolean = prefs.getBoolean(KEY_FOREGROUND_ENABLED, true)
    fun setForegroundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOREGROUND_ENABLED, enabled).apply()
    }

    fun isHeadsUpEnabled(): Boolean = prefs.getBoolean(KEY_HEADS_UP_ENABLED, true)
    fun setHeadsUpEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HEADS_UP_ENABLED, enabled).apply()
    }

    fun isAutoRetryEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_RETRY_ENABLED, true)
    fun setAutoRetryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RETRY_ENABLED, enabled).apply()
    }
}
