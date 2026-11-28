package ui.glass

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.glass.internal.LiquidState
import ui.glass.internal.rememberLiquidState

/**
 * Rendering quality tier for Liquid Glass surfaces.
 */
enum class GlassQuality {
    FULL,
    BALANCED,
    REDUCED
}

/**
 * State manager wrapping the underlying liquid glass engine.
 * One instance should be hoisted per screen to connect the background
 * source content ([Modifier.glassSource]) to glass overlays ([Modifier.glassSurface]).
 */
@Stable
class PanelGlassState internal constructor(
    internal val liquidState: LiquidState,
    initialQuality: GlassQuality = GlassQuality.BALANCED
) {
    /**
     * Tracks whether the primary scrollable content behind this glass layer is actively scrolling.
     * Read inside [Modifier.glassSurface] draw blocks so scroll transitions only invalidate draw
     * instead of recomposing or rebuilding modifier nodes.
     */
    var isScrolling: Boolean by mutableStateOf(false)

    var quality: GlassQuality by mutableStateOf(initialQuality)
}

/**
 * Creates and remembers a [PanelGlassState] instance surviving recomposition.
 */
@Composable
fun rememberPanelGlassState(
    quality: GlassQuality = GlassQuality.BALANCED
): PanelGlassState {
    val liquidState = rememberLiquidState()
    return remember(liquidState) { PanelGlassState(liquidState, quality) }
}

/**
 * Syncs [ScrollableState.isScrollInProgress] into [PanelGlassState.isScrolling] so glass surfaces
 * can drop expensive blur/dispersion passes during active scroll without recomposing.
 */
@Composable
fun BindGlassScrollState(
    glassState: PanelGlassState,
    scrollableState: ScrollableState
) {
    LaunchedEffect(glassState, scrollableState.isScrollInProgress) {
        glassState.isScrolling = scrollableState.isScrollInProgress
    }
    DisposableEffect(glassState) {
        onDispose { glassState.isScrolling = false }
    }
}
