package core.extensions.lnreader

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LNReaderSourceAdapterTest {

    private val mockLNReaderScript = """
        const plugin = {
            id: "wuxiaworld",
            name: "WuxiaWorld",
            site: "https://www.wuxiaworld.com",
            lang: "English",
            version: "1.0.0",

            async popularNovels(page, { showLatestNovels }) {
                return [
                    { name: showLatestNovels ? "Latest Coiling Dragon" : "Popular Coiling Dragon", path: "novel/coiling-dragon", cover: "https://cdn.example.com/cd.jpg" }
                ];
            },

            async searchNovels(query, page) {
                return [
                    { name: "Result for " + query, path: "novel/search-res", cover: "https://cdn.example.com/res.jpg" }
                ];
            },

            async parseNovel(novelPath) {
                return {
                    name: "Coiling Dragon",
                    cover: "https://cdn.example.com/cd.jpg",
                    summary: "Empires rise and fall on the Yulan Continent.",
                    author: "I Eat Tomatoes",
                    status: "Completed",
                    genres: ["Action", "Fantasy", "Xianxia"],
                    chapters: [
                        { name: "Book 1 Chapter 1", path: "novel/cd/b1-c1", chapterNumber: 1 }
                    ]
                };
            },

            // PagePlugin pagination emulation: page 2 returns Chapter 2; page 3 returns empty
            async parsePage(novelPath, page) {
                if (page === 2) {
                    return [
                        { name: "Book 1 Chapter 2", path: "novel/cd/b1-c2", chapterNumber: 2 }
                    ];
                }
                return [];
            },

            async parseChapter(chapterPath) {
                return "<div><p>Linley took a deep breath as the wind howled across Mount Wushan.</p></div>";
            }
        };

        module.exports = plugin;
    """.trimIndent()

    @Test
    fun testLNReaderLifecycleAndPagePluginPagination() = runBlocking {
        val pool = JsEnginePool(maxActiveEngines = 3)
        val bridge = JsBridge()

        val manifest = LNReaderManifest(
            id = "wuxiaworld",
            name = "WuxiaWorld",
            site = "https://www.wuxiaworld.com",
            lang = "en",
            version = "1.0.0",
            script = mockLNReaderScript
        )

        val adapter = LNReaderSourceAdapter(manifest, pool, bridge)

        // 1. Popular & Latest
        val popular = adapter.getPopular(1)
        assertEquals(1, popular.mangas.size)
        assertEquals("Popular Coiling Dragon", popular.mangas[0].title)

        val latest = adapter.getLatestUpdates(1)
        assertEquals("Latest Coiling Dragon", latest.mangas[0].title)

        // 2. Search
        val search = adapter.search("Linley", 1)
        assertEquals("Result for Linley", search.mangas[0].title)

        // 3. Details
        val manga = SourceManga(url = "novel/coiling-dragon", title = "Coiling Dragon")
        val details = adapter.getMangaDetails(manga)
        assertEquals("Coiling Dragon", details.title)
        assertEquals("Empires rise and fall on the Yulan Continent.", details.description)
        assertEquals("I Eat Tomatoes", details.author)
        assertEquals(MangaStatus.COMPLETED, details.status)
        assertTrue(details.genres.contains("Xianxia"))

        // 4. Chapters via PagePlugin multi-page iteration (retrieves page 1 from parseNovel + page 2 from parsePage)
        val chapters = adapter.getChapterList(manga)
        assertEquals(2, chapters.size)
        assertEquals("Book 1 Chapter 1", chapters[0].name)
        assertEquals("Book 1 Chapter 2", chapters[1].name)

        // 5. Chapter text
        val content = adapter.getChapterContent(SourceChapter(url = "novel/cd/b1-c1", name = "Chapter 1"))
        assertTrue(content.htmlBody.contains("Mount Wushan"))
        assertEquals("Linley took a deep breath as the wind howled across Mount Wushan.", content.plainText)

        pool.close()
    }
}
