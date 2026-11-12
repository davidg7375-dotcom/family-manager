package com.family.manager

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class PayListener : NotificationListenerService() {
    override fun onNotificationPosted(s: StatusBarNotification) {
        if (s.packageName in Core.BLOCK) return
        val e = s.notification.extras
        val t = e.getCharSequence("android.title")?.toString() ?: ""
        val x = (e.getCharSequence("android.bigText") ?: e.getCharSequence("android.text"))?.toString() ?: ""
        Core.handle(this, "$t $x", t)
    }
}
