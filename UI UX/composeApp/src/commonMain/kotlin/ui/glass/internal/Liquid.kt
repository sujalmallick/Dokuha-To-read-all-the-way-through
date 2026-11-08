package ui.glass.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * State manager of recorded [Liquefiable] nodes to be rendered into [liquid] effect nodes.
 */
@Stable
public class LiquidState public constructor() {
  internal val liquefiables = mutableStateListOf<Liquefiable>()
}

/**
 * Remembers a single [LiquidState] instance in the current composition.
 */
@Composable
public fun rememberLiquidState(): LiquidState = remember { LiquidState() }

/**
 * Applies the Liquid effect, sampling pixels recorded in [liquidState].
 */
@Stable
public fun Modifier.liquid(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit = {},
): Modifier = this then liquidElement(liquidState, block)
