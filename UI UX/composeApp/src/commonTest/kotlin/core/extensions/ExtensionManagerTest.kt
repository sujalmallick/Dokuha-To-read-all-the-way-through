package core.extensions

import core.extensions.js.ExtensionCatalogItem
import core.extensions.js.ExtensionType
import core.extensions.model.ExtensionEcosystem
import core.storage.InMemoryKeyValueStorage
import data.mock.FakePanelRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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

    @Test
    fun testEmptyStateWithNoPreSeededRepositoriesOrFallbacks() = runBlocking {
        val storage = InMemoryKeyValueStorage()
        val manager = ExtensionManager(storage = storage)

        // Verify zero pre-seeded repos
        assertEquals(0, manager.repoUrls.value.size)

        // Verify zero available extensions without repos
        assertEquals(0, manager.availableExtensions.value.size)

        // Refreshing catalog with 0 repos leaves available extensions empty
        manager.refreshCatalog()
        assertEquals(0, manager.availableExtensions.value.size)
    }

    @Test
    fun testRefreshCatalogMultiEcosystem() = runBlocking {
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("index.json") -> respond(
                    content = """
                        [
                          {
                            "id": 5001,
                            "name": "Mangayomi Source",
                            "baseUrl": "https://manga.example.com",
                            "lang": "en",
                            "version": "1.0.0",
                            "sourceCodeUrl": "https://repo.example.com/manga.js",
                            "sourceCodeLanguage": 1,
                            "itemType": 0
                          }
                        ]
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                path.endsWith("plugins.min.json") -> respond(
                    content = """
                        [
                          {
                            "id": "ln-source",
                            "name": "LN Source",
                            "site": "https://novel.example.com",
                            "lang": "en",
                            "version": "1.0.0",
                            "url": "https://repo.example.com/novel.js"
                          }
                        ]
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                path.endsWith("versioning.json") -> respond(
                    content = """
                        {
                          "sources": [
                            {
                              "id": "pb-source",
                              "name": "PB Source",
                              "version": "1.0.0",
                              "websiteBaseURL": "https://pb.example.com"
                            }
                          ]
                        }
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(content = "", status = HttpStatusCode.NotFound)
            }
        }

        val client = HttpClient(mockEngine)
        val storage = InMemoryKeyValueStorage()
        val manager = ExtensionManager(storage = storage, httpClient = client)

        manager.addRepository("https://repo.example.com")
        manager.refreshCatalog()

        val items = manager.availableExtensions.value
        assertEquals(3, items.size)
        assertTrue(items.any { it.ecosystem == ExtensionEcosystem.MANGAYOMI && it.name == "Mangayomi Source" })
        assertTrue(items.any { it.ecosystem == ExtensionEcosystem.LNREADER && it.name == "LN Source" })
        assertTrue(items.any { it.ecosystem == ExtensionEcosystem.PAPERBACK && it.name == "PB Source" })
    }

    @Test
    fun testInstallAndActivateMultiEcosystemSources() = runBlocking {
        val storage = InMemoryKeyValueStorage()
        val repo = FakePanelRepository()
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("manga.js") -> respond(
                    content = """
                        class DefaultExtension {
                            async getPopular(page) { return { list: [], hasNextPage: false }; }
                        }
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, "text/javascript")
                )
                path.endsWith("novel.js") -> respond(
                    content = """
                        module.exports = {
                            async popularNovels(page, options) { return []; }
                        };
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, "text/javascript")
                )
                path.endsWith("pb.js") -> respond(
                    content = """
                        const source = {
                            default: class {
                                async getDiscoverSectionItems(section, metadata) { return []; }
                            }
                        };
                        globalThis.source = source;
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, "text/javascript")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val client = HttpClient(mockEngine)
        val manager = ExtensionManager(storage = storage, panelRepository = repo, httpClient = client)

        // 1. Install Mangayomi
        val mItem = ExtensionCatalogItem(
            id = "5001",
            name = "Mangayomi Dynamic",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://manga.example.com",
            type = ExtensionType.COMIC,
            pkg = "https://repo.example.com/manga.js",
            ecosystem = ExtensionEcosystem.MANGAYOMI
        )
        val mSuccess = manager.installExtension(mItem, "https://repo.example.com")
        assertTrue(mSuccess, "Mangayomi install should succeed")
        assertNotNull(manager.getInstalledSource("5001"))

        // 2. Install LNReader
        val lnItem = ExtensionCatalogItem(
            id = "ln-test",
            name = "LN Dynamic",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://novel.example.com",
            type = ExtensionType.NOVEL,
            pkg = "https://repo.example.com/novel.js",
            ecosystem = ExtensionEcosystem.LNREADER
        )
        val lnSuccess = manager.installExtension(lnItem, "https://repo.example.com")
        assertTrue(lnSuccess, "LNReader install should succeed")
        assertNotNull(manager.getInstalledSource("ln-test"))

        // 3. Install Paperback
        val pbItem = ExtensionCatalogItem(
            id = "pb-test",
            name = "Paperback Dynamic",
            version = "1.0.0",
            lang = "en",
            baseUrl = "https://pb.example.com",
            type = ExtensionType.COMIC,
            pkg = "https://repo.example.com/pb.js",
            ecosystem = ExtensionEcosystem.PAPERBACK
        )
        val pbSuccess = manager.installExtension(pbItem, "https://repo.example.com")
        assertTrue(pbSuccess, "Paperback install should succeed")
        assertNotNull(manager.getInstalledSource("pb-test"))

        // Verify all 3 appear in PanelRepository
        val registered = repo.getRegisteredSources()
        assertEquals(3, registered.size)

        // Uninstall Paperback
        manager.uninstallExtension("pb-test")
        assertNull(manager.getInstalledSource("pb-test"))
        assertEquals(2, manager.getAllInstalledSources().size)
    }
}
