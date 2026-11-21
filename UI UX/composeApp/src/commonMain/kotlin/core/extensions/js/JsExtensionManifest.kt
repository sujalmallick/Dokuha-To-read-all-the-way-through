package core.extensions.js

import core.extensions.model.ExtensionEcosystem
import kotlinx.serialization.Serializable

@Serializable
enum class ExtensionType {
    COMIC,
    NOVEL,
    BOTH
}

@Serializable
data class JsExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val lang: String,
    val baseUrl: String,
    val type: ExtensionType,
    val iconUrl: String? = null,
    val isNsfw: Boolean = false,
    val script: String,
    val ecosystem: ExtensionEcosystem = ExtensionEcosystem.CUSTOM
)

@Serializable
data class ExtensionCatalogItem(
    val id: String,
    val name: String,
    val version: String,
    val lang: String,
    val baseUrl: String,
    val type: ExtensionType,
    val iconUrl: String? = null,
    val isNsfw: Boolean = false,
    val pkg: String,
    val sha256: String = "",
    val ecosystem: ExtensionEcosystem = ExtensionEcosystem.CUSTOM,
    /** The base URL of the repository this item was fetched from. Used for repo-picker UI. */
    val repoBaseUrl: String = ""
)
