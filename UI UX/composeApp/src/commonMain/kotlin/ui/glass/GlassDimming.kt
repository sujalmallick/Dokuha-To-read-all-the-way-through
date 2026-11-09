package ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * 35% dark scrim placed behind Clear glass controls when rendered over bright art or reader content,
 * ensuring text and icon contrast without blurring the background.
 */
object GlassDimming {
    val DefaultColor: Color = Color.Black.copy(alpha = 0.35f)
}

/**
 * Applies a 35% dark scrim behind a glass control for contrast over bright backgrounds.
 */
fun Modifier.glassDimming(
    enabled: Boolean = true,
    alpha: Float = 0.35f,
    shape: Shape = CircleShape
): Modifier = if (enabled) {
    this.background(Color.Black.copy(alpha = alpha), shape)
} else {
    this
}
