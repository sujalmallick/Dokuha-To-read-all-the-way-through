<div align="center">

<img src="UI UX/design/logo.png" alt="PANEL Logo (Dokuha)" width="128" />

# 🌌 PANEL (読破 — Dokuha)
### *Read Anywhere. To Read All The Way Through.*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.7.1-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/Platform-Android_%7C_iOS-brightgreen.svg)]()
[![Navigation](https://img.shields.io/badge/Navigation-Voyager-FF4081.svg)](https://voyager.adriel.cafe/)
[![Liquid Glass](https://img.shields.io/badge/Design_Engine-ui%2Fglass_Lens-00F5FF.svg)]()
[![DI](https://img.shields.io/badge/DI-Koin_4.0-E91E63.svg)](https://insert-koin.io/)
[![Unit Tests](https://img.shields.io/badge/Unit%20Tests-58%20Passed-00C853.svg)]()
[![Roborazzi](https://img.shields.io/badge/Verified-Roborazzi_Screenshots-00C853.svg)](https://github.com/takahirom/roborazzi)

<p align="center">
  <b>A next-generation, luxury-minimalist cross-platform reader for Manga, Manhwa, and Webnovels.</b><br>
  Built with Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP) for buttery 60+ FPS performance on Android & iOS.
</p>

---

<p align="center">
  <img src="UI UX/design/option_2.png" alt="Panel Master Design Concept" width="100%">
</p>

</div>

---

## ✨ Features & Architecture Highlights

### 1. 🌌 Proprietary Clear-Lens Glass Engine (`ui/glass`)
- Zero external blur dependencies. Custom GPU-accelerated refraction layers simulate physical glass thickness, chromatic boundary borders, and subtle liquid ambient glow.
- **`Modifier.glassSource(state)`**: Marks the underlying scrollable content layer for sampling.
- **`Modifier.glassSurface(...)`**: Applies physically styled clear/regular glass with responsive specular borders.
- **`Modifier.glassDimming(...)`**: Proportional backdrop dimming to guarantee readability over dynamic artwork.
- **`Modifier.glassPress(...)`**: Authentic physical spring press feedback matching Apple Human Interface Guidelines and Material 3 expressiveness.

### 2. 📖 Advanced Multi-Mode Reader Engine
- **Webtoon Mode**: Continuous zero-gap vertical scrolling strip via `LazyColumn` with floating episode scrubbers.
- **RTL Manga Mode**: Japanese right-to-left paginated reader using `HorizontalPager(reverseLayout = true)`.
- **LTR Comic Mode**: Western left-to-right paginated reader with snap paging.
- **Vertical Paging Mode**: Snapping vertical page transitions via `VerticalPager`.
- **3-Zone Smart Tap Gestures**:
  - Left 25%: Previous page / scroll up.
  - Center 50%: Toggle HUD glass overlay.
  - Right 25%: Next page / scroll down.
  - Direction automatically reverses when reading in RTL mode.
- **Live Viewport Customization**:
  - In-app brightness dimmer overlay (0.1..1.0) without altering system settings.
  - Side padding slider (0%..40%) for comfortable reading on tablets and foldables.
  - Panel border crop toggle.
- **Editorial Webnovel Reader**:
  - Curated colorways: Dark Obsidian, Warm Sepia, Light Cream, and Pitch OLED Black.
  - Typography options: System, Serif, Sans-Serif, and Monospace.
  - Dynamic font sizing and proportional line-height controls.
- **Transitions**: Seamless HUD chapter navigation and end-of-chapter glass summary cards.

### 3. 🔍 Advanced Browse & Multi-Source Search
- **Global Debounced Search**: Fast, case-insensitive search across titles, authors, and genres.
- **Liquid Glass Filter Bottom Sheet**:
  - **Sort Order**: Popularity, Latest, Alphabetical (A-Z).
  - **Publication Status**: All, Ongoing, Completed.
  - **Content Format**: All, Manga / Manhwa, Webnovels.
  - **Genres**: Multi-select glass chips with active glow borders and check indicators.
- **Active Filter Badges**: Dismissible filter chips with one-tap "Clear All".
- **Source Detail Screen**:
  - Dedicated catalog view for each source (MangaDex, Comick, Asura Scans).
  - Brand avatars, language tags (`EN`, `JP`, `KR`), in-source search, and paginated 3-column manga grid.

### 4. 🔒 Enterprise Security & Hardware Storage
- **Hardware-Backed Storage**:
  - Android: `EncryptedSharedPreferences` backed by Android KeyStore.
  - iOS: Native Apple Keychain services with `kSecAttrAccessibleAfterFirstUnlock`.
- **Tracker Integration**: `TrackerRepository` supporting AniList, MyAnimeList, and Kitsu with automated token wipe upon logout.
- **Extension Safety**: Repository validator enforcing HTTPS, domain whitelist checks, and SHA-256 fingerprint verification. Android-only extension management UI.

### 5. ♿ Accessibility & Motion System
- Dynamic Voyager transitions bound to the user's `reduceMotion` preference:
  - Standard mode: Fluid `SlideTransition`.
  - Accessible mode: Non-disorienting `FadeTransition`.

---

## 📱 Screen Showcase (Native Roborazzi Captures)

All screens rendered with 100% shared Compose Multiplatform code:

<div align="center">

### Onboarding & Library

<table>
  <tr>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/01_welcome.png" alt="01 Welcome Screen" width="100%" /><br />
      <b>01. Welcome & Immersion</b><br />
      <i>Starry night sky canvas & city silhouettes</i>
    </td>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/02_library.png" alt="02 Library Screen" width="100%" /><br />
      <b>02. Dynamic Glass Library</b><br />
      <i>Quick-resume glass card & manga grid</i>
    </td>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/03_title_details.png" alt="03 Title Details Screen" width="100%" /><br />
      <b>03. Title Details Hub</b><br />
      <i>Cinematic glow header & chapter feeds</i>
    </td>
  </tr>
</table>

### Multi-Format Reader Experience

<table>
  <tr>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/04_webtoon_reader.png" alt="04 Webtoon Reader" width="100%" /><br />
      <b>04. Webtoon & Manga Reader</b><br />
      <i>Multi-mode strip/paging & floating HUD</i>
    </td>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/05_reader_settings.png" alt="05 Reader Settings" width="100%" /><br />
      <b>05. Reader Settings Sheet</b><br />
      <i>Mode selector tiles & ambient sliders</i>
    </td>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/06_webnovel_reader.png" alt="06 Webnovel Reader" width="100%" /><br />
      <b>06. Editorial Webnovel Reader</b><br />
      <i>Illuminated drop-caps & palette bar</i>
    </td>
  </tr>
</table>

### Discovery, Search & Catalogues

<table>
  <tr>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/07_browse.png" alt="07 Browse Screen" width="100%" /><br />
      <b>07. Browse Hub & Search</b><br />
      <i>Live debounced search & filter sheet</i>
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/08_extensions.png" alt="08 Extensions Screen" width="100%" /><br />
      <b>08. Extensions & Sources</b><br />
      <i>Verified repo catalog & updater</i>
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/09_updates.png" alt="09 Updates Screen" width="100%" /><br />
      <b>09. Daily Updates</b><br />
      <i>Date-grouped chapter timeline</i>
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/10_downloads.png" alt="10 Downloads Screen" width="100%" /><br />
      <b>10. Storage & Queue</b><br />
      <i>Horizontal capsule bar & queue</i>
    </td>
  </tr>
</table>

### Profile & Settings Hub

<table>
  <tr>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/11_profile.png" alt="11 Profile Screen" width="100%" /><br />
      <b>11. Reader Profile</b><br />
      <i>Avatar, activity stats & trackers</i>
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/12_more_hub.png" alt="12 More Hub Screen" width="100%" /><br />
      <b>12. More Hub</b><br />
      <i>Profile summary & settings categories</i>
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/13_appearance_settings.png" alt="13 Appearance Settings" width="100%" /><br />
      <b>13. Appearance</b><br />
      <i>Themes, accent colors & accessibility</i>
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/14_reader_settings.png" alt="14 Reader Settings" width="100%" /><br />
      <b>14. Reader Defaults</b><br />
      <i>Default reading mode & typography</i>
    </td>
  </tr>
</table>

</div>

---

## 🏗 Architecture & Code Structure

Dokuha follows clean, modular architecture ready for multiplatform expansion:

```
UI UX/composeApp/src/
├── commonMain/kotlin/
│   ├── core/
│   │   ├── di/                 # Dependency injection modules (Koin)
│   │   ├── security/           # SecureStorage & KeyValueStorage contracts
│   │   └── source/             # Domain source models (Manga, Chapter, Page, NovelContent)
│   ├── data/
│   │   ├── extension/          # ExtensionRepoValidator & SHA-256 fingerprinting
│   │   ├── mock/               # MockRepositories (PanelRepository implementation)
│   │   ├── profile/            # PersistentProfileRepository & Tracker linking
│   │   ├── settings/           # PersistentSettingsRepository & user preferences
│   │   └── tracker/            # TrackerRepository (AniList, MyAnimeList, Kitsu)
│   ├── io/panel/reader/        # App entry point, Voyager Navigator & transition bindings
│   └── ui/
│       ├── glass/              # Clear-Lens Glass Engine (specular borders, dimming, press physics)
│       ├── motion/             # Canonical spring specs & easing curves
│       ├── theme/              # Color palettes, typography, spacing, and shapes
│       ├── components/         # GlassContainer, SearchBar, BottomNavBar, PillButtons, Sliders
│       └── screens/
│           ├── welcome/        # WelcomeScreen (Onboarding hero)
│           ├── library/        # LibraryScreen (Category filters & manga grid)
│           ├── details/        # TitleDetailsScreen (Hero glow & chapter list)
│           ├── webtoonreader/  # WebtoonReaderScreen, ReaderTapZones, MangaPageCanvas
│           ├── novelreader/    # NovelReaderScreen (Editorial novel engine)
│           ├── browse/         # BrowseScreen, BrowseFilterSheet, SourceDetailScreen
│           ├── extensions/     # ExtensionsScreen (Source repo management)
│           ├── updates/        # UpdatesScreen (Chronological feed)
│           ├── downloads/      # DownloadsScreen (Horizontal storage bar & queue)
│           ├── profile/        # ProfileScreen (Stats & tracker accounts)
│           ├── more/           # MoreScreen (Hub navigation)
│           └── settings/       # Appearance & Reader settings screens
├── androidMain/kotlin/         # Android KeyStore implementation & platform specs
├── iosMain/kotlin/             # Apple Keychain implementation & iOS lifecycle
└── commonTest/kotlin/          # Automated test suites (58 passing tests)
    ├── data/                   # PersistentStorage, Settings, Profile, Tracker, Validator tests
    ├── ui/browse/              # BrowseSearchEngineTest
    ├── ui/motion/              # PanelMotionTest
    └── ui/reader/              # ReaderEngineTest
```

---

## 🧪 Automated Test Suites

58 tests run on every build to guarantee rock-solid stability:

| Suite | Tests | Description |
|:---|:---:|:---|
| **`BrowseSearchEngineTest`** | 9 | Case-insensitive title search, genre multi-select, status filter, A-Z sort, pagination boundaries |
| **`ReaderEngineTest`** | 9 | 3-zone tap zone resolution, RTL action flip, mode bounds, progress calculations, settings clamping |
| **`ExtensionRepoValidatorTest`** | 7 | HTTPS enforcement, malicious URL rejection, SHA-256 fingerprint validation |
| **`PersistentStorageRepositoriesTest`** | 5 | Key-value & secure storage persistence across simulated app restarts |
| **`SettingsProfileRepositoriesTest`** | 7 | Reactive StateFlow updates for theme, reading mode, profile data |
| **`TrackerRepositoryTest`** | 3 | Token storage, OAuth credential lifecycle, secure wipe on logout |
| **`PanelMotionTest`** | 4 | Motion tokens, spring dynamics, reduceMotion disabling |
| **`ScreenshotsTest`** | 14 | Roborazzi screenshot verification across all 14 visual surfaces |
| **Total** | **58** | **100% Passed** |

---

## 🚀 Building & Testing

### Prerequisites
- JDK 17+
- Android Studio Ladybug+ / IntelliJ IDEA with Kotlin Multiplatform plugin
- macOS with Xcode 15+ (for iOS build)

### Run Unit Test Suite
```bash
cd "UI UX"
./gradlew :composeApp:testDebugUnitTest
```

### Verify Common Code Compilation
```bash
cd "UI UX"
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

### Assemble Android APK
```bash
cd "UI UX"
./gradlew :composeApp:assembleDebug
```

### Record / Verify Native Roborazzi Screenshots
```bash
cd "UI UX"
./gradlew :composeApp:recordRoborazziDebug
```

---

<div align="center">
  <sub>Crafted with passion for readers around the world.</sub>
</div>
