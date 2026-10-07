package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["provider", "transactionId"], unique = true),
        Index(value = ["detectedAt"]),
        Index(value = ["provider"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "user_dipu_01",
    val provider: String, // "bKash", "Nagad", "Binance"
    val transactionId: String, // TrxID or OrderId
    val orderId: String? = null,
    val amount: Double,
    val currency: String = "BDT", // "BDT" or "USDT"
    val senderInfo: String? = null,
    val rawMessage: String? = null,
    val status: String = STATUS_PROCESSED, // "PROCESSED", "DUPLICATE", "FAILED"
    val source: String = SOURCE_SMS, // "SMS", "BINANCE_API", "MANUAL_ADD"
    val detectedAt: Long = System.currentTimeMillis(),
    val processedAt: Long = System.currentTimeMillis(),
    val telegramStatus: String = TELEGRAM_DELIVERED, // "DELIVERED", "FAILED", "PENDING"
    val telegramResponse: String? = null,
    val deviceId: String = "Phone-1",
    val isReviewed: Boolean = false
) {
    companion object {
        const val PROVIDER_BKASH = "bKash"
        const val PROVIDER_NAGAD = "Nagad"
        const val PROVIDER_BINANCE = "Binance"

        const val STATUS_PROCESSED = "PROCESSED"
        const val STATUS_DUPLICATE = "DUPLICATE"
        const val STATUS_FAILED = "FAILED"

        const val SOURCE_SMS = "SMS"
        const val SOURCE_BINANCE_API = "BINANCE_API"
        const val SOURCE_MANUAL = "MANUAL_ADD"

        const val TELEGRAM_DELIVERED = "DELIVERED"
        const val TELEGRAM_FAILED = "FAILED"
        const val TELEGRAM_SKIPPED = "SKIPPED"
        const val TELEGRAM_PENDING = "PENDING"
    }
}
