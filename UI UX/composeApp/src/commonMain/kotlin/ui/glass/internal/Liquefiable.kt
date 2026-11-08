package ui.glass.internal

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * Marks this modifier node as a recording surface whose rendered content can be sampled and
 * displayed through another UI layer using a [LiquidState] effect.
 */
@Stable
public fun Modifier.liquefiable(
  liquidState: LiquidState,
): Modifier = this then LiquefiableElement(liquidState)
