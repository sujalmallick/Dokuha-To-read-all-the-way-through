package core.extensions.js

import core.extensions.SampleExtensions
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
            script = SampleExtensions.ASURA_SCANS_BUNDLE
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
            script = SampleExtensions.ASURA_SCANS_BUNDLE
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
