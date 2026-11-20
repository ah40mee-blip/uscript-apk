package com.uscript.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private val prefs by lazy { getSharedPreferences("uscript", Context.MODE_PRIVATE) }

    private fun serverUrl(): String {
        val ip = prefs.getString("ip", "192.168.1.42") ?: "192.168.1.42"
        return "http://$ip:8080/"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        val s: WebSettings = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportMultipleWindows(false)
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.loadUrl(serverUrl())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "تغيير IP الرسيفر")
        menu.add(0, 2, 0, "تحديث")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                val input = EditText(this)
                input.setText(prefs.getString("ip", "192.168.1.42"))
                input.hint = "مثال: 192.168.1.42"
                AlertDialog.Builder(this)
                    .setTitle("IP الرسيفر")
                    .setView(input)
                    .setPositiveButton("حفظ") { _, _ ->
                        val ip = input.text.toString().trim()
                        if (ip.isNotEmpty()) {
                            prefs.edit().putString("ip", ip).apply()
                            web.loadUrl(serverUrl())
                        }
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            }
            2 -> web.reload()
        }
        return true
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
