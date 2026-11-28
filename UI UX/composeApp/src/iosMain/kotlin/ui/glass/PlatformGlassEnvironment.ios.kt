package ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoPowerStateDidChangeNotification
import platform.UIKit.UIAccessibilityIsReduceTransparencyEnabled
import platform.UIKit.UIAccessibilityReduceTransparencyStatusDidChangeNotification

@Composable
actual fun rememberPlatformGlassEnvironment(): PlatformGlassEnvironment {
    var reduceTransparency by remember {
        mutableStateOf(UIAccessibilityIsReduceTransparencyEnabled())
    }
    var isLowPower by remember {
        mutableStateOf(NSProcessInfo.processInfo.lowPowerModeEnabled)
    }

    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val transparencyObserver = center.addObserverForName(
            name = UIAccessibilityReduceTransparencyStatusDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _: NSNotification? ->
            reduceTransparency = UIAccessibilityIsReduceTransparencyEnabled()
        }
        val powerObserver = center.addObserverForName(
            name = NSProcessInfoPowerStateDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _: NSNotification? ->
            isLowPower = NSProcessInfo.processInfo.lowPowerModeEnabled
        }

        onDispose {
            center.removeObserver(transparencyObserver)
            center.removeObserver(powerObserver)
        }
    }

    return PlatformGlassEnvironment(
        isIos = true,
        androidSdkInt = 0,
        isLowPowerMode = isLowPower,
        platformReduceTransparency = reduceTransparency
    )
}
