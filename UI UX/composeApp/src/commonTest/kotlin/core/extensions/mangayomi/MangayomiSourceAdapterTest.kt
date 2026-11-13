package core.extensions.mangayomi

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MangayomiSourceAdapterTest {

    private val mockMangayomiScript = """
        class DefaultExtension extends MProvider {
            constructor() {
                super();
                this.detailCallCount = 0;
            }
            async getPopular(page) {
                return {
                    list: [
                        { name: "Asura Title", link: "/manga/asura-1", imageUrl: "https://cdn.example.com/cover1.jpg" }
                    ],
                    hasNextPage: true
                };
            }
            async getLatestUpdates(page) {
                return {
                    list: [
                        { name: "Latest Asura", link: "/manga/asura-latest", imageUrl: "https://cdn.example.com/cover2.jpg" }
                    ],
                    hasNextPage: false
                };
            }
            async search(query, page, filters) {
                return {
                    list: [
                        { name: "Found " + query, link: "/manga/found-1", imageUrl: "https://cdn.example.com/found.jpg" }
                    ],
                    hasNextPage: false
                };
            }
            async getDetail(url) {
                this.detailCallCount++;
                return {
                    name: "Detailed Asura Manga",
                    imageUrl: "https://cdn.example.com/cover1.jpg",
                    description: "Epic action fantasy manhwa",
                    author: "Author A",
                    artist: "Artist B",
                    status: 0,
                    chapters: [
                        { url: "/chapter/1", name: "Chapter 1", chapterNumber: 1, dateUpload: 1700000000000, scanlator: "Asura" },
                        { url: "/chapter/2", name: "Chapter 2", chapterNumber: 2, dateUpload: 1700000010000, scanlator: "Asura" }
                    ],
                    detailCallCount: this.detailCallCount
                };
            }
            async getPageList(url) {
                return [
                    "https://cdn.example.com/p1.jpg",
                    { url: "https://cdn.example.com/p2.jpg" }
                ];
            }
            async getHtmlContent(name, url) {
                return "<p>This is the web novel chapter text for " + name + "</p>";
            }
        }
    """.trimIndent()

    @Test
    fun testMangayomiAdapterLifecycleAndDetailCache() = runBlocking {
        val pool = JsEnginePool(maxActiveEngines = 3)
        val bridge = JsBridge()

        val manifest = MangayomiManifest(
            id = 524070078L,
            name = "Asura Scans",
            baseUrl = "https://asuracomic.net",
            lang = "en",
            version = "0.1.7",
            script = mockMangayomiScript
        )

        val adapter = MangayomiSourceAdapter(manifest, pool, bridge)

        // 1. Popular
        val popular = adapter.getPopular(1)
        assertEquals(1, popular.mangas.size)
        assertEquals("Asura Title", popular.mangas[0].title)
        assertEquals("/manga/asura-1", popular.mangas[0].url)
        assertTrue(popular.hasNextPage)

        // 2. Search
        val search = adapter.search("Return of Mount Hua", 1)
        assertEquals(1, search.mangas.size)
        assertEquals("Found Return of Mount Hua", search.mangas[0].title)

        // 3. Details & Chapters with 60s Cache Verification
        val baseManga = SourceManga(url = "/manga/asura-1", title = "Old Title")
        val details = adapter.getMangaDetails(baseManga)
        assertEquals("Detailed Asura Manga", details.title)
        assertEquals("Epic action fantasy manhwa", details.description)
        assertEquals(MangaStatus.ONGOING, details.status)

        // Second call for chapter list must use the cached detail JSON without re-invoking remote fetch
        val chapters = adapter.getChapterList(baseManga)
        assertEquals(2, chapters.size)
        assertEquals("Chapter 1", chapters[0].name)
        assertEquals(1f, chapters[0].chapterNumber)

        // Verify detailCallCount inside JS engine is exactly 1
        val callCount = pool.withEngine(adapter.id) {
            it.evaluate("extention.detailCallCount")
        }
        assertEquals("1", callCount, "getDetail should be invoked exactly once due to 60s cache")

        // 4. Page list
        val pages = adapter.getPageList(SourceChapter(url = "/chapter/1", name = "Chapter 1"))
        assertEquals(2, pages.size)
        assertEquals("https://cdn.example.com/p1.jpg", pages[0].imageUrl)
        assertEquals("https://cdn.example.com/p2.jpg", pages[1].imageUrl)

        // 5. Novel HTML content
        val content = adapter.getChapterContent(SourceChapter(url = "/chapter/1", name = "Chapter 1"))
        assertEquals("<p>This is the web novel chapter text for Chapter 1</p>", content.htmlBody)
        assertEquals("This is the web novel chapter text for Chapter 1", content.plainText)

        pool.close()
    }
}
