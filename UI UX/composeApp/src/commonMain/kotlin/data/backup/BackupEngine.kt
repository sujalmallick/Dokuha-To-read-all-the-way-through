package data.backup

import core.source.SourceManga
import data.library.CategoryRepository
import data.mock.HistoryItem
import data.mock.PanelRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

// ─── Portable backup schema ────────────────────────────────────────────────

@Serializable
data class BackupManga(
    val url: String,
    val title: String,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val genres: List<String> = emptyList(),
    @SerialName("source_id") val sourceId: Long = 0L,
    @SerialName("is_novel") val isNovel: Boolean = false
)

@Serializable
data class BackupCategory(
    val id: String,
    val name: String,
    val order: Int = 0
)

@Serializable
data class BackupHistoryEntry(
    val mangaUrl: String,
    val chapterUrl: String,
    val chapterName: String,
    @SerialName("readAt") val readAt: Long,
    val lastPageRead: Int = 0
)

@Serializable
data class BackupFile(
    @SerialName("schema_version") val schemaVersion: Int = 2,
    @SerialName("created_at") val createdAt: Long = 0L,
    val library: List<BackupManga> = emptyList(),
    val history: List<BackupHistoryEntry> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    @SerialName("category_mappings") val categoryMappings: Map<String, List<String>> = emptyMap()
)

// ─── Engine ────────────────────────────────────────────────────────────────

private val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

object BackupEngine {

    /**
     * Collect the current library, categories, and reading history from [repository]
     * and [categoryRepository] and serialise them to a compact JSON string.
     */
    suspend fun exportJson(
        repository: PanelRepository,
        nowMs: Long,
        categoryRepository: CategoryRepository? = null
    ): String {
        val library: List<SourceManga> =
            repository.getLibrary(category = "").firstOrNull() ?: emptyList()

        val historyItems: List<HistoryItem> =
            repository.getRecentlyReadManga(limit = 5000L).firstOrNull() ?: emptyList()

        val backupManga = library.map { m ->
            BackupManga(
                url = m.url,
                title = m.title,
                author = m.author,
                artist = m.artist,
                description = m.description,
                thumbnailUrl = m.thumbnailUrl,
                genres = m.genres,
                sourceId = m.sourceId,
                isNovel = m.isNovel
            )
        }

        val backupHistory = historyItems.map { h ->
            BackupHistoryEntry(
                mangaUrl = h.manga.url,
                chapterUrl = h.chapter.url,
                chapterName = h.chapter.name,
                readAt = h.readAt,
                lastPageRead = h.lastPageRead
            )
        }

        val backupCategories = categoryRepository?.categories?.value
            ?.filter { !it.isDefault }
            ?.map { BackupCategory(id = it.id, name = it.name, order = it.order) }
            ?: emptyList()

        val backupMappings = categoryRepository?.mangaCategoryMappings?.value
            ?.mapValues { (_, set) -> set.toList() }
            ?: emptyMap()

        return backupJson.encodeToString(
            BackupFile(
                schemaVersion = 2,
                createdAt = nowMs,
                library = backupManga,
                history = backupHistory,
                categories = backupCategories,
                categoryMappings = backupMappings
            )
        )
    }

    /**
     * Parse [jsonString] (Dokuha v1/v2 JSON or Mihon/Tachiyomi JSON backup) and restore
     * library titles, categories, and reading progress into [repository].
     *
     * @return Number of library titles restored, or -1 if the JSON is malformed.
     */
    suspend fun importJson(
        jsonString: String,
        repository: PanelRepository,
        categoryRepository: CategoryRepository? = null
    ): Int {
        // 1. Try Dokuha BackupFile format
        val dokuhaBackup = runCatching { backupJson.decodeFromString<BackupFile>(jsonString) }.getOrNull()
        if (dokuhaBackup != null && (jsonString.contains("\"library\"") || jsonString.contains("\"schema_version\""))) {
            for (entry in dokuhaBackup.library) {
                val manga = SourceManga(
                    url = entry.url,
                    title = entry.title,
                    author = entry.author,
                    artist = entry.artist,
                    description = entry.description,
                    thumbnailUrl = entry.thumbnailUrl,
                    genres = entry.genres,
                    sourceId = entry.sourceId,
                    isNovel = entry.isNovel
                )
                repository.toggleFavorite(manga, isFavorite = true)
            }

            if (categoryRepository != null) {
                val existingNames = categoryRepository.categories.value.map { it.name }.toSet()
                dokuhaBackup.categories.forEach { cat ->
                    if (cat.name !in existingNames) {
                        categoryRepository.addCategory(cat.name)
                    }
                }
                dokuhaBackup.categoryMappings.forEach { (mangaUrl, catIds) ->
                    if (catIds.isNotEmpty()) {
                        categoryRepository.setMangaCategories(mangaUrl, catIds.toSet())
                    }
                }
            }

            for (hist in dokuhaBackup.history) {
                repository.updateChapterProgress(hist.chapterUrl, hist.lastPageRead, isRead = false)
            }

            return dokuhaBackup.library.size
        }

        // 2. Fallback: Mihon / Tachiyomi JSON (`backupManga` array)
        return runCatching {
            val root = backupJson.parseToJsonElement(jsonString).jsonObject
            val mangaArray = root["backupManga"]?.jsonArray ?: return -1
            var count = 0
            for (elem in mangaArray) {
                val obj = elem.jsonObject
                val url = obj["url"]?.jsonPrimitive?.content ?: continue
                val title = obj["title"]?.jsonPrimitive?.content ?: continue
                val manga = SourceManga(
                    url = url,
                    title = title,
                    author = obj["author"]?.jsonPrimitive?.content,
                    artist = obj["artist"]?.jsonPrimitive?.content,
                    description = obj["description"]?.jsonPrimitive?.content,
                    thumbnailUrl = obj["thumbnailUrl"]?.jsonPrimitive?.content,
                    sourceId = obj["source"]?.jsonPrimitive?.longOrNull ?: 0L
                )
                repository.toggleFavorite(manga, isFavorite = true)
                count++
            }
            count
        }.getOrDefault(-1)
    }
}
