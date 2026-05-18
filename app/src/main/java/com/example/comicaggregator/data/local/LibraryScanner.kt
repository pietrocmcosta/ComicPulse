package com.example.comicaggregator.data.local

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Data class representing a locally discovered comic file with full optional metadata associations.
 */
data class LocalComic(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isCbr: Boolean,
    val coverPath: String? = null,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val detailUrl: String? = null
)

/**
 * Scans the Storage Access Framework (SAF) tree for comic archive files and matches them to SQLite metadata.
 */
class LibraryScanner(private val context: Context) {

    /**
     * Iterates through the given DocumentTree URI and filters for `.cbz` and `.cbr` files.
     * Integrates transparent lookup to the local metadata DB.
     */
    fun scanLibraryFolder(folderUriString: String): List<LocalComic> {
        val rootUri = Uri.parse(folderUriString)
        val documentFile = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()

        if (!documentFile.canRead() || !documentFile.isDirectory) {
            return emptyList()
        }

        val comics = mutableListOf<LocalComic>()
        val dbHelper = LibraryDatabaseHelper(context)
        
        for (file in documentFile.listFiles()) {
            if (file.isDirectory) continue
            val fileName = file.name ?: continue
            val lowerName = fileName.lowercase()
            
            if (lowerName.endsWith(".cbz") || lowerName.endsWith(".cbr")) {
                val dbMeta = dbHelper.getComicMetadata(fileName)
                comics.add(
                    LocalComic(
                        uri = file.uri,
                        name = dbMeta?.title ?: fileName,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        isCbr = lowerName.endsWith(".cbr"),
                        coverPath = dbMeta?.localCoverPath,
                        description = dbMeta?.description,
                        coverImageUrl = dbMeta?.coverImageUrl,
                        detailUrl = dbMeta?.detailUrl
                    )
                )
            }
        }
        
        // Sort newest first by default
        return comics.sortedByDescending { it.lastModified }
    }
}
