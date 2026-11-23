package com.uscript.web

import android.content.Context
import com.jcraft.jsch.JSch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// ScriptInstaller مصغر - نفس فكرة MainActivity المصغر: صغير وبيعدي من الموبايل
// بيعتبر الـ OKs نجاح، وبيخرج أول ما يشوف Reboot Guard + Universal Edition
object ScriptInstaller {
    const val ASSET_FILE = "uscript.sh"

    fun installWithError(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Pair<Boolean, String> {
        try {
            val bytes = try { context.assets.open(ASSET_FILE).readBytes() } catch (e: Exception) {
                return false to "ملف uscript.sh مش موجود في assets"
            }
            if (bytes.isEmpty()) return false to "ملف uscript.sh فاضي"
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
                val ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand("export TERM=dumb; chmod +x /tmp/uscript.sh && sh /tmp/uscript.sh install < /dev/null")
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
                ch.setOutputStream(out); ch.setErrStream(err)
                ch.connect(10000)
                // متستناش القناة تتقفل - أول ما نشوف علامات النجاح نخرج فورا
                var saw = false
                var w = 0
                while (!ch.isClosed && w < 120) {
                    Thread.sleep(500); w++
                    val so = out.toString()
                    if (so.contains("Reboot Guard") && so.contains("Universal Edition")) {
                        Thread.sleep(2000); saw = true; break
                    }
                }
                val exit = ch.exitStatus
                fun clean(t: String): String { return t.replace(Regex("\u001B\\[[0-9;]*[a-zA-Z]"), "").trim() }
                val outT = clean(out.toString())
                val errT = clean(err.toString())
                try { ch.disconnect() } catch (e: Exception) {}
                val ok = saw || outT.contains("[OK]") || outT.contains("Universal Edition") || outT.contains("Reboot Guard")
                if (exit != 0 && !ok) {
                    return false to "الاسكريبت اترفع بس التثبيت رجع خطأ (exit=$exit). ${if (errT.isNotEmpty()) errT else outT}".take(300)
                }
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

