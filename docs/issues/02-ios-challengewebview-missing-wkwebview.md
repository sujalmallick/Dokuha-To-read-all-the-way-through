# [iOS] ChallengeWebView lacks WKWebView implementation and does not resolve Cloudflare challenges

- **Labels**: `backend`, `ios`, `security`, `bug`, `priority:critical`
- **Blocked by**: None

### Summary
The iOS implementation of `ChallengeWebView` is currently a placeholder Compose `Box` displaying static text `"iOS Verification ($url)"`. It does not instantiate `WKWebView`, cannot run Cloudflare Turnstile/challenge JavaScript, and never captures clearance cookies (`cf_clearance`). Furthermore, `HeadlessCloudflareSolver.ios.kt` is a hardcoded failure stub.

### Evidence
- **File**: `UI UX/composeApp/src/iosMain/kotlin/core/webview/ChallengeWebView.ios.kt#L22-L26`
```kotlin
actual fun ChallengeWebView(
    url: String,
    onSuccess: (List<Pair<String, String>>) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("iOS Verification ($url)")
    }
}
```
- **File**: `UI UX/composeApp/src/iosMain/kotlin/core/extensions/js/HeadlessCloudflareSolver.ios.kt#L12-L17`
```kotlin
actual class HeadlessCloudflareSolver {
    actual suspend fun solve(url: String, headers: Map<String, String>): HeadlessSolverResult {
        return HeadlessSolverResult(
            success = false,
            isInteractiveChallengeRequired = true,
            errorMessage = "Headless solver not supported on iOS"
        )
    }
}
```

### Impact
When an extension encounters a Cloudflare anti-bot challenge (e.g. MangaDex, Asura, FlameScans) on iOS, the user sees a blank screen with static text that cannot be completed. The user is permanently blocked from reading manga protected by Cloudflare.

### Expected Behavior
`ChallengeWebView` must embed a native `WKWebView` using `UIKitView`, configure a realistic desktop/mobile User-Agent, render the Turnstile challenge for user interaction, and upon navigation or cookie creation, extract `cf_clearance` and user-agent cookies and pass them to `onSuccess`.

### Suggested Approach
1. In `ChallengeWebView.ios.kt`, use `UIKitView` to instantiate and configure `WKWebView`.
2. Attach a `WKNavigationDelegate` to intercept redirect URLs and inspect `WKHTTPCookieStore`.
3. When `cf_clearance` cookie is detected, extract all cookies and call `onSuccess`.
4. In `HeadlessCloudflareSolver.ios.kt`, implement an off-screen `WKWebView` instance to attempt silent challenge solving before showing the interactive UI.

### Acceptance Criteria
- [ ] Navigating to a Cloudflare challenge URL presents an interactive `WKWebView` on iOS.
- [ ] Completing the Cloudflare challenge extracts `cf_clearance` and dismisses the webview.
- [ ] Subsequent Ktor extension network requests retain the acquired cookies.

- **Effort**: M
- **Related findings**: BRK-02, BRK-01
