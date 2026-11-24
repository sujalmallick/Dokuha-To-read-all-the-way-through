package ui.glass.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp

internal class LiquidBackupElement(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit,
) : AbstractLiquidElement<LiquidBackupNode>(liquidState, block) {
  override fun create() = LiquidBackupNode(liquidState, block)
}

internal class LiquidBackupNode(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit,
) : AbstractLiquidNode(liquidState, block) {
  private var cachedColorFilter: ColorFilter? = null

  override val invalidateFlags: Int = Fields.PreTiramisuInvalidateFlags
  override val renderEffectFlags: Int = Fields.Frost

  override fun inspectDirtyFields() = with(reusableScope) {
    if (mutatedFields has (Fields.Saturation or Fields.Contrast)) {
      cachedColorFilter = createColorFilter(
        saturation = saturation,
        contrast = contrast,
      )
    }
  }

  override fun ContentDrawScope.applyAdditionalEffects(
    layer: GraphicsLayer,
    drawBlock: () -> Unit,
  ) {
    val shapeOutline = reusableScope.shape.createOutline(size, layoutDirection, this)
    val shapePath = shapeOutline.asPath()
    layer.colorFilter = cachedColorFilter

    clipPath(shapePath) { drawBlock() }

    if (reusableScope.tint.isSpecified) {
      drawOutline(
        outline = shapeOutline,
        color = reusableScope.tint,
        style = Fill,
      )
    }

    // Curvature sheen across the glass face
    drawLiquidGlassSheen(shapePath)

    // Directional specular rim and caustic depth bevel
    if (reusableScope.edge > 0f) {
      drawLiquidGlassEdgeEffect(shapePath)
    }
  }

  override fun createRenderEffect(): RenderEffect? = with(reusableScope) {
    if (frostRadius < 1f || size.isUnspecified) return null

    // Clamp blur to preserve optical clarity (avoiding cloudy, milky "frost" look)
    val effectiveRadius = frostRadius.coerceIn(1f, 18f)
    return try {
      BlurEffect(
        radiusX = effectiveRadius,
        radiusY = effectiveRadius,
        edgeTreatment = TileMode.Clamp,
      )
    } catch (_: Throwable) {
      null
    }
  }

  private fun createColorFilter(
    saturation: Float,
    contrast: Float,
  ): ColorFilter? = when {
    saturation == 1f && contrast == 1f -> {
      // Subtle glass vibrancy elevation for crystal optical depth
      val compositeMatrix = ColorMatrix().apply {
        setToSaturation(1.10f)
        timesAssign(ColorMatrix().apply { setContrast(1.04f) })
      }
      ColorFilter.colorMatrix(compositeMatrix)
    }
    else -> {
      val compositeMatrix = ColorMatrix().apply {
        setToSaturation((saturation * 1.10f).coerceAtLeast(0f))
      }
      compositeMatrix.timesAssign(ColorMatrix().apply { setContrast((contrast * 1.04f).coerceAtLeast(0f)) })
      ColorFilter.colorMatrix(compositeMatrix)
    }
  }
}

private fun Outline.asPath(): Path = when (this) {
  is Outline.Rectangle -> Path().apply { addRect(rect) }
  is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
  is Outline.Generic -> path
}

/**
 * Directional specular rim lighting and caustic bevel for iOS Liquid Glass.
 *
 * Implements an incident light source from the top-left, wrapping the contour with
 * a brilliant specular rim that fades into an ambient ground-bounce rim at the bottom.
 */
private fun ContentDrawScope.drawLiquidGlassEdgeEffect(shapePath: Path) = clipPath(shapePath) {
  val strokeWidth = 1.dp.toPx()
  if (size.minDimension <= 0f) return@clipPath

  // 1. Primary Directional Specular Rim Bevel (Incident lighting from top-left to bottom-right)
  val specularBrush = Brush.linearGradient(
    colors = listOf(
      Color(0xD9FFFFFF), // 85% specular highlight at top-left apex
      Color(0x33FFFFFF), // 20% transitional rim along sides
      Color(0x1AFFFFFF), // 10% lower flank
      Color(0x59FFFFFF)  // 35% ambient ground bounce at bottom-right
    ),
    start = Offset(0f, 0f),
    end = Offset(size.width, size.height),
  )
  drawPath(
    path = shapePath,
    brush = specularBrush,
    style = Stroke(width = strokeWidth),
  )

  // 2. Hairline Inner Caustic Depth Rim
  val innerStrokeWidth = 0.5.dp.toPx()
  drawPath(
    path = shapePath,
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0x26FFFFFF),
        Color.Transparent,
      ),
      startY = 0f,
      endY = size.height * 0.5f,
    ),
    style = Stroke(width = innerStrokeWidth),
  )
}

/**
 * Top-down specular glare / curvature sheen simulating light reflecting off polished glass.
 */
private fun ContentDrawScope.drawLiquidGlassSheen(shapePath: Path) = clipPath(shapePath) {
  if (size.minDimension <= 0f) return@clipPath
  val sheenHeight = size.height * 0.45f
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0x24FFFFFF), // ~14% white glare highlight at top
        Color(0x08FFFFFF), // ~3% mid sheen
        Color.Transparent
      ),
      startY = 0f,
      endY = sheenHeight,
    ),
    size = androidx.compose.ui.geometry.Size(size.width, sheenHeight),
  )
}

private fun ColorMatrix.setContrast(contrast: Float) {
  val translate = 0.5f * (1f - contrast) * 255f
  setToScale(
    redScale = contrast,
    greenScale = contrast,
    blueScale = contrast,
    alphaScale = 1f,
  )
  this[0, 4] = translate
  this[1, 4] = translate
  this[2, 4] = translate
}
