package data.download

import core.source.SourceChapter
import core.source.SourceManga
import core.source.StorageInfo
import data.mock.MockStorageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class DownloadJob(
    val id: String,
    val manga: SourceManga,
    val chapter: SourceChapter,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 24 * 1024 * 1024L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Int = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val error: String? = null
) {
    val isPaused: Boolean get() = status == DownloadStatus.PAUSED
    val isCompleted: Boolean get() = status == DownloadStatus.COMPLETED
    val isDownloading: Boolean get() = status == DownloadStatus.DOWNLOADING
}

interface DownloadManager {
    val queue: StateFlow<List<DownloadJob>>
    val completedDownloads: StateFlow<List<DownloadJob>>
    val storageInfo: StateFlow<MockStorageInfo>

    fun enqueue(manga: SourceManga, chapters: List<SourceChapter>)
    fun pause(id: String)
    fun resume(id: String)
    fun cancel(id: String)
    fun pauseAll()
    fun resumeAll()
    fun retry(id: String)
    fun clearCompleted()
    fun deleteDownloadedChapter(mangaUrl: String, chapterUrl: String)
    fun isChapterDownloaded(mangaUrl: String, chapterUrl: String): Boolean
    fun getChapterStatus(mangaUrl: String, chapterUrl: String): DownloadStatus?
}

class DefaultDownloadManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    autoStartSimulation: Boolean = true,
    initialQueue: List<DownloadJob>? = null,
    initialCompleted: List<DownloadJob>? = null,
    initialStorage: MockStorageInfo = MockStorageInfo()
) : DownloadManager {

    private val _queue = MutableStateFlow<List<DownloadJob>>(
        initialQueue ?: createDefaultSeedQueue()
    )
    override val queue: StateFlow<List<DownloadJob>> = _queue.asStateFlow()

    private val _completedDownloads = MutableStateFlow<List<DownloadJob>>(
        initialCompleted ?: createDefaultSeedCompleted()
    )
    override val completedDownloads: StateFlow<List<DownloadJob>> = _completedDownloads.asStateFlow()

    private val _storageInfo = MutableStateFlow(initialStorage)
    override val storageInfo: StateFlow<MockStorageInfo> = _storageInfo.asStateFlow()

    private var simulationJob: Job? = null

    init {
        if (autoStartSimulation) {
            startSimulationLoop()
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            while (isActive) {
                delay(500)
                tickSimulation()
            }
        }
    }

    /**
     * Advances download progress of active jobs by 1.2 MB per tick (~2.4 MB/s simulation speed).
     */
    fun tickSimulation() {
        var completedJob: DownloadJob? = null

        _queue.update { currentList ->
            if (currentList.isEmpty()) return@update currentList

            // If nothing is actively downloading, promote the first QUEUED job
            val hasDownloading = currentList.any { it.status == DownloadStatus.DOWNLOADING }
            val workingList = if (!hasDownloading) {
                val firstQueuedIndex = currentList.indexOfFirst { it.status == DownloadStatus.QUEUED }
                if (firstQueuedIndex >= 0) {
                    currentList.toMutableList().apply {
                        this[firstQueuedIndex] = this[firstQueuedIndex].copy(
                            status = DownloadStatus.DOWNLOADING,
                            speedBytesPerSec = 2_800_000L
                        )
                    }
                } else currentList
            } else currentList

            workingList.map { job ->
                if (job.status == DownloadStatus.DOWNLOADING) {
                    val increment = 1_200_000L
                    val newDownloaded = (job.downloadedBytes + increment).coerceAtMost(job.totalBytes)
                    val newProgress = (newDownloaded.toFloat() / job.totalBytes.toFloat()).coerceIn(0f, 1f)
                    val remainingBytes = (job.totalBytes - newDownloaded).coerceAtLeast(0L)
                    val speed = 2_800_000L
                    val eta = if (speed > 0) (remainingBytes / speed).toInt() else 0

                    if (newDownloaded >= job.totalBytes) {
                        val finished = job.copy(
                            progress = 1.0f,
                            downloadedBytes = job.totalBytes,
                            speedBytesPerSec = 0L,
                            etaSeconds = 0,
                            status = DownloadStatus.COMPLETED
                        )
                        completedJob = finished
                        finished
                    } else {
                        job.copy(
                            progress = newProgress,
                            downloadedBytes = newDownloaded,
                            speedBytesPerSec = speed,
                            etaSeconds = eta
                        )
                    }
                } else {
                    job
                }
            }
        }

        completedJob?.let { finished ->
            // Move finished job from queue to completed
            _queue.update { list -> list.filterNot { it.id == finished.id } }
            _completedDownloads.update { list ->
                if (list.none { it.id == finished.id }) listOf(finished) + list else list
            }
            // Update storage metrics
            val addedMb = finished.totalBytes / (1024f * 1024f)
            _storageInfo.update { info ->
                val newMangaMb = info.mangaMb + addedMb
                val newUsedGb = ((info.mangaMb + info.manhwaMb + info.novelsMb + info.othersMb + addedMb) / 1024f)
                info.copy(
                    mangaMb = newMangaMb,
                    usedGb = (newUsedGb * 10f).roundToInt() / 10f
                )
            }
        }
    }

    override fun enqueue(manga: SourceManga, chapters: List<SourceChapter>) {
        val newJobs = chapters.mapNotNull { chapter ->
            val jobId = "${manga.url}::${chapter.url}"
            val alreadyQueued = _queue.value.any { it.id == jobId }
            val alreadyCompleted = _completedDownloads.value.any { it.id == jobId }
            if (alreadyQueued || alreadyCompleted) null
            else {
                DownloadJob(
                    id = jobId,
                    manga = manga,
                    chapter = chapter,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = 24 * 1024 * 1024L,
                    status = DownloadStatus.QUEUED
                )
            }
        }

        if (newJobs.isNotEmpty()) {
            _queue.update { it + newJobs }
        }
    }

    override fun pause(id: String) {
        _queue.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L) else it }
        }
    }

    override fun resume(id: String) {
        _queue.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.QUEUED) else it }
        }
    }

    override fun cancel(id: String) {
        _queue.update { list -> list.filterNot { it.id == id } }
    }

    override fun pauseAll() {
        _queue.update { list ->
            list.map {
                if (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED) {
                    it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L)
                } else it
            }
        }
    }

    override fun resumeAll() {
        _queue.update { list ->
            list.map {
                if (it.status == DownloadStatus.PAUSED) {
                    it.copy(status = DownloadStatus.QUEUED)
                } else it
            }
        }
    }

    override fun retry(id: String) {
        _queue.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = DownloadStatus.QUEUED,
                        progress = 0f,
                        downloadedBytes = 0L,
                        error = null
                    )
                } else it
            }
        }
    }

    override fun clearCompleted() {
        _completedDownloads.value = emptyList()
    }

    override fun deleteDownloadedChapter(mangaUrl: String, chapterUrl: String) {
        val jobId = "$mangaUrl::$chapterUrl"
        val item = _completedDownloads.value.find { it.id == jobId }
        if (item != null) {
            _completedDownloads.update { list -> list.filterNot { it.id == jobId } }
            val removedMb = item.totalBytes / (1024f * 1024f)
            _storageInfo.update { info ->
                val newMangaMb = (info.mangaMb - removedMb).coerceAtLeast(0f)
                val newUsedGb = ((newMangaMb + info.manhwaMb + info.novelsMb + info.othersMb) / 1024f).coerceAtLeast(0.1f)
                info.copy(
                    mangaMb = newMangaMb,
                    usedGb = (newUsedGb * 10f).roundToInt() / 10f
                )
            }
        }
    }

    override fun isChapterDownloaded(mangaUrl: String, chapterUrl: String): Boolean {
        val jobId = "$mangaUrl::$chapterUrl"
        return _completedDownloads.value.any { it.id == jobId }
    }

    override fun getChapterStatus(mangaUrl: String, chapterUrl: String): DownloadStatus? {
        val jobId = "$mangaUrl::$chapterUrl"
        val inQueue = _queue.value.find { it.id == jobId }
        if (inQueue != null) return inQueue.status
        val inCompleted = _completedDownloads.value.find { it.id == jobId }
        if (inCompleted != null) return DownloadStatus.COMPLETED
        return null
    }

    companion object {
        fun createDefaultSeedQueue(): List<DownloadJob> {
            val lastHorizon = SourceManga(
                url = "manga/the-last-horizon",
                title = "The Last Horizon",
                genres = listOf("Action", "Fantasy")
            )
            val stellarOdyssey = SourceManga(
                url = "manga/stellar-odyssey",
                title = "Stellar Odyssey",
                genres = listOf("Sci-Fi")
            )

            return listOf(
                DownloadJob(
                    id = "${lastHorizon.url}::ch-209",
                    manga = lastHorizon,
                    chapter = SourceChapter(url = "ch-209", name = "Ch. 209 - The Final Stand", chapterNumber = 209f),
                    progress = 0.68f,
                    downloadedBytes = 18_400_000L,
                    totalBytes = 27_000_000L,
                    speedBytesPerSec = 2_800_000L,
                    etaSeconds = 45,
                    status = DownloadStatus.DOWNLOADING
                ),
                DownloadJob(
                    id = "${lastHorizon.url}::ch-208",
                    manga = lastHorizon,
                    chapter = SourceChapter(url = "ch-208", name = "Ch. 208 - Echoes of Abyss", chapterNumber = 208f),
                    progress = 0.12f,
                    downloadedBytes = 3_200_000L,
                    totalBytes = 26_500_000L,
                    speedBytesPerSec = 0L,
                    etaSeconds = 210,
                    status = DownloadStatus.QUEUED
                ),
                DownloadJob(
                    id = "${stellarOdyssey.url}::ch-44",
                    manga = stellarOdyssey,
                    chapter = SourceChapter(url = "ch-44", name = "Ch. 44 - Singularity Gate", chapterNumber = 44f),
                    progress = 0.0f,
                    downloadedBytes = 0L,
                    totalBytes = 31_000_000L,
                    speedBytesPerSec = 0L,
                    etaSeconds = 340,
                    status = DownloadStatus.PAUSED
                )
            )
        }

        fun createDefaultSeedCompleted(): List<DownloadJob> {
            val lastHorizon = SourceManga(
                url = "manga/the-last-horizon",
                title = "The Last Horizon",
                genres = listOf("Action", "Fantasy")
            )
            val towerOfGod = SourceManga(
                url = "manga/tower-of-god",
                title = "Tower of God",
                genres = listOf("Action", "Mystery")
            )

            return listOf(
                DownloadJob(
                    id = "${lastHorizon.url}::ch-207",
                    manga = lastHorizon,
                    chapter = SourceChapter(url = "ch-207", name = "Ch. 207 - The Golden Age", chapterNumber = 207f),
                    progress = 1.0f,
                    downloadedBytes = 25_400_000L,
                    totalBytes = 25_400_000L,
                    status = DownloadStatus.COMPLETED
                ),
                DownloadJob(
                    id = "${lastHorizon.url}::ch-206",
                    manga = lastHorizon,
                    chapter = SourceChapter(url = "ch-206", name = "Ch. 206 - Ancient Awakening", chapterNumber = 206f),
                    progress = 1.0f,
                    downloadedBytes = 24_100_000L,
                    totalBytes = 24_100_000L,
                    status = DownloadStatus.COMPLETED
                ),
                DownloadJob(
                    id = "${towerOfGod.url}::ch-550",
                    manga = towerOfGod,
                    chapter = SourceChapter(url = "ch-550", name = "Ch. 550 - Arrival", chapterNumber = 550f),
                    progress = 1.0f,
                    downloadedBytes = 32_000_000L,
                    totalBytes = 32_000_000L,
                    status = DownloadStatus.COMPLETED
                )
            )
        }
    }
}
