package data

import core.storage.InMemoryKeyValueStorage
import core.storage.InMemorySecureStorage
import data.profile.PersistentProfileRepository
import data.settings.DefaultReadingMode
import data.settings.PersistentSettingsRepository
import ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersistentStorageRepositoriesTest {

    @Test
    fun testSchemaVersionInitializedOnStartup() {
        val storage = InMemoryKeyValueStorage()
        assertEquals(0, storage.getInt(PersistentSettingsRepository.KEY_SCHEMA_VERSION, 0))

        PersistentSettingsRepository(storage)

        assertEquals(
            PersistentSettingsRepository.CURRENT_SCHEMA_VERSION,
            storage.getInt(PersistentSettingsRepository.KEY_SCHEMA_VERSION, 0)
        )
    }

    @Test
    fun testSettingsPersistenceAcrossInstances() {
        val storage = InMemoryKeyValueStorage()
        val repo1 = PersistentSettingsRepository(storage)

        // Modify settings
        repo1.setThemeMode(ThemeMode.OLED_BLACK)
        repo1.setAccentColorIndex(2)
        repo1.setReduceGlass(true)
        repo1.setReduceMotion(true)
        repo1.setDefaultReadingMode(DefaultReadingMode.RIGHT_TO_LEFT)
        repo1.setSidePadding(0.25f)
        repo1.setBrightness(0.95f)

        // Verify live state
        assertEquals(ThemeMode.OLED_BLACK, repo1.themeMode.value)
        assertEquals(2, repo1.accentColorIndex.value)
        assertTrue(repo1.reduceGlass.value)
        assertTrue(repo1.reduceMotion.value)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, repo1.defaultReadingMode.value)

        // Recreate repo with the same underlying storage to simulate app restart
        val repo2 = PersistentSettingsRepository(storage)

        assertEquals(ThemeMode.OLED_BLACK, repo2.themeMode.value)
        assertEquals(2, repo2.accentColorIndex.value)
        assertTrue(repo2.reduceGlass.value)
        assertTrue(repo2.reduceMotion.value)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, repo2.defaultReadingMode.value)
        assertEquals(0.25f, repo2.sidePadding.value)
        assertEquals(0.95f, repo2.brightness.value)
    }

    @Test
    fun testProfileOnlyPersistsUserEnteredFields() {
        val storage = InMemoryKeyValueStorage()
        val profileRepo1 = PersistentProfileRepository(storage)

        // Initial default
        assertEquals("Sujal", profileRepo1.profile.value.name)
        assertEquals(0xFF007AFF, profileRepo1.profile.value.avatarColorHex)

        // User edits profile
        profileRepo1.updateProfile("DokuhaFan", 0xFFEF4444)

        assertEquals("DokuhaFan", profileRepo1.profile.value.name)
        assertEquals("D", profileRepo1.profile.value.initials)
        assertEquals(0xFFEF4444, profileRepo1.profile.value.avatarColorHex)

        // Check that user-entered fields are in storage
        assertEquals("DokuhaFan", storage.getString("profile_display_name", ""))
        assertEquals(0xFFEF4444, storage.getLong("profile_avatar_color_hex", 0L))

        // Check that stats and library summaries are NOT stored in KeyValueStorage
        assertEquals(-1, storage.getInt("chapters_read", -1))
        assertEquals(-1, storage.getInt("library_reading_count", -1))
        assertEquals("", storage.getString("weekly_activity_json", ""))

        // Recreate profile repo with the same storage
        val profileRepo2 = PersistentProfileRepository(storage)

        assertEquals("DokuhaFan", profileRepo2.profile.value.name)
        assertEquals("D", profileRepo2.profile.value.initials)
        assertEquals(0xFFEF4444, profileRepo2.profile.value.avatarColorHex)
    }

    @Test
    fun testSecureStorageSeparationTokensNeverInKeyValueStorage() {
        val keyValueStorage = InMemoryKeyValueStorage()
        val secureStorage = InMemorySecureStorage()

        // Place sensitive tokens in SecureStorage
        secureStorage.putSecret("anilist_oauth_token", "secret_jwt_token_abc_123")
        secureStorage.putSecret("mal_refresh_token", "secret_refresh_xyz_789")

        // Verify tokens are retrievable from SecureStorage
        assertEquals("secret_jwt_token_abc_123", secureStorage.getSecret("anilist_oauth_token"))
        assertEquals("secret_refresh_xyz_789", secureStorage.getSecret("mal_refresh_token"))

        // Strictly verify tokens NEVER leak or appear in normal KeyValueStorage
        assertEquals("NONE", keyValueStorage.getString("anilist_oauth_token", "NONE"))
        assertEquals("NONE", keyValueStorage.getString("mal_refresh_token", "NONE"))

        // Removal
        secureStorage.removeSecret("anilist_oauth_token")
        assertNull(secureStorage.getSecret("anilist_oauth_token"))
    }

    @Test
    fun testCorruptedStorageFallsBackSafelyWithoutCrashing() {
        val storage = InMemoryKeyValueStorage()

        // Write corrupt or schema-mismatched data
        storage.putString("setting_theme_mode", "NON_EXISTENT_THEME_MODE_STRING")
        storage.putString("setting_default_reading_mode", "CORRUPTED_VALUE")
        storage.putString("setting_extension_repos", "{ this is invalid json !!! }")

        // Should not throw, should fall back to safe defaults
        val repo = PersistentSettingsRepository(storage)

        assertEquals(ThemeMode.DARK, repo.themeMode.value)
        assertEquals(DefaultReadingMode.WEBTOON, repo.defaultReadingMode.value)
        assertEquals(emptyList(), repo.extensionRepos.value)
    }
}
