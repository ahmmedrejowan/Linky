package com.rejowan.linky.util

import org.junit.Assert.*
import org.junit.Test

class HtmlToMarkdownConverterTest {

    // Heading tests
    @Test
    fun `convert converts h1 to markdown heading`() {
        val html = "<h1>Title</h1>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("# Title"))
    }

    @Test
    fun `convert converts h2 to markdown heading`() {
        val html = "<h2>Subtitle</h2>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("## Subtitle"))
    }

    @Test
    fun `convert converts h3 to markdown heading`() {
        val html = "<h3>Section</h3>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("### Section"))
    }

    @Test
    fun `convert converts h4 to markdown heading`() {
        val html = "<h4>Subsection</h4>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("#### Subsection"))
    }

    @Test
    fun `convert converts h5 to markdown heading`() {
        val html = "<h5>Minor section</h5>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("##### Minor section"))
    }

    @Test
    fun `convert converts h6 to markdown heading`() {
        val html = "<h6>Tiny section</h6>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("###### Tiny section"))
    }

    // Paragraph tests
    @Test
    fun `convert converts paragraph to markdown`() {
        val html = "<p>This is a paragraph.</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("This is a paragraph."))
    }

    @Test
    fun `convert handles multiple paragraphs`() {
        val html = "<p>First paragraph.</p><p>Second paragraph.</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("First paragraph."))
        assertTrue(result.contains("Second paragraph."))
    }

    // Text formatting tests
    @Test
    fun `convert converts strong to bold markdown`() {
        val html = "<p>This is <strong>bold</strong> text.</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("**bold**"))
    }

    @Test
    fun `convert converts b to bold markdown`() {
        val html = "<p>This is <b>bold</b> text.</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("**bold**"))
    }

    @Test
    fun `convert converts em to italic markdown`() {
        val html = "<p>This is <em>italic</em> text.</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("*italic*"))
    }

    @Test
    fun `convert converts i to italic markdown`() {
        val html = "<p>This is <i>italic</i> text.</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("*italic*"))
    }

    // Code tests
    @Test
    fun `convert converts inline code`() {
        val html = "<p>Use <code>println()</code> to print.</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("`println()`"))
    }

    @Test
    fun `convert converts code block`() {
        val html = "<pre><code>fun main() {\n    println(\"Hello\")\n}</code></pre>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("```"))
        assertTrue(result.contains("fun main()"))
    }

    // List tests
    @Test
    fun `convert converts unordered list`() {
        val html = "<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("- Item 1"))
        assertTrue(result.contains("- Item 2"))
        assertTrue(result.contains("- Item 3"))
    }

    @Test
    fun `convert converts ordered list`() {
        val html = "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("1. First"))
        assertTrue(result.contains("2. Second"))
        assertTrue(result.contains("3. Third"))
    }

    // Link tests
    @Test
    fun `convert converts links`() {
        val html = "<a href=\"https://example.com\">Example</a>"
        val result = HtmlToMarkdownConverter.convert(html)
        // Check that the link text is present
        assertTrue(result.contains("Example"))
    }

    @Test
    fun `convert converts links with nested content`() {
        val html = "<a href=\"https://example.com\"><strong>Bold Link</strong></a>"
        val result = HtmlToMarkdownConverter.convert(html)
        // Check that the link content is present
        assertTrue(result.contains("Bold Link"))
    }

    // Image tests
    @Test
    fun `convert converts images`() {
        val html = "<img src=\"https://example.com/image.png\" alt=\"Alt text\">"
        val result = HtmlToMarkdownConverter.convert(html)
        // Check that image markdown syntax is used
        assertTrue(result.contains("!"))
    }

    @Test
    fun `convert handles images without alt text`() {
        val html = "<img src=\"https://example.com/image.png\">"
        val result = HtmlToMarkdownConverter.convert(html)
        // Check that image markdown syntax is used
        assertTrue(result.contains("!"))
    }

    // Blockquote tests
    @Test
    fun `convert converts blockquotes`() {
        val html = "<blockquote>This is a quote.</blockquote>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("> This is a quote."))
    }

    @Test
    fun `convert converts multiline blockquotes`() {
        val html = "<blockquote><p>First line.</p><p>Second line.</p></blockquote>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains(">"))
    }

    // Horizontal rule tests
    @Test
    fun `convert converts horizontal rule`() {
        val html = "<hr>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("---"))
    }

    // Line break tests
    @Test
    fun `convert converts br tags`() {
        val html = "<p>Line 1<br>Line 2</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("Line 1"))
        assertTrue(result.contains("Line 2"))
    }

    // Table tests
    @Test
    fun `convert converts tables`() {
        val html = """
            <table>
                <thead>
                    <tr><th>Header 1</th><th>Header 2</th></tr>
                </thead>
                <tbody>
                    <tr><td>Cell 1</td><td>Cell 2</td></tr>
                </tbody>
            </table>
        """.trimIndent()
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("|"))
        assertTrue(result.contains("Header 1"))
        assertTrue(result.contains("Cell 1"))
    }

    // Container tests
    @Test
    fun `convert passes through div content`() {
        val html = "<div>Content in div</div>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("Content in div"))
    }

    @Test
    fun `convert passes through article content`() {
        val html = "<article><p>Article content</p></article>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("Article content"))
    }

    @Test
    fun `convert passes through section content`() {
        val html = "<section><p>Section content</p></section>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("Section content"))
    }

    @Test
    fun `convert passes through main content`() {
        val html = "<main><p>Main content</p></main>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("Main content"))
    }

    @Test
    fun `convert passes through span content`() {
        val html = "<span>Span content</span>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("Span content"))
    }

    // Cleanup tests
    @Test
    fun `convert collapses excessive newlines`() {
        val html = "<p>Para 1</p><p></p><p></p><p></p><p>Para 2</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertFalse(result.contains("\n\n\n"))
    }

    @Test
    fun `convert trims output`() {
        val html = "  <p>  Content  </p>  "
        val result = HtmlToMarkdownConverter.convert(html)
        assertFalse(result.startsWith(" "))
        assertFalse(result.endsWith(" "))
    }

    // Complex document tests
    @Test
    fun `convert handles complex document structure`() {
        val html = """
            <h1>Title</h1>
            <p>Introduction paragraph with <strong>bold</strong> and <em>italic</em> text.</p>
            <h2>Section 1</h2>
            <p>Some content with a <a href="https://example.com">link</a>.</p>
            <ul>
                <li>Item 1</li>
                <li>Item 2</li>
            </ul>
            <blockquote>A famous quote.</blockquote>
            <pre><code>code block</code></pre>
        """.trimIndent()

        val result = HtmlToMarkdownConverter.convert(html)

        assertTrue(result.contains("# Title"))
        assertTrue(result.contains("**bold**"))
        assertTrue(result.contains("*italic*"))
        assertTrue(result.contains("## Section 1"))
        assertTrue(result.contains("link"))
        assertTrue(result.contains("- Item 1"))
        assertTrue(result.contains("```"))
    }

    @Test
    fun `convert normalizes whitespace in text nodes`() {
        val html = "<p>Text with     multiple    spaces</p>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.contains("Text with multiple spaces"))
        assertFalse(result.contains("     "))
    }

    @Test
    fun `convert handles empty html`() {
        val html = ""
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `convert handles html without body`() {
        val html = "<html><head></head></html>"
        val result = HtmlToMarkdownConverter.convert(html)
        assertTrue(result.isEmpty())
    }
}
