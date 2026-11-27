package data.settings

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import core.platform.currentTimeMillis
import io.panel.reader.db.PanelDatabase
import io.panel.reader.db.Reader_prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface ReaderPrefsRepository {
    fun observePrefs(titleKey: String): Flow<TitleReaderPrefs?>
    suspend fun getPrefs(titleKey: String): TitleReaderPrefs?
    suspend fun getAllPrefs(): List<TitleReaderPrefs>
    suspend fun updatePrefs(titleKey: String, transform: (TitleReaderPrefs) -> TitleReaderPrefs)
    suspend fun upsertPrefs(prefs: TitleReaderPrefs)
    suspend fun resetPrefs(titleKey: String)
}

/**
 * SQLDelight-backed implementation of [ReaderPrefsRepository].
 *
 * When [SettingsRepository.incognitoMode] is active, writes are kept in an in-memory
 * session overlay and never persisted to SQLite.
 */
class SqlDelightReaderPrefsRepository(
    private val database: PanelDatabase,
    private val settingsRepository: SettingsRepository? = null,
    private val clock: () -> Long = { currentTimeMillis() }
) : ReaderPrefsRepository {

    private val incognitoOverlay = MutableStateFlow<Map<String, TitleReaderPrefs?>>(emptyMap())

    private fun isIncognito(): Boolean = settingsRepository?.incognitoMode?.value == true

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePrefs(titleKey: String): Flow<TitleReaderPrefs?> {
        val dbFlow = database.readerPrefsQueries
            .getByTitleKey(titleKey)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { row -> row?.toModel() }

        val incognitoFlow = settingsRepository?.incognitoMode ?: flowOf(false)

        return incognitoFlow.flatMapLatest { incognito ->
            if (!incognito) {
                dbFlow
            } else {
                combine(dbFlow, incognitoOverlay) { dbPrefs, overlay ->
                    if (overlay.containsKey(titleKey)) overlay[titleKey] else dbPrefs
                }
            }
        }
    }

    override suspend fun getPrefs(titleKey: String): TitleReaderPrefs? {
        if (isIncognito()) {
            val overlay = incognitoOverlay.value
            if (overlay.containsKey(titleKey)) return overlay[titleKey]
        }
        return database.readerPrefsQueries
            .getByTitleKey(titleKey)
            .executeAsOneOrNull()
            ?.toModel()
    }

    override suspend fun getAllPrefs(): List<TitleReaderPrefs> {
        return database.readerPrefsQueries
            .getAll()
            .executeAsList()
            .map { it.toModel() }
    }

    override suspend fun updatePrefs(
        titleKey: String,
        transform: (TitleReaderPrefs) -> TitleReaderPrefs
    ) {
        val current = getPrefs(titleKey) ?: TitleReaderPrefs(titleKey = titleKey)
        val updated = transform(current).copy(titleKey = titleKey).sanitized()
        if (!updated.hasAnyOverrides) {
            resetPrefs(titleKey)
        } else {
            upsertPrefs(updated)
        }
    }

    override suspend fun upsertPrefs(prefs: TitleReaderPrefs) {
        val sanitized = prefs.sanitized().copy(updatedAt = clock())
        if (!sanitized.hasAnyOverrides) {
            resetPrefs(sanitized.titleKey)
            return
        }
        if (isIncognito()) {
            incognitoOverlay.value = incognitoOverlay.value + (sanitized.titleKey to sanitized)
            return
        }
        database.readerPrefsQueries.upsert(
            titleKey = sanitized.titleKey,
            readingMode = sanitized.readingMode?.name,
            colorFilter = sanitized.colorFilter?.name,
            sidePadding = sanitized.sidePadding?.toDouble(),
            splitDoubleSpread = sanitized.splitDoubleSpread?.let { if (it) 1L else 0L },
            dualPageOffset = sanitized.dualPageOffset?.let { if (it) 1L else 0L },
            novelTheme = sanitized.novelTheme?.name,
            novelFont = sanitized.novelFont?.name,
            novelFontSize = sanitized.novelFontSize?.toDouble(),
            novelLineHeight = sanitized.novelLineHeight?.toDouble(),
            updatedAt = sanitized.updatedAt
        )
    }

    override suspend fun resetPrefs(titleKey: String) {
        if (isIncognito()) {
            incognitoOverlay.value = incognitoOverlay.value + (titleKey to null)
            return
        }
        database.readerPrefsQueries.deleteByTitleKey(titleKey)
    }
}

private fun Reader_prefs.toModel(): TitleReaderPrefs = TitleReaderPrefs(
    titleKey = title_key,
    readingMode = safeEnumValueOf<DefaultReadingMode>(reading_mode),
    colorFilter = safeEnumValueOf<ReaderColorFilter>(color_filter),
    sidePadding = side_padding?.toFloat(),
    splitDoubleSpread = split_double_spread?.let { it != 0L },
    dualPageOffset = dual_page_offset?.let { it != 0L },
    novelTheme = safeEnumValueOf<NovelReaderTheme>(novel_theme),
    novelFont = safeEnumValueOf<NovelFont>(novel_font),
    novelFontSize = novel_font_size?.toFloat(),
    novelLineHeight = novel_line_height?.toFloat(),
    updatedAt = updated_at
).sanitized()

class FakeReaderPrefsRepository(
    private val settingsRepository: SettingsRepository? = null
) : ReaderPrefsRepository {
    private val store = MutableStateFlow<Map<String, TitleReaderPrefs>>(emptyMap())

    override fun observePrefs(titleKey: String): Flow<TitleReaderPrefs?> =
        store.map { it[titleKey] }

    override suspend fun getPrefs(titleKey: String): TitleReaderPrefs? =
        store.value[titleKey]

    override suspend fun getAllPrefs(): List<TitleReaderPrefs> =
        store.value.values.toList()

    override suspend fun updatePrefs(
        titleKey: String,
        transform: (TitleReaderPrefs) -> TitleReaderPrefs
    ) {
        val current = getPrefs(titleKey) ?: TitleReaderPrefs(titleKey = titleKey)
        val updated = transform(current).copy(titleKey = titleKey).sanitized()
        if (!updated.hasAnyOverrides) {
            resetPrefs(titleKey)
        } else {
            upsertPrefs(updated)
        }
    }

    override suspend fun upsertPrefs(prefs: TitleReaderPrefs) {
        if (settingsRepository?.incognitoMode?.value == true) return
        val sanitized = prefs.sanitized()
        if (!sanitized.hasAnyOverrides) {
            resetPrefs(sanitized.titleKey)
            return
        }
        store.value = store.value + (sanitized.titleKey to sanitized)
    }

    override suspend fun resetPrefs(titleKey: String) {
        if (settingsRepository?.incognitoMode?.value == true) return
        store.value = store.value - titleKey
    }
}
