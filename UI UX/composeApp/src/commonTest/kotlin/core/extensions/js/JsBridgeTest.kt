package core.extensions.js

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsBridgeTest {

    @Test
    fun testParseHtmlSelectors() = runBlocking {
        val bridge = JsBridge()
        val engine = JsEngine()
        engine.evaluate(bridge.getPreambleScript())

        val html = """
            <html>
                <body>
                    <div id="main" class="container dark">
                        <h1 class="title">Solo Leveling</h1>
                        <ul class="chapters">
                            <li><a href="/ch/1" data-id="101">Chapter 1</a></li>
                            <li><a href="/ch/2" data-id="102">Chapter 2</a></li>
                        </ul>
                    </div>
                </body>
            </html>
        """.trimIndent()

        engine.setGlobalVariable("sampleHtml", "\"${html.replace("\n", "").replace("\"", "\\\"")}\"")
        val title = engine.evaluate("""
            (() => {
                const $ = dokuha.parseHtml(sampleHtml);
                return $('h1.title').text();
            })()
        """.trimIndent())
        assertEquals("Solo Leveling", title)

        val firstHref = engine.evaluate("""
            (() => {
                const $ = dokuha.parseHtml(sampleHtml);
                return $('ul.chapters a').attr('href');
            })()
        """.trimIndent())
        assertEquals("/ch/1", firstHref)

        val chapterCount = engine.evaluate("""
            (() => {
                const $ = dokuha.parseHtml(sampleHtml);
                let count = 0;
                $('ul.chapters li').each(() => { count++; });
                return count;
            })()
        """.trimIndent())
        assertEquals("2", chapterCount)

        engine.close()
    }

    @Test
    fun testEncodingAndCrypto() = runBlocking {
        val bridge = JsBridge()
        val engine = JsEngine()
        engine.setGlobalFunction("__dokuha_btoa") { args -> bridge.btoa(args[0].toString()) }
        engine.setGlobalFunction("__dokuha_atob") { args -> bridge.atob(args[0].toString()) }
        engine.setGlobalFunction("__dokuha_sha256") { args -> bridge.sha256(args[0].toString()) }
        engine.evaluate(bridge.getPreambleScript())

        val encoded = engine.evaluate("btoa('DokuhaReader')")
        assertEquals("RG9rdWhhUmVhZGVy", encoded)

        val decoded = engine.evaluate("atob('$encoded')")
        assertEquals("DokuhaReader", decoded)

        val hash = engine.evaluate("crypto.sha256('hello')")
        assertEquals(bridge.sha256("hello").uppercase(), hash.uppercase())

        engine.close()
    }

    @Test
    fun testConsoleLogging() = runBlocking {
        val bridge = JsBridge()
        val engine = JsEngine()
        engine.setGlobalFunction("__dokuha_log") { args ->
            bridge.log(args[0].toString(), args[1].toString())
            null
        }
        engine.evaluate(bridge.getPreambleScript())

        engine.evaluate("console.log('Testing console capture', 42);")
        assertTrue(bridge.diagnosticLogs.any { it.contains("Testing console capture 42") })

        engine.close()
    }

    @Test
    fun testMockFetchAndAntiBotSuspension() = runBlocking {
        var attempts = 0
        var resolverInvoked = false

        val mockEngine = MockEngine { request ->
            attempts++
            if (attempts == 1) {
                // First attempt: simulate Cloudflare 403 challenge
                respond(
                    content = "<html><title>Just a moment...</title><body>Checking your browser</body></html>",
                    status = HttpStatusCode.Forbidden,
                    headers = headersOf(HttpHeaders.ContentType, "text/html")
                )
            } else {
                // Second attempt after captcha solved
                respond(
                    content = """{"success":true,"message":"Welcome to target site"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val client = HttpClient(mockEngine)
        val resolver = AntiBotResolver { url, _ ->
            resolverInvoked = true
            AntiBotResolutionResult(
                success = true,
                cookies = mapOf("cf_clearance" to "verified_token_123")
            )
        }

        val bridge = JsBridge(httpClient = client, antiBotResolver = resolver)
        val response = bridge.fetch("https://protected.example.com/api/data")

        assertTrue(resolverInvoked, "AntiBotResolver should have been invoked on 403")
        assertEquals(200, response.status)
        assertTrue(response.ok)
        assertTrue(response.body.contains("Welcome to target site"))
        assertEquals(2, attempts, "Request should have been retried after solving challenge")
    }
}
