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

class MangaDexExtensionTest {

    private val mangaDexScript = """
/**
 * MangaDex Extension for Dokuha
 */
const manifest = {
  "id": "mangadex",
  "name": "MangaDex",
  "version": "1.0.0",
  "lang": "en",
  "baseUrl": "https://mangadex.org",
  "type": "COMIC",
  "iconUrl": "https://mangadex.org/favicon.ico",
  "isNsfw": false
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
      return `${'$'}{this.coverBaseUrl}/${'$'}{mangaId}/${'$'}{coverRel.attributes.fileName}.256.jpg`;
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

    let status = "UNKNOWN";
    if (attr.status === "ongoing") status = "ONGOING";
    else if (attr.status === "completed") status = "COMPLETED";
    else if (attr.status === "hiatus") status = "ON_HIATUS";
    else if (attr.status === "cancelled") status = "CANCELLED";

    return {
      url: id,
      title: title,
      thumbnailUrl: coverUrl,
      author: author,
      artist: artist,
      description: description,
      genres: genres,
      status: status,
      isNovel: false
    };
  },

  async getPopular(page) {
    const limit = 20;
    const offset = Math.max(0, (page - 1) * limit);
    const url = `${'$'}{this.apiUrl}/manga?limit=${'$'}{limit}&offset=${'$'}{offset}&includes[]=cover_art&includes[]=author&includes[]=artist&order[followedCount]=desc&contentRating[]=safe&contentRating[]=suggestive`;

    const res = await dokuha.fetch(url, { headers: this._headers() });
    const data = await res.json();
    const mangas = (data.data || []).map(item => this._mapManga(item));
    const total = data.total || 0;
    const hasNext = offset + limit < total;
    return { mangas: mangas, hasNextPage: hasNext };
  },

  async getLatestUpdates(page) {
    return this.getPopular(page);
  },

  async search(query, page, filters) {
    const limit = 20;
    const offset = Math.max(0, (page - 1) * limit);
    let url = `${'$'}{this.apiUrl}/manga?limit=${'$'}{limit}&offset=${'$'}{offset}&includes[]=cover_art&includes[]=author&includes[]=artist&contentRating[]=safe&contentRating[]=suggestive`;
    if (query && query.trim().length > 0) {
      url += `&title=${'$'}{encodeURIComponent(query.trim())}`;
    } else {
      url += `&order[followedCount]=desc`;
    }

    const res = await dokuha.fetch(url, { headers: this._headers() });
    const data = await res.json();
    const mangas = (data.data || []).map(item => this._mapManga(item));
    const total = data.total || 0;
    const hasNext = offset + limit < total;
    return { mangas: mangas, hasNextPage: hasNext };
  },

  async getDetails(url) {
    const id = url.replace("https://mangadex.org/title/", "").replace("https://api.mangadex.org/manga/", "").split("/")[0];
    const fetchUrl = `${'$'}{this.apiUrl}/manga/${'$'}{id}?includes[]=cover_art&includes[]=author&includes[]=artist`;

    const res = await dokuha.fetch(fetchUrl, { headers: this._headers() });
    const json = await res.json();
    if (json.data) {
      return this._mapManga(json.data);
    }
    return { url: id, title: "Unknown", isNovel: false };
  },

  async getChapters(url) {
    const id = url.replace("https://mangadex.org/title/", "").replace("https://api.mangadex.org/manga/", "").split("/")[0];
    const fetchUrl = `${'$'}{this.apiUrl}/manga/${'$'}{id}/feed?limit=100&translatedLanguage[]=en&order[chapter]=desc&includeExternalUrl=0`;

    const res = await dokuha.fetch(fetchUrl, { headers: this._headers() });
    const json = await res.json();
    const chaptersData = json.data || [];

    return chaptersData.map(ch => {
      const attr = ch.attributes || {};
      const chNum = parseFloat(attr.chapter) || 0;
      const title = attr.title ? ` - ${'$'}{attr.title}` : "";
      const name = attr.chapter ? `Chapter ${'$'}{attr.chapter}${'$'}{title}` : (attr.title || "Oneshot");
      const dateUpload = attr.publishAt ? new Date(attr.publishAt).getTime() : 0;

      return {
        url: ch.id,
        name: name,
        chapterNumber: chNum,
        dateUpload: dateUpload,
        scanlator: null
      };
    });
  },

  async getPages(chapterUrl) {
    const id = chapterUrl.split("/").pop();
    const fetchUrl = `${'$'}{this.apiUrl}/at-home/server/${'$'}{id}`;

    const res = await dokuha.fetch(fetchUrl, { headers: this._headers() });
    const json = await res.json();

    if (json.result !== "ok" || !json.baseUrl || !json.chapter) {
      return [];
    }

    const baseUrl = json.baseUrl;
    const hash = json.chapter.hash;
    const files = json.chapter.data || [];

    return files.map((file, idx) => ({
      index: idx + 1,
      url: `${'$'}{baseUrl}/data/${'$'}{hash}/${'$'}{file}`,
      imageUrl: `${'$'}{baseUrl}/data/${'$'}{hash}/${'$'}{file}`
    }));
  }
};
    """.trimIndent()

