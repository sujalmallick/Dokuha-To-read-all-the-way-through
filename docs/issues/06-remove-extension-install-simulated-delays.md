# [Extensions] Remove artificial simulation delays from DefaultExtensionRepository

- **Labels**: `backend`, `tech-debt`, `priority:medium`
- **Blocked by**: None

### Summary
`DefaultExtensionRepository.installExtension` and `updateExtension` contain artificial `delay()` calls (`delay(120)`, `delay(150)`, `delay(100)`) simulating installation progress rather than executing real asynchronous download and verification tasks through `ExtensionManager`.

### Evidence
- **File**: `UI UX/composeApp/src/commonMain/kotlin/data/extension/ExtensionRepository.kt#L81-L103`
```kotlin
scope.launch {
    // Simulated smooth progress increments
    delay(120)
    _extensions.update { list ->
        list.map { if (it.id == extensionId) it.copy(installProgress = 0.5f) else it }
    }
    delay(150)
    _extensions.update { list ->
        list.map { if (it.id == extensionId) it.copy(installProgress = 0.9f) else it }
    }
    delay(100)
    _extensions.update { list ->
        list.map {
            if (it.id == extensionId) {
                it.copy(status = ExtensionStatus.INSTALLED, installProgress = 1.0f, updateVersion = null)
            } else it
        }
    }
}
```

### Impact
Extension installation and updates display artificial simulated progress bars instead of tracking real download bytes, checksum verification, and script compilation.

### Expected Behavior
`installExtension` and `updateExtension` should invoke `ExtensionManager.installExtension()`, streaming real download bytes and reporting real error states (e.g. network failure, signature verification failure).

### Suggested Approach
1. Remove all artificial `delay()` calls.
2. Delegate installation and update workflows directly to `ExtensionManager`.
3. Emit real progress states or indeterminate progress while downloading and verifying bundles.

### Acceptance Criteria
- [ ] No artificial `delay()` calls exist in `DefaultExtensionRepository`.
- [ ] Extension status accurately reflects real bundle download, verification, and registration.

- **Effort**: S
- **Related findings**: FKE-07, BK-EXT
