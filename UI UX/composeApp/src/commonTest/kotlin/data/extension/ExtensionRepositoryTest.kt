package data.extension

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExtensionRepositoryTest {

    @Test
    fun testInitialStateIsEmpty() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        // Strict empty state: zero preloaded mock extensions or preloaded repos
        assertTrue(repository.extensions.value.isEmpty(), "Extensions list should initially be empty")
        assertTrue(repository.repositories.value.isEmpty(), "Repositories list should initially be empty")
    }

    @Test
    fun testAddRepositoryValidHttps() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val result = repository.addRepository("https://example.com/tachiyomi/repo/index.json", "Example Repo")
        assertTrue(result.isSuccess)

        val repo = result.getOrNull()
        assertNotNull(repo)
        assertEquals("Example Repo", repo.name)
        assertEquals("https://example.com/tachiyomi/repo", repo.url)
        assertTrue(repo.fingerprint.contains(":"), "Fingerprint should be formatted hex pairs separated by colons")
        assertFalse(repo.isOfficial)

        val allRepos = repository.repositories.value
        assertTrue(allRepos.any { it.id == repo.id })
    }

    @Test
    fun testAddRepositoryInsecureUrlRejected() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        // Rejects HTTP
        val httpResult = repository.addRepository("http://insecure-repo.com/repo")
        assertTrue(httpResult.isFailure)

        // Rejects localhost
        val localResult = repository.addRepository("https://localhost/repo")
        assertTrue(localResult.isFailure)

        // Rejects user credentials in URL
        val credResult = repository.addRepository("https://admin:pass@example.com/repo")
        assertTrue(credResult.isFailure)
    }

    @Test
    fun testAddAndRemoveCustomRepository() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val result = repository.addRepository("https://custom-repo.org/manga", "Custom Manga Hub")
        assertTrue(result.isSuccess)
        val customRepo = result.getOrThrow()

        val removed = repository.removeRepository(customRepo.id)
        assertTrue(removed)
        assertFalse(repository.repositories.value.any { it.id == customRepo.id })
    }

    @Test
    fun testSideloadValidExtensionScript() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val script = """
            manifest = {
                "id": "sample-reader",
                "name": "Sample Reader",
                "version": "2.0.1",
                "lang": "en",
                "baseUrl": "https://sample.reader.com",
                "type": "comic",
                "isNsfw": false
            };
            class SampleSource {
                // Extension code
            }
        """.trimIndent()

        val result = repository.sideloadExtension(script)
        assertTrue(result.isSuccess, "Sideloading valid script should succeed: ${result.exceptionOrNull()?.message}")

        val item = result.getOrThrow()
        assertEquals("sample-reader", item.id)
        assertEquals("Sample Reader", item.name)
        assertEquals("2.0.1", item.versionName)
        assertEquals(ExtensionTrustLevel.UNTRUSTED, item.trustLevel)
        assertEquals(ExtensionStatus.INSTALLED, item.status)

        val inList = repository.extensions.value.find { it.id == "sample-reader" }
        assertNotNull(inList)
        assertEquals("Sample Reader", inList.name)
    }

    @Test
    fun testSideloadMalformedScriptFails() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val badScript = "console.log('hello without manifest');"
        val result = repository.sideloadExtension(badScript)
        assertTrue(result.isFailure, "Missing manifest must fail sideloading")
    }

    @Test
    fun testSideloadEmptyScriptFails() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val result = repository.sideloadExtension("   ")
        assertTrue(result.isFailure, "Empty script must fail sideloading")
    }

    @Test
    fun testFetchAndSideloadSSRFProtection() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        // Block HTTP
        val httpResult = repository.fetchAndSideloadExtension("http://evil.com/ext.js")
        assertTrue(httpResult.isFailure)

        // Block localhost
        val localResult = repository.fetchAndSideloadExtension("https://localhost:8080/ext.js")
        assertTrue(localResult.isFailure)

        // Block private IP
        val ipResult = repository.fetchAndSideloadExtension("https://192.168.1.1/ext.js")
        assertTrue(ipResult.isFailure)
    }

    @Test
    fun testSideloadedExtensionCacheAndUninstallLifecycle() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val script = """
            manifest = {
                "id": "cache-test-reader",
                "name": "Cache Test Reader",
                "version": "1.0.0",
                "lang": "en",
                "baseUrl": "https://cache.test.com",
                "type": "comic",
                "isNsfw": false
            };
        """.trimIndent()

        val sideloadResult = repository.sideloadExtension(script)
        assertTrue(sideloadResult.isSuccess)
        val ext = sideloadResult.getOrThrow()

        // Clear cache
        repository.clearExtensionCache(ext.id)
        val cleared = repository.extensions.value.find { it.id == ext.id }
        assertNotNull(cleared)
        assertEquals(0L, cleared.cachedBytes)

        // Uninstall
        repository.uninstallExtension(ext.id)
        val uninstalled = repository.extensions.value.find { it.id == ext.id }
        assertNotNull(uninstalled)
        assertEquals(ExtensionStatus.AVAILABLE, uninstalled.status)
    }
}
