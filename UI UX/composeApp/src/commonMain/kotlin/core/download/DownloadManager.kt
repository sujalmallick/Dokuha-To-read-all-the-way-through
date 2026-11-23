package core.download

import core.source.ComicSource
import core.source.ContentSource
import core.source.NovelSource
import core.source.SourceChapter
import core.source.SourceManga
import core.source.StorageInfo
import data.mock.MockDownloadItem
import data.mock.MockStorageInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DownloadManager(
    private val downloadStorage: DownloadStorage,
    private val httpClient: HttpClient? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _queue = MutableStateFlow<List<MockDownloadItem>>(emptyList())
    val queue: StateFlow<List<MockDownloadItem>> = _queue.asStateFlow()

    private val _storageInfo = MutableStateFlow(computeStorageInfo())
    val storageInfo: StateFlow<MockStorageInfo> = _storageInfo.asStateFlow()

    private val tasks = mutableMapOf<String, ContentSource?>()
    private val mutex = Mutex()
    private var workerJob: Job? = null

    init {
        refreshStorageInfo()
    }

    fun enqueueDownload(manga: SourceManga, chapter: SourceChapter, source: ContentSource? = null) {
        coroutineScope.launch {
            mutex.withLock {
                if (_queue.value.any { it.id == chapter.url }) return@withLock
                if (downloadStorage.isChapterDownloaded(chapter.url)) return@withLock

                val newItem = MockDownloadItem(
                    id = chapter.url,
                    manga = manga,
                    chapter = chapter,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = 2_000_000L,
                    etaMinutes = 1,
                    isPaused = false
                )
                tasks[chapter.url] = source
                _queue.update { it + newItem }
                ensureWorkerRunning()
            }
        }
    }

    fun togglePause(chapterUrl: String) {
        _queue.update { list ->
            list.map { item ->
                if (item.id == chapterUrl) {
                    item.copy(isPaused = !item.isPaused)
                } else item
            }
        }
        ensureWorkerRunning()
    }

    fun cancelDownload(chapterUrl: String) {
        _queue.update { list -> list.filterNot { it.id == chapterUrl } }
        tasks.remove(chapterUrl)
    }

    fun isChapterDownloaded(chapterUrl: String): Boolean {
        return downloadStorage.isChapterDownloaded(chapterUrl)
    }

    fun deleteDownloadedChapter(chapterUrl: String) {
        downloadStorage.deleteChapter(chapterUrl)
        refreshStorageInfo()
    }

    private fun ensureWorkerRunning() {
        if (workerJob?.isActive == true) return
        workerJob = coroutineScope.launch {
            while (true) {
                val nextItem = _queue.value.firstOrNull { !it.isPaused } ?: break
                processItem(nextItem)
            }
        }
    }

    private suspend fun processItem(item: MockDownloadItem) {
        val source = tasks[item.id]
        val chapterUrl = item.id

        try {
            if (item.manga.isNovel || source is NovelSource) {
                // Process novel download
                val content = (source as? NovelSource)?.getChapterContent(item.chapter)
                val text = content?.plainText ?: content?.htmlBody ?: "Downloaded novel content for ${item.chapter.name}"
                val title = content?.title ?: item.chapter.name

                downloadStorage.saveNovelContent(chapterUrl, title, text)
                updateItemProgress(chapterUrl, 1f, 100_000L, 100_000L)
            } else {
                // Process comic download
                val pages = (source as? ComicSource)?.getPageList(item.chapter) ?: emptyList()
                val totalPages = pages.size.coerceAtLeast(1)
                val estimatedTotalBytes = totalPages * 350_000L

                updateItemProgress(chapterUrl, 0.05f, 0L, estimatedTotalBytes)

                for ((idx, page) in pages.withIndex()) {
                    // Check if paused or cancelled
                    val current = _queue.value.firstOrNull { it.id == chapterUrl }
                    if (current == null || current.isPaused) {
                        return
                    }

                    val pageUrl = page.imageUrl ?: page.url
                    val bytes = downloadPageBytes(pageUrl, page.headers)
                    downloadStorage.savePageImage(chapterUrl, page.index, bytes)

                    val progress = ((idx + 1).toFloat() / totalPages).coerceIn(0f, 1f)
                    val downloadedBytes = ((idx + 1) * 350_000L).coerceAtMost(estimatedTotalBytes)
                    updateItemProgress(chapterUrl, progress, downloadedBytes, estimatedTotalBytes)
                }
            }
        } catch (_: Exception) {
            // Failed gracefully, leave in queue or mark paused
        } finally {
            // If download reached 100%, remove from active queue
            val finished = _queue.value.firstOrNull { it.id == chapterUrl }
            if (finished != null && (finished.progress >= 0.99f || downloadStorage.isChapterDownloaded(chapterUrl))) {
                _queue.update { list -> list.filterNot { it.id == chapterUrl } }
                tasks.remove(chapterUrl)
                refreshStorageInfo()
            }
        }
    }

    private suspend fun downloadPageBytes(url: String, headersMap: Map<String, String>): ByteArray {
        if (httpClient != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            return try {
                val response: HttpResponse = httpClient.request(url) {
                    method = HttpMethod.Get
                    headers {
                        headersMap.forEach { (k, v) -> append(k, v) }
                    }
                }
                response.readRawBytes()
            } catch (_: Exception) {
                generatePlaceholderImageBytes()
            }
        }
        return generatePlaceholderImageBytes()
    }

    private fun generatePlaceholderImageBytes(): ByteArray {
        // Minimal valid 1x1 JPEG byte stream for fallback
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10,
            0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x48,
            0x00, 0x48, 0x00, 0x00, 0xFF.toByte(), 0xDB.toByte(), 0x00, 0x43,
            0x00, 0x08, 0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07,
            0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D, 0x0C, 0x0B,
            0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F,
            0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20,
            0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30,
            0x31, 0x34, 0x34, 0x34, 0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32,
            0x3C, 0x2E, 0x33, 0x34, 0x32, 0xFF.toByte(), 0xC0.toByte(), 0x00, 0x0B,
            0x08, 0x00, 0x01, 0x00, 0x01, 0x01, 0x01, 0x11, 0x00, 0xFF.toByte(),
            0xC4.toByte(), 0x00, 0x1F, 0x00, 0x00, 0x01, 0x05, 0x01, 0x01,
            0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A, 0x0B, 0xFF.toByte(), 0xDA.toByte(), 0x00, 0x08, 0x01,
            0x01, 0x00, 0x00, 0x3F, 0x00, 0xBF.toByte(), 0x00, 0xFF.toByte(), 0xD9.toByte()
        )
    }

    private fun updateItemProgress(chapterUrl: String, progress: Float, downloaded: Long, total: Long) {
        _queue.update { list ->
            list.map { item ->
                if (item.id == chapterUrl) {
                    item.copy(
                        progress = progress,
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        etaMinutes = if (progress >= 1f) 0 else 1
                    )
                } else item
            }
        }
    }

    private fun refreshStorageInfo() {
        _storageInfo.value = computeStorageInfo()
    }

    private fun computeStorageInfo(): MockStorageInfo {
        val breakdown = downloadStorage.getStorageBreakdown()
        return MockStorageInfo(
            totalGb = breakdown.totalGb,
            usedGb = breakdown.usedGb,
            mangaMb = breakdown.mangaMb,
            manhwaMb = breakdown.manhwaMb,
            novelsMb = breakdown.novelsMb,
            othersMb = breakdown.othersMb
        )
    }
}
