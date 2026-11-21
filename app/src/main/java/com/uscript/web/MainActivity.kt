package com.uscript.web

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var connectView: View
    private lateinit var welcomeView: View
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeSub: TextView
    private lateinit var welcomeRing: View
    private lateinit var welcomeDivider: View
    private lateinit var welcomeConnecting: TextView
    private lateinit var progressFill: View
    private lateinit var webView: WebView
    private lateinit var disconnectView: View
    private lateinit var etIp: EditText
    private lateinit var etPort: EditText

    private var currentUrl: String = ""
    private var currentIp: String = ""
    private var currentPort: Int = 8080
    private var showingDisconnected = false
    private var loadFailed = false

    // نتيجة فحص الاتصال قبل فتح الـ WebView
    @Volatile private var checkDone = false
    @Volatile private var checkOk = false

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

        // ===== شاشة الترحيب البريميوم =====
        val welcomeBox = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070B1F"))
            visibility = View.GONE
        }

        // حلقة زخرفية حوالين العنوان
        welcomeRing = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(230), dp(230), Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), Color.parseColor("#F472B6"))
            }
            alpha = 0f
        }

        val welcomeCenter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
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
            scaleX = 0.85f
            scaleY = 0.85f
        }
        // خط متدرج وردي -> سماوي
        welcomeDivider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(120), dp(3)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE"))
            ).apply { cornerRadius = dp(2).toFloat() }
            alpha = 0f
        }
        welcomeSub = TextView(this).apply {
            text = "Control Enigma World"
            textSize = 21f
            setTextColor(Color.parseColor("#22D3EE"))
            gravity = Gravity.CENTER
            alpha = 0f
            translationY = dp(24).toFloat()
        }
        welcomeConnecting = TextView(this).apply {
            text = "Connecting to your receiver..."
            textSize = 13f
            setTextColor(Color.parseColor("#8A93B2"))
            gravity = Gravity.CENTER
            alpha = 0f
        }
        // شريط تقدم بريميوم
        val progressTrack = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(220), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = roundedBg("#1E2A4A", 2)
        }
        progressFill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#F472B6"), Color.parseColor("#22D3EE"))
            ).apply { cornerRadius = dp(2).toFloat() }
        }
        progressTrack.addView(progressFill)

        welcomeCenter.addView(welcomeTitle)
        welcomeCenter.addView(spacer(16))
        welcomeCenter.addView(welcomeDivider)
        welcomeCenter.addView(spacer(16))
        welcomeCenter.addView(welcomeSub)
        welcomeCenter.addView(spacer(36))
        welcomeCenter.addView(welcomeConnecting)
        welcomeCenter.addView(spacer(14))
        welcomeCenter.addView(progressTrack)

        welcomeBox.addView(welcomeRing)
        welcomeBox.addView(welcomeCenter)
        welcomeView = welcomeBox

        // ===== الـ WebView (الداشبورد) =====
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // مخفي تمامًا لحد ما الصفحة تحمل بنجاح - عشان الصفحة البيضا متظهرش أبدًا
            visibility = View.INVISIBLE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    loadFailed = false
                    showingDisconnected = false
                    disconnectView.visibility = View.GONE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url == "about:blank") return
                    if (loadFailed) {
                        showDisconnected()
                    } else {
                        // الصفحة حملت بنجاح = أظهر الـ WebView
                        disconnectView.visibility = View.GONE
                        webView.visibility = View.VISIBLE
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
                        view?.stopLoading()
                        view?.loadUrl("about:blank")
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
                    loadFailed = true
                    view?.stopLoading()
                    view?.loadUrl("about:blank")
                    showDisconnected()
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                        loadFailed = true
                        view?.stopLoading()
                        view?.loadUrl("about:blank")
                        showDisconnected()
                    }
                }
            }
        }

        // ===== شاشة الانقطاع (بدل الصفحة البيضا) =====
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
        val portStr = etPort.text.toString().trim().ifEmpty { "8080" }

        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter the receiver IP", Toast.LENGTH_SHORT).show()
            return
        }

        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(etIp.windowToken, 0)

        prefs.edit()
            .putString("last_ip", ip)
            .putString("last_port", portStr)
            .apply()

        currentIp = ip
        currentPort = portStr.toIntOrNull() ?: 8080
        currentUrl = "http://$ip:$currentPort"

        // إخفاء شاشة الإدخال + إظهار الترحيب البريميوم
        connectView.visibility = View.GONE
        disconnectView.visibility = View.GONE
        welcomeView.visibility = View.VISIBLE
        welcomeTitle.alpha = 0f
        welcomeTitle.scaleX = 0.85f
        welcomeTitle.scaleY = 0.85f
        welcomeDivider.alpha = 0f
        welcomeSub.alpha = 0f
        welcomeSub.translationY = dp(24).toFloat()
        welcomeConnecting.alpha = 0f
        welcomeRing.alpha = 0f
        welcomeRing.scaleX = 0.9f
        welcomeRing.scaleY = 0.9f
        (progressFill.layoutParams as FrameLayout.LayoutParams).width = 0
        progressFill.requestLayout()

        // الحلقة الزخرفية
        welcomeRing.animate().alpha(0.45f).scaleX(1f).scaleY(1f)
            .setDuration(1200).setStartDelay(200).start()
        // U-Script: Fade In + تكبير ناعم
        welcomeTitle.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(900).setStartDelay(100).start()
        // الخط المتدرج
        welcomeDivider.animate().alpha(1f)
            .setDuration(700).setStartDelay(500).start()
        // Control Enigma World: Fade In + صعود
        welcomeSub.animate().alpha(1f).translationY(0f)
            .setDuration(900).setStartDelay(700).start()
        // نص الاتصال: نبض ناعم
        welcomeConnecting.animate().alpha(1f).setDuration(600).setStartDelay(1100).start()
        ObjectAnimator.ofFloat(welcomeConnecting, "alpha", 1f, 0.4f, 1f).apply {
            duration = 1400
            repeatCount = ObjectAnimator.INFINITE
            startDelay = 1400
            start()
        }
        // شريط التقدم: بيتملي خلال الترحيب
        ValueAnimator.ofInt(0, dp(220)).apply {
            duration = 2600
            startDelay = 300
            addUpdateListener {
                (progressFill.layoutParams as FrameLayout.LayoutParams).width = it.animatedValue as Int
                progressFill.requestLayout()
            }
            start()
        }

        // فحص الاتصال في الخلفية - قبل ما نفتح الـ WebView أصلًا
        checkDone = false
        checkOk = false
        startConnectionCheck()

        // بعد الترحيب: لو الاتصال تمام افتح الداشبورد، لو فاصل اظهر شاشة الانقطاع
        handler.postDelayed({ proceedAfterWelcome() }, 3000)
    }

    private fun startConnectionCheck() {
        Thread {
            var ok = false
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(currentIp, currentPort), 3000)
                    ok = true
                }
            } catch (e: Exception) {
                ok = false
            }
            checkOk = ok
            checkDone = true
        }.start()
    }

    private fun proceedAfterWelcome() {
        if (!checkDone) {
            // لسه الفحص مخلصش - استنى شوية وجرب تاني
            handler.postDelayed({ proceedAfterWelcome() }, 500)
            return
        }
        welcomeView.animate().alpha(0f).setDuration(400).withEndAction {
            welcomeView.visibility = View.GONE
            welcomeView.alpha = 1f
            if (checkOk) {
                loadFailed = false
                webView.visibility = View.INVISIBLE
                webView.loadUrl(currentUrl)
            } else {
                showDisconnected()
            }
        }.start()
    }

    private fun retryConnection() {
        // إعادة المحاولة: فحص الاتصال من جديد قبل ما نفتح الـ WebView
        disconnectView.visibility = View.GONE
        showingDisconnected = false
        loadFailed = false
        webView.visibility = View.INVISIBLE
        checkDone = false
        checkOk = false
        startConnectionCheck()
        handler.postDelayed({
            if (!checkDone) {
                handler.postDelayed({
                    if (checkOk) {
                        loadFailed = false
                        webView.visibility = View.INVISIBLE
                        webView.loadUrl(currentUrl)
                    } else showDisconnected()
                }, 800)
            } else if (checkOk) {
                loadFailed = false
                webView.visibility = View.INVISIBLE
                webView.loadUrl(currentUrl)
            } else showDisconnected()
        }, 600)
    }

    private fun showDisconnected() {
        runOnUiThread {
            showingDisconnected = true
            webView.visibility = View.INVISIBLE
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
                disconnectView.visibility = View.GONE
                connectView.visibility = View.VISIBLE
            }
            webView.visibility == View.VISIBLE && webView.canGoBack() -> webView.goBack()
            webView.visibility == View.VISIBLE -> {
                webView.visibility = View.INVISIBLE
                connectView.visibility = View.VISIBLE
            }
            else -> super.onBackPressed()
        }
    }
}
