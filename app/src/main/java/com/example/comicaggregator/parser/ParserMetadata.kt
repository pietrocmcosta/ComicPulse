package com.example.comicaggregator.parser

/**
 * Holds descriptive metadata for a registered parser.
 * This is the data class stored in the registry alongside the live [ParserContract] instance.
 *
 * @property id Unique machine key matching [ParserContract.id].
 * @property displayName Human‑readable name for Settings UI.
 * @property description Short one‑liner description.
 * @property version Semantic version string (e.g. "1.0.0").
 * @property baseUrl The target website URL.
 * @property isBuiltIn True for parsers compiled into the APK; false for dynamically loaded ones.
 * @property enabled Whether this parser is currently active in searches.
 * @property jarPath For dynamic parsers, the absolute path to the loaded DEX/JAR file. Null for built‑in.
 * @property entryClass For dynamic parsers, the fully qualified class name to instantiate. Null for built‑in.
 */
data class ParserMetadata(
    val id: String,
    val displayName: String,
    val description: String,
    val version: String,
    val baseUrl: String,
    val isBuiltIn: Boolean = true,
    val enabled: Boolean = true,
    val jarPath: String? = null,
    val entryClass: String? = null
)
