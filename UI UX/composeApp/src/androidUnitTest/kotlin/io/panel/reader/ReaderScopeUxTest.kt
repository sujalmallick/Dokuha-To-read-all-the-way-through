package io.panel.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import data.settings.DefaultReadingMode
import data.settings.FakeReaderPrefsRepository
import data.settings.FakeSettingsRepository
import data.settings.ReaderColorFilter
import data.settings.ReaderSettingsScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.components.ReaderScopeToggle
import ui.components.SettingLabelWithCustomBadge
import ui.screens.novelreader.NovelSettingsSheetContent
import ui.screens.webtoonreader.ReaderSettingsSheetCard
import ui.theme.PanelTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderScopeUxTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readerScopeToggleDisplaysOverrideCountAndHelperCaption() {
        var scope by mutableStateOf(ReaderSettingsScope.ALL_TITLES)

        composeRule.setContent {
            PanelTheme {
                ReaderScopeToggle(
                    scope = scope,
                    overrideCount = 2,
                    onScopeSelected = { scope = it }
                )
            }
        }

        composeRule.onNodeWithText("This title (2)").assertIsDisplayed()
        composeRule.onNodeWithText("All titles").assertIsDisplayed()
        composeRule.onNodeWithText("Updates defaults for titles without custom overrides.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("This title (2)").performClick()
        composeRule.waitForIdle()

        assertEquals(ReaderSettingsScope.THIS_TITLE, scope)
        composeRule.onNodeWithText("Changes apply only to this title.").assertIsDisplayed()
    }

    @Test
    fun settingLabelWithCustomBadgeInvokesSingleFieldReset() {
        var resetClicks = 0

        composeRule.setContent {
            PanelTheme {
                SettingLabelWithCustomBadge(
                    label = "Side Padding",
                    isCustom = true,
                    onReset = { resetClicks++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Reset Side Padding")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, resetClicks)
    }

    @Test
    fun readerSettingsShowsMaskedNoticeOnAllTitlesAndSupportsSingleFieldReset() {
        runBlocking {
        val settingsRepo = FakeSettingsRepository()
        val prefsRepo = FakeReaderPrefsRepository(settingsRepo)
        val titleKey = "manga/test-scope-ux"
        prefsRepo.updatePrefs(titleKey) {
            it.copy(
                readingMode = DefaultReadingMode.LEFT_TO_RIGHT,
                colorFilter = ReaderColorFilter.SEPIA
            )
        }

        composeRule.setContent {
            PanelTheme {
                ReaderSettingsSheetCard(
                    settingsRepository = settingsRepo,
                    titleKey = titleKey,
                    readerPrefsRepository = prefsRepo
                )
            }
        }

        composeRule.waitForIdle()

        // Opens on This title because overrides exist, showing count (2)
        composeRule.onNodeWithText("This title (2)").assertIsDisplayed()

        // Switch to All titles -> masked-override notice appears
        composeRule.onNodeWithText("All titles").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2 custom overrides for this title take priority.")
            .assertIsDisplayed()

        // Quick action switches back to This title
        composeRule.onNodeWithText("Edit this title").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Changes apply only to this title.").assertIsDisplayed()

        // Reset only Reading Mode, keeping Color Filter
        composeRule.onNodeWithContentDescription("Reset Reading Mode").performClick()
        composeRule.waitForIdle()

        val updated = prefsRepo.getPrefs(titleKey)
        assertNull(updated?.readingMode)
        assertEquals(ReaderColorFilter.SEPIA, updated?.colorFilter)
        composeRule.onNodeWithText("This title (1)").assertIsDisplayed()
        }
    }

    @Test
    fun novelSettingsSheetDefaultsToAllTitlesWhenNoOverridesExist() {
        val settingsRepo = FakeSettingsRepository()
        val prefsRepo = FakeReaderPrefsRepository(settingsRepo)

        composeRule.setContent {
            PanelTheme {
                NovelSettingsSheetContent(
                    settingsRepository = settingsRepo,
                    titleKey = "novel/default-scope",
                    readerPrefsRepository = prefsRepo
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Updates defaults for titles without custom overrides.")
            .assertIsDisplayed()
    }
}
