package data.settings

import core.platform.normalizeUnicodeNfkc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

enum class ReaderSettingsScope {
    THIS_TITLE,
    ALL_TITLES
}

/**
 * Per-title reader setting overrides.
 * Every overridable field is nullable: `null` means "inherit global default".
 */
data class TitleReaderPrefs(
    val titleKey: String,
    val readingMode: DefaultReadingMode? = null,
    val colorFilter: ReaderColorFilter? = null,
    val sidePadding: Float? = null,
    val splitDoubleSpread: Boolean? = null,
    val dualPageOffset: Boolean? = null,
    val novelTheme: NovelReaderTheme? = null,
    val novelFont: NovelFont? = null,
    val novelFontSize: Float? = null,
    val novelLineHeight: Float? = null,
    val updatedAt: Long = 0L
) {
    val hasMangaOverrides: Boolean
        get() = readingMode != null ||
            colorFilter != null ||
            sidePadding != null ||
            splitDoubleSpread != null ||
            dualPageOffset != null

    val hasNovelOverrides: Boolean
        get() = novelTheme != null ||
            novelFont != null ||
            novelFontSize != null ||
            novelLineHeight != null

    val hasAnyOverrides: Boolean
        get() = hasMangaOverrides || hasNovelOverrides
}

/**
 * Effective reader settings resolved by merging [TitleReaderPrefs] over global defaults.
 */
data class ResolvedReaderSettings(
    val titleKey: String,
    val readingMode: DefaultReadingMode,
    val colorFilter: ReaderColorFilter,
    val sidePadding: Float,
    val splitDoubleSpread: Boolean,
    val dualPageOffset: Boolean,
    val novelTheme: NovelReaderTheme,
    val novelFont: NovelFont,
    val novelFontSize: Float,
    val novelLineHeight: Float,
    val brightness: Float,
    val keepScreenOn: Boolean,
    val readerPreloadPages: Int,
    val autoDownloadNextChapter: Boolean,
    val volumeKeyNavigation: Boolean,
    val isReadingModeCustom: Boolean = false,
    val isColorFilterCustom: Boolean = false,
    val isSidePaddingCustom: Boolean = false,
    val isSplitDoubleSpreadCustom: Boolean = false,
    val isDualPageOffsetCustom: Boolean = false,
    val isNovelThemeCustom: Boolean = false,
    val isNovelFontCustom: Boolean = false,
    val isNovelFontSizeCustom: Boolean = false,
    val isNovelLineHeightCustom: Boolean = false
) {
    val hasMangaOverrides: Boolean
        get() = isReadingModeCustom ||
            isColorFilterCustom ||
            isSidePaddingCustom ||
            isSplitDoubleSpreadCustom ||
            isDualPageOffsetCustom

    val hasNovelOverrides: Boolean
        get() = isNovelThemeCustom ||
            isNovelFontCustom ||
            isNovelFontSizeCustom ||
            isNovelLineHeightCustom

    val mangaOverrideCount: Int
        get() = listOf(
            isReadingModeCustom,
            isColorFilterCustom,
            isSidePaddingCustom,
            isSplitDoubleSpreadCustom,
            isDualPageOffsetCustom
        ).count { it }

    val novelOverrideCount: Int
        get() = listOf(
            isNovelThemeCustom,
            isNovelFontCustom,
            isNovelFontSizeCustom,
            isNovelLineHeightCustom
        ).count { it }
}

/**
 * Resolves effective settings for a title using per-title overrides when non-null,
 * falling back to [global] defaults otherwise.
 */
fun resolveReaderSettings(
    titleKey: String,
    global: SettingsRepository,
    override: TitleReaderPrefs?
): ResolvedReaderSettings {
    val sanitizedOverride = override?.sanitized()
    return ResolvedReaderSettings(
        titleKey = titleKey,
        readingMode = sanitizedOverride?.readingMode ?: global.defaultReadingMode.value,
        colorFilter = sanitizedOverride?.colorFilter ?: global.readerColorFilter.value,
        sidePadding = (sanitizedOverride?.sidePadding ?: global.sidePadding.value).coerceIn(0f, 0.4f),
        splitDoubleSpread = sanitizedOverride?.splitDoubleSpread ?: global.splitDoubleSpread.value,
        dualPageOffset = sanitizedOverride?.dualPageOffset ?: global.dualPageOffset.value,
        novelTheme = sanitizedOverride?.novelTheme ?: global.novelTheme.value,
        novelFont = sanitizedOverride?.novelFont ?: global.novelFont.value,
        novelFontSize = (sanitizedOverride?.novelFontSize ?: global.novelFontSize.value).coerceIn(12f, 26f),
        novelLineHeight = (sanitizedOverride?.novelLineHeight ?: global.novelLineHeight.value).coerceIn(1.2f, 2.2f),
        brightness = global.brightness.value.coerceIn(0.1f, 1.0f),
        keepScreenOn = global.keepScreenOn.value,
        readerPreloadPages = global.readerPreloadPages.value.coerceIn(0, 20),
        autoDownloadNextChapter = global.autoDownloadNextChapter.value,
        volumeKeyNavigation = global.volumeKeyNavigation.value,
        isReadingModeCustom = sanitizedOverride?.readingMode != null,
        isColorFilterCustom = sanitizedOverride?.colorFilter != null,
        isSidePaddingCustom = sanitizedOverride?.sidePadding != null,
        isSplitDoubleSpreadCustom = sanitizedOverride?.splitDoubleSpread != null,
        isDualPageOffsetCustom = sanitizedOverride?.dualPageOffset != null,
        isNovelThemeCustom = sanitizedOverride?.novelTheme != null,
        isNovelFontCustom = sanitizedOverride?.novelFont != null,
        isNovelFontSizeCustom = sanitizedOverride?.novelFontSize != null,
        isNovelLineHeightCustom = sanitizedOverride?.novelLineHeight != null
    )
}

