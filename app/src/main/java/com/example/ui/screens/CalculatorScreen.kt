package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
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
fun CalculatorScreen(
    viewModel: DipuViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val telegramConfig by viewModel.telegramConfig.collectAsState()
    val binanceConfig by viewModel.binanceConfig.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DipuDarkBg)
            .testTag("calculator_screen")
    ) {
        DipuTopBar(
            title = "Financial Terminal",
            subtitle = "Transaction & Rate Utility",
            deviceId = deviceId,
            isTelegramConnected = telegramConfig.isConnected,
            isBinanceConnected = binanceConfig.isConnected
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DipuDarkBg,
            contentColor = DipuCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = DipuCyan
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Standard Calculator", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Payment Metrics", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            StandardArithmeticCalculator()
        } else {
            PaymentMetricsCalculator(
                autoTotal = stats.totalReceivedBdt,
                autoCount = stats.totalTransactions
            )
        }
    }
}

@Composable
fun StandardArithmeticCalculator() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (expression.isEmpty()) "0" else expression,
                    fontSize = 18.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = DipuCyan,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad Grid
        val buttons = listOf(
            listOf("AC", "DEL", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=", "")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { btn ->
                    if (btn.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val isOp = btn in listOf("÷", "×", "-", "+", "=")
                        val isSpecial = btn in listOf("AC", "DEL", "%")
                        val isEquals = btn == "="

                        val bgColor = when {
                            isEquals -> DipuCyan
                            isOp -> DipuSurfaceElevated
                            isSpecial -> DipuSurfaceElevated.copy(alpha = 0.7f)
                            else -> DipuSurfaceCard
                        }

                        val textColor = when {
                            isEquals -> Color.Black
                            isOp -> DipuCyan
                            isSpecial -> NagadOrange
                            else -> TextPrimary
                        }

                        Box(
                            modifier = Modifier
                                .weight(if (btn == "0") 2f else 1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(1.dp, DipuBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    when (btn) {
                                        "AC" -> {
                                            expression = ""
                                            result = "0"
                                        }
                                        "DEL" -> {
                                            if (expression.isNotEmpty()) {
                                                expression = expression.dropLast(1)
                                            }
                                        }
                                        "=" -> {
                                            result = evaluateSimpleExpression(expression)
                                        }
                                        "%" -> {
                                            val num = result.toDoubleOrNull() ?: 0.0
                                            result = (num / 100.0).toString()
                                            expression = result
                                        }
                                        else -> {
                                            expression += btn
                                        }
                                    }
                                }
                                .testTag("calc_btn_$btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (btn == "DEL") {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = btn,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun PaymentMetricsCalculator(
    autoTotal: Double,
    autoCount: Int
) {
    var totalAmountInput by remember { mutableStateOf(autoTotal.toString()) }
    var txCountInput by remember { mutableStateOf(autoCount.toString()) }

    val total = totalAmountInput.toDoubleOrNull() ?: 0.0
    val count = txCountInput.toIntOrNull() ?: 1
    val average = if (count > 0) total / count else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                Text(
                    text = "AVERAGE TRANSACTION ANALYZER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DipuCyan,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = totalAmountInput,
                    onValueChange = { totalAmountInput = it },
                    label = { Text("Total Received Amount (৳)", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_total_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DipuCyan,
                        unfocusedBorderColor = DipuBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = txCountInput,
                    onValueChange = { txCountInput = it },
                    label = { Text("Total Number of Transactions", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_count_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DipuCyan,
                        unfocusedBorderColor = DipuBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DipuCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DipuSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "COMPUTED METRICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                MetricItem(label = "Total Volume", value = "৳ %,.2f".format(total))
                MetricItem(label = "Transaction Count", value = "$count transactions")
                MetricItem(
                    label = "Average Per Transaction",
                    value = "৳ %,.2f".format(average),
                    isHighlight = true
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MetricItem(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = if (isHighlight) 18.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) DipuCyan else TextPrimary
        )
    }
}

private fun evaluateSimpleExpression(expr: String): String {
    if (expr.isBlank()) return "0"
    return try {
        val clean = expr.replace("×", "*").replace("÷", "/")
        // Simple 2-operand or multi-operand evaluation
        val parts = clean.split(Regex("(?<=[-+*/])|(?=[-+*/])"))
        var total = parts.firstOrNull()?.toDoubleOrNull() ?: 0.0
        var i = 1
        while (i < parts.size - 1) {
            val op = parts[i]
            val next = parts[i + 1].toDoubleOrNull() ?: 0.0
            when (op) {
                "+" -> total += next
                "-" -> total -= next
                "*" -> total *= next
                "/" -> if (next != 0.0) total /= next
            }
            i += 2
        }
        if (total % 1.0 == 0.0) {
            total.toLong().toString()
        } else {
            "%.2f".format(total)
        }
    } catch (e: Exception) {
        "Error"
    }
}
