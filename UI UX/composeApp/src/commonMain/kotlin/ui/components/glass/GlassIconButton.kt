package ui.components.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.glass.GlassQuality
import ui.glass.GlassSurfaceContext
import ui.glass.PanelGlassState
import ui.glass.glassPress
import ui.theme.LocalGlassTokens
import ui.theme.LocalReduceMotion

/**
 * Glass icon button enforcing a minimum 48.dp touch target and explicit [contentDescription].
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualSize: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = CircleShape,
    style: GlassStyle = GlassStyle.CLEAR,
    context: GlassSurfaceContext = GlassSurfaceContext.STANDARD,
    backdropHint: GlassBackdropHint = GlassBackdropHint.AUTO,
    tint: Color = LocalGlassTokens.current.textOnGlass,
    glassState: PanelGlassState? = null,
    forcedQuality: GlassQuality? = null
) {
    val tokens = LocalGlassTokens.current
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }

    val pressMod = if (enabled && !reduceMotion) {
        Modifier.glassPress(interactionSource, enabled = true, pressedScale = tokens.floatingPressScale)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else tokens.disabledAlpha }
            .then(pressMod)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier.requiredSize(visualSize),
            shape = shape,
            style = style,
            context = context,
            backdropHint = backdropHint,
            glassState = glassState,
            forcedQuality = forcedQuality
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier
                    .requiredSize(iconSize)
                    .align(Alignment.Center)
            )
        }
    }
}
