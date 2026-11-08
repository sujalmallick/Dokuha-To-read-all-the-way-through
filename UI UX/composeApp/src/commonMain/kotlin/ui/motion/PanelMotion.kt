package ui.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import ui.theme.LocalReduceMotion

/**
 * Canonical motion design tokens for the Panel (読破 — Dokuha) reader app.
 *
 * Grounded in the 4 fundamental motion jobs:
 * 1. Feedback (quick: toggles, taps, tab switches)
 * 2. Change (content swap, search bar expands, transitions)
 * 3. Arrival (sheets, modals, screen reveals)
 * 4. Emphasis (rare: favorites/milestones)
 *
 * Rules:
 * - Strictly NO bounce/overshoot on navigation or indicators (dampingRatio = 1.0f).
 * - Bounce allowed only on favorite/milestone triggers (dampingRatio = 0.62f).
 * - All animated transitions must respect LocalReduceMotion by collapsing to snap() or a soft opacity fade.
 * - Physics formula: stiffness = (2 * PI / response)^2
 */
object PanelMotion {

    // === Spring Tokens ===

    /**
     * 1. Floating tab bar selection indicator sliding lens between tabs.
     * Response: 0.32s, Damping: 1.0 (critically damped, zero overshoot).
     */
    val TabIndicatorSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 385f
    )

    /**
     * 2. Inset segmented control sliding lens glass indicator.
     * Response: 0.28s, Damping: 1.0 (snappy, no bounce).
     */
    val SegmentedIndicatorSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 504f
    )

    /**
     * 3. Toggle and slider thumb travel along track.
     * Response: 0.25s, Damping: 1.0 (crisp thumb travel).
     */
    val ControlThumbSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 631f
    )

    /**
     * 4. Toggle/slider knob glass expansion/swell on touch press.
     * Response: 0.22s, Damping: 0.88 (gentle tactile swell).
     */
    val ControlPressSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.88f,
        stiffness = 815f
    )

    /**
     * 5. Bottom sheet and edit dialog presentation & dismissal.
     * Response: 0.36s, Damping: 1.0 (natural sheet descent/ascent).
     */
    val BottomSheetSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 304f
    )

    /**
     * 6. Search icon expanding into full search bar at top of Settings.
     * Response: 0.30s, Damping: 1.0.
     */
    val SearchExpandSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 438f
    )

    /**
     * 7. Cover grid item to detail view hero expansion.
     * Response: 0.38s, Damping: 1.0 (fluid screen continuity).
     */
    val SharedHeroSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 274f
    )

    /**
     * 8. Reader HUD overlay show/hide on tap.
     * Response: 0.24s, Damping: 1.0 (instant immersive reveal/hide).
     */
    val ReaderHudSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 685f
    )

    /**
     * 9. Heart / add-to-library confirmation bounce (the only bouncy element).
     * Response: 0.42s, Damping: 0.62 (delightful tactile pop).
     */
    val FavoriteBounceSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.62f,
        stiffness = 224f
    )

    // === Duration Constants (for crossfades & alpha transitions) ===
    const val DurationFeedbackMs: Int = 150
    const val DurationChangeMs: Int = 220
    const val DurationArrivalMs: Int = 280

    // === Generic Motion-Aware Spring Helper ===
    fun <T> springSpec(
        dampingRatio: Float = Spring.DampingRatioNoBouncy,
        stiffness: Float = 400f
    ): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

// === Motion-Aware Composable Spring Accessors ===

/**
 * Returns an animation spec that collapses to snap() when Reduce Motion is enabled.
 */
@Composable
fun <T> rememberPanelSpring(
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

@Composable
fun <T> rememberTabIndicatorSpring(): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) snap() else spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 385f
    )
}

@Composable
fun <T> rememberSegmentedIndicatorSpring(): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) snap() else spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 504f
    )
}

@Composable
fun <T> rememberControlThumbSpring(): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) snap() else spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 631f
    )
}

@Composable
fun <T> rememberControlPressSpring(): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) snap() else spring(
        dampingRatio = 0.88f,
        stiffness = 815f
    )
}

@Composable
fun <T> rememberBottomSheetSpring(): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) snap() else spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 304f
    )
}

@Composable
fun <T> rememberSearchExpandSpring(): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) snap() else spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 438f
    )
}

@Composable
fun <T> rememberReaderHudSpring(): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) snap() else spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 685f
    )
}

@Composable
fun <T> rememberFavoriteBounceSpring(): AnimationSpec<T> {
    val reduceMotion = LocalReduceMotion.current
    return if (reduceMotion) snap() else spring(
        dampingRatio = 0.62f,
        stiffness = 224f
    )
}

// === Tactile Feedback Helpers ===

/**
 * Triggers a light tactile tick (e.g. For segmented switches, toggles).
 */
fun triggerLightHaptic(hapticFeedback: HapticFeedback?) {
    try {
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    } catch (_: Throwable) {
        // Safe fallback on platforms or simulators without haptics
    }
}

/**
 * Triggers a firmer tactile feedback for primary actions or favorites.
 */
fun triggerPrimaryHaptic(hapticFeedback: HapticFeedback?) {
    try {
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    } catch (_: Throwable) {
        // Safe fallback on platforms or simulators without haptics
    }
}
