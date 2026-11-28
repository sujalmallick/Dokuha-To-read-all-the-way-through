package core.download

import core.source.SourcePage

internal fun detectImageExtension(data: ByteArray): String {
    if (data.size >= 4 &&
        data[0] == 0x89.toByte() &&
        data[1] == 0x50.toByte() &&
        data[2] == 0x4E.toByte() &&
        data[3] == 0x47.toByte()
    ) {
        return "png"
    }
    if (data.size >= 4 &&
        data[0] == 0x47.toByte() &&
        data[1] == 0x49.toByte() &&
        data[2] == 0x46.toByte() &&
        data[3] == 0x38.toByte()
    ) {
        return "gif"
    }
    if (data.size >= 12 &&
        data[0] == 0x52.toByte() && // R
        data[1] == 0x49.toByte() && // I
        data[2] == 0x46.toByte() && // F
        data[3] == 0x46.toByte() && // F
        data[8] == 0x57.toByte() && // W
        data[9] == 0x45.toByte() && // E
        data[10] == 0x42.toByte() && // B
        data[11] == 0x50.toByte()   // P
    ) {
        return "webp"
    }
    return "jpg"
}

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

class MemoryDownloadStorage : DownloadStorage {
    private val pages = mutableMapOf<String, MutableList<SourcePage>>()
    private val novels = mutableMapOf<String, Pair<String, String>>()

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
        return DownloadStorageBreakdown(totalGb = 64f, usedGb = 0.5f, mangaMb = 350f, novelsMb = 50f)
    }
}
