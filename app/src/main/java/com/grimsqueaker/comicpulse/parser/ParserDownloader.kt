package com.grimsqueaker.comicpulse.parser

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Downloads parser plugin files (JAR + .meta) from a remote URL
 * and stores them in `context.filesDir/parsers/`.
 *
 * Expected flow:
 *  1. Call [downloadParser] with a base URL pointing to a directory that contains
 *     both `<name>.jar` and `<name>.meta`.
 *  2. Files are saved locally.
 *  3. Caller uses [DynamicParserLoader] to load and register the parser.
 */
object ParserDownloader {

    private const val TAG = "ParserDownloader"

    /**
     * Downloads a parser JAR and its companion .meta file from a remote URL.
     *
     * @param context Application context.
     * @param jarUrl Full URL to the JAR/DEX file (e.g. "https://raw.githubusercontent.com/.../MyParser.jar").
     * @param metaUrl Full URL to the .meta JSON file.
     * @param fileName Base file name (without extension) to save locally.
     * @return True if both files downloaded successfully, false otherwise.
     */
    suspend fun downloadParser(
        context: Context,
        jarUrl: String,
        metaUrl: String,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val parsersDir = File(context.filesDir, "parsers")
        if (!parsersDir.exists()) parsersDir.mkdirs()

        val jarFile = File(parsersDir, "$fileName.jar")
        val metaFile = File(parsersDir, "$fileName.meta")

        try {
            // Download JAR
            URL(jarUrl).openStream().use { input ->
                jarFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Downloaded JAR: ${jarFile.absolutePath} (${jarFile.length()} bytes)")

            // Download meta
            URL(metaUrl).openStream().use { input ->
                metaFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Downloaded META: ${metaFile.absolutePath}")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download parser: $fileName", e)
            // Clean up partial downloads
            if (jarFile.exists()) jarFile.delete()
            if (metaFile.exists()) metaFile.delete()
            false
        }
    }

    /**
     * Removes a downloaded parser from local storage.
     *
     * @param context Application context.
     * @param fileName Base file name (without extension) to remove.
     * @return True if files were deleted, false otherwise.
     */
    fun removeParser(context: Context, fileName: String): Boolean {
        val parsersDir = File(context.filesDir, "parsers")
        val jarFile = File(parsersDir, "$fileName.jar")
        val metaFile = File(parsersDir, "$fileName.meta")

        var deleted = false
        if (jarFile.exists()) { jarFile.delete(); deleted = true }
        if (metaFile.exists()) { metaFile.delete(); deleted = true }
        return deleted
    }
}
