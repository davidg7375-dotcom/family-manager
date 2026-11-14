package com.family.manager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
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
            @JavascriptInterface fun saveData(s: String) { try { java.io.File(filesDir, "data.json").writeText(s) } catch (e: Exception) {} }
            @JavascriptInterface fun loadData(): String = try { java.io.File(filesDir, "data.json").readText() } catch (e: Exception) { "" }
            @JavascriptInterface fun notifOn(): Boolean = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
            @JavascriptInterface fun openSettings(w: String) {
                val i = if (w == "app") Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName")) else Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }, "Android")
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, u: String) { loaded = true; push() }
        }
        if (b == null || w.restoreState(b) == null) w.loadUrl("file:///android_asset/index.html")
        val perms = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECEIVE_SMS)
                    else arrayOf(Manifest.permission.RECEIVE_SMS)
        requestPermissions(perms, 1)
    }

    override fun onSaveInstanceState(o: Bundle) { super.onSaveInstanceState(o); w.saveState(o) }

    override fun onResume() {
        super.onResume()
        push()
        if (loaded) w.evaluateJavascript("if(window.bgRender)bgRender()", null)
        val on = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
        val pf = getSharedPreferences("fm", 0)
        if (!on && !asked && !pf.getBoolean("askedN", false)) {
            asked = true; pf.edit().putBoolean("askedN", true).apply()
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }

    private fun push() {
        if (!loaded) return
        val j = Core.takePending(this)
        if (j != "[]") w.evaluateJavascript("importPending($j)", null)
    }
}
