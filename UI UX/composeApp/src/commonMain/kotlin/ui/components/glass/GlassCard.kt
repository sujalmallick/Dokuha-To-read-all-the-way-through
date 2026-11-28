package ui.components.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import ui.glass.GlassQuality
import ui.glass.GlassSurfaceContext
import ui.glass.PanelGlassState
import ui.glass.nonGlassPress
import ui.theme.LocalGlassTokens
import ui.theme.LocalReduceMotion

/**
 * Rounded-rectangle glass card using [GlassTokens.shapeMd] (16.dp).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = LocalGlassTokens.current.shapeMd,
    style: GlassStyle = GlassStyle.REGULAR,
    context: GlassSurfaceContext = GlassSurfaceContext.SCROLLING_LIST,
    backdropHint: GlassBackdropHint = GlassBackdropHint.AUTO,
    glassState: PanelGlassState? = null,
    forcedQuality: GlassQuality? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }

    val pressModifier = if (onClick != null && enabled && !reduceMotion) {
        Modifier.nonGlassPress(
            interactionSource = interactionSource,
            enabled = true,
            pressedScale = tokens.buttonPressScale
        )
    } else {
        Modifier
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    GlassSurface(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else tokens.disabledAlpha }
            .then(pressModifier)
            .then(clickModifier),
        shape = shape,
        style = style,
        context = context,
        backdropHint = backdropHint,
        glassState = glassState,
        forcedQuality = forcedQuality,
        content = content
    )
}
