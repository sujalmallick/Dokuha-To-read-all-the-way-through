package core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NovelTextSanitizerTest {

    @Test
    fun testStripHtmlTags() {
        val input = "<p>Hello <b>World</b>!</p><br/><div>This is a test.</div>"
        val sanitized = NovelTextSanitizer.sanitizeForSpeech(input)
        assertEquals("Hello World! This is a test.", sanitized)
    }

    @Test
    fun testDecodeHtmlEntities() {
        val input = "Tom &amp; Jerry &lt;friends&gt; &#39;Forever&#39; &quot;Classic&quot;"
        val sanitized = NovelTextSanitizer.sanitizeForSpeech(input)
        assertEquals("Tom & Jerry <friends> 'Forever' \"Classic\"", sanitized)
    }

    @Test
    fun testStripMarkdownArtifacts() {
        val input = "### Chapter 1: The Beginning\n\n**Bold move**, said the *warrior*. [Source](http://example.com) and ![Cover](img.png)"
        val sanitized = NovelTextSanitizer.sanitizeForSpeech(input)
        assertTrue(sanitized.contains("Chapter 1: The Beginning"))
        assertTrue(sanitized.contains("Bold move, said the warrior."))
        assertTrue(sanitized.contains("Source and Cover"))
        assertTrue(!sanitized.contains("###"))
        assertTrue(!sanitized.contains("**"))
        assertTrue(!sanitized.contains("http://example.com"))
    }

    @Test
    fun testSanitizeParagraphs() {
        val input = """
            <p>Paragraph one.</p>
            
            <p>   </p>
            
            <p>Paragraph two with <i>emphasis</i>.</p>
        """.trimIndent()
        val paragraphs = NovelTextSanitizer.sanitizeParagraphs(input)
        assertEquals(2, paragraphs.size)
        assertEquals("Paragraph one.", paragraphs[0])
        assertEquals("Paragraph two with emphasis.", paragraphs[1])
    }
}
