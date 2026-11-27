package io.panel.reader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import core.source.SourceManga
import core.storage.InMemoryKeyValueStorage
import data.backup.BackupEngine
import data.mock.FakePanelRepository
import data.settings.DefaultReadingMode
import data.settings.FakeReaderPrefsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.PersistentSettingsRepository
import data.settings.ReaderColorFilter
import data.settings.SqlDelightReaderPrefsRepository
import data.settings.TitleReaderPrefs
import data.settings.canonicalTitleKey
import io.panel.reader.db.PanelDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class BackupEngineTest {

    private val dbName = "test_backup_engine_prefs.db"

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testBackupV3RoundTrip_preservesReaderPrefs() = runBlocking {
        val panelRepo = FakePanelRepository()
        val prefsRepo = FakeReaderPrefsRepository()
        val mangaKey = canonicalTitleKey("Solo Leveling", isNovel = false)
        val novelKey = canonicalTitleKey("Omniscient Reader", isNovel = true)

        prefsRepo.upsertPrefs(
            TitleReaderPrefs(
                titleKey = mangaKey,
                readingMode = DefaultReadingMode.RIGHT_TO_LEFT,
                colorFilter = ReaderColorFilter.SEPIA,
                sidePadding = 0.15f,
                splitDoubleSpread = false,
                dualPageOffset = true
            )
        )
        prefsRepo.upsertPrefs(
            TitleReaderPrefs(
                titleKey = novelKey,
                novelTheme = NovelReaderTheme.OLED,
                novelFont = NovelFont.SERIF,
                novelFontSize = 20f,
                novelLineHeight = 1.8f
            )
        )

        val json = BackupEngine.exportJson(
            repository = panelRepo,
            nowMs = 999_000L,
            readerPrefsRepository = prefsRepo
        )
        assertTrue(json.contains("\"schema_version\":3"))
        assertTrue(json.contains(mangaKey))
        assertTrue(json.contains(novelKey))

        val restoredPanelRepo = FakePanelRepository()
        val restoredPrefsRepo = FakeReaderPrefsRepository()
        val restoredCount = BackupEngine.importJson(
            jsonString = json,
            repository = restoredPanelRepo,
            readerPrefsRepository = restoredPrefsRepo
        )
        assertTrue(restoredCount >= 0)

        val restoredManga = restoredPrefsRepo.getPrefs(mangaKey)
        assertNotNull(restoredManga)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, restoredManga.readingMode)
        assertEquals(ReaderColorFilter.SEPIA, restoredManga.colorFilter)
        assertEquals(0.15f, restoredManga.sidePadding)
        assertEquals(false, restoredManga.splitDoubleSpread)
        assertEquals(true, restoredManga.dualPageOffset)

        val restoredNovel = restoredPrefsRepo.getPrefs(novelKey)
        assertNotNull(restoredNovel)
        assertEquals(NovelReaderTheme.OLED, restoredNovel.novelTheme)
        assertEquals(NovelFont.SERIF, restoredNovel.novelFont)
        assertEquals(20f, restoredNovel.novelFontSize)
        assertEquals(1.8f, restoredNovel.novelLineHeight)
    }

    @Test
    fun testOldV1AndV2BackupsRestoreWithoutErrors() = runBlocking {
        val v1Json = """
            {
              "schema_version": 1,
              "created_at": 1000,
              "library": [
                {"url": "manga/v1-title", "title": "V1 Title", "source_id": 1}
              ],
              "history": []
            }
        """.trimIndent()
        val v2Json = """
            {
              "schema_version": 2,
              "created_at": 2000,
              "library": [
                {"url": "manga/v2-title", "title": "V2 Title", "source_id": 2, "is_novel": true}
              ],
              "history": [],
              "categories": [],
              "category_mappings": {}
            }
        """.trimIndent()

        val panelRepo = FakePanelRepository()
        val prefsRepo = FakeReaderPrefsRepository()

        assertEquals(1, BackupEngine.importJson(v1Json, panelRepo, readerPrefsRepository = prefsRepo))
        assertEquals(1, BackupEngine.importJson(v2Json, panelRepo, readerPrefsRepository = prefsRepo))
        assertTrue(prefsRepo.getAllPrefs().isEmpty())
    }

    @Test
    fun testIncognitoSessionOverlayIsNeverExported() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
        val driver = AndroidSqliteDriver(PanelDatabase.Schema, context, dbName)
        val db = PanelDatabase(driver)
        val settingsRepo = PersistentSettingsRepository(InMemoryKeyValueStorage())
        val prefsRepo = SqlDelightReaderPrefsRepository(db, settingsRepo, clock = { 5000L })
        val panelRepo = FakePanelRepository()

        val persistedKey = canonicalTitleKey("Persisted Manga", isNovel = false)
        val incognitoOnlyKey = canonicalTitleKey("Incognito Secret", isNovel = false)

        prefsRepo.upsertPrefs(TitleReaderPrefs(titleKey = persistedKey, readingMode = DefaultReadingMode.LEFT_TO_RIGHT))

        settingsRepo.setIncognitoMode(true)
        prefsRepo.upsertPrefs(TitleReaderPrefs(titleKey = incognitoOnlyKey, readingMode = DefaultReadingMode.VERTICAL))
        prefsRepo.updatePrefs(persistedKey) { it.copy(colorFilter = ReaderColorFilter.INVERTED_OLED) }

        val exported = BackupEngine.exportJson(
            repository = panelRepo,
            nowMs = 12345L,
            readerPrefsRepository = prefsRepo
        )
        assertTrue(exported.contains(persistedKey))
        assertFalse(exported.contains(incognitoOnlyKey))
        assertFalse(exported.contains("INVERTED_OLED"))

        driver.close()
    }

    @Test
    fun testRestorePolicyLocalWins_andRestoresPrefsForNonLibraryTitles() = runBlocking {
        val sourcePanel = FakePanelRepository()
        val sourcePrefs = FakeReaderPrefsRepository()
        val existingKey = canonicalTitleKey("Existing Local", isNovel = false)
        val browseOnlyKey = canonicalTitleKey("Browse Only Title", isNovel = true)

        sourcePrefs.upsertPrefs(TitleReaderPrefs(titleKey = existingKey, readingMode = DefaultReadingMode.VERTICAL))
        sourcePrefs.upsertPrefs(TitleReaderPrefs(titleKey = browseOnlyKey, novelTheme = NovelReaderTheme.SEPIA, novelFontSize = 21f))

        // Export with empty library / without browseOnlyKey in library
        val json = BackupEngine.exportJson(sourcePanel, nowMs = 555L, readerPrefsRepository = sourcePrefs)

        val targetPanel = FakePanelRepository()
        val targetPrefs = FakeReaderPrefsRepository()
        // Pre-populate local override for existingKey
        targetPrefs.upsertPrefs(TitleReaderPrefs(titleKey = existingKey, readingMode = DefaultReadingMode.RIGHT_TO_LEFT))

        BackupEngine.importJson(json, targetPanel, readerPrefsRepository = targetPrefs)

        // Local wins for existingKey
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, targetPrefs.getPrefs(existingKey)?.readingMode)

        // Browse-only title prefs restored even though title was not in backup.library
        val restoredBrowseOnly = targetPrefs.getPrefs(browseOnlyKey)
        assertNotNull(restoredBrowseOnly)
        assertEquals(NovelReaderTheme.SEPIA, restoredBrowseOnly.novelTheme)
        assertEquals(21f, restoredBrowseOnly.novelFontSize)
    }
}
