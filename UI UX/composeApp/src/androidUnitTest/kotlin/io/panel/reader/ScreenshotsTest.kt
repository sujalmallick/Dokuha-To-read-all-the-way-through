package io.panel.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import core.source.MangaStatus
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.mock.MockStorageInfo
import data.profile.DayReadingActivity
import data.profile.LibrarySummary
import data.profile.ReadingStats
import data.profile.TrackerAccount
import data.profile.TrackerService
import data.profile.UserProfile
import data.settings.DefaultReadingMode
import data.settings.FakeSettingsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.screens.browse.BrowseScreenContent
import ui.screens.details.TitleDetailsScreenContent
import ui.screens.downloads.DownloadsScreenContent
import ui.screens.extensions.ExtensionsScreenContent
import ui.screens.library.LibraryScreenContent
import ui.screens.more.MoreScreenContent
import ui.screens.novelreader.NovelReaderScreenContent
import ui.screens.profile.ProfileScreenContent
import ui.screens.settings.AppearanceSettingsScreenContent
import ui.screens.settings.GlobalReaderSettingsScreenContent
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
        val extensions = runBlocking { repository.getExtensions().first() }

        composeTestRule.setContent {
            PanelTheme {
                ExtensionsScreenContent(
                    initialExtensions = extensions,
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
        val storageInfo = runBlocking { repository.getStorageInfo().first() }
        val downloads = runBlocking { repository.getDownloads().first() }

        composeTestRule.setContent {
            PanelTheme {
                DownloadsScreenContent(
                    storageInfo = storageInfo,
                    initialDownloads = downloads,
                    onOpenManga = {},
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/10_downloads.png")
    }

    @Test
    fun capture_11_profile_screen() {
        val profile = UserProfile(
            name = "Sujal",
            memberSince = "Reading since March 2026",
            avatarColorHex = 0xFF007AFF,
            initials = "S"
        )
        val stats = ReadingStats(
            chaptersRead = 482,
            titlesCount = 37,
            hoursRead = 142.5f
        )
        val weeklyActivity = listOf(
            DayReadingActivity("M", 45),
            DayReadingActivity("T", 90),
            DayReadingActivity("W", 30),
            DayReadingActivity("T", 120),
            DayReadingActivity("F", 60),
            DayReadingActivity("S", 150),
            DayReadingActivity("S", 85)
        )
        val trackers = listOf(
            TrackerAccount(TrackerService.ANILIST, isConnected = true, username = "sujal_reads"),
            TrackerAccount(TrackerService.MY_ANIME_LIST, isConnected = false),
            TrackerAccount(TrackerService.KITSU, isConnected = false)
        )
        val librarySummary = LibrarySummary(readingCount = 14, completedCount = 18, planToReadCount = 5)

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
            name = "Sujal",
            memberSince = "Reading since March 2026",
            avatarColorHex = 0xFF007AFF,
            initials = "S"
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
}
