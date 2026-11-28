/*!
 * ReactDrops Component License 1.0
 * Copyright (c) 2026 ReactDrops
 *
 * Ported and adapted for Dokuha (Compose Multiplatform).
 */
package ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_glassmorphic
import ui.components.liquidglass.LiquidGlassShaderUniforms
import ui.components.liquidglass.rememberLiquidGlassShaderRenderer
import ui.glass.LocalReduceGlass
import ui.theme.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
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

internal const val LIQUID_GLASS_LOGO_FINISH_MS = 1800
internal const val LIQUID_GLASS_WORDMARK_FINISH_MS = 3300

internal data class LiquidGlassFrameState(
    val progress: Float,
    val timeSec: Float,
    val cornerRadiusFactor: Float,
    val glassScale: Float,
    val glassOpacity: Float,
    val artScale: Float,
    val settle: Float,
    val sweepAngleDeg: Float
)

internal fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge1 <= edge0) return if (x >= edge1) 1f else 0f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun computeLiquidGlassFitScale(
    widthPx: Float,
    heightPx: Float,
    baseLogoSizePx: Float,
    ending: LiquidGlassEnding = LiquidGlassEnding.LOGO
): Float {
    if (widthPx <= 0f || heightPx <= 0f || baseLogoSizePx <= 0f) return 1f
    val widthDivisor = if (ending == LiquidGlassEnding.WORDMARK) baseLogoSizePx * 2.2f else baseLogoSizePx * 1.9f
    val heightDivisor = baseLogoSizePx * 1.5f
    return min(1f, min(widthPx / widthDivisor, heightPx / heightDivisor)).coerceAtLeast(0.25f)
}

