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
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
import android.view.View
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
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

    private lateinit var splashView: View
    private lateinit var splashLogo: ImageView
    private lateinit var splashTitle: TextView
    private lateinit var splashDivider: View
    private lateinit var splashSub: TextView
    private lateinit var splashE2: TextView

    private lateinit var ipView: View
    private lateinit var ipField: EditText
    private lateinit var portField: EditText
    private lateinit var pinField: EditText
    private lateinit var connectBtn: Button
    private lateinit var installBtn: Button
    private lateinit var statusText: TextView

    private lateinit var welcomeView: View
    private lateinit var welcomeLogo: ImageView
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeConnecting: TextView
    private lateinit var progressTrack: View
    private lateinit var progressFill: View

    private lateinit var webView: WebView
    private lateinit var disconnectedView: View
    private lateinit var retryBtn: Button

    private lateinit var welcomeConnectedView: View
    private lateinit var wcWelcome: TextView
    private lateinit var wcTo: TextView
    private lateinit var wcU: TextView
    private lateinit var wcMessage: TextView
    private lateinit var wcSub: TextView

    private var baseUrl = ""
    private var connectStarted = false
    private var loadFailed = false
    private var welcomeShown = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun spacer(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(h)
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            text = "U-Panel"
            textSize = 48f
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

        // اللوجو الموحد - نفس اللي في الصورة اللي عجبتك
        val ipLogoHeader = buildLogoHeader()
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
        val savedPin = getSharedPreferences("uscript_prefs", MODE_PRIVATE).getString("pin", "") ?: ""

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

        pinField = EditText(this).apply {
            hint = "PIN  ( اختياري )"
            setText(savedPin)
            setHintTextColor(Color.parseColor("#5A6378"))
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 16f
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
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

        ipCenter.addView(ipLogoHeader)
        ipCenter.addView(spacer(8))
        ipCenter.addView(ipSub)
        ipCenter.addView(spacer(24))
        ipCenter.addView(ipField)
        ipCenter.addView(portField)
        ipCenter.addView(pinField)
        ipCenter.addView(connectBtn)
        // زرار Install - بنفس ستايل CONNECT بالظبط، من غير تغيير أي حاجة تانية
        installBtn = Button(this).apply {
            text = "📦 INSTALL U-PANEL"
            textSize = 15f; setTextColor(Color.WHITE); isAllCaps = false
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.parseColor("#0F172A")) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(10) }
            setOnClickListener { showInstallDialog() }
        }
        ipCenter.addView(installBtn)
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

        // ================= 3.5) شاشة الترحيب الجديدة - بنفس لوجو التطبيق + Fade In =================
        val wcBox = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
            visibility = View.GONE
        }
        welcomeConnectedView = wcBox

        val wcCenter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

        val wcLogo = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_icon)
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
            layoutParams = LinearLayout.LayoutParams(dp(140), dp(140))
        }

        wcU = TextView(this).apply {
            text = "U-Panel"
            textSize = 38f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            alpha = 0f
        }

        wcWelcome = TextView(this).apply {
            text = "●  Connected Successfully"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            alpha = 0f
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(Color.parseColor("#1B2A4A"))
                setStroke(dp(1), Color.parseColor("#2A3560"))
            }
            setPadding(dp(22), dp(12), dp(22), dp(12))
        }

        wcMessage = TextView(this).apply {
            text = "Connected to 192.168.1.45"
            textSize = 14f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(Color.parseColor("#8A93B2"))
            gravity = Gravity.CENTER
            alpha = 0f
        }

        val wcLine = View(this).apply {
            alpha = 0f
            layoutParams = LinearLayout.LayoutParams(dp(180), dp(3)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE"))
            ).apply { cornerRadius = dp(2).toFloat() }
        }

        wcTo = TextView(this).apply {
            text = "Control Ur Enigma"
            textSize = 13f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.25f
            setTextColor(Color.parseColor("#8A93B2"))
            gravity = Gravity.CENTER
            alpha = 0f
        }

        wcSub = TextView(this).apply { visibility = View.GONE }

        wcCenter.addView(wcLogo)
        wcCenter.addView(spacer(20))
        wcCenter.addView(wcU)
        wcCenter.addView(spacer(22))
        wcCenter.addView(wcWelcome)
        wcCenter.addView(spacer(14))
        wcCenter.addView(wcMessage)
        wcCenter.addView(spacer(22))
        wcCenter.addView(wcLine)
        wcCenter.addView(spacer(18))
        wcCenter.addView(wcTo)
        wcBox.addView(wcCenter)
        root.addView(wcBox)

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
            layoutParams = LinearLayout.LayoutParams(dp(150), dp(150))
        }

        welcomeTitle = TextView(this).apply {
            text = "U-Panel"
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
            settings.mediaPlaybackRequiresUserGesture = false
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (connectStarted && !loadFailed && !welcomeShown) {
                        welcomeShown = true
                        welcomeView.visibility = View.GONE
                        disconnectedView.visibility = View.GONE
                        showWelcomeConnected()
                    }
                }
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        loadFailed = true
                        connectStarted = false
                        view?.stopLoading()
                        showDisconnected()
                    }
                }
                @Suppress("DEPRECATION")
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    loadFailed = true
                    connectStarted = false
                    view?.stopLoading()
                    showDisconnected()
                }
            }
            // ===== Fullscreen للمشغل - لما تدوس Fullscreen في الفيديو =====
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                    if (customView != null) { callback?.onCustomViewHidden(); return }
                    customView = view
                    customViewCallback = callback
                    isFullscreen = true
                    val decor = window.decorView as android.view.ViewGroup
                    decor.addView(customView, android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT))
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }
                override fun onHideCustomView() {
                    if (customView == null) return
                    val decor = window.decorView as android.view.ViewGroup
                    decor.removeView(customView)
                    customView = null
                    customViewCallback?.onCustomViewHidden()
                    isFullscreen = false
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
        root.addView(webView)

        // ================= 5) شاشة عدم الاتصال =================
        val discBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
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
            layoutParams = LinearLayout.LayoutParams(dp(130), dp(130))
        }

        val discTitle = TextView(this).apply {
            text = "U-Panel is not connected"
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        retryBtn = Button(this).apply {
            text = "إعادة محاولة الاتصال"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0f
            setTextColor(Color.parseColor("#FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                dp(240),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(24) }
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#E879F9"))
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { onConnectClicked() }
        }

        discBox.addView(discIcon)
        discBox.addView(spacer(20))
        discBox.addView(discTitle)
        discBox.addView(retryBtn)
        root.addView(discBox)

        startSplash()
    }

    private fun startSplash() {
        splashLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700).setInterpolator(DecelerateInterpolator()).start()
        splashTitle.animate().alpha(1f).setDuration(700).setStartDelay(150).start()
        splashDivider.animate().alpha(1f).setDuration(700).setStartDelay(300).start()
        splashSub.animate().alpha(1f).setDuration(700).setStartDelay(450).start()
        splashE2.animate().alpha(1f)
            .setDuration(900).setStartDelay(900).start()
        Handler(Looper.getMainLooper()).postDelayed({ showIpScreen() }, 5000)
    }

    private fun showIpScreen() {
        splashView.visibility = View.GONE
        ipView.visibility = View.VISIBLE
    }

    private fun onConnectClicked() {
        val ip = ipField.text.toString().trim()
        val port = portField.text.toString().trim().ifEmpty { "8080" }
        val pin = pinField.text.toString().trim()
        if (ip.isEmpty()) {
            statusText.text = "Please enter the receiver IP"
            return
        }
        getSharedPreferences("uscript_prefs", MODE_PRIVATE)
            .edit().putString("ip", ip).putString("port", port).putString("pin", pin).apply()
        baseUrl = if (pin.isNotEmpty()) "http://root:$pin@$ip:$port" else "http://$ip:$port"

        ipView.visibility = View.GONE
        disconnectedView.visibility = View.GONE
        welcomeView.visibility = View.VISIBLE

        welcomeLogo.alpha = 0f
        welcomeLogo.scaleX = 0.8f
        welcomeLogo.scaleY = 0.8f
        welcomeTitle.alpha = 0f
        welcomeConnecting.alpha = 0f
        progressTrack.alpha = 0f
        progressFill.alpha = 0f
        progressFill.layoutParams.width = 0
        progressFill.requestLayout()

        welcomeLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700).setInterpolator(DecelerateInterpolator()).start()
        welcomeTitle.animate().alpha(1f).setDuration(700).setStartDelay(150).start()
        welcomeConnecting.animate().alpha(1f).setDuration(700).setStartDelay(300).start()
        progressTrack.animate().alpha(1f).setDuration(700).setStartDelay(400).start()
        progressFill.animate().alpha(1f).setDuration(700).setStartDelay(400).start()

        val barAnim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                progressFill.layoutParams.width = (dp(240) * f).toInt()
                progressFill.requestLayout()
            }
        }
        barAnim.start()

        loadFailed = false
        welcomeShown = false
        welcomeConnectedView.visibility = View.GONE
        connectStarted = true
        webView.loadUrl(baseUrl)
    }

    private fun showWelcomeConnected() {
        val savedIp = getSharedPreferences("uscript_prefs", MODE_PRIVATE).getString("ip", "") ?: ""
        if (savedIp.isNotEmpty()) wcMessage.text = "Connected to $savedIp"

        welcomeConnectedView.visibility = View.VISIBLE

        val wcCenterView = (welcomeConnectedView as FrameLayout).getChildAt(0) as LinearLayout
        val wcLogoView = wcCenterView.getChildAt(0) as ImageView
        val wcLineView = wcCenterView.getChildAt(8) as View

        wcLogoView.alpha = 0f
        wcLogoView.scaleX = 0.85f
        wcLogoView.scaleY = 0.85f
        wcU.alpha = 0f
        wcWelcome.alpha = 0f
        wcMessage.alpha = 0f
        wcLineView.alpha = 0f
        wcTo.alpha = 0f

        wcLogoView.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700).setInterpolator(DecelerateInterpolator()).start()
        wcU.animate().alpha(1f).setDuration(600).setStartDelay(250).start()
        wcWelcome.animate().alpha(1f).setDuration(600).setStartDelay(450).start()
        wcMessage.animate().alpha(1f).setDuration(600).setStartDelay(600).start()
        wcLineView.animate().alpha(1f).setDuration(600).setStartDelay(700).start()
        wcTo.animate().alpha(1f).setDuration(600).setStartDelay(800).start()

        Handler(Looper.getMainLooper()).postDelayed({
            welcomeConnectedView.visibility = View.GONE
            webView.visibility = View.VISIBLE
            
        }, 1800)
    }

    // ===== Install Script - إضافة فقط، بدون تغيير أي شكل أو وظيفة =====
    private fun showInstallDialog() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(16), dp(24), dp(16)) }
        val pass = EditText(this).apply {
            hint = "باسورد Root للرسيفر"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(14), dp(14), dp(14), dp(14)); textSize = 15f
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(Color.parseColor("#F1F5F9")) }
        }
        val info = TextView(this).apply { text = "IP: " + ipInput.text.toString() + "\nهيتم تثبيت U-Panel من GitHub على الرسيفر عبر SSH (بورت 22)"; textSize = 13f; setTextColor(Color.parseColor("#334155")) }
        lay.addView(info); lay.addView(pass)
        var progressDlg: android.app.AlertDialog? = null
        android.app.AlertDialog.Builder(this).setTitle("📦 Install U-Panel").setView(lay)
            .setPositiveButton("تثبيت") { _, _ ->
                val ip = ipInput.text.toString().trim()
                val pw = pass.text.toString()
                if (ip.isEmpty()) { Toast.makeText(this, "اكتب IP الرسيفر الأول", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                progressDlg = android.app.AlertDialog.Builder(this).setTitle("جاري التثبيت...").setMessage("جاري الاتصال بالرسيفر...").setCancelable(false).show()
                Thread {
                    val res = ScriptInstaller.install(this, ip, pw) { msg -> runOnUiThread { try { progressDlg?.setMessage(msg) } catch(e: Exception) {} } }
                    runOnUiThread {
                        try { progressDlg?.dismiss() } catch(e: Exception) {}
                        android.app.AlertDialog.Builder(this).setTitle(if (res) "✅ تم التثبيت" else "❌ فشل التثبيت")
                            .setMessage(if (res) "تم تثبيت U-Panel بنجاح.\nدوس CONNECT للدخول." else "فشل التثبيت.\nاتأكد إن SSH شغال على الرسيفر والباسورد صح وحاول تاني.")
                            .setPositiveButton("تمام", null).show()
                    }
                }.start()
            }.setNegativeButton("إلغاء", null).show()
    }

    // ===== اللوجو الموحد - U-Panel في كل الشاشات =====
    private fun buildLogoHeader(withAnimation: Boolean = false): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        // اللوجو: سداسي بجرادينت وردي / سماوي + حرف U
        val logoBox = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE"))
                gradientType = GradientDrawable.LINEAR_GRADIENT
                orientation = GradientDrawable.Orientation.TL_BR
            }
        }
        val uTxt = TextView(this).apply {
            text = "U"; textSize = 38f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        logoBox.addView(uTxt)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        val title = TextView(this).apply {
            text = "U-Panel"; textSize = 38f; setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val sub = TextView(this).apply {
            text = "Control Ur Enigma"; textSize = 16f
            setTextColor(Color.parseColor("#7DD3FC"))
        }
        col.addView(title); col.addView(sub)
        row.addView(logoBox); row.addView(col)
        if (withAnimation) {
            row.alpha = 0f
            row.animate().alpha(1f).setDuration(900).start()
        }
        return row
    }


    // ===== ملء الشاشة للتطبيق كله =====
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    // ===== PiP - صورة عايمة للبث =====
    // لما المستخدم يسيب التطبيق وهو بيتفرج، الفيديو يصغر ويبقى عايم فوق أي برنامج
    private var isInPip = false
    private var pipRequested = false
    // ===== Fullscreen - ملء الشاشة للمشغل =====
    private var customView: android.view.View? = null
    private var customViewCallback: android.webkit.WebChromeClient.CustomViewCallback? = null
    private var isFullscreen = false

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // لو الداشبورد مفتوح (WebView ظاهر) ادخل PiP أوتوماتيك
        if (!pipRequested && webView.visibility == View.VISIBLE) {
            enterPipMode()
        }
    }

    fun enterPipMode() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                pipRequested = true
                enterPictureInPictureMode(params)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPip = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            // في PiP: اخفي كل حاجة ما عدا الفيديو
            // الـ WebView هيفضل شغال والفيديو مكمل
            rootView?.findViewById<View>(android.R.id.content)?.let {}
            // اخفي الواجهات اللي فوق الـ WebView
            // هنخفي كل الـ Views اللي مش WebView عن طريق إخفاء الـ overlay
            hideUiForPip(true)
        } else {
            hideUiForPip(false)
            pipRequested = false
        }
    }

    private fun hideUiForPip(hide: Boolean) {
        // في PiP بنخفي الأزرار العايمة بس، الـ WebView يفضل ظاهر
        try {
            // لو فيه زرار Install أو Connect ظاهرين، اخفيهم في PiP
            // الـ WebView نفسه مش هيتخفي أبدا
        } catch (e: Exception) {}
    }

    override fun onPause() {
        // مهم: لو في PiP متوقفش الـ WebView - خلي الفيديو مكمل
        if (!isInPip) super.onPause()
        // في PiP: متعملش pause للـ WebView
    }

    override fun onResume() {
        super.onResume()
        pipRequested = false
    }

    private fun showDisconnected() {
        welcomeView.visibility = View.GONE
        welcomeConnectedView.visibility = View.GONE
        webView.visibility = View.GONE
        disconnectedView.visibility = View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // لو في Fullscreen للفيديو، اخرج منه الأول بدل ما تطلع من التطبيق
        if (customView != null) { (webView.webChromeClient as? android.webkit.WebChromeClient)?.onHideCustomView(); return } {
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
