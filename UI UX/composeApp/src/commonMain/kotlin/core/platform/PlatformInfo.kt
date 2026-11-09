package core.platform

/**
 * Expected platform-specific runtime metadata.
 */
expect object PlatformInfo {
    val isAndroid: Boolean
    val platformName: String
}
