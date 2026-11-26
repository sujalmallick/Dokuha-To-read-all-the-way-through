package data.backup

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual object BackupFileStore {
    private val fs = FileSystem.SYSTEM

    private fun backupsDir(): Path {
        val docsUrl = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL
        val basePath = docsUrl?.path ?: "/tmp"
        val dir = "$basePath/backups".toPath()
        fs.createDirectories(dir)
        return dir
    }

    actual fun writeBackup(fileName: String, content: String): Result<String> = runCatching {
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target = backupsDir() / safeName
        fs.write(target) { writeUtf8(content) }
        target.toString()
    }

    actual fun readBackup(fileName: String): Result<String> = runCatching {
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target = backupsDir() / safeName
        if (!fs.exists(target)) error("Backup file not found: $safeName")
        fs.read(target) { readUtf8() }
    }

    actual fun listBackups(): List<String> {
        val dir = backupsDir()
        if (!fs.exists(dir)) return emptyList()
        return fs.list(dir).map { it.name }.sortedDescending()
    }
}
