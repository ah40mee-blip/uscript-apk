// U-Panel NEW LOOK: اسود رخامي جلوسي + U رمادي معدني + كروت زجاجية - مفيش وردي/احمر
package com.uscript.web

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import java.io.File
import java.util.Vector
import java.io.FileInputStream
import java.io.FileOutputStream

class FileManagerActivity : AppCompatActivity() {
    private var ip = ""
    private var mode = "RECEIVER" // RECEIVER or MOBILE
    private var recvPath = "/media/hdd/"
    private var mobPath = Environment.getExternalStorageDirectory().absolutePath + "/Download"
    private lateinit var pathView: TextView
    private lateinit var listBox: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var searchField: EditText
    private lateinit var progressBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var cache = mutableMapOf<String, List<Item>>()
    private var searchQuery = ""
    private var sortBy = "name" // name | size | date
    private var categoryFilter = "" // "" = الكل | images | videos | audio | docs | downloads | apks
    private var isHome = true
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun pw(): String = getSharedPreferences("uscript_prefs", MODE_PRIVATE).getString("root_pw", "root") ?: "root"

    private fun sftp(): ChannelSftp? {
        return try {
            val jsch = JSch(); val s = jsch.getSession("root", ip, 22); s.setPassword(pw()); s.setConfig("StrictHostKeyChecking", "no"); s.setConfig("PreferredAuthentications", "password,keyboard-interactive"); s.connect(10000)
            val ch = s.openChannel("sftp") as ChannelSftp; ch.connect(10000); ch
        } catch (e: Exception) { null }
    }
    data class Item(val name: String, val path: String, val isDir: Boolean, val size: Long)

