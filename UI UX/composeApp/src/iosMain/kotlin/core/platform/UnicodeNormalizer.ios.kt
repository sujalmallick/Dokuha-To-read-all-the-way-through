package core.platform

import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCompatibilityMapping

@Suppress("CAST_NEVER_SUCCEEDS")
actual fun normalizeUnicodeNfkc(input: String): String {
    return (input as NSString).precomposedStringWithCompatibilityMapping
}
