package com.rejowan.linky.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Converts HTML content to Markdown format
 * Handles common HTML elements from reader mode content
 */
object HtmlToMarkdownConverter {

    /**
     * Converts HTML string to Markdown
     * @param html The HTML content to convert
     * @return Markdown formatted string
     */
    fun convert(html: String): String {
        val doc = Jsoup.parse(html)
        val markdown = StringBuilder()

        // Process the body content
        doc.body().let { body ->
            processNode(body, markdown, 0)
        }

        // Clean up the output
        return cleanupMarkdown(markdown.toString())
    }

    private fun processNode(node: Node, output: StringBuilder, depth: Int) {
        when (node) {
            is TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) {
                    // Normalize whitespace: collapse multiple spaces/newlines into single space
                    val normalized = text.replace("\\s+".toRegex(), " ")
                    output.append(normalized)
                }
            }
            is Element -> {
                processElement(node, output, depth)
            }
        }
    }

    private fun processElement(element: Element, output: StringBuilder, depth: Int) {
        when (element.tagName().lowercase()) {
            "h1" -> {
                output.append("\n\n# ")
                processChildren(element, output, depth)
                output.append("\n\n")
            }
            "h2" -> {
                output.append("\n\n## ")
                processChildren(element, output, depth)
                output.append("\n\n")
            }
            "h3" -> {
                output.append("\n\n### ")
                processChildren(element, output, depth)
                output.append("\n\n")
            }
            "h4" -> {
                output.append("\n\n#### ")
                processChildren(element, output, depth)
                output.append("\n\n")
            }
            "h5" -> {
                output.append("\n\n##### ")
                processChildren(element, output, depth)
                output.append("\n\n")
            }
            "h6" -> {
                output.append("\n\n###### ")
                processChildren(element, output, depth)
                output.append("\n\n")
            }
            "p" -> {
                output.append("\n\n")
                processChildren(element, output, depth)
                output.append("\n\n")
            }
            "br" -> {
                output.append("  \n")
            }
            "strong", "b" -> {
                output.append("**")
                processChildren(element, output, depth)
                output.append("**")
            }
            "em", "i" -> {
                output.append("*")
                processChildren(element, output, depth)
                output.append("*")
            }
            "code" -> {
                if (element.parent()?.tagName()?.lowercase() == "pre") {
                    // Code block - handled by pre tag
                    processChildren(element, output, depth)
                } else {
                    // Inline code
                    output.append("`")
                    processChildren(element, output, depth)
                    output.append("`")
                }
            }
            "pre" -> {
                output.append("\n\n```\n")
                processChildren(element, output, depth)
                output.append("\n```\n\n")
            }
            "blockquote" -> {
                output.append("\n\n")
                val quoteContent = StringBuilder()
                processChildren(element, quoteContent, depth)
                // Add > to each line
                quoteContent.toString().lines().forEach { line ->
                    if (line.isNotBlank()) {
                        output.append("> ").append(line).append("\n")
                    }
                }
                output.append("\n")
            }
            "ul" -> {
                output.append("\n\n")
                processChildren(element, output, depth)
                output.append("\n")
            }
            "ol" -> {
                output.append("\n\n")
                var index = 1
                element.children().forEach { child ->
                    if (child.tagName().lowercase() == "li") {
                        output.append("${index}. ")
                        processChildren(child, output, depth + 1)
                        output.append("\n")
                        index++
                    }
                }
                output.append("\n")
            }
            "li" -> {
                // Handle unordered list items (ordered handled in ol case)
                if (element.parent()?.tagName()?.lowercase() == "ul") {
                    output.append("- ")
                    processChildren(element, output, depth + 1)
                    output.append("\n")
                }
            }
            "a" -> {
                val href = element.attr("href")
                output.append("[")
                processChildren(element, output, depth)
                output.append("](").append(href).append(")")
            }
            "img" -> {
                val src = element.attr("src")
                val alt = element.attr("alt").ifEmpty { "image" }
                output.append("\n\n![").append(alt).append("](").append(src).append(")\n\n")
            }
            "hr" -> {
                output.append("\n\n---\n\n")
            }
            "table" -> {
                // Basic table support
                output.append("\n\n")
                processChildren(element, output, depth)
                output.append("\n")
            }
            "thead", "tbody", "tfoot" -> {
                processChildren(element, output, depth)
            }
            "tr" -> {
                output.append("| ")
                element.children().forEach { cell ->
                    processChildren(cell, output, depth)
                    output.append(" | ")
                }
                output.append("\n")
                // Add separator after header row
                if (element.parent()?.tagName()?.lowercase() == "thead") {
                    element.children().forEach { _ ->
                        output.append("| --- ")
                    }
                    output.append("|\n")
                }
            }
            "th", "td" -> {
                processChildren(element, output, depth)
            }
            "div", "article", "section", "main", "span" -> {
                // Pass through containers
                processChildren(element, output, depth)
            }
            else -> {
                // For unknown elements, just process children
                processChildren(element, output, depth)
            }
        }
    }

    private fun processChildren(element: Element, output: StringBuilder, depth: Int) {
        element.childNodes().forEach { child ->
            processNode(child, output, depth)
        }
    }

    /**
     * Clean up the generated markdown
     * - Collapse excessive newlines (max 2 consecutive)
     * - Remove trailing spaces on lines
     * - Trim overall content
     */
    private fun cleanupMarkdown(markdown: String): String {
        return markdown
            // Collapse more than 2 consecutive newlines into 2
            .replace(Regex("\n{3,}"), "\n\n")
            // Remove trailing spaces at end of lines
            .replace(Regex(" +\n"), "\n")
            // Remove space before punctuation
            .replace(Regex(" +([.,!?;:])"), "$1")
            // Ensure space after punctuation if followed by word
            .replace(Regex("([.,!?;:])([A-Za-z])"), "$1 $2")
            .trim()
    }
}
