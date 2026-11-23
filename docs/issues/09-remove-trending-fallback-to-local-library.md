# [Browse] Remove deceptive trending fallback to local library in SqlDelightPanelRepository.getTrending()

- **Labels**: `backend`, `tech-debt`, `priority:low`
- **Blocked by**: None

### Summary
In `SqlDelightPanelRepository.getTrending()`, if no sources are installed or if sources fail to return trending titles, the repository falls back to querying the user's local library and emitting the first 3 items (`database.mangaQueries.getLibrary().executeAsList().take(3)`). This deceptively disguises source failure and misleads users into thinking their own library manga are the current global trending titles.

### Evidence
- **File**: `UI UX/composeApp/src/commonMain/kotlin/data/repository/SqlDelightPanelRepository.kt#L385-L393`
```kotlin
                val local = database.mangaQueries.getLibrary().executeAsList().take(3).map { it.toSourceManga() }
                emit(local)
            }
        }
        return database.mangaQueries.getLibrary()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.take(3).map { it.toSourceManga() } }
    }
```

### Impact
When a user launches the app without extensions installed or without an active internet connection, their library items appear in the "Trending" section of the Browse tab.

### Expected Behavior
`getTrending()` should return an empty list or throw a typed error when no dynamic sources are registered or available, allowing the UI to show an honest empty state ("No sources installed" or "Install extensions to view trending titles").

### Suggested Approach
1. In `SqlDelightPanelRepository.getTrending()`, remove the fallback to `database.mangaQueries.getLibrary()`.
2. Emit `emptyList()` when `_dynamicSources.value` is empty or when source queries return no results.
3. In `BrowseScreen.kt`, handle empty trending lists by rendering an empty state CTA encouraging extension installation.

### Acceptance Criteria
- [ ] No library titles are emitted as "trending" manga when extensions are absent.
- [ ] Browse screen shows an empty state when no trending titles are returned.

- **Effort**: S
- **Related findings**: BK-TRD
