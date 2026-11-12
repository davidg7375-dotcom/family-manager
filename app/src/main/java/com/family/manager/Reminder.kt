package com.family.manager

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Reminder {
    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(c, 7, Intent(c, ReminderReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val t = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1) }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, t.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) { Reminder.schedule(c); return }
        val today = java.time.LocalDate.now().toString()
        val names = (c.getSharedPreferences("fm", 0).getString("rem", "") ?: "").lines()
            .filter { it.contains("|") && it.substringBefore("|") <= today }.map { it.substringAfter("|") }.distinct()
        if (names.isEmpty()) return
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("rem", "תזכורות הוצאת מרשם", NotificationManager.IMPORTANCE_HIGH))
        val pi = PendingIntent.getActivity(c, 8, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(8, Notification.Builder(c, "rem").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💊 תזכורת מרשמים").setContentText(names.joinToString(", ")).setContentIntent(pi).setAutoCancel(true).build())
        val pf = c.getSharedPreferences("fm", 0)
        val tk = pf.getString("tgt", "") ?: ""
        val ch = pf.getString("tgc", "") ?: ""
        if (pf.getBoolean("tgon", false) && tk.isNotEmpty() && ch.isNotEmpty()) {
            val pr = goAsync()
            Thread {
                try {
                    val body = "chat_id=" + java.net.URLEncoder.encode(ch, "UTF-8") + "&text=" + java.net.URLEncoder.encode("💊 תזכורת מרשמים: " + names.joinToString(", "), "UTF-8")
                    val cn = java.net.URL("https://api.telegram.org/bot$tk/sendMessage").openConnection() as java.net.HttpURLConnection
                    cn.requestMethod = "POST"; cn.doOutput = true
                    cn.outputStream.use { it.write(body.toByteArray()) }
                    val rc = cn.responseCode; cn.disconnect()
                } catch (e: Exception) {
                } finally { pr.finish() }
            }.start()
        }
    }
}
