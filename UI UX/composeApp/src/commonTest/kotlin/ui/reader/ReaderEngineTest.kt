package ui.reader

import data.settings.DefaultReadingMode
import data.settings.FakeSettingsRepository
import data.settings.NovelFont
import data.settings.NovelReaderTheme
import ui.screens.novelreader.toColors
import ui.screens.novelreader.toFontFamily
import ui.screens.webtoonreader.ReaderTapZone
import ui.screens.webtoonreader.ReaderTapZones
import ui.screens.webtoonreader.TapAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReaderEngineTest {

    @Test
    fun testTapZoneCoordinateMapping_leftZone() {
        assertEquals(ReaderTapZone.LEFT, ReaderTapZones.resolveZone(0.0f))
        assertEquals(ReaderTapZone.LEFT, ReaderTapZones.resolveZone(0.12f))
        assertEquals(ReaderTapZone.LEFT, ReaderTapZones.resolveZone(0.249f))
        // Out-of-bounds negative clamped to left
        assertEquals(ReaderTapZone.LEFT, ReaderTapZones.resolveZone(-0.5f))
    }

    @Test
    fun testTapZoneCoordinateMapping_centerZone() {
        assertEquals(ReaderTapZone.CENTER, ReaderTapZones.resolveZone(0.25f))
        assertEquals(ReaderTapZone.CENTER, ReaderTapZones.resolveZone(0.50f))
        assertEquals(ReaderTapZone.CENTER, ReaderTapZones.resolveZone(0.75f))
    }

    @Test
    fun testTapZoneCoordinateMapping_rightZone() {
        assertEquals(ReaderTapZone.RIGHT, ReaderTapZones.resolveZone(0.751f))
        assertEquals(ReaderTapZone.RIGHT, ReaderTapZones.resolveZone(0.90f))
        assertEquals(ReaderTapZone.RIGHT, ReaderTapZones.resolveZone(1.0f))
        // Out-of-bounds positive clamped to right
        assertEquals(ReaderTapZone.RIGHT, ReaderTapZones.resolveZone(1.8f))
    }

    @Test
    fun testTapActionResolution_standardLtrAndContinuous() {
        // Center always toggles controls
        assertEquals(TapAction.TOGGLE_CONTROLS, ReaderTapZones.resolveAction(ReaderTapZone.CENTER, isRtl = false))

        // In LTR: Left = Previous, Right = Next
        assertEquals(TapAction.PREVIOUS, ReaderTapZones.resolveAction(ReaderTapZone.LEFT, isRtl = false))
        assertEquals(TapAction.NEXT, ReaderTapZones.resolveAction(ReaderTapZone.RIGHT, isRtl = false))
    }

    @Test
    fun testTapActionResolution_mangaRtlDirectionality() {
        // Center always toggles controls regardless of RTL
        assertEquals(TapAction.TOGGLE_CONTROLS, ReaderTapZones.resolveAction(ReaderTapZone.CENTER, isRtl = true))

        // In RTL manga: Left = Next (advances page in right-to-left layout), Right = Previous
        assertEquals(TapAction.NEXT, ReaderTapZones.resolveAction(ReaderTapZone.LEFT, isRtl = true))
        assertEquals(TapAction.PREVIOUS, ReaderTapZones.resolveAction(ReaderTapZone.RIGHT, isRtl = true))
    }

    @Test
    fun testReadingModes_allModesAvailable() {
        val modes = DefaultReadingMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(DefaultReadingMode.WEBTOON))
        assertTrue(modes.contains(DefaultReadingMode.RIGHT_TO_LEFT))
        assertTrue(modes.contains(DefaultReadingMode.LEFT_TO_RIGHT))
        assertTrue(modes.contains(DefaultReadingMode.VERTICAL))

        assertEquals("Webtoon", DefaultReadingMode.WEBTOON.label)
        assertEquals("Right to Left", DefaultReadingMode.RIGHT_TO_LEFT.label)
        assertEquals("Left to Right", DefaultReadingMode.LEFT_TO_RIGHT.label)
        assertEquals("Vertical", DefaultReadingMode.VERTICAL.label)
    }

    @Test
    fun testSettingsRepository_readerSettingsPersistenceAndClamping() {
        val repo = FakeSettingsRepository()

        // Default reading mode
        assertEquals(DefaultReadingMode.WEBTOON, repo.defaultReadingMode.value)
        repo.setDefaultReadingMode(DefaultReadingMode.RIGHT_TO_LEFT)
        assertEquals(DefaultReadingMode.RIGHT_TO_LEFT, repo.defaultReadingMode.value)

        // Side padding clamping (0.0f..0.40f)
        repo.setSidePadding(0.25f)
        assertEquals(0.25f, repo.sidePadding.value)
        repo.setSidePadding(-0.1f)
        assertEquals(0.0f, repo.sidePadding.value)
        repo.setSidePadding(0.9f)
        assertEquals(0.40f, repo.sidePadding.value)

        // Brightness dimmer clamping (0.10f..1.0f)
        repo.setBrightness(0.50f)
        assertEquals(0.50f, repo.brightness.value)
        repo.setBrightness(0.02f)
        assertEquals(0.10f, repo.brightness.value)
        repo.setBrightness(1.50f)
        assertEquals(1.0f, repo.brightness.value)

        // Crop borders toggle
        assertFalse(repo.cropBorders.value)
        repo.setCropBorders(true)
        assertTrue(repo.cropBorders.value)
    }

    @Test
    fun testNovelSettings_fontsThemesAndClamping() {
        val repo = FakeSettingsRepository()

        // Font and font family mapping
        assertEquals(NovelFont.SYSTEM, repo.novelFont.value)
        NovelFont.entries.forEach { font ->
            repo.setNovelFont(font)
            assertEquals(font, repo.novelFont.value)
            assertNotNull(font.toFontFamily())
        }

        // Font size clamping (12f..28f)
        repo.setNovelFontSize(20f)
        assertEquals(20f, repo.novelFontSize.value)
        repo.setNovelFontSize(5f)
        assertEquals(12f, repo.novelFontSize.value)
        repo.setNovelFontSize(45f)
        assertEquals(28f, repo.novelFontSize.value)

        // Line height clamping (1.2f..2.2f)
        repo.setNovelLineHeight(1.8f)
        assertEquals(1.8f, repo.novelLineHeight.value)
        repo.setNovelLineHeight(0.5f)
        assertEquals(1.2f, repo.novelLineHeight.value)
        repo.setNovelLineHeight(3.5f)
        assertEquals(2.2f, repo.novelLineHeight.value)

        // Theme palette mapping
        NovelReaderTheme.entries.forEach { theme ->
            repo.setNovelTheme(theme)
            assertEquals(theme, repo.novelTheme.value)
            val colors = theme.toColors()
            assertNotEquals(colors.backgroundColor, colors.textColor)
        }
    }

    @Test
    fun testProgressCalculation_paginatedAndContinuousLogic() {
        // Continuous: total slices = 6
        val totalSlices = 6
        fun continuousProgress(itemIndex: Int, offsetFraction: Float): Float {
            return ((itemIndex + offsetFraction) / totalSlices.toFloat()).coerceIn(0f, 1f)
        }

        assertEquals(0.0f, continuousProgress(0, 0f))
        assertEquals(0.5f, continuousProgress(3, 0f))
        assertEquals(1.0f, continuousProgress(6, 0f))

        // Paginated: total pages = 6 (pages 0..5)
        fun paginatedProgress(currentPage: Int, total: Int): Float {
            if (total <= 1) return 0f
            return (currentPage.toFloat() / (total - 1).toFloat()).coerceIn(0f, 1f)
        }

        assertEquals(0.0f, paginatedProgress(0, 6))
        assertEquals(0.2f, paginatedProgress(1, 6))
        assertEquals(0.6f, paginatedProgress(3, 6))
        assertEquals(1.0f, paginatedProgress(5, 6))
    }

    private fun assertNotNull(value: Any?) {
        assertTrue(value != null)
    }
}
