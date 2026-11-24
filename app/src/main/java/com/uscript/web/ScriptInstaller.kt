package com.uscript.web

import android.content.Context
import com.jcraft.jsch.JSch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// ScriptInstaller - يزرع الاسكريبت في مكانه الأصلي اللي الـ WebUI متعود عليه
// /media/hdd/U-Panel/uscript.sh - مفيش نقل ولا تصليح مسارات
object ScriptInstaller {
    const val ASSET_FILE = "uscript.sh"
    const val PANEL_DIR = "/media/hdd/U-Panel"
    const val SCRIPT_PATH = "/media/hdd/U-Panel/uscript.sh"

    fun installWithError(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Pair<Boolean, String> {
        try {
            val bytes = try { context.assets.open(ASSET_FILE).readBytes() } catch (e: Exception) {
                return false to "ملف uscript.sh مش موجود في assets"
            }
            if (bytes.isEmpty()) return false to "ملف uscript.sh فاضي"
            val session = try {
                val s = JSch().getSession("root", ip, 22)
                s.setPassword(if (rootPassword.isEmpty()) "root" else rootPassword)
                s.setConfig("StrictHostKeyChecking", "no")
                s.setConfig("PreferredAuthentications", "password,keyboard-interactive")
                s.connect(10000); s
            } catch (e: Exception) { return false to "مش قادر اتصل بـ SSH على $ip:22 - ${e.message}" }
            try {
                var ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand("mkdir -p $PANEL_DIR /media/hdd/backup && chmod 755 $PANEL_DIR && echo MKDIR_OK")
                var out = ByteArrayOutputStream(); ch.setOutputStream(out); ch.connect(10000)
                while (!ch.isClosed) Thread.sleep(300)
                try { ch.disconnect() } catch (e: Exception) {}

                val sftp = session.openChannel("sftp") as com.jcraft.jsch.ChannelSftp
                sftp.connect(10000)
                try { sftp.put(ByteArrayInputStream(bytes), SCRIPT_PATH) } catch (e: Exception) {
                    return false to "فشل زرع الاسكريبت في $PANEL_DIR: ${e.message}"
                } finally { try { sftp.disconnect() } catch (e: Exception) {} }

                // شغّله بأمر التشغيل من مكانه الأصلي - من غير أي تصليح مسارات
                ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand("""export TERM=dumb; chmod +x $SCRIPT_PATH; cd $PANEL_DIR; setsid nohup sh $SCRIPT_PATH </dev/null > $PANEL_DIR/.run.log 2>&1 & disown; echo RUN_OK; sleep 2; cat $PANEL_DIR/.run.log 2>/dev/null | tail -20""")
                out = ByteArrayOutputStream(); val err = ByteArrayOutputStream()
                ch.setOutputStream(out); ch.setErrStream(err)
                ch.connect(10000)
                var saw = false; var w = 0
                while (!ch.isClosed && w < 60) {
                    Thread.sleep(500); w++
                    val so = out.toString()
                    if (so.contains("RUN_OK") || (so.contains("Reboot Guard") && so.contains("Universal Edition"))) { Thread.sleep(1000); saw = true; break }
                }
                val exit = ch.exitStatus
                fun clean(t: String): String { return t.replace(Regex("\u001B\\[[0-9;]*[a-zA-Z]"), "").trim() }
                val outT = clean(out.toString()); val errT = clean(err.toString())
                try { ch.disconnect() } catch (e: Exception) {}
                val ok = outT.contains("RUN_OK") || saw || outT.contains("[OK]") || outT.contains("Universal Edition")
                if (exit != 0 && exit != -1 && !ok) {
                    return false to "الزرع تم بس التشغيل رجع خطأ (exit=$exit). ${if (errT.isNotEmpty()) errT else outT}".take(300)
                }
                Thread.sleep(2000)
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
