package core.platform

object NovelTextSanitizer {
    private val markdownSymbolsRegex = Regex("""^(\s*[*_\-#>~=]{2,}\s*)$""", RegexOption.MULTILINE)
    private val htmlTagsRegex = Regex("""<[^>]*>""")
    private val multipleSpacesRegex = Regex("""\s+""")

    /**
     * Cleans novel paragraph text for speech synthesis:
     * - Strips markdown dividers and heading marks
     * - Removes raw HTML tags
     * - Normalizes brackets [System: ...] -> System: ...
     * - Formats long ellipses (...) to brief natural pause
     */
    fun cleanForSpeech(raw: String): String {
        var text = raw.trim()
        if (text.isBlank()) return ""

        // Skip pure decorative dividers like *** or ---
        if (markdownSymbolsRegex.matches(text)) return ""

        // Strip HTML tags if present
        text = text.replace(htmlTagsRegex, " ")

        // Clean bracket decorators often used in light novels: [Status Window] -> Status Window
        text = text.replace(Regex("""\[(.*?)\]"""), "$1")
        text = text.replace(Regex("""\{(.*?)\}"""), "$1")

        // Clean markdown bold/italic decorators
        text = text.replace(Regex("""[*_~]{1,3}"""), "")

        // Normalize multiple spaces
        text = text.replace(multipleSpacesRegex, " ").trim()

        return text
    }
}
