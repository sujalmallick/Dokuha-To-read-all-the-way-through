package core.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppHttpClientTest {

    @Test
    fun configureAppHttpClientInstallsTimeoutRetryAndUserAgent() = runTest {
        var capturedUserAgent: String? = null
        val engine = MockEngine { request ->
            capturedUserAgent = request.headers[HttpHeaders.UserAgent]
            respond("ok", HttpStatusCode.OK)
        }
        val client = HttpClient(engine) {
            configureAppHttpClient()
        }

        assertNotNull(client.pluginOrNull(HttpTimeout), "HttpTimeout must be installed")
        assertNotNull(client.pluginOrNull(HttpRequestRetry), "HttpRequestRetry must be installed")

        val response = client.get("https://example.com/test")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(capturedUserAgent?.contains("Mozilla/5.0") == true)
        client.close()
    }

    @Test
    fun configureAppHttpClientRetriesOn5xxAndStopsAfterSuccess() = runTest {
        var attempts = 0
        val engine = MockEngine {
            attempts++
            if (attempts < 3) {
                respond("unavailable", HttpStatusCode.ServiceUnavailable)
            } else {
                respond("recovered", HttpStatusCode.OK)
            }
        }
        val client = HttpClient(engine) {
            configureAppHttpClient()
        }

        val response = client.get("https://example.com/flaky")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(3, attempts)
        client.close()
    }

    @Test
    fun configureAppHttpClientDoesNotRetryOn404() = runTest {
        var attempts = 0
        val engine = MockEngine {
            attempts++
            respond("missing", HttpStatusCode.NotFound)
        }
        val client = HttpClient(engine) {
            configureAppHttpClient()
        }

        val response = client.get("https://example.com/missing")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(1, attempts)
        client.close()
    }
}
