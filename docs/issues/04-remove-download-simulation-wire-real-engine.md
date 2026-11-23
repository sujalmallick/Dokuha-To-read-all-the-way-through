# [Downloads] Remove DefaultDownloadManager simulator and wire real core.download.DownloadManager to UI and DI

- **Labels**: `backend`, `bug`, `tech-debt`, `priority:critical`
- **Blocked by**: None

### Summary
The codebase contains two conflicting download managers: `data.download.DefaultDownloadManager` (a simulation ticker that fakes 1.2 MB progress per 500ms with hardcoded seed manga like *The Last Horizon*) and `core.download.DownloadManager` (the real file downloader backed by Ktor and `DownloadStorage`). `DownloadsScreen` and `AppModule` currently inject the simulation class, disconnecting the user interface from actual file downloads.

### Evidence
- **File**: `UI UX/composeApp/src/commonMain/kotlin/data/download/DownloadManager.kt#L31-L235`
```kotlin
class DefaultDownloadManager(
    autoStartSimulation: Boolean = true
) : DownloadManager {
    // ...
    private fun tickSimulation() {
        val currentJob = _queue.value.firstOrNull { it.status == DownloadStatus.DOWNLOADING } ?: return
        val currentManga = _queue.value.mapNotNull { it.manga }.firstOrNull { it.url == currentJob.mangaUrl }
        val speed = 2.8f // MB/s simulation speed
        val incrementBytes = (1.2 * 1024 * 1024).toLong()
        // ...
    }
}
```
- **File**: `UI UX/composeApp/src/commonMain/kotlin/core/di/AppModule.kt#L36-L45`
```kotlin
single<data.download.DownloadManager> { data.download.DefaultDownloadManager(autoStartSimulation = true) }
single<core.download.DownloadManager> {
    core.download.DownloadManager(
        downloadStorage = get(),
        httpClient = get(),
        database = get()
    )
}
```

### Impact
When a user enqueues a chapter download from Title Details or Webtoon Reader, the request goes to the core downloader while the Downloads screen displays fake simulated downloads. The user cannot monitor real downloads, pause/resume active downloads, or view downloaded chapters offline through the UI.

### Expected Behavior
1. Delete `data.download.DefaultDownloadManager` and its simulation loops.
2. Unify the download manager interface so there is only one `DownloadManager` class in `core.download`.
3. Provide single DI registration in `AppModule.kt` for `core.download.DownloadManager`.
4. `DownloadsScreen.kt` should observe real download jobs, progress bytes, and completion states from `core.download.DownloadManager`.

### Suggested Approach
1. Move any needed domain models from `data.download` into `core.download`.
2. Delete `DefaultDownloadManager` simulation logic and seed queues.
3. Expose StateFlows in `core.download.DownloadManager` for `queue`, `completedDownloads`, and download speed.
4. Update Koin module to inject `core.download.DownloadManager` across all screens.
5. In the UI, handle empty queue and completed downloads states with real values.

### Acceptance Criteria
- [ ] No simulated ticker or hardcoded seed titles exist in the codebase.
- [ ] Enqueuing a chapter download in Title Details appears immediately in the Downloads screen queue.
- [ ] Downloaded chapters are saved to disk and accessible offline in the reader.

- **Effort**: M
- **Related findings**: FKE-01, BK-STR
