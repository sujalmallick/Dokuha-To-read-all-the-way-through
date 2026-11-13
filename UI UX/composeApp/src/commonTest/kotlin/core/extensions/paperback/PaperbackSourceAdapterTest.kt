package core.extensions.paperback

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaperbackSourceAdapterTest {

    // Emulates a compiled Paperback v0.9 IIFE module registering to globalThis.source
    private val mockPaperbackV9Script = """
        (function() {
            globalThis.source = globalThis.source || {};
            
            class MangaSee {
                async getDiscoverSectionItems(section, metadata) {
                    return {
                        items: [
                            { id: "manga-one", title: "MangaSee Popular One", image: "https://cdn.example.com/ms1.jpg" }
                        ]
                    };
                }
                async getSearchResults(query, metadata) {
                    return {
                        items: [
                            { id: "manga-found", title: "MangaSee Result " + query.title, image: "https://cdn.example.com/msf.jpg" }
                        ]
                    };
                }
                async getMangaDetails(mangaId) {
                    return {
                        mangaInfo: {
                            titles: ["MangaSee Detailed Title"],
                            image: "https://cdn.example.com/ms1.jpg",
                            desc: "Thrilling adventure manga",
                            author: "Author M",
                            status: "Ongoing"
                        }
                    };
                }
                async getChapters(mangaId) {
                    return [
                        { id: "ch-100", name: "Chapter 100", chapNum: 100, group: "ScanGroup" }
                    ];
                }
                async getChapterDetails(mangaId, chapterId) {
                    return {
                        pages: [
                            "https://cdn.example.com/page1.jpg",
                            "https://cdn.example.com/page2.jpg"
                        ]
                    };
                }
            }

            globalThis.source["mangasee"] = MangaSee;
        })();
    """.trimIndent()

    @Test
    fun testPaperbackV9LifecycleAndKeyedInstantiation() = runBlocking {
        val pool = JsEnginePool(maxActiveEngines = 3)
        val bridge = JsBridge()

        val manifest = PaperbackManifest(
            id = "mangasee",
            name = "MangaSee",
            websiteBaseURL = "https://mangasee123.com",
            language = "en",
            version = "0.9.1",
            contentRating = "EVERYONE",
            script = mockPaperbackV9Script
        )

        val adapter = PaperbackSourceAdapter(manifest, pool, bridge, isV9 = true)

        // 1. Popular
        val popular = adapter.getPopular(1)
        assertEquals(1, popular.mangas.size)
        assertEquals("MangaSee Popular One", popular.mangas[0].title)

        // 2. Search
        val search = adapter.search("Berserk", 1)
        assertEquals("MangaSee Result Berserk", search.mangas[0].title)

        // 3. Details
        val manga = SourceManga(url = "manga-one", title = "Old Title")
        val details = adapter.getMangaDetails(manga)
        assertEquals("MangaSee Detailed Title", details.title)
        assertEquals("Thrilling adventure manga", details.description)
        assertEquals("Author M", details.author)
        assertEquals(MangaStatus.ONGOING, details.status)

        // 4. Chapters
        val chapters = adapter.getChapterList(manga)
        assertEquals(1, chapters.size)
        assertEquals("Chapter 100", chapters[0].name)
        assertEquals(100f, chapters[0].chapterNumber)

        // 5. Pages
        val pages = adapter.getPageList(SourceChapter(url = "ch-100", name = "Chapter 100"))
        assertEquals(2, pages.size)
        assertEquals("https://cdn.example.com/page1.jpg", pages[0].imageUrl)
        assertEquals("https://cdn.example.com/page2.jpg", pages[1].imageUrl)

        pool.close()
    }
}
