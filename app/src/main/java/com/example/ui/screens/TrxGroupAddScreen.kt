package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DipuViewModel

@Composable
fun TrxGroupAddScreen(
    viewModel: DipuViewModel
) {
    var selectedProvider by remember { mutableStateOf("bKash") }
    var trxIdInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var lastSentTrx by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val telegramConfig by viewModel.telegramConfig.collectAsState()
    val binanceConfig by viewModel.binanceConfig.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()

    val isBinance = selectedProvider.equals("Binance", ignoreCase = true)
    val idLabel = if (isBinance) "Order ID" else "Transaction ID"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DipuDarkBg)
            .testTag("trx_group_add_screen")
    ) {
        DipuTopBar(
            title = "TRX → GROUP ADD",
            subtitle = "Direct Telegram Dispatcher",
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DipuBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "DISPATCH TRX TO TELEGRAM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DipuCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Provider Dropdown
                    Text(
                        text = "Payment Method",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DipuSurfaceElevated)
                            .border(1.dp, DipuBorder, RoundedCornerShape(12.dp))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                            .testTag("provider_dropdown_selector")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val providerColor = when (selectedProvider.lowercase()) {
                                "bkash" -> BkashPink
                                "nagad" -> NagadOrange
                                else -> BinanceGold
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(providerColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedProvider,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(DipuSurfaceElevated)
                        ) {
                            listOf("bKash", "Nagad", "Binance").forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov, color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        selectedProvider = prov
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Transaction ID / Order ID input
                    Text(
                        text = idLabel,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = trxIdInput,
                        onValueChange = { trxIdInput = it.uppercase() },
                        placeholder = { Text("e.g. BL99K3X8A1", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trx_id_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DipuCyan,
                            unfocusedBorderColor = DipuBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount input (optional)
                    Text(
                        text = if (isBinance) "Amount (USDT) - Optional" else "Amount (BDT) - Optional",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        placeholder = { Text("e.g. 500", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DipuCyan,
                            unfocusedBorderColor = DipuBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (trxIdInput.isBlank()) return@Button
                            isSending = true
                            resultMessage = null
                            val amt = amountInput.toDoubleOrNull()
                            viewModel.manualSendTrx(selectedProvider, trxIdInput, amt) { ok, msg ->
                                isSending = false
                                isSuccess = ok
                                resultMessage = msg
                                if (ok) {
                                    lastSentTrx = trxIdInput
                                    trxIdInput = ""
                                    amountInput = ""
                                }
                            }
                        },
                        enabled = !isSending && trxIdInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_to_group_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DipuCyan, contentColor = Color(0xFF002227))
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🚀 SEND TO GROUP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Configured Target: Group ID ${telegramConfig.groupId}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Status Card on Result
            if (resultMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isSuccess) StatusSuccess.copy(alpha = 0.5f) else StatusError.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DipuSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) StatusSuccess else StatusError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSuccess) "✅ Transaction ID sent successfully" else "❌ Dispatch Failed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }

                        if (lastSentTrx != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Transaction ID: $lastSentTrx",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DipuCyan
                            )
                            Text(
                                text = "Telegram: ✅ Delivered to ${telegramConfig.groupId}",
                                fontSize = 12.sp,
                                color = StatusSuccess
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resultMessage.orEmpty(),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
