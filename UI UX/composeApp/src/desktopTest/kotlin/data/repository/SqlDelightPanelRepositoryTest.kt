package data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import core.source.MangaStatus
import core.source.SourceManga
import io.panel.reader.db.PanelDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightPanelRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: PanelDatabase
    private lateinit var repository: SqlDelightPanelRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PanelDatabase.Schema.create(driver)
        database = PanelDatabase(driver)
        repository = SqlDelightPanelRepository(database)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun testInitialLibraryIsEmptyForCleanUser() = runBlocking {
        val library = repository.getLibrary("All").first()
        assertTrue(library.isEmpty(), "Clean user should start with an empty library without fake entries")
    }

    @Test
    fun testToggleFavoritePersistence() = runBlocking {
        val customManga = SourceManga(
            url = "manga/test-custom-title",
            title = "Custom Adventure",
            author = "Test Author",
            description = "A test manga for persistence verification.",
            genres = listOf("Action", "Fantasy"),
            status = MangaStatus.ONGOING,
            thumbnailUrl = "https://example.com/cover.jpg",
            isNovel = false
        )

        // 1. Add to library (favorite = true)
        repository.toggleFavorite(customManga, isFavorite = true)

        var library = repository.getLibrary("All").first()
        val found = library.find { it.url == customManga.url }
        assertNotNull(found, "Custom manga should exist in library after favoriting")
        assertEquals("Custom Adventure", found.title)
        assertEquals("Test Author", found.author)

        // 2. Remove from library (favorite = false)
        repository.toggleFavorite(customManga, isFavorite = false)

        library = repository.getLibrary("All").first()
        assertFalse(library.any { it.url == customManga.url }, "Custom manga should not be in library after unfavoriting")
    }

    @Test
    fun testChapterProgressAndContinueReading() = runBlocking {
        val manga = SourceManga(
            url = "manga/progress-test",
            title = "Progress Manga",
            status = MangaStatus.ONGOING
        )
        repository.toggleFavorite(manga, isFavorite = true)

        val ch1 = "manga/progress-test/ch-1"
        // Seed chapter in DB
        val mangaEntity = database.mangaQueries.getMangaByUrl(manga.url).executeAsOne()
        database.chapterQueries.insertOrUpdateChapter(
            id = null,
            mangaId = mangaEntity.id,
            url = ch1,
            name = "Chapter 1",
            scanlator = null,
            read = 0L,
            bookmark = 0L,
            lastPageRead = 0L,
            chapterNumber = 1.0,
            sourceOrder = 0L,
            dateFetch = 0L,
            dateUpload = 0L,
            lastModifiedAt = 0L
        )

        // Update progress
        repository.updateChapterProgress(chapterUrl = ch1, page = 15, isRead = true)

        val updatedCh = database.chapterQueries.getChapterByUrl(ch1).executeAsOne()
        assertEquals(1L, updatedCh.read)
        assertEquals(15L, updatedCh.last_page_read)

        // Verify continue reading
        val continueReading = repository.getContinueReading().first()
        assertNotNull(continueReading)
        assertEquals(manga.url, continueReading.first.url)
        assertEquals(ch1, continueReading.second.url)
        assertEquals(1.0f, continueReading.second.chapterNumber)
    }
}
