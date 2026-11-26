package core.download

import core.source.MangasPage
import core.source.SourceFilter
import core.source.ComicSource
import core.source.NovelContent
import core.source.NovelSource
import core.source.SourceChapter
import core.source.SourceManga
import core.source.SourcePage
import data.mock.FakePanelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MemoryDownloadStorage : DownloadStorage {
    val pages = mutableMapOf<String, MutableList<SourcePage>>()
    val novels = mutableMapOf<String, Pair<String, String>>()

    override fun savePageImage(chapterUrl: String, pageIndex: Int, data: ByteArray): String {
        val list = pages.getOrPut(chapterUrl) { mutableListOf() }
        val fakeUrl = "file:///memory/$chapterUrl/$pageIndex.jpg"
        list.add(SourcePage(index = pageIndex, url = fakeUrl, imageUrl = fakeUrl))
        return fakeUrl
    }

    override fun saveNovelContent(chapterUrl: String, title: String, content: String): String {
        novels[chapterUrl] = Pair(title, content)
        return "file:///memory/$chapterUrl/novel.json"
    }

    override fun getDownloadedPages(chapterUrl: String): List<SourcePage>? = pages[chapterUrl]

    override fun getDownloadedNovel(chapterUrl: String): Pair<String, String>? = novels[chapterUrl]

    override fun isChapterDownloaded(chapterUrl: String): Boolean =
        pages.containsKey(chapterUrl) || novels.containsKey(chapterUrl)

    override fun deleteChapter(chapterUrl: String): Boolean {
        val p = pages.remove(chapterUrl) != null
        val n = novels.remove(chapterUrl) != null
        return p || n
    }

    override fun getStorageBreakdown(): DownloadStorageBreakdown {
        return DownloadStorageBreakdown(usedGb = 0.5f, mangaMb = 350f, novelsMb = 50f)
    }
}

class FakeComicSource : ComicSource {
    override val id: Long = 101L
    override val name: String = "Fake Comic"
    override val baseUrl: String = "https://fake.comic"
    override val lang: String = "en"
    override val isNsfw: Boolean = false

    override suspend fun getPopular(page: Int): MangasPage = MangasPage(emptyList(), false)
    override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)
    override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = MangasPage(emptyList(), false)
    override suspend fun getMangaDetails(manga: SourceManga): SourceManga = manga
    override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = emptyList()
    override suspend fun getPageList(chapter: SourceChapter): List<SourcePage> = listOf(
        SourcePage(index = 1, url = "https://fake.comic/p1.jpg"),
        SourcePage(index = 2, url = "https://fake.comic/p2.jpg"),
        SourcePage(index = 3, url = "https://fake.comic/p3.jpg")
    )
}

class FakeNovelSource : NovelSource {
    override val id: Long = 202L
    override val name: String = "Fake Novel"
    override val baseUrl: String = "https://fake.novel"
    override val lang: String = "en"
    override val isNsfw: Boolean = false

    override suspend fun getPopular(page: Int): MangasPage = MangasPage(emptyList(), false)
    override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)
    override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = MangasPage(emptyList(), false)
    override suspend fun getMangaDetails(manga: SourceManga): SourceManga = manga
    override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = emptyList()
    override suspend fun getChapterContent(chapter: SourceChapter): NovelContent = NovelContent(
        chapterUrl = chapter.url,
        title = chapter.name,
        htmlBody = "<p>This is the full text of chapter ${chapter.name}</p>",
        plainText = "This is the full text of chapter ${chapter.name}"
    )
}

class DownloadManagerTest {

    @Test
    fun testEnqueueAndCompleteComicDownload() = runBlocking {
        val storage = MemoryDownloadStorage()
        val manager = DownloadManager(downloadStorage = storage)
        val manga = SourceManga(url = "manga/test", title = "Test Manga")
        val chapter = SourceChapter(url = "chapter/1", name = "Chapter 1", chapterNumber = 1f)
        val source = FakeComicSource()

        assertFalse(manager.isChapterDownloaded(chapter.url))
        manager.enqueueDownload(manga, chapter, source)

        // Wait for coroutine worker to complete download
        var retries = 0
        while (!manager.isChapterDownloaded(chapter.url) && retries < 20) {
            kotlinx.coroutines.delay(100)
            retries++
        }

        assertTrue(manager.isChapterDownloaded(chapter.url))
        val downloadedPages = storage.getDownloadedPages(chapter.url)
        assertNotNull(downloadedPages)
        assertEquals(3, downloadedPages.size)
        assertEquals("file:///memory/chapter/1/1.jpg", downloadedPages[0].imageUrl)
    }

