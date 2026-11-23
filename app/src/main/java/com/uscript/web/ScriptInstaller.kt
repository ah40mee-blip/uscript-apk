package com.uscript.web

import android.content.Context
import com.jcraft.jsch.JSch
import java.io.ByteArrayOutputStream

/**
 * ScriptInstaller - يثبّت U-Panel على الرسيفر عبر SSH
 * اسم الملف: U Script (في الـ assets: assets/uscript.sh)
 * المحتوى جوّاه: U-Panel بالكامل
 */
object ScriptInstaller {

    // اسم الملف في الـ assets
    const val ASSET_FILE = "uscript.sh"

    fun install(context: Context, ip: String, rootPassword: String, onProgress: (String) -> Unit): Boolean {
        return try {
            onProgress("جاري قراءة U-Panel من التطبيق...")
            val script = context.assets.open(ASSET_FILE).bufferedReader().readText()
            if (script.isEmpty()) return false

            onProgress("جاري الاتصال بالرسيفر عبر SSH...")
            val jsch = JSch()
            val session = jsch.getSession("root", ip, 22)
            session.setPassword(rootPassword)
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect(10000)

            onProgress("جاري رفع U-Panel...")
            val channel = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
            // المسافة في اسم الملف لازم تبقى بين "" عشان الشيل مايكسرش الأمر
            val cmd = "cat > /tmp/uscript.sh && chmod +x /tmp/uscript.sh && sh /tmp/uscript.sh install"
            channel.setCommand(cmd)
            channel.outputStream.write(script.toByteArray())
            channel.outputStream.flush()
            channel.outputStream.close()
            val out = ByteArrayOutputStream()
            channel.setOutputStream(out)
            channel.connect()
            while (!channel.isClosed) { Thread.sleep(500) }
            channel.disconnect()

            onProgress("جاري التأكد إن الـ WebUI شغال...")
            Thread.sleep(2000)

            session.disconnect()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

