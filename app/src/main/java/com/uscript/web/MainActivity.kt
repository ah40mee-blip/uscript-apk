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
    private var autoRetrying = false // عشان الـ Auto-Retry ميتكررش لانهائي لو السيرفر فعلا واقع
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

    // لوجو برميوم: دائرة جريدينت + حرف U + هالة
    // لوجو برميوم أكتر: هالة خارجية + حلقتين + جريدينت 4 ألوان + ظل + لمعة
    private fun premiumLogo(sizeDp: Int, logoTextSize: Float): FrameLayout {
        val halo = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp + 28), dp(sizeDp + 28)).apply { gravity = Gravity.CENTER; topMargin = dp(10); bottomMargin = dp(10) }
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#1A1033")); setStroke(dp(1), Color.parseColor("#3B2F5E")) }
            elevation = dp(20).toFloat()
        }
        val outer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp)).apply { gravity = Gravity.CENTER }
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#2A1B4A")); setStroke(dp(3), Color.parseColor("#E879F9")) }
            elevation = dp(16).toFloat()
        }
        val mid = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(sizeDp - 10), dp(sizeDp - 10)).apply { gravity = Gravity.CENTER }
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#E879F9"), Color.parseColor("#A855F7"), Color.parseColor("#6366F1"), Color.parseColor("#22D3EE"))).apply { shape = GradientDrawable.OVAL }
        }
        val inner = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(sizeDp - 26), dp(sizeDp - 26)).apply { gravity = Gravity.CENTER }
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#E879F9"), Color.parseColor("#A855F7"), Color.parseColor("#6366F1"))).apply { shape = GradientDrawable.OVAL }
        }
        val letter = TextView(this).apply { text = "U"; this.textSize = logoTextSize; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setTypeface(null, android.graphics.Typeface.BOLD); elevation = dp(4).toFloat(); setShadowLayer(12f, 0f, 4f, Color.parseColor("#4C1D95")) }
        // لمعة صغيرة فوق اللوجو
        val shine = View(this).apply { layoutParams = FrameLayout.LayoutParams(dp(sizeDp / 3), dp(sizeDp / 6)).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; topMargin = dp(10) }; background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#FFFFFF")); alpha = 60 } }
        inner.addView(letter); inner.addView(shine); mid.addView(inner); outer.addView(mid); halo.addView(outer)
        return halo
    }
    // دايلوج برميوم لـ Panel Configured
    private fun showConfiguredDialog() {
        val dlg = android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(28), dp(32), dp(28), dp(28)); background = GradientDrawable().apply { cornerRadius = dp(24).toFloat(); setColor(Color.parseColor("#0D1330")); setStroke(dp(2), Color.parseColor("#E879F9")) }; elevation = dp(24).toFloat() }
        val checkBg = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(dp(84), dp(84)).apply { gravity = Gravity.CENTER }; background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#4ADE80"), Color.parseColor("#22D3EE"))).apply { shape = GradientDrawable.OVAL }; elevation = dp(12).toFloat() }
        checkBg.addView(TextView(this).apply { text = "✓"; textSize = 44f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setTypeface(null, android.graphics.Typeface.BOLD) })
        card.addView(checkBg)
        card.addView(TextView(this).apply { text = "Panel Configured"; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, dp(16), 0, 0) })
        card.addView(TextView(this).apply { text = "U-Panel is ready\nEnjoy your premium experience"; textSize = 14f; setTextColor(Color.parseColor("#9AA4C0")); gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(20)) })
        val ok = btn("Enter U-Panel", "#E879F9") { dlg.dismiss(); try { nativeEnterPanel() } catch (e: Exception) {} }
        card.addView(ok)
        dlg.setContentView(FrameLayout(this).apply { setPadding(dp(28), dp(28), dp(28), dp(28)); addView(card); setOnClickListener { dlg.dismiss() } })
        dlg.setCancelable(true); dlg.show()
        card.alpha = 0f; card.scaleX = 0.8f; card.scaleY = 0.8f; card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(OvershootInterpolator(1.4f)).start()
    }
    private fun nativeEnterPanel() { onEnterPanelClicked() }

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
        val splashLogo = premiumLogo(150, 88f)
        val splashTitle = TextView(this).apply { text = "U-Panel"; textSize = 36f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val splashSub = TextView(this).apply { text = "✨  Control Ur Enigma  ✨"; textSize = 15f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        val splashCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(splashLogo); addView(splashTitle); addView(splashSub)
        }
        splashView = FrameLayout(this).apply { addView(splashCol) }

        // ===== 2) صفحة الخانات =====
        val formLogo = premiumLogo(110, 62f)
        val title = TextView(this).apply { text = "U-Panel"; textSize = 38f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val sub = TextView(this).apply { text = "✨  Control Ur Enigma  ✨"; textSize = 15f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        ipField = field("Receiver IP  ( 192.168.1.45 )", savedIp)
        portField = field("Port  ( 8080 )", savedPort)
        usernameField = field("Username  ( root )", savedUser)
        passwordField = field("Password  ( root )", savedPw, true)
        statusText = TextView(this).apply { textSize = 14f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        connectBtn = btn("CONNECT", "#E879F9") { onConnectClicked() }
        configureBtn = btn("Configure U-Panel", "#E879F9") { onConfigureClicked() }
        enterPanelBtn = btn("Enter U-Panel", "#1E2A55") { onEnterPanelClicked() }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(40), dp(20), dp(20))
            addView(formLogo); addView(title); addView(sub); addView(ipField); addView(portField)
            addView(configureBtn); addView(enterPanelBtn); addView(statusText)
        }
        ipView = ScrollView(this).apply { addView(form); visibility = View.GONE }

        // ===== 3) شاشة Connecting / Configuring - متحركة + بار متحرك =====
        val wLogo = premiumLogo(120, 68f)
        welcomeTitle = TextView(this).apply { text = "U-Panel"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        welcomeConnecting = TextView(this).apply { text = "Connecting..."; textSize = 16f; setTextColor(Color.parseColor("#F472B6")); gravity = Gravity.CENTER }
        val track = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(280), dp(10)).apply { topMargin = dp(20); gravity = Gravity.CENTER }
            background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(Color.parseColor("#1E2A55")); setStroke(dp(1), Color.parseColor("#3B2F5E")) }
        }
        progressFill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, dp(10))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#E879F9"), Color.parseColor("#A855F7"), Color.parseColor("#6366F1"), Color.parseColor("#22D3EE"))).apply { cornerRadius = dp(6).toFloat() }
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
                    // السيرفر وقع في نص الشغل - متخرجش المستخدم بره، اعمل Restart لوحده وارجع حمّل
                    if (r?.isForMainFrame == true && !autoRetrying) {
                        autoRetrying = true
                        showWelcome("Server dropped... Restarting...")
                        Thread {
                            val (_, _, pw) = saveFields(); val (ip2, _, _) = saveFields()
                            val ok = try { ScriptInstaller.restartServer(this@MainActivity, ip2, pw) } catch (e2: Exception) { false }
                            runOnUiThread {
                                stopBar(); welcomeView.visibility = View.GONE; autoRetrying = false
                                if (ok) { webView.visibility = View.VISIBLE; webView.loadUrl(baseUrl); Toast.makeText(this@MainActivity, "Server restarted", Toast.LENGTH_SHORT).show() }
                                else { ipView.visibility = View.VISIBLE; statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Cannot reach $baseUrl - دوس ENTER PANEL تاني" }
                            }
                        }.start()
                    }
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
                progressFill.layoutParams.width = (dp(280) * f).toInt()
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
    // ENTER: استنى السيرفر يقوم الأول - جرّب 8080 لحد 30 ثانية قبل ما يفتح
    private fun onEnterPanelClicked() {
        val (ip, port, _) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        showWelcome("Waiting for server...")
        Thread {
            var up = false
            for (i in 1..15) {
                runOnUiThread { welcomeConnecting.text = "Waiting for server... (" + (16 - i) * 2 + "s)" }
                try { val s = java.net.Socket(); s.connect(java.net.InetSocketAddress(ip, port.toIntOrNull() ?: 8080), 2000); s.close(); up = true; break } catch (e: Exception) { Thread.sleep(2000) }
            }
            runOnUiThread {
                stopBar(); welcomeView.visibility = View.GONE
                if (up) {
                    ipView.visibility = View.GONE; discView.visibility = View.GONE
                    webView.visibility = View.VISIBLE; webView.loadUrl(baseUrl)
                } else {
                    ipView.visibility = View.VISIBLE
                    statusText.setTextColor(Color.parseColor("#F472B6"))
                    statusText.text = "Cannot reach http://$ip:$port - اتأكد السيرفر شغال ودوس ENTER PANEL تاني"
                }
            }
        }.start()
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
                    // Configure بيعمل كل حاجة: زرع + تشغيل + دخول لصفحة PIN على طول
                    statusText.setTextColor(Color.parseColor("#4ADE80")); statusText.text = "U-Panel is Configured"
                    showConfiguredDialog()
                    welcomeView.visibility = View.GONE
                    ipView.visibility = View.GONE
                    discView.visibility = View.GONE
                    // استنى السيرفر يقوم بعد --restart-server قبل ما نفتح (كان بيفتح وهو لسه بيقوم فيضرب Cannot reach)
                    Thread {
                        var up = false
                        for (i in 1..15) { try { val s = java.net.Socket(); s.connect(java.net.InetSocketAddress(ip, 8080), 2000); s.close(); up = true; break } catch (e: Exception) { Thread.sleep(2000) } }
                        runOnUiThread {
                            welcomeView.visibility = View.GONE; ipView.visibility = View.GONE; discView.visibility = View.GONE
                            if (up) { try { webView.clearCache(true) } catch (e: Exception) {}; webView.visibility = View.VISIBLE; webView.loadUrl(baseUrl) }
                            else { ipView.visibility = View.VISIBLE; statusText.setTextColor(Color.parseColor("#F472B6")); statusText.text = "Server not up yet on 8080 - دوس ENTER U-PANEL بعد 10 ثواني" }
                        }
                    }.start()
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
