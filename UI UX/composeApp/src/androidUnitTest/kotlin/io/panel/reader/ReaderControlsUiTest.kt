package io.panel.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import core.source.SourcePage
import data.settings.DefaultReadingMode
import data.settings.FakeSettingsRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ui.screens.webtoonreader.WebtoonReaderScreenContent
import ui.theme.PanelTheme
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ReaderControlsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val samplePages = listOf(
        SourcePage(index = 0, url = "p1"),
        SourcePage(index = 1, url = "p2")
    )

    @Test
    fun ltrMode_topAndBottomControls_triggerCorrectCallbacks() {
        var backClicks = 0
        var settingsClicks = 0
        var prevClicks = 0
        var nextClicks = 0
        val settingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.LEFT_TO_RIGHT)
        }

        composeTestRule.setContent {
            PanelTheme {
                WebtoonReaderScreenContent(
                    chapterUrl = "ch-10",
                    chapterTitle = "Chapter 10",
                    pages = samplePages,
                    onBack = { backClicks++ },
                    onOpenSettings = { settingsClicks++ },
                    onPreviousChapter = { prevClicks++ },
                    onNextChapter = { nextClicks++ },
                    settingsRepository = settingsRepo,
                    hasPreviousChapter = true,
                    hasNextChapter = true
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("Reader Settings").performClick()
        composeTestRule.onNodeWithContentDescription("Previous Chapter").performClick()
        composeTestRule.mainClock.advanceTimeBy(600L)
        composeTestRule.onNodeWithContentDescription("Next Chapter").performClick()

        assertEquals(1, backClicks)
        assertEquals(1, settingsClicks)
        assertEquals(1, prevClicks)
        assertEquals(1, nextClicks)
    }

    @Test
    fun rtlMode_chapterButtonsRespectDirectionAndDisabledState() {
        var prevClicks = 0
        var nextClicks = 0
        val settingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.RIGHT_TO_LEFT)
        }

        composeTestRule.setContent {
            PanelTheme {
                WebtoonReaderScreenContent(
                    chapterUrl = "ch-1",
                    chapterTitle = "Chapter 1",
                    pages = samplePages,
                    onBack = {},
                    onOpenSettings = {},
                    onPreviousChapter = { prevClicks++ },
                    onNextChapter = { nextClicks++ },
                    settingsRepository = settingsRepo,
                    hasPreviousChapter = false,
                    hasNextChapter = true
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Previous Chapter").performClick()
        composeTestRule.onNodeWithContentDescription("Next Chapter").performClick()

        assertEquals(0, prevClicks)
        assertEquals(1, nextClicks)
    }
}
