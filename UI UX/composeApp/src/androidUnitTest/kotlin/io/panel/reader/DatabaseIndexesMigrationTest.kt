package io.panel.reader

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import data.db.DatabaseDriverFactory
import io.panel.reader.db.PanelDatabase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DatabaseIndexesMigrationTest {

    private val dbName = "test_db_indexes_migration.db"

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
        context.deleteDatabase("dokuha.db")
    }

    private fun seedV3Database(context: Context) {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 3) {
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
                db.execSQL(
                    """
                    CREATE TABLE reader_prefs (
                        title_key TEXT PRIMARY KEY NOT NULL,
                        reading_mode TEXT,
                        color_filter TEXT,
                        side_padding REAL,
                        split_double_spread INTEGER,
                        dual_page_offset INTEGER,
                        novel_theme TEXT,
                        novel_font TEXT,
                        novel_font_size REAL,
                        novel_line_height REAL,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO mangas (id, source_id, url, title, favorite) VALUES (1, 7, 'manga/solo', 'Solo Leveling', 1)")
                db.execSQL("INSERT INTO chapters (id, manga_id, url, name, chapter_number, source_order, date_fetch) VALUES (10, 1, 'ch/1', 'Ch 1', 1.0, 0, 1000)")
                db.execSQL("INSERT INTO chapters (id, manga_id, url, name, chapter_number, source_order, date_fetch) VALUES (11, 1, 'ch/2', 'Ch 2', 2.0, 1, 2000)")
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.close()
        helper.close()
    }

    @Test
    fun upgradeFromV3CreatesCompositeIndexesAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
        seedV3Database(context)

        val driver = AndroidSqliteDriver(
            schema = PanelDatabase.Schema,
            context = context,
            name = dbName,
            callback = object : AndroidSqliteDriver.Callback(PanelDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.enableWriteAheadLogging()
                }
            }
        )
        val db = PanelDatabase(driver)

        val indexes = mutableSetOf<String>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type = 'index'",
            mapper = { cursor ->
                while (cursor.next().value) {
                    cursor.getString(0)?.let { indexes.add(it) }
                }
                app.cash.sqldelight.db.QueryResult.Unit
            },
            parameters = 0
        )

        assertTrue("mangas_favorite_title_idx" in indexes)
        assertTrue("chapters_manga_id_order_idx" in indexes)
        assertTrue("chapters_date_fetch_idx" in indexes)

        val library = db.mangaQueries.getLibrary().executeAsList()
        assertEquals(1, library.size)
        assertEquals("Solo Leveling", library.first().title)

        val chapters = db.chapterQueries.getChaptersByMangaId(1L).executeAsList()
        assertEquals(listOf("Ch 2", "Ch 1"), chapters.map { it.name })

        val updates = db.chapterQueries.getRecentLibraryUpdates(10L).executeAsList()
        assertEquals(2, updates.size)
        assertEquals("Ch 2", updates.first().chapter_name)

        driver.close()
    }

    @Test
    fun androidDriverFactoryEnablesWalMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("dokuha.db")
        val driver = DatabaseDriverFactory(context).createDriver()
        var journalMode: String? = null
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA journal_mode",
            mapper = { cursor ->
                if (cursor.next().value) {
                    journalMode = cursor.getString(0)
                }
                app.cash.sqldelight.db.QueryResult.Unit
            },
            parameters = 0
        )
        assertEquals("wal", journalMode?.lowercase())
        driver.close()
    }
}
