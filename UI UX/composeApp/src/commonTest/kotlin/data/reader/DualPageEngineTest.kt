package data.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DualPageEngineTest {

    @Test
    fun testRtlMangaSpreadPairing() {
        val spreads = DualPageEngine.calculateSpreads(
            pageCount = 6,
            isRtl = true,
            offsetCover = false
        )

        assertEquals(3, spreads.size)

        // Spread 0: Right = Page 0 (read first), Left = Page 1 (read second)
        assertEquals(0, spreads[0].rightPage)
        assertEquals(1, spreads[0].leftPage)
        assertFalse(spreads[0].isSinglePage)

        // Spread 1: Right = Page 2, Left = Page 3
        assertEquals(2, spreads[1].rightPage)
        assertEquals(3, spreads[1].leftPage)

        // Spread 2: Right = Page 4, Left = Page 5
        assertEquals(4, spreads[2].rightPage)
        assertEquals(5, spreads[2].leftPage)
    }

    @Test
    fun testLtrComicSpreadPairing() {
        val spreads = DualPageEngine.calculateSpreads(
            pageCount = 6,
            isRtl = false,
            offsetCover = false
        )

        assertEquals(3, spreads.size)

        // Spread 0: Left = Page 0 (read first), Right = Page 1 (read second)
        assertEquals(0, spreads[0].leftPage)
        assertEquals(1, spreads[0].rightPage)
        assertFalse(spreads[0].isSinglePage)

        // Spread 1: Left = Page 2, Right = Page 3
        assertEquals(2, spreads[1].leftPage)
        assertEquals(3, spreads[1].rightPage)

        // Spread 2: Left = Page 4, Right = Page 5
        assertEquals(4, spreads[2].leftPage)
        assertEquals(5, spreads[2].rightPage)
    }

    @Test
    fun testRtlCoverOffsetMode() {
        val spreads = DualPageEngine.calculateSpreads(
            pageCount = 6,
            isRtl = true,
            offsetCover = true
        )

        // Spread 0: Standalone cover on right side
        assertEquals(4, spreads.size)
        assertEquals(0, spreads[0].rightPage)
        assertNull(spreads[0].leftPage)
        assertTrue(spreads[0].isSinglePage)

        // Spread 1: Page 1 on right, Page 2 on left
        assertEquals(1, spreads[1].rightPage)
        assertEquals(2, spreads[1].leftPage)

        // Spread 2: Page 3 on right, Page 4 on left
        assertEquals(3, spreads[2].rightPage)
        assertEquals(4, spreads[2].leftPage)

        // Spread 3: Page 5 trailing on right
        assertEquals(5, spreads[3].rightPage)
        assertNull(spreads[3].leftPage)
        assertTrue(spreads[3].isSinglePage)
    }

    @Test
    fun testOddPageCountHandling() {
        val spreads = DualPageEngine.calculateSpreads(
            pageCount = 5,
            isRtl = true,
            offsetCover = false
        )

        assertEquals(3, spreads.size)
        // Spread 0: 0 & 1
        assertEquals(0, spreads[0].rightPage)
        assertEquals(1, spreads[0].leftPage)

        // Spread 1: 2 & 3
        assertEquals(2, spreads[1].rightPage)
        assertEquals(3, spreads[1].leftPage)

        // Spread 2: 4 alone
        assertEquals(4, spreads[2].rightPage)
        assertNull(spreads[2].leftPage)
        assertTrue(spreads[2].isSinglePage)
    }

    @Test
    fun testDoubleSpreadSplashArtSpansBothPages() {
        val spreads = DualPageEngine.calculateSpreads(
            pageCount = 6,
            isRtl = true,
            offsetCover = false,
            doubleSpreadIndices = setOf(2) // Page 2 is wide double spread
        )

        assertEquals(4, spreads.size)

        // Spread 0: pages 0 & 1
        assertEquals(0, spreads[0].rightPage)
        assertEquals(1, spreads[0].leftPage)

        // Spread 1: page 2 double splash
        assertEquals(2, spreads[1].rightPage)
        assertEquals(2, spreads[1].leftPage)
        assertTrue(spreads[1].isDoubleSplash)

        // Spread 2: pages 3 & 4
        assertEquals(3, spreads[2].rightPage)
        assertEquals(4, spreads[2].leftPage)

        // Spread 3: page 5 trailing
        assertEquals(5, spreads[3].rightPage)
        assertNull(spreads[3].leftPage)
    }

    @Test
    fun testSpreadPageRangeLabels() {
        val singleSpread = DualPageSpread(leftPage = null, rightPage = 0)
        assertEquals("Page 1 / 10", DualPageEngine.getSpreadPageRangeLabel(singleSpread, 10))

        val dualSpread = DualPageSpread(leftPage = 2, rightPage = 1)
        assertEquals("Pages 2-3 / 10", DualPageEngine.getSpreadPageRangeLabel(dualSpread, 10))
    }

    @Test
    fun testFindSpreadIndexForPage() {
        val spreads = DualPageEngine.calculateSpreads(pageCount = 6, isRtl = true)
        assertEquals(0, DualPageEngine.findSpreadIndexForPage(spreads, 0))
        assertEquals(0, DualPageEngine.findSpreadIndexForPage(spreads, 1))
        assertEquals(1, DualPageEngine.findSpreadIndexForPage(spreads, 2))
        assertEquals(1, DualPageEngine.findSpreadIndexForPage(spreads, 3))
        assertEquals(2, DualPageEngine.findSpreadIndexForPage(spreads, 4))
        assertEquals(2, DualPageEngine.findSpreadIndexForPage(spreads, 5))
    }

    @Test
    fun testEmptyPageCount() {
        val spreads = DualPageEngine.calculateSpreads(pageCount = 0, isRtl = true)
        assertTrue(spreads.isEmpty())
    }
}