fun observeResolvedMangaSettings(
    titleKey: String,
    global: SettingsRepository,
    overrideFlow: Flow<TitleReaderPrefs?>
): Flow<ResolvedReaderSettings> {
    val mangaGlobalFlow = combine(
        global.defaultReadingMode,
        global.readerColorFilter,
        global.sidePadding,
        global.splitDoubleSpread,
        global.dualPageOffset
    ) { _, _, _, _, _ -> Unit }

    val deviceGlobalFlow = combine(
        global.brightness,
        global.keepScreenOn,
        global.readerPreloadPages,
        global.autoDownloadNextChapter,
        global.volumeKeyNavigation
    ) { _, _, _, _, _ -> Unit }

    return combine(mangaGlobalFlow, deviceGlobalFlow, overrideFlow) { _, _, override ->
        resolveReaderSettings(titleKey, global, override)
    }
}

fun observeResolvedNovelSettings(
    titleKey: String,
    global: SettingsRepository,
    overrideFlow: Flow<TitleReaderPrefs?>
): Flow<ResolvedReaderSettings> {
    val novelGlobalFlow = combine(
        global.novelTheme,
        global.novelFont,
        global.novelFontSize,
        global.novelLineHeight
    ) { _, _, _, _ -> Unit }

    return combine(novelGlobalFlow, overrideFlow) { _, override ->
        resolveReaderSettings(titleKey, global, override)
    }
}

fun TitleReaderPrefs.sanitized(): TitleReaderPrefs = copy(
    sidePadding = sidePadding?.takeIf { !it.isNaN() }?.coerceIn(0f, 0.4f),
    novelFontSize = novelFontSize?.takeIf { !it.isNaN() }?.coerceIn(12f, 26f),
    novelLineHeight = novelLineHeight?.takeIf { !it.isNaN() }?.coerceIn(1.2f, 2.2f)
)

private const val MAX_KEY_BODY_LENGTH = 180

/**
 * Computes a source-independent canonical key for a title so per-title preferences
 * survive source migration. Preserves all Unicode letters and digits (Latin, CJK,
 * Hangul, Cyrillic, etc.) after NFKC normalization, falling back to the raw trimmed
 * title if stripping punctuation/emoji leaves an empty string.
 */
fun canonicalTitleKey(title: String, isNovel: Boolean): String {
    val prefix = if (isNovel) "novel:" else "manga:"
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return "${prefix}__untitled__"

    val nfkcLower = normalizeUnicodeNfkc(trimmed).lowercase()
    val sb = StringBuilder(nfkcLower.length)
    var lastWasSeparator = false
    for (ch in nfkcLower) {
        if (ch.isLetterOrDigit()) {
            sb.append(ch)
            lastWasSeparator = false
        } else if (sb.isNotEmpty() && !lastWasSeparator) {
            sb.append('-')
            lastWasSeparator = true
        }
    }
    val slug = sb.toString().trimEnd('-')
    val body = slug.ifEmpty { trimmed }
    val bounded = if (body.length > MAX_KEY_BODY_LENGTH) {
        "${body.take(140)}-${fnv1a64Hex(body)}"
    } else {
        body
    }
    return "$prefix$bounded"
}

private fun fnv1a64Hex(input: String): String {
    var hash = -3750763034362895579L // 0xcbf29ce484222325UL
    val prime = 1099511628211L       // 0x100000001b3UL
    for (ch in input) {
        val code = ch.code
        hash = (hash xor (code and 0xFF).toLong()) * prime
        hash = (hash xor ((code ushr 8) and 0xFF).toLong()) * prime
    }
    return hash.toULong().toString(16).padStart(16, '0')
}

inline fun <reified T : Enum<T>> safeEnumValueOf(raw: String?): T? {
    if (raw.isNullOrBlank()) return null
    return runCatching { enumValueOf<T>(raw) }.getOrNull()
}
