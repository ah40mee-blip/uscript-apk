package com.uscript.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// U-Panel - نسخة مصغرة جذري: بدون أي Imports متضاربة
// PiP بيتنده باسمه الكامل جوه الكود
class MainActivity : AppCompatActivity() {

    private lateinit var ipField: EditText
    private lateinit var portField: EditText
    private lateinit var usernameField: EditText
    private lateinit var passwordField: EditText
    private lateinit var connectBtn: Button
    private lateinit var configureBtn: Button
    private lateinit var statusText: TextView
    private lateinit var webView: WebView
    private var baseUrl = ""

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun field(hint: String, text: String, isPw: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(text)
            setHintTextColor(Color.parseColor("#5A6378"))
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            if (isPw) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#0D1330"))
                setStroke(dp(1), Color.parseColor("#2A3560"))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
    }

    private fun btn(text: String, color: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) }
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.parseColor(color)) }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener { onClick() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("uscript_prefs", MODE_PRIVATE)
        val savedIp = prefs.getString("ip", "") ?: ""
        val savedPort = prefs.getString("port", "8080") ?: "8080"
        val savedUser = prefs.getString("ssh_user", "root") ?: "root"
        val savedPw = prefs.getString("root_pw", "root") ?: "root"

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#070B1E")); setPadding(dp(20), dp(40), dp(20), dp(20)) }
        val title = TextView(this).apply { text = "U-Panel"; textSize = 32f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val sub = TextView(this).apply { text = "Control Ur Enigma"; textSize = 14f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }

        ipField = field("Receiver IP  ( 192.168.1.45 )", savedIp)
        portField = field("Port  ( 8080 )", savedPort)
        usernameField = field("Username  ( root )", savedUser)
        passwordField = field("Password  ( root )", savedPw, true)
        statusText = TextView(this).apply { textSize = 14f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }

        connectBtn = btn("CONNECT", "#E879F9") { onConnectClicked() }
        configureBtn = btn("CONFIGURE PANEL", "#0D1330") { onConfigureClicked() }

        webView = WebView(this).apply {
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
        }

        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        form.addView(title); form.addView(sub)
        form.addView(ipField); form.addView(portField); form.addView(usernameField); form.addView(passwordField)
        form.addView(connectBtn); form.addView(configureBtn); form.addView(statusText)
        val scroll = ScrollView(this).apply { addView(form) }
        root.addView(scroll); root.addView(webView)
        setContentView(root)
    }

    private fun saveFields(): Triple<String, String, String> {
        val ip = ipField.text.toString().trim()
        val port = portField.text.toString().trim().ifEmpty { "8080" }
        val user = usernameField.text.toString().trim().ifEmpty { "root" }
        val pw = passwordField.text.toString().trim().ifEmpty { "root" }
        getSharedPreferences("uscript_prefs", MODE_PRIVATE).edit()
            .putString("ip", ip).putString("port", port).putString("ssh_user", user).putString("root_pw", pw).apply()
        baseUrl = "http://$ip:$port"
        return Triple(ip, port, pw)
    }

    // CONNECT: اختبار بس - يفضل في الصفحة ويقول Connected وميدخلش البانل
    private fun onConnectClicked() {
        val (ip, port, _) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Connecting..."
        connectBtn.isEnabled = false
        Thread {
            var ok = false
            try {
                val c = java.net.URL(baseUrl).openConnection() as java.net.HttpURLConnection
                c.connectTimeout = 5000; c.readTimeout = 5000; c.requestMethod = "GET"
                ok = c.responseCode in 200..499; c.disconnect()
            } catch (e: Exception) {
                try {
                    val s = java.net.Socket(); s.connect(java.net.InetSocketAddress(ip, port.toIntOrNull() ?: 8080), 3000); s.close(); ok = true
                } catch (e2: Exception) { ok = false }
            }
            runOnUiThread {
                connectBtn.isEnabled = true
                if (ok) { statusText.setTextColor(Color.parseColor("#4ADE80")); statusText.text = "Connected ✅ to $ip" }
                else { statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Cannot reach $ip:$port - check IP / Port" }
            }
        }.start()
    }

    // CONFIGURE PANEL: تثبيت + دخول مع بعض - ويقول Panel configured
    private fun onConfigureClicked() {
        val (ip, _, pw) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Configuring U-Panel..."
        Thread {
            val (ok, err) = ScriptInstaller.installWithError(this, ip, pw) { _ -> }
            runOnUiThread {
                if (ok) {
                    statusText.setTextColor(Color.parseColor("#4ADE80")); statusText.text = "Panel configured ✅"
                    Toast.makeText(this, "Panel configured ✅", Toast.LENGTH_LONG).show()
                    try { webView.clearCache(true) } catch (e: Exception) {}
                    webView.visibility = View.VISIBLE
                    webView.loadUrl(baseUrl)
                } else {
                    statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Configure failed: $err"
                }
            }
        }.start()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // PiP باسمه الكامل - من غير Import
        try {
            if (webView.visibility == View.VISIBLE) {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9)).build()
                enterPictureInPictureMode(params)
            }
        } catch (e: Exception) {}
    }
}
