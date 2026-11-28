package ui.screens.splash

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ui.theme.LocalReduceMotion
import ui.theme.PanelTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SplashTickerUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splashTicker_cyclesAllFiveWordsEndsOnNovelWithConstantSlotWidth() {
        var formatIndex by mutableIntStateOf(0)

        composeTestRule.setContent {
            PanelTheme {
                val density = LocalDensity.current
                val textMeasurer = rememberTextMeasurer()
                val style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                val maxWidthDp = with(density) {
                    SplashFormats.maxOf { textMeasurer.measure(it, style).size.width }.toDp()
                }
                val heightPx = textMeasurer.measure(SplashFormats.first(), style).size.height
                val heightDp = with(density) { heightPx.toDp() }

                Row {
                    Text("not a single weak")
                    SplashTickerWordSlot(
                        formatIndex = formatIndex,
                        reduceMotion = false,
                        stopImmediately = false,
                        slotWidth = maxWidthDp,
                        slotHeight = heightDp,
                        slotHeightPx = heightPx.toFloat(),
                        wordStyle = style.copy(brush = Brush.linearGradient(listOf(Color.White, Color.LightGray)))
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("not a single weak").assertIsDisplayed()
        composeTestRule.onNodeWithText("manhua").assertIsDisplayed()
        val initialSlotWidth = composeTestRule.onNodeWithTag("splash_ticker_slot")
            .fetchSemanticsNode().size.width

        for (i in 1..SplashFormats.lastIndex) {
            formatIndex = i
            composeTestRule.waitForIdle()
            val widthAtStep = composeTestRule.onNodeWithTag("splash_ticker_slot")
                .fetchSemanticsNode().size.width
            assertEquals(initialSlotWidth, widthAtStep)
        }

        composeTestRule.onNodeWithText("novel").assertIsDisplayed()
        assertEquals(listOf("manhua", "manhwa", "manga", "comic", "novel"), SplashFormats)
        assertEquals("novel", SplashFormats.last())
    }

    @Test
    fun splashTicker_reduceMotionFinishesOnTime() {
        var finished = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                PanelTheme {
                    SplashScreenContent(
                        autoFinish = true,
                        onFinished = { finished = true }
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1_200L)
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { finished }
        assertTrue(finished)
    }
}
