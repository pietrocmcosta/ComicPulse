package com.grimsqueaker.comicpulse.ui.state

import com.grimsqueaker.comicpulse.data.model.ComicBook

/**
 * Sealed interface representing the unidirectional reactive UI states.
 * Adheres to standard Compose state management patterns.
 */
sealed interface ComicUiState {
    
    /**
     * Represents the active fetching/scraping loading state.
     */
    object Loading : ComicUiState

    /**
     * Represents a successful network fetch and scraping result.
     * @property comics The clean list of [ComicBook] objects returned to the UI.
     * @property isFetchingMore Whether we are currently fetching a successive page of comics.
     * @property isLastPage Whether the scrapper has hit the last available page of comics.
     * @property pageError Optional localized error message if loading a next page fails.
     * @property currentPage The current loaded page index.
     */
    data class Success(
        val comics: List<ComicBook>,
        val isFetchingMore: Boolean = false,
        val isLastPage: Boolean = false,
        val pageError: String? = null,
        val currentPage: Int = 1,
        val searchQuery: String? = null
    ) : ComicUiState

    /**
     * Represents a failure state (e.g. offline, timeout, or parsing structural failure).
     * @property message A user-friendly error message description.
     */
    data class Error(val message: String) : ComicUiState
}
