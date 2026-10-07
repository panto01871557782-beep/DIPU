package com.example.sms

import java.util.regex.Pattern

data class ParsedPaymentSms(
    val provider: String,
    val amount: Double,
    val transactionId: String,
    val senderInfo: String,
    val rawMessage: String,
    val isValid: Boolean = true
)

object SmsParser {

    // bKash Regex Patterns
    private val BKASH_RECEIVED_PATTERN = Pattern.compile(
        "You have received Tk\\s*([0-9,.]+)\\s*from\\s*([0-9A-Za-z+*]+).*?TrxID\\s*([A-Za-z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )
    private val BKASH_CASHIN_PATTERN = Pattern.compile(
        "Cash In Tk\\s*([0-9,.]+)\\s*from\\s*([0-9A-Za-z+*]+).*?TrxID\\s*([A-Za-z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )
    private val BKASH_PAYMENT_PATTERN = Pattern.compile(
        "Payment Tk\\s*([0-9,.]+)\\s*to\\s*([0-9A-Za-z+*]+).*?TrxID\\s*([A-Za-z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )
    private val BKASH_FALLBACK_PATTERN = Pattern.compile(
        "(?:Tk|BDT)\\s*([0-9,.]+).*?TrxID[:\\s]+([A-Za-z0-9]{8,15})",
        Pattern.CASE_INSENSITIVE
    )

    // Nagad Regex Patterns
    private val NAGAD_CASHIN_PATTERN = Pattern.compile(
        "Cash In Tk\\s*([0-9,.]+)\\s*from\\s*([0-9A-Za-z+*]+).*?TxnID[:\\s]+([A-Za-z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )
    private val NAGAD_RECEIVED_PATTERN = Pattern.compile(
        "Received Tk\\s*([0-9,.]+)\\s*from\\s*([0-9A-Za-z+*]+).*?TxnID[:\\s]+([A-Za-z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )
    private val NAGAD_PAYMENT_PATTERN = Pattern.compile(
        "Payment Tk\\s*([0-9,.]+)\\s*to\\s*([0-9A-Za-z+*]+).*?TxnID[:\\s]+([A-Za-z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )
    private val NAGAD_FALLBACK_PATTERN = Pattern.compile(
        "(?:Tk|BDT)\\s*([0-9,.]+).*?TxnID[:\\s]+([A-Za-z0-9]{7,15})",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(sender: String?, body: String?): ParsedPaymentSms? {
        if (body.isNullOrBlank()) return null
        val cleanSender = sender?.trim().orEmpty().lowercase()
        val text = body.trim()

        // Filter out unrelated OTP, personal messages, or spam
        if (isIgnoredSms(text)) {
            return null
        }

        // Try bKash parsing if sender matches or message content clearly identifies bKash
        val isBkashCandidate = cleanSender.contains("bkash") ||
                cleanSender.contains("16247") ||
                text.contains("bKash", ignoreCase = true) ||
                (text.contains("TrxID", ignoreCase = true) && !text.contains("Nagad", ignoreCase = true))

        if (isBkashCandidate) {
            parseBkash(text)?.let { return it }
        }

        // Try Nagad parsing if sender matches or message content clearly identifies Nagad
        val isNagadCandidate = cleanSender.contains("nagad") ||
                cleanSender.contains("16167") ||
                text.contains("Nagad", ignoreCase = true) ||
                text.contains("TxnID", ignoreCase = true)

        if (isNagadCandidate) {
            parseNagad(text)?.let { return it }
        }

        return null
    }

    private fun isIgnoredSms(body: String): Boolean {
        val lower = body.lowercase()
        // If message contains OTP/verification and no TrxID, it's strictly authentication
        if ((lower.contains("verification code") || lower.contains("security code") || lower.contains("otp is"))
            && !lower.contains("trxid") && !lower.contains("txnid")) {
            return true
        }
        // Ads and promotions without financial transaction id
        if (!lower.contains("trxid") && !lower.contains("txnid")) {
            return true
        }
        return false
    }

    private fun parseBkash(body: String): ParsedPaymentSms? {
        // Match Received
        var matcher = BKASH_RECEIVED_PATTERN.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1))
            val sender = matcher.group(2) ?: "Customer"
            val trxId = matcher.group(3) ?: ""
            if (isValidTrxId(trxId)) {
                return ParsedPaymentSms("bKash", amount, trxId, sender, body)
            }
        }

        // Match Cash In
        matcher = BKASH_CASHIN_PATTERN.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1))
            val sender = matcher.group(2) ?: "Agent"
            val trxId = matcher.group(3) ?: ""
            if (isValidTrxId(trxId)) {
                return ParsedPaymentSms("bKash", amount, trxId, sender, body)
            }
        }

        // Match Payment
        matcher = BKASH_PAYMENT_PATTERN.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1))
            val sender = matcher.group(2) ?: "Merchant"
            val trxId = matcher.group(3) ?: ""
            if (isValidTrxId(trxId)) {
                return ParsedPaymentSms("bKash", amount, trxId, sender, body)
            }
        }

        // Fallback
        matcher = BKASH_FALLBACK_PATTERN.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1))
            val trxId = matcher.group(2) ?: ""
            if (isValidTrxId(trxId)) {
                return ParsedPaymentSms("bKash", amount, trxId, "Direct", body)
            }
        }

        return null
    }

    private fun parseNagad(body: String): ParsedPaymentSms? {
        // Match Cash In
        var matcher = NAGAD_CASHIN_PATTERN.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1))
            val sender = matcher.group(2) ?: "Agent"
            val trxId = matcher.group(3) ?: ""
            if (isValidTrxId(trxId)) {
                return ParsedPaymentSms("Nagad", amount, trxId, sender, body)
            }
        }

        // Match Received
        matcher = NAGAD_RECEIVED_PATTERN.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1))
            val sender = matcher.group(2) ?: "Customer"
            val trxId = matcher.group(3) ?: ""
            if (isValidTrxId(trxId)) {
                return ParsedPaymentSms("Nagad", amount, trxId, sender, body)
            }
        }

        // Match Payment
        matcher = NAGAD_PAYMENT_PATTERN.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1))
            val sender = matcher.group(2) ?: "Merchant"
            val trxId = matcher.group(3) ?: ""
            if (isValidTrxId(trxId)) {
                return ParsedPaymentSms("Nagad", amount, trxId, sender, body)
            }
        }

        // Fallback
        matcher = NAGAD_FALLBACK_PATTERN.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1))
            val trxId = matcher.group(2) ?: ""
            if (isValidTrxId(trxId)) {
                return ParsedPaymentSms("Nagad", amount, trxId, "Direct", body)
            }
        }

        return null
    }

    private fun parseAmount(str: String?): Double {
        if (str.isNullOrBlank()) return 0.0
        val clean = str.replace(",", "").trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun isValidTrxId(id: String): Boolean {
        return id.isNotBlank() && id.length >= 6 && id.all { it.isLetterOrDigit() }
    }
}
