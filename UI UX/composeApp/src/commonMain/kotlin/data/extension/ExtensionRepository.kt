package data.extension

import core.extensions.ExtensionManager
import core.extensions.ExtensionRepoValidator
import core.extensions.js.ExtensionType
import core.extensions.js.JsExtensionManifest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface ExtensionRepository {
    val extensions: StateFlow<List<ExtensionItem>>
    val repositories: StateFlow<List<ExtensionRepo>>
    val selectedLanguage: StateFlow<String>
    val searchQuery: StateFlow<String>

    fun setLanguageFilter(lang: String)
    fun setSearchQuery(query: String)
    fun installExtension(extensionId: String)
    fun updateExtension(extensionId: String)
    fun uninstallExtension(extensionId: String)
    fun updateAllExtensions()
    suspend fun sideloadExtension(script: String): Result<ExtensionItem>
    suspend fun fetchAndSideloadExtension(url: String): Result<ExtensionItem>
    fun parseExtensionManifest(script: String): JsExtensionManifest?
    fun addRepository(rawUrl: String, customName: String? = null): Result<ExtensionRepo>
    fun removeRepository(repoId: String): Boolean
    fun clearExtensionCache(extensionId: String)
}

class DefaultExtensionRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val httpClient: HttpClient? = null,
    private val extensionManager: ExtensionManager? = null
) : ExtensionRepository {

    private val _extensions = MutableStateFlow<List<ExtensionItem>>(emptyList())
    override val extensions: StateFlow<List<ExtensionItem>> = _extensions.asStateFlow()

    private val _repositories = MutableStateFlow<List<ExtensionRepo>>(emptyList())
    override val repositories: StateFlow<List<ExtensionRepo>> = _repositories.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("ALL")
    override val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    override val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    override fun setLanguageFilter(lang: String) {
        _selectedLanguage.value = lang
    }

    override fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun installExtension(extensionId: String) {
        val target = _extensions.value.find { it.id == extensionId } ?: return
        if (target.status == ExtensionStatus.INSTALLED || target.status == ExtensionStatus.INSTALLING) return

        // Set to INSTALLING state
        _extensions.update { list ->
            list.map { item ->
                if (item.id == extensionId) {
                    item.copy(status = ExtensionStatus.INSTALLING, installProgress = 0.1f)
                } else item
            }
        }

        scope.launch {
            // Simulated smooth progress increments
            delay(120)
            _extensions.update { list ->
                list.map { if (it.id == extensionId) it.copy(installProgress = 0.5f) else it }
            }
            delay(150)
            _extensions.update { list ->
                list.map { if (it.id == extensionId) it.copy(installProgress = 0.9f) else it }
            }
            delay(100)
            _extensions.update { list ->
                list.map {
                    if (it.id == extensionId) {
                        it.copy(
                            status = ExtensionStatus.INSTALLED,
                            installProgress = 1.0f,
                            updateVersion = null
                        )
                    } else it
                }
            }
        }
    }

    override fun updateExtension(extensionId: String) {
        val target = _extensions.value.find { it.id == extensionId } ?: return
        if (target.status != ExtensionStatus.UPDATE_AVAILABLE) return

        _extensions.update { list ->
            list.map { item ->
                if (item.id == extensionId) {
                    item.copy(status = ExtensionStatus.INSTALLING, installProgress = 0.2f)
                } else item
            }
        }

        scope.launch {
            delay(150)
            _extensions.update { list ->
                list.map { if (it.id == extensionId) it.copy(installProgress = 0.7f) else it }
            }
            delay(150)
            _extensions.update { list ->
                list.map {
                    if (it.id == extensionId) {
                        val newVer = it.updateVersion ?: it.versionName
                        it.copy(
                            versionName = newVer,
                            status = ExtensionStatus.INSTALLED,
                            updateVersion = null,
                            installProgress = 1.0f
                        )
                    } else it
                }
            }
        }
    }

    override fun uninstallExtension(extensionId: String) {
        val target = _extensions.value.find { it.id == extensionId } ?: return
        if (target.status != ExtensionStatus.INSTALLED && target.status != ExtensionStatus.UPDATE_AVAILABLE) return

        _extensions.update { list ->
            list.map { item ->
                if (item.id == extensionId) {
                    item.copy(
                        status = ExtensionStatus.AVAILABLE,
                        installProgress = 0f,
                        cachedBytes = 0L
                    )
                } else item
            }
        }
    }

    override fun addRepository(rawUrl: String, customName: String?): Result<ExtensionRepo> {
        val validation = ExtensionRepoValidator.validateAndNormalize(rawUrl)
        if (!validation.isValid || validation.normalizedUrl == null) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid repository URL"))
        }

        val url = validation.normalizedUrl
        if (_repositories.value.any { it.url.equals(url, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Repository already added: $url"))
        }

        val repoName = customName?.takeIf { it.isNotBlank() }
            ?: url.substringAfter("://").substringBefore('/').replaceFirstChar { it.uppercase() }

        val newRepo = ExtensionRepo(
            id = "repo-${kotlin.math.abs(url.hashCode())}-${_repositories.value.size + 1}",
            name = repoName,
            url = url,
            fingerprint = validation.fingerprint ?: ExtensionRepoValidator.computeFingerprint(url),
            isOfficial = false,
            extensionCount = 0
        )

        _repositories.update { it + newRepo }
        return Result.success(newRepo)
    }

    override fun removeRepository(repoId: String): Boolean {
        val target = _repositories.value.find { it.id == repoId } ?: return false
        if (target.isOfficial) return false // Cannot remove official repository

        _repositories.update { it.filter { repo -> repo.id != repoId } }
        return true
    }

    override fun clearExtensionCache(extensionId: String) {
        _extensions.update { list ->
            list.map {
                if (it.id == extensionId) it.copy(cachedBytes = 0L) else it
            }
        }
    }

    override fun updateAllExtensions() {
        val updateableIds = _extensions.value
            .filter { it.status == ExtensionStatus.UPDATE_AVAILABLE }
            .map { it.id }

        for (id in updateableIds) {
            updateExtension(id)
        }
    }

    override suspend fun sideloadExtension(script: String): Result<ExtensionItem> {
        val trimmed = script.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Script content cannot be empty"))
        }
        if (trimmed.length > 5 * 1024 * 1024) { // 5 MB max script size
            return Result.failure(IllegalArgumentException("Script exceeds maximum size limit of 5 MB"))
        }

        val manifest = parseExtensionManifest(trimmed)
            ?: return Result.failure(IllegalArgumentException("Invalid extension script: Manifest not found or malformed. Ensure 'manifest = { id, name, version, baseUrl, ... };' is declared."))

        if (manifest.id.isBlank() || manifest.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Extension manifest must specify valid 'id' and 'name'"))
        }

        // Install into underlying JS runtime engine if available
        extensionManager?.installExtensionFromBundle(trimmed)

        val domain = try {
            manifest.baseUrl.removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':')
        } catch (_: Exception) { "" }

        val item = ExtensionItem(
            id = manifest.id,
            name = manifest.name,
            pkgName = "sideload.${manifest.id}",
            versionName = manifest.version.ifBlank { "1.0.0" },
            versionCode = 1L,
            lang = manifest.lang.ifBlank { "EN" }.uppercase(),
            isNsfw = manifest.isNsfw,
            trustLevel = ExtensionTrustLevel.UNTRUSTED,
            repoUrl = "sideload://local",
            status = ExtensionStatus.INSTALLED,
            iconColorHex = 0xFF38BDF8,
            description = "Sideloaded custom JavaScript extension.",
            sources = listOf(
                ExtensionSourceInfo(
                    id = "src-${manifest.id}",
                    name = manifest.name,
                    baseUrl = manifest.baseUrl,
                    languages = listOf(manifest.lang.ifBlank { "EN" }.uppercase()),
                    contentType = when (manifest.type) {
                        ExtensionType.NOVEL -> "Novel"
                        ExtensionType.BOTH -> "Manga / Novel"
                        else -> "Manga"
                    }
                )
            ),
            networkDomains = if (domain.isNotBlank()) listOf(domain) else emptyList(),
            cachedBytes = 0L,
            installProgress = 1.0f
        )

        _extensions.update { list ->
            list.filter { it.id != item.id } + item
        }

        return Result.success(item)
    }

    override suspend fun fetchAndSideloadExtension(url: String): Result<ExtensionItem> {
        val validation = ExtensionRepoValidator.validateAndNormalize(url)
        if (!validation.isValid || validation.normalizedUrl == null) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid URL"))
        }

        val client = httpClient
            ?: return Result.failure(IllegalStateException("HTTP client not configured for remote downloads"))

        return try {
            val response = client.get(validation.normalizedUrl)
            val script = response.bodyAsText()
            sideloadExtension(script)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to download extension from ${validation.normalizedUrl}: ${e.message}"))
        }
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun parseExtensionManifest(script: String): JsExtensionManifest? {
        return try {
            val match = Regex("""(?:const|let|var)?\s*manifest\s*=\s*(\{[\s\S]*?\})\s*;?""").find(script)
            if (match != null) {
                val rawStr = match.groupValues[1]
                val cleanedJson = rawStr
                    .replace(Regex("""//.*"""), "")
                    .replace(Regex("""/\*[\s\S]*?\*/"""), "")
                    .trim()
                val jsonParser = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    allowTrailingComma = true
                }
                val dto = jsonParser.decodeFromString<ManifestJsonDto>(cleanedJson)
                JsExtensionManifest(
                    id = dto.id,
                    name = dto.name,
                    version = dto.version,
                    lang = dto.lang,
                    baseUrl = dto.baseUrl,
                    type = try { ExtensionType.valueOf(dto.type.uppercase()) } catch (_: Exception) { ExtensionType.COMIC },
                    iconUrl = dto.iconUrl,
                    isNsfw = dto.isNsfw,
                    script = script
                )
            } else {
                extractManifestFieldsByRegex(script)
            }
        } catch (_: Exception) {
            extractManifestFieldsByRegex(script)
        }
    }

    private fun extractManifestFieldsByRegex(script: String): JsExtensionManifest? {
        return try {
            fun extractField(fieldName: String): String {
                val pattern = Regex("""["']?$fieldName["']?\s*:\s*["']([^"']*)["']""")
                return pattern.find(script)?.groupValues?.get(1)?.trim() ?: ""
            }

            val id = extractField("id")
            val name = extractField("name")
            if (id.isBlank() || name.isBlank()) return null

            val version = extractField("version").ifBlank { "1.0.0" }
            val lang = extractField("lang").ifBlank { "en" }
            val baseUrl = extractField("baseUrl")
            val typeStr = extractField("type")
            val isNsfw = Regex("""["']?isNsfw["']?\s*:\s*(true|false)""").find(script)?.groupValues?.get(1)?.toBoolean() ?: false
            val iconUrl = extractField("iconUrl").ifBlank { null }

            val type = try {
                ExtensionType.valueOf(typeStr.uppercase())
            } catch (_: Exception) {
                ExtensionType.COMIC
            }

            JsExtensionManifest(
                id = id,
                name = name,
                version = version,
                lang = lang,
                baseUrl = baseUrl,
                type = type,
                iconUrl = iconUrl,
                isNsfw = isNsfw,
                script = script
            )
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
internal data class ManifestJsonDto(
    val id: String = "",
    val name: String = "",
    val version: String = "1.0.0",
    val lang: String = "en",
    val baseUrl: String = "",
    val type: String = "comic",
    val iconUrl: String? = null,
    val isNsfw: Boolean = false
)
