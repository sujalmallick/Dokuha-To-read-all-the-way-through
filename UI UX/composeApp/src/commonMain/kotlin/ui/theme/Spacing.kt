package ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PanelSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val default: Dp = 16.dp,
    val large: Dp = 20.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
    val extraHuge: Dp = 48.dp
)

val LocalPanelSpacing = staticCompositionLocalOf { PanelSpacing() }

val PanelSpacingTokens: PanelSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalPanelSpacing.current