internal fun computeLiquidGlassFrameState(
    progress: Float,
    ending: LiquidGlassEnding = LiquidGlassEnding.LOGO
): LiquidGlassFrameState {
    val p = progress.coerceIn(0f, 1f)
    val durationSec = if (ending == LiquidGlassEnding.LOGO) 1.8f else 3.3f
    val tSec = p * durationSec

    // Sphere (0.5f circle) -> superellipse tile (0.22f rounded rect)
    val morphT = (tSec / 0.36f).coerceIn(0f, 1f)
    val easeMorph = CubicBezierEasing(0.22f, 1f, 0.36f, 1f).transform(morphT)
    val cornerRadiusFactor = 0.5f * (1f - easeMorph) + 0.22f * easeMorph

    // Expansion with exponential settle from ReactDrops: 0.64 + 0.41 * (1 - exp(-max(t - 0.16, 0) * 5.8))
    val enter = smoothstep(0.025f, 0.16f, tSec)
    val expand = enter * (0.64f + 0.41f * (1f - exp(-max(tSec - 0.16f, 0f) * 5.8f)))
    val collapse = 1f - smoothstep(1.33f, 1.52f, tSec)
    val settle = if (ending == LiquidGlassEnding.LOGO) {
        ((tSec - 1.16f) / (1.40f - 1.16f)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val glassScale = (expand * (0.48f + 0.52f * collapse) * (1f - settle) + 1f * settle).coerceIn(0.65f, 1.06f)

    val glassOpacity = if (ending == LiquidGlassEnding.LOGO) {
        if (tSec < 1.05f) {
            (tSec / 0.25f).coerceIn(0f, 1f)
        } else {
            (1f - (tSec - 1.05f) / 0.70f).coerceIn(0f, 1f)
        }
    } else {
        (tSec / 0.30f).coerceIn(0f, 1f)
    }

    // Logo artwork settles smoothly to 1.0x (exact resting size) at 1.13s..1.28s
    val artworkSettle = smoothstep(1.13f, 1.28f, tSec)
    val earlyArtScale = 0.72f + 0.20f * (tSec / 1.13f).coerceIn(0f, 1f)
    val artScale = earlyArtScale * (1f - artworkSettle) + 1f * artworkSettle

    val sweepAngleDeg = -45f + p * 180f

    return LiquidGlassFrameState(
        progress = p,
        timeSec = tSec,
        cornerRadiusFactor = cornerRadiusFactor,
        glassScale = glassScale,
        glassOpacity = glassOpacity,
        artScale = artScale,
        settle = settle,
        sweepAngleDeg = sweepAngleDeg
    )
}

/**
 * Compose Multiplatform implementation of the ReactDrops "Liquid Glass Logo" entrance.
 *
 * - Runs a one-shot 1.8s sphere-to-tile glass entrance (`Animatable<Float>`), then swaps
 *   seamlessly to a plain static [Image] of [logo] at its normal size.
 * - Uses platform shaders (Android 13+ AGSL / iOS Skia RuntimeEffect) when available and
 *   quality tier permits, with an automatic, crash-proof Canvas fallback on older OS versions,
 *   [GlassQuality.REDUCED], or shader compile failures.
 * - Supports [reduceMotion] by jumping immediately to the final static logo and invoking [onFinished].
 */
@Composable
fun LiquidGlassLogo(
    logo: Painter,
    modifier: Modifier = Modifier,
    ending: LiquidGlassEnding = LiquidGlassEnding.LOGO,
    logoColor: LiquidGlassLogoColor = LiquidGlassLogoColor.ORIGINAL,
    reduceMotion: Boolean = LocalReduceMotion.current,
    onFinished: (() -> Unit)? = null
) {
    LiquidGlassLogoInternal(
        logo = logo,
        modifier = modifier,
        ending = ending,
        logoColor = logoColor,
        reduceMotion = reduceMotion,
        progressOverride = null,
        onFinished = onFinished
    )
}

/**
 * Convenience overload accepting a Compose Multiplatform [DrawableResource].
 */
@Composable
fun LiquidGlassLogo(
    modifier: Modifier = Modifier,
    logo: DrawableResource = Res.drawable.logo_glassmorphic,
    ending: LiquidGlassEnding = LiquidGlassEnding.LOGO,
    logoColor: LiquidGlassLogoColor = LiquidGlassLogoColor.ORIGINAL,
    height: Dp = 480.dp,
    backgroundColor: Color = Color.Black,
    reduceMotion: Boolean = LocalReduceMotion.current,
    onAnimationEnd: (() -> Unit)? = null
) {
    LiquidGlassLogo(
        logo = painterResource(logo),
        modifier = modifier
            .height(height)
            .background(backgroundColor),
        ending = ending,
        logoColor = logoColor,
        reduceMotion = reduceMotion,
        onFinished = onAnimationEnd
    )
}

@Composable
internal fun LiquidGlassLogoInternal(
    logo: Painter,
    modifier: Modifier = Modifier,
    ending: LiquidGlassEnding = LiquidGlassEnding.LOGO,
    logoColor: LiquidGlassLogoColor = LiquidGlassLogoColor.ORIGINAL,
    reduceMotion: Boolean = LocalReduceMotion.current,
    progressOverride: Float? = null,
    onFinished: (() -> Unit)? = null
) {
    val progress = remember(reduceMotion, progressOverride) {
        Animatable(if (reduceMotion) 1f else (progressOverride ?: 0f))
    }

    LaunchedEffect(reduceMotion, ending, progressOverride) {
        if (progressOverride != null) {
            progress.snapTo(progressOverride.coerceIn(0f, 1f))
            return@LaunchedEffect
        }
        if (reduceMotion) {
            progress.snapTo(1f)
            onFinished?.invoke()
        } else {
            progress.snapTo(0f)
            val durationMs = if (ending == LiquidGlassEnding.LOGO) {
                LIQUID_GLASS_LOGO_FINISH_MS
            } else {
                LIQUID_GLASS_WORDMARK_FINISH_MS
            }
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    easing = LinearEasing
                )
            )
            onFinished?.invoke()
        }
    }

    val isSettled by remember {
        derivedStateOf { (progressOverride ?: progress.value) >= 1f }
    }

    val tintFilter = remember(logoColor) {
        when (logoColor) {
            LiquidGlassLogoColor.ORIGINAL -> null
            LiquidGlassLogoColor.WHITE -> ColorFilter.tint(Color.White)
            LiquidGlassLogoColor.TINTED -> ColorFilter.tint(Color(0xFF818CF8))
        }
    }

    val reduceGlass = LocalReduceGlass.current
    val density = LocalDensity.current
    var containerSizePx by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(
        modifier = modifier.onSizeChanged { containerSizePx = it },
        contentAlignment = Alignment.Center
    ) {
        val maxAvailable = if (maxWidth < maxHeight) maxWidth else maxHeight
        val baseLogoDp = (maxAvailable * 0.82f).coerceAtMost(260.dp)
        val fitScale = computeLiquidGlassFitScale(
            widthPx = if (containerSizePx.width > 0) containerSizePx.width.toFloat() else with(density) { maxWidth.toPx() },
            heightPx = if (containerSizePx.height > 0) containerSizePx.height.toFloat() else with(density) { maxHeight.toPx() },
            baseLogoSizePx = with(density) { baseLogoDp.toPx() },
            ending = ending
        )
        val plateSize = baseLogoDp * fitScale
        val finalLogoSize = plateSize * 0.72f

        if (isSettled) {
            // Static plain Image once entrance completes (zero shader cost, exact final size)
            Box(
                modifier = Modifier.size(plateSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = logo,
                    contentDescription = null,
                    modifier = Modifier.size(finalLogoSize),
                    contentScale = ContentScale.Fit,
                    colorFilter = tintFilter
                )
            }
        } else {
            val platePx = with(density) { plateSize.toPx() }.toInt().coerceIn(64, 512)
            val dpr = density.density.coerceIn(1f, 2f)
            val rasterBitmap = remember(logo, platePx, tintFilter) {
                rasterizePainterToBitmap(
                    painter = logo,
                    sizePx = platePx,
                    colorFilter = tintFilter
                )
            }
            val shaderRenderer = if (!reduceGlass) {
                rememberLiquidGlassShaderRenderer(logoBitmap = rasterBitmap)
            } else {
                null
            }

            Box(
                modifier = Modifier.size(plateSize),
                contentAlignment = Alignment.Center
            ) {
                if (shaderRenderer != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                onDrawBehind {
                                    val live = computeLiquidGlassFrameState(
                                        progress = progressOverride ?: progress.value,
                                        ending = ending
                                    )
                                    val brush = shaderRenderer.brushForFrame(
                                        LiquidGlassShaderUniforms(
                                            canvasSize = IntSize(platePx, platePx),
                                            dpr = dpr,
                                            unitPx = platePx.toFloat(),
                                            logoBoxPx = platePx * 0.72f,
                                            timeSec = live.timeSec,
                                            settle = live.settle,
                                            artScale = live.artScale
                                        )
                                    )
                                    if (brush != null) {
                                        drawRect(brush = brush)
                                    }
                                }
                            }
                    )
                } else {
                    // Fallback: lightweight Canvas sphere-to-tile + static bevel + scaled logo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val w = size.width
                                val h = size.height
                                val baseGlowBrush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF0F1A2E).copy(alpha = 0.85f),
                                        Color(0xFF060C18).copy(alpha = 0.95f),
                                        Color(0xFF020408)
                                    ),
                                    center = Offset(w * 0.35f, h * 0.35f),
                                    radius = (w * 0.85f).coerceAtLeast(1f)
                                )
                                val rimSweepBrush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.90f),
                                        Color(0xFF94A3B8).copy(alpha = 0.30f),
                                        Color.White.copy(alpha = 0.70f),
                                        Color(0xFF38BDF8).copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.90f)
                                    ),
                                    center = Offset(w / 2f, h / 2f)
                                )
                                val innerHighlightBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.50f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.25f)
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(w, h)
                                )

                                onDrawBehind {
                                    val live = computeLiquidGlassFrameState(
                                        progress = progressOverride ?: progress.value,
                                        ending = ending
                                    )
                                    if (live.glassOpacity <= 0.005f) return@onDrawBehind

                                    val scaledW = w * live.glassScale
                                    val scaledH = h * live.glassScale
                                    val left = (w - scaledW) * 0.5f
                                    val top = (h - scaledH) * 0.5f
                                    val cornerPx = scaledW * live.cornerRadiusFactor
                                    val plateRect = RoundRect(
                                        left = left,
                                        top = top,
                                        right = left + scaledW,
                                        bottom = top + scaledH,
                                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                                    )
                                    val platePath = Path().apply { addRoundRect(plateRect) }

                                    clipPath(platePath) {
                                        drawRect(brush = baseGlowBrush, alpha = live.glassOpacity)
                                        val rad = live.sweepAngleDeg.toDouble() * (PI / 180.0)
                                        val cx = w * 0.5f + (scaledW * 0.35f * cos(rad)).toFloat()
                                        val cy = h * 0.5f + (scaledH * 0.35f * sin(rad)).toFloat()
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.38f * live.glassOpacity),
                                                    Color(0xFF67E8F9).copy(alpha = 0.18f * live.glassOpacity),
                                                    Color.Transparent
                                                ),
                                                center = Offset(cx, cy),
                                                radius = (scaledW * 0.55f).coerceAtLeast(1f)
                                            )
                                        )
                                    }

                                    val aberrationOffset = 2.2.dp.toPx() * (1f - (live.timeSec / 2.2f)).coerceIn(0f, 1f)
                                    drawRoundRect(
                                        color = Color(0xFFFF0055).copy(alpha = 0.32f * live.glassOpacity),
                                        topLeft = Offset(left - aberrationOffset, top - aberrationOffset),
                                        size = Size(scaledW, scaledH),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                                        style = Stroke(width = 1.6.dp.toPx())
                                    )
                                    drawRoundRect(
                                        color = Color(0xFF00F0FF).copy(alpha = 0.36f * live.glassOpacity),
                                        topLeft = Offset(left + aberrationOffset, top + aberrationOffset),
                                        size = Size(scaledW, scaledH),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                                        style = Stroke(width = 1.6.dp.toPx())
                                    )

                                    drawRoundRect(
                                        brush = rimSweepBrush,
                                        topLeft = Offset(left, top),
                                        size = Size(scaledW, scaledH),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                                        alpha = live.glassOpacity,
                                        style = Stroke(width = 2.0.dp.toPx())
                                    )

                                    val inset = 3.5.dp.toPx()
                                    if (scaledW > inset * 2f && scaledH > inset * 2f) {
                                        drawRoundRect(
                                            brush = innerHighlightBrush,
                                            topLeft = Offset(left + inset, top + inset),
                                            size = Size(scaledW - inset * 2f, scaledH - inset * 2f),
                                            cornerRadius = CornerRadius(
                                                max(2.dp.toPx(), cornerPx - inset),
                                                max(2.dp.toPx(), cornerPx - inset)
                                            ),
                                            alpha = live.glassOpacity,
                                            style = Stroke(width = 1.0.dp.toPx())
                                        )
                                    }
                                }
                            }
                    )

                    Image(
                        painter = logo,
                        contentDescription = null,
                        modifier = Modifier
                            .size(finalLogoSize)
                            .graphicsLayer {
                                val live = computeLiquidGlassFrameState(
                                    progress = progressOverride ?: progress.value,
                                    ending = ending
                                )
                                scaleX = live.artScale
                                scaleY = live.artScale
                            },
                        contentScale = ContentScale.Fit,
                        colorFilter = tintFilter
                    )
                }
            }
        }
    }
}

private fun rasterizePainterToBitmap(
    painter: Painter,
    sizePx: Int,
    colorFilter: ColorFilter?
): ImageBitmap? {
    if (sizePx <= 0) return null
    return runCatching {
        val bitmap = ImageBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(sizePx.toFloat(), sizePx.toFloat())
        ) {
            with(painter) {
                draw(size = Size(sizePx.toFloat(), sizePx.toFloat()), colorFilter = colorFilter)
            }
        }
        bitmap
    }.getOrNull()
}
