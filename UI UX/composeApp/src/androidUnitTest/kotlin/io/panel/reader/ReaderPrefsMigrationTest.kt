package io.panel.reader

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import core.storage.InMemoryKeyValueStorage
import data.settings.DefaultReadingMode
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.PersistentSettingsRepository
import data.settings.ReaderColorFilter
import data.settings.SqlDelightReaderPrefsRepository
import data.settings.TitleReaderPrefs
import data.settings.canonicalTitleKey
import io.panel.reader.db.PanelDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class ReaderPrefsMigrationTest {

    private val dbName = "test_reader_prefs_migration.db"

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
    }

    private fun seedV2Database(context: Context) {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 2) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE mangas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        source_id INTEGER NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT,
                        author TEXT,
                        description TEXT,
                        genres TEXT,
                        status TEXT NOT NULL DEFAULT 'UNKNOWN',
                        thumbnail_url TEXT,
                        favorite INTEGER NOT NULL DEFAULT 0,
                        last_update INTEGER NOT NULL DEFAULT 0,
                        next_update INTEGER NOT NULL DEFAULT 0,
                        initialized INTEGER NOT NULL DEFAULT 0,
                        viewer_flags INTEGER NOT NULL DEFAULT 0,
                        chapter_flags INTEGER NOT NULL DEFAULT 0,
                        cover_last_modified INTEGER NOT NULL DEFAULT 0,
                        date_added INTEGER NOT NULL DEFAULT 0,
                        is_novel INTEGER NOT NULL DEFAULT 0,
                        CONSTRAINT mangas_source_id_url_unique UNIQUE (source_id, url)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sort INTEGER NOT NULL DEFAULT 0,
                        flags INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE mangas_categories (
                        manga_id INTEGER NOT NULL,
                        category_id INTEGER NOT NULL,
                        PRIMARY KEY (manga_id, category_id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE chapters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        manga_id INTEGER NOT NULL,
                        url TEXT NOT NULL,
                        name TEXT NOT NULL,
                        scanlator TEXT,
                        read INTEGER NOT NULL DEFAULT 0,
                        bookmark INTEGER NOT NULL DEFAULT 0,
                        last_page_read INTEGER NOT NULL DEFAULT 0,
                        chapter_number REAL NOT NULL DEFAULT -1.0,
                        source_order INTEGER NOT NULL DEFAULT 0,
                        date_fetch INTEGER NOT NULL DEFAULT 0,
                        date_upload INTEGER NOT NULL DEFAULT 0,
                        last_modified_at INTEGER NOT NULL DEFAULT 0,
                        CONSTRAINT chapters_manga_id_url_unique UNIQUE (manga_id, url)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chapter_id INTEGER NOT NULL UNIQUE,
                        read_at INTEGER NOT NULL,
                        time_read INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS mangas_categories_category_id_idx ON mangas_categories(category_id)")
                db.execSQL(
                    "INSERT INTO mangas (id, source_id, url, title, favorite) VALUES (1, 7, 'manga/solo', 'Solo Leveling', 1)"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.close()
        helper.close()
    }

    @Test
    fun testUpgradeFromV2PreservesExistingDataAndSupportsReaderPrefs() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
        seedV2Database(context)

        val driver = AndroidSqliteDriver(PanelDatabase.Schema, context, dbName)
        val db = PanelDatabase(driver)

        val migratedManga = db.mangaQueries.getMangaByUrl("manga/solo").executeAsOneOrNull()
        assertNotNull(migratedManga)
        assertEquals("Solo Leveling", migratedManga.title)

        val global = PersistentSettingsRepository(InMemoryKeyValueStorage())
        val repo = SqlDelightReaderPrefsRepository(db, global, clock = { 1000L })
        val key = canonicalTitleKey("Solo Leveling", isNovel = false)

        repo.upsertPrefs(
            TitleReaderPrefs(
                titleKey = key,
                readingMode = DefaultReadingMode.RIGHT_TO_LEFT,
                colorFilter = ReaderColorFilter.SEPIA,
                sidePadding = 0.18f,
                splitDoubleSpread = false,
                dualPageOffset = true,
                novelTheme = NovelReaderTheme.OLED,
                novelFont = NovelFont.SERIF,
                novelFontSize = 19f,
                novelLineHeight = 1.6f
            )
        )

        val loaded = repo.getPrefs(key)
        assertNotNull(loaded)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, loaded.readingMode)
        assertEquals(ReaderColorFilter.SEPIA, loaded.colorFilter)
        assertEquals(0.18f, loaded.sidePadding)
        assertEquals(false, loaded.splitDoubleSpread)
        assertEquals(true, loaded.dualPageOffset)

        driver.close()
    }

    @Test
    fun testIncognitoWritesNeverReachDb_readsIncludeOverlay_andResetClearsOverlay() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
        seedV2Database(context)

        val driver = AndroidSqliteDriver(PanelDatabase.Schema, context, dbName)
        val db = PanelDatabase(driver)
        val global = PersistentSettingsRepository(InMemoryKeyValueStorage())
        val repo = SqlDelightReaderPrefsRepository(db, global, clock = { 1000L })
        val key = canonicalTitleKey("Solo Leveling", isNovel = false)

        // Seed persisted override while incognito is OFF
        repo.upsertPrefs(TitleReaderPrefs(titleKey = key, readingMode = DefaultReadingMode.RIGHT_TO_LEFT))
        assertEquals("RIGHT_TO_LEFT", db.readerPrefsQueries.getByTitleKey(key).executeAsOneOrNull()?.reading_mode)

        // Turn incognito ON: writes stay in session overlay, DB remains unchanged
        global.setIncognitoMode(true)
        repo.updatePrefs(key) { it.copy(readingMode = DefaultReadingMode.VERTICAL) }
        assertEquals(DefaultReadingMode.VERTICAL, repo.getPrefs(key)?.readingMode)
        assertEquals(DefaultReadingMode.VERTICAL, repo.observePrefs(key).first()?.readingMode)
        assertEquals("RIGHT_TO_LEFT", db.readerPrefsQueries.getByTitleKey(key).executeAsOneOrNull()?.reading_mode)

        // Reset during incognito clears overlay so reads return null, while DB remains untouched
        repo.resetPrefs(key)
        assertNull(repo.getPrefs(key))
        assertNull(repo.observePrefs(key).first())
        assertEquals("RIGHT_TO_LEFT", db.readerPrefsQueries.getByTitleKey(key).executeAsOneOrNull()?.reading_mode)

        // Turning incognito OFF restores persisted DB state
        global.setIncognitoMode(false)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, repo.getPrefs(key)?.readingMode)

        driver.close()
    }

    @Test
    fun testUnknownEnumInDatabaseFallsBackToNull() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
        val driver = AndroidSqliteDriver(PanelDatabase.Schema, context, dbName)
        val db = PanelDatabase(driver)
        val global = PersistentSettingsRepository(InMemoryKeyValueStorage())
        val repo = SqlDelightReaderPrefsRepository(db, global)

        db.readerPrefsQueries.upsert(
            titleKey = "manga:legacy-unknown",
            readingMode = "OLD_REMOVED_MODE",
            colorFilter = "OLD_FILTER",
            sidePadding = 5.0,
            splitDoubleSpread = 1L,
            dualPageOffset = 0L,
            novelTheme = "OLD_THEME",
            novelFont = "OLD_FONT",
            novelFontSize = 100.0,
            novelLineHeight = 0.2,
            updatedAt = 2000L
        )

        val safeLoaded = repo.getPrefs("manga:legacy-unknown")
        assertNotNull(safeLoaded)
        assertNull(safeLoaded.readingMode)
        assertNull(safeLoaded.colorFilter)
        assertNull(safeLoaded.novelTheme)
        assertNull(safeLoaded.novelFont)
        assertEquals(0.4f, safeLoaded.sidePadding)
        assertEquals(26f, safeLoaded.novelFontSize)
        assertEquals(1.2f, safeLoaded.novelLineHeight)

        driver.close()
    }

    @Test
    fun testResetDeletesRowFromDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
        val driver = AndroidSqliteDriver(PanelDatabase.Schema, context, dbName)
        val db = PanelDatabase(driver)
        val global = PersistentSettingsRepository(InMemoryKeyValueStorage())
        val repo = SqlDelightReaderPrefsRepository(db, global)
        val key = canonicalTitleKey("Omniscient Reader", isNovel = false)

        repo.upsertPrefs(TitleReaderPrefs(titleKey = key, sidePadding = 0.2f))
        assertNotNull(db.readerPrefsQueries.getByTitleKey(key).executeAsOneOrNull())

        repo.resetPrefs(key)
        assertNull(db.readerPrefsQueries.getByTitleKey(key).executeAsOneOrNull())
        assertNull(repo.getPrefs(key))

        driver.close()
    }
}
