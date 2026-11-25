package core.logging

import okio.Path
import okio.Path.Companion.toPath
import java.awt.Desktop
import java.io.File

actual object PlatformLogger {
    actual fun getLogDirectory(): Path {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, ".dokuha/logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath.toPath()
    }

    actual fun shareLogFile(filePath: Path) {
        try {
            val file = File(filePath.toString())
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.parentFile ?: file)
            }
        } catch (_: Throwable) {
        }
    }
}
