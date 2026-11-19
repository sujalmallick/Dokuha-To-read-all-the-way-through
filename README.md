<div align="center">

<img src="UI UX/design/logo.png" alt="PANEL Logo (Dokuha)" width="128" />

# PANEL
### 読破 — *Dokuha: To Read All The Way Through*

**A modern, distraction-free reader for Manga, Manhwa, and Webnovels.**  
Crafted with Compose Multiplatform for fluid 60+ FPS performance on Android and iOS.

[![Platform](https://img.shields.io/badge/Platform-Android_%7C_iOS-007AFF.svg)]()
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-1.7.1-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%7C%20Koin-E91E63.svg)](docs/ARCHITECTURE.md)
[![Test Suite](https://img.shields.io/badge/Tests-58%20Passed-00C853.svg)](docs/ARCHITECTURE.md#6-automated-verification-matrix)


## The Reading Experience

Dokuha is built from the ground up for readers who demand speed, tactile elegance, and complete focus on the story. No banner ads, no clunky web wrappers, and no visual clutter.

### Liquid Glass Aesthetic
A custom translucent chrome engine that feels weightless. Ambient specular borders and physical backdrop dimming ensure controls remain legible without obscuring dynamic artwork.

### Quad-Mode Reading Engine
Read any publication in its native format:
- **Webtoon Strip**: Continuous, zero-gap vertical scrolling with floating episode scrubbers.
- **RTL Manga**: Authentic right-to-left page turning with instant gesture flip.
- **LTR Comic**: Traditional left-to-right paging with snappy page transitions.
- **Vertical Paging**: Discrete, snapping page-by-page vertical reading.

### Editorial Webnovel Studio
Long-form prose demands thoughtful typography. Dokuha features illuminated drop-caps, customizable typefaces (System, Classic Serif, Modern Sans, Monospace), adjustable line-height, and four calibrated color palettes: Dark Obsidian, Warm Sepia, Light Cream, and Pitch OLED Black.

### Smart 3-Zone Gestures
Turn pages without looking. The display is divided into three intuitive touch regions for previous page, controls toggle, and next page—automatically reversing directions when switching between Western comics and Japanese manga.

### Ambient Viewport Control
- **In-App Dimmer**: Dim the reader surface below system brightness levels for comfortable late-night sessions.
- **Dynamic Margins**: Adjustable side padding (0% to 40%) optimizes panel proportions on large tablets and foldables.
- **Border Cropping**: Remove dead white or black margins with a single toggle.

### Universal Discovery & Search
Find your next read in seconds:
- **Instant Search**: Fast, debounced search querying across titles, authors, and genres simultaneously.
- **Multi-Parameter Filtering**: Narrow down thousands of titles by completion status, content format, sort order, and multi-selected genre tags.
- **Source Hub**: Browse modular catalogs with dedicated brand views and language filters.

### Privacy by Architecture
- **Hardware-Backed Encryption**: Tracker tokens and credentials are encrypted directly using Android KeyStore and Apple Keychain.
- **Direct Cloud Sync**: Connect directly to AniList, MyAnimeList, and Kitsu with zero intermediary proxy servers.
- **Zero Telemetry**: No analytics trackers, no user fingerprinting, and zero advertising SDKs.

---

## Gallery

*Native captures rendered with 100% shared Compose Multiplatform code:*

<div align="center">

### Onboarding & Library

<table>
  <tr>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/01_welcome.png" alt="Welcome Screen" width="100%" /><br />
      <b>Welcome & Immersion</b><br />
      Starry sky canvas and luxury entry
    </td>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/02_library.png" alt="Library Screen" width="100%" /><br />
      <b>Dynamic Library</b><br />
      Quick-resume glass card and manga grid
    </td>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/03_title_details.png" alt="Title Details Screen" width="100%" /><br />
      <b>Title Details Hub</b><br />
      Cinematic header, synopsis, and chapter list
    </td>
  </tr>
</table>

### Multi-Format Reader

<table>
  <tr>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/04_webtoon_reader.png" alt="Webtoon & Manga Reader" width="100%" /><br />
      <b>Webtoon & Manga Reader</b><br />
      Multi-mode strip, RTL paging, and floating HUD
    </td>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/05_reader_settings.png" alt="Reader Settings" width="100%" /><br />
      <b>Reader Settings Sheet</b><br />
      Mode selectors, dimmer, and margin sliders
    </td>
    <td align="center" width="33%">
      <img src="UI UX/composeApp/screenshots/06_webnovel_reader.png" alt="Editorial Webnovel Reader" width="100%" /><br />
      <b>Editorial Webnovel Reader</b><br />
      Illuminated drop-caps and reading palettes
    </td>
  </tr>
</table>

### Discovery, Updates & Storage

<table>
  <tr>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/07_browse.png" alt="Browse Hub" width="100%" /><br />
      <b>Browse Hub</b><br />
      Live search and filter bottom sheet
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/08_extensions.png" alt="Extensions" width="100%" /><br />
      <b>Extensions</b><br />
      Verified repository management
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/09_updates.png" alt="Daily Updates" width="100%" /><br />
      <b>Daily Updates</b><br />
      Chronological chapter timeline
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/10_downloads.png" alt="Downloads" width="100%" /><br />
      <b>Storage & Queue</b><br />
      Horizontal capsule bar and queue
    </td>
  </tr>
</table>

### Profile & System Controls

<table>
  <tr>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/11_profile.png" alt="Profile" width="100%" /><br />
      <b>Reader Profile</b><br />
      Reading statistics and linked trackers
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/12_more_hub.png" alt="More Hub" width="100%" /><br />
      <b>More Hub</b><br />
      Profile card and preferences index
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/13_appearance_settings.png" alt="Appearance Settings" width="100%" /><br />
      <b>Appearance</b><br />
      Themes, accent colors, and motion settings
    </td>
    <td align="center" width="25%">
      <img src="UI UX/composeApp/screenshots/14_reader_settings.png" alt="Reader Settings" width="100%" /><br />
      <b>Reader Defaults</b><br />
      Default reading modes and live preview
    </td>
  </tr>
</table>

</div>

---

## Engineering & Architecture

For detailed technical specifications, project structure, the clear-lens rendering engine implementation, hardware security contracts, and build steps, see:

**[Dokuha Architecture & Engineering Specification](docs/ARCHITECTURE.md)**

---

<div align="center">
  <sub>Crafted for readers around the world.</sub>
</div>
