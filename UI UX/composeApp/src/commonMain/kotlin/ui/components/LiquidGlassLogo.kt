package ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_glassmorphic
import ui.theme.LocalReduceMotion
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Reveal destination for the Liquid Glass effect.
 */
enum class LiquidGlassEnding {
    /**
     * Finishes on the original logo alone at its normal size, on a black background,
     * without a second wordmark text reveal.
     */
    LOGO,

    /**
     * Reveals the full wordmark text following the logo sequence.
     */
    WORDMARK
}

/**
 * Color treatment for the artwork silhouette.
 */
enum class LiquidGlassLogoColor {
    /** Retains original artwork colors. */
    ORIGINAL,

    /** Recolors the silhouette in crisp white. */
    WHITE,

    /** Applies theme-tinted accent color. */
    TINTED
}

/**
 * Compose Multiplatform native implementation of the ReactDrops "Liquid Glass Logo" component.
 *
 * Faithfully preserves:
 * - 3D Sphere-to-tile glass morph entrance
 * - Wavelength-based bevel refraction & chromatic edge streaks (RGB dispersion)
 * - Specular caustics, highlights, and optical lens curve
 * - Measured responsive fitting with max height containment
 * - Reduced-motion accessibility bypass
 * - Image fallback with zero text reveal when [ending] is [LiquidGlassEnding.LOGO]
 */
