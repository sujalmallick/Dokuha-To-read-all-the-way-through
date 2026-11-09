package core.storage

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

/**
 * iOS SecureStorage implementation stub.
 * Note: Real Keychain access requires an iOS device or simulator with entitlements.
 * Per user instructions, iOS implementation is reported as unverified.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSecureStorage : SecureStorage {
    // Unverified iOS Keychain stub with memory cache fallback for non-Mac build environments
    private val memoryFallback = mutableMapOf<String, String>()

    override fun getSecret(key: String): String? = memoryFallback[key]

    override fun putSecret(key: String, secret: String) {
        memoryFallback[key] = secret
    }

    override fun removeSecret(key: String) {
        memoryFallback.remove(key)
    }

    override fun clearSecrets() {
        memoryFallback.clear()
    }
}
