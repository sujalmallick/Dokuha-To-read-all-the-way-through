package core.download

import core.source.MangasPage
import core.source.SourceFilter
import core.source.ComicSource
import core.source.NovelContent
import core.source.NovelSource
import core.source.SourceChapter
import core.source.SourceManga
import core.source.SourcePage
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
        assertFalse(queue[0].isPaused)

        manager.togglePause(chapter.url)
        val pausedQueue = manager.queue.first()
        assertTrue(pausedQueue[0].isPaused)

        manager.cancelDownload(chapter.url)
        val cancelledQueue = manager.queue.first()
        assertTrue(cancelledQueue.isEmpty())
    }
}
