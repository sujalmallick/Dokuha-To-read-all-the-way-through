# Wire background & filtering settings end-to-end

## Problem
Several persisted `SettingsRepository` preferences currently have no runtime consumers and were disabled/hidden in the UI to avoid fake controls:
- `defaultCategory` (`LibrarySettingsScreen`)
- `updateOnAppLaunch` (`LibrarySettingsScreen`)
- `downloadOnlyOnWifi`, `deleteAfterReading`, `concurrentDownloads`, `downloadLocation` (`DownloadsSettingsScreen`)
- `languageFilters`, `showNsfw` (`SourcesSettingsScreen`)
- `ProfileStatsRow`, `ActivityBarChart`, `Library Overview`, and `Trackers` on `ProfileScreen` (currently backed by `0` / debug stubs)

## Dependencies
- `DownloadManager` queue/network/storage enforcement (issues #16–#25)
- Persistent `CategoryRepository` (#20) for `defaultCategory`
- `LibraryUpdateScheduler` / app launch hook for `updateOnAppLaunch`

## Acceptance Criteria
- Enforce `downloadOnlyOnWifi`, `concurrentDownloads`, `deleteAfterReading`, and `downloadLocation` inside `DownloadManager` and re-enable their controls in `DownloadsSettingsScreen`.
- Trigger `refreshLibraryUpdates()` on launch when `updateOnAppLaunch` is enabled and re-enable the toggle in `LibrarySettingsScreen`.
- Filter catalog/extension results by `languageFilters` and `showNsfw` and re-enable the toggles in `SourcesSettingsScreen`.
- Back `ProfileScreen` stats & trackers with SQLDelight aggregates and `ProductionTrackerRepository`.
