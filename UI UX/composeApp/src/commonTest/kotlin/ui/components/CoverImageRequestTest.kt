package ui.components

import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class CoverImageRequestTest {

    @Test
    fun computeCoverDecodeSizeUsesDefaultsWhenNoOverridesProvided() {
        val size = computeCoverDecodeSize()
        assertEquals(Size(DEFAULT_COVER_WIDTH_PX, DEFAULT_COVER_HEIGHT_PX), size)
        assertEquals(360, (size.width as Dimension.Pixels).px)
        assertEquals(540, (size.height as Dimension.Pixels).px)
    }

    @Test
    fun computeCoverDecodeSizeRespectsBoundedDimensions() {
        val size = computeCoverDecodeSize(widthPx = 220, heightPx = 280)
        assertEquals(220, (size.width as Dimension.Pixels).px)
        assertEquals(280, (size.height as Dimension.Pixels).px)
    }

    @Test
    fun computeCoverDecodeSizeClampsNonPositiveAndOversizedDimensions() {
        val tooSmall = computeCoverDecodeSize(widthPx = 0, heightPx = -10)
        assertEquals(MIN_COVER_DIMENSION_PX, (tooSmall.width as Dimension.Pixels).px)
        assertEquals(MIN_COVER_DIMENSION_PX, (tooSmall.height as Dimension.Pixels).px)

        val tooLarge = computeCoverDecodeSize(widthPx = 5000, heightPx = 9000)
        assertEquals(MAX_COVER_DIMENSION_PX, (tooLarge.width as Dimension.Pixels).px)
        assertEquals(MAX_COVER_DIMENSION_PX, (tooLarge.height as Dimension.Pixels).px)
    }

    @Test
    fun defaultCoverPrecisionIsInexactForMemoryEfficientDownsampling() {
        assertEquals(Precision.INEXACT, DEFAULT_COVER_PRECISION)
    }
}
