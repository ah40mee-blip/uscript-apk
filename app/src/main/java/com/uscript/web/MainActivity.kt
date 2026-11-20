package com.uscript.web

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var setupLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 160, 48, 48)
        }

        val welcome = TextView(this).apply {
            text = "أهلاً بيك في U-Script"
            textSize = 24f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        val hint = TextView(this).apply {
            text = "دخل IP الريسيفر والبورت عشان نتصل"
            textSize = 16f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 32, 0, 32)
        }

        val ipInput = EditText(this).apply {
            hint = "IP الريسيفر"
            setText("192.168.1.45")
        }

        val portInput = EditText(this).apply {
            hint = "البورت"
            setText("8080")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val connectBtn = Button(this).apply {
            text = "اتصل"
        }

        setupLayout.addView(welcome)
        setupLayout.addView(hint)
        setupLayout.addView(ipInput)
        setupLayout.addView(portInput)
        setupLayout.addView(connectBtn)

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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(setupLayout)
            addView(webView)
        }

        setContentView(root)

        connectBtn.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            val port = portInput.text.toString().trim().ifEmpty { "8080" }
            if (ip.isNotEmpty()) {
                setupLayout.visibility = View.GONE
                webView.visibility = View.VISIBLE
                webView.loadUrl("http://$ip:$port")
            }
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
