# [iOS] JsEngine stub never invokes Kotlin host callbacks

- **Labels**: `backend`, `ios`, `bug`, `priority:critical`
- **Blocked by**: None

### Summary
The iOS implementation of `JsEngine` registers global JavaScript functions (`setGlobalFunction` and `setGlobalAsyncFunction`) as dummy stubs that immediately return `null` or `Promise.resolve(null)` without forwarding calls to the Kotlin host callbacks. Consequently, all dynamic JavaScript extensions (network requests, HTML Cheerio DOM queries, cryptographic hashing) fail silently or throw exceptions on iOS.

### Evidence
- **File**: `UI UX/composeApp/src/iosMain/kotlin/core/extensions/js/JsEngine.ios.kt#L32-L60`
```kotlin
actual fun setGlobalFunction(name: String, fn: (List<Any?>) -> Any?) {
    val escapedName = name.replace("'", "\\'")
    val js = "globalThis['$escapedName'] = function(...args) { return null; };"
    context.evaluateScript(js)
}

actual fun setGlobalAsyncFunction(name: String, fn: suspend (List<Any?>) -> Any?) {
    val escapedName = name.replace("'", "\\'")
    val js = "globalThis['$escapedName'] = function(...args) { return Promise.resolve(null); };"
    context.evaluateScript(js)
}
```

### Impact
Every dynamic extension loaded on iOS is completely non-functional. Users cannot search for titles, fetch popular/trending manga, load chapter lists, or parse reader pages when running on iOS devices.

### Expected Behavior
`setGlobalFunction` and `setGlobalAsyncFunction` should bridge JavaScript calls from JavaScriptCore to Kotlin host functions (e.g., via `JSValue` block callbacks in Objective-C / Swift or QuickJS C bindings), returning the deserialized Kotlin results back into the JavaScript context.

### Suggested Approach
1. Replace or extend the iOS `JSContext` binding using Objective-C block bridging (`@convention(block)` via Kotlin/Native cinterop) to capture arguments passed into `globalThis[name]`.
2. Convert arguments between `JSValue` and Kotlin types (`Map`, `List`, `String`, `Number`, `Boolean`).
3. For asynchronous functions, return a `JSValue` Promise and resolve/reject it when the Kotlin coroutine completes.
4. Alternatively, compile and link QuickJS native C library for iOS via Kotlin Multiplatform cinterop, mirroring the Android engine behavior.

### Acceptance Criteria
- [ ] `__native_http_request` invoked from JavaScript returns the HTTP response body and headers to JavaScript.
- [ ] `__native_crypto_sha256` correctly computes hashes on iOS.
- [ ] Extension `getPopular(1)` returns parsed titles on an iOS simulator and physical device.

- **Effort**: L
- **Related findings**: BRK-01, BRK-02, SEC-02
