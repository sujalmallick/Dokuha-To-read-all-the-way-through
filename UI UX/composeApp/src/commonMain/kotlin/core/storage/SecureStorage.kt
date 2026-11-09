package core.storage

/**
 * Hardware-backed secure storage for sensitive authentication tokens and secrets.
 * - Android: Android Keystore + AES/GCM cipher.
 * - iOS: Keychain Services (kSecClassGenericPassword).
 *
 * Rules:
 * - Tokens and secrets go ONLY here, never in KeyValueStorage.
 * - Never log or print values retrieved from SecureStorage.
 */
interface SecureStorage {
    fun getSecret(key: String): String?
    fun putSecret(key: String, secret: String)
    fun removeSecret(key: String)
    fun clearSecrets()
}
