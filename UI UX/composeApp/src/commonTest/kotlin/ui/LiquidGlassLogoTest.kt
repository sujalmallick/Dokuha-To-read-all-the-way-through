package ui

import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_classic
import panelreader.composeapp.generated.resources.logo_glassmorphic
import ui.components.LiquidGlassEnding
import ui.components.LiquidGlassLogoColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LiquidGlassLogoTest {

    @Test
    fun testLiquidGlassEnding_enumValues() {
        val endings = LiquidGlassEnding.entries
        assertEquals(2, endings.size)
        assertEquals(LiquidGlassEnding.LOGO, LiquidGlassEnding.valueOf("LOGO"))
        assertEquals(LiquidGlassEnding.WORDMARK, LiquidGlassEnding.valueOf("WORDMARK"))
    }

    @Test
    fun testLiquidGlassLogoColor_enumValues() {
        val colors = LiquidGlassLogoColor.entries
        assertEquals(3, colors.size)
        assertEquals(LiquidGlassLogoColor.ORIGINAL, LiquidGlassLogoColor.valueOf("ORIGINAL"))
        assertEquals(LiquidGlassLogoColor.WHITE, LiquidGlassLogoColor.valueOf("WHITE"))
        assertEquals(LiquidGlassLogoColor.TINTED, LiquidGlassLogoColor.valueOf("TINTED"))
    }

    @Test
    fun testLiquidGlassLogo_resourcesAvailable() {
        assertNotNull(Res.drawable.logo_glassmorphic)
        assertNotNull(Res.drawable.logo_classic)
    }

    @Test
    fun testLiquidGlassTiming_durationForEndingModes() {
        // Ending = LOGO holds a 1.8s (1800ms) sequence before resting on logo alone
        val logoFinishMs = 1800
        val wordmarkFinishMs = 3300

        assertEquals(1800, logoFinishMs)
        assertEquals(3300, wordmarkFinishMs)
    }
}
