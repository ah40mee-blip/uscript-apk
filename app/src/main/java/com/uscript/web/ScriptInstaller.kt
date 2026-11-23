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
    fun installWithError(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Pair<Boolean, String> {
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
            val session = jsch.getSession("root", ip, 22)
            if (rootPassword.isNotEmpty()) session.setPassword(rootPassword)
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
                channel.setCommand("chmod +x /tmp/uscript.sh && sh /tmp/uscript.sh install")
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
                channel.setOutputStream(out)
                channel.setErrStream(err)
                channel.connect(10000)
                // استنى لحد ما التثبيت يخلص - لحد 60 ثانية
                var waited = 0
                while (!channel.isClosed && waited < 120) {
                    Thread.sleep(500)
                    waited++
                }
                val exit = channel.exitStatus
                val errText = err.toString().trim()
                val outText = out.toString().trim()
                try { channel.disconnect() } catch (e: Exception) {}
                if (exit != 0) {
                    return false to "الاسكريبت اترفع بس التثبيت رجع خطأ (exit=$exit). ${if (errText.isNotEmpty()) errText else outText}".take(300)
                }
            } finally {
                try { session.disconnect() } catch (e: Exception) {}
            }

            onProgress("تم التثبيت بنجاح")
            return true to ""
        } catch (e: Exception) {
            e.printStackTrace()
            return false to "خطأ غير متوقع: ${e.message}"
        }
    }

    // الدالة القديمة - عشان MainActivity القديم ميكسرش
    fun install(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Boolean {
        return installWithError(context, ip, rootPassword, onProgress).first
    }
}

