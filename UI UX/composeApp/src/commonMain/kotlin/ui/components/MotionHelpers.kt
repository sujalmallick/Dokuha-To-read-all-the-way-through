package ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import ui.motion.PanelMotion
import ui.motion.rememberControlThumbSpring
import ui.motion.rememberSegmentedIndicatorSpring
import ui.theme.LocalReduceMotion

/**
 * Standard spring spec for UI controls (toggles, sliders, buttons).
 */
fun <T> panelSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = 400f
): AnimationSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

/**
 * Strict no-overshoot spring for segmented indicators and sheets.
 */
fun <T> noOvershootSpring(
    stiffness: Float = 504f
): AnimationSpec<T> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = stiffness
)

/**
 * Returns an animation spec that instantly snaps if Reduce Motion is enabled.
 */
@Composable
fun <T> rememberMotionAwareSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = 400f
): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) {
        snap()
    } else {
        spring(dampingRatio = dampingRatio, stiffness = stiffness)
    }
}

/**
 * Returns a strict no-overshoot animation spec honoring Reduce Motion.
 */
@Composable
fun <T> rememberNoOvershootSpring(
    stiffness: Float = 504f
): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) {
        snap()
    } else {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness)
    }
}

/**
 * Executes a light tactile haptic tick if available.
 */
fun triggerLightHaptic(hapticFeedback: HapticFeedback?) {
    ui.motion.triggerLightHaptic(hapticFeedback)
}
