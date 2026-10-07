package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BinanceConfig
import com.example.data.model.ConnectedDevice
import com.example.data.model.DashboardStats
import com.example.data.model.ForwardingMode
import com.example.data.model.TelegramConfig
import com.example.data.model.TransactionEntity
import com.example.data.model.UserAccount
import com.example.data.repository.ProcessResult
import com.example.data.repository.RealtimeEvent
import com.example.data.repository.TransactionRepository
import com.example.sms.SmsParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class AuthUiState(
    val isLoggedIn: Boolean = true, // Default active session for quick access
    val user: UserAccount = UserAccount(),
    val isAuthenticating: Boolean = false,
    val authError: String? = null
)

class DipuViewModel(
    val repository: TransactionRepository
) : ViewModel() {

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    val telegramConfig: StateFlow<TelegramConfig> = repository.securityPrefs.telegramConfigFlow
    val binanceConfig: StateFlow<BinanceConfig> = repository.securityPrefs.binanceConfigFlow
    val deviceId: StateFlow<String> = repository.securityPrefs.deviceIdFlow

    // Search and filter state
    val searchQuery = MutableStateFlow("")
    val selectedProviderFilter = MutableStateFlow("ALL") // "ALL", "bKash", "Nagad", "Binance"
    val selectedDateFilter = MutableStateFlow("All") // "All", "Today", "Yesterday", "7 Days", "30 Days"

    // Filtered transactions flow
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        searchQuery,
        selectedProviderFilter,
        selectedDateFilter
    ) { list, query, providerFilter, dateFilter ->
        list.filter { tx ->
            val matchQuery = if (query.isBlank()) true else {
                tx.transactionId.contains(query, ignoreCase = true) ||
                        (tx.orderId?.contains(query, ignoreCase = true) == true) ||
                        (tx.senderInfo?.contains(query, ignoreCase = true) == true) ||
                        tx.amount.toString().contains(query)
            }

            val matchProvider = if (providerFilter == "ALL") true else {
                tx.provider.equals(providerFilter, ignoreCase = true)
            }

            val matchDate = matchesDateFilter(tx.detectedAt, dateFilter)

            matchQuery && matchProvider && matchDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard stats flow
    val dashboardStats: StateFlow<DashboardStats> = repository.getDashboardStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Audit logs flow
    val auditLogs = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Transaction for Detail Dialog
    private val _selectedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransaction: StateFlow<TransactionEntity?> = _selectedTransaction.asStateFlow()

    // Realtime UI notification messages (Toast/Banner)
    private val _uiEvents = MutableSharedFlow<String>(replay = 0)
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    // Connected devices (Two-Phone Sync State)
    private val _connectedDevices = MutableStateFlow(
        listOf(
            ConnectedDevice(
                id = repository.securityPrefs.getOrGenerateDeviceId(),
                name = "Primary Mobile Scanner (${repository.securityPrefs.getDeviceName()})",
                platform = "Android 15 (ARM64)",
                isCurrent = true,
                isActive = true,
                lastActive = "Live Now"
            ),
            ConnectedDevice(
                id = "DPZ-DEV-TERM2",
                name = "Secondary Station (Phone 2 Mirror)",
                platform = "Android 14 (ARM64)",
                isCurrent = false,
                isActive = true,
                lastActive = "Live (Sync Active)"
            )
        )
    )
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    // Status testing flags
    val isTestingTelegram = MutableStateFlow(false)
    val isTestingBinance = MutableStateFlow(false)

    init {
        // Collect realtime events from repository
        viewModelScope.launch {
            repository.realtimeEvents.collect { event ->
                when (event) {
                    is RealtimeEvent.NewTransaction -> {
                        _uiEvents.emit("⚡ New ${event.transaction.provider} payment: ${event.transaction.transactionId}")
                    }
                    is RealtimeEvent.TransactionDuplicate -> {
                        _uiEvents.emit("ℹ️ Duplicate ignored: ${event.trxId}")
                    }
                    is RealtimeEvent.TelegramStatusChanged -> {
                        _uiEvents.emit(if (event.isConnected) "🟢 Telegram Connected" else "🔴 Telegram Issue: ${event.message}")
                    }
                    is RealtimeEvent.BinanceStatusChanged -> {
                        _uiEvents.emit(if (event.isConnected) "🟢 Binance Synced" else "🔴 Binance: ${event.message}")
                    }
                    is RealtimeEvent.DeviceSyncCompleted -> {
                        _uiEvents.emit("🔄 Synchronized across ${event.deviceCount} mobile devices")
                    }
                }
            }
        }
    }

    fun selectTransaction(tx: TransactionEntity?) {
        _selectedTransaction.value = tx
    }

    // Auth actions
    fun login(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onResult(false, "Please provide email and password")
            return
        }
        _authUiState.value = _authUiState.value.copy(
            isLoggedIn = true,
            user = UserAccount(email = email)
        )
        onResult(true, "Logged in successfully")
    }

    fun register(name: String, email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            onResult(false, "Please fill in all fields")
            return
        }
        _authUiState.value = _authUiState.value.copy(
            isLoggedIn = true,
            user = UserAccount(name = name, email = email)
        )
        onResult(true, "Account created successfully")
    }

    fun logout() {
        _authUiState.value = _authUiState.value.copy(isLoggedIn = false)
    }

    // Telegram Configuration
    fun saveTelegramConfig(token: String, groupId: String, mode: ForwardingMode, enabled: Boolean) {
        val current = telegramConfig.value
        val updated = current.copy(
            botToken = token.trim(),
            groupId = groupId.trim(),
            forwardingMode = mode,
            isEnabled = enabled
        )
        repository.securityPrefs.saveTelegramConfig(updated)
        viewModelScope.launch {
            _uiEvents.emit("✅ Telegram configuration saved")
        }
    }

    fun testTelegramConnection(onResult: (Boolean, String) -> Unit) {
        val token = telegramConfig.value.botToken
        if (token.isBlank()) {
            onResult(false, "Bot token cannot be empty")
            return
        }
        viewModelScope.launch {
            isTestingTelegram.value = true
            val res = repository.telegramService.testBotToken(token)
            isTestingTelegram.value = false
            if (res.isSuccess) {
                val botInfo = res.getOrNull().orEmpty()
                repository.securityPrefs.saveTelegramConfig(telegramConfig.value.copy(isConnected = true))
                onResult(true, "Bot connected: $botInfo")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to connect to Telegram")
            }
        }
    }

    fun sendTelegramTestMessage(onResult: (Boolean, String) -> Unit) {
        val token = telegramConfig.value.botToken
        val groupId = telegramConfig.value.groupId
        if (token.isBlank() || groupId.isBlank()) {
            onResult(false, "Configure Bot Token and Group ID first")
            return
        }
        viewModelScope.launch {
            isTestingTelegram.value = true
            val testMsg = """
                ━━━━━━━━━━━━━━
                🚀 CONNECTION TEST
                ━━━━━━━━━━━━━━
                
                Device: ${repository.securityPrefs.getDeviceName()}
                Status: 🟢 Operational
                Time: ${Calendar.getInstance().time}
                
                ━━━━━━━━━━━━━━
                𝙳𝚒𝚙𝚞 𝙿𝚛𝚘𝚡𝚢 𝚉𝚘𝚗𝚎
                ━━━━━━━━━━━━━━
            """.trimIndent()

            val res = repository.telegramService.sendMessage(token, groupId, testMsg)
            isTestingTelegram.value = false
            if (res.isSuccess) {
                onResult(true, "Test message delivered to group $groupId")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Delivery failed")
            }
        }
    }

    // Binance Configuration
    fun saveBinanceConfig(key: String, secret: String) {
        val updated = binanceConfig.value.copy(
            apiKey = key.trim(),
            apiSecret = secret.trim()
        )
        repository.securityPrefs.saveBinanceConfig(updated)
        viewModelScope.launch {
            _uiEvents.emit("✅ Binance API credentials saved securely")
        }
    }

    fun syncBinance(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isTestingBinance.value = true
            val res = repository.syncBinanceNow()
            isTestingBinance.value = false
            if (res.isSuccess) {
                onResult(true, res.getOrNull().orEmpty())
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    fun disconnectBinance() {
        val updated = BinanceConfig()
        repository.securityPrefs.saveBinanceConfig(updated)
        viewModelScope.launch {
            _uiEvents.emit("Binance disconnected")
        }
    }

    // Manual TRX -> Group Add
    fun manualSendTrx(provider: String, trxId: String, amount: Double?, onResult: (Boolean, String) -> Unit) {
        if (trxId.isBlank()) {
            onResult(false, "Transaction ID / Order ID cannot be empty")
            return
        }
        viewModelScope.launch {
            val res = repository.manualSendToTelegram(provider, trxId, amount)
            if (res.isSuccess) {
                onResult(true, res.getOrNull().orEmpty())
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to deliver")
            }
        }
    }

    // Resend Existing Transaction
    fun resendTransaction(tx: TransactionEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.resendExistingTransactionToTelegram(tx)
            if (res.isSuccess) {
                onResult(true, "Resent to Telegram group successfully")
                _selectedTransaction.value = _selectedTransaction.value?.copy(telegramStatus = TransactionEntity.TELEGRAM_DELIVERED)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Resend failed")
            }
        }
    }

    fun markTransactionReviewed(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.markAsReviewed(tx)
            _selectedTransaction.value = _selectedTransaction.value?.copy(isReviewed = true)
            _uiEvents.emit("Marked as reviewed")
        }
    }

    // Simulator for Admin/Testing
    fun simulateSmsInput(sender: String, messageText: String, onResult: (Boolean, String) -> Unit) {
        val parsed = SmsParser.parse(sender, messageText)
        if (parsed == null) {
            onResult(false, "SMS format not recognized as bKash or Nagad payment")
            return
        }
        viewModelScope.launch {
            val res = repository.processIncomingTransaction(
                provider = parsed.provider,
                amount = parsed.amount,
                currency = "BDT",
                transactionId = parsed.transactionId,
                senderInfo = parsed.senderInfo,
                rawMessage = messageText,
                source = TransactionEntity.SOURCE_SMS
            )
            when (res) {
                is ProcessResult.Success -> onResult(true, "✅ Detected ${parsed.provider} payment! TrxID: ${parsed.transactionId}")
                is ProcessResult.Duplicate -> onResult(false, "ℹ️ Duplicate TrxID: ${parsed.transactionId} was detected and ignored")
                is ProcessResult.Error -> onResult(false, res.reason)
            }
        }
    }

    fun unlinkDevice(device: ConnectedDevice) {
        _connectedDevices.value = _connectedDevices.value.filter { it.id != device.id }
        viewModelScope.launch {
            _uiEvents.emit("Unlinked device ${device.name}")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiEvents.emit("All database records cleared")
        }
    }

    private fun matchesDateFilter(timestamp: Long, filter: String): Boolean {
        if (filter == "All") return true
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        return when (filter) {
            "Today" -> timestamp >= todayStart
            "Yesterday" -> {
                val yesterdayStart = todayStart - (24 * 60 * 60 * 1000L)
                timestamp in yesterdayStart until todayStart
            }
            "7 Days" -> timestamp >= (now - 7L * 24 * 60 * 60 * 1000L)
            "30 Days" -> timestamp >= (now - 30L * 24 * 60 * 60 * 1000L)
            else -> true
        }
    }
}

class DipuViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DipuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DipuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
