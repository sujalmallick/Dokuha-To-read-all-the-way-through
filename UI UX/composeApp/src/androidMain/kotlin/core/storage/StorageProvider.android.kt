package core.storage

import android.content.Context
import android.os.StatFs
import java.io.File

actual class StorageProvider(private val context: Context) {
    actual fun getStorageStats(): DeviceStorageStats {
        val downloadsDir = File(context.filesDir, "downloads")
        val appBytes = if (downloadsDir.exists()) {
            downloadsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L

        val stat = runCatching { StatFs(context.filesDir.absolutePath) }.getOrNull()
        val totalBytes = stat?.totalBytes ?: 0L
        val freeBytes = stat?.availableBytes ?: 0L
        return DeviceStorageStats(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            appDownloadsBytes = appBytes
        )
    }
}
