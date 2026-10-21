package com.grimsqueaker.comicpulse.ui.screen

import android.app.DownloadManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grimsqueaker.comicpulse.R
import com.grimsqueaker.comicpulse.data.local.DownloadTask
import com.grimsqueaker.comicpulse.ui.viewmodel.DownloadsViewModel

private val DarkBackground = Color(0xFF0C0C10)
private val CardBackground = Color(0xFF161622)
private val NeonCyan = Color(0xFF00F2FE)
private val TextWhite = Color(0xFFF0F0F5)
private val TextMuted = Color(0xFF9E9EB2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.downloadsFlow.collectAsState()
    val aggregateProgress by viewModel.aggregateProgressFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.downloads_title_prefix),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = NeonCyan
                        )
                        Text(
                            text = stringResource(R.string.downloads_title_suffix),
                            fontWeight = FontWeight.Light,
                            fontSize = 24.sp,
                            color = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextWhite
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Aggregate Progress Header
            val hasActiveDownloads = downloads.any {
                it.status == DownloadManager.STATUS_RUNNING ||
                it.status == DownloadManager.STATUS_PENDING ||
                it.status == DownloadManager.STATUS_PAUSED
            }
            if (hasActiveDownloads && aggregateProgress < 1f) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = aggregateProgress,
                            color = NeonCyan,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.downloads_total_progress), color = TextWhite, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.downloads_percent_complete, (aggregateProgress * 100).toInt()), color = TextMuted)
                        }
                    }
                }
            }

            if (downloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.downloads_empty), color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(downloads, key = { it.id }) { task ->
                        DownloadItem(task = task)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItem(task: DownloadTask) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (task.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> Icons.Default.Done
                    DownloadManager.STATUS_FAILED -> Icons.Default.Warning
                    else -> Icons.Default.KeyboardArrowDown
                }
                val iconColor = when (task.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> Color(0xFF10B981) // Green
                    DownloadManager.STATUS_FAILED -> Color(0xFFEF4444) // Red
                    else -> NeonCyan
                }

                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = task.title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                if (task.status == DownloadManager.STATUS_RUNNING || task.status == DownloadManager.STATUS_PENDING) {
                    Text("${(task.progress * 100).toInt()}%", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
            
            if (task.status == DownloadManager.STATUS_RUNNING || task.status == DownloadManager.STATUS_PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = task.progress,
                    color = NeonCyan,
                    trackColor = Color(0xFF2D2D3A),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            } else if (task.status == DownloadManager.STATUS_FAILED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.downloads_status_failed), color = Color(0xFFEF4444), fontSize = 12.sp)
            } else if (task.status == DownloadManager.STATUS_SUCCESSFUL) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.downloads_status_completed), color = Color(0xFF10B981), fontSize = 12.sp)
            }
        }
    }
}
