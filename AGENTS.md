# Dokuha Workspace Rules

## Supported Targets
- Target platforms are **Android** (`androidTarget`) and **iOS** (`iosX64`, `iosArm64`, `iosSimulatorArm64`) only.
- Do **not** re-add a Desktop (`jvm("desktop")`, `desktopMain`, `desktopTest`, or `compose.desktop`) target or desktop dependencies.

## Gradle & JVM Memory Bounds
- Keep `UI UX/gradle.properties` capped at `org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8` and `kotlin.daemon.jvmargs=-Xmx1024M -XX:+UseG1GC` so builds stay reliable on both local machines and GitHub Actions macOS runners.
- Run Gradle commands from `UI UX/` (`./gradlew :composeApp:compileDebugKotlinAndroid`, `./gradlew :composeApp:testDebugUnitTest`).

## Security & Architecture
- Keep extension script execution sandboxed in `JsEngine` (QuickJS via `quickjs-kt` in `commonMain`).
- Keep Android (`ChallengeWebView.android.kt`, `HeadlessCloudflareSolver.android.kt`) and iOS (`ChallengeWebView.ios.kt`, `HeadlessCloudflareSolver.ios.kt`) anti-bot solvers scoped to public HTTP/HTTPS hosts and domain-matched cookies via `CloudflareChallengeHeuristics`.
