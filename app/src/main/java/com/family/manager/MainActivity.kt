package com.family.manager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var w: WebView
    private var loaded = false
    private var asked = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        w = WebView(this)
        setContentView(w)
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.allowUniversalAccessFromFileURLs = true
        w.addJavascriptInterface(object {
            @JavascriptInterface fun setReminders(s: String) {
                getSharedPreferences("fm", 0).edit().putString("rem", s).apply(); Reminder.schedule(this@MainActivity)
            }
            @JavascriptInterface fun setTelegram(t: String, c: String, on: Boolean) {
                getSharedPreferences("fm", 0).edit().putString("tgt", t).putString("tgc", c).putBoolean("tgon", on).apply()
            }
        }, "Android")
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, u: String) { loaded = true; push() }
        }
        w.loadUrl("file:///android_asset/index.html")
        val perms = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECEIVE_SMS)
                    else arrayOf(Manifest.permission.RECEIVE_SMS)
        requestPermissions(perms, 1)
    }

    override fun onResume() {
        super.onResume()
        push()
        val on = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
        if (!on && !asked) { asked = true; startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
    }

    private fun push() {
        if (!loaded) return
        val j = Core.takePending(this)
        if (j != "[]") w.evaluateJavascript("importPending($j)", null)
    }
}
