package data.settings

import core.extensions.ExtensionRepoValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import ui.theme.ThemeMode

enum class DefaultReadingMode(val label: String) {
    WEBTOON("Webtoon"),
    RIGHT_TO_LEFT("Right to Left"),
    LEFT_TO_RIGHT("Left to Right"),
    VERTICAL("Vertical")
}

enum class NovelFont(val displayName: String) {
    SYSTEM("System Default"),
    SERIF("Serif"),
    SANS_SERIF("Sans-Serif"),
    MONOSPACE("Monospace")
}

enum class NovelReaderTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SEPIA("Sepia"),
    OLED("OLED")
}

enum class UpdateInterval(val label: String) {
    MANUAL("Never"),
    EVERY_12_HOURS("Every 12 hours"),
    DAILY("Daily"),
    EVERY_2_DAYS("Every 2 days"),
    WEEKLY("Weekly")
}

@Serializable
data class ExtensionRepo(
    val id: String,
    val name: String,
    val url: String,
    val isOfficial: Boolean = false,
    val fingerprint: String? = null
)

const val REPOSITORY_SECURITY_WARNING = "Extensions can run code on your device. Only add repositories you trust."

interface SettingsRepository {
    // Appearance
    val themeMode: StateFlow<ThemeMode>
    val accentColorIndex: StateFlow<Int>
    val reduceGlass: StateFlow<Boolean>
    val reduceMotion: StateFlow<Boolean>

    fun setThemeMode(mode: ThemeMode)
    fun setAccentColorIndex(index: Int)
    fun setReduceGlass(enabled: Boolean)
    fun setReduceMotion(enabled: Boolean)

    // Reader
    val defaultReadingMode: StateFlow<DefaultReadingMode>
    val sidePadding: StateFlow<Float>
    val brightness: StateFlow<Float>
    val cropBorders: StateFlow<Boolean>
    val keepScreenOn: StateFlow<Boolean>

    fun setDefaultReadingMode(mode: DefaultReadingMode)
    fun setSidePadding(padding: Float)
    fun setBrightness(brightness: Float)
    fun setCropBorders(enabled: Boolean)
    fun setKeepScreenOn(enabled: Boolean)

    // Novel Reader
    val novelFont: StateFlow<NovelFont>
    val novelFontSize: StateFlow<Float>
    val novelLineHeight: StateFlow<Float>
    val novelTheme: StateFlow<NovelReaderTheme>

    fun setNovelFont(font: NovelFont)
    fun setNovelFontSize(size: Float)
    fun setNovelLineHeight(lineHeight: Float)
    fun setNovelTheme(theme: NovelReaderTheme)

    // Library
    val defaultCategory: StateFlow<String>
    val showUnreadCount: StateFlow<Boolean>
    val updateOnAppLaunch: StateFlow<Boolean>
    val updateInterval: StateFlow<UpdateInterval>

    fun setDefaultCategory(category: String)
    fun setShowUnreadCount(enabled: Boolean)
    fun setUpdateOnAppLaunch(enabled: Boolean)
    fun setUpdateInterval(interval: UpdateInterval)

    // Downloads
    val downloadOnlyOnWifi: StateFlow<Boolean>
    val deleteAfterReading: StateFlow<Boolean>
    val concurrentDownloads: StateFlow<Int>
    val downloadLocation: StateFlow<String>

    fun setDownloadOnlyOnWifi(enabled: Boolean)
    fun setDeleteAfterReading(enabled: Boolean)
    fun setConcurrentDownloads(count: Int)
    fun setDownloadLocation(location: String)

    // Sources & Extensions
    val extensionRepos: StateFlow<List<ExtensionRepo>>
    val languageFilters: StateFlow<Set<String>>
    val showNsfw: StateFlow<Boolean>

    fun addExtensionRepo(name: String, url: String): Result<Unit>
    fun removeExtensionRepo(id: String)
    fun toggleLanguageFilter(lang: String)
    fun setShowNsfw(enabled: Boolean)

    // Backup & Restore
    val lastBackupTimestamp: StateFlow<String>

    fun createBackup(): String
    fun restoreBackup(uri: String): Result<Unit>

    // Storage
    val cacheSize: StateFlow<String>

