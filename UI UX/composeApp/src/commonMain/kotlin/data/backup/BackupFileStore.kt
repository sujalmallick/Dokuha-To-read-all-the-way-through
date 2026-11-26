package data.backup

/**
 * Persists and reads Dokuha backup files on local device storage.
 */
expect object BackupFileStore {
    fun writeBackup(fileName: String, content: String): Result<String>
    fun readBackup(fileName: String): Result<String>
    fun listBackups(): List<String>
}
