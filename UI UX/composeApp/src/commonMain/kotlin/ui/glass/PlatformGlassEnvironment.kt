package ui.glass

import androidx.compose.runtime.Composable

/**
 * Observes platform-level Reduce Transparency and Low Power / Battery Saver state at runtime.
 */
@Composable
expect fun rememberPlatformGlassEnvironment(): PlatformGlassEnvironment
