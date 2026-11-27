package core.platform

import androidx.compose.runtime.Composable

/**
 * Platform side effect that prevents the screen from sleeping while [enabled] is true
 * and restores the previous idle-timer state on disposal.
 */
@Composable
expect fun KeepScreenOnEffect(enabled: Boolean)
