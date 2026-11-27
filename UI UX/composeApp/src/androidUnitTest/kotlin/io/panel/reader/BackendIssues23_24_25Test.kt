package io.panel.reader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import core.download.DownloadManager as CoreDownloadManager
import core.download.MemoryDownloadStorage
import core.extensions.ExtensionManager
import core.source.ComicSource
import core.source.MangasPage
import core.source.SourceChapter
import core.source.SourceFilter
import core.source.SourceManga
import core.source.SourcePage
import core.source.StorageInfo
import core.storage.InMemoryKeyValueStorage
import core.storage.StorageProvider
import data.download.CoreBackedDownloadManager
import data.repository.SqlDelightPanelRepository
import io.panel.reader.db.PanelDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class BackendIssues23_24_25Test {

    private val dbName = "test_backend_23_24_25.db"

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
    }

    private fun createDatabase(): PanelDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val driver = AndroidSqliteDriver(PanelDatabase.Schema, context, dbName)
        return PanelDatabase(driver)
    }

    @Test
    fun issue25_storageInfoDefaultsAreZero_andStorageProviderDrivesRealStats() = runBlocking {
        val defaults = StorageInfo()
        assertEquals(0f, defaults.totalGb)
        assertEquals(0f, defaults.usedGb)
        assertEquals(0f, defaults.mangaMb)
        assertEquals(0f, defaults.manhwaMb)
        assertEquals(0f, defaults.novelsMb)
        assertEquals(0f, defaults.othersMb)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val storageProvider = StorageProvider(context)
        val coreDownloadManager = CoreDownloadManager(
            downloadStorage = MemoryDownloadStorage(),
            storageProvider = storageProvider
        )
        val backedManager = CoreBackedDownloadManager(
            coreDownloadManager = coreDownloadManager,
            downloadStorage = MemoryDownloadStorage(),
            storageProvider = storageProvider
        )

        withTimeout(3_000) {
            while (backedManager.storageInfo.value.totalGb == 0f) {
                delay(20)
            }
        }
        val stats = backedManager.storageInfo.value
        assertTrue(stats.totalGb > 0f, "Expected real totalGb > 0 from StorageProvider, got ${stats.totalGb}")
        assertTrue(stats.usedGb >= 0f, "Expected non-negative usedGb, got ${stats.usedGb}")
        assertTrue(stats.othersMb >= 0f, "Expected non-negative othersMb, got ${stats.othersMb}")
    }

    @Test
    fun issue24_getTrending_neverFallsBackToLocalLibrary_andReactsToDynamicSources() = runBlocking {
        val db = createDatabase()
        db.mangaQueries.insertOrUpdateManga(
            id = null,
            sourceId = 1L,
            url = "local/library-manga",
            title = "Local Library Manga",
            artist = null,
            author = "Author",
            description = "Desc",
            genres = "Action",
            status = "ONGOING",
            thumbnailUrl = null,
            favorite = 1L,
            lastUpdate = 0L,
            nextUpdate = 0L,
            initialized = 1L,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            dateAdded = 0L,
            isNovel = 0L
        )

        val repo = SqlDelightPanelRepository(database = db)
        try {
            // 1. No dynamic sources -> must emit emptyList(), NOT local library manga
            assertEquals(emptyList(), repo.getTrending().first())

            // 2. Failing dynamic source -> must emit emptyList(), NOT local library manga
            val failingSource = object : ComicSource {
                override val id: Long = 1001L
                override val name: String = "FailingSource"
                override val baseUrl: String = "https://failing.example.com"
                override val lang: String = "en"
                override val isNsfw: Boolean = false
                override suspend fun getPopular(page: Int): MangasPage = throw RuntimeException("network error")
                override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)
                override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = MangasPage(emptyList(), false)
                override suspend fun getMangaDetails(manga: SourceManga): SourceManga = manga
                override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = emptyList()
                override suspend fun getPageList(chapter: SourceChapter): List<SourcePage> = emptyList()
            }
            repo.registerSource(failingSource)
            assertEquals(emptyList(), repo.getTrending().first())

            // 3. Reactive update when a working source is registered while collecting
            val emitted = mutableListOf<List<SourceManga>>()
            val collectorJob = launch {
                repo.getTrending().collect { emitted.add(it) }
            }
            delay(50)
            val workingSource = object : ComicSource {
                override val id: Long = 1002L
                override val name: String = "WorkingSource"
                override val baseUrl: String = "https://working.example.com"
                override val lang: String = "en"
                override val isNsfw: Boolean = false
                override suspend fun getPopular(page: Int): MangasPage = MangasPage(
                    mangas = listOf(SourceManga(url = "remote/popular-1", title = "Remote Popular 1", sourceId = 0L)),
                    hasNextPage = false
                )
                override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)
                override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = MangasPage(emptyList(), false)
                override suspend fun getMangaDetails(manga: SourceManga): SourceManga = manga
                override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = emptyList()
                override suspend fun getPageList(chapter: SourceChapter): List<SourcePage> = emptyList()
            }
            repo.registerSource(workingSource)
            withTimeout(3_000) {
                while (emitted.lastOrNull()?.isEmpty() != false) {
                    delay(20)
                }
            }
            val latest = emitted.last()
            assertEquals(1, latest.size)
            assertEquals("remote/popular-1", latest[0].url)
            assertEquals(1002L, latest[0].sourceId)
            collectorJob.cancel()
        } finally {
            repo.close()
        }
    }

    @Test
    fun issue23_getExtensions_emitsLiveInstalledExtensionsFromExtensionManager() = runBlocking {
        val db = createDatabase()
        val repo = SqlDelightPanelRepository(database = db)
        val storage = InMemoryKeyValueStorage()
        val extensionManager = ExtensionManager(
            storage = storage,
            panelRepository = repo,
            autoSeedDefaults = false,
            deferInit = false
        )
        repo.bindExtensionFlows(
            installed = extensionManager.installedExtensions,
            available = extensionManager.availableExtensions
        )

        val bundle = """
            const manifest = {
                id: "test-ext",
                name: "Test Extension",
                version: "1.0.0",
                lang: "en",
                baseUrl: "https://example.com",
                type: "COMIC",
                isNsfw: false
            };
            const extension = {
                async getPopular(page) {
                    return { mangas: [], hasNextPage: false };
                }
            };
        """.trimIndent()

        try {
            assertEquals(emptyList(), repo.getExtensions().first())

            assertTrue(extensionManager.installExtensionFromBundle(bundle))

            val installed = repo.getExtensions().first()
            assertEquals(1, installed.size)
            assertEquals("test-ext", installed[0].id)
            assertEquals("Test Extension", installed[0].name)
            assertEquals("1.0.0", installed[0].version)
            assertTrue(installed[0].isInstalled)

            extensionManager.uninstallExtension("test-ext")
            assertEquals(emptyList(), repo.getExtensions().first())
        } finally {
            repo.close()
        }
    }
}
