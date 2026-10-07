package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DipuViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugAdminScreen(
    viewModel: DipuViewModel,
    onBack: () -> Unit
) {
    val auditLogs by viewModel.auditLogs.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val telegramConfig by viewModel.telegramConfig.collectAsState()
    val binanceConfig by viewModel.binanceConfig.collectAsState()

    var testSmsSender by remember { mutableStateOf("16247") }
    var testSmsBody by remember {
        mutableStateOf("You have received Tk 500.00 from 01711000000. Fee Tk 0.00. Balance Tk 12,500.00. TrxID 9K8L2M3P01 at 07/10/2026 14:30")
    }
    var simFeedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DipuDarkBg)
            .testTag("debug_admin_screen")
    ) {
        // Custom Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DipuCyan)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("ADMIN & DIAGNOSTICS", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Text("Live Test Engine & Audit Trail", fontSize = 11.sp, color = TextMuted)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Diagnostics Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DipuBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("CONNECTION STATUS DIAGNOSTICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DiagRow("Telegram Engine", if (telegramConfig.isConnected) "Operational" else "Inactive", if (telegramConfig.isConnected) StatusSuccess else StatusError)
                        DiagRow("Binance WebSocket/API", if (binanceConfig.isConnected) "Synced" else "Not Connected", if (binanceConfig.isConnected) StatusSuccess else StatusError)
                        DiagRow("Dual Mobile Sync Cluster", "SYNC-CLUSTER-ALPHA Active", StatusSuccess)
                        DiagRow("Total Transactions in DB", "${stats.totalTransactions} items", DipuCyan)
                        DiagRow("Duplicate Prevention Index", "UNIQUE(provider, trxId) Active", StatusSuccess)

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.testTelegramConnection { _, _ -> } },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("PING TG", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.syncBinance { _, _ -> } },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("PING BINANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SMS Simulator Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DipuCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DipuSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = DipuCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SMS INGESTION SIMULATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = 1.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Simulate real payment SMS from bKash (16247) or Nagad (16167) to test parsing, duplicate prevention, and Telegram forwarding end-to-end.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Template Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    val randomSuffix = (1000..9999).random()
                                    testSmsSender = "16247"
                                    testSmsBody = "You have received Tk 500.00 from 01711000000. Fee Tk 0.00. Balance Tk 12,500.00. TrxID BK$randomSuffix at 07/10/2026 14:30"
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BkashPink.copy(alpha = 0.25f), contentColor = BkashPink)
                            ) {
                                Text("bKash Sample", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val randomSuffix = (1000..9999).random()
                                    testSmsSender = "16167"
                                    testSmsBody = "Cash In Tk 1,200.00 from 01822000000. Fee Tk 0.00. Balance Tk 3,450.00. TxnID NG$randomSuffix at 07/10/2026 15:10"
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NagadOrange.copy(alpha = 0.25f), contentColor = NagadOrange)
                            ) {
                                Text("Nagad Sample", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = testSmsSender,
                            onValueChange = { testSmsSender = it },
                            label = { Text("Sender ID", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DipuCyan,
                                unfocusedBorderColor = DipuBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = testSmsBody,
                            onValueChange = { testSmsBody = it },
                            label = { Text("SMS Body", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DipuCyan,
                                unfocusedBorderColor = DipuBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.simulateSmsInput(testSmsSender, testSmsBody) { ok, msg ->
                                    simFeedback = msg
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("sim_inject_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DipuCyan, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN SIMULATED INGESTION", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        if (simFeedback != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = simFeedback.orEmpty(), fontSize = 11.sp, color = DipuCyan)
                        }
                    }
                }
            }

            // Realtime Audit Logs Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AUDIT LOGS (${auditLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Text("Clear All Data", fontSize = 11.sp, color = StatusError, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.clearAllData() })
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (auditLogs.isEmpty()) {
                item {
                    Text("No audit logs recorded yet.", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(vertical = 12.dp))
                }
            } else {
                items(auditLogs) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, DipuBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DipuCyan)
                                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                Text(text = timeStr, fontSize = 10.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = log.details, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DiagRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
