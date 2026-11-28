package ui.glass.internal

import android.graphics.RenderEffect.createBlurEffect
import android.graphics.RenderEffect.createChainEffect
import android.graphics.RenderEffect.createRuntimeShaderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

private val isRuntimeShaderSupported: Boolean by lazy {
  if (Build.VERSION.SDK_INT < 33) return@lazy false
  // Robolectric's software Canvas ignores AGSL RenderEffect shaders; use Canvas backup for deterministic rendering.
  if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) return@lazy false
  try {
    RuntimeShader(LiquidShader)
    true
  } catch (_: Throwable) {
    false
  }
}

internal actual fun liquidElement(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit,
): AbstractLiquidElement<out AbstractLiquidNode> = when {
  Build.VERSION.SDK_INT >= 33 && isRuntimeShaderSupported -> LiquidElement(liquidState, block)
  else -> LiquidBackupElement(liquidState, block)
}

internal actual fun LayoutCoordinates.liquidPositionOnScreen(): Offset = positionOnScreen()

internal actual fun LayoutCoordinates.liquidInverseTransform(): LiquidInverseTransform {
  val matrix = Matrix()
  transformToScreen(matrix)
  val scaleX = matrix.values[Matrix.ScaleX]
  val scaleY = matrix.values[Matrix.ScaleY]
  val skewX = matrix.values[Matrix.SkewX]
  val skewY = matrix.values[Matrix.SkewY]
  val scaleXMagnitude = sqrt(scaleX * scaleX + skewY * skewY)
  val scaleYMagnitude = sqrt(skewX * skewX + scaleY * scaleY)
  return LiquidInverseTransform(
    inverseScaleX = if (scaleXMagnitude > 0f) 1f / scaleXMagnitude else 0f,
    inverseScaleY = if (scaleYMagnitude > 0f) 1f / scaleYMagnitude else 0f,
    inverseRotationZ = -(180.0 / PI).toFloat() * atan2(skewY, scaleX),
  )
}

@RequiresApi(33)
internal class LiquidElement(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit,
) : AbstractLiquidElement<LiquidNode>(liquidState, block) {
  override fun create() = LiquidNode(liquidState, block)
}

@RequiresApi(33)
internal class LiquidNode(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit,
) : AbstractLiquidNode(liquidState, block) {
  private val liquidShader = RuntimeShader(LiquidShader)
  private var cachedBlurEffect: android.graphics.RenderEffect? = null

  override fun createRenderEffect(): RenderEffect? = with(reusableScope) {
    if (size.isUnspecified || size.width <= 0f || size.height <= 0f) return null

    try {
      liquidShader.updateLiquidUniforms()
      val liquidEffect = createRuntimeShaderEffect(liquidShader, "content")

      if (frostRadius < 1f) {
        return liquidEffect.asComposeRenderEffect()
      }

      val blurEffect = cachedBlurEffect
        ?.takeUnless { mutatedFields has Fields.Frost }
        ?: createBlurEffect(
          frostRadius.coerceAtLeast(1f),
          frostRadius.coerceAtLeast(1f),
          Shader.TileMode.CLAMP,
        ).also { cachedBlurEffect = it }

      return createChainEffect(liquidEffect, blurEffect).asComposeRenderEffect()
    } catch (_: Throwable) {
      // Graceful fallback to basic blur or null on unsupported/failing GPU drivers
      return cachedBlurEffect?.asComposeRenderEffect()
    }
  }

  private fun RuntimeShader.updateLiquidUniforms() = with(reusableScope) {
    setFloatUniform("size", size.width, size.height)
    setFloatUniform("cornerRadii", cornerRadii)
    setFloatUniform("refraction", refraction)
    setFloatUniform("curve", curve)
    setFloatUniform("edge", edge)
    setFloatUniform("tint", colorComponents)
    setFloatUniform("saturation", saturation)
    setFloatUniform("dispersion", dispersion)
    setFloatUniform("contrast", contrast)
  }
}
