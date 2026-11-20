package com.uscript.web

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var setupLayout: ScrollView
    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText
    private lateinit var statusText: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun gradientBg(colors: IntArray, radius: Float): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = radius
        }
    }

    private fun cardBg(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            setStroke(dp(1), Color.parseColor("#1E293B"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("uscript", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("ip", "192.168.1.45") ?: "192.168.1.45"
        val savedPort = prefs.getString("port", "8080") ?: "8080"

        // ===== البانر =====
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradientBg(
                intArrayOf(
                    Color.parseColor("#A855F7"),
                    Color.parseColor("#EC4899"),
                    Color.parseColor("#22D3EE")
                ), dp(24).toFloat()
            )
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }
        val titleText = TextView(this).apply {
            text = "U-Script"
            textSize = 44f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subText = TextView(this).apply {
            text = "Control Enigma"
            textSize = 20f
            setTextColor(Color.parseColor("#E0E7FF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        banner.addView(titleText)
        banner.addView(subText)

        // ===== حقول الإدخال =====
        val detailsLabel = TextView(this).apply {
            text = "Connection Details"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#7DD3FC"))
        }
        val ipLabel = TextView(this).apply {
            text = "Receiver IP"
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(24), 0, dp(8))
        }
        ipInput = EditText(this).apply {
            this.hint = "192.168.1.45"
            setText(savedIp)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 20f
            isSingleLine = true
            background = cardBg(Color.parseColor("#0B1130"), dp(14).toFloat())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val portLabel = TextView(this).apply {
            text = "Port"
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(20), 0, dp(8))
        }
        portInput = EditText(this).apply {
            this.hint = "8080"
            setText(savedPort)
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 20f
            isSingleLine = true
            background = cardBg(Color.parseColor("#0B1130"), dp(14).toFloat())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // ===== زرار الاتصال =====
        val connectBtn = TextView(this).apply {
            text = "CONNECT"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = gradientBg(
                intArrayOf(
                    Color.parseColor("#EC4899"),
                    Color.parseColor("#8B5CF6"),
                    Color.parseColor("#22D3EE")
                ), dp(16).toFloat()
            )
            setPadding(dp(16), dp(18), dp(16), dp(18))
        }
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(28) }
        connectBtn.layoutParams = btnParams

        statusText = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.parseColor("#F87171"))
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }
        val footNote = TextView(this).apply {
            text = "Ensure device is on the same network"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }

        // ===== الكارت =====
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg(Color.parseColor("#121A3A"), dp(24).toFloat())
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        card.addView(detailsLabel)
        card.addView(ipLabel)
        card.addView(ipInput)
        card.addView(portLabel)
        card.addView(portInput)
        card.addView(connectBtn)
        card.addView(statusText)
        card.addView(footNote)

        // ===== المحتوى =====
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }
        content.addView(banner)
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(24)
            )
        }
        content.addView(spacer)
        content.addView(card)

        setupLayout = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#070B1F"))
            addView(content)
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(setupLayout)
            addView(webView)
        }
        setContentView(container)

        connectBtn.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            val port = portInput.text.toString().trim().ifEmpty { "8080" }
            if (ip.isEmpty()) {
                statusText.text = "Please enter the Receiver IP"
                return@setOnClickListener
            }
            prefs.edit().putString("ip", ip).putString("port", port).apply()
            statusText.text = ""
            setupLayout.visibility = View.GONE
            webView.visibility = View.VISIBLE
            webView.loadUrl("http://$ip:$port")
        }
    }

    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
            webView.goBack()
        } else if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
            setupLayout.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }
}
