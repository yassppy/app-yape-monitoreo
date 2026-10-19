package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncStatus
import com.example.data.model.YapePayment
import com.example.ui.components.ConfirmDeletePaymentDialog
import com.example.ui.components.PaymentDetailDialog
import com.example.ui.theme.YapeTeal
import java.util.Locale

@Composable
fun HistoryScreen(
    payments: List<YapePayment>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRetrySync: (Long) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onDeleteDuplicates: () -> Unit,
    onDeleteTestPayments: () -> Unit = {},
    onClearAll: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedPayment by remember { mutableStateOf<YapePayment?>(null) }
    var paymentToDelete by remember { mutableStateOf<YapePayment?>(null) }
    var showDuplicatesConfirm by remember { mutableStateOf(false) }
    var showDeleteTestConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filteredList = payments.filter { payment ->
        when (selectedFilter) {
            "SUCCESS" -> payment.syncStatus == SyncStatus.SUCCESS
            "FAILED" -> payment.syncStatus == SyncStatus.FAILED
            "PENDING" -> payment.syncStatus == SyncStatus.PENDING
            else -> true
        }
    }

    val totalAmount = filteredList.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Search and Clear Actions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar por remitente o monto...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Todos (${payments.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "SUCCESS",
                        onClick = { selectedFilter = "SUCCESS" },
                        label = { Text("Enviados") }
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Locales") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Summary bar & cleanup tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredList.size} pagos • S/ ${String.format(Locale.US, "%.2f", totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { showDuplicatesConfirm = true },
                            modifier = Modifier.testTag("clean_duplicates_history_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = YapeTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Depurar", fontSize = 12.sp, color = YapeTeal)
                        }

                        val hasTestPayments = payments.any {
                            it.rawTitle.contains("Prueba", ignoreCase = true) ||
                            it.sender.contains("Prueba", ignoreCase = true) ||
                            it.rawTitle.contains("Test", ignoreCase = true)
                        }
                        if (hasTestPayments) {
                            TextButton(
                                onClick = { showDeleteTestConfirm = true },
                                modifier = Modifier.testTag("delete_test_payments_button")
                            ) {
                                Text("Borrar pruebas", fontSize = 12.sp, color = Color(0xFFFFB74D))
                            }
                        }

                        if (payments.isNotEmpty()) {
                            TextButton(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Limpiar", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161026)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No se encontraron coincidencias" else "Historial de pagos vacío",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { payment ->
                PaymentItemCard(
                    payment = payment,
                    isFastApiEnabled = true,
                    onClick = { selectedPayment = payment },
                    onDelete = { paymentToDelete = payment }
                )
            }
        }
    }

    if (paymentToDelete != null) {
        ConfirmDeletePaymentDialog(
            payment = paymentToDelete!!,
            onConfirm = {
                val id = paymentToDelete!!.id
                paymentToDelete = null
                onDeletePayment(id)
            },
            onDismiss = { paymentToDelete = null }
        )
    }

    if (showDuplicatesConfirm) {
        AlertDialog(
            onDismissRequest = { showDuplicatesConfirm = false },
            title = {
                Text(
                    text = "¿Depurar notificaciones duplicadas?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Se conservará un solo registro por cada pago repetido con el mismo remitente y monto.\n\n¿Deseas depurar las notificaciones duplicadas?",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicatesConfirm = false
                        onDeleteDuplicates()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Sí, depurar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showDuplicatesConfirm = false }) {
                    Text("No, cancelar")
                }
            }
        )
    }

    if (showDeleteTestConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteTestConfirm = false },
            title = {
                Text(
                    text = "¿Eliminar notificaciones de prueba?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Se eliminarán únicamente los registros de prueba simulados previamente.\n\n¿Deseas eliminarlos?",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteTestConfirm = false
                        onDeleteTestPayments()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, eliminar pruebas", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showDeleteTestConfirm = false }) {
                    Text("No, cancelar")
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(
                    text = "¿Eliminar historial completo?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar TODOS los registros de pagos guardados en este dispositivo?\n\nEsta acción no se puede deshacer.",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, eliminar todo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("No, cancelar")
                }
            }
        )
    }

    if (selectedPayment != null) {
        PaymentDetailDialog(
            payment = selectedPayment!!,
            onDismiss = { selectedPayment = null },
            onRetrySync = onRetrySync,
            onDelete = { id ->
                onDeletePayment(id)
                selectedPayment = null
            }
        )
    }
}
