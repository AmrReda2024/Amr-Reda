package com.amrreda.downloader.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

/** DIRECT = plain HTTP file download; VIDEO = handled by the embedded yt-dlp engine. */
enum class DownloadKind { DIRECT, VIDEO }

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
    // ---- v2: video-site downloads (yt-dlp) ----
    @ColumnInfo(defaultValue = "DIRECT") val kind: String = DownloadKind.DIRECT.name,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val formatSelector: String? = null,
    val formatSort: String? = null,
    val audioFormat: String? = null,
    val qualityLabel: String? = null,
    val platform: String? = null,
) {
    val statusEnum: DownloadStatus get() = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED)
    val categoryEnum: FileCategory get() = runCatching { FileCategory.valueOf(category) }.getOrDefault(FileCategory.OTHER)
    val isVideoKind: Boolean get() = kind == DownloadKind.VIDEO.name
    val displayName: String get() = if (statusEnum != DownloadStatus.COMPLETED && !title.isNullOrBlank()) title else fileName
    val progress: Float get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}
