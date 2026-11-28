package core.download

import core.source.SourcePage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@Serializable
private data class IosDownloadManifest(
    val chapterUrl: String,
    val pageCount: Int,
    val fileNames: List<String>
)

@Serializable
private data class IosNovelSavedData(
    val title: String,
    val content: String
)

/**
 * Persistent iOS DownloadStorage backed by Okio FileSystem.SYSTEM inside the app's
 * NSDocumentDirectory/downloads directory.
 */
@OptIn(ExperimentalForeignApi::class)
class IosDownloadStorage : DownloadStorage {
    private val fileSystem = FileSystem.SYSTEM
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val root: Path by lazy {
        val docsUrl = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL
        val basePath = docsUrl?.path ?: "/tmp"
        val downloadsPath = "$basePath/downloads".toPath()
        fileSystem.createDirectories(downloadsPath)
        downloadsPath
    }

    private fun getChapterDir(chapterUrl: String): Path {
        val safeName = chapterUrl.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
        val hash = (chapterUrl.hashCode() and 0x7FFFFFFF).toString(16)
        return root / "${safeName}_$hash"
    }

    override fun savePageImage(chapterUrl: String, pageIndex: Int, data: ByteArray): String {
        require(data.isNotEmpty()) { "Cannot save empty page image for $chapterUrl page $pageIndex" }
        val dir = getChapterDir(chapterUrl)
        fileSystem.createDirectories(dir)
        val ext = detectImageExtension(data)
        val fileName = "$pageIndex.$ext"
        val filePath = dir / fileName
        fileSystem.write(filePath) {
            write(data)
        }

        val manifestPath = dir / "manifest.json"
        val currentManifest = if (fileSystem.exists(manifestPath)) {
            runCatching {
                val raw = fileSystem.read(manifestPath) { readUtf8() }
                json.decodeFromString<IosDownloadManifest>(raw)
            }.getOrNull()
        } else null

        val files = (currentManifest?.fileNames?.toMutableSet() ?: mutableSetOf()).apply { add(fileName) }
        val manifest = IosDownloadManifest(
            chapterUrl = chapterUrl,
            pageCount = files.size,
            fileNames = files.sortedBy { it.substringBefore('.').toIntOrNull() ?: 0 }
        )
        fileSystem.write(manifestPath) {
            writeUtf8(json.encodeToString(manifest))
        }

        return "file://$filePath"
    }

    override fun saveNovelContent(chapterUrl: String, title: String, content: String): String {
        val dir = getChapterDir(chapterUrl)
        fileSystem.createDirectories(dir)
        val novelPath = dir / "novel.json"
        val data = IosNovelSavedData(title = title, content = content)
        fileSystem.write(novelPath) {
            writeUtf8(json.encodeToString(data))
        }
        return "file://$novelPath"
    }

    override fun getDownloadedPages(chapterUrl: String): List<SourcePage>? {
        val dir = getChapterDir(chapterUrl)
        val manifestPath = dir / "manifest.json"
        if (!fileSystem.exists(manifestPath)) return null
        return runCatching {
            val raw = fileSystem.read(manifestPath) { readUtf8() }
            val manifest = json.decodeFromString<IosDownloadManifest>(raw)
            val validPages = manifest.fileNames.mapNotNull { fileName ->
                val filePath = dir / fileName
                val size = fileSystem.metadataOrNull(filePath)?.size ?: 0L
                if (!fileSystem.exists(filePath) || size <= 0L) return@mapNotNull null
                val uri = "file://$filePath"
                SourcePage(
                    index = 0,
                    url = uri,
                    imageUrl = uri
                )
            }.mapIndexed { index, page ->
                page.copy(index = index + 1)
            }
            validPages.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    override fun getDownloadedNovel(chapterUrl: String): Pair<String, String>? {
        val dir = getChapterDir(chapterUrl)
        val novelPath = dir / "novel.json"
        if (!fileSystem.exists(novelPath)) return null
        return runCatching {
            val raw = fileSystem.read(novelPath) { readUtf8() }
            val data = json.decodeFromString<IosNovelSavedData>(raw)
            Pair(data.title, data.content)
        }.getOrNull()
    }

    override fun isChapterDownloaded(chapterUrl: String): Boolean {
        val dir = getChapterDir(chapterUrl)
        if (!fileSystem.exists(dir)) return false
        return fileSystem.exists(dir / "manifest.json") || fileSystem.exists(dir / "novel.json")
    }

    override fun deleteChapter(chapterUrl: String): Boolean {
        val dir = getChapterDir(chapterUrl)
        if (!fileSystem.exists(dir)) return false
        fileSystem.deleteRecursively(dir)
        return true
    }

    override fun getStorageBreakdown(): DownloadStorageBreakdown {
        var totalBytes = 0L
        if (fileSystem.exists(root)) {
            fileSystem.listRecursively(root).forEach { path ->
                val meta = fileSystem.metadataOrNull(path)
                if (meta?.isRegularFile == true) {
                    totalBytes += meta.size ?: 0L
                }
            }
        }
        val usedMb = totalBytes / (1024f * 1024f)
        val usedGb = totalBytes / (1024f * 1024f * 1024f)
        return DownloadStorageBreakdown(
            totalGb = 64f,
            usedGb = usedGb,
            mangaMb = usedMb * 0.8f,
            manhwaMb = 0f,
            novelsMb = usedMb * 0.2f,
            othersMb = 0f
        )
    }
}
