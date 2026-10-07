package com.example.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebugAdminScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransactionDetailDialog
import com.example.ui.screens.TrxGroupAddScreen
import com.example.ui.theme.DipuBorder
import com.example.ui.theme.DipuCyan
import com.example.ui.theme.DipuDarkBg
import com.example.ui.theme.DipuSurfaceCard
import com.example.ui.theme.DipuSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DipuViewModel
import kotlinx.coroutines.launch

enum class ScreenRoute {
    SPLASH,
    AUTH,
    DASHBOARD,
    HISTORY,
    TRX_GROUP,
    CALCULATOR,
    PROFILE,
    SETTINGS,
    DEBUG_ADMIN
}

data class BottomNavItem(
    val route: ScreenRoute,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun DipuAppNavigation(viewModel: DipuViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(ScreenRoute.SPLASH) }
    val authState by viewModel.authUiState.collectAsState()
    val selectedTx by viewModel.selectedTransaction.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Listen to realtime events and show toast/snackbar
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem(ScreenRoute.DASHBOARD, "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
        BottomNavItem(ScreenRoute.HISTORY, "History", Icons.Default.History, "nav_history"),
        BottomNavItem(ScreenRoute.TRX_GROUP, "TRX → Group", Icons.Default.Send, "nav_trx_group"),
        BottomNavItem(ScreenRoute.CALCULATOR, "Calc", Icons.Default.Calculate, "nav_calc"),
        BottomNavItem(ScreenRoute.SETTINGS, "Settings", Icons.Default.Settings, "nav_settings"),
        BottomNavItem(ScreenRoute.PROFILE, "Profile", Icons.Default.Person, "nav_profile")
    )

    val showBottomBar = currentScreen !in listOf(ScreenRoute.SPLASH, ScreenRoute.AUTH, ScreenRoute.DEBUG_ADMIN)

    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen != ScreenRoute.DASHBOARD && currentScreen != ScreenRoute.SPLASH) {
        if (currentScreen == ScreenRoute.DEBUG_ADMIN) {
            currentScreen = ScreenRoute.SETTINGS
        } else {
            currentScreen = ScreenRoute.DASHBOARD
        }
    }

    Scaffold(
        containerColor = DipuDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DipuSurfaceCard,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DipuBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .height(72.dp)
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentScreen == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = item.route },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) DipuCyan else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) DipuCyan else TextMuted
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = DipuSurfaceElevated
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ScreenRoute.SPLASH -> {
                    SplashScreen(
                        onSplashFinished = {
                            currentScreen = if (authState.isLoggedIn) ScreenRoute.DASHBOARD else ScreenRoute.AUTH
                        }
                    )
                }

                ScreenRoute.AUTH -> {
                    AuthScreen(
                        onLoginSuccess = { currentScreen = ScreenRoute.DASHBOARD },
                        onLoginAttempt = { email, pass, callback ->
                            viewModel.login(email, pass, callback)
                        },
                        onRegisterAttempt = { name, email, pass, callback ->
                            viewModel.register(name, email, pass, callback)
                        }
                    )
                }

                ScreenRoute.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToHistory = { currentScreen = ScreenRoute.HISTORY },
                        onNavigateToTrxGroup = { currentScreen = ScreenRoute.TRX_GROUP },
                        onNavigateToCalculator = { currentScreen = ScreenRoute.CALCULATOR },
                        onNavigateToSettings = { currentScreen = ScreenRoute.SETTINGS },
                        onTransactionClick = { tx -> viewModel.selectTransaction(tx) }
                    )
                }

                ScreenRoute.HISTORY -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onTransactionClick = { tx -> viewModel.selectTransaction(tx) }
                    )
                }

                ScreenRoute.TRX_GROUP -> {
                    TrxGroupAddScreen(viewModel = viewModel)
                }

                ScreenRoute.CALCULATOR -> {
                    CalculatorScreen(viewModel = viewModel)
                }

                ScreenRoute.PROFILE -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        onLogoutClick = {
                            viewModel.logout()
                            currentScreen = ScreenRoute.AUTH
                        },
                        onOpenSettings = { currentScreen = ScreenRoute.SETTINGS }
                    )
                }

                ScreenRoute.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToDebugAdmin = { currentScreen = ScreenRoute.DEBUG_ADMIN }
                    )
                }

                ScreenRoute.DEBUG_ADMIN -> {
                    DebugAdminScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenRoute.SETTINGS }
                    )
                }
            }

            // Transaction Detail Dialog
            if (selectedTx != null) {
                TransactionDetailDialog(
                    transaction = selectedTx!!,
                    onDismiss = { viewModel.selectTransaction(null) },
                    onResendTelegram = { tx ->
                        viewModel.resendTransaction(tx) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onMarkReviewed = { tx ->
                        viewModel.markTransactionReviewed(tx)
                    }
                )
            }
        }
    }
}
