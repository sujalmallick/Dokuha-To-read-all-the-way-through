package data

import core.storage.InMemoryKeyValueStorage
import data.settings.DefaultReadingMode
import data.settings.FakeReaderPrefsRepository
import data.settings.FakeSettingsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import data.settings.PersistentSettingsRepository
import data.settings.ReaderColorFilter
import data.settings.TitleReaderPrefs
import data.settings.canonicalTitleKey
import data.settings.observeResolvedMangaSettings
import data.settings.observeResolvedNovelSettings
import data.settings.resolveReaderSettings
import data.settings.safeEnumValueOf
import data.settings.sanitized
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReaderPrefsRepositoryTest {

    @Test
    fun testResolutionOrder_overrideBeatsGlobal_andNullInheritsGlobal() {
        val global = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.WEBTOON)
            setReaderColorFilter(ReaderColorFilter.DEFAULT)
            setSidePadding(0.10f)
            setSplitDoubleSpread(true)
            setDualPageOffset(false)
            setBrightness(0.75f)
        }

        val key = canonicalTitleKey("Solo Leveling", isNovel = false)
        val override = TitleReaderPrefs(
            titleKey = key,
            readingMode = DefaultReadingMode.RIGHT_TO_LEFT,
            colorFilter = ReaderColorFilter.INVERTED_OLED,
            sidePadding = null,
            splitDoubleSpread = null,
            dualPageOffset = true
        )

        val resolved = resolveReaderSettings(key, global, override)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, resolved.readingMode)
        assertEquals(ReaderColorFilter.INVERTED_OLED, resolved.colorFilter)
        assertEquals(0.10f, resolved.sidePadding)
        assertTrue(resolved.splitDoubleSpread)
        assertTrue(resolved.dualPageOffset)
        assertEquals(0.75f, resolved.brightness)

        assertTrue(resolved.isReadingModeCustom)
        assertTrue(resolved.isColorFilterCustom)
        assertFalse(resolved.isSidePaddingCustom)
        assertFalse(resolved.isSplitDoubleSpreadCustom)
        assertTrue(resolved.isDualPageOffsetCustom)
        assertTrue(resolved.hasMangaOverrides)
        assertFalse(resolved.hasNovelOverrides)

        // Changing a global setting updates non-overridden fields without clobbering overrides
        global.setDefaultReadingMode(DefaultReadingMode.LEFT_TO_RIGHT)
        global.setSidePadding(0.24f)
        val updated = resolveReaderSettings(key, global, override)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, updated.readingMode)
        assertEquals(0.24f, updated.sidePadding)
    }

    @Test
    fun testResetPrefs_clearsAllOverridesAndRevertsToGlobal() = runTest {
        val global = FakeSettingsRepository().apply {
            setDefaultReadingMode(DefaultReadingMode.WEBTOON)
            setReaderColorFilter(ReaderColorFilter.SEPIA)
        }
        val prefsRepo = FakeReaderPrefsRepository(global)
        val key = canonicalTitleKey("Omniscient Reader", isNovel = false)

        prefsRepo.updatePrefs(key) {
            it.copy(
                readingMode = DefaultReadingMode.LEFT_TO_RIGHT,
                colorFilter = ReaderColorFilter.HIGH_CONTRAST
            )
        }

        val overridden = observeResolvedMangaSettings(key, global, prefsRepo.observePrefs(key)).first()
        assertEquals(DefaultReadingMode.LEFT_TO_RIGHT, overridden.readingMode)
        assertEquals(ReaderColorFilter.HIGH_CONTRAST, overridden.colorFilter)
        assertTrue(overridden.hasMangaOverrides)

        prefsRepo.resetPrefs(key)

        val reset = observeResolvedMangaSettings(key, global, prefsRepo.observePrefs(key)).first()
        assertEquals(DefaultReadingMode.WEBTOON, reset.readingMode)
        assertEquals(ReaderColorFilter.SEPIA, reset.colorFilter)
        assertFalse(reset.hasMangaOverrides)
        assertNull(prefsRepo.getPrefs(key))
    }

    @Test
    fun testClampingAndUnknownEnumSafety() {
        val raw = TitleReaderPrefs(
            titleKey = "manga:test",
            sidePadding = 2.5f,
            novelFontSize = 99f,
            novelLineHeight = 0.1f
        ).sanitized()

        assertEquals(0.4f, raw.sidePadding)
        assertEquals(26f, raw.novelFontSize)
        assertEquals(1.2f, raw.novelLineHeight)

        val nanSanitized = TitleReaderPrefs(
            titleKey = "manga:nan",
            sidePadding = Float.NaN,
            novelFontSize = Float.NaN,
            novelLineHeight = Float.NaN
        ).sanitized()
        assertNull(nanSanitized.sidePadding)
        assertNull(nanSanitized.novelFontSize)
        assertNull(nanSanitized.novelLineHeight)

        assertNull(safeEnumValueOf<DefaultReadingMode>("LEGACY_UNKNOWN_MODE"))
        assertNull(safeEnumValueOf<ReaderColorFilter>("UNKNOWN_FILTER"))
        assertNull(safeEnumValueOf<NovelFont>("COMIC_SANS_LEGACY"))
        assertNull(safeEnumValueOf<NovelReaderTheme>(""))
        assertEquals(DefaultReadingMode.VERTICAL, safeEnumValueOf<DefaultReadingMode>("VERTICAL"))
    }

    @Test
    fun testIncognitoMode_doesNotPersistOverrides() = runTest {
        val storage = InMemoryKeyValueStorage()
        val global = PersistentSettingsRepository(storage)
        val prefsRepo = FakeReaderPrefsRepository(global)
        val key = canonicalTitleKey("Secret Manga", isNovel = false)

        global.setIncognitoMode(true)
        prefsRepo.updatePrefs(key) { it.copy(readingMode = DefaultReadingMode.RIGHT_TO_LEFT) }

        assertNull(prefsRepo.getPrefs(key))
    }

    @Test
    fun testCanonicalTitleKey_allScriptsEdgeCasesAndNoCjkCollisions() {
        // Format isolation: same title as manga vs novel must never collide
        assertNotEquals(
            canonicalTitleKey("Solo Leveling", isNovel = false),
            canonicalTitleKey("Solo Leveling", isNovel = true)
        )

        // Korean
        val kr1 = canonicalTitleKey("나 혼자만 레벨업", isNovel = false)
        val kr2 = canonicalTitleKey("전지적 독자 시점", isNovel = false)
        assertTrue(kr1.contains("나-혼자만-레벨업"))
        assertNotEquals(kr1, kr2)

        // Japanese
        val jp1 = canonicalTitleKey("進撃の巨人", isNovel = false)
        val jp2 = canonicalTitleKey("呪術廻戦", isNovel = false)
        assertEquals("manga:進撃の巨人", jp1)
        assertEquals("manga:呪術廻戦", jp2)
        assertNotEquals(jp1, jp2)

        // Chinese
        val zh1 = canonicalTitleKey("斗罗大陆", isNovel = false)
        val zh2 = canonicalTitleKey("全职高手", isNovel = false)
        assertEquals("manga:斗罗大陆", zh1)
        assertEquals("manga:全职高手", zh2)
        assertNotEquals(zh1, zh2)

        // NFKC normalization (fullwidth vs halfwidth Latin & Katakana)
        assertEquals(
            canonicalTitleKey("Solo Leveling", isNovel = false),
            canonicalTitleKey("Ｓｏｌｏ　Ｌｅｖｅｌｉｎｇ", isNovel = false)
        )
        assertEquals(
            canonicalTitleKey("ソロ", isNovel = false),
            canonicalTitleKey("ｿﾛ", isNovel = false)
        )

        // Emoji-only fallback
        val emoji1 = canonicalTitleKey("🔥🚀", isNovel = false)
        val emoji2 = canonicalTitleKey("😭😭", isNovel = false)
        assertEquals("manga:🔥🚀", emoji1)
        assertNotEquals(emoji1, emoji2)

        // Punctuation-only fallback
        val punct1 = canonicalTitleKey("...", isNovel = false)
        val punct2 = canonicalTitleKey("???", isNovel = false)
        assertEquals("manga:...", punct1)
        assertNotEquals(punct1, punct2)

        // Whitespace-only fallback
        val ws = canonicalTitleKey("   \t\n  ", isNovel = false)
        assertEquals("manga:__untitled__", ws)

        // Very long titles differing only at the end
        val prefix = "a".repeat(250)
        val long1 = canonicalTitleKey("${prefix}one", isNovel = true)
        val long2 = canonicalTitleKey("${prefix}two", isNovel = true)
        assertTrue(long1.length <= 190)
        assertNotEquals(long1, long2)
    }

    @Test
    fun testNovelScopeBehavior_resolvesAndUpdatesPerTitleVsGlobal() = runTest {
        val global = FakeSettingsRepository().apply {
            setNovelTheme(NovelReaderTheme.DARK)
            setNovelFont(NovelFont.SYSTEM)
            setNovelFontSize(16f)
            setNovelLineHeight(1.5f)
        }
        val prefsRepo = FakeReaderPrefsRepository(global)
        val novelKey = canonicalTitleKey("The Beginning After The End", isNovel = true)

        prefsRepo.updatePrefs(novelKey) {
            it.copy(
                novelTheme = NovelReaderTheme.SEPIA,
                novelFontSize = 20f
            )
        }

        val resolved = observeResolvedNovelSettings(novelKey, global, prefsRepo.observePrefs(novelKey)).first()
        assertEquals(NovelReaderTheme.SEPIA, resolved.novelTheme)
        assertEquals(NovelFont.SYSTEM, resolved.novelFont)
        assertEquals(20f, resolved.novelFontSize)
        assertEquals(1.5f, resolved.novelLineHeight)
        assertTrue(resolved.isNovelThemeCustom)
        assertFalse(resolved.isNovelFontCustom)
        assertTrue(resolved.isNovelFontSizeCustom)
        assertTrue(resolved.hasNovelOverrides)

        // Updating global font affects novel because novelFont override is null
        global.setNovelFont(NovelFont.SERIF)
        val afterGlobalChange = observeResolvedNovelSettings(novelKey, global, prefsRepo.observePrefs(novelKey)).first()
        assertEquals(NovelFont.SERIF, afterGlobalChange.novelFont)
        assertEquals(NovelReaderTheme.SEPIA, afterGlobalChange.novelTheme)
    }
}
