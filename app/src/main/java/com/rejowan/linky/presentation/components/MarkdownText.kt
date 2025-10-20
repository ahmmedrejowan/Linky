package com.rejowan.linky.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import coil.compose.AsyncImage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import timber.log.Timber
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/**
 * Renders Markdown content with proper styling
 * @param markdown The Markdown text to render
 * @param modifier Modifier for the composable
 * @param fontSize Base font size for the text
 * @param lineHeight Line height multiplier (default 1.6)
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    lineHeight: Float = 1.6f
) {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)

    // Get theme colors
    val linkColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val codeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val imageTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        RenderMarkdownNode(
            node = parsedTree,
            content = markdown,
            fontSize = fontSize,
            lineHeight = lineHeight,
            linkColor = linkColor,
            codeBackgroundColor = codeBackgroundColor,
            imageTextColor = imageTextColor
        )
    }
}

@Composable
private fun RenderMarkdownNode(
    node: ASTNode,
    content: String,
    fontSize: TextUnit,
    lineHeight: Float,
    linkColor: androidx.compose.ui.graphics.Color,
    codeBackgroundColor: androidx.compose.ui.graphics.Color,
    imageTextColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE -> {
            Column(modifier = modifier) {
                node.children.forEach { child ->
                    RenderMarkdownNode(child, content, fontSize, lineHeight, linkColor, codeBackgroundColor, imageTextColor)
                }
            }
        }
        MarkdownElementTypes.PARAGRAPH -> {
            val uriHandler = LocalUriHandler.current

            // Collect image URLs from this paragraph
            val imageUrls = mutableListOf<String>()
            collectImageUrls(node, content, imageUrls)

            val annotatedText = buildAnnotatedString {
                processInlineContent(node, content, this, linkColor, codeBackgroundColor, imageTextColor)
            }

            // Render text content
            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize,
                    fontFamily = FontFamily.Serif,
                    lineHeight = (fontSize.value * lineHeight).sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = if (imageUrls.isEmpty()) 12.dp else 4.dp),
                onClick = { offset ->
                    // Find URL annotations at click position
                    annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                Timber.d("MarkdownText: Opening URL: ${annotation.item}")
                                uriHandler.openUri(annotation.item)
                            } catch (e: Exception) {
                                Timber.e(e, "MarkdownText: Failed to open URL: ${annotation.item}")
                            }
                        }
                }
            )

            // Render images if any
            imageUrls.forEach { imageUrl ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Article image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            if (imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        MarkdownElementTypes.ATX_1 -> {
            val text = node.getTextInNode(content).toString().removePrefix("#").trim()
            Text(
                text = text,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (fontSize.value + 12).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }
        MarkdownElementTypes.ATX_2 -> {
            val text = node.getTextInNode(content).toString().removePrefix("##").trim()
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (fontSize.value + 8).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
            )
        }
        MarkdownElementTypes.ATX_3 -> {
            val text = node.getTextInNode(content).toString().removePrefix("###").trim()
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (fontSize.value + 4).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }
        MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> {
            val text = node.getTextInNode(content).toString()
                .replace(Regex("^#+\\s*"), "")
                .trim()
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (fontSize.value + 2).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
        }
        MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                node.children.forEach { child ->
                    RenderMarkdownNode(child, content, fontSize, lineHeight, linkColor, codeBackgroundColor, imageTextColor)
                }
            }
        }
        MarkdownElementTypes.LIST_ITEM -> {
            val text = buildAnnotatedString {
                processInlineContent(node, content, this, linkColor, codeBackgroundColor, imageTextColor)
            }
            Text(
                text = "• $text",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize,
                    fontFamily = FontFamily.Serif,
                    lineHeight = (fontSize.value * lineHeight).sp
                ),
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
        }
        MarkdownElementTypes.BLOCK_QUOTE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp)
                    .padding(bottom = 8.dp)
            ) {
                Column {
                    node.children.forEach { child ->
                        RenderMarkdownNode(child, content, fontSize, lineHeight, linkColor, codeBackgroundColor, imageTextColor)
                    }
                }
            }
        }
        MarkdownElementTypes.CODE_FENCE, MarkdownElementTypes.CODE_BLOCK -> {
            val codeText = node.getTextInNode(content).toString()
                .replace(Regex("^```[a-z]*\\n"), "")
                .replace(Regex("```$"), "")
                .trim()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    text = codeText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (fontSize.value - 2).sp
                    )
                )
            }
        }
        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
        else -> {
            // For other node types, recursively process children
            node.children.forEach { child ->
                RenderMarkdownNode(child, content, fontSize, lineHeight, linkColor, codeBackgroundColor, imageTextColor)
            }
        }
    }
}

private fun processInlineContent(
    node: ASTNode,
    content: String,
    builder: androidx.compose.ui.text.AnnotatedString.Builder,
    linkColor: androidx.compose.ui.graphics.Color,
    codeBackgroundColor: androidx.compose.ui.graphics.Color,
    imageTextColor: androidx.compose.ui.graphics.Color
) {
    var skipUntilCloseParen = false
    var linkUrl: String? = null
    val urlBuilder = StringBuilder()

    node.children.forEachIndexed { index, child ->
        Timber.d("processInlineContent: Processing child type: ${child.type}, skipMode: $skipUntilCloseParen")

        // If we're in skip mode (collecting URL), check if we hit the closing paren
        if (skipUntilCloseParen) {
            val tokenString = child.type.toString()
            when {
                tokenString == "Markdown:)" -> {
                    linkUrl = urlBuilder.toString()
                    Timber.d("processInlineContent: Collected URL: $linkUrl")
                    skipUntilCloseParen = false
                    urlBuilder.clear()
                    return@forEachIndexed // Skip the closing paren
                }
                child.type == MarkdownTokenTypes.TEXT -> {
                    urlBuilder.append(child.getTextInNode(content))
                    return@forEachIndexed // Skip URL text
                }
                else -> {
                    // Skip other URL parts like : and whitespace
                    if (tokenString == "Markdown::") {
                        urlBuilder.append(":")
                    }
                    return@forEachIndexed
                }
            }
        }

        when (child.type) {
            MarkdownTokenTypes.TEXT -> {
                val text = child.getTextInNode(content).toString()
                Timber.d("processInlineContent: TEXT node content: '$text'")
                builder.append(text)
            }
            MarkdownTokenTypes.WHITE_SPACE -> {
                // Preserve whitespace - it's needed for spacing between words
                builder.append(" ")
            }
            MarkdownElementTypes.STRONG -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    processInlineContent(child, content, builder, linkColor, codeBackgroundColor, imageTextColor)
                }
            }
            MarkdownElementTypes.EMPH -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    processInlineContent(child, content, builder, linkColor, codeBackgroundColor, imageTextColor)
                }
            }
            MarkdownElementTypes.CODE_SPAN -> {
                builder.withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackgroundColor
                    )
                ) {
                    val codeText = child.getTextInNode(content).toString()
                        .removePrefix("`")
                        .removeSuffix("`")
                    builder.append(codeText)
                }
            }
            MarkdownElementTypes.INLINE_LINK, MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
                Timber.d("processInlineContent: LINK detected (${child.type})")
                Timber.d("processInlineContent: Link full text: '${child.getTextInNode(content)}'")

                // For SHORT_REFERENCE_LINK format: [text](url) or [text]
                // Structure: LINK_LABEL contains [text], followed by url parts as SIBLINGS
                val linkLabelNode = child.children.find { it.type == MarkdownElementTypes.LINK_LABEL }

                if (linkLabelNode != null) {
                    Timber.d("processInlineContent: LINK_LABEL found")
                    // Extract text from LINK_LABEL (recursively to handle bold/italic)
                    val linkText = extractTextRecursively(linkLabelNode, content)
                    Timber.d("processInlineContent: Extracted link text: '$linkText'")

                    // Look ahead to collect URL before adding link annotation
                    var collectedUrl = ""
                    var lookAheadIndex = index + 1
                    if (lookAheadIndex < node.children.size &&
                        node.children[lookAheadIndex].type.toString() == "Markdown:(") {
                        Timber.d("processInlineContent: Looking ahead to collect URL")
                        val tempUrlBuilder = StringBuilder()
                        lookAheadIndex++ // Skip the opening paren

                        while (lookAheadIndex < node.children.size) {
                            val lookAheadChild = node.children[lookAheadIndex]
                            val tokenString = lookAheadChild.type.toString()

                            if (tokenString == "Markdown:)") {
                                break // Found closing paren
                            } else if (lookAheadChild.type == MarkdownTokenTypes.TEXT) {
                                tempUrlBuilder.append(lookAheadChild.getTextInNode(content))
                            } else if (tokenString == "Markdown::") {
                                tempUrlBuilder.append(":")
                            }
                            lookAheadIndex++
                        }
                        collectedUrl = tempUrlBuilder.toString()
                        Timber.d("processInlineContent: Collected URL via lookahead: $collectedUrl")

                        // Set flag to skip these nodes when we encounter them in the main loop
                        skipUntilCloseParen = true
                        urlBuilder.clear()
                    }

                    // Add clickable link with URL annotation
                    builder.pushStringAnnotation(tag = "URL", annotation = collectedUrl)
                    builder.withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        builder.append(linkText)
                    }
                    builder.pop()
                } else {
                    // Fallback: try LINK_TEXT for INLINE_LINK format
                    val linkTextNode = child.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                    Timber.d("processInlineContent: LINK_TEXT found: ${linkTextNode != null}")

                    if (linkTextNode != null) {
                        builder.withStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            processInlineContent(linkTextNode, content, builder, linkColor, codeBackgroundColor, imageTextColor)
                        }
                    } else {
                        Timber.w("processInlineContent: No LINK_LABEL or LINK_TEXT found, skipping link")
                        // Don't render anything to avoid showing markdown syntax
                    }
                }
            }
            MarkdownElementTypes.IMAGE -> {
                Timber.d("processInlineContent: IMAGE detected")

                // Look ahead to skip URL parts (similar to links)
                var lookAheadIndex = index + 1
                if (lookAheadIndex < node.children.size &&
                    node.children[lookAheadIndex].type.toString() == "Markdown:(") {
                    Timber.d("processInlineContent: Image has URL, will skip it")
                    skipUntilCloseParen = true
                    urlBuilder.clear()
                }

                // Don't append [Image] placeholder since actual images are rendered separately
                // The collectImageUrls() function handles displaying the actual images
            }
            // Skip markdown syntax tokens that are handled by parent nodes
            MarkdownElementTypes.LINK_LABEL -> {
                Timber.d("processInlineContent: Skipping LINK_LABEL (handled by parent)")
                // Skip - already handled in SHORT_REFERENCE_LINK
            }
            else -> {
                // Skip markdown syntax characters
                val tokenString = child.type.toString()

                // Check if this is the opening paren after a link (start of URL)
                if (tokenString == "Markdown:(") {
                    // This will be caught by the skipUntilCloseParen logic if it follows a link
                    // If we reach here and skipUntilCloseParen is false, it might be a standalone paren
                    Timber.d("processInlineContent: Skipping opening paren (start of URL or syntax)")
                    return@forEachIndexed
                }

                // Skip markdown syntax tokens (but NOT whitespace - that's handled above)
                if (tokenString.matches(Regex("Markdown:[\\[\\]().]"))) {
                    Timber.d("processInlineContent: Skipping syntax token: ${child.type}")
                    // Skip these tokens - they're markdown syntax
                } else {
                    Timber.d("processInlineContent: Processing unknown type: ${child.type}")
                    processInlineContent(child, content, builder, linkColor, codeBackgroundColor, imageTextColor)
                }
            }
        }
    }
}

/**
 * Recursively extract all text content from a node and its descendants
 * This handles nested formatting like **bold** or *italic* within link text
 */
