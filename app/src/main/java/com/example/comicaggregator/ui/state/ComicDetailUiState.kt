package com.example.comicaggregator.ui.state

import com.example.comicaggregator.data.model.ComicBookDetail

/**
 * Sealed interface representing the reactive UI states for the Comic Detail screen.
 */
sealed interface ComicDetailUiState {

    /**
     * Detail screen is currently downloading and parsing the web page.
     */
    object Loading : ComicDetailUiState

    /**
     * Detail screen page content successfully parsed and ready.
     * @property detail The scraped [ComicBookDetail] object.
     */
    data class Success(val detail: ComicBookDetail) : ComicDetailUiState

    /**
     * Scraper network or layout compilation failed.
     * @property message Friendly description of the error.
     */
    data class Error(val message: String) : ComicDetailUiState
}
