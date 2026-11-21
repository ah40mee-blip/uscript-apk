// U-Script - آخر تعديل قبل الدمج (نقطة رجوع)
// دي النسخة اللي الشات ده واقف عندها - لو حبيت ترجع لها، انسخ الملف ده كله في MainActivity.kt وامسح ScriptInstaller.kt
// وبعدها Commit - Actions ✅ - امسح القديم - Restart - ثبت الجديد

package com.uscript.web
// U-Script BEFORE-MERGE - آخر تعديل قبل الدمج - Auto-Login - MainActivity.kt - توزيع منظم
// القايمة الأساسية: عرض الشاشة + ريموت | باقي القوايم: Backup / Channels / Streaming / Picons-EPG / Multiboot / Tools / Setting
// الاسكريبت نفسه مش متعدل - التطبيق بيكلم WebUI بتاعه على نفس IP:8080 + Web PIN
// المطلوب: 1) Manifest فيه usesCleartextTraffic + INTERNET  2) ملف ScriptInstaller.kt جنب الملف ده  3) الاسكريبت الأصلي في app/src/main/assets/uscript.sh
// ملف الاسكريبت: حط نسخة الاسكريبت الأصلية في app/src/main/assets/uscript.sh (من غير أي تعديل)

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() { // + ScriptInstaller.kt لازم يكون جنبه في نفس الفولدر

    private lateinit var root: FrameLayout
    private lateinit var prefs: android.content.SharedPreferences
    private var ip = "192.168.1.45"; private var port = "8080"; private var pin = ""
    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())

    // ألوان U-Script
    private val BG = Color.parseColor("#070B1F")
    private val CARD = Color.parseColor("#0D1330")
    private val BORDER = Color.parseColor("#2A3560")
    private val PINK = Color.parseColor("#E879F9")
    private val CYAN = Color.parseColor("#22D3EE")
    private val GOLD = Color.parseColor("#FFD166")
    private val GREEN = Color.parseColor("#4ADE80")
    private val MUTED = Color.parseColor("#9AA3C7")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("uscript", Context.MODE_PRIVATE)
        ip = prefs.getString("ip", "192.168.1.45") ?: "192.168.1.45"
        port = prefs.getString("port", "8080") ?: "8080"
        pin = prefs.getString("pin", "") ?: ""
        root = FrameLayout(this).apply { setBackgroundColor(BG); layoutParams = ViewGroup.LayoutParams(-1, -1) }
        setContentView(root)
        showSplash()
    }

    // ---------- أدوات UI ----------
    private fun bgDrawable(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable {
        return GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = radius; setColor(color); if (stroke > 0) setStroke(stroke, strokeColor) }
    }
    private fun txt(text: String, size: Float = 16f, color: Int = Color.WHITE, bold: Boolean = false): TextView {
        return TextView(this).apply { this.text = text; textSize = size; setTextColor(color); typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT }
    }
    private fun btn(text: String, bg: Int = CARD, fg: Int = Color.WHITE, radius: Float = 32f, stroke: Int = 0, strokeColor: Int = 0, onClick: () -> Unit): Button {
        return Button(this).apply { this.text = text; textSize = 15f; setTextColor(fg); typeface = Typeface.DEFAULT_BOLD; background = bgDrawable(bg, radius, stroke, strokeColor); setOnClickListener { onClick() }; stateListAnimator = null }
    }
    private fun input(hint: String, value: String): EditText {
        return EditText(this).apply { setHint(hint); setText(value); setHintTextColor(MUTED); setTextColor(Color.WHITE); textSize = 16f; background = bgDrawable(CARD, 28f, 2, BORDER); setPadding(40, 30, 40, 30) }
    }
    private fun clearRoot() { root.removeAllViews(); webView?.destroy(); webView = null }
    private fun centerWrap(view: View): FrameLayout {
        return FrameLayout(this).apply { addView(view, FrameLayout.LayoutParams(-1, -1).apply { gravity = Gravity.CENTER }) }
    }

    // ---------- 1) Splash 5 ثواني ----------
    private fun showSplash() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(60, 60, 60, 60) }
        lay.addView(txt("U", 90f, PINK, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("U-Script", 48f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("Control Enigma World", 18f, MUTED).apply { gravity = Gravity.CENTER })
        lay.addView(txt("E2", 14f, MUTED).apply { gravity = Gravity.CENTER })
        root.addView(centerWrap(lay))
        handler.postDelayed({ showConnect() }, 5000)
    }

    // ---------- 2) شاشة IP + Port + PIN ----------
    private fun showConnect() {
        clearRoot()
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(60, 60, 60, 60) }
        lay.addView(txt("U", 70f, PINK, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("U-Script", 40f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        val ipIn = input("Receiver IP  (192.168.1.45)", ip); val portIn = input("Port  (8080)", port); val pinIn = input("Web PIN  (optional)", pin)
        lay.addView(ipIn); lay.addView(txt("", 6f)); lay.addView(portIn); lay.addView(txt("", 6f)); lay.addView(pinIn); lay.addView(txt("", 10f))
        lay.addView(btn("Connect", PINK, Color.WHITE, 32f) {
            ip = ipIn.text.toString().trim().ifEmpty { "192.168.1.45" }
            port = portIn.text.toString().trim().ifEmpty { "8080" }
            pin = pinIn.text.toString().trim()
            prefs.edit().putString("ip", ip).putString("port", port).putString("pin", pin).apply()
            showConnecting()
        })
        root.addView(centerWrap(lay))
    }

    // ---------- 3) Connecting ----------
    private fun showConnecting() {
        clearRoot()
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(60, 60, 60, 60) }
        lay.addView(txt("U", 70f, PINK, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("U-Script", 40f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        val capsule = TextView(this).apply { text = "Connecting..."; textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; background = bgDrawable(PINK, 60f); setPadding(80, 30, 80, 30) }
        lay.addView(capsule); lay.addView(txt("", 8f))
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = true }
        lay.addView(bar)
        root.addView(centerWrap(lay))
        // اختبار الاتصال: بنجرب WebUI بتاع الاسكريبت
        Thread {
            val ok = pingWebUI()
            handler.post {
                if (ok) showWelcome()
                else showAutoInstall() // مش متصل -> جرب تثبيت الاسكريبت أوتوماتيك
            }
        }.start()
    }
    private fun pingWebUI(): Boolean {
        return try {
            val c = URL("http://$ip:$port/").openConnection() as HttpURLConnection
            c.connectTimeout = 4000; c.readTimeout = 4000; c.requestMethod = "GET"; c.connect(); c.responseCode in 200..499
        } catch (e: Exception) { false }
    }


    // ---------- 3.5) التثبيت الأوتوماتيك ----------
    private fun showAutoInstall() {
        clearRoot()
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(60, 60, 60, 60) }
        lay.addView(txt("U", 70f, PINK, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("U-Script", 40f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        val statusTxt = txt("Installing U-Script on receiver...", 18f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        val detailTxt = txt("Step 1/3: Uploading script via FTP...", 14f, MUTED).apply { gravity = Gravity.CENTER }
        lay.addView(statusTxt); lay.addView(txt("", 6f)); lay.addView(detailTxt); lay.addView(txt("", 10f))
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = true }
        lay.addView(bar)
        val retryBtn = btn("Retry Install", PINK, Color.WHITE, 32f) { showAutoInstall() }.apply { visibility = View.GONE }
        val backBtn = btn("Back to Connect", CARD, Color.WHITE, 32f, 2, BORDER) { showConnect() }
        lay.addView(txt("", 10f)); lay.addView(retryBtn); lay.addView(txt("", 4f)); lay.addView(backBtn)
        root.addView(centerWrap(lay))
        Thread {
            val result = ScriptInstaller.autoInstall(this, ip, port, pin) { step ->
                handler.post { detailTxt.text = step }
            }
            handler.post {
                when {
                    result.ok -> showWelcome()
                    else -> {
                        statusTxt.text = "Install failed"
                        detailTxt.text = result.error
                        retryBtn.visibility = View.VISIBLE
                    }
                }
            }
        }.start()
    }

    // ---------- 4) Not Connected ----------
    private fun showNotConnected() {
        clearRoot()
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(60, 60, 60, 60) }
        lay.addView(txt("U", 60f, PINK, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("U-Script is not connected", 20f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("Check Wi-Fi + IP + Port + Web PIN", 14f, MUTED).apply { gravity = Gravity.CENTER })
        lay.addView(txt("", 10f))
        lay.addView(btn("Reconnect", PINK, Color.WHITE, 32f) { showConnecting() })
        lay.addView(txt("", 6f))
        lay.addView(btn("Change IP / PIN", CARD, Color.WHITE, 32f, 2, BORDER) { showConnect() })
        root.addView(centerWrap(lay))
    }

    // ---------- 5) Welcome 3 ثواني ----------
    private fun showWelcome() {
        clearRoot()
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(40, 40, 40, 40) }
        lay.addView(txt("welcome", 60f, GOLD, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("To", 36f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("U-Script", 70f, PINK, true).apply { gravity = Gravity.CENTER })
        lay.addView(txt("You are now connected to your receiver", 16f, MUTED).apply { gravity = Gravity.CENTER })
        lay.addView(txt("Control Ur Enigma", 22f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        root.addView(centerWrap(lay))
        handler.postDelayed({ showMain() }, 3000)
    }

    // ---------- 6) القايمة الأساسية + باقي القوايم ----------
    private var contentHolder: FrameLayout? = null
    private fun showMain() {
        clearRoot()
        val main = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // هيدر
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(40, 30, 40, 20) }
        head.addView(txt("U-Script", 26f, Color.WHITE, true))
        head.addView(txt("  •  $ip", 14f, MUTED))
        main.addView(head)
        // منطقة المحتوى (بتتغير حسب القايمة)
        contentHolder = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        main.addView(contentHolder)
        // شريط القوايم تحت: Scroll أفقي عشان مفيش حاجة تتزنق أو تتقسم على سطرين
        val navScroll = HorizontalScrollView(this).apply { setBackgroundColor(CARD); isHorizontalScrollBarEnabled = false }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(16, 14, 16, 14) }
        val tabs = listOf("MAIN" to { showMainTab() }, "BACKUP" to { showMenuTab("BACKUP", listOf("Backup Ur Data" to "/?action=backup-data", "Backup Ur Files" to "/?action=backup-files", "Smart Restore" to "/?action=smart-restore", "Custom Restore" to "/?action=custom-restore", "Backup Image" to "/?action=backup-image", "Backup Manager" to "/?action=backup-manager")) },
            "CHANNELS" to { showMenuTab("CHANNEL EDITOR", listOf("Renew Channel List" to "/?action=channellist-renew", "Bouquet Editor" to "/?action=bouquet", "Transponder Update" to "/?action=transponder")) },
            "STREAM" to { showMenuTab("STREAMING", listOf("Sharing Servers" to "/?action=servers", "My IPTV (Xtream / M3U)" to "/?action=iptv", "My Servers (CCcam / Mgcamd)" to "/?action=servers")) },
            "MEDIA" to { showMenuTab("PICONS / EPG", listOf("Download Picons" to "/?action=picons", "Download EPG" to "/?action=epg-download", "EPG Auto-Refresh" to "/?action=epg-auto")) },
            "BOOT" to { showMenuTab("MULTIBOOT / IMAGE", listOf("Multiboot Manager" to "/?action=multiboot", "Image Update" to "/?action=image-update", "Download Image" to "/?action=image-links", "Backup Image" to "/?action=backup-image")) },
            "TOOLS" to { showMenuTab("ENIGMA TOOLS", listOf("Enigma Tools" to "/?action=tools", "Skin Changer" to "/?action=skin", "Factory Reset" to "/?action=factory", "Self-Test" to "/?action=selftest")) },
            "SET" to { showMenuTab("U-SCRIPT SETTING", listOf("Owner PIN / Lock" to "/?action=device-pin", "MAC Lock" to "/?action=mac-lock", "Device Setting" to "/?action=setup", "Web PIN" to "/?action=webpin")) })
        // MAIN هو اللي شغال في الأول
        var selectedTab = "MAIN"
        for ((label, action) in tabs) {
            val isSel = label == selectedTab
            val b = Button(this).apply {
                text = label; textSize = 13f; setTextColor(if (isSel) Color.WHITE else MUTED); typeface = Typeface.DEFAULT_BOLD
                background = bgDrawable(if (isSel) PINK else CARD, 28f); setPadding(36, 18, 36, 18)
                isSingleLine = true
                setOnClickListener {
                    selectedTab = label
                    // نلون الزرار اللي اتداس ونطفي الباقي
                    for (i in 0 until nav.childCount) {
                        val cb = nav.getChildAt(i) as Button
                        val on = cb.text.toString() == label
                        cb.setTextColor(if (on) Color.WHITE else MUTED)
                        cb.background = bgDrawable(if (on) PINK else CARD, 28f)
                    }
                    action()
                }
                stateListAnimator = null
            }
            // عرض ثابت + مسافة ثابتة بين كل زرار والتاني - مفيش زرار بيتقسم
            nav.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(8, 0, 8, 0) })
        }
        navScroll.addView(nav)
        main.addView(navScroll)
        root.addView(main)
        showMainTab()
    }

    // ----- القايمة الأساسية: عرض الشاشة + ريموت - توزيع منظم -----
    private fun showMainTab() {
        val holder = contentHolder ?: return
        holder.removeAllViews()
        val scroll = ScrollView(this)
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 10, 40, 20) }

        // ===== 1) عرض الشاشة: نسبة 16:9 ثابتة =====
        val screenBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = bgDrawable(Color.BLACK, 28f, 3, BORDER); setPadding(30, 40, 30, 40)
        }
        screenBox.addView(txt("LIVE  •  $ip", 14f, Color.parseColor("#FF5B5B"), true).apply { gravity = Gravity.CENTER })
        screenBox.addView(txt("Tap Play to open Live Stream", 15f, Color.WHITE).apply { gravity = Gravity.CENTER })
        screenBox.addView(txt("Port 8001", 13f, MUTED).apply { gravity = Gravity.CENTER })
        lay.addView(screenBox); lay.addView(txt("", 8f))
        lay.addView(btn("▶  PLAY LIVE SCREEN", PINK, Color.WHITE, 32f) { openWebPanel("http://$ip:8001/") })
        lay.addView(txt("", 16f))

        // ===== 2) القناة والصوت: مجموعتين واضحين جنب بعض =====
        lay.addView(txt("Channel  /  Volume", 15f, MUTED, true))
        lay.addView(txt("", 4f))
        // صف القناة
        val chRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val chMinus = btn("CH  −", CARD, CYAN, 28f, 2, CYAN) { sendRemote("channeldown") }
        val chPlus = btn("CH  +", CARD, CYAN, 28f, 2, CYAN) { sendRemote("channelup") }
        chRow.addView(chMinus, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, 8, 0) })
        chRow.addView(chPlus, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(8, 0, 0, 0) })
        lay.addView(chRow); lay.addView(txt("", 8f))
        // صف الصوت
        val volRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val vMinus = btn("VOL  −", CARD, CYAN, 28f, 2, CYAN) { sendRemote("volumedown") }
        val vPlus = btn("VOL  +", CARD, CYAN, 28f, 2, CYAN) { sendRemote("volumeup") }
        val mute = btn("MUTE", CARD, Color.WHITE, 28f, 2, BORDER) { sendRemote("mute") }
        volRow.addView(vMinus, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, 8, 0) })
        volRow.addView(vPlus, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(8, 0, 8, 0) })
        volRow.addView(mute, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(8, 0, 0, 0) })
        lay.addView(volRow); lay.addView(txt("", 20f))

        // ===== 3) الريموت: كارت واحد منظم =====
        lay.addView(txt("Remote Control", 20f, Color.WHITE, true)); lay.addView(txt("", 6f))
        val remote = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = bgDrawable(CARD, 36f, 2, BORDER); setPadding(40, 30, 40, 30)
        }
        // باور - عرضه ثابت ومتوسطن
        val powerWrap = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val powerBtn = btn("⏻   POWER", Color.parseColor("#FF5B5B"), Color.WHITE, 60f) { sendRemote("power") }
        powerWrap.addView(powerBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        remote.addView(powerWrap); remote.addView(txt("", 16f))

        // D-Pad: 3 صفوف مظبوطة - فوق / شمال-OK-يمين / تحت - OK دايرة في النص
        fun dpadBtn(text: String, cmd: String, isOk: Boolean = false): Button {
            return Button(this).apply {
                this.text = text; textSize = if (isOk) 18f else 20f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
                background = if (isOk) {
                    GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(PINK) }
                } else {
                    bgDrawable(Color.parseColor("#1A2145"), 28f, 2, BORDER)
                }
                setOnClickListener { sendRemote(cmd) }; stateListAnimator = null
            }
        }
        // صف فوق
        val upRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        upRow.addView(dpadBtn("▲", "up"), LinearLayout.LayoutParams(150, 110))
        remote.addView(upRow)
        // صف النص: شمال - OK - يمين (OK دايرة 140×140)
        val midRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        midRow.addView(dpadBtn("◀", "left"), LinearLayout.LayoutParams(150, 110).apply { setMargins(0, 0, 20, 0) })
        midRow.addView(dpadBtn("OK", "ok", true), LinearLayout.LayoutParams(150, 150).apply { setMargins(20, 0, 20, 0) })
        midRow.addView(dpadBtn("▶", "right"), LinearLayout.LayoutParams(150, 110).apply { setMargins(20, 0, 0, 0) })
        remote.addView(midRow)
        // صف تحت
        val dnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        dnRow.addView(dpadBtn("▼", "down"), LinearLayout.LayoutParams(150, 110))
        remote.addView(dnRow); remote.addView(txt("", 16f))

        // MENU / EXIT / INFO / EPG - 4 زرارير متساوية بالظبط
        val sysRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((t, cmd) in listOf("MENU" to "menu", "EXIT" to "exit", "INFO" to "info", "EPG" to "epg")) {
            val b = btn(t, Color.parseColor("#1A2145"), CYAN, 24f, 2, CYAN) { sendRemote(cmd) }
            sysRow.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(6, 0, 6, 0) })
        }
        remote.addView(sysRow); remote.addView(txt("", 16f))

        // الأرقام: Grid 3×3 - كل زرار ليه خلفية وحجم ثابت
        for (r in 0..2) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            for (c in 0..2) {
                val n = (r * 3 + c + 1).toString()
                val b = Button(this).apply {
                    text = n; textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
                    background = bgDrawable(Color.parseColor("#1A2145"), 28f, 2, BORDER)
                    setOnClickListener { sendRemote(n) }; stateListAnimator = null
                }
                row.addView(b, LinearLayout.LayoutParams(0, 110).apply { weight = 1f; setMargins(8, 8, 8, 8) })
            }
            remote.addView(row)
        }
        // صف 0
        val zeroRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val zeroBtn = Button(this).apply {
            text = "0"; textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            background = bgDrawable(Color.parseColor("#1A2145"), 28f, 2, BORDER)
            setOnClickListener { sendRemote("0") }; stateListAnimator = null
        }
        zeroRow.addView(zeroBtn, LinearLayout.LayoutParams(0, 110).apply { weight = 1f; setMargins(8, 8, 8, 8) })
        // مكانين فاضيين عشان الـ 0 يبقى في النص
        val spacer1 = View(this); val spacer2 = View(this)
        zeroRow.addView(spacer1, LinearLayout.LayoutParams(0, 110).apply { weight = 1f; setMargins(8, 8, 8, 8) })
        zeroRow.addView(spacer2, LinearLayout.LayoutParams(0, 110).apply { weight = 1f; setMargins(8, 8, 8, 8) })
        // نرتب: فاضي - 0 - فاضي عشان يبقى في النص بالظبط
        zeroRow.removeAllViews()
        zeroRow.addView(spacer1, LinearLayout.LayoutParams(0, 110).apply { weight = 1f; setMargins(8, 8, 8, 8) })
        zeroRow.addView(zeroBtn, LinearLayout.LayoutParams(0, 110).apply { weight = 1f; setMargins(8, 8, 8, 8) })
        zeroRow.addView(spacer2, LinearLayout.LayoutParams(0, 110).apply { weight = 1f; setMargins(8, 8, 8, 8) })
        remote.addView(zeroRow)

        lay.addView(remote); lay.addView(txt("", 14f))
        lay.addView(btn("Open Full Web Panel", PINK, Color.WHITE, 32f) { openWebPanel("http://$ip:$port/") })
        scroll.addView(lay); holder.addView(scroll)
    }

    // ----- باقي القوايم: زرارير بتكلم الاسكريبت -----
    private fun showMenuTab(title: String, items: List<Pair<String, String>>) {
        val holder = contentHolder ?: return
        holder.removeAllViews()
        val scroll = ScrollView(this)
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 10, 40, 20) }
        lay.addView(txt(title, 24f, Color.WHITE, true)); lay.addView(txt("", 6f))
        for ((label, path) in items) {
            lay.addView(btn(label, CARD, Color.WHITE, 28f, 2, BORDER) { openWebPanel("http://$ip:$port$path") })
            lay.addView(txt("", 4f))
        }
        lay.addView(txt("", 6f))
        lay.addView(btn("Open Full Web Panel", PINK, Color.WHITE, 32f) { openWebPanel("http://$ip:$port/") })
        scroll.addView(lay); holder.addView(scroll)
    }

    // ----- فتح Web Panel + Auto-Login بالـ PIN -----
    // السبب: الـ WebUI بتاع الاسكريبت بيطلب PIN عن طريق Cookie اسمها uscript_pin
    // الحل: التطبيق بيحط الـ Cookie دي في الـ WebView قبل ما يفتح الصفحة - فمفيش شاشة Login تاني
    private fun openWebPanel(url: String) {
        val holder = contentHolder ?: return
        holder.removeAllViews()
        // الـ PIN اللي كتبته في شاشة الاتصال - ولو فاضي بنجرب الافتراضي 1234
        val webPin = if (pin.isNotEmpty()) pin else "1234"
        // نحط كوكي الدخول قبل فتح الصفحة
        try {
            val cm = android.webkit.CookieManager.getInstance()
            cm.setAcceptCookie(true)
            cm.setAcceptThirdPartyCookies(null, true)
            // لازم نحط الكوكي على دومين الريسيفر نفسه
            cm.setCookie("http://$ip:$port/", "uscript_pin=$webPin; Path=/")
            cm.flush()
        } catch (e: Exception) { }
        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
            // نخلي الكوكيز تفضل محفوظة طول ما التطبيق شغال
            settings.databaseEnabled = true
            webViewClient = WebViewClient()
            loadUrl(url)
        }
        webView = wv
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(20, 10, 20, 10) }
        bar.addView(btn("←  Back to Menus", CARD, Color.WHITE, 28f) { showMainTab() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(bar); col.addView(wv, LinearLayout.LayoutParams(-1, 0, 1f))
        holder.addView(col)
    }

    // ----- الريموت: بيكلم OpenWebif على نفس الريسيفر -----
    private fun sendRemote(cmd: String) {
        Thread {
            try {
                val u = URL("http://$ip/web/remotecontrol?command=$cmd")
                (u.openConnection() as HttpURLConnection).apply { connectTimeout = 3000; readTimeout = 3000; connect(); responseCode }
            } catch (e: Exception) { }
        }.start()
        Toast.makeText(this, cmd, Toast.LENGTH_SHORT).show()
    }
}
