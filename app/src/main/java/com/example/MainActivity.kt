package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FastApiConfigScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.YapePurple
import com.example.ui.theme.YapeTeal

enum class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Monitor", Icons.Default.GraphicEq, "nav_dashboard"),
    HISTORY("Historial", Icons.Default.ReceiptLong, "nav_history"),
    FASTAPI("FastAPI", Icons.Default.Code, "nav_fastapi"),
    SETTINGS("Ajustes", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
                val todayPayments by viewModel.todayPayments.collectAsStateWithLifecycle()
                val filteredPayments by viewModel.filteredPayments.collectAsStateWithLifecycle()

                var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }

                // Check and request POST_NOTIFICATIONS on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Result handled gracefully */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPerm = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasNotificationPerm) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Refresh permissions on resume when user comes back from Settings
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermission()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Handle back press to return to Dashboard tab
                if (currentTab != NavTab.DASHBOARD) {
                    BackHandler {
                        currentTab = NavTab.DASHBOARD
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0814)),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Custom speech bubble S/ logo icon from image.png
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(Color(0xFF5A1E8A), CircleShape)
                                            .border(1.dp, Color(0xFF00E5C9).copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Text(
                                            text = "S/",
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF00E5C9),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Yape ",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Monitor",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = Color(0xFF00E5C9)
                                    )
                                }
                            },
                            actions = {
                                // Monitoreo activo badge matching mockup image.png
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(end = 14.dp)
                                        .background(
                                            if (uiState.isPermissionGranted) Color(0xFF102824) else Color(0xFF2B131D),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (uiState.isPermissionGranted) Color(0xFF00E5C9).copy(alpha = 0.4f) else StatusFailed.copy(alpha = 0.4f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(
                                                if (uiState.isPermissionGranted) Color(0xFF00E5C9) else StatusFailed,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.isPermissionGranted) "Monitoreo activo" else "Monitoreo inactivo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isPermissionGranted) Color(0xFF00E5C9) else StatusFailed
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF0C0814)
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF0C0814),
                            tonalElevation = 8.dp
                        ) {
                            NavTab.values().forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                                        indicatorColor = Color(0xFF6B1B9A)
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0C0814))
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            NavTab.DASHBOARD -> DashboardScreen(
                                uiState = uiState,
                                todayPayments = todayPayments,
                                recentPayments = allPayments,
                                onRefreshPermission = { viewModel.refreshPermission() },
                                onSimulatePayment = { sender, amount ->
                                    viewModel.simulateNotification(sender, amount)
                                },
                                onScanActiveNotifications = { viewModel.scanActiveNotifications() },
                                onDismissScanMessage = { viewModel.dismissScanMessage() },
                                onTestConnection = { viewModel.testWebhookConnection() },
                                onDismissAlert = { viewModel.dismissAlert() },
                                onDismissTestResult = { viewModel.dismissTestResult() },
                                onDeletePayment = { id -> viewModel.deletePayment(id) },
                                onDeleteDuplicates = { viewModel.deleteDuplicates() },
                                onRetrySync = { id -> viewModel.retrySync(id) },
                                onNavigateToHistory = { currentTab = NavTab.HISTORY },
                                onNavigateToConfig = { currentTab = NavTab.FASTAPI }
                            )

                            NavTab.HISTORY -> HistoryScreen(
                                payments = filteredPayments,
                                searchQuery = uiState.searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onRetrySync = { id -> viewModel.retrySync(id) },
                                onDeletePayment = { id -> viewModel.deletePayment(id) },
                                onDeleteDuplicates = { viewModel.deleteDuplicates() },
                                onDeleteTestPayments = { viewModel.deleteTestPayments() },
                                onClearAll = { viewModel.clearAllPayments() }
                            )

                            NavTab.FASTAPI -> FastApiConfigScreen(
                                uiState = uiState,
                                onSaveConfig = { url, token -> viewModel.saveFastApiConfig(url, token) },
                                onSaveUrl = { viewModel.setWebhookUrl(it) },
                                onSaveToken = { viewModel.setAuthToken(it) },
                                onToggleFastApiSync = { viewModel.toggleFastApiSync(it) },
                                onTestConnection = { url, token -> viewModel.testWebhookConnection(url, token) },
                                onDismissTestResult = { viewModel.dismissTestResult() }
                            )

                            NavTab.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onToggleSound = { viewModel.toggleSound(it) },
                                onToggleVibration = { viewModel.toggleVibration(it) },
                                onToggleTts = { viewModel.toggleTts(it) },
                                onToggleForeground = { viewModel.toggleForeground(it) },
                                onToggleHeadsUp = { viewModel.toggleHeadsUp(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
