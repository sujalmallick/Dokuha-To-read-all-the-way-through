package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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

@Composable
fun PanelTheme(
    darkTheme: Boolean = true, // Panel is custom dark navy by design
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPanelSpacing provides PanelSpacing()
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = PanelTypography,
            shapes = PanelShapes,
            content = content
        )
    }
}
