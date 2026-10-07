package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class BinanceSyncResult(
    val isSuccess: Boolean,
    val message: String,
    val serverTime: Long = 0L,
    val accountType: String? = null,
    val canTrade: Boolean = false,
    val usdtBalance: Double = 0.0,
    val newTransactionsFound: List<BinanceOrderRecord> = emptyList()
)

data class BinanceOrderRecord(
    val orderId: String,
    val amount: Double,
    val currency: String = "USDT",
    val status: String = "PAID",
    val time: Long = System.currentTimeMillis()
)

class BinanceService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun pingBinance(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val request = Request.Builder()
                .url("https://api.binance.com/api/v3/ping")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                if (response.isSuccessful) {
                    Result.success(latency)
                } else {
                    Result.failure(Exception("Binance ping HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServerTime(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.binance.com/api/v3/time")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    Result.success(json.getLong("serverTime"))
                } else {
                    Result.failure(Exception("Could not fetch server time: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testAndSyncAccount(apiKey: String, apiSecret: String): BinanceSyncResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            return@withContext BinanceSyncResult(
                isSuccess = false,
                message = "Binance API Key and Secret are required"
            )
        }

        try {
            val timeResult = getServerTime()
            val timestamp = timeResult.getOrDefault(System.currentTimeMillis())
            val queryString = "timestamp=$timestamp&recvWindow=60000"
            val signature = HmacSha256Util.sign(queryString, apiSecret)

            val url = "https://api.binance.com/api/v3/account?$queryString&signature=$signature"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-MBX-APIKEY", apiKey)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val canTrade = json.optBoolean("canTrade", true)
                    val accountType = json.optString("accountType", "SPOT")
                    
                    var usdt = 0.0
                    val balances = json.optJSONArray("balances")
                    if (balances != null) {
                        for (i in 0 until balances.length()) {
                            val b = balances.getJSONObject(i)
                            if (b.optString("asset") == "USDT") {
                                usdt = b.optString("free", "0.0").toDoubleOrNull() ?: 0.0
                                break
                            }
                        }
                    }

                    BinanceSyncResult(
                        isSuccess = true,
                        message = "Connected to Binance ($accountType). Balance: $usdt USDT",
                        serverTime = timestamp,
                        accountType = accountType,
                        canTrade = canTrade,
                        usdtBalance = usdt
                    )
                } else {
                    val err = try {
                        val errObj = JSONObject(body)
                        errObj.optString("msg", "HTTP ${response.code}")
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $body"
                    }
                    BinanceSyncResult(
                        isSuccess = false,
                        message = "Binance API Error: $err"
                    )
                }
            }
        } catch (e: Exception) {
            BinanceSyncResult(
                isSuccess = false,
                message = "Network error: ${e.localizedMessage ?: "Failed to reach Binance"}"
            )
        }
    }
}
