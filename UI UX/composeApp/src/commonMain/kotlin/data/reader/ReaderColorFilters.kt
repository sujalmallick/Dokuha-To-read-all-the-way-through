package data.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import data.settings.ReaderColorFilter

/**
 * ColorMatrix filters and background palettes for enhanced reading comfort.
 */
object ReaderColorFilters {

    /**
     * Inverted OLED filter:
     * Inverts RGB channels (-1f diagonal + 255f translation) while keeping alpha intact.
     * Transforms white background manga into dark canvas, saving power on OLED displays
     * and dramatically reducing eye strain in dark environments.
     */
    val InvertedOledFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f,  0f, 255f,
                 0f, -1f,  0f,  0f, 255f,
                 0f,  0f, -1f,  0f, 255f,
                 0f,  0f,  0f,  1f,   0f
            )
        )
    )

    /**
     * Warm Sepia filter:
     * Standard human-eye chromatic transformation for authentic vintage book paper feel.
     * Drastically reduces blue light spectrum emission.
     */
    val SepiaFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
        )
    )

    /**
     * High Contrast filter:
     * Boosts dynamic range (contrast = 1.35x), sharpening ink lines and deepening faded blacks
     * in older scanlations and archival magazines.
     */
    val HighContrastFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix().apply {
            val contrast = 1.35f
            val translate = 0.5f * (1f - contrast) * 255f
            setToScale(contrast, contrast, contrast, 1f)
            this[0, 4] = translate
            this[1, 4] = translate
            this[2, 4] = translate
        }
    )

    fun toColorFilter(filter: ReaderColorFilter): ColorFilter? = when (filter) {
        ReaderColorFilter.DEFAULT -> null
        ReaderColorFilter.INVERTED_OLED -> InvertedOledFilter
        ReaderColorFilter.SEPIA -> SepiaFilter
        ReaderColorFilter.HIGH_CONTRAST -> HighContrastFilter
    }

    /**
     * Returns the canvas background color corresponding to the active color filter.
     */
    fun getCanvasBackgroundColor(filter: ReaderColorFilter): Color = when (filter) {
        ReaderColorFilter.DEFAULT -> Color(0xFFF9F7F1) // Archival paper off-white
        ReaderColorFilter.INVERTED_OLED -> Color(0xFF000000) // Pitch black for true OLED off pixels
        ReaderColorFilter.SEPIA -> Color(0xFFF3EAD3) // Warm golden aged paper tone
        ReaderColorFilter.HIGH_CONTRAST -> Color(0xFFFFFFFF) // Pure clean white contrast
    }

    /**
     * Returns the outer desk backplate color.
     */
    fun getBackplateColor(filter: ReaderColorFilter): Color = when (filter) {
        ReaderColorFilter.INVERTED_OLED -> Color(0xFF000000)
        ReaderColorFilter.SEPIA -> Color(0xFF1E1712)
        else -> Color(0xFF0F1115)
    }
}
