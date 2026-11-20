package core.extensions.js

import core.source.MangaStatus
import core.source.SourceChapter
import core.source.SourceManga
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AsuraScansExtensionTest {

    companion object {
        private val TEST_ASURA_SCANS_BUNDLE = """
            const manifest = {
                id: "asurascans",
                name: "Asura Scans",
                version: "1.2.0",
                lang: "en",
                baseUrl: "https://asurascans.com",
                type: "COMIC",
                isNsfw: false
            };

            const URL_RANDOM_PART = /-[a-z0-9]{8}${'$'}/i;
            const OLD_FORMAT_MANGA_REGEX = /^\/manga\/(?:\d+-)?([^/]+)\/?${'$'}/;
            const PAGE_TOKEN_REGEX = /pageToken\*=\*"([^"]+)"/;
            const PER_PAGE_LIMIT = 20;

            const slugMap = new Map();

            function cleanSlug(url) {
                if (!url) return "";
                let clean = url.trim();
                if (clean.includes("/series/")) {
                    clean = clean.split("/series/")[1];
                } else if (clean.includes("/comics/")) {
                    clean = clean.split("/comics/")[1];
                } else if (clean.includes("/manga/")) {
                    const match = clean.match(OLD_FORMAT_MANGA_REGEX);
                    clean = match ? match[1] : clean.split("/manga/")[1];
                }
                return clean.split("/")[0].split("?")[0].replace(/\/+${'$'}/, "");
            }

            function getCachedSlug(slug) {
                if (!slug) return slug;
                return slugMap.get(slug) || slugMap.get(slug.replace(URL_RANDOM_PART, "")) || slug;
            }

            function setCachedSlug(requestedSlug, canonicalSlug) {
                if (!requestedSlug || !canonicalSlug) return;
                const cleanReq = cleanSlug(requestedSlug);
                const cleanCanon = cleanSlug(canonicalSlug);
                if (cleanReq && cleanCanon) {
                    slugMap.set(cleanReq, cleanCanon);
                    const baseSlug = cleanReq.replace(URL_RANDOM_PART, "");
                    if (baseSlug) slugMap.set(baseSlug, cleanCanon);
                }
            }

            function unwrapAstro(val) {
                if (Array.isArray(val)) {
                    if (val.length === 0) return null;
                    if (val.length === 1 && typeof val[0] === 'number') return null;
                    if (val.length === 2 && typeof val[0] === 'number') {
                        return unwrapAstro(val[1]);
                    }
                    return val.map(unwrapAstro);
                }
                if (val !== null && typeof val === 'object') {
                    const obj = {};
                    for (const k of Object.keys(val)) {
                        obj[k] = unwrapAstro(val[k]);
                    }
                    return obj;
                }
                return val;
            }

            function decodeHtmlEntities(str) {
                if (!str) return "";
                return str
                    .replace(/&quot;/g, '"')
                    .replace(/&amp;/g, '&')
                    .replace(/&#39;/g, "'")
                    .replace(/&lt;/g, '<')
                    .replace(/&gt;/g, '>');
            }

            function extractAstroData(html, key) {
                if (!html) return null;
                try {
                    if (typeof dokuha !== 'undefined' && typeof dokuha.parseHtml === 'function') {
                        const ${'$'} = dokuha.parseHtml(html);
                        const island = ${'$'}(`[props*="${'$'}{key}"]`).first();
                        const raw = island.attr("props");
                        if (raw) {
                            return unwrapAstro(JSON.parse(decodeHtmlEntities(raw)));
                        }
                    } else if (typeof Document !== 'undefined') {
                        const doc = new Document(html);
                        const el = doc.selectFirst(`[props*="${'$'}{key}"]`);
                        if (el) {
                            const raw = el.attr("props");
                            if (raw) return unwrapAstro(JSON.parse(decodeHtmlEntities(raw)));
                        }
                    }
                } catch (_) {}

                try {
                    const regex = new RegExp(`<astro-island[^>]*props="([^"]*${'$'}{key}[^"]*)"`, 'i');
                    const match = html.match(regex);
                    if (match && match[1]) {
                        const decoded = decodeHtmlEntities(match[1]);
                        return unwrapAstro(JSON.parse(decoded));
                    }
                } catch (_) {}

                return null;
            }

            function formatBookmarks(count) {
                if (typeof count !== 'number') return null;
                if (count >= 1000000) return (count / 1000000).toFixed(1) + "M";
                if (count >= 1000) return (count / 1000).toFixed(1) + "K";
                return String(count);
            }

            function parseDescription(data) {
                const parts = [];
                const meta = [];
                if (data.popularityRank) meta.push(`Rank: #${'$'}{data.popularityRank}`);
                if (typeof data.rating === 'number') meta.push(`Rating: ${'$'}{data.rating.toFixed(2)}`);
                if (typeof data.bookmarkCount === 'number') {
                    const formatted = formatBookmarks(data.bookmarkCount);
                    if (formatted) meta.push(`Bookmarks: ${'$'}{formatted}`);
                }
                if (meta.length > 0) parts.push(meta.join(" • "));

                if (data.description) {
                    const clean = decodeHtmlEntities(data.description)
                        .replace(/<[^>]*>/g, "")
                        .trim();
                    if (clean) parts.push(clean);
                }

                if (data.alternativeTitles) {
                    const rawAlt = data.alternativeTitles.includes("•")
                        ? data.alternativeTitles.split("•")
                        : data.alternativeTitles.split(",");
                    const altList = rawAlt.map(s => s.trim()).filter(Boolean);
                    if (altList.length > 0) {
                        parts.push("Alternative Titles:\n" + altList.map(t => `- ${'$'}{t}`).join("\n"));
                    }
                }

                return parts.join("\n\n");
            }

            function buildGenres(data) {
                const list = [];
                if (data.type) {
                    const capType = data.type.charAt(0).toUpperCase() + data.type.slice(1);
                    list.push(capType);
                }
                if (Array.isArray(data.genres)) {
                    data.genres.forEach(g => {
                        const name = typeof g === 'object' && g !== null ? (g.name || g.slug) : String(g);
                        if (name) list.push(name);
                    });
                }
                return list;
            }

            function mapStatus(statusStr) {
                if (!statusStr) return "UNKNOWN";
                const s = String(statusStr).toLowerCase();
                if (s === "ongoing") return "ONGOING";
                if (s === "completed") return "COMPLETED";
                if (s === "hiatus") return "ON_HIATUS";
                if (s === "dropped" || s === "axed" || s === "cancel" || s === "cancelled") return "CANCELLED";
                return "UNKNOWN";
            }

            function formatChapterNumber(num) {
                const parsed = parseFloat(num);
                if (isNaN(parsed)) return "0";
                return Number.isInteger(parsed) ? String(parsed) : String(parsed);
            }

            const extension = {
                baseUrl: "https://asurascans.com",
                apiUrl: "https://api.asurascans.com/api",
                _lastRequestTime: 0,

                _headers(customHeaders = {}) {
                    return {
                        "Referer": "https://asurascans.com/",
                        "Origin": "https://asurascans.com",
                        "Accept": "application/json, text/plain, */*",
                        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                        ...customHeaders
                    };
                },

                async _throttle(url) {
                    if (url && (url.includes("/covers/") || url.includes("/_astro/") || url.endsWith(".webp") || url.endsWith(".jpg") || url.endsWith(".png"))) {
                        return;
                    }
                    const now = Date.now();
                    const elapsed = now - (this._lastRequestTime || 0);
                    const minInterval = 50; // Kept snappy for test runs while validating pacing logic
                    if (elapsed < minInterval) {
                        const delay = minInterval - elapsed;
                        this._lastRequestTime = now + delay;
                        if (typeof dokuha !== 'undefined' && typeof dokuha.sleep === 'function') {
                            await dokuha.sleep(delay);
                        } else if (typeof __dokuha_sleep === 'function') {
                            await __dokuha_sleep(delay);
                        } else if (typeof setTimeout === 'function') {
                            await new Promise(resolve => setTimeout(resolve, delay));
                        }
                    } else {
                        this._lastRequestTime = now;
                    }
                },

                async _fetch(url, options = {}) {
                    await this._throttle(url);
                    const headers = this._headers(options.headers || {});
                    if (typeof dokuha !== 'undefined' && typeof dokuha.fetch === 'function') {
                        return await dokuha.fetch(url, { ...options, headers });
                    }
                    if (typeof fetch === 'function') {
                        return await fetch(url, { ...options, headers });
                    }
                    const res = await new Client().get(url, headers);
                    return {
                        ok: res.statusCode >= 200 && res.statusCode < 300,
                        status: res.statusCode,
                        async json() { return typeof res.body === 'string' ? JSON.parse(res.body) : res.body; },
                        async text() { return res.body || ''; }
                    };
                },

                async getPopular(page = 1) {
                    const limit = PER_PAGE_LIMIT;
                    const offset = Math.max(0, (page - 1) * limit);
                    const url = `${'$'}{this.apiUrl}/series?offset=${'$'}{offset}&limit=${'$'}{limit}&order=popular`;

                    const res = await this._fetch(url);
                    if (!res.ok) return { mangas: [], hasNextPage: false };

                    const data = await res.json();
                    const items = (data && data.data) || [];
                    const meta = (data && data.meta) || {};

                    const mangas = items.map(item => {
                        const publicUrl = item.public_url || `/comics/${'$'}{item.slug}`;
                        if (item.slug && item.public_url) {
                            setCachedSlug(item.slug, item.public_url);
                        }
                        return {
                            url: publicUrl,
                            title: item.title || "Untitled",
                            thumbnailUrl: item.coverUrl || item.cover || "",
                            status: "UNKNOWN",
                            genres: [],
                            isNovel: false
                        };
                    });

                    const hasNext = Boolean(meta.has_more ?? (items.length >= limit));
                    return { mangas, hasNextPage: hasNext };
                },

                async getDetails(url) {
                    const clean = cleanSlug(url);
                    const resolvedSlug = getCachedSlug(clean);
                    let targetUrl = url.startsWith("http")
                        ? url
                        : `${'$'}{this.baseUrl}/comics/${'$'}{resolvedSlug}`;

                    let res = await this._fetch(targetUrl);

                    if (!res.ok && URL_RANDOM_PART.test(targetUrl)) {
                        const fallbackUrl = targetUrl.replace(URL_RANDOM_PART, "");
                        const retryRes = await this._fetch(fallbackUrl);
                        if (retryRes.ok) {
                            res = retryRes;
                            targetUrl = fallbackUrl;
                        }
                    }

                    if (!res.ok) {
                        return {
                            url: url,
                            title: "Unknown",
                            thumbnailUrl: "",
                            description: "",
                            author: "",
                            artist: "",
                            genres: [],
                            status: "UNKNOWN",
                            isNovel: false
                        };
                    }

                    const html = await res.text();
                    const data = extractAstroData(html, "title");
                    const publicUrlData = extractAstroData(html, "publicUrl");

                    let canonicalUrl = targetUrl;
                    const extractedPublicUrl = (publicUrlData && (publicUrlData.publicUrl || publicUrlData.public_url)) ||
                        (data && (data.publicUrl || data.public_url));

                    if (extractedPublicUrl) {
                        canonicalUrl = extractedPublicUrl.startsWith("http")
                            ? extractedPublicUrl
                            : `${'$'}{this.baseUrl}${'$'}{extractedPublicUrl.startsWith("/") ? "" : "/"}${'$'}{extractedPublicUrl}`;
                        const canonicalSlug = cleanSlug(canonicalUrl);
                        if (canonicalSlug) {
                            setCachedSlug(clean, canonicalSlug);
                        }
                    } else if (URL_RANDOM_PART.test(clean)) {
                        setCachedSlug(clean, cleanSlug(targetUrl));
                    }

                    const returnUrl = canonicalUrl.startsWith("http")
                        ? canonicalUrl.replace(this.baseUrl, "")
                        : canonicalUrl;

                    if (data) {
                        const genres = buildGenres(data);
                        const desc = parseDescription(data);

                        return {
                            url: returnUrl.startsWith("/") ? returnUrl : `/${'$'}{returnUrl}`,
                            title: data.title || "Untitled",
                            thumbnailUrl: data.coverUrl || data.cover || "",
                            author: data.author || "",
                            artist: data.artist || "",
                            description: desc,
                            genres: genres,
                            status: mapStatus(data.status),
                            isNovel: false
                        };
                    }

                    return {
                        url: returnUrl.startsWith("/") ? returnUrl : `/${'$'}{returnUrl}`,
                        title: "Unknown",
                        thumbnailUrl: "",
                        description: "",
                        author: "",
                        artist: "",
                        genres: [],
                        status: "UNKNOWN",
                        isNovel: false
                    };
                },

                async getChapters(url, options = {}) {
                    const clean = cleanSlug(url);
                    const resolvedSlug = getCachedSlug(clean);
                    let targetUrl = url.startsWith("http")
                        ? url
                        : `${'$'}{this.baseUrl}/comics/${'$'}{resolvedSlug}`;

                    let res = await this._fetch(targetUrl);

                    if (!res.ok && URL_RANDOM_PART.test(targetUrl)) {
                        const fallbackUrl = targetUrl.replace(URL_RANDOM_PART, "");
                        const retryRes = await this._fetch(fallbackUrl);
                        if (retryRes.ok) {
                            res = retryRes;
                            targetUrl = fallbackUrl;
                        }
                    }

                    if (!res.ok) return [];

                    const html = await res.text();
                    const data = extractAstroData(html, "chapters");
                    const rawChapters = (data && data.chapters) ? data.chapters : [];

                    const publicUrlData = extractAstroData(html, "publicUrl");
                    const extractedPublicUrl = (publicUrlData && (publicUrlData.publicUrl || publicUrlData.public_url)) ||
                        (data && (data.publicUrl || data.public_url));
                    if (extractedPublicUrl) {
                        setCachedSlug(clean, cleanSlug(extractedPublicUrl));
                    }

                    const currentSlug = getCachedSlug(clean) || clean;
                    const now = Date.now();
                    const hidePremium = Boolean(options && (options.hidePremium || options.pref_hide_premium_chapters));

                    const chapters = [];
                    for (const ch of rawChapters) {
                        const isPremium = Boolean(ch.is_premium || ch.is_locked);
                        const isEarlyAccess = Boolean(ch.early_access_until && new Date(ch.early_access_until).getTime() > now);
                        const isLocked = isPremium || isEarlyAccess;

                        if (hidePremium && isLocked) {
                            continue;
                        }

                        const numFloat = parseFloat(ch.number) || 0;
                        const numStr = formatChapterNumber(numFloat);
                        const chapterSlug = ch.series_slug || currentSlug;
                        const chapterUrl = `/comics/${'$'}{chapterSlug}/chapter/${'$'}{numStr}`;

                        let name = "";
                        if (isLocked) name += "[Locked] ";
                        name += `Chapter ${'$'}{numStr}`;
                        if (ch.title && ch.title.trim().length > 0) {
                            name += `: ${'$'}{ch.title.trim()}`;
                        }

                        const dateUpload = ch.created_at ? (new Date(ch.created_at).getTime() || 0) : 0;

                        chapters.push({
                            url: chapterUrl,
                            name: name,
                            chapterNumber: numFloat,
                            dateUpload: dateUpload,
                            scanlator: isLocked ? "[Early Access]" : "Asura Scans",
                            isLocked: isLocked
                        });
                    }

                    return chapters;
                },

                async getPages(chapterUrl) {
                    let fullUrl = chapterUrl.startsWith("http") ? chapterUrl : `${'$'}{this.baseUrl}${'$'}{chapterUrl.startsWith("/") ? "" : "/"}${'$'}{chapterUrl}`;
                    let res = await this._fetch(fullUrl);

                    if (!res.ok && URL_RANDOM_PART.test(fullUrl)) {
                        const fallbackUrl = fullUrl.replace(URL_RANDOM_PART, "");
                        const retryRes = await this._fetch(fallbackUrl);
                        if (retryRes.ok) {
                            res = retryRes;
                            fullUrl = fallbackUrl;
                        }
                    }

                    if (!res.ok) return [];

                    const html = await res.text();
                    const data = extractAstroData(html, "pages");
                    let pages = (data && data.pages) ? data.pages : [];

                    // Authenticated / Token fallback for locked chapters (Keiyoushi auth parity)
                    if (pages.length === 0) {
                        try {
                            const pageTokenMatch = html.match(PAGE_TOKEN_REGEX) || html.match(/"pageToken":\s*"([^"]+)"/);
                            const pageToken = pageTokenMatch ? pageTokenMatch[1] : "asura-reader-2026";

                            const pathSegments = fullUrl.replace(this.baseUrl, "").split("/").filter(Boolean);
                            const comicsIdx = pathSegments.indexOf("comics");
                            const mangaSlug = comicsIdx !== -1 && pathSegments[comicsIdx + 1] ? pathSegments[comicsIdx + 1] : "";
                            const chapterIdx = pathSegments.indexOf("chapter");
                            const chapterNumber = chapterIdx !== -1 && pathSegments[chapterIdx + 1] ? pathSegments[chapterIdx + 1] : "";

                            if (mangaSlug && chapterNumber) {
                                const apiUrl = `${'$'}{this.apiUrl}/series/${'$'}{mangaSlug}/chapters/${'$'}{chapterNumber}`;
                                const headers = { "X-Page-Token": pageToken };

                                let accessToken = "";
                                try {
                                    if (typeof dokuha !== 'undefined' && typeof dokuha.getCookies === 'function') {
                                        const cookies = dokuha.getCookies(this.baseUrl) || "";
                                        const m = cookies.match(/access_token=([^;]+)/);
                                        if (m) accessToken = m[1];
                                    }
                                    if (!accessToken && typeof dokuha !== 'undefined' && dokuha.storage && typeof dokuha.storage.getString === 'function') {
                                        accessToken = dokuha.storage.getString("access_token", "");
                                    }
                                } catch (_) {}

                                if (accessToken) {
                                    headers["Authorization"] = `Bearer ${'$'}{accessToken}`;
                                }

                                const apiRes = await this._fetch(apiUrl, { headers });
                                if (apiRes.ok) {
                                    const apiJson = await apiRes.json();
                                    if (apiJson && apiJson.data && apiJson.data.chapter && Array.isArray(apiJson.data.chapter.pages)) {
                                        pages = apiJson.data.chapter.pages;
                                    }
                                }
                            }
                        } catch (_) {}
                    }

                    // Fallback regex if still empty
                    if (pages.length === 0) {
                        const match = html.match(/"pages":\s*(\[[^\]]+\])/);
                        if (match && match[1]) {
                            try {
                                pages = JSON.parse(match[1]);
                            } catch (_) {}
                        }
                    }

                    return pages.map((p, idx) => {
                        const rawImg = typeof p === "string" ? p : (p.url || p.imageUrl || "");
                        let finalUrl = rawImg;
                        const hasTiles = p && Array.isArray(p.tiles) && p.tiles.length > 0;
                        let tileData = null;
                        if (hasTiles) {
                            tileData = {
                                tiles: p.tiles,
                                tileCols: p.tile_cols || p.tileCols || 4,
                                tileRows: p.tile_rows || p.tileRows || 5
                            };
                            finalUrl = `${'$'}{rawImg}#${'$'}{encodeURIComponent(JSON.stringify(tileData))}`;
                        }
                        const item = {
                            index: idx,
                            url: finalUrl,
                            imageUrl: finalUrl
                        };
                        if (tileData) {
                            item.tiles = tileData.tiles;
                            item.tileCols = tileData.tileCols;
                            item.tileRows = tileData.tileRows;
                        }
                        return item;
                    });
                }
            };
        """.trimIndent()
    }

    @Test
    fun testAsuraScansPopularCatalogAndInfinitePagination() = runBlocking {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("/api/series")) {
                val json = """
                {
                    "data": [
                        {
                            "slug": "solo-max-level-newbie",
                            "title": "Solo Max-Level Newbie",
                            "coverUrl": "https://cdn.asurascans.com/covers/solo.webp",
                            "public_url": "/comics/solo-max-level-newbie-f212c582"
                        },
                        {
                            "slug": "bad-born-blood",
                            "title": "Bad Born Blood",
                            "coverUrl": "https://cdn.asurascans.com/covers/bad.webp",
                            "public_url": "/comics/bad-born-blood-f212c582"
                        }
                    ],
                    "meta": {
                        "has_more": true
                    }
                }
                """.trimIndent()
                respond(
                    content = json,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("Not Found", status = HttpStatusCode.NotFound)
            }
        }

        val httpClient = HttpClient(mockEngine)
        val bridge = JsBridge(httpClient = httpClient)
        val engine = JsEngine()
        val manifest = JsExtensionManifest(
            id = "asurascans",
            name = "Asura Scans",
            version = "1.2.0",
            lang = "en",
            baseUrl = "https://asurascans.com",
            type = ExtensionType.COMIC,
            isNsfw = false,
            script = TEST_ASURA_SCANS_BUNDLE
        )
        val source = JsExtensionSource(manifest, engine, bridge)
        source.initialize()

        val page1 = source.getPopular(1)
        assertEquals(2, page1.mangas.size)
        assertEquals("Solo Max-Level Newbie", page1.mangas[0].title)
        assertEquals("/comics/solo-max-level-newbie-f212c582", page1.mangas[0].url)
        assertEquals("https://cdn.asurascans.com/covers/solo.webp", page1.mangas[0].thumbnailUrl)
        assertTrue(page1.hasNextPage, "Catalogue should indicate hasNextPage=true for infinite scroll")

        source.close()
    }

    @Test
    fun testAsuraScansDetailsAndChaptersAstroPropsExtraction() = runBlocking {
        val mockHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Bad Born Blood | Asura Scans</title>
        </head>
        <body>
            <astro-island component-url="/_astro/Title.js" props="{&quot;title&quot;:[0,&quot;Bad Born Blood&quot;],&quot;author&quot;:[0,&quot;Kim Chul-soo&quot;],&quot;artist&quot;:[0,&quot;Lee Young-hee&quot;],&quot;description&quot;:[0,&quot;&lt;p&gt;A gripping martial arts story.&lt;/p&gt;&quot;],&quot;status&quot;:[0,&quot;ongoing&quot;],&quot;genres&quot;:[1,[[0,&quot;Action&quot;],[0,&quot;Martial Arts&quot;]]],&quot;coverUrl&quot;:[0,&quot;https://cdn.asurascans.com/covers/bad.webp&quot;]}"></astro-island>
            <astro-island component-url="/_astro/Chapters.js" props="{&quot;chapters&quot;:[1,[[0,{&quot;number&quot;:[0,2.0],&quot;title&quot;:[0,&quot;The Clan War&quot;],&quot;created_at&quot;:[0,&quot;2026-11-10T12:00:00Z&quot;],&quot;is_premium&quot;:[0,false],&quot;series_slug&quot;:[0,&quot;bad-born-blood&quot;]}],[0,{&quot;number&quot;:[0,1.0],&quot;title&quot;:[0,&quot;Prologue&quot;],&quot;created_at&quot;:[0,&quot;2026-11-01T12:00:00Z&quot;],&quot;is_premium&quot;:[0,false],&quot;series_slug&quot;:[0,&quot;bad-born-blood&quot;]}]]]}"></astro-island>
        </body>
        </html>
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockHtml,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }

        val httpClient = HttpClient(mockEngine)
        val bridge = JsBridge(httpClient = httpClient)
        val engine = JsEngine()
        val manifest = JsExtensionManifest(
            id = "asurascans",
            name = "Asura Scans",
            version = "1.2.0",
            lang = "en",
            baseUrl = "https://asurascans.com",
            type = ExtensionType.COMIC,
            isNsfw = false,
            script = TEST_ASURA_SCANS_BUNDLE
        )
        val source = JsExtensionSource(manifest, engine, bridge)
        source.initialize()

        // 1. Details
        val details = source.getMangaDetails(SourceManga(url = "/comics/bad-born-blood", title = "Fallback Title"))
        assertEquals("Bad Born Blood", details.title)
        assertEquals("Kim Chul-soo", details.author)
        assertEquals("Lee Young-hee", details.artist)
        assertEquals("A gripping martial arts story.", details.description)
        assertEquals(MangaStatus.ONGOING, details.status)
        assertEquals(listOf("Action", "Martial Arts"), details.genres)

        // 2. Chapters
        val chapters = source.getChapterList(SourceManga(url = "/comics/bad-born-blood", title = "Bad Born Blood"))
        assertEquals(2, chapters.size)
        assertEquals("Chapter 2: The Clan War", chapters[0].name)
        assertEquals(2.0f, chapters[0].chapterNumber)
        assertEquals("/comics/bad-born-blood/chapter/2", chapters[0].url)
        assertEquals("Chapter 1: Prologue", chapters[1].name)
        assertEquals(1.0f, chapters[1].chapterNumber)

        source.close()
    }

    @Test
    fun testAsuraScansRichMetadataLockedChaptersAndTiling() = runBlocking {
        val detailsMockHtml = """
        <!DOCTYPE html>
        <html>
        <head><title>Solo Leveling</title></head>
        <body>
            <astro-island component-url="/_astro/Title.js" props="{&quot;title&quot;:[0,&quot;Solo Leveling&quot;],&quot;author&quot;:[0,&quot;Chugong&quot;],&quot;artist&quot;:[0,&quot;DUBU&quot;],&quot;type&quot;:[0,&quot;manhwa&quot;],&quot;popularityRank&quot;:[0,1],&quot;rating&quot;:[0,9.85],&quot;bookmarkCount&quot;:[0,1250000],&quot;alternativeTitles&quot;:[0,&quot;I Alone Level Up • Na Honjaman Level Up&quot;],&quot;description&quot;:[0,&quot;In a world where hunters must battle deadly monsters...&quot;],&quot;status&quot;:[0,&quot;completed&quot;],&quot;genres&quot;:[1,[[0,&quot;Action&quot;],[0,&quot;Fantasy&quot;]]],&quot;coverUrl&quot;:[0,&quot;https://cdn.asurascans.com/covers/solo.webp&quot;]}"></astro-island>
            <astro-island component-url="/_astro/Chapters.js" props="{&quot;chapters&quot;:[1,[[0,{&quot;number&quot;:[0,100.0],&quot;title&quot;:[0,&quot;The Monarch&quot;],&quot;created_at&quot;:[0,&quot;2026-11-15T12:00:00Z&quot;],&quot;is_premium&quot;:[0,true],&quot;series_slug&quot;:[0,&quot;solo-leveling&quot;]}],[0,{&quot;number&quot;:[0,99.0],&quot;title&quot;:[0,&quot;Shadow Army&quot;],&quot;created_at&quot;:[0,&quot;2026-11-01T12:00:00Z&quot;],&quot;is_premium&quot;:[0,false],&quot;series_slug&quot;:[0,&quot;solo-leveling&quot;]}]]]}"></astro-island>
        </body>
        </html>
        """.trimIndent()

        val pagesMockHtml = """
        <!DOCTYPE html>
        <html>
        <head><title>Chapter 100</title></head>
        <body>
            <astro-island component-url="/_astro/Reader.js" props="{&quot;pages&quot;:[1,[[0,{&quot;url&quot;:[0,&quot;https://cdn.asurascans.com/page1.webp&quot;],&quot;tiles&quot;:[1,[1,0,3,2]],&quot;tile_cols&quot;:[0,2],&quot;tile_rows&quot;:[0,2]}],[0,{&quot;url&quot;:[0,&quot;https://cdn.asurascans.com/page2.webp&quot;]}]]]}"></astro-island>
        </body>
        </html>
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("/chapter/")) {
                respond(content = pagesMockHtml, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "text/html"))
            } else {
                respond(content = detailsMockHtml, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "text/html"))
            }
        }

        val httpClient = HttpClient(mockEngine)
        val bridge = JsBridge(httpClient = httpClient)
        val engine = JsEngine()
        val manifest = JsExtensionManifest(
            id = "asurascans",
            name = "Asura Scans",
            version = "1.2.0",
            lang = "en",
            baseUrl = "https://asurascans.com",
            type = ExtensionType.COMIC,
            isNsfw = false,
            script = TEST_ASURA_SCANS_BUNDLE
        )
        val source = JsExtensionSource(manifest, engine, bridge)
        source.initialize()

        // 1. Details with rich description & type in genre
        val details = source.getMangaDetails(SourceManga(url = "/comics/solo-leveling-8f2a1b9c", title = "Fallback Title"))
        assertEquals("Solo Leveling", details.title)
        assertEquals(listOf("Manhwa", "Action", "Fantasy"), details.genres)
        assertEquals(MangaStatus.COMPLETED, details.status)
        assertTrue(details.description?.contains("Rank: #1 • Rating: 9.85 • Bookmarks: 1.3M") == true)
        assertTrue(details.description?.contains("Alternative Titles:\n- I Alone Level Up\n- Na Honjaman Level Up") == true)

        // 2. Chapters with [Locked] prefix on premium chapter
        val chapters = source.getChapterList(SourceManga(url = "/comics/solo-leveling-8f2a1b9c", title = "Solo Leveling"))
        assertEquals(2, chapters.size)
        assertEquals("[Locked] Chapter 100: The Monarch", chapters[0].name)
        assertEquals("Chapter 99: Shadow Army", chapters[1].name)

        // 3. Pages with tiling URI fragment
        val pages = source.getPageList(chapters[0])
        assertEquals(2, pages.size)
        assertTrue(pages[0].imageUrl?.contains("%7B%22tiles%22%3A%5B1%2C0%2C3%2C2%5D") == true || pages[0].imageUrl?.contains("tiles") == true)
        assertEquals("https://cdn.asurascans.com/page2.webp", pages[1].imageUrl)

        source.close()
    }

    @Test
    fun testAsuraScansSlugDriftRecoveryAndCanonicalUrlPropagation() = runBlocking {
        // Outdated URL with expired random hash that 404s, but resolves upon fallback
        val mockRedirectHtml = """
        <!DOCTYPE html>
        <html>
        <head><title>Reincarnation of the Veteran Soldier</title></head>
        <body>
            <astro-island component-url="/_astro/Title.js" props="{&quot;title&quot;:[0,&quot;Veteran Soldier&quot;],&quot;publicUrl&quot;:[0,&quot;/comics/veteran-soldier-new77889&quot;],&quot;status&quot;:[0,&quot;ongoing&quot;]}"></astro-island>
            <astro-island component-url="/_astro/Chapters.js" props="{&quot;chapters&quot;:[1,[[0,{&quot;number&quot;:[0,1.0],&quot;title&quot;:[0,&quot;Chapter 1&quot;],&quot;series_slug&quot;:[0,&quot;veteran-soldier-new77889&quot;]}]]]}"></astro-island>
        </body>
        </html>
        """.trimIndent()

        var initialAttemptCount = 0
        var fallbackAttemptCount = 0

        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("/comics/veteran-soldier-old11223")) {
                initialAttemptCount++
                // Expired hash 404s
                respond("Not Found", status = HttpStatusCode.NotFound)
            } else if (url.contains("/comics/veteran-soldier") || url.contains("/comics/veteran-soldier-new77889")) {
                fallbackAttemptCount++
                respond(content = mockRedirectHtml, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "text/html"))
            } else {
                respond("Not Found", status = HttpStatusCode.NotFound)
            }
        }

        val httpClient = HttpClient(mockEngine)
        val bridge = JsBridge(httpClient = httpClient)
        val engine = JsEngine()
        val manifest = JsExtensionManifest(
            id = "asurascans",
            name = "Asura Scans",
            version = "1.2.0",
            lang = "en",
            baseUrl = "https://asurascans.com",
            type = ExtensionType.COMIC,
            isNsfw = false,
            script = TEST_ASURA_SCANS_BUNDLE
        )
        val source = JsExtensionSource(manifest, engine, bridge)
        source.initialize()

        // Request with outdated URL
        val details = source.getMangaDetails(SourceManga(url = "/comics/veteran-soldier-old11223", title = "Old Title"))
        assertEquals("Veteran Soldier", details.title)
        // Canonical publicUrl extracted and propagated
        assertEquals("/comics/veteran-soldier-new77889", details.url)
        assertEquals(1, initialAttemptCount, "Should have attempted the expired URL once")
        assertEquals(1, fallbackAttemptCount, "Should have fallen back to strip random suffix")

        // Subsequent chapter lookup should hit the cached canonical slug directly without 404
        val chapters = source.getChapterList(details)
        assertEquals(1, chapters.size)
        assertEquals("/comics/veteran-soldier-new77889/chapter/1", chapters[0].url)

        source.close()
    }
}
