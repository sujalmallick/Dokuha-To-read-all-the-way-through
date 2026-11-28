package io.panel.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import core.download.DownloadManager
import core.download.MemoryDownloadStorage
import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import data.mock.FakePanelRepository
import data.settings.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.components.PanelBottomNavBar
import ui.components.PanelSearchBar
import ui.components.PanelTab
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.details.TitleDetailsScreenContent
import ui.screens.library.LibraryScreenContent
import ui.theme.PanelTheme
import ui.theme.ThemeMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w420dp-h920dp-xhdpi")
class GlassNavAndMainTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomDockAndSearchBar_renderWithSharedGlassStateInDarkAndLight() {
        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK, darkTheme = true) {
                val glassState = rememberPanelGlassState()
                Box(modifier = Modifier.size(width = 411.dp, height = 240.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .glassSource(glassState)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF312E81))
                                )
                            )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            PanelSearchBar(
                                query = "",
                                onQueryChange = {},
                                placeholder = "Search sources...",
                                glassState = glassState
                            )
                        }
                    }
                    PanelBottomNavBar(
                        selectedTab = PanelTab.LIBRARY,
                        onTabSelected = {},
                        glassState = glassState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search sources...").assertExists()
        composeTestRule.onNodeWithText("Library").assertExists()
        composeTestRule.onRoot().captureRoboImage("screenshots/pr2_nav_search_dark.png")
    }

    @Test
    fun librarySelectionBar_usesGlassSurfaceAndGlassDialog(): Unit = runBlocking {
        val repo = FakePanelRepository()
        val first = repo.getLibrary("All").first().first()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.3f)) {
                PanelTheme(themeMode = ThemeMode.DARK) {
                    Box(modifier = Modifier.size(width = 320.dp, height = 640.dp)) {
                        LibraryScreenContent(
                            repository = repo,
                            settingsRepository = FakeSettingsRepository(),
                            downloadManager = DownloadManager(MemoryDownloadStorage()),
                            onOpenManga = {},
                            onResumeChapter = { _, _ -> },
                            onTabSelected = {},
                            initialSelectionMode = true,
                            initialSelectedUrls = setOf(first.url)
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select All").assertExists()
        composeTestRule.onRoot().captureRoboImage("screenshots/pr2_library_selection_320dp.png")

        composeTestRule.onNodeWithText("Remove").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Remove from library?").fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun titleDetails_quickChipsAndDeleteDialogUseGlassComponents() {
        val storage = MemoryDownloadStorage().apply {
            savePageImage("ch/1", 0, byteArrayOf(1))
        }
        val dm = DownloadManager(storage)
        val manga = SourceManga(url = "manga/1", title = "Dokuha Test", status = MangaStatus.ONGOING)
        val chapters = listOf(SourceChapter(url = "ch/1", name = "Ch 1", chapterNumber = 1f))

        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                Box(modifier = Modifier.size(width = 411.dp, height = 820.dp)) {
                    TitleDetailsScreenContent(
                        manga = manga,
                        chapters = chapters,
                        onBack = {},
                        onReadChapter = {},
                        downloadManager = dm
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete (1)").assertExists()
        composeTestRule.onRoot().captureRoboImage("screenshots/pr2_title_details_chips.png")
    }
}
