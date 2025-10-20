package com.rejowan.linky.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    Column(modifier = modifier) {
        RenderMarkdownNode(
            node = parsedTree,
            content = markdown,
            fontSize = fontSize,
            lineHeight = lineHeight,
            linkColor = linkColor,
            codeBackgroundColor = codeBackgroundColor
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
    modifier: Modifier = Modifier
) {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE -> {
            Column(modifier = modifier) {
                node.children.forEach { child ->
                    RenderMarkdownNode(child, content, fontSize, lineHeight, linkColor, codeBackgroundColor)
                }
            }
        }
        MarkdownElementTypes.PARAGRAPH -> {
            val text = buildAnnotatedString {
                processInlineContent(node, content, this, linkColor, codeBackgroundColor)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize,
                    fontFamily = FontFamily.Serif,
                    lineHeight = (fontSize.value * lineHeight).sp
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
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
                    RenderMarkdownNode(child, content, fontSize, lineHeight, linkColor, codeBackgroundColor)
                }
            }
        }
        MarkdownElementTypes.LIST_ITEM -> {
            val text = buildAnnotatedString {
                processInlineContent(node, content, this, linkColor, codeBackgroundColor)
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
                        RenderMarkdownNode(child, content, fontSize, lineHeight, linkColor, codeBackgroundColor)
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
                RenderMarkdownNode(child, content, fontSize, lineHeight, linkColor, codeBackgroundColor)
            }
        }
    }
}

private fun processInlineContent(
    node: ASTNode,
    content: String,
    builder: androidx.compose.ui.text.AnnotatedString.Builder,
    linkColor: androidx.compose.ui.graphics.Color,
    codeBackgroundColor: androidx.compose.ui.graphics.Color
) {
    node.children.forEach { child ->
        when (child.type) {
            MarkdownTokenTypes.TEXT -> {
                builder.append(child.getTextInNode(content).toString())
            }
            MarkdownElementTypes.STRONG -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    processInlineContent(child, content, builder, linkColor, codeBackgroundColor)
                }
            }
            MarkdownElementTypes.EMPH -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    processInlineContent(child, content, builder, linkColor, codeBackgroundColor)
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
            MarkdownElementTypes.INLINE_LINK -> {
                // Extract link text (first child is usually the text)
                val linkTextNode = child.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                if (linkTextNode != null) {
                    builder.withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        processInlineContent(linkTextNode, content, builder, linkColor, codeBackgroundColor)
                    }
                } else {
                    // Fallback if structure is different
                    builder.withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        processInlineContent(child, content, builder, linkColor, codeBackgroundColor)
                    }
                }
            }
            MarkdownElementTypes.IMAGE -> {
                // Extract alt text and URL
                val imageText = child.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                val altText = if (imageText != null) {
                    imageText.getTextInNode(content).toString()
                } else {
                    "[Image]"
                }
                builder.append("\n")
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    builder.append(altText)
                }
                builder.append("\n")
            }
            else -> {
                processInlineContent(child, content, builder, linkColor, codeBackgroundColor)
            }
        }
    }
}
