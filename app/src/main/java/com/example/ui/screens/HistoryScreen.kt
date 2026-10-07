package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.DipuTopBar
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BkashPink
import com.example.ui.theme.DipuBorder
import com.example.ui.theme.DipuCyan
import com.example.ui.theme.DipuDarkBg
import com.example.ui.theme.DipuSurfaceCard
import com.example.ui.theme.DipuSurfaceElevated
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DipuViewModel

@Composable
fun HistoryScreen(
    viewModel: DipuViewModel,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedProvider by viewModel.selectedProviderFilter.collectAsState()
    val selectedDate by viewModel.selectedDateFilter.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()
    val telegramConfig by viewModel.telegramConfig.collectAsState()
    val binanceConfig by viewModel.binanceConfig.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DipuDarkBg)
            .testTag("history_screen")
    ) {
        DipuTopBar(
            title = "Transaction Log",
            subtitle = "Audit Trail & Verification",
            deviceId = deviceId,
            isTelegramConnected = telegramConfig.isConnected,
            isBinanceConnected = binanceConfig.isConnected
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search Transaction ID / Order ID", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = DipuCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextMuted
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_trx_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DipuCyan,
                    unfocusedBorderColor = DipuBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Provider Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "bKash", "Nagad", "Binance").forEach { provider ->
                    val isSelected = selectedProvider.equals(provider, ignoreCase = true)
                    val activeColor = when (provider.lowercase()) {
                        "bkash" -> BkashPink
                        "nagad" -> NagadOrange
                        "binance" -> BinanceGold
                        else -> DipuCyan
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedProviderFilter.value = provider },
                        label = {
                            Text(
                                text = provider.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeColor.copy(alpha = 0.25f),
                            selectedLabelColor = activeColor,
                            containerColor = DipuSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = DipuBorder,
                            selectedBorderColor = activeColor
                        ),
                        modifier = Modifier.testTag("filter_provider_$provider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Date Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Today", "Yesterday", "7 Days", "30 Days").forEach { dateOpt ->
                    val isSelected = selectedDate == dateOpt
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedDateFilter.value = dateOpt },
                        label = {
                            Text(
                                text = dateOpt,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DipuCyan.copy(alpha = 0.2f),
                            selectedLabelColor = DipuCyan,
                            containerColor = DipuSurfaceElevated,
                            labelColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = DipuBorder,
                            selectedBorderColor = DipuCyan
                        ),
                        modifier = Modifier.testTag("filter_date_$dateOpt")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Transactions List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RECORD COUNT (${transactions.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .border(1.dp, DipuBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔍", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No records match criteria",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your search query, provider selection or date range.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
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
