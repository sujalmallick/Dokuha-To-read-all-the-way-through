package ui.screens.webtoonreader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

enum class ReaderTapZone {
    LEFT,
    CENTER,
    RIGHT
}

enum class TapAction {
    PREVIOUS,
    TOGGLE_CONTROLS,
    NEXT
}

/**
 * Smart 3-zone tap navigation logic for manga, webtoon, and novel readers.
 * Left Zone: [0.0f, 0.25f)
 * Center Zone: [0.25f, 0.75f]
 * Right Zone: (0.75f, 1.0f]
 */
object ReaderTapZones {

    const val DEFAULT_EDGE_EXCLUSION_FRACTION = 0.06f

    fun resolveZone(xFraction: Float): ReaderTapZone {
        val clamped = xFraction.coerceIn(0f, 1f)
        return when {
            clamped < 0.25f -> ReaderTapZone.LEFT
            clamped > 0.75f -> ReaderTapZone.RIGHT
            else -> ReaderTapZone.CENTER
        }
    }

    fun resolveAction(zone: ReaderTapZone, isRtl: Boolean = false): TapAction {
        return when (zone) {
            ReaderTapZone.CENTER -> TapAction.TOGGLE_CONTROLS
            ReaderTapZone.LEFT -> if (isRtl) TapAction.NEXT else TapAction.PREVIOUS
            ReaderTapZone.RIGHT -> if (isRtl) TapAction.PREVIOUS else TapAction.NEXT
        }
    }

    /**
     * Resolves a tap fraction with outer edge-exclusion (to avoid colliding with OS Back gestures)
     * and optional swipe-only mode ([tapZonesEnabled] == false -> taps only toggle HUD).
     */
    fun resolveActionForFraction(
        xFraction: Float,
        isRtl: Boolean = false,
        tapZonesEnabled: Boolean = true,
        edgeExclusionFraction: Float = DEFAULT_EDGE_EXCLUSION_FRACTION
    ): TapAction? {
        val clamped = xFraction.coerceIn(0f, 1f)
        if (!tapZonesEnabled) {
            return TapAction.TOGGLE_CONTROLS
        }
        val safeEdge = edgeExclusionFraction.coerceIn(0f, 0.2f)
        if (clamped < safeEdge || clamped > 1f - safeEdge) {
            return null
        }
        return resolveAction(resolveZone(clamped), isRtl)
    }
}

/**
 * Attaches 3-zone smart tap handling to a reader viewport without blocking drag or scroll gestures.
 */
fun Modifier.readerTapHandler(
    isRtl: Boolean = false,
    tapZonesEnabled: Boolean = true,
    edgeExclusionFraction: Float = ReaderTapZones.DEFAULT_EDGE_EXCLUSION_FRACTION,
    onPrevious: () -> Unit,
    onToggleControls: () -> Unit,
    onNext: () -> Unit
): Modifier = pointerInput(isRtl, tapZonesEnabled, edgeExclusionFraction, onPrevious, onToggleControls, onNext) {
    detectTapGestures { offset ->
        val fraction = if (size.width > 0) (offset.x / size.width).coerceIn(0f, 1f) else 0.5f
        when (
            ReaderTapZones.resolveActionForFraction(
                xFraction = fraction,
                isRtl = isRtl,
                tapZonesEnabled = tapZonesEnabled,
                edgeExclusionFraction = edgeExclusionFraction
            )
        ) {
            TapAction.PREVIOUS -> onPrevious()
            TapAction.TOGGLE_CONTROLS -> onToggleControls()
            TapAction.NEXT -> onNext()
            null -> Unit
        }
    }
}
