package core.crash

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.popoverPresentationController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ObsoleteWorkersApi

/**
 * Installs a Kotlin/Native unhandled exception hook that persists crash traces
 * (exception type, message, stack trace — no personal data) to NSDocumentDirectory
 * so the next launch can surface a diagnostic screen.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
object IosCrashHandler {
    private const val CRASH_FILE_NAME = "dokuha_last_crash.txt"
    private val fs = FileSystem.SYSTEM
    private var hookInstalled = false

    private fun crashFilePath(): Path {
        val docsUrl = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL
        val basePath = docsUrl?.path ?: "/tmp"
        return "$basePath/$CRASH_FILE_NAME".toPath()
    }

    fun install() {
        if (hookInstalled) return
        hookInstalled = true
        val previousHook = kotlin.native.getUnhandledExceptionHook()
        setUnhandledExceptionHook { throwable ->
            runCatching {
                val trace = buildString {
                    appendLine("${throwable::class.simpleName ?: "Throwable"}: ${throwable.message ?: ""}")
                    appendLine(throwable.stackTraceToString())
                }
                val path = crashFilePath()
                fs.write(path) { writeUtf8(trace) }
            }
            previousHook?.invoke(throwable)
        }
    }

    fun readLastCrash(): String? {
        val path = crashFilePath()
        if (!fs.exists(path)) return null
        return runCatching {
            fs.read(path) { readUtf8() }.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun clearLastCrash() {
        val path = crashFilePath()
        runCatching {
            if (fs.exists(path)) fs.delete(path)
        }
    }

    fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    fun shareCrashTrace(text: String) {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val activityViewController = UIActivityViewController(listOf(text), null)
        activityViewController.popoverPresentationController?.let { popover ->
            popover.sourceView = rootViewController.view
            popover.sourceRect = rootViewController.view?.bounds ?: CGRectMake(0.0, 0.0, 0.0, 0.0)
        }
        rootViewController.presentViewController(activityViewController, animated = true, completion = null)
    }
}
