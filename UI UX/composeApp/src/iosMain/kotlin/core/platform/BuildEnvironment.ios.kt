package core.platform

import kotlin.native.Platform

actual object BuildEnvironment {
    actual val isDebug: Boolean = Platform.isDebugBinary
}
