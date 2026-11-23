# [Downloads] Replace hardcoded MockStorageInfo with real platform disk storage APIs

- **Labels**: `backend`, `android`, `ios`, `tech-debt`, `priority:medium`
- **Blocked by**: #19

### Summary
The storage stats displayed in the Downloads screen are backed by `MockStorageInfo`, which hardcodes fixed numbers: 14.8 GB used, 64 GB total, and 2.4 GB Dokuha app bytes. The app does not query real device storage or calculate the actual size of downloaded chapters on disk.

### Evidence
- **File**: `UI UX/composeApp/src/commonMain/kotlin/data/mock/MockRepositories.kt#L29-L33`
```kotlin
data class MockStorageInfo(
    val usedBytes: Long = 14_800_000_000L,
    val totalBytes: Long = 64_000_000_000L,
    val dokuhaBytes: Long = 2_400_000_000L
)
```
- **File**: `UI UX/composeApp/src/commonMain/kotlin/data/repository/SqlDelightPanelRepository.kt#L526-L528`
```kotlin
override fun getStorageInfo(): Flow<MockStorageInfo> {
    return downloadManager?.storageInfo ?: flowOf(MockStorageInfo())
}
```

### Impact
Users see completely fictitious device storage information in the Downloads screen, making it impossible to know how much storage downloaded manga chapters are actually consuming.

### Expected Behavior
A platform-specific disk storage reader should measure:
1. Total device storage space.
2. Available free space.
3. Total bytes consumed by the Dokuha download directory (chapters, covers, cache).

### Suggested Approach
1. Define an `expect class StorageProvider` in `commonMain`:
   ```kotlin
   data class StorageInfo(val totalBytes: Long, val freeBytes: Long, val appBytes: Long)
   expect class StorageProvider {
       suspend fun getStorageInfo(): StorageInfo
   }
   ```
2. Implement `actual class StorageProvider` on Android using `android.os.StatFs` and recursive file length calculations on the app's external files directory.
3. Implement `actual class StorageProvider` on iOS using `NSFileManager.defaultManager.attributesOfFileSystemForPath` and directory enumerator size summation.
4. Expose `storageInfo: StateFlow<StorageInfo>` in `core.download.DownloadManager`.

### Acceptance Criteria
- [ ] Hardcoded `MockStorageInfo` default numbers are removed.
- [ ] Real storage and app download folder sizes are reported accurately on Android and iOS.

- **Effort**: M
- **Related findings**: BK-STR, FKE-01
