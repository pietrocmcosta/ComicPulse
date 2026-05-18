package com.example.comicaggregator.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.example.comicaggregator.data.local.DownloadTracker
import com.example.comicaggregator.data.local.LibraryDatabaseHelper
import com.example.comicaggregator.data.local.StoragePreferences
import com.example.comicaggregator.util.ComicExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Listens for system broadcasts when a DownloadManager task completes.
 * Safely copies the completed cache download to the SAF user-selected folder in the background.
 * Assesses the true native file format by inspecting the completed file's MAGIC BYTES (file signature),
 * completely bypassing URL guessing, redirection mismatches, and incorrect server MIME headers.
 */
class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return

        val appContext = context.applicationContext
        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            var newFileUri: Uri? = null
            try {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                        val title = if (titleIndex >= 0) cursor.getString(titleIndex) else "Comic"
                        
                        cursor.close()

                        val contentUri = downloadManager.getUriForDownloadedFile(downloadId)
                        if (contentUri != null) {
                            val format = detectFileTypeFromSignature(appContext, contentUri)
                            
                            val isCbr = format == "rar"
                            val isZip = format == "zip"

                            val storagePreferences = StoragePreferences(appContext)
                            val libraryUriString = storagePreferences.libraryUriFlow.first()
                            
                            if (!libraryUriString.isNullOrEmpty()) {
                                val libraryFolder = DocumentFile.fromTreeUri(appContext, Uri.parse(libraryUriString))
                                if (libraryFolder != null && libraryFolder.canWrite()) {
                                    
                                    val containsArchives = if (isZip) checkZipContainsArchives(appContext, contentUri) else false
                                    
                                    if (isZip && containsArchives) {
                                        // Unpack all inner comic books directly to Library folder
                                        unpackZipToLibrary(appContext, contentUri, libraryFolder)
                                        
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(appContext, "Unpacked comic bundle successfully!", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        val extension = when {
                                            isCbr -> ".cbr"
                                            isZip -> ".cbz"
                                            else -> {
                                                val realFileName = getRealFileName(appContext, contentUri)
                                                val lower = realFileName.lowercase()
                                                when {
                                                    lower.endsWith(".cbr") || lower.endsWith(".rar") -> ".cbr"
                                                    else -> ".cbz"
                                                }
                                            }
                                        }
                                        
                                        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9.\\s-]"), "")
                                            .replace(Regex("[\\s-]+"), "_")
                                            .trim('_')
                                        val truncatedTitle = if (cleanTitle.length > 80) cleanTitle.take(80) else cleanTitle
                                        val fileName = "$truncatedTitle$extension"
                                        
                                        val newFile = libraryFolder.createFile("application/octet-stream", fileName)
                                        if (newFile != null) {
                                            newFileUri = newFile.uri
                                            appContext.contentResolver.openInputStream(contentUri)?.use { input ->
                                                appContext.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                                    input.copyTo(output)
                                                }
                                            }

                                            // Persistent SQLite Library Registration
                                            val dbHelper = LibraryDatabaseHelper(appContext)
                                            val dlMeta = dbHelper.getDownloadMetadata(downloadId)
                                            
                                            // Extract a premium local thumbnail cover path
                                            val coverFile = ComicExtractor.getOrCreateCover(appContext, newFile.uri, isCbr)
                                            val localCoverPath = coverFile?.absolutePath ?: ""
                                            
                                            if (dlMeta != null) {
                                                dbHelper.insertComicMetadata(
                                                    fileName = fileName,
                                                    title = dlMeta.title,
                                                    coverUrl = dlMeta.coverImageUrl,
                                                    description = dlMeta.description,
                                                    localCover = localCoverPath,
                                                    detailUrl = dlMeta.detailUrl
                                                )
                                                dbHelper.deleteDownloadMetadata(downloadId)
                                            } else {
                                                // Fallback registration for externally triggered or untracked downloads
                                                dbHelper.insertComicMetadata(
                                                    fileName = fileName,
                                                    title = fileName.substringBeforeLast("."),
                                                    coverUrl = "",
                                                    description = "Locally imported comic book file.",
                                                    localCover = localCoverPath,
                                                    detailUrl = ""
                                                )
                                            }
                                            
                                            launch(Dispatchers.Main) {
                                                Toast.makeText(appContext, "Comic added to library!", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            launch(Dispatchers.Main) {
                                                Toast.makeText(appContext, "Failed to create target file in Library.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    
                                    downloadManager.remove(downloadId)
                                    DownloadTracker.downloadCompletedEvent.tryEmit(Unit)
                                }
                            }
                        }
                    } else {
                        cursor.close()
                    }
                } else {
                    cursor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    newFileUri?.let { uri ->
                        DocumentFile.fromSingleUri(appContext, uri)?.delete()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(appContext, "Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun detectFileTypeFromSignature(context: Context, uri: Uri): String {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val header = ByteArray(4)
                val bytesRead = inputStream.read(header)
                if (bytesRead >= 4) {
                    if (header[0] == 0x50.toByte() &&
                        header[1] == 0x4B.toByte() &&
                        header[2] == 0x03.toByte() &&
                        header[3] == 0x04.toByte()
                    ) {
                        return "zip"
                    }
                    if (header[0] == 0x52.toByte() &&
                        header[1] == 0x61.toByte() &&
                        header[2] == 0x72.toByte() &&
                        header[3] == 0x21.toByte()
                    ) {
                        return "rar"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "unknown"
    }

    private fun getRealFileName(context: Context, uri: Uri?): String {
        if (uri == null) return ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return cursor.getString(nameIndex) ?: ""
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun checkZipContainsArchives(context: Context, uri: Uri): Boolean {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && !entry.name.endsWith("/")) {
                            val name = entry.name.lowercase()
                            if (name.endsWith(".cbz") || name.endsWith(".cbr") || name.endsWith(".zip") || name.endsWith(".rar")) {
                                return true
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun unpackZipToLibrary(context: Context, zipUri: Uri, libraryFolder: DocumentFile) {
        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && !entry.name.endsWith("/")) {
                        val fileName = File(entry.name).name
                        if (fileName.isNotBlank()) {
                            val lower = fileName.lowercase()
                            if (lower.endsWith(".cbz") || lower.endsWith(".cbr") || lower.endsWith(".zip") || lower.endsWith(".rar")) {
                                val newFile = libraryFolder.createFile("application/octet-stream", fileName)
                                if (newFile != null) {
                                    context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                        zis.copyTo(output)
                                    }
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }
}
