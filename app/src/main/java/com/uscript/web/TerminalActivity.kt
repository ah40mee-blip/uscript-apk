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
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val name = TextView(this).apply { text = item.first + "\n" + item.second; textSize = 12f; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val play = Button(this).apply { text = "▶"; setOnClickListener { sendCmd(item.second) } }
            val edit = Button(this).apply { text = "✏️"; setOnClickListener { editSaved(idx, item) } }
            val del = Button(this).apply { text = "🗑️"; setOnClickListener { val l = loadSaved(); l.removeAt(idx); saveSaved(l); refreshSaved() } }
            row.addView(name); row.addView(play); row.addView(edit); row.addView(del)
            savedBox.addView(row)
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
        val btnClear = Button(this).apply { text = "Clear"; setOnClickListener { sb.clear(); outView.text = "" } }
        val btnReconnect = Button(this).apply { text = "Reconnect"; setOnClickListener { connect() } }
        val btnFiles = Button(this).apply { text = "📁 Files"; setOnClickListener { val i = android.content.Intent(this@TerminalActivity, FileManagerActivity::class.java); i.putExtra("ip", ip); startActivity(i) } }
        rowTop.addView(btnClear); rowTop.addView(btnReconnect); rowTop.addView(btnFiles)
        val rowInput = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        input = EditText(this).apply { hint = "Type command..."; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); background = glassBg(); if (prefill.isNotEmpty()) setText(prefill + " ") }
        val btnSend = Button(this).apply { text = "Send"; setOnClickListener { sendCmd(input.text.toString()) } }
        val btnUp = Button(this).apply { text = "↑"; setOnClickListener { if (history.isNotEmpty()) { histIdx = (histIdx + 1).coerceAtMost(history.size - 1); input.setText(history[histIdx]) } } }
        val btnStar = Button(this).apply { text = "⭐"; setOnClickListener { val c = input.text.toString(); if (c.isBlank()) return@setOnClickListener; val nameIn = EditText(this@TerminalActivity).apply { hint = "Command name" }; android.app.AlertDialog.Builder(this@TerminalActivity).setTitle("Save command").setView(nameIn).setPositiveButton("Save") { _, _ -> val l = loadSaved(); l.add((if (nameIn.text.isBlank()) c else nameIn.text.toString()) to c); saveSaved(l); refreshSaved() }.setNegativeButton("Cancel", null).show() } }
        val btnCtrlC = Button(this).apply { text = "Ctrl+C"; setOnClickListener { Thread { try { sockOut?.write(byteArrayOf(3)); sockOut?.flush() } catch (e: Exception) {} }.start() } }
        rowInput.addView(input); rowInput.addView(btnSend); rowInput.addView(btnUp); rowInput.addView(btnStar); rowInput.addView(btnCtrlC)
        val savedTitle = TextView(this).apply { text = "Saved Commands"; textSize = 15f; setTextColor(Color.WHITE) }
        savedBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val savedScroll = ScrollView(this).apply { addView(savedBox); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180)) }
        root.addView(statusView); root.addView(rowTop); root.addView(scroll); root.addView(rowInput); root.addView(savedTitle); root.addView(savedScroll)
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
                    val s = Socket(ip, 23); s.soTimeout = 8000
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
            append("Connected.\nroot@enigma2:~# ")
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
            } catch (e: Exception) { append("\n[Disconnected]\n") }
        }.start()
    }
    override fun onDestroy() { try { socket?.close() } catch (e: Exception) {}; super.onDestroy() }
}
