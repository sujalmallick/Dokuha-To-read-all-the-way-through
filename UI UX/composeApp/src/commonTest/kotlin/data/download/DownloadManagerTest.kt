package data.download

import core.source.SourceChapter
import core.source.SourceManga
import data.mock.MockStorageInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadManagerTest {

    private val testManga = SourceManga(
        url = "manga/test-title",
        title = "Test Manga",
        genres = listOf("Action")
    )

    private val testChapter1 = SourceChapter(
        url = "ch-1",
        name = "Chapter 1",
        chapterNumber = 1f
    )

    private val testChapter2 = SourceChapter(
        url = "ch-2",
        name = "Chapter 2",
        chapterNumber = 2f
    )

    private val testChapter3 = SourceChapter(
        url = "ch-3",
        name = "Chapter 3",
        chapterNumber = 3f
    )

    @Test
    fun testDefaultSeededQueueAndCompleted() {
        val manager = DefaultDownloadManager(autoStartSimulation = false)
        val queue = manager.queue.value
        val completed = manager.completedDownloads.value

        assertTrue(queue.isNotEmpty(), "Initial queue should be pre-populated with realistic jobs")
        assertTrue(completed.isNotEmpty(), "Initial completed downloads should be pre-populated")
        assertTrue(manager.isChapterDownloaded("manga/the-last-horizon", "ch-207"))
        assertEquals(DownloadStatus.COMPLETED, manager.getChapterStatus("manga/the-last-horizon", "ch-207"))
    }

    @Test
    fun testEnqueueSingleChapter() {
        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = emptyList(),
            initialCompleted = emptyList()
        )

        manager.enqueue(testManga, listOf(testChapter1))

        val queue = manager.queue.value
        assertEquals(1, queue.size)
        assertEquals("manga/test-title::ch-1", queue.first().id)
        assertEquals(DownloadStatus.QUEUED, queue.first().status)
        assertEquals(0f, queue.first().progress)
    }

    @Test
    fun testEnqueueBatchChaptersPreservesOrder() {
        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = emptyList(),
            initialCompleted = emptyList()
        )

        manager.enqueue(testManga, listOf(testChapter1, testChapter2, testChapter3))

        val queue = manager.queue.value
        assertEquals(3, queue.size)
        assertEquals("Chapter 1", queue[0].chapter.name)
        assertEquals("Chapter 2", queue[1].chapter.name)
        assertEquals("Chapter 3", queue[2].chapter.name)
    }

    @Test
    fun testDuplicateEnqueueIgnored() {
        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = emptyList(),
            initialCompleted = emptyList()
        )

        manager.enqueue(testManga, listOf(testChapter1))
        manager.enqueue(testManga, listOf(testChapter1, testChapter2))

        val queue = manager.queue.value
        assertEquals(2, queue.size, "Duplicate chapter 1 should not be enqueued twice")
    }

    @Test
    fun testPauseAndResumeJob() {
        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = listOf(
                DownloadJob(
                    id = "job-1",
                    manga = testManga,
                    chapter = testChapter1,
                    status = DownloadStatus.DOWNLOADING,
                    speedBytesPerSec = 2_000_000L
                )
            ),
            initialCompleted = emptyList()
        )

        manager.pause("job-1")
        val pausedJob = manager.queue.value.first()
        assertEquals(DownloadStatus.PAUSED, pausedJob.status)
        assertEquals(0L, pausedJob.speedBytesPerSec)

        manager.resume("job-1")
        val resumedJob = manager.queue.value.first()
        assertEquals(DownloadStatus.QUEUED, resumedJob.status)
    }

    @Test
    fun testCancelJobRemovesFromQueue() {
        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = listOf(
                DownloadJob(
                    id = "job-1",
                    manga = testManga,
                    chapter = testChapter1,
                    status = DownloadStatus.QUEUED
                ),
                DownloadJob(
                    id = "job-2",
                    manga = testManga,
                    chapter = testChapter2,
                    status = DownloadStatus.QUEUED
                )
            ),
            initialCompleted = emptyList()
        )

        manager.cancel("job-1")
        val queue = manager.queue.value
        assertEquals(1, queue.size)
        assertEquals("job-2", queue.first().id)
    }

    @Test
    fun testPauseAllAndResumeAll() {
        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = listOf(
                DownloadJob(id = "job-1", manga = testManga, chapter = testChapter1, status = DownloadStatus.DOWNLOADING),
                DownloadJob(id = "job-2", manga = testManga, chapter = testChapter2, status = DownloadStatus.QUEUED)
            ),
            initialCompleted = emptyList()
        )

        manager.pauseAll()
        assertTrue(manager.queue.value.all { it.status == DownloadStatus.PAUSED })

        manager.resumeAll()
        assertTrue(manager.queue.value.all { it.status == DownloadStatus.QUEUED })
    }

    @Test
    fun testClearCompleted() {
        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = emptyList(),
            initialCompleted = listOf(
                DownloadJob(id = "c-1", manga = testManga, chapter = testChapter1, status = DownloadStatus.COMPLETED)
            )
        )

        assertEquals(1, manager.completedDownloads.value.size)
        manager.clearCompleted()
        assertEquals(0, manager.completedDownloads.value.size)
    }

    @Test
    fun testDeleteDownloadedChapterUpdatesStorage() {
        val initialStorage = MockStorageInfo(
            totalGb = 64f,
            usedGb = 4.0f,
            mangaMb = 2000f
        )

        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = emptyList(),
            initialCompleted = listOf(
                DownloadJob(
                    id = "manga/test-title::ch-1",
                    manga = testManga,
                    chapter = testChapter1,
                    totalBytes = 50 * 1024 * 1024L, // 50 MB
                    status = DownloadStatus.COMPLETED
                )
            ),
            initialStorage = initialStorage
        )

        assertTrue(manager.isChapterDownloaded("manga/test-title", "ch-1"))
        manager.deleteDownloadedChapter("manga/test-title", "ch-1")

        assertFalse(manager.isChapterDownloaded("manga/test-title", "ch-1"))
        assertEquals(0, manager.completedDownloads.value.size)
        val updatedStorage = manager.storageInfo.value
        assertEquals(1950f, updatedStorage.mangaMb)
    }

    @Test
    fun testSimulationProgressTickCompletesAndMovesToCompleted() {
        val manager = DefaultDownloadManager(
            autoStartSimulation = false,
            initialQueue = listOf(
                DownloadJob(
                    id = "manga/test-title::ch-1",
                    manga = testManga,
                    chapter = testChapter1,
                    progress = 0.95f,
                    downloadedBytes = 23_000_000L,
                    totalBytes = 24_000_000L,
                    status = DownloadStatus.DOWNLOADING
                )
            ),
            initialCompleted = emptyList()
        )

        // One tick increments by 1.2 MB -> exceeds 24 MB -> completes
        manager.tickSimulation()

        assertTrue(manager.queue.value.isEmpty(), "Queue should be empty after job completes")
        assertEquals(1, manager.completedDownloads.value.size, "Job should be moved to completed")
        val completedJob = manager.completedDownloads.value.first()
        assertEquals(1.0f, completedJob.progress)
        assertEquals(DownloadStatus.COMPLETED, completedJob.status)
        assertTrue(manager.isChapterDownloaded("manga/test-title", "ch-1"))
    }
}
