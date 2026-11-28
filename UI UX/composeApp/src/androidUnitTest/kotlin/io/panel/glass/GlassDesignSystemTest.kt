package io.panel.glass

import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.components.glass.GlassBackdropHint
import ui.components.glass.GlassDesignSystemGallery
import ui.components.glass.GlassStyle
import ui.components.glass.GlassSurface
import ui.glass.GlassQuality
import ui.theme.LocalGlassTokens
import ui.theme.PanelTheme
import ui.theme.ThemeMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w420dp-h920dp-xhdpi")
class GlassDesignSystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private data class GalleryCase(
        val themeMode: ThemeMode,
        val darkTheme: Boolean,
        val widthDp: Int,
        val fontScale: Float,
        val backdrop: BackdropType,
        val path: String
    )

    private data class ContrastCase(
        val themeMode: ThemeMode,
        val isDark: Boolean,
        val quality: GlassQuality,
        val backdrop: BackdropType
    )

    @Test
    fun galleryScreenshots_darkOledLightAnd320dpScaledFonts() {
        val cases = listOf(
            GalleryCase(ThemeMode.DARK, true, 411, 1.0f, BackdropType.BUSY, "screenshots/glass_ds_dark_busy.png"),
            GalleryCase(ThemeMode.OLED_BLACK, true, 411, 1.0f, BackdropType.DARK, "screenshots/glass_ds_oled_dark.png"),
            GalleryCase(ThemeMode.SYSTEM, false, 411, 1.0f, BackdropType.BRIGHT, "screenshots/glass_ds_light_bright.png"),
            GalleryCase(ThemeMode.DARK, true, 320, 1.3f, BackdropType.BRIGHT, "screenshots/glass_ds_320dp_1_3x.png"),
            GalleryCase(ThemeMode.DARK, true, 320, 2.0f, BackdropType.BUSY, "screenshots/glass_ds_320dp_2_0x.png")
        )
        var currentCase by mutableStateOf(cases.first())

        composeTestRule.setContent {
            val c = currentCase
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = c.fontScale)) {
                PanelTheme(themeMode = c.themeMode, darkTheme = c.darkTheme) {
                    Box(modifier = Modifier.size(width = c.widthDp.dp, height = 820.dp)) {
                        BackdropCanvas(c.backdrop)
                        GlassDesignSystemGallery(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        cases.forEach { c ->
            currentCase = c
            composeTestRule.waitForIdle()
            composeTestRule.onRoot().captureRoboImage(c.path)
        }
    }

    @Test
    fun contrastTest_balancedAndReducedMaintainAtLeast45To1OverWhiteBlackAndNoisyBackdrops() {
        val cases = buildList {
            for ((themeMode, isDark) in listOf(ThemeMode.DARK to true, ThemeMode.SYSTEM to false)) {
                for (quality in listOf(GlassQuality.BALANCED, GlassQuality.REDUCED)) {
                    for (backdrop in listOf(BackdropType.WHITE, BackdropType.BLACK, BackdropType.BUSY)) {
                        add(ContrastCase(themeMode, isDark, quality, backdrop))
                    }
                }
            }
        }

        var currentCase by mutableStateOf(cases.first())
        var expectedTextColor = Color.White
        var hostView: View? = null

        composeTestRule.setContent {
            val c = currentCase
            hostView = LocalView.current
            PanelTheme(themeMode = c.themeMode, darkTheme = c.isDark) {
                val tokens = LocalGlassTokens.current
                expectedTextColor = tokens.textOnGlass
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .testTag("contrast_target")
                ) {
                    BackdropCanvas(c.backdrop)
                    GlassSurface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 180.dp, height = 80.dp),
                        style = GlassStyle.REGULAR,
                        backdropHint = if (c.backdrop == BackdropType.WHITE || c.backdrop == BackdropType.BUSY) {
                            GlassBackdropHint.OVER_BRIGHT_ART
                        } else {
                            GlassBackdropHint.OVER_DARK
                        },
                        forcedQuality = c.quality
                    ) {
                        // Empty body so sampled center pixels reflect the composited glass surface
                    }
                }
            }
        }

        cases.forEach { c ->
            currentCase = c
            composeTestRule.waitForIdle()
            val view = requireNotNull(hostView)
            val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            composeTestRule.runOnUiThread {
                view.draw(android.graphics.Canvas(bitmap))
            }
            val cx = bitmap.width / 2
            val cy = bitmap.height / 2
            val sampledBg = Color(bitmap.getPixel(cx, cy))
            val ratio = wcagContrastRatio(expectedTextColor, sampledBg)
            assertTrue(
                ratio >= 4.5,
                "Contrast failed for $c: ratio=$ratio (< 4.5), fg=$expectedTextColor, bg=$sampledBg"
            )
        }
    }

    private enum class BackdropType { WHITE, BRIGHT, BLACK, DARK, BUSY }

    @Composable
    private fun BackdropCanvas(type: BackdropType) {
        when (type) {
            BackdropType.WHITE -> Box(Modifier.fillMaxSize().background(Color.White))
            BackdropType.BRIGHT -> Box(Modifier.fillMaxSize().background(Color(0xFFF1F5F9)))
            BackdropType.BLACK -> Box(Modifier.fillMaxSize().background(Color.Black))
            BackdropType.DARK -> Box(Modifier.fillMaxSize().background(Color(0xFF0B1120)))
            BackdropType.BUSY -> Canvas(Modifier.fillMaxSize()) {
                val cell = 24f
                val cols = (size.width / cell).toInt() + 1
                val rows = (size.height / cell).toInt() + 1
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val color = when ((r + c) % 4) {
                            0 -> Color.White
                            1 -> Color(0xFF38BDF8)
                            2 -> Color(0xFFF43F5E)
                            else -> Color(0xFF0F172A)
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(c * cell, r * cell),
                            size = androidx.compose.ui.geometry.Size(cell, cell)
                        )
                    }
                }
            }
        }
    }

    private fun wcagContrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(c: Color): Double {
        fun linear(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(c.red) + 0.7152 * linear(c.green) + 0.0722 * linear(c.blue)
    }
}
