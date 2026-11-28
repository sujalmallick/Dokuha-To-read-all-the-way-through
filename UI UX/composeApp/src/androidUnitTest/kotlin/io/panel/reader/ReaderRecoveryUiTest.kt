package io.panel.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.source.SourcePage
import data.settings.DefaultReadingMode
import data.settings.FakeSettingsRepository
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.screens.webtoonreader.MangaPageErrorCard
import ui.screens.webtoonreader.WebtoonReaderScreenContent
import ui.screens.webtoonreader.formatImageLoadError
import ui.theme.PanelTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderRecoveryUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        coil3.SingletonImageLoader.reset()
    }

    @Test
    fun primaryAction_readerRendersPagesAndAdvancesChapter() {
        var nextChapterClicks = 0
        val settingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.WEBTOON)
        }
        val pages = listOf(
            SourcePage(index = 0, url = "page-1"),
            SourcePage(index = 1, url = "page-2")
        )

        composeTestRule.setContent {
            PanelTheme {
                WebtoonReaderScreenContent(
                    chapterTitle = "Episode 42",
                    pages = pages,
                    settingsRepository = settingsRepo,
                    hasNextChapter = true,
                    nextChapterTitle = "Episode 43",
                    onNextChapter = { nextChapterClicks++ },
                    onBack = {}
                )
            }
        }

        // Controls are visible on open without duplicate floating pill or middle-menu clutter
        composeTestRule.onNodeWithText("Episode 42").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 / 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("◉ Menu").assertDoesNotExist()

        // Exercise primary chapter-advance action via Next Chapter control
        composeTestRule.onNodeWithContentDescription("Next Chapter").performClick()
        assert(nextChapterClicks == 1)
    }

    @Test
    fun recoveryPath_errorCardExposesButtonSemanticsAndRetries() {
        var hasError by mutableStateOf(true)
        var retryCount = 0
        val message = formatImageLoadError(
            url = "http://example.com/p1.jpg",
            throwableMessage = "Cleartext HTTP traffic not permitted"
        )

        composeTestRule.setContent {
            PanelTheme {
                if (hasError) {
                    MangaPageErrorCard(
                        pageNumber = 1,
                        errorMessage = message,
                        onRetry = {
                            retryCount++
                            hasError = false
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Failed to load page 1").assertIsDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        ).assertIsDisplayed().performClick()

        composeTestRule.waitForIdle()
        assert(retryCount == 1)
        composeTestRule.onNodeWithText("Failed to load page 1").assertDoesNotExist()
    }

    @Test
    fun narrowWidth_highFontScale_preserves48dpTouchTargets() {
        val settingsRepo = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.WEBTOON)
        }
        val pages = listOf(SourcePage(index = 0, url = "page-1"))
        var fontScale by mutableFloatStateOf(1.3f)

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale = fontScale)
            ) {
                PanelTheme {
                    Box(modifier = Modifier.width(320.dp)) {
                        WebtoonReaderScreenContent(
                            chapterTitle = "Chapter 142: The Sovereign of Eternal Shadows and Crimson Stars",
                            pages = pages,
                            settingsRepository = settingsRepo,
                            onBack = {}
                        )
                    }
                }
            }
        }

        for (scale in listOf(1.3f, 2.0f)) {
            fontScale = scale
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Reader Settings")
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
            composeTestRule.onNodeWithContentDescription("Back")
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }
}
