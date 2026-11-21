package core.extensions

import core.extensions.js.*
import core.extensions.lnreader.LNReaderManifest
import core.extensions.lnreader.LNReaderSourceAdapter
import core.extensions.mangayomi.MangayomiManifest
import core.extensions.mangayomi.MangayomiSourceAdapter
import core.extensions.model.ExtensionEcosystem
import core.extensions.paperback.PaperbackManifest
import core.extensions.paperback.PaperbackSourceAdapter
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
 * Manages the lifecycle of extensions across all supported ecosystems (Mangayomi, LNReader,
 * Paperback, and Dokuha native): indexing repositories, downloading scripts, validating SHA-256
 * checksums, persisting bundles, pooled sandbox activation, and registering dynamic sources into PanelRepository.
 */
class ExtensionManager(
    private val storage: KeyValueStorage,
    private val panelRepository: PanelRepository? = null,
    private val httpClient: HttpClient? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val antiBotResolver: AntiBotResolver? = null,
    private val enginePool: JsEnginePool = JsEnginePool(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val autoSeedDefaults: Boolean = false
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val catalogParser = ExtensionCatalogParser(json)
    private val bridge = JsBridge(httpClient = httpClient, antiBotResolver = antiBotResolver)

    private val _installedExtensions = MutableStateFlow<List<JsExtensionManifest>>(emptyList())
    val installedExtensions: StateFlow<List<JsExtensionManifest>> = _installedExtensions.asStateFlow()

    private val _availableExtensions = MutableStateFlow<List<ExtensionCatalogItem>>(emptyList())
    val availableExtensions: StateFlow<List<ExtensionCatalogItem>> = _availableExtensions.asStateFlow()

    /**
     * Maps the deduplication key ("name::lang") to all catalog variants available for that
     * extension across all configured repositories. Only populated when more than one repo
     * provides the same extension, so the UI can offer a repo-picker instead of silently
     * choosing the highest-version winner.
     */
    private val _extensionVariants = MutableStateFlow<Map<String, List<ExtensionCatalogItem>>>(emptyMap())
    val extensionVariants: StateFlow<Map<String, List<ExtensionCatalogItem>>> = _extensionVariants.asStateFlow()

    private val _repoUrls = MutableStateFlow<List<String>>(emptyList())
    val repoUrls: StateFlow<List<String>> = _repoUrls.asStateFlow()

    private val activeSources = mutableMapOf<String, ContentSource>()

    init {
        loadRepositories()
        restoreInstalledExtensions()
        if (autoSeedDefaults) {
            seedDefaultsIfEmpty()
        }
        settingsRepository?.let { settings ->
            coroutineScope.launch {
                settings.extensionRepos.collect { repos ->
                    val urls = repos.map { it.url }
                    if (urls != _repoUrls.value) {
                        _repoUrls.value = urls
                        saveRepositories(urls)
                    }
                    refreshCatalog()
                }
            }
        }
        coroutineScope.launch {
            refreshCatalog()
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
     * Refreshes the extension catalog from all configured repository URLs across all ecosystems.
     */
    suspend fun refreshCatalog() {
        val client = httpClient
        val items = mutableListOf<ExtensionCatalogItem>()

        if (client != null) {
            val candidateEndpoints = listOf(
                "index.min.json",
                "index.json",
                "novel_index.json",
                "plugins.min.json",
                "versioning.json"
            )

            for (repoUrl in _repoUrls.value) {
                val endpoints = if (repoUrl.endsWith(".json")) {
                    listOf("", "index.json", "novel_index.json", "plugins.min.json", "versioning.json")
                } else {
                    candidateEndpoints
                }

                for (endpoint in endpoints) {
                    try {
                        val fullUrl = if (endpoint.isEmpty()) repoUrl else "${repoUrl.trimEnd('/')}/$endpoint"
                        val response = client.get(fullUrl)
                        if (response.status.value in 200..299) {
                            val body = response.bodyAsText()
                            if (body.isNotBlank()) {
                                val parsed = catalogParser.parseCatalog(body, repoUrl)
                                if (parsed.isNotEmpty()) {
                                    println("[ExtensionManager] Discovered ${parsed.size} extensions from $fullUrl")
                                    items.addAll(parsed)
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Skip unreachable endpoint or 404
                    }
                }
            }
        }

        // Deduplicate by id first
        val deduplicatedById = items.distinctBy { it.id }

        // Build variants map: group ALL repo variants by name::lang so the UI can offer a
        // repo-picker when multiple repos provide the same extension name and language.
        val variantsByKey = deduplicatedById
            .groupBy { "${it.name.lowercase().trim()}::${it.lang.lowercase().trim()}" }
            .filter { (_, group) -> group.map { it.repoBaseUrl }.distinct().size > 1 }
        _extensionVariants.value = variantsByKey

        // Collapse to the highest-version winner per name::lang::ecosystem for the main catalog
        val allItems = deduplicatedById
            .groupBy { "${it.name.lowercase().trim()}::${it.lang.lowercase().trim()}::${it.ecosystem}" }
            .map { (_, group) -> group.maxByOrNull { it.version } ?: group.first() }
        _availableExtensions.value = allItems

        // Auto-update installed extensions when a newer version exists in the refreshed catalog
        for (installed in _installedExtensions.value) {
            val catalogMatch = allItems.firstOrNull { it.id == installed.id }
            if (catalogMatch != null && isNewerVersion(catalogMatch.version, installed.version)) {
                val repoBase = catalogMatch.repoBaseUrl.ifBlank {
                    _repoUrls.value.firstOrNull() ?: ""
                }
                coroutineScope.launch {
                    println("[ExtensionManager] Auto-updating ${installed.name} from v${installed.version} to v${catalogMatch.version}")
                    installExtension(catalogMatch, repoBase)
                }
            }
        }
    }

    private fun isNewerVersion(newVer: String, currentVer: String): Boolean {
        val newParts = newVer.split('.').mapNotNull { it.toIntOrNull() }
        val curParts = currentVer.split('.').mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(newParts.size, curParts.size)
        for (i in 0 until maxLen) {
            val n = newParts.getOrElse(i) { 0 }
            val c = curParts.getOrElse(i) { 0 }
            if (n > c) return true
            if (n < c) return false
        }
        return false
    }

    /**
     * Downloads, validates, and installs an extension from a repository.
     */
    suspend fun installExtension(catalogItem: ExtensionCatalogItem, repoBaseUrl: String): Boolean {
        if (catalogItem.pkg.startsWith("demo://") || catalogItem.pkg.isBlank()) {
            return false
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

            val manifest = JsExtensionManifest(
                id = catalogItem.id,
                name = catalogItem.name,
                version = catalogItem.version,
                lang = catalogItem.lang,
                baseUrl = catalogItem.baseUrl,
                type = catalogItem.type,
                iconUrl = catalogItem.iconUrl,
                isNsfw = catalogItem.isNsfw,
                script = script,
                ecosystem = catalogItem.ecosystem
            )

            saveManifest(manifest)
            registerAndActivateSource(manifest)

            val updated = _installedExtensions.value.filter { it.id != manifest.id }.toMutableList()
            updated.add(manifest)
            _installedExtensions.value = updated

            true
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
            if (source is AutoCloseable) {
                try {
                    source.close()
                } catch (_: Exception) {}
            }
            coroutineScope.launch {
                enginePool.evict(source.id)
            }
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
            when (manifest.ecosystem) {
                ExtensionEcosystem.MANGAYOMI -> {
                    val numericId = manifest.id.toLongOrNull() ?: ((manifest.id.hashCode().toLong() and 0x7FFFFFFF) + 10_000L)
                    val mManifest = MangayomiManifest(
                        id = numericId,
                        name = manifest.name,
                        baseUrl = manifest.baseUrl,
                        lang = manifest.lang,
                        version = manifest.version,
                        isNovel = manifest.type == ExtensionType.NOVEL,
                        isNsfw = manifest.isNsfw,
                        script = manifest.script,
                        iconUrl = manifest.iconUrl
                    )
                    val adapter = MangayomiSourceAdapter(mManifest, enginePool, bridge)
                    activeSources[manifest.id] = adapter
                    panelRepository?.registerSource(adapter)
                }
                ExtensionEcosystem.LNREADER -> {
                    val lnManifest = LNReaderManifest(
                        id = manifest.id,
                        name = manifest.name,
                        site = manifest.baseUrl,
                        lang = manifest.lang,
                        version = manifest.version,
                        script = manifest.script,
                        iconUrl = manifest.iconUrl
                    )
                    val adapter = LNReaderSourceAdapter(lnManifest, enginePool, bridge)
                    activeSources[manifest.id] = adapter
                    panelRepository?.registerSource(adapter)
                }
                ExtensionEcosystem.PAPERBACK -> {
                    val pbManifest = PaperbackManifest(
                        id = manifest.id,
                        name = manifest.name,
                        websiteBaseURL = manifest.baseUrl,
                        language = manifest.lang,
                        version = manifest.version,
                        contentRating = if (manifest.isNsfw) "ADULT" else "EVERYONE",
                        script = manifest.script,
                        iconUrl = manifest.iconUrl
                    )
                    val isV9 = !manifest.script.contains("Sources") && !manifest.script.contains("window.Sources")
                    val adapter = PaperbackSourceAdapter(pbManifest, enginePool, bridge, isV9 = isV9)
                    activeSources[manifest.id] = adapter
                    panelRepository?.registerSource(adapter)
                }
                ExtensionEcosystem.CUSTOM -> {
                    // Auto-detect based on script tokens
                    when {
                        manifest.script.contains("DefaultExtension") || manifest.script.contains("MProvider") -> {
                            val numericId = manifest.id.toLongOrNull() ?: ((manifest.id.hashCode().toLong() and 0x7FFFFFFF) + 10_000L)
                            val mManifest = MangayomiManifest(
                                id = numericId,
                                name = manifest.name,
                                baseUrl = manifest.baseUrl,
                                lang = manifest.lang,
                                version = manifest.version,
                                isNovel = manifest.type == ExtensionType.NOVEL,
                                isNsfw = manifest.isNsfw,
                                script = manifest.script,
                                iconUrl = manifest.iconUrl
                            )
                            val adapter = MangayomiSourceAdapter(mManifest, enginePool, bridge)
                            activeSources[manifest.id] = adapter
                            panelRepository?.registerSource(adapter)
                        }
                        manifest.script.contains("module.exports") -> {
                            val lnManifest = LNReaderManifest(
                                id = manifest.id,
                                name = manifest.name,
                                site = manifest.baseUrl,
                                lang = manifest.lang,
                                version = manifest.version,
                                script = manifest.script,
                                iconUrl = manifest.iconUrl
                            )
                            val adapter = LNReaderSourceAdapter(lnManifest, enginePool, bridge)
                            activeSources[manifest.id] = adapter
                            panelRepository?.registerSource(adapter)
                        }
                        manifest.script.contains("globalThis.Sources") || manifest.script.contains("Sources[") -> {
                            val pbManifest = PaperbackManifest(
                                id = manifest.id,
                                name = manifest.name,
                                websiteBaseURL = manifest.baseUrl,
                                language = manifest.lang,
                                version = manifest.version,
                                contentRating = if (manifest.isNsfw) "ADULT" else "EVERYONE",
                                script = manifest.script,
                                iconUrl = manifest.iconUrl
                            )
                            val adapter = PaperbackSourceAdapter(pbManifest, enginePool, bridge, isV9 = false)
                            activeSources[manifest.id] = adapter
                            panelRepository?.registerSource(adapter)
                        }
                        else -> {
                            val engine = JsEngine()
                            val source = JsExtensionSource(manifest = manifest, engine = engine, bridge = bridge)
                            source.initialize()
                            activeSources[manifest.id] = source
                            panelRepository?.registerSource(source)
                        }
                    }
                }
            }
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
            json.decodeFromString<List<String>>(reposJson).toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
        _repoUrls.value = repos
    }

    fun seedDefaultsIfEmpty() {
        if (_repoUrls.value.isEmpty()) {
            val defaultRepos = listOf(
                "https://raw.githubusercontent.com/RitwikGupta-0501/dokuha-extensions/main",
                "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
            )
            val current = _repoUrls.value.toMutableList()
            for (repo in defaultRepos) {
                if (!current.contains(repo)) current.add(repo)
            }
            _repoUrls.value = current
            saveRepositories(current)
            coroutineScope.launch {
                refreshCatalog()
            }
        }
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
        val isNsfw: Boolean = false,
        val ecosystem: ExtensionEcosystem = ExtensionEcosystem.CUSTOM
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
            script = script,
            ecosystem = ecosystem
        )
    }

    companion object {
        private const val KEY_INSTALLED_IDS = "dokuha_installed_ext_ids"
        private const val KEY_REPO_URLS = "dokuha_repo_urls"
    }
}
