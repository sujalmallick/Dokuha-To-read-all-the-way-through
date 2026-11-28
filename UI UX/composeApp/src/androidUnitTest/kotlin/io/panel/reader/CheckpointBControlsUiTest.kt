package io.panel.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import core.source.NovelContent
import data.settings.FakeSettingsRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.screens.novelreader.NovelReaderScreenContent
import ui.theme.PanelTheme
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class CheckpointBControlsUiTest {

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
    fun bug07_bug08_novelTtsPauseKeepsBarAndResumeWorksAndProgressFires() {
        var progressCalls = 0
        val novel = NovelContent(
            chapterUrl = "ch/1",
            title = "Chapter 1",
            htmlBody = "<p>First paragraph of the novel.</p><p>Second paragraph of the novel.</p><p>Third paragraph.</p>",
            plainText = "First paragraph of the novel.\n\nSecond paragraph of the novel.\n\nThird paragraph."
        )

        composeTestRule.setContent {
            PanelTheme {
                NovelReaderScreenContent(
                    chapterNumber = "Chapter 1",
                    chapterTitle = "Awakening",
                    novelContent = novel,
                    settingsRepository = FakeSettingsRepository(),
                    onBack = {},
                    onProgressChange = { _, _ -> progressCalls++ }
                )
            }
        }

        // Progress callback fires on load
        composeTestRule.waitUntil(timeoutMillis = 3_000) { progressCalls > 0 }
        assertTrue(progressCalls > 0)

        // Start TTS -> bar opens with Pause button
        composeTestRule.onNodeWithText("Listen").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("AUDIO READER").assertExists()

        // Pause keeps player bar visible and flips to Resume
        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithContentDescription("Resume").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("AUDIO READER").assertExists()

        // Resume flips back to Pause
        composeTestRule.onNodeWithContentDescription("Resume").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun bug07_novelAudioBar320dpScaledFontScreenshot() {
        val novel = NovelContent(
            chapterUrl = "ch/1",
            title = "Chapter 1",
            htmlBody = "<p>First paragraph.</p><p>Second paragraph.</p>",
            plainText = "First paragraph.\n\nSecond paragraph."
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.3f)) {
                PanelTheme {
                    Box(modifier = Modifier.size(width = 320.dp, height = 640.dp)) {
                        NovelReaderScreenContent(
                            chapterNumber = "Chapter 1",
                            chapterTitle = "Awakening",
                            novelContent = novel,
                            settingsRepository = FakeSettingsRepository(),
                            onBack = {}
                        )
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Listen").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/checkpoint_b_novel_tts_320dp_1_3x.png")
    }
}
