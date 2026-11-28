package ui.screens.webtoonreader

import coil3.size.Dimension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderImageRequestTest {

    @Test
    fun computeReaderDecodeSizeClampsBounds() {
        val defaults = computeReaderDecodeSize()
        assertEquals(2048, (defaults.width as Dimension.Pixels).px)
        assertEquals(4096, (defaults.height as Dimension.Pixels).px)

        val tooSmall = computeReaderDecodeSize(maxWidthPx = 100, maxHeightPx = 100)
        assertEquals(320, (tooSmall.width as Dimension.Pixels).px)
        assertEquals(480, (tooSmall.height as Dimension.Pixels).px)

        val tooLarge = computeReaderDecodeSize(maxWidthPx = 8000, maxHeightPx = 20000)
        assertEquals(2048, (tooLarge.width as Dimension.Pixels).px)
        assertEquals(4096, (tooLarge.height as Dimension.Pixels).px)
    }

    @Test
    fun computePreloadIndicesSkipsAlreadyPreloadedPages() {
        val initial = computePreloadIndices(
            currentPageIndex = 0,
            preloadCount = 4,
            totalPages = 20,
            alreadyPreloadedEnd = 0
        )
        assertEquals(0..3, initial)

        val nextStep = computePreloadIndices(
            currentPageIndex = 1,
            preloadCount = 4,
            totalPages = 20,
            alreadyPreloadedEnd = 4
        )
        assertEquals(4..4, nextStep)

        val alreadyCovered = computePreloadIndices(
            currentPageIndex = 1,
            preloadCount = 4,
            totalPages = 20,
            alreadyPreloadedEnd = 5
        )
        assertTrue(alreadyCovered.isEmpty())
    }

    @Test
    fun computePreloadIndicesHandlesEmptyAndEndOfChapter() {
        assertTrue(computePreloadIndices(0, 4, 0, 0).isEmpty())
        assertTrue(computePreloadIndices(0, 0, 10, 0).isEmpty())

        val tail = computePreloadIndices(
            currentPageIndex = 8,
            preloadCount = 5,
            totalPages = 10,
            alreadyPreloadedEnd = 9
        )
        assertEquals(9..9, tail)
    }
}
