package com.uscript.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    // ===== شاشة الافتتاح =====
    private lateinit var splashView: View
    private lateinit var splashLogo: ImageView
    private lateinit var splashTitle: TextView
    private lateinit var splashDivider: View
    private lateinit var splashSub: TextView
    private lateinit var splashE2: TextView

    // ===== شاشة الـ IP =====
    private lateinit var ipView: View
    private lateinit var ipField: EditText
    private lateinit var portField: EditText
    private lateinit var connectBtn: Button
    private lateinit var statusText: TextView

    // ===== الشاشة الثابتة (Connecting) =====
    private lateinit var welcomeView: View
    private lateinit var welcomeLogo: ImageView
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeConnecting: TextView
    private lateinit var progressTrack: View
    private lateinit var progressFill: View

    // ===== الـ WebView + شاشة عدم الاتصال =====
    private lateinit var webView: WebView
    private lateinit var disconnectedView: View
    private lateinit var retryBtn: Button

    private var baseUrl = ""
    private var connectStarted = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun spacer(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(h)
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ملء الشاشة بالكامل - من غير شريط علوي ولا سفلي
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
        }
        setContentView(root)

        // ================= 1) شاشة الافتتاح (Fade In) =================
        val splashBox = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
        }
        splashView = splashBox

        val splashCenter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

        splashLogo = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_icon)
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            layoutParams = LinearLayout.LayoutParams(dp(150), dp(150))
        }

        splashTitle = TextView(this).apply {
            text = "U-Script"
            textSize = 56f
            typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
            letterSpacing = 0.02f
            gravity = Gravity.CENTER
            alpha = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            post {
                paint.shader = LinearGradient(
                    0f, 0f, dp(300).toFloat(), 0f,
                    intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE")),
                    null, Shader.TileMode.CLAMP
                )
            }
        }

        splashDivider = View(this).apply {
            alpha = 0f
            layoutParams = LinearLayout.LayoutParams(dp(140), dp(3)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE"))
            ).apply { cornerRadius = dp(2).toFloat() }
        }

        splashSub = TextView(this).apply {
            text = "Control Enigma World"
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.25f
            setTextColor(Color.parseColor("#E5E7EB"))
            gravity = Gravity.CENTER
            alpha = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        splashCenter.addView(splashLogo)
        splashCenter.addView(spacer(16))
        splashCenter.addView(splashTitle)
        splashCenter.addView(spacer(24))
        splashCenter.addView(splashDivider)
        splashCenter.addView(spacer(16))
        splashCenter.addView(splashSub)

        // E2 آخر الشاشة تحت - أكبر ومرسومة
        splashE2 = TextView(this).apply {
            text = "E2"
            textSize = 30f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.3f
            gravity = Gravity.CENTER
            alpha = 0f
            setShadowLayer(dp(14).toFloat(), 0f, 0f, Color.parseColor("#E879F9"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(32)
            }
            post {
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), 0f,
                    intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE")),
                    null, Shader.TileMode.CLAMP
                )
            }
        }

        splashBox.addView(splashCenter)
        splashBox.addView(splashE2)
        root.addView(splashBox)

        // ================= 2) شاشة الـ IP =================
        val ipScroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
            visibility = View.GONE
        }
        ipView = ipScroll

        val ipCenter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), dp(48), dp(32), dp(32))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val ipLogo = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_icon)
            layoutParams = LinearLayout.LayoutParams(dp(90), dp(90))
        }

        val ipTitle = TextView(this).apply {
            text = "U-Script"
            textSize = 34f
            typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
            letterSpacing = 0.02f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            post {
                paint.shader = LinearGradient(
                    0f, 0f, dp(200).toFloat(), 0f,
                    intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE")),
                    null, Shader.TileMode.CLAMP
                )
            }
        }

        val ipSub = TextView(this).apply {
            text = "Enter your receiver IP"
            textSize = 15f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(Color.parseColor("#8A93B2"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val savedIp = getSharedPreferences("uscript_prefs", MODE_PRIVATE).getString("ip", "") ?: ""
        val savedPort = getSharedPreferences("uscript_prefs", MODE_PRIVATE).getString("port", "8080") ?: "8080"

        ipField = EditText(this).apply {
            hint = "Receiver IP  ( 192.168.1.45 )"
            setText(savedIp)
            setHintTextColor(Color.parseColor("#5A6378"))
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#0D1330"))
                setStroke(dp(1), Color.parseColor("#2A3560"))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        portField = EditText(this).apply {
            hint = "Port  ( 8080 )"
            setText(savedPort)
            setHintTextColor(Color.parseColor("#5A6378"))
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#0D1330"))
                setStroke(dp(1), Color.parseColor("#2A3560"))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        connectBtn = Button(this).apply {
            text = "CONNECT"
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.2f
            setTextColor(Color.parseColor("#FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(24) }
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#E879F9"))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener { onConnectClicked() }
        }

        statusText = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.parseColor("#F472B6"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }

        ipCenter.addView(ipLogo)
        ipCenter.addView(spacer(16))
        ipCenter.addView(ipTitle)
        ipCenter.addView(spacer(8))
        ipCenter.addView(ipSub)
        ipCenter.addView(spacer(24))
        ipCenter.addView(ipField)
        ipCenter.addView(portField)
        ipCenter.addView(connectBtn)
        ipCenter.addView(statusText)
        ipScroll.addView(ipCenter)
        root.addView(ipScroll)

        // ================= 3) الشاشة الثابتة (Connecting) =================
        val welcomeBox = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
            visibility = View.GONE
        }
        welcomeView = welcomeBox

        val welcomeCenter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

        welcomeLogo = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_icon)
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            layoutParams = LinearLayout.LayoutParams(dp(120), dp(120))
        }

        welcomeTitle = TextView(this).apply {
            text = "U-Script"
            textSize = 44f
            typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
            letterSpacing = 0.02f
            gravity = Gravity.CENTER
            alpha = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            post {
                paint.shader = LinearGradient(
                    0f, 0f, dp(260).toFloat(), 0f,
                    intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE")),
                    null, Shader.TileMode.CLAMP
                )
            }
        }

        welcomeConnecting = TextView(this).apply {
            text = "Connecting..."
            textSize = 18f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.2f
            setTextColor(Color.parseColor("#8A93B2"))
            gravity = Gravity.CENTER
            alpha = 0f
        }

        progressTrack = FrameLayout(this).apply {
            alpha = 0f
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(14)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(7).toFloat()
                setColor(Color.parseColor("#1E2A4A"))
            }
        }

        // البار المتحرك - تدرج مباشر
        progressFill = View(this).apply {
            alpha = 0f
            layoutParams = FrameLayout.LayoutParams(0, dp(14)).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE"))
            ).apply {
                cornerRadius = dp(7).toFloat()
            }
        }
        (progressTrack as FrameLayout).addView(progressFill)

        // الكبسولة حوالين Connecting والبار - مرسومة جوّه الكود (من غير ملف بره)
        val connectCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(dp(300), LinearLayout.LayoutParams.WRAP_CONTENT)
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.parseColor("#0D1330"))
                setStroke(dp(1), Color.parseColor("#2A3560"))
            }
            setPadding(dp(24), dp(26), dp(24), dp(26))
        }
        welcomeConnecting.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        connectCard.addView(welcomeConnecting)
        connectCard.addView(spacer(16))
        connectCard.addView(progressTrack)

        welcomeCenter.addView(welcomeLogo)
        welcomeCenter.addView(spacer(6))
        welcomeCenter.addView(welcomeTitle)
        welcomeCenter.addView(spacer(36))
        welcomeCenter.addView(connectCard)

        welcomeBox.addView(welcomeCenter)
        root.addView(welcomeBox)

        // ================= 4) الـ WebView =================
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (connectStarted) {
                        welcomeView.visibility = View.GONE
                        disconnectedView.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true && webView.visibility != View.VISIBLE) {
                        showDisconnected()
                    }
                }
            }
        }
        root.addView(webView)

        // ================= 5) شاشة عدم الاتصال =================
        val discBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#070B1F"))
            setPadding(dp(32), dp(32), dp(32), dp(32))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        disconnectedView = discBox

        val discIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_icon)
            layoutParams = LinearLayout.LayoutParams(dp(100), dp(100))
        }

        val discTitle = TextView(this).apply {
            text = "U-Script Panel is disconnected"
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val discSub = TextView(this).apply {
            text = "Check the receiver IP and try again"
            textSize = 14f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(Color.parseColor("#8A93B2"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        retryBtn = Button(this).apply {
            text = "TRY AGAIN"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.2f
            setTextColor(Color.parseColor("#FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(24) }
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#E879F9"))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener { onConnectClicked() }
        }

        discBox.addView(discIcon)
        discBox.addView(spacer(20))
        discBox.addView(discTitle)
        discBox.addView(spacer(8))
        discBox.addView(discSub)
        discBox.addView(retryBtn)
        root.addView(discBox)

        // ابدأ بشاشة الافتتاح
        startSplash()
    }

    // ===== حركة شاشة الافتتاح =====
    private fun startSplash() {
        splashLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700).setInterpolator(DecelerateInterpolator()).start()
        splashTitle.animate().alpha(1f).setDuration(700).setStartDelay(150).start()
        splashDivider.animate().alpha(1f).setDuration(700).setStartDelay(300).start()
        splashSub.animate().alpha(1f).setDuration(700).setStartDelay(450).start()
        // E2 بيظهر مع الباقي
        splashE2.animate().alpha(1f)
            .setDuration(900).setStartDelay(900).start()
        Handler(Looper.getMainLooper()).postDelayed({ showIpScreen() }, 1600)
    }

    private fun showIpScreen() {
        splashView.visibility = View.GONE
        ipView.visibility = View.VISIBLE
    }

    // ===== زرار CONNECT =====
    private fun onConnectClicked() {
        val ip = ipField.text.toString().trim()
        val port = portField.text.toString().trim().ifEmpty { "8080" }
        if (ip.isEmpty()) {
            statusText.text = "Please enter the receiver IP"
            return
        }
        getSharedPreferences("uscript_prefs", MODE_PRIVATE)
            .edit().putString("ip", ip).putString("port", port).apply()
        baseUrl = "http://$ip:$port"

        ipView.visibility = View.GONE
        disconnectedView.visibility = View.GONE
        welcomeView.visibility = View.VISIBLE

        // تصفير عناصر الشاشة الثابتة
        welcomeLogo.alpha = 0f
        welcomeLogo.scaleX = 0.8f
        welcomeLogo.scaleY = 0.8f
        welcomeTitle.alpha = 0f
        welcomeConnecting.alpha = 0f
        progressTrack.alpha = 0f
        progressFill.alpha = 0f
        progressFill.layoutParams.width = 0
        progressFill.requestLayout()

        // ظهور العناصر
        welcomeLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700).setInterpolator(DecelerateInterpolator()).start()
        welcomeTitle.animate().alpha(1f).setDuration(700).setStartDelay(150).start()
        welcomeConnecting.animate().alpha(1f).setDuration(700).setStartDelay(300).start()
        progressTrack.animate().alpha(1f).setDuration(700).setStartDelay(400).start()
        progressFill.animate().alpha(1f).setDuration(700).setStartDelay(400).start()

        // حركة البار المتحرك
        val barAnim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                progressFill.layoutParams.width = (dp(240) * f).toInt()
                progressFill.requestLayout()
            }
        }
        barAnim.start()

        // افتح صفحة الريسيفر
        connectStarted = true
        webView.loadUrl(baseUrl)
    }

    private fun showDisconnected() {
        welcomeView.visibility = View.GONE
        webView.visibility = View.GONE
        disconnectedView.visibility = View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
            webView.goBack()
        } else if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
            ipView.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }
}
