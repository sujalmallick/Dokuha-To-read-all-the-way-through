package ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * User-selectable glass quality preference in Appearance Settings.
 */
enum class GlassQualitySetting(val label: String, val description: String) {
    AUTO("Auto", "Adapts to device capability, scrolling, and power saver"),
    FULL("Full", "Live backdrop blur on static overlays; balanced in lists and readers"),
    BALANCED("Balanced", "Tinted glass with specular rim and sheen, no live blur"),
    REDUCED("Reduced", "High-contrast translucent surface without blur")
}

/**
 * Rendering context used by [resolveGlassQuality] so scrolling lists and readers
 * never run per-frame live backdrop blur.
 */
enum class GlassSurfaceContext {
    STANDARD,
    SCROLLING_LIST,
    READER_OVERLAY
}

/**
 * Runtime platform signals affecting automatic glass tier selection.
 */
@Immutable
data class PlatformGlassEnvironment(
    val isIos: Boolean = false,
    val androidSdkInt: Int = 33,
    val isLowPowerMode: Boolean = false,
    val platformReduceTransparency: Boolean = false
)

val LocalGlassQualitySetting = compositionLocalOf { GlassQualitySetting.AUTO }
val LocalPlatformGlassEnvironment = compositionLocalOf { PlatformGlassEnvironment() }

/**
 * Resolves the effective [GlassQuality] tier from user settings, platform state,
 * and surface context.
 *
 * Rules:
 * 1. In-app `reduceGlass`, platform Reduce Transparency, or `REDUCED` setting -> [GlassQuality.REDUCED].
 * 2. `SCROLLING_LIST`, `READER_OVERLAY`, or active `isScrolling` -> never [GlassQuality.FULL] (clamped to [GlassQuality.BALANCED]).
 * 3. `AUTO` drops to [GlassQuality.BALANCED] when Low Power / Battery Saver is active or Android SDK < 31.
 */
fun resolveGlassQuality(
    setting: GlassQualitySetting,
    environment: PlatformGlassEnvironment,
    context: GlassSurfaceContext = GlassSurfaceContext.STANDARD,
    inAppReduceGlass: Boolean = false,
    isScrolling: Boolean = false
): GlassQuality {
    if (inAppReduceGlass || environment.platformReduceTransparency || setting == GlassQualitySetting.REDUCED) {
        return GlassQuality.REDUCED
    }

    val liveBlurForbidden = context == GlassSurfaceContext.SCROLLING_LIST ||
        context == GlassSurfaceContext.READER_OVERLAY ||
        isScrolling

    return when (setting) {
        GlassQualitySetting.REDUCED -> GlassQuality.REDUCED
        GlassQualitySetting.BALANCED -> GlassQuality.BALANCED
        GlassQualitySetting.FULL -> if (liveBlurForbidden) GlassQuality.BALANCED else GlassQuality.FULL
        GlassQualitySetting.AUTO -> when {
            liveBlurForbidden -> GlassQuality.BALANCED
            environment.isLowPowerMode -> GlassQuality.BALANCED
            !environment.isIos && environment.androidSdkInt < 31 -> GlassQuality.BALANCED
            else -> GlassQuality.FULL
        }
    }
}
