package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ui.glass.LocalReduceGlass

enum class ThemeMode {
    SYSTEM,
    DARK,
    OLED_BLACK
}

val PanelAccentColors = listOf(
    ElectricBlue,          // #007AFF
    Color(0xFFA855F7),     // Amethyst Purple
    Color(0xFFEF4444),     // Ruby Crimson
    Color(0xFF10B981),     // Emerald Green
    Color(0xFFF59E0B)      // Amber Gold
)

val LocalReduceMotion = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = TextPrimary,
    primaryContainer = ElectricBlueDark,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricBlueLight,
    onSecondary = TextPrimary,
    background = PanelBackground,
    onBackground = TextPrimary,
    surface = PanelSurface,
    onSurface = TextPrimary,
    surfaceVariant = PanelSurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderStrong
)

private val OledColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = TextPrimary,
    primaryContainer = Color(0xFF18181B),
    onPrimaryContainer = TextPrimary,
    secondary = ElectricBlueLight,
    onSecondary = TextPrimary,
    background = ReadingOledBg,
    onBackground = ReadingOledText,
    surface = ReadingOledBg,
    onSurface = ReadingOledText,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderStrong
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = ElectricBlueLight,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x1F000000),
    outlineVariant = Color(0x33000000)
)

@Composable
fun PanelTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: Color = ElectricBlue,
    reduceGlass: Boolean = false,
    reduceMotion: Boolean = false,
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val effectiveMode = if (darkTheme != null) {
        if (darkTheme) ThemeMode.DARK else ThemeMode.SYSTEM
    } else {
        themeMode
    }

    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (effectiveMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK, ThemeMode.OLED_BLACK -> true
    }
    val isOled = effectiveMode == ThemeMode.OLED_BLACK

    val baseScheme = when {
        isOled -> OledColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val finalScheme = baseScheme.copy(
        primary = accentColor,
        secondary = accentColor
    )

    CompositionLocalProvider(
        LocalPanelSpacing provides PanelSpacing(),
        LocalReduceGlass provides reduceGlass,
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = finalScheme,
            typography = PanelTypography,
            shapes = PanelShapes,
            content = content
        )
    }
}
