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
import kotlin.test.assertFalse
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

    @Test
    fun testHeadersPolyfill() = runBlocking {
        val bridge = JsBridge()
        val engine = JsEngine()
        bridge.registerFunctions(engine)
        engine.evaluate(bridge.getPreambleScript())

        val res = engine.evaluate("""
            (() => {
                const h = new Headers({ "Content-Type": "application/json", "Authorization": "Bearer token123" });
                h.set("user-agent", "CustomUA");
                h.append("accept", "text/html");
                h.append("accept", "application/xhtml+xml");
                
                return JSON.stringify({
                    hasCT: h.has("content-type"),
                    getCT: h.get("CONTENT-TYPE"),
                    getUA: h.get("User-Agent"),
                    getAccept: h.get("accept"),
                    hasMissing: h.has("non-existent")
                });
            })()
        """.trimIndent())

        assertTrue(res.contains("\"hasCT\":true"))
        assertTrue(res.contains("\"getCT\":\"application/json\""))
        assertTrue(res.contains("\"getUA\":\"CustomUA\""))
        assertTrue(res.contains("\"getAccept\":\"text/html, application/xhtml+xml\""))
        assertTrue(res.contains("\"hasMissing\":false"))

        engine.close()
    }

    @Test
    fun testDocumentAndElementDomEmulation() = runBlocking {
        val bridge = JsBridge()
        val engine = JsEngine()
        bridge.registerFunctions(engine, sourceId = 42L)
        engine.evaluate(bridge.getPreambleScript(sourceId = 42L))

        val html = """
            <html>
                <body>
                    <div id="content" class="chapter-list">
                        <a href="/manga/one-piece/ch-1" class="ch-link" data-src="https://cdn.example.com/cover.jpg">Chapter 1</a>
                        <a href="/manga/one-piece/ch-2" class="ch-link">Chapter 2</a>
                    </div>
                </body>
            </html>
        """.trimIndent()

        engine.setGlobalVariable("testHtml", "\"${html.replace("\n", "").replace("\"", "\\\"")}\"")
        val result = engine.evaluate("""
            (() => {
                const doc = new Document(testHtml);
                const first = doc.selectFirst('a.ch-link');
                const all = doc.select('a.ch-link');
                const byId = doc.getElementById('content');
                
                return JSON.stringify({
                    firstText: first ? first.text : "",
                    firstHref: first ? first.getHref : "",
                    firstSrc: first ? first.getSrc : "",
                    allCount: all.length,
                    hasId: byId !== null
                });
            })()
        """.trimIndent())

        assertTrue(result.contains("\"firstText\":\"Chapter 1\""))
        assertTrue(result.contains("\"firstHref\":\"/manga/one-piece/ch-1\""))
        assertTrue(result.contains("\"firstSrc\":\"https://cdn.example.com/cover.jpg\""))
        assertTrue(result.contains("\"allCount\":2"))
        assertTrue(result.contains("\"hasId\":true"))

        engine.close()
    }

    @Test
    fun testStringPrototypeHelpers() = runBlocking {
        val bridge = JsBridge()
        val engine = JsEngine()
        engine.evaluate(bridge.getPreambleScript())

        val res = engine.evaluate("""
            (() => {
                const s = "manga/action/solo-leveling/chapter-10";
                return JSON.stringify({
                    after: s.substringAfter("action/"),
                    before: s.substringBefore("/chapter"),
                    afterLast: s.substringAfterLast("/"),
                    between: s.substringBetween("manga/", "/solo")
                });
            })()
        """.trimIndent())

        assertTrue(res.contains("\"after\":\"solo-leveling/chapter-10\""))
        assertTrue(res.contains("\"before\":\"manga/action/solo-leveling\""))
        assertTrue(res.contains("\"afterLast\":\"chapter-10\""))
        assertTrue(res.contains("\"between\":\"action\""))

        engine.close()
    }

    @Test
    fun testCommonJsModuleRegistryAndRequire() = runBlocking {
        val bridge = JsBridge()
        val engine = JsEngine()
        bridge.registerFunctions(engine, sourceId = 101L)
        engine.evaluate(bridge.getPreambleScript(sourceId = 101L))

        val res = engine.evaluate("""
            (() => {
                const urlencode = require('urlencode');
                const cheerio = require('cheerio');
                const novelStatus = require('@libs/novelStatus');
                
                const encoded = urlencode.encode("solo leveling");
                const $ = cheerio.load("<div class='item'>Title</div>");
                
                return JSON.stringify({
                    encoded: encoded,
                    text: $('.item').text(),
                    status: novelStatus.NovelStatus.Ongoing
                });
            })()
        """.trimIndent())

        assertTrue(res.contains("\"encoded\":\"solo%20leveling\""))
        assertTrue(res.contains("\"text\":\"Title\""))
        assertTrue(res.contains("\"status\":\"Ongoing\""))

        engine.close()
    }

    @Test
    fun testSandboxedSharedPreferencesAndStateManagers() = runBlocking {
        val bridge = JsBridge()
        val engine = JsEngine()
        bridge.registerFunctions(engine, sourceId = 555L)
        engine.evaluate(bridge.getPreambleScript(sourceId = 555L))

        val res = engine.evaluate("""
            (async () => {
                const sp = new SharedPreferences();
                sp.setString("theme", "liquid-glass");
                const themeVal = sp.getString("theme", "default");
                
                const sm = App.createSourceStateManager();
                await sm.store("user_token", { token: "abc123xyz" });
                const tokenObj = await sm.retrieve("user_token");
                
                Application.setState({ readingMode: "webtoon" }, "settings");
                const appState = Application.getState("settings");
                
                return JSON.stringify({
                    theme: themeVal,
                    token: tokenObj ? tokenObj.token : null,
                    readingMode: appState ? appState.readingMode : null
                });
            })()
        """.trimIndent())

        assertTrue(res.contains("\"theme\":\"liquid-glass\""))
        assertTrue(res.contains("\"token\":\"abc123xyz\""))
        assertTrue(res.contains("\"readingMode\":\"webtoon\""))

        engine.close()
    }

    @Test
    fun testRfc6265DotBoundedCookieIsolation() {
        val bridge = JsBridge()

        // Same domain matches
        assertTrue(bridge.isDomainMatch("asuracomic.net", "asuracomic.net"))
        assertTrue(bridge.isDomainMatch(".asuracomic.net", "asuracomic.net"))

        // Subdomain matches parent domain
        assertTrue(bridge.isDomainMatch("asuracomic.net", "api.asuracomic.net"))
        assertTrue(bridge.isDomainMatch(".asuracomic.net", "m.asuracomic.net"))

        // Lookalike or spoofed domains must NOT match (RFC 6265 dot-bounded isolation)
        assertFalse(bridge.isDomainMatch("asuracomic.net", "evil-asuracomic.net"))
        assertFalse(bridge.isDomainMatch("mangadex.org", "fakemangadex.org"))
        assertFalse(bridge.isDomainMatch("kodjodev.com", "notkodjodev.com"))
    }

    @Test
    fun testCloudflareTurnstileMarkerDetection() {
        val bridge = JsBridge()

        assertTrue(bridge.isCloudflareChallenge("<div class=\"cf-turnstile\"></div>"))
        assertTrue(bridge.isCloudflareChallenge("<script src=\"https://challenges.cloudflare.com/turnstile/v0/api.js\"></script>"))
        assertTrue(bridge.isCloudflareChallenge("<div id=\"cf-chl-widget-container\"></div>"))
        assertTrue(bridge.isCloudflareChallenge("<html><title>Just a moment...</title></html>"))
        assertTrue(bridge.isCloudflareChallenge("Ray ID: 7abc123 Cloudflare"))

        // Normal HTML must not trigger challenge
        assertFalse(bridge.isCloudflareChallenge("<html><head><title>Chapter 10 - Asura Scans</title></head><body><h1>Content</h1></body></html>"))
    }

    @Test
    fun testAsyncFunctionAndEvaluateSuspension() = runBlocking {
        val engine = JsEngine()
        engine.setGlobalAsyncFunction("mockFetch") {
            kotlinx.coroutines.delay(50)
            """{"status":"success","data":"fetched_content"}"""
        }

        // Test evaluate with async IIFE
        val evalResult = engine.evaluate("""
            (async () => {
                const res = await mockFetch();
                return res;
            })()
        """.trimIndent())
        assertTrue(evalResult.contains("fetched_content"), "evaluate with async IIFE should await host async function: $evalResult")

        // Test callFunction with async method on object
        engine.evaluate("""
            globalThis.testExtension = {
                fetchPage: async function(page) {
                    const raw = await mockFetch();
                    const parsed = JSON.parse(raw);
                    return {
                        page: page,
                        list: [{ title: 'Item on page ' + page, content: parsed.data }]
                    };
                }
            };
        """.trimIndent())

        val callResult = engine.callFunction("testExtension.fetchPage", 3)
        assertTrue(callResult.contains("\"page\":3"), "callFunction should pass argument correctly: $callResult")
        assertTrue(callResult.contains("fetched_content"), "callFunction should return resolved async data: $callResult")

        engine.close()
    }

    @Test
    fun testApexDomainAndVerificationUrlNormalization() {
        val bridge = JsBridge()

        assertEquals("asurascans.com", bridge.extractApexDomain("asurascans.com"))
        assertEquals("asurascans.com", bridge.extractApexDomain("api.asurascans.com"))
        assertEquals("asurascans.com", bridge.extractApexDomain("www.api.asurascans.com"))
        assertEquals("mangadex.org", bridge.extractApexDomain("api.mangadex.org"))
        assertEquals("site.co.uk", bridge.extractApexDomain("api.site.co.uk"))

        assertEquals("https://asurascans.com/", bridge.getHumanVerificationUrl("https://api.asurascans.com/api/series?offset=0&limit=20"))
        assertEquals("https://asurascans.com/comics/my-title", bridge.getHumanVerificationUrl("https://asurascans.com/comics/my-title", "GET"))
        assertEquals("https://asurascans.com/", bridge.getHumanVerificationUrl("https://asurascans.com/comics/my-title", "POST"))
        assertEquals("https://site.co.uk/", bridge.getHumanVerificationUrl("https://api.site.co.uk/data"))
    }

    @Test
    fun testIsCloudflareChallengeResponse() {
        val bridge = JsBridge()

        // 403 with Cloudflare server header
        val cfHeaderResp = JsHttpResponse(
            status = 403,
            ok = false,
            headers = mapOf("Server" to "cloudflare", "cf-ray" to "12345"),
            body = "error"
        )
        assertTrue(bridge.isCloudflareChallengeResponse(cfHeaderResp))

        // 403 with Turnstile HTML
        val cfHtmlResp = JsHttpResponse(
            status = 403,
            ok = false,
            headers = emptyMap(),
            body = "<html><title>Just a moment...</title></html>"
        )
        assertTrue(bridge.isCloudflareChallengeResponse(cfHtmlResp))

        // Generic 403 JSON error without Cloudflare markers - should NOT trigger challenge
        val json403 = JsHttpResponse(
            status = 403,
            ok = false,
            headers = mapOf("Server" to "nginx"),
            body = "{\"error\": \"Unauthorized\", \"message\": \"Token expired\"}"
        )
        assertFalse(bridge.isCloudflareChallengeResponse(json403))

        // Non-challenge status codes must never trigger challenge response
        val notFound404 = JsHttpResponse(status = 404, ok = false, headers = emptyMap(), body = "Not Found")
        assertFalse(bridge.isCloudflareChallengeResponse(notFound404))

        val unauthorized401 = JsHttpResponse(status = 401, ok = false, headers = emptyMap(), body = "Unauthorized")
        assertFalse(bridge.isCloudflareChallengeResponse(unauthorized401))

        val rateLimit429 = JsHttpResponse(status = 429, ok = false, headers = emptyMap(), body = "Too Many Requests")
        assertFalse(bridge.isCloudflareChallengeResponse(rateLimit429))

        val serverError500 = JsHttpResponse(status = 500, ok = false, headers = emptyMap(), body = "Internal Server Error")
        assertFalse(bridge.isCloudflareChallengeResponse(serverError500))
    }

    @Test
    fun testDomainCooldownPreventsRepeatedResolverInvocations() = runBlocking {
        var resolverCallCount = 0
        val resolver = AntiBotResolver { url, ua ->
            resolverCallCount++
            AntiBotResolutionResult(success = false, errorMessage = "Failed to solve")
        }

        val mockEngine = MockEngine { _ ->
            respond(
                content = "<html><title>Just a moment...</title></html>",
                status = HttpStatusCode.Forbidden,
                headers = headersOf("Server", "cloudflare")
            )
        }
        val client = HttpClient(mockEngine)
        val bridge = JsBridge(httpClient = client, antiBotResolver = resolver)

        // First call triggers resolver
        bridge.fetch("https://protected.example.com/api/data")
        assertEquals(1, resolverCallCount)

        // Domain should now be in cooldown
        assertTrue(bridge.isDomainInCooldown("protected.example.com"))
        assertTrue(bridge.isDomainInCooldown("example.com"))

        // Second call to same domain during cooldown must NOT invoke resolver
        val secondResp = bridge.fetch("https://protected.example.com/api/other")
        assertEquals(1, resolverCallCount, "Resolver should NOT be invoked when domain is in cooldown")
        assertEquals(403, secondResp.status)
    }
}
