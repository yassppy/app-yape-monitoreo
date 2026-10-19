package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.YapeMonitorApp
import com.example.alert.AlertManager
import com.example.data.AppPreferences
import com.example.data.model.YapePayment
import com.example.data.repository.PaymentRepository
import com.example.network.WebhookResult
import com.example.service.YapeForegroundService
import com.example.service.YapeNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val isPermissionGranted: Boolean = false,
    val isListenerConnected: Boolean = false,
    val webhookUrl: String = "",
    val authToken: String = "",
    val isFastApiSyncEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val ttsEnabled: Boolean = true,
    val foregroundEnabled: Boolean = true,
    val headsUpEnabled: Boolean = true,
    val isTestingConnection: Boolean = false,
    val testResult: WebhookResult? = null,
    val latestAlert: YapePayment? = null,
    val searchQuery: String = "",
    val scanMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as YapeMonitorApp
    private val repository: PaymentRepository = app.repository
    private val preferences: AppPreferences = app.preferences
    private val alertManager: AlertManager = app.alertManager

    private val _uiState = MutableStateFlow(
        UiState(
            isPermissionGranted = YapeNotificationListenerService.isNotificationAccessGranted(app),
            isListenerConnected = YapeNotificationListenerService.isListenerConnected.value,
            webhookUrl = preferences.getWebhookUrl(),
            authToken = preferences.getAuthToken(),
            isFastApiSyncEnabled = preferences.isFastApiSyncEnabled(),
            soundEnabled = preferences.isSoundEnabled(),
            vibrationEnabled = preferences.isVibrationEnabled(),
            ttsEnabled = preferences.isTtsEnabled(),
            foregroundEnabled = preferences.isForegroundEnabled(),
            headsUpEnabled = preferences.isHeadsUpEnabled()
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val allPayments: StateFlow<List<YapePayment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayPayments: StateFlow<List<YapePayment>> = repository.getTodayPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPayments: StateFlow<List<YapePayment>> = combine(allPayments, _uiState) { payments, state ->
        if (state.searchQuery.isBlank()) {
            payments
        } else {
            val q = state.searchQuery.trim().lowercase()
            payments.filter {
                it.sender.lowercase().contains(q) ||
                it.amount.toString().contains(q) ||
                it.rawBody.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect real-time in-app alerts from AlertManager
        viewModelScope.launch {
            alertManager.livePaymentEvents.collect { payment ->
                _uiState.value = _uiState.value.copy(latestAlert = payment)
            }
        }

        // Collect listener connected state and scan active notifications on connect
        viewModelScope.launch {
            YapeNotificationListenerService.isListenerConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isListenerConnected = connected)
                if (connected) {
                    YapeNotificationListenerService.instance?.scanActiveNotifications()
                }
            }
        }

        // Start foreground service if enabled
        if (preferences.isForegroundEnabled()) {
            try {
                YapeForegroundService.startService(app)
            } catch (_: Exception) {}
        }

        // Clean any existing duplicates from earlier tests
        viewModelScope.launch {
            repository.deleteDuplicates()
        }
    }

    fun refreshPermission() {
        val granted = YapeNotificationListenerService.isNotificationAccessGranted(app)
        _uiState.value = _uiState.value.copy(isPermissionGranted = granted)
        if (granted) {
            viewModelScope.launch {
                YapeNotificationListenerService.instance?.scanActiveNotifications()
            }
        }
    }

    fun scanActiveNotifications() {
        viewModelScope.launch {
            val listener = YapeNotificationListenerService.instance
            if (listener == null) {
                val hasPerm = YapeNotificationListenerService.isNotificationAccessGranted(app)
                val msg = if (!hasPerm) {
                    "Acceso a Notificaciones pendiente. Por favor activa el permiso arriba para que la app pueda leer la barra de notificaciones."
                } else {
                    "El servicio del sistema se está iniciando. Espera un momento y vuelve a escanear."
                }
                _uiState.value = _uiState.value.copy(scanMessage = msg)
                return@launch
            }
            val count = listener.scanActiveNotifications()
            val msg = if (count > 0) {
                "¡Se detectaron y agregaron $count pagos de Yape desde tu barra de notificaciones!"
            } else {
                "No se encontraron notificaciones pendientes de Yape en la barra de estado. (Comprueba que la notificación de Yape siga visible arriba y no haya sido descartada)."
            }
            _uiState.value = _uiState.value.copy(scanMessage = msg)
        }
    }

    fun dismissScanMessage() {
        _uiState.value = _uiState.value.copy(scanMessage = null)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setWebhookUrl(url: String) {
        preferences.setWebhookUrl(url)
        _uiState.value = _uiState.value.copy(webhookUrl = preferences.getWebhookUrl())
    }

    fun setAuthToken(token: String) {
        preferences.setAuthToken(token)
        _uiState.value = _uiState.value.copy(authToken = preferences.getAuthToken())
    }

    fun saveFastApiConfig(url: String, token: String) {
        preferences.setWebhookUrl(url)
        preferences.setAuthToken(token)
        _uiState.value = _uiState.value.copy(
            webhookUrl = preferences.getWebhookUrl(),
            authToken = preferences.getAuthToken()
        )
    }

    fun toggleFastApiSync(enabled: Boolean) {
        preferences.setFastApiSyncEnabled(enabled)
        _uiState.value = _uiState.value.copy(isFastApiSyncEnabled = enabled)
    }

    fun toggleSound(enabled: Boolean) {
        preferences.setSoundEnabled(enabled)
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
    }

    fun toggleVibration(enabled: Boolean) {
        preferences.setVibrationEnabled(enabled)
        _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
    }

    fun toggleTts(enabled: Boolean) {
        preferences.setTtsEnabled(enabled)
        _uiState.value = _uiState.value.copy(ttsEnabled = enabled)
    }

    fun toggleForeground(enabled: Boolean) {
        preferences.setForegroundEnabled(enabled)
        _uiState.value = _uiState.value.copy(foregroundEnabled = enabled)
        if (enabled) {
            YapeForegroundService.startService(app)
        } else {
            YapeForegroundService.stopService(app)
        }
    }

    fun toggleHeadsUp(enabled: Boolean) {
        preferences.setHeadsUpEnabled(enabled)
        _uiState.value = _uiState.value.copy(headsUpEnabled = enabled)
    }

    fun testWebhookConnection(customUrl: String? = null, customToken: String? = null) {
        if (customUrl != null) {
            preferences.setWebhookUrl(customUrl)
        }
        if (customToken != null) {
            preferences.setAuthToken(customToken)
        }
        val targetUrl = preferences.getWebhookUrl()
        val targetToken = preferences.getAuthToken()

        _uiState.value = _uiState.value.copy(
            webhookUrl = targetUrl,
            authToken = targetToken,
            isTestingConnection = true,
            testResult = null
        )

        viewModelScope.launch {
            val result = repository.testFastApiConnection(
                url = targetUrl,
                token = targetToken
            )
            _uiState.value = _uiState.value.copy(isTestingConnection = false, testResult = result)
        }
    }

    fun dismissTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }

    fun dismissAlert() {
        _uiState.value = _uiState.value.copy(latestAlert = null)
    }

    fun simulateNotification(sender: String, amount: Double) {
        YapeNotificationListenerService.markAsProcessed(sender, amount)
        viewModelScope.launch {
            val payment = YapePayment(
                sender = sender,
                amount = amount,
                currency = "PEN",
                timestamp = System.currentTimeMillis(),
                rawTitle = "Confirmación de Pago",
                rawBody = "Yape! $sender te envió un pago por S/ ${if (amount % 1.0 == 0.0) amount.toInt().toString() else String.format("%.2f", amount)}",
                packageName = "com.bcp.innovacxion.yapeapp"
            )
            repository.processIncomingPayment(payment)
        }
    }

    fun retrySync(paymentId: Long) {
        viewModelScope.launch {
            repository.retrySync(paymentId)
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
        }
    }

    fun deleteDuplicates() {
        viewModelScope.launch {
            repository.deleteDuplicates()
        }
    }

    fun deleteTestPayments() {
        viewModelScope.launch {
            repository.deleteTestPayments()
        }
    }

    fun clearAllPayments() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
