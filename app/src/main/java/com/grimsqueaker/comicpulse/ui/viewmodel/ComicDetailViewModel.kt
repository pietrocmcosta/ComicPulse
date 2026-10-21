package com.grimsqueaker.comicpulse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grimsqueaker.comicpulse.data.repository.ComicRepository
import com.grimsqueaker.comicpulse.ui.state.ComicDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android ViewModel managing the state of the Comic Detail Screen.
 * Exposes a unidirectional StateFlow.
 *
 * @param repository The repository contract interface to fetch the detailed scraper page.
 */
class ComicDetailViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ComicDetailUiState>(ComicDetailUiState.Loading)
    val uiState: StateFlow<ComicDetailUiState> = _uiState.asStateFlow()

    /**
     * Triggers the asynchronous page loading and parsing on background coroutine contexts.
     * Updates the reactive state machine flow accordingly.
     *
     * @param url The getcomics.org direct URL.
     */
    fun loadComicDetail(url: String) {
        _uiState.value = ComicDetailUiState.Loading
        viewModelScope.launch {
            repository.getComicDetail(url)
                .onSuccess { detail ->
                    _uiState.value = ComicDetailUiState.Success(detail)
                }
                .onFailure { exception ->
                    _uiState.value = ComicDetailUiState.Error(
                        exception.localizedMessage ?: "Failed to connect to getcomics.org detail server. Check internet connection."
                    )
                }
        }
    }
}
