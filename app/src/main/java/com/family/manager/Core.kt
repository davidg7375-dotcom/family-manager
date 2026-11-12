package com.family.manager

import android.app.*
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

object Core {
    private val AMT = Regex("""(?:₪\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?))|(?:(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)\s*(?:₪|ש"ח|ש״ח|שח|ILS|NIS))""")
    private val PAY = Regex("חויבת|חיוב|עסקה|תשלום|רכישה|שילמת|שולם|בוצע|payment|paid|charged|purchase", RegexOption.IGNORE_CASE)
    private val SKIP = Regex("זיכוי|החזר|קיבלת|התקבל|refund|received", RegexOption.IGNORE_CASE)
    private val MER = Regex("""(?:בבית העסק|בבית עסק|אצל|ב-)\s*([^,.\d₪\n]{2,30})""")
    // אפליקציות הודעות שמתעלמים מהן (כדי לא לתפוס שיחות); אפשר להוסיף
    val BLOCK = setOf("com.whatsapp", "org.telegram.messenger", "com.facebook.orca", "com.google.android.gm", "com.family.manager")

    fun parse(text: String, title: String): Pair<Double, String>? {
        if (!PAY.containsMatchIn(text) || SKIP.containsMatchIn(text)) return null
        val m = AMT.find(text) ?: return null
        val a = m.groupValues[1].ifEmpty { m.groupValues[2] }.replace(",", "").toDoubleOrNull() ?: return null
        val mer = MER.find(text)?.groupValues?.get(1)?.split(Regex("\\s+(?:באמצעות|בכרטיס|דרך|בסך)\\s*"))?.first()?.trim() ?: title
        return Pair(a, mer.take(40))
    }

    private fun prefs(c: Context) = c.getSharedPreferences("fm", 0)

    fun handle(c: Context, text: String, title: String) {
        val r = parse(text, title) ?: return
        val p = prefs(c); val now = System.currentTimeMillis()
        // אותה עסקה יכולה להגיע מכמה מקורות (ארנק, ביט, SMS): מתעלמים מאותו סכום בתוך 10 דקות
        if (p.getLong("lastT", 0) > now - 600000 && p.getFloat("lastA", 0f) == r.first.toFloat()) return
        p.edit().putLong("lastT", now).putFloat("lastA", r.first.toFloat()).apply()
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("exp", "הוצאות שזוהו", NotificationManager.IMPORTANCE_HIGH))
        val i = Intent(c, PopupActivity::class.java).putExtra("a", r.first).putExtra("m", r.second).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val id = (now % 100000).toInt()
        val pi = PendingIntent.getActivity(c, id, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(c, "exp").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("זוהתה הוצאה: ₪" + r.first).setContentText(r.second + " · לחץ לסיווג")
            .setContentIntent(pi).setAutoCancel(true).build()
        nm.notify(id, n)
    }

    fun savePending(c: Context, amt: Double, desc: String, cat: String) {
        val p = prefs(c); val arr = JSONArray(p.getString("pend", "[]"))
        arr.put(JSONObject().put("amt", amt).put("desc", desc).put("cat", cat).put("date", java.time.LocalDate.now().toString()))
        p.edit().putString("pend", arr.toString()).apply()
    }

    fun takePending(c: Context): String {
        val p = prefs(c); val s = p.getString("pend", "[]") ?: "[]"
        p.edit().remove("pend").apply(); return s
    }
}
