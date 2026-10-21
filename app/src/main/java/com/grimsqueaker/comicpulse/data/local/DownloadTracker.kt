package com.grimsqueaker.comicpulse.data.local

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

/**
 * Tracks native Android DownloadManager tasks.
 */
data class DownloadTask(
    val id: Long,
    val title: String,
    val progress: Float, // 0.0 to 1.0
    val status: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long
)

object DownloadTracker {
    
    // Global event bus for when a download finishes and gets copied to the library
    val downloadCompletedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    
    /**
     * Polls the DownloadManager every second to return a real-time Flow of active and recently completed tasks.
     */
    fun pollDownloads(context: Context): Flow<List<DownloadTask>> = flow {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        while (true) {
            val query = DownloadManager.Query()
            val cursor: Cursor = downloadManager.query(query)
            
            val tasks = mutableListOf<DownloadTask>()
            
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "Unknown"
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytesDownloadedSoFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalSizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    
                    val progress = if (totalSizeBytes > 0) {
                        (bytesDownloadedSoFar.toFloat() / totalSizeBytes.toFloat()).coerceIn(0f, 1f)
                    } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        1f
                    } else {
                        0f
                    }
                    
                    tasks.add(
                        DownloadTask(
                            id = id,
                            title = title,
                            progress = progress,
                            status = status,
                            bytesDownloaded = bytesDownloadedSoFar,
                            totalBytes = totalSizeBytes
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
            
            emit(tasks)
            delay(1000) // Poll every second
        }
    }
}
