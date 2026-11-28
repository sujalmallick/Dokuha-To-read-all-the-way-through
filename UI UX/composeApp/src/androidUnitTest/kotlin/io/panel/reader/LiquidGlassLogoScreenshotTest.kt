package io.panel.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.jetbrains.compose.resources.painterResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_glassmorphic
import ui.components.LiquidGlassEnding
import ui.components.LiquidGlassLogoColor
import ui.components.LiquidGlassLogoInternal
import ui.theme.PanelTheme

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class LiquidGlassLogoScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun RenderLogoFrame(
        progress: Float? = null,
        reduceMotion: Boolean = false
    ) {
        PanelTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassLogoInternal(
                    logo = painterResource(Res.drawable.logo_glassmorphic),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                    ending = LiquidGlassEnding.LOGO,
                    logoColor = LiquidGlassLogoColor.ORIGINAL,
                    reduceMotion = reduceMotion,
                    progressOverride = progress
                )
            }
        }
    }

    @Test
    fun capture_liquid_glass_p000() {
        composeTestRule.setContent { RenderLogoFrame(progress = 0.0f) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_000.png")
    }

    @Test
    fun capture_liquid_glass_p025() {
        composeTestRule.setContent { RenderLogoFrame(progress = 0.25f) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_025.png")
    }

    @Test
    fun capture_liquid_glass_p050() {
        composeTestRule.setContent { RenderLogoFrame(progress = 0.50f) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_050.png")
    }

    @Test
    fun capture_liquid_glass_p075() {
        composeTestRule.setContent { RenderLogoFrame(progress = 0.75f) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_075.png")
    }

    @Test
    fun capture_liquid_glass_p100() {
        composeTestRule.setContent { RenderLogoFrame(progress = 1.0f) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_100.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun capture_liquid_glass_narrow_320dp() {
        composeTestRule.setContent { RenderLogoFrame(progress = 0.50f) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_narrow_320dp.png")
    }

    @Test
    @Config(qualifiers = "w768dp-h1024dp-xhdpi")
    fun capture_liquid_glass_ipad_768dp() {
        composeTestRule.setContent { RenderLogoFrame(progress = 0.50f) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_ipad_768dp.png")
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-xhdpi")
    fun capture_liquid_glass_landscape() {
        composeTestRule.setContent { RenderLogoFrame(progress = 0.50f) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_landscape.png")
    }

    @Test
    fun capture_liquid_glass_reduce_motion() {
        composeTestRule.setContent { RenderLogoFrame(reduceMotion = true) }
        composeTestRule.onRoot().captureRoboImage("screenshots/liquid_glass_reduce_motion.png")
    }
}
