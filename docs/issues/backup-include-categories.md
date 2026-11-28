# Include categories and manga-category mappings in Backup & Restore

## Problem
`BackupEngine.exportJson` and `BackupEngine.importJson` already support `categoryRepository` and `readerPrefsRepository`, but `SqlDelightPanelRepository.exportLibraryJson()` and `SqlDelightPanelRepository.importLibraryJson()` omit `categoryRepository` (leaving it `null`). As a result, user categories and manga-to-category assignments are silently lost on backup/wipe/restore.

## Dependency
- Depends on persistent `CategoryRepository` (#20) so categories and mappings survive app restarts and can be reliably exported/restored.

## Acceptance Criteria
- Inject `CategoryRepository` into `SqlDelightPanelRepository` (or pass it in `appModule`).
- Forward `categoryRepository` to `BackupEngine.exportJson` and `BackupEngine.importJson`.
- Add a round-trip backup/restore test verifying categories, sort order, and manga-category mappings survive wipe + restore alongside `reader_prefs`.