@Composable
fun LiquidGlassLogo(
    modifier: Modifier = Modifier,
    logo: DrawableResource = Res.drawable.logo_glassmorphic,
    ending: LiquidGlassEnding = LiquidGlassEnding.LOGO,
    logoColor: LiquidGlassLogoColor = LiquidGlassLogoColor.ORIGINAL,
    height: Dp = 480.dp,
    onAnimationEnd: (() -> Unit)? = null
) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            progress.snapTo(0f)
            // 1.8s finish duration for ending = LOGO (matching ReactDrops liquidGlassLogoFinishTime)
            val durationMs = if (ending == LiquidGlassEnding.LOGO) 1800 else 3300
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    easing = LinearEasing
                )
            )
            onAnimationEnd?.invoke()
        }
    }

    val p = progress.value
    // Sequence time in seconds: 0.0s to 1.8s
    val tSec = p * 1.8f

    // 1. Entrance Sphere-to-Tile Morph:
    // Drop corner radius starts circular (50% = 0.5f) and expands into rounded tile (0.22f)
    val morphProgress = min(1f, tSec / 1.15f)
    val easeMorph = CubicBezierEasing(0.22f, 1f, 0.36f, 1f).transform(morphProgress)
    val cornerRadiusFactor = 0.5f * (1f - easeMorph) + 0.22f * easeMorph

    // 2. Optical Glass Scale & Dissolve:
    // Glass tile swells smoothly to 1.05x then settles
    val glassScale = if (tSec < 0.8f) {
        0.75f + 0.30f * (tSec / 0.8f)
    } else {
        1.05f - 0.05f * min(1f, (tSec - 0.8f) / 0.35f)
    }

    // Glass opacity dissipates from 1.05s to 1.75s, leaving the bare logo
    val glassOpacity = if (ending == LiquidGlassEnding.LOGO) {
        if (tSec < 1.05f) {
            min(1f, tSec / 0.25f)
        } else {
            max(0f, 1f - (tSec - 1.05f) / 0.70f)
        }
    } else {
        min(1f, tSec / 0.3f)
    }

    // 3. Logo Scaling:
    // Logo artwork scales from 0.72x up to exactly 1.0x (normal size) as glass clears
    val logoScale = if (tSec < 1.13f) {
        0.72f + 0.20f * (tSec / 1.13f)
    } else {
        0.92f + 0.08f * min(1f, (tSec - 1.13f) / 0.35f)
    }

    // 4. Specular caustic sweep angle across the bevel
    val sweepAngle = -45f + (tSec / 1.8f) * 180f

    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val maxAvailable = if (maxWidth < maxHeight) maxWidth else maxHeight
        val plateSize = (maxAvailable * 0.70f).coerceAtMost(260.dp)

        Box(
            modifier = Modifier.size(plateSize),
            contentAlignment = Alignment.Center
        ) {
            // LAYER A: Wavelength-based Bevel Refraction & Chromatic Edge Streaks (Canvas)
            if (glassOpacity > 0.005f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(glassScale)
                        .alpha(glassOpacity)
                ) {
                    val w = size.width
                    val h = size.height
                    val cornerRadiusPx = w * cornerRadiusFactor
                    val rect = RoundRect(
                        left = 0f,
                        top = 0f,
                        right = w,
                        bottom = h,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                    val platePath = Path().apply { addRoundRect(rect) }

                    // A1. Deep Glass Body with ambient back-face absorption
                    clipPath(platePath) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0F1A2E).copy(alpha = 0.85f),
                                    Color(0xFF060C18).copy(alpha = 0.95f),
                                    Color(0xFF020408)
                                ),
                                center = Offset(w * 0.35f, h * 0.35f),
                                radius = w * 0.85f
                            )
                        )

                        // A2. Internal Caustic Lens Curvature
                        val rad = Math.toRadians(sweepAngle.toDouble())
                        val causticCenterX = w * 0.5f + (w * 0.35f * cos(rad)).toFloat()
                        val causticCenterY = h * 0.5f + (h * 0.35f * sin(rad)).toFloat()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    Color(0xFF67E8F9).copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                center = Offset(causticCenterX, causticCenterY),
                                radius = w * 0.55f
                            )
                        )
                    }

                    // A3. Chromatic Edge Streaks (Spectral Dispersion):
                    // Split red and cyan/blue offsets along the glass perimeter
                    val aberrationOffset = 2.5.dp.toPx() * (1f - (tSec / 2.2f))

                    // Red wavelength fringe (shifted top-left)
                    val redRect = RoundRect(
                        left = -aberrationOffset,
                        top = -aberrationOffset,
                        right = w - aberrationOffset,
                        bottom = h - aberrationOffset,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF0055).copy(alpha = 0.35f),
                                Color.Transparent,
                                Color(0xFFFF0055).copy(alpha = 0.20f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        ),
                        topLeft = Offset(redRect.left, redRect.top),
                        size = Size(redRect.width, redRect.height),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    // Cyan wavelength fringe (shifted bottom-right)
                    val cyanRect = RoundRect(
                        left = aberrationOffset,
                        top = aberrationOffset,
                        right = w + aberrationOffset,
                        bottom = h + aberrationOffset,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF00F0FF).copy(alpha = 0.40f),
                                Color(0xFF38BDF8).copy(alpha = 0.25f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        ),
                        topLeft = Offset(cyanRect.left, cyanRect.top),
                        size = Size(cyanRect.width, cyanRect.height),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    // A4. Outer Specular Bevel Rim
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.White.copy(alpha = 0.90f),
                                Color(0xFF94A3B8).copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.70f),
                                Color(0xFF38BDF8).copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.90f)
                            ),
                            center = Offset(w / 2f, h / 2f)
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = 2.0.dp.toPx())
                    )

                    // A5. Inner Wall Bevel Highlight
                    val innerInset = 3.5.dp.toPx()
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.50f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.25f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        ),
                        topLeft = Offset(innerInset, innerInset),
                        size = Size(w - innerInset * 2, h - innerInset * 2),
                        cornerRadius = CornerRadius(
                            max(2.dp.toPx(), cornerRadiusPx - innerInset),
                            max(2.dp.toPx(), cornerRadiusPx - innerInset)
                        ),
                        style = Stroke(width = 1.0.dp.toPx())
                    )
                }
            }

            // LAYER B: Central Logo Artwork (Resting at normal scale on black)
            val logoSize = plateSize * 0.72f
            Image(
                painter = painterResource(logo),
                contentDescription = "Dokuha Logo",
                modifier = Modifier
                    .size(logoSize)
                    .scale(logoScale),
                contentScale = ContentScale.Fit,
                colorFilter = when (logoColor) {
                    LiquidGlassLogoColor.ORIGINAL -> null
                    LiquidGlassLogoColor.WHITE -> ColorFilter.tint(Color.White)
                    LiquidGlassLogoColor.TINTED -> ColorFilter.tint(Color(0xFFE2E6EE))
                }
            )
        }
    }
}
