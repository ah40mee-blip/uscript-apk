// U-Panel FileManager - من الصفر: عمودين | يمين موبايل / شمال رسيفر | فولدرات أسود جلوسي
package com.uscript.web

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Vector

class FileManagerActivity : AppCompatActivity() {
    private var ip = ""
    private var recvPath = "/media/hdd/"
    private var mobPath = Environment.getExternalStorageDirectory().absolutePath + "/Download"
    private var recvRoot = "HDD" // HDD | FLASH
    private var mobRoot = "INTERNAL" // INTERNAL | SD
    private lateinit var recvList: LinearLayout
    private lateinit var mobList: LinearLayout
    private lateinit var recvPathView: TextView
    private lateinit var mobPathView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var progressBox: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var selectedRecv = mutableSetOf<String>()
    private var selectedMob = mutableSetOf<String>()
    private var recvItems = listOf<Item>()
    private var mobItems = listOf<Item>()

    data class Item(val name: String, val path: String, val isDir: Boolean, val size: Long)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun pw(): String = getSharedPreferences("uscript_prefs", MODE_PRIVATE).getString("root_pw", "root") ?: "root"
    private fun glossyBg(): GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#4A4A4A"), Color.parseColor("#111111"), Color.parseColor("#000000"))).apply { cornerRadius = dp(20).toFloat(); setStroke(dp(2), Color.parseColor("#E8E8E8")) }
    private fun glossyCard(): GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#2A2A2A"), Color.parseColor("#111111"), Color.parseColor("#000000"))).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.parseColor("#E8E8E8")) }

    // ===== فولدر أسود جلوسي لامع =====
    private fun folderIcon(): TextView = TextView(this).apply {
        text = "\uD83D\uDCC1"; textSize = 30f; gravity = Gravity.CENTER
        // خلفية سودا جلوسي لامعة للفولدر
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#5A5A5A"), Color.parseColor("#1A1A1A"), Color.parseColor("#000000"))).apply { cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.parseColor("#E8E8E8")) }
        layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
    }

    private fun sftp(): ChannelSftp? {
        return try {
            val jsch = JSch(); val s = jsch.getSession("root", ip, 22)
            s.setPassword(pw()); s.setConfig("StrictHostKeyChecking", "no"); s.connect(10000)
            val c = s.openChannel("sftp") as ChannelSftp; c.connect(10000); c
        } catch (e: Exception) { null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ip = intent.getStringExtra("ip") ?: getSharedPreferences("uscript_prefs", MODE_PRIVATE).getString("ip", "") ?: ""

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#050505")); setPadding(dp(8), dp(8), dp(8), dp(8)) }

        // ===== هيدر =====
        val header = TextView(this).apply { text = "U-Panel  •  File Manager"; textSize = 18f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        root.addView(header)

        // ===== العمودين + النص =====
        val main = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }

        // --- الشمال: الرسيفر ---
        val leftCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { rightMargin = dp(4) } }
        val recvHeader = TextView(this).apply { text = "RECEIVER"; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; background = glossyBg(); setPadding(dp(8), dp(10), dp(8), dp(10)) }
        // زرارين HDD / FLASH
        val recvRoots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnHDD = Button(this).apply { text = "HDD"; textSize = 12f; setTextColor(Color.WHITE); background = glossyCard(); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { recvRoot = "HDD"; recvPath = "/media/hdd/"; refreshReceiver() } }
        val btnFlash = Button(this).apply { text = "FLASH"; textSize = 12f; setTextColor(Color.WHITE); background = glossyCard(); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { recvRoot = "FLASH"; recvPath = "/"; refreshReceiver() } }
        recvRoots.addView(btnHDD); recvRoots.addView(btnFlash)
        recvPathView = TextView(this).apply { text = recvPath; textSize = 11f; setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER }
        val recvBack = Button(this).apply { text = "⬆ Back"; setTextColor(Color.WHITE); background = glossyCard(); setOnClickListener { recvPath = recvPath.trimEnd('/').substringBeforeLast("/", "/") + "/"; if (recvPath.isEmpty()) recvPath = "/"; refreshReceiver() } }
        recvList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val recvScroll = ScrollView(this).apply { addView(recvList); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        leftCol.addView(recvHeader); leftCol.addView(recvRoots); leftCol.addView(recvPathView); leftCol.addView(recvBack); leftCol.addView(recvScroll)

        // --- النص: سهمين + مسح ---
        val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.MATCH_PARENT) }
        fun arrowBtn(txt: String, toReceiver: Boolean): Button {
            val b = Button(this).apply {
                text = txt; textSize = 24f; setTextColor(Color.WHITE); background = glossyBg()
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { bottomMargin = dp(10) }
            }
            // دوسة واحدة = نسخ
            b.setOnClickListener { if (toReceiver) copyMobToRecv(false) else copyRecvToMob(false) }
            // دوسة طويلة = تحريك كامل
            b.setOnLongClickListener { if (toReceiver) copyMobToRecv(true) else copyRecvToMob(true); true }
            return b
        }
        // > = رسيفر -> موبايل (يمين) | < = موبايل -> رسيفر (شمال)
        val btnToMob = arrowBtn(">", false)
        val btnToRecv = arrowBtn("<", true)
        val btnDel = Button(this).apply {
            text = "\uD83D\uDDD1\uFE0F"; textSize = 22f; setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#7A1A1A"), Color.parseColor("#330000"), Color.parseColor("#000000"))).apply { cornerRadius = dp(20).toFloat(); setStroke(dp(2), Color.parseColor("#FF8888")) }
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { topMargin = dp(10) }
            setOnClickListener { deleteSelected() }
        }
        val midLabel = TextView(this).apply { text = "Copy\n1 tap\n\nMove\nlong\n\nDelete"; textSize = 9f; setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER }
        mid.addView(btnToMob); mid.addView(btnToRecv); mid.addView(btnDel); mid.addView(midLabel)

