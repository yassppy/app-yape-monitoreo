package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulatePaymentDialog(
    onDismiss: () -> Unit,
    onSimulate: (sender: String, amount: Double) -> Unit
) {
    // Starts completely empty as requested by user:
    var sender by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    val quickAmounts = listOf("5.00", "10.00", "20.00", "50.00", "100.00")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simular Notificación Yape", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Escribe el nombre y monto que deseas probar para activar la alerta inmediata (sonido, vibración y voz):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Nombre del Remitente") },
                    placeholder = { Text("Escribe aquí el nombre") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_sender_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto en Soles (S/)") },
                    placeholder = { Text("Ej: 20.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Montos sugeridos:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAmounts.forEach { amt ->
                        FilterChip(
                            selected = amountText == amt,
                            onClick = { amountText = amt },
                            label = { Text("S/ $amt") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            var isSubmitted by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (!isSubmitted) {
                        isSubmitted = true
                        val amt = amountText.toDoubleOrNull() ?: 20.0
                        val cleanSender = sender.trim().ifEmpty { "Cliente Yape" }
                        onSimulate(cleanSender, amt)
                        onDismiss()
                    }
                },
                enabled = !isSubmitted,
                modifier = Modifier.testTag("simulate_confirm_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lanzar Alerta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
