# iOS Build Failures & Solutions Log

Living record of Kotlin Multiplatform (`iosArm64` / `iosSimulatorArm64`) and Xcode CI build failures encountered on **Build iOS IPA** (`macos-15`) and their exact fixes.

> **Why iOS errors appear on CI first**: Development runs on Windows, where Kotlin/Native cannot compile Apple SDK targets locally. Always review changes in `commonMain` and `iosMain` against the checklist at the bottom before merging.

---

## Failure History

| Date / Commit | Failing File(s) | Root Cause (Brief) | Solution | Fixed In |
|---|---|---|---|---|
| `c343f34` | `ui/components/LiquidGlassLogo.kt` | Used JVM-only `java.lang.Math.toRadians(...)` in `commonMain`, which does not exist on Kotlin/Native iOS. | Replaced with pure Kotlin `sweepAngle.toDouble() * (PI / 180.0)` (`kotlin.math.PI`). | PR #29 (`ad1d3a4`) |
| `ad1d3a4` | `core/extensions/js/JsEngine.ios.kt` | C-interop callback signature used `CPointer<JSValueRef?>` instead of `CPointer<JSValueRefVar>`. | Updated callback parameters and pointer dereferencing to `JSValueRefVar`. | PR #30 (`06a75c2`) |
| `14984e6` | `core/webview/ChallengeWebView.ios.kt` | 1) Colliding `WKNavigationDelegateProtocol` overrides (`webView(...)` sharing identical Kotlin parameter types).<br>2) Removed/deprecated `WKPreferences.javaScriptEnabled`. | 1) Added `@ObjCSignatureOverride` + `@Suppress("CONFLICTING_OVERLOADS")` to all 5 `webView` overrides.<br>2) Switched to `WKWebViewConfiguration.defaultWebpagePreferences = WKWebpagePreferences().apply { allowsContentJavaScript = true }`. | PR #33 (`f299d5a`) |
| `f299d5a` | `core/platform/PlatformTts.ios.kt` | 1) Colliding `AVSpeechSynthesizerDelegateProtocol` callbacks (`didStart`, `didFinish`, `didCancel`).<br>2) Missing `ExperimentalForeignApi` opt-in for `AVAudioSession.setCategory(..., error = null)`. | 1) Added `@ObjCSignatureOverride` + `@Suppress("CONFLICTING_OVERLOADS")` to all 3 callbacks.<br>2) Added `@OptIn(ExperimentalForeignApi::class)` on `PlatformTts`. | PR #34 (`30506f4`) |
| `4143736` | `core/logging/AppLogger.kt`<br>`ui/screens/settings/LogViewerScreen.kt`<br>`core/logging/PlatformLogger.ios.kt` | 1) `AppLogger.kt:103` omitted `import okio.use` (Android resolved `kotlin.io.use` via `java.io.Closeable`, iOS failed type inference).<br>2) `LogViewerScreen.kt:238` missed `import ui.components.GlassCollapsingTopBar`.<br>3) `PlatformLogger.ios.kt:18` called `NSFileManager.createDirectoryAtPath(..., error = null)` without `@OptIn(ExperimentalForeignApi::class)`. | 1) Added `import okio.use`.<br>2) Added `import ui.components.GlassCollapsingTopBar`.<br>3) Added `@OptIn(ExperimentalForeignApi::class)` on `PlatformLogger`. | `fix/ios-logging-compile-errors` |

---

## Pre-Merge iOS Checklist
1. **No JVM APIs in `commonMain`**: Never import `java.*` (use `kotlin.math.*`, `okio.*`, `kotlinx.*`).
2. **Explicit `okio.use`**: Always add `import okio.use` when calling `.use { ... }` on Okio sinks/sources in `commonMain`.
3. **Objective-C Delegate Overloads (`iosMain`)**: Annotate Objective-C protocol methods that share a base name and parameter types (like `WKNavigationDelegateProtocol`, `AVSpeechSynthesizerDelegateProtocol`) with `@ObjCSignatureOverride` (`kotlinx.cinterop.ObjCSignatureOverride`) and `@Suppress("CONFLICTING_OVERLOADS")`.
4. **C-Interop Opt-In (`iosMain`)**: Any Apple API taking `NSError**` (`error = null`), `CValue<CGRect>`, or `CPointer<*>` requires `@OptIn(ExperimentalForeignApi::class)`.
5. **Modern Apple APIs**: Prefer `WKWebpagePreferences.allowsContentJavaScript` over `WKPreferences.javaScriptEnabled`.
