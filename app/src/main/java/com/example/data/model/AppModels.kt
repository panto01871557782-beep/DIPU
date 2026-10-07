package com.example.data.model

enum class ForwardingMode {
    ID_ONLY,
    PAYMENT_DETAILS
}

data class TelegramConfig(
    val botToken: String = "",
    val groupId: String = "-1004323009837",
    val forwardingMode: ForwardingMode = ForwardingMode.PAYMENT_DETAILS,
    val isEnabled: Boolean = true,
    val isConnected: Boolean = false,
    val lastTestMessage: String? = null,
    val lastTestStatus: Boolean? = null
)

data class BinanceConfig(
    val apiKey: String = "",
    val apiSecret: String = "",
    val isConnected: Boolean = false,
    val lastSyncTime: Long = 0L,
    val lastSyncMessage: String? = null
)

data class UserAccount(
    val userId: String = "DPZ-89421",
    val name: String = "Dipu Proxy Admin",
    val email: String = "poiuytrewqasdfghjklmnbvc234@gmail.com",
    val createdAt: String = "2026-10-01",
    val role: String = "Super Administrator",
    val activeDevicesCount: Int = 2
)

data class ConnectedDevice(
    val id: String,
    val name: String,
    val platform: String = "Android 15 (ARM64)",
    val isCurrent: Boolean = false,
    val isActive: Boolean = true,
    val lastActive: String = "Just now"
)

data class SmsDetectionRule(
    val id: String,
    val provider: String,
    val name: String,
    val pattern: String,
    val isEnabled: Boolean = true,
    val example: String
)

data class DashboardStats(
    val totalReceivedBdt: Double = 0.0,
    val totalReceivedUsdt: Double = 0.0,
    val validSmsCount: Int = 0,
    val totalTransactions: Int = 0,
    val todayAmountBdt: Double = 0.0,
    val todayAmountUsdt: Double = 0.0,

    val bkashCount: Int = 0,
    val bkashTotal: Double = 0.0,
    val bkashLastTrx: String = "None",

    val nagadCount: Int = 0,
    val nagadTotal: Double = 0.0,
    val nagadLastTrx: String = "None",

    val binanceCount: Int = 0,
    val binanceTotal: Double = 0.0,
    val binanceLastTrx: String = "None"
)
