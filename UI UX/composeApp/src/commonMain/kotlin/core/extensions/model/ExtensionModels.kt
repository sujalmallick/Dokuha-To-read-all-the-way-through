package core.extensions.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
enum class ExtensionEcosystem {
    MANGAYOMI,
    LNREADER,
    PAPERBACK,
    CUSTOM
}

/**
 * Model representing an entry in a Mangayomi index.json or novel_index.json catalog.
 */
@Serializable
data class MangayomiCatalogItem(
    val id: Long,
    val name: String,
    val baseUrl: String,
    val lang: String,
    val version: String,
    val sourceCodeUrl: String,
    val sourceCodeLanguage: Int = 1, // 0 = Dart (ignored), 1 = JavaScript
    val itemType: Int = 0,           // 0 = Manga, 1 = Anime, 2 = Novel
    val iconUrl: String = "",
    val typeSource: String = "single",
    val isNsfw: Boolean = false,
    val hasCloudflare: Boolean = false
)

/**
 * Model representing an entry in an LNReader plugins.min.json catalog.
 */
@Serializable
data class LNReaderCatalogItem(
    val id: String,
    val name: String,
    val site: String,
    val lang: String,
    val version: String,
    val url: String,
    val iconUrl: String? = null,
    val filters: JsonElement? = null
)

/**
 * Model representing Paperback versioning.json catalog manifest.
 */
@Serializable
data class PaperbackVersioningManifest(
    val buildTime: String? = null,
    val builtWith: JsonElement? = null,
    val sources: List<PaperbackSourceItem> = emptyList(),
    val repository: JsonObject? = null
)

/**
 * Model representing an individual source item within a Paperback versioning.json.
 */
@Serializable
data class PaperbackSourceItem(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val desc: String? = null,
    val description: String? = null,
    val website: String? = null,
    val websiteBaseURL: String? = null,
    val icon: String = "icon.png",
    val contentRating: String = "EVERYONE",
    val language: String = "en",
    val intents: Int? = null,
    val capabilities: List<Int>? = null
)
