package ui.glass.internal

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize

internal interface InternalLiquidScope : LiquidScope {
  var density: Density
  var layoutDirection: LayoutDirection
  var size: Size
  var intSize: IntSize
  var positionOnScreen: Offset
  var inverseScaleX: Float
  var inverseScaleY: Float
  var inverseRotationZ: Float
  var boundsInRoot: Rect
  var liquefiables: List<Liquefiable>

  fun clean()
}

internal class LiquidScopeImpl : InternalLiquidScope {
  internal var mutatedFields = 0
    private set

  override var frost: Dp = 0.dp
    set(value) {
      if (field != value) {
        field = value
        frostRadius = with(density) { value.toPx() }
      }
    }

  override var shape: Shape = CircleShape
    set(value) {
      if (field != value) {
        field = value
        if (size.isSpecified) {
          cornerRadii = value.normalizedCornerRadii(size, density, layoutDirection)
        }
      }
    }

  override var refraction: Float = 0.25f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Refraction
        field = value
      }
    }

  override var curve: Float = 0.25f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Curve
        field = value
      }
    }

  override var edge: Float = 0f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Edge
        field = value
      }
    }

  override var tint: Color = Color.Unspecified
    set(value) {
      if (field != value) {
        field = value
        argbColor = value.toArgb()
        colorComponents = value.getComponents()
      }
    }

  override var saturation: Float = 1f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Saturation
        field = value
      }
    }

  override var dispersion: Float = 0f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Dispersion
        field = value
      }
    }

  override var contrast: Float = 1f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Contrast
        field = value
      }
    }

  override var density: Density = Density(1f)
    set(value) {
      if (field != value) {
        field = value
        frostRadius = with(value) { frost.toPx() }
      }
    }

  override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    set(value) {
      if (field != value) {
        field = value
        if (size.isSpecified) {
          cornerRadii = shape.normalizedCornerRadii(size, density, value)
        }
      }
    }

  override var size: Size = Size.Unspecified
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Size
        field = value
        intSize = value.toIntSize()
        if (value.isSpecified) {
          cornerRadii = shape.normalizedCornerRadii(value, density, layoutDirection)
        }
      }
    }

  override var intSize: IntSize = IntSize.Zero

  override var positionOnScreen: Offset = Offset.Unspecified
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.PositionOnScreen
        field = value
      }
    }

  override var inverseRotationZ: Float = 0f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Rotation
        field = value
      }
    }

  override var inverseScaleX: Float = 1f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.ScaleX
        field = value
      }
    }

  override var inverseScaleY: Float = 1f
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.ScaleY
        field = value
      }
    }

  override var boundsInRoot: Rect = Rect.Zero
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.BoundsInRoot
        field = value
      }
    }

  override var liquefiables: List<Liquefiable> = emptyList()
    set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Liquefiables
        field = value
      }
    }

  @androidx.annotation.Size(value = 4)
  internal var cornerRadii: FloatArray = Float4Zero
    private set(value) {
      if (!field.contentEquals(value)) {
        mutatedFields = mutatedFields or Fields.Shape
        field = value
      }
    }

  internal var frostRadius: Float = 0f
    private set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Frost
        field = value
        sigma = 0.57735f * value + 0.5f
      }
    }

  internal var argbColor: Int = 0
    private set(value) {
      if (field != value) {
        mutatedFields = mutatedFields or Fields.Tint
        field = value
      }
    }

  @androidx.annotation.Size(value = 4)
  internal var colorComponents: FloatArray = Float4Zero
    private set

  internal var sigma: Float = 0f
    private set

  override fun clean() {
    mutatedFields = 0
  }

  @androidx.annotation.Size(value = 4)
  private fun Color.getComponents(): FloatArray = floatArrayOf(red, green, blue, alpha)

  companion object {
    @Stable
    internal val Float4Zero = floatArrayOf(0f, 0f, 0f, 0f)
  }
}

@Suppress("ConstPropertyName")
internal object Fields {
  const val Frost: Int = 0b1
  const val Shape: Int = 0b1 shl 1
  const val Refraction: Int = 0b1 shl 2
  const val Curve: Int = 0b1 shl 3
  const val Edge: Int = 0b1 shl 4
  const val Size: Int = 0b1 shl 5
  const val Tint: Int = 0b1 shl 6
  const val Saturation: Int = 0b1 shl 7
  const val Dispersion: Int = 0b1 shl 8
  const val Contrast: Int = 0b1 shl 9

  const val PositionOnScreen: Int = 0b1 shl 10
  const val Rotation: Int = 0b1 shl 11
  const val ScaleX: Int = 0b1 shl 12
  const val ScaleY: Int = 0b1 shl 13
  const val Liquefiables: Int = 0b1 shl 14
  const val BoundsInRoot: Int = 0b1 shl 15

  const val RenderEffectFields: Int =
    Frost or
      Shape or
      Refraction or
      Curve or
      Edge or
      Size or
      Tint or
      Saturation or
      Dispersion or
      Contrast

  const val InvalidateFlags: Int =
    RenderEffectFields or
      PositionOnScreen or
      Rotation or
      ScaleX or
      ScaleY or
      Liquefiables or
      BoundsInRoot

  const val PreSnowConeInvalidateFlags: Int =
    Shape or
      Edge or
      Size or
      Tint or
      Saturation or
      Contrast or
      PositionOnScreen or
      Rotation or
      ScaleX or
      ScaleY or
      Liquefiables or
      BoundsInRoot

  const val PreTiramisuInvalidateFlags: Int = PreSnowConeInvalidateFlags or Frost
}
