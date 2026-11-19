package data.extension

/**
 * Trust and safety classification for an extension source.
 */
enum class ExtensionTrustLevel {
    /** Cryptographically verified official upstream extension repository */
    OFFICIAL,
    /** Reputable community repository */
    COMMUNITY,
    /** Unverified third-party external repository */
    UNTRUSTED
}

/**
 * Lifecycle state machine for extensions.
 */
enum class ExtensionStatus {
    AVAILABLE,
    INSTALLING,
    INSTALLED,
    UPDATE_AVAILABLE,
    UNINSTALLING
}

/**
 * Metadata for a content source provided by an extension.
 */
data class ExtensionSourceInfo(
    val id: String,
    val name: String,
    val baseUrl: String,
    val languages: List<String>,
    val contentType: String = "Manga" // Manga, Manhwa, Webnovel, Comic
)

/**
 * Comprehensive model representing an extension package.
 */
data class ExtensionItem(
    val id: String,
    val name: String,
    val pkgName: String,
    val versionName: String,
    val versionCode: Long,
    val lang: String, // "ALL", "EN", "JA", "KO", "ES", "FR"
    val isNsfw: Boolean = false,
    val trustLevel: ExtensionTrustLevel = ExtensionTrustLevel.OFFICIAL,
    val repoUrl: String,
    val status: ExtensionStatus = ExtensionStatus.AVAILABLE,
    val installProgress: Float = 0f, // 0.0f .. 1.0f when INSTALLING
    val updateVersion: String? = null,
    val iconColorHex: Long = 0xFF38BDF8,
    val description: String = "",
    val sources: List<ExtensionSourceInfo> = emptyList(),
    val repoFingerprint: String? = null,
    val networkDomains: List<String> = emptyList(),
    val cachedBytes: Long = 0L
) {
    val hasUpdate: Boolean get() = status == ExtensionStatus.UPDATE_AVAILABLE
    val isInstalled: Boolean get() = status == ExtensionStatus.INSTALLED || status == ExtensionStatus.UPDATE_AVAILABLE
}

/**
 * Verified repository configuration.
 */
data class ExtensionRepo(
    val id: String,
    val name: String,
    val url: String,
    val fingerprint: String,
    val isOfficial: Boolean = false,
    val extensionCount: Int = 0
)
