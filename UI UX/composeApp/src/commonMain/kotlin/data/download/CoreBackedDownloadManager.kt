package data.download

import core.download.DownloadManager as CoreDownloadManager
import core.download.DownloadStorage
import core.source.SourceChapter
import core.source.SourceManga
import core.source.StorageInfo
import core.storage.KeyValueStorage
import core.storage.StorageProvider
import data.mock.MockDownloadItem
import data.mock.PanelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class PersistedCompletedDownload(
    val id: String,
    val manga: SourceManga,
    val chapter: SourceChapter,
    val totalBytes: Long = 0L
)

/**
 * Production DownloadManager implementation backed by [CoreDownloadManager],
 * persistent [DownloadStorage], and platform [StorageProvider].
 * Contains zero simulation loops or hardcoded seed data.
 */
class CoreBackedDownloadManager(
    private val coreDownloadManager: CoreDownloadManager,
    private val downloadStorage: DownloadStorage,
    private val storageProvider: StorageProvider? = null,
    private val panelRepository: PanelRepository? = null,
    private val keyValueStorage: KeyValueStorage? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : DownloadManager {

    private val json = Json { ignoreUnknownKeys = true }
    private val enqueuedMetadata = mutableMapOf<String, Pair<SourceManga, SourceChapter>>()

    private val _completedDownloads = MutableStateFlow(loadPersistedCompleted())
    override val completedDownloads: StateFlow<List<DownloadJob>> = _completedDownloads.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageInfo())
    override val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    override val queue: StateFlow<List<DownloadJob>> = coreDownloadManager.queue
        .map { items ->
            items.map { item -> item.toDownloadJob() }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        refreshStorageInfo()
        scope.launch {
            var previousItems = emptyMap<String, core.download.DownloadJob>()
            coreDownloadManager.queue.collect { currentItems ->
                val currentById = currentItems.associateBy { it.id }
                previousItems.forEach { (id, prev) ->
                    if (!currentById.containsKey(id) && downloadStorage.isChapterDownloaded(prev.chapter.url)) {
                        val completedJob = prev.toDownloadJob().copy(
                            progress = 1f,
                            downloadedBytes = prev.totalBytes,
                            status = DownloadStatus.COMPLETED,
                            etaSeconds = 0
                        )
                        _completedDownloads.update { list ->
                            val next = if (list.any { it.id == completedJob.id }) list else listOf(completedJob) + list
                            persistCompleted(next)
                            next
                        }
                    }
                }
                previousItems = currentById
                refreshStorageInfo()
            }
        }
    }

    override fun enqueue(manga: SourceManga, chapters: List<SourceChapter>) {
        val sources = panelRepository?.getRegisteredSources().orEmpty()
        val matchingSource = sources.firstOrNull { it.id == manga.sourceId }
            ?: sources.firstOrNull { (it is core.source.NovelSource) == manga.isNovel }

        chapters.forEach { chapter ->
            val jobKey = jobId(manga.url, chapter.url)
            enqueuedMetadata[chapter.url] = manga to chapter
            if (!downloadStorage.isChapterDownloaded(chapter.url)) {
                coreDownloadManager.enqueueDownload(manga, chapter, matchingSource)
            } else {
                val alreadyDone = DownloadJob(
                    id = jobKey,
                    manga = manga,
                    chapter = chapter,
                    progress = 1f,
                    status = DownloadStatus.COMPLETED
                )
                _completedDownloads.update { list ->
                    val next = if (list.any { it.chapter.url == chapter.url }) list else listOf(alreadyDone) + list
                    persistCompleted(next)
                    next
                }
            }
        }
        refreshStorageInfo()
    }

    override fun pause(id: String) {
        val chapterUrl = resolveChapterUrl(id)
        val item = coreDownloadManager.queue.value.firstOrNull { it.id == chapterUrl }
        if (item != null && !item.isPaused) {
            coreDownloadManager.togglePause(chapterUrl)
        }
    }

    override fun resume(id: String) {
        val chapterUrl = resolveChapterUrl(id)
        val item = coreDownloadManager.queue.value.firstOrNull { it.id == chapterUrl }
        if (item != null && item.isPaused) {
            coreDownloadManager.togglePause(chapterUrl)
        }
    }

    override fun cancel(id: String) {
        coreDownloadManager.cancelDownload(resolveChapterUrl(id))
    }

    override fun pauseAll() {
        coreDownloadManager.queue.value.filter { !it.isPaused }.forEach {
            coreDownloadManager.togglePause(it.id)
        }
    }

    override fun resumeAll() {
        coreDownloadManager.queue.value.filter { it.isPaused }.forEach {
            coreDownloadManager.togglePause(it.id)
        }
    }

    override fun retry(id: String) {
        val chapterUrl = resolveChapterUrl(id)
        val meta = enqueuedMetadata[chapterUrl] ?: return
        enqueue(meta.first, listOf(meta.second))
    }

    override fun clearCompleted() {
        _completedDownloads.value = emptyList()
        persistCompleted(emptyList())
        refreshStorageInfo()
    }

    override fun deleteDownloadedChapter(mangaUrl: String, chapterUrl: String) {
        downloadStorage.deleteChapter(chapterUrl)
        _completedDownloads.update { list ->
            val next = list.filterNot { it.chapter.url == chapterUrl }
            persistCompleted(next)
            next
        }
        refreshStorageInfo()
    }

    override fun isChapterDownloaded(mangaUrl: String, chapterUrl: String): Boolean {
        return downloadStorage.isChapterDownloaded(chapterUrl)
    }

    override fun getChapterStatus(mangaUrl: String, chapterUrl: String): DownloadStatus? {
        val active = coreDownloadManager.queue.value.firstOrNull { it.id == chapterUrl }
        if (active != null) {
            return if (active.isPaused) DownloadStatus.PAUSED else DownloadStatus.DOWNLOADING
        }
        if (downloadStorage.isChapterDownloaded(chapterUrl)) {
            return DownloadStatus.COMPLETED
        }
        return null
    }

    fun refreshStorageInfo() {
        scope.launch {
            _storageInfo.value = computeStorageInfo()
        }
    }

    private fun loadPersistedCompleted(): List<DownloadJob> {
        val raw = keyValueStorage?.getString(KEY_COMPLETED_DOWNLOADS, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val parsed = runCatching { json.decodeFromString<List<PersistedCompletedDownload>>(raw) }
            .getOrDefault(emptyList())
        return parsed
            .filter { downloadStorage.isChapterDownloaded(it.chapter.url) }
            .map {
                DownloadJob(
                    id = it.id,
                    manga = it.manga,
                    chapter = it.chapter,
                    progress = 1f,
                    downloadedBytes = it.totalBytes,
                    totalBytes = it.totalBytes,
                    status = DownloadStatus.COMPLETED
                )
            }
    }

    private fun persistCompleted(jobs: List<DownloadJob>) {
        val store = keyValueStorage ?: return
        val serialized = jobs.map {
            PersistedCompletedDownload(
                id = it.id,
                manga = it.manga,
                chapter = it.chapter,
                totalBytes = it.totalBytes
            )
        }
        runCatching {
            store.putString(KEY_COMPLETED_DOWNLOADS, json.encodeToString(serialized))
        }
    }

    private fun computeStorageInfo(): StorageInfo {
        val stats = storageProvider?.getStorageStats()
        if (stats != null && stats.totalBytes > 0L) {
            val bytesPerGb = 1024f * 1024f * 1024f
            val bytesPerMb = 1024f * 1024f
            val totalUsedBytes = (stats.totalBytes - stats.freeBytes).coerceAtLeast(0L)
            val totalGb = stats.totalBytes / bytesPerGb
            val usedGb = totalUsedBytes / bytesPerGb
            val appMb = stats.appDownloadsBytes / bytesPerMb
            val otherUsedMb = ((totalUsedBytes - stats.appDownloadsBytes).coerceAtLeast(0L)) / bytesPerMb

            val completed = _completedDownloads.value
            val novelTrackedBytes = completed.filter { it.manga.isNovel }.sumOf { it.totalBytes.coerceAtLeast(0L) }
            val mangaTrackedBytes = completed.filter { !it.manga.isNovel }.sumOf { it.totalBytes.coerceAtLeast(0L) }
            val trackedTotal = novelTrackedBytes + mangaTrackedBytes

            val (mangaMb, novelsMb) = if (trackedTotal > 0L) {
                val mangaRatio = mangaTrackedBytes.toFloat() / trackedTotal.toFloat()
                (appMb * mangaRatio) to (appMb * (1f - mangaRatio))
            } else {
                appMb to 0f
            }

            return StorageInfo(
                totalGb = totalGb,
                usedGb = usedGb,
                mangaMb = mangaMb,
                manhwaMb = 0f,
                novelsMb = novelsMb,
                othersMb = otherUsedMb
            )
        }
        val breakdown = downloadStorage.getStorageBreakdown()
        return StorageInfo(
            totalGb = breakdown.totalGb,
            usedGb = breakdown.usedGb,
            mangaMb = breakdown.mangaMb,
            manhwaMb = breakdown.manhwaMb,
            novelsMb = breakdown.novelsMb,
            othersMb = breakdown.othersMb
        )
    }

    private fun core.download.DownloadJob.toDownloadJob(): DownloadJob {
        return DownloadJob(
            id = id,
            manga = manga,
            chapter = chapter,
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            status = DownloadStatus.valueOf(status.name),
            error = error
        )
    }

    private fun jobId(mangaUrl: String, chapterUrl: String): String = "$mangaUrl::$chapterUrl"

    private fun resolveChapterUrl(id: String): String {
        val separatorIdx = id.indexOf("::")
        return if (separatorIdx >= 0) id.substring(separatorIdx + 2) else id
    }

    private companion object {
        const val KEY_COMPLETED_DOWNLOADS = "completed_downloads_v1"
    }
}
