package core.logging

import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
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

        activityViewController.popoverPresentationController?.let { popover ->
            popover.sourceView = rootViewController?.view
            popover.sourceRect = rootViewController?.view?.bounds ?: CGRectMake(0.0, 0.0, 0.0, 0.0)
        }

        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }
}
