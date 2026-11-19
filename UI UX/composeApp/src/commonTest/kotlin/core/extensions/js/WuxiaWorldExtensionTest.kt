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

class WuxiaWorldExtensionTest {

    private val wuxiaScript = """
const manifest = {
  "id": "wuxiaworld",
  "name": "WuxiaWorld",
  "version": "1.0.0",
  "lang": "en",
  "baseUrl": "https://wuxiaworld.site",
  "type": "NOVEL",
  "iconUrl": "https://wuxiaworld.site/favicon.ico",
  "isNsfw": false
};

const extension = {
  baseUrl: "https://wuxiaworld.site",

  _headers() {
    return {
      "User-Agent": "Mozilla/5.0",
      "Accept": "text/html"
    };
  },

  async getPopular(page = 1) {
    const url = `${'$'}{this.baseUrl}/novel/?m_orderby=trending`;
    const res = await dokuha.fetch(url, { headers: this._headers() });
    if (!res.ok) return { mangas: [], hasNextPage: false };

    const html = await res.text();
    const $ = dokuha.parseHtml(html);
    const mangas = [];

    $('.page-item-detail').each((i, el) => {
      const titleLink = $(el).find('.post-title a').first();
      const title = titleLink.text().trim();
      const novelUrl = titleLink.attr('href');
      if (!title || !novelUrl) return;

      const img = $(el).find('img').first();
      const thumb = img.attr('data-src') || img.attr('src') || '';

      mangas.push({
        url: novelUrl,
        title: title,
        thumbnailUrl: thumb,
        status: "ONGOING",
        genres: ["Web Novel", "Fantasy"],
        isNovel: true
      });
    });

    return { mangas: mangas, hasNextPage: false };
  },

  async getLatestUpdates(page = 1) {
    return this.getPopular(page);
  },

  async search(query, page = 1, filters = []) {
    const cleanQuery = encodeURIComponent(query || '');
    const url = `${'$'}{this.baseUrl}/?s=${'$'}{cleanQuery}&post_type=wp-manga`;
    const res = await dokuha.fetch(url, { headers: this._headers() });
    if (!res.ok) return { mangas: [], hasNextPage: false };

    const html = await res.text();
    const $ = dokuha.parseHtml(html);
    const mangas = [];

    $('.c-tabs-item__content').each((i, el) => {
      const titleLink = $(el).find('.post-title a').first();
      const title = titleLink.text().trim();
      const novelUrl = titleLink.attr('href');
      if (!title || !novelUrl) return;

      const img = $(el).find('img').first();
      const thumb = img.attr('data-src') || img.attr('src') || '';

      mangas.push({
        url: novelUrl,
        title: title,
        thumbnailUrl: thumb,
        status: "ONGOING",
        genres: ["Web Novel"],
        isNovel: true
      });
    });

    return { mangas: mangas, hasNextPage: false };
  },

  async getDetails(url) {
    const res = await dokuha.fetch(url, { headers: this._headers() });
    if (!res.ok) throw new Error("Failed to load details");

    const html = await res.text();
    const $ = dokuha.parseHtml(html);

    const title = $('.post-title h1').text().trim() || 'Untitled Novel';
    const author = $('.author-content a').first().text().trim() || null;
    const desc = $('.description-summary .summary__content').text().trim() || '';
    
    const genres = [];
    $('.genres-content a').each((i, el) => {
      const g = $(el).text().trim();
      if (g) genres.push(g);
    });

    const img = $('.summary_image img').first();
    const thumb = img.attr('data-src') || img.attr('src') || '';

    return {
      url: url,
      title: title,
      author: author,
      description: desc,
      genres: genres.length > 0 ? genres : ["Web Novel"],
      status: "ONGOING",
      thumbnailUrl: thumb,
      isNovel: true
    };
  },

  async getChapters(url) {
    const ajaxUrl = `${'$'}{url.replace(/\/?$/, '')}/ajax/chapters/`;
    const res = await dokuha.fetch(ajaxUrl, {
      method: "POST",
      headers: this._headers()
    });
    if (!res.ok) return [];

    const html = await res.text();
    const $ = dokuha.parseHtml(html);
    const chapters = [];

    $('.wp-manga-chapter a').each((i, el) => {
      const chapUrl = $(el).attr('href');
      const name = $(el).text().trim();
      if (!chapUrl || !name) return;

      const numMatch = name.match(/chapter\s*(\d+(?:\.\d+)?)/i) || name.match(/(\d+(?:\.\d+)?)/);
      const chapterNumber = numMatch ? parseFloat(numMatch[1]) : (i + 1.0);

      chapters.push({
        url: chapUrl,
        name: name,
        chapterNumber: chapterNumber
      });
    });

    chapters.reverse();
    return chapters;
  },

  async getNovelContent(chapterUrl) {
    const res = await dokuha.fetch(chapterUrl, { headers: this._headers() });
    if (!res.ok) throw new Error("Failed to load chapter content");

    const html = await res.text();
    const $ = dokuha.parseHtml(html);

    const title = $('h3, .breadcrumb .active').first().text().trim() || "Chapter 1";
    const contentNode = $('.reading-content .text-left').first();
    const htmlBody = contentNode.html() || "";
    const plainText = contentNode.text() || "";

    return {
      chapterUrl: chapterUrl,
      title: title,
      htmlBody: htmlBody,
      plainText: plainText
    };
  }
};
    """.trimIndent()

    private val samplePopularHtml = """
        <div class="page-item-detail">
            <div class="item-thumb">
                <img src="https://wuxiaworld.site/covers/attributes.jpg" />
            </div>
            <div class="post-title">
                <a href="https://wuxiaworld.site/novel/complete-martial-arts-attributes-spp/">Complete Martial Arts Attributes</a>
            </div>
        </div>
    """.trimIndent()

