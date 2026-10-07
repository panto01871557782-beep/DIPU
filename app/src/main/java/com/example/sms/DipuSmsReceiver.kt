package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.DipuProxyApplication
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DipuSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as? DipuProxyApplication ?: return
        val repo = app.repository
        val prefs = repo.securityPrefs

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress.orEmpty()
            val body = sms.displayMessageBody.orEmpty()

            val parsed = SmsParser.parse(sender, body) ?: continue

            // Check provider toggles
            if (parsed.provider.equals("bKash", ignoreCase = true) && !prefs.isBkashEnabled()) {
                continue
            }
            if (parsed.provider.equals("Nagad", ignoreCase = true) && !prefs.isNagadEnabled()) {
                continue
            }

            // Valid financial notification - Process in Coroutine
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repo.processIncomingTransaction(
                        provider = parsed.provider,
                        amount = parsed.amount,
                        currency = "BDT",
                        transactionId = parsed.transactionId,
                        senderInfo = parsed.senderInfo,
                        rawMessage = body,
                        source = TransactionEntity.SOURCE_SMS
                    )
                } catch (e: Exception) {
                    Log.e("DipuSmsReceiver", "Error processing SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
