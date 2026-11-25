# Dokuha — Architecture & Engineering Specification

This document provides a technical deep-dive into the architectural patterns, security model, rendering engine, and test infrastructure powering **Dokuha (読破)**.

---

## 1. System Overview

Dokuha is architected as a decoupled, mobile-only multiplatform application built on **Compose Multiplatform (CMP)** and **Kotlin Multiplatform (KMP)** targeting **Android** (API 24+ minSdk, API 35 compile/targetSdk) and **iOS** (iOS 15+ via `iosX64`, `iosArm64`, `iosSimulatorArm64`). There is no Desktop target.

```
UI UX/composeApp/src/
├── commonMain/kotlin/
│   ├── core/
│   │   ├── di/                 # Dependency injection modules (Koin Multiplatform)
│   │   ├── security/           # SecureStorage & KeyValueStorage contracts
│   │   └── source/             # Domain models (SourceManga, SourceChapter, SourcePage, NovelContent)
│   ├── data/
│   │   ├── extension/          # ExtensionRepoValidator & SHA-256 fingerprint verification
│   │   ├── mock/               # PanelRepository interface & mock implementation
│   │   ├── profile/            # PersistentProfileRepository & Tracker account persistence
│   │   ├── settings/           # PersistentSettingsRepository & reactive user preferences
│   │   └── tracker/            # TrackerRepository contract (AniList, MyAnimeList, Kitsu)
│   ├── io/panel/reader/        # App entry point, Voyager Navigator & transition bindings
│   └── ui/
│       ├── glass/              # Clear-Lens Glass Engine (specular borders, dimming, spring press physics)
│       ├── motion/             # Canonical motion specs & spring animations
│       ├── theme/              # Color palettes, typography, spacing, and shapes
│       ├── components/         # GlassContainer, SearchBar, BottomNavBar, PillButtons, Sliders
│       └── screens/
│           ├── welcome/        # WelcomeScreen (Onboarding hero)
│           ├── library/        # LibraryScreen (Category filters & manga grid)
│           ├── details/        # TitleDetailsScreen (Hero glow & chapter list)
│           ├── webtoonreader/  # WebtoonReaderScreen, ReaderTapZones, MangaPageCanvas
│           ├── novelreader/    # NovelReaderScreen (Editorial webnovel engine)
│           ├── browse/         # BrowseScreen, BrowseFilterSheet, SourceDetailScreen
│           ├── extensions/     # ExtensionsScreen (Source repository management)
│           ├── updates/        # UpdatesScreen (Chronological feed)
│           ├── downloads/      # DownloadsScreen (Horizontal storage bar & queue)
│           ├── profile/        # ProfileScreen (Stats & tracker accounts)
│           ├── more/           # MoreScreen (Hub navigation)
│           └── settings/       # Appearance & Reader settings screens
├── androidMain/kotlin/         # Android KeyStore, QuickJS/WebView, TextToSpeech & platform specs
├── iosMain/kotlin/             # Apple Keychain, WKWebView, AVSpeechSynthesizer & iOS lifecycle
└── commonTest/kotlin/          # Shared & Android unit/screenshot test suites (195 passing tests across 32 suites)
    ├── data/                   # Storage, Settings, Profile, Tracker, Validator tests
    ├── ui/browse/              # BrowseSearchEngineTest
    ├── ui/motion/              # PanelMotionTest
    └── ui/reader/              # ReaderEngineTest
```

---

## 2. Rendering & The `ui/glass` Engine

Rather than relying on third-party blur libraries with unpredictable GPU overhead, Dokuha employs a proprietary clear-lens engine located in `ui/glass/`:

- **Sampling Architecture (`glassSource`)**:
  Marks the primary scrollable content layer (artwork, cards, text) to provide coordinate references for overlying chrome elements.
- **Physical Specular Surface (`glassSurface`)**:
  Applies directional gradient borders and specular highlights that react to touch. Simulates the refractive index of treated optical glass without heavy blur shaders.
- **Atmospheric Dimming (`glassDimming`)**:
  Provides a proportional, variable-alpha dark mask beneath the glass layer to ensure high-contrast legibility over dynamic, colorful artwork.
- **Touch Dynamics (`glassPress` & `nonGlassPress`)**:
  Physics-based spring press response (`dampingRatio = 0.75f`, `stiffness = Spring.StiffnessMediumLow`) ensuring natural tactile feedback on all interactive elements.

---

## 3. Reader Engines

### Multi-Mode Comic & Manga Engine (`WebtoonReaderScreen`)
- **Webtoon**: Infinite continuous vertical column with zero-gap panel stitching via `LazyColumn`.
- **RTL Manga**: True Japanese right-to-left paginated navigation via `HorizontalPager(reverseLayout = true)`.
- **LTR Comic**: Left-to-right paginated paging with snap physics.
- **Vertical Paging**: Discrete vertical page snaps via `VerticalPager`.
- **3-Zone Tap Resolver (`ReaderTapZones.kt`)**:
  - `0.00..0.25`: Previous page / scroll up (inverts automatically in RTL mode).
  - `0.25..0.75`: Controls HUD toggle.
  - `0.75..1.00`: Next page / scroll down (inverts automatically in RTL mode).