    private val sampleSearchHtml = """
        <div class="c-tabs-item__content">
            <div class="tab-thumb">
                <img data-src="https://wuxiaworld.site/covers/biochemical.jpg" src="placeholder.jpg" />
            </div>
            <div class="post-title">
                <a href="https://wuxiaworld.site/novel/biochemical-martial-arts/">Biochemical Martial Arts</a>
            </div>
        </div>
    """.trimIndent()

    private val sampleDetailsHtml = """
        <div class="post-title">
            <h1>Complete Martial Arts Attributes</h1>
        </div>
        <div class="author-content">
            <a href="https://wuxiaworld.site/author/dont-enter/">Don't Enter The Jianghu</a>
        </div>
        <div class="genres-content">
            <a href="https://wuxiaworld.site/genre/action/">Action</a>
            <a href="https://wuxiaworld.site/genre/fantasy/">Fantasy</a>
        </div>
        <div class="summary_image">
            <img data-src="https://wuxiaworld.site/covers/attributes.jpg" />
        </div>
        <div class="description-summary">
            <div class="summary__content show-more">
                Donghai was as hot as a furnace in June. A story of attribute bubbles.
            </div>
        </div>
    """.trimIndent()

    private val sampleAjaxChaptersHtml = """
        <ul class="main version-chap">
            <li class="wp-manga-chapter">
                <a href="https://wuxiaworld.site/novel/complete-martial-arts/chapter-2/">Chapter 2: The Second Day</a>
            </li>
            <li class="wp-manga-chapter">
                <a href="https://wuxiaworld.site/novel/complete-martial-arts/chapter-1/">Chapter 1: This World Isn't Right!</a>
            </li>
        </ul>
    """.trimIndent()

    private val sampleChapterContentHtml = """
        <div class="reading-content">
            <div class="text-left">
                <h3>Chapter 1: This World Isn't Right!</h3>
                <p>Donghai was as hot as a furnace in June.</p>
                <p>The sky was azure blue, and not a cloud was in sight.</p>
            </div>
        </div>
    """.trimIndent()

    @Test
    fun testWuxiaWorldNovelScraper() = runBlocking {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("/ajax/chapters/") -> {
                    respond(
                        content = sampleAjaxChaptersHtml,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
                    )
                }
                url.contains("?s=martial") -> {
                    respond(
                        content = sampleSearchHtml,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
                    )
                }
                url.contains("chapter-1") -> {
                    respond(
                        content = sampleChapterContentHtml,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
                    )
                }
                url.contains("/novel/complete-martial-arts-attributes-spp/") -> {
                    respond(
                        content = sampleDetailsHtml,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
                    )
                }
                else -> {
                    respond(
                        content = samplePopularHtml,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
                    )
                }
            }
        }

        val client = HttpClient(mockEngine)
        val bridge = JsBridge(httpClient = client)
        val manifest = JsExtensionManifest(
            id = "wuxiaworld",
            name = "WuxiaWorld",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://wuxiaworld.site",
            type = ExtensionType.NOVEL,
            script = wuxiaScript
        )
        val engine = JsEngine()
        val source = JsExtensionSource(manifest = manifest, engine = engine, bridge = bridge)
        source.initialize()

        // 1. Validate manifest
        assertEquals("wuxiaworld", source.manifest.id)
        assertEquals("WuxiaWorld", source.manifest.name)
        assertEquals("en", source.manifest.lang)

        // 2. Validate getPopular
        val popular = source.getPopular(1)
        assertEquals(1, popular.mangas.size)
        val novel = popular.mangas.first()
        assertEquals("https://wuxiaworld.site/novel/complete-martial-arts-attributes-spp/", novel.url)
        assertEquals("Complete Martial Arts Attributes", novel.title)
        assertEquals("https://wuxiaworld.site/covers/attributes.jpg", novel.thumbnailUrl)
        assertTrue(novel.isNovel)

        // 3. Validate search
        val search = source.search("martial", 1, emptyList())
        assertEquals(1, search.mangas.size)
        assertEquals("Biochemical Martial Arts", search.mangas.first().title)
        assertTrue(search.mangas.first().isNovel)

        // 4. Validate getDetails
        val details = source.getMangaDetails(novel)
        assertNotNull(details)
        assertEquals("Complete Martial Arts Attributes", details.title)
        assertEquals("Don't Enter The Jianghu", details.author)
        assertTrue(details.genres.contains("Action"))
        assertTrue(details.genres.contains("Fantasy"))
        assertTrue(details.description?.contains("Donghai was as hot as a furnace in June") == true)
        assertTrue(details.isNovel)

        // 5. Validate getChapterList
        val chapters = source.getChapterList(novel)
        assertEquals(2, chapters.size)
        assertEquals("https://wuxiaworld.site/novel/complete-martial-arts/chapter-1/", chapters[0].url)
        assertEquals("Chapter 1: This World Isn't Right!", chapters[0].name)
        assertEquals(1.0f, chapters[0].chapterNumber)
        assertEquals("https://wuxiaworld.site/novel/complete-martial-arts/chapter-2/", chapters[1].url)
        assertEquals("Chapter 2: The Second Day", chapters[1].name)
        assertEquals(2.0f, chapters[1].chapterNumber)

        // 6. Validate getChapterContent
        val content = source.getChapterContent(chapters[0])
        assertNotNull(content)
        assertEquals("https://wuxiaworld.site/novel/complete-martial-arts/chapter-1/", content.chapterUrl)
        assertTrue(content.htmlBody.contains("<p>Donghai was as hot as a furnace in June.</p>"))
        assertTrue(content.plainText.contains("Donghai was as hot as a furnace in June."))

        source.close()
    }
}
