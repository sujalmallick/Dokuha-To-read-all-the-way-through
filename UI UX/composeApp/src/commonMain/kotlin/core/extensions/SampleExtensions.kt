package core.extensions

import core.extensions.js.ExtensionCatalogItem
import core.extensions.js.ExtensionType

/**
 * Built-in reference extensions used for demonstration, quick testing, and verification
 * of the JavaScript Extension Runner across Android, iOS, and Desktop.
 */
object SampleExtensions {
    val SAMPLE_MANGA_BUNDLE = """
        const manifest = {
            id: "sample-manga",
            name: "Sample Manga (Demo)",
            version: "1.0.0",
            lang: "en",
            baseUrl: "https://samplemanga.example.com",
            type: "COMIC",
            isNsfw: false
        };

        const extension = {
            async getPopular(page) {
                return {
                    mangas: [
                        {
                            url: "https://samplemanga.example.com/manga/shadow-monarch",
                            title: "Shadow Monarch",
                            thumbnailUrl: "https://samplemanga.example.com/covers/shadow.jpg",
                            status: "ONGOING",
                            genres: ["Action", "Fantasy"],
                            isNovel: false
                        },
                        {
                            url: "https://samplemanga.example.com/manga/celestial-blade",
                            title: "Celestial Blade",
                            thumbnailUrl: "https://samplemanga.example.com/covers/celestial.jpg",
                            status: "COMPLETED",
                            genres: ["Martial Arts", "Adventure"],
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
                    title: url.includes("shadow") ? "Shadow Monarch" : "Celestial Blade",
                    author: "Chugong",
                    artist: "DUBU",
                    description: "Awakened hunters battle deadly dungeon monsters using mysterious shadow arts.",
                    status: "COMPLETED",
                    genres: ["Action", "Fantasy"]
                };
            },

            async getChapters(url) {
                return [
                    {
                        url: url + "/ch-1",
                        name: "Chapter 1: The Awakening",
                        chapterNumber: 1.0,
                        dateUpload: 1700000000000,
                        scanlator: "Dokuha Scans"
                    },
                    {
                        url: url + "/ch-2",
                        name: "Chapter 2: The Double Dungeon",
                        chapterNumber: 2.0,
                        dateUpload: 1700600000000,
                        scanlator: "Dokuha Scans"
                    }
                ];
            },

            async getPages(chapterUrl) {
                return [
                    { index: 1, url: chapterUrl + "/1", imageUrl: "https://samplemanga.example.com/p1.jpg" },
                    { index: 2, url: chapterUrl + "/2", imageUrl: "https://samplemanga.example.com/p2.jpg" }
                ];
            }
        };
    """.trimIndent()

    val SAMPLE_NOVEL_BUNDLE = """
        const manifest = {
            id: "sample-novel",
            name: "Sample Novel (Demo)",
            version: "1.0.0",
            lang: "en",
            baseUrl: "https://samplenovel.example.com",
            type: "NOVEL",
            isNsfw: false
        };

        const extension = {
            async getPopular(page) {
                return {
                    mangas: [
                        {
                            url: "https://samplenovel.example.com/novel/infinite-mage",
                            title: "The Infinite Mage",
                            thumbnailUrl: "https://samplenovel.example.com/covers/mage.jpg",
                            status: "ONGOING",
                            genres: ["Magic", "Progression"],
                            isNovel: true
                        }
                    ],
                    hasNextPage: false
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
                    title: "The Infinite Mage",
                    author: "Kim Chi-woo",
                    description: "A commoner boy discovers the infinite realm of mathematical magic.",
                    isNovel: true,
                    status: "ONGOING",
                    genres: ["Magic", "Progression"]
                };
            },

            async getChapters(url) {
                return [
                    { url: url + "/c1", name: "Chapter 1: The Geometry of Space", chapterNumber: 1.0 },
                    { url: url + "/c2", name: "Chapter 2: The Red Flame", chapterNumber: 2.0 }
                ];
            },

            async getNovelContent(chapterUrl) {
                return {
                    chapterUrl: chapterUrl,
                    title: chapterUrl.endsWith("c1") ? "Chapter 1: The Geometry of Space" : "Chapter 2: The Red Flame",
                    htmlBody: "<p>The morning light filtered through the library windows, casting long geometric shadows on the ancient parchment.</p><p>Shirone opened his eyes. For the first time in his life, the invisible numbers forming the world became visible.</p>",
                    plainText: "The morning light filtered through the library windows, casting long geometric shadows on the ancient parchment.\n\nShirone opened his eyes. For the first time in his life, the invisible numbers forming the world became visible."
                };
            }
        };
    """.trimIndent()

    val DEMO_CATALOG_ITEMS = listOf(
        ExtensionCatalogItem(
            id = "sample-manga",
            name = "Sample Manga (Demo)",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://samplemanga.example.com",
            type = ExtensionType.COMIC,
            iconUrl = "",
            isNsfw = false,
            pkg = "demo://sample-manga",
            sha256 = ""
        ),
        ExtensionCatalogItem(
            id = "sample-novel",
            name = "Sample Novel (Demo)",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://samplenovel.example.com",
            type = ExtensionType.NOVEL,
            iconUrl = "",
            isNsfw = false,
            pkg = "demo://sample-novel",
            sha256 = ""
        )
    )

    fun getDemoBundle(id: String): String? = when (id) {
        "sample-manga" -> SAMPLE_MANGA_BUNDLE
        "sample-novel" -> SAMPLE_NOVEL_BUNDLE
        else -> null
    }
}
