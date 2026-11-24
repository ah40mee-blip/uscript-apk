package com.uscript.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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

// U-Panel - الشاشات المتحركة رجعت + البار المتحرك
// صغير (مفيش Imports متضاربة) ومتوافق مع ScriptInstaller المصغر
class MainActivity : AppCompatActivity() {

    private lateinit var ipField: EditText
    private lateinit var portField: EditText
    private lateinit var pinField: EditText
    lateinit var usernameField: EditText
    private lateinit var passwordField: EditText
    private lateinit var connectBtn: Button
    private lateinit var configureBtn: Button
    private lateinit var enterPanelBtn: Button
    private lateinit var statusText: TextView
    private lateinit var webView: WebView
    private lateinit var splashView: View
    private lateinit var ipView: View
    private lateinit var welcomeView: View
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeConnecting: TextView
    private lateinit var progressFill: View
    private lateinit var discView: View
    private var baseUrl = ""
    private var barAnim: android.animation.ValueAnimator? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun box(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(Color.parseColor("#0D1330"))
        setStroke(dp(1), Color.parseColor("#2A3560"))
    }

    private fun field(hint: String, text: String, isPw: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint; setText(text)
            setHintTextColor(Color.parseColor("#5A6378")); setTextColor(Color.WHITE)
            textSize = 16f; gravity = Gravity.CENTER
            if (isPw) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
            background = box(); setPadding(dp(16), dp(16), dp(16), dp(16))
        }
    }

    private fun btn(text: String, color: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text; textSize = 16f; setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) }
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.parseColor(color)) }
            setPadding(dp(16), dp(16), dp(16), dp(16)); setOnClickListener { onClick() }
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

        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#070B1E")) }

        // ===== 1) Splash متحرك =====
        val splashLogo = TextView(this).apply { text = "U"; textSize = 72f; setTextColor(Color.parseColor("#E879F9")); gravity = Gravity.CENTER }
        val splashTitle = TextView(this).apply { text = "U-Panel"; textSize = 36f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val splashSub = TextView(this).apply { text = "Control Ur Enigma"; textSize = 14f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        val splashCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(splashLogo); addView(splashTitle); addView(splashSub)
        }
        splashView = FrameLayout(this).apply { addView(splashCol) }

        // ===== 2) صفحة الخانات =====
        val title = TextView(this).apply { text = "U-Panel"; textSize = 32f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val sub = TextView(this).apply { text = "Control Ur Enigma"; textSize = 14f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        ipField = field("Receiver IP  ( 192.168.1.45 )", savedIp)
        portField = field("Port  ( 8080 )", savedPort)
        usernameField = field("Username  ( root )", savedUser)
        passwordField = field("Password  ( root )", savedPw, true)
        statusText = TextView(this).apply { textSize = 14f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        connectBtn = btn("CONNECT", "#E879F9") { onConnectClicked() }
        configureBtn = btn("Configure U-Panel", "#E879F9") { onConfigureClicked() }
        enterPanelBtn = btn("Enter U-Panel", "#1E2A55") { onEnterPanelClicked() }.apply { visibility = View.GONE }
        pinField = field("PIN").apply { visibility = View.GONE; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(40), dp(20), dp(20))
            addView(title); addView(sub); addView(ipField); addView(portField)
            addView(configureBtn); addView(pinField); addView(enterPanelBtn); addView(statusText)
        }
        ipView = ScrollView(this).apply { addView(form); visibility = View.GONE }

        // ===== 3) شاشة Connecting / Configuring - متحركة + بار متحرك =====
        val wLogo = TextView(this).apply { text = "U"; textSize = 64f; setTextColor(Color.parseColor("#E879F9")); gravity = Gravity.CENTER }
        welcomeTitle = TextView(this).apply { text = "U-Panel"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        welcomeConnecting = TextView(this).apply { text = "Connecting..."; textSize = 16f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        val track = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(6)).apply { topMargin = dp(20); gravity = Gravity.CENTER }
            background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.parseColor("#1E2A55")) }
        }
        progressFill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, dp(6))
            background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.parseColor("#E879F9")) }
        }
        track.addView(progressFill)
        val wCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(wLogo); addView(welcomeTitle); addView(welcomeConnecting); addView(track)
        }
        welcomeView = FrameLayout(this).apply { addView(wCol); visibility = View.GONE }

        // ===== 4) شاشة Disconnected - ثابتة =====
        val dTitle = TextView(this).apply { text = "Disconnected"; textSize = 26f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val dSub = TextView(this).apply { text = "Check connection and try again"; textSize = 14f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        val dBack = btn("BACK", "#0D1330") { discView.visibility = View.GONE; ipView.visibility = View.VISIBLE }
        val dCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20), dp(40), dp(20), dp(20))
            addView(dTitle); addView(dSub); addView(dBack)
        }
        discView = FrameLayout(this).apply { addView(dCol); visibility = View.GONE }

        // ===== WebView =====
        webView = WebView(this).apply {
            visibility = View.GONE; settings.javaScriptEnabled = true; settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(v: WebView?, r: android.webkit.WebResourceRequest?, e: android.webkit.WebResourceError?) {
                    v?.visibility = View.GONE
                    ipView.visibility = View.VISIBLE
                    statusText.setTextColor(Color.parseColor("#F472B6"))
                    statusText.text = "Cannot reach $baseUrl - اتأكد الرسيفر شغال ودوس ENTER PANEL تاني"
                }
            }
        }

        root.addView(splashView); root.addView(ipView); root.addView(welcomeView); root.addView(discView); root.addView(webView)
        setContentView(root)

        // أنيميشن الـ Splash
        splashLogo.alpha = 0f; splashLogo.scaleX = 0.7f; splashLogo.scaleY = 0.7f
        splashTitle.alpha = 0f; splashSub.alpha = 0f
        splashLogo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).setInterpolator(OvershootInterpolator(1.3f)).start()
        splashTitle.animate().alpha(1f).setDuration(700).setStartDelay(150).start()
        splashSub.animate().alpha(1f).setDuration(700).setStartDelay(300).start()
        Handler(Looper.getMainLooper()).postDelayed({
            splashView.visibility = View.GONE; ipView.visibility = View.VISIBLE
        }, 1600)
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

    // البار المتحرك
    private fun startBar() {
        barAnim?.cancel()
        barAnim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200; repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener { a ->
                val f = a.animatedValue as Float
                progressFill.layoutParams.width = (dp(240) * f).toInt()
                progressFill.requestLayout()
            }
        }
        barAnim?.start()
    }
    private fun stopBar() { barAnim?.cancel(); barAnim = null }

    private fun showWelcome(msg: String) {
        ipView.visibility = View.GONE; discView.visibility = View.GONE; webView.visibility = View.GONE
        welcomeView.visibility = View.VISIBLE
        welcomeConnecting.text = msg
        // أنيميشن دخول شاشة Connecting
        welcomeView.alpha = 0f
        welcomeView.animate().alpha(1f).setDuration(400).setInterpolator(DecelerateInterpolator()).start()
        startBar()
    }

    // CONNECT: اختبار بس - يفضل في الصفحة ويقول Connected وميدخلش البانل
    private fun onConnectClicked() {
        val (ip, port, _) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Connecting..."
        connectBtn.isEnabled = false
        Thread {
            var ok = false
            // اختبر الرسيفر نفسه على بورت SSH 22 الأول - حتى لو الـ WebUI لسه بيقوم بعد الريستارت
            try {
                val s = java.net.Socket(); s.connect(java.net.InetSocketAddress(ip, 22), 4000); s.close(); ok = true
            } catch (e: Exception) { ok = false }
            // لو SSH مقفول جرّب بورت الـ WebUI
            if (!ok) {
                try {
                    val c = java.net.URL(baseUrl).openConnection() as java.net.HttpURLConnection
                    c.connectTimeout = 5000; c.readTimeout = 5000; c.requestMethod = "GET"
                    ok = c.responseCode in 200..499; c.disconnect()
                } catch (e: Exception) { ok = false }
            }
            runOnUiThread {
                connectBtn.isEnabled = true
                if (ok) { statusText.setTextColor(Color.parseColor("#4ADE80")); statusText.text = "Connected ✅ to $ip" }
                else { statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Cannot reach $ip - لو لسه عامل ريستارت استنى دقيقة وجرب تاني" }
            }
        }.start()
    }

    // ENTER PANEL: افتح البانل على طول - من غير SSH
    // سيرفر الـ WebUI بتاع Enigma2 بيشتغل لوحده، مش محتاج أمر start
    private fun onEnterPanelClicked() {
        val (ip, _, _) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        val pin = try { pinField.text.toString().trim() } catch (e: Exception) { "" }
        if (pin.isEmpty()) { statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Enter the Panel PIN first"; return }
        // PIN بيتسلم للبانل نفسه - هو اللي بيأكده
        getSharedPreferences("uscript_prefs", MODE_PRIVATE).edit().putString("panel_pin", pin).apply()
        statusText.setTextColor(Color.parseColor("#4ADE80")); statusText.text = "Opening U-Panel..."
        ipView.visibility = View.GONE; discView.visibility = View.GONE; welcomeView.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(baseUrl)
    }

private fun onConfigureClicked() {
        val (ip, _, pw) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        showWelcome("Configuring U-Panel")
        Thread {
            val (ok, err) = ScriptInstaller.installWithError(this, ip, pw) { _ -> }
            runOnUiThread {
                stopBar()
                if (ok) {
                    // CONFIGURE خلص - متدخلش لوحدك، استنى ENTER PANEL
                    statusText.setTextColor(Color.parseColor("#4ADE80")); statusText.text = "U-Panel is Configured"
                    Toast.makeText(this, "U-Panel is Configured", Toast.LENGTH_LONG).show()
                    welcomeView.visibility = View.GONE
                    ipView.visibility = View.VISIBLE
                    pinField.visibility = View.VISIBLE
                    enterPanelBtn.visibility = View.VISIBLE
                    try { webView.clearCache(true) } catch (e: Exception) {}
                } else {
                    welcomeView.visibility = View.GONE
                    ipView.visibility = View.VISIBLE
                    statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Configure failed: $err"
                }
            }
        }.start()
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
