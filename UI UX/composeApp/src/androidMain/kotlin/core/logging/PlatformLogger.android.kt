package core.logging

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.context.GlobalContext
import java.io.File

actual object PlatformLogger {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getContext(): Context? {
        if (appContext != null) return appContext
        return try {
            GlobalContext.getOrNull()?.get<Context>()
        } catch (_: Throwable) {
            null
        }
    }

    actual fun getLogDirectory(): Path {
        val ctx = getContext()
        val dir = if (ctx != null) {
            File(ctx.filesDir, "logs")
        } else {
            File(System.getProperty("java.io.tmpdir") ?: "/tmp", "dokuha_logs")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath.toPath()
    }

    actual fun shareLogFile(filePath: Path) {
        val ctx = getContext() ?: return
        val file = File(filePath.toString())
        if (!file.exists()) return

        val uri: Uri = try {
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        } catch (_: Throwable) {
            Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dokuha Diagnostic Logs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Diagnostic Logs").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(chooser)
    }
}
