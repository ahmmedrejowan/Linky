package com.rejowan.linky.util

data class LinkPreview(
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val url: String,
    val favicon: String? = null,
    val siteName: String? = null
)
