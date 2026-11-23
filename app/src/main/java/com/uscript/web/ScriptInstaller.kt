package com.uscript.web

import android.content.Context
import com.jcraft.jsch.JSch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// ScriptInstaller - من غير مسح مدمر للـ WebUI القديم
// ENTER PANEL بقى بيشغّل سيرفر الـ WebUI الأول قبل ما يفتح
object ScriptInstaller {
    const val ASSET_FILE = "uscript.sh"

    private fun exec(session: com.jcraft.jsch.Session, cmd: String, waitMs: Long = 0): String {
        return try {
            val ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
            ch.setCommand(cmd)
            val out = ByteArrayOutputStream()
            ch.setOutputStream(out); ch.setErrStream(ByteArrayOutputStream())
            ch.connect(10000)
            if (waitMs > 0) Thread.sleep(waitMs)
            else { var w = 0; while (!ch.isClosed && w < 60) { Thread.sleep(500); w++ } }
            val r = out.toString()
            try { ch.disconnect() } catch (e: Exception) {}
            r
        } catch (e: Exception) { "" }
    }

    private fun connect(ip: String, pw: String): com.jcraft.jsch.Session? {
        return try {
            val s = JSch().getSession("root", ip, 22)
            s.setPassword(if (pw.isEmpty()) "root" else pw)
            s.setConfig("StrictHostKeyChecking", "no")
            s.setConfig("PreferredAuthentications", "password,keyboard-interactive")
            s.connect(10000); s
        } catch (e: Exception) { null }
    }

    // شغّل سيرفر الـ WebUI - جرّب كل الطرق المعروفة، واحدة منهم هتشتغل
    fun startWebUi(context: Context, ip: String, rootPassword: String): Boolean {
        val session = connect(ip, rootPassword) ?: return false
        try {
            // لو السيرفر شغال أصلا متعملش حاجة
            val check = exec(session, "ss -tlnp 2>/dev/null | grep 8080; netstat -tlnp 2>/dev/null | grep 8080; echo CHECK_DONE")
            if (check.contains("8080")) return true
            // جرّب تشغيله بالطرق المختلفة - السكريبت بتاعك هو اللي بيعرف يشغّله
            exec(session, "chmod +x /tmp/uscript.sh 2>/dev/null; sh /tmp/uscript.sh start < /dev/null 2>/dev/null; echo START1", 3000)
            var again = exec(session, "ss -tlnp 2>/dev/null | grep 8080; netstat -tlnp 2>/dev/null | grep 8080; echo CHECK2")
            if (again.contains("8080")) return true
            exec(session, "systemctl restart uscript 2>/dev/null; /etc/init.d/uscript start 2>/dev/null; service uscript start 2>/dev/null; echo START2", 3000)
            again = exec(session, "ss -tlnp 2>/dev/null | grep 8080; netstat -tlnp 2>/dev/null | grep 8080; echo CHECK3")
            if (again.contains("8080")) return true
            // آخر محاولة: شغّل السكريبت install تاني - هو بيظبط السيرفر لوحده
            return false
        } finally { try { session.disconnect() } catch (e: Exception) {} }
    }

    fun installWithError(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Pair<Boolean, String> {
        try {
            val bytes = try { context.assets.open(ASSET_FILE).readBytes() } catch (e: Exception) {
                return false to "ملف uscript.sh مش موجود في assets"
            }
            if (bytes.isEmpty()) return false to "ملف uscript.sh فاضي"
            val session = connect(ip, rootPassword) ?: return false to "مش قادر اتصل بـ SSH على $ip:22"
            try {
                val sftp = session.openChannel("sftp") as com.jcraft.jsch.ChannelSftp
                sftp.connect(10000)
                try { sftp.put(ByteArrayInputStream(bytes), "/tmp/uscript.sh") } catch (e: Exception) {
                    return false to "فشل رفع الملف: ${e.message}"
                } finally { try { sftp.disconnect() } catch (e: Exception) {} }
                // من غير rm -rf للـ WebUI - النسخة القسرية اللي فاتت هي اللي بوّظت السيرفر
                val ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand("export TERM=dumb FORCE=1; chmod +x /tmp/uscript.sh && sh /tmp/uscript.sh install < /dev/null")
                val out = ByteArrayOutputStream(); val err = ByteArrayOutputStream()
                ch.setOutputStream(out); ch.setErrStream(err)
                ch.connect(10000)
                var saw = false; var w = 0
                while (!ch.isClosed && w < 120) {
                    Thread.sleep(500); w++
                    val so = out.toString()
                    if (so.contains("Reboot Guard") && so.contains("Universal Edition")) { Thread.sleep(2000); saw = true; break }
                }
                val exit = ch.exitStatus
                fun clean(t: String): String { return t.replace(Regex("\u001B\\[[0-9;]*[a-zA-Z]"), "").trim() }
                val outT = clean(out.toString()); val errT = clean(err.toString())
                try { ch.disconnect() } catch (e: Exception) {}
                val ok = saw || outT.contains("[OK]") || outT.contains("Universal Edition")
                if (exit != 0 && !ok) {
                    return false to "التثبيت رجع خطأ (exit=$exit). ${if (errT.isNotEmpty()) errT else outT}".take(300)
                }
                // بعد التثبيت: شغّل السيرفر الأول، وبعدين ريستارت عشان يتحمل نضيف
                exec(session, "sh /tmp/uscript.sh start < /dev/null 2>/dev/null; echo STARTED", 3000)
                exec(session, "sync; reboot", 1500)
                return true to ""
            } finally { try { session.disconnect() } catch (e: Exception) {} }
        } catch (e: Exception) {
            return false to "خطأ غير متوقع: ${e.message}"
        }
    }

    fun install(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Boolean {
        return installWithError(context, ip, rootPassword, onProgress).first
    }
}
