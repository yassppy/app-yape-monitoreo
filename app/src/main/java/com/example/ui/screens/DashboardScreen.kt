package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncStatus
import com.example.data.model.YapePayment
import com.example.ui.UiState
import com.example.ui.components.ConfirmDeletePaymentDialog
import com.example.ui.components.FastApiCodeDialog
import com.example.ui.components.LiveAlertBanner
import com.example.ui.components.PaymentDetailDialog
import com.example.ui.components.PermissionCard
import com.example.ui.components.SimulatePaymentDialog
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.YapePink
import com.example.ui.theme.YapePurple
import com.example.ui.theme.YapeTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: UiState,
    todayPayments: List<YapePayment>,
    recentPayments: List<YapePayment>,
    onRefreshPermission: () -> Unit,
    onSimulatePayment: (sender: String, amount: Double) -> Unit,
    onScanActiveNotifications: () -> Unit,
    onDismissScanMessage: () -> Unit,
    onTestConnection: () -> Unit,
    onDismissAlert: () -> Unit,
    onDismissTestResult: () -> Unit,
    onDeletePayment: (Long) -> Unit,
    onDeleteDuplicates: () -> Unit,
    onRetrySync: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    var showSimulateDialog by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var selectedPayment by remember { mutableStateOf<YapePayment?>(null) }
    var paymentToDelete by remember { mutableStateOf<YapePayment?>(null) }
    var showDuplicatesConfirm by remember { mutableStateOf(false) }

    val totalTodayAmount = todayPayments.sumOf { it.amount }
    val totalTodayCount = todayPayments.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Live alert banner if active
        item {
            LiveAlertBanner(
                payment = uiState.latestAlert,
                onDismiss = onDismissAlert
            )
        }

        // Notification Permission prompt
        item {
            PermissionCard(
                isGranted = uiState.isPermissionGranted,
                isListenerConnected = uiState.isListenerConnected,
                onRefresh = onRefreshPermission
            )
        }

        // Scan Result Message Card
        if (uiState.scanMessage != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1730)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, YapeTeal.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = YapeTeal,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.scanMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            lineHeight = 18.sp
                        )
                        IconButton(
                            onClick = onDismissScanMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hero Card: Recaudación de hoy (Styled exactly as mockup image.png)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF53127A),
                                    Color(0xFF741E9E),
                                    Color(0xFF400962)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // Title row with icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Recaudación de hoy",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.95f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big Amount
                        Text(
                            text = String.format(Locale.US, "S/ %.2f", totalTodayAmount),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom row of hero card (Transactions count & Today)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = YapeTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$totalTodayCount transacciones",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Depósitos hoy",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(1.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Hoy",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Primary Action: Escanear notificaciones (Cyan pill button with bell icon and arrow from image.png)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = onScanActiveNotifications,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5C9),
                        contentColor = Color(0xFF0C0814)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("scan_active_notifications_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFF0C0814),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Escanear notificaciones",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0C0814)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF0C0814),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Two Action Buttons: "Probar pago" and "Probar FastAPI" (Styled exactly as mockup image.png)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Probar Pago Button
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D142E)),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable { showSimulateDialog = true }
                        .testTag("simulate_notification_button")
                        .border(1.dp, Color(0xFF33204E), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF8A2BE2), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Probar pago",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Probar FastAPI Button
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D142E)),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable { if (!uiState.isTestingConnection) onTestConnection() }
                        .testTag("test_fastapi_button")
                        .border(1.dp, Color(0xFF33204E), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF5A1E8A), CircleShape)
                        ) {
                            if (uiState.isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Probar FastAPI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Test connection result banner
        if (uiState.testResult != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.testResult.isSuccess) {
                            StatusSuccess.copy(alpha = 0.15f)
                        } else {
                            StatusFailed.copy(alpha = 0.15f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(
                            1.dp,
                            if (uiState.testResult.isSuccess) StatusSuccess.copy(alpha = 0.4f) else StatusFailed.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.testResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (uiState.testResult.isSuccess) StatusSuccess else StatusFailed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.testResult.isSuccess) {
                                    "¡Conexión FastAPI Exitosa! (HTTP ${uiState.testResult.httpCode} - ${uiState.testResult.latencyMs}ms)"
                                } else {
                                    "No se pudo conectar: ${uiState.testResult.errorMessage}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.testResult.isSuccess) StatusSuccess else StatusFailed
                            )
                            Text(
                                text = "Destino: ${uiState.webhookUrl}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = onDismissTestResult,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar aviso",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section Title: "Últimos depósitos detectados" + "Ver historial >" (Styled exactly as mockup image.png)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.5.dp, Color(0xFFB346D8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFFB346D8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Últimos depósitos detectados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToHistory() }
                ) {
                    Text(
                        text = "Ver historial",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD678EE)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFFD678EE),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Clean duplicates action bar if items exist
        if (recentPayments.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showDuplicatesConfirm = true },
                        modifier = Modifier.testTag("clean_duplicates_button")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = YapeTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Depurar duplicados",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = YapeTeal
                        )
                    }
                }
            }
        }

        // Empty state matching the exact graphic and copy in mockup image.png
        if (recentPayments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161026)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, Color(0xFF281C3F), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 24.dp)
                    ) {
                        // Graphic matching the phone with S/ badge in image.png
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(74.dp)
                                .background(Color(0xFF24163E), CircleShape)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF00E5C9), CircleShape)
                                ) {
                                    Text(
                                        text = "S/",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF53127A)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Aún no hay depósitos\nregistrados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Cuando recibas una notificación de Yape,\naparecerá aquí.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(recentPayments.take(10), key = { it.id }) { payment ->
                PaymentItemCard(
                    payment = payment,
                    isFastApiEnabled = uiState.isFastApiSyncEnabled,
                    onClick = { selectedPayment = payment },
                    onDelete = { paymentToDelete = payment }
                )
            }
        }
    }

    if (showSimulateDialog) {
        SimulatePaymentDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { sender, amount ->
                onSimulatePayment(sender, amount)
            }
        )
    }

    if (showCodeDialog) {
        FastApiCodeDialog(
            onDismiss = { showCodeDialog = false }
        )
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
        androidx.compose.material3.AlertDialog(
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
                    text = "Se conservará un solo registro por cada pago repetido con el mismo remitente y monto.\n\n¿Deseas depurar las notificaciones duplicadas ahora?",
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

@Composable
fun PaymentItemCard(
    payment: YapePayment,
    isFastApiEnabled: Boolean = true,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = sdf.format(Date(payment.timestamp))

    val statusColor = when (payment.syncStatus) {
        SyncStatus.SUCCESS -> StatusSuccess
        SyncStatus.FAILED -> StatusFailed
        SyncStatus.PENDING -> if (isFastApiEnabled) StatusPending else StatusSuccess
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161026)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, Color(0xFF26193E), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .background(statusColor.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = when (payment.syncStatus) {
                        SyncStatus.SUCCESS -> Icons.Default.CheckCircle
                        SyncStatus.FAILED -> Icons.Default.Error
                        SyncStatus.PENDING -> if (isFastApiEnabled) Icons.Default.HourglassEmpty else Icons.Default.NotificationsActive
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.sender,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (payment.syncStatus) {
                            SyncStatus.SUCCESS -> "Enviado a FastAPI"
                            SyncStatus.FAILED -> "Error de red"
                            SyncStatus.PENDING -> if (isFastApiEnabled) "Pendiente" else "Guardado"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = String.format(Locale.US, "S/ %.2f", payment.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Direct 1-tap delete button on each item
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar notificación",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
