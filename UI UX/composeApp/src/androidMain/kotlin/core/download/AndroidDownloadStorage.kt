package core.download

import android.content.Context
import core.source.SourcePage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class AndroidDownloadManifest(
    val chapterUrl: String,
    val pageCount: Int,
    val fileNames: List<String>
)

@Serializable
private data class AndroidNovelSavedData(
    val title: String,
    val content: String
)

class AndroidDownloadStorage(context: Context) : DownloadStorage {
    private val root = File(context.filesDir, "downloads").apply { mkdirs() }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun getChapterDir(chapterUrl: String): File {
        val safeName = chapterUrl.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
        val hash = (chapterUrl.hashCode() and 0x7FFFFFFF).toString(16)
        return File(root, "${safeName}_$hash")
    }

    override fun savePageImage(chapterUrl: String, pageIndex: Int, data: ByteArray): String {
        val dir = getChapterDir(chapterUrl).apply { mkdirs() }
        val ext = "jpg"
        val fileName = "$pageIndex.$ext"
        val file = File(dir, fileName)
        file.writeBytes(data)

        val manifestFile = File(dir, "manifest.json")
        val currentManifest = if (manifestFile.exists()) {
            try { json.decodeFromString<AndroidDownloadManifest>(manifestFile.readText()) } catch (_: Exception) { null }
        } else null

        val files = (currentManifest?.fileNames?.toMutableSet() ?: mutableSetOf()).apply { add(fileName) }
        val manifest = AndroidDownloadManifest(
            chapterUrl = chapterUrl,
            pageCount = files.size,
            fileNames = files.sortedBy { it.substringBefore('.').toIntOrNull() ?: 0 }
        )
        manifestFile.writeText(json.encodeToString(manifest))

        return file.toURI().toString()
    }

    override fun saveNovelContent(chapterUrl: String, title: String, content: String): String {
        val dir = getChapterDir(chapterUrl).apply { mkdirs() }
        val file = File(dir, "novel.json")
        val data = AndroidNovelSavedData(title = title, content = content)
        file.writeText(json.encodeToString(data))
        return file.toURI().toString()
    }

    override fun getDownloadedPages(chapterUrl: String): List<SourcePage>? {
        val dir = getChapterDir(chapterUrl)
        if (!dir.exists()) return null
        val manifestFile = File(dir, "manifest.json")
        if (!manifestFile.exists()) return null
        return try {
            val manifest = json.decodeFromString<AndroidDownloadManifest>(manifestFile.readText())
            manifest.fileNames.mapIndexed { index, fileName ->
                val file = File(dir, fileName)
                SourcePage(
                    index = index + 1,
                    url = file.toURI().toString(),
                    imageUrl = file.toURI().toString()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun getDownloadedNovel(chapterUrl: String): Pair<String, String>? {
        val dir = getChapterDir(chapterUrl)
        if (!dir.exists()) return null
        val file = File(dir, "novel.json")
        if (!file.exists()) return null
        return try {
            val data = json.decodeFromString<AndroidNovelSavedData>(file.readText())
            Pair(data.title, data.content)
        } catch (_: Exception) {
            null
        }
    }

    override fun isChapterDownloaded(chapterUrl: String): Boolean {
        val dir = getChapterDir(chapterUrl)
        if (!dir.exists()) return false
        val manifest = File(dir, "manifest.json")
        val novel = File(dir, "novel.json")
        return manifest.exists() || novel.exists()
    }

    override fun deleteChapter(chapterUrl: String): Boolean {
        val dir = getChapterDir(chapterUrl)
        return if (dir.exists()) dir.deleteRecursively() else false
    }

    override fun getStorageBreakdown(): DownloadStorageBreakdown {
        var totalBytes = 0L
        if (root.exists()) {
            root.walkTopDown().filter { it.isFile }.forEach { totalBytes += it.length() }
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
