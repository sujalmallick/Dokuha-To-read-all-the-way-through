package ui

import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_classic
import panelreader.composeapp.generated.resources.logo_glassmorphic
import ui.components.LIQUID_GLASS_LOGO_FINISH_MS
import ui.components.LIQUID_GLASS_WORDMARK_FINISH_MS
import ui.components.LiquidGlassEnding
import ui.components.LiquidGlassLogoColor
import ui.components.computeLiquidGlassFitScale
import ui.components.computeLiquidGlassFrameState
import ui.components.smoothstep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        assertEquals(1800, LIQUID_GLASS_LOGO_FINISH_MS)
        assertEquals(3300, LIQUID_GLASS_WORDMARK_FINISH_MS)
    }

    @Test
    fun testFrameState_sphereToTileMorphAndSettleAtFinalFrame() {
        val start = computeLiquidGlassFrameState(0f, LiquidGlassEnding.LOGO)
        val quarter = computeLiquidGlassFrameState(0.25f, LiquidGlassEnding.LOGO)
        val mid = computeLiquidGlassFrameState(0.5f, LiquidGlassEnding.LOGO)
        val threeQuarter = computeLiquidGlassFrameState(0.75f, LiquidGlassEnding.LOGO)
        val end = computeLiquidGlassFrameState(1f, LiquidGlassEnding.LOGO)

        // Starts circular (0.5f) and morphs to squircle tile (0.22f)
        assertEquals(0.5f, start.cornerRadiusFactor, 0.001f)
        assertTrue(quarter.cornerRadiusFactor < start.cornerRadiusFactor)
        assertEquals(0.22f, mid.cornerRadiusFactor, 0.001f)

        // Glass peaks mid-sequence and clears completely by 1.0
        assertTrue(mid.glassOpacity > 0.8f)
        assertTrue(threeQuarter.glassOpacity < mid.glassOpacity)
        assertEquals(0f, end.glassOpacity, 0.001f)

        // Final frame equals static logo at normal size (artScale = 1f, settle = 1f)
        assertEquals(1f, end.artScale, 0.001f)
        assertEquals(1f, end.settle, 0.001f)
    }

    @Test
    fun testResponsiveFitScale_shrinksOnNarrowAndShortContainers() {
        val roomy = computeLiquidGlassFitScale(widthPx = 1080f, heightPx = 1920f, baseLogoSizePx = 260f)
        val narrow = computeLiquidGlassFitScale(widthPx = 320f, heightPx = 640f, baseLogoSizePx = 260f)
        val landscapeShort = computeLiquidGlassFitScale(widthPx = 1200f, heightPx = 300f, baseLogoSizePx = 260f)

        assertEquals(1f, roomy, 0.001f)
        assertTrue(narrow < 1f)
        assertTrue(landscapeShort < 1f)
    }

    @Test
    fun testSmoothstep_clampsAndMonotonic() {
        assertEquals(0f, smoothstep(0.2f, 0.8f, 0.0f), 0.001f)
        assertEquals(0.5f, smoothstep(0.2f, 0.8f, 0.5f), 0.001f)
        assertEquals(1f, smoothstep(0.2f, 0.8f, 1.0f), 0.001f)
    }
}
