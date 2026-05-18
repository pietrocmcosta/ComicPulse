package com.example.comicaggregator.data.model

/**
 * Data class representing the detailed view of a Comic post.
 *
 * @property title The full title of the comic book.
 * @property description The plot summary or description text.
 * @property coverImageUrl The high-res cover image URL.
 * @property downloadLinks A map representing server hostnames as keys (e.g. "TERABOX", "PIXELDRAIN")
 *                         and download URLs as values.
 */
data class ComicBookDetail(
    val title: String,
    val description: String,
    val coverImageUrl: String,
    val downloadLinks: Map<String, String>
)
