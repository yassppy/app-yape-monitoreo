package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UiState
import com.example.ui.components.FastApiCodeDialog
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.YapePurple
import com.example.ui.theme.YapeTeal

@Composable
fun FastApiConfigScreen(
    uiState: UiState,
    onSaveUrl: (String) -> Unit = {},
    onSaveToken: (String) -> Unit = {},
    onSaveConfig: (String, String) -> Unit = { _, _ -> },
    onToggleFastApiSync: (Boolean) -> Unit,
    onTestConnection: (String?, String?) -> Unit = { _, _ -> }
) {
    var urlInput by remember(uiState.webhookUrl) { mutableStateOf(uiState.webhookUrl) }
    var tokenInput by remember(uiState.authToken) { mutableStateOf(uiState.authToken) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }

    val presetUrls = listOf(
        "http://192.168.1.50:8000/webhook/yape" to "/webhook/yape",
        "http://192.168.1.50:8000/api/yape-webhook" to "/api/yape-webhook",
        "http://10.0.2.2:8000/webhook/yape" to "Emulador (10.0.2.2)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fastapi_config_screen"),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Toggle Master for FastAPI Sync
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isFastApiSyncEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (uiState.isFastApiSyncEnabled) YapePurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Envío a Servidor FastAPI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isFastApiSyncEnabled) {
                                "Activado: enviará cada pago en tiempo real a tu endpoint."
                            } else {
                                "Desactivado: funciona en modo alertas locales (sin intentar conectar por red)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = uiState.isFastApiSyncEnabled,
                        onCheckedChange = onToggleFastApiSync,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = YapePurple
                        ),
                        modifier = Modifier.testTag("toggle_fastapi_sync")
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Webhook URL and Password Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "URL del Webhook (POST)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dirección HTTP o HTTPS de tu endpoint donde corre FastAPI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            showSavedMessage = false
                        },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        placeholder = { Text("http://192.168.1.X:8000/webhook/yape") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_url_input")
                    )

                    if (urlInput.isNotBlank() && !urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 Se añadirá http:// automáticamente al guardar",
                            style = MaterialTheme.typography.labelSmall,
                            color = YapeTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Atajos rápidos de URL:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetUrls.forEach { (url, label) ->
                            FilterChip(
                                selected = urlInput == url,
                                onClick = {
                                    urlInput = url
                                    onSaveUrl(url)
                                    onSaveConfig(url, tokenInput)
                                    showSavedMessage = true
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Contraseña / Token Secreto (API Key)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Se enviará en los headers Authorization (Bearer) y X-API-Key. Se oculta por seguridad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            showSavedMessage = false
                        },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                modifier = Modifier.testTag("toggle_password_visibility")
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        placeholder = { Text("••••••••••••") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_token_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Saved Confirmation Banner
                    AnimatedVisibility(
                        visible = showSavedMessage,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("save_success_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "¡Configuración guardada exitosamente!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = StatusSuccess
                                    )
                                    Text(
                                        text = "La URL y contraseña están listas para las notificaciones.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { showSavedMessage = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar aviso",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveConfig(urlInput, tokenInput)
                                onSaveUrl(urlInput)
                                onSaveToken(tokenInput)
                                showSavedMessage = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showSavedMessage) StatusSuccess else YapePurple
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_config_button")
                        ) {
                            Icon(
                                imageVector = if (showSavedMessage) Icons.Default.Check else Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showSavedMessage) "¡Guardado!" else "Guardar Cambios")
                        }

                        OutlinedButton(
                            onClick = {
                                onSaveConfig(urlInput, tokenInput)
                                onSaveUrl(urlInput)
                                onSaveToken(tokenInput)
                                showSavedMessage = true
                                onTestConnection(urlInput, tokenInput)
                            },
                            enabled = !uiState.isTestingConnection,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_config_connection_button")
                        ) {
                            if (uiState.isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Probar Conexión")
                        }
                    }
                }
            }
        }

        // Test Result Box
        if (uiState.testResult != null) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.testResult.isSuccess) {
                            StatusSuccess.copy(alpha = 0.12f)
                        } else {
                            StatusFailed.copy(alpha = 0.12f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.testResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (uiState.testResult.isSuccess) StatusSuccess else StatusFailed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.testResult.isSuccess) {
                                    "¡Respuesta exitosa! HTTP ${uiState.testResult.httpCode}"
                                } else {
                                    "Fallo de conexión"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.testResult.isSuccess) StatusSuccess else StatusFailed
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${uiState.testResult.latencyMs} ms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!uiState.testResult.isSuccess && uiState.testResult.errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.testResult.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusFailed
                            )
                        }

                        if (!uiState.testResult.responseBody.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Respuesta JSON recibida de FastAPI:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.testResult.responseBody,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA6E3A1),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Schema description & code generator button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Estructura del Payload JSON enviado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cada vez que llega un depósito, la app envía automáticamente esta carga útil a tu endpoint:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = """{
  "event": "yape_payment_received",
  "payment_id": 1,
  "sender": "Nombre del cliente",
  "amount": 20.00,
  "currency": "PEN",
  "timestamp": 1729309440000,
  "datetime_iso": "2026-10-18T22:44:00Z",
  "raw_title": "Confirmación de Pago",
  "raw_message": "Yape! ... te envió un pago por S/ 20",
  "app_source": "com.bcp.innovacxion.yapeapp",
  "device_name": "Xiaomi / Samsung / Pixel"
}""",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFA6E3A1),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showCodeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver y Copiar Código FastAPI en Python")
                    }
                }
            }
        }
    }

    if (showCodeDialog) {
        FastApiCodeDialog(onDismiss = { showCodeDialog = false })
    }
}
