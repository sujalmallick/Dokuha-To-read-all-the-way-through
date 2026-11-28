package ui.glass

import ui.glass.internal.LiquidState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PanelGlassStateTest {

    @Test
    fun panelGlassStateDefaultsToBalancedAndNotScrolling() {
        val state = PanelGlassState(LiquidState())
        assertEquals(GlassQuality.BALANCED, state.quality)
        assertFalse(state.isScrolling)
    }

    @Test
    fun panelGlassStateTracksScrollingAndQualityTransitions() {
        val state = PanelGlassState(LiquidState(), initialQuality = GlassQuality.FULL)
        assertEquals(GlassQuality.FULL, state.quality)

        state.isScrolling = true
        assertTrue(state.isScrolling)

        state.quality = GlassQuality.REDUCED
        assertEquals(GlassQuality.REDUCED, state.quality)
    }
}
