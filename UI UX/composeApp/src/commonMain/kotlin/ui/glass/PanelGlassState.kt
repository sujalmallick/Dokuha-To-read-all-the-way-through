package ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import ui.glass.internal.LiquidState
import ui.glass.internal.rememberLiquidState

/**
 * State manager wrapping the underlying liquid glass engine.
 * One instance should be hoisted per screen to connect the background
 * source content ([Modifier.glassSource]) to glass overlays ([Modifier.glassSurface]).
 */
@Stable
class PanelGlassState internal constructor(
    internal val liquidState: LiquidState
)

/**
 * Creates and remembers a [PanelGlassState] instance surviving recomposition.
 */
@Composable
fun rememberPanelGlassState(): PanelGlassState {
    val liquidState = rememberLiquidState()
    return remember(liquidState) { PanelGlassState(liquidState) }
}
