package io.panel.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import core.download.DownloadJob
import core.download.DownloadManager
import core.download.DownloadStatus
import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import data.extension.DefaultExtensionRepository
import data.extension.ExtensionItem
import data.extension.ExtensionSourceInfo
import data.extension.ExtensionStatus
import data.extension.ExtensionTrustLevel
import data.library.DefaultCategoryRepository
import data.mock.FakePanelRepository
import data.mock.MockStorageInfo
import data.profile.DayReadingActivity
import data.profile.LibrarySummary
import data.profile.ReadingStats
import data.profile.TrackerAccount
import data.profile.TrackerService
import data.profile.UserProfile
import data.settings.AppLogoStyle
import data.settings.DefaultReadingMode
import data.settings.FakeReaderPrefsRepository
import data.settings.FakeSettingsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.ReaderColorFilter
import data.settings.TitleReaderPrefs
import data.settings.canonicalTitleKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.screens.browse.BrowseScreenContent
import ui.screens.details.BatchDownloadSheet
import ui.screens.details.TitleDetailsScreenContent
import ui.screens.downloads.DownloadsScreenContent
import ui.screens.extensions.AddRepoDialog
import ui.screens.extensions.ExtensionDetailSheet
import ui.screens.extensions.ExtensionsScreenContent
import ui.screens.extensions.SideloadExtensionDialog
import ui.screens.library.BatchMoveSheet
import ui.screens.library.CategoryEditSheet
import ui.screens.library.LibraryScreenContent
import ui.screens.more.MoreScreenContent
import ui.screens.novelreader.NovelReaderScreenContent
import ui.screens.novelreader.NovelSettingsSheetContent
import ui.screens.profile.ProfileScreenContent
import ui.screens.settings.AppLogoSettingsScreenContent
import ui.screens.settings.AppearanceSettingsScreenContent
import ui.screens.settings.GlobalReaderSettingsScreenContent
import ui.screens.splash.SplashScreenContent
import ui.screens.updates.UpdatesScreenContent
import ui.screens.webtoonreader.ReaderSettingsScreenContent
import ui.screens.webtoonreader.WebtoonReaderScreenContent
import ui.screens.welcome.WelcomeScreenContent
import ui.theme.PanelTheme
import ui.theme.ThemeMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class ScreenshotsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository = FakePanelRepository()

    @Test
    fun capture_00_splash_screen() {
        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                SplashScreenContent(
                    onFinished = {},
                    logoStyle = AppLogoStyle.GLASSMORPHIC
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1300L)
        composeTestRule.onRoot().captureRoboImage("screenshots/00_splash_screen.png")
    }

    @Test
    fun capture_22_app_logo_settings() {
        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                AppLogoSettingsScreenContent(
                    selectedStyle = AppLogoStyle.GLASSMORPHIC,
                    onBack = {},
                    onSelectStyle = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/22_app_logo_settings.png")
    }

    @Test
    fun capture_01_welcome_screen() {
        composeTestRule.setContent {
            PanelTheme {
                WelcomeScreenContent(onGetStarted = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/01_welcome.png")
    }

    @Test
    fun capture_02_library_screen() {
        composeTestRule.setContent {
            PanelTheme {
                LibraryScreenContent(
                    repository = repository,
                    onOpenManga = {},
                    onResumeChapter = { _, _ -> },
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/02_library.png")
    }

    @Test
    fun capture_03_title_details_screen() {
        val chapters = runBlocking { repository.getChapters("").first() }
        val manga = runBlocking { repository.getMangaDetails("").first() }

        composeTestRule.setContent {
            PanelTheme {
                TitleDetailsScreenContent(
                    manga = manga,
                    chapters = chapters,
                    onBack = {},
                    onReadChapter = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/03_title_details.png")
    }

    @Test
    fun capture_04_webtoon_reader_screen() {
        val pages = runBlocking { repository.getPages("").first() }
        val mangaSettingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.RIGHT_TO_LEFT)
        }

        composeTestRule.setContent {
            PanelTheme {
                WebtoonReaderScreenContent(
                    chapterTitle = "Chapter 42: Awakening",
                    pages = pages,
                    settingsRepository = mangaSettingsRepo,
                    isOfflineCached = true,
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/04_webtoon_reader.png")
    }

    @Test
    fun capture_05_reader_settings_screen() {
        composeTestRule.setContent {
            PanelTheme {
                ReaderSettingsScreenContent(
                    onDismiss = {},
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/05_reader_settings.png")
    }

    @Test
    fun capture_06_webnovel_reader_screen() {
        val novelContent = runBlocking { repository.getNovelContent("").first() }

        composeTestRule.setContent {
            PanelTheme {
                NovelReaderScreenContent(
                    chapterNumber = "Chapter 12",
                    chapterTitle = "The Wind Rises",
                    novelContent = novelContent,
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/06_webnovel_reader.png")
    }

    @Test
    fun capture_07_browse_screen() {
        val sources = runBlocking { repository.getSources().first() }
        val trending = runBlocking { repository.getTrending().first() }

        composeTestRule.setContent {
            PanelTheme {
                BrowseScreenContent(
                    sources = sources,
                    trending = trending,
                    onOpenSource = {},
                    onSeeAllSources = {},
                    onOpenManga = {},
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/07_browse.png")
    }

    @Test
    fun capture_08_extensions_screen() {
        composeTestRule.setContent {
            PanelTheme {
                ExtensionsScreenContent(
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/08_extensions.png")
    }

    @Test
    fun capture_09_updates_screen() {
        val updateGroups = runBlocking { repository.getUpdates().first() }

        composeTestRule.setContent {
            PanelTheme {
                UpdatesScreenContent(
                    updateGroups = updateGroups,
                    onOpenManga = {},
                    onResumeChapter = { _, _ -> },
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/09_updates.png")
    }

    @Test
    fun capture_10_downloads_screen() {
        val testManga = SourceManga("manga/the-last-horizon", "The Last Horizon")
        val storageInfo = MockStorageInfo(
            totalGb = 128f,
            usedGb = 42.5f,
            mangaMb = 14500f,
            manhwaMb = 8200f,
            novelsMb = 950f,
            othersMb = 18850f
        )
        val queue = listOf(
            DownloadJob(
                id = "manga/the-last-horizon::ch-208",
                manga = testManga,
                chapter = SourceChapter(url = "ch-208", name = "Chapter 208", chapterNumber = 208f),
                progress = 0.65f,
                downloadedBytes = 15_600_000L,
                totalBytes = 24_000_000L,
                speedBytesPerSec = 2_800_000L,
                etaSeconds = 3,
                status = DownloadStatus.DOWNLOADING
            ),
            DownloadJob(
                id = "manga/the-last-horizon::ch-209",
                manga = testManga,
                chapter = SourceChapter(url = "ch-209", name = "Chapter 209", chapterNumber = 209f),
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = 24_000_000L,
                speedBytesPerSec = 0L,
                etaSeconds = 0,
                status = DownloadStatus.QUEUED
            )
        )
        val completed = listOf(
            DownloadJob(
                id = "manga/the-last-horizon::ch-207",
                manga = testManga,
                chapter = SourceChapter(url = "ch-207", name = "Chapter 207", chapterNumber = 207f),
                progress = 1f,
                downloadedBytes = 24_000_000L,
                totalBytes = 24_000_000L,
                speedBytesPerSec = 0L,
                etaSeconds = 0,
                status = DownloadStatus.COMPLETED
            )
        )

        composeTestRule.setContent {
            PanelTheme {
                DownloadsScreenContent(
                    storageInfo = storageInfo,
                    queue = queue,
                    completed = completed,
                    onOpenManga = {},
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/10_downloads.png")
    }

    @Test
    fun capture_17_batch_download_sheet() {
        val storage = core.download.MemoryDownloadStorage()
        val downloadManager = DownloadManager(downloadStorage = storage)
        val manga = runBlocking { repository.getMangaDetails("manga/the-last-horizon").first() }
            ?: SourceManga("manga/the-last-horizon", "The Last Horizon")
        val chapters = runBlocking { repository.getChapters("manga/the-last-horizon").first() }

        composeTestRule.setContent {
            PanelTheme {
                BatchDownloadSheet(
                    visible = true,
                    manga = manga,
                    chapters = chapters,
                    downloadManager = downloadManager,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/17_batch_download_sheet.png")
    }

    @Test
    fun capture_11_profile_screen() {
        val profile = UserProfile(
            name = "Reader",
            memberSince = "Member since November 2026",
            avatarColorHex = 0xFF38BDF8,
            initials = "R"
        )
        val stats = ReadingStats(
            chaptersRead = 0,
            titlesCount = 0,
            hoursRead = 0.0f
        )
        val weeklyActivity = listOf(
            DayReadingActivity("M", 0),
            DayReadingActivity("T", 0),
            DayReadingActivity("W", 0),
            DayReadingActivity("T", 0),
            DayReadingActivity("F", 0),
            DayReadingActivity("S", 0),
            DayReadingActivity("S", 0)
        )
        val trackers = listOf(
            TrackerAccount(TrackerService.ANILIST, isConnected = false, username = null),
            TrackerAccount(TrackerService.MY_ANIME_LIST, isConnected = false),
            TrackerAccount(TrackerService.KITSU, isConnected = false)
        )
        val librarySummary = LibrarySummary(readingCount = 0, completedCount = 0, planToReadCount = 0)

        composeTestRule.setContent {
            PanelTheme {
                ProfileScreenContent(
                    profile = profile,
                    stats = stats,
                    weeklyActivity = weeklyActivity,
                    trackers = trackers,
                    librarySummary = librarySummary,
                    onBack = {},
                    onUpdateProfile = { _, _ -> },
                    onToggleTracker = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/11_profile.png")
    }

    @Test
    fun capture_12_more_hub_screen() {
        val profile = UserProfile(
            name = "Reader",
            memberSince = "Member since November 2026",
            avatarColorHex = 0xFF38BDF8,
            initials = "R"
        )

        composeTestRule.setContent {
            PanelTheme {
                MoreScreenContent(
                    profile = profile,
                    themeModeName = "Dark Navy",
                    cacheSize = "324 MB",
                    onOpenProfile = {},
                    onNavigateAppearance = {},
                    onNavigateReader = {},
                    onNavigateLibrary = {},
                    onNavigateDownloads = {},
                    onNavigateSources = {},
                    onNavigateBackup = {},
                    onNavigateStorage = {},
                    onNavigateAbout = {},
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/12_more_hub.png")
    }

    @Test
    fun capture_13_appearance_settings_screen() {
        composeTestRule.setContent {
            PanelTheme {
                AppearanceSettingsScreenContent(
                    themeMode = ThemeMode.DARK,
                    accentIndex = 0,
                    reduceGlass = false,
                    reduceMotion = false,
                    onBack = {},
                    onSelectThemeMode = {},
                    onSelectAccentIndex = {},
                    onToggleReduceGlass = {},
                    onToggleReduceMotion = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/13_appearance_settings.png")
    }

    @Test
    fun capture_14_reader_settings_screen() {
        composeTestRule.setContent {
            PanelTheme {
                GlobalReaderSettingsScreenContent(
                    defaultReadingMode = DefaultReadingMode.WEBTOON,
                    sidePadding = 0.12f,
                    brightness = 0.85f,
                    cropBorders = false,
                    keepScreenOn = true,
                    novelFont = NovelFont.SYSTEM,
                    novelFontSize = 16f,
                    novelLineHeight = 1.6f,
                    novelTheme = NovelReaderTheme.DARK,
                    onBack = {},
                    onSelectReadingMode = {},
                    onChangeSidePadding = {},
                    onChangeBrightness = {},
                    onToggleCropBorders = {},
                    onToggleKeepScreenOn = {},
                    onSelectNovelTheme = {},
                    onChangeNovelFontSize = {},
                    onChangeNovelLineHeight = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/14_reader_settings.png")
    }

    @Test
    @Config(qualifiers = "w840dp-h600dp-xhdpi")
    fun capture_18_dual_page_spread_tablet() {
        val pages = runBlocking { repository.getPages("").first() }
        val tabletSettingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.RIGHT_TO_LEFT)
            setSplitDoubleSpread(true)
            setDualPageOffset(false)
        }

        composeTestRule.setContent {
            PanelTheme {
                WebtoonReaderScreenContent(
                    chapterTitle = "Chapter 42: Awakening",
                    pages = pages,
                    settingsRepository = tabletSettingsRepo,
                    isOfflineCached = true,
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/18_dual_page_spread_tablet.png")
    }

    @Test
    fun capture_19_extension_detail_sheet() {
        val extRepo = DefaultExtensionRepository()
        val mangadex = extRepo.extensions.value.firstOrNull { it.name == "MangaDex" } ?: ExtensionItem(
            id = "ext-mangadex",
            name = "MangaDex",
            pkgName = "eu.kanade.tachiyomi.extension.all.mangadex",
            versionName = "1.4.32",
            versionCode = 32,
            lang = "ALL",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.OFFICIAL,
            repoUrl = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo",
            status = ExtensionStatus.INSTALLED,
            iconColorHex = 0xFFFF6740,
            description = "Community manga platform with high-resolution scans, multi-language scanlations, and chapter discussion integration.",
            sources = listOf(
                ExtensionSourceInfo("src-mdx-en", "MangaDex (EN)", "https://mangadex.org", listOf("EN"), "Manga")
            ),
            networkDomains = listOf("api.mangadex.org", "mangadex.org", "uploads.mangadex.org"),
            cachedBytes = 48 * 1024 * 1024L
        )

        composeTestRule.setContent {
            PanelTheme {
                ExtensionDetailSheet(
                    extension = mangadex,
                    isVisible = true,
                    onDismiss = {},
                    onInstall = {},
                    onUpdate = {},
                    onUninstall = {},
                    onClearCache = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/19_extension_detail_sheet.png")
    }

    @Test
    fun capture_20_library_batch_selection() {
        val catRepo = DefaultCategoryRepository()
        val allManga = runBlocking { repository.getLibrary("All").first() }
        val selectedUrls = allManga.take(2).map { it.url }.toSet()

        composeTestRule.setContent {
            PanelTheme {
                LibraryScreenContent(
                    repository = repository,
                    categoryRepository = catRepo,
                    initialSelectionMode = true,
                    initialSelectedUrls = selectedUrls,
                    onOpenManga = {},
                    onResumeChapter = { _, _ -> },
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/20_library_batch_selection.png")
    }

    @Test
    fun capture_21_category_edit_sheet() {
        val catRepo = DefaultCategoryRepository()
        val categories = catRepo.categories.value

        composeTestRule.setContent {
            PanelTheme {
                CategoryEditSheet(
                    categories = categories,
                    isVisible = true,
                    onDismiss = {},
                    onAddCategory = {},
                    onDeleteCategory = {},
                    onMoveCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/21_category_edit_sheet.png")
    }

    @Test
    fun capture_23_sideload_extension_dialog() {
        val extRepo = DefaultExtensionRepository()

        composeTestRule.setContent {
            PanelTheme {
                SideloadExtensionDialog(
                    isVisible = true,
                    onDismiss = {},
                    repository = extRepo
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/23_sideload_extension_dialog.png")
    }

    @Test
    fun capture_24_repo_manager_dialog() {
        val extRepo = DefaultExtensionRepository()

        composeTestRule.setContent {
            PanelTheme {
                AddRepoDialog(
                    isVisible = true,
                    onDismiss = {},
                    onAddRepo = { _, _ -> },
                    repositories = extRepo.repositories.value,
                    onRemoveRepo = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/24_repo_manager_dialog.png")
    }

    private fun createPerTitlePreviewRepos(): Pair<FakeSettingsRepository, FakeReaderPrefsRepository> {
        val settingsRepo = FakeSettingsRepository().apply {
            setIncognitoMode(true)
        }
        val prefsRepo = FakeReaderPrefsRepository(FakeSettingsRepository())
        val titleKey = canonicalTitleKey("Solo Leveling", isNovel = false)
        runBlocking {
            prefsRepo.upsertPrefs(
                TitleReaderPrefs(
                    titleKey = titleKey,
                    readingMode = DefaultReadingMode.RIGHT_TO_LEFT,
                    colorFilter = ReaderColorFilter.SEPIA,
                    sidePadding = 0.16f
                )
            )
        }
        return settingsRepo to prefsRepo
    }

    @Test
    fun capture_25_reader_settings_per_title_dark_narrow_320dp() {
        val (settingsRepo, prefsRepo) = createPerTitlePreviewRepos()
        val titleKey = canonicalTitleKey("Solo Leveling", isNovel = false)

        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                Box(modifier = Modifier.width(320.dp)) {
                    ReaderSettingsScreenContent(
                        settingsRepository = settingsRepo,
                        titleKey = titleKey,
                        readerPrefsRepository = prefsRepo,
                        onDismiss = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/25_reader_settings_per_title_dark_320dp.png")
    }

    @Test
    fun capture_26_reader_settings_per_title_light_font13() {
        val (settingsRepo, prefsRepo) = createPerTitlePreviewRepos()
        val titleKey = canonicalTitleKey("Solo Leveling", isNovel = false)

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.3f)
            ) {
                PanelTheme(darkTheme = false) {
                    Box(modifier = Modifier.width(340.dp)) {
                        ReaderSettingsScreenContent(
                            settingsRepository = settingsRepo,
                            titleKey = titleKey,
                            readerPrefsRepository = prefsRepo,
                            onDismiss = {}
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/26_reader_settings_per_title_light_font13.png")
    }

    @Test
    fun capture_27_reader_settings_per_title_dark_font20() {
        val (settingsRepo, prefsRepo) = createPerTitlePreviewRepos()
        val titleKey = canonicalTitleKey("Solo Leveling", isNovel = false)

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                PanelTheme(themeMode = ThemeMode.DARK) {
                    Box(modifier = Modifier.width(320.dp)) {
                        ReaderSettingsScreenContent(
                            settingsRepository = settingsRepo,
                            titleKey = titleKey,
                            readerPrefsRepository = prefsRepo,
                            onDismiss = {}
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/27_reader_settings_per_title_dark_font20.png")
    }

    private fun createNovelPerTitlePreviewRepos(): Pair<FakeSettingsRepository, FakeReaderPrefsRepository> {
        val settingsRepo = FakeSettingsRepository().apply {
            setIncognitoMode(true)
        }
        val prefsRepo = FakeReaderPrefsRepository(FakeSettingsRepository())
        val titleKey = canonicalTitleKey("Omniscient Reader", isNovel = true)
        runBlocking {
            prefsRepo.upsertPrefs(
                TitleReaderPrefs(
                    titleKey = titleKey,
                    novelTheme = NovelReaderTheme.SEPIA,
                    novelFont = NovelFont.SERIF,
                    novelFontSize = 20f
                )
            )
        }
        return settingsRepo to prefsRepo
    }

    @Test
    fun capture_28_novel_settings_dark_320dp() {
        val (settingsRepo, prefsRepo) = createNovelPerTitlePreviewRepos()
        val titleKey = canonicalTitleKey("Omniscient Reader", isNovel = true)

        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                Box(modifier = Modifier.width(320.dp)) {
                    NovelSettingsSheetContent(
                        settingsRepository = settingsRepo,
                        titleKey = titleKey,
                        readerPrefsRepository = prefsRepo
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/28_novel_settings_dark_320dp.png")
    }

    @Test
    fun capture_29_novel_settings_dark_font20() {
        val (settingsRepo, prefsRepo) = createNovelPerTitlePreviewRepos()
        val titleKey = canonicalTitleKey("Omniscient Reader", isNovel = true)

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                PanelTheme(themeMode = ThemeMode.DARK) {
                    Box(modifier = Modifier.width(320.dp)) {
                        NovelSettingsSheetContent(
                            settingsRepository = settingsRepo,
                            titleKey = titleKey,
                            readerPrefsRepository = prefsRepo
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/29_novel_settings_dark_font20.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun capture_25_webtoon_reader_narrow_320dp() {
        val pages = runBlocking { repository.getPages("").first() }
        val mangaSettingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.WEBTOON)
        }

        composeTestRule.setContent {
            PanelTheme {
                WebtoonReaderScreenContent(
                    chapterTitle = "Chapter 142: The Sovereign of Eternal Shadows and Crimson Stars",
                    pages = pages,
                    settingsRepository = mangaSettingsRepo,
                    isOfflineCached = true,
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/25_webtoon_reader_narrow_320dp.png")
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-xhdpi")
    fun capture_30_reader_rtl_tablet_landscape() {
        val pages = runBlocking { repository.getPages("").first() }
        val mangaSettingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.RIGHT_TO_LEFT)
        }

        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                WebtoonReaderScreenContent(
                    chapterTitle = "Chapter 142: Tablet Dual-Page RTL",
                    pages = pages,
                    settingsRepository = mangaSettingsRepo,
                    hasPreviousChapter = true,
                    hasNextChapter = true,
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/30_reader_rtl_tablet_landscape.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun capture_31_reader_minimal_glass_font20() {
        val pages = runBlocking { repository.getPages("").first() }
        val mangaSettingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.LEFT_TO_RIGHT)
        }

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                PanelTheme(themeMode = ThemeMode.DARK) {
                    WebtoonReaderScreenContent(
                        chapterTitle = "Chapter 142: The Sovereign of Eternal Shadows",
                        pages = pages,
                        settingsRepository = mangaSettingsRepo,
                        hasPreviousChapter = false,
                        hasNextChapter = true,
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/31_reader_minimal_glass_font20.png")
    }
}
