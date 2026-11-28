package ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPlatformGlassEnvironment(): PlatformGlassEnvironment = remember {
    PlatformGlassEnvironment(
        isIos = false,
        androidSdkInt = 33,
        isLowPowerMode = false,
        platformReduceTransparency = false
    )
}
