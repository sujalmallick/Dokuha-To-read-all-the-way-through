package ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import ui.glass.GlassQuality
import ui.glass.GlassSurfaceContext
import ui.glass.GlassTuning
import ui.glass.GlassVariant
import ui.glass.LocalGlassQualitySetting
import ui.glass.LocalPlatformGlassEnvironment
import ui.glass.LocalReduceGlass
import ui.glass.PanelGlassState
import ui.glass.glassSurface
import ui.glass.resolveGlassQuality
import ui.theme.LocalGlassTokens

/**
 * Hint describing the visual backdrop behind a glass surface so contrast stays >= 4.5:1.
 */
enum class GlassBackdropHint {
    AUTO,
    OVER_DARK,
    OVER_BRIGHT_ART
}

/**
 * Semantic glass surface style selecting tint and blur tokens from [LocalGlassTokens].
 */
enum class GlassStyle {
    REGULAR,
    CLEAR,
    SHEET,
    READER_HIGH_CONTRAST
}

/**
 * Resolves the effective [GlassQuality] inside a Composable using [LocalGlassQualitySetting],
 * [LocalPlatformGlassEnvironment], and [LocalReduceGlass].
 */
@Composable
fun rememberResolvedGlassQuality(
    context: GlassSurfaceContext = GlassSurfaceContext.STANDARD,
    isScrolling: Boolean = false,
    forcedQuality: GlassQuality? = null
): GlassQuality {
    if (forcedQuality != null) return forcedQuality
    val setting = LocalGlassQualitySetting.current
    val env = LocalPlatformGlassEnvironment.current
    val inAppReduce = LocalReduceGlass.current
    return resolveGlassQuality(
        setting = setting,
        environment = env,
        context = context,
        inAppReduceGlass = inAppReduce,
        isScrolling = isScrolling
    )
}

/**
 * Core Glass Design System container.
 *
 * - [GlassQuality.FULL]: live backdrop blur (when [glassState] != null and [context] == [GlassSurfaceContext.STANDARD])
 *   plus directional 1dp specular rim and top sheen.
 * - [GlassQuality.BALANCED]: tint + 1dp top-left directional border gradient + 0.5dp inner glow + top sheen, zero live blur.
 * - [GlassQuality.REDUCED]: high-opacity translucent fill + 1dp crisp border, zero blur or sheen.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LocalGlassTokens.current.shapeMd,
    style: GlassStyle = GlassStyle.REGULAR,
    context: GlassSurfaceContext = GlassSurfaceContext.STANDARD,
    backdropHint: GlassBackdropHint = GlassBackdropHint.AUTO,
    glassState: PanelGlassState? = null,
    isScrolling: Boolean = false,
    isHoveredOrFocused: Boolean = false,
    forcedQuality: GlassQuality? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val quality = rememberResolvedGlassQuality(
        context = context,
        isScrolling = isScrolling,
        forcedQuality = forcedQuality
    )

    val baseTint = when {
        quality == GlassQuality.REDUCED -> tokens.reducedTint
        style == GlassStyle.READER_HIGH_CONTRAST -> tokens.readerOverlayTint
        style == GlassStyle.SHEET -> tokens.sheetTint
        style == GlassStyle.CLEAR -> tokens.clearTint
        else -> tokens.regularTint
    }

    val scrimColor = if (backdropHint == GlassBackdropHint.OVER_BRIGHT_ART && quality != GlassQuality.REDUCED) {
        tokens.adaptiveBrightArtScrim
    } else {
        Color.Transparent
    }

    val liveBlurModifier = if (quality == GlassQuality.FULL && glassState != null && style != GlassStyle.READER_HIGH_CONTRAST) {
        val blurDp = when (style) {
            GlassStyle.CLEAR -> tokens.clearBlur
            GlassStyle.SHEET -> tokens.sheetBlur
            else -> tokens.regularBlur
        }
        val variant = if (style == GlassStyle.CLEAR) GlassVariant.Clear else GlassVariant.Regular
        val tuning = GlassTuning(
            frost = blurDp,
            tint = baseTint
        )
        Modifier.glassSurface(
            state = glassState,
            variant = variant,
            shape = shape,
            tuning = tuning
        )
    } else {
        Modifier
    }

    val borderBoost = if (isHoveredOrFocused) tokens.hoverBorderAlphaBoost else 0f

    Box(
        modifier = modifier
            .clip(shape)
            .then(liveBlurModifier)
            .background(if (quality == GlassQuality.FULL && glassState != null) Color.Transparent else baseTint)
            .background(scrimColor)
            .drawWithCache {
                if (size.minDimension <= 0f) {
                    return@drawWithCache onDrawWithContent { drawContent() }
                }
                val outline = shape.createOutline(size, layoutDirection, this)
                val path = outline.asGlassPath()
                val sheenHeight = size.height * 0.42f
                val sheenBrush = if (quality != GlassQuality.REDUCED) {
                    Brush.verticalGradient(
                        colors = listOf(
                            tokens.sheenTopColor,
                            tokens.sheenMidColor,
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = sheenHeight
                    )
                } else {
                    null
                }
                val innerGlowBrush = if (quality != GlassQuality.REDUCED) {
                    Brush.verticalGradient(
                        colors = listOf(tokens.innerGlowColor, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.5f
                    )
                } else {
                    null
                }
                val innerGlowStroke = if (quality != GlassQuality.REDUCED) {
                    Stroke(width = tokens.innerGlowWidth.toPx())
                } else {
                    null
                }
                val topLeft = tokens.borderHighlightTopLeft.copy(
                    alpha = (tokens.borderHighlightTopLeft.alpha + borderBoost).coerceIn(0f, 1f)
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(topLeft, tokens.borderMid, tokens.borderBottomRight),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                val borderStroke = Stroke(width = tokens.borderWidth.toPx())

                onDrawWithContent {
                    drawContent()
                    if (sheenBrush != null && innerGlowBrush != null && innerGlowStroke != null) {
                        clipPath(path) {
                            drawRect(
                                brush = sheenBrush,
                                size = Size(size.width, sheenHeight)
                            )
                        }
                        clipPath(path) {
                            drawPath(
                                path = path,
                                brush = innerGlowBrush,
                                style = innerGlowStroke
                            )
                        }
                    }
                    clipPath(path) {
                        drawPath(
                            path = path,
                            brush = borderBrush,
                            style = borderStroke
                        )
                    }
                }
            },
        content = content
    )
}

private fun Outline.asGlassPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
