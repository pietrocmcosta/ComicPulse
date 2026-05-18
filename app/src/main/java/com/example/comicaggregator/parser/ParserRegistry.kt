package com.example.comicaggregator.parser

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

// Dedicated DataStore instance for parser settings (separate from app settings)
private val Context.parserDataStore: DataStore<Preferences> by preferencesDataStore(name = "parser_settings")

/**
 * Central registry that manages all installed comic parsers (both built-in and dynamic).
 *
 * Design goals:
 *  - Single source of truth for which parsers exist and their enabled state.
 *  - Persists enabled/disabled flags in Jetpack DataStore.
 *  - Exposes [getActiveParsers] for the search aggregation layer.
 *  - Thread-safe: internal map is synchronized.
 */
class ParserRegistry(private val context: Context) {

    private val ENABLED_MAP_KEY = stringPreferencesKey("parser_enabled_map")

    // In-memory registry: parserId -> (contract, metadata)
    private val registry = mutableMapOf<String, Pair<ParserContract, ParserMetadata>>()

    /**
     * Register a parser into the registry.
     * If a parser with the same id already exists, it is silently replaced.
     *
     * @param parser The live [ParserContract] instance.
     * @param metadata The associated [ParserMetadata].
     */
    @Synchronized
    fun register(parser: ParserContract, metadata: ParserMetadata) {
        registry[parser.id] = Pair(parser, metadata)
    }

    /**
     * Unregister a parser by its id.
     */
    @Synchronized
    fun unregister(id: String) {
        registry.remove(id)
    }

    /**
     * Returns all registered parsers that are currently enabled.
     */
    @Synchronized
    fun getActiveParsers(): List<ParserContract> {
        return registry.values
            .filter { it.second.enabled }
            .map { it.first }
    }

    /**
     * Returns all registered parser metadata entries (for the Settings UI).
     */
    @Synchronized
    fun getAllMetadata(): List<ParserMetadata> {
        return registry.values.map { it.second }
    }

    /**
     * Retrieves a single parser by id.
     */
    @Synchronized
    fun getParser(id: String): ParserContract? {
        return registry[id]?.first
    }

    /**
     * Retrieves metadata for a single parser by id.
     */
    @Synchronized
    fun getMetadata(id: String): ParserMetadata? {
        return registry[id]?.second
    }

    /**
     * Updates the enabled flag for a given parser and persists the change.
     */
    suspend fun setEnabled(id: String, enabled: Boolean) {
        synchronized(this) {
            val entry = registry[id] ?: return@synchronized
            registry[id] = Pair(entry.first, entry.second.copy(enabled = enabled))
        }
        persistEnabledMap()
    }

    /**
     * Loads persisted enabled/disabled map from DataStore and applies it to current registry.
     * Call this once after all parsers have been registered during app startup.
     */
    suspend fun loadPersistedState() {
        val json = context.parserDataStore.data.map { prefs ->
            prefs[ENABLED_MAP_KEY]
        }.first()

        if (json != null) {
            try {
                val obj = JSONObject(json)
                synchronized(this) {
                    for (key in obj.keys()) {
                        val entry = registry[key] ?: continue
                        val enabled = obj.optBoolean(key, true)
                        registry[key] = Pair(entry.first, entry.second.copy(enabled = enabled))
                    }
                }
            } catch (_: Exception) {
                // Corrupted prefs – keep defaults
            }
        }
    }

    /**
     * Persists the current enabled map to DataStore.
     */
    private suspend fun persistEnabledMap() {
        val obj = JSONObject()
        synchronized(this) {
            for ((id, pair) in registry) {
                obj.put(id, pair.second.enabled)
            }
        }
        context.parserDataStore.edit { prefs ->
            prefs[ENABLED_MAP_KEY] = obj.toString()
        }
    }
}
