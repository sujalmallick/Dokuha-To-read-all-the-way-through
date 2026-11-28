package ui.screens.webtoonreader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.theme.PanelTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebtoonPreviousChapterTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldAutoReturnOnTopScrollGuardsAndTriggersAtThreshold() {
        assertFalse(
            shouldAutoReturnOnTopScroll(
                hasPreviousChapter = false,
                alreadyTriggered = false,
                canScrollBackward = false,
                upwardScrollDeltaY = 120f,
                isEmptyPages = false
            )
        )
        assertFalse(
            shouldAutoReturnOnTopScroll(
                hasPreviousChapter = true,
                alreadyTriggered = true,
                canScrollBackward = false,
                upwardScrollDeltaY = 120f,
                isEmptyPages = false
            )
        )
        assertFalse(
            shouldAutoReturnOnTopScroll(
                hasPreviousChapter = true,
                alreadyTriggered = false,
                canScrollBackward = true,
                upwardScrollDeltaY = 120f,
                isEmptyPages = false
            )
        )
        assertFalse(
            shouldAutoReturnOnTopScroll(
                hasPreviousChapter = true,
                alreadyTriggered = false,
                canScrollBackward = false,
                upwardScrollDeltaY = 40f,
                isEmptyPages = false
            )
        )
        assertTrue(
            shouldAutoReturnOnTopScroll(
                hasPreviousChapter = true,
                alreadyTriggered = false,
                canScrollBackward = false,
                upwardScrollDeltaY = 60f,
                isEmptyPages = false
            )
        )
    }

    @Test
    fun previousChapterHeaderShowsCurrentAndPreviousChapterAndHandlesTap() {
        var clicks = 0

        composeRule.setContent {
            PanelTheme {
                WebtoonPreviousChapterHeader(
                    currentChapterTitle = "Chapter 349",
                    previousChapterTitle = "Chapter 348",
                    hasPreviousChapter = true,
                    onPreviousChapter = { clicks++ }
                )
            }
        }

        composeRule.onNodeWithText("Current: Chapter 349").assertIsDisplayed()
        composeRule.onNodeWithText("Previous: Chapter 348 · Pull or tap to go back")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun previousChapterHeaderShowsFirstChapterWhenNoPreviousExists() {
        composeRule.setContent {
            PanelTheme {
                WebtoonPreviousChapterHeader(
                    currentChapterTitle = "Chapter 1",
                    previousChapterTitle = "",
                    hasPreviousChapter = false,
                    onPreviousChapter = {}
                )
            }
        }

        composeRule.onNodeWithText("Current: Chapter 1").assertIsDisplayed()
        composeRule.onNodeWithText("First chapter").assertIsDisplayed()
    }
}
