package core.platform

import kotlin.native.Platform
import kotlin.experimental.ExperimentalNativeApi

actual object BuildEnvironment {
    @OptIn(ExperimentalNativeApi::class)
    actual val isDebug: Boolean = Platform.isDebugBinary
}
