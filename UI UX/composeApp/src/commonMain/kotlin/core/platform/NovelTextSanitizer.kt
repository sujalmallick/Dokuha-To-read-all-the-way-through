package core.platform

object NovelTextSanitizer {
    private val markdownSymbolsRegex = Regex("""^(\s*[*_\-#>~=]{2,}\s*)$""", RegexOption.MULTILINE)
    private val blockHtmlTagsRegex = Regex("""(?i)</?(p|div|br|hr|h[1-6]|li|blockquote)[^>]*>""")
    private val otherHtmlTagsRegex = Regex("""<[^>]*>""")
    private val multipleSpacesRegex = Regex("""\s+""")

    fun cleanForSpeech(raw: String): String = sanitizeForSpeech(raw)

    fun sanitizeForSpeech(raw: String): String {
        var text = raw.trim()
        if (text.isBlank()) return ""

        // Skip pure decorative dividers like *** or ---
        if (markdownSymbolsRegex.matches(text)) return ""

        // Markdown images and links: ![Cover](img.png) -> Cover, [Source](url) -> Source
        text = text.replace(Regex("""!\[(.*?)\]\(.*?\)"""), "$1")
        text = text.replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")

        // Heading marks at start of line
        text = text.replace(Regex("""^#{1,6}\s*""", RegexOption.MULTILINE), "")

        // Replace block tags with a space so words don't run together, then strip remaining tags
        // (Must be done before decoding &lt; and &gt; entities)
        text = text.replace(blockHtmlTagsRegex, " ")
        text = text.replace(otherHtmlTagsRegex, "")

        // Decode common HTML entities after HTML tags have been stripped
        text = text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")

        // Clean bracket decorators: [Status Window] -> Status Window
        text = text.replace(Regex("""\[(.*?)\]"""), "$1")
        text = text.replace(Regex("""\{(.*?)\}"""), "$1")

        // Clean markdown bold/italic/strikethrough decorators
        text = text.replace(Regex("""[*_~]{1,3}"""), "")

        // Normalize multiple spaces
        text = text.replace(multipleSpacesRegex, " ").trim()

        return text
    }

    fun sanitizeParagraphs(raw: String): List<String> {
        return raw.split(Regex("""(?i)</p>|<br\s*/?>|\r?\n\s*\r?\n|\n"""))
            .map { sanitizeForSpeech(it) }
            .filter { it.isNotBlank() }
    }
}
