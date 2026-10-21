package com.grimsqueaker.comicpulse.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grimsqueaker.comicpulse.util.ComicExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manages the background extraction and state of the comic reader.
 */
class ComicReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun loadComic(uriString: String, isCbr: Boolean) {
        _uiState.value = ReaderUiState.Loading
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val uri = Uri.parse(uriString)
                val imageFiles = ComicExtractor.extractComic(context, uri, isCbr)
                
                if (imageFiles.isEmpty()) {
                    _uiState.value = ReaderUiState.Error("Archive is empty or contains no supported images.")
                } else {
                    _uiState.value = ReaderUiState.Success(imageFiles)
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: e.javaClass.simpleName
                val isEncrypted = errorMsg.contains("password", ignoreCase = true) || 
                                  errorMsg.contains("encrypted", ignoreCase = true) || 
                                  errorMsg.contains("decrypt", ignoreCase = true)
                                  
                val finalMessage = if (isEncrypted) {
                    "Failed to open comic. This archive appears to be password-protected or encrypted, which is not supported."
                } else {
                    "Failed to open comic.\n\nDetails: $errorMsg\n\n(Note: Modern RAR5 formats are not supported by the reader. Please verify if the archive is compressed in RAR5 format or if your cache directory is full.)"
                }
                _uiState.value = ReaderUiState.Error(finalMessage)
            }
        }
    }
}

sealed interface ReaderUiState {
    object Loading : ReaderUiState
    data class Success(val pages: List<File>) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}
