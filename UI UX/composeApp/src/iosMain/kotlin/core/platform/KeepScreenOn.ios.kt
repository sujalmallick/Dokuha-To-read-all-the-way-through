package core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) {
    DisposableEffect(enabled) {
        val app = UIApplication.sharedApplication
        val previous = app.idleTimerDisabled
        app.idleTimerDisabled = enabled
        onDispose {
            app.idleTimerDisabled = previous
        }
    }
}
