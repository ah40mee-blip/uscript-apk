package com.uscript.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var connectView: View
    private lateinit var welcomeView: View
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeSub: TextView
    private lateinit var webView: WebView
    private lateinit var disconnectView: View
    private lateinit var etIp: EditText
    private lateinit var etPort: EditText

    private var currentUrl: String = ""
    private var showingDisconnected = false

    private val prefs by lazy { getSharedPreferences("uscript", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
        }

        // ===== شاشة إدخال الـ IP =====
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(48), dp(28), dp(28))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(this).apply {
            text = "U-Script"
            textSize = 46f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F472B6"))
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "Control Enigma"
            textSize = 17f
            setTextColor(Color.parseColor("#22D3EE"))
            gravity = Gravity.CENTER
        }
        val hint = TextView(this).apply {
            text = "Enter your receiver IP to connect"
            textSize = 14f
            setTextColor(Color.parseColor("#8A93B2"))
            gravity = Gravity.CENTER
        }

        etIp = styledEditText("Receiver IP  •  e.g. 192.168.1.45").apply {
            inputType = InputType.TYPE_CLASS_PHONE
            setText(prefs.getString("last_ip", "192.168.1.45"))
        }
        etPort = styledEditText("Port  •  e.g. 8080").apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getString("last_port", "8080"))
        }

        val btnConnect = Button(this).apply {
            text = "CONNECT"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundedBg("#F472B6", 16)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener { onConnectClicked() }
        }

        form.addView(title)
        form.addView(sub)
        form.addView(spacer(10))
        form.addView(hint)
        form.addView(spacer(36))
        form.addView(etIp)
        form.addView(spacer(16))
        form.addView(etPort)
        form.addView(spacer(28))
        form.addView(btnConnect, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        scroll.addView(form)
        connectView = scroll

        // ===== 1️⃣ شاشة الترحيب المتحركة Fade In =====
        val welcomeBox = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
            visibility = View.GONE
        }
        val welcomeCenter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        welcomeTitle = TextView(this).apply {
            text = "U-Script"
            textSize = 58f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F472B6"))
            gravity = Gravity.CENTER
            alpha = 0f
        }
        welcomeSub = TextView(this).apply {
            text = "Control Enigma World"
            textSize = 21f
            setTextColor(Color.parseColor("#22D3EE"))
            gravity = Gravity.CENTER
            alpha = 0f
        }
        welcomeCenter.addView(welcomeTitle)
        welcomeCenter.addView(spacer(10))
        welcomeCenter.addView(welcomeSub)
        welcomeBox.addView(welcomeCenter)
        welcomeView = welcomeBox

        // ===== الـ WebView (الداشبورد) =====
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // بداية تحميل جديد = إخفاء رسالة الانقطاع
                    showingDisconnected = false
                    disconnectView.visibility = View.GONE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // لو الصفحة حملت بنجاح، تأكد إن رسالة الانقطاع مخفية
                    if (!showingDisconnected) {
                        disconnectView.visibility = View.GONE
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    // بس لو الخطأ في الصفحة الرئيسية مش ملف فرعي
                    if (request?.isForMainFrame == true) {
                        showDisconnected()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    showDisconnected()
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                        showDisconnected()
                    }
                }
            }
        }

        // ===== 2️⃣ شاشة الانقطاع (بدل الصفحة البيضا) =====
        val disconnectBox = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
            visibility = View.GONE
        }
        val disconnectCenter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(24), dp(28), dp(24))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        val disconnectTitle = TextView(this).apply {
            text = "U-Script Panel is disconnected"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F472B6"))
            gravity = Gravity.CENTER
        }
        val disconnectHint = TextView(this).apply {
            text = "Make sure the receiver is ON and on the same Wi-Fi"
            textSize = 14f
            setTextColor(Color.parseColor("#8A93B2"))
            gravity = Gravity.CENTER
        }

        // ===== 3️⃣ زرار Try Again - وردي وكبير =====
        val btnRetry = Button(this).apply {
            text = "↻  Try Again"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundedBg("#F472B6", 18)
            setPadding(dp(18), dp(20), dp(18), dp(20))
            isAllCaps = false
            setOnClickListener { retryConnection() }
        }

        disconnectCenter.addView(disconnectTitle)
        disconnectCenter.addView(spacer(12))
        disconnectCenter.addView(disconnectHint)
        disconnectCenter.addView(spacer(28))
        disconnectCenter.addView(btnRetry, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        disconnectBox.addView(disconnectCenter)
        disconnectView = disconnectBox

        root.addView(connectView)
        root.addView(welcomeView)
        root.addView(webView)
        root.addView(disconnectView)
        setContentView(root)
    }

    private fun onConnectClicked() {
        val ip = etIp.text.toString().trim()
        val port = etPort.text.toString().trim().ifEmpty { "8080" }

        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter the receiver IP", Toast.LENGTH_SHORT).show()
            return
        }

        // إخفاء الكيبورد
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(etIp.windowToken, 0)

        // حفظ الـ IP والبورت
        prefs.edit()
            .putString("last_ip", ip)
            .putString("last_port", port)
            .apply()

        currentUrl = "http://$ip:$port"

        // إخفاء شاشة الإدخال + إظهار الترحيب
        connectView.visibility = View.GONE
        disconnectView.visibility = View.GONE
        welcomeView.visibility = View.VISIBLE
        welcomeTitle.alpha = 0f
        welcomeSub.alpha = 0f

        // Fade In: U-Script الأول
        welcomeTitle.animate()
            .alpha(1f)
            .setDuration(900)
            .setStartDelay(100)
            .start()

        // Fade In: Control Enigma World بعدها
        welcomeSub.animate()
            .alpha(1f)
            .setDuration(900)
            .setStartDelay(700)
            .start()

        // بعد 3 ثواني: إخفاء الترحيب + فتح الداشبورد
        handler.postDelayed({
            welcomeView.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    welcomeView.visibility = View.GONE
                    welcomeView.alpha = 1f
                    webView.visibility = View.VISIBLE
                    webView.loadUrl(currentUrl)
                }
                .start()
        }, 3000)
    }

    private fun retryConnection() {
        // إعادة المحاولة بنفس الـ IP والبورت
        disconnectView.visibility = View.GONE
        showingDisconnected = false
        webView.visibility = View.VISIBLE
        webView.loadUrl(currentUrl)
    }

    private fun showDisconnected() {
        runOnUiThread {
            showingDisconnected = true
            webView.visibility = View.GONE
            welcomeView.visibility = View.GONE
            disconnectView.visibility = View.VISIBLE
        }
    }

    private fun styledEditText(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#8A93B2"))
            background = roundedBg("#131A33", 14)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun roundedBg(colorHex: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun spacer(heightDp: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(heightDp)
            )
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        when {
            disconnectView.visibility == View.VISIBLE -> {
                // من شاشة الانقطاع: رجوع لشاشة إدخال الـ IP
                disconnectView.visibility = View.GONE
                connectView.visibility = View.VISIBLE
            }
            webView.visibility == View.VISIBLE && webView.canGoBack() -> webView.goBack()
            webView.visibility == View.VISIBLE -> {
                // رجوع لشاشة إدخال الـ IP
                webView.visibility = View.GONE
                connectView.visibility = View.VISIBLE
            }
            else -> super.onBackPressed()
        }
    }
}
