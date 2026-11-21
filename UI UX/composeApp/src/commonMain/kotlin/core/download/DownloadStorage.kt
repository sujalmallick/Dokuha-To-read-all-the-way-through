package core.download

import core.source.SourcePage

data class DownloadStorageBreakdown(
    val totalGb: Float = 64f,
    val usedGb: Float = 0f,
    val mangaMb: Float = 0f,
    val manhwaMb: Float = 0f,
    val novelsMb: Float = 0f,
    val othersMb: Float = 0f
)

interface DownloadStorage {
    fun savePageImage(chapterUrl: String, pageIndex: Int, data: ByteArray): String
    fun saveNovelContent(chapterUrl: String, title: String, content: String): String
    fun getDownloadedPages(chapterUrl: String): List<SourcePage>?
    fun getDownloadedNovel(chapterUrl: String): Pair<String, String>?
    fun isChapterDownloaded(chapterUrl: String): Boolean
    fun deleteChapter(chapterUrl: String): Boolean
    fun getStorageBreakdown(): DownloadStorageBreakdown
}
