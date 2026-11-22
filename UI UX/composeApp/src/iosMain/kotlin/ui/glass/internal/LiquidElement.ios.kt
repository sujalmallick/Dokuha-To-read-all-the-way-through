package ui.glass.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow

internal actual fun liquidElement(
  liquidState: LiquidState,
  block: LiquidScope.() -> Unit,
): AbstractLiquidElement<out AbstractLiquidNode> = LiquidBackupElement(liquidState, block)

internal actual fun LayoutCoordinates.liquidPositionOnScreen(): Offset = positionInWindow()
