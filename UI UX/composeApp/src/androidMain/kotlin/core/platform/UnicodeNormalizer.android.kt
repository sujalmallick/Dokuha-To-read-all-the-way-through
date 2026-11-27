package core.platform

import java.text.Normalizer

actual fun normalizeUnicodeNfkc(input: String): String {
    return Normalizer.normalize(input, Normalizer.Form.NFKC)
}
