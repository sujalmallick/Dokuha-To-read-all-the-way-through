# [Library] Migrate CategoryRepository from in-memory state to SQLDelight database persistence

- **Labels**: `backend`, `bug`, `tech-debt`, `priority:high`
- **Blocked by**: None

### Summary
`DefaultCategoryRepository` currently stores all custom categories and manga-category mappings in in-memory `MutableStateFlow` instances initialized with hardcoded titles (`solo-leveling`, `aurora-blade`, `wind-beneath`). When the application restarts or is terminated by the OS, all user-created categories, renames, and category assignments are completely lost.

### Evidence
- **File**: `UI UX/composeApp/src/commonMain/kotlin/data/library/CategoryRepository.kt#L24-L51`
```kotlin
class DefaultCategoryRepository : CategoryRepository {
    private val initialCategories = listOf(
        LibraryCategory(DefaultCategories.ALL, "All", 0, isDefault = true),
        // ...
    )

    private val initialMappings = mapOf(
        "solo-leveling" to setOf(DefaultCategories.ALL, DefaultCategories.READING, "cat-favorites"),
        "aurora-blade" to setOf(DefaultCategories.ALL, DefaultCategories.READING),
        "wind-beneath" to setOf(DefaultCategories.ALL, DefaultCategories.READING),
        "shadow-crown" to setOf(DefaultCategories.ALL, DefaultCategories.PLAN_TO_READ),
        "neon-requiem" to setOf(DefaultCategories.ALL, DefaultCategories.COMPLETED),
        "frostbound" to setOf(DefaultCategories.ALL, DefaultCategories.ON_HOLD)
    )

    private val _categories = MutableStateFlow<List<LibraryCategory>>(initialCategories)
    private val _mangaCategoryMappings = MutableStateFlow<Map<String, Set<String>>>(initialMappings)
```

### Impact
Users cannot reliably organize their library into custom categories (e.g. "Favorites", "To Read", "Completed"). Any custom category created by the user disappears on app restart.

### Expected Behavior
`CategoryRepository` should read from and write to the SQLDelight database (`categories` and `manga_categories` tables in `PanelDatabase.sq`), reacting to database changes via SQLDelight Coroutines extensions (`asFlow().mapToList()`).

### Suggested Approach
1. Verify tables `categories` and `manga_categories` in `PanelDatabase.sq`.
2. Add queries for inserting, updating, deleting, and fetching categories with ordering.
3. Inject `PanelDatabase` into `DefaultCategoryRepository`.
4. Replace in-memory flows with SQLDelight reactive query flows.
5. Seed only standard default categories (e.g. "Default", "Reading", "Completed") on initial database creation if empty, without mock manga keys.

### Acceptance Criteria
- [ ] Creating a category persists across app kill and restart.
- [ ] Assigning manga to a category persists across app kill and restart.
- [ ] Deleting a category safely clears association rows in `manga_categories`.
- [ ] No hardcoded manga slugs exist in repository source code.

- **Effort**: M
- **Related findings**: FKE-06, FKE-05
