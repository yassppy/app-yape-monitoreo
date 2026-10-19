package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncStatus
import com.example.data.model.YapePayment
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentDetailDialog(
    payment: YapePayment,
    onDismiss: () -> Unit,
    onRetrySync: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val formattedDate = sdf.format(Date(payment.timestamp))
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Detalle del Depósito",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "S/ %.2f", payment.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Status chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            when (payment.syncStatus) {
                                SyncStatus.SUCCESS -> StatusSuccess.copy(alpha = 0.15f)
                                SyncStatus.FAILED -> StatusFailed.copy(alpha = 0.15f)
                                SyncStatus.PENDING -> StatusPending.copy(alpha = 0.15f)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = when (payment.syncStatus) {
                            SyncStatus.SUCCESS -> Icons.Default.CheckCircle
                            SyncStatus.FAILED -> Icons.Default.Error
                            SyncStatus.PENDING -> Icons.Default.HourglassEmpty
                        },
                        contentDescription = null,
                        tint = when (payment.syncStatus) {
                            SyncStatus.SUCCESS -> StatusSuccess
                            SyncStatus.FAILED -> StatusFailed
                            SyncStatus.PENDING -> StatusPending
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (payment.syncStatus) {
                            SyncStatus.SUCCESS -> "Enviado a FastAPI (HTTP ${payment.httpStatusCode ?: 200})"
                            SyncStatus.FAILED -> "Error de envío (${payment.errorMessage ?: "Fallo de red"})"
                            SyncStatus.PENDING -> "Guardado en dispositivo"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (payment.syncStatus) {
                            SyncStatus.SUCCESS -> StatusSuccess
                            SyncStatus.FAILED -> StatusFailed
                            SyncStatus.PENDING -> StatusPending
                        }
                    )
                }

                // If failed, provide copyable error details
                if (payment.syncStatus == SyncStatus.FAILED && !payment.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusFailed.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Detalle del error:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusFailed
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = payment.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = StatusFailed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val errReport = "🚨 Error de envío FastAPI (Pago #${payment.id}):\nRemitente: ${payment.sender}\nMonto: S/ ${payment.amount}\nError: ${payment.errorMessage}"
                                    val clip = ClipData.newPlainText("Error Webhook Pago", errReport)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Error copiado para pasarle al dev", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar error al dev", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                DetailRow(label = "Remitente:", value = payment.sender)
                DetailRow(label = "Fecha y hora:", value = formattedDate)
                DetailRow(label = "Moneda:", value = payment.currency)
                DetailRow(label = "Origen:", value = payment.packageName.ifEmpty { "Yape App" })
                DetailRow(label = "Intentos de envío:", value = "${payment.attempts}")

                if (payment.syncedAt != null) {
                    DetailRow(
                        label = "Confirmado a las:",
                        value = sdf.format(Date(payment.syncedAt))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mensaje original de la notificación:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = payment.rawBody.ifEmpty { payment.rawTitle },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (!payment.responseBody.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Respuesta del servidor FastAPI:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = payment.responseBody,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFA6E3A1),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Delete button requesting confirmation
                TextButton(
                    onClick = {
                        showDeleteConfirm = true
                    }
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar notificación",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }

                if (payment.syncStatus != SyncStatus.SUCCESS) {
                    Button(
                        onClick = {
                            onRetrySync(payment.id)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reintentar")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )

    // Confirmation dialog before deletion
    if (showDeleteConfirm) {
        ConfirmDeletePaymentDialog(
            payment = payment,
            onConfirm = {
                showDeleteConfirm = false
                onDelete(payment.id)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
