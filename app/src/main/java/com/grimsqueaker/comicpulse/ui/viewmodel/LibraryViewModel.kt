package com.grimsqueaker.comicpulse.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grimsqueaker.comicpulse.data.local.LibraryDatabaseHelper
import com.grimsqueaker.comicpulse.data.local.LibraryScanner
import com.grimsqueaker.comicpulse.data.local.LocalComic
import com.grimsqueaker.comicpulse.data.local.StoragePreferences
import com.grimsqueaker.comicpulse.data.model.ComicBook
import com.grimsqueaker.comicpulse.data.repository.ComicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages the state for the local offline comic library.
 * Now injects ComicRepository for offline metadata-matching searches.
 */
class LibraryViewModel(
    application: Application,
    private val repository: ComicRepository
) : AndroidViewModel(application) {

    private val storagePreferences = StoragePreferences(application)
    private val libraryScanner = LibraryScanner(application)

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _viewTypeState = MutableStateFlow("GRID")
    val viewTypeState: StateFlow<String> = _viewTypeState.asStateFlow()

    init {
        loadLibrary()
        
        // Load view type from preferences
        viewModelScope.launch {
            _viewTypeState.value = storagePreferences.viewTypeFlow.first()
        }
        
        // Auto-refresh when a background download completes and copies
        viewModelScope.launch {
            com.grimsqueaker.comicpulse.data.local.DownloadTracker.downloadCompletedEvent.collect {
                loadLibrary()
            }
        }
    }

    /**
     * Attempts to read the saved folder URI and index all comic files within it.
     */
    fun loadLibrary() {
        _uiState.value = LibraryUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val savedUriString = storagePreferences.libraryUriFlow.first()
            if (savedUriString.isNullOrEmpty()) {
                _uiState.value = LibraryUiState.NoFolderSelected
                return@launch
            }

            try {
                val comics = libraryScanner.scanLibraryFolder(savedUriString)
                _uiState.value = LibraryUiState.Success(savedUriString, comics)
                
                // Asynchronously extract and cache the covers in the background
                launch(Dispatchers.IO) {
                    val context = getApplication<Application>().applicationContext
                    val dbHelper = LibraryDatabaseHelper(context)
                    
                    val updatedComics = comics.map { comic ->
                        val coverFile = com.grimsqueaker.comicpulse.util.ComicExtractor.getOrCreateCover(context, comic.uri, comic.isCbr)
                        val localPath = coverFile?.absolutePath
                        
                        // Proactively register manual imports in SQLite so we never have to parse again
                        if (localPath != null && comic.coverPath == null) {
                            val fName = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, comic.uri)?.name ?: comic.name
                            dbHelper.insertComicMetadata(
                                fileName = fName,
                                title = comic.name.substringBeforeLast("."),
                                coverUrl = comic.coverImageUrl ?: "",
                                description = comic.description ?: "Locally imported comic book file.",
                                localCover = localPath,
                                detailUrl = comic.detailUrl ?: ""
                            )
                        }
                        
                        comic.copy(coverPath = localPath)
                    }
                    
                    val currentState = _uiState.value
                    if (currentState is LibraryUiState.Success && currentState.folderUriString == savedUriString) {
                        _uiState.value = LibraryUiState.Success(savedUriString, updatedComics)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to read folder. Please select it again. Error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Saves a newly selected folder URI and triggers a rescan.
     */
    fun setLibraryFolder(uri: Uri) {
        viewModelScope.launch {
            storagePreferences.saveLibraryUri(uri.toString())
            loadLibrary()
        }
    }
    
    /**
     * Clears the active folder configuration.
     */
    fun clearLibraryFolder() {
        viewModelScope.launch {
            storagePreferences.clearLibraryUri()
            loadLibrary()
        }
    }

    /**
     * Saves the active view type layout style.
     */
    fun saveViewType(viewType: String) {
        viewModelScope.launch {
            storagePreferences.saveViewType(viewType)
            _viewTypeState.value = viewType
        }
    }

    /**
     * Deletes a set of selected local comics from the device storage.
     */
    fun deleteComics(comics: Set<LocalComic>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            comics.forEach { comic ->
                try {
                    val file = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, comic.uri)
                    if (file != null && file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadLibrary()
        }
    }

    /**
     * Searches getcomics.org catalog for matching search queries.
     */
    fun searchComicsForMatch(
        query: String,
        onResult: (List<ComicBook>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.getLatestComics(page = 1, searchQuery = query)
                .onSuccess { comics ->
                    onResult(comics)
                }
                .onFailure { exception ->
                    onError(exception.localizedMessage ?: "Failed to connect to server.")
                }
        }
    }

    /**
     * Links a local comic to a matched online comic book page.
     * Fetches details, downloads/caches its cover, and registers it in SQLite.
     */
    fun matchComicMetadata(comic: LocalComic, matchedBook: ComicBook) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val dbHelper = LibraryDatabaseHelper(context)
            
            // 1. Fetch full details from the site
            repository.getComicDetail(matchedBook.detailUrl)
                .onSuccess { detail ->
                    // 2. Download the official high-quality cover image to cache
                    val uniqueName = comic.uri.hashCode().toString()
                    val destFile = java.io.File(context.cacheDir, "covers/$uniqueName.jpg")
                    destFile.parentFile?.mkdirs()
                    
                    try {
                        val url = java.net.URL(detail.coverImageUrl)
                        url.openStream().use { input ->
                            java.io.FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    val finalCoverPath = if (destFile.exists() && destFile.length() > 0) {
                        destFile.absolutePath
                    } else {
                        comic.coverPath ?: ""
                    }
                    
                    // 3. Register matched metadata inside SQLite
                    val fName = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, comic.uri)?.name ?: comic.name
                    dbHelper.insertComicMetadata(
                        fileName = fName,
                        title = detail.title,
                        coverUrl = detail.coverImageUrl,
                        description = detail.description,
                        localCover = finalCoverPath,
                        detailUrl = matchedBook.detailUrl
                    )
                    
                    // 4. Reload Library Grid
                    loadLibrary()
                }
                .onFailure {
                    // Fallback to basic match without full description if request failed
                    val uniqueName = comic.uri.hashCode().toString()
                    val destFile = java.io.File(context.cacheDir, "covers/$uniqueName.jpg")
                    destFile.parentFile?.mkdirs()
                    try {
                        val url = java.net.URL(matchedBook.coverImageUrl)
                        url.openStream().use { input ->
                            java.io.FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    val finalCoverPath = if (destFile.exists() && destFile.length() > 0) {
                        destFile.absolutePath
                    } else {
                        comic.coverPath ?: ""
                    }
                    
                    val fName = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, comic.uri)?.name ?: comic.name
                    dbHelper.insertComicMetadata(
                        fileName = fName,
                        title = matchedBook.title,
                        coverUrl = matchedBook.coverImageUrl,
                        description = "Locally matched online metadata.",
                        localCover = finalCoverPath,
                        detailUrl = matchedBook.detailUrl
                    )
                    loadLibrary()
                }
        }
    }

    /**
     * Extracts and retrieves the first 15 pages of a local comic for custom cover previews.
     */
    suspend fun getComicPreviewPages(comic: LocalComic): List<java.io.File> {
        val context = getApplication<Application>().applicationContext
        return com.grimsqueaker.comicpulse.util.ComicExtractor.getPreviewPages(context, comic.uri, comic.isCbr)
    }

    /**
     * Links a local comic to a matched online comic book page, using a chosen custom cover.
     * The cover can be the official online one, or a selected local comic page.
     */
    fun matchComicMetadataWithCustomCover(
        comic: LocalComic,
        matchedBook: ComicBook,
        isOnlineCover: Boolean,
        coverSource: String // Online URL or Local file path
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val dbHelper = LibraryDatabaseHelper(context)
            
            // 1. Fetch full details from the site
            repository.getComicDetail(matchedBook.detailUrl)
                .onSuccess { detail ->
                    // 2. Prepare the selected cover path
                    val uniqueName = comic.uri.hashCode().toString()
                    val destFile = java.io.File(context.cacheDir, "covers/$uniqueName.jpg")
                    destFile.parentFile?.mkdirs()
                    
                    if (isOnlineCover) {
                        // Download the official cover URL
                        try {
                            val url = java.net.URL(coverSource)
                            url.openStream().use { input ->
                                java.io.FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        // Copy the local preview page file to the official cache path
                        try {
                            val srcFile = java.io.File(coverSource)
                            if (srcFile.exists()) {
                                srcFile.inputStream().use { input ->
                                    java.io.FileOutputStream(destFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    val finalCoverPath = if (destFile.exists() && destFile.length() > 0) {
                        destFile.absolutePath
                    } else {
                        comic.coverPath ?: ""
                    }
                    
                    // 3. Register inside SQLite database
                    val fName = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, comic.uri)?.name ?: comic.name
                    dbHelper.insertComicMetadata(
                        fileName = fName,
                        title = detail.title,
                        coverUrl = detail.coverImageUrl,
                        description = detail.description,
                        localCover = finalCoverPath,
                        detailUrl = matchedBook.detailUrl
                    )
                    
                    loadLibrary()
                }
                .onFailure {
                    // Fallback using basic matchedBook fields if detail page request failed
                    val uniqueName = comic.uri.hashCode().toString()
                    val destFile = java.io.File(context.cacheDir, "covers/$uniqueName.jpg")
                    destFile.parentFile?.mkdirs()
                    
                    if (isOnlineCover) {
                        try {
                            val url = java.net.URL(coverSource)
                            url.openStream().use { input ->
                                java.io.FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            val srcFile = java.io.File(coverSource)
                            if (srcFile.exists()) {
                                srcFile.inputStream().use { input ->
                                    java.io.FileOutputStream(destFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    val finalCoverPath = if (destFile.exists() && destFile.length() > 0) {
                        destFile.absolutePath
                    } else {
                        comic.coverPath ?: ""
                    }
                    
                    val fName = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, comic.uri)?.name ?: comic.name
                    dbHelper.insertComicMetadata(
                        fileName = fName,
                        title = matchedBook.title,
                        coverUrl = matchedBook.coverImageUrl,
                        description = "Locally matched online metadata.",
                        localCover = finalCoverPath,
                        detailUrl = matchedBook.detailUrl
                    )
                    loadLibrary()
                }
        }
    }
}

sealed interface LibraryUiState {
    object Loading : LibraryUiState
    object NoFolderSelected : LibraryUiState
    data class Success(val folderUriString: String, val comics: List<LocalComic>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}
