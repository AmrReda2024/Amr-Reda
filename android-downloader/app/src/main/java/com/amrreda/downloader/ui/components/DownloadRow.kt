package com.amrreda.downloader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amrreda.downloader.R
import com.amrreda.downloader.data.DownloadEntity
import com.amrreda.downloader.data.DownloadStatus
import com.amrreda.downloader.engine.DownloadEngine
import com.amrreda.downloader.ui.Format

@Composable
fun ActiveDownloadRow(item: DownloadEntity, engine: DownloadEngine) {
    val status = item.statusEnum
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.thumbnailUrl != null) Thumb(item.thumbnailUrl, com.amrreda.downloader.data.FileCategory.IMAGE, 48.dp)
            else Thumb(item.url, item.categoryEnum, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                when {
                    status == DownloadStatus.RUNNING && item.totalBytes > 0 ->
                        LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
                    status == DownloadStatus.RUNNING ->
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    item.totalBytes > 0 ->
                        LinearProgressIndicator(
                            progress = { item.progress }, modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outline,
                        )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    statusLine(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.End) {
                when (status) {
                    DownloadStatus.RUNNING, DownloadStatus.QUEUED ->
                        IconButton(onClick = { engine.pause(item.id) }) { Icon(Icons.Filled.Pause, stringResource(R.string.pause)) }
                    DownloadStatus.PAUSED ->
                        IconButton(onClick = { engine.resume(item.id) }) { Icon(Icons.Filled.PlayArrow, stringResource(R.string.resume)) }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED ->
                        IconButton(onClick = { engine.resume(item.id) }) { Icon(Icons.Filled.Refresh, stringResource(R.string.retry)) }
                    else -> {}
                }
                if (status == DownloadStatus.CANCELLED || status == DownloadStatus.FAILED)
                    IconButton(onClick = { engine.delete(item.id, false) }) { Icon(Icons.Filled.Delete, stringResource(R.string.remove)) }
                else
                    IconButton(onClick = { engine.cancel(item.id) }) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
private fun statusLine(item: DownloadEntity): String {
    val sizes = if (item.totalBytes > 0) "${Format.bytes(item.downloadedBytes)} / ${Format.bytes(item.totalBytes)}"
    else Format.bytes(item.downloadedBytes)
    return when (item.statusEnum) {
        DownloadStatus.RUNNING -> buildString {
            item.qualityLabel?.let { append(it).append(" • ") }
            if (item.totalBytes > 0) append("${(item.progress * 100).toInt()}% • ")
            append(sizes).append(" • ").append(Format.speed(item.speedBps))
            val eta = Format.eta(item.totalBytes - item.downloadedBytes, item.speedBps)
            if (eta.isNotEmpty()) append(" • ").append(eta)
        }
        DownloadStatus.QUEUED -> stringResource(R.string.status_queued) + (item.qualityLabel?.let { " • $it" } ?: "")
        DownloadStatus.PAUSED -> stringResource(R.string.status_paused) + " • " + sizes
        DownloadStatus.FAILED -> stringResource(R.string.status_failed) + ": " +
            com.amrreda.downloader.ui.ErrorText.describe(androidx.compose.ui.platform.LocalContext.current, item.error)
        DownloadStatus.CANCELLED -> stringResource(R.string.status_cancelled)
        DownloadStatus.COMPLETED -> sizes
    }
}
