package ui.browse

import core.source.MangaStatus
import data.mock.BrowseFilters
import data.mock.BrowseSortOrder
import data.mock.FakePanelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowseSearchEngineTest {

    private val repository = FakePanelRepository()

    @Test
    fun testEmptySearchReturnsAllCatalogTitles() = runTest {
        val results = repository.searchManga(query = "", filters = BrowseFilters()).first()
        assertTrue(results.isNotEmpty(), "Default search should return catalog items")
        assertTrue(results.size >= 10, "Default catalog should have at least 10 items")
    }

    @Test
    fun testCaseInsensitiveTitleSearch() = runTest {
        val lowerResults = repository.searchManga(query = "solo").first()
        assertEquals(1, lowerResults.size)
        assertEquals("Solo Leveling", lowerResults.first().title)

        val upperResults = repository.searchManga(query = "SOLO").first()
        assertEquals(1, upperResults.size)
        assertEquals("Solo Leveling", upperResults.first().title)

        val partialResults = repository.searchManga(query = "horiz").first()
        assertTrue(partialResults.any { it.title == "The Last Horizon" })
    }

    @Test
    fun testGenreFilterReturnsMatchingTitles() = runTest {
        val fantasyFilters = BrowseFilters(genres = setOf("Fantasy"))
        val fantasyResults = repository.searchManga(filters = fantasyFilters).first()

        assertTrue(fantasyResults.isNotEmpty())
        assertTrue(fantasyResults.all { manga ->
            manga.genres.any { it.equals("Fantasy", ignoreCase = true) }
        }, "All results must contain the Fantasy genre")
    }

    @Test
    fun testStatusFilterOngoingAndCompleted() = runTest {
        val completedFilters = BrowseFilters(status = MangaStatus.COMPLETED)
        val completedResults = repository.searchManga(filters = completedFilters).first()

        assertTrue(completedResults.isNotEmpty())
        assertTrue(completedResults.all { it.status == MangaStatus.COMPLETED })

        val ongoingFilters = BrowseFilters(status = MangaStatus.ONGOING)
        val ongoingResults = repository.searchManga(filters = ongoingFilters).first()

        assertTrue(ongoingResults.isNotEmpty())
        assertTrue(ongoingResults.all { it.status == MangaStatus.ONGOING })
    }

    @Test
    fun testNovelFilterSeparatesNovelsFromComics() = runTest {
        val novelFilters = BrowseFilters(isNovel = true)
        val novelResults = repository.searchManga(filters = novelFilters).first()

        assertTrue(novelResults.isNotEmpty())
        assertTrue(novelResults.all { it.isNovel })
        assertTrue(novelResults.any { it.title.contains("Beginning After the End") })

        val comicFilters = BrowseFilters(isNovel = false)
        val comicResults = repository.searchManga(filters = comicFilters).first()

        assertTrue(comicResults.isNotEmpty())
        assertTrue(comicResults.all { !it.isNovel })
    }

    @Test
    fun testAlphabeticalSortingOrder() = runTest {
        val alphaFilters = BrowseFilters(sortBy = BrowseSortOrder.ALPHABETICAL)
        val results = repository.searchManga(filters = alphaFilters).first()

        assertTrue(results.size > 1)
        for (i in 0 until results.lastIndex) {
            val current = results[i].title.lowercase()
            val next = results[i + 1].title.lowercase()
            assertTrue(current <= next, "Expected '$current' to precede or equal '$next'")
        }
    }

    @Test
    fun testSourceScopedSearch() = runTest {
        val source1Results = repository.searchManga(sourceId = "1").first()
        val source3Results = repository.searchManga(sourceId = "3").first()

        assertTrue(source1Results.isNotEmpty())
        assertTrue(source3Results.isNotEmpty())

        // Verify source specific title inclusion
        assertTrue(source1Results.any { it.title == "MangaDex" || it.title == "The Last Horizon" })
        assertTrue(source3Results.any { it.title == "Asura Scans" || it.title == "The Last Horizon" || it.title == "Wind Beneath" })
    }

    @Test
    fun testSourceCataloguePagination() = runTest {
        val page1 = repository.getSourceCatalogue(sourceId = "1", page = 1).first()
        val page2 = repository.getSourceCatalogue(sourceId = "1", page = 2).first()

        assertEquals(4, page1.size, "Page size should be 4")
        assertTrue(page2.isNotEmpty(), "Page 2 should contain remaining items")

        // Ensure no overlap between pages
        val page1Titles = page1.map { it.title }.toSet()
        val page2Titles = page2.map { it.title }.toSet()
        assertTrue(page1Titles.intersect(page2Titles).isEmpty(), "Page 1 and Page 2 should not have overlapping items")

        // Out of bounds page
        val emptyPage = repository.getSourceCatalogue(sourceId = "1", page = 99).first()
        assertTrue(emptyPage.isEmpty(), "Out of bounds page should return empty list cleanly")
    }

    @Test
    fun testCombinedQueryAndFilterNarrowing() = runTest {
        val combinedFilters = BrowseFilters(
            genres = setOf("Action"),
            status = MangaStatus.COMPLETED
        )
        val results = repository.searchManga(query = "Solo", filters = combinedFilters).first()

        assertEquals(1, results.size)
        assertEquals("Solo Leveling", results.first().title)

        // Non-existent combination
        val impossibleResults = repository.searchManga(query = "NonExistentZXYZ", filters = combinedFilters).first()
        assertTrue(impossibleResults.isEmpty())
    }
}
