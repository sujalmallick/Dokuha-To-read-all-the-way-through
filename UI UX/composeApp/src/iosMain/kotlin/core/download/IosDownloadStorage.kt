package core.download

import core.source.SourcePage

/**
 * iOS DownloadStorage implementation stub / memory storage.
 * Per user instructions, iOS implementation is reported as unverified.
 */
class IosDownloadStorage : DownloadStorage {
    private val memoryPages = mutableMapOf<String, MutableList<SourcePage>>()
    private val memoryNovels = mutableMapOf<String, Pair<String, String>>()

    override fun savePageImage(chapterUrl: String, pageIndex: Int, data: ByteArray): String {
        val list = memoryPages.getOrPut(chapterUrl) { mutableListOf() }
        val fakeUrl = "file:///ios_documents/downloads/$chapterUrl/$pageIndex.jpg"
        list.add(SourcePage(index = pageIndex, url = fakeUrl, imageUrl = fakeUrl))
        return fakeUrl
    }

    override fun saveNovelContent(chapterUrl: String, title: String, content: String): String {
        memoryNovels[chapterUrl] = Pair(title, content)
        return "file:///ios_documents/downloads/$chapterUrl/novel.json"
    }

    override fun getDownloadedPages(chapterUrl: String): List<SourcePage>? {
        return memoryPages[chapterUrl]
    }

    override fun getDownloadedNovel(chapterUrl: String): Pair<String, String>? {
        return memoryNovels[chapterUrl]
    }

    override fun isChapterDownloaded(chapterUrl: String): Boolean {
        return memoryPages.containsKey(chapterUrl) || memoryNovels.containsKey(chapterUrl)
    }

    override fun deleteChapter(chapterUrl: String): Boolean {
        val hadPages = memoryPages.remove(chapterUrl) != null
        val hadNovel = memoryNovels.remove(chapterUrl) != null
        return hadPages || hadNovel
    }

    override fun getStorageBreakdown(): DownloadStorageBreakdown {
        return DownloadStorageBreakdown()
    }
}
