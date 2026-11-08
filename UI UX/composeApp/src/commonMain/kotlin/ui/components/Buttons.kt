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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.ElectricBlueDark
import ui.theme.ElectricBlueLight
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

enum class ButtonVariant {
    PRIMARY,
    GLASS,
    OUTLINE
}

@Composable
fun PanelPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
) {
    val backgroundModifier = when (variant) {
        ButtonVariant.PRIMARY -> Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(ElectricBlue, ElectricBlueLight)
            )
        )
        ButtonVariant.GLASS -> Modifier
            .background(PanelSurfaceGlassHigh)
            .border(1.dp, GlassBorderStrong, CapsuleShape)
        ButtonVariant.OUTLINE -> Modifier
            .background(Color.Transparent)
            .border(1.dp, GlassBorder, CapsuleShape)
    }

    Box(
        modifier = modifier
            .clip(CapsuleShape)
            .then(backgroundModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
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
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PanelCircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    tint: Color = TextPrimary,
    backgroundColor: Color = PanelSurfaceGlassHigh,
    borderColor: Color = GlassBorderStrong
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Preview
@Composable
fun PanelPillButtonPreview() {
    PanelTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelPillButton(
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
