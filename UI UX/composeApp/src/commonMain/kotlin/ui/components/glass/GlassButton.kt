package ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.glass.GlassQuality
import ui.glass.GlassSurfaceContext
import ui.glass.PanelGlassState
import ui.glass.glassPress
import ui.glass.nonGlassPress
import ui.theme.LocalGlassTokens
import ui.theme.LocalReduceMotion

enum class GlassButtonVariant {
    PRIMARY,
    GLASS,
    OUTLINE,
    DESTRUCTIVE
}

/**
 * Rounded-rectangle Glass Design System button (12.dp radius, 48.dp minimum height).
 * Replaces legacy `PanelPillButton` and `FrostedGlassCapsule` button usages.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlassButtonVariant = GlassButtonVariant.GLASS,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    shape: Shape = LocalGlassTokens.current.shapeSm,
    backdropHint: GlassBackdropHint = GlassBackdropHint.AUTO,
    glassState: PanelGlassState? = null,
    forcedQuality: GlassQuality? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
) {
    val tokens = LocalGlassTokens.current
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }

    val contentColor = when (variant) {
        GlassButtonVariant.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        GlassButtonVariant.DESTRUCTIVE -> tokens.destructiveContent
        GlassButtonVariant.GLASS, GlassButtonVariant.OUTLINE -> tokens.textOnGlass
    }

    val pressMod = if (enabled && !reduceMotion) {
        if (variant == GlassButtonVariant.GLASS) {
            Modifier.glassPress(interactionSource, enabled = true, pressedScale = tokens.floatingPressScale)
        } else {
            Modifier.nonGlassPress(interactionSource, enabled = true, pressedScale = tokens.buttonPressScale)
        }
    } else {
        Modifier
    }

    val buttonContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    val baseModifier = modifier
        .defaultMinSize(minHeight = 48.dp)
        .graphicsLayer { alpha = if (enabled) 1f else tokens.disabledAlpha }
        .then(pressMod)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )

    when (variant) {
        GlassButtonVariant.GLASS -> {
            GlassSurface(
                modifier = baseModifier,
                shape = shape,
                style = GlassStyle.REGULAR,
                context = GlassSurfaceContext.STANDARD,
                backdropHint = backdropHint,
                glassState = glassState,
                forcedQuality = forcedQuality,
                contentAlignment = Alignment.Center
            ) {
                buttonContent()
            }
        }
        GlassButtonVariant.PRIMARY -> {
            Box(
                modifier = baseModifier
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                buttonContent()
            }
        }
        GlassButtonVariant.OUTLINE -> {
            Box(
                modifier = baseModifier
                    .clip(shape)
                    .background(Color.Transparent)
                    .border(tokens.borderWidth, tokens.borderMid, shape),
                contentAlignment = Alignment.Center
            ) {
                buttonContent()
            }
        }
        GlassButtonVariant.DESTRUCTIVE -> {
            Box(
                modifier = baseModifier
                    .clip(shape)
                    .background(tokens.destructiveTint)
                    .border(tokens.borderWidth, tokens.destructiveBorder, shape),
                contentAlignment = Alignment.Center
            ) {
                buttonContent()
            }
        }
    }
}

@Composable
private fun GlassSurface(
    modifier: Modifier,
    shape: Shape,
    style: GlassStyle,
    context: GlassSurfaceContext,
    backdropHint: GlassBackdropHint,
    glassState: PanelGlassState?,
    forcedQuality: GlassQuality?,
    contentAlignment: Alignment,
    content: @Composable () -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        style = style,
        context = context,
        backdropHint = backdropHint,
        glassState = glassState,
        forcedQuality = forcedQuality
    ) {
        Box(modifier = Modifier.align(contentAlignment)) {
            content()
        }
    }
}
