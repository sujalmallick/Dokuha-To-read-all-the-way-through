package data.extension

import core.extensions.ExtensionRepoValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    fun addRepository(rawUrl: String, customName: String? = null): Result<ExtensionRepo>
    fun removeRepository(repoId: String): Boolean
    fun clearExtensionCache(extensionId: String)
}

class DefaultExtensionRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : ExtensionRepository {

    private val initialRepositories = listOf(
        ExtensionRepo(
            id = "repo-official",
            name = "Official Extensions",
            url = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo",
            fingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo"),
            isOfficial = true,
            extensionCount = 5
        ),
        ExtensionRepo(
            id = "repo-keiyoushi",
            name = "Keiyoushi Community",
            url = "https://raw.githubusercontent.com/keiyoushi/extensions/repo",
            fingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/keiyoushi/extensions/repo"),
            isOfficial = false,
            extensionCount = 4
        )
    )

    private val initialExtensions = listOf(
        ExtensionItem(
            id = "ext-mangadex",
            name = "MangaDex",
            pkgName = "eu.kanade.tachiyomi.extension.all.mangadex",
            versionName = "1.4.32",
            versionCode = 32,
            lang = "ALL",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.OFFICIAL,
            repoUrl = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo",
            status = ExtensionStatus.INSTALLED,
            iconColorHex = 0xFFFF6740,
            description = "Community manga platform with high-resolution scans, multi-language scanlations, and chapter discussion integration.",
            sources = listOf(
                ExtensionSourceInfo("src-mdx-en", "MangaDex (EN)", "https://mangadex.org", listOf("EN"), "Manga"),
                ExtensionSourceInfo("src-mdx-ja", "MangaDex (JA)", "https://mangadex.org", listOf("JA"), "Manga")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo"),
            networkDomains = listOf("api.mangadex.org", "mangadex.org", "uploads.mangadex.org"),
            cachedBytes = 48 * 1024 * 1024L
        ),
        ExtensionItem(
            id = "ext-comick",
            name = "Comick",
            pkgName = "eu.kanade.tachiyomi.extension.all.comick",
            versionName = "1.3.1",
            versionCode = 13,
            lang = "ALL",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.OFFICIAL,
            repoUrl = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo",
            status = ExtensionStatus.UPDATE_AVAILABLE,
            updateVersion = "1.3.5",
            iconColorHex = 0xFF60A5FA,
            description = "Modern comic & manga aggregator featuring responsive chapter images and fast multi-language scan group releases.",
            sources = listOf(
                ExtensionSourceInfo("src-cmk-all", "Comick", "https://comick.io", listOf("EN", "KO", "JA"), "Manga / Manhwa")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo"),
            networkDomains = listOf("api.comick.io", "comick.io"),
            cachedBytes = 24 * 1024 * 1024L
        ),
        ExtensionItem(
            id = "ext-asurascans",
            name = "Asura Scans",
            pkgName = "eu.kanade.tachiyomi.extension.en.asurascans",
            versionName = "1.2.0",
            versionCode = 20,
            lang = "EN",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.OFFICIAL,
            repoUrl = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo",
            status = ExtensionStatus.INSTALLED,
            iconColorHex = 0xFFA855F7,
            description = "Premier translation team specializing in high-action Korean manhwa, martial arts, system apocalypse, and fantasy webtoons.",
            sources = listOf(
                ExtensionSourceInfo("src-asura", "Asura Scans", "https://asurascans.com", listOf("EN"), "Manhwa")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo"),
            networkDomains = listOf("asurascans.com", "img.asurascans.com"),
            cachedBytes = 32 * 1024 * 1024L
        ),
        ExtensionItem(
            id = "ext-webnovel",
            name = "WebNovel",
            pkgName = "eu.kanade.tachiyomi.extension.en.webnovel",
            versionName = "1.1.9",
            versionCode = 19,
            lang = "EN",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.COMMUNITY,
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo",
            status = ExtensionStatus.AVAILABLE,
            iconColorHex = 0xFFEC4899,
            description = "Official and community serialized webnovels and authorized comic adaptations with millions of original stories.",
            sources = listOf(
                ExtensionSourceInfo("src-webnovel", "WebNovel", "https://webnovel.com", listOf("EN"), "Webnovel")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/keiyoushi/extensions/repo"),
            networkDomains = listOf("webnovel.com", "m.webnovel.com"),
            cachedBytes = 0L
        ),
        ExtensionItem(
            id = "ext-flamecomics",
            name = "Flame Comics",
            pkgName = "eu.kanade.tachiyomi.extension.en.flamecomics",
            versionName = "1.0.8",
            versionCode = 8,
            lang = "EN",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.COMMUNITY,
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo",
            status = ExtensionStatus.AVAILABLE,
            iconColorHex = 0xFFF97316,
            description = "Dedicated action manhwa translation group with regular weekly releases and crisp digital typesetting.",
            sources = listOf(
                ExtensionSourceInfo("src-flame", "Flame Comics", "https://flamecomics.xyz", listOf("EN"), "Manhwa")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/keiyoushi/extensions/repo"),
            networkDomains = listOf("flamecomics.xyz"),
            cachedBytes = 0L
        ),
        ExtensionItem(
            id = "ext-rawkuma",
            name = "RawKuma",
            pkgName = "eu.kanade.tachiyomi.extension.ja.rawkuma",
            versionName = "2.0.1",
            versionCode = 21,
            lang = "JA",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.COMMUNITY,
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo",
            status = ExtensionStatus.AVAILABLE,
            iconColorHex = 0xFF10B981,
            description = "Original Japanese untranslated manga scans directly from Japanese magazine serialization.",
            sources = listOf(
                ExtensionSourceInfo("src-rawkuma", "RawKuma", "https://rawkuma.net", listOf("JA"), "Manga")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/keiyoushi/extensions/repo"),
            networkDomains = listOf("rawkuma.net"),
            cachedBytes = 0L
        ),
        ExtensionItem(
            id = "ext-naver",
            name = "Naver Webtoon",
            pkgName = "eu.kanade.tachiyomi.extension.ko.naver",
            versionName = "3.1.0",
            versionCode = 31,
            lang = "KO",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.OFFICIAL,
            repoUrl = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo",
            status = ExtensionStatus.AVAILABLE,
            iconColorHex = 0xFF00DC64,
            description = "Official Korean digital webtoon publishing platform from NAVER Corp featuring world-renowned manhwa.",
            sources = listOf(
                ExtensionSourceInfo("src-naver", "Naver Webtoon", "https://comic.naver.com", listOf("KO"), "Manhwa")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo"),
            networkDomains = listOf("comic.naver.com", "image-comic.pstatic.net"),
            cachedBytes = 0L
        ),
        ExtensionItem(
            id = "ext-tumanga",
            name = "TuMangaOnline",
            pkgName = "eu.kanade.tachiyomi.extension.es.tumangaonline",
            versionName = "1.2.4",
            versionCode = 24,
            lang = "ES",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.COMMUNITY,
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo",
            status = ExtensionStatus.AVAILABLE,
            iconColorHex = 0xFFEF4444,
            description = "The largest Spanish manga and manhwa community portal with thousands of scanlated series.",
            sources = listOf(
                ExtensionSourceInfo("src-tmo", "TuMangaOnline", "https://lectortmo.com", listOf("ES"), "Manga / Manhwa")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/keiyoushi/extensions/repo"),
            networkDomains = listOf("lectortmo.com"),
            cachedBytes = 0L
        ),
        ExtensionItem(
            id = "ext-scanfr",
            name = "Scan-FR",
            pkgName = "eu.kanade.tachiyomi.extension.fr.scanfr",
            versionName = "1.0.5",
            versionCode = 5,
            lang = "FR",
            isNsfw = false,
            trustLevel = ExtensionTrustLevel.COMMUNITY,
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo",
            status = ExtensionStatus.AVAILABLE,
            iconColorHex = 0xFF3B82F6,
            description = "Popular French scanlation community and release hub for popular manga and manhwa.",
            sources = listOf(
                ExtensionSourceInfo("src-scanfr", "Scan-FR", "https://scan-fr.org", listOf("FR"), "Manga")
            ),
            repoFingerprint = ExtensionRepoValidator.computeFingerprint("https://raw.githubusercontent.com/keiyoushi/extensions/repo"),
            networkDomains = listOf("scan-fr.org"),
            cachedBytes = 0L
        )
    )

    private val _extensions = MutableStateFlow<List<ExtensionItem>>(initialExtensions)
    override val extensions: StateFlow<List<ExtensionItem>> = _extensions.asStateFlow()

    private val _repositories = MutableStateFlow<List<ExtensionRepo>>(initialRepositories)
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
}
