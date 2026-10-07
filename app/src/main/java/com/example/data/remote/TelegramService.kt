package com.example.data.remote

import android.util.Log
import com.example.data.model.ForwardingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TelegramService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun testBotToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bot Token cannot be empty"))
        }
        try {
            val url = "https://api.telegram.org/bot$token/getMe"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    if (json.optBoolean("ok")) {
                        val result = json.getJSONObject("result")
                        val username = result.optString("username", "UnknownBot")
                        val firstName = result.optString("first_name", "Telegram Bot")
                        Result.success("@$username ($firstName)")
                    } else {
                        Result.failure(Exception(json.optString("description", "Bot token verification failed")))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        botToken: String,
        groupId: String,
        text: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (botToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Telegram Bot Token is not configured"))
        }
        if (groupId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Telegram Group ID is not configured"))
        }
        try {
            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val formBody = FormBody.Builder()
                .add("chat_id", groupId)
                .add("text", text)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    if (json.optBoolean("ok")) {
                        Result.success("Message delivered successfully to group $groupId")
                    } else {
                        Result.failure(Exception(json.optString("description", "Telegram rejected message")))
                    }
                } else {
                    Result.failure(Exception("Telegram API Error (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatTransactionMessage(
        mode: ForwardingMode,
        provider: String,
        amount: Double,
        currency: String,
        trxId: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        return if (mode == ForwardingMode.ID_ONLY) {
            trxId
        } else {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(Date(timestamp))
            val symbol = if (currency == "BDT") "৳" else "$"
            val icon = when (provider.lowercase()) {
                "bkash" -> "🟢"
                "nagad" -> "🟠"
                else -> "🔵"
            }
            val idLabel = if (provider.equals("binance", ignoreCase = true)) "OrderID" else "TrxID"

            buildString {
                appendLine("━━━━━━━━━━━━━━")
                appendLine("$icon NEW PAYMENT")
                appendLine("━━━━━━━━━━━━━━")
                appendLine()
                appendLine("💳 Method: $provider")
                appendLine("💰 Amount: $symbol$amount ${if (currency != "BDT") currency else ""}".trim())
                appendLine("🆔 $idLabel: $trxId")
                appendLine("🕐 Time: $formattedTime")
                appendLine()
                appendLine("━━━━━━━━━━━━━━")
                appendLine("𝙳𝚒𝚙𝚞 𝙿𝚛𝚘𝚡𝚢 𝚉𝚘𝚗𝚎")
                append("━━━━━━━━━━━━━━")
            }
        }
    }
}
