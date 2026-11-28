package ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.glass.GlassQuality
import ui.glass.GlassSurfaceContext
import ui.glass.PanelGlassState
import ui.theme.CapsuleShape
import ui.theme.LocalGlassTokens

/**
 * Standardized rounded-rectangle Glass Top Bar.
 */
@Composable
fun GlassTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    glassState: PanelGlassState? = null,
    context: GlassSurfaceContext = GlassSurfaceContext.STANDARD,
    forcedQuality: GlassQuality? = null,
    shape: Shape = LocalGlassTokens.current.shapeMd,
    trailingActions: @Composable RowScope.() -> Unit = {}
) {
    val tokens = LocalGlassTokens.current
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = shape,
        style = GlassStyle.REGULAR,
        context = context,
        glassState = glassState,
        forcedQuality = forcedQuality
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    style = GlassStyle.CLEAR,
                    backdropHint = GlassBackdropHint.AUTO,
                    glassState = null,
                    forcedQuality = GlassQuality.BALANCED
                )
            }
            Text(
                text = title,
                color = tokens.textOnGlass,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (onBack == null) 8.dp else 0.dp)
            )
            trailingActions()
        }
    }
}

/**
 * Floating bottom navigation dock.
 * This is the ONLY component in the design system that uses [CapsuleShape].
 */
@Composable
fun GlassBottomDock(
    modifier: Modifier = Modifier,
    glassState: PanelGlassState? = null,
    forcedQuality: GlassQuality? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            shape = CapsuleShape,
            style = GlassStyle.REGULAR,
            context = GlassSurfaceContext.STANDARD,
            glassState = glassState,
            forcedQuality = forcedQuality,
            content = content
        )
    }
}
