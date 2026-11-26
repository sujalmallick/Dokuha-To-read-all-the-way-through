package core.storage

data class DeviceStorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val appDownloadsBytes: Long
)

/**
 * Queries real device disk capacity and Dokuha's download directory footprint.
 */
expect class StorageProvider {
    fun getStorageStats(): DeviceStorageStats
}
