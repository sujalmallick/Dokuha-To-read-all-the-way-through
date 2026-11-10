package data

import data.profile.FakeProfileRepository
import data.profile.TrackerService
import data.settings.DefaultReadingMode
import data.settings.FakeSettingsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.UpdateInterval
import ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsProfileRepositoriesTest {

    @Test
    fun testSettingsRepository_appearanceDefaultsAndUpdates() {
        val repo = FakeSettingsRepository()

        assertEquals(ThemeMode.DARK, repo.themeMode.value)
        assertEquals(0, repo.accentColorIndex.value)
        assertFalse(repo.reduceGlass.value)
        assertFalse(repo.reduceMotion.value)

        repo.setThemeMode(ThemeMode.OLED_BLACK)
        assertEquals(ThemeMode.OLED_BLACK, repo.themeMode.value)

        repo.setAccentColorIndex(3)
        assertEquals(3, repo.accentColorIndex.value)

        repo.setReduceGlass(true)
        assertTrue(repo.reduceGlass.value)

        repo.setReduceMotion(true)
        assertTrue(repo.reduceMotion.value)
    }

    @Test
    fun testSettingsRepository_readerAndNovelSettings() {
        val repo = FakeSettingsRepository()

        assertEquals(DefaultReadingMode.WEBTOON, repo.defaultReadingMode.value)
        repo.setDefaultReadingMode(DefaultReadingMode.RIGHT_TO_LEFT)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, repo.defaultReadingMode.value)

        repo.setBrightness(0.95f)
        assertEquals(0.95f, repo.brightness.value)

        repo.setSidePadding(0.25f)
        assertEquals(0.25f, repo.sidePadding.value)

        repo.setNovelFont(NovelFont.SERIF)
        assertEquals(NovelFont.SERIF, repo.novelFont.value)

        repo.setNovelFontSize(20f)
        assertEquals(20f, repo.novelFontSize.value)

        repo.setNovelTheme(NovelReaderTheme.SEPIA)
        assertEquals(NovelReaderTheme.SEPIA, repo.novelTheme.value)
    }

    @Test
    fun testSettingsRepository_libraryAndDownloads() {
        val repo = FakeSettingsRepository()

        assertEquals(UpdateInterval.DAILY, repo.updateInterval.value)
        repo.setUpdateInterval(UpdateInterval.EVERY_12_HOURS)
        assertEquals(UpdateInterval.EVERY_12_HOURS, repo.updateInterval.value)

        assertTrue(repo.downloadOnlyOnWifi.value)
        repo.setDownloadOnlyOnWifi(false)
        assertFalse(repo.downloadOnlyOnWifi.value)

        assertEquals(2, repo.concurrentDownloads.value)
        repo.setConcurrentDownloads(3)
        assertEquals(3, repo.concurrentDownloads.value)
    }

    @Test
    fun testSettingsRepository_extensionReposSecurityValidation() {
        val repo = FakeSettingsRepository()
        val initialCount = repo.extensionRepos.value.size

        // Insecure http URL should be rejected
        val insecureResult = repo.addExtensionRepo("Unsafe Repo", "http://insecure.example.com/repo")
        assertTrue(insecureResult.isFailure)
        assertEquals(initialCount, repo.extensionRepos.value.size)

        // Valid https URL should succeed
        val secureResult = repo.addExtensionRepo("Safe Repo", "https://trusted.example.com/repo/index.min.json")
        assertTrue(secureResult.isSuccess)
        assertEquals(initialCount + 1, repo.extensionRepos.value.size)
        assertTrue(repo.extensionRepos.value.any { it.name == "Safe Repo" })
    }

    @Test
    fun testSettingsRepository_storageAndBackupActions() {
        val repo = FakeSettingsRepository()

        assertEquals("324 MB", repo.cacheSize.value)
        repo.clearCache()
        assertEquals("0 MB", repo.cacheSize.value)

        val backupMsg = repo.createBackup()
        assertTrue(backupMsg.isNotEmpty())
        assertEquals("Today at 01:25 AM", repo.lastBackupTimestamp.value)

        val restoreResult = repo.restoreBackup("file://backup.tachibk")
        assertTrue(restoreResult.isSuccess)
        assertEquals("Restored just now", repo.lastBackupTimestamp.value)

        val failedRestore = repo.restoreBackup("")
        assertTrue(failedRestore.isFailure)
    }

    @Test
    fun testProfileRepository_defaultsAndProfileUpdates() {
        val repo = FakeProfileRepository()

        val initialProfile = repo.profile.value
        assertEquals("Ritwik", initialProfile.name)
        assertEquals("R", initialProfile.initials)

        repo.updateProfile("Jane Doe", 0xFFA855F7)
        val updatedProfile = repo.profile.value
        assertEquals("Jane Doe", updatedProfile.name)
        assertEquals("JD", updatedProfile.initials)
        assertEquals(0xFFA855F7, updatedProfile.avatarColorHex)

        // Verify stats and library summary for clean user
        assertEquals(0, repo.stats.value.chaptersRead)
        assertEquals(0, repo.librarySummary.value.readingCount)
        assertEquals(7, repo.weeklyActivity.value.size)
    }

    @Test
    fun testProfileRepository_toggleTracker() {
        val repo = FakeProfileRepository()

        val malBefore = repo.trackers.value.first { it.service == TrackerService.MY_ANIME_LIST }
        assertFalse(malBefore.isConnected)

        repo.toggleTrackerConnection(TrackerService.MY_ANIME_LIST)
        val malAfter = repo.trackers.value.first { it.service == TrackerService.MY_ANIME_LIST }
        assertTrue(malAfter.isConnected)
        assertEquals("sujal_my_anime_list", malAfter.username)

        repo.toggleTrackerConnection(TrackerService.MY_ANIME_LIST)
        val malDisconnected = repo.trackers.value.first { it.service == TrackerService.MY_ANIME_LIST }
        assertFalse(malDisconnected.isConnected)
    }
}
