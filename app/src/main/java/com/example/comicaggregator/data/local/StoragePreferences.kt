package com.example.comicaggregator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creates a datastore instance scoped to the application context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Encapsulates the persistence layer for application preferences.
 * Uses Jetpack DataStore to safely store the user's selected Library Folder URI.
 */
class StoragePreferences(private val context: Context) {

    private val LIBRARY_URI_KEY = stringPreferencesKey("library_folder_uri")
    private val VIEW_TYPE_KEY = stringPreferencesKey("library_view_type")
    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    /**
     * A Flow that emits the currently saved Library Folder URI string, or null if none is selected.
     */
    val libraryUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LIBRARY_URI_KEY]
    }

    /**
     * A Flow that emits the active layout style (GRID, LIST, or DETAIL).
     */
    val viewTypeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[VIEW_TYPE_KEY] ?: "GRID"
    }

    /**
     * A Flow that emits the selected app language ("system", "en", or "pt").
     */
    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "system"
    }

    /**
     * Saves the selected folder URI to persistent storage.
     */
    suspend fun saveLibraryUri(uriString: String) {
        context.dataStore.edit { preferences ->
            preferences[LIBRARY_URI_KEY] = uriString
        }
    }

    /**
     * Saves the selected layout view type to persistent storage.
     */
    suspend fun saveViewType(viewType: String) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_TYPE_KEY] = viewType
        }
    }

    /**
     * Saves the selected app language to persistent storage.
     */
    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    /**
     * Clears the selected library folder.
     */
    suspend fun clearLibraryUri() {
        context.dataStore.edit { preferences ->
            preferences.remove(LIBRARY_URI_KEY)
        }
    }
}
