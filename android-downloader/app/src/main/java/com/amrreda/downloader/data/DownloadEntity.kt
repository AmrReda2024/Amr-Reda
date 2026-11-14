package com.amrreda.downloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    /** True when the user typed the name; the server's name will not override it. */
    val nameFromUser: Boolean = false,
    val mimeType: String? = null,
    val category: String = FileCategory.OTHER.name,
    val totalBytes: Long = -1,
    val downloadedBytes: Long = 0,
    val speedBps: Long = 0,
    val status: String = DownloadStatus.QUEUED.name,
    val tempPath: String? = null,
    val contentUri: String? = null,
    val error: String? = null,
    val referer: String? = null,
    val userAgent: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
) {
    val statusEnum: DownloadStatus get() = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED)
    val categoryEnum: FileCategory get() = runCatching { FileCategory.valueOf(category) }.getOrDefault(FileCategory.OTHER)
    val progress: Float get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}
