package io.panel.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import core.source.MangaStatus
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.mock.MockStorageInfo
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
import ui.screens.novelreader.NovelReaderScreenContent
import ui.screens.updates.UpdatesScreenContent
import ui.screens.webtoonreader.ReaderSettingsScreenContent
import ui.screens.webtoonreader.WebtoonReaderScreenContent
import ui.screens.welcome.WelcomeScreenContent
import ui.theme.PanelTheme

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

        composeTestRule.setContent {
            PanelTheme {
                WebtoonReaderScreenContent(
                    chapterTitle = "Episode 42",
                    pages = pages,
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
}
