package ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for all glass design system tokens (Dark, OLED, Light).
 */
@Immutable
data class GlassTokens(
    val isDark: Boolean,
    // Blur radii (used only in FULL tier)
    val regularBlur: Dp = 12.dp,
    val clearBlur: Dp = 6.dp,
    val sheetBlur: Dp = 20.dp,
    // Surface tints
    val regularTint: Color,
    val clearTint: Color,
    val sheetTint: Color,
    val reducedTint: Color,
    val readerOverlayTint: Color,
    // Borders & inner highlights
    val borderWidth: Dp = 1.dp,
    val innerGlowWidth: Dp = 0.5.dp,
    val borderHighlightTopLeft: Color,
    val borderMid: Color,
    val borderBottomRight: Color,
    val innerGlowColor: Color,
    val sheenTopColor: Color,
    val sheenMidColor: Color,
    // Elevations
    val chipElevation: Dp = 2.dp,
    val cardElevation: Dp = 6.dp,
    val barElevation: Dp = 10.dp,
    val sheetElevation: Dp = 14.dp,
    // Corner radii (CapsuleShape is reserved exclusively for the floating bottom dock)
    val radiusXs: Dp = 8.dp,
    val radiusSm: Dp = 12.dp,
    val radiusMd: Dp = 16.dp,
    val radiusLg: Dp = 24.dp,
    // Interaction states
    val floatingPressScale: Float = 1.03f,
    val buttonPressScale: Float = 0.97f,
    val disabledAlpha: Float = 0.45f,
    val hoverBorderAlphaBoost: Float = 0.20f,
    // Contrast-safe text & adaptive scrims
    val adaptiveBrightArtScrim: Color,
    val textOnGlass: Color,
    val subtextOnGlass: Color,
    val destructiveTint: Color = Color(0x2EEF4444),
    val destructiveBorder: Color = Color(0x66EF4444),
    val destructiveContent: Color = Color(0xFFFCA5A5)
) {
    val shapeXs: RoundedCornerShape get() = RoundedCornerShape(radiusXs)
    val shapeSm: RoundedCornerShape get() = RoundedCornerShape(radiusSm)
    val shapeMd: RoundedCornerShape get() = RoundedCornerShape(radiusMd)
    val shapeLg: RoundedCornerShape get() = RoundedCornerShape(radiusLg)
}

val DarkGlassTokens = GlassTokens(
    isDark = true,
    regularTint = Color(0x14FFFFFF),      // ~8% crystal-clear iOS Liquid Glass tint
    clearTint = Color(0x0AFFFFFF),        // ~4% ultra-clear optical lens
    sheetTint = Color(0xD9111622),        // ~85% sheet glass for readable modal content
    reducedTint = Color(0xEB101623),      // ~92% opaque translucent surface (REDUCED tier)
    readerOverlayTint = Color(0xD10B0F18),// ~82% high-contrast dark glass for reader overlays
    borderHighlightTopLeft = Color(0x80FFFFFF), // 50% specular top-left
    borderMid = Color(0x28FFFFFF),              // 16% side transition
    borderBottomRight = Color(0x45FFFFFF),      // 27% ground bounce
    innerGlowColor = Color(0x38FFFFFF),         // 22% top inner caustic
    sheenTopColor = Color(0x22FFFFFF),          // 13% top sheen
    sheenMidColor = Color(0x08FFFFFF),          // 3% mid sheen
    adaptiveBrightArtScrim = Color(0x94000000), // 58% adaptive scrim when explicitly over bright/white art
    textOnGlass = Color(0xFFF8FAFC),
    subtextOnGlass = Color(0xFFCBD5E1)
)

val OledGlassTokens = GlassTokens(
    isDark = true,
    regularTint = Color(0x12FFFFFF),      // ~7% crystal-clear OLED Liquid Glass
    clearTint = Color(0x08FFFFFF),        // ~3% ultra-clear optical lens
    sheetTint = Color(0xE00A0C12),        // ~88% sheet glass
    reducedTint = Color(0xF20A0C12),      // ~95% opaque translucent surface (REDUCED tier)
    readerOverlayTint = Color(0xD907090E),// ~85% high-contrast reader glass
    borderHighlightTopLeft = Color(0x73FFFFFF),
    borderMid = Color(0x24FFFFFF),
    borderBottomRight = Color(0x3DFFFFFF),
    innerGlowColor = Color(0x30FFFFFF),
    sheenTopColor = Color(0x1FFFFFFF),
    sheenMidColor = Color(0x06FFFFFF),
    adaptiveBrightArtScrim = Color(0x99000000),
    textOnGlass = Color(0xFFF8FAFC),
    subtextOnGlass = Color(0xFFCBD5E1)
)

val LightGlassTokens = GlassTokens(
    isDark = false,
    regularTint = Color(0xD9F8FAFC),      // ~85% frosted light glass
    clearTint = Color(0xB8F8FAFC),        // ~72% clear light lens
    sheetTint = Color(0xEDF8FAFC),        // ~93% light sheet
    reducedTint = Color(0xF2FFFFFF),      // ~95% opaque white surface
    readerOverlayTint = Color(0xD10B0F18),// Reader overlays stay high-contrast dark over artwork
    borderHighlightTopLeft = Color(0xCCFFFFFF), // 80% bright white top-left highlight
    borderMid = Color(0x290F172A),              // 16% slate border
    borderBottomRight = Color(0x3D0F172A),      // 24% subtle shadow rim
    innerGlowColor = Color(0x66FFFFFF),
    sheenTopColor = Color(0x38FFFFFF),
    sheenMidColor = Color(0x12FFFFFF),
    adaptiveBrightArtScrim = Color(0x52000000),
    textOnGlass = Color(0xFF0F172A),
    subtextOnGlass = Color(0xFF334155),
    destructiveTint = Color(0x1FEF4444),
    destructiveBorder = Color(0x66DC2626),
    destructiveContent = Color(0xFFB91C1C)
)

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }
