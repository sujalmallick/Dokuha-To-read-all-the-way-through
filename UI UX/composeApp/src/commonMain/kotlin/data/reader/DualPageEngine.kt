package data.reader

data class DualPageSpread(
    val leftPage: Int?,
    val rightPage: Int?,
    val isDoubleSplash: Boolean = false
) {
    val isSinglePage: Boolean get() = leftPage == null || rightPage == null || isDoubleSplash
    val activePages: List<Int> get() = listOfNotNull(leftPage, rightPage).distinct()
}

object DualPageEngine {

    /**
     * Calculates spread pairings for a chapter with [pageCount] pages.
     *
     * In Japanese Manga (isRtl = true):
     * - Reading flows right to left.
     * - Right page is read first (recto), left page is read second (verso).
     * - When [offsetCover] is true, the first page (index 0) is standalone on the right side.
     *
     * In Western Comics (isRtl = false):
     * - Reading flows left to right.
     * - Left page is read first (verso), right page is read second (recto).
     * - When [offsetCover] is true, the first page (index 0) is standalone on the left side.
     */
    fun calculateSpreads(
        pageCount: Int,
        isRtl: Boolean,
        offsetCover: Boolean = false,
        doubleSpreadIndices: Set<Int> = emptySet()
    ): List<DualPageSpread> {
        if (pageCount <= 0) return emptyList()

        val spreads = mutableListOf<DualPageSpread>()
        var cursor = 0

        // Standalone cover page
        if (offsetCover && cursor < pageCount) {
            if (isRtl) {
                spreads.add(DualPageSpread(leftPage = null, rightPage = cursor))
            } else {
                spreads.add(DualPageSpread(leftPage = cursor, rightPage = null))
            }
            cursor++
        }

        while (cursor < pageCount) {
            // Wide double spread splash art
            if (cursor in doubleSpreadIndices) {
                spreads.add(DualPageSpread(leftPage = cursor, rightPage = cursor, isDoubleSplash = true))
                cursor++
                continue
            }

            val first = cursor
            val second = cursor + 1

            if (second < pageCount && second !in doubleSpreadIndices) {
                if (isRtl) {
                    // RTL: Right is first, Left is second
                    spreads.add(DualPageSpread(leftPage = second, rightPage = first))
                } else {
                    // LTR: Left is first, Right is second
                    spreads.add(DualPageSpread(leftPage = first, rightPage = second))
                }
                cursor += 2
            } else {
                // Trailing single page
                if (isRtl) {
                    spreads.add(DualPageSpread(leftPage = null, rightPage = first))
                } else {
                    spreads.add(DualPageSpread(leftPage = first, rightPage = null))
                }
                cursor++
            }
        }

        return spreads
    }

    fun getSpreadPageRangeLabel(spread: DualPageSpread, totalPages: Int): String {
        val active = spread.activePages.sorted()
        return when (active.size) {
            0 -> ""
            1 -> "Page ${active[0] + 1} / $totalPages"
            else -> "Pages ${active.first() + 1}-${active.last() + 1} / $totalPages"
        }
    }

    fun findSpreadIndexForPage(spreads: List<DualPageSpread>, pageIndex: Int): Int {
        val idx = spreads.indexOfFirst { it.leftPage == pageIndex || it.rightPage == pageIndex }
        return if (idx >= 0) idx else 0
    }
}
