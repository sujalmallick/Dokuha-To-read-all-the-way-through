package ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Core Theme Static Colors (used by ColorSchemes)
val PanelBackgroundStatic = Color(0xFF080B11)
val PanelSurfaceStatic = Color(0xFF101623)
val PanelSurfaceGlassStatic = Color(0x99101623)
val PanelSurfaceGlassHighStatic = Color(0xB3182235)
val PanelSurfaceSubtleStatic = Color(0xFF161E2E)

val TextPrimaryStatic = Color(0xFFFFFFFF)
val TextSecondaryStatic = Color(0xFF94A3B8)
val TextTertiaryStatic = Color(0xFF64748B)
val TextMutedStatic = Color(0xFF475569)

val GlassBorderStatic = Color(0x1FFFFFFF)
val GlassBorderStrongStatic = Color(0x33FFFFFF)

// Reactive Theme-Aware Color Accessors (adapts to Dark, OLED, and System Light Mode)
val PanelBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val PanelSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val PanelSurfaceGlass: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)

val PanelSurfaceGlassHigh: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)

val PanelSurfaceSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

// Primary Accents
val ElectricBlue = Color(0xFF007AFF)
val ElectricBlueGlow = Color(0x66007AFF)
val ElectricBlueLight = Color(0xFF389BFF)
val ElectricBlueDark = Color(0xFF0056B3)

// Secondary Accents & Status
val StatusOngoingGreen = Color(0xFF22C55E)
val StarGold = Color(0xFFFBBF24)
val BadgeRed = Color(0xFFEF4444)

// Neutral Text & Icons (theme-reactive for WCAG AA contrast in Light & Dark)
val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

// Borders & Dividers
val GlassBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val GlassBorderStrong: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant

val GlassBorderActive = Color(0x66007AFF)

// Reading Themes
val ReadingDarkBg = Color(0xFF0B0E14)
val ReadingSepiaBg = Color(0xFF2D241E)
val ReadingSepiaText = Color(0xFFE2D9C8)
val ReadingCreamBg = Color(0xFFF7F1E5)
val ReadingCreamText = Color(0xFF2B2620)
val ReadingOledBg = Color(0xFF000000)
val ReadingOledText = Color(0xFFD4D4D8)

// Atmospheric Gradient Cover Palettes
val CoverGradients = listOf(
    listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF030712)),
    listOf(Color(0xFF1E1B4B), Color(0xFF2E1065), Color(0xFF0F0B1E)),
    listOf(Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF061512)),
    listOf(Color(0xFF701A75), Color(0xFF4A044E), Color(0xFF19061A)),
    listOf(Color(0xFF1E3A8A), Color(0xFF172554), Color(0xFF0A0F29)),
    listOf(Color(0xFF831843), Color(0xFF500724), Color(0xFF20030E)),
    listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF042F2E))
)
