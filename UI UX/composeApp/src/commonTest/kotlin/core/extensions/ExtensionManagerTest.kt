package core.extensions

import core.storage.InMemoryKeyValueStorage
import data.mock.FakePanelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtensionManagerTest {

    private val sampleBundle = """
        const manifest = {
            id: "persisted-manga",
            name: "Persisted Manga",
            version: "1.2.0",
            lang: "en",
            baseUrl: "https://persisted.example.com",
            type: "COMIC",
            isNsfw: false
        };

        const extension = {
            async getPopular(page) {
                return {
                    mangas: [
                        { url: "m1", title: "Persisted Title", status: "ONGOING" }
                    ],
                    hasNextPage: false
                };
            }
        };
    """.trimIndent()

    @Test
    fun testInstallPersistenceAndUninstall() = runBlocking {
        val storage = InMemoryKeyValueStorage()
        val repo = FakePanelRepository()
        val manager = ExtensionManager(storage = storage, panelRepository = repo)

        // Initially no extensions installed
        assertEquals(0, manager.installedExtensions.value.size)

        // 1. Install from single bundle
        val installed = manager.installExtensionFromBundle(sampleBundle)
        assertTrue(installed, "Extension installation should succeed")
        assertEquals(1, manager.installedExtensions.value.size)
        assertEquals("persisted-manga", manager.installedExtensions.value[0].id)

        // Verify registered into PanelRepository
        val sources = repo.getSources().first()
        assertTrue(sources.any { it.name == "Persisted Manga" }, "Source should appear in PanelRepository")

        // 2. Simulate app restart with a fresh ExtensionManager using the same storage
        val restartedManager = ExtensionManager(storage = storage, panelRepository = repo)
        assertEquals(1, restartedManager.installedExtensions.value.size)
        assertEquals("persisted-manga", restartedManager.installedExtensions.value[0].id)
        assertEquals("1.2.0", restartedManager.installedExtensions.value[0].version)

        // 3. Uninstall extension
        restartedManager.uninstallExtension("persisted-manga")
        assertEquals(0, restartedManager.installedExtensions.value.size)
        assertNull(restartedManager.getInstalledSource("persisted-manga"))

        // Verify storage is cleaned up
        val storedIds = storage.getString("dokuha_installed_ext_ids", "[]")
        assertFalse(storedIds.contains("persisted-manga"))
    }

    @Test
    fun testRepositoryUrlManagement() {
        val storage = InMemoryKeyValueStorage()
        val manager = ExtensionManager(storage = storage)

        // Validate and add repo
        val res = manager.addRepository("https://github.com/dokuha/dokuha-extensions")
        assertTrue(res.isValid)
        assertEquals("https://github.com/dokuha/dokuha-extensions", res.normalizedUrl)
        assertTrue(manager.repoUrls.value.contains("https://github.com/dokuha/dokuha-extensions"))

        // Reject invalid URL
        val invalidRes = manager.addRepository("http://insecure.example.com")
        assertFalse(invalidRes.isValid)
    }

    @Test
    fun testExtractManifestWithCommentsAndFormatting() = runBlocking {
        val storage = InMemoryKeyValueStorage()
        val manager = ExtensionManager(storage = storage)

        val bundleWithComments = """
            // Top-level extension configuration
            const manifest = {
                // Extension identifier
                id: "commented-ext",
                name: "Commented Source",
                version: "2.0.0",
                lang: "en",
                baseUrl: "https://comments.example.com",
                type: "COMIC",
                isNsfw: false
            };
            const extension = {};
        """.trimIndent()

        val manifest = manager.extractManifest(bundleWithComments)
        assertNotNull(manifest)
        assertEquals("commented-ext", manifest.id)
        assertEquals("Commented Source", manifest.name)
        assertEquals("2.0.0", manifest.version)
    }

    @Test
    fun testDynamicSourceCatalogueAndSearch() = runBlocking {
        val storage = InMemoryKeyValueStorage()
        val repo = FakePanelRepository()
        val manager = ExtensionManager(storage = storage, panelRepository = repo)

        val installed = manager.installExtensionFromBundle(sampleBundle)
        assertTrue(installed)

        val registeredSource = manager.getInstalledSource("persisted-manga")
        assertNotNull(registeredSource)

        // Query catalogue for dynamic source
        val catalogue = repo.getSourceCatalogue(registeredSource.id.toString(), page = 1).first()
        assertEquals(1, catalogue.size)
        assertEquals("Persisted Title", catalogue[0].title)

        // Search dynamic source
        val searchResults = repo.searchManga(query = "Persisted", sourceId = registeredSource.id.toString()).first()
        assertEquals(1, searchResults.size)
        assertEquals("Persisted Title", searchResults[0].title)
    }
}