    @Test
    fun testEnqueueAndCompleteNovelDownload() = runBlocking {
        val storage = MemoryDownloadStorage()
        val manager = DownloadManager(downloadStorage = storage)
        val manga = SourceManga(url = "novel/test", title = "Test Novel", isNovel = true)
        val chapter = SourceChapter(url = "novel/test/ch1", name = "Chapter 1", chapterNumber = 1f)
        val source = FakeNovelSource()

        assertFalse(manager.isChapterDownloaded(chapter.url))
        manager.enqueueDownload(manga, chapter, source)

        var retries = 0
        while (!manager.isChapterDownloaded(chapter.url) && retries < 20) {
            kotlinx.coroutines.delay(100)
            retries++
        }

        assertTrue(manager.isChapterDownloaded(chapter.url))
        val novel = storage.getDownloadedNovel(chapter.url)
        assertNotNull(novel)
        assertEquals("Chapter 1", novel.first)
        assertTrue(novel.second.contains("full text of chapter Chapter 1"))
    }

    @Test
    fun testTogglePauseAndCancelDownload() = runBlocking {
        val storage = MemoryDownloadStorage()
        val manager = DownloadManager(downloadStorage = storage)
        val manga = SourceManga(url = "manga/paused", title = "Paused Manga")
        val chapter = SourceChapter(url = "chapter/paused/1", name = "Chapter Paused", chapterNumber = 1f)

        // Enqueue without a source so it stays in queue
        manager.enqueueDownload(manga, chapter, null)

        var queue = manager.queue.first()
        var retries = 0
        while (queue.isEmpty() && retries < 10) {
            kotlinx.coroutines.delay(50)
            queue = manager.queue.first()
            retries++
        }

        assertEquals(1, queue.size)
        assertEquals(DownloadStatus.QUEUED, queue[0].status)

        manager.togglePause(chapter.url)
        val pausedQueue = manager.queue.first()
        assertTrue(pausedQueue[0].isPaused)
        assertEquals(DownloadStatus.PAUSED, pausedQueue[0].status)

        manager.cancelDownload(chapter.url)
        val cancelledQueue = manager.queue.first()
        assertTrue(cancelledQueue.isEmpty())
    }

    @Test
    fun testBatchEnqueueAndStatusTransitions() = runBlocking {
        val storage = MemoryDownloadStorage()
        val manager = DownloadManager(downloadStorage = storage)
        val manga = SourceManga(url = "manga/batch", title = "Batch Manga")
        val ch1 = SourceChapter(url = "ch1", name = "Chapter 1", chapterNumber = 1f)
        val ch2 = SourceChapter(url = "ch2", name = "Chapter 2", chapterNumber = 2f)

        manager.enqueue(manga, listOf(ch1, ch2), FakeComicSource())

        var retries = 0
        while (!manager.isChapterDownloaded(ch1.url) && retries < 25) {
            kotlinx.coroutines.delay(100)
            retries++
        }

        assertTrue(manager.isChapterDownloaded(manga.url, ch1.url))
        assertEquals(DownloadStatus.COMPLETED, manager.getChapterStatus(manga.url, ch1.url))

        manager.deleteDownloadedChapter(manga.url, ch1.url)
        assertFalse(manager.isChapterDownloaded(manga.url, ch1.url))
    }

