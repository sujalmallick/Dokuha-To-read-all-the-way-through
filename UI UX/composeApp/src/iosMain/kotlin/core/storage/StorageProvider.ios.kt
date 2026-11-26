package core.storage

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual class StorageProvider {
    actual fun getStorageStats(): DeviceStorageStats {
        val fileManager = NSFileManager.defaultManager
        val docsUrl = fileManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL
        val docsPath = docsUrl?.path ?: "/tmp"
        val downloadsPath = "$docsPath/downloads".toPath()

        var appBytes = 0L
        val fs = FileSystem.SYSTEM
        if (fs.exists(downloadsPath)) {
            fs.listRecursively(downloadsPath).forEach { path ->
                val meta = fs.metadataOrNull(path)
                if (meta?.isRegularFile == true) {
                    appBytes += meta.size ?: 0L
                }
            }
        }

        val attrs = runCatching {
            fileManager.attributesOfFileSystemForPath(docsPath, error = null)
        }.getOrNull()
        val totalBytes = (attrs?.get(NSFileSystemSize) as? NSNumber)?.longLongValue ?: 0L
        val freeBytes = (attrs?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue ?: 0L

        return DeviceStorageStats(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            appDownloadsBytes = appBytes
        )
    }
}
