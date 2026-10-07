package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.TransactionEntity
import com.example.ui.components.DipuTopBar
import com.example.ui.components.PaymentMethodCard
import com.example.ui.components.StatCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BkashPink
import com.example.ui.theme.DipuBorder
import com.example.ui.theme.DipuCyan
import com.example.ui.theme.DipuDarkBg
import com.example.ui.theme.DipuSurfaceCard
import com.example.ui.theme.DipuSurfaceElevated
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DipuViewModel

@Composable
fun DashboardScreen(
    viewModel: DipuViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToTrxGroup: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    val stats by viewModel.dashboardStats.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val telegramConfig by viewModel.telegramConfig.collectAsState()
    val binanceConfig by viewModel.binanceConfig.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()

    // SMS permission check
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermission = results[Manifest.permission.RECEIVE_SMS] == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DipuDarkBg)
            .testTag("dashboard_screen")
    ) {
        DipuTopBar(
            deviceId = deviceId,
            isTelegramConnected = telegramConfig.isConnected,
            isBinanceConnected = binanceConfig.isConnected,
            onSyncClick = { viewModel.syncBinance { _, _ -> } }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Permission Banner (if not granted)
            if (!hasSmsPermission) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusWarning.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DipuSurfaceElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SMS Detection Inactive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "SMS permission required to detect bKash and Nagad payments automatically.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    smsPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusWarning, contentColor = Color.Black),
                                modifier = Modifier.testTag("grant_sms_btn")
                            ) {
                                Text("GRANT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick Actions Bar
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        title = "TRX → Group",
                        icon = Icons.Default.Send,
                        color = DipuCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTrxGroup
                    )
                    QuickActionButton(
                        title = "Calculator",
                        icon = Icons.Default.Calculate,
                        color = BinanceGold,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCalculator
                    )
                    QuickActionButton(
                        title = "History",
                        icon = Icons.Outlined.History,
                        color = BkashPink,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHistory
                    )
                }
            }

            // 4 Animated Stats Cards Grid
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "TOTAL RECEIVED",
                        value = "৳ %,.2f".format(stats.totalReceivedBdt),
                        subtitle = if (stats.totalReceivedUsdt > 0) "+ $%,.2f USDT".format(stats.totalReceivedUsdt) else "Across all methods",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        accentColor = DipuCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "TODAY",
                        value = "৳ %,.2f".format(stats.todayAmountBdt),
                        subtitle = if (stats.todayAmountUsdt > 0) "+ $%,.2f USDT".format(stats.todayAmountUsdt) else "Live today",
                        icon = Icons.Outlined.Today,
                        accentColor = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "VALID SMS",
                        value = "%03d".format(stats.validSmsCount),
                        subtitle = "Verified notifications",
                        icon = Icons.Default.Sms,
                        accentColor = NagadOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "TRANSACTIONS",
                        value = "%03d".format(stats.totalTransactions),
                        subtitle = "Sync cluster",
                        icon = Icons.Outlined.Check,
                        accentColor = BinanceGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Payment Methods Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAYMENT GATEWAYS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Configured & Monitored",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                PaymentMethodCard(
                    provider = "bKash",
                    color = BkashPink,
                    count = stats.bkashCount,
                    total = stats.bkashTotal,
                    currency = "BDT",
                    lastTrx = stats.bkashLastTrx,
                    isConnected = true,
                    onClick = onNavigateToHistory
                )

                Spacer(modifier = Modifier.height(8.dp))

                PaymentMethodCard(
                    provider = "Nagad",
                    color = NagadOrange,
                    count = stats.nagadCount,
                    total = stats.nagadTotal,
                    currency = "BDT",
                    lastTrx = stats.nagadLastTrx,
                    isConnected = true,
                    onClick = onNavigateToHistory
                )

                Spacer(modifier = Modifier.height(8.dp))

                PaymentMethodCard(
                    provider = "Binance",
                    color = BinanceGold,
                    count = stats.binanceCount,
                    total = stats.binanceTotal,
                    currency = "USDT",
                    lastTrx = stats.binanceLastTrx,
                    isConnected = binanceConfig.isConnected,
                    onClick = onNavigateToSettings
                )
            }

            // Recent Transactions Section
            item {
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT TRANSACTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "View All →",
                        fontSize = 12.sp,
                        color = DipuCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, DipuBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📭", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Incoming payments from bKash, Nagad or Binance will automatically appear here.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(transactions.take(5)) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, DipuBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DipuSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}
