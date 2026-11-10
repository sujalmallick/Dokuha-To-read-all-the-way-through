package core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun getCacheDirectory(context: PlatformContext): Path {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val cacheDir = paths.firstOrNull() as? String ?: "/tmp"
    return "$cacheDir/dokuha_image_cache".toPath()
}