    private fun listReceiver() {
        statusView.text = "Loading $recvPath ..."
        Thread {
            val items = mutableListOf<Item>()
            try {
                val ch = sftp() ?: throw Exception("SFTP failed")
                try {
                    @Suppress("UNCHECKED_CAST")
                    val v = ch.ls(recvPath) as Vector<com.jcraft.jsch.ChannelSftp.LsEntry>
                    for (e in v) { if (e.filename == "." || e.filename == "..") continue; items.add(Item(e.filename, recvPath.trimEnd('/') + "/" + e.filename, e.attrs.isDir, e.attrs.size)) }
                } finally { try { ch.session.disconnect() } catch (e: Exception) {}; try { ch.disconnect() } catch (e: Exception) {} }
            } catch (e: Exception) { handler.post { statusView.text = "Cannot list receiver: ${e.message}" } }
            handler.post { showItems(items.sortedWith(compareByDescending<Item> { it.isDir }.thenBy { it.name.lowercase() })) }
        }.start()
    }
    private fun listMobile() {
        val dir = File(mobPath); val items = mutableListOf<Item>()
        if (dir.exists()) { for (f in dir.listFiles() ?: emptyArray()) items.add(Item(f.name, f.absolutePath, f.isDirectory, f.length())) }
        showItems(items.sortedWith(compareByDescending<Item> { it.isDir }.thenBy { it.name.lowercase() }))
        statusView.text = "Mobile: $mobPath - ${items.size} items"
    }
    private fun showItems(items: List<Item>) {
        listBox.removeAllViews()
        // ===== بحث + ترتيب - أداء أسرع =====
        // ===== منطق File Manager Plus: فلترة بالتصنيف =====
        var filtered = items
        if (categoryFilter.isNotEmpty()) {
            filtered = filtered.filter { it.name.lowercase().let { n ->
                when (categoryFilter) {
                    "images" -> n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".gif")
                    "videos" -> n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi") || n.endsWith(".ts") || n.endsWith(".mov")
                    "audio" -> n.endsWith(".mp3") || n.endsWith(".flac") || n.endsWith(".wav") || n.endsWith(".aac") || n.endsWith(".m4a")
                    "docs" -> n.endsWith(".pdf") || n.endsWith(".txt") || n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".xls") || n.endsWith(".xlsx")
                    "apks" -> n.endsWith(".apk")
                    "downloads" -> true
                    else -> true
                }
            }}
        }
        filtered = if (searchQuery.isEmpty()) filtered else filtered.filter { it.name.lowercase().contains(searchQuery.lowercase()) }
        filtered = when (sortBy) {
            "size" -> filtered.sortedByDescending { it.size }
            else -> filtered.sortedWith(compareByDescending<Item> { it.isDir }.thenBy { it.name.lowercase() })
        }
        val items = filtered
        pathView.text = if (mode == "RECEIVER") "Receiver: $recvPath" else "Mobile: $mobPath"
        // ===== قسم الفولدرات - كروت زجاجية منظمة =====
        for (item in items) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#2A2A2A"), Color.parseColor("#111111"), Color.parseColor("#000000"))).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.parseColor("#E8E8E8")) }
                setPadding(dp(12), dp(12), dp(12), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6); leftMargin = dp(8); rightMargin = dp(8) }
            }
            // فولدر مجسم 3D زي Root Explorer: أيقونة كبيرة + اسم + تاريخ وصلاحيات + صح
            val icon = TextView(this).apply { text = if (item.isDir) "📁" else "📄"; textSize = 32f; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)) }
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val nameTv = TextView(this).apply { text = item.name; textSize = 16f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD }
            val dateStr = java.text.SimpleDateFormat("dd MMM yy HH:mm", java.util.Locale.ENGLISH).format(java.util.Date())
            val permStr = if (item.isDir) "rwxr-xr-x" else "rw-r--r--"
            val sizeTv = TextView(this).apply { text = "$dateStr   $permStr" + if (!item.isDir) "   ${item.size / 1024} KB" else ""; textSize = 12f; setTextColor(Color.parseColor("#AAAAAA")) }
            col.addView(nameTv); col.addView(sizeTv)
            val arrow = TextView(this).apply { text = "✓"; textSize = 18f; setTextColor(Color.WHITE); background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(Color.parseColor("#111111")); setStroke(dp(1), Color.parseColor("#E8E8E8")) }; setPadding(dp(8), dp(6), dp(8), dp(6)) }
            card.addView(icon); card.addView(col); card.addView(arrow)
            card.setOnClickListener { if (item.isDir) { if (mode == "RECEIVER") recvPath = item.path + "/" else mobPath = item.path; refresh() } else { openFile(item) } }
            card.setOnLongClickListener { showMenu(item); true }
            // لمعة عند الدوس
            card.setOnTouchListener { v, e ->
                when (e.action) {
                    android.view.MotionEvent.ACTION_DOWN -> v.alpha = 0.6f
                    android.view.MotionEvent.ACTION_UP -> { v.alpha = 1f; v.performClick() }
                    android.view.MotionEvent.ACTION_CANCEL -> v.alpha = 1f
                }
                true
            }
            listBox.addView(card)
        }
        if (items.isEmpty()) listBox.addView(TextView(this).apply { text = "Empty folder"; setTextColor(Color.GRAY); gravity = Gravity.CENTER })
    }
    private fun showMenu(it: Item) {
        val isApk = it.name.lowercase().endsWith(".apk")
        val opts = if (isApk) arrayOf("Install App", "Copy Path", "Copy Name", "Copy Extension", "Send to Terminal", "Download / Upload", "Open With", "Rename", "Delete", "New Folder here") else arrayOf("Copy Path", "Copy Name", "Copy Extension", "Send to Terminal", "Download / Upload", "Open With", "Rename", "Delete", "New Folder here")
        android.app.AlertDialog.Builder(this).setTitle(it.name).setItems(opts) { _, which ->
            var w = which
            if (isApk) {
                if (w == 0) { installApk(it); return@setItems }
                w -= 1
            }
            when (w) {
                0 -> copy(it.path)
                1 -> copy(it.name)
                2 -> copy(if (it.name.contains(".")) "." + it.name.substringAfterLast(".") else "")
                3 -> { val i = Intent(this, TerminalActivity::class.java); i.putExtra("ip", ip); i.putExtra("filePath", it.path); startActivity(i) }
                4 -> if (mode == "RECEIVER") downloadFromReceiver(it) else uploadToReceiver(it)
                5 -> openFile(it)
                6 -> renameItem(it)
                7 -> deleteItem(it)
                8 -> createFolder()
            }
        }.show()
    }
    private fun copy(s: String) { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("path", s)); Toast.makeText(this, "Copied: $s", Toast.LENGTH_SHORT).show() }
    private fun refresh() { if (mode == "RECEIVER") listReceiver() else listMobile() }
    private fun downloadFromReceiver(it: Item) {
        statusView.text = "Downloading ${it.name} ..."
        Thread {
            try {
                val ch = sftp() ?: throw Exception("SFTP failed")
                val dest = File(Environment.getExternalStorageDirectory().absolutePath + "/Download/U-Panel", it.name); dest.parentFile?.mkdirs()
                try { ch.get(it.path, dest.absolutePath) } finally { try { ch.session.disconnect() } catch (e: Exception) {} }
                handler.post { statusView.text = "Downloaded to ${dest.absolutePath}"; Toast.makeText(this, "Downloaded", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) { handler.post { statusView.text = "Download failed: ${e.message}" } }
        }.start()
    }
    private fun uploadToReceiver(it: Item) {
        // موبايل -> رسيفر: يرفع في /media/hdd/
        statusView.text = "Uploading ${it.name} to /media/hdd/ ..."
        Thread {
            try {
                val ch = sftp() ?: throw Exception("SFTP failed")
                try { ch.put(FileInputStream(File(it.path)), "/media/hdd/" + it.name) } finally { try { ch.session.disconnect() } catch (e: Exception) {} }
                handler.post { statusView.text = "Uploaded to /media/hdd/${it.name}" }
            } catch (e: Exception) { handler.post { statusView.text = "Upload failed: ${e.message}" } }
        }.start()
    }
    private fun openFile(it: Item) {
        // ملف على الرسيفر: نزله الأول في temp وبعدين افتحه بتطبيق خارجي - التطبيق الخارجي هو اللي يطلب Permissions
        if (mode == "RECEIVER") {
            statusView.text = "Downloading for Open With..."
            Thread {
                try {
                    val ch = sftp() ?: throw Exception("SFTP failed")
                    val dest = File(cacheDir, it.name)
                    try { ch.get(it.path, dest.absolutePath) } finally { try { ch.session.disconnect() } catch (e: Exception) {} }
                    handler.post { openExternal(dest) }
                } catch (e: Exception) { handler.post { statusView.text = "Open failed: ${e.message}" } }
            }.start()
        } else {
            if (it.name.lowercase().endsWith(".apk")) doInstall(File(it.path)) else openExternal(File(it.path))
        }
    }
    private fun installApk(it: Item) {
        // APK على الرسيفر: نزله الأول في cache وبعدين ثبته - النظام هو اللي يطلب إذن التثبيت
        if (mode == "RECEIVER") {
            statusView.text = "Downloading APK..."
            Thread {
                try {
                    val ch = sftp() ?: throw Exception("SFTP failed")
                    val dest = File(cacheDir, it.name)
                    try { ch.get(it.path, dest.absolutePath) } finally { try { ch.session.disconnect() } catch (e: Exception) {} }
                    handler.post { doInstall(dest) }
                } catch (e: Exception) { handler.post { statusView.text = "APK download failed: ${e.message}" } }
            }.start()
        } else doInstall(File(it.path))
    }
    private fun doInstall(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) { Toast.makeText(this, "Install failed: ${e.message} - allow Install unknown apps for U-Panel", Toast.LENGTH_LONG).show() }
    }
    private fun openExternal(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mime = when (file.extension.lowercase()) { "zip" -> "application/zip"; "rar" -> "application/x-rar-compressed"; "mp4","mkv" -> "video/*"; "mp3","wav" -> "audio/*"; "jpg","png" -> "image/*"; "pdf" -> "application/pdf"; "txt","sh","log" -> "text/plain"; else -> "*/*" }
            val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(i, "Open With"))
        } catch (e: Exception) { Toast.makeText(this, "Add FileProvider in AndroidManifest: $packageName.fileprovider - ${e.message}", Toast.LENGTH_LONG).show() }
    }
    private fun renameItem(it: Item) {
        val ed = EditText(this).apply { setText(it.name) }
        android.app.AlertDialog.Builder(this).setTitle("Rename").setView(ed).setPositiveButton("Save") { _, _ ->
            Thread {
                try {
                    if (mode == "RECEIVER") { val ch = sftp()!!; try { ch.rename(it.path, it.path.substringBeforeLast("/") + "/" + ed.text.toString()) } finally { try { ch.session.disconnect() } catch (e: Exception) {} } }
                    else File(it.path).renameTo(File(File(it.path).parent, ed.text.toString()))
                    handler.post { refresh() }
                } catch (e: Exception) { handler.post { statusView.text = "Rename failed: ${e.message}" } }
            }.start()
        }.setNegativeButton("Cancel", null).show()
    }
    private fun deleteItem(it: Item) {
        val dangerous = it.path.startsWith("/etc/") || it.path == "/" || it.path.startsWith("/usr/")
        android.app.AlertDialog.Builder(this).setTitle("Delete ${it.name}?").setMessage(if (dangerous) "This is a system file - are you REALLY sure?" else "Delete this file?").setPositiveButton("Delete") { _, _ ->
            if (dangerous) { android.app.AlertDialog.Builder(this).setTitle("Confirm again").setMessage("System file - confirm delete").setPositiveButton("Delete") { _, _ -> doDelete(it) }.setNegativeButton("Cancel", null).show() } else doDelete(it)
        }.setNegativeButton("Cancel", null).show()
    }
    private fun doDelete(it: Item) {
        Thread {
            try {
                if (mode == "RECEIVER") { val ch = sftp()!!; try { if (it.isDir) ch.rmdir(it.path) else ch.rm(it.path) } finally { try { ch.session.disconnect() } catch (e: Exception) {} } }
                else { val f = File(it.path); if (f.isDirectory) f.deleteRecursively() else f.delete() }
                handler.post { refresh() }
            } catch (e: Exception) { handler.post { statusView.text = "Delete failed: ${e.message}" } }
        }.start()
    }
    private fun createFolder() {
        val ed = EditText(this).apply { hint = "Folder name" }
        android.app.AlertDialog.Builder(this).setTitle("New Folder").setView(ed).setPositiveButton("Create") { _, _ ->
            Thread {
                try {
                    if (mode == "RECEIVER") { val ch = sftp()!!; try { ch.mkdir(recvPath.trimEnd('/') + "/" + ed.text.toString()) } finally { try { ch.session.disconnect() } catch (e: Exception) {} } }
                    else File(mobPath, ed.text.toString()).mkdirs()
                    handler.post { refresh() }
                } catch (e: Exception) { handler.post { statusView.text = "Create failed: ${e.message}" } }
            }.start()
        }.setNegativeButton("Cancel", null).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ip = intent.getStringExtra("ip") ?: getSharedPreferences("uscript_prefs", MODE_PRIVATE).getString("ip", "") ?: ""
        // ===== تقسيمة Root Explorer - مجسمة جلوسي =====
        fun glossy3D(text: String, weight: Float, onClick: () -> Unit): Button {
            return Button(this).apply {
                this.text = text; textSize = 15f; setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply { leftMargin = dp(4); rightMargin = dp(4) }
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#4A4A4A"), Color.parseColor("#1A1A1A"), Color.parseColor("#000000"))).apply { cornerRadius = dp(20).toFloat(); setStroke(dp(2), Color.parseColor("#E8E8E8")) }
                setPadding(dp(8), dp(14), dp(8), dp(14))
                setOnTouchListener { v, e -> when (e.action) { android.view.MotionEvent.ACTION_DOWN -> { v.alpha = 0.6f; v.scaleX = 0.97f; v.scaleY = 0.97f } android.view.MotionEvent.ACTION_UP -> { v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f; onClick() } android.view.MotionEvent.ACTION_CANCEL -> { v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f } } ; true }
            }
        }
        fun glossyIcon(text: String, onClick: () -> Unit): Button {
            val b = glossy3D(text, 1f, onClick)
            b.textSize = 22f
            b.layoutParams = LinearLayout.LayoutParams(0, dp(56), 1f).apply { leftMargin = dp(4); rightMargin = dp(4) }
            return b
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#050505")); setPadding(dp(10), dp(10), dp(10), dp(10)) }
        // هيدر: فولدر مجسم + sdcard / المسار
        val headerIcon = TextView(this).apply { text = "📁"; textSize = 38f; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)) }
        val headerCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val headerTitle = TextView(this).apply { text = "sdcard"; textSize = 24f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val headerPath = TextView(this).apply { text = "/mnt"; textSize = 14f; setTextColor(Color.parseColor("#AAAAAA")) }
        headerCol.addView(headerTitle); headerCol.addView(headerPath)
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#2A2A2A"), Color.parseColor("#0A0A0A"))).apply { cornerRadius = dp(20).toFloat(); setStroke(dp(1), Color.parseColor("#E8E8E8")) }; setPadding(dp(12), dp(12), dp(12), dp(12)) }
        header.addView(headerIcon); header.addView(headerCol)
        // تابين مجسمين: RECEIVER / MOBILE زي Root / Storage
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) } }
        val btnRecv = glossy3D("RECEIVER", 1f) { mode = "RECEIVER"; headerTitle.text = "Receiver"; headerPath.text = recvPath; refresh() }
        val btnMob = glossy3D("MOBILE", 1f) { mode = "MOBILE"; headerTitle.text = "Mobile"; headerPath.text = mobPath; refresh() }
        tabs.addView(btnRecv); tabs.addView(btnMob)
        // سطر المساحة زي الصورة
        val infoBar = TextView(this).apply { text = "Loading storage info..."; textSize = 13f; setTextColor(Color.parseColor("#CCCCCC")); gravity = Gravity.CENTER; background = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(Color.parseColor("#111111")); setStroke(dp(1), Color.parseColor("#E8E8E8")) }; setPadding(dp(10), dp(10), dp(10), dp(10)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) } }
        pathView = TextView(this).apply { textSize = 13f; setTextColor(Color.WHITE) }
        statusView = TextView(this).apply { textSize = 12f; setTextColor(Color.parseColor("#9AA4C0")) }
        progressBar = ProgressBar(this).apply { visibility = android.view.View.GONE }
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(listBox); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        // بار تحت: 5 زراير مجسمة زي الصورة: رجوع + بحث + ترتيب + تيرمينال + قفل
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(Color.parseColor("#0A0A0A")); setStroke(dp(1), Color.parseColor("#E8E8E8")) }; setPadding(dp(6), dp(8), dp(6), dp(8)) }
        val btnBack = glossyIcon("↩") { if (mode == "RECEIVER") { recvPath = recvPath.trimEnd('/').substringBeforeLast("/", "/") + "/"; if (recvPath.isEmpty()) recvPath = "/" } else { mobPath = File(mobPath).parent ?: mobPath }; refresh() }
        val btnNew = glossyIcon("+") { Toast.makeText(this@FileManagerActivity, "Long-press a folder - New Folder here", Toast.LENGTH_SHORT).show() }
        val btnSearch = glossyIcon("🔍") { val e = EditText(this@FileManagerActivity).apply { hint = "Search..." }; android.app.AlertDialog.Builder(this@FileManagerActivity).setTitle("Search").setView(e).setPositiveButton("Search") { _, _ -> searchQuery = e.text.toString(); refresh() }.setNegativeButton("Clear") { _, _ -> searchQuery = ""; refresh() }.show() }
        val btnSort = glossyIcon("≡") { sortBy = if (sortBy == "name") "size" else "name"; refresh() }
        val btnClose = glossyIcon("✕") { finish() }
        bottom.addView(btnBack); bottom.addView(btnNew); bottom.addView(btnSearch); bottom.addView(btnSort); bottom.addView(btnClose)
        // ===== شبكة تصنيفات File Manager Plus =====
        fun catBtn(title: String, icon: String, cat: String): Button {
            val b = glossy3D(icon + "\n" + title, 1f) {
                categoryFilter = cat; isHome = false
                headerTitle.text = title; headerPath.text = if (mode == "RECEIVER") recvPath else mobPath
                refresh()
            }
            b.textSize = 13f
            b.layoutParams = LinearLayout.LayoutParams(0, dp(72), 1f).apply { leftMargin = dp(4); rightMargin = dp(4); bottomMargin = dp(8) }
            return b
        }
        val catRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        catRow1.addView(catBtn("Images", "🖼️", "images")); catRow1.addView(catBtn("Videos", "🎬", "videos")); catRow1.addView(catBtn("Audio", "🎵", "audio"))
        val catRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        catRow2.addView(catBtn("Docs", "📄", "docs")); catRow2.addView(catBtn("APKs", "📦", "apks")); catRow2.addView(catBtn("All Files", "📁", ""))
        val catAll = glossy3D("🏠  Home  •  All Files", 1f) { categoryFilter = ""; isHome = true; refresh() }
        root.addView(header); root.addView(tabs); root.addView(infoBar); root.addView(catRow1); root.addView(catRow2); root.addView(catAll); root.addView(pathView); root.addView(statusView); root.addView(progressBar); root.addView(scroll); root.addView(bottom)
        // تحديث سطر المساحة من الرسيفر
        Thread { try { val s = java.net.Socket(ip, 22); s.close() } catch (e: Exception) {} }.start()
        setContentView(root)
        refresh()
    }
}
