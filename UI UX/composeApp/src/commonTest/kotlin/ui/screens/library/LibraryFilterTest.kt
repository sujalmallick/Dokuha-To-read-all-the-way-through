package ui.screens.library

import core.source.MangaStatus
import core.source.SourceManga
import data.mock.LibraryMangaStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryFilterTest {

    private val sampleMangas = listOf(
        SourceManga(
            url = "manga/solo-leveling",
            title = "Solo Leveling",
            genres = listOf("Action", "Fantasy", "Manhwa"),
            status = MangaStatus.COMPLETED,
            isNovel = false
        ),
        SourceManga(
            url = "manga/one-piece",
            title = "One Piece",
            genres = listOf("Action", "Adventure", "Comedy"),
            status = MangaStatus.ONGOING,
            isNovel = false
        ),
        SourceManga(
            url = "novel/shadow-slave",
            title = "Shadow Slave",
            genres = listOf("Fantasy", "Supernatural"),
            status = MangaStatus.ONGOING,
            isNovel = true
        ),
        SourceManga(
            url = "manga/tower-of-god",
            title = "Tower of God",
            genres = listOf("Action", "Mystery", "Webtoon"),
            status = MangaStatus.ONGOING,
            isNovel = false
        )
    )

    private val sampleStats = mapOf(
        "manga/solo-leveling" to LibraryMangaStats(
            mangaUrl = "manga/solo-leveling",
            totalChapters = 179,
            readChapters = 179,
            lastReadAt = 3000L,
            dateAdded = 100L
        ),
        "manga/one-piece" to LibraryMangaStats(
            mangaUrl = "manga/one-piece",
            totalChapters = 1100,
            readChapters = 500,
            lastReadAt = 5000L,
            dateAdded = 400L
        ),
        "novel/shadow-slave" to LibraryMangaStats(
            mangaUrl = "novel/shadow-slave",
            totalChapters = 1500,
            readChapters = 0,
            lastReadAt = 0L,
            dateAdded = 200L
        ),
        "manga/tower-of-god" to LibraryMangaStats(
            mangaUrl = "manga/tower-of-god",
            totalChapters = 600,
            readChapters = 100,
            lastReadAt = 4000L,
            dateAdded = 300L
        )
    )

    @Test
    fun testDefaultFilterReturnsAllUnmodified() {
        val filter = LibraryFilter()
        assertFalse(filter.isActive)
        assertEquals(0, filter.activeFilterCount)

        val result = filterAndSortLibrary(sampleMangas, filter, sampleStats)
        assertEquals(sampleMangas.size, result.size)
        // Default sort is Title ASC: One Piece, Shadow Slave, Solo Leveling, Tower of God
        assertEquals("One Piece", result[0].title)
        assertEquals("Shadow Slave", result[1].title)
        assertEquals("Solo Leveling", result[2].title)
        assertEquals("Tower of God", result[3].title)
    }

    @Test
    fun testStatusFilter() {
        val ongoingFilter = LibraryFilter(status = MangaStatusFilter.ONGOING)
        assertTrue(ongoingFilter.isActive)
        assertEquals(1, ongoingFilter.activeFilterCount)

        val ongoingResult = filterAndSortLibrary(sampleMangas, ongoingFilter, sampleStats)
        assertEquals(3, ongoingResult.size)
        assertTrue(ongoingResult.all { it.status == MangaStatus.ONGOING })

        val completedFilter = LibraryFilter(status = MangaStatusFilter.COMPLETED)
        val completedResult = filterAndSortLibrary(sampleMangas, completedFilter, sampleStats)
        assertEquals(1, completedResult.size)
        assertEquals("Solo Leveling", completedResult[0].title)
    }

    @Test
    fun testFormatFilter() {
        // Novel filter
        val novelFilter = LibraryFilter(contentType = ContentTypeFilter.NOVEL)
        val novels = filterAndSortLibrary(sampleMangas, novelFilter, sampleStats)
        assertEquals(1, novels.size)
        assertEquals("Shadow Slave", novels[0].title)

        // Manhwa filter (matches genre "Manhwa" or "Webtoon")
        val manhwaFilter = LibraryFilter(contentType = ContentTypeFilter.MANHWA)
        val manhwas = filterAndSortLibrary(sampleMangas, manhwaFilter, sampleStats)
        assertEquals(2, manhwas.size)
        val manhwaTitles = manhwas.map { it.title }.toSet()
        assertTrue("Solo Leveling" in manhwaTitles)
        assertTrue("Tower of God" in manhwaTitles)

        // Manga filter (non-novel, non-manhwa)
        val mangaFilter = LibraryFilter(contentType = ContentTypeFilter.MANGA)
        val mangas = filterAndSortLibrary(sampleMangas, mangaFilter, sampleStats)
        assertEquals(1, mangas.size)
        assertEquals("One Piece", mangas[0].title)
    }

    @Test
    fun testReadStatusFilter() {
        // Unread
        val unreadFilter = LibraryFilter(readStatus = ReadStatusFilter.UNREAD)
        val unread = filterAndSortLibrary(sampleMangas, unreadFilter, sampleStats)
        assertEquals(1, unread.size)
        assertEquals("Shadow Slave", unread[0].title)

        // In Progress
        val inProgressFilter = LibraryFilter(readStatus = ReadStatusFilter.IN_PROGRESS)
        val inProgress = filterAndSortLibrary(sampleMangas, inProgressFilter, sampleStats)
        assertEquals(2, inProgress.size)
        val inProgressTitles = inProgress.map { it.title }.toSet()
        assertTrue("One Piece" in inProgressTitles)
        assertTrue("Tower of God" in inProgressTitles)

        // Completed
        val completedFilter = LibraryFilter(readStatus = ReadStatusFilter.COMPLETED)
        val completed = filterAndSortLibrary(sampleMangas, completedFilter, sampleStats)
        assertEquals(1, completed.size)
        assertEquals("Solo Leveling", completed[0].title)
    }

    @Test
    fun testSorting() {
        // Last Read DESC (most recent first: One Piece [5000], Tower of God [4000], Solo Leveling [3000], Shadow Slave [0])
        val lastReadFilter = LibraryFilter(sortBy = LibrarySort.LAST_READ, sortAscending = false)
        val sortedByLastRead = filterAndSortLibrary(sampleMangas, lastReadFilter, sampleStats)
        assertEquals("One Piece", sortedByLastRead[0].title)
        assertEquals("Tower of God", sortedByLastRead[1].title)
        assertEquals("Solo Leveling", sortedByLastRead[2].title)
        assertEquals("Shadow Slave", sortedByLastRead[3].title)

        // Total Chapters DESC (Shadow Slave [1500], One Piece [1100], Tower of God [600], Solo Leveling [179])
        val totalChaptersFilter = LibraryFilter(sortBy = LibrarySort.TOTAL_CHAPTERS, sortAscending = false)
        val sortedByChapters = filterAndSortLibrary(sampleMangas, totalChaptersFilter, sampleStats)
        assertEquals("Shadow Slave", sortedByChapters[0].title)
        assertEquals("One Piece", sortedByChapters[1].title)
        assertEquals("Tower of God", sortedByChapters[2].title)
        assertEquals("Solo Leveling", sortedByChapters[3].title)

        // Date Added ASC (Solo Leveling [100], Shadow Slave [200], Tower of God [300], One Piece [400])
        val dateAddedFilter = LibraryFilter(sortBy = LibrarySort.DATE_ADDED, sortAscending = true)
        val sortedByDate = filterAndSortLibrary(sampleMangas, dateAddedFilter, sampleStats)
        assertEquals("Solo Leveling", sortedByDate[0].title)
        assertEquals("Shadow Slave", sortedByDate[1].title)
        assertEquals("Tower of God", sortedByDate[2].title)
        assertEquals("One Piece", sortedByDate[3].title)
    }

    @Test
    fun testDownloadedOnlyFilter() {
        val downloadedUrls = setOf("manga/solo-leveling", "manga/one-piece")
        val filter = LibraryFilter(downloadedOnly = true)

        val result = filterAndSortLibrary(sampleMangas, filter, sampleStats) { manga ->
            manga.url in downloadedUrls
        }
        assertEquals(2, result.size)
        assertTrue(result.all { it.url in downloadedUrls })
    }
}