    fun clearCache()
    fun clearReadingHistory()
}

class FakeSettingsRepository : SettingsRepository {
    // Appearance
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentColorIndex = MutableStateFlow(0)
    override val accentColorIndex: StateFlow<Int> = _accentColorIndex.asStateFlow()

    private val _reduceGlass = MutableStateFlow(false)
    override val reduceGlass: StateFlow<Boolean> = _reduceGlass.asStateFlow()

    private val _reduceMotion = MutableStateFlow(false)
    override val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    override fun setAccentColorIndex(index: Int) {
        _accentColorIndex.value = index.coerceIn(0, 4)
    }

    override fun setReduceGlass(enabled: Boolean) {
        _reduceGlass.value = enabled
    }

    override fun setReduceMotion(enabled: Boolean) {
        _reduceMotion.value = enabled
    }

    // Reader
    private val _defaultReadingMode = MutableStateFlow(DefaultReadingMode.WEBTOON)
    override val defaultReadingMode: StateFlow<DefaultReadingMode> = _defaultReadingMode.asStateFlow()

    private val _sidePadding = MutableStateFlow(0.12f)
    override val sidePadding: StateFlow<Float> = _sidePadding.asStateFlow()

    private val _brightness = MutableStateFlow(0.80f)
    override val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _cropBorders = MutableStateFlow(false)
    override val cropBorders: StateFlow<Boolean> = _cropBorders.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(true)
    override val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    override fun setDefaultReadingMode(mode: DefaultReadingMode) {
        _defaultReadingMode.value = mode
    }

    override fun setSidePadding(padding: Float) {
        _sidePadding.value = padding.coerceIn(0f, 0.4f)
    }

    override fun setBrightness(brightness: Float) {
        _brightness.value = brightness.coerceIn(0.1f, 1.0f)
    }

    override fun setCropBorders(enabled: Boolean) {
        _cropBorders.value = enabled
    }

