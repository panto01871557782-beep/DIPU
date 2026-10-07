package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.model.AuditLogEntity
import com.example.data.model.DashboardStats
import com.example.data.model.ForwardingMode
import com.example.data.model.TransactionEntity
import com.example.data.remote.BinanceOrderRecord
import com.example.data.remote.BinanceService
import com.example.data.remote.TelegramService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ProcessResult {
    data class Success(val transaction: TransactionEntity, val telegramDelivered: Boolean, val message: String) : ProcessResult()
    data class Duplicate(val provider: String, val transactionId: String, val existing: TransactionEntity) : ProcessResult()
    data class Error(val reason: String) : ProcessResult()
}

sealed class RealtimeEvent {
    data class NewTransaction(val transaction: TransactionEntity) : RealtimeEvent()
    data class TransactionDuplicate(val provider: String, val trxId: String) : RealtimeEvent()
    data class TelegramStatusChanged(val isConnected: Boolean, val message: String) : RealtimeEvent()
    data class BinanceStatusChanged(val isConnected: Boolean, val message: String) : RealtimeEvent()
    data class DeviceSyncCompleted(val deviceCount: Int, val syncedItems: Int) : RealtimeEvent()
}

class TransactionRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    val securityPrefs: SecurityPreferences = SecurityPreferences(context),
    val telegramService: TelegramService = TelegramService(),
    val binanceService: BinanceService = BinanceService()
) {
    private val transactionDao = database.transactionDao()
    private val auditLogDao = database.auditLogDao()

    private val _realtimeEvents = MutableSharedFlow<RealtimeEvent>(replay = 1)
    val realtimeEvents: SharedFlow<RealtimeEvent> = _realtimeEvents.asSharedFlow()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allAuditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()

    suspend fun processIncomingTransaction(
        provider: String,
        amount: Double,
        currency: String = "BDT",
        transactionId: String,
        senderInfo: String? = null,
        rawMessage: String? = null,
        source: String = TransactionEntity.SOURCE_SMS,
        orderId: String? = null
    ): ProcessResult = withContext(Dispatchers.IO) {
        val cleanTrxId = transactionId.trim()
        val deviceId = securityPrefs.getOrGenerateDeviceId()

        // 1. Strict Duplicate Protection Check
        val existing = transactionDao.findByProviderAndTrxId(provider, cleanTrxId)
        if (existing != null) {
            // Transaction already exists! Never forward duplicate to Telegram.
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "TRANSACTION_DUPLICATE",
                    details = "Duplicate $provider ID: $cleanTrxId detected and ignored. Orig date: ${existing.detectedAt}",
                    deviceId = deviceId
                )
            )
            _realtimeEvents.emit(RealtimeEvent.TransactionDuplicate(provider, cleanTrxId))
            return@withContext ProcessResult.Duplicate(provider, cleanTrxId, existing)
        }

        // 2. Prepare initial transaction entity
        val initialEntity = TransactionEntity(
            provider = provider,
            transactionId = cleanTrxId,
            orderId = orderId,
            amount = amount,
            currency = currency,
            senderInfo = senderInfo,
            rawMessage = rawMessage,
            status = TransactionEntity.STATUS_PROCESSED,
            source = source,
            detectedAt = System.currentTimeMillis(),
            processedAt = System.currentTimeMillis(),
            telegramStatus = TransactionEntity.TELEGRAM_PENDING,
            deviceId = deviceId
        )

        val insertedId = transactionDao.insertTransaction(initialEntity)

        // 3. Telegram Forwarding (if configured and enabled)
        val telegramConfig = securityPrefs.telegramConfigFlow.value
        var telegramDelivered = false
        var telegramResponseMsg = "Telegram not configured"

        if (telegramConfig.isEnabled && telegramConfig.botToken.isNotBlank() && telegramConfig.groupId.isNotBlank()) {
            val formattedMsg = telegramService.formatTransactionMessage(
                mode = telegramConfig.forwardingMode,
                provider = provider,
                amount = amount,
                currency = currency,
                trxId = cleanTrxId,
                timestamp = initialEntity.detectedAt
            )

            val tgResult = telegramService.sendMessage(
                botToken = telegramConfig.botToken,
                groupId = telegramConfig.groupId,
                text = formattedMsg
            )

            if (tgResult.isSuccess) {
                telegramDelivered = true
                telegramResponseMsg = tgResult.getOrNull() ?: "Delivered"
                auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "TELEGRAM_FORWARDED",
                        details = "$provider ($cleanTrxId) forwarded to group ${telegramConfig.groupId}",
                        deviceId = deviceId
                    )
                )
            } else {
                telegramDelivered = false
                telegramResponseMsg = tgResult.exceptionOrNull()?.message ?: "Failed"
                auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "TELEGRAM_FAILED",
                        details = "Failed forwarding $cleanTrxId: $telegramResponseMsg",
                        deviceId = deviceId
                    )
                )
            }
        }

        // Update transaction with final Telegram status
        val finalEntity = initialEntity.copy(
            id = insertedId,
            telegramStatus = if (telegramDelivered) TransactionEntity.TELEGRAM_DELIVERED else if (telegramConfig.botToken.isBlank()) TransactionEntity.TELEGRAM_SKIPPED else TransactionEntity.TELEGRAM_FAILED,
            telegramResponse = telegramResponseMsg
        )
        transactionDao.updateTransaction(finalEntity)

        // Log audit
        auditLogDao.insertLog(
            AuditLogEntity(
                action = "NEW_TRANSACTION",
                details = "New $provider TRX $cleanTrxId amount $amount $currency via $source",
                deviceId = deviceId
            )
        )

        // Broadcast to Realtime Event bus (synchronizing UI and other device listeners)
        _realtimeEvents.emit(RealtimeEvent.NewTransaction(finalEntity))

        ProcessResult.Success(
            transaction = finalEntity,
            telegramDelivered = telegramDelivered,
            message = if (telegramDelivered) "Processed & Sent to Telegram" else "Processed locally ($telegramResponseMsg)"
        )
    }

    suspend fun manualSendToTelegram(
        provider: String,
        transactionId: String,
        amount: Double?
    ): Result<String> = withContext(Dispatchers.IO) {
        val telegramConfig = securityPrefs.telegramConfigFlow.value
        if (telegramConfig.botToken.isBlank() || telegramConfig.groupId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Telegram Bot Token and Group ID must be configured in Settings."))
        }

        val currency = if (provider.equals("binance", ignoreCase = true)) "USDT" else "BDT"
        val amt = amount ?: 0.0

        // Forward to Telegram
        val formattedMsg = telegramService.formatTransactionMessage(
            mode = telegramConfig.forwardingMode,
            provider = provider,
            amount = amt,
            currency = currency,
            trxId = transactionId.trim(),
            timestamp = System.currentTimeMillis()
        )

        val sendResult = telegramService.sendMessage(
            botToken = telegramConfig.botToken,
            groupId = telegramConfig.groupId,
            text = formattedMsg
        )

        if (sendResult.isSuccess) {
            // Also store in DB if not duplicate
            processIncomingTransaction(
                provider = provider,
                amount = amt,
                currency = currency,
                transactionId = transactionId.trim(),
                senderInfo = "Manual Send",
                source = TransactionEntity.SOURCE_MANUAL
            )
            Result.success("Delivered to Telegram group ${telegramConfig.groupId}")
        } else {
            Result.failure(sendResult.exceptionOrNull() ?: Exception("Unknown Telegram error"))
        }
    }

    suspend fun resendExistingTransactionToTelegram(transaction: TransactionEntity): Result<String> = withContext(Dispatchers.IO) {
        val telegramConfig = securityPrefs.telegramConfigFlow.value
        if (telegramConfig.botToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Telegram bot token not configured"))
        }

        val formattedMsg = telegramService.formatTransactionMessage(
            mode = telegramConfig.forwardingMode,
            provider = transaction.provider,
            amount = transaction.amount,
            currency = transaction.currency,
            trxId = transaction.transactionId,
            timestamp = transaction.detectedAt
        )

        val result = telegramService.sendMessage(
            botToken = telegramConfig.botToken,
            groupId = telegramConfig.groupId,
            text = formattedMsg
        )

        if (result.isSuccess) {
            val updated = transaction.copy(telegramStatus = TransactionEntity.TELEGRAM_DELIVERED, telegramResponse = "Manual Resend Success")
            transactionDao.updateTransaction(updated)
            Result.success("Resent to Telegram group successfully")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to resend"))
        }
    }

    suspend fun markAsReviewed(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        val updated = transaction.copy(isReviewed = true)
        transactionDao.updateTransaction(updated)
    }

    fun getDashboardStats(): Flow<DashboardStats> {
        return allTransactions.map { list ->
            var totalBdt = 0.0
            var totalUsdt = 0.0
            var validSms = 0
            var todayBdt = 0.0
            var todayUsdt = 0.0

            var bkCount = 0
            var bkTotal = 0.0
            var bkLast = "None"

            var ngCount = 0
            var ngTotal = 0.0
            var ngLast = "None"

            var bnCount = 0
            var bnTotal = 0.0
            var bnLast = "None"

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis

            for (tx in list) {
                if (tx.currency == "USDT" || tx.provider.equals("binance", ignoreCase = true)) {
                    totalUsdt += tx.amount
                    if (tx.detectedAt >= startOfDay) todayUsdt += tx.amount
                } else {
                    totalBdt += tx.amount
                    if (tx.detectedAt >= startOfDay) todayBdt += tx.amount
                }

                if (tx.source == TransactionEntity.SOURCE_SMS) {
                    validSms++
                }

                when (tx.provider.lowercase()) {
                    "bkash" -> {
                        bkCount++
                        bkTotal += tx.amount
                        if (bkLast == "None") bkLast = tx.transactionId
                    }
                    "nagad" -> {
                        ngCount++
                        ngTotal += tx.amount
                        if (ngLast == "None") ngLast = tx.transactionId
                    }
                    "binance" -> {
                        bnCount++
                        bnTotal += tx.amount
                        if (bnLast == "None") bnLast = tx.transactionId
                    }
                }
            }

            DashboardStats(
                totalReceivedBdt = totalBdt,
                totalReceivedUsdt = totalUsdt,
                validSmsCount = validSms,
                totalTransactions = list.size,
                todayAmountBdt = todayBdt,
                todayAmountUsdt = todayUsdt,
                bkashCount = bkCount,
                bkashTotal = bkTotal,
                bkashLastTrx = bkLast,
                nagadCount = ngCount,
                nagadTotal = ngTotal,
                nagadLastTrx = ngLast,
                binanceCount = bnCount,
                binanceTotal = bnTotal,
                binanceLastTrx = bnLast
            )
        }
    }

    suspend fun syncBinanceNow(): Result<String> = withContext(Dispatchers.IO) {
        val config = securityPrefs.binanceConfigFlow.value
        if (config.apiKey.isBlank() || config.apiSecret.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Binance API credentials missing. Please set them in Settings."))
        }

        val syncResult = binanceService.testAndSyncAccount(config.apiKey, config.apiSecret)
        if (syncResult.isSuccess) {
            val updated = config.copy(
                isConnected = true,
                lastSyncTime = System.currentTimeMillis(),
                lastSyncMessage = syncResult.message
            )
            securityPrefs.saveBinanceConfig(updated)
            _realtimeEvents.emit(RealtimeEvent.BinanceStatusChanged(true, syncResult.message))
            Result.success(syncResult.message)
        } else {
            _realtimeEvents.emit(RealtimeEvent.BinanceStatusChanged(false, syncResult.message))
            Result.failure(Exception(syncResult.message))
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        transactionDao.clearAll()
        auditLogDao.clearLogs()
    }
}
