package ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_glassmorphic

/**
 * Example integration of the ReactDrops Liquid Glass Logo in Compose Multiplatform.
 *
 * Configured with:
 * - logo: Project's own Dokuha Glassmorphic Logo
 * - ending: [LiquidGlassEnding.LOGO] (finishes on logo alone without wordmark text reveal)
 * - logoColor: [LiquidGlassLogoColor.ORIGINAL] (preserves original artwork colors)
 * - height: 480.dp
 */
@Composable
fun Example() {
    LiquidGlassLogo(
        logo = Res.drawable.logo_glassmorphic,
        ending = LiquidGlassEnding.LOGO,
        logoColor = LiquidGlassLogoColor.ORIGINAL,
        height = 480.dp
    )
}
