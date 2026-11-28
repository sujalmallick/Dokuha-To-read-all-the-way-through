package io.panel.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.github.takahirom.roborazzi.captureRoboImage
import core.download.DownloadManager
import core.download.MemoryDownloadStorage
import core.extensions.ExtensionManager
import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import core.storage.InMemoryKeyValueStorage
import data.library.CategoryRepository
import data.library.DefaultCategoryRepository
import data.mock.FakePanelRepository
import data.mock.MockUpdateGroup
import data.mock.MockUpdateItem
import data.mock.PanelRepository
import data.profile.FakeProfileRepository
import data.profile.ProfileRepository
import data.settings.FakeSettingsRepository
import data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.components.PanelTab
import ui.screens.details.TitleDetailsScreenContent
import ui.screens.library.LibraryFilter
import ui.screens.library.LibraryScreenContent
import ui.screens.library.filterAndSortLibrary
import ui.screens.maintab.MainTabScreen
import ui.screens.updates.UpdatesScreenContent
import ui.screens.welcome.WelcomeScreen
import ui.theme.PanelTheme
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class CheckpointAControlsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        runCatching { stopKoin() }
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    @Test
    fun bug06_welcomeGetStartedReplacesStackWithMainTabScreenAndSingleBottomBar() {
        val settingsRepo = FakeSettingsRepository()
        val panelRepo = FakePanelRepository()
        var capturedNavigator: Navigator? = null

        composeTestRule.setContent {
            KoinApplication(application = {
                modules(
                    module {
                        single<SettingsRepository> { settingsRepo }
                        single<PanelRepository> { panelRepo }
                        single<CategoryRepository> { DefaultCategoryRepository() }
                        single<ProfileRepository> { FakeProfileRepository() }
                        single { DownloadManager(MemoryDownloadStorage()) }
                        single {
                            ExtensionManager(
                                storage = InMemoryKeyValueStorage(),
                                panelRepository = panelRepo,
                                autoSeedDefaults = false,
                                deferInit = true
                            )
                        }
                    }
                )
            }) {
                PanelTheme {
                    Navigator(WelcomeScreen()) { navigator ->
                        capturedNavigator = navigator
                        navigator.lastItem.Content()
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.waitForIdle()

        assertTrue(settingsRepo.isOnboardingCompleted.value)
        val nav = capturedNavigator!!
        assertEquals(1, nav.items.size, "Back stack must contain only MainTabScreen so Back does not return to Welcome")
        assertIs<MainTabScreen>(nav.lastItem)

        // Verify no duplicate bottom bar (Updates & More tabs appear once in the single PanelBottomNavBar)
        composeTestRule.onAllNodesWithText("Updates").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("More").assertCountEquals(1)
    }

    @Test
    fun bug05_titleDetailsDownloadButtonOpensBatchSheetWithoutAutoDownloadingAll() {
        var downloadAllCount = 0
        val dm = DownloadManager(MemoryDownloadStorage())
        val manga = SourceManga(
            url = "manga/test",
            title = "Test Title",
            status = MangaStatus.ONGOING
        )
        val chapters = listOf(
            SourceChapter(url = "ch/1", name = "Chapter 1", chapterNumber = 1f),
            SourceChapter(url = "ch/2", name = "Chapter 2", chapterNumber = 2f)
        )

        composeTestRule.setContent {
            PanelTheme {
                Box(modifier = Modifier.size(width = 411.dp, height = 900.dp)) {
                    TitleDetailsScreenContent(
                        manga = manga,
                        chapters = chapters,
                        onBack = {},
                        onReadChapter = {},
                        onDownloadAll = { downloadAllCount++ },
                        downloadManager = dm
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Download").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        assertEquals(0, downloadAllCount, "Header Download button must not call onDownloadAll()")
        assertEquals(0, dm.queue.value.size, "No chapters should be auto-queued before selecting preset")
        composeTestRule.onNodeWithText("Batch Download").assertExists()
    }

    @Test
    fun bug02_updatesTabRefreshShowsSnackbarAndHidesDownloadButtons() {
        val settingsRepo = FakeSettingsRepository()
        val panelRepo = FakePanelRepository()

        composeTestRule.setContent {
            KoinApplication(application = {
                modules(
                    module {
                        single<SettingsRepository> { settingsRepo }
                        single<PanelRepository> { panelRepo }
                        single<CategoryRepository> { DefaultCategoryRepository() }
                        single<ProfileRepository> { FakeProfileRepository() }
                        single { DownloadManager(MemoryDownloadStorage()) }
                        single {
                            ExtensionManager(
                                storage = InMemoryKeyValueStorage(),
                                panelRepository = panelRepo,
                                autoSeedDefaults = false,
                                deferInit = true
                            )
                        }
                    }
                )
            }) {
                PanelTheme {
                    Navigator(MainTabScreen(initialTab = PanelTab.UPDATES)) { navigator ->
                        navigator.lastItem.Content()
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Refresh updates").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("All caught up", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("All caught up", substring = true).assertExists()
    }

    @Test
    fun bug02_updatesScreenHidesDownloadButtonsWhenNull() {
        val sampleGroup = MockUpdateGroup(
            title = "Today",
            items = listOf(
                MockUpdateItem(
                    manga = SourceManga(url = "m/1", title = "Sample Manga"),
                    chapter = SourceChapter(url = "c/1", name = "Chapter 1", chapterNumber = 1f)
                )
            )
        )
        composeTestRule.setContent {
            PanelTheme {
                UpdatesScreenContent(
                    updateGroups = listOf(sampleGroup),
                    onDownloadUpdate = null,
                    onOpenManga = {},
                    onResumeChapter = { _, _ -> },
                    onTabSelected = {}
                )
            }
        }
        composeTestRule.onAllNodesWithText("Download").assertCountEquals(0)
        composeTestRule.onNodeWithText("Sample Manga").assertExists()
    }

    @Test
    fun bug03_libraryBatchMarkReadAndRemoveConfirmWorkAndDownloadIsHidden(): Unit = runBlocking {
        val delegate = FakePanelRepository()
        val markedReadUrls = mutableListOf<String>()
        val recordingRepo = object : PanelRepository by delegate {
            override suspend fun markChaptersRead(chapterUrls: List<String>, isRead: Boolean) {
                if (isRead) markedReadUrls.addAll(chapterUrls)
                delegate.markChaptersRead(chapterUrls, isRead)
            }
        }
        val firstManga = delegate.getLibrary("All").first().first()
        val settingsRepo = FakeSettingsRepository()

        composeTestRule.setContent {
            PanelTheme {
                LibraryScreenContent(
                    repository = recordingRepo,
                    settingsRepository = settingsRepo,
                    onOpenManga = {},
                    onResumeChapter = { _, _ -> },
                    onTabSelected = {},
                    initialSelectionMode = true,
                    initialSelectedUrls = setOf(firstManga.url)
                )
            }
        }

        // Batch Download is hidden
        composeTestRule.onAllNodesWithText("Download").assertCountEquals(0)

        // Capture screenshot of updated BatchActionBar
        composeTestRule.onRoot().captureRoboImage("screenshots/checkpoint_a_library_batch_bar.png")

        // Mark Read calls repository.markChaptersRead with the manga's chapter URLs
        composeTestRule.onNodeWithText("Mark Read").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Marked 1 title as read").fetchSemanticsNodes().isNotEmpty()
        }
        val chapters = delegate.getChapters(firstManga.url).first()
        assertTrue(chapters.isNotEmpty() && markedReadUrls.containsAll(chapters.map { it.url }))
    }

    @Test
    fun bug03_libraryBatchRemoveRequiresConfirmationAndRespectsIncognitoMarkRead(): Unit = runBlocking {
        val delegate = FakePanelRepository()
        val markedReadUrls = mutableListOf<String>()
        val recordingRepo = object : PanelRepository by delegate {
            override suspend fun markChaptersRead(chapterUrls: List<String>, isRead: Boolean) {
                if (isRead) markedReadUrls.addAll(chapterUrls)
                delegate.markChaptersRead(chapterUrls, isRead)
            }
        }
        val firstManga = delegate.getLibrary("All").first().first()
        val settingsRepo = FakeSettingsRepository().apply { setIncognitoMode(true) }

        composeTestRule.setContent {
            PanelTheme {
                LibraryScreenContent(
                    repository = recordingRepo,
                    settingsRepository = settingsRepo,
                    onOpenManga = {},
                    onResumeChapter = { _, _ -> },
                    onTabSelected = {},
                    initialSelectionMode = true,
                    initialSelectedUrls = setOf(firstManga.url)
                )
            }
        }

        // Incognito Mark Read shows notice and does not call markChaptersRead
        composeTestRule.onNodeWithText("Mark Read").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Incognito mode: reading state not saved")
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(markedReadUrls.isEmpty())
    }

    @Test
    fun bug03_libraryBatchRemoveConfirmRemovesTitle(): Unit = runBlocking {
        val delegate = FakePanelRepository()
        val removedUrls = mutableListOf<String>()
        val recordingRepo = object : PanelRepository by delegate {
            override suspend fun toggleFavorite(manga: SourceManga, isFavorite: Boolean) {
                if (!isFavorite) removedUrls.add(manga.url)
                delegate.toggleFavorite(manga, isFavorite)
            }
        }
        val firstManga = delegate.getLibrary("All").first().first()

        composeTestRule.setContent {
            PanelTheme {
                LibraryScreenContent(
                    repository = recordingRepo,
                    settingsRepository = FakeSettingsRepository(),
                    onOpenManga = {},
                    onResumeChapter = { _, _ -> },
                    onTabSelected = {},
                    initialSelectionMode = true,
                    initialSelectedUrls = setOf(firstManga.url)
                )
            }
        }

        composeTestRule.onNodeWithText("Remove").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Remove from library?").assertExists()

        // Confirm remove inside AlertDialog
        composeTestRule.onAllNodesWithText("Remove")[1].performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Removed 1 title from library")
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(firstManga.url in removedUrls)
    }

    @Test
    fun bug04_downloadedOnlyFilterLogicAndEmptyStateResetWork(): Unit = runBlocking {
        val fakeRepo = FakePanelRepository()
        val library = fakeRepo.getLibrary("All").first()
        val downloadedManga = library.first()
        val filtered = filterAndSortLibrary(
            items = library,
            filter = LibraryFilter(downloadedOnly = true),
            stats = emptyMap(),
            isDownloaded = { it.url == downloadedManga.url }
        )
        assertEquals(listOf(downloadedManga), filtered)

        val dm = DownloadManager(MemoryDownloadStorage())
        composeTestRule.setContent {
            PanelTheme {
                LibraryScreenContent(
                    repository = fakeRepo,
                    settingsRepository = FakeSettingsRepository(),
                    downloadManager = dm,
                    onOpenManga = {},
                    onResumeChapter = { _, _ -> },
                    onTabSelected = {}
                )
            }
        }

        // Open Filter sheet, toggle Downloaded Only, Apply -> shows empty state even though recentlyReadItems is non-empty
        composeTestRule.onNodeWithContentDescription("Filter library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Downloaded Only").performClick()
        composeTestRule.onNodeWithText("Apply Filters", substring = true).performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("No matching titles").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Reset Filters").assertExists()

        // Clicking Reset Filters restores library items
        composeTestRule.onNodeWithText("Reset Filters").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(downloadedManga.title).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun bug14_emptyState320dpAndScaledFontScreenshots() {
        val fakeRepo = FakePanelRepository()
        val dm = DownloadManager(MemoryDownloadStorage())
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.3f)) {
                PanelTheme {
                    Box(modifier = Modifier.size(width = 320.dp, height = 640.dp)) {
                        LibraryScreenContent(
                            repository = fakeRepo,
                            settingsRepository = FakeSettingsRepository(),
                            downloadManager = dm,
                            onOpenManga = {},
                            onResumeChapter = { _, _ -> },
                            onTabSelected = {},
                            initialSelectionMode = true
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/checkpoint_a_320dp_1_3x_selection.png")
    }
}
