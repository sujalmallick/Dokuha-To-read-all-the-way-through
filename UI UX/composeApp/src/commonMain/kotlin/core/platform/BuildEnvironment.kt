package core.platform

/**
 * Expected platform-specific build environment flags.
 * - Android: backed by BuildConfig.DEBUG.
 * - iOS: backed by Platform.isDebugBinary.
 */
expect object BuildEnvironment {
    val isDebug: Boolean
}
