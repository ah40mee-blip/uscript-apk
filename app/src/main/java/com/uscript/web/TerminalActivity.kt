// U-Panel NEW LOOK: اسود رخامي جلوسي + U رمادي معدني + كروت زجاجية - مفيش وردي/احمر
package com.uscript.web

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue

class TerminalActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var outView: TextView
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var statusView: TextView
    private lateinit var savedBox: LinearLayout
    private var socket: Socket? = null
    private var sockIn: InputStream? = null
    private var sockOut: OutputStream? = null
    private var ip = ""
    private var connected = false
    private val handler = Handler(Looper.getMainLooper())
    private val history = mutableListOf<String>()
    private var histIdx = -1
    private val sb = StringBuilder()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun glassBg(): GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#2A2A2A"), Color.parseColor("#0A0A0A"), Color.parseColor("#000000"))).apply { cornerRadius = dp(16).toFloat(); setStroke(dp(1), Color.parseColor("#80FFFFFF")) }
    // زرار جلوسي اسود - بيلمع عند الدوس عليه
    private fun glossyBtn(text: String, weight: Float = 1f, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text; textSize = 13f; setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply { leftMargin = dp(4); rightMargin = dp(4) }
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#3A3A3A"), Color.parseColor("#111111"), Color.parseColor("#000000"))).apply { cornerRadius = dp(16).toFloat(); setStroke(dp(1), Color.parseColor("#E8E8E8")) }
            setPadding(dp(8), dp(12), dp(8), dp(12))
            setOnTouchListener { v, e ->
                when (e.action) {
                    android.view.MotionEvent.ACTION_DOWN -> { v.alpha = 0.6f; v.scaleX = 0.97f; v.scaleY = 0.97f }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> { v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f; if (e.action == android.view.MotionEvent.ACTION_UP) onClick() }
                }
                true
            }
        }
    }
    private fun glossyBtnFixed(text: String, widthDp: Int, onClick: () -> Unit): Button {
        val b = glossyBtn(text, 0f, onClick)
        b.layoutParams = LinearLayout.LayoutParams(dp(widthDp), LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(4); rightMargin = dp(4) }
        return b
    }
    private fun append(text: String) {
        handler.post {
            sb.append(text)
            if (sb.length > 30000) sb.delete(0, sb.length - 30000)
            outView.text = sb.toString()
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
    }
    private fun savedKey(): String = "saved_cmds"
    private fun loadSaved(): MutableList<Pair<String,String>> {
        val raw = prefs.getString(savedKey(), "") ?: ""
        if (raw.isEmpty()) {
            val defs = listOf("Restart Enigma" to "init 4; sleep 2; init 3", "Restart Server" to "sh /media/hdd/U-Panel/uscript.sh --restart-server", "Free Space" to "df -h", "List HDD" to "ls -lh /media/hdd/", "Backup Channels" to "tar -czf /media/hdd/backup/channels.tar.gz /etc/enigma2/")
            prefs.edit().putString(savedKey(), defs.joinToString("\n") { it.first + "|||" + it.second }).apply()
            return defs.toMutableList()
        }
        return raw.lines().filter { it.contains("|||") }.map { val p = it.split("|||", limit = 2); p[0] to p[1] }.toMutableList()
    }
    private fun saveSaved(list: List<Pair<String,String>>) { prefs.edit().putString(savedKey(), list.joinToString("\n") { it.first + "|||" + it.second }).apply() }
    private fun isDangerous(cmd: String): Boolean {
        val c = cmd.lowercase()
        return c.contains("rm -rf") || c.contains("reboot") || c.contains("format") || c.contains("mkfs") || c.contains(":(){")
    }
    private fun sendCmd(cmd: String) {
        if (cmd.isBlank()) return
        if (isDangerous(cmd)) {
            android.app.AlertDialog.Builder(this).setTitle("Dangerous command").setMessage(cmd + "\n\nAre you sure?").setNegativeButton("Cancel", null).setPositiveButton("Run") { _, _ -> doSend(cmd) }.show()
            return
        }
        doSend(cmd)
    }
    private fun doSend(cmd: String) {
        history.add(0, cmd); histIdx = -1
        append("\n# " + cmd + "\n")
        Thread {
            try { sockOut?.write((cmd + "\n").toByteArray()); sockOut?.flush() } catch (e: Exception) { append("\n[Not connected]\n") }
        }.start()
        input.setText("")
    }
    private fun refreshSaved() {
        savedBox.removeAllViews()
        val list = loadSaved()
        for ((idx, item) in list.withIndex()) {
            val card = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = glassBg(); setPadding(dp(10), dp(10), dp(10), dp(10)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10); bottomMargin = dp(2) } }
            val name = TextView(this).apply { text = item.first + "\n" + item.second; textSize = 12f; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val play = glossyBtnFixed("▶", 52) { sendCmd(item.second) }
            val edit = glossyBtnFixed("✏️", 52) { editSaved(idx, item) }
            val del = glossyBtnFixed("🗑️", 52) { val l = loadSaved(); l.removeAt(idx); saveSaved(l); refreshSaved() }
            card.addView(name); card.addView(play); card.addView(edit); card.addView(del)
            savedBox.addView(card)
        }
    }
    private fun editSaved(idx: Int, item: Pair<String,String>) {
        val nameIn = EditText(this).apply { setText(item.first) }
        val cmdIn = EditText(this).apply { setText(item.second) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(nameIn); addView(cmdIn) }
        android.app.AlertDialog.Builder(this).setTitle("Edit saved command").setView(col).setPositiveButton("Save") { _, _ ->
            val l = loadSaved(); l[idx] = nameIn.text.toString() to cmdIn.text.toString(); saveSaved(l); refreshSaved()
        }.setNegativeButton("Cancel", null).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("uscript_prefs", MODE_PRIVATE)
        ip = intent.getStringExtra("ip") ?: prefs.getString("ip", "") ?: ""
        val prefill = intent.getStringExtra("filePath") ?: intent.getStringExtra("initialCommand") ?: ""
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK); setPadding(dp(12), dp(12), dp(12), dp(12)) }
        statusView = TextView(this).apply { text = "● Connecting $ip:23 root"; textSize = 13f; setTextColor(Color.parseColor("#4ADE80")) }
        outView = TextView(this).apply { text = "Connecting...\n"; textSize = 13f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.MONOSPACE }
        scroll = ScrollView(this).apply { addView(outView); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        val rowTop = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnClear = glossyBtn("CLEAR") { sb.clear(); outView.text = "" }
        val btnReconnect = glossyBtn("RECONNECT") { connect() }
        val btnFiles = glossyBtn("📁 FILES") { val i = android.content.Intent(this@TerminalActivity, FileManagerActivity::class.java); i.putExtra("ip", ip); startActivity(i) }
        rowTop.addView(btnClear); rowTop.addView(btnReconnect); rowTop.addView(btnFiles)
        // ===== قسم الادخال منظم: مربع جلوسي واحد يلم الكتابة + SEND + الزراير - بمسافات مش لازقين =====
        val inputBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = glassBg(); setPadding(dp(10), dp(10), dp(10), dp(10)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10); bottomMargin = dp(10) } }
        val rowInput = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) } }
        input = EditText(this).apply { hint = "Type command..."; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 15f; minHeight = dp(48); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(8) }; background = glassBg(); setPadding(dp(14), dp(14), dp(14), dp(14)); if (prefill.isNotEmpty()) setText(prefill + " ") }
        val btnSend = glossyBtnFixed("SEND", 86) { sendCmd(input.text.toString()) }
        rowInput.addView(input); rowInput.addView(btnSend)
        // ===== صف زراير الاكشن - بمسافات بينهم =====
        val rowActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val btnUp = glossyBtn("↑") { if (history.isNotEmpty()) { histIdx = (histIdx + 1).coerceAtMost(history.size - 1); input.setText(history[histIdx]) } }
        val btnStar = glossyBtn("⭐ SAVE") { val c = input.text.toString(); if (c.isBlank()) return@glossyBtn; val nameIn = EditText(this@TerminalActivity).apply { hint = "Command name" }; android.app.AlertDialog.Builder(this@TerminalActivity).setTitle("Save command").setView(nameIn).setPositiveButton("Save") { _, _ -> val l = loadSaved(); l.add((if (nameIn.text.isBlank()) c else nameIn.text.toString()) to c); saveSaved(l); refreshSaved() }.setNegativeButton("Cancel", null).show() }
        val btnCtrlC = glossyBtn("CTRL+C") { Thread { try { sockOut?.write(byteArrayOf(3)); sockOut?.flush() } catch (e: Exception) {} }.start() }
        rowActions.addView(btnUp); rowActions.addView(btnStar); rowActions.addView(btnCtrlC)
        val savedTitle = TextView(this).apply { text = "Saved Commands"; textSize = 15f; setTextColor(Color.WHITE); setPadding(dp(2), dp(2), dp(2), dp(8)) }
        savedBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(2), dp(4), dp(2), dp(4)) }
        val savedScroll = ScrollView(this).apply { addView(savedBox); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180)) }
        inputBox.addView(rowInput); inputBox.addView(rowActions)
        // ===== الأوامر المتخزنة في مربع لوحده - مش سايحة على بعضها =====
        val savedBoxOuter = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = glassBg(); setPadding(dp(10), dp(10), dp(10), dp(10)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) } }
        savedBoxOuter.addView(savedTitle); savedBoxOuter.addView(savedScroll)
        root.addView(statusView); root.addView(rowTop); root.addView(scroll); root.addView(inputBox); root.addView(savedBoxOuter)
        setContentView(root)
        refreshSaved()
        connect()
    }
    private fun connect() {
        if (ip.isEmpty()) { append("No IP\n"); return }
        Thread {
            // Telnet 23 root/root - fallback root/empty - fallback SSH 22 not implemented as socket telnet, show error
            var ok = false
            var lastErr = ""
            val attempts = listOf("root" to "root", "root" to "")
            for ((u, p) in attempts) {
                try {
                    socket?.close()
                    val s = Socket(ip, 23); s.soTimeout = 0; s.keepAlive = true
                    socket = s; sockIn = s.getInputStream(); sockOut = s.getOutputStream()
                    Thread.sleep(800)
                    // drain banner
                    try { while (sockIn!!.available() > 0) sockIn!!.read() } catch (e: Exception) {}
                    sockOut!!.write((u + "\r\n").toByteArray()); sockOut!!.flush(); Thread.sleep(800)
                    try { while (sockIn!!.available() > 0) sockIn!!.read() } catch (e: Exception) {}
                    if (p.isNotEmpty()) { sockOut!!.write((p + "\r\n").toByteArray()); sockOut!!.flush(); Thread.sleep(800) }
                    ok = true; connected = true; break
                } catch (e: Exception) { lastErr = e.message ?: ""; ok = false }
            }
            handler.post { statusView.text = if (ok) "● Connected $ip:23 root" else "● Failed: $lastErr" }
            if (!ok) { append("\n[Cannot connect to $ip:23 - check receiver and WiFi]\n"); return@Thread }
            startKeepAlive(); append("Connected.\nroot@enigma2:~# ")
            // read loop - strip Telnet IAC
            val buf = ByteArray(4096)
            try {
                while (true) {
                    val n = sockIn!!.read(buf)
                    if (n <= 0) break
                    val out = StringBuilder()
                    var i = 0
                    while (i < n) {
                        val b = buf[i].toInt() and 0xFF
                        if (b == 255) { i += 3; continue }
                        out.append(buf[i].toInt().toChar()); i++
                    }
                    if (out.isNotEmpty()) append(out.toString())
                }
            } catch (e: Exception) {
                append("\n[Disconnected - reconnecting...]\n")
                // إعادة اتصال تلقائية 3 مرات
                var tries = 0
                while (tries < 3 && !connected) {
                    tries++
                    try { Thread.sleep(2000); connect(); if (connected) { append("\n[Reconnected]\n"); break } } catch (ex: Exception) {}
                }
            }
        }.start()
    }
    private var keepAliveJob: Thread? = null
    private fun startKeepAlive() {
        keepAliveJob?.interrupt()
        keepAliveJob = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try { Thread.sleep(30000); sockOut?.write(byteArrayOf(0)); sockOut?.flush() } catch (e: Exception) { break }
            }
        }
        keepAliveJob?.start()
    }
    override fun onDestroy() { try { keepAliveJob?.interrupt(); socket?.close() } catch (e: Exception) {}; super.onDestroy() }
}