package io.panel.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
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
import data.mock.PanelRepository
import data.profile.FakeProfileRepository
import data.profile.ProfileRepository
import data.settings.FakeSettingsRepository
import data.settings.SettingsRepository
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
import ui.screens.details.TitleDetailsScreenContent
import ui.screens.maintab.MainTabScreen
import ui.screens.welcome.WelcomeScreen
import ui.theme.PanelTheme
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
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
}
