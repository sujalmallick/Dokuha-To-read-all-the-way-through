package ui.motion

import androidx.compose.animation.core.Spring
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PanelMotionTest {

    @Test
    fun testAllIndicatorAndNavigationSpringsHaveNoOvershoot() {
        // Strict app rule: Zero overshoot on navigation and indicators
        assertEquals(Spring.DampingRatioNoBouncy, PanelMotion.TabIndicatorSpring.dampingRatio)
        assertEquals(Spring.DampingRatioNoBouncy, PanelMotion.SegmentedIndicatorSpring.dampingRatio)
        assertEquals(Spring.DampingRatioNoBouncy, PanelMotion.ControlThumbSpring.dampingRatio)
        assertEquals(Spring.DampingRatioNoBouncy, PanelMotion.BottomSheetSpring.dampingRatio)
        assertEquals(Spring.DampingRatioNoBouncy, PanelMotion.SearchExpandSpring.dampingRatio)
        assertEquals(Spring.DampingRatioNoBouncy, PanelMotion.SharedHeroSpring.dampingRatio)
        assertEquals(Spring.DampingRatioNoBouncy, PanelMotion.ReaderHudSpring.dampingRatio)

        assertTrue(PanelMotion.TabIndicatorSpring.dampingRatio >= 1.0f)
        assertTrue(PanelMotion.SegmentedIndicatorSpring.dampingRatio >= 1.0f)
        assertTrue(PanelMotion.ControlThumbSpring.dampingRatio >= 1.0f)
        assertTrue(PanelMotion.BottomSheetSpring.dampingRatio >= 1.0f)
        assertTrue(PanelMotion.SearchExpandSpring.dampingRatio >= 1.0f)
        assertTrue(PanelMotion.SharedHeroSpring.dampingRatio >= 1.0f)
        assertTrue(PanelMotion.ReaderHudSpring.dampingRatio >= 1.0f)
    }

    @Test
    fun testFavoriteBounceSpringHasControlledOvershoot() {
        // Strict app rule: Bounce is only allowed on the favorite/add-to-library heart button
        assertEquals(0.62f, PanelMotion.FavoriteBounceSpring.dampingRatio)
        assertEquals(224f, PanelMotion.FavoriteBounceSpring.stiffness)
        assertTrue(PanelMotion.FavoriteBounceSpring.dampingRatio < 1.0f)
    }

    @Test
    fun testControlPressSpringHasGentleTactileSwell() {
        assertEquals(0.88f, PanelMotion.ControlPressSpring.dampingRatio)
        assertEquals(815f, PanelMotion.ControlPressSpring.stiffness)
    }

    @Test
    fun testDurationsAreRestrained() {
        // Animations on frequent paths must be snappy and not keep user waiting (<300ms)
        assertTrue(PanelMotion.DurationFeedbackMs <= 200)
        assertTrue(PanelMotion.DurationChangeMs <= 250)
        assertTrue(PanelMotion.DurationArrivalMs <= 300)
    }
}
