# [Extensions] Wire SqlDelightPanelRepository.getExtensions() to ExtensionManager.installedExtensions

- **Labels**: `backend`, `bug`, `tech-debt`, `priority:medium`
- **Blocked by**: None

### Summary
`SqlDelightPanelRepository.getExtensions()` is stubbed to return `flowOf(emptyList())`. Even when extensions are installed via `ExtensionManager` or sideloaded, any component relying on `PanelRepository.getExtensions()` receives an empty list.

### Evidence
- **File**: `UI UX/composeApp/src/commonMain/kotlin/data/repository/SqlDelightPanelRepository.kt#L417-L419`
```kotlin
override fun getExtensions(): Flow<List<MockExtension>> {
    return flowOf(emptyList())
}
```

### Impact
Screen components querying `PanelRepository` for installed extensions see 0 extensions, creating inconsistent state across the app.

### Expected Behavior
`SqlDelightPanelRepository.getExtensions()` should delegate to `ExtensionManager.installedExtensions` and map installed extensions into standard domain models.

### Suggested Approach
1. Inject `ExtensionManager` into `SqlDelightPanelRepository`.
2. Map `extensionManager.installedExtensions` to the appropriate domain model.
3. Replace `MockExtension` return type with a clean production domain model in `core.extensions.model`.

### Acceptance Criteria
- [ ] `SqlDelightPanelRepository.getExtensions()` emits all currently installed extensions.
- [ ] Installing or uninstalling an extension immediately updates the emitted flow.

- **Effort**: S
- **Related findings**: BK-EXT, FKE-07
