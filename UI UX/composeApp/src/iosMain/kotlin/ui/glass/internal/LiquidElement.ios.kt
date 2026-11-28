package ui.glass.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

internal actual fun liquidElement(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit,
): AbstractLiquidElement<out AbstractLiquidNode> = LiquidBackupElement(liquidState, block)

internal actual fun LayoutCoordinates.liquidPositionOnScreen(): Offset = positionInWindow()

internal actual fun LayoutCoordinates.liquidInverseTransform(): LiquidInverseTransform {
  // Compute scale and rotation from window-relative basis vectors; iOS LayoutCoordinates.transformToScreen throws.
  val origin = localToWindow(Offset.Zero)
  val unitX = localToWindow(Offset(1f, 0f)) - origin
  val unitY = localToWindow(Offset(0f, 1f)) - origin
  val scaleXMagnitude = hypot(unitX.x, unitX.y)
  val scaleYMagnitude = hypot(unitY.x, unitY.y)
  return LiquidInverseTransform(
    inverseScaleX = if (scaleXMagnitude > 0f) 1f / scaleXMagnitude else 0f,
    inverseScaleY = if (scaleYMagnitude > 0f) 1f / scaleYMagnitude else 0f,
    inverseRotationZ = -(180.0 / PI).toFloat() * atan2(unitX.y, unitX.x),
  )
}
