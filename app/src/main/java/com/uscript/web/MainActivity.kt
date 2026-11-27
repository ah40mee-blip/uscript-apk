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
    private lateinit var configureBtn: Button // بقى Connect Panel - بيعمل Configure + دخول أوتوماتيك
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
    private var autoRetrying = false
    private var installRunning = false // التثبيت شغال - Enjoy متفتحش البانل قبل ما يخلص // عشان الـ Auto-Retry ميتكررش لانهائي لو السيرفر فعلا واقع
    private var barAnim: android.animation.ValueAnimator? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun box(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(Color.parseColor("#000000")) // خانة IP سودة - تالت حاجة
        setStroke(dp(1), Color.parseColor("#80FFFFFF")) // حافة زجاجية شفافة
    }

    private fun field(hint: String, text: String, isPw: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint; setText(text)
            setHintTextColor(Color.parseColor("#8A8A8A")); setTextColor(Color.parseColor("#B0FFFFFF"))
            textSize = 16f; gravity = Gravity.CENTER
            if (isPw) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
            background = box(); setPadding(dp(16), dp(16), dp(16), dp(16))
        }
    }

    // لوجو برميوم: دائرة جريدينت + حرف U + هالة
    // لوجو برميوم أكتر: هالة خارجية + حلقتين + جريدينت 4 ألوان + ظل + لمعة
    private fun premiumLogo(sizeDp: Int, logoTextSize: Float): FrameLayout {
        // اللوجو المتفق عليه: دايرة أسود جلوسي + U جراي + لمسة أحمر - مش دايرة حمرا متدرجة
        val halo = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp + 28), dp(sizeDp + 28)).apply { gravity = Gravity.CENTER; topMargin = dp(10); bottomMargin = dp(10) }
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#0A0A0A")); setStroke(dp(1), Color.parseColor("#80FFFFFF")) }
            elevation = dp(20).toFloat()
        }
        val outer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp)).apply { gravity = Gravity.CENTER }
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#0A0A0A")); setStroke(dp(1), Color.parseColor("#80FFFFFF")) }
            elevation = dp(16).toFloat()
        }
        val mid = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(sizeDp - 10), dp(sizeDp - 10)).apply { gravity = Gravity.CENTER }
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#080808"), Color.parseColor("#0A0A0A"), Color.parseColor("#000000"))).apply { shape = GradientDrawable.OVAL }
        }
        val inner = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(sizeDp - 26), dp(sizeDp - 26)).apply { gravity = Gravity.CENTER }
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#0A0A0A"), Color.parseColor("#0A0A0A"))).apply { shape = GradientDrawable.OVAL }
        }
        val letter = TextView(this).apply { text = "U"; this.textSize = logoTextSize; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER; setTypeface(null, android.graphics.Typeface.BOLD); elevation = dp(4).toFloat(); setShadowLayer(12f, 0f, 4f, Color.parseColor("#80FFFFFF")) }
        val shine = View(this).apply { layoutParams = FrameLayout.LayoutParams(dp(sizeDp / 3), dp(sizeDp / 6)).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; topMargin = dp(10) }; background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#FFFFFF")); alpha = 60 } }
        inner.addView(letter); inner.addView(shine); mid.addView(inner); outer.addView(mid); halo.addView(outer)
        return halo
    }
    // شاشة Enjoy Ur Premium Experience - استبدال كامل: مفيش فريم أحمر خالص
    // الخلفية سودة رخامي مموّجة - اللوجو Fade In الأول - بعدين Enjoy Ur Premium Experience Fade In
    private fun showConfiguredDialog() {
        val dlg = android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        // خلفية سودة رخامي مموّجة - ملء الشاشة - من غير كارت ومن غير فريم
        val bg = FrameLayout(this).apply { background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#000000"), Color.parseColor("#080808"), Color.parseColor("#030303"), Color.parseColor("#0A0A0A"), Color.parseColor("#000000"))) } // خلفية أغمق
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(28), dp(28), dp(28), dp(28)); layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER } } // الكتابة في نص الشاشة بالظبط - منظم
        val enjoyLogo = premiumLogo(200, 115f).apply { alpha = 0f } // كبر اللوجو شوية - نفس الأولى // يبدأ مخفي - Fade In الأول
        val enjoyText = TextView(this).apply { text = "Enjoy Ur Premium\nExperience"; textSize = 26f; setTextColor(Color.parseColor("#F2FFFFFF")); gravity = Gravity.CENTER; setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)); letterSpacing = 0.08f; setPadding(0, dp(16), 0, 0); setShadowLayer(18f, 0f, 6f, Color.parseColor("#A0FFFFFF")); alpha = 0f } // يبدأ مخفي - Fade In التاني // رمز شيك يمين وشمال الكتابة
        val enjoySub = TextView(this).apply { text = "Your Enigma is connected"; textSize = 12f; setTextColor(Color.parseColor("#9AA4C0")); gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(22)); alpha = 0f }
        // كارت زجاجي للكتابة - Enjoy + Sub جواه
        val enjoyCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10)) // الكارت اتصغر
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14); leftMargin = dp(32); rightMargin = dp(32) } // أضيق من الجناب - أصغر
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#1AFFFFFF"), Color.parseColor("#0A0A0A"), Color.parseColor("#000000"))).apply { cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.parseColor("#80FFFFFF")) }
            elevation = dp(12).toFloat(); alpha = 0f // يبدأ مخفي - يظهر بعد اختفاء اللوجو
            val lineTop = View(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(dp(48), dp(1)).apply { bottomMargin = dp(12) }; background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#00FFFFFF"), Color.parseColor("#CCFFFFFF"), Color.parseColor("#00FFFFFF"))) }
            val lineBottom = View(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(dp(48), dp(1)).apply { topMargin = dp(12) }; background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#00FFFFFF"), Color.parseColor("#CCFFFFFF"), Color.parseColor("#00FFFFFF"))) }
            addView(lineTop); addView(enjoyText); addView(lineBottom); addView(enjoySub)
        }
        col.addView(enjoyLogo); col.addView(enjoyCard)
        bg.addView(col)
        dlg.setContentView(bg)
        dlg.setCancelable(true)
        dlg.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)) // الأركان البيضا اتشالت - الخلفية الرخامي مالية الشاشة كلها
        dlg.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT) // ملء الشاشة - مفيش أبيض في الأركان
        dlg.show()
        // التسلسل: 1) اللوجو Fade In - 2) اللوجو Fade Out - 3) Enjoy تدخل لوحدها
        // 1) اللوجو Fade In - 900ms + تكبير خفيف
        enjoyLogo.animate().alpha(1f).scaleX(1.05f).scaleY(1.05f).setDuration(900).withEndAction {
            enjoyLogo.animate().scaleX(1f).scaleY(1f).setDuration(400).start()
            // ثبات نص ثانية - وبعدين Fade Out
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                // 2) اللوجو Fade Out - 800ms - يختفي خالص
                enjoyLogo.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(800).withEndAction {
                    enjoyLogo.visibility = View.GONE
                    // 3) Enjoy تدخل لوحدها - Fade In + طلوع خفيف من تحت
                    enjoyCard.animate().alpha(1f).translationY(14f).setDuration(800).withEndAction { enjoyCard.animate().translationY(0f).setDuration(500).start() }.start()
                    enjoyText.animate().alpha(1f).translationY(14f).setDuration(800).withEndAction {
                        enjoyText.animate().translationY(0f).setDuration(500).start()
                        enjoyText.animate().scaleX(1.04f).scaleY(1.04f).setDuration(800).withEndAction { enjoyText.animate().scaleX(1f).scaleY(1f).setDuration(800).start() }.start()
                        enjoyText.animate().translationY(-6f).setDuration(700).withEndAction { enjoyText.animate().translationY(0f).setDuration(700).start() }.start()
                    }.start()
                    enjoySub.animate().alpha(1f).setDuration(800).setStartDelay(400).start()
                }.start()
            }, 500)
        }.start()
        // 5 ثواني وبعدها دخول أوتوماتيك على البانل - من غير زراير
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ dlg.dismiss(); if (!installRunning) { try { nativeEnterPanel() } catch (e: Exception) {} } }, 6500) // 6500 عشان Enjoy تتلحق بعد اللوجو - لو التثبيت لسه شغال متفتحش البانل متفتحش البانل - هو اللي هيفتحها لما يخلص + يعيد تشغيل السيرفر
    }

    private fun nativeEnterPanel() { onEnterPanelClicked() }

    private fun btn(text: String = "Connect", color: String = "#0A0A0A", onClick: () -> Unit = {}): Button {
        return Button(this).apply {
            this.text = text; textSize = 17f; setTextColor(Color.parseColor("#FFFFFF")); setTypeface(null, android.graphics.Typeface.BOLD); letterSpacing = 0.04f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(16) }
            // زرار برميوم: تدرج أسود جلوسي + حافة زجاجية شفافة + لمعة
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#2A2A2A"), Color.parseColor("#0A0A0A"), Color.parseColor("#000000"))).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.parseColor("#80FFFFFF")) }
            elevation = dp(12).toFloat(); setShadowLayer(12f, 0f, 4f, Color.parseColor("#80FFFFFF"))
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

        val root = FrameLayout(this).apply { background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#000000"), Color.parseColor("#080808"), Color.parseColor("#030303"), Color.parseColor("#0A0A0A"), Color.parseColor("#000000"))) } // خلفية أغمق // خلفية سودة مموّجة رخامي - أول شاشتين

        // ===== 1) Splash متحرك =====
        val splashLogo = premiumLogo(200, 115f) // كبر اللوجو شوية - أولاً
        val splashTitle = TextView(this).apply { text = "U-Panel"; textSize = 42f; typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD); letterSpacing = 0.06f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER; setShadowLayer(12f, 0f, 4f, Color.parseColor("#80FFFFFF")); setShadowLayer(10f, 0f, 6f, Color.parseColor("#FFFFFF")) } // أسود جلوسي بلمسة فوشيا
        val splashSub = TextView(this).apply { text = "Control Ur Enigma"; textSize = 26f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER; setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)); letterSpacing = 0.04f; setShadowLayer(12f, 0f, 4f, Color.parseColor("#80FFFFFF")); setShadowLayer(10f, 0f, 6f, Color.parseColor("#FFFFFF")) } // الكتابة أسود جلوسي بلمسة فوشيا
        // شاشة البداية اللي قبل دي: اللوجو والكتابة في النص - من غير Enigma2
        val splashCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(splashLogo); addView(splashTitle); addView(splashSub)
        }
        // كارت زجاجي By Eng Ahmed Abdel Moati - في أول شاشة تحت خالص
        val splashCredit = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; bottomMargin = dp(28); leftMargin = dp(24); rightMargin = dp(24) }
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#1AFFFFFF"), Color.parseColor("#0A0A0A"), Color.parseColor("#000000"))).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.parseColor("#80FFFFFF")) }
            elevation = dp(10).toFloat()
            addView(TextView(this@MainActivity).apply { text = "By Eng Ahmed Abdel Moati"; textSize = 15f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER; setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)); letterSpacing = 0.03f; setShadowLayer(10f, 0f, 4f, Color.parseColor("#80FFFFFF")) })
        }
        splashView = FrameLayout(this).apply { addView(splashCol); addView(splashCredit) }

        // ===== 2) صفحة الخانات =====
        val formLogo = premiumLogo(125, 70f)
        val title = TextView(this).apply { text = "U-Panel"; textSize = 42f; typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD); letterSpacing = 0.06f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER }
        val sub = TextView(this).apply { text = "Control Ur Enigma"; textSize = 18f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER }
        ipField = field("Receiver IP  ( 192.168.1.45 )", savedIp)
        // البورت اتشال من الشاشة - ثابت على 8080
        usernameField = field("Username  ( root )", savedUser)
        passwordField = field("Password  ( root )", savedPw, true)
        statusText = TextView(this).apply { text = ""; textSize = 14f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER }
        connectBtn = btn("CONNECT", "#0A0A0A") { onConnectClicked() }
        configureBtn = btn("Connect to your Enigma", "#0A0A0A") { onConnectPanelClicked() } // جلوسي أسود - بيعمل Configure + دخول أوتوماتيك
        enterPanelBtn = btn("Enter U-Panel", "#1E2A55") { onEnterPanelClicked() }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(48), dp(24), dp(28)) // قايمة أكبر وأشيك
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            addView(formLogo); addView(title); addView(sub); addView(ipField) // من غير خانة بورت - البورت ثابت 8080
            addView(configureBtn); addView(statusText) // الكارت اتشال من هنا - اتنقل لأول شاشة
        }
        ipView = ScrollView(this).apply { addView(form); visibility = View.GONE }

        // ===== 3) شاشة Enjoy - أسود جلوسي زجاجي + بار متحرك =====
        val wLogo = premiumLogo(135, 76f)
        welcomeTitle = TextView(this).apply { text = "U-Panel"; textSize = 34f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER }
        welcomeConnecting = TextView(this).apply { text = "Connecting..."; textSize = 20f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER }
        val track = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(320), dp(28)).apply { topMargin = dp(20); gravity = Gravity.CENTER }
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.parseColor("#0A0A0A")); setStroke(dp(1), Color.parseColor("#80FFFFFF")) }
            elevation = dp(8).toFloat()
        } // بار تخين 28dp - أسود جلوسي الغالب عليه أسود - لمسة أحمر رفيعة بس
        progressFill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, dp(28))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#0A0A0A"), Color.parseColor("#0A0A0A"), Color.parseColor("#000000"))).apply { cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.parseColor("#80FFFFFF")) }
        } // الملء أسود جلوسي - لمسة أحمر خفيفة بس على الحافة
        track.addView(progressFill)
        val wCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(wLogo); addView(welcomeTitle); addView(welcomeConnecting); addView(track)
        }
        welcomeView = FrameLayout(this).apply { addView(wCol); visibility = View.GONE } // شاشة Welcome اتشالت خالص - مش هتظهر أبداً

        // ===== 4) شاشة Disconnected - ثابتة =====
        val dTitle = TextView(this).apply { text = "Disconnected"; textSize = 26f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER }
        val dSub = TextView(this).apply { text = "Check connection and try again"; textSize = 14f; setTextColor(Color.parseColor("#B0FFFFFF")); gravity = Gravity.CENTER }
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
                        // شاشة Welcome اتشالت - مفيش Server dropped screen
                        Thread {
                            val (_, _, pw) = saveFields(); val (ip2, _, _) = saveFields()
                            val ok = try { ScriptInstaller.restartServer(this@MainActivity, ip2, pw) } catch (e2: Exception) { false }
                            runOnUiThread {
                                stopBar(); welcomeView.visibility = View.GONE; autoRetrying = false
                                if (ok) { webView.visibility = View.VISIBLE; webView.loadUrl(baseUrl); Toast.makeText(this@MainActivity, "Server restarted", Toast.LENGTH_SHORT).show() }
                                else { ipView.visibility = View.VISIBLE; statusText.setTextColor(Color.parseColor("#B0FFFFFF")); statusText.text = "Cannot reach $baseUrl - دوس ENTER PANEL تاني" }
                            }
                        }.start()
                    }
                }
            }
        }

        root.addView(splashView); root.addView(ipView); root.addView(welcomeView); root.addView(discView); root.addView(webView)
        setContentView(root)

        // أنيميشن الـ Splash - الكلام متحرك
        splashLogo.alpha = 0f; splashLogo.scaleX = 0.7f; splashLogo.scaleY = 0.7f
        splashTitle.alpha = 0f; splashSub.alpha = 0f
        splashLogo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).setInterpolator(OvershootInterpolator(1.3f)).start()
        // الكلام متحرك: Fade In + تكبير خفيف + نبضة لمعة مستمرة
        splashTitle.animate().alpha(1f).scaleX(1.05f).scaleY(1.05f).setDuration(700).setStartDelay(150).withEndAction {
            splashTitle.animate().scaleX(1f).scaleY(1f).setDuration(600).start()
            // نبضة مستمرة: U-Panel يكبر ويصغر بخفة طول الـ 5 ثواني
            splashTitle.animate().scaleX(1.04f).scaleY(1.04f).setDuration(800).withEndAction { splashTitle.animate().scaleX(1f).scaleY(1f).setDuration(800).start() }.start()
        }.start()
        splashSub.animate().alpha(1f).translationY(12f).setDuration(700).setStartDelay(300).withEndAction {
            splashSub.animate().translationY(0f).setDuration(500).start()
            // Control Ur Enigma متحرك: يطلع وينزل بخفة + لمعة
            splashSub.animate().translationY(-6f).setDuration(700).withEndAction { splashSub.animate().translationY(0f).setDuration(700).start() }.start()
        }.start()
        Handler(Looper.getMainLooper()).postDelayed({
            splashView.visibility = View.GONE; ipView.visibility = View.VISIBLE
        }, 5000) // Splash تقعد 5 ثواني
    }

    private fun saveFields(): Triple<String, String, String> {
        val ip = ipField.text.toString().trim()
        val port = "8080" // ثابت - خانة البورت اتشالت
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
                progressFill.layoutParams.width = (dp(300) * f).toInt()
                progressFill.requestLayout()
            }
        }
        barAnim?.start()
    }
    private fun stopBar() { barAnim?.cancel(); barAnim = null }

    private fun showWelcome(msg: String) {
        // اتشالت خالص - مفيش شاشة Welcome / Configuring / Waiting - بعد Enjoy يدخل على البانل على طول
        welcomeView.visibility = View.GONE
    }

    // CONNECT: اختبار بس - يفضل في الصفحة ويقول Connected وميدخلش البانل
    private fun onConnectClicked() {
        val (ip, port, _) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        statusText.setTextColor(Color.parseColor("#B0FFFFFF")); statusText.text = "Connecting..."
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
                else { statusText.setTextColor(Color.parseColor("#B0FFFFFF")); statusText.text = "Cannot reach $ip - لو لسه عامل ريستارت استنى دقيقة وجرب تاني" }
            }
        }.start()
    }

    // ENTER PANEL: افتح البانل على طول - من غير SSH
    // سيرفر الـ WebUI بتاع Enigma2 بيشتغل لوحده، مش محتاج أمر start
    // ENTER: استنى السيرفر يقوم الأول - جرّب 8080 لحد 30 ثانية قبل ما يفتح
    private fun onEnterPanelClicked() {
        val (ip, port, _) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        // افتح البانل فوراً - متستناش السوكت - الشاشة السودة كانت من الانتظار 90 ثانية
        ipView.visibility = View.GONE; discView.visibility = View.GONE; welcomeView.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(baseUrl) // الـ WebView نفسه هيستنى السيرفر وهيعمل Retry لو لسه بيقوم
    }

    // Connect Panel: بيعمل اللي كان بيعمله Configure + بيدخل أوتوماتيك
    // بمجرد Connect: شاشة Enjoy Ur Premium Experience - 5 ثواني - دخول أوتوماتيك
    private fun onConnectPanelClicked() {
        val (ip, _, pw) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        // مفيش شاشة Configuring خالص - بمجرد ما تدوس Connect تظهر شاشة Enjoy Ur Premium Experience
        ipView.visibility = View.GONE
        installRunning = true
        showConfiguredDialog()
        Thread {
            // 1) زرع البانل (install) - 2) تشغيل السيرفر (start) - 3) إعادة تشغيل السيرفر (reboot) - كل ده جوه ScriptInstaller
            val (ok, err) = ScriptInstaller.installWithError(this, ip, pw) { _ -> }
            // 4) استنى السيرفر يقوم بعد الـ Reboot - لحد 90 ثانية
            var up = false
            for (i in 1..45) {
                try { val s = java.net.Socket(); s.connect(java.net.InetSocketAddress(ip, 8080), 2000); s.close(); up = true; break } catch (e: Exception) { Thread.sleep(2000) }
            }
            installRunning = false
            runOnUiThread {
                if (!ok) { ipView.visibility = View.VISIBLE; statusText.setTextColor(Color.parseColor("#B0FFFFFF")); statusText.text = "Configure failed: $err" }
                else if (!up) { ipView.visibility = View.VISIBLE; statusText.setTextColor(Color.parseColor("#B0FFFFFF")); statusText.text = "Server did not come up on 8080 - اتأكد ان الموبايل على نفس الـ WiFi والرسيفر شغال" }
                else { onEnterPanelClicked() } // السيرفر قام - افتح البانل
            }
        }.start()
    }

    private fun onConfigureClicked() {
        val (ip, _, pw) = saveFields()
        if (ip.isEmpty()) { statusText.text = "Please enter the receiver IP"; return }
        ipView.visibility = View.GONE
        showConfiguredDialog()
        Thread {
            val (ok, err) = ScriptInstaller.installWithError(this, ip, pw) { _ -> }
            runOnUiThread {
                if (!ok) { statusText.setTextColor(Color.parseColor("#B0FFFFFF")); statusText.text = "Configure failed: $err" }
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