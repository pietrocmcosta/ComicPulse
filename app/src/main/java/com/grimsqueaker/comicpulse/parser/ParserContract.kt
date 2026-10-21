package com.grimsqueaker.comicpulse.parser

import com.grimsqueaker.comicpulse.data.model.ComicBook
import com.grimsqueaker.comicpulse.data.model.ComicBookDetail

/**
 * The universal contract that every comic source parser must implement.
 *
 * This is the plug-and-play boundary: built-in parsers implement this directly,
 * and dynamically-loaded parsers are instantiated via reflection and cast to this interface.
 *
 * By decoupling the parsing logic behind this contract, we can:
 *  - Ship the app as a pure offline reader with zero parsers.
 *  - Let the user download and enable parsers at runtime.
 *  - Swap, update, or remove parsers without touching the core app.
 */
interface ParserContract {

    /**
     * Human-readable, unique identifier for this parser (e.g. "GetComics").
     * Used as the key inside the registry and DataStore persistence.
     */
    val id: String

    /**
     * Display name shown in the Settings screen (e.g. "GetComics.org").
     */
    val displayName: String

    /**
     * Short description shown under the toggle switch in Settings.
     */
    val description: String

    /**
     * Semantic version string of this parser (e.g. "1.0.0").
     * Useful for future update-checking logic.
     */
    val version: String

    /**
     * The base URL that this parser targets (e.g. "https://getcomics.org").
     */
    val baseUrl: String

    // ─────────────────────────────────────────
    // Search & Discovery
    // ─────────────────────────────────────────

    /**
     * Fetches and parses the latest comic listings from the source.
     *
     * @param page Pagination index, starting at 1.
     * @param searchQuery Optional search filter text.
     * @return A list of [ComicBook] items scraped from the source.
     */
    suspend fun fetchComics(page: Int = 1, searchQuery: String? = null): List<ComicBook>

    /**
     * Fetches the full detail page for a specific comic.
     *
     * @param detailUrl The URL of the comic detail page.
     * @return A [ComicBookDetail] with description, cover, and download links.
     */
    suspend fun fetchComicDetail(detailUrl: String): ComicBookDetail
}
