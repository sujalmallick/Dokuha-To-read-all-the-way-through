package data.backup

import core.source.SourceManga
import data.mock.HistoryItem
import data.mock.PanelRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    @SerialName("source_id") val sourceId: Long = 0L
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
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("created_at") val createdAt: Long = 0L,
    val library: List<BackupManga> = emptyList(),
    val history: List<BackupHistoryEntry> = emptyList()
)

// ─── Engine ────────────────────────────────────────────────────────────────

private val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

object BackupEngine {

    /**
     * Collect the current library and reading history from [repository] and
     * serialise them to a compact JSON string ready for export / file-write.
     */
    suspend fun exportJson(
        repository: PanelRepository,
        nowMs: Long
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
                sourceId = m.sourceId
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

        return backupJson.encodeToString(
            BackupFile(
                schemaVersion = 1,
                createdAt = nowMs,
                library = backupManga,
                history = backupHistory
            )
        )
    }

    /**
     * Parse [jsonString] and restore library titles into [repository].
     *
     * @return Number of library titles restored, or -1 if the JSON is malformed.
     */
    suspend fun importJson(
        jsonString: String,
        repository: PanelRepository
    ): Int {
        val backup = runCatching { backupJson.decodeFromString<BackupFile>(jsonString) }
            .getOrNull() ?: return -1

        // Restore favourite titles
        for (entry in backup.library) {
            val manga = SourceManga(
                url = entry.url,
                title = entry.title,
                author = entry.author,
                artist = entry.artist,
                description = entry.description,
                thumbnailUrl = entry.thumbnailUrl,
                genres = entry.genres,
                sourceId = entry.sourceId
            )
            repository.toggleFavorite(manga, isFavorite = true)
        }

        return backup.library.size
    }
}
