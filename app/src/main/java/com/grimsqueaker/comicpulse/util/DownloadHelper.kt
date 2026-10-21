package com.grimsqueaker.comicpulse.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.grimsqueaker.comicpulse.R
import com.grimsqueaker.comicpulse.data.local.LibraryDatabaseHelper

/**
 * Utility object to interact with the Android native DownloadManager.
 */
object DownloadHelper {

    /**
     * Enqueues a direct file download using the system's robust DownloadManager.
     * Maps the download ID to the full site metadata for persistent SQLite registration on completion.
     */
    fun startDirectDownload(
        context: Context, 
        url: String, 
        title: String,
        coverImageUrl: String = "",
        description: String = "",
        detailUrl: String = ""
    ) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)

            val request = DownloadManager.Request(uri).apply {
                setTitle(title)
                setDescription(context.getString(R.string.notification_downloading_comic))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            
            // Map the enqueued task ID to its GetComics details persistently
            val dbHelper = LibraryDatabaseHelper(context)
            dbHelper.insertDownloadMetadata(downloadId, title, coverImageUrl, description, detailUrl)
            
            Toast.makeText(context, context.getString(R.string.toast_download_started, title), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.toast_download_error, e.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
        }
    }
}
