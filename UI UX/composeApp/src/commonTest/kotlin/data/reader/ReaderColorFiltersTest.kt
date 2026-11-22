package data.reader

import androidx.compose.ui.graphics.Color
import data.settings.ReaderColorFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReaderColorFiltersTest {

    @Test
    fun testToColorFilter_mapsAllEntriesCorrectly() {
        assertNull(ReaderColorFilters.toColorFilter(ReaderColorFilter.DEFAULT))
        assertNotNull(ReaderColorFilters.toColorFilter(ReaderColorFilter.INVERTED_OLED))
        assertNotNull(ReaderColorFilters.toColorFilter(ReaderColorFilter.SEPIA))
        assertNotNull(ReaderColorFilters.toColorFilter(ReaderColorFilter.HIGH_CONTRAST))
    }

    @Test
    fun testCanvasBackgroundColor_matchesAesthetics() {
        assertEquals(Color(0xFFF9F7F1), ReaderColorFilters.getCanvasBackgroundColor(ReaderColorFilter.DEFAULT))
        assertEquals(Color(0xFF000000), ReaderColorFilters.getCanvasBackgroundColor(ReaderColorFilter.INVERTED_OLED))
        assertEquals(Color(0xFFF3EAD3), ReaderColorFilters.getCanvasBackgroundColor(ReaderColorFilter.SEPIA))
        assertEquals(Color(0xFFFFFFFF), ReaderColorFilters.getCanvasBackgroundColor(ReaderColorFilter.HIGH_CONTRAST))
    }

    @Test
    fun testBackplateColor_matchesFilters() {
        assertEquals(Color(0xFF000000), ReaderColorFilters.getBackplateColor(ReaderColorFilter.INVERTED_OLED))
        assertEquals(Color(0xFF1E1712), ReaderColorFilters.getBackplateColor(ReaderColorFilter.SEPIA))
        assertEquals(Color(0xFF0F1115), ReaderColorFilters.getBackplateColor(ReaderColorFilter.DEFAULT))
    }
}
