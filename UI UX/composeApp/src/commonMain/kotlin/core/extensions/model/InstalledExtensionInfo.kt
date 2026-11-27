package core.extensions.model

/**
 * Production domain model representing an installed extension surfaced by [data.mock.PanelRepository.getExtensions].
 */
data class InstalledExtensionInfo(
    val id: String,
    val name: String,
    val version: String,
    val lang: String,
    val isInstalled: Boolean,
    val hasUpdate: Boolean,
    val iconUrl: String? = null
)
