package io.panel.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import core.source.NovelContent
import core.source.SourcePage
import data.settings.FakeSettingsRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.screens.novelreader.NovelReaderScreenContent
import ui.screens.settings.SettingsScreenContent
import ui.screens.webtoonreader.ReaderSettingsSheetCard
import ui.screens.webtoonreader.WebtoonReaderScreenContent
import ui.theme.PanelTheme
import ui.theme.ThemeMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w420dp-h920dp-xhdpi")
class GlassReadersAndSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun webtoonReaderOverlays_renderHighContrastBalancedGlass() {
        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                Box(modifier = Modifier.size(width = 411.dp, height = 820.dp)) {
                    WebtoonReaderScreenContent(
                        chapterTitle = "Episode 42: Glass Horizon",
                        pages = listOf(SourcePage(0, "p0"), SourcePage(1, "p1")),
                        settingsRepository = FakeSettingsRepository(),
                        onBack = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Episode 42: Glass Horizon").assertExists()
        composeTestRule.onRoot().captureRoboImage("screenshots/pr3_webtoon_reader_overlays.png")
    }

    @Test
    fun novelReaderOverlays_renderHighContrastBalancedGlassAt320dp() {
        val novel = NovelContent(
            chapterUrl = "ch/12",
            title = "The Wind Rises",
            htmlBody = "<p>First paragraph of the novel.</p><p>Second paragraph of the novel.</p>",
            plainText = "First paragraph of the novel.\n\nSecond paragraph of the novel."
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.3f)) {
                PanelTheme(themeMode = ThemeMode.DARK) {
                    Box(modifier = Modifier.size(width = 320.dp, height = 640.dp)) {
                        NovelReaderScreenContent(
                            chapterNumber = "Chapter 12",
                            chapterTitle = "The Wind Rises",
                            novelContent = novel,
                            settingsRepository = FakeSettingsRepository(),
                            onBack = {}
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithText("Listen").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("AUDIO READER").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/pr3_novel_reader_320dp.png")
    }

    @Test
    fun readerSettingsSheetAndSettingsScreen_renderWithGlassTokens() {
        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                Box(modifier = Modifier.size(width = 411.dp, height = 820.dp)) {
                    ReaderSettingsSheetCard(settingsRepository = FakeSettingsRepository())
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("screenshots/pr3_reader_settings_sheet.png")
    }

    @Test
    fun settingsScreen_rendersGlassSearchFieldInLightMode() {
        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.SYSTEM, darkTheme = false) {
                Box(modifier = Modifier.size(width = 411.dp, height = 820.dp)) {
                    SettingsScreenContent(
                        themeModeName = "System",
                        readingModeName = "Webtoon",
                        cacheSize = "12 MB",
                        onBack = {},
                        onNavigateAppearance = {},
                        onNavigateReader = {},
                        onNavigateLibrary = {},
                        onNavigateDownloads = {},
                        onNavigateSources = {},
                        onNavigateBackup = {},
                        onNavigateStorage = {},
                        onNavigateAbout = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search settings...").assertExists()
        composeTestRule.onRoot().captureRoboImage("screenshots/pr3_settings_light.png")
    }
}
