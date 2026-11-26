package ui.volume

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/**
 * Shared composition-local that propagates Android volume key events
 * into the Compose tree without coupling the reader composable to Activity.
 *
 * On non-Android targets (iOS) the state never increments.
 */
data class VolumeKeyState(
    val upPresses: State<Int>,
    val downPresses: State<Int>,
    val isReaderActive: MutableState<Boolean> = mutableStateOf(false)
)

private val _noOpUp = mutableStateOf(0)
private val _noOpDown = mutableStateOf(0)

/** Default value — volume keys do nothing (iOS). */
val LocalVolumeKeyState = compositionLocalOf {
    VolumeKeyState(upPresses = _noOpUp, downPresses = _noOpDown)
}
