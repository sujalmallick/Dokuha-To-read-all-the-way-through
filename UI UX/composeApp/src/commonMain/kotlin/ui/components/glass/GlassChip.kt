package ui.components.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.glass.GlassQuality
import ui.glass.GlassSurfaceContext
import ui.glass.nonGlassPress
import ui.theme.LocalGlassTokens
import ui.theme.LocalReduceMotion

/**
 * Rounded-rectangle Glass Chip (12.dp radius; never CapsuleShape).
 */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    forcedQuality: GlassQuality? = null
) {
    val tokens = LocalGlassTokens.current
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = tokens.shapeSm

    val pressMod = if (enabled && !reduceMotion) {
        Modifier.nonGlassPress(interactionSource, enabled = true, pressedScale = tokens.buttonPressScale)
    } else {
        Modifier
    }

    val accent = MaterialTheme.colorScheme.primary
    val contentColor = tokens.textOnGlass

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .then(pressMod)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = shape,
            style = GlassStyle.REGULAR,
            context = GlassSurfaceContext.SCROLLING_LIST,
            forcedQuality = forcedQuality,
            modifier = Modifier
                .then(
                    if (selected) {
                        Modifier
                            .background(accent.copy(alpha = 0.16f), shape)
                            .border(tokens.borderWidth, accent.copy(alpha = 0.55f), shape)
                    } else {
                        Modifier
                    }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Rounded-rectangle Glass Segmented Control with sliding indicator (12.dp outer, 10.dp indicator).
 */
@Composable
fun <T> GlassSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier,
    forcedQuality: GlassQuality? = null
) {
    if (options.isEmpty()) return
    val tokens = LocalGlassTokens.current
    val reduceMotion = LocalReduceMotion.current
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val outerShape = tokens.shapeSm
    val indicatorShape = RoundedCornerShape(10.dp)

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = outerShape,
        style = GlassStyle.REGULAR,
        context = GlassSurfaceContext.STANDARD,
        forcedQuality = forcedQuality
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val segmentWidth = maxWidth / options.size
            val offsetX by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.85f, stiffness = 420f),
                label = "segmentedOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = offsetX)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(indicatorShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                    .border(tokens.borderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), indicatorShape)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(indicatorShape)
                            .clickable { onSelect(option) }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelOf(option),
                            color = if (isSelected) tokens.textOnGlass else tokens.subtextOnGlass,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
