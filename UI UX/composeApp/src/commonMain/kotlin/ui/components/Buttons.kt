package ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.glass.GlassVariant
import ui.glass.PanelGlassState
import ui.glass.glassDimming
import ui.glass.glassPress
import ui.glass.glassSurface
import ui.glass.nonGlassPress
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextPrimary

enum class ButtonVariant {
    PRIMARY,
    GLASS,
    OUTLINE
}

val PanelButtonShape = RoundedCornerShape(12.dp)

/**
 * Modern rounded-rectangle action button with 12.dp corner radius.
 * - Crisp Luxe White fill with dark charcoal ink on PRIMARY (professional minimalist luxury)
 * - Liquid glass lens on GLASS variant
 * - Modern spring press feedback
 */
@Composable
fun PanelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    glassState: PanelGlassState? = null,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }

    val surfaceModifier = when (variant) {
        ButtonVariant.PRIMARY -> Modifier
            .nonGlassPress(interactionSource)
            .clip(PanelButtonShape)
            .background(Color.White)
        ButtonVariant.GLASS -> Modifier
            .glassPress(interactionSource)
            .glassSurface(
                state = glassState,
                variant = GlassVariant.Regular,
                shape = PanelButtonShape
            )
        ButtonVariant.OUTLINE -> Modifier
            .nonGlassPress(interactionSource)
            .clip(PanelButtonShape)
            .background(Color.Transparent)
            .border(1.dp, GlassBorder, PanelButtonShape)
    }

    val contentColor = when (variant) {
        ButtonVariant.PRIMARY -> Color(0xFF090D15)
        else -> TextPrimary
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .then(surfaceModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
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
                textAlign = TextAlign.Center,
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
}

@Deprecated(
    message = "Use PanelButton with standard 12.dp rounded corners instead",
    replaceWith = ReplaceWith("PanelButton(text, onClick, modifier, variant, glassState, icon, trailingIcon, contentPadding)")
)
@Composable
fun PanelPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    glassState: PanelGlassState? = null,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
) {
    PanelButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        glassState = glassState,
        icon = icon,
        trailingIcon = trailingIcon,
        contentPadding = contentPadding
    )
}

/**
 * Circular glass icon button with specular rim lighting and gentle swell press feedback.
 */
@Composable
fun PanelCircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    glassState: PanelGlassState? = null,
    hasDimming: Boolean = true,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    tint: Color = TextPrimary,
    backgroundColor: Color = PanelSurfaceGlassHigh,
    borderColor: Color = GlassBorderStrong
) {
    val interactionSource = remember { MutableInteractionSource() }

    val surfaceModifier = if (glassState != null) {
        Modifier
            .glassPress(interactionSource)
            .glassDimming(enabled = hasDimming, alpha = 0.35f, shape = CircleShape)
            .glassSurface(
                state = glassState,
                variant = GlassVariant.Clear,
                shape = CircleShape
            )
    } else {
        Modifier
            .nonGlassPress(interactionSource)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .then(surfaceModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Preview
@Composable
fun PanelButtonPreview() {
    PanelTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelButton(
                text = "Get Started",
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = {}
            )
            PanelCircleIconButton(
                icon = Icons.Default.BookmarkBorder,
                onClick = {}
            )
        }
    }
}
