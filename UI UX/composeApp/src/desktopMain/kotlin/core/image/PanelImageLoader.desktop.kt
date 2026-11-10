package core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

actual fun getCacheDirectory(context: PlatformContext): Path {
    val dir = File(System.getProperty("user.home"), ".config/dokuha/cache/images")
    dir.mkdirs()
    return dir.absolutePath.toPath()
}
