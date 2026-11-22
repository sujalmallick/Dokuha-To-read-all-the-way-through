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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExtensionRepositoryTest {

    @Test
    fun testInitialExtensionsLoaded() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val list = repository.extensions.value
        assertTrue(list.isNotEmpty(), "Extensions list should not be empty")

        val mangadex = list.find { it.name == "MangaDex" }
        assertNotNull(mangadex)
        assertEquals(ExtensionStatus.INSTALLED, mangadex.status)
        assertEquals(ExtensionTrustLevel.OFFICIAL, mangadex.trustLevel)

        val comick = list.find { it.name == "Comick" }
        assertNotNull(comick)
        assertEquals(ExtensionStatus.UPDATE_AVAILABLE, comick.status)
        assertEquals("1.3.5", comick.updateVersion)
    }

    @Test
    fun testInstallLifecycle() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val webnovel = repository.extensions.value.find { it.name == "WebNovel" }
        assertNotNull(webnovel)
        assertEquals(ExtensionStatus.AVAILABLE, webnovel.status)

        repository.installExtension(webnovel.id)
        assertEquals(ExtensionStatus.INSTALLING, repository.extensions.value.find { it.id == webnovel.id }?.status)

        testScope.advanceUntilIdle()
        val installed = repository.extensions.value.find { it.id == webnovel.id }
        assertNotNull(installed)
        assertEquals(ExtensionStatus.INSTALLED, installed.status)
        assertEquals(1.0f, installed.installProgress)
    }

    @Test
    fun testUpdateLifecycle() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val comick = repository.extensions.value.find { it.name == "Comick" }
        assertNotNull(comick)
        assertEquals(ExtensionStatus.UPDATE_AVAILABLE, comick.status)
        assertEquals("1.3.1", comick.versionName)

        repository.updateExtension(comick.id)
        assertEquals(ExtensionStatus.INSTALLING, repository.extensions.value.find { it.id == comick.id }?.status)

        testScope.advanceUntilIdle()
        val updated = repository.extensions.value.find { it.id == comick.id }
        assertNotNull(updated)
        assertEquals(ExtensionStatus.INSTALLED, updated.status)
        assertEquals("1.3.5", updated.versionName)
        assertNull(updated.updateVersion)
    }

    @Test
    fun testUninstallLifecycle() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val mangadex = repository.extensions.value.find { it.name == "MangaDex" }
        assertNotNull(mangadex)
        assertEquals(ExtensionStatus.INSTALLED, mangadex.status)

        repository.uninstallExtension(mangadex.id)
        val uninstalled = repository.extensions.value.find { it.id == mangadex.id }
        assertNotNull(uninstalled)
        assertEquals(ExtensionStatus.AVAILABLE, uninstalled.status)
        assertEquals(0L, uninstalled.cachedBytes)
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
    fun testCannotRemoveOfficialRepository() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val officialRepo = repository.repositories.value.find { it.isOfficial }
        assertNotNull(officialRepo)

        val removed = repository.removeRepository(officialRepo.id)
        assertFalse(removed, "Official repository must never be removed")
        assertTrue(repository.repositories.value.any { it.id == officialRepo.id })
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
    fun testClearExtensionCache() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val mangadex = repository.extensions.value.find { it.name == "MangaDex" }
        assertNotNull(mangadex)
        assertTrue(mangadex.cachedBytes > 0)

        repository.clearExtensionCache(mangadex.id)
        val cleared = repository.extensions.value.find { it.id == mangadex.id }
        assertNotNull(cleared)
        assertEquals(0L, cleared.cachedBytes)
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
    fun testUpdateAllExtensions() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val repository = DefaultExtensionRepository(scope = testScope)

        val beforeUpdates = repository.extensions.value.filter { it.status == ExtensionStatus.UPDATE_AVAILABLE }
        assertTrue(beforeUpdates.isNotEmpty(), "Should have pending updates initially")

        repository.updateAllExtensions()
        testScope.advanceUntilIdle()

        val afterUpdates = repository.extensions.value.filter { it.status == ExtensionStatus.UPDATE_AVAILABLE }
        assertTrue(afterUpdates.isEmpty(), "All extensions should be updated")
    }
}