    override fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
    }

    // Novel Reader
    private val _novelFont = MutableStateFlow(NovelFont.SYSTEM)
    override val novelFont: StateFlow<NovelFont> = _novelFont.asStateFlow()

    private val _novelFontSize = MutableStateFlow(16f)
    override val novelFontSize: StateFlow<Float> = _novelFontSize.asStateFlow()

    private val _novelLineHeight = MutableStateFlow(1.6f)
    override val novelLineHeight: StateFlow<Float> = _novelLineHeight.asStateFlow()

    private val _novelTheme = MutableStateFlow(NovelReaderTheme.DARK)
    override val novelTheme: StateFlow<NovelReaderTheme> = _novelTheme.asStateFlow()

    override fun setNovelFont(font: NovelFont) {
        _novelFont.value = font
    }

    override fun setNovelFontSize(size: Float) {
        _novelFontSize.value = size.coerceIn(12f, 28f)
    }

    override fun setNovelLineHeight(lineHeight: Float) {
        _novelLineHeight.value = lineHeight.coerceIn(1.2f, 2.2f)
    }

    override fun setNovelTheme(theme: NovelReaderTheme) {
        _novelTheme.value = theme
    }

    // Library
    private val _defaultCategory = MutableStateFlow("Default")
    override val defaultCategory: StateFlow<String> = _defaultCategory.asStateFlow()

    private val _showUnreadCount = MutableStateFlow(true)
    override val showUnreadCount: StateFlow<Boolean> = _showUnreadCount.asStateFlow()

    private val _updateOnAppLaunch = MutableStateFlow(true)
    override val updateOnAppLaunch: StateFlow<Boolean> = _updateOnAppLaunch.asStateFlow()

    private val _updateInterval = MutableStateFlow(UpdateInterval.DAILY)
    override val updateInterval: StateFlow<UpdateInterval> = _updateInterval.asStateFlow()

    override fun setDefaultCategory(category: String) {
        _defaultCategory.value = category
    }

    override fun setShowUnreadCount(enabled: Boolean) {
        _showUnreadCount.value = enabled
    }

    override fun setUpdateOnAppLaunch(enabled: Boolean) {
        _updateOnAppLaunch.value = enabled
    }

    override fun setUpdateInterval(interval: UpdateInterval) {
        _updateInterval.value = interval
    }

    // Downloads
    private val _downloadOnlyOnWifi = MutableStateFlow(true)
    override val downloadOnlyOnWifi: StateFlow<Boolean> = _downloadOnlyOnWifi.asStateFlow()

    private val _deleteAfterReading = MutableStateFlow(false)
    override val deleteAfterReading: StateFlow<Boolean> = _deleteAfterReading.asStateFlow()

    private val _concurrentDownloads = MutableStateFlow(2)
    override val concurrentDownloads: StateFlow<Int> = _concurrentDownloads.asStateFlow()

    private val _downloadLocation = MutableStateFlow("Internal Storage / Panel")
    override val downloadLocation: StateFlow<String> = _downloadLocation.asStateFlow()

    override fun setDownloadOnlyOnWifi(enabled: Boolean) {
        _downloadOnlyOnWifi.value = enabled
    }

    override fun setDeleteAfterReading(enabled: Boolean) {
        _deleteAfterReading.value = enabled
    }

    override fun setConcurrentDownloads(count: Int) {
        _concurrentDownloads.value = count.coerceIn(1, 3)
    }

    override fun setDownloadLocation(location: String) {
        _downloadLocation.value = location
    }

    // Sources & Extensions
    private val _extensionRepos = MutableStateFlow(
        listOf(
            ExtensionRepo(
                id = "repo-official",
                name = "Official Panel Repository",
                url = "https://raw.githubusercontent.com/panel-app/extensions/repo/index.min.json",
                isOfficial = true
            ),
            ExtensionRepo(
                id = "repo-keiyoushi",
                name = "Keiyoushi Extensions",
                url = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
                isOfficial = false
            )
        )
    )
    override val extensionRepos: StateFlow<List<ExtensionRepo>> = _extensionRepos.asStateFlow()

    private val _languageFilters = MutableStateFlow(setOf("en", "ja", "ko"))
    override val languageFilters: StateFlow<Set<String>> = _languageFilters.asStateFlow()

    private val _showNsfw = MutableStateFlow(false)
    override val showNsfw: StateFlow<Boolean> = _showNsfw.asStateFlow()

    private var repoCounter = 100

    override fun addExtensionRepo(name: String, url: String): Result<Unit> {
        val validation = ExtensionRepoValidator.validateAndNormalize(url)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid URL"))
        }

        val normalized = validation.normalizedUrl!!
        if (_extensionRepos.value.any { it.url.equals(normalized, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Repository has already been added"))
        }

        val cleanName = name.trim().ifEmpty { "Custom Repository" }
        repoCounter++
        val newRepo = ExtensionRepo(
            id = "repo-$repoCounter",
            name = cleanName,
            url = normalized,
            isOfficial = false,
            fingerprint = validation.fingerprint
        )
        _extensionRepos.value = _extensionRepos.value + newRepo
        return Result.success(Unit)
    }

    override fun removeExtensionRepo(id: String) {
        _extensionRepos.value = _extensionRepos.value.filterNot { it.id == id }
    }

    override fun toggleLanguageFilter(lang: String) {
        val current = _languageFilters.value
        _languageFilters.value = if (current.contains(lang)) {
            current - lang
        } else {
            current + lang
        }
    }

    override fun setShowNsfw(enabled: Boolean) {
        _showNsfw.value = enabled
    }

    // Backup & Restore
    private val _lastBackupTimestamp = MutableStateFlow("Yesterday at 11:42 PM")
    override val lastBackupTimestamp: StateFlow<String> = _lastBackupTimestamp.asStateFlow()

    override fun createBackup(): String {
        _lastBackupTimestamp.value = "Today at 01:25 AM"
        return "Backup created successfully"
    }

    override fun restoreBackup(uri: String): Result<Unit> {
        if (uri.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid backup file"))
        }
        _lastBackupTimestamp.value = "Restored just now"
        return Result.success(Unit)
    }

    // Storage
    private val _cacheSize = MutableStateFlow("324 MB")
    override val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    override fun clearCache() {
        _cacheSize.value = "0 MB"
    }

    override fun clearReadingHistory() {
        // In-memory fake clear history stub
    }
}