    private class ConfigurableComicSource(
        override val id: Long,
        override val name: String,
        override val baseUrl: String,
        private val pagesProvider: (SourceChapter) -> List<SourcePage>
    ) : ComicSource {
        var requestedChapters = mutableListOf<String>()
        override val lang: String = "en"
        override val isNsfw: Boolean = false
        override suspend fun getPopular(page: Int): MangasPage = MangasPage(emptyList(), false)
        override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)
        override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = MangasPage(emptyList(), false)
        override suspend fun getMangaDetails(manga: SourceManga): SourceManga = manga
        override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = emptyList()
        override suspend fun getPageList(chapter: SourceChapter): List<SourcePage> {
            requestedChapters.add(chapter.url)
            return pagesProvider(chapter)
        }
    }

    @Test
    fun testRelativeChapterUrlResolvesViaExplicitSourceId() = runBlocking {
        val storage = MemoryDownloadStorage()
        val repo = FakePanelRepository()
        val sourceA = ConfigurableComicSource(1001L, "Source A", "https://source-a.test") {
            listOf(SourcePage(index = 1, url = "https://source-a.test/1.jpg"))
        }
        val sourceB = ConfigurableComicSource(2002L, "Source B", "https://source-b.test") {
            listOf(
                SourcePage(index = 1, url = "https://source-b.test/1.jpg"),
                SourcePage(index = 2, url = "https://source-b.test/2.jpg")
            )
        }
        repo.registerSource(sourceA)
        repo.registerSource(sourceB)

        val manager = DownloadManager(downloadStorage = storage)
        manager.setSourceProvider { manga, chapter ->
            val sid = manga.sourceId.takeIf { it != 0L }?.toString()
            repo.resolveSource(sid, manga.url, chapter.url)
        }

        // Relative URLs that do NOT start with baseUrl, anchored by explicit sourceId = 2002L
        val manga = SourceManga(url = "/comics/orv-123", title = "ORV", sourceId = 2002L)
        val chapter = SourceChapter(url = "/comics/orv-123/chapter/317", name = "Ch. 317", chapterNumber = 317f)

        manager.enqueue(manga, listOf(chapter))

        var retries = 0
        while (!manager.isChapterDownloaded(chapter.url) && retries < 25) {
            kotlinx.coroutines.delay(100)
            retries++
        }

        assertTrue(manager.isChapterDownloaded(chapter.url))
        assertEquals(0, sourceA.requestedChapters.size, "Source A must not be contacted")
        assertEquals(listOf(chapter.url), sourceB.requestedChapters, "Source B must handle the download")
        assertEquals(2, storage.getDownloadedPages(chapter.url)?.size)
    }

    @Test
    fun testMultiSourceStreamingIsolation() = runBlocking {
        val repo = FakePanelRepository()
        val sourceA = ConfigurableComicSource(1001L, "Source A", "https://source-a.test") {
            listOf(SourcePage(index = 1, url = "https://source-a.test/p1.jpg"))
        }
        val sourceB = ConfigurableComicSource(2002L, "Source B", "https://source-b.test") {
            listOf(SourcePage(index = 1, url = "https://source-b.test/p1.jpg"))
        }
        repo.registerSource(sourceA)
        repo.registerSource(sourceB)

        val pages = repo.getPages("/comics/shared-slug/chapter/1", sourceId = "2002").first()
        assertEquals(1, pages.size)
        assertEquals("https://source-b.test/p1.jpg", pages[0].url)
        assertEquals(0, sourceA.requestedChapters.size)
    }

    @Test
    fun testUnresolvableSourceWithProviderMarksError() = runBlocking {
        val storage = MemoryDownloadStorage()
        val manager = DownloadManager(downloadStorage = storage)
        manager.setSourceProvider { _, _ -> null }

        val manga = SourceManga(url = "/comics/missing", title = "Missing", sourceId = 9999L)
        val chapter = SourceChapter(url = "/comics/missing/ch-1", name = "Ch. 1", chapterNumber = 1f)

        manager.enqueue(manga, listOf(chapter))

        var queue = manager.queue.first()
        var retries = 0
        while ((queue.isEmpty() || queue[0].status == DownloadStatus.QUEUED) && retries < 20) {
            kotlinx.coroutines.delay(50)
            queue = manager.queue.first()
            retries++
        }

        assertEquals(1, queue.size)
        assertEquals(DownloadStatus.ERROR, queue[0].status)
        assertEquals(9999L, queue[0].sourceId)
    }

    @Test
    fun testEmptyPageListMarksErrorInsteadOfCompleted() = runBlocking {
        val storage = MemoryDownloadStorage()
        val manager = DownloadManager(downloadStorage = storage)
        val emptySource = ConfigurableComicSource(3003L, "Empty", "https://empty.test") { emptyList() }
        val manga = SourceManga(url = "/comics/empty", title = "Empty", sourceId = 3003L)
        val chapter = SourceChapter(url = "/comics/empty/ch-1", name = "Ch. 1", chapterNumber = 1f)

        manager.enqueue(manga, listOf(chapter), emptySource)

        var queue = manager.queue.first()
        var retries = 0
        while ((queue.isEmpty() || queue[0].status != DownloadStatus.ERROR) && retries < 20) {
            kotlinx.coroutines.delay(50)
            queue = manager.queue.first()
            retries++
        }

        assertFalse(manager.isChapterDownloaded(chapter.url))
        assertEquals(DownloadStatus.ERROR, queue[0].status)
    }

    @Test
    fun testDualInterfaceComicSourceUsesComicPipeline() = runBlocking {
        val storage = MemoryDownloadStorage()
        val manager = DownloadManager(downloadStorage = storage)
        val dualSource = object : ComicSource, NovelSource {
            override val id: Long = 4004L
            override val name: String = "Dual Adapter"
            override val baseUrl: String = "https://dual.test"
            override val lang: String = "en"
            override val isNsfw: Boolean = false
            override suspend fun getPopular(page: Int): MangasPage = MangasPage(emptyList(), false)
            override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)
            override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = MangasPage(emptyList(), false)
            override suspend fun getMangaDetails(manga: SourceManga): SourceManga = manga
            override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = emptyList()
            override suspend fun getPageList(chapter: SourceChapter): List<SourcePage> = listOf(
                SourcePage(index = 0, url = "https://dual.test/0.jpg"),
                SourcePage(index = 1, url = "https://dual.test/1.jpg")
            )
            override suspend fun getChapterContent(chapter: SourceChapter): NovelContent {
                throw IllegalStateException("Comic download must not call getChapterContent")
            }
        }

        val manga = SourceManga(url = "/comics/dual", title = "Dual Comic", isNovel = false, sourceId = 4004L)
        val chapter = SourceChapter(url = "/comics/dual/ch-1", name = "Ch. 1", chapterNumber = 1f)

        manager.enqueue(manga, listOf(chapter), dualSource)

        var retries = 0
        while (!manager.isChapterDownloaded(chapter.url) && retries < 25) {
            kotlinx.coroutines.delay(100)
            retries++
        }

        assertTrue(manager.isChapterDownloaded(chapter.url))
        assertEquals(2, storage.getDownloadedPages(chapter.url)?.size)
    }
}
