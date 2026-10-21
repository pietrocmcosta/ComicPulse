package com.grimsqueaker.comicpulse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grimsqueaker.comicpulse.data.local.DownloadTask
import com.grimsqueaker.comicpulse.data.local.DownloadTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    val downloadsFlow: StateFlow<List<DownloadTask>> = DownloadTracker.pollDownloads(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val aggregateProgressFlow: StateFlow<Float> = downloadsFlow.map { tasks ->
        val activeTasks = tasks.filter {
            it.status == android.app.DownloadManager.STATUS_RUNNING ||
            it.status == android.app.DownloadManager.STATUS_PENDING ||
            it.status == android.app.DownloadManager.STATUS_PAUSED
        }
        if (activeTasks.isEmpty()) return@map 0f
        val totalBytes = activeTasks.sumOf { it.totalBytes.coerceAtLeast(1L) }
        val downloadedBytes = activeTasks.sumOf { it.bytesDownloaded }
        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )
}
