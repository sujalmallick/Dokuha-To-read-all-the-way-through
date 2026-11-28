package ui.glass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformGlassEnvironment(): PlatformGlassEnvironment {
    val context = LocalContext.current
    val powerManager = remember(context) {
        runCatching { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }.getOrNull()
    }
    var isLowPower by remember(powerManager) {
        mutableStateOf(runCatching { powerManager?.isPowerSaveMode == true }.getOrDefault(false))
    }

    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                isLowPower = runCatching { powerManager?.isPowerSaveMode == true }.getOrDefault(false)
            }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return PlatformGlassEnvironment(
        isIos = false,
        androidSdkInt = Build.VERSION.SDK_INT,
        isLowPowerMode = isLowPower,
        platformReduceTransparency = false
    )
}
