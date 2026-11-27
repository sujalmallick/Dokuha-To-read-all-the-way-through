package data.backup

import core.source.SourceManga
import data.library.CategoryRepository
import data.mock.HistoryItem
import data.mock.PanelRepository
import data.settings.DefaultReadingMode
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.ReaderColorFilter
import data.settings.ReaderPrefsRepository
import data.settings.TitleReaderPrefs
import data.settings.safeEnumValueOf
import data.settings.sanitized
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
data class BackupReaderPrefs(
    @SerialName("title_key") val titleKey: String,
    @SerialName("reading_mode") val readingMode: String? = null,
    @SerialName("color_filter") val colorFilter: String? = null,
    @SerialName("side_padding") val sidePadding: Float? = null,
    @SerialName("split_double_spread") val splitDoubleSpread: Boolean? = null,
    @SerialName("dual_page_offset") val dualPageOffset: Boolean? = null,
    @SerialName("novel_theme") val novelTheme: String? = null,
    @SerialName("novel_font") val novelFont: String? = null,
    @SerialName("novel_font_size") val novelFontSize: Float? = null,
    @SerialName("novel_line_height") val novelLineHeight: Float? = null,
    @SerialName("updated_at") val updatedAt: Long = 0L
)

@Serializable
data class BackupFile(
    @SerialName("schema_version") val schemaVersion: Int = 3,
    @SerialName("created_at") val createdAt: Long = 0L,
    val library: List<BackupManga> = emptyList(),
    val history: List<BackupHistoryEntry> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    @SerialName("category_mappings") val categoryMappings: Map<String, List<String>> = emptyMap(),
    @SerialName("reader_prefs") val readerPrefs: List<BackupReaderPrefs> = emptyList()
)

// ─── Engine ────────────────────────────────────────────────────────────────

private val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

object BackupEngine {

    /**
     * Collect the current library, categories, reading history, and persisted per-title
     * reader preferences (never including incognito session overlays) and serialise them
     * to a compact JSON string.
     */
    suspend fun exportJson(
        repository: PanelRepository,
        nowMs: Long,
        categoryRepository: CategoryRepository? = null,
        readerPrefsRepository: ReaderPrefsRepository? = null
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

        // Always reads persisted DB rows via getAllPrefs(), never incognito session overlays
        val backupReaderPrefs = readerPrefsRepository?.getAllPrefs()
            ?.map { p ->
                BackupReaderPrefs(
                    titleKey = p.titleKey,
                    readingMode = p.readingMode?.name,
                    colorFilter = p.colorFilter?.name,
                    sidePadding = p.sidePadding,
                    splitDoubleSpread = p.splitDoubleSpread,
                    dualPageOffset = p.dualPageOffset,
                    novelTheme = p.novelTheme?.name,
                    novelFont = p.novelFont?.name,
                    novelFontSize = p.novelFontSize,
                    novelLineHeight = p.novelLineHeight,
                    updatedAt = p.updatedAt
                )
            }
            ?: emptyList()

        return backupJson.encodeToString(
            BackupFile(
                schemaVersion = 3,
                createdAt = nowMs,
                library = backupManga,
                history = backupHistory,
                categories = backupCategories,
                categoryMappings = backupMappings,
                readerPrefs = backupReaderPrefs
            )
        )
    }

    /**
     * Parse [jsonString] (Dokuha v1/v2/v3 JSON or Mihon/Tachiyomi JSON backup) and restore
     * library titles, categories, reading progress, and per-title reader preferences.
     *
     * Restore policy for per-title reader preferences: **Local wins** — if a title already
     * has persisted preferences locally, existing local preferences are preserved; otherwise
     * the backed-up preferences are restored regardless of whether the title is in [BackupFile.library].
     *
     * Note: [backupJson] uses `ignoreUnknownKeys = true` and does not reject higher `schema_version`
     * values, so older app versions can still import known fields from newer backups.
     *
     * @return Number of library titles restored, or -1 if the JSON is malformed.
     */
    suspend fun importJson(
        jsonString: String,
        repository: PanelRepository,
        categoryRepository: CategoryRepository? = null,
        readerPrefsRepository: ReaderPrefsRepository? = null
    ): Int {
        // 1. Try Dokuha BackupFile format (v1, v2, v3+)
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
                val oldToNewCatId = mutableMapOf<String, String>()
                val existingByName = categoryRepository.categories.value
                    .associateBy { it.name.trim().lowercase() }
                    .toMutableMap()

                dokuhaBackup.categories.forEach { cat ->
                    val normalizedName = cat.name.trim().lowercase()
                    val existing = existingByName[normalizedName]
                    if (existing != null) {
                        oldToNewCatId[cat.id] = existing.id
                    } else {
                        val created = categoryRepository.addCategory(cat.name).getOrNull()
                        if (created != null) {
                            oldToNewCatId[cat.id] = created.id
                            existingByName[normalizedName] = created
                        }
                    }
                }

                val validCurrentIds = categoryRepository.categories.value.map { it.id }.toSet()

                dokuhaBackup.categoryMappings.forEach { (mangaUrl, catIds) ->
                    val remappedIds = catIds.mapNotNull { oldId ->
                        oldToNewCatId[oldId] ?: oldId.takeIf { it in validCurrentIds }
                    }.toSet()
                    if (remappedIds.isNotEmpty()) {
                        categoryRepository.setMangaCategories(mangaUrl, remappedIds)
                    }
                }
            }

            for (hist in dokuhaBackup.history) {
                repository.updateChapterProgress(hist.chapterUrl, hist.lastPageRead, isRead = false)
            }

            if (readerPrefsRepository != null) {
                for (prefEntry in dokuhaBackup.readerPrefs) {
                    if (prefEntry.titleKey.isBlank()) continue
                    // Local wins: preserve existing local overrides if already present
                    if (readerPrefsRepository.getPrefs(prefEntry.titleKey) != null) continue
                    val model = TitleReaderPrefs(
                        titleKey = prefEntry.titleKey,
                        readingMode = safeEnumValueOf<DefaultReadingMode>(prefEntry.readingMode),
                        colorFilter = safeEnumValueOf<ReaderColorFilter>(prefEntry.colorFilter),
                        sidePadding = prefEntry.sidePadding,
                        splitDoubleSpread = prefEntry.splitDoubleSpread,
                        dualPageOffset = prefEntry.dualPageOffset,
                        novelTheme = safeEnumValueOf<NovelReaderTheme>(prefEntry.novelTheme),
                        novelFont = safeEnumValueOf<NovelFont>(prefEntry.novelFont),
                        novelFontSize = prefEntry.novelFontSize,
                        novelLineHeight = prefEntry.novelLineHeight,
                        updatedAt = prefEntry.updatedAt
                    ).sanitized()
                    if (model.hasAnyOverrides) {
                        readerPrefsRepository.upsertPrefs(model)
                    }
                }
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
