package data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import core.source.ComicSource
import core.source.MangaStatus
import core.source.SourceManga
import io.panel.reader.db.PanelDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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

    @Test
    fun testRecordHistoryAndGetHistory() = runBlocking {
        val manga = SourceManga(
            url = "manga/history-test",
            title = "History Manga",
            thumbnailUrl = "https://example.com/cover.jpg",
            status = MangaStatus.ONGOING
        )
        repository.toggleFavorite(manga, isFavorite = true)

        val ch1 = "manga/history-test/ch-1"
        val mangaEntity = database.mangaQueries.getMangaByUrl(manga.url).executeAsOne()
        database.chapterQueries.insertOrUpdateChapter(
            id = null,
            mangaId = mangaEntity.id,
            url = ch1,
            name = "Chapter 1: The Beginning",
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

        // Record history
        repository.recordHistory(ch1)

        val historyGroups = repository.getHistory().first()
        assertTrue(historyGroups.isNotEmpty(), "History groups should not be empty")
        val todayGroup = historyGroups.firstOrNull { it.title == "Today" }
        assertNotNull(todayGroup, "Should have a Today group")
        assertTrue(todayGroup.items.any { it.manga.url == manga.url && it.chapter.url == ch1 })
    }

    @Test
    fun testDynamicSourceReactiveFlowsAndGlobalSearch() = runBlocking {
        // Initially no sources
        val initialSources = repository.getSources().first()
        assertTrue(initialSources.isEmpty(), "Should have no sources initially")

        val dummySource = object : core.source.ComicSource {
            override val id: Long = 999L
            override val name: String = "Dynamic Test Source"
            override val baseUrl: String = "https://dynamic.example.com"
            override val lang: String = "en"
            override val isNsfw: Boolean = false

            override suspend fun getPopular(page: Int): core.source.MangasPage = core.source.MangasPage(
                mangas = listOf(
                    SourceManga(url = "m1", title = "Dynamic Popular Title", isNovel = false)
                ),
                hasNextPage = false
            )

            override suspend fun getLatestUpdates(page: Int): core.source.MangasPage = core.source.MangasPage(
                mangas = listOf(
                    SourceManga(url = "m2", title = "Dynamic Latest Title", isNovel = false)
                ),
                hasNextPage = false
            )

            override suspend fun search(query: String, page: Int, filters: List<core.source.SourceFilter>): core.source.MangasPage = core.source.MangasPage(
                mangas = listOf(
                    SourceManga(url = "m1", title = "Dynamic Popular Title", isNovel = false)
                ).filter { it.title.contains(query, ignoreCase = true) },
                hasNextPage = false
            )

            override suspend fun getMangaDetails(manga: SourceManga): SourceManga = manga
            override suspend fun getChapterList(manga: SourceManga): List<core.source.SourceChapter> = emptyList()
            override suspend fun getPageList(chapter: core.source.SourceChapter): List<core.source.SourcePage> = emptyList()
        }

        // Register dynamic source
        repository.registerSource(dummySource)

        // Verify getSources reactive emission
        val updatedSources = repository.getSources().first()
        assertEquals(1, updatedSources.size)
        assertEquals("Dynamic Test Source", updatedSources.first().name)

        // Global search with sourceId = null queries dynamic source
        val searchResults = repository.searchManga(query = "Dynamic", sourceId = null).first()
        assertEquals(1, searchResults.size)
        assertEquals("Dynamic Popular Title", searchResults.first().title)

        // Get catalogue for popular
        val popular = repository.getSourceCatalogue(sourceId = "999", page = 1).first()
        assertEquals(1, popular.size)
        assertEquals("Dynamic Popular Title", popular.first().title)

        // Get catalogue for latest
        val latest = repository.getSourceCatalogue(
            sourceId = "999",
            page = 1,
            filters = data.mock.BrowseFilters(sortBy = data.mock.BrowseSortOrder.LATEST)
        ).first()
        assertEquals(1, latest.size)
        assertEquals("Dynamic Latest Title", latest.first().title)

        // Unregister source
        repository.unregisterSource(999L)
        val emptySources = repository.getSources().first()
        assertTrue(emptySources.isEmpty(), "Should be empty after unregistering")
    }

    @Test
    fun testSourceIdDirectRoutingAndFallbackTitle() = runBlocking {
        val sourceA = object : ComicSource {
            override val id: Long = 1001L
            override val name: String = "Source Alpha"
            override val baseUrl: String = "https://alpha.example.com"
            override val lang: String = "en"
            override val isNsfw: Boolean = false
            override suspend fun getPopular(page: Int) = core.source.MangasPage(emptyList(), false)
            override suspend fun getLatestUpdates(page: Int) = core.source.MangasPage(emptyList(), false)
            override suspend fun search(query: String, page: Int, filters: List<core.source.SourceFilter>) = core.source.MangasPage(emptyList(), false)
            override suspend fun getMangaDetails(manga: SourceManga) = manga.copy(title = "Alpha Resolved Title", description = "Alpha Description")
            override suspend fun getChapterList(manga: SourceManga) = listOf(
                core.source.SourceChapter(url = "ch-alpha-1", name = "Alpha Chapter 1", chapterNumber = 1f)
            )
            override suspend fun getPageList(chapter: core.source.SourceChapter) = emptyList<core.source.SourcePage>()
        }

        val sourceB = object : ComicSource {
            override val id: Long = 1002L
            override val name: String = "Source Beta"
            override val baseUrl: String = "https://beta.example.com"
            override val lang: String = "en"
            override val isNsfw: Boolean = false
            override suspend fun getPopular(page: Int) = core.source.MangasPage(emptyList(), false)
            override suspend fun getLatestUpdates(page: Int) = core.source.MangasPage(emptyList(), false)
            override suspend fun search(query: String, page: Int, filters: List<core.source.SourceFilter>) = core.source.MangasPage(emptyList(), false)
            override suspend fun getMangaDetails(manga: SourceManga) = manga.copy(title = "Beta Resolved Title", description = "Beta Description")
            override suspend fun getChapterList(manga: SourceManga) = listOf(
                core.source.SourceChapter(url = "ch-beta-1", name = "Beta Chapter 1", chapterNumber = 1f)
            )
            override suspend fun getPageList(chapter: core.source.SourceChapter) = emptyList<core.source.SourcePage>()
        }

        repository.registerSource(sourceA)
        repository.registerSource(sourceB)

        // Given a relative URL that doesn't start with either baseUrl:
        val relativeUrl = "/comics/shared-relative-slug-123"

        // 1. Direct routing to Source Beta using sourceId="1002"
        val emissions = repository.getMangaDetails(relativeUrl, sourceId = "1002", fallbackTitle = "Fallback Beta").take(2).toList()
        assertEquals(2, emissions.size)
        assertEquals("Fallback Beta", emissions[0]?.title, "Initial emission must be the fallback title")
        assertEquals("Beta Resolved Title", emissions[1]?.title, "Second emission must be the fresh resolved title")
        assertEquals("Beta Description", emissions[1]?.description)

        val betaChapters = repository.getChapters(relativeUrl, sourceId = "1002").first()
        assertEquals(1, betaChapters.size)
        assertEquals("Beta Chapter 1", betaChapters.first().name)

        // 2. Fallback title preservation when scraper throws
        val failingSource = object : ComicSource {
            override val id: Long = 1003L
            override val name: String = "Failing Source"
            override val baseUrl: String = "https://fail.example.com"
            override val lang: String = "en"
            override val isNsfw: Boolean = false
            override suspend fun getPopular(page: Int) = core.source.MangasPage(emptyList(), false)
            override suspend fun getLatestUpdates(page: Int) = core.source.MangasPage(emptyList(), false)
            override suspend fun search(query: String, page: Int, filters: List<core.source.SourceFilter>) = core.source.MangasPage(emptyList(), false)
            override suspend fun getMangaDetails(manga: SourceManga): SourceManga {
                throw RuntimeException("Network / Scraper Failure")
            }
            override suspend fun getChapterList(manga: SourceManga) = emptyList<core.source.SourceChapter>()
            override suspend fun getPageList(chapter: core.source.SourceChapter) = emptyList<core.source.SourcePage>()
        }
        repository.registerSource(failingSource)

        val fallbackManga = repository.getMangaDetails("/comics/failing-manga", sourceId = "1003", fallbackTitle = "Known Title Keep Alive").first()
        assertNotNull(fallbackManga)
        assertEquals("Known Title Keep Alive", fallbackManga.title, "Fallback title must be preserved when scraper fails")

        repository.unregisterSource(1001L)
        repository.unregisterSource(1002L)
        repository.unregisterSource(1003L)
    }

    @Test
    fun testCompositeUniqueConstraintsAllowSharedRelativeChapterSlugs() = runBlocking {
        // Insert Manga 1
        database.mangaQueries.insertOrUpdateManga(
            id = 101L,
            sourceId = 1L,
            url = "manga-one",
            title = "Manga One",
            artist = null,
            author = null,
            description = null,
            genres = null,
            status = "ONGOING",
            thumbnailUrl = null,
            favorite = 0L,
            lastUpdate = 0L,
            nextUpdate = 0L,
            initialized = 1L,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            dateAdded = 0L,
            isNovel = 0L
        )

        // Insert Manga 2 from different source but same relative url "manga-one"
        database.mangaQueries.insertOrUpdateManga(
            id = 102L,
            sourceId = 2L,
            url = "manga-one",
            title = "Manga One from Source 2",
            artist = null,
            author = null,
            description = null,
            genres = null,
            status = "ONGOING",
            thumbnailUrl = null,
            favorite = 0L,
            lastUpdate = 0L,
            nextUpdate = 0L,
            initialized = 1L,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            dateAdded = 0L,
            isNovel = 0L
        )

        // Both manga records must coexist cleanly due to UNIQUE(source_id, url)
        val m1 = database.mangaQueries.getMangaBySourceAndUrl(1L, "manga-one").executeAsOneOrNull()
        val m2 = database.mangaQueries.getMangaBySourceAndUrl(2L, "manga-one").executeAsOneOrNull()
        assertNotNull(m1)
        assertNotNull(m2)
        assertEquals(101L, m1.id)
        assertEquals(102L, m2.id)

        // Insert identical relative slug "/chapter-1" for Manga 1
        database.chapterQueries.insertOrUpdateChapter(
            id = null,
            mangaId = 101L,
            url = "/chapter-1",
            name = "Chapter 1 for Manga 1",
            scanlator = "Scan 1",
            read = 0L,
            bookmark = 0L,
            lastPageRead = 0L,
            chapterNumber = 1.0,
            sourceOrder = 0L,
            dateFetch = 0L,
            dateUpload = 0L,
            lastModifiedAt = 0L
        )

        // Insert identical relative slug "/chapter-1" for Manga 2
        database.chapterQueries.insertOrUpdateChapter(
            id = null,
            mangaId = 102L,
            url = "/chapter-1",
            name = "Chapter 1 for Manga 2",
            scanlator = "Scan 2",
            read = 0L,
            bookmark = 0L,
            lastPageRead = 0L,
            chapterNumber = 1.0,
            sourceOrder = 0L,
            dateFetch = 0L,
            dateUpload = 0L,
            lastModifiedAt = 0L
        )

        // Verify both chapters exist independently without collision due to UNIQUE(manga_id, url)
        val ch1 = database.chapterQueries.getChapterByMangaIdAndUrl(101L, "/chapter-1").executeAsOneOrNull()
        val ch2 = database.chapterQueries.getChapterByMangaIdAndUrl(102L, "/chapter-1").executeAsOneOrNull()

        assertNotNull(ch1)
        assertNotNull(ch2)
        assertEquals("Chapter 1 for Manga 1", ch1.name)
        assertEquals("Chapter 1 for Manga 2", ch2.name)
        assertEquals(101L, ch1.manga_id)
        assertEquals(102L, ch2.manga_id)
    }

    @Test
    fun testBrowseMangaResolutionWithAndWithoutSourceId() = runBlocking {
        val testSource = object : ComicSource {
            override val id: Long = 2001L
            override val name: String = "Asura Scans"
            override val baseUrl: String = "https://asurascans.com"
            override val lang: String = "en"
            override val isNsfw: Boolean = false
            override suspend fun getPopular(page: Int) = core.source.MangasPage(
                listOf(
                    SourceManga(url = "/comics/bad-born-blood", title = "Bad Born Blood", isNovel = false)
                ),
                false
            )
            override suspend fun getLatestUpdates(page: Int) = core.source.MangasPage(emptyList(), false)
            override suspend fun search(query: String, page: Int, filters: List<core.source.SourceFilter>) = core.source.MangasPage(
                listOf(
                    SourceManga(url = "/comics/bad-born-blood", title = "Bad Born Blood", isNovel = false)
                ),
                false
            )
            override suspend fun getMangaDetails(manga: SourceManga) = manga.copy(
                title = "Bad Born Blood (Resolved)",
                description = "Deep in the slums, a monster was born.",
                genres = listOf("Action", "Fantasy")
            )
            override suspend fun getChapterList(manga: SourceManga) = listOf(
                core.source.SourceChapter(url = "/comics/bad-born-blood/chapter-1", name = "Chapter 1", chapterNumber = 1f),
                core.source.SourceChapter(url = "/comics/bad-born-blood/chapter-2", name = "Chapter 2", chapterNumber = 2f)
            )
            override suspend fun getPageList(chapter: core.source.SourceChapter) = emptyList<core.source.SourcePage>()
        }

        repository.registerSource(testSource)

        // 1. Verify getTrending() stamps the manga with sourceId
        val trending = repository.getTrending().first()
        assertTrue(trending.isNotEmpty(), "Trending should not be empty")
        val trendingManga = trending.first()
        assertEquals(2001L, trendingManga.sourceId, "Trending manga must carry its sourceId")

        // 2. Open manga with sourceId (as BrowseScreen now does)
        val detailsWithSourceId = repository.getMangaDetails(
            url = trendingManga.url,
            sourceId = trendingManga.sourceId.toString(),
            fallbackTitle = trendingManga.title
        ).take(2).toList()

        val resolvedManga = detailsWithSourceId.last()
        assertNotNull(resolvedManga)
        assertEquals("Bad Born Blood (Resolved)", resolvedManga.title)
        assertEquals("Deep in the slums, a monster was born.", resolvedManga.description)

        val chaptersWithSourceId = repository.getChapters(
            mangaUrl = trendingManga.url,
            sourceId = trendingManga.sourceId.toString()
        ).first()
        assertEquals(2, chaptersWithSourceId.size)
        assertEquals("Chapter 2", chaptersWithSourceId.first().name)
        assertEquals("Chapter 1", chaptersWithSourceId.last().name)

        // 3. Open manga WITHOUT sourceId (via single installed source fallback)
        val detailsWithoutSourceId = repository.getMangaDetails(
            url = "/comics/another-relative-title",
            sourceId = null,
            fallbackTitle = "Fallback"
        ).take(2).toList()

        val resolvedWithoutId = detailsWithoutSourceId.last()
        assertNotNull(resolvedWithoutId)
        assertEquals("Deep in the slums, a monster was born.", resolvedWithoutId.description)

        val chaptersWithoutSourceId = repository.getChapters(
            mangaUrl = "/comics/another-relative-title",
            sourceId = null
        ).first()
        assertEquals(2, chaptersWithoutSourceId.size)

        repository.unregisterSource(2001L)
    }
}
