package core.extensions.js

import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsExtensionSourceTest {

    private val sampleMangaScript = """
        const manifest = {
            id: "test-manga",
            name: "Test Manga Source",
            version: "1.0.0",
            lang: "en",
            baseUrl: "https://manga.example.com",
            type: "COMIC",
            isNsfw: false
        };

        const extension = {
            async getPopular(page) {
                return {
                    mangas: [
                        {
                            url: "https://manga.example.com/manga/shadow-monarch",
                            title: "Shadow Monarch",
                            thumbnailUrl: "https://manga.example.com/covers/shadow.jpg",
                            status: "ONGOING",
                            genres: ["Action", "Fantasy"],
                            isNovel: false
                        },
                        {
                            url: "https://manga.example.com/manga/celestial-blade",
                            title: "Celestial Blade",
                            thumbnailUrl: "https://manga.example.com/covers/celestial.jpg",
                            status: "COMPLETED",
                            genres: ["Martial Arts"],
                            isNovel: false
                        }
                    ],
                    hasNextPage: true
                };
            },

            async search(query, page) {
                const pop = await this.getPopular(page);
                const q = (query || "").toLowerCase();
                const matched = pop.mangas.filter(m => m.title.toLowerCase().includes(q));
                return { mangas: matched, hasNextPage: false };
            },

            async getDetails(url) {
                return {
                    url: url,
                    title: "Shadow Monarch Detailed",
                    author: "Chugong",
                    artist: "DUBU",
                    description: "Awakened hunters battle monsters.",
                    status: "COMPLETED",
                    genres: ["Action", "Fantasy"]
                };
            },

            async getChapters(url) {
                return [
                    {
                        url: url + "/ch-1",
                        name: "Chapter 1",
                        chapterNumber: 1.0,
                        dateUpload: 1700000000000,
                        scanlator: "ScanGroup"
                    }
                ];
            },

            async getPages(chapterUrl) {
                return [
                    { index: 1, url: chapterUrl + "/1", imageUrl: "https://manga.example.com/p1.jpg" },
                    { index: 2, url: chapterUrl + "/2", imageUrl: "https://manga.example.com/p2.jpg" }
                ];
            }
        };
    """.trimIndent()

    private val sampleNovelScript = """
        const manifest = {
            id: "test-novel",
            name: "Test Novel Source",
            version: "1.0.0",
            lang: "en",
            baseUrl: "https://novel.example.com",
            type: "NOVEL",
            isNsfw: false
        };

        const extension = {
            async getPopular(page) {
                return {
                    mangas: [
                        {
                            url: "https://novel.example.com/novel/rebirth",
                            title: "Supreme Rebirth",
                            thumbnailUrl: "https://novel.example.com/covers/rebirth.jpg",
                            status: "ONGOING",
                            genres: ["Fantasy"],
                            isNovel: true
                        }
                    ],
                    hasNextPage: false
                };
            },

            async getDetails(url) {
                return {
                    url: url,
                    title: "Supreme Rebirth",
                    author: "Legion",
                    description: "Reborn into a realm of magic.",
                    isNovel: true
                };
            },

            async getChapters(url) {
                return [
                    { url: url + "/c1", name: "Chapter 1", chapterNumber: 1.0 }
                ];
            },

            async getNovelContent(chapterUrl) {
                return {
                    chapterUrl: chapterUrl,
                    title: "Chapter 1: Dawn",
                    htmlBody: "<p>The morning mist rolled over the mountains.</p>",
                    plainText: "The morning mist rolled over the mountains."
                };
            }
        };
    """.trimIndent()

    @Test
    fun testComicExtensionLifecycle() = runBlocking {
        val manifest = JsExtensionManifest(
            id = "test-manga",
            name = "Test Manga Source",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://manga.example.com",
            type = ExtensionType.COMIC,
            script = sampleMangaScript
        )

        val engine = JsEngine()
        val bridge = JsBridge()
        val source = JsExtensionSource(manifest = manifest, engine = engine, bridge = bridge)
        source.initialize()

        // 1. getPopular
        val popular = source.getPopular(1)
        assertEquals(2, popular.mangas.size)
        assertTrue(popular.hasNextPage)
        assertEquals("Shadow Monarch", popular.mangas[0].title)
        assertFalse(popular.mangas[0].isNovel)

        // 2. search
        val searchRes = source.search("Celestial", 1)
        assertEquals(1, searchRes.mangas.size)
        assertEquals("Celestial Blade", searchRes.mangas[0].title)

        // 3. getMangaDetails
        val details = source.getMangaDetails(popular.mangas[0])
        assertEquals("Shadow Monarch Detailed", details.title)
        assertEquals("Chugong", details.author)
        assertEquals("DUBU", details.artist)
        assertEquals(MangaStatus.COMPLETED, details.status)

        // 4. getChapters
        val chapters = source.getChapterList(popular.mangas[0])
        assertEquals(1, chapters.size)
        assertEquals("Chapter 1", chapters[0].name)
        assertEquals(1.0f, chapters[0].chapterNumber)

        // 5. getPages
        val pages = source.getPageList(chapters[0])
        assertEquals(2, pages.size)
        assertEquals("https://manga.example.com/p1.jpg", pages[0].imageUrl)

        source.close()
    }

    @Test
    fun testNovelExtensionLifecycle() = runBlocking {
        val manifest = JsExtensionManifest(
            id = "test-novel",
            name = "Test Novel Source",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://novel.example.com",
            type = ExtensionType.NOVEL,
            script = sampleNovelScript
        )

        val engine = JsEngine()
        val bridge = JsBridge()
        val source = JsExtensionSource(manifest = manifest, engine = engine, bridge = bridge)
        source.initialize()

        val popular = source.getPopular(1)
        assertEquals(1, popular.mangas.size)
        assertTrue(popular.mangas[0].isNovel)

        val chapters = source.getChapterList(popular.mangas[0])
        assertEquals(1, chapters.size)

        val content = source.getChapterContent(chapters[0])
        assertEquals("Chapter 1: Dawn", content.title)
        assertTrue(content.htmlBody.contains("<p>The morning mist"))
        assertTrue(content.plainText.contains("The morning mist"))

        source.close()
    }
}
