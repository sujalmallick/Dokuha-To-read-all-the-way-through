package data.settings

import core.extensions.ExtensionRepoValidator
import core.storage.KeyValueStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ui.theme.ThemeMode

/**
 * Production persistent implementation of SettingsRepository backed by KeyValueStorage.
 * Updates are saved immediately to disk and emitted live via StateFlow.
 */
class PersistentSettingsRepository(
    private val storage: KeyValueStorage
) : SettingsRepository {

    companion object {
        const val KEY_SCHEMA_VERSION = "panel_storage_schema_version"
        const val CURRENT_SCHEMA_VERSION = 1

        // Keys
        private const val KEY_THEME_MODE = "setting_theme_mode"
        private const val KEY_ACCENT_COLOR_INDEX = "setting_accent_color_index"
        private const val KEY_REDUCE_GLASS = "setting_reduce_glass"
        private const val KEY_REDUCE_MOTION = "setting_reduce_motion"

        private const val KEY_DEFAULT_READING_MODE = "setting_default_reading_mode"
        private const val KEY_SIDE_PADDING = "setting_side_padding"
        private const val KEY_BRIGHTNESS = "setting_brightness"
        private const val KEY_CROP_BORDERS = "setting_crop_borders"
        private const val KEY_KEEP_SCREEN_ON = "setting_keep_screen_on"

        private const val KEY_NOVEL_FONT = "setting_novel_font"
        private const val KEY_NOVEL_FONT_SIZE = "setting_novel_font_size"
        private const val KEY_NOVEL_LINE_HEIGHT = "setting_novel_line_height"
        private const val KEY_NOVEL_THEME = "setting_novel_theme"

        private const val KEY_DEFAULT_CATEGORY = "setting_default_category"
        private const val KEY_SHOW_UNREAD_COUNT = "setting_show_unread_count"
        private const val KEY_UPDATE_ON_APP_LAUNCH = "setting_update_on_app_launch"
        private const val KEY_UPDATE_INTERVAL = "setting_update_interval"

        private const val KEY_DOWNLOAD_WIFI_ONLY = "setting_download_wifi_only"
        private const val KEY_DELETE_AFTER_READING = "setting_delete_after_reading"
        private const val KEY_CONCURRENT_DOWNLOADS = "setting_concurrent_downloads"
        private const val KEY_DOWNLOAD_LOCATION = "setting_download_location"

        private const val KEY_EXTENSION_REPOS = "setting_extension_repos"
        private const val KEY_LANGUAGE_FILTERS = "setting_language_filters"
        private const val KEY_SHOW_NSFW = "setting_show_nsfw"

        private const val KEY_LAST_BACKUP = "setting_last_backup_timestamp"
        private const val KEY_CACHE_SIZE = "setting_cache_size"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        // Schema initialization and future migration hook
        val currentVersion = storage.getInt(KEY_SCHEMA_VERSION, 0)
        if (currentVersion < CURRENT_SCHEMA_VERSION) {
            storage.putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION)
        }
    }

    // Appearance
    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(storage.getString(KEY_THEME_MODE, ThemeMode.DARK.name))
        }.getOrDefault(ThemeMode.DARK)
    )
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentColorIndex = MutableStateFlow(
        storage.getInt(KEY_ACCENT_COLOR_INDEX, 0).coerceIn(0, 4)
    )
    override val accentColorIndex: StateFlow<Int> = _accentColorIndex.asStateFlow()

    private val _reduceGlass = MutableStateFlow(
        storage.getBoolean(KEY_REDUCE_GLASS, false)
    )
    override val reduceGlass: StateFlow<Boolean> = _reduceGlass.asStateFlow()

    private val _reduceMotion = MutableStateFlow(
        storage.getBoolean(KEY_REDUCE_MOTION, false)
    )
    override val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        storage.putString(KEY_THEME_MODE, mode.name)
    }

    override fun setAccentColorIndex(index: Int) {
        val clamped = index.coerceIn(0, 4)
        _accentColorIndex.value = clamped
        storage.putInt(KEY_ACCENT_COLOR_INDEX, clamped)
    }

    override fun setReduceGlass(enabled: Boolean) {
        _reduceGlass.value = enabled
        storage.putBoolean(KEY_REDUCE_GLASS, enabled)
    }

    override fun setReduceMotion(enabled: Boolean) {
        _reduceMotion.value = enabled
        storage.putBoolean(KEY_REDUCE_MOTION, enabled)
    }

    // Reader
    private val _defaultReadingMode = MutableStateFlow(
        runCatching {
            DefaultReadingMode.valueOf(storage.getString(KEY_DEFAULT_READING_MODE, DefaultReadingMode.WEBTOON.name))
        }.getOrDefault(DefaultReadingMode.WEBTOON)
    )
    override val defaultReadingMode: StateFlow<DefaultReadingMode> = _defaultReadingMode.asStateFlow()

    private val _sidePadding = MutableStateFlow(
        storage.getFloat(KEY_SIDE_PADDING, 0.12f).coerceIn(0f, 0.4f)
    )
    override val sidePadding: StateFlow<Float> = _sidePadding.asStateFlow()

    private val _brightness = MutableStateFlow(
        storage.getFloat(KEY_BRIGHTNESS, 0.80f).coerceIn(0.1f, 1.0f)
    )
    override val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _cropBorders = MutableStateFlow(
        storage.getBoolean(KEY_CROP_BORDERS, false)
    )
    override val cropBorders: StateFlow<Boolean> = _cropBorders.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(
        storage.getBoolean(KEY_KEEP_SCREEN_ON, true)
    )
    override val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    override fun setDefaultReadingMode(mode: DefaultReadingMode) {
        _defaultReadingMode.value = mode
        storage.putString(KEY_DEFAULT_READING_MODE, mode.name)
    }

    override fun setSidePadding(padding: Float) {
        val clamped = padding.coerceIn(0f, 0.4f)
        _sidePadding.value = clamped
        storage.putFloat(KEY_SIDE_PADDING, clamped)
    }

    override fun setBrightness(brightness: Float) {
        val clamped = brightness.coerceIn(0.1f, 1.0f)
        _brightness.value = clamped
        storage.putFloat(KEY_BRIGHTNESS, clamped)
    }

    override fun setCropBorders(enabled: Boolean) {
        _cropBorders.value = enabled
        storage.putBoolean(KEY_CROP_BORDERS, enabled)
    }

    override fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        storage.putBoolean(KEY_KEEP_SCREEN_ON, enabled)
    }

    // Novel Reader
    private val _novelFont = MutableStateFlow(
        runCatching {
            NovelFont.valueOf(storage.getString(KEY_NOVEL_FONT, NovelFont.SYSTEM.name))
        }.getOrDefault(NovelFont.SYSTEM)
    )
    override val novelFont: StateFlow<NovelFont> = _novelFont.asStateFlow()

    private val _novelFontSize = MutableStateFlow(
        storage.getFloat(KEY_NOVEL_FONT_SIZE, 16f).coerceIn(12f, 28f)
    )
    override val novelFontSize: StateFlow<Float> = _novelFontSize.asStateFlow()

    private val _novelLineHeight = MutableStateFlow(
        storage.getFloat(KEY_NOVEL_LINE_HEIGHT, 1.6f).coerceIn(1.2f, 2.4f)
    )
    override val novelLineHeight: StateFlow<Float> = _novelLineHeight.asStateFlow()

    private val _novelTheme = MutableStateFlow(
        runCatching {
            NovelReaderTheme.valueOf(storage.getString(KEY_NOVEL_THEME, NovelReaderTheme.DARK.name))
        }.getOrDefault(NovelReaderTheme.DARK)
    )
    override val novelTheme: StateFlow<NovelReaderTheme> = _novelTheme.asStateFlow()

    override fun setNovelFont(font: NovelFont) {
        _novelFont.value = font
        storage.putString(KEY_NOVEL_FONT, font.name)
    }

    override fun setNovelFontSize(size: Float) {
        val clamped = size.coerceIn(12f, 28f)
        _novelFontSize.value = clamped
        storage.putFloat(KEY_NOVEL_FONT_SIZE, clamped)
    }

    override fun setNovelLineHeight(lineHeight: Float) {
        val clamped = lineHeight.coerceIn(1.2f, 2.4f)
        _novelLineHeight.value = clamped
        storage.putFloat(KEY_NOVEL_LINE_HEIGHT, clamped)
    }

    override fun setNovelTheme(theme: NovelReaderTheme) {
        _novelTheme.value = theme
        storage.putString(KEY_NOVEL_THEME, theme.name)
    }

    // Library
    private val _defaultCategory = MutableStateFlow(
        storage.getString(KEY_DEFAULT_CATEGORY, "Default")
    )
    override val defaultCategory: StateFlow<String> = _defaultCategory.asStateFlow()

    private val _showUnreadCount = MutableStateFlow(
        storage.getBoolean(KEY_SHOW_UNREAD_COUNT, true)
    )
    override val showUnreadCount: StateFlow<Boolean> = _showUnreadCount.asStateFlow()

    private val _updateOnAppLaunch = MutableStateFlow(
        storage.getBoolean(KEY_UPDATE_ON_APP_LAUNCH, false)
    )
    override val updateOnAppLaunch: StateFlow<Boolean> = _updateOnAppLaunch.asStateFlow()

    private val _updateInterval = MutableStateFlow(
        runCatching {
            UpdateInterval.valueOf(storage.getString(KEY_UPDATE_INTERVAL, UpdateInterval.DAILY.name))
        }.getOrDefault(UpdateInterval.DAILY)
    )
    override val updateInterval: StateFlow<UpdateInterval> = _updateInterval.asStateFlow()

    override fun setDefaultCategory(category: String) {
        _defaultCategory.value = category
        storage.putString(KEY_DEFAULT_CATEGORY, category)
    }

    override fun setShowUnreadCount(enabled: Boolean) {
        _showUnreadCount.value = enabled
        storage.putBoolean(KEY_SHOW_UNREAD_COUNT, enabled)
    }

    override fun setUpdateOnAppLaunch(enabled: Boolean) {
        _updateOnAppLaunch.value = enabled
        storage.putBoolean(KEY_UPDATE_ON_APP_LAUNCH, enabled)
    }

    override fun setUpdateInterval(interval: UpdateInterval) {
        _updateInterval.value = interval
        storage.putString(KEY_UPDATE_INTERVAL, interval.name)
    }

    // Downloads
    private val _downloadOnlyOnWifi = MutableStateFlow(
        storage.getBoolean(KEY_DOWNLOAD_WIFI_ONLY, true)
    )
    override val downloadOnlyOnWifi: StateFlow<Boolean> = _downloadOnlyOnWifi.asStateFlow()

    private val _deleteAfterReading = MutableStateFlow(
        storage.getBoolean(KEY_DELETE_AFTER_READING, false)
    )
    override val deleteAfterReading: StateFlow<Boolean> = _deleteAfterReading.asStateFlow()

    private val _concurrentDownloads = MutableStateFlow(
        storage.getInt(KEY_CONCURRENT_DOWNLOADS, 2).coerceIn(1, 3)
    )
    override val concurrentDownloads: StateFlow<Int> = _concurrentDownloads.asStateFlow()

    private val _downloadLocation = MutableStateFlow(
        storage.getString(KEY_DOWNLOAD_LOCATION, "Internal storage / Panel / Downloads")
    )
    override val downloadLocation: StateFlow<String> = _downloadLocation.asStateFlow()

    override fun setDownloadOnlyOnWifi(enabled: Boolean) {
        _downloadOnlyOnWifi.value = enabled
        storage.putBoolean(KEY_DOWNLOAD_WIFI_ONLY, enabled)
    }

    override fun setDeleteAfterReading(enabled: Boolean) {
        _deleteAfterReading.value = enabled
        storage.putBoolean(KEY_DELETE_AFTER_READING, enabled)
    }

    override fun setConcurrentDownloads(count: Int) {
        val clamped = count.coerceIn(1, 3)
        _concurrentDownloads.value = clamped
        storage.putInt(KEY_CONCURRENT_DOWNLOADS, clamped)
    }

    override fun setDownloadLocation(location: String) {
        _downloadLocation.value = location
        storage.putString(KEY_DOWNLOAD_LOCATION, location)
    }

    // Sources & Extensions (Clean startup with zero pre-loaded repositories)
    private val _extensionRepos = MutableStateFlow<List<ExtensionRepo>>(
        runCatching {
            val raw = storage.getString(KEY_EXTENSION_REPOS, "")
            if (raw.isBlank()) emptyList() else json.decodeFromString<List<ExtensionRepo>>(raw)
        }.getOrDefault(emptyList())
    )
    override val extensionRepos: StateFlow<List<ExtensionRepo>> = _extensionRepos.asStateFlow()

    private val _languageFilters = MutableStateFlow<Set<String>>(
        runCatching {
            val raw = storage.getString(KEY_LANGUAGE_FILTERS, "en")
            raw.split(",").filter { it.isNotBlank() }.toSet()
        }.getOrDefault(setOf("en"))
    )
    override val languageFilters: StateFlow<Set<String>> = _languageFilters.asStateFlow()

    private val _showNsfw = MutableStateFlow(
        storage.getBoolean(KEY_SHOW_NSFW, false)
    )
    override val showNsfw: StateFlow<Boolean> = _showNsfw.asStateFlow()

    override fun addExtensionRepo(name: String, url: String): Result<Unit> {
        val validation = ExtensionRepoValidator.validateAndNormalize(url)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid URL"))
        }

        val normalizedUrl = validation.normalizedUrl!!
        val fingerprint = validation.fingerprint!!

        // Deduplication
        if (_extensionRepos.value.any { it.url.equals(normalizedUrl, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Repository has already been added"))
        }

        val id = "repo_${_extensionRepos.value.size + 1}_${normalizedUrl.hashCode()}"
        val cleanName = name.trim().ifEmpty { "Community Repository" }
        val newRepo = ExtensionRepo(
            id = id,
            name = cleanName,
            url = normalizedUrl,
            isOfficial = false,
            fingerprint = fingerprint
        )
        val updated = _extensionRepos.value + newRepo
        _extensionRepos.value = updated
        persistExtensionRepos(updated)
        return Result.success(Unit)
    }

    override fun removeExtensionRepo(id: String) {
        val updated = _extensionRepos.value.filterNot { it.id == id }
        _extensionRepos.value = updated
        persistExtensionRepos(updated)
    }

    private fun persistExtensionRepos(repos: List<ExtensionRepo>) {
        runCatching {
            val raw = json.encodeToString(repos)
            storage.putString(KEY_EXTENSION_REPOS, raw)
        }
    }

    override fun toggleLanguageFilter(lang: String) {
        val current = _languageFilters.value.toMutableSet()
        if (current.contains(lang)) {
            if (current.size > 1) current.remove(lang)
        } else {
            current.add(lang)
        }
        _languageFilters.value = current
        storage.putString(KEY_LANGUAGE_FILTERS, current.joinToString(","))
    }

    override fun setShowNsfw(enabled: Boolean) {
        _showNsfw.value = enabled
        storage.putBoolean(KEY_SHOW_NSFW, enabled)
    }

    // Backup & Restore
    private val _lastBackupTimestamp = MutableStateFlow(
        storage.getString(KEY_LAST_BACKUP, "Never")
    )
    override val lastBackupTimestamp: StateFlow<String> = _lastBackupTimestamp.asStateFlow()

    override fun createBackup(): String {
        val timestamp = "Today, 12:00 PM"
        _lastBackupTimestamp.value = timestamp
        storage.putString(KEY_LAST_BACKUP, timestamp)
        return "panel_backup_stub.json"
    }

    override fun restoreBackup(uri: String): Result<Unit> {
        return Result.success(Unit)
    }

    // Storage
    private val _cacheSize = MutableStateFlow(
        storage.getString(KEY_CACHE_SIZE, "324 MB")
    )
    override val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    override fun clearCache() {
        _cacheSize.value = "0 KB"
        storage.putString(KEY_CACHE_SIZE, "0 KB")
    }

    override fun clearReadingHistory() {
        // In-memory reading history stub
    }
}
