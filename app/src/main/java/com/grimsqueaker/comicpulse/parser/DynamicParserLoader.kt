package com.grimsqueaker.comicpulse.parser

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader

/**
 * Loads external parser plugins at runtime from downloaded DEX/JAR files.
 *
 * How it works:
 *  1. A JAR containing a class that implements [ParserContract] is placed in
 *     `context.filesDir/parsers/`.
 *  2. [loadParser] uses Android's [DexClassLoader] to open the JAR,
 *     instantiate the entry class, and cast it to [ParserContract].
 *  3. The caller then registers the resulting instance in [ParserRegistry].
 *
 * Security note: In production you should validate the JAR against a known
 * checksum or signature before loading. This is left as a future enhancement.
 */
object DynamicParserLoader {

    private const val TAG = "DynamicParserLoader"

    /**
     * Attempts to load a [ParserContract] from a JAR/DEX file on disk.
     *
     * @param context Application context (used for optimized DEX output directory).
     * @param jarPath Absolute path to the JAR/DEX file.
     * @param entryClassName Fully qualified name of the class that implements [ParserContract].
     * @return The instantiated [ParserContract], or null on failure.
     */
    fun loadParser(context: Context, jarPath: String, entryClassName: String): ParserContract? {
        return try {
            val optimizedDir = context.getDir("dex_opt", Context.MODE_PRIVATE)
            val classLoader = DexClassLoader(
                jarPath,
                optimizedDir.absolutePath,
                null,
                context.classLoader
            )

            val clazz = classLoader.loadClass(entryClassName)
            val instance = clazz.getDeclaredConstructor().newInstance()

            if (instance is ParserContract) {
                Log.i(TAG, "Successfully loaded parser: ${instance.displayName} v${instance.version}")
                instance
            } else {
                Log.e(TAG, "Class $entryClassName does not implement ParserContract")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load parser from $jarPath", e)
            null
        }
    }

    /**
     * Scans the parsers directory and loads all valid plugins.
     * Each JAR must be accompanied by a `.meta` JSON file with the same base name
     * containing at minimum: `{ "entryClass": "com.grimsqueaker.comicpulse.MyParser" }`.
     *
     * @param context Application context.
     * @param registry The [ParserRegistry] to register loaded parsers into.
     */
    fun loadAllDynamicParsers(context: Context, registry: ParserRegistry) {
        val parsersDir = java.io.File(context.filesDir, "parsers")
        if (!parsersDir.exists()) {
            parsersDir.mkdirs()
            return
        }

        val jarFiles = parsersDir.listFiles { file -> file.extension in listOf("jar", "dex", "apk") }
            ?: return

        for (jarFile in jarFiles) {
            val metaFile = java.io.File(parsersDir, "${jarFile.nameWithoutExtension}.meta")
            if (!metaFile.exists()) {
                Log.w(TAG, "Skipping ${jarFile.name}: no accompanying .meta file found")
                continue
            }

            try {
                val metaJson = org.json.JSONObject(metaFile.readText())
                val entryClass = metaJson.getString("entryClass")
                val parser = loadParser(context, jarFile.absolutePath, entryClass)

                if (parser != null) {
                    val metadata = ParserMetadata(
                        id = parser.id,
                        displayName = parser.displayName,
                        description = parser.description,
                        version = parser.version,
                        baseUrl = parser.baseUrl,
                        isBuiltIn = false,
                        enabled = true,
                        jarPath = jarFile.absolutePath,
                        entryClass = entryClass
                    )
                    registry.register(parser, metadata)
                    Log.i(TAG, "Registered dynamic parser: ${parser.displayName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading dynamic parser from ${jarFile.name}", e)
            }
        }
    }
}
