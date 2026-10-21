package com.grimsqueaker.comicpulse.data.repository

import com.grimsqueaker.comicpulse.data.model.ComicBook
import com.grimsqueaker.comicpulse.data.model.ComicBookDetail

/**
 * Repository interface exposing comic data to the ViewModel layer.
 * Using this repository contract hides the implementation details (such as whether
 * the data is loaded from memory, cache, local DB, or scraped online).
 */
interface ComicRepository {
    /**
     * Fetches the latest comic books from getcomics.org.
     *
     * @param page The page number to load (starting from 1).
     * @return A [Result] wrapping the list of [ComicBook] to represent success or failure.
     */
    suspend fun getLatestComics(page: Int = 1, searchQuery: String? = null): Result<List<ComicBook>>

    /**
     * Fetches details and download hosting links of a specific comic book.
     *
     * @param detailUrl The direct getcomics.org URL of the comic.
     * @return A [Result] wrapping the [ComicBookDetail] object.
     */
    suspend fun getComicDetail(detailUrl: String): Result<ComicBookDetail>
}
