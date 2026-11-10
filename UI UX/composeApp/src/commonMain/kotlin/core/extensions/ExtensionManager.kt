package core.extensions

import core.extensions.js.ExtensionCatalogItem
import core.extensions.js.ExtensionType
import core.extensions.js.JsBridge
import core.extensions.js.JsEngine
import core.extensions.js.JsExtensionManifest
import core.extensions.js.JsExtensionSource
import core.source.ContentSource
import core.storage.KeyValueStorage
import data.mock.PanelRepository
import data.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages the lifecycle of JavaScript extensions: installing, updating,
 * persisting bundles to storage, and dynamically registering sources into PanelRepository.
 */
class ExtensionManager(
    private val storage: KeyValueStorage,
    private val panelRepository: PanelRepository? = null,
    private val httpClient: HttpClient? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _installedExtensions = MutableStateFlow<List<JsExtensionManifest>>(emptyList())
    val installedExtensions: StateFlow<List<JsExtensionManifest>> = _installedExtensions.asStateFlow()

    private val _availableExtensions = MutableStateFlow<List<ExtensionCatalogItem>>(SampleExtensions.DEMO_CATALOG_ITEMS)
    val availableExtensions: StateFlow<List<ExtensionCatalogItem>> = _availableExtensions.asStateFlow()

    private val _repoUrls = MutableStateFlow<List<String>>(emptyList())
    val repoUrls: StateFlow<List<String>> = _repoUrls.asStateFlow()

    private val activeSources = mutableMapOf<String, JsExtensionSource>()

    init {
        loadRepositories()
        restoreInstalledExtensions()
        settingsRepository?.let { settings ->
            coroutineScope.launch {
                settings.extensionRepos.collect { repos ->
                    val urls = repos.map { it.url }
                    if (urls != _repoUrls.value) {
                        _repoUrls.value = urls
                        saveRepositories(urls)
                        refreshCatalog()
                    }
                }
            }
        }
    }

    /**
     * Adds and persists a new repository URL after validation.
     */
    fun addRepository(url: String): ExtensionRepoValidator.ValidationResult {
        val result = ExtensionRepoValidator.validateAndNormalize(url)
        if (!result.isValid || result.normalizedUrl == null) {
            return result
        }

        val current = _repoUrls.value.toMutableList()
        if (!current.contains(result.normalizedUrl)) {
            current.add(result.normalizedUrl)
            _repoUrls.value = current
            saveRepositories(current)
            coroutineScope.launch {
                refreshCatalog()
            }
        }
        return result
    }

    /**
     * Refreshes the extension catalog from all configured repository URLs.
     */
    suspend fun refreshCatalog() {
        val client = httpClient
        val items = mutableListOf<ExtensionCatalogItem>()

        if (client != null) {
            for (repoUrl in _repoUrls.value) {
                try {
                    val indexUrl = "$repoUrl/index.min.json"
                    val response = client.get(indexUrl)
                    val body = response.bodyAsText()
                    val parsed = json.decodeFromString<List<ExtensionCatalogItem>>(body)
                    items.addAll(parsed)
                } catch (_: Exception) {
                    // Try fallback to index.json
                    try {
                        val indexUrl = "$repoUrl/index.json"
                        val response = client.get(indexUrl)
                        val body = response.bodyAsText()
                        val parsed = json.decodeFromString<List<ExtensionCatalogItem>>(body)
                        items.addAll(parsed)
                    } catch (_: Exception) {
                        // Skip unreachable repo
                    }
                }
            }
        }

        val allItems = (SampleExtensions.DEMO_CATALOG_ITEMS + items).distinctBy { it.id }
        _availableExtensions.value = allItems
    }

    /**
     * Downloads and installs an extension from a repository.
     */
    suspend fun installExtension(catalogItem: ExtensionCatalogItem, repoBaseUrl: String): Boolean {
        if (catalogItem.pkg.startsWith("demo://")) {
            val demoScript = SampleExtensions.getDemoBundle(catalogItem.id) ?: return false
            return installExtensionFromBundle(demoScript)
        }

        val client = httpClient ?: return false
        val pkgUrl = if (catalogItem.pkg.startsWith("http://") || catalogItem.pkg.startsWith("https://")) {
            catalogItem.pkg
        } else {
            "${repoBaseUrl.trimEnd('/')}/${catalogItem.pkg.trimStart('/')}"
        }

        return try {
            val response = client.get(pkgUrl)
            val script = response.bodyAsText()

            // Verify SHA-256 fingerprint if provided
            if (catalogItem.sha256.isNotBlank()) {
                val computedHash = Sha256.hashHex(script).lowercase()
                if (!computedHash.equals(catalogItem.sha256.lowercase(), ignoreCase = true)) {
                    println("[ExtensionManager] Checksum mismatch for ${catalogItem.id}: expected ${catalogItem.sha256}, got $computedHash")
                    return false
                }
            }

            installExtensionFromBundle(script)
        } catch (e: Exception) {
            println("[ExtensionManager] Failed to install extension ${catalogItem.id}: ${e.message}")
            false
        }
    }

    /**
     * Installs an extension directly from a single .js bundle script containing embedded manifest.
     */
    suspend fun installExtensionFromBundle(bundleScript: String): Boolean {
        val manifest = extractManifest(bundleScript) ?: return false

        // Persist manifest and script
        saveManifest(manifest)

        // Instantiate, initialize, and register the source
        registerAndActivateSource(manifest)

        val updated = _installedExtensions.value.filter { it.id != manifest.id }.toMutableList()
        updated.add(manifest)
        _installedExtensions.value = updated

        return true
    }

    /**
     * Uninstalls an extension and removes it from storage and PanelRepository.
     */
    fun uninstallExtension(extensionId: String) {
        val source = activeSources.remove(extensionId)
        if (source != null) {
            panelRepository?.unregisterSource(source.id)
            source.close()
        }

        storage.remove(storageManifestKey(extensionId))
        val currentIds = getStoredInstalledIds().toMutableList()
        currentIds.remove(extensionId)
        storage.putString(KEY_INSTALLED_IDS, json.encodeToString(currentIds))

        _installedExtensions.value = _installedExtensions.value.filter { it.id != extensionId }
    }

    fun getInstalledSource(extensionId: String): ContentSource? = activeSources[extensionId]

    fun getAllInstalledSources(): List<ContentSource> = activeSources.values.toList()

    private suspend fun registerAndActivateSource(manifest: JsExtensionManifest) {
        try {
            val engine = JsEngine()
            val bridge = JsBridge(httpClient = httpClient)
            val source = JsExtensionSource(manifest = manifest, engine = engine, bridge = bridge)

            source.initialize()

            activeSources[manifest.id] = source
            panelRepository?.registerSource(source)
        } catch (e: Exception) {
            println("[ExtensionManager] Failed to initialize source ${manifest.id}: ${e.message}")
        }
    }

    private fun restoreInstalledExtensions() {
        val installedIds = getStoredInstalledIds()
        val restored = mutableListOf<JsExtensionManifest>()

        for (id in installedIds) {
            val manifestJson = storage.getString(storageManifestKey(id), "")
            if (manifestJson.isNotEmpty()) {
                try {
                    val manifest = json.decodeFromString<JsExtensionManifest>(manifestJson)
                    restored.add(manifest)
                    coroutineScope.launch {
                        registerAndActivateSource(manifest)
                    }
                } catch (e: Exception) {
                    println("[ExtensionManager] Failed to restore extension $id: ${e.message}")
                }
            }
        }

        _installedExtensions.value = restored
    }

    private fun saveManifest(manifest: JsExtensionManifest) {
        val currentIds = getStoredInstalledIds().toMutableList()
        if (!currentIds.contains(manifest.id)) {
            currentIds.add(manifest.id)
            storage.putString(KEY_INSTALLED_IDS, json.encodeToString(currentIds))
        }
        storage.putString(storageManifestKey(manifest.id), json.encodeToString(manifest))
    }

    private fun getStoredInstalledIds(): List<String> {
        val idsJson = storage.getString(KEY_INSTALLED_IDS, "[]")
        return try {
            json.decodeFromString<List<String>>(idsJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadRepositories() {
        val reposJson = storage.getString(KEY_REPO_URLS, "[]")
        val repos = try {
            json.decodeFromString<List<String>>(reposJson)
        } catch (_: Exception) {
            emptyList()
        }
        _repoUrls.value = repos
    }

    private fun saveRepositories(repos: List<String>) {
        storage.putString(KEY_REPO_URLS, json.encodeToString(repos))
    }

    private fun storageManifestKey(id: String) = "dokuha_ext_manifest_$id"

    /**
     * Extracts extension manifest metadata from a single .js bundle.
     */
    suspend fun extractManifest(script: String): JsExtensionManifest? {
        return try {
            val engine = JsEngine()
            engine.evaluate(script)
            val manifestJson = engine.evaluate("JSON.stringify(manifest)")
            engine.close()
            val dto = json.decodeFromString<ManifestDto>(manifestJson)
            dto.toManifest(script)
        } catch (_: Exception) {
            // Regex fallback for top-level manifest JSON
            extractManifestRegex(script)
        }
    }

    private fun extractManifestRegex(script: String): JsExtensionManifest? {
        return try {
            val match = Regex("""manifest\s*=\s*(\{[\s\S]*?\});""").find(script)
            if (match != null) {
                val rawStr = match.groupValues[1]
                val cleanedJson = rawStr
                    .replace(Regex("""//.*"""), "")
                    .replace(Regex("""/\*[\s\S]*?\*/"""), "")
                val dto = json.decodeFromString<ManifestDto>(cleanedJson)
                dto.toManifest(script)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    @Serializable
    private data class ManifestDto(
        val id: String,
        val name: String,
        val version: String,
        val lang: String,
        val baseUrl: String,
        val type: String,
        val iconUrl: String? = null,
        val isNsfw: Boolean = false
    ) {
        fun toManifest(script: String): JsExtensionManifest = JsExtensionManifest(
            id = id,
            name = name,
            version = version,
            lang = lang,
            baseUrl = baseUrl,
            type = try { ExtensionType.valueOf(type.uppercase()) } catch (_: Exception) { ExtensionType.COMIC },
            iconUrl = iconUrl,
            isNsfw = isNsfw,
            script = script
        )
    }

    companion object {
        private const val KEY_INSTALLED_IDS = "dokuha_installed_ext_ids"
        private const val KEY_REPO_URLS = "dokuha_repo_urls"
    }
}
