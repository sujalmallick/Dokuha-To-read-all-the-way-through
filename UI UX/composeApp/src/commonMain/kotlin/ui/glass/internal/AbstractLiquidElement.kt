package ui.glass.internal

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastFilter

internal expect fun liquidElement(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit,
): AbstractLiquidElement<out AbstractLiquidNode>

internal expect fun LayoutCoordinates.liquidPositionOnScreen(): Offset

internal data class LiquidInverseTransform(
  val inverseScaleX: Float = 1f,
  val inverseScaleY: Float = 1f,
  val inverseRotationZ: Float = 0f,
)

internal expect fun LayoutCoordinates.liquidInverseTransform(): LiquidInverseTransform

internal abstract class AbstractLiquidElement<N : AbstractLiquidNode>(
  protected val liquidState: LiquidState,
  protected val block: LiquidScope.() -> Unit,
) : ModifierNodeElement<N>() {

  override fun update(node: N) {
    node.liquidState = liquidState
    node.block = block
    node.invalidateLiquidBlock()
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "Liquid"
    properties["block"] = block
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AbstractLiquidElement<N>) return false
    if (liquidState !== other.liquidState) return false
    if (block !== other.block) return false
    return true
  }

  override fun hashCode(): Int {
    var result = liquidState.hashCode()
    result = 31 * result + block.hashCode()
    return result
  }
}

internal abstract class AbstractLiquidNode(
  var liquidState: LiquidState,
  var block: LiquidScope.() -> Unit,
) : Modifier.Node(),
  GlobalPositionAwareModifierNode,
  DrawModifierNode,
  CompositionLocalConsumerModifierNode,
  ObserverModifierNode {
  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  internal val reusableScope = LiquidScopeImpl()
  private var cachedLayer: GraphicsLayer? = null
  private var cachedRenderEffect: RenderEffect? = null

  protected abstract fun createRenderEffect(): RenderEffect?

  protected open fun inspectDirtyFields() = Unit

  protected open fun ContentDrawScope.applyAdditionalEffects(
    layer: GraphicsLayer,
    drawBlock: () -> Unit,
  ) = drawBlock()

  protected open val invalidateFlags: Int = Fields.InvalidateFlags
  protected open val renderEffectFlags: Int = Fields.RenderEffectFields

  internal fun invalidateLiquidBlock() {
    if (!isAttached) return
    reusableScope.density = currentValueOf(LocalDensity)
    reusableScope.layoutDirection = currentValueOf(LocalLayoutDirection)
    block(reusableScope)

    val ancestor = Snapshot.withoutReadObservation {
      (findNearestAncestor(LiquefiableNode.LiquefiableKey) as? LiquefiableNode)?.liquefiable
    }
    reusableScope.liquefiables = liquidState.liquefiables.fastFilter { it != ancestor }

    if (reusableScope.size.isSpecified) {
      invalidateDrawIfNeeded()
    }
  }

  private fun invalidateDrawIfNeeded() {
    if (reusableScope.mutatedFields has invalidateFlags) {
      inspectDirtyFields()

      if (reusableScope.mutatedFields has renderEffectFlags) {
        cachedRenderEffect = createRenderEffect()
      }

      reusableScope.clean()
      invalidateDraw()
    }
  }

  private fun obtainGraphicsLayer() = cachedLayer?.takeUnless { it.isReleased }
    ?: currentValueOf(LocalGraphicsContext)
      .createGraphicsLayer()
      .also { cachedLayer = it }

  override val shouldAutoInvalidate: Boolean = false

  override fun onAttach() = observeReads(::invalidateLiquidBlock)

  override fun onDetach() {
    cachedLayer?.let { currentValueOf(LocalGraphicsContext).releaseGraphicsLayer(it) }
    cachedLayer = null
    reusableScope.clean()
  }

  override fun onObservedReadsChanged() = invalidateLiquidBlock()

  override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
    if (!isAttached || !coordinates.isAttached) return

    val transform = coordinates.liquidInverseTransform()
    reusableScope.inverseScaleX = transform.inverseScaleX
    reusableScope.inverseScaleY = transform.inverseScaleY
    reusableScope.inverseRotationZ = transform.inverseRotationZ
    reusableScope.positionOnScreen = coordinates.liquidPositionOnScreen()
    reusableScope.size = coordinates.size.toSize()
    reusableScope.boundsInRoot = coordinates.boundsInRoot()

    invalidateDrawIfNeeded()
  }

  override fun ContentDrawScope.draw() {
    if (size.minDimension < 1f) {
      drawContent()
      return
    }

    var renderedGlass = false
    try {
      val layer = obtainGraphicsLayer()
      val recorded = recordLiquefiablesIntoLayer(layer, reusableScope)

      if (recorded) {
        applyAdditionalEffects(layer) {
          layer.renderEffect = cachedRenderEffect
          drawLayer(layer)
        }
        renderedGlass = true
      }
    } catch (_: Throwable) {
      // Graceful fallback to prevent hardware renderer aborts
    }

    if (!renderedGlass) {
      drawFallbackGlass()
    }

    drawContent()
  }

  private fun ContentDrawScope.drawFallbackGlass() = with(reusableScope) {
    val shapeOutline = shape.createOutline(size, layoutDirection, this@drawFallbackGlass)
    if (tint.isSpecified) {
      drawOutline(
        outline = shapeOutline,
        color = tint,
        style = Fill,
      )
    }
    if (edge > 0f) {
      val rimBrush = Brush.linearGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.28f),
          Color.White.copy(alpha = 0.08f),
          Color.White.copy(alpha = 0.16f),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
      )
      drawOutline(
        outline = shapeOutline,
        brush = rimBrush,
        style = Stroke(width = 0.75.dp.toPx()),
      )
    }
  }
}
