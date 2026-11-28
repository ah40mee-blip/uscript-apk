package com.uscript.web

import android.content.Context
import com.jcraft.jsch.JSch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * ScriptInstaller - يثبّت U-Panel على الرسيفر عبر SSH
 * اسم الملف: uscript.sh في assets/uscript.sh
 * المحتوى: U-Panel
 */
object ScriptInstaller {

    const val ASSET_FILE = "uscript.sh"

    // بيرجع Pair: نجح ولا لأ + رسالة الخطأ الحقيقية عشان تظهر في التطبيق بدل "فشل التثبيت" العام
    // الدالة الجديدة: بيوزر + باسورد من الخانات
    fun installWithError(context: Context, ip: String, sshUser: String, rootPassword: String, onProgress: (String) -> Unit): Pair<Boolean, String> {
        try {
            onProgress("جاري قراءة U-Panel من التطبيق...")
            val scriptBytes: ByteArray
            try {
                scriptBytes = context.assets.open(ASSET_FILE).readBytes()
            } catch (e: Exception) {
                return false to "ملف uscript.sh مش موجود في assets. اتأكد انك رافع نسختك في app/src/main/assets/uscript.sh"
            }
            if (scriptBytes.isEmpty()) return false to "ملف uscript.sh فاضي - ارفع نسختك الكاملة تاني"

            onProgress("جاري الاتصال بالرسيفر عبر SSH (بورت 22)...")
            val jsch = JSch()
            val user = if (sshUser.isEmpty()) "root" else sshUser
            val session = jsch.getSession(user, ip, 22)
            val pw = if (rootPassword.isEmpty()) "root" else rootPassword
            session.setPassword(pw)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password,keyboard-interactive")
            try {
                session.connect(10000)
            } catch (e: Exception) {
                return false to "مش قادر اتصل بـ SSH على $ip:22. اتأكد ان SSH شغال على الرسيفر، والباسورد صح. الخطأ: ${e.message}"
            }

            try {
                onProgress("جاري رفع U-Panel للرسيفر...")
                // SFTP أضمن بكتير من cat - مبيتكسرش مع الملفات الكبيرة
                val sftp = session.openChannel("sftp") as com.jcraft.jsch.ChannelSftp
                sftp.connect(10000)
                try {
                    sftp.put(ByteArrayInputStream(scriptBytes), "/tmp/uscript.sh")
                } catch (e: Exception) {
                    return false to "فشل رفع الملف للرسيفر: ${e.message}"
                } finally {
                    try { sftp.disconnect() } catch (e: Exception) {}
                }

                onProgress("جاري تثبيت U-Panel على الرسيفر...")
                val channel = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                channel.setCommand("export TERM=dumb; chmod +x /tmp/uscript.sh && sh /tmp/uscript.sh install < /dev/null")
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
                channel.setOutputStream(out)
                channel.setErrStream(err)
                channel.connect(10000)
                // متستناش القناة تتقفل - السكريبت تفاعلي ومبيقفلش لوحده
                // أول ما نشوف علامات النجاح في الخرج نخرج فورا، مش نستنى 180 ثانية
                var waited = 0
                var sawSuccess = false
                while (!channel.isClosed && waited < 120) {
                    Thread.sleep(500)
                    waited++
                    val soFar = out.toString()
                    // علامات إن التثبيت خلص فعلا (بتظهر في أول 5-10 ثواني)
                    if (soFar.contains("Reboot Guard") && soFar.contains("Universal Edition")) {
                        // استنى ثانيتين كمان للتأكيد وبعدين اخرج
                        Thread.sleep(2000)
                        sawSuccess = true
                        break
                    }
                }
                val exit = channel.exitStatus
                // شيل أكواد الألوان ANSI من الخرج عشان الرسالة تبقى نضيفة
                fun clean(t: String): String { return t.replace(Regex("\\u001B\\[[0-9;]*[a-zA-Z]"), "").replace(Regex("\\[H\\[J"), "").trim() }
                val errText = clean(err.toString())
                val outText = clean(out.toString())
                try { channel.disconnect() } catch (e: Exception) {}
                // السكريبت ده تفاعلي: ساعات بيفضل فاتح وميقفلش القناة (exit=-1) مع إنه خلص التثبيت وطبع OKs
                // فلو طبع OKs يبقى نجح حتى لو exit != 0
                val looksInstalled = sawSuccess || outText.contains("[OK]") || outText.contains("Universal Edition") || outText.contains("Reboot Guard")
                if (exit != 0 && !looksInstalled) {
                    return false to "الاسكريبت اترفع بس التثبيت رجع خطأ (exit=$exit). ${if (errText.isNotEmpty()) errText else outText}".take(300)
                }
            } finally {
                try { session.disconnect() } catch (e: Exception) {}
            }

            // بعد الزرع: 1) شغّل السيرفر 2) اعيد تشغيل السيرفر عشان يتحمل نضيف
            try {
                val s2 = jsch.getSession(user, ip, 22)
                s2.setPassword(pw); s2.setConfig("StrictHostKeyChecking", "no"); s2.setConfig("PreferredAuthentications", "password,keyboard-interactive")
                s2.connect(10000)
                try {
                    fun exec2(cmd: String, waitMs: Long = 3000): String {
                        val ch = s2.openChannel("exec") as com.jcraft.jsch.ChannelExec
                        ch.setCommand(cmd)
                        val o = ByteArrayOutputStream(); ch.setOutputStream(o); ch.setErrStream(ByteArrayOutputStream())
                        ch.connect(10000); Thread.sleep(waitMs)
                        val r = o.toString(); try { ch.disconnect() } catch (e: Exception) {}; return r
                    }
                    onProgress("جاري تشغيل السيرفر...")
                    exec2("chmod +x /tmp/uscript.sh /etc/uscript.sh 2>/dev/null; cp /tmp/uscript.sh /etc/uscript.sh 2>/dev/null; sh /tmp/uscript.sh start < /dev/null 2>/dev/null; echo STARTED", 4000)
                    // إعادة تشغيل السيرفر - الرسيفر نفسه مش هيطفي، السيرفر بس هيقوم من جديد
                    exec2("sh /tmp/uscript.sh restart-server < /dev/null 2>/dev/null; sh /etc/uscript.sh restart-server < /dev/null 2>/dev/null; echo RESTARTED", 4000)
                    // لو restart-server مش موجود في السكربت - اعمل reboot كامل كبديل آمن
                    val check = exec2("ss -tlnp 2>/dev/null | grep 8080; netstat -tlnp 2>/dev/null | grep 8080; echo CHECK", 2000)
                    if (!check.contains("8080")) { exec2("sync; reboot", 1500) }
                } finally { try { s2.disconnect() } catch (e: Exception) {} }
            } catch (e: Exception) { /* لو فشل الـ start - الـ MainActivity هيستنى السيرفر 90 ثانية وهيحاول تاني */ }
            onProgress("تم التثبيت + تشغيل السيرفر + إعادة تشغيل السيرفر")
            return true to ""
        } catch (e: Exception) {
            e.printStackTrace()
            return false to "خطأ غير متوقع: ${e.message}"
        }
    }

    // توافق قديم: بيوزر root
    fun installWithError(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Pair<Boolean, String> {
        return installWithError(context, ip, "root", rootPassword, onProgress)
    }
    fun install(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Boolean {
        return installWithError(context, ip, "root", rootPassword, onProgress).first
    }
}
