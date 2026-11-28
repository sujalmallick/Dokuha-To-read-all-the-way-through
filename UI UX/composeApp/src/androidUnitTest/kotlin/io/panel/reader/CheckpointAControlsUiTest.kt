package io.panel.reader

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.Navigator
import core.download.DownloadManager
import core.download.MemoryDownloadStorage
import core.extensions.ExtensionManager
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
import ui.screens.maintab.MainTabScreen
import ui.screens.welcome.WelcomeScreen
import ui.theme.PanelTheme
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
}
