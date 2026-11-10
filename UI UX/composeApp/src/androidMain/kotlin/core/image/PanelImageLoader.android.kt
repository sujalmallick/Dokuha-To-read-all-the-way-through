package core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath

actual fun getCacheDirectory(context: PlatformContext): Path {
    return context.cacheDir.resolve("dokuha_image_cache").absolutePath.toPath()
}
