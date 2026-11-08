# Panel — UI / UX (Compose Multiplatform)

Cross-platform manga, manhwa, and webnovel reader user interface built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)** for Android and iOS.

Target UI design: `design/option_2.png`

---

## Tech Stack

- **Kotlin**: 2.1.0
- **Compose Multiplatform**: 1.7.1 (Material 3 base + custom PanelTheme)
- **Navigation**: Voyager 1.1.0-beta03
- **Dependency Injection**: Koin 4.0.0
- **Image Pipeline**: Coil 3 (3.0.4)
- **Glassmorphism / Blur**: Haze 1.1.1
- **Serialization**: `kotlinx.serialization` 1.7.3

---

## Package Architecture

```
composeApp/src/commonMain/kotlin/
├── core/
│   ├── di/
│   │   └── AppModule.kt             # Koin module definition
│   └── source/
│       └── Models.kt                # SourceManga, SourceChapter, SourcePage,
│                                    # NovelContent, MangaStatus, ContentSource...
├── data/
│   └── mock/
│       ├── MockModels.kt            # Mock models (Extensions, Downloads, Updates...)
│       └── MockRepositories.kt      # PanelRepository interface & FakePanelRepository
├── io/panel/reader/
│   └── PanelApp.kt                  # Root application composable & Navigator
└── ui/
    ├── theme/
    │   ├── Color.kt                 # Dark navy, electric blue accent, glass borders
    │   ├── Shape.kt                 # CapsuleShape, CoverImageShape, BottomSheetShape
    │   ├── Spacing.kt               # Spacing design tokens
    │   ├── Theme.kt                 # PanelTheme composable
    │   └── Type.kt                  # Typography scale
    ├── components/
    │   ├── BottomNavBar.kt          # Floating glass bottom bar (Library, Updates, Browse, More)
    │   ├── Buttons.kt               # PanelPillButton, PanelCircleIconButton
    │   ├── CommonComponents.kt      # MangaCoverCard, StatusBadge, RatingBadge, Sliders...
    │   └── GlassComponents.kt       # GlassContainer, FrostedGlassCapsule (with Haze blur)
    └── screens/
        ├── welcome/                 # Screen 1: Welcome Screen
        ├── library/                 # Screen 2: Library Screen
        ├── details/                 # Screen 3: Title Details Screen
        ├── webtoonreader/           # Screen 4: Webtoon Reader Screen
        │                            # Screen 5: Reader Settings Sheet
        ├── novelreader/             # Screen 6: Webnovel Reader Screen
        ├── browse/                  # Screen 7: Browse Screen
        ├── extensions/              # Screen 8: Extensions Screen
        ├── updates/                 # Screen 9: Updates Screen
        └── downloads/               # Screen 10: Downloads Screen
```

---

## Handover to Backend

All screens consume data through the **`PanelRepository`** interface defined in `data/mock/MockRepositories.kt`.
To connect real backend engines (SQLDelight, Ktor, Dalvik/DEX extension loader, Cloudflare bypass), implement `PanelRepository` or provide domain use-cases that replace `FakePanelRepository` in `core/di/AppModule.kt`.

---

## Building & Running

### Common Compilation Check
```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

### Android Run
```bash
./gradlew :composeApp:assembleDebug
```
