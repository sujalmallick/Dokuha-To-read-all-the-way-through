package io.panel.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.components.glass.GlassDesignSystemGallery
import ui.components.glass.GlassIconButton
import ui.glass.GlassQuality
import ui.glass.GlassQualitySetting
import ui.glass.GlassSurfaceContext
import ui.glass.LocalGlassQualitySetting
import ui.glass.LocalPlatformGlassEnvironment
import ui.glass.PlatformGlassEnvironment
import ui.glass.resolveGlassQuality
import ui.screens.settings.SettingsScreenContent
import ui.theme.PanelTheme
import ui.theme.ThemeMode
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w420dp-h920dp-xhdpi")
class GlassPerfAndA11yTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun resolveGlassQuality_clampsScrollingReadersLowPowerAndPreAndroid12() {
        val android33 = PlatformGlassEnvironment(isIos = false, androidSdkInt = 33)
        val android29 = PlatformGlassEnvironment(isIos = false, androidSdkInt = 29)
        val lowPower = PlatformGlassEnvironment(isIos = false, androidSdkInt = 33, isLowPowerMode = true)
        val reduceTransparency = PlatformGlassEnvironment(isIos = true, platformReduceTransparency = true)

        assertEquals(GlassQuality.FULL, resolveGlassQuality(GlassQualitySetting.AUTO, android33, GlassSurfaceContext.STANDARD))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(GlassQualitySetting.AUTO, android33, GlassSurfaceContext.SCROLLING_LIST))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(GlassQualitySetting.AUTO, android33, GlassSurfaceContext.READER_OVERLAY))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(GlassQualitySetting.AUTO, android33, GlassSurfaceContext.STANDARD, isScrolling = true))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(GlassQualitySetting.AUTO, android29, GlassSurfaceContext.STANDARD))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(GlassQualitySetting.AUTO, lowPower, GlassSurfaceContext.STANDARD))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(GlassQualitySetting.FULL, android33, GlassSurfaceContext.SCROLLING_LIST))
        assertEquals(GlassQuality.REDUCED, resolveGlassQuality(GlassQualitySetting.FULL, reduceTransparency, GlassSurfaceContext.STANDARD))
        assertEquals(GlassQuality.REDUCED, resolveGlassQuality(GlassQualitySetting.AUTO, android33, GlassSurfaceContext.STANDARD, inAppReduceGlass = true))
    }

    @Test
    fun glassIconButton_enforces48dpMinTouchTargetAndContentDescription() {
        composeTestRule.setContent {
            PanelTheme(themeMode = ThemeMode.DARK) {
                GlassIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Search library",
                    visualSize = 36.dp,
                    onClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Search library")
            .assertExists()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun reducedGlassAndReduceMotion_renderCleanlyAndCaptureScreenshot() {
        composeTestRule.setContent {
            PanelTheme(
                themeMode = ThemeMode.DARK,
                darkTheme = true,
                glassQualitySetting = GlassQualitySetting.REDUCED,
                platformGlassEnvironment = PlatformGlassEnvironment(
                    isIos = false,
                    androidSdkInt = 29,
                    isLowPowerMode = true,
                    platformReduceTransparency = true
                ),
                reduceMotion = true
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 411.dp, height = 820.dp)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    GlassDesignSystemGallery(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("screenshots/pr4_reduced_glass_dark.png")
    }

    @Test
    fun settingsSectionHeaders_renderMixedCaseNotAllCaps() {
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
        composeTestRule.onNodeWithText("Experience").assertExists()
        composeTestRule.onNodeWithText("Content & Storage").assertExists()
        composeTestRule.onNodeWithText("Application").assertExists()
        composeTestRule.onRoot().captureRoboImage("screenshots/pr4_settings_mixed_case_light.png")
    }
}
