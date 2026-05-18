package com.example.comicaggregator

import android.app.Application
import com.example.comicaggregator.parser.DynamicParserLoader
import com.example.comicaggregator.parser.ParserMetadata
import com.example.comicaggregator.parser.ParserRegistry
import com.example.comicaggregator.parser.builtin.GetComicsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application class that bootstraps the plug-and-play parser system.
 *
 * Lifecycle:
 *  1. Register all built-in parsers.
 *  2. Scan the `parsers/` directory for dynamically downloaded plugins.
 *  3. Restore persisted enabled/disabled state from DataStore.
 */
class ComicPulseApp : Application() {

    /** Global parser registry – accessible from any component via `(application as ComicPulseApp).parserRegistry`. */
    lateinit var parserRegistry: ParserRegistry
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialise the registry
        parserRegistry = ParserRegistry(this)

        // Register built-in parsers
        val getComics = GetComicsParser()
        parserRegistry.register(
            parser = getComics,
            metadata = ParserMetadata(
                id = getComics.id,
                displayName = getComics.displayName,
                description = getComics.description,
                version = getComics.version,
                baseUrl = getComics.baseUrl,
                isBuiltIn = true,
                enabled = true
            )
        )

        // Load dynamic (downloaded) parsers from disk
        DynamicParserLoader.loadAllDynamicParsers(this, parserRegistry)

        // Restore user's enabled/disabled preferences
        appScope.launch {
            parserRegistry.loadPersistedState()
        }
    }
}
