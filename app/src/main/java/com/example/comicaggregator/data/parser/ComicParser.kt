package com.example.comicaggregator.data.parser

import com.example.comicaggregator.data.model.ComicBook
import com.example.comicaggregator.data.model.ComicBookDetail

/**
 * Interface defining the parsing/scraping contract.
 * By using an interface, we successfully isolate Jsoup (or any other scraper library)
 * from the rest of the application layers, adhering to Clean Architecture principles.
 */
interface ComicParser {
    /**
     * Parses the raw HTML of getcomics.org and extracts a list of [ComicBook] posts.
     *
     * @param html The fetched raw HTML content string of the page.
     * @return A list of cleanly extracted [ComicBook] objects.
     * @throws Exception if parsing fails due to drastic structural layout changes (allowing proper error handling).
     */
    @Throws(Exception::class)
    fun parseLatestComics(html: String): List<ComicBook>

    /**
     * Parses the raw HTML of a specific getcomics.org comic detail page.
     *
     * @param html The fetched raw HTML content string of the detail page.
     * @return Clean [ComicBookDetail] containing description, cover and download links.
     * @throws Exception if parsing fails.
     */
    @Throws(Exception::class)
    fun parseComicDetail(html: String): ComicBookDetail
}
