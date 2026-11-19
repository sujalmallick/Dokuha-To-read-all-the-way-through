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

    val MANGADEX_BUNDLE = """
        const manifest = {
            id: "mangadex",
            name: "MangaDex",
            version: "1.0.0",
            lang: "en",
            baseUrl: "https://mangadex.org",
            type: "COMIC",
            iconUrl: "https://mangadex.org/favicon.ico",
            isNsfw: false
        };

        const extension = {
            baseUrl: "https://mangadex.org",
            apiUrl: "https://api.mangadex.org",
            coverBaseUrl: "https://uploads.mangadex.org/covers",

            _headers() {
                return {
                    "User-Agent": "Dokuha/1.0.0",
                    "Accept": "application/json"
                };
            },

            _extractCover(relationships, mangaId) {
                if (!relationships || !Array.isArray(relationships)) return null;
                const coverRel = relationships.find(r => r.type === "cover_art");
                if (coverRel && coverRel.attributes && coverRel.attributes.fileName) {
                    return this.coverBaseUrl + "/" + mangaId + "/" + coverRel.attributes.fileName + ".256.jpg";
                }
                return null;
            },

            _extractTitle(attributes) {
                if (!attributes || !attributes.title) return "Untitled";
                const titleObj = attributes.title;
                return titleObj.en || titleObj["ja-ro"] || titleObj["ko-ro"] || titleObj["zh-ro"] || Object.values(titleObj)[0] || "Untitled";
            },

            _extractDescription(attributes) {
                if (!attributes || !attributes.description) return "";
                const descObj = attributes.description;
                return descObj.en || Object.values(descObj)[0] || "";
            },

            _extractAuthor(relationships, type) {
                if (!relationships || !Array.isArray(relationships)) return null;
                const rel = relationships.find(r => r.type === type);
                return (rel && rel.attributes && rel.attributes.name) ? rel.attributes.name : null;
            },

            _extractGenres(attributes) {
                if (!attributes || !attributes.tags) return [];
                return attributes.tags
                    .filter(t => t.attributes && t.attributes.name)
                    .map(t => t.attributes.name.en || Object.values(t.attributes.name)[0]);
            },

            _mapManga(item) {
                const id = item.id;
                const attr = item.attributes || {};
                const coverUrl = this._extractCover(item.relationships, id);
                const title = this._extractTitle(attr);
                const author = this._extractAuthor(item.relationships, "author");
                const artist = this._extractAuthor(item.relationships, "artist");
                const description = this._extractDescription(attr);
                const genres = this._extractGenres(attr);

                let status = "ONGOING";
                if (attr.status === "completed") status = "COMPLETED";
                else if (attr.status === "hiatus") status = "HIATUS";
                else if (attr.status === "cancelled") status = "CANCELLED";

                return {
                    url: this.baseUrl + "/title/" + id,
                    title: title,
                    author: author,
                    artist: artist,
                    description: description,
                    genres: genres,
                    status: status,
                    thumbnailUrl: coverUrl,
                    isNovel: false
                };
            },

            async getPopular(page) {
                const offset = ((page || 1) - 1) * 20;
                const url = this.apiUrl + "/manga?limit=20&offset=" + offset + "&includes[]=cover_art&includes[]=author&includes[]=artist&order[followedCount]=desc&contentRating[]=safe&contentRating[]=suggestive";
                const response = await dokuha.fetch(url, { headers: this._headers() });
                const json = JSON.parse(response.body);
                const data = json.data || [];
                const total = json.total || 0;
                const mangas = data.map(item => this._mapManga(item));
                return {
                    mangas: mangas,
                    hasNextPage: offset + 20 < total
                };
            },

            async getLatestUpdates(page) {
                const offset = ((page || 1) - 1) * 20;
                const url = this.apiUrl + "/manga?limit=20&offset=" + offset + "&includes[]=cover_art&includes[]=author&includes[]=artist&order[latestUploadedChapter]=desc&contentRating[]=safe&contentRating[]=suggestive";
                const response = await dokuha.fetch(url, { headers: this._headers() });
                const json = JSON.parse(response.body);
                const data = json.data || [];
                const total = json.total || 0;
                const mangas = data.map(item => this._mapManga(item));
                return {
                    mangas: mangas,
                    hasNextPage: offset + 20 < total
                };
            },

            async search(query, page, filters) {
                const offset = ((page || 1) - 1) * 20;
                let url = this.apiUrl + "/manga?limit=20&offset=" + offset + "&includes[]=cover_art&includes[]=author&includes[]=artist&contentRating[]=safe&contentRating[]=suggestive";
                if (query && query.trim()) {
                    url += "&title=" + encodeURIComponent(query.trim());
                }
                const response = await dokuha.fetch(url, { headers: this._headers() });
                const json = JSON.parse(response.body);
                const data = json.data || [];
                const total = json.total || 0;
                const mangas = data.map(item => this._mapManga(item));
                return {
                    mangas: mangas,
                    hasNextPage: offset + 20 < total
                };
            },

            async getDetails(url) {
                const mangaId = url.split("/").pop();
                const apiUrl = this.apiUrl + "/manga/" + mangaId + "?includes[]=cover_art&includes[]=author&includes[]=artist";
                const response = await dokuha.fetch(apiUrl, { headers: this._headers() });
                const json = JSON.parse(response.body);
                return this._mapManga(json.data);
            },

            async getChapters(url) {
                const mangaId = url.split("/").pop();
                const apiUrl = this.apiUrl + "/manga/" + mangaId + "/feed?limit=100&translatedLanguage[]=en&order[chapter]=desc&includeEmptyPages=0";
                const response = await dokuha.fetch(apiUrl, { headers: this._headers() });
                const json = JSON.parse(response.body);
                const data = json.data || [];

                return data.map((item, index) => {
                    const attr = item.attributes || {};
                    const chNum = parseFloat(attr.chapter) || 0.0;
                    const title = attr.title ? (" - " + attr.title) : "";
                    const name = attr.chapter ? ("Chapter " + attr.chapter + title) : ("Oneshot" + title);
                    const dateUpload = attr.publishAt ? new Date(attr.publishAt).getTime() : Date.now();

                    return {
                        url: this.baseUrl + "/chapter/" + item.id,
                        name: name,
                        chapterNumber: chNum,
                        dateUpload: dateUpload,
                        scanlator: null
                    };
                });
            },

            async getPages(chapterUrl) {
                const chapterId = chapterUrl.split("/").pop();
                const atHomeUrl = this.apiUrl + "/at-home/server/" + chapterId;
                const response = await dokuha.fetch(atHomeUrl, { headers: this._headers() });
                const json = JSON.parse(response.body);
                const baseUrl = json.baseUrl;
                const hash = json.chapter.hash;
                const data = json.chapter.data || [];

                return data.map((filename, index) => ({
                    index: index + 1,
                    url: chapterUrl + "/" + (index + 1),
                    imageUrl: baseUrl + "/data/" + hash + "/" + filename
                }));
            }
        };
    """.trimIndent()

    val DEMO_CATALOG_ITEMS = listOf(
        ExtensionCatalogItem(
            id = "mangadex",
            name = "MangaDex",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://mangadex.org",
            type = ExtensionType.COMIC,
            iconUrl = "https://mangadex.org/favicon.ico",
            isNsfw = false,
            pkg = "demo://mangadex",
            sha256 = ""
        ),
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
        "mangadex" -> MANGADEX_BUNDLE
        "sample-manga" -> SAMPLE_MANGA_BUNDLE
        "sample-novel" -> SAMPLE_NOVEL_BUNDLE
        else -> null
    }
}
