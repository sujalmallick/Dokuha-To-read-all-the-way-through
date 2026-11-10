package core.extensions.js

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
    val script: String
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
    val sha256: String
)
