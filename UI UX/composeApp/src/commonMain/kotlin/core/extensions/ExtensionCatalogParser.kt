package core.extensions

import core.extensions.js.ExtensionCatalogItem
import core.extensions.js.ExtensionType
import core.extensions.model.ExtensionEcosystem
import core.extensions.model.LNReaderCatalogItem
import core.extensions.model.MangayomiCatalogItem
import core.extensions.model.PaperbackVersioningManifest
import kotlinx.serialization.json.*

/**
 * Polymorphic parser capable of auto-detecting and indexing extension repositories from:
 * 1. Mangayomi (index.json, novel_index.json)
 * 2. LNReader (plugins.min.json)
 * 3. Paperback (versioning.json)
 * 4. Dokuha Native Catalogs (index.min.json, index.json)
 */
class ExtensionCatalogParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {

    fun parseCatalog(rawBody: String, repoBaseUrl: String): List<ExtensionCatalogItem> {
        val root = try {
            json.parseToJsonElement(rawBody)
        } catch (_: Exception) {
            return emptyList()
        }

        // Branch 1: Paperback returns root JsonObject containing "sources" array
        if (root is JsonObject && root.containsKey("sources")) {
            return try {
                val manifest = json.decodeFromJsonElement<PaperbackVersioningManifest>(root)
                manifest.sources.map { src ->
                    val base = src.websiteBaseURL ?: src.website ?: ""
                    val scriptUrl = "${repoBaseUrl.trimEnd('/')}/${src.id}/index.js"
                    val iconUrl = if (src.icon.startsWith("http://") || src.icon.startsWith("https://")) {
                        src.icon
                    } else {
                        "${repoBaseUrl.trimEnd('/')}/${src.id}/${src.icon.trimStart('/')}"
                    }
                    ExtensionCatalogItem(
                        id = src.id,
                        name = src.name,
                        baseUrl = base,
                        pkg = scriptUrl,
                        iconUrl = iconUrl,
                        version = src.version,
                        lang = src.language,
                        type = ExtensionType.COMIC,
                        isNsfw = src.contentRating.equals("ADULT", ignoreCase = true),
                        ecosystem = ExtensionEcosystem.PAPERBACK,
                        repoBaseUrl = repoBaseUrl
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        // Branch 2: Array-based catalogs (Mangayomi, LNReader, or native Dokuha)
        if (root is JsonArray && root.isNotEmpty()) {
            val first = root.firstOrNull() as? JsonObject ?: return emptyList()

            // LNReader contains "site" and "url"
            if (first.containsKey("site") && first.containsKey("url")) {
                return try {
                    val items = json.decodeFromJsonElement<List<LNReaderCatalogItem>>(root)
                    items.map { item ->
                        ExtensionCatalogItem(
                            id = item.id,
                            name = item.name,
                            baseUrl = item.site,
                            pkg = item.url,
                            iconUrl = item.iconUrl?.takeIf { it.isNotBlank() },
                            version = item.version,
                            lang = item.lang,
                            type = ExtensionType.NOVEL,
                            isNsfw = false,
                            ecosystem = ExtensionEcosystem.LNREADER,
                            repoBaseUrl = repoBaseUrl
                        )
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            // Mangayomi contains "sourceCodeLanguage" or "sourceCodeUrl"
            if (first.containsKey("sourceCodeLanguage") || first.containsKey("sourceCodeUrl")) {
                return try {
                    val items = json.decodeFromJsonElement<List<MangayomiCatalogItem>>(root)
                    items.filter { it.sourceCodeLanguage == 1 && it.itemType != 1 } // JS only, skip Anime (1)
                        .map { item ->
                            val isNovel = item.itemType == 2
                            ExtensionCatalogItem(
                                id = item.id.toString(),
                                name = item.name,
                                baseUrl = item.baseUrl,
                                pkg = item.sourceCodeUrl,
                                iconUrl = item.iconUrl.takeIf { it.isNotBlank() },
                                version = item.version,
                                lang = item.lang,
                                type = if (isNovel) ExtensionType.NOVEL else ExtensionType.COMIC,
                                isNsfw = item.isNsfw,
                                ecosystem = ExtensionEcosystem.MANGAYOMI,
                                repoBaseUrl = repoBaseUrl
                            )
                        }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            // Native Dokuha catalog items (containing "pkg")
            if (first.containsKey("pkg")) {
                return try {
                    json.decodeFromJsonElement<List<ExtensionCatalogItem>>(root)
                        .map { it.copy(repoBaseUrl = repoBaseUrl) }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        return emptyList()
    }
}
