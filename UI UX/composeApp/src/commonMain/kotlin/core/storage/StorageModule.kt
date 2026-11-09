package core.storage

import org.koin.core.module.Module

/**
 * Expected platform-specific storage module.
 * - Android: provides KeyValueStorage (SharedPreferences) and SecureStorage (Keystore) using androidContext().
 * - iOS: provides KeyValueStorage (NSUserDefaults) and SecureStorage (Keychain).
 */
expect val platformStorageModule: Module
