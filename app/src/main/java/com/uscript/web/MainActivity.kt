package com.uscript.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// U-Panel - الشكل الجديد: لوجو دائري رخامي اسود جلوسي + U رمادي معدني
// مفيش وردي / فوشيا / احمر - كله اسود + رمادي فضي زجاجي
class MainActivity : AppCompatActivity() {

    private lateinit var ipField: EditText
    private lateinit var portField: EditText
    private lateinit var pinField: EditText
    lateinit var usernameField: EditText
    private lateinit var passwordField: EditText
    private lateinit var connectBtn: Button
    private lateinit var configureBtn: Button
    private lateinit var enterPanelBtn: Button
    private lateinit var terminalBtn: Button
    private lateinit var fileManagerBtn: Button
    private lateinit var statusText: TextView
    private lateinit var webView: WebView
    private lateinit var splashView: View
    private lateinit var ipView: View
    private lateinit var welcomeView: View
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeConnecting: TextView
    private lateinit var progressFill: View
    private var baseUrl = ""
    private var barAnim: android.animation.ValueAnimator? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // كارت زجاجي شفاف اسود
    private fun glassBox(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(18).toFloat()
        setColor(Color.parseColor("#22000000"))
        setStroke(dp(1), Color.parseColor("#55C0C0C0"))
    }
    private fun box(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(Color.parseColor("#0A0A0A"))
        setStroke(dp(1), Color.parseColor("#333333"))
    }
    private fun field(hint: String, text: String, isPw: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint; setText(text)
            setHintTextColor(Color.parseColor("#888888")); setTextColor(Color.WHITE)
            textSize = 16f; gravity = Gravity.CENTER
            if (isPw) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
            background = box(); setPadding(dp(16), dp(16), dp(16), dp(16))
        }
    }
    private fun btn(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text; textSize = 16f; setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) }
            background = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(Color.parseColor("#0A0A0A")); setStroke(dp(1), Color.parseColor("#666666")) }
            setPadding(dp(16), dp(18), dp(16), dp(18)); setOnClickListener { onClick() }
        }
    }
    private fun btnSmall(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text; textSize = 14f; setTextColor(Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(6); rightMargin = dp(6); topMargin = dp(12) }
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.parseColor("#111111")); setStroke(dp(1), Color.parseColor("#444444")) }
            setPadding(dp(8), dp(14), dp(8), dp(14)); setOnClickListener { onClick() }
        }
    }
    private fun logoView(sizeDp: Int): ImageView {
        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)).apply { gravity = Gravity.CENTER }
            setImageResource(R.drawable.ic_app_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
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

        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#050505")) }

        // ===== 1) Splash - اللوجو الجديد =====
        val splashLogo = logoView(150)
        val splashTitle = TextView(this).apply { text = "U-Panel"; textSize = 36f; setTextColor(Color.parseColor("#E8E8E8")); gravity = Gravity.CENTER }
        val splashSub = TextView(this).apply { text = "Control Ur Enigma"; textSize = 14f; setTextColor(Color.parseColor("#9A9A9A")); gravity = Gravity.CENTER }
        val splashBy = TextView(this).apply { text = "By Eng Ahmed Abdel Moati"; textSize = 12f; setTextColor(Color.parseColor("#777777")); gravity = Gravity.CENTER }
        val splashCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(splashLogo); addView(splashTitle); addView(splashSub)
            val byWrap = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(16), dp(10), dp(16), dp(10)); background = glassBox(); addView(splashBy) }
            addView(byWrap)
        }
        splashView = FrameLayout(this).apply { addView(splashCol) }

        // ===== 2) صفحة الخانات - شكل جديد =====
        val title = TextView(this).apply { text = "U-Panel"; textSize = 32f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val sub = TextView(this).apply { text = "Control Ur Enigma"; textSize = 14f; setTextColor(Color.parseColor("#9A9A9A")); gravity = Gravity.CENTER }
        ipField = field("Receiver IP  ( 192.168.1.45 )", savedIp)
        portField = field("Port  ( 8080 )", savedPort)
        usernameField = field("Username  ( root )", savedUser)
        passwordField = field("Password  ( root )", savedPw, true)
        statusText = TextView(this).apply { textSize = 14f; setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER }
        connectBtn = btn("CONNECT") { onConnectClicked() }
        configureBtn = btn("Connect to your Enigma") { onConfigureClicked() }
        enterPanelBtn = btn("Enter U-Panel") { onEnterPanelClicked() }.apply { visibility = View.GONE }
        terminalBtn = btnSmall("Terminal") { onTerminalClicked() }
        fileManagerBtn = btnSmall("File Manager") { onFileManagerClicked() }
        val toolsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; addView(terminalBtn); addView(fileManagerBtn) }
        pinField = field("PIN").apply { visibility = View.GONE; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(30), dp(20), dp(20))
            addView(logoView(90)); addView(title); addView(sub); addView(ipField)
            addView(configureBtn); addView(toolsRow); addView(pinField); addView(enterPanelBtn); addView(statusText)
        }
        ipView = ScrollView(this).apply { addView(form); visibility = View.GONE }

        // ===== 3) شاشة Connecting - بار متحرك رمادي =====
        val wLogo = logoView(110)
        welcomeTitle = TextView(this).apply { text = "U-Panel"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        welcomeConnecting = TextView(this).apply { text = "Connecting..."; textSize = 16f; setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER }
        val track = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(6)).apply { topMargin = dp(20); gravity = Gravity.CENTER }
            background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.parseColor("#222222")) }
        }
        progressFill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, dp(6))
            background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.parseColor("#C0C0C0")) }
        }
        track.addView(progressFill)
        val wCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(wLogo); addView(welcomeTitle); addView(welcomeConnecting); addView(track)
        }
        welcomeView = FrameLayout(this).apply { addView(wCol); visibility = View.GONE }

        // ===== 4) Disconnected =====
        val dTitle = TextView(this).apply { text = "Disconnected"; textSize = 26f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val dSub = TextView(this).apply { text = "Check connection and try again"; textSize = 14f; setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER }
        val dBack = btn("BACK") { showIp() }
        val dCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; addView(dTitle); addView(dSub); addView(dBack) }
        val discView = FrameLayout(this).apply { addView(dCol); visibility = View.GONE }

        // ===== 5) WebView البانل =====
        webView = WebView(this).apply {
            visibility = View.GONE
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { super.onPageFinished(view, url) }
            }
        }

        root.addView(splashView); root.addView(ipView); root.addView(welcomeView); root.addView(discView); root.addView(webView)
        setContentView(root)

        // Splash 5 ثواني -> IP
        Handler(Looper.getMainLooper()).postDelayed({ splashView.visibility = View.GONE; ipView.visibility = View.VISIBLE }, 5000)
    }

    private fun showIp() { webView.visibility = View.GONE; ipView.visibility = View.VISIBLE }
    private fun startBar() {
        barAnim?.cancel()
        barAnim = android.animation.ValueAnimator.ofInt(0, 240).apply {
            duration = 2000; repeatCount = android.animation.ValueAnimator.INFINITE; repeatMode = android.animation.ValueAnimator.REVERSE
            addUpdateListener { progressFill.layoutParams = FrameLayout.LayoutParams(dp(it.animatedValue as Int), dp(6)); progressFill.requestLayout() }
            start()
        }
    }
    private fun showEnjoyThenEnter() {
        // اللوجو Fade In -> Fade Out -> Enjoy Ur Premium Experience -> دخول البانل
        welcomeView.visibility = View.VISIBLE; ipView.visibility = View.GONE
        welcomeTitle.text = "U-Panel"; welcomeConnecting.text = "Enjoy Ur Premium Experience"
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 1200 }
        val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 1200; startOffset = 1800 }
        welcomeView.startAnimation(fadeIn)
        Handler(Looper.getMainLooper()).postDelayed({ welcomeView.startAnimation(fadeOut) }, 1800)
        Handler(Looper.getMainLooper()).postDelayed({ onEnterPanelClicked() }, 3200)
    }
    private fun onConnectClicked() { onConfigureClicked() }
    private fun onConfigureClicked() {
        val ip = ipField.text.toString().trim()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP first"; return }
        getSharedPreferences("uscript_prefs", MODE_PRIVATE).edit().putString("ip", ip).putString("port", "8080").apply()
        welcomeView.visibility = View.VISIBLE; ipView.visibility = View.GONE
        welcomeTitle.text = "U-Panel"; welcomeConnecting.text = "Connecting..."
        startBar()
        Thread {
            val res = ScriptInstaller.installWithError(this, ip, "root", "root") { msg ->
                runOnUiThread { welcomeConnecting.text = msg }
            }
            runOnUiThread {
                barAnim?.cancel()
                if (res.first) {
                    baseUrl = "http://$ip:8080/"
                    statusText.setTextColor(Color.parseColor("#4ADE80")); statusText.text = "U-Panel is Configured"
                    Toast.makeText(this, "U-Panel is Configured", Toast.LENGTH_LONG).show()
                    showEnjoyThenEnter()
                } else {
                    welcomeView.visibility = View.GONE; ipView.visibility = View.VISIBLE
                    statusText.setTextColor(Color.parseColor("#FF6B6B")); statusText.text = "Configure failed: ${res.second}"
                }
            }
        }.start()
    }
    private fun onEnterPanelClicked() {
        if (baseUrl.isEmpty()) { val ip = ipField.text.toString().trim(); if (ip.isEmpty()) return; baseUrl = "http://$ip:8080/" }
        welcomeView.visibility = View.GONE; ipView.visibility = View.GONE
        webView.visibility = View.VISIBLE
        try { webView.clearCache(true) } catch (e: Exception) {}
        webView.loadUrl(baseUrl)
    }
    private fun onTerminalClicked() {
        val ip = ipField.text.toString().trim()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP first"; return }
        startActivity(Intent(this, TerminalActivity::class.java).apply { putExtra("ip", ip) })
    }
    private fun onFileManagerClicked() {
        val ip = ipField.text.toString().trim()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP first"; return }
        startActivity(Intent(this, FileManagerActivity::class.java).apply { putExtra("ip", ip) })
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        try {
            if (webView.visibility == View.VISIBLE) {
                val p = android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(16, 9)).build()
                enterPictureInPictureMode(p)
            }
        } catch (e: Exception) {}
    }
}