- **In-App Brightness Dimmer**: Hardware-independent dark overlay adjustable from `0.10f` to `1.00f`.
- **Dynamic Side Padding**: Viewport margin adjustable from `0%` to `40%` for comfortable reading on tablets and foldables.

### Editorial Novel Engine (`NovelReaderScreen`)
- Curated reading palettes: Dark Obsidian, Warm Sepia, Light Cream, OLED Black.
- Typography engines: System Default, Classic Serif, Modern Sans-Serif, Technical Monospace.
- Dynamic point sizing and proportional line-height calculations.

---

## 4. Hardware Security & Persistence Model

Dokuha enforces zero-plaintext storage of user credentials and strict extension sandboxing:

### Storage Architecture
- **`SecureStorage` (Hardware-Backed)**:
  - **Android**: AES-256-GCM encryption via `EncryptedSharedPreferences` backed by the Android KeyStore.
  - **iOS**: Apple Keychain services with `kSecAttrAccessibleAfterFirstUnlock`.
  - Used for tracker OAuth tokens and sensitive user data.
- **`KeyValueStorage`**:
  - Light multiplatform key-value layer for non-sensitive UI preferences (theme, reading mode, typography).

### Extension Security (`ExtensionRepoValidator`)
- Enforces strict HTTPS transport for all external source repositories.
- Blocks localhost, private IP ranges (RFC 1918), and known malicious domains.
- Validates repository fingerprint against pre-computed SHA-256 hashes to prevent man-in-the-middle repository poisoning.

---

## 5. Navigation & Motion System

Navigation is powered by **Voyager** with accessibility-first motion curves:

```kotlin
PanelTheme(
    themeMode = themeMode,
    accentColor = accentColor,
    reduceGlass = reduceGlass,
    reduceMotion = reduceMotion
) {
    Navigator(WelcomeScreen()) { navigator ->
        if (reduceMotion) {
            FadeTransition(navigator)
        } else {
            SlideTransition(navigator)
        }
    }
}
```

- When `reduceMotion = false`: Smooth horizontal slide transitions with natural damping.
- When `reduceMotion = true`: Clean crossfades that avoid disorienting vestibular triggers.

---

## 6. Automated Verification Matrix

Every pull request is verified via `./gradlew :composeApp:testDebugUnitTest` (currently **195 passing tests across 32 test suites**, covering extension runtime, anti-bot heuristics, SQLDelight persistence, storage/settings/tracker repositories, reader & webnovel TTS utilities, splash concepts, and 23 Roborazzi screenshot captures).

Key suites include:

| Suite | Scope | Tests | Pass Rate |
|:---|:---|:---:|:---:|
| **`BrowseSearchEngineTest`** | Partial matching, multi-genre filtering, status segregation, A-Z sorting, pagination | 10 | 100% |
| **`ReaderEngineTest`** | 3-zone tap calculations, RTL layout inversion, mode bounds, progress scrubbers | 9 | 100% |
| **`JsBridgeTest` & `JsEngineTest`** | QuickJS sandbox evaluation, host marshaling, cookies & anti-bot retry | 25 | 100% |
| **`CloudflareChallengeHeuristicsTest` & `AntiBotChallengeManagerTest`** | SSRF URL guards, domain cookie matching, iOS UA compatibility, challenge coalescing | 11 | 100% |
| **`ExtensionRepoValidatorTest` & `ExtensionRepositoryTest`** | HTTPS verification, URL sanitization, SHA-256 fingerprint & persistence | 19 | 100% |
| **`PersistentStorageRepositoriesTest` & `SettingsProfileRepositoriesTest`** | Hardware secure storage, reactive preferences & profile persistence | 12 | 100% |
| **`NovelTextSanitizerTest`, `AppLogoAndSplashTest`, `LiquidGlassLogoTest`** | TTS HTML/Markdown cleaning, splash motion concepts & rotating formats | 14 | 100% |
| **`ScreenshotsTest`** | Native Roborazzi visual regression tests across 23 captures | 23 | 100% |
| **All 32 Suites Total** | Complete Android & `commonTest` verification (`0 skipped`, `0 failed`) | **195** | **100%** |

---

## 7. Build & Compilation Guide

### Requirements
- JDK 17 or higher
- Android SDK (API 35 compile target, minSdk 24)
- macOS with Xcode 15+ (required only for iOS builds)
- JVM memory settings in `UI UX/gradle.properties`: `org.gradle.jvmargs=-Xmx2048M`, `kotlin.daemon.jvmargs=-Xmx1024M`

### Common Commands

```bash
# Verify shared multiplatform code compilation
./gradlew :composeApp:compileCommonMainKotlinMetadata

# Run the complete test suite
./gradlew :composeApp:testDebugUnitTest

# Assemble Android Debug APK
./gradlew :composeApp:assembleDebug

# Record / Update Roborazzi reference screenshots
./gradlew :composeApp:recordRoborazziDebug
```