private fun extractTextRecursively(node: ASTNode, content: String): String {
    val textBuilder = StringBuilder()

    fun traverse(n: ASTNode) {
        if (n.type == MarkdownTokenTypes.TEXT) {
            textBuilder.append(n.getTextInNode(content))
        } else {
            // Recursively process children
            n.children.forEach { child ->
                traverse(child)
            }
        }
    }

    traverse(node)
    return textBuilder.toString()
}

/**
 * Collect all image URLs from a paragraph node
 * Images appear as IMAGE nodes followed by (url) tokens as siblings
 */
private fun collectImageUrls(node: ASTNode, content: String, imageUrls: MutableList<String>) {
    var i = 0
    while (i < node.children.size) {
        val child = node.children[i]

        if (child.type == MarkdownElementTypes.IMAGE) {
            Timber.d("collectImageUrls: Found IMAGE node at index $i")

            // Look ahead for the URL (next sibling should be '(')
            var lookAheadIndex = i + 1
            if (lookAheadIndex < node.children.size &&
                node.children[lookAheadIndex].type.toString() == "Markdown:(") {

                val urlBuilder = StringBuilder()
                lookAheadIndex++ // Skip opening paren

                // Collect URL parts
                while (lookAheadIndex < node.children.size) {
                    val urlChild = node.children[lookAheadIndex]
                    val tokenString = urlChild.type.toString()

                    when {
                        tokenString == "Markdown:)" -> break
                        urlChild.type == MarkdownTokenTypes.TEXT -> {
                            urlBuilder.append(urlChild.getTextInNode(content))
                        }
                        tokenString == "Markdown::" -> urlBuilder.append(":")
                    }
                    lookAheadIndex++
                }

                val imageUrl = urlBuilder.toString()
                if (imageUrl.isNotBlank()) {
                    Timber.d("collectImageUrls: Collected image URL: $imageUrl")
                    imageUrls.add(imageUrl)
                }
            }
        }
        i++
    }
}