    private fun createMockMangaDexClient(): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val urlStr = request.url.toString()
                    when {
                        urlStr.contains("/manga?") -> {
                            val isSearch = urlStr.contains("title=Frieren")
                            val title = if (isSearch) "Sousou no Frieren" else "Solo Leveling"
                            val mangaId = if (isSearch) "b0b721ff-c388-4486-aa0f-c2b0bb321512" else "32d76d19-8a05-4db0-9fc2-e0b0648fe9d0"
                            val json = """
                            {
                                "result": "ok",
                                "total": 1,
                                "data": [
                                    {
                                        "id": "$mangaId",
                                        "type": "manga",
                                        "attributes": {
                                            "title": { "en": "$title" },
                                            "description": { "en": "An epic journey through fantasy realms." },
                                            "status": "ongoing",
                                            "tags": [
                                                { "attributes": { "name": { "en": "Action" } } },
                                                { "attributes": { "name": { "en": "Fantasy" } } }
                                            ]
                                        },
                                        "relationships": [
                                            {
                                                "id": "cover-123",
                                                "type": "cover_art",
                                                "attributes": { "fileName": "cover_art.jpg" }
                                            },
                                            {
                                                "id": "author-456",
                                                "type": "author",
                                                "attributes": { "name": "Kanehito Yamada" }
                                            }
                                        ]
                                    }
                                ]
                            }
                            """.trimIndent()
                            respond(
                                content = json,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        urlStr.contains("/feed?") -> {
                            val json = """
                            {
                                "result": "ok",
                                "total": 2,
                                "data": [
                                    {
                                        "id": "chap-002",
                                        "type": "chapter",
                                        "attributes": {
                                            "chapter": "2",
                                            "title": "Priest's Lie",
                                            "publishAt": "2026-01-02T12:00:00Z"
                                        }
                                    },
                                    {
                                        "id": "chap-001",
                                        "type": "chapter",
                                        "attributes": {
                                            "chapter": "1",
                                            "title": "The End of the Adventure",
                                            "publishAt": "2026-01-01T12:00:00Z"
                                        }
                                    }
                                ]
                            }
                            """.trimIndent()
                            respond(
                                content = json,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        urlStr.contains("/at-home/server/") -> {
                            val json = """
                            {
                                "result": "ok",
                                "baseUrl": "https://cdn.mangadex.network",
                                "chapter": {
                                    "hash": "abcdef123456",
                                    "data": [
                                        "p001.jpg",
                                        "p002.jpg",
                                        "p003.jpg"
                                    ]
                                }
                            }
                            """.trimIndent()
                            respond(
                                content = json,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        else -> {
                            respond(
                                content = "Not found",
                                status = HttpStatusCode.NotFound
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testMangaDexExtensionExecution() = runBlocking {
        val mockClient = createMockMangaDexClient()
        val bridge = JsBridge(httpClient = mockClient)
        val manifest = JsExtensionManifest(
            id = "mangadex",
            name = "MangaDex",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://mangadex.org",
            type = ExtensionType.COMIC,
            script = mangaDexScript
        )
        val engine = JsEngine()
        val source = JsExtensionSource(manifest = manifest, engine = engine, bridge = bridge)
        source.initialize()

        // 1. Validate manifest extraction
        assertEquals("mangadex", source.manifest.id)
        assertEquals("MangaDex", source.manifest.name)
        assertEquals("en", source.manifest.lang)

        // 2. Validate getPopular
        val popular = source.getPopular(1)
        assertEquals(1, popular.mangas.size)
        val manga = popular.mangas.first()
        assertEquals("32d76d19-8a05-4db0-9fc2-e0b0648fe9d0", manga.url)
        assertEquals("Solo Leveling", manga.title)
        assertEquals("Kanehito Yamada", manga.author)
        assertEquals("https://uploads.mangadex.org/covers/32d76d19-8a05-4db0-9fc2-e0b0648fe9d0/cover_art.jpg.256.jpg", manga.thumbnailUrl)
        assertEquals(MangaStatus.ONGOING, manga.status)
        assertTrue(manga.genres.contains("Action"))
        assertTrue(manga.genres.contains("Fantasy"))

        // 3. Validate search
        val searchResult = source.search("Frieren", 1, emptyList())
        assertEquals(1, searchResult.mangas.size)
        assertEquals("Sousou no Frieren", searchResult.mangas.first().title)

        // 4. Validate getChapterList
        val chapters = source.getChapterList(manga)
        assertEquals(2, chapters.size)
        assertEquals("chap-002", chapters[0].url)
        assertEquals("Chapter 2 - Priest's Lie", chapters[0].name)
        assertEquals(2.0f, chapters[0].chapterNumber)
        assertEquals("chap-001", chapters[1].url)
        assertEquals("Chapter 1 - The End of the Adventure", chapters[1].name)
        assertEquals(1.0f, chapters[1].chapterNumber)

        // 5. Validate getPageList
        val pages = source.getPageList(chapters.first())
        assertEquals(3, pages.size)
        assertEquals(1, pages[0].index)
        assertEquals("https://cdn.mangadex.network/data/abcdef123456/p001.jpg", pages[0].imageUrl)
        assertEquals(2, pages[1].index)
        assertEquals("https://cdn.mangadex.network/data/abcdef123456/p002.jpg", pages[1].imageUrl)
        assertEquals(3, pages[2].index)
        assertEquals("https://cdn.mangadex.network/data/abcdef123456/p003.jpg", pages[2].imageUrl)

        source.close()
    }
}
