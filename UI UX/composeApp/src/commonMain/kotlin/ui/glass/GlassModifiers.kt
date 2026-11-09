package ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import ui.glass.internal.liquefiable
import ui.glass.internal.liquid

/**
 * Composition local to force fallback glass everywhere (accessibility / low power mode).
 */
val LocalReduceGlass = compositionLocalOf { false }

/**
 * Marks this composable/layer as the content to be sampled by [glassSurface].
 *
 * HARD RULE: A [glassSurface] must NEVER be a descendant of a [glassSource] using the same state.
 */
fun Modifier.glassSource(state: PanelGlassState): Modifier {
    return this.liquefiable(state.liquidState)
}

/**
 * Applies the Apple iOS 26 Liquid Glass lens effect to this surface.
 *
 * Features:
 * - Refraction, curve, and chromatic dispersion via GPU shader
 * - Specular rim lighting at the perimeter
 * - Minimal frost blur (0-2dp)
 * - Android 12 and lower fallback: clear tinted fill + thin highlight rim
 * - [LocalReduceGlass] fallback support
 */
@Composable
fun Modifier.glassSurface(
    state: PanelGlassState? = null,
    variant: GlassVariant = GlassVariant.Regular,
    shape: Shape = CircleShape,
    tuning: GlassTuning? = null
): Modifier {
    val reduceGlass = LocalReduceGlass.current
    val effectiveTuning = tuning ?: when (variant) {
        GlassVariant.Regular -> GlassTuning.Regular
        GlassVariant.Clear -> GlassTuning.Clear
    }

    return if (reduceGlass || state == null) {
        this
            .clip(shape)
            .background(effectiveTuning.tint)
            .border(0.5.dp, Color.White.copy(alpha = 0.20f), shape)
    } else {
        this.liquid(state.liquidState) {
            this.frost = effectiveTuning.frost
            this.refraction = effectiveTuning.refraction
            this.curve = effectiveTuning.curve
            this.edge = effectiveTuning.edge
            this.dispersion = effectiveTuning.dispersion
            this.saturation = effectiveTuning.saturation
            this.contrast = effectiveTuning.contrast
            this.tint = effectiveTuning.tint
            this.shape = shape
        }
    }
}

/**
 * Modern spring press motion for glass controls:
 * Swells slightly on press (~1.04) with no overshoot.
 */
@Composable
fun Modifier.glassPress(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 1.04f
): Modifier {
    if (!enabled) return this
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 380f
        ),
        label = "glassPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Modern spring press motion for non-glass tappables:
 * Scales down to 0.97 on press with no overshoot.
 */
@Composable
fun Modifier.nonGlassPress(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f
): Modifier {
    if (!enabled) return this
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 380f
        ),
        label = "nonGlassPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
