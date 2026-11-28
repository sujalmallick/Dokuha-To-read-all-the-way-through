/*!
 * ReactDrops Component License 1.0
 * Copyright (c) 2026 ReactDrops
 *
 * Adapted for Dokuha (Compose Multiplatform).
 */
package ui.components.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

/**
 * Uniform snapshot passed to the platform shader implementation per frame.
 */
data class LiquidGlassShaderUniforms(
    val canvasSize: IntSize,
    val dpr: Float,
    val unitPx: Float,
    val logoBoxPx: Float,
    val timeSec: Float,
    val settle: Float,
    val artScale: Float,
    val tileEnabled: Float = 1f,
    val depth: Float = 1.2f,
    val dispersion: Float = 1f,
    val relief: Float = 1f,
    val caustics: Float = 1f,
    val highlights: Float = 1f,
    val dropStrength: Float = 1f,
    val echoTint: Float = 0.5f,
    val innerWallFocus: Float = 0f,
    val pageLight: Float = 0f
)

/**
 * Platform-backed per-frame shader brush factory.
 *
 * Called inside `onDrawBehind` so [progress] / uniform updates never trigger Compose recomposition.
 * Implementations MUST catch all shader compile/runtime exceptions and return `null` on failure.
 */
fun interface LiquidGlassShaderRenderer {
    fun brushForFrame(uniforms: LiquidGlassShaderUniforms): Brush?
}

/**
 * Creates and caches a [LiquidGlassShaderRenderer] for [logoBitmap], or returns `null` when
 * platform shaders are unavailable or fail to compile.
 */
@Composable
expect fun rememberLiquidGlassShaderRenderer(
    logoBitmap: ImageBitmap?
): LiquidGlassShaderRenderer?
