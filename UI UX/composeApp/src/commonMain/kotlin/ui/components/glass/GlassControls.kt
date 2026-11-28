package ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.glass.GlassQuality
import ui.glass.GlassSurfaceContext
import ui.theme.LocalGlassTokens

/**
 * Glass-themed slider using Material3 [Slider] with token-driven track/thumb colors.
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val tokens = LocalGlassTokens.current
    val accent = MaterialTheme.colorScheme.primary
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = tokens.subtextOnGlass.copy(alpha = 0.25f),
            disabledThumbColor = tokens.subtextOnGlass.copy(alpha = tokens.disabledAlpha),
            disabledActiveTrackColor = tokens.subtextOnGlass.copy(alpha = 0.2f)
        )
    )
}

/**
 * Glass-backed SnackbarHost for consistent notifications across screens.
 */
@Composable
fun GlassSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    forcedQuality: GlassQuality? = null
) {
    val tokens = LocalGlassTokens.current
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data: SnackbarData ->
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = tokens.shapeSm,
                style = GlassStyle.SHEET,
                context = GlassSurfaceContext.STANDARD,
                forcedQuality = forcedQuality
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = data.visuals.message,
                        color = tokens.textOnGlass,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    data.visuals.actionLabel?.let { actionLabel ->
                        TextButton(onClick = { data.performAction() }) {
                            Text(
                                text = actionLabel,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
