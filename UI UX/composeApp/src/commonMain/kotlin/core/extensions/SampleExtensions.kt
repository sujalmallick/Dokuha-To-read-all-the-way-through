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
                const ratings = "&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic";
                const apiUrlEn = this.apiUrl + "/manga/" + mangaId + "/feed?limit=500&translatedLanguage[]=en&order[chapter]=desc" + ratings;
                const responseEn = await dokuha.fetch(apiUrlEn, { headers: this._headers() });
                const jsonEn = JSON.parse(responseEn.body);
                let data = jsonEn.data || [];

                const hasHostedEn = data.some(ch => !ch.attributes || !ch.attributes.externalUrl);
                if (!hasHostedEn) {
                    const apiUrlAll = this.apiUrl + "/manga/" + mangaId + "/feed?limit=500&order[chapter]=desc" + ratings;
                    const responseAll = await dokuha.fetch(apiUrlAll, { headers: this._headers() });
                    const jsonAll = JSON.parse(responseAll.body);
                    const allData = jsonAll.data || [];
                    const existingIds = {};
                    for (let i = 0; i < data.length; i++) {
                        existingIds[data[i].id] = true;
                    }
                    for (let i = 0; i < allData.length; i++) {
                        if (!existingIds[allData[i].id]) {
                            data.push(allData[i]);
                            existingIds[allData[i].id] = true;
                        }
                    }
                }

                return data.map((item, index) => {
                    const attr = item.attributes || {};
                    const chNum = parseFloat(attr.chapter) || 0.0;
                    const extUrl = attr.externalUrl;
                    const lang = attr.translatedLanguage;
                    const title = attr.title ? (" - " + attr.title) : "";

                    let prefix = "";
                    if (lang && lang !== "en") {
                        prefix = "[" + lang.toUpperCase() + "] ";
                    }
                    if (extUrl) {
                        try {
                            const host = extUrl.split("/")[2].replace("www.", "");
                            prefix = "[" + host + "] " + prefix;
                        } catch (_) {
                            prefix = "[External] " + prefix;
                        }
                    }

                    const name = attr.chapter ? (prefix + "Chapter " + attr.chapter + title) : (prefix + (attr.title || "Oneshot"));
                    const dateUpload = attr.publishAt ? new Date(attr.publishAt).getTime() : Date.now();

                    return {
                        url: extUrl || (this.baseUrl + "/chapter/" + item.id),
                        name: name,
                        chapterNumber: chNum,
                        dateUpload: dateUpload,
                        scanlator: null
                    };
                });
            },

            async getPages(chapterUrl) {
                if (chapterUrl.startsWith("http://") || chapterUrl.startsWith("https://")) {
                    if (!chapterUrl.includes("mangadex.org/chapter/")) {
                        return [];
                    }
                }
                const chapterId = chapterUrl.split("/").pop();
                const atHomeUrl = this.apiUrl + "/at-home/server/" + chapterId;
                const response = await dokuha.fetch(atHomeUrl, { headers: this._headers() });
                const json = JSON.parse(response.body);
                if (json.result !== "ok" || !json.baseUrl || !json.chapter) {
                    return [];
                }
                const baseUrl = json.baseUrl;
                const hash = json.chapter.hash;
                const data = json.chapter.data || [];

                return data.map((filename, index) => ({
                    index: index + 1,
                    url: baseUrl + "/data/" + hash + "/" + filename,
                    imageUrl: baseUrl + "/data/" + hash + "/" + filename,
                    headers: {
                        "User-Agent": "Dokuha/1.0.0 (https://github.com/RitwikGupta-0501/dokuha)",
                        "Referer": "https://mangadex.org"
                    }
                }));
            }
        };
    """.trimIndent()

    val ASURA_SCANS_BUNDLE = """
        function unwrapAstro(val) {
            if (Array.isArray(val)) {
                if (val.length === 0) return null;
                if (val.length === 2 && typeof val[0] === 'number') {
                    return unwrapAstro(val[1]);
                }
                return val.map(unwrapAstro);
            }
            if (val !== null && typeof val === 'object') {
                const obj = {};
                for (const k in val) {
                    if (Object.prototype.hasOwnProperty.call(val, k)) {
                        obj[k] = unwrapAstro(val[k]);
                    }
                }
                return obj;
            }
            return val;
        }

        function extractAstroData(html, key) {
            if (!html) return null;
            try {
                const doc = new Document(html);
                const el = doc.selectFirst(`[props*="${'$'}{key}"]`);
                if (el) {
                    const raw = el.attr("props");
                    if (raw) return unwrapAstro(JSON.parse(raw));
                }
            } catch (_) {}
            try {
                const regex = new RegExp(`<astro-island[^>]*props="([^"]*${'$'}{key}[^"]*)"`, 'i');
                const match = html.match(regex);
                if (match && match[1]) {
                    const decoded = match[1].replace(/&quot;/g, '"').replace(/&amp;/g, '&');
                    return unwrapAstro(JSON.parse(decoded));
                }
            } catch (_) {}
            return null;
        }

        function mapStatus(statusStr) {
            if (!statusStr) return "UNKNOWN";
            const s = String(statusStr).toLowerCase();
            if (s.includes("ongoing")) return "ONGOING";
            if (s.includes("complete")) return "COMPLETED";
            if (s.includes("hiatus")) return "ON_HIATUS";
            if (s.includes("dropped") || s.includes("cancel") || s.includes("axed")) return "CANCELLED";
            return "UNKNOWN";
        }

        const API_BASE = "https://api.asurascans.com/api";
        const WEB_BASE = "https://asurascans.com";
        const DEFAULT_HEADERS = {
            "Referer": "https://asurascans.com/",
            "Origin": "https://asurascans.com",
            "Accept": "application/json, text/plain, */*"
        };

        const manifest = {
            id: "asurascans",
            name: "Asura Scans",
            version: "2.0.0",
            lang: "en",
            baseUrl: WEB_BASE,
            apiUrl: API_BASE,
            type: "COMIC",
            isNsfw: false
        };

        const extension = {
            async getPopular(page) {
                const offset = Math.max(0, (page - 1) * 20);
                const url = `${'$'}{API_BASE}/series?offset=${'$'}{offset}&limit=20&order=popular`;
                const res = await new Client().get(url, DEFAULT_HEADERS);
                return this.parseCatalogResponse(res.body);
            },

            async getLatestUpdates(page) {
                const offset = Math.max(0, (page - 1) * 20);
                const url = `${'$'}{API_BASE}/series?offset=${'$'}{offset}&limit=20&order=latest`;
                const res = await new Client().get(url, DEFAULT_HEADERS);
                return this.parseCatalogResponse(res.body);
            },

            async search(query, page, filters) {
                const offset = Math.max(0, (page - 1) * 20);
                const searchParam = query ? `&search=${'$'}{encodeURIComponent(query)}` : "";
                const url = `${'$'}{API_BASE}/series?offset=${'$'}{offset}&limit=20${'$'}{searchParam}`;
                const res = await new Client().get(url, DEFAULT_HEADERS);
                return this.parseCatalogResponse(res.body);
            },

            parseCatalogResponse(body) {
                try {
                    const parsed = typeof body === "string" ? JSON.parse(body) : body;
                    const data = (parsed && parsed.data) || [];
                    const meta = (parsed && parsed.meta) || {};
                    const mangas = data.map(item => ({
                        url: item.public_url || `/comics/${'$'}{item.slug}`,
                        title: item.title || "Untitled",
                        thumbnailUrl: item.coverUrl || item.cover || "",
                        status: "UNKNOWN",
                        genres: [],
                        isNovel: false
                    }));
                    const hasNext = Boolean(meta.has_more ?? (data.length >= 20));
                    return { mangas, hasNextPage: hasNext };
                } catch (_) {
                    return { mangas: [], hasNextPage: false };
                }
            },

            async getDetails(url) {
                const fullUrl = url.startsWith("http") ? url : `${'$'}{WEB_BASE}${'$'}{url.startsWith("/") ? "" : "/"}${'$'}{url}`;
                const res = await new Client().get(fullUrl, DEFAULT_HEADERS);
                const html = res.body || "";
                const data = extractAstroData(html, "title");

                if (data) {
                    const genres = (data.genres || []).map(g => (typeof g === "object" ? g.name : String(g))).filter(Boolean);
                    const cleanDesc = data.description
                        ? data.description.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/<[^>]*>/g, "").trim()
                        : "";
                    return {
                        url: url,
                        title: data.title || "Untitled",
                        thumbnailUrl: data.coverUrl || data.cover || "",
                        author: data.author || "",
                        artist: data.artist || "",
                        description: cleanDesc,
                        genres: genres,
                        status: mapStatus(data.status),
                        isNovel: false
                    };
                }

                const doc = new Document(html);
                const ogTitle = doc.selectFirst("meta[property='og:title']")?.attr("content") || "Unknown";
                const ogImage = doc.selectFirst("meta[property='og:image']")?.attr("content") || "";
                const ogDesc = doc.selectFirst("meta[property='og:description']")?.attr("content") || "";

                return {
                    url: url,
                    title: ogTitle,
                    thumbnailUrl: ogImage,
                    description: ogDesc,
                    author: "",
                    artist: "",
                    genres: [],
                    status: "UNKNOWN",
                    isNovel: false
                };
            },

            async getChapters(url) {
                const fullUrl = url.startsWith("http") ? url : `${'$'}{WEB_BASE}${'$'}{url.startsWith("/") ? "" : "/"}${'$'}{url}`;
                const res = await new Client().get(fullUrl, DEFAULT_HEADERS);
                const html = res.body || "";
                const data = extractAstroData(html, "chapters");
                const rawChapters = (data && data.chapters) ? data.chapters : [];
                const slug = url.split("/").filter(Boolean).pop() || "";
                const now = Date.now();

                return rawChapters.map(ch => {
                    const isLocked = ch.is_premium || (ch.early_access_until && new Date(ch.early_access_until).getTime() > now);
                    const num = parseFloat(ch.number) || 0;
                    const chapterSlug = ch.series_slug || slug;
                    const chapterUrl = `/comics/${'$'}{chapterSlug}/chapter/${'$'}{num}`;
                    const chapterName = ch.title ? `Chapter ${'$'}{num}: ${'$'}{ch.title}` : `Chapter ${'$'}{num}`;
                    const dateUpload = ch.created_at ? new Date(ch.created_at).getTime() : 0;

                    return {
                        url: chapterUrl,
                        name: chapterName,
                        chapterNumber: num,
                        dateUpload: dateUpload,
                        scanlator: isLocked ? "[Early Access]" : "Asura Scans"
                    };
                });
            },

            async getPages(chapterUrl) {
                const fullUrl = chapterUrl.startsWith("http") ? chapterUrl : `${'$'}{WEB_BASE}${'$'}{chapterUrl.startsWith("/") ? "" : "/"}${'$'}{chapterUrl}`;
                const res = await new Client().get(fullUrl, DEFAULT_HEADERS);
                const html = res.body || "";
                const data = extractAstroData(html, "pages");
                const pages = (data && data.pages) ? data.pages : [];

                if (pages.length > 0) {
                    return pages.map((p, idx) => {
                        const img = typeof p === "string" ? p : (p.url || p.imageUrl || "");
                        return { index: idx, url: img, imageUrl: img };
                    });
                }

                const match = html.match(/"pages":\s*(\[[^\]]+\])/);
                if (match && match[1]) {
                    try {
                        const fallbackList = JSON.parse(match[1]);
                        return fallbackList.map((p, idx) => {
                            const img = typeof p === "string" ? p : (p.url || "");
                            return { index: idx, url: img, imageUrl: img };
                        });
                    } catch (_) {}
                }

                return [];
            }
        };
    """.trimIndent()

    val DEMO_CATALOG_ITEMS = emptyList<ExtensionCatalogItem>()

    fun getDemoBundle(id: String): String? = when (id) {
        "asurascans" -> ASURA_SCANS_BUNDLE
        "mangadex" -> MANGADEX_BUNDLE
        "sample-manga" -> SAMPLE_MANGA_BUNDLE
        "sample-novel" -> SAMPLE_NOVEL_BUNDLE
        else -> null
    }
}
