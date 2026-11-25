package com.uscript.web

import android.content.Context
import com.jcraft.jsch.JSch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// ScriptInstaller - يزرع الاسكريبت في /media/hdd/U-Panel/ ويشغّله
// CONFIGURE PANEL = زرع + تشغيل + Panel Configured
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
                // 1) اعمل الفولدر
                var ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand("mkdir -p $PANEL_DIR && chmod 755 $PANEL_DIR && echo MKDIR_OK")
                var out = ByteArrayOutputStream(); ch.setOutputStream(out); ch.connect(10000)
                while (!ch.isClosed) Thread.sleep(300)
                try { ch.disconnect() } catch (e: Exception) {}

                // 2) ازرع الاسكريبت في /media/hdd/U-Panel/
                val sftp = session.openChannel("sftp") as com.jcraft.jsch.ChannelSftp
                sftp.connect(10000)
                try { sftp.put(ByteArrayInputStream(bytes), SCRIPT_PATH) } catch (e: Exception) {
                    return false to "فشل زرع الاسكريبت في $PANEL_DIR: ${e.message}"
                } finally { try { sftp.disconnect() } catch (e: Exception) {} }

                // 3) شغّله - السيرفر هيقوم اوتوماتيك من جوه الاسكريبت
                ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand("""export TERM=dumb; chmod +x $SCRIPT_PATH; cd $PANEL_DIR; setsid nohup sh $SCRIPT_PATH --restart-server > $PANEL_DIR/.run.log 2>&1 < /dev/null & disown; sleep 4; echo RUN_OK; (netstat -tlnp 2>/dev/null | grep 8080 || ss -tlnp 2>/dev/null | grep 8080 || echo NO_PORT); ps 2>/dev/null | grep -v grep | grep uscript | head -3; cat $PANEL_DIR/.run.log 2>/dev/null | tail -20""")
                out = ByteArrayOutputStream(); val err = ByteArrayOutputStream()
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
                    return false to "الزرع تم بس التشغيل رجع خطأ (exit=$exit). ${if (errT.isNotEmpty()) errT else outT}".take(300)
                }
                // استنى ثانيتين السيرفر يقوم
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
