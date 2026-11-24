package com.uscript.web

import android.content.Context
import com.jcraft.jsch.JSch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// ScriptInstaller - الاسكريبت في /etc/enigma2/ - الباكب بس في /media/hdd/backup/
// CONFIGURE PANEL = زرع + تشغيل + Panel Configured
object ScriptInstaller {
    const val ASSET_FILE = "uscript.sh"
    const val PANEL_DIR = "/etc/enigma2"  // الملف ظاهر هنا
    const val SCRIPT_PATH = "/etc/enigma2/uscript.sh"  // ظاهر في etc/enigma2

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
                ch.setCommand("mkdir -p $PANEL_DIR && chmod 700 $PANEL_DIR && chmod 755 $PANEL_DIR && echo MKDIR_OK")
                var out = ByteArrayOutputStream(); ch.setOutputStream(out); ch.connect(10000)
                while (!ch.isClosed) Thread.sleep(300)
                try { ch.disconnect() } catch (e: Exception) {}

                // 2) ازرع الاسكريبت في /media/hdd/U-Panel/
                val sftp = session.openChannel("sftp") as com.jcraft.jsch.ChannelSftp
                sftp.connect(10000)
                try { sftp.put(ByteArrayInputStream(bytes), SCRIPT_PATH) } catch (e: Exception) {
                    return false to "فشل زرع الاسكريبت في $PANEL_DIR: ${e.message}"
                } finally { try { sftp.disconnect() } catch (e: Exception) {} }

                // 3) شغّله بأمر التشغيل - من غير install
                // nohup + & مهم: عشان السيرفر يفضل شغال بعد ما SSH يقفل
                ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand("export TERM=dumb; pkill -f uscript.sh 2>/dev/null; pkill -f U-Panel 2>/dev/null; sleep 1; grep -rl "/media/hdd/U-Panel" /etc/enigma2 /tmp /usr/lib/enigma2 /var/lib 2>/dev/null | xargs sed -i 's|/media/hdd/U-Panel|/etc/enigma2|g' 2>/dev/null; mkdir -p /media/hdd/backup; sed -i 's|/media/hdd/U-Panel/uscript.sh|/etc/enigma2/uscript.sh|g; s|/media/hdd/U-Panel|/etc/enigma2|g' $SCRIPT_PATH; sed -i '/backup/I s|/etc/enigma2|/media/hdd/backup|g; /restore/I s|/etc/enigma2|/media/hdd/backup|g' $SCRIPT_PATH; grep -rl "/etc/enigma2" /etc/enigma2 2>/dev/null | xargs sed -i '/backup/I s|/etc/enigma2|/media/hdd/backup|g; /restore/I s|/etc/enigma2|/media/hdd/backup|g' 2>/dev/null; chmod +x $SCRIPT_PATH && rm -f /media/hdd/U-Panel/uscript.sh /media/hdd/U-Panel/.uscript.sh 2>/dev/null; cd $PANEL_DIR && nohup sh $SCRIPT_PATH > $PANEL_DIR/.run.log 2>&1 & echo RUN_OK; sleep 2; cat $PANEL_DIR/.run.log 2>/dev/null | tail -20")
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
                val ok = outT.contains("RUN_OK") || saw || outT.contains("[OK]") || outT.contains("Universal Edition")
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
