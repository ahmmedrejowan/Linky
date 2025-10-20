package com.rejowan.linky.util

/**
 * Represents clean, reader-friendly content extracted from a webpage
 */
data class ReaderContent(
    val title: String,
    val author: String?,
    val content: String,              // Clean Markdown content
    val excerpt: String,
    val wordCount: Int,
    val estimatedReadTime: Int,       // In minutes
    val siteName: String?
)
