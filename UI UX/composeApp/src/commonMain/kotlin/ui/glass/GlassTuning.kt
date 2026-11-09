package ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lens tuning parameters for Apple iOS 26 Liquid Glass refraction.
 *
 * Exposes all 8 properties:
 * @param frost Tiny blur radius (0-2dp), kept minimal so glass remains a clear optical lens.
 * @param refraction Amount of lens distortion when sampling content behind.
 * @param curve Curvature of the lens from center to edges.
 * @param edge Specular rim lighting width at the boundary.
 * @param dispersion Chromatic aberration (RGB channel splitting through the lens).
 * @param saturation Color saturation boost of refracted content.
 * @param contrast Contrast adjustment of refracted content.
 * @param tint Subtle translucent tint applied before specular lighting.
 */
@Immutable
data class GlassTuning(
    val frost: Dp = 1.dp,
    val refraction: Float = 0.6f,
    val curve: Float = 0.6f,
    val edge: Float = 0.15f,
    val dispersion: Float = 0.15f,
    val saturation: Float = 1.15f,
    val contrast: Float = 1.0f,
    val tint: Color = Color.White.copy(alpha = 0.06f)
) {
    companion object {
        /**
         * Standard functional glass for floating bars, bottom sheets, search fields.
         */
        val Regular = GlassTuning(
            frost = 1.dp,
            refraction = 0.6f,
            curve = 0.6f,
            edge = 0.15f,
            dispersion = 0.15f,
            saturation = 1.15f,
            contrast = 1.0f,
            tint = Color.White.copy(alpha = 0.06f)
        )

        /**
         * Ultra-clear lens for floating circular buttons and controls over hero art / readers.
         */
        val Clear = GlassTuning(
            frost = 0.5.dp,
            refraction = 0.6f,
            curve = 0.6f,
            edge = 0.18f,
            dispersion = 0.15f,
            saturation = 1.15f,
            contrast = 1.0f,
            tint = Color.White.copy(alpha = 0.04f)
        )

        /**
         * Darkened glass tuning for high legibility over very bright art (15-20% dark tint).
         */
        val DarkDim = GlassTuning(
            frost = 1.dp,
            refraction = 0.6f,
            curve = 0.6f,
            edge = 0.15f,
            dispersion = 0.15f,
            saturation = 1.10f,
            contrast = 1.0f,
            tint = Color.Black.copy(alpha = 0.18f)
        )
    }
}
