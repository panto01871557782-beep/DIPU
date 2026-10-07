package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ForwardingMode
import com.example.ui.components.DipuTopBar
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: DipuViewModel,
    onNavigateToDebugAdmin: () -> Unit
) {
    val context = LocalContext.current
    val telegramConfig by viewModel.telegramConfig.collectAsState()
    val binanceConfig by viewModel.binanceConfig.collectAsState()
    val isTestingTelegram by viewModel.isTestingTelegram.collectAsState()
    val isTestingBinance by viewModel.isTestingBinance.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()

    // Telegram Local Form State
    var botTokenInput by remember(telegramConfig) { mutableStateOf(telegramConfig.botToken) }
    var groupIdInput by remember(telegramConfig) { mutableStateOf(telegramConfig.groupId) }
    var forwardingMode by remember(telegramConfig) { mutableStateOf(telegramConfig.forwardingMode) }
    var isTokenVisible by remember { mutableStateOf(false) }
    var tgFeedback by remember { mutableStateOf<String?>(null) }
    var isTgFeedbackSuccess by remember { mutableStateOf(false) }

    // Binance Local Form State
    var apiKeyInput by remember(binanceConfig) { mutableStateOf(binanceConfig.apiKey) }
    var apiSecretInput by remember(binanceConfig) { mutableStateOf(binanceConfig.apiSecret) }
    var isSecretVisible by remember { mutableStateOf(false) }
    var binanceFeedback by remember { mutableStateOf<String?>(null) }

    // Payment toggles
    val prefs = viewModel.repository.securityPrefs
    var bkashEnabled by remember { mutableStateOf(prefs.isBkashEnabled()) }
    var nagadEnabled by remember { mutableStateOf(prefs.isNagadEnabled()) }
    var binanceEnabled by remember { mutableStateOf(prefs.isBinanceEnabled()) }

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
            .testTag("settings_screen")
    ) {
        DipuTopBar(
            title = "Terminal Settings",
            subtitle = "Security & Gateway Config",
            deviceId = deviceId,
            isTelegramConnected = telegramConfig.isConnected,
            isBinanceConnected = binanceConfig.isConnected
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ==========================================
            // TELEGRAM BOT INTEGRATION SECTION
            // ==========================================
            SettingsSectionHeader(title = "TELEGRAM BOT INTEGRATION", icon = Icons.Outlined.Send)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DipuBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connection Status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (telegramConfig.isConnected) StatusSuccess else StatusError)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (telegramConfig.isConnected) "Connected" else "Not Connected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (telegramConfig.isConnected) StatusSuccess else StatusError
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bot Token Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bot Token",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        // User quick token autofill helper
                        Text(
                            text = "Insert Test Token",
                            fontSize = 11.sp,
                            color = DipuCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    botTokenInput = "8747439451:AAGkE9k3CCz_RSIAnwN1-L5GI9QR8AqzFyQ"
                                    groupIdInput = "-1004323009837"
                                }
                                .testTag("quick_fill_token_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = botTokenInput,
                        onValueChange = { botTokenInput = it },
                        placeholder = { Text("Enter Telegram Bot Token", color = TextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    imageVector = if (isTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle token visibility",
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_token_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DipuCyan,
                            unfocusedBorderColor = DipuBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Group ID Field
                    Text(
                        text = "Group ID (Target Channel/Group)",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = groupIdInput,
                        onValueChange = { groupIdInput = it },
                        placeholder = { Text("e.g. -1004323009837", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_group_id_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DipuCyan,
                            unfocusedBorderColor = DipuBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Forwarding Mode (Mode 1 vs Mode 2)
                    Text(
                        text = "Telegram Forwarding Format",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DipuSurfaceElevated)
                            .clickable { forwardingMode = ForwardingMode.ID_ONLY }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = forwardingMode == ForwardingMode.ID_ONLY,
                            onClick = { forwardingMode = ForwardingMode.ID_ONLY },
                            colors = RadioButtonDefaults.colors(selectedColor = DipuCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Mode 1: Transaction ID Only", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Example: ABC123XYZ", fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DipuSurfaceElevated)
                            .clickable { forwardingMode = ForwardingMode.PAYMENT_DETAILS }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = forwardingMode == ForwardingMode.PAYMENT_DETAILS,
                            onClick = { forwardingMode = ForwardingMode.PAYMENT_DETAILS },
                            colors = RadioButtonDefaults.colors(selectedColor = DipuCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Mode 2: Full Payment Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Method, Amount, TrxID, Timestamp & Dipu branding", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save and Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveTelegramConfig(
                                    token = botTokenInput,
                                    groupId = groupIdInput,
                                    mode = forwardingMode,
                                    enabled = true
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("save_telegram_config_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DipuSurfaceElevated, contentColor = TextPrimary)
                        ) {
                            Text("SAVE CONFIG", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.saveTelegramConfig(botTokenInput, groupIdInput, forwardingMode, true)
                                viewModel.testTelegramConnection { success, msg ->
                                    isTgFeedbackSuccess = success
                                    tgFeedback = msg
                                }
                            },
                            enabled = !isTestingTelegram,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("test_telegram_connection_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DipuCyan, contentColor = Color.Black)
                        ) {
                            if (isTestingTelegram) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("TEST BOT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.saveTelegramConfig(botTokenInput, groupIdInput, forwardingMode, true)
                            viewModel.sendTelegramTestMessage { success, msg ->
                                isTgFeedbackSuccess = success
                                tgFeedback = msg
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("send_test_message_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SEND TEST MESSAGE TO GROUP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    if (tgFeedback != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTgFeedbackSuccess) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = tgFeedback.orEmpty(),
                                fontSize = 11.sp,
                                color = if (isTgFeedbackSuccess) StatusSuccess else StatusError
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // PAYMENT SOURCES TOGGLE SECTION
            // ==========================================
            SettingsSectionHeader(title = "PAYMENT MONITORS", icon = Icons.Default.Security)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DipuBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PaymentToggleRow(
                        title = "bKash SMS Monitor",
                        subtitle = "Parse 16247 payments & Cash-in automatically",
                        color = BkashPink,
                        isChecked = bkashEnabled,
                        onCheckedChange = {
                            bkashEnabled = it
                            prefs.setBkashEnabled(it)
                        }
                    )

                    Divider(color = DipuBorder, modifier = Modifier.padding(vertical = 10.dp))

                    PaymentToggleRow(
                        title = "Nagad SMS Monitor",
                        subtitle = "Parse 16167 notifications & payments",
                        color = NagadOrange,
                        isChecked = nagadEnabled,
                        onCheckedChange = {
                            nagadEnabled = it
                            prefs.setNagadEnabled(it)
                        }
                    )

                    Divider(color = DipuBorder, modifier = Modifier.padding(vertical = 10.dp))

                    PaymentToggleRow(
                        title = "Binance Official API Monitor",
                        subtitle = "Sync crypto payments and C2C/spot orders",
                        color = BinanceGold,
                        isChecked = binanceEnabled,
                        onCheckedChange = {
                            binanceEnabled = it
                            prefs.setBinanceEnabled(it)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // BINANCE OFFICIAL API CONFIGURATION
            // ==========================================
            SettingsSectionHeader(title = "BINANCE OFFICIAL API", icon = Icons.Outlined.CurrencyBitcoin)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DipuBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Official API Connection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = if (binanceConfig.isConnected) "🟢 Connected" else "🔴 Not Connected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (binanceConfig.isConnected) StatusSuccess else StatusError
                        )
                    }

                    if (binanceConfig.lastSyncTime > 0) {
                        val syncTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(binanceConfig.lastSyncTime))
                        Text(
                            text = "Last Sync: $syncTimeStr (${binanceConfig.lastSyncMessage ?: "OK"})",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Binance API Key", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("Paste official Binance API Key", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("binance_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BinanceGold,
                            unfocusedBorderColor = DipuBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Binance API Secret (HMAC SHA-256)", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = apiSecretInput,
                        onValueChange = { apiSecretInput = it },
                        placeholder = { Text("Paste official Binance API Secret", color = TextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                Icon(
                                    imageVector = if (isSecretVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle secret",
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("binance_secret_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BinanceGold,
                            unfocusedBorderColor = DipuBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveBinanceConfig(apiKeyInput, apiSecretInput)
                                viewModel.syncBinance { ok, msg -> binanceFeedback = msg }
                            },
                            enabled = !isTestingBinance,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("connect_binance_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BinanceGold, contentColor = Color.Black)
                        ) {
                            if (isTestingBinance) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("CONNECT & SYNC", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        if (binanceConfig.isConnected) {
                            OutlinedButton(
                                onClick = {
                                    apiKeyInput = ""
                                    apiSecretInput = ""
                                    viewModel.disconnectBinance()
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("DISCONNECT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StatusError)
                            }
                        }
                    }

                    if (binanceFeedback != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = binanceFeedback.orEmpty(), fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SMS DETECTION & PERMISSIONS SECTION
            // ==========================================
            SettingsSectionHeader(title = "SMS DETECTION ENGINE", icon = Icons.Default.Sms)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DipuBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SMS Permission Status", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = if (hasSmsPermission) "🟢 GRANTED" else "⚠️ REQUIRED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSmsPermission) StatusSuccess else StatusWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Android requires explicit permission to inspect payment SMS. The app strictly extracts ONLY valid bKash and Nagad payment notification patterns and never forwards private SMS, OTPs or ads.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    if (!hasSmsPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                smsPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarning, contentColor = Color.Black)
                        ) {
                            Text("REQUEST SMS PERMISSIONS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // DEBUG / ADMIN MODE LINK
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DipuCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { onNavigateToDebugAdmin() }
                    .testTag("admin_debug_nav_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DipuSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = DipuCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ADMIN & DEBUG TERMINAL", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("Simulate incoming SMS, test audit logs, verify duplicate prevention", fontSize = 11.sp, color = TextSecondary)
                    }
                    Text("OPEN →", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DipuCyan)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = DipuCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun PaymentToggleRow(
    title: String,
    subtitle: String,
    color: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = color, checkedTrackColor = color.copy(alpha = 0.4f))
        )
    }
}
