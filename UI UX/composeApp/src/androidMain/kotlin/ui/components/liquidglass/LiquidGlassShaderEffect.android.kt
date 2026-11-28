package ui.components.liquidglass

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import core.logging.AppLogger

private const val TAG = "LiquidGlassShaderAndroid"

@Composable
actual fun rememberLiquidGlassShaderRenderer(
    logoBitmap: ImageBitmap?
): LiquidGlassShaderRenderer? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || logoBitmap == null) {
        return null
    }
    return remember(logoBitmap) {
        createAndroidLiquidGlassRenderer(logoBitmap)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createAndroidLiquidGlassRenderer(
    logoBitmap: ImageBitmap
): LiquidGlassShaderRenderer? {
    return runCatching {
        val androidBitmap = logoBitmap.asAndroidBitmap()
        val logoShader = BitmapShader(
            androidBitmap,
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        val runtimeShader = RuntimeShader(LiquidGlassLogoShaderSource.buildShaderSource(bands = 6))
        val brush = ShaderBrush(runtimeShader)
        val logoW = androidBitmap.width.toFloat().coerceAtLeast(1f)
        val logoH = androidBitmap.height.toFloat().coerceAtLeast(1f)

        LiquidGlassShaderRenderer { uniforms ->
            runCatching {
                runtimeShader.setInputShader("uLogo", logoShader)
                runtimeShader.setFloatUniform(
                    "uResolution",
                    uniforms.canvasSize.width.toFloat().coerceAtLeast(1f),
                    uniforms.canvasSize.height.toFloat().coerceAtLeast(1f)
                )
                runtimeShader.setFloatUniform("uLogoPixelSize", logoW, logoH)
                runtimeShader.setFloatUniform("uDpr", uniforms.dpr.coerceIn(1f, 2f))
                runtimeShader.setFloatUniform("uUnit", uniforms.unitPx.coerceAtLeast(1f))
                runtimeShader.setFloatUniform("uLogoBox", uniforms.logoBoxPx.coerceAtLeast(1f), uniforms.logoBoxPx.coerceAtLeast(1f))
                runtimeShader.setFloatUniform("uTime", uniforms.timeSec)
                runtimeShader.setFloatUniform("uSettle", uniforms.settle)
                runtimeShader.setFloatUniform("uArtScale", uniforms.artScale)
                runtimeShader.setFloatUniform("uTile", 0.0235f, 0.047f, 0.094f)
                runtimeShader.setFloatUniform("uTileEnabled", uniforms.tileEnabled)
                runtimeShader.setFloatUniform("uDepth", uniforms.depth)
                runtimeShader.setFloatUniform("uDispersion", uniforms.dispersion)
                runtimeShader.setFloatUniform("uRelief", uniforms.relief)
                runtimeShader.setFloatUniform("uCaustics", uniforms.caustics)
                runtimeShader.setFloatUniform("uHighlights", uniforms.highlights)
                runtimeShader.setFloatUniform("uDropStrength", uniforms.dropStrength)
                runtimeShader.setFloatUniform("uPageLight", uniforms.pageLight)
                brush as Brush
            }.onFailure { err ->
                AppLogger.w(TAG, "AGSL uniform update failed; falling back to Canvas: ${err::class.simpleName}")
            }.getOrNull()
        }
    }.onFailure { err ->
        AppLogger.w(TAG, "AGSL RuntimeShader compile failed; falling back to Canvas: ${err::class.simpleName}")
    }.getOrNull()
}
