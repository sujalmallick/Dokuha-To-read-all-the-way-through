package data.backup

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

actual object BackupFileStore : KoinComponent {
    private val context: Context by inject()

    private fun backupsDir(): File = File(context.filesDir, "backups").apply { mkdirs() }

    actual fun writeBackup(fileName: String, content: String): Result<String> = runCatching {
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target = File(backupsDir(), safeName)
        target.writeText(content)
        target.absolutePath
    }

    actual fun readBackup(fileName: String): Result<String> = runCatching {
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target = File(backupsDir(), safeName)
        if (!target.exists()) error("Backup file not found: $safeName")
        target.readText()
    }

    actual fun listBackups(): List<String> {
        return backupsDir().listFiles()?.map { it.name }?.sortedDescending() ?: emptyList()
    }
}
