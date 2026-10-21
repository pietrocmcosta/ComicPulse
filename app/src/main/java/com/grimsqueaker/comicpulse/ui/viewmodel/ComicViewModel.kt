package com.grimsqueaker.comicpulse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grimsqueaker.comicpulse.data.repository.ComicRepository
import com.grimsqueaker.comicpulse.ui.state.ComicUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Senior ViewModel implementing reactive UI state management.
 * Coordinates comic fetching with native web pagination support, handles exceptions,
 * and exposes a read-only StateFlow.
 *
 * @param repository The repository contract interface to retrieve the comic data.
 */
class ComicViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    // Internal mutable StateFlow representing the backing field for UI state
    private val _uiState = MutableStateFlow<ComicUiState>(ComicUiState.Loading)
    
    // Expose as read-only StateFlow to Compose UI layer to avoid unexpected mutations
    val uiState: StateFlow<ComicUiState> = _uiState.asStateFlow()

    init {
        fetchComics()
    }

    /**
     * Executes the comic fetching coroutine asynchronously from page 1.
     * Resets pagination counters and updates state.
     * @param searchQuery Optional filter text to search the getcomics.org catalog.
     */
    fun fetchComics(searchQuery: String? = null) {
        _uiState.value = ComicUiState.Loading
        
        viewModelScope.launch {
            repository.getLatestComics(page = 1, searchQuery = searchQuery)
                .onSuccess { comics ->
                    if (comics.isEmpty()) {
                        _uiState.value = ComicUiState.Error(
                            if (searchQuery.isNullOrBlank()) "No comics found. The website structure may have changed. Please update the parser selectors." else "No results found for \"$searchQuery\"."
                        )
                    } else {
                        _uiState.value = ComicUiState.Success(
                            comics = comics,
                            currentPage = 1,
                            isLastPage = false,
                            isFetchingMore = false,
                            searchQuery = searchQuery
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = ComicUiState.Error(
                        exception.localizedMessage ?: "Failed to connect to getcomics.org. Check your internet connection."
                    )
                }
        }
    }

    /**
     * Loads the next successive page of comics.
     * Prevents race conditions and appends new results to the loaded list.
     */
    fun fetchNextPage() {
        val currentState = _uiState.value
        if (currentState !is ComicUiState.Success) return
        if (currentState.isFetchingMore || currentState.isLastPage) return

        // Set state to fetching more
        _uiState.value = currentState.copy(isFetchingMore = true, pageError = null)

        val nextPage = currentState.currentPage + 1
        val activeQuery = currentState.searchQuery

        viewModelScope.launch {
            repository.getLatestComics(page = nextPage, searchQuery = activeQuery)
                .onSuccess { newComics ->
                    val successState = _uiState.value as? ComicUiState.Success ?: return@onSuccess
                    if (newComics.isEmpty()) {
                        // Empty indicates we have successfully reached the final page on the website
                        _uiState.value = successState.copy(
                            isFetchingMore = false,
                            isLastPage = true
                        )
                    } else {
                        // Append and update StateFlow
                        val combinedList = successState.comics + newComics
                        _uiState.value = ComicUiState.Success(
                            comics = combinedList,
                            currentPage = nextPage,
                            isLastPage = false,
                            isFetchingMore = false,
                            searchQuery = activeQuery
                        )
                    }
                }
                .onFailure { exception ->
                    val successState = _uiState.value as? ComicUiState.Success ?: return@onFailure
                    _uiState.value = successState.copy(
                        isFetchingMore = false,
                        pageError = exception.localizedMessage ?: "Failed to load page $nextPage"
                    )
                }
        }
    }
}
