package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.GlassCardShape
import ui.theme.PanelSurfaceGlass
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    backgroundColor: Color = PanelSurfaceGlass,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    hazeState: HazeState? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val hazeModifier = if (hazeState != null) {
        Modifier.hazeChild(state = hazeState)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(hazeModifier)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        content = content
    )
}

@Composable
fun FrostedGlassCapsule(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    GlassContainer(
        modifier = modifier.then(clickableModifier),
        shape = CapsuleShape,
        backgroundColor = PanelSurfaceGlassHigh,
        borderColor = GlassBorderStrong,
        hazeState = hazeState,
        content = content
    )
}

@Preview
@Composable
fun GlassContainerPreview() {
    PanelTheme {
        GlassContainer(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Glass Surface Preview",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
