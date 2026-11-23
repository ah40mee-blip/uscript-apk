package com.uscript.web

import android.content.Context
import com.jcraft.jsch.JSch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// ScriptInstaller - تثبيت قسري: بيمسح القديم قبل ما يثبّت الجديد، وبعدين ريستارت
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

    fun installWithError(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Pair<Boolean, String> {
        try {
            val bytes = try { context.assets.open(ASSET_FILE).readBytes() } catch (e: Exception) {
                return false to "ملف uscript.sh مش موجود في assets"
            }
            if (bytes.isEmpty()) return false to "ملف uscript.sh فاضي"
            val firstLine = String(bytes).lines().firstOrNull { it.isNotBlank() } ?: ""
            val jsch = JSch()
            val session = jsch.getSession("root", ip, 22)
            session.setPassword(if (rootPassword.isEmpty()) "root" else rootPassword)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password,keyboard-interactive")
            try { session.connect(10000) } catch (e: Exception) {
                return false to "مش قادر اتصل بـ SSH على $ip:22 - ${e.message}"
            }
            try {
                val sftp = session.openChannel("sftp") as com.jcraft.jsch.ChannelSftp
                sftp.connect(10000)
                try { sftp.put(ByteArrayInputStream(bytes), "/tmp/uscript.sh") } catch (e: Exception) {
                    return false to "فشل رفع الملف: ${e.message}"
                } finally { try { sftp.disconnect() } catch (e: Exception) {} }

                // 1) اتأكد الملف اللي وصل للرسيفر هو الجديد فعلا
                val onBox = exec(session, "head -3 /tmp/uscript.sh")
                // 2) امسح الـ WebUI القديم قبل التثبيت - عشان الجديد ميتغطاش عليه
                exec(session, "rm -rf /tmp/upanel_old; rm -rf /usr/share/upanel /etc/upanel /var/www/html/upanel 2>/dev/null; echo cleaned", 2000)
                // 3) ثبّت الجديد قسري
                val ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand("export TERM=dumb FORCE=1; chmod +x /tmp/uscript.sh && sh /tmp/uscript.sh install < /dev/null")
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
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
                // 4) ريستارت للرسيفر عشان الـ WebUI الجديد يتحمل مش القديم المتخزن في الذاكرة
                exec(session, "sync; reboot", 1500)
                // رجّع أول سطر من الملف اللي اترفع عشان نعرف هو الجديد ولا القديم
                return true to "NEW_FILE_FIRST_LINE:$firstLine | ON_BOX:$onBox"
            } finally { try { session.disconnect() } catch (e: Exception) {} }
        } catch (e: Exception) {
            return false to "خطأ غير متوقع: ${e.message}"
        }
    }

    fun install(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Boolean {
        return installWithError(context, ip, rootPassword, onProgress).first
    }
}

