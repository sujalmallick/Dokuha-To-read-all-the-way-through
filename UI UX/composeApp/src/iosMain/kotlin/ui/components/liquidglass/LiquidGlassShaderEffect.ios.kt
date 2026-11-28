package ui.components.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asSkiaBitmap
import core.logging.AppLogger
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode

private const val TAG = "LiquidGlassShaderIos"

@Composable
actual fun rememberLiquidGlassShaderRenderer(
    logoBitmap: ImageBitmap?
): LiquidGlassShaderRenderer? {
    if (logoBitmap == null) return null
    return remember(logoBitmap) {
        createIosLiquidGlassRenderer(logoBitmap)
    }
}

private fun createIosLiquidGlassRenderer(
    logoBitmap: ImageBitmap
): LiquidGlassShaderRenderer? {
    return runCatching {
        val skiaBitmap = logoBitmap.asSkiaBitmap()
        val skiaImage = Image.makeFromBitmap(skiaBitmap)
        val logoShader = skiaImage.makeShader(
            FilterTileMode.CLAMP,
            FilterTileMode.CLAMP,
            SamplingMode.LINEAR
        )
        val effect = RuntimeEffect.makeForShader(LiquidGlassLogoShaderSource.buildShaderSource(bands = 6))
        val builder = RuntimeShaderBuilder(effect)
        val logoW = skiaBitmap.width.toFloat().coerceAtLeast(1f)
        val logoH = skiaBitmap.height.toFloat().coerceAtLeast(1f)

        LiquidGlassShaderRenderer { uniforms ->
            runCatching {
                builder.child("uLogo", logoShader)
                builder.uniform(
                    "uResolution",
                    uniforms.canvasSize.width.toFloat().coerceAtLeast(1f),
                    uniforms.canvasSize.height.toFloat().coerceAtLeast(1f)
                )
                builder.uniform("uLogoPixelSize", logoW, logoH)
                builder.uniform("uDpr", uniforms.dpr.coerceIn(1f, 2f))
                builder.uniform("uUnit", uniforms.unitPx.coerceAtLeast(1f))
                builder.uniform(
                    "uLogoBox",
                    uniforms.logoBoxPx.coerceAtLeast(1f),
                    uniforms.logoBoxPx.coerceAtLeast(1f)
                )
                builder.uniform("uTime", uniforms.timeSec)
                builder.uniform("uSettle", uniforms.settle)
                builder.uniform("uArtScale", uniforms.artScale)
                builder.uniform("uTile", 0.0235f, 0.047f, 0.094f)
                builder.uniform("uTileEnabled", uniforms.tileEnabled)
                builder.uniform("uDepth", uniforms.depth)
                builder.uniform("uDispersion", uniforms.dispersion)
                builder.uniform("uRelief", uniforms.relief)
                builder.uniform("uCaustics", uniforms.caustics)
                builder.uniform("uHighlights", uniforms.highlights)
                builder.uniform("uDropStrength", uniforms.dropStrength)
                builder.uniform("uPageLight", uniforms.pageLight)
                ShaderBrush(builder.makeShader()) as Brush
            }.onFailure { err ->
                AppLogger.w(TAG, "SkSL uniform/shader build failed; falling back to Canvas: ${err::class.simpleName}")
            }.getOrNull()
        }
    }.onFailure { err ->
        AppLogger.w(TAG, "Skia RuntimeEffect compile failed; falling back to Canvas: ${err::class.simpleName}")
    }.getOrNull()
}
