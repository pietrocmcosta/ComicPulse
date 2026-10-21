package com.grimsqueaker.comicpulse.data.model

/**
 * Data class representing a parsed Comic post from getcomics.org.
 * Fully prepared to be sent to UI, saved into local Database, or serialized.
 *
 * @property title The main title of the comic issue or pack (e.g. "Absolute Batman #20 (2026)").
 * @property detailUrl The link pointing to the full page on getcomics.org for downloading/viewing details.
 * @property coverImageUrl The HTTP/HTTPS secure cover image URL.
 * @property year Optional year of release parsed from the post metadata (adds high-fidelity context to UI).
 * @property size Optional file size parsed from the post metadata (adds high-fidelity context to UI).
 */
data class ComicBook(
    val title: String,
    val detailUrl: String,
    val coverImageUrl: String,
    val year: String? = null,
    val size: String? = null,
    val category: String? = null
)
