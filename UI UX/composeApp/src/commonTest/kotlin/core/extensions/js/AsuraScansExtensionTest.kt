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
                version: "1.0.0",
                lang: "en",
                baseUrl: "https://asurascans.com",
                type: "COMIC",
                isNsfw: false
            };

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
                    for (const k of Object.keys(val)) {
                        obj[k] = unwrapAstro(val[k]);
                    }
                    return obj;
                }
                return val;
            }

            function extractAstroData(html, key) {
                if (!html) return null;
                try {
                    if (typeof dokuha !== 'undefined' && typeof dokuha.parseHtml === 'function') {
                        const ${'$'} = dokuha.parseHtml(html);
                        const island = ${'$'}(`[props*="${'$'}{key}"]`).first();
                        const raw = island.attr("props");
                        if (raw) {
                            return unwrapAstro(JSON.parse(raw));
                        }
                    } else if (typeof Document !== 'undefined') {
                        const doc = new Document(html);
                        const el = doc.selectFirst(`[props*="${'$'}{key}"]`);
                        if (el) {
                            const raw = el.attr("props");
                            if (raw) return unwrapAstro(JSON.parse(raw));
                        }
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

            const extension = {
                baseUrl: "https://asurascans.com",
                apiUrl: "https://api.asurascans.com/api",

                _headers() {
                    return {
                        "Referer": "https://asurascans.com/",
                        "Origin": "https://asurascans.com",
                        "Accept": "application/json, text/plain, */*",
                        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    };
                },

                async _fetch(url) {
                    if (typeof dokuha !== 'undefined' && typeof dokuha.fetch === 'function') {
                        return await dokuha.fetch(url, { headers: this._headers() });
                    }
                    const res = await new Client().get(url, this._headers());
                    return {
                        ok: res.statusCode >= 200 && res.statusCode < 300,
                        status: res.statusCode,
                        async json() { return typeof res.body === 'string' ? JSON.parse(res.body) : res.body; },
                        async text() { return res.body || ''; }
                    };
                },

                async getPopular(page = 1) {
                    const limit = 20;
                    const offset = Math.max(0, (page - 1) * limit);
                    const url = `${'$'}{this.apiUrl}/series?offset=${'$'}{offset}&limit=${'$'}{limit}&order=popular`;

                    const res = await this._fetch(url);
                    if (!res.ok) return { mangas: [], hasNextPage: false };

                    const data = await res.json();
                    const items = (data && data.data) || [];
                    const meta = (data && data.meta) || {};

                    const mangas = items.map(item => ({
                        url: item.public_url || `/comics/${'$'}{item.slug}`,
                        title: item.title || "Untitled",
                        thumbnailUrl: item.coverUrl || item.cover || "",
                        status: "UNKNOWN",
                        genres: [],
                        isNovel: false
                    }));

                    const hasNext = Boolean(meta.has_more ?? (items.length >= limit));
                    return { mangas, hasNextPage: hasNext };
                },

                async getDetails(url) {
                    const fullUrl = url.startsWith("http") ? url : `${'$'}{this.baseUrl}${'$'}{url.startsWith("/") ? "" : "/"}${'$'}{url}`;
                    const res = await this._fetch(fullUrl);
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
                },

                async getChapters(url) {
                    const fullUrl = url.startsWith("http") ? url : `${'$'}{this.baseUrl}${'$'}{url.startsWith("/") ? "" : "/"}${'$'}{url}`;
                    const res = await this._fetch(fullUrl);
                    if (!res.ok) return [];

                    const html = await res.text();
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
            version = "2.0.0",
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
            version = "2.0.0",
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
}
