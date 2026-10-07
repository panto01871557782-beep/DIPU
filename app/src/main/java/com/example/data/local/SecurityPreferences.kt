package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.data.model.BinanceConfig
import com.example.data.model.ForwardingMode
import com.example.data.model.TelegramConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

class SecurityPreferences(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dipu_secure_prefs", Context.MODE_PRIVATE)

    private val _telegramConfigFlow = MutableStateFlow(loadTelegramConfig())
    val telegramConfigFlow: StateFlow<TelegramConfig> = _telegramConfigFlow.asStateFlow()

    private val _binanceConfigFlow = MutableStateFlow(loadBinanceConfig())
    val binanceConfigFlow: StateFlow<BinanceConfig> = _binanceConfigFlow.asStateFlow()

    private val _deviceIdFlow = MutableStateFlow(getOrGenerateDeviceId())
    val deviceIdFlow: StateFlow<String> = _deviceIdFlow.asStateFlow()

    init {
        // Ensure device ID is initialized
        getOrGenerateDeviceId()
    }

    fun getOrGenerateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val newId = "DPZ-DEV-" + UUID.randomUUID().toString().take(6).uppercase()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    fun setDeviceName(name: String) {
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
    }

    fun getDeviceName(): String {
        return prefs.getString(KEY_DEVICE_NAME, "Mobile Terminal Alpha") ?: "Mobile Terminal Alpha"
    }

    private fun loadTelegramConfig(): TelegramConfig {
        val encToken = prefs.getString(KEY_TELEGRAM_TOKEN, "") ?: ""
        val token = decrypt(encToken)
        val groupId = prefs.getString(KEY_TELEGRAM_GROUP_ID, "-1004323009837") ?: "-1004323009837"
        val modeStr = prefs.getString(KEY_TELEGRAM_MODE, ForwardingMode.PAYMENT_DETAILS.name)
        val mode = try { ForwardingMode.valueOf(modeStr ?: ForwardingMode.PAYMENT_DETAILS.name) } catch (e: Exception) { ForwardingMode.PAYMENT_DETAILS }
        val isEnabled = prefs.getBoolean(KEY_TELEGRAM_ENABLED, true)
        val isConnected = prefs.getBoolean(KEY_TELEGRAM_CONNECTED, token.isNotBlank())
        return TelegramConfig(
            botToken = token,
            groupId = groupId,
            forwardingMode = mode,
            isEnabled = isEnabled,
            isConnected = isConnected
        )
    }

    fun saveTelegramConfig(config: TelegramConfig) {
        prefs.edit()
            .putString(KEY_TELEGRAM_TOKEN, encrypt(config.botToken))
            .putString(KEY_TELEGRAM_GROUP_ID, config.groupId)
            .putString(KEY_TELEGRAM_MODE, config.forwardingMode.name)
            .putBoolean(KEY_TELEGRAM_ENABLED, config.isEnabled)
            .putBoolean(KEY_TELEGRAM_CONNECTED, config.isConnected)
            .apply()
        _telegramConfigFlow.value = config
    }

    private fun loadBinanceConfig(): BinanceConfig {
        val encKey = prefs.getString(KEY_BINANCE_API_KEY, "") ?: ""
        val encSecret = prefs.getString(KEY_BINANCE_API_SECRET, "") ?: ""
        val key = decrypt(encKey)
        val secret = decrypt(encSecret)
        val isConnected = prefs.getBoolean(KEY_BINANCE_CONNECTED, false)
        val lastSync = prefs.getLong(KEY_BINANCE_LAST_SYNC, 0L)
        return BinanceConfig(
            apiKey = key,
            apiSecret = secret,
            isConnected = isConnected,
            lastSyncTime = lastSync
        )
    }

    fun saveBinanceConfig(config: BinanceConfig) {
        prefs.edit()
            .putString(KEY_BINANCE_API_KEY, encrypt(config.apiKey))
            .putString(KEY_BINANCE_API_SECRET, encrypt(config.apiSecret))
            .putBoolean(KEY_BINANCE_CONNECTED, config.isConnected)
            .putLong(KEY_BINANCE_LAST_SYNC, config.lastSyncTime)
            .apply()
        _binanceConfigFlow.value = config
    }

    // Payment source enable/disable toggles
    fun isBkashEnabled(): Boolean = prefs.getBoolean(KEY_BKASH_ENABLED, true)
    fun setBkashEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_BKASH_ENABLED, enabled).apply()

    fun isNagadEnabled(): Boolean = prefs.getBoolean(KEY_NAGAD_ENABLED, true)
    fun setNagadEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_NAGAD_ENABLED, enabled).apply()

    fun isBinanceEnabled(): Boolean = prefs.getBoolean(KEY_BINANCE_ENABLED, true)
    fun setBinanceEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_BINANCE_ENABLED, enabled).apply()

    // Realtime sync simulated pairing ID for two-phone sync
    fun getPairingSessionId(): String {
        var id = prefs.getString(KEY_PAIRING_SESSION_ID, null)
        if (id == null) {
            id = "SYNC-CLUSTER-ALPHA"
            prefs.edit().putString(KEY_PAIRING_SESSION_ID, id).apply()
        }
        return id
    }

    fun setPairingSessionId(id: String) {
        prefs.edit().putString(KEY_PAIRING_SESSION_ID, id).apply()
    }

    // Simple obfuscation/XOR encryption layer so secrets are NEVER in raw plain text
    private fun encrypt(plain: String): String {
        if (plain.isBlank()) return ""
        try {
            val key = 0x5A.toByte()
            val bytes = plain.toByteArray(StandardCharsets.UTF_8)
            val obfuscated = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor key.toInt()).toByte() }
            return Base64.encodeToString(obfuscated, Base64.NO_WRAP)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun decrypt(encrypted: String): String {
        if (encrypted.isBlank()) return ""
        try {
            val key = 0x5A.toByte()
            val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
            val restored = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor key.toInt()).toByte() }
            return String(restored, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            return ""
        }
    }

    companion object {
        private const val KEY_DEVICE_ID = "pref_device_id"
        private const val KEY_DEVICE_NAME = "pref_device_name"
        private const val KEY_TELEGRAM_TOKEN = "pref_telegram_token_enc"
        private const val KEY_TELEGRAM_GROUP_ID = "pref_telegram_group_id"
        private const val KEY_TELEGRAM_MODE = "pref_telegram_mode"
        private const val KEY_TELEGRAM_ENABLED = "pref_telegram_enabled"
        private const val KEY_TELEGRAM_CONNECTED = "pref_telegram_connected"
        private const val KEY_BINANCE_API_KEY = "pref_binance_key_enc"
        private const val KEY_BINANCE_API_SECRET = "pref_binance_secret_enc"
        private const val KEY_BINANCE_CONNECTED = "pref_binance_connected"
        private const val KEY_BINANCE_LAST_SYNC = "pref_binance_last_sync"
        private const val KEY_BKASH_ENABLED = "pref_bkash_enabled"
        private const val KEY_NAGAD_ENABLED = "pref_nagad_enabled"
        private const val KEY_BINANCE_ENABLED = "pref_binance_enabled"
        private const val KEY_PAIRING_SESSION_ID = "pref_pairing_session_id"
    }
}
