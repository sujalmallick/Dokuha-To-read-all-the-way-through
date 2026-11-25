package core.logging

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual object PlatformLogger {
    actual fun getLogDirectory(): Path {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsDirectory = paths.firstOrNull() as? String ?: "/tmp"
        val logPath = "$documentsDirectory/logs"
        NSFileManager.defaultManager.createDirectoryAtPath(logPath, true, null, null)
        return logPath.toPath()
    }

    actual fun shareLogFile(filePath: Path) {
        val fileUrl = NSURL.fileURLWithPath(filePath.toString())
        val activityViewController = UIActivityViewController(listOf(fileUrl), null)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }
}
