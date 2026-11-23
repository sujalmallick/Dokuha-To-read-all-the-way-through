# [Updates] Updates screen lacks background sync and chapter update detection engine

- **Labels**: `backend`, `android`, `ios`, `tech-debt`, `priority:high`
- **Blocked by**: None

### Summary
The production repository method `SqlDelightPanelRepository.getUpdates()` is stubbed to return `flowOf(emptyList())`. There is no background worker or sync engine to check installed extension sources for newly published chapters of titles in the user's library.

### Evidence
- **File**: `UI UX/composeApp/src/commonMain/kotlin/data/repository/SqlDelightPanelRepository.kt#L421-L423`
```kotlin
override fun getUpdates(): Flow<List<MockUpdateGroup>> {
    return flowOf(emptyList())
}
```

### Impact
The "Updates" tab remains completely empty at all times. Users have no way to know when new chapters of their favorite manga or webnovels are released without manually clicking into each individual title.

### Expected Behavior
A dedicated update checking service should:
1. Query favorited manga in the library.
2. Fetch latest chapter lists from their respective sources (rate-limited and with backoff).
3. Compare against previously stored chapters in SQLDelight.
4. Record newly discovered chapters in an `updates` table.
5. Provide a Flow of grouped update items (e.g. "Today", "Yesterday", "Last Week") to the Updates screen.
6. Support manual pull-to-refresh and background scheduled execution (WorkManager on Android, BGAppRefreshTask on iOS).

### Suggested Approach
1. Add an `updates` table or flags on `chapters` (`is_new`, `date_discovered`) in `PanelDatabase.sq`.
2. Implement `UpdateCheckerService` in `data.repository` or `core.updates` that iterates through favorited titles and compares chapter counts / URLs.
3. Replace `getUpdates()` in `SqlDelightPanelRepository` with a SQLDelight query flow mapping new chapters to `UpdateItem` domain models.
4. On Android, register a periodic `CoroutineWorker` via AndroidX WorkManager. On iOS, configure `BGAppRefreshTask`.

### Acceptance Criteria
- [ ] Pull-to-refresh on Updates screen queries sources for favorited manga.
- [ ] New chapters not previously in the database are inserted and emitted by `getUpdates()`.
- [ ] Tapping an update item opens the reader or title details directly.

- **Effort**: L
- **Related findings**: BRK-03, FKE-01
