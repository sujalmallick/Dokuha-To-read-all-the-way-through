# Re-enable batch and update chapter downloads after download-integrity fix

## Problem
The Library multi-select `BatchActionBar` Download button and the `Updates` tab per-chapter download buttons (`UpdateItemCard` and `ExpandableComicUpdatesCard`) are currently hidden with `// TODO` markers because `DownloadManager` still has the blank-page download-integrity bug.

## Dependencies
- `DownloadManager` blank-page integrity fix (#16–#25).

## Acceptance Criteria
- Re-enable `onDownloadUpdate` in `MainTabScreen` (`PanelTab.UPDATES`) and `UpdatesScreen` so `UpdateItemCard` and `ExpandableComicUpdatesCard` download buttons enqueue chapters via `DownloadManager`.
- Re-enable the `Download` action in `LibraryScreen`'s `BatchActionBar` to enqueue unread/all chapters for selected library titles.
- Add UI + integration tests verifying downloaded pages are non-empty and readable offline.