        // --- اليمين: الموبايل ---
        val rightCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = dp(4) } }
        val mobHeader = TextView(this).apply { text = "MOBILE"; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; background = glossyBg(); setPadding(dp(8), dp(10), dp(8), dp(10)) }
        val mobRoots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnInt = Button(this).apply { text = "INTERNAL"; textSize = 11f; setTextColor(Color.WHITE); background = glossyCard(); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { mobRoot = "INTERNAL"; mobPath = Environment.getExternalStorageDirectory().absolutePath + "/Download"; refreshMobile() } }
        val btnSD = Button(this).apply { text = "SD CARD"; textSize = 11f; setTextColor(Color.WHITE); background = glossyCard(); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { mobRoot = "SD"; mobPath = "/storage"; refreshMobile() } }
        mobRoots.addView(btnInt); mobRoots.addView(btnSD)
        mobPathView = TextView(this).apply { text = mobPath; textSize = 11f; setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER }
        val mobBack = Button(this).apply { text = "⬆ Back"; setTextColor(Color.WHITE); background = glossyCard(); setOnClickListener { mobPath = File(mobPath).parent ?: mobPath; refreshMobile() } }
        mobList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val mobScroll = ScrollView(this).apply { addView(mobList); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        rightCol.addView(mobHeader); rightCol.addView(mobRoots); rightCol.addView(mobPathView); rightCol.addView(mobBack); rightCol.addView(mobScroll)

        main.addView(leftCol); main.addView(mid); main.addView(rightCol)
        root.addView(main)

        // ===== بار النقل =====
        progressBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = glossyCard(); setPadding(dp(10), dp(10), dp(10), dp(10)); visibility = android.view.View.GONE }
        progressText = TextView(this).apply { text = "Transferring... 0%"; textSize = 13f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(22)) }
        progressBox.addView(progressText); progressBox.addView(progressBar)
        root.addView(progressBox)

        // ===== بار تحت: تيرمينال + قفل =====
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnTerm = Button(this).apply { text = "Terminal"; setTextColor(Color.WHITE); background = glossyBg(); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { val i = Intent(this@FileManagerActivity, TerminalActivity::class.java); i.putExtra("ip", ip); startActivity(i) } }
        val btnClose = Button(this).apply { text = "✕ Close"; setTextColor(Color.WHITE); background = glossyBg(); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { finish() } }
        bottom.addView(btnTerm); bottom.addView(btnClose)
        root.addView(bottom)

        setContentView(root)
        refreshReceiver(); refreshMobile()
    }

    private fun showProgress(show: Boolean, pct: Int = 0, label: String = "") {
        handler.post {
            progressBox.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
            progressBar.progress = pct
            if (label.isNotEmpty()) progressText.text = label
        }
    }

    private fun refreshReceiver() {
        recvPathView.text = recvPath
        Thread {
            val items = mutableListOf<Item>()
            try {
                val c = sftp(); c?.let {
                    val v = it.ls(recvPath) as Vector<ChannelSftp.LsEntry>
                    for (e in v) {
                        if (e.filename == "." || e.filename == "..") continue
                        val isDir = e.attrs.isDir
                        items.add(Item(e.filename, recvPath.trimEnd('/') + "/" + e.filename, isDir, e.attrs.size))
                    }
                    try { it.session.disconnect() } catch (ex: Exception) {}
                    try { it.disconnect() } catch (ex: Exception) {}
                }
            } catch (e: Exception) {}
            recvItems = items.sortedWith(compareByDescending<Item> { it.isDir }.thenBy { it.name.lowercase() })
            handler.post { drawColumn(recvList, recvItems, selectedRecv, true) }
        }.start()
    }

    private fun refreshMobile() {
        mobPathView.text = mobPath
        Thread {
            val f = File(mobPath)
            val arr = f.listFiles() ?: arrayOf()
            val items = arr.map { Item(it.name, it.absolutePath, it.isDirectory, it.length()) }.sortedWith(compareByDescending<Item> { it.isDir }.thenBy { it.name.lowercase() })
            mobItems = items
            handler.post { drawColumn(mobList, mobItems, selectedMob, false) }
        }.start()
    }

    private fun drawColumn(box: LinearLayout, items: List<Item>, selected: MutableSet<String>, isRecv: Boolean) {
        box.removeAllViews()
        if (items.isEmpty()) { box.addView(TextView(this).apply { text = "Empty folder"; setTextColor(Color.GRAY); gravity = Gravity.CENTER }); return }
        for (item in items) {
            val isSel = selected.contains(item.path)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = if (isSel) GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#5A5A5A"), Color.parseColor("#222222"), Color.parseColor("#000000"))).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(3), Color.parseColor("#FFFFFF")) } else glossyCard()
                setPadding(dp(10), dp(10), dp(10), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6); leftMargin = dp(4); rightMargin = dp(4) }
            }
            // فولدر أسود جلوسي لامع
            val icon = if (item.isDir) folderIcon() else TextView(this).apply { text = "\uD83D\uDCC4"; textSize = 26f; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)) }
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val nameTv = TextView(this).apply { text = item.name; textSize = 14f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD }
            val sizeTv = TextView(this).apply { text = if (item.isDir) "Folder" else "${item.size / 1024} KB"; textSize = 11f; setTextColor(Color.parseColor("#AAAAAA")) }
            col.addView(nameTv); col.addView(sizeTv)
            val check = TextView(this).apply { text = if (isSel) "✓" else "○"; textSize = 20f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)) }
            card.addView(icon); card.addView(col); card.addView(check)
            // دوسة واحدة = افتح الفولدر
            card.setOnClickListener {
                if (item.isDir) {
                    if (isRecv) { recvPath = item.path + "/"; refreshReceiver() } else { mobPath = item.path; refreshMobile() }
                } else {
                    Toast.makeText(this, item.name, Toast.LENGTH_SHORT).show()
                }
            }
            // دوسة طويلة = اختار
            card.setOnLongClickListener {
                if (isSel) selected.remove(item.path) else selected.add(item.path)
                drawColumn(box, items, selected, isRecv); true
            }
            box.addView(card)
        }
    }

    // ===== نسخ موبايل -> رسيفر | دوسة = نسخ / طويلة = تحريك =====
    private fun copyMobToRecv(move: Boolean) {
        if (selectedMob.isEmpty()) { Toast.makeText(this, "اختار ملفات من الموبايل بدوسة طويلة الأول", Toast.LENGTH_SHORT).show(); return }
        val list = selectedMob.toList()
        showProgress(true, 0, "Copying to Receiver... 0%")
        Thread {
            var done = 0
            for (p in list) {
                try {
                    val f = File(p)
                    val c = sftp(); c?.let {
                        val dest = recvPath.trimEnd('/') + "/" + f.name
                        if (f.isDirectory) { try { it.mkdir(dest) } catch (e: Exception) {} } else {
                            it.put(FileInputStream(f), dest)
                        }
                        try { it.session.disconnect() } catch (e: Exception) {}
                        try { it.disconnect() } catch (e: Exception) {}
                    }
                    if (move) f.deleteRecursively()
                } catch (e: Exception) {}
                done++
                val pct = (done * 100 / list.size)
                showProgress(true, pct, if (move) "Moving to Receiver... $pct%" else "Copying to Receiver... $pct%")
            }
            showProgress(false)
            handler.post { selectedMob.clear(); refreshMobile(); refreshReceiver(); Toast.makeText(this, if (move) "تم التحريك للرسيفر" else "تم النسخ للرسيفر", Toast.LENGTH_SHORT).show() }
        }.start()
    }

    // ===== نسخ رسيفر -> موبايل =====
    private fun copyRecvToMob(move: Boolean) {
        if (selectedRecv.isEmpty()) { Toast.makeText(this, "اختار ملفات من الرسيفر بدوسة طويلة الأول", Toast.LENGTH_SHORT).show(); return }
        val list = selectedRecv.toList()
        showProgress(true, 0, "Copying to Mobile... 0%")
        Thread {
            var done = 0
            for (p in list) {
                try {
                    val name = p.substringAfterLast("/")
                    val dest = File(mobPath, name)
                    val c = sftp(); c?.let {
                        it.get(p, FileOutputStream(dest))
                        if (move) { try { it.rm(p) } catch (e: Exception) { try { it.rmdir(p) } catch (ex: Exception) {} } }
                        try { it.session.disconnect() } catch (e: Exception) {}
                        try { it.disconnect() } catch (e: Exception) {}
                    }
                } catch (e: Exception) {}
                done++
                val pct = (done * 100 / list.size)
                showProgress(true, pct, if (move) "Moving to Mobile... $pct%" else "Copying to Mobile... $pct%")
            }
            showProgress(false)
            handler.post { selectedRecv.clear(); refreshMobile(); refreshReceiver(); Toast.makeText(this, if (move) "تم التحريك للموبايل" else "تم النسخ للموبايل", Toast.LENGTH_SHORT).show() }
        }.start()
    }

    private fun deleteSelected() {
        if (selectedRecv.isEmpty() && selectedMob.isEmpty()) { Toast.makeText(this, "اختار ملف بدوسة طويلة الأول", Toast.LENGTH_SHORT).show(); return }
        android.app.AlertDialog.Builder(this).setTitle("Delete?").setMessage("Delete ${selectedRecv.size + selectedMob.size} items?").setPositiveButton("Yes") { _, _ ->
            Thread {
                for (p in selectedRecv.toList()) { try { val c = sftp(); c?.let { try { it.rm(p) } catch (e: Exception) { try { it.rmdir(p) } catch (ex: Exception) {} }; try { it.session.disconnect() } catch (ex: Exception) {}; try { it.disconnect() } catch (ex: Exception) {} } } catch (e: Exception) {} }
                for (p in selectedMob.toList()) { try { File(p).deleteRecursively() } catch (e: Exception) {} }
                handler.post { selectedRecv.clear(); selectedMob.clear(); refreshMobile(); refreshReceiver() }
            }.start()
        }.setNegativeButton("No", null).show()
    }
}
