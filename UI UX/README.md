<div align="center">

<img src="design/logo.png" alt="PANEL Logo (Dokuha)" width="128" />

# D O K U H A
### 読破 — *Dokuha: To Read All The Way Through*

**A modern, distraction-free mobile reader for Manga, Manhwa, Manhua, Comics, and Webnovels.**  
Crafted with Compose Multiplatform for fluid 60+ FPS performance exclusively on Android and iOS (desktop target removed).

[![Platform](https://img.shields.io/badge/Platform-Android_%7C_iOS-007AFF.svg)]()
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-1.7.1-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%7C%20Koin-E91E63.svg)](../docs/ARCHITECTURE.md)
[![Test Suite](https://img.shields.io/badge/Tests-195%20Passed-00C853.svg)](../docs/ARCHITECTURE.md#6-automated-verification-matrix)

---

<p align="center">
  <img src="design/option_2.png" alt="Dokuha Master Design Concept" width="100%">
</p>

</div>

---

## The Reading Experience

Dokuha is built from the ground up for mobile readers who demand speed, tactile elegance, and complete focus on the story. No desktop bloat, no banner ads, and no visual clutter.

### Liquid Glass Aesthetic & Multi-Concept Splash
A custom translucent chrome engine powered by AGSL `RuntimeShader` (Android API 33+) and directional specular bevel/sheen Canvas rendering (Android `< 33` & iOS). Includes 6 animated splash concepts paired with rotating format badges (`MANHWA` → `MANHUA` → `MANGA` → `COMIC` → `WEBNOVEL`).

### Quad-Mode Reading Engine & Hands-Free Autoscroll
Read any publication in its native format:
- **Webtoon Strip**: Continuous vertical scrolling with seamless inline next-chapter transition gaps, end-of-series cards, and a floating autoscroll pill (`1x` / `2x` / `3x`) with touch-to-pause.
- **RTL Manga**: Authentic right-to-left page turning with instant gesture flip.
- **LTR Comic**: Traditional left-to-right paging with snappy page transitions.
- **Vertical Paging**: Discrete, snapping page-by-page vertical reading.

### Editorial Webnovel Studio & Background TTS
Long-form prose features illuminated drop-caps, customizable typefaces (System, Classic Serif, Modern Sans, Monospace), adjustable line-height, four color palettes (Dark Obsidian, Warm Sepia, Light Cream, Pitch OLED Black), and native background Text-to-Speech (`TextToSpeech` on Android, `AVSpeechSynthesizer` on iOS) with HTML/Markdown sanitization and live paragraph tracking.

### Comic ↔ Novel Adaptation Bridge & Smart Catch-up
Jump between a title's comic/manhwa adaptation and its counterpart webnovel from the details screen, or review spoiler-free **"Previously On..."** recaps up to your last read chapter.

### Universal Discovery, Sandboxed Extensions & Anti-Bot Solver
- **Instant Search & Filtering**: Debounced title/author/genre search and multi-parameter catalog filtering.
- **Unified QuickJS Extension Runtime**: Runs Mangayomi, LNReader, and Paperback extension adapters via `quickjs-kt` across Android and iOS with SHA-256 repo validation and SSRF guards.
- **Two-Stage Cloudflare Resolution**: Silent headless `WebView` (Android) / off-screen `WKWebView` (iOS) clearance plus an interactive `ChallengeWebView` fallback.
- **Persistent Diagnostics**: Rolling multiplatform log persistence (`Okio`) with an in-app Log Viewer (`LogViewerScreen`).

### Privacy by Architecture & Build Memory
- **Hardware-Backed Encryption**: Tracker tokens and credentials use Android KeyStore and Apple Keychain.
- **Direct Cloud Sync**: Connect directly to AniList, MyAnimeList, and Kitsu with zero intermediary servers.
- **Memory-Tuned Builds**: `gradle.properties` sets `-Xmx2048M` (Gradle) and `-Xmx1024M` (Kotlin daemon) for stable local and CI mobile builds.

---

## Gallery

*Native captures rendered with 100% shared Compose Multiplatform code:*

<div align="center">

### Onboarding & Library

<table>
  <tr>
    <td align="center" width="33%">
      <img src="composeApp/screenshots/01_welcome.png" alt="Welcome Screen" width="100%" /><br />
      <b>Welcome & Immersion</b><br />
      Starry sky canvas and luxury entry
    </td>
    <td align="center" width="33%">
      <img src="composeApp/screenshots/02_library.png" alt="Library Screen" width="100%" /><br />
      <b>Dynamic Library</b><br />
      Quick-resume glass card and manga grid
    </td>
    <td align="center" width="33%">
      <img src="composeApp/screenshots/03_title_details.png" alt="Title Details Screen" width="100%" /><br />
      <b>Title Details Hub</b><br />
      Cinematic header, synopsis, and chapter list
    </td>
  </tr>
</table>

### Multi-Format Reader

<table>
  <tr>
    <td align="center" width="33%">
      <img src="composeApp/screenshots/04_webtoon_reader.png" alt="Webtoon & Manga Reader" width="100%" /><br />
      <b>Webtoon & Manga Reader</b><br />
      Multi-mode strip, RTL paging, and floating HUD
    </td>
    <td align="center" width="33%">
      <img src="composeApp/screenshots/05_reader_settings.png" alt="Reader Settings" width="100%" /><br />
      <b>Reader Settings Sheet</b><br />
      Mode selectors, dimmer, and margin sliders
    </td>
    <td align="center" width="33%">
      <img src="composeApp/screenshots/06_webnovel_reader.png" alt="Editorial Webnovel Reader" width="100%" /><br />
      <b>Editorial Webnovel Reader</b><br />
      Illuminated drop-caps and reading palettes
    </td>
  </tr>
</table>

### Discovery, Updates & Storage

<table>
  <tr>
    <td align="center" width="25%">
      <img src="composeApp/screenshots/07_browse.png" alt="Browse Hub" width="100%" /><br />
      <b>Browse Hub</b><br />
      Live search and filter bottom sheet
    </td>
    <td align="center" width="25%">
      <img src="composeApp/screenshots/08_extensions.png" alt="Extensions" width="100%" /><br />
      <b>Extensions</b><br />
      Verified repository management
    </td>
    <td align="center" width="25%">
      <img src="composeApp/screenshots/09_updates.png" alt="Daily Updates" width="100%" /><br />
      <b>Daily Updates</b><br />
      Chronological chapter timeline
    </td>
    <td align="center" width="25%">
      <img src="composeApp/screenshots/10_downloads.png" alt="Downloads" width="100%" /><br />
      <b>Storage & Queue</b><br />
      Horizontal capsule bar and queue
    </td>
  </tr>
</table>

### Profile & System Controls

<table>
  <tr>
    <td align="center" width="25%">
      <img src="composeApp/screenshots/11_profile.png" alt="Profile" width="100%" /><br />
      <b>Reader Profile</b><br />
      Reading statistics and linked trackers
    </td>
    <td align="center" width="25%">
      <img src="composeApp/screenshots/12_more_hub.png" alt="More Hub" width="100%" /><br />
      <b>More Hub</b><br />
      Profile card and preferences index
    </td>
    <td align="center" width="25%">
      <img src="composeApp/screenshots/13_appearance_settings.png" alt="Appearance Settings" width="100%" /><br />
      <b>Appearance</b><br />
      Themes, accent colors, and motion settings
    </td>
    <td align="center" width="25%">
      <img src="composeApp/screenshots/14_reader_settings.png" alt="Reader Settings" width="100%" /><br />
      <b>Reader Defaults</b><br />
      Default reading modes and live preview
    </td>
  </tr>
</table>

</div>

---

## Engineering & Architecture

For detailed technical specifications, project structure, the clear-lens rendering engine implementation, hardware security contracts, and build steps, see:

**[Dokuha Architecture & Engineering Specification](../docs/ARCHITECTURE.md)**

---

<div align="center">
  <sub>Crafted for readers around the world.</sub>
</div>
