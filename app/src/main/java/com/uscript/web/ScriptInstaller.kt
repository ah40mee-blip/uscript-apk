package com.uscript.web

// ScriptInstaller.kt — التثبيت الأوتوماتيك
// الفكرة: الاسكريبت موجود جوه الـ APK في app/src/main/assets/uscript.sh (من غير أي تعديل)
// أول مرة التطبيق يتصل بريسيفر مفيهوش الاسكريبت:
//   1) يرفعه بالـ FTP على /usr/script/uscript.sh
//   2) يديله صلاحية تشغيل + يشغّل الـ WebUI بتاعه بالـ Telnet
//   3) يستنى الـ WebUI يرد على Port 8080 وبعدين يكمل
// ملحوظة: Enigma2 الافتراضي FTP/Telnet يوزر root من غير باسورد. لو الريسيفر عليه باسورد، التطبيق هيجرب الـ Web PIN اللي كتبته.

import java.io.*
import java.net.Socket
import java.net.HttpURLConnection
import java.net.URL

object ScriptInstaller {

    data class Result(val ok: Boolean, val step: String, val error: String = "")

    // خطوة 1: رفع الاسكريبت بالـ FTP
    fun ftpUpload(host: String, user: String, pass: String, data: ByteArray, remotePath: String): Boolean {
        var ctrl: Socket? = null
        var dataSock: Socket? = null
        return try {
            ctrl = Socket(host, 21).apply { soTimeout = 15000 }
            val reader = BufferedReader(InputStreamReader(ctrl.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(ctrl.getOutputStream()))
            fun cmd(c: String): String {
                writer.write(c + "\r\n"); writer.flush()
                var line = reader.readLine() ?: ""
                // ردود FTP ممكن تكون كذا سطر - بنقرا لحد ما نوصل للكود النهائي
                while (line.length >= 4 && line[3] == '-') { line = reader.readLine() ?: "" }
                return line
            }
            reader.readLine() // رسالة الترحيب
            if (!cmd("USER $user").startsWith("2") && !cmd("USER $user").startsWith("3")) {
                // لو اليوزر لوحده مكفاش بنكمل
            }
            val passResp = cmd("PASS $pass")
            if (passResp.startsWith("5")) return false
            if (!cmd("TYPE I").startsWith("2")) return false
            // PASV
            val pasv = cmd("PASV")
            val s = pasv.indexOf('('); val e = pasv.indexOf(')', s)
            if (s < 0 || e < 0) return false
            val parts = pasv.substring(s + 1, e).split(",")
            val dataHost = parts.subList(0, 4).joinToString(".")
            val dataPort = parts[4].toInt() * 256 + parts[5].toInt()
            dataSock = Socket(dataHost, dataPort).apply { soTimeout = 30000 }
            val stor = cmd("STOR $remotePath")
            if (!stor.startsWith("1") && !stor.startsWith("2")) return false
            dataSock.getOutputStream().write(data); dataSock.getOutputStream().flush(); dataSock.close(); dataSock = null
            val done = reader.readLine() ?: ""
            if (!done.startsWith("2")) return false
            cmd("QUIT")
            true
        } catch (ex: Exception) {
            false
        } finally {
            try { dataSock?.close() } catch (_: Exception) {}
            try { ctrl?.close() } catch (_: Exception) {}
        }
    }

    // خطوة 2: تشغيل الاسكريبت بالـ Telnet
    fun telnetRun(host: String, user: String, pass: String, commands: List<String>): Boolean {
        return try {
            val sock = Socket(host, 23).apply { soTimeout = 15000 }
            val input = sock.getInputStream(); val out = sock.getOutputStream()
            fun sleep(ms: Long) { try { Thread.sleep(ms) } catch (_: Exception) {} }
            fun drain() { try { while (input.available() > 0) input.read() } catch (_: Exception) {} }
            fun send(line: String) { out.write((line + "\n").toByteArray()); out.flush() }
            sleep(1200); drain()
            // تسجيل دخول Telnet - بنجرب root وبعدين الباسورد لو اتطلب
            send(user); sleep(800); drain()
            if (pass.isNotEmpty()) { send(pass); sleep(800); drain() }
            for (c in commands) { send(c); sleep(900); drain() }
            send("exit"); sleep(400)
            sock.close(); true
        } catch (e: Exception) { false }
    }

    fun isWebUiUp(ip: String, port: String): Boolean {
        return try {
            val c = URL("http://$ip:$port/").openConnection() as HttpURLConnection
            c.connectTimeout = 3000; c.readTimeout = 3000; c.requestMethod = "GET"; c.connect(); c.responseCode in 200..499
        } catch (e: Exception) { false }
    }

    // الدالة الرئيسية: بتتنادى من MainActivity بعد الاتصال
    fun autoInstall(
        context: android.content.Context,
        ip: String, port: String, webPin: String,
        onStep: (String) -> Unit
    ): Result {
        // لو الـ WebUI شغال أصلاً -> الاسكريبت موجود، مفيش حاجة تتعمل
        if (isWebUiUp(ip, port)) return Result(true, "already-installed")
        onStep("Uploading U-Script to receiver...")
        // قراءة الاسكريبت من assets
        val bytes = try {
            context.assets.open("uscript.sh").readBytes()
        } catch (e: Exception) {
            return Result(false, "read-assets", "uscript.sh مش موجود في assets - حطه في app/src/main/assets/")
        }
        // بنجرب كذا باسورد: فاضي، root، وبعدين Web PIN
        val passwords = listOf("", "root", webPin).distinct()
        var uploaded = false
        for (pw in passwords) {
            if (ftpUpload(ip, "root", pw, bytes, "/usr/script/uscript.sh")) { uploaded = true; break }
        }
        if (!uploaded) return Result(false, "ftp", "FTP فشل - اتأكد إن الريسيفر فاتح FTP والـ Wi-Fi واحد")
        onStep("Setting permissions + starting WebUI...")
        val cmds = listOf(
            "mkdir -p /usr/script",
            "chmod +x /usr/script/uscript.sh",
            "nohup /usr/script/uscript.sh --web-start-bg > /dev/null 2>&1 &",
            "sleep 2"
        )
        var started = false
        for (pw in passwords) {
            if (telnetRun(ip, "root", pw, cmds)) { started = true; break }
        }
        if (!started) return Result(false, "telnet", "Telnet فشل - اتأكد إن الريسيفر فاتح Telnet")
        // استنى الـ WebUI يقوم
        onStep("Waiting for WebUI...")
        repeat(10) {
            Thread.sleep(1500)
            if (isWebUiUp(ip, port)) return Result(true, "installed")
        }
        return Result(false, "webui", "الاسكريبت اترفع بس الـ WebUI مردش - جرب Reconnect")
    }
}
